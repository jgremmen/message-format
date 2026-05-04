---
icon: material/alphabet-latin
---

# Named Formatters

When a parameter appears in a message, the library selects a formatter based on the Java type
of the parameter value. An `int` is handled by the number formatter, a `LocalDate` by the
temporal formatter, a `List` by the iterable formatter, and so on. This automatic type-based
selection covers the majority of cases, but sometimes the way a value should be presented has
nothing to do with its Java type. You might want to treat a number as a boolean, determine
the size of a collection, or select one of several messages based on a value without
formatting the value itself. That is where named formatters come in.

A named formatter implements the `NamedParameterFormatter` interface and is registered under
a unique name. It is selected explicitly in the message format string using the `format`
configuration key with the syntax `format:<name>`.


## Differences from Type-Specific Formatters

Type-specific formatters are bound to one or more Java types. They return a non-empty set
from `getFormattableTypes()` and the library matches them to parameter values by walking the
class hierarchy until it finds a registered formatter. This happens automatically and requires
no special syntax in the message.

Named formatters implement `NamedParameterFormatter`, which extends `ParameterFormatter` and
adds two key characteristics. The `getName()` method returns the formatter's name used to
reference it in a message, and `getFormattableTypes()` returns an empty set by default, so
the formatter is never selected automatically. It is only activated when the message
explicitly requests it through `format:<name>`.

This allows named formatters to interpret a value in a way that has nothing to do with its
type. The `bool` formatter, for example, accepts numbers, strings, optionals and booleans
and interprets all of them as a boolean. The `size` formatter computes the value's size rather
than formatting the value itself. Purely type-based selection could not achieve this without
registering the formatter for every supported type, potentially conflicting with other
formatters.

Some named formatters are also registered as type-specific formatters. The `bool` formatter
is the type-specific formatter for `Boolean` and `boolean` values, so it is selected
automatically for those types. But when you pass an `int` and want it interpreted as a
boolean, you must write `format:bool` to override the number formatter that would otherwise
be selected.


## Selecting a Named Formatter

To select a named formatter, add `format:<name>` to the parameter configuration. The
following example forces the `bool` formatter on an integer value. Without the explicit
`format:bool`, the library would use the number formatter instead, and the `true` and `false`
map keys would never match because the number formatter does not interpret integers as
booleans.

```java
messageSupport
    .message("%{errorCount,format:bool,true:'has errors',false:'no errors'}")
    .with("errorCount", 3)
    .format();
// "has errors"
```

When the library encounters `format:bool`, it looks up the named formatter registered under
the name `bool` and delegates formatting to it. The `bool` formatter converts the integer `3`
to `true` (any non-zero number is considered `true`) and then matches the `true` map key to
produce the text "has errors".


## Type Compatibility

A named formatter can optionally restrict which types it accepts by overriding the
`canFormat(Class<?>)` method. The default implementation returns `true` for all types, meaning
any named formatter accepts any value by default. When `canFormat` returns `false` for the
value's type, the library falls back to the regular type-based formatter selection instead of
using the named formatter.

The `bool` formatter, for example, accepts `Boolean`, all numeric primitives, `String`,
`Optional`, `OptionalInt`, `OptionalLong` and `null`. If you were to pass a `List` with
`format:bool`, the `bool` formatter would decline because `canFormat(List.class)` returns
`false`, and the library would fall back to the default formatter for lists.


## Showcase

The following examples demonstrate a few of the built-in named formatters to give you an idea
of what is possible. Each named formatter has its own dedicated documentation page with full
details and configuration options.

### Boolean Interpretation

The `bool` formatter converts a wide range of input types to a boolean value and maps it to
custom text. Numbers are interpreted as `false` when zero and `true` otherwise. Strings
`"true"` and `"false"` are recognized literally, and numeric strings are parsed and treated
as numbers.

```java
messageSupport
    .message("Feature flag: %{flag,format:bool,true:'enabled',false:'disabled'}")
    .with("flag", "true")
    .format();
// "Feature flag: enabled"
```

```java
messageSupport
    .message("Signal: %{level,format:bool,true:'HIGH',false:'LOW'}")
    .with("level", 0)
    .format();
// "Signal: LOW"
```

### Value-Based Selection

The `choice` formatter selects one of several mapped messages based on the parameter value.
It supports all map key types (null, empty, bool, number and string) and picks the best
matching entry. Unlike the type-specific formatters, it does not format the value itself. It
is purely a selector.

```java
messageSupport
    .message("%{status,format:choice,'active':'running','paused':'on hold',:'unknown'}")
    .with("status", "paused")
    .format();
// "on hold"
```

A common use of the `choice` formatter is pluralization, where the same parameter appears
twice in the message: once for the number and once to select the right word form.

```java
messageSupport
    .message("%{n} %{n,format:choice,1:'item',:'items'} remaining")
    .with("n", 1)
    .format();
// "1 item remaining"

messageSupport
    .message("%{n} %{n,format:choice,1:'item',:'items'} remaining")
    .with("n", 5)
    .format();
// "5 items remaining"
```

### Size Queries

The `size` formatter determines the size of a value by delegating to `SizeQueryable`
formatters registered for the value's type. It can compute the length of a string, the number
of elements in a collection or array, the number of entries in a map, and similar
measurements. The resulting size can be mapped to custom text using number map keys.

```java
messageSupport
    .message("%{names,format:size,0:'nobody',1:'one person',:'%{names,format:size} people'}")
    .with("names", List.of("Alice", "Bob", "Charlie"))
    .format();
// "3 people"
```

```java
messageSupport
    .message("Password strength: %{pw,format:size,<8:'too short',<12:'acceptable',:'strong'}")
    .with("pw", "s3cret!")
    .format();
// "Password strength: too short"
```

### Forced String Conversion

The `string` formatter converts any value to its string representation. It also serves as the
default formatter for types that have no specific formatter registered. Explicitly selecting
it with `format:string` is useful when you want to force string conversion on a value that
would otherwise be handled by a different type-specific formatter.

```java
messageSupport
    .message("Raw: %{obj,format:string}")
    .with("obj", 3.14)
    .format();
// "Raw: 3.14"
```


## Reference

| Format Name  | Formatter Class       | Documentation               |
|--------------|-----------------------|-----------------------------|
| `bitmask`    | `BitmaskFormatter`    | [Bitmask](bitmask.md)       |
| `bool`       | `BoolFormatter`       | [Bool](bool.md)             |
| `choice`     | `ChoiceFormatter`     | [Choice](choice.md)         |
| `classifier` | `ClassifierFormatter` | [Classifier](classifier.md) |
| `geo`        | `GeoFormatter`        | [Geo](geo.md)               |
| `size`       | `SizeFormatter`       | [Size](size.md)             |
| `spel`       | `SpELFormatter`       | [SpEL](spel.md)             |
| `string`     | `StringFormatter`     | [String](string.md)         |
