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

import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.data.Data;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.util.Collections;
import java.util.Set;


/**
 * @author Jeroen Gremmen
 */
public final class URLFormatter extends AbstractParameterFormatter
{
  @Override
  @SuppressWarnings({"squid:S3358", "squid:S3776"})
  public String formatValue(Object value, String format, @NotNull Parameters parameters, Data data)
  {
    if (value == null)
      return null;

    final URL url = (URL)value;

    if ("authority".equals(format))
      return url.getAuthority();
    else if ("file".equals(format))
      return url.getFile();
    else if ("host".equals(format))
      return url.getHost();
    else if ("path".equals(format))
      return url.getPath();
    else if ("port".equals(format))
    {
      int port = url.getPort();
      if (port == -1)
        port = url.getDefaultPort();
      return Integer.toString(port);
//      return hasMessageFor(port, data)
//          ? ((DataMap)data).format(parameters, port) : (port == -1 ? null : Integer.toString(port));
    }
    else if ("query".equals(format))
      return url.getQuery();
    else if ("protocol".equals(format))
    {
      String protocol = url.getProtocol();
      return protocol;
//      return hasMessageFor(protocol, data) ? ((DataMap)data).format(parameters, protocol) : protocol;
    }
    else if ("user-info".equals(format))
      return url.getUserInfo();
    else if ("ref".equals(format))
      return url.getRef();

    return url.toExternalForm();
  }


  @NotNull
  @Override
  @Contract(value = "-> new", pure = true)
  public Set<Class<?>> getFormattableTypes() {
    return Collections.<Class<?>>singleton(URL.class);
  }
}
