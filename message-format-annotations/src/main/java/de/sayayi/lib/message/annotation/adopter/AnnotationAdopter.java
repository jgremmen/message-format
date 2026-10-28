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
 *   <li>{@link MessageDef} / {@link MessageDefs}: message definitions</li>
 *   <li>{@link TemplateDef} / {@link TemplateDefs}: template definitions</li>
 * </ul>
 * Annotations are detected on class-level as well as on non-synthetic methods.
 * <p>
 * Class files are analyzed with the ASM bytecode library, which is relocated and bundled into this module's
 * artifact (rather than being an external dependency), which means the scanned classes do not need to be on
 * the runtime classpath.
 * <p>
 * If there is a requirement to select a part of the messages provided by a class, the message support must be
 * configured with an appropriate {@link de.sayayi.lib.message.MessageSupport.MessageFilter MessageFilter} or
 * {@link de.sayayi.lib.message.MessageSupport.TemplateFilter TemplateFilter}.
 * <p>
 * In addition to class-file scanning, the {@link #adopt(MessageDef)} and {@link #adopt(TemplateDef)} methods
 * inherited from {@link AbstractAnnotationAdopter} can be used to adopt synthesized or mocked annotations directly.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public final class AnnotationAdopter extends AbstractAnnotationAdopter
{
  /** Type descriptor of the {@link MessageDefs} annotation. */
  private static final String MESSAGE_DEFS_DESCRIPTOR = getDescriptor(MessageDefs.class);

  /** Type descriptor of the {@link MessageDef} annotation. */
  private static final String MESSAGE_DEF_DESCRIPTOR = getDescriptor(MessageDef.class);

  /** Type descriptor of the {@link TemplateDefs} annotation. */
  private static final String TEMPLATE_DEFS_DESCRIPTOR = getDescriptor(TemplateDefs.class);

  /** Type descriptor of the {@link TemplateDef} annotation. */
  private static final String TEMPLATE_DEF_DESCRIPTOR = getDescriptor(TemplateDef.class);

  /** Type descriptor of the {@link Text} annotation. */
  private static final String TEXT_DESCRIPTOR = getDescriptor(Text.class);


  /**
   * Creates an ASM annotation adopter for the given {@code configurableMessageSupport}. The message factory and
   * message publisher are both obtained from the configurable message support instance.
   *
   * @param configurableMessageSupport  configurable message support, not {@code null}
   */
  public AnnotationAdopter(@NotNull ConfigurableMessageSupport configurableMessageSupport) {
    super(configurableMessageSupport);
  }


  /**
   * Creates an ASM annotation adopter for the given {@code messageFactory} and {@code publisher}. This constructor
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
   *
   * @since 0.24.0
   */
  private final class MainClassVisitor extends ClassVisitor
  {
    /** Creates a new class visitor. */
    private MainClassVisitor() {
      super(ASM9);
    }


    /**
     * {@inheritDoc}
     * <p>
     * Returns a visitor for the annotations of non-synthetic methods.
     */
    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                     String[] exceptions) {
      return (access & ACC_SYNTHETIC) == ACC_SYNTHETIC ? null : new MessageMethodVisitor();
    }


    /**
     * {@inheritDoc}
     * <p>
     * Returns a visitor for recognized message and template annotations.
     */
    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
      return AnnotationAdopter.this.visitAnnotation(descriptor);
    }
  }




  /**
   * Returns an appropriate annotation visitor for the given annotation descriptor, or {@code null} if the descriptor
   * does not match a recognized message or template annotation.
   *
   * @param descriptor  annotation type descriptor
   *
   * @return  annotation visitor, or {@code null} if the annotation is not recognized
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
   *
   * @since 0.24.0
   */
  private final class MessageMethodVisitor extends MethodVisitor
  {
    /** Creates a new method visitor. */
    private MessageMethodVisitor() {
      super(ASM9);
    }


    /**
     * {@inheritDoc}
     * <p>
     * Returns a visitor for recognized message and template annotations.
     */
    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
      return AnnotationAdopter.this.visitAnnotation(descriptor);
    }
  }




  /**
   * Visitor for the {@link MessageDefs} repeatable container annotation. Delegates each contained {@link MessageDef} to
   * a {@link MessageDefAnnotationVisitor}.
   *
   * @since 0.24.0
   */
  private final class MessageDefsAnnotationVisitor extends AnnotationVisitor
  {
    /** Creates a new visitor for a {@link MessageDefs} annotation. */
    private MessageDefsAnnotationVisitor() {
      super(ASM9);
    }


    /** {@inheritDoc} */
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
   * Visitor for a single {@link MessageDef} annotation. Collects the message code, text and localized {@link Text}
   * entries, then adopts the resulting message definition.
   *
   * @since 0.24.0
   */
  private final class MessageDefAnnotationVisitor extends AnnotationVisitor
  {
    /** Message code collected from the annotation. */
    private String code;

    /** Default text collected from the annotation. */
    private String text;

    /** Localized texts collected from the annotation. */
    private final List<Text> texts = new ArrayList<>();


    /** Creates a new visitor for a {@link MessageDef} annotation. */
    private MessageDefAnnotationVisitor() {
      super(ASM9);
    }


    /** {@inheritDoc} */
    @Override
    public void visit(String name, Object value)
    {
      if ("code".equals(name))
        code = (String)value;
      else if ("text".equals(name))
        text = (String)value;
    }


    /** {@inheritDoc} */
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


    /**
     * {@inheritDoc}
     * <p>
     * Adopts or collects the definition gathered by this visitor.
     */
    @Override
    public void visitEnd() {
      adopt(new SyntheticMessageDef(code, text, texts.toArray(Text[]::new)));
    }
  }




  /**
   * Visitor for the {@link TemplateDefs} repeatable container annotation. Delegates each contained {@link TemplateDef}
   * to a {@link TemplateDefAnnotationVisitor}.
   *
   * @since 0.24.0
   */
  private final class TemplateDefsAnnotationVisitor extends AnnotationVisitor
  {
    /** Creates a new visitor for a {@link TemplateDefs} annotation. */
    private TemplateDefsAnnotationVisitor() {
      super(ASM9);
    }


    /** {@inheritDoc} */
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
   * Visitor for a single {@link TemplateDef} annotation. Collects the template name, text and localized {@link Text}
   * entries, then adopts the resulting template definition.
   *
   * @since 0.24.0
   */
  private final class TemplateDefAnnotationVisitor extends AnnotationVisitor
  {
    /** Template name collected from the annotation. */
    private String name;

    /** Default text collected from the annotation. */
    private String text;

    /** Localized texts collected from the annotation. */
    private final List<Text> texts = new ArrayList<>();


    /** Creates a new visitor for a {@link TemplateDef} annotation. */
    private TemplateDefAnnotationVisitor() {
      super(ASM9);
    }


    /** {@inheritDoc} */
    @Override
    public void visit(String name, Object value)
    {
      if ("name".equals(name))
        this.name = (String)value;
      else if ("text".equals(name))
        text = (String)value;
    }


    /** {@inheritDoc} */
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


    /**
     * {@inheritDoc}
     * <p>
     * Adopts or collects the definition gathered by this visitor.
     */
    @Override
    public void visitEnd() {
      adopt(new SyntheticTemplateDef(name, text, texts.toArray(Text[]::new)));
    }
  }




  /**
   * Visitor for a single {@link Text} annotation. Collects the locale, text and value attributes and adds the resulting
   * synthetic text to a shared list.
   *
   * @since 0.24.0
   */
  private static final class TextAnnotationVisitor extends AnnotationVisitor
  {
    /** List receiving the text collected from the annotation. */
    private final List<Text> inheritedTexts;

    /** Locale collected from the annotation. */
    private String locale;

    /** Localized text collected from the annotation. */
    private String text;

    /** Value collected from the annotation. */
    private String value;


    /**
     * Creates a new visitor for a {@link Text} annotation.
     *
     * @param inheritedTexts  list receiving the collected text, not {@code null}
     */
    private TextAnnotationVisitor(@NotNull List<Text> inheritedTexts)
    {
      super(ASM9);

      this.inheritedTexts = inheritedTexts;
    }


    /** {@inheritDoc} */
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


    /**
     * {@inheritDoc}
     * <p>
     * Adopts or collects the definition gathered by this visitor.
     */
    @Override
    public void visitEnd() {
      inheritedTexts.add(new SyntheticText(locale, text, value));
    }
  }
}
