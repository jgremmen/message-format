# Custom Post Formatter

A post formatter transforms text that has already been formatted. Unlike a parameter formatter, which
converts a Java object into its textual representation, a post formatter receives the final string
output of a sub-message and modifies it. The built-in `case` and `clip` post formatters handle common
cases like changing letter case and truncating text. When your application needs a transformation that
goes beyond what the built-in formatters offer, you can write your own.

Post formatters are invoked in messages using the `%(name, 'sub-message', ...)` syntax. The
sub-message is first formatted as a regular message, and the resulting string is then passed to the
post formatter's `format` method. The post formatter returns the transformed string, which takes the
place of the entire `%(...)` expression in the final output.


## The Interface

The `PostFormatter` interface declares two methods:

```java
public interface PostFormatter
{
  @NotNull String getName();

  @NotNull String format(@NotNull String string,
                         @NotNull PostFormatterContext context);
}
```

The `getName()` method returns the name that identifies the post formatter in the message syntax.
This name must follow the kebab-case naming convention (lowercase letters and digits separated by
single hyphens) and must be unique across all registered post formatters.

The `format` method receives the already formatted sub-message text and the `PostFormatterContext`.
It returns the transformed string. The framework guarantees that `string` is never empty. If the
sub-message produces an empty result, the post formatter is not invoked at all and the empty string
is returned directly.

The input string passed to `format` is the trimmed result of formatting the sub-message, so it
never has leading or trailing whitespace. The same trimming is applied to the string returned by
`format`. Any leading or trailing spaces your implementation adds to the result will be stripped
by the framework before the text is inserted into the surrounding message.


## The Context

The `PostFormatterContext` extends `ConfigAccessor` and provides access to the configuration keys
declared in the message syntax. It also exposes the locale for which the message is being formatted.
The typed accessor methods are the same ones used by parameter formatters:

`getConfigValueString(name)` returns a string configuration value,
`getConfigValueNumber(name)` returns a numeric value as `OptionalLong`, and
`getConfigValueBool(name)` returns a boolean value. Each returns an empty `Optional` when the key
is absent or when the stored value type does not match the requested type.

Configuration keys specified inline in the message always take precedence over global defaults
registered via `setDefaultConfig` on `ConfigurableMessageSupport`.


## A Complete Example: Masking Sensitive Data

Suppose your application formats messages that include sensitive information like credit card numbers,
email addresses or account identifiers. In production logs or user-facing error messages, you want to
partially redact these values so that they remain recognizable without exposing the full content. This
is a text transformation that operates on already formatted output, which makes it a natural fit for
a post formatter.

The following implementation masks a portion of the input text by replacing characters with a
configurable mask character. It reads the number of visible characters to keep at the end from a
`mask-show` configuration key, and optionally a custom mask character from `mask-char`:

```java
public final class MaskPostFormatter implements PostFormatter
{
  @Override
  public @NotNull String getName() {
    return "mask";
  }

  @Override
  public @NotNull String format(@NotNull String string,
                                @NotNull PostFormatterContext context)
  {
    var show = (int)context.getConfigValueNumber("mask-show").orElse(4);
    var maskChar = context.getConfigValueString("mask-char").orElse("*");

    if (show >= string.length())
      return string;

    var masked = maskChar.repeat(string.length() - show);

    return masked + string.substring(string.length() - show);
  }
}
```

With this formatter registered, a message author can redact sensitive values while preserving a
recognizable suffix:

```java
messageSupport
    .message("Card: %(mask,'%{card}')")
    .with("card", "4111222233334444")
    .format();
// "Card: ************4444"
```

The `mask-char` key lets the message author choose a different mask character when asterisks are
not appropriate:

```java
messageSupport
    .message("Account: %(mask,'%{acct}',mask-show:3,mask-char:'X')")
    .with("acct", "NL91ABNA0417164300")
    .format();
// "Account: XXXXXXXXXXXXXXX300"
```

The same approach works for other types of sensitive data. Here the domain part of an email
address remains visible while the local part is masked:

```java
messageSupport
    .message("Email: %(mask,'%{email}',mask-show:12)")
    .with("email", "john.doe@example.com")
    .format();
// "Email: ********@example.com"
```


## Using the Locale

The `PostFormatterContext` provides the formatting locale through `getLocale()`. This is important
when your transformation involves locale-sensitive operations like collation, case conversion or
number formatting.

The following example implements a `slug` post formatter that converts text into a URL-friendly slug
by lowercasing, replacing whitespace with hyphens and removing non-alphanumeric characters. The
lowercasing must use the formatting locale to produce correct results for all languages:

```java
public final class SlugPostFormatter implements PostFormatter
{
  @Override
  public @NotNull String getName() {
    return "slug";
  }

  @Override
  public @NotNull String format(@NotNull String string,
                                @NotNull PostFormatterContext context)
  {
    return string
        .toLowerCase(context.getLocale())
        .replaceAll("\\s+", "-")
        .replaceAll("[^a-z0-9\\-]", "");
  }
}
```

```java
messageSupport
    .message("%(slug,'%{title}')")
    .with("title", "Custom Post Formatter Guide")
    .locale(Locale.US)
    .format();
// "custom-post-formatter-guide"
```

Note that this `slug` formatter does not use any configuration keys. Not every post formatter
needs them. If the transformation is self-contained, the method can ignore the context entirely
and work with just the input string.


## Registration

Post formatters are registered on the formatter service through the `addPostFormatter` method. The
shared instance returned by `DefaultFormatterService.getSharedInstance()` is sealed and cannot be
modified. To register custom post formatters, create a new `DefaultFormatterService` instance:

```java
var formatterService = new DefaultFormatterService();
formatterService.addPostFormatter(new MaskPostFormatter());
formatterService.addPostFormatter(new SlugPostFormatter());

var messageSupport = MessageSupportFactory.create(formatterService);
```

Each post formatter must have a unique name. Attempting to register two post formatters with the same
name will result in an exception.

### ServiceLoader Auto-Discovery

For library authors who distribute their post formatters as a JAR, the Java `ServiceLoader` mechanism
provides automatic registration. Create a file named
`META-INF/services/de.sayayi.lib.message.formatter.post.PostFormatter` in your resources directory
and list the fully qualified class names of your post formatters, one per line:

```
com.example.formatter.MaskPostFormatter
com.example.formatter.SlugPostFormatter
```

When the application creates a `DefaultFormatterService`, it calls `ServiceLoader.load` for the
`PostFormatter` interface and registers every discovered implementation alongside the built-in
`case` and `clip` post formatters.
