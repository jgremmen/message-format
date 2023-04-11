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
import de.sayayi.lib.message.internal.part.MessagePart.Text;
import de.sayayi.lib.message.internal.part.TextPart;
import org.jetbrains.annotations.NotNull;

import static de.sayayi.lib.message.internal.part.MessagePartFactory.nullText;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class ClipFormatter extends AbstractParameterFormatter
    implements NamedParameterFormatter
{
  @Override
  public @NotNull String getName() {
    return "clip";
  }


  @Override
  protected @NotNull Text formatValue(@NotNull FormatterContext formatterContext, Object value)
  {
    if (value == null)
      return nullText();

    final Text text = formatterContext.format(value, false);

    String s = text.getText();
    if (s == null)
      return nullText();
    s = s.trim();

    final int maxSize =
        (int)Math.max(formatterContext.getConfigValueNumber("clip-size").orElse(64), 8);

    return s.length() <= maxSize
        ? text
        : new TextPart(s.substring(0, maxSize - 3).trim() + "...", text.isSpaceBefore(),
            text.isSpaceAfter());
  }
}
