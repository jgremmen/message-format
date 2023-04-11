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
package de.sayayi.lib.message.parser.normalizer;

import de.sayayi.lib.message.internal.part.MessagePart;
import org.jetbrains.annotations.NotNull;


/**
 * @author Jeroen Gremmen
 */
public interface MessagePartNormalizer
{
  /**
   * Normalize the given message part. The returned part may be replaced with an identical
   * cached version in order to reduce memory load.
   *
   * @param <T>   message part implementation type
   * @param part  message part
   *
   * @return  message part
   */
  <T extends MessagePart> @NotNull T normalize(@NotNull T part);
}
