/*
 * Copyright 2020 Jeroen Gremmen
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
package de.sayayi.lib.message.data.map;

import de.sayayi.lib.message.MessageContext;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.text.Collator;
import java.util.Locale;

import static de.sayayi.lib.message.data.map.MapKey.CompareType.EQ;
import static de.sayayi.lib.message.data.map.MapKey.CompareType.NE;
import static de.sayayi.lib.message.data.map.MapKey.MatchResult.*;


/**
 * @author Jeroen Gremmen
 */
@ToString(doNotUseGetters = true)
@EqualsAndHashCode(doNotUseGetters = true)
@AllArgsConstructor
public final class MapKeyString implements MapKey
{
  private static final long serialVersionUID = 800L;

  private final @NotNull CompareType compareType;
  private final @NotNull String string;


  @Override
  public @NotNull Type getType() {
    return Type.STRING;
  }


  @Override
  public @NotNull MatchResult match(@NotNull MessageContext messageContext, @NotNull Locale locale, Object value)
  {
    if (value == null)
      return MISMATCH;

    MatchResult result = EXACT;
    int cmp = 0;

    doMatch: {
      if (!(value instanceof CharSequence || value instanceof Character))
        result = EQUIVALENT;

      final String text = value.toString();

      if (compareType == EQ)
      {
        if (text.equals(string))
          break doMatch;

        //noinspection DuplicateExpressions
        if (text.toLowerCase(locale).equals(string.toLowerCase(locale)))
        {
          result = LENIENT;
          break doMatch;
        }

        cmp = 1;
        break doMatch;
      }

      //noinspection DuplicateExpressions
      if (compareType == NE && !text.toLowerCase(locale).equals(string.toLowerCase(locale)))
      {
        result = LENIENT;
        cmp = 1;
        break doMatch;
      }

      cmp = Collator.getInstance(locale).compare(text, string);
    }

    return compareType.match(cmp) ? result : MISMATCH;
  }


  /**
   * @param dataOutput  data output pack target
   *
   * @throws IOException  if an I/O error occurs.
   *
   * @since 0.8.0
   */
  public void pack(@NotNull DataOutput dataOutput) throws IOException
  {
    dataOutput.writeByte(6);
    dataOutput.writeByte(compareType.ordinal());
    dataOutput.writeUTF(string);
  }


  /**
   * @param dataInput  source data input, not {@code null}
   *
   * @return  unpacked string map key, never {@code null}
   *
   * @throws IOException  if an I/O error occurs.
   *
   * @since 0.8.0
   */
  public static @NotNull MapKeyNumber unpack(@NotNull DataInput dataInput) throws IOException
  {
    final CompareType compareType = CompareType.values()[dataInput.readByte() & 0xf];

    return new MapKeyNumber(compareType, dataInput.readUTF());
  }
}
