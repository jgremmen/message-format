/*
 * Copyright 2019 Jeroen Gremmen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.sayayi.lib.message.formatter.named;

import de.sayayi.lib.message.formatter.AbstractParameterFormatter;
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.formatter.NamedParameterFormatter;
import de.sayayi.lib.message.internal.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

import static java.util.Collections.emptySet;


/**
 * @author Jeroen Gremmen
 */
public final class ChoiceFormatter extends AbstractParameterFormatter implements NamedParameterFormatter
{
  @Override
  @Contract(pure = true)
  public @NotNull String getName() {
    return "choice";
  }


  @Override
  @Contract(pure = true)
  public @NotNull Text format(@NotNull FormatterContext formatterContext, Object value) {
    return formatterContext.format(formatterContext.getMapMessageOrEmpty(value, NO_NAME_KEY_TYPES, true));
  }


  @Override
  protected @NotNull Text formatValue(@NotNull FormatterContext formatterContext, Object value) {
    throw new IllegalStateException();
  }


  @Override
  public @NotNull Set<FormattableType> getFormattableTypes() {
    return emptySet();
  }
}