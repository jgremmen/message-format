# Token

/// note
This formatter is **not** included in the `DefaultFormatterService`. It must be registered explicitly by adding a
`TokenFormatter` instance to the formatter service.
///

The `TokenFormatter` handles ANTLR `org.antlr.v4.runtime.Token` values. It is automatically selected whenever a
parameter value implements the `Token` interface, which includes `CommonToken` and any custom token implementation. The
formatter extracts different aspects of a token depending on the `token` configuration key.


## Registration

Because the `TokenFormatter` is not part of the default formatter set, it must be added manually to the formatter
service before use. The ANTLR runtime dependency must be on the classpath.

```java
var formatterService = new DefaultFormatterService();
formatterService.addFormatter(new TokenFormatter());

var messageSupport = MessageSupportFactory.create(formatterService).seal();
```


## Token Configuration Values

The `token` configuration key controls which aspect of the token is extracted and formatted. When omitted, the
formatter defaults to `text`.

| Value      | Extracted information                           |
|------------|-------------------------------------------------|
| `text`     | The token text as returned by `Token.getText()` |
| `type`     | The token type number (integer)                 |
| `channel`  | The token channel number (integer)              |
| `line`     | The adjusted line number (integer)              |
| `column`   | The adjusted column number (integer)            |
| `position` | A combined line and column string               |

Any value not listed above produces an empty string.


## Formatting Token Text

The default behavior (or `token:text`) formats the literal text content of the token. This is the character sequence
that the lexer matched for this token in the input stream.

```java
messageSupport
    .message("unexpected token '%{t}'")
    .with("t", token)
    .format();
// "unexpected token 'bloolean'"
```

```java
messageSupport
    .message("unexpected token '%{t,token:text}'")
    .with("t", token)
    .format();
// "unexpected token 'bloolean'"
```


## Formatting Token Type and Channel

The `type` and `channel` values extract the numeric token type and channel identifiers. These are the integer constants
defined by the ANTLR-generated lexer and parser classes. The formatter delegates the integer value to the number
formatter, so all number formatting options apply.

```java
messageSupport
    .message("token type: %{t,token:type}")
    .with("t", token)
    .format();
// "token type: 5"  (the actual number depends on the grammar)
```

```java
messageSupport
    .message("channel: %{t,token:channel}")
    .with("t", token)
    .format();
// "channel: 0"  (0 = DEFAULT_TOKEN_CHANNEL)
```


## Formatting Line and Column

The `line` and `column` values extract the token's position in the source input. ANTLR reports lines starting at 1 and
columns (character position in line) starting at 0. The formatter adjusts these raw values using the `token-1st-line`
and `token-1st-column` configuration keys before formatting.

The `token-1st-line` key defines what line number the first line should be reported as. It defaults to `1`, meaning the
raw ANTLR line numbers are used unchanged. Setting it to `0` produces zero-based line numbers.

The `token-1st-column` key defines what column number the first column should be reported as. It defaults to `0`,
matching ANTLR's native zero-based column reporting. Setting it to `1` produces one-based column numbers.

If the token has no valid line information (line < 1), the `line` value produces an empty string. Similarly, if the
token has no valid column information (character position < 0), the `column` value produces an empty string.

```java
messageSupport
    .message("line %{t,token:line}, column %{t,token:column}")
    .with("t", token)
    .format();
// "line 1, column 2"
```

```java
messageSupport
    .message("line %{t,token:line,token-1st-line:0}")
    .with("t", token)
    .format();
// "line 0"
```

```java
messageSupport
    .message("col %{t,token:column,token-1st-column:1}")
    .with("t", token)
    .format();
// "col 3"
```

In the last example, the raw ANTLR column is 2 (zero-based). Adding `token-1st-column:1` shifts all columns up by one,
producing a one-based column number of 3.


## Formatting Position

The `position` value combines line and column into a single formatted string. When the token has no valid line
information, the position produces an empty string regardless of the format used. This prevents nonsensical output like
`0:5` when the token's line data is unavailable.

The default position format is `%{line}%{column,!empty:':%{column}'}`, which renders as `line:column` when a column is
available or just `line` when the column is empty.

```java
messageSupport
    .message("at %{t,token:position}")
    .with("t", token)
    .format();
// "at 1:2"
```

### Custom Position Format

The `token-position-format` configuration key accepts a message that overrides the default position format. The message
has access to two parameters: `line` (the adjusted line number) and `column` (the adjusted column number, or empty when
unavailable). This allows full customization of how the position is rendered.

```java
messageSupport
    .message("""
        at %{t,\
            token:position,\
            token-position-format:'(%{line}, %{column})'}\
        """)
    .with("t", token)
    .format();
// "at (1, 2)"
```

```java
messageSupport
    .message(
        "at %{t,token:position,token-position-format:'line %{line}'}")
    .with("t", token)
    .format();
// "at line 1"
```

The adjustments from `token-1st-line` and `token-1st-column` apply to the position format as well. They affect the
`line` and `column` parameters available inside `token-position-format`.

```java
messageSupport
    .message("""
        %{t,token:position,\
            token-position-format:'%{line}/%{column}',\
            token-1st-line:0,\
            token-1st-column:1}\
        """)
    .with("t", token)
    .format();
// "0/3"
```


## Default Configuration

All token-related configuration keys (`token-1st-line`, `token-1st-column`, `token-position-format`) can be set as
application-wide defaults on the `ConfigurableMessageSupport` using `setDefaultConfig(...)`. This avoids repeating the
same adjustments in every message that formats a token. A configuration value specified directly in a message parameter
always takes precedence over the default.

```java
var configurableMessageSupport = 
    MessageSupportFactory.create(formatterService);

configurableMessageSupport
    .setDefaultConfig("token-1st-column", 1L)
    .setDefaultConfig("token-1st-line", 1L);

var factory = 
    configurableMessageSupport.getMessageAccessor().getMessageFactory();
configurableMessageSupport.setDefaultConfig(
    "token-position-format",
    factory.parseMessage("(%{line}, %{column})"));

var messageSupport = configurableMessageSupport.seal();

messageSupport
    .message("at %{t,token:position}")
    .with("t", token)
    .format();
// "at (1, 3)"  (line stays 1, column shifted from 0-based to 1-based, 
// custom format applied globally)
```


## Null Handling

A `null` parameter value produces an empty string. Since the `TokenFormatter` extends
`AbstractSingleTypeParameterFormatter`, it only receives non-null `Token` instances. A null value is handled by the
framework before the formatter is invoked.

```java
messageSupport
    .message("token: '%{t,token:text}'")
    .with("t", null)
    .format();
// "token: ''"
```


## Practical Example

A typical use case for the `TokenFormatter` is constructing user-facing error messages in a parser. When the ANTLR
parser encounters a syntax error, the offending token can be passed directly into a message parameter to produce a
clear diagnostic.

```java
messageSupport
    .message(
        "syntax error at %{t,token:position}: unexpected '%{t,token:text}'")
    .with("t", errorToken)
    .format();
// "syntax error at 3:12: unexpected ';'"
```

The same token can appear in multiple parameters with different `token` configurations, extracting different aspects
each time without requiring the caller to decompose the token manually.
