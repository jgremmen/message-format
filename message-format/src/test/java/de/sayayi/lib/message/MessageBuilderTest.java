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

import de.sayayi.lib.message.part.MessagePart;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static de.sayayi.lib.message.MessageSupportFactory.shared;
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
      final var message = MessageBuilder.create()
          .parameter("flag")
              .mapBool(true).message(b -> b.text("yes"))
              .mapBool(false).message(b -> b.text("no"))
          .build();

      final var messageSupport = shared();

      assertEquals("yes", messageSupport.message(message).with("flag", true).format());
      assertEquals("no", messageSupport.message(message).with("flag", false).format());
    }


    @Test
    @DisplayName("mapDefault with nested builder message")
    void testMapDefaultWithConsumerMessage()
    {
      final var message = MessageBuilder.create()
          .parameter("name")
              .withFormat("choice")
              .mapNull().message(b -> b.text("stranger"))
              .mapDefault().message(b -> b.text("Hello").parameter("name").spaceBefore())
          .build();

      final var messageSupport = shared();

      assertEquals("Hello World", messageSupport.message(message).with("name", "World").format());
      assertEquals("stranger", messageSupport.message(message).with("name", null).format());
    }


    @Test
    @DisplayName("mapNumber with nested builder message")
    void testMapNumberWithConsumerMessage()
    {
      final var message = MessageBuilder.create()
          .parameter("count")
              .withFormat("choice")
              .mapNumber(1).message(b -> b.text("one item"))
              .mapDefault().message(b -> b.parameter("count").text(" items").spaceBefore())
          .build();

      final var messageSupport = shared();

      assertEquals("one item", messageSupport.message(message).with("count", 1).format());
      assertEquals("5 items", messageSupport.message(message).with("count", 5).format());
    }


    @Test
    @DisplayName("mapString with nested builder message")
    void testMapStringWithConsumerMessage()
    {
      final var message = MessageBuilder.create()
          .parameter("color")
              .withFormat("choice")
              .mapString("red").message(b -> b.text("Red color"))
              .mapDefault().message(b -> b.text("Unknown color"))
          .build();

      final var messageSupport = shared();

      assertEquals("Red color", messageSupport.message(message).with("color", "red").format());
      assertEquals("Unknown color", messageSupport.message(message).with("color", "blue").format());
    }


    @Test
    @DisplayName("mapNull with nested builder message")
    void testMapNullWithConsumerMessage()
    {
      final var message = MessageBuilder.create()
          .parameter("value")
              .mapNull().message(b -> b.text("nothing"))
              .mapDefault().message(b -> b.parameter("value"))
          .build();

      final var messageSupport = shared();

      assertEquals("nothing", messageSupport.message(message).with("value", null).format());
      assertEquals("hello", messageSupport.message(message).with("value", "hello").format());
    }


    @Test
    @DisplayName("mapEmpty with nested builder message")
    void testMapEmptyWithConsumerMessage()
    {
      final var message = MessageBuilder.create()
          .parameter("text")
              .mapEmpty().message(b -> b.text("(empty)"))
              .mapDefault().message(b -> b.parameter("text"))
          .build();

      final var messageSupport = shared();

      assertEquals("(empty)", messageSupport.message(message).with("text", "").format());
      assertEquals("hi", messageSupport.message(message).with("text", "hi").format());
    }


    @Test
    @DisplayName("mapNumber with relational operator and nested builder message")
    void testMapNumberRelationalWithConsumerMessage()
    {
      final var message = MessageBuilder.create()
          .parameter("n")
              .withFormat("choice")
              .mapNumber(0).lt().message(b -> b.text("negative"))
              .mapNumber(0).gt().message(b -> b.text("positive"))
              .mapDefault().message(b -> b.text("zero"))
          .build();

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
      assertThrows(NullPointerException.class, () ->
          MessageBuilder.create()
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
      final var message = MessageBuilder.create()
          .text("Hello")
          .text("World").spaceBefore()
          .build();

      assertEquals(1, message.getMessageParts().length);
      assertInstanceOf(MessagePart.Text.class, message.getMessageParts()[0]);

      assertEquals("Hello World", shared().message(message).format());
    }


    @Test
    @DisplayName("Three consecutive text parts are merged into one")
    void testThreeConsecutiveTexts()
    {
      final var message = MessageBuilder.create()
          .text("one")
          .text("two").spaceBefore()
          .text("three").spaceBefore()
          .build();

      assertEquals(1, message.getMessageParts().length);

      assertEquals("one two three", shared().message(message).format());
    }


    @Test
    @DisplayName("Text parts around a parameter remain separate")
    void testTextAroundParameter()
    {
      final var message = MessageBuilder.create()
          .text("Hello")
          .parameter("name").spaceBefore()
          .text("!")
          .build();

      // text + parameter + text = 3 parts
      assertEquals(3, message.getMessageParts().length);

      assertEquals("Hello Alice!", shared().message(message).with("name", "Alice").format());
    }


    @Test
    @DisplayName("Multiple text parts before and after a parameter are each merged")
    void testMultipleTextGroupsAroundParameter()
    {
      final var message = MessageBuilder.create()
          .text("Dear")
          .text("customer").spaceBefore()
          .parameter("name").spaceBefore()
          .text(",")
          .text("welcome").spaceBefore()
          .text("back!").spaceBefore()
          .build();

      // "Dear customer" (merged) + parameter + ", welcome back!" (merged) = 3 parts
      assertEquals(3, message.getMessageParts().length);

      assertEquals("Dear customer Alice, welcome back!",
          shared().message(message).with("name", "Alice").format());
    }


    @Test
    @DisplayName("Single text part produces a single-part message")
    void testSingleText()
    {
      final var message = MessageBuilder.create()
          .text("solo")
          .build();

      assertEquals(1, message.getMessageParts().length);

      assertEquals("solo", shared().message(message).format());
    }


    @Test
    @DisplayName("Text with spaceAfter merges correctly with following text")
    void testSpaceAfterMerge()
    {
      final var message = MessageBuilder.create()
          .text("left").spaceAfter()
          .text("right")
          .build();

      assertEquals(1, message.getMessageParts().length);

      assertEquals("left right", shared().message(message).format());
    }


    @Test
    @DisplayName("Consecutive texts between two parameters are merged")
    void testConsecutiveTextsBetweenParameters()
    {
      final var message = MessageBuilder.create()
          .parameter("a")
          .text(",")
          .text("and").spaceBefore().spaceAfter()
          .parameter("b")
          .build();

      // parameter + ", and " (merged) + parameter = 3 parts
      assertEquals(3, message.getMessageParts().length);

      assertEquals("1, and 2",
          shared().message(message).with("a", 1).with("b", 2).format());
    }
  }
}


