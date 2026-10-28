# Temporal

This formatter is included in the `DefaultFormatterService`.

The library provides two formatters for date and time values: `TemporalFormatter` for the modern `java.time`
temporal types and `ToTemporalDelegate` which bridges legacy date/time types (`Date`, `Calendar`, etc.) to the
temporal formatter.


## TemporalFormatter

The `TemporalFormatter` handles any `java.time.temporal.Temporal` value, which includes `LocalDate`, `LocalTime`,
`LocalDateTime`, `ZonedDateTime`, `OffsetDateTime`, `Instant`, `Year`, `YearMonth` and all other temporal types. It is
automatically selected whenever a parameter value implements `Temporal`.

The formatter automatically detects whether the temporal value supports date fields, time fields, or both and adjusts
the output accordingly. All formatting is locale-aware.

```java
messageSupport
    .message("%{date}")
    .with("date", LocalDate.of(1972, 8, 17))
    .locale("de-DE")
    .format();
// "17.08.1972"
```

```java
messageSupport
    .message("%{time}")
    .with("time", LocalTime.of(16, 34, 11))
    .locale("de-DE")
    .format();
// "16:34:11"
```

```java
messageSupport
    .message("%{dt}")
    .with("dt", LocalDateTime.of(2019, 2, 19, 14, 23, 1))
    .locale("en-GB")
    .format();
// "19 Feb 2019, 14:23:01"
```

### The `date` Configuration Key

The `date` configuration key controls the output format. It accepts predefined style names or a custom
`DateTimeFormatter` pattern.

#### Predefined Styles

| Style    | Description                       |
|----------|-----------------------------------|
| `short`  | Short date and/or time            |
| `medium` | Medium date and/or time (default) |
| `long`   | Long date and/or time             |
| `full`   | Full date and/or time             |
| `date`   | Date only (medium style)          |
| `time`   | Time only (medium style)          |

The formatter automatically omits the date or time portion if the temporal value does not support the necessary fields.
For example, formatting a `LocalDate` with `date:short` produces a short date (no time), while formatting a `LocalTime`
with `date:short` produces a short time (no date).

```java
LocalDate date = LocalDate.of(1972, 8, 17);

messageSupport
    .message("%{d,date:short}")
    .with("d", date)
    .locale("de-DE")
    .format();
// "17.08.72"

messageSupport
    .message("%{d,date:long}")
    .with("d", date)
    .locale("de-DE")
    .format();
// "17. August 1972"

messageSupport
    .message("%{d,date:full}")
    .with("d", date)
    .locale("de-DE")
    .format();
// "Donnerstag, 17. August 1972"
```

```java
LocalTime time = LocalTime.of(16, 34, 11);

messageSupport
    .message("%{t,date:short}")
    .with("t", time)
    .locale("de-DE")
    .format();
// "16:34"

messageSupport
    .message("%{t,date:medium}")
    .with("t", time)
    .locale("de-DE")
    .format();
// "16:34:11"
```

```java
LocalDateTime dt = LocalDateTime.of(1972, 8, 17, 2, 40, 23);

messageSupport
    .message("%{dt,date:short}")
    .with("dt", dt)
    .locale("en-GB")
    .format();
// "17/08/1972, 02:40"

messageSupport
    .message("%{dt,date:full}")
    .with("dt", dt)
    .locale("en-GB")
    .format();
// "Thursday, 17 August 1972, 02:40:23 Central European Standard Time"
// (system time zone Europe/Berlin)
```

The `long` and `full` styles include a time zone name. For a value without a zone, such as a `LocalDateTime`, the
system default time zone is used, as described in [Time Zones](#time-zones).

The `date` and `time` styles extract only one portion:

```java
LocalDateTime dt = LocalDateTime.of(2019, 2, 19, 14, 23, 1);

messageSupport
    .message("%{dt,date:date}")
    .with("dt", dt)
    .locale("en-GB")
    .format();
// "19 Feb 2019"

messageSupport
    .message("%{dt,date:time}")
    .with("dt", dt)
    .locale("en-GB")
    .format();
// "14:23:01"
```

When a temporal does not support the requested portion, the formatter produces an empty string. For example, requesting
`date:time` on a `LocalDate` or `date:date` on a `LocalTime` returns nothing.

#### Custom Patterns

Any string that is not a predefined style name is treated as a `DateTimeFormatter` pattern. This gives full control
over the output format.

```java
messageSupport
    .message("%{dt,date:'yyyy-MM-dd'}")
    .with("dt", LocalDate.of(2019, 2, 19))
    .format();
// "2019-02-19"
```

```java
messageSupport
    .message("%{dt,date:'HH:mm:ss,SSS'}")
    .with("dt", LocalDateTime.of(1972, 8, 17, 2, 40, 23, 833000000))
    .locale("fr-FR")
    .format();
// "02:40:23,833"
```

```java
messageSupport
    .message("%{dt,date:'dd MMMM'}")
    .with("dt", LocalDateTime.of(1972, 8, 17, 2, 40, 23))
    .locale("fr-FR")
    .format();
// "17 août"
```

An invalid pattern (for example one containing an unknown pattern letter) produces an empty string.

### Missing Fields

Formatting never throws an exception. If the temporal value does not support all fields required by the selected style
or pattern, the formatter produces an empty string. This applies to partial temporals such as `Year` and `YearMonth`: 
the localized date styles require a day of month, so `%{ym}` or `%{ym,date:short}` with a `YearMonth` produce nothing.
The same happens when a pattern refers to fields the value lacks, such as `yyyy` for a `LocalTime` or `HH:mm` for a
`LocalDate`.

Use a custom pattern with the fields the value provides to format partial temporals:

```java
messageSupport
    .message("%{ym,date:'MMMM yyyy'}")
    .with("ym", YearMonth.of(2024, 3))
    .locale("de-DE")
    .format();
// "März 2024"

messageSupport
    .message("%{ym}")
    .with("ym", YearMonth.of(2024, 3))
    .format();
// ""
```

### Time Zones

A value that carries a zone or offset, such as a `ZonedDateTime` or an `OffsetDateTime`, is formatted in that zone.
A value without a zone, such as a `LocalDateTime`, and an `Instant` are formatted in the system default time zone
(`ZoneId.systemDefault()`). For an `Instant`, this determines which local date and time is printed, so the output of
the same instant differs between servers with different time zone settings. To format an instant for a particular
zone, convert it before passing it to the message, for example with `instant.atZone(zone)`.

```java
var instant = Instant.parse("2024-03-01T23:30:00Z");

messageSupport
    .message("%{ts,date:short}")
    .with("ts", instant.atZone(ZoneId.of("America/New_York")))
    .locale(Locale.US)
    .format();
// "3/1/24, 6:30 PM"

messageSupport
    .message("%{ts,date:short}")
    .with("ts", instant.atZone(ZoneId.of("Asia/Tokyo")))
    .locale(Locale.US)
    .format();
// "3/2/24, 8:30 AM"
```

/// note | Migration from 0.24
Up to version 0.24.0, every value was formatted in the system default time zone, even a `ZonedDateTime` or an
`OffsetDateTime` with a different zone. Such values are now formatted in their own zone.
///

### Null Handling

A `null` parameter value produces an empty string by default. A `null` map key can be provided to handle this case
explicitly.

```java
messageSupport
    .message("%{date,null:'no date'}")
    .with("date", (LocalDate) null)
    .format();
// "no date"
```


## ToTemporalDelegate

The `ToTemporalDelegate` bridges legacy date/time types to the `TemporalFormatter`. It converts values of the
following types into a `Temporal` representation and then delegates formatting to the temporal formatter:

- `java.util.Date` is converted to `Instant`
- `java.util.Calendar` is converted to `Instant`
- `java.time.InstantSource` is converted to `Instant`
- `java.nio.file.attribute.FileTime` is converted to `Instant`
- `java.sql.Date` is converted to `LocalDate` (no time component)
- `java.sql.Time` is converted to `LocalTime` (no date component)

The `date` configuration key and all predefined styles and custom patterns work the same way as with
`TemporalFormatter`, since it is the temporal formatter that ultimately produces the output.

```java
Date legacyDate = Date.from(LocalDate.of(2024, 11, 9)
    .atStartOfDay(ZoneId.systemDefault())
    .toInstant());

messageSupport
    .message("%{d,date:medium}")
    .with("d", legacyDate)
    .locale("de-DE")
    .format();
// "09.11.2024, 00:00:00"
```

The converted `Instant` has no time zone, so it is formatted in the system default time zone like any other instant.

```java
java.sql.Date sqlDate = java.sql.Date.valueOf("2024-11-09");

messageSupport
    .message("%{d,date:'yyyy-MM-dd'}")
    .with("d", sqlDate)
    .format();
// "2024-11-09"
```

```java
java.sql.Time sqlTime = java.sql.Time.valueOf("23:36:04");

messageSupport
    .message("%{t,date:'HH:mm'}")
    .with("t", sqlTime)
    .format();
// "23:36"
```

### Null Handling

A `null` parameter value produces an empty string by default. A `null` map key can be provided to handle this case
explicitly.

```java
messageSupport
    .message("%{date,null:'unknown'}")
    .with("date", (Date) null)
    .format();
// "unknown"
```
