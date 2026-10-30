/*
 * Copyright 2023 Jeroen Gremmen
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
package de.sayayi.lib.message.internal.pack;

import de.sayayi.lib.message.Message.LocaleAware;
import de.sayayi.lib.message.MessageSupport;
import de.sayayi.lib.message.MessageSupportFactory;
import lombok.val;
import org.junit.jupiter.api.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import static de.sayayi.lib.message.Message.Parameters;
import static java.util.Locale.*;
import static java.util.stream.Collectors.toMap;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests packing and unpacking through {@link MessageSupport}: export and import of messages to and from a stream,
 * default locale messages and message and template count limits.
 *
 * @author Jeroen Gremmen
 * @since 0.8.1
 */
@DisplayName("Pack/unpack various messages to/from stream")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class MessageSupportPackTest
{
  private static MessageSupport messageSupport;


  @BeforeAll
  static void initMessageSupport()
  {
    var cms = MessageSupportFactory.createGeneric();
    var messageFactory = cms.getMessageAccessor().getMessageFactory();

    cms.addMessage("MSG-001", "");
    cms.addMessage("MSG-002", "Not empty");
    cms.addMessage(messageFactory.parseMessage("MSG-003", Map.of(
        Locale.forLanguageTag("en"), "English",
        Locale.forLanguageTag("de"), "Deutsch")));
    cms.addMessage("MSG-004", "Compound parameter %{n} and template %[tpl]");
    cms.addMessage("MSG-005", "%{n,true:yes,64:'2^6','name':'name',null:'val %{n1}',empty:'empty',:'xyz'}");
    cms.addMessage("MSG-006", "%{n,name:-128,check:false,str:'string',msg:'msg %{p}'}");
    cms.addMessage("MSG-007", "^°!§$%&/()=?ßüöäÖÄÜ@€«∑®†Ω¨⁄øπ@∆ª©ƒ∂‚å¥≈ç√∫~∞…🍀");
    cms.addTemplate("exception", messageFactory.parseTemplate("%{ex,!empty:': %{ex}'}"));
    cms.addMessage("MSG-008", "Something went wrong%[exception,with-stack=true]");

    messageSupport = cms.seal();
  }


  @Test
  @DisplayName("Import exported messages and templates")
  void testExportImport() throws IOException
  {
    val pack = new ByteArrayOutputStream();

    messageSupport.exportMessages(pack);

    val messageSupportCloned = MessageSupportFactory.createGeneric();

    try(val packStream = new ByteArrayInputStream(pack.toByteArray())) {
      messageSupportCloned.importMessages(packStream);
    }

    val messageAccessor = messageSupport.getMessageAccessor();
    val messageAccessorCloned = messageSupportCloned.getMessageAccessor();

    val messageCodes = messageAccessor.getMessageCodes();
    assertEquals(messageCodes, messageAccessorCloned.getMessageCodes());

    for(val messageCode: messageCodes)
    {
      assertEquals(
          messageAccessor.getMessageByCode(messageCode),
          messageAccessorCloned.getMessageByCode(messageCode));
    }
  }


  @Test
  @DisplayName("Pack/unpack localized bundle with default locale entry")
  void testExportImportDefaultLocaleMessage() throws IOException
  {
    var cms = MessageSupportFactory.createGeneric();
    var localizedMessages = new LinkedHashMap<Locale,String>();

    localizedMessages.put(ROOT, "Default");
    localizedMessages.put(GERMAN, "Deutsch");

    cms.addMessage(cms.getMessageAccessor().getMessageFactory().parseMessage("MSG-DEFAULT", localizedMessages));

    val pack = new ByteArrayOutputStream();
    cms.exportMessages(pack);

    val imported = MessageSupportFactory.createGeneric();
    try(val packStream = new ByteArrayInputStream(pack.toByteArray())) {
      imported.importMessages(packStream);
    }

    final var importedMessageAccessor = imported.getMessageAccessor();
    var importedMessage = importedMessageAccessor.getMessageByCode("MSG-DEFAULT");
    var localizedBundle = assertInstanceOf(LocaleAware.class, importedMessage);

    assertEquals(
        localizedMessages,
        localizedBundle
            .getLocalizedMessages()
            .entrySet()
            .stream()
            .collect(toMap(
                Entry::getKey,
                entry -> entry.getValue().format(importedMessageAccessor, Parameters.empty(ROOT)))));
    assertEquals(
        "Default",
        importedMessage.format(importedMessageAccessor, Parameters.empty(JAPANESE)));
  }


  @Test
  @DisplayName("Export maximum number of messages")
  void testExportMaxMessageCount() throws IOException
  {
    val cms = MessageSupportFactory.createGeneric();
    for(var n = 0; n < 0xffff; n++)
      cms.addMessage("MSG-" + n, "text");

    val pack = new ByteArrayOutputStream();
    cms.exportMessages(pack);

    val imported = MessageSupportFactory.createGeneric();
    try(val packStream = new ByteArrayInputStream(pack.toByteArray())) {
      imported.importMessages(packStream);
    }

    assertEquals(0xffff, imported.getMessageAccessor().getMessageCodes().size());
  }


  @Test
  @DisplayName("Export too many messages")
  void testExportTooManyMessages()
  {
    val cms = MessageSupportFactory.createGeneric();
    for(var n = 0; n <= 0xffff; n++)
      cms.addMessage("MSG-" + n, "text");

    val pack = new ByteArrayOutputStream();
    val ex = assertThrows(IOException.class, () -> cms.exportMessages(pack));

    assertTrue(ex.getMessage().contains("messages"));
    assertEquals(0, pack.size());

    // filtering below the limit makes the export succeed
    assertDoesNotThrow(() -> cms.exportMessages(new ByteArrayOutputStream(), false, code -> !code.equals("MSG-0"), null));
  }


  @Test
  @DisplayName("Export too many templates")
  void testExportTooManyTemplates()
  {
    val cms = MessageSupportFactory.createGeneric();
    val messageFactory = cms.getMessageAccessor().getMessageFactory();
    val message = new StringBuilder();

    for(var n = 0; n <= 0xffff; n++)
    {
      cms.addTemplate("tpl-" + n, messageFactory.parseTemplate("template"));
      message.append("%[tpl-").append(n).append(']');
    }

    cms.addMessage("MSG", message.toString());

    val pack = new ByteArrayOutputStream();
    val ex = assertThrows(IOException.class, () -> cms.exportMessages(pack));

    assertTrue(ex.getMessage().contains("templates"));
    assertEquals(0, pack.size());

    // filtering below the limit makes the export succeed
    assertDoesNotThrow(() -> cms.exportMessages(new ByteArrayOutputStream(), false, null, name -> !name.equals("tpl-0")));
  }
}
