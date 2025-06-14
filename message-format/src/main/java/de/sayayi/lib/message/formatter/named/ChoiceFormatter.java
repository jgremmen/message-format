/*
 * Copyright 2019 Jeroen Gremmen
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
package de.sayayi.lib.message.formatter.named;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.formatter.NamedParameterFormatter;
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.part.parameter.key.ConfigKey;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.EnumSet;
import java.util.Set;

import static de.sayayi.lib.message.part.parameter.key.ConfigKey.Type.*;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class ChoiceFormatter implements NamedParameterFormatter
{
  private static final Set<ConfigKey.Type> KEY_TYPES = EnumSet.of(NULL, EMPTY, BOOL, NUMBER, STRING);


  @Override
  @Contract(pure = true)
  public @NotNull String getName() {
    return "choice";
  }


  @Override
  @Contract(pure = true)
  public @NotNull Text format(@NotNull FormatterContext context, Object value)
  {
    return context.format(context
        .getConfigMapMessage(value, KEY_TYPES, true)
        .orElse(Message.WithSpaces.EMPTY));
  }


  @Override
  public @Unmodifiable @NotNull Set<String> getParameterConfigNames() {
    return Set.of("choice");
  }
}
