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
package de.sayayi.lib.message.formatter.parameter.runtime;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.parameter.AbstractSingleTypeParameterFormatter;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;
import java.util.Optional;

import static de.sayayi.lib.message.formatter.parameter.ParameterFormatter.ClassifierContext.CLASSIFIER_STRING;
import static de.sayayi.lib.message.part.MapKey.STRING_TYPE;
import static de.sayayi.lib.message.part.MapKey.Type.STRING;
import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;


/**
 * Parameter formatter for {@link Charset} values.
 * <p>
 * The charset's canonical name and aliases are matched against string map keys in the parameter
 * configuration. If no match is found, the default map entry is used. If no default is provided,
 * the charset's locale-sensitive display name is used as the formatted result.
 *
 * @author Jeroen Gremmen
 * @since 0.22.1
 */
public final class CharsetFormatter extends AbstractSingleTypeParameterFormatter<Charset>
{
  /**
   * {@inheritDoc}
   * <p>
   * Adds the {@code charset} and {@link ClassifierContext#CLASSIFIER_STRING string} classifiers.
   */
  @Override
  public boolean updateClassifiers(@NotNull ClassifierContext context, @NotNull Object value)
  {
    context.addClassifier("charset");
    context.addClassifier(CLASSIFIER_STRING);

    return true;
  }


  /**
   * {@inheritDoc}
   * <p>
   * Formats the given charset by first attempting to match its canonical name or aliases against
   * string map keys. If no match is found, the default map entry is consulted. If no default is
   * available either, the charset's locale-sensitive display name is returned.
   */
  @Override
  protected @NotNull Text formatValue(@NotNull ParameterFormatterContext context, @NotNull Charset charset)
  {
    Optional<Message.WithSpaces> mappedMessage;

    findMapped: {
      final var name = charset.name();

      if ((mappedMessage = context.getMapMessage(name, STRING_TYPE)).isPresent())
        break findMapped;

      for(var alias: charset.aliases())
        if (!name.equals(alias) && (mappedMessage = context.getMapMessage(alias, STRING_TYPE)).isPresent())
          break findMapped;

      mappedMessage = context.getMap().getDefaultMessage(context.getMessageAccessor(), STRING);
    }

    return mappedMessage
        .map(context::format)
        .orElseGet(() -> noSpaceText(charset.displayName(context.getLocale())));
  }


  /**
   * {@inheritDoc}
   *
   * @return  the {@link Charset} formattable type, never {@code null}
   */
  @Override
  protected @NotNull FormattableType getFormattableType() {
    return new FormattableType(Charset.class);
  }
}
