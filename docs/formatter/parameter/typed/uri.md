# URI

This formatter is included in the `DefaultFormatterService`.

The `URIFormatter` is a type-based formatter registered for `java.net.URI`. It is automatically selected whenever
a parameter value is a `URI`. By default, it outputs the full URI string representation.

The `uri` configuration key controls which component of the URI to display:

| Value         | Output                                             |
|---------------|----------------------------------------------------|
| `default`     | Full URI string (default when no config is set)    |
| `authority`   | Authority component (e.g. `user@host:8080`)        |
| `fragment`    | Fragment component (after `#`)                     |
| `host`        | Host name                                          |
| `path`        | Path component                                     |
| `port`        | Port number (empty when not specified)             |
| `query`       | Query string (after `?`)                           |
| `scheme`      | Scheme (e.g. `https`, `ftp`)                       |
| `user-info`   | User info component (before `@`)                   |


## Full URI

When the `uri` configuration key is absent or set to `default`, the formatter outputs the complete URI.

```java
messageSupport
    .message("%{u}")
    .with("u", URI.create("https://user@example.com:8443/api/v1?q=test#section"))
    .format();
// "https://user@example.com:8443/api/v1?q=test#section"
```


## Individual Components

Each component can be extracted using the `uri` configuration key.

```java
URI uri = URI.create("https://user@example.com:8443/api/v1?q=test#section");

messageSupport
    .message("%{u,uri:scheme}")
    .with("u", uri)
    .format();
// "https"

messageSupport
    .message("%{u,uri:host}")
    .with("u", uri)
    .format();
// "example.com"

messageSupport
    .message("%{u,uri:port}")
    .with("u", uri)
    .format();
// "8443"

messageSupport
    .message("%{u,uri:path}")
    .with("u", uri)
    .format();
// "/api/v1"

messageSupport
    .message("%{u,uri:query}")
    .with("u", uri)
    .format();
// "q=test"

messageSupport
    .message("%{u,uri:fragment}")
    .with("u", uri)
    .format();
// "section"

messageSupport
    .message("%{u,uri:authority}")
    .with("u", uri)
    .format();
// "user@example.com:8443"

messageSupport
    .message("%{u,uri:user-info}")
    .with("u", uri)
    .format();
// "user"
```


## Map Key Overrides

The `scheme` component supports string map keys to override the output for specific schemes.

```java
messageSupport
    .message("%{u,uri:scheme,'https':'secure','http':'insecure'}")
    .with("u", URI.create("https://example.com"))
    .format();
// "secure"
```

The `port` component supports number map keys to override the output for specific port numbers. When no port
is specified in the URI, the port value is `-1` and the output is an empty string.

```java
messageSupport
    .message("%{u,uri:port,8080:'development',443:'production'}")
    .with("u", URI.create("https://example.com:8080/"))
    .format();
// "development"
```


## Null Handling

A `null` parameter value produces an empty string by default. You can provide a `null` map key to produce
specific text.

```java
messageSupport
    .message("%{u,null:'no URI'}")
    .with("u", null)
    .format();
// "no URI"
```
