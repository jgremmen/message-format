---
title: 0.23.0 -> 0.24.0
toc_depth: 2
---

# [Version 0.24.0](https://github.com/jgremmen/message-format/tree/0.24.0) (2026-08-03)


## Breaking Changes

### Templates are now a dedicated type

The method `MessageSupport.TemplateAccessor.getTemplateByName(String)` now returns `Template` instead of `Message`.
Similarly, `MessageSupport.MessagePublisher.addTemplate(String, Message)` has been changed to
`addTemplate(String, Template)`. The `MessageFactory.parseTemplate(...)` methods now return `Template` instead of
`Message.WithSpaces`.

The new `Template` interface (in the new `de.sayayi.lib.message.template` package) represents a reusable template
that can be registered by name and referenced from messages. It provides a `formatAsText(MessageAccessor, Parameters)`
method and an `isSame(Template)` method for semantic equality.

To create a `Template` from a message format string:

```java
MessageFactory factory = MessageFactory.getSharedInstance();
Template template = factory.parseTemplate("Hello %{name}");
```

The `MessageBuilder` now also provides `buildAsTemplate()`:

```java
Template template = MessageBuilder
    .create()
    .text("Hello ")
    .parameter("name")
    .buildAsTemplate();
```

Custom templates can be implemented by extending `AbstractNamedTemplate` and discovered via the `ServiceLoader`
mechanism. Register them in `module-info.java`:

```java
provides de.sayayi.lib.message.template.NamedTemplate
    with com.example.MyCustomTemplate;
```

or via `META-INF/services/de.sayayi.lib.message.template.NamedTemplate`.

A new method `ConfigurableMessageSupport.registerTemplatesFromService(ClassLoader)` loads and registers all
`NamedTemplate` service providers.

### Template names must follow kebab-case naming convention

Calling `addTemplate(String, Template)` with a template name that does not conform to kebab-case (e.g.
`myTemplate` or `My_Template`) now throws an `IllegalArgumentException`. Existing code using camelCase or other
naming styles for template names must be updated to use kebab-case (e.g. `my-template`).

### Sealed `MessageSupport` interfaces

The following interfaces inside `MessageSupport` are now `sealed`:

- `ConfigurableMessageSupport` (permits `MessageSupportImpl`)
- `MessageConfigurer` (permits `MessageSupportImpl.Configurer`)
- `MessageAccessor` (permits `MessageSupportImpl.Accessor`)
- `TemplateAccessor` (permits `MessageAccessor`)
- `MessagePublisher` (permits `ConfigurableMessageSupport`)

`MessageSupportImpl` is now `final`. Code that previously subclassed `MessageSupportImpl` or implemented these
interfaces directly will no longer compile. Use the factory method `MessageSupportFactory.create(...)` to obtain
an instance of `ConfigurableMessageSupport`.

### `Message.EMPTY` constant removed

The public constant `Message.WithSpaces.EMPTY` has been removed. Use the new static method `Message.empty()`
instead:

```java
// Before:
Message.WithSpaces msg = Message.WithSpaces.EMPTY;

// After:
Message.WithSpaces msg = Message.empty();
```

### `AbstractMapKeyComparator` removed

The abstract class `AbstractMapKeyComparator` has been removed. Its sole purpose was to provide a `format()` method
that delegates to the next formatter. This behavior is now the default implementation in the
`MapKeyComparator.format(ParameterFormatterContext, Object)` interface method. Custom `MapKeyComparator`
implementations that previously extended `AbstractMapKeyComparator` should now implement `MapKeyComparator` directly;
no `format()` override is needed unless custom formatting behavior is desired.

### `ConfigAccessor.getConfigValueNumber` split into `getConfigValueInt` and `getConfigValueLong`

The method `getConfigValueNumber(String)` returning `OptionalLong` has been removed and replaced by two methods:

- `getConfigValueInt(String)` returning `OptionalInt` (returns empty if the value is outside integer range)
- `getConfigValueLong(String)` returning `OptionalLong`

```java
// Before:
var n = (int)context.getConfigValueNumber("size").orElse(10);

// After:
var n = context.getConfigValueInt("size").orElse(10);
```

### Map values in parameter configurations are now strictly messages

Map entry values in parameter configurations (`%{param, key:'value'}`) are now always stored as
`TypedValue.MessageValue`. Previously, simple string values in map entries were stored as `TypedValue.StringValue`
and converted lazily. This change affects the pack format (version incremented to 4) and makes the internal type
system more consistent. This library version can still read older pack files, but pack files generated with this
version cannot be read by older library versions.

The `MessagePart.Map.getMessage(...)` method now returns `Optional<Message.WithSpaces>` instead of a nullable
`Message.WithSpaces`.

### `MessagePart.Map.getDefaultMessage` signature change

The `MessageAccessor` parameter has been removed from `getDefaultMessage`:

```java
// Before:
map.getDefaultMessage(messageAccessor, keyType);

// After:
map.getDefaultMessage(keyType);
```

### `MessageUtil.importMessages` signature change

The static method `MessageUtil.importMessages(...)` now requires a `MessageFactory` as its first parameter, and
the template consumer accepts `Template` instead of `Message.WithSpaces`:

```java
// Before:
MessageUtil.importMessages(inputStream, msgConsumer, templateConsumer);

// After:
MessageUtil.importMessages(messageFactory, inputStream, 
                           msgConsumer, templateConsumer);
```

### `TextJoiner.addWithSpace(String)` renamed to `add(String)`

The method `TextJoiner.addWithSpace(String)` has been renamed to `add(String)`. The method retains the same
behavior of preserving leading and trailing spaces from the input string.

### `TemplateBuilder.withDefaultParameterXYZ` methods renamed

The following `TemplateBuilder` methods have been renamed to overloaded `withDefaultParameter(...)`:

| Old method                                                | New method                                         |
|-----------------------------------------------------------|----------------------------------------------------|
| `withDefaultParameterString(String, String)`              | `withDefaultParameter(String, String)`             |
| `withDefaultParameterBool(String, boolean)`               | `withDefaultParameter(String, boolean)`            |
| `withDefaultParameterNumber(String, long)`                | `withDefaultParameter(String, long)`               |
| `withDefaultParameterMessage(String, Message.WithSpaces)` | `withDefaultParameter(String, Message.WithSpaces)` |

### Gradle plugin DSL restructured

The `messageFormat` extension DSL has been restructured. The `duplicateMsgStrategy`, `validateReferencedTemplates`,
`includeRegexFilters` and `excludeRegexFilters` properties have been moved into nested `messages` and `templates`
blocks. The enum `DuplicateMsgStrategy` has been renamed to `DuplicateStrategy`.

```groovy
// Before:
messageFormat {
  duplicateMsgStrategy = 'fail'
  validateReferencedTemplates = true
  includeRegexFilter.add('xy')
  excludeRegexFilter.add('r.*')
}

// After:
messageFormat {
  messages {
    duplicateStrategy = 'fail'
    include 'xy'
    exclude 'r.*'
  }
  templates {
    validateReferences = true
  }
}
```

### Default pack filename changed

The default value of `messageFormat.packFilename` in the Gradle plugin is now `<project-name>.mfp` instead of the
fixed `messages.mfp`. Builds that rely on the previous filename must set it explicitly:

```groovy
messageFormat {
  packFilename = 'messages.mfp'
}
```

### Module `message-format-asm` removed

The `message-format-asm` module has been removed entirely. Its functionality has been merged into
`message-format-annotations`. The class `de.sayayi.lib.message.asm.adopter.AsmAnnotationAdopter` no longer exists.

The dependency must be replaced:

```groovy
// Before:
implementation 'de.sayayi.lib:message-format-asm:0.23.0'

// After (no additional dependency needed):
implementation 'de.sayayi.lib:message-format-annotations:0.24.0'
```

The replacement class is `de.sayayi.lib.message.annotation.adopter.AnnotationAdopter`, with identical constructor
signatures and scanning behavior:

```java
// Before:
import de.sayayi.lib.message.asm.adopter.AsmAnnotationAdopter;

var adopter = new AsmAnnotationAdopter(cms);
adopter.adopt(classLoader, Set.of("com.example"));

// After:
import de.sayayi.lib.message.annotation.adopter.AnnotationAdopter;

var adopter = new AnnotationAdopter(cms);
adopter.adopt(classLoader, Set.of("com.example"));
```

The ASM library (`org.ow2.asm:asm`) is now relocated and bundled inside `message-format-annotations` at build time.
It is no longer exposed as a transitive dependency. Code that previously obtained ASM classes transitively through
this library must now declare an explicit `org.ow2.asm:asm` dependency.

### Module `message-format-spring`: package `spring.adopter` removed

The `de.sayayi.lib.message.spring.adopter` package and its class `SpringAsmAnnotationAdopter` have been removed.
This class used Spring's bundled ASM to scan the classpath via a `ResourceLoader`. The replacement is the unified
`AnnotationAdopter` from `message-format-annotations`, which accepts a standard `ClassLoader`:

```java
// Before:
import de.sayayi.lib.message.spring.adopter.SpringAsmAnnotationAdopter;

var adopter = new SpringAsmAnnotationAdopter(cms);
adopter.adopt(resourceLoader, Set.of("com.example"));

// After:
import de.sayayi.lib.message.annotation.adopter.AnnotationAdopter;

var adopter = new AnnotationAdopter(cms);
adopter.adopt(
    resourceLoader.getClassLoader(),
    Set.of("com.example"));
```

The `byte-buddy` and `spring-context` transitive API dependencies have also been removed from
`message-format-annotations`. The `AnnotationAdopter` now works without any additional external dependencies beyond
the `message-format` core library.

### `Map<String,Object>` parameter methods widened to `Map<String,?>`

The following methods now accept `Map<String,?>` instead of `Map<String,Object>`:

- `Message.format(MessageAccessor, Map)`
- `Message.formatAsText(MessageAccessor, Map)`
- `MessageConfigurer.with(Map)`

Existing code compiles without changes since `Map<String,Object>` is assignable to `Map<String,?>`.

### Map entry parsing corrected

The internal grammar for parameter map entries has been corrected. Map entries are now parsed more strictly, and
their values are always typed as messages. Format strings that relied on undocumented parsing behavior may need
adjustment.

### Dependency changes

| Dependency                          | Type                 | Old version | New version              |
|-------------------------------------|----------------------|-------------|--------------------------|
| `de.sayayi.lib:message-format-asm`  | compile              | 0.23.0      | removed (merged [^3])    |
| `org.ow2.asm:asm`                   | compile              | [9.0,10.0)  | removed (bundled [^3])   |
| `de.sayayi.lib:pack`                | runtime              | [0.1.3,0.3) | [0.1.2,0.4)             |
| `org.springframework:spring-*` [^2] | compile              | [5.0,7.0)   | [6.0.8,7.1)             |
| `com.ibm.icu:icu4j` [^1]            | compile (ICU module) | -           | [74.1,79.0)             |

[^1]: The ICU4J dependency applies only to the new `message-format-icu` module.
[^2]: The Spring dependency applies only to the `message-format-spring` module.
[^3]: Functionality merged into `message-format-annotations`; ASM is relocated and no longer a public dependency.


## New Features

### New `message-format-icu` module

A new module `message-format-icu` provides ICU4J-based parameter formatters:

#### ICU Formatter (`icu`)

Formats parameter values using ICU `MessageFormat` patterns, supporting plurals, select expressions and
locale-aware number/date formatting. The formatter is triggered by the `icu` configuration key:

```
%{count, icu:'{count, plural, one {# item} other {# items}}'}
```

#### ICU Person Formatter (`icu-person`)

Formats person names with configurable control over name part visibility, ordering, formality and length using the
ICU4J `PersonNameFormatter` API.

### `Template` type and service discovery

Templates are now first-class types (see Breaking Changes above). The new `de.sayayi.lib.message.template` package
exports `Template`, `NamedTemplate` and `AbstractNamedTemplate`. Custom templates can be loaded automatically:

```java
cms.registerTemplatesFromService(getClass().getClassLoader());
```

### `AbstractAntlr4Parser` for message-format syntax errors

A new abstract class `de.sayayi.lib.message.util.AbstractAntlr4Parser` extends the ANTLR4 base parser with
message-format integration for syntax error reporting. It provides `syntaxErrorCode(String)` and
`syntaxErrorMessage(String)` builder methods that combine parameterized message formatting with syntax error
location tracking.

### ANTLR `Token` formatter

A new named formatter `token` formats ANTLR `Token` objects. It supports the following `token` config values:
`text`, `type`, `channel`, `line`, `column`, and `position`. The `position` mode formats line and column together
using a configurable `token-position-format` message.

### `@MessageDef` and `@TemplateDef` annotations on constructors

The `@MessageDef`, `@MessageDefs`, `@TemplateDef` and `@TemplateDefs` annotations can now be placed on constructors
in addition to types and methods.

### `Parameters.getParameterValueAsXXX(...)` convenience methods

The `Message.Parameters` interface now provides typed accessor methods:

```java
Optional<Boolean> getParameterValueAsBoolean(String)
OptionalInt getParameterValueAsInt(String)
OptionalLong getParameterValueAsLong(String)
Optional<String> getParameterValueAsString(String)
<T extends Enum<T>> Optional<T> getParameterValueAsEnum(String, Class<T>)
```

### `Message.asParameterMap()`

The `Parameters` interface now includes `asParameterMap()` returning an unmodifiable `Map<String,Object>` of all
parameter names and values. The existing `getParameterNames()` method is now a default method delegating to
`asParameterMap().keySet()`.

### `ConfigAccessor.getConfigValueEnum(...)`

A new method on `ConfigAccessor` retrieves a configuration value as an enum constant. The match is
case-insensitive and supports hyphenated names (e.g. `my-value` matches `MY_VALUE`):

```java
Optional<MyEnum> val = context.getConfigValueEnum("mode", MyEnum.class);
```

### Hexadecimal escape sequences in message format strings

The lexer now supports `\xHH` escape sequences (two hex digits) in addition to the existing `\uHHHH` (four hex
digits). For example, `\x20` represents a space character.

### Default map entry allowed anywhere in parameter definition

In previous versions, the default map entry (`:message`) had to be the last entry in a parameter definition. The
grammar now treats all parameter entries — format, config, map entries and the default map entry — uniformly, so
the default map entry can appear at any position.

```
// Before: default map entry was only allowed at the end
%{type, null:'n/a', :'unknown'}

// Now: default map entry can appear anywhere
%{type, :'unknown', null:'n/a'}
```

### Reduced quote escaping in format string serialization

The serializer now picks the quote character (single or double) that minimizes escape sequences in the output,
based on the relative frequency of quote characters in the string content.

### `MessageBuilder.create(MessageSupport)`

A new factory method creates a `MessageBuilder` using the `MessageFactory` obtained from the given
`MessageSupport` instance.

### `PostFormatterBuilder.withMessage(...)` methods

The post-formatter builder now provides `withMessage(Message.WithSpaces)` and `withMessage(String)` to set
the inner message directly without using a nested builder callback.

### `TemplateBuilder.withDefaultParameter(String, Consumer<MessageBuilder>)`

A new overload accepts a consumer callback for constructing the default parameter message value using a nested
builder.

### `MessagePart.Text.trim()` and `TextPartFactory.setSpaces(...)`

`Text.trim()` returns a copy of the text part with leading and trailing spaces removed.
`TextPartFactory.setSpaces(Text, boolean, boolean)` returns a text part with explicit space settings.

### `TextJoiner.add(String)` method

A new `add(String)` method on `TextJoiner` adds a string while preserving its leading and trailing spaces. This
replaces the former `addWithSpace(String)` method.


## Bug Fixes

- The `MessageFactory` message cache and the `LRUMessagePartNormalizer` (large variant) used a `LinkedHashMap`
  configured with access-order and called `computeIfAbsent` on it. In Java, `computeIfAbsent` on an access-ordered
  `LinkedHashMap` is considered a structural modification, which throws `ConcurrentModificationException` even under
  a lock if the map's internal state is mutated during the call. The fix replaces `computeIfAbsent` with an explicit
  `get`/`put` sequence and adds a `ReentrantLock` to the `LRUMessagePartNormalizer.Large` class.

- The `FormatterCache` used `synchronized` methods that held the monitor while invoking the potentially expensive
  `buildFormatters` function. This blocked all other threads attempting cache lookups for unrelated types. The fix
  replaces `synchronized` with a `ReentrantLock` and moves the `buildFormatters` invocation outside the lock,
  using a double-check pattern with a modification counter to handle concurrent inserts for the same type.

- The `GenericFormatterService` had no synchronization around mutations of `typeFormatters`, `postFormatters` and
  `parameterConfigNames`. Concurrent calls to `addFormatter` or `addFormatterForType` from multiple threads could
  corrupt these collections. All mutating and reading operations are now protected by a shared `ReentrantLock`.

- When constructing a `FormatterCache` with a capacity below 8, the internal array was allocated with size `n * 2`
  (where `n` is the requested capacity) but the effective capacity was clamped to a minimum of 8. This caused
  `ArrayIndexOutOfBoundsException` when the cache attempted to store entries beyond the undersized array. The array
  is now allocated using the clamped capacity.

- Unpacking localized templates from `.mfp` pack files failed because the unpack logic called
  `unpackMessageWithSpaces`, which expects a `Message.WithSpaces` wire format. Localized templates are stored as
  full `Message` instances (which may be `LocaleAware`). The fix switches to `unpackMessage` and widens the template
  consumer type to accept `Message` instead of `Message.WithSpaces`.

- In the `GenericFormatterService.addFormatterForType` method, the `formattableType.getType()` call was performed
  inside the `computeIfAbsent` key expression after the null-check on `formattableType`. If the type was `Object`
  and the formatter did not implement `DefaultFormatter`, the exception message referenced `formattableType` before
  it was dereferenced, causing a `NullPointerException` when `formattableType` was `null`. The fix extracts the type
  into a local variable early.

- When a template parameter delegate mapped a parameter to itself (e.g. `name->name`), the parser stored this as
  an explicit delegation entry. At format time, the template resolver followed the delegation and re-resolved the
  same parameter, resulting in infinite recursion. The fix skips storing delegations where the source and target
  parameter names are identical, treating them as no-ops.

- The `MessageBuilder`'s parameter, template and post-formatter sub-builders could flush their accumulated state
  multiple times if accessed after the builder had already moved on to a subsequent part. Each `flush()` call
  appended a duplicate `MessagePart` to the parts list. The fix introduces a `flushed` flag in each sub-builder
  that prevents repeated flushes.

- The list and array formatters (`IterableFormatter`, `ArrayFormatter`) formatted element values via
  `Message.format(...)` which returns a plain `String`, then wrapped the result with `noSpaceText(...)`. This
  discarded any leading or trailing spaces that were part of the element's formatted output. Additionally, the
  `UniqueTextIterator` used `Text` object equality for deduplication, causing elements with identical text content
  but different space flags to be treated as distinct. The fix uses `formatAsText(...)` to preserve space
  information and compares the raw text content for uniqueness.
