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
package de.sayayi.lib.message.asm.adopter;

import de.sayayi.lib.message.Message.LocaleAware;
import de.sayayi.lib.message.Message.WithCode;
import de.sayayi.lib.message.MessageSupport;
import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.NoParameters;
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
import static java.util.Locale.forLanguageTag;
import static org.junit.jupiter.api.Assertions.*;


/**
 * @author Jeroen Gremmen
 */
@DisplayName("@MessageDef annotations")
public class MessageDefAnnotationTest
{
  private static MessageSupport messageSupport;


  @BeforeAll
  static void initialize()
  {
    val cms = MessageSupportFactory
        .create(DefaultFormatterService.getSharedInstance(), NO_CACHE_INSTANCE);

    new AsmAnnotationAdopter(cms).adopt(MessageDefAnnotationTest.class);

    messageSupport = cms;
  }


  @Test
  @DisplayName("Multiple message def annotations on same method")
  @MessageDef(code = "T4", texts = @Text(locale = "en", text = "Message %{p1}"))
  @MessageDef(code = "T5", texts = {
      @Text(locale = "en", text = "English message"),
      @Text(locale = "de", text = "Deutsche   Nachricht")
  })
  void testMultiMessageAnnotation()
  {
    WithCode msg = messageSupport.code("T4").getMessage();

    assertEquals("T4", msg.getCode());
    assertFalse(msg instanceof LocaleAware);

    msg = messageSupport.code("T5").getMessage();

    assertEquals("T5", msg.getCode());
    assertInstanceOf(LocaleAware.class, msg);
  }


  @Test
  @DisplayName("Empty message with code")
  @MessageDef(code = "MSG-052", texts = {})
  @SuppressWarnings("DefaultAnnotationParam")
  void testEmptyMessageWithCode()
  {
    val msg = messageSupport.code("MSG-052").getMessage();

    assertInstanceOf(EmptyMessageWithCode.class, msg);
    assertEquals("MSG-052", msg.getCode());
  }


  @Test
  @DisplayName("Message without locale for context with locale")
  @MessageDef(code = "T3", text = "m3")
  void testMessageWithoutLocale()
  {
    val msg = messageSupport.code("T3").getMessage();
    val messageAccessor = messageSupport.getMessageAccessor();

    assertEquals("m3", msg.format(messageAccessor, NoParameters.EMPTY));
    assertEquals("m3", msg.format(messageAccessor, new NoParameters(Locale.ROOT)));
    assertEquals("m3", msg.format(messageAccessor, new NoParameters(Locale.US)));
    assertEquals("m3",
        msg.format(messageAccessor, new NoParameters(forLanguageTag("xx-YY"))));
    assertFalse(msg instanceof LocaleAware);
  }


  @Test
  @DisplayName("Single message selection for context with exact and lenient locales")
  @MessageDef(code = "T2", texts = @Text(locale = "nl-NL", text = "nl"))
  void testSingleMessageWithLocale()
  {
    val msg = messageSupport.code("T2").getMessage();
    val messageAccessor = messageSupport.getMessageAccessor();

    assertEquals("nl", msg.format(messageAccessor, NoParameters.EMPTY));
    assertEquals("nl", msg.format(messageAccessor, new NoParameters(Locale.ROOT)));
    assertEquals("nl", msg.format(messageAccessor, new NoParameters(Locale.US)));
    assertEquals("nl",
        msg.format(messageAccessor, new NoParameters(forLanguageTag("xx-YY"))));
  }


  @Test
  @DisplayName("Message selection for context with exact and lenient locales")
  @MessageDef(code = "T1", texts={
      @Text(locale = "en-US", text = "us"),
      @Text(locale = "nl", text = "nl"),
      @Text(locale = "en-GB", text = "uk"),
      @Text(locale = "de-DE", text = "de")
  })
  void testLocaleSelection()
  {
    val msg = messageSupport.code("T1").getMessage();
    val messageAccessor = messageSupport.getMessageAccessor();

    assertEquals("us", msg.format(messageAccessor, new NoParameters(Locale.ROOT)));
    assertEquals("uk", msg.format(messageAccessor, new NoParameters(Locale.UK)));
    assertEquals("nl",
        msg.format(messageAccessor, new NoParameters(forLanguageTag("nl-BE"))));
    assertEquals("us", msg.format(messageAccessor, new NoParameters(Locale.CHINESE)));
    assertEquals("de",
        msg.format(messageAccessor, new NoParameters(forLanguageTag("de-AT"))));
  }


  @Test
  @DisplayName("All message codes present")
  void testCodes()
  {
    val codes = messageSupport.getMessageAccessor().getMessageCodes();

    assertTrue(codes.contains("T1"));
    assertTrue(codes.contains("T2"));
    assertTrue(codes.contains("T3"));
    assertTrue(codes.contains("T4"));
    assertTrue(codes.contains("T5"));
    assertTrue(codes.contains("MSG-052"));
  }


  @Test
  @DisplayName("Pack message bundle and unpack it to a new bundle")
  void testPackUnpack() throws IOException
  {
    val packStream = new ByteArrayOutputStream();
    messageSupport.exportMessages(packStream, true, code -> code.startsWith("T"));

    val newMessageSupport = MessageSupportFactory
        .create(DefaultFormatterService.getSharedInstance(), NO_CACHE_INSTANCE);
    newMessageSupport.importMessages(new ByteArrayInputStream(packStream.toByteArray()));

    val codes = newMessageSupport.getMessageAccessor().getMessageCodes();

    assertTrue(codes.contains("T1"));
    assertTrue(codes.contains("T2"));
    assertTrue(codes.contains("T3"));
    assertTrue(codes.contains("T4"));
    assertTrue(codes.contains("T5"));
    assertFalse(codes.contains("MSG-052"));
  }
}
