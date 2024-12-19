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
import de.sayayi.lib.message.internal.part.TextPart;
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.part.parameter.key.ConfigKey;
import de.sayayi.lib.message.part.parameter.key.ConfigKeyName;
import de.sayayi.lib.message.part.parameter.value.ConfigValue;
import de.sayayi.lib.message.part.parameter.value.ConfigValueString;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

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
  @DisplayName("Formattable types")
  void testFormattableTypes() {
    assertFormatterForType(new ArrayFormatter(), Object[].class);
  }


  @Test
  @DisplayName("Native boolean array")
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
  @DisplayName("Native int array")
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
  @DisplayName("Object array")
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
  @DisplayName("Empty or null array")
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
  @DisplayName("Separator spaces")
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


  private static Stream<Arguments> crossTableParameters()
  {
    return Stream.of(
        Arguments.of("Empty array with max size 0", new String[0], null, 0, null, ""),
        Arguments.of("Array with last separator", new String[] { "A", "B", "C" }, " and ", null, null, "A, B and C"),
        Arguments.of("Array with max size 2 and more value", new String[] { "A", "B", "C" }, null, 2, "...", "A, B, ..."),
        Arguments.of("Array with max size 2 and last separator", new String[] { "A", "B", "C" }, " and ", 2, null, "A and B"),
        Arguments.of("Array with max size 1 and last separator", new String[] { "A", "B", "C" }, " and ", 1, null, "A"),
        Arguments.of("Non-empty array with max size 0 and last separator", new String[] { "A", "B", "C" }, null, 0, null, ""),
        Arguments.of("Non-empty array with max size 0 and more value", new String[] { "A", "B", "C" }, null, 0, "...", "..."),
        Arguments.of("Array without last separator and more value", new String[] { "A", "B", "C" }, null, null, null, "A, B, C"),
        Arguments.of("Array with max size 2", new String[] { "A", "B", "C" }, null, 2, null, "A, B"),
        Arguments.of("Array with max size 1", new String[] { "A", "B", "C" }, null, 1, null, "A"),
        Arguments.of("Non-empty array with max size 0", new String[] { "A", "B", "C" }, null, 0, null, ""),
        Arguments.of("Array with empty element and max size 2", new String[] { "A", "", "C", "D" }, null, 2, null, "A, C")
    );
  }


  @DisplayName("Max size, last separator and more value")
  @ParameterizedTest(name = "{0}")
  @MethodSource("crossTableParameters")
  void crossTable(@NotNull String name, Object array, String listSepLast, Integer listMaxSize, String listValueMore,
                  @NotNull String result)
  {
    val messageFormat = new StringBuilder("%{array");
    if (listSepLast != null)
      messageFormat.append(",list-sep-last:\"").append(listSepLast).append('"');
    if (listMaxSize != null)
      messageFormat.append(",list-max-size:").append(listMaxSize);
    if (listValueMore != null)
      messageFormat.append(",list-value-more:\"").append(listValueMore).append('"');
    messageFormat.append("}");

    val message = MessageSupportFactory
        .create(createFormatterService(new ArrayFormatter()), NO_CACHE_INSTANCE)
        .message(messageFormat.toString())
        .with("array", array);

    assertEquals(result, message.format());
  }
}
