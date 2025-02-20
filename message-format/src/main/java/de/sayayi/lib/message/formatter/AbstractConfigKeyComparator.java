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
package de.sayayi.lib.message.formatter;

import de.sayayi.lib.message.formatter.ParameterFormatter.ConfigKeyComparator;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.NotNull;


/**
 * @param <T>  parameter type
 *
 * @author Jeroen Gremmen
 * @since 0.9.2
 */
public abstract class AbstractConfigKeyComparator<T> implements ConfigKeyComparator<T>
{
  @Override
  public final @NotNull Text format(@NotNull FormatterContext context, Object value) {
    return context.delegateToNextFormatter();
  }
}
