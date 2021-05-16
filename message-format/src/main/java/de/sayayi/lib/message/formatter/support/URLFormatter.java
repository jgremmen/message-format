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

import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.MessageContext.Parameters;
import de.sayayi.lib.message.data.Data;
import de.sayayi.lib.message.internal.part.MessagePart.Text;
import de.sayayi.lib.message.internal.part.TextPart;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.util.Set;

import static de.sayayi.lib.message.internal.part.MessagePartFactory.nullText;
import static java.util.Collections.singleton;


/**
 * @author Jeroen Gremmen
 */
public final class URLFormatter extends AbstractParameterFormatter
{
  @Override
  @SuppressWarnings({"squid:S3358", "squid:S3776"})
  public @NotNull Text formatValue(@NotNull MessageContext messageContext, Object value, String format,
                                   @NotNull Parameters parameters, Data data)
  {
    if (value == null)
      return nullText();

    final URL url = (URL)value;

    if ("authority".equals(format))
      return new TextPart(url.getAuthority());
    else if ("file".equals(format))
      return new TextPart(url.getFile());
    else if ("host".equals(format))
      return new TextPart(url.getHost());
    else if ("path".equals(format))
      return new TextPart(url.getPath());
    else if ("port".equals(format))
    {
      int port = url.getPort();
      if (port == -1)
        port = url.getDefaultPort();
      return new TextPart(Integer.toString(port));
//      return hasMessageFor(port, data)
//          ? ((DataMap)data).format(parameters, port) : (port == -1 ? null : Integer.toString(port));
    }
    else if ("query".equals(format))
      return new TextPart(url.getQuery());
    else if ("protocol".equals(format))
    {
      String protocol = url.getProtocol();
      return new TextPart(protocol);
//      return hasMessageFor(protocol, data) ? ((DataMap)data).format(parameters, protocol) : protocol;
    }
    else if ("user-info".equals(format))
      return new TextPart(url.getUserInfo());
    else if ("ref".equals(format))
      return new TextPart(url.getRef());

    return new TextPart(url.toExternalForm());
  }


  @Override
  @Contract(value = "-> new", pure = true)
  public @NotNull Set<Class<?>> getFormattableTypes() {
    return singleton(URL.class);
  }
}
