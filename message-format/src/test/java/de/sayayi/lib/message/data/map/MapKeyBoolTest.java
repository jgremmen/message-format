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

import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.formatter.DefaultFormatterService;
import org.junit.Test;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
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
  public void testMatchNull()
  {
    final MessageContext context = new MessageContext(DefaultFormatterService.getSharedInstance(), NO_CACHE_INSTANCE);

    assertEquals(MISMATCH, new MapKeyBool(true).match(context, context.noParameters(), null));
  }


  @Test
  public void testMatchBoolean()
  {
    final MessageContext context = new MessageContext(DefaultFormatterService.getSharedInstance(), NO_CACHE_INSTANCE);

    assertEquals(EXACT, new MapKeyBool(true).match(context, context.noParameters(), true));
    assertEquals(MISMATCH, new MapKeyBool(true).match(context, context.noParameters(), false));
    assertEquals(MISMATCH, new MapKeyBool(false).match(context, context.noParameters(), true));
    assertEquals(EXACT, new MapKeyBool(false).match(context, context.noParameters(), false));
  }


  @Test
  public void testMatchNumber()
  {
    final MessageContext context = new MessageContext(DefaultFormatterService.getSharedInstance(), NO_CACHE_INSTANCE);

    // byte
    assertEquals(MISMATCH, new MapKeyBool(true).match(context, context.noParameters(), (byte)0));
    assertEquals(LENIENT, new MapKeyBool(true).match(context, context.noParameters(), (byte)100));

    assertEquals(LENIENT, new MapKeyBool(false).match(context, context.noParameters(), (byte)0));
    assertEquals(MISMATCH, new MapKeyBool(false).match(context, context.noParameters(), Byte.MIN_VALUE));

    // integer
    assertEquals(MISMATCH, new MapKeyBool(true).match(context, context.noParameters(), 0));
    assertEquals(LENIENT, new MapKeyBool(true).match(context, context.noParameters(), 100));

    assertEquals(LENIENT, new MapKeyBool(false).match(context, context.noParameters(), 0));
    assertEquals(MISMATCH, new MapKeyBool(false).match(context, context.noParameters(), Integer.MAX_VALUE));

    // long
    assertEquals(MISMATCH, new MapKeyBool(true).match(context, context.noParameters(), 0L));
    assertEquals(LENIENT, new MapKeyBool(true).match(context, context.noParameters(), -100L));

    assertEquals(LENIENT, new MapKeyBool(false).match(context, context.noParameters(), 0L));
    assertEquals(MISMATCH, new MapKeyBool(false).match(context, context.noParameters(), Long.MIN_VALUE));
  }


  @Test
  public void testMatchString()
  {
    final MessageContext context = new MessageContext(DefaultFormatterService.getSharedInstance(), NO_CACHE_INSTANCE);

    assertEquals(EQUIVALENT, new MapKeyBool(true).match(context, context.noParameters(), "true"));
    assertEquals(MISMATCH, new MapKeyBool(true).match(context, context.noParameters(), "false"));
    assertEquals(MISMATCH, new MapKeyBool(true).match(context, context.noParameters(), "TRUE"));

    assertEquals(EQUIVALENT, new MapKeyBool(false).match(context, context.noParameters(), "false"));
    assertEquals(MISMATCH, new MapKeyBool(false).match(context, context.noParameters(), "true"));
    assertEquals(MISMATCH, new MapKeyBool(false).match(context, context.noParameters(), "FALSE"));

    assertEquals(LENIENT, new MapKeyBool(true).match(context, context.noParameters(), "0.9"));
    assertEquals(MISMATCH, new MapKeyBool(true).match(context, context.noParameters(), "-0"));
    assertEquals(LENIENT, new MapKeyBool(true).match(context, context.noParameters(), "+1234567890000000"));

    assertEquals(LENIENT, new MapKeyBool(false).match(context, context.noParameters(), "+0"));
    assertEquals(MISMATCH, new MapKeyBool(false).match(context, context.noParameters(), "1e-100"));
    assertEquals(MISMATCH, new MapKeyBool(false).match(context, context.noParameters(), "-1234567890000000"));
  }


  @Test
  public void testMatchCharacter()
  {
    final MessageContext context = new MessageContext(DefaultFormatterService.getSharedInstance(), NO_CACHE_INSTANCE);

    assertEquals(LENIENT, new MapKeyBool(true).match(context, context.noParameters(), '5'));
    assertEquals(MISMATCH, new MapKeyBool(true).match(context, context.noParameters(), (char)1));
    assertEquals(MISMATCH, new MapKeyBool(true).match(context, context.noParameters(), 'Y'));

    assertEquals(LENIENT, new MapKeyBool(false).match(context, context.noParameters(), '0'));
    assertEquals(MISMATCH, new MapKeyBool(false).match(context, context.noParameters(), '9'));
    assertEquals(MISMATCH, new MapKeyBool(false).match(context, context.noParameters(), (char)0));
  }
}
