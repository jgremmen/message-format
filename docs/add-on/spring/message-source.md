# MessageSupportMessageSource

Spring applications typically resolve internationalized messages through the `MessageSource`
interface. If you want to use the `MessageSupport` formatting engine from within Spring's
ecosystem, the `MessageSupportMessageSource` class provides a bridge between the two worlds.
It implements Spring's `HierarchicalMessageSource` interface and delegates all message
resolution and formatting to a backing `MessageSupport` instance.

This means that any component in your application that already uses `MessageSource` (such as
Thymeleaf templates, Spring MVC validation messages, or custom service beans) can
transparently benefit from the message format library's powerful formatting capabilities
without any code changes on the consumer side.


## Positional Argument Mapping

The Spring `MessageSource` API passes arguments as a positional `Object[]` array rather than
as named parameters. Because `MessageSupport` works exclusively with named parameters, the
adapter must translate positional arguments into names. It does this by combining a
configurable prefix with a 1-based index.

With the default prefix `p`, the first argument becomes `p1`, the second `p2`, the third
`p3`, and so on. When you write a message template that will be resolved through this message
source, you reference the arguments by their generated names.

```java
// Message registered with code "order.confirmation"
// template: "Order %{p1} placed for %{p2} item(s)."

messageSource.getMessage("order.confirmation",
    new Object[] { "ORD-4821", 3 }, Locale.US);
// "Order ORD-4821 placed for 3 item(s)."
```

The mapping starts at index 1, not 0. If you pass an array with three elements, the available
parameters are `p1`, `p2` and `p3`. There is no `p0` parameter unless you explicitly provide
a value for it in your message template through other means.


## Creating a MessageSupportMessageSource

The simplest way to create the message source is to pass a `MessageSupport` instance to the
constructor. This uses the default parameter prefix `p`.

```java
MessageSupport messageSupport = MessageSupportFactory.create(
    DefaultFormatterService.getSharedInstance());

messageSupport.addMessage("greeting", "Hello %{p1}!");

MessageSupportMessageSource messageSource =
    new MessageSupportMessageSource(messageSupport);

messageSource.getMessage("greeting", new Object[] { "World" }, Locale.US);
// "Hello World!"
```

### Custom Parameter Prefix

If `p1`, `p2`, etc. are not descriptive enough or conflict with existing parameter names in
your templates, you can specify a custom prefix. The prefix must start with a letter and must
not be blank.

```java
MessageSupportMessageSource messageSource =
    new MessageSupportMessageSource("arg", messageSupport);
```

With this configuration, arguments are mapped to `arg1`, `arg2`, `arg3`, and so on. Your
message templates must reference these names accordingly.

```java
// Message registered with code "welcome"
// template: "Welcome back, %{arg1}!"

messageSource.getMessage("welcome", new Object[] { "Alice" }, Locale.US);
// "Welcome back, Alice!"
```

If you try to create a message source with a prefix that is blank or does not start with a
letter, an `IllegalArgumentException` is thrown immediately.

```java
// throws IllegalArgumentException: parameterPrefix must start with a letter
new MessageSupportMessageSource("1x", messageSupport);
```


## Parent Message Source Fallback

Because `MessageSupportMessageSource` implements `HierarchicalMessageSource`, you can
configure a parent `MessageSource` that acts as a fallback. When a message code is not found
in the backing `MessageSupport`, the adapter delegates to the parent before giving up.

This is useful in applications where some messages are managed by the message format library
while others come from traditional `.properties` files or another `MessageSource`
implementation.

```java
// A standard Spring ResourceBundleMessageSource for legacy messages
ResourceBundleMessageSource legacySource = new ResourceBundleMessageSource();
legacySource.setBasename("messages");

// The message-format-backed source with the legacy source as parent
MessageSupportMessageSource messageSource =
    new MessageSupportMessageSource(messageSupport);
messageSource.setParentMessageSource(legacySource);
```

The resolution order is as follows. First, the adapter checks whether the backing
`MessageSupport` contains a message with the requested code. If it does, that message is
formatted and returned. If it does not, the parent message source is consulted. If the parent
also cannot resolve the code, the behavior depends on which `getMessage` overload was called:
either a `NoSuchMessageException` is thrown, or the provided default message is returned.

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

You can also retrieve the current parent using `getParentMessageSource()`, which returns
`null` if no parent has been configured.


## MessageSourceResolvable Support

Spring's `MessageSourceResolvable` interface allows an object to carry multiple candidate
codes, arguments and a default message. The `MessageSupportMessageSource` processes a
resolvable by trying each code in the order provided. The first code that exists in the
backing `MessageSupport` is used for formatting.

```java
DefaultMessageSourceResolvable resolvable = new DefaultMessageSourceResolvable(
    new String[] { "error.specific", "error.generic" },
    new Object[] { "file.txt" },
    "An error occurred");

messageSupport.addMessage("error.generic", "Error processing %{p1}");

messageSource.getMessage(resolvable, Locale.US);
// "Error processing file.txt"
```

In this example, `error.specific` does not exist in `MessageSupport`, so the adapter moves
on to `error.generic`, which does exist, and formats it with the provided argument. If none
of the codes were found in `MessageSupport`, the adapter would consult the parent message
source. If that also fails, the default message from the resolvable (`"An error occurred"`)
would be returned. If no default message is available either, a `NoSuchMessageException` is
thrown.


## Spring Bean Configuration

In a typical Spring Boot application you register the message source as a bean so that it can
be injected wherever a `MessageSource` is needed.

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

With this configuration in place, any Spring component can inject and use the `MessageSource`
as usual.

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

Calling `greet("Alice", Locale.US)` returns `"Hello Alice!"`, while calling
`greet("", Locale.US)` returns `"Hello stranger!"`.
