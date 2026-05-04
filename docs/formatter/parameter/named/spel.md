# SpEL

/// note
This formatter is **not** included in the core library. It is part of the `message-format-spring` module
and must be added as a dependency to use.

```
de.sayayi.lib:message-format-spring:<version>
```
///

The named formatter `spel` evaluates
[Spring Expression Language](https://docs.spring.io/spring-framework/reference/core/expressions.html) (SpEL)
expressions against a parameter value. This gives you the ability to extract, transform or navigate into complex
objects before the result is formatted and inserted into the message. You select it explicitly by writing
`format:spel` in the parameter configuration, but it can also be activated automatically when a `spel-expr`
or `spel-format` configuration key is present (see [Auto Application](#auto-application) below).

The formatter accepts any Java type as input. The parameter value becomes the *root object* of the SpEL evaluation
context, meaning the expression operates directly on that value. Properties, method calls and collection indexing
in the expression all resolve against the parameter value itself.


## Configuration Keys

The `spel` formatter recognizes two configuration keys.

### `spel-expr`

A SpEL expression string that is evaluated against the parameter value. The result of the evaluation
replaces the original parameter value for subsequent formatting.

When `spel-expr` is omitted, the formatter passes the parameter value through unchanged. This can be useful
when you only want to apply a specific format via `spel-format` without transforming the value.

### `spel-format`

An optional named format that is applied to the result after the expression has been evaluated. This works
exactly like specifying `format:<name>` for the result value. You can use any named formatter here, such as
`bool`, `choice` or `size`, to further interpret the extracted value.

When `spel-format` is omitted, the result of the SpEL expression is formatted using the default type-based
formatter selection.


## Basic Usage

Consider a `Map<String,Integer>` passed as a parameter. To extract a specific entry from the map, you can
write a SpEL expression that navigates the map's entry set.

```java
var map = new TreeMap<String, Integer>();
map.put("A", 0);
map.put("D", -8);
map.put("C", 34);

messageSupport
    .message("%{map,spel-expr:'entrySet().toArray()[1].value'}")
    .with("map", map)
    .format();
// "34"
```

The expression `entrySet().toArray()[1].value` calls `entrySet()` on the `TreeMap` root object, converts it
to an array, picks the second entry (index 1, which is key `"C"` because a `TreeMap` iterates in sorted order)
and reads its `value` property. The result is the integer `34`, which is then formatted as text.


## Combining Expression and Format

The real power of this formatter emerges when you combine `spel-expr` with `spel-format`. The expression
extracts a value, and the format interprets that value through a different named formatter.

```java
var map = new TreeMap<String, Integer>();
map.put("A", 0);
map.put("C", 34);
map.put("D", -8);

messageSupport
    .message(
        "%{map,format:spel,spel-expr:'entrySet().toArray()[0].value'," +
        "                  spel-format:bool,false:no,true:yes}")
    .with("map", map)
    .format();
// "no"
```

Here the expression extracts the value of the first entry (`"A"` → `0`). The `spel-format:bool` configuration
tells the formatter to pass the result through the `bool` formatter, which interprets `0` as `false`. The
`false:no` map key then produces the text "no".

The same technique works with the `choice` formatter for numeric range matching.

```java
messageSupport
    .message(
        "%{map,spel-expr:'entrySet().toArray()[2].value'," +
        "      spel-format:choice,<0:negative,0:zero,>0:positive}")
    .with("map", map)
    .format();
// "negative"
```

The third entry (`"D"` → `-8`) is extracted. The `choice` formatter receives `-8`, the `<0` map key matches,
and the output is "negative".


## Accessing Other Parameters as Variables

Inside a SpEL expression, you can reference other message parameters as SpEL variables using the `#variableName`
syntax. The evaluation context's `lookupVariable` method resolves variable references by looking up the
corresponding parameter value from the message context.

```java
messageSupport
    .message("%{order,spel-expr:'items.get(#index)'}")
    .with("order", order)
    .with("index", 0)
    .format();
// the first item from the order's items list
```

This allows expressions to be data-driven, with the extraction logic parameterized by other message values.

/// warning
Variable assignment is **not** supported. Any attempt to use `#var = value` in a SpEL expression throws a
`SpelEvaluationException`. The evaluation context is strictly read-only.
///


## Property Access

The SpEL evaluation context uses `DataBindingPropertyAccessor` in read-only mode. This means you can access
JavaBean properties on the root object using the standard dot notation.

```java
messageSupport
    .message("Name: %{person,spel-expr:'firstName'}")
    .with("person", new Person("Alice", "Smith"))
    .format();
// "Name: Alice"
```

```java
messageSupport
    .message("%{person,spel-expr:'firstName.length()'}")
    .with("person", new Person("Alice", "Smith"))
    .format();
// "5"
```

Method calls are resolved through reflection. You can invoke any public method on the root object or on
intermediate results returned by the expression.

```java
messageSupport
    .message("%{text,spel-expr:'toUpperCase()'}")
    .with("text", "hello")
    .format();
// "HELLO"
```

```java
messageSupport
    .message("%{names,spel-expr:'get(0).toUpperCase()'}")
    .with("names", List.of("alice", "bob", "charlie"))
    .format();
// "ALICE"
```


## Auto Application

The `spel` formatter supports automatic application. When the formatter service encounters a parameter
configuration that contains one of the formatter's configuration keys (`spel-expr` or `spel-format`) but
no explicit `format:spel`, it automatically selects the `spel` formatter. This means you do not need to
write `format:spel` every time you use `spel-expr`.

The following two message format strings are equivalent:

```java
// with explicit format selection
messageSupport
    .message("%{map,format:spel,spel-expr:'size()'}")
    .with("map", Map.of("a", 1, "b", 2))
    .format();
// "2"

// with auto application (spel-expr triggers the spel formatter automatically)
messageSupport
    .message("%{map,spel-expr:'size()'}")
    .with("map", Map.of("a", 1, "b", 2))
    .format();
// "2"
```

Auto application makes the syntax more concise when the presence of a `spel-expr` key already makes it
clear that SpEL evaluation is intended.


## Security Considerations

/// warning
SpEL is a powerful expression language that can invoke arbitrary methods on objects. Constructors are
**not** available in the evaluation context (the constructor resolver list is empty), and bean resolution
is disabled, which limits some attack vectors. However, the expression can still call any public method
on the root object and on objects reachable from it.

Never allow untrusted user input to be used as a SpEL expression string. The `spel-expr` value should
always be a fixed string defined by the developer in the message format, never a value derived from
external input.
///


## Registration

Before the `spel` formatter can be used, it must be registered with the formatter service. The
`SpELFormatter` class provides three constructors for different levels of Spring integration.

### Default Constructor

The simplest option creates a formatter with default type conversion, a default type locator and no
special class loader configuration.

```java
var formatterService = new DefaultFormatterService();
formatterService.addFormatter(new SpELFormatter());
```

### ConversionService and ResourceLoader

In a Spring application you typically have a `ConversionService` and a `ResourceLoader` (often the
`ApplicationContext` itself) available. This constructor configures the SpEL parser with the application's
class loader and wires the type converter to Spring's conversion infrastructure.

```java
@Bean
public SpELFormatter spelFormatter(ConversionService conversionService,
                                   ResourceLoader resourceLoader) {
    return new SpELFormatter(conversionService, resourceLoader);
}
```

### ConversionService and ClassLoader

When you need a specific class loader but do not have a `ResourceLoader`, you can pass the class loader
directly.

```java
var formatterService = new DefaultFormatterService();
formatterService.addFormatter(
    new SpELFormatter(new DefaultConversionService(),
                      MyApp.class.getClassLoader()));
```

The `ConversionService` is used by the SpEL `TypeConverter` to convert values during expression evaluation.
The `ClassLoader` is used by the `TypeLocator` to resolve type references in expressions and by the
`SpelParserConfiguration` to locate classes at parse time.


## Null Handling

When the parameter value is `null`, the SpEL expression is not evaluated. You can provide a `null` map key
to produce specific output for that case.

```java
messageSupport
    .message("%{data,spel-expr:'toString()',null:'no data'}")
    .with("data", null)
    .format();
// "no data"
```
