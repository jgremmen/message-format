---
toc_depth: 2
---

# ICU Person Name

/// note
This formatter is **not** included in the core library. It is part of the `message-format-icu` module and must be
added as a dependency to use.

```
de.sayayi.lib:message-format-icu:<version>
```
///

The named formatter `icu-person` formats person names from individual name part parameters. It is specifically designed
for assembling person names with fine-grained control over which parts appear, how each part is rendered and in what
order. It is selected explicitly by writing `format:icu-person` in the parameter configuration.

The formatter reads five name parts from the message parameters: `given-name`, `family-name`, `middle-name`, `prefix`
and `suffix`. These are regular message parameters, not configuration keys. They must be provided through `.with()`
calls on the message, just like any other parameter. The anchor parameter referenced in `%{...}` serves only as a
syntactic placeholder; its value is not used.


## Name Parts

The formatter recognizes five parameters by name. Each parameter is optional, but at least one of `given-name` or
`family-name` must be present for the formatter to produce output.

The `given-name` parameter holds the first name ("Alice", "James"). The `family-name` parameter holds the surname or
last name ("Smith", "Carter"). The `middle-name` parameter holds one or more middle names ("Michael", "Earl"). The
`prefix` parameter holds a title or honorific that precedes the name ("Dr.", "Mr.", "Prof."). The `suffix` parameter
holds a generational or professional suffix ("Jr.", "Sr.", "Ph.D.").


## Configuration Keys

The formatter recognizes nine configuration keys. All are optional and have sensible defaults that produce a natural
full name in given-first order.

### `given-format`

Controls how the given name is rendered. Accepts `full` (default), `initial` or `none`. When set to `full`, the given
name appears as provided. When set to `initial`, only the first character followed by a period is shown (e.g. "John"
becomes "J."). When set to `none`, the given name is omitted entirely.

### `family-format`

Controls how the family name is rendered. Accepts `full` (default), `initial` or `none`, with the same semantics as
`given-format`.

### `middle-format`

Controls how the middle name is rendered. Accepts `full`, `initial` (default) or `none`. The default is `initial`
rather than `full`, which reflects the common convention of abbreviating middle names.

### `prefix-format`

Controls whether the prefix appears in the output. Accepts `full` (default) or `none`. Unlike the name part keys,
`initial` is not supported for prefixes because abbreviating a title like "Dr." or "Prof." further would not produce a
meaningful result.

### `suffix-format`

Controls whether the suffix appears in the output. Accepts `full` (default) or `none`. As with `prefix-format`,
`initial` is not supported.

### `order`

Determines the arrangement of name parts in the output. Accepts `given-first` (default), `surname-first` or `sorting`.

In `given-first` order, the parts are assembled as prefix, given name, middle name, family name and suffix. This is
the conventional Western name order (e.g. "Dr. John M. Smith Jr.").

In `surname-first` order, the family name appears first, followed by the prefix, given name, middle name and suffix.
This produces output like "Smith Dr. John M. Jr." and is common in East Asian and some administrative contexts.

In `sorting` order, the family name appears first followed by a comma, then the remaining parts in the same sequence
as `surname-first`. This produces output like "Smith, Dr. John M. Jr." and is suitable for alphabetical indices and
bibliographic references.

### `usage`

Determines the formatting strategy. Accepts `referring` (default), `addressing` or `monogram`.

The `referring` and `addressing` values both use the manual assembly path, where each name part is formatted according
to the individual `*-format` keys described above. In the current implementation these two modes produce identical
output; the distinction exists as a semantic marker for the intended context (third-person reference vs. direct
address).

The `monogram` value switches to ICU4J's `PersonNameFormatter`, which produces locale-specific initials or abbreviated
name forms. In monogram mode, the individual `*-format` keys are not used. Instead, the `formality` and `length` keys
control the output.

### `formality`

Controls the formality level of the formatted name. Accepts `formal` (default) or `informal`. This key is only used
when `usage` is set to `monogram`. In all other modes it has no effect.

### `length`

Controls the overall length of the formatted name. Accepts `long`, `medium` (default) or `short`. This key is only
used when `usage` is set to `monogram`. In all other modes it has no effect.


## Configuration Matrix

The formatter operates in two distinct modes depending on the `usage` setting. Each mode recognizes a different subset
of configuration keys, and keys from the other mode are silently ignored. Understanding which keys are active in each
mode is essential to avoid confusion.

When `usage` is `referring` or `addressing` (the default), the formatter assembles the name manually by applying the
five part format keys (`given-format`, `family-format`, `middle-format`, `prefix-format`, `suffix-format`) and the
`order` key. This mode provides precise control over every aspect of the output. The `formality` and `length` keys
have no effect in this mode.

When `usage` is `monogram`, the formatter delegates to ICU4J's `PersonNameFormatter` to produce locale-specific
initials. In this mode, the `order`, `formality` and `length` keys control the output. The five part format keys have
no effect because the ICU engine determines how name parts are abbreviated.

The following table summarizes which keys are active in each mode.

| Configuration Key | Manual Mode (`referring` / `addressing`) | Monogram Mode (`monogram`) |
|-------------------|:----------------------------------------:|:--------------------------:|
| `given-format`    |                    ✓                    |                            |
| `family-format`   |                    ✓                    |                            |
| `middle-format`   |                    ✓                    |                            |
| `prefix-format`   |                    ✓                    |                            |
| `suffix-format`   |                    ✓                    |                            |
| `order`           |                    ✓                    |             ✓             |
| `formality`       |                                          |             ✓             |
| `length`          |                                          |             ✓             |


## Basic Usage

With default settings, the formatter produces a full name in given-first order.

```java
messageSupport
    .message("%{unused,format:icu-person}")
    .with("given-name", "John")
    .with("family-name", "Smith")
    .format();
// "John Smith"
```

Adding a middle name renders it as an initial by default (the `middle-format` key defaults to `initial`).

```java
messageSupport
    .message("%{unused,format:icu-person}")
    .with("given-name", "John")
    .with("middle-name", "Michael")
    .with("family-name", "Smith")
    .format();
// "John M. Smith"
```

All five name parts together produce a complete formal name.

```java
messageSupport
    .message("%{unused,format:icu-person,middle-format:full}")
    .with("prefix", "Mr.")
    .with("given-name", "James")
    .with("middle-name", "Earl")
    .with("family-name", "Carter")
    .with("suffix", "Jr.")
    .format();
// "Mr. James Earl Carter Jr."
```


## Name Ordering

The `order` key controls the arrangement of parts. In `given-first` order (the default), the name reads naturally as
prefix, given, middle, family, suffix.

```java
messageSupport
    .message("%{unused,format:icu-person}")
    .with("given-name", "John")
    .with("family-name", "Smith")
    .format();
// "John Smith"
```

In `surname-first` order, the family name moves to the front and the remaining parts follow directly after it.

```java
messageSupport
    .message("%{unused,format:icu-person,order:surname-first}")
    .with("given-name", "John")
    .with("family-name", "Smith")
    .format();
// "Smith John"
```

In `sorting` order, the family name is followed by a comma before the remaining parts. This format is commonly used in
alphabetical indices and bibliographies.

```java
messageSupport
    .message("%{unused,format:icu-person,order:sorting}")
    .with("given-name", "John")
    .with("family-name", "Smith")
    .format();
// "Smith, John"
```

Sorting order combined with initials produces a compact citation format.

```java
messageSupport
    .message("%{unused,format:icu-person,order:sorting,given-format:initial}")
    .with("given-name", "John")
    .with("family-name", "Smith")
    .format();
// "Smith, J."
```

All ordering modes respect all five name parts. The following example shows `surname-first` with prefix, middle initial
and suffix.

```java
messageSupport
    .message("%{unused,format:icu-person,order:surname-first,middle-format:initial}")
    .with("prefix", "Dr.")
    .with("given-name", "Jane")
    .with("middle-name", "Marie")
    .with("family-name", "Doe")
    .with("suffix", "Ph.D.")
    .format();
// "Doe Dr. Jane M. Ph.D."
```


## Part Format Control

Each name part can be independently configured to appear in full, as an initial or not at all.

Reducing the given name to an initial while keeping the family name in full is useful for compact displays.

```java
messageSupport
    .message("%{unused,format:icu-person,given-format:initial}")
    .with("given-name", "John")
    .with("family-name", "Smith")
    .format();
// "J. Smith"
```

The reverse configuration, showing the full given name with only the family initial, also works.

```java
messageSupport
    .message("%{unused,format:icu-person,family-format:initial}")
    .with("given-name", "John")
    .with("family-name", "Smith")
    .format();
// "John S."
```

Setting all three name parts to `initial` produces a dotted abbreviation.

```java
messageSupport
    .message("%{unused,format:icu-person,given-format:initial,middle-format:initial,family-format:initial}")
    .with("given-name", "John")
    .with("middle-name", "Michael")
    .with("family-name", "Smith")
    .format();
// "J. M. S."
```

To display only the given name, set `family-format` to `none`.

```java
messageSupport
    .message("%{unused,format:icu-person,family-format:none}")
    .with("given-name", "Alice")
    .with("family-name", "Johnson")
    .format();
// "Alice"
```

To display only the family name, set `given-format` to `none`.

```java
messageSupport
    .message("%{unused,format:icu-person,given-format:none}")
    .with("given-name", "Alice")
    .with("family-name", "Johnson")
    .format();
// "Johnson"
```


## Prefix and Suffix

The `prefix-format` and `suffix-format` keys accept `full` or `none`. The default is `full`, which means prefixes and
suffixes are included whenever the corresponding parameter is present. Setting a format key to `none` suppresses the
part even if the parameter is provided.

```java
messageSupport
    .message("%{unused,format:icu-person}")
    .with("prefix", "Dr.")
    .with("given-name", "John")
    .with("family-name", "Smith")
    .format();
// "Dr. John Smith"

messageSupport
    .message("%{unused,format:icu-person,prefix-format:none}")
    .with("prefix", "Dr.")
    .with("given-name", "John")
    .with("family-name", "Smith")
    .format();
// "John Smith"
```

Suffixes work the same way.

```java
messageSupport
    .message("%{unused,format:icu-person}")
    .with("given-name", "John")
    .with("family-name", "Smith")
    .with("suffix", "Jr.")
    .format();
// "John Smith Jr."

messageSupport
    .message("%{unused,format:icu-person,suffix-format:none}")
    .with("given-name", "John")
    .with("family-name", "Smith")
    .with("suffix", "Jr.")
    .format();
// "John Smith"
```


## Monogram Formatting

When `usage` is set to `monogram`, the formatter delegates to ICU4J's `PersonNameFormatter` to produce locale-specific
initials. In this mode the individual `*-format` keys are ignored and the output is controlled by `formality`, `length`
and `order`.

```java
messageSupport
    .message("%{unused,format:icu-person,usage:monogram}")
    .with("given-name", "John")
    .with("family-name", "Smith")
    .format();
// locale-specific monogram, e.g. "JS" for English
```

The `formality` and `length` keys allow adjusting the monogram style.

```java
messageSupport
    .message("%{unused,format:icu-person,usage:monogram,formality:informal}")
    .with("given-name", "John")
    .with("family-name", "Smith")
    .format();
// locale-specific informal monogram

messageSupport
    .message("%{unused,format:icu-person,usage:monogram,length:short}")
    .with("given-name", "John")
    .with("family-name", "Smith")
    .format();
// locale-specific short monogram
```


## Pre-configured Templates

The `icu-person` formatter requires `format:icu-person` and often several additional configuration keys. When the same
name format is needed in multiple messages, repeating the full configuration in every message format string becomes
tedious and error-prone. [Templates](../../../configuration/templates.md) solve this by encapsulating the formatter
configuration in a reusable fragment that can be referenced with `%[template-name]`.

### Registering Templates

A template is a parsed message fragment registered under a name. The fragment contains the full `icu-person`
parameter configuration, and the name part parameters are resolved from the surrounding message context at format time.

```java
var factory = messageSupport.getMessageAccessor().getMessageFactory();

messageSupport.addTemplate("name-full", factory.parseTemplate(
    "%{unused,format:icu-person}"));

messageSupport.addTemplate("name-citation", factory.parseTemplate(
    "%{unused,format:icu-person,order:sorting,given-format:initial,middle-format:initial}"));

messageSupport.addTemplate("name-informal", factory.parseTemplate(
    "%{unused,format:icu-person,family-format:none,prefix-format:none,suffix-format:none}"));

messageSupport.addTemplate("name-formal", factory.parseTemplate(
    "%{unused,format:icu-person,middle-format:full,prefix-format:full,suffix-format:full}"));

messageSupport.addTemplate("name-family-only", factory.parseTemplate(
    "%{unused,format:icu-person,given-format:none,middle-format:none,prefix-format:none,suffix-format:none}"));
```

### Using Templates in Messages

Once registered, a template is referenced with `%[template-name]`. The name part parameters set on the message flow
into the template automatically.

```java
messageSupport
    .message("Welcome, %[name-informal]!")
    .with("given-name", "Alice")
    .with("family-name", "Johnson")
    .format();
// "Welcome, Alice!"
```

```java
messageSupport
    .message("Reference: %[name-citation]")
    .with("given-name", "John")
    .with("middle-name", "Michael")
    .with("family-name", "Smith")
    .format();
// "Reference: Smith, J. M."
```

```java
messageSupport
    .message("Dear %[name-formal],")
    .with("prefix", "Mr.")
    .with("given-name", "James")
    .with("middle-name", "Earl")
    .with("family-name", "Carter")
    .with("suffix", "Jr.")
    .format();
// "Dear Mr. James Earl Carter Jr.,"
```

```java
messageSupport
    .message("Sorted by: %[name-family-only]")
    .with("given-name", "Alice")
    .with("family-name", "Johnson")
    .format();
// "Sorted by: Johnson"
```

Different templates can be combined in a single message to show the same person's name in different formats.

```java
messageSupport
    .message("%[name-formal] (see also: %[name-citation])")
    .with("prefix", "Dr.")
    .with("given-name", "Jane")
    .with("middle-name", "Marie")
    .with("family-name", "Doe")
    .with("suffix", "Ph.D.")
    .format();
// "Dr. Jane Marie Doe Ph.D. (see also: Doe, J. M.)"
```

### Parameter Delegation

When the message parameters use different names than the ones the `icu-person` formatter expects, the template
reference can map between them using the `->` delegation syntax. The left side is the parameter name expected by the
template and the right side is the parameter name available in the parent message.

```java
messageSupport
    .message("Author: %[name-citation, given-name->firstName, family-name->lastName]")
    .with("firstName", "Alice")
    .with("lastName", "Smith")
    .format();
// "Author: Smith, A."
```

```java
messageSupport
    .message("Patient: %[name-formal, given-name->first, family-name->last, prefix->title]")
    .with("title", "Mrs.")
    .with("first", "Emily")
    .with("last", "Watson")
    .format();
// "Patient: Mrs. Emily Watson"
```

### Parameter Defaults

Default values can be provided directly in the template reference using the `=` syntax. These defaults apply when the
corresponding parameter is not set on the message.

```java
messageSupport
    .message("By: %[name-citation, given-name='Anonymous', family-name='Author']")
    .format();
// "By: Author, A."
```

```java
messageSupport
    .message("By: %[name-citation, given-name='Anonymous', family-name='Author']")
    .with("given-name", "John")
    .with("family-name", "Smith")
    .format();
// "By: Smith, J."
```


## Null Handling

When both `given-name` and `family-name` are absent or `null`, the formatter delegates to its null handling logic. A
`null` map key can be provided to produce specific output for that case.

```java
messageSupport
    .message("%{unused,format:icu-person,null:'unknown'}")
    .format();
// "unknown"
```

If no `null` map key is defined and both required name parts are missing, the formatter produces empty text.


## Registration

When using `DefaultFormatterService`, the `ICUPersonFormatter` is registered automatically via the Java `ServiceLoader`
mechanism. Having the `message-format-icu` module on the classpath is sufficient.

When using `GenericFormatterService`, the formatter must be registered manually:

```java
var formatterService = new GenericFormatterService();
formatterService.addFormatter(new ICUPersonFormatter());

var messageSupport = MessageSupportFactory
    .create(formatterService)
    .setLocale(Locale.ENGLISH);
```

### Dependency

Add the `message-format-icu` module to the project alongside the core message format dependency.

=== "Gradle (Groovy DSL)"

    ```groovy
    dependencies {
      implementation 'de.sayayi.lib:message-format-icu:0.24.0'
    }
    ```

=== "Gradle (Kotlin DSL)"

    ```kotlin
    dependencies {
      implementation("de.sayayi.lib:message-format-icu:0.24.0")
    }
    ```

=== "Maven"

    ```xml
    <dependency>
      <groupId>de.sayayi.lib</groupId>
      <artifactId>message-format-icu</artifactId>
      <version>0.24.0</version>
    </dependency>
    ```

For JPMS-based projects, the module name is `de.sayayi.lib.message.icu`. It requires `de.sayayi.lib.message` and
`com.ibm.icu`.
