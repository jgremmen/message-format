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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.WriterAppender;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.MessageFactory;
import org.junit.jupiter.api.*;

import java.io.StringWriter;

import static org.apache.logging.log4j.Level.ALL;
import static org.apache.logging.log4j.LogManager.ROOT_LOGGER_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
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
        .setLayout(PatternLayout.newBuilder().withPattern("%m%n").withConfiguration(config).build())
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
  @DisplayName("Format a simple message without parameters")
  void testSimpleMessage()
  {
    logger.info("Hello World");
    assertEquals("Hello World\n", stringWriter.toString());
  }


  @Test
  @DisplayName("Format a message with named parameters p1 and p2")
  void testMessageWithParameters()
  {
    logger.info("Hello %{p1}, you are %{p2} years old", "Alice", 30);
    assertEquals("Hello Alice, you are 30 years old\n", stringWriter.toString());
  }


  @Test
  @DisplayName("Attach throwable from last parameter to the log message")
  void testMessageWithThrowable()
  {
    final var exception = new RuntimeException("test error");

    logger.info("Something went wrong: %{p1}", "oops", exception);

    final var output = stringWriter.toString();
    assertTrue(output.contains("Something went wrong: oops"), "actual: " + output);
    assertTrue(output.contains(exception.getClass().getName()));
  }


  @Test
  @DisplayName("Format a static message with no placeholders")
  void testMessageWithNoParameters()
  {
    logger.info("Static message with no placeholders");

    assertEquals("Static message with no placeholders\n", stringWriter.toString());
  }


  @Test
  @DisplayName("Capture multiple sequential log messages")
  void testMultipleMessages()
  {
    logger.info("First");
    logger.info("Second");

    assertEquals("First\nSecond\n", stringWriter.toString());
  }


  @Test
  @DisplayName("Log object message")
  void testNewMessageWithObject()
  {
    logger.info(LOG4J_MESSAGE_FACTORY.newMessage((Object)42));
    assertEquals("42\n", stringWriter.toString());
  }


  @Test
  @DisplayName("Log null message string")
  void testNewMessageWithNullString()
  {
    logger.info(LOG4J_MESSAGE_FACTORY.newMessage(null, (Object[])null));
    assertEquals("null\n", stringWriter.toString());
  }


  @Test
  @DisplayName("Use ParameterizedMessage for log4j-style {} placeholders")
  void testParameterizedMessageForLog4jPlaceholders()
  {
    logger.info("Hello {}, you are {} years old", "Eve", 30);
    assertEquals("Hello Eve, you are 30 years old\n", stringWriter.toString());
  }


  @Test
  @DisplayName("Use Log4jMessage for message-format %{} placeholders")
  void testLog4jMessageForMessageFormatPlaceholders()
  {
    logger.info("Hello %{p1}", "World");
    assertEquals("Hello World\n", stringWriter.toString());
  }


  @Test
  @DisplayName("Prefer Log4jMessage when both {} and %{ are present")
  void testLog4jMessageWhenBothPlaceholderStylesPresent()
  {
    logger.info("%{p1} {}", "Test");
    assertEquals("Test {}\n", stringWriter.toString());
  }


  @Test
  @DisplayName("Prefer Log4jMessage when both {} and %[ are present")
  void testLog4jMessageWithBracketPlaceholder()
  {
    logger.info("text %[p1] {}", "a");

    final var output = stringWriter.toString();
    assertTrue(output.contains("{}"), "Expected literal {} in output: " + output);
  }


  @Test
  @DisplayName("Use Log4jMessage for plain text without any placeholders")
  void testLog4jMessageForPlainText()
  {
    logger.info("plain text");
    assertEquals("plain text\n", stringWriter.toString());
  }


  @Test
  @DisplayName("Use Log4jMessage when {} is escaped")
  void testLog4jMessageForEscapedLog4jPlaceholder()
  {
    logger.info("escaped \\{} placeholder");
    assertEquals("escaped {} placeholder\n", stringWriter.toString());
  }


  @Test
  @DisplayName("Return error placeholder for invalid message format")
  void testInvalidMessageFormat()
  {
    logger.info("not valid %{}");
    assertEquals("<internal error formatting: not valid %{}>\n", stringWriter.toString());
  }
}
