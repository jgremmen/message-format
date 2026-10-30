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
import de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext;
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.part.TypedValue.NumberValue;
import de.sayayi.lib.message.part.TypedValue.StringValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;

import static de.sayayi.lib.message.part.TextPartFactory.emptyText;
import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;


/**
 * Parameter formatter for {@link MatchResult} values.
 * <p>
 * This formatter extracts and formats a capture group from a regex match result. The {@code matcher} configuration
 * key controls which group is returned.
 * <p>
 * If the match result is a {@link Matcher} without an existing match, the whole input is matched against the
 * pattern as a fallback (without disturbing an already established match, e.g. from a prior {@code find()} call).
 * If no match can be established this way, or the specified group does not exist, empty text is returned.
 * <p>
 * <b>Configuration keys:</b>
 * <table border="1">
 *   <caption>Configuration keys for the match result formatter</caption>
 *   <tr><th>Key</th><th>Values</th><th>Default</th><th>Purpose</th></tr>
 *   <tr>
 *     <td>{@code matcher}</td>
 *     <td>number or string</td>
 *     <td>{@code 0}</td>
 *     <td>
 *       A number selects the capture group with the given index, with {@code 0} being the entire match. A string
 *       selects the named capture group.
 *     </td>
 *   </tr>
 * </table>
 * <p>
 * <b>Examples:</b>
 * <p>
 * This formatter is not part of the {@link de.sayayi.lib.message.formatter.DefaultFormatterService
 * DefaultFormatterService} and must be registered explicitly:
 * <pre>{@code
 * var formatterService = new DefaultFormatterService();
 * formatterService.addFormatter(new MatchResultFormatter());
 *
 * var messageSupport = MessageSupportFactory.create(formatterService).seal();
 * }</pre>
 * <p>
 * Extract capture groups by index or by name:
 * <pre>{@code
 * var matcher = Pattern.compile("(\\w+)@(?<host>[\\w.]+)").matcher("alice@example.com");
 *
 * messageSupport
 *     .message("User %{m,matcher:1} at %{m,matcher:'host'}")
 *     .with("m", matcher)
 *     .format();
 * // "User alice at example.com"
 *
 * messageSupport.message("%{m}").with("m", matcher).format();
 * // "alice@example.com"
 * }</pre>
 * <p>
 * A prior {@code find()} is retained, and a {@link MatchResult} can be used as well:
 * <pre>{@code
 * var matcher = Pattern.compile("\\d+").matcher("Error 404 at line 12");
 *
 * matcher.find();
 *
 * messageSupport.message("Code %{m}").with("m", matcher.toMatchResult()).format();
 * // "Code 404"
 * }</pre>
 * <p>
 * A failed match or an unknown group results in an empty text, which the {@code empty} key can handle:
 * <pre>{@code
 * var matcher = Pattern.compile("\\d+").matcher("no digits");
 *
 * messageSupport.message("%{m,empty:'no match'}").with("m", matcher).format();
 * // "no match"
 * }</pre>
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class MatchResultFormatter extends AbstractSingleTypeParameterFormatter<MatchResult>
{
  /**
   * {@inheritDoc}
   * <p>
   * Extracts a capture group from the matcher based on the {@code matcher} configuration key.
   * Returns empty text if no match can be established, or the group does not exist.
   */
  @Override
  protected @NotNull Text formatValue(@NotNull ParameterFormatterContext context, @NotNull MatchResult matchResult)
  {
    if (matchResult instanceof Matcher matcher)
    {
      try {
        //noinspection ResultOfMethodCallIgnored
        matcher.start();
      } catch(IllegalStateException ex) {
        if (!matcher.matches())
          return emptyText();
      }
    }

    return context
        .getConfigValue("matcher")
        .map(matcherConfigValue -> switch(matcherConfigValue) {
          case NumberValue groupNumberValue -> byGroupNumber(matchResult, groupNumberValue.intValue());
          case StringValue groupNameValue -> byGroupName(matchResult, groupNameValue.stringValue());
          default -> emptyText();
        })
        .orElseGet(() -> byGroupNumber(matchResult, 0));
  }


  /**
   * Returns the text captured by the group with the given number.
   *
   * @param matchResult  match result, not {@code null}
   * @param groupNumber  group number, {@code 0} denoting the entire match
   *
   * @return  captured text, or empty text if the group does not exist, never {@code null}
   */
  private @NotNull Text byGroupNumber(@NotNull MatchResult matchResult, int groupNumber)
  {
    return groupNumber < 0 || groupNumber > matchResult.groupCount()
        ? emptyText()
        : noSpaceText(matchResult.group(groupNumber));
  }


  /**
   * Returns the text captured by the named group.
   *
   * @param matchResult  match result, not {@code null}
   * @param groupName    group name
   *
   * @return  captured text, or empty text if no group with the given name exists, never {@code null}
   */
  private @NotNull Text byGroupName(@NotNull MatchResult matchResult, String groupName)
  {
    try {
      return noSpaceText(matchResult.group(groupName));
    } catch(IllegalArgumentException ex) {
      return emptyText();
    }
  }


  /**
   * {@inheritDoc}
   *
   * @return  formattable type for {@link MatchResult}, never {@code null}
   */
  @Override
  protected @NotNull FormattableType getFormattableType() {
    return new FormattableType(MatchResult.class);
  }


  /**
   * {@inheritDoc}
   *
   * @return  a set containing {@code "matcher"}, never {@code null}
   */
  @Override
  public @Unmodifiable @NotNull Set<String> getParameterConfigNames() {
    return Set.of("matcher");
  }
}
