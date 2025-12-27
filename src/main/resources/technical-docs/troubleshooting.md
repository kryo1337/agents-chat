# Troubleshooting

## What is the most common error during integration?

The most common issue is sending an invalid JSON body or missing the required `message` field in the request. The backend then returns a `400 Bad Request` response indicating that the payload could not be processed.

## Why am I getting a 401 Unauthorized error?

A `401 Unauthorized` error indicates that the `X-User-ID` header is missing or empty in your request. Ensure you are sending this header with a valid user identifier (e.g., a UUID).

## What does "AI Service Error" (503) mean?

A `503 Service Unavailable` response with the body "AI Service Error: ..." usually means the backend failed to communicate with Azure OpenAI. Check your `.env` file to ensure the `AZURE_OPENAI_API_KEY` and `AZURE_OPENAI_ENDPOINT` are correct. It can also mean the model deployment name is wrong or the service is temporarily down.

## What should I do if the application crashes on startup?

First, check the console logs for specific error messages. Common startup failures include missing environment variables, port 8080 being already in use by another application, or an incompatible Java version (must be 21+). If the port is busy, you can change it in `application.yml` under `server.port`.

## Why is the AI response empty or null?

If the AI returns an empty response, it might be due to strict content filtering policies on the Azure OpenAI resource. Check the Azure portal logs for "Responsible AI" filter events. Alternatively, it could mean the model encountered an internal error or timeout; check the application logs for `AiCallException` details.

## How do I fix "JSON parse error" responses?

A "JSON parse error" means the request body sent to `/api/chat` is not valid JSON. Ensure that all strings are properly escaped (especially newlines and quotes), that you are using double quotes for keys and string values, and that there are no trailing commas.
