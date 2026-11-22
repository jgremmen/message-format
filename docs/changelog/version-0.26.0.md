---
title: 0.25.0 -> 0.26.0
toc_depth: 2
---

# [Version 0.26.0](https://github.com/jgremmen/message-format/tree/0.26.0) (2026-11-22)


## Breaking Changes

### `NamedTemplate` and `AbstractNamedTemplate` replaced by `JavaTemplate`

The sealed interface `NamedTemplate` and its only permitted implementation, the base class `AbstractNamedTemplate`,
have been removed. Templates implemented in Java now implement the new interface
`de.sayayi.lib.message.template.JavaTemplate` directly. `JavaTemplate` is non-sealed and declares the same
`getName()` method. Its `isSame(Template)` is a default method with the behavior of the former base class: two
templates are the same if the other template is a `JavaTemplate` with the same name.

Custom templates must replace `extends AbstractNamedTemplate` with `implements JavaTemplate`:

```java
// before
public class CurrentDateTemplate extends AbstractNamedTemplate
{
  ...
}

// now
public class CurrentDateTemplate implements JavaTemplate
{
  @Override
  public @NotNull String getName() {
    return "current-date";
  }

  @Override
  public @NotNull Text formatAsText(
      @NotNull MessageAccessor messageAccessor,
      @NotNull Parameters parameters)
  {
    ...
  }
}
```

`AbstractNamedTemplate` also provided a `toString()` implementation returning `Template(<name>)`. An interface cannot
provide this, so a custom template that relies on this representation, e.g. in log output, must override `toString()`
itself.

The overload `MessagePublisher.addTemplate(NamedTemplate)` has been changed to `addTemplate(JavaTemplate)`. It still
registers the template under the name returned by `getName()`.

### Service providers for templates are declared for `JavaTemplate`

`ConfigurableMessageSupport.registerTemplatesFromService(ClassLoader)` and the shared message support returned by
`MessageSupportFactory.shared()` now discover service providers of `de.sayayi.lib.message.template.JavaTemplate`. The
module descriptor of the library declares `uses de.sayayi.lib.message.template.JavaTemplate` instead of
`NamedTemplate`.

A `provides` declaration in `module-info.java` that still names `NamedTemplate` fails to compile, as the interface no
longer exists. A `META-INF/services/de.sayayi.lib.message.template.NamedTemplate` file, however, is silently ignored,
so the templates it lists are no longer registered and messages referencing them fail to format. The file must be
renamed:

```text
META-INF/services/de.sayayi.lib.message.template.JavaTemplate
```

```java
module com.example.reports {
  requires de.sayayi.lib.message;

  provides de.sayayi.lib.message.template.JavaTemplate
      with com.example.CurrentDateTemplate;
}
```

### Permitted subtypes of `Template`

The sealed interface `Template` now permits `Template.WithMessage` and `JavaTemplate`, instead of the internal class
`MessageTemplate` and `NamedTemplate`. Code that distinguishes template kinds with `instanceof` or an exhaustive
`switch` over `Template` must test for these two interfaces:

```java
switch(template) {
  case Template.WithMessage withMessage ->
      System.out.println(withMessage.getMessage());
  case JavaTemplate javaTemplate ->
      System.out.println(javaTemplate.getName());
}
```

### Self-reference texts depend on the locale

If an array, an iterable or a map contains itself as an element, key or value, the respective formatter outputs a
placeholder text instead of recursing. Without the configuration keys `list-this` (arrays and iterables) or `map-this`
(maps), this text was always English: `(this array)`, `(this collection)` and `(this map)`.

The default text is now chosen by the locale used for formatting. For `Locale.GERMAN`, for example, the texts are
`(dieses Array)`, `(diese Sammlung)` and `(diese Map)`. The English texts are still used for English and for all
locales without a translation. Messages that require the English texts regardless of the locale must configure them
explicitly:

```java
messageSupport
    .message("%{list,list-this:'(this collection)'}")
    .with("list", list)
    .locale(Locale.GERMAN)
    .format();
```

The available translations are described under New Features.


## New Features

### Access to the message of a message-backed template

Templates created from a message format string, by `MessageFactory` or the message builder, or imported from a pack
file now implement the public interface `Template.WithMessage`. Its method `getMessage()` returns the message that
provides the template content. This makes it possible to inspect a registered template, e.g. its message parts or
its format string:

```java
Template template = messageSupport
    .getMessageAccessor()
    .getTemplateByName("greeting");

if (template instanceof Template.WithMessage withMessage)
{
  Message message = withMessage.getMessage();
  ...
}
```

`Template.WithMessage` is sealed and cannot be implemented by application code. Its default `isSame(Template)`
implementation considers another template the same if it is a `Template.WithMessage` whose message is the same, as
determined by `MessageFactory.isSame(Message, Message)`.

### Localized self-reference texts

The texts output by the array, iterable and map formatters for self-references are translated into Chinese (simplified
`zh` and traditional `zh-Hant`), Croatian, Danish, Dutch, English, Finnish, French, German, Greek, Hungarian,
Icelandic, Italian, Japanese, Norwegian, Polish, Portuguese, Russian, Spanish and Swedish. The text is resolved for
the locale of the formatting call. A text missing for a specific locale, such as `de-CH`, is taken from its more
general locale, such as `de`, and finally from the English default. The default locale of the JVM is never used as a
fallback, so the output does not depend on the environment the application runs in.

The configuration keys `list-this` and `map-this` take precedence over the localized texts, as before.

### Formatter resources in `AbstractParameterFormatter`

`AbstractParameterFormatter` provides the new protected method `getResource(String key, Locale locale)`. It returns a
localized text from the resources shipped with the library, or `null` if no resource with this key exists. The lookup
follows the same locale fallback as described above. The resources of a locale are read once and cached, so the
method can be called in every formatting call. Currently the resources contain the self-reference texts with the keys
`this.array`, `this.collection` and `this.map`:

```java
@Override
protected @NotNull Text formatValue(
    @NotNull ParameterFormatterContext context,
    @NotNull Object value)
{
  var text = getResource("this.collection", context.getLocale());
  ...
}
```


## Performance Improvements

### Lock-free formatting in the message support

A configurable message support guarded its messages, templates, default configuration values, locale and filters
with a single read/write lock, which every formatting call acquired several times. These lookups no longer acquire a
lock. Registering messages and templates is still serialized, but formatting threads never wait for a registration
and do not contend with each other.

`MessageSupport.message(String)` parses the message format string lazily on first use without acquiring a lock.

### Lock-free formatter lookups

`GenericFormatterService`, and thereby `DefaultFormatterService`, kept its formatters behind a read/write lock that was
acquired for every formatter lookup, i.e. for every parameter of every formatted message. The registered formatters
are now held in an immutable snapshot, which is replaced on each registration. `getFormatters`, `getPostFormatter`,
`getPostFormatters` and `getParameterConfigNames` read the current snapshot without a lock. `getPostFormatters` and
`getParameterConfigNames` return the immutable snapshot collections instead of creating a copy on each call.

The cache of formatters resolved for a value type is read without a lock as well. A cache hit writes no shared state
in the common case, so frequently formatted types no longer cause contention between threads.

### Concurrent caches for messages, number formats, date-time patterns and SpEL expressions

The message cache of a `MessageFactory` created with `MessageFactory(MessagePartNormalizer, int)`, the number format
cache of the number formatter, the pattern cache of the temporal formatter and the expression cache of the `spel`
formatter were each guarded by a lock taken on every access. They are now concurrent caches: a cache hit acquires no
lock, and values are created outside of any lock, so a cache miss does not block other threads.

The caches of the library evict entries using an approximate least recently used policy. As eviction does not block
other threads, a cache may briefly hold a few more entries than its configured size. The `spel` formatter uses
Spring's `ConcurrentLruCache`.

### Plain text messages are compiled without the parser

Message format strings consisting of plain text only, i.e. letters, digits, punctuation, symbols and spaces up to
`U+00FF` without `%` and `\`, are compiled directly into a text message, without running the ANTLR lexer and parser.
The resulting message is identical to the one created by the parser. All other format strings are parsed as before.

### Less overhead for Log4j messages

`Log4jMessageFactory` created two supplier wrappers with a lock each for every log event. A message now formats its
text on first access and caches it in a field without locking. The parameter names `p1` to `p16` are no longer built
for each log event.

### Unknown codes in `MessageSupportMessageSource`

`MessageSupportMessageSource.getMessage(String, Object[], Locale)` detected an unknown message code by catching the
exception thrown by the message support. It now looks up the code directly. Message sources which delegate many codes
to a parent message source no longer create and discard an exception for each of these lookups.


## Bug Fixes

- `MessageSupportFactory.shared()` could deadlock if a parameter formatter, post formatter or template discovered by
  the `ServiceLoader` mechanism accessed the shared message support while another thread was creating it: one thread
  held the lock of the shared message support and waited for the shared formatter service, while the other held the
  formatter service and waited for the message support. The shared instances are now created in an order which cannot
  deadlock. An access from the same thread still fails with an `IllegalStateException`.

- `GenericFormatterService` invoked methods of the formatters being registered, such as `getFormattableTypes()`,
  `getParameterConfigNames()` and `equals(Object)`, while holding its lock, and formatter lookups invoked `canFormat`
  while holding the read lock. A formatter whose methods waited for another thread using the formatter service, or a
  `canFormat` implementation registering a formatter, made the application hang. Lookups now acquire no lock, and the
  methods of registered formatters are invoked outside the registration lock.

- A message filter or template filter of a configurable message support was invoked while holding the lock of the
  message support, which also guarded all lookups of messages, templates and default configuration values. A filter
  waiting for a message to be formatted on another thread therefore hung forever. Lookups no longer acquire this lock.

- `new Log4jMessageFactory(boolean)` created its default message support in the constructor and thereby accessed the
  shared `DefaultFormatterService`. If a logger using this factory was created in the static initializer of a formatter
  discovered by the shared formatter service, the initialization of the service was re-entered or deadlocked. The
  default message support is now created when the factory creates its first message.
