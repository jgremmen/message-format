/*
 * Copyright 2023 Jeroen Gremmen
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
package de.sayayi.lib.message.formatter.runtime.extra;

import de.sayayi.lib.message.formatter.AbstractSingleTypeParameterFormatter;
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.formatter.ParameterFormatter.ConfigKeyComparator;
import de.sayayi.lib.message.formatter.runtime.TypeFormatter;
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.lang.reflect.Parameter;
import java.util.Set;

import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult.Defined.EQUIVALENT;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult.Defined.MISMATCH;


/**
 * @author Jeroen Gremmen
 * @since 0.10.0
 */
public final class MethodParameterFormatter
    extends AbstractSingleTypeParameterFormatter<Parameter>
    implements ConfigKeyComparator<Parameter>
{
  @Override
  @Contract(pure = true)
  public @NotNull Text formatValue(@NotNull FormatterContext context, @NotNull Parameter parameter)
  {
    switch(context.getConfigValueString("parameter").orElse("default"))
    {
      case "name":
        return noSpaceText(parameter.getName());

      case "class":
        return noSpaceText(TypeFormatter.toString(parameter.getParameterizedType(), "Cju"));

      case "default":
        return noSpaceText(parameter.toString());
    }

    return context.delegateToNextFormatter();
  }


  @Override
  public @NotNull FormattableType getFormattableType() {
    return new FormattableType(Parameter.class);
  }


  @Override
  public @Unmodifiable @NotNull Set<String> getParameterConfigNames() {
    return Set.of("parameter");
  }


  @Override
  public @NotNull MatchResult compareToStringKey(@NotNull Parameter value, @NotNull ComparatorContext context)
  {
    return context
        .getCompareType()
        .match(value.getName().compareTo(context.getStringKeyValue())) ? EQUIVALENT : MISMATCH;
  }
}
