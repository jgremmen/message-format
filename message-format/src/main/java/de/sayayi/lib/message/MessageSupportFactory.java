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

import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Predicate;

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
   * ({@link DefaultFormatterService#getSharedInstance()}). This means that changes (eg. adding
   * new formatters) to the formatting service will reflect in formatting operations of the
   * shared message support.
   *
   * @return  shared message support instance, never {@code null}
   */
  public static @NotNull MessageSupport shared()
  {
    synchronized($LOCK) {
      if (SHARED == null)
        SHARED = seal(create(DefaultFormatterService.getSharedInstance(), NO_CACHE_INSTANCE));

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


  /**
   * Seals off a message support instance by asserting that the message support does not implement
   * {@link ConfigurableMessageSupport} and thus is not modifiable using the returned instance.
   * <p>
   * If the given {@code messageSupport} does not implement {@link ConfigurableMessageSupport} then it is returned
   * unmodified. Otherwise, a {@link MessageSupport} wrapper is returned which is backed by the given
   * {@code messageSupport}, so changes to the configurable message support always reflect in the returned instance.
   *
   * @param messageSupport  message support to seal, not {@code null}
   * @return  sealed message support, never {@code null}
   *
   * @since 0.11.1
   */
  @Contract(pure = true)
  public static @NotNull MessageSupport seal(@NotNull MessageSupport messageSupport)
  {
    return messageSupport instanceof ConfigurableMessageSupport
        ? new MessageSupportDelegate(messageSupport)
        : messageSupport;
  }




  /**
   * Message support delegator
   *
   * @since 0.11.1
   */
  private static final class MessageSupportDelegate implements MessageSupport
  {
    private final MessageSupport delegate;


    private MessageSupportDelegate(@NotNull MessageSupport delegate) {
      this.delegate = delegate;
    }


    @Override
    public @NotNull MessageAccessor getMessageAccessor() {
      return delegate.getMessageAccessor();
    }


    @Override
    public @NotNull MessageConfigurer<Message.WithCode> code(@NotNull String code) {
      return delegate.code(code);
    }


    @Override
    public @NotNull MessageConfigurer<Message> message(@NotNull String message) {
      return delegate.message(message);
    }


    @Override
    public @NotNull <M extends Message> MessageConfigurer<M> message(@NotNull M message) {
      return delegate.message(message);
    }


    @Override
    public void exportMessages(@NotNull OutputStream stream, boolean compress, Predicate<String> messageCodeFilter)
        throws IOException {
      delegate.exportMessages(stream, compress, messageCodeFilter);
    }
  }
}
