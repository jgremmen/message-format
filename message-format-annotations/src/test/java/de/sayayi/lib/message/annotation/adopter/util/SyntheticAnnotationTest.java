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
package de.sayayi.lib.message.annotation.adopter.util;

import de.sayayi.lib.message.annotation.Text;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.lang.annotation.Annotation;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests the {@link Annotation} contract ({@code equals}, {@code hashCode} and immutability) of the synthetic annotation
 * implementations.
 *
 * @author Jeroen Gremmen
 * @since 0.25.0
 */
@DisplayName("Synthetic annotations")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class SyntheticAnnotationTest
{
  @Test
  @DisplayName("Synthetic text equals any text annotation with equal members")
  void testText()
  {
    val text = new SyntheticText("de", "Hallo", null);
    val other = new Text() {
      @Override public String locale() { return "de"; }
      @Override public String text() { return "Hallo"; }
      @Override public String value() { return ""; }
      @Override public Class<? extends Annotation> annotationType() { return Text.class; }
    };

    assertEquals(text, other);
    assertEquals(new SyntheticText("de", "Hallo", ""), text);
    assertEquals(new SyntheticText("de", "Hallo", "").hashCode(), text.hashCode());
    assertNotEquals(new SyntheticText("en", "Hallo", ""), text);
    assertEquals(
        ((127 * "locale".hashCode()) ^ "de".hashCode()) +
        ((127 * "text".hashCode()) ^ "Hallo".hashCode()) +
        ((127 * "value".hashCode()) ^ "".hashCode()),
        text.hashCode());
  }


  @Test
  @DisplayName("Synthetic message definitions compare texts element-wise")
  void testMessageDef()
  {
    val def1 = new SyntheticMessageDef("MSG", "", new Text[] { new SyntheticText("de", "Hallo", null) });
    val def2 = new SyntheticMessageDef("MSG", "", new Text[] { new SyntheticText("de", "Hallo", null) });

    assertEquals(def1, def2);
    assertEquals(def1.hashCode(), def2.hashCode());
    assertNotEquals(new SyntheticMessageDef("MSG", "", new Text[0]), def1);
    assertNotEquals(new SyntheticMessageDef("MSG2", "", new Text[] { new SyntheticText("de", "Hallo", null) }), def1);
  }


  @Test
  @DisplayName("Synthetic template definitions compare texts element-wise")
  void testTemplateDef()
  {
    val def1 = new SyntheticTemplateDef("tpl", "", new Text[] { new SyntheticText("de", "Hallo", null) });
    val def2 = new SyntheticTemplateDef("tpl", "", new Text[] { new SyntheticText("de", "Hallo", null) });

    assertEquals(def1, def2);
    assertEquals(def1.hashCode(), def2.hashCode());
    assertNotEquals(new SyntheticTemplateDef("tpl", "", new Text[0]), def1);
  }


  @Test
  @DisplayName("Texts of synthetic definitions cannot be modified")
  void testTextsImmutable()
  {
    val texts = new Text[] { new SyntheticText("de", "Hallo", null) };
    val messageDef = new SyntheticMessageDef("MSG", "", texts);
    val templateDef = new SyntheticTemplateDef("tpl", "", texts);

    texts[0] = new SyntheticText("en", "Hello", null);
    assertEquals("de", messageDef.texts()[0].locale());
    assertEquals("de", templateDef.texts()[0].locale());

    messageDef.texts()[0] = new SyntheticText("en", "Hello", null);
    templateDef.texts()[0] = new SyntheticText("en", "Hello", null);
    assertEquals("de", messageDef.texts()[0].locale());
    assertEquals("de", templateDef.texts()[0].locale());
  }
}
