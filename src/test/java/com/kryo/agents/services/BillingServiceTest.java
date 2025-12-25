package com.kryo.agents.services;

import com.kryo.agents.services.BillingService.PlanChangeResult;
import com.kryo.agents.services.BillingService.RefundPolicy;
import com.kryo.agents.services.BillingService.RefundResult;
import com.kryo.agents.services.BillingService.SubscriptionDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class BillingServiceTest {

  private BillingService billingService;

  @BeforeEach
  void setUp() {
    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
    billingService = new BillingService(mapper, new ClassPathResource("billing-policy.json"));
    billingService.init();
  }

  @Test
  void checkSubscription_validCustomerId_returnsCorrectPlan() {
    SubscriptionDetails details = billingService.checkSubscription("customer-001");

    assertEquals("customer-001", details.customerId());
    assertEquals("Pro", details.plan());
    assertEquals(new BigDecimal("99.99"), details.price());
    assertEquals("monthly", details.billingCycle());
  }

  @Test
  void checkSubscription_invalidCustomerId_throwsException() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> billingService.checkSubscription("invalid-customer"));

    assertTrue(exception.getMessage().contains("Customer not found"));
  }

  @Test
  void checkSubscription_nullCustomerId_throwsException() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> billingService.checkSubscription(null));

    assertTrue(exception.getMessage().contains("CustomerId cannot be null"));
  }

  @Test
  void checkSubscription_blankCustomerId_throwsException() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> billingService.checkSubscription("   "));

    assertTrue(exception.getMessage().contains("CustomerId cannot be null"));
  }

  @Test
  void initiateRefund_within7Days_fullRefund() {
    RefundResult result = billingService.initiateRefund("customer-002", "Service not as expected");

    assertTrue(Double.parseDouble(result.refundAmount()) > 0);
    assertNotNull(result.ticketId());
    assertTrue(result.ticketId().startsWith("REF-"));
    assertTrue(result.formUrl().contains(result.ticketId()));
    assertTrue(result.policyExplanation().contains("Full refund"));
  }

  @Test
  void initiateRefund_within30Days_partialRefund() {
    RefundResult result = billingService.initiateRefund("customer-001", "Changed my mind");

    BigDecimal expectedPartial = new BigDecimal("99.99")
        .multiply(new BigDecimal("50"))
        .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);

    assertEquals(expectedPartial.toString(), result.refundAmount());
    assertNotNull(result.ticketId());
    assertTrue(result.policyExplanation().contains("50%"));
  }

  @Test
  void initiateRefund_after30Days_noRefund() {
    RefundResult result = billingService.initiateRefund("customer-003", "Too expensive");

    assertEquals("0.00", result.refundAmount());
    assertTrue(result.policyExplanation().contains("No refund"));
    assertNotNull(result.ticketId());
  }

  @Test
  void initiateRefund_invalidCustomerId_throwsException() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> billingService.initiateRefund("invalid", "reason"));

    assertTrue(exception.getMessage().contains("Customer not found"));
  }

  @Test
  void initiateRefund_nullReason_throwsException() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> billingService.initiateRefund("customer-001", null));

    assertTrue(exception.getMessage().contains("Refund reason cannot be null"));
  }

  @Test
  void explainRefundPolicy_returnsCorrectPolicy() {
    RefundPolicy policy = billingService.explainRefundPolicy();

    assertEquals("7 days", policy.fullRefundWindow());
    assertEquals("30 days", policy.partialRefundWindow());
    assertEquals("50%", policy.partialRefundPercentage());
    assertEquals("after 30 days", policy.noRefundAfter());
  }

  @Test
  void changePlan_validPlan_success() {
    PlanChangeResult result = billingService.changePlan("customer-001", "Enterprise");

    assertEquals("customer-001", result.customerId());
    assertEquals("Pro", result.previousPlan());
    assertEquals("Enterprise", result.newPlan());
    assertTrue(result.message().contains("Plan successfully changed"));
    assertNotNull(result.effectiveDate());
  }

  @Test
  void changePlan_samePlan_noChange() {
    PlanChangeResult result = billingService.changePlan("customer-001", "Pro");

    assertEquals("customer-001", result.customerId());
    assertEquals("Pro", result.previousPlan());
    assertEquals("Pro", result.newPlan());
    assertTrue(result.message().contains("No change needed"));
  }

  @Test
  void changePlan_invalidPlan_throwsException() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> billingService.changePlan("customer-001", "InvalidPlan"));

    assertTrue(exception.getMessage().contains("Invalid plan"));
  }

  @Test
  void changePlan_invalidCustomerId_throwsException() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> billingService.changePlan("invalid", "Pro"));

    assertTrue(exception.getMessage().contains("Customer not found"));
  }

  @Test
  void changePlan_nullCustomerId_throwsException() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> billingService.changePlan(null, "Pro"));

    assertTrue(exception.getMessage().contains("CustomerId cannot be null"));
  }

  @Test
  void changePlan_caseInsensitive_success() {
    PlanChangeResult result = billingService.changePlan("customer-001", "enterprise");

    assertEquals("Enterprise", result.newPlan());
  }

  @Test
  void multipleOperations_sameCustomerId_preserveState() {
    String customerId = "customer-002";

    SubscriptionDetails initial = billingService.checkSubscription(customerId);
    assertEquals("Starter", initial.plan());

    PlanChangeResult changeResult = billingService.changePlan(customerId, "Pro");
    assertEquals("Pro", changeResult.newPlan());

    SubscriptionDetails updated = billingService.checkSubscription(customerId);
    assertEquals("Pro", updated.plan());
  }
}
