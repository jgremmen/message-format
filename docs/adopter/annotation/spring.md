# Spring Annotation Adopter

/// note
This adopter requires the `message-format-spring` module. See [Module Coordinates](#module-coordinates)
for dependency information.
///

`SpringAsmAnnotationAdopter` is the annotation adopter implementation that performs the same
bytecode-level scanning as
[`AsmAnnotationAdopter`](asm.md), but uses Spring Framework's internally bundled copy of the ASM
library (`org.springframework.asm`) instead of the standalone `org.ow2.asm:asm` artifact. If your
application already depends on Spring, this implementation is the natural choice because it does
not introduce an additional ASM dependency.

All discovery strategies, deduplication behavior, method chaining, and error handling are
inherited from [`AbstractAnnotationAdopter`](index.md) and work exactly as described there. This
page covers only what is specific to the Spring-based implementation.


## Setup

Create a `SpringAsmAnnotationAdopter` by passing a `ConfigurableMessageSupport` instance, then
use any of the inherited `adopt` methods to scan for annotations:

```java
var messageSupport = MessageSupportFactory.create(
    DefaultFormatterService.getSharedInstance());

var adopter = new SpringAsmAnnotationAdopter(messageSupport);

// Scan packages for annotated classes
adopter.adopt(
    Thread.currentThread().getContextClassLoader(),
    Set.of("com.example.messages"));
```

Individual classes can be adopted by type reference or file path, just like with the ASM variant:

```java
adopter.adopt(MyMessages.class);
adopter.adopt(Path.of("build/classes/java/main/com/example/Templates.class"));
```

A two-argument constructor is available when the factory and publisher need to be provided
independently:

```java
var adopter = new SpringAsmAnnotationAdopter(messageFactory, publisher);
```


## ResourceLoader Integration

In addition to the standard `ClassLoader`-based classpath scan inherited from
`AbstractAnnotationAdopter`, the Spring variant offers an `adopt(ResourceLoader, Set)` overload
that accepts a Spring `ResourceLoader`. This is convenient in Spring application contexts where
you already have a `ResourceLoader` (or an `ApplicationContext`, which extends it) available. The
adopter extracts the class loader from the resource loader and delegates to the regular classpath
scan:

```java
@Autowired
private ResourceLoader resourceLoader;

// ...

adopter.adopt(resourceLoader, Set.of("com.example.messages"));
```

This is functionally equivalent to calling `adopt(resourceLoader.getClassLoader(), packageNames)`
but saves you from extracting the class loader yourself.


## How It Works

The scanning mechanism mirrors that of `AsmAnnotationAdopter`. Class files are passed to a
Spring ASM `ClassReader` configured to extract only annotation metadata. The adopter visits annotations on the class
declaration itself and on every non-synthetic method, recognizing both the singular
(`@MessageDef`, `@TemplateDef`) and repeatable container (`@MessageDefs`, `@TemplateDefs`)
forms.


## Complete Example

The following example shows a typical Spring-based setup where annotations are scanned at
application startup:

```java
@TemplateDef(name = "user-name",
    text = "%{firstName} %{lastName}")
@MessageDef(code = "user-greeting", texts = {
    @Text(locale = "en", text = "Hello, %[user-name]!"),
    @Text(locale = "fr", text = "Bonjour, %[user-name]!")
})
public class UserMessages {}
```

```java
// Set up message support and adopter
var messageSupport = MessageSupportFactory.create(
    DefaultFormatterService.getSharedInstance());
var adopter = new SpringAsmAnnotationAdopter(messageSupport);

// Scan for annotated classes using a ResourceLoader
adopter.adopt(resourceLoader, Set.of("com.example"));

// Format the message
String result = messageSupport
    .code("user-greeting")
    .with("firstName", "Jean")
    .with("lastName", "Dupont")
    .locale(Locale.FRENCH)
    .format();
// "Bonjour, Jean Dupont!"
```


## Module Coordinates

```
de.sayayi.lib:message-format-spring:<version>
```

This module depends on `message-format-annotations` (which transitively includes the
`message-format` core) and on `org.springframework:spring-context` (version 5.x or 6.x). Spring
Context transitively provides `spring-core`, which contains the bundled ASM classes. When using
Gradle:

```groovy
dependencies {
    implementation 'de.sayayi.lib:message-format-spring:<version>'
}
```

For Maven:

```xml
<dependency>
    <groupId>de.sayayi.lib</groupId>
    <artifactId>message-format-spring</artifactId>
    <version><!-- version --></version>
</dependency>
```

The `message-format-spring` module also provides other Spring integration components such as
`MessageSupportMessageSource` and a SpEL parameter formatter. See the
[Spring module overview](../../add-on/spring/index.md) for details.
