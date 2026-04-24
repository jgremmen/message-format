---
title: 0.20.0 -> 0.20.1
toc_depth: 2
---

# Version [0.20.1](https://github.com/jgremmen/message-format/tree/0.20.1) (2025-07-06)

## Breaking Changes

### Pack file format version changed to 2

The internal pack serialization format version has been incremented from `1` to `2`. Numeric
configuration keys and values are now serialized using a variable-length encoding scheme that
produces significantly smaller output for values in the lower long range, which covers the
majority of practical use cases.

Pack files created with version 0.20.0 (format version 1) can still be read by 0.20.1. The
deserialization code checks the pack version and falls back to the fixed-width `readLong()` format
when reading version 1 files. However, newly created pack files use format version 2 and cannot be
read by 0.20.0. Re-export all pack files after upgrading.

### Dependency changes

| Dependency | Type | 0.20.0 | 0.20.1 |
|---|---|---|---|
| de.sayayi.lib:pack | compile | [0.1,0.2) | [0.1.3,0.2) |


## Bug Fixes

- Fixed parameter name parsing to accept common naming structures such as names containing digits after a dash (e.g. `my-2nd-param`). Previously, the lexer required each segment after a dash to start with a letter.
- Fixed compatibility issue with `de.sayayi.lib:pack` version 0.1.0 which had a bug reading long values. The minimum required version has been raised to 0.1.3.
