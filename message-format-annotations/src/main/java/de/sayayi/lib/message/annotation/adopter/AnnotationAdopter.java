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
package de.sayayi.lib.message.annotation.adopter;

import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.MessageSupport.ConfigurableMessageSupport;
import de.sayayi.lib.message.MessageSupport.MessagePublisher;
import de.sayayi.lib.message.annotation.*;
import de.sayayi.lib.message.annotation.adopter.util.SyntheticMessageDef;
import de.sayayi.lib.message.annotation.adopter.util.SyntheticTemplateDef;
import de.sayayi.lib.message.annotation.adopter.util.SyntheticText;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.objectweb.asm.ClassReader.*;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.ASM9;
import static org.objectweb.asm.Type.getDescriptor;


/**
 * ASM-based annotation adopter that scans compiled class files for message and template annotations without
 * loading the classes into the JVM.
 * <p>
 * This adopter recognizes the following annotations (including their repeatable container forms):
 * <ul>
 *   <li>{@link MessageDef} / {@link MessageDefs} – message definitions</li>
 *   <li>{@link TemplateDef} / {@link TemplateDefs} – template definitions</li>
 * </ul>
 * Annotations are detected on class-level as well as on non-synthetic methods.
 * <p>
 * Class files are analyzed with the ASM bytecode library, which means the scanned classes do not need to be on
 * the runtime classpath.
 * <p>
 * If there is a requirement to select a part of the messages provided by a class, the message support must be
 * configured with an appropriate {@link de.sayayi.lib.message.MessageSupport.MessageFilter MessageFilter} or
 * {@link de.sayayi.lib.message.MessageSupport.TemplateFilter TemplateFilter}.
 * <p>
 * In addition to class-file scanning, the {@link #adopt(MessageDef)} and {@link #adopt(TemplateDef)} methods
 * inherited from {@link AbstractAnnotationAdopter} can be used to adopt synthesized or mocked annotations directly.
 * <p>
 * Using this class requires a dependency on the ASM library ({@code org.ow2.asm:asm:9.+}).
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class AnnotationAdopter extends AbstractAnnotationAdopter
{
  private static final String MESSAGE_DEFS_DESCRIPTOR = getDescriptor(MessageDefs.class);
  private static final String MESSAGE_DEF_DESCRIPTOR = getDescriptor(MessageDef.class);
  private static final String TEMPLATE_DEFS_DESCRIPTOR = getDescriptor(TemplateDefs.class);
  private static final String TEMPLATE_DEF_DESCRIPTOR = getDescriptor(TemplateDef.class);
  private static final String TEXT_DESCRIPTOR = getDescriptor(Text.class);


  /**
   * Create an ASM annotation adopter for the given {@code configurableMessageSupport}. The message factory and
   * message publisher are both obtained from the configurable message support instance.
   *
   * @param configurableMessageSupport  configurable message support, not {@code null}
   */
  public AnnotationAdopter(@NotNull ConfigurableMessageSupport configurableMessageSupport) {
    super(configurableMessageSupport);
  }


  /**
   * Create an ASM annotation adopter for the given {@code messageFactory} and {@code publisher}. This constructor
   * allows the message factory and message publisher to be provided independently.
   *
   * @param messageFactory  message factory used for parsing message format strings, not {@code null}
   * @param publisher       message publisher used for publishing parsed messages and templates, not {@code null}
   */
  public AnnotationAdopter(@NotNull MessageFactory messageFactory, @NotNull MessagePublisher publisher) {
    super(messageFactory, publisher);
  }


  /**
   * Parses the given class file input stream using the ASM {@link ClassReader}, visiting class-level and method-level
   * annotations to detect message and template definitions. Only non-synthetic methods are visited.
   *
   * @param classInputStream  input stream of a class file to scan, not {@code null}
   *
   * @throws IOException  if an I/O error occurs while reading the class file
   */
  @Override
  protected void parseClass(@NotNull InputStream classInputStream) throws IOException {
    new ClassReader(classInputStream).accept(new MainClassVisitor(), SKIP_CODE | SKIP_DEBUG | SKIP_FRAMES);
  }




  /**
   * ASM class visitor that delegates class-level and method-level annotation visits to the enclosing adopter.
   * Synthetic methods are skipped.
   */
  private final class MainClassVisitor extends ClassVisitor
  {
    private MainClassVisitor() {
      super(ASM9);
    }


    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                     String[] exceptions) {
      return (access & ACC_SYNTHETIC) == ACC_SYNTHETIC ? null : new MessageMethodVisitor();
    }


    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
      return AnnotationAdopter.this.visitAnnotation(descriptor);
    }
  }




  /**
   * Returns an appropriate annotation visitor for the given annotation descriptor, or {@code null} if the
   * descriptor does not match a recognized message or template annotation.
   */
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




  /**
   * ASM method visitor that delegates method-level annotation visits to the enclosing adopter.
   */
  private final class MessageMethodVisitor extends MethodVisitor
  {
    private MessageMethodVisitor() {
      super(ASM9);
    }


    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
      return AnnotationAdopter.this.visitAnnotation(descriptor);
    }
  }




  /**
   * Visitor for the {@link MessageDefs} repeatable container annotation. Delegates each contained
   * {@link MessageDef} to a {@link MessageDefAnnotationVisitor}.
   */
  private final class MessageDefsAnnotationVisitor extends AnnotationVisitor
  {
    private MessageDefsAnnotationVisitor() {
      super(ASM9);
    }


    @Override
    public AnnotationVisitor visitArray(String name)
    {
      return new AnnotationVisitor(ASM9) {
        @Override
        public AnnotationVisitor visitAnnotation(String name, String descriptor) {
          return MESSAGE_DEF_DESCRIPTOR.equals(descriptor) ? new MessageDefAnnotationVisitor() : null;
        }
      };
    }
  }




  /**
   * Visitor for a single {@link MessageDef} annotation. Collects the message code, text and localized
   * {@link Text} entries, then adopts the resulting message definition.
   */
  private final class MessageDefAnnotationVisitor extends AnnotationVisitor
  {
    private String code;
    private String text;
    private final List<Text> texts = new ArrayList<>();


    private MessageDefAnnotationVisitor() {
      super(ASM9);
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
      return new AnnotationVisitor(ASM9) {
        @Override
        public AnnotationVisitor visitAnnotation(String name, String descriptor) {
          return TEXT_DESCRIPTOR.equals(descriptor) ? new TextAnnotationVisitor(texts) : null;
        }
      };
    }


    @Override
    public void visitEnd() {
      adopt(new SyntheticMessageDef(code, text, texts.toArray(Text[]::new)));
    }
  }




  /**
   * Visitor for the {@link TemplateDefs} repeatable container annotation. Delegates each contained
   * {@link TemplateDef} to a {@link TemplateDefAnnotationVisitor}.
   */
  private final class TemplateDefsAnnotationVisitor extends AnnotationVisitor
  {
    private TemplateDefsAnnotationVisitor() {
      super(ASM9);
    }


    @Override
    public AnnotationVisitor visitArray(String name)
    {
      return new AnnotationVisitor(ASM9) {
        @Override
        public AnnotationVisitor visitAnnotation(String name, String descriptor) {
          return TEMPLATE_DEF_DESCRIPTOR.equals(descriptor) ? new TemplateDefAnnotationVisitor() : null;
        }
      };
    }
  }




  /**
   * Visitor for a single {@link TemplateDef} annotation. Collects the template name, text and localized
   * {@link Text} entries, then adopts the resulting template definition.
   */
  private final class TemplateDefAnnotationVisitor extends AnnotationVisitor
  {
    private String name;
    private String text;
    private final List<Text> texts = new ArrayList<>();


    private TemplateDefAnnotationVisitor() {
      super(ASM9);
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
      return new AnnotationVisitor(ASM9) {
        @Override
        public AnnotationVisitor visitAnnotation(String name, String descriptor) {
          return TEXT_DESCRIPTOR.equals(descriptor) ? new TextAnnotationVisitor(texts) : null;
        }
      };
    }


    @Override
    public void visitEnd() {
      adopt(new SyntheticTemplateDef(name, text, texts.toArray(Text[]::new)));
    }
  }




  /**
   * Visitor for a single {@link Text} annotation. Collects the locale, text and value attributes
   * and adds the resulting synthetic text to a shared list.
   */
  private static final class TextAnnotationVisitor extends AnnotationVisitor
  {
    private final List<Text> inheritedTexts;
    private String locale;
    private String text;
    private String value;


    private TextAnnotationVisitor(List<Text> inheritedTexts)
    {
      super(ASM9);
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
      inheritedTexts.add(new SyntheticText(locale, text, value));
    }
  }
}
