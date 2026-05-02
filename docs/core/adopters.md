# Message Adopters

Adopters are the bridge between external message sources and the message format library. They
read messages and templates from formats you may already have in your project, such as Java
resource bundles or properties files, parse the values as message format strings, and publish the
results to a `MessageSupport` instance. This lets you reuse existing localization infrastructure
without manually re-registering every message in code.

The core library ships with two concrete adopters: `ResourceBundleAdopter` for Java
`ResourceBundle` instances and `PropertiesAdopter` for `Properties` objects. Additional adopters
for annotation-based message definitions are provided by the
[Annotations](../annotations/adopter.md), [ASM](../asm/index.md) and
[Spring](../spring/index.md) modules.


## AbstractMessageAdopter

All adopters extend `AbstractMessageAdopter`, which holds the two collaborators every adopter
needs: a `MessageFactory` for parsing message format strings into `Message` objects, and a
`MessagePublisher` for storing the parsed messages and templates.

The most common way to construct an adopter is by passing a `ConfigurableMessageSupport`. Because
`ConfigurableMessageSupport` implements `MessagePublisher` and provides access to a
`MessageFactory` through its message accessor, a single argument is sufficient:

```java
var messageSupport = MessageSupportFactory.create(
    DefaultFormatterService.getSharedInstance());

var adopter = new PropertiesAdopter(messageSupport);
```

When you need to decouple factory and publisher, for example to collect messages before
publishing them to multiple targets, every adopter also offers a two-argument constructor:

```java
var adopter = new ResourceBundleAdopter(messageFactory, publisher);
```


## ResourceBundleAdopter

`ResourceBundleAdopter` reads messages from Java `ResourceBundle` instances. Each key in a
bundle becomes a message code and its value is parsed as a message format string. When the same
code appears in bundles for different locales, the localized values are automatically combined
into a single locale-aware message.

### Adopting by Bundle Base Name

The simplest approach is to provide a bundle base name. The adopter resolves bundles for all
locales available to the JVM and silently ignores any locale for which no bundle exists. This is
convenient during development when you may not have translations for every locale yet:

```java
var messageSupport = MessageSupportFactory.create(
    DefaultFormatterService.getSharedInstance());
var adopter = new ResourceBundleAdopter(messageSupport);

adopter.adopt("com.example.messages");

// Assumes messages_en.properties contains:
//   greeting=Hello, %{name}!
messageSupport
    .code("greeting")
    .with("name", "Alice")
    .locale(Locale.ENGLISH)
    .format();
// "Hello, Alice!"
```

A custom `ClassLoader` can be provided as a second argument when the bundles are not accessible
from the adopter's own class loader, for example when loading resources from a plugin or module
system:

```java
adopter.adopt("com.example.messages", pluginClassLoader);
```

### Adopting with Specific Locales

If you know exactly which locales your application supports, you can pass a `Set<Locale>` instead
of scanning all available locales. When a bundle for one of the requested locales cannot be
found, the adopter throws a `MessageAdopterException` rather than silently skipping it. This
turns missing translations into an early, visible error:

```java
adopter.adopt("com.example.messages",
    Set.of(Locale.ENGLISH, Locale.GERMAN, Locale.FRENCH));
```

The specific-locales variant also accepts an optional `ClassLoader`:

```java
adopter.adopt("com.example.messages",
    Set.of(Locale.ENGLISH, Locale.GERMAN),
    pluginClassLoader);
```

### Adopting a Single ResourceBundle

When you already have a `ResourceBundle` instance, you can adopt it directly. Every key in the
bundle is registered as a message associated with the bundle's locale:

```java
var bundle = ResourceBundle.getBundle("com.example.messages", Locale.FRENCH);
adopter.adopt(bundle);

// Assumes messages_fr.properties contains:
//   farewell=Au revoir, %{name}!
messageSupport
    .code("farewell")
    .with("name", "Bob")
    .locale(Locale.FRENCH)
    .format();
// "Au revoir, Bob!"
```

### Adopting a Collection of Bundles

Passing a collection of `ResourceBundle` instances combines them into locale-aware messages. If
the same message code appears in multiple bundles, each bundle contributes its locale-specific
text. This is the recommended approach when constructing bundles manually rather than relying on
base-name resolution:

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


## PropertiesAdopter

`PropertiesAdopter` reads messages and templates from `Properties` objects. Property keys serve
as message codes or template names, and their values are parsed as message format strings. This
adopter is useful when you load properties from custom sources such as configuration files,
databases or in-memory maps.

### Adopting Messages

The `adopt` method takes a `Properties` object and registers each entry as a message:

```java
var messageSupport = MessageSupportFactory.create(
    DefaultFormatterService.getSharedInstance());
var adopter = new PropertiesAdopter(messageSupport);

var props = new Properties();
props.setProperty("welcome", "Welcome, %{user}!");
props.setProperty("logout", "%{user} has logged out.");
adopter.adopt(props);

messageSupport
    .code("welcome")
    .with("user", "Charlie")
    .format();
// "Welcome, Charlie!"
```

### Adopting Templates

Templates are adopted separately through the `adoptTemplates` method. Property keys become
template names and their values are parsed as template format strings:

```java
var templateProps = new Properties();
templateProps.setProperty("opt-detail",
    "%{detail,!empty:' (%{detail})'}");
adopter.adoptTemplates(templateProps);

messageSupport
    .message("Task completed%[opt-detail]")
    .with("detail", "3 warnings")
    .format();
// "Task completed (3 warnings)"

messageSupport
    .message("Task completed%[opt-detail]")
    .with("detail", "")
    .format();
// "Task completed"
```

### Adopting Localized Messages

The `adopt(Map<Locale, Properties>)` method accepts a map of properties keyed by locale. Entries
that share the same property key across different locales are combined into a single locale-aware
message. This is useful when localized properties are loaded from separate files or data sources:

```java
var english = new Properties();
english.setProperty("color", "Colour: %{name}");

var german = new Properties();
german.setProperty("color", "Farbe: %{name}");

adopter.adopt(Map.of(
    Locale.ENGLISH, english,
    Locale.GERMAN, german));

messageSupport
    .code("color")
    .with("name", "red")
    .locale(Locale.ENGLISH)
    .format();
// "Colour: red"

messageSupport
    .code("color")
    .with("name", "rot")
    .locale(Locale.GERMAN)
    .format();
// "Farbe: rot"
```
