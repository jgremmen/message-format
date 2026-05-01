# Message Format User Manual

**message-format** is a type-safe, locale-aware message formatting library for Java. It parses
message format strings into reusable `Message` objects and formats them with named parameters,
locale selection, and extensible formatters.

The library is split into several modules, each published as its own JAR:

- **[Core Library](core/index.md)** -- The main library providing message parsing, formatting,
  the formatter service, adopters, pack file support and utilities.

- **[Annotations](annotations/index.md)** -- Compile-time annotations (`@MessageDef`,
  `@TemplateDef`) for declaring messages and templates directly in source code.

- **[ASM](asm/index.md)** -- Bytecode-level scanning of message annotations from compiled
  class files using the ASM library -- no class loading required.

- **[Log4j](log4j/index.md)** -- Apache Log4j 2 integration -- use message-format syntax
  directly in log statements.

- **[Spring](spring/index.md)** -- Spring Framework integration with `MessageSource` adapter,
  classpath scanning via Spring ASM, and a SpEL parameter formatter.

- **[Gradle Plugin](gradle-plugin/index.md)** -- Gradle plugin for scanning compiled classes
  and producing packed message files (`.mfp`) at build time.
