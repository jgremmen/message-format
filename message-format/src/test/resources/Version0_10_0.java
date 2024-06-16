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
@MessageDef(code = "MSG-09", text = "%{t,name:true}%['TPL01',a:true,b:'hello',c:-6]")
@MessageDef(code = "MSG-10", text = "%{x,choice,(1,21,41,61):'A',(true,>80):'B'}")
@TemplateDef(name = "TPL01", text = "%{a} %{true} %{'c4'}")
public class Version0_10_1 {
}
