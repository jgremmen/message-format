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
import de.sayayi.lib.message.formatter.GenericFormatterService;
import de.sayayi.lib.message.formatter.parameter.NamedParameterFormatter;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext;
import de.sayayi.lib.message.internal.MessageTemplate;
import de.sayayi.lib.message.internal.message.LocalizedMessageBundleWithCode;
import de.sayayi.lib.message.internal.message.MessageDelegateWithCode;
import de.sayayi.lib.message.internal.message.TextMessage;
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.template.JavaTemplate;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Locale.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * Tests {@link MessageFormatException}: creation of the detail message from every combination of recorded context, and
 * preservation of the stack trace when adding context.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
@DisplayName("Message format exception")
@TestMethodOrder(MethodOrderer.DisplayName.class)
@SuppressWarnings({"ResultOfMethodCallIgnored", "ThrowableNotThrown"})
final class MessageFormatExceptionTest
{
  private static final MessageSupport.ConfigurableMessageSupport MESSAGE_SUPPORT;
  private static final Message THROWING_MESSAGE;


  static
  {
    var formatterService = new GenericFormatterService();
    var formatter = mock(NamedParameterFormatter.class, CALLS_REAL_METHODS);

    when(formatter.getName()).thenReturn("throw");
    when(formatter.format(any(ParameterFormatterContext.class), any()))
        .thenAnswer(invocation -> { throw new MessageFormatException(null); });

    formatterService.addFormatter(formatter);

    MESSAGE_SUPPORT = MessageSupportFactory.create(formatterService);
    THROWING_MESSAGE = mock(TextMessage.class, CALLS_REAL_METHODS);
    when(THROWING_MESSAGE.formatAsText(any(MessageAccessor.class), any(Parameters.class)))
        .thenAnswer(invocation -> { throw new MessageFormatException(null); });

    MESSAGE_SUPPORT.addTemplate("tpl", new MessageTemplate(THROWING_MESSAGE));
    MESSAGE_SUPPORT.addTemplate(new JavaTemplate() {
      @Override
      public @NotNull String getName() {
        return "throwing-tpl";
      }

      @Override
      public @NotNull Text formatAsText(@NotNull MessageAccessor messageAccessor, @NotNull Parameters parameters) {
        throw new IllegalStateException("template failure");
      }
    });
  }


  static Stream<Arguments> getMessage_parameters()
  {
    return Stream.of(
        Arguments.of(null, null, null, null,
            "failed to format message",
            "failed to format post formatter 'clip'"),
        Arguments.of(null, null, null, "p",
            "failed to format parameter 'p'",
            "failed to format sub-message parameter 'p' of post formatter 'clip'"),
        Arguments.of(null, null, GERMANY, null,
            "failed to format message using locale German (Germany)",
            "failed to format post formatter 'clip' using locale German (Germany)"),
        Arguments.of(null, null, GERMAN, "n",
            "failed to format parameter 'n' using locale German",
            "failed to format sub-message parameter 'n' of post formatter 'clip' using locale German"),
        Arguments.of(null, "msg", null, null,
            "failed to format template 'msg'",
            "failed to format post formatter 'clip' in template 'msg'"),
        Arguments.of(null, "msg-with-default", null, "m",
            "failed to format parameter 'm' in template 'msg-with-default'",
            "failed to format sub-message parameter 'm' of post formatter 'clip' in template 'msg-with-default'"),
        Arguments.of(null, "tpl", UK, null,
            "failed to format template 'tpl' using locale English (United Kingdom)",
            "failed to format post formatter 'clip' in template 'tpl' using locale English (United Kingdom)"),
        Arguments.of(null, "name", ITALY, "p",
            "failed to format parameter 'p' in template 'name' using locale Italian (Italy)",
            "failed to format sub-message parameter 'p' of post formatter 'clip' in template 'name' using locale " +
            "Italian (Italy)"),
        Arguments.of("MSG-001", null, null, null,
            "failed to format message with code 'MSG-001'",
            "message 'MSG-001' failed to format post formatter 'clip'"),
        Arguments.of("MSG-002", null, null, "number",
            "message 'MSG-002' failed to format parameter 'number'",
            "message 'MSG-002' failed to format sub-message parameter 'number' of post formatter 'clip'"),
        Arguments.of("MSG-003", null, TRADITIONAL_CHINESE, null,
            "failed to format message with code 'MSG-003' using locale Chinese (Taiwan)",
            "message 'MSG-003' failed to format post formatter 'clip' using locale Chinese (Taiwan)"),
        Arguments.of("MSG-004", null, US, "flag",
            "message 'MSG-004' failed to format parameter 'flag' using locale English (United States)",
            "message 'MSG-004' failed to format sub-message parameter 'flag' of post formatter 'clip' using locale " +
            "English (United States)"),
        Arguments.of("MFP1", "flow", null, null,
            "message 'MFP1' failed to format template 'flow'",
            "message 'MFP1' failed to format post formatter 'clip' in template 'flow'"),
        Arguments.of("MFP2", "age-range", null, "ABC",
            "message 'MFP2' failed to format parameter 'ABC' in template 'age-range'",
            "message 'MFP2' failed to format sub-message parameter 'ABC' of post formatter 'clip' in template " +
            "'age-range'"),
        Arguments.of("MFP3", "test", Locale.forLanguageTag("nl-BE"), null,
            "message 'MFP3' failed to format template 'test' using locale Dutch (Belgium)",
            "message 'MFP3' failed to format post formatter 'clip' in template 'test' using locale Dutch (Belgium)"),
        Arguments.of("MFP4", "bool", Locale.forLanguageTag("fr-CH"), "j",
            "message 'MFP4' failed to format parameter 'j' in template 'bool' using locale French (Switzerland)",
            "message 'MFP4' failed to format sub-message parameter 'j' of post formatter 'clip' in template 'bool' " +
            "using locale French (Switzerland)")
    );
  }


  @DisplayName("Detailed exception messages")
  @ParameterizedTest(name = "msg: {4}")
  @MethodSource("getMessage_parameters")
  void getMessage(String code, String template, Locale locale, String parameter, @NotNull String msg,
                      @SuppressWarnings("unused") String msgWithPostFormatter) {
    assertEquals(msg, exceptionWith(code, template, locale, parameter, null).getMessage());
  }


  @Test
  @DisplayName("Exclude generated code from detailed exception message")
  void generatedMessageCode()
  {
    assertEquals("failed to format parameter 'q' using locale Dutch (Netherlands)",
        MessageFormatException.of(null)
            .withParameter("q")
            .withCode("MSG[5JJRMZBVNJ-30UO]")
            .withLocale(Locale.forLanguageTag("nl-NL"))
            .getMessage());
  }


  @Test
  @DisplayName("format: failed to format message")
  void format0000()
  {
    assertFormatFails(
        "failed to format message",
        () -> MESSAGE_SUPPORT
            .message(THROWING_MESSAGE)
            .format());
  }


  @Test
  @DisplayName("format: failed to format message parameter 'p'")
  void format0001()
  {
    assertFormatFails(
        "failed to format parameter 'p'",
        () -> MESSAGE_SUPPORT
            .message("%{p,format:throw}")
            .with("p", new NullPointerException())
            .format());
  }


  @Test
  @DisplayName("format: failed to format message for locale German (Germany)")
  void format0010()
  {
    assertFormatFails(
        "failed to format message using locale German (Germany)",
        () -> MESSAGE_SUPPORT
            .message(new LocalizedMessageBundleWithCode("MSG[ABC]",
                Map.of(Locale.forLanguageTag("de-CH"), THROWING_MESSAGE)))
            .locale(GERMANY)
            .format());
  }


  @Test
  @DisplayName("format: failed to format message parameter 'n' for locale German")
  void format0011()
  {
    var lmMap = new HashMap<Locale,String>();

    lmMap.put(GERMANY, "%{n,format:throw}");
    lmMap.put(ENGLISH, "%{e}");

    assertFormatFails(
        "failed to format parameter 'n' using locale German",
        () -> MESSAGE_SUPPORT
            .message(MESSAGE_SUPPORT.getMessageAccessor().getMessageFactory().parseMessage(lmMap))
            .with("n", new NullPointerException())
            .locale(GERMAN)
            .format());
  }


  @Test
  @DisplayName("format: failed to format template 'msg'")
  void format0100()
  {
    assertFormatFails(
        "failed to format template 'tpl'",
        () -> MESSAGE_SUPPORT
            .message("%[tpl]")
            .format());
  }


  @Test
  @DisplayName("format: failed to format parameter 'm' in template 'msg-with-default'")
  void format0101()
  {
    MESSAGE_SUPPORT.addTemplate("msg-with-default", MESSAGE_SUPPORT.getMessageAccessor()
        .getMessageFactory().parseTemplate("%{m,format:throw}"));

    assertFormatFails(
        "failed to format parameter 'm' in template 'msg-with-default'",
        () -> MESSAGE_SUPPORT
            .message("%[msg-with-default]")
            .format());
  }


  @Test
  @DisplayName("format: failed to format template 'tpl' for locale English (United Kingdom)")
  void format0110()
  {
    var lmMap = new HashMap<Locale,String>();

    lmMap.put(GERMANY, "%{n,format:throw}");
    lmMap.put(ENGLISH, "%[tpl]");

    assertFormatFails(
        "failed to format template 'tpl' using locale English (United Kingdom)",
        () -> MESSAGE_SUPPORT
            .message(MESSAGE_SUPPORT.getMessageAccessor().getMessageFactory().parseMessage(lmMap))
            .locale(UK)
            .format());
  }


  @Test
  @DisplayName("format: failed to format parameter 'p' in template 'name' for locale Italian (Italy)")
  void format0111()
  {
    MESSAGE_SUPPORT.addTemplate("name", MESSAGE_SUPPORT.getMessageAccessor()
        .getMessageFactory().parseTemplate("%{p,format:throw}"));

    var lmMap = new HashMap<Locale,String>();

    lmMap.put(GERMANY, "%{n,format:throw}");
    lmMap.put(ITALIAN, "%[name]");

    assertFormatFails(
        "failed to format parameter 'p' in template 'name' using locale Italian (Italy)",
        () -> MESSAGE_SUPPORT
            .message(MESSAGE_SUPPORT.getMessageAccessor().getMessageFactory().parseMessage(lmMap))
            .locale(ITALY)
            .format());
  }


  @Test
  @DisplayName("format: failed to format message with code 'MSG-001'")
  void format1000()
  {
    MESSAGE_SUPPORT.addMessage(new MessageDelegateWithCode("MSG-001", THROWING_MESSAGE));

    assertFormatFails(
        "failed to format message with code 'MSG-001'",
        () -> MESSAGE_SUPPORT
            .code("MSG-001")
            .format());
  }


  @Test
  @DisplayName("format: failed to format parameter 'number' for message with code 'MSG-002'")
  void format1001()
  {
    MESSAGE_SUPPORT.addMessage("MSG-002", "%{number,format:throw}");

    assertFormatFails(
        "message 'MSG-002' failed to format parameter 'number'",
        () -> MESSAGE_SUPPORT
            .code("MSG-002")
            .format());
  }


  @Test
  @DisplayName("format: failed to format message with code 'MSG-003' and locale Chinese (Taiwan)")
  void format1010()
  {
    assertFormatFails(
        "failed to format message with code 'MSG-003' using locale Chinese (Taiwan)",
        () -> MESSAGE_SUPPORT
            .message(new LocalizedMessageBundleWithCode("MSG-003",
                Map.of(Locale.forLanguageTag("de-CH"), THROWING_MESSAGE)))
            .locale(TRADITIONAL_CHINESE)
            .format());
  }


  @Test
  @DisplayName("format: failed to format parameter 'flag' for message with code 'MSG-004' and locale English (United States)")
  void format1011()
  {
    var lmMap = new HashMap<Locale,String>();

    lmMap.put(GERMANY, "%{n,format:throw}");
    lmMap.put(ENGLISH, "%{flag,format:throw}");

    assertFormatFails(
        "message 'MSG-004' failed to format parameter 'flag' using locale English (United States)",
        () -> MESSAGE_SUPPORT
            .message(MESSAGE_SUPPORT.getMessageAccessor().getMessageFactory().parseMessage("MSG-004", lmMap))
            .locale(US)
            .format());
  }


  @Test
  @DisplayName("format: failed to format template 'tpl' for message with code 'MFP1'")
  void format1100()
  {
    assertFormatFails(
        "message 'MFP1' failed to format template 'tpl'",
        () -> MESSAGE_SUPPORT
            .message(MESSAGE_SUPPORT.getMessageAccessor().getMessageFactory()
                .parseMessage("MFP1", "%[tpl]"))
            .format());
  }


  @Test
  @DisplayName("format: failed to format parameter 'abc' in template 'age-range' for message with code 'MFP2'")
  void format1101()
  {
    MESSAGE_SUPPORT.addTemplate("age-range", MESSAGE_SUPPORT.getMessageAccessor()
        .getMessageFactory().parseTemplate("%{abc,format:throw}"));

    assertFormatFails(
        "message 'MFP2' failed to format parameter 'abc' in template 'age-range'",
        () -> MESSAGE_SUPPORT
            .message(MESSAGE_SUPPORT.getMessageAccessor().getMessageFactory()
                .parseMessage("MFP2", "%[age-range]"))
            .format());
  }


  @Test
  @DisplayName("format: failed to format template 'tpl' for message with code 'MFP3' and locale Dutch (Belgium)")
  void format1110()
  {
    var lmMap = new HashMap<Locale,String>();

    lmMap.put(GERMANY, "%{n,format:throw}");
    lmMap.put(Locale.forLanguageTag("nl"), "%[tpl]");

    assertFormatFails(
        "message 'MFP3' failed to format template 'tpl' using locale Dutch (Belgium)",
        () -> MESSAGE_SUPPORT
            .message(MESSAGE_SUPPORT.getMessageAccessor().getMessageFactory()
                .parseMessage("MFP3", lmMap))
            .locale(Locale.forLanguageTag("nl-BE"))
            .format());
  }


  @Test
  @DisplayName("format: failed to format parameter 'j' in template 'bool' for message with code 'MFP4' and locale French (Switzerland)")
  void format1111()
  {
    MESSAGE_SUPPORT.addTemplate("bool", MESSAGE_SUPPORT.getMessageAccessor()
        .getMessageFactory().parseTemplate("%{j,format:throw}"));

    var lmMap = new HashMap<Locale,String>();

    lmMap.put(GERMANY, "%{n,format:throw}");
    lmMap.put(FRANCE, "%[bool]");

    assertFormatFails(
        "message 'MFP4' failed to format parameter 'j' in template 'bool' using locale French (Switzerland)",
        () -> MESSAGE_SUPPORT
            .message(MESSAGE_SUPPORT.getMessageAccessor().getMessageFactory()
                .parseMessage("MFP4", lmMap))
            .locale(Locale.forLanguageTag("fr-CH"))
            .format());
  }


  @Test
  @DisplayName("with...: copies keep the original stack trace, cause and suppressed exceptions")
  void withKeepsStackTrace()
  {
    var cause = new IllegalStateException("cause");
    var suppressed = new IllegalArgumentException("suppressed");
    var ex = MessageFormatException.of(cause);
    ex.addSuppressed(suppressed);
    var stackTrace = ex.getStackTrace();

    var copy = ex
        .withParameter("p")
        .withTemplate("tpl")
        .withLocale(GERMANY)
        .withCode("MSG-1");

    assertArrayEquals(stackTrace, copy.getStackTrace());
    assertSame(cause, copy.getCause());
    assertArrayEquals(new Throwable[] { suppressed }, copy.getSuppressed());
  }


  @DisplayName("Detailed exception messages with post formatter")
  @ParameterizedTest(name = "msg: {5}")
  @MethodSource("getMessage_parameters")
  void getMessageWithPostFormatter(String code, String template, Locale locale, String parameter,
                                       @SuppressWarnings("unused") String msg, @NotNull String msgWithPostFormatter)
  {
    var ex = exceptionWith(code, template, locale, parameter, "clip");

    assertEquals("clip", ex.getPostFormatter());
    assertEquals(msgWithPostFormatter, ex.getMessage());
  }


  @Test
  @DisplayName("format: unregistered post formatter is reported with its name")
  void formatUnregisteredPostFormatter()
  {
    var ex = assertFormatFails(
        "failed to format post formatter 'unknown-pf'",
        () -> MESSAGE_SUPPORT
            .message("%(unknown-pf,'text')")
            .format());

    assertEquals("unknown-pf", ex.getPostFormatter());
    assertInstanceOf(IllegalStateException.class, ex.getCause());
  }


  @Test
  @DisplayName("format: failed to format message parameter 'n' in post formatter 'case'")
  void formatParameterInPostFormatter()
  {
    assertFormatFails(
        "failed to format sub-message parameter 'n' of post formatter 'case'",
        () -> MESSAGE_SUPPORT
            .message("%(case,'%{n,format:throw}',case:upper)")
            .format());
  }


  @Test
  @DisplayName("format: failed to format parameter 'n' in post formatter 'case' for message with code 'MFP5'")
  void formatParameterInPostFormatterWithCode()
  {
    MESSAGE_SUPPORT.addMessage("MFP5", "%(case,'%{n,format:throw}',case:upper)");

    assertFormatFails(
        "message 'MFP5' failed to format sub-message parameter 'n' of post formatter 'case'",
        () -> MESSAGE_SUPPORT.code("MFP5").format());
  }


  @Test
  @DisplayName("format: failed to format parameter 'x' in template 'nested' in parameter 'p'")
  void formatParameterInTemplateInParameter()
  {
    MESSAGE_SUPPORT.addTemplate("nested", MESSAGE_SUPPORT.getMessageAccessor()
        .getMessageFactory().parseTemplate("%{x,format:throw}"));

    assertFormatFails(
        "failed to format parameter 'x' in template 'nested' for mapped message of parameter 'p'",
        () -> MESSAGE_SUPPORT
            .message("%{p,null:'%[nested]'}")
            .format());
  }


  @Test
  @DisplayName("format: failed to format post formatter 'unknown-pf' in parameter 'p' for message with code 'MFP6'")
  void formatPostFormatterInParameterWithCode()
  {
    MESSAGE_SUPPORT.addMessage("MFP6", "%{p,null:\"%(unknown-pf,'text')\"}");

    assertFormatFails(
        "message 'MFP6' failed to format post formatter 'unknown-pf' in mapped message of parameter 'p'",
        () -> MESSAGE_SUPPORT
            .code("MFP6")
            .format());
  }


  @Test
  @DisplayName("with...: context is numbered per kind and getters return the innermost value")
  void contextNumbering()
  {
    var ex = MessageFormatException.of(null)
        .withParameter("inner")
        .withTemplate("tpl")
        .withParameter("outer")
        .withCode("MSG-1");

    assertEquals("inner", ex.getParameter());
    assertEquals("tpl", ex.getTemplate());
    assertEquals("MSG-1", ex.getCode());
    assertNull(ex.getLocale());
    assertEquals("message 'MSG-1' failed to format parameter 'inner' in template 'tpl' for mapped message of parameter 'outer'", ex.getMessage());
  }


  @Test
  @DisplayName("with...: code is recorded once")
  void codeRecordedOnce()
  {
    var ex = MessageFormatException.of(null)
        .withParameter("p")
        .withCode("MSG-1")
        .withTemplate("tpl")
        .withCode("MSG-2");

    assertEquals("MSG-1", ex.getCode());
    assertEquals("message 'MSG-1' failed to format parameter 'p' in template 'tpl'", ex.getMessage());
  }


  @Test
  @DisplayName("with...: locale is recorded once and kept for a partial match")
  void localeRecordedOnce()
  {
    var ex = MessageFormatException.of(null)
        .withLocale(GERMANY)
        .withParameter("p1")
        .withParameter("p2")
        .withParameter("p3")
        .withLocale(FRANCE);

    assertEquals(GERMANY, ex.getLocale());
    assertEquals("failed to format parameter 'p1' in mapped message of parameter 'p2' using locale German (Germany)", ex.getMessage());
  }


  /**
   * Returns the registered context key sequences which can be produced by formatting a real message. Sequences
   * without a message part cannot be produced, as every failure originates in a parameter, post formatter or template
   * part.
   */
  private static Stream<String> producibleContextKeys()
  {
    return MessageFormatException
        .getContextKeys()
        .stream()
        .filter(key -> !getParts(key).isEmpty());
  }


  @DisplayName("format: context key sequence")
  @ParameterizedTest(name = "sequence: {0}")
  @MethodSource("producibleContextKeys")
  @SuppressWarnings({"LanguageMismatch", "DataFlowIssue"})
  void contextKeySequence(@NotNull String sequence)
  {
    var messageFactory = MESSAGE_SUPPORT.getMessageAccessor().getMessageFactory();
    final var keys = List.of(sequence.split("-"));
    String format = null;

    // build the format from the innermost part outwards
    for(var part: getParts(sequence))
    {
      if (format == null)
      {
        format = part.startsWith("pf")
            ? "%(" + part + ",'text')"
            : part.startsWith("p") ? "%{" + part + ",format:throw}" : "%[throwing-tpl]";
      }
      else if (part.startsWith("pf"))
        format = "%(" + part + ",'" + format + "')";
      else if (part.startsWith("p"))
        format = "%{" + part + ",null:'" + format + "'}";
      else
      {
        final var templateName = "tpl-" + sequence;

        MESSAGE_SUPPORT.addTemplate(templateName, messageFactory.parseTemplate(format));
        format = "%[" + templateName + "]";
      }
    }

    final var hasCode = keys.contains("c");
    final Message message;

    if (keys.contains("l"))
    {
      final var localizedFormats = Map.of(GERMANY, format, ENGLISH, "text");

      message = hasCode
          ? messageFactory.parseMessage("MSG-SEQ", localizedFormats)
          : messageFactory.parseMessage(localizedFormats);
    }
    else
    {
      message = hasCode
          ? messageFactory.parseMessage("MSG-SEQ", format)
          : messageFactory.parseMessage(format);
    }

    var ex = assertThrowsExactly(MessageFormatException.class, () -> MESSAGE_SUPPORT
        .message(message)
        .locale(GERMANY)
        .format());

    assertEquals(sequence, ex.getContextKey(), format);
  }


  private static @NotNull List<String> getParts(@NotNull String sequence)
  {
    return Stream
        .of(sequence.split("-"))
        .filter(key -> !key.isEmpty() && !key.equals("l") && !key.equals("c"))
        .toList();
  }


  private static @NotNull MessageFormatException exceptionWith(String code, String template, Locale locale,
                                                                String parameter, String postFormatter)
  {
    var ex = MessageFormatException.of(null);

    if (parameter != null)
      ex = ex.withParameter(parameter);
    if (postFormatter != null)
      ex = ex.withPostFormatter(postFormatter);
    if (template != null)
      ex = ex.withTemplate(template);
    if (code != null)
      ex = ex.withCode(code);
    if (locale != null)
      ex = ex.withLocale(locale);

    return ex;
  }


  private static @NotNull MessageFormatException assertFormatFails(@NotNull String expectedMessage,
                                                                   @NotNull Executable executable)
  {
    var ex = assertThrowsExactly(MessageFormatException.class, executable);

    assertEquals(expectedMessage, ex.getMessage());

    return ex;
  }
}
