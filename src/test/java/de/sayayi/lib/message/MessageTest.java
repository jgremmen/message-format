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
package de.sayayi.lib.message;

import org.junit.Test;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


/**
 * @author Jeroen Gremmen
 */
public class MessageTest
{
  @Test
  public void testParse1()
  {
    final Message m = MessageFactory.parse("Just a simple message without parameters ");
    assertNotNull(m);

    final String text = m.format(ParameterFactory.DEFAULT.noParameters());
    assertEquals("Just a simple message without parameters", text);
  }


  @Test
  public void testParseMultiLocale()
  {
    final Map<Locale,String> texts = new HashMap<>();

    texts.put(Locale.UK, "%{n} %{n,choice,{1 -> 'colour', 'colours'}}.");
    texts.put(new Locale("nl", "NL"), "%{n} %{n,choice,{1 -> 'kleur', 'kleuren'}}.");
    texts.put(Locale.GERMAN, "%{n} %{n,choice,{1 -> 'Farbe', 'Farben'}}.");
    texts.put(Locale.US, "%{n} %{n,choice,{1 -> 'color', 'colors'}}.");

    final Message m = MessageFactory.parse(texts);

    final String nl = m.format(ParameterFactory.createFor("nl-NL").with("n", 1));
    assertEquals("1 kleur.", nl);

    final String uk = m.format(ParameterFactory.createFor(Locale.UK).with("n", 4));
    assertEquals("4 colours.", uk);
  }


  @Test
  public void testCompareType()
  {
    Message m = MessageFactory.parse("%{n,choice,{<0->'negative',>0->'positive','zero'}}");

    assertEquals("negative",
        m.format(ParameterFactory.createFor(Locale.UK).with("n", -1)));

    assertEquals("zero",
        m.format(ParameterFactory.createFor(Locale.UK).with("n", 0)));

    assertEquals("positive",
        m.format(ParameterFactory.createFor(Locale.UK).with("n", 1234)));
  }
}
