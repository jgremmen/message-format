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
package de.sayayi.lib.message.formatter.named.extra;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.formatter.AbstractParameterFormatter;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.formatter.NamedParameterFormatter;
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.part.parameter.key.ConfigKey;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static java.lang.Boolean.FALSE;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
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


  @Override
  public @NotNull String getName() {
    return "filesize";
  }


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


  @Override
  protected @NotNull Text formatValue(@NotNull FormatterContext context, @NotNull Number value)
  {
    final long size = value.longValue();
    int scale = normalizeScale(context.getConfigValueNumber("scale").orElse(1));
    final StringBuilder s = new StringBuilder();
    final int unitIndex;

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

    final String unit = UNITS[unitIndex];
    final Message.WithSpaces unitMessage = context
        .getConfigMapMessage(unit, ConfigKey.STRING_TYPE)
        .orElse(null);

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
    for(int i = (POW10.length - 1) / 3; i >= 0; i--)
      if (n >= POW10[i * 3])
        return i;

    return 0;
  }
}
