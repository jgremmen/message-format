/*
 * Copyright 2021 Jeroen Gremmen
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
import de.sayayi.lib.message.annotation.impl.MessageDefImpl;
import de.sayayi.lib.message.annotation.impl.TemplateDefImpl;
import de.sayayi.lib.message.annotation.impl.TextImpl;
import de.sayayi.lib.message.exception.MessageAdopterException;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import static java.lang.ClassLoader.getSystemClassLoader;
import static java.util.Arrays.asList;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.ASM9;
import static org.objectweb.asm.Type.getDescriptor;


/**
 * <p>
 *   The asm classpath scanner scans classes and publishes the annotated messages found.
 * </p>
 * <p>
 *   The scanned classes are not loaded by the classloader but instead are analysed using the ASM library.
 *   Using this class therefore requires a dependency with library {@code org.ow2.asm:asm:9.4}.
 * </p>
 *
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 *
 * @see AnnotationAdopter
 */
public final class AsmClassPathScannerAdopter extends AbstractMessageAdopter
{
  private static final String MESSAGE_DEFS_DESCRIPTOR = getDescriptor(MessageDefs.class);
  private static final String MESSAGE_DEF_DESCRIPTOR = getDescriptor(MessageDef.class);
  private static final String TEMPLATE_DEFS_DESCRIPTOR = getDescriptor(TemplateDefs.class);
  private static final String TEMPLATE_DEF_DESCRIPTOR = getDescriptor(TemplateDef.class);
  private static final String TEXT_DESCRIPTOR = getDescriptor(Text.class);

  private static final Set<String> ZIP_PROTOCOLS = new HashSet<>(asList("zip", "jar", "war"));

  private final ClassLoader classLoader;
  private final Set<String> packageNames;
  private final Set<String> visitedClasses;
  private final AnnotationAdopter annotationAdopter;


  public AsmClassPathScannerAdopter(@NotNull ConfigurableMessageSupport configurableMessageSupport,
                                    @NotNull Set<String> packageNames, ClassLoader classLoader)
  {
    this(configurableMessageSupport.getAccessor().getMessageFactory(), configurableMessageSupport,
        packageNames, classLoader);
  }


  public AsmClassPathScannerAdopter(@NotNull MessageFactory messageFactory,
                                    @NotNull MessagePublisher publisher,
                                    @NotNull Set<String> packageNames,
                                    ClassLoader classLoader)
  {
    super(messageFactory, publisher);

    this.packageNames = packageNames;
    this.classLoader = classLoader == null ? getSystemClassLoader() : classLoader;

    visitedClasses = new HashSet<>();
    annotationAdopter = new AnnotationAdopter(messageFactory, publisher);
  }


  public void scan()
  {
    try {
      for(val packageName: packageNames)
        scan(packageName);
    } catch(Exception ex) {
      throw new MessageAdopterException("failed to scan class path for messages and templates", ex);
    }
  }


  private void scan(@NotNull String packageName) throws Exception
  {
    String classPathPrefix = packageName.replace('.', '/');
    if (!classPathPrefix.endsWith("/"))
      classPathPrefix = classPathPrefix + '/';

    for(final Enumeration<URL> urls = classLoader.getResources(classPathPrefix); urls.hasMoreElements();)
    {
      val url = urls.nextElement();

      if (ZIP_PROTOCOLS.contains(url.getProtocol()))
        scan_zipEntries(url, classPathPrefix);
      else
      {
        val directory = url.getFile();
        val baseDirectory = new File(directory.endsWith(classPathPrefix)
            ? directory.substring(0, directory.length() - classPathPrefix.length()) : directory);

        if (baseDirectory.isDirectory())
          scan_directory(baseDirectory, new File(directory));
      }
    }
  }


  private void scan_directory(@NotNull File baseDirectory, @NotNull File directory) throws IOException
  {
    val files = directory.listFiles();
    if (files != null)
    {
      val baseDirectoryPath = baseDirectory.toPath();

      for(val file: files)
        if (file.isDirectory())
          scan_directory(baseDirectory, file);
        else
        {
          val classNamePath =
              baseDirectoryPath.relativize(file.toPath()).toString().replace('\\', '/');

          if (classNamePath.endsWith(".class") && scan_checkVisited(classNamePath))
          {
            try(val classInputStream = new FileInputStream(file)) {
              scan_parseClass(classInputStream);
            }
          }
        }
    }
  }


  private void scan_zipEntries(@NotNull URL zipUrl, @NotNull String classPathPrefix) throws IOException
  {
    val con = zipUrl.openConnection();
    final ZipFile zipFile;

    if (con instanceof JarURLConnection)
      zipFile = ((JarURLConnection)con).getJarFile();
    else
    {
      val urlFile = zipUrl.getFile();
      try {
        int separatorIndex = urlFile.indexOf("*/");
        if (separatorIndex == -1)
          separatorIndex = urlFile.indexOf("!/");

        zipFile = scan_createZipFileFromUrl(separatorIndex != -1 ? urlFile.substring(0, separatorIndex) : urlFile);
      } catch(ZipException ex) {
        return;
      }
    }

    try {
      for(Enumeration<? extends ZipEntry> entries = zipFile.entries(); entries.hasMoreElements();)
      {
        val zipEntry = entries.nextElement();
        val classPathName = zipEntry.getName();

        if (classPathName.endsWith(".class") &&
            classPathName.startsWith(classPathPrefix) &&
            scan_checkVisited(classPathName))
        {
          try(val classInputStream = zipFile.getInputStream(zipEntry)) {
            scan_parseClass(classInputStream);
          }
        }
      }
    } finally {
      zipFile.close();
    }
  }


  private @NotNull ZipFile scan_createZipFileFromUrl(@NotNull String zipFileUrl) throws IOException
  {
    if (zipFileUrl.startsWith("file:"))
    {
      try {
        return new JarFile(new URI(zipFileUrl.replace(" ", "%20")).getSchemeSpecificPart());
      } catch(URISyntaxException ex) {
        return new JarFile(zipFileUrl.substring(5));
      }
    }

    return new JarFile(zipFileUrl);
  }


  private boolean scan_checkVisited(@NotNull String classPathName) {
    return visitedClasses.add(classPathName);
  }


  private void scan_parseClass(@NotNull InputStream classInputStream) throws IOException {
    new ClassReader(classInputStream).accept(new MainClassVisitor(), 0);
  }




  @SuppressWarnings("DuplicatedCode")
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
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible)
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
  }




  @SuppressWarnings("DuplicatedCode")
  private final class MessageMethodVisitor extends MethodVisitor
  {
    private MessageMethodVisitor() {
      super(ASM9);
    }


    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible)
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
  }




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
      annotationAdopter.adopt(new MessageDefImpl(code, text, texts.toArray(new Text[0])));
    }
  }




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
      annotationAdopter.adopt(new TemplateDefImpl(name, text, texts.toArray(new Text[0])));
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
      inheritedTexts.add(new TextImpl(locale, text, value));
    }
  }
}
