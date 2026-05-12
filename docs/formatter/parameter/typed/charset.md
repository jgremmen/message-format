# Charset

This formatter is included in the `DefaultFormatterService`.

The `CharsetFormatter` is a type-based formatter registered for `java.nio.charset.Charset`. It is automatically
selected whenever a parameter value is a `Charset` instance. When formatting, the formatter first consults the
parameter's string map keys, comparing each key against the charset's canonical name and aliases. If a map key
matches, the corresponding mapped message is used. If no key matches and a default map entry is present, the
default is used. Otherwise, the charset's locale-sensitive display name as returned by
`Charset.displayName(Locale)` is the final fallback.

```java
messageSupport
    .message("Encoding: %{cs}")
    .with("cs", StandardCharsets.UTF_8)
    .format();
// "Encoding: UTF-8"
```

```java
messageSupport
    .message("The file uses %{encoding}")
    .with("encoding", Charset.forName("ISO-8859-11"))
    .format();
// "The file uses x-iso-8859-11"
```


## Map Keys

String map keys are matched against the charset value using the `MapKeyComparator` implementation provided by
this formatter. The comparator checks the charset's canonical name first and then its aliases. Only the equality
(`=` or implicit) and not-equal (`!` or `<>`) operators are supported for charset comparison. Other relational
operators such as `<`, `<=`, `>` and `>=` are not handled by this formatter.

### Canonical Name Matching

The charset's canonical name is the primary comparison target. When a string map key equals the canonical name,
the match is considered exact.

```java
messageSupport
    .message("%{cs,'UTF-8':'8-bit Unicode Transformation Format'}")
    .with("cs", StandardCharsets.UTF_8)
    .format();
// "8-bit Unicode Transformation Format"
```

Multiple charsets can be mapped in the same parameter. The formatter selects the entry whose key matches the
parameter value.

```java
messageSupport
    .message("%{cs,'UTF-8':'Unicode','ISO-8859-1':'Latin','US-ASCII':'ASCII',:'unknown'}")
    .with("cs", StandardCharsets.ISO_8859_1)
    .format();
// "Latin"
```

### Alias Matching

Every charset has a set of aliases in addition to its canonical name. `ISO-8859-1`, for example, is also known as
`latin1`, `iso-ir-100` and `IBM819`, among others. The formatter accepts any of these aliases as a string map key.
This is useful when a message should reference a charset by a well-known shorthand rather than by its canonical
IANA name.

```java
messageSupport
    .message("%{cs,'latin1':'Western European'}")
    .with("cs", StandardCharsets.ISO_8859_1)
    .format();
// "Western European"
```

An alias match is ranked lower than a canonical name match. When both the canonical name and an alias appear as
map keys for the same parameter, the canonical name entry is always selected.

```java
messageSupport
    .message("%{cs,'ISO-8859-1':'by canonical name','latin1':'by alias'}")
    .with("cs", StandardCharsets.ISO_8859_1)
    .format();
// "by canonical name"
```

### Not-Equal Operator

The not-equal operator (`!` or `<>`) inverts the matching logic. A not-equal string key matches when the charset
is not identified by that key. If neither the canonical name nor any alias equals the key string, the not-equal
condition is fully satisfied. If only an alias matches the key, the not-equal condition is leniently satisfied,
which means it can still be outranked by a stronger match. If the canonical name equals the key, the not-equal
condition fails entirely.

This makes it possible to provide a mapped message for all charsets except a specific one.

```java
messageSupport
    .message("%{cs,!'UTF-8':'not UTF-8',:'is UTF-8'}")
    .with("cs", StandardCharsets.ISO_8859_1)
    .format();
// "not UTF-8"

messageSupport
    .message("%{cs,!'UTF-8':'not UTF-8',:'is UTF-8'}")
    .with("cs", StandardCharsets.UTF_8)
    .format();
// "is UTF-8"
```

When multiple not-equal keys are present, the formatter selects the entry with the strongest match. Consider a
parameter with two not-equal keys targeting different charsets.

```java
messageSupport
    .message("%{cs,!'UTF-8':'not UTF-8',!'latin1':'not latin1',:'default'}")
    .with("cs", StandardCharsets.UTF_8)
    .format();
// "not latin1"
```

In this example UTF-8 is excluded by the first key, so that entry fails. The second key `!'latin1'` succeeds
because UTF-8 is not `latin1` in any way, so `"not latin1"` is selected.

### Default and Display Name Fallback

When neither the canonical name nor any alias matches a map key, the formatter checks for a default map entry.
If a default is provided, its message is used.

```java
messageSupport
    .message("%{cs,'UTF-16':'wide','UTF-32':'very wide',:'unknown encoding'}")
    .with("cs", StandardCharsets.UTF_8)
    .format();
// "unknown encoding"
```

Without a default entry the charset's display name is used as the final fallback. The display name is
locale-sensitive and determined by `Charset.displayName(Locale)`.

```java
messageSupport
    .message("%{cs,'UTF-16':'wide','UTF-32':'very wide'}")
    .with("cs", StandardCharsets.UTF_8)
    .format();
// "UTF-8"
```


## Null Handling

A `null` parameter value produces an empty string by default. A `null` map key can be used to provide specific
text for this case.

```java
messageSupport
    .message("%{cs,null:'no encoding specified'}")
    .with("cs", null)
    .format();
// "no encoding specified"
```
