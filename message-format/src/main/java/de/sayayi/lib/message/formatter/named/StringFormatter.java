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
package de.sayayi.lib.message.formatter.named;

import de.sayayi.lib.message.formatter.AbstractParameterFormatter;
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.formatter.NamedParameterFormatter;
import de.sayayi.lib.message.formatter.ParameterFormatter.ConfigKeyComparator;
import de.sayayi.lib.message.formatter.ParameterFormatter.DefaultFormatter;
import de.sayayi.lib.message.formatter.ParameterFormatter.SizeQueryable;
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.part.parameter.key.ConfigKey.CompareType;
import de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.text.Collator;
import java.util.HashSet;
import java.util.OptionalLong;
import java.util.Set;

import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult.*;
import static java.text.Collator.*;
import static java.util.Arrays.asList;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class StringFormatter extends AbstractParameterFormatter<Object>
    implements SizeQueryable, NamedParameterFormatter, ConfigKeyComparator<Object>, DefaultFormatter
{
  @Override
  public @NotNull String getName() {
    return "string";
  }


  @Override
  @Contract(pure = true)
  public @NotNull Text formatValue(@NotNull FormatterContext context, @NotNull Object value) {
    return noSpaceText(value instanceof char[] ? new String((char[])value) : String.valueOf(value));
  }


  @Override
  public @NotNull OptionalLong size(@NotNull FormatterContext context, @NotNull Object value)
  {
    if (value instanceof char[])
      return OptionalLong.of(((char[])value).length);
    else if (value instanceof CharSequence)
      return OptionalLong.of(((CharSequence)value).length());
    else
      return OptionalLong.empty();
  }


  @Override
  public @NotNull Set<FormattableType> getFormattableTypes()
  {
    return new HashSet<>(asList(
        new FormattableType(CharSequence.class),
        new FormattableType(char[].class)
    ));
  }


  @Override
  public @NotNull MatchResult compareToConfigKey(@NotNull Object value, @NotNull ComparatorContext context)
  {
    final String s = value instanceof char[] ? new String((char[]) value) : String.valueOf(value);

    switch(context.getKeyType())
    {
      case EMPTY:
        return compareToEmptyKey(s, context.getCompareType());

      case BOOL:
        return compareToBoolKey(s, context.getBoolKeyValue());

      case NUMBER:
        return compareToNumberKey(s, context, context.getNumberKeyValue());

      case STRING:
        return compareToStringKey(s, context);
    }

    return MISMATCH;
  }


  private @NotNull MatchResult compareToEmptyKey(String value, @NotNull CompareType compareType)
  {
    return compareType.match(value.length())
        ? TYPELESS_EXACT
        : compareType.match(value.trim().length()) ? TYPELESS_LENIENT : MISMATCH;
  }


  private @NotNull MatchResult compareToBoolKey(String value, boolean bool)
  {
    if ("true".equals(value))
      return bool ? EQUIVALENT : MISMATCH;

    if ("false".equals(value))
      return bool ? MISMATCH : EQUIVALENT;

    return MISMATCH;
  }


  private @NotNull MatchResult compareToNumberKey(String value, @NotNull ComparatorContext context,
                                                  long number)
  {
    try {
      return context.getCompareType().match(new BigDecimal(value).compareTo(BigDecimal.valueOf(number)))
          ? EQUIVALENT : MISMATCH;
    } catch(NumberFormatException ignored) {
    }

    return MISMATCH;
  }


  private @NotNull MatchResult compareToStringKey(String value, @NotNull ComparatorContext context)
  {
    final CompareType compareType = context.getCompareType();
    final Collator collator = Collator.getInstance(context.getLocale());
    final String string = context.getStringKeyValue();

    collator.setDecomposition(CANONICAL_DECOMPOSITION);

    // match exact comparison
    collator.setStrength(IDENTICAL);
    if (compareType.match(collator.compare(value, string)))
      return EXACT;

    // match lenient comparison by ignoring case
    collator.setStrength(PRIMARY);
    if (compareType.match(collator.compare(value, string)))
      return LENIENT;

    return MISMATCH;
  }
}
