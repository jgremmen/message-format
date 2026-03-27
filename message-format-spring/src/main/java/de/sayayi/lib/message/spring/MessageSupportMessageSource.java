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
package de.sayayi.lib.message.spring;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.MessageSupport;
import de.sayayi.lib.message.MessageSupport.MessageConfigurer;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.HierarchicalMessageSource;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;

import java.util.Locale;

import static java.lang.Character.isLetter;
import static org.springframework.util.ObjectUtils.isEmpty;


/**
 * Spring {@link MessageSource} implementation that delegates message resolution and formatting to a
 * {@link MessageSupport} instance.
 * <p>
 * This class implements {@link HierarchicalMessageSource}, allowing a parent {@link MessageSource} to be configured
 * as a fallback when a message code is not found in the backing {@code MessageSupport}.
 * <p>
 * Because the Spring {@link MessageSource} API passes positional {@code Object[]} arguments rather than named
 * parameters, this adapter maps each argument to a parameter named <code>&lt;prefix&gt;&lt;n&gt;</code>, where
 * {@code prefix} is the configurable parameter prefix (default {@code "p"}) and {@code n} is the 1-based argument
 * index. For example, with the default prefix the arguments are available as {@code p1}, {@code p2}, {@code p3}, etc.
 *
 * @author Jeroen Gremmen
 * @since 0.9.2  (refactored in 0.12.0)
 */
public class MessageSupportMessageSource implements HierarchicalMessageSource
{
  private final String parameterPrefix;
  private final MessageSupport messageSupport;

  private MessageSource parentMessageSource;


  /**
   * Create a message source backed by the given {@code messageSupport} with the default parameter prefix {@code "p"}
   * and no parent message source.
   *
   * @param messageSupport  message support instance, not {@code null}
   */
  public MessageSupportMessageSource(@NotNull MessageSupport messageSupport) {
    this(messageSupport, null);
  }


  /**
   * Create a message source backed by the given {@code messageSupport} with a custom {@code parameterPrefix} and no
   * parent message source.
   *
   * @param parameterPrefix  prefix for positional parameter names (e.g. {@code "p"} results in {@code p1},
   *                         {@code p2}, …), must start with a letter, not {@code null}
   * @param messageSupport   message support instance, not {@code null}
   *
   * @throws IllegalArgumentException  if {@code parameterPrefix} is blank or does not start with a letter
   */
  public MessageSupportMessageSource(@NotNull String parameterPrefix, @NotNull MessageSupport messageSupport) {
    this(parameterPrefix, messageSupport, null);
  }


  /**
   * Create a message source backed by the given {@code messageSupport} with the default parameter prefix {@code "p"}
   * and an optional parent message source.
   *
   * @param messageSupport      message support instance, not {@code null}
   * @param parentMessageSource parent message source used as fallback, or {@code null}
   */
  protected MessageSupportMessageSource(@NotNull MessageSupport messageSupport, MessageSource parentMessageSource) {
    this("p", messageSupport, parentMessageSource);
  }


  /**
   * Create a message source backed by the given {@code messageSupport} with a custom {@code parameterPrefix} and an
   * optional parent message source.
   *
   * @param parameterPrefix      prefix for positional parameter names (e.g. {@code "p"} results in {@code p1},
   *                             {@code p2}, …), must start with a letter,
   *                             not {@code null}
   * @param messageSupport       message support instance, not {@code null}
   * @param parentMessageSource  parent message source used as fallback, or {@code null}
   *
   * @throws IllegalArgumentException  if {@code parameterPrefix} is blank or does not start with a letter
   */
  protected MessageSupportMessageSource(@NotNull String parameterPrefix,
                                        @NotNull MessageSupport messageSupport,
                                        MessageSource parentMessageSource)
  {
    if (parameterPrefix.isBlank() || !isLetter(parameterPrefix.charAt(0)))
      throw new IllegalArgumentException("parameterPrefix must start with a letter");

    this.parameterPrefix = parameterPrefix;
    this.messageSupport = messageSupport;
    this.parentMessageSource = parentMessageSource;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public MessageSource getParentMessageSource() {
    return parentMessageSource;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public void setParentMessageSource(MessageSource parentMessageSource) {
    this.parentMessageSource = parentMessageSource;
  }


  /**
   * Resolve and format a message for the given {@code code} and {@code locale}. If the code is known to the backing
   * {@link MessageSupport}, the message is formatted with the supplied {@code args} mapped as positional parameters.
   * Otherwise, the parent message source is consulted. If no message can be resolved, the {@code defaultMessage}
   * is returned.
   *
   * @param code            message code to look up, not {@code null}
   * @param args            positional arguments mapped to named parameters, or {@code null}
   * @param defaultMessage  default message returned if no message is found, or {@code null}
   * @param locale          locale for message formatting
   *
   * @return  the formatted message, or the {@code defaultMessage} if the code cannot be resolved
   */
  @Override
  public String getMessage(@NotNull String code, Object[] args, String defaultMessage, Locale locale)
  {
    if (messageSupport.getMessageAccessor().hasMessageWithCode(code))
      return getMessage(code, args, locale);

    if (parentMessageSource != null)
    {
      try {
        return parentMessageSource.getMessage(code, args, locale);
      } catch(NoSuchMessageException ignored) {
      }
    }

    return defaultMessage;
  }


  /**
   * Resolve and format a message for the given {@code code} and {@code locale}. The supplied {@code args} are mapped
   * to positional parameters using the configured parameter prefix. If the code is unknown to the backing
   * {@link MessageSupport}, the parent message source is consulted.
   *
   * @param code    message code to look up, not {@code null}
   * @param args    positional arguments mapped to named parameters, or {@code null}
   * @param locale  locale for message formatting
   *
   * @return  the formatted message, never {@code null}
   *
   * @throws NoSuchMessageException  if the code cannot be resolved and no parent message source is available
   */
  @Override
  public @NotNull String getMessage(@NotNull String code, Object[] args, Locale locale) throws NoSuchMessageException
  {
    final MessageConfigurer<Message.WithCode> msg;

    try {
      msg = messageSupport.code(code);
    } catch(IllegalArgumentException ex) {
      if (parentMessageSource != null)
        return parentMessageSource.getMessage(code, args, locale);

      throw new NoSuchMessageException(code, locale);
    }

    if (args != null)
      for(var n = 0; n < args.length; n++)
        msg.with(parameterPrefix + (n + 1), args[n]);

    return msg
        .locale(locale)
        .format();
  }


  /**
   * Resolve and format a message using the codes, arguments and default message provided by the given
   * {@code resolvable}. The codes are tried in order; the first code known to the backing {@link MessageSupport} is
   * used. If none of the codes can be resolved, the parent message source is consulted. If that also fails, the
   * {@linkplain MessageSourceResolvable#getDefaultMessage() default message} is returned, or a
   * {@link NoSuchMessageException} is thrown if no default message is available.
   *
   * @param resolvable  message source resolvable providing codes, arguments and a default message, not {@code null}
   * @param locale      locale for message formatting
   *
   * @return  the formatted message, never {@code null}
   *
   * @throws NoSuchMessageException  if none of the codes can be resolved and no default message is available
   */
  @Override
  public @NotNull String getMessage(@NotNull MessageSourceResolvable resolvable, Locale locale)
      throws NoSuchMessageException
  {
    final var codes = resolvable.getCodes();
    if (codes != null)
    {
      final var accessor = messageSupport.getMessageAccessor();

      for(var code: resolvable.getCodes())
        if (accessor.hasMessageWithCode(code))
          return getMessage(code, resolvable.getArguments(), locale);
    }

    if (parentMessageSource != null)
      return parentMessageSource.getMessage(resolvable, locale);

    final var defaultMessage = resolvable.getDefaultMessage();
    if (defaultMessage == null)
      throw new NoSuchMessageException(isEmpty(codes) ? "" : codes[codes.length - 1], locale);

    return defaultMessage;
  }
}
