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
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static de.sayayi.lib.message.data.map.MapKey.MatchResult.*;
import static de.sayayi.lib.message.data.map.MapKeyBool.FALSE;
import static de.sayayi.lib.message.data.map.MapKeyBool.TRUE;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Jeroen Gremmen
 */
public class MapKeyBoolTest
{
  @Test
  public void testMatchNull()
  {
    final MessageContext context = new MessageContext(DefaultFormatterService.getSharedInstance(), NO_CACHE_INSTANCE);

    assertEquals(MISMATCH, TRUE.match(context, context.noParameters().getLocale(), null));
  }


  @Test
  public void testMatchBoolean()
  {
    final MessageContext context = new MessageContext(DefaultFormatterService.getSharedInstance(), NO_CACHE_INSTANCE);
    final Locale locale = context.noParameters().getLocale();

    assertEquals(EXACT, TRUE.match(context, locale, true));
    assertEquals(MISMATCH, TRUE.match(context, locale, false));
    assertEquals(MISMATCH, FALSE.match(context, locale, true));
    assertEquals(EXACT, FALSE.match(context, locale, false));
  }


  @Test
  public void testMatchNumber()
  {
    final MessageContext context = new MessageContext(DefaultFormatterService.getSharedInstance(), NO_CACHE_INSTANCE);
    final Locale locale = context.noParameters().getLocale();

    // byte
    assertEquals(MISMATCH, TRUE.match(context, locale, (byte)0));
    assertEquals(LENIENT, TRUE.match(context, locale, (byte)100));

    assertEquals(LENIENT, FALSE.match(context, locale, (byte)0));
    assertEquals(MISMATCH, FALSE.match(context, locale, Byte.MIN_VALUE));

    // integer
    assertEquals(MISMATCH, TRUE.match(context, locale, 0));
    assertEquals(LENIENT, TRUE.match(context, locale, 100));

    assertEquals(LENIENT, FALSE.match(context, locale, 0));
    assertEquals(MISMATCH, FALSE.match(context, locale, Integer.MAX_VALUE));

    // long
    assertEquals(MISMATCH, TRUE.match(context, locale, 0L));
    assertEquals(LENIENT, TRUE.match(context, locale, -100L));

    assertEquals(LENIENT, FALSE.match(context, locale, 0L));
    assertEquals(MISMATCH, FALSE.match(context, locale, Long.MIN_VALUE));
  }


  @Test
  public void testMatchString()
  {
    final MessageContext context = new MessageContext(DefaultFormatterService.getSharedInstance(), NO_CACHE_INSTANCE);
    final Locale locale = context.noParameters().getLocale();

    assertEquals(EQUIVALENT, TRUE.match(context, locale, "true"));
    assertEquals(MISMATCH, TRUE.match(context, locale, "false"));
    assertEquals(EQUIVALENT, TRUE.match(context, locale, "TRUE"));

    assertEquals(EQUIVALENT, FALSE.match(context, locale, "false"));
    assertEquals(MISMATCH, FALSE.match(context, locale, "true"));
    assertEquals(EQUIVALENT, FALSE.match(context, locale, "FALSE"));

    assertEquals(LENIENT, TRUE.match(context, locale, "0.9"));
    assertEquals(MISMATCH, TRUE.match(context, locale, "-0"));
    assertEquals(LENIENT, TRUE.match(context, locale, "+1234567890000000"));

    assertEquals(LENIENT, FALSE.match(context, locale, "+0"));
    assertEquals(MISMATCH, FALSE.match(context, locale, "1e-100"));
    assertEquals(MISMATCH, FALSE.match(context, locale, "-1234567890000000"));
  }


  @Test
  public void testMatchCharacter()
  {
    final MessageContext context = new MessageContext(DefaultFormatterService.getSharedInstance(), NO_CACHE_INSTANCE);
    final Locale locale = context.noParameters().getLocale();

    assertEquals(LENIENT, TRUE.match(context, locale, '5'));
    assertEquals(MISMATCH, TRUE.match(context, locale, (char)1));
    assertEquals(MISMATCH, TRUE.match(context, locale, 'Y'));

    assertEquals(LENIENT, FALSE.match(context, locale, '0'));
    assertEquals(MISMATCH, FALSE.match(context, locale, '9'));
    assertEquals(MISMATCH, FALSE.match(context, locale, (char)0));
  }
}
