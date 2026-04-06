/*
 * Copyright 2021 Jeroen Gremmen
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
package de.sayayi.lib.message.formatter.parameter.runtime.extra;

import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.parameter.AbstractSingleTypeParameterFormatter;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatter.MapKeyComparator;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext;
import de.sayayi.lib.message.formatter.parameter.runtime.TypeFormatter;
import de.sayayi.lib.message.part.MapKey;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.lang.reflect.Method;
import java.util.Set;

import static de.sayayi.lib.message.part.MapKey.MatchResult.Defined.EQUIVALENT;
import static de.sayayi.lib.message.part.MapKey.MatchResult.Defined.MISMATCH;
import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;


/**
 * Parameter formatter for {@link Method} values.
 * <p>
 * This formatter uses the {@code method} configuration key to select the output format:
 * <ul>
 *   <li>{@code default} (default) &ndash; the method's default string representation</li>
 *   <li>{@code name} &ndash; the method name</li>
 *   <li>{@code class} &ndash; the declaring class</li>
 *   <li>{@code return-type} &ndash; the method's return type</li>
 * </ul>
 * <p>
 * If the configuration value does not match a known option, formatting is delegated to the next available formatter.
 * <p>
 * Map key comparison for {@code string} keys is based on the method name.
 *
 * @author Jeroen Gremmen
 */
public final class MethodFormatter
    extends AbstractSingleTypeParameterFormatter<Method>
    implements MapKeyComparator<Method>
{
  /**
   * {@inheritDoc}
   * <p>
   * Formats the method based on the {@code method} configuration key.
   */
  @Override
  @Contract(pure = true)
  public @NotNull Text formatValue(@NotNull ParameterFormatterContext context, @NotNull Method method)
  {
    return switch(context.getConfigValueString("method").orElse("default")) {
      case "name" -> noSpaceText(method.getName());
      case "class" -> noSpaceText(TypeFormatter.toString(method.getDeclaringClass(), "Cju"));
      case "return-type" -> noSpaceText(TypeFormatter.toString(method.getGenericReturnType(), "Cju"));
      case "default" -> noSpaceText(method.toString());

      default -> context.delegateToNextFormatter();
    };
  }


  /**
   * {@inheritDoc}
   *
   * @return  formattable type for {@link Method}, never {@code null}
   */
  @Override
  protected @NotNull FormattableType getFormattableType() {
    return new FormattableType(Method.class);
  }


  /**
   * {@inheritDoc}
   *
   * @return  a set containing {@code "method"}, never {@code null}
   */
  @Override
  public @Unmodifiable @NotNull Set<String> getParameterConfigNames() {
    return Set.of("method");
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull MapKey.MatchResult compareToStringKey(@NotNull Method value, @NotNull ComparatorContext context)
  {
    return context.getCompareType().match(value.getName().compareTo(context.getStringKeyValue()))
        ? EQUIVALENT : MISMATCH;
  }
}
