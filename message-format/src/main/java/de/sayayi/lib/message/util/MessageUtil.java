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
package de.sayayi.lib.message.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import static java.lang.Character.*;


/**
 * This class contains various methods related to messages.
 *
 * @author Jeroen Gremmen
 * @since 0.21.0
 */
public final class MessageUtil
{
  private MessageUtil() {}


  /**
   * Check whether {@code name} is a valid kebab-case name.
   * A valid kebab-case name must satisfy the following conditions:
   * <ul>
   *   <li> It must not be empty.
   *   <li> It must start with a lowercase letter.
   *   <li> It must not end with a hyphen ('-').
   *   <li> It must not contain consecutive hyphens ('--').
   *   <li> It must only contain lowercase letters, digits, and hyphens ('-').
   * </ul>
   *
   * @param name  the name to check, not {@code null}
   *
   * @return  {@code true} if {@code name} is a valid kebab-case name,
   *          {@code false} otherwise
   */
  @Contract(pure = true)
  public static boolean isKebabCaseName(@NotNull String name)
  {
    final var length = name.length();

    if (length == 0 || !isLowerCase(name.codePointAt(0)) || name.charAt(length - 1) == '-')
      return false;

    for(int idx = 0, cp; idx < length; idx++)
      if ((cp = name.codePointAt(idx)) == '-')
      {
        // we already checked that '-' cannot be the last character of the name
        if (name.charAt(idx + 1) == '-')
          return false;
      }
      else if (!isLowerCase(cp) && !isDigit(cp))
        return false;

    return true;
  }


  /**
   * Check whether {@code name} is a valid lower camel-case name.
   * A valid lower camel case name must satisfy the following conditions:
   * <ul>
   *   <li> It must not be empty.
   *   <li> It must start with a lowercase letter.
   *   <li> It must only contain letters and digits.
   * </ul>
   *
   * @param name  the name to check, not {@code null}
   *
   * @return  {@code true} if {@code name} is a valid lower camel-case name,
   *          {@code false} otherwise
   */
  @Contract(pure = true)
  public static boolean isLowerCamelCaseName(@NotNull String name)
  {
    final var length = name.length();

    if (length == 0 || !isLowerCase(name.codePointAt(0)))
      return false;

    for(var idx = 1; idx < length; idx++)
    {
      final var cp = name.codePointAt(idx);

      if (!isLetter(cp) && !isDigit(cp))
        return false;
    }

    return true;
  }


  /**
   * Check whether {@code name} is either a valid kebab-case or lower camel-case name.
   * This method combines the logic of {@link #isKebabCaseName(String)} and
   * {@link #isLowerCamelCaseName(String)} in a single pass for optimal performance.
   * <p>
   * A valid kebab-case name must satisfy the following conditions:
   * <ul>
   *   <li> It must not be empty.
   *   <li> It must start with a lowercase letter.
   *   <li> It must not end with a hyphen ('-').
   *   <li> It must not contain consecutive hyphens ('--').
   *   <li> It must only contain lowercase letters, digits, and hyphens ('-').
   * </ul>
   * <p>
   * A valid lower camel case name must satisfy the following conditions:
   * <ul>
   *   <li> It must not be empty.
   *   <li> It must start with a lowercase letter.
   *   <li> It must only contain letters and digits.
   * </ul>
   *
   * @param name  the name to check, not {@code null}
   *
   * @return  {@code true} if {@code name} is either a valid kebab-case or lower camel-case name,
   *          {@code false} otherwise
   */
  @Contract(pure = true)
  public static boolean isKebabOrLowerCamelCaseName(@NotNull String name)
  {
    final var length = name.length();

    if (length == 0 || !isLowerCase(name.codePointAt(0)))
      return false;

    var hasHyphen = false;
    var hasUppercase = false;

    for(int idx = 1, cp; idx < length; idx++)
    {
      if ((cp = name.codePointAt(idx)) == '-')
      {
        // If we've seen uppercase before it's camel case in which hyphens are not allowed
        if (hasUppercase)
          return false;

        hasHyphen = true;

        // Kebab-case cannot end with hyphen and cannot have consecutive hyphens
        if (idx == length - 1 || name.charAt(idx + 1) == '-')
          return false;
      }
      else if (!isLowerCase(cp) && !isDigit(cp))
      {
        if (isLetter(cp))
        {
          // If we've seen a hyphen before it's kebab case in which uppercase/titlecase letters are not allowed
          if (hasHyphen)
            return false;

          hasUppercase = true;
        }
        else
          return false;
      }
    }

    return true;
  }
}
