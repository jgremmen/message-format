# Message Format Library

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Java](https://img.shields.io/badge/Java-21%2B-red.svg)]()
[![Maven Central](https://img.shields.io/maven-central/v/de.sayayi.lib/message-format.svg)](https://central.sonatype.com/artifact/de.sayayi.lib/message-format)

Message Format is a Java library for producing human-readable messages. Instead of constructing
text by concatenating strings, format conversions, and conditional logic in Java code, messages
are written as format strings where the focus is on the text itself. Variable parts are
represented by named parameters that the library resolves and formats automatically.

The key idea is that a parameter should produce output that reads naturally. A list of strings
should not render as `[ A, B, C, D ]`. It should come out as `A, B, C and D` or `A, B, ...` or 
simply `A, B, C, D` depending on how the parameter is configured in the format string. The calling
code just passes the value; it does not need to know or care whether that value is a `Collection` 
a `String[]` or any other type. The library's formatter layer figures out how to turn the object 
into readable text.

The library ships with formatters for all common Java types, like primitives, numbers, dates and
times (`java.time`), enums, collections, maps, `Optional`, `Path` and more. When none of the 
uilt-in formatters fit, custom formatters can be registered through the `FormatterService` SPI.
Formatters are discovered automatically via `ServiceLoader`, so adding a formatter to the classpath
is often all that is needed.

Messages can be stored centrally in a `MessageSupport` instance, each identified by a unique code.
Reusable fragments can be extracted into templates and embedded in other messages.

## Features

### Parameterized messages

Messages contain named parameter placeholders that are resolved and formatted at runtime. The
formatting is type-aware: the library selects a matching `ParameterFormatter` based on the runtime
type of the value, so the calling code never needs to perform explicit conversions. A `Number`, a
`LocalDate`, an `Enum`, a `Collection` or any other object is each formatted into readable text
automatically. Parameters can also be configured directly in the format string. For example, a
list parameter can be told to join its elements with commas, to abbreviate after a certain number
of items, or to insert "and" before the last element. In addition to type-based formatters, named
formatters such as `bool`, `choice`, `size` and `string` are available for common formatting
patterns.

### Locale support

A message can carry multiple locale-specific variants. When the message is formatted, the library
selects the best matching variant for the requested `Locale`, falling back through the locale
hierarchy. This makes it straightforward to maintain multilingual applications: all translations
for a given message code live together, and the selection logic is handled by the library.

### Templates

Frequently used message fragments can be defined as templates and embedded in other messages by
reference. Templates are parsed and resolved like regular messages, including full parameter and
locale support. This avoids duplication and provides a single place to update shared text such as
product names, legal phrases, or recurring sentence patterns.

### Annotations

Messages and templates can be declared directly in Java source code using `@MessageDef` and
`@TemplateDef` annotations. Each annotation carries a message code (or template name) and one or
more locale-tagged format strings. Annotated classes are picked up by an adopter that extracts
the definitions and publishes them to a `MessageSupport` instance. This keeps message definitions
close to the code that uses them while still allowing them to be managed centrally.

### Import and export

Messages and templates can be serialized into a compact binary pack format (`.mfp`). A pack file
contains pre-compiled message definitions that can be loaded into a `MessageSupport` instance at
application startup. This is the recommended way to distribute message definitions: the Gradle
plugin produces a single `.mfp` file during the build, and the application imports it at runtime
with a single method call. The pack format also provides as a compatibility mechanism, so older pack
files can be read by newer versions of the library.

### Adopters

Adopters are pluggable readers that import messages from external sources into a `MessageSupport`
instance. The core module includes adopters for `ResourceBundle` and `Properties` files. The
annotations module adds an adopter for `@MessageDef` / `@TemplateDef` annotations, and the ASM
module provides a variant that works at the bytecode level. It can extract definitions from
classes that are already loaded as well as from classes that are not present in the JVM. The
Spring module contributes a Spring-aware ASM adopter that uses Spring's `ResourceLoader` for
classpath scanning. Custom adopters can be implemented by extending `AbstractMessageAdopter`.

### Log4j integration

The `message-format-log4j` module integrates Message Format with Apache Log4j. It provides
`Log4jMessageFactory`, a Log4j `MessageFactory` implementation that uses message-format syntax
for formatting log messages. Parameters passed to the logger are made available in the message
template as `p1`, `p2`, etc. If the message string does not contain message-format placeholders
but uses Log4j-style `{}` placeholders, the factory can optionally fall back to Log4j's
`ParameterizedMessage` for backward compatibility.

### Spring integration

The `message-format-spring` module bridges Message Format into the Spring ecosystem. It provides
`MessageSupportMessageSource`, a `HierarchicalMessageSource` implementation that delegates
message resolution and formatting to a `MessageSupport` instance. Positional `Object[]` arguments
from the Spring `MessageSource` API are mapped to named parameters (`p1`, `p2`, …) with a
configurable prefix. The module also registers a `SpELFormatter` that evaluates Spring Expression
Language expressions inside message format strings, and includes `SpringAsmAnnotationAdopter` for
classpath scanning of annotated message definitions using Spring's resource infrastructure.

### Gradle plugin

The `de.sayayi.plugin.gradle.message` Gradle plugin automates message packing as part of the
build. It scans the project's compiled classes for `@MessageDef` and `@TemplateDef` annotations,
pre-compiles the format strings, and writes all definitions into a single `.mfp` pack file. The
plugin provides a `messageFormat` extension for configuring the source sets to scan and the
strategy for handling duplicate message codes. The resulting pack file is added to the project's
resources so it is included in the final artifact automatically.

## Modules

| Module                                                                                                  | Description |
|---------------------------------------------------------------------------------------------------------|---|
| message-format&nbsp;&nbsp;[📘](https://javadoc.io/doc/de.sayayi.lib/message-format/0.20.1)              | Core library: parsing, formatting, adopters, pack format, and the formatter SPI |
| message-format-annotations&nbsp;&nbsp;[📘](https://javadoc.io/doc/de.sayayi.lib/message-format-annotations/0.20.1) | `@MessageDef`, `@TemplateDef` and related annotations |
| message-format-asm&nbsp;&nbsp;[📘](https://javadoc.io/doc/de.sayayi.lib/message-format-asm/0.20.1)               | ASM-based bytecode scanner for annotation-defined messages |
| message-format-log4j&nbsp;&nbsp;[📘](https://javadoc.io/doc/de.sayayi.lib/message-format-log4j/0.21.0)           | Log4j integration: `MessageFactory` using message-format syntax |
| message-format-spring&nbsp;&nbsp;[📘](https://javadoc.io/doc/de.sayayi.lib/message-format-spring/0.20.1)         | Spring `MessageSource` bridge, SpEL formatter, classpath scanning |
| message-gradle-plugin                                                                                  | Gradle plugin for build-time annotation scanning and message packing |

## Quick Example

```java
MessageSupport messageSupport = MessageSupportFactory.shared();

String text = messageSupport
    .message("%{n,choice,0:'no results',1:'1 result',>1:'%{n} results'} found")
    .with("n", resultCount)
    .format();
// "no results found"   (n = 0)
// "1 result found"     (n = 1)
// "42 results found"   (n = 42)
```

<!--
## Documentation

The full documentation is available at [lib.sayayi.de/message-format](https://lib.sayayi.de/message-format).
-->

## License

This project is licensed under the [Apache License 2.0](LICENSE).
