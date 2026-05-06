# Extension Configuration

The `messageFormat` extension is the central configuration point for the Gradle plugin. All
properties set on the extension are forwarded as conventions to the `messageFormatPack` task, so
in most cases you only need to configure the extension and the task will pick up the values
automatically.


## Properties

### `packFilename`

The name of the output pack file. The default value is `messages.mfp`. If your project produces
multiple pack files (for example, one per subproject in a multi-project build), you can give each
one a distinct name to avoid collisions:

```groovy
messageFormat {
  packFilename = 'orders-messages.mfp'
}
```

The file is written to the task's destination directory (`build/messageFormatPack/` by default).
Only the filename is configured here, not the full path.


### `compress`

Controls whether the output pack file is GZip-compressed. The default value is `false`. The
binary pack format already uses extensive bit-packing, so compression may not reduce the size
noticeably for small message sets. For larger sets with hundreds or thousands of messages,
enabling compression can reduce the file size significantly:

```groovy
messageFormat {
  compress = true
}
```

Compressed and uncompressed pack files are both imported the same way at runtime. The
`importMessages` method detects the format automatically.


### `duplicateMsgStrategy`

Determines how the plugin handles duplicate message codes and template names. A duplicate occurs
when two messages share the same code but have different message text, or when two templates share
the same name but have different content. If two entries with the same code or name have identical
content, they are silently accepted regardless of the strategy.

The default strategy is `IGNORE_AND_WARN`. The following strategies are available:

`IGNORE` silently discards the second definition and keeps the first.

`IGNORE_AND_WARN` discards the second definition but logs a warning that identifies the
duplicate code and the class in which it was found.

`OVERRIDE` silently replaces the first definition with the second.

`OVERRIDE_AND_WARN` replaces the first definition with the second and logs a warning.

`FAIL` immediately stops the build with an error when a duplicate is encountered.

The property accepts both the enum constant and a case-insensitive string. Dashes in the string
are converted to underscores automatically, so all of the following are equivalent:

```groovy
messageFormat {
  duplicateMsgStrategy = 'FAIL'
}
```

```groovy
messageFormat {
  duplicateMsgStrategy = 'override-and-warn'
}
```

```groovy
messageFormat {
  duplicateMsgStrategy = 'IGNORE_AND_WARN'
}
```


### `validateReferencedTemplates`

Controls whether the plugin checks that all templates referenced by messages (including nested
template references) are present in the scanned classes. The default value is `true`.

When enabled, the task collects all template names that appear in `%[template-name]` references
across all scanned messages and verifies that a corresponding `@TemplateDef` exists. If one or
more templates are missing, the build fails with an error listing the missing template names.
This catches broken template references early, at build time, rather than at runtime when a
message is formatted.

When disabled, no such validation is performed. This can be useful if templates are loaded from
a different source at runtime, for example from a separate pack file or through programmatic
registration:

```groovy
messageFormat {
  validateReferencedTemplates = false
}
```


## Source Sets

By default, the plugin scans the output of the `main` source set, which means all compiled
`.class` files under `build/classes/java/main/`. If your messages and templates are defined in
additional source sets, you can add them to the scan with the `sourceSet` method:

```groovy
messageFormat {
  sourceSet sourceSets.main
  sourceSet sourceSets.test
}
```

You can also point the plugin at arbitrary file collections through the `sources` property. Only
`.class` files in the collection are actually scanned; all other file types are ignored:

```groovy
messageFormat {
  sources.from(files('libs/external-messages.jar'))
}
```

This flexibility is useful when message definitions come from precompiled libraries or generated
code that does not belong to a standard Gradle source set.


## Include and Exclude Filters

The `include` and `exclude` methods control which message codes end up in the pack file. Both
accept one or more regular expressions that are matched against each message code found during
scanning.

When no include filters are configured (the default), all scanned messages are eligible for
inclusion. As soon as at least one include filter is specified, only messages whose code matches
at least one of the include patterns are considered. Exclude filters are evaluated after include
filters: if a message code matches an exclude pattern, it is removed from the output even if it
also matches an include pattern.

The following example includes only message codes that start with `ORDER-` but excludes any
codes ending in `-DRAFT`:

```groovy
messageFormat {
  include 'ORDER-.*'
  exclude '.*-DRAFT'
}
```

Multiple patterns can be passed in a single call or across multiple calls, and they accumulate:

```groovy
messageFormat {
  include 'ORDER-.*', 'INVOICE-.*'
  exclude '.*-INTERNAL'
}
```

Filters apply only to messages, not to templates. Templates are included automatically if they
are referenced by any message that passes the filters.


## Single-Project Configuration

A typical single-project setup requires very little configuration. Apply the plugin, optionally
adjust the extension properties, and wire the pack file into the jar:

```groovy
plugins {
  id 'java'
  id 'de.sayayi.plugin.gradle.message'
}

dependencies {
  implementation 'de.sayayi.lib:message-format-annotations:<version>'
}

messageFormat {
  compress = true
  duplicateMsgStrategy = 'fail'
}

jar {
  from messageFormatPack {
    into 'META-INF'
  }
}
```

With this configuration in place, running `./gradlew jar` compiles your Java sources, scans the
compiled classes for `@MessageDef` and `@TemplateDef` annotations, produces a compressed
`messages.mfp` file, and bundles it into `META-INF/` inside the jar. The build fails immediately
if two classes define the same message code with different text.


## Multi-Project Configuration

In a Gradle multi-project build, each subproject typically defines its own messages and templates.
There are two common approaches for handling this.

### Per-Subproject Pack Files

The simplest approach is to apply the plugin independently to each subproject that contains
message definitions. Every subproject produces its own `.mfp` file, and at runtime the
application imports all of them:

```groovy
// settings.gradle
rootProject.name = 'my-application'
include 'core', 'orders', 'billing'
```

```groovy
// core/build.gradle
plugins {
  id 'java-library'
  id 'de.sayayi.plugin.gradle.message'
}

messageFormat {
  packFilename = 'core-messages.mfp'
}

jar {
  from messageFormatPack {
    into 'META-INF'
  }
}
```

```groovy
// orders/build.gradle
plugins {
  id 'java-library'
  id 'de.sayayi.plugin.gradle.message'
}

messageFormat {
  packFilename = 'orders-messages.mfp'
}

jar {
  from messageFormatPack {
    into 'META-INF'
  }
}
```

At runtime, import each pack file separately:

```java
var messageSupport = MessageSupportFactory.create(
    DefaultFormatterService.getSharedInstance());

for(var resource: List.of(
    "/META-INF/core-messages.mfp",
    "/META-INF/orders-messages.mfp")) {
  try(var in = getClass().getResourceAsStream(resource)) {
    messageSupport.importMessages(in);
  }
}
// Messages from all subprojects are now available.
```

This approach keeps each subproject self-contained and allows independent builds and testing.

### Shared Plugin Configuration

If many subprojects share the same plugin configuration, you can define it once in a convention
plugin or a `subprojects` block in the root build script to avoid repetition:

```groovy
// build.gradle (root)
subprojects {
  plugins.withId('de.sayayi.plugin.gradle.message') {
    messageFormat {
      compress = true
      duplicateMsgStrategy = 'ignore-and-warn'
    }

    jar {
      from messageFormatPack {
        into 'META-INF'
      }
    }
  }
}
```

Each subproject still applies the plugin itself, but the configuration block in the root project
ensures that all subprojects share the same compression and duplicate handling settings. The
`plugins.withId` guard makes the block apply only to subprojects that actually use the message
format plugin, so subprojects without message definitions are not affected.

### Aggregated Pack File

If you prefer a single pack file that contains all messages from all subprojects, you can
configure one subproject (or the root project) to scan the compiled classes of multiple
subprojects:

```groovy
// app/build.gradle
plugins {
  id 'java'
  id 'de.sayayi.plugin.gradle.message'
}

dependencies {
  implementation project(':core')
  implementation project(':orders')
  implementation project(':billing')
}

messageFormat {
  sourceSet project(':core').sourceSets.main
  sourceSet project(':orders').sourceSets.main
  sourceSet project(':billing').sourceSets.main
  duplicateMsgStrategy = 'fail'
}

jar {
  from messageFormatPack {
    into 'META-INF'
  }
}
```

This configuration produces a single `messages.mfp` containing all messages and templates from
the `core`, `orders`, and `billing` subprojects, plus any messages defined in `app` itself
(because the plugin always includes the `main` source set by default). Because the sources from
other subprojects are added explicitly, the task also depends on their compilation output, so
Gradle compiles all three subprojects before scanning.

Using the `FAIL` strategy in an aggregated setup is recommended because it catches accidental
code collisions between subprojects at build time.
