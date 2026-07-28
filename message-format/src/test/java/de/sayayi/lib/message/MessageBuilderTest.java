/*
 * Copyright 2026 Jeroen Gremmen
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
package de.sayayi.lib.message;

import de.sayayi.lib.message.formatter.DefaultFormatterService;
import de.sayayi.lib.message.internal.MessageTemplate;
import de.sayayi.lib.message.part.MessagePart;
import de.sayayi.lib.message.template.Template;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static de.sayayi.lib.message.MessageSupportFactory.shared;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;


/**
 * @author Jeroen Gremmen
 * @since 0.23.0
 */
@DisplayName("MessageBuilder")
class MessageBuilderTest
{
  @Nested
  @DisplayName("MapValueBuilder.message(Consumer)")
  class MapValueBuilderConsumerTest
  {
    @Test
    @DisplayName("mapBool with nested builder message")
    void testMapBoolWithConsumerMessage()
    {
      final var message = MessageBuilder
          .create()
          .parameter("flag")
              .mapBool(true).message(b -> b.text("yes"))
              .mapBool(false).message(b -> b.text("no"))
          .build();

      assertEquals("%{flag,true:yes,false:no}", message.asFormatString(UTF_8));

      final var messageSupport = shared();

      assertEquals("yes", messageSupport.message(message).with("flag", true).format());
      assertEquals("no", messageSupport.message(message).with("flag", false).format());
    }


    @Test
    @DisplayName("mapDefault with nested builder message")
    void testMapDefaultWithConsumerMessage()
    {
      final var message = MessageBuilder
          .create()
          .parameter("name")
              .withFormat("choice")
              .mapNull().message(b -> b.text("stranger"))
              .mapDefault().message(b -> b.text("Hello").parameter("name").spaceBefore())
          .build();

      assertEquals("%{name,format:choice,null:stranger,:'Hello %{name}'}", message.asFormatString(UTF_8));

      final var messageSupport = shared();

      assertEquals("Hello World", messageSupport.message(message).with("name", "World").format());
      assertEquals("stranger", messageSupport.message(message).with("name", null).format());
    }


    @Test
    @DisplayName("mapNumber with nested builder message")
    void testMapNumberWithConsumerMessage()
    {
      final var message = MessageBuilder
          .create()
          .parameter("count")
              .withFormat("choice")
              .mapNumber(1).message(b -> b.text("one item"))
              .mapDefault().message(b -> b.parameter("count").text(" items").spaceBefore())
          .build();

      assertEquals("%{count,format:choice,1:'one item',:'%{count} items'}", message.asFormatString(UTF_8));

      final var messageSupport = shared();

      assertEquals("one item", messageSupport.message(message).with("count", 1).format());
      assertEquals("5 items", messageSupport.message(message).with("count", 5).format());
    }


    @Test
    @DisplayName("mapString with nested builder message")
    void testMapStringWithConsumerMessage()
    {
      final var message = MessageBuilder
          .create()
          .parameter("color")
              .withFormat("choice")
              .mapString("red").message(b -> b.text("Red color"))
              .mapDefault().message(b -> b.text("Unknown color"))
          .build();

      assertEquals("%{color,format:choice,'red':'Red color',:'Unknown color'}", message.asFormatString(UTF_8));

      final var messageSupport = shared();

      assertEquals("Red color", messageSupport.message(message).with("color", "red").format());
      assertEquals("Unknown color", messageSupport.message(message).with("color", "blue").format());
    }


    @Test
    @DisplayName("mapNull with nested builder message")
    void testMapNullWithConsumerMessage()
    {
      final var message = MessageBuilder
          .create()
          .parameter("value")
              .mapNull().message(b -> b.text("nothing"))
              .mapDefault().message(b -> b.parameter("value"))
          .build();

      assertEquals("%{value,null:nothing,:'%{value}'}", message.asFormatString(UTF_8));

      final var messageSupport = shared();

      assertEquals("nothing", messageSupport.message(message).with("value", null).format());
      assertEquals("hello", messageSupport.message(message).with("value", "hello").format());
    }


    @Test
    @DisplayName("mapEmpty with nested builder message")
    void testMapEmptyWithConsumerMessage()
    {
      final var message = MessageBuilder
          .create()
          .parameter("text")
              .mapEmpty().message(b -> b.text("(empty)"))
              .mapDefault().message(b -> b.parameter("text"))
          .build();

      assertEquals("%{text,empty:'(empty)',:'%{text}'}", message.asFormatString(UTF_8));

      final var messageSupport = shared();

      assertEquals("(empty)", messageSupport.message(message).with("text", "").format());
      assertEquals("hi", messageSupport.message(message).with("text", "hi").format());
    }


    @Test
    @DisplayName("mapNumber with relational operator and nested builder message")
    void testMapNumberRelationalWithConsumerMessage()
    {
      final var message = MessageBuilder
          .create()
          .parameter("n")
              .withFormat("choice")
              .mapNumber(0).lt().message(b -> b.text("negative"))
              .mapNumber(0).gt().message(b -> b.text("positive"))
              .mapDefault().message(b -> b.text("zero"))
          .build();

      assertEquals("%{n,format:choice,<0:negative,>0:positive,:zero}", message.asFormatString(UTF_8));

      final var messageSupport = shared();

      assertEquals("negative", messageSupport.message(message).with("n", -5).format());
      assertEquals("positive", messageSupport.message(message).with("n", 10).format());
      assertEquals("zero", messageSupport.message(message).with("n", 0).format());
    }


    @Test
    @DisplayName("Null messageConfigurer throws NullPointerException")
    @SuppressWarnings("DataFlowIssue")
    void testNullConsumerThrowsException()
    {
      assertThrows(NullPointerException.class, () -> MessageBuilder
          .create()
          .parameter("x")
              .mapDefault().message((java.util.function.Consumer<MessageBuilder>)null));
    }
  }




  @Nested
  @DisplayName("Consecutive text part merging")
  class ConsecutiveTextMergingTest
  {
    @Test
    @DisplayName("Two consecutive text parts are merged into one")
    void testTwoConsecutiveTexts()
    {
      final var message = MessageBuilder
          .create()
          .text("Hello")
          .text("World").spaceBefore()
          .build();

      assertEquals(1, message.getMessageParts().length);
      assertInstanceOf(MessagePart.Text.class, message.getMessageParts()[0]);

      assertEquals("Hello World", message.asFormatString(UTF_8));
      assertEquals("Hello World", shared().message(message).format());
    }


    @Test
    @DisplayName("Three consecutive text parts are merged into one")
    void testThreeConsecutiveTexts()
    {
      final var message = MessageBuilder
          .create()
          .text("one")
          .text("two").spaceBefore()
          .text("three").spaceBefore()
          .build();

      assertEquals(1, message.getMessageParts().length);

      assertEquals("one two three", message.asFormatString(UTF_8));
      assertEquals("one two three", shared().message(message).format());
    }


    @Test
    @DisplayName("Text parts around a parameter remain separate")
    void testTextAroundParameter()
    {
      final var message = MessageBuilder
          .create()
          .text("Hello")
          .parameter("name").spaceBefore()
          .text("!")
          .build();

      // text + parameter + text = 3 parts
      assertEquals(3, message.getMessageParts().length);

      assertEquals("Hello %{name}!", message.asFormatString(UTF_8));
      assertEquals("Hello Alice!", shared().message(message).with("name", "Alice").format());
    }


    @Test
    @DisplayName("Multiple text parts before and after a parameter are each merged")
    void testMultipleTextGroupsAroundParameter()
    {
      final var message = MessageBuilder
          .create()
          .text("Dear")
          .text("customer").spaceBefore()
          .parameter("name").spaceBefore()
          .text(",")
          .text("welcome").spaceBefore()
          .text("back!").spaceBefore()
          .build();

      // "Dear customer" (merged) + parameter + ", welcome back!" (merged) = 3 parts
      assertEquals(3, message.getMessageParts().length);

      assertEquals("Dear customer %{name}, welcome back!", message.asFormatString(UTF_8));
      assertEquals("Dear customer Alice, welcome back!",
          shared().message(message).with("name", "Alice").format());
    }


    @Test
    @DisplayName("Single text part produces a single-part message")
    void testSingleText()
    {
      final var message = MessageBuilder
          .create()
          .text("solo")
          .build();

      assertEquals(1, message.getMessageParts().length);

      assertEquals("solo", message.asFormatString(UTF_8));
      assertEquals("solo", shared().message(message).format());
    }


    @Test
    @DisplayName("Text with spaceAfter merges correctly with following text")
    void testSpaceAfterMerge()
    {
      final var message = MessageBuilder
          .create()
          .text("left").spaceAfter()
          .text("right")
          .build();

      assertEquals(1, message.getMessageParts().length);

      assertEquals("left right", message.asFormatString(UTF_8));
      assertEquals("left right", shared().message(message).format());
    }


    @Test
    @DisplayName("Consecutive texts between two parameters are merged")
    void testConsecutiveTextsBetweenParameters()
    {
      final var message = MessageBuilder
          .create()
          .parameter("a")
          .text(",")
          .text("and").spaceBefore().spaceAfter()
          .parameter("b")
          .build();

      // parameter + ", and " (merged) + parameter = 3 parts
      assertEquals(3, message.getMessageParts().length);

      assertEquals("%{a}, and %{b}", message.asFormatString(UTF_8));
      assertEquals("1, and 2",
          shared().message(message).with("a", 1).with("b", 2).format());
    }
  }




  @Nested
  @DisplayName("buildAsTemplate")
  class BuildAsTemplateTest
  {
    @Test
    @DisplayName("Simple text message produces a MessageTemplate")
    void testSimpleTextTemplate()
    {
      final Template template = MessageBuilder
          .create()
          .text("hello")
          .buildAsTemplate();

      assertNotNull(template);
      assertInstanceOf(MessageTemplate.class, template);
      assertEquals("Template(\"hello\")", template.toString());
    }


    @Test
    @DisplayName("Template with parameter part")
    void testTemplateWithParameter()
    {
      final Template template = MessageBuilder
          .create()
          .text("Hello")
          .parameter("name").spaceBefore()
          .text("!")
          .buildAsTemplate();

      final var message = assertInstanceOf(MessageTemplate.class, template).getMessage();

      assertEquals("Hello %{name}!", message.asFormatString(java.nio.charset.StandardCharsets.UTF_8));
    }


    @Test
    @DisplayName("Template wraps a message that formats correctly")
    void testTemplateFormatsCorrectly()
    {
      final Template template = MessageBuilder
          .create()
          .text("Welcome")
          .parameter("user").spaceBefore()
          .buildAsTemplate();

      final var message = assertInstanceOf(MessageTemplate.class, template).getMessage();

      assertEquals("Welcome Alice",
          shared().message(message).with("user", "Alice").format());
    }


    @Test
    @DisplayName("Template from builder with map entries")
    void testTemplateWithMapEntries()
    {
      final Template template = MessageBuilder
          .create()
          .parameter("count")
              .withFormat("choice")
              .mapNumber(1).message("one item")
              .mapDefault().message(b -> b.parameter("count").text(" items").spaceBefore())
          .buildAsTemplate();

      final var message = assertInstanceOf(MessageTemplate.class, template).getMessage();

      assertEquals("%{count,format:choice,1:'one item',:'%{count} items'}",
          message.asFormatString(java.nio.charset.StandardCharsets.UTF_8));
    }


    @Test
    @DisplayName("Empty message produces a template")
    void testEmptyMessageTemplate()
    {
      final Template template = MessageBuilder
          .create()
          .buildAsTemplate();

      assertNotNull(template);
      assertInstanceOf(MessageTemplate.class, template);
    }


    @Test
    @DisplayName("buildAsTemplate from sub-builder finalizes current part")
    void testBuildAsTemplateFromSubBuilder()
    {
      final Template template = MessageBuilder
          .create()
          .text("test")
          .parameter("x").spaceBefore()
              .withFormat("string")
              .buildAsTemplate();

      final var message = assertInstanceOf(MessageTemplate.class, template).getMessage();

      assertEquals("test %{x,format:string}", message.asFormatString(java.nio.charset.StandardCharsets.UTF_8));
    }


    @Test
    @DisplayName("isSame returns true for identical templates built separately")
    void testIsSameForIdenticalTemplates()
    {
      final Template template1 = MessageBuilder
          .create()
          .text("hello")
          .buildAsTemplate();

      final Template template2 = MessageBuilder
          .create()
          .text("hello")
          .buildAsTemplate();

      assertTrue(template1.isSame(template2));
    }


    @Test
    @DisplayName("isSame returns false for different templates")
    void testIsSameForDifferentTemplates()
    {
      final Template template1 = MessageBuilder
          .create()
          .text("hello")
          .buildAsTemplate();

      final Template template2 = MessageBuilder
          .create()
          .text("world")
          .buildAsTemplate();

      assertFalse(template1.isSame(template2));
    }
  }




  @Nested
  @DisplayName("PostFormatterBuilder.withMessage")
  class PostFormatterBuilderWithMessageTest
  {
    @Test
    @DisplayName("withMessage(Message.WithSpaces) sets inner message")
    void testWithMessageObject()
    {
      final var innerMessage = MessageBuilder
          .create()
          .text("hello world")
          .build();

      final var message = MessageBuilder
          .create()
          .postFormatter("case")
              .withMessage(innerMessage)
              .configString("case", "upper")
          .build();

      assertEquals("%(case,'hello world',case:upper)", message.asFormatString(UTF_8));
      assertEquals("HELLO WORLD", shared().message(message).format());
    }


    @Test
    @DisplayName("withMessage(String) parses format string as inner message")
    void testWithMessageString()
    {
      final var message = MessageBuilder
          .create()
          .postFormatter("case")
              .withMessage("hello world")
              .configString("case", "upper")
          .build();

      assertEquals("%(case,'hello world',case:upper)", message.asFormatString(UTF_8));
      assertEquals("HELLO WORLD", shared().message(message).format());
    }


    @Test
    @DisplayName("withMessage(String) with parameter reference")
    void testWithMessageStringContainingParameter()
    {
      final var message = MessageBuilder
          .create()
          .postFormatter("case")
              .withMessage("Hello %{name}")
              .configString("case", "upper")
          .build();

      assertEquals("%(case,'Hello %{name}',case:upper)", message.asFormatString(UTF_8));
      assertEquals("HELLO ALICE", shared().message(message).with("name", "Alice").format());
    }


    @Test
    @DisplayName("withMessage(Consumer) builds inner message via nested builder")
    void testWithMessageConsumer()
    {
      final var message = MessageBuilder
          .create()
          .postFormatter("case")
              .withMessage(b -> b.text("hello world"))
              .configString("case", "upper")
          .build();

      assertEquals("%(case,'hello world',case:upper)", message.asFormatString(UTF_8));
      assertEquals("HELLO WORLD", shared().message(message).format());
    }


    @Test
    @DisplayName("withMessage(Consumer) with parameter in nested builder")
    void testWithMessageConsumerWithParameter()
    {
      final var message = MessageBuilder
          .create()
          .postFormatter("case")
              .withMessage(b -> b.text("Hello").parameter("name").spaceBefore())
              .configString("case", "upper")
          .build();

      assertEquals("%(case,'Hello %{name}',case:upper)", message.asFormatString(UTF_8));
      assertEquals("HELLO ALICE", shared().message(message).with("name", "Alice").format());
    }


    @Test
    @DisplayName("withMessage defaults to empty message when not set")
    void testDefaultEmptyMessage()
    {
      final var message = MessageBuilder
          .create()
          .postFormatter("case")
              .configString("case", "upper")
          .build();

      assertEquals("", shared().message(message).format());
    }


    @Test
    @DisplayName("withMessage(Consumer) with null throws NullPointerException")
    @SuppressWarnings("DataFlowIssue")
    void testWithMessageConsumerNullThrows()
    {
      assertThrows(NullPointerException.class, () -> MessageBuilder
          .create()
          .postFormatter("case")
              .withMessage((Consumer<MessageBuilder>)null));
    }


    @Test
    @DisplayName("withMessage(Message.WithSpaces) with null throws NullPointerException")
    @SuppressWarnings("DataFlowIssue")
    void testWithMessageObjectNullThrows()
    {
      assertThrows(NullPointerException.class, () -> MessageBuilder
          .create()
          .postFormatter("case")
              .withMessage((Message.WithSpaces)null));
    }


    @Test
    @DisplayName("withMessage(String) with null throws NullPointerException")
    @SuppressWarnings("DataFlowIssue")
    void testWithMessageStringNullThrows()
    {
      assertThrows(NullPointerException.class, () -> MessageBuilder
          .create()
          .postFormatter("case")
              .withMessage((String)null));
    }


    @Test
    @DisplayName("Post-formatter with spaceBefore and spaceAfter")
    void testWithSpacing()
    {
      final var message = MessageBuilder
          .create()
          .text("Result:")
          .postFormatter("case")
              .withMessage("hello")
              .configString("case", "upper")
              .spaceBefore()
          .text("!")
          .build();

      assertEquals("Result: %(case,'hello',case:upper)!", message.asFormatString(UTF_8));
      assertEquals("Result: HELLO!", shared().message(message).format());
    }


    @Test
    @DisplayName("Last withMessage call wins")
    void testLastWithMessageWins()
    {
      final var message = MessageBuilder
          .create()
          .postFormatter("case")
              .withMessage("first")
              .withMessage("second")
              .configString("case", "upper")
          .build();

      assertEquals("SECOND", shared().message(message).format());
    }
  }




  @Nested
  @DisplayName("TemplateBuilder.withDefaultParameter")
  class TemplateBuilderWithDefaultParameterTest
  {
    @Test
    @DisplayName("withDefaultParameter(String, String) provides string default")
    void testWithDefaultParameterString()
    {
      final var cms = createMessageSupportWithTemplate("greeting", "Hello %{name}");
      final var message = MessageBuilder
          .create()
          .template("greeting")
              .withDefaultParameter("name", "World")
          .build();

      assertEquals("Hello World", cms.message(message).format());
    }


    @Test
    @DisplayName("withDefaultParameter(String, String) is overridden by explicit parameter")
    void testWithDefaultParameterStringOverridden()
    {
      final var cms = createMessageSupportWithTemplate("greeting", "Hello %{name}");
      final var message = MessageBuilder
          .create()
          .template("greeting")
              .withDefaultParameter("name", "World")
          .build();

      assertEquals("Hello Alice", cms.message(message).with("name", "Alice").format());
    }


    @Test
    @DisplayName("withDefaultParameter(String, boolean) provides boolean default")
    void testWithDefaultParameterBoolean()
    {
      final var cms = createMessageSupportWithTemplate("status",
          "%{active,true:enabled,false:disabled}");
      final var message = MessageBuilder
          .create()
          .template("status")
              .withDefaultParameter("active", true)
          .build();

      assertEquals("enabled", cms.message(message).format());
    }


    @Test
    @DisplayName("withDefaultParameter(String, boolean) false value")
    void testWithDefaultParameterBooleanFalse()
    {
      final var cms = createMessageSupportWithTemplate("status",
          "%{active,true:enabled,false:disabled}");
      final var message = MessageBuilder
          .create()
          .template("status")
              .withDefaultParameter("active", false)
          .build();

      assertEquals("disabled", cms.message(message).format());
    }


    @Test
    @DisplayName("withDefaultParameter(String, long) provides numeric default")
    void testWithDefaultParameterLong()
    {
      final var cms = createMessageSupportWithTemplate("count-msg",
          "%{count,format:choice,1:'one item',:'%{count} items'}");
      final var message = MessageBuilder
          .create()
          .template("count-msg")
              .withDefaultParameter("count", 42)
          .build();

      assertEquals("42 items", cms.message(message).format());
    }


    @Test
    @DisplayName("withDefaultParameter(String, long) is overridden by explicit parameter")
    void testWithDefaultParameterLongOverridden()
    {
      final var cms = createMessageSupportWithTemplate("count-msg",
          "%{count,format:choice,1:'one item',:'%{count} items'}");
      final var message = MessageBuilder
          .create()
          .template("count-msg")
              .withDefaultParameter("count", 42)
          .build();

      assertEquals("one item", cms.message(message).with("count", 1).format());
    }


    @Test
    @DisplayName("withDefaultParameter(String, Message.WithSpaces) provides message default")
    void testWithDefaultParameterMessage()
    {
      final var cms = createMessageSupportWithTemplate("wrapper",
          "%{content,!null:'has content',null:'no content'}");
      final var innerMessage = MessageBuilder
          .create()
          .text("hello world")
          .build();

      final var message = MessageBuilder
          .create()
          .template("wrapper")
              .withDefaultParameter("content", innerMessage)
          .build();

      assertEquals("has content", cms.message(message).format());
    }


    @Test
    @DisplayName("withDefaultParameter(String, Consumer) builds message via nested builder")
    void testWithDefaultParameterConsumer()
    {
      final var cms = createMessageSupportWithTemplate("wrapper",
          "%{content,!null:'has content',null:'no content'}");
      final var message = MessageBuilder
          .create()
          .template("wrapper")
              .withDefaultParameter("content", b -> b.text("built via consumer"))
          .build();

      assertEquals("has content", cms.message(message).format());
    }


    @Test
    @DisplayName("withDefaultParameter(String, Consumer) with parameter in nested message")
    void testWithDefaultParameterConsumerWithParameter()
    {
      final var cms = createMessageSupportWithTemplate("wrapper",
          "%{content,!null:'has content',null:'no content'}");
      final var message = MessageBuilder
          .create()
          .template("wrapper")
              .withDefaultParameter("content", b -> b.text("Hello").parameter("user").spaceBefore())
          .build();

      assertEquals("has content", cms.message(message).with("user", "Admin").format());
    }


    @Test
    @DisplayName("Multiple default parameters on same template")
    void testMultipleDefaultParameters()
    {
      final var cms = createMessageSupportWithTemplate("multi",
          "%{greeting} %{name}");
      final var message = MessageBuilder
          .create()
          .template("multi")
              .withDefaultParameter("greeting", "Hi")
              .withDefaultParameter("name", "there")
          .build();

      assertEquals("Hi there", cms.message(message).format());
    }


    @Test
    @DisplayName("withDefaultParameter with null name throws NullPointerException")
    @SuppressWarnings("DataFlowIssue")
    void testWithDefaultParameterNullNameThrows()
    {
      assertThrows(NullPointerException.class, () -> MessageBuilder
          .create()
          .template("t")
              .withDefaultParameter(null, "value"));
    }


    @Test
    @DisplayName("withDefaultParameter(String) with null value throws NullPointerException")
    @SuppressWarnings("DataFlowIssue")
    void testWithDefaultParameterNullStringValueThrows()
    {
      assertThrows(NullPointerException.class, () -> MessageBuilder
          .create()
          .template("t")
              .withDefaultParameter("name", (String)null));
    }


    @Test
    @DisplayName("withDefaultParameter(Message.WithSpaces) with null value throws NullPointerException")
    @SuppressWarnings("DataFlowIssue")
    void testWithDefaultParameterNullMessageValueThrows()
    {
      assertThrows(NullPointerException.class, () -> MessageBuilder
          .create()
          .template("t")
              .withDefaultParameter("name", (Message.WithSpaces)null));
    }


    @Test
    @DisplayName("withDefaultParameter(Consumer) with null consumer throws NullPointerException")
    @SuppressWarnings("DataFlowIssue")
    void testWithDefaultParameterNullConsumerThrows()
    {
      assertThrows(NullPointerException.class, () -> MessageBuilder
          .create()
          .template("t")
              .withDefaultParameter("name", (Consumer<MessageBuilder>)null));
    }


    @Test
    @DisplayName("withDefaultParameter with invalid name throws IllegalArgumentException")
    void testWithDefaultParameterInvalidNameThrows()
    {
      assertThrows(IllegalArgumentException.class, () -> MessageBuilder
          .create()
          .template("t")
              .withDefaultParameter("INVALID NAME!", "value"));
    }


    @Test
    @DisplayName("Template with default parameter serializes correctly")
    void testDefaultParameterSerialization()
    {
      final var message = MessageBuilder
          .create()
          .template("tpl")
              .withDefaultParameter("name", "World")
          .build();

      final var formatString = message.asFormatString(UTF_8);

      assertTrue(formatString.contains("%[tpl"), "Expected template reference in: " + formatString);
      assertTrue(formatString.contains("name="), "Expected default parameter in: " + formatString);
    }


    @Test
    @DisplayName("Template with spaceBefore and default parameter")
    void testTemplateWithSpacingAndDefault()
    {
      final var cms = createMessageSupportWithTemplate("suffix",
          "%{value}");
      final var message = MessageBuilder
          .create()
          .text("Result:")
          .template("suffix")
              .withDefaultParameter("value", "ok")
              .spaceBefore()
          .build();

      assertEquals("Result: ok", cms.message(message).format());
    }


    @Test
    @DisplayName("withParameterDelegate maps template param to message param")
    void testWithParameterDelegate()
    {
      final var cms = createMessageSupportWithTemplate("greeting",
          "Hello %{name}");
      final var message = MessageBuilder
          .create()
          .template("greeting")
              .withParameterDelegate("name", "user")
          .build();

      assertEquals("Hello Alice", cms.message(message).with("user", "Alice").format());
    }


    @Contract(pure = true)
    private @NotNull MessageSupport createMessageSupportWithTemplate(
        @NotNull String templateName, @NotNull @Language("MessageFormat") String templateFormat)
    {
      final var messageFactory = MessageFactory.getSharedInstance();

      return MessageSupportFactory
          .create(DefaultFormatterService.getSharedInstance(), messageFactory)
          .addTemplate(templateName, messageFactory.parseTemplate(templateFormat))
          .seal();
    }
  }
}
