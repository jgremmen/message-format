# Properties Adopter

`PropertiesAdopter` reads messages and templates from Java `Properties` objects. Property keys
serve as message codes or template names, and their values are parsed as message format strings.
Unlike `ResourceBundleAdopter`, which ties directly into the Java resource bundle mechanism,
this adopter works with any `Properties` instance regardless of where it came from. This makes
it a good fit when you load localized text from custom sources such as configuration files,
databases, REST endpoints or in-memory maps.

The adopter offers three operations: adopting non-localized messages, adopting templates, and
adopting locale-aware messages from a map of properties.


## Adopting Messages

The `adopt(Properties)` method registers every entry in the given `Properties` object as a
message. Each property key becomes the message code that you later use to look up the message,
and the corresponding value is parsed as a message format string.

Because no locale is associated with the properties, the resulting messages are locale-neutral.
They will be returned regardless of which locale is requested at format time. This is
convenient for technical messages, log output or any situation where localization is not needed.

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

You can call `adopt` multiple times with different `Properties` objects. Each call adds the
entries to the same `MessagePublisher`, so all messages accumulate in one place. This is useful
if your application assembles messages from several property files or modules.

```java
var coreProps = new Properties();
coreProps.setProperty("app.start", "Application started.");
coreProps.setProperty("app.stop", "Application stopped.");
adopter.adopt(coreProps);

var errorProps = new Properties();
errorProps.setProperty("err.timeout", "Connection to %{host} timed out after %{seconds} seconds.");
errorProps.setProperty("err.auth", "Authentication failed for user %{user}.");
adopter.adopt(errorProps);

messageSupport
    .code("err.timeout")
    .with("host", "db.example.com")
    .with("seconds", 30)
    .format();
// "Connection to db.example.com timed out after 30 seconds."
```


## Adopting Templates

Templates are reusable message fragments that can be referenced from other messages using the
`%[template-name]` syntax. The `adoptTemplates(Properties)` method works like `adopt`, but
registers each entry as a template rather than a message. Property keys become template names
and their values are parsed as template format strings.

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

Because templates are shared across all messages, they are a natural way to enforce consistent
formatting patterns throughout your application. For example, you could define a template that
renders an optional suffix only when its parameter is present, and then reference it from
dozens of different messages without repeating the conditional logic.

```java
var templates = new Properties();
templates.setProperty("user-label",
    "%{name,null:'unknown',!null:'%{name}'}");
templates.setProperty("item-count",
    "%{n,0:'no items',1:'1 item',:'%{n} items'}");
adopter.adoptTemplates(templates);

messageSupport
    .message("%[user-label] has %[item-count,n->count] in the cart.")
    .with("name", "Alice")
    .with("count", 5)
    .format();
// "Alice has 5 items in the cart."

messageSupport
    .message("%[user-label] has %[item-count,n->count] in the cart.")
    .with("name", null)
    .with("count", 0)
    .format();
// "unknown has no items in the cart."
```


## Adopting Localized Messages

The `adopt(Map<Locale, Properties>)` method accepts a map where each key is a `Locale` and each
value is a `Properties` object containing messages for that locale. When the same property key
appears in multiple locale entries, all locale-specific values are combined into a single
locale-aware message. At format time the library selects the value that best matches the
requested locale.

This approach is useful when localized property files are loaded individually, for example from
a naming convention like `messages_en.properties` and `messages_de.properties`, or when
translations are fetched from a database where each locale corresponds to a separate result set.

```java
var english = new Properties();
english.setProperty("greeting", "Hello, %{name}!");
english.setProperty("farewell", "Goodbye, %{name}.");

var german = new Properties();
german.setProperty("greeting", "Hallo, %{name}!");
german.setProperty("farewell", "Auf Wiedersehen, %{name}.");

adopter.adopt(Map.of(
    Locale.ENGLISH, english,
    Locale.GERMAN, german));

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

The map can contain as many locales as needed. If only some property keys appear in a given
locale, the adopter still registers whatever is available. Messages that exist for only one
locale will simply not produce a result for the other locales.

```java
var english = new Properties();
english.setProperty("unit.meter", "meter");
english.setProperty("unit.liter", "liter");

var french = new Properties();
french.setProperty("unit.meter", "mètre");
french.setProperty("unit.liter", "litre");

var japanese = new Properties();
japanese.setProperty("unit.meter", "メートル");
japanese.setProperty("unit.liter", "リットル");

adopter.adopt(Map.of(
    Locale.ENGLISH, english,
    Locale.FRENCH, french,
    Locale.JAPANESE, japanese));

messageSupport
    .code("unit.liter")
    .locale(Locale.FRENCH)
    .format();
// "litre"

messageSupport
    .code("unit.meter")
    .locale(Locale.JAPANESE)
    .format();
// "メートル"
```

Localized entries can contain the full message format syntax, including parameters, map keys
and template references. This lets you handle locale-specific differences that go beyond simple
word substitution, such as different pluralization rules or different word order.

```java
var english = new Properties();
english.setProperty("item-summary",
    "%{count,0:'no items',1:'1 item',:'%{count} items'}");

var german = new Properties();
german.setProperty("item-summary",
    "%{count,0:'keine Einträge',1:'1 Eintrag',:'%{count} Einträge'}");

adopter.adopt(Map.of(
    Locale.ENGLISH, english,
    Locale.GERMAN, german));

messageSupport
    .code("item-summary")
    .with("count", 1)
    .locale(Locale.ENGLISH)
    .format();
// "1 item"

messageSupport
    .code("item-summary")
    .with("count", 1)
    .locale(Locale.GERMAN)
    .format();
// "1 Eintrag"

messageSupport
    .code("item-summary")
    .with("count", 7)
    .locale(Locale.GERMAN)
    .format();
// "7 Einträge"
```
