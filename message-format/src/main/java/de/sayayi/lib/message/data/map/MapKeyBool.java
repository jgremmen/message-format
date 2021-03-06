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
import de.sayayi.lib.message.MessageContext.Parameters;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.BigInteger;

import static de.sayayi.lib.message.data.map.MapKey.MatchResult.EQUIVALENT;
import static de.sayayi.lib.message.data.map.MapKey.MatchResult.EXACT;
import static de.sayayi.lib.message.data.map.MapKey.MatchResult.LENIENT;
import static de.sayayi.lib.message.data.map.MapKey.MatchResult.MISMATCH;


/**
 * @author Jeroen Gremmen
 */
@ToString(doNotUseGetters = true)
@AllArgsConstructor
public final class MapKeyBool implements MapKey
{
  private static final long serialVersionUID = 500L;

  @Getter private final boolean bool;


  public MapKeyBool(@NotNull String bool) {
    this(Boolean.parseBoolean(bool));
  }


  @Override
  public @NotNull Type getType() {
    return Type.BOOL;
  }


  @Override
  @SuppressWarnings({"java:S3776", "java:S5411", "java:S108"})
  public @NotNull MatchResult match(@NotNull MessageContext messageContext, @NotNull Parameters parameters,
                                    Object value)
  {
    if (value != null)
    {
      if (value instanceof Boolean && (Boolean)value == bool)
        return EXACT;

      if ("true".equals(value) && bool)
        return EQUIVALENT;
      if ("false".equals(value) && !bool)
        return EQUIVALENT;

      if (value instanceof BigInteger)
        return ((((BigInteger)value).signum() != 0) == bool) ? LENIENT : MISMATCH;

      if (value instanceof CharSequence || value instanceof Character)
      {
        try {
          value = new BigDecimal(value.toString());
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
}
