/*
 * Copyright 2025 Jeroen Gremmen
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
package de.sayayi.lib.message.internal;

import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.formatter.DefaultFormatterService;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static java.util.Locale.GERMAN;
import static java.util.Locale.ITALIAN;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * @author Jeroen Gremmen
 * @since 0.20.0
 */
@DisplayName("Message parameters")
class MessageParametersTest
{
  private MessageParameters parameters;


  @BeforeEach
  void init()
  {
    val messageSupport = new MessageSupportImpl(DefaultFormatterService.getSharedInstance(), NO_CACHE_INSTANCE);
    val configurer = messageSupport.new Configurer<>(() -> null);

    configurer
        .locale(ITALIAN)
        .with("o", OptionalInt.empty())
        .with("B", 45)
        .with("C", true)
        .with("A", "Hello");

    parameters = new MessageParameters(configurer);
  }


  @Test
  @DisplayName("Querying parameter values")
  void testParameterValues()
  {
    assertEquals(ITALIAN, parameters.getLocale());

    assertEquals("Hello", parameters.getParameterValue("A"));
    assertEquals(45, parameters.getParameterValue("B"));
    assertEquals(true, parameters.getParameterValue("C"));
    assertEquals(OptionalInt.empty(), parameters.getParameterValue("o"));
    assertNull(parameters.getParameterValue("XYZ"));

    assertEquals(Set.of("o", "A", "B", "C"), parameters.getParameterNames());

    //noinspection ResultOfMethodCallIgnored
    parameters.toString();
  }


  @Test
  @DisplayName("Equals")
  void testEquals()
  {
    val otherParameters = mock(Parameters.class);
    val otherMap = Map.of(
        "o", OptionalInt.empty(),
        "B", 45,
        "C", true,
        "A", "Hello");

    when(otherParameters.getLocale()).thenReturn(ITALIAN);
    when(otherParameters.getParameterNames()).thenReturn(otherMap.keySet());
    when(otherParameters.getParameterValue(anyString()))
        .thenAnswer(invocation -> otherMap.get(invocation.getArgument(0, String.class)));

    assertEquals(parameters, otherParameters);

    when(otherParameters.getLocale()).thenReturn(GERMAN);

    assertNotEquals(parameters, otherParameters);
  }


  @Test
  @DisplayName("HashCode")
  void testHashCode()
  {
    val otherMap = Map.of(
        "o", OptionalInt.empty(),
        "B", 45,
        "C", true,
        "A", "Hello");

    assertEquals(parameters.hashCode(), otherMap.entrySet().stream()
        .mapToInt(e -> e.getKey().hashCode() + e.getValue().hashCode()).sum() + ITALIAN.hashCode());
  }
}