---
icon: simple/spring
---

# Spring

The Spring add-on integrates the message format library with the
[Spring Framework](https://spring.io/projects/spring-framework). It is published as the Maven artifact
`message-format-spring` and supports Spring Framework versions from 6.0.8 up to, but not including, 7.1.

The add-on contains the following independent parts.

[Message Source](message-source.md) describes `MessageSupportMessageSource`, an implementation of Spring's
`MessageSource` interface that delegates to a `MessageSupport` instance. Any Spring component that resolves messages
through `MessageSource`, such as Thymeleaf templates or validation error messages, can use the message format syntax
without changes on the consumer side.

[SpEL](../../formatter/parameter/named/spel.md) describes the named formatter `spel`. It evaluates Spring Expression
Language expressions against a parameter value before the result is inserted into the message.

[ResolvableType](../../formatter/parameter/typed/resolvable-type.md) and
[TypeDescriptor](../../formatter/parameter/typed/type-descriptor.md) describe the type-specific formatters for
Spring's representations of Java types. They format a `ResolvableType` or a `TypeDescriptor` like the Java type it
stands for, including its generic type arguments.

[DataSize](../../formatter/parameter/typed/data-size.md) describes the type-specific formatter for Spring's
`DataSize`, which formats an amount of data by its number of bytes.

Unlike the `spel` formatter, the three type-specific formatters are registered automatically by the
`DefaultFormatterService` when the module is present.


## Dependency

=== "Gradle (Groovy)"

    ```groovy
    dependencies {
      implementation 'de.sayayi.lib:message-format-spring:0.26.0'
    }
    ```

=== "Gradle (Kotlin)"

    ```kotlin
    dependencies {
      implementation("de.sayayi.lib:message-format-spring:0.26.0")
    }
    ```

=== "Maven"

    ```xml
    <dependency>
      <groupId>de.sayayi.lib</groupId>
      <artifactId>message-format-spring</artifactId>
      <version>0.26.0</version>
    </dependency>
    ```

On the module path, the module name is `de.sayayi.lib.message.spring`. It requires `de.sayayi.lib.message`,
`spring.core` and `spring.context` transitively, so a single `requires` statement is sufficient:

```java title="module-info.java"
module com.example.shop {
  requires de.sayayi.lib.message.spring;
}
```
