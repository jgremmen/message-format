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

import de.sayayi.lib.message.Message.LocaleAware;
import de.sayayi.lib.message.Message.WithCode;
import de.sayayi.lib.message.annotation.MessageDef;
import de.sayayi.lib.message.annotation.Text;
import de.sayayi.lib.message.formatter.DefaultFormatterService;
import de.sayayi.lib.message.internal.EmptyMessageWithCode;
import lombok.val;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static org.junit.jupiter.api.Assertions.*;


/**
 * @author Jeroen Gremmen
 */
public class MessageDefAnnotationTest
{
  private static MessageBundle bundle;


  @BeforeAll
  static void initialize() {
    bundle = new MessageBundle(NO_CACHE_INSTANCE, MessageDefAnnotationTest.class);
  }


  @Test
  @DisplayName("Multiple message def annotations on same method")
  @MessageDef(code = "T4", texts = @Text(locale = "en", text = "Message %{p1}"))
  @MessageDef(code = "T5", texts = {
      @Text(locale = "en", text = "English message"),
      @Text(locale = "de", text = "Deutsche   Nachricht")
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
  @DisplayName("Empty message with code")
  @MessageDef(code = "MSG-052", texts = {})
  @SuppressWarnings("DefaultAnnotationParam")
  public void testEmptyMessageWithCode()
  {
    val msg = bundle.getByCode("MSG-052");

    assertTrue(msg instanceof EmptyMessageWithCode);
    assertEquals("MSG-052", msg.getCode());
  }


  @Test
  @DisplayName("Message without locale for context with locale")
  @MessageDef(code = "T3", text = "m3")
  public void testMessageWithoutLocale()
  {
    val msg = bundle.getByCode("T3");
    val context = new MessageContext(DefaultFormatterService.getSharedInstance(), NO_CACHE_INSTANCE);

    assertEquals("m3", msg.format(context, context.noParameters()));
    assertEquals("m3", msg.format(context, context.parameters().withLocale(Locale.ROOT)));
    assertEquals("m3", msg.format(context, context.parameters().withLocale(Locale.US)));
    assertEquals("m3", msg.format(context, context.parameters().withLocale("xx-YY")));
    assertFalse(msg instanceof LocaleAware);
  }


  @Test
  @DisplayName("Single message selection for context with exact and lenient locales")
  @MessageDef(code = "T2", texts = @Text(locale = "nl-NL", text = "nl"))
  public void testSingleMessageWithLocale()
  {
    val msg = bundle.getByCode("T2");
    val context = new MessageContext(DefaultFormatterService.getSharedInstance(), NO_CACHE_INSTANCE);

    assertEquals("nl", msg.format(context, context.noParameters()));
    assertEquals("nl", msg.format(context, context.parameters().withLocale(Locale.ROOT)));
    assertEquals("nl", msg.format(context, context.parameters().withLocale(Locale.US)));
    assertEquals("nl", msg.format(context, context.parameters().withLocale("xx-YY")));
  }


  @Test
  @DisplayName("Message selection for context with exact and lenient locales")
  @MessageDef(code = "T1", texts={
      @Text(locale = "en-US", text = "us"),
      @Text(locale = "nl", text = "nl"),
      @Text(locale = "en-GB", text = "uk"),
      @Text(locale = "de-DE", text = "de")
  })
  public void testLocaleSelection()
  {
    val msg = bundle.getByCode("T1");
    val context = new MessageContext(DefaultFormatterService.getSharedInstance(), NO_CACHE_INSTANCE);

    assertEquals("us", msg.format(context, context.parameters().withLocale(Locale.ROOT)));
    assertEquals("uk", msg.format(context, context.parameters().withLocale(Locale.UK)));
    assertEquals("nl", msg.format(context, context.parameters().withLocale("nl-BE")));
    assertEquals("us", msg.format(context, context.parameters().withLocale(Locale.CHINESE)));
    assertEquals("de", msg.format(context, context.parameters().withLocale("de-AT")));
  }


  @Test
  @DisplayName("All message codes present")
  public void testCodes()
  {
    val codes = bundle.getCodes();

    assertTrue(codes.contains("T1"));
    assertTrue(codes.contains("T2"));
    assertTrue(codes.contains("T3"));
    assertTrue(codes.contains("T4"));
    assertTrue(codes.contains("T5"));
    assertTrue(codes.contains("MSG-052"));
  }


  @Test
  @DisplayName("Pack message bundle and unpack it to a new bundle")
  public void testPackUnpack() throws IOException
  {
    val packStream = new ByteArrayOutputStream();
    bundle.pack(packStream, true, code -> code.startsWith("T"));

    val newBundle = new MessageBundle(NO_CACHE_INSTANCE);
    newBundle.add(new ByteArrayInputStream(packStream.toByteArray()));

    val codes = newBundle.getCodes();

    assertTrue(codes.contains("T1"));
    assertTrue(codes.contains("T2"));
    assertTrue(codes.contains("T3"));
    assertTrue(codes.contains("T4"));
    assertTrue(codes.contains("T5"));
    assertFalse(codes.contains("MSG-052"));
  }
}
