/*
 * Copyright 2023 Jeroen Gremmen
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
package de.sayayi.lib.message.formatter;

import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static de.sayayi.lib.message.formatter.FormattableType.DEFAULT_ORDER;
import static de.sayayi.lib.message.formatter.FormattableType.DEFAULT_PRIMITIVE_OR_ARRAY_ORDER;
import static org.junit.jupiter.api.Assertions.*;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
@DisplayName("Formattable type")
class FormattableTypeTest
{
  @Test
  @DisplayName("Check constructors")
  void constructor()
  {
    assertEquals(String.class, new FormattableType(String.class, 0).getType());
    assertEquals(127, new FormattableType(String.class, 127).getOrder());

    assertThrows(IllegalArgumentException.class, () -> new FormattableType(String.class, -1));
    assertThrows(IllegalArgumentException.class, () -> new FormattableType(int.class, 128));
    assertThrows(IllegalArgumentException.class, () -> new FormattableType(Object.class, 64));

    assertEquals(127, new FormattableType(Object.class).getOrder());
    assertEquals(DEFAULT_PRIMITIVE_OR_ARRAY_ORDER, new FormattableType(long.class).getOrder());
    assertEquals(DEFAULT_ORDER, new FormattableType(Map.class).getOrder());
  }


  @Test
  @DisplayName("Compare formattable types")
  void compare()
  {
    val ft1 = new FormattableType(Object.class);
    val ft2 = new FormattableType(int.class);

    assertTrue(ft1.compareTo(ft2) > 0);
    assertTrue(ft2.compareTo(ft1) < 0);
    assertTrue(ft2.compareTo(new FormattableType(String.class)) >= 0);
  }
}
