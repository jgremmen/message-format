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

import static de.sayayi.lib.message.formatter.parameter.ParameterFormatter.ClassifierContext.CLASSIFIER_NUMBER;
import static de.sayayi.lib.message.formatter.parameter.ParameterFormatter.ClassifierContext.CLASSIFIER_STRING;
import static de.sayayi.lib.message.part.TextPartFactory.emptyText;
import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;


/**
 * Parameter formatter for {@link URL} values.
 * <p>
 * This formatter uses the {@code url} configuration key to select which component of the URL to format.
 * <p>
 * If the configuration value does not match a known option, formatting is delegated to the next available formatter.
 * <p>
 * <b>Configuration keys:</b>
 * <table border="1">
 *   <caption>Configuration keys for the url formatter</caption>
 *   <tr><th>Key</th><th>Values</th><th>Default</th><th>Purpose</th></tr>
 *   <tr>
 *     <td>{@code url}</td>
 *     <td>
 *       {@code external}, {@code authority}, {@code file}, {@code host}, {@code path}, {@code port}, {@code
 *       protocol}, {@code query}, {@code user-info}, {@code ref}
 *     </td>
 *     <td>{@code external}</td>
 *     <td>
 *       Component of the URL to format, {@code external} being its full external form. {@code port} falls back to
 *       the default port of the protocol. Number keys override the output for {@code port} and string keys for
 *       {@code protocol}. Any other value delegates to the next formatter.
 *     </td>
 *   </tr>
 * </table>
 * <p>
 * <b>Examples:</b>
 * <p>
 * Format individual components of a URL:
 * <pre>{@code
 * var url = URI.create("https://example.com/search?q=test#results").toURL();
 *
 * messageSupport.message("%{u,url:host}%{u,url:file}").with("u", url).format();
 * // "example.com/search?q=test"
 *
 * messageSupport.message("Section: %{u,url:ref}").with("u", url).format();
 * // "Section: results"
 * }</pre>
 * <p>
 * Without an explicit port, the default port of the protocol is used:
 * <pre>{@code
 * var url = URI.create("https://example.com/").toURL();
 *
 * messageSupport.message("Port %{u,url:port}").with("u", url).format();
 * // "Port 443"
 *
 * messageSupport.message("%{u,url:port,443:'standard port',:'custom port'}").with("u", url).format();
 * // "standard port"
 * }</pre>
 * <p>
 * Describe the protocol using string keys:
 * <pre>{@code
 * messageSupport
 *     .message("%{u,url:protocol,'https':'encrypted',:'not encrypted'}")
 *     .with("u", URI.create("http://example.com/").toURL())
 *     .format();
 * // "not encrypted"
 * }</pre>
 *
 * @author Jeroen Gremmen
 * @since 0.3.1
 */
public final class URLFormatter extends AbstractSingleTypeParameterFormatter<URL>
{
  /**
   * {@inheritDoc}
   * <p>
   * Adds the {@code url} classifier. Depending on the {@code url} configuration key, the
   * {@link ClassifierContext#CLASSIFIER_STRING string} or {@link ClassifierContext#CLASSIFIER_NUMBER number}
   * classifier is added as well.
   */
  @Override
  protected boolean updateTypedClassifiers(@NotNull ClassifierContext context, @NotNull URL value)
  {
    context.addClassifier("url");

    switch(context.getConfigValueString("url").orElse("external"))
    {
      case "authority", "external", "file", "host", "path", "query", "protocol", "user-info", "ref" ->
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
   * Formats the URL component specified by the {@code url} configuration key. If no configuration is provided, the
   * full external form of the URL is returned.
   */
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


  /**
   * {@inheritDoc}
   *
   * @return  formattable type for {@link URL}, never {@code null}
   */
  @Override
  protected @NotNull FormattableType getFormattableType() {
    return new FormattableType(URL.class);
  }


  /**
   * {@inheritDoc}
   *
   * @return  a set containing {@code "url"}, never {@code null}
   */
  @Override
  public @Unmodifiable @NotNull Set<String> getParameterConfigNames() {
    return Set.of("url");
  }
}
