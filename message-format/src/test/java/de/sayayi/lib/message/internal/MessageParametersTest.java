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
import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.formatter.DefaultFormatterService;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;

import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

import static java.util.Locale.GERMAN;
import static java.util.Locale.ITALIAN;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * Tests {@link MessageParameters}: parameter value lookup and the {@code equals} and {@code hashCode} contract.
 *
 * @author Jeroen Gremmen
 * @since 0.20.0
 */
@DisplayName("Message parameters")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class MessageParametersTest
{
  private static final Map<String,Object> VALUES = Map.of(
      "a", "Hello",
      "b", 45,
      "c", true,
      "o", OptionalInt.empty());

  private MessageParameters parameters;


  @BeforeEach
  void init() {
    parameters = createParameters(ITALIAN, "o", "b", "c", "a");
  }


  @Test
  @DisplayName("Querying parameter values")
  void testParameterValues()
  {
    assertEquals(ITALIAN, parameters.getLocale());

    assertEquals("Hello", parameters.getParameterValue("a"));
    assertEquals(45, parameters.getParameterValue("b"));
    assertEquals(true, parameters.getParameterValue("c"));
    assertEquals(OptionalInt.empty(), parameters.getParameterValue("o"));
    assertNull(parameters.getParameterValue("XYZ"));

    assertEquals(Set.of("o", "a", "b", "c"), parameters.getParameterNames());

    //noinspection ResultOfMethodCallIgnored
    parameters.toString();
  }


  @Test
  @DisplayName("Equals and hashCode do not depend on the parameter order")
  void testEqualsAndHashCode()
  {
    val sameParameters = createParameters(ITALIAN, "a", "b", "c", "o");

    assertEquals(parameters, sameParameters);
    assertEquals(parameters.hashCode(), sameParameters.hashCode());
    assertNotEquals(parameters, createParameters(GERMAN, "a", "b", "c", "o"));
  }


  @Test
  @DisplayName("Not equal to other Parameters implementations")
  void testNotEqualsOtherImplementation()
  {
    val otherParameters = mock(Parameters.class);

    when(otherParameters.getLocale()).thenReturn(ITALIAN);
    when(otherParameters.getParameterNames()).thenReturn(VALUES.keySet());
    when(otherParameters.getParameterValue(anyString()))
        .thenAnswer(invocation -> VALUES.get(invocation.getArgument(0, String.class)));
    when(otherParameters.asParameterMap()).thenReturn(VALUES);

    // symmetric: neither side considers the other equal
    assertNotEquals(parameters, otherParameters);
    assertNotEquals(otherParameters, parameters);
  }


  private static @NotNull MessageParameters createParameters(@NotNull Locale locale, @NotNull String... names)
  {
    val messageSupport = new MessageSupportImpl(DefaultFormatterService.getSharedInstance(),
        MessageFactory.getSharedInstance());
    val configurer = messageSupport.new Configurer<>(() -> null);

    configurer.locale(locale);
    for(val name: names)
      configurer.with(name, VALUES.get(name));

    return new MessageParameters(configurer);
  }
}
