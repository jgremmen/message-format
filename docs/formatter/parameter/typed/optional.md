# Optional

This formatter is included in the `DefaultFormatterService`.

The library provides four formatters that handle Java's optional types: `Optional`, `OptionalInt`, `OptionalLong`
and `OptionalDouble`. Each formatter is automatically selected based on the parameter's type. The core principle
is the same for all four: if the optional contains a value, it is unwrapped and formatting is delegated to the
formatter appropriate for the contained value's type. If the optional is empty, it is treated as an empty value.

Because the contained value is delegated to another formatter, the output and map key behavior depends entirely
on what is inside the optional. An `OptionalInt` containing `42` behaves the same as passing `42` directly. An
`Optional` containing a `String` behaves the same as passing that string directly.


## OptionalFormatter

The `OptionalFormatter` handles `java.util.Optional` values. When the optional is present, the contained object
is unwrapped and formatted according to its own type. When the optional is empty, the formatter produces an empty
string.

```java
messageSupport
    .message("%{name}")
    .with("name", Optional.of("Alice"))
    .format();
// "Alice"
```

```java
messageSupport
    .message("%{name}")
    .with("name", Optional.empty())
    .format();
// ""
```

Because formatting is delegated to the contained value's formatter, any map keys or configuration that apply to
the contained type work transparently through the optional wrapper.

```java
messageSupport
    .message("%{count,>0:'positive',0:'zero'}")
    .with("count", Optional.of(5))
    .format();
// "positive"
```

```java
messageSupport
    .message("%{active,true:'yes',false:'no'}")
    .with("active", Optional.of(true))
    .format();
// "yes"
```

### Map Key Behavior

Map key comparisons for `bool`, `number` and `string` keys are delegated to the contained value. When the
optional is empty, these key types never match.

The `null` key matches when the parameter value is `null` or when the optional is empty. The `empty` key also
matches when the optional is empty. When both are present, the `null` key takes precedence because it is
evaluated first.

```java
messageSupport
    .message("%{opt,null:'nothing'}")
    .with("opt", Optional.empty())
    .format();
// "nothing"
```

```java
messageSupport
    .message("%{opt,empty:'nothing'}")
    .with("opt", Optional.empty())
    .format();
// "nothing"
```

```java
messageSupport
    .message("%{opt,empty:empty,null:null}")
    .with("opt", Optional.empty())
    .format();
// "null"
```

```java
messageSupport
    .message("%{opt,empty:empty,null:null}")
    .with("opt", null)
    .format();
// "null"
```

### Size Delegation

The `OptionalFormatter` supports size queries. When the optional is present, the size query is delegated to the
contained value. When the optional is empty, no size is available.


## OptionalIntFormatter

The `OptionalIntFormatter` handles `java.util.OptionalInt` values. When the optional is present, the contained
`int` value is unwrapped and formatting is delegated to the integer formatter. When the optional is empty, the
formatter produces an empty string.

```java
messageSupport
    .message("%{count}")
    .with("count", OptionalInt.of(42))
    .format();
// "42"
```

```java
messageSupport
    .message("%{count}")
    .with("count", OptionalInt.empty())
    .format();
// ""
```

All number map keys work against the contained integer value.

```java
messageSupport
    .message("%{n,0:'none',1:'one',>1:'many'}")
    .with("n", OptionalInt.of(3))
    .format();
// "many"
```

The `empty` key matches when the optional has no value.

```java
messageSupport
    .message("%{n,empty:'unknown',>0:'positive'}")
    .with("n", OptionalInt.empty())
    .format();
// "unknown"
```


## OptionalLongFormatter

The `OptionalLongFormatter` handles `java.util.OptionalLong` values. When the optional is present, the contained
`long` value is unwrapped and formatting is delegated to the long formatter. When the optional is empty, the
formatter produces an empty string.

```java
messageSupport
    .message("%{id}")
    .with("id", OptionalLong.of(9876543210L))
    .format();
// "9876543210"
```

```java
messageSupport
    .message("%{id}")
    .with("id", OptionalLong.empty())
    .format();
// ""
```

All number map keys work against the contained long value.

```java
messageSupport
    .message("%{bytes,0:'empty file',>0:'has content'}")
    .with("bytes", OptionalLong.of(1024))
    .format();
// "has content"
```

The `empty` key matches when the optional has no value.

```java
messageSupport
    .message("%{bytes,empty:'size unknown'}")
    .with("bytes", OptionalLong.empty())
    .format();
// "size unknown"
```


## OptionalDoubleFormatter

The `OptionalDoubleFormatter` handles `java.util.OptionalDouble` values. When the optional is present, the
contained `double` value is unwrapped and formatting is delegated to the double formatter. When the optional is
empty, the formatter produces an empty string.

```java
messageSupport
    .message("%{rate}")
    .with("rate", OptionalDouble.of(3.14))
    .format();
// "3.14"
```

```java
messageSupport
    .message("%{rate}")
    .with("rate", OptionalDouble.empty())
    .format();
// ""
```

All number map keys work against the contained double value.

```java
messageSupport
    .message("%{score,>=90:'excellent',>=70:'good',<70:'needs work'}")
    .with("score", OptionalDouble.of(85.5))
    .format();
// "good"
```

The `empty` key matches when the optional has no value.

```java
messageSupport
    .message("%{score,empty:'not graded'}")
    .with("score", OptionalDouble.empty())
    .format();
// "not graded"
```


## Null Handling

For all four formatters, a `null` parameter value (as opposed to an empty optional) produces an empty string by
default. You can provide a `null` map key to handle this case explicitly. An empty optional also matches the
`null` key.

```java
messageSupport
    .message("%{value,null:'not provided'}")
    .with("value", (Optional<?>) null)
    .format();
// "not provided"
```

```java
messageSupport
    .message("%{count,null:'not provided'}")
    .with("count", (OptionalInt) null)
    .format();
// "not provided"
```
