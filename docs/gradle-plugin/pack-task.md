# Pack Task

The `messageFormatPack` task is the Gradle task registered by the plugin. It performs the actual
work of scanning compiled class files, collecting messages and templates, validating template
references, executing custom actions, and writing the resulting pack file. The task is placed in
the `build` group and is annotated with `@CacheableTask`, so Gradle can cache and skip it when
inputs have not changed.


## Task Inputs and Outputs

The task tracks its inputs and outputs through Gradle's incremental build system. When none of
the inputs change between builds, Gradle skips the task entirely and reuses the cached output.

The input properties are the source file collection, the pack filename, the compression flag,
the duplicate message strategy, the template validation flag, and the include and exclude regex
filter lists. The single output is the generated pack file, which is located at
`build/messageFormatPack/<packFilename>` by default.


## Scanning

The task iterates over all `.class` files in its configured source collection and passes each
one to an `AsmAnnotationAdopter`. The adopter reads `@MessageDef` and `@TemplateDef` annotations
directly from the bytecode without loading the class into the JVM. Annotations are recognized on
the class declaration itself and on every non-synthetic method in the class, in both their
singular form and their repeatable container form (`@MessageDefs`, `@TemplateDefs`).

Each discovered message and template is published to an internal `ConfigurableMessageSupport`
instance. The duplicate message strategy configured on the task determines what happens when two
annotations define the same message code or template name with different content.

You can observe the scanning progress by running Gradle with increased log verbosity. At the
`info` level, the task logs a general scanning start message. At the `debug` level, it logs each
class name as it is scanned. At the `trace` level, the full file path is logged alongside the
class name.


## Include and Exclude Filters

The include and exclude regex filters configured through the [extension](extension.md) are
forwarded to the task as conventions. They can also be set directly on the task if needed. The
filters control which message codes are written to the pack file and which are skipped.

The filtering logic works as follows. If no include filters are defined, every scanned message is
eligible. If at least one include filter is defined, a message is eligible only when its code
matches at least one of the include patterns. After that, if the message code matches any exclude
pattern, it is removed from the output. Filters are standard Java regular expressions matched
against the full message code string.

The filters also apply during template validation. When `validateReferencedTemplates` is enabled,
the task checks for missing templates only among messages that pass the filters. Messages that
are excluded by the filters are not considered.


## Template Validation

When the `validateReferencedTemplates` property is `true` (the default), the task collects all
template names referenced by the filtered messages and checks that each one has a corresponding
`@TemplateDef` in the scanned classes. The check follows nested references as well: if template
A references template B, then template B must also be present.

If one or more templates are missing, the task fails with an error that lists the missing
template names. For a single missing template the error reads
`Missing message template: <name>`, and for multiple missing templates
the error reads `Missing message templates: <name1>, <name2> and <name3>`.

Disabling this check (by setting the property to `false`) can be useful when templates are
loaded from a different source at runtime, for example from a separate pack file
or through programmatic registration. Be aware that a missing template at runtime causes a
formatting error when the message that references it is formatted.


## Duplicate Handling

The duplicate message strategy controls what happens when two `@MessageDef` annotations define
the same message code with different text, or when two `@TemplateDef` annotations define the
same template name with different content. The five available strategies are described in detail
on the [Extension](extension.md) page.

When the `FAIL` strategy is active, the task throws a `DuplicateMessageException` or
`DuplicateTemplateException` and the build stops immediately. The error message includes the
duplicate code or name and the class in which the duplicate was found.

When a warning strategy is active (`IGNORE_AND_WARN` or `OVERRIDE_AND_WARN`), the task logs a
warning at the `WARN` level with the same information. This lets you identify duplicates without
failing the build.

Note that two annotations with the same code or name and identical content are never considered
duplicates. They are silently accepted regardless of the strategy.


## Custom Actions

The task supports registering one or more custom actions that are executed after scanning and
validation, but before the pack file is written. Each action receives a `MessageAccessor` that
provides read-only access to all scanned messages and templates. This is useful for build-time
analysis, reporting, or validation that goes beyond what the built-in checks offer.

Actions are registered using the `action` method on the task. If you configure the task through
the `messageFormatPack` task directly, the action block looks like this:

```groovy
tasks.named('messageFormatPack') {
  action {
    // 'it' is a MessageAccessor
    println "Total messages: ${it.messageCodes.size()}"
    println "Total templates: ${it.templateNames.size()}"
  }
}
```

The `MessageAccessor` exposes methods such as `getMessageCodes()` to retrieve all collected
message codes, `getTemplateNames()` to retrieve all template names, `hasMessageWithCode(String)`
to check for a specific code, and `getMessageByCode(String)` to retrieve a message by its code.

A more elaborate example uses the action to find unused message codes in a predefined range.
Suppose all your messages follow a naming convention where each code starts with `ERR-` followed
by a four-digit number. The following action prints the next ten available codes:

```groovy
tasks.named('messageFormatPack') {
  action {
    def codes = it.getMessageCodes()
    def available = []

    for(int n = 1; available.size() < 10; n++) {
      def code = String.format("ERR-%04d", n)

      if (!codes.contains(code))
        available.add(code)
    }

    println "Available error codes:"
    println String.join(" ", available)
  }
}
```

If multiple actions are registered, they are executed in the order they were defined. Each action
receives the same `MessageAccessor` instance.


## Writing the Pack File

After scanning, validation, and action execution, the task serializes all collected messages and
templates into the output pack file. Messages that were excluded by the include/exclude filters
are omitted. Templates that are referenced by the included messages are written automatically.

If the `compress` property is `true`, the output is wrapped in GZip compression. After writing,
the task verifies that the produced file is a valid message format pack by checking its magic
bytes. If the check fails, the task throws a `GradleException` with the message
`Message pack file missing or corrupt`.

The destination directory defaults to `build/messageFormatPack/`. The filename defaults to
`messages.mfp` but can be changed through the `packFilename` property.


## Running the Task

The task can be run directly from the command line:

```shell
./gradlew messageFormatPack
```

Because the task depends on the `main` source set output, Gradle automatically compiles your
Java sources before running the task. If you have wired the task into the `jar` task (as shown
on the [plugin overview page](index.md)), it also runs automatically as part of a regular build:

```shell
./gradlew jar
```
