# Annotation Adopter

The annotation adopter reads `@MessageDef` and `@TemplateDef` annotations from compiled `.class` files and publishes 
the discovered messages and templates to a `MessageSupport` instance. The annotated classes do not need to be loaded 
into the JVM. Both annotations use `RetentionPolicy.CLASS`, so the adopter can read them directly from the binary class
data without requiring runtime reflection.

The `AnnotationAdopter` class provides multiple strategies for locating annotated classes. It tracks which classes have
already been processed to avoid duplicate registrations.


## Creating an Adopter

The most common way to create an `AnnotationAdopter` is by passing a `ConfigurableMessageSupport`, which provides both 
a `MessageFactory` and a `MessagePublisher` through a single argument:

```java
var messageSupport = MessageSupportFactory.create(
    DefaultFormatterService.getSharedInstance());

var adopter = new AnnotationAdopter(messageSupport);
```

When the factory and publisher need to be provided independently, for example to collect messages from multiple sources 
before publishing them to different targets, use the two-argument constructor:

```java
var adopter = new AnnotationAdopter(messageFactory, publisher);
```


## Discovery Strategies

The annotation adopter provides four strategies for discovering annotated classes. They can be mixed freely and in any
order, because the adopter tracks which classes have already been processed and silently skips duplicates.

### Classpath Scanning

The broadest strategy scans one or more packages for class files. The scan traverses directories on the filesystem as 
well as jar, war and zip archives on the classpath. Every `.class` file found under the specified packages, including 
nested sub-packages, is analyzed for `@MessageDef` and `@TemplateDef` annotations.

```java
adopter.adopt(
    Thread.currentThread().getContextClassLoader(),
    Set.of("com.example.messages", "com.example.templates"));
```

The first argument is the `ClassLoader` used to resolve package resources. The second argument is a set of package 
names. This strategy is the most convenient when your message definitions are spread across many classes within a known 
set of packages, because a single call processes everything:

```java
// Scan the entire com.example hierarchy
adopter.adopt(
    getClass().getClassLoader(),
    Set.of("com.example"));
```

### Single Class File

When you know the exact location of a class file on disk, you can provide its path directly. This is useful in build
tool integrations, Gradle tasks, or test setups where the output directory is known:

```java
adopter.adopt(Path.of("build/classes/java/main/com/example/MyMessages.class"));
adopter.adopt(new File("build/classes/java/main/com/example/MyConstants.class"));
```

The `File` variant delegates to the `Path` variant internally, so both behave identically.

### Loaded Type

If the annotated class is already loaded in the JVM, you can pass its `Class` object. The adopter uses the class loader
associated with the type to locate the corresponding `.class` resource and parses it for annotations. If the type has 
no class loader (e.g. bootstrap classes), the call returns immediately without doing anything.

```java
adopter.adopt(MyMessages.class);
```


### Annotation Instances

The `adopt(MessageDef)` and `adopt(TemplateDef)` methods accept annotation instances directly, bypassing bytecode
scanning entirely. This is useful in programmatic or testing scenarios where you want to register messages without
creating an annotated class. The `adopter.util` package provides the record implementations `SyntheticMessageDef`, 
`SyntheticTemplateDef` and `SyntheticText` for constructing these instances.

For a simple, non-localized message you only need to provide the code and the text. The convenience constructor creates
the record without any localized `Text` variants:

```java
adopter.adopt(new SyntheticMessageDef("welcome", "Hello, %{name}!"));
// Registers a message with code "welcome"

messageSupport
    .code("welcome")
    .with("name", "Alice")
    .format();
// "Hello, Alice!"
```

For localized messages, provide the translations through the `texts` array and leave the `text` parameter `null` or 
empty:

```java
adopter.adopt(new SyntheticMessageDef(
    "goodbye", null, new Text[] {
      new SyntheticText("en", "Goodbye, %{name}!", null),
      new SyntheticText("de", "Auf Wiedersehen, %{name}!", null)
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

The `SyntheticText` record accepts three string parameters: `locale`, `text` and `value`. When both `locale` and `text`
are empty, the `value` parameter is used instead, which mirrors the behavior of the `@Text` annotation's shorthand form 
`@Text("...")`. All three parameters are trimmed during construction and `null` values are treated as empty strings. 
Similarly, `SyntheticMessageDef` trims the `code` and defaults a `null` text to an empty string, while
`SyntheticTemplateDef` validates the `name` and trims the text.


## Method Chaining

All `adopt` methods return the adopter instance itself, so calls can be chained fluently:

```java
adopter
    .adopt(AuthMessages.class)
    .adopt(OrderMessages.class)
    .adopt(CommonTemplates.class);
```


## Deduplication

The adopter tracks every class it has visited, identified by its classpath entry or fully qualified type name. When the
same class is encountered a second time, whether through a repeated `adopt` call, overlapping package scans, or a 
combination of different discovery strategies, it is silently skipped. This makes it safe to scan broad package 
hierarchies without worrying about duplicate processing:

```java
// These two calls overlap on com.example, but each class is processed only once.
adopter.adopt(classLoader, Set.of("com.example"));
adopter.adopt(classLoader, Set.of("com.example.messages"));
```

Deduplication applies at the class level, not at the individual message or template level. If you need fine-grained 
control over which messages or templates are accepted, configure a `MessageFilter` or `TemplateFilter` on the 
`ConfigurableMessageSupport` before adopting. The filter is consulted each time a message or template is about to be 
published, regardless of whether the class itself has been visited before.


## Error Handling

The adopter throws specific exceptions when problems are encountered during adoption.

A `MessageAdopterException` is thrown when a class file cannot be read, for example because the specified path does not
exist or a classpath scan encounters an I/O error. A `MessageParserException` is thrown when a message or template text 
contains invalid message format syntax. In both cases the exception message provides details about the source of the
error, such as the class file path or the type name that failed.

When two `@Text` entries within the same `@MessageDef` target the same locale but contain different text, the adopter
throws a `DuplicateMessageException`. The equivalent situation for `@TemplateDef` results in a 
`DuplicateTemplateException`. If two entries target the same locale and contain identical text, the duplicate is 
silently accepted.


## Complete Example

The following example brings together all the pieces. It declares annotated messages, creates an adopter, scans a
package and formats one of the discovered messages:

```java
@MessageDef(code = "order-confirm", texts = {
    @Text(locale = "en", text = "Order %{id} confirmed for %{customer}."),
    @Text(locale = "de", text = "Bestellung %{id} bestätigt für %{customer}.")
})
public class OrderMessages {}
```

```java
// Set up message support and create the adopter
var messageSupport = MessageSupportFactory.create(
    DefaultFormatterService.getSharedInstance());
var adopter = new AnnotationAdopter(messageSupport);

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

The annotation adopter is part of the `message-format-annotations` module. No additional dependencies are required.

=== "Gradle"

    ```groovy
    dependencies {
      implementation 'de.sayayi.lib:message-format-annotations:<version>'
    }
    ```

=== "Maven"

    ```xml
    <dependency>
      <groupId>de.sayayi.lib</groupId>
      <artifactId>message-format-annotations</artifactId>
      <version><!-- version --></version>
    </dependency>
    ```
