---
icon: material/paperclip-plus
---

# Add-Ons

Add-on modules integrate the message format library with external frameworks. Each module is published as a separate
Maven artifact so that only the dependencies actually needed are pulled in.

The [ICU](../formatter/parameter/named/icu.md) add-on provides named formatters that delegate to the ICU4J engine. The
[`icu`](../formatter/parameter/named/icu.md) formatter enables locale-sensitive plural rules, gender-based selection,
ordinal formatting, and ICU number and date styles within message format strings. The
[`icu-person`](../formatter/parameter/named/icu-person.md) formatter assembles person names from individual name part
parameters with configurable formatting and ordering.

The [Log4j](log4j.md) add-on provides a `Log4jMessageFactory` that plugs into Apache Log4j2's logging API. It replaces
Log4j's default `{}` placeholder formatting with the full message format syntax, mapping positional log arguments to 
named parameters `p1`, `p2` and so on. Messages are formatted lazily, so the formatting cost is only paid when a
log statement actually reaches an appender.

The [Spring](spring/message-source.md) add-on bridges Spring's `MessageSource` interface to a `MessageSupport` instance. Any Spring
component that resolves messages through `MessageSource`, such as Thymeleaf templates or validation error messages, can 
use the message format syntax without changes on the consumer side.
