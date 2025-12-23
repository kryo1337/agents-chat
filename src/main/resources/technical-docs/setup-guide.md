# Setup Guide

## How do I run the application locally?

You run the application by starting the Spring Boot backend (for example with `mvn spring-boot:run`) and then sending HTTP POST requests to `http://localhost:8080/api/chat` with a JSON body that includes at least a `conversationId` and a `message`.