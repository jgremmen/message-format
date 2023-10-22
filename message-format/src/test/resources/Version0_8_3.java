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
@MessageDef(code = "MSG-05", text = "%{t,empty:'',null:'',:'no'}")
@MessageDef(code = "MSG-06", text = "%{b,bool,true:'yes',false:'no',bool:45}")
@MessageDef(code = "MSG-07", text = "%{n,>0:'pos',<10:'small',<>3467864826:''}")
@MessageDef(code = "MSG-08", text = "%{s,'yes':true,'no':false}")
@MessageDef(code = "MSG-09", text = "%{t,name:true}%[TPL01,a:true,b:'hello',c:-6]")
@TemplateDef(name = "TPL01", text = "%{a} %{b} %{c}")
public class Version0_8_3 {
}
