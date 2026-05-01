# Exceptions

All exceptions thrown by the message format library extend a common base class. They are
unchecked exceptions, so you are not forced to catch them, but the hierarchy makes it easy to
handle specific error categories when needed.


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

`MessageException` is the base class for all exceptions in the library. It extends
`RuntimeException` and does not add any behavior of its own. You can catch this type to
handle any error originating from the message format library in a single place.


## MessageFormatException

`MessageFormatException` is thrown when something goes wrong while formatting a message or
template. It carries optional context about exactly what was being formatted when the error
occurred:

- **code** – the message code, if the message has one.
- **template** – the template name, if the error occurred inside a template.
- **locale** – the locale that was in effect.
- **parameter** – the parameter name that was being formatted.

The exception message is assembled automatically from whichever of these fields are available,
producing messages such as

```
failed to format parameter 'price' for message with code 'order.summary' and locale English (United Kingdom)
```

As the exception propagates up through nested messages and templates, each layer can enrich it
with additional context using the `withCode`, `withTemplate`, `withLocale` and `withParameter`
methods.


## MessageParserException

`MessageParserException` is thrown when a message or template cannot be parsed. In addition to
the same code, template and locale context fields found on `MessageFormatException`, it
provides:

- **errorMessage** – a human-readable description of the parsing error.
- **syntaxError** – a visual representation that shows exactly where in the input the error
  occurred, similar to a compiler error with a caret pointing at the problematic position.

Together these fields produce detailed diagnostic output that makes it straightforward to
locate and fix syntax errors. The `withCode`, `withTemplate`, `withLocale` and `withType`
methods allow the exception to be enriched with context as it propagates.


## DuplicateMessageException

`DuplicateMessageException` is thrown by the default message filter when a message with the
same code is published more than once. The `getCode()` method returns the duplicate code. You
can change this behavior by registering a custom `MessageFilter` via
`ConfigurableMessageSupport.setMessageFilter()`.


## DuplicateTemplateException

`DuplicateTemplateException` is thrown by the default template filter when a template with the
same name is published more than once. The `getName()` method returns the duplicate name. As
with messages, you can override this behavior by registering a custom `TemplateFilter` via
`ConfigurableMessageSupport.setTemplateFilter()`.


## MessageAdopterException

`MessageAdopterException` is thrown when an error occurs while adopting messages and templates
from an external source. This can happen during classpath scanning for annotated classes,
reading class files, or importing from resource bundles. The original cause is typically
available via `getCause()`.


## FormatterServiceException

`FormatterServiceException` is thrown when a formatter registration violates a constraint. For
example, it is thrown when:

- A formatter registered for `Object` does not implement the `DefaultFormatter` interface.
- A formatter or parameter configuration name does not follow the kebab-case naming convention.
- A formatter name is empty.
- A post formatter with the same name has already been registered.
