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
package de.sayayi.lib.message.formatter.runtime;

import de.sayayi.lib.message.formatter.AbstractSingleTypeParameterFormatter;
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.formatter.ParameterFormatter.ConfigKeyComparator;
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.part.parameter.key.ConfigKey;
import de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

import static de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult.MISMATCH;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult.TYPELESS_EXACT;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.Type.*;


/**
 * @author Jeroen Gremmen
 */
public final class AtomicBooleanFormatter
    extends AbstractSingleTypeParameterFormatter<AtomicBoolean>
    implements ConfigKeyComparator<AtomicBoolean>
{
  @Override
  public @NotNull Text formatValue(@NotNull FormatterContext context,
                                   @NotNull AtomicBoolean atomicBoolean) {
    return context.format(atomicBoolean.get(), boolean.class, true);
  }


  @Override
  public @NotNull FormattableType getFormattableType() {
    return new FormattableType(AtomicBoolean.class);
  }


  @Override
  public @NotNull MatchResult compareToConfigKey(@NotNull AtomicBoolean value,
                                                 @NotNull ComparatorContext context)
  {
    if (context.getKeyType() == EMPTY)
      return context.getCompareType().match(1) ? TYPELESS_EXACT : MISMATCH;

    final ConfigKey.Type keyType = context.getKeyType();
    return keyType == BOOL || keyType == STRING
        ? context.matchForObject(value.get(), boolean.class) : MISMATCH;
  }
}
