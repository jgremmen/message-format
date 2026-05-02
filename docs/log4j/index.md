# Log4j

The `message-format-log4j` module integrates the message-format library with Apache Log4j 2
by providing a drop-in `MessageFactory` replacement. This allows you to use the full
message-format syntax in your log statements.


## Log4jMessageFactory

`Log4jMessageFactory` is a Log4j `MessageFactory` that parses log message strings using the
message-format syntax. Parameters passed to `newMessage(String, Object...)` are made available
as `p1`, `p2`, `p3`, etc.

If the last parameter is a `Throwable`, it is propagated as the message's throwable so that
Log4j can include the stack trace in the log output.

Message formatting is lazy: the message string is only formatted when `getFormattedMessage()`
is called. If the log level is not enabled, no formatting work is performed. If a formatting
error occurs, an error placeholder is returned instead of throwing an exception, ensuring
that logging never disrupts the application.

### Examples

Simple log messages work as expected:

```java
logger.info("Hello World");
// logs: Hello World
```

Parameters are referenced as `p1`, `p2`, etc.:

```java
logger.info("Hello %{p1}, you are %{p2} years old", "Alice", 30);
// logs: Hello Alice, you are 30 years old
```

All message-format features are available, including map keys and configuration:

```java
logger.info("Processing %{p1} %{p1,1:'item',:'items'}", 5);
// logs: Processing 5 items

logger.info("Processing %{p1} %{p1,1:'item',:'items'}", 1);
// logs: Processing 1 item
```

A `Throwable` as the last parameter is attached to the log message. It is also available as a
parameter for formatting:

```java
logger.error("Failed to load %{p1}: %{p2}", "config.xml",
    new IOException("file not found"));
// logs: Failed to load config.xml: java.io.IOException: file not found
// (followed by the full stack trace, rendered by the Log4j layout)
```

### Parameterized Message Fallback

When enabled (the default), messages containing Log4j-style `{}` placeholders, and no
message-format placeholders (`%{`, `%[` or `%(`), are delegated to Log4j's
`ParameterizedMessage` for backward compatibility. This makes it safe to introduce
`Log4jMessageFactory` in an existing codebase without rewriting all log statements at once:

```java
// Handled by ParameterizedMessage (no message-format placeholders)
logger.info("Hello {}, you are {} years old", "Eve", 30);
// logs: Hello Eve, you are 30 years old

// Handled by message-format (contains %{...})
logger.info("Hello %{p1}", "World");
// logs: Hello World
```

When both styles are present in the same message, message-format takes over and the `{}`
literals are left as-is:

```java
logger.info("%{p1} {}", "Test");
// logs: Test {}
```

The fallback can be disabled by passing `false` to the constructor:

```java
var factory = new Log4jMessageFactory(false);
```


## Configuring Log4j

### Per-logger factory

The most straightforward approach is to pass the factory when obtaining a logger:

```java
private static final Log4jMessageFactory MESSAGE_FACTORY =
    new Log4jMessageFactory();

private static final Logger logger =
    LogManager.getLogger(MyClass.class, MESSAGE_FACTORY);
```

### Application-wide factory

To use `Log4jMessageFactory` for all loggers in your application, set the Log4j system
property before the logging system initializes:

```
-Dlog4j2.messageFactory=de.sayayi.lib.message.log4j.Log4jMessageFactory
```

Or set it programmatically before any logger is created:

```java
System.setProperty("log4j2.messageFactory",
    Log4jMessageFactory.class.getName());
```

### Custom MessageSupport

If you need custom formatters or a pre-configured message support instance, pass it to the
constructor:

```java
var messageSupport = MessageSupportFactory.create(
    DefaultFormatterService.getSharedInstance());
messageSupport.setLocale(Locale.US);

var factory = new Log4jMessageFactory(messageSupport, true);

var logger = LogManager.getLogger(MyClass.class, factory);

logger.info("Price: %{p1}", 49.95);
// logs: Price: 49.95
```


## Module Coordinates

```
de.sayayi.lib:message-format-log4j:<version>
```
