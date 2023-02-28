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
package de.sayayi.lib.message.parameter.key;

import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.pack.PackInputStream;
import de.sayayi.lib.message.pack.PackOutputStream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Locale;

import static de.sayayi.lib.message.parameter.key.ConfigKey.MatchResult.*;
import static lombok.AccessLevel.PRIVATE;


/**
 * @author Jeroen Gremmen
 */
@ToString(doNotUseGetters = true)
@AllArgsConstructor(access = PRIVATE)
public enum ConfigKeyBool implements ConfigKey
{
  FALSE(false),
  TRUE(true);


  private static final long serialVersionUID = 800L;

  @Getter private final boolean bool;


  @Override
  public @NotNull Type getType() {
    return Type.BOOL;
  }


  @Override
  public @NotNull MatchResult match(@NotNull MessageContext messageContext, @NotNull Locale locale, Object value)
  {
    if (value != null)
    {
      if (value instanceof Boolean && (Boolean)value == bool)
        return EXACT;

      if (value instanceof BigInteger)
        return ((((BigInteger)value).signum() != 0) == bool) ? LENIENT : MISMATCH;

      if (value instanceof CharSequence || value instanceof Character)
      {
        final String string = value.toString();

        if (("true".equalsIgnoreCase(string) && bool) || ("false".equalsIgnoreCase(string) && !bool))
          return EQUIVALENT;

        try {
          value = new BigDecimal(string);
        } catch(Exception ignore) {
        }
      }

      if (value instanceof BigDecimal)
       return ((((BigDecimal)value).signum() != 0) == bool) ? LENIENT : MISMATCH;

      if (value instanceof Number && (((Number)value).longValue() != 0) == bool)
        return LENIENT;
    }

    return MISMATCH;
  }


  /**
   * @param packStream  data output pack target
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   */
  public void pack(@NotNull PackOutputStream packStream) throws IOException {
    packStream.writeBoolean(bool);
  }


  /**
   * @param packStream  source data input, not {@code null}
   *
   * @return  unpacked boolean map key, never {@code null}
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   */
  public static @NotNull ConfigKeyBool unpack(@NotNull PackInputStream packStream) throws IOException {
    return packStream.readBoolean() ? TRUE : FALSE;
  }
}
