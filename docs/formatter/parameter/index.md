---
icon: material/code-json
---

# Parameter Formatters

Every time a parameter reference like `%{name}` appears in a message, the library needs a parameter formatter to 
convert the parameter value into text. A parameter formatter receives the value, any configuration keys and map entries
declared in the message and the current locale. It produces a text fragment that replaces the parameter reference in 
the formatted output.

All parameter formatters implement the `ParameterFormatter` interface. The library ships with a comprehensive set of 
built-in formatters that cover common Java types such as numbers, strings, booleans, dates, collections and maps. It 
also provides formatters for more specialized scenarios like value selection, size queries and bitmask interpretation.

Parameter formatters come in two varieties: type-specific formatters and named formatters. Both implement the same base 
interface, but they differ in how the library selects them.


## Type-Specific Formatters

A type-specific formatter is bound to one or more Java types. The library selects it automatically based on the runtime 
type of the parameter value. When a `double` value is passed into a message, the library walks the type hierarchy, 
finds the `NumberFormatter` registered for numeric types and uses it to produce the text. No special syntax is required
in the message format string.

The examples on this page use the following message support.

```java title="Example setup"
import de.sayayi.lib.message.MessageSupportFactory;

import java.util.Locale;

var messageSupport = MessageSupportFactory.createDefault();
// all built-in formatters are available
```

```java
messageSupport
    .message("Total: %{amount}")
    .with("amount", 49.95)
    .locale(Locale.US)
    .format();
// "Total: 49.95"
```

For a full explanation of how type-based selection works, including the type hierarchy walk, formatter ordering and 
delegation, see the [Type-Specific Formatters](typed/index.md) page.


## Named Formatters

A named formatter is not tied to a particular Java type. Instead, it is registered under a unique name and selected
explicitly in the message format string using the `format` configuration key. This is useful when the way a value 
should be presented has nothing to do with its Java type. A number might need to be interpreted as a boolean, the size
of a collection computed, or one of several messages selected based on a value without formatting the value itself.

```java
messageSupport
    .message(
        "%{errorCount,format:bool,true:'has errors',false:'no errors'}")
    .with("errorCount", 3)
    .format();
// "has errors"
```

In this example, `errorCount` is an integer, but instead of formatting it as a number the message explicitly requests
the `bool` formatter. The `bool` formatter interprets any non-zero number as `true` and matches the corresponding map 
entry.

For a full explanation of named formatters, how they are selected and the built-in named formatters available, see the
[Named Formatters](named/index.md) page.


## Null Values and Empty Results

All built-in parameter formatters treat `null` values and empty results the same way, unless the page of a formatter
describes a different behavior. A `null` value is never passed to the formatting logic of a formatter. Without a
matching map key, it produces empty text. The `null` key matches a `null` value, and the `empty` key matches it as
well, because a `null` value has no content. The negated keys `!null` and `!empty` match all other values.

```java
messageSupport
    .message("Due: %{dueDate}")
    .with("dueDate", null)
    .format();
// "Due:"

messageSupport
    .message("Due: %{dueDate,null:'not scheduled'}")
    .with("dueDate", null)
    .format();
// "Due: not scheduled"
```

The `null` and `empty` keys are checked twice. Before the value is formatted, they are compared with the value itself.
After formatting, they are compared with the formatted text. A value that formats to empty text therefore matches the
`empty` key, even if the value itself is not empty. The
[string](typed/string.md#the-ignore-default-tostring-configuration) formatter, for example, can turn an object with a
default `toString()` result into empty text, which an `empty` key then replaces.

Formatters that compute something other than the formatted value, such as [`size`](named/size.md) or
[`choice`](named/choice.md), and formatters for container types, such as [optionals](typed/optional.md), refine these
rules. Their pages describe the differences.
