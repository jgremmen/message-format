/*
 * Copyright 2021 Jeroen Gremmen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.sayayi.lib.message;

import de.sayayi.lib.message.annotation.MessageDef;
import de.sayayi.lib.message.annotation.MessageDefs;
import de.sayayi.lib.message.annotation.Text;
import org.junit.Test;

import static java.util.Collections.singleton;
import static org.junit.Assert.assertTrue;


/**
 * @author Jeroen Gremmen
 */
@MessageDefs({
    @MessageDef(code = "123", texts = {
        @Text(locale = "de", text = "Deutsch"),
        @Text("Andere Sprache")
    })
})
public final class ClassPathMessageBundleTest
{
  @MessageDef(code = "TEST", text = "Hallo")
  @MessageDef(code = "NEW", texts = @Text("Neu"))
  @Test
  public void testScan() throws Exception
  {
    final ClassPathMessageBundle bundle = new ClassPathMessageBundle(
        singleton(ClassPathMessageBundleTest.class.getPackage().getName()));

    assertTrue(bundle.hasMessageWithCode("TEST"));
    assertTrue(bundle.hasMessageWithCode("123"));
    assertTrue(bundle.hasMessageWithCode("NEW"));
  }
}
