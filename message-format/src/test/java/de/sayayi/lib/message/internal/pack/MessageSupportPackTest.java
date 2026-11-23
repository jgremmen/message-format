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
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StreamCorruptedException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

import static de.sayayi.lib.message.Message.Parameters;
import static de.sayayi.lib.message.util.PackTestSupport.packBytes;
import static de.sayayi.lib.message.util.PackTestSupport.packRoundTrip;
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
  void exportImport() throws IOException
  {
    var messageSupportCloned = packRoundTrip(messageSupport, MessageSupportFactory::createGeneric);
    var messageAccessor = messageSupport.getMessageAccessor();
    var messageAccessorCloned = messageSupportCloned.getMessageAccessor();

    var messageCodes = messageAccessor.getMessageCodes();
    assertEquals(messageCodes, messageAccessorCloned.getMessageCodes());

    for(var messageCode: messageCodes)
    {
      assertEquals(
          messageAccessor.getMessageByCode(messageCode),
          messageAccessorCloned.getMessageByCode(messageCode));
    }
  }


  @Test
  @DisplayName("Pack/unpack localized bundle with default locale entry")
  void exportImportDefaultLocaleMessage() throws IOException
  {
    var cms = MessageSupportFactory.createGeneric();
    var localizedMessages = new LinkedHashMap<Locale,String>();

    localizedMessages.put(ROOT, "Default");
    localizedMessages.put(GERMAN, "Deutsch");

    cms.addMessage(cms.getMessageAccessor().getMessageFactory().parseMessage("MSG-DEFAULT", localizedMessages));

    var imported = packRoundTrip(cms, MessageSupportFactory::createGeneric);
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
  void exportMaxMessageCount() throws IOException
  {
    var imported = packRoundTrip(messageSupportWithMessages(0xffff), MessageSupportFactory::createGeneric);

    assertEquals(0xffff, imported.getMessageAccessor().getMessageCodes().size());
  }


  @Test
  @DisplayName("Export and import close the stream")
  void streamsClosed() throws IOException
  {
    var pack = new CloseRecordingOutputStream();

    messageSupport.exportMessages(pack);
    assertTrue(pack.closed);

    var packStream = new CloseRecordingInputStream(pack.toByteArray());

    MessageSupportFactory
        .createGeneric()
        .importMessages(packStream);

    assertTrue(packStream.closed);
  }


  @ParameterizedTest(name = "[{index}] compress = {0}")
  @DisplayName("Export writes the pack in blocks, not byte by byte")
  @ValueSource(booleans = { false, true })
  void exportBuffered(boolean compress) throws IOException
  {
    var stream = new WriteCountingOutputStream();

    messageSupport.exportMessages(stream, compress, null, null);

    assertTrue(stream.bytes > 0);
    assertEquals(0, stream.singleByteWrites);
    // the pack is smaller than the buffer; flushing a compressed pack writes the GZip trailer separately
    assertTrue(stream.blockWrites <= 2, "too many block writes: " + stream.blockWrites);

    // the buffered pack is complete
    var imported = MessageSupportFactory.createGeneric();
    imported.importMessages(new ByteArrayInputStream(stream.toByteArray()));

    assertEquals(messageSupport.getMessageAccessor().getMessageCodes(),
        imported.getMessageAccessor().getMessageCodes());
  }


  @Test
  @DisplayName("Import closes the stream if the pack is invalid")
  void importFailureClosesStream()
  {
    var packStream = new CloseRecordingInputStream("not a message pack".getBytes(UTF_8));

    assertThrows(
        IOException.class,
        () -> MessageSupportFactory
            .createGeneric()
            .importMessages(packStream));
    assertTrue(packStream.closed);
  }


  @Test
  @DisplayName("Import rejects a pack with an unsupported version")
  void importUnsupportedVersion() throws IOException
  {
    var pack = packBytes(PackSupport.VERSION + 1, packStream -> {
      packStream.writeUnsignedShort(0);  // messages
      packStream.writeUnsignedShort(0);  // templates
    });

    var ex = assertThrows(
        IOException.class,
        () -> MessageSupportFactory
            .createGeneric()
            .importMessages(new ByteArrayInputStream(pack)));

    assertTrue(ex.getMessage().contains("unsupported pack version " + (PackSupport.VERSION + 1)));
  }


  @Test
  @DisplayName("Import rejects a pack with an invalid message type")
  void importInvalidMessageType() throws IOException
  {
    var pack = packBytes(PackSupport.VERSION, packStream -> {
      packStream.writeUnsignedShort(1);  // messages
      packStream.writeSmall(7, 3);       // invalid message type
      packStream.writeUnsignedShort(0);  // templates
    });

    var ex = assertThrows(
        StreamCorruptedException.class,
        () -> MessageSupportFactory
            .createGeneric()
            .importMessages(new ByteArrayInputStream(pack)));

    assertInstanceOf(
        IllegalStateException.class,
        ex.getCause());
  }


  @Test
  @DisplayName("Import rejects a pack with a missing template name")
  void importMissingTemplateName() throws IOException
  {
    var pack = packBytes(PackSupport.VERSION, packStream -> {
      packStream.writeUnsignedShort(0);  // messages
      packStream.writeUnsignedShort(1);  // templates
      packStream.writeString(null);      // template name
    });

    var ex = assertThrows(
        StreamCorruptedException.class,
        () -> MessageSupportFactory
            .createGeneric()
            .importMessages(new ByteArrayInputStream(pack)));

    assertInstanceOf(
        NullPointerException.class,
        ex.getCause());
  }


  @Test
  @DisplayName("Import rejects a truncated pack")
  void importTruncatedPack() throws IOException
  {
    var pack = new ByteArrayOutputStream();
    messageSupport.exportMessages(pack, false, null, null);

    var bytes = pack.toByteArray();
    var truncated = Arrays.copyOf(bytes, bytes.length / 2);

    assertThrows(
        IOException.class,
        () -> MessageSupportFactory
            .createGeneric()
            .importMessages(new ByteArrayInputStream(truncated)));
  }


  @Test
  @DisplayName("Import relays exceptions from adding messages")
  void importDuplicateMessage() throws IOException
  {
    var pack = new ByteArrayOutputStream();
    messageSupport.exportMessages(pack);

    var cms = MessageSupportFactory
        .createGeneric()
        .addMessage("MSG-002", "Different");

    assertThrows(
        DuplicateMessageException.class,
        () -> cms.importMessages(new ByteArrayInputStream(pack.toByteArray())));
  }


  @Test
  @DisplayName("Export too many messages fails and closes the stream")
  void exportTooManyMessages()
  {
    var cms = messageSupportWithMessages(0x10000);
    var pack = new CloseRecordingOutputStream();
    var ex = assertThrows(IOException.class, () -> cms.exportMessages(pack));

    assertTrue(ex.getMessage().contains("messages"));
    assertEquals(0, pack.size());
    assertTrue(pack.closed);

    // filtering below the limit makes the export succeed
    assertDoesNotThrow(() -> cms.exportMessages(new ByteArrayOutputStream(), false,
        code -> !code.equals("MSG-0"), null));
  }


  @Test
  @DisplayName("Export too many templates")
  void exportTooManyTemplates()
  {
    var cms = MessageSupportFactory.createGeneric();
    var messageFactory = cms.getMessageAccessor().getMessageFactory();
    var message = new StringBuilder();

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

    var pack = new ByteArrayOutputStream();
    var ex = assertThrows(IOException.class, () -> cms.exportMessages(pack));

    assertTrue(ex.getMessage().contains("templates"));
    assertEquals(0, pack.size());

    // filtering below the limit makes the export succeed
    assertDoesNotThrow(() -> cms.exportMessages(new ByteArrayOutputStream(), false, null,
        name -> !name.equals("tpl-0")));
  }


  @Test
  @DisplayName("Export message with too many parts")
  void exportTooManyMessageParts()
  {
    var cms = MessageSupportFactory.createGeneric();

    cms.addMessage("MSG-255", "%{p}.".repeat(127) + "%{p}");
    cms.addMessage("MSG-256", "%{p}.".repeat(128));

    // 255 message parts is the limit
    assertDoesNotThrow(() -> cms.exportMessages(new ByteArrayOutputStream(), false, "MSG-255"::equals,
        null));

    var ex = assertThrows(
        IOException.class,
        () -> cms.exportMessages(new ByteArrayOutputStream()));
    assertTrue(ex.getMessage().contains("255 parts"));
  }


  @Test
  @DisplayName("Export message exceeding the pack format limits")
  void exportPackFormatLimits()
  {
    var cms = MessageSupportFactory.createGeneric();

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
    var cms = MessageSupportFactory.createGeneric();
    for(var n = 0; n < count; n++)
      cms.addMessage("MSG-" + n, "text");

    return cms;
  }


  private static void assertExportFails(@NotNull MessageSupport messageSupport, @NotNull String code,
                                        @NotNull String expectedMessage)
  {
    var ex = assertThrows(
        IOException.class,
        () -> messageSupport.exportMessages(new ByteArrayOutputStream(), false, code::equals, null),
        code);

    assertTrue(
        ex.getMessage().contains(expectedMessage),
        ex.getMessage());
  }


  private static @NotNull String elements(int count, @NotNull IntFunction<String> element)
  {
    return IntStream
        .range(0, count)
        .mapToObj(element)
        .collect(joining(","));
  }


  // private use language codes qaa..qtz
  private static @NotNull Locale locale(int n) {
    return Locale.of("q" + (char)('a' + n / 26) + (char)('a' + n % 26));
  }




  /** Byte array output stream recording whether it has been closed. */
  private static final class CloseRecordingOutputStream extends ByteArrayOutputStream
  {
    private boolean closed;


    @Override
    public void close() {
      closed = true;
    }
  }




  /** Output stream counting single byte and block writes. */
  private static final class WriteCountingOutputStream extends OutputStream
  {
    private final ByteArrayOutputStream bytesWritten = new ByteArrayOutputStream();
    private int singleByteWrites;
    private int blockWrites;
    private int bytes;


    @Override
    public void write(int b)
    {
      singleByteWrites++;
      bytes++;
      bytesWritten.write(b);
    }


    @Override
    public void write(byte @NotNull [] b, int off, int len)
    {
      blockWrites++;
      bytes += len;
      bytesWritten.write(b, off, len);
    }


    private byte @NotNull [] toByteArray() {
      return bytesWritten.toByteArray();
    }
  }




  /** Byte array input stream recording whether it has been closed. */
  private static final class CloseRecordingInputStream extends ByteArrayInputStream
  {
    private boolean closed;


    private CloseRecordingInputStream(byte @NotNull [] bytes) {
      super(bytes);
    }


    @Override
    public void close() {
      closed = true;
    }
  }
}
