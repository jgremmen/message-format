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
package de.sayayi.lib.message.internal.part.typedvalue;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.part.TypedValue.StringValue;
import de.sayayi.lib.message.util.SupplierDelegate;
import de.sayayi.lib.pack.PackInputStream;
import de.sayayi.lib.pack.PackOutputStream;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.function.Supplier;

import static de.sayayi.lib.message.util.MessageUtil.*;
import static java.util.Objects.requireNonNull;


/**
 * Typed value implementation for string configuration or map values. In addition to exposing the raw string, it is
 * able to parse that string lazily into a {@link Message.WithSpaces message} on demand.
 *
 * @author Jeroen Gremmen
 * @since 0.4.0 (renamed in 0.8.0)
 */
public final class TypedValueString implements StringValue
{
  /** The string value. */
  private final @NotNull String string;

  /** Lazily evaluated, cached message representation of {@link #string}. */
  private final Supplier<Message.WithSpaces> messageSupplier;


  /**
   * Creates a new typed value string wrapping the given string.
   *
   * @param messageFactory  factory used for lazily parsing the string into a message, not {@code null}
   * @param string          the string value, not {@code null}
   *
   * @throws NullPointerException  if {@code messageFactory} or {@code string} is {@code null}
   */
  public TypedValueString(@NotNull MessageFactory messageFactory, @NotNull String string)
  {
    requireNonNull(messageFactory, "messageFactory must not be null");

    this.string = requireNonNull(string, "string must not be null");

    //noinspection LanguageMismatch
    messageSupplier = SupplierDelegate.of(() -> messageFactory.parseMessage(string));
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull String stringValue() {
    return string;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  @Contract(pure = true)
  public @NotNull String asObject() {
    return string;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Message.WithSpaces asMessage() {
    return messageSupplier.get();
  }


  /**
   * Serializes this string value into its format string representation. If the string is a valid
   * {@linkplain de.sayayi.lib.message.util.MessageUtil#isName(String) name}, it is serialized
   * unquoted; otherwise it is serialized as a quoted string.
   *
   * @param context  the serialization context, not {@code null}
   */
  @Override
  public void serialize(@NotNull Context context)
  {
    if (isName(string))
      serializeString(context, string);
    else
      serializeQuotedString(context, string);
  }


  /**
   * Compares this typed value string with another object for equality based on the wrapped
   * string value.
   *
   * @param o  the object to compare with
   *
   * @return  {@code true} if {@code o} is a {@code TypedValueString} with the same string value
   */
  @Override
  public boolean equals(Object o) {
    return o instanceof TypedValueString && string.equals(((TypedValueString)o).string);
  }


  /**
   * Returns the hash code based on the wrapped string value.
   *
   * @return  hash code
   */
  @Override
  public int hashCode() {
    return 59 + string.hashCode();
  }


  /**
   * Returns the string value enclosed in single quotes, with internal single quotes escaped.
   *
   * @return  quoted string representation, never {@code null}
   */
  @Override
  @Contract(pure = true)
  public @NotNull String toString() {
    return '\'' + string.replace("'", "\\'") + '\'';
  }


  /**
   * Writes this string value to the given pack output stream for binary serialization.
   *
   * @param packStream  data output pack target, not {@code null}
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   */
  public void pack(@NotNull PackOutputStream packStream) throws IOException {
    packStream.writeString(string);
  }


  /**
   * Reads a {@code TypedValueString} from the given pack input stream.
   *
   * @param packStream  source data input, not {@code null}
   *
   * @return  unpacked string value, never {@code null}
   *
   * @throws IOException  if an I/O error occurs
   *
   * @since 0.8.0
   */
  public static @NotNull TypedValueString unpack(@NotNull MessageFactory messageFactory,
                                                 @NotNull PackInputStream packStream) throws IOException {
    return new TypedValueString(messageFactory, requireNonNull(packStream.readString()));
  }
}
