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

import de.sayayi.lib.message.formatter.DefaultFormatterService;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static java.util.Locale.UK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/**
 * @author Jeroen Gremmen
 */
public class MessageTest
{
  @Test
  public void testParse1()
  {
    final Message m = NO_CACHE_INSTANCE.parse("Just a simple message without parameters ");
    assertNotNull(m);

    final MessageContext context = new MessageContext(DefaultFormatterService.getSharedInstance(), NO_CACHE_INSTANCE);
    final String text = m.format(context, context.noParameters());
    assertEquals("Just a simple message without parameters", text);
  }


  @Test
  public void testParseMultiLocale()
  {
    final Map<Locale,String> texts = new HashMap<>();

    texts.put(UK, "%{n} %{n,choice,{1:'colour', 'colours'}}.");
    texts.put(new Locale("nl", "NL"), "%{n} %{n,choice,{1 : 'kleur', 'kleuren'}}.");
    texts.put(Locale.GERMAN, "%{n} %{n,choice,{1: 'Farbe', 'Farben'}}.");
    texts.put(Locale.US, "%{n} %{n,choice,{1:'color', 'colors'}}.");

    final Message m = NO_CACHE_INSTANCE.parse(texts);
    final MessageContext context = new MessageContext(DefaultFormatterService.getSharedInstance(), NO_CACHE_INSTANCE);

    final String nl = m.format(context, context.parameters().withLocale("nl-NL").with("n", 1));
    assertEquals("1 kleur.", nl);

    final String uk = m.format(context, context.parameters().withLocale(UK).with("n", 4));
    assertEquals("4 colours.", uk);
  }


  @Test
  public void testCompareType()
  {
    final MessageContext context = new MessageContext(DefaultFormatterService.getSharedInstance(), NO_CACHE_INSTANCE);
    Message m = context.getMessageFactory().parse("%{n,choice,{<0:'negative',>0:'positive','zero'}}");

    assertEquals("negative",
        m.format(context, context.parameters().withLocale(UK).with("n", -1)));

    assertEquals("zero",
        m.format(context, context.parameters().withLocale(UK).with("n", 0)));

    assertEquals("positive",
        m.format(context, context.parameters().withLocale(UK).with("n", 1234)));
  }
}
