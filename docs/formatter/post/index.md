---
icon: material/code-block-parentheses
---

# Post Formatters

Post formatters operate on already formatted text rather than on raw parameter values. Where a
parameter formatter converts a Java object into its textual representation, a post formatter
receives the final text output of a sub-message and applies a transformation to it. The
transformation can be anything from changing letter case to truncating the text to a maximum
length.

A post formatter is invoked using the `%(...)` syntax inside a message format string. The
general structure is:

```
%(name, 'sub-message', config-name:value, config-name:value, ...)
```

The first element is the name of the post formatter. The second element is a quoted message
whose formatted result becomes the input to the post formatter. Any additional elements are
configuration key-value pairs that control the transformation. The sub-message is a full
message in its own right: it can contain text, parameter references, template references and
even nested post formatter invocations.

```java
messageSupport
    .message("%(clip,'%{description}',clip:30)")
    .with("description", "A text that is longer than thirty characters")
    .format();
```

The sub-message must be quoted using either single quotes (`'...'`) or double quotes
(`"..."`). When the sub-message itself contains quoted strings (for example, map values on a
nested parameter), you typically use double quotes for the outer message and single quotes
inside, or vice versa.

```java
messageSupport
    .message("%(case,\"%{flag,true:'enabled',false:'disabled'}\",case:upper)")
    .with("flag", true)
    .locale(Locale.US)
    .format();
```


## Configuration Keys

Each post formatter defines its own set of configuration keys. A configuration value can be a
boolean (`true` or `false`), a number, a plain string, or a quoted message. The key name must
follow the kebab-case naming convention: lowercase letters and digits separated by single
hyphens.

```
%(clip, '%{text}', clip:20, clip-suffix:false)
```

Configuration keys specified in the message format string always take precedence over global
defaults registered via `setDefaultConfig` on the `ConfigurableMessageSupport`. If neither an
inline value nor a default is present, the post formatter falls back to its own built-in
default. This resolution order is the same as for parameter formatter configuration keys and
is described in detail on the [Default Configuration](../../core/default-configuration.md)
page.


## Registration

Post formatters are registered on the `FormatterService.WithRegistry` using the
`addPostFormatter` method. Each post formatter has a unique name returned by its `getName()`
method. Registering two post formatters with the same name is not allowed and results in an
exception.

When using `DefaultFormatterService`, all post formatter implementations declared as services
via `META-INF/services/de.sayayi.lib.message.formatter.post.PostFormatter` are discovered and
registered automatically through Java's `ServiceLoader` mechanism. The built-in post
formatters `case` and `clip` are registered this way and are available out of the box.

If you need to register a post formatter manually, for example when using a
`GenericFormatterService`, you can do so during setup:

```java
var formatterService = new GenericFormatterService();
formatterService.addPostFormatter(new CasePostFormatter());
formatterService.addPostFormatter(new ClipPostFormatter());

var messageSupport = MessageSupportFactory.create(formatterService);
```


## Nesting

Post formatter invocations can be nested. The output of one post formatter can serve as the
sub-message of another. The inner post formatter is evaluated first, and its result is then
passed to the outer one.

```java
messageSupport
    .message("%(clip,\"%(case,'%{text}',case:upper)\",clip:10)")
    .with("text", "important warning message")
    .locale(Locale.US)
    .format();
```

Post formatters can also appear inside parameter map values and template messages. There is no
restriction on where a `%(...)` part can be placed as long as it is inside a quoted message
context.


## Empty Input

If the sub-message produces an empty result, the post formatter is not invoked. The empty
string is returned directly. This means post formatters do not need to guard against empty
input.


## Reference

| Post Format Name | Formatter Class     | Documentation   |
|------------------|---------------------|-----------------|
| `case`           | `CasePostFormatter` | [Case](case.md) |
| `clip`           | `ClipPostFormatter` | [Clip](clip.md) |
