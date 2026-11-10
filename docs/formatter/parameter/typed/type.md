# Type

This formatter is included in the `DefaultFormatterService`.

The `TypeFormatter` is a type-based formatter registered for `java.lang.reflect.Type` (which includes `Class`). It is
automatically selected whenever a parameter value is a `Type` or `Class` instance. The formatter renders the type as a
human-readable string, handling classes, generic types, parameterized types, type variables, wildcard types and arrays.

The `type` configuration key controls which simplifications are applied to the output. It accepts a string of format 
flags:

| Flag | Effect                                      |
|------|---------------------------------------------|
| `c`  | Use the simple class name only (no package) |
| `j`  | Strip the `java.lang.` package prefix       |
| `u`  | Strip the `java.util.` package prefix       |
| `v`  | Expand type variable bounds                 |

The default format is `ju`, which strips both the `java.lang.` and `java.util.` package prefixes while keeping all 
other packages fully qualified.


## Basic Output

Without any configuration, the formatter uses the default `ju` format. Primitive types always use their simple name 
regardless of the format flags.

The examples on this page use the following message support.

```java title="Example setup"
import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.parameter.runtime.TypeFormatter;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

var messageSupport = MessageSupportFactory.createDefault();
// all built-in formatters are available
```

```java
messageSupport
    .message("%{t}")
    .with("t", String.class)
    .format();
// "String"

messageSupport
    .message("%{t}")
    .with("t", int.class)
    .format();
// "int"

messageSupport
    .message("%{t}")
    .with("t", java.lang.reflect.Method.class)
    .format();
// "reflect.Method"
```

The `java.lang.` prefix is stripped from `String` and `reflect.Method` by the default `j` flag. The `reflect`
sub-package is preserved because only the `java.lang.` prefix itself is removed.


## Format Flags

### Simple Class Name (`c`)

The `c` flag reduces any class to its simple name, discarding the package entirely.

```java
messageSupport
    .message("%{t,type:c}")
    .with("t", java.lang.reflect.Method.class)
    .format();
// "Method"

messageSupport
    .message("%{t,type:c}")
    .with("t", TypeFormatter.class)
    .format();
// "TypeFormatter"
```

### Strip `java.lang.` (`j`)

The `j` flag removes the `java.lang.` prefix. Classes in sub-packages of `java.lang` keep the sub-package portion.

```java
messageSupport
    .message("%{t,type:j}")
    .with("t", String.class)
    .format();
// "String"

messageSupport
    .message("%{t,type:j}")
    .with("t", java.lang.reflect.Method.class)
    .format();
// "reflect.Method"
```

### Strip `java.util.` (`u`)

The `u` flag removes the `java.util.` prefix.

```java
messageSupport
    .message("%{t,type:u}")
    .with("t", java.util.Map.class)
    .format();
// "Map"
```

### Fully Qualified (no flags)

When no flags are set, all packages are shown in full.

```java
messageSupport
    .message("%{t,type:''}")
    .with("t", java.lang.reflect.Method.class)
    .format();
// "java.lang.reflect.Method"

messageSupport
    .message("%{t,type:''}")
    .with("t", java.util.Map.class)
    .format();
// "java.util.Map"
```


## Arrays

Array types are rendered with `[]` suffixes. Multi-dimensional arrays produce multiple pairs of brackets.

```java
messageSupport
    .message("%{t}")
    .with("t", int[].class)
    .format();
// "int[]"

messageSupport
    .message("%{t}")
    .with("t", long[][][].class)
    .format();
// "long[][][]"

messageSupport
    .message("%{t}")
    .with("t", String[].class)
    .format();
// "String[]"
```


## Parameterized Types

Parameterized types (generics) are displayed with their type arguments. The format flags apply to both the raw type and
the type arguments.

```java
// Map<K,V> unmodifiableMap(Map<? extends K,? extends V>)
var method = Collections.class.getMethod("unmodifiableMap", Map.class);

messageSupport
    .message("%{t,type:''}")
    .with("t", method.getGenericReturnType())
    .format();
// "java.util.Map<K, V>"

messageSupport
    .message("%{t}")
    .with("t", method.getGenericReturnType())
    .format();
// "Map<K, V>"
```


## Wildcard Types

Wildcard types are rendered with their bounds. An unbounded wildcard is shown as `?`. Upper bounds produce
`? extends ...` and lower bounds produce `? super ...`.

```java
// void copy(List<? super T>, List<? extends T>)
var copy =
    Collections.class.getMethod("copy", List.class, List.class);

messageSupport
    .message("%{t}")
    .with("t", copy.getGenericParameterTypes()[0])
    .format();
// "List<? super T>"

// void shuffle(List<?>)
var shuffle = Collections.class.getMethod("shuffle", List.class);

messageSupport
    .message("%{t}")
    .with("t", shuffle.getGenericParameterTypes()[0])
    .format();
// "List<?>"
```


## Type Variable Bounds (`v`)

By default, type variables are shown as their name only (e.g. `T`). The `v` flag expands the bounds of type variables,
showing the full constraint.

```java
interface Samples
{
  <T extends Iterable<String> & Enumeration<String>> void accept(T t);

  Optional<? extends Number>[] values();
}

// the erasure of T is its first bound, Iterable
var accept = Samples.class.getMethod("accept", Iterable.class);

messageSupport
    .message("%{t}")
    .with("t", accept.getGenericParameterTypes()[0])
    .format();
// "T"

messageSupport
    .message("%{t,type:juv}")
    .with("t", accept.getGenericParameterTypes()[0])
    .format();
// "<T extends Iterable<String> & Enumeration<String>>"
```


## Generic Arrays

Generic array types combine the generic element type with `[]` suffixes.

The `Samples` interface declared above has a method with a generic array return type.

```java
var values = Samples.class.getMethod("values");

messageSupport
    .message("%{t}")
    .with("t", values.getGenericReturnType())
    .format();
// "Optional<? extends Number>[]"

messageSupport
    .message("%{t,type:''}")
    .with("t", values.getGenericReturnType())
    .format();
// "java.util.Optional<? extends java.lang.Number>[]"
```


## Null Handling

A `null` parameter value produces an empty string by default. A `null` map key can be provided to produce specific text.

```java
messageSupport
    .message("%{t,null:'unknown type'}")
    .with("t", null)
    .format();
// "unknown type"
```
