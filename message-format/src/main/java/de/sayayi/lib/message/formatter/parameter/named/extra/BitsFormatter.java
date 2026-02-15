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
package de.sayayi.lib.message.formatter.parameter.named.extra;

import de.sayayi.lib.message.formatter.parameter.AbstractParameterFormatter;
import de.sayayi.lib.message.formatter.parameter.FormatterContext;
import de.sayayi.lib.message.formatter.parameter.NamedParameterFormatter;
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.part.config.ConfigValue.NumberValue;
import de.sayayi.lib.message.part.config.ConfigValue.StringValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.math.BigInteger;
import java.util.Set;

import static de.sayayi.lib.message.part.TextPartFactory.emptyText;
import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;


/**
 * @author Jeroen Gremmen
 */
public final class BitsFormatter extends AbstractParameterFormatter<Object>
    implements NamedParameterFormatter
{
  @Override
  public @NotNull String getName() {
    return "bits";
  }


  @Override
  public boolean canFormat(@NotNull Class<?> type)
  {
    return
        Number.class.isAssignableFrom(type) ||
        type == long.class ||
        type == int.class ||
        type == short.class ||
        type == byte.class ||
        type == char.class ||
        type == NULL_TYPE;
  }


  @Override
  public @NotNull Text formatValue(@NotNull FormatterContext context, @NotNull Object value)
  {
    final var bitCount = detectBitCount(context, value);
    return bitCount > 0 ? noSpaceText(format(bitCount, value)) : emptyText();
  }


  private int detectBitCount(@NotNull FormatterContext context, @NotNull Object value)
  {
    final var configValue = context.getConfigValue("bits").orElse(null);

    if (configValue instanceof StringValue && "auto".equals(configValue.asObject()))
      return autoDetectBitCount(value);

    if (configValue instanceof NumberValue numberValue)
    {
      var bits = numberValue.intValue();
      if (bits > 0 && bits <= 1024)
        return bits;
    }

    return detectBitCountByRange(value);
  }


  private int autoDetectBitCount(Object value)
  {
    // autodetect for big integer in range 0..Long.MAX_VALUE
    if (value instanceof BigInteger bigInteger)
    {
      if (BigInteger.ZERO.equals(bigInteger))
        return 1;
      else if (bigInteger.signum() == 1 && bigInteger.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) <= 0)
        return 64 - Long.numberOfLeadingZeros(bigInteger.longValue());
    }

    // autodetect for numbers of type byte, short, integer or long
    if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long)
    {
      final var l = ((Number)value).longValue();

      if (l == 0L)
        return 1;
      else if (Long.signum(l) == 1)
        return 64 - Long.numberOfLeadingZeros(l);

      // fixed size for negative values of type byte, short, integer or long
      return detectBitCountByRange(value);
    }

    // bit count detection failed
    return 0;
  }


  @SuppressWarnings("unused")
  private int detectBitCountByRange(Object value)
  {
    return switch(value) {
      case Byte b -> 8;
      case Short i -> 16;
      case Integer i -> 32;
      case Long l -> 64;
      case null, default -> 0;
    };
  }


  private String format(int bitCount, Object value)
  {
    final var bits = new char[bitCount];

    if (value instanceof BigInteger && bitCount > 64)
      formatBigInteger(bits, (BigInteger)value);
    else if (value instanceof Number)
      formatLong(bits, ((Number)value).longValue());

    return new String(bits);
  }


  private void formatLong(char[] bits, long value)
  {
    for(var n = bits.length; --n >= 0; value >>= 1)
      bits[n] = (char)('0' + (value & 1));
  }


  private void formatBigInteger(char[] bits, BigInteger value)
  {
    for(var n = bits.length; --n >= 0; value = value.shiftRight(1))
      bits[n] = value.testBit(0) ? '1' : '0';
  }


  @Override
  public @Unmodifiable @NotNull Set<String> getParameterConfigNames() {
    return Set.of("bits");
  }
}
