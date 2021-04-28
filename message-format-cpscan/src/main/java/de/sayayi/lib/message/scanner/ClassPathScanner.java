/*
 * Copyright 2021 Jeroen Gremmen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.sayayi.lib.message.scanner;

import de.sayayi.lib.message.MessageBundle;
import de.sayayi.lib.message.annotation.MessageDef;
import de.sayayi.lib.message.annotation.MessageDefs;
import de.sayayi.lib.message.annotation.Text;
import de.sayayi.lib.message.exception.ClassPathScannerException;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.stream.Collectors;

import static de.sayayi.lib.message.MessageFactory.parse;
import static org.objectweb.asm.Opcodes.ASM9;
import static org.objectweb.asm.Type.getDescriptor;


/**
 * @author Jeroen Gremmen
 */
public final class ClassPathScanner
{
  private static final String MESSAGE_DEFS_DESCRIPTOR = getDescriptor(MessageDefs.class);
  private static final String MESSAGE_DEF_DESCRIPTOR = getDescriptor(MessageDef.class);
  private static final String TEXT_DESCRIPTOR =  getDescriptor(Text.class);

  private final MessageBundle messageBundle;
  private final ClassLoader classLoader;
  private final Set<String> packages;


  public ClassPathScanner(@NotNull MessageBundle messageBundle, @NotNull Set<String> packageNames,
                          ClassLoader classLoader)
  {
    this.messageBundle = messageBundle;
    this.classLoader = classLoader == null ? ClassPathScanner.class.getClassLoader() : classLoader;

    packages = packageNames.stream()
        .map(name -> {
          String path = name.replace('.', '/');
          return path.endsWith("/") ? path : (path + '/');
        })
        .collect(Collectors.toSet());
  }


  public void run()
  {
    try {
      final Set<URL> urls = new LinkedHashSet<>();

      for(ClassLoader cl = classLoader; cl != null; cl = cl.getParent())
        if (cl instanceof URLClassLoader)
          urls.addAll(Arrays.asList(((URLClassLoader)cl).getURLs()));

      for(final URL url: urls)
      {
        final File fileOrDirectory = new File(url.getPath());

        if (fileOrDirectory.isDirectory())
          run_directory(fileOrDirectory, fileOrDirectory);
        else if (fileOrDirectory.getName().endsWith(".jar"))
          run_jar(url);
      }
    } catch(Exception ex) {
      throw new ClassPathScannerException("failed to scan class path for messages", ex);
    }
  }


  private void run_directory(@NotNull File baseDirectory, @NotNull File directory) throws IOException
  {
    final File[] files = directory.listFiles();

    if (files != null)
    {
      final Path baseDirectoryPath = baseDirectory.toPath();

      for(final File file: files)
        if (file.isDirectory())
          run_directory(baseDirectory, file);
        else if (matchClassPath(baseDirectoryPath.relativize(file.toPath()).toString()))
        {
          try(InputStream classInputStream = new FileInputStream(file)) {
            run_class(classInputStream);
          }
        }
    }
  }


  private void run_jar(@NotNull URL jarUrl) throws IOException
  {
    try(final JarInputStream jarInputStream = new JarInputStream(jarUrl.openStream())) {
      JarEntry jarEntry;

      while((jarEntry = jarInputStream.getNextJarEntry()) != null)
        if (!jarEntry.isDirectory() && matchClassPath(jarEntry.getName()))
        {
          final int classSize = (int)jarEntry.getSize();
          if (classSize > 0)
          {
            final byte[] classBytes = new byte[classSize];

            if (jarInputStream.read(classBytes, 0, classBytes.length) == classSize)
            {
              try(final InputStream classInputStream = new ByteArrayInputStream(classBytes)) {
                run_class(classInputStream);
              }
            }
          }
        }
    }
  }


  private void run_class(@NotNull InputStream classInputStream) throws IOException {
    new ClassReader(classInputStream).accept(new MainClassVisitor(), 0);
  }


  private boolean matchClassPath(@NotNull String className) {
    return className.endsWith(".class") && (packages.isEmpty() || packages.stream().anyMatch(className::startsWith));
  }




  private final class MainClassVisitor extends ClassVisitor
  {
    private MainClassVisitor() {
      super(ASM9);
    }


    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                     String[] exceptions) {
      return new MessageMethodVisitor();
    }


    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible)
    {
      if (MESSAGE_DEF_DESCRIPTOR.equals(descriptor))
        return new MessageDefAnnotationVisitor();
      if (MESSAGE_DEFS_DESCRIPTOR.equals(descriptor))
        return new MessageDefsAnnotationVisitor();

      return null;
    }
  }




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
      messageBundle.add(parse(new MessageDefImpl(code, text, texts.toArray(new Text[0]))));
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
