# Byte Array

This formatter is included in the `DefaultFormatterService`.

The `ByteArrayFormatter` handles `byte[]` values and provides specialized encoding and decoding options that go
beyond what the general array formatter offers. It is registered with a higher priority than the general
`ArrayFormatter` for `byte[]`, so it is selected first whenever a byte array is used as a parameter value.

The formatter's behavior is controlled by the `bytes` configuration key. When this key is present, the byte
array is converted to a string using the specified encoding. When the key is absent, formatting is delegated to
the next available formatter in the chain (typically the general `ArrayFormatter`, which renders each byte as a
comma-separated number).


## The `bytes` Configuration Key

The `bytes` configuration key determines how the byte array is converted to a string. The following values are
recognized:

### `base64`

Encodes the byte array as a standard Base64 string without line breaks.

```java
messageSupport
    .message("%{data,bytes:base64}")
    .with("data", "Hello".getBytes())
    .format();
// "SGVsbG8="
```

### `base64-lf`

Encodes the byte array as a MIME Base64 string with line breaks inserted every 76 characters.

```java
messageSupport
    .message("%{data,bytes:'base64-lf'}")
    .with("data", "This appears to be a very long text with a single linefeed!".getBytes())
    .format();
// "VGhpcyBhcHBlYXJzIHRvIGJlIGEgdmVyeSBsb25nIHRleHQgd2l0aCBhIHNpbmdsZSBsaW5lZmVl\nZCE="
```

### Charset Name

Any valid Java charset name (such as `UTF-8`, `iso-8859-1`, `US-ASCII`) decodes the byte array into a string
using that charset.

```java
messageSupport
    .message("%{text,bytes:'UTF-8'}")
    .with("text", "Größe".getBytes(StandardCharsets.UTF_8))
    .format();
// "Größe"
```

```java
messageSupport
    .message("%{text,bytes:'iso-8859-1'}")
    .with("text", "Größe".getBytes(StandardCharsets.ISO_8859_1))
    .format();
// "Größe"
```

### Empty or Unsupported Charset

When the `bytes` value is an empty string or a charset name that is not supported by the platform, the byte
array is decoded using the platform's default charset.

```java
messageSupport
    .message("%{text,bytes:''}")
    .with("text", "hello".getBytes())
    .format();
// "hello"
```


## Delegation

When the `bytes` configuration key is absent, the formatter delegates to the next formatter in the chain. This
is typically the general `ArrayFormatter`, which renders each byte as a comma-separated decimal number.

```java
messageSupport
    .message("%{data}")
    .with("data", new byte[] { 71, 114, -61, -74 })
    .format();
// "71, 114, -61, -74"
```

This means you can still use all list configuration keys (`list-sep`, `list-max-size`, etc.) when no `bytes`
key is specified.


## Empty Array

When the byte array is empty (length zero) and the `bytes` configuration key is present, the formatter produces
an empty string.

```java
messageSupport
    .message("%{data,bytes:base64}")
    .with("data", new byte[0])
    .format();
// ""
```


## Map Keys

The `empty` key matches when the byte array has zero length. Its negated form `!empty` matches when the array
contains at least one byte.

```java
messageSupport
    .message("%{data,bytes:base64,empty:'no data'}")
    .with("data", new byte[0])
    .format();
// "no data"
```

The `null` key matches when the parameter value is `null`.

```java
messageSupport
    .message("%{data,null:'missing'}")
    .with("data", (byte[]) null)
    .format();
// "missing"
```


## Size Queries

The `ByteArrayFormatter` reports the length of the byte array as its size. This can be used with the `size`
named formatter.

```java
messageSupport
    .message("%{data,format:size}")
    .with("data", new byte[] { 'a', 'b' })
    .format();
// "2"
```
