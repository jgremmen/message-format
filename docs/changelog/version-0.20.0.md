---
title: 0.12.0 -> 0.20.0
toc_depth: 2
---

# Version [0.20.0](https://github.com/jgremmen/message-format/tree/0.20.0) (2025-06-08)

## Breaking Changes

### Pack serialization replaced by `de.sayayi.lib:pack` library

The internal `PackInputStream` and `PackOutputStream` classes in `de.sayayi.lib.message.internal.pack`
have been removed. The message format pack serialization now uses the external `de.sayayi.lib:pack`
library. The classes `de.sayayi.lib.pack.PackInputStream` and `de.sayayi.lib.pack.PackOutputStream`
replace them.

`PackHelper` has been renamed to `PackSupport`. Both classes are internal and not part of the public API.

The pack file format version has been reset to `1`. Pack files created with version 0.12.0 are not
compatible with 0.20.0. Re-export all pack files after upgrading.

### `ClipFormatter` replaced by `ClipPostFormatter`

The named formatter `ClipFormatter` (`de.sayayi.lib.message.formatter.named.ClipFormatter`) has been
removed. Its functionality has been moved to the new post-formatter mechanism as `ClipPostFormatter`
in `de.sayayi.lib.message.formatter.post`.

In 0.12.0, clipping was a named formatter invoked with `%{value,clip,clip-size:64}`. In 0.20.0,
clipping is a post-formatter that activates when the `clip` config key is present on any parameter:

```java
// 0.12.0
"%{name,clip,clip-size:64}"

// 0.20.0
"%{name,clip:64}"
```

The `clip-ellipsis` boolean config key controls whether an ellipsis (`...`) is appended when the text
is clipped. It defaults to `true`. When enabled, the minimum clip size is 7.

### `LegacyToTemporalDelegate` renamed to `ToTemporalDelegate`

The class `de.sayayi.lib.message.formatter.runtime.LegacyToTemporalDelegate` has been renamed to
`de.sayayi.lib.message.formatter.runtime.ToTemporalDelegate`. Update all references accordingly.

`ToTemporalDelegate` now also supports `java.time.Clock` values by converting them to `Instant`
before delegating. In addition, `java.sql.Time` and `java.sql.Date` are now formatted using their
specific temporal types (`LocalTime`/`LocalDate`) instead of attempting instant conversion.

### `MessageSupportFactory.seal()` moved to `ConfigurableMessageSupport.seal()`

The static method `MessageSupportFactory.seal(MessageSupport)` has been removed along with the
internal `MessageSupportDelegate` class. Use the new default method `ConfigurableMessageSupport.seal()`
instead.

```java
// 0.12.0
MessageSupport sealed = MessageSupportFactory.seal(configurable);

// 0.20.0
MessageSupport sealed = configurable.seal();
```

### `ParameterConfig.getConfig()` replaced by `getConfigNames()` and `getDefaultValue()`

The method `ParameterConfig.getConfig()` which returned a `Map<String,ConfigValue>` has been removed.
Use `getConfigNames()` to obtain the set of named configuration keys and `getDefaultValue()` to
retrieve the default configuration value.

### `SizeQueryable` removed from `AbstractListFormatter`

`AbstractListFormatter` no longer implements `ParameterFormatter.SizeQueryable`. The `SizeQueryable`
interface has been moved to the concrete subclasses `ArrayFormatter`, `IterableFormatter` and
`MapFormatter`. `StreamFormatter` no longer implements `SizeQueryable` because calling `count()` on a
stream consumes it.

Code that relied on size-querying for streams must be adapted. Arrays, iterables and maps are unaffected
since the concrete classes still implement the interface.

### `ConfigKeyNull` and `ConfigKeyEmpty` changed to enums

`ConfigKeyNull` and `ConfigKeyEmpty` are now enum types with constants `EQ` and `NE` instead of
regular classes instantiated via constructor. Replace constructor calls:

```java
// 0.12.0
new ConfigKeyNull(CompareType.EQ)
new ConfigKeyEmpty(CompareType.NE)

// 0.20.0
ConfigKeyNull.EQ
ConfigKeyEmpty.NE
```

### `SpacesAware.isSpaceAround()` removed from internal message parts

The `isSpaceAround()` method has been removed from `NoSpaceTextPart`, `TextPart`, `TemplatePart` and
`ParameterPart`. These are internal classes. The method remains available on the `SpacesAware` interface
as a default method.

### `Message.WithSpaces` provides default implementations for `isSpaceBefore()` and `isSpaceAfter()`

The `Message.WithSpaces` interface now provides default implementations for `isSpaceBefore()` and
`isSpaceAfter()` that derive the values from the first and last message part respectively. Classes
implementing `Message.WithSpaces` that previously provided their own implementations should verify
compatibility with the new defaults.

### `Message.LocaleAware.getMessageParts()` throws `UnsupportedOperationException`

The `Message.LocaleAware` interface now provides a default `getMessageParts()` method that throws
`UnsupportedOperationException`. Locale-aware messages do not have a single set of message parts.

### `Message.getTemplateNames()` is now a default method

The `getTemplateNames()` method on the `Message` interface now has a default implementation that
returns an empty set. Implementations that previously returned `Set.of()` can remove their override.

### `MessageFactory.parseTemplate()` return type changed

The method `MessageFactory.parseTemplate(String)` now returns `Message.WithSpaces` instead of
`Message`. This is a source-compatible change for most callers but may require adjustments if the
return type was captured explicitly.

### Default pack filename changed

The default message format pack filename in the Gradle plugin has changed from `message.pack` to
`messages.mfp`. Update build configurations that rely on the default name.

### `MessageSupportMessageSource` constructor changes

The no-argument convenience constructor `MessageSupportMessageSource(MessageSupport)` now delegates
to a new two-argument constructor `MessageSupportMessageSource(MessageSupport, MessageSource)` instead
of directly passing a default prefix. The three-argument constructor
`MessageSupportMessageSource(String, MessageSupport, MessageSource)` now validates that
`parameterPrefix` starts with a letter.

### `FormatterServiceException` replaces `IllegalArgumentException` in `GenericFormatterService`

`GenericFormatterService` now throws `FormatterServiceException` (a subclass of `MessageException`)
instead of `IllegalArgumentException` when registering formatters that violate constraints, such as
associating a non-`DefaultFormatter` with `Object.class` or registering a formatter with an empty name.

### Dependency changes

| Dependency | Type | 0.12.0 | 0.20.0 |
|---|---|---|---|
| de.sayayi.lib:antlr4-runtime-ext | compile | [0.5,0.6) | [0.6,0.7) |
| de.sayayi.lib:pack | compile | - | [0.1,0.2) |


## New Features

### Post-formatter mechanism (`ParameterPostFormatter`)

A new `ParameterPostFormatter` interface allows modifying the formatted text of any parameter after
the primary formatter has produced its output. Post-formatters are triggered by the presence of a
specific config key in the parameter configuration. They are registered on the `FormatterService` and
discovered automatically via `ServiceLoader`.

```java
public final class ClipPostFormatter implements ParameterPostFormatter
{
  @Override
  public @NotNull String getParameterConfigName() {
    return "clip";
  }

  @Override
  public @NotNull Text postFormat(@NotNull FormatterContext context, @NotNull Text text) {
    // modify text ...
    return text;
  }
}
```

Post-formatters have a `getOrder()` method (default 80) that controls the execution order when
multiple post-formatters match the same parameter. The formatter service validates that parameter
config names used by post-formatters do not conflict with names used by regular formatters.

`DefaultFormatterService` loads post-formatters from the classpath via `ServiceLoader` alongside
regular parameter formatters.

The `FormatterService` interface has been extended with:

- `getParameterPostFormatters()` -- returns all registered post-formatters
- `getParameterConfigNames()` -- returns the accumulated set of config names from all formatters

The `FormatterService.WithRegistry` interface has been extended with:

- `addParameterPostFormatter(ParameterPostFormatter)` -- registers a post-formatter

### `ParameterFormatter.getParameterConfigNames()`

A new default method `getParameterConfigNames()` on `ParameterFormatter` returns the set of
config key names recognized by the formatter. This is used by the formatter service to detect
name conflicts between formatters and post-formatters. All built-in formatters now declare their
config names.

### `ConfigurableMessageSupport.seal()`

A new default method `seal()` on `ConfigurableMessageSupport` returns a `MessageSupport` wrapper
that hides the configurable interface. This replaces the static `MessageSupportFactory.seal()` method.

```java
var configurable = MessageSupportFactory.create(formatterService, messageFactory);
configurable.addMessage(myMessage);

MessageSupport sealed = configurable.seal();
```

### `MessageFactory.isSame(Message, Message)`

A new static method `MessageFactory.isSame(Message, Message)` compares two messages for structural
equality. It properly handles `LocaleAware` messages by comparing all locale-specific messages
individually and unwraps `MessageDelegateWithCode` instances. The `Message.isSame()` default method
now delegates to this static method.

### `ParameterConfig.getDefaultValue()`

A new method `getDefaultValue()` on `ParameterConfig` returns the default `ConfigValue` from the
parameter configuration map, or `null` if none is defined.

### `ParameterConfig.getConfigNames()`

A new method `getConfigNames()` returns an unmodifiable set of the named configuration keys defined
in the parameter configuration map.

### `PropertiesAdopter.adoptTemplates(Properties)`

A new method `adoptTemplates(Properties)` on `PropertiesAdopter` allows loading templates from a
`Properties` object. Each key is used as the template name and each value is parsed as a template.

### `FormattableType.DEFAULT` constant

A new public constant `FormattableType.DEFAULT` represents the default formattable type matching
`Object.class`. This replaces inline `new FormattableType(Object.class)` calls.

### `ToTemporalDelegate` supports `Clock`

The renamed `ToTemporalDelegate` (formerly `LegacyToTemporalDelegate`) now converts `java.time.Clock`
values to `Instant` before delegating to the temporal formatter chain.

### MIME type detection for message format packs

Message format pack files are now detected by the `java.nio.file.spi.FileTypeDetector` SPI and
optionally by Apache Tika. The MIME type is `application/x-message-format-pack`. The `PackFileTypeDetector`
and `PackTikaDetector` classes are registered as services in `module-info.java`.

### `PathFormatter` supports `mimetype` config value

The `PathFormatter` now supports the config value `mimetype`, which probes the content type of a
file path using `Files.probeContentType(Path)`.

### Improved parser error messages

The ANTLR message parser now generates context-specific error messages for common syntax errors
such as missing parameter names, incomplete parameters, duplicate config elements and missing template
names. This replaces the generic error messages produced by the ANTLR error handling.

### `FormatterServiceException`

A new `FormatterServiceException` (extending `MessageException`) is thrown by `GenericFormatterService`
for configuration-related errors such as conflicting formatter registrations.

### `Message.Parameters.hashCode()` contract

The `Message.Parameters` interface now formally specifies the `hashCode()` contract. The hash code is
defined as the sum of the hash codes of the locale, parameter name strings and parameter values.


## Bug Fixes

- Fixed `MessageParameters.equals()` to compare against any `Parameters` implementation instead of only `MessageParameters`.
- Fixed `MessageParameters.hashCode()` to produce consistent values across different `Parameters` implementations.
- Fixed `ConfigValueString.asMessage()` to be thread-safe by declaring the cached field `volatile` instead of using `synchronized`.
- Fixed `ToTemporalDelegate` to format `java.sql.Time` and `java.sql.Date` using their specific temporal types (`LocalTime`/`LocalDate`) instead of attempting instant conversion.
- Fixed `SpELFormatter` to use a per-instance `TypeLocator` that respects the configured `ClassLoader` instead of a shared static instance.
- Fixed `SpELFormatter` to throw `SpelEvaluationException` on variable assignment in `setVariable()` instead of silently ignoring it.
- Fixed redundant text part removal in the message compiler for empty space-around text parts.

