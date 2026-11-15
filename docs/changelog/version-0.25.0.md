---
title: 0.24.0 -> 0.25.0
toc_depth: 2
---

# [Version 0.25.0](https://github.com/jgremmen/message-format/tree/0.25.0) (2026-11-15)


## Breaking Changes

### `number:integer` formats integer types with grouping

For `number:integer`, the number formatter rendered integer types (`Integer`, `Long`, `BigInteger` and the atomic and
accumulator types) with their plain `toString()` representation, while floating point values and `BigDecimal` were
formatted with the locale-specific integer format. For `Locale.US`, `1234567L` was rendered as `1234567` and
`1234567.0` as `1,234,567`.

`number:integer` now applies the locale-specific integer format to all numeric types. The previous output for integer
types is available as the new value `number:plain`, which is also the behavior when the `number` configuration key is
absent:

```java
// before: "1234567", now: "1,234,567"
messageSupport
    .message("%{v,number:integer}")
    .with("v", 1234567)
    .locale(Locale.US)
    .format();

// "1234567"
messageSupport
    .message("%{v,number:plain}")
    .with("v", 1234567)
    .locale(Locale.US)
    .format();
```

Messages that use `number:integer` to suppress grouping separators for integer values must use `number:plain`
instead.

### String configuration values are no longer parsed as messages

Formatters that expect a message for a configuration key, such as `list-value` or `map-kv`, also accept a string
value. Such a string value was parsed as a message format string when it was used. A string value is now used as
plain text and is never parsed. This applies to string values in format strings, to `configString(...)` of the message
builder and to `ConfigurableMessageSupport.setDefaultConfig(String, String)`:

```java
// before: the default was parsed, "%{value}" referenced a parameter
// now: the default is the literal text "<%{value}>"
messageSupport.setDefaultConfig("list-value", "<%{value}>");

// a configuration value which must be a message
messageSupport.setDefaultConfig("list-value",
    MessageFactory.getSharedInstance().parseMessage("<%{value}>"));
```

In a format string, a quoted value containing message syntax, like `list-value:'<%{value}>'`, is a message value and is
not affected. A value whose message syntax is escaped, like `list-value:'<\%{value}>'`, is a string value and now
results in the text `<%{value}>`.

`TypedValue.StringValue.asMessage(MessageFactory)` has been replaced by `asMessage()`, which returns the string as a
text message. Callers must drop the factory argument. Code that requires a parsed message must parse the string itself
with `MessageFactory.parseMessage(String)`.

### The `bool` formatter no longer matches string keys

When selected with `format:bool`, the formatter compared the converted boolean value with string map keys as well, so
a key like `'true'` matched. String keys are no longer considered. Only `bool`, `empty` and `null` keys are matched,
and bool keys are always compared with the converted value:

```java
// before: "yes", now: "true"
messageSupport.message("%{v,format:bool,'true':'yes'}")
    .with("v", 1)
    .format();

// "yes"
messageSupport.message("%{v,format:bool,true:'yes'}")
    .with("v", 1)
    .format();
```

### Non-finite numbers are neither true nor false

`NaN` and infinite values were converted to `true`, both by the `bool` formatter and by the number formatter when
comparing them with bool map keys, as used by the `choice` formatter. They now match neither `true` nor `false`. The
`bool` formatter selects the `empty` key for such a value, and a bool key never matches it:

```java
// before: "yes", now: "unknown"
messageSupport
    .message("%{v,format:choice,true:'yes',false:'no',:'unknown'}")
    .with("v", Double.NaN)
    .format();
```

### Supplier parameter values are evaluated at most once

The formatters for `Supplier`, `BooleanSupplier`, `IntSupplier`, `LongSupplier` and `DoubleSupplier` invoked the
supplier for every map key comparison and once more for the formatted text. An expensive supplier was evaluated
several times per formatting call, and a supplier returning a different value on each call (a counter, a clock or a
random value) could select a map entry that did not match the formatted value.

`MessageConfigurer.with(String, Object)` now wraps values of these five types in a caching delegate, which invokes the
supplier at most once. The cached value is kept for as long as the parameter is set: formatting again with the same
configurer, or calling a supplier obtained from `formatSupplier()` again, reuses the value of the first evaluation.
A configurer that is reused and must evaluate the supplier again needs to set the parameter again with `with(...)`.
`MessageConfigurer.getParameters()` returns the caching delegate instead of the original supplier.

The formatter is selected by the type of the delegate. If an application class implements one of the supplier
interfaces, for example `class Counter implements LongSupplier`, a parameter formatter registered for `Counter` is no
longer used for values set with `with(...)`. Such values are formatted by the formatter of the supplier interface.

For an application class implementing `Supplier`, the formatter for that class can be replaced by a parameter
formatter for `SupplierDelegate` with a higher priority than the `Supplier` formatter. The new method
`SupplierDelegate.getDelegateClass()` returns the class of the wrapped supplier, so the formatter can handle the
application supplier and pass all other suppliers on to the next formatter:

```java
public final class ReportSupplierFormatter
    extends AbstractSingleTypeParameterFormatter<SupplierDelegate<?>>
{
  @Override
  protected @NotNull FormattableType getFormattableType()
  {
    // takes precedence over the Supplier formatter (default order)
    return new FormattableType(SupplierDelegate.class,
        FormattableType.DEFAULT_ORDER - 10);
  }

  @Override
  protected @NotNull Text formatValue(
      @NotNull ParameterFormatterContext context,
      @NotNull SupplierDelegate<?> delegate)
  {
    var delegateClass = delegate.getDelegateClass();

    if (ReportSupplier.class.isAssignableFrom(delegateClass))
      return context.format(((Report)delegate.get()).getTitle());

    return context.delegateToNextFormatter();
  }
}
```

The formatter has access to the supplied value only, as the original supplier instance is released after its first
evaluation. For application classes implementing `BooleanSupplier`, `IntSupplier`, `LongSupplier` or
`DoubleSupplier`, the delegate class does not identify the application class, so this approach is not available.

### Temporal values keep their time zone

The temporal formatter converted every value to the system default time zone before formatting, so a `ZonedDateTime`
or `OffsetDateTime` was rendered in the local time of the server instead of its own zone or offset. Values with a zone
or an offset are now formatted in that zone or offset. A `Calendar` is formatted in its own time zone as well, instead
of being converted to an instant in the system default time zone. Values without a zone, such as `LocalDateTime`, and
instants are still formatted in the system default time zone.

Code that relied on the conversion to the system time zone must convert the value itself before passing it, for
example with `zonedDateTime.withZoneSameInstant(ZoneId.systemDefault())`.

### Iterables other than collections are not iterated for map keys

To determine the size of a parameter value or to match it against the `empty` and `!empty` map keys, the iterable
formatter iterated `Iterable` values that are not a `Collection`. An iterable that can be iterated only once, such as a
`DirectoryStream`, was consumed before it was formatted. Such iterables are no longer iterated for these purposes: the
size is unknown, and the `empty` and `!empty` keys are matched against the formatted text instead of the elements.
Collections are not affected. To match an iterable by its elements, pass it as a collection, for example as a `List`.

### `MessagePart` leaf interfaces are sealed

`MessagePart.Text`, `MessagePart.Parameter`, `MessagePart.Template` and `MessagePart.PostFormat` were `non-sealed`, so
any class could implement them. Custom implementations were not supported by the formatting, export and template
lookup code, and the built-in parts considered themselves equal to custom parts with the same properties but not the
other way around.

The four interfaces are now `sealed` and permit the built-in implementations only. Classes implementing them no longer
compile. Text parts are created with `TextPartFactory`, all other parts by parsing or building a message. The built-in
parts are now only equal to parts of the same implementation.

### Formatter context interfaces

`ParameterFormatter.ComparatorContext` is now `sealed` and permits the internal implementation only. It provides the
new method `getCollator(int)`, described under New Features. `ParameterFormatterContext` declares the new abstract
method `isSelectedByName()`. Both interfaces are implemented by the library and passed to formatters. Classes
implementing them, such as test doubles, no longer compile and must be replaced by formatting through a message
support.

### `Message.Parameters` no longer specifies `hashCode()`

`Message.Parameters` redeclared `hashCode()` with a contract based on the locale and the parameter values. None of the
implementations followed it, and the equality of the built-in implementation was not symmetric. The redeclaration and
its contract have been removed. Implementations may use identity equality, and code must not expect two instances with
the same locale and values to be equal. To compare the content of two instances, compare `getLocale()` and
`asParameterMap()`.

### Exception constructor overloads without cause removed

`MessageConfigurer.formattedException(...)` and `formattedExceptionSupplier(...)` had two single-argument overloads,
one for `ExceptionConstructor` and one for `ExceptionConstructorWithCause`. For an exception class with a `(String)`
and a `(String, Throwable)` constructor, which applies to almost every JDK exception, a constructor reference such as
`IllegalStateException::new` matched both overloads and did not compile.

The single-argument overloads taking an `ExceptionConstructorWithCause` have been removed, as they only passed `null`
as cause. Constructor references now compile with and without a cause:

```java
IllegalStateException ex1 = messageSupport
    .code("invalid-state")
    .formattedException(IllegalStateException::new);

IllegalStateException ex2 = messageSupport
    .code("invalid-state")
    .formattedException(IllegalStateException::new, cause);
```

A two-parameter lambda without cause no longer compiles. It must either pass `null` as the cause argument or be
replaced by a lambda with one parameter.

### Message-valued template default parameters removed

`MessageBuilder.TemplateBuilder.withDefaultParameter(String, Message.WithSpaces)` and
`withDefaultParameter(String, Consumer<MessageBuilder>)` have been removed. A default parameter of a template reference
in a format string can only be a string, a boolean or a number, so a template reference with a message default could
not be written as a format string. The overloads for `String`, `boolean` and `long` remain. A message that must be
passed to a template is passed as a message parameter and delegated to the template parameter:

```java
MessageBuilder
    .create()
    .template("order-summary")
        .withDefaultParameter("count", 0)
        .withParameterDelegate("status", "orderStatus")
    .build();
```

### Builder restrictions for templates and post formatters

`MessageBuilder.buildAsTemplate()` now throws an `IllegalStateException` if the message contains template references,
because a template referencing another template cannot be resolved. A post formatter does not accept message-typed
configuration values: `PostFormatterBuilder.configMessage(...)` throws an `IllegalArgumentException`, and the parser
reports a syntax error for a post formatter configuration value that contains message syntax:

```java
// rejected: clip-suffix-text contains a parameter reference
messageSupport.message(
    "%(clip,'%{text}',clip:20,clip-suffix-text:'%{more}')");

// accepted: plain text value
messageSupport.message(
    "%(clip,'%{text}',clip:20,clip-suffix-text:'...')");
```

### `MessageFormatException` records the formatting context in nesting order

`MessageFormatException` stored one code, template, locale and parameter name and built its detail message from these
four values. For nested parts, such as a parameter inside a template referenced from a mapped message, the message
named the wrong parameter or omitted parts of the context.

The exception now records each context entry in the order the failure propagates through the nested parts, including
the post formatter being applied. The detail message is derived from this sequence, so the wording of detail messages
has changed. If several parameters or post formatters have been recorded, `getParameter()` and `getPostFormatter()`
return the innermost one. The protected constructor `MessageFormatException(String, String, Locale, String,
Throwable)` has been removed. Subclasses must use the protected `MessageFormatException(Throwable)` constructor and
add context through the `with...` methods.

### Duplicate named formatters are rejected

Registering a named parameter formatter whose name was already taken silently replaced the previous formatter.
`FormatterService.WithRegistry.addFormatter(ParameterFormatter)` now throws a `FormatterServiceException` in this case,
consistent with `addPostFormatter(PostFormatter)`. Applications that register their own formatter under the name of a
built-in formatter must use a new formatter service without that formatter, for example a `GenericFormatterService`.

### Auto-applied named formatters no longer have a fallback

A named formatter that is selected automatically through one of its configuration keys
(`autoApplyOnNamedConfigParameter()`) was put in front of the formatters for the value type, so it could delegate to
them with `delegateToNextFormatter()`. It is now the only formatter in the chain, exactly as if it had been selected
with `format:<name>`. A delegating call from such a formatter throws a `NoSuchElementException`. Formatters that need
the formatting of another type must call `context.format(value, type)` instead.

### `DefaultFormatterService` uses the library class loader by default

`new DefaultFormatterService()` and the constructors receiving a `null` class loader discovered service-provided
formatters through the system class loader. In environments where the system class loader does not see the
application libraries, such as Spring Boot executable jars, servlet containers or build tool plugins, formatters from
other modules were not found. The class loader of the library is now used instead. To use a different class loader,
pass it to `DefaultFormatterService(ClassLoader, int)`.

### Invalid `geo` format patterns fail

A `geo` configuration value that is neither a predefined format name nor a valid pattern was silently ignored and the
value was formatted in degrees only. Such a pattern now makes formatting fail with a `MessageFormatException` caused by
an `IllegalArgumentException` that names the pattern.

### Message codes and template names are returned as snapshots

`MessageAccessor.getMessageCodes()` and `getTemplateNames()` returned unmodifiable views of the internal maps of the
message support. They now return sorted, unmodifiable copies taken at the time of the call. Code that kept the returned
set to observe later registrations must call the method again.

### Pack failures are reported as `IOException`

Importing a pack without a version, or with content that cannot be decoded, failed with runtime exceptions such as
`IllegalArgumentException` or `NullPointerException`. Both cases now throw a `java.io.StreamCorruptedException`, and a
pack with a version newer than the supported one throws an `IOException`. Code that caught runtime exceptions around
`importMessages(...)` must catch `IOException` instead.

The pack format limits the size of a message: at most 255 parts per message, 255 entries per parameter map or
parameter configuration, 255 default parameters and 255 parameter delegates per template reference, 255 locales per
localized message and 65535 bytes per string. Exporting a message or template exceeding one of these limits failed
with an `IllegalArgumentException`. `exportMessages(...)` now throws an `IOException` that names the message code or
template name. As before, the stream then contains an incomplete pack.

### `ResourceBundleAdopter` requires the requested locales

`adopt(String)` and `adopt(String, ClassLoader)` have been removed. They looked up a bundle for every locale returned
by `Locale.getAvailableLocales()`, which is slow and silently ignores missing bundles. The locales must now be passed
explicitly with `adopt(String, Set<Locale>)` or `adopt(String, Set<Locale>, ClassLoader)`:

```java
new ResourceBundleAdopter(messageSupport)
    .adopt("com.example.messages",
        Set.of(Locale.ENGLISH, Locale.GERMAN));
```

These overloads accepted a bundle that the `ResourceBundle` lookup had found for a fallback locale (a parent locale,
the default locale or the base bundle) and registered its texts for that fallback locale. A requested locale without a
bundle of its own was therefore never reported. A bundle whose locale differs from the requested one is now treated as
missing and causes a `MessageAdopterException`. Texts are registered for the requested locale, including the keys
inherited from parent bundles.

### Annotation adopter exceptions

`AbstractAnnotationAdopter` wrapped every exception raised while scanning and adopting classes in a
`MessageAdopterException`, including the `MessageParserException` for an invalid message or template text. A
`MessageParserException` now propagates unchanged from `adopt(Class<?>)`, `adopt(Path)`, `adopt(byte[])` and
`adopt(ClassLoader, Set<String>)`. All other failures while reading a class are reported as a
`MessageAdopterException` that names the class file, the class or the archive:

```java
try {
  new AnnotationAdopter(messageSupport).adopt(ShopMessages.class);
} catch(MessageParserException ex) {
  // invalid message or template text
} catch(MessageAdopterException ex) {
  // class path scanning or class file failure
}
```

During package scanning, class path resources with a protocol other than `file`, `jar`, `war` or `zip`, such as `vfs:`
or `bundleresource:`, and archives that cannot be opened were skipped silently. They now cause a
`MessageAdopterException`.

The `locale` of a `@Text` annotation was converted with `Locale.forLanguageTag`, which returns the root locale for
notations like `de_DE`. Such a text became the fallback for every locale, and two of them failed with a duplicate
definition error for the empty locale. The locale must now be empty, an ISO 639 language code like `de` or an IETF
BCP 47 language tag like `de-DE`. Any other value causes a `MessageAdopterException` naming the locale:

```java
// before
@Text(locale = "de_DE", text = "Hallo %{name}!")

// now
@Text(locale = "de-DE", text = "Hallo %{name}!")
```

The `value` of a `@Text` annotation is a shorthand for a text without locale. It was ignored if `locale` or `text` was
set as well. Such a combination now causes a `MessageAdopterException`. Annotations combining `value` with a locale
must use `text` instead.

### SpEL expressions are restricted to reading data

The `spel` formatter of the `message-format-spring` module evaluated expressions with the standard type locator and a
reflective method resolver. Expressions could therefore reference types, invoke static methods and call methods of
`Class` objects. Type references such as `T(java.lang.System)` are now rejected, and only instance methods of objects
other than `Class` objects can be invoked. Such expressions make formatting fail with a `MessageFormatException` caused
by a `SpelEvaluationException`. Expressions using these constructs must be rewritten, for example by passing the
required value as an additional message parameter and referencing it as a variable (`#name`).

### `MessageSupportMessageSource` validates the parameter prefix

The prefix passed to the `MessageSupportMessageSource` constructors names the positional message arguments, for
example `p1`, `p2` and so on for the prefix `p`. It only had to start with a letter, so prefixes like `Arg` or `arg_`
produced parameter names that cannot be referenced in a format string. The prefix must now form kebab-case or lower
camel-case parameter names together with the argument number. Otherwise the constructor throws an
`IllegalArgumentException`.

### Log4j formatting failures are reported

`Log4jMessageFactory` replaced the text of a log message by a placeholder whenever formatting threw any `Throwable`,
without reporting the failure. Runtime exceptions now still produce the placeholder text but are additionally reported
as a warning through Log4j's `StatusLogger`, including the message format string and the stack trace. An `Error`
thrown while formatting is no longer suppressed and propagates to the logging call. The module now requires Log4j
2.11.0 or later.

### Apache Tika detectors are no longer registered automatically

The core module contained an internal Apache Tika detector for message format packs and registered it through a
`META-INF/services/org.apache.tika.detect.Detector` file. It only worked with Tika versions before 4.0, and the
`pack` dependency brought `tika-core` into the class path of every application.

The service file has been removed, and `tika-core` is excluded from the dependencies, so Tika is no longer pulled in
transitively. The detectors are now public classes in the exported package `de.sayayi.lib.message.pack`.
`PackTika3Detector` and `PackTikaDetector` support Tika versions `[1.19,4.0)`, and `PackTika4Detector` supports
`[4.0,5.0)`. An application that wants Tika to detect pack files must register the detector matching its Tika version
in its own service file:

```text title="META-INF/services/org.apache.tika.detect.Detector"
de.sayayi.lib.message.pack.PackTika4Detector
```

Only one of the detectors must be registered. The detector interface differs between Tika 3 and Tika 4, so a detector
for the other major version fails with an `AbstractMethodError` when Tika invokes it.

### Gradle plugin regex filters are `ListProperty`

The `include`, `exclude` and `ignore` filters of the `messageFormat` extension were copied to the `messageFormatPack`
task once, when the task was created. If the task was realized before the `messageFormat` block had been evaluated,
for example by `jar { from messageFormatPack }` placed above it, all filters were silently lost.

The filters are now modelled as `ListProperty<String>` and forwarded to the task lazily. The return type of
`MessageFormatMessagesExtension.getIncludeRegexFilters()`, `getExcludeRegexFilters()` and
`MessageFormatTemplatesExtension.getIgnoreRegexFilters()` changed from `List<String>` to `ListProperty<String>`. Build
scripts using the `include`, `exclude` and `ignore` methods are not affected. Build logic accessing the lists directly
must use the `ListProperty` API, such as `get()` and `add(...)`.

### Pygments lexer removed

The Pygments lexer for message format strings in `syntax/pygments` has been removed. It is replaced by the highlight.js
language described under New Features.

### Dependency changes

| Dependency                           | Type    | Old version | New version  |
|--------------------------------------|---------|-------------|--------------|
| `org.apache.logging.log4j:log4j-api` | compile | [2.0,3.0)   | [2.11.0,3.0) |

The dependency applies only to the `message-format-log4j` module. Its scope changed from `runtime` to `compile`, as
described under Bug Fixes.


## New Features

### Factory methods for configurable message support

`MessageSupportFactory` provides two methods for the most common setups. `createDefault()` returns a configurable
message support with all formatters of the shared `DefaultFormatterService`. `createGeneric()` returns one with an
empty `GenericFormatterService`, so all formatters must be registered explicitly. Both use the shared message factory.

```java
ConfigurableMessageSupport messageSupport =
    MessageSupportFactory.createDefault();

messageSupport.addMessage("cart.summary",
    "%{items,format:choice,0:'empty',1:'one item'," +
    ":'%{items} items'}");
```

### Templates from resource bundles

`ResourceBundleAdopter` can adopt templates in addition to messages. The new `adoptTemplates(...)` methods mirror the
`adopt(...)` methods and accept a single `ResourceBundle`, a collection of bundles, or a bundle base name with a set of
locales and an optional class loader. Each key becomes the template name and must follow the kebab-case naming
convention. Texts for the same name from bundles of different locales are combined into one locale-aware template.

```java
new ResourceBundleAdopter(messageSupport)
    .adoptTemplates("com.example.templates",
        Set.of(Locale.ENGLISH, Locale.GERMAN));
```

### Spring formatters for `DataSize`, `ResolvableType` and `TypeDescriptor`

The `message-format-spring` module provides three parameter formatters, which are registered automatically by the
`DefaultFormatterService`.

`ResolvableTypeFormatter` and `TypeDescriptorFormatter` format a `ResolvableType` or a `TypeDescriptor` like the Java
type it represents, including the generic type arguments. The `type` configuration key of the type formatter applies.
`ResolvableType.NONE` is matched by the `empty` key and formatted as empty text.

```java
var listOfDates = TypeDescriptor.collection(List.class,
    TypeDescriptor.valueOf(LocalDate.class));

// "List<LocalDate>"
messageSupport.message("%{t,type:c}")
    .with("t", listOfDates)
    .format();
```

`DataSizeFormatter` formats a `DataSize` by its number of bytes and delegates to the `filesize` formatter, so the
configuration keys of that formatter apply. The `filesize` formatter is not registered automatically and is deprecated
for removal. Without it, the number of bytes is formatted as a `long`.

### Selection by name in `ParameterFormatterContext`

`ParameterFormatterContext.isSelectedByName()` tells a formatter whether it was selected by name, with
`format:<name>`, or by the type of the value. A formatter that serves both purposes can restrict the value conversion
to the selection by name:

```java
if (!context.isSelectedByName())
  return context.delegateToNextFormatter();
```

### Shared collators for map key comparison

`ParameterFormatter.ComparatorContext.getCollator(int)` returns a collator for the locale of the formatting call and
the given strength, such as `Collator.PRIMARY`. The same instance is returned for all map keys compared during one map
lookup, so formatters implementing `MapKeyComparator` no longer need to create a collator per key. The returned
collator must not be modified.

### Post formatter lookup by name

`FormatterService.getPostFormatter(String)` returns the post formatter registered under a name, or `null`. It avoids
copying the registry, which `getPostFormatters().get(name)` requires for a formatter service with a registry.

### Deferred formatter discovery in `DefaultFormatterService` subclasses

The constructors of `DefaultFormatterService` call the overridable `addDefaultFormatters()`, which calls
`addParameterFormattersFromService()` and `addPostFormattersFromService()`. These calls happen before the fields of a
subclass are initialized. The new protected constructor `DefaultFormatterService(ClassLoader, int, boolean)` lets a
subclass skip the call and invoke `addDefaultFormatters()` at the end of its own constructor:

```java
public class FilteringFormatterService
    extends DefaultFormatterService
{
  private final Set<String> excluded;

  public FilteringFormatterService(Set<String> excluded)
  {
    super(null, DEFAULT_FORMATTER_CACHE_SIZE, false);

    this.excluded = excluded;

    addDefaultFormatters();
  }

  @Override
  protected void addPostFormattersFromService()
  {
    ServiceLoader
        .load(PostFormatter.class, classLoader)
        .stream()
        .map(ServiceLoader.Provider::get)
        .filter(pf -> !excluded.contains(pf.getName()))
        .forEach(this::addPostFormatter);
  }
}
```

### Post formatter context in `MessageFormatException`

`MessageFormatException` records the post formatter being applied when formatting fails. It is available through
`getPostFormatter()` and can be added with `withPostFormatter(String)`. `getContextKey()` returns the recorded context
as a key sequence, such as `l-c-p1-t1` for a parameter inside a template of a message with code and locale.

### Adopting class file content

`AbstractAnnotationAdopter.adopt(byte[])` adopts the messages and templates of class file content that has already
been read. Classes adopted this way are recognized together with `adopt(Path)`, `adopt(Class<?>)` and package
scanning, so the same class is never adopted twice. Invalid class file content is reported as
`MessageAdopterException`.

### Syntax errors spanning a token

`AbstractAntlr4Parser.SyntaxErrorMessageBuilder` provides `with(Token)` in addition to `with(SyntaxTree)`. It formats
the error message and creates a syntax error builder that spans the given token:

```java
syntaxErrorMessage("unexpected token '%{t}'")
    .with("t", token.getText())
    .with(token)
    .report();
```

### Public pack API

The new exported package `de.sayayi.lib.message.pack` contains `PackConstants`, which provides the MIME type and the
pack configuration of message format packs, and the Apache Tika detectors described under Breaking Changes.

### Format string serialization helpers

`MessageUtil.isUnquotableString(String)` tells whether a string can be written without quotes in a message format
string. Names that are keywords of the syntax, such as `true` or `empty`, require quotes. `serializeSimpleString(...)`
writes a string with or without quotes accordingly, which is useful for custom message parts and configuration values
that implement `FormatStringSerializer`.

### Optional `java.sql` module

The module descriptor of `message-format` requires `java.sql` statically. Applications on the module path no longer
need the `java.sql` module. If it is present, `java.sql.Date` and `java.sql.Time` values are formatted as a local
date and a local time as before.

### highlight.js language

The new [highlight.js add-on](https://github.com/jgremmen/message-format/tree/0.25.0/syntax/highlight) is a language
definition for message format strings in documentation and web applications. It is generated from the ANTLR grammar of
the library, so it recognizes the same syntax as the parser. The version of the add-on follows the library version.

### Gradle plugin

The plugin declares compatibility with the Gradle configuration cache and isolated projects. The new
`MessageFormatMessagesExtension.getResolvedDuplicateStrategy()` returns the configured duplicate strategy converted
to a `DuplicateStrategy` value.


## Bug Fixes

- On the module path, `DefaultFormatterService` did not find any of the built-in parameter and post formatters,
  because the module descriptor of `message-format` did not declare them as service providers and `META-INF/services`
  files are ignored for named modules. Formatting therefore fell back to the string formatter for all values. The
  module descriptors now provide all built-in formatters.

- Types of the ANTLR runtime, the `pack` library and the core module are part of the public API of the library and
  its add-on modules, but were declared as runtime dependencies and not required transitively by the module
  descriptors. Applications using these types had to declare the dependencies themselves. They are now `compile`
  dependencies and are required transitively, as are the Log4j API and the Spring context dependencies of the
  respective add-on modules.

- Messages, templates and default configuration values of a configurable message support were stored without
  synchronization, so concurrent registration and formatting could corrupt the internal maps or let two threads both
  pass the duplicate check. Registration is now atomic and guarded together with the message and template filters.
  Formatter registration validates all preconditions before changing the formatter service, so a failed registration
  no longer leaves a partially registered formatter, and a rejected duplicate post formatter no longer replaces the
  registered one.

- Several caches kept strong references to classes for the lifetime of the application, which prevented their class
  loaders from being garbage collected after a redeployment: the formatter cache of the formatter service, the type
  cache of the annotation formatter and the reflection caches shared by all `spel` formatter instances. The first two
  no longer reference the cached classes, and the reflection caches now belong to the formatter instance. The number
  formatter no longer keeps decimal format symbols for every locale ever used.

- The formatter cache located types by a binary search over class names but detected hits by identity. Two different
  classes with the same name, loaded by different class loaders, made the lookup loop forever while holding the cache
  lock. Types with the same name are now ordered consistently.

- A locale-aware message or template ignored the script of the requested locale, so for texts for `sr-Latn` and
  `sr-Cyrl` the first one was selected for both locales. Without a text for the requested language and country, the
  first text with the same language in hash map order was selected, so for `de-AT` a text for `de-DE` could be chosen
  over a text for `de`. A text with the same script, and then with the same country, is now preferred over one without
  script or country, which in turn is preferred over one with a different script or country. Texts are kept in
  declaration order.

- A post formatter received the default locale of the message support through `PostFormatterContext.getLocale()`
  instead of the locale of the current formatting call. Locale-sensitive post formatters, such as `case`, ignored a
  locale set with `locale(...)`.

- A default value of a template parameter was looked up under the name of the delegated message parameter instead of
  the template parameter name. For a reference like `%[tpl,count->n,count=0]`, the default was ignored when `n` was not
  set.

- Characters outside the Basic Multilingual Plane, such as emoji, lost their low surrogate in message text and were
  rendered as a lone high surrogate.

- Message text could not contain combining marks, format characters or private use characters, so text in scripts
  like Thai or Arabic and emoji sequences joined by zero width joiners could not be parsed. These characters are now
  accepted, and names and unquoted values accept combining marks and zero width joiners. C1 control characters are
  ignored like C0 control characters.

- `asFormatString()` did not always produce a format string that parses to the same message. Backslashes were not
  escaped, string values that are keywords of the syntax were written without quotes, string defaults of template
  references could be written unquoted, and characters that the parser rejects, such as unassigned code points, were
  written unescaped. A single backslash in message text is now treated as a regular character instead of starting an
  escape sequence.

- A map that contains itself as a key or value was formatted with the internal representation of a text part, such as
  `Text('(this map)')`, instead of the text `(this map)` or the text configured with `map-this`.

- The `type:v` configuration expands the bounds of type variables, but also expanded the bounds of type variables
  within these bounds. For a recursive bound such as `E extends Enum<E>`, formatting failed with a
  `StackOverflowError`. The bounds are now expanded for the outermost type variable only.

- Two template parts with the same name and spacing but different default parameters or parameter delegations were
  considered equal. A message part normalizer could therefore replace a template reference by one with different
  defaults. In addition, the small LRU normalizer could return the part of an earlier cache slot after an identity hit,
  and `FormattableType` instances with the same type but a different order were considered equal.

- The `clip` post formatter counted UTF-16 characters, so it could split a surrogate pair, an emoji sequence or a
  letter with a combining accent. It now counts user-perceived characters (grapheme clusters) for the formatting
  locale and never splits them.

- The list formatters added the `list-value-more` text but continued with the remaining elements instead of stopping.

- `NaN` and infinite values threw an exception when compared to string map keys and could match number map keys. They
  no longer match number keys and do not throw for string keys. The `geo` formatter renders them as empty text.

- A temporal value lacking a field required by the selected format, such as a `LocalDate` with a format containing a
  time, made formatting fail. It is now formatted as empty text, as are invalid date and time patterns.

- The formatter for legacy date types (`Calendar`, `Date`, `FileTime` and `InstantSource`) classified the string
  `"temporal"` instead of adding the `temporal` classifier. The `classifier` formatter therefore reported these values
  as `string` values.

- The number formatter compared `AtomicInteger`, `AtomicLong`, `LongAdder` and `LongAccumulator` values with bool,
  number and string map keys as `double` values, which loses precision for large values. They are now compared like
  the other integer types.

- The formatter for `MatchResult` values called `matches()` on a `Matcher`, which discarded a match found before with
  `find()` or `lookingAt()` and produced empty text. An existing match is now kept.

- A post formatter part whose sub-message formatted to empty text lost its surrounding spaces. It now produces a single
  space if the part has a leading or trailing space.

- `MessageBuilder` and the part builders could still be used after `build()`, `buildWithCode(String)` or
  `buildAsTemplate()`, which modified the message that had already been returned. A sub-builder retained after its part
  had been completed could also change that part, and the configuration map of a built part could be modified through
  the builder. All of these now throw an `IllegalStateException` or have no effect on the built message.

- `formattedExceptionSupplier(ExceptionConstructorWithCause, Throwable)` returned the same exception instance on every
  `get()`, so the exception kept the stack trace of its first creation and accumulated suppressed exceptions. The
  variant without cause created a new exception but formatted the message again on every call. Both now format the
  message once and create a new exception on every call.

- Adding context to a `MessageFormatException` or a `MessageParserException` with one of the `with...` methods created
  a new exception with a new stack trace and without the suppressed exceptions, which hid the location of the failure.
  The original stack trace and the suppressed exceptions are now preserved.

- `MessageConfigurer.getParameters()` returned the internal parameter map, so modifications bypassed the parameter
  name validation. It now returns an unmodifiable view.

- The pack stream was left open when an import failed on an invalid pack header. The import now closes the stream in
  all cases, as documented. An export with more than 65535 messages or templates, which the pack format cannot
  represent, is rejected with an `IOException` before anything is written.

- `toString()` of a locale-aware template threw an exception. It now lists the text for each locale.

- `SupplierDelegate` did not publish the computed value safely to other threads, and a supplier that requested its own
  value recursively invoked itself until the stack overflowed. The value is now visible to all threads, and recursive
  initialization fails with an `IllegalStateException`.

- `SortedStringMap` reported its spliterator as `IMMUTABLE` even for an unsealed map and estimated the remaining size
  incorrectly. Copying an empty sealed map threw a `NullPointerException`, and `remove(Object)` on a sealed map only
  threw `UnsupportedOperationException` if the key was present. An unsealed map now supports all optional `Map`
  operations, including `replaceAll(...)`, `Entry.setValue(...)` and removal through its views and iterators.

- `MessageUtil.trimAndNormalizeSpaces(String)` returned the unchanged string if trimming and normalizing did not change
  its length, even when it contained space characters other than a regular space. The name validation methods of
  `MessageUtil` processed UTF-16 characters instead of code points and could misjudge names with supplementary
  characters.

- `Parameters.getParameterValueAsEnum(...)` did not unwrap an `Optional` containing a constant of the requested enum
  type and returned an empty result. The `Optional` is now unwrapped first, as for the other conversion methods.

- The annotation adopter recorded visited classes under different keys for package scanning and for `adopt(Path)` or
  `adopt(ClassLoader, Set<String>)`, so the same class could be adopted twice and fail with a duplicate definition.
  Class directories whose path contains encoded characters, such as spaces, were not scanned. Scanning a jar file
  closed the jar file cached by the JVM, which broke later class loading from that jar. A parse error in a
  single-text `@TemplateDef` did not name the template.

- The synthetic `MessageDef`, `TemplateDef` and `Text` annotations in `de.sayayi.lib.message.annotation.adopter.util`
  used identity equality and exposed their internal text arrays. They now implement `equals(...)` and `hashCode()` as
  specified by `java.lang.annotation.Annotation`, and `texts()` returns a copy.

- `MessageSupportMessageSource` passed `MessageSourceResolvable` arguments to the message as objects instead of
  resolving them, unlike Spring's `AbstractMessageSource`. They are now resolved lazily, when the message uses the
  parameter. A `NoSuchMessageException` raised while resolving such an argument is propagated unwrapped.

- The log messages created by `Log4jMessageFactory` returned `null` from `getFormat()` and failed with a
  `NotSerializableException` when a log event was serialized. They now return the message format string and are
  serialized with their formatted text.

- The Gradle plugin failed when it was applied before the `java` plugin, and the pack task left a file handle open for
  every scanned class file. Actions registered with `action(...)` were not part of the task inputs, so the task could be
  up to date or restored from the build cache without running them; the task now always runs if actions are registered.
  The duplicate strategy is tracked in its converted form, so equal strategies in different notations (`'fail'`,
  `'FAIL'` or `DuplicateStrategy.FAIL`) no longer differ as task inputs, and unknown strategies as well as invalid
  regular expressions in the filters are reported before the task runs.
