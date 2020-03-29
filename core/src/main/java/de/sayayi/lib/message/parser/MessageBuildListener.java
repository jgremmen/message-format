/*
 * Copyright 2020 Jeroen Gremmen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.sayayi.lib.message.parser;

import de.sayayi.lib.message.impl.EmptyMessage;
import de.sayayi.lib.message.impl.MultipartMessage;
import de.sayayi.lib.message.impl.SinglePartMessage;
import de.sayayi.lib.message.parser.MessageParser.DataContext;
import de.sayayi.lib.message.parser.MessageParser.Message0Context;
import de.sayayi.lib.message.parser.MessageParser.ParameterContext;
import de.sayayi.lib.message.parser.MessageParser.TextPartContext;
import lombok.AllArgsConstructor;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;

import java.util.List;


/**
 * @author Jeroen Gremmen
 */
@AllArgsConstructor
public final class MessageBuildListener extends MessageParserBaseListener
{
  private final TokenStream tokenStream;


  @Override
  public void exitTextPart(TextPartContext ctx)
  {
    final String text = ctx.text().value;
    final int length = text.length();

    ctx.value = new TextPart(text.trim(), text.charAt(0) == ' ', text.charAt(length - 1) == ' ');
  }


  @Override
  public void exitMessage0(Message0Context ctx)
  {
    final List<MessagePart> parts = ctx.parts;

    switch(parts.size())
    {
      case 0:
        ctx.value = new EmptyMessage();

      case 1:
        ctx.value = new SinglePartMessage(parts.get(0));

      default:
        ctx.value = new MultipartMessage(parts);
    }
  }


  @Override
  public void exitParameter(ParameterContext ctx)
  {
    final DataContext data = ctx.data();

    ctx.value = new ParameterPart(ctx.name.getText(),
        ctx.format == null ? null : ctx.format.getText(),
        exitParameter_isSpaceAtTokenIndex(ctx, ctx.getStart().getTokenIndex() - 1),
        exitParameter_isSpaceAtTokenIndex(ctx, ctx.getStop().getTokenIndex() + 1),
        data == null ? null : data.value);
  }


  private boolean exitParameter_isSpaceAtTokenIndex(ParserRuleContext ctx, int i)
  {
    if (i < 0)
      return false;

    Token token = tokenStream.get(i);
    if (token.getType() == Token.EOF)
      return false;

    String text = token.getText();
    if (text == null || text.isEmpty())
      return false;

    return text.charAt(0) == ' ';
  }
}
