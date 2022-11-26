/*
 * Copyright 2019 Jeroen Gremmen
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

import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.MessageContext.Parameters;
import de.sayayi.lib.message.data.DataMap;
import de.sayayi.lib.message.formatter.AbstractParameterFormatter;
import de.sayayi.lib.message.formatter.NamedParameterFormatter;
import de.sayayi.lib.message.internal.part.MessagePart.Text;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.sayayi.lib.message.internal.part.MessagePartFactory.noSpaceText;
import static de.sayayi.lib.message.internal.part.MessagePartFactory.nullText;
import static java.util.Collections.emptySet;


/**
 * @author Jeroen Gremmen
 */
public class GeoFormatter extends AbstractParameterFormatter implements NamedParameterFormatter
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
  @SuppressWarnings("squid:S3776")
  public @NotNull Text formatValue(@NotNull MessageContext messageContext, Object value, String format,
                                   @NotNull Parameters parameters, DataMap map)
  {
    if (value == null)
      return nullText();

    final Format fmt = getFormat(messageContext, parameters, map);
    final StringBuilder s = new StringBuilder();
    final double v = ((Number)value).doubleValue();
    final double[] dms = dmsSplitter(fmt, v);

    if (v < 0 && !fmt.hasLoLa() && (dms[0] > 0 || dms[1] > 0 || dms[2] > 0))
      s.append('-');

    s.append((int)dms[0]).append('\u00b0');
    if (fmt.separatorAfterDegree)
      s.append(' ');

    if (fmt.hasMinutes())
    {
      s.append(formatMinOrSec(parameters.getLocale(), dms[1], fmt.minuteDigits, fmt.zeroPadMinutes)).append('\'');
      if (fmt.separatorAfterMinute)
        s.append(' ');

      if (fmt.hasSeconds())
      {
        s.append(formatMinOrSec(parameters.getLocale(), dms[2], fmt.secondDigits, fmt.zeroPadSeconds)).append('"');
        if (fmt.separatorAfterSecond)
          s.append(' ');
      }
    }

    if (fmt.hasLoLa())
    {
      if (fmt.longitude)
        s.append(v < 0 ? 'W' : 'E');
      else
        s.append(v < 0 ? 'S' : 'N');
    }

    return noSpaceText(s.toString());
  }


  private @NotNull Format getFormat(@NotNull MessageContext messageContext,
                                    @NotNull Parameters parameters, DataMap map)
  {
    String formatString = getConfigValueString(messageContext, "geo", parameters, map, "dms");
    Format format = FORMAT.get(formatString);

    return format == null ? parseFormatString(formatString) : format;
  }


  private static final int[] DIGIT_FACTOR = new int[] { 1000, 100, 10, 1 };
  private static final int DEGREE_MILLIS = 3600000;
  private static final int MINUTE_MILLIS = 60000;

  static double[] dmsSplitter(Format fmt, double v)
  {
    long millis = Math.round(Math.abs(v) * DEGREE_MILLIS);

    if (!fmt.hasMinutes() && !fmt.hasSeconds())
      return new double[] { (int)((millis + DEGREE_MILLIS / 2) / DEGREE_MILLIS), 0.0, 0.0 };

    if (fmt.hasMinutes() && !fmt.hasSeconds())
    {
      // round value to the number of minute digits
      final int factor = MINUTE_MILLIS / 1000 * DIGIT_FACTOR[fmt.minuteDigits];
      millis = (millis + factor / 2) / factor * factor;

      final int degree = (int)(millis / DEGREE_MILLIS);

      return new double[] { degree, (millis - degree * DEGREE_MILLIS) / (double)MINUTE_MILLIS, 0.0 };
    }

    // round value to the number of second digits
    final int factor = DIGIT_FACTOR[fmt.secondDigits];
    millis = ((millis + factor / 2) / factor) * factor;

    final long degree = millis / DEGREE_MILLIS;
    millis -= degree * DEGREE_MILLIS;
    final long minute = millis / MINUTE_MILLIS;
    millis -= minute * MINUTE_MILLIS;

    return new double[] { degree, minute, millis / 1000.0 };
  }


  @SuppressWarnings("squid:S3457")
  private String formatMinOrSec(Locale locale, double d, int digits, boolean zeropad)
  {
    String s = String.format(locale, "%." + digits + 'f', d);

    if (zeropad && d < 10.0)
      s = "0" + s;

    return s;
  }


  @Override
  public @NotNull Set<Class<?>> getFormattableTypes() {
    return emptySet();
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

  static @NotNull Format parseFormatString(@NotNull String formatString)
  {
    Matcher matcher = PATTERN_FORMAT.matcher(formatString.trim());
    if (!matcher.matches())
      throw new IllegalArgumentException("invalid geo format: " + formatString);

    Format format = new Format();

    format.longitude = matcher.group(8) == null ? null : "LO".equals(matcher.group(8));
    format.separatorAfterDegree = matcher.group(1) != null;
    format.separatorAfterMinute = matcher.group(4) != null;
    format.separatorAfterSecond = matcher.group(7) != null;
    format.zeroPadMinutes = matcher.group(2) != null;
    format.zeroPadSeconds = matcher.group(5) != null;

    String mmm = matcher.group(3);
    if (mmm != null)
      format.minuteDigits = "m".equals(mmm) ? 0 : mmm.length();

    String sss = matcher.group(6);
    if (sss != null)
    {
      if (mmm == null)
        throw new IllegalArgumentException("missing minute specification in geo format: " + formatString);

      format.minuteDigits = 0;  // reduce precision for minutes
      format.secondDigits = "s".equals(sss) ? 0 : sss.length();
    }

    return format;
  }




  @NoArgsConstructor
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