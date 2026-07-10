---
icon: simple/gradle
---

# Gradle Plugin

When a project uses `@MessageDef` and `@TemplateDef` annotations to declare messages and templates in source code, the 
Gradle plugin can scan the compiled classes and produce a `.mfp` pack file automatically as part of the build. This
removes the need to export pack files manually and ensures that the pack file is always in sync with the annotated 
message definitions in the codebase.


## Applying the Plugin

Apply the plugin in the `build.gradle` file. Because the plugin requires compiled class files to scan, the project must
also apply the `java` plugin (or a plugin that extends it, such as `java-library`):

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

Applying the plugin has two effects. It registers a `messageFormat` extension block that can be used to configure how
messages are packed and it registers a `messageFormatPack` task in the `build` group that performs the actual scanning 
and packing. The extension and its properties are described in detail on the [Extension](extension.md) page and the 
task specifics are covered on the [Pack Task](pack-task.md) page.


## Including the Pack File in the Jar

The `messageFormatPack` task produces its output in the `<buildDir>/messageFormatPack/` directory. To include the 
generated pack file in the application jar, add a `from` directive to the `jar` task:

=== "Groovy DSL"

    ```groovy
    jar {
      from messageFormatPack {
        into 'META-INF'
      }
    }
    ```

=== "Kotlin DSL"

    ```kotlin
    tasks.jar {
      from(tasks.named("messageFormatPack")) {
        into("META-INF")
      }
    }
    ```

This tells Gradle to copy the output of `messageFormatPack` into the `META-INF` directory inside the jar. Gradle
automatically establishes a task dependency, so `messageFormatPack` runs before `jar` whenever the project is built.

At runtime, the pack file can then be loaded from the classpath. The default filename is derived from the project name
(e.g. a project named `my-app` produces `my-app.mfp`):

```java
var messageSupport = MessageSupportFactory.create(
    DefaultFormatterService.getSharedInstance());

try(var in = getClass().getResourceAsStream("/META-INF/my-app.mfp")) {
  messageSupport.importMessages(in);
}
// All messages and templates from the pack file are now available.
```


## Minimal Example

The following example shows the complete workflow from annotated source code to a runnable application. Consider a
class with a few message definitions:

```java
@MessageDef(code = "greeting", text = "Hello %{name}!")
@MessageDef(code = "item-count",
    text = "%{count,0:'No items',1:'1 item',:'%{count} items'} in stock.")
public class ShopMessages {}
```

The build script applies the plugin and includes the pack file in the jar:

=== "Groovy DSL"

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

=== "Kotlin DSL"

    ```kotlin
    plugins {
      java
      id("de.sayayi.plugin.gradle.message")
    }

    tasks.jar {
      from(tasks.named("messageFormatPack")) {
        into("META-INF")
      }
    }
    ```

Running `./gradlew jar` compiles the source, scans the compiled classes for annotations, writes the pack file and
bundles it into the jar. At runtime:

```java
var messageSupport = MessageSupportFactory.create(
    DefaultFormatterService.getSharedInstance());

try(var in = getClass().getResourceAsStream("/META-INF/shop.mfp")) {
  messageSupport.importMessages(in);
}

messageSupport.code("greeting").with("name", "World").format();
// "Hello World!"

messageSupport.code("item-count").with("count", 5).format();
// "5 items in stock."
```

In this example the pack file is named `shop.mfp` because the Gradle project is named `shop`. If a different filename 
is needed, configure the `packFilename` property in the `messageFormat` extension block as described on the
[Extension](extension.md) page.
