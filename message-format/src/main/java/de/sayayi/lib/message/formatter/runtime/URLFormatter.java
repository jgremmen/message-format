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

import de.sayayi.lib.message.formatter.AbstractSingleTypeParameterFormatter;
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.NotNull;

import java.net.URL;

import static de.sayayi.lib.message.part.TextPartFactory.emptyText;
import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;


/**
 * @author Jeroen Gremmen
 */
public final class URLFormatter extends AbstractSingleTypeParameterFormatter<URL>
{
  @Override
  public @NotNull Text formatValue(@NotNull FormatterContext context, @NotNull URL url)
  {
    switch(context.getConfigValueString("url").orElse("external"))
    {
      case "authority":
        return noSpaceText(url.getAuthority());

      case "external":
        return noSpaceText(url.toExternalForm());

      case "file":
        return noSpaceText(url.getFile());

      case "host":
        return noSpaceText(url.getHost());

      case "path":
        return noSpaceText(url.getPath());

      case "port": {
        final int port = url.getPort() == -1 ? url.getDefaultPort() : url.getPort();

        return formatUsingMappedNumber(context, port, true)
            .orElseGet(() -> port == -1 ? emptyText() : noSpaceText(Integer.toString(port)));
      }

      case "query":
        return noSpaceText(url.getQuery());

      case "protocol":
        return formatUsingMappedString(context, url.getProtocol(), true)
            .orElseGet(() -> noSpaceText(url.getProtocol()));

      case "user-info":
        return noSpaceText(url.getUserInfo());

      case "ref":
        return noSpaceText(url.getRef());
    }

    return context.delegateToNextFormatter();
  }


  @Override
  public @NotNull FormattableType getFormattableType() {
    return new FormattableType(URL.class);
  }
}
