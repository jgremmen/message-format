package test;

import de.sayayi.lib.message.annotation.MessageDef;
import de.sayayi.lib.message.annotation.TemplateDef;
import de.sayayi.lib.message.annotation.Text;


@MessageDef(code = "MSG-01")
@MessageDef(code = "MSG-02", texts = {
    @Text(locale = "en", text = "Message"),
    @Text(locale = "de", text = "Nachricht")
})
@MessageDef(code = "MSG-03", text = "Text")
@MessageDef(code = "MSG-04", text = "Text %{t}.")
@MessageDef(code = "MSG-05", text = "%{t,empty:'',null:''}")
@MessageDef(code = "MSG-06", text = "%{b,bool,bool:45}")
@MessageDef(code = "MSG-07", text = "%{n,>0:'pos',<10:'small',<>3467864826:''}")
@MessageDef(code = "MSG-08", text = "%{s,'yes':'true','no':'false'}")
public class Version0_8_0 {
}