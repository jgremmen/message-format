---
title: 0.24.0 -> 0.25.0
toc_depth: 2
---

# [Version 0.25.0](https://github.com/jgremmen/message-format/tree/develop) (2026-09-23)


## Breaking Changes

### Temporal values are no longer misclassified

The formatter for temporal values (`ToTemporalDelegate`) registered the `temporal` classifier incorrectly. Instead of
calling `ClassifierContext.addClassifier(String)`, it called `ClassifierContext.updateClassifiers(Object)` and passed
the classifier name `"temporal"` as the value to classify. As a result, formatter selection logic that inspects
classifiers never saw a value actually tagged as `temporal`; the string `"temporal"` was classified as a `string`
value instead.

Custom formatters or match expressions that relied on the (absent) `temporal` classifier to select temporal-specific
behavior must be re-checked: values of type `java.time.temporal.Temporal` are now correctly reported with the
`temporal` classifier, which may change which formatter or match branch is selected for such values.

### `SortedStringMap.remove(Object)` always throws once sealed

Calling `remove(Object)` on a sealed (immutable) `SortedStringMap` previously only threw
`UnsupportedOperationException` when the key to remove was actually present in the map. Removing a key that did not
exist silently returned `null` instead, even though the map is immutable. `remove(Object)` on a sealed map now always
throws `UnsupportedOperationException`, regardless of whether the key exists:

```java
SortedStringMap<String> sealed = SortedStringMap
    .<String>builder()
    .put("a", "1")
    .seal();

// Before: returned null, no exception
// After: throws UnsupportedOperationException
sealed.remove("does-not-exist");
```

Code that relied on `remove` silently doing nothing for absent keys on a sealed map must now catch or avoid calling
`remove` on sealed instances entirely.

### Parser exceptions from annotation scanning are no longer wrapped

`AbstractAnnotationAdopter.adopt(...)` used to catch `Exception` around class scanning and message parsing, wrapping
every failure &ndash; including `MessageParserException` raised while parsing an invalid message or template text
&ndash; in a `MessageAdopterException`. Only I/O related failures are now wrapped this way; the catch clauses were
narrowed from `Exception` to `IOException`.

As a consequence, a `MessageParserException` raised for an invalid message or template annotation now propagates
unchanged from `adopt(Class<?>)`, `adopt(Path)` and `adopt(ClassLoader, Set<String>)`, instead of being wrapped in a
`MessageAdopterException`. Code that catches `MessageAdopterException` around these calls to handle malformed message
syntax must additionally catch `MessageParserException`:

```java
try {
  new AnnotationAdopter(messageSupport).adopt(SomeAnnotatedClass.class);
} catch(MessageParserException ex) {
  // invalid message/template format string
} catch(MessageAdopterException ex) {
  // class path scanning or class file I/O failure
}
```

### Apache Tika pack detectors are no longer auto-registered

Automatic detection of message format pack files by Apache Tika previously worked out of the box: the module declared
`requires static org.apache.tika.core` and shipped a `META-INF/services/org.apache.tika.detect.Detector` file that
registered an internal, non-exported `PackTikaDetector` class for Tika 3.

This automatic registration has been removed. Detector support is now split into two public, exported classes in the
new `de.sayayi.lib.message.pack` package:

- `PackTika3Detector` for Apache Tika in the range `[1.19,4.0)`
- `PackTika4Detector` for Apache Tika in the range `[4.0,5.0)`

`PackTikaDetector` still exists as a deprecated alias for `PackTika3Detector`, kept for source compatibility. None of
these classes are registered automatically anymore; applications that want Tika to detect message format pack files
must add the fully qualified class name of the appropriate detector to their own
`META-INF/services/org.apache.tika.detect.Detector` file:

```
de.sayayi.lib.message.pack.PackTika3Detector
```

or, for Apache Tika 4:

```
de.sayayi.lib.message.pack.PackTika4Detector
```

With the service file in place, detection through `Tika#detect(InputStream)` works exactly as before. This change
also allows applications running on Apache Tika 4 to use the message pack detector, which was not possible in
0.24.0.

*Dependency versions for compile and runtime scopes are unchanged in this release.*


## New Features

### `MessageSupportFactory.createDefault()` and `createGeneric()`

Two convenience factory methods have been added to simplify the most common ways of obtaining a configurable
`MessageSupport` instance:

```java
// Fully populated, ready to add messages and templates
ConfigurableMessageSupport withDefaults =
    MessageSupportFactory.createDefault();

// Empty formatter service, for full control over
// which formatters are registered
ConfigurableMessageSupport minimal =
    MessageSupportFactory.createGeneric();
```

`createDefault()` is equivalent to `create(DefaultFormatterService.getSharedInstance())`, while `createGeneric()` is
equivalent to `create(new GenericFormatterService())`. Both remain fully configurable until `seal()` is called.

### Stricter, atomic formatter registration

`GenericFormatterService.addFormatter(...)` and `addNamedFormatter(...)` now validate all preconditions (kebab-case
naming, `Object` formatters implementing `DefaultFormatter`, and auto-apply configuration key conflicts) before
mutating any internal state. If registration fails, the formatter service is left completely unchanged instead of
possibly containing a partially registered formatter.

In addition, registering a named formatter whose name has already been registered is now rejected with a
`FormatterServiceException`, consistent with the existing behavior of `addPostFormatter(PostFormatter)`:

```java
// name: "custom"
service.addNamedFormatter(new MyNamedFormatter());
// throws FormatterServiceException: duplicate name
service.addNamedFormatter(new MyNamedFormatter());
```

### `SyntaxErrorMessageBuilder.with(Token)`

`AbstractAntlr4Parser.SyntaxErrorMessageBuilder` gained a `with(Token)` method, mirroring the existing
`with(SyntaxTree)`. It formats the error message and creates a `SyntaxErrorBuilder` spanning the entire given token,
without having to construct a start/stop token pair manually:

```java
throw createSyntaxError()
    .with("expected a number")
    .with(token)
    .build();
```

### Apache Tika pack detectors as public API

The `de.sayayi.lib.message.pack` package is now exported from the module and contains `PackConstants` (the shared
MIME type and pack configuration), together with `PackTika3Detector` and `PackTika4Detector` described under Breaking
Changes above. Applications integrating with Apache Tika can now reference these types directly instead of relying on
internal, non-exported classes.


## Bug Fixes

`InternalMessageBuilder` allowed methods such as `text(...)`, `parameter(...)` and `build()` to be called again after
`build()`, `buildWithCode(String)` or `buildAsTemplate()` had already produced a message. Since the built
`CompoundMessage` holds a direct reference to the builder's internal parts list, continuing to use the builder after
building silently mutated the already returned message. The builder now tracks whether it has been built and throws
`IllegalStateException` from every mutating method once that happens.

`LocalizedMessageBundleWithCode` stored its locale-to-message entries in a `HashMap`, so the iteration order used to
pick a fallback ("default language") entry when no exact locale match exists was undefined and could vary between
JVM runs. The map is now a `LinkedHashMap`, so the first entry with an empty language acts as a predictable,
insertion-order-based fallback. The constructor also now rejects `null` message values explicitly and normalizes a
`null` locale key to `Locale.ROOT`, instead of relying on ad-hoc `null` checks scattered through the lookup code.

`MatchResultFormatter` treated any `Matcher` that did not satisfy `matches()` (a whole-input match) as "no match" and
returned empty text, even if the matcher already had an established match from a prior `find()` or `lookingAt()`
call. Partial matches obtained through `find()` were therefore discarded and formatted as empty text. The formatter
now checks whether a match has already been established (via `Matcher.start()`) before falling back to a whole-input
`matches()` attempt, so results from `find()` are preserved.

`PostFormatterContextImpl.getLocale()` returned the message accessor's configured locale instead of the locale
supplied for the current formatting call. A post formatter that depends on locale (such as case conversion) ignored a
locale explicitly set via `.locale(...)` on the message and always used the message support's default locale.

`PostFormatterPart` discarded the surrounding space markers whenever the message being post-formatted evaluated to
empty text, causing spacing configured on the message part to be lost entirely. Empty results with a configured
leading or trailing space now produce a single space instead of no text at all.

`FormattableType.equals(Object)` and `hashCode()` only considered the formatted type and ignored the registration
`order`. Two `FormattableType` instances for the same type but a different priority order were therefore treated as
equal, which could cause one of them to be silently dropped when used in a set or as a map key.

`TemplatePart.equals(Object)` and `hashCode()` did not take the default parameter map and parameter delegate map into
account. Two template parts with the same name and spacing but different default parameter values or parameter
delegations were incorrectly considered equal, which could affect message part deduplication and normalization.

`SortedStringMap`'s spliterator always reported the `IMMUTABLE` characteristic, even for a non-sealed, mutable map,
which violates the `Spliterator` contract and can lead to incorrect optimizations by callers such as parallel
streams. `estimateSize()` also always returned the total map size rather than the number of elements remaining after
some have already been consumed. Both characteristics are now reported correctly. Additionally, copying a sealed,
empty `SortedStringMap` (whose backing array is `null`) into a new mutable map no longer throws a
`NullPointerException`.

`SupplierDelegate.value` was not declared `volatile`, so a thread reading a cached value after another thread had
already computed and stored it via the delegate supplier was not guaranteed to see the up-to-date value under the
Java Memory Model. The field is now `volatile`, making the cached value visible across threads as intended.

`LRUMessagePartNormalizer` lacked a locking mechanism around its internal cache, allowing concurrent access from
multiple threads to leave the cache in an inconsistent state. A locking mechanism has been added to guard cache reads
and writes.

`MessageUtil`'s kebab-case name validation methods iterated over names using UTF-16 char indices instead of Unicode
code points, so names containing supplementary characters (outside the Basic Multilingual Plane) could be validated
incorrectly, since a surrogate pair could be misread as two separate characters. The iteration now advances by
`Character.charCount(codePoint)` instead of a fixed step of one.

`PropertiesAdopter.adopt(Properties)`, `adoptTemplates(Properties)` and `adopt(Map<Locale,Properties>)` did not check
their `properties` argument for `null`, resulting in a `NullPointerException` with no indication of which parameter
was `null`. All three methods now explicitly reject a `null` properties argument with a descriptive message.

`AbstractAnnotationAdopter.scan_checkVisited(String)` recorded raw classpath entry names (including `.class`
suffixes and OS-specific path separators) as visited, while `adopt(ClassLoader, Set<String>)` recorded normalized
binary class names. As a result, the same class scanned through both entry points could be visited and adopted
twice, which could lead to duplicate message or template registration errors. Classpath names are now normalized to
binary class names consistently across both entry points.
