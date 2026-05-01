# Message Adopters

Message adopters read messages and templates from external sources and publish them to a
message support instance. They bridge external formats—such as Java resource bundles or
properties files—with the message format library so that existing localization infrastructure
can be reused.


## AbstractMessageAdopter

`AbstractMessageAdopter` is the base class for all adopters. It holds a `MessageFactory` for
parsing message format strings and a `MessagePublisher` for publishing the resulting messages
and templates.

Concrete adopters provide one or more `adopt` methods specific to their source format. You
typically construct an adopter by passing a `ConfigurableMessageSupport`, which serves as both
the message factory source and the publisher:

```java
var messageSupport = MessageSupportFactory.create(
    DefaultFormatterService.getSharedInstance());

var adopter = new PropertiesAdopter(messageSupport);
```

If you need to separate the factory from the publisher (for example when collecting messages
before publishing them), use the two-argument constructor:

```java
var adopter = new ResourceBundleAdopter(messageFactory, publisher);
```


## ResourceBundleAdopter

`ResourceBundleAdopter` reads messages from Java `ResourceBundle` instances. Each key in the
bundle is used as the message code and its value is parsed as a message format string. When
the same code appears in bundles for different locales, the localized values are automatically
combined into a single locale-aware message.

### Adopting by bundle base name

The simplest approach is providing a bundle base name. The adopter resolves bundles for all
available locales and silently ignores locales for which no bundle exists:

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
// Assumes messages_en.properties contains: greeting=Hello, %{name}!
// "Hello, Alice!"
```

### Adopting with specific locales

If you know exactly which locales your application supports, pass a `Set<Locale>`. A
`MessageAdopterException` is thrown if a bundle for any of the requested locales is missing:

```java
adopter.adopt("com.example.messages",
    Set.of(Locale.ENGLISH, Locale.GERMAN));
```

### Adopting a single ResourceBundle directly

If you already have a `ResourceBundle` instance, you can adopt it directly:

```java
var bundle = ResourceBundle.getBundle("com.example.messages", Locale.FRENCH);
adopter.adopt(bundle);

messageSupport
    .code("farewell")
    .with("name", "Bob")
    .locale(Locale.FRENCH)
    .format();
// Assumes messages_fr.properties contains: farewell=Au revoir, %{name}!
// "Au revoir, Bob!"
```

### Adopting a collection of bundles

Pass a collection of bundles to combine multiple locales into locale-aware messages:

```java
var bundles = List.of(
    ResourceBundle.getBundle("com.example.messages", Locale.ENGLISH),
    ResourceBundle.getBundle("com.example.messages", Locale.GERMAN));
adopter.adopt(bundles);

messageSupport
    .code("item-count")
    .with("count", 3)
    .locale(Locale.GERMAN)
    .format();
// Assumes messages_de.properties contains: item-count=%{count} Einträge
// "3 Einträge"
```


## PropertiesAdopter

`PropertiesAdopter` reads messages and templates from `Properties` objects. Property keys
serve as message codes (or template names) and values are parsed as message format strings.

### Adopting messages from a Properties object

```java
var messageSupport = MessageSupportFactory.create(
    DefaultFormatterService.getSharedInstance());
var adopter = new PropertiesAdopter(messageSupport);

var props = new Properties();
props.setProperty("welcome", "Welcome, %{user}!");
props.setProperty("balance", "Your balance is %{amount}.");
adopter.adopt(props);

messageSupport
    .code("welcome")
    .with("user", "Charlie")
    .format();
// "Welcome, Charlie!"

messageSupport
    .code("balance")
    .with("amount", 42.50)
    .locale(Locale.US)
    .format();
// "Your balance is 42.5."
```

### Adopting templates from a Properties object

Templates are adopted separately using `adoptTemplates`. Property keys become template names:

```java
var templateProps = new Properties();
templateProps.setProperty("currency", "%{amount,number:'#,##0.00'}");
adopter.adoptTemplates(templateProps);

messageSupport
    .message("Total: %[currency]")
    .with("amount", 1234.5)
    .locale(Locale.US)
    .format();
// "Total: 1,234.50"
```

### Adopting localized messages from multiple Properties

The `adopt(Map<Locale, Properties>)` method accepts a map of properties keyed by locale.
Properties that share the same key across locales are combined into a single locale-aware
message:

```java
var english = new Properties();
english.setProperty("color", "Color: %{name}");

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
// "Color: red"

messageSupport
    .code("color")
    .with("name", "rot")
    .locale(Locale.GERMAN)
    .format();
// "Farbe: rot"
```
