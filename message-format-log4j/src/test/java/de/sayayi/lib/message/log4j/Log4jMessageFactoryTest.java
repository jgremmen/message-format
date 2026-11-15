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
package de.sayayi.lib.message.log4j;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.WriterAppender;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.status.StatusListener;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.jupiter.api.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static de.sayayi.lib.message.util.AssertionTestSupport.assertNullRejected;
import static org.apache.logging.log4j.Level.ALL;
import static org.apache.logging.log4j.Level.WARN;
import static org.apache.logging.log4j.LogManager.ROOT_LOGGER_NAME;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests {@link Log4jMessageFactory}: creation of log messages with parameters and throwables, the selection between
 * message format and Log4j placeholder styles, and the handling of exceptions and errors thrown while formatting.
 *
 * @author Jeroen Gremmen
 * @since 0.21.0
 */
@DisplayName("Log4j message factory")
@TestMethodOrder(MethodOrderer.DisplayName.class)
class Log4jMessageFactoryTest
{
  private static final MessageFactory LOG4J_MESSAGE_FACTORY = new Log4jMessageFactory();

  private StringWriter stringWriter;
  private Logger logger;
  private WriterAppender appender;


  @BeforeEach
  void setUp()
  {
    stringWriter = new StringWriter();

    final var context = (LoggerContext)LogManager.getContext(false);
    final var config = context.getConfiguration();

    appender = WriterAppender.newBuilder()
        .setName("TestStringWriter")
        .setTarget(stringWriter)
        .setLayout(PatternLayout
            .newBuilder()
            .setPattern("%m%n")
            .setConfiguration(config)
            .build())
        .setFollow(true)
        .build();
    appender.start();

    config.addAppender(appender);

    final var loggerConfig = config.getLoggerConfig(ROOT_LOGGER_NAME);
    loggerConfig.addAppender(appender, ALL, null);
    loggerConfig.setLevel(ALL);

    context.updateLoggers();

    logger = LogManager.getLogger(Log4jMessageFactoryTest.class, LOG4J_MESSAGE_FACTORY);
  }


  @AfterEach
  void tearDown()
  {
    final var context = (LoggerContext)LogManager.getContext(false);
    final var config = context.getConfiguration();
    final var loggerConfig = config.getLoggerConfig(ROOT_LOGGER_NAME);

    loggerConfig.removeAppender("TestStringWriter");
    config.getAppenders().remove("TestStringWriter");
    appender.stop();

    context.updateLoggers();
  }


  @Test
  @DisplayName("Constructor rejects a null message support")
  @SuppressWarnings("DataFlowIssue")
  void nullMessageSupport()
  {
    assertNullRejected(
        "messageSupport must not be null",
        () -> new Log4jMessageFactory(null, true));
  }


  @Test
  @DisplayName("Use Log4jMessage for message-format %{} placeholders")
  void log4jMessageForMessageFormatPlaceholders()
  {
    logger.info("Hello %{p1}, you are %{p2} years old", "Alice", 30);
    assertLogged("Hello Alice, you are 30 years old");
  }


  @Test
  @DisplayName("Attach throwable from last parameter to the log message")
  void messageWithThrowable()
  {
    final var exception = new RuntimeException("test error");

    logger.info("Something went wrong: %{p1}", "oops", exception);

    final var output = stringWriter.toString();
    assertTrue(output.contains("Something went wrong: oops"), "actual: " + output);
    assertTrue(output.contains(exception.getClass().getName()));
  }


  @Test
  @DisplayName("Capture multiple sequential log messages")
  void multipleMessages()
  {
    logger.info("First");
    logger.info("Second");

    assertLogged("First\nSecond");
  }


  @Test
  @DisplayName("Log object message")
  void newMessageWithObject()
  {
    logger.info(LOG4J_MESSAGE_FACTORY.newMessage(42));
    assertLogged("42");
  }


  @Test
  @DisplayName("Log null message string")
  void newMessageWithNullString()
  {
    logger.info(LOG4J_MESSAGE_FACTORY.newMessage(null, (Object[])null));
    assertLogged("null");
  }


  @Test
  @DisplayName("Use ParameterizedMessage for log4j-style {} placeholders")
  void parameterizedMessageForLog4jPlaceholders()
  {
    logger.info("Hello {}, you are {} years old", "Eve", 30);
    assertLogged("Hello Eve, you are 30 years old");
  }


  @Test
  @DisplayName("Prefer Log4jMessage when both {} and %{ are present")
  void log4jMessageWhenBothPlaceholderStylesPresent()
  {
    logger.info("%{p1} {}", "Test");
    assertLogged("Test {}");
  }


  @Test
  @DisplayName("Prefer Log4jMessage when both {} and %[ are present")
  void log4jMessageWithBracketPlaceholder()
  {
    logger.info("text %[p1] {}", "a");

    final var output = stringWriter.toString();
    assertTrue(output.contains("{}"), "Expected literal {} in output: " + output);
  }


  @Test
  @DisplayName("Use Log4jMessage for plain text without any placeholders")
  void log4jMessageForPlainText()
  {
    logger.info("plain text");
    assertLogged("plain text");
  }


  @Test
  @DisplayName("Use Log4jMessage when {} is escaped")
  void log4jMessageForEscapedLog4jPlaceholder()
  {
    logger.info("escaped \\{} placeholder");
    assertLogged("escaped {} placeholder");
  }


  @Test
  @DisplayName("Return error placeholder for invalid message format")
  void invalidMessageFormat()
  {
    logger.info("not valid %{}");
    assertLogged("<internal error formatting: not valid %{}>");
  }


  @Test
  @DisplayName("Report formatting errors through the status logger")
  void invalidMessageFormatReportsCause()
  {
    final List<StatusData> statusData = new ArrayList<>();
    final var listener = new StatusListener() {
      @Override public void log(StatusData data) { statusData.add(data); }
      @Override public Level getStatusLevel() { return WARN; }
      @Override public void close() {}
    };

    StatusLogger.getLogger().registerListener(listener);
    try {
      assertEquals("<internal error formatting: not valid %{}>",
          LOG4J_MESSAGE_FACTORY.newMessage("not valid %{}").getFormattedMessage());
    } finally {
      StatusLogger.getLogger().removeListener(listener);
    }

    final var warning = statusData
        .stream()
        .filter(data -> data.getMessage().getFormattedMessage().contains("not valid %{}"))
        .findFirst()
        .orElse(null);

    assertNotNull(warning, "no status logger warning for invalid message");
    assertEquals(WARN, warning.getLevel());
    assertInstanceOf(RuntimeException.class, warning.getThrowable());
  }


  @Test
  @DisplayName("Propagate errors thrown while formatting")
  void formattingErrorIsPropagated()
  {
    final var message = LOG4J_MESSAGE_FACTORY.newMessage("value %{p1}", new Object() {
      @Override public String toString() {
        throw new StackOverflowError("test");
      }
    });

    assertThrows(StackOverflowError.class, message::getFormattedMessage);
  }


  @Test
  @DisplayName("Message returns the unformatted format string")
  @SuppressWarnings("deprecation")
  void getFormat()
  {
    final var message = LOG4J_MESSAGE_FACTORY.newMessage("Hello %{p1}", "Alice");

    assertEquals("Hello %{p1}", message.getFormat());
    assertEquals("Hello Alice", message.getFormattedMessage());
  }


  @Test
  @DisplayName("Serialized message contains the formatted message and throwable")
  @SuppressWarnings("deprecation")
  void serialization() throws Exception
  {
    final var message = LOG4J_MESSAGE_FACTORY.newMessage("Hello %{p1}", "Alice", new RuntimeException("test error"));
    final var bytes = new ByteArrayOutputStream();

    try(var out = new ObjectOutputStream(bytes)) {
      out.writeObject(message);
    }

    final org.apache.logging.log4j.message.Message deserialized;

    try(var in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
      deserialized = (org.apache.logging.log4j.message.Message)in.readObject();
    }

    assertEquals("Hello %{p1}", deserialized.getFormat());
    assertEquals("Hello Alice", deserialized.getFormattedMessage());
    assertNull(deserialized.getParameters());
    assertEquals("test error", deserialized.getThrowable().getMessage());
  }


  private void assertLogged(String expected) {
    assertEquals(expected + "\n", stringWriter.toString());
  }
}
