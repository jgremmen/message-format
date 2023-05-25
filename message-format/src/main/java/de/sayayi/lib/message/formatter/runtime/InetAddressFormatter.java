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

import de.sayayi.lib.message.formatter.AbstractParameterFormatter;
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.util.Set;

import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static de.sayayi.lib.message.part.TextPartFactory.nullText;
import static java.util.Collections.singleton;


/**
 * @author Jeroen Gremmen
 */
public final class InetAddressFormatter extends AbstractParameterFormatter
{
  @Override
  public @NotNull Text formatValue(@NotNull FormatterContext context, Object value)
  {
    if (value == null)
      return nullText();

    final InetAddress inetAddress = (InetAddress)value;

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
  public @NotNull Set<FormattableType> getFormattableTypes() {
    return singleton(new FormattableType(InetAddress.class));
  }
}
