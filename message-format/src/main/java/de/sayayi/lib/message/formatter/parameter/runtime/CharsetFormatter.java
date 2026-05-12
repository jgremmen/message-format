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
package de.sayayi.lib.message.formatter.parameter.runtime;

import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.parameter.AbstractSingleTypeParameterFormatter;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatter.MapKeyComparator;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext;
import de.sayayi.lib.message.part.MapKey.MatchResult;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;

import static de.sayayi.lib.message.formatter.parameter.ParameterFormatter.ClassifierContext.CLASSIFIER_STRING;
import static de.sayayi.lib.message.part.MapKey.CompareType.EQ;
import static de.sayayi.lib.message.part.MapKey.CompareType.NE;
import static de.sayayi.lib.message.part.MapKey.MatchResult.Defined.*;
import static de.sayayi.lib.message.part.MapKey.STRING_TYPE;
import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;


/**
 * Parameter formatter for {@link Charset} values.
 * <p>
 * When no string map key matches, the default map entry is used. If no default is provided,
 * the charset's locale-sensitive display name is used as the formatted result.
 * <p>
 * Map key comparison supports {@code string} keys matched against the charset's canonical name
 * and aliases. Only the {@code EQ} and {@code NE} comparison types are supported:
 * <ul>
 *   <li>
 *     {@code EQ} a canonical name match returns
 *     {@link de.sayayi.lib.message.part.MapKey.MatchResult.Defined#EXACT EXACT}, an alias match
 *     returns {@link de.sayayi.lib.message.part.MapKey.MatchResult.Defined#EQUIVALENT EQUIVALENT}
 *   </li>
 *   <li>
 *     {@code NE} if neither the canonical name nor any alias matches the key,
 *     {@link de.sayayi.lib.message.part.MapKey.MatchResult.Defined#EXACT EXACT} is returned;
 *     if only an alias matches,
 *     {@link de.sayayi.lib.message.part.MapKey.MatchResult.Defined#LENIENT LENIENT} is returned;
 *     a canonical name match results in
 *     {@link de.sayayi.lib.message.part.MapKey.MatchResult.Defined#MISMATCH MISMATCH}
 *   </li>
 * </ul>
 *
 * @author Jeroen Gremmen
 * @since 0.22.1
 */
public final class CharsetFormatter
    extends AbstractSingleTypeParameterFormatter<Charset>
    implements MapKeyComparator<Charset>
{
  /**
   * {@inheritDoc}
   * <p>
   * Adds the {@code charset} and {@link ClassifierContext#CLASSIFIER_STRING string} classifiers.
   */
  @Override
  public boolean updateClassifiers(@NotNull ClassifierContext context, @NotNull Object value)
  {
    context.addClassifier("charset");
    context.addClassifier(CLASSIFIER_STRING);

    return true;
  }


  /**
   * {@inheritDoc}
   * <p>
   * Formats the given charset by first attempting to match its canonical name or aliases against
   * string map keys. If no match is found, the default map entry is consulted. If no default is
   * available either, the charset's locale-sensitive display name is returned.
   */
  @Override
  protected @NotNull Text formatValue(@NotNull ParameterFormatterContext context, @NotNull Charset charset)
  {
    return context
        .getMapMessage(charset, STRING_TYPE, true)
        .map(context::format)
        .orElseGet(() -> noSpaceText(charset.displayName(context.getLocale())));
  }


  /**
   * {@inheritDoc}
   *
   * @return  the {@link Charset} formattable type, never {@code null}
   */
  @Override
  protected @NotNull FormattableType getFormattableType() {
    return new FormattableType(Charset.class);
  }


  /**
   * {@inheritDoc}
   * <p>
   * Compares the charset's canonical name and aliases against the string map key. Only the
   * {@link de.sayayi.lib.message.part.MapKey.CompareType#EQ EQ} and
   * {@link de.sayayi.lib.message.part.MapKey.CompareType#NE NE} comparison types are supported;
   * all other types return {@link de.sayayi.lib.message.part.MapKey.MatchResult.Defined#MISMATCH MISMATCH}.
   */
  @Override
  public @NotNull MatchResult compareToStringKey(@NotNull Charset charset, @NotNull ComparatorContext context)
  {
    final var compareType = context.getCompareType();
    if (compareType != EQ && compareType != NE)
      return MISMATCH;

    final var stringKey = context.getStringKeyValue();
    var matchResult = MISMATCH;

    if (charset.name().equals(stringKey))
      matchResult = EXACT;
    else
    {
      for(var alias: charset.aliases())
        if (alias.equals(stringKey))
        {
          matchResult = EQUIVALENT;
          break;
        }
    }

    if (compareType == EQ)
      return matchResult;

    // NE
    return matchResult == MISMATCH
        ? EXACT
        : matchResult == EQUIVALENT ? LENIENT : MISMATCH;
  }
}
