# API Integration Guide

## What is the main API endpoint?

The main endpoint is a single HTTP POST endpoint that accepts user messages and returns an AI‑generated reply from the appropriate support agent (technical or billing). It will be exposed as `/api/chat` on top of the Spring Boot backend.