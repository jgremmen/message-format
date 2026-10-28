---
icon: material/code-block-parentheses
---

# Post Formatters

Post formatters operate on already formatted text rather than on raw parameter values. Where a parameter formatter 
converts a Java object into its textual representation, a post formatter receives the final text output of a 
sub-message and applies a transformation to it. The transformation can be anything from changing letter case to 
truncating the text to a maximum length.

A post formatter is invoked using the `%(...)` syntax inside a message format string. The general structure is:

```text
%(name, 'sub-message', config-name:value, config-name:value, ...)
```

The first element is the name of the post formatter. The second element is a quoted message whose formatted result
becomes the input to the post formatter. Any additional elements are configuration key-value pairs that control the 
transformation. The sub-message is a full message in its own right: it can contain text, parameter references, template
references and even nested post formatter invocations.

```java
messageSupport
    .message("%(clip,'%{description}',clip:30)")
    .with("description", "A text that is longer than thirty characters")
    .format();
// "A text that is longer than th…"
```

The sub-message must be quoted using either single quotes (`'...'`) or double quotes (`"..."`). When the sub-message
itself contains quoted strings (for example, map values on a nested parameter), double quotes are typically used for 
the outer message and single quotes inside, or vice versa.

```java
messageSupport
    .message(
        "%(case,\"%{flag,true:'enabled',false:'disabled'}\",case:upper)")
    .with("flag", true)
    .locale(Locale.US)
    .format();
// "ENABLED"
```

Both quote characters may also be nested with the same character, because a parameter reference starts a new quoting
context. `'%{flag,true:'enabled'}'` is therefore valid as well.


## Configuration Keys

Each post formatter defines its own set of configuration keys. A configuration value can be a boolean (`true` or 
`false`), a number or a string, either plain or quoted. Unlike parameter configuration, a post formatter does not
accept a message as a configuration value, so a quoted value that contains a parameter, template or post formatter
reference is rejected by the parser. The key name must follow the kebab-case naming convention: lowercase letters and
digits separated by single hyphens.

```text
%(clip, '%{text}', clip:20, clip-suffix:false)
```

Configuration keys specified in the message format string always take precedence over global defaults registered via 
`setDefaultConfig` on the `ConfigurableMessageSupport`. If neither an inline value nor a default is present, the post 
formatter falls back to its own built-in default. This resolution order is the same as for parameter formatter 
configuration keys and is described in detail on the
[Default Configuration](../../configuration/default-configuration.md) page.


## Registration

Post formatters are registered on the `FormatterService.WithRegistry` using the `addPostFormatter` method. Each post 
formatter has a unique name returned by its `getName()` method. Registering two post formatters with the same name is 
not allowed and results in an exception.

When using `DefaultFormatterService`, all post formatter implementations declared as services via 
`META-INF/services/de.sayayi.lib.message.formatter.post.PostFormatter` are discovered and registered automatically 
through Java's `ServiceLoader` mechanism. The built-in post formatters `case` and `clip` are registered this way and
are available out of the box.

To register a post formatter manually, for example when using a `GenericFormatterService`, do so during setup:

```java
var formatterService = new GenericFormatterService();
formatterService.addPostFormatter(new CasePostFormatter());
formatterService.addPostFormatter(new ClipPostFormatter());

var messageSupport = MessageSupportFactory.create(formatterService);
// "case" and "clip" are available, no other post formatters
```


## Nesting

Post formatter invocations can be nested. The output of one post formatter can serve as the sub-message of another. The 
inner post formatter is evaluated first and its result is then passed to the outer one.

```java
messageSupport
    .message("%(clip,\"%(case,'%{text}',case:upper)\",clip:10)")
    .with("text", "important warning message")
    .locale(Locale.US)
    .format();
// "IMPORTANT…"
```

Post formatters can appear at the top level of a message as well as inside parameter map values, inside the
sub-message of another post formatter and in templates. A post formatter name that is not registered is not detected
when the message is parsed. Formatting such a message throws a `MessageFormatException` whose cause names the missing
post formatter.


## Empty Input

If the sub-message produces an empty result, the post formatter is not invoked. This means post formatters do not need
to guard against empty input. The post formatter part then produces no text. If the `%(...)` reference is surrounded by
spaces in the message, a single space remains, so the surrounding words stay separated.

```java
messageSupport
    .message("Name: %(case,'%{name}',case:upper) (unverified)")
    .with("name", "")
    .format();
// "Name: (unverified)"
```

/// note | Migration from 0.24
Up to version 0.24.0, the spaces around a post formatter with an empty result were discarded entirely, which could
join the surrounding words.
///


## Reference

| Post Format Name | Formatter Class     | Documentation   |
|------------------|---------------------|-----------------|
| `case`           | `CasePostFormatter` | [Case](case.md) |
| `clip`           | `ClipPostFormatter` | [Clip](clip.md) |
