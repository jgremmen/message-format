/*
 * Copyright 2019 Jeroen Gremmen
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

import de.sayayi.lib.message.formatter.AbstractParameterFormatter;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.formatter.NamedParameterFormatter;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static java.lang.Math.round;


/**
 * @author Jeroen Gremmen
 */
public final class GeoFormatter extends AbstractParameterFormatter<Number> implements NamedParameterFormatter
{
  private static final Map<String,Format> FORMAT = new HashMap<>();

  static {
    // longitude
    FORMAT.put("short-longitude", new Format(true, 0, -1));  // 12°45'E
    FORMAT.put("longitude", new Format(true, 0, 0));         // 12°45'3"E
    FORMAT.put("medium-longitude", new Format(true, 0, 1));  // 12°45'2.9"E
    FORMAT.put("long-longitude", new Format(true, 0, 3));    // 12°45'2.581"E

    // latitude
    FORMAT.put("short-latitude", new Format(false, 0, -1));  // 12°45'S
    FORMAT.put("latitude", new Format(false, 0, 0));         // 12°45'3"S
    FORMAT.put("medium-latitude", new Format(false, 0, 1));  // 12°45'2.9"S
    FORMAT.put("long-latitude", new Format(false, 0, 3));    // 12°45'2.581"S
  }


  @Override
  public @NotNull String getName() {
    return "geo";
  }


  @Override
  public boolean canFormat(@NotNull Class<?> type)
  {
    return
        Number.class.isAssignableFrom(type) ||
        type == double.class ||
        type == float.class ||
        type == NULL_TYPE;
  }


  @Override
  public @NotNull Text formatValue(@NotNull FormatterContext context, @NotNull Number number)
  {
    var fmt = getFormat(context);
    var s = new StringBuilder();
    var v = number.doubleValue();
    var dms = dmsSplitter(fmt, v);

    if (!fmt.hasLoLa() && v < 0.0 && (dms[0] > 0 || dms[1] > 0 || dms[2] > 0))
      s.append('-');

    s.append((int)dms[0]).append('°');
    if (fmt.separatorAfterDegree)
      s.append(' ');

    if (fmt.hasMinutes())
    {
      var locale = context.getLocale();

      s.append(formatMinOrSec(locale, dms[1], fmt.minuteDigits, fmt.zeroPadMinutes)).append('\'');
      if (fmt.separatorAfterMinute)
        s.append(' ');

      if (fmt.hasSeconds())
      {
        s.append(formatMinOrSec(locale, dms[2], fmt.secondDigits, fmt.zeroPadSeconds)).append('"');
        if (fmt.separatorAfterSecond)
          s.append(' ');
      }
    }

    if (fmt.hasLoLa())
    {
      if (fmt.longitude)
      {
        s.append(v < 0.0
            ? context.getConfigValueString("geo-w").orElse("W")
            : context.getConfigValueString("geo-e").orElse("E"));
      }
      else
      {
        s.append(v < 0.0
            ? context.getConfigValueString("geo-s").orElse("S")
            : context.getConfigValueString("geo-n").orElse("N"));
      }
    }

    return noSpaceText(s.toString());
  }


  private @NotNull Format getFormat(@NotNull FormatterContext context)
  {
    var formatString = context.getConfigValueString("geo").orElse("dms");
    var format = FORMAT.get(formatString);

    return format == null ? parseFormatString(formatString) : format;
  }


  @Override
  public @Unmodifiable @NotNull Set<String> getParameterConfigNames() {
    return Set.of("geo", "geo-w", "geo-e", "geo-n", "geo-s");
  }


  private static final int[] DIGIT_FACTOR = new int[] { 1000, 100, 10, 1 };
  private static final int DEGREE_MILLIS = 3600000;
  private static final int MINUTE_MILLIS = 60000;

  @Contract(pure = true)
  static double[] dmsSplitter(@NotNull Format fmt, double v)
  {
    var millis = round(Math.abs(v) * DEGREE_MILLIS);

    if (!fmt.hasSeconds())
    {
      if (!fmt.hasMinutes())
        return new double[] { (int)((millis + DEGREE_MILLIS / 2) / DEGREE_MILLIS), 0.0, 0.0 };

      // round value to the number of minute digits
      var factor = MINUTE_MILLIS / 1000 * DIGIT_FACTOR[fmt.minuteDigits];
      millis = (millis + factor / 2) / factor * factor;

      var degree = (int)(millis / DEGREE_MILLIS);

      return new double[] {
          degree, (millis - degree * DEGREE_MILLIS) / (double)MINUTE_MILLIS, 0.0
      };
    }

    // round value to the number of second digits
    var factor = DIGIT_FACTOR[fmt.secondDigits];
    millis = ((millis + factor / 2) / factor) * factor;

    var degree = millis / DEGREE_MILLIS;
    millis -= degree * DEGREE_MILLIS;
    var minute = millis / MINUTE_MILLIS;
    millis -= minute * MINUTE_MILLIS;

    return new double[] { degree, minute, millis / 1000.0 };
  }


  @Contract(pure = true)
  private @NotNull String formatMinOrSec(@NotNull Locale locale, double d, int digits, boolean zeroPadding)
  {
    //noinspection MalformedFormatString
    var s = String.format(locale, "%." + digits + 'f', d);

    if (zeroPadding && d < 10.0)
      s = "0" + s;

    return s;
  }


  private static final Pattern PATTERN_FORMAT =
      Pattern.compile("d( )?(?:(0)?(m|M|MM|MMM))?( )?(?:(0)?(s|S|SS|SSS))?( )?(LO|LA)?");

  /*
     1 = ( )?  ->  separatorAfterDegree
     2 = (0)?  ->  zeroPadMinutes
     3 = (m|M|MM|MMM)  ->  withMinutes, minuteDigits
     4 = ( )?  ->  separatorAfterMinute
     5 = (0)?  ->  zeroPadSeconds
     6 = (s|S|SS|SSS)  ->  withSeconds, secondDigits
     7 = ( )?  ->  separatorAfterSecond
     8 = (LO|LA)  ->  longitude
   */

  @Contract(pure = true)
  static @NotNull Format parseFormatString(@NotNull String formatString)
  {
    var matcher = PATTERN_FORMAT.matcher(formatString.trim());
    var format = new Format();

    if (matcher.matches())
    {
      format.longitude = matcher.group(8) == null ? null : "LO".equals(matcher.group(8));
      format.separatorAfterDegree = matcher.group(1) != null;
      format.separatorAfterMinute = matcher.group(4) != null;
      format.separatorAfterSecond = matcher.group(7) != null;
      format.zeroPadMinutes = matcher.group(2) != null;
      format.zeroPadSeconds = matcher.group(5) != null;

      var minuteFormat = matcher.group(3);
      if (minuteFormat != null)
        format.minuteDigits = "m".equals(minuteFormat) ? 0 : minuteFormat.length();

      var secondsFormat = matcher.group(6);
      if (secondsFormat != null)
      {
        if (minuteFormat == null)
        {
          throw new IllegalArgumentException("missing minute specification in geo format: " +
              formatString);
        }

        format.minuteDigits = 0;  // reduce precision for minutes
        format.secondDigits = "s".equals(secondsFormat) ? 0 : secondsFormat.length();
      }
    }

    return format;
  }




  static final class Format
  {
    Boolean longitude;
    boolean separatorAfterDegree;
    boolean separatorAfterMinute;
    boolean separatorAfterSecond;
    boolean zeroPadMinutes;
    boolean zeroPadSeconds;
    int minuteDigits = -1;
    int secondDigits = -1;


    Format() {
    }


    Format(Boolean longitude, int minuteDigits, int secondDigits)
    {
      this.longitude = longitude;
      this.minuteDigits = minuteDigits;
      this.secondDigits = secondDigits;
    }


    boolean hasLoLa() {
      return longitude != null;
    }


    boolean hasMinutes() {
      return minuteDigits >= 0;
    }


    boolean hasSeconds() {
      return secondDigits >= 0;
    }
  }
}
