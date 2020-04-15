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

import de.sayayi.lib.message.Message.LocaleAware;
import de.sayayi.lib.message.Message.WithCode;
import de.sayayi.lib.message.annotation.MessageDef;
import de.sayayi.lib.message.annotation.MessageDefs;
import de.sayayi.lib.message.annotation.Text;
import de.sayayi.lib.message.internal.EmptyMessageWithCode;
import org.junit.Before;
import org.junit.Test;

import java.util.Locale;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * @author Jeroen Gremmen
 */
public class MessageDefAnnotationTest
{
  private MessageBundle bundle;


  @Before
  public void initialize() {
    bundle = new MessageBundle(MessageDefAnnotationTest.class);
  }


  @Test
  @MessageDefs({
      @MessageDef(code = "T4", texts = @Text(locale = "en", text = "Message %{p1}")),
      @MessageDef(code = "T5", texts = {
          @Text(locale = "en", text = "English message"),
          @Text(locale = "de", text = "Deutsche Nachricht")
      })
  })
  public void testMultiMessageAnotation()
  {
    WithCode msg = bundle.getByCode("T4");

    assertEquals("T4", msg.getCode());
    assertTrue(msg.hasParameters());
    assertTrue(msg instanceof LocaleAware);

    msg = bundle.getByCode("T5");

    assertEquals("T5", msg.getCode());
    assertFalse(msg.hasParameters());
    assertTrue(msg instanceof LocaleAware);
  }


  @Test
  @MessageDef(code="MSG-052", texts={})
  public void testEmptyMessageWithCode()
  {
    final WithCode msg = bundle.getByCode("MSG-052");

    assertTrue(msg instanceof EmptyMessageWithCode);
    assertEquals("MSG-052", msg.getCode());
  }


  @Test
  @MessageDef(code="T3", texts=@Text("m3"))
  public void testMessageWithoutLocale()
  {
    final WithCode msg = bundle.getByCode("T3");
    ParameterFactory factory = ParameterFactory.DEFAULT;

    assertEquals("m3", msg.format(factory.noParameters()));
    assertEquals("m3", msg.format(factory.withLocale(Locale.ROOT)));
    assertEquals("m3", msg.format(factory.withLocale(Locale.US)));
    assertEquals("m3", msg.format(factory.withLocale("xx-YY")));
    assertFalse(msg instanceof LocaleAware);
  }


  @Test
  @MessageDef(code="T2", texts=@Text(locale="nl-NL", text="nl"))
  public void testSingleMessageWithLocale()
  {
    final WithCode msg = bundle.getByCode("T2");
    ParameterFactory factory = ParameterFactory.DEFAULT;

    assertEquals("nl", msg.format(factory.noParameters()));
    assertEquals("nl", msg.format(factory.withLocale(Locale.ROOT)));
    assertEquals("nl", msg.format(factory.withLocale(Locale.US)));
    assertEquals("nl", msg.format(factory.withLocale("xx-YY")));
  }


  @Test
  @MessageDef(code = "T1", texts={
      @Text(locale="en-US", text="us"),
      @Text(locale="nl", text="nl"),
      @Text(locale="en-GB", text="uk"),
      @Text(locale="de-DE", text="de")
  })
  public void testLocaleSelection()
  {
    final WithCode msg = bundle.getByCode("T1");
    ParameterFactory factory = ParameterFactory.DEFAULT;

    assertEquals("us", msg.format(factory.withLocale(Locale.ROOT)));
    assertEquals("uk", msg.format(factory.withLocale(Locale.UK)));
    assertEquals("nl", msg.format(factory.withLocale("nl-BE")));
    assertEquals("us", msg.format(factory.withLocale(Locale.CHINESE)));
    assertEquals("de", msg.format(factory.withLocale("de-AT")));
  }


  @Test
  public void testCodes()
  {
    Set<String> codes = bundle.getCodes();

    assertTrue(codes.contains("T1"));
    assertTrue(codes.contains("T2"));
    assertTrue(codes.contains("T3"));
    assertTrue(codes.contains("T4"));
    assertTrue(codes.contains("T5"));
    assertTrue(codes.contains("MSG-052"));
  }
}
