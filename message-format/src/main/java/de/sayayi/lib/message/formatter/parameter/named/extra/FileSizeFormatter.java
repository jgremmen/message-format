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
package de.sayayi.lib.message.formatter.parameter.named.extra;

import de.sayayi.lib.message.formatter.parameter.AbstractParameterFormatter;
import de.sayayi.lib.message.formatter.parameter.NamedParameterFormatter;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Set;

import static de.sayayi.lib.message.part.MapKey.STRING_TYPE;
import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static java.lang.Boolean.FALSE;


/**
 * Named parameter formatter that formats numeric values as human-readable file sizes.
 * <p>
 * This formatter is selected by using the name {@code filesize} in a message parameter, e.g.
 * {@code %{myParam,format:filesize}}.
 * <p>
 * It accepts numeric values representing a file size in bytes and automatically selects the appropriate unit
 * (B, KB, MB, GB, TB, PB). The number of decimal places is controlled by the {@code scale} configuration key
 * (0&ndash;3, default: 1).
 * <p>
 * String map keys in the parameter configuration can be used to override the unit labels (e.g. to provide localized
 * unit names). A {@code space} boolean configuration key controls whether a space is inserted between the number and
 * the unit.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
@Deprecated(forRemoval = true)
public final class FileSizeFormatter extends AbstractParameterFormatter<Number>
    implements NamedParameterFormatter
{
  private static final String[] UNITS = new String[] { "B", "KB", "MB", "GB", "TB", "PB" };

  private static final long[] POW10 = new long[] {
      1L, 10L, 100L, 1000L, 10000L, 100000L, 1000000L, 10000000L, 100000000L, 1000000000L,
      10000000000L, 100000000000L, 1000000000000L, 10000000000000L, 100000000000000L,
      1000000000000000L
  };

  private static final String[] FORMAT = new String[] {
      "#,##0", "#,##0.0", "#,##0.00", "#,##0.000"
  };


  /**
   * {@inheritDoc}
   *
   * @return  {@code "filesize"}, never {@code null}
   */
  @Override
  public @NotNull String getName() {
    return "filesize";
  }


  /**
   * {@inheritDoc}
   * <p>
   * This formatter can handle {@link Number} types as well as primitive {@code long},
   * {@code int} and {@code short} values and {@code null}.
   */
  @Override
  public boolean canFormat(@NotNull Class<?> type)
  {
    return
        Number.class.isAssignableFrom(type) ||
        type == long.class ||
        type == int.class ||
        type == short.class ||
        type == NULL_TYPE;
  }


  /**
   * {@inheritDoc}
   * <p>
   * Formats the numeric value as a human-readable file size with the appropriate unit.
   */
  @Override
  protected @NotNull Text formatValue(@NotNull ParameterFormatterContext context, @NotNull Number value)
  {
    final var size = value.longValue();
    final var s = new StringBuilder();
    final int unitIndex;
    var scale = normalizeScale(context.getConfigValueNumber("scale").orElse(1));

    if (size <= 0)
    {
      unitIndex = 0;
      s.append('0');
    }
    else
    {
      if ((unitIndex = calculateUnitIndex(size, scale)) == 0)
        scale = 0;

      s.append(new DecimalFormat(FORMAT[scale],
          DecimalFormatSymbols.getInstance(context.getLocale()))
          .format((double)size / POW10[unitIndex * 3]));
    }

    final var unit = UNITS[unitIndex];
    final var unitMessage = context.getMapMessage(unit, STRING_TYPE).orElse(null);

    if ((unitMessage != null && unitMessage.isSpaceBefore()) ||
        context.getConfigValueBool("space").orElse(FALSE))
      s.append(' ');

    return noSpaceText(s
        .append(unitMessage == null ? unit : context.format(unitMessage).getText())
        .toString());
  }


  private static int normalizeScale(long scale) {
    return scale < 0 ? 0 : (int)Math.min(scale, 3);
  }


  private static int calculateUnitIndex(long n, int scale) {
    return n < 1000 ? 0 : log1000(n + POW10[log1000(n) * 3 - scale] / 2);
  }


  private static int log1000(long n)
  {
    for(var i = (POW10.length - 1) / 3; i >= 0; i--)
      if (n >= POW10[i * 3])
        return i;

    return 0;
  }

  
  /**
   * {@inheritDoc}
   *
   * @return  a set containing {@code "scale"}, never {@code null}
   */
  @Override
  public @Unmodifiable @NotNull Set<String> getParameterConfigNames() {
    return Set.of("scale");
  }
}
