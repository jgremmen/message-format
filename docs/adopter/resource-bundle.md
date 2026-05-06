# ResourceBundle Adopter

`ResourceBundleAdopter` reads messages from Java `ResourceBundle` instances. Each key in a
bundle becomes a message code and its value is parsed as a message format string. When the same
code appears in bundles for different locales, the localized values are automatically combined
into a single locale-aware message. This makes the adopter a natural fit for applications that
already organize their translations as `.properties` files following the standard Java resource
bundle naming convention.

The adopter provides several ways to load bundles: by base name (scanning all available locales
or a specific set), from a single `ResourceBundle` instance, or from a collection of bundles.


## Adopting by Bundle Base Name

The simplest approach is to pass a bundle base name to the `adopt(String)` method. The adopter
resolves bundles for every locale available to the JVM and silently ignores any locale for which
no bundle file exists.

Suppose your project contains two property files on the classpath:

```properties
# com/example/messages_en.properties
greeting=Hello, %{name}!
farewell=Goodbye, %{name}.
```

```properties
# com/example/messages_de.properties
greeting=Hallo, %{name}!
farewell=Auf Wiedersehen, %{name}.
```

A single call to `adopt` loads both files and combines entries that share the same key into
locale-aware messages:

```java
var messageSupport = MessageSupportFactory.create(
    DefaultFormatterService.getSharedInstance());
var adopter = new ResourceBundleAdopter(messageSupport);

adopter.adopt("com.example.messages");

messageSupport
    .code("greeting")
    .with("name", "Alice")
    .locale(Locale.ENGLISH)
    .format();
// "Hello, Alice!"

messageSupport
    .code("greeting")
    .with("name", "Alice")
    .locale(Locale.GERMAN)
    .format();
// "Hallo, Alice!"
```

### Custom ClassLoader

When the bundle files are not accessible from the adopter's own class loader, for example when
loading resources from a plugin or module system, you can provide a custom `ClassLoader` as a
second argument:

```java
adopter.adopt("com.example.messages", pluginClassLoader);
```

The behavior is otherwise identical: all available locales are scanned and missing bundles are
silently skipped.


## Adopting with Specific Locales

If you know exactly which locales your application supports, you can pass a `Set<Locale>` to
the `adopt(String, Set)` method. Unlike the base-name-only variant, this one throws a
`MessageAdopterException` when a bundle for one of the requested locales cannot be found. This
turns missing translations into an early, visible error rather than a silent gap that only
surfaces at runtime when a user requests a particular locale.

```java
adopter.adopt("com.example.messages",
    Set.of(Locale.ENGLISH, Locale.GERMAN, Locale.FRENCH));
// Throws MessageAdopterException if messages_fr.properties is missing
```

This variant also accepts an optional `ClassLoader` for the same scenarios described above:

```java
adopter.adopt("com.example.messages",
    Set.of(Locale.ENGLISH, Locale.GERMAN),
    pluginClassLoader);
```


## Adopting a Single ResourceBundle

When you already have a `ResourceBundle` instance in hand, you can adopt it directly using
`adopt(ResourceBundle)`. Every key in the bundle is registered as a message associated with the
bundle's locale.

This is useful when the bundle was obtained through custom lookup logic, or when you want to
adopt only a single locale from a bundle family without scanning the rest.

```java
var messageSupport = MessageSupportFactory.create(
    DefaultFormatterService.getSharedInstance());
var adopter = new ResourceBundleAdopter(messageSupport);

var bundle = ResourceBundle.getBundle("com.example.messages", Locale.FRENCH);
adopter.adopt(bundle);

// Assumes messages_fr.properties contains:
//   farewell=Au revoir, %{name}!
messageSupport
    .code("farewell")
    .with("name", "Pierre")
    .locale(Locale.FRENCH)
    .format();
// "Au revoir, Pierre!"
```


## Adopting a Collection of Bundles

The `adopt(Collection)` method accepts a collection of `ResourceBundle` instances and merges
them into locale-aware messages. When the same message code appears in multiple bundles, each
bundle contributes its locale-specific text. This is the recommended approach when you
construct bundles manually rather than relying on base-name resolution.

```java
var bundles = List.of(
    ResourceBundle.getBundle("com.example.messages", Locale.ENGLISH),
    ResourceBundle.getBundle("com.example.messages", Locale.GERMAN));
adopter.adopt(bundles);

// Assumes messages_en.properties contains:
//   item-count=%{count,1:'1 item',:'%{count} items'}
// and messages_de.properties contains:
//   item-count=%{count,1:'1 Eintrag',:'%{count} Einträge'}
messageSupport
    .code("item-count")
    .with("count", 3)
    .locale(Locale.GERMAN)
    .format();
// "3 Einträge"

messageSupport
    .code("item-count")
    .with("count", 1)
    .locale(Locale.ENGLISH)
    .format();
// "1 item"
```

You can also combine bundles from entirely different base names into a single message support
instance. This is helpful when your application organizes messages into separate bundle families
by feature area and you want to merge them all at startup.

```java
var bundles = List.of(
    ResourceBundle.getBundle("com.example.ui", Locale.ENGLISH),
    ResourceBundle.getBundle("com.example.ui", Locale.GERMAN),
    ResourceBundle.getBundle("com.example.errors", Locale.ENGLISH),
    ResourceBundle.getBundle("com.example.errors", Locale.GERMAN));
adopter.adopt(bundles);
```

Because the adopter groups entries by message code across all bundles, codes that only exist in
one bundle family still work correctly. The resulting messages simply have fewer locale variants
than codes that appear in every bundle.
