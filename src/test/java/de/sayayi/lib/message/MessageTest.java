package de.sayayi.lib.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.text.ParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.Test;


/**
 * @author Jeroen Gremmen
 */
public class MessageTest
{
  @Test
  public void testParse1() throws ParseException
  {
    final Message m = MessageFactory.parse("Just a simple message without parameters ");
    assertNotNull(m);

    final String text = m.format(new Message.Context() {
      @Override
      public Locale getLocale() {
        return Locale.getDefault();
      }

      @Override
      public Object getParameterValue(String parameter) {
        return null;
      }

      @Override
      public Set<String> getParameters() {
        return Collections.emptySet();
      }
    });
    assertEquals("Just a simple message without parameters", text);
  }


  @Test
  public void testParseMultiLocale() throws ParseException
  {
    final Map<Locale,String> texts = new HashMap<Locale,String>();

    texts.put(Locale.UK, "%{n} %{n,choice,{1 -> 'colour', 'colours'}}.");
    texts.put(new Locale("nl", "NL"), "%{n} %{n,choice,{1 -> 'kleur', 'kleuren'}}.");
    texts.put(Locale.GERMAN, "%{n} %{n,choice,{1 -> 'Farbe', 'Farben'}}.");
    texts.put(Locale.US, "%{n} %{n,choice,{1 -> 'color', 'colors'}}.");

    final Message m = MessageFactory.parse(texts);

    final String nl = m.format(MessageContext.builder().withLocale(new Locale("nl", "NL")).withParameter("n", 1).buildContext());
    assertEquals("1 kleur.", nl);

    final String uk = m.format(MessageContext.builder().withLocale(Locale.UK).withParameter("n", 4).buildContext());
    assertEquals("4 colours.", uk);
  }
}
