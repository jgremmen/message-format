package de.sayayi.lib.message.pack;

import de.sayayi.lib.message.MessageSupport;
import de.sayayi.lib.message.MessageSupport.ConfigurableMessageSupport;
import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.adopter.AsmAnnotationAdopter;
import de.sayayi.lib.message.annotation.MessageDef;
import de.sayayi.lib.message.annotation.TemplateDef;
import de.sayayi.lib.message.annotation.Text;
import de.sayayi.lib.message.formatter.GenericFormatterService;
import lombok.val;
import org.junit.jupiter.api.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

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
  @MessageDef(code = "MSG-001", text = "")  // empty message with code
  @MessageDef(code = "MSG-002", text = "Not empty")
  @MessageDef(code = "MSG-003", texts = {
      @Text(locale = "en", text = "English"),
      @Text(locale = "de", text = "Deutsch")
  })
  @MessageDef(code = "MSG-004", text = "Compound parameter %{n} and template %[tpl]")
  @MessageDef(code = "MSG-005", text =
      "%{n,true:yes,64:'2^6','name':'name',null:'val %{n1}',empty:'empty',:'xyz'}")
  @MessageDef(code = "MSG-006", text = "%{n,name:-128,check:false,str:'string',msg:'msg %{p}'}")
  @MessageDef(code = "MSG-007", text = "^°!§$%&/()=?ßüöäÖÄÜ@€«∑®†Ω¨⁄øπ@∆ª©ƒ∂‚å¥≈ç√∫~∞…")
  @TemplateDef(name = "exception", text = "%{ex,!empty:': %{ex}'}")
  @MessageDef(code = "MSG-008", text = "Something went wrong%[exception]")
  static void initMessageSupport()
  {
    messageSupport = MessageSupportFactory.create(new GenericFormatterService(), NO_CACHE_INSTANCE);

    new AsmAnnotationAdopter((ConfigurableMessageSupport)messageSupport)
        .adopt(MessageSupportPackTest.class);
  }


  @Test
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
      assertEquals(messageAccessor.getMessageByCode(messageCode),
          messageAccessorCloned.getMessageByCode(messageCode));
    }
  }
}
