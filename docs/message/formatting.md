---
toc_depth: 2
---

# Formatting Messages

A [`MessageSupport`](../configuration/message-support.md) instance provides a fluent API to format messages. The entry 
point is either `code(...)` for messages that have been published with a message code, or `message(...)` for inline 
format strings and pre-parsed `Message` objects. Both methods return a `MessageConfigurer` that allows parameter values
to be set, a locale to be chosen and produce the final formatted text.

This page covers the full lifecycle of formatting a message: starting with the configurer, setting parameters, choosing 
a locale, producing the formatted result and creating exceptions with formatted messages.


## Starting a Message Configurer

There are three ways to obtain a `MessageConfigurer` from a `MessageSupport` instance.

### By Message Code

If a message has been published to the message support (for example through annotations or by calling `addMessage`), it
can be referenced by its code. The `code` method looks up the message and returns a configurer for it.

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

If the code does not exist, an `IllegalArgumentException` is thrown. This makes it easy to catch configuration errors
early.

### By Inline Format String

When a pre-registered message is not needed, a format string can be passed directly to the `message` method. The format
string is not parsed immediately when `message` is called. Instead, parsing is deferred until the message is actually 
needed for formatting. This means that for an immediate `format()` call, parsing happens right away as part of that 
call. But when `formatSupplier()` or `formattedExceptionSupplier()` is used, parsing does not happen until the
supplier's `get()` method is invoked.

```java
String text = messageSupport
    .message("Welcome back, %{name}!")
    .with("name", "Bob")
    .format();
// "Welcome back, Bob!"
```

This approach is convenient for one-off messages or for situations where messages are constructed dynamically.

### By Pre-Parsed Message Object

An already parsed `Message` object can be passed directly to the `message` method. This avoids parsing the format
string again and is useful when the same message is formatted repeatedly with different parameters.

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

The `MessageConfigurer` provides several `with` methods for binding parameter values. All of them return the configurer
itself, so calls can be chained fluently.

### Individual Parameters

The most common pattern is setting parameters one at a time with `.with(name, value)`. The library provides overloaded
methods for all Java primitive types (`boolean`, `byte`, `char`, `short`, `int`, `long`, `float`, `double`) as well as 
`Object`, so values never need to be boxed manually. Setting a parameter that already has a value replaces it, and
`null` is a valid value.

The parameter name must follow the kebab-case or lower camelCase convention described on the
[Syntax](syntax.md#parameters) page. A `null` name throws a `NullPointerException`, and an empty or non-conforming name
throws an `IllegalArgumentException`.

```java
String text = messageSupport
    .message("""
        %{product} costs %{price} and has %{inStock,\
            true:'in stock',\
            false:'sold out'}.\
        """)
    .with("product", "Widget")
    .with("price", 29.95)
    .with("inStock", true)
    .format();
// "Widget costs 29.95 and has in stock."
```

### Bulk Parameters from a Map

When parameter values are already collected in a `Map<String,?>`, the entire map can be passed in a single call. Each
entry is set as if `with(name, value)` had been called for it. A separate overload accepts a `Properties` object, whose
keys must be strings.

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

The configurer is mutable. A single parameter can be removed by name or all parameters cleared at once. This is useful 
when a configurer is reused for multiple formatting calls with slightly different parameter sets. Removing a name that
has no value has no effect.

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

/// warning | Configurers Are Not Thread-Safe
A `MessageConfigurer` represents a single formatting operation and must not be shared between threads. Each call to
`code(...)` or `message(...)` returns a new, independent configurer. The `MessageSupport` it was obtained from can be
shared freely.
///


## Choosing a Locale

The locale controls locale-sensitive formatting such as number grouping, date patterns and string comparisons in map 
keys. The locale is set using the `locale` method, which accepts either a `Locale` object or a language tag string.

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

If no locale is set, the default locale configured on the `MessageSupport` instance is used. The configurer reads
this default when it is created, so a later call to `setLocale(...)` on the message support does not affect existing
configurers. Passing `null` explicitly resets the configurer to the current default locale. The `locale(String)`
overload converts the language tag with `Locale.forLanguageTag(String)`.


## Producing the Formatted Result

### Immediate Formatting

The simplest way to get the formatted text is to call `format()`. It evaluates all parameters and returns the result as
a `String` immediately.

```java
String text = messageSupport
    .message("You have %{n,0:'no',1:'one'} new messages.")
    .with("n", 5)
    .format();
// "You have 5 new messages."
```

### Deferred Formatting with a Supplier

Sometimes the actual formatting should be delayed until the result is needed. The `formatSupplier()` method returns a 
`Supplier<String>` that captures the current parameter values and locale at the time of the call. The formatting itself 
does not happen until `Supplier.get()` is invoked.

When the message was created from an inline format string via `message(String)`, deferred formatting also means 
deferred parsing. The format string is only parsed the first time the supplier is evaluated. This matters for 
performance-sensitive code paths where the formatted result may never be needed at all.

This is particularly useful for logging frameworks where the message should only be formatted if the log level is 
active, or for lazy evaluation scenarios. The supplier formats the message on the first call to `get()` and returns the
same string on every later call.

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

An important detail is that the supplier captures a snapshot of the parameters and locale. If the configurer is changed
after obtaining the supplier, the supplier still uses the values that were set when it was created.

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

A common pattern in Java applications is to throw exceptions with descriptive messages. The message configurer provides 
dedicated methods that format the message and pass the result directly to an exception constructor. This avoids the 
need to format the message into a temporary variable before constructing the exception.

### Immediate Exception

The `formattedException` method formats the message and passes the result to the constructor function provided. The
function is either an `ExceptionConstructor`, which receives the formatted message, or an
`ExceptionConstructorWithCause`, which also receives a cause. Exceptions thrown by the function are passed on to the
caller.

```java
int userId = 42;
String tenantName = "ACME";

throw messageSupport
    .message("User %{userId} not found in tenant %{tenant}.")
    .with("userId", userId)
    .with("tenant", tenantName)
    .formattedException(msg -> new IllegalArgumentException(msg));
// throws IllegalArgumentException("User 42 not found in tenant ACME.")
```

/// warning | Ambiguous Constructor References
Without a cause argument, `formattedException` and `formattedExceptionSupplier` each have two overloads, one for each
functional interface. For an exception class that has a `(String)` and a `(String, Throwable)` constructor, a
constructor reference such as `IllegalArgumentException::new` matches both overloads and does not compile. Use a
lambda expression with one parameter, as shown above, to select `ExceptionConstructor`. A constructor reference works
when the cause is passed as well, as in the next example.
///

If the exception needs a root cause, use the overload that accepts a `Throwable`.

```java
String dbHost = "db.local";
int dbPort = 5432;

try {
    DriverManager.getConnection(
        "jdbc:postgresql://" + dbHost + ":" + dbPort + "/shop");
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

The `formattedExceptionSupplier` method returns a `Supplier` that creates the exception lazily. This is useful in 
combination with `Optional.orElseThrow` and similar methods where the exception should only be constructed if the value 
is actually absent.

```java
Map<Integer,String> users = Map.of(7, "Alice");
int userId = 42;

String user = Optional
    .ofNullable(users.get(userId))
    .orElseThrow(messageSupport
        .message("No user with id %{id}")
        .with("id", userId)
        .formattedExceptionSupplier(
            msg -> new IllegalStateException(msg)));
// throws IllegalStateException("No user with id 42") only if the
// Optional is empty
```

Just like `formatSupplier`, the exception supplier captures a snapshot of the current parameters and locale. The
message is only formatted (and, for inline format strings, only parsed) when `Supplier.get()` is invoked.

```java
int timeout = 30;
String url = "/api/data";

Supplier<IOException> supplier = messageSupport
    .message("Timeout after %{seconds}s on %{endpoint}")
    .with("seconds", timeout)
    .with("endpoint", url)
    .formattedExceptionSupplier(msg -> new IOException(msg));

// Much later, when the exception is actually needed
OptionalInt.empty().orElseThrow(supplier);
// throws IOException("Timeout after 30s on /api/data")
```


## Accessing the Message and Parameters

The configurer also provides read access to the message it is working with and to the parameters that have been set so 
far. The `getMessage()` method returns the `Message` object and `getParameters()` returns the parameter map of the
configurer. For a configurer created from an inline format string, `getMessage()` parses the string if it has not
been parsed yet.

This can be useful for debugging or when the configurer's state needs to be inspected programmatically.

```java
messageSupport.addMessage("welcome", "Welcome %{name} (%{role})!");

var configurer = messageSupport
    .code("welcome")
    .with("name", "Alice")
    .with("role", "admin");

Message.WithCode msg = configurer.getMessage();
// the Message object registered under code "welcome"

Map<String,Object> params = configurer.getParameters();
// {name=Alice, role=admin}
```

The map returned by `getParameters()` is a live view backed by the configurer. Parameters set or removed later are
visible in a previously obtained map. To keep the current state, copy the map, for example with `Map.copyOf(...)`.
The map is intended for reading only. Parameters are changed through the `with`, `remove` and `clear` methods of
the configurer.

```java
Map<String,Object> view = configurer.getParameters();

configurer.with("lang", "en");

view.containsKey("lang");
// true
```
