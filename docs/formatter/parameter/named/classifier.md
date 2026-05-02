# Classifier

/// note
This formatter is **not** included in the `DefaultFormatterService`. You must register it explicitly
by adding a `ClassifierFormatter` instance to your formatter service.
///

The named formatter `classifier` is selected explicitly by writing `format:classifier` in the message parameter
configuration. It inspects the parameter value and determines one or more classifiers that describe the nature
of the value. These classifiers are type-based labels such as `null`, `bool`, `number`, `string`, `enum`, `list`
or `temporal`. The formatter then iterates over the classifiers and matches each one against the string map keys
in the parameter configuration. The message associated with the first matching key is output. If no classifier
matches any key, the default map entry is used. If no default is present either, the `null` representation
(typically an empty string) is returned.

This formatter is useful for branching message output based on the runtime type of a parameter value, without
having to know the exact Java class.


## Classifiers

Each value type produces one or more classifiers. The type-based formatter registered for the value's Java type
is responsible for declaring which classifiers apply. When multiple classifiers are present, they are checked
against the map keys in order. The first match wins.

The following table lists the most common classifiers and the types that produce them.

| Classifier   | Produced by                                                                       |
|--------------|-----------------------------------------------------------------------------------|
| `null`       | Any `null` value                                                                  |
| `bool`       | `Boolean`, `AtomicBoolean`, `BooleanSupplier`                                     |
| `number`     | All `Number` types, `IntSupplier`, `DoubleSupplier`, `OptionalInt`, `OptionalLong` |
| `string`     | `CharSequence` (including `String`), `char[]`                                     |
| `enum`       | All `Enum` types                                                                  |
| `list`       | `Iterable`, `Collection`, arrays, `Map`, `BitSet` (in set-bit mode)               |
| `temporal`   | All temporal types (`LocalDate`, `Instant`, `ZonedDateTime`, etc.)                 |

Some formatters also add more specific classifiers before the general ones. For example, a `Locale` value
produces the classifier `locale` first, followed by `string` when a string-producing configuration is active.
A `Path` value produces `path`, a `BitSet` produces `bit-set` (and `list` in set-bit mode), and wrapper types
like `Supplier` and `Reference` produce their own classifier before delegating to the contained value.

```java
messageSupport
    .message("%{v,format:classifier,'number':'a number','string':'text','list':'a list'}")
    .with("v", 42)
    .format();
// "a number"

messageSupport
    .message("%{v,format:classifier,'number':'a number','string':'text','list':'a list'}")
    .with("v", "hello")
    .format();
// "text"

messageSupport
    .message("%{v,format:classifier,'number':'a number','string':'text','list':'a list'}")
    .with("v", List.of(1, 2, 3))
    .format();
// "a list"
```


## Null Values

When the parameter value is `null`, the only classifier is `null`. You can match it with a `'null'` string key.

```java
messageSupport
    .message("%{v,format:classifier,'null':'nothing','number':'a number','string':'text'}")
    .with("v", null)
    .format();
// "nothing"
```


## Classifier Priority

When a value produces multiple classifiers, they are checked in order and the first matching string key wins.
For example, a `Locale` value produces `locale` first and then `string` (when a string-producing configuration
is active). If you provide keys for both, the more specific one matches.

```java
messageSupport
    .message("%{v,format:classifier,'bool':'boolean','enum':'enumeration','string':'text'}")
    .with("v", RetentionPolicy.RUNTIME)
    .format();
// "enumeration"
```

Since `enum` is checked before any general classifier, the `'enum'` key matches even though enum values can
also be represented as strings.


## Unmatched Values

When no classifier matches any string map key and no default key is present, the output is an empty string.

```java
messageSupport
    .message("%{v,format:classifier,'number':'a number'}")
    .with("v", "hello")
    .format();
// ""
```

A default key (`:`) can be used to catch all unmatched values.

```java
messageSupport
    .message("%{v,format:classifier,'number':'numeric',:'other'}")
    .with("v", "hello")
    .format();
// "other"
```


## Practical Example

The `classifier` formatter is well suited for building messages that adapt to the type of a parameter. In the
following example, a diagnostic message describes what kind of value was received. The nested parameter `%{v}`
inside the mapped messages outputs the value itself using its type-based formatter.

```java
messageSupport
    .message("Received %{v,format:classifier,'null':'a null value','bool':'boolean %{v}','number':'number %{v}','string':'text \"%{v}\"','list':'a list of elements'}")
    .with("v", true)
    .format();
// "Received boolean true"

messageSupport
    .message("Received %{v,format:classifier,'null':'a null value','bool':'boolean %{v}','number':'number %{v}','string':'text \"%{v}\"','list':'a list of elements'}")
    .with("v", 3.14)
    .format();
// "Received number 3.14"

messageSupport
    .message("Received %{v,format:classifier,'null':'a null value','bool':'boolean %{v}','number':'number %{v}','string':'text \"%{v}\"','list':'a list of elements'}")
    .with("v", null)
    .format();
// "Received a null value"
```
