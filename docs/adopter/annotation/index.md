---
icon: material/at
toc_depth: 2
---

# Annotation Adopter

The annotation adopter reads `@MessageDef` and `@TemplateDef` annotations from compiled `.class` files and publishes 
the discovered messages and templates to a `MessageSupport` instance.

The `AnnotationAdopter` class provides multiple strategies for locating annotated classes. It tracks which classes have
already been processed to avoid duplicate registrations.


## Declaring Messages and Templates

Before the adopter can discover anything, the messages and templates have to be declared through annotations in the 
source code. `@MessageDef` defines a message that is later retrieved by its code and `@TemplateDef` defines a reusable 
message fragment that is referenced by name from other messages.

### Where the Annotations Go

Both `@MessageDef` and `@TemplateDef` may be placed on a type, a method or a constructor. The annotated element is only 
a carrier for the declaration. The message or template has no relationship to what the class, method or constructor 
actually does. Declarations may therefore be grouped wherever they read best, for example on a dedicated holder class 
that contains nothing but annotations, or directly on the method or constructor whose behavior a message describes.

The following holder class keeps all authentication messages in one place. Because both annotations are repeatable, 
any number of them may be stacked on a single element.

```java
@MessageDef(code = "auth.login-failed", text = "Login failed for %{user}.")
@MessageDef(code = "auth.locked", text = "Account %{user} is locked.")
public final class AuthMessages {}
// Declares two messages, retrieved later by code "auth.login-failed" 
// and "auth.locked"
```

Placing a declaration on a method or constructor is useful when a message belongs conceptually to a specific operation. 
The method body remains untouched.

```java
public class OrderService
{
  @MessageDef(code = "order.shipped", text = "Order %{id} has shipped.")
  public void ship(String id) {
    // business logic
  }

  @MessageDef(code = "order.invalid-state", 
              text = "Order %{id} is in an invalid state.")
  public OrderService(String id) {
    // initialization logic
  }
}
// Declares "order.shipped" on a method and "order.invalid-state" on 
// a constructor
```

### Messages Without a Locale

When a message has only one text and no localization is required, use the `text` element of `@MessageDef`. It is a
shorthand for a single `@Text` without a locale. A message declared this way matches every locale used for formatting.

```java
@MessageDef(code = "app.name", text = "Message Format Library")
public final class BrandingMessages {}

messageSupport
    .code("app.name")
    .format();
// "Message Format Library"
```

The equivalent long form assigns a single `@Text` to the `texts` element and relies on its value shorthand. Both 
declarations produce the same locale independent message, so prefer the compact `text` form for this case.

```java
@MessageDef(code = "app.name", texts = @Text("Message Format Library"))
public final class BrandingMessages {}
// Identical in effect to the text = "..." form above
```

The same shorthand exists for templates. A locale independent template uses the `text` element of `@TemplateDef`. 
The template below appends an error detail only when one is present.

```java
@TemplateDef(name = "opt-error", text = "%{err,!empty:': %{err}'}")
public final class CommonTemplates {}

messageSupport
    .message("Operation failed%[opt-error]")
    .with("err", "disk full")
    .format();
// "Operation failed: disk full"
```

### Locale Dependent Messages

To provide translations, list several `@Text` entries in the `texts` element and give each one a `locale`. The locale 
is either a plain language code such as `en` or `de`, or a language and country combination such as `de_DE` or `fr_CA`.
Leave the `text` element unset when `texts` is used.

```java
@MessageDef(code = "greeting", texts = {
    @Text(locale = "en", text = "Hello %{name}!"),
    @Text(locale = "de", text = "Hallo %{name}!"),
    @Text(locale = "fr", text = "Bonjour %{name} !")
})
public final class GreetingMessages {}

messageSupport
    .code("greeting")
    .with("name", "Alice")
    .locale(Locale.GERMAN)
    .format();
// "Hallo Alice!"
```

A `@Text` whose `locale` is omitted corresponds to `Locale.ROOT`. It acts as the fallback that matches any locale for 
which no dedicated translation exists. Combining a root text with specific translations provides a default plus targeted
overrides in a single declaration.

```java
@MessageDef(code = "farewell", texts = {
    @Text("Goodbye %{name}."),
    @Text(locale = "de", text = "Auf Wiedersehen %{name}.")
})
public final class FarewellMessages {}

messageSupport
    .code("farewell")
    .with("name", "Bob")
    .locale(Locale.FRENCH)
    .format();
// "Goodbye Bob." because no French text exists, so the root text applies
```

Localization becomes especially valuable together with the map key features of the message format, because plural rules 
differ between languages. The next example selects the correct wording for zero, one and many items independently per 
locale.

```java
@MessageDef(code = "cart.count", texts = {
    @Text(locale = "en", text = """
        %{n,format:choice,\
            0:'your cart is empty',\
            1:'1 item',\
             :'%{n} items'}\
        """),
    @Text(locale = "de", text = """
        %{n,format:choice,\
            0:'Ihr Warenkorb ist leer',\
            1:'1 Artikel',\
             :'%{n} Artikel'}\
        """)
})
public final class CartMessages {}

messageSupport
    .code("cart.count")
    .with("n", 3)
    .locale(Locale.GERMAN)
    .format();
// "3 Artikel"
```

Templates are localized in exactly the same way. Supply multiple `@Text` entries in the `texts` element of 
`@TemplateDef` to translate a shared fragment.

```java
@TemplateDef(name = "unit-days", texts = {
    @Text(locale = "en", 
          text = "%{d,format:choice,1:'1 day',:'%{d} days'}"),
    @Text(locale = "de", 
          text = "%{d,format:choice,1:'1 Tag',:'%{d} Tage'}")
})
public final class DurationTemplates {}

messageSupport
    .message("Delivery in %[unit-days].")
    .with("d", 1)
    .locale(Locale.ENGLISH)
    .format();
// "Delivery in 1 day."
```


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
names. This strategy is the most convenient when message definitions are spread across many classes within a known set 
of packages, because a single call processes everything:

```java
// Scan the entire com.example hierarchy
adopter.adopt(
    getClass().getClassLoader(),
    Set.of("com.example"));
```

### Single Class File

When the exact location of a class file on disk is known, its path can be provided directly. This is useful in build
tool integrations, Gradle tasks, or test setups where the output directory is known:

```java
adopter.adopt(Path.of(
    "build/classes/java/main/com/example/MyMessages.class"));
```

### Loaded Type

If the annotated class is already loaded in the JVM, its `Class` object can be passed. If the type has no class loader
(e.g. bootstrap classes), the call returns immediately without doing anything.

```java
adopter.adopt(MyMessages.class);
```


### Annotation Instances

The `adopt(MessageDef)` and `adopt(TemplateDef)` methods accept annotation instances directly. This is useful in
programmatic or testing scenarios where messages need to be registered without creating an annotated class. The 
`adopter.util` package provides the record implementations `SyntheticMessageDef`, `SyntheticTemplateDef` and 
`SyntheticText` for constructing these instances.

For a simple, non-localized message only the code and the text need to be provided. The convenience constructor creates
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


## Deduplication

The adopter tracks every class it has visited, identified by its classpath entry or fully qualified type name. When the
same class is encountered a second time, whether through a repeated `adopt` call, overlapping package scans, or a 
combination of different discovery strategies, it is silently skipped. This makes it safe to scan broad package 
hierarchies without worrying about duplicate processing:

```java
// These two calls overlap on com.example, but each class is processed 
// only once.
adopter.adopt(classLoader, Set.of("com.example"));
adopter.adopt(classLoader, Set.of("com.example.messages"));
```

Deduplication applies at the class level, not at the individual message or template level. For fine-grained 
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
    @Text(locale = "en", 
          text = "Order %{id} confirmed for %{customer}."),
    @Text(locale = "de", 
          text = "Bestellung %{id} bestätigt für %{customer}.")
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
      implementation 'de.sayayi.lib:message-format-annotations:0.24.0'
    }
    ```

=== "Maven"

    ```xml
    <dependency>
      <groupId>de.sayayi.lib</groupId>
      <artifactId>message-format-annotations</artifactId>
      <version>0.24.0</version>
    </dependency>
    ```
