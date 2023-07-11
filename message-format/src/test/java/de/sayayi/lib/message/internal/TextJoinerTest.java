package de.sayayi.lib.message.internal;

import lombok.val;
import org.junit.jupiter.api.Test;

import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;
import static de.sayayi.lib.message.part.TextPartFactory.spacedText;
import static org.junit.jupiter.api.Assertions.assertEquals;


class TextJoinerTest
{
  @Test
  void testStrings()
  {
    val joiner = new TextJoiner();

    joiner
        .addWithSpace(" ")
        .addWithSpace(" Test ")
        .addWithSpace(null)
        .addWithSpace("1 ");

    assertEquals(noSpaceText("Test 1"), joiner.asNoSpaceText());
    assertEquals(spacedText(" Test 1 "), joiner.asSpacedText());
  }


  @Test
  void testTexts()
  {
    val joiner = new TextJoiner();

    joiner
        .add(noSpaceText("e1"))
        .add(spacedText(", "))
        .add(noSpaceText("e2"));

    assertEquals(noSpaceText("e1, e2"), joiner.asNoSpaceText());
  }
}
