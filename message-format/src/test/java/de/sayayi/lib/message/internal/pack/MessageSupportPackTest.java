package de.sayayi.lib.message.internal.pack;

import de.sayayi.lib.message.MessageSupport;
import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.GenericFormatterService;
import lombok.val;
import org.junit.jupiter.api.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Jeroen Gremmen
 * @since 0.8.1
 */
@TestMethodOrder(MethodOrderer.DisplayName.class)
@DisplayName("Pack/unpack various messages to/from stream")
class MessageSupportPackTest
{
  private static MessageSupport messageSupport;


  @BeforeAll
  static void initMessageSupport()
  {
    var cms =
        MessageSupportFactory.create(new GenericFormatterService(), NO_CACHE_INSTANCE);
    var messageFactory = cms.getMessageAccessor().getMessageFactory();

    cms.addMessage("MSG-001", "");
    cms.addMessage("MSG-002", "Not empty");
    cms.addMessage(messageFactory.parseMessage("MSG-003", Map.of(
        Locale.forLanguageTag("en"), "English",
        Locale.forLanguageTag("de"), "Deutsch")));
    cms.addMessage("MSG-004", "Compound parameter %{n} and template %[tpl]");
    cms.addMessage("MSG-005", "%{n,true:yes,64:'2^6','name':'name',null:'val %{n1}',empty:'empty',:'xyz'}");
    cms.addMessage("MSG-006", "%{n,name:-128,check:false,str:'string',msg:'msg %{p}'}");
    cms.addMessage("MSG-007", "^°!§$%&/()=?ßüöäÖÄÜ@€«∑®†Ω¨⁄øπ@∆ª©ƒ∂‚å¥≈ç√∫~∞…");
    cms.addTemplate("exception", messageFactory.parseTemplate("%{ex,!empty:': %{ex}'}"));
    cms.addMessage("MSG-008", "Something went wrong%[exception,withStack:true]");

    messageSupport = cms;
  }


  @Test
  @DisplayName("Import exported messages and templates")
  void testExportImport() throws IOException
  {
    val pack = new ByteArrayOutputStream();

    messageSupport.exportMessages(pack);

    val messageSupportCloned =
        MessageSupportFactory.create(new GenericFormatterService(), NO_CACHE_INSTANCE);

    try(val inStream = new ByteArrayInputStream(pack.toByteArray())) {
      messageSupportCloned.importMessages(inStream);
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
}
