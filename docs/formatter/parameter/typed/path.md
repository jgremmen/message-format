# Path

This formatter is included in the `DefaultFormatterService`.

The `PathFormatter` handles `java.nio.file.Path` and `java.io.File` values. It is automatically selected whenever
a parameter value is a `Path` or `File` instance. The formatter uses the `path` configuration key to select which
aspect of the path to render.


## The `path` Configuration Key

The `path` configuration key determines what part or representation of the path is output. If the key is absent,
the path is rendered as-is (equivalent to `path:path`). If the value does not match any known option, formatting
is delegated to the next available formatter.

### `path` (default)

Outputs the path string as-is, exactly as `Path.toString()` returns it.

```java
messageSupport
    .message("%{file}")
    .with("file", Path.of("/home/user/docs/report.pdf"))
    .format();
// "/home/user/docs/report.pdf"
```

### `name`

Outputs only the file name component (the last element of the path).

```java
messageSupport
    .message("%{file,path:name}")
    .with("file", Path.of("/home/user/docs/report.pdf"))
    .format();
// "report.pdf"
```

### `parent`

Outputs the parent path (everything except the last element). Produces an empty string if the path has no parent.

```java
messageSupport
    .message("%{file,path:parent}")
    .with("file", Path.of("/home/user/docs/report.pdf"))
    .format();
// "/home/user/docs"
```

### `root`

Outputs the root component of the path. Produces an empty string if the path is relative.

```java
messageSupport
    .message("%{file,path:root}")
    .with("file", Path.of("/home/user/docs/report.pdf"))
    .format();
// "/"
```

### `absolute-path`

Outputs the absolute path, resolving it against the current working directory if necessary.

```java
messageSupport
    .message("%{file,path:'absolute-path'}")
    .with("file", Path.of("docs/report.pdf"))
    .format();
// "/home/user/project/docs/report.pdf" (depending on working directory)
```

### `real-path`

Outputs the real (resolved) path with symbolic links and relative segments resolved. If the path cannot be
resolved (e.g. the file does not exist), it falls back to the absolute path.

```java
messageSupport
    .message("%{file,path:'real-path'}")
    .with("file", Path.of("/tmp/link-to-report.pdf"))
    .format();
// "/home/user/docs/report.pdf" (if the symlink resolves there)
```

### `normalize` / `normalized-path`

Outputs the normalized path with redundant elements (like `.` and `..`) removed.

```java
messageSupport
    .message("%{file,path:normalize}")
    .with("file", Path.of("/home/user/../user/docs/./report.pdf"))
    .format();
// "/home/user/docs/report.pdf"
```

### `ext` / `extension`

Outputs the file extension (the part after the last dot in the file name). If the file has no extension, the
output is an empty string. If the path has no file name component, the output is empty.

String map keys can be used to map specific extensions to custom text.

```java
messageSupport
    .message("%{file,path:ext}")
    .with("file", Path.of("report.pdf"))
    .format();
// "pdf"
```

```java
messageSupport
    .message("%{file,path:extension,'pdf':'PDF document','txt':'text file'}")
    .with("file", Path.of("report.pdf"))
    .format();
// "PDF document"
```

```java
messageSupport
    .message("%{file,path:ext}")
    .with("file", Path.of("Makefile"))
    .format();
// ""
```

### `mimetype`

Outputs the MIME type of the file as detected by the operating system. This only works for regular files that
exist on disk. If the file does not exist or is not a regular file, the output is empty.

```java
messageSupport
    .message("%{file,path:mimetype}")
    .with("file", Path.of("/home/user/photo.jpg"))
    .format();
// "image/jpeg" (if the file exists)
```


## File Support

The formatter also handles `java.io.File` values. A `File` is internally converted to a `Path` before
formatting, so all configuration options work identically.

```java
messageSupport
    .message("%{file,path:name}")
    .with("file", new File("/home/user/docs/report.pdf"))
    .format();
// "report.pdf"
```


## Size Queries

The `PathFormatter` reports the file size in bytes for regular files that exist on disk. If the path does not
refer to a regular file or the file cannot be accessed, no size is available.

```java
messageSupport
    .message("%{file,format:size}")
    .with("file", Path.of("/home/user/docs/report.pdf"))
    .format();
// "1048576" (if the file is 1 MB)
```


## Null Handling

A `null` parameter value produces an empty string by default. You can provide a `null` map key to handle this
case explicitly.

```java
messageSupport
    .message("%{file,null:'no file specified'}")
    .with("file", (Path) null)
    .format();
// "no file specified"
```
