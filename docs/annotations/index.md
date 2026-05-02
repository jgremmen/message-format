# Annotations

When working with a growing number of messages and templates in a project, maintaining them in a
central configuration file or programmatic setup code can become unwieldy. The
`message-format-annotations` module solves this problem by letting you declare messages and
templates directly in your Java source code using annotations. Because the annotations are placed
close to the code that uses them, they are easy to discover, review and maintain.

All annotations in this module use `RetentionPolicy.CLASS`. This means they are written into the
compiled `.class` files by the Java compiler, but they are not available at runtime through
reflection. Instead, they are read from the bytecode by an annotation adopter, which parses the
class files and publishes the discovered messages and templates to a `MessageSupport` instance.
The adopter implementations are provided by the [ASM](../asm/index.md) and
[Spring](../spring/index.md) modules.


## `@MessageDef`

The `@MessageDef` annotation defines a message with a unique code. It can be placed on types,
methods and other annotation types. The `code` attribute identifies the message within the scope
of a `MessageSupport` instance. No two messages may share the same code within that scope.

The simplest form uses the `text` attribute to provide a single, non-localized message format
string:

```java
@MessageDef(code = "file-saved", text = "File %{filename} has been saved.")
public class FileMessages {}
```

When this class is adopted, the message with code `file-saved` becomes available for formatting.
The parameter `%{filename}` is replaced with whatever value is provided at format time:

```java
adopter.adopt(FileMessages.class);

messageSupport
    .code("file-saved")
    .with("filename", "report.pdf")
    .format();
// "File report.pdf has been saved."
```

### Localized Messages

For applications that need to support multiple languages, the `texts` attribute accepts an array
of `@Text` annotations, each carrying a locale and the corresponding translation. Locales are
specified as language tags such as `"en"`, `"de"` or `"fr-CA"`.

```java
@MessageDef(code = "welcome", texts = {
    @Text(locale = "en", text = "Welcome, %{name}!"),
    @Text(locale = "de", text = "Willkommen, %{name}!"),
    @Text(locale = "fr", text = "Bienvenue, %{name}!")
})
public class GreetingMessages {}
```

At format time, the library selects the text that best matches the requested locale:

```java
adopter.adopt(GreetingMessages.class);

messageSupport
    .code("welcome")
    .with("name", "Alice")
    .locale(Locale.GERMAN)
    .format();
// "Willkommen, Alice!"
```

### Repeatable Annotations

`@MessageDef` is repeatable, which means you can define multiple messages on the same class or
method without needing a wrapper annotation. This is convenient for grouping related messages
together:

```java
@MessageDef(code = "login-ok", text = "Login successful for %{user}.")
@MessageDef(code = "login-fail", text = "Login failed for %{user}: %{reason}")
@MessageDef(code = "logout", text = "%{user} has logged out.")
public class AuthMessages {}
```

The compiler automatically wraps multiple `@MessageDef` annotations into a `@MessageDefs`
container. You never need to use the container annotation directly.


### Placement on Methods

Annotations are not limited to type declarations. Placing a `@MessageDef` on a method can help
associate the message with the code that produces it, making the relationship explicit:

```java
public class OrderService {

    @MessageDef(code = "order-placed",
        text = "Order %{orderId} placed with %{itemCount,1:'1 item',:'%{itemCount} items'}.")
    public void placeOrder(Order order) {
        // ...
    }
}
```


## `@TemplateDef`

Templates are reusable message fragments that can be referenced from any message using the
`%[template-name]` syntax. The `@TemplateDef` annotation declares such a template with a unique
name. Like `@MessageDef`, it can be placed on types, methods and annotation types, and it is
repeatable.

The `text` attribute provides a single, non-localized template body:

```java
@TemplateDef(name = "opt-detail", text = "%{detail,!empty:' (%{detail})'}")
public class CommonTemplates {}
```

Once adopted, this template can be referenced from any message registered in the same
`MessageSupport` instance:

```java
adopter.adopt(CommonTemplates.class);

messageSupport
    .message("Task completed%[opt-detail]")
    .with("detail", "3 warnings")
    .format();
// "Task completed (3 warnings)"

messageSupport
    .message("Task completed%[opt-detail]")
    .with("detail", "")
    .format();
// "Task completed"
```

The real advantage of templates becomes apparent when the same fragment appears in multiple
messages. Instead of repeating the conditional logic in every message, you define it once in a
template and reference it wherever needed.

### Localized Templates

Just like messages, templates support localized variants through the `texts` attribute:

```java
@TemplateDef(name = "item-count", texts = {
    @Text(locale = "en",
          text = "%{n,0:'no items',1:'one item',:'%{n} items'}"),
    @Text(locale = "de",
          text = "%{n,0:'keine Einträge',1:'ein Eintrag',:'%{n} Einträge'}")
})
public class SharedTemplates {}
```

```java
adopter.adopt(SharedTemplates.class);

messageSupport
    .message("Cart contains %[item-count].")
    .with("n", 5)
    .locale(Locale.ENGLISH)
    .format();
// "Cart contains 5 items."

messageSupport
    .message("Cart contains %[item-count].")
    .with("n", 1)
    .locale(Locale.GERMAN)
    .format();
// "Cart contains ein Eintrag."
```

### Combining Messages and Templates

A single class can carry both `@MessageDef` and `@TemplateDef` annotations. This is useful when
a set of messages and the templates they reference are logically related:

```java
@TemplateDef(name = "currency",
    text = "%{amount,format:number,number:'#,##0.00'}")
@MessageDef(code = "invoice-total",
    text = "Total: %[currency] %{currencyCode}")
@MessageDef(code = "invoice-tax",
    text = "Tax: %[currency] %{currencyCode}")
public class InvoiceMessages {}
```

```java
adopter.adopt(InvoiceMessages.class);

messageSupport
    .code("invoice-total")
    .with("amount", 1234.5)
    .with("currencyCode", "EUR")
    .locale(Locale.GERMANY)
    .format();
// "Total: 1.234,50 EUR"
```


## `@Text`

The `@Text` annotation represents a single locale-specific text within a `@MessageDef` or
`@TemplateDef`. It has three attributes.

The `locale` attribute is a language tag such as `"en"` or `"de-DE"`. When left empty (the
default), it corresponds to `Locale.ROOT`, which acts as a fallback that matches any locale for
which no more specific text exists.

The `text` attribute holds the localized message format string. It is used together with `locale`
to provide a translation for a specific language.

The `value` attribute is a shorthand for non-localized text. It is only evaluated when both
`locale` and `text` are empty, which enables the compact form `@Text("message text")`. This
shorthand is useful when a `@MessageDef` or `@TemplateDef` carries a single `texts` entry that
does not target any particular locale:

```java
@MessageDef(code = "simple", texts = @Text("A simple message."))
public class SimpleMessages {}
```

This is equivalent to:

```java
@MessageDef(code = "simple", text = "A simple message.")
public class SimpleMessages {}
```

In practice, the `text` attribute on `@MessageDef` and `@TemplateDef` is the preferred shorthand
for single-text definitions like this.


## Module Coordinates

```
de.sayayi.lib:message-format-annotations:<version>
```

This module depends on the [core library](../core/index.md) (`message-format`). When using
Gradle, add the dependency as follows:

```groovy
dependencies {
    implementation 'de.sayayi.lib:message-format-annotations:<version>'
}
```

For Maven:

```xml
<dependency>
    <groupId>de.sayayi.lib</groupId>
    <artifactId>message-format-annotations</artifactId>
    <version><!-- version --></version>
</dependency>
```

To actually scan and adopt the annotations from compiled class files, you also need an adopter
implementation such as the one provided by the [ASM module](../asm/index.md). The annotations
module itself only contains the annotation definitions and the abstract base class for adopters.
