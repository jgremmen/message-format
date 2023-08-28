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
package de.sayayi.lib.message.adopter;

import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.MessageSupport.ConfigurableMessageSupport;
import de.sayayi.lib.message.MessageSupport.MessagePublisher;
import de.sayayi.lib.message.annotation.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.asm.AnnotationVisitor;
import org.springframework.asm.ClassReader;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.MethodVisitor;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.util.Objects.requireNonNull;
import static org.springframework.asm.Opcodes.ACC_SYNTHETIC;
import static org.springframework.asm.SpringAsmInfo.ASM_VERSION;
import static org.springframework.asm.Type.getDescriptor;


/**
 * This class defines various methods for adopting messages and templates defined by annotations.
 * <p>
 * Messages are analysed per class (see {@link #parseClass(InputStream)}). If there is a
 * requirement to select a part of the messages provided by a class, the message support must
 * be configured with an appropriate
 * {@link de.sayayi.lib.message.MessageSupport.MessageFilter MessageFilter} or
 * {@link de.sayayi.lib.message.MessageSupport.TemplateFilter TemplateFilter}.
 * <p>
 * Even though the annotations all have class retention, 2 adopt methods
 * ({@link #adopt(MessageDef)} and {@link #adopt(TemplateDef)}) are available for analyzing
 * synthesized/mocked annotations.
 * <p>
 * The scanned classes are analysed using the ASM library bundled with Spring. Using this class
 * therefore requires a dependency with library {@code org.springframework:spring-core:5.3.+}.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
@SuppressWarnings("unused")
public final class SpringAsmAnnotationAdopter extends AbstractAnnotationAdopter
{
  private static final String MESSAGE_DEFS_DESCRIPTOR = getDescriptor(MessageDefs.class);
  private static final String MESSAGE_DEF_DESCRIPTOR = getDescriptor(MessageDef.class);
  private static final String TEMPLATE_DEFS_DESCRIPTOR = getDescriptor(TemplateDefs.class);
  private static final String TEMPLATE_DEF_DESCRIPTOR = getDescriptor(TemplateDef.class);
  private static final String TEXT_DESCRIPTOR = getDescriptor(Text.class);


  /**
   * Create an annotation adopter for the given {@code configurableMessageSupport}.
   *
   * @param configurableMessageSupport  configurable message support, not {@code null}
   */
  public SpringAsmAnnotationAdopter(
      @NotNull ConfigurableMessageSupport configurableMessageSupport) {
    super(configurableMessageSupport);
  }


  /**
   * Create an annotation adopter for the given {@code messageFactory} and {@code publisher}.
   *
   * @param messageFactory  message factory, not {@code null}
   * @param publisher       message publisher, not {@code null}
   */
  public SpringAsmAnnotationAdopter(@NotNull MessageFactory messageFactory,
                                    @NotNull MessagePublisher publisher) {
    super(messageFactory, publisher);
  }


  /**
   * Scan the classpath (with the given packages) for message annotations and adopt them.
   *
   * @param resourceLoader  Spring resource loader for locating classes, not {@code null}
   * @param packageNames    package names to scan, not {@code null}
   *
   * @return  this annotation adopter, never {@code null}
   */
  public @NotNull SpringAsmAnnotationAdopter adopt(@NotNull ResourceLoader resourceLoader,
                                                   @NotNull Set<String> packageNames)
  {
    return (SpringAsmAnnotationAdopter)
        adopt(requireNonNull(resourceLoader.getClassLoader()), packageNames);
  }


  @Override
  protected void parseClass(@NotNull InputStream classInputStream) throws IOException {
    new ClassReader(classInputStream).accept(new MainClassVisitor(), 0);
  }




  @SuppressWarnings("DuplicatedCode")
  private final class MainClassVisitor extends ClassVisitor
  {
    private MainClassVisitor() {
      super(ASM_VERSION);
    }


    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                     String[] exceptions) {
      return (access & ACC_SYNTHETIC) == ACC_SYNTHETIC ? null : new MessageMethodVisitor();
    }


    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
      return SpringAsmAnnotationAdopter.this.visitAnnotation(descriptor);
    }
  }




  @SuppressWarnings("DuplicatedCode")
  private AnnotationVisitor visitAnnotation(String descriptor)
  {
    if (MESSAGE_DEF_DESCRIPTOR.equals(descriptor))
      return new MessageDefAnnotationVisitor();
    if (MESSAGE_DEFS_DESCRIPTOR.equals(descriptor))
      return new MessageDefsAnnotationVisitor();
    if (TEMPLATE_DEF_DESCRIPTOR.equals(descriptor))
      return new TemplateDefAnnotationVisitor();
    if (TEMPLATE_DEFS_DESCRIPTOR.equals(descriptor))
      return new TemplateDefsAnnotationVisitor();

    return null;
  }




  @SuppressWarnings("DuplicatedCode")
  private final class MessageMethodVisitor extends MethodVisitor
  {
    private MessageMethodVisitor() {
      super(ASM_VERSION);
    }


    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
      return SpringAsmAnnotationAdopter.this.visitAnnotation(descriptor);
    }
  }




  private final class MessageDefsAnnotationVisitor extends AnnotationVisitor
  {
    private MessageDefsAnnotationVisitor() {
      super(ASM_VERSION);
    }


    @Override
    public AnnotationVisitor visitArray(String name)
    {
      return new AnnotationVisitor(ASM_VERSION) {
        @Override
        public AnnotationVisitor visitAnnotation(String name, String descriptor)
        {
          return MESSAGE_DEF_DESCRIPTOR.equals(descriptor)
              ? new MessageDefAnnotationVisitor() : null;
        }
      };
    }
  }




  private final class MessageDefAnnotationVisitor extends AnnotationVisitor
  {
    private String code;
    private String text;
    private final List<Text> texts = new ArrayList<>();


    private MessageDefAnnotationVisitor() {
      super(ASM_VERSION);
    }


    @Override
    public void visit(String name, Object value)
    {
      if ("code".equals(name))
        code = (String)value;
      else if ("text".equals(name))
        text = (String)value;
    }


    @Override
    public AnnotationVisitor visitArray(String name)
    {
      return new AnnotationVisitor(ASM_VERSION) {
        @Override
        public AnnotationVisitor visitAnnotation(String name, String descriptor) {
          return TEXT_DESCRIPTOR.equals(descriptor) ? new TextAnnotationVisitor(texts) : null;
        }
      };
    }


    @Override
    public void visitEnd() {
      adopt(new MessageDefImpl(code, text, texts.toArray(new Text[0])));
    }
  }




  private final class TemplateDefsAnnotationVisitor extends AnnotationVisitor
  {
    private TemplateDefsAnnotationVisitor() {
      super(ASM_VERSION);
    }


    @Override
    public AnnotationVisitor visitArray(String name)
    {
      return new AnnotationVisitor(ASM_VERSION) {
        @Override
        public AnnotationVisitor visitAnnotation(String name, String descriptor)
        {
          return TEMPLATE_DEF_DESCRIPTOR.equals(descriptor)
              ? new TemplateDefAnnotationVisitor() : null;
        }
      };
    }
  }




  private final class TemplateDefAnnotationVisitor extends AnnotationVisitor
  {
    private String name;
    private String text;
    private final List<Text> texts = new ArrayList<>();


    private TemplateDefAnnotationVisitor() {
      super(ASM_VERSION);
    }


    @Override
    public void visit(String name, Object value)
    {
      if ("name".equals(name))
        this.name = (String)value;
      else if ("text".equals(name))
        text = (String)value;
    }


    @Override
    public AnnotationVisitor visitArray(String name)
    {
      return new AnnotationVisitor(ASM_VERSION) {
        @Override
        public AnnotationVisitor visitAnnotation(String name, String descriptor) {
          return TEXT_DESCRIPTOR.equals(descriptor) ? new TextAnnotationVisitor(texts) : null;
        }
      };
    }


    @Override
    public void visitEnd() {
      adopt(new TemplateDefImpl(name, text, texts.toArray(new Text[0])));
    }
  }




  private static final class TextAnnotationVisitor extends AnnotationVisitor
  {
    private final List<Text> inheritedTexts;
    private String locale;
    private String text;
    private String value;


    private TextAnnotationVisitor(List<Text> inheritedTexts)
    {
      super(ASM_VERSION);
      this.inheritedTexts = inheritedTexts;
    }


    @Override
    public void visit(String name, Object value)
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
      inheritedTexts.add(new TextImpl(locale, text, value));
    }
  }
}
