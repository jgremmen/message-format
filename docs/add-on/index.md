---
icon: material/paperclip-plus
---

# Add-Ons

Add-on modules integrate the message format library with external frameworks. Each module
is published as a separate Maven artifact so you only pull in the dependencies you actually
need.

The [Log4j](log4j.md) add-on provides a `Log4jMessageFactory` that plugs into
Apache Log4j2's logging API. It replaces Log4j's default `{}` placeholder formatting with
the full message format syntax, mapping positional log arguments to named parameters `p1`,
`p2` and so on. Messages are formatted lazily, so the formatting cost is only paid when a
log statement actually reaches an appender.

The [Spring](spring/message-source.md) add-on bridges Spring's `MessageSource` interface to
a `MessageSupport` instance. Any Spring component that resolves messages through
`MessageSource`, such as Thymeleaf templates or validation error messages, can use the
message format syntax without changes on the consumer side.
