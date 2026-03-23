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
package de.sayayi.lib.message;

import de.sayayi.lib.message.part.TextJoiner;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;


/**
 * Interface for objects that can serialize themselves into a message format string representation.
 * <p>
 * Implementing types write their serialized form to the {@link Context#textJoiner() text joiner} of a provided
 * {@link Context Context}. The context also carries charset encoding information and the current string quoting state,
 * allowing implementations to correctly escape characters and handle nested quoted strings.
 * <p>
 * A typical serialization is initiated by creating a new {@link Context Context} with a target
 * {@link java.nio.charset.Charset Charset} and calling {@link #serialize(Context) serialize(Context)}.
 * The result can then be obtained from the context's text joiner.
 *
 * @see Message#asFormatString(Charset)
 *
 * @author Jeroen Gremmen
 * @since 0.21.0
 */
@FunctionalInterface
public interface FormatStringSerializer
{
  /**
   * Serializes this object into its message format string representation by appending the serialized text to the 
   * {@link Context#textJoiner() text joiner} provided by the given {@code context}.
   * <p>
   * The context provides a {@link java.nio.charset.CharsetEncoder CharsetEncoder} for checking character encodability, 
   * a {@link TextJoiner} for accumulating serialized text fragments, and optional string quoting information for 
   * proper escaping of nested strings.
   *
   * @param context  the serialization context providing the text joiner, charset encoder and
   *                 string quoting information, not {@code null}
   *
   * @see Context Context
   * @see Message#asFormatString(Charset)
   */
  void serialize(@NotNull Context context);




  /**
   * Immutable serialization context used during format string serialization. It provides a {@link CharsetEncoder} 
   * for checking character encodability, a {@link TextJoiner} for accumulating serialized text fragments, and an
   * optional string quote character that indicates the current quoting context for proper escaping of nested strings.
   *
   * @param encoder            charset encoder used to determine whether a character can be
   *                           represented in the target charset, not {@code null}
   * @param textJoiner         text joiner used to accumulate serialized text fragments, not {@code null}
   * @param inStringWithQuote  the active string quote character ({@code '} or {@code "}), or
   *                           {@code null} if not currently inside a quoted string
   *
   * @since 0.21.0
   */
  record Context(@NotNull CharsetEncoder encoder, @NotNull TextJoiner textJoiner, Character inStringWithQuote)
  {
    /**
     * Validates that {@code inStringWithQuote}, if not {@code null}, is either a single quote
     * ({@code '}) or a double quote ({@code "}).
     *
     * @throws IllegalArgumentException  if {@code inStringWithQuote} is not {@code '}, {@code "} or {@code null}
     */
    public Context
    {
      if (inStringWithQuote != null && inStringWithQuote != '\'' && inStringWithQuote != '"')
        throw new IllegalArgumentException("inStringWithQuote must be either ', \" or null");
    }


    /**
     * Creates a new context for the given charset with a fresh {@link TextJoiner} and no active string quote.
     *
     * @param charset  charset to use for encoding checks, not {@code null}
     */
    public Context(@NotNull Charset charset) {
      this(charset.newEncoder(), new TextJoiner(), null);
    }


    /**
     * Returns a new context that shares the same encoder and text joiner but with the specified string quote 
     * character set as the active quote. This is used when serializing content inside a quoted string so that 
     * occurrences of the quote character are properly escaped.
     *
     * @param stringQuote  the string quote character ({@code '} or {@code "})
     *
     * @return  new context with the given string quote, never {@code null}
     *
     * @throws IllegalArgumentException  if {@code stringQuote} is not {@code '} or {@code "}
     */
    @Contract(pure = true)
    public Context withStringQuote(char stringQuote) {
      return new Context(encoder, textJoiner, stringQuote);
    }


    /**
     * Returns a new context that shares the same encoder and text joiner but with no active string quote. This is
     * used when serializing content that is not inside a quoted string.
     *
     * @return  new context without a string quote, never {@code null}
     */
    @Contract(pure = true)
    public Context withoutStringQuote() {
      return new Context(encoder, textJoiner, null);
    }


    /**
     * Tells whether the given character can be encoded using the context's charset encoder. Characters that cannot
     * be encoded are serialized as Unicode escape sequences (e.g. <code>&#92;u00e9</code>).
     *
     * @param ch  the character to check
     *
     * @return  {@code true} if the character can be encoded, {@code false} otherwise
     */
    @Contract(pure = true)
    public boolean canEncode(char ch) {
      return encoder.canEncode(ch);
    }
  }
}
