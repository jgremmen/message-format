---
icon: simple/gradle
---

# Gradle Plugin

When your project uses `@MessageDef` and `@TemplateDef` annotations to declare messages and
templates in source code, the Gradle plugin can scan the compiled classes and produce a `.mfp`
pack file automatically as part of the build. This removes the need to export pack files
manually and ensures that the pack file is always in sync with the annotated message definitions
in your codebase.


## Applying the Plugin

Apply the plugin in your `build.gradle` file. Because the plugin requires compiled class files to
scan, your project must also apply the `java` plugin (or a plugin that extends it, such as
`java-library`):

=== "Groovy DSL"

    ```groovy
    plugins {
      id 'java'
      id 'de.sayayi.plugin.gradle.message'
    }
    ```

=== "Kotlin DSL"

    ```kotlin
    plugins {
      java
      id("de.sayayi.plugin.gradle.message")
    }
    ```

Applying the plugin has two effects. It registers a `messageFormat` extension block that you can
use to configure how messages are packed, and it registers a `messageFormatPack` task in the
`build` group that performs the actual scanning and packing. The extension and its properties are
described in detail on the [Extension](extension.md) page, and the task specifics are covered on
the [Pack Task](pack-task.md) page.


## Including the Pack File in Your Jar

The `messageFormatPack` task produces its output in the `build/messageFormatPack/` directory. To
include the generated pack file in your application jar, add a `from` directive to the `jar`
task:

```groovy
jar {
  from messageFormatPack {
    into 'META-INF'
  }
}
```

This tells Gradle to copy the output of `messageFormatPack` into the `META-INF` directory inside
the jar. Gradle automatically establishes a task dependency, so `messageFormatPack` runs before
`jar` whenever you build the project.

At runtime, you can then load the pack file from the classpath:

```java
var messageSupport = MessageSupportFactory.create(
    DefaultFormatterService.getSharedInstance());

try(var in = getClass().getResourceAsStream("/META-INF/messages.mfp")) {
  messageSupport.importMessages(in);
}
// All messages and templates from the pack file are now available.
```


## Minimal Example

The following example shows the complete workflow from annotated source code to a runnable
application. Suppose you have a class with a few message definitions:

```java
@MessageDef(code = "greeting", text = "Hello %{name}!")
@MessageDef(code = "item-count",
    text = "%{count,0:'No items',1:'1 item',:'%{count} items'} in stock.")
public class ShopMessages {}
```

Your `build.gradle` applies the plugin and includes the pack file in the jar:

```groovy
plugins {
  id 'java'
  id 'de.sayayi.plugin.gradle.message'
}

jar {
  from messageFormatPack {
    into 'META-INF'
  }
}
```

Running `./gradlew jar` compiles the source, scans the compiled classes for annotations, writes
the pack file, and bundles it into the jar. At runtime:

```java
var messageSupport = MessageSupportFactory.create(
    DefaultFormatterService.getSharedInstance());

try(var in = getClass().getResourceAsStream("/META-INF/messages.mfp")) {
  messageSupport.importMessages(in);
}

messageSupport.code("greeting").with("name", "World").format();
// "Hello World!"

messageSupport.code("item-count").with("count", 5).format();
// "5 items in stock."
```
