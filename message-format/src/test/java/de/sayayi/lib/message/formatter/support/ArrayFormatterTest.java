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
package de.sayayi.lib.message.formatter.support;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.MessageContext;
import de.sayayi.lib.message.MessageContext.Parameters;
import de.sayayi.lib.message.data.Data;
import de.sayayi.lib.message.data.DataMap;
import de.sayayi.lib.message.data.DataString;
import de.sayayi.lib.message.data.map.MapKey;
import de.sayayi.lib.message.data.map.MapKeyBool;
import de.sayayi.lib.message.data.map.MapValue;
import de.sayayi.lib.message.data.map.MapValueMessage;
import de.sayayi.lib.message.formatter.GenericFormatterService;
import de.sayayi.lib.message.formatter.NamedParameterFormatter;
import de.sayayi.lib.message.internal.part.MessagePart.Text;
import de.sayayi.lib.message.internal.part.TextPart;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static java.util.Collections.emptySortedSet;
import static org.junit.Assert.assertEquals;


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
    GenericFormatterService registry = new GenericFormatterService();
    registry.addFormatter(new ArrayFormatter());
    registry.addFormatter(new BoolFormatter());

    final MessageContext context = new MessageContext(registry, NO_CACHE_INSTANCE, "de-DE");
    Parameters noParameters = context.noParameters();

    assertEquals(new TextPart("wahr, falsch, wahr"), registry.getFormatter(null, boolean[].class)
        .format(context, new boolean[] { true, false, true }, "bool", noParameters, null));

    DataMap booleanMap = new DataMap(new HashMap<MapKey, MapValue>() {
      {
        put(new MapKeyBool(true), new MapValueMessage(new Message.WithSpaces() {
          @Override public String format(@NotNull MessageContext context, @NotNull MessageContext.Parameters parameters) { return "YES"; }
          @Override public boolean hasParameters() { return false; }
          @NotNull
          @Override public SortedSet<String> getParameterNames() { return emptySortedSet(); }
          @Override public boolean isSpaceBefore() { return false; }
          @Override public boolean isSpaceAfter() { return false; }
        }));

        put(new MapKeyBool(false), new MapValueMessage(new Message.WithSpaces() {
          @Override public String format(@NotNull MessageContext context, @NotNull MessageContext.Parameters parameters) { return "NO"; }
          @Override public boolean hasParameters() { return false; }
          @NotNull
          @Override public SortedSet<String> getParameterNames() { return emptySortedSet(); }
          @Override public boolean isSpaceBefore() { return false; }
          @Override public boolean isSpaceAfter() { return false; }
        }));
      }
    });

    assertEquals(new TextPart("NO, YES"), registry.getFormatter(null, boolean[].class)
        .format(context, new boolean[] { false, true }, null, noParameters, booleanMap));

    assertEquals(TextPart.EMPTY, registry.getFormatter(null, boolean[].class)
        .format(context, new boolean[0], null, noParameters, null));

    registry.addFormatter(new NamedParameterFormatter() {
      @NotNull
      @Override
      public Text format(@NotNull MessageContext context, Object value, String format, @NotNull MessageContext.Parameters parameters, Data data) {
        return (value == null) ? null : new TextPart((Boolean)value ? "1" : "0");
      }

      @NotNull
      @Override
      public String getName() {
        return "bool";
      }

      @NotNull
      @Override
      public Set<Class<?>> getFormattableTypes() {
        return new HashSet<>(Arrays.asList(Boolean.class, boolean.class));
      }

      @Override
      public int getPriority() {
        return 1;
      }
    });

    assertEquals(new TextPart("1, 1, 0, 1, 0, 0, 0"), registry.getFormatter(null, boolean[].class)
        .format(context, new boolean[] { true, true, false, true, false, false, false }, "bool", noParameters, null));
  }


  @Test
  public void testIntegerArray()
  {
    GenericFormatterService registry = new GenericFormatterService();
    registry.addFormatter(new ArrayFormatter());

    final MessageContext context = new MessageContext(registry, NO_CACHE_INSTANCE, "de-DE");
    Parameters noParameters = context.noParameters();

    assertEquals(new TextPart("12, -7, 99"), registry.getFormatter(null, int[].class)
        .format(context, new int[] { 12, -7, 99 }, null , noParameters, null));

    assertEquals(new TextPart("1, -7, 248"), registry.getFormatter(null, int[].class)
        .format(context, new int[] { 1, -7, 248 }, null , noParameters, new DataString("##00")));

    registry.addFormatter(new NumberFormatter());

    assertEquals(new TextPart("01, -07, 248"), registry.getFormatter(null, int[].class)
        .format(context, new int[] { 1, -7, 248 }, null , noParameters, new DataString("##00")));

    registry.addFormatter(new NamedParameterFormatter() {
      @NotNull
      @Override
      public String getName() {
        return "hex";
      }

      @NotNull
      @SuppressWarnings("RedundantCast")
      @Override
      public Text format(@NotNull MessageContext context, Object value, String format, @NotNull MessageContext.Parameters parameters, Data data) {
        return (value == null) ? null : new TextPart(String.format("0x%02x", (Integer)value));
      }

      @NotNull
      @Override
      public Set<Class<?>> getFormattableTypes() {
        return Collections.singleton(Integer.class);
      }

      @Override
      public int getPriority() {
        return 0;
      }
    });

    assertEquals(new TextPart("0x40, 0xda, 0x2e"), registry.getFormatter(null, int[].class)
        .format(context, new int[] { 64, 218, 46 }, "hex" , noParameters, null));
  }


  @Test
  public void testObjectArray()
  {
    GenericFormatterService registry = new GenericFormatterService();
    registry.addFormatter(new ArrayFormatter());
    registry.addFormatter(new BoolFormatter());
    registry.addFormatter(new NumberFormatter());

    final MessageContext context = new MessageContext(registry, NO_CACHE_INSTANCE, "de-DE");
    Parameters noParameters = context.noParameters();

    assertEquals(new TextPart("Test, wahr, -0006"), registry.getFormatter(null, int[].class)
        .format(context, new Object[] { "Test", true, null, -6 }, null , noParameters, new DataString("0000")));

    assertEquals(new TextPart("this, is, a, test"), registry.getFormatter(null, int[].class)
        .format(context, new Object[] { null, "this", null, "is", null, "a", null, "test" }, null , noParameters, null));
  }


  @Test
  public void testEmptyOrNullArray()
  {
    GenericFormatterService registry = new GenericFormatterService();
    registry.addFormatter(new ArrayFormatter());

    final MessageContext context = new MessageContext(registry, NO_CACHE_INSTANCE);

    Message message = context.getMessageFactory().parse("%{array,{null:'null',empty:'empty'}}");

    assertEquals("null", message.format(context, context.parameters().with("array", null)));
    assertEquals("empty", message.format(context, context.parameters().with("array", new int[0])));
  }
}
