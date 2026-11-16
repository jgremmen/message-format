# ResolvableType

/// note
This formatter is **not** included in the core library. It is part of the `message-format-spring` module, whose
coordinates are listed on the [Spring](../../../add-on/spring/index.md#dependency) page.
///

Spring describes generic Java types with `org.springframework.core.ResolvableType`, for example in its conversion and
dependency injection infrastructure. The `toString()` result of a `ResolvableType` is meant for debugging, so a
resolvable type that appears in a message, such as an error message about a failed conversion, needs a formatter that
renders it like the Java type it stands for.

The `ResolvableTypeFormatter` is a type-based formatter registered for `ResolvableType`. It formats the underlying
Java type of the resolvable type, as returned by `ResolvableType.getType()`, and delegates to the
[Type](type.md) formatter. The parameter configuration is passed on unchanged, so the `type` configuration key and its
format flags apply exactly as described on that page. The module declares the formatter as a service provider, so the
`DefaultFormatterService` registers it automatically as soon as the module is on the class path or the module path.

The examples on this page use the following message support.

```java title="Example setup"
import de.sayayi.lib.message.MessageSupportFactory;
import org.springframework.core.ResolvableType;

import java.time.LocalDate;
import java.util.Map;

var messageSupport = MessageSupportFactory.createDefault();
// all built-in formatters and the formatters for Spring types
```


## Basic Output

Without configuration, the default format `ju` of the type formatter is used, which strips the `java.lang.` and
`java.util.` package prefixes. The generic type arguments are part of the output:

```java
var mapType = ResolvableType.forClassWithGenerics(
    Map.class, String.class, LocalDate.class);

messageSupport
    .message("%{t}")
    .with("t", mapType)
    .format();
// "Map<String, java.time.LocalDate>"
```


## The `type` Configuration Key

The format flags of the [`type`](type.md) configuration key select which package prefixes are removed. The flag `c`
reduces every class to its simple name, and an empty value keeps all names fully qualified:

```java
messageSupport
    .message("%{t,type:c} / %{t,type:''}")
    .with("t", mapType)
    .format();
// "Map<String, LocalDate> /
//  java.util.Map<java.lang.String, java.time.LocalDate>"
```


## Unresolved Type Variables

The underlying type is formatted as it is declared. A type variable is not replaced by the type it is bound to in the
context of the resolvable type. A resolvable type for a field of type `T`, created for a subclass that binds `T` to
`LocalDate`, is therefore formatted as `T`:

```java
public class Box<T>
{
  public T content;
}

public class DateBox extends Box<LocalDate> {}

var contentType = ResolvableType.forField(
    Box.class.getField("content"), DateBox.class);

messageSupport
    .message("%{t}")
    .with("t", contentType)
    .format();
// "T"
```

To format the resolved type, resolve it before passing it to the message, for example with
`contentType.resolve()`, which returns the class `LocalDate`.


## Map Keys

`ResolvableType.NONE` represents the absence of a type. It has no underlying type, so it is formatted as empty text and
matches the `empty` key. Every other resolvable type matches `!empty`:

```java
messageSupport
    .message("Target type: %{t,empty:'unknown'}")
    .with("t", ResolvableType.NONE)
    .format();
// "Target type: unknown"

messageSupport
    .message("%{t,empty:'no type',!empty:'type %{t}'}")
    .with("t", ResolvableType.forClass(String.class))
    .format();
// "type String"
```

The `null` key and the handling of `null` values follow the
[general rules](../index.md#null-values-and-empty-results) for parameter formatters.
