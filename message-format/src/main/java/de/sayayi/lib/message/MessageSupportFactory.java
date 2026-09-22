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
import de.sayayi.lib.message.formatter.GenericFormatterService;
import de.sayayi.lib.message.internal.MessageSupportImpl;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * Factory for creating {@link MessageSupport} instances, which are the central entry point for registering,
 * accessing and formatting messages and templates.
 * <p>
 * A message support combines a {@link FormatterService}, which supplies the parameter formatters used during message
 * formatting, with a {@link MessageFactory}, which parses message format strings. This factory offers two ways of
 * obtaining an instance:
 * <ul>
 *   <li>
 *     The {@link #shared()} method returns a lazily initialized, sealed singleton that is ready to use out of the box.
 *     It is backed by the {@linkplain DefaultFormatterService#getSharedInstance() shared default formatter service}
 *     and automatically registers all named templates found on the classpath. Use it for simple use cases where no
 *     custom configuration is required.
 *   </li>
 *   <li>
 *     The various {@code create} methods return a new {@link ConfigurableMessageSupport} that can still be freely
 *     configured with custom formatters, messages and templates before being
 *     {@linkplain ConfigurableMessageSupport#seal() sealed} for use.
 *   </li>
 * </ul>
 * <p>
 * The configurable {@code create} methods differ only in the formatter service they start from:
 * {@link #create(FormatterService, MessageFactory)} and {@link #create(FormatterService)} let you supply your own
 * service, {@link #createGeneric()} starts from an empty {@link GenericFormatterService} for full control over the
 * registered formatters, and {@link #createDefault()} starts from the fully populated default formatter service.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class MessageSupportFactory
{
  private static final Lock $LOCK = new ReentrantLock();
  private static volatile MessageSupport SHARED = null;


  /** This class is not meant to be instantiated. */
  private MessageSupportFactory() {
  }


  /**
   * Returns a shared instance of the message support.
   * <p>
   * The shared message support is backed by the shared instance of the default formatter service
   * ({@link DefaultFormatterService#getSharedInstance()}). This means that changes (e.g. adding new formatters) to
   * the formatting service will reflect in formatting operations of the shared message support.
   * <p>
   * The shared instance automatically discovers and registers all
   * {@link de.sayayi.lib.message.template.NamedTemplate NamedTemplate} service providers on the classpath,
   * and is then {@linkplain ConfigurableMessageSupport#seal() sealed} so it cannot be modified further.
   *
   * @return  shared message support instance, never {@code null}
   */
  public static @NotNull MessageSupport shared()
  {
    var shared = SHARED;
    if (shared == null)
    {
      $LOCK.lock();
      try {
        if ((shared = SHARED) == null)
        {
          SHARED = shared = createDefault()
              .registerTemplatesFromService(MessageSupportFactory.class.getClassLoader())
              .seal();
        }
      } finally {
        $LOCK.unlock();
      }
    }

    return shared;
  }


  /**
   * Create a new {@link MessageSupport} instance with the given {@code formatterService} and {@code messageFactory}.
   * <p>
   * The returned instance is the configurable version of the message support, which allows for further configuration
   * such as adding messages and templates.
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
   * Create a new {@link MessageSupport} instance with the given {@code formatterService} and a non-caching
   * {@link MessageFactory}.
   * <p>
   * This is a convenience method equivalent to calling
   * {@link #create(FormatterService, MessageFactory) create(formatterService, MessageFactory.getSharedInstance())}.
   *
   * @param formatterService  formatter service, not {@code null}
   *
   * @return  new configurable message support instance, never {@code null}
   *
   * @since 0.22.0
   */
  @Contract(value = "_ -> new")
  public static @NotNull ConfigurableMessageSupport create(@NotNull FormatterService formatterService) {
    return create(formatterService, MessageFactory.getSharedInstance());
  }


  /**
   * Create a new configurable {@link MessageSupport} instance backed by an empty {@link GenericFormatterService} and
   * a non-caching {@link MessageFactory}.
   * <p>
   * Use this method when you want full control over which formatters are registered, starting from a service that only
   * provides the default string fallback formatter.
   *
   * @return  new configurable message support instance, never {@code null}
   *
   * @since 0.25.0
   */
  @Contract(value = "-> new")
  public static @NotNull ConfigurableMessageSupport createGeneric() {
    return create(new GenericFormatterService(), MessageFactory.getSharedInstance());
  }


  /**
   * Create a new configurable {@link MessageSupport} instance backed by the
   * {@linkplain DefaultFormatterService#getSharedInstance() shared default formatter service} and a non-caching
   * {@link MessageFactory}.
   * <p>
   * The default formatter service provides the full set of built-in formatters, making this a convenient starting
   * point for most use cases that still require additional configuration such as adding messages and templates.
   *
   * @return  new configurable message support instance, never {@code null}
   *
   * @since 0.25.0
   */
  @Contract(value = "-> new")
  public static @NotNull ConfigurableMessageSupport createDefault() {
    return create(DefaultFormatterService.getSharedInstance(), MessageFactory.getSharedInstance());
  }
}
