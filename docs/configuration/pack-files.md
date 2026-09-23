# Export and Import (Pack Files)

Messages and templates can be serialized to a compact binary format called a message format pack file (`.mfp`). This 
allows all messages to be prepared at build time and loaded very quickly at runtime, avoiding the cost of parsing
message format strings on every application start.


## Exporting Messages

The `exportMessages` method on `MessageSupport` writes all published messages and the templates they reference to an 
`OutputStream`.

```java
try(var out = new FileOutputStream("messages.mfp")) {
  messageSupport.exportMessages(out);
}
```

The no-argument convenience overload enables compression and exports all messages. The full signature gives more 
control:

```java
void exportMessages(OutputStream stream, boolean compress,
                    Predicate<String> messageCodeFilter,
                    Predicate<String> templateNameFilter) 
    throws IOException
```

- **compress** – when `true`, the output is wrapped in GZip compression. The binary format already uses extensive 
  bit-packing, so compression may not reduce the size significantly for small message sets. For larger sets, enabling 
  compression typically reduces the file size noticeably.
- **messageCodeFilter** – an optional predicate that selects which message codes to include.
  Pass `null` to export all messages.
- **templateNameFilter** – an optional predicate that selects which template names to include.
  Pass `null` to include all templates referenced by the selected messages. Only templates
  that were created from parsed message format strings (i.e. `MessageTemplate` instances) can
  be serialized; custom `AbstractNamedTemplate` implementations are excluded automatically since
  they have no serializable message representation.

Only the templates that are actually referenced by the exported messages are included in the pack file. Templates that
exist in the message support but are not used by any selected message are omitted.


## Importing Messages

The `importMessages` method on `ConfigurableMessageSupport` reads a pack file from an `InputStream` and adds all 
messages and templates it contains to the message support instance.

```java
try(var in = new FileInputStream("messages.mfp")) {
  messageSupport.importMessages(in);
}
```

The stream is closed automatically when the method returns, regardless of whether the import succeeded. Each message
and template found in the pack file is added to the message support through the same mechanism as `addMessage` and
`addTemplate`, so message filters and template filters apply as usual.

For lower-level control over the imported entries, the static utility method `MessageUtil.importMessages` can be used 
instead. It accepts a `Consumer<Message.WithCode>` for messages and a `BiConsumer<String,Template>` for templates,
allowing each entry to be inspected or transformed before adding it.

```java
MessageUtil.importMessages(inputStream,
    message -> System.out.println("Message: " + message.getCode()),
    (name, template) -> System.out.println("Template: " + name));
```


## Pack File Format

A pack file uses the magic bytes `%{msg}` and a version number to identify itself. The MIME type is 
`application/x-message-format-pack`. The `MessageUtil.isMessageFormatPack(Path)` utility method can be used to check 
whether a given file is a valid pack file.

The binary format uses bit-packing techniques that produce a very compact representation of messages and templates, 
including localized message variants, parameter configurations, map keys and template references.

The pack format is designed to be backward-compatible, so pack files created with an older version of the library can 
generally be read by newer versions. However, pack files created with a newer version cannot be read by older versions.
When in doubt, re-export the pack files after upgrading.


## Detecting Pack Files with Apache Tika

Content inspection frameworks such as [Apache Tika](https://tika.apache.org/) can recognize a pack file by its magic 
bytes and report the `application/x-message-format-pack` MIME type. The library ships two detector classes in the 
`de.sayayi.lib.message.pack` package that plug into Tika's detection pipeline. Which class applies depends on the Tika 
version on the classpath. `PackTika3Detector` covers Apache Tika in the range `[1.19,4.0)`, and `PackTika4Detector` 
covers Apache Tika in the range `[4.0,5.0)`. A third class, `PackTikaDetector`, remains as a deprecated alias for 
`PackTika3Detector` and exists only for source compatibility with earlier releases.

Prior to version 0.25.0 the Tika 3 detector was registered automatically. That automatic registration has been removed.
Tika discovers detectors through the `ServiceLoader` mechanism, so the appropriate detector must now be registered 
explicitly. This is done by adding the fully qualified class name to a service file named 
`META-INF/services/org.apache.tika.detect.Detector` on the application classpath.

For Apache Tika 3, the service file contains a single line:

```
de.sayayi.lib.message.pack.PackTika3Detector
```

For Apache Tika 4, the same file references the Tika 4 detector instead:

```
de.sayayi.lib.message.pack.PackTika4Detector
```

Once the service file is present, detection works through the standard Tika API without any further configuration.

```java
Tika tika = new Tika();

// detects "application/x-message-format-pack" for a pack file
String mimeType = tika.detect(new File("messages.mfp"));
```

Applications that only need a direct check, without a full Tika integration, can use the 
`MessageUtil.isMessageFormatPack(Path)` utility method described in the previous section instead.


## Generating Pack Files at Build Time

If a project uses `@MessageDef` and `@TemplateDef` annotations to declare messages and templates in source code, the
[Gradle Plugin](../gradle-plugin/index.md) can scan the compiled classes and produce a `.mfp` pack file automatically 
as part of the build. This removes the need to export pack files manually and ensures that the pack file is always in 
sync with the annotated message definitions in the codebase.
