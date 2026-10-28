# String

This formatter is included in the `DefaultFormatterService`.

The `StringFormatter` is a type-based formatter registered for `CharSequence` (which includes `String`) and `char[]`.
It is automatically selected by the library when a parameter holds one of these types. It also serves as the library's
default formatter, the fallback used for any value type that has no specific formatter registered.

By default, the formatter outputs the string representation of the parameter value. For `CharSequence` and `char[]`
values this is their text content; for all other types it is the result of `String.valueOf(value)`.

```java
messageSupport
    .message("%{name}")
    .with("name", "Hello World")
    .format();
// "Hello World"
```

```java
messageSupport
    .message("%{chars}")
    .with("chars", new char[] {'a', 'b', 'c'})
    .format();
// "abc"
```

Because `StringFormatter` is the default formatter, it also handles values of types that have no dedicated formatter.
In that case, `toString()` is called on the value.

```java
record Point(int x, int y) {}

messageSupport
    .message("%{obj}")
    .with("obj", new Point(3, 4))
    .format();
// "Point[x=3, y=4]"
```


## Map Keys

### String Keys

String map keys are compared against the parameter value using a locale-aware `Collator`. The comparison first attempts 
an exact match (identical strength). If that fails, a lenient match ignoring case and accents (primary strength) is 
attempted.

```java
messageSupport
    .message("%{text,'Süd':'exact'}")
    .with("text", "Süd")
    .format();
// "exact"

messageSupport
    .message("%{text,'SUD':'lenient'}")
    .with("text", "Süd")
    .format();
// "lenient"
```

Comparison operators are supported and use the same collator-based comparison.

```java
messageSupport
    .message("%{grade,<'C':'good',>='C':'needs improvement'}")
    .with("grade", "A")
    .format();
// "good"
```

### Empty and Null Keys

The `empty` key matches when the string value is empty or (with an equality comparison) consists only of whitespace. 
The `null` key matches when the parameter value is `null`. A `null` value also matches `empty` since it has no string 
content. The negated forms `!empty` and `!null` are available as well.

```java
messageSupport
    .message("%{text,empty:'nothing',!empty:'something'}")
    .with("text", "")
    .format();
// "nothing"

messageSupport
    .message("%{text,empty:'nothing',!empty:'something'}")
    .with("text", "  ")
    .format();
// "nothing"

messageSupport
    .message("%{text,null:'no value',empty:'blank',!empty:'%{text}!'}")
    .with("text", null)
    .format();
// "no value"

messageSupport
    .message("%{text,!empty:'%{text}!'}")
    .with("text", "hello")
    .format();
// "hello!"
```

### Bool Keys, Number Keys and the Default Entry

Bool keys, number keys and the default entry are not consulted when a string is formatted. A string value is output
unchanged if none of the string, `empty` or `null` keys matches. The [`choice`](../named/choice.md) formatter evaluates
all key types for a string, including the default entry. Bool keys then match the text `"true"` or `"false"` ignoring
case, and number keys match if the string parses as an equal number.

```java
messageSupport
    .message(
        "%{status,'active':'Active','inactive':'Inactive',:'Unknown'}")
    .with("status", "other")
    .format();
// "other"

messageSupport
    .message("""
        %{status,format:choice,\
            'active':'Active',\
            'inactive':'Inactive',\
            :'Unknown'}\
        """)
    .with("status", "other")
    .format();
// "Unknown"

messageSupport
    .message("%{code,format:choice,0:'ok',>0:'error'}")
    .with("code", "0")
    .format();
// "ok"
```


## Null Handling

When the parameter value is `null`, the formatter outputs an empty string by default. A `null` map key can be provided 
to produce specific text.

```java
messageSupport
    .message("%{name}")
    .with("name", null)
    .format();
// ""

messageSupport
    .message("%{name,null:'<unnamed>'}")
    .with("name", null)
    .format();
// "<unnamed>"
```


## The `ignore-default-tostring` Configuration

When the `StringFormatter` is used as the default fallback for arbitrary objects, some objects produce unhelpful
`toString()` output. In particular the default `Object.toString()` identity string (e.g. `java.lang.Object@1a2b3c`)
and lambda class names are rarely useful. The `ignore-default-tostring` configuration option tells the formatter to 
treat such values as empty strings instead.

This is a system-wide default configuration set via `setDefaultConfig`:

```java
messageSupport.setDefaultConfig("ignore-default-tostring", true);

messageSupport
    .message("%{object}")
    .with("object", new Object())
    .format();
// ""

messageSupport
    .message("%{object,empty:'(empty)'}")
    .with("object", new Object())
    .format();
// "(empty)"
```

When this option is not set or set to `false`, the identity string is output as-is.


## Named Formatter `string` (Explicit Selection)

The named formatter `string` is documented on its own page: [String](../named/string.md). It is selected explicitly by
writing `format:string` to force string conversion on any value, regardless of its Java type.
