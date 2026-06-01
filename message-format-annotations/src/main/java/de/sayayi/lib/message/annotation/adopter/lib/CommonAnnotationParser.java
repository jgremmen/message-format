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
package de.sayayi.lib.message.annotation.adopter.lib;

import de.sayayi.lib.message.annotation.*;
import de.sayayi.lib.message.annotation.adopter.util.SyntheticMessageDef;
import de.sayayi.lib.message.annotation.adopter.util.SyntheticTemplateDef;
import de.sayayi.lib.message.annotation.adopter.util.SyntheticText;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;


/**
 * Parses message and template annotation structures from bytecode data, independent of a specific ASM library variant.
 * <p>
 * This class is used internally by the concrete annotation adopter implementations ({@link AsmAnnotationAdopter},
 * {@link ByteBuddyAnnotationAdopter}, {@link SpringAnnotationAdopter}) to extract {@link MessageDef} and
 * {@link TemplateDef} annotation data from class files. The parsed data is delivered to the adopter through
 * {@link Consumer} callbacks.
 *
 * @author Jeroen Gremmen
 * @since 0.24.0
 */
final class CommonAnnotationParser
{
  private static final String MESSAGE_DEFS_DESCRIPTOR = MessageDefs.class.descriptorString();
  private static final String MESSAGE_DEF_DESCRIPTOR = MessageDef.class.descriptorString();
  private static final String TEMPLATE_DEFS_DESCRIPTOR = TemplateDefs.class.descriptorString();
  private static final String TEMPLATE_DEF_DESCRIPTOR = TemplateDef.class.descriptorString();
  private static final String TEXT_DESCRIPTOR = Text.class.descriptorString();

  private final Consumer<MessageDef> messageDefConsumer;
  private final Consumer<TemplateDef> templateDefConsumer;


  /**
   * Creates a new annotation parser that delivers parsed message and template definitions to the given consumers.
   *
   * @param messageDefConsumer   consumer for parsed message definitions, not {@code null}
   * @param templateDefConsumer  consumer for parsed template definitions, not {@code null}
   */
  CommonAnnotationParser(@NotNull Consumer<MessageDef> messageDefConsumer,
                         @NotNull Consumer<TemplateDef> templateDefConsumer)
  {
    this.messageDefConsumer = messageDefConsumer;
    this.templateDefConsumer = templateDefConsumer;
  }


  /**
   * Returns an {@link AsmNode} for the given annotation descriptor, or {@code null} if the descriptor does not
   * match a known message or template annotation.
   *
   * @param descriptor  annotation type descriptor, not {@code null}
   *
   * @return  an ASM node for the annotation, or {@code null} if the descriptor is not recognized
   */
  @Nullable AsmNode visitAnnotation(@NotNull String descriptor)
  {
    if (MESSAGE_DEF_DESCRIPTOR.equals(descriptor))
      return new MessageDefAsmNode();
    if (MESSAGE_DEFS_DESCRIPTOR.equals(descriptor))
      return new MessageDefsAsmNode();
    if (TEMPLATE_DEF_DESCRIPTOR.equals(descriptor))
      return new TemplateDefAsmNode();
    if (TEMPLATE_DEFS_DESCRIPTOR.equals(descriptor))
      return new TemplateDefsAsmNode();

    return null;
  }


  @Contract(pure = true)
  private static @NotNull String notNull(@Nullable String s) {
    return s == null ? "" : s;
  }


  @Contract(pure = true)
  private static @NotNull Text[] toTexts(@NotNull List<ParsedText> texts)
  {
    return texts
        .stream()
        .map(text -> new SyntheticText(text.locale(), text.text(), text.value()))
        .toArray(Text[]::new);
  }




  /**
   * Callback interface that models annotation visitor operations. Concrete implementations build up annotation data
   * as the bytecode visitor traverses annotation structures.
   */
  interface AsmNode
  {
    default void visit(@Nullable String name, @Nullable Object value) {
    }


    default @Nullable AsmNode visitArray(@Nullable String name) {
      return null;
    }


    default @Nullable AsmNode visitAnnotation(@Nullable String name, @NotNull String descriptor) {
      return null;
    }


    default void visitEnd() {
    }
  }




  /**
   * Holds the parsed values of a single {@link Text} annotation.
   *
   * @param locale  locale tag, not {@code null}
   * @param text    localized text, not {@code null}
   * @param value   shorthand text value, not {@code null}
   */
  record ParsedText(@NotNull String locale, @NotNull String text, @NotNull String value) {
  }




  private final class MessageDefsAsmNode implements AsmNode
  {
    @Override
    public @NotNull AsmNode visitArray(@Nullable String name) {
      return new MessageDefsArrayAsmNode();
    }


    @Override
    public @Nullable AsmNode visitAnnotation(@Nullable String name, @NotNull String descriptor) {
      return MESSAGE_DEF_DESCRIPTOR.equals(descriptor) ? new MessageDefAsmNode() : null;
    }
  }




  private final class MessageDefsArrayAsmNode implements AsmNode
  {
    @Override
    public @Nullable AsmNode visitAnnotation(@Nullable String name, @NotNull String descriptor) {
      return MESSAGE_DEF_DESCRIPTOR.equals(descriptor) ? new MessageDefAsmNode() : null;
    }
  }




  private final class MessageDefAsmNode implements AsmNode
  {
    private String code;
    private String text;
    private final List<ParsedText> texts = new ArrayList<>();


    @Override
    public void visit(@Nullable String name, @Nullable Object value)
    {
      if ("code".equals(name))
        code = (String)value;
      else if ("text".equals(name))
        text = (String)value;
    }


    @Override
    public @NotNull AsmNode visitArray(@Nullable String name) {
      return new TextArrayAsmNode(texts);
    }


    @Override
    public void visitEnd() {
      messageDefConsumer.accept(new SyntheticMessageDef(notNull(code), notNull(text), toTexts(texts)));
    }
  }




  private final class TemplateDefsAsmNode implements AsmNode
  {
    @Override
    public @NotNull AsmNode visitArray(@Nullable String name) {
      return new TemplateDefsArrayAsmNode();
    }


    @Override
    public @Nullable AsmNode visitAnnotation(@Nullable String name, @NotNull String descriptor) {
      return TEMPLATE_DEF_DESCRIPTOR.equals(descriptor) ? new TemplateDefAsmNode() : null;
    }
  }




  private final class TemplateDefsArrayAsmNode implements AsmNode
  {
    @Override
    public @Nullable AsmNode visitAnnotation(@Nullable String name, @NotNull String descriptor) {
      return TEMPLATE_DEF_DESCRIPTOR.equals(descriptor) ? new TemplateDefAsmNode() : null;
    }
  }




  private final class TemplateDefAsmNode implements AsmNode
  {
    private String name;
    private String text;
    private final List<ParsedText> texts = new ArrayList<>();


    @Override
    public void visit(@Nullable String name, @Nullable Object value)
    {
      if ("name".equals(name))
        this.name = (String)value;
      else if ("text".equals(name))
        text = (String)value;
    }


    @Override
    public @NotNull AsmNode visitArray(@Nullable String name) {
      return new TextArrayAsmNode(texts);
    }


    @Override
    public void visitEnd() {
      templateDefConsumer.accept(new SyntheticTemplateDef(notNull(name), notNull(text), toTexts(texts)));
    }
  }




  private record TextArrayAsmNode(@NotNull List<ParsedText> texts) implements AsmNode
  {
    @Override
    public @Nullable CommonAnnotationParser.AsmNode visitAnnotation(@Nullable String name, @NotNull String descriptor) {
      return TEXT_DESCRIPTOR.equals(descriptor) ? new TextAsmNode(texts) : null;
    }
  }




  private static final class TextAsmNode implements AsmNode
  {
    private final List<ParsedText> inheritedTexts;
    private String locale;
    private String text;
    private String value;


    private TextAsmNode(@NotNull List<ParsedText> inheritedTexts) {
      this.inheritedTexts = inheritedTexts;
    }


    @Override
    public void visit(@Nullable String name, @Nullable Object value)
    {
      if ("locale".equals(name))
        locale = (String)value;
      else if ("text".equals(name))
        text = (String)value;
      else if ("value".equals(name))
        this.value = (String)value;
    }


    @Override
    public void visitEnd() {
      inheritedTexts.add(new ParsedText(notNull(locale), notNull(text), notNull(value)));
    }
  }
}
