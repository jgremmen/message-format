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
package de.sayayi.lib.message.formatter.parameter.named;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.formatter.parameter.NamedParameterFormatter;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import static de.sayayi.lib.message.part.MapKey.STRING_TYPE;
import static de.sayayi.lib.message.part.MapKey.Type.STRING;


/**
 * Named parameter formatter that selects a message from a map based on the classifiers of the parameter value.
 * <p>
 * This formatter is selected by using the name {@code classifier} in a message parameter, e.g.
 * {@code %{p,format:classifier,'number':'a number','string':'text','list':'a list',:'something else'}}.
 * <p>
 * Classifiers are type-based labels (such as {@code null}, {@code bool}, {@code number}, {@code string}, {@code list},
 * etc.) that describe the nature of a parameter value. This formatter iterates over the classifiers of the given value
 * and returns the message associated with the first matching string map key. If no classifier matches, the default map
 * entry is used. If no default is present either, the {@code null} representation is returned.
 *
 * @author Jeroen Gremmen
 * @since 0.21.0
 */
public final class ClassifierFormatter implements NamedParameterFormatter
{
  /**
   * {@inheritDoc}
   *
   * @return  {@code "classifier"}, never {@code null}
   */
  @Override
  @Contract(pure = true)
  public @NotNull String getName() {
    return "classifier";
  }


  /**
   * {@inheritDoc}
   * <p>
   * Iterates over the classifiers of the given {@code value} and returns the formatted message associated with the
   * first matching string map key. If no classifier matches, the default map entry is formatted and returned. If no
   * default is present, the {@code null} representation is returned.
   */
  @Override
  @Contract(pure = true)
  public @NotNull Text format(@NotNull ParameterFormatterContext context, Object value)
  {
    Optional<Message.WithSpaces> mappedMessage;

    for(var classifier: context.getClassifiers(value, null))
      if ((mappedMessage = context.getMapMessage(classifier, STRING_TYPE)).isPresent())
        return context.format(mappedMessage.get());

    return context
        .getMap()
        .getDefaultMessage(context.getMessageAccessor(), STRING)
        .map(context::format)
        .orElseGet(() -> formatNull(context));
  }
}
