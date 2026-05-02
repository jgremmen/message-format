# Temporal

This formatter is included in the `DefaultFormatterService`.

The library provides two formatters for date and time values: `TemporalFormatter` for the modern `java.time`
temporal types, and `ToTemporalDelegate` which bridges legacy date/time types (`Date`, `Calendar`, etc.) to the
temporal formatter.


## TemporalFormatter

The `TemporalFormatter` handles any `java.time.temporal.Temporal` value, which includes `LocalDate`, `LocalTime`,
`LocalDateTime`, `ZonedDateTime`, `OffsetDateTime`, `Instant`, `Year`, `YearMonth` and all other temporal types.
It is automatically selected whenever a parameter value implements `Temporal`.

The formatter automatically detects whether the temporal value supports date fields, time fields, or both, and
adjusts the output accordingly. All formatting is locale-aware.

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

| Style      | Description                          |
|------------|--------------------------------------|
| `short`    | Short date and/or time               |
| `medium`   | Medium date and/or time (default)    |
| `long`     | Long date and/or time                |
| `full`     | Full date and/or time                |
| `date`     | Date only (medium style)             |
| `time`     | Time only (medium style)             |

The formatter automatically omits the date or time portion if the temporal value does not support the necessary
fields. For example, formatting a `LocalDate` with `date:short` produces a short date (no time), while formatting
a `LocalTime` with `date:short` produces a short time (no date).

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
```

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

When a temporal does not support the requested portion, the formatter produces an empty string. For example,
requesting `date:time` on a `LocalDate` or `date:date` on a `LocalTime` returns nothing.

#### Custom Patterns

Any string that is not a predefined style name is treated as a `DateTimeFormatter` pattern. This gives you full
control over the output format.

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

### Null Handling

A `null` parameter value produces an empty string by default. You can provide a `null` map key to handle this
case explicitly.

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
Date legacyDate = new Date(124, Calendar.NOVEMBER, 9);  // 2024-11-09

messageSupport
    .message("%{d,date:medium}")
    .with("d", legacyDate)
    .locale("de-DE")
    .format();
// "09.11.2024, 00:00:00"
```

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

A `null` parameter value produces an empty string by default. You can provide a `null` map key to handle this
case explicitly.

```java
messageSupport
    .message("%{date,null:'unknown'}")
    .with("date", (Date) null)
    .format();
// "unknown"
```
