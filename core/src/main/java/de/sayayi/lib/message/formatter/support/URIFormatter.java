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
import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.data.Data;
import de.sayayi.lib.message.data.map.MapKey.Type;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import static de.sayayi.lib.message.data.map.MapKey.Type.EMPTY;
import static de.sayayi.lib.message.data.map.MapKey.Type.NULL;
import static de.sayayi.lib.message.data.map.MapKey.Type.STRING;


/**
 * @author Jeroen Gremmen
 */
public final class URIFormatter extends AbstractParameterFormatter
{
  @Override
  @SuppressWarnings({"squid:S3358", "squid:S3776"})
  public String format(Object value, String format, @NotNull Parameters parameters, Data data)
  {
    if (value == null)
      return formatNull(parameters, data);

    final URI uri = (URI)value;

    if ("authority".equals(format))
      return parameters.getFormatter(String.class).format(uri.getAuthority(), null, parameters, data);
    else if ("fragment".equals(format))
      return uri.getFragment();
    else if ("host".equals(format))
      return uri.getHost();
    else if ("path".equals(format))
      return uri.getPath();
    else if ("port".equals(format))
    {
      final int port = uri.getPort();

      if (port == -1)
      {
        String undefined = getConfigValueString("undefined", data);
        if (undefined != null)
          return undefined;
      }
      else if (port >= 0)
      {
        Message msg = getMessage(port, EnumSet.of(Type.NUMBER), data, false);
        if (msg != null)
          return msg.format(parameters);
      }

      return port == -1 ? null : Integer.toString(port);
    }
    else if ("query".equals(format))
      return uri.getQuery();
    else if ("scheme".equals(format))
    {
      final String scheme = uri.getScheme();
      final Message msg = getMessage(scheme, EnumSet.of(STRING, EMPTY, NULL), data, false);

      return msg != null ? msg.format(parameters) : scheme;
    }
    else if ("user-info".equals(format))
      return uri.getUserInfo();

    return uri.toString();
  }


  @NotNull
  @Override
  @Contract(value = "-> new", pure = true)
  public Set<Class<?>> getFormattableTypes() {
    return Collections.<Class<?>>singleton(URI.class);
  }
}
