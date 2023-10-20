package test;

import de.sayayi.lib.message.annotation.MessageDef;


@MessageDef(code = "MSG1", text = "Hello 1")
public class Version000803
{
  @MessageDef(code = "MSG2", text = "Hello 2")
  @MessageDef(code = "MSG3", text = "Hello 3")
  private void method1() {
  }


  class SourceInner1
  {
    @MessageDef(code = "MSG-INNER1", text = "Hello inner")
    void method1() {
    }
  }
}
