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
import de.sayayi.lib.message.part.MapKey.MatchResult;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Set;

import static de.sayayi.lib.message.part.MapKey.MatchResult.Defined.EQUIVALENT;
import static de.sayayi.lib.message.part.MapKey.MatchResult.Defined.MISMATCH;
import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;


/**
 * Parameter formatter for {@link Field} values.
 * <p>
 * This formatter renders a Java reflection field as a human-readable string. The output is controlled by the
 * {@code field} configuration key.
 * <p>
 * Map key comparison for {@code string} keys is based on the field name.
 * <p>
 * <b>Configuration keys:</b>
 * <table border="1">
 *   <caption>Configuration keys for the field formatter</caption>
 *   <tr><th>Key</th><th>Values</th><th>Default</th><th>Purpose</th></tr>
 *   <tr>
 *     <td>{@code field}</td>
 *     <td>combination of the flags {@code M}, {@code T}, {@code N}, {@code c}, {@code j} and {@code u}</td>
 *     <td>{@code juMTN}</td>
 *     <td>
 *       Flag {@code M} includes the modifiers, {@code T} the field type and {@code N} the field name. The flags
 *       {@code c}, {@code j} and {@code u} control the format of the type as described by {@link TypeFormatter}.
 *     </td>
 *   </tr>
 * </table>
 * <p>
 * <b>Examples:</b>
 * <p>
 * This formatter is not part of the {@link de.sayayi.lib.message.formatter.DefaultFormatterService
 * DefaultFormatterService} and must be registered explicitly:
 * <pre>{@code
 * var formatterService = new DefaultFormatterService();
 * formatterService.addFormatter(new FieldFormatter());
 *
 * var messageSupport = MessageSupportFactory.create(formatterService).seal();
 * }</pre>
 * <p>
 * By default, modifiers, type and name are included:
 * <pre>{@code
 * var field = Integer.class.getField("MAX_VALUE");
 *
 * messageSupport.message("%{f}").with("f", field).format();
 * // "public static final int MAX_VALUE"
 * }</pre>
 * <p>
 * Select the parts of the field and the format of the type:
 * <pre>{@code
 * var field = Locale.class.getField("GERMANY");
 *
 * messageSupport.message("%{f,field:TN}").with("f", field).format();
 * // "java.util.Locale GERMANY"
 *
 * messageSupport.message("%{f,field:cTN}").with("f", field).format();
 * // "Locale GERMANY"
 *
 * messageSupport.message("Field %{f,field:N} not accessible").with("f", field).format();
 * // "Field GERMANY not accessible"
 * }</pre>
 *
 * @author Jeroen Gremmen
 * @since 0.6.0
 */
public final class FieldFormatter
    extends AbstractSingleTypeParameterFormatter<Field>
    implements MapKeyComparator<Field>
{
  /**
   * {@inheritDoc}
   * <p>
   * Adds the {@code field} and {@link ClassifierContext#CLASSIFIER_STRING string} classifiers.
   */
  @Override
  public boolean updateClassifiers(@NotNull ClassifierContext context, @NotNull Object value)
  {
    context.addClassifier("field");
    context.addClassifier(ClassifierContext.CLASSIFIER_STRING);

    return true;
  }


  /**
   * {@inheritDoc}
   * <p>
   * Formats the field using the format flags specified by the {@code field} configuration key.
   */
  @Override
  @Contract(pure = true)
  public @NotNull Text formatValue(@NotNull ParameterFormatterContext context, @NotNull Field field)
  {
    var formattedField = new StringBuilder();
    var format = context.getConfigValueString("field").orElse("juMTN");

    // c = short class
    // j = no java.lang. prefix
    // u = no java.util. prefix
    // M = with modifiers
    // T = field type
    // N = field name

    if (format.indexOf('M') >= 0)
      formattedField.append(Modifier.toString(field.getModifiers())).append(' ');

    if (format.indexOf('T') >= 0)
      formattedField.append(TypeFormatter.toString(field.getGenericType(), format)).append(' ');

    if (format.indexOf('N') >= 0)
      formattedField.append(field.getName());

    return noSpaceText(formattedField.toString());
  }


  /**
   * {@inheritDoc}
   *
   * @return  formattable type for {@link Field}, never {@code null}
   */
  @Override
  protected @NotNull FormattableType getFormattableType() {
    return new FormattableType(Field.class);
  }


  /**
   * {@inheritDoc}
   *
   * @return  a set containing {@code "field"}, never {@code null}
   */
  @Override
  public @Unmodifiable @NotNull Set<String> getParameterConfigNames() {
    return Set.of("field");
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull MatchResult compareToStringKey(@NotNull Field value, @NotNull ComparatorContext context)
  {
    return context.getCompareType().match(value.getName().compareTo(context.getStringKeyValue()))
        ? EQUIVALENT : MISMATCH;
  }
}
