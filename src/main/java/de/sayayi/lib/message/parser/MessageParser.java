package de.sayayi.lib.message.parser;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.SimpleMessageContext;
import de.sayayi.lib.message.parameter.ParameterBoolean;
import de.sayayi.lib.message.parameter.ParameterData;
import de.sayayi.lib.message.parameter.ParameterInteger;
import de.sayayi.lib.message.parameter.ParameterMap;
import de.sayayi.lib.message.parameter.ParameterString;
import lombok.ToString;


/**
 * @author Jeroen Gremmen
 */
public final class MessageParser
{
  private final String text;
  private final int length;


  public MessageParser(String text)
  {
    this.text = text;
    length = text.length();
  }


  public Message parse() throws ParseException
  {
    final Context context = new Context(0);

    parseMessageWithoutQuotes(context);

    return new MultipartMessage(context.parts);
  }


  private void parseMessageWithoutQuotes(Context context) throws ParseException
  {
    while(context.pos < length)
    {
      final int pidx = text.indexOf("%{", context.pos);
      final boolean spaceBefore = !context.parts.isEmpty() && isWhitespace(context.pos);

      if (pidx >= 0)
      {
        if (pidx > context.pos)
        {
          context.parts.add(new TextPart(text.substring(context.pos, pidx).trim(), spaceBefore, isWhitespace(pidx - 1)));
          context.pos = pidx;
        }
      }
      else
      {
        context.parts.add(new TextPart(text.substring(context.pos).trim(), spaceBefore, false));
        break;
      }

      parseParameter(context);
    }

    context.pos = length;
  }


  private void parseMessageWithQuotes(Context context, boolean allowNestedParameters) throws ParseException
  {
    final char quote = context.getChar();

    context.pos++;

    for(;;)
    {
      final int pidx = text.indexOf("%{", context.pos);
      final int qidx = text.indexOf(quote, context.pos);

      if (qidx == -1)
        throw new ParseException("missing closing quote for message", length);

      if (pidx >= 0 && pidx < qidx)
      {
        if (!allowNestedParameters)
          throw new ParseException("no nested parameter allowed in message", pidx);

        if (pidx > context.pos)
        {
          context.parts.add(new TextPart(text.substring(context.pos, pidx).trim(), isWhitespace(context.pos), isWhitespace(pidx - 1)));
          context.pos = pidx;
        }
      }
      else if (pidx == -1 || pidx > qidx)
      {
        if (qidx > context.pos)
          context.parts.add(new TextPart(text.substring(context.pos, qidx).trim(), isWhitespace(context.pos), false));

        context.pos = qidx;
        break;
      }

      parseParameter(context);
    }

    context.eatCharacter(quote);
  }


  private String parseString(Context context) throws ParseException
  {
    final Context string = new Context(context.pos);

    parseMessageWithQuotes(string, false);
    context.pos = string.pos;

    return string.parts.isEmpty() ? null : string.parts.get(0).getText(null);
  }


  private Object parseBoolOrIntOrString(Context context) throws ParseException
  {
    char c = context.getChar();

    if (c == '"' || c == '\'')
      return parseString(context);

    if (context.pos + 4 <= length && text.substring(context.pos, context.pos + 4).equals("true"))
      return Boolean.TRUE;
    if (context.pos + 5 <= length && text.substring(context.pos, context.pos + 5).equals("false"))
      return Boolean.FALSE;

    boolean negative = false;

    if (c == '+' || (negative = c == '-'))
    {
      context.pos++;
      if (context.pos >= length)
        throw new ParseException("unexpected end of integer value", context.pos);

      c = context.getChar();
    }

    if (!Character.isDigit(c))
      throw new ParseException("expecting integer value", context.pos);

    int i = 0;
    while(Character.isDigit(c = context.getChar()))
    {
      i = i * 10 + (c - '0');
      context.pos++;
    }

    return Integer.valueOf(negative ? -i : i);
  }


  private void parseParameter(Context context) throws ParseException
  {
    int pos;

    final boolean spaceBefore = context.pos > 0 && isWhitespace(context.pos - 1);

    context.pos += 2;
    context.eatWhitespace("parameter");

    String parameter;
    String format = "string";
    ParameterData data = null;

    pos = context.pos;
    if ((parameter = eatName(context)).isEmpty())
      throw new ParseException("missing parameter name", pos);

    context.eatWhitespace("parameter");

    if (context.checkAndEat(','))
    {
      context.eatWhitespace("parameter");
      pos = context.pos;

      if ((format = eatName(context)).isEmpty())
        throw new ParseException("missing format for parameter " + parameter, pos);

      context.eatWhitespace("parameter");

      if (context.checkAndEat(','))
      {
        context.eatWhitespace("parameter");

        final char c = context.getChar();
        if (c == '{')
          data = parseMap(context);
        else
          data = toParameterData(parseBoolOrIntOrString(context));
      }
    }

    context.eatWhitespace("parameter");
    context.eatCharacter('}');

    context.parts.add(new ParameterPart(parameter, format, spaceBefore, context.pos < length && isWhitespace(context.pos), data));
  }


  private ParameterData toParameterData(Object o)
  {
    if (o instanceof String)
      return new ParameterString((String)o);
    if (o instanceof Boolean)
      return new ParameterBoolean(((Boolean)o).booleanValue());
    if (o instanceof Number)
      return new ParameterInteger(((Number)o).intValue());

    return null;
  }


  private ParameterMap parseMap(Context context) throws ParseException
  {
    final Map<Object,Message> map = new LinkedHashMap<Object,Message>();

    context.eatCharacter('{');
    context.eatWhitespace("map");

    while(context.getChar() != '}')
    {
      final int pos = context.pos;
      boolean foundValidKey = false;
      Object key = null;

      try {
        key = parseBoolOrIntOrString(context);
        foundValidKey = true;
      } catch(final ParseException ex) {
        foundValidKey = false;
      }

      context.eatWhitespace("map");

      if (!foundValidKey || !context.lookAhead("->"))
      {
        final Context defaultValue = new Context(pos);
        parseMessageWithQuotes(defaultValue, true);
        map.put(null, new MultipartMessage(defaultValue.parts));
        context.pos = defaultValue.pos;
        break;
      }

      context.pos += 2;
      context.eatWhitespace("map");

      final Context value = new Context(context.pos);
      parseMessageWithQuotes(value, true);
      map.put(key, new MultipartMessage(value.parts));
      context.pos = value.pos;

      context.eatWhitespace("map");

      if (context.getChar() != '}')
      {
        context.eatCharacter(',');
        context.eatWhitespace("map");
      }
    }

    context.eatWhitespace("map");
    context.eatCharacter('}');

    return new ParameterMap(map);
  }


  private boolean isWhitespace(int pos) {
    return text.charAt(pos) == ' ';
  }


  private String eatName(Context context)
  {
    final int startPos = context.pos;

    for(; context.pos < length; context.pos++)
    {
      final char c = text.charAt(context.pos);
      if (Character.isLetter(c) || (context.pos > startPos && (Character.isDigit(c) || c == '_' || c == '-')))
        continue;

      break;
    }

    return text.substring(startPos, context.pos);
  }




  @ToString
  private class Context
  {
    List<MessagePart> parts = new ArrayList<MessagePart>();
    int pos;


    Context(int pos) {
      this.pos = pos;
    }


    void eatWhitespace(String msg) throws ParseException
    {
      while(pos < length && isWhitespace(pos))
        pos++;

      if (pos >= length)
        throw new ParseException("unexpected end of " + msg, length);
    }


    void eatCharacter(char expectedChar) throws ParseException
    {
      if (pos >= length || text.charAt(pos) != expectedChar)
        throw new ParseException("expected " + expectedChar, pos);

      pos++;
    }


    boolean checkAndEat(char expectedCharacter)
    {
      if (pos < length && getChar() == expectedCharacter)
      {
        pos++;
        return true;
      }

      return false;
    }


    boolean lookAhead(String expectedText)
    {
      final int endPos = pos + expectedText.length();

      return endPos <= length && text.subSequence(pos, endPos).equals(expectedText);
    }


    char getChar() {
      return text.charAt(pos);
    }
  }








  public static void main(String[] args) throws Exception
  {
    final Message m1 = new MessageParser("Es wurde%{count,choice,{0 -> 'n keine Dateien', 1 -> ' eine Datei', 'n %{count,integer} Dateien'}} gespeichert.").parse();

    System.out.println(m1.format(SimpleMessageContext.builder()
        .with("count", false).buildContext()));
  }
}
