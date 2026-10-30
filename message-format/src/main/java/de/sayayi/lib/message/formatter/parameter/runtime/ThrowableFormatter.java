/*
 * Copyright 2020 Jeroen Gremmen
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

import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.parameter.AbstractSingleTypeParameterFormatter;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.NotNull;

import static de.sayayi.lib.message.formatter.parameter.ParameterFormatter.ClassifierContext.CLASSIFIER_STRING;


/**
 * Parameter formatter for {@link Throwable} values.
 * <p>
 * This formatter extracts the throwable's localized message using {@link Throwable#getLocalizedMessage()} and
 * delegates the formatting to the string formatter.
 * <p>
 * <b>Examples:</b>
 * <p>
 * Include the message of an exception. Map keys of the string formatter are applied to the message:
 * <pre>{@code
 * var exception = new IOException("Connection refused");
 *
 * messageSupport.message("Upload failed: %{ex}").with("ex", exception).format();
 * // "Upload failed: Connection refused"
 *
 * messageSupport
 *     .message("%{ex,'Connection refused':'The server is not reachable'}")
 *     .with("ex", exception)
 *     .format();
 * // "The server is not reachable"
 * }</pre>
 * <p>
 * Handle an exception without a message:
 * <pre>{@code
 * messageSupport
 *     .message("Upload failed: %{ex,null:'unknown reason'}")
 *     .with("ex", new IOException())
 *     .format();
 * // "Upload failed: unknown reason"
 * }</pre>
 *
 * @author Jeroen Gremmen
 * @since 0.5.0
 */
public final class ThrowableFormatter extends AbstractSingleTypeParameterFormatter<Throwable>
{
  /**
   * {@inheritDoc}
   * <p>
   * Adds the {@code throwable} and {@link ClassifierContext#CLASSIFIER_STRING string} classifiers.
   */
  @Override
  public boolean updateClassifiers(@NotNull ClassifierContext context, @NotNull Object value)
  {
    context.addClassifier("throwable");
    context.addClassifier(CLASSIFIER_STRING);

    return true;
  }


  /**
   * {@inheritDoc}
   * <p>
   * Formats the throwable's localized message as a string.
   */
  @Override
  protected @NotNull Text formatValue(@NotNull ParameterFormatterContext context, @NotNull Throwable throwable) {
    return context.format(throwable.getLocalizedMessage(), String.class);
  }


  /**
   * {@inheritDoc}
   *
   * @return  formattable type for {@link Throwable}, never {@code null}
   */
  @Override
  protected @NotNull FormattableType getFormattableType() {
    return new FormattableType(Throwable.class);
  }
}
