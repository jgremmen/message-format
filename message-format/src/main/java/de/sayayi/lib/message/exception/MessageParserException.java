/*
 * Copyright 2019 Jeroen Gremmen
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
package de.sayayi.lib.message.exception;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;


/**
 * @author Jeroen Gremmen
 * @since 0.1.0
 */
public class MessageParserException extends MessageException
{
  private final String errorMessage;
  private final String syntaxError;


  public MessageParserException(@NotNull String errorMessage, @NotNull String syntaxError,
                                Exception cause)
  {
    super(errorMessage + '\n' + syntaxError, cause);

    this.errorMessage = errorMessage;
    this.syntaxError = syntaxError;
  }


  /**
   * Returns the error message describing what went wrong during parsing.
   *
   * @return  error message, never {@code null}
   *
   * @since 0.8.4
   */
  @Contract(pure = true)
  public @NotNull String getErrorMessage() {
    return errorMessage;
  }


  /**
   * Returns a visual representation of the location where the syntax error occurred during
   * parsing.
   *
   * @return  syntax error, never {@code null}
   *
   * @since 0.8.4
   */
  @Contract(pure = true)
  public @NotNull String getSyntaxError() {
    return syntaxError;
  }
}
