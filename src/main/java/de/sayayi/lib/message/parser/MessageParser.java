package de.sayayi.lib.message.parser;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.parameter.ParameterBoolean;
import de.sayayi.lib.message.parameter.ParameterData;
import de.sayayi.lib.message.parameter.ParameterInteger;
import de.sayayi.lib.message.parameter.ParameterMap;
import de.sayayi.lib.message.parameter.ParameterString;
import de.sayayi.lib.message.parser.MessageLexer.Token;
import de.sayayi.lib.message.parser.MessageLexer.TokenType;
import de.sayayi.lib.message.spi.EmptyMessageWithCode;
import de.sayayi.lib.message.spi.MultipartMessage;
import de.sayayi.lib.message.spi.SinglePartMessage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static de.sayayi.lib.message.parser.MessageLexer.TokenType.ARROW;
import static de.sayayi.lib.message.parser.MessageLexer.TokenType.COMMA;
import static de.sayayi.lib.message.parser.MessageLexer.TokenType.MAP_END;
import static de.sayayi.lib.message.parser.MessageLexer.TokenType.MAP_START;
import static de.sayayi.lib.message.parser.MessageLexer.TokenType.NAME;
import static de.sayayi.lib.message.parser.MessageLexer.TokenType.PARAM_END;
import static de.sayayi.lib.message.parser.MessageLexer.TokenType.PARAM_START;


public final class MessageParser
{
  private final MessageLexer lexer;
  private final Iterator<Token> tokenIterator;
  private final List<Token> tokens;


  public MessageParser(String message)
  {
    lexer = new MessageLexer(message);
    tokenIterator = lexer.iterator();
    tokens = new ArrayList<Token>();
  }


  protected Token getTokenAt(int idx)
  {
    while(idx >= tokens.size() && tokenIterator.hasNext())
      tokens.add(tokenIterator.next());

    return (idx < tokens.size()) ? tokens.get(idx) : null;
  }


  protected TokenType getTypeAt(int idx)
  {
    final Token token = getTokenAt(idx);
    return (token == null) ? null : token.getType();
  }


  public Message parseMessage()
  {
    final Message message = parseMessage(0);

    final Token token = getTokenAt(0);
    if (token != null)
      throw new MessageParserException(token.getStart(), "unexpected token " + token.getText());

    return message;
  }


  private Message parseMessage(int t)
  {
    final List<MessagePart> parts = new ArrayList<MessagePart>();
    Token token;

    message: {
      while((token = getTokenAt(t)) != null)
        switch(token.getType())
        {
          case TEXT:
            parts.add(new TextPart(token.getText(), token.isSpaceBefore(), token.isSpaceAfter()));
            tokens.remove(t);
            break;

          case PARAM_START:
            parts.add(parseParameter(t));
            break;

          default:
            break message;
        }
    }

    switch(parts.size())
    {
      case 0:
        return new EmptyMessageWithCode();

      case 1:
        return new SinglePartMessage(parts.get(0));

      default:
        return new MultipartMessage(parts);
    }
  }


  private ParameterPart parseParameter(int t)
  {
    final int tokenStart = t;
    final Token t0 = getTokenAt(t);
    final Token t1 = getTokenAt(t + 1);

    String parameter = null;
    String format = null;
    ParameterData data = null;

    // t0=PARAM_START t1=NAME COMMA NAME COMMA (NUMBER | BOOLEAN | MAP_START ... MAP_END) PARAM_END
    // t0=PARAM_START t1=NAME COMMA NAME PARAM_END
    // t0=PARAM_START t1=NAME COMMA (NUMBER | BOOLEAN | MAP_START ... MAP_END) PARAM_END
    // t0=PARAM_START t1=NAME PARAM_END

    assert t0.getType() == PARAM_START;
    if (t1.getType() != NAME)
      throw new MessageParserException((t1 == null) ? t0.getStart() + 2 : t1.getStart(), "missing parameter name");

    parse: {
      parameter = t1.getText();

      final Token t2 = getTokenAt(t + 2);
      if (t2 == null)
        throw new MessageParserException(t1.getEnd() + 1, "unexpected end of parameter " + parameter);

      // t2=COMMA NAME COMMA (NUMBER | BOOLEAN | MAP_START ... MAP_END) PARAM_END
      // t2=COMMA NAME PARAM_END
      // t2=COMMA (NUMBER | BOOLEAN | MAP_START ... MAP_END) PARAM_END
      // t2=PARAM_END

      if (t2.getType() == PARAM_END)
      {
        t += 2;
        break parse;
      }

      // t2=COMMA NAME COMMA (NUMBER |BOOLEAN | MAP_START ... MAP_END) PARAM_END
      // t2=COMMA NAME PARAM_END
      // t2=COMMA (NUMBER | BOOLEAN | MAP_START ... MAP_END) PARAM_END

      if (t2.getType() != COMMA)
        throw new MessageParserException(t2.getStart(), "comma expected in parameter " + parameter);

      final Token t3 = getTokenAt(t + 3);
      if (t3 == null)
        throw new MessageParserException(t2.getEnd() + 1, "unexpected end of parameter " + parameter);

      // t3=NAME COMMA (NUMBER | BOOLEAN | MAP_START ... MAP_END) PARAM_END
      // t3=NAME PARAM_END
      // t3=(NUMBER | BOOLEAN | MAP_START ... MAP_END) PARAM_END

      if (t3.getType() != NAME)
      {
        // t3=(NUMBER | BOOLEAN | MAP_START ... MAP_END) PARAM_END

        t += 3;
        data = parseParameterData(t);
        break parse;
      }
      else
      {
        // t3=NAME COMMA (NUMBER | BOOLEAN | MAP_START ... MAP_END) PARAM_END
        // t3=NAME PARAM_END

        format = t3.getText();

        final Token t4 = getTokenAt(t + 4);
        if (t4 == null)
          throw new MessageParserException(t3.getEnd() + 1, "unexpected end of parameter " + parameter);

        // t4=COMMA (NUMBER | BOOLEAN | MAP_START ... MAP_END) PARAM_END
        // t4=PARAM_END

        t += 4;

        if (t4.getType() == PARAM_END)
          break parse;

        // t4=COMMA (NUMBER | BOOLEAN | MAP_START ... MAP_END) PARAM_END

        if (t4.getType() != COMMA)
          throw new MessageParserException(t4.getStart(), "comma expected in parameter " + parameter);

        // (NUMBER | BOOLEAN | MAP_START ... MAP_END) PARAM_END

        data = parseParameterData(++t);
        break parse;
      }
    }

    final Token paramEnd = getTokenAt(t);
    if (paramEnd == null)
      throw new MessageParserException(t0.getStart(), "unexpected end of parameter " + parameter);
    if (paramEnd.getType() != PARAM_END)
      throw new MessageParserException(paramEnd.getStart(), "missing closing brace for parameter " + parameter);

    tokens.subList(tokenStart, t + 1).clear();

    return new ParameterPart(parameter, format, t0.isSpaceBefore(), paramEnd.isSpaceAfter(), data);
  }


  private ParameterData parseParameterData(int t)
  {
    final Token t0 = getTokenAt(t);

    // t0=NUMBER
    // t0=BOOLEAN
    // t0=IN_TEXT
    // t0=MAP_START ... MAP_END

    switch(t0.getType())
    {
      case NUMBER:
      case BOOLEAN:
        return parseNumberBoolean(t);

      case TEXT:
        tokens.remove(t);
        return new ParameterString(t0.getText());

      case MAP_START:
        return parseParameterMap(t);

      default:
        throw new MessageParserException(t0.getStart(), "unexpected token " + t0.getText());
    }
  }


  private ParameterData parseNumberBoolean(int t)
  {
    final Token t0 = getTokenAt(t);
    assert t0 != null;

    // t0=NUMBER
    // t0=BOOLEAN

    switch(t0.getType())
    {
      case NUMBER:
        tokens.remove(t);
        return new ParameterInteger(Integer.parseInt(t0.getText()));

      case BOOLEAN:
        tokens.remove(t);
        return new ParameterBoolean(t0.getNumber() != 0);

      default:
        throw new MessageParserException(t0.getStart(), "unexpected token " + t0.getText());
    }
  }


  private ParameterMap parseParameterMap(int t)
  {
    final Map<Object,Message> map = new LinkedHashMap<Object,Message>();

    Token t0 = getTokenAt(t);
    assert t0.getType() == MAP_START;

    tokens.remove(t);

    buildMap: {
      for(;;)
      {
        t0 = getTokenAt(t);
        if (t0 == null)
          throw new MessageParserException(lexer.getLength() + 1, "number, boolean or string expected");

        Object key = null;
        Message value = null;

        // t1=(NUMBER | BOOLEAN | IN_TEXT) ARROW MESSAGE (COMMA (NUMBER | BOOLEAN | IN_TEXT) ARROW MESSAGE)* MAP_END
        // t1=MAP_END

        switch(t0.getType())
        {
          case NUMBER:
          case BOOLEAN:
            key = parseNumberBoolean(t).asObject();
            break;

          case TEXT:
          case PARAM_START:
            final Message m = parseMessage(t);
            if (getTypeAt(t) == MAP_END)
            {
              map.put(null, m);
              break buildMap;
            }

            if (m.hasParameter())
              throw new MessageParserException(t0.getStart(), "parameter string is not allowed as a map key");

            key = m.format(null);
            break;

          case MAP_END:
            break buildMap;

          default:
            throw new MessageParserException(t0.getStart(), "unexpected token " + t0.getText());
        }

        t0 = getTokenAt(t);
        if (t0 == null || t0.getType() != ARROW)
          throw new MessageParserException((t0 == null) ? lexer.getLength() : t0.getStart(), "-> expected");

        tokens.remove(t);

        value = parseMessage(t);
        map.put(key, value);

        t0 = getTokenAt(t);
        if (t0 == null)
          throw new MessageParserException(lexer.getLength(), "unexpected end of map");

        switch(t0.getType())
        {
          case MAP_END:
            break buildMap;

          case COMMA:
            tokens.remove(t);
            break;

          default:
            throw new MessageParserException(t0.getStart(), "unexpected token " + t0.getText());
        }
      }
    }

    t0 = getTokenAt(t);
    if (t0 == null || t0.getType() != MAP_END)
      throw new MessageParserException((t0 == null) ? lexer.getLength() : t0.getStart(), "closing brace for map expected");

    tokens.remove(t);

    return new ParameterMap(map);
  }
}
