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
package de.sayayi.lib.message.formatter.runtime;

import de.sayayi.lib.message.formatter.AbstractMultiSelectFormatter;
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.formatter.ParameterFormatter.SizeQueryable;
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
import static java.util.OptionalLong.empty;


/**
 * @author Jeroen Gremmen
 */
public final class PathFormatter extends AbstractMultiSelectFormatter<Object> implements SizeQueryable
{
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
  }


  private @NotNull Text formatPathRealPath(@NotNull Path path)
  {
    try {
      return toText(path.toRealPath());
    } catch(IOException ignore) {
    }

    return formatPathAbsolutePath(path);
  }


  private @NotNull Text formatPathAbsolutePath(@NotNull Path path) {
    return toText(path.toAbsolutePath());
  }


  private @NotNull Text formatPathExtension(@NotNull FormatterContext context, @NotNull Path path)
  {
    path = path.getFileName();
    if (path == null)
      return nullText();

    var name = path.toString();
    var dotIndex = name.lastIndexOf('.');
    if (dotIndex == -1)
      return emptyText();

    var extension = name.substring(dotIndex + 1);

    return formatUsingMappedString(context, extension, true).orElseGet(() -> noSpaceText(extension));
  }


  @Override
  public @NotNull OptionalLong size(@NotNull FormatterContext context, @NotNull Object fileOrPath)
  {
    var path = toPath(fileOrPath);

    if (isRegularFile(path))
    {
      try {
        return OptionalLong.of(Files.size(path));
      } catch(IOException ignored) {
      }
    }

    return empty();
  }


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
