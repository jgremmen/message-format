---
toc_depth: 2
---

# ICU

/// note
This formatter is **not** included in the core library. It is part of the `message-format-icu` module and must be
added as a dependency to use.

```
de.sayayi.lib:message-format-icu:0.24.0
```
///

The named formatter `icu` delegates formatting to the
[ICU4J](https://unicode-org.github.io/icu/userguide/format_parse/messages/) `MessageFormat` engine. This makes ICU's
locale-sensitive formatting capabilities available directly within message format strings, including plural rules,
gender-based selection, ordinal formatting and number and date styles.

The formatter operates on the full parameter map of the current message. All parameters passed to the message are
available inside the ICU pattern by their original names. The parameter referenced in the `%{...}` anchor does not need
to match any of the names used in the ICU pattern; its only purpose is to satisfy the message format syntax. A
meaningful parameter name can be used instead if that reads better in context, but its value is not used by the ICU
formatter itself.


## Configuration Keys

The `icu` formatter recognizes a single configuration key.

### `icu`

A quoted string containing an ICU `MessageFormat` pattern. The formatter passes all message parameters as a named map
to the ICU engine, which evaluates the pattern against the locale set on the message.

When `icu` is omitted, the formatter produces empty text.


## Basic Usage

The `icu` configuration key takes a quoted string containing the ICU pattern. The result of the ICU evaluation replaces
the parameter in the message output.

```java
messageSupport
    .message("""
        %{unused,icu:\
            '{name} bought {count, plural, one {# book} other {# books}}'}\
        """)
    .with("name", "Alice")
    .with("count", 3)
    .format();
// "Alice bought 3 books"
```

Multiple parameters can be referenced inside a single ICU pattern. This example combines a simple substitution with a
plural expression.

```java
messageSupport
    .message("""
        %{unused,icu:\
            '{name} has {count, plural, one {# cat} other {# cats}}'}\
        """)
    .with("name", "Bob")
    .with("count", 1)
    .format();
// "Bob has 1 cat"
```

The `select` construct chooses text based on an exact string match.

```java
messageSupport
    .message("""
        %{unused,icu:\
            '{gender, select, male {He} female {She} other {They}} liked\
              the post.'}\
        """)
    .with("gender", "female")
    .format();
// "She liked the post."
```

English ordinal suffixes can be expressed with `selectordinal`.

```java
messageSupport
    .message("""
        %{unused,icu:\
            '{rank, selectordinal, one {#st} two {#nd} few {#rd} other\
             {#th}}'}\
        """)
    .with("rank", 22)
    .format();
// "22nd"
```

ICU number and date styles are also available.

```java
messageSupport
    .message("%{unused,icu:'{amount, number, currency}'}")
    .with("amount", 1234.56)
    .locale(Locale.US)
    .format();
// "$1,234.56"

messageSupport
    .message("%{unused,icu:'{ratio, number, percent}'}")
    .with("ratio", 0.75)
    .locale(Locale.US)
    .format();
// "75%"
```


## Pattern Quoting

The ICU pattern must be enclosed in quotes within the message format syntax (`'...'` or `"..."`). ICU patterns that
contain literal single quotes must escape them as `''` (two consecutive single quotes) per ICU conventions. When the
pattern is wrapped in single quotes, each ICU `''` must be written as `\'\'` to avoid prematurely ending the quoted
string. Using double quotes avoids this issue because single quotes pass through unchanged.

```java
messageSupport
    .message("""
        %{unused,icu:\
            "{name} doesn''t have\
             {count, plural, one {# item} other {# items}}"}\
        """)
    .with("name", "Alice")
    .with("count", 0)
    .format();
// "Alice doesn't have 0 items"
```


## Locale-Sensitive Formatting

The locale set on the message determines which ICU locale rules are applied. Languages with complex plural categories,
such as Polish with its four forms, are handled automatically by ICU's CLDR-based rules.

```java
messageSupport
    .message("""
        %{unused,icu:\
            '{count, plural, one {# plik} few {# pliki} many {# plików}\
             other {# pliku}}'}\
        """)
    .with("count", 22)
    .locale(Locale.forLanguageTag("pl"))
    .format();
// "22 pliki"
```

Welsh ordinals illustrate a language with six ordinal categories (zero, one, two, few, many, other).

```java
messageSupport
    .message("""
        %{unused,icu:\
            '{rank, selectordinal, zero {#ain} one {#af} two {#ail} few\
             {#ydd} many {#ed} other {#fed}}'}\
        """)
    .with("rank", 5)
    .locale(Locale.forLanguageTag("cy"))
    .format();
// "5ed"
```


## Nested Constructs

ICU constructs can be nested. A common pattern is a `select` that branches on gender, with a `plural` inside each
branch.

```java
messageSupport
    .message("""
        %{unused,icu:\
            '{gender, select,\
             male {He has {count, plural, one {# new message} other {# new messages}}}\ 
             other {She has {count, plural, one {# new message} other {# new messages}}}}'}\
        """)
    .with("gender", "male")
    .with("count", 5)
    .format();
// "He has 5 new messages"
```

The `plural` construct supports an `offset` that reduces the displayed count, allowing patterns like "Alice and 4 other
people".

```java
messageSupport
    .message("""
        %{unused,icu:\
            '{guests, plural, offset:1\ 
             =0 {Nobody is attending}\
             =1 {Only {name} is attending}\ 
             one {{name} and # other person are attending}\ 
             other {{name} and # other people are attending}}'}\
        """)
    .with("guests", 5)
    .with("name", "Alice")
    .format();
// "Alice and 4 other people are attending"
```


## Auto Application

The `icu` formatter supports automatic application. When the formatter service encounters a parameter configuration
that contains the `icu` configuration key but no explicit `format:icu`, it automatically selects the `icu` formatter.
This means `format:icu` does not need to be written every time `icu:` is used.

The following two message format strings are equivalent:

```java
// with explicit format selection
messageSupport
    .message("""
        %{unused,format:icu,icu:
            '{count, plural, one {# item} other {# items}}'}\
        """)
    .with("count", 1)
    .format();
// "1 item"

// with auto application (icu key triggers the icu formatter automatically)
messageSupport
    .message("""
        %{unused,icu:'{count, plural, one {# item} other {# items}}'}\
        """)
    .with("count", 1)
    .format();
// "1 item"
```


## Map Keys on the Result

The ICU formatter supports `empty` and `!empty` map keys on its result. After the ICU pattern is evaluated, the
resulting text is checked against map keys defined in the parameter. This allows providing fallback text when the ICU
formatting produces an empty result.

```java
messageSupport
    .message("%{unused,icu:'{name}',empty:'(anonymous)'}")
    .with("name", "")
    .format();
// "(anonymous)"

messageSupport
    .message("%{unused,icu:'{name}',empty:'(anonymous)'}")
    .with("name", "Bob")
    .format();
// "Bob"
```


## Error Handling

If the ICU pattern is syntactically invalid or if formatting fails for any reason, the formatter returns empty text
rather than throwing an exception. This prevents a broken ICU pattern from crashing the application at runtime.

```java
messageSupport
    .message("%{unused,icu:'{count, plural, }'}")
    .with("count", 5)
    .format();
// ""
```

/// warning | Silent failures
Because errors produce empty text, typos in ICU patterns can be difficult to spot in production. ICU patterns should
be verified with representative test data during development.
///


## Registration

When using `DefaultFormatterService`, the `ICUFormatter` is registered automatically via the Java `ServiceLoader`
mechanism. Having the `message-format-icu` module on the classpath is sufficient.

When using `GenericFormatterService`, the formatter must be registered manually:

```java
var formatterService = new GenericFormatterService();
formatterService.addFormatter(new ICUFormatter());

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


## Performance

The ICU `MessageFormat` is instantiated and evaluated on every formatting call. There is no caching of the compiled ICU
pattern at the formatter level. For high-throughput scenarios where the same ICU pattern is formatted repeatedly with
different parameter values, the overhead of repeated pattern compilation may be noticeable. In those cases, the core
library's built-in `choice` formatter or map keys may express the same logic without involving the ICU engine.
