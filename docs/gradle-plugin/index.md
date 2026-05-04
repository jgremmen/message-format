---
icon: material/connection
---

# Gradle Plugin

The `message-gradle-plugin` provides a Gradle plugin for scanning compiled classes for
`@MessageDef` and `@TemplateDef` annotations and packing them into a single binary file
(`.mfp`) that can be efficiently imported at runtime.

## Applying the Plugin

```kotlin
plugins {
    id("de.sayayi.message-format")
}
```

## Extension Configuration

The plugin registers a `messageFormat` extension with the following properties:

| Property | Type | Default | Description |
|---|---|---|---|
| `packFilename` | `String` | `messages.mfp` | Name of the output pack file |
| `compress` | `Boolean` | `false` | Whether to GZip-compress the output |
| `duplicateMsgStrategy` | `DuplicateMsgStrategy` | `IGNORE_AND_WARN` | How to handle duplicate message codes |
| `validateReferencedTemplates` | `Boolean` | `true` | Validate that all referenced templates exist |

The `main` source set is used as the default source for class scanning.
