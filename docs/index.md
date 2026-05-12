---
icon: material/home-outline
---

# Message Format User Manual

**message-format** is a type-safe, locale-aware message formatting library for Java. It parses
message format strings into reusable `Message` objects and formats them with named parameters,
locale selection, and extensible formatters. The goal is to produce text that reads like a
proper sentence rather than a mechanical concatenation of placeholder values.

The [Message](message/index.md) section is the starting point. It introduces the format syntax
and explains how to turn a format string into a formatted result through the formatting API.

[Configuration](configuration/index.md) builds on that foundation and describes the central
components that drive the formatting pipeline. It covers `MessageSupport` and `MessageFactory`,
the formatter service, default configuration values, pack files for precompiled message
bundles, and how the library handles exceptions.

The library ships with a wide range of built-in [Formatters](formatter/index.md).
[Type-specific](formatter/parameter/typed/index.md) formatters handle common Java types such
as numbers, dates, enums, and collections. [Named](formatter/parameter/named/index.md)
formatters apply explicit formatting logic like choice selection or geographic coordinates.
[Post formatters](formatter/post/index.md) operate on already-formatted output, for example
by changing its case or clipping it to a maximum length. If the built-in set does not cover
your needs, the [development guide](formatter/development/index.md) walks through writing and
registering custom formatters.

[Adopters](adopter/index.md) covers the different ways to populate a `MessageSupport` instance
with messages and templates. Messages can be declared through
[annotations](adopter/annotation/index.md) and discovered at runtime via
[ASM bytecode scanning](adopter/annotation/asm.md) or through
[Spring's component scanning](adopter/annotation/spring.md). Alternatively, messages can be
loaded from [properties files](adopter/properties.md) or standard Java
[resource bundles](adopter/resource-bundle.md).

The [Add-Ons](add-on/index.md) section describes integration with external frameworks.
[Apache Log4j2](add-on/log4j.md) integration replaces Log4j's default message formatting with
the message format syntax. The [Spring Framework](add-on/spring/message-source.md) integration
provides a `MessageSource` backed by the message format engine.

The [Gradle Plugin](gradle-plugin/index.md) automates annotation scanning and pack file
generation at build time, so messages are precompiled and ready to use without runtime
classpath scanning.
