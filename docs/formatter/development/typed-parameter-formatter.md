# Custom Type-Specific Parameter Formatter

A typed parameter formatter handles the formatting of values whose runtime type matches one or more
Java classes registered by the formatter. When the formatting engine encounters a parameter value,
it walks the value's type hierarchy and selects the best matching formatter based on type and
priority. By writing your own typed formatter, you can teach the library how to render any custom
Java type as text inside a message.

The library provides two base classes that cover the most common scenarios.
`AbstractSingleTypeParameterFormatter<T>` is the right choice when your formatter handles exactly one
Java type. It takes care of `null` and empty value mapping and lets you focus on the formatting logic
itself. When a formatter needs to handle multiple unrelated types, you implement the
`ParameterFormatter` interface directly or extend `AbstractParameterFormatter<T>`.


## Single-Type Formatter

The vast majority of custom formatters handle a single type. Extending
`AbstractSingleTypeParameterFormatter<T>` requires you to implement two methods:
`formatValue(context, value)`, which produces the formatted text, and `getFormattableType()`, which
tells the framework which Java class this formatter handles. The base class automatically handles
`null` and empty values through parameter map key matching before your `formatValue` method is called,
so `value` is guaranteed to be non-null.

The following example formats a hypothetical `Color` class by rendering its hex representation:

```java
public final class ColorFormatter
    extends AbstractSingleTypeParameterFormatter<Color>
{
  @Override
  protected @NotNull Text formatValue(
      @NotNull ParameterFormatterContext context,
      @NotNull Color color)
  {
    // renders the color as a hex string, e.g. "#ff8800"
    return noSpaceText(String.format("#%06x", color.getRGB() & 0xFFFFFF));
  }

  @Override
  protected @NotNull FormattableType getFormattableType() {
    return new FormattableType(Color.class);
  }
}
```

With this formatter registered, a message like `%{bg}` produces the color's hex code:

```java
messageSupport
    .message("Background: %{bg}")
    .with("bg", new Color(255, 136, 0))
    .format();
// "Background: #ff8800"
```

Because `AbstractParameterFormatter` handles `null` and empty mapping automatically, you can use
map keys without any extra code in your formatter:

```java
messageSupport
    .message("Background: %{bg,null:'not set'}")
    .with("bg", null)
    .format();
// "Background: not set"
```


## The `FormattableType` and Ordering

Every formatter declares one or more `FormattableType` entries. A `FormattableType` pairs a Java class
with an order value in the range 0 to 127. The order controls priority when multiple formatters are
registered for the same type. A lower order value means higher precedence. If two formatters share the
same type and order, the framework falls back to comparing class names to keep the behavior
deterministic.

When you construct a `FormattableType` without specifying an order, it receives a sensible default.
Regular classes get order 80 (`DEFAULT_ORDER`), primitive types and array types get order 100
(`DEFAULT_PRIMITIVE_OR_ARRAY_ORDER`), and `Object` is fixed at order 127. To override the default
order and give your formatter higher or lower precedence than an existing one, pass an explicit
order value to the constructor:

```java
@Override
protected @NotNull FormattableType getFormattableType() 
{
  // higher precedence than DEFAULT_ORDER (80)
  return new FormattableType(Color.class, 60);
}
```

This is useful when you want your custom formatter to take priority over another formatter registered
for the same type, or when you want to ensure that a more specific formatter is always consulted
before a more general one.


## Multi-Type Formatter

Some formatters logically belong together because they handle a family of related types. Rather than
creating a separate class for each type, you can implement `ParameterFormatter` directly (or extend
`AbstractParameterFormatter<T>`) and return multiple entries from `getFormattableTypes()`.

The built-in `ToTemporalDelegate` follows this pattern. It converts legacy date and time objects such
as `Calendar`, `Date`, `FileTime` and `InstantSource` into their `java.time` equivalents and delegates
the actual formatting. Let's look at a similar example. Suppose you have a family of measurement
classes that share a common `Measurement` interface:

```java
public final class MeasurementFormatter
    extends AbstractParameterFormatter<Measurement>
{
  @Override
  protected @NotNull Text formatValue(
      @NotNull ParameterFormatterContext context,
      @NotNull Measurement measurement)
  {
    // renders the measurement as "value unit", e.g. "3.5 kg"
    return noSpaceText(measurement.getValue() + " " + measurement.getUnit());
  }

  @Override
  public @NotNull Set<FormattableType> getFormattableTypes()
  {
    return Set.of(
        new FormattableType(Weight.class),
        new FormattableType(Length.class),
        new FormattableType(Temperature.class));
  }
}
```

All three types are now formatted by the same formatter instance. When the framework encounters a
`Weight`, `Length` or `Temperature` value, it finds the `MeasurementFormatter` and calls its
`formatValue` method.


## Reading Configuration Keys

A formatter can accept configuration keys that influence its behavior. Configuration keys are named
values embedded in the message syntax using the `name:value` notation. The
`ParameterFormatterContext` (which extends `ConfigAccessor`) provides typed accessors to read them:
`getConfigValueString(name)`, `getConfigValueNumber(name)`, `getConfigValueBool(name)` and
`getConfigValueMessage(name)`. Each returns an `Optional` that is empty when the key is absent or
when the value type does not match.

The following example extends the `ColorFormatter` to support a `color` configuration key that
controls the output format:

```java
public final class ColorFormatter
    extends AbstractSingleTypeParameterFormatter<Color>
{
  @Override
  protected @NotNull Text formatValue(
      @NotNull ParameterFormatterContext context,
      @NotNull Color color)
  {
    var format = context.getConfigValueString("color").orElse("hex");

    return switch(format) {
      case "rgb" -> noSpaceText(
          color.getRed() + "," + color.getGreen() + "," + color.getBlue());
      case "name" -> noSpaceText(color.getName());
      // "hex" and any unrecognized value default to hex output
      default -> noSpaceText(
          String.format("#%06x", color.getRGB() & 0xFFFFFF));
    };
  }

  @Override
  protected @NotNull FormattableType getFormattableType() {
    return new FormattableType(Color.class);
  }

  @Override
  public @NotNull Set<String> getParameterConfigNames() {
    return Set.of("color");
  }
}
```

The message author controls the output by providing the `color` key:

```java
messageSupport
    .message("Hex: %{c,color:'hex'}")
    .with("c", Color.ORANGE)
    .format();
// "Hex: #ffc800"

messageSupport
    .message("RGB: %{c,color:'rgb'}")
    .with("c", Color.ORANGE)
    .format();
// "RGB: 255,200,0"
```

Overriding `getParameterConfigNames()` to return the set of recognized key names is not strictly
required for the formatter to work. However, the formatter service uses this information to detect
overlapping configuration key names across formatters. It is good practice to always declare them.
Configuration key names must follow the kebab-case naming convention.


## Using Map Entries

Map entries in the parameter configuration let the message author map specific values to custom
output. The formatter decides which map key types it consults and how the value is matched.
`ParameterFormatterContext` extends `MapAccessor`, which provides `getMapMessage(key, keyTypes)` to
look up a mapped message. The convenience methods `formatUsingMappedString` and
`formatUsingMappedNumber` from the `ParameterFormatter` interface handle the common cases of matching
against string and number map keys.

The following example formats an `InetAddress` by rendering its host address. It first checks
whether the message author has provided a string map entry that matches the address, and falls
back to the plain host address string if no mapping was found:

```java
public final class InetAddressFormatter
    extends AbstractSingleTypeParameterFormatter<InetAddress>
{
  @Override
  protected @NotNull Text formatValue(
      @NotNull ParameterFormatterContext context,
      @NotNull InetAddress address)
  {
    // check for a string map entry matching the host address
    return formatUsingMappedString(context, address.getHostAddress(), true)
        .orElseGet(() -> noSpaceText(address.getHostAddress()));
  }

  @Override
  protected @NotNull FormattableType getFormattableType() {
    return new FormattableType(InetAddress.class);
  }
}
```

The message author can now map well-known addresses to descriptive labels:

```java
messageSupport
    .message("Connected to %{addr,'127.0.0.1':'localhost','0.0.0.0':'any interface',:'unknown host'}")
    .with("addr", InetAddress.getByName("127.0.0.1"))
    .format();
// "Connected to localhost"
```

When no string key matches, `formatUsingMappedString` checks whether the map contains a default
entry if `includeDefault` is `true`. A default entry in the message syntax is written with a bare
colon followed by a message. In the example above, the default entry `:'unknown host'` produces a
generic label for any address that is not explicitly mapped:

```java
messageSupport
    .message("Connected to %{addr,'127.0.0.1':'localhost','0.0.0.0':'any interface',:'unknown host'}")
    .with("addr", InetAddress.getByName("192.168.1.42"))
    .format();
// "Connected to unknown host"
```

The `getMapMessage` method on the context provides the most flexibility. You can pass any combination
of `MapKey.Type` values to control which key types are considered during matching. This is useful
when your formatted type maps naturally to more than one key type.


## Delegation to the Next Formatter

When the framework resolves a formatter for a given value, it does not pick a single formatter. It
builds a prioritized chain of all formatters whose registered type matches the value's runtime type
or any of its supertypes and interfaces. Normally the first formatter in that chain produces the
result. But a formatter can explicitly pass control to the next one in the chain by calling
`context.delegateToNextFormatter()`.

This matters when a formatter adds specialized behavior for a type that already has a more general
formatter registered. The specialized formatter only wants to act when specific conditions are met.
In all other cases, the existing general formatter should handle the value as it normally would.
Without delegation, the specialized formatter would have to duplicate the general formatting logic
or leave the value unformatted.

The built-in `ByteArrayFormatter` is a good example of this pattern. The library already ships with
an `ArrayFormatter` that can format any array, including `byte[]`. The `ByteArrayFormatter` adds the
ability to encode byte arrays as Base64 strings or decode them using a specific charset, but only
when the `bytes` configuration key is present. When that key is absent, there is nothing special to
do, so the formatter delegates to the next one in the chain, which is the general `ArrayFormatter`.
This way, `%{data}` still formats a `byte[]` as a regular array, while
`%{data,bytes:'base64'}` triggers the specialized encoding.

```java
@Override
public @NotNull Text formatValue(
    @NotNull ParameterFormatterContext context,
    byte @NotNull [] byteArray)
{
  var bytesConfig = context.getConfigValueString("bytes");
  if (bytesConfig.isEmpty())
    return context.delegateToNextFormatter();

  // ... encode as base64, decode using a charset, etc.
}
```

To make the specialized formatter run before the general one, it must be registered with a lower
order value. The `ByteArrayFormatter` uses order 90 while the `ArrayFormatter` uses the default 100,
so the byte array formatter is always consulted first. If it delegates, the array formatter takes
over transparently.

The final formatter in every chain is the one registered for `Object`, which is the built-in
`StringFormatter` by default. That formatter must never delegate because there is no next formatter
after it. If it does, a `NoSuchElementException` is thrown.



## Delegation to Another Type's Formatter

Sometimes a value needs to be converted to a different type before formatting. Rather than
reimplementing the formatting logic for the target type, you can convert the value and ask the
context to format it as the target type. The `format(value, type)` method on the context looks up
the formatter registered for the given type and applies it to the converted value.

The built-in `ToTemporalDelegate` uses this approach to convert legacy date objects:

```java
if (value instanceof java.sql.Time)
  return context.format(((java.sql.Time)value).toLocalTime(), LocalTime.class);

if (value instanceof java.sql.Date)
  return context.format(((java.sql.Date)value).toLocalDate(), LocalDate.class);
```

The same technique works for any type conversion. For example, a formatter for a `Money` class
could convert the currency amount to a `Number` and delegate to the existing number formatter:

```java
public final class MoneyFormatter
    extends AbstractSingleTypeParameterFormatter<Money>
{
  @Override
  protected @NotNull Text formatValue(
      @NotNull ParameterFormatterContext context,
      @NotNull Money money)
  {
    // delegate to the Number formatter for the amount
    var amountText = context.format(money.getAmount(), Number.class);

    // append the currency symbol
    return noSpaceText(amountText.getText() + " " + money.getCurrency().getSymbol());
  }

  @Override
  protected @NotNull FormattableType getFormattableType() {
    return new FormattableType(Money.class);
  }
}
```

The `context.format(value, type)` method preserves the current parameter configuration, so any
map entries or configuration keys in the original message are passed through to the delegate
formatter.

For full control over the delegated formatting, the context also provides a four-argument variant:
`format(value, type, format, config)`. This lets you specify a named formatter and a separate
parameter configuration for the delegation. If you pass `null` for `type`, the framework determines
the type from the value's runtime class. If `config` is `null`, the current configuration is used.


## Registration

Custom formatters are registered with the formatter service. The shared instance returned by
`DefaultFormatterService.getSharedInstance()` is sealed and cannot be modified. To add your own
formatter, create a new `DefaultFormatterService` instance:

```java
var formatterService = new DefaultFormatterService();
formatterService.addFormatter(new ColorFormatter());
```

The `addFormatter` method reads the `FormattableType` entries returned by `getFormattableTypes()` and
registers the formatter for each type. Multiple formatters can coexist for the same type. They are
ordered by their `FormattableType` order value, and the formatter with the lowest order is used first.

If you need to bind a formatter to a specific type regardless of what `getFormattableTypes()` returns,
use `addFormatterForType`:

```java
formatterService.addFormatterForType(
    new FormattableType(MySpecialColor.class, 40),
    new ColorFormatter());
```

This registers the `ColorFormatter` for `MySpecialColor` at order 40, even though the formatter's
`getFormattableType()` method returns `Color.class`. This is useful when you want to reuse an existing
formatter for a subclass at a different priority.

### ServiceLoader Auto-Discovery

For library authors who distribute their formatters as a JAR, the Java `ServiceLoader` mechanism
provides automatic registration. Create a file named
`META-INF/services/de.sayayi.lib.message.formatter.parameter.ParameterFormatter` in your resources
directory and list the fully qualified class names of your formatters, one per line:

```
com.example.formatter.ColorFormatter
com.example.formatter.MeasurementFormatter
```

When the application creates a `DefaultFormatterService`, it calls `ServiceLoader.load` for the
`ParameterFormatter` interface and registers every discovered implementation. This allows your
formatters to be picked up without any manual registration code.
