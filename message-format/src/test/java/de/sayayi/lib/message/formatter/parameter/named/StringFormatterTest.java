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
package de.sayayi.lib.message.formatter.parameter.named;

import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.GenericFormatterService;
import de.sayayi.lib.message.formatter.parameter.AbstractSingleTypeParameterFormatter;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatter.MapKeyComparator;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext;
import de.sayayi.lib.message.internal.part.parameter.AbstractFormatterTest;
import de.sayayi.lib.message.part.MapKey.MatchResult;
import de.sayayi.lib.message.part.MessagePart.Text;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.lang.annotation.RetentionPolicy;
import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static de.sayayi.lib.message.part.MapKey.MatchResult.Defined.MISMATCH;
import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static java.text.Collator.IDENTICAL;
import static java.text.Collator.PRIMARY;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests {@link StringFormatter}: formattable types, formatting with and without maps, map key matching and the use of
 * {@code toString()}.
 *
 * @author Jeroen Gremmen
 * @since 0.1.0
 */
@DisplayName("'string' formatter")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class StringFormatterTest extends AbstractFormatterTest
{
  @Test
  public void testFormattableTypes()
  {
    assertFormatterForType(new StringFormatter(), CharSequence.class);
    assertFormatterForType(new StringFormatter(), char[].class);
  }


  @Test
  public void testFormat()
  {
    val messageAccessor = MessageSupportFactory
        .createGeneric()
        .getMessageAccessor();

    assertEquals(noSpaceText("text"), format(messageAccessor, " text "));
    assertEquals(noSpaceText("RUNTIME"), format(messageAccessor, RetentionPolicy.RUNTIME));
    assertEquals(noSpaceText("hello"), format(messageAccessor, new Object() {
      @Override
      public String toString() {
        return " hello";
      }
    }));
  }


  @Test
  public void testFormatter()
  {
    val messageSupport = MessageSupportFactory.createGeneric();

    assertEquals("This is a test 1234", messageSupport
        .message("This is %{a} %{b} %{c}")
        .with("a", " a test ")
        .with("b", null)
        .with("c", 1234)
        .format());
  }


  @Test
  public void testFormatterWithMap()
  {
    val messageSupport = MessageSupportFactory.createGeneric();
    val parameters = new HashMap<String,Object>();

    parameters.put("empty", "");
    parameters.put("null", null);
    parameters.put("spaces", "  ");
    parameters.put("text", "hello  ");

    assertEquals("",
        messageSupport.message("%{empty,!empty:nok}").with(parameters).format());
    assertEquals("ok",
        messageSupport.message("%{empty,empty:ok}").with(parameters).format());
    assertEquals("ok",
        messageSupport.message("%{null,empty:nok,null:ok}").with(parameters).format());
    assertEquals("ok",
        messageSupport.message("%{null,empty:ok}").with(parameters).format());
    assertEquals("ok",
        messageSupport.message("%{spaces,empty:ok}").with(parameters).format());
    assertEquals("ok",
        messageSupport.message("%{spaces,!null:ok}").with(parameters).format());
    assertEquals("hello!",
        messageSupport.message("%{text,null:nok,!empty:'%{text}!'}").with(parameters).format());
    assertEquals("hello!",
        messageSupport.message("%{text,!null:'%{text}!'}").with(parameters).format());
  }


  @Test
  void testMapKey()
  {
    val messageSupport = MessageSupportFactory.shared();

    assertEquals("exact", messageSupport
        .message("%{text,format:choice,'Süd':'exact'}")
        .with("text", "Süd")
        .format());

    assertEquals("lenient", messageSupport
        .message("%{text,format:choice,'SUD':'lenient'}")
        .with("text", "Süd")
        .format());
  }


  @Test
  @DisplayName("Collators are shared by all map keys of a map lookup")
  void testMapKeyCollatorReuse()
  {
    val collatorFormatter = new CollatorCapturingFormatter();
    val formatterService = new GenericFormatterService();
    formatterService.addFormatter(collatorFormatter);
    formatterService.addFormatter(new ChoiceFormatter());

    assertEquals("other", MessageSupportFactory
        .create(formatterService)
        .message("%{v,format:choice,'a':'a','b':'b','c':'c',:'other'}")
        .with("v", new Value())
        .format());

    val collators = collatorFormatter.collators;
    assertEquals(6, collators.size());
    for(var n = 2; n < 6; n++)
      assertSame(collators.get(n % 2), collators.get(n));

    assertNotSame(collators.get(0), collators.get(1));
    assertEquals(IDENTICAL, collators.get(0).getStrength());
    assertEquals(PRIMARY, collators.get(1).getStrength());
  }


  @Test
  void testDefaultToString()
  {
    val messageSupport = MessageSupportFactory.createDefault();
    messageSupport.addMessage("OBJ", "%{object}");

    val object = new Object();
    val objectToString = object.toString();

    // default
    assertEquals(objectToString, messageSupport
        .code("OBJ")
        .with("object", object)
        .format());

    // default
    messageSupport.setDefaultConfig("ignore-default-tostring", false);
    assertEquals(objectToString, messageSupport
        .code("OBJ")
        .with("object", object)
        .format());

    // ignore
    messageSupport.setDefaultConfig("ignore-default-tostring", true);
    assertEquals("", messageSupport
        .code("OBJ")
        .with("object", object)
        .format());

    // ignored empty
    messageSupport.setDefaultConfig("ignore-default-tostring", true);
    assertEquals("(empty)", messageSupport
        .message("%{object,empty:'(empty)'}")
        .with("object", object)
        .format());

    // ignored lambda (hidden class)
    final Runnable lambda = () -> {};
    assertEquals("", messageSupport
        .code("OBJ")
        .with("object", lambda)
        .format());
  }




  private static final class Value {}




  private static final class CollatorCapturingFormatter extends AbstractSingleTypeParameterFormatter<Value>
      implements MapKeyComparator<Value>
  {
    private final List<Collator> collators = new ArrayList<>();


    @Override
    protected @NotNull Text formatValue(@NotNull ParameterFormatterContext context, @NotNull Value value) {
      return noSpaceText("value");
    }


    @Override
    public @NotNull MatchResult compareToStringKey(@NotNull Value value, @NotNull ComparatorContext context)
    {
      collators.add(context.getCollator(IDENTICAL));
      collators.add(context.getCollator(PRIMARY));

      return MISMATCH;
    }


    @Override
    protected @NotNull FormattableType getFormattableType() {
      return new FormattableType(Value.class);
    }
  }
}
