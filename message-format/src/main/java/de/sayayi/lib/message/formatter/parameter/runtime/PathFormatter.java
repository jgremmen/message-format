/*
 * Copyright 2019 Jeroen Gremmen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.sayayi.lib.message.formatter.parameter.runtime;

import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.parameter.AbstractMultiSelectFormatter;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatter.SizeQueryable;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.OptionalLong;
import java.util.Set;

import static de.sayayi.lib.message.formatter.FormattableType.DEFAULT_ORDER;
import static de.sayayi.lib.message.part.TextPartFactory.*;
import static java.nio.file.Files.isRegularFile;
import static java.nio.file.Files.probeContentType;
import static java.util.OptionalLong.empty;


/**
 * Parameter formatter for {@link Path} and {@link File} values.
 * <p>
 * This formatter uses the {@code path} configuration key to select which aspect of the path to format.
 * <p>
 * If the configuration key is absent, the path is used as-is. If the value does not match a known option, formatting
 * is delegated to the next available formatter.
 * <p>
 * As a {@link SizeQueryable} formatter, it reports the file size in bytes for regular files.
 * <p>
 * <b>Configuration keys:</b>
 * <table border="1">
 *   <caption>Configuration keys for the path formatter</caption>
 *   <tr><th>Key</th><th>Values</th><th>Default</th><th>Purpose</th></tr>
 *   <tr>
 *     <td>{@code path}</td>
 *     <td>
 *       {@code path}, {@code name}, {@code parent}, {@code root}, {@code absolute-path}, {@code real-path},
 *       {@code normalize}, {@code normalized-path}, {@code ext}, {@code extension}, {@code mimetype}
 *     </td>
 *     <td>{@code path}</td>
 *     <td>
 *       Aspect of the path to format: the path as-is, the file name, the parent path, the root component, the
 *       absolute path, the real path (or the absolute path if it cannot be resolved), the normalized path, the
 *       file extension or the MIME type of a regular file. String keys override the output for specific
 *       extensions. Any other value delegates to the next formatter.
 *     </td>
 *   </tr>
 * </table>
 * <p>
 * <b>Examples:</b>
 * <p>
 * Format individual components of a path:
 * <pre>{@code
 * var path = Path.of("/home/user/docs/report.pdf");
 *
 * messageSupport
 *     .message("File %{p,path:name} in %{p,path:parent}")
 *     .with("p", path)
 *     .format();
 * // "File report.pdf in /home/user/docs"
 * }</pre>
 * <p>
 * Map a file extension to a description, using the default entry for all other extensions:
 * <pre>{@code
 * var message = "%{p,path:ext,'pdf':'PDF document','txt':'text file',:'unknown file type'}";
 *
 * messageSupport.message(message).with("p", Path.of("report.pdf")).format();
 * // "PDF document"
 *
 * messageSupport.message(message).with("p", Path.of("archive.zip")).format();
 * // "unknown file type"
 * }</pre>
 * <p>
 * Remove redundant elements from a path. {@link java.io.File File} values are supported as well:
 * <pre>{@code
 * messageSupport
 *     .message("%{f,path:normalize}")
 *     .with("f", new File("/home/user/../shared/./data.csv"))
 *     .format();
 * // "/home/shared/data.csv"
 * }</pre>
 * <p>
 * The size of a path to a regular file is the size of that file in bytes:
 * <pre>{@code
 * var file = Files.writeString(Files.createTempFile("report", ".txt"), "Hello");
 *
 * messageSupport.message("%{p,format:size} bytes").with("p", file).format();
 * // "5 bytes"
 * }</pre>
 *
 * @author Jeroen Gremmen
 * @since 0.3.1
 */
public final class PathFormatter extends AbstractMultiSelectFormatter<Object> implements SizeQueryable
{
  /**
   * Creates a new path formatter with the configuration key {@code path} and selection options for the various
   * path aspects.
   */
  public PathFormatter()
  {
    super("path", "path", true);

    register("real-path", (context,value) -> formatPathRealPath(toPath(value)));
    register("absolute-path", (context,value) -> formatPathAbsolutePath(toPath(value)));
    register("name", (context,value) -> toText(toPath(value).getFileName()));
    register(new String[] { "normalize", "normalized-path" }, (context,value) -> toText(toPath(value).normalize()));
    register("path", (context,value) -> toText(toPath(value)));
    register("parent", (context,value) -> toText(toPath(value).getParent()));
    register("root", (context,value) -> toText(toPath(value).getRoot()));
    register(new String[] { "ext", "extension" }, (context,value) -> formatPathExtension(context, toPath(value)));
    register("mimetype", ((context, value) -> formatPathMimeType(toPath(value))));
  }


  /**
   * {@inheritDoc}
   * <p>
   * Adds the {@code path} classifier.
   */
  @Override
  public boolean updateClassifiers(@NotNull ClassifierContext context, @NotNull Object value)
  {
    context.addClassifier("path");

    return true;
  }


  /**
   * Formats the real path of {@code path}, falling back to the absolute path if the real path cannot be determined.
   *
   * @param path  path to format, not {@code null}
   *
   * @return  formatted real path, never {@code null}
   */
  @Contract(pure = true)
  private @NotNull Text formatPathRealPath(@NotNull Path path)
  {
    try {
      return toText(path.toRealPath());
    } catch(IOException ignore) {
    }

    return formatPathAbsolutePath(path);
  }


  /**
   * Formats the absolute path of {@code path}.
   *
   * @param path  path to format, not {@code null}
   *
   * @return  formatted absolute path, never {@code null}
   */
  @Contract(pure = true)
  private @NotNull Text formatPathAbsolutePath(@NotNull Path path) {
    return toText(path.toAbsolutePath());
  }


  /**
   * Formats the extension of the file name of {@code path}, using a message mapped to the extension if available.
   *
   * @param context  formatter context, not {@code null}
   * @param path     path to format, not {@code null}
   *
   * @return  formatted extension, empty text if the file name has no extension or {@code null} text if the path has no
   *          file name, never {@code null}
   */
  @Contract(pure = true)
  private @NotNull Text formatPathExtension(@NotNull ParameterFormatterContext context, @NotNull Path path)
  {
    path = path.getFileName();
    if (path == null)
      return nullText();

    final var name = path.toString();
    final var dotIndex = name.lastIndexOf('.');

    if (dotIndex == -1)
      return emptyText();

    final var extension = name.substring(dotIndex + 1);

    return formatUsingMappedString(context, extension, true).orElseGet(() -> noSpaceText(extension));
  }


  /**
   * Formats the MIME type of the regular file denoted by {@code path}.
   *
   * @param path  path to format, not {@code null}
   *
   * @return  formatted MIME type, or {@code null} text if the path is not a regular file or its MIME type cannot be
   *          determined, never {@code null}
   */
  @Contract(pure = true)
  private @NotNull Text formatPathMimeType(@NotNull Path path)
  {
    if (isRegularFile(path))
    {
      try {
        return noSpaceText(probeContentType(path));
      } catch(IOException ignored) {
      }
    }

    return nullText();
  }


  /**
   * {@inheritDoc}
   * <p>
   * Returns the file size in bytes for regular files, or empty if the path does not refer to a regular file.
   */
  @Override
  public @NotNull OptionalLong size(@NotNull ParameterFormatterContext context, @NotNull Object fileOrPath)
  {
    final var path = toPath(fileOrPath);

    if (isRegularFile(path))
    {
      try {
        return OptionalLong.of(Files.size(path));
      } catch(IOException ignored) {
      }
    }

    return empty();
  }


  /**
   * {@inheritDoc}
   *
   * @return  a set containing {@link File} and {@link Path} formattable types, never {@code null}
   */
  @Override
  public @NotNull Set<FormattableType> getFormattableTypes()
  {
    return Set.of(
        new FormattableType(File.class),

        // path implements iterable, so make sure it has a higher precedence than IterableFormatter
        new FormattableType(Path.class, DEFAULT_ORDER - 5)
    );
  }


  /**
   * Converts {@code fileOrPath} into a path.
   *
   * @param fileOrPath  {@link File} or {@link Path} instance, not {@code null}
   *
   * @return  path, never {@code null}
   */
  @Contract(pure = true)
  private static Path toPath(@NotNull Object fileOrPath) {
    return fileOrPath instanceof File ? ((File)fileOrPath).toPath() : (Path)fileOrPath;
  }


  /**
   * Converts {@code path} into text.
   *
   * @param path  path to convert, or {@code null}
   *
   * @return  text containing the path, or empty text if {@code path} is {@code null}, never {@code null}
   */
  @Contract(pure = true)
  private static @NotNull Text toText(Path path) {
    return path == null ? emptyText() : noSpaceText(path.toString());
  }
}
