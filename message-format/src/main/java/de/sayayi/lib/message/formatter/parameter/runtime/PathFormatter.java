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
 * This formatter uses the {@code path} configuration key to select which aspect of the path to format:
 * <ul>
 *   <li>{@code path} (default) &ndash; the path as-is</li>
 *   <li>{@code name} &ndash; the file name component</li>
 *   <li>{@code parent} &ndash; the parent path</li>
 *   <li>{@code root} &ndash; the root component</li>
 *   <li>{@code absolute-path} &ndash; the absolute path</li>
 *   <li>{@code real-path} &ndash; the real (resolved) path, falling back to the absolute path if resolution fails</li>
 *   <li>{@code normalize} or {@code normalized-path} &ndash; the normalized path</li>
 *   <li>
 *     {@code ext} or {@code extension} &ndash; the file extension; string map keys can be used to override the
 *     output for specific extensions
 *   </li>
 *   <li>{@code mimetype} &ndash; the MIME type of the file (regular files only)</li>
 * </ul>
 * <p>
 * If the configuration key is absent, the path is used as-is. If the value does not match a known option, formatting
 * is delegated to the next available formatter.
 * <p>
 * As a {@link SizeQueryable} formatter, it reports the file size in bytes for regular files.
 *
 * @author Jeroen Gremmen
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


  @Contract(pure = true)
  private @NotNull Text formatPathRealPath(@NotNull Path path)
  {
    try {
      return toText(path.toRealPath());
    } catch(IOException ignore) {
    }

    return formatPathAbsolutePath(path);
  }


  @Contract(pure = true)
  private @NotNull Text formatPathAbsolutePath(@NotNull Path path) {
    return toText(path.toAbsolutePath());
  }


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


  @Contract(pure = true)
  private static Path toPath(@NotNull Object fileOrPath) {
    return fileOrPath instanceof File ? ((File)fileOrPath).toPath() : (Path)fileOrPath;
  }


  @Contract(pure = true)
  private static @NotNull Text toText(Path path) {
    return path == null ? emptyText() : noSpaceText(path.toString());
  }
}
