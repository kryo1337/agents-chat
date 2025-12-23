package com.kryo.agents.agents;

import org.springframework.stereotype.Service;

@Service
public class RouterAgent implements Agent {
  @Override
  public String getName() {
    return "router";
  }

  @Override
  public String respond(String message) {
    return "I apologize, but I can only assist with Technical Support or Billing inquiries. Could you please rephrase your question related to one of these topics?";
  }
}
