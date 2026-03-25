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

import de.sayayi.lib.message.FormatStringSerializer.Context;
import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.internal.pack.PackSupport;
import de.sayayi.lib.pack.PackInputStream;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static de.sayayi.lib.message.internal.pack.PackSupport.PACK_CONFIG;
import static java.lang.Character.*;
import static java.util.Objects.requireNonNull;


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
   * Validates that the specified name is not {@code null} and not empty.
   *
   * @param name             the name to validate, may be {@code null}
   * @param nameDescription  the name description to use in exception messages
   *
   * @return  the validated name, never {@code null} or empty
   */
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
   * Returns a string without leading and trailing spaces and with consecutive spaces collapsed
   * into a single space character ({@code ' '}). This method differs from {@link String#trim()}
   * in that it doesn't trim newlines.
   *
   * @param s  string to normalize, or {@code null}
   *
   * @return  normalized string, or {@code null} if {@code s} is {@code null}
   *
   * @see #isSpaceChar(char)
   * @see #trimSpaces(String)
   */
  @Contract(value = "null -> null; !null -> !null", pure = true)
  @SuppressWarnings("DuplicatedCode")
  public static String trimAndNormalizeSpaces(String s)
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

    if (startIdx == endIndex)
      return "";

    final var len = endIndex - startIdx;
    final var result = new char[len];
    var resultLen = 0;
    var lastWasSpace = false;

    for(var i = startIdx; i < endIndex; i++)
    {
      if (isSpaceChar(val[i]))
      {
        if (!lastWasSpace)
        {
          result[resultLen++] = ' ';
          lastWasSpace = true;
        }
      }
      else
      {
        result[resultLen++] = val[i];
        lastWasSpace = false;
      }
    }

    if (resultLen == len)
      return startIdx == 0 && endIndex == val.length ? s : new String(val, startIdx, len);

    return new String(result, 0, resultLen);
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
        // If we've seen uppercase before, it's camel case in which hyphens are not allowed
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
          // If we've seen a hyphen before, it's kebab case in which uppercase/titlecase letters are not allowed
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


  /**
   * Check whether {@code name} is a valid name as defined by the message format lexer grammar.
   * <p>
   * A valid name must satisfy the following conditions:
   * <ul>
   *   <li> It must not be empty.
   *   <li> It must start with a Unicode letter (category {@code \p{L}}).
   *   <li> After the first letter, it may contain zero or more Unicode letters or numbers
   *        (categories {@code \p{L}} or {@code \p{N}}).
   *   <li> Optionally followed by one or more groups, each consisting of a single underscore
   *        ({@code '_'}) or hyphen ({@code '-'}) followed by one or more Unicode letters or
   *        numbers.
   * </ul>
   *
   * @param name  the name to check, not {@code null}
   *
   * @return  {@code true} if {@code name} is a valid name, {@code false} otherwise
   */
  @Contract(pure = true)
  public static boolean isName(@NotNull String name)
  {
    final var length = name.length();
    if (length == 0)
      return false;

    // Name must start with NameStartChar: Unicode letter (\p{L})
    var cp = name.codePointAt(0);
    if (!isLetter(cp))
      return false;

    var idx = charCount(cp);

    // Zero or more NameChar: Unicode letter (\p{L}) or Unicode number (\p{N})
    while(idx < length && isNameChar(cp = name.codePointAt(idx)))
      idx += charCount(cp);

    // Zero or more groups of [_-] followed by one or more NameChar
    while(idx < length)
    {
      cp = name.codePointAt(idx++);
      if (cp != '_' && cp != '-')
        return false;

      if (idx >= length || !isNameChar(cp = name.codePointAt(idx)))
        return false;

      do {
        idx += charCount(cp);
      } while(idx < length && isNameChar(cp = name.codePointAt(idx)));
    }

    return true;
  }


  @Contract(pure = true)
  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private static boolean isNameChar(int cp)
  {
    final var type = getType(cp);
    return isLetter(cp) || type == DECIMAL_DIGIT_NUMBER || type == LETTER_NUMBER || type == OTHER_NUMBER;
  }


  /**
   * Serializes a string into the format string representation by appending characters to the
   * context's {@linkplain Context#textJoiner() text joiner}, applying the following escaping rules:
   * <ul>
   *   <li>The active {@linkplain Context#inStringWithQuote() string quote character} is backslash-escaped.</li>
   *   <li>A {@code %} character followed by <code>{</code>, <code>[</code> or <code>(</code> is
   *       backslash-escaped to prevent it from being interpreted as a parameter reference.</li>
   *   <li>ISO control characters and characters that cannot be
   *       {@linkplain Context#canEncode(char) encoded} by the context's charset are written as
   *       Unicode escape sequences (e.g. <code>&#92;u00e9</code>).</li>
   *   <li>All other characters are appended as-is.</li>
   * </ul>
   *
   * @param context  the serialization context providing charset encoding, text joiner and
   *                 string quoting information, not {@code null}
   * @param string   the string to serialize, not {@code null}
   *
   * @see #serializeQuotedString(Context, String)
   */
  public static void serializeString(@NotNull Context context, @NotNull String string)
  {
    final var stringCharArray = string.toCharArray();
    final var textJoiner = context.textJoiner();

    for(int i = 0, l = stringCharArray.length; i < l; i++)
    {
      final var ch = stringCharArray[i];

      if (Character.valueOf(ch).equals(context.inStringWithQuote()))
        textJoiner.add('\\').add(ch);
      else if (ch == '%' && i + 1 < l && "{[(".indexOf(stringCharArray[i + 1]) >= 0)
        textJoiner.addNoSpace("\\%");
      else if (!isISOControl(ch) && context.canEncode(ch))
        textJoiner.add(ch);
      else
        textJoiner.addNoSpace(String.format("\\u%04x", (int)ch));
    }
  }


  /**
   * Serializes a string as a quoted string into the format string representation. The quote
   * character is chosen automatically: if the string contains a single quote ({@code '}), a
   * double quote ({@code "}) is used; otherwise a single quote is used.
   * <p>
   * The opening and closing quote characters are appended to the context's
   * {@linkplain Context#textJoiner() text joiner}, and the string content between the quotes is
   * serialized using {@link #serializeString(Context, String)} with the chosen quote set as the
   * active {@linkplain Context#withStringQuote(char) string quote}.
   *
   * @param context  the serialization context providing charset encoding, text joiner and
   *                 string quoting information, not {@code null}
   * @param string   the string to serialize as a quoted string, not {@code null}
   *
   * @see #serializeString(Context, String)
   */
  public static void serializeQuotedString(@NotNull Context context, @NotNull String string)
  {
    final var quote = string.contains("'") ? '"' : '\'';

    context.textJoiner().add(quote);
    serializeString(context.withStringQuote(quote), string);
    context.textJoiner().add(quote);
  }


  /**
   * Import messages and templates from a message format pack file. The {@code packStream} is
   * validated and all entries are iterated. Each message is passed to the optional
   * {@code messageConsumer} and each template to the optional {@code templateConsumer}.
   * <p>
   * The {@code packStream} is closed when this method returns, regardless of whether the
   * import was successful or not.
   *
   * @param packStream        pack input stream, not {@code null}
   * @param messageConsumer   consumer invoked for each message found, or {@code null}
   * @param templateConsumer  consumer invoked for each template found, or {@code null}
   *
   * @throws IOException  if an I/O error occurs or the pack stream is invalid
   */
  @Contract(mutates = "param1,io")
  public static void importMessages(@NotNull InputStream packStream,
                                    Consumer<Message.WithCode> messageConsumer,
                                    BiConsumer<String,Message.WithSpaces> templateConsumer)
      throws IOException
  {
    requireNonNull(packStream, "packStream must not be null");

    final var packHelper = new PackSupport();

    try(var dataStream = new PackInputStream(PACK_CONFIG, packStream)) {
      if (dataStream.getVersion().isEmpty())
        throw new IllegalArgumentException("packStream has no version");

      // messages
      for(int n = 0, size = dataStream.readUnsignedShort(); n < size; n++)
      {
        final var message = packHelper.unpackMessageWithCode(dataStream);

        if (messageConsumer != null)
          messageConsumer.accept(message);
      }

      // templates
      for(int n = 0, size = dataStream.readUnsignedShort(); n < size; n++)
      {
        final var name = requireNonNull(dataStream.readString());
        final var template = packHelper.unpackMessageWithSpaces(dataStream);

        if (templateConsumer != null)
          templateConsumer.accept(name, template);
      }
    }
  }
}
