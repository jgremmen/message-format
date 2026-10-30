/*
 * Copyright 2023 Jeroen Gremmen
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
package de.sayayi.lib.message.formatter.parameter.runtime.extra;

import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.parameter.AbstractMultiSelectFormatter;
import org.jetbrains.annotations.NotNull;

import java.security.Key;
import java.util.Set;

import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;


/**
 * Parameter formatter for {@link Key} (cryptographic key) values.
 * <p>
 * This formatter uses the {@code key} configuration key to select which aspect of the cryptographic key to format.
 * <p>
 * If the configuration key is absent or does not match a known option, formatting is delegated to the next available
 * formatter.
 * <p>
 * <b>Configuration keys:</b>
 * <table border="1">
 *   <caption>Configuration keys for the key formatter</caption>
 *   <tr><th>Key</th><th>Values</th><th>Default</th><th>Purpose</th></tr>
 *   <tr>
 *     <td>{@code key}</td>
 *     <td>{@code algorithm}, {@code format}, {@code encoded}</td>
 *     <td>(none)</td>
 *     <td>
 *       Formats the algorithm name, the encoding format name or the encoded form of the key. The encoded form is
 *       formatted as a byte array. If absent or any other value, formatting is delegated to the next formatter.
 *     </td>
 *   </tr>
 * </table>
 * <p>
 * <b>Examples:</b>
 * <p>
 * This formatter is not part of the {@link de.sayayi.lib.message.formatter.DefaultFormatterService
 * DefaultFormatterService} and must be registered explicitly:
 * <pre>{@code
 * var formatterService = new DefaultFormatterService();
 * formatterService.addFormatter(new KeyFormatter());
 *
 * var messageSupport = MessageSupportFactory.create(formatterService).seal();
 * }</pre>
 * <p>
 * Describe a key, without exposing its content:
 * <pre>{@code
 * var publicKey = KeyPairGenerator.getInstance("RSA").generateKeyPair().getPublic();
 *
 * messageSupport
 *     .message("Using %{k,key:algorithm} key in %{k,key:format} format")
 *     .with("k", publicKey)
 *     .format();
 * // "Using RSA key in X.509 format"
 * }</pre>
 * <p>
 * The encoded form of a public key, using the {@code bytes} configuration key of the byte array formatter:
 * <pre>{@code
 * var publicKey = KeyPairGenerator.getInstance("RSA").generateKeyPair().getPublic();
 *
 * messageSupport
 *     .message("%{k,key:encoded,bytes:base64}")
 *     .with("k", publicKey)
 *     .format();
 * // e.g. "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA..."
 * }</pre>
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class KeyFormatter extends AbstractMultiSelectFormatter<Key>
{
  /**
   * Creates a new cryptographic key formatter with the configuration key {@code key} and
   * selection options for algorithm, format and encoded form.
   */
  public KeyFormatter()
  {
    super("key");

    register("algorithm", (context,key) -> noSpaceText(key.getAlgorithm()));
    register("format", (context,key) -> noSpaceText(key.getFormat()));
    register("encoded", (context,key) -> context.format(key.getEncoded(), byte[].class));
  }


  /**
   * {@inheritDoc}
   *
   * @return  a set containing the {@link Key} formattable type, never {@code null}
   */
  @Override
  public @NotNull Set<FormattableType> getFormattableTypes() {
    return Set.of(new FormattableType(Key.class));
  }
}
