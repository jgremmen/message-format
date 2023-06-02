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

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.formatter.AbstractParameterFormatter;
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Set;

import static de.sayayi.lib.message.part.TextPartFactory.*;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.STRING_EMPTY_TYPE;
import static java.util.Collections.singleton;


/**
 * @author Jeroen Gremmen
 */
public final class PathFormatter extends AbstractParameterFormatter
{
  @Override
  @SuppressWarnings("DuplicatedCode")
  public @NotNull Text formatValue(@NotNull FormatterContext context, @NotNull Object value)
  {
    Path path = (Path)value;

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
      case "extension": {
        path = path.getFileName();
        if (path == null)
          return nullText();

        final String name = path.toString();
        final int dotidx = name.lastIndexOf('.');
        if (dotidx == -1)
          return emptyText();

        final String extension = name.substring(dotidx + 1);
        final Message.WithSpaces msg = context
            .getConfigMapMessage(extension, STRING_EMPTY_TYPE)
            .orElse(null);

        return msg != null ? context.format(msg) : noSpaceText(extension);
      }

      default:
        return context.delegateToNextFormatter();
    }

    return path == null ? emptyText() : noSpaceText(path.toString());
  }


  @Override
  @Contract(value = "-> new", pure = true)
  public @NotNull Set<FormattableType> getFormattableTypes() {
    return singleton(new FormattableType(Path.class));
  }
}
