# ASM

The `message-format-asm` module provides `AsmAnnotationAdopter`, an ASM-based class file scanner
that detects `@MessageDef` and `@TemplateDef` annotations without loading classes into the JVM.

## AsmAnnotationAdopter

Scans compiled `.class` files using the ASM bytecode library. Annotations are detected on
class-level as well as on non-synthetic methods.

```java
var messageSupport = MessageSupportFactory.create(
    DefaultFormatterService.getSharedInstance());

var adopter = new AsmAnnotationAdopter(messageSupport);
adopter.adopt(inputStream);  // InputStream of a .class file
```

### Features

- Class-level and method-level annotation scanning
- No runtime class loading required
- Supports `MessageFilter` and `TemplateFilter` for selective adoption
- Direct annotation adoption via inherited `adopt(MessageDef)` / `adopt(TemplateDef)`

### Requirements

Requires a dependency on the ASM library: `org.ow2.asm:asm:9.+`

## Module Coordinates

```
de.sayayi.lib:message-format-asm:<version>
```
