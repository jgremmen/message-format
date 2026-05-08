# MessageSupport

`MessageSupport` is the central entry point for formatting messages. Every formatting operation
in the library goes through this interface, whether the message was registered beforehand with a
code or constructed inline from a format string. It provides a fluent API for supplying parameter
values, selecting a locale and producing the final text.

A `MessageSupport` instance is backed by two collaborating components. The `FormatterService`
knows how to convert Java values into text fragments (see
[Formatter Service](formatter-service.md)), while the [`MessageFactory`](message-factory.md) parses format strings
into `Message` objects. Together they form the foundation that every formatting operation relies
on.


## Creating a MessageSupport

Instances are created through `MessageSupportFactory`, which offers two strategies.

The `shared()` method returns a lazily initialized, sealed singleton that is backed by the shared
`DefaultFormatterService`. It is convenient for simple scenarios where you only need inline
message formatting and do not need to register messages or customize configuration:

```java
MessageSupport shared = MessageSupportFactory.shared();

String text = shared
    .message("Hello %{name}!")
    .with("name", "World")
    .format();
// "Hello World!"
```

Because the shared instance is sealed, it does not expose any of the mutating methods found on
`ConfigurableMessageSupport`. You cannot add messages, templates or default configuration through
it.

For most applications you will use the `create` method, which returns a
`ConfigurableMessageSupport` that you can populate with messages, templates and default
configuration:

```java
var messageSupport = MessageSupportFactory.create(
    DefaultFormatterService.getSharedInstance());
```

If you need a custom `MessageFactory`, for example to enable message caching, you can supply it
as a second argument. See [MessageFactory](message-factory.md) for details.


## ConfigurableMessageSupport

`ConfigurableMessageSupport` extends `MessageSupport` with mutating methods for adding messages
and templates, setting default configuration values, changing the default locale, and installing
filters. It also implements `MessagePublisher`, which means it can be passed directly to adopters
and other components that register messages.

### Adding Messages

Messages are registered by code. Once a message has been added, it can be formatted anywhere in
the application by referring to that code:

```java
messageSupport.addMessage("order.placed",
    "Order %{orderId} placed for %{customer}.");

messageSupport
    .code("order.placed")
    .with("orderId", "A-1042")
    .with("customer", "Alice")
    .format();
// "Order A-1042 placed for Alice."
```

A convenience overload accepts the code and a format string directly. If you already have a
parsed `Message.WithCode` object (for example from a `MessageFactory`), you can pass it to
`addMessage(Message.WithCode)` instead.

Attempting to add a message with a code that already exists throws a `DuplicateMessageException`
when the content differs. If the new message is identical to the existing one, the duplicate is
silently ignored. This default behavior can be changed by installing a custom `MessageFilter`
(see [Filters](#filters)).

### Adding Templates

Templates are reusable message fragments registered under a name. They are referenced from
messages using the `%[template-name]` syntax. Because they share the parameter context of the
enclosing message, they can access the same parameter values without any extra wiring:

```java
var factory = messageSupport.getMessageAccessor().getMessageFactory();

messageSupport.addTemplate("opt-unit",
    factory.parseTemplate("%{unit,!empty:' %{unit}'}"));

messageSupport
    .message("Distance: %{value}%[opt-unit]")
    .with("value", 42)
    .with("unit", "km")
    .format();
// "Distance: 42 km"

messageSupport
    .message("Distance: %{value}%[opt-unit]")
    .with("value", 42)
    .with("unit", "")
    .format();
// "Distance: 42"
```

Just like messages, adding a template whose name already exists throws a
`DuplicateTemplateException` when the content differs. Identical duplicates are silently ignored.

### Default Configuration and Locale

`ConfigurableMessageSupport` provides methods for setting application-wide default configuration
values and a default locale. These topics are covered in detail on the
[Default Configuration](default-configuration.md) page.

### Filters

The default duplicate handling described above can be customized by installing a `MessageFilter`
or `TemplateFilter`. Both are functional interfaces whose single method receives the incoming
entry and returns `true` to accept it or `false` to skip it.

A typical use case is logging every message that gets registered, for example during development
when you want to track what is being loaded from annotation scanning or pack files:

```java
messageSupport.setMessageFilter(message -> {
    logger.debug("Registering message '{}'", message.getCode());
    var existing = messageSupport.getMessageAccessor()
        .getMessageByCode(message.getCode());
    if (existing != null && !existing.isSame(message))
    {
      throw new DuplicateMessageException(message.getCode(),
          "conflict detected");
    }

    return existing == null;
});
```

Another scenario is selectively ignoring certain codes. When loading messages from multiple
sources, you may want to keep the first registration and silently discard any later attempt
to register the same code, regardless of whether the content differs:

```java
messageSupport.setMessageFilter(message ->
    !messageSupport.getMessageAccessor()
        .hasMessageWithCode(message.getCode()));
```

Template filters work the same way. The `TemplateFilter` receives the template name and the
template message:

```java
messageSupport.setTemplateFilter((name, template) ->
    !messageSupport.getMessageAccessor()
        .hasTemplateWithName(name));
```

### Sealing

Once all messages, templates and configuration have been registered, you can seal the
`ConfigurableMessageSupport` to produce a read-only `MessageSupport`:

```java
MessageSupport sealed = messageSupport.seal();
```

The sealed instance is a lightweight wrapper that delegates formatting calls to the underlying
configurable instance but hides mutating methods such as `addMessage`, `addTemplate`, and
`setDefaultConfig`. This is useful when you want to expose the message support to other
components without allowing them to modify it.

The sealed wrapper does not copy any data. Changes made to the underlying
`ConfigurableMessageSupport` after sealing are visible through the sealed instance. If you need
a truly immutable snapshot, stop modifying the configurable instance after sealing.


## MessageAccessor

The `MessageAccessor` interface provides read-only access to the messages, templates, formatters
and default configuration managed by a `MessageSupport`. You obtain it through
`getMessageAccessor()`:

```java
MessageSupport.MessageAccessor accessor = messageSupport.getMessageAccessor();
```

Through the accessor you can inspect the current state of the message support without risk of
modification. It provides methods to query messages by code, list all registered message codes,
check whether a message or template exists, look up formatters, retrieve default configuration
values, and access the `MessageFactory`.

```java
// check if a message exists before formatting
if (accessor.hasMessageWithCode("user.welcome")) 
{
  messageSupport
      .code("user.welcome")
      .with("name", userName)
      .format();
}

// retrieve the set of registered message codes
Set<String> codes = accessor.getMessageCodes();

// look up the default locale
Locale locale = accessor.getLocale();

// access the message factory for parsing
MessageFactory factory = accessor.getMessageFactory();
```

### TemplateAccessor

`MessageAccessor` extends `TemplateAccessor`, which provides template-specific queries. You can
list template names, retrieve a template by name, check for existence, and find templates that
are referenced by messages but have not been registered:

```java
Set<String> templateNames = accessor.getTemplateNames();

boolean exists = accessor.hasTemplateWithName("opt-detail");

Message template = accessor.getTemplateByName("opt-detail");

// find templates referenced by messages but not yet registered
Set<String> missing = accessor.findMissingTemplates(null);
```

The `findMissingTemplates` method accepts an optional `Predicate<String>` that filters which
message codes to analyze. Passing `null` analyzes all registered messages. This is useful during
application startup to verify that all required templates have been added.


## Export and Import

`MessageSupport` provides an `exportMessages` method that serializes all registered messages and
the templates they reference to a compact binary pack file. The
`ConfigurableMessageSupport.importMessages` method reads such a file and adds all messages and
templates to the instance. This topic is covered in detail on the [Pack Files](pack-files.md) page.


## Thread Safety

`MessageFactory`, `ConfigurableMessageSupport` and the singletons returned by
`DefaultFormatterService.getSharedInstance()` and `MessageSupportFactory.shared()` are all
thread-safe. Adding messages, changing configuration and formatting can happen concurrently.
