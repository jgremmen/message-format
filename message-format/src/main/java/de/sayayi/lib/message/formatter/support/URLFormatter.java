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
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.util.Set;

import static de.sayayi.lib.message.internal.part.MessagePartFactory.noSpaceText;
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
      return noSpaceText(url.getAuthority());
    else if ("file".equals(format))
      return noSpaceText(url.getFile());
    else if ("host".equals(format))
      return noSpaceText(url.getHost());
    else if ("path".equals(format))
      return noSpaceText(url.getPath());
    else if ("port".equals(format))
    {
      int port = url.getPort();
      return noSpaceText(Integer.toString(port == -1 ? url.getDefaultPort() : port));
    }
    else if ("query".equals(format))
      return noSpaceText(url.getQuery());
    else if ("protocol".equals(format))
      return noSpaceText(url.getProtocol());
    else if ("user-info".equals(format))
      return noSpaceText(url.getUserInfo());
    else if ("ref".equals(format))
      return noSpaceText(url.getRef());

    return noSpaceText(url.toExternalForm());
  }


  @Override
  @Contract(value = "-> new", pure = true)
  public @NotNull Set<Class<?>> getFormattableTypes() {
    return singleton(URL.class);
  }
}
