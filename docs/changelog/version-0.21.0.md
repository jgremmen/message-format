---
title: 0.20.1 -> 0.21.0
toc_depth: 2
---

# Version [0.21.0](https://github.com/jgremmen/message-format/tree/0.21.0) (2026-04-21)

## Breaking Changes

### Java 21 required

The minimum Java version has been raised from 11 to 21. All modules are now compiled with `sourceCompatibility = 21`
and `targetCompatibility = 21`. Code throughout the library has been refactored to use modern Java syntax (sealed
interfaces, records, pattern matching, etc.).

### Package restructuring of formatter classes

The formatter-related packages have been reorganized under a new hierarchy. All parameter formatters have moved from
`de.sayayi.lib.message.formatter` into sub-packages under `de.sayayi.lib.message.formatter.parameter`:

| Old package | New package |
|---|---|
| `de.sayayi.lib.message.formatter` | `de.sayayi.lib.message.formatter.parameter` |
| `de.sayayi.lib.message.formatter.named` | `de.sayayi.lib.message.formatter.parameter.named` |
| `de.sayayi.lib.message.formatter.named.extra` | `de.sayayi.lib.message.formatter.parameter.named.extra` |
| `de.sayayi.lib.message.formatter.runtime` | `de.sayayi.lib.message.formatter.parameter.runtime` |
| `de.sayayi.lib.message.formatter.runtime.extra` | `de.sayayi.lib.message.formatter.parameter.runtime.extra` |

The following interfaces have been moved accordingly:

- `ParameterFormatter` is now at `de.sayayi.lib.message.formatter.parameter.ParameterFormatter`
- `NamedParameterFormatter` is now at `de.sayayi.lib.message.formatter.parameter.NamedParameterFormatter`
- `AbstractParameterFormatter` is now at `de.sayayi.lib.message.formatter.parameter.AbstractParameterFormatter`
- `AbstractSingleTypeParameterFormatter` is now at `de.sayayi.lib.message.formatter.parameter.AbstractSingleTypeParameterFormatter`
- `AbstractMultiSelectFormatter` is now at `de.sayayi.lib.message.formatter.parameter.AbstractMultiSelectFormatter`

Update all import statements in your code to reflect the new package paths.

### `ParameterPostFormatter` replaced by `PostFormatter`

The `ParameterPostFormatter` interface in the `de.sayayi.lib.message.formatter` package has been removed and replaced
by a new `PostFormatter` interface at `de.sayayi.lib.message.formatter.post.PostFormatter`. The `ClipPostFormatter`
has been moved to `de.sayayi.lib.message.formatter.post.runtime.ClipPostFormatter`.

The service loader declaration has changed accordingly. If you have custom post formatter implementations registered
via `META-INF/services`, update the service file name from
`de.sayayi.lib.message.formatter.ParameterPostFormatter` to
`de.sayayi.lib.message.formatter.post.PostFormatter`.

### `ParameterConfig` removed and replaced by `MessagePart.Config` and `MessagePart.Map`

The `ParameterConfig` class in `de.sayayi.lib.message.part.parameter` has been removed. Its functionality has been
split into two separate inner interfaces of `MessagePart`:

- `MessagePart.Config` holds named configuration entries (key-value pairs like `clip:20`).
- `MessagePart.Map` holds the condition-to-message mapping (e.g. `null:'n/a', empty:'none', :'%{value}'`).

Code that previously called `getParamConfig()` on a `MessagePart.Parameter` must now call either `getConfig()` or
`getMap()` depending on the type of data being accessed.

### `ConfigKey` and `ConfigValue` removed

The `ConfigKey` and `ConfigValue` types from `de.sayayi.lib.message.part.parameter.key` and
`de.sayayi.lib.message.part.parameter.value` have been removed. Map keys are now represented by `MapKey`
(in `de.sayayi.lib.message.part`) and configuration values by `TypedValue<?>` (in `de.sayayi.lib.message.part`).
The concrete key implementations (`ConfigKeyBool`, `ConfigKeyNull`, etc.) have been moved to internal packages and
are no longer part of the public API.

### `MessagePart` interfaces are now sealed

The `MessagePart` interface hierarchy is now sealed. `MessagePart` permits only `MessagePart.Text` and
`MessagePart.NamedMessagePart`. `NamedMessagePart` permits `MessagePart.Parameter`, `MessagePart.Template` and the
new `MessagePart.PostFormat`. Custom implementations of these interfaces are no longer allowed.

### `SortedArrayMap` moved to internal package

`SortedArrayMap` has been moved from `de.sayayi.lib.message.util` to an internal package and is no longer accessible
as public API.

### `MatcherFormatter` renamed to `MatchResultFormatter`

The `MatcherFormatter` in `de.sayayi.lib.message.formatter.runtime.extra` has been renamed to
`MatchResultFormatter` and moved to `de.sayayi.lib.message.formatter.parameter.runtime.extra.MatchResultFormatter`.

### `getDefaultParameterConfig` renamed to `getDefaultConfig`

On the `MessageAccessor` interface, `getDefaultParameterConfig(String)` has been renamed to `getDefaultConfig(String)`.
The return type has changed from `ConfigValue` to `TypedValue<?>`. Similarly, all `setDefaultParameterConfig` overloads
on `ConfigurableMessageSupport` have been renamed to `setDefaultConfig`.

Update call sites from:

```java
ConfigValue value = accessor.getDefaultParameterConfig("name");
messageSupport.setDefaultParameterConfig("clip", 20);
```

to:

```java
TypedValue<?> value = accessor.getDefaultConfig("name");
messageSupport.setDefaultConfig("clip", 20);
```

### `getParameterPostFormatters` replaced by `getPostFormatter`

The `getParameterPostFormatters()` method on `MessageAccessor` (which returned a `Map<String,ParameterPostFormatter>`)
has been replaced by `getPostFormatter(String)` which returns a single `PostFormatter` for the given name, or `null`
if not found.

### `getFormatters` signature changed

The `getFormatters` methods on `MessageAccessor` now require an additional `MessagePart.Config` parameter. This is
used to support auto-application of named formatters based on configuration names. Update call sites from:

```java
ParameterFormatter[] formatters = accessor.getFormatters(type);
ParameterFormatter[] formatters = accessor.getFormatters(format, type);
```

to:

```java
ParameterFormatter[] formatters = accessor.getFormatters(type, config);
ParameterFormatter[] formatters = accessor.getFormatters(format, type, config);
```

### `AbstractConfigKeyComparator` renamed to `AbstractMapKeyComparator`

The class `AbstractConfigKeyComparator` in `de.sayayi.lib.message.formatter` has been renamed to
`AbstractMapKeyComparator`.

### `MessagePart.Map.getDefaultValue` renamed to `getDefaultMessage`

On the `MessagePart.Map` interface, `getDefaultValue` has been renamed to `getDefaultMessage` and now returns
`Optional<Message.WithSpaces>` instead of a nullable value.

### `importMessages` API changed

The `importMessages` method on `ConfigurableMessageSupport` no longer accepts varargs (`InputStream...`) or
`Enumeration<URL>`. It now accepts a single `InputStream`. The static utility method
`MessageUtil.importMessages(InputStream, Consumer<Message.WithCode>, BiConsumer<String,Message.WithSpaces>)` is
available for lower-level control over imported messages and templates.

If you previously used:

```java
messageSupport.importMessages(stream1, stream2);
```

call `importMessages` for each stream individually:

```java
messageSupport.importMessages(stream1);
messageSupport.importMessages(stream2);
```

### `LRUMessagePartNormalizer` is no longer directly instantiable

The constructor of `LRUMessagePartNormalizer` has been replaced by a factory method. Use:

```java
MessagePartNormalizer normalizer = LRUMessagePartNormalizer.create(256);
```

instead of:

```java
MessagePartNormalizer normalizer = new LRUMessagePartNormalizer(256);
```

### Template syntax changed

Both template parameter delegation and template parameter defaults have changed syntax.

Parameter delegation has changed from `=` to `->`:

```msgfmt
// 0.20.1
%[my-template, localParam=delegatedParam]

// 0.21.0
%[my-template, localParam->delegatedParam]
```

Parameter defaults previously used the named config syntax (`name:value`) and are now specified with `=`:

```msgfmt
// 0.20.1
%[my-template, paramName:'default value']
%[my-template, count:42]

// 0.21.0
%[my-template, paramName='default value']
%[my-template, count=42]
```

Named config elements (`configNamedElement`) are no longer supported inside templates.

### Parameter format syntax changed

The named formatter for a parameter is no longer specified as a positional second argument. It now uses the reserved
config keyword `format` and can appear in any order among the parameter's comma-separated elements.

Previously:

```msgfmt
%{myParam, string, clip:20}
```

Now:

```msgfmt
%{myParam, format:string, clip:20}
```

Additionally, configuration entries and map entries within a parameter can now appear in any order. In the old
syntax, the format had to come first, followed by configuration/map entries, followed by the default message.

### `clip-ellipsis` renamed to `clip-suffix`

The `clip-ellipsis` configuration parameter for the clip post formatter has been renamed to `clip-suffix`.
A new configuration parameter `clip-suffix-text` allows customizing the suffix text appended when clipping.

### Naming conventions enforced

The parser now validates naming conventions at parse time. Names that do not match the required convention will
produce a syntax error:

- **Parameter names** and **template parameter delegate names**: lower camelCase or kebab-case
- **Template names**, **parameter format names**, **post formatter names** and **config names**: kebab-case

Messages that previously used names not matching these conventions (e.g. underscores in parameter names or
uppercase letters in template names) will fail to parse.

### Tika detector no longer declared in `module-info.java`

The `requires static org.apache.tika.core` directive and the `provides org.apache.tika.detect.Detector` declaration
have been removed from `module-info.java`. The `PackTikaDetector` class still exists and is still registered via
the `META-INF/services/org.apache.tika.detect.Detector` service provider file, so it remains available on the
classpath. However, in a modular (JPMS) environment Tika will no longer automatically discover the detector through
the module system.

### `FormatterContext` removed

The `FormatterContext` class in `de.sayayi.lib.message.formatter` has been removed. Parameter formatters now receive
a `ParameterFormatterContext` (in `de.sayayi.lib.message.formatter.parameter`) which provides the same functionality
under a new type.

### `StreamFormatter` removed

The `StreamFormatter` has been removed. Formatting a `Stream` requires consuming it, which mutates the stream's
state and makes it unusable afterwards. This side effect is unacceptable in a formatting context. Collect the stream
into a list or array before passing it to the formatter.

### `SpacesUtil` removed

The utility class `SpacesUtil` in `de.sayayi.lib.message.util` has been removed. Its functionality (space detection,
string trimming) has been consolidated into the new `MessageUtil` class in the same package.

### `ParameterPart` and `part.parameter` packages no longer accessible

The `ParameterPart` class and the entire `de.sayayi.lib.message.part.parameter` package hierarchy
(`part.parameter`, `part.parameter.key`, `part.parameter.value`) are no longer exported from the module. The
parameter part API is now represented by the `MessagePart.Parameter`, `MessagePart.Config` and `MessagePart.Map`
interfaces in `de.sayayi.lib.message.part`.

### Dependency changes

| Dependency | Type | 0.20.1 | 0.21.0 |
|---|---|---|---|
| de.sayayi.lib:antlr4-runtime-ext | compile | [0.6,0.7) | [0.6,0.8) |
| de.sayayi.lib:pack | compile | [0.1.3,0.2) | [0.1.3,0.3) |
| org.springframework:spring-core | compile | [5.0,6.0) | [5.0,7.0) |
| org.springframework:spring-context | compile | [5.0,6.0) | [5.0,7.0) |


## New Features

### Post formatter message parts

A new message part type, post formatter, has been introduced. Post formatters transform the formatted text of a
contained message using a named post formatter. The syntax uses parentheses:

```msgfmt
%(case, 'hello world', case:'upper')
```

This applies the `case` post formatter to the message `hello world` with the configuration `case:'upper'`.
Post formatters implement the `PostFormatter` interface in `de.sayayi.lib.message.formatter.post` and are
discovered via `ServiceLoader`.

Two built-in post formatters are provided:

The `clip` post formatter clips text to a maximum length. It was previously available as a parameter post formatter
(`ClipPostFormatter`) and has been promoted to a standalone post formatter message part.

The `case` post formatter converts text to uppercase or lowercase based on the `case` configuration key.

### Compound message validation

The parser now validates the list of message parts for compound messages, rejecting invalid combinations at parse
time rather than at format time.

### `MessageBuilder` fluent API

A new fluent builder API allows constructing `Message` instances programmatically without parsing a format string.
Use `MessageBuilder.create()` or `MessageFactory.messageBuilder()` to obtain a builder:

```java
// builds: Hello %{name,format:string}!
Message.WithSpaces message = MessageBuilder.create()
    .text("Hello")
    .parameter("name")
        .withFormat("string")
        .spaceBefore()
    .text("!")
    .build();
```

The builder supports all four message part types: text, parameter, post formatter and template.

### `Message#asFormatString(Charset)`

Messages can now be serialized back into their message format string representation. This is useful for debugging
or exporting messages in a human-readable form:

```java
String formatString = message.asFormatString(StandardCharsets.UTF_8);
```

Characters that cannot be encoded by the specified charset are serialized as Unicode escape sequences.

### `Text.getTextNotNull()`

A new method `getTextNotNull()` on `MessagePart.Text` returns the trimmed text as a non-null `String`, returning
an empty string instead of `null` for empty text parts.

### Message caching in `MessageFactory`

`MessageFactory` now supports an optional LRU message cache to avoid repeated parsing of the same format string.
Enable it by passing a cache size to the constructor:

```java
MessageFactory factory = new MessageFactory(normalizer, 256);
```

When the cache is full, the least recently used entry is evicted.

### `classifier` named formatter

A new named formatter `classifier` selects a message from the parameter map based on classifiers associated with the
parameter value. This is useful for grouping values into categories and selecting display text accordingly.

### `DictionaryFormatter`

A new `DictionaryFormatter` handles `java.util.Dictionary` values by looking up entries using a key specified via
the `key` configuration parameter. This formatter takes priority over `MapFormatter` for `Dictionary` subtypes
(e.g. `Hashtable`) to ensure correct handling.

### `InstantSource` support in temporal delegation

The `TemporalFormatter` now handles the more generic `java.time.InstantSource` in addition to `java.time.Clock`
for temporal parameter delegation.

### Auto-application of named formatters

Named formatters can now declare that they should be automatically applied when a parameter's configuration contains
one of their config names, even without an explicit `format:` specification. This is controlled by the new
`autoApplyOnNamedConfigParameter()` method on `NamedParameterFormatter`. The `getFormatters` method on
`MessageAccessor` takes the parameter configuration into account when resolving formatters.

### Log4j integration module (`message-format-log4j`)

A new module `message-format-log4j` provides Log4j integration through `Log4jMessageFactory`. This allows using
message-format syntax in Log4j log statements:

```java
Logger logger = LogManager.getLogger(MyClass.class, new Log4jMessageFactory());
logger.info("Hello %{p1}, you have %{p2} new messages.", "World", 5);
```

Parameters are available as `p1`, `p2`, etc. in the message format. If the last parameter is a `Throwable`, it is
propagated as the message's throwable for Log4j stack trace rendering.

### `MessageUtil` utility class

A new utility class `MessageUtil` in `de.sayayi.lib.message.util` consolidates various message-related helper
methods, including `importMessages` (moved from `MessageSupport`), name validation, space character detection and
string trimming.



## Bug Fixes

- Fixed a missing closing quote on post formatter messages that produced incorrect serialization output.
- Fixed template serializer producing incorrect output for certain template configurations.
- Fixed `getParameterConfigNames()` returning incorrect results in some formatter implementations.
- Fixed number parsing to produce proper syntax errors for numbers out of the valid range.
- Fixed inconsistent handling of tab (`\t`) and carriage return (`\r`) characters in space detection.
