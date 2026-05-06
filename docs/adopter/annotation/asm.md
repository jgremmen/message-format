# ASM Annotation Adopter

/// note
This adopter requires the `message-format-asm` module. See [Module Coordinates](#module-coordinates)
for dependency information.
///

`AsmAnnotationAdopter` is the concrete annotation adopter implementation that uses the
[ASM bytecode library](https://asm.ow2.io/) to read
`@MessageDef` and `@TemplateDef` annotations directly from compiled `.class` files. Because ASM
operates on raw bytecode, the scanned classes never need to be loaded into the JVM. This makes
`AsmAnnotationAdopter` the right choice for standalone applications, command-line tools, build
plugins, and any environment that does not already include Spring Framework.

All discovery strategies, deduplication behavior, method chaining, and error handling are
inherited from [`AbstractAnnotationAdopter`](index.md) and work exactly as described there. This
page covers only what is specific to the ASM-based implementation.


## Setup

Create an `AsmAnnotationAdopter` by passing a `ConfigurableMessageSupport` instance, then use
any of the inherited `adopt` methods to scan for annotations:

```java
var messageSupport = MessageSupportFactory.create(
    DefaultFormatterService.getSharedInstance());

var adopter = new AsmAnnotationAdopter(messageSupport);

// Scan packages for annotated classes
adopter.adopt(
    Thread.currentThread().getContextClassLoader(),
    Set.of("com.example.messages"));
```

Individual classes can be adopted by type reference or file path:

```java
adopter.adopt(MyMessages.class);
adopter.adopt(Path.of("build/classes/java/main/com/example/Templates.class"));
```

A two-argument constructor is available when the factory and publisher need to be provided
independently:

```java
var adopter = new AsmAnnotationAdopter(messageFactory, publisher);
```


## How It Works

When a class file is presented to the adopter, it is passed to an ASM `ClassReader` that is
configured to extract only annotation metadata. This makes the scan both fast and
memory-efficient, even for large class files.

The adopter visits annotations on the class declaration itself and on every non-synthetic method.
Annotations are recognized in both their singular form (`@MessageDef`, `@TemplateDef`) and their
repeatable container form (`@MessageDefs`, `@TemplateDefs`).


## Complete Example

The following example brings together all the pieces. It creates a `MessageSupport` instance,
scans a package for annotated classes, and formats one of the discovered messages:

```java
@MessageDef(code = "order-confirm", texts = {
    @Text(locale = "en", text = "Order %{id} confirmed for %{customer}."),
    @Text(locale = "de", text = "Bestellung %{id} bestätigt für %{customer}.")
})
public class OrderMessages {}
```

```java
// Set up message support and adopter
var messageSupport = MessageSupportFactory.create(
    DefaultFormatterService.getSharedInstance());
var adopter = new AsmAnnotationAdopter(messageSupport);

// Scan for annotated classes
adopter.adopt(
    Thread.currentThread().getContextClassLoader(),
    Set.of("com.example"));

// Format the message
String result = messageSupport
    .code("order-confirm")
    .with("id", "A-7042")
    .with("customer", "Alice")
    .locale(Locale.ENGLISH)
    .format();
// "Order A-7042 confirmed for Alice."
```


## Module Coordinates

```
de.sayayi.lib:message-format-asm:<version>
```

This module depends on `message-format-annotations` (which transitively includes the
`message-format` core) and on the ASM library `org.ow2.asm:asm` (version 9.x).

=== "Gradle"

    ```groovy
    dependencies {
      implementation 'de.sayayi.lib:message-format-asm:<version>'
    }
    ```

=== "Maven"

    ```xml
    <dependency>
      <groupId>de.sayayi.lib</groupId>
      <artifactId>message-format-asm</artifactId>
      <version><!-- version --></version>
    </dependency>
    ```

If your project already uses Spring Framework, consider using the
[Spring Annotation Adopter](spring.md) instead. Spring bundles its own copy of ASM, and the
`SpringAsmAnnotationAdopter` provides the same scanning functionality without adding an extra
ASM dependency to your classpath.
