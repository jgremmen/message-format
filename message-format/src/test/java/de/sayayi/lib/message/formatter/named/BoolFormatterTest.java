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
package de.sayayi.lib.message.formatter.named;

import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.data.map.MapKey;
import de.sayayi.lib.message.data.map.MapKeyBool;
import de.sayayi.lib.message.data.map.MapValue;
import de.sayayi.lib.message.data.map.MapValueString;
import de.sayayi.lib.message.formatter.AbstractFormatterTest;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static de.sayayi.lib.message.internal.part.MessagePartFactory.noSpaceText;
import static de.sayayi.lib.message.internal.part.MessagePartFactory.nullText;
import static java.util.Locale.ENGLISH;
import static java.util.Locale.GERMAN;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Jeroen Gremmen
 */
public class BoolFormatterTest extends AbstractFormatterTest
{
  @Test
  public void testFormattableTypes()
  {
    assertFormatterForType(new BoolFormatter(), boolean.class);
    assertFormatterForType(new BoolFormatter(), Boolean.class);
  }


  @Test
  public void testFormat()
  {
    val context = new MessageContext(createFormatterService(new BoolFormatter()), NO_CACHE_INSTANCE, "de-DE");
    val map = new HashMap<MapKey,MapValue>();
    map.put(MapKeyBool.TRUE, new MapValueString("wahr"));
    map.put(MapKeyBool.FALSE, new MapValueString("falsch"));

    assertEquals(noSpaceText("wahr"), format(context, Boolean.TRUE, map));
    assertEquals(noSpaceText("falsch"), format(context, 0.0d, map, "bool"));
    assertEquals(noSpaceText("wahr"), format(context, -0.0001f, map, "bool"));
    assertEquals(noSpaceText("falsch"), format(context, "FALSE", map, "bool"));
    assertEquals(noSpaceText("wahr"), format(context, "TrUe", map, "bool"));
    assertEquals(noSpaceText("wahr"), format(context, -4, map, "bool"));
    assertEquals(nullText(), format(context, (Object)null, map, "bool"));
  }


  @Test
  public void testFormatter()
  {
    val context = new MessageContext(createFormatterService(new BoolFormatter()), NO_CACHE_INSTANCE, ENGLISH);
    val parameters = context.parameters()
        .with("a", Boolean.FALSE)
        .with("b", Boolean.TRUE)
        .with("c", Integer.valueOf(1234))
        .with("d", Integer.valueOf(0))
        .with("e", Double.valueOf(3.14d));
    val msg = context.getMessageFactory()
        .parse("%{a} %{b} %{c} %{c,bool} %{d,bool,true:'yes',false:'no'} %{e}");

    assertEquals("false true 1234 true no 3.14", msg.format(context, parameters));
  }


  @Test
  public void testNamedFormatter()
  {
    val context = new MessageContext(createFormatterService(new BoolFormatter()), NO_CACHE_INSTANCE, GERMAN);
    val msg = context.getMessageFactory().parse("%{b,bool,null:'<unknown>',true:'yes',false:'no'}");

    assertEquals("<unknown>", msg.format(context, context.parameters().with("b", null)));
    assertEquals("yes", msg.format(context, context.parameters().with("b", true)));
    assertEquals("no", msg.format(context, context.parameters().with("b", false)));
  }
}
