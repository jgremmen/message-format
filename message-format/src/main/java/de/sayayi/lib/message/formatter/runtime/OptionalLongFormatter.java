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
package de.sayayi.lib.message.formatter.runtime;

import de.sayayi.lib.message.formatter.AbstractSingleTypeParameterFormatter;
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.formatter.ParameterFormatter.ConfigKeyComparator;
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.OptionalLong;

import static de.sayayi.lib.message.part.TextPartFactory.emptyText;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult.MISMATCH;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult.TYPELESS_EXACT;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.Type.EMPTY;


/**
 * @author Jeroen Gremmen
 */
public final class OptionalLongFormatter
    extends AbstractSingleTypeParameterFormatter<OptionalLong>
    implements ConfigKeyComparator<OptionalLong>
{
  @Override
  @Contract(pure = true)
  public @NotNull Text formatValue(@NotNull FormatterContext context,
                                   @NotNull OptionalLong optionalLong)
  {
    return optionalLong.isPresent()
        ? context.format(optionalLong.getAsLong(), long.class, true)
        : emptyText();
  }


  @Override
  public @NotNull FormattableType getFormattableType() {
    return new FormattableType(OptionalLong.class);
  }


  @Override
  public @NotNull MatchResult compareToConfigKey(@NotNull OptionalLong value,
                                                 @NotNull ComparatorContext context)
  {
    return context.getKeyType() == EMPTY
        ? context.getCompareType().match(value.isPresent() ? 1 : 0) ? TYPELESS_EXACT : MISMATCH
        : value.isPresent() ? context.matchForObject(value.getAsLong(), long.class) : MISMATCH;
  }
}
