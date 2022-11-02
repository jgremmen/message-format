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
package de.sayayi.lib.message.scanner.asm;

import de.sayayi.lib.message.MessageBundle;
import de.sayayi.lib.message.annotation.MessageDef;
import de.sayayi.lib.message.annotation.MessageDefs;
import de.sayayi.lib.message.annotation.Text;
import de.sayayi.lib.message.exception.ClassPathScannerException;
import de.sayayi.lib.message.scanner.MessageDefImpl;
import de.sayayi.lib.message.scanner.TextImpl;
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

import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.ASM9;
import static org.objectweb.asm.Type.getDescriptor;


/**
 * @author Jeroen Gremmen
 */
final class AsmClassPathScanner
{
  private static final String MESSAGE_DEFS_DESCRIPTOR = getDescriptor(MessageDefs.class);
  private static final String MESSAGE_DEF_DESCRIPTOR = getDescriptor(MessageDef.class);
  private static final String TEXT_DESCRIPTOR = getDescriptor(Text.class);

  private static final Set<String> ZIP_PROTOCOLS = new HashSet<>(Arrays.asList("zip", "jar", "war"));

  private final MessageBundle messageBundle;
  private final ClassLoader classLoader;
  private final Set<String> packageNames;
  private final Set<String> visitedClasses;


  public AsmClassPathScanner(@NotNull MessageBundle messageBundle, @NotNull Set<String> packageNames,
                             ClassLoader classLoader)
  {
    this.messageBundle = messageBundle;
    this.classLoader = classLoader == null ? ClassLoader.getSystemClassLoader() : classLoader;
    this.packageNames = packageNames;

    visitedClasses = new HashSet<>();
  }


  public void run()
  {
    try {
      for(val packageName: packageNames)
        scan(packageName);
    } catch(Exception ex) {
      throw new ClassPathScannerException("failed to scan class path for messages", ex);
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
    public void visitEnd()
    {
      messageBundle.add(messageBundle.getMessageFactory().parse(
          new MessageDefImpl(code, text, texts.toArray(new Text[0]))));
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