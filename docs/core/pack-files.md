# Export and Import (Pack Files)

Messages and templates can be serialized to a compact binary format called a message format
pack file (`.mfp`). This allows you to prepare all messages at build time and load them very
quickly at runtime, avoiding the cost of parsing message format strings on every application
start.


## Exporting Messages

The `exportMessages` method on `MessageSupport` writes all published messages and the
templates they reference to an `OutputStream`.

```java
try (var out = new FileOutputStream("messages.mfp")) {
    messageSupport.exportMessages(out);
}
```

The no-argument convenience overload enables compression and exports all messages. The full
signature gives you more control:

```java
void exportMessages(OutputStream stream, boolean compress,
                    Predicate<String> messageCodeFilter) throws IOException
```

- **compress** – when `true`, the output is wrapped in GZip compression. The binary format
  already uses extensive bit-packing, so compression may not reduce the size significantly
  for small message sets. For larger sets, enabling compression typically reduces the file
  size noticeably.
- **messageCodeFilter** – an optional predicate that selects which message codes to include.
  Pass `null` to export all messages.

Only the templates that are actually referenced by the exported messages are included in the
pack file. Templates that exist in the message support but are not used by any selected
message are omitted.


## Importing Messages

The `importMessages` method on `ConfigurableMessageSupport` reads a pack file from an
`InputStream` and adds all messages and templates it contains to the message support instance.

```java
try (var in = new FileInputStream("messages.mfp")) {
    messageSupport.importMessages(in);
}
```

The stream is closed automatically when the method returns, regardless of whether the import
succeeded. Each message and template found in the pack file is added to the message support
through the same mechanism as `addMessage` and `addTemplate`, so message filters and template
filters apply as usual.

If you need lower-level control over the imported entries, you can use the static utility
method `MessageUtil.importMessages` instead. It accepts a `Consumer<Message.WithCode>` for
messages and a `BiConsumer<String, Message.WithSpaces>` for templates, letting you inspect or
transform each entry before adding it.

```java
MessageUtil.importMessages(inputStream,
    message -> System.out.println("Message: " + message.getCode()),
    (name, template) -> System.out.println("Template: " + name));
```


## Pack File Format

A pack file uses the magic bytes `%{msg}` and a version number to identify itself. The MIME
type is `application/x-message-format-pack`. The `MessageUtil.isMessageFormatPack(Path)`
utility method can be used to check whether a given file is a valid pack file.

The binary format uses bit-packing techniques that produce a very compact representation of
messages and templates, including localized message variants, parameter configurations, map
keys and template references.

The pack format is designed to be backward-compatible, so pack files created with an older
version of the library can generally be read by newer versions. However, pack files created
with a newer version cannot be read by older versions. When in doubt, re-export your pack
files after upgrading.
