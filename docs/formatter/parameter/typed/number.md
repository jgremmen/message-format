# Number

This formatter is included in the `DefaultFormatterService`.

When a parameter value is a numeric type, the library automatically selects the `NumberFormatter`. This formatter
is registered for `Number` and all primitive numeric types (`byte`, `short`, `int`, `long`, `float`, `double`),
so it handles `Integer`, `Long`, `BigInteger`, `BigDecimal`, `AtomicInteger`, `AtomicLong`, `LongAdder`,
`LongAccumulator`, and all other `Number` subclasses.

By default, integer types (`BigInteger`, `Long`, `Integer`, `Short`, `Byte`, `AtomicInteger`, `AtomicLong`,
`LongAdder`, `LongAccumulator`) are rendered using their plain `toString()` representation. All other numeric
types (floating point, `BigDecimal`) are formatted using a locale-specific number format.

```java
messageSupport
    .message("%{count}")
    .with("count", 42)
    .format();
// "42"
```

```java
messageSupport
    .message("%{amount}")
    .with("amount", 1234567)
    .format();
// "1234567"
```

```java
messageSupport
    .message("%{price}")
    .with("price", 19.99)
    .locale(Locale.GERMANY)
    .format();
// "19,99"
```

```java
messageSupport
    .message("%{ratio}")
    .with("ratio", 0.1d)
    .locale(Locale.GERMANY)
    .format();
// "0,1"
```


## The `number` Configuration Key

The formatter uses the `number` configuration key to select the output format. The following values are
recognized.

### `integer`

Formats the number as an integer using a locale-specific integer format. For types that are already integer
types, this produces the same output as the default (plain `toString()`). For floating point values, it rounds
to the nearest integer and applies locale-specific grouping.

```java
messageSupport
    .message("%{val,number:integer}")
    .with("val", 3.7)
    .format();
// "4"
```

### `percent`

Formats the number as a percentage using the locale-specific percent format. The value is multiplied by 100
and a percent sign is appended.

```java
messageSupport
    .message("%{progress,number:percent}")
    .with("progress", 0.75)
    .format();
// "75%"
```

### `currency`

Formats the number as a currency value using the locale-specific currency format.

```java
messageSupport
    .message("%{price,number:currency}")
    .with("price", 49.99)
    .locale(Locale.US)
    .format();
// "$49.99"

messageSupport
    .message("%{price,number:currency}")
    .with("price", 49.99)
    .locale(Locale.GERMANY)
    .format();
// "49,99 €"
```

### `bool`

Converts the number to a boolean value and delegates formatting to the boolean formatter. Zero is `false`,
non-zero is `true`.

```java
messageSupport
    .message("%{errors,number:bool,true:'has errors',false:'no errors'}")
    .with("errors", 3)
    .format();
// "has errors"

messageSupport
    .message("%{errors,number:bool,true:'has errors',false:'no errors'}")
    .with("errors", 0)
    .format();
// "no errors"
```

### Custom `DecimalFormat` Pattern

Any value that does not match one of the predefined options is interpreted as a `DecimalFormat` pattern. This
gives you full control over the number format, including digit counts, grouping separators, and decimal places.

```java
messageSupport
    .message("%{pi,number:'#0.00'}")
    .with("pi", Math.PI)
    .locale(Locale.GERMANY)
    .format();
// "3,14"
```

```java
messageSupport
    .message("%{size,number:'#,##0.0'}")
    .with("size", 12345.678)
    .locale(Locale.US)
    .format();
// "12,345.7"
```

The pattern follows the Java `DecimalFormat` syntax. Locale-specific symbols (decimal separator, grouping
separator) are applied automatically based on the formatting locale.


## Map Keys

### Number Keys

Number map keys are compared against the parameter value. When a match is found, the corresponding text is
output instead of the formatted number. Comparison operators are supported.

```java
messageSupport
    .message("%{count,0:'none',1:'one',:'many'}")
    .with("count", 0)
    .format();
// "none"

messageSupport
    .message("%{count,0:'none',1:'one',:'many'}")
    .with("count", 5)
    .format();
// "many"
```

```java
messageSupport
    .message("%{temp,<0:'freezing',0:'zero',>0:'above zero'}")
    .with("temp", -5)
    .format();
// "freezing"
```

### Bool Keys

Bool keys interpret the number as a boolean: zero matches `false`, non-zero matches `true`.

```java
messageSupport
    .message("%{items,true:'has items',false:'empty'}")
    .with("items", 0)
    .format();
// "empty"

messageSupport
    .message("%{items,true:'has items',false:'empty'}")
    .with("items", 7)
    .format();
// "has items"
```

### String Keys

String keys are parsed as numbers and compared against the parameter value. This allows numeric comparison using
quoted strings, which can be useful for values that exceed the range of number map keys.

```java
messageSupport
    .message("%{val,>'100':'large',<='100':'small'}")
    .with("val", 200)
    .format();
// "large"
```

### Null Handling

When the parameter value is `null`, the formatter outputs an empty string by default. You can provide a `null`
map key to produce specific text.

```java
messageSupport
    .message("%{score,null:'N/A'}")
    .with("score", (Integer) null)
    .format();
// "N/A"
```
