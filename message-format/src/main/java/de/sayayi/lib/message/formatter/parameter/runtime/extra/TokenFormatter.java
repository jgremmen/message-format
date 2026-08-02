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
 * the {@code token} configuration value:
 * <ul>
 *   <li>{@code "text"} (default) formats the token text</li>
 *   <li>{@code "type"} formats the token type number</li>
 *   <li>{@code "channel"} formats the token channel number</li>
 *   <li>{@code "line"} formats the token line number</li>
 *   <li>{@code "column"} formats the token column number</li>
 *   <li>{@code "position"} formats the token position as a combined line and column string</li>
 * </ul>
 * <p>
 * The line and column numbers can be adjusted using the {@code token-1st-line} (default {@code 1}) and
 * {@code token-1st-column} (default {@code 0}) configuration values.
 * <p>
 * The position format can be customized with the {@code token-position-format} configuration message. If no custom
 * format is provided, the default position format is {@code %{line}%{column,!empty:':%{column}'}} which renders as
 * {@code line:column} when a column is available, or just {@code line} when the column is empty.
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
   */
  @SuppressWarnings({"ClassCanBeRecord", "OptionalGetWithoutIsPresent"})
  private static class PositionParameters implements Parameters
  {
    private final ParameterFormatterContext context;
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
