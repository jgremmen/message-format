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

import static de.sayayi.lib.message.formatter.parameter.ParameterFormatter.ClassifierContext.CLASSIFIER_STRING;
import static de.sayayi.lib.message.part.MapKey.MatchResult.Defined.EQUIVALENT;
import static de.sayayi.lib.message.part.MapKey.MatchResult.Defined.MISMATCH;
import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;


/**
 * Parameter formatter for {@link Method} values.
 * <p>
 * This formatter uses the {@code method} configuration key to select the output format.
 * <p>
 * If the configuration value does not match a known option, formatting is delegated to the next available formatter.
 * <p>
 * Map key comparison for {@code string} keys is based on the method name.
 * <p>
 * <b>Configuration keys:</b>
 * <table border="1">
 *   <caption>Configuration keys for the method formatter</caption>
 *   <tr><th>Key</th><th>Values</th><th>Default</th><th>Purpose</th></tr>
 *   <tr>
 *     <td>{@code method}</td>
 *     <td>{@code default}, {@code name}, {@code class}, {@code return-type}</td>
 *     <td>{@code default}</td>
 *     <td>
 *       Formats the default string representation of the method, its name, its declaring class or its return
 *       type. Any other value delegates to the next formatter.
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
 * formatterService.addFormatter(new MethodFormatter());
 *
 * var messageSupport = MessageSupportFactory.create(formatterService).seal();
 * }</pre>
 * <p>
 * Format individual aspects of a method:
 * <pre>{@code
 * var method = String.class.getMethod("substring", int.class);
 *
 * messageSupport.message("%{m,method:class}.%{m,method:name}()").with("m", method).format();
 * // "String.substring()"
 *
 * messageSupport.message("returns %{m,method:'return-type'}").with("m", method).format();
 * // "returns String"
 * }</pre>
 * <p>
 * The default string representation of a method includes its signature:
 * <pre>{@code
 * var method = String.class.getMethod("length");
 *
 * messageSupport.message("%{m}").with("m", method).format();
 * // "public int java.lang.String.length()"
 * }</pre>
 * <p>
 * String keys are compared with the method name if selected by a formatter like {@code choice}:
 * <pre>{@code
 * var message = "%{m,format:choice,'length':'size query',:'other'}";
 *
 * messageSupport.message(message).with("m", String.class.getMethod("length")).format();
 * // "size query"
 *
 * messageSupport.message(message).with("m", String.class.getMethod("trim")).format();
 * // "other"
 * }</pre>
 *
 * @author Jeroen Gremmen
 * @since 0.5.0
 */
public final class MethodFormatter
    extends AbstractSingleTypeParameterFormatter<Method>
    implements MapKeyComparator<Method>
{
  /**
   * {@inheritDoc}
   * <p>
   * Adds the {@code method} classifier. If the {@code method} configuration key refers to a known option, the
   * {@link ClassifierContext#CLASSIFIER_STRING string} classifier is added as well.
   */
  @Override
  protected boolean updateTypedClassifiers(@NotNull ClassifierContext context, @NotNull Method value)
  {
    context.addClassifier("method");

    switch(context.getConfigValueString("method").orElse("default"))
    {
      case "default", "name", "class", "return-type" -> {
        context.addClassifier(CLASSIFIER_STRING);
        return true;
      }

      default -> {
        return false;
      }
    }
  }


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
