# TypeDescriptor

/// note
This formatter is **not** included in the core library. It is part of the `message-format-spring` module, whose
coordinates are listed on the [Spring](../../../add-on/spring/index.md#dependency) page.
///

Spring's type conversion describes source and target types with `org.springframework.core.convert.TypeDescriptor`. A
type descriptor carries the type of a value together with the element types of collections and maps and the
annotations of the field or method parameter it was created for. Messages about failed conversions or validation
errors often refer to such a descriptor, and its `toString()` result includes the annotations, which rarely belong in a
user-facing message.

The `TypeDescriptorFormatter` is a type-based formatter registered for `TypeDescriptor`. It converts the descriptor to
the Java type it describes, including its generic type arguments, and delegates to the [Type](type.md) formatter.
Annotations of the descriptor are not formatted. The parameter configuration is passed on unchanged, so the `type`
configuration key and its format flags apply exactly as described on that page. The module declares the formatter as
a service provider, so the `DefaultFormatterService` registers it automatically as soon as the module is on the class
path or the module path.

The examples on this page use the following message support.

```java title="Example setup"
import de.sayayi.lib.message.MessageSupportFactory;
import org.springframework.core.convert.TypeDescriptor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

var messageSupport = MessageSupportFactory.createDefault();
// all built-in formatters and the formatters for Spring types
```


## Basic Output

Without configuration, the default format `ju` of the type formatter is used, which strips the `java.lang.` and
`java.util.` package prefixes. A descriptor for a plain class is formatted as the class name:

```java
messageSupport
    .message("Expected a value of type %{t}.")
    .with("t", TypeDescriptor.valueOf(String.class))
    .format();
// "Expected a value of type String."
```

The element types of collection and map descriptors are formatted as type arguments:

```java
var listOfDates = TypeDescriptor.collection(List.class,
    TypeDescriptor.valueOf(LocalDate.class));

messageSupport
    .message("%{t}")
    .with("t", listOfDates)
    .format();
// "List<java.time.LocalDate>"

var scores = TypeDescriptor.map(Map.class,
    TypeDescriptor.valueOf(String.class),
    TypeDescriptor.valueOf(Integer.class));

messageSupport
    .message("%{t}")
    .with("t", scores)
    .format();
// "Map<String, Integer>"
```


## The `type` Configuration Key

The format flags of the [`type`](type.md) configuration key select which package prefixes are removed. The flag `c`
reduces every class to its simple name, which also applies to the component type of an array:

```java
messageSupport
    .message("Cannot convert %{from,type:c} to %{to,type:c}.")
    .with("from", listOfDates)
    .with("to", TypeDescriptor.array(
        TypeDescriptor.valueOf(LocalDate.class)))
    .format();
// "Cannot convert List<LocalDate> to LocalDate[]."
```


## Map Keys

The formatter evaluates the `null` and `empty` keys according to the
[general rules](../index.md#null-values-and-empty-results) for parameter formatters. A type descriptor always
describes a type, so its formatted text is never empty.
