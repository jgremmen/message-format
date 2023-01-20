/*
 * Copyright 2022 Jeroen Gremmen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.sayayi.lib.message.formatter.named;

import de.sayayi.lib.message.formatter.AbstractParameterFormatter;
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.formatter.NamedParameterFormatter;
import de.sayayi.lib.message.internal.part.MessagePart.Text;
import de.sayayi.lib.message.internal.part.TextPart;
import lombok.val;
import lombok.var;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

import static de.sayayi.lib.message.internal.part.MessagePartFactory.nullText;
import static java.util.Collections.emptySet;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class CutOffFormatter extends AbstractParameterFormatter implements NamedParameterFormatter
{
  @Override
  public @NotNull String getName() {
    return "cutoff";
  }


  @Override
  protected @NotNull Text formatValue(@NotNull FormatterContext formatterContext, Object value)
  {
    if (value == null)
      return nullText();

    val text = formatterContext.format(value, false);

    var s = text.getText();
    if (s == null)
      return nullText();
    s = s.trim();

    val maxSize = (int)Math.max(formatterContext.getConfigValueNumber("cut-size").orElse(64), 8);

    return s.length() <= maxSize
        ? text
        : new TextPart(s.substring(0, maxSize - 3).trim() + "...", text.isSpaceBefore(), text.isSpaceAfter());
  }


  @Override
  public @NotNull Set<FormattableType> getFormattableTypes() {
    return emptySet();
  }
}