---
icon: material/home-outline
---

# Message Format User Manual

**message-format** is a type-safe, locale-aware message formatting library for Java. It parses message format strings
into reusable `Message` objects and formats them with named parameters, locale selection and extensible formatters. 
The goal is to produce text that reads like a proper sentence rather than a mechanical concatenation of placeholder 
values.


## Getting Started

The library requires Java 21 or later. The core module `message-format` is published to Maven Central under the group
`de.sayayi.lib`. It brings the ANTLR 4 runtime and two small support libraries of the same author
(`antlr4-runtime-ext` and `pack`) as transitive dependencies.

=== "Gradle (Groovy)"

    ```groovy
    dependencies {
      implementation 'de.sayayi.lib:message-format:0.25.0'
    }
    ```

=== "Gradle (Kotlin)"

    ```kotlin
    dependencies {
      implementation("de.sayayi.lib:message-format:0.25.0")
    }
    ```

=== "Maven"

    ```xml
    <dependency>
      <groupId>de.sayayi.lib</groupId>
      <artifactId>message-format</artifactId>
      <version>0.25.0</version>
    </dependency>
    ```

On the module path, the module name is `de.sayayi.lib.message`. It requires the modules of its dependencies
transitively, so a single `requires` statement is sufficient:

```java title="module-info.java"
module com.example.shop {
  requires de.sayayi.lib.message;
}
```

The add-on modules `message-format-annotations`, `message-format-icu`, `message-format-log4j` and
`message-format-spring` and the Gradle plugin are optional and are described in their own sections.

The following program registers a message under a code and formats it. `MessageSupportFactory.createDefault()` returns
a message support that knows all built-in formatters, so numbers, dates, collections and other common types are
formatted without any further setup. The `choice` formatter selects the text that matches the number of items.

```java title="Main.java"
import de.sayayi.lib.message.MessageSupportFactory;
import java.util.Locale;

public class Main
{
  public static void main(String[] args)
  {
    var messageSupport = MessageSupportFactory.createDefault();

    messageSupport.addMessage("cart.summary",
        "%{customer} has %{items,format:choice,0:'no items'," +
        "1:'one item',:'%{items} items'} in the cart.");

    String text = messageSupport
        .code("cart.summary")
        .with("customer", "Alice")
        .with("items", 3)
        .locale(Locale.US)
        .format();

    // prints "Alice has 3 items in the cart."
    System.out.println(text);
  }
}
```


## Content of the Manual

The [Message](message/index.md) section is the starting point. It introduces the format syntax and explains how to turn
a format string into a formatted result through the formatting API.

[Configuration](configuration/index.md) builds on that foundation and describes the central components that drive the 
formatting pipeline. It covers `MessageSupport` and `MessageFactory`, the formatter service, default configuration 
values, pack files for precompiled message bundles and how the library handles exceptions.

The library ships with a wide range of built-in [Formatters](formatter/index.md).
[Type-specific](formatter/parameter/typed/index.md) formatters handle common Java types such as numbers, dates, enums 
and collections. [Named](formatter/parameter/named/index.md) formatters apply explicit formatting logic like choice 
selection or geographic coordinates. [Post formatters](formatter/post/index.md) operate on already-formatted output, 
for example by changing its case or clipping it to a maximum length. If the built-in set does not cover a given need, 
the [development guide](formatter/development/index.md) walks through writing and registering custom formatters.

[Adopters](adopter/index.md) covers the different ways to populate a `MessageSupport` instance with messages and 
templates. Messages can be declared through [annotations](adopter/annotation/index.md) and discovered from compiled 
class files using several strategies, from classpath scanning to registering individual classes or annotation instances.
Alternatively, messages can be loaded from [properties files](adopter/properties/index.md) or standard Java
[resource bundles](adopter/resource-bundle/index.md).

The [Add-Ons](add-on/index.md) section describes integration with external frameworks. [Apache Log4j2](add-on/log4j.md) integration 
replaces Log4j's default message formatting with the message format syntax. The [Spring Framework](add-on/spring/message-source.md) integration
provides a `MessageSource` backed by the message format engine.

The [Gradle Plugin](gradle-plugin/index.md) automates annotation scanning and pack file generation at build time, so messages are 
precompiled and ready to use without runtime classpath scanning.
