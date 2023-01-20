package de.sayayi.lib.message.formatter.named;

import de.sayayi.lib.message.formatter.AbstractParameterFormatter;
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.formatter.NamedParameterFormatter;
import de.sayayi.lib.message.internal.part.MessagePart.Text;
import lombok.val;
import lombok.var;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.EnumSet;
import java.util.Set;

import static de.sayayi.lib.message.data.map.MapKey.Type.STRING;
import static de.sayayi.lib.message.internal.part.MessagePartFactory.noSpaceText;
import static de.sayayi.lib.message.internal.part.MessagePartFactory.nullText;
import static java.util.Collections.emptySet;


/**
 * @author Jeroen Gremmen
 */
public final class FileSizeFormatter extends AbstractParameterFormatter implements NamedParameterFormatter
{
  private static final String[] UNITS = new String[] { "B", "KB", "MB", "GB", "TB", "PB" };

  private static final long[] POW10 = new long[] {
      1L, 10L, 100L, 1000L, 10000L, 100000L, 1000000L, 10000000L, 100000000L, 1000000000L, 10000000000L,
      100000000000L, 1000000000000L, 10000000000000L, 100000000000000L, 1000000000000000L
  };

  private static final String[] FORMAT = new String[] { "#,##0", "#,##0.0", "#,##0.00", "#,##0.000" };


  @Override
  public @NotNull String getName() {
    return "filesize";
  }


  @Override
  public boolean canFormat(@NotNull Class<?> type) {
    return Number.class.isAssignableFrom(type);
  }


  @Override
  protected @NotNull Text formatValue(@NotNull FormatterContext formatterContext, Object value)
  {
    if (!(value instanceof Number))
      return nullText();

    val size = ((Number)value).longValue();
    var scale = normalizeScale(formatterContext.getConfigValueNumber("scale").orElse(1));
    val s = new StringBuilder();
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

      s.append(new DecimalFormat(FORMAT[scale], DecimalFormatSymbols.getInstance(formatterContext.getLocale()))
          .format((double)size / POW10[unitIndex * 3]));
    }

    val unit = UNITS[unitIndex];
    val unitMessage = formatterContext.getMapMessage(unit, EnumSet.of(STRING)).orElse(null);

    if ((unitMessage != null && unitMessage.isSpaceBefore()) ||
        formatterContext.getConfigValueBool("space").orElse(false))
      s.append(' ');

    return noSpaceText(s.append(unitMessage == null
        ? unit : unitMessage.format(formatterContext.getMessageContext(), formatterContext)).toString());
  }


  @Override
  public @NotNull Set<FormattableType> getFormattableTypes() {
    return emptySet();
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