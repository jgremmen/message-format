---
title: 0.11.0 -> 0.12.0
toc_depth: 2
---

# Version [0.12.0](https://github.com/jgremmen/message-format/tree/0.12.0) (2025-01-06)

## Breaking Changes

### Modularization (JPMS)

All modules now contain a `module-info.java` descriptor. The following modules are defined:

- `de.sayayi.lib.message` (message-format)
- `de.sayayi.lib.message.annotations` (message-format-annotations)
- `de.sayayi.lib.message.asm` (message-format-asm)
- `de.sayayi.lib.message.spring` (message-format-spring)

Classes in packages not listed in the module descriptor are no longer accessible. In particular, the
following packages have been moved to internal packages and are no longer part of the public API:

- `de.sayayi.lib.message.pack` has moved to `de.sayayi.lib.message.internal.pack`
- `de.sayayi.lib.message.parser` has moved to `de.sayayi.lib.message.internal.parser`

Code that directly references `MessageCompiler`, `PackHelper`, `PackInputStream`, `PackOutputStream` or
any other class from these packages must be updated. If you were using `MessageCompiler` directly, use
the `MessageFactory` API instead.

The `NoSpaceTextPart`, `TextPart` and `TemplatePart` classes have moved from `de.sayayi.lib.message.part`
to `de.sayayi.lib.message.internal.part`. These classes are implementation details and should not be
referenced directly.

### Annotations and ASM classes moved to separate modules

The annotation-related classes and the ASM-based adopter have been extracted into dedicated modules:

- **message-format-annotations** - contains `MessageDef`, `MessageDefs`, `TemplateDef`, `TemplateDefs`,
  `Text` annotations and `AbstractAnnotationAdopter`. The package has changed from
  `de.sayayi.lib.message.annotation` / `de.sayayi.lib.message.adopter` to
  `de.sayayi.lib.message.annotation` (consolidated).

- **message-format-asm** - contains `AsmAnnotationAdopter`. The package has changed from
  `de.sayayi.lib.message.adopter` to `de.sayayi.lib.message.asm.adopter`.

Projects using `AsmAnnotationAdopter` must add a dependency on `message-format-asm` (which transitively
depends on `message-format-annotations`). Projects using only the annotations must add a dependency on
`message-format-annotations`.

```groovy
// before
implementation "de.sayayi.lib:message-format:0.11.0"

// after
implementation "de.sayayi.lib:message-format:0.12.0"
implementation "de.sayayi.lib:message-format-asm:0.12.0"
```

### Spring module package restructuring

All classes in the `message-format-spring` module have moved to new packages:

| Class | 0.11.0 | 0.12.0 |
|---|---|---|
| `MessageSupportMessageSource` | `de.sayayi.lib.message` | `de.sayayi.lib.message.spring` |
| `SpringAsmAnnotationAdopter` | `de.sayayi.lib.message.adopter` | `de.sayayi.lib.message.spring.adopter` |
| `SpELFormatter` | `de.sayayi.lib.message.formatter.spring` | `de.sayayi.lib.message.spring.formatter` |

All import statements for these classes must be updated accordingly.

### Joda-Time support removed

The `message-format-jodatime` module has been removed entirely. The `JodaDateTimeFormatter` is no longer
available. Use the `java.time` API instead. The new `TemporalFormatter` handles all `java.time.temporal.Temporal`
types, and legacy `java.util.Date` / `java.util.Calendar` objects are automatically converted via
`LegacyToTemporalDelegate`.

### Date/time formatter redesign

`DateFormatter` (for `java.util.Date`) and `Java8DateTimeFormatter` (for `java.time.temporal.Temporal`) have
been removed and replaced by:

- `TemporalFormatter` - formats any `Temporal` value using localized or custom `DateTimeFormatter` patterns.
  The `date` config key controls the output style: `short`, `medium`, `long`, `full`, `date`, `time` or a
  custom `DateTimeFormatter` pattern string. The formatter automatically detects whether the temporal value
  supports date fields, time fields, or both, and selects the appropriate format.

- `LegacyToTemporalDelegate` - converts `Calendar`, `Date`, `java.sql.Date`, `java.sql.Time` and
  `java.nio.file.attribute.FileTime` into their `Temporal` equivalents and delegates formatting to the
  next formatter in the chain.

Code that relied on the `DateFormatter` or `Java8DateTimeFormatter` classes directly must switch to
`TemporalFormatter`.

### JCache message part normalizer removed

`JCacheMessagePartNormalizer` has been removed. The `javax.cache:cache-api` dependency is no longer part of
the project. Use `LRUMessagePartNormalizer` instead, which has moved from
`de.sayayi.lib.message.parser.normalizer` to `de.sayayi.lib.message.part.normalizer`.

```java
// before
import de.sayayi.lib.message.parser.normalizer.JCacheMessagePartNormalizer;
MessagePartNormalizer normalizer = new JCacheMessagePartNormalizer(cache);

// after
import de.sayayi.lib.message.part.normalizer.LRUMessagePartNormalizer;
MessagePartNormalizer normalizer = new LRUMessagePartNormalizer(256);
```

### ASM dependency removed from message-format

The optional `org.ow2.asm:asm` dependency has been removed from the `message-format` module. ASM is now
a compile dependency of the new `message-format-asm` module only.

### Dependency changes

| Dependency | Type | 0.11.0 | 0.12.0 |
|---|---|---|---|
| org.ow2.asm:asm | compile | [9.0,10.0) (optional in message-format) | [9.0,10.0) (in message-format-asm) |
| javax.cache:cache-api | compile | 1.1.1 (optional in message-format) | removed |
| joda-time:joda-time | compile | [2.12.0,) (in message-format-jodatime) | removed |
| org.jetbrains:annotations | compile | 25.0.0 | [24.0,26.1) |

### `MessagePartNormalizer` package change

`MessagePartNormalizer` and `LRUMessagePartNormalizer` have moved from `de.sayayi.lib.message.parser.normalizer`
to `de.sayayi.lib.message.part.normalizer`. Update all import statements.

### `Message.isSame()` is now a default method

The `isSame(Message)` method on the `Message` interface now has a default implementation. Classes that
previously implemented this method may need to verify compatibility with the new default behavior, which
compares message parts using `Arrays.equals(getMessageParts(), message.getMessageParts())` and unwraps
`MessageDelegateWithCode` instances.

### `ValueParameters` renamed to `SingletonParameters`

The class `de.sayayi.lib.message.formatter.runtime.ValueParameters` has been renamed to
`de.sayayi.lib.message.formatter.SingletonParameters` and moved to the `formatter` package.

### List formatter refactoring

`ArrayFormatter` and `IterableFormatter` now extend the new `AbstractListFormatter` base class. The
list-related config key constants (`list-sep`, `list-sep-last`, `list-max-size`) are defined in
`AbstractListFormatter`. Code that referenced these keys as string literals is unaffected, but code
extending these formatters must adapt to the new class hierarchy.


## New Features

### `MessageSupportFactory.seal()`

A new static method `MessageSupportFactory.seal(MessageSupport)` returns a `MessageSupport` wrapper that
hides the `ConfigurableMessageSupport` interface. This prevents callers from modifying a message support
instance. The shared message support returned by `MessageSupportFactory.getSharedInstance()` now uses this
mechanism instead of throwing `UnsupportedOperationException` on configuration methods.

```java
var configurable = MessageSupportFactory.create(formatterService, messageFactory);
configurable.addMessage(myMessage);

MessageSupport sealed = MessageSupportFactory.seal(configurable);
// sealed does not expose addMessage(), setLocale(), etc.
```

### `ParameterConfig.getConfig()`

A new method `getConfig()` on `ParameterConfig` returns an unmodifiable `Map<String,ConfigValue>` of all
named configuration entries for a message parameter. This is useful for inspecting or debugging parameter
configurations at runtime.

### `AbstractMultiSelectFormatter`

A new abstract base class `AbstractMultiSelectFormatter<T>` simplifies the implementation of formatters
that select a formatting strategy based on a single config key value. Subclasses register named functions
via `register(String, MultiSelectFunction)` and the base class dispatches to the matching function at
format time.

```java
public class MyFormatter extends AbstractMultiSelectFormatter<MyType> {
  public MyFormatter() {
    super("my-config-key");
    register("option-a", (context, value) -> noSpaceText(value.toA()));
    register("option-b", (context, value) -> noSpaceText(value.toB()));
  }
}
```

### `AbstractListFormatter`

A new abstract base class `AbstractListFormatter<T>` provides shared logic for formatting list-like
structures (arrays, iterables, streams). It supports the config keys `list-sep` (separator between
elements), `list-sep-last` (separator before the last element) and `list-max-size` (maximum number
of elements to render).

### New formatters

#### `CharsetFormatter`

Formats `java.nio.charset.Charset` values. Uses the `AbstractMultiSelectFormatter` mechanism with
config key support for selecting the output representation.

#### `MapEntryFormatter`

Formats `java.util.Map.Entry` values. Uses the `AbstractMultiSelectFormatter` mechanism to select
whether to display the key, value, or both.

#### `StreamFormatter`

Formats `java.util.stream.Stream` values as lists, extending `AbstractListFormatter`. The stream
is consumed during formatting. Supports the same list config keys as `ArrayFormatter` and
`IterableFormatter`.

#### `TemporalFormatter`

See the "Date/time formatter redesign" section under Breaking Changes for details.

#### `LegacyToTemporalDelegate`

Converts legacy date/time types (`Calendar`, `Date`, `java.sql.Date`, `java.sql.Time`, `FileTime`)
to their `java.time` equivalents and delegates to the next formatter. This replaces the removed
`DateFormatter`.

#### `AnnotationFormatter`

Formats `java.lang.annotation.Annotation` instances by rendering their type and element values.
Located in the `formatter.runtime.extra` package and loaded via the service loader mechanism.

#### `ManifestFormatter`

Formats `java.util.jar.Manifest` values. Uses the `AbstractMultiSelectFormatter` mechanism.
Located in the `formatter.runtime.extra` package.

### `Message.EMPTY` constant

A public constant `Message.WithSpaces EMPTY` has been added to the `Message` interface, providing a
shared empty message instance. This replaces direct references to the internal `EmptyMessage.INSTANCE`.

### `SingletonParameters`

The new `SingletonParameters` class (formerly `ValueParameters`) implements `Message.Parameters` for
the common case where only a single parameter value needs to be passed for formatting.


## Bug Fixes

- Fixed hashcode and clone overrides in internal data structures.
- Improved ANTLR parser performance by configuring a missing heap exit-rule walker.
- Fixed `isSpaceChar` inconsistency regarding `\t` and `\r` characters.
- Fixed template serializer for packed message export/import.
- Fixed proper syntax error generation for numbers out of range in parameter configuration.

