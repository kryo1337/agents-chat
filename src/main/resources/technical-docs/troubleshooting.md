# Troubleshooting

## What is the most common error during integration?

The most common issue is sending an invalid JSON body or missing the required `message` field in the request. The backend then returns a `400 Bad Request` response indicating that the payload could not be processed.