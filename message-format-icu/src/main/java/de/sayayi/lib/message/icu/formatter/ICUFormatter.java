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
import static de.sayayi.lib.message.util.MessageUtil.trimAndNormalizeSpaces;


/**
 * Named parameter formatter that formats values using ICU {@link MessageFormat} patterns.
 * <p>
 * This formatter is selected by using the name {@code icu} in a message parameter, or automatically when the parameter
 * configuration contains the {@code icu} config key:
 * <pre>{@code %{p,icu:''{0,number,currency}''}}</pre>
 * <p>
 * The ICU pattern is read from the {@code icu} configuration value. All parameters available in the formatting context
 * are passed to the ICU message format as a named argument map. The formatter uses the context locale for
 * locale-sensitive formatting.
 * <p>
 * If the ICU pattern is missing or formatting fails, empty text is returned.
 *
 * @author Jeroen Gremmen
 * @since 0.24.0
 *
 * @see MessageFormat
 */
public final class ICUFormatter implements NamedParameterFormatter
{
  /**
   * {@inheritDoc}
   *
   * @return  {@code "icu"}, never {@code null}
   */
  @Override
  @Contract(pure = true)
  public @NotNull String getName() {
    return "icu";
  }


  /**
   * {@inheritDoc}
   * <p>
   * Formats the parameter value using the ICU {@link MessageFormat} pattern specified in the {@code icu} configuration
   * key. The pattern is trimmed and normalized before being passed to ICU. All parameters available in the formatting
   * context are passed to the ICU message format as a named argument map, and the context locale is applied for
   * locale-sensitive formatting.
   * <p>
   * If the formatted result is empty, mapped messages for {@code empty} key types are consulted. If no ICU pattern is
   * configured or an error occurs during formatting, empty text is returned.
   *
   * @param context  message context providing formatting information, not {@code null}
   * @param value    parameter value (can be {@code null})
   *
   * @return  formatted text, never {@code null}
   */
  @Override
  public @NotNull Text format(@NotNull ParameterFormatterContext context, Object value)
  {
    final var icuPattern = context.getConfigValueString("icu");

    if (icuPattern.isPresent())
    {
      try {
        final var icuFormat = new MessageFormat(trimAndNormalizeSpaces(icuPattern.get()));

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


  /**
   * {@inheritDoc}
   *
   * @return  unmodifiable set containing {@code "icu"}, never {@code null}
   */
  @Override
  public @Unmodifiable @NotNull Set<String> getParameterConfigNames() {
    return Set.of("icu");
  }


  /**
   * {@inheritDoc}
   *
   * @return  always {@code true}
   */
  @Override
  public boolean autoApplyOnNamedConfigParameter() {
    return true;
  }
}
