/*
 * Copyright 2021 Jeroen Gremmen
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
package de.sayayi.lib.message.adopter;

import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.annotation.MessageDef;
import de.sayayi.lib.message.annotation.MessageDefs;
import de.sayayi.lib.message.annotation.Text;
import de.sayayi.lib.message.formatter.DefaultFormatterService;
import de.sayayi.lib.message.parser.normalizer.LRUMessagePartNormalizer;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Jeroen Gremmen
 */
@DisplayName("ASM annotation adopter")
@MessageDefs({
    @MessageDef(code = "CLASS", texts = {
        @Text(locale = "de", text = "Deutsch"),
        @Text("Other language")
    })
})
@SuppressWarnings("SimplifiableAnnotation")
public final class AsmAnnotationAdopterTest
{
  @Test
  @DisplayName("Scan messages")
  @MessageDef(code = "M1", text = "Method message 1")
  @MessageDef(code = "M2", texts = @Text("Method message 2"))
  void testScan()
  {
    val messageFactory = new MessageFactory(new LRUMessagePartNormalizer(10));
    val messageSupport = MessageSupportFactory.create(new DefaultFormatterService(), messageFactory);

    new AsmAnnotationAdopter(NO_CACHE_INSTANCE, messageSupport).adopt(
        AsmAnnotationAdopterTest.class.getClassLoader(),
        singleton(AsmAnnotationAdopterTest.class.getPackage().getName()));

    val messageAccessor = messageSupport.getMessageAccessor();

    assertTrue(messageAccessor.hasMessageWithCode("A0"));
    assertTrue(messageAccessor.hasMessageWithCode("CLASS"));
    assertTrue(messageAccessor.hasMessageWithCode("M1"));
    assertTrue(messageAccessor.hasMessageWithCode("M2"));
    assertTrue(messageAccessor.hasMessageWithCode("INNER"));
  }




  @SuppressWarnings("unused")
  @MessageDef(code = "INNER", text = "Inner class")
  private static final class InnerClass {
  }




  private interface InterfaceA {
    @SuppressWarnings("unused")
    void syntheticA();
  }



  private abstract static class AbstractA implements InterfaceA
  {
    @MessageDef(code = "A0", text = "In abstract class")
    public void syntheticA() {
    }
  }




  @SuppressWarnings("unused")
  private static class B extends AbstractA {
  }
}
