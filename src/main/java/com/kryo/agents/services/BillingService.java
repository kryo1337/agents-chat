package com.kryo.agents.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class BillingService {

  private static final Logger logger = LoggerFactory.getLogger(BillingService.class);
  private static final String SUPPORT_FORM_URL = "https://support.kryo.com/refund/";

  private final ObjectMapper objectMapper;
  private final Resource billingPolicyResource;
  private final Map<String, Customer> customers = new ConcurrentHashMap<>();
  private final AtomicLong ticketCounter = new AtomicLong(1000);

  private JsonNode billingPolicy;

  public BillingService(ObjectMapper objectMapper,
      @Value("${billing.policy.path:classpath:billing-policy.json}") Resource billingPolicyResource) {
    this.objectMapper = objectMapper;
    this.billingPolicyResource = billingPolicyResource;
  }

  @PostConstruct
  public void init() {
    loadBillingPolicy();
    initializeMockCustomers();
  }

  private void loadBillingPolicy() {
    try {
      billingPolicy = objectMapper.readTree(billingPolicyResource.getInputStream());
      logger.info("Loaded billing policy with {} subscription plans",
          billingPolicy.path("subscriptionPlans").size());
    } catch (IOException e) {
      logger.error("Failed to load billing policy: {}", e.getMessage());
      throw new RuntimeException("Failed to initialize billing service", e);
    }
  }

  private void initializeMockCustomers() {
    LocalDate now = LocalDate.now();

    customers.put("customer-001", new Customer(
        "customer-001",
        "Pro",
        new BigDecimal("99.99"),
        "monthly",
        now.minusDays(25),
        now.plusDays(5)));

    customers.put("customer-002", new Customer(
        "customer-002",
        "Starter",
        new BigDecimal("29.99"),
        "monthly",
        now.minusDays(5),
        now.plusDays(20)));

    customers.put("customer-003", new Customer(
        "customer-003",
        "Enterprise",
        new BigDecimal("499.99"),
        "yearly",
        now.minusDays(350),
        now.plusDays(15)));

    logger.info("Initialized {} mock customers", customers.size());
  }

  public SubscriptionDetails checkSubscription(String customerId) {
    if (customerId == null || customerId.isBlank()) {
      throw new IllegalArgumentException("CustomerId cannot be null or blank");
    }

    Customer customer = customers.get(customerId);
    if (customer == null) {
      logger.warn("Subscription check failed: customer not found - {}", customerId);
      throw new IllegalArgumentException("Customer not found: " + customerId);
    }

    logger.info("Subscription check: customerId={}, plan={}", customerId, customer.plan());
    return new SubscriptionDetails(
        customer.customerId(),
        customer.plan(),
        customer.price(),
        customer.billingCycle(),
        customer.startDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
        customer.renewalDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
  }

  public RefundResult initiateRefund(String customerId, String reason) {
    if (customerId == null || customerId.isBlank()) {
      throw new IllegalArgumentException("CustomerId cannot be null or blank");
    }
    if (reason == null || reason.isBlank()) {
      throw new IllegalArgumentException("Refund reason cannot be null or blank");
    }

    Customer customer = customers.get(customerId);
    if (customer == null) {
      logger.warn("Refund request failed: customer not found - {}", customerId);
      throw new IllegalArgumentException("Customer not found: " + customerId);
    }

    long daysSinceStart = calculateDaysSinceStart(customer);
    JsonNode refundPolicy = billingPolicy.path("refundPolicy");

    int fullRefundDays = refundPolicy.path("fullRefundDays").asInt();
    int partialRefundDays = refundPolicy.path("partialRefundDays").asInt();
    int partialRefundPercentage = refundPolicy.path("partialRefundPercentage").asInt();

    String refundAmount = "0.00";
    String refundPolicyDescription;

    if (daysSinceStart <= fullRefundDays) {
      refundAmount = customer.price().setScale(2, RoundingMode.HALF_UP).toString();
      refundPolicyDescription = "Full refund available (within " + fullRefundDays + " days)";
    } else if (daysSinceStart <= partialRefundDays) {
      BigDecimal partial = customer.price()
          .multiply(new BigDecimal(partialRefundPercentage))
          .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
      refundAmount = partial.toString();
      refundPolicyDescription = String.format("Partial refund available (%d%% within %d days)",
          partialRefundPercentage, partialRefundDays);
    } else {
      refundPolicyDescription = "No refund available (after " + partialRefundDays + " days)";
    }

    String ticketId = "REF-" + ticketCounter.incrementAndGet();
    String formUrl = SUPPORT_FORM_URL + ticketId;

    logger.info("Refund initiated: customerId={}, ticketId={}, amount={}, policy={}",
        customerId, ticketId, refundAmount, refundPolicyDescription);

    return new RefundResult(
        ticketId,
        formUrl,
        refundAmount,
        refundPolicyDescription);
  }

  public RefundPolicy explainRefundPolicy() {
    JsonNode refundPolicy = billingPolicy.path("refundPolicy");

    return new RefundPolicy(
        refundPolicy.path("fullRefundDays").asInt() + " days",
        refundPolicy.path("partialRefundDays").asInt() + " days",
        refundPolicy.path("partialRefundPercentage").asInt() + "%",
        "after " + refundPolicy.path("partialRefundDays").asInt() + " days");
  }

  public PlanChangeResult changePlan(String customerId, String newPlan) {
    if (customerId == null || customerId.isBlank()) {
      throw new IllegalArgumentException("CustomerId cannot be null or blank");
    }
    if (newPlan == null || newPlan.isBlank()) {
      throw new IllegalArgumentException("New plan cannot be null or blank");
    }

    Customer customer = customers.get(customerId);
    if (customer == null) {
      logger.warn("Plan change failed: customer not found - {}", customerId);
      throw new IllegalArgumentException("Customer not found: " + customerId);
    }

    String currentPlan = customer.plan();
    if (currentPlan.equalsIgnoreCase(newPlan)) {
      logger.info("Plan change not needed: customerId={}, already on {}", customerId, newPlan);
      return new PlanChangeResult(
          customerId,
          currentPlan,
          newPlan,
          "No change needed - you are already on the " + newPlan + " plan",
          LocalDate.now().toString());
    }

    newPlan = validateAndNormalizePlan(newPlan);
    BigDecimal newPrice = getPlanPrice(newPlan);
    LocalDate effectiveDate = calculateEffectiveDate(customer);

    customers.put(customerId, new Customer(
        customer.customerId(),
        newPlan,
        newPrice,
        customer.billingCycle(),
        customer.startDate(),
        effectiveDate));

    logger.info("Plan changed: customerId={}, from={}, to={}, effectiveDate={}",
        customerId, currentPlan, newPlan, effectiveDate);

    return new PlanChangeResult(
        customerId,
        currentPlan,
        newPlan,
        "Plan successfully changed. Your new billing will take effect on " + effectiveDate,
        effectiveDate.toString());
  }

  private long calculateDaysSinceStart(Customer customer) {
    return java.time.temporal.ChronoUnit.DAYS.between(customer.startDate(), LocalDate.now());
  }

  private String validateAndNormalizePlan(String plan) {
    List<String> validPlans = List.of("Starter", "Pro", "Enterprise");
    for (String validPlan : validPlans) {
      if (validPlan.equalsIgnoreCase(plan)) {
        return validPlan;
      }
    }
    throw new IllegalArgumentException("Invalid plan: must be one of " + validPlans);
  }

  private BigDecimal getPlanPrice(String plan) {
    JsonNode plans = billingPolicy.path("subscriptionPlans");
    for (JsonNode planNode : plans) {
      if (planNode.path("name").asText().equals(plan)) {
        return new BigDecimal(planNode.path("price").asText());
      }
    }
    throw new IllegalArgumentException("Plan price not found: " + plan);
  }

  private LocalDate calculateEffectiveDate(Customer customer) {
    if ("yearly".equalsIgnoreCase(customer.billingCycle())) {
      return customer.renewalDate();
    } else {
      return LocalDate.now().plusDays(1);
    }
  }

  public record Customer(
      String customerId,
      String plan,
      BigDecimal price,
      String billingCycle,
      LocalDate startDate,
      LocalDate renewalDate) {
  }

  public record SubscriptionDetails(
      String customerId,
      String plan,
      BigDecimal price,
      String billingCycle,
      String startDate,
      String renewalDate) {
  }

  public record RefundResult(
      String ticketId,
      String formUrl,
      String refundAmount,
      String policyExplanation) {
  }

  public record RefundPolicy(
      String fullRefundWindow,
      String partialRefundWindow,
      String partialRefundPercentage,
      String noRefundAfter) {
  }

  public record PlanChangeResult(
      String customerId,
      String previousPlan,
      String newPlan,
      String message,
      String effectiveDate) {
  }
}
