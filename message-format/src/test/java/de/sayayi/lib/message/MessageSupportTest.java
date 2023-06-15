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
package de.sayayi.lib.message;

import lombok.val;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;
import java.util.OptionalLong;

import static java.util.Locale.GERMANY;
import static java.util.Locale.US;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Jeroen Gremmen
 */
class MessageSupportTest
{
  @Test
  void format()
  {
    val messageSupport = MessageSupportFactory.shared();

    assertEquals("69", messageSupport
        .message("%{n}").with("n", OptionalLong.of(69)).format());

    assertEquals("Messa...", messageSupport
        .message("%{s,clip,clip-size:8}")
        .with("s", "Message")
        .remove("s")
        .with("s", "Message Support")
        .format());
  }


  @Test
  void formatSupplier()
  {
    val messageSupport = MessageSupportFactory.shared();
    val messageBuilder = messageSupport
        .message("%{d,date:medium}")
        .with("d", LocalDate.of(2023, 6, 15))
        .locale(GERMANY);

    val supplier = messageBuilder.formatSupplier();

    assertEquals("15.06.2023", Optional.<String>empty().orElseGet(supplier));

    // change parameter
    messageBuilder
        .with("d", LocalDate.of(2000, 1, 1))
        .locale(US);

    // supplier contains the old value and locale
    assertEquals("15.06.2023", supplier.get());

    // new supplier contains the new value and locale
    assertEquals("Jan 1, 2000", messageBuilder.formatSupplier().get());
  }
}
