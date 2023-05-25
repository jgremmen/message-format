/*
 * Copyright 2020 Jeroen Gremmen
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
package de.sayayi.lib.message.part.parameter.key;

import de.sayayi.lib.message.MessageSupport.MessageAccessor;
import de.sayayi.lib.message.pack.PackInputStream;
import de.sayayi.lib.message.pack.PackOutputStream;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.text.Collator;
import java.util.Locale;

import static de.sayayi.lib.message.part.parameter.key.ConfigKey.CompareType.EQ;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.CompareType.NE;
import static de.sayayi.lib.message.part.parameter.key.ConfigKey.MatchResult.*;
import static java.util.Objects.requireNonNull;


/**
 * @author Jeroen Gremmen
 */
public final class ConfigKeyString implements ConfigKey
{
  private static final long serialVersionUID = 800L;

  /** Configuration string key comparison type. */
  private final @NotNull CompareType compareType;

  /** Configuration key string. */
  private final @NotNull String string;


  public ConfigKeyString(@NotNull CompareType compareType, @NotNull String string)
  {
    this.compareType = requireNonNull(compareType, "compareType must not be null");
    this.string = requireNonNull(string, "string must not be null");
  }


  /**
   * Returns the config key string value.
   *
   * @return  config key string value, never {@code null}
   */
  @Contract(pure = true)
  public @NotNull String getString() {
    return string;
  }


  /**
   * {@inheritDoc}
   *
   * @return  always {@link Type#STRING Type#STRING}
   */
  @Override
  public @NotNull Type getType() {
    return Type.STRING;
  }


  @Override
  public @NotNull MatchResult match(@NotNull MessageAccessor messageAccessor,
                                    @NotNull Locale locale, Object value)
  {
    if (value == null)
      return MISMATCH;

    MatchResult result = EXACT;
    int cmp = 0;

    doMatch: {
      if (!(value instanceof CharSequence || value instanceof Character))
        result = EQUIVALENT;

      final String text = value.toString();

      if (compareType == EQ)
      {
        if (text.equals(string))
          break doMatch;

        //noinspection DuplicateExpressions
        if (text.toLowerCase(locale).equals(string.toLowerCase(locale)))
        {
          result = LENIENT;
          break doMatch;
        }

        cmp = 1;
        break doMatch;
      }

      //noinspection DuplicateExpressions
      if (compareType == NE && !text.toLowerCase(locale).equals(string.toLowerCase(locale)))
      {
        result = LENIENT;
        cmp = 1;
        break doMatch;
      }

      cmp = Collator.getInstance(locale).compare(text, string);
    }

    return compareType.match(cmp) ? result : MISMATCH;
  }


  @Override
  public boolean equals(Object o)
  {
    if (o == this)
      return true;
    else if (!(o instanceof ConfigKeyString))
      return false;

    ConfigKeyString that = (ConfigKeyString)o;

    return compareType == that.compareType && string.equals(that.string);
  }


  @Override
  public int hashCode() {
    return (59 + compareType.hashCode()) * 59 + string.hashCode();
  }


  @Override
  public String toString() {
    return compareType.asPrefix() + '\'' + string.replace("'", "\\'") + '\'';
  }


  /**
   * @param packStream  data output pack target
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   */
  public void pack(@NotNull PackOutputStream packStream) throws IOException
  {
    packStream.writeEnum(compareType);
    packStream.writeString(string);
  }


  /**
   * @param packStream  source data input, not {@code null}
   *
   * @return  unpacked string map key, never {@code null}
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   */
  public static @NotNull ConfigKeyNumber unpack(@NotNull PackInputStream packStream)
      throws IOException
  {
    return new ConfigKeyNumber(packStream.readEnum(CompareType.class),
        requireNonNull(packStream.readString()));
  }
}
