---
title: 0.22.0 -> 0.23.0
toc_depth: 2
---

# Version [0.23.0](https://github.com/jgremmen/message-format/tree/0.23.0) (2026-05-13)

## Breaking Changes

### `MessageFactory.NO_CACHE_INSTANCE` replaced by `MessageFactory.getSharedInstance()`

The public static field `MessageFactory.NO_CACHE_INSTANCE` has been removed. It is replaced by the static method
`MessageFactory.getSharedInstance()`, which returns a lazily initialized, thread-safe singleton. Unlike
`NO_CACHE_INSTANCE`, the shared instance includes a message cache with a maximum size of 128 entries.

If you referenced `NO_CACHE_INSTANCE` directly:

```java
// before
MessageFactory factory = MessageFactory.NO_CACHE_INSTANCE;

// after
MessageFactory factory = MessageFactory.getSharedInstance();
```

If you passed `NO_CACHE_INSTANCE` to `MessageSupportFactory.create`:

```java
// before
var messageSupport = MessageSupportFactory.create(formatterService, MessageFactory.NO_CACHE_INSTANCE);

// after
var messageSupport = MessageSupportFactory.create(formatterService);
```

The convenience method `MessageSupportFactory.create(FormatterService)` now delegates to `getSharedInstance()`
internally.

### `MessageBuilder` map operator methods return `MapValueBuilder`

The methods `eq()`, `ne()`, `lt()`, `lte()`, `gt()` and `gte()` on `MapEqualityBuilder` and `MapRelationalBuilder`
now return `MapValueBuilder` instead of their previous, more specific return types (`MapEqualityBuilder` and
`MapRelationalBuilder` respectively). The `@Contract("-> this")` annotation has been removed from these methods.

If your code relied on chaining an operator method followed by another operator method on the same builder, this is
no longer possible after the operator has been set. Assign the operator before adding the map value:

```java
// before (compiled because eq() returned MapEqualityBuilder)
builder.forString("x").eq().message("...");

// after (still works, eq() now returns MapValueBuilder)
builder.forString("x").eq().message("...");
```

The common usage pattern of calling an operator followed by `.message(...)` or `.text(...)` is unaffected, since
`MapValueBuilder` declares both of these methods. Code that called a second operator after the first (e.g.
`.eq().ne()`) will no longer compile; this was not a meaningful pattern.


## New Features

### `CharsetFormatter` for `java.nio.charset.Charset` values

A new typed parameter formatter handles `java.nio.charset.Charset` values. When no string map key matches, the
charset's locale-sensitive display name is used as the formatted result.

Map key comparison supports `string` keys matched against the charset's canonical name and aliases. Only equality
(`EQ`) and inequality (`NE`) comparison types are supported:

- `EQ`: a canonical name match returns `EXACT`; an alias match returns `EQUIVALENT`.
- `NE`: if neither the canonical name nor any alias matches the key, `EXACT` is returned; if only an alias
  matches, `LENIENT` is returned; a canonical name match results in `MISMATCH`.

```msgfmt
%{encoding, 'UTF-8':'Unicode (8-bit)', 'ISO-8859-1':'Latin-1', :'unknown'}
```

The formatter is registered automatically via the `ParameterFormatter` service loader.

### Nested builder messages in `MapValueBuilder`

`MapValueBuilder` has a new method `message(Consumer<MessageBuilder>)` that allows constructing the map value
message using a nested builder callback instead of a format string:

```java
MessageBuilder.create()
    .parameter("status")
        .forNumber(0).message(mb -> mb.text("inactive"))
        .forNumber(1).message(mb -> mb
            .text("active since")
            .parameter("date").withFormat("date"))
    .build();
```

The consumer must not invoke `build()` or `buildWithCode(String)` on the provided builder; the message is built
automatically when the consumer returns.

### Thread-safe `SupplierDelegate`

`SupplierDelegate` is now thread-safe. The delegate supplier is invoked at most once, even when `get()` is called
concurrently from multiple threads. The implementation uses double-checked locking with `volatile` fields. After the
delegate has been invoked, the lock is released and subsequent calls return the cached value without
synchronization overhead.

### Thread-safe shared singletons with double-checked locking

`DefaultFormatterService.getSharedInstance()`, `MessageSupportFactory.shared()` and the new
`MessageFactory.getSharedInstance()` now use double-checked locking with `volatile` fields. This reduces lock
contention on the common path where the singleton is already initialized.

### `MessageUtil.serializeMessage` utility method

A new static method `MessageUtil.serializeMessage(Context, Message, boolean)` serializes a message into its format
string representation. The method automatically chooses the quote character: if any text part contains a single
quote, a double quote is used; otherwise a single quote is used. The `forceQuoted` parameter controls whether
simple name-like text messages are serialized without quotes.


## Bug Fixes

*There are no bug fixes in this version.*

