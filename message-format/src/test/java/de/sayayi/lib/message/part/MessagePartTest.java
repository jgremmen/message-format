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
package de.sayayi.lib.message.part;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.internal.part.TextPart;
import de.sayayi.lib.message.internal.part.parameter.ParameterPart;
import de.sayayi.lib.message.internal.part.post.PostFormatterPart;
import de.sayayi.lib.message.internal.part.template.TemplatePart;
import de.sayayi.lib.message.part.normalizer.MessagePartNormalizer;
import lombok.val;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.Contract;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests the {@link MessagePart} type hierarchy: the leaf interfaces only permit the built-in implementations, and
 * {@code equals}/{@code hashCode} of these implementations are consistent.
 *
 * @author Jeroen Gremmen
 * @since 0.25.0
 */
@DisplayName("Message part")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class MessagePartTest
{
  @Test
  @DisplayName("Leaf interfaces are sealed and permit the built-in implementations only")
  void testSealedLeafInterfaces()
  {
    assertPermits(MessagePart.Text.class, TextPart.class);
    assertPermits(MessagePart.Parameter.class, ParameterPart.class);
    assertPermits(MessagePart.Template.class, TemplatePart.class);
    assertPermits(MessagePart.PostFormat.class, PostFormatterPart.class);
  }


  private static void assertPermits(Class<?> leafInterface, Class<?> implementation)
  {
    assertTrue(leafInterface.isSealed(), leafInterface.getSimpleName() + " must be sealed");
    assertArrayEquals(new Class<?>[] { implementation }, leafInterface.getPermittedSubclasses());
  }


  @ParameterizedTest(name = "{0}")
  @DisplayName("Equal parts parsed separately are equal in both directions and have the same hash code")
  @ValueSource(strings = {
      "hello",
      "%{name}",
      "%{name,number:'0.00',null:'-'}",
      "%[greet]",
      "%(case,'Hello %{who}!',case:upper)"
  })
  void testEqualsSymmetric(@Language("MessageFormat") String format)
  {
    val messageFactory = new MessageFactory(MessagePartNormalizer.PASS_THROUGH);
    val part1 = firstPart(messageFactory.parseMessage(format));
    val part2 = firstPart(messageFactory.parseMessage(format));

    assertNotSame(part1, part2);
    assertEquals(part1, part2);
    assertEquals(part2, part1);
    assertEquals(part1.hashCode(), part2.hashCode());
  }


  @Test
  @DisplayName("Parts of different types are never equal")
  void testDifferentTypesNotEqual()
  {
    val text = new TextPart("name");
    val parameter = firstPart(MessageFactory.getSharedInstance().parseMessage("%{name}"));

    assertNotEquals(text, parameter);
    assertNotEquals(parameter, text);
  }


  @Contract(pure = true)
  private static MessagePart firstPart(Message message) {
    return message.getMessageParts()[0];
  }
}
