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

import static de.sayayi.lib.message.formatter.parameter.ParameterFormatter.ClassifierContext.CLASSIFIER_NUMBER;
import static de.sayayi.lib.message.formatter.parameter.ParameterFormatter.ClassifierContext.CLASSIFIER_STRING;
import static de.sayayi.lib.message.part.TextPartFactory.emptyText;
import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;


/**
 * Parameter formatter for {@link URI} values.
 * <p>
 * This formatter uses the {@code uri} configuration key to select which component of the URI to format:
 * <ul>
 *   <li>{@code default} (default) &ndash; the full URI string representation</li>
 *   <li>{@code authority} &ndash; the authority component</li>
 *   <li>{@code fragment} &ndash; the fragment component</li>
 *   <li>{@code host} &ndash; the host name</li>
 *   <li>{@code path} &ndash; the path component</li>
 *   <li>{@code port} &ndash; the port number; number map keys can be used to override the output</li>
 *   <li>{@code query} &ndash; the query string</li>
 *   <li>{@code scheme} &ndash; the scheme; string map keys can be used to override the output</li>
 *   <li>{@code user-info} &ndash; the user info component</li>
 * </ul>
 * <p>
 * If the configuration value does not match a known option, formatting is delegated to the next available formatter.
 *
 * @author Jeroen Gremmen
 */
public final class URIFormatter extends AbstractSingleTypeParameterFormatter<URI>
{
  @Override
  protected boolean updateTypedClassifiers(@NotNull ClassifierContext context, @NotNull URI value)
  {
    context.addClassifier("uri");

    switch(context.getConfigValueString("uri").orElse("default"))
    {
      case "default", "authority", "fragment", "host", "path", "query", "scheme", "user-info" ->
          context.addClassifier(CLASSIFIER_STRING);
      case "port" -> context.addClassifier(CLASSIFIER_NUMBER);

      default -> {
        return false;
      }
    }

    return true;
  }


  /**
   * {@inheritDoc}
   * <p>
   * Formats the URI component specified by the {@code uri} configuration key. If no configuration is provided, the
   * full URI string representation is returned.
   */
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


  /**
   * {@inheritDoc}
   *
   * @return  formattable type for {@link URI}, never {@code null}
   */
  @Override
  protected @NotNull FormattableType getFormattableType() {
    return new FormattableType(URI.class);
  }


  /**
   * {@inheritDoc}
   *
   * @return  a set containing {@code "uri"}, never {@code null}
   */
  @Override
  public @Unmodifiable @NotNull Set<String> getParameterConfigNames() {
    return Set.of("uri");
  }
}
