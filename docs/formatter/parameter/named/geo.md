# Geo

/// note
This formatter is **not** included in the `DefaultFormatterService`. You must register it explicitly
by adding a `GeoFormatter` instance to your formatter service.
///

The named formatter `geo` is selected explicitly by writing `format:geo` in the message parameter configuration.
It can also be activated automatically by providing the `geo` configuration key directly (e.g. `geo:latitude`).
The formatter converts numeric values representing decimal degrees into geographic coordinates in degrees, minutes
and seconds (DMS) notation.

It accepts all `Number` types (including primitive `double` and `float`) and formats them using either a predefined
format name or a custom format pattern. Negative values represent west (longitude) or south (latitude) coordinates.


## Predefined Formats

The `geo` configuration key accepts the following predefined format names.

### Longitude Formats

| Format name          | Example output     | Description                                      |
|----------------------|--------------------|--------------------------------------------------|
| `short-longitude`    | `12°45'E`          | Degrees and whole minutes, compass direction      |
| `longitude`          | `12°45'3"E`        | Degrees, minutes and whole seconds                |
| `medium-longitude`   | `12°45'2.9"E`      | Degrees, minutes and seconds with 1 decimal place |
| `long-longitude`     | `12°45'2.581"E`    | Degrees, minutes and seconds with 3 decimal places|

### Latitude Formats

| Format name          | Example output     | Description                                      |
|----------------------|--------------------|--------------------------------------------------|
| `short-latitude`     | `51°34'N`          | Degrees and whole minutes, compass direction      |
| `latitude`           | `51°34'9"N`        | Degrees, minutes and whole seconds                |
| `medium-latitude`    | `51°34'9.0"N`      | Degrees, minutes and seconds with 1 decimal place |
| `long-latitude`      | `51°34'9.000"N`    | Degrees, minutes and seconds with 3 decimal places|

```java
messageSupport
    .message("coordinates %{lon,geo:longitude}, %{lat,geo:latitude}")
    .with("lon", 4.8)
    .with("lat", 51.569167)
    .format();
// "coordinates 4°48'0"E, 51°34'9"N"
```

```java
messageSupport
    .message("%{lon,geo:short-longitude}")
    .with("lon", 4.8)
    .format();
// "4°48'E"
```

Negative values produce the opposite compass direction (W instead of E, S instead of N).

```java
messageSupport
    .message("%{lon,geo:long-longitude}")
    .with("lon", -18.999697)
    .format();
// "18°59'59.891"W"
```


## Custom Format Patterns

When none of the predefined formats fit, you can provide a custom format pattern as the `geo` configuration
value. The pattern syntax is:

```
d[ ][0](m|M|MM|MMM)[ ][0](s|S|SS|SSS)[ ](LO|LA)
```

Each element of the pattern controls a specific aspect of the output:

| Element           | Meaning                                                              |
|-------------------|----------------------------------------------------------------------|
| `d`               | Degrees (always present)                                             |
| ` ` (space)       | Insert a space after the preceding element                           |
| `0`               | Zero-pad the following minutes or seconds (e.g. `05` instead of `5`) |
| `m`               | Minutes with no decimal places                                       |
| `M`               | Minutes with 1 decimal place                                         |
| `MM`              | Minutes with 2 decimal places                                        |
| `MMM`             | Minutes with 3 decimal places                                        |
| `s`               | Seconds with no decimal places                                       |
| `S`               | Seconds with 1 decimal place                                         |
| `SS`              | Seconds with 2 decimal places                                        |
| `SSS`             | Seconds with 3 decimal places                                        |
| `LO`              | Append compass direction for longitude (E/W)                         |
| `LA`              | Append compass direction for latitude (N/S)                          |

When `LO` or `LA` is omitted, negative values are shown with a minus sign. When a compass direction is appended,
the sign is expressed as the direction letter instead.

```java
messageSupport
    .message("%{lat,geo:'d0m0sLA'}")
    .with("lat", 51.569167)
    .format();
// "51°34'09"N"
```

The `0` before `m` and `s` pads minutes and seconds to two digits. This ensures consistent column alignment.

```java
messageSupport
    .message("%{lon,geo:'dM LO'}")
    .with("lon", 4.8)
    .format();
// "4°48.0' E"
```

Here, `M` produces minutes with one decimal place and the space after `M` inserts a space after the minute symbol.
The space before `LO` inserts a space before the compass direction.

A pattern with only degrees and a compass direction:

```java
messageSupport
    .message("%{lat,geo:'d LA'}")
    .with("lat", -33.8688)
    .format();
// "34° S"
```

Without a compass direction, negative values produce a minus sign:

```java
messageSupport
    .message("%{v,geo:'d0m0s'}")
    .with("v", -18.999697)
    .format();
// "-18°59'59""
```


## Compass Direction Labels

The compass direction letters default to `N`, `S`, `E` and `W`. These can be customized using the configuration
keys `geo-n`, `geo-s`, `geo-e` and `geo-w`.

```java
messageSupport
    .message("%{lat,geo:latitude,geo-n:Nord,geo-s:Süd}")
    .with("lat", 51.569167)
    .format();
// "51°34'9"Nord"
```


## Null Handling

A `null` parameter value produces an empty string by default. You can provide a `null` map key to produce
specific text.

```java
messageSupport
    .message("%{lat,format:geo,geo:latitude}")
    .with("lat", null)
    .format();
// ""
```
