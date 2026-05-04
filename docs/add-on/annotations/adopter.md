# Annotation Adopter Base

The annotations described on the [Annotations](index.md) page only declare messages and
templates in source code. To make them available for formatting at runtime, the annotations need
to be extracted from the compiled class files and published to a `MessageSupport` instance. This
is the responsibility of the annotation adopter.

`AbstractAnnotationAdopter` is the abstract base class that provides this functionality. It
extends `AbstractMessageAdopter` and offers several strategies for discovering annotated classes,
ranging from broad classpath scans down to individual class files or even programmatically
constructed annotation instances. Concrete subclasses supply the actual bytecode analysis.
`AsmAnnotationAdopter` in the [ASM module](../asm/index.md) uses the standalone ASM library,
while `SpringAsmAnnotationAdopter` in the [Spring module](../spring/index.md) uses Spring's
bundled ASM copy, which avoids an extra dependency in Spring-based applications.

All `adopt` methods return the adopter instance itself, so calls can be chained fluently.


## Creating an Adopter

An adopter is constructed by passing a `ConfigurableMessageSupport` instance, which serves as
both the source for the message factory (responsible for parsing message format strings) and the
publisher (responsible for storing the resulting messages and templates). This is the most common
pattern:

```java
var messageSupport = MessageSupportFactory.create(
    DefaultFormatterService.getSharedInstance());
var adopter = new AsmAnnotationAdopter(messageSupport);
```

If you need to separate the factory from the publisher, for example when collecting messages
before publishing them to multiple targets, use the two-argument constructor that accepts a
`MessageFactory` and a `MessagePublisher` independently:

```java
var adopter = new AsmAnnotationAdopter(messageFactory, publisher);
```


## Discovery Strategies

### Classpath Scanning

The broadest strategy scans one or more packages for class files. The scan traverses directories
on the filesystem as well as jar, war and zip archives on the classpath. Every class file found
under the specified packages is analyzed for `@MessageDef` and `@TemplateDef` annotations.

```java
adopter.adopt(
    Thread.currentThread().getContextClassLoader(),
    Set.of("com.example.messages", "com.example.templates"));
```

The first argument is the `ClassLoader` used to resolve package resources. The second argument is
a set of package names to scan. All classes found under those packages, including nested
packages, are processed.

Classpath scanning is the most convenient approach for applications that organize their message
definitions across multiple classes within a known set of packages. It requires only a single
call to discover everything, regardless of how many annotated classes exist:

```java
// Scan multiple packages in one call
adopter.adopt(
    getClass().getClassLoader(),
    Set.of("com.example.core.messages",
           "com.example.web.messages",
           "com.example.service.messages"));
```

### Single Class File

When you know exactly which class file to process, you can provide its location as a `Path` or
`File`. This is useful in build tool integrations or test setups where the class file location is
known at compile time.

```java
adopter.adopt(Path.of("target/classes/com/example/MyMessages.class"));
adopter.adopt(new File("target/classes/com/example/MyConstants.class"));
```

### Loaded Type

If the class is already loaded in the JVM, you can pass the `Class` object directly. The adopter
uses the class loader associated with the type to locate the corresponding `.class` file and
parse it for annotations. This is the most concise approach when you have a direct reference to
the annotated class:

```java
adopter.adopt(MyMessages.class);
```

Classes loaded by the bootstrap class loader (which have no class loader) are silently skipped,
since the adopter cannot locate their class file through the standard resource mechanism.

Multiple classes can be adopted in sequence using method chaining:

```java
adopter
    .adopt(AuthMessages.class)
    .adopt(OrderMessages.class)
    .adopt(CommonTemplates.class);
```

### Annotation Instances

The `adopt(MessageDef)` and `adopt(TemplateDef)` methods accept annotation instances directly,
bypassing the bytecode scanning entirely. This is primarily useful in programmatic or testing
scenarios where you want to register messages and templates without creating an annotated class.

The `AbstractAnnotationAdopter` provides inner record implementations, `MessageDefImpl`,
`TemplateDefImpl` and `TextImpl`, that can be used to construct these instances. The third
constructor parameter is an array of `Text` instances. When providing a single non-localized text
via the `text` parameter, pass an empty array for `texts`:

```java
adopter.adopt(new AbstractAnnotationAdopter.MessageDefImpl(
    "welcome", "Hello, %{name}!", new Text[0]));
// Registers a message with code "welcome"

messageSupport
    .code("welcome")
    .with("name", "Alice")
    .format();
// "Hello, Alice!"
```

For localized messages, leave the `text` parameter empty and provide the translations through the
`texts` array:

```java
adopter.adopt(new AbstractAnnotationAdopter.MessageDefImpl(
    "goodbye", "", new Text[] {
        new AbstractAnnotationAdopter.TextImpl("en", "Goodbye, %{name}!", ""),
        new AbstractAnnotationAdopter.TextImpl("de", "Auf Wiedersehen, %{name}!", "")
    }));

messageSupport
    .code("goodbye")
    .with("name", "Bob")
    .locale(Locale.GERMAN)
    .format();
// "Auf Wiedersehen, Bob!"
```

Templates work the same way using `TemplateDefImpl`:

```java
adopter.adopt(new AbstractAnnotationAdopter.TemplateDefImpl(
    "opt-suffix", "%{suffix,!empty:' %{suffix}'}", new Text[0]));
// Registers a template named "opt-suffix"

messageSupport
    .message("Done%[opt-suffix]")
    .with("suffix", "successfully")
    .format();
// "Done successfully"

messageSupport
    .message("Done%[opt-suffix]")
    .with("suffix", "")
    .format();
// "Done"
```

The `TextImpl` record accepts three string parameters: `locale`, `text` and `value`. The `value`
parameter is only used when both `locale` and `text` are empty, matching the behavior of the
`@Text` annotation. All three parameters are trimmed during construction, and `null` values are
treated as empty strings.


## Deduplication

The adopter tracks every class it has visited, identified by its classpath entry or fully
qualified type name. When the same class is encountered a second time, whether through a
repeated `adopt` call, overlapping package scans, or a combination of different discovery
strategies, it is silently skipped. This makes it safe to scan broad package hierarchies without
worrying about duplicate processing:

```java
// These two calls overlap on the "com.example" package, but each class
// under that package is still processed only once.
adopter.adopt(classLoader, Set.of("com.example"));
adopter.adopt(classLoader, Set.of("com.example.messages"));
```

Deduplication applies at the class level, not at the individual message or template level. If you
need to control which messages or templates are accepted from a class, configure a
`MessageFilter` or `TemplateFilter` on the `ConfigurableMessageSupport` before adopting. The
filter is consulted each time a message or template is about to be published, regardless of
whether the class has been visited before.


## Error Handling

The adopter throws specific exceptions when problems are encountered during adoption.

A `MessageAdopterException` is thrown when a class file cannot be read, for example when a path
does not exist or the classpath scan encounters an I/O error. A `MessageParserException` is
thrown when a message or template text contains invalid message format syntax. In both cases the
exception message provides details about the source of the error, such as the class file path or
the type name that failed.

When two `@Text` entries within the same `@MessageDef` target the same locale but contain
different text, the adopter throws a `DuplicateMessageException`. The same rule applies to
`@TemplateDef`, which throws a `DuplicateTemplateException` in the equivalent situation.
Identical duplicate texts for the same locale are silently accepted.
