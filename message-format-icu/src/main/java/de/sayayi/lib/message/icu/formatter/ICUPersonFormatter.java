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
 *   <tr>
 *     <td>{@code given-format}</td>
 *     <td>{@code full}, {@code initial}, {@code none}</td>
 *     <td>{@code full}</td>
 *     <td>Format of the given name: as provided, as an initial or omitted.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code family-format}</td>
 *     <td>{@code full}, {@code initial}, {@code none}</td>
 *     <td>{@code full}</td>
 *     <td>Format of the family name: as provided, as an initial or omitted.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code middle-format}</td>
 *     <td>{@code full}, {@code initial}, {@code none}</td>
 *     <td>{@code initial}</td>
 *     <td>Format of the middle name: as provided, as an initial or omitted.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code prefix-format}</td>
 *     <td>{@code full}, {@code none}</td>
 *     <td>{@code full}</td>
 *     <td>Whether the prefix or title is included.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code suffix-format}</td>
 *     <td>{@code full}, {@code none}</td>
 *     <td>{@code full}</td>
 *     <td>Whether the suffix or generation is included.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code order}</td>
 *     <td>{@code given-first}, {@code surname-first}, {@code sorting}</td>
 *     <td>{@code given-first}</td>
 *     <td>Order of the name parts. With {@code sorting}, the family name comes first and is followed by a comma.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code usage}</td>
 *     <td>{@code referring}, {@code addressing}, {@code monogram}</td>
 *     <td>{@code referring}</td>
 *     <td>Formatting context. {@code monogram} uses the ICU formatter, which ignores the {@code *-format} keys.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code formality}</td>
 *     <td>{@code formal}, {@code informal}</td>
 *     <td>{@code formal}</td>
 *     <td>Formality level. Only used with usage {@code monogram}.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code length}</td>
 *     <td>{@code long}, {@code medium}, {@code short}</td>
 *     <td>{@code medium}</td>
 *     <td>Overall name length. Only used with usage {@code monogram}.</td>
 *   </tr>
 * </table>
 * <p>
 * <b>Examples:</b>
 * <p>
 * A middle name is formatted as an initial by default. The value of the anchor parameter is not used:
 * <pre>{@code
 * messageSupport
 *     .message("Dear %{unused,format:icu-person}")
 *     .with("prefix", "Dr.")
 *     .with("given-name", "John")
 *     .with("middle-name", "Michael")
 *     .with("family-name", "Smith")
 *     .format();
 * // "Dear Dr. John M. Smith"
 * }</pre>
 * <p>
 * Control the format of each name part:
 * <pre>{@code
 * messageSupport
 *     .message("%{unused,format:icu-person,given-format:initial,middle-format:none}")
 *     .with("given-name", "John")
 *     .with("middle-name", "Michael")
 *     .with("family-name", "Smith")
 *     .format();
 * // "J. Smith"
 * }</pre>
 * <p>
 * Format the name for an alphabetical index:
 * <pre>{@code
 * messageSupport
 *     .message("%{unused,format:icu-person,order:sorting,middle-format:full}")
 *     .with("given-name", "James")
 *     .with("middle-name", "Earl")
 *     .with("family-name", "Carter")
 *     .with("suffix", "Jr.")
 *     .format();
 * // "Carter, James Earl Jr."
 * }</pre>
 *
 * @author Jeroen Gremmen
 * @since 0.24.0
 */
public final class ICUPersonFormatter implements NamedParameterFormatter
{
  /** Configuration keys supported by this formatter. */
  private static final Set<String> CONFIG_NAMES = Set.of(
      "given-format", "family-format", "middle-format", "prefix-format", "suffix-format",
      "order", "usage", "formality", "length");


  /**
   * {@inheritDoc}
   *
   * @return  {@code "icu-person"}, never {@code null}
   */
  @Override
  @Contract(pure = true)
  public @NotNull String getName() {
    return "icu-person";
  }


  /**
   * Formats a person name by assembling the name parts obtained from the parameter context.
   * <p>
   * The name parts ({@code given-name}, {@code family-name}, {@code middle-name}, {@code prefix} and {@code suffix})
   * are read from the context parameters. If both {@code given-name} and {@code family-name} are absent, the
   * {@linkplain #formatNull(ParameterFormatterContext) null format} is used.
   * <p>
   * For {@link Usage#MONOGRAM monogram} usage, the ICU {@link PersonNameFormatter} is used to produce locale-specific
   * initials. For all other usages, the name is assembled manually based on the per-part format configuration.
   *
   * @param context  the formatter context providing parameter values and configuration, not {@code null}
   * @param value    the parameter value (unused, name parts are read from named parameters)
   *
   * @return  the formatted person name as text, never {@code null}
   */
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


  /**
   * Formats a person name using the ICU {@link PersonNameFormatter}, which provides locale-sensitive name formatting.
   * This method is used for {@link Usage#MONOGRAM monogram} usage.
   *
   * @param locale      locale used for formatting
   * @param givenName   given name, or {@code null}
   * @param familyName  family name, or {@code null}
   * @param middleName  middle name, or {@code null}
   * @param prefix      prefix (title), or {@code null}
   * @param suffix      suffix (generation), or {@code null}
   * @param order       name display order, not {@code null}
   * @param usage       formatting context, not {@code null}
   * @param formality   formality level, not {@code null}
   * @param length      overall name length, not {@code null}
   *
   * @return  formatted person name, or empty text if the name is empty, never {@code null}
   */
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


  /**
   * Assembles a person name manually based on per-part format settings. Each name part is individually formatted
   * according to its configured format before assembly.
   *
   * @param givenName     given name, or {@code null}
   * @param familyName    family name, or {@code null}
   * @param middleName    middle name, or {@code null}
   * @param prefix        prefix (title), or {@code null}
   * @param suffix        suffix (generation), or {@code null}
   * @param givenFormat   given name format, not {@code null}
   * @param familyFormat  family name format, not {@code null}
   * @param middleFormat  middle name format, not {@code null}
   * @param prefixFormat  prefix format, not {@code null}
   * @param suffixFormat  suffix format, not {@code null}
   * @param order         name display order, not {@code null}
   *
   * @return  formatted person name, or empty text if the name is empty, never {@code null}
   */
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


  /**
   * Assembles name parts in given-first order: prefix, given, middle, family, suffix.
   *
   * @param sb      string builder receiving the assembled name, not {@code null}
   * @param given   formatted given name, or {@code null} to omit it
   * @param middle  formatted middle name, or {@code null} to omit it
   * @param family  formatted family name, or {@code null} to omit it
   * @param prefix  formatted prefix, or {@code null} to omit it
   * @param suffix  formatted suffix, or {@code null} to omit it
   */
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


  /**
   * Assembles name parts in surname-first order: family, prefix, given, middle, suffix.
   * In sorting mode, a comma separates the family name from the remaining parts.
   *
   * @param sb       string builder receiving the assembled name, not {@code null}
   * @param family   formatted family name, or {@code null} to omit it
   * @param given    formatted given name, or {@code null} to omit it
   * @param middle   formatted middle name, or {@code null} to omit it
   * @param prefix   formatted prefix, or {@code null} to omit it
   * @param suffix   formatted suffix, or {@code null} to omit it
   * @param sorting  {@code true} to separate the family name with a comma, {@code false} otherwise
   */
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


  /**
   * {@inheritDoc}
   *
   * @return  unmodifiable set of supported configuration key names, never {@code null}
   */
  @Override
  public @Unmodifiable @NotNull Set<String> getParameterConfigNames() {
    return CONFIG_NAMES;
  }




  /**
   * Display format for name parts that can be shown in full, as an initial or not at all.
   *
   * @see #apply(String)
   *
   * @since 0.24.0
   */
  public enum PartFormat
  {
    /** Show the full name part. */
    FULL,

    /** Show only the first character followed by a period. */
    INITIAL,

    /** Omit the name part entirely. */
    NONE;


    /**
     * Applies this format to the given name part value.
     *
     * @param value  the name part value, or {@code null}
     *
     * @return  the formatted value, or {@code null} if the part should be omitted
     */
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
   * Display format for prefix and suffix parts, which can only be shown in full or not at all.
   *
   * @see #apply(String)
   *
   * @since 0.24.0
   */
  public enum PresenceFormat
  {
    /** Include the part as-is. */
    FULL,

    /** Omit the part entirely. */
    NONE;


    /**
     * Applies this format to the given prefix or suffix value.
     *
     * @param value  the prefix or suffix value, or {@code null}
     *
     * @return  the value unchanged for {@link #FULL}, or {@code null} for {@link #NONE}
     */
    @Contract(pure = true)
    String apply(String value) {
      return this == NONE ? null : value;
    }
  }




  /**
   * Name display order, mapping to the corresponding ICU {@link DisplayOrder} values.
   *
   * @since 0.24.0
   */
  public enum Order
  {
    /** Given name appears before the family name. */
    GIVEN_FIRST(DisplayOrder.FORCE_GIVEN_FIRST),

    /** Family name appears before the given name. */
    SURNAME_FIRST(DisplayOrder.FORCE_SURNAME_FIRST),

    /** Family name first, separated by a comma for sorting purposes. */
    SORTING(DisplayOrder.SORTING);

    /** Corresponding ICU display order. */
    final DisplayOrder displayOrder;


    /**
     * Creates an order constant for the given ICU display order.
     *
     * @param displayOrder  corresponding ICU display order, not {@code null}
     */
    Order(DisplayOrder displayOrder) {
      this.displayOrder = displayOrder;
    }
  }
}
