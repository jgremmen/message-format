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
package de.sayayi.lib.message.formatter.parameter.named;

import de.sayayi.lib.message.MessageSupport.MessageAccessor;
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.parameter.NamedParameterFormatter;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatter.DefaultFormatter;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatter.SizeQueryable;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext;
import de.sayayi.lib.message.part.MapKey;
import de.sayayi.lib.message.part.MapKey.MatchResult;
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.part.TypedValue.BoolValue;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.math.BigDecimal;
import java.text.Collator;
import java.util.OptionalLong;
import java.util.Set;

import static de.sayayi.lib.message.formatter.parameter.ParameterFormatter.ClassifierContext.CLASSIFIER_STRING;
import static de.sayayi.lib.message.part.MapKey.CompareType.EQ;
import static de.sayayi.lib.message.part.MapKey.MatchResult.Defined.*;
import static de.sayayi.lib.message.part.MapKey.MatchResult.forEmptyKey;
import static de.sayayi.lib.message.part.MapKey.Type.*;
import static de.sayayi.lib.message.part.MapKey.Type.EMPTY;
import static de.sayayi.lib.message.part.MapKey.Type.NULL;
import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static java.text.Collator.*;
import static java.util.Objects.toIdentityString;


/**
 * Named parameter formatter that converts a parameter value to its string representation.
 * <p>
 * This formatter is selected by using the name {@code string} in a message parameter, e.g.
 * {@code %{myParam,format:string}}.
 * It also serves as the default formatter for any value type that has no specific formatter registered.
 * <p>
 * It handles the following input types:
 * <ul>
 *   <li>{@link CharSequence} values (including {@link String}) are used directly</li>
 *   <li>{@code char[]} arrays are converted to a string</li>
 *   <li>All other non-null values are converted using {@link String#valueOf(Object)}</li>
 *   <li>{@code null} values are handled separately using the null map key</li>
 * </ul>
 * <p>
 * The resulting string value can be mapped to custom text using map keys of type {@code string}, {@code empty} and
 * {@code null} in the parameter configuration. If no mapping matches, the string value itself is returned as text.
 * <p>
 * This formatter also supports the {@code ignore-default-tostring} configuration option. When enabled, values whose
 * {@code toString()} output matches the default {@link Object#toString()} identity string (or lambda class names)
 * are treated as empty strings instead.
 * <p>
 * As a {@link SizeQueryable} formatter, it can report the length of {@link CharSequence} and {@code char[]} values.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class StringFormatter implements SizeQueryable, NamedParameterFormatter, DefaultFormatter
{
  private static final Set<MapKey.Type> STRING_KEY_TYPES = Set.of(EMPTY, NULL, STRING);


  /**
   * {@inheritDoc}
   *
   * @return  {@code "string"}, never {@code null}
   */
  @Override
  public @NotNull String getName() {
    return "string";
  }


  @Override
  public boolean updateClassifiers(@NotNull ClassifierContext context, @NotNull Object value)
  {
    context.addClassifier(CLASSIFIER_STRING);

    return true;
  }


  /**
   * {@inheritDoc}
   * <p>
   * Converts the given {@code value} to a string and matches it against the parameter map configuration. If the value
   * is {@code null}, the null map key is used. If no map key matches, the string value itself is returned.
   */
  @Override
  public @NotNull Text format(@NotNull ParameterFormatterContext context, Object value)
  {
    if (value == null)
      return formatNull(context);

    final var string = valueAsString(context.getMessageAccessor(), value);

    return context
        .getMapMessage(string, STRING_KEY_TYPES)
        .map(context::format)
        .orElseGet(() -> noSpaceText(string));
  }


  @Contract(pure = true)
  private @NotNull String valueAsString(@NotNull MessageAccessor messageAccessor, @NotNull Object value)
  {
    var string = asString(value);

    if (!(value instanceof CharSequence) &&
        !(value instanceof char[]) &&
        messageAccessor.getDefaultConfig("ignore-default-tostring") instanceof BoolValue boolValue &&
        boolValue.booleanValue() &&
        isDefaultToString(value))
      string = "";

    return string;
  }


  @Contract(pure = true)
  @SuppressWarnings("RedundantIfStatement")
  private boolean isDefaultToString(@NotNull Object value)
  {
    if (toIdentityString(value).equals(value.toString()))
      return true;

    final var fqClassName = value.getClass().getName();
    if (fqClassName.contains("$$Lambda$"))
      return true;

    return false;
  }


  /**
   * {@inheritDoc}
   * <p>
   * Returns the length of {@link CharSequence} and {@code char[]} values. For all other types, an empty result
   * is returned.
   */
  @Override
  public @NotNull OptionalLong size(@NotNull ParameterFormatterContext context, @NotNull Object value)
  {
    return switch(value) {
      case char[] chars -> OptionalLong.of(chars.length);
      case CharSequence charSequence -> OptionalLong.of(charSequence.length());

      default -> OptionalLong.empty();
    };
  }


  /**
   * {@inheritDoc}
   *
   * @return  a set containing {@link CharSequence} and {@code char[]} formattable types, never {@code null}
   */
  @Override
  public @NotNull Set<FormattableType> getFormattableTypes()
  {
    return Set.of(
        new FormattableType(CharSequence.class),
        new FormattableType(char[].class));
  }


  /**
   * {@inheritDoc}
   *
   * @return  a set containing {@code "ignore-default-tostring"}, never {@code null}
   */
  @Override
  public @Unmodifiable @NotNull Set<String> getParameterConfigNames() {
    return Set.of("ignore-default-tostring");
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull MatchResult compareToEmptyKey(Object value, @NotNull ComparatorContext context)
  {
    final var compareType = context.getCompareType();

    if (value == null)
      return forEmptyKey(compareType, true);

    if (value instanceof char[])
      return forEmptyKey(compareType, ((char[])value).length == 0);

    var string = String.valueOf(value);
    if (string.isEmpty())
      return forEmptyKey(compareType, true);

    if (string.trim().isEmpty() && compareType == EQ)
      return () -> Defined.EMPTY.value() - 1;

    return forEmptyKey(compareType, false);
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull MatchResult compareToBoolKey(@NotNull Object value, @NotNull ComparatorContext context)
  {
    if (context.getCompareType().match(0))
    {
      final var string = asString(value);
      final var bool = context.getBoolKeyValue();

      if (("true".equals(string) && bool) ||
          ("false".equals(string) && !bool))
        return EQUIVALENT;

      if (("true".equalsIgnoreCase(string) && bool) ||
          ("false".equalsIgnoreCase(string) && !bool))
        return LENIENT;
    }

    return MISMATCH;
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull MatchResult compareToNumberKey(@NotNull Object value, @NotNull ComparatorContext context)
  {
    try {
      final var cmp = new BigDecimal(asString(value)).compareTo(BigDecimal.valueOf(context.getNumberKeyValue()));

      if (context.getCompareType().match(cmp))
        return EQUIVALENT;
    } catch(NumberFormatException ignored) {
    }

    return MISMATCH;
  }


  /** {@inheritDoc} */
  @Override
  public @NotNull MatchResult compareToStringKey(@NotNull Object value, @NotNull ComparatorContext context)
  {
    final var collator = Collator.getInstance(context.getLocale());

    collator.setDecomposition(CANONICAL_DECOMPOSITION);

    final var compareType = context.getCompareType();
    final var stringKeyValue = context.getStringKeyValue();
    final var string = asString(value);

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
