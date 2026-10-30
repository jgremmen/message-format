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
 * This formatter uses the {@code uri} configuration key to select which component of the URI to format.
 * <p>
 * If the configuration value does not match a known option, formatting is delegated to the next available formatter.
 * <p>
 * <b>Configuration keys:</b>
 * <table border="1">
 *   <caption>Configuration keys for the uri formatter</caption>
 *   <tr><th>Key</th><th>Values</th><th>Default</th><th>Purpose</th></tr>
 *   <tr>
 *     <td>{@code uri}</td>
 *     <td>
 *       {@code default}, {@code authority}, {@code fragment}, {@code host}, {@code path}, {@code port}, {@code
 *       query}, {@code scheme}, {@code user-info}
 *     </td>
 *     <td>{@code default}</td>
 *     <td>
 *       Component of the URI to format, {@code default} being the full string representation. Number keys
 *       override the output for {@code port} and string keys for {@code scheme}. Any other value delegates to the
 *       next formatter.
 *     </td>
 *   </tr>
 * </table>
 * <p>
 * <b>Examples:</b>
 * <p>
 * Format individual components of a URI:
 * <pre>{@code
 * var uri = URI.create("https://user@example.com:8443/api/v1?q=test#section");
 *
 * messageSupport
 *     .message("Connecting to %{u,uri:host} on port %{u,uri:port}")
 *     .with("u", uri)
 *     .format();
 * // "Connecting to example.com on port 8443"
 *
 * messageSupport.message("%{u,uri:path}?%{u,uri:query}").with("u", uri).format();
 * // "/api/v1?q=test"
 * }</pre>
 * <p>
 * Describe the scheme using string keys, with the default entry being used for all other schemes:
 * <pre>{@code
 * var message = "%{u,uri:scheme,'https':'secure','http':'not secure',:'unsupported'}";
 *
 * messageSupport.message(message).with("u", URI.create("https://example.com")).format();
 * // "secure"
 *
 * messageSupport.message(message).with("u", URI.create("ftp://example.com")).format();
 * // "unsupported"
 * }</pre>
 * <p>
 * A missing component results in an empty text, which the {@code empty} key can handle. A missing port has the number
 * {@code -1}:
 * <pre>{@code
 * var uri = URI.create("https://example.com");
 *
 * messageSupport.message("%{u,uri:fragment,empty:'no fragment'}").with("u", uri).format();
 * // "no fragment"
 *
 * messageSupport.message("%{u,uri:port,-1:'default port'}").with("u", uri).format();
 * // "default port"
 * }</pre>
 *
 * @author Jeroen Gremmen
 * @since 0.3.1
 */
public final class URIFormatter extends AbstractSingleTypeParameterFormatter<URI>
{
  /**
   * {@inheritDoc}
   * <p>
   * Adds the {@code uri} classifier. Depending on the {@code uri} configuration key, the
   * {@link ClassifierContext#CLASSIFIER_STRING string} or {@link ClassifierContext#CLASSIFIER_NUMBER number}
   * classifier is added as well.
   */
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
