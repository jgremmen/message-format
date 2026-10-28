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
package de.sayayi.lib.message.internal.part.config;

import de.sayayi.lib.message.internal.part.typedvalue.TypedValueNumber;
import de.sayayi.lib.message.part.TypedValue;
import de.sayayi.lib.pack.PackOutputStream;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static de.sayayi.lib.message.internal.part.typedvalue.TypedValueBool.FALSE;
import static de.sayayi.lib.message.internal.part.typedvalue.TypedValueBool.TRUE;
import static de.sayayi.lib.message.pack.PackConstants.PACK_CONFIG;
import static org.junit.jupiter.api.Assertions.*;


/**
 * @author Jeroen Gremmen
 * @since 0.25.0
 */
@DisplayName("MessagePartConfig")
class MessagePartConfigTest
{
  @Test
  @DisplayName("Copies the configuration map")
  void copiesConfigurationMap()
  {
    val source = new HashMap<String,TypedValue<?>>();
    source.put("enabled", TRUE);

    val config = new MessagePartConfig(source);

    source.put("enabled", FALSE);
    source.put("added", TRUE);

    assertEquals(Set.of("enabled"), config.getConfigNames());
    assertSame(TRUE, config.getConfigValue("enabled"));
  }


  @Test
  @DisplayName("Iterates config names in sorted order")
  void iteratesConfigNamesSorted()
  {
    val source = new HashMap<String,TypedValue<?>>();
    for(val name: List.of("g", "f", "a", "e", "c", "b", "d"))
      source.put(name, new TypedValueNumber(name.charAt(0)));

    val config = new MessagePartConfig(source);

    assertEquals(List.of("a", "b", "c", "d", "e", "f", "g"), List.copyOf(config.getConfigNames()));
    assertEquals(config, new MessagePartConfig(Map.copyOf(source)));
  }


  @Test
  @DisplayName("Packs identical configurations into identical bytes")
  void packIsIndependentOfSourceMapOrder() throws Exception
  {
    val source = new HashMap<String,TypedValue<?>>();
    for(val name: List.of("zeta", "alpha", "mu", "beta", "omega"))
      source.put(name, TRUE);

    assertArrayEquals(pack(new MessagePartConfig(source)), pack(new MessagePartConfig(Map.copyOf(source))));
  }


  private static byte[] pack(@NotNull MessagePartConfig config) throws Exception
  {
    val bytes = new ByteArrayOutputStream();

    try(val packStream = new PackOutputStream(PACK_CONFIG, 1, false, bytes)) {
      config.pack(packStream);
    }

    return bytes.toByteArray();
  }
}
