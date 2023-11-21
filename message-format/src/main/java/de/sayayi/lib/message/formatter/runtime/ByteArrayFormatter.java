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
import de.sayayi.lib.message.formatter.ParameterFormatter.ConfigKeyComparator;
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.Optional;

import static de.sayayi.lib.message.formatter.FormattableType.DEFAULT_PRIMITIVE_OR_ARRAY_ORDER;
import static de.sayayi.lib.message.part.TextPartFactory.emptyText;
import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult.forEmptyKey;
import static java.nio.charset.Charset.isSupported;
import static java.util.Base64.getMimeEncoder;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class ByteArrayFormatter extends AbstractSingleTypeParameterFormatter<byte[]>
    implements ConfigKeyComparator<byte[]>
{
  private static final byte[] LINE_SEPARATOR = new byte[] { '\n' };


  @Override
  @SneakyThrows(UnsupportedEncodingException.class)
  public @NotNull Text formatValue(@NotNull FormatterContext context, @NotNull byte[] byteArray)
  {
    final Optional<String> bytesConfig = context.getConfigValueString("bytes");
    if (!bytesConfig.isPresent())
      return context.delegateToNextFormatter();

    if (byteArray.length == 0)
      return emptyText();

    final String bytes = bytesConfig.get();
    if ("base64".equals(bytes))
      return noSpaceText(Base64.getEncoder().encodeToString(byteArray));
    else if ("base64-lf".equals(bytes))
      return noSpaceText(getMimeEncoder(76, LINE_SEPARATOR).encodeToString(byteArray));

    return context.format(bytes.isEmpty() || !isSupported(bytes)
        ? new String(byteArray)
        : new String(byteArray, bytes), String.class, true);
  }


  @Override
  public @NotNull FormattableType getFormattableType() {
    return new FormattableType(byte[].class, DEFAULT_PRIMITIVE_OR_ARRAY_ORDER - 10);
  }


  @Override
  public @NotNull MatchResult compareToEmptyKey(byte[] value, @NotNull ComparatorContext context) {
    return forEmptyKey(context.getCompareType(), value == null || value.length == 0);
  }
}
