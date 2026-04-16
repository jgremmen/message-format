# Version [0.10.0](https://github.com/jgremmen/message-format/tree/0.10.0) (2024-06-09)

## Breaking Changes

### `ConfigKeyComparator` interface redesigned

The `ConfigKeyComparator<T>` interface has been fundamentally reworked. The single method
`compareToConfigKey(@NotNull T value, @NotNull ComparatorContext context)` has been removed and replaced by five
type-specific default methods:

- `compareToNullKey(T value, @NotNull ComparatorContext context)`
- `compareToEmptyKey(T value, @NotNull ComparatorContext context)`
- `compareToBoolKey(@NotNull T value, @NotNull ComparatorContext context)`
- `compareToNumberKey(@NotNull T value, @NotNull ComparatorContext context)`
- `compareToStringKey(@NotNull T value, @NotNull ComparatorContext context)`

The dispatch logic that was previously the responsibility of each `ConfigKeyComparator` implementation (typically a
`switch` on `context.getKeyType()`) has been moved into the `ConfigKey.Type` enum itself. Each `Type` constant now
has a `compareValueToKey(...)` method that calls the appropriate comparator method.

All default implementations return `MISMATCH` for `compareToBoolKey`, `compareToNumberKey` and `compareToStringKey`.
The defaults for `compareToNullKey` and `compareToEmptyKey` use the new static factory methods
`MatchResult.forNullKey(...)` and `MatchResult.forEmptyKey(...)`.

Existing implementations must migrate. For example:

```java
// Before (0.9.1)
@Override
public @NotNull MatchResult compareToConfigKey(@NotNull MyType value,
    @NotNull ComparatorContext context) {
  switch (context.getKeyType()) {
    case STRING:
      return context.getCompareType()
          .match(value.getName().compareTo(context.getStringKeyValue()))
              ? EQUIVALENT : MISMATCH;
    case BOOL:
      return (value.isActive() == context.getBoolKeyValue())
          ? EXACT : MISMATCH;
    default:
      return MISMATCH;
  }
}

// After (0.10.0)
@Override
public @NotNull MatchResult compareToStringKey(@NotNull MyType value,
    @NotNull ComparatorContext context) {
  return context.getCompareType()
      .match(value.getName().compareTo(context.getStringKeyValue()))
          ? EQUIVALENT : MISMATCH;
}

@Override
public @NotNull MatchResult compareToBoolKey(@NotNull MyType value,
    @NotNull ComparatorContext context) {
  return (value.isActive() == context.getBoolKeyValue())
      ? EXACT : MISMATCH;
}
```

Note that `compareToNullKey` and `compareToEmptyKey` accept a nullable `value` parameter (no `@NotNull`
annotation), whereas the other three methods require a non-null value.

### `MatchResult` changed from enum to interface

`ConfigKey.MatchResult` is no longer an enum. It has been changed to a `@FunctionalInterface` with a single method
`int value()`. The previously defined enum constants have been moved into the inner enum `MatchResult.Defined` and
their semantics have been refined:

| 0.9.1 | 0.10.0 equivalent |
|---|---|
| `MISMATCH` | `Defined.MISMATCH` |
| `TYPELESS_LENIENT` | removed (use `Defined.NOT_EMPTY` or `Defined.NOT_NULL`) |
| `TYPELESS_EXACT` | removed (use `Defined.EMPTY` or `Defined.NULL`) |
| `LENIENT` | `Defined.LENIENT` |
| `EQUIVALENT` | `Defined.EQUIVALENT` |
| `EXACT` | `Defined.EXACT` |

The new `Defined` enum adds `NOT_EMPTY`, `NOT_NULL`, `EMPTY` and `NULL` constants for more precise null/empty
matching. The `MatchResult` interface also provides:

- `boolean isMismatch()` - returns `true` if `value() <= 0`
- `static int compare(MatchResult, MatchResult)` - replaces the previous `compareTo` usage
- `static MatchResult forNullKey(CompareType, boolean isNull)` - creates the correct result for null key matching
- `static MatchResult forEmptyKey(CompareType, boolean isEmpty)` - creates the correct result for empty key matching

Since `MatchResult` is now a functional interface, custom match results can be created as lambdas:

```java
// Custom match result with a priority between EMPTY and LENIENT
return () -> Defined.EMPTY.value() - 1;
```

Code that previously compared `MatchResult` values using `compareTo` must switch to `MatchResult.compare(r1, r2)`.
Code that tested for specific enum constants (e.g., `result == EXACT`) should use the `Defined` enum or
`result.isMismatch()`.

### `DefaultFormatter` now extends `ConfigKeyComparator<Object>`

The marker interface `ParameterFormatter.DefaultFormatter` now extends `ConfigKeyComparator<Object>`. Any class
implementing `DefaultFormatter` must ensure it is compatible with the `ConfigKeyComparator` contract. Since all
methods of `ConfigKeyComparator` have default implementations, no immediate code changes are required. However, the
default formatter is now included in the config key comparison chain, which changes the matching behavior for
`Object.class` formatters.

### `FormatterContext.getMessageSupport()` renamed to `getMessageAccessor()`

The method `getMessageSupport()` on the `FormatterContext` interface has been renamed to `getMessageAccessor()` to
better reflect its return type (`MessageAccessor`). All formatter implementations that call this method must be
updated.

### `FileFormatter` removed; `PathFormatter` now handles `File` and `Path`

The `FileFormatter` class has been removed entirely. The `PathFormatter` now handles both `java.io.File` and
`java.nio.file.Path` values. It extends `AbstractParameterFormatter<Object>` instead of
`AbstractSingleTypeParameterFormatter<Path>` and registers itself for both `File.class` and `Path.class` via
`getFormattableTypes()`.

The `PathFormatter` also gained new format options for the `path` configuration key:

- `absolute-path` - the absolute path
- `real-path` - the real path (resolving symlinks)
- `normalized-path` - the normalized path

The `size` query now reads the file size using `java.nio.file.Files.size(...)` instead of delegating to the
removed `FileFormatter`.

### `TemplatePart` constructor requires parameter delegate map

The `TemplatePart` constructor has an additional parameter `@NotNull Map<String,String> parameterDelegates`. This
map associates template-internal parameter names with external parameter names. When constructing `TemplatePart`
instances directly, pass `Collections.emptyMap()` for the new parameter if no delegation is needed.

### `ConfigKeyNumber` constructor changed

The constructor `ConfigKeyNumber(CompareType, String)` that parsed a string as a number has been removed. Use the
constructor `ConfigKeyNumber(CompareType, long)` instead, which was previously private and is now public.

### `ComparatorContext.matchForObject` parameter nullability

The method `ComparatorContext.matchForObject(T value, Class<T> valueType)` now accepts a nullable `value`
parameter (the `@NotNull` annotation has been removed). This reflects the fact that null values are now handled
through the `compareToNullKey` and `compareToEmptyKey` methods of `ConfigKeyComparator`.

### `ConfigKey.Type.isNullOrEmpty()` removed

The method `isNullOrEmpty()` on `ConfigKey.Type` has been removed. Each `Type` enum constant now has an abstract
method `compareValueToKey(...)` instead. Code that tested `keyType.isNullOrEmpty()` should use the individual
`compareTo*Key` methods.

### message-format-spring dependency change

The `message-format-spring` module now depends on `spring-context` instead of `spring-expression`. Since
`spring-context` transitively includes `spring-expression`, this is not a problem for projects that already
have `spring-context` on the classpath. However, projects that only depended on `spring-expression` (without
`spring-context`) will now pull in the additional Spring context module.

| Dependency | Type | 0.9.1 | 0.10.0 |
|---|---|---|---|
| org.springframework:spring-expression | compile | [5.0,6.0) | (removed) |
| org.springframework:spring-context | compile | (not present) | [5.0,6.0) |

## New Features

### `MessageSupportMessageSource` (Spring)

A new `MessageSupportMessageSource` class has been added to the `message-format-spring` module. It implements
Spring's `HierarchicalMessageSource` interface, allowing `MessageSupport` to be used as a Spring `MessageSource`.

Spring `MessageSource` arguments (the `Object[] args` parameter) are mapped to numbered message parameters using a
configurable prefix. The default prefix is `p`, so arguments are available as parameters `p1`, `p2`, `p3`, etc.

```java
MessageSupport messageSupport = ...;

// Default prefix "p": args are mapped to p1, p2, p3, ...
MessageSupportMessageSource source =
    new MessageSupportMessageSource(messageSupport);

// Custom prefix "arg": args are mapped to arg1, arg2, arg3, ...
MessageSupportMessageSource source =
    new MessageSupportMessageSource("arg", messageSupport);
```

The message source supports a parent `MessageSource` via `setParentMessageSource(...)`. If a message code is not
found in the `MessageSupport`, the parent source is consulted before falling back to the default message or
throwing `NoSuchMessageException`.

### `MethodParameterFormatter`

A new formatter for `java.lang.reflect.Parameter` values has been added. It supports the following format options
via the `parameter` configuration key:

- `name` - the parameter name (requires compilation with `-parameters`)
- `class` - the parameter type
- `default` - the full parameter string representation (default)

The formatter also implements `ConfigKeyComparator<Parameter>` and supports string key comparison against the
parameter name.

### `AbstractConfigKeyComparator<T>`

A new abstract base class `AbstractConfigKeyComparator<T>` has been added. It extends
`AbstractSingleTypeParameterFormatter<T>` and implements `ConfigKeyComparator<T>`. The `formatValue` method is
declared `final` and delegates to the next formatter in the chain. This class is intended for formatters that only
participate in config key comparison but do not perform formatting themselves.

### Parameter delegation in templates

Templates now support parameter delegation. Inside a template definition, parameters used by the template message
can be mapped to different parameter names provided by the caller. The syntax uses `=` to define a delegation:

```
%{tpl,templateParam=callerParam}
```

In this example, when the template `tpl` references the parameter `templateParam`, the value of `callerParam` from
the surrounding context is used instead.

### String as parameter name

The message parser now accepts quoted strings as parameter names in parameter parts. Previously, parameter names
were restricted to the `nameOrKeyword` grammar rule. With this change, a `simpleString` is accepted, allowing
parameter names that contain characters not permitted in identifiers.

### `ConfigKeyString` convenience constructor

A new single-argument constructor `ConfigKeyString(@NotNull String string)` has been added that defaults the
comparison type to `EQ`. This is a convenience for the common case of exact string matching.

### `SortedArrayMap<K,V>` utility class

A new immutable `SortedArrayMap<K extends Comparable<? super K>, V>` class has been added. It stores entries in a
sorted array, minimizing memory overhead. The class implements `Iterable<Map.Entry<K,V>>` and provides `stream()`,
`findValue(K)`, `getKeys(Class<K>)`, `size()` and `isEmpty()` methods. It is used internally by `TemplatePart` for
the default parameter and parameter delegate maps.

### Primitive array type resolution

The `GenericFormatterService` now maps primitive array types to their wrapper array equivalents for formatter
resolution. For example, `int[]` is mapped to `Integer[]` and `boolean[]` is mapped to `Boolean[]`. Additionally,
`boolean.class` is now included in the primitive-to-wrapper mapping (it was missing in 0.9.1).

### `PathFormatter` additional format options

The `PathFormatter` gained the following new format options for the `path` configuration key: `absolute-path`,
`real-path` and `normalized-path`. See the breaking changes section for details.

## Bug Fixes

- `ByteArrayFormatter` now implements `SizeQueryable`, returning the byte array length.
- Null value handling in config key matching has been reworked. Previously, `null` values were handled in a
  separate code path (`getMessage_findNull`) that could miss comparator-based matching. Now, null values are
  dispatched through the same `findBestMatch` logic as non-null values, using `compareToNullKey` and
  `compareToEmptyKey`.
- The `ParameterConfig` matching loop no longer exits early on `EXACT` match. Previously, the loop would stop
  at the first `EXACT` match, potentially missing a better match from a higher-priority formatter.
- `ParameterFormatterContext.format(Message.WithSpaces)` now uses `formatAsText` instead of `format` to preserve
  leading/trailing space information from sub-messages.
