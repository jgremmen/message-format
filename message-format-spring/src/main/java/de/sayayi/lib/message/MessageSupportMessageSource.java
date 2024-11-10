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

import de.sayayi.lib.message.MessageSupport.MessageAccessor;
import de.sayayi.lib.message.MessageSupport.MessageConfigurer;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.HierarchicalMessageSource;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;

import java.util.Locale;

import static org.springframework.util.ObjectUtils.isEmpty;


/**
 * @author Jeroen Gremmen
 * @since 0.9.2
 */
public class MessageSupportMessageSource implements HierarchicalMessageSource
{
  private final String parameterPrefix;
  private final MessageSupport messageSupport;

  private MessageSource parentMessageSource;


  public MessageSupportMessageSource(@NotNull MessageSupport messageSupport) {
    this("p", messageSupport, null);
  }


  public MessageSupportMessageSource(@NotNull String parameterPrefix,
                                     @NotNull MessageSupport messageSupport) {
    this(parameterPrefix, messageSupport, null);
  }


  protected MessageSupportMessageSource(@NotNull String parameterPrefix,
                                        @NotNull MessageSupport messageSupport,
                                        MessageSource parentMessageSource)
  {
    this.parameterPrefix = parameterPrefix;
    this.messageSupport = messageSupport;
    this.parentMessageSource = parentMessageSource;
  }


  @Override
  public MessageSource getParentMessageSource() {
    return parentMessageSource;
  }


  @Override
  public void setParentMessageSource(MessageSource parentMessageSource) {
    this.parentMessageSource = parentMessageSource;
  }


  @Override
  public String getMessage(@NotNull String code, Object[] args, String defaultMessage,
                           @NotNull Locale locale)
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


  @Override
  public @NotNull String getMessage(@NotNull String code, Object[] args, @NotNull Locale locale)
      throws NoSuchMessageException
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
      for(int n = 0; n < args.length; n++)
        msg.with(parameterPrefix + (n + 1), args[n]);

    return msg
        .locale(locale)
        .format();
  }


  @Override
  public @NotNull String getMessage(@NotNull MessageSourceResolvable resolvable,
                                    @NotNull Locale locale)
      throws NoSuchMessageException
  {
    final String[] codes = resolvable.getCodes();
    if (codes != null)
    {
      final MessageAccessor accessor = messageSupport.getMessageAccessor();

      for(var code: resolvable.getCodes())
        if (accessor.hasMessageWithCode(code))
          return getMessage(code, resolvable.getArguments(), locale);
    }

    if (parentMessageSource != null)
      return parentMessageSource.getMessage(resolvable, locale);

    final String defaultMessage = resolvable.getDefaultMessage();
    if (defaultMessage == null)
      throw new NoSuchMessageException(isEmpty(codes) ? "" : codes[codes.length - 1], locale);

    return defaultMessage;
  }
}
