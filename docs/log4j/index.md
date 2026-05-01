# Log4j

The `message-format-log4j` module integrates the message-format library with
Apache Log4j 2 by providing a drop-in `MessageFactory` replacement.

## Log4jMessageFactory

`Log4jMessageFactory` is a Log4j `MessageFactory` that parses log message strings using the
message-format syntax. Parameters passed to `newMessage(String, Object...)` are made available
as `p1`, `p2`, `p3`, etc.

If the last parameter is a `Throwable`, it is propagated as the message's throwable.

### Parameterized Message Fallback

When enabled (default), messages containing Log4j-style `{}` placeholders -- and no
message-format `%{...}` placeholders -- are delegated to Log4j's `ParameterizedMessage` for
backward compatibility.

### Configuration

```java
// Default: shared formatter service, message cache of 256, fallback enabled
var factory = new Log4jMessageFactory();

// Custom MessageSupport, fallback disabled
var factory = new Log4jMessageFactory(customMessageSupport, false);
```

TODO: Document how to configure Log4j to use `Log4jMessageFactory`.

## Module Coordinates

```
de.sayayi.lib:message-format-log4j:<version>
```
