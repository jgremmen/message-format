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
package de.sayayi.lib.message.formatter.parameter.runtime.extra;

import de.sayayi.lib.message.Message.Parameters;
import de.sayayi.lib.message.MessageBuilder;
import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.parameter.AbstractSingleTypeParameterFormatter;
import de.sayayi.lib.message.formatter.parameter.ParameterFormatterContext;
import de.sayayi.lib.message.part.MessagePart.Text;
import de.sayayi.lib.message.part.TextPartFactory;
import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static de.sayayi.lib.message.part.TextPartFactory.*;


/**
 * Parameter formatter for ANTLR {@link Token} values. This formatter extracts various aspects of a token depending on
 * the {@code token} configuration value.
 * <p>
 * <b>Configuration keys:</b>
 * <table border="1">
 *   <caption>Configuration keys for the token formatter</caption>
 *   <tr><th>Key</th><th>Values</th><th>Default</th><th>Purpose</th></tr>
 *   <tr>
 *     <td>{@code token}</td>
 *     <td>{@code text}, {@code type}, {@code channel}, {@code line}, {@code column}, {@code position}</td>
 *     <td>{@code text}</td>
 *     <td>
 *       Formats the token text, the token type number, the channel number, the line number, the column number or
 *       the combined position. Line, column and position are empty if the token has no such information. Any
 *       other value results in a {@code null} text.
 *     </td>
 *   </tr>
 *   <tr>
 *     <td>{@code token-1st-line}</td>
 *     <td>number</td>
 *     <td>{@code 1}</td>
 *     <td>Number that is reported for the first line.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code token-1st-column}</td>
 *     <td>number</td>
 *     <td>{@code 0}</td>
 *     <td>Number that is reported for the first column.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code token-position-format}</td>
 *     <td>message</td>
 *     <td>{@code %{line}%{column,!empty:':%{column}'}}</td>
 *     <td>
 *       Message used to format the position. The parameters {@code line} and {@code column} are available in the
 *       message. By default it renders as {@code line:column} or just {@code line} if the column is empty.
 *     </td>
 *   </tr>
 * </table>
 * <p>
 * <b>Examples:</b>
 * <p>
 * This formatter is not part of the {@link de.sayayi.lib.message.formatter.DefaultFormatterService
 * DefaultFormatterService} and must be registered explicitly:
 * <pre>{@code
 * var formatterService = new DefaultFormatterService();
 * formatterService.addFormatter(new TokenFormatter());
 *
 * var messageSupport = MessageSupportFactory.create(formatterService).seal();
 * }</pre>
 * <p>
 * Report a syntax error at the position of a token. ANTLR columns start at 0, which is adjustable:
 * <pre>{@code
 * var token = new CommonToken(Token.MIN_USER_TOKEN_TYPE, "bloolean");
 * token.setLine(12);
 * token.setCharPositionInLine(4);
 *
 * messageSupport
 *     .message("Unexpected '%{t}' at %{t,token:position}")
 *     .with("t", token)
 *     .format();
 * // "Unexpected 'bloolean' at 12:4"
 *
 * messageSupport
 *     .message("Line %{t,token:line}, column %{t,token:column,token-1st-column:1}")
 *     .with("t", token)
 *     .format();
 * // "Line 12, column 5"
 * }</pre>
 * <p>
 * Use a custom format for the position:
 * <pre>{@code
 * var token = new CommonToken(Token.MIN_USER_TOKEN_TYPE, "bloolean");
 * token.setLine(12);
 * token.setCharPositionInLine(4);
 *
 * messageSupport
 *     .message("at %{t,token:position,token-position-format:'(%{line}, %{column})'}")
 *     .with("t", token)
 *     .format();
 * // "at (12, 4)"
 * }</pre>
 * <p>
 * A token without line information, e.g. a synthetic one, has no position:
 * <pre>{@code
 * var token = new CommonToken(Token.EOF, "<EOF>");
 *
 * messageSupport.message("%{t,token:position,empty:'at end of input'}").with("t", token).format();
 * // "at end of input"
 * }</pre>
 *
 * @author Jeroen Gremmen
 * @since 0.24.0
 */
public final class TokenFormatter extends AbstractSingleTypeParameterFormatter<Token>
{
  /**
   * Formats the given ANTLR {@code token} based on the {@code token} configuration value in the formatting context.
   *
   * @param context  parameter formatter context providing configuration values, not {@code null}
   * @param token    the ANTLR token to format, not {@code null}
   *
   * @return  the formatted token text, never {@code null}
   */
  @Override
  protected @NotNull Text formatValue(@NotNull ParameterFormatterContext context, @NotNull Token token)
  {
    return switch(context.getConfigValueString("token").orElse("text")) {
      case "text" -> noSpaceText(token.getText());

      case "type" -> context.format(token.getType(), int.class);

      case "channel" -> context.format(token.getChannel(), int.class);

      case "line" -> getLine(context, token)
          .map(line -> context.format(line, int.class))
          .orElseGet(TextPartFactory::emptyText);

      case "column" -> getColumn(context, token)
          .map(column -> context.format(column, int.class))
          .orElseGet(TextPartFactory::emptyText);

      case "position" -> {
        if (token.getLine() < 1)
          yield emptyText();

        yield context
            .getConfigValueMessage("token-position-format")
            .orElseGet(() -> MessageBuilder
                .create(context)
                .parameter("line")
                .parameter("column")
                    .mapEmpty().ne().message(mb -> mb.text(":").parameter("column"))
                .build())
            .formatAsText(context.getMessageAccessor(), new PositionParameters(context, token));
      }

      default -> nullText();
    };
  }


  /**
   * {@inheritDoc}
   *
   * @return  formattable type for ANTLR {@link Token}, never {@code null}
   */
  @Override
  protected @NotNull FormattableType getFormattableType() {
    return new FormattableType(Token.class);
  }


  /**
   * Returns the adjusted line number of the given token, or empty if the token has no valid line information.
   * The line number is adjusted using the {@code token-1st-line} configuration value (default {@code 1}).
   *
   * @param context  parameter formatter context providing configuration values, not {@code null}
   * @param token    the ANTLR token to extract the line number from, not {@code null}
   *
   * @return  an optional containing the adjusted line number, or empty if the token has no line information
   */
  @Contract(pure = true)
  private static @NotNull Optional<Integer> getLine(@NotNull ParameterFormatterContext context, @NotNull Token token)
  {
    final var line = token.getLine();

    return line < 1
        ? Optional.empty()
        : Optional.of(line + context.getConfigValueInt("token-1st-line").orElse(1) - 1);
  }


  /**
   * Returns the adjusted column number of the given token, or empty if the token has no valid column information.
   * The column number is adjusted using the {@code token-1st-column} configuration value (default {@code 0}).
   *
   * @param context  parameter formatter context providing configuration values, not {@code null}
   * @param token    the ANTLR token to extract the column number from, not {@code null}
   *
   * @return  an optional containing the adjusted column number, or empty if the token has no column information
   */
  @Contract(pure = true)
  private static @NotNull Optional<Integer> getColumn(@NotNull ParameterFormatterContext context, @NotNull Token token)
  {
    final var column = token.getCharPositionInLine();

    return column < 0
        ? Optional.empty()
        : Optional.of(column + context.getConfigValueInt("token-1st-column").orElse(0));
  }




  /**
   * Message parameters implementation that provides {@code line} and {@code column} values for the position format
   * message. This class supplies the adjusted line and column numbers of a token to the position format template.
   *
   * @since 0.24.0
   */
  @SuppressWarnings({"ClassCanBeRecord", "OptionalGetWithoutIsPresent"})
  private static class PositionParameters implements Parameters
  {
    /** Formatter context providing the line and column configuration. */
    private final ParameterFormatterContext context;

    /** Token whose position is provided. */
    private final Token token;


    /**
     * Creates a new position parameters instance for the given context and token.
     *
     * @param context  parameter formatter context providing configuration values, not {@code null}
     * @param token    the ANTLR token to extract position information from, not {@code null}
     */
    private PositionParameters(@NotNull ParameterFormatterContext context, @NotNull Token token)
    {
      this.context = context;
      this.token = token;
    }


    /**
     * {@inheritDoc}
     *
     * @return  the locale from the formatting context, never {@code null}
     */
    @Override
    public @NotNull Locale getLocale() {
      return context.getLocale();
    }


    /**
     * Returns the value for the given parameter name. Supported parameters are {@code "line"} and {@code "column"}.
     *
     * @param parameter  parameter name to look up, not {@code null}
     *
     * @return  the adjusted line or column number, or {@code null} if the parameter is not recognized or the column
     *          is not available
     */
    @Override
    public Integer getParameterValue(@NotNull String parameter)
    {
      return switch(parameter) {
        case "line" -> getLine(context, token).get();
        case "column" -> getColumn(context, token).orElse(null);
        default -> null;
      };
    }


    /**
     * Returns all position parameters as an unmodifiable map containing {@code "line"} and {@code "column"} entries.
     *
     * @return  unmodifiable map with line and column values, never {@code null}
     */
    @Override
    public @Unmodifiable @NotNull Map<String,Object> asParameterMap()
    {
      final var parameters = new HashMap<String,Object>();

      parameters.put("line", getLine(context, token).get());
      getColumn(context, token).ifPresent(column -> parameters.put("column", column));

      return Map.copyOf(parameters);
    }
  }
}
