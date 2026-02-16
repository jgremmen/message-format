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

import java.net.URI;
import java.util.Set;

import static de.sayayi.lib.message.part.TextPartFactory.emptyText;
import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;


/**
 * @author Jeroen Gremmen
 */
public final class URIFormatter extends AbstractSingleTypeParameterFormatter<URI>
{
  @Override
  public @NotNull Text formatValue(@NotNull ParameterFormatterContext context, @NotNull URI uri)
  {
    return switch(context.getConfigValueString("uri").orElse("default")) {
      case "default" -> noSpaceText(uri.toString());
      case "authority" -> noSpaceText(uri.getAuthority());
      case "fragment" -> noSpaceText(uri.getFragment());
      case "host" -> noSpaceText(uri.getHost());
      case "path" -> noSpaceText(uri.getPath());
      case "port" -> {
        final var port = uri.getPort();
        yield formatUsingMappedNumber(context, port, true)
            .orElseGet(() -> port == -1 ? emptyText() : noSpaceText(Integer.toString(port)));
      }
      case "query" -> noSpaceText(uri.getQuery());
      case "scheme" -> formatUsingMappedString(context, uri.getScheme(), true)
          .orElseGet(() -> noSpaceText(uri.getScheme()));
      case "user-info" -> noSpaceText(uri.getUserInfo());

      default -> context.delegateToNextFormatter();
    };
  }


  @Override
  public @NotNull FormattableType getFormattableType() {
    return new FormattableType(URI.class);
  }


  @Override
  public @Unmodifiable @NotNull Set<String> getParameterConfigNames() {
    return Set.of("uri");
  }
}
