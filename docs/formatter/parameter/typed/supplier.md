# Supplier

This formatter is included in the `DefaultFormatterService`.

The library provides five formatters that handle Java's supplier functional interfaces: `Supplier`,
`BooleanSupplier`, `IntSupplier`, `LongSupplier` and `DoubleSupplier`. Each formatter is automatically selected
based on the parameter's type. The core principle is the same for all five: the supplier is evaluated once at
format time and the resulting value is delegated to the formatter appropriate for its type.

Suppliers are useful when you want to defer value computation until the message is actually formatted. The
supplier is called exactly once during formatting, so any side effects occur at that point.


## SupplierFormatter

The `SupplierFormatter` handles `java.util.function.Supplier` values. It calls `get()` on the supplier and
delegates formatting of the returned object to the formatter appropriate for that object's type. Because the
supplied value can be of any type, the output depends entirely on what the supplier returns.

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

All map keys and configuration options that apply to the supplied value's type work transparently through the
supplier wrapper.

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

The `SupplierFormatter` supports size queries. The size is determined by evaluating the supplier and querying
the size of the returned value.


## BooleanSupplierFormatter

The `BooleanSupplierFormatter` handles `java.util.function.BooleanSupplier` values. It calls `getAsBoolean()`
on the supplier and delegates formatting to the boolean formatter. The result is identical to passing a plain
`boolean` value directly.

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
and delegates formatting to the integer formatter. The result is identical to passing a plain `int` value
directly.

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

The `LongSupplierFormatter` handles `java.util.function.LongSupplier` values. It calls `getAsLong()` on the
supplier and delegates formatting to the long formatter. The result is identical to passing a plain `long` value
directly.

```java
LongSupplier timestamp = () -> System.currentTimeMillis();

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

The `DoubleSupplierFormatter` handles `java.util.function.DoubleSupplier` values. It calls `getAsDouble()` on
the supplier and delegates formatting to the double formatter. The result is identical to passing a plain
`double` value directly.

```java
DoubleSupplier pi = () -> Math.PI;

messageSupport
    .message("Pi: %{pi}")
    .with("pi", pi)
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

For all five supplier formatters, a `null` parameter value (the supplier itself is `null`) produces an empty
string by default. You can provide a `null` map key to handle this case explicitly.

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
