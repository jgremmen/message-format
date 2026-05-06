---
icon: material/format-textbox
---

# Formatters

Formatters are responsible for producing the text output of parameter and post formatter
message parts. Every time the library encounters a `%{...}` parameter reference or a
`%(...)` post formatter invocation in a message format string, it delegates to a formatter
to produce the corresponding piece of text.

The library distinguishes between two kinds of formatters that serve fundamentally different
purposes.

**Parameter formatters** operate on raw Java values. When a parameter reference like
`%{amount}` is encountered, the library looks up the value bound to that name and hands it to
a parameter formatter. The formatter is responsible for converting the value into its textual
representation, taking into account any configuration keys and map entries declared in the
message as well as the current locale. Parameter formatters can be selected automatically
based on the Java type of the value, or explicitly by name using the `format` configuration
key. Everything about parameter formatters, including the type-based selection mechanism,
the built-in typed formatters and the available named formatters, is covered on the
[Parameter Formatters](parameter/index.md) page.

**Post formatters** operate on already formatted text. Rather than converting a Java object,
a post formatter receives the text output of a sub-message and applies a transformation to
it. Typical transformations include changing letter case or truncating the text to a maximum
length. Post formatters are invoked using the `%(...)` syntax and are identified by name. The
[Post Formatters](post/index.md) page explains how they work, how to configure them, and how
to implement your own.
