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

import de.sayayi.lib.message.formatter.AbstractSingleTypeParameterFormatter;
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.formatter.ParameterFormatter.SizeQueryable;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.OptionalLong;

import static de.sayayi.lib.message.part.TextPartFactory.*;


/**
 * @author Jeroen Gremmen
 */
public final class PathFormatter extends AbstractSingleTypeParameterFormatter<Path>
    implements SizeQueryable
{
  @Override
  @SuppressWarnings("DuplicatedCode")
  public @NotNull Text formatValue(@NotNull FormatterContext context, @NotNull Path path)
  {
    switch(context.getConfigValueString("path").orElse("path"))
    {
      case "name":
        path = path.getFileName();
        break;

      case "path":
        break;

      case "parent":
        path = path.getParent();
        break;

      case "root":
        path = path.getRoot();
        break;

      case "ext":
      case "extension":
        return formatValue_extension(context, path);

      default:
        return context.delegateToNextFormatter();
    }

    return path == null ? emptyText() : noSpaceText(path.toString());
  }


  @SuppressWarnings("DuplicatedCode")
  private @NotNull Text formatValue_extension(@NotNull FormatterContext context,
                                              @NotNull Path path)
  {
    path = path.getFileName();
    if (path == null)
      return nullText();

    final String name = path.toString();
    final int dotidx = name.lastIndexOf('.');
    if (dotidx == -1)
      return emptyText();

    final String extension = name.substring(dotidx + 1);

    return formatUsingMappedString(context, extension, true)
        .orElseGet(() -> noSpaceText(extension));
  }


  @Override
  public @NotNull OptionalLong size(@NotNull FormatterContext context, @NotNull Object value) {
    return context.size(((Path)value).toFile());
  }


  @Override
  public @NotNull FormattableType getFormattableType() {
    return new FormattableType(Path.class);
  }
}
