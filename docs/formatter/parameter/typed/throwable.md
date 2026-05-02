# Throwable

This formatter is included in the `DefaultFormatterService`.

The `ThrowableFormatter` handles `java.lang.Throwable` and all of its subclasses, including `Exception`,
`RuntimeException`, `Error` and any custom exception types. The formatter is automatically selected whenever a
parameter value is an instance of `Throwable`.

The formatter extracts the throwable's localized message by calling `getLocalizedMessage()` and delegates the
formatting of that string to the string formatter. This means the output is the exception's message text, and
all map keys and behavior that apply to string values work transparently.

```java
messageSupport
    .message("Error: %{ex}")
    .with("ex", new IllegalArgumentException("invalid input"))
    .format();
// "Error: invalid input"
```

```java
messageSupport
    .message("%{error}")
    .with("error", new IOException("file not found"))
    .format();
// "file not found"
```


## Map Keys

Because the throwable's message is delegated to the string formatter, all string map keys work against the
message text. This allows you to match specific error messages or patterns.

```java
messageSupport
    .message("%{ex,'file not found':'missing file',:'unexpected error'}")
    .with("ex", new IOException("file not found"))
    .format();
// "missing file"
```

The `empty` key matches when the throwable's localized message is `null` or empty (some exceptions do not carry
a message).

```java
messageSupport
    .message("%{ex,empty:'no details available'}")
    .with("ex", new RuntimeException())
    .format();
// "no details available"
```


## Null Handling

A `null` parameter value produces an empty string by default. You can provide a `null` map key to handle this
case explicitly.

```java
messageSupport
    .message("%{ex,null:'no error'}")
    .with("ex", (Exception) null)
    .format();
// "no error"
```
