# List

This formatter is included in the `DefaultFormatterService`.

The library provides two formatters for rendering collections of elements as joined text: `ArrayFormatter` for
arrays and `IterableFormatter` for any `Iterable` (including all `Collection` types such as `List` and `Set`).
Both formatters share the same underlying logic: each element is formatted individually and the results are
joined into a single text string. Separator, truncation and overflow behavior are controlled by a set of
configuration keys.

Elements that format to an empty string are silently omitted from the output.


## ArrayFormatter

The `ArrayFormatter` handles all Java array types: primitive arrays (`int[]`, `long[]`, `boolean[]`, `byte[]`,
`short[]`, `float[]`, `double[]`), object arrays (`Object[]`, `String[]`, etc.) and the atomic array types
`AtomicIntegerArray`, `AtomicLongArray` and `AtomicReferenceArray`. It is automatically selected whenever a
parameter value is an array.

```java
messageSupport
    .message("%{items}")
    .with("items", new String[] { "apple", "banana", "cherry" })
    .format();
// "apple, banana, cherry"
```

```java
messageSupport
    .message("%{numbers}")
    .with("numbers", new int[] { 1, 2, 3 })
    .format();
// "1, 2, 3"
```

Each element is formatted using the formatter appropriate for its type. For primitive arrays, the corresponding
primitive type formatter is used. For object arrays, each element is formatted according to its own runtime type.

```java
messageSupport
    .message("%{values}")
    .with("values", new Object[] { "hello", true, 42 })
    .format();
// "hello, true, 42"
```


## IterableFormatter

The `IterableFormatter` handles any `java.lang.Iterable` value, which includes all `Collection` types such as
`List`, `Set`, `Queue` and any custom iterable. It is automatically selected whenever a parameter value implements
`Iterable`.

```java
messageSupport
    .message("%{names}")
    .with("names", List.of("Alice", "Bob", "Charlie"))
    .format();
// "Alice, Bob, Charlie"
```

```java
messageSupport
    .message("%{ids}")
    .with("ids", Set.of(1, 2, 3))
    .format();
// "1, 2, 3"
```


## Configuration Keys

Both formatters support the same set of configuration keys that control how the list is rendered.

### `list-sep`

The separator inserted between elements. Defaults to `", "` (comma followed by a space).

```java
messageSupport
    .message("%{items,list-sep:'.'}")
    .with("items", new int[] { 1, 2, 3, 4, 5 })
    .format();
// "1.2.3.4.5"
```

```java
messageSupport
    .message("%{items,list-sep:' | '}")
    .with("items", List.of("A", "B", "C"))
    .format();
// "A | B | C"
```

### `list-sep-last`

The separator inserted before the last element instead of the regular separator. This is useful for producing
natural-language lists like "A, B and C". If not set, the regular separator is used for all positions.

```java
messageSupport
    .message("%{items,list-sep:', ',list-sep-last:' and '}")
    .with("items", new int[] { 1, 2, 3, 4, 5 })
    .format();
// "1, 2, 3, 4 and 5"
```

```java
messageSupport
    .message("%{colors,list-sep-last:' or '}")
    .with("colors", List.of("red", "green", "blue"))
    .format();
// "red, green or blue"
```

### `list-max-size`

The maximum number of elements to include in the output. Elements beyond this limit are omitted. When truncation
occurs, the `list-sep-last` separator is used before the last included element (if `list-value-more` is not set).

```java
messageSupport
    .message("%{items,list-max-size:2}")
    .with("items", new String[] { "A", "B", "C", "D" })
    .format();
// "A, B"
```

```java
messageSupport
    .message("%{items,list-sep-last:' and ',list-max-size:2}")
    .with("items", new String[] { "A", "B", "C" })
    .format();
// "A and B"
```

Setting `list-max-size` to `0` suppresses all elements entirely (unless `list-value-more` is set, in which case
only the overflow text is shown).

```java
messageSupport
    .message("%{items,list-max-size:0}")
    .with("items", new String[] { "A", "B", "C" })
    .format();
// ""

messageSupport
    .message("%{items,list-max-size:0,list-value-more:'...'}")
    .with("items", new String[] { "A", "B", "C" })
    .format();
// "..."
```

### `list-value-more`

The text appended when the list is truncated due to `list-max-size`. This text is treated as an additional
element and separated from the preceding elements by the regular separator.

```java
messageSupport
    .message("%{items,list-max-size:2,list-value-more:'...'}")
    .with("items", new String[] { "A", "B", "C" })
    .format();
// "A, B, ..."
```

When `list-value-more` is set, the `list-sep-last` separator is not used. The regular separator is used before
the overflow text.

### `list-value`

A message format used to format each individual element. The element is available as the parameter `value` inside
this message. Defaults to `%{value}` which simply formats the element using its own type's formatter.

```java
messageSupport
    .message("%{flags,list-value:'%{value,true:YES,false:NO}'}")
    .with("flags", new boolean[] { true, false, true })
    .format();
// "YES, NO, YES"
```

```java
messageSupport
    .message("%{numbers,list-value:'%{value,number:\"0000\"}'}")
    .with("numbers", new int[] { 1, -7, 248 })
    .format();
// "0001, -0007, 0248"
```

This is a powerful mechanism that allows you to apply any formatting, map keys or configuration to each element
independently.

### `list-unique`

When set to `true`, duplicate element texts are suppressed. Only the first occurrence of each formatted text is
included in the output.

```java
messageSupport
    .message("%{tags,list-unique:true}")
    .with("tags", new String[] { "java", "kotlin", "java", "scala", "kotlin" })
    .format();
// "java, kotlin, scala"
```

### `list-this`

The text to output when an element refers to the collection itself (self-reference). Defaults to
`(this array)` for arrays and `(this collection)` for iterables.

```java
Object[] arr = new Object[2];
arr[0] = "hello";
arr[1] = arr;  // self-reference

messageSupport
    .message("%{items,list-this:'[...]'}")
    .with("items", arr)
    .format();
// "hello, [...]"
```


## Map Keys

### Empty Key

The `empty` key matches when the array or iterable contains no elements (length zero or no elements in the
iterator). Its negated form `!empty` matches when there is at least one element.

```java
messageSupport
    .message("%{items,empty:'no items',!empty:'has items'}")
    .with("items", new int[0])
    .format();
// "no items"
```

```java
messageSupport
    .message("%{items,empty:'no items',!empty:'has items'}")
    .with("items", Set.of())
    .format();
// "no items"
```

### Null Key

The `null` key matches when the parameter value is `null`.

```java
messageSupport
    .message("%{items,null:'null',empty:'empty'}")
    .with("items", null)
    .format();
// "null"

messageSupport
    .message("%{items,null:'null',empty:'empty'}")
    .with("items", new int[0])
    .format();
// "empty"
```


## Size Queries

Both formatters report the number of elements as their size. For arrays this is the array length. For collections
this uses `Collection.size()`. For other iterables the elements are counted by iteration.


## Interaction Table

The following table illustrates how the configuration keys interact with each other:

| Array       | `list-sep-last` | `list-max-size` | `list-value-more` | Result         |
|-------------|-----------------|-----------------|--------------------| ---------------|
| []          | n/a             | 0               | n/a                | (empty)        |
| [A, B, C]   | `" and "`       | (not set)       | n/a                | `A, B and C`   |
| [A, B, C]   | (not set)       | 2               | `"..."`            | `A, B, ...`    |
| [A, B, C]   | `" and "`       | 2               | (not set)          | `A and B`      |
| [A, B, C]   | `" and "`       | 1               | (not set)          | `A`            |
| [A, B, C]   | (not set)       | 0               | (not set)          | (empty)        |
| [A, B, C]   | (not set)       | 0               | `"..."`            | `...`          |
| [A, B, C]   | (not set)       | (not set)       | n/a                | `A, B, C`      |
| [A, B, C]   | (not set)       | 2               | (not set)          | `A, B`         |
| [A, B, C]   | (not set)       | 1               | (not set)          | `A`            |
