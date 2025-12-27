# Multi-Agent Support System

A conversational AI support system built with Java 21 and Spring Boot 4, featuring specialized agents for Technical Support (RAG-based) and Billing Operations (Tool-calling).

## Features

### Intelligent Orchestration
- **Dynamic Routing**: Automatically routes queries to the appropriate agent (Technical vs. Billing) based on context.
- **Context Awareness**: Maintains history across multi-turn interactions (limited to last 10 messages for efficiency).
- **Seamless Switching**: Agents hand off control dynamically (e.g., switching from billing to tech support mid-chat).

### Technical Support Agent
- **RAG Architecture**: Retrieval-Augmented Generation using local markdown documentation.
- **Grounded Answers**: Strictly answers from provided docs (API Guide, Troubleshooting, Setup, FAQ).
- **No Guessing**: Explicitly refuses to answer if the information is not found in the local documentation.
- **In-text Citations**: References specific documentation sources directly in the reply.

### Billing Support Agent
- **Function Calling**: Uses Azure OpenAI tool calling to perform real actions via Java logic.
- **Capabilities**:
  - Check subscription details
  - Initiate refunds (with automated policy checks)
  - Explain refund policies
  - Change subscription plans

### Frontend
- **UI**: Terminal-inspired chat interface.
- **Conversation Sidebar**: Browse and switch between the last 10 active conversations.
- **Real-time Indicators**: Visual feedback for connection status and "Thinking" state.

## Tech Stack

- **Core**: Java 21, Spring Boot 4.0.1
- **AI**: Azure OpenAI (GPT-4o mini)
- **Build**: Maven
- **Frontend**: HTML5, CSS3, Vanilla JS
- **Storage**: In-memory (ConcurrentHashMap with LRU eviction)

## Prerequisites

- Java 21+
- Maven
- Azure OpenAI Endpoint & Key

## Configuration

Create a `.env` file in the root directory:

```properties
AZURE_OPENAI_ENDPOINT=https://your-resource.openai.azure.com/
AZURE_OPENAI_API_KEY=your_api_key
AZURE_OPENAI_DEPLOYMENT_NAME=gpt-4o-mini
AZURE_OPENAI_API_VERSION=2024-02-15-preview
```

## How to Run

1.  **Build**:
    ```bash
    ./mvnw clean package
    ```
2.  **Run**:
    ```bash
    ./mvnw spring-boot:run
    ```
3.  **Access**: Open `http://localhost:8080`

## Mock Data for Testing

The system is pre-loaded with mock customers to test the Billing Agent's capabilities (refunds, subscription checks, plan changes).

| Customer ID    | Plan       | Price   | Billing Cycle | Account Age   | Refund Eligibility           |
| :------------- | :--------- | :------ | :------------ | :------------ | :--------------------------- |
| `customer-001` | Pro        | $99.99  | Monthly       | ~25 days      | Partial (50%)                |
| `customer-002` | Starter    | $29.99  | Monthly       | ~5 days       | Full Refund                  |
| `customer-003` | Enterprise | $499.99 | Yearly        | ~350 days     | None (Policy limit exceeded) |

> **Note**: Dates are calculated relative to the current date when the application starts.

## API Reference

### Chat Endpoints

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/chat` | Send a message to the agent gateway. |
| `GET` | `/api/chat/conversations` | List summaries of recent conversations. |
| `GET` | `/api/chat/conversations/{id}` | Retrieve full history for a specific chat. |

**Chat Request Example:**
```json
{
  "message": "I need a refund for customer-002",
  "conversationId": "uuid-here"
}
```

## Architecture

```
User -> [OpenCode UI] -> [ChatController]
                             |
                     [AgentOrchestrator]
                             |
           +-----------------+-----------------+
           |                 |                 |
   [TechnicalAgent]   [BillingAgent]     [RouterAgent]
           |                 |
      [RAG Service]    [BillingService]
      (Markdown)       (Mock Database)
```

## Testing

Run the full integration and unit test suite:
```bash
./mvnw test
```
