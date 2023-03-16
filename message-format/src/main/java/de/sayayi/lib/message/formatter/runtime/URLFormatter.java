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
package de.sayayi.lib.message.formatter.runtime;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.formatter.AbstractParameterFormatter;
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.internal.part.MessagePart.Text;
import de.sayayi.lib.message.internal.part.TextPart;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.util.Optional;
import java.util.Set;

import static de.sayayi.lib.message.internal.part.MessagePartFactory.noSpaceText;
import static de.sayayi.lib.message.internal.part.MessagePartFactory.nullText;
import static de.sayayi.lib.message.parameter.key.ConfigKey.NUMBER_TYPE;
import static java.util.Collections.singleton;


/**
 * @author Jeroen Gremmen
 */
public final class URLFormatter extends AbstractParameterFormatter
{
  @Override
  public @NotNull Text formatValue(@NotNull FormatterContext formatterContext, Object value)
  {
    if (value == null)
      return nullText();

    final URL url = (URL)value;

    switch(formatterContext.getConfigValueString("url").orElse("external"))
    {
      case "authority":
        return noSpaceText(url.getAuthority());

      case "external":
        return noSpaceText(url.toExternalForm());

      case "file":
        return noSpaceText(url.getFile());

      case "host":
        return noSpaceText(url.getHost());

      case "path":
        return noSpaceText(url.getPath());

      case "port": {
        final int port = url.getPort();
        if (port == -1)
        {
          final Optional<String> portUndef = formatterContext.getConfigValueString("uri-port-undef");
          if (portUndef.isPresent())
            return noSpaceText(portUndef.get());
        }

        final Message.WithSpaces msg = formatterContext.getMapMessage(port, NUMBER_TYPE).orElse(null);
        return msg != null
            ? new TextPart(msg.format(formatterContext.getMessageSupport(), formatterContext),
                msg.isSpaceBefore(), msg.isSpaceAfter())
            : port == -1 ? nullText() : noSpaceText(Integer.toString(port));
      }

      case "query":
        return noSpaceText(url.getQuery());

      case "protocol":
        return noSpaceText(url.getProtocol());

      case "user-info":
        return noSpaceText(url.getUserInfo());

      case "ref":
        return noSpaceText(url.getRef());
    }

    return formatterContext.delegateToNextFormatter();
  }


  @Override
  @Contract(value = "-> new", pure = true)
  public @NotNull Set<FormattableType> getFormattableTypes() {
    return singleton(new FormattableType(URL.class));
  }
}
