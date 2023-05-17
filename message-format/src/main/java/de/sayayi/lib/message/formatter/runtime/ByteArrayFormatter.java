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
import de.sayayi.lib.message.formatter.ParameterFormatter.SizeQueryable;
import de.sayayi.lib.message.internal.part.MessagePart.Text;
import de.sayayi.lib.message.parameter.key.ConfigKey.CompareType;
import de.sayayi.lib.message.parameter.key.ConfigKey.MatchResult;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.io.UnsupportedEncodingException;
import java.util.OptionalLong;
import java.util.Set;

import static de.sayayi.lib.message.internal.part.MessagePartFactory.emptyText;
import static de.sayayi.lib.message.parameter.key.ConfigKey.MatchResult.TYPELESS_EXACT;
import static java.nio.charset.Charset.isSupported;
import static java.util.Collections.singleton;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class ByteArrayFormatter extends AbstractParameterFormatter
    implements EmptyMatcher, SizeQueryable
{
  @Override
  @SneakyThrows(UnsupportedEncodingException.class)
  public @NotNull Text formatValue(@NotNull FormatterContext context, Object byteArray)
  {
    final byte[] bytes = (byte[])byteArray;
    if (bytes == null || bytes.length == 0)
      return emptyText();

    final String charset = context.getConfigValueString("charset").orElse(null);
    if (charset != null && !charset.isEmpty())
    {
      return context.format(isSupported(charset)
          ? new String(bytes, charset) : new String(bytes), true);
    }

    return new ArrayFormatter().formatValue(context, bytes);
  }


  @Override
  public MatchResult matchEmpty(@NotNull CompareType compareType, @NotNull Object value) {
    return compareType.match(((byte[])value).length) ? TYPELESS_EXACT : null;
  }


  @Override
  public @NotNull OptionalLong size(@NotNull FormatterContext context, @NotNull Object value) {
    return OptionalLong.of(((byte[])value).length);
  }


  @Override
  public @NotNull Set<FormattableType> getFormattableTypes() {
    return singleton(new FormattableType(byte[].class));
  }
}
