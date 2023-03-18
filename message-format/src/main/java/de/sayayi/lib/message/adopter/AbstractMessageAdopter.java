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
package de.sayayi.lib.message.adopter;

import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.MessageSupport.ConfigurableMessageSupport;
import de.sayayi.lib.message.MessageSupport.MessagePublisher;
import org.jetbrains.annotations.NotNull;


/**
 * <p>
 *   A message adopter takes message information from a particular source and publishes it to a
 *   message publisher.
 * </p>
 * <p>
 *   This class provides the minimum requirements which are shared by all message adopters.
 * </p>
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public abstract class AbstractMessageAdopter
{
  protected final MessageFactory messageFactory;
  protected final MessagePublisher messagePublisher;


  protected AbstractMessageAdopter(@NotNull ConfigurableMessageSupport configurableMessageSupport) {
    this(configurableMessageSupport.getAccessor().getMessageFactory(), configurableMessageSupport);
  }


  protected AbstractMessageAdopter(@NotNull MessageFactory messageFactory,
                                   @NotNull MessagePublisher publisher)
  {
    this.messageFactory = messageFactory;
    this.messagePublisher = publisher;
  }
}
