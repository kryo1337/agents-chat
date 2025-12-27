# Frequently Asked Questions

## What is the purpose of this project?

The purpose is to demonstrate a small but realistic multi‑agent support system for a recruitment task, where separate AI agents handle technical and billing questions inside one continuous conversation.

## What languages does the support system support?

The underlying AI model (GPT-4o mini) is multilingual and can understand and generate text in dozens of languages, including English, Spanish, French, German, and Chinese. However, the system prompts and technical documentation are currently optimized for English interactions.

## How long is conversation data retained?

For this demo implementation, conversation history is stored in an in-memory cache and is retained only while the application is running. The cache uses an LRU (Least Recently Used) policy and holds a maximum of 10 active conversations. Restarting the server clears all history.

## Can I add my own custom agents?

Yes, the system is designed with extensibility in mind. You can add new agents by implementing the `Agent` interface and registering them with the `AgentOrchestrator`. You will also need to update the orchestrator's routing logic to recognize the new agent's domain.

## Is there a limit on concurrent conversations?

The current in-memory storage implementation limits the system to tracking the context of the last 10 active conversations. If more concurrent users access the system, the oldest conversations will lose their history context, though single-turn interactions will still work.
