# Custom Size Queryable

The `SizeQueryable` interface is a sub-interface of `ParameterFormatter` that marks a formatter as capable of 
calculating the size of a parameter value. When the built-in `size` named formatter processes a parameter like
`%{items,format:size}`, it does not compute the size itself. Instead, it asks the formatter context to find a 
`SizeQueryable` formatter registered for the value's runtime type and calls its `size` method.

This mechanism makes the `size` formatter extensible. When a custom type has a meaningful notion of size, implementing
`SizeQueryable` in its formatter lets the `size` named formatter work with that type without any additional 
configuration.


## The Interface

The `SizeQueryable` interface declares a single method:

```java
@NotNull OptionalLong size(@NotNull ParameterFormatterContext context,
                           @NotNull Object value);
```

The method receives the current formatter context and the non-null parameter value. It must return an `OptionalLong`
containing the computed size (a value greater than or equal to zero), or `OptionalLong.empty()` if the formatter cannot 
determine a meaningful size for the given value. Returning empty signals that the framework should continue searching 
through additional formatters registered for the value's type.


## How the Framework Uses SizeQueryable

When a message contains `%{param,format:size}`, the `SizeFormatter` calls `context.size(value)`. The context 
implementation iterates over all formatters registered for the value's runtime type. For each formatter that implements
`SizeQueryable`, it calls the `size` method. The first formatter that returns a present `OptionalLong` wins and its 
value becomes the computed size. If no formatter can determine the size, the `size` formatter treats the result as 
indeterminate and uses the `empty` map key.

This means that `SizeQueryable` is not limited to the `size` named formatter. Any formatter that holds a reference to
the `ParameterFormatterContext` can invoke `context.size(value)` to query the size of an arbitrary object. Custom 
formatters can leverage this when they need to make decisions based on value size.


## Implementing SizeQueryable in a Typed Parameter Formatter

The most common scenario is adding size support to a formatter that already handles a specific type. Such a formatter 
extends `AbstractSingleTypeParameterFormatter` (or `AbstractParameterFormatter`) and additionally implements 
`SizeQueryable`. This gives the formatter two responsibilities: formatting the value into text and reporting its size.

The following example defines a formatter for an application `Ring<E>` data structure, which is a fixed-size circular
buffer. The "size" of a ring is the number of elements currently stored in it. Once the ring is full, adding an element
overwrites the oldest one.

```java
import java.util.Iterator;
import java.util.NoSuchElementException;

public final class Ring<E> implements Iterable<E>
{
  private final Object[] elements;
  private int start;
  private int count;

  public Ring(int capacity) {
    elements = new Object[capacity];
  }

  public void add(E element)
  {
    elements[(start + count) % elements.length] = element;

    if (count < elements.length)
      count++;
    else
      start = (start + 1) % elements.length;
  }

  public int count() {
    return count;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Iterator<E> iterator()
  {
    return new Iterator<>() {
      private int index;

      public boolean hasNext() {
        return index < count;
      }

      public E next()
      {
        if (!hasNext())
          throw new NoSuchElementException();

        return (E)elements[(start + index++) % elements.length];
      }
    };
  }
}
```

```java
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.parameter.AbstractSingleTypeParameterFormatter;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatter;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext;
import de.sayayi.lib.message.part.MessagePart.Text;

import org.jetbrains.annotations.NotNull;

import java.util.OptionalLong;

import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;

public final class RingFormatter
    extends AbstractSingleTypeParameterFormatter<Ring<?>>
    implements ParameterFormatter.SizeQueryable
{
  @Override
  protected @NotNull Text formatValue(
      @NotNull ParameterFormatterContext context,
      @NotNull Ring<?> ring)
  {
    // format as comma-separated list of elements
    var sb = new StringBuilder();
    for(var element: ring)
    {
      if (!sb.isEmpty())
        sb.append(", ");
      sb.append(context.format(element).getText());
    }
    return noSpaceText(sb.toString());
  }
  
  @Override
  public @NotNull OptionalLong size(
      @NotNull ParameterFormatterContext context,
      @NotNull Object value)
  {
    // returns number of elements currently in the ring
    return OptionalLong.of(((Ring<?>)value).count());
  }
  
  @Override
  protected @NotNull FormattableType getFormattableType() {
    return new FormattableType(Ring.class);
  }
}
```

The examples on this page use the following message support and a helper method that creates a filled ring:

```java
import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.DefaultFormatterService;

import java.util.List;

var formatterService = new DefaultFormatterService();
var messageSupport = MessageSupportFactory.create(formatterService);

@SafeVarargs
static <E> Ring<E> ringWithElements(E... elements)
{
  var ring = new Ring<E>(8);
  for(var element: elements)
    ring.add(element);

  return ring;
}
// formatters added to formatterService are used by messageSupport
```

After registering this formatter, the `size` named formatter works transparently with `Ring` values:

```java
formatterService.addFormatter(new RingFormatter());

messageSupport
    .message("%{buffer,format:size}")
    .with("buffer", ringWithElements("a", "b", "c"))
    .format();
// "3"

messageSupport
    .message("""
        %{buffer,format:size,\
            0:'empty ring',\
            1:'one element',\
             :'%{buffer,format:size} elements'}\
        """)
    .with("buffer", ringWithElements("a", "b", "c"))
    .format();
// "3 elements"
```


## Delegating Size Calculation

Some types are wrappers around another value. A formatter for such a type does not define its own notion of size but 
instead delegates to the contained value's formatter. The `SupplierFormatter` and `ReferenceFormatter` in the library 
follow this pattern. They call `context.size(innerValue)` to let the framework resolve the appropriate `SizeQueryable`
formatter for the inner value.

Consider a `Lazy<T>` wrapper that defers initialization. The interface deliberately does not extend `Supplier`, because
supplier values are wrapped in a caching delegate and a formatter registered for a `Supplier` subtype would never be
selected (see [Supplier](../parameter/typed/supplier.md)).

```java
@FunctionalInterface
public interface Lazy<T>
{
  T get();
}

public final class LazyFormatter
    extends AbstractSingleTypeParameterFormatter<Lazy<?>>
    implements ParameterFormatter.SizeQueryable
{
  @Override
  protected @NotNull Text formatValue(
      @NotNull ParameterFormatterContext context,
      @NotNull Lazy<?> lazy)
  {
    return context.format(lazy.get());
    // delegates formatting to the contained value's formatter
  }
  
  @Override
  public @NotNull OptionalLong size(
      @NotNull ParameterFormatterContext context,
      @NotNull Object value)
  {
    var inner = ((Lazy<?>)value).get();

    // delegates size calculation to the contained value's formatter
    return inner == null
        ? OptionalLong.empty()
        : context.size(inner);
  }
  
  @Override
  protected @NotNull FormattableType getFormattableType() {
    return new FormattableType(Lazy.class);
  }
}
```

When the contained value is a `List` with four elements, `context.size(inner)` finds the `IterableFormatter` (which 
implements `SizeQueryable`) and returns `4`.

```java
formatterService.addFormatter(new LazyFormatter());

Lazy<List<String>> lazyList = () -> List.of("a", "b", "c", "d");

messageSupport
    .message("%{items,format:size}")
    .with("items", lazyList)
    .format();
// "4"
```

 When the contained value is a type that has no size-aware formatter, the 
delegation returns `OptionalLong.empty()` and the `size` formatter falls through to the `empty` map key.


## Standalone Size-Only Formatter

Combining size calculation with formatting logic is not required. A formatter can exist solely to provide size 
information for a type, while leaving formatting entirely to other registered formatters. This is useful for adding 
size awareness to a type that already has a satisfactory formatter but no `SizeQueryable` implementation.

The key insight is that the `format` method can delegate to the next formatter in the chain by calling
`context.delegateToNextFormatter()`. The formatter itself handles no formatting at all. Its only contribution is the
`size` method.

The following example adds size support for an application `Multimap<K,V>` interface that maps each key to a
collection of values. It extends `Map`, so `Map.size()` counts the keys, while the size of a multimap is defined as the
total number of values across all keys:

```java
import java.util.Collection;
import java.util.Map;

public interface Multimap<K,V> extends Map<K,Collection<V>>
{
  default int valueCount()
  {
    return values().stream()
        .mapToInt(Collection::size)
        .sum();
  }
}

public final class MultimapSizeFormatter
    extends AbstractSingleTypeParameterFormatter<Multimap<?,?>>
    implements ParameterFormatter.SizeQueryable
{
  @Override
  protected @NotNull Text formatValue(
      @NotNull ParameterFormatterContext context,
      @NotNull Multimap<?,?> multimap)
  {
    // does not format; delegates to the next formatter in the chain
    return context.delegateToNextFormatter();
  }


  @Override
  public @NotNull OptionalLong size(
      @NotNull ParameterFormatterContext context,
      @NotNull Object value)
  {
    // total number of values across all keys
    return OptionalLong.of(((Multimap<?,?>)value).valueCount());
  }


  @Override
  protected @NotNull FormattableType getFormattableType() {
    return new FormattableType(Multimap.class);
  }
}
```

Because this formatter delegates formatting entirely, the formatter registered for the parent type `Map` still produces
the textual output. The `MultimapSizeFormatter` only activates when something queries the size.

To ensure this formatter is consulted for size queries before the general `MapFormatter` (which would count the keys),
assign a higher precedence through the `FormattableType` order:

```java
@Override
protected @NotNull FormattableType getFormattableType() 
{
  // lower order value = higher precedence
  return new FormattableType(Multimap.class, 
      FormattableType.DEFAULT_ORDER - 10);
}
```


## Returning Empty

When a formatter cannot determine the size for a particular value instance, return `OptionalLong.empty()`. This tells
the framework to try the next `SizeQueryable` formatter in the chain. A common pattern is returning empty for certain 
subtypes or states:

```java
@Override
public @NotNull OptionalLong size(
    @NotNull ParameterFormatterContext context,
    @NotNull Object value)
{
  var resource = (Resource)value;

  if (!resource.exists()) 
  {
    // cannot determine size of a non-existent resource
    return OptionalLong.empty();
  }
  
  try {
    return OptionalLong.of(resource.contentLength());
  } catch(IOException ex) {
    // size cannot be determined due to I/O failure
    return OptionalLong.empty();
  }
}
```

This is the same approach used by the built-in `PathFormatter`, which returns empty when the path does not refer to a 
regular file or when an I/O error occurs while reading the file size.


## Registration

Register a `SizeQueryable` formatter the same way as any other parameter formatter. Because the shared instance
returned by `DefaultFormatterService.getSharedInstance()` is sealed, a separate `DefaultFormatterService` instance must 
be created:

```java
var formatterService = new DefaultFormatterService();
formatterService.addFormatter(new RingFormatter());
formatterService.addFormatter(new MultimapSizeFormatter());
```

The formatter is stored alongside other formatters for the same type. When the context iterates formatters for a size 
query, it checks each one in order of precedence. The first formatter that implements `SizeQueryable` and returns a 
present `OptionalLong` provides the final answer.

If multiple `SizeQueryable` formatters exist for the same type, the one with the lowest `FormattableType.order` value 
(highest precedence) is consulted first. Design the order values accordingly to override a built-in size calculation.
