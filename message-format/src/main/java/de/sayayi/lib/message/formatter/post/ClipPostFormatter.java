/*
 * Copyright 2022 Jeroen Gremmen
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
package de.sayayi.lib.message.formatter.post;

import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.formatter.ParameterPostFormatter;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.NotNull;

import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;


/**
 * @author Jeroen Gremmen
 * @since 0.20.0
 */
public final class ClipPostFormatter implements ParameterPostFormatter
{
  @Override
  public @NotNull String getParameterConfigName() {
    return "clip";
  }


  @Override
  @SuppressWarnings("OptionalGetWithoutIsPresent")
  public @NotNull Text postFormat(@NotNull FormatterContext context, @NotNull Text text)
  {
    var maxSize = (int)context.getConfigValueNumber("clip").getAsLong();
    if (maxSize > 0)
    {
      var ellipsis = (boolean)context.getConfigValueBool("clip-ellipsis").orElse(true);
      if (ellipsis && maxSize < 7)
        maxSize = 7;

      var s = text.getText();
      if (s != null && s.length() > maxSize)
      {
        return noSpaceText(ellipsis
            ? s.substring(0, maxSize - 3).trim() + "..."
            : s.substring(0, maxSize));
      }
    }

    return text;
  }
}
