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
package de.sayayi.lib.message.formatter.support;

import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.data.Data;
import de.sayayi.lib.message.data.map.MapKey.CompareType;
import de.sayayi.lib.message.data.map.MapKey.MatchResult;
import de.sayayi.lib.message.formatter.NamedParameterFormatter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static de.sayayi.lib.message.data.map.MapKey.MatchResult.TYPELESS_EXACT;
import static de.sayayi.lib.message.data.map.MapKey.MatchResult.TYPELESS_LENIENT;


/**
 * @author Jeroen Gremmen
 */
public final class StringFormatter extends AbstractParameterFormatter implements NamedParameterFormatter
{
  @NotNull
  @Override
  @Contract(pure = true)
  public String getName() {
    return "string";
  }


  @Override
  @Contract(pure = true)
  @SuppressWarnings({"squid:S3358", "squid:S3776"})
  public String formatValue(Object value, String format, @NotNull Parameters parameters, Data data) {
    return value == null ? null : ((value instanceof char[]) ? new String((char[])value) : String.valueOf(value)).trim();
  }


  @Override
  public MatchResult matchEmpty(@NotNull CompareType compareType, @NotNull Object value)
  {
    final String s = value instanceof char[] ? new String((char[])value) : String.valueOf(value);
    final boolean empty = s.isEmpty();
    final boolean lenientEmpty = s.trim().isEmpty();

    if (compareType == CompareType.EQ)
      return empty ? TYPELESS_EXACT : (lenientEmpty ? TYPELESS_LENIENT : null);
    else
      return lenientEmpty ? (empty ? null : TYPELESS_LENIENT) : TYPELESS_EXACT;
  }


  @NotNull
  @Override
  @Contract(value = "-> new", pure = true)
  public Set<Class<?>> getFormattableTypes() {
    return new HashSet<Class<?>>(Arrays.asList(CharSequence.class, char[].class));
  }
}
