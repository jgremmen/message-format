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
package de.sayayi.lib.message.data.map;

import org.junit.Test;

import static de.sayayi.lib.message.data.map.MapKey.MatchResult.EXACT;
import static de.sayayi.lib.message.data.map.MapKey.MatchResult.LENIENT;
import static de.sayayi.lib.message.data.map.MapKey.MatchResult.MISMATCH;
import static java.util.Locale.ROOT;
import static org.junit.Assert.assertEquals;


/**
 * @author Jeroen Gremmen
 */
public class MapKeyBoolTest
{
  @Test
  public void testMatchNull() {
    assertEquals(MISMATCH, new MapKeyBool(true).match(ROOT, null));
  }


  @Test
  public void testMatchBoolean()
  {
    assertEquals(EXACT, new MapKeyBool(true).match(ROOT, true));
    assertEquals(MISMATCH, new MapKeyBool(true).match(ROOT, false));
    assertEquals(MISMATCH, new MapKeyBool(false).match(ROOT, true));
    assertEquals(EXACT, new MapKeyBool(false).match(ROOT, false));
  }


  @Test
  public void testMatchNumber()
  {
    // byte
    assertEquals(MISMATCH, new MapKeyBool(true).match(ROOT, (byte)0));
    assertEquals(LENIENT, new MapKeyBool(true).match(ROOT, (byte)100));

    assertEquals(LENIENT, new MapKeyBool(false).match(ROOT, (byte)0));
    assertEquals(MISMATCH, new MapKeyBool(false).match(ROOT, Byte.MIN_VALUE));

    // integer
    assertEquals(MISMATCH, new MapKeyBool(true).match(ROOT, 0));
    assertEquals(LENIENT, new MapKeyBool(true).match(ROOT, 100));

    assertEquals(LENIENT, new MapKeyBool(false).match(ROOT, 0));
    assertEquals(MISMATCH, new MapKeyBool(false).match(ROOT, Integer.MAX_VALUE));

    // long
    assertEquals(MISMATCH, new MapKeyBool(true).match(ROOT, 0L));
    assertEquals(LENIENT, new MapKeyBool(true).match(ROOT, -100L));

    assertEquals(LENIENT, new MapKeyBool(false).match(ROOT, 0L));
    assertEquals(MISMATCH, new MapKeyBool(false).match(ROOT, Long.MIN_VALUE));
  }


  @Test
  public void testMatchString()
  {
    assertEquals(LENIENT, new MapKeyBool(true).match(ROOT, "true"));
    assertEquals(MISMATCH, new MapKeyBool(true).match(ROOT, "false"));
    assertEquals(MISMATCH, new MapKeyBool(true).match(ROOT, "TRUE"));

    assertEquals(LENIENT, new MapKeyBool(false).match(ROOT, "false"));
    assertEquals(MISMATCH, new MapKeyBool(false).match(ROOT, "true"));
    assertEquals(MISMATCH, new MapKeyBool(false).match(ROOT, "FALSE"));

    assertEquals(LENIENT, new MapKeyBool(true).match(ROOT, "0.9"));
    assertEquals(MISMATCH, new MapKeyBool(true).match(ROOT, "-0"));
    assertEquals(LENIENT, new MapKeyBool(true).match(ROOT, "+1234567890000000"));

    assertEquals(LENIENT, new MapKeyBool(false).match(ROOT, "+0"));
    assertEquals(MISMATCH, new MapKeyBool(false).match(ROOT, "1e-100"));
    assertEquals(MISMATCH, new MapKeyBool(false).match(ROOT, "-1234567890000000"));
  }


  @Test
  public void testMatchCharacter()
  {
    assertEquals(LENIENT, new MapKeyBool(true).match(ROOT, '5'));
    assertEquals(MISMATCH, new MapKeyBool(true).match(ROOT, (char)1));
    assertEquals(MISMATCH, new MapKeyBool(true).match(ROOT, 'Y'));

    assertEquals(LENIENT, new MapKeyBool(false).match(ROOT, '0'));
    assertEquals(MISMATCH, new MapKeyBool(false).match(ROOT, '9'));
    assertEquals(MISMATCH, new MapKeyBool(false).match(ROOT, (char)0));
  }
}
