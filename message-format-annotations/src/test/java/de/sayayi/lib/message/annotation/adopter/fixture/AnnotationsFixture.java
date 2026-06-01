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
package de.sayayi.lib.message.annotation.adopter.fixture;

import de.sayayi.lib.message.annotation.MessageDef;
import de.sayayi.lib.message.annotation.TemplateDef;
import de.sayayi.lib.message.annotation.Text;


/**
 * Annotation fixture class used by {@code AnnotationAdopterTest} to verify that all annotation forms are
 * correctly adopted for all 3 adopter implementations.
 *
 * <p>This class contains @MessageDef, @MessageDefs, @TemplateDef and @TemplateDefs on both
 * type-level and method-level targets, covering:
 * <ul>
 *   <li>plain {@code text=} form (single text, no @Text)</li>
 *   <li>{@code texts=@Text("value")} form (single @Text via {@code value} attribute)</li>
 *   <li>{@code texts={@Text(locale=..., text=...), ...}} form (multi-locale)</li>
 * </ul>
 *
 * @author Jeroen Gremmen
 * @since 0.24.0
 */
// Type-level: 3 @MessageDef → stored as @MessageDefs container in bytecode
@MessageDef(code = "type-msg-1", text = "Type message 1")
@MessageDef(code = "type-msg-2", texts = @Text("Type message 2"))
@MessageDef(code = "type-msg-3", texts = {
    @Text(locale = "en", text = "EN type msg 3"),
    @Text(locale = "de", text = "DE type msg 3"),
    @Text(locale = "fr", text = "FR type msg 3")
})
// Type-level: 3 @TemplateDef → stored as @TemplateDefs container in bytecode
@TemplateDef(name = "type-tmpl-1", text = "Type template 1")
@TemplateDef(name = "type-tmpl-2", texts = @Text("Type template 2"))
@TemplateDef(name = "type-tmpl-3", texts = {
    @Text(locale = "en", text = "EN type tmpl 3"),
    @Text(locale = "de", text = "DE type tmpl 3")
})
@SuppressWarnings("unused")
public final class AnnotationsFixture
{
  // 2 @MessageDef → stored as @MessageDefs container in bytecode
  @MessageDef(code = "method-msg-1", text = "Method message 1")
  @MessageDef(code = "method-msg-2", texts = @Text("Method message 2"))
  public void methodA() {}


  // 1 @MessageDef (multi-locale) → stored as standalone @MessageDef in bytecode
  // 1 @TemplateDef → stored as standalone @TemplateDef in bytecode
  @MessageDef(code = "method-msg-3", texts = {
      @Text(locale = "en", text = "EN method msg 3"),
      @Text(locale = "de", text = "DE method msg 3")
  })
  @TemplateDef(name = "method-tmpl-1", text = "Method template 1")
  public void methodB() {}


  // 2 @TemplateDef → stored as @TemplateDefs container in bytecode
  @TemplateDef(name = "method-tmpl-2", texts = @Text("Method template 2"))
  @TemplateDef(name = "method-tmpl-3", texts = {
      @Text(locale = "en", text = "EN method tmpl 3"),
      @Text(locale = "fr", text = "FR method tmpl 3")
  })
  public void methodC() {}
}
