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
import de.sayayi.lib.message.formatter.ParameterFormatter.SizeQueryable;
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.part.parameter.key.ConfigKey;
import de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.Reference;
import java.util.OptionalLong;

import static de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult.*;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.Type.EMPTY;


/**
 * @author Jeroen Gremmen
 */
public final class ReferenceFormatter
    extends AbstractSingleTypeParameterFormatter<Reference<?>>
    implements SizeQueryable, ConfigKeyComparator<Reference<?>>
{
  @Override
  public @NotNull Text formatValue(@NotNull FormatterContext context,
                                   @NotNull Reference<?> reference) {
    return context.format(reference.get(), true);
  }


  @Override
  public @NotNull OptionalLong size(@NotNull FormatterContext context, @NotNull Object value) {
    return context.size(((Reference<?>)value).get());
  }


  @Override
  public @NotNull FormattableType getFormattableType() {
    return new FormattableType(Reference.class);
  }


  @Override
  public @NotNull MatchResult compareToConfigKey(@NotNull Reference<?> value,
                                                 @NotNull ComparatorContext context)
  {
    final ConfigKey.Type keyType = context.getKeyType();

    if (keyType.isNullOrEmpty())
    {
      return context.getCompareType().match(value.get() == null ? 0 : 1)
          ? keyType == EMPTY ? TYPELESS_EXACT : TYPELESS_LENIENT
          : MISMATCH;
    }

    return context.matchForObject(value.get());
  }
}
