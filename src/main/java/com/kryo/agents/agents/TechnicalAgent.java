package com.kryo.agents.agents;

import org.springframework.stereotype.Service;

@Service
public class TechnicalAgent implements Agent {
    @Override
    public String getName() {
        return "technical";
    }

    @Override
    public String respond(String message) {
        return "TECHNICAL AGENT: ...";
    }
}
