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
import de.sayayi.lib.message.formatter.AbstractSingleTypeParameterFormatter;
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.part.TextPart;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Optional;

import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static de.sayayi.lib.message.part.TextPartFactory.nullText;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.NUMBER_TYPE;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.STRING_EMPTY_TYPE;


/**
 * @author Jeroen Gremmen
 */
public final class URIFormatter extends AbstractSingleTypeParameterFormatter<URI>
{
  @Override
  public @NotNull Text formatValue(@NotNull FormatterContext context, @NotNull URI uri)
  {
    switch(context.getConfigValueString("uri").orElse("default"))
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
          final Optional<String> portUndef = context.getConfigValueString("uri-port-undef");
          if (portUndef.isPresent())
            return noSpaceText(portUndef.get());
        }

        final Message.WithSpaces msg = context
            .getConfigMapMessage(port, NUMBER_TYPE)
            .orElse(null);

        return msg != null
            ? new TextPart(msg.format(context.getMessageSupport(), context),
                msg.isSpaceBefore(), msg.isSpaceAfter())
            : port == -1 ? nullText() : noSpaceText(Integer.toString(port));
      }

      case "query":
        return noSpaceText(uri.getQuery());

      case "scheme": {
        final String scheme = uri.getScheme();
        final Message.WithSpaces msg = context
            .getConfigMapMessage(scheme, STRING_EMPTY_TYPE)
            .orElse(null);

        return msg != null
            ? new TextPart(msg.format(context.getMessageSupport(), context),
                msg.isSpaceBefore(), msg.isSpaceAfter())
            : new TextPart(scheme);
      }

      case "user-info":
        return noSpaceText(uri.getUserInfo());
    }

    return context.delegateToNextFormatter();
  }


  @Override
  public @NotNull FormattableType getFormattableType() {
    return new FormattableType(URI.class);
  }
}
