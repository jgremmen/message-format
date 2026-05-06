# Clip

The `clip` post formatter truncates formatted text to a maximum number of characters. This is
useful when displaying user-generated or dynamically formatted content in space-constrained
contexts such as log messages, table columns, notification previews or UI labels.


## Basic Usage

The maximum length is specified by the `clip` configuration key as a numeric value. When the
formatted sub-message exceeds this length, it is truncated and an ellipsis character (`…`) is
appended by default to signal that the text was cut short.

```java
messageSupport
    .message("%(clip,'%{title}',clip:20)")
    .with("title", "A very elaborate and excessively long title")
    .format();
// "A very elaborate an…"
```

The total length of the result, including the ellipsis, does not exceed the configured maximum.
In the example above the output is exactly 20 characters: 19 visible characters plus the
ellipsis.

If the text is shorter than or equal to the maximum length, it is returned unchanged.

```java
messageSupport
    .message("%(clip,'%{title}',clip:50)")
    .with("title", "Short title")
    .format();
// "Short title"
```


## Clipping Disabled

Setting the `clip` configuration key to zero or a negative number disables clipping entirely.
The text passes through unmodified regardless of its length.

```java
messageSupport
    .message("%(clip,'%{description}',clip:0)")
    .with("description", "This text will not be clipped at all")
    .format();
// "This text will not be clipped at all"
```


## Minimum Effective Length

When the suffix is enabled, the clip post formatter enforces a minimum effective length to
ensure that at least a few characters of actual content are visible alongside the suffix. With
the default ellipsis suffix (one character), the minimum effective output length is 5
characters: 4 content characters plus the ellipsis. This means that even if you set `clip` to
a very small value like 2, the result will still contain enough characters to be meaningful.

```java
messageSupport
    .message("%(clip,'%{text}',clip:2)")
    .with("text", "This is a very long text")
    .format();
// "This…"
```

The minimum scales with the suffix length. For a custom suffix of 3 characters, the minimum
effective length is 7: at least 4 content characters plus the 3-character suffix.


## Trailing Whitespace

When the text is clipped and a suffix is appended, any trailing whitespace at the truncation
point is trimmed before the suffix is added. This prevents awkward results where a space
appears right before the ellipsis.

```java
messageSupport
    .message("%(clip,'%{text}',clip:9)")
    .with("text", "This is a very long text")
    .format();
// "This is…"
```

In this example the first 8 characters of the text would be `"This is "`, with a trailing
space. That space is trimmed before appending the ellipsis, producing `"This is…"` (8
characters total) rather than `"This is …"`.


## Disabling the Suffix

By default, clipped text receives an ellipsis suffix to indicate truncation. You can disable
this behavior by setting the `clip-suffix` configuration key to `false`. When disabled, the
text is hard-truncated at the exact maximum length with no suffix appended and no trailing
whitespace trimming.

```java
messageSupport
    .message("%(clip,'%{text}',clip:10,clip-suffix:false)")
    .with("text", "This is a very long text")
    .format();
// "This is a "
```


## Custom Suffix Text

Instead of the default ellipsis character, you can provide your own suffix string using the
`clip-suffix-text` configuration key. This replaces the ellipsis entirely. When a custom
suffix is set, the `clip-suffix` key does not need to be specified because providing a custom
suffix text implicitly enables the suffix behavior.

```java
messageSupport
    .message("%(clip,'%{text}',clip:15,clip-suffix-text:'...')")
    .with("text", "A sentence that is way too long")
    .format();
// "A sentence t..."
```

```java
messageSupport
    .message("%(clip,'%{text}',clip:20,clip-suffix-text:' [more]')")
    .with("text", "Documentation for the message format library")
    .format();
// "Documentation [more]"
```

Custom suffix text is particularly handy for localization. In German, for instance, you might
want to append `" usw."` (abbreviation for "und so weiter") instead of an ellipsis.

```java
messageSupport
    .message("%(clip,\"%{v,number:'#.##################'}\",clip:12,clip-suffix-text:' usw.')")
    .with("v", Math.PI)
    .locale(Locale.GERMANY)
    .format();
// "3,14159 usw."
```


## Global Default Configuration

If you want to control the suffix behavior consistently across all messages without repeating
configuration keys in every post formatter invocation, you can set global defaults on the
`ConfigurableMessageSupport` instance using `setDefaultConfig`. These defaults apply whenever
a configuration key is not explicitly provided in the message itself.

```java
var messageSupport = MessageSupportFactory
    .create(new DefaultFormatterService(), NO_CACHE_INSTANCE);

// Globally disable the ellipsis suffix
messageSupport.setDefaultConfig("clip-suffix", false);

messageSupport
    .message("%(clip,'%{text}',clip:10)")
    .with("text", "Hello World, this is too long")
    .format();
// "Hello Worl"
```

Per-message configuration keys always take precedence over global defaults. This allows you to
set a project-wide policy and override it selectively where needed.

```java
// Global default: suffix disabled
messageSupport.setDefaultConfig("clip-suffix", false);

// This message overrides the global default
messageSupport
    .message("%(clip,'%{text}',clip:10,clip-suffix:true)")
    .with("text", "Hello World, this is too long")
    .format();
// "Hello Wor…"
```


## Combining with Other Formatters

The sub-message inside a clip post formatter is a full message. It can contain parameter
references with their own formatting configuration, template references and even nested post
formatter invocations. This makes `clip` a natural fit for capping the output of any complex
formatting pipeline.

```java
messageSupport
    .message("%(clip,\"%{price,number:'#,##0.00'} %{currency}\",clip:15)")
    .with("price", 1234567.89)
    .with("currency", "USD")
    .locale(Locale.US)
    .format();
// "1,234,567.89 U…"
```

You can also nest `clip` inside a `case` post formatter, or apply `case` first and then clip
the result.

```java
messageSupport
    .message("%(clip,\"%(case,'%{label}',case:upper)\",clip:10)")
    .with("label", "important warning message")
    .locale(Locale.US)
    .format();
// "IMPORTANT…"
```


## Configuration Reference

| Key                | Type    | Default | Description                                       |
|--------------------|---------|---------|---------------------------------------------------|
| `clip`             | number  |         | Maximum length of the output text                 |
| `clip-suffix`      | boolean | `true`  | Whether to append a suffix when text is clipped    |
| `clip-suffix-text` | string  | `…`     | Custom suffix text to use instead of the ellipsis |
