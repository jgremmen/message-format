---
icon: material/code-json
---

# Parameter Formatters

Every time a parameter reference like `%{name}` appears in a message, the library needs a
parameter formatter to convert the parameter value into text. A parameter formatter receives
the value, any configuration keys and map entries declared in the message, and the current
locale. It produces a text fragment that replaces the parameter reference in the formatted
output.

All parameter formatters implement the `ParameterFormatter` interface. The library ships with
a comprehensive set of built-in formatters that cover common Java types such as numbers,
strings, booleans, dates, collections and maps. It also provides formatters for more
specialized scenarios like value selection, size queries and bitmask interpretation.

Parameter formatters come in two varieties: type-specific formatters and named formatters.
Both implement the same base interface, but they differ in how the library selects them.


## Type-Specific Formatters

A type-specific formatter is bound to one or more Java types. The library selects it
automatically based on the runtime type of the parameter value. When you pass a `double`
value into a message, the library walks the type hierarchy, finds the `NumberFormatter`
registered for numeric types, and uses it to produce the text. No special syntax is required
in the message format string.

```java
messageSupport
    .message("Total: %{amount}")
    .with("amount", 49.95)
    .format();
// "Total: 49.95"
```

For a full explanation of how type-based selection works, including the type hierarchy walk,
formatter ordering and delegation, see the [Type-Specific Formatters](typed/index.md) page.


## Named Formatters

A named formatter is not tied to a particular Java type. Instead, it is registered under a
unique name and selected explicitly in the message format string using the `format`
configuration key. This is useful when the way a value should be presented has nothing to do
with its Java type. You might want to interpret a number as a boolean, compute the size of a
collection, or select one of several messages based on a value without formatting the value
itself.

```java
messageSupport
    .message("%{errorCount,format:bool,true:'has errors',false:'no errors'}")
    .with("errorCount", 3)
    .format();
// "has errors"
```

In this example, `errorCount` is an integer, but instead of formatting it as a number the
message explicitly requests the `bool` formatter. The `bool` formatter interprets any non-zero
number as `true` and matches the corresponding map entry.

For a full explanation of named formatters, how they are selected, and the built-in named
formatters available, see the [Named Formatters](named/index.md) page.
