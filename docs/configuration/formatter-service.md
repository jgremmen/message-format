# Formatter Service

The formatter service is responsible for resolving the right formatter for a given parameter value. When a message is
formatted, each parameter's value needs to be converted to text. The formatter service determines which parameter 
formatter handles this conversion based on the value's type, the optional named format specified in the message and any
configuration keys present on the parameter.

In addition to parameter formatters, the formatter service also manages post formatters that transform already 
formatted text.


## Interface Hierarchy

The formatter service is defined by two interfaces. `FormatterService` is the read-only interface used during message
formatting. It provides methods to look up parameter formatters and post formatters but does not allow registration of 
new ones.

The `FormatterService.WithRegistry` sub-interface extends `FormatterService` with methods for registering parameter 
formatters and post formatters. This interface is used when setting up a formatter service and then optionally sealing 
it to prevent further modifications.


## GenericFormatterService

`GenericFormatterService` is the standard implementation of `FormatterService.WithRegistry`. It manages type-based
parameter formatters, named formatters and post formatters. Out of the box it registers a `StringFormatter` as the 
default fallback formatter, which means any value type will at minimum be formatted using its `toString()` method.

Creating a bare `GenericFormatterService` produces an empty formatter service with only the string fallback. The 
required formatters are then registered manually. This is useful when full control over which formatters are available 
is needed.

```java
var formatterService = new GenericFormatterService();
formatterService.addFormatter(new BoolFormatter());
formatterService.addFormatter(new ChoiceFormatter());

var messageSupport = MessageSupportFactory.create(formatterService);
```


## DefaultFormatterService

For most applications registering every formatter by hand is undesirable. `DefaultFormatterService` extends 
`GenericFormatterService` and automatically discovers all `ParameterFormatter` and `PostFormatter` implementations
using Java's `ServiceLoader` mechanism. This means that all formatters bundled with the library (and any third-party
formatters that are declared as service providers) are registered without any additional code. Formatters of the
library that are not declared as service providers, such as the [named formatters](../formatter/parameter/named/index.md)
`classifier`, `bitmask` and `geo`, must be registered explicitly.

Discovery works on the class path as well as on the module path. The core module declares its built-in formatters
with `provides` clauses in its `module-info.java` and additionally lists them in `META-INF/services` files, so both
deployment styles find the same set of formatters. A third-party formatter needs its own service declaration, either
a `provides de.sayayi.lib.message.formatter.parameter.ParameterFormatter with ...` clause in its module descriptor or
a `META-INF/services/de.sayayi.lib.message.formatter.parameter.ParameterFormatter` file. Post formatters are declared
the same way for `de.sayayi.lib.message.formatter.post.PostFormatter`.

A shared singleton is available via `getSharedInstance()`. This instance is created lazily and sealed so it cannot be 
modified. Its formatters are discovered with the class loader that loaded the library.

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

To use the auto-discovered formatters as a starting point while adding custom ones on top, create a new 
`DefaultFormatterService` instance instead of using the shared one. The following example adds the
`InetAddressFormatter`, which ships with the library but is not registered by default. Without it, the address would
be formatted by its `toString()` method as `/10.0.0.1`.

```java
var formatterService = new DefaultFormatterService();
formatterService.addFormatter(new InetAddressFormatter());

var messageSupport = MessageSupportFactory.create(formatterService);
messageSupport
    .message("Server: %{address}")
    .with("address",
        InetAddress.getByAddress(new byte[] { 10, 0, 0, 1 }))
    .format();
// "Server: 10.0.0.1"
```

Unless a class loader is passed explicitly, formatters are discovered with the class loader that loaded the library
(`DefaultFormatterService.class.getClassLoader()`). This makes formatters of add-on modules on the same class path
visible in environments whose system class loader does not see the application libraries, such as Spring Boot
executable jars, servlet containers or build tool plugins. The thread context class loader is deliberately not used.

/// note | Migration from 0.24
Up to version 0.24.0, `DefaultFormatterService` used the system class loader when no class loader was given.
Applications that relied on discovering formatters that are visible to the system class loader but not to the class
loader of the library must now pass that class loader to the constructor.
///

A custom `ClassLoader` can also be provided if the formatters need to be discovered from a specific location, and the
internal formatter cache size can be configured. Passing `null` as the class loader selects the class loader of the
library. The formatter cache stores the resolved list of parameter formatters for each value type so that the class
hierarchy does not need to be walked on every format call. The cache size determines how many type-to-formatter
mappings are kept and defaults to `GenericFormatterService.DEFAULT_FORMATTER_CACHE_SIZE` (256). When the cache is full,
entries are evicted using an approximate least recently used policy. The default size works well for most
applications, but it can be increased for applications that format many different value types. A constructor that
only takes the cache size is available as well.

```java
ClassLoader pluginClassLoader =
    Thread.currentThread().getContextClassLoader();

var formatterService =
    new DefaultFormatterService(pluginClassLoader, 512);
```

### Customizing Discovery

Subclasses of `DefaultFormatterService` can override `addDefaultFormatters()` or the individual 
`addParameterFormattersFromService()` and `addPostFormattersFromService()` methods to customize which formatters are
loaded during construction.


## Registering Formatters

The `WithRegistry` interface provides three methods for registering formatters.

### addFormatter

The `addFormatter` method is the most common way to register a parameter formatter. It registers the formatter for all 
types returned by the formatter's `getFormattableTypes()` method. If the formatter implements `NamedParameterFormatter`, 
it is additionally registered by name so it can be selected via `format:<name>` in message parameters.

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

Registration validates the formatter before the service is changed, so a failed registration leaves the formatter
service unchanged. A `FormatterServiceException` is thrown in the following cases:

* The formatter name or one of the names returned by `getParameterConfigNames()` does not follow the kebab-case naming
  convention.
* A named formatter with the same name has already been registered.
* The formatter is registered for `Object` but does not implement `DefaultFormatter`.
* The named formatter is applied automatically through its configuration keys (see
  [How Formatter Resolution Works](#how-formatter-resolution-works)) and one of these keys is already claimed by a
  different automatically applied formatter.

Registering a formatter clears the internal formatter cache.

### addFormatterForType

The `addFormatterForType` method registers a formatter for a specific `FormattableType`. This is useful for registering
a formatter for a type that is different from the types declared by the formatter itself, or for controlling the 
priority order explicitly.

The `FormattableType` combines a Java class with an order value between 0 and 127. A lower order value means higher
priority when multiple formatters match the same type. The default order for most types is 80
(`FormattableType.DEFAULT_ORDER`), while primitives and arrays default to 100
(`FormattableType.DEFAULT_PRIMITIVE_OR_ARRAY_ORDER`). The `Object` type is always fixed at order 127, meaning it is
always the last resort. The constructor throws an `IllegalArgumentException` for an order outside this range or for
an `Object` type with an order other than 127. Two `FormattableType` instances are equal only if both the type and the
order are equal.

```java
var formatterService = new DefaultFormatterService();

// format IPv4 addresses only, IPv6 addresses keep using toString()
formatterService.addFormatterForType(
    new FormattableType(Inet4Address.class, 50),
    new InetAddressFormatter());
```

### addPostFormatter

The `addPostFormatter` method registers a post formatter. Post formatters are identified by name and used in 
`%(name, '...', config:value)` syntax. The name must be kebab-case, and registering a second post formatter with the
same name throws a `FormatterServiceException`.

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

When a parameter is being formatted, the formatter service resolves the appropriate formatter using the following steps:

1. If the parameter specifies a `format:<name>`, the service looks up the named formatter with that name. If found and
   the formatter supports the value's type, it is the only formatter used for this parameter. Unlike type-based 
   formatters, a named formatter cannot delegate to the next formatter in the chain; the fallback mechanism described 
   in step 3 does not apply.

2. If no named format is specified (or no formatter with that name is registered, or it does not support the type),
   the service checks whether any 
   named formatter has registered itself for auto-apply based on configuration keys present on the parameter. 
   Auto-apply formatters are named formatters that return `true` from `autoApplyOnNamedConfigParameter()` and whose 
   configuration key names appear in the parameter's configuration.

3. Finally, the service resolves type-based formatters by walking the value's class hierarchy (superclasses and
   interfaces) and collecting all matching formatters in priority order. The formatter with the lowest order value is 
   tried first. A matching formatter can still decide that it is not responsible for formatting and delegate to the 
   next formatter in the list. For example, `ByteArrayFormatter` delegates when the `bytes` configuration key is absent, 
   allowing the value to be handled by a more general formatter such as `ArrayFormatter`.

The string formatter registered for `Object` ensures that there is always at least one formatter available for any 
value type.


## Sealing a Formatter Service

Once all formatters have been registered, the formatter service can be sealed. Sealing produces a read-only 
`FormatterService` view that no longer exposes the registration methods. This is useful to prevent accidental 
modifications after setup is complete.

```java
var formatterService = new DefaultFormatterService();
formatterService.addFormatter(new InetAddressFormatter());

FormatterService sealed = formatterService.seal();

// sealed offers lookups only, no registration methods
var messageSupport = MessageSupportFactory.create(sealed);
```

The shared instance returned by `DefaultFormatterService.getSharedInstance()` is always sealed. Sealing does not copy 
the formatters; the sealed instance delegates all lookups to the original service. Formatters registered later
through a retained reference to the original service are therefore visible through the sealed view as well. To make
the configuration final, discard every reference to the original service after sealing it.


## Thread Safety

`GenericFormatterService` and `DefaultFormatterService` are thread-safe. Registration and formatter lookup are guarded
by a read/write lock, so formatters can be registered while other threads format messages. The formatters
themselves are shared by all threads that use the service, so custom formatters must be thread-safe as well.
