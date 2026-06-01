# Annotation Adopter

The annotation adopter reads `@MessageDef` and `@TemplateDef` annotations from compiled `.class`
files and publishes the discovered messages and templates to a `MessageSupport` instance. Unlike
traditional Java reflection, the adopter works at the bytecode level: the annotated classes never
need to be loaded into the JVM. This is possible because both annotations use
`RetentionPolicy.CLASS`, which means the Java compiler writes them into the `.class` file but
they are not retained for runtime reflection. A bytecode analysis library such as ASM can still
read them directly from the binary class data. This approach makes annotation adopters safe to use
in build-time processing, plugin environments, and any other situation where loading classes into
the running JVM would be undesirable or impossible.

The `AnnotationAdopter` interface defines the contract for all annotation-based adopters. It
declares the discovery methods for locating annotated classes and provides two static
`getAutoDetected` factory methods that select the best available implementation at runtime.
Behind the interface, the abstract base class `AbstractAnnotationAdopter` contains all the logic
for locating class files, deduplicating scans, and converting annotation data into messages and
templates. It extends `AbstractMessageAdopter`, which holds the two collaborators every adopter
needs: a `MessageFactory` for parsing message format strings and a `MessagePublisher` for
storing the results. Concrete subclasses only need to implement a single method,
`parseClass(InputStream)`, which performs the actual bytecode analysis.

The `message-format-annotations` module ships with three implementations, all located in the
`de.sayayi.lib.message.annotation.adopter.lib` package. All three are based on the
[ASM bytecode library](https://asm.ow2.io/) but differ in where the ASM classes come from.
`AsmAnnotationAdopter` uses the standalone ASM artifact (`org.ow2.asm:asm`).
`ByteBuddyAnnotationAdopter` uses the ASM copy repackaged under `net.bytebuddy.jar.asm` by
Byte Buddy. `SpringAnnotationAdopter` uses the ASM copy repackaged under `org.springframework.asm`
by Spring Framework. Because many projects already pull in Byte Buddy or Spring as a transitive
dependency, the corresponding adopter avoids adding yet another ASM artifact to the classpath.
All three implementations share the same discovery strategies, deduplication behavior, and error
handling described on this page.


## Auto-Detection

In most cases you do not need to pick a specific implementation yourself. The `AnnotationAdopter`
interface provides a static `getAutoDetected` method that inspects the classpath and returns an
instance backed by the best available bytecode library. The selection is based on a priority
order managed through the Java `ServiceLoader` mechanism: each implementation registers an
`AnnotationAdopterProvider` that declares which classes it requires and a numeric priority. At
runtime, `getAutoDetected` loads all providers, filters out those whose required classes are
missing, and selects the one with the lowest order value. The built-in priorities are Spring at
100, ASM at 200, and Byte Buddy at 300. This means Spring is preferred when available, followed
by standalone ASM, then Byte Buddy.

The simplest way to obtain an adopter is to pass a `ConfigurableMessageSupport`:

```java
var messageSupport = MessageSupportFactory.create(
    DefaultFormatterService.getSharedInstance());

var adopter = AnnotationAdopter.getAutoDetected(messageSupport);
```

When the factory and publisher need to be provided independently, use the two-argument overload:

```java
var adopter = AnnotationAdopter.getAutoDetected(messageFactory, publisher);
```

If no suitable bytecode library is found on the classpath, both methods throw a
`MessageAdopterException`.


## Creating an Adopter Directly

When you know which bytecode library you want to use, you can instantiate the corresponding
implementation directly. Pass a `ConfigurableMessageSupport` instance, which provides both a
`MessageFactory` and a `MessagePublisher` through a single argument:

```java
var messageSupport = MessageSupportFactory.create(
    DefaultFormatterService.getSharedInstance());

// Standalone ASM
var adopter = new AsmAnnotationAdopter(messageSupport);

// Byte Buddy's bundled ASM
var adopter = new ByteBuddyAnnotationAdopter(messageSupport);

// Spring Framework's bundled ASM
var adopter = new SpringAnnotationAdopter(messageSupport);
```

When you need to separate the factory from the publisher, for example to collect messages from
multiple sources before publishing them to different targets, every implementation also offers a
two-argument constructor:

```java
var adopter = new AsmAnnotationAdopter(messageFactory, publisher);
```


## Discovery Strategies

The annotation adopter provides four strategies for discovering annotated classes. They can be
mixed freely and in any order, because the adopter tracks which classes have already been
processed and silently skips duplicates.

### Classpath Scanning

The broadest strategy scans one or more packages for class files. The scan traverses directories
on the filesystem as well as jar, war, and zip archives on the classpath. Every `.class` file
found under the specified packages, including nested sub-packages, is analyzed for `@MessageDef`
and `@TemplateDef` annotations.

```java
adopter.adopt(
    Thread.currentThread().getContextClassLoader(),
    Set.of("com.example.messages", "com.example.templates"));
```

The first argument is the `ClassLoader` used to resolve package resources. The second argument is
a set of package names. This strategy is the most convenient when your message definitions are
spread across many classes within a known set of packages, because a single call processes
everything:

```java
// Scan the entire com.example hierarchy
adopter.adopt(
    getClass().getClassLoader(),
    Set.of("com.example"));
```

The `SpringAnnotationAdopter` additionally accepts a Spring `ResourceLoader` in place of the
`ClassLoader`. It extracts the class loader from the resource loader and delegates to the regular
classpath scan, which saves you from calling `getClassLoader()` yourself:

```java
adopter.adopt(resourceLoader, Set.of("com.example.messages"));
```

### Single Class File

When you know the exact location of a class file on disk, you can provide its path directly.
This is useful in build tool integrations, Gradle tasks, or test setups where the output
directory is known:

```java
adopter.adopt(Path.of("build/classes/java/main/com/example/MyMessages.class"));
adopter.adopt(new File("build/classes/java/main/com/example/MyConstants.class"));
```

The `File` variant delegates to the `Path` variant internally, so both behave identically.

### Loaded Type

If the annotated class is already loaded in the JVM, you can pass its `Class` object. The
adopter uses the class loader associated with the type to locate the corresponding `.class`
resource and parses it for annotations:

```java
adopter.adopt(MyMessages.class);
```


### Annotation Instances

The `adopt(MessageDef)` and `adopt(TemplateDef)` methods accept annotation instances directly,
bypassing bytecode scanning entirely. This is primarily useful in programmatic or testing
scenarios where you want to register messages without creating an annotated class. The
`adopter.util` package provides the record implementations `SyntheticMessageDef`,
`SyntheticTemplateDef`, and `SyntheticText` that you can use to construct these instances.

For a simple, non-localized message, pass an empty `Text` array as the third argument and provide
the message text as the second argument:

```java
adopter.adopt(new SyntheticMessageDef("welcome", "Hello, %{name}!"));
// Registers a message with code "welcome"

messageSupport
    .code("welcome")
    .with("name", "Alice")
    .format();
// "Hello, Alice!"
```

For localized messages, leave the text parameter empty and provide the translations through the
`texts` array:

```java
adopter.adopt(new SyntheticMessageDef(
    "goodbye", "", new Text[] {
      new SyntheticText("en", "Goodbye, %{name}!", ""),
      new SyntheticText("de", "Auf Wiedersehen, %{name}!", "")
    }));

messageSupport
    .code("goodbye")
    .with("name", "Bob")
    .locale(Locale.GERMAN)
    .format();
// "Auf Wiedersehen, Bob!"
```

Templates work the same way using `SyntheticTemplateDef`:

```java
adopter.adopt(new SyntheticTemplateDef(
    "opt-suffix", "%{suffix,!empty:' %{suffix}'}"));
// Registers a template named "opt-suffix"

messageSupport
    .message("Done%[opt-suffix]")
    .with("suffix", "successfully")
    .format();
// "Done successfully"
```

The `SyntheticText` record accepts three string parameters: `locale`, `text`, and `value`. The `value`
parameter is only evaluated when both `locale` and `text` are empty, which mirrors the behavior
of the `@Text` annotation's shorthand form. All three parameters are trimmed during construction,
and `null` values are treated as empty strings.


## Method Chaining

All `adopt` methods return the adopter instance itself, so calls can be chained fluently. This is
convenient when adopting multiple individual classes in sequence:

```java
adopter
    .adopt(AuthMessages.class)
    .adopt(OrderMessages.class)
    .adopt(CommonTemplates.class);
```


## Deduplication

The adopter tracks every class it has visited, identified by its classpath entry or fully
qualified type name. When the same class is encountered a second time, whether through a repeated
`adopt` call, overlapping package scans, or a combination of different discovery strategies, it
is silently skipped. This makes it safe to scan broad package hierarchies without worrying about
duplicate processing:

```java
// These two calls overlap on com.example, but each class is processed only once.
adopter.adopt(classLoader, Set.of("com.example"));
adopter.adopt(classLoader, Set.of("com.example.messages"));
```

Deduplication applies at the class level, not at the individual message or template level. If you
need fine-grained control over which messages or templates are accepted, configure a
`MessageFilter` or `TemplateFilter` on the `ConfigurableMessageSupport` before adopting. The
filter is consulted each time a message or template is about to be published, regardless of
whether the class itself has been visited before.


## Error Handling

The adopter throws specific exceptions when problems are encountered during adoption.

A `MessageAdopterException` is thrown when a class file cannot be read, for example because the
specified path does not exist or a classpath scan encounters an I/O error. A
`MessageParserException` is thrown when a message or template text contains invalid message
format syntax. In both cases the exception message provides details about the source of the
error, such as the class file path or the type name that failed.

When two `@Text` entries within the same `@MessageDef` target the same locale but contain
different text, the adopter throws a `DuplicateMessageException`. The equivalent situation for
`@TemplateDef` results in a `DuplicateTemplateException`. If two entries target the same locale
and contain identical text, the duplicate is silently accepted.


## Complete Example

The following example brings together all the pieces. It declares annotated messages, uses
auto-detection to obtain an adopter, scans a package, and formats one of the discovered messages:

```java
@MessageDef(code = "order-confirm", texts = {
    @Text(locale = "en", text = "Order %{id} confirmed for %{customer}."),
    @Text(locale = "de", text = "Bestellung %{id} bestätigt für %{customer}.")
})
public class OrderMessages {}
```

```java
// Set up message support and auto-detect the adopter
var messageSupport = MessageSupportFactory.create(
    DefaultFormatterService.getSharedInstance());
var adopter = AnnotationAdopter.getAutoDetected(messageSupport);

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

All three implementations are part of the `message-format-annotations` module. You only need a
runtime dependency on the bytecode library that backs the implementation you want to use. If you
rely on auto-detection, simply make sure at least one of these libraries is on the classpath.

```
de.sayayi.lib:message-format-annotations:<version>
```

=== "Gradle"

    ```groovy
    dependencies {
      implementation 'de.sayayi.lib:message-format-annotations:<version>'

      // Pick one (or rely on a transitive dependency from your framework):
      runtimeOnly 'org.ow2.asm:asm:9.+'                  // for AsmAnnotationAdopter
      runtimeOnly 'net.bytebuddy:byte-buddy:1.+'         // for ByteBuddyAnnotationAdopter
      runtimeOnly 'org.springframework:spring-core:6.+'  // for SpringAnnotationAdopter
    }
    ```

=== "Maven"

    ```xml
    <dependency>
      <groupId>de.sayayi.lib</groupId>
      <artifactId>message-format-annotations</artifactId>
      <version><!-- version --></version>
    </dependency>

    <!-- Pick one (or rely on a transitive dependency from your framework): -->

    <!-- for AsmAnnotationAdopter -->
    <dependency>
      <groupId>org.ow2.asm</groupId>
      <artifactId>asm</artifactId>
      <version><!-- 9.x --></version>
      <scope>runtime</scope>
    </dependency>

    <!-- for ByteBuddyAnnotationAdopter -->
    <dependency>
      <groupId>net.bytebuddy</groupId>
      <artifactId>byte-buddy</artifactId>
      <version><!-- 1.x --></version>
      <scope>runtime</scope>
    </dependency>

    <!-- for SpringAnnotationAdopter -->
    <dependency>
      <groupId>org.springframework</groupId>
      <artifactId>spring-core</artifactId>
      <version><!-- 6.x --></version>
      <scope>runtime</scope>
    </dependency>
    ```
