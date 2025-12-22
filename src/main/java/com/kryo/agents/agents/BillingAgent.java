package com.kryo.agents.agents;

import org.springframework.stereotype.Service;

@Service
public class BillingAgent implements Agent {
    @Override
    public String getName() {
        return "billing";
    }

    @Override
    public String respond(String message) {
        return "BILLING AGENT: ...";
    }
}
