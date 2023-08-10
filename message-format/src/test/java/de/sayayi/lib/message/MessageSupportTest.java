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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

import static de.sayayi.lib.message.MessageSupportFactory.shared;
import static java.util.Locale.GERMANY;
import static java.util.Locale.US;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
@DisplayName("Test message support interface")
class MessageSupportTest
{
  @Test
  @DisplayName("Immediate message formatting")
  void testFormat()
  {
    assertEquals("69", shared()
        .message("%{n}")
        .with("n", OptionalLong.of(69))
        .format());

    assertEquals("Messa...", shared()
        .message("%{r} %{s,clip,clip-size:8}")
        .with("r", "Message")
        .remove("r")  // test parameter removal
        .with("s", "Message Support")
        .format());
  }


  @Test
  @DisplayName("Deferred message formatting")
  void testFormatSupplier()
  {
    val messageBuilder = shared()
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


  @Test
  @DisplayName("Immediate exception throw with formatted message")
  void testException()
  {
    val exception = assertThrowsExactly(IOException.class, () -> {
      throw shared()
          .message("%{list,list-sep-last:' and '}")
          .with("list", new int[] { -5, 12, 0 })
          .formattedException(msg -> new IOException("error: " + msg));
    });

    assertEquals("error: -5, 12 and 0", exception.getMessage());
  }


  @Test
  @DisplayName("Deferred exception throw with formatted message")
  void testFormattedExceptionSupplier()
  {
    val exception = assertThrowsExactly(IOException.class, () -> {
      val supplier = shared()
          .message("%{b,bool,true:yes,false:no}")
          .with("b", BigInteger.valueOf(-5000))
          .formattedExceptionSupplier(msg -> new IOException("answer = " + msg));

      //noinspection DataFlowIssue
      OptionalInt
          .empty()
          .orElseThrow(supplier);
    });

    assertEquals("answer = yes", exception.getMessage());
  }
}
