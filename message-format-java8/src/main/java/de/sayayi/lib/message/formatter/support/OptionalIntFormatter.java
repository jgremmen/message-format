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
package de.sayayi.lib.message.formatter.support;

import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.data.Data;
import de.sayayi.lib.message.data.map.MapKey.CompareType;
import de.sayayi.lib.message.data.map.MapKey.MatchResult;
import de.sayayi.lib.message.formatter.ParameterFormatter.EmptyMatcher;
import de.sayayi.lib.message.internal.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;


/**
 * @author Jeroen Gremmen
 */
public final class OptionalIntFormatter extends AbstractParameterFormatter implements EmptyMatcher
{
  @NotNull
  @SuppressWarnings("squid:S2789")
  @Override
  @Contract(pure = true)
  public Text formatValue(Object value, String format, @NotNull Parameters parameters, Data data)
  {
    if (value == null)
      return Text.NULL;

    final OptionalInt optional = (OptionalInt)value;
    if (!optional.isPresent())
      return Text.EMPTY;

    return parameters.getFormatter(format, int.class).format(optional.getAsInt(), format, parameters, data);
  }


  @Override
  public MatchResult matchEmpty(@NotNull CompareType compareType, @NotNull Object value) {
    return compareType.match(((Optional<?>)value).isPresent() ? 1 : 0) ? MatchResult.TYPELESS_EXACT : null;
  }


  @NotNull
  @Override
  @Contract(value = "-> new", pure = true)
  public Set<Class<?>> getFormattableTypes() {
    return Collections.singleton(Optional.class);
  }
}
