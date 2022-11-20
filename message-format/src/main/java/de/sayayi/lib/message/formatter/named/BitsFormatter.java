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
import de.sayayi.lib.message.data.Data;
import de.sayayi.lib.message.data.DataMap;
import de.sayayi.lib.message.data.DataNumber;
import de.sayayi.lib.message.data.DataString;
import de.sayayi.lib.message.formatter.AbstractParameterFormatter;
import de.sayayi.lib.message.formatter.NamedParameterFormatter;
import de.sayayi.lib.message.internal.part.MessagePart.Text;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.Set;

import static de.sayayi.lib.message.internal.part.MessagePartFactory.*;
import static java.util.Collections.emptySet;


/**
 * @author Jeroen Gremmen
 */
public final class BitsFormatter extends AbstractParameterFormatter implements NamedParameterFormatter
{
  @Override
  public @NotNull String getName() {
    return "bits";
  }


  @Override
  public @NotNull Text formatValue(@NotNull MessageContext messageContext, Object value, String format,
                                   @NotNull Parameters parameters, DataMap map)
  {
    if (!(value instanceof Number))
      return nullText();

    final int bitCount = detectBitCount(messageContext, parameters, map, value);
    return bitCount > 0 ? noSpaceText(format(bitCount, value)) : emptyText();
  }


  private int detectBitCount(@NotNull MessageContext messageContext, @NotNull Parameters parameters,
                             DataMap map, Object value)
  {
    final Data dataValue = getConfigValue(messageContext, "length", parameters, map);

    if (dataValue instanceof DataString && "auto".equals(dataValue.asObject()))
      return autoDetectBitCount(value);

    if (dataValue instanceof DataNumber)
    {
      int bits = ((DataNumber)dataValue).asObject().intValue();
      if (bits > 0 && bits <= 64)
        return bits;
    }

    return detectBitCountByRange(value);
  }


  private int autoDetectBitCount(Object value)
  {
    // auto detect for big integer in range 0..Long.MAX_VALUE
    if (value instanceof BigInteger)
    {
      BigInteger i = (BigInteger)value;

      if (BigInteger.ZERO.equals(i))
        return 1;
      else if (i.signum() == 1 && i.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) <= 0)
        return 64 - Long.numberOfLeadingZeros(i.longValue());
    }

    // auto detect for numbers of type byte, short, integer or long
    if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long)
    {
      long l = ((Number)value).longValue();

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


  private int detectBitCountByRange(Object value)
  {
    if (value instanceof Byte)
      return 8;
    else if (value instanceof Short)
      return 16;
    else if (value instanceof Integer)
      return 32;
    else if (value instanceof Long)
      return 64;

    return 0;
  }


  private String format(int bitCount, Object value)
  {
    final char[] bits = new char[bitCount];

    if (value instanceof BigInteger && bitCount > 64)
      formatBigInteger(bits, (BigInteger)value);
    else if (value instanceof Number)
      formatLong(bits, ((Number)value).longValue());

    return new String(bits);
  }


  private void formatLong(char[] bits, long value)
  {
    for(int n = bits.length; --n >= 0; value >>= 1)
      bits[n] = (char)('0' + (value & 1));
  }


  private void formatBigInteger(char[] bits, BigInteger value)
  {
    for(int n = bits.length; --n >= 0; value = value.shiftRight(1))
      bits[n] = value.testBit(0) ? '1' : '0';
  }


  @Override
  public @NotNull Set<Class<?>> getFormattableTypes() {
    return emptySet();
  }
}