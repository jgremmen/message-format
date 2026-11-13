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
package de.sayayi.lib.message;

import de.sayayi.lib.message.exception.DuplicateMessageException;
import de.sayayi.lib.message.exception.DuplicateTemplateException;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntConsumer;

import static de.sayayi.lib.message.MessageSupportFactory.createGeneric;
import static de.sayayi.lib.message.util.ConcurrentTestSupport.*;
import static java.util.Locale.GERMANY;
import static java.util.Locale.US;
import static java.util.concurrent.ThreadLocalRandom.current;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Concurrency regression tests for {@link MessageSupport} / {@link de.sayayi.lib.message.internal.MessageSupportImpl}.
 * <p>
 * These tests exercise concurrent message/template registration, locale and default configuration changes together
 * with concurrent reads (message/template lookup and message formatting) to make sure the read/write lock used
 * internally does not allow readers and writers to corrupt the registries, lose updates or deadlock.
 *
 * @author Jeroen Gremmen
 * @since 0.25.0
 */
@DisplayName("MessageSupport concurrency")
@TestMethodOrder(MethodOrderer.DisplayName.class)
@SuppressWarnings("ResultOfMethodCallIgnored")
final class MessageSupportConcurrencyTest
{
  @Test
  @DisplayName("Concurrent message and template registration is visible to concurrent reads")
  void concurrentMessageAndTemplateRegistrationAndReads() throws Exception
  {
    val support = createGeneric();
    val threadCount = 16;

    runWithReaders(threadCount, idx -> {
      support.addMessage("MSG-" + idx, "text " + idx);
      support.addTemplate("tpl-" + idx,
          support.getMessageAccessor().getMessageFactory().parseTemplate("template " + idx));
    }, 8, reader -> {
      val accessor = support.getMessageAccessor();

      accessor.getMessageCodes();
      accessor.getTemplateNames();
      accessor.hasMessageWithCode("MSG-0");
      accessor.hasTemplateWithName("tpl-0");
    });

    val accessor = support.getMessageAccessor();

    for(var i = 0; i < threadCount; i++)
    {
      assertTrue(accessor.hasMessageWithCode("MSG-" + i), "message MSG-" + i + " missing");
      assertTrue(accessor.hasTemplateWithName("tpl-" + i), "template tpl-" + i + " missing");
    }

    assertEquals(threadCount, accessor.getMessageCodes().size());
    assertEquals(threadCount, accessor.getTemplateNames().size());
  }


  @Test
  @DisplayName("Concurrent locale/default-config writes with concurrent formatting reads do not throw")
  void concurrentLocaleAndDefaultConfigWritesWithReads() throws Exception
  {
    val support = createGeneric();

    support.addMessage("MSG-DATE", "%{d,date:medium}");

    runWithReaders(8, idx -> {
      for(var j = 0; j < 200; j++)
      {
        support.setLocale(idx % 2 == 0 ? GERMANY : US);
        support.setDefaultConfig("ignore-default-tostring", j % 2 == 0);
      }
    }, 8, reader -> {
      support.getMessageAccessor().getLocale();
      support.getMessageAccessor().getDefaultConfig("ignore-default-tostring");
      support.code("MSG-DATE").with("d", LocalDate.of(2023, 6, 15)).format();
    });
  }


  @RepeatedTest(3)
  @Timeout(value = 10, unit = SECONDS)
  @DisplayName("Concurrent registration of different messages with the same code adds exactly one")
  void concurrentDuplicateMessageRegistration() throws Exception
  {
    val support = createGeneric();
    val threadCount = 8;

    // slow filter which reads through the accessor, widening the window between check and put
    support.setMessageFilter(message -> {
      if (support.getMessageAccessor().hasMessageWithCode(message.getCode()))
        throw new DuplicateMessageException(message.getCode(), "duplicate");

      sleep();
      return true;
    });

    val results = registerConcurrently(threadCount, idx -> support.addMessage("MSG", "text " + idx));

    assertEquals(1, results.added);
    assertEquals(threadCount - 1, results.duplicates);
  }


  @RepeatedTest(3)
  @Timeout(value = 10, unit = SECONDS)
  @DisplayName("Concurrent registration of different templates with the same name adds exactly one")
  void concurrentDuplicateTemplateRegistration() throws Exception
  {
    val support = createGeneric();
    val messageFactory = support.getMessageAccessor().getMessageFactory();
    val threadCount = 8;

    // slow filter which reads through the accessor, widening the window between check and put
    support.setTemplateFilter((name, template) -> {
      if (support.getMessageAccessor().hasTemplateWithName(name))
        throw new DuplicateTemplateException(name, "duplicate");

      sleep();
      return true;
    });

    val results = registerConcurrently(threadCount,
        idx -> support.addTemplate("tpl", messageFactory.parseTemplate("template " + idx)));

    assertEquals(1, results.added);
    assertEquals(threadCount - 1, results.duplicates);
  }


  @RepeatedTest(3)
  @Timeout(value = 10, unit = SECONDS)
  @DisplayName("Concurrent registration with default filters detects all duplicates")
  void concurrentDuplicateRegistrationDefaultFilters() throws Exception
  {
    val support = createGeneric();
    val messageFactory = support.getMessageAccessor().getMessageFactory();
    val threadCount = 16;

    val messageResults = registerConcurrently(threadCount, idx -> support.addMessage("MSG", "text " + idx));

    assertEquals(1, messageResults.added);
    assertEquals(threadCount - 1, messageResults.duplicates);

    val templateResults = registerConcurrently(threadCount,
        idx -> support.addTemplate("tpl", messageFactory.parseTemplate("template " + idx)));

    assertEquals(1, templateResults.added);
    assertEquals(threadCount - 1, templateResults.duplicates);
  }


  @Test
  @Timeout(value = 5, unit = SECONDS)
  @DisplayName("Missing-template predicates can update the support")
  void missingTemplatePredicateCanUpdateSupport()
  {
    val support = createGeneric();
    val messageAccessor = support.getMessageAccessor();

    support.addMessage("MSG", "%[missing]");

    val missingTemplates = messageAccessor.findMissingTemplates(code -> {
      support.addMessage("FROM-FILTER", "text");
      return true;
    });

    assertEquals(Set.of("missing"), missingTemplates);
    assertTrue(messageAccessor.hasMessageWithCode("FROM-FILTER"));
  }


  @Test
  @Timeout(value = 5, unit = SECONDS)
  @DisplayName("Export predicates and output can update the support")
  void exportCallbacksCanUpdateSupportWithoutChangingExportSnapshot() throws IOException
  {
    val support = createGeneric();
    val messageAccessor = support.getMessageAccessor();

    support.addTemplate("template", messageAccessor.getMessageFactory().parseTemplate("template text"));
    support.addMessage("ORIGINAL", "%[template]");

    val bytes = new ByteArrayOutputStream();
    val streamMutation = new AtomicBoolean();
    val stream = new OutputStream() {
      @Override
      public void write(int value)
      {
        bytes.write(value);
        mutateFromStream();
      }

      @Override
      public void write(byte @NotNull [] values, int offset, int length)
      {
        bytes.write(values, offset, length);
        mutateFromStream();
      }

      private void mutateFromStream()
      {
        if (streamMutation.compareAndSet(false, true))
          support.addMessage("FROM-STREAM", "text");
      }
    };

    support.exportMessages(stream, false, code -> {
      support.addMessage("FROM-MESSAGE-FILTER", "text");
      return true;
    }, name -> {
      support.addTemplate("from-template-filter", messageAccessor.getMessageFactory().parseTemplate("text"));
      return true;
    });

    val imported = createGeneric();
    val importedMessageAccessor = imported.getMessageAccessor();
    imported.importMessages(new ByteArrayInputStream(bytes.toByteArray()));

    assertEquals(Set.of("ORIGINAL"), importedMessageAccessor.getMessageCodes());
    assertEquals(Set.of("template"), importedMessageAccessor.getTemplateNames());

    assertTrue(streamMutation.get());
    assertTrue(messageAccessor.hasMessageWithCode("FROM-MESSAGE-FILTER"));
    assertTrue(messageAccessor.hasMessageWithCode("FROM-STREAM"));
    assertTrue(messageAccessor.hasTemplateWithName("from-template-filter"));
  }


  @RepeatedTest(3)
  @DisplayName("High contention stress test with concurrent reads and writes")
  void highContentionStressTest() throws Exception
  {
    val support = createGeneric();

    stress(24, 200,
        (idx, i) -> support.addMessage("STRESS-" + idx + '-' + i, "text " + i),
        (idx, i) -> support.addTemplate("stress-tpl-" + idx + '-' + i,
            support.getMessageAccessor().getMessageFactory().parseTemplate("template " + i)),
        (idx, i) -> support.setLocale(current().nextBoolean() ? GERMANY : US),
        (idx, i) -> support.setDefaultConfig("ignore-default-tostring", current().nextBoolean()),
        (idx, i) -> {
          val accessor = support.getMessageAccessor();

          accessor.getMessageCodes();
          accessor.getTemplateNames();
          accessor.getLocale();
        },
        (idx, i) -> support.message("plain text").format());
  }


  private static @NotNull RegistrationResults registerConcurrently(int threadCount, @NotNull IntConsumer registration)
      throws Exception
  {
    val added = (int)getConcurrently(threadCount, idx -> {
      try {
        registration.accept(idx);
        return true;
      } catch(DuplicateMessageException | DuplicateTemplateException ex) {
        return false;
      }
    }).stream().filter(Boolean::booleanValue).count();

    return new RegistrationResults(added, threadCount - added);
  }


  private static void sleep()
  {
    try {
      Thread.sleep(20);
    } catch(InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }




  private record RegistrationResults(int added, int duplicates) {}
}
