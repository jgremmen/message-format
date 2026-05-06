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

The abstract base class `AbstractAnnotationAdopter` contains all the logic for locating class
files, deduplicating scans, and converting annotation data into messages and templates. It
extends `AbstractMessageAdopter`, which holds the two collaborators every adopter needs: a
`MessageFactory` for parsing message format strings and a `MessagePublisher` for storing the
results. Concrete subclasses only need to implement a single method, `parseClass(InputStream)`,
which performs the actual bytecode analysis. Two such implementations are provided by the
library: [`AsmAnnotationAdopter`](asm.md) uses the standalone ASM library, while
[`SpringAsmAnnotationAdopter`](spring.md) uses Spring Framework's bundled ASM copy, which avoids
an extra dependency in Spring-based applications.


## Creating an Adopter

An adopter is constructed by passing a `ConfigurableMessageSupport` instance. Because
`ConfigurableMessageSupport` implements `MessagePublisher` and provides access to a
`MessageFactory` through its message accessor, a single argument is sufficient to set up both
collaborators:

```java
var messageSupport = MessageSupportFactory.create(
    DefaultFormatterService.getSharedInstance());

var adopter = new AsmAnnotationAdopter(messageSupport);
```

When you need to separate the factory from the publisher, for example to collect messages from
multiple sources before publishing them to different targets, every annotation adopter also
offers a two-argument constructor:

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
`AbstractAnnotationAdopter` provides inner record implementations `MessageDefImpl`,
`TemplateDefImpl`, and `TextImpl` that you can use to construct these instances.

For a simple, non-localized message, pass an empty `Text` array as the third argument and provide
the message text as the second argument:

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

For localized messages, leave the text parameter empty and provide the translations through the
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
```

The `TextImpl` record accepts three string parameters: `locale`, `text`, and `value`. The `value`
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


## Implementations

The `message-format-annotations` module contains only the annotation definitions and the abstract
base class. To actually scan class files, you need a concrete implementation:

The [ASM Annotation Adopter](asm.md) uses the standalone ASM bytecode library
(`org.ow2.asm:asm`) to parse class files. It is the right choice for non-Spring applications or
build-time tooling.

The [Spring Annotation Adopter](spring.md) uses the ASM copy bundled with Spring Framework. If
your application already depends on Spring, this implementation avoids adding an extra ASM
dependency to your project.

Both implementations share the same discovery strategies, deduplication behavior, and error
handling described on this page. They differ only in the bytecode library they use to read
annotation metadata from class files.
