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
package de.sayayi.lib.message.formatter.named;

import de.sayayi.lib.message.formatter.AbstractParameterFormatter;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.formatter.NamedParameterFormatter;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.NotNull;

import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static de.sayayi.lib.message.part.TextPartFactory.nullText;
import static java.lang.Math.max;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class ClipFormatter extends AbstractParameterFormatter<Object> implements NamedParameterFormatter
{
  @Override
  public @NotNull String getName() {
    return "clip";
  }


  @Override
  protected @NotNull Text formatValue(@NotNull FormatterContext context, @NotNull Object value)
  {
    final Text text = context.format(value);

    var s = text.getText();
    if (s == null)
      return nullText();

    s = s.trim();

    final int maxSize = (int)max(context.getConfigValueNumber("clip-size").orElse(64), 8);

    return s.length() <= maxSize ? text : noSpaceText(s.substring(0, maxSize - 3).trim() + "...");
  }
}
