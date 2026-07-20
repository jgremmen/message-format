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
package de.sayayi.lib.message.icu.formatter;

import com.ibm.icu.text.PersonName.NameField;
import com.ibm.icu.text.PersonNameFormatter;
import com.ibm.icu.text.PersonNameFormatter.DisplayOrder;
import com.ibm.icu.text.PersonNameFormatter.Formality;
import com.ibm.icu.text.PersonNameFormatter.Length;
import com.ibm.icu.text.PersonNameFormatter.Usage;
import com.ibm.icu.text.SimplePersonName;
import de.sayayi.lib.message.formatter.parameter.NamedParameterFormatter;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Locale;
import java.util.Set;

import static de.sayayi.lib.message.part.TextPartFactory.emptyText;
import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;


/**
 * Named parameter formatter that formats person names using the ICU PersonNameFormatter.
 * <p>
 * This formatter is selected by using the name {@code icu-person} in a message parameter:
 * <pre>{@code %{unused,format:icu-person,given-format:initial,family-format:full}}</pre>
 * <p>
 * The formatter reads the name parts from parameters named {@code given-name}, {@code family-name},
 * {@code middle-name}, {@code prefix} and {@code suffix}.
 * <p>
 * <b>Configuration keys:</b>
 * <table border="1">
 *   <caption>Configuration keys for the icu-person formatter</caption>
 *   <tr><th>Key</th><th>Values</th><th>Default</th><th>Purpose</th></tr>
 *   <tr><td>given-format</td><td>full, initial, none</td><td>full</td><td>Given name format</td></tr>
 *   <tr><td>family-format</td><td>full, initial, none</td><td>full</td><td>Family name format</td></tr>
 *   <tr><td>middle-format</td><td>full, initial, none</td><td>initial</td><td>Middle name format</td></tr>
 *   <tr><td>prefix-format</td><td>full, none</td><td>full</td><td>Prefix/title format</td></tr>
 *   <tr><td>suffix-format</td><td>full, none</td><td>full</td><td>Suffix/generation format</td></tr>
 *   <tr><td>order</td><td>given-first, surname-first, sorting</td><td>given-first</td><td>Name ordering</td></tr>
 *   <tr><td>usage</td><td>referring, addressing, monogram</td><td>referring</td><td>Formatting context</td></tr>
 *   <tr><td>formality</td><td>formal, informal</td><td>formal</td><td>Formality level</td></tr>
 *   <tr><td>length</td><td>long, medium, short</td><td>medium</td><td>Overall name length</td></tr>
 * </table>
 *
 * @author Jeroen Gremmen
 * @since 0.24.0
 */
public final class ICUPersonFormatter implements NamedParameterFormatter
{
  private static final Set<String> CONFIG_NAMES = Set.of(
      "given-format", "family-format", "middle-format", "prefix-format", "suffix-format",
      "order", "usage", "formality", "length");


  @Override
  @Contract(pure = true)
  public @NotNull String getName() {
    return "icu-person";
  }


  @Override
  public @NotNull Text format(@NotNull ParameterFormatterContext context, Object value)
  {
    final var givenName = context.getParameterValueAsString("given-name").orElse(null);
    final var familyName = context.getParameterValueAsString("family-name").orElse(null);

    if (givenName == null && familyName == null)
      return formatNull(context);

    final var order = context
        .getConfigValueEnum("order", Order.class)
        .orElse(Order.GIVEN_FIRST);
    final var usage = context
        .getConfigValueEnum("usage", Usage.class)
        .orElse(Usage.REFERRING);

    final var middleName = context.getParameterValueAsString("middle-name").orElse(null);
    final var prefix = context.getParameterValueAsString("prefix").orElse(null);
    final var suffix = context.getParameterValueAsString("suffix").orElse(null);

    if (usage == Usage.MONOGRAM)
    {
      final var length = context
          .getConfigValueEnum("length", Length.class)
          .orElse(Length.MEDIUM);
      final var formality = context
          .getConfigValueEnum("formality", Formality.class)
          .orElse(Formality.FORMAL);

      // use ICU PersonNameFormatter for monogram usage (locale-specific initials)
      return formatWithICU(context.getLocale(), givenName, familyName, middleName, prefix, suffix, order, usage,
          formality, length);
    }
    else
    {
      final var givenFormat = context
          .getConfigValueEnum("given-format", PartFormat.class)
          .orElse(PartFormat.FULL);
      final var familyFormat = context
          .getConfigValueEnum("family-format", PartFormat.class)
          .orElse(PartFormat.FULL);
      final var middleFormat = context
          .getConfigValueEnum("middle-format", PartFormat.class)
          .orElse(PartFormat.INITIAL);
      final var prefixFormat = context
          .getConfigValueEnum("prefix-format", PresenceFormat.class)
          .orElse(PresenceFormat.FULL);
      final var suffixFormat = context
          .getConfigValueEnum("suffix-format", PresenceFormat.class)
          .orElse(PresenceFormat.FULL);

      // manual assembly for per-part format control
      return formatManually(givenName, familyName, middleName, prefix, suffix, givenFormat, familyFormat,
          middleFormat, prefixFormat, suffixFormat, order);
    }
  }


  private static @NotNull Text formatWithICU(Locale locale, String givenName, String familyName, String middleName,
                                             String prefix, String suffix, Order order, Usage usage,
                                             Formality formality, Length length)
  {
    final var nameBuilder = SimplePersonName
        .builder()
        .setLocale(locale);

    if (givenName != null)
      nameBuilder.addField(NameField.GIVEN, null, givenName);

    if (familyName != null)
      nameBuilder.addField(NameField.SURNAME, null, familyName);

    if (middleName != null)
      nameBuilder.addField(NameField.GIVEN2, null, middleName);

    if (prefix != null)
      nameBuilder.addField(NameField.TITLE, null, prefix);

    if (suffix != null)
      nameBuilder.addField(NameField.GENERATION, null, suffix);

    final var result = PersonNameFormatter
        .builder()
        .setLocale(locale)
        .setLength(length)
        .setUsage(usage)
        .setFormality(formality)
        .setDisplayOrder(order.displayOrder)
        .build()
        .formatToString(nameBuilder.build());

    return result.isEmpty() ? emptyText() : noSpaceText(result);
  }


  private static @NotNull Text formatManually(String givenName, String familyName, String middleName, String prefix,
                                              String suffix, PartFormat givenFormat, PartFormat familyFormat,
                                              PartFormat middleFormat, PresenceFormat prefixFormat,
                                              PresenceFormat suffixFormat, Order order)
  {
    final var givenPart = givenFormat.apply(givenName);
    final var familyPart = familyFormat.apply(familyName);
    final var middlePart = middleFormat.apply(middleName);
    final var prefixPart = prefixFormat.apply(prefix);
    final var suffixPart = suffixFormat.apply(suffix);

    final var sb = new StringBuilder();

    if (order == Order.SURNAME_FIRST || order == Order.SORTING)
      assembleSurnameFirst(sb, familyPart, givenPart, middlePart, prefixPart, suffixPart,
          order == Order.SORTING);
    else
      assembleGivenFirst(sb, givenPart, middlePart, familyPart, prefixPart, suffixPart);

    final var result = sb.toString().trim();

    return result.isEmpty() ? emptyText() : noSpaceText(result);
  }


  private static void assembleGivenFirst(@NotNull StringBuilder sb, String given, String middle, String family,
                                         String prefix, String suffix)
  {
    if (prefix != null)
      sb.append(prefix).append(' ');
    if (given != null)
      sb.append(given).append(' ');
    if (middle != null)
      sb.append(middle).append(' ');
    if (family != null)
      sb.append(family);
    if (suffix != null)
      sb.append(' ').append(suffix);
  }


  private static void assembleSurnameFirst(@NotNull StringBuilder sb, String family, String given, String middle,
                                           String prefix, String suffix, boolean sorting)
  {
    if (family != null)
    {
      sb.append(family);
      if (given != null || middle != null || prefix != null)
        sb.append(sorting ? ", " : " ");
    }
    if (prefix != null)
      sb.append(prefix).append(' ');
    if (given != null)
      sb.append(given);
    if (middle != null)
      sb.append(' ').append(middle);
    if (suffix != null)
      sb.append(' ').append(suffix);
  }


  @Override
  public @Unmodifiable @NotNull Set<String> getParameterConfigNames() {
    return CONFIG_NAMES;
  }




  /**
   * Format for name parts that support full, initial and none.
   */
  public enum PartFormat
  {
    FULL, INITIAL, NONE;


    @Contract(pure = true)
    String apply(String value)
    {
      if (value == null || this == NONE)
        return null;
      if (this == INITIAL)
        return value.isEmpty() ? null : value.charAt(0) + ".";
      return value;
    }
  }




  /**
   * Format for prefix/suffix parts that only support full and none.
   */
  public enum PresenceFormat
  {
    FULL, NONE;


    @Contract(pure = true)
    String apply(String value) {
      return this == NONE ? null : value;
    }
  }




  /**
   * Name display order.
   */
  public enum Order
  {
    GIVEN_FIRST(DisplayOrder.FORCE_GIVEN_FIRST),
    SURNAME_FIRST(DisplayOrder.FORCE_SURNAME_FIRST),
    SORTING(DisplayOrder.SORTING);

    final DisplayOrder displayOrder;


    Order(DisplayOrder displayOrder) {
      this.displayOrder = displayOrder;
    }
  }
}
