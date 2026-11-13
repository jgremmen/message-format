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
package de.sayayi.lib.message.formatter.parameter.runtime;

import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.parameter.NamedParameterFormatter;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext;
import de.sayayi.lib.message.formatter.parameter.named.BoolFormatter;
import de.sayayi.lib.message.internal.part.TextPart;
import de.sayayi.lib.message.internal.part.parameter.AbstractFormatterTest;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueMessage;
import de.sayayi.lib.message.internal.part.typedvalue.TypedValueString;
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.part.TypedValue;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static de.sayayi.lib.message.part.TextPartFactory.nullText;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Tests {@link ArrayFormatter}: formattable types, primitive and object arrays. The list configuration shared with
 * {@link IterableFormatter} is tested by {@link ListFormatterConfigTest}.
 *
 * @author Jeroen Gremmen
 * @since 0.1.0
 */
@DisplayName("Array formatter")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class ArrayFormatterTest extends AbstractFormatterTest
{
  @Test
  @DisplayName("Formattable types")
  void testFormattableTypes() {
    assertFormatterForType(new ArrayFormatter(), Object[].class);
  }


  @Test
  @DisplayName("Native boolean array")
  void testBooleanArray()
  {
    val formatterService = createFormatterService(new ArrayFormatter(), new BoolFormatter());
    val messageAccessor = MessageSupportFactory
        .create(formatterService)
        .setLocale("de-DE")
        .getMessageAccessor();
    val messageFactory = messageAccessor.getMessageFactory();

    val map = Map.<String,TypedValue<?>>of(
        "list-value", new TypedValueMessage(messageFactory.parseMessage("%{value,true:wahr,false:falsch}")));

    assertEquals(new TextPart("wahr, falsch, wahr"),
        format(messageAccessor, new boolean[] { true, false, true }, map, Map.of()));

    val booleanMap = Map.<String,TypedValue<?>>of(
        "list-value", new TypedValueMessage(messageFactory.parseMessage("%{value,true:YES,false:NO}")));

    assertEquals(new TextPart("NO, YES"),
        format(messageAccessor, new boolean[] { false, true }, booleanMap, Map.of()));
    assertEquals(TextPart.EMPTY, format(messageAccessor, new boolean[0]));

    formatterService.addFormatter(new NamedParameterFormatter() {
      @Override
      public @NotNull Text format(@NotNull ParameterFormatterContext context, Object value) {
        return value == null ? nullText() : new TextPart((Boolean)value ? "1" : "0");
      }

      @Override
      public boolean canFormat(@NotNull Class<?> type) {
        return Boolean.class.isAssignableFrom(type) || boolean.class.isAssignableFrom(type);
      }

      @Override
      public @NotNull String getName() {
        return "bool-binary";
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
        new boolean[] { true, true, false, true, false, false, false }, "bool-binary"));
  }


  @Test
  @DisplayName("Native int array")
  void testIntegerArray()
  {
    val formatterService = createFormatterService(new ArrayFormatter());
    val messageAccessor = MessageSupportFactory
        .create(formatterService)
        .setLocale("de-DE")
        .getMessageAccessor();
    val messageFactory = messageAccessor.getMessageFactory();

    assertEquals(new TextPart("12, -7, 99"), format(messageAccessor, new int[] { 12, -7, 99 }));

    assertEquals(new TextPart("1, -7, 248"), format(messageAccessor, new int[] { 1, -7, 248 },
        Map.of("number", new TypedValueString(messageFactory, "##00")), Map.of()));

    formatterService.addFormatter(new NumberFormatter());

    assertEquals(new TextPart("01, -07, 248"), format(messageAccessor, new int[] { 1, -7, 248 },
        Map.of("list-value", new TypedValueMessage(messageFactory.parseMessage("%{value,number:'##00'}"))),
        Map.of()));

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
      public @NotNull Text format(@NotNull ParameterFormatterContext context, Object value) {
        return value == null ? nullText() : new TextPart(String.format("0x%02x", (Integer)value));
      }

      @Override
      public @NotNull Set<FormattableType> getFormattableTypes() {
        return Set.of(new FormattableType(Integer.class));
      }
    });

    assertEquals(new TextPart("0x40, 0xda, 0x2e"),
        format(messageAccessor, new int[] { 64, 218, 46 },
            Map.of("list-value", new TypedValueMessage(messageFactory.parseMessage("%{value,format:hex}"))),
            Map.of()));
  }


  @Test
  @DisplayName("Object array")
  void testObjectArray()
  {
    val registry = createFormatterService(
        new ArrayFormatter(),
        new BoolFormatter(),
        new NumberFormatter());
    val messageAccessor = MessageSupportFactory
        .create(registry)
        .setLocale("de-DE")
        .getMessageAccessor();
    val messageFactory = messageAccessor.getMessageFactory();

    val map = new HashMap<String,TypedValue<?>>();
    map.put("list-value",
        new TypedValueMessage(messageFactory.parseMessage("%{value,number:'0000',true:wahr,false:falsch}")));

    assertEquals(new TextPart("Test, wahr, -0006"), format(messageAccessor,
        new Object[] { "Test", true, null, -6 }, map, Map.of()));
    assertEquals(new TextPart("this, is, a, test"), format(messageAccessor,
        new Object[] { null, "this", null, "is", null, "a", null, "test" }));
  }
}
