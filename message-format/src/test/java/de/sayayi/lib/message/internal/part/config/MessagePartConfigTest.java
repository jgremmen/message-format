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

import de.sayayi.lib.message.internal.part.typedvalue.TypedValueBool;
import de.sayayi.lib.message.part.TypedValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;


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
    final Map<String,TypedValue<?>> source = new HashMap<>();
    source.put("enabled", TypedValueBool.TRUE);

    final var config = new MessagePartConfig(source);

    source.put("enabled", TypedValueBool.FALSE);
    source.put("added", TypedValueBool.TRUE);

    assertEquals(Set.of("enabled"), config.getConfigNames());
    assertSame(TypedValueBool.TRUE, config.getConfigValue("enabled"));
  }
}
