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
import java.util.EnumSet;
import java.util.Set;

import static de.sayayi.lib.message.data.map.MapKey.Type.*;
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
    String format = formatterContext.getConfigValueString("uri").orElse("default");

    if ("authority".equals(format))
      return noSpaceText(uri.getAuthority());
    else if ("fragment".equals(format))
      return noSpaceText(uri.getFragment());
    else if ("host".equals(format))
      return noSpaceText(uri.getHost());
    else if ("path".equals(format))
      return noSpaceText(uri.getPath());
    else if ("port".equals(format))
    {
      final int port = uri.getPort();

      if (port == -1)
      {
        String undefined = formatterContext.getConfigValueString("uri-port-undef").orElse(null);
        if (undefined != null)
          return noSpaceText(undefined);
      }
      else if (port >= 0)
      {
        Message.WithSpaces msg = formatterContext.getMapMessage(port, EnumSet.of(NUMBER)).orElse(null);
        if (msg != null)
          return new TextPart(msg.format(formatterContext.getMessageContext(), formatterContext), msg.isSpaceBefore(), msg.isSpaceAfter());
      }

      return port == -1 ? nullText() : noSpaceText(Integer.toString(port));
    }
    else if ("query".equals(format))
      return noSpaceText(uri.getQuery());
    else if ("scheme".equals(format))
    {
      final String scheme = uri.getScheme();
      final Message.WithSpaces msg = formatterContext.getMapMessage(scheme, EnumSet.of(STRING, EMPTY, NULL)).orElse(null);

      return msg != null
          ? new TextPart(msg.format(formatterContext.getMessageContext(), formatterContext), msg.isSpaceBefore(), msg.isSpaceAfter())
          : new TextPart(scheme);
    }
    else if ("user-info".equals(format))
      return noSpaceText(uri.getUserInfo());

    return noSpaceText(uri.toString());
  }


  @Override
  @Contract(value = "-> new", pure = true)
  public @NotNull Set<FormattableType> getFormattableTypes() {
    return singleton(new FormattableType(URI.class));
  }
}