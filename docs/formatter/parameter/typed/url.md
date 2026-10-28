# URL

This formatter is included in the `DefaultFormatterService`.

The `URLFormatter` is a type-based formatter registered for `java.net.URL`. It is automatically selected whenever a
parameter value is a `URL`. By default, it outputs the full external form of the URL.

The `url` configuration key controls which component of the URL to display:

| Value       | Output                                                                   |
|-------------|--------------------------------------------------------------------------|
| `external`  | Full external form of the URL (default when no config is set)            |
| `authority` | Authority component including user info (e.g. `user@host:8080`)          |
| `file`      | File component (path + query)                                            |
| `host`      | Host name                                                                |
| `path`      | Path component                                                           |
| `port`      | Port number (falls back to the protocol's default port if not specified) |
| `protocol`  | Protocol (e.g. `https`, `ftp`)                                           |
| `query`     | Query string (after `?`)                                                 |
| `user-info` | User info component (before `@`)                                         |
| `ref`       | Reference/fragment component (after `#`)                                 |


## Full URL

When the `url` configuration key is absent or set to `external`, the formatter outputs the complete URL in its external
form.

```java
messageSupport
    .message("%{u}")
    .with("u", URI
        .create("https://example.com:8443/api/v1?q=test#section")
        .toURL())
    .format();
// "https://example.com:8443/api/v1?q=test#section"
```


## Individual Components

Each component can be extracted using the `url` configuration key.

```java
URL url = URI
    .create("https://user@example.com:8443/api/v1?q=test#section")
    .toURL();

messageSupport
    .message("%{u,url:protocol}")
    .with("u", url)
    .format();
// "https"

messageSupport
    .message("%{u,url:host}")
    .with("u", url)
    .format();
// "example.com"

messageSupport
    .message("%{u,url:port}")
    .with("u", url)
    .format();
// "8443"

messageSupport
    .message("%{u,url:path}")
    .with("u", url)
    .format();
// "/api/v1"

messageSupport
    .message("%{u,url:query}")
    .with("u", url)
    .format();
// "q=test"

messageSupport
    .message("%{u,url:ref}")
    .with("u", url)
    .format();
// "section"

messageSupport
    .message("%{u,url:file}")
    .with("u", url)
    .format();
// "/api/v1?q=test"

messageSupport
    .message("%{u,url:authority}")
    .with("u", url)
    .format();
// "user@example.com:8443"

messageSupport
    .message("%{u,url:user-info}")
    .with("u", url)
    .format();
// "user"
```


## Port Default Fallback

Unlike the [URI formatter](uri.md), the URL formatter falls back to the protocol's default port when no explicit port 
is specified. For example, an `https` URL without a port returns `443`.

```java
messageSupport
    .message("%{u,url:port}")
    .with("u", URI.create("https://example.com/").toURL())
    .format();
// "443"

messageSupport
    .message("%{u,url:port}")
    .with("u", URI.create("http://example.com/").toURL())
    .format();
// "80"
```


## Map Key Overrides

The `protocol` component supports string map keys to override the output for specific protocols.

```java
messageSupport
    .message("%{u,url:protocol,'https':'secure','http':'insecure'}")
    .with("u", URI.create("https://example.com").toURL())
    .format();
// "secure"
```

The `port` component supports number map keys to override the output for specific port numbers.

```java
messageSupport
    .message("%{u,url:port,443:'standard HTTPS',80:'standard HTTP'}")
    .with("u", URI.create("https://example.com/").toURL())
    .format();
// "standard HTTPS"
```


## Null Handling

A `null` parameter value produces an empty string by default. A `null` map key can be provided to produce
specific text.

```java
messageSupport
    .message("%{u,null:'no URL'}")
    .with("u", null)
    .format();
// "no URL"
```
