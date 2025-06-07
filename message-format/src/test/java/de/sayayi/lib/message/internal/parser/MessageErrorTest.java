package de.sayayi.lib.message.internal.parser;

import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.exception.MessageParserException;
import de.sayayi.lib.message.part.normalizer.LRUMessagePartNormalizer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;


/**
 * @author Jeroen Gremmen
 * @since 0.20.0
 */
@DisplayName("Message lexer/parser errors")
public class MessageErrorTest
{
  private static MessageCompiler COMPILER;


  @BeforeAll
  static void init() {
    COMPILER = new MessageCompiler(new MessageFactory(new LRUMessagePartNormalizer(64)));
    //COMPILER.compileMessage("%{p,:");
  }


  @Test
  @DisplayName("Token recognition message")
  void tokenRecognitionMessage()
  {
    var ex = assertThrowsExactly(MessageParserException.class,
        () -> COMPILER.compileMessage("%{r{"));
    assertEquals("message syntax error at '{'", ex.getErrorMessage());
  }


  @Test
  @DisplayName("Input mismatch message")
  void inputMismatchMessage()
  {
    var ex = assertThrowsExactly(MessageParserException.class,
        () -> COMPILER.compileMessage("%{64"));
    assertEquals("missing parameter name at '64'", ex.getErrorMessage());

    ex = assertThrowsExactly(MessageParserException.class,
        () -> COMPILER.compileMessage("%{p'"));
    assertEquals("end of message parameter expected at \"'\"", ex.getErrorMessage());

    ex = assertThrowsExactly(MessageParserException.class,
        () -> COMPILER.compileMessage("%{p,:"));
    assertEquals("pre-mature end of message parameter reached; missing default message", ex.getErrorMessage());
  }


  @Test
  @DisplayName("No viable alternative message")
  void parserNoViableAlternative()
  {
    var ex = assertThrowsExactly(MessageParserException.class,
        () -> COMPILER.compileMessage("%{r,true:!}"));
    assertEquals("syntax error in message parameter map element at '!'", ex.getErrorMessage());
  }


  @Test
  @DisplayName("Unwanted token message")
  void unwantedTokenMessage()
  {
    var ex = assertThrowsExactly(MessageParserException.class,
        () -> COMPILER.compileMessage("%{r,d:6"));
    assertEquals("pre-mature end of message parameter reached; missing '}'", ex.getErrorMessage());
  }
}
