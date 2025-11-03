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
import static java.lang.Math.max;


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
  @SuppressWarnings({"OptionalGetWithoutIsPresent", "UnnecessaryUnicodeEscape"})
  public @NotNull Text postFormat(@NotNull FormatterContext context, @NotNull Text text)
  {
    var maxSize = (int)context.getConfigValueNumber("clip").getAsLong();
    if (maxSize > 0)
    {
      final var string = text.getText();

      if (context.getConfigValueBool("clip-suffix").orElse(true))
      {
        final var suffixText = context.getConfigValueString("clip-suffix-text").orElse("\u2026");
        final var suffixTextLength = suffixText.length();

        maxSize = max(maxSize, max(4, suffixTextLength + 1) + suffixTextLength);

        if (string.length() > maxSize)
          return noSpaceText(string.substring(0, maxSize - suffixTextLength).trim() + suffixText);
      }
      else if (string.length() > maxSize)
        return noSpaceText(string.substring(0, maxSize));
    }

    return text;
  }
}
