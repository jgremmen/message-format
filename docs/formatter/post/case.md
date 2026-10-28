# Case

The `case` post formatter converts formatted text to uppercase or lowercase. The conversion is
locale-aware, so it respects locale-specific casing rules such as the Turkish dotted and
dotless I distinction.


## Uppercase

Setting the `case` configuration key to `upper` or `uppercase` converts all characters in the
text to their uppercase equivalent.

```java
messageSupport
    .message("%(case,'%{name}',case:upper)")
    .with("name", "alice")
    .locale(Locale.US)
    .format();
// "ALICE"
```

```java
messageSupport
    .message("%(case,'Hello %{who}!',case:uppercase)")
    .with("who", "world")
    .locale(Locale.US)
    .format();
// "HELLO WORLD!"
```


## Lowercase

Setting the `case` configuration key to `lower` or `lowercase` converts all characters in the
text to their lowercase equivalent.

```java
messageSupport
    .message("%(case,'%{label}',case:lower)")
    .with("label", "WARNING")
    .locale(Locale.US)
    .format();
// "warning"
```

```java
messageSupport
    .message("%(case,'%{status}',case:lowercase)")
    .with("status", "CRITICAL ERROR")
    .locale(Locale.US)
    .format();
// "critical error"
```


## Unrecognized Values

If the `case` configuration key contains a value that is not one of the four recognized
strings, the text is returned unchanged. The same applies when the `case` key is absent
entirely.

```java
messageSupport
    .message("%(case,'%{text}',case:title)")
    .with("text", "hello world")
    .locale(Locale.US)
    .format();
// "hello world"
```


## Locale Sensitivity

Because the conversion delegates to `String.toUpperCase(Locale)` and
`String.toLowerCase(Locale)`, it produces correct results for locales with special casing
rules. In Turkish, for example, the lowercase letter `i` uppercases to `İ` (capital I with a
dot above) rather than the ASCII `I`.

```java
messageSupport
    .message("%(case,'bilgi',case:upper)")
    .locale(Locale.of("tr", "TR"))
    .format();
// "BİLGİ"

messageSupport
    .message("%(case,'bilgi',case:upper)")
    .locale(Locale.US)
    .format();
// "BILGI"
```


The locale is the one used for the current formatting call, as set with `locale(...)` on the message configurer, or
the default locale of the message support if none was set.

/// note | Migration from 0.24
Up to version 0.24.0, post formatters always received the default locale of the message support, even if a different
locale had been set on the message configurer. Messages formatted with an explicit locale may now produce different
case conversions, for example for Turkish.
///


## Configuration Reference

| Key    | Type   | Default | Description                                               |
|--------|--------|---------|-----------------------------------------------------------|
| `case` | string |         | Target case: `upper`, `uppercase`, `lower` or `lowercase` |
