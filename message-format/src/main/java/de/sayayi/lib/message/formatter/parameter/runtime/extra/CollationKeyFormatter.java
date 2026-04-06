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
import de.sayayi.lib.message.formatter.parameter.AbstractSingleTypeParameterFormatter;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatter.MapKeyComparator;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatter.SizeQueryable;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext;
import de.sayayi.lib.message.part.MapKey.MatchResult;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.text.CollationKey;
import java.util.OptionalLong;

import static de.sayayi.lib.message.part.MapKey.MatchResult.forEmptyKey;
import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;


/**
 * Parameter formatter for {@link CollationKey} values.
 * <p>
 * This formatter renders the collation key using its source string obtained from {@link CollationKey#getSourceString()}.
 * <p>
 * As a {@link SizeQueryable} formatter, it reports the length of the source string.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class CollationKeyFormatter extends AbstractSingleTypeParameterFormatter<CollationKey>
    implements SizeQueryable, MapKeyComparator<CollationKey>
{
  /**
   * {@inheritDoc}
   * <p>
   * Formats the collation key as its source string.
   */
  @Override
  @Contract(pure = true)
  public @NotNull Text formatValue(@NotNull ParameterFormatterContext context, @NotNull CollationKey collationKey) {
    return noSpaceText(collationKey.getSourceString());
  }


  /**
   * {@inheritDoc}
   * <p>
   * Returns the length of the source string.
   */
  @Override
  public @NotNull OptionalLong size(@NotNull ParameterFormatterContext context, @NotNull Object value) {
    return OptionalLong.of(((CollationKey)value).getSourceString().length());
  }


  /**
   * {@inheritDoc}
   *
   * @return  formattable type for {@link CollationKey}, never {@code null}
   */
  @Override
  protected @NotNull FormattableType getFormattableType() {
    return new FormattableType(CollationKey.class);
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull MatchResult compareToEmptyKey(CollationKey value, @NotNull ComparatorContext context) {
    return forEmptyKey(context.getCompareType(), value == null || value.getSourceString().isEmpty());
  }
}
