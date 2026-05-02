# Exceptions

All exceptions thrown by the message format library are unchecked and extend a single common
base class, `MessageException`. You are never forced to catch them, but the hierarchy is designed
so that you can handle broad categories of errors with a single catch clause or drill down to a
specific exception type when you need precise control.


## Exception Hierarchy

```
RuntimeException
└── MessageException
    ├── MessageFormatException
    ├── MessageParserException
    ├── DuplicateMessageException
    ├── DuplicateTemplateException
    ├── MessageAdopterException
    └── FormatterServiceException
```


## MessageException

`MessageException` is the root of the hierarchy. It extends `RuntimeException` directly and does
not add any behavior beyond the standard constructors for a message string and an optional
cause. Catching `MessageException` is a convenient way to handle every error that originates from
the library in one place:

```java
try {
  messageSupport
      .code("order-summary")
      .with("total", amount)
      .format();
} catch(MessageException ex) {
  log.error("Message formatting failed", ex);
}
```


## MessageFormatException

A `MessageFormatException` is thrown when something goes wrong while formatting a message or
template at runtime. Typical causes include a parameter formatter throwing an unexpected
exception or a template reference that cannot be resolved.

The exception carries optional context fields that describe exactly what was being formatted when
the error occurred: the message code, the template name, the locale in effect and the parameter
name being formatted. Whichever of these fields are available are assembled into a detailed
diagnostic message automatically. For example, if a parameter called `price` fails while
formatting a code-based message in a British English locale, the message reads:

```
failed to format parameter 'price' for message with code 'order.summary' and locale English (United Kingdom)
```

When a message format string uses nested templates, the error initially occurs deep inside the
template. As the exception propagates outward, each layer enriches it with its own context using
the `withCode`, `withTemplate`, `withLocale` and `withParameter` methods. Each of these methods
returns a new exception instance with the additional context field set, leaving the original
instance unmodified:

```java
try {
  // ...
} catch(MessageFormatException ex) {
  // Enrich with the code of the outer message before re-throwing
  throw ex.withCode("order-summary");
}
```

The static factory method `MessageFormatException.of(Exception)` wraps any exception in a
`MessageFormatException` if it is not already one. If the argument is already a
`MessageFormatException`, it is returned as-is.

The individual context fields can be retrieved through `getCode()`, `getTemplate()`,
`getLocale()` and `getParameter()`, all of which return `null` when the corresponding piece of
context is not available.


## MessageParserException

A `MessageParserException` is thrown when a message or template format string cannot be parsed.
This happens at the point where message format strings are converted into `Message` objects,
either explicitly through the `MessageFactory` or implicitly when an adopter processes a message
text.

In addition to the code, template and locale context fields (which work the same way as on
`MessageFormatException`), the parser exception provides two properties that help pinpoint the
error. The `getErrorMessage()` method returns a human-readable description of what went wrong,
such as an unexpected token or a missing closing brace. The `getSyntaxError()` method returns a
visual representation of the input showing exactly where the error occurred, similar to what a
compiler would produce with a caret pointing at the problematic position.

Together, the `getMessage()` output combines all available context, the error description and the
visual syntax error into a single string. For instance, attempting to parse the malformed format
string `%{8}` as a message with code `MSG-003` and locale `zh-TW` produces:

```
failed to parse message with code 'MSG-003' for locale Chinese (Taiwan): parameter name ...
%{8}
  ^
```

The `withCode`, `withTemplate`, `withLocale` and `withType` methods allow the exception to be
enriched as it propagates. The `withType` method accepts a `MessageParserException.Type` enum
value of either `MESSAGE` or `TEMPLATE`, which determines whether the diagnostic message reads
"failed to parse message" or "failed to parse template."


## DuplicateMessageException

A `DuplicateMessageException` is thrown when a message with the same code is published to a
`ConfigurableMessageSupport` that already contains a different message under that code. The
`getCode()` method returns the duplicate code.

The default behavior is to reject duplicates with different content while silently ignoring
duplicates that are identical. This default is implemented by the built-in message filter. If
your application needs different behavior, for example allowing overwrites or logging a warning
instead of throwing, you can register a custom `MessageFilter`:

```java
// Allow overwrites: always accept the new message
messageSupport.setMessageFilter(message -> true);
```


## DuplicateTemplateException

`DuplicateTemplateException` works the same way as `DuplicateMessageException` but for
templates. It is thrown when a template with the same name is published and the existing template
has different content. The `getName()` method returns the duplicate template name.

As with messages, the behavior can be changed by registering a custom `TemplateFilter`:

```java
// Allow overwrites: always accept the new template
messageSupport.setTemplateFilter((name, template) -> true);
```


## MessageAdopterException

A `MessageAdopterException` signals that an error occurred while an adopter was reading messages
or templates from an external source. This can happen during classpath scanning for annotated
classes, when reading individual class files, when loading resource bundles, or when importing
from pack files. The underlying I/O or resolution error is typically available through
`getCause()`.


## FormatterServiceException

A `FormatterServiceException` is thrown when registering a formatter violates a constraint
enforced by the `GenericFormatterService`. This includes situations such as registering a
formatter for `Object.class` that does not implement the `DefaultFormatter` interface,
registering a formatter whose name does not conform to kebab-case, providing an empty formatter
name, registering a parameter configuration name that conflicts with an existing auto-apply
formatter, or registering a post formatter whose name has already been taken.
