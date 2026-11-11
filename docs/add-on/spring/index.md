---
icon: simple/spring
---

# Spring

The Spring add-on integrates the message format library with the
[Spring Framework](https://spring.io/projects/spring-framework). It is published as the Maven artifact
`message-format-spring` and supports Spring Framework versions from 6.0.8 up to, but not including, 7.1.

The add-on contains two independent parts.

[Message Source](message-source.md) describes `MessageSupportMessageSource`, an implementation of Spring's
`MessageSource` interface that delegates to a `MessageSupport` instance. Any Spring component that resolves messages
through `MessageSource`, such as Thymeleaf templates or validation error messages, can use the message format syntax
without changes on the consumer side.

[SpEL](../../formatter/parameter/named/spel.md) describes the named formatter `spel`. It evaluates Spring Expression
Language expressions against a parameter value before the result is inserted into the message.
