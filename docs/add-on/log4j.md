# Log4j

The `message-format-log4j` module integrates the message format library with
[Apache Log4j2](https://logging.apache.org/log4j/2.x/). It provides a Log4j `MessageFactory`
implementation that replaces Log4j's default message formatting with the full power of the
message format syntax. Instead of writing log statements with Log4j's `{}` placeholders, you
can use parameter references, map keys, templates and post formatters directly in your log
messages.

The module consists of two classes. `Log4jMessageFactory` is the public entry point that you
configure on your loggers. It parses incoming log messages using the message format syntax and
maps the positional arguments from Log4j's logging methods to named parameters `p1`, `p2`,
`p3` and so on. `Log4jMessage` is a package-private implementation of Log4j's `Message`
interface that evaluates the formatted result lazily, so the formatting work only happens if
the log message is actually written to an appender.


## Dependency

Add the `message-format-log4j` module to your project alongside the Log4j API dependency.

=== "Gradle (Groovy DSL)"

    ```groovy
    dependencies {
      implementation 'de.sayayi.lib:message-format-log4j:0.23.0'
      implementation 'org.apache.logging.log4j:log4j-api:2.24.3'
    }
    ```

=== "Gradle (Kotlin DSL)"

    ```kotlin
    dependencies {
      implementation("de.sayayi.lib:message-format-log4j:0.23.0")
      implementation("org.apache.logging.log4j:log4j-api:2.24.3")
    }
    ```

=== "Maven"

    ```xml
    <dependency>
      <groupId>de.sayayi.lib</groupId>
      <artifactId>message-format-log4j</artifactId>
      <version>0.22.0</version>
    </dependency>
    <dependency>
      <groupId>org.apache.logging.log4j</groupId>
      <artifactId>log4j-api</artifactId>
      <version>2.24.3</version>
    </dependency>
    ```

For JPMS-based projects, the module name is `de.sayayi.lib.message.log4j`. It requires
`de.sayayi.lib.message` and `org.apache.logging.log4j`.


## Creating a Logger

To use the message format syntax in your log statements, create a `Logger` with a
`Log4jMessageFactory` instance. The factory is passed as the second argument to
`LogManager.getLogger`.

```java
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import de.sayayi.lib.message.log4j.Log4jMessageFactory;

public class OrderService
{
  private static final Logger logger =
      LogManager.getLogger(OrderService.class, new Log4jMessageFactory());

  public void placeOrder(String customerId, int itemCount)
  {
    logger.info("Customer %{p1} placed an order with %{p2} items.",
        customerId, itemCount);
    // Output: "Customer C-42 placed an order with 3 items."
  }
}
```

The factory created with the no-argument constructor uses the shared `DefaultFormatterService`
and enables the parameterized message fallback (explained below). Every logger that should use
the message format syntax needs its own factory reference at construction time. Loggers created
without a factory continue to use Log4j's default formatting.


## Global Configuration

Instead of passing a `Log4jMessageFactory` to each individual logger, you can configure it as
the default message factory for the entire application. Log4j2 reads the property
`log4j2.messageFactory` at startup and uses the specified class for every logger that is
created without an explicit factory argument.

The most straightforward way to set this property is on the JVM command line:

```
-Dlog4j2.messageFactory=de.sayayi.lib.message.log4j.Log4jMessageFactory
```

Alternatively, you can place a file named `log4j2.component.properties` on the classpath
(typically in `src/main/resources`). Log4j2 reads this file automatically during
initialization, so no JVM arguments are required.

```properties
# src/main/resources/log4j2.component.properties
log4j2.messageFactory=de.sayayi.lib.message.log4j.Log4jMessageFactory
```

With either approach in place, all calls to `LogManager.getLogger()` throughout the
application will automatically use the message format syntax.

```java
// No factory argument needed — the global default applies
private static final Logger logger = LogManager.getLogger(OrderService.class);
```

Log4j instantiates the factory through its public no-argument constructor. This means the
global factory always uses the shared `DefaultFormatterService` and has the parameterized
message fallback enabled. If you need a custom `MessageSupport` instance or want to disable
the fallback, you must pass the factory explicitly to each logger as shown in the previous
section.

Because the property affects all loggers, third-party libraries that use Log4j in the same
JVM will also receive a `Log4jMessageFactory`. This is usually harmless when the parameterized
message fallback is active, since their existing `{}` log statements are automatically
delegated to Log4j's `ParameterizedMessage`. However, if a library produces log messages that
contain character sequences resembling message format markers (`%{`, `%[` or `%(`), those
messages will be parsed by the message format engine instead. Review your dependencies before
enabling this setting globally.


## Parameter Mapping

Log4j passes arguments to log methods as a positional `Object[]` array. Because the message
format library works with named parameters, the factory maps each positional argument to a
name by combining the prefix `p` with a 1-based index. The first argument becomes `p1`, the
second `p2`, the third `p3`, and so on.

```java
logger.info("Hello %{p1}, welcome to %{p2}!", "Alice", "Wonderland");
// Output: "Hello Alice, welcome to Wonderland!"
```

You can use the full message format syntax on these parameters, including map keys,
configuration keys and named formatters.

```java
logger.info(
    "%{p1,0:'no items',1:'one item',:'%{p1} items'} in the cart.",
    itemCount);
// itemCount = 0  -> "no items in the cart."
// itemCount = 1  -> "one item in the cart."
// itemCount = 7  -> "7 items in the cart."
```

```java
logger.debug(
    "User %{p1} has role %{p2,'admin':'Administrator','user':'Standard User',:'%{p2}'}.",
    userName, role);
// userName = "bob", role = "admin"
// Output: "User bob has role Administrator."
```


## Throwable Handling

When the last argument passed to a log method is a `Throwable`, the factory attaches it to
the returned `Log4jMessage` so that Log4j can render the stack trace according to the
configured layout pattern. The `Throwable` is still available as a regular parameter, so you
can reference it in the message text if needed.

```java
try {
  connectToDatabase();
} catch(SQLException ex) {
  logger.error("Connection to %{p1}:%{p2} failed.", host, port, ex);
  // Output: "Connection to db.local:5432 failed."
  // The stack trace of 'ex' is rendered by Log4j's layout.
}
```

In this example the exception is the third argument and therefore mapped to `p3`. Because it
is not referenced in the message string, it does not appear in the formatted text, but Log4j
still receives it through `getThrowable()` for stack trace rendering.

Only the last parameter is inspected for this behavior. If a `Throwable` appears at any other
position in the argument list, it is still available as a regular parameter for formatting,
but Log4j will not receive it for stack trace rendering.

```java
logger.warn("Step %{p1} failed: %{p2}. Retrying with %{p3}.",
    3, new IOException("disk full"), "fallback-path");
// Output: "Step 3 failed: disk full. Retrying with fallback-path."
// No stack trace is rendered because the last argument is a String, not a Throwable.
```


## Lazy Formatting

The `Log4jMessageFactory` produces `Log4jMessage` instances that evaluate the formatted
message string lazily. The message format string is not parsed, and the parameters are not
formatted, until `getFormattedMessage()` is called by the logging framework. This means that
if a log statement's level is below the configured threshold, no formatting work takes place
at all.

Internally, the factory obtains a `Supplier<String>` from the message support's
`formatSupplier()` method and wraps it in a caching delegate. The first call to
`getFormattedMessage()` invokes the supplier and caches the result. Subsequent calls return
the cached value without re-evaluating the format expression.

/// warning | Performance consideration
Using `Log4jMessageFactory` is slower than Log4j's default parameterized formatting. The
message format engine parses the format string, resolves named parameters and evaluates map
keys, which involves considerably more work than simple `{}` placeholder substitution. Lazy
evaluation ensures that this cost is only paid when the message is actually written, but for
high-throughput logging at active log levels the overhead can be noticeable. Profile your
application if logging performance is critical.
///


## Parameterized Message Fallback

Existing codebases often contain log statements that already use Log4j's native `{}`
placeholder syntax. To ease migration, the factory supports a fallback mode that is enabled
by default. When the fallback is active, the factory inspects each message string before
parsing it. If the string contains `{}` placeholders but does not contain any message format
markers (`%{`, `%[` or `%(`), the factory delegates formatting to Log4j's built-in
`ParameterizedMessage` instead of processing it through the message format engine.

This means that you can introduce the `Log4jMessageFactory` to an existing logger without
breaking any existing log statements. Old-style `{}` messages continue to work as before,
while new log statements can use the full message format syntax.

```java
// Works with the fallback: formatted by Log4j's ParameterizedMessage
logger.info("User {} logged in from {}", userName, ipAddress);
// Output: "User Alice logged in from 192.168.1.1"

// Uses the message format engine: contains %{...}
logger.info("User %{p1} logged in from %{p2}", userName, ipAddress);
// Output: "User Alice logged in from 192.168.1.1"
```

When both `{}` and a message format marker are present in the same string, the factory always
uses the message format engine. The `{}` characters are treated as literal text in that case.

```java
logger.info("%{p1} literal {} text", "value");
// Output: "value literal {} text"
```

Escaped `{}` sequences (`\{}`) are also recognized. If all `{}` occurrences in the string are
escaped, the factory treats the string as a message format string rather than delegating to
`ParameterizedMessage`.

```java
logger.info("escaped \\{} placeholder");
// Output: "escaped {} placeholder"
```

If your codebase has been fully migrated to message format syntax, you can disable the
fallback by passing `false` to the constructor. With the fallback disabled, every message
string is parsed as a message format string and any `{}` in the text is treated as literal
characters.

```java
private static final Logger logger =
    LogManager.getLogger(MyClass.class, new Log4jMessageFactory(false));
```


## Custom MessageSupport

The default constructor creates a `MessageSupport` instance internally, using the shared
`DefaultFormatterService` and a `MessageFactory` with pass-through normalization and a cache
size of 256 entries. This setup is appropriate for most applications.

If you need more control over the formatting pipeline, you can provide your own
`MessageSupport` instance. This allows you to register custom formatters, add templates,
configure default parameter values, or use a different formatter service entirely.

```java
var formatterService = new DefaultFormatterService();
// register custom formatters if needed

var messageSupport = MessageSupportFactory.create(
    formatterService,
    new MessageFactory(MessagePartNormalizer.PASS_THROUGH, 512));

// Add a reusable template
messageSupport.addTemplate("user-info",
    MessageFactory
        .getSharedInstance()
        .parseTemplate("%{name} (%{role})"));

var factory = new Log4jMessageFactory(messageSupport, true);

Logger logger = LogManager.getLogger(MyClass.class, factory);

logger.info("Logged in: %[user-info,name->p1,role->p2]", "Alice", "admin");
// Output: "Logged in: Alice (admin)"
```

The second constructor argument controls the parameterized message fallback, just like the
single-boolean constructor described earlier.


## Error Handling

If a message format string is syntactically invalid or if formatting fails for any other
reason, the factory does not throw an exception. Instead, it returns a diagnostic placeholder
string that includes the original message text. This prevents a broken log statement from
crashing the application.

```java
logger.info("invalid %{}");
// Output: "<internal error formatting: invalid %{}>"
```

This behavior is intentional for a logging context, where reliability is more important than
strict validation. The diagnostic output makes it easy to spot and fix the problem without
disrupting the application's runtime behavior.
