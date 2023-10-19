/*
 * Copyright 2022 Jeroen Gremmen
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
package de.sayayi.lib.message.formatter.runtime;

import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.AbstractFormatterTest;
import lombok.val;
import org.junit.jupiter.api.Test;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static java.lang.Math.PI;
import static java.util.Locale.GERMANY;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Jeroen Gremmen
 * @since 0.9.1
 */
class NumberFormatterTest extends AbstractFormatterTest
{
  @Test
  public void testFormattableTypes()
  {
    val formatter = new NumberFormatter();

    assertFormatterForType(formatter, Number.class);
    assertFormatterForType(formatter, byte.class);
    assertFormatterForType(formatter, short.class);
    assertFormatterForType(formatter, int.class);
    assertFormatterForType(formatter, long.class);
    assertFormatterForType(formatter, float.class);
    assertFormatterForType(formatter, double.class);
  }


  @Test
  public void testDoubleFormat()
  {
    val messageSupport = MessageSupportFactory
        .create(createFormatterService(new NumberFormatter()), NO_CACHE_INSTANCE)
        .setLocale(GERMANY);

    assertEquals("3,14", messageSupport
        .message("%{pi,number:'#0.00'}")
        .with("pi", PI)
        .format());

    assertEquals("0,1", messageSupport
        .message("%{d}")
        .with("d", 0.1d)
        .format());
  }
}
