package de.sayayi.lib.message.parser;

import de.sayayi.lib.message.impl.EmptyMessage;
import de.sayayi.lib.message.impl.MultipartMessage;
import de.sayayi.lib.message.impl.SinglePartMessage;
import de.sayayi.lib.message.parser.MsgParser.MessageContext;
import de.sayayi.lib.message.parser.MsgParser.TextPartContext;

import java.util.List;


public class MessageBuildListener extends MsgParserBaseListener
{
  @Override
  public void exitTextPart(TextPartContext ctx)
  {
    final String text = ctx.text().value;
    final int length = text.length();

    ctx.value = new TextPart(text.trim(), text.charAt(0) == ' ', text.charAt(length - 1) == ' ');
  }


  @Override
  public void exitMessage(MessageContext ctx)
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
}
