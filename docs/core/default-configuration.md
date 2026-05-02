# Default Configuration

When formatting a message, parameter formatters and post formatters often need configuration
values to control their behavior. These values can be specified directly in the message format
string, for example `clip:20` or `list-sep:', '`. However, when many messages share the same
configuration, repeating it in every format string becomes tedious and error-prone. Default
configuration solves this by letting you register application-wide fallback values on the
`ConfigurableMessageSupport`. A formatter first checks the parameter's own configuration as
declared in the format string. Only when the requested key is not present there does it fall
back to the default configuration. If no default has been set either, the value is absent and the
formatter applies its own built-in default.


## Setting Default Values

The `setDefaultConfig` method registers a default value for a named configuration key. Four
value types are supported: boolean, number (long), string and message. Each call returns the
`ConfigurableMessageSupport` itself, which allows multiple defaults to be set in a fluent chain:

```java
var messageSupport = MessageSupportFactory.create(
    DefaultFormatterService.getSharedInstance());

messageSupport
    .setDefaultConfig("clip-suffix", false)
    .setDefaultConfig("clip", 80)
    .setDefaultConfig("list-sep", ", ")
    .setDefaultConfig("list-sep-last", " and ");
```

Calling `setDefaultConfig` with a key that already has a value simply overwrites the previous
entry. Configuration keys must be kebab-case: lowercase letters and digits separated by single
hyphens, such as `clip-suffix` or `list-sep-last`.


## Overriding at the Message Level

A default configuration value is always a fallback. When a message format string specifies the
same key explicitly, the inline value takes precedence. This means you can establish sensible
application-wide defaults and override them selectively where a different behavior is needed.

Consider a default that disables the clip suffix globally:

```java
messageSupport.setDefaultConfig("clip-suffix", false);

// Uses the default: no suffix
messageSupport
    .message("%(clip,'%{text}',clip:15)")
    .with("text", "A very long sentence that will be clipped")
    .format();
// "A very long sen"

// Overrides the default: suffix enabled for this message
messageSupport
    .message("%(clip,'%{text}',clip:15,clip-suffix:true)")
    .with("text", "A very long sentence that will be clipped")
    .format();
// "A very long se\u2026"
```


## Practical Examples

### List Separators

The list/array formatter uses configuration keys to control how elements are separated. Setting
defaults for `list-sep` and `list-sep-last` avoids repeating the same separator strings across
every message that formats a collection:

```java
messageSupport
    .setDefaultConfig("list-sep", ", ")
    .setDefaultConfig("list-sep-last", " and ");

messageSupport
    .message("Colours: %{colours}")
    .with("colours", List.of("red", "green", "blue"))
    .format();
// "Colours: red, green and blue"

messageSupport
    .message("Invitees: %{names}")
    .with("names", new String[] { "Alice", "Bob" })
    .format();
// "Invitees: Alice and Bob"
```

Because the defaults apply to all list-formatted parameters, both messages above produce
naturally separated output without specifying `list-sep` or `list-sep-last` explicitly.

### Enum Formatting

The `enum` configuration key controls how enum values are rendered. By default, the enum
formatter uses the constant name. Setting the default to `"ordinal"` switches all enum parameters
to their ordinal number unless an individual message overrides it:

```java
messageSupport.setDefaultConfig("enum", "ordinal");

messageSupport
    .message("State: %{state}")
    .with("state", Thread.State.BLOCKED)
    .format();
// "State: 2"
```

### Suppressing Default toString() Output

Some objects produce unhelpful `toString()` output such as `java.lang.Object@1a2b3c4d`. The
`string` formatter supports a configuration key called `ignore-default-tostring`. When set to
`true` globally, any value whose `toString()` result matches the default
`ClassName@hashCode` pattern is treated as empty instead:

```java
messageSupport.setDefaultConfig("ignore-default-tostring", true);

messageSupport
    .message("%{obj}")
    .with("obj", new Object())
    .format();
// ""
```

This works especially well in combination with an `empty` map key, which lets you provide a
meaningful fallback when the value has no useful string representation:

```java
messageSupport.setDefaultConfig("ignore-default-tostring", true);

messageSupport
    .message("%{obj,empty:'(no value)',!empty:'%{obj}'}")
    .with("obj", new Object())
    .format();
// "(no value)"
```

### Message-Typed Defaults

The default configuration also accepts a parsed `Message.WithSpaces` value. This is useful when a
formatter expects a message as its configuration value, such as a custom suffix or prefix
template. The message is parsed separately and passed as a default:

```java
var factory = messageSupport.getMessageAccessor().getMessageFactory();

messageSupport.setDefaultConfig("clip-suffix-text",
    factory.parseMessage("..."));
```


## Default Locale

In addition to configuration defaults, `ConfigurableMessageSupport` allows you to set a default
locale. This locale is used whenever a message is formatted without an explicit locale on the
message configurer. On construction, the default locale is initialized to `Locale.getDefault()`,
so it matches the JVM's locale unless changed.

```java
messageSupport.setLocale(Locale.GERMANY);
```

A string variant accepting language tags is also available:

```java
messageSupport.setLocale("de-DE");
```

The default locale affects every formatting operation that depends on locale-specific rules.
Number formatting, for example, uses locale-appropriate grouping separators and decimal marks:

```java
messageSupport.setLocale(Locale.US);

messageSupport
    .message("Total: %{amount}")
    .with("amount", 1234567.89)
    .format();
// "Total: 1,234,567.89"

messageSupport.setLocale(Locale.GERMANY);

messageSupport
    .message("Total: %{amount}")
    .with("amount", 1234567.89)
    .format();
// "Total: 1.234.567,89"
```

Just as with configuration defaults, a locale specified directly on the message configurer always
takes precedence. The default locale is only consulted when no explicit locale has been provided:

```java
messageSupport.setLocale(Locale.US);

messageSupport
    .message("Total: %{amount}")
    .with("amount", 1234567.89)
    .locale(Locale.GERMANY)
    .format();
// "Total: 1.234.567,89"
```
