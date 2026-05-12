---
icon: material/message-cog-outline
---

# Message Format Configuration

The configuration section covers the components you assemble to set up the message format
library in your application. At the center sits
[`MessageSupport`](message-support.md), the single entry point through which all formatting
flows. It combines a [`MessageFactory`](message-factory.md) for parsing format strings into
`Message` objects with a [formatter service](formatter-service.md) that knows how to convert
Java values into text.

A `MessageSupport` can hold a registry of named
[messages and templates](messages-and-templates.md). Messages are registered with a code and
can then be looked up at format time, while templates provide reusable message fragments that
other messages can reference. Both can be added programmatically, discovered from annotations,
or loaded from [pack files](pack-files.md). Pack files are a compact binary format that lets
you precompile all messages at build time and load them at startup without any parsing
overhead.

Formatters often accept configuration keys that control their behavior, such as date patterns
or list separators. Rather than repeating these keys in every format string, you can register
application-wide fallback values through
[default configuration](default-configuration.md). Per-message keys always take precedence,
so defaults act as a baseline that individual messages can override selectively.
