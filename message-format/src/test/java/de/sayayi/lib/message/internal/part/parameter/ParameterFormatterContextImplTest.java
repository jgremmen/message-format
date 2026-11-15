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
package de.sayayi.lib.message.internal.part.parameter;

import de.sayayi.lib.message.formatter.parameter.NamedParameterFormatter;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext;
import de.sayayi.lib.message.part.MessagePart.Text;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


/**
 * Tests {@link ParameterFormatterContextImpl}: the exception message when a formatter delegates past the end of
 * the formatter chain.
 *
 * @author Jeroen Gremmen
 * @since 0.25.0
 */
@DisplayName("Parameter formatter context")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class ParameterFormatterContextImplTest extends AbstractFormatterTest
{
  @ParameterizedTest(name = "{0}")
  @DisplayName("Named formatter delegating past the end of the chain names type and format")
  @CsvSource(nullValues = "null", textBlock = """
      text | java.lang.String
      null | null
      """, delimiter = '|')
  void namedFormatterDelegatesPastEnd(String value, String type)
  {
    var messageAccessor = createMessageAccessor(new DelegatingFormatter());

    var ex = assertThrows(
        NoSuchElementException.class,
        () -> format(messageAccessor, value, "delegating"));
    assertEquals(
        "no further formatter for type " + type + " (format: delegating)",
        ex.getMessage());
  }




  private static final class DelegatingFormatter implements NamedParameterFormatter
  {
    @Override
    public @NotNull String getName() {
      return "delegating";
    }


    @Override
    public boolean canFormat(@NotNull Class<?> type) {
      return true;
    }


    @Override
    public @NotNull Text format(@NotNull ParameterFormatterContext context, Object value) {
      return context.delegateToNextFormatter();
    }
  }
}
