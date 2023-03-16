/*
 * Copyright 2020 Jeroen Gremmen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.sayayi.lib.message;

import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Locale;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static java.util.Locale.UK;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Jeroen Gremmen
 */
public class MessageTest
{
  @Test
  void testParse1()
  {
    val messageSupport = MessageSupportFactory.shared();

    assertEquals("Just a simple message without parameters",
        messageSupport.message("Just a simple message without parameters ").format());
  }


  @Test
  void testParseMultiLocale()
  {
    val texts = new HashMap<Locale,String>();

    texts.put(UK, "%{n} %{n,choice,1:'colour', :'colours'}.");
    texts.put(new Locale("nl", "NL"), "%{n} %{n,choice,1 : 'kleur',: 'kleuren'}.");
    texts.put(Locale.GERMAN, "%{n} %{n,choice,1: 'Farbe', :'Farben'}.");
    texts.put(Locale.US, "%{n} %{n,choice,1:'color', :'colors'}.");

    val msg = NO_CACHE_INSTANCE.parseMessage(texts);
    val messageSupport = MessageSupportFactory.shared();

    assertEquals("1 kleur.", messageSupport.message(msg).locale("nl-NL").with("n", 1).format());
    assertEquals("4 colours.", messageSupport.message(msg).locale(UK).with("n", 4).format());
  }


  @Test
  void testCompareType()
  {
    val messageSupport = MessageSupportFactory.shared();
    val m = messageSupport.message("%{n,choice,<0:'negative',>0:'positive',:'zero'}").getMessage();

    assertEquals("negative", messageSupport.message(m).locale(UK).with("n", -1).format());
    assertEquals("zero", messageSupport.message(m).locale(UK).with("n", 0).format());
    assertEquals("positive", messageSupport.message(m).locale(UK).with("n", 1234).format());
  }


  @Test
  void testEmptyMessage()
  {
    val messageSupport = MessageSupportFactory.shared();

    assertEquals("", messageSupport.message("").format());
  }
}
