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
import de.sayayi.lib.message.formatter.parameter.ParameterFormatter.MapKeyComparator;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatter.SizeQueryable;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext;
import de.sayayi.lib.message.part.MapKey.MatchResult;
import de.sayayi.lib.message.part.MessagePart.Text;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.OptionalLong;
import java.util.Set;

import static de.sayayi.lib.message.formatter.FormattableType.DEFAULT_PRIMITIVE_OR_ARRAY_ORDER;
import static de.sayayi.lib.message.formatter.parameter.ParameterFormatter.ClassifierContext.CLASSIFIER_STRING;
import static de.sayayi.lib.message.part.MapKey.MatchResult.forEmptyKey;
import static de.sayayi.lib.message.part.TextPartFactory.emptyText;
import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static java.nio.charset.Charset.isSupported;
import static java.util.Base64.getMimeEncoder;


/**
 * Parameter formatter for {@code byte[]} values.
 * <p>
 * This formatter converts a byte array to a string representation based on the {@code bytes} configuration key.
 * <p>
 * If the {@code bytes} configuration key is absent, formatting is delegated to the next available formatter
 * (e.g. the general array formatter).
 * <p>
 * As a {@link SizeQueryable} formatter, it reports the length of the byte array.
 * <p>
 * <b>Configuration keys:</b>
 * <table border="1">
 *   <caption>Configuration keys for the byte array formatter</caption>
 *   <tr><th>Key</th><th>Values</th><th>Default</th><th>Purpose</th></tr>
 *   <tr>
 *     <td>{@code bytes}</td>
 *     <td>{@code base64}, {@code base64-lf}, charset name</td>
 *     <td>(none)</td>
 *     <td>
 *       {@code base64} encodes the bytes as a Base64 string, {@code base64-lf} as a MIME Base64 string with line
 *       breaks. A charset name, e.g. {@code UTF-8}, decodes the bytes as a string. An empty or unsupported
 *       charset name uses the platform default charset. If absent, formatting is delegated to the next formatter.
 *     </td>
 *   </tr>
 * </table>
 * <p>
 * <b>Examples:</b>
 * <p>
 * Encode binary data as Base64 for diagnostic output:
 * <pre>{@code
 * messageSupport
 *     .message("Checksum: %{sum,bytes:base64}")
 *     .with("sum", new byte[] { 1, 2, 3, 4 })
 *     .format();
 * // "Checksum: AQIDBA=="
 * }</pre>
 * <p>
 * Decode text received as bytes. The decoded string is formatted by the string formatter, so its map keys apply:
 * <pre>{@code
 * var payload = "Grüße".getBytes(StandardCharsets.ISO_8859_1);
 *
 * messageSupport.message("%{data,bytes:'ISO-8859-1'}").with("data", payload).format();
 * // "Grüße"
 *
 * messageSupport
 *     .message("%{data,bytes:'US-ASCII','OK':'accepted',:'rejected'}")
 *     .with("data", "OK".getBytes(StandardCharsets.US_ASCII))
 *     .format();
 * // "accepted"
 * }</pre>
 * <p>
 * Without the {@code bytes} key, the array formatter is used. The size and emptiness of the array are available in
 * either case:
 * <pre>{@code
 * messageSupport.message("%{data}").with("data", new byte[] { 1, 2, 3 }).format();
 * // "1, 2, 3"
 *
 * messageSupport
 *     .message("%{data,bytes:base64,empty:'no data',!empty:'%{data,format:size} bytes'}")
 *     .with("data", new byte[16])
 *     .format();
 * // "16 bytes"
 * }</pre>
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class ByteArrayFormatter extends AbstractSingleTypeParameterFormatter<byte[]>
    implements SizeQueryable, MapKeyComparator<byte[]>
{
  /** Line separator used when encoding a byte array in base64 MIME format. */
  private static final byte[] LINE_SEPARATOR = new byte[] { '\n' };


  /**
   * {@inheritDoc}
   * <p>
   * Adds the {@code array} classifier. If the {@code bytes} configuration key is present, the
   * {@link ClassifierContext#CLASSIFIER_STRING string} classifier is added as well.
   */
  @Override
  protected boolean updateTypedClassifiers(@NotNull ClassifierContext context, byte @NotNull [] value)
  {
    context.addClassifier("array");

    if (context.getConfigValueString("bytes").isEmpty())
      return false;
    else
    {
      context.addClassifier(CLASSIFIER_STRING);
      return true;
    }
  }


  /**
   * {@inheritDoc}
   * <p>
   * Converts the byte array to a string based on the {@code bytes} configuration key. If the key is absent,
   * formatting is delegated to the next formatter.
   */
  @Override
  @SneakyThrows(UnsupportedEncodingException.class)
  public @NotNull Text formatValue(@NotNull ParameterFormatterContext context, byte @NotNull [] byteArray)
  {
    var bytesConfig = context.getConfigValueString("bytes");
    if (bytesConfig.isEmpty())
      return context.delegateToNextFormatter();

    if (byteArray.length == 0)
      return emptyText();

    var bytes = bytesConfig.get();
    if ("base64".equals(bytes))
      return noSpaceText(Base64.getEncoder().encodeToString(byteArray));
    else if ("base64-lf".equals(bytes))
      return noSpaceText(getMimeEncoder(76, LINE_SEPARATOR).encodeToString(byteArray));

    return context.format(bytes.isEmpty() || !isSupported(bytes)
        ? new String(byteArray)
        : new String(byteArray, bytes), String.class);
  }


  /**
   * {@inheritDoc}
   *
   * @return  formattable type for {@code byte[]}, never {@code null}
   */
  @Override
  protected @NotNull FormattableType getFormattableType() {
    return new FormattableType(byte[].class, DEFAULT_PRIMITIVE_OR_ARRAY_ORDER - 10);
  }


  /**
   * {@inheritDoc}
   * <p>
   * Returns the length of the byte array.
   */
  @Override
  public @NotNull OptionalLong size(@NotNull ParameterFormatterContext context, @NotNull Object value) {
    return OptionalLong.of(((byte[])value).length);
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull MatchResult compareToEmptyKey(byte[] value, @NotNull ComparatorContext context) {
    return forEmptyKey(context.getCompareType(), value == null || value.length == 0);
  }


  /**
   * {@inheritDoc}
   *
   * @return  a set containing {@code "bytes"}, never {@code null}
   */
  @Override
  public @Unmodifiable @NotNull Set<String> getParameterConfigNames() {
    return Set.of("bytes");
  }
}
