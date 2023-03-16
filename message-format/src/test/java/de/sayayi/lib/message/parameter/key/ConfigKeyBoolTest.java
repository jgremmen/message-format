/*
 * Copyright 2020 Jeroen Gremmen
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
package de.sayayi.lib.message.parameter.key;

import de.sayayi.lib.message.MessageSupport.MessageSupportAccessor;
import de.sayayi.lib.message.MessageSupportFactory;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static de.sayayi.lib.message.parameter.key.ConfigKey.MatchResult.*;
import static de.sayayi.lib.message.parameter.key.ConfigKeyBool.FALSE;
import static de.sayayi.lib.message.parameter.key.ConfigKeyBool.TRUE;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Jeroen Gremmen
 */
public class ConfigKeyBoolTest
{
  private MessageSupportAccessor accessor;


  @BeforeEach
  void init() {
    accessor = MessageSupportFactory.shared().getAccessor();
  }


  @Test
  void testMatchNull() {
    assertEquals(MISMATCH, TRUE.match(accessor, accessor.getLocale(), null));
  }


  @Test
  void testMatchBoolean()
  {
    val locale = accessor.getLocale();

    assertEquals(EXACT, TRUE.match(accessor, locale, true));
    assertEquals(MISMATCH, TRUE.match(accessor, locale, false));
    assertEquals(MISMATCH, FALSE.match(accessor, locale, true));
    assertEquals(EXACT, FALSE.match(accessor, locale, false));
  }


  @Test
  void testMatchNumber()
  {
    val locale = accessor.getLocale();

    // byte
    assertEquals(MISMATCH, TRUE.match(accessor, locale, (byte)0));
    assertEquals(LENIENT, TRUE.match(accessor, locale, (byte)100));

    assertEquals(LENIENT, FALSE.match(accessor, locale, (byte)0));
    assertEquals(MISMATCH, FALSE.match(accessor, locale, Byte.MIN_VALUE));

    // integer
    assertEquals(MISMATCH, TRUE.match(accessor, locale, 0));
    assertEquals(LENIENT, TRUE.match(accessor, locale, 100));

    assertEquals(LENIENT, FALSE.match(accessor, locale, 0));
    assertEquals(MISMATCH, FALSE.match(accessor, locale, Integer.MAX_VALUE));

    // long
    assertEquals(MISMATCH, TRUE.match(accessor, locale, 0L));
    assertEquals(LENIENT, TRUE.match(accessor, locale, -100L));

    assertEquals(LENIENT, FALSE.match(accessor, locale, 0L));
    assertEquals(MISMATCH, FALSE.match(accessor, locale, Long.MIN_VALUE));
  }


  @Test
  void testMatchString()
  {
    val locale = accessor.getLocale();

    assertEquals(EQUIVALENT, TRUE.match(accessor, locale, "true"));
    assertEquals(MISMATCH, TRUE.match(accessor, locale, "false"));
    assertEquals(EQUIVALENT, TRUE.match(accessor, locale, "TRUE"));

    assertEquals(EQUIVALENT, FALSE.match(accessor, locale, "false"));
    assertEquals(MISMATCH, FALSE.match(accessor, locale, "true"));
    assertEquals(EQUIVALENT, FALSE.match(accessor, locale, "FALSE"));

    assertEquals(LENIENT, TRUE.match(accessor, locale, "0.9"));
    assertEquals(MISMATCH, TRUE.match(accessor, locale, "-0"));
    assertEquals(LENIENT, TRUE.match(accessor, locale, "+1234567890000000"));

    assertEquals(LENIENT, FALSE.match(accessor, locale, "+0"));
    assertEquals(MISMATCH, FALSE.match(accessor, locale, "1e-100"));
    assertEquals(MISMATCH, FALSE.match(accessor, locale, "-1234567890000000"));
  }


  @Test
  void testMatchCharacter()
  {
    val locale = accessor.getLocale();

    assertEquals(LENIENT, TRUE.match(accessor, locale, '5'));
    assertEquals(MISMATCH, TRUE.match(accessor, locale, (char)1));
    assertEquals(MISMATCH, TRUE.match(accessor, locale, 'Y'));

    assertEquals(LENIENT, FALSE.match(accessor, locale, '0'));
    assertEquals(MISMATCH, FALSE.match(accessor, locale, '9'));
    assertEquals(MISMATCH, FALSE.match(accessor, locale, (char)0));
  }
}
