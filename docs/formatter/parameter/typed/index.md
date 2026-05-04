---
icon: material/alpha-t-circle-outline
---

# Type-Specific Formatters

When a parameter value is inserted into a message, the library needs to decide how to convert
that value into text. It does this by looking at the Java type of the value and finding a
formatter that has been registered for that type. This is called type-specific formatting
because the selection is driven entirely by the value's class, without any explicit instruction
in the message format string.

Every type-specific formatter implements the `ParameterFormatter` interface and returns one or
more `FormattableType` entries from its `getFormattableTypes()` method. Each `FormattableType`
pairs a Java class with an order number. When the formatter is registered with the formatter
service, these pairings tell the library which value types the formatter can handle and in
what priority relative to other formatters for the same type.

Consider a simple example. Passing an `int` value to a message parameter causes the library
to find the `NumberFormatter`, which is registered for `int.class` (among other numeric types).
No format directive is needed in the message itself.

```java
messageSupport
    .message("You have %{count} new notifications.")
    .with("count", 7)
    .format();
// "You have 7 new notifications."
```

Passing a `LocalDate` causes the library to find the `TemporalFormatter`, which is registered
for `Temporal.class`. Since `LocalDate` implements `Temporal`, it is matched through the type
hierarchy walk described below.

```java
messageSupport
    .message("Report date: %{date}")
    .with("date", LocalDate.of(2026, 5, 6))
    .locale(Locale.US)
    .format();
// "Report date: May 6, 2026"
```


## Type Hierarchy Walk

The library does not require a formatter to be registered for the exact runtime class of a
value. Instead, it walks the value's type hierarchy, collecting the class itself, all its
superclasses and all implemented interfaces, and then looks up registered formatters for each
of those types. This means a formatter registered for `Number.class` will be found when the
value is an `Integer`, a `BigDecimal` or any other `Number` subclass.

The walk proceeds upward from the runtime type through `getSuperclass()` and recursively
through `getInterfaces()`, stopping at `Object`. The result is a set of candidate types.
For each candidate that has at least one registered formatter, those formatters are collected
into a single list, sorted by order, and used in sequence.

For primitive types and primitive array types the library automatically maps them to their
wrapper equivalents if no formatter is registered for the primitive directly. A `boolean`
value, for example, is mapped to `Boolean` if no formatter has been registered specifically
for `boolean.class`. In practice the built-in formatters do register for both the primitive
and the wrapper class, but this fallback ensures custom setups work without surprises.

Object arrays whose component type is not `Object` (for example `String[]`) additionally
include `Object[]` in the set of candidate types. This allows the generic `ArrayFormatter`,
which is registered for `Object[].class`, to handle any object array type without requiring
a dedicated registration for every possible array element type.


## Formatter Order

Because the type hierarchy walk can collect formatters from multiple levels of the class
hierarchy, and because multiple formatters can be registered for the same type, the library
needs a way to decide which formatter takes precedence. This is where the order attribute of
`FormattableType` comes in.

Each `FormattableType` carries an order value in the range 0 through 127. A lower number
means higher priority. When the library resolves the formatter list for a given value type,
all candidate formatters are sorted by their order, and the formatter with the lowest order
is tried first. If two formatters have the same order, the library falls back to sorting by
the formatter's fully qualified class name to keep the behavior deterministic.

The library defines three default order levels. Most formatters for class and interface types
use the `DEFAULT_ORDER` of 80. Formatters for primitive types and array types use the
`DEFAULT_PRIMITIVE_OR_ARRAY_ORDER` of 100. The `Object` type is fixed at order 127 and serves
as the ultimate fallback. These defaults are applied automatically when a `FormattableType`
is constructed without an explicit order value, based on whether the type is a primitive, an
array or a regular class.

This ordering scheme has a practical consequence. When you pass an `Integer` value, the
library finds two formatters through the hierarchy walk: `NumberFormatter` registered for
`int.class` at order 100 and `NumberFormatter` registered for `Number.class` at order 80.
After sorting, the `Number.class` registration (order 80) comes first. If the value were a
primitive `int` instead of a boxed `Integer`, the `int.class` registration at order 100 would
also be a direct match, but the `Number.class` entry at order 80 still wins because its order
is lower.

Custom formatters can choose any order value from 0 to 126 (127 is reserved for `Object`).
A formatter that needs to take priority over the built-in formatters for a given type should
use an order lower than 80. A formatter that should serve as a fallback behind the built-ins
should use an order higher than 100 but below 127.


## Delegation Between Formatters

The order-based list of candidate formatters is not just used to pick a single winner. A
formatter can explicitly hand off formatting to the next formatter in the list by calling
`context.delegateToNextFormatter()`. This mechanism allows multiple formatters to coexist for
the same type, each handling a different aspect or configuration scenario.

A concrete example of this is `byte[]`. Two formatters are registered for `byte[]`:
`ByteArrayFormatter` at order 90 and `ArrayFormatter` at order 100. The `ByteArrayFormatter`
checks whether the `bytes` configuration key is present. If it is, the formatter encodes or
decodes the byte array accordingly. If the key is absent, the formatter calls
`context.delegateToNextFormatter()`, which passes control to the `ArrayFormatter`. The
`ArrayFormatter` then formats the byte array as a list of its individual elements.

```java
messageSupport
    .message("Encoded: %{data,bytes:base64}")
    .with("data", new byte[] { 72, 101, 108, 108, 111 })
    .format();
// "Encoded: SGVsbG8="
```

In this example the `bytes:base64` configuration key is present, so `ByteArrayFormatter`
handles the formatting and produces a Base64-encoded string. Without the `bytes` key, the
same byte array would be formatted as a list:

```java
messageSupport
    .message("Raw bytes: %{data}")
    .with("data", new byte[] { 72, 101, 108, 108, 111 })
    .format();
// "Raw bytes: 72, 101, 108, 108, 111"
```

Here `ByteArrayFormatter` is invoked first (order 90), but since the `bytes` configuration
key is absent, it delegates to the next formatter. `ArrayFormatter` (order 100) takes over
and formats each byte as a number, separated by commas.

The last formatter in the chain is always the one registered for `Object`, which by default
is the `StringFormatter`. This formatter must never delegate because there is no formatter
beyond it. Attempting to do so throws a `NoSuchElementException`.


## The Default Formatter

The `StringFormatter` is registered as the default formatter for the `Object` type at order
127. It implements the `DefaultFormatter` marker interface, which is required for any
formatter registered directly against `Object.class`. This formatter acts as the catch-all
at the bottom of every formatter chain. When no other formatter handles a value, the
`StringFormatter` converts it to text using `String.valueOf()`.

Because the `Object` type is always included in the candidate set during the type hierarchy
walk, the `StringFormatter` guarantees that every value can be formatted regardless of its
type. Even if you pass an instance of a custom class that implements no recognized interfaces
and extends no known superclass, the `StringFormatter` will produce a text representation.

```java
record Gadget(String name) {}

messageSupport
    .message("Device: %{device}")
    .with("device", new Gadget("sensor"))
    .format();
// "Device: Gadget[name=sensor]"
```


## Showcase

The following examples illustrate how formatter ordering and the hierarchy walk interact
in situations where multiple formatters are candidates for the same value.

### Legacy Date Objects

The `ToTemporalDelegate` formatter is registered for `Calendar`, `Date`, `FileTime` and
`InstantSource`, all at the default order of 80. When you pass a `java.util.Date`, this
formatter converts it to an `Instant` and then delegates formatting to the `TemporalFormatter`
by calling `context.format(instant, Instant.class)`. The result is that legacy date/time
objects are formatted using the same temporal formatting logic as modern `java.time` types.

```java
messageSupport
    .message("Timestamp: %{ts,date:medium}")
    .with("ts", new java.util.Date(1746489600000L))
    .locale(Locale.US)
    .format();
// "Timestamp: May 6, 2025"
```

### Enum Values

When you pass an enum value, the library finds the `EnumFormatter` registered for `Enum.class`
at order 80. The `EnumFormatter` outputs the enum constant's `name()` by default and supports
map keys to select text based on the constant's name or ordinal.

```java
messageSupport
    .message("Status: %{status}")
    .with("status", Thread.State.RUNNABLE)
    .format();
// "Status: RUNNABLE"
```

### Collections and Arrays

An `ArrayList<String>` is matched through the type hierarchy walk. `ArrayList` extends
`AbstractList`, which extends `AbstractCollection`, which implements `Collection`, which
extends `Iterable`. The `IterableFormatter` is registered for `Iterable.class` at order 80,
so it is found and used.

```java
messageSupport
    .message("Winners: %{names,list-sep-last:' and '}")
    .with("names", List.of("Alice", "Bob", "Charlie"))
    .format();
// "Winners: Alice, Bob and Charlie"
```

A `String[]` array works similarly. The `ArrayFormatter` is registered for `Object[]` at
order 100. Because `String[]` is not `Object[]` directly, the library adds `Object[]` to the
candidate set as part of the hierarchy walk for non-`Object` component type arrays.

```java
messageSupport
    .message("Tags: %{tags}")
    .with("tags", new String[] { "java", "format", "library" })
    .format();
// "Tags: java, format, library"
```

### Map and Dictionary

A `HashMap` implements `Map`, for which the `MapFormatter` is registered at order 75.
For a regular `HashMap` there is only one candidate formatter and it handles the value
directly.

```java
messageSupport
    .message("Settings: %{cfg}")
    .with("cfg", Map.of("color", "blue", "size", "large"))
    .format();
// "Settings: color=blue, size=large"
```

The situation becomes more interesting with `Hashtable`. `Hashtable` extends `Dictionary`
and also implements `Map`, so both the `DictionaryFormatter` (order 70) and the
`MapFormatter` (order 75) are candidates. The `DictionaryFormatter` wins because its order
is lower, but it only handles the value when the `key` configuration entry is present. The
`key` entry tells it which dictionary key to look up. When that configuration is absent, the
`DictionaryFormatter` calls `context.delegateToNextFormatter()`, passing control to the
`MapFormatter`, which then formats the `Hashtable` as a regular map of key-value pairs.

```java
var table = new java.util.Hashtable<String,Integer>();
table.put("x", 10);
table.put("y", 20);

messageSupport
    .message("Coords: %{coords}")
    .with("coords", table)
    .format();
// "Coords: x=10, y=20"
```

In this example no `key` configuration is provided, so the `DictionaryFormatter` delegates
and the `MapFormatter` produces the output. When the `key` entry is present, the
`DictionaryFormatter` handles it itself by looking up the specified key in the dictionary
and formatting the resulting value.

```java
var table = new java.util.Hashtable<String,Integer>();
table.put("x", 10);
table.put("y", 20);

messageSupport
    .message("X coordinate: %{coords,key:'x'}")
    .with("coords", table)
    .format();
// "X coordinate: 10"
```


## Reference

| Java Type Simple Name  | Formatter Class                    | Order | Documentation               |
|------------------------|------------------------------------|-------|-----------------------------|
| `AtomicBoolean`        | `AtomicBooleanFormatter`           | 80    | [Boolean](boolean.md)       |
| `AtomicIntegerArray`   | `ArrayFormatter`                   | 80    | [List](iterable.md)         |
| `AtomicLongArray`      | `ArrayFormatter`                   | 80    | [List](iterable.md)         |
| `AtomicReferenceArray` | `ArrayFormatter`                   | 80    | [List](iterable.md)         |
| `BitSet`               | `BitSetFormatter`                  | 80    | [BitSet](bit-set.md)        |
| `boolean[]`            | `ArrayFormatter`                   | 100   | [List](iterable.md)         |
| `Boolean`              | `BoolFormatter`                    | 80    | [Boolean](boolean.md)       |
| `boolean`              | `BoolFormatter`                    | 100   | [Boolean](boolean.md)       |
| `BooleanSupplier`      | `BooleanSupplierFormatter`         | 80    | [Supplier](supplier.md)     |
| `byte[]`               | `ByteArrayFormatter`               | 90    | [Byte Array](byte-array.md) |
| `byte[]`               | `ArrayFormatter`                   | 100   | [List](iterable.md)         |
| `byte`                 | `NumberFormatter`                  | 100   | [Number](number.md)         |
| `Calendar`             | `ToTemporalDelegate`               | 80    | [Temporal](temporal.md)     |
| `char[]`               | `StringFormatter`[^1]              | 100   | [String](string.md)         |
| `CharSequence`         | `StringFormatter`[^1]              | 80    | [String](string.md)         |
| `Charset`              | `CharsetFormatter`                 | 80    | [Charset](charset.md)       |
| `Date`                 | `ToTemporalDelegate`               | 80    | [Temporal](temporal.md)     |
| `Dictionary`           | `DictionaryFormatter`              | 70    | [Dictionary](dictionary.md) |
| `double[]`             | `ArrayFormatter`                   | 100   | [List](iterable.md)         |
| `double`               | `NumberFormatter`                  | 100   | [Number](number.md)         |
| `DoubleSupplier`       | `DoubleSupplierFormatter`          | 80    | [Supplier](supplier.md)     |
| `Enum`                 | `EnumFormatter`                    | 80    | [Enum](enum.md)             |
| `FileTime`             | `ToTemporalDelegate`               | 80    | [Temporal](temporal.md)     |
| `float[]`              | `ArrayFormatter`                   | 100   | [List](iterable.md)         |
| `float`                | `NumberFormatter`                  | 100   | [Number](number.md)         |
| `InstantSource`        | `ToTemporalDelegate`               | 80    | [Temporal](temporal.md)     |
| `int[]`                | `ArrayFormatter`                   | 100   | [List](iterable.md)         |
| `int`                  | `NumberFormatter`                  | 100   | [Number](number.md)         |
| `IntSupplier`          | `IntSupplierFormatter`             | 80    | [Supplier](supplier.md)     |
| `Iterable`             | `IterableFormatter`                | 80    | [List](iterable.md)         |
| `Locale`               | `LocaleFormatter`                  | 80    | [Locale](locale.md)         |
| `long[]`               | `ArrayFormatter`                   | 100   | [List](iterable.md)         |
| `long`                 | `NumberFormatter`                  | 100   | [Number](number.md)         |
| `LongSupplier`         | `LongSupplierFormatter`            | 80    | [Supplier](supplier.md)     |
| `Map.Entry`            | `MapEntryFormatter`                | 80    | [Map](map.md)               |
| `Map`                  | `MapFormatter`                     | 75    | [Map](map.md)               |
| `Number`               | `NumberFormatter`                  | 80    | [Number](number.md)         |
| `Object[]`             | `ArrayFormatter`                   | 100   | [List](iterable.md)         |
| `Object`               | _registered default formatter_[^1] | 127   |                             |
| `Optional`             | `OptionalFormatter`                | 80    | [Optional](optional.md)     |
| `OptionalDouble`       | `OptionalDoubleFormatter`          | 80    | [Optional](optional.md)     |
| `OptionalInt`          | `OptionalIntFormatter`             | 80    | [Optional](optional.md)     |
| `OptionalLong`         | `OptionalLongFormatter`            | 80    | [Optional](optional.md)     |
| `Path`                 | `PathFormatter`                    | 80    | [Path](path.md)             |
| `Reference`            | `ReferenceFormatter`               | 80    | [Reference](reference.md)   |
| `short[]`              | `ArrayFormatter`                   | 100   | [List](iterable.md)         |
| `short`                | `NumberFormatter`                  | 100   | [Number](number.md)         |
| `Supplier`             | `SupplierFormatter`                | 80    | [Supplier](supplier.md)     |
| `Temporal`             | `TemporalFormatter`                | 80    | [Temporal](temporal.md)     |
| `Throwable`            | `ThrowableFormatter`               | 80    | [Throwable](throwable.md)   |
| `TimeZone`             | `TimeZoneFormatter`                | 80    | [TimeZone](time-zone.md)    |
| `Type`                 | `TypeFormatter`                    | 80    | [Type](type.md)             |
| `URI`                  | `URIFormatter`                     | 80    | [URI](uri.md)               |
| `URL`                  | `URLFormatter`                     | 80    | [URL](url.md)               |

[^1]: This formatter implements the `DefaultFormatter` interface and can be used to format any object which is not
handled by any other registered formatter.
