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

import de.sayayi.lib.message.MessageSupport.MessageAccessor;
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.formatter.NamedParameterFormatter;
import de.sayayi.lib.message.formatter.ParameterFormatter.DefaultFormatter;
import de.sayayi.lib.message.formatter.ParameterFormatter.SizeQueryable;
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.part.parameter.key.ConfigKey;
import de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult;
import de.sayayi.lib.message.part.parameter.value.ConfigValueBool;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.math.BigDecimal;
import java.text.Collator;
import java.util.OptionalLong;
import java.util.Set;

import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.CompareType.EQ;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult.Defined.*;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult.forEmptyKey;
import static de.sayayi.lib.message.part.parameter.value.ConfigValue.Type.BOOL;
import static java.lang.Integer.toHexString;
import static java.text.Collator.*;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class StringFormatter implements SizeQueryable, NamedParameterFormatter, DefaultFormatter
{
  private static final Set<ConfigKey.Type> STRING_KEY_TYPES = Set.of(
      ConfigKey.Type.EMPTY,
      ConfigKey.Type.NULL,
      ConfigKey.Type.STRING
  );


  @Override
  public @NotNull String getName() {
    return "string";
  }


  @Override
  public @NotNull Text format(@NotNull FormatterContext context, Object value)
  {
    if (value == null)
      return formatNull(context);

    var string = valueAsString(context.getMessageAccessor(), value);

    return context
        .getConfigMapMessage(string, STRING_KEY_TYPES)
        .map(context::format)
        .orElseGet(() -> noSpaceText(string));
  }


  @Contract(pure = true)
  private @NotNull String valueAsString(@NotNull MessageAccessor messageAccessor, @NotNull Object value)
  {
    var string = asString(value);

    if (!(value instanceof CharSequence) && !(value instanceof char[]))
    {
      var cv = messageAccessor.getDefaultParameterConfig("ignore-default-tostring");

      if (cv != null && cv.getType() == BOOL && ((ConfigValueBool)cv).booleanValue() && isDefaultToString(value))
        string = "";
    }

    return string;
  }


  @Contract(pure = true)
  @SuppressWarnings("RedundantIfStatement")
  private boolean isDefaultToString(@NotNull Object value)
  {
    var fqClassName = value.getClass().getName();

    if ((fqClassName + '@' + toHexString(value.hashCode())).equals(value.toString()))
      return true;

    if (fqClassName.contains("$$Lambda$"))
      return true;

    return false;
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
    return Set.of(
        new FormattableType(CharSequence.class),
        new FormattableType(char[].class));
  }


  @Override
  public @Unmodifiable @NotNull Set<String> getParameterConfigNames() {
    return Set.of("ignore-default-tostring");
  }


  @Override
  public @NotNull MatchResult compareToEmptyKey(Object value, @NotNull ComparatorContext context)
  {
    var compareType = context.getCompareType();

    if (value == null)
      return forEmptyKey(compareType, true);

    if (value instanceof char[])
      return forEmptyKey(compareType, ((char[])value).length == 0);

    var string = String.valueOf(value);
    if (string.isEmpty())
      return forEmptyKey(compareType, true);

    if (string.trim().isEmpty() && compareType == EQ)
      return () -> EMPTY.value() - 1;

    return forEmptyKey(compareType, false);
  }


  @Override
  public @NotNull MatchResult compareToBoolKey(@NotNull Object value, @NotNull ComparatorContext context)
  {
    if (context.getCompareType().match(0))
    {
      var string = asString(value);
      var bool = context.getBoolKeyValue();

      if (("true".equals(string) && bool) ||
          ("false".equals(string) && !bool))
        return EQUIVALENT;

      if (("true".equalsIgnoreCase(string) && bool) ||
          ("false".equalsIgnoreCase(string) && !bool))
        return LENIENT;
    }

    return MISMATCH;
  }


  @Override
  public @NotNull MatchResult compareToNumberKey(@NotNull Object value, @NotNull ComparatorContext context)
  {
    try {
      var cmp = new BigDecimal(asString(value)).compareTo(BigDecimal.valueOf(context.getNumberKeyValue()));

      if (context.getCompareType().match(cmp))
        return EQUIVALENT;
    } catch(NumberFormatException ignored) {
    }

    return MISMATCH;
  }


  @Override
  public @NotNull MatchResult compareToStringKey(@NotNull Object value, @NotNull ComparatorContext context)
  {
    var collator = Collator.getInstance(context.getLocale());

    collator.setDecomposition(CANONICAL_DECOMPOSITION);

    var compareType = context.getCompareType();
    var stringKeyValue = context.getStringKeyValue();
    var string = asString(value);

    // match exact comparison
    collator.setStrength(IDENTICAL);
    if (compareType.match(collator.compare(string, stringKeyValue)))
      return EXACT;

    // match lenient comparison by ignoring case
    collator.setStrength(PRIMARY);
    if (compareType.match(collator.compare(string, stringKeyValue)))
      return LENIENT;

    return MISMATCH;
  }


  @Contract(pure = true)
  private static String asString(@NotNull Object value) {
    return value instanceof char[] ? new String((char[])value) : String.valueOf(value);
  }
}
