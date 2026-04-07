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
 * key controls which group is returned:
 * <ul>
 *   <li>A number value &ndash; returns the capture group at the given index (0 for the entire match)</li>
 *   <li>A string value &ndash; returns the named capture group</li>
 *   <li>Absent &ndash; returns the entire match (group 0)</li>
 * </ul>
 * <p>
 * If the match result is a {@link Matcher} that does not match, or the specified group does not exist, empty text
 * is returned.
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
   * Returns empty text if the matcher does not match or the group does not exist.
   */
  @Override
  protected @NotNull Text formatValue(@NotNull ParameterFormatterContext context, @NotNull MatchResult matchResult)
  {
    if (matchResult instanceof Matcher matcher && !matcher.matches())
      return emptyText();

    return context
        .getConfigValue("matcher")
        .map(matcherConfigValue -> switch(matcherConfigValue) {
          case NumberValue groupNumberValue -> byGroupNumber(matchResult, groupNumberValue.intValue());
          case StringValue groupNameValue -> byGroupName(matchResult, groupNameValue.stringValue());
          default -> emptyText();
        })
        .orElseGet(() -> byGroupNumber(matchResult, 0));
  }


  private @NotNull Text byGroupNumber(@NotNull MatchResult matchResult, int groupNumber)
  {
    return groupNumber < 0 || groupNumber > matchResult.groupCount()
        ? emptyText()
        : noSpaceText(matchResult.group(groupNumber));
  }


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
