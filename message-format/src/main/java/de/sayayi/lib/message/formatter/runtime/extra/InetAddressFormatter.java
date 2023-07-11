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
package de.sayayi.lib.message.formatter.runtime.extra;

import de.sayayi.lib.message.formatter.AbstractSingleTypeParameterFormatter;
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;

import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;


/**
 * @author Jeroen Gremmen
 */
public final class InetAddressFormatter extends AbstractSingleTypeParameterFormatter<InetAddress>
{
  @Override
  public @NotNull Text formatValue(@NotNull FormatterContext context,
                                   @NotNull InetAddress inetAddress)
  {
    switch(context.getConfigValueString("inet").orElse("ip"))
    {
      case "name":
        return noSpaceText(inetAddress.getHostName());

      case "fqdn":
        return noSpaceText(inetAddress.getCanonicalHostName());

      case "ip":
        return noSpaceText(inetAddress.getHostAddress());
    }

    return context.delegateToNextFormatter();
  }


  @Override
  public @NotNull FormattableType getFormattableType() {
    return new FormattableType(InetAddress.class);
  }
}
