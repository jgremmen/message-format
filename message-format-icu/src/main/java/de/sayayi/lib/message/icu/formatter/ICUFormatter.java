/*
 * Copyright 2026 Jeroen Gremmen
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
package de.sayayi.lib.message.icu.formatter;

import com.ibm.icu.text.MessageFormat;
import de.sayayi.lib.message.formatter.parameter.NamedParameterFormatter;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.text.FieldPosition;
import java.util.Set;

import static de.sayayi.lib.message.part.MapKey.EMPTY_TYPE;
import static de.sayayi.lib.message.part.TextPartFactory.emptyText;
import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;


/**
 * @author Jeroen Gremmen
 * @since 0.24.0
 */
public final class ICUFormatter implements NamedParameterFormatter
{
  @Override
  @Contract(pure = true)
  public @NotNull String getName() {
    return "icu";
  }


  @Override
  public @NotNull Text format(@NotNull ParameterFormatterContext context, Object value)
  {
    final var icuPattern = context.getConfigValueString("icu");

    if (icuPattern.isPresent())
    {
      try {
        final var icuFormat = new MessageFormat(icuPattern.get());

        icuFormat.setLocale(context.getLocale());

        final var text = noSpaceText(icuFormat
            .format(context.asParameterMap(), new StringBuffer(), new FieldPosition(0))
            .toString());

        // handle empty and !empty for result
        return context
            .getMapMessage(text.getText(), EMPTY_TYPE)
            .map(context::format)
            .orElse(text);
      } catch(Exception ignored) {
        // ignore exception and return empty text
      }
    }

    return emptyText();
  }


  @Override
  public @Unmodifiable @NotNull Set<String> getParameterConfigNames() {
    return Set.of("icu");
  }


  @Override
  public boolean autoApplyOnNamedConfigParameter() {
    return true;
  }
}
