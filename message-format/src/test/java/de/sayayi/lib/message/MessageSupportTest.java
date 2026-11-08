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

import de.sayayi.lib.message.exception.DuplicateTemplateException;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicInteger;

import static de.sayayi.lib.message.MessageSupportFactory.createDefault;
import static de.sayayi.lib.message.MessageSupportFactory.shared;
import static java.util.Locale.GERMANY;
import static java.util.Locale.US;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests {@link MessageSupport}: formatting of messages, format suppliers, exception creation and post formatter locale
 * handling.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
@DisplayName("Message support interface")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class MessageSupportTest
{
  @Test
  @DisplayName("Immediate message formatting")
  void testFormat()
  {
    assertEquals("69", shared()
        .message("%{n}")
        .with("n", OptionalLong.of(69))
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
  @DisplayName("Immediate exception construction with formatted message")
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
  @DisplayName("Deferred exception construction with formatted message")
  void testFormattedExceptionSupplier()
  {
    val exception = assertThrowsExactly(IOException.class, () -> {
      val supplier = shared()
          .message("%{b,format:bool,true:yes,false:no}")
          .with("b", BigInteger.valueOf(-5000))
          .formattedExceptionSupplier(msg -> new IOException("answer = " + msg));

      //noinspection DataFlowIssue
      OptionalInt
          .empty()
          .orElseThrow(supplier);
    });

    assertEquals("answer = yes", exception.getMessage());
  }


  @Test
  @DisplayName("Exception construction with constructor references")
  void testFormattedExceptionConstructorReference()
  {
    val cause = new IOException("cause");
    val configurer = shared().message("value %{v}").with("v", 7);

    val exception = configurer.formattedException(IllegalStateException::new);
    assertEquals("value 7", exception.getMessage());
    assertNull(exception.getCause());

    val exceptionWithCause = configurer.formattedException(IllegalStateException::new, cause);
    assertEquals("value 7", exceptionWithCause.getMessage());
    assertSame(cause, exceptionWithCause.getCause());

    assertEquals("value 7",
        configurer.formattedExceptionSupplier(IllegalStateException::new).get().getMessage());
    assertSame(cause, configurer.formattedExceptionSupplier(IllegalStateException::new, cause).get().getCause());
  }


  @Test
  @DisplayName("Exception suppliers create a new exception on every call and format the message once")
  void testFormattedExceptionSupplierCreatesNewException()
  {
    val formatCount = new AtomicInteger();
    val cause = new IOException("cause");
    val configurer = shared()
        .message("%{v}")
        .with("v", new Object() {
          @Override
          public String toString() {
            return "value " + formatCount.incrementAndGet();
          }
        });

    val supplier = configurer.formattedExceptionSupplier(IllegalStateException::new);
    val supplierWithCause = configurer.formattedExceptionSupplier(IllegalStateException::new, cause);

    val exception1 = supplier.get();
    val exception2 = supplier.get();
    assertNotSame(exception1, exception2);
    assertEquals("value 1", exception1.getMessage());
    assertEquals("value 1", exception2.getMessage());

    val exceptionWithCause1 = supplierWithCause.get();
    val exceptionWithCause2 = supplierWithCause.get();
    assertNotSame(exceptionWithCause1, exceptionWithCause2);
    assertEquals("value 2", exceptionWithCause1.getMessage());
    assertEquals("value 2", exceptionWithCause2.getMessage());
    assertSame(cause, exceptionWithCause2.getCause());

    // each supplier formats the message once
    assertEquals(2, formatCount.get());
  }


  @Test
  @DisplayName("Post formatter uses per-call locale")
  @SuppressWarnings("UnnecessaryUnicodeEscape")
  void testPostFormatterLocaleOverride()
  {
    assertEquals("I", MessageSupportFactory
        .createDefault()
        .setLocale(Locale.forLanguageTag("tr"))
        .message("%(case,'i',case:upper)")
        .locale(US)
        .format());

    assertEquals("\u0130", MessageSupportFactory
        .createDefault()
        .setLocale(US)
        .message("%(case,'i',case:upper)")
        .locale(Locale.forLanguageTag("tr"))
        .format());
  }


  @Test
  @DisplayName("Template filter accepting a duplicate replaces the existing template")
  void testTemplateFilterReplacesDuplicate()
  {
    val messageSupport = createDefault();
    val messageFactory = messageSupport.getMessageAccessor().getMessageFactory();

    messageSupport
        .addTemplate("tpl", messageFactory.parseTemplate("first"))
        .setTemplateFilter((name, template) -> true)
        .addTemplate("tpl", messageFactory.parseTemplate("second"));

    assertEquals("second", messageSupport.message("%[tpl]").format());
  }


  @Test
  @DisplayName("Default template filter rejects a different duplicate")
  void testDefaultTemplateFilterRejectsDuplicate()
  {
    val messageSupport = createDefault();
    val messageFactory = messageSupport.getMessageAccessor().getMessageFactory();

    messageSupport
        .addTemplate("tpl", messageFactory.parseTemplate("first"))
        .addTemplate("tpl", messageFactory.parseTemplate("first"));

    assertThrowsExactly(DuplicateTemplateException.class,
        () -> messageSupport.addTemplate("tpl", messageFactory.parseTemplate("second")));
    assertEquals("first", messageSupport.message("%[tpl]").format());
  }
}
