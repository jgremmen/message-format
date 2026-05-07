# Message Format User Manual

**message-format** is a type-safe, locale-aware message formatting library for Java. It parses
message format strings into reusable `Message` objects and formats them with named parameters,
locale selection, and extensible formatters.

This manual covers the [core library](core/index.md) with its message syntax, formatting API,
formatter service, and pack file support. It then describes how messages and templates can be
declared through [annotations](adopter/annotation/index.md) and how those annotations are
discovered, either via [ASM bytecode scanning](adopter/annotation/asm.md) or at build time with
the [Gradle plugin](gradle-plugin/index.md). Integration guides for
[Apache Log4j 2](add-on/log4j.md) and [Spring Framework](add-on/spring/message-source.md) explain how to
use message-format within those ecosystems. A section on
[formatter development](formatter/development/index.md) covers how to write custom parameter
formatters, post formatters, and how to register them with the formatter service.
