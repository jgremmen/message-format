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
 * @author Jeroen Gremmen
 * @since 0.21.0
 */
public interface FormatStringSerializer
{
  void serialize(@NotNull Context context);




  record Context(@NotNull CharsetEncoder encoder, @NotNull TextJoiner textJoiner, Character inStringWithQuote)
  {
    public Context
    {
      if (inStringWithQuote != null && inStringWithQuote != '\'' && inStringWithQuote != '"')
        throw new IllegalArgumentException("inStringWithQuote must be either ', \" or null");
    }


    public Context(@NotNull Charset charset) {
      this(charset.newEncoder(), new TextJoiner(), null);
    }


    @Contract(pure = true)
    public Context withStringQuote(char stringQuote) {
      return new Context(encoder, textJoiner, stringQuote);
    }


    @Contract(pure = true)
    public Context withoutStringQuote() {
      return new Context(encoder, textJoiner, null);
    }


    @Contract(pure = true)
    public boolean canEncode(char ch) {
      return encoder.canEncode(ch);
    }
  }
}
