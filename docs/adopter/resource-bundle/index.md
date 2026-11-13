---
icon: material/comment-text-multiple-outline
---

# ResourceBundle Adopter

`ResourceBundleAdopter` reads messages and templates from Java `ResourceBundle` instances. Each key in a bundle
becomes a message code or template name and its value is parsed as a message or template format string. When the same
code or template name appears in bundles for different locales, the localized values are automatically combined into
a single locale-aware message or template. This makes the adopter a natural fit for applications that already
organize their translations as `.properties` files following the standard Java resource bundle naming convention.

The adopter provides several ways to load bundles: by base name and a set of locales, from a single `ResourceBundle`
instance, or from a collection of bundles.

Resource bundle entries can also be adopted as reusable templates with the corresponding `adoptTemplates` methods.
Template keys must follow the library's kebab-case naming convention. When a template name is present in bundles for
multiple locales, the localized values are combined into one locale-aware template.


## Adopting Templates

Template adoption supports the same sources and lookup options as message adoption. For example, a bundle base name
and a set of locales can be specified to require the bundles for those locales:

```properties
# com/example/templates_en.properties
greeting=Hello, %{name}!
```

```properties
# com/example/templates_de.properties
greeting=Hallo, %{name}!
```

The examples on this page use the following message support.

```java title="Example setup"
import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.adopter.ResourceBundleAdopter;
import de.sayayi.lib.message.formatter.DefaultFormatterService;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

var messageSupport = MessageSupportFactory.createDefault();
// all built-in formatters are available
```

```java
adopter.adoptTemplates("com.example.templates",
    Set.of(Locale.ENGLISH, Locale.GERMAN));

messageSupport
    .message("%[greeting]")
    .with("name", "Alice")
    .locale(Locale.GERMAN)
    .format();
// "Hallo, Alice!"
```

Templates can likewise be adopted from a single `ResourceBundle` or a collection of bundles with
`adoptTemplates(ResourceBundle)` and `adoptTemplates(Collection)`. A custom class loader can be supplied to the
base-name overloads just as with messages.


## Adopting by Bundle Base Name

To load a family of bundle files, pass the bundle base name and the set of locales the application supports to the
`adopt(String, Set)` method. Bundles are looked up with `ResourceBundle.getBundle`, and entries that share the same key
are combined into locale-aware messages. Keys inherited from parent bundles are adopted for the requested locale as
well.

Suppose a project contains two property files on the classpath:

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

A single call to `adopt` loads both files:

```java
var messageSupport = MessageSupportFactory.create(
    DefaultFormatterService.getSharedInstance());
var adopter = new ResourceBundleAdopter(messageSupport);

adopter.adopt("com.example.messages",
    Set.of(Locale.ENGLISH, Locale.GERMAN));

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

A `MessageAdopterException` is thrown when the bundle for one of the requested locales cannot be found. A bundle for a
parent locale, the JVM default locale or the base bundle (`messages.properties`) does not count as a bundle for the
requested locale. This turns missing translations into an early, visible error rather than a silent gap that only
surfaces at runtime when a user requests a particular locale.

```java
adopter.adopt("com.example.messages",
    Set.of(Locale.ENGLISH, Locale.GERMAN, Locale.FRENCH));
// Throws MessageAdopterException if messages_fr.properties is missing
```

### Custom ClassLoader

When the bundle files are not accessible from the adopter's own class loader, for example when loading resources from a
plugin or module system, a custom `ClassLoader` can be provided as a third argument:

```java
adopter.adopt("com.example.messages",
    Set.of(Locale.ENGLISH, Locale.GERMAN),
    pluginClassLoader);
```


## Adopting a Single ResourceBundle

When a `ResourceBundle` instance is already in hand, it can be adopted directly using `adopt(ResourceBundle)`.
Every key in the bundle is registered as a message. Because each message receives only a single text, the locale of
the bundle is not retained: the message is a plain message that is used for every requested locale. To combine
several locales into locale-aware messages, adopt the bundles together with `adopt(Collection)`.

This is useful when the bundle was obtained through custom lookup logic, or to adopt only a single locale
from a bundle family.

```java
var messageSupport = MessageSupportFactory.create(
    DefaultFormatterService.getSharedInstance());
var adopter = new ResourceBundleAdopter(messageSupport);

var bundle = ResourceBundle.getBundle("com.example.messages", 
    Locale.FRENCH);
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

The `adopt(Collection)` method accepts a collection of `ResourceBundle` instances and merges them into locale-aware 
messages. When the same message code appears in multiple bundles, each bundle contributes its locale-specific text.
This is the recommended approach when bundles are constructed manually rather than relying on base-name resolution.

```java
var bundles = List.of(
    ResourceBundle.getBundle("com.example.messages", Locale.ENGLISH),
    ResourceBundle.getBundle("com.example.messages", Locale.GERMAN));
adopter.adopt(bundles);

// Assumes messages_en.properties contains:
//   item-count=%{count,format:choice,1:'1 item',:'%{count} items'}
// and messages_de.properties contains:
//   item-count=%{count,format:choice,1:'1 Eintrag',
//     :'%{count} Einträge'}
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

Bundles from entirely different base names can also be combined into a single message support instance. This is helpful
when an application organizes messages into separate bundle families by feature area and all of them need to be merged
at startup.

```java
var bundles = List.of(
    ResourceBundle.getBundle("com.example.ui", Locale.ENGLISH),
    ResourceBundle.getBundle("com.example.ui", Locale.GERMAN),
    ResourceBundle.getBundle("com.example.errors", Locale.ENGLISH),
    ResourceBundle.getBundle("com.example.errors", Locale.GERMAN));
adopter.adopt(bundles);
```

Because the adopter groups entries by message code across all bundles, codes that only exist in one bundle family
still work correctly. The resulting messages simply have fewer locale variants than codes that appear in every bundle.
