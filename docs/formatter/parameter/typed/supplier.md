# Supplier

This formatter is included in the `DefaultFormatterService`.

The library provides five formatters that handle Java's supplier functional interfaces: `Supplier`, `BooleanSupplier`,
`IntSupplier`, `LongSupplier` and `DoubleSupplier`. Each formatter is automatically selected based on the parameter's
type. The core principle is the same for all five: the supplier is evaluated once at format time and the resulting 
value is delegated to the formatter appropriate for its type.

Suppliers are useful for deferring value computation until the message is actually formatted. The supplier is called
when the parameter is formatted, not when it is passed to `with(...)`. In most cases, however, it is better to
compute the value up front and pass the result instead of a supplier. See
[Choosing Parameter Values](../../../message/formatting.md#choosing-parameter-values) for the reasons.

A supplier set with `with(...)` is wrapped in a caching delegate and is invoked at most once. This holds no matter how
many map keys the value is compared with, as with [`choice`](../named/choice.md), and how often the parameter is
referenced in the message. The selected map entry and the formatted text are therefore always based on the same
value, even for suppliers that return a different value on each call, such as a counter, a clock or a random value.

The examples on this page use the following message support.

```java title="Example setup"
import de.sayayi.lib.message.MessageSupportFactory;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

var messageSupport = MessageSupportFactory.createDefault();
// all built-in formatters are available
```

```java
var calls = new AtomicInteger();
Supplier<Integer> level = () -> {
  calls.incrementAndGet();
  return 2;
};

messageSupport
    .message("%{v,format:choice,1:'low',2:'medium',3:'high'}")
    .with("v", level)
    .format();
// "medium"

calls.get();
// 1
```

The cached value is kept for as long as the parameter is set. Formatting the message again with the same configurer,
or using a supplier returned by `formatSupplier()`, does not invoke the supplier again but reuses the value supplied
on the first evaluation. To have the supplier evaluated anew, set the parameter again:

```java
calls.set(0);

var configurer = messageSupport
    .message("%{v}")
    .with("v", level);

configurer.format();
configurer.format();
calls.get();
// 1

configurer.with("v", level).format();
calls.get();
// 2
```

If the supplier throws an exception, no value is cached and the supplier is invoked again the next time the parameter
is formatted. `getParameters()` returns the caching delegate, not the supplier passed to `with(...)`.

/// warning | Formatters for custom supplier types are not used
The formatter for a parameter value is selected based on the type of the caching delegate, not on the type of the
supplier passed to `with(...)`. If an application provides its own implementation of one of the supplier interfaces
and registers a formatter for that specific type, the formatter never becomes a candidate for a value set with
`with(...)`. The delegate only implements the supplier interface, so the value is formatted by the matching supplier
formatter described on this page instead.

```java
public final class FreeMemory implements LongSupplier
{
  @Override
  public long getAsLong() {
    return Runtime.getRuntime().freeMemory();
  }
}

// registered for FreeMemory.class
formatterService.addFormatter(new FreeMemoryFormatter());

messageSupport
    .message("%{mem}")
    .with("mem", new FreeMemory())
    .format();
// formatted by LongSupplierFormatter, FreeMemoryFormatter is not used
```

The same applies to any other interface or type implemented by the supplier: the delegate does not implement it, so
formatters registered for those types are not used either. A formatter registered for the supplier interface itself
(e.g. `LongSupplier`) is still used.
///

/// note | Supplier caching only applies to `MessageConfigurer`
Supplier parameter values are only cached when they are set with `MessageConfigurer.with(...)`, which includes
`with(Map)`. If parameters are passed to a `Message` by other means, for example by calling
`Message.format(MessageAccessor, Parameters)` or `Message.format(MessageAccessor, Map)` directly, the supplier
formatters invoke the supplier each time the value is needed. In that case, the supplier should be cheap and return
the same value on each call, or be wrapped in a [`SupplierDelegate`](../../development/utilities.md#supplierdelegate)
by the caller.
///


## SupplierFormatter

The `SupplierFormatter` handles `java.util.function.Supplier` values. It calls `get()` on the supplier and delegates
formatting of the returned object to the formatter appropriate for that object's type. Because the supplied value can 
be of any type, the output depends entirely on what the supplier returns.

```java
Supplier<String> greeting = () -> "hello";

messageSupport
    .message("%{msg}")
    .with("msg", greeting)
    .format();
// "hello"
```

```java
Supplier<Integer> counter = () -> 42;

messageSupport
    .message("%{count,>0:'positive',0:'zero'}")
    .with("count", counter)
    .format();
// "positive"
```

All map keys and configuration options that apply to the supplied value's type work transparently through the supplier
wrapper.

```java
Supplier<Boolean> check = () -> true;

messageSupport
    .message("%{ok,true:'passed',false:'failed'}")
    .with("ok", check)
    .format();
// "passed"
```

If the supplier returns `null`, the value is treated as `null` for formatting purposes.

```java
Supplier<String> empty = () -> null;

messageSupport
    .message("%{val,null:'nothing'}")
    .with("val", empty)
    .format();
// "nothing"
```

### Size Delegation

The `SupplierFormatter` supports size queries. The size is determined by evaluating the supplier and querying the size
of the returned value.


## BooleanSupplierFormatter

The `BooleanSupplierFormatter` handles `java.util.function.BooleanSupplier` values. It calls `getAsBoolean()` on the
supplier and delegates formatting to the boolean formatter. The result is identical to passing a plain `boolean` value
directly.

This formatter is also documented on the [Boolean](boolean.md) page.

```java
BooleanSupplier isReady = () -> true;

messageSupport
    .message("System %{status,true:'online',false:'offline'}")
    .with("status", isReady)
    .format();
// "System online"
```

```java
BooleanSupplier hasLock = () -> false;

messageSupport
    .message("%{locked,true:'yes',false:'no'}")
    .with("locked", hasLock)
    .format();
// "no"
```


## IntSupplierFormatter

The `IntSupplierFormatter` handles `java.util.function.IntSupplier` values. It calls `getAsInt()` on the supplier
and delegates formatting to the integer formatter. The result is identical to passing a plain `int` value directly.

```java
IntSupplier itemCount = () -> 7;

messageSupport
    .message("Items: %{count}")
    .with("count", itemCount)
    .format();
// "Items: 7"
```

All number map keys work against the evaluated integer value.

```java
IntSupplier errorCount = () -> 0;

messageSupport
    .message("%{errors,0:'no errors',1:'one error',>1:'multiple errors'}")
    .with("errors", errorCount)
    .format();
// "no errors"
```


## LongSupplierFormatter

The `LongSupplierFormatter` handles `java.util.function.LongSupplier` values. It calls `getAsLong()` on the supplier 
and delegates formatting to the long formatter. The result is identical to passing a plain `long` value directly.

```java
LongSupplier timestamp = () -> 1714645200000L;

messageSupport
    .message("Time: %{ts}")
    .with("ts", timestamp)
    .format();
// "Time: 1714645200000"
```

All number map keys work against the evaluated long value.

```java
LongSupplier freeMemory = () -> Runtime.getRuntime().freeMemory();

messageSupport
    .message("%{mem,>1000000:'sufficient',<=1000000:'low'}")
    .with("mem", freeMemory)
    .format();
// "sufficient" (depending on available memory)
```


## DoubleSupplierFormatter

The `DoubleSupplierFormatter` handles `java.util.function.DoubleSupplier` values. It calls `getAsDouble()` on the 
supplier and delegates formatting to the double formatter. The result is identical to passing a plain `double` value
directly.

```java
DoubleSupplier pi = () -> Math.PI;

messageSupport
    .message("Pi: %{pi}")
    .with("pi", pi)
    .locale(Locale.US)
    .format();
// "Pi: 3.142"
```

All number map keys work against the evaluated double value.

```java
DoubleSupplier temperature = () -> 36.6;

messageSupport
    .message("%{temp,>=38:'fever',<38:'normal'}")
    .with("temp", temperature)
    .format();
// "normal"
```


## Null Handling

For all five supplier formatters, a `null` parameter value (the supplier itself is `null`) produces an empty string by
default. A `null` map key can be provided to handle this case explicitly.

```java
messageSupport
    .message("%{data,null:'no supplier'}")
    .with("data", (Supplier<?>) null)
    .format();
// "no supplier"
```

```java
messageSupport
    .message("%{count,null:'unavailable'}")
    .with("count", (IntSupplier) null)
    .format();
// "unavailable"
```
