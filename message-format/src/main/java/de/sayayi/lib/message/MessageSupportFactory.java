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
package de.sayayi.lib.message;

import de.sayayi.lib.message.MessageSupport.ConfigurableMessageSupport;
import de.sayayi.lib.message.formatter.DefaultFormatterService;
import de.sayayi.lib.message.formatter.FormatterService;
import de.sayayi.lib.message.internal.MessageSupportImpl;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;


/**
 * Factory class for creating {@link MessageSupport} instances.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class MessageSupportFactory
{
  private static final Object $LOCK = new Object[0];
  private static MessageSupport SHARED = null;


  private MessageSupportFactory() {
  }


  /**
   * Returns a shared instance of the message support.
   * <p>
   * The shared message support is backed by the shared instance of the default formatter service
   * ({@link DefaultFormatterService#getSharedInstance()}). This means that changes (e.g. adding
   * new formatters) to the formatting service will reflect in formatting operations of the
   * shared message support.
   *
   * @return  shared message support instance, never {@code null}
   */
  public static @NotNull MessageSupport shared()
  {
    synchronized($LOCK) {
      if (SHARED == null)
        SHARED = create(DefaultFormatterService.getSharedInstance(), NO_CACHE_INSTANCE).seal();

      return SHARED;
    }
  }


  /**
   * Create a new {@link MessageSupport} instance with the given {@code formatterService} and
   * {@code messageFactory}.
   * <p>
   * The returned instance is the configurable version of the message support, which allows for
   * further configuration such as adding messages and templates.
   *
   * @param formatterService  formatter service, not {@code null}
   * @param messageFactory    message factory, not {@code null}
   *
   * @return  new configurable message support instance, never {@code null}
   */
  @Contract(value = "_, _ -> new")
  public static @NotNull ConfigurableMessageSupport create(@NotNull FormatterService formatterService,
                                                           @NotNull MessageFactory messageFactory) {
    return new MessageSupportImpl(formatterService, messageFactory);
  }
}
