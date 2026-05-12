---
icon: material/message-text-outline
---

# Working with Messages

Messages are the core concept of the message-format library. A message is a text pattern
that can contain literal text, parameter references, template references and post formatter
invocations. When formatted with concrete parameter values and a locale, it produces a
human-readable string.

Parsing and formatting are separate steps. The library compiles a format string into an
immutable `Message` object once, and that object can then be reused across threads with
different parameter values and locales each time.

The [Syntax](syntax.md) page describes the format string grammar in detail. It starts with
plain text and escape sequences, then moves on to parameter references that insert formatted
values into the output. Parameters can carry configuration keys that control how the value
is rendered, and map keys that select different text based on the parameter's value. The page
also covers template references for reusing common message fragments and post formatters
that transform the formatted output.

The [Formatting](formatting.md) page covers the runtime API. It explains how to obtain a
`MessageConfigurer` from a `MessageSupport` instance, either by message code, inline format
string, or pre-parsed `Message` object. From there it walks through setting parameters,
choosing a locale, producing the formatted result immediately or through a deferred
`Supplier`, and creating exceptions whose messages are formatted by the library.
