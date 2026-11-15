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
package de.sayayi.lib.message.formatter;

import de.sayayi.lib.message.formatter.post.PostFormatter;
import de.sayayi.lib.message.formatter.post.PostFormatterContext;
import de.sayayi.lib.message.formatter.post.runtime.ClipPostFormatter;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static de.sayayi.lib.message.util.AssertionTestSupport.assertNullRejected;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests argument validation of {@link GenericFormatterService}.
 *
 * @author Jeroen Gremmen
 * @since 0.25.0
 */
@DisplayName("Generic formatter service")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class GenericFormatterServiceTest
{
  @Test
  @DisplayName("Adding a null post formatter throws with a descriptive message")
  @SuppressWarnings("DataFlowIssue")
  void addNullPostFormatter()
  {
    assertNullRejected(
        "postFormatter must not be null",
        () -> new GenericFormatterService().addPostFormatter(null));
  }


  @Test
  @DisplayName("Adding a post formatter without name throws with a descriptive message")
  void addPostFormatterWithoutName()
  {
    var formatterService = new GenericFormatterService();

    assertNullRejected(
        "name of post formatter NamelessPostFormatter must not be null",
        () -> formatterService.addPostFormatter(new NamelessPostFormatter()));
    assertTrue(formatterService.getPostFormatters().isEmpty());
  }


  @Test
  @DisplayName("Look up a single post formatter by name")
  void getPostFormatter()
  {
    var formatterService = new GenericFormatterService();
    var postFormatter = new ClipPostFormatter();

    assertNull(formatterService.getPostFormatter("clip"));

    formatterService.addPostFormatter(postFormatter);

    assertSame(postFormatter, formatterService.getPostFormatter("clip"));
    assertNull(formatterService.getPostFormatter("case"));
  }




  /** Post formatter that violates the contract by returning {@code null} as its name. */
  private static final class NamelessPostFormatter implements PostFormatter
  {
    @Override
    @SuppressWarnings("DataFlowIssue")
    public @NotNull String getName() {
      return null;
    }


    @Override
    public @NotNull String format(@NotNull String string, @NotNull PostFormatterContext context) {
      return string;
    }
  }
}
