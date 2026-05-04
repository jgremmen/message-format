# Charset

This formatter is included in the `DefaultFormatterService`.

The `CharsetFormatter` is a type-based formatter registered for `java.nio.charset.Charset`. It is automatically
selected whenever a parameter value is a `Charset` instance. The formatter attempts to match the charset against
string map keys by canonical name and aliases, then falls back to a default map entry if present. If none of
these match, the charset's display name as returned by `Charset.displayName(Locale)` is used.

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

String map keys are compared against the charset's canonical name. When the canonical name does not match any
key, the formatter iterates over the charset's aliases and checks each one against the available string map keys.
As soon as a match is found, the corresponding mapped message is used instead of the display name. This means
that a map key can reference either the canonical name or any known alias of the charset.

The canonical name is always checked first. If a map entry exists for the canonical name, alias matching is
skipped entirely. This guarantees a deterministic result when both the canonical name and an alias appear as map
keys for the same parameter.

```java
messageSupport
    .message("%{cs,'UTF-8':'8-bit Unicode Transformation Format'}")
    .with("cs", StandardCharsets.UTF_8)
    .format();
// "8-bit Unicode Transformation Format"
```

### Alias Matching

Every charset has a set of aliases in addition to its canonical name. For example, `ISO-8859-1` is known by
aliases such as `latin1`, `iso-ir-100`, `IBM819` and others. The formatter lets you use any of these aliases
as a string map key.

This is particularly useful when your message format should reference a charset by a well-known shorthand rather
than the canonical IANA name.

```java
messageSupport
    .message("%{cs,'latin1':'Western European'}")
    .with("cs", StandardCharsets.ISO_8859_1)
    .format();
// "Western European"
```

In this example the canonical name of the charset is `ISO-8859-1`, which does not match the map key `latin1`.
The formatter then checks the aliases and finds that `latin1` is a registered alias, so the mapped message is
returned.

When neither the canonical name nor any alias matches a map key, the formatter checks for a default map entry.
If a default is provided, its message is used. Otherwise the charset's display name is returned.

```java
messageSupport
    .message("%{cs,'UTF-16':'wide','UTF-32':'very wide',:'unknown encoding'}")
    .with("cs", StandardCharsets.UTF_8)
    .format();
// "unknown encoding"
```

Without a default entry, the display name is used as a fallback.

```java
messageSupport
    .message("%{cs,'UTF-16':'wide','UTF-32':'very wide'}")
    .with("cs", StandardCharsets.UTF_8)
    .format();
// "UTF-8"
```

## Null Handling

A `null` parameter value produces an empty string by default. You can provide a `null` map key to produce
specific text.

```java
messageSupport
    .message("%{cs,null:'no encoding specified'}")
    .with("cs", null)
    .format();
// "no encoding specified"
```
