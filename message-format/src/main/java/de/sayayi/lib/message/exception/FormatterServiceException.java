/*
 * Copyright 2025 Jeroen Gremmen
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


/**
 * @author Jeroen Gremmen
 * @since 0.20.0
 */
public class FormatterServiceException extends MessageException
{
  /**
   * Constructs a new formatter service exception with the specified detail message.
   * The cause is not initialized, and may subsequently be initialized by a
   * call to {@link #initCause}.
   *
   * @param message  the detail message. The detail message is saved for later retrieval by the
   *                 {@link #getMessage()} method.
   */
  public FormatterServiceException(String message) {
    super(message);
  }
}
