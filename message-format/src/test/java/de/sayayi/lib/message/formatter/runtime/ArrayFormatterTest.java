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
package de.sayayi.lib.message.formatter.runtime;

import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.data.map.*;
import de.sayayi.lib.message.formatter.AbstractFormatterTest;
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.formatter.NamedParameterFormatter;
import de.sayayi.lib.message.formatter.named.BoolFormatter;
import de.sayayi.lib.message.internal.TextMessage;
import de.sayayi.lib.message.internal.part.MessagePart.Text;
import de.sayayi.lib.message.internal.part.MessagePartFactory;
import de.sayayi.lib.message.internal.part.TextPart;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static de.sayayi.lib.message.internal.part.MessagePartFactory.nullText;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Jeroen Gremmen
 */
public class ArrayFormatterTest extends AbstractFormatterTest
{
  @Test
  public void testFormattableTypes()
  {
    assertFormatterForType(new ArrayFormatter(), boolean[].class);
    assertFormatterForType(new ArrayFormatter(), short[].class);
    assertFormatterForType(new ArrayFormatter(), int[].class);
    assertFormatterForType(new ArrayFormatter(), long[].class);
    assertFormatterForType(new ArrayFormatter(), float[].class);
    assertFormatterForType(new ArrayFormatter(), double[].class);
    assertFormatterForType(new ArrayFormatter(), Object[].class);
  }


  @Test
  @SuppressWarnings("serial")
  public void testBooleanArray()
  {
    val formatterService = createFormatterService(new ArrayFormatter(), new BoolFormatter());
    val context = new MessageContext(formatterService, NO_CACHE_INSTANCE, "de-DE");

    assertEquals(new TextPart("wahr, falsch, wahr"), format(context, new boolean[] { true, false, true }));

    val booleanMap = new HashMap<MapKey, MapValue>() {
      {
        put(MapKeyBool.TRUE, new MapValueMessage(new TextMessage(MessagePartFactory.noSpaceText("YES"))));
        put(MapKeyBool.FALSE, new MapValueMessage(new TextMessage(MessagePartFactory.noSpaceText("NO"))));
      }
    };

    assertEquals(new TextPart("NO, YES"), format(context, new boolean[] { false, true }, booleanMap));
    assertEquals(TextPart.EMPTY, format(context, new boolean[0]));

    formatterService.addFormatter(new NamedParameterFormatter() {
      @Override
      public @NotNull Text format(@NotNull FormatterContext context, Object value) {
        return value == null ? nullText() : new TextPart((Boolean)value ? "1" : "0");
      }

      @Override
      public boolean canFormat(@NotNull Class<?> type) {
        return Boolean.class.isAssignableFrom(type) || boolean.class.isAssignableFrom(type);
      }

      @Override
      public @NotNull String getName() {
        return "bool";
      }

      @Override
      public @NotNull Set<FormattableType> getFormattableTypes()
      {
        return new HashSet<>(Arrays.asList(
            new FormattableType(Boolean.class, 10),
            new FormattableType(boolean.class, 10)));
      }
    });

    assertEquals(new TextPart("1, 1, 0, 1, 0, 0, 0"), format(context,
        new boolean[] { true, true, false, true, false, false, false }, "bool"));
  }


  @Test
  public void testIntegerArray()
  {
    val formatterService = createFormatterService(new ArrayFormatter());
    val context = new MessageContext(formatterService, NO_CACHE_INSTANCE, "de-DE");

    assertEquals(new TextPart("12, -7, 99"), format(context, new int[] { 12, -7, 99 }));

    assertEquals(new TextPart("1, -7, 248"), format(context, new int[] { 1, -7, 248 },
        singletonMap(new MapKeyName("number"), new MapValueString("##00"))));

    formatterService.addFormatter(new NumberFormatter());

    assertEquals(new TextPart("01, -07, 248"), format(context, new int[] { 1, -7, 248 },
        singletonMap(new MapKeyName("number"), new MapValueString("##00"))));

    formatterService.addFormatter(new NamedParameterFormatter() {
      @Override
      public @NotNull String getName() {
        return "hex";
      }

      @Override
      public boolean canFormat(@NotNull Class<?> type) {
        return Integer.class.isAssignableFrom(type);
      }

      @Override
      public @NotNull Text format(@NotNull FormatterContext context, Object value) {
        return value == null ? nullText() : new TextPart(String.format("0x%02x", (Integer)value));
      }

      @Override
      public @NotNull Set<FormattableType> getFormattableTypes() {
        return singleton(new FormattableType(Integer.class));
      }
    });

    assertEquals(new TextPart("0x40, 0xda, 0x2e"), format(context, new int[] { 64, 218, 46 }, "hex"));
  }


  @Test
  public void testObjectArray()
  {
    val registry = createFormatterService(new ArrayFormatter(), new BoolFormatter(), new NumberFormatter());
    val context = new MessageContext(registry, NO_CACHE_INSTANCE, "de-DE");

    assertEquals(new TextPart("Test, wahr, -0006"), format(context, new Object[] { "Test", true, null, -6 },
        singletonMap(new MapKeyName("number"), new MapValueString("0000"))));

    assertEquals(new TextPart("this, is, a, test"), format(context,
        new Object[] { null, "this", null, "is", null, "a", null, "test" }));
  }


  @Test
  public void testEmptyOrNullArray()
  {
    val context = new MessageContext(createFormatterService(new ArrayFormatter()), NO_CACHE_INSTANCE);
    val message = context.getMessageFactory().parse("%{array,null:null,empty:empty}");

    assertEquals("null", message.format(context, context.parameters().with("array", null)));
    assertEquals("empty", message.format(context, context.parameters().with("array", new int[0])));
  }


  @Test
  public void testSeparator()
  {
    val context = new MessageContext(createFormatterService(new ArrayFormatter()), NO_CACHE_INSTANCE);

    assertEquals("1, 2, 3, 4 and 5", context.getMessageFactory()
        .parse("%{c,list-sep:', ',list-sep-last:' and '}")
        .format(context, context.parameters().with("c", new int[] { 1, 2, 3, 4, 5})));

    assertEquals("1.2.3.4.5", context.getMessageFactory()
        .parse("%{c,list-sep:'.'}")
        .format(context, context.parameters().with("c", new long[] { 1, 2, 3, 4, 5 })));
  }
}