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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Consumer;
import java.util.stream.Stream;

import static de.sayayi.lib.message.MessageSupportFactory.shared;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;


/**
 * Tests {@link MessageBuilder}: programmatic construction of messages from text, parameter and map parts, including
 * merging of consecutive text parts and handling of invalid arguments.
 *
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
    void mapBoolWithConsumerMessage()
    {
      final var message = MessageBuilder
          .create()
          .parameter("flag")
              .mapBool(true).message(b -> b.text("yes"))
              .mapBool(false).message(b -> b.text("no"))
          .build();

      assertMessage(message, "%{flag,true:yes,false:no}", "yes", "flag", true);
      assertEquals("no", shared().message(message).with("flag", false).format());
    }


    @Test
    @DisplayName("mapDefault with nested builder message")
    void mapDefaultWithConsumerMessage()
    {
      final var message = MessageBuilder
          .create()
          .parameter("name")
              .withFormat("choice")
              .mapNull().message(b -> b.text("stranger"))
              .mapDefault().message(b -> b.text("Hello").parameter("name").spaceBefore())
          .build();

      assertMessage(message, "%{name,format:choice,null:stranger,:'Hello %{name}'}", "Hello World", "name", "World");
      assertEquals("stranger", shared().message(message).with("name", null).format());
    }


    @Test
    @DisplayName("mapNumber with nested builder message")
    void mapNumberWithConsumerMessage()
    {
      final var message = MessageBuilder
          .create()
          .parameter("count")
              .withFormat("choice")
              .mapNumber(1).message(b -> b.text("one item"))
              .mapDefault().message(b -> b.parameter("count").text(" items").spaceBefore())
          .build();

      assertMessage(message, "%{count,format:choice,1:'one item',:'%{count} items'}", "one item", "count", 1);
      assertEquals("5 items", shared().message(message).with("count", 5).format());
    }


    @Test
    @DisplayName("mapString with nested builder message")
    void mapStringWithConsumerMessage()
    {
      final var message = MessageBuilder
          .create()
          .parameter("color")
              .withFormat("choice")
              .mapString("red").message(b -> b.text("Red color"))
              .mapDefault().message(b -> b.text("Unknown color"))
          .build();

      assertMessage(message, "%{color,format:choice,'red':'Red color',:'Unknown color'}", "Red color", "color", "red");
      assertEquals("Unknown color", shared().message(message).with("color", "blue").format());
    }


    @Test
    @DisplayName("mapNull with nested builder message")
    void mapNullWithConsumerMessage()
    {
      final var message = MessageBuilder
          .create()
          .parameter("value")
              .mapNull().message(b -> b.text("nothing"))
              .mapDefault().message(b -> b.parameter("value"))
          .build();

      assertMessage(message, "%{value,null:nothing,:'%{value}'}", "nothing", "value", null);
      assertEquals("hello", shared().message(message).with("value", "hello").format());
    }


    @Test
    @DisplayName("mapEmpty with nested builder message")
    void mapEmptyWithConsumerMessage()
    {
      final var message = MessageBuilder
          .create()
          .parameter("text")
              .mapEmpty().message(b -> b.text("(empty)"))
              .mapDefault().message(b -> b.parameter("text"))
          .build();

      assertMessage(message, "%{text,empty:'(empty)',:'%{text}'}", "(empty)", "text", "");
      assertEquals("hi", shared().message(message).with("text", "hi").format());
    }


    @Test
    @DisplayName("mapNumber with relational operator and nested builder message")
    void mapNumberRelationalWithConsumerMessage()
    {
      final var message = MessageBuilder
          .create()
          .parameter("n")
              .withFormat("choice")
              .mapNumber(0).lt().message(b -> b.text("negative"))
              .mapNumber(0).gt().message(b -> b.text("positive"))
              .mapDefault().message(b -> b.text("zero"))
          .build();

      assertMessage(message, "%{n,format:choice,<0:negative,>0:positive,:zero}", "negative", "n", -5);
      assertEquals("positive", shared().message(message).with("n", 10).format());
      assertEquals("zero", shared().message(message).with("n", 0).format());
    }


    @Test
    @DisplayName("Null messageConfigurer throws NullPointerException")
    @SuppressWarnings("DataFlowIssue")
    void nullConsumerThrowsException()
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
    void twoConsecutiveTexts()
    {
      final var message = MessageBuilder
          .create()
          .text("Hello")
          .text("World").spaceBefore()
          .build();

      assertEquals(1, message.getMessageParts().length);
      assertInstanceOf(MessagePart.Text.class, message.getMessageParts()[0]);

      assertMessage(message, "Hello World", "Hello World");
    }


    @Test
    @DisplayName("Three consecutive text parts are merged into one")
    void threeConsecutiveTexts()
    {
      final var message = MessageBuilder
          .create()
          .text("one")
          .text("two").spaceBefore()
          .text("three").spaceBefore()
          .build();

      assertEquals(1, message.getMessageParts().length);

      assertMessage(message, "one two three", "one two three");
    }


    @Test
    @DisplayName("Text parts around a parameter remain separate")
    void textAroundParameter()
    {
      final var message = MessageBuilder
          .create()
          .text("Hello")
          .parameter("name").spaceBefore()
          .text("!")
          .build();

      // text + parameter + text = 3 parts
      assertEquals(3, message.getMessageParts().length);

      assertMessage(message, "Hello %{name}!", "Hello Alice!", "name", "Alice");
    }


    @Test
    @DisplayName("Multiple text parts before and after a parameter are each merged")
    void multipleTextGroupsAroundParameter()
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

      assertMessage(
          message,
          "Dear customer %{name}, welcome back!",
          "Dear customer Alice, welcome back!",
          "name", "Alice");
    }


    @Test
    @DisplayName("Single text part produces a single-part message")
    void singleText()
    {
      final var message = MessageBuilder
          .create()
          .text("solo")
          .build();

      assertEquals(1, message.getMessageParts().length);

      assertMessage(message, "solo", "solo");
    }


    @Test
    @DisplayName("Text with spaceAfter merges correctly with following text")
    void spaceAfterMerge()
    {
      final var message = MessageBuilder
          .create()
          .text("left").spaceAfter()
          .text("right")
          .build();

      assertEquals(1, message.getMessageParts().length);

      assertMessage(message, "left right", "left right");
    }


    @Test
    @DisplayName("Consecutive texts between two parameters are merged")
    void consecutiveTextsBetweenParameters()
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

      assertMessage(message, "%{a}, and %{b}", "1, and 2", "a", 1, "b", 2);
    }
  }




  @Nested
  @DisplayName("buildAsTemplate")
  class BuildAsTemplateTest
  {
    @Test
    @DisplayName("Simple text message produces a MessageTemplate")
    void simpleTextTemplate()
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
    @DisplayName("Template references are not allowed in a template built by the builder")
    void nestedTemplateReferenceIsRejected()
    {
      final var builder = MessageBuilder
          .create()
          .text("outer")
          .template("inner");

      assertThrows(IllegalStateException.class, builder::buildAsTemplate);
    }


    @Test
    @DisplayName("Template with parameter part formats correctly")
    void templateWithParameter()
    {
      final Template template = MessageBuilder
          .create()
          .text("Hello")
          .parameter("name").spaceBefore()
          .text("!")
          .buildAsTemplate();

      final var message = assertInstanceOf(MessageTemplate.class, template).getMessage();

      assertMessage(message, "Hello %{name}!", "Hello Alice!", "name", "Alice");
    }


    @Test
    @DisplayName("Template from builder with map entries")
    void templateWithMapEntries()
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
    void emptyMessageTemplate()
    {
      final Template template = MessageBuilder
          .create()
          .buildAsTemplate();

      assertNotNull(template);
      assertInstanceOf(MessageTemplate.class, template);
    }


    @Test
    @DisplayName("buildAsTemplate from sub-builder finalizes current part")
    void buildAsTemplateFromSubBuilder()
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
    void isSameForIdenticalTemplates()
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
    void isSameForDifferentTemplates()
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
    void withMessageObject()
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

      assertMessage(message, "%(case,'hello world',case:upper)", "HELLO WORLD");
    }


    @Test
    @DisplayName("withMessage(String) parses format string as inner message")
    void withMessageString()
    {
      final var message = MessageBuilder
          .create()
          .postFormatter("case")
              .withMessage("hello world")
              .configString("case", "upper")
          .build();

      assertMessage(message, "%(case,'hello world',case:upper)", "HELLO WORLD");
    }


    @Test
    @DisplayName("withMessage(String) with parameter reference")
    void withMessageStringContainingParameter()
    {
      final var message = MessageBuilder
          .create()
          .postFormatter("case")
              .withMessage("Hello %{name}")
              .configString("case", "upper")
          .build();

      assertMessage(message, "%(case,'Hello %{name}',case:upper)", "HELLO ALICE", "name", "Alice");
    }


    @Test
    @DisplayName("withMessage(Consumer) builds inner message via nested builder")
    void withMessageConsumer()
    {
      final var message = MessageBuilder
          .create()
          .postFormatter("case")
              .withMessage(b -> b.text("hello world"))
              .configString("case", "upper")
          .build();

      assertMessage(message, "%(case,'hello world',case:upper)", "HELLO WORLD");
    }


    @Test
    @DisplayName("withMessage(Consumer) with parameter in nested builder")
    void withMessageConsumerWithParameter()
    {
      final var message = MessageBuilder
          .create()
          .postFormatter("case")
              .withMessage(b -> b.text("Hello").parameter("name").spaceBefore())
              .configString("case", "upper")
          .build();

      assertMessage(message, "%(case,'Hello %{name}',case:upper)", "HELLO ALICE", "name", "Alice");
    }


    @Test
    @DisplayName("withMessage defaults to empty message when not set")
    void defaultEmptyMessage()
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
    void withMessageConsumerNullThrows()
    {
      assertThrows(NullPointerException.class, () -> MessageBuilder
          .create()
          .postFormatter("case")
              .withMessage((Consumer<MessageBuilder>)null));
    }


    @Test
    @DisplayName("withMessage(Message.WithSpaces) with null throws NullPointerException")
    @SuppressWarnings("DataFlowIssue")
    void withMessageObjectNullThrows()
    {
      assertThrows(NullPointerException.class, () -> MessageBuilder
          .create()
          .postFormatter("case")
              .withMessage((Message.WithSpaces)null));
    }


    @Test
    @DisplayName("configMessage is not supported for post-formatter configuration")
    void messageConfigValueIsRejected()
    {
      final var message = MessageBuilder.create().text("config message").build();

      assertThrows(IllegalArgumentException.class, () -> MessageBuilder
          .create()
          .postFormatter("case")
              .configMessage("config", message));
    }


    @Test
    @DisplayName("withMessage(String) with null throws NullPointerException")
    @SuppressWarnings("DataFlowIssue")
    void withMessageStringNullThrows()
    {
      assertThrows(NullPointerException.class, () -> MessageBuilder
          .create()
          .postFormatter("case")
              .withMessage((String)null));
    }


    @Test
    @DisplayName("Post-formatter with spaceBefore and spaceAfter")
    void withSpacing()
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

      assertMessage(message, "Result: %(case,'hello',case:upper)!", "Result: HELLO!");
    }


    @Test
    @DisplayName("Last withMessage call wins")
    void lastWithMessageWins()
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
  @DisplayName("Finalized configuration builders")
  class FinalizedConfigurationBuilderTest
  {
    @Test
    @DisplayName("Parameter configuration cannot be changed after its part is flushed")
    void parameterConfigurationIsFinalized()
    {
      final var parameterBuilder = MessageBuilder
          .create()
          .parameter("value")
          .configString("setting", "original");
      final var message = parameterBuilder
          .text("end")
          .build();

      assertThrows(IllegalStateException.class, () -> parameterBuilder.configString("setting", "changed"));
      assertEquals("%{value,setting:original}end", message.asFormatString(UTF_8));
    }


    @Test
    @DisplayName("Post-formatter configuration cannot be changed after its part is flushed")
    void postFormatterConfigurationIsFinalized()
    {
      final var postFormatterBuilder = MessageBuilder
          .create()
          .postFormatter("case")
          .withMessage("hello")
          .configString("case", "upper");
      final var message = postFormatterBuilder
          .text("end")
          .build();

      assertThrows(IllegalStateException.class, () -> postFormatterBuilder.configString("case", "lower"));
      assertEquals("%(case,'hello',case:upper)end", message.asFormatString(UTF_8));
    }


    @Test
    @DisplayName("Parameter format and map entries cannot be added after its part is flushed")
    void parameterIsFinalized()
    {
      final var parameterBuilder = MessageBuilder
          .create()
          .parameter("value");
      final var message = parameterBuilder
          .text("end")
          .build();

      assertThrows(IllegalStateException.class, () -> parameterBuilder.withFormat("string"));
      assertThrows(IllegalStateException.class, () -> parameterBuilder.mapNull().message("n"));
      assertThrows(IllegalStateException.class, () -> parameterBuilder.mapDefault().message(b -> b.text("d")));
      assertEquals("%{value}end", message.asFormatString(UTF_8));
    }


    @Test
    @DisplayName("Post-formatter message cannot be changed after its part is flushed")
    void postFormatterMessageIsFinalized()
    {
      final var postFormatterBuilder = MessageBuilder
          .create()
          .postFormatter("case")
          .withMessage("hello");
      final var message = postFormatterBuilder
          .text("end")
          .build();

      assertThrows(IllegalStateException.class, () -> postFormatterBuilder.withMessage("changed"));
      assertThrows(IllegalStateException.class, () -> postFormatterBuilder.withMessage(b -> b.text("changed")));
      assertEquals("%(case,'hello')end", message.asFormatString(UTF_8));
    }


    @Test
    @DisplayName("Template default parameters and delegates cannot be added after its part is flushed")
    void templateIsFinalized()
    {
      final var templateBuilder = MessageBuilder
          .create()
          .template("tpl");
      final var message = templateBuilder
          .text("end")
          .build();

      assertThrows(IllegalStateException.class, () -> templateBuilder.withDefaultParameter("p", "v"));
      assertThrows(IllegalStateException.class, () -> templateBuilder.withParameterDelegate("a", "b"));
      assertEquals("%[tpl]end", message.asFormatString(UTF_8));
    }


    @Test
    @DisplayName("Spacing cannot be changed after a part is flushed")
    void spacingIsFinalized()
    {
      final var textBuilder = MessageBuilder
          .create()
          .text("start");
      final var message = textBuilder
          .parameter("value")
          .build();

      assertThrows(IllegalStateException.class, textBuilder::spaceAfter);
      assertThrows(IllegalStateException.class, textBuilder::spaceBefore);
      assertEquals("start%{value}", message.asFormatString(UTF_8));
    }
  }




  @Nested
  @DisplayName("TemplateBuilder.withDefaultParameter")
  class TemplateBuilderWithDefaultParameterTest
  {
    @Test
    @DisplayName("withDefaultParameter(String, String) provides string default, overridden by explicit parameter")
    void withDefaultParameterString()
    {
      final var cms = createMessageSupportWithTemplate("greeting", "Hello %{name}");
      final var message = MessageBuilder
          .create()
          .template("greeting")
              .withDefaultParameter("name", "World")
          .build();

      assertEquals("Hello World", cms.message(message).format());
      assertEquals("Hello Alice", cms.message(message).with("name", "Alice").format());
    }


    @Test
    @DisplayName("withDefaultParameter(String, boolean) provides boolean default")
    void withDefaultParameterBoolean()
    {
      final var cms = createMessageSupportWithTemplate("status",
          "%{active,true:enabled,false:disabled}");

      assertEquals("enabled", cms.message(MessageBuilder
          .create()
          .template("status")
              .withDefaultParameter("active", true)
          .build()).format());
      assertEquals("disabled", cms.message(MessageBuilder
          .create()
          .template("status")
              .withDefaultParameter("active", false)
          .build()).format());
    }


    @Test
    @DisplayName("withDefaultParameter(String, long) provides numeric default, overridden by explicit parameter")
    void withDefaultParameterLong()
    {
      final var cms = createMessageSupportWithTemplate("count-msg",
          "%{count,format:choice,1:'one item',:'%{count} items'}");
      final var message = MessageBuilder
          .create()
          .template("count-msg")
              .withDefaultParameter("count", 42)
          .build();

      assertEquals("42 items", cms.message(message).format());
      assertEquals("one item", cms.message(message).with("count", 1).format());
    }


    @Test
    @DisplayName("Multiple default parameters on same template")
    void multipleDefaultParameters()
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
    void withDefaultParameterNullNameThrows()
    {
      assertThrows(NullPointerException.class, () -> MessageBuilder
          .create()
          .template("t")
              .withDefaultParameter(null, "value"));
    }


    @Test
    @DisplayName("withDefaultParameter(String) with null value throws NullPointerException")
    @SuppressWarnings("DataFlowIssue")
    void withDefaultParameterNullStringValueThrows()
    {
      assertThrows(NullPointerException.class, () -> MessageBuilder
          .create()
          .template("t")
              .withDefaultParameter("name", (String)null));
    }


    @Test
    @DisplayName("withDefaultParameter with invalid name throws IllegalArgumentException")
    void withDefaultParameterInvalidNameThrows()
    {
      assertThrows(IllegalArgumentException.class, () -> MessageBuilder
          .create()
          .template("t")
              .withDefaultParameter("INVALID NAME!", "value"));
    }


    @Test
    @DisplayName("Template with default parameter serializes correctly")
    void defaultParameterSerialization()
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
    void templateWithSpacingAndDefault()
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
    void withParameterDelegate()
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




  @Nested
  @DisplayName("Builder reuse after build()")
  class BuilderReuseTest
  {
    static Stream<Arguments> operationsAfterBuild()
    {
      return Stream.of(
          reuse("Calling build() twice throws IllegalStateException",
              MessageBuilder::build, MessageBuilder::build),
          reuse("Calling buildWithCode() after build() throws IllegalStateException",
              MessageBuilder::build, builder -> builder.buildWithCode("MSG-001")),
          reuse("Calling buildAsTemplate() after buildWithCode() throws IllegalStateException",
              builder -> builder.buildWithCode("MSG-001"), MessageBuilder::buildAsTemplate),
          reuse("Calling text() after build() throws IllegalStateException",
              MessageBuilder::build, builder -> builder.text("world")),
          reuse("Calling parameter() after build() throws IllegalStateException",
              MessageBuilder::build, builder -> builder.parameter("name")),
          reuse("Calling postFormatter() after build() throws IllegalStateException",
              MessageBuilder::build, builder -> builder.postFormatter("upper-case")),
          reuse("Calling template() after build() throws IllegalStateException",
              MessageBuilder::build, builder -> builder.template("greeting"))
      );
    }


    @ParameterizedTest(name = "{0}")
    @DisplayName("Builder methods throw IllegalStateException after the message has been built")
    @MethodSource("operationsAfterBuild")
    void operationAfterBuildThrows(@SuppressWarnings("unused") String name, Consumer<MessageBuilder> finisher,
                                       Consumer<MessageBuilder> operation)
    {
      final var builder = MessageBuilder.create().text("hello");

      finisher.accept(builder);

      assertThrows(IllegalStateException.class, () -> operation.accept(builder));
    }


    private static Arguments reuse(String name, Consumer<MessageBuilder> finisher,
                                   Consumer<MessageBuilder> operation) {
      return arguments(name, finisher, operation);
    }
  }




  private static void assertMessage(@NotNull Message message, @NotNull String formatString, @NotNull String formatted,
                                    @NotNull Object... parameters)
  {
    assertEquals(formatString, message.asFormatString(UTF_8));

    final var configurer = shared().message(message);

    for(var n = 0; n < parameters.length; n += 2)
      configurer.with((String)parameters[n], parameters[n + 1]);

    assertEquals(formatted, configurer.format());
  }
}
