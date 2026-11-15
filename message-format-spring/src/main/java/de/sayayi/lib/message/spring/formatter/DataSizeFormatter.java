/*
 * Copyright 2026 Jeroen Gremmen
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
package de.sayayi.lib.message.spring.formatter;

import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.parameter.AbstractSingleTypeParameterFormatter;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.unit.DataSize;


/**
 * Parameter formatter for Spring {@link DataSize} values.
 * <p>
 * The data size is converted to its number of bytes and formatting is delegated to the {@code filesize} formatter. The
 * parameter configuration is passed on unchanged, so the configuration keys and unit labels of the {@code filesize}
 * formatter apply. If no {@code filesize} formatter is registered, the number of bytes is formatted according to its
 * type, as a {@code long}.
 * <p>
 * The {@code filesize} formatter selects units based on powers of 1000, whereas the units of {@link DataSize} are based
 * on powers of 1024. A data size of 1 kilobyte ({@code DataSize.ofKilobytes(1)}, 1024 bytes) is therefore formatted as
 * {@code 1.0KB}, but 512 kilobytes (524,288 bytes) as {@code 524.3KB}.
 * <p>
 * <b>Examples:</b>
 * <p>
 * This formatter is provided as a service and is registered automatically by the
 * {@link de.sayayi.lib.message.formatter.DefaultFormatterService DefaultFormatterService}. The {@code filesize}
 * formatter is not registered automatically and must be added to the formatter service:
 * <pre>{@code
 * var formatterService = new DefaultFormatterService();
 * formatterService.addFormatter(new FileSizeFormatter());
 *
 * var messageSupport = MessageSupportFactory.create(formatterService).seal();
 * }</pre>
 * <p>
 * The configuration keys of the {@code filesize} formatter apply to the data size:
 * <pre>{@code
 * messageSupport.message("%{size}").with("size", DataSize.ofBytes(1234567)).locale(Locale.US).format();
 * // "1.2MB"
 *
 * messageSupport
 *     .message("%{size,scale:0,space:true}")
 *     .with("size", DataSize.ofMegabytes(5))
 *     .locale(Locale.US)
 *     .format();
 * // "5 MB"
 * }</pre>
 *
 * @author Jeroen Gremmen
 * @since 0.25.0
 */
public final class DataSizeFormatter extends AbstractSingleTypeParameterFormatter<DataSize>
{
  /**
   * {@inheritDoc}
   * <p>
   * Delegates formatting of the number of bytes to the {@code filesize} formatter.
   */
  @Override
  @Contract(pure = true)
  protected @NotNull Text formatValue(@NotNull ParameterFormatterContext context, @NotNull DataSize dataSize) {
    return context.format(dataSize.toBytes(), long.class, "filesize", null);
  }


  /**
   * {@inheritDoc}
   *
   * @return  formattable type for {@link DataSize}, never {@code null}
   */
  @Override
  protected @NotNull FormattableType getFormattableType() {
    return new FormattableType(DataSize.class);
  }
}
