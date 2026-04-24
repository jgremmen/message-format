---
title: 0.10.0 -> 0.11.0
toc_depth: 2
---

# Version [0.11.0](https://github.com/jgremmen/message-format/tree/0.11.0) (2024-10-01)

## Breaking Changes

### Minimum Java version raised to JDK 11

The project now requires Java 11 or later. Source and target compatibility have been changed from Java 8 to
Java 11. All modules are compiled and tested against JDK 11. Code that previously ran on Java 8 must be
migrated to a Java 11 runtime.

As a consequence, the JetBrains annotations dependency has changed from `annotations-java5` to `annotations`.
This is a compile-only dependency and does not affect the runtime classpath.

### Dependency version ranges narrowed

Several dependency version ranges have been tightened to reduce the risk of incompatible transitive upgrades:

| Dependency | Type | 0.10.0 | 0.11.0 |
|---|---|---|---|
| org.antlr:antlr4-runtime | compile | (transitive via antlr4-runtime-ext) | [4.13.0,4.14) |
| de.sayayi.lib:antlr4-runtime-ext | compile | [0.4.1,) | [0.5,0.6) |
| org.ow2.asm:asm | compile | [6.0,) | [9.0,10.0) |

The `antlr4-runtime` dependency is now declared explicitly as an `implementation` dependency instead of being
pulled in transitively through `antlr4-runtime-ext`. The `antlr` dependency inside `antlr4-runtime-ext` is
excluded to prevent version conflicts.

### Multiple map keys for the same config value

The grammar for parameter configuration has been extended to support grouping multiple map keys for a single
value. The `configMapElement` parser rule now returns a `List<ConfigKey>` instead of a single `ConfigKey`.

This change affects code that directly interacts with the ANTLR-generated parser context objects
(`ParameterConfigElementContext`, `ConfigMapElementContext`). The field `configKey` has been replaced by
`configKeys` (a `List<ConfigKey>`).

The new syntax uses parentheses to group keys:

```
%{param,(null,empty):'n/a','active':'yes'}
```

In this example, both the `null` and `empty` keys map to the value `n/a`.

## New Features

### `MessageConfigurer.getParameters()`

The `MessageSupport.MessageConfigurer` interface has a new method `getParameters()` that returns an unmodifiable
`Map<String,Object>` snapshot of all parameters currently configured on the message configurer. The returned map
is not backed by the configurer, so subsequent changes to the configurer are not reflected in the map.

```java
MessageSupport.MessageConfigurer<?> configurer = messageSupport
    .code("my-message")
    .with("name", "World")
    .with("count", 42);

Map<String,Object> params = configurer.getParameters();
// params = {count=42, name=World}
```

### `ResourceBundleAdopter` auto-detection methods

The `ResourceBundleAdopter` class has been extended with several overloaded `adopt(String bundleBaseName, ...)`
methods that automatically detect and load resource bundles by base name:

- `adopt(String bundleBaseName)` - scans all available locales using the default class loader
- `adopt(String bundleBaseName, ClassLoader classLoader)` - scans all available locales using the given class loader
- `adopt(String bundleBaseName, Set<Locale> locales)` - loads bundles for the specified locales only
- `adopt(String bundleBaseName, Set<Locale> locales, ClassLoader classLoader)` - loads bundles for the specified
  locales using the given class loader

When no explicit locale set is provided, missing resource bundles are silently ignored. When an explicit locale set
is provided, a `MessageAdopterException` is thrown if a bundle cannot be found.

### `SupplierDelegate` for deferred formatting

The `formatSupplier()` and `formattedExceptionSupplier()` methods in the message configurer now wrap their
return values in a `SupplierDelegate`. This ensures that the deferred supplier is properly represented when
inspected or logged.

## Bug Fixes

- `Optional.isEmpty()` is now used instead of `!Optional.isPresent()` in `OptionalFormatter` null/empty key
  comparisons, aligning with the Java 11 API.
- The `ParameterMap` returned by `getParameters()` uses binary search for key lookup, matching the sorted
  storage order of the internal parameter array.
