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
import de.sayayi.lib.message.formatter.ParameterFormatter.EmptyMatcher;
import de.sayayi.lib.message.internal.part.MessagePart.Text;
import de.sayayi.lib.message.parameter.key.ConfigKey.CompareType;
import de.sayayi.lib.message.parameter.key.ConfigKey.MatchResult;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.io.UnsupportedEncodingException;
import java.util.Optional;
import java.util.Set;

import static de.sayayi.lib.message.internal.part.TextPartFactory.emptyText;
import static de.sayayi.lib.message.internal.part.TextPartFactory.nullText;
import static de.sayayi.lib.message.parameter.key.ConfigKey.MatchResult.TYPELESS_EXACT;
import static java.nio.charset.Charset.isSupported;
import static java.util.Collections.singleton;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class ByteArrayFormatter extends AbstractParameterFormatter implements EmptyMatcher
{
  @Override
  @SneakyThrows(UnsupportedEncodingException.class)
  public @NotNull Text formatValue(@NotNull FormatterContext context, Object byteArray)
  {
    final Optional<String> charsetConfig = context.getConfigValueString("charset");
    if (!charsetConfig.isPresent())
      return context.delegateToNextFormatter();

    if (byteArray == null)
      return nullText();

    final byte[] bytes = (byte[])byteArray;
    if (bytes.length == 0)
      return emptyText();

    final String charset = charsetConfig.get();

    return context.format(charset.isEmpty() || !isSupported(charset)
        ? new String(bytes) : new String(bytes, charset), true);
  }


  @Override
  public MatchResult matchEmpty(@NotNull CompareType compareType, @NotNull Object value) {
    return compareType.match(((byte[])value).length) ? TYPELESS_EXACT : null;
  }


  @Override
  public @NotNull Set<FormattableType> getFormattableTypes() {
    return singleton(new FormattableType(byte[].class));
  }
}
