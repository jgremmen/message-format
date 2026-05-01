# Annotations

The `message-format-annotations` module provides compile-time annotations for declaring messages
and templates directly in Java source code. Annotations are retained at class level
(`RetentionPolicy.CLASS`) and can be scanned from compiled class files without loading classes.

## Annotations

### `@MessageDef`

Defines a message with a unique code. Can be placed on types, methods, or annotation types.
Repeatable via `@MessageDefs`.

```java
@MessageDef(code = "MSG-001", texts = {
    @Text(locale = "en", text = "Hello %{name}!"),
    @Text(locale = "de", text = "Hallo %{name}!")
})
```

A shorthand form is available for single-locale messages:

```java
@MessageDef(code = "MSG-002", text = "Goodbye %{name}!")
```

### `@TemplateDef`

Defines a template message with a unique name. Repeatable via `@TemplateDefs`.

```java
@TemplateDef(name = "ex-msg", text = "%{ex,!empty:': %{ex}'}")
```

### `@Text`

Specifies a locale and the corresponding message format text. Used inside `@MessageDef` and
`@TemplateDef`.

## Module Coordinates

```
de.sayayi.lib:message-format-annotations:<version>
```

