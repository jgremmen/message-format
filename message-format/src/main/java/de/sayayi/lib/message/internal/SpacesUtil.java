/*
 * Copyright 2022 Jeroen Gremmen
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
package de.sayayi.lib.message.internal;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import static java.lang.Character.*;


/**
 * @author Jeroen Gremmen
 */
public final class SpacesUtil
{
  private SpacesUtil() {}


  @Contract(pure = true)
  public static boolean isSpaceChar(char ch) {
    return ((((1 << SPACE_SEPARATOR) | (1 << PARAGRAPH_SEPARATOR)) >> getType((int)ch)) & 1) != 0;
  }


  @Contract(value = "null -> null", pure = true)
  @SuppressWarnings("DuplicatedCode")
  public static String trimSpaces(String s)
  {
    if (s == null)
      return null;

    final char[] val = s.toCharArray();
    int endIndex = val.length;
    int startIdx = 0;

    while(startIdx < endIndex && isSpaceChar(val[startIdx]))
      startIdx++;

    while(startIdx < endIndex && isSpaceChar(val[endIndex - 1]))
      endIndex--;

    return startIdx == endIndex
        ? ""
        : startIdx > 0 || endIndex < val.length ? new String(val, startIdx, endIndex - startIdx) : s;
  }


  @Contract(pure = true)
  public static @NotNull String trimSpacesNotNull(String s) {
    return s == null ? "" : trimSpaces(s);
  }


  @Contract(pure = true)
  @SuppressWarnings("DuplicatedCode")
  public static boolean isTrimmedEmpty(String s)
  {
    final char[] val = s.toCharArray();
    int endIndex = val.length;
    int startIdx = 0;

    while(startIdx < endIndex && isSpaceChar(val[startIdx]))
      startIdx++;

    while(startIdx < endIndex && isSpaceChar(val[endIndex - 1]))
      endIndex--;

    return startIdx == endIndex;
  }


  @Contract(value = "null -> true", pure = true)
  public static boolean isEmpty(String s) {
    return s == null || s.isEmpty();
  }
}
