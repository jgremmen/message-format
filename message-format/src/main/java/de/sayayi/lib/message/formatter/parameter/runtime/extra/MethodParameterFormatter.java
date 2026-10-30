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

import java.lang.reflect.Parameter;
import java.util.Set;

import static de.sayayi.lib.message.formatter.parameter.ParameterFormatter.ClassifierContext.CLASSIFIER_STRING;
import static de.sayayi.lib.message.part.MapKey.MatchResult.Defined.EQUIVALENT;
import static de.sayayi.lib.message.part.MapKey.MatchResult.Defined.MISMATCH;
import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;


/**
 * Parameter formatter for method {@link Parameter} values.
 * <p>
 * This formatter uses the {@code parameter} configuration key to select the output format.
 * <p>
 * If the configuration value does not match a known option, formatting is delegated to the next available formatter.
 * <p>
 * Map key comparison for {@code string} keys is based on the parameter name.
 * <p>
 * <b>Configuration keys:</b>
 * <table border="1">
 *   <caption>Configuration keys for the method parameter formatter</caption>
 *   <tr><th>Key</th><th>Values</th><th>Default</th><th>Purpose</th></tr>
 *   <tr>
 *     <td>{@code parameter}</td>
 *     <td>{@code default}, {@code name}, {@code class}</td>
 *     <td>{@code default}</td>
 *     <td>
 *       Formats the default string representation of the parameter, its name or its type. Any other value
 *       delegates to the next formatter.
 *     </td>
 *   </tr>
 * </table>
 * <p>
 * Real parameter names are only available for classes compiled with the {@code -parameters} option.
 * <p>
 * <b>Examples:</b>
 * <p>
 * This formatter is not part of the {@link de.sayayi.lib.message.formatter.DefaultFormatterService
 * DefaultFormatterService} and must be registered explicitly:
 * <pre>{@code
 * var formatterService = new DefaultFormatterService();
 * formatterService.addFormatter(new MethodParameterFormatter());
 *
 * var messageSupport = MessageSupportFactory.create(formatterService).seal();
 * }</pre>
 * <p>
 * Describe a parameter of a method:
 * <pre>{@code
 * var parameter = List.class.getMethod("get", int.class).getParameters()[0];
 *
 * messageSupport.message("%{p}").with("p", parameter).format();
 * // "int arg0"
 *
 * messageSupport.message("Type: %{p,parameter:class}").with("p", parameter).format();
 * // "Type: int"
 * }</pre>
 * <p>
 * Generic types are supported, with the prefixes of the most common packages being omitted:
 * <pre>{@code
 * var parameter = List.class.getMethod("addAll", Collection.class).getParameters()[0];
 *
 * messageSupport.message("%{p,parameter:class}").with("p", parameter).format();
 * // "Collection<? extends E>"
 * }</pre>
 * <p>
 * List all parameters of a method:
 * <pre>{@code
 * var method = String.class.getMethod("replace", char.class, char.class);
 *
 * messageSupport
 *     .message("%{m,method:name}(%{params,list-value:'%{value,parameter:class}'})")
 *     .with("m", method)
 *     .with("params", method.getParameters())
 *     .format();
 * // "replace(char, char)"
 * }</pre>
 *
 * @author Jeroen Gremmen
 * @since 0.10.0
 */
public final class MethodParameterFormatter
    extends AbstractSingleTypeParameterFormatter<Parameter>
    implements MapKeyComparator<Parameter>
{
  /**
   * {@inheritDoc}
   * <p>
   * Adds the {@code method-parameter} classifier. If the {@code parameter} configuration key refers to a known
   * option, the {@link ClassifierContext#CLASSIFIER_STRING string} classifier is added as well.
   */
  @Override
  protected boolean updateTypedClassifiers(@NotNull ClassifierContext context, @NotNull Parameter value)
  {
    context.addClassifier("method-parameter");

    switch(context.getConfigValueString("parameter").orElse("default"))
    {
      case "name", "class", "default" -> {
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
   * Formats the method parameter based on the {@code parameter} configuration key.
   */
  @Override
  @Contract(pure = true)
  public @NotNull Text formatValue(@NotNull ParameterFormatterContext context, @NotNull Parameter parameter)
  {
    return switch(context.getConfigValueString("parameter").orElse("default")) {
      case "name" -> noSpaceText(parameter.getName());
      case "class" -> noSpaceText(TypeFormatter.toString(parameter.getParameterizedType(), "Cju"));
      case "default" -> noSpaceText(parameter.toString());

      default -> context.delegateToNextFormatter();
    };
  }


  /**
   * {@inheritDoc}
   *
   * @return  formattable type for {@link Parameter}, never {@code null}
   */
  @Override
  protected @NotNull FormattableType getFormattableType() {
    return new FormattableType(Parameter.class);
  }


  /**
   * {@inheritDoc}
   *
   * @return  a set containing {@code "parameter"}, never {@code null}
   */
  @Override
  public @Unmodifiable @NotNull Set<String> getParameterConfigNames() {
    return Set.of("parameter");
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull MatchResult compareToStringKey(@NotNull Parameter value, @NotNull ComparatorContext context)
  {
    return context
        .getCompareType()
        .match(value.getName().compareTo(context.getStringKeyValue())) ? EQUIVALENT : MISMATCH;
  }
}
