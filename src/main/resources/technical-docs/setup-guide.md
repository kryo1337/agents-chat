# Setup Guide

## How do I run the application locally?

You run the application by starting the Spring Boot backend (for example with `mvn spring-boot:run`) and then sending HTTP POST requests to `http://localhost:8080/api/chat`.

**Note**: You must include the `X-User-ID` header in your requests for user identification.

Request Body Example:
```json
{
  "conversationId": "uuid-1",
  "message": "Hello"
}
```

## What environment variables are required?

You must configure the Azure OpenAI connection settings. Create a `.env` file in the project root with the following variables:
- `AZURE_OPENAI_ENDPOINT`: Your Azure resource URL.
- `AZURE_OPENAI_API_KEY`: Your secret API key.
- `AZURE_OPENAI_DEPLOYMENT_NAME`: The name of your deployed model (e.g., gpt-4o-mini).
- `AZURE_OPENAI_API_VERSION`: The API version string (e.g., 2024-02-15-preview).

## Which Java version is required?

This project requires **Java 21** or higher. We use features like Records and Pattern Matching which are standard in modern Java versions. Ensure your `JAVA_HOME` environment variable points to a valid JDK 21 installation before building.

## How do I import this into IntelliJ or Eclipse?

Since this is a standard Maven project, you can import it by selecting "Open Project" and navigating to the `pom.xml` file. Your IDE should automatically detect the project structure and download the necessary dependencies. Ensure your project SDK is set to Java 21 in the IDE settings.

## How can I change the logging level?

You can adjust the logging level by modifying the `src/main/resources/application.yml` file. Under the `logging.level` section, you can set `com.kryo.agents` to `DEBUG` to see detailed logs about agent routing and tool execution, or `INFO` for standard operational logs.
