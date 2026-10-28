# MessageSupportMessageSource

Spring applications typically resolve internationalized messages through the `MessageSource` interface. To use the
`MessageSupport` formatting engine from within Spring's ecosystem, the `MessageSupportMessageSource` class provides a 
bridge between the two worlds. It implements Spring's `HierarchicalMessageSource` interface and delegates all message
resolution and formatting to a backing `MessageSupport` instance.

This means that any component in an application that already uses `MessageSource` (such as Thymeleaf templates, Spring
MVC validation messages, or custom service beans) can transparently benefit from the message format library's powerful
formatting capabilities without any code changes on the consumer side.


## Dependency

The class is part of the `message-format-spring` module, which also contains the
[SpEL formatter](../../formatter/parameter/named/spel.md). It requires Spring Framework 6.0.8 or later.

=== "Gradle (Groovy)"

    ```groovy
    dependencies {
      implementation 'de.sayayi.lib:message-format-spring:0.25.0'
    }
    ```

=== "Gradle (Kotlin)"

    ```kotlin
    dependencies {
      implementation("de.sayayi.lib:message-format-spring:0.25.0")
    }
    ```

=== "Maven"

    ```xml
    <dependency>
      <groupId>de.sayayi.lib</groupId>
      <artifactId>message-format-spring</artifactId>
      <version>0.25.0</version>
    </dependency>
    ```

For JPMS-based projects, the module name is `de.sayayi.lib.message.spring`. It requires `de.sayayi.lib.message`,
`spring.core` and `spring.context` transitively.


## Positional Argument Mapping

The Spring `MessageSource` API passes arguments as a positional `Object[]` array rather than as named parameters. 
Because `MessageSupport` works exclusively with named parameters, the adapter must translate positional arguments into
names. It does this by combining a configurable prefix with a 1-based index.

With the default prefix `p`, the first argument becomes `p1`, the second `p2`, the third `p3` and so on. A message 
template that will be resolved through this message source references the arguments by their generated names.

```java
// Message registered with code "order.confirmation"
// template: "Order %{p1} placed for %{p2} item(s)."

messageSource.getMessage("order.confirmation",
    new Object[] { "ORD-4821", 3 }, Locale.US);
// "Order ORD-4821 placed for 3 item(s)."
```

The mapping starts at index 1, not 0. Passing an array with three elements makes the available parameters `p1`, `p2` 
and `p3`. There is no `p0` parameter unless a value is explicitly provided for it in the message template through other 
means.


## Creating a MessageSupportMessageSource

The simplest way to create the message source is to pass a `MessageSupport` instance to the constructor. This uses the
default parameter prefix `p`.

```java
var messageSupport = MessageSupportFactory.createDefault();

messageSupport.addMessage("greeting", "Hello %{p1}!");

MessageSupportMessageSource messageSource =
    new MessageSupportMessageSource(messageSupport);

messageSource.getMessage("greeting", new Object[] { "World" }, Locale.US);
// "Hello World!"
```

### Custom Parameter Prefix

If `p1`, `p2`, etc. are not descriptive enough or conflict with existing parameter names in the templates, a custom 
prefix can be specified. The prefix must start with a letter and must not be blank. In addition, the generated names
must be valid parameter names (see [Syntax](../../message/syntax.md#parameters)). A prefix such as `Arg` or `my_arg`
is accepted by the constructor, but formatting a message with arguments then throws an `IllegalArgumentException`,
because `Arg1` and `my_arg1` are not valid parameter names.

```java
MessageSupportMessageSource messageSource =
    new MessageSupportMessageSource("arg", messageSupport);
```

With this configuration, arguments are mapped to `arg1`, `arg2`, `arg3` and so on. The message templates must reference
these names accordingly.

```java
// Message registered with code "welcome"
// template: "Welcome back, %{arg1}!"

messageSource.getMessage("welcome", new Object[] { "Alice" }, Locale.US);
// "Welcome back, Alice!"
```

Attempting to create a message source with a prefix that is blank or does not start with a letter throws an
`IllegalArgumentException` immediately.

```java
// throws IllegalArgumentException: parameterPrefix must start with 
// a letter
new MessageSupportMessageSource("1x", messageSupport);
```


## Parent Message Source Fallback

Because `MessageSupportMessageSource` implements `HierarchicalMessageSource`, a parent `MessageSource` can be
configured that acts as a fallback. When a message code is not found in the backing `MessageSupport`, the adapter 
delegates to the parent before giving up.

This is useful in applications where some messages are managed by the message format library while others come from 
traditional `.properties` files or another `MessageSource` implementation.

```java
// A standard Spring ResourceBundleMessageSource for legacy messages
ResourceBundleMessageSource legacySource = 
    new ResourceBundleMessageSource();
legacySource.setBasename("messages");

// The message-format-backed source with the legacy source as parent
MessageSupportMessageSource messageSource =
    new MessageSupportMessageSource(messageSupport);
messageSource.setParentMessageSource(legacySource);
```

The resolution order is as follows. First, the adapter checks whether the backing `MessageSupport` contains a message
with the requested code. If it does, that message is formatted and returned. If it does not, the parent message source
is consulted. If the parent also cannot resolve the code, the behavior depends on which `getMessage` overload was 
called: either a `NoSuchMessageException` is thrown, or the provided default message is returned.

```java
messageSupport.addMessage("app.title", "My Application");

// Resolves from MessageSupport
messageSource.getMessage("app.title", null, Locale.US);
// "My Application"

// Not in MessageSupport, falls through to parent
messageSource.getMessage("legacy.key", null, Locale.US);
// resolved by legacySource (e.g. from messages.properties)

// Not in MessageSupport or parent, returns default
messageSource.getMessage("unknown.code", null, "fallback text", Locale.US);
// "fallback text"
```

The current parent can also be retrieved using `getParentMessageSource()`, which returns `null` if no parent has been 
configured.

A default message is returned as it is. It is not parsed as a message format string, so it cannot contain parameter
references. A `null` locale formats the message in the default locale of the backing message support.


## MessageSourceResolvable Support

Spring's `MessageSourceResolvable` interface allows an object to carry multiple candidate codes, arguments and a 
default message. The `MessageSupportMessageSource` processes a resolvable by trying each code in the order provided. 
The first code that exists in the backing `MessageSupport` is used for formatting.

```java
DefaultMessageSourceResolvable resolvable = 
    new DefaultMessageSourceResolvable(
        new String[] { "error.specific", "error.generic" }, 
        new Object[] { "file.txt" }, 
        "An error occurred");

messageSupport.addMessage("error.generic", "Error processing %{p1}");

messageSource.getMessage(resolvable, Locale.US);
// "Error processing file.txt"
```

In this example, `error.specific` does not exist in `MessageSupport`, so the adapter moves on to `error.generic`, which
does exist and formats it with the provided argument. If none of the codes were found in `MessageSupport`, the adapter
would consult the parent message source. If that also fails, the default message from the resolvable (`"An error 
occurred"`) would be returned. If no default message is available either, a `NoSuchMessageException` is thrown.

/// note | Arguments Are Not Resolved
The arguments are passed to the message as they are. Unlike Spring's own `AbstractMessageSource`, the adapter does not
resolve arguments that are themselves a `MessageSourceResolvable`. Spring's validation framework passes the field name
as such an argument, so a message for a validation error receives the resolvable object as `p1` rather than the
localized field name. Such arguments have to be resolved before they are passed, or the message must not refer to
them.
///


## Spring Bean Configuration

In a typical Spring Boot application the message source is registered as a bean so that it can be injected wherever a
`MessageSource` is needed.

```java
@Configuration
public class MessageConfig 
{
  @Bean
  public MessageSupport messageSupport() 
  {
    var messageSupport = MessageSupportFactory.create(
        DefaultFormatterService.getSharedInstance());

    // Add messages programmatically or load from pack files
    messageSupport.addMessage("user.greeting",
        "Hello %{p1,!empty:'%{p1}',empty:'stranger'}!");

    return messageSupport;
  }

  @Bean
  public MessageSource messageSource(MessageSupport messageSupport) {
    return new MessageSupportMessageSource(messageSupport);
  }
}
```

With this configuration in place, any Spring component can inject and use the `MessageSource` as usual.

```java
@Service
public class NotificationService 
{
  private final MessageSource messageSource;
   
  public NotificationService(MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  public String greet(String userName, Locale locale) 
  {
    return messageSource.getMessage("user.greeting",
        new Object[] { userName }, locale);
  }
}
```

Calling `greet("Alice", Locale.US)` returns `"Hello Alice!"`, while calling `greet("", Locale.US)` returns
`"Hello stranger!"`.
