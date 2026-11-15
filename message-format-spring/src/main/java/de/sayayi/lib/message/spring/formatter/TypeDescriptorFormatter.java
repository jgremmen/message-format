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
import de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.TypeDescriptor;

import java.lang.reflect.Type;


/**
 * Parameter formatter for Spring {@link TypeDescriptor} values.
 * <p>
 * The type descriptor is converted to the {@link Type} it describes, including its generic type arguments, and
 * formatting is delegated to the formatter for {@link Type} values. The parameter configuration is passed on
 * unchanged, so the {@code type} configuration key of the
 * {@link de.sayayi.lib.message.formatter.parameter.runtime.TypeFormatter TypeFormatter} applies. Annotations of the
 * type descriptor are not formatted.
 * <p>
 * <b>Examples:</b>
 * <p>
 * This formatter is provided as a service and is registered automatically by the
 * {@link de.sayayi.lib.message.formatter.DefaultFormatterService DefaultFormatterService}.
 * <p>
 * Type descriptors are formatted like the type they describe, including the generic type arguments:
 * <pre>{@code
 * messageSupport.message("%{t}").with("t", TypeDescriptor.valueOf(String.class)).format();
 * // "String"
 *
 * var listOfDates = TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(LocalDate.class));
 *
 * messageSupport.message("%{t}").with("t", listOfDates).format();
 * // "List<java.time.LocalDate>"
 *
 * messageSupport.message("%{t,type:c}").with("t", listOfDates).format();
 * // "List<LocalDate>"
 * }</pre>
 *
 * @author Jeroen Gremmen
 * @since 0.25.0
 */
public final class TypeDescriptorFormatter extends AbstractSingleTypeParameterFormatter<TypeDescriptor>
{
  /**
   * {@inheritDoc}
   * <p>
   * Delegates formatting of the described type to the formatter for {@link Type} values.
   */
  @Override
  @Contract(pure = true)
  protected @NotNull Text formatValue(@NotNull ParameterFormatterContext context,
                                      @NotNull TypeDescriptor typeDescriptor) {
    return context.format(typeDescriptor.getResolvableType().getType(), Type.class);
  }


  /**
   * {@inheritDoc}
   *
   * @return  formattable type for {@link TypeDescriptor}, never {@code null}
   */
  @Override
  protected @NotNull FormattableType getFormattableType() {
    return new FormattableType(TypeDescriptor.class);
  }
}
