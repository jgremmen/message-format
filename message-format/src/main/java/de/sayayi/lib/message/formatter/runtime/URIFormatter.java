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

import java.net.URI;
import java.util.Optional;
import java.util.Set;

import static de.sayayi.lib.message.data.map.MapKey.NUMBER_TYPE;
import static de.sayayi.lib.message.data.map.MapKey.STRING_EMPTY_TYPE;
import static de.sayayi.lib.message.internal.part.MessagePartFactory.noSpaceText;
import static de.sayayi.lib.message.internal.part.MessagePartFactory.nullText;
import static java.util.Collections.singleton;


/**
 * @author Jeroen Gremmen
 */
public final class URIFormatter extends AbstractParameterFormatter
{
  @Override
  public @NotNull Text formatValue(@NotNull FormatterContext formatterContext, Object value)
  {
    if (value == null)
      return nullText();

    final URI uri = (URI)value;

    switch(formatterContext.getConfigValueString("uri").orElse("default"))
    {
      case "default":
        return noSpaceText(uri.toString());

      case "authority":
        return noSpaceText(uri.getAuthority());

      case "fragment":
        return noSpaceText(uri.getFragment());

      case "host":
        return noSpaceText(uri.getHost());

      case "path":
        return noSpaceText(uri.getPath());

      case "port": {
        final int port = uri.getPort();
        if (port == -1)
        {
          final Optional<String> portUndef = formatterContext.getConfigValueString("uri-port-undef");
          if (portUndef.isPresent())
            return noSpaceText(portUndef.get());
        }

        final Message.WithSpaces msg = formatterContext.getMapMessage(port, NUMBER_TYPE).orElse(null);
        return msg != null
            ? new TextPart(msg.format(formatterContext.getMessageContext(), formatterContext),
                msg.isSpaceBefore(), msg.isSpaceAfter())
            : port == -1 ? nullText() : noSpaceText(Integer.toString(port));
      }

      case "query":
        return noSpaceText(uri.getQuery());

      case "scheme": {
        final String scheme = uri.getScheme();
        final Message.WithSpaces msg = formatterContext.getMapMessage(scheme, STRING_EMPTY_TYPE).orElse(null);

        return msg != null
            ? new TextPart(msg.format(formatterContext.getMessageContext(), formatterContext),
                msg.isSpaceBefore(), msg.isSpaceAfter())
            : new TextPart(scheme);
      }

      case "user-info":
        return noSpaceText(uri.getUserInfo());
    }

    return formatterContext.delegateToNextFormatter();
  }


  @Override
  @Contract(value = "-> new", pure = true)
  public @NotNull Set<FormattableType> getFormattableTypes() {
    return singleton(new FormattableType(URI.class));
  }
}
