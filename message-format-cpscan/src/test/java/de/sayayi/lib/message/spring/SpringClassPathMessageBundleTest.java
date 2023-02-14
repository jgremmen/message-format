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
package de.sayayi.lib.message.spring;

import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.annotation.MessageDef;
import de.sayayi.lib.message.annotation.MessageDefs;
import de.sayayi.lib.message.annotation.Text;
import de.sayayi.lib.message.parser.normalizer.LRUMessagePartNormalizer;
import de.sayayi.lib.message.scanner.spring.SpringClassPathMessageBundle;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

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
public final class SpringClassPathMessageBundleTest
{
  @MessageDef(code = "M1", text = "Method message 1")
  @MessageDef(code = "M2", texts = @Text("Method message 2"))
  @Test
  public void testScan()
  {
    val messageFactory = new MessageFactory(new LRUMessagePartNormalizer(10));
    val bundle = new SpringClassPathMessageBundle(messageFactory,
        singleton(SpringClassPathMessageBundleTest.class.getPackage().getName()),
        new DefaultResourceLoader());

    assertTrue(bundle.hasMessageWithCode("CLASS"));
    assertTrue(bundle.hasMessageWithCode("M1"));
    assertTrue(bundle.hasMessageWithCode("M2"));
    assertTrue(bundle.hasMessageWithCode("INNER"));
  }




  @SuppressWarnings("unused")
  @MessageDef(code = "INNER", text = "Inner class")
  private static final class InnerClass {
  }
}
