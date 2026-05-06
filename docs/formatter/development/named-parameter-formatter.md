# Custom Named Parameter Formatter

This page explains how to implement the `NamedParameterFormatter` interface, how to control
which value types your formatter accepts, and how to make a formatter activate automatically
when its configuration key is present.


## The `NamedParameterFormatter` Interface

The `NamedParameterFormatter` interface extends `ParameterFormatter` and adds three methods
that control how the formatter is discovered and selected. Together with the `format` method
inherited from `ParameterFormatter`, these methods define the complete contract.

`getName()` returns the formatter's unique name. The name must follow the kebab-case naming
convention, using only lowercase letters, digits and hyphens. This is the name that message
authors write after `format:` to select the formatter. The formatter service validates the
name on registration and rejects names that do not conform.

`canFormat(Class<?>)` determines whether the formatter is willing to handle a value of a
given type. When a message requests a named formatter through `format:<name>`, the library
looks up the formatter by name and then calls `canFormat` with the runtime type of the
parameter value. If the method returns `true`, the formatter is used. If it returns `false`,
the library falls back to the regular type-based formatter selection as if no named formatter
had been specified. The default implementation returns `true` for all types, so a formatter
that does not override this method accepts any value. When the parameter value is `null`, the
library passes the sentinel type `ParameterFormatter.NULL_TYPE` to this method.

`getFormattableTypes()` returns the set of Java types that the formatter should also be
registered for in the type-based lookup. The default implementation returns an empty set,
which means the formatter is only reachable by name. If you return a non-empty set, the
formatter is registered both by name and by type, making it a hybrid formatter. The built-in
`BoolFormatter` uses this approach: it returns `Boolean.class` and `boolean.class` so that
boolean values are formatted by the `bool` formatter automatically, while other types like
`int` or `String` still require an explicit `format:bool`.


## Minimal Example

The simplest named formatter implements `NamedParameterFormatter` directly and overrides
`getName()` and `format()`. The following example creates an `elapsed` formatter that
reinterprets a numeric value as a number of milliseconds and renders it as a human-readable
time span such as `2h 30m 15s`. This kind of value reinterpretation is exactly where a named
formatter shines: the parameter value is a plain number, but the desired output has nothing to
do with how numbers are normally formatted.

```java
public final class ElapsedNamedFormatter implements NamedParameterFormatter
{
  @Override
  public @NotNull String getName() {
    // message authors select this formatter with format:elapsed
    return "elapsed";
  }

  @Override
  public boolean canFormat(@NotNull Class<?> type) 
  {
    return Number.class.isAssignableFrom(type) ||
           type == long.class || type == int.class ||
           type == NULL_TYPE;
  }

  @Override
  public @NotNull Text format(
      @NotNull ParameterFormatterContext context, Object value)
  {
    if (value == null)
      return formatNull(context);

    var totalMs = ((Number)value).longValue();
    var seconds = (totalMs / 1000) % 60;
    var minutes = (totalMs / 60_000) % 60;
    var hours = totalMs / 3_600_000;
    var sb = new StringBuilder();

    if (hours > 0)
      sb.append(hours).append("h ");
    if (hours > 0 || minutes > 0)
      sb.append(minutes).append("m ");
    sb.append(seconds).append('s');

    // renders e.g. "2h 30m 15s"
    return noSpaceText(sb.toString());
  }
}
```

With this formatter registered, the message author can render a millisecond count as a
readable time span:

```java
messageSupport
    .message("Uptime: %{ms,format:elapsed}")
    .with("ms", 9_015_000L)
    .format();
// "Uptime: 2h 30m 15s"
```

Short durations omit the hours component:

```java
messageSupport
    .message("Request took %{ms,format:elapsed}")
    .with("ms", 4200L)
    .format();
// "Request took 0m 4s"
```

Because the formatter handles `null` values through `formatNull`, the message author can
provide a `null` map key to control what happens when the value is absent:

```java
messageSupport
    .message("Uptime: %{ms,format:elapsed,null:'unknown'}")
    .with("ms", null)
    .format();
// "Uptime: unknown"
```


## Using `AbstractParameterFormatter`

When your named formatter needs automatic `null` and empty value handling before the actual
formatting logic runs, extend `AbstractParameterFormatter<T>` and implement
`NamedParameterFormatter`. The base class intercepts `null` and empty values, checks whether
the parameter configuration contains a matching map key for them, and only calls your
`formatValue` method when the value is non-null and no map key matched. This eliminates the
boilerplate of checking for `null` in every named formatter.

The following example formats a numeric value as a percentage string. It extends
`AbstractParameterFormatter<Number>` so that `null` handling is automatic, and it restricts
itself to numeric types through `canFormat`:

```java
public final class PercentNamedFormatter
    extends AbstractParameterFormatter<Number>
    implements NamedParameterFormatter
{
  @Override
  public @NotNull String getName() {
    return "percent";
  }

  @Override
  public boolean canFormat(@NotNull Class<?> type) {
    return Number.class.isAssignableFrom(type) ||
           type == int.class || type == long.class ||
           type == double.class || type == float.class ||
           type == NULL_TYPE;
  }

  @Override
  protected @NotNull Text formatValue(
      @NotNull ParameterFormatterContext context,
      @NotNull Number number)
  {
    // read the number of decimal places from the 'percent-scale' config key
    var scale = (int)context
        .getConfigValueNumber("percent-scale").orElse(1);

    var formatted = String.format(
        context.getLocale(), "%." + scale + "f%%",
        number.doubleValue() * 100.0);

    // renders e.g. "73.2%"
    return noSpaceText(formatted);
  }

  @Override
  public @NotNull Set<String> getParameterConfigNames() {
    return Set.of("percent-scale");
  }
}
```

The message author can now format any numeric value as a percentage:

```java
messageSupport
    .message("Progress: %{ratio,format:percent}")
    .with("ratio", 0.732)
    .format();
// "Progress: 73.2%"
```

Controlling the number of decimal places:

```java
messageSupport
    .message("Completion: %{ratio,format:percent,percent-scale:0}")
    .with("ratio", 1.0)
    .format();
// "Completion: 100%"
```

Because `AbstractParameterFormatter` handles `null` automatically, a `null` map key works
without any extra code in the formatter:

```java
messageSupport
    .message("Progress: %{ratio,format:percent,null:'n/a'}")
    .with("ratio", null)
    .format();
// "Progress: n/a"
```


## Type Guard with `canFormat`

The `canFormat` method acts as a type guard. When a message requests your named formatter but
the value's runtime type is not something the formatter understands, returning `false` tells
the library to fall back to the regular type-based selection. This prevents runtime errors and
keeps the formatting behavior predictable.

A common pattern is to accept a base type and all its subtypes through `isAssignableFrom`,
plus the `NULL_TYPE` sentinel if the formatter can handle `null` meaningfully.
The `PercentNamedFormatter` example above demonstrates this pattern: it accepts
all `Number` subclasses and several primitive numeric types, but rejects types
like `String` or `List`. If a message
author writes `format:percent` on a string parameter, the `canFormat` check fails and the
library uses the string formatter instead.

Be deliberate about which types you accept. When `canFormat` returns `true` for a type that
the `format` method does not actually handle, the result is unpredictable. Conversely, when
you accept `NULL_TYPE`, make sure your `format` method can handle `null` values gracefully,
either by implementing the logic yourself or by extending `AbstractParameterFormatter` which
takes care of it. The two approaches shown in the examples above illustrate both options.


## Auto-Apply with `autoApplyOnNamedConfigParameter`

By default, a named formatter is only used when the message explicitly specifies
`format:<name>`. However, some formatters are so tightly coupled to a configuration key that
requiring the message author to write both the `format` key and the configuration key feels
redundant. For these cases, a named formatter can override `autoApplyOnNamedConfigParameter()`
to return `true`.

When auto-apply is enabled, the formatter service registers a mapping from each of the
formatter's configuration key names (returned by `getParameterConfigNames()`) to the formatter
itself. During formatter resolution, if any of those configuration keys appear in the
parameter configuration, the formatter is added to the formatter chain automatically, without
the message author having to write `format:<name>`.

The built-in `GeoFormatter` uses this mechanism. It declares a configuration key named `geo`
and enables auto-apply. A message that contains the `geo` configuration key activates the
geo formatter without needing `format:geo`:

```java
// explicit selection (always works)
messageSupport
    .message("Position: %{lat,format:geo,geo:'latitude'}")
    .with("lat", 51.5074)
    .format();
// "Position: 51°30'27"N"

// auto-apply selection (same result, shorter syntax)
messageSupport
    .message("Position: %{lat,geo:'latitude'}")
    .with("lat", 51.5074)
    .format();
// "Position: 51°30'27"N"
```

Both messages produce the same output. In the second form, the presence of the `geo`
configuration key is enough to activate the `GeoFormatter`.

To implement this in your own formatter, override `autoApplyOnNamedConfigParameter()` and
return `getParameterConfigNames()` with at least one configuration key name:

```java
public final class CurrencyNamedFormatter
    extends AbstractParameterFormatter<Number>
    implements NamedParameterFormatter
{
  @Override
  public @NotNull String getName() {
    return "currency";
  }

  @Override
  public boolean canFormat(@NotNull Class<?> type) {
    return Number.class.isAssignableFrom(type) ||
           type == int.class || type == long.class ||
           type == double.class || type == float.class ||
           type == NULL_TYPE;
  }

  @Override
  public boolean autoApplyOnNamedConfigParameter() {
    // activate this formatter whenever 'currency' config key is present
    return true;
  }

  @Override
  protected @NotNull Text formatValue(
      @NotNull ParameterFormatterContext context,
      @NotNull Number amount)
  {
    var currencyCode = context
        .getConfigValueString("currency").orElse("USD");
    var currency = java.util.Currency.getInstance(currencyCode);
    var formatted = java.text.NumberFormat
        .getCurrencyInstance(context.getLocale());
    formatted.setCurrency(currency);

    return noSpaceText(formatted.format(amount.doubleValue()));
  }

  @Override
  public @NotNull Set<String> getParameterConfigNames() {
    return Set.of("currency");
  }
}
```

With this formatter registered, the message author can write:

```java
messageSupport
    .message("Total: %{amount,currency:'EUR'}")
    .with("amount", 42.50)
    .format();
// "Total: €42.50" (assuming en_US locale)
```

The `currency` configuration key triggers auto-apply, so the
`CurrencyNamedFormatter` is selected without `format:currency`. The message
author can still write `format:currency` explicitly if preferred; both forms
are equivalent.

Be aware that each configuration key name can only be mapped to a single auto-apply formatter.
If two auto-apply formatters declare the same configuration key name, the formatter service
throws a `FormatterServiceException` during registration. Choose distinctive configuration
key names to avoid conflicts.


## Dual Registration (Named and Typed)

A named formatter can also participate in type-based selection by returning a non-empty set
from `getFormattableTypes()`. This makes it a hybrid formatter: it is reachable both by name
through `format:<name>` and automatically through the type hierarchy.

The built-in `BoolFormatter` demonstrates this pattern. It implements `NamedParameterFormatter`
with the name `bool`, but it also returns `Boolean.class` and `boolean.class` from
`getFormattableTypes()`. As a result, `Boolean` values are formatted by the `bool` formatter
automatically, without the message author having to write `format:bool`. For non-boolean types
like `int` or `String`, the `format:bool` syntax is still required to override the default
type-based selection.

If your formatter is the natural default for one or more specific types but also needs to be
selectable by name for other types, implement it as a dual-registered formatter. Override
`getFormattableTypes()` to return the types that should use this formatter automatically:

```java
public final class HexNamedFormatter
    extends AbstractParameterFormatter<Number>
    implements NamedParameterFormatter
{
  @Override
  public @NotNull String getName() {
    return "hex";
  }

  @Override
  public boolean canFormat(@NotNull Class<?> type) 
  {
    return Number.class.isAssignableFrom(type) ||
           type == int.class || type == long.class ||
           type == byte.class || type == short.class ||
           type == NULL_TYPE;
  }

  @Override
  protected @NotNull Text formatValue(
      @NotNull ParameterFormatterContext context,
      @NotNull Number number)
  {
    var prefix = context
        .getConfigValueBool("hex-prefix").orElse(true);

    // renders e.g. "0xff" or "ff" depending on the prefix config
    var hex = Long.toHexString(number.longValue());
    return noSpaceText(prefix ? "0x" + hex : hex);
  }

  @Override
  public @NotNull Set<String> getParameterConfigNames() {
    return Set.of("hex-prefix");
  }
}
```

This formatter is reachable by name (`format:hex`), but it does not return any types from
`getFormattableTypes()`, so it is never used automatically. If you wanted `Byte` values to
always be formatted as hex by default, you could add them to the set:

```java
@Override
public @NotNull Set<FormattableType> getFormattableTypes() 
{
  // Byte values will be formatted as hex automatically;
  // order 60 gives this formatter higher precedence than the default NumberFormatter
  return Set.of(new FormattableType(Byte.class, 60));
}
```

With that change, any `Byte` value is formatted as hex without needing `format:hex`, while
other numeric types still require the explicit `format:hex` syntax.


## Delegation

Named parameter formatters cannot delegate to a next formatter in the chain. When the library
selects a named formatter through `format:<name>`, it creates a formatter chain that contains
only that single formatter. Calling `context.delegateToNextFormatter()` from inside a named
formatter results in a `NoSuchElementException`. This is intentional because the message author
has explicitly chosen a specific formatter and there is no meaningful "next" formatter to fall
back to.

If your named formatter needs to delegate formatting to a different type's formatter, use the
`context.format(value, type)` method instead. This performs a fresh formatter lookup for the
given type and applies it to the value. The `BitmaskFormatter` built into the library uses this
technique: it converts a numeric value to a `BitSet` and then delegates to the `BitSet`
formatter:

```java
@Override
public @NotNull Text formatValue(
    @NotNull ParameterFormatterContext context,
    @NotNull Object value)
{
  var bitSet = convertToBitSet(value);

  // delegate to the BitSet formatter chain
  return context.format(bitSet, BitSet.class);
}
```


## Using Map Entries

Named formatters can use map entries in the parameter configuration just like typed formatters.
The `formatUsingMappedString` and `formatUsingMappedNumber` convenience methods and the
`getMapMessage` method on the context are all available. This lets the message author map
specific formatted values to custom output.

The following example creates a named formatter for a hypothetical application-specific
`Priority` enum. It converts the enum's numeric level to a string and uses
`formatUsingMappedNumber` so that the message author can map individual levels to custom
labels:

```java
public final class PriorityNamedFormatter implements NamedParameterFormatter
{
  @Override
  public @NotNull String getName() {
    return "priority";
  }

  @Override
  public boolean canFormat(@NotNull Class<?> type) {
    return Priority.class.isAssignableFrom(type) || type == NULL_TYPE;
  }

  @Override
  public @NotNull Text format(
      @NotNull ParameterFormatterContext context, Object value)
  {
    if (value == null)
      return formatNull(context);

    var priority = (Priority)value;

    // let the message author map specific levels to custom labels
    return formatUsingMappedNumber(context, priority.getLevel(), true)
        .orElseGet(() -> noSpaceText(priority.name()));
  }
}
```

The message author can now map priority levels to descriptive labels:

```java
messageSupport
    .message("%{p,format:priority,1:'low',2:'medium',3:'high',:'unclassified'}")
    .with("p", Priority.HIGH)  // getLevel() returns 3
    .format();
// "high"
```

When a priority level has no explicit mapping, the default map entry acts as a fallback:

```java
messageSupport
    .message("%{p,format:priority,1:'low',2:'medium',3:'high',:'unclassified'}")
    .with("p", Priority.CRITICAL)  // getLevel() returns 5
    .format();
// "unclassified"
```

Without any map entries at all, the formatter falls back to the enum constant name:

```java
messageSupport
    .message("Priority: %{p,format:priority}")
    .with("p", Priority.MEDIUM)
    .format();
// "Priority: MEDIUM"
```


## Registration

Named formatters are registered with the formatter service in the same way as typed
formatters. The `addFormatter` method handles both. When it detects that the formatter
implements `NamedParameterFormatter`, it validates the name against the kebab-case convention,
registers the formatter under its name, and, if the formatter returns types from
`getFormattableTypes()`, also registers it for those types. If auto-apply is enabled, it
additionally maps the formatter's configuration key names for automatic activation.

```java
var formatterService = new DefaultFormatterService();
formatterService.addFormatter(new ElapsedNamedFormatter());
formatterService.addFormatter(new PercentNamedFormatter());
```

The shared instance returned by `DefaultFormatterService.getSharedInstance()` is sealed and
cannot be modified. To register custom named formatters, create a new
`DefaultFormatterService` instance as shown above.

### ServiceLoader Auto-Discovery

For library authors who distribute their named formatters as a JAR, the Java `ServiceLoader`
mechanism provides automatic registration. Create a file named
`META-INF/services/de.sayayi.lib.message.formatter.parameter.ParameterFormatter` in your
resources directory and list the fully qualified class names of your formatters, one per line:

```
com.example.formatter.ElapsedNamedFormatter
com.example.formatter.PercentNamedFormatter
com.example.formatter.CurrencyNamedFormatter
```

When the application creates a `DefaultFormatterService`, it calls `ServiceLoader.load` for
the `ParameterFormatter` interface and registers every discovered implementation. Named
formatters are recognized and registered by name automatically, just as they would be if
`addFormatter` were called manually.
