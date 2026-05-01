# Default Configuration

The `ConfigurableMessageSupport` interface provides methods for setting default configuration
values and a default locale. These defaults act as fallbacks: when a parameter formatter looks
up a configuration value that is not specified on the message parameter itself, the default
configuration is consulted.


## Default Configuration Values

The `setDefaultConfig` method registers a default value for a named configuration key. Four
value types are supported:

```java
messageSupport.setDefaultConfig("flag", true);            // boolean
messageSupport.setDefaultConfig("clip", 80);              // long (number)
messageSupport.setDefaultConfig("bytes", "UTF-8");        // string
messageSupport.setDefaultConfig("label", parsedMessage);  // message
```

When a formatter requests a configuration value by name—for example by calling
`getConfigValueString("bytes")`—the lookup first checks the parameter's own configuration as
declared in the message format string. If the key is not present there, the default
configuration registered on the message support is returned instead. If no default has been
set either, the value is absent.

This mechanism is useful for establishing application-wide conventions.

Default configuration values can be overwritten by calling `setDefaultConfig` again with the
same name.

### Examples

**Disabling the clip suffix globally.** The `clip` post formatter appends a suffix character
(ellipsis) when text is clipped. Setting `clip-suffix` to `false` by default turns this off
for all messages, so clipped text is truncated without a suffix:

```java
var messageSupport = MessageSupportFactory.create(
    DefaultFormatterService.getSharedInstance());
messageSupport.setDefaultConfig("clip-suffix", false);

messageSupport
    .message("%(clip,'%{text}',clip:10)")
    .with("text", "Hello, World!")
    .format();
// "Hello, Wo"
```

Individual messages can still override the default by specifying the configuration key
explicitly. The parameter-level value always takes precedence:

```java
messageSupport
    .message("%(clip,'%{text}',clip:10,clip-suffix:true)")
    .with("text", "Hello, World!")
    .format();
// "Hello, W…"
```

**Formatting enums as ordinal values by default.** The `enum` configuration key controls how
enum values are formatted. Setting it globally to `"ordinal"` means all enum parameters
produce their ordinal number unless overridden:

```java
messageSupport.setDefaultConfig("enum", "ordinal");

messageSupport
    .message("Status: %{status}")
    .with("status", Thread.State.BLOCKED)
    .format();
// "Status: 2"
```

**Suppressing default `toString()` output.** Some objects produce unhelpful `toString()`
output (e.g. `Object@1a2b3c4d`). Setting `ignore-default-tostring` to `true` causes the
string formatter to return empty text instead:

```java
messageSupport.setDefaultConfig("ignore-default-tostring", true);

messageSupport
    .message("%{obj}")
    .with("obj", new Object())
    .format();
// ""
```

Combined with an `empty` map key, a fallback message can be shown instead:

```java
messageSupport.setDefaultConfig("ignore-default-tostring", true);

messageSupport
    .message("%{obj,empty:'(no value)'}")
    .with("obj", new Object())
    .format();
// "(no value)"
```


## Default Locale

The `setLocale` method sets the default locale used for formatting when no locale is specified
on the message configurer.

```java
messageSupport.setLocale(Locale.GERMANY);
```

A string variant is also available:

```java
messageSupport.setLocale("de-DE");
```

If no default locale is set explicitly, the JVM's default locale (`Locale.getDefault()`) is
used.

The default locale affects all formatting operations. For example, number formatting uses
locale-specific grouping separators and decimal marks:

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

A locale specified on the message configurer always takes precedence over the default:

```java
messageSupport.setLocale(Locale.US);

messageSupport
    .message("Total: %{amount}")
    .with("amount", 1234567.89)
    .locale(Locale.GERMANY)
    .format();
// "Total: 1.234.567,89"
```
