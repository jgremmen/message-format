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
import lombok.Synchronized;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;


/**
 * Factory class for creating {@link MessageSupport} instances.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class MessageSupportFactory
{
  private static MessageSupport SHARED = null;


  private MessageSupportFactory() {
  }


  /**
   * Returns an instance of the shared message support.
   * <p>
   * The shared message support cannot be configured and contains all formatters which can be found
   * as services by the default classloader.
   *
   * @return  shared message support instance, never {@code null}
   */
  @Synchronized
  public static @NotNull MessageSupport shared()
  {
    if (SHARED == null)
      SHARED = new SharedMessageSupport();

    return SHARED;
  }


  /**
   * <p>
   *   Create a new {@link MessageSupport} instance with the given {@code formatterService} and
   *   {@code messageFactory}.
   * </p>
   * <p>
   *   The returned instance is the configurable version of the message support, which allows for
   *   further configuration such as adding messages and templates.
   * </p>
   *
   * @param formatterService  formatter service, not {@code null}
   * @param messageFactory    message factory, not {@code null}
   *
   * @return  new message support instance, never {@code null}
   */
  @Contract(value = "_, _ -> new")
  public static @NotNull ConfigurableMessageSupport create(
      @NotNull FormatterService formatterService,
      @NotNull MessageFactory messageFactory) {
    return new MessageSupportImpl(formatterService, messageFactory);
  }




  private static final class SharedMessageSupport extends MessageSupportImpl
  {
    private SharedMessageSupport() {
      super(DefaultFormatterService.getSharedInstance(), NO_CACHE_INSTANCE);
    }


    @Override
    public @NotNull ConfigurableMessageSupport setLocale(@NotNull Locale locale) {
      throw new UnsupportedOperationException("shared message support");
    }


    @Override
    public @NotNull ConfigurableMessageSupport setDefaultParameterConfig(@NotNull String name,
                                                                         boolean value) {
      throw new UnsupportedOperationException("shared message support");
    }


    @Override
    public @NotNull ConfigurableMessageSupport setDefaultParameterConfig(@NotNull String name,
                                                                         long value) {
      throw new UnsupportedOperationException("shared message support");
    }


    @Override
    public @NotNull ConfigurableMessageSupport setDefaultParameterConfig(@NotNull String name,
                                                                         @NotNull String value) {
      throw new UnsupportedOperationException("shared message support");
    }


    @Override
    public @NotNull ConfigurableMessageSupport setDefaultParameterConfig(
        @NotNull String name,
        @NotNull Message.WithSpaces value) {
      throw new UnsupportedOperationException("shared message support");
    }


    @Override
    public void addMessage(@NotNull Message.WithCode message) {
      throw new UnsupportedOperationException("shared message support");
    }


    @Override
    public void addTemplate(@NotNull String name, @NotNull Message template) {
      throw new UnsupportedOperationException("shared message support");
    }
  }
}
