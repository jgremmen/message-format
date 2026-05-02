# TimeZone

This formatter is included in the `DefaultFormatterService`.

The `TimeZoneFormatter` handles `java.util.TimeZone` values. It is automatically selected whenever a parameter
value is a `TimeZone` instance. The formatter renders the time zone using its locale-specific display name
obtained from `TimeZone.getDisplayName(Locale)`.

Because the display name is delegated to the string formatter, all string map keys and behavior that apply to
string values work transparently.

```java
messageSupport
    .message("%{tz}")
    .with("tz", TimeZone.getTimeZone("Europe/Berlin"))
    .locale("en-US")
    .format();
// "Central European Standard Time"
```

```java
messageSupport
    .message("%{tz}")
    .with("tz", TimeZone.getTimeZone("America/New_York"))
    .locale("en-US")
    .format();
// "Eastern Standard Time"
```

The output is locale-aware. The same time zone produces different display names depending on the formatting
locale.

```java
messageSupport
    .message("%{tz}")
    .with("tz", TimeZone.getTimeZone("Europe/Berlin"))
    .locale("de-DE")
    .format();
// "Mitteleuropäische Normalzeit"
```


## Map Keys

Because the formatter delegates to the string formatter, string map keys match against the display name text.

```java
messageSupport
    .message("%{tz,'Central European Standard Time':'CET','Eastern Standard Time':'EST'}")
    .with("tz", TimeZone.getTimeZone("Europe/Berlin"))
    .locale("en-US")
    .format();
// "CET"
```


## Null Handling

A `null` parameter value produces an empty string by default. You can provide a `null` map key to handle this
case explicitly.

```java
messageSupport
    .message("%{tz,null:'no timezone'}")
    .with("tz", (TimeZone) null)
    .format();
// "no timezone"
```
