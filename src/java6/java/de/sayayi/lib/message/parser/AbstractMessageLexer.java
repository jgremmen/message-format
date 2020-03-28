package de.sayayi.lib.message.parser;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.WritableToken;


/**
 * @author Jeroen Gremmen
 */
abstract class AbstractMessageLexer extends Lexer
{
  protected AbstractMessageLexer(CharStream input) {
    super(input);
  }


  @Override
  public void emit(Token token)
  {
    WritableToken t = (WritableToken)token;

    switch(t.getType())
    {
      // %{
      case MessageTokenizer.PARAM_START1:
      case MessageTokenizer.PARAM_START2:
        t.setType(MessageTokenizer.PARAM_START);
        break;

      // '
      case MessageTokenizer.P_SQ_START:
      case MessageTokenizer.M_SQ_START:
        t.setType(MessageTokenizer.SINGLE_QUOTE_START);
        break;

      // "
      case MessageTokenizer.P_DQ_START:
      case MessageTokenizer.M_DQ_START:
        t.setType(MessageTokenizer.DOUBLE_QUOTE_START);
        break;

      // character
      case MessageTokenizer.CH1:
      case MessageTokenizer.CH2:
        t.setType(MessageTokenizer.CH);
      case MessageTokenizer.CH:
        emit_fixCharacter(t);
        break;

      // name
      case MessageTokenizer.P_NAME:
      case MessageTokenizer.M_NAME:
        t.setType(MessageTokenizer.NAME);
        break;
    }

    super.emit(token);
  }


  private void emit_fixCharacter(WritableToken token)
  {
    final String text = token.getText();
    final int length = text.length();

    if (text.charAt(0) == '\\')
    {
      if (length == 2)  // escape
        token.setText(text.substring(1));
      else if (length == 6)  // unicode
        token.setText(Character.toString((char)Integer.parseInt(text.substring(2), 16)));
    }
    else if (length > 1)
      token.setText(" ");

    assert token.getText().length() == 1;
  }
}
