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

import de.sayayi.lib.message.MessageSupport.MessageSupportAccessor;
import de.sayayi.lib.message.pack.PackInputStream;
import de.sayayi.lib.message.pack.PackOutputStream;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Locale;

import static de.sayayi.lib.message.parameter.key.ConfigKey.MatchResult.*;


/**
 * @author Jeroen Gremmen
 */
public enum ConfigKeyBool implements ConfigKey
{
  /** Config key representing {@code false}. */
  FALSE(false),

  /** Config key representing {@code true}. */
  TRUE(true);


  private static final long serialVersionUID = 800L;

  private final boolean bool;


  ConfigKeyBool(boolean bool) {
    this.bool = bool;
  }


  /**
   * Returns the config key boolean value.
   *
   * @return  config key boolean value
   */
  @Contract(pure = true)
  public boolean isBool() {
    return bool;
  }


  /**
   * {@inheritDoc}
   *
   * @return  always {@link Type#BOOL Type#BOOL}
   */
  @Override
  public @NotNull Type getType() {
    return Type.BOOL;
  }


  @Override
  public @NotNull MatchResult match(@NotNull MessageSupportAccessor messageSupportAccessor,
                                    @NotNull Locale locale, Object value)
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

        if (("true".equalsIgnoreCase(string) && bool) ||
            ("false".equalsIgnoreCase(string) && !bool))
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


  @Override
  public String toString() {
    return "ConfigKeyBool(bool=" + bool + ')';
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
  public static @NotNull ConfigKeyBool unpack(@NotNull PackInputStream packStream)
      throws IOException {
    return packStream.readBoolean() ? TRUE : FALSE;
  }
}
