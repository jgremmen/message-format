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


  @Contract(value = "null, _ -> fail; _, _ -> param1", pure = true)
  public static @NotNull String validateName(String name, @NotNull String nameDescription)
  {
    if (name == null)
      throw new NullPointerException(nameDescription + " must not be null");
    else if (name.isBlank())
      throw new IllegalArgumentException(nameDescription + " must not be empty");

    return name;
  }


  /**
   * Determines if the specified character is a Unicode space character. A character is considered
   * to be a space character if and only if it is specified to be a space character by the
   * Unicode Standard. This method returns true if the character's general category type is any of
   * the following:
   * <ul>
   * <li> {@code SPACE_SEPARATOR}
   * <li> {@code PARAGRAPH_SEPARATOR}
   * </ul>
   *
   * @param ch  the character to be tested
   *
   * @return  {@code true} if the character is a space character,
   *          {@code false} otherwise
   *
   * @see Character#isSpaceChar(char)
   */
  @Contract(pure = true)
  public static boolean isSpaceChar(char ch) {
    return ((((1 << SPACE_SEPARATOR) | (1 << PARAGRAPH_SEPARATOR)) >> getType((int)ch)) & 1) != 0;
  }


  /**
   * Returns a string without leading and trailing spaces. This method differs from
   * {@link String#trim()} in that it doesn't trim newlines.
   *
   * @param s  string to trim, or {@code null}
   *
   * @return  trimmed string, or {@code null} if {@code s} is {@code null}
   *
   * @see #isSpaceChar(char)
   */
  @Contract(value = "null -> null; !null -> !null", pure = true)
  @SuppressWarnings("DuplicatedCode")
  public static String trimSpaces(String s)
  {
    if (s == null)
      return null;

    final var val = s.toCharArray();
    var endIndex = val.length;
    var startIdx = 0;

    while(startIdx < endIndex && isSpaceChar(val[startIdx]))
      startIdx++;

    while(startIdx < endIndex && isSpaceChar(val[endIndex - 1]))
      endIndex--;

    return startIdx == endIndex
        ? ""
        : startIdx > 0 || endIndex < val.length
            ? new String(val, startIdx, endIndex - startIdx)
            : s;
  }


  /**
   * Returns the trimmed variant of {@code s}. If {@code s = null} an empty string is returned.
   *
   * @param s  string to trim
   *
   * @return  trimmed string, never {@code null}
   *
   * @see #trimSpaces(String)
   */
  @Contract(pure = true)
  public static @NotNull String trimSpacesNotNull(String s) {
    return s == null ? "" : trimSpaces(s);
  }


  /**
   * Tells if a string is empty.
   * <p>
   * A string is considered empty if it has zero length, or it contains spaces only.
   *
   * @param s  string, not {@code null}
   *
   * @return  {@code true} if the string is empty or contains spaces only, {@code false} otherwise
   *
   * @see #isSpaceChar(char)
   */
  @Contract(pure = true)
  @SuppressWarnings("DuplicatedCode")
  public static boolean isTrimmedEmpty(@NotNull String s)
  {
    final var val = s.toCharArray();
    var endIndex = val.length;
    var startIdx = 0;

    while(startIdx < endIndex && isSpaceChar(val[startIdx]))
      startIdx++;

    while(startIdx < endIndex && isSpaceChar(val[endIndex - 1]))
      endIndex--;

    return startIdx == endIndex;
  }


  /**
   * Returns whether string {@code s} is empty or not.
   *
   * @param s  string or {@code null}
   *
   * @return  {@code true} if the string is empty or {@code null}, {@code false} otherwise
   */
  @Contract(value = "null -> true", pure = true)
  public static boolean isEmpty(String s) {
    return s == null || s.isEmpty();
  }


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
