/*
 * Copyright 2019 Jeroen Gremmen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.sayayi.lib.message.formatter.support;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.MessageContext.Parameters;
import de.sayayi.lib.message.data.Data;
import de.sayayi.lib.message.data.map.MapKey.Type;
import de.sayayi.lib.message.internal.part.MessagePart.Text;
import de.sayayi.lib.message.internal.part.TextPart;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.EnumSet;
import java.util.Set;

import static de.sayayi.lib.message.data.map.MapKey.Type.EMPTY;
import static de.sayayi.lib.message.data.map.MapKey.Type.NULL;
import static de.sayayi.lib.message.data.map.MapKey.Type.STRING;
import static de.sayayi.lib.message.internal.part.MessagePartFactory.nullText;
import static java.util.Collections.singleton;


/**
 * @author Jeroen Gremmen
 */
public final class URIFormatter extends AbstractParameterFormatter
{
  @Override
  @SuppressWarnings({"squid:S3358", "squid:S3776"})
  public @NotNull Text formatValue(@NotNull MessageContext messageContext, Object value, String format,
                                   @NotNull Parameters parameters, Data data)
  {
    if (value == null)
      return nullText();

    final URI uri = (URI)value;

    if ("authority".equals(format))
      return messageContext.getFormatter(String.class).format(messageContext, uri.getAuthority(), null, parameters, data);
    else if ("fragment".equals(format))
      return new TextPart(uri.getFragment());
    else if ("host".equals(format))
      return new TextPart(uri.getHost());
    else if ("path".equals(format))
      return new TextPart(uri.getPath());
    else if ("port".equals(format))
    {
      final int port = uri.getPort();

      if (port == -1)
      {
        String undefined = getConfigValueString(messageContext, "undefined", parameters, data,
            false,null);
        if (undefined != null)
          return new TextPart(undefined);
      }
      else if (port >= 0)
      {
        Message.WithSpaces msg = getMessage(messageContext, port, EnumSet.of(Type.NUMBER), parameters, data,
            false);
        if (msg != null)
          return new TextPart(msg.format(messageContext, parameters), msg.isSpaceBefore(), msg.isSpaceAfter());
      }

      return port == -1 ? nullText() : new TextPart(Integer.toString(port));
    }
    else if ("query".equals(format))
      return new TextPart(uri.getQuery());
    else if ("scheme".equals(format))
    {
      final String scheme = uri.getScheme();
      final Message.WithSpaces msg = getMessage(messageContext, scheme, EnumSet.of(STRING, EMPTY, NULL), parameters,
          data, false);

      return msg != null
          ? new TextPart(msg.format(messageContext, parameters), msg.isSpaceBefore(), msg.isSpaceAfter())
          : new TextPart(scheme);
    }
    else if ("user-info".equals(format))
      return new TextPart(uri.getUserInfo());

    return new TextPart(uri.toString());
  }


  @Override
  @Contract(value = "-> new", pure = true)
  public @NotNull Set<Class<?>> getFormattableTypes() {
    return singleton(URI.class);
  }
}
