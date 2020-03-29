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
import de.sayayi.lib.message.parser.MsgParser.DataContext;
import de.sayayi.lib.message.parser.MsgParser.Message0Context;
import de.sayayi.lib.message.parser.MsgParser.ParameterContext;
import de.sayayi.lib.message.parser.MsgParser.TextPartContext;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.List;


/**
 * @author Jeroen Gremmen
 */
public final class MessageBuildListener extends MsgParserBaseListener
{
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
    TerminalNode tn = ctx.getToken(MessageTokenizer.CH, i);
    if (tn == null)
      return false;

    String text = tn.getSymbol().getText();
    if (text == null || text.isEmpty())
      return false;

    return text.charAt(0) == ' ';
  }
}
