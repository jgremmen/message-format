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

import static de.sayayi.lib.message.ParameterFactory.DEFAULT;
import static de.sayayi.lib.message.data.map.MapKey.MatchResult.EQUIVALENT;
import static de.sayayi.lib.message.data.map.MapKey.MatchResult.EXACT;
import static de.sayayi.lib.message.data.map.MapKey.MatchResult.LENIENT;
import static de.sayayi.lib.message.data.map.MapKey.MatchResult.MISMATCH;
import static org.junit.Assert.assertEquals;


/**
 * @author Jeroen Gremmen
 */
public class MapKeyBoolTest
{
  @Test
  public void testMatchNull() {
    assertEquals(MISMATCH, new MapKeyBool(true).match(DEFAULT.noParameters(), null));
  }


  @Test
  public void testMatchBoolean()
  {
    assertEquals(EXACT, new MapKeyBool(true).match(DEFAULT.noParameters(), true));
    assertEquals(MISMATCH, new MapKeyBool(true).match(DEFAULT.noParameters(), false));
    assertEquals(MISMATCH, new MapKeyBool(false).match(DEFAULT.noParameters(), true));
    assertEquals(EXACT, new MapKeyBool(false).match(DEFAULT.noParameters(), false));
  }


  @Test
  public void testMatchNumber()
  {
    // byte
    assertEquals(MISMATCH, new MapKeyBool(true).match(DEFAULT.noParameters(), (byte)0));
    assertEquals(LENIENT, new MapKeyBool(true).match(DEFAULT.noParameters(), (byte)100));

    assertEquals(LENIENT, new MapKeyBool(false).match(DEFAULT.noParameters(), (byte)0));
    assertEquals(MISMATCH, new MapKeyBool(false).match(DEFAULT.noParameters(), Byte.MIN_VALUE));

    // integer
    assertEquals(MISMATCH, new MapKeyBool(true).match(DEFAULT.noParameters(), 0));
    assertEquals(LENIENT, new MapKeyBool(true).match(DEFAULT.noParameters(), 100));

    assertEquals(LENIENT, new MapKeyBool(false).match(DEFAULT.noParameters(), 0));
    assertEquals(MISMATCH, new MapKeyBool(false).match(DEFAULT.noParameters(), Integer.MAX_VALUE));

    // long
    assertEquals(MISMATCH, new MapKeyBool(true).match(DEFAULT.noParameters(), 0L));
    assertEquals(LENIENT, new MapKeyBool(true).match(DEFAULT.noParameters(), -100L));

    assertEquals(LENIENT, new MapKeyBool(false).match(DEFAULT.noParameters(), 0L));
    assertEquals(MISMATCH, new MapKeyBool(false).match(DEFAULT.noParameters(), Long.MIN_VALUE));
  }


  @Test
  public void testMatchString()
  {
    assertEquals(EQUIVALENT, new MapKeyBool(true).match(DEFAULT.noParameters(), "true"));
    assertEquals(MISMATCH, new MapKeyBool(true).match(DEFAULT.noParameters(), "false"));
    assertEquals(MISMATCH, new MapKeyBool(true).match(DEFAULT.noParameters(), "TRUE"));

    assertEquals(EQUIVALENT, new MapKeyBool(false).match(DEFAULT.noParameters(), "false"));
    assertEquals(MISMATCH, new MapKeyBool(false).match(DEFAULT.noParameters(), "true"));
    assertEquals(MISMATCH, new MapKeyBool(false).match(DEFAULT.noParameters(), "FALSE"));

    assertEquals(LENIENT, new MapKeyBool(true).match(DEFAULT.noParameters(), "0.9"));
    assertEquals(MISMATCH, new MapKeyBool(true).match(DEFAULT.noParameters(), "-0"));
    assertEquals(LENIENT, new MapKeyBool(true).match(DEFAULT.noParameters(), "+1234567890000000"));

    assertEquals(LENIENT, new MapKeyBool(false).match(DEFAULT.noParameters(), "+0"));
    assertEquals(MISMATCH, new MapKeyBool(false).match(DEFAULT.noParameters(), "1e-100"));
    assertEquals(MISMATCH, new MapKeyBool(false).match(DEFAULT.noParameters(), "-1234567890000000"));
  }


  @Test
  public void testMatchCharacter()
  {
    assertEquals(LENIENT, new MapKeyBool(true).match(DEFAULT.noParameters(), '5'));
    assertEquals(MISMATCH, new MapKeyBool(true).match(DEFAULT.noParameters(), (char)1));
    assertEquals(MISMATCH, new MapKeyBool(true).match(DEFAULT.noParameters(), 'Y'));

    assertEquals(LENIENT, new MapKeyBool(false).match(DEFAULT.noParameters(), '0'));
    assertEquals(MISMATCH, new MapKeyBool(false).match(DEFAULT.noParameters(), '9'));
    assertEquals(MISMATCH, new MapKeyBool(false).match(DEFAULT.noParameters(), (char)0));
  }
}
