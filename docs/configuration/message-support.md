# MessageSupport

`MessageSupport` is the central entry point for formatting messages. Every formatting operation in the library goes 
through this interface, whether the message was registered beforehand with a code or constructed inline from a format
string. It provides a fluent API for supplying parameter values, selecting a locale and producing the final text.

A `MessageSupport` instance is backed by two collaborating components. The `FormatterService` knows how to convert Java
values into text fragments (see [Formatter Service](formatter-service.md)), while the
[`MessageFactory`](message-factory.md) parses format strings into `Message` objects. Together they form the foundation 
that every formatting operation relies on.


## Creating a MessageSupport

Instances are created through `MessageSupportFactory`, which offers a sealed shared singleton for simple scenarios and
several `create` methods for configurable instances.

The `shared()` method returns a lazily initialized, sealed singleton that is backed by the shared 
`DefaultFormatterService`. It also discovers and registers any `JavaTemplate` service providers on the classpath.
It is convenient for simple scenarios that only require inline message formatting and do not need to register messages
or customize configuration:

```java
import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.MessageSupport;
import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.exception.DuplicateMessageException;
import de.sayayi.lib.message.formatter.DefaultFormatterService;
import de.sayayi.lib.message.template.Template;

import java.util.Locale;
import java.util.Set;

MessageSupport shared = MessageSupportFactory.shared();

String text = shared
    .message("Hello %{name}!")
    .with("name", "World")
    .format();
// "Hello World!"
```

Because the shared instance is sealed, it does not expose any of the mutating methods found on
`ConfigurableMessageSupport`. Messages, templates or default configuration cannot be added through it.

Most applications use the `create` method, which returns a `ConfigurableMessageSupport` that can be populated with 
messages, templates and default configuration. The backing `FormatterService` is passed explicitly, which makes it the
right choice when a custom or pre-configured formatter service is already available:

```java
var messageSupport = MessageSupportFactory.create(
    DefaultFormatterService.getSharedInstance());
```

A custom `MessageFactory`, for example one with a larger message cache, can be supplied as a second argument. See 
[MessageFactory](message-factory.md) for details.

Two convenience methods cover the most common starting points and avoid the need to construct a formatter service by
hand. `createDefault()` returns a `ConfigurableMessageSupport` backed by the shared `DefaultFormatterService`, which
already provides the full set of built-in formatters. It is the natural choice for most applications that intend to
register their own messages and templates:

```java
var messageSupport = MessageSupportFactory.createDefault();

messageSupport.addMessage("greeting", "Hello %{name}!");

String text = messageSupport
    .code("greeting")
    .with("name", "World")
    .format();
// "Hello World!"
```

`createGeneric()` instead starts from an empty `GenericFormatterService` that only provides the default string fallback
formatter. Nothing else is registered, so every additional formatter has to be added explicitly. This is useful when
full control over the available formatters is required, for example to keep the formatter set minimal or to avoid the
default formatters entirely:

```java
var messageSupport = MessageSupportFactory.createGeneric();

// only the string fallback is available until formatters
// are registered on the underlying formatter service
```

Both convenience methods, as well as `create(FormatterService)`, use the shared `MessageFactory`, which caches up to
128 parsed messages. When a different cache size, a part normalizer or another custom factory is needed, the explicit
`create(FormatterService, MessageFactory)` overload remains the way to supply it.


## ConfigurableMessageSupport

`ConfigurableMessageSupport` extends `MessageSupport` with mutating methods for adding messages and templates, setting 
default configuration values, changing the default locale and installing filters. It also implements `MessagePublisher`,
which means it can be passed directly to adopters and other components that register messages.

### Adding Messages

Messages are registered by code. Once a message has been added, it can be formatted anywhere in the application by 
referring to that code:

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

Besides a code and a format string, `addMessage` accepts a parsed `Message.WithCode`, for example a localized message
created by a `MessageFactory`. The [Messages](messages.md#adding-messages-to-configurablemessagesupport) page shows all
variants.

### Adding Templates

Templates are reusable message fragments registered under a kebab-case name and referenced from messages with the
`%[template-name]` syntax. `addTemplate` accepts a name together with a parsed `Template`, or a custom
`JavaTemplate`, which carries its own name. A name that does not follow the kebab-case convention is rejected with
an `IllegalArgumentException`. Parsing, building and implementing templates is covered on the
[Templates](templates.md) page.

### Duplicate Handling

A message code and a template name can only be registered once. Adding a message under a code that already exists
throws a `DuplicateMessageException` if the new message differs from the registered one. Two messages are equal when
their parsed message parts are equal. For localized messages, the locales and the message of every locale must be
equal. If the new message is equal to the registered one, it is silently ignored, so registering the same messages
twice is harmless.

Templates follow the same rules and throw a `DuplicateTemplateException` on a conflict. Two parsed templates are equal
when their message parts are equal. Two custom templates are equal when they have the same name.

The same rules apply to messages and templates registered by [adopters](../adopter/index.md) and by
[importing a pack file](pack-files.md#importing-messages), because these use `addMessage` and `addTemplate` as well.
The default behavior can be replaced by installing a filter, as described under [Filters](#filters).

### Default Configuration and Locale

`ConfigurableMessageSupport` provides methods for setting application-wide default configuration values and a default
locale. These topics are covered in detail on the [Default Configuration](default-configuration.md) page.

### Filters

The default [duplicate handling](#duplicate-handling) can be customized by installing a `MessageFilter` or
`TemplateFilter`. Both are functional interfaces whose single method receives the incoming entry and returns `true` to
accept it or `false` to skip it. An exception thrown by a filter is passed on to the caller of `addMessage` or
`addTemplate`. Installing a filter replaces the default filter, so the filter itself must detect duplicates if they are
to be rejected.

The filter is invoked while the message support holds its internal write lock. Checking for an existing entry and
adding the new one is therefore atomic. A filter may read from the message support, for example through its message
accessor, but it must return quickly and must never wait for another thread that uses the same message support, as
this would cause a deadlock.

A typical use case is logging every message that gets registered, for example during development when tracking what is
being loaded from annotation scanning or pack files is desired:

```java
System.Logger logger = System.getLogger("messages");

messageSupport.setMessageFilter(message -> {
    logger.log(System.Logger.Level.DEBUG,
        "Registering message " + message.getCode());
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

A filter that always returns `true` lets a later registration replace an earlier one, so the last registration of a
code wins:

```java
messageSupport.setMessageFilter(message -> true);

messageSupport.addMessage("greeting", "Hello!");
messageSupport.addMessage("greeting", "Hi!");
// no exception, "greeting" now formats as "Hi!"
```

Another scenario is selectively ignoring certain codes. When loading messages from multiple sources, it may be 
preferable to keep the first registration and silently discard any later attempt to register the same code, regardless 
of whether the content differs:

```java
messageSupport.setMessageFilter(message ->
    !messageSupport.getMessageAccessor()
        .hasMessageWithCode(message.getCode()));
```

Template filters work the same way. The `TemplateFilter` receives the template name and the template:

```java
messageSupport.setTemplateFilter((name, template) ->
    !messageSupport.getMessageAccessor().hasTemplateWithName(name));
```

### Sealing

Once all messages, templates and configuration have been registered, the `ConfigurableMessageSupport` can be sealed to
produce a read-only `MessageSupport`:

```java
MessageSupport sealed = messageSupport.seal();
```

The sealed instance is a lightweight wrapper that delegates formatting calls to the underlying configurable instance 
but hides mutating methods such as `addMessage`, `addTemplate` and `setDefaultConfig`. This is useful for exposing the
message support to other components without allowing them to modify it.

The sealed wrapper does not copy any data. Changes made to the underlying `ConfigurableMessageSupport` after sealing
are visible through the sealed instance. For a truly immutable snapshot, stop modifying the configurable instance after 
sealing.


## MessageAccessor

The `MessageAccessor` interface provides read-only access to the messages, formatters and default configuration managed
by a `MessageSupport`. It also extends `TemplateAccessor`, so all template-related queries are available through the 
same object. It is obtained through `getMessageAccessor()`:

```java
MessageSupport.MessageAccessor accessor = 
    messageSupport.getMessageAccessor();
```

Through the accessor the current state of the message support can be inspected without risk of modification. It provides
methods to query messages by code, list all registered message codes, check whether a message exists, look up
formatters, retrieve default configuration values and access the `MessageFactory`.

```java
messageSupport.addMessage("user.welcome", "Welcome, %{name}!");
String userName = "Alice";

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


## TemplateAccessor

`TemplateAccessor` provides read-only access to the templates managed by a `MessageSupport`. Because `MessageAccessor` 
extends `TemplateAccessor`, all template queries are available directly on the accessor obtained through 
`getMessageAccessor()`.

Template names can be listed, a template retrieved by name, existence checked, and templates that are referenced by 
messages but have not been registered found:

```java
Set<String> templateNames = accessor.getTemplateNames();

boolean exists = accessor.hasTemplateWithName("opt-detail");

Template template = accessor.getTemplateByName("opt-detail");

// find templates referenced by messages but not yet registered
Set<String> missing = accessor.findMissingTemplates(null);
```

The `findMissingTemplates` method accepts an optional `Predicate<String>` that filters which message codes to analyze.
Passing `null` analyzes all registered messages. This is useful during application startup to verify that all required 
templates have been added.


## Export and Import

`MessageSupport` provides an `exportMessages` method that serializes all registered messages and the templates they
reference to a compact binary pack file. The `ConfigurableMessageSupport.importMessages` method reads such a file and
adds all messages and templates to the instance. This topic is covered in detail on the [Pack Files](pack-files.md) 
page.


## Thread Safety

`MessageFactory`, `ConfigurableMessageSupport` and the singletons returned by
`DefaultFormatterService.getSharedInstance()` and `MessageSupportFactory.shared()` are all thread-safe. Adding messages,
changing configuration and formatting can happen concurrently. The same applies to the sealed view returned by
`seal()`, to the `MessageAccessor` and to all `Message` objects, which are immutable.

The objects that represent a single operation are not thread-safe and must be confined to the thread that created them.
These are the `MessageConfigurer` returned by `code(...)` and `message(...)`, the
[`MessageBuilder`](messages.md#building-messages-programmatically) and the
[`SingletonParameters`](../formatter/development/utilities.md#singletonparameters) helper. Filters installed with
`setMessageFilter` and `setTemplateFilter` run while the message support holds its write lock (see [Filters](#filters)).
