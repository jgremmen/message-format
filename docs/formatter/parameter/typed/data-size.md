# DataSize

/// note
This formatter is **not** included in the core library. It is part of the `message-format-spring` module, whose
coordinates are listed on the [Spring](../../../add-on/spring/index.md#dependency) page.
///

Spring represents amounts of data, such as configured upload limits or buffer sizes, with
`org.springframework.util.unit.DataSize`. Its `toString()` result is the number of bytes followed by the letter `B`,
for example `2048B`, which is a technical notation rather than text for a message.

The `DataSizeFormatter` is a type-based formatter registered for `DataSize`. It converts the data size to its number
of bytes and delegates formatting to the named formatter `filesize`, passing on the parameter configuration unchanged.
If no `filesize` formatter is registered, the number of bytes is formatted according to its type, as a `long`, by the
[Number](number.md) formatter. The module declares the formatter as a service provider, so the
`DefaultFormatterService` registers it automatically as soon as the module is on the class path or the module path.

The examples on this page use the following message support.

```java title="Example setup"
import de.sayayi.lib.message.MessageSupportFactory;
import org.springframework.util.unit.DataSize;

import java.util.Locale;

var messageSupport = MessageSupportFactory.createDefault();
// all built-in formatters and the formatters for Spring types
```


## Number of Bytes

The `filesize` formatter is not registered by the `DefaultFormatterService`. With the default formatters, a data size
is therefore formatted as its number of bytes:

```java
messageSupport
    .message("Maximum upload size: %{max} bytes")
    .with("max", DataSize.ofMegabytes(5))
    .locale(Locale.US)
    .format();
// "Maximum upload size: 5242880 bytes"
```

Because the number formatter handles the number of bytes, its configuration keys and map keys apply. A number key, for
example, selects a text for a particular size:

```java
messageSupport
    .message("Upload limit: %{max,0:'none'}")
    .with("max", DataSize.ofBytes(0))
    .format();
// "Upload limit: none"
```


## Units with the `filesize` Formatter

/// warning | Deprecated: `filesize`
`FileSizeFormatter` (`format:filesize`) is deprecated and marked for removal, without a direct replacement (see
[Named Formatters](../named/index.md#reference)). The output described in this section is only available while the
formatter exists and is registered.
///

A `FileSizeFormatter` is added to the formatter service following the steps described under
[Formatters Not Registered by Default](../../../configuration/formatter-service.md#formatters-not-registered-by-default)
on the formatter service page. With this formatter, a data size is formatted with a unit, and the configuration keys of
that formatter, such as `scale` and `space`, apply:

```java
import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.DefaultFormatterService;
import de.sayayi.lib.message.formatter.parameter.named.extra.FileSizeFormatter;
import org.springframework.util.unit.DataSize;

import java.util.Locale;

var formatterService = new DefaultFormatterService();
formatterService.addFormatter(new FileSizeFormatter());

var messageSupport = MessageSupportFactory.create(formatterService);

messageSupport
    .message("%{max} / %{max,scale:0,space:true}")
    .with("max", DataSize.ofMegabytes(5))
    .locale(Locale.US)
    .format();
// "5.2MB / 5 MB"
```

The `filesize` formatter selects units based on powers of 1000, whereas the units of `DataSize` are based on powers of
1024. `DataSize.ofMegabytes(5)` holds 5,242,880 bytes and is therefore formatted as `5.2MB`, and
`DataSize.ofKilobytes(512)` as `524.3KB`.


## Map Keys

The `null` key and the handling of `null` values follow the
[general rules](../index.md#null-values-and-empty-results) for parameter formatters.
