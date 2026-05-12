---
toc_depth: 2
---

# Messages and Templates

Every piece of text that the library can format is represented by a `Message` object. A message
is the parsed, immutable form of a message format string and is composed of one or more message
parts such as literal text, parameter references, template references and post-formatter
invocations. Once parsed, a message can be formatted repeatedly with different parameter values
and locales without paying the parsing cost again.

This page explains the `Message` interface hierarchy, how to create messages and templates
through `MessageFactory` parsing methods and the programmatic `MessageBuilder`, and how to
register them on a `ConfigurableMessageSupport`. For the format string syntax itself, see
[Syntax](../message/syntax.md). For how to configure and obtain a `MessageFactory`, see
[MessageFactory](message-factory.md).


## The Message Interface Hierarchy

The `Message` interface is sealed and defines three sub-interfaces that progressively refine what
a message can do. All message objects are immutable and thread-safe.

### Message

The root `Message` interface represents a message in its most generic form. It provides methods
for formatting the message, retrieving its constituent message parts, listing the template names
it references, and serializing it back into a format string. The static field `Message.EMPTY`
holds a shared singleton instance that always formats to an empty string.

### Message.WithSpaces

`Message.WithSpaces` extends `Message` and adds information about whether the message has a
leading or trailing space. When multiple messages or message parts are concatenated during
formatting, this space information determines whether a separator space is inserted between
adjacent parts. The `isSpaceBefore()` and `isSpaceAfter()` methods derive their values from
the first and last message part respectively.

Every message produced by `MessageFactory.parseMessage(String)` and
`MessageFactory.parseTemplate(String)` returns a `Message.WithSpaces`, because after parsing, the
space information is always known.

### Message.WithCode

`Message.WithCode` extends `Message` and associates a unique code with the message. This code is
what you use to register and look up a message in a `ConfigurableMessageSupport`.

When you register a message through `ConfigurableMessageSupport.addMessage(...)`, the message is
always stored as a `Message.WithCode`. If you parse a message without an explicit code, the
factory wraps it with the code you supply.

### Message.LocaleAware

`Message.LocaleAware` extends `Message` and holds multiple locale-specific messages. When the
message is formatted, the locale provided through the `Parameters` is used to select the best
matching variant. The matching algorithm first looks for an exact locale match (language and
country), then falls back to the same language with a different country, and finally selects the
first available variant as a last resort.

A `Message.LocaleAware` does not have a single array of message parts or a single format string.
Calling `getMessageParts()` or `asFormatString(Charset)` on it throws
`UnsupportedOperationException`. To inspect individual locale variants, use
`getLocalizedMessages()`, which returns an unmodifiable `Map<Locale,Message>`.


## Creating Messages with MessageFactory

`MessageFactory` provides parsing methods for both messages and templates, in two flavors each:
a single format string and a locale-keyed map of format strings.

### Parsing a Single Message

The `parseMessage(String)` method parses a message format string and returns a
`Message.WithSpaces`. If the factory has caching enabled, previously parsed strings are served
from the cache:

```java
MessageFactory factory = MessageFactory.getSharedInstance();

Message.WithSpaces msg = factory.parseMessage("Order %{orderId} placed.");
// Produces a Message.WithSpaces
```

To associate a code at parse time, use `parseMessage(String code, String text)`:

```java
Message.WithCode msg = factory.parseMessage("order.placed",
    "Order %{orderId} placed for %{customer}.");
// msg.getCode() returns "order.placed"
```

### Parsing Localized Messages

When you have the same message in multiple languages, pass a `Map<Locale, String>` keyed by
locale. With an explicit code:

```java
Message.WithCode msg = factory.parseMessage("item.count", Map.of(
    Locale.ENGLISH, "%{count,1:'1 item',:'%{count} items'}",
    Locale.GERMAN,  "%{count,1:'1 Eintrag',:'%{count} Einträge'}"));

// The resulting message implements both Message.WithCode and Message.LocaleAware.
// At format time, the best locale match is selected automatically.
```

Without an explicit code the factory generates one automatically. Generated codes follow the
pattern `MSG[...]` and can be recognized with `MessageFactory.isGeneratedCode(String)`:

```java
Message.WithCode msg = factory.parseMessage(Map.of(
    Locale.ENGLISH, "Hello %{name}!",
    Locale.FRENCH,  "Bonjour %{name} !"));

// msg.getCode() returns something like "MSG[1A2B3C4D5E-10001]"
MessageFactory.isGeneratedCode(msg.getCode());
// true
```

When the map contains only a single entry, the result is a plain `Message.WithCode` rather than
a `Message.LocaleAware`, because there is no locale selection to perform.

### Parsing Templates

Templates are parsed with `parseTemplate(String)`, which returns a `Message.WithSpaces`.
Templates differ from messages in that they cannot contain nested template references. The
parsed result is otherwise identical to a regular message.

```java
Message.WithSpaces template = factory.parseTemplate(
    "%{error,!empty:': %{error}'}");
```

Localized templates work the same way as localized messages. Pass a `Map<Locale, String>` to
`parseTemplate(Map)`:

```java
Message template = factory.parseTemplate(Map.of(
    Locale.ENGLISH, "%{count} item(s)",
    Locale.GERMAN,  "%{count} Eintrag/Einträge"));
```

When the map contains more than one entry, the factory generates a template code with the prefix
`TPL[...]` internally.

### Wrapping a Message with a Code

If you already have a parsed `Message` and need to associate it with a particular code, use
`withCode(String, Message)`:

```java
Message.WithSpaces parsed = factory.parseMessage("Hello %{name}!");
Message.WithCode withCode = factory.withCode("greeting", parsed);
// withCode.getCode() returns "greeting"
```

If the message already has the requested code, the same instance is returned. Otherwise, the
factory wraps the message with the new code. For locale-aware messages the locale map is
preserved; for empty messages a code-carrying empty message is created.


## Building Messages Programmatically

While `MessageFactory` parsing covers the common case of creating messages from format strings,
the `MessageBuilder` offers a fluent Java API for constructing messages entirely in code. This
is useful when the message structure is determined at runtime, when you want to avoid embedding
format strings in Java source, or when you need fine-grained control over individual parts.

### Obtaining a Builder

A `MessageBuilder` is obtained from the factory or through the static convenience method:

```java
// from a MessageFactory instance
MessageBuilder builder = factory.messageBuilder();

// using the shared factory
MessageBuilder builder = MessageBuilder.create();
```

The builder is not thread-safe and must not be reused after calling `build()` or
`buildWithCode(String)`.

### Text Parts

The simplest part is literal text. Consecutive `text()` calls are automatically merged into a
single text part:

```java
Message.WithSpaces msg = MessageBuilder
    .create()
    .text("Hello World!")
    .build();
// Equivalent format string: "Hello World!"
```

You can control leading and trailing spaces on a text part with `spaceBefore()`, `spaceAfter()`
and `spacesAround()`. A space is inserted between two adjacent parts when either the first part
has a trailing space or the second part has a leading space:

```java
Message.WithSpaces msg = MessageBuilder
    .create()
    .text("Order")
    .text("confirmed").spaceBefore()
    .build();
// Equivalent format string: "Order confirmed"
```

### Parameter Parts

A parameter part inserts a formatted value. After calling `parameter(name)`, the returned
`ParameterBuilder` lets you configure the formatter, map entries, configuration values, and
spaces:

```java
Message.WithSpaces msg = MessageBuilder
    .create()
    .text("Hello")
    .parameter("name")
        .withFormat("string")
        .spaceBefore()
    .text("!")
    .build();
// Equivalent format string: "Hello %{name,format:string}!"
```

The parameter builder supports map entries that mirror the map syntax of the format string. Each
map entry has a key, an optional comparison operator, and a value message:

```java
Message.WithSpaces msg = MessageBuilder
    .create()
    .text("You have")
    .parameter("count")
        .mapNumber(0).message("no items")
        .mapNumber(1).message("1 item")
        .mapDefault().message("%{count} items")
        .spaceBefore()
    .text(".")
    .build();
// Equivalent format string: "You have %{count,0:'no items',1:'1 item',:'%{count} items'}."
```

Map entries support relational operators for numeric and string keys. For null and empty keys,
equality operators (`eq`, `ne`) are available:

```java
Message.WithSpaces msg = MessageBuilder
    .create()
    .parameter("temperature")
        .mapNumber(0).lt().message("below zero")
        .mapNumber(0).eq().message("exactly zero")
        .mapNumber(0).gt().message("above zero")
        .mapDefault().message("unknown")
    .build();
// Equivalent format string: "%{temperature,<0:'below zero',0:'exactly zero',>0:'above zero',:'unknown'}"
```

Boolean map keys use `mapBool(boolean)`:

```java
Message.WithSpaces msg = MessageBuilder
    .create()
    .parameter("active")
        .mapBool(true).message("enabled")
        .mapBool(false).message("disabled")
    .build();
// Equivalent format string: "%{active,true:'enabled',false:'disabled'}"
```

Null and empty checks use `mapNull()` and `mapEmpty()`, each returning a builder that
supports the `ne()` modifier for negation:

```java
Message.WithSpaces msg = MessageBuilder
    .create()
    .text("Name:")
    .parameter("name")
        .mapNull().message("(not provided)")
        .mapEmpty().message("(blank)")
        .mapDefault().message("%{name}")
        .spaceBefore()
    .build();
// Equivalent format string: "Name: %{name,null:'(not provided)',empty:'(blank)',:'%{name}'}"
```

Configuration values can be attached to a parameter through `configString`, `configBool`,
`configNumber` and `configMessage`:

```java
Message.WithSpaces msg = MessageBuilder
    .create()
    .parameter("amount")
        .withFormat("number")
        .configString("grouping-separator", ".")
        .configBool("leading-zero", true)
    .build();
// Equivalent format string: "%{amount,format:number,grouping-separator:'.',leading-zero:true}"
```

### Post-Formatter Parts

A post-formatter wraps an inner message and transforms its output through a named post-formatter.
The inner message is configured through a callback on `withMessage(Consumer<MessageBuilder>)`:

```java
Message.WithSpaces msg = MessageBuilder
    .create()
    .postFormatter("upper")
        .withMessage(inner -> inner.text("hello world"))
    .build();
// Equivalent format string: "%(upper,'hello world')"
```

Post-formatters also support configuration values and space control, just like parameter parts.

### Template Parts

A template reference is added with `template(name)`. Default parameter values and parameter
delegation can be configured on the returned `TemplateBuilder`:

```java
Message.WithSpaces msg = MessageBuilder
    .create()
    .text("Result:")
    .template("summary")
        .withDefaultParameterString("label", "N/A")
        .withParameterDelegate("item", "product")
        .spaceBefore()
    .build();
// Equivalent format string: "Result: %[summary,label='N/A',item->product]"
```

### Building with a Code

To produce a `Message.WithCode` directly, call `buildWithCode(String)` instead of `build()`:

```java
Message.WithCode msg = MessageBuilder
    .create()
    .text("File")
    .parameter("filename").spaceBefore()
    .text("not found.")
    .spaceAfter()
    .buildWithCode("file.not-found");
// Equivalent format string: "File %{filename} not found. "
```


## Adding Messages to ConfigurableMessageSupport

Once you have created or parsed messages and templates, they need to be registered on a
`ConfigurableMessageSupport` before they can be formatted by code. The
[MessageSupport](message-support.md) page covers the `ConfigurableMessageSupport` API in
detail; this section focuses on the different ways to add messages and templates.

### Adding Messages by Code and Format String

The most convenient method accepts a code and a format string directly. The format string is
parsed internally using the `MessageFactory` associated with the message support:

```java
var messageSupport = MessageSupportFactory.create(
    DefaultFormatterService.getSharedInstance());

messageSupport.addMessage("user.greeting",
    "Welcome back, %{username}!");

messageSupport
    .code("user.greeting")
    .with("username", "Alice")
    .format();
// "Welcome back, Alice!"
```

### Adding Pre-parsed Messages

If you have a `Message.WithCode` from the factory or the builder, pass it to
`addMessage(Message.WithCode)`:

```java
MessageFactory factory = messageSupport.getMessageAccessor().getMessageFactory();

Message.WithCode msg = factory.parseMessage("order.shipped",
    "Your order %{orderId} has shipped.");

messageSupport.addMessage(msg);
```

This is also the path that adopters use internally. Every adopter eventually calls
`addMessage(Message.WithCode)` on the underlying `MessagePublisher`.

### Adding Localized Messages

Localized messages are created by parsing a `Map<Locale, String>` and then adding the result:

```java
MessageFactory factory = messageSupport.getMessageAccessor().getMessageFactory();

Message.WithCode msg = factory.parseMessage("cart.summary", Map.of(
    Locale.ENGLISH, "%{count,1:'1 item',:'%{count} items'} in your cart",
    Locale.GERMAN,  "%{count,1:'1 Artikel',:'%{count} Artikel'} in Ihrem Warenkorb"));

messageSupport.addMessage(msg);

messageSupport
    .code("cart.summary")
    .with("count", 3)
    .locale(Locale.GERMAN)
    .format();
// "3 Artikel in Ihrem Warenkorb"
```

### Adding Templates

Templates are registered with a name and a `Message`. The template is typically obtained from
`parseTemplate(String)` or `parseTemplate(Map)`:

```java
MessageFactory factory = messageSupport.getMessageAccessor().getMessageFactory();

messageSupport.addTemplate("opt-error",
    factory.parseTemplate("%{error,!empty:': %{error}'}"));

messageSupport
    .message("Operation failed%[opt-error]")
    .with("error", "disk full")
    .format();
// "Operation failed: disk full"

messageSupport
    .message("Operation failed%[opt-error]")
    .with("error", "")
    .format();
// "Operation failed"
```

Templates can also be built programmatically and then registered:

```java
Message.WithSpaces template = MessageBuilder
    .create()
    .parameter("unit")
        .mapEmpty().ne().message(inner ->
            inner.parameter("unit").spaceBefore())
        .mapDefault().message(Message.EMPTY)
    .build();

messageSupport.addTemplate("opt-unit", template);

messageSupport
    .message("Distance: %{value}%[opt-unit]")
    .with("value", 42)
    .with("unit", "km")
    .format();
// "Distance: 42 km"
```

### Duplicate Handling

Attempting to add a message whose code already exists throws a `DuplicateMessageException` if
the content differs. If the new message is identical to the existing one, the duplicate is
silently ignored. The same applies to templates with `DuplicateTemplateException`. This
behavior can be customized by installing a `MessageFilter` or `TemplateFilter` as described on
the [MessageSupport](message-support.md#filters) page.

### Bulk Loading

For loading messages from external sources such as properties files, resource bundles, annotated
classes, or compiled pack files, the library provides adopters that handle parsing and
registration in bulk. See [Adopters](../adopter/index.md) and
[Pack Files](pack-files.md) for details.
