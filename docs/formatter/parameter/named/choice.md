# Choice

This formatter is included in the `DefaultFormatterService`.

The named formatter `choice` is selected explicitly by writing `format:choice` in the message parameter
configuration. Unlike type-based formatters, `choice` never outputs the parameter value itself. It matches the
value against the map keys defined in the parameter configuration and outputs the message associated with the best
matching key. If no key matches, the output is an empty string. This is the key difference from type-based
formatters, which fall back to outputting the formatted value when no map key matches.

All map key types are supported: `null`, `empty`, `bool`, `number` and `string`. Each match is ranked by
accuracy (exact, equivalent, lenient) and the message associated with the best matching key is selected.


## Basic Usage

The simplest use case is mapping specific values to specific text. The following example maps a numeric value to
a word. Because `choice` never outputs the value itself, a value of `0` produces an empty string since it does
not match either key.

```java
messageSupport
    .message("%{n,format:choice,<0:'negative',>0:'positive'}")
    .with("n", -3)
    .format();
// "negative"

messageSupport
    .message("%{n,format:choice,<0:'negative',>0:'positive'}")
    .with("n", 0)
    .format();
// ""

messageSupport
    .message("%{n,format:choice,<0:'negative',>0:'positive'}")
    .with("n", 7)
    .format();
// "positive"
```

The same parameter can appear multiple times in a message. In the following example, the first occurrence
outputs the numeric value while the second uses `choice` to select a unit label. When the choice produces no
output, the space before it is discarded automatically.

```java
messageSupport
    .message("%{n} %{n,format:choice,1:'color'}")
    .with("n", 1)
    .format();
// "1 color"

messageSupport
    .message("%{n} %{n,format:choice,1:'color'}")
    .with("n", 5)
    .format();
// "5"
```

Because map values are messages themselves, they can contain nested parameters. This allows a mapped message to
reference the same or other parameters. This technique is shown in the [plural suffix](#plural-suffix) example
below.


## Map Key Types

### String Keys

String keys compare the parameter value against a literal string. Matching uses a locale-aware `Collator` with
exact strength first, then a lenient match ignoring case and accents.

```java
messageSupport
    .message("%{status,format:choice,'active':'System is running','inactive':'System is stopped'}")
    .with("status", "active")
    .format();
// "System is running"

messageSupport
    .message("%{status,format:choice,'active':'System is running','inactive':'System is stopped'}")
    .with("status", "ACTIVE")
    .format();
// "System is running"

messageSupport
    .message("%{status,format:choice,'active':'System is running','inactive':'System is stopped'}")
    .with("status", "other")
    .format();
// ""
```

### Number Keys

Number keys compare the parameter value as a number. Comparison operators are supported.

```java
messageSupport
    .message("%{score,format:choice,<50:'fail',<70:'pass',<90:'merit'}")
    .with("score", 45)
    .format();
// "fail"

messageSupport
    .message("%{score,format:choice,<50:'fail',<70:'pass',<90:'merit'}")
    .with("score", 85)
    .format();
// "merit"

messageSupport
    .message("%{score,format:choice,<50:'fail',<70:'pass',<90:'merit'}")
    .with("score", 95)
    .format();
// ""
```

### Bool Keys

Bool keys match `true` and `false` values.

```java
messageSupport
    .message("%{enabled,format:choice,true:'enabled',false:'disabled'}")
    .with("enabled", true)
    .format();
// "enabled"

messageSupport
    .message("%{enabled,format:choice,true:'enabled',false:'disabled'}")
    .with("enabled", false)
    .format();
// "disabled"
```

### Null and Empty Keys

The `null` key matches when the parameter value is `null`. The `empty` key matches when the value is considered
empty (e.g. an empty string or an empty collection). The negated forms `!null` and `!empty` are available as well.

```java
messageSupport
    .message("%{name,format:choice,null:'no name',empty:'blank name',!empty:'%{name}'}")
    .with("name", null)
    .format();
// "no name"

messageSupport
    .message("%{name,format:choice,null:'no name',empty:'blank name',!empty:'%{name}'}")
    .with("name", "")
    .format();
// "blank name"

messageSupport
    .message("%{name,format:choice,null:'no name',empty:'blank name',!empty:'%{name}'}")
    .with("name", "Alice")
    .format();
// "Alice"
```

### Default Key

A default map key (`:`) catches any value that does not match a specific key. Be aware that adding a default key
means every value produces output, which removes the main advantage of `choice` over a type-based formatter.


## Practical Examples

### Plural Suffix

A classic application is appending a plural suffix only when the count is not one. The `choice` parameter is
placed directly after the word stem without a space. For a count of one, the `<>1` key does not match
and `choice` produces nothing, leaving the bare stem. For any other count, `choice` appends the suffix.

This pattern cannot be achieved with a type-based formatter because the number formatter would output the number
itself when no key matches, corrupting the word.

```java
messageSupport
    .message("%{n} item%{n,format:choice,<>1:'s'}")
    .with("n", 1)
    .format();
// "1 item"

messageSupport
    .message("%{n} item%{n,format:choice,<>1:'s'}")
    .with("n", 5)
    .format();
// "5 items"

messageSupport
    .message("%{n} item%{n,format:choice,<>1:'s'}")
    .with("n", 0)
    .format();
// "0 items"
```

### Conditional Message

Another use case is showing a message only for specific values and nothing at all for everything else. In this
example, a warning is appended only for recognized error codes. For all other codes `choice` produces nothing
and the space before it is discarded, leaving the preceding text clean.

Without `choice`, the number formatter would output the numeric code for unmatched values, producing unwanted
output like "Login complete. 0".

```java
messageSupport
    .message("Login complete. %{warn,format:choice,403:'Access denied.',429:'Too many attempts.'}")
    .with("warn", 0)
    .format();
// "Login complete."

messageSupport
    .message("Login complete. %{warn,format:choice,403:'Access denied.',429:'Too many attempts.'}")
    .with("warn", 403)
    .format();
// "Login complete. Access denied."

messageSupport
    .message("Login complete. %{warn,format:choice,403:'Access denied.',429:'Too many attempts.'}")
    .with("warn", 429)
    .format();
// "Login complete. Too many attempts."
```


### Exit Code Classification

When a value is used purely as a selector and should never appear in the output, `choice` is the right tool.
In this example specific exit codes are mapped to labels. Codes that do not match any key produce an empty string,
which is intentional because only recognized codes should be labeled.

```java
messageSupport
    .message("%{code,format:choice,0:'SUCCESS',1:'GENERAL_ERROR',2:'MISUSE',126:'NOT_EXECUTABLE',127:'NOT_FOUND'}")
    .with("code", 0)
    .format();
// "SUCCESS"

messageSupport
    .message("%{code,format:choice,0:'SUCCESS',1:'GENERAL_ERROR',2:'MISUSE',126:'NOT_EXECUTABLE',127:'NOT_FOUND'}")
    .with("code", 126)
    .format();
// "NOT_EXECUTABLE"

messageSupport
    .message("%{code,format:choice,0:'SUCCESS',1:'GENERAL_ERROR',2:'MISUSE',126:'NOT_EXECUTABLE',127:'NOT_FOUND'}")
    .with("code", 42)
    .format();
// ""
```
