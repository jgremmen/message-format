/*
 * Copyright 2019 Jeroen Gremmen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.sayayi.lib.message.exception;

import org.antlr.v4.runtime.RecognitionException;


/**
 * @author Jeroen Gremmen
 */
@SuppressWarnings("serial")
public class MessageParserException extends MessageException
{
  public MessageParserException(String message, RecognitionException cause) {
    super(message, cause);
  }


  @Override
  public synchronized RecognitionException getCause() {
    return (RecognitionException) super.getCause();
  }
}