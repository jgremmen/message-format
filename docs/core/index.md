# Core Library

The `message-format` module is the foundation of the message-format library. It provides
everything needed to parse message format strings into immutable `Message` objects, format them
with named parameters and locale awareness, and manage collections of messages and templates. All
other modules in the library depend on this core.


## Overview

At the center of the library is the `MessageSupport` interface. It is the single entry point
through which your application formats messages, whether those messages were registered ahead of
time with a code or constructed inline from a format string. A `MessageSupport` instance is
backed by a `FormatterService` that knows how to convert Java values into text, and a
`MessageFactory` that parses format strings into `Message` objects.

The library follows a configure-once, use-everywhere pattern. You create a
`ConfigurableMessageSupport`, add your messages and templates, set any default configuration, and
then either use it directly or `seal()` it to produce a read-only `MessageSupport` that can be
shared safely across your application.


## Getting Started

The fastest way to get a working `MessageSupport` is through `MessageSupportFactory`. The factory
offers two approaches.

For applications that only need inline message formatting without pre-registered messages, the
shared singleton is the simplest option:

```java
MessageSupport shared = MessageSupportFactory.shared();

String text = shared
    .message("Hello %{name}!")
    .with("name", "World")
    .format();
// "Hello World!"
```

The shared instance is backed by the `DefaultFormatterService`, which automatically discovers all
parameter formatters and post formatters on the classpath through the Java `ServiceLoader`
mechanism. It is sealed and cannot be modified, so you cannot add messages or change configuration
on it.

For most applications you will want a `ConfigurableMessageSupport` that you can populate with
messages and configure to your needs. Create one by passing a `FormatterService`:

```java
ConfigurableMessageSupport messageSupport = MessageSupportFactory.create(
    DefaultFormatterService.getSharedInstance());
```

If you need explicit control over the `MessageFactory`, for example to enable message caching or
to provide a custom `MessagePartNormalizer`, you can supply it as a second argument:

```java
ConfigurableMessageSupport messageSupport = MessageSupportFactory.create(
    DefaultFormatterService.getSharedInstance(),
    new MessageFactory(myNormalizer, 256));
```

When no factory is specified, `MessageFactory.getSharedInstance()` is used by default. This
shared singleton, which caches up to 128 parsed messages, is sufficient for most use cases.


## Adding Messages

A `ConfigurableMessageSupport` lets you register messages identified by a unique code. Once
registered, a message can be formatted by referring to its code:

```java
messageSupport.addMessage("welcome", "Hello %{name}!");
messageSupport.addMessage("farewell", "Goodbye %{name}.");

messageSupport
    .code("welcome")
    .with("name", "Alice")
    .format();
// "Hello Alice!"
```

The `addMessage` method parses the format string and stores the resulting `Message` object.
Attempting to add a second message with the same code throws a `DuplicateMessageException` if the
content differs. Identical duplicates are silently ignored. This behavior can be changed by
registering a custom `MessageFilter`.

Messages can also be loaded in bulk from external sources through
[adopters](../adopter/index.md), from [annotation-scanned classes](../adopter/annotation/index.md), or
from [pack files](pack-files.md).


## Formatting Messages

The formatting API is a fluent chain: select a message, provide parameter values, optionally set
a locale, and call `format()` to produce the result.

When formatting a message registered by code, use `code()`:

```java
var factory = messageSupport.getMessageAccessor().getMessageFactory();

messageSupport.addMessage(factory.parseMessage("price-label", Map.of(
    Locale.ENGLISH, "Price: %{amount}",
    Locale.GERMAN, "Preis: %{amount}")));

messageSupport
    .code("price-label")
    .with("amount", 49.99)
    .locale(Locale.GERMAN)
    .format();
// "Preis: 49,99"
```

When formatting an inline format string that has not been registered, use `message()`:

```java
messageSupport
    .message("%{count,0:'No items',1:'1 item',:'%{count} items'} in the cart.")
    .with("count", 3)
    .format();
// "3 items in the cart."
```

Both methods return a `MessageConfigurer` that provides `with()` for setting parameter values,
`locale()` for specifying the formatting locale, and `format()` for producing the final string.
The configurer also offers `formattedException()` and `formattedExceptionSupplier()` for creating
exceptions whose message is a formatted string.


## Adding Templates

Templates are reusable message fragments registered under a name. They can be referenced from any
message using the `%[template-name]` syntax. This avoids duplicating complex formatting logic
across multiple messages:

```java
var factory = messageSupport.getMessageAccessor().getMessageFactory();

messageSupport.addTemplate("opt-detail",
    factory.parseTemplate("%{detail,!empty:' (%{detail})'}"));

messageSupport
    .message("Task completed%[opt-detail]")
    .with("detail", "3 warnings")
    .format();
// "Task completed (3 warnings)"

messageSupport
    .message("Task completed%[opt-detail]")
    .with("detail", "")
    .format();
// "Task completed"
```

Templates share the same parameter context as the enclosing message. The
[syntax reference](syntax.md) describes parameter delegation and defaults for templates.


## Sealing

Once all messages, templates and default configuration have been registered, you can seal the
`ConfigurableMessageSupport` to produce a read-only `MessageSupport`:

```java
MessageSupport sealed = messageSupport.seal();
```

The sealed instance is a lightweight wrapper that delegates all formatting calls to the
underlying configurable instance. It simply hides the mutating methods (`addMessage`,
`addTemplate`, `setDefaultConfig`, etc.) from the rest of your application. This is useful when
you want to expose the message support to other components without allowing them to modify it.

Note that the sealed wrapper does not copy the data. Changes made to the underlying
`ConfigurableMessageSupport` after sealing will be visible through the sealed instance.


## Thread Safety

`MessageFactory` is thread-safe. All its public methods can be called concurrently from multiple
threads. When caching is enabled, the internal cache is protected by a lock.

`ConfigurableMessageSupport` is also safe to use from multiple threads. Adding messages and
formatting can happen concurrently. The `DefaultFormatterService.getSharedInstance()` singleton
and the `MessageSupportFactory.shared()` singleton are both lazily initialized in a thread-safe
manner.


## Module Coordinates

```
de.sayayi.lib:message-format:<version>
```

=== "Gradle"

    ```groovy
    dependencies {
      implementation 'de.sayayi.lib:message-format:<version>'
    }
    ```

=== "Maven"

    ```xml
    <dependency>
      <groupId>de.sayayi.lib</groupId>
      <artifactId>message-format</artifactId>
      <version><!-- version --></version>
    </dependency>
    ```
