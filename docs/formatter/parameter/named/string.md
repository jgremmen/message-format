# String

This formatter is included in the `DefaultFormatterService`.

The named formatter `string` is selected explicitly by writing `format:string` in the message parameter configuration.
Its purpose is to force string conversion on any value, regardless of its Java type. If your parameter is already a
`CharSequence` (including `String`) or `char[]`, you do not need this formatter; the
[type-based formatter](../typed/string.md) handles that automatically.


## Conversion Rules

The named `string` formatter converts any non-null value to its string representation using `String.valueOf(value)`.
For `CharSequence` and `char[]` values this is their text content; for all other types it is the result of
`toString()`.

```java
messageSupport
    .message("%{policy,format:string}")
    .with("policy", RetentionPolicy.RUNTIME)
    .format();
// "RUNTIME"
```

```java
messageSupport
    .message("%{count,format:string}")
    .with("count", 42)
    .format();
// "42"
```

A **null** value is handled separately. If no `null` map key is provided, the formatter outputs an empty string.

```java
messageSupport
    .message("%{name,format:string,null:'<unnamed>'}")
    .with("name", null)
    .format();
// "<unnamed>"
```


## Map Key Types

The named `string` formatter supports the following map key types: `string`, `bool`, `number`, `null`, `empty`
and default.

### String Keys

String map keys are compared against the parameter value using a locale-aware `Collator`. The comparison first
attempts an exact match (identical strength). If that fails, a lenient match ignoring case and accents (primary
strength) is attempted.

```java
messageSupport
    .message("%{text,format:string,'Süd':'exact'}")
    .with("text", "Süd")
    .format();
// "exact"

messageSupport
    .message("%{text,format:string,'SUD':'lenient'}")
    .with("text", "Süd")
    .format();
// "lenient"
```

Comparison operators are supported and use the same collator-based comparison.

```java
messageSupport
    .message("%{grade,format:string,<'C':'good',>='C':'needs improvement'}")
    .with("grade", "A")
    .format();
// "good"
```

### Bool Keys

Bool keys match when the string value equals the literal text `"true"` or `"false"`. The match is
case-insensitive at lenient strength.

```java
messageSupport
    .message("%{val,format:string,true:'yes',false:'no'}")
    .with("val", "true")
    .format();
// "yes"
```

### Number Keys

Number keys parse the string value as a number and compare it against the key value. If the string is not a
valid number, the key does not match.

```java
messageSupport
    .message("%{code,format:string,0:'ok',>0:'error'}")
    .with("code", "0")
    .format();
// "ok"
```

### Empty and Null Keys

The `empty` key matches when the string value is empty or (with an equality comparison) consists only of
whitespace. The `null` key matches when the parameter value is `null`. A `null` value also matches `empty`
since it has no string content. The negated forms `!empty` and `!null` are available as well.

```java
messageSupport
    .message("%{text,format:string,empty:'nothing',!empty:'something'}")
    .with("text", "")
    .format();
// "nothing"

messageSupport
    .message("%{text,format:string,empty:'nothing',!empty:'something'}")
    .with("text", "  ")
    .format();
// "nothing"

messageSupport
    .message("%{text,format:string,null:'no value',empty:'blank',!empty:'%{text}!'}")
    .with("text", null)
    .format();
// "no value"

messageSupport
    .message("%{text,format:string,!empty:'%{text}!'}")
    .with("text", "hello")
    .format();
// "hello!"
```

### Default Key

A default map key (`:`) catches any value that does not match a specific key.

```java
messageSupport
    .message("%{status,format:string,'active':'Active','inactive':'Inactive',:'Unknown'}")
    .with("status", "other")
    .format();
// "Unknown"
```


## The `ignore-default-tostring` Configuration

When the `string` formatter is used for arbitrary objects, some objects produce unhelpful `toString()` output.
In particular the default `Object.toString()` identity string (e.g. `java.lang.Object@1a2b3c`) and lambda class
names are rarely useful. The `ignore-default-tostring` configuration option tells the formatter to treat such
values as empty strings instead.

This is a system-wide default configuration set via `setDefaultConfig`:

```java
messageSupport.setDefaultConfig("ignore-default-tostring", true);

messageSupport
    .message("%{object,format:string}")
    .with("object", new Object())
    .format();
// ""

messageSupport
    .message("%{object,format:string,empty:'(empty)'}")
    .with("object", new Object())
    .format();
// "(empty)"
```

When this option is not set or set to `false`, the identity string is output as-is.


## Default Output

When no map keys are provided, the named `string` formatter outputs the string representation of the value.
For `null` values the output is an empty string.

```java
messageSupport
    .message("%{name,format:string}")
    .with("name", "Hello World")
    .format();
// "Hello World"

messageSupport
    .message("%{name,format:string}")
    .with("name", null)
    .format();
// ""
```


## Practical Example

A common use case is normalizing a status field that may arrive as different types (enum, string, number) into
a user-facing label. The following example shows how to match specific values and provide a fallback.

```java
messageSupport
    .message("Status: %{code,format:string,'200':'OK','404':'Not Found','500':'Server Error',:'Unexpected'}")
    .with("code", 200)
    .format();
// "Status: OK"

messageSupport
    .message("Status: %{code,format:string,'200':'OK','404':'Not Found','500':'Server Error',:'Unexpected'}")
    .with("code", 418)
    .format();
// "Status: Unexpected"

messageSupport
    .message("Status: %{code,format:string,null:'No status received','200':'OK',:'Other'}")
    .with("code", null)
    .format();
// "Status: No status received"
```
