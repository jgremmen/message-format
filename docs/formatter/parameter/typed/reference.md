# Reference

This formatter is included in the `DefaultFormatterService`.

The `ReferenceFormatter` handles `java.lang.ref.Reference` values. This includes all standard reference types
such as `WeakReference`, `SoftReference` and `PhantomReference`. The formatter is automatically selected whenever
a parameter value is an instance of `Reference`.

The formatter unwraps the reference by calling `get()` and delegates all formatting to the formatter appropriate
for the referenced object's type. If the referenced object has been garbage collected (meaning `get()` returns
`null`), the value is treated as `null`.

```java
WeakReference<String> ref = new WeakReference<>("hello");

messageSupport
    .message("%{value}")
    .with("value", ref)
    .format();
// "hello"
```

```java
SoftReference<Integer> ref = new SoftReference<>(42);

messageSupport
    .message("%{n,>0:'positive',0:'zero'}")
    .with("n", ref)
    .format();
// "positive"
```

Because formatting is fully delegated to the referenced value's formatter, any map keys or configuration that
apply to the referenced type work transparently through the reference wrapper.

```java
WeakReference<Boolean> ref = new WeakReference<>(true);

messageSupport
    .message("%{flag,true:'yes',false:'no'}")
    .with("flag", ref)
    .format();
// "yes"
```


## Garbage Collected References

When the referenced object has been garbage collected, `get()` returns `null`. In this case the formatter treats
the value as `null`. The `null` and `empty` map keys both match in this situation.

```java
WeakReference<String> ref = new WeakReference<>(null);

messageSupport
    .message("%{value,null:'collected'}")
    .with("value", ref)
    .format();
// "collected"
```

```java
SoftReference<String> ref = new SoftReference<>(null);

messageSupport
    .message("%{value,empty:'gone'}")
    .with("value", ref)
    .format();
// "gone"
```

Without a `null` or `empty` map key, a collected reference produces an empty string.


## Map Key Behavior

Map key comparisons for `bool`, `number` and `string` keys are delegated to the referenced value. If the
reference has been collected (the referenced value is `null`), these key types are compared against `null`.

The `null` key matches when the parameter itself is `null` or when the referenced object has been collected.
The `empty` key behaves the same way.

```java
WeakReference<Integer> ref = new WeakReference<>(7);

messageSupport
    .message("%{n,>5:'high',<=5:'low'}")
    .with("n", ref)
    .format();
// "high"
```


## Size Delegation

The `ReferenceFormatter` supports size queries. The size is determined by calling `get()` and querying the size
of the referenced value. If the reference has been collected, no size is available.


## Null Handling

A `null` parameter value (the reference itself is `null`, not just the referenced object) produces an empty
string by default. You can provide a `null` map key to handle this case explicitly.

```java
messageSupport
    .message("%{value,null:'no reference'}")
    .with("value", (WeakReference<String>) null)
    .format();
// "no reference"
```
