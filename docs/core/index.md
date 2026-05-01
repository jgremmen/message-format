# Core Library

The `message-format` module is the core of the message-format library. It provides everything
needed to parse message format strings, format messages with named parameters and locale
awareness, and manage collections of messages and templates.

## Key Concepts

- **Message** -- the parsed, immutable representation of a message format string.
- **MessageSupport** -- the central entry point for formatting messages by code or inline format
  string.
- **MessageFactory** -- parses message format strings into `Message` instances.
- **FormatterService** -- resolves parameter formatters and post formatters for value types.
- **Templates** -- reusable message fragments that can be referenced from other messages.

## Getting Started

A `MessageSupport` instance is created via `MessageSupportFactory`:

```java
// Shared singleton (default formatters, no custom messages)
MessageSupport shared = MessageSupportFactory.shared();

// Custom instance
ConfigurableMessageSupport messageSupport = MessageSupportFactory.create(
    DefaultFormatterService.getSharedInstance(),
    MessageFactory.NO_CACHE_INSTANCE);

messageSupport.addMessage("welcome", "Hello %{name}!");

String text = messageSupport
    .code("welcome")
    .with("name", "World")
    .format();  // "Hello World!"
```

A `ConfigurableMessageSupport` can be sealed to produce an immutable `MessageSupport`:

```java
MessageSupport sealed = messageSupport.seal();
```

## Module Coordinates

```
de.sayayi.lib:message-format:<version>
```
