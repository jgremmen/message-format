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
import de.sayayi.lib.message.MessageSupport.ConfigurableMessageSupport;
import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.exception.DuplicateMessageException;
import de.sayayi.lib.pack.PackOutputStream;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

import static de.sayayi.lib.message.Message.Parameters;
import static de.sayayi.lib.message.pack.PackConstants.PACK_CONFIG;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Locale.*;
import static java.util.stream.Collectors.joining;
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
    val pack = new ByteArrayOutputStream();
    messageSupportWithMessages(0xffff).exportMessages(pack);

    val imported = MessageSupportFactory.createGeneric();
    try(val packStream = new ByteArrayInputStream(pack.toByteArray())) {
      imported.importMessages(packStream);
    }

    assertEquals(0xffff, imported.getMessageAccessor().getMessageCodes().size());
  }


  @Test
  @DisplayName("Export and import close the stream")
  void testStreamsClosed() throws IOException
  {
    val packClosed = new AtomicBoolean();
    val pack = new ByteArrayOutputStream() {
      @Override
      public void close() {
        packClosed.set(true);
      }
    };

    messageSupport.exportMessages(pack);
    assertTrue(packClosed.get());

    val packStreamClosed = new AtomicBoolean();
    val packStream = new ByteArrayInputStream(pack.toByteArray()) {
      @Override
      public void close() {
        packStreamClosed.set(true);
      }
    };

    MessageSupportFactory.createGeneric().importMessages(packStream);
    assertTrue(packStreamClosed.get());
  }


  @Test
  @DisplayName("Import closes the stream if the pack is invalid")
  void testImportFailureClosesStream()
  {
    val closed = new AtomicBoolean();
    val packStream = new ByteArrayInputStream("not a message pack".getBytes(UTF_8)) {
      @Override
      public void close() {
        closed.set(true);
      }
    };

    assertThrows(IOException.class, () -> MessageSupportFactory.createGeneric().importMessages(packStream));
    assertTrue(closed.get());
  }


  @Test
  @DisplayName("Import rejects a pack with an unsupported version")
  void testImportUnsupportedVersion() throws IOException
  {
    val pack = new ByteArrayOutputStream();

    try(val packStream = new PackOutputStream(PACK_CONFIG, PackSupport.VERSION + 1, false, pack)) {
      packStream.writeUnsignedShort(0);  // messages
      packStream.writeUnsignedShort(0);  // templates
    }

    val ex = assertThrows(IOException.class, () -> MessageSupportFactory
        .createGeneric()
        .importMessages(new ByteArrayInputStream(pack.toByteArray())));

    assertTrue(ex.getMessage().contains("unsupported pack version " + (PackSupport.VERSION + 1)));
  }


  @Test
  @DisplayName("Import rejects a pack with an invalid message type")
  void testImportInvalidMessageType() throws IOException
  {
    val pack = new ByteArrayOutputStream();

    try(val packStream = new PackOutputStream(PACK_CONFIG, PackSupport.VERSION, false, pack)) {
      packStream.writeUnsignedShort(1);  // messages
      packStream.writeSmall(7, 3);       // invalid message type
      packStream.writeUnsignedShort(0);  // templates
    }

    val ex = assertThrows(StreamCorruptedException.class, () -> MessageSupportFactory
        .createGeneric()
        .importMessages(new ByteArrayInputStream(pack.toByteArray())));

    assertInstanceOf(IllegalStateException.class, ex.getCause());
  }


  @Test
  @DisplayName("Import rejects a pack with a missing template name")
  void testImportMissingTemplateName() throws IOException
  {
    val pack = new ByteArrayOutputStream();

    try(val packStream = new PackOutputStream(PACK_CONFIG, PackSupport.VERSION, false, pack)) {
      packStream.writeUnsignedShort(0);  // messages
      packStream.writeUnsignedShort(1);  // templates
      packStream.writeString(null);      // template name
    }

    val ex = assertThrows(StreamCorruptedException.class, () -> MessageSupportFactory
        .createGeneric()
        .importMessages(new ByteArrayInputStream(pack.toByteArray())));

    assertInstanceOf(NullPointerException.class, ex.getCause());
  }


  @Test
  @DisplayName("Import rejects a truncated pack")
  void testImportTruncatedPack() throws IOException
  {
    val pack = new ByteArrayOutputStream();
    messageSupport.exportMessages(pack, false, null, null);

    val bytes = pack.toByteArray();
    val truncated = Arrays.copyOf(bytes, bytes.length / 2);

    assertThrows(IOException.class, () -> MessageSupportFactory
        .createGeneric()
        .importMessages(new ByteArrayInputStream(truncated)));
  }


  @Test
  @DisplayName("Import relays exceptions from adding messages")
  void testImportDuplicateMessage() throws IOException
  {
    val pack = new ByteArrayOutputStream();
    messageSupport.exportMessages(pack);

    val cms = MessageSupportFactory.createGeneric().addMessage("MSG-002", "Different");

    assertThrows(DuplicateMessageException.class,
        () -> cms.importMessages(new ByteArrayInputStream(pack.toByteArray())));
  }


  @Test
  @DisplayName("Export too many messages fails and closes the stream")
  void testExportTooManyMessages()
  {
    val cms = messageSupportWithMessages(0x10000);
    val closed = new AtomicBoolean();
    val pack = new ByteArrayOutputStream() {
      @Override
      public void close() {
        closed.set(true);
      }
    };
    val ex = assertThrows(IOException.class, () -> cms.exportMessages(pack));

    assertTrue(ex.getMessage().contains("messages"));
    assertEquals(0, pack.size());
    assertTrue(closed.get());

    // filtering below the limit makes the export succeed
    assertDoesNotThrow(() -> cms.exportMessages(new ByteArrayOutputStream(), false,
        code -> !code.equals("MSG-0"), null));
  }


  @Test
  @DisplayName("Export too many templates")
  void testExportTooManyTemplates()
  {
    val cms = MessageSupportFactory.createGeneric();
    val messageFactory = cms.getMessageAccessor().getMessageFactory();
    val message = new StringBuilder();

    // spread template references over several messages, each below the message part limit
    for(var n = 0; n <= 0xffff; n++)
    {
      cms.addTemplate("tpl-" + n, messageFactory.parseTemplate("template"));
      message.append("%[tpl-").append(n).append(']');

      if (n % 255 == 254 || n == 0xffff)
      {
        cms.addMessage("MSG-" + n, message.toString());
        message.setLength(0);
      }
    }

    val pack = new ByteArrayOutputStream();
    val ex = assertThrows(IOException.class, () -> cms.exportMessages(pack));

    assertTrue(ex.getMessage().contains("templates"));
    assertEquals(0, pack.size());

    // filtering below the limit makes the export succeed
    assertDoesNotThrow(() -> cms.exportMessages(new ByteArrayOutputStream(), false, null,
        name -> !name.equals("tpl-0")));
  }


  @Test
  @DisplayName("Export message with too many parts")
  void testExportTooManyMessageParts()
  {
    val cms = MessageSupportFactory.createGeneric();

    cms.addMessage("MSG-255", "%{p}.".repeat(127) + "%{p}");
    cms.addMessage("MSG-256", "%{p}.".repeat(128));

    // 255 message parts is the limit
    assertDoesNotThrow(() -> cms.exportMessages(new ByteArrayOutputStream(), false, "MSG-255"::equals,
        null));

    val ex = assertThrows(IOException.class, () -> cms.exportMessages(new ByteArrayOutputStream()));
    assertTrue(ex.getMessage().contains("255 parts"));
  }


  @Test
  @DisplayName("Export message exceeding the pack format limits")
  void testExportPackFormatLimits()
  {
    val cms = MessageSupportFactory.createGeneric();

    cms.addMessage("MAP", "%{p," + elements(256, n -> "'k" + n + "':x") + '}');
    cms.addMessage("CONFIG", "%{p," + elements(256, n -> "c" + n + ":1") + '}');
    cms.addMessage("TPL-DEFAULT", "%[tpl," + elements(256, n -> "p" + n + "=1") + ']');
    cms.addMessage("TPL-DELEGATE", "%[tpl," + elements(256, n -> "p" + n + "->q") + ']');
    cms.addMessage(cms.getMessageAccessor().getMessageFactory().parseMessage("LOCALES",
        IntStream.range(0, 256).boxed().collect(toMap(MessageSupportPackTest::locale, n -> "text"))));
    cms.addMessage("STRING", "x".repeat(0x10000));
    cms.addTemplate("tpl", cms.getMessageAccessor().getMessageFactory().parseTemplate("%{p0}"));

    // messages exceeding the pack format limits can be used as usual
    assertEquals("x", cms.code("MAP").with("p", "k255").format());
    assertEquals("1", cms.code("TPL-DEFAULT").format());
    assertEquals("text", cms.code("LOCALES").locale(locale(255)).format());

    assertExportFails(cms, "MAP", "parameter map with more than 255 entries");
    assertExportFails(cms, "CONFIG", "parameter configuration with more than 255 entries");
    assertExportFails(cms, "TPL-DEFAULT", "template with more than 255 default parameters");
    assertExportFails(cms, "TPL-DELEGATE", "template with more than 255 parameter delegates");
    assertExportFails(cms, "LOCALES", "localized message bundle with more than 255 locales");
    assertExportFails(cms, "STRING", "message 'STRING' cannot be packed");
  }


  private static @NotNull ConfigurableMessageSupport messageSupportWithMessages(int count)
  {
    val cms = MessageSupportFactory.createGeneric();
    for(var n = 0; n < count; n++)
      cms.addMessage("MSG-" + n, "text");

    return cms;
  }


  private static void assertExportFails(@NotNull MessageSupport messageSupport, @NotNull String code,
                                        @NotNull String expectedMessage)
  {
    val ex = assertThrows(
        IOException.class,
        () -> messageSupport.exportMessages(new ByteArrayOutputStream(), false, code::equals, null),
        code);

    assertTrue(ex.getMessage().contains(expectedMessage), ex.getMessage());
  }


  private static @NotNull String elements(int count, @NotNull IntFunction<String> element) {
    return IntStream.range(0, count).mapToObj(element).collect(joining(","));
  }


  // private use language codes qaa..qtz
  private static @NotNull Locale locale(int n) {
    return Locale.of("q" + (char)('a' + n / 26) + (char)('a' + n % 26));
  }
}
