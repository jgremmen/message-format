# Bool

This formatter is included in the `DefaultFormatterService`.

The named formatter `bool` is selected explicitly by writing `format:bool` in the message parameter configuration.
Its purpose is to coerce values that are not inherently boolean, such as numbers, strings or optionals, into a
boolean result. If your parameter is already a `Boolean`, `BooleanSupplier` or `AtomicBoolean`, you do not need
this formatter; the [type-based formatter](../typed/boolean.md) handles that automatically.


## Conversion Rules

The named `bool` formatter accepts the following types and applies these conversion rules.

**Numbers** (all primitive numeric types, their boxed equivalents, `BigInteger` and `BigDecimal`) are interpreted
as `false` when the value is zero and `true` when the value is non-zero. The sign of the number does not matter,
only whether it equals zero.

```java
messageSupport
    .message("%{count,format:bool,true:'has items',false:'empty'}")
    .with("count", 0)
    .format();
// "empty"

messageSupport
    .message("%{count,format:bool,true:'has items',false:'empty'}")
    .with("count", -24)
    .format();
// "has items"
```

**Strings** are recognized when they match the literal text `"true"` or `"false"` (case-sensitive). If the string
does not match either literal, the formatter attempts to parse it as a number and applies the same zero-check
rule. A string that is neither a boolean literal nor a valid number cannot be converted to a boolean and is
treated as an empty value.

```java
messageSupport
    .message("%{val,format:bool,true:'yes',false:'no'}")
    .with("val", "true")
    .format();
// "yes"

messageSupport
    .message("%{val,format:bool,true:'yes',false:'no'}")
    .with("val", "0")
    .format();
// "no"

messageSupport
    .message("%{val,format:bool,true:'yes',false:'no',empty:'unknown'}")
    .with("val", "maybe")
    .format();
// "unknown"
```

**Optional**, **OptionalInt** and **OptionalLong** are unwrapped before conversion. If the optional contains a
value, that value is converted according to the rules described above. An empty optional cannot produce a boolean
and is treated as an empty value.

```java
messageSupport
    .message("%{flag,format:bool,true:'present',false:'absent',empty:'missing'}")
    .with("flag", Optional.of(false))
    .format();
// "absent"

messageSupport
    .message("%{flag,format:bool,true:'present',false:'absent',empty:'missing'}")
    .with("flag", OptionalInt.empty())
    .format();
// "missing"

messageSupport
    .message("%{flag,format:bool,true:'present',false:'absent',empty:'missing'}")
    .with("flag", OptionalLong.of(100))
    .format();
// "present"
```

A **null** value is handled separately. If no `null` map key is provided, the formatter outputs an empty string.

```java
messageSupport
    .message("%{status,format:bool,null:'<unknown>',true:'active',false:'inactive'}")
    .with("status", null)
    .format();
// "<unknown>"
```


## Map Key Types

The named `bool` formatter supports four map key types: `bool`, `string`, `null` and `empty`.

The **bool** keys `true` and `false` are the most common way to customize the output. They map the converted
boolean value to custom text.

```java
messageSupport
    .message("%{enabled,format:bool,true:'on',false:'off'}")
    .with("enabled", 1)
    .format();
// "on"
```

**String** keys match against the string representation of the converted boolean value, although this is not
recommended. Use `true` and `false` bool keys instead.

```java
messageSupport
    .message("%{b,format:bool,'false':nein,'true':ja}")
    .with("b", "true")
    .format();
// "ja"
```

The **null** key matches when the parameter value is `null`.


## The `empty` Map Key

Because the named `bool` formatter coerces non-boolean types, the conversion can fail. When it does, the value
is considered empty. This is the key distinction from the type-based formatters, where the value is always a
valid boolean and the `empty` key never matches.

For example, a string that is neither a boolean literal nor a valid number cannot be converted to a boolean.
The `empty` key catches these cases. Its negated form `!empty` matches any value that was successfully converted.

```java
messageSupport
    .message("%{b,format:bool,empty:'invalid input',!empty:'valid input'}")
    .with("b", "hello world")
    .format();
// "invalid input"

messageSupport
    .message("%{b,format:bool,empty:'invalid input',!empty:'valid input'}")
    .with("b", "42")
    .format();
// "valid input"
```


## Default Output

When no map keys are provided, the named `bool` formatter outputs the text `true` or `false` after conversion.
For `null` values and for values that cannot be converted to a boolean, the output is an empty string.

```java
messageSupport
    .message("%{count,format:bool}")
    .with("count", 7)
    .format();
// "true"

messageSupport
    .message("%{count,format:bool}")
    .with("count", 0)
    .format();
// "false"
```


## Practical Example

A common use case is presenting a user-facing label derived from a numeric or string value. The following example
shows how to build a status message that covers all possible states including null and unconvertible values.

```java
messageSupport
    .message("Access: %{granted,format:bool,null:'pending',true:'granted',false:'denied',empty:'error'}")
    .with("granted", 1)
    .format();
// "Access: granted"

messageSupport
    .message("Access: %{granted,format:bool,null:'pending',true:'granted',false:'denied',empty:'error'}")
    .with("granted", null)
    .format();
// "Access: pending"

messageSupport
    .message("Access: %{granted,format:bool,null:'pending',true:'granted',false:'denied',empty:'error'}")
    .with("granted", "invalid")
    .format();
// "Access: error"
```
