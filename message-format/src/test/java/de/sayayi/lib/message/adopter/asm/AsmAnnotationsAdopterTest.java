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
package de.sayayi.lib.message.adopter.asm;

import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.adopter.AsmAnnotationAdopter;
import de.sayayi.lib.message.annotation.MessageDef;
import de.sayayi.lib.message.annotation.MessageDefs;
import de.sayayi.lib.message.annotation.Text;
import de.sayayi.lib.message.formatter.DefaultFormatterService;
import de.sayayi.lib.message.parser.normalizer.LRUMessagePartNormalizer;
import lombok.val;
import org.junit.jupiter.api.Test;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Jeroen Gremmen
 */
@SuppressWarnings("SimplifiableAnnotation")
@MessageDefs({
    @MessageDef(code = "CLASS", texts = {
        @Text(locale = "de", text = "Deutsch"),
        @Text("Other language")
    })
})
public final class AsmAnnotationsAdopterTest
{
  @Test
  @MessageDef(code = "M1", text = "Method message 1")
  @MessageDef(code = "M2", texts = @Text("Method message 2"))
  void testScan()
  {
    val messageFactory = new MessageFactory(new LRUMessagePartNormalizer(10));
    val messageSupport = MessageSupportFactory.create(new DefaultFormatterService(), messageFactory);

    new AsmAnnotationAdopter(NO_CACHE_INSTANCE, messageSupport).adopt(
        AsmAnnotationsAdopterTest.class.getClassLoader(),
        singleton(AsmAnnotationsAdopterTest.class.getPackage().getName()));

    val accessor = messageSupport.getAccessor();

    assertTrue(accessor.hasMessageWithCode("A0"));
    assertTrue(accessor.hasMessageWithCode("CLASS"));
    assertTrue(accessor.hasMessageWithCode("M1"));
    assertTrue(accessor.hasMessageWithCode("M2"));
    assertTrue(accessor.hasMessageWithCode("INNER"));
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
