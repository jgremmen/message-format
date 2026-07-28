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
[ICU4J](https://unicode-org.github.io/icu/userguide/format_parse/messages/) `MessageFormat` engine. Any valid ICU
`MessageFormat` pattern can be embedded in a message format string and evaluated at runtime against the current locale.

The formatter operates on the full parameter map of the current message. All parameters passed to the message are
available inside the ICU pattern by their original names. The parameter referenced in the `%{...}` anchor does not need
to match any of the names used in the ICU pattern; its only purpose is to satisfy the message format syntax. A
meaningful parameter name can be used instead if that reads better in context, but its value is not used by the ICU
formatter itself.


## Configuration Key

The `icu` formatter recognizes a single configuration key.

### `icu`

A quoted string containing an ICU `MessageFormat` pattern. The formatter passes all message parameters as a named map
to the ICU engine, which evaluates the pattern against the locale set on the message.

Before the pattern is compiled, leading and trailing whitespace is stripped and internal runs of whitespace are 
collapsed into single spaces. This normalization allows multiline patterns in Java text blocks without affecting the 
ICU output.

When `icu` is omitted, the formatter produces empty text.


## Basic Usage

The `icu` configuration key takes a quoted string containing the ICU pattern. The result of the ICU evaluation replaces
the parameter in the message output.

```java
messageSupport
    .message("""
        %{unused,icu:\
            '{count, plural, one {# item} other {# items}}'}\
        """)
    .with("count", 5)
    .format();
// "5 items"
```

All parameters available in the message context can be referenced inside the pattern by name, regardless of which
parameter the `%{...}` anchor refers to.

```java
messageSupport
    .message("""
        %{unused,icu:\
            '{name} has {count, plural,one {# cat} other {# cats}}'}\
        """)
    .with("name", "Bob")
    .with("count", 1)
    .format();
// "Bob has 1 cat"
```

The anchor parameter name is arbitrary. It does not need to exist as a parameter and its value is never read by the
formatter. The following example uses `p` as a short throwaway name.

```java
messageSupport
    .message("%{p,icu:'{greeting}, {name}!'}")
    .with("greeting", "Hello")
    .with("name", "World")
    .format();
// "Hello, World!"
```


## Pattern Quoting

The ICU pattern must be enclosed in quotes within the message format syntax. Both single quotes (`'...'`) and double
quotes (`"..."`) are supported. Which one to choose depends on whether the ICU pattern itself contains literal single
quotes.

ICU uses `''` (two consecutive single quotes) to represent a literal single quote inside a pattern. When the
message format string wraps the pattern in single quotes, each ICU `''` must be escaped as `\'\'` to prevent the
message format parser from interpreting the quote as the end of the string. Using double quotes as the outer wrapper
avoids this issue entirely because single quotes pass through to ICU unchanged.

```java
// Double-quoted wrapper: ICU '' passes through directly
messageSupport
    .message("""
        %{p,icu:\
            "{name} doesn''t have\
             {count, plural, one {# item} other {# items}}"}\
        """)
    .with("name", "Alice")
    .with("count", 0)
    .format();
// "Alice doesn't have 0 items"

// Single-quoted wrapper: ICU '' must be written as \'\'
messageSupport
    .message("""
        %{p,icu:\
            '{name} doesn\\'\\'\\'t have\
             {count, plural,one {# item} other {# items}}'}\
        """)
    .with("name", "Alice")
    .with("count", 0)
    .format();
// "Alice doesn't have 0 items"
```

For patterns that contain literal single quotes, the double-quote wrapper is the more readable option.


## Locale

The locale set on the message determines which ICU locale rules are applied during formatting. The formatter calls
`setLocale` on the ICU `MessageFormat` instance before evaluating the pattern, so all locale-sensitive behavior inside
the pattern (plural categories, number formatting, date formatting) follows the active locale.

```java
messageSupport
    .message("""
        %{p,icu:\
            '{count, plural,\
              one {# plik}\
              few {# pliki} many {# plików}\
              other {# pliku}}'}\
        """)
    .with("count", 22)
    .locale(Locale.forLanguageTag("pl"))
    .format();
// "22 pliki"
```


## Auto Application

The `icu` formatter supports automatic application. When the formatter service encounters a parameter configuration
that contains the `icu` configuration key but no explicit `format:icu`, it automatically selects the `icu` formatter.
Writing `format:icu` is therefore optional whenever the `icu` key is present.

```java
// explicit format selection
messageSupport
    .message("""
        %{unused,format:icu,icu:\
            '{count, plural, one {# item} other {# items}}'}\
        """)
    .with("count", 1)
    .format();
// "1 item"

// auto application (equivalent)
messageSupport
    .message("""
        %{unused,icu:\
            '{count, plural, one {# item} other {# items}}'}\
        """)
    .with("count", 1)
    .format();
// "1 item"
```


## Map Keys on the Result

After the ICU pattern is evaluated, the resulting text is checked against map keys defined in the parameter. The
formatter supports the `empty` and `!empty` map key types. This allows providing fallback text when the ICU formatting
produces an empty result.

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
