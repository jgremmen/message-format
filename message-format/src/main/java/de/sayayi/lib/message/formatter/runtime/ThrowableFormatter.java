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
package de.sayayi.lib.message.formatter.runtime;

import de.sayayi.lib.message.formatter.AbstractSingleTypeParameterFormatter;
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.NotNull;


/**
 * @author Jeroen Gremmen
 */
public final class ThrowableFormatter extends AbstractSingleTypeParameterFormatter<Throwable>
{
  @Override
  protected @NotNull Text formatValue(@NotNull FormatterContext context, @NotNull Throwable throwable) {
    return context.format(throwable.getLocalizedMessage(), String.class, true);
  }


  @Override
  public @NotNull FormattableType getFormattableType() {
    return new FormattableType(Throwable.class);
  }
}
