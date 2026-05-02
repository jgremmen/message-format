---
title: Type Formatters
---

# Type-Specific Formatters

TODO


## Reference

| Java Type Simple Name  | Formatter Class                    | Order | Documentation                               |
|------------------------|------------------------------------|-------|---------------------------------------------|
| `AtomicBoolean`        | `AtomicBooleanFormatter`           | 80    | [Boolean](parameter/typed/boolean.md)       |
| `AtomicIntegerArray`   | `ArrayFormatter`                   | 80    | [List](parameter/typed/iterable.md)         |
| `AtomicLongArray`      | `ArrayFormatter`                   | 80    | [List](parameter/typed/iterable.md)         |
| `AtomicReferenceArray` | `ArrayFormatter`                   | 80    | [List](parameter/typed/iterable.md)         |
| `BitSet`               | `BitSetFormatter`                  | 80    | [BitSet](parameter/typed/bit-set.md)        |
| `boolean[]`            | `ArrayFormatter`                   | 100   | [List](parameter/typed/iterable.md)         |
| `Boolean`              | `BoolFormatter`                    | 80    | [Boolean](parameter/typed/boolean.md)       |
| `boolean`              | `BoolFormatter`                    | 100   | [Boolean](parameter/typed/boolean.md)       |
| `BooleanSupplier`      | `BooleanSupplierFormatter`         | 80    | [Supplier](parameter/typed/supplier.md)     |
| `byte[]`               | `ByteArrayFormatter`               | 90    | [Byte Array](parameter/typed/byte-array.md) |
| `byte[]`               | `ArrayFormatter`                   | 100   | [List](parameter/typed/iterable.md)         |
| `byte`                 | `NumberFormatter`                  | 100   | [Number](parameter/typed/number.md)         |
| `Calendar`             | `ToTemporalDelegate`               | 80    | [Temporal](parameter/typed/temporal.md)     |
| `char[]`               | `StringFormatter`[^1]              | 100   | [String](parameter/typed/string.md)         |
| `CharSequence`         | `StringFormatter`[^1]              | 80    | [String](parameter/typed/string.md)         |
| `Date`                 | `ToTemporalDelegate`               | 80    | [Temporal](parameter/typed/temporal.md)     |
| `Dictionary`           | `DictionaryFormatter`              | 70    | [Dictionary](parameter/typed/dictionary.md) |
| `double[]`             | `ArrayFormatter`                   | 100   | [List](parameter/typed/iterable.md)         |
| `double`               | `NumberFormatter`                  | 100   | [Number](parameter/typed/number.md)         |
| `DoubleSupplier`       | `DoubleSupplierFormatter`          | 80    | [Supplier](parameter/typed/supplier.md)     |
| `Enum`                 | `EnumFormatter`                    | 80    | [Enum](parameter/typed/enum.md)             |
| `FileTime`             | `ToTemporalDelegate`               | 80    | [Temporal](parameter/typed/temporal.md)     |
| `float[]`              | `ArrayFormatter`                   | 100   | [List](parameter/typed/iterable.md)         |
| `float`                | `NumberFormatter`                  | 100   | [Number](parameter/typed/number.md)         |
| `InstantSource`        | `ToTemporalDelegate`               | 80    | [Temporal](parameter/typed/temporal.md)     |
| `int[]`                | `ArrayFormatter`                   | 100   | [List](parameter/typed/iterable.md)         |
| `int`                  | `NumberFormatter`                  | 100   | [Number](parameter/typed/number.md)         |
| `IntSupplier`          | `IntSupplierFormatter`             | 80    | [Supplier](parameter/typed/supplier.md)     |
| `Iterable`             | `IterableFormatter`                | 80    | [List](parameter/typed/iterable.md)         |
| `Locale`               | `LocaleFormatter`                  | 80    | [Locale](parameter/typed/locale.md)         |
| `long[]`               | `ArrayFormatter`                   | 100   | [List](parameter/typed/iterable.md)         |
| `long`                 | `NumberFormatter`                  | 100   | [Number](parameter/typed/number.md)         |
| `LongSupplier`         | `LongSupplierFormatter`            | 80    | [Supplier](parameter/typed/supplier.md)     |
| `Map.Entry`            | `MapEntryFormatter`                | 80    | [Map](parameter/typed/map.md)               |
| `Map`                  | `MapFormatter`                     | 75    | [Map](parameter/typed/map.md)               |
| `Number`               | `NumberFormatter`                  | 80    | [Number](parameter/typed/number.md)         |
| `Object[]`             | `ArrayFormatter`                   | 100   | [List](parameter/typed/iterable.md)         |
| `Object`               | _registered default formatter_[^1] | 127   |                                             |
| `Optional`             | `OptionalFormatter`                | 80    | [Optional](parameter/typed/optional.md)     |
| `OptionalDouble`       | `OptionalDoubleFormatter`          | 80    | [Optional](parameter/typed/optional.md)     |
| `OptionalInt`          | `OptionalIntFormatter`             | 80    | [Optional](parameter/typed/optional.md)     |
| `OptionalLong`         | `OptionalLongFormatter`            | 80    | [Optional](parameter/typed/optional.md)     |
| `Path`                 | `PathFormatter`                    | 80    | [Path](parameter/typed/path.md)             |
| `Reference`            | `ReferenceFormatter`               | 80    | [Reference](parameter/typed/reference.md)   |
| `short[]`              | `ArrayFormatter`                   | 100   | [List](parameter/typed/iterable.md)         |
| `short`                | `NumberFormatter`                  | 100   | [Number](parameter/typed/number.md)         |
| `Supplier`             | `SupplierFormatter`                | 80    | [Supplier](parameter/typed/supplier.md)     |
| `Temporal`             | `TemporalFormatter`                | 80    | [Temporal](parameter/typed/temporal.md)     |
| `Throwable`            | `ThrowableFormatter`               | 80    | [Throwable](parameter/typed/throwable.md)   |
| `TimeZone`             | `TimeZoneFormatter`                | 80    | [TimeZone](parameter/typed/time-zone.md)    |
| `Type`                 | `TypeFormatter`                    | 80    | [Type](parameter/typed/type.md)             |
| `URI`                  | `URIFormatter`                     | 80    | [URI](parameter/typed/uri.md)               |
| `URL`                  | `URLFormatter`                     | 80    | [URL](parameter/typed/url.md)               |

[^1]: This formatter implements the `DefaultFormatter` interface and can be used to format any object which is not
handled by any other registered formatter.
