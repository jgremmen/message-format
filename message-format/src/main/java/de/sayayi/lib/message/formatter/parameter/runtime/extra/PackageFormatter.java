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
package de.sayayi.lib.message.formatter.parameter.runtime.extra;

import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.parameter.AbstractSingleTypeParameterFormatter;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.NotNull;

import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;


/**
 * Parameter formatter for {@link Package} values.
 * <p>
 * This formatter renders the package using its fully qualified name obtained from {@link Package#getName()}.
 * <p>
 * <b>Examples:</b>
 * <p>
 * This formatter is not part of the {@link de.sayayi.lib.message.formatter.DefaultFormatterService
 * DefaultFormatterService} and must be registered explicitly:
 * <pre>{@code
 * var formatterService = new DefaultFormatterService();
 * formatterService.addFormatter(new PackageFormatter());
 *
 * var messageSupport = MessageSupportFactory.create(formatterService).seal();
 * }</pre>
 * <p>
 * Format the name of a package:
 * <pre>{@code
 * messageSupport
 *     .message("Class %{c,type:c} is located in %{p}")
 *     .with("c", String.class)
 *     .with("p", String.class.getPackage())
 *     .format();
 * // "Class String is located in java.lang"
 * }</pre>
 * <p>
 * Format multiple packages using the list configuration keys:
 * <pre>{@code
 * messageSupport
 *     .message("Scanning %{packages,list-sep-last:' and '}")
 *     .with("packages", List.of(String.class.getPackage(), List.class.getPackage(), Path.class.getPackage()))
 *     .format();
 * // "Scanning java.lang, java.util and java.nio.file"
 * }</pre>
 *
 * @author Jeroen Gremmen
 * @since 0.3.1
 */
public final class PackageFormatter extends AbstractSingleTypeParameterFormatter<Package>
{
  /**
   * {@inheritDoc}
   * <p>
   * Formats the package as its fully qualified name.
   */
  @Override
  public @NotNull Text formatValue(@NotNull ParameterFormatterContext context, @NotNull Package value) {
    return noSpaceText(value.getName());
  }


  /**
   * {@inheritDoc}
   *
   * @return  formattable type for {@link Package}, never {@code null}
   */
  @Override
  protected @NotNull FormattableType getFormattableType() {
    return new FormattableType(Package.class);
  }
}
