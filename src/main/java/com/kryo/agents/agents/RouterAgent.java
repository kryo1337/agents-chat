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
    return "ROUTER: ...";
  }
}
