---
icon: material/puzzle-edit-outline
---

# Formatter Development

The formatting engine is designed to be extensible. Every built-in formatter uses the same
interfaces and registration mechanisms that are available to you. When a built-in formatter
does not cover your needs, you write your own implementation, register it with the formatter
service, and the library treats it exactly like one of its own.

There are four extension points, each targeting a different aspect of the formatting
pipeline. The sections below introduce each one and explain when to use it. The dedicated
pages linked from each section provide full implementation guidance, complete code examples
and registration details.


## Parameter Formatters

A parameter formatter converts a Java object into text. When the library encounters a
parameter reference such as `%{amount}`, it resolves the bound value and passes it to a
parameter formatter. The formatter decides how to represent that value as a string, taking
into account configuration keys, map entries and the current locale.

The library supports two kinds of parameter formatters. A **typed parameter formatter**
is selected automatically based on the runtime type of the parameter value. If you have a
custom domain class like `Color` or `HttpStatus` and want the library to format it whenever
it appears in a message, you write a typed formatter. The framework walks the value's type
hierarchy, finds the best matching formatter by type and priority, and invokes it. You
control the priority through the `FormattableType` order value.

A **named parameter formatter** is selected explicitly by the message author through the
`format:<name>` syntax. Named formatters are useful when the desired output has nothing to
do with how the value's type is normally formatted. A numeric value might need to be rendered
as an elapsed time, a percentage or a hex string, depending on context. Named formatters can
also declare formattable types, making them hybrid formatters reachable both by name and by
type.

Both flavors share the same registration mechanism on `DefaultFormatterService` and can be
discovered automatically through the Java `ServiceLoader`. The
[Custom Type-Specific Parameter Formatter](typed-parameter-formatter.md) and
[Custom Named Parameter Formatter](named-parameter-formatter.md) pages cover each variant
in detail.


## Post Formatters

A post formatter transforms text that has already been produced. Unlike a parameter
formatter, which works with a Java object, a post formatter receives the string output of a
sub-message and modifies it. The built-in `case` and `clip` post formatters handle common
transformations like changing letter case and truncating text.

Post formatters are invoked using the `%(name, 'sub-message', ...)` syntax. The sub-message
is first formatted as a regular message, and the resulting string is then passed to the post
formatter. Any transformation that operates purely on textual output rather than on the
original Java value is a natural fit for a post formatter.

The `PostFormatter` interface requires only two methods: `getName()` for identification and
`format(String, PostFormatterContext)` for the transformation itself. The
[Custom Post Formatter](post-formatter.md) page explains the interface, the context, and
provides complete implementation examples.


## Size Queryable

The `SizeQueryable` interface is a sub-interface of `ParameterFormatter` that adds the
ability to report the "size" of a parameter value. When a message uses
`%{items,format:size}`, the built-in `size` formatter does not compute the size itself.
Instead, it finds a `SizeQueryable` formatter registered for the value's runtime type and
calls its `size` method.

If you introduce a custom container type that has a meaningful notion of size, implementing
`SizeQueryable` lets the `size` named formatter work with your type transparently. You can
combine size reporting with formatting in a single class, delegate to a wrapped value's
formatter, or create a standalone size-only formatter that leaves all formatting to the next
formatter in the chain.

The `size` method returns an `OptionalLong`. Returning a present value provides the computed
size; returning empty signals that the framework should continue searching. The
[Custom Size Queryable](size-queryable.md) page covers all implementation patterns and
registration details.


## Map Key Comparator

When a message contains map entries such as `%{status,200:'OK',404:'Not Found'}`, the
formatting engine needs to determine which entry matches the current parameter value. The
`MapKeyComparator<T>` interface is the contract that a formatter implements to define how a
value of type `T` compares against each kind of map key: string, number, bool, null and
empty.

Every built-in formatter that supports map key matching already implements
`MapKeyComparator`. When you write a formatter for your own type and want map entries to
work with it, you implement `MapKeyComparator` yourself, overriding only the comparison
methods that make sense for your type. Each method returns a `MatchResult` that indicates
how well the value matches the key. When multiple keys match, the engine picks the one with
the highest score.

The library also provides `AbstractMapKeyComparator<T>` for cases where you only want to
add map key comparison logic to a type that already has a satisfactory formatter. The base
class delegates all formatting to the next formatter in the chain, so your subclass
contributes nothing but comparison behavior. The
[Custom Map Key Comparator](map-key-comparator.md) page explains the interface, the scoring
system, the comparator context, and provides complete examples for both integrated and
standalone comparators.
