# The Formatter Service

The formatter service is responsible for resolving the right formatter for a given parameter
value. When a message is formatted, each parameter's value needs to be converted to text. The
formatter service determines which parameter formatter handles this conversion based on the
value's type, the optional named format specified in the message, and any configuration keys
present on the parameter.

In addition to parameter formatters, the formatter service also manages post formatters that
transform already formatted text.


## The Interface Hierarchy

The formatter service is defined by two interfaces. `FormatterService` is the read-only
interface used during message formatting. It provides methods to look up parameter formatters
and post formatters but does not allow registration of new ones.

The `FormatterService.WithRegistry` sub-interface extends `FormatterService` with methods for
registering parameter formatters and post formatters. You use this interface when setting up
your formatter service, and then optionally seal it to prevent further modifications.


## GenericFormatterService

`GenericFormatterService` is the standard implementation of `FormatterService.WithRegistry`.
It manages type-based parameter formatters, named formatters and post formatters. Out of the
box it registers a `StringFormatter` as the default fallback formatter, which means any value
type will at minimum be formatted using its `toString()` method.

Creating a bare `GenericFormatterService` gives you an empty formatter service with only the
string fallback. You would then register the formatters you need manually. This is useful when
you want full control over which formatters are available.

```java
var formatterService = new GenericFormatterService();
formatterService.addFormatter(new BoolFormatter());
formatterService.addFormatter(new ChoiceFormatter());

var messageSupport = MessageSupportFactory.create(formatterService);
```


## DefaultFormatterService

For most applications you do not want to register every formatter by hand.
`DefaultFormatterService` extends `GenericFormatterService` and automatically discovers all
`ParameterFormatter` and `PostFormatter` implementations on the classpath using Java's
`ServiceLoader` mechanism. This means that all formatters bundled with the library (and any
third-party formatters that follow the `ServiceLoader` convention) are registered without any
additional code.

A shared singleton is available via `getSharedInstance()`. This instance is created lazily
and sealed so it cannot be modified.

```java
FormatterService shared = DefaultFormatterService.getSharedInstance();

var messageSupport = MessageSupportFactory.create(shared);
messageSupport
    .message("%{price}")
    .with("price", 49.95)
    .locale(Locale.US)
    .format();
// "49.95"
```

If you need the auto-discovered formatters as a starting point but want to add your own on
top, create a new `DefaultFormatterService` instance instead of using the shared one.

```java
var formatterService = new DefaultFormatterService();
formatterService.addFormatter(new MyCustomFormatter());

var messageSupport = MessageSupportFactory.create(formatterService);
```

You can also provide a custom `ClassLoader` if the formatters need to be discovered from a
specific location, and configure the internal formatter cache size. The formatter cache stores
the resolved list of parameter formatters for each value type so that the class hierarchy does
not need to be walked on every format call. The cache size determines how many type-to-formatter
mappings are kept; the least frequently used entries are evicted when the cache is full. The
default size works well for most applications, but you can increase it if your application
formats many different value types.

```java
var formatterService = new DefaultFormatterService(
    getClass().getClassLoader(), 512);
```

### Customizing Discovery

Subclasses of `DefaultFormatterService` can override `addDefaultFormatters()` or the
individual `addParameterFormattersFromService()` and `addPostFormattersFromService()` methods
to customize which formatters are loaded during construction.


## Registering Formatters

The `WithRegistry` interface provides three methods for registering formatters.

### addFormatter

The `addFormatter` method is the most common way to register a parameter formatter. It
registers the formatter for all types returned by the formatter's `getFormattableTypes()`
method. If the formatter implements `NamedParameterFormatter`, it is additionally registered
by name so it can be selected via `format:<name>` in message parameters.

```java
var formatterService = new GenericFormatterService();
formatterService.addFormatter(new BoolFormatter());

var messageSupport = MessageSupportFactory.create(formatterService);
messageSupport
    .message("%{flag,format:bool,true:'yes',false:'no'}")
    .with("flag", 2)
    .format();
// "yes"
```

Formatter names and parameter configuration names are validated to follow the kebab-case
naming convention. An exception is thrown if the name does not match.

### addFormatterForType

The `addFormatterForType` method registers a formatter for a specific `FormattableType`. This
is useful when you want to register a formatter for a type that is different from the types
declared by the formatter itself, or when you want to control the priority order explicitly.

The `FormattableType` combines a Java class with an order value between 0 and 127. A lower
order value means higher priority when multiple formatters match the same type. The default
order for most types is 80, while primitives and arrays default to 100. The `Object` type is
always fixed at order 127, meaning it is always the last resort.

```java
var formatterService = new GenericFormatterService();
formatterService.addFormatterForType(
    new FormattableType(Currency.class, 50),
    new MyCurrencyFormatter());
```

### addPostFormatter

The `addPostFormatter` method registers a post formatter. Post formatters are identified by
name and used in `%(name, '...', config:value)` syntax. Duplicate registrations are not
allowed.

```java
var formatterService = new GenericFormatterService();
formatterService.addPostFormatter(new CasePostFormatter());
formatterService.addPostFormatter(new ClipPostFormatter());

var messageSupport = MessageSupportFactory.create(formatterService);
messageSupport
    .message("%(case,'%{name}',case:upper)")
    .with("name", "alice")
    .format();
// "ALICE"
```


## How Formatter Resolution Works

When a parameter is being formatted, the formatter service resolves the appropriate formatter
using the following steps:

1. If the parameter specifies a `format:<name>`, the service looks up the named formatter with
   that name. If found and the formatter supports the value's type, it is the only formatter
   used for this parameter. Unlike type-based formatters, a named formatter cannot delegate to
   the next formatter in the chain; the fallback mechanism described in step 3 does not apply.

2. If no named format is specified (or the named formatter does not support the type), the
   service checks whether any named formatter has registered itself for auto-apply based on
   configuration keys present on the parameter. Auto-apply formatters are named formatters
   that return `true` from `autoApplyOnNamedConfigParameter()` and whose configuration key
   names appear in the parameter's configuration.

3. Finally, the service resolves type-based formatters by walking the value's class hierarchy
   (superclasses and interfaces) and collecting all matching formatters in priority order. The
   formatter with the lowest order value is tried first. A matching formatter can still decide
   that it is not responsible for formatting and delegate to the next formatter in the list.
   For example, `ByteArrayFormatter` delegates when the `bytes` configuration key is absent, 
   allowing the value to be handled by a more general formatter such as `ArrayFormatter`.

The string formatter registered for `Object` ensures that there is always at least one
formatter available for any value type.


## Sealing a Formatter Service

Once you have finished registering all formatters, you can seal the formatter service. Sealing
produces an immutable `FormatterService` instance that no longer exposes the registration
methods. This is useful to prevent accidental modifications after setup is complete.

```java
var formatterService = new DefaultFormatterService();
formatterService.addFormatter(new MyCustomFormatter());

FormatterService sealed = formatterService.seal();

var messageSupport = MessageSupportFactory.create(sealed);
```

The shared instance returned by `DefaultFormatterService.getSharedInstance()` is always
sealed. Sealing does not copy the formatters; the sealed instance delegates all lookups to the
original service.
