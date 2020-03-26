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
package de.sayayi.lib.message.parser;

import de.sayayi.lib.message.exception.MessageParserException;
import de.sayayi.lib.message.parser.MessageLexer.Token;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static de.sayayi.lib.message.parser.MessageLexer.TokenType.*;


/**
 * @author Jeroen Gremmen
 */
public final class MessageLexer implements Iterable<Token>
{
  @Getter private final String message;
  @Getter private final int length;


  MessageLexer(String message)
  {
    this.message = message;
    length = message.length();
  }


  private boolean isSpace(int idx) {
    return idx >= 0 && idx < length && message.charAt(idx) == ' ';
  }


  private void skipWhitespace(TokenIterator data)
  {
    while(data.pos < length && message.charAt(data.pos) == ' ')
      data.pos++;
  }


  @SuppressWarnings({"squid:S3776", "squid:S135", "squid:S3398"})
  private Token nextToken(TokenIterator data)
  {
    Token token = null;

    while(token == null)
    {
      final int start = data.pos;

      if (data.pos == length)
      {
        if (data.state.isEmpty() || data.state.peek() == State.IN_TEXT)
          break;
        else
          throw new MessageParserException(length - 1, "unexpected end of message");
      }

      if (data.state.peek() == State.IN_TEXT)
      {
        final int idx_p = message.indexOf("%{", data.pos);

        if (idx_p == -1)
        {
          data.state.pop();
          data.pos = length;
          return new Token(start, length - 1, TEXT, message.substring(start), 0,
              isSpace(start) && start > 0, false);
        }
        else if (idx_p > data.pos)
        {
          data.pos = idx_p;
          return new Token(start, idx_p - 1, TEXT, message.substring(start, idx_p), 0,
              isSpace(start) && start > 0, isSpace(idx_p - 1));
        }
        else
        {
          data.pos += 2;
          data.state.push(State.IN_PARAMETER);
          return new Token(start, start + 1, PARAM_START, "%{", 0,isSpace(start - 1), false);
        }
      }
      else if (data.state.peek().isQuotedText())
      {
        int idxp = message.indexOf("%{", data.pos);
        final int idx_q = message.indexOf(data.state.peek().getQuote(), data.pos);

        if (idx_q == -1)
          throw new MessageParserException(data.pos, "quoted text not properly ended");
        else if (idxp > idx_q)
          idxp = -1;

        if (idxp == -1)
        {
          data.state.pop();
          data.pos = idx_q + 1;

          if (idx_q == start)
            continue;

          return new Token(start, idx_q - 1, TEXT, message.substring(start, idx_q), 0,
              isSpace(start), isSpace(idx_q - 1));
        }
        else if (idxp > start)
        {
          data.pos = idxp;
          return new Token(start, idxp - 1, TEXT, message.substring(start, idxp), 0,
              isSpace(start), isSpace(idxp - 1));
        }
        else
        {
          data.pos += 2;
          data.state.push(State.IN_PARAMETER);
          return new Token(start, start + 1, PARAM_START, "%{", 0, isSpace(start - 1), false);
        }
      }

      token = nextTokenParameter(data);
    }

    return token;
  }


  @SuppressWarnings({"squid:S3776", "incomplete-switch"})
  private Token nextTokenParameter(TokenIterator data)
  {
    skipWhitespace(data);

    if (data.pos == length)
      throw new MessageParserException(length - 1, "unexpected end of message");

    final int start = data.pos;
    char c = message.charAt(data.pos);

    switch(c)
    {
      case ',':
        data.pos++;
        return new Token(start, start, COMMA, ",", 0, false, false);

      case '{':
        data.pos++;
        data.state.push(State.IN_MAP);
        return new Token(start, start, MAP_START, "{", 0, false, false);

      case '}':
        TokenType token = null;
        switch(data.state.peek())
        {
          case IN_MAP:        token = MAP_END; break;
          case IN_PARAMETER:  token = PARAM_END; break;
          default:            break;
        }

        if (token != null)
        {
          data.pos++;
          data.state.pop();
          return new Token(start, start, token, "}", 0, false, isSpace(data.pos));
        }
        break;

      case '-':
        if (data.pos + 1 < length && message.charAt(data.pos + 1) == '>' && data.state.peek() == State.IN_MAP)
        {
          data.pos += 2;
          return new Token(start, start + 1, ARROW, "->", 0, false, false);
        }
        break;

      case 't':
      case 'T':
        if (start + 4 <= length)
        {
          final String text = message.substring(start, start + 4);
          if ("true".equalsIgnoreCase(text))
          {
            data.pos = start + 4;
            return new Token(start, start + 3, BOOLEAN, text, 1, false, false);
          }
        }
        break;

      case 'f':
      case 'F':
        if (start + 5 <= length)
        {
          final String text = message.substring(start, start + 5);
          if ("false".equalsIgnoreCase(text))
          {
            data.pos = start + 5;
            return new Token(start, start + 4, BOOLEAN, text, 0, false, false);
          }
        }
        break;

      case '\'':
      case '"':
        data.pos++;
        data.state.push((c == '"') ? State.IN_TEXT_DOUBLE_QUOTED : State.IN_TEXT_SINGLE_QUOTED);
        return null;

      case '=':
        data.pos++;
        return new Token(start, start, EQ, "=", 0, false, false);

      case '<':
        if (start + 1 <= length)
        {
          final char c2 = message.charAt(start + 1);
          if (c2 == '=')
          {
            data.pos += 2;
            return new Token(start, start + 1, LTE,"<=", 0 ,false, false);
          }
          else if (c2 == '>')
          {
            data.pos += 2;
            return new Token(start, start + 1, NE,"<>", 0 ,false, false);
          }
        }

        data.pos++;
        return new Token(start, start, LT, "<", 0, false, false);

      case '>':
        if (start + 1 <= length && message.charAt(start + 1) == '=')
        {
          data.pos += 2;
          return new Token(start, start + 1, GTE, ">=", 0, false, false);
        }

        data.pos++;
        return new Token(start, start, GT, ">", 0, false, false);

      default:
        break;
    }

    if (Character.isLetter(c))
    {
      int lookAheadPos = data.pos;
      int end = data.pos;

      while(++lookAheadPos < length)
      {
        c = message.charAt(lookAheadPos);

        if (Character.isLetterOrDigit(c))
          end = lookAheadPos;
        else if (c != '-' && c != '_')
          break;
      }

      data.pos = end + 1;
      return new Token(start, end, NAME, message.substring(start, end + 1), 0, false, false);
    }

    if (c == '-' && data.pos + 1 < length && Character.isDigit(message.charAt(data.pos + 1)))
      data.pos++;

    if (Character.isDigit(message.charAt(data.pos)))
    {
      //noinspection StatementWithEmptyBody
      while(data.pos + 1 < length && Character.isDigit(message.charAt(++data.pos)))
        ;

      final String number = message.substring(start, data.pos);
      return new Token(start, data.pos - 1, NUMBER, number, Integer.parseInt(number), false, false);
    }

    throw new MessageParserException(data.pos, "unexpected character '" + c + "' found");
  }



  @Override
  public Iterator<Token> iterator() {
    return new TokenIterator();
  }


  private final class TokenIterator implements Iterator<Token>
  {
    private final Deque<State> state;

    private int pos;
    private Token token;


    private TokenIterator()
    {
      state = new ArrayDeque<State>();
      state.push(State.IN_TEXT);
      pos = 0;

      token = nextToken(this);
    }


    @Override
    public boolean hasNext() {
      return token != null;
    }


    @Override
    public Token next()
    {
      if (!hasNext())
        throw new NoSuchElementException("no more tokens available");

      final Token returnToken = token;
      token = nextToken(this);

      return returnToken;
    }


    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }


  @ToString
  @EqualsAndHashCode(doNotUseGetters=true)
  static final class Token
  {
    @Getter int start;
    @Getter int end;
    @Getter TokenType type;
    @Getter String text;
    @Getter String rawText;
    @Getter int number;
    @Getter boolean spaceBefore;
    @Getter boolean spaceAfter;


    Token(int start, int end, TokenType type, String rawText, int number, boolean spaceBefore, boolean spaceAfter)
    {
      this.start = start;
      this.end = end;
      this.type = type;
      this.text = rawText.trim();
      this.rawText = rawText;
      this.number = number;
      this.spaceBefore = spaceBefore;
      this.spaceAfter = spaceAfter;
    }
  }


  @SuppressWarnings("squid:CommentedOutCodeLine")
  public enum TokenType
  {
    PARAM_START,  // %{
    PARAM_END,    // }
    COMMA,        // ,
    MAP_START,    // {
    MAP_END,      // }
    BOOLEAN,      // true | false
    NAME,
    TEXT,
    NUMBER,
    ARROW,        // ->
    LT,           // <
    LTE,          // <=
    EQ,           // =
    NE,           // <>
    GT,           // >
    GTE,          // >=
    ;
  }


  private enum State
  {
    IN_TEXT, IN_TEXT_SINGLE_QUOTED, IN_TEXT_DOUBLE_QUOTED, IN_PARAMETER, IN_MAP;


    boolean isQuotedText() {
      return this == IN_TEXT_SINGLE_QUOTED || this == IN_TEXT_DOUBLE_QUOTED;
    }


    char getQuote() {
      return (this == IN_TEXT_SINGLE_QUOTED) ? '\'' : '"';
    }
  }
}
