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
package de.sayayi.lib.message.annotation.adopter.lib;

import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.MessageSupport.ConfigurableMessageSupport;
import de.sayayi.lib.message.MessageSupport.MessagePublisher;
import de.sayayi.lib.message.annotation.MessageDef;
import de.sayayi.lib.message.annotation.TemplateDef;
import de.sayayi.lib.message.annotation.adopter.AbstractAnnotationAdopter;
import de.sayayi.lib.message.annotation.adopter.lib.CommonAnnotationParser.AsmNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.asm.*;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import static java.util.Objects.requireNonNull;
import static org.springframework.asm.ClassReader.*;
import static org.springframework.asm.Opcodes.ACC_SYNTHETIC;


/**
 * Spring ASM-based annotation adopter that scans compiled class files for message and template annotations without
 * loading the classes into the JVM.
 * <p>
 * This adopter recognizes the following annotations (including their repeatable container forms):
 * <ul>
 *   <li>{@link MessageDef} / {@link de.sayayi.lib.message.annotation.MessageDefs} – message definitions</li>
 *   <li>{@link TemplateDef} / {@link de.sayayi.lib.message.annotation.TemplateDefs} – template definitions</li>
 * </ul>
 * Annotations are detected on class-level as well as on non-synthetic methods.
 * <p>
 * Class files are analyzed with the ASM bytecode library bundled with Spring, which means the scanned classes do not
 * need to be on the runtime classpath.
 * <p>
 * In addition to the classpath scanning methods inherited from {@link AbstractAnnotationAdopter}, the
 * {@link #adopt(ResourceLoader, Set)} method accepts a Spring {@link ResourceLoader} for locating classes.
 * <p>
 * If there is a requirement to select a part of the messages provided by a class, the message support must be
 * configured with an appropriate {@link de.sayayi.lib.message.MessageSupport.MessageFilter MessageFilter} or
 * {@link de.sayayi.lib.message.MessageSupport.TemplateFilter TemplateFilter}.
 * <p>
 * In addition to class-file scanning, the {@link #adopt(MessageDef)} and {@link #adopt(TemplateDef)} methods inherited
 * from {@link AbstractAnnotationAdopter} can be used to adopt synthesized or mocked annotations directly.
 * <p>
 * Using this class requires a dependency on the Spring Core library ({@code org.springframework:spring-core}).
 *
 * @author Jeroen Gremmen
 * @since 0.8.0  (refactored in 0.12.0)
 */
public final class SpringAnnotationAdopter extends AbstractAnnotationAdopter
{
  private final CommonAnnotationParser annotationParser = new CommonAnnotationParser(this::adopt, this::adopt);

  /** Highest supported ASM Api version */
  private static final int ASM_API;


  static
  {
    var api = SpringAsmInfo.ASM_VERSION;

    try {
      api = SpringAsmInfo.class.getDeclaredField("ASM_VERSION").getInt(null);
    } catch(ReflectiveOperationException ignored) {
    }

    ASM_API = api;
  }


  /**
   * Create a Spring ASM annotation adopter for the given {@code configurableMessageSupport}. The message factory and
   * message publisher are both obtained from the configurable message support instance.
   *
   * @param configurableMessageSupport  configurable message support, not {@code null}
   */
  public SpringAnnotationAdopter(@NotNull ConfigurableMessageSupport configurableMessageSupport) {
    super(configurableMessageSupport);
  }


  /**
   * Create a Spring ASM annotation adopter for the given {@code messageFactory} and {@code publisher}. This
   * constructor allows the message factory and message publisher to be provided independently.
   *
   * @param messageFactory  message factory used for parsing message format strings, not {@code null}
   * @param publisher       message publisher used for publishing parsed messages and templates, not {@code null}
   */
  public SpringAnnotationAdopter(@NotNull MessageFactory messageFactory, @NotNull MessagePublisher publisher) {
    super(messageFactory, publisher);
  }


  /**
   * Scan the classpath (with the given packages) for message annotations and adopt them. The class loader is obtained
   * from the given Spring {@code resourceLoader}.
   *
   * @param resourceLoader  Spring resource loader providing the class loader for locating classes, not {@code null}
   * @param packageNames    package names to scan, not {@code null}
   *
   * @return  this annotation adopter, never {@code null}
   *
   * @see AbstractAnnotationAdopter#adopt(ClassLoader, Set)
   */
  public @NotNull SpringAnnotationAdopter adopt(@NotNull ResourceLoader resourceLoader,
                                                @NotNull Set<String> packageNames) {
    return (SpringAnnotationAdopter)adopt(requireNonNull(resourceLoader.getClassLoader()), packageNames);
  }


  /**
   * Parses the given class file input stream using the Spring-bundled ASM {@link ClassReader}, visiting class-level
   * and method-level annotations to detect message and template definitions. Only non-synthetic methods are visited.
   *
   * @param classInputStream  input stream of a class file to scan, not {@code null}
   *
   * @throws IOException  if an I/O error occurs while reading the class file
   */
  @Override
  @SuppressWarnings("DuplicatedCode")
  protected void parseClass(@NotNull InputStream classInputStream) throws IOException
  {
    new ClassReader(classInputStream).accept(new ClassVisitor(ASM_API) {
      @Override
      public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                       String[] exceptions) {
        return (access & ACC_SYNTHETIC) == ACC_SYNTHETIC ? null : new MessageMethodVisitor();
      }


      @Override
      public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        return wrap(annotationParser.visitAnnotation(descriptor));
      }
    }, SKIP_CODE | SKIP_DEBUG | SKIP_FRAMES);
  }


  @SuppressWarnings("DuplicatedCode")
  private @Nullable AnnotationVisitor wrap(@Nullable AsmNode asmNode)
  {
    if (asmNode == null)
      return null;

    return new AnnotationVisitor(ASM_API) {
      @Override
      public void visit(String name, Object value) {
        asmNode.visit(name, value);
      }


      @Override
      public AnnotationVisitor visitArray(String name) {
        return wrap(asmNode.visitArray(name));
      }


      @Override
      public AnnotationVisitor visitAnnotation(String name, String descriptor) {
        return wrap(asmNode.visitAnnotation(name, descriptor));
      }


      @Override
      public void visitEnd() {
        asmNode.visitEnd();
      }
    };
  }




  private final class MessageMethodVisitor extends MethodVisitor
  {
    private MessageMethodVisitor() {
      super(ASM_API);
    }


    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
      return wrap(annotationParser.visitAnnotation(descriptor));
    }
  }
}
