/*
 * Copyright 2026 Jeroen Gremmen
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
package de.sayayi.lib.message.spring.formatter;

import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.parameter.AbstractSingleTypeParameterFormatter;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatter.MapKeyComparator;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext;
import de.sayayi.lib.message.part.MapKey.MatchResult;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Type;

import static de.sayayi.lib.message.part.MapKey.MatchResult.forEmptyKey;
import static de.sayayi.lib.message.part.TextPartFactory.emptyText;
import static org.springframework.core.ResolvableType.NONE;


/**
 * Parameter formatter for Spring {@link ResolvableType} values.
 * <p>
 * Formatting is delegated to the formatter for {@link Type} values, using the {@linkplain ResolvableType#getType()
 * underlying type} of the resolvable type. The parameter configuration is passed on unchanged, so the {@code type}
 * configuration key of the {@link de.sayayi.lib.message.formatter.parameter.runtime.TypeFormatter TypeFormatter}
 * applies.
 * <p>
 * The underlying type is formatted as it is declared. Type variables are not replaced by the types they resolve to in
 * the context of the resolvable type; a field of type {@code T} is formatted as {@code T}, even if the resolvable type
 * was created for a subclass that binds {@code T} to a concrete type.
 * <p>
 * {@link ResolvableType#NONE} has no underlying type. It is treated as an empty value: it is matched by the
 * {@code empty} key and formatted as empty text.
 * <p>
 * <b>Examples:</b>
 * <p>
 * This formatter is provided as a service and is registered automatically by the
 * {@link de.sayayi.lib.message.formatter.DefaultFormatterService DefaultFormatterService}.
 * <p>
 * Resolvable types are formatted like their underlying type, including the generic type arguments:
 * <pre>{@code
 * var mapType = ResolvableType.forClassWithGenerics(Map.class, String.class, LocalDate.class);
 *
 * messageSupport.message("%{t}").with("t", mapType).format();
 * // "Map<String, java.time.LocalDate>"
 *
 * messageSupport.message("%{t,type:c}").with("t", mapType).format();
 * // "Map<String, LocalDate>"
 * }</pre>
 * <p>
 * {@link ResolvableType#NONE} is matched by the {@code empty} key:
 * <pre>{@code
 * messageSupport.message("%{t,empty:'unknown'}").with("t", ResolvableType.NONE).format();
 * // "unknown"
 * }</pre>
 *
 * @author Jeroen Gremmen
 * @since 0.25.0
 */
public final class ResolvableTypeFormatter
    extends AbstractSingleTypeParameterFormatter<ResolvableType>
    implements MapKeyComparator<ResolvableType>
{
  /**
   * {@inheritDoc}
   * <p>
   * Delegates formatting of the underlying type to the formatter for {@link Type} values. Returns empty text for
   * {@link ResolvableType#NONE}.
   */
  @Override
  @Contract(pure = true)
  protected @NotNull Text formatValue(@NotNull ParameterFormatterContext context,
                                      @NotNull ResolvableType resolvableType)
  {
    return resolvableType == NONE
        ? emptyText()
        : context.format(resolvableType.getType(), Type.class);
  }


  /**
   * {@inheritDoc}
   *
   * @return  formattable type for {@link ResolvableType}, never {@code null}
   */
  @Override
  protected @NotNull FormattableType getFormattableType() {
    return new FormattableType(ResolvableType.class);
  }


  /**
   * {@inheritDoc}
   * <p>
   * Both {@code null} and {@link ResolvableType#NONE} are considered empty.
   */
  @Override
  public @NotNull MatchResult compareToEmptyKey(ResolvableType resolvableType, @NotNull ComparatorContext context) {
    return forEmptyKey(context.getCompareType(), resolvableType == null || resolvableType == NONE);
  }
}
