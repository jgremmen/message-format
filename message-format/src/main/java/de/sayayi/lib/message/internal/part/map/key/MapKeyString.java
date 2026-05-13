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
package de.sayayi.lib.message.internal.part.map.key;

import de.sayayi.lib.message.part.MapKey;
import de.sayayi.lib.pack.PackInputStream;
import de.sayayi.lib.pack.PackOutputStream;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static de.sayayi.lib.message.part.MapKey.CompareType.EQ;
import static de.sayayi.lib.message.util.MessageUtil.serializeQuotedString;
import static java.util.Objects.requireNonNull;


/**
 * Internal implementation of {@link MapKey} representing a string map key. A string key consists of a string value
 * and a {@link CompareType} that determines how the provided value is compared to this key.
 *
 * @author Jeroen Gremmen
 * @since 0.4.0 (renamed in 0.8.0)
 */
@SuppressWarnings("ClassCanBeRecord")
public final class MapKeyString implements MapKey
{
  /** The comparison type for this map key. */
  private final @NotNull CompareType compareType;

  /** The string key value. */
  private final @NotNull String string;


  /**
   * Creates a string map key with compare type {@link CompareType#EQ EQ}.
   *
   * @param string  the string key value, not {@code null}
   *
   * @throws NullPointerException  if {@code string} is {@code null}
   *
   * @since 0.10.0
   */
  public MapKeyString(@NotNull String string) {
    this(EQ, string);
  }


  /**
   * Creates a string map key with the given comparison type and string value.
   *
   * @param compareType  comparison type, not {@code null}
   * @param string       the string key value, not {@code null}
   *
   * @throws NullPointerException  if {@code compareType} or {@code string} is {@code null}
   */
  public MapKeyString(@NotNull CompareType compareType, @NotNull String string)
  {
    this.compareType = requireNonNull(compareType, "compareType must not be null");
    this.string = requireNonNull(string, "string must not be null");
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull CompareType getCompareType() {
    return compareType;
  }


  /**
   * Returns the string key value.
   *
   * @return  string key value, never {@code null}
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


  /**
   * Serializes this string map key into its format string representation, including the comparison type prefix
   * if applicable.
   *
   * @param context  the serialization context, not {@code null}
   */
  @Override
  public void serialize(@NotNull Context context)
  {
    context.textJoiner().addNoSpace(compareType.asPrefix());
    serializeQuotedString(context, string);
  }


  /**
   * Compares this string map key with another object for equality based on the string value and comparison type.
   *
   * @param o  the object to compare with
   *
   * @return  {@code true} if {@code o} is a {@code MapKeyString} with the same string value and comparison type
   */
  @Override
  public boolean equals(Object o) {
    return o instanceof MapKeyString that && compareType == that.compareType && string.equals(that.string);
  }


  /**
   * Returns the hash code based on the string value and comparison type.
   *
   * @return  hash code
   */
  @Override
  public int hashCode() {
    return (59 + compareType.hashCode()) * 59 + string.hashCode();
  }


  /**
   * Returns the string representation of this string map key, including the comparison type prefix and the quoted
   * string value.
   *
   * @return  string representation, never {@code null}
   */
  @Override
  public String toString() {
    return compareType.asPrefix() + '\'' + string.replace("'", "\\'") + '\'';
  }


  /**
   * Writes this string map key to the given pack output stream for binary serialization.
   *
   * @param packStream  data output pack target, not {@code null}
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
   * Reads a {@code MapKeyString} from the given pack input stream.
   *
   * @param packStream  source data input, not {@code null}
   *
   * @return  unpacked string map key, never {@code null}
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   */
  public static @NotNull MapKeyString unpack(@NotNull PackInputStream packStream) throws IOException {
    return new MapKeyString(packStream.readEnum(CompareType.class), requireNonNull(packStream.readString()));
  }
}
