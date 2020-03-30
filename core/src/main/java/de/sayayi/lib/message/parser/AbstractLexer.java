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
  public void emit(Token token)
  {
    WritableToken t = (WritableToken)token;

    switch(t.getType())
    {
      // %{
      case MessageLexer.PARAM_START1:
      case MessageLexer.PARAM_START2:
        t.setType(MessageLexer.PARAM_START);
        break;

      // ,
      case MessageLexer.P_COMMA:
      case MessageLexer.M_COMMA:
        t.setType(MessageLexer.COMMA);
        break;

      // '
      case MessageLexer.P_SQ_START:
      case MessageLexer.M_SQ_START:
        t.setType(MessageLexer.SINGLE_QUOTE_START);
        break;

      // "
      case MessageLexer.P_DQ_START:
      case MessageLexer.M_DQ_START:
        t.setType(MessageLexer.DOUBLE_QUOTE_START);
        break;

      // character
      case MessageLexer.CH1:
      case MessageLexer.CH2:
        t.setType(MessageLexer.CH);
      case MessageLexer.CH:
        emit_fixEscapeCharacter(t);
        break;

      // name
      case MessageLexer.P_NAME:
      case MessageLexer.M_NAME:
        t.setType(MessageLexer.NAME);
        break;

      // number
      case MessageLexer.P_NUMBER:
      case MessageLexer.M_NUMBER:
        t.setType(MessageLexer.NUMBER);
        break;

      // bool
      case MessageLexer.P_BOOL:
      case MessageLexer.M_BOOL:
        t.setType(MessageLexer.BOOL);
        break;
    }

    super.emit(token);
  }


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
