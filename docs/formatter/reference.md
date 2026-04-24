# Reference

TODO


## Java Type Formatters

| Java Type Simple Name  | Formatter Class                    | Order | Documentation                         |
|------------------------|------------------------------------|-------|---------------------------------------|
| `AtomicBoolean`        | `AtomicBooleanFormatter`           | 80    | [Boolean](parameter/boolean.md)       |
| `AtomicIntegerArray`   | `ArrayFormatter`                   | 80    | [List](parameter/iterable.md)         |
| `AtomicLongArray`      | `ArrayFormatter`                   | 80    | [List](parameter/iterable.md)         |
| `AtomicReferenceArray` | `ArrayFormatter`                   | 80    | [List](parameter/iterable.md)         |
| `boolean[]`            | `ArrayFormatter`                   | 100   | [List](parameter/iterable.md)         |
| `Boolean`              | `BoolFormatter`                    | 80    | [Boolean](parameter/boolean.md)       |
| `boolean`              | `BoolFormatter`                    | 100   | [Boolean](parameter/boolean.md)       |
| `BooleanSupplier`      | `BooleanSupplierFormatter`         | 80    | [Supplier](parameter/supplier.md)     |
| `byte[]`               | `ByteArrayFormatter`               | 90    | [Byte Array](parameter/byte-array.md) |
| `byte[]`               | `ArrayFormatter`                   | 100   | [List](parameter/iterable.md)         |
| `byte`                 | `NumberFormatter`                  | 100   | [Number](parameter/number.md)         |
| `Calendar`             | `ToTemporalDelegate`               | 80    | [Temporal](parameter/temporal.md)     |
| `char[]`               | `StringFormatter`[^1]              | 100   | [String](parameter/string.md)         |
| `CharSequence`         | `StringFormatter`[^1]              | 80    | [String](parameter/string.md)         |
| `Charset`              | `CharsetFormatter`                 | 80    | [Charset](parameter/charset.md)       |
| `Date`                 | `ToTemporalDelegate`               | 80    | [Temporal](parameter/temporal.md)     |
| `Dictionary`           | `DictionaryFormatter`              | 70    | [Dictionary](parameter/dictionary.md) |
| `double[]`             | `ArrayFormatter`                   | 100   | [List](parameter/iterable.md)         |
| `double`               | `NumberFormatter`                  | 100   | [Number](parameter/number.md)         |
| `DoubleSupplier`       | `DoubleSupplierFormatter`          | 80    | [Supplier](parameter/supplier.md)     |
| `Enum`                 | `EnumFormatter`                    | 80    | [Enum](parameter/enum.md)             |
| `FileTime`             | `ToTemporalDelegate`               | 80    | [Temporal](parameter/temporal.md)     |
| `float[]`              | `ArrayFormatter`                   | 100   | [List](parameter/iterable.md)         |
| `float`                | `NumberFormatter`                  | 100   | [Number](parameter/number.md)         |
| `InstantSource`        | `ToTemporalDelegate`               | 80    | [Temporal](parameter/temporal.md)     |
| `int[]`                | `ArrayFormatter`                   | 100   | [List](parameter/iterable.md)         |
| `int`                  | `NumberFormatter`                  | 100   | [Number](parameter/number.md)         |
| `IntSupplier`          | `IntSupplierFormatter`             | 80    | [Supplier](parameter/supplier.md)     |
| `Iterable`             | `IterableFormatter`                | 80    | [List](parameter/iterable.md)         |
| `Locale`               | `LocaleFormatter`                  | 80    | [Locale](parameter/locale.md)         |
| `long[]`               | `ArrayFormatter`                   | 100   | [List](parameter/iterable.md)         |
| `long`                 | `NumberFormatter`                  | 100   | [Number](parameter/number.md)         |
| `LongSupplier`         | `LongSupplierFormatter`            | 80    | [Supplier](parameter/supplier.md)     |
| `Map.Entry`            | `MapEntryFormatter`                | 80    | [Map](parameter/map.md)               |
| `Map`                  | `MapFormatter`                     | 75    | [Map](parameter/map.md)               |
| `Number`               | `NumberFormatter`                  | 80    | [Number](parameter/number.md)         |
| `Object[]`             | `ArrayFormatter`                   | 100   | [List](parameter/iterable.md)         |
| `Object`               | _registered default formatter_[^1] | 127   |                                       |
| `Optional`             | `OptionalFormatter`                | 80    | [Optional](parameter/optional.md)     |
| `OptionalDouble`       | `OptionalDoubleFormatter`          | 80    | [Optional](parameter/optional.md)     |
| `OptionalInt`          | `OptionalIntFormatter`             | 80    | [Optional](parameter/optional.md)     |
| `OptionalLong`         | `OptionalLongFormatter`            | 80    | [Optional](parameter/optional.md)     |
| `Path`                 | `PathFormatter`                    | 80    | [Path](parameter/path.md)             |
| `Reference`            | `ReferenceFormatter`               | 80    | [Reference](parameter/reference.md)   |
| `short[]`              | `ArrayFormatter`                   | 100   | [List](parameter/iterable.md)         |
| `short`                | `NumberFormatter`                  | 100   | [Number](parameter/number.md)         |
| `Supplier`             | `SupplierFormatter`                | 80    | [Supplier](parameter/supplier.md)     |
| `Temporal`             | `TemporalFormatter`                | 80    | [Temporal](parameter/temporal.md)     |
| `Throwable`            | `ThrowableFormatter`               | 80    | [Throwable](parameter/throwable.md)   |
| `TimeZone`             | `TimeZoneFormatter`                | 80    | [TimeZone](parameter/time-zone.md)    |
| `Type`                 | `TypeFormatter`                    | 80    | [Type](parameter/type.md)             |
| `URI`                  | `URIFormatter`                     | 80    | [URI](parameter/uri.md)               |
| `URL`                  | `URLFormatter`                     | 80    | [URL](parameter/url.md)               |

[^1]: This formatter implements the `DefaultFormatter` interface and can be used to format any object which is not 
      handled by any other registered formatter.


## Named Formatters

| Format Name  | Formatter Class       | Documentation                               |
|--------------|-----------------------|---------------------------------------------|
| `bool`       | `BoolFormatter`       | [Bool](named-parameter/bool.md)             |
| `choice`     | `ChoiceFormatter`     | [Choice](named-parameter/choice.md)         |
| `classifier` | `ClassifierFormatter` | [Classifier](named-parameter/classifier.md) |
| `geo`        | `GeoFormatter`        | [Geo](named-parameter/geo.md)               |
| `size`       | `SizeFormatter`       | [Size](named-parameter/size.md)             |
| `string`     | `StringFormatter`     | [String](named-parameter/string.md)         |
