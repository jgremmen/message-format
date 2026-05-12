---
toc_depth: 2
---

# Formatting Messages

Once you have a [`MessageSupport`](../configuration/message-support.md) instance, you use its fluent API to format
messages. The entry point is either `code(...)` for messages that have been published with a message code,
or `message(...)` for inline format strings and pre-parsed `Message` objects. Both methods
return a `MessageConfigurer` that lets you set parameter values, choose a locale, and produce
the final formatted text.

This page covers the full lifecycle of formatting a message: starting with the configurer,
setting parameters, choosing a locale, producing the formatted result, and creating
exceptions with formatted messages.


## Starting a Message Configurer

There are three ways to obtain a `MessageConfigurer` from a `MessageSupport` instance.

### By Message Code

If a message has been published to the message support (for example through annotations or
by calling `addMessage`), you can reference it by its code. The `code` method looks up the
message and returns a configurer for it.

```java
var messageSupport = MessageSupportFactory.create(
    DefaultFormatterService.getSharedInstance());

messageSupport.addMessage("order.confirm",
    "Order %{orderId} confirmed for %{customer}.");

String text = messageSupport
    .code("order.confirm")
    .with("orderId", "A-1234")
    .with("customer", "Alice")
    .format();
// "Order A-1234 confirmed for Alice."
```

If the code does not exist, an `IllegalArgumentException` is thrown. This makes it easy to
catch configuration errors early.

### By Inline Format String

When you do not need a pre-registered message, you can pass a format string directly to the
`message` method. The format string is not parsed immediately when you call `message`. Instead,
parsing is deferred until the message is actually needed for formatting. This means that for
an immediate `format()` call, parsing happens right away as part of that call. But when you
use `formatSupplier()` or `formattedExceptionSupplier()`, parsing does not happen until the
supplier's `get()` method is invoked.

```java
String text = messageSupport
    .message("Welcome back, %{name}!")
    .with("name", "Bob")
    .format();
// "Welcome back, Bob!"
```

This approach is convenient for one-off messages or for situations where messages are
constructed dynamically.

### By Pre-Parsed Message Object

If you have already parsed a `Message` object, you can pass it directly to the `message`
method. This avoids parsing the format string again and is useful when the same message is
formatted repeatedly with different parameters.

```java
MessageFactory factory = messageSupport
    .getMessageAccessor()
    .getMessageFactory();
Message msg = factory
    .parseMessage("Hello %{who}!");

String text = messageSupport
    .message(msg)
    .with("who", "World")
    .format();
// "Hello World!"
```


## Setting Parameters

The `MessageConfigurer` provides several `with` methods for binding parameter values. All of
them return the configurer itself, so calls can be chained fluently.

### Individual Parameters

The most common pattern is setting parameters one at a time with `.with(name, value)`. The
library provides overloaded methods for all Java primitive types (`boolean`, `byte`, `char`,
`short`, `int`, `long`, `float`, `double`) as well as `Object`, so you never need to box
values manually.

```java
String text = messageSupport
    .message("%{product} costs %{price} and has %{inStock,true:'in stock',false:'sold out'}.")
    .with("product", "Widget")
    .with("price", 29.95)
    .with("inStock", true)
    .format();
// "Widget costs 29.95 and has in stock."
```

### Bulk Parameters from a Map

If your parameter values are already collected in a `Map<String,Object>`, you can pass the
entire map in a single call. Since `Properties` extends `Map`, this works with `Properties`
objects as well.

```java
var params = Map.of(
    "city", "Amsterdam",
    "temp", 18
);

String text = messageSupport
    .message("It is %{temp}\u00b0C in %{city}.")
    .with(params)
    .format();
// "It is 18°C in Amsterdam."
```

### Removing and Clearing Parameters

The configurer is mutable. You can remove a single parameter by name or clear all parameters
at once. This is useful when you reuse a configurer for multiple formatting calls with
slightly different parameter sets.

```java
var configurer = messageSupport
    .message("%{greeting} %{name}!")
    .with("greeting", "Hello")
    .with("name", "Alice");

configurer.format();
// "Hello Alice!"

configurer
    .remove("name")
    .with("name", "Bob")
    .format();
// "Hello Bob!"

configurer.clear();
configurer
    .with("greeting", "Goodbye")
    .with("name", "Charlie")
    .format();
// "Goodbye Charlie!"
```


## Choosing a Locale

The locale controls locale-sensitive formatting such as number grouping, date patterns and
string comparisons in map keys. You can set the locale using the `locale` method, which
accepts either a `Locale` object or a language tag string.

```java
String text = messageSupport
    .message("%{amount}")
    .with("amount", 1_234_567.89)
    .locale(Locale.GERMANY)
    .format();
// "1.234.567,89"
```

```java
String text = messageSupport
    .message("%{date,date:long}")
    .with("date", LocalDate.of(2026, 5, 1))
    .locale("fr-FR")
    .format();
// "1 mai 2026"
```

If you do not set a locale, the default locale configured on the `MessageSupport` instance
is used. You can also pass `null` to explicitly reset to the default locale.


## Producing the Formatted Result

### Immediate Formatting

The simplest way to get the formatted text is to call `format()`. It evaluates all parameters
and returns the result as a `String` immediately.

```java
String text = messageSupport
    .message("%{n,0:'no messages',1:'1 message',:'%{n} messages'}")
    .with("n", 5)
    .format();
// "5 messages"
```

### Deferred Formatting with a Supplier

Sometimes you want to delay the actual formatting until the result is needed. The
`formatSupplier()` method returns a `Supplier<String>` that captures the current parameter
values and locale at the time of the call. The formatting itself does not happen until
`Supplier.get()` is invoked.

When the message was created from an inline format string via `message(String)`, deferred
formatting also means deferred parsing. The format string is only parsed the first time the
supplier is evaluated. This matters for performance-sensitive code paths where the formatted
result may never be needed at all.

This is particularly useful for logging frameworks where the message should only be formatted
if the log level is active, or for lazy evaluation scenarios.

```java
var configurer = messageSupport
    .message("%{d,date:medium}")
    .with("d", LocalDate.of(2023, 6, 15))
    .locale(Locale.GERMANY);

Supplier<String> supplier = configurer.formatSupplier();

// Formatting happens here, not when formatSupplier() was called
String text = supplier.get();
// "15.06.2023"
```

An important detail is that the supplier captures a snapshot of the parameters and locale.
If you change the configurer after obtaining the supplier, the supplier still uses the values
that were set when it was created.

```java
var configurer = messageSupport
    .message("Hello %{name}!")
    .with("name", "Alice");

Supplier<String> first = configurer.formatSupplier();

configurer.with("name", "Bob");
Supplier<String> second = configurer.formatSupplier();

first.get();
// "Hello Alice!"

second.get();
// "Hello Bob!"
```


## Creating Exceptions with Formatted Messages

A common pattern in Java applications is to throw exceptions with descriptive messages. The
message configurer provides dedicated methods that format the message and pass the result
directly to an exception constructor. This avoids the need to format the message into a
temporary variable before constructing the exception.

### Immediate Exception

The `formattedException` method formats the message and passes the result to the constructor
function you provide. You can use a method reference or a lambda expression for any exception
type.

```java
throw messageSupport
    .message("User %{userId} not found in tenant %{tenant}.")
    .with("userId", userId)
    .with("tenant", tenantName)
    .formattedException(IllegalArgumentException::new);
// throws IllegalArgumentException("User 42 not found in tenant ACME.")
```

If the exception needs a root cause, use the overload that accepts a `Throwable`.

```java
try {
    connectToDatabase();
} catch(SQLException ex) {
    throw messageSupport
        .message("Failed to connect to %{host}:%{port}")
        .with("host", dbHost)
        .with("port", dbPort)
        .formattedException(RuntimeException::new, ex);
    // throws RuntimeException("Failed to connect to db.local:5432", ex)
}
```

### Deferred Exception with a Supplier

The `formattedExceptionSupplier` method returns a `Supplier` that creates the exception
lazily. This is useful in combination with `Optional.orElseThrow` and similar methods where
you only want to construct the exception if the value is actually absent.

```java
User user = findUserById(userId)
    .orElseThrow(messageSupport
        .message("No user with id %{id}")
        .with("id", userId)
        .formattedExceptionSupplier(IllegalStateException::new));
// throws IllegalStateException("No user with id 42") only if the Optional is empty
```

Just like `formatSupplier`, the exception supplier captures a snapshot of the current
parameters and locale. The message is only formatted (and, for inline format strings, only
parsed) when `Supplier.get()` is invoked.

```java
Supplier<IOException> supplier = messageSupport
    .message("Timeout after %{seconds}s on %{endpoint}")
    .with("seconds", timeout)
    .with("endpoint", url)
    .formattedExceptionSupplier(IOException::new);

// Much later, when the exception is actually needed
OptionalInt.empty().orElseThrow(supplier);
// throws IOException("Timeout after 30s on /api/data")
```


## Accessing the Message and Parameters

The configurer also provides read access to the message it is working with and to the
parameters that have been set so far. The `getMessage()` method returns the `Message` object,
and `getParameters()` returns an unmodifiable snapshot of the current parameter map.

This can be useful for debugging or when you need to inspect the configurer's state
programmatically.

```java
var configurer = messageSupport
    .code("welcome")
    .with("name", "Alice")
    .with("role", "admin");

Message.WithCode msg = configurer.getMessage();
// the Message object registered under code "welcome"

Map<String, Object> params = configurer.getParameters();
// {name=Alice, role=admin}
```

The map returned by `getParameters()` is a snapshot. Subsequent changes to the configurer
are not reflected in a previously obtained map.
