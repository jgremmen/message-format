---
title: 0.23.0 -> 0.24.0
toc_depth: 2
---

# Version [0.24.0](https://github.com/jgremmen/message-format/tree/0.24.0) (2026-07-28)


## Breaking Changes


### Template type replaces `Message` for template registration

Templates are no longer represented as plain `Message` instances. A new `Template` interface (in the
`de.sayayi.lib.message.template` package) replaces `Message` wherever templates are registered or
accessed.

The following method signatures have changed:

```java
// Before (0.23.0)
ConfigurableMessageSupport addTemplate(String name, Message template);
Message getTemplateByName(String name);

// After (0.24.0)
ConfigurableMessageSupport addTemplate(String name, Template template);
Template getTemplateByName(String name);
```

To convert existing `Message.WithSpaces` instances for use as templates, call `MessageBuilder.buildAsTemplate()`
or wrap your message in a `Template` implementation. A `MessageBuilder` now provides the `buildAsTemplate()` method
to produce a `Template` directly.


### Sealed `MessageSupport` interfaces

The following interfaces are now `sealed`:

- `MessageSupport.MessageConfigurer`
- `MessageSupport.ConfigurableMessageSupport`
- `MessageSupport.MessageAccessor`
- `MessageSupport.TemplateAccessor`
- `MessageSupport.MessagePublisher`

Code that directly implements any of these interfaces will no longer compile. Use the provided factory
methods (`MessageSupportFactory.create(...)`) and the built-in implementations instead.


### `Message.EMPTY` constant removed

The public constant `Message.EMPTY` has been removed. Use the new static factory method instead:

```java
// Before
Message.WithSpaces empty = Message.EMPTY;

// After
Message.WithSpaces empty = Message.empty();
```


### `AbstractMapKeyComparator` removed

The abstract class `AbstractMapKeyComparator<T>` has been removed. The `MapKeyComparator` interface now provides
a default `format(...)` method that delegates to the next formatter in the chain. Implementations that extended
`AbstractMapKeyComparator` should implement `MapKeyComparator` directly; no additional method override is needed
since the default `format(...)` method already provides the delegation behavior.


### `ConfigAccessor.getConfigValueNumber(...)` split into two methods

The method `getConfigValueNumber(String)` returning `OptionalLong` has been removed and replaced by two methods:

```java
// Before
OptionalLong getConfigValueNumber(String name);

// After
OptionalLong getConfigValueLong(String name);
OptionalInt getConfigValueInt(String name);
```

The `getConfigValueInt(...)` variant returns `OptionalInt.empty()` if the stored number exceeds the integer range.


### `MessagePart.Map.getMessage(...)` now returns `Optional`

The method `getMessage(...)` on `MessagePart.Map` now returns `Optional<Message.WithSpaces>` instead of a
nullable `Message.WithSpaces`:

```java
// Before
Message.WithSpaces getMessage(MessageAccessor messageAccessor, 
    Object key, Locale locale, Set<MapKey.Type> keyTypes, 
    boolean includeDefault, MessagePart.Config config);

// After
Optional<Message.WithSpaces> getMessage(MessageAccessor messageAccessor,
    Object key, Locale locale, Set<MapKey.Type> keyTypes, 
    boolean includeDefault, MessagePart.Config config);
```


### `MessagePart.Map.getDefaultMessage(...)` signature changed

The `MessageAccessor` parameter has been removed:

```java
// Before
Optional<Message.WithSpaces> getDefaultMessage(
    MessageAccessor messageAccessor, MapKey.Type keyType);

// After
Optional<Message.WithSpaces> getDefaultMessage(MapKey.Type keyType);
```


### `Parameters.getParameterNames()` replaced by `asParameterMap()`

The abstract method `getParameterNames()` on `Message.Parameters` is no longer abstract. It is now a default
method that delegates to the new abstract method `asParameterMap()`:

```java
// New abstract method that implementations must provide
@Unmodifiable Map<String,Object> asParameterMap();
```

Existing `Parameters` implementations must add `asParameterMap()`. The `getParameterNames()` method remains
available but is derived from the map's key set.


### `TextJoiner.addWithSpace(String)` removed

The method `addWithSpace(String)` has been removed from `TextJoiner`. Use `add(String)` instead, which preserves
leading and trailing spaces from the input string.


### Map entry parsing restructured

The internal grammar for parameter map entries has been restructured. Map entries (key-value pairs), config
definitions, format specifications and the default entry can now appear in any order within the parameter
definition. Previously, map entries had to follow config/format definitions and the default entry had to be last.

This change affects custom code that programmatically constructs `MessagePartMap` instances: map values are now
always stored as `TypedValue.MessageValue` instead of `TypedValue<?>`. Plain string values are automatically
wrapped as messages during parsing.


### Pack format version upgraded to 4

The binary pack format has been upgraded from version 3 to version 4. Map values are now packed as messages
rather than as typed values. Pack files created with version 0.24.0 cannot be read by older library versions.
The library still reads pack files of version 3.


### `message-format-asm` module removed

The `message-format-asm` module (containing `AsmAnnotationAdopter`) has been removed. Its functionality has been
merged into the `message-format-annotations` module. The ASM dependency is now bundled (shaded) so there is no
additional runtime dependency required.

If you previously depended on `message-format-asm`, replace it with `message-format-annotations`:

```groovy
// Before
implementation 'de.sayayi.lib:message-format-asm:0.23.0'

// After
implementation 'de.sayayi.lib:message-format-annotations:0.24.0'
```

The `AnnotationAdopter` class is now located at `de.sayayi.lib.message.annotation.adopter.AnnotationAdopter`.


### `SpringAsmAnnotationAdopter` removed

The `de.sayayi.lib.message.spring.adopter` package and its `SpringAsmAnnotationAdopter` class have been removed.
Use `AnnotationAdopter` from the `message-format-annotations` module instead.


### Template names must follow kebab-case

Registering a template with a name that does not follow the kebab-case naming convention now throws
`IllegalArgumentException`. Previously, any string was accepted as a template name.


### Default `packFilename` changed

The Gradle plugin default for `packFilename` has changed from `messages.mfp` to `${project.name}.mfp`.
If your build relies on the old default filename, set it explicitly:

```groovy
messageFormat {
  packFilename = 'messages.mfp'
}
```


### Gradle plugin DSL restructured

The `messageFormat` extension has been restructured. Message filtering and duplicate strategy configuration
have moved into a nested `messages` block, and template validation has moved into a nested `templates` block:

```groovy
// Before (0.23.0)
messageFormat {
  include 'MSG-.*'
  exclude 'INTERNAL-.*'
  duplicateMsgStrategy = 'fail'
  validateReferencedTemplates = true
}

// After (0.24.0)
messageFormat {
  messages {
    include 'MSG-.*'
    exclude 'INTERNAL-.*'
    duplicateStrategy = 'fail'
  }
  templates {
    validateReferences = true
    ignore 'tpl-.*'
  }
}
```

The `getIncludeRegexFilters()`, `getExcludeRegexFilters()`, `getDuplicateMsgStrategy()` and
`getValidateReferencedTemplates()` properties on the extension and the task have been removed.


### `TemplateBuilder.withDefaultParameterXYZ(...)` methods renamed

The following methods on `TemplateBuilder` have been renamed to overloaded `withDefaultParameter(...)`:

| Before                                                    | After                                              |
|-----------------------------------------------------------|----------------------------------------------------|
| `withDefaultParameterString(String, String)`              | `withDefaultParameter(String, String)`             |
| `withDefaultParameterBool(String, boolean)`               | `withDefaultParameter(String, boolean)`            |
| `withDefaultParameterNumber(String, long)`                | `withDefaultParameter(String, long)`               |
| `withDefaultParameterMessage(String, Message.WithSpaces)` | `withDefaultParameter(String, Message.WithSpaces)` |


### `exportMessages(...)` requires additional parameter

The method `exportMessages(OutputStream, boolean, Predicate<String>)` now requires a fourth parameter for
template name filtering:

```java
// Before
void exportMessages(OutputStream stream, boolean compress,
   Predicate<String> messageCodeFilter);

// After
void exportMessages(OutputStream stream, boolean compress,
    Predicate<String> messageCodeFilter, 
    Predicate<String> templateNameFilter);
```

Pass `null` for `templateNameFilter` to include all templates referenced by the selected messages.


### Dependency changes

| Dependency                          | Type    | 0.23.0      | 0.24.0            |
|-------------------------------------|---------|-------------|-------------------|
| `com.ibm.icu:icu4j` [^1]            | compile | -           | [74.1,79.0)       |
| `org.ow2.asm:asm`                   | compile | [9.0,10.0)  | removed (bundled) |
| `de.sayayi.lib:antlr4-runtime-ext`  | runtime | [0.6,0.8)   | [0.6,0.8)         |
| `org.springframework:spring-*` [^2] | compile | [5.0,7.0)   | [6.0.8,7.1)       |
| `de.sayayi.lib:message-format-pack` | compile | [0.1.3,0.3) | [0.1.2,0.4)       |

[^1]: The ICU4J dependency applies only to the new `message-format-icu` module.
[^2]: The Spring dependency applies only to the `message-format-spring` module.


## New Features


### New `message-format-icu` module

A new module `message-format-icu` provides ICU4J-based parameter formatters:

#### `ICUFormatter` (name: `icu`)

Formats parameter values using ICU `MessageFormat` patterns. The ICU pattern is specified via the `icu`
configuration key:

```
%{amount,format:icu,icu:'{amount, number, currency}'}
```

All parameters available in the formatting context are passed to the ICU message format as named arguments.
The formatter uses the context locale for locale-sensitive formatting.

#### `ICUPersonFormatter` (name: `icu-person`)

Formats person names using the ICU `PersonNameFormatter`. Name parts are read from parameters named
`given-name`, `family-name`, `middle-name`, `prefix` and `suffix`. Configuration keys control formatting:

```
%{unused,format:icu-person,given-format:initial,
         family-format:full,formality:formal}
```


### `Template` type and service discovery

Templates can now be implemented as Java classes that extend `AbstractNamedTemplate`. They are discovered
automatically via the `ServiceLoader` mechanism:

```java
public class MyTemplate extends AbstractNamedTemplate
{
  @Override
  public @NotNull String getName() {
    return "my-template";
  }

  @Override
  public @NotNull Text formatAsText(
      @NotNull MessageAccessor messageAccessor, 
      @NotNull Parameters parameters)
  {
    // custom formatting logic
    return noSpaceText("formatted result");
  }
}
```

Register discovered templates with:

```java
configurableMessageSupport.registerTemplatesFromService(
    MyTemplate.class.getClassLoader());
```

Declare the provider in `module-info.java`:

```java
provides de.sayayi.lib.message.template.NamedTemplate 
    with com.example.MyTemplate;
```

Or, for non-modular projects, create a file `META-INF/services/de.sayayi.lib.message.template.NamedTemplate`
containing the fully qualified class name:

```
com.example.MyTemplate
```


### `Parameters.getParameterValueAsXYZ(...)` convenience methods

The `Message.Parameters` interface provides new default methods for typed parameter access:

- `getParameterValueAsBoolean(String)` returns `Optional<Boolean>`
- `getParameterValueAsInt(String)` returns `OptionalInt`
- `getParameterValueAsLong(String)` returns `OptionalLong`
- `getParameterValueAsEnum(String, Class<T>)` returns `Optional<T>`
- `getParameterValueAsString(String)` returns `Optional<String>`


### `ConfigAccessor.getConfigValueEnum(...)`

A new method retrieves configuration values as enum constants:

```java
Optional<MyEnum> value = configAccessor
    .getConfigValueEnum("key", MyEnum.class);
```


### `@MessageDef` and `@TemplateDef` on constructors

The `@MessageDef` and `@TemplateDef` annotations can now be placed on constructors in addition to types and
methods. The `AnnotationAdopter` scans constructor annotations as well.


### Hexadecimal escape sequences in message format strings

The lexer now supports `\xHH` escape sequences (two hex digits) in addition to the existing `\uHHHH` (four hex
digits):

```
'Hello \x41 World'  // produces "Hello A World"
```


### `MessagePart.Text.trim()`

A new `trim()` method on `MessagePart.Text` returns a trimmed copy with leading/trailing space flags removed.


### `TextJoiner.add(String)` and `TextJoiner.add(char[])`

`TextJoiner` gains an `add(String)` method that preserves leading/trailing spaces from the input string,
and an `add(char[])` method that processes each character individually with space collapsing.


### `PostFormatterBuilder.withMessage(...)` methods

The `PostFormatterBuilder` now provides `withMessage(String)` and `withMessage(Message.WithSpaces)` to set
the inner message directly instead of requiring a consumer callback.


### `TemplateBuilder.withDefaultParameter(String, Consumer<MessageBuilder>)`

A new overload accepts a `Consumer<MessageBuilder>` to construct the default message parameter value using
the builder API.


### `MessageBuilder.buildAsTemplate()`

Builds the message directly as a `Template` instance for registration.


### `Message.Parameters.asParameterMap()`

Returns all parameters as an unmodifiable `Map<String,Object>`, giving direct access to all parameter
names and values in a single call.


### Reduced quote escaping in format string serialization

The `asFormatString(...)` serialization now chooses the wrapping quote character (single or double) based on
which character appears less frequently in the text content, reducing the number of escape sequences in the
serialized output.


## Bug Fixes

- `FormatterCache` used `synchronized` methods, which meant the `buildFormatters` function (potentially expensive)
  was invoked while holding the lock, blocking all other threads from cache lookups. The cache now uses a
  `ReentrantLock` with a lock-release-build-reacquire pattern: the lock is released before calling
  `buildFormatters` and reacquired afterwards, with a modification counter to detect concurrent insertions and
  avoid redundant type lookups on re-entry.

- `FormatterCache` allocated its internal array based on the constructor argument `n` directly
  (`new Object[n * 2]`), but the minimum capacity is clamped to 8. When `n < 8`, the array was undersized
  relative to the actual capacity, causing `ArrayIndexOutOfBoundsException` when the cache filled up. The
  array is now allocated using the clamped capacity.

- `GenericFormatterService` methods `addFormatter(...)`, `addFormatterForType(...)`, `addPostFormatter(...)`,
  `getPostFormatters()` and `getParameterConfigNames()` were not thread-safe. Concurrent calls to register
  formatters while other threads were resolving formatters could corrupt internal data structures. All mutating
  and read operations are now protected by a shared `ReentrantLock`.

- `GenericFormatterService.addFormatterForType(...)` called `requireNonNull` on `formattableType` inside the
  `computeIfAbsent` lambda, after already dereferencing `formattableType.getType()` on the line above. If
  `formattableType` was `null`, a `NullPointerException` was thrown on the `getType()` call with no descriptive
  message. The `requireNonNull` check is now performed before the first use.

- `MessageFactory` and `LRUMessagePartNormalizer` used `LinkedHashMap` configured with access-order and called
  `computeIfAbsent(...)` on it. In access-order mode, `computeIfAbsent` structurally modifies the map (to move the
  accessed entry to the end), which triggers a `ConcurrentModificationException` from within the same call. Both
  caches now use explicit `get`/`put` sequences instead of `computeIfAbsent`, and `LRUMessagePartNormalizer`
  additionally protects access with a `ReentrantLock`.

- The list formatters (`ArrayFormatter`, `IterableFormatter`) called `noSpaceText(...)` on the formatted text of
  each element, stripping any leading/trailing space information from the resulting `Text` parts. When the unique
  text deduplication iterator compared `Text` objects, two entries with the same text content but different space
  flags were treated as distinct. The formatters now use `formatAsText(...)` to preserve space information, and
  the deduplication set compares on the raw text string rather than the `Text` object.

- In a template part definition like `%[tmpl,p->p]`, where a parameter is delegated to itself (`p->p`), the
  compiler silently accepted the delegation, which at format time caused infinite recursion. Self-delegations
  are now detected during compilation and silently ignored.

- Unpacking templates from a pack file always called `unpackMessageWithSpaces(...)`, which failed for locale-aware
  templates (which implement `Message.LocaleAware`, not `Message.WithSpaces`). The unpacker now calls the more
  general `unpackMessage(...)` method, which handles both message types.

- The `MessageBuilder` sub-builders for parameter, template and post-formatter parts did not guard against
  double-flushing. When a builder method like `withMessage(Consumer)` triggered an early flush (by building the
  inner message), and then the parent builder flushed again during `build()`, the same part was added to the
  message parts list twice. Each sub-builder now tracks a `flushed` flag to ensure the part is added exactly once.
