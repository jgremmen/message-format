/*
 * Copyright 2020 Jeroen Gremmen
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
package de.sayayi.lib.message.data.map;

import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.pack.PackInputStream;
import de.sayayi.lib.message.pack.PackOutputStream;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Locale;

import static de.sayayi.lib.message.data.map.MapKey.MatchResult.*;


/**
 * @author Jeroen Gremmen
 */
@ToString(doNotUseGetters = true)
@EqualsAndHashCode(doNotUseGetters = true)
@AllArgsConstructor
public final class MapKeyNumber implements MapKey
{
  public static final byte PACK_ID = 5;

  private static final long serialVersionUID = 800L;

  private final @NotNull CompareType compareType;
  @Getter private final long number;


  public MapKeyNumber(@NotNull CompareType compareType, @NotNull String number) {
    this(compareType, Long.parseLong(number));
  }


  @Override
  public @NotNull Type getType() {
    return Type.NUMBER;
  }


  @Override
  public @NotNull MatchResult match(@NotNull MessageContext messageContext, @NotNull Locale locale, Object value)
  {
    if (value == null)
      return MISMATCH;

    MatchResult result = EXACT;
    int cmp;

    doMatch: {
      if (value instanceof Long || value instanceof Integer || value instanceof Short || value instanceof Byte)
      {
        cmp = Long.signum(((Number)value).longValue() - number);
        break doMatch;
      }

      if (value instanceof BigInteger)
      {
        cmp = ((BigInteger)value).compareTo(BigInteger.valueOf(number));
        break doMatch;
      }

      if (value instanceof CharSequence || value instanceof Character)
      {
        try {
          value = new BigDecimal(value.toString());
          result = LENIENT;
        } catch(Exception ignore) {
        }
      }

      if (value instanceof BigDecimal)
      {
        cmp = ((BigDecimal)value).compareTo(BigDecimal.valueOf(number));
        break doMatch;
      }

      if (value instanceof Float)
      {
        cmp = Float.compare((Float)value, number);
        break doMatch;
      }

      if (value instanceof Number)
      {
        cmp = Double.compare(((Number)value).doubleValue(), number);
        break doMatch;
      }

      return MISMATCH;
    }

    return compareType.match(cmp) ? result : MISMATCH;
  }


  /**
   * @param packStream  data output pack target
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   */
  public void pack(@NotNull PackOutputStream packStream) throws IOException
  {
    packStream.write(PACK_ID, 3);
    packStream.writeEnum(compareType);
    packStream.writeLong(number);
  }


  /**
   * @param packStream  source data input, not {@code null}
   *
   * @return  unpacked number map key, never {@code null}
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   */
  public static @NotNull MapKeyNumber unpack(@NotNull PackInputStream packStream) throws IOException {
    return new MapKeyNumber(packStream.readEnum(CompareType.class), packStream.readLong());
  }
}
