/*
 * Copyright 2020 Jeroen Gremmen
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
package de.sayayi.lib.message.parser;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.WritableToken;


/**
 * @author Jeroen Gremmen
 */
abstract class AbstractLexer extends Lexer
{
  protected AbstractLexer(CharStream input) {
    super(input);
  }


  @Override
  @SuppressWarnings({"java:S131", "java:S125", "java:S128"})
  public void emit(Token token)
  {
    // character
    if (token.getType() == MessageLexer.CH)
      emit_fixEscapeCharacter((WritableToken)token);

    super.emit(token);
  }


  @SuppressWarnings({"java:S100", "java:S1301", "java:S131"})
  private void emit_fixEscapeCharacter(WritableToken token)
  {
    final String text = token.getText();

    if (text.charAt(0) == '\\')
    {
      switch(text.length())
      {
        case 2:  // escape
          token.setText(text.substring(1));
          break;

        case 6:  // unicode
          token.setText(Character.toString((char)Integer.parseInt(text.substring(2), 16)));
          break;
      }
    }
  }
}
