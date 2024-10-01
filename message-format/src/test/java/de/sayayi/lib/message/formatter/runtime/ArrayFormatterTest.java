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
package de.sayayi.lib.message.formatter.runtime;

import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.AbstractFormatterTest;
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.formatter.NamedParameterFormatter;
import de.sayayi.lib.message.formatter.named.BoolFormatter;
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.part.TextPart;
import de.sayayi.lib.message.part.parameter.key.ConfigKey;
import de.sayayi.lib.message.part.parameter.key.ConfigKeyName;
import de.sayayi.lib.message.part.parameter.value.ConfigValue;
import de.sayayi.lib.message.part.parameter.value.ConfigValueString;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static de.sayayi.lib.message.part.TextPartFactory.nullText;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Jeroen Gremmen
 */
@DisplayName("Array formatter")
class ArrayFormatterTest extends AbstractFormatterTest
{
  @Test
  void testFormattableTypes() {
    assertFormatterForType(new ArrayFormatter(), Object[].class);
  }


  @Test
  void testBooleanArray()
  {
    val formatterService = createFormatterService(new ArrayFormatter(), new BoolFormatter());
    val messageAccessor = MessageSupportFactory.create(formatterService, NO_CACHE_INSTANCE)
        .setLocale("de-DE")
        .getMessageAccessor();

    val map = Map.<ConfigKey,ConfigValue>of(
        new ConfigKeyName("list-value"),
        new ConfigValueString("%{value,true:wahr,false:falsch}"));

    assertEquals(new TextPart("wahr, falsch, wahr"),
        format(messageAccessor, new boolean[] { true, false, true }, map));

    val booleanMap = Map.<ConfigKey,ConfigValue>of(
        new ConfigKeyName("list-value"),
        new ConfigValueString("%{value,true:YES,false:NO}"));

    assertEquals(new TextPart("NO, YES"),
        format(messageAccessor, new boolean[] { false, true }, booleanMap));
    assertEquals(TextPart.EMPTY, format(messageAccessor, new boolean[0]));

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
        return Set.of(
            new FormattableType(Boolean.class, 10),
            new FormattableType(boolean.class, 10));
      }
    });

    assertEquals(new TextPart("1, 1, 0, 1, 0, 0, 0"), format(messageAccessor,
        new boolean[] { true, true, false, true, false, false, false }, "bool"));
  }


  @Test
  void testIntegerArray()
  {
    val formatterService = createFormatterService(new ArrayFormatter());
    val messageAccessor = MessageSupportFactory.create(formatterService, NO_CACHE_INSTANCE)
        .setLocale("de-DE")
        .getMessageAccessor();

    assertEquals(new TextPart("12, -7, 99"), format(messageAccessor, new int[] { 12, -7, 99 }));

    assertEquals(new TextPart("1, -7, 248"), format(messageAccessor, new int[] { 1, -7, 248 },
        Map.of(new ConfigKeyName("number"), new ConfigValueString("##00"))));

    formatterService.addFormatter(new NumberFormatter());

    assertEquals(new TextPart("01, -07, 248"), format(messageAccessor, new int[] { 1, -7, 248 },
        Map.of(new ConfigKeyName("list-value"), new ConfigValueString("%{value,number:'##00'}"))));

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
        return Set.of(new FormattableType(Integer.class));
      }
    });

    assertEquals(new TextPart("0x40, 0xda, 0x2e"),
        format(messageAccessor, new int[] { 64, 218, 46 },
            Map.of(new ConfigKeyName("list-value"), new ConfigValueString("%{value,hex}"))));
  }


  @Test
  void testObjectArray()
  {
    val registry = createFormatterService(
        new ArrayFormatter(),
        new BoolFormatter(),
        new NumberFormatter());
    val messageAccessor = MessageSupportFactory.create(registry, NO_CACHE_INSTANCE)
        .setLocale("de-DE")
        .getMessageAccessor();

    val map = new HashMap<ConfigKey, ConfigValue>();
    map.put(new ConfigKeyName("list-value"),
        new ConfigValueString("%{value,number:'0000',true:wahr,false:falsch}"));

    assertEquals(new TextPart("Test, wahr, -0006"), format(messageAccessor,
        new Object[] { "Test", true, null, -6 }, map));
    assertEquals(new TextPart("this, is, a, test"), format(messageAccessor,
        new Object[] { null, "this", null, "is", null, "a", null, "test" }));
  }


  @Test
  void testEmptyOrNullArray()
  {
    val messageSupport = MessageSupportFactory.create(
        createFormatterService(new ArrayFormatter()), NO_CACHE_INSTANCE);
    val message = messageSupport.message("%{array,null:null,empty:empty}").getMessage();

    assertEquals("null",
        messageSupport.message(message).with("array", null).format());
    assertEquals("empty",
        messageSupport.message(message).with("array", new int[0]).format());
  }


  @Test
  void testSeparator()
  {
    val messageSupport = MessageSupportFactory.create(
        createFormatterService(new ArrayFormatter()), NO_CACHE_INSTANCE);

    assertEquals("1, 2, 3, 4 and 5", messageSupport
        .message("%{c,list-sep:', ',list-sep-last:' and '}")
        .with("c", new int[] { 1, 2, 3, 4, 5})
        .format());

    assertEquals("1.2.3.4.5", messageSupport
        .message("%{c,list-sep:'.'}")
        .with("c", new long[] { 1, 2, 3, 4, 5 })
        .format());
  }
}
