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
package de.sayayi.lib.message.formatter.runtime.extra;

import de.sayayi.lib.message.formatter.AbstractSingleTypeParameterFormatter;
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.part.parameter.value.ConfigValueNumber;
import de.sayayi.lib.message.part.parameter.value.ConfigValueString;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;

import static de.sayayi.lib.message.part.TextPartFactory.emptyText;
import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class MatcherFormatter extends AbstractSingleTypeParameterFormatter<Matcher>
{
  @Override
  protected @NotNull Text formatValue(@NotNull FormatterContext context, @NotNull Matcher matcher)
  {
    if (matcher.matches())
    {
      var matcherConfig = context.getConfigValue("matcher");
      if (matcherConfig.isEmpty())
        return formatValue_groupNumber(matcher, 0);

      var cv = matcherConfig.get();

      switch(cv.getType())
      {
        case NUMBER:
          return formatValue_groupNumber(matcher, ((ConfigValueNumber)cv).intValue());

        case STRING:
          return formatValue_groupName(matcher, ((ConfigValueString)cv).stringValue());
      }
    }

    return emptyText();
  }


  private @NotNull Text formatValue_groupNumber(@NotNull Matcher matcher, int groupNumber)
  {
    return groupNumber < 0 || groupNumber > matcher.groupCount()
        ? emptyText()
        : noSpaceText(matcher.group(groupNumber));
  }


  private @NotNull Text formatValue_groupName(@NotNull Matcher matcher, String groupName)
  {
    try {
      return noSpaceText(matcher.group(groupName));
    } catch(IllegalArgumentException ex) {
      return emptyText();
    }
  }


  @Override
  public @NotNull FormattableType getFormattableType() {
    return new FormattableType(Matcher.class);
  }
}
