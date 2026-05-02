# Size

This formatter is included in the `DefaultFormatterService`.

The named formatter `size` is selected explicitly by writing `format:size` in the message parameter configuration.
Its purpose is to determine the size of a parameter value, such as the length of a string, the number of elements
in a collection or array, or the number of entries in a map. The size is returned as a numeric value that can be
matched against number map keys to produce custom text, or formatted as a plain number when no map keys are provided.

The formatter works with any type that has a size-aware (`SizeQueryable`) type-based formatter registered. This
includes strings, collections, iterables, maps, arrays, byte arrays, paths (file size in bytes), optionals,
references and suppliers. If the value's type does not support size queries, the formatter cannot determine the size
and the `empty` map key is used.


## How it Works

When the `size` formatter processes a parameter value, it delegates the size calculation to the type-based formatter
registered for the value's type. Each type defines what "size" means:

| Type                                  | Size meaning                                                                 |
|---------------------------------------|------------------------------------------------------------------------------|
| `CharSequence` (including `String`)   | Number of characters                                                         |
| `char[]`                              | Length of the array                                                          |
| `Collection` (including `List`, `Set`)| Number of elements                                                           |
| `Iterable`                            | Number of elements (counted by iterating)                                    |
| `Map`                                 | Number of entries                                                            |
| Arrays (all types)                    | Length of the array                                                          |
| `byte[]`                              | Length of the array                                                          |
| `Path`                                | File size in bytes (for regular files)                                       |
| `Optional`                            | Delegates to the contained value                                             |
| `Reference`                           | Delegates to the referenced value                                            |
| `Supplier`                            | Delegates to the supplied value                                              |

```java
messageSupport
    .message("%{name,format:size}")
    .with("name", "Hello")
    .format();
// "5"

messageSupport
    .message("%{items,format:size}")
    .with("items", List.of("a", "b", "c"))
    .format();
// "3"

messageSupport
    .message("%{props,format:size}")
    .with("props", Map.of("x", 1, "y", 2))
    .format();
// "2"

messageSupport
    .message("%{data,format:size}")
    .with("data", new int[] {4, -45, 8, 1})
    .format();
// "4"
```


## Map Key Types

The `size` formatter supports the following map key types: `number`, `null`, `empty` and default.

### Number Keys

Because the computed size is a number, number map keys are the natural way to customize the output. They are matched
against the size value in the same way as the [number formatter](../typed/number.md) matches number keys, including
support for comparison operators.

```java
messageSupport
    .message("%{c,format:size,0:'empty',1:'singleton',:'multiple'}")
    .with("c", List.of())
    .format();
// "empty"

messageSupport
    .message("%{c,format:size,0:'empty',1:'singleton',:'multiple'}")
    .with("c", Map.of("a", "b"))
    .format();
// "singleton"

messageSupport
    .message("%{c,format:size,0:'empty',1:'singleton',:'multiple'}")
    .with("c", new int[] {4, -45, 8, 1})
    .format();
// "multiple"
```

Comparison operators allow expressing ranges.

```java
messageSupport
    .message("%{items,format:size,0:'none',<5:'a few',<20:'several',:'many'}")
    .with("items", List.of(1, 2, 3))
    .format();
// "a few"

messageSupport
    .message("%{items,format:size,0:'none',<5:'a few',<20:'several',:'many'}")
    .with("items", List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12))
    .format();
// "several"
```

### Null Key

The `null` key matches when the parameter value is `null`.

```java
messageSupport
    .message("%{items,format:size,null:'no list provided',0:'empty list',:'%{items,format:size} items'}")
    .with("items", null)
    .format();
// "no list provided"
```

### Empty Key

The `empty` key matches when the size cannot be determined. This happens when the value's type does not have a
size-aware formatter registered. For example, a plain `boolean` has no notion of size, so the `size` formatter
cannot compute one.

```java
messageSupport
    .message("%{val,format:size,empty:'unsupported'}")
    .with("val", true)
    .format();
// "unsupported"
```

When no `empty` map key is provided and the size cannot be determined, the output is an empty string.

```java
messageSupport
    .message("%{val,format:size}")
    .with("val", true)
    .format();
// ""
```

### Default Key

A default map key (`:`) catches any size that does not match a specific number key. When combined with number keys
it acts as a fallback for unmatched sizes.

```java
messageSupport
    .message("%{text,format:size,0:'empty',:'not empty (%{text,format:size} chars)'}")
    .with("text", "hello world")
    .format();
// "not empty (11 chars)"
```


## Default Output

When no map keys are provided, the `size` formatter outputs the numeric size as text. For `null` values the output
is an empty string. When the size cannot be determined, the output is also an empty string.

```java
messageSupport
    .message("%{items,format:size}")
    .with("items", List.of("a", "b", "c", "d"))
    .format();
// "4"

messageSupport
    .message("%{items,format:size}")
    .with("items", null)
    .format();
// ""
```


## Practical Example

A common use case is building a summary message that describes the contents of a collection, changing the wording
based on how many elements are present. The following example covers all relevant cases including null and various
collection sizes.

```java
messageSupport
    .message("Cart: %{cart,format:size,null:'not loaded',0:'empty',1:'1 item',:'%{cart,format:size} items'}")
    .with("cart", null)
    .format();
// "Cart: not loaded"

messageSupport
    .message("Cart: %{cart,format:size,null:'not loaded',0:'empty',1:'1 item',:'%{cart,format:size} items'}")
    .with("cart", List.of())
    .format();
// "Cart: empty"

messageSupport
    .message("Cart: %{cart,format:size,null:'not loaded',0:'empty',1:'1 item',:'%{cart,format:size} items'}")
    .with("cart", List.of("Laptop"))
    .format();
// "Cart: 1 item"

messageSupport
    .message("Cart: %{cart,format:size,null:'not loaded',0:'empty',1:'1 item',:'%{cart,format:size} items'}")
    .with("cart", List.of("Laptop", "Mouse", "Keyboard"))
    .format();
// "Cart: 3 items"
```
