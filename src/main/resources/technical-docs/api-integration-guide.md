# API Integration Guide

## What is the main API endpoint?

The main endpoint is a single HTTP POST endpoint that accepts user messages and returns an AI‑generated reply from the appropriate support agent (technical or billing). It will be exposed as `/api/chat` on top of the Spring Boot backend.

## How do I authenticate my requests?

The API uses a custom header for user identification and data isolation. You must include the `X-User-ID` header in every request. The value should be a unique string identifier for the user (e.g., a UUID).

Example:
`X-User-ID: 550e8400-e29b-41d4-a716-446655440000`

## What are the rate limits for the API?

Currently, there are no enforced rate limits for this demonstration. However, in a production environment, you should implement rate limiting (e.g., 60 requests/minute) to protect the underlying AI resources.

## What HTTP status codes should I expect?

- `200 OK`: The request was successful, and the agent has generated a reply.
- `400 Bad Request`: The request body is missing required fields (like `message` or `conversationId`) or contains invalid JSON.
- `401 Unauthorized`: The `X-User-ID` header is missing or blank.
- `500 Internal Server Error`: An unexpected error occurred on the server.
- `503 Service Unavailable`: The AI service is currently down, overloaded, or the API Key configuration is incorrect.

## What is the request timeout setting?

The API enforces a 30-second timeout for generating a response. If the agent takes longer than 30 seconds to process the request (e.g., due to complex tool execution), the connection will be closed. Clients should set their read timeout to at least 35 seconds.
