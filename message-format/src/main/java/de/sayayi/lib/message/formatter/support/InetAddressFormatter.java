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
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.util.Set;

import static de.sayayi.lib.message.internal.part.MessagePartFactory.noSpaceText;
import static java.util.Collections.singleton;


/**
 * @author Jeroen Gremmen
 */
public final class InetAddressFormatter extends AbstractParameterFormatter
{
  @Override
  public @NotNull Text formatValue(@NotNull MessageContext messageContext, Object value, String format,
                                   @NotNull Parameters parameters, Data data)
  {
    String s = null;

    if (value != null)
    {
      final InetAddress inetAddress = (InetAddress)value;
      format = getConfigFormat(messageContext, format, data, true, null);

      if ("name".equals(format))
        s = inetAddress.getHostName();
      else if ("canonical".equals(format) || "fqdn".equals(format))
        s = inetAddress.getCanonicalHostName();
      else
        s = inetAddress.getHostAddress();
    }

    return noSpaceText(s);
  }


  @Override
  public @NotNull Set<Class<?>> getFormattableTypes() {
    return singleton(InetAddress.class);
  }
}
