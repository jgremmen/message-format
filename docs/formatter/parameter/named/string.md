# String

This formatter is included in the `DefaultFormatterService`.

The named formatter `string` is selected explicitly by writing `format:string` in the message parameter configuration.
Its purpose is to force string conversion on any value, regardless of its Java type. If the parameter is already a
`CharSequence` (including `String`) or `char[]`, this formatter is not needed; the
[type-based formatter](../typed/string.md) handles that automatically.

Both are the same `StringFormatter`. The map keys, the `ignore-default-tostring` configuration and the handling of
`null` values described on the [String](../typed/string.md) page of the type-based formatter therefore apply
unchanged. This page covers what changes when the formatter is selected by name.


## Conversion Rules

The named `string` formatter converts any non-null value to its string representation using `String.valueOf(value)`.
For `CharSequence` and `char[]` values this is their text content; for all other types it is the result of `toString()`.
The formatter that would otherwise be selected for the type of the value is bypassed, together with its configuration
keys and its interpretation of map keys.

The examples on this page use the following message support.

```java title="Example setup"
import de.sayayi.lib.message.MessageSupportFactory;

import java.lang.annotation.RetentionPolicy;
import java.util.Locale;

var messageSupport = MessageSupportFactory.createDefault();
// all built-in formatters are available
```

An enum constant is formatted by the enum formatter, which supports the `enum` configuration key. With `format:string`
the constant is converted with `toString()` instead, so the configuration key has no effect:

```java
messageSupport
    .message("%{policy,enum:ordinal} / " +
        "%{policy,format:string,enum:ordinal}")
    .with("policy", RetentionPolicy.RUNTIME)
    .format();
// "2 / RUNTIME"
```

A number is formatted by the number formatter with locale-specific grouping. The `string` formatter outputs the result
of `toString()`, which does not depend on the locale:

```java
messageSupport
    .message("%{total} / %{total,format:string}")
    .with("total", 1234567.5)
    .locale(Locale.US)
    .format();
// "1,234,567.5 / 1234567.5"
```


## Map Keys

String keys are compared with the string representation of the value, as described for the
[type-based formatter](../typed/string.md#string-keys). Bool keys, number keys and the default entry are not
consulted, so a number selected with `format:string` is matched by its text only. The number key `200` matches for
the number formatter, but not for the `string` formatter, which requires the string key `'200'`:

```java
messageSupport
    .message("%{code,200:'OK'} / %{code,format:string,200:'OK'} / " +
        "%{code,format:string,'200':'OK'}")
    .with("code", 200)
    .format();
// "OK / 200 / OK"
```

To fall back to a fixed text for values that match none of the keys, the [`choice`](choice.md) formatter is selected
instead, because only selecting formatters use the default entry.


## Practical Example

A common use case is normalizing a status field that may arrive as different types (enum, string, number) into a
user-facing label. String keys match the string representation of the value. Values without a matching key are
output unchanged.

```java
messageSupport
    .message("""
        Status: %{code,format:string,\
            '200':'OK',\
            '404':'Not Found',\
            '500':'Server Error'}\
        """)
    .with("code", 200)
    .format();
// "Status: OK"

messageSupport
    .message("""
        Status: %{code,format:string,\
            '200':'OK',\
            '404':'Not Found',\
            '500':'Server Error'}\
        """)
    .with("code", 418)
    .format();
// "Status: 418"

messageSupport
    .message("""
        Status: %{code,format:string,\
            null:'No status received',\
            '200':'OK'}\
        """)
    .with("code", null)
    .format();
// "Status: No status received"
```

A fixed label for unexpected codes requires a default entry, which the [`choice`](choice.md) formatter provides.

```java
messageSupport
    .message("""
        Status: %{code,format:choice,\
            '200':'OK',\
            '404':'Not Found',\
            :'Unexpected'}\
        """)
    .with("code", 418)
    .format();
// "Status: Unexpected"
```
