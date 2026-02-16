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
package de.sayayi.lib.message.formatter.parameter.runtime;

import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.parameter.AbstractSingleTypeParameterFormatter;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.net.URL;
import java.util.Set;

import static de.sayayi.lib.message.part.TextPartFactory.emptyText;
import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;


/**
 * @author Jeroen Gremmen
 */
public final class URLFormatter extends AbstractSingleTypeParameterFormatter<URL>
{
  @Override
  public @NotNull Text formatValue(@NotNull ParameterFormatterContext context, @NotNull URL url)
  {
    return switch(context.getConfigValueString("url").orElse("external")) {
      case "authority" -> noSpaceText(url.getAuthority());
      case "external" -> noSpaceText(url.toExternalForm());
      case "file" -> noSpaceText(url.getFile());
      case "host" -> noSpaceText(url.getHost());
      case "path" -> noSpaceText(url.getPath());
      case "port" -> {
        final var port = url.getPort() == -1 ? url.getDefaultPort() : url.getPort();
        yield formatUsingMappedNumber(context, port, true)
            .orElseGet(() -> port == -1 ? emptyText() : noSpaceText(Integer.toString(port)));
      }
      case "query" -> noSpaceText(url.getQuery());
      case "protocol" -> formatUsingMappedString(context, url.getProtocol(), true)
          .orElseGet(() -> noSpaceText(url.getProtocol()));
      case "user-info" -> noSpaceText(url.getUserInfo());
      case "ref" -> noSpaceText(url.getRef());

      default -> context.delegateToNextFormatter();
    };
  }


  @Override
  public @NotNull FormattableType getFormattableType() {
    return new FormattableType(URL.class);
  }


  @Override
  public @Unmodifiable @NotNull Set<String> getParameterConfigNames() {
    return Set.of("url");
  }
}
