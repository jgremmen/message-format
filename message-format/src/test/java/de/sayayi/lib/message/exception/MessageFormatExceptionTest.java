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
package de.sayayi.lib.message.exception;

import de.sayayi.lib.message.Message;
import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.MessageSupport;
import de.sayayi.lib.message.MessageSupport.MessageAccessor;
import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.FormatterContext;
import de.sayayi.lib.message.formatter.GenericFormatterService;
import de.sayayi.lib.message.formatter.NamedParameterFormatter;
import de.sayayi.lib.message.internal.LocalizedMessageBundleWithCode;
import de.sayayi.lib.message.internal.MessageDelegateWithCode;
import lombok.val;
import lombok.var;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.Locale;
import java.util.stream.Stream;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static java.util.Collections.singletonMap;
import static java.util.Locale.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
@DisplayName("Message format exception")
class MessageFormatExceptionTest
{
  private static final MessageSupport.ConfigurableMessageSupport MESSAGE_SUPPORT;
  private static final Message THROWING_MESSAGE;


  static
  {
    val formatterService = new GenericFormatterService();
    val formatter = mock(NamedParameterFormatter.class, CALLS_REAL_METHODS);

    when(formatter.getName()).thenReturn("throw");
    when(formatter.format(any(FormatterContext.class), any()))
        .thenAnswer(invocation -> { throw new MessageFormatException(null); });

    formatterService.addFormatter(formatter);

    MESSAGE_SUPPORT = MessageSupportFactory.create(formatterService, NO_CACHE_INSTANCE);
    THROWING_MESSAGE = mock(Message.class, CALLS_REAL_METHODS);
    when(THROWING_MESSAGE.formatAsText(any(MessageAccessor.class), any(Parameters.class)))
        .thenAnswer(invocation -> { throw new MessageFormatException(null); });

    MESSAGE_SUPPORT.addTemplate("tpl", THROWING_MESSAGE);
  }


  private static Stream<Arguments> getMessage_parameters()
  {
    return Stream.of(
        Arguments.of(null, null, null, null,
            "failed to format message"),
        Arguments.of(null, null, null, "p",
            "failed to format message parameter 'p'"),
        Arguments.of(null, null, GERMANY, null,
            "failed to format message for locale German (Germany)"),
        Arguments.of(null, null, GERMAN, "n",
            "failed to format message parameter 'n' for locale German"),
        Arguments.of(null, "msg", null, null,
            "failed to format template 'msg'"),
        Arguments.of(null, "msg-with-default", null, "m",
            "failed to format parameter 'm' in template 'msg-with-default'"),
        Arguments.of(null, "tpl", UK, null,
            "failed to format template 'tpl' for locale English (United Kingdom)"),
        Arguments.of(null, "name", ITALY, "p",
            "failed to format parameter 'p' in template 'name' for locale Italian (Italy)"),
        Arguments.of("MSG-001", null, null, null,
            "failed to format message with code 'MSG-001'"),
        Arguments.of("MSG-002", null, null, "number",
            "failed to format parameter 'number' for message with code 'MSG-002'"),
        Arguments.of("MSG-003", null, TRADITIONAL_CHINESE, null,
            "failed to format message with code 'MSG-003' and locale Chinese (Taiwan)"),
        Arguments.of("MSG-004", null, US, "flag",
            "failed to format parameter 'flag' for message with code 'MSG-004' and locale English (United States)"),
        Arguments.of("MFP1", "flow", null, null,
            "failed to format template 'flow' for message with code 'MFP1'"),
        Arguments.of("MFP2", "age-range", null, "ABC",
            "failed to format parameter 'ABC' in template 'age-range' for message with code 'MFP2'"),
        Arguments.of("MFP3", "test", new Locale("nl", "BE"), null,
            "failed to format template 'test' for message with code 'MFP3' and locale Dutch (Belgium)"),
        Arguments.of("MFP4", "bool", new Locale("fr", "CH"), "j",
            "failed to format parameter 'j' in template 'bool' for message with code 'MFP4' and locale French (Switzerland)")
    );
  }


  @DisplayName("Detailed exception messages")
  @ParameterizedTest(name = "msg: {4}")
  @MethodSource("getMessage_parameters")
  void testGetMessage(String code, String template, Locale locale, String parameter,
                      @NotNull String msg)
  {
    var ex = MessageFormatException.of(null);

    if (code != null)
      ex = ex.withCode(code);

    if (template != null)
      ex = ex.withTemplate(template);

    if (locale != null)
      ex = ex.withLocale(locale);

    if (parameter != null)
      ex = ex.withParameter(parameter);

    assertEquals(msg, ex.getMessage());
  }


  @Test
  @DisplayName("Exclude generated code from detailed exception message")
  void testGeneratedMessageCode()
  {
    assertEquals("failed to format message parameter 'q' for locale Dutch (Netherlands)",
        MessageFormatException.of(null)
            .withCode("MSG[5JJRMZBVNJ-30UO]")
            .withParameter("q")
            .withLocale(new Locale("nl", "NL"))
            .getMessage());
  }


  @Test
  @DisplayName("format: failed to format message")
  void testFormat0000()
  {
    assertEquals("failed to format message",
        assertThrowsExactly(MessageFormatException.class, () -> MESSAGE_SUPPORT
            .message(THROWING_MESSAGE)
            .format())
            .getMessage());
  }


  @Test
  @DisplayName("format: failed to format message parameter 'p'")
  void testFormat0001()
  {
    assertEquals("failed to format message parameter 'p'",
        assertThrowsExactly(MessageFormatException.class, () -> MESSAGE_SUPPORT
            .message("%{p,throw}")
            .with("p", new NullPointerException())
            .format())
            .getMessage());
  }


  @Test
  @DisplayName("format: failed to format message for locale German (Germany)")
  void testFormat0010()
  {
    assertEquals("failed to format message for locale German (Germany)",
        assertThrowsExactly(MessageFormatException.class, () -> MESSAGE_SUPPORT
            .message(new LocalizedMessageBundleWithCode("MSG[ABC]",
                singletonMap(new Locale("de", "CH"), THROWING_MESSAGE)))
            .locale(GERMANY)
            .format())
            .getMessage());
  }


  @Test
  @DisplayName("format: failed to format message parameter 'n' for locale German")
  void testFormat0011()
  {
    val lmMap = new HashMap<Locale,String>();

    lmMap.put(GERMANY, "%{n,throw}");
    lmMap.put(ENGLISH, "%{e}");

    assertEquals("failed to format message parameter 'n' for locale German",
        assertThrowsExactly(MessageFormatException.class, () -> MESSAGE_SUPPORT
            .message(MESSAGE_SUPPORT.getMessageAccessor().getMessageFactory().parseMessage(lmMap))
            .with("n", new NullPointerException())
            .locale(GERMAN)
            .format())
            .getMessage());
  }


  @Test
  @DisplayName("format: failed to format template 'msg'")
  void testFormat0100()
  {
    assertEquals("failed to format template 'tpl'",
        assertThrowsExactly(MessageFormatException.class, () -> MESSAGE_SUPPORT
            .message("%[tpl]")
            .format())
            .getMessage());
  }


  @Test
  @DisplayName("format: failed to format parameter 'm' in template 'msg-with-default'")
  void testFormat0101()
  {
    MESSAGE_SUPPORT.addTemplate("msg-with-default", MESSAGE_SUPPORT.getMessageAccessor()
        .getMessageFactory().parseMessage("%{m,throw}"));

    assertEquals("failed to format parameter 'm' in template 'msg-with-default'",
        assertThrowsExactly(MessageFormatException.class, () -> MESSAGE_SUPPORT
            .message("%[msg-with-default]")
            .format())
            .getMessage());
  }


  @Test
  @DisplayName("format: failed to format template 'tpl' for locale English (United Kingdom)")
  void testFormat0110()
  {
    val lmMap = new HashMap<Locale,String>();

    lmMap.put(GERMANY, "%{n,throw}");
    lmMap.put(ENGLISH, "%[tpl]");

    assertEquals("failed to format template 'tpl' for locale English (United Kingdom)",
        assertThrowsExactly(MessageFormatException.class, () -> MESSAGE_SUPPORT
            .message(MESSAGE_SUPPORT.getMessageAccessor().getMessageFactory().parseMessage(lmMap))
            .locale(UK)
            .format())
            .getMessage());
  }


  @Test
  @DisplayName("format: failed to format parameter 'p' in template 'name' for locale Italian (Italy)")
  void testFormat0111()
  {
    MESSAGE_SUPPORT.addTemplate("name", MESSAGE_SUPPORT.getMessageAccessor()
        .getMessageFactory().parseMessage("%{p,throw}"));

    val lmMap = new HashMap<Locale,String>();

    lmMap.put(GERMANY, "%{n,throw}");
    lmMap.put(ITALIAN, "%[name]");

    assertEquals("failed to format parameter 'p' in template 'name' for locale Italian (Italy)",
        assertThrowsExactly(MessageFormatException.class, () -> MESSAGE_SUPPORT
            .message(MESSAGE_SUPPORT.getMessageAccessor().getMessageFactory().parseMessage(lmMap))
            .locale(ITALY)
            .format())
            .getMessage());
  }


  @Test
  @DisplayName("format: failed to format message with code 'MSG-001'")
  void testFormat1000()
  {
    MESSAGE_SUPPORT.addMessage(new MessageDelegateWithCode("MSG-001", THROWING_MESSAGE));

    assertEquals("failed to format message with code 'MSG-001'",
        assertThrowsExactly(MessageFormatException.class, () -> MESSAGE_SUPPORT
            .code("MSG-001")
            .format())
            .getMessage());
  }


  @Test
  @DisplayName("format: failed to format parameter 'number' for message with code 'MSG-002'")
  void testFormat1001()
  {
    MESSAGE_SUPPORT.addMessage("MSG-002", "%{number,throw}");

    assertEquals("failed to format parameter 'number' for message with code 'MSG-002'",
        assertThrowsExactly(MessageFormatException.class, () -> MESSAGE_SUPPORT
            .code("MSG-002")
            .format())
            .getMessage());
  }


  @Test
  @DisplayName("format: failed to format message with code 'MSG-003' and locale Chinese (Taiwan)")
  void testFormat1010()
  {
    assertEquals("failed to format message with code 'MSG-003' and locale Chinese (Taiwan)",
        assertThrowsExactly(MessageFormatException.class, () -> MESSAGE_SUPPORT
            .message(new LocalizedMessageBundleWithCode("MSG-003",
                singletonMap(new Locale("de", "CH"), THROWING_MESSAGE)))
            .locale(TRADITIONAL_CHINESE)
            .format())
            .getMessage());
  }


  @Test
  @DisplayName("format: failed to format parameter 'flag' for message with code 'MSG-004' and locale English (United States)")
  void testFormat1011()
  {
    val lmMap = new HashMap<Locale,String>();

    lmMap.put(GERMANY, "%{n,throw}");
    lmMap.put(ENGLISH, "%{flag,throw}");

    assertEquals("failed to format parameter 'flag' for message with code 'MSG-004' and locale English (United States)",
        assertThrowsExactly(MessageFormatException.class, () -> MESSAGE_SUPPORT
            .message(MESSAGE_SUPPORT.getMessageAccessor().getMessageFactory().parseMessage("MSG-004", lmMap))
            .locale(US)
            .format())
            .getMessage());
  }


  @Test
  @DisplayName("format: failed to format template 'tpl' for message with code 'MFP1'")
  void testFormat1100()
  {
    assertEquals("failed to format template 'tpl' for message with code 'MFP1'",
        assertThrowsExactly(MessageFormatException.class, () -> MESSAGE_SUPPORT
            .message(MESSAGE_SUPPORT.getMessageAccessor().getMessageFactory()
                .parseMessage("MFP1", "%[tpl]"))
            .format())
            .getMessage());
  }


  @Test
  @DisplayName("format: failed to format parameter 'ABC' in template 'age-range' for message with code 'MFP2'")
  void testFormat1101()
  {
    MESSAGE_SUPPORT.addTemplate("age-range", MESSAGE_SUPPORT.getMessageAccessor()
        .getMessageFactory().parseMessage("%{ABC,throw}"));

    assertEquals("failed to format parameter 'ABC' in template 'age-range' for message with code 'MFP2'",
        assertThrowsExactly(MessageFormatException.class, () -> MESSAGE_SUPPORT
            .message(MESSAGE_SUPPORT.getMessageAccessor().getMessageFactory()
                .parseMessage("MFP2", "%[age-range]"))
            .format())
            .getMessage());
  }


  @Test
  @DisplayName("format: failed to format template 'tpl' for message with code 'MFP3' and locale Dutch (Belgium)")
  void testFormat1110()
  {
    val lmMap = new HashMap<Locale,String>();

    lmMap.put(GERMANY, "%{n,throw}");
    lmMap.put(new Locale("nl", ""), "%[tpl]");

    assertEquals("failed to format template 'tpl' for message with code 'MFP3' and locale Dutch (Belgium)",
        assertThrowsExactly(MessageFormatException.class, () -> MESSAGE_SUPPORT
            .message(MESSAGE_SUPPORT.getMessageAccessor().getMessageFactory()
                .parseMessage("MFP3", lmMap))
            .locale(new Locale("nl", "BE"))
            .format())
            .getMessage());
  }


  @Test
  @DisplayName("format: failed to format parameter 'j' in template 'bool' for message with code 'MFP4' and locale French (Switzerland)")
  void testFormat1111()
  {
    MESSAGE_SUPPORT.addTemplate("bool", MESSAGE_SUPPORT.getMessageAccessor()
        .getMessageFactory().parseMessage("%{j,throw}"));

    val lmMap = new HashMap<Locale,String>();

    lmMap.put(GERMANY, "%{n,throw}");
    lmMap.put(FRANCE, "%[bool]");

    assertEquals("failed to format parameter 'j' in template 'bool' for message with code 'MFP4' and locale French (Switzerland)",
        assertThrowsExactly(MessageFormatException.class, () -> MESSAGE_SUPPORT
            .message(MESSAGE_SUPPORT.getMessageAccessor().getMessageFactory()
                .parseMessage("MFP4", lmMap))
            .locale(new Locale("fr", "CH"))
            .format())
            .getMessage());
  }
}
