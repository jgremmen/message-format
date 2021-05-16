/*
 * Copyright 2020 Jeroen Gremmen
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
import de.sayayi.lib.message.annotation.Text;
import org.junit.Test;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;


/**
 * @author Jeroen Gremmen
 */
public class MessageBundleTest
{
  @Test
  public void testClassInheritance()
  {
    MessageBundle bundle = new MessageBundle(NO_CACHE_INSTANCE);
    bundle.add(E1.class);
    bundle.add(E2.class);
  }



  public static class Base
  {
    @SuppressWarnings("unused")
    @MessageDef(code = "base", texts = @Text("Hello"))
    public void someMethod() {
    }
  }


  public static class E1 extends Base {
  }


  public static class E2 extends Base {
  }
}
