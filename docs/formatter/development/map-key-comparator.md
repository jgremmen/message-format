# Custom Map Key Comparator

When a message contains a parameter with map entries such as `%{status,0:'off',1:'on'}`, the
formatting engine needs to determine which map entry matches the current parameter value. Each map
entry has a key with a type (string, number, bool, null, or empty) and a comparison operator. The
`MapKeyComparator<T>` interface is the contract that formatters implement to tell the engine how a
value of type `T` compares against each kind of map key.

Every built-in formatter that supports map key matching already implements `MapKeyComparator`. The
`NumberFormatter`, for example, knows how to compare a `Number` value against number keys, string
keys that contain numeric text, and bool keys where zero maps to `false`. When you create a custom
formatter for your own type and want map entries to work with that type, you implement
`MapKeyComparator` yourself. The library also provides `AbstractMapKeyComparator<T>`, a convenience
base class for formatters that only contribute map key comparison logic and delegate all formatting
to the next formatter in the chain.


## The Interface

The `MapKeyComparator<T>` interface extends `ParameterFormatter` and declares five comparison methods,
one for each map key type. Each method receives the parameter value and a `ComparatorContext` that
provides access to the key's value, its comparison operator, and the current locale. The method
returns a `MatchResult` that tells the engine how well the value matches the key.

```java
public interface MapKeyComparator<T> extends ParameterFormatter
{
  MatchResult compareToNullKey(T value, ComparatorContext context);
  MatchResult compareToEmptyKey(T value, ComparatorContext context);
  MatchResult compareToBoolKey(T value, ComparatorContext context);
  MatchResult compareToNumberKey(T value, ComparatorContext context);
  MatchResult compareToStringKey(T value, ComparatorContext context);
}
```

All five methods have default implementations. `compareToNullKey` checks whether the value is
`null` and returns a match or mismatch accordingly. `compareToEmptyKey` does the same for `null`
values, treating them as empty. `compareToBoolKey`, `compareToNumberKey` and `compareToStringKey`
all return `MISMATCH` by default. This means a formatter only needs to override the methods that
are meaningful for its type.


## Match Results

The `MatchResult` interface represents the outcome of comparing a value to a map key. Its `value()`
method returns a numeric score. Higher scores indicate better matches, and a score of zero or less
indicates a mismatch. When multiple map keys match the same value, the engine selects the entry with
the highest score.

The `MatchResult.Defined` enum provides predefined results ordered from worst to best:

| Result       | Score | Meaning                                                              |
|--------------|-------|----------------------------------------------------------------------|
| `MISMATCH`   | 0     | No match at all                                                      |
| `NOT_EMPTY`  | 2     | Value is not empty (from `!empty` key)                               |
| `NOT_NULL`   | 4     | Value is not null (from `!null` key)                                 |
| `EMPTY`      | 6     | Value is empty                                                       |
| `NULL`       | 8     | Value is null                                                        |
| `LENIENT`    | 10    | Same meaning but different representation (e.g. `0` matches `false`) |
| `EQUIVALENT` | 12    | Same value but different type (e.g. `4` matches `'4'`)               |
| `EXACT`      | 14    | Exact type and value match                                           |

When your comparator can match a value against a key, return the result that best describes the
quality of the match. Use `EXACT` when value and key share the same type and the comparison succeeds,
`EQUIVALENT` when the key is a different representation of the same value, and `LENIENT` when the
match requires interpreting the value in a non-obvious way.

The scoring matters when a message contains keys of different types that could both match. For
example, if a message contains both `1:'one'` and `'1':'one as string'`, a numeric value of `1`
would match the number key with `EXACT` and the string key with `EQUIVALENT`. The engine picks the
number key because `EXACT` scores higher.


## The ComparatorContext

The `ComparatorContext` passed to each comparison method provides everything you need to perform the
comparison. It extends `ConfigAccessor`, so you can read parameter configuration values if needed.

`getCompareType()` returns the comparison operator for the current key. The `CompareType` enum
defines six operators: `EQ` (equal), `NE` (not equal), `LT` (less than), `LTE` (less than or equal),
`GT` (greater than) and `GTE` (greater than or equal). When the message author writes `>5:'big'`, the
key has a `GT` compare type with a number key value of `5`.

The `CompareType.match(int signum)` method is the standard way to evaluate a comparison. You compute a
signum value (negative, zero, or positive) using a method like `Long.compare` or
`String.compareTo`, and then call `match` on the compare type with that signum. If the comparison
operator matches the signum, the method returns `true`.

For retrieving the actual key value, the context provides `getBoolKeyValue()`,
`getNumberKeyValue()` and `getStringKeyValue()`. Each method returns the key's value in the
appropriate Java type. Calling the wrong getter for the current key type throws a
`ClassCastException`, but this is not a concern in practice because each comparison method is only
called when the key type matches.

The context also provides two `matchForObject` methods. These are useful when your type wraps another
value and you want to delegate the comparison to the wrapped value's formatter. For example, the
`OptionalIntFormatter` delegates comparisons for number, bool and string keys to the contained `int`
value's formatter by calling `context.matchForObject(optionalInt.getAsInt(), int.class)`.


## Adding Map Key Comparison to a Typed Formatter

The most common scenario is adding `MapKeyComparator` support to a formatter that already handles
formatting for a specific type. Your formatter extends `AbstractSingleTypeParameterFormatter` (or
`AbstractParameterFormatter`) and additionally implements `MapKeyComparator<T>`. You override only the
comparison methods that make sense for your type.

The following example creates a formatter for an `HttpStatus` class that wraps an HTTP status code.
The formatter renders the status as its code number by default, but map entries allow the message
author to provide custom labels for specific codes. The comparator supports number keys (matched
against the numeric code) and string keys (matched against the reason phrase):

```java
public final class HttpStatusFormatter
    extends AbstractSingleTypeParameterFormatter<HttpStatus>
    implements MapKeyComparator<HttpStatus>
{
  @Override
  protected @NotNull Text formatValue(
      @NotNull ParameterFormatterContext context,
      @NotNull HttpStatus status)
  {
    // check number map entries first, then fall back to "code reason"
    return formatUsingMappedNumber(context, status.getCode(), true)
        .orElseGet(() -> noSpaceText(
            status.getCode() + " " + status.getReasonPhrase()));
  }

  @Override
  public @NotNull MatchResult compareToNumberKey(
      @NotNull HttpStatus status,
      @NotNull ComparatorContext context)
  {
    // compare status code against the number key
    return context.getCompareType()
        .match(Long.compare(status.getCode(), context.getNumberKeyValue()))
            ? EXACT : MISMATCH;
  }

  @Override
  public @NotNull MatchResult compareToStringKey(
      @NotNull HttpStatus status,
      @NotNull ComparatorContext context)
  {
    // compare reason phrase against the string key (case-sensitive)
    return context.getCompareType()
        .match(status.getReasonPhrase().compareTo(context.getStringKeyValue()))
            ? EQUIVALENT : MISMATCH;
  }

  @Override
  protected @NotNull FormattableType getFormattableType() {
    return new FormattableType(HttpStatus.class);
  }
}
```

With this formatter registered, the message author can map individual status codes to labels:

```java
messageSupport
    .message("%{status,200:'OK',404:'Not Found',>=500:'Server Error',:'Unknown'}")
    .with("status", HttpStatus.of(404))
    .format();
// "Not Found"
```

Status codes above or equal to 500 are matched by the `>=500` key, which uses the `GTE` compare type.
The `compareToNumberKey` method computes `Long.compare(503, 500)` which returns a positive signum, and
`GTE.match(positive)` returns `true`:

```java
messageSupport
    .message("%{status,200:'OK',404:'Not Found',>=500:'Server Error',:'Unknown'}")
    .with("status", HttpStatus.of(503))
    .format();
// "Server Error"
```

When no key matches, the default entry `:'Unknown'` acts as a fallback:

```java
messageSupport
    .message("%{status,200:'OK',404:'Not Found',>=500:'Server Error',:'Unknown'}")
    .with("status", HttpStatus.of(301))
    .format();
// "Unknown"
```

Because the formatter also supports string key comparison, the message author can match against reason
phrases if preferred:

```java
messageSupport
    .message("%{status,'OK':'success','Not Found':'missing resource'}")
    .with("status", HttpStatus.of(200))
    .format();
// "success"
```


## Implementing the Empty Key

The default `compareToEmptyKey` treats only `null` values as empty. Many custom types have their own
notion of emptiness. An empty collection, a blank string, or a container with no elements are all
conceptually "empty" but not `null`. When your type has such a concept, override `compareToEmptyKey`
so that the `empty` map key works naturally.

The following example adds empty key support to the `HttpStatus` formatter. An `HttpStatus` is
considered empty when it has no reason phrase:

```java
@Override
public @NotNull MatchResult compareToEmptyKey(
    HttpStatus status,
    @NotNull ComparatorContext context)
{
  var isEmpty = status == null ||
      status.getReasonPhrase() == null ||
      status.getReasonPhrase().isEmpty();

  return forEmptyKey(context.getCompareType(), isEmpty);
}
```

The static helper method `MatchResult.forEmptyKey(compareType, isEmpty)` handles the standard logic:
it returns `EMPTY` when the compare type is `EQ` and the value is empty, `NOT_EMPTY` when the compare
type is `NE` and the value is not empty, and `MISMATCH` in all other cases.

With this override in place, the message author can use the `empty` key to detect status objects
without a reason phrase:

```java
messageSupport
    .message("%{status,empty:'(no reason)',!empty:'%{status}'}")
    .with("status", HttpStatus.of(200, null))
    .format();
// "(no reason)"
```


## Delegating Comparison to Another Type

Some types are wrappers or adapters around a simpler value. Rather than duplicating comparison logic,
you can delegate the map key comparison to the wrapped value's formatter by calling
`context.matchForObject(wrappedValue, valueType)` on the `ComparatorContext`. This looks up the
`MapKeyComparator` registered for the wrapped value's type and uses it to perform the comparison.

The built-in `BooleanSupplierFormatter` demonstrates this approach. It wraps a `BooleanSupplier` and
delegates all comparisons to the `boolean` type's comparator:

```java
@Override
public @NotNull MatchResult compareToBoolKey(
    @NotNull BooleanSupplier booleanSupplier,
    @NotNull ComparatorContext context)
{
  // delegates to the boolean formatter's comparator
  return context.matchForObject(booleanSupplier.getAsBoolean(), boolean.class);
}

@Override
public @NotNull MatchResult compareToStringKey(
    @NotNull BooleanSupplier booleanSupplier,
    @NotNull ComparatorContext context)
{
  return context.matchForObject(booleanSupplier.getAsBoolean(), boolean.class);
}
```

This pattern is especially useful for `Optional`-like types. The `OptionalIntFormatter` delegates
comparisons for number, bool and string keys to the contained `int` value when the optional is
present, and returns `MISMATCH` when the optional is empty:

```java
@Override
public @NotNull MatchResult compareToNumberKey(
    @NotNull OptionalInt optionalInt,
    @NotNull ComparatorContext context)
{
  return optionalInt.isPresent()
      ? context.matchForObject(optionalInt.getAsInt(), int.class)
      : MISMATCH;
}
```


## Standalone Map Key Comparator

Sometimes you want to add map key matching behavior for a type that already has a satisfactory
formatter but no `MapKeyComparator` support. The `AbstractMapKeyComparator<T>` base class exists for
exactly this purpose. It implements `MapKeyComparator<T>` and provides a `format` method that always
delegates to the next formatter in the chain. Your subclass only contributes comparison logic without
affecting how the value is rendered.

This approach is useful when you cannot modify the existing formatter, or when the comparison logic
is orthogonal to the formatting logic and you want to keep them in separate classes.

The following example adds string key comparison to a hypothetical `Country` type. The existing
formatter already renders `Country` objects as their display name. The standalone comparator adds the
ability to match against ISO country codes in map entries:

```java
public final class CountryMapKeyComparator
    extends AbstractMapKeyComparator<Country>
{
  @Override
  public @NotNull MatchResult compareToStringKey(
      @NotNull Country country,
      @NotNull ComparatorContext context)
  {
    var key = context.getStringKeyValue();

    // exact match on ISO 3166-1 alpha-2 code
    if (country.getIsoCode().equalsIgnoreCase(key))
      return EXACT;

    // equivalent match on display name
    if (country.getDisplayName(context.getLocale())
        .equalsIgnoreCase(key))
      return EQUIVALENT;

    return MISMATCH;
  }

  @Override
  public @NotNull MatchResult compareToEmptyKey(
      Country country,
      @NotNull ComparatorContext context)
  {
    return forEmptyKey(context.getCompareType(),
        country == null || country.getIsoCode().isEmpty());
  }

  @Override
  public @NotNull Set<FormattableType> getFormattableTypes() {
    return Set.of(new FormattableType(Country.class));
  }
}
```

Because `AbstractMapKeyComparator` delegates formatting to the next formatter in the chain, the
existing `Country` formatter continues to produce the output text. The standalone comparator only
participates in map key resolution.

With both the formatter and the comparator registered, the message author can use map entries to map
country codes:

```java
messageSupport
    .message("%{country,'US':'United States','DE':'Germany','FR':'France',:'Other'}")
    .with("country", Country.of("DE"))
    .format();
// "Germany"
```

To ensure the standalone comparator is consulted during map key matching, register it with a
`FormattableType` order value that places it before or alongside the existing formatter. Because the
framework iterates all formatters registered for the value's type (not just the first one) when
resolving map keys, both the formatter and the comparator will participate in the matching process.
If multiple `MapKeyComparator` implementations produce a match for the same key, the engine takes the
one with the highest `MatchResult` score.


## Registration

Map key comparators are registered like any other parameter formatter. Use `addFormatter` on a
`DefaultFormatterService` instance:

```java
var formatterService = new DefaultFormatterService();
formatterService.addFormatter(new HttpStatusFormatter());
formatterService.addFormatter(new CountryMapKeyComparator());
```

The shared instance returned by `DefaultFormatterService.getSharedInstance()` is sealed and cannot be
modified. Custom comparators require a new `DefaultFormatterService` instance.

### ServiceLoader Auto-Discovery

For library authors distributing comparators as a JAR, the Java `ServiceLoader` mechanism provides
automatic registration. Create a file named
`META-INF/services/de.sayayi.lib.message.formatter.parameter.ParameterFormatter` in your resources
directory and list the fully qualified class names:

```
com.example.formatter.HttpStatusFormatter
com.example.formatter.CountryMapKeyComparator
```

When the application creates a `DefaultFormatterService`, it calls `ServiceLoader.load` for the
`ParameterFormatter` interface and registers every discovered implementation automatically.
