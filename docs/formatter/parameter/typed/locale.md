# Locale

This formatter is included in the `DefaultFormatterService`.

The `LocaleFormatter` is a type-based formatter registered for `java.util.Locale`. It is automatically selected
whenever a parameter value is a `Locale`. By default, it outputs the locale's display name, localized to the
formatting context's locale.

The `locale` configuration key controls which aspect of the locale to display:

| Value                  | Output                                                        |
|------------------------|---------------------------------------------------------------|
| `name` (default)       | Full display name (e.g. "English (United Kingdom)")           |
| `country`              | Country display name (e.g. "United Kingdom")                  |
| `lang` or `language`   | Language display name (e.g. "English")                        |
| `script`               | Script display name (e.g. "Latin")                            |
| `variant`              | Variant display name                                          |

All display names are resolved using the formatting context's locale, so the output language depends on
the locale set on the `MessageSupport` instance, not on the parameter value itself.


## Display Name

When the `locale` configuration key is absent or set to `name`, the formatter outputs the full display name
of the locale.

```java
messageSupport.setLocale(Locale.forLanguageTag("nl-BE"));

messageSupport
    .message("%{loc}")
    .with("loc", Locale.UK)
    .format();
// "Engels (Verenigd Koninkrijk)"

messageSupport
    .message("%{loc}")
    .with("loc", Locale.GERMAN)
    .format();
// "Duits"

messageSupport
    .message("%{loc}")
    .with("loc", Locale.KOREA)
    .format();
// "Koreaans (Zuid-Korea)"
```


## Country

When `locale` is set to `country`, the formatter outputs the country display name. The country code (ISO 3166
two-letter code such as `US`, `GB`, `FR`) is first checked against string map keys in the parameter configuration.
If a match is found, the mapped message is used instead of the JDK display name.

```java
messageSupport.setLocale(Locale.FRANCE);

messageSupport
    .message("%{loc,locale:country}")
    .with("loc", Locale.US)
    .format();
// "États-Unis"
```

String map keys allow overriding the display name for specific country codes.

```java
messageSupport.setLocale(Locale.FRANCE);

messageSupport
    .message("%{loc,locale:country,'GB':'The Great Kingdom'}")
    .with("loc", Locale.UK)
    .format();
// "The Great Kingdom"
```


## Language

When `locale` is set to `language` (or its alias `lang`), the formatter outputs the language display name. The
language code (ISO 639 two-letter code such as `en`, `fr`, `de`) is checked against string map keys first, just
as with country.

```java
messageSupport.setLocale(Locale.forLanguageTag("es-ES"));

messageSupport
    .message("%{loc,locale:language}")
    .with("loc", Locale.UK)
    .format();
// "inglés"
```

String map keys can override the language display name for specific language codes.

```java
messageSupport.setLocale(Locale.forLanguageTag("es-ES"));

messageSupport
    .message("%{loc,locale:language,'fr':francesa}")
    .with("loc", Locale.FRANCE)
    .format();
// "francesa"
```


## Script and Variant

The `script` and `variant` options output the corresponding display name. These are less commonly used and
produce an empty string for locales that do not have a script or variant defined.

```java
messageSupport
    .message("%{loc,locale:script}")
    .with("loc", Locale.forLanguageTag("zh-Hant"))
    .format();
// "Traditional Chinese" (or localized equivalent)
```


## Null Handling

A `null` parameter value produces an empty string by default. You can provide a `null` map key to produce
specific text.

```java
messageSupport
    .message("%{loc,null:'no locale'}")
    .with("loc", null)
    .format();
// "no locale"
```
