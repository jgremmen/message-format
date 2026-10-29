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
package de.sayayi.lib.message.annotation.adopter;

import de.sayayi.lib.message.Message.LocaleAware;
import de.sayayi.lib.message.MessageSupport.ConfigurableMessageSupport;
import de.sayayi.lib.message.MessageSupport.MessageAccessor;
import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.annotation.MessageDef;
import de.sayayi.lib.message.annotation.TemplateDef;
import de.sayayi.lib.message.annotation.Text;
import de.sayayi.lib.message.annotation.adopter.fixture.AnnotationsFixture;
import de.sayayi.lib.message.annotation.adopter.util.SyntheticMessageDef;
import de.sayayi.lib.message.annotation.adopter.util.SyntheticTemplateDef;
import de.sayayi.lib.message.annotation.adopter.util.SyntheticText;
import de.sayayi.lib.message.exception.MessageAdopterException;
import de.sayayi.lib.message.exception.MessageParserException;
import de.sayayi.lib.message.internal.MessageTemplate;
import lombok.val;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.createTempFile;
import static java.util.Locale.*;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.objectweb.asm.Opcodes.*;


/**
 * Tests for all {@link AnnotationAdopter} methods.
 *
 * @author Jeroen Gremmen
 * @since 0.24.0
 */
@DisplayName("AnnotationAdopter adopt methods")
@TestMethodOrder(MethodOrderer.DisplayName.class)
class AnnotationAdopterTest
{
  @Test
  @DisplayName("adopt(Class<?> type)")
  void testAdoptType()
  {
    val cms = newMessageSupport();

    new AnnotationAdopter(cms).adopt(AnnotationsFixture.class);
    verifyFixture(cms.getMessageAccessor());
  }


  @Test
  @DisplayName("adopt(Path classFile)")
  void testAdoptPath() throws Exception
  {
    val cms = newMessageSupport();

    new AnnotationAdopter(cms).adopt(classFilePath(AnnotationsFixture.class));
    verifyFixture(cms.getMessageAccessor());
  }


  @Test
  @DisplayName("ignore synthetic method annotations")
  void testIgnoreSyntheticMethodAnnotations() throws Exception
  {
    val cms = newMessageSupport();

    new AnnotationAdopter(cms).adopt(syntheticMethodFixtureClassFile());

    val accessor = cms.getMessageAccessor();
    assertTrue(accessor.hasMessageWithCode("control-msg"));
    assertEquals("Control message", accessor.getMessageByCode("control-msg").asFormatString(UTF_8));
    assertTrue(accessor.hasTemplateWithName("control-tmpl"));
    assertEquals("Control template",
        ((MessageTemplate)accessor.getTemplateByName("control-tmpl")).getMessage().asFormatString(UTF_8));

    assertFalse(accessor.hasMessageWithCode("synthetic-msg"));
    assertFalse(accessor.hasTemplateWithName("synthetic-tmpl"));
  }


  @Test
  @DisplayName("adopt(Class<?> innerType)")
  void testAdoptInnerType()
  {
    val cms = newMessageSupport();

    new AnnotationAdopter(cms).adopt(AnnotationsFixture.InnerRecord.class);
    verifyInnerRecordFixture(cms.getMessageAccessor());
  }


  @Test
  @DisplayName("adopt(Path innerClassFile)")
  void testAdoptInnerPath() throws Exception
  {
    val cms = newMessageSupport();

    new AnnotationAdopter(cms).adopt(classFilePath(AnnotationsFixture.InnerRecord.class));
    verifyInnerRecordFixture(cms.getMessageAccessor());
  }


  @Test
  @DisplayName("adopt(ClassLoader, Set<String>)")
  void testAdoptClassLoader()
  {
    val cms = newMessageSupport();

    new AnnotationAdopter(cms).adopt(
        AnnotationsFixture.class.getClassLoader(),
        Set.of(AnnotationsFixture.class.getPackageName()));
    verifyFixture(cms.getMessageAccessor());
    verifyInnerRecordFixture(cms.getMessageAccessor());
  }


  @Test
  @DisplayName("adopt(ClassLoader, Set<String>) with spaces and non-ASCII characters in directory path")
  @SuppressWarnings("UnnecessaryUnicodeEscape")
  void testAdoptClassLoaderDirectoryWithSpaces(@TempDir Path tempDir) throws Exception
  {
    val classesDir = copyFixturePackage(tempDir.resolve("with space/\u00e4\u00f6\u00fc \u65e5\u672c"));
    val cms = newMessageSupport();

    try(val classLoader = new URLClassLoader(new URL[] { classesDir.toUri().toURL() }, null)) {
      new AnnotationAdopter(cms).adopt(classLoader, Set.of(AnnotationsFixture.class.getPackageName()));
    }

    verifyFixture(cms.getMessageAccessor());
    verifyInnerRecordFixture(cms.getMessageAccessor());
  }


  @Test
  @DisplayName("adopt(ClassLoader, Set<String>) with spaces and non-ASCII characters in jar path")
  @SuppressWarnings("UnnecessaryUnicodeEscape")
  void testAdoptClassLoaderJarWithSpaces(@TempDir Path tempDir) throws Exception
  {
    val jarFile = createFixtureJar(tempDir,
        createDirectories(tempDir.resolve("with space/\u00e4\u00f6\u00fc")).resolve("fixture.jar"));
    val cms = newMessageSupport();

    try(val classLoader = new URLClassLoader(new URL[] { jarFile.toUri().toURL() }, null)) {
      new AnnotationAdopter(cms).adopt(classLoader, Set.of(AnnotationsFixture.class.getPackageName()));
    }

    verifyFixture(cms.getMessageAccessor());
    verifyInnerRecordFixture(cms.getMessageAccessor());
  }


  @Test
  @DisplayName("adopt(ClassLoader, Set<String>) fails for unsupported url protocols")
  void testAdoptClassLoaderUnsupportedProtocol() throws Exception
  {
    val url = URL.of(URI.create("vfs:/content/app.war/WEB-INF/classes/test/"), new URLStreamHandler() {
      @Override
      protected URLConnection openConnection(URL u) {
        throw new UnsupportedOperationException();
      }
    });
    val classLoader = new ClassLoader(null) {
      @Override
      public Enumeration<URL> getResources(String name) {
        return Collections.enumeration(List.of(url));
      }
    };

    val ex = assertThrows(MessageAdopterException.class,
        () -> new AnnotationAdopter(newMessageSupport()).adopt(classLoader, Set.of("test")));
    assertTrue(ex.getMessage().contains("vfs:"));
  }


  @Test
  @DisplayName("propagate MessageParserException from adopt entry points")
  void testPropagateParserExceptionFromEntryPoints() throws Exception
  {
    val classFile = invalidTemplateClassFile();
    val invalidTemplateFixtureClass = invalidTemplateFixtureClass();

    assertThrows(MessageParserException.class,
        () -> new AnnotationAdopter(newMessageSupport()).adopt(classFile));
    assertThrows(MessageParserException.class,
        () -> new AnnotationAdopter(newMessageSupport()).adopt(invalidTemplateFixtureClass));
    assertThrows(MessageParserException.class,
        () -> new AnnotationAdopter(newMessageSupport())
            .adopt(invalidTemplateFixtureClass.getClassLoader(), Set.of(invalidTemplateFixtureClass.getPackageName())));
  }


  @Test
  @DisplayName("avoid duplicate visit across adopt(Class<?>) and adopt(ClassLoader, Set<String>)")
  void testAvoidDuplicateVisitAcrossEntryPoints()
  {
    val cms = newMessageSupport();
    val adopter = new AnnotationAdopter(cms);

    adopter.adopt(AnnotationsFixture.class);
    adopter.adopt(
        AnnotationsFixture.class.getClassLoader(),
        Set.of(AnnotationsFixture.class.getPackageName()));

    verifyFixture(cms.getMessageAccessor());
    verifyInnerRecordFixture(cms.getMessageAccessor());
  }


  @Test
  @DisplayName("adopt(MessageDef)")
  void testAdoptMessageDef()
  {
    val cms = newMessageSupport();
    val adopter = new AnnotationAdopter(cms);

    // Form 1: plain text= attribute (no @Text), uses single-text path
    adopter.adopt(new SyntheticMessageDef("MSG-D1", "Direct message 1"));

    // Form 2: texts=@Text("value") — single @Text via value attribute, ROOT locale
    adopter.adopt(new SyntheticMessageDef("MSG-D2", "",
        new SyntheticText[] {
            new SyntheticText("", "", "Direct message 2")
        }));

    // Form 3: multi-locale texts
    adopter.adopt(new SyntheticMessageDef("MSG-D3", "",
        new SyntheticText[] {
            new SyntheticText("en", "EN direct msg 3", ""),
            new SyntheticText("de", "DE direct msg 3", ""),
            new SyntheticText("fr", "FR direct msg 3", "")
        }));

    val accessor = cms.getMessageAccessor();

    assertTrue(accessor.hasMessageWithCode("MSG-D1"));
    assertEquals("Direct message 1", accessor.getMessageByCode("MSG-D1").asFormatString(UTF_8));

    assertTrue(accessor.hasMessageWithCode("MSG-D2"));
    assertEquals("Direct message 2", accessor.getMessageByCode("MSG-D2").asFormatString(UTF_8));

    assertTrue(accessor.hasMessageWithCode("MSG-D3"));
    val msg3 = accessor.getMessageByCode("MSG-D3");
    assertInstanceOf(LocaleAware.class, msg3);
    val locMessages3 = ((LocaleAware) msg3).getLocalizedMessages();
    assertEquals("EN direct msg 3", locMessages3.get(ENGLISH).asFormatString(UTF_8));
    assertEquals("DE direct msg 3", locMessages3.get(GERMAN).asFormatString(UTF_8));
    assertEquals("FR direct msg 3", locMessages3.get(FRENCH).asFormatString(UTF_8));
  }


  @Test
  @DisplayName("adopt(TemplateDef)")
  void testAdoptTemplateDef()
  {
    val cms = newMessageSupport();
    val adopter = new AnnotationAdopter(cms);

    // Form 1: plain text= attribute (no @Text), uses single-text path
    adopter.adopt(new SyntheticTemplateDef("tmpl-d1", "Direct template 1"));

    // Form 2: texts=@Text("value") — single @Text via value attribute, ROOT locale
    adopter.adopt(new SyntheticTemplateDef("tmpl-d2", "",
        new SyntheticText[] {
            new SyntheticText("", "", "Direct template 2")
        }));

    // Form 3: multi-locale texts
    adopter.adopt(new SyntheticTemplateDef("tmpl-d3", "",
        new SyntheticText[] {
            new SyntheticText("en", "EN direct tmpl 3", ""),
            new SyntheticText("de", "DE direct tmpl 3", ""),
            new SyntheticText("fr", "FR direct tmpl 3", "")
        }));

    val accessor = cms.getMessageAccessor();

    assertTrue(accessor.hasTemplateWithName("tmpl-d1"));
    assertEquals("Direct template 1",
        ((MessageTemplate)accessor.getTemplateByName("tmpl-d1")).getMessage().asFormatString(UTF_8));

    assertTrue(accessor.hasTemplateWithName("tmpl-d2"));
    assertEquals("Direct template 2",
        ((MessageTemplate)accessor.getTemplateByName("tmpl-d2")).getMessage().asFormatString(UTF_8));

    assertTrue(accessor.hasTemplateWithName("tmpl-d3"));
    val tmpl3 = ((MessageTemplate)accessor.getTemplateByName("tmpl-d3")).getMessage();
    assertInstanceOf(LocaleAware.class, tmpl3);
    val locTemplates3 = ((LocaleAware) tmpl3).getLocalizedMessages();
    assertEquals("EN direct tmpl 3", locTemplates3.get(ENGLISH).asFormatString(UTF_8));
    assertEquals("DE direct tmpl 3", locTemplates3.get(GERMAN).asFormatString(UTF_8));
    assertEquals("FR direct tmpl 3", locTemplates3.get(FRENCH).asFormatString(UTF_8));
  }


  @Test
  @DisplayName("adopt(MessageDef) with BCP 47 language tags")
  void testAdoptMessageDefLanguageTags()
  {
    val cms = newMessageSupport();

    new AnnotationAdopter(cms).adopt(new SyntheticMessageDef("MSG-L1", "",
        new SyntheticText[] {
            new SyntheticText("", "root", ""),
            new SyntheticText("de", "de", ""),
            new SyntheticText("de-DE", "de-DE", ""),
            new SyntheticText("fr-CA", "fr-CA", ""),
            new SyntheticText("zh-Hant-TW", "zh-Hant-TW", "")
        }));

    val localizedMessages =
        ((LocaleAware)cms.getMessageAccessor().getMessageByCode("MSG-L1")).getLocalizedMessages();

    assertEquals(
        Set.of(ROOT, GERMAN, GERMANY, CANADA_FRENCH, Locale.forLanguageTag("zh-Hant-TW")),
        localizedMessages.keySet());
    assertEquals("de-DE", localizedMessages.get(GERMANY).asFormatString(UTF_8));
    assertEquals("de-DE", cms.code("MSG-L1").locale(GERMANY).format());
    assertEquals("root", cms.code("MSG-L1").locale(ITALIAN).format());
  }


  @Test
  @DisplayName("adopt(MessageDef) rejects invalid locales")
  void testAdoptMessageDefInvalidLocale()
  {
    for(var locale: List.of("de_DE", "fr_CA", "not a locale", "de-"))
    {
      val adopter = new AnnotationAdopter(newMessageSupport());
      val messageDef = new SyntheticMessageDef("MSG-INVALID", "",
          new SyntheticText[] {
              new SyntheticText("en", "English", ""),
              new SyntheticText(locale, "Other", "")
          });

      val message =
          assertThrows(MessageAdopterException.class, () -> adopter.adopt(messageDef), locale).getMessage();
      assertTrue(message.contains("'" + locale + "'"), message);
      assertTrue(message.contains("message 'MSG-INVALID'"), message);
    }
  }


  @Test
  @DisplayName("adopt(TemplateDef) with BCP 47 language tags")
  void testAdoptTemplateDefLanguageTags()
  {
    val cms = newMessageSupport();

    new AnnotationAdopter(cms).adopt(new SyntheticTemplateDef("tmpl-l1", "",
        new SyntheticText[] {
            new SyntheticText("en", "en", ""),
            new SyntheticText("en-GB", "en-GB", "")
        }));

    val localizedTemplates =
        ((LocaleAware)((MessageTemplate)cms.getMessageAccessor().getTemplateByName("tmpl-l1")).getMessage())
            .getLocalizedMessages();

    assertEquals(Set.of(ENGLISH, UK), localizedTemplates.keySet());
    assertEquals("en-GB", localizedTemplates.get(UK).asFormatString(UTF_8));
  }


  @Test
  @DisplayName("adopt(TemplateDef) rejects invalid locales")
  void testAdoptTemplateDefInvalidLocale()
  {
    val adopter = new AnnotationAdopter(newMessageSupport());
    val templateDef = new SyntheticTemplateDef("tmpl-invalid", "",
        new SyntheticText[] {
            new SyntheticText("en_GB", "English", ""),
            new SyntheticText("de", "Deutsch", "")
        });

    val ex = assertThrows(MessageAdopterException.class, () -> adopter.adopt(templateDef));
    assertTrue(ex.getMessage().contains("'en_GB'"), ex.getMessage());
    assertTrue(ex.getMessage().contains("template 'tmpl-invalid'"), ex.getMessage());
  }


  @Test
  @DisplayName("adopt(Class<?> type) rejects invalid locales")
  void testAdoptTypeInvalidLocale()
  {
    assertThrows(MessageAdopterException.class,
        () -> new AnnotationAdopter(newMessageSupport()).adopt(InvalidMessageLocaleFixture.class));
    assertThrows(MessageAdopterException.class,
        () -> new AnnotationAdopter(newMessageSupport()).adopt(InvalidTemplateLocaleFixture.class));
  }


  @Contract(pure = true)
  @Test
  @DisplayName("adopt(ClassLoader, Set<String>) does not close a cached jar file used by other code")
  @SuppressWarnings("TryFinallyCanBeTryWithResources")
  void testAdoptClassLoaderJarKeepsCachedJarFileOpen(@TempDir Path tempDir) throws Exception
  {
    val jarFile = createFixtureJar(tempDir, tempDir.resolve("fixture.jar"));

    // obtain the jar file instance that the JVM caches for jar: urls (useCaches is true by default)
    val connection = (JarURLConnection)URI.create("jar:" + jarFile.toUri() + "!/").toURL().openConnection();
    val sharedJarFile = connection.getJarFile();

    try {
      val cms = newMessageSupport();

      try(val classLoader = new URLClassLoader(new URL[] { jarFile.toUri().toURL() }, null)) {
        new AnnotationAdopter(cms).adopt(classLoader, Set.of(AnnotationsFixture.class.getPackageName()));
      }

      verifyFixture(cms.getMessageAccessor());

      // the shared instance must still be usable
      val entryName = AnnotationsFixture.class.getName().replace('.', '/') + ".class";
      try(val inputStream = sharedJarFile.getInputStream(requireNonNull(sharedJarFile.getEntry(entryName)))) {
        assertTrue(inputStream.readAllBytes().length > 0);
      }
    } finally {
      sharedJarFile.close();
    }
  }


  private static @NotNull Path createFixtureJar(@NotNull Path tempDir, @NotNull Path jarFile) throws Exception
  {
    val classesDir = copyFixturePackage(tempDir.resolve("classes"));

    try(val jar = new JarOutputStream(Files.newOutputStream(jarFile)); val paths = Files.walk(classesDir)) {
      for(val path: (Iterable<Path>)paths.filter(p -> !p.equals(classesDir))::iterator)
      {
        val entryName = classesDir.relativize(path).toString().replace('\\', '/');

        if (Files.isDirectory(path))
          jar.putNextEntry(new JarEntry(entryName + '/'));  // required to locate the package
        else
        {
          jar.putNextEntry(new JarEntry(entryName));
          jar.write(Files.readAllBytes(path));
        }

        jar.closeEntry();
      }
    }

    return jarFile;
  }


  private static @NotNull ConfigurableMessageSupport newMessageSupport() {
    return MessageSupportFactory.createDefault();
  }


  @Contract(pure = true)
  private static @NotNull Path copyFixturePackage(@NotNull Path classesDir) throws Exception
  {
    val packagePath = AnnotationsFixture.class.getPackageName().replace('.', '/');
    val sourceDir = classFilePath(AnnotationsFixture.class).getParent();
    val targetDir = createDirectories(classesDir.resolve(packagePath));

    try(val files = Files.list(sourceDir)) {
      for(val file: (Iterable<Path>)files.filter(f -> f.toString().endsWith(".class"))::iterator)
        Files.copy(file, targetDir.resolve(file.getFileName().toString()));
    }

    return classesDir;
  }


  private static @NotNull Path classFilePath(Class<?> type) throws URISyntaxException
  {
    val resourceName = type.getName().replace('.', '/') + ".class";
    return Path.of(requireNonNull(type.getClassLoader().getResource(resourceName)).toURI());
  }


  private static @NotNull Path syntheticMethodFixtureClassFile() throws IOException
  {
    val writer = new ClassWriter(0);
    val fixtureClassName = (AnnotationAdopterTest.class.getPackageName() + ".SyntheticMethodFixtureGenerated")
        .replace('.', '/');

    writer.visit(V21, ACC_PUBLIC | ACC_FINAL, fixtureClassName, null, "java/lang/Object", null);

    MethodVisitor method = writer.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
    method.visitCode();
    method.visitVarInsn(ALOAD, 0);
    method.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
    method.visitInsn(RETURN);
    method.visitMaxs(1, 1);
    method.visitEnd();

    method = writer.visitMethod(ACC_PUBLIC, "control", "()V", null, null);
    val controlMessageAnnotation = method.visitAnnotation(Type.getDescriptor(MessageDef.class), true);
    controlMessageAnnotation.visit("code", "control-msg");
    controlMessageAnnotation.visit("text", "Control message");
    controlMessageAnnotation.visitEnd();
    val controlTemplateAnnotation = method.visitAnnotation(Type.getDescriptor(TemplateDef.class), true);
    controlTemplateAnnotation.visit("name", "control-tmpl");
    controlTemplateAnnotation.visit("text", "Control template");
    controlTemplateAnnotation.visitEnd();
    method.visitCode();
    method.visitInsn(RETURN);
    method.visitMaxs(0, 1);
    method.visitEnd();

    method = writer.visitMethod(ACC_PRIVATE | ACC_STATIC | ACC_SYNTHETIC, "synthetic$annotated", "()V", null, null);
    val syntheticMessageAnnotation = method.visitAnnotation(Type.getDescriptor(MessageDef.class), true);
    syntheticMessageAnnotation.visit("code", "synthetic-msg");
    syntheticMessageAnnotation.visit("text", "Synthetic message");
    syntheticMessageAnnotation.visitEnd();
    val syntheticTemplateAnnotation = method.visitAnnotation(Type.getDescriptor(TemplateDef.class), true);
    syntheticTemplateAnnotation.visit("name", "synthetic-tmpl");
    syntheticTemplateAnnotation.visit("text", "Synthetic template");
    syntheticTemplateAnnotation.visitEnd();
    method.visitCode();
    method.visitInsn(RETURN);
    method.visitMaxs(0, 0);
    method.visitEnd();

    writer.visitEnd();

    val classFile = createTempFile("synthetic-method-fixture-", ".class");
    Files.write(classFile, writer.toByteArray());
    classFile.toFile().deleteOnExit();

    return classFile;
  }


  private static @NotNull Path invalidTemplateClassFile() throws IOException
  {
    val writer = new ClassWriter(0);
    val fixtureClassName = (AnnotationAdopterTest.class.getPackageName() + ".InvalidTemplateFixtureGenerated")
        .replace('.', '/');

    writer.visit(V21, ACC_PUBLIC | ACC_FINAL, fixtureClassName, null, "java/lang/Object", null);

    MethodVisitor method = writer.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
    method.visitCode();
    method.visitVarInsn(ALOAD, 0);
    method.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
    method.visitInsn(RETURN);
    method.visitMaxs(1, 1);
    method.visitEnd();

    val classAnnotation = writer.visitAnnotation(Type.getDescriptor(TemplateDef.class), true);
    classAnnotation.visit("name", "invalid-template");
    classAnnotation.visit("text", "%{x,true false:1}");
    classAnnotation.visitEnd();

    writer.visitEnd();

    val classFile = createTempFile("invalid-template-fixture-", ".class");
    Files.write(classFile, writer.toByteArray());
    classFile.toFile().deleteOnExit();

    return classFile;
  }


  private static @NotNull Class<?> invalidTemplateFixtureClass() {
    return InvalidTemplateFixture.class;
  }


  private static void verifyFixture(@NotNull MessageAccessor accessor)
  {
    // --- Messages: type-level ---

    // type-msg-1: plain text= form
    assertTrue(accessor.hasMessageWithCode("type-msg-1"));
    assertEquals("Type message 1", accessor.getMessageByCode("type-msg-1").asFormatString(UTF_8));

    // type-msg-2: texts=@Text("value") form — single ROOT-locale text
    assertTrue(accessor.hasMessageWithCode("type-msg-2"));
    assertEquals("Type message 2", accessor.getMessageByCode("type-msg-2").asFormatString(UTF_8));

    // type-msg-3: multi-locale (EN, DE, FR)
    assertTrue(accessor.hasMessageWithCode("type-msg-3"));
    val typeMsg3 = accessor.getMessageByCode("type-msg-3");
    assertInstanceOf(LocaleAware.class, typeMsg3);
    val typeMsg3Locales = ((LocaleAware)typeMsg3).getLocalizedMessages();
    assertEquals("EN type msg 3", typeMsg3Locales.get(ENGLISH).asFormatString(UTF_8));
    assertEquals("DE type msg 3", typeMsg3Locales.get(GERMAN).asFormatString(UTF_8));
    assertEquals("FR type msg 3", typeMsg3Locales.get(FRENCH).asFormatString(UTF_8));

    // --- Messages: method-level ---

    // method-msg-1: plain text= form (@MessageDefs container on method)
    assertTrue(accessor.hasMessageWithCode("method-msg-1"));
    assertEquals("Method message 1", accessor.getMessageByCode("method-msg-1").asFormatString(UTF_8));

    // method-msg-2: texts=@Text("value") form (@MessageDefs container on method)
    assertTrue(accessor.hasMessageWithCode("method-msg-2"));
    assertEquals("Method message 2", accessor.getMessageByCode("method-msg-2").asFormatString(UTF_8));

    // method-msg-3: multi-locale (EN, DE) — standalone @MessageDef on method
    assertTrue(accessor.hasMessageWithCode("method-msg-3"));
    val methodMsg3 = accessor.getMessageByCode("method-msg-3");
    assertInstanceOf(LocaleAware.class, methodMsg3);
    val methodMsg3Locales = ((LocaleAware)methodMsg3).getLocalizedMessages();
    assertEquals("EN method msg 3", methodMsg3Locales.get(ENGLISH).asFormatString(UTF_8));
    assertEquals("DE method msg 3", methodMsg3Locales.get(GERMAN).asFormatString(UTF_8));

    // --- Templates: type-level ---

    // type-tmpl-1: plain text= form
    assertTrue(accessor.hasTemplateWithName("type-tmpl-1"));
    assertEquals("Type template 1",
        ((MessageTemplate)accessor.getTemplateByName("type-tmpl-1")).getMessage().asFormatString(UTF_8));

    // type-tmpl-2: texts=@Text("value") form
    assertTrue(accessor.hasTemplateWithName("type-tmpl-2"));
    assertEquals("Type template 2",
        ((MessageTemplate)accessor.getTemplateByName("type-tmpl-2")).getMessage().asFormatString(UTF_8));

    // type-tmpl-3: multi-locale (EN, DE)
    assertTrue(accessor.hasTemplateWithName("type-tmpl-3"));
    val typeTmpl3 = ((MessageTemplate)accessor.getTemplateByName("type-tmpl-3")).getMessage();
    assertInstanceOf(LocaleAware.class, typeTmpl3);
    val typeTmpl3Locales = ((LocaleAware)typeTmpl3).getLocalizedMessages();
    assertEquals("EN type tmpl 3", typeTmpl3Locales.get(ENGLISH).asFormatString(UTF_8));
    assertEquals("DE type tmpl 3", typeTmpl3Locales.get(GERMAN).asFormatString(UTF_8));

    // --- Templates: method-level ---

    // method-tmpl-1: plain text= form — standalone @TemplateDef on method
    assertTrue(accessor.hasTemplateWithName("method-tmpl-1"));
    assertEquals("Method template 1",
        ((MessageTemplate)accessor.getTemplateByName("method-tmpl-1")).getMessage().asFormatString(UTF_8));

    // method-tmpl-2: texts=@Text("value") form (@TemplateDefs container on method)
    assertTrue(accessor.hasTemplateWithName("method-tmpl-2"));
    assertEquals("Method template 2",
        ((MessageTemplate)accessor.getTemplateByName("method-tmpl-2")).getMessage().asFormatString(UTF_8));

    // method-tmpl-3: multi-locale (EN, FR) — @TemplateDefs container on method
    assertTrue(accessor.hasTemplateWithName("method-tmpl-3"));
    val methodTmpl3 = ((MessageTemplate)accessor.getTemplateByName("method-tmpl-3")).getMessage();
    assertInstanceOf(LocaleAware.class, methodTmpl3);
    val methodTmpl3Locales = ((LocaleAware)methodTmpl3).getLocalizedMessages();
    assertEquals("EN method tmpl 3", methodTmpl3Locales.get(ENGLISH).asFormatString(UTF_8));
    assertEquals("FR method tmpl 3", methodTmpl3Locales.get(FRENCH).asFormatString(UTF_8));

    // --- Messages: constructor-level ---

    // ctor-msg-1: plain text= form (@MessageDefs container on constructor)
    assertTrue(accessor.hasMessageWithCode("ctor-msg-1"));
    assertEquals("Constructor message 1", accessor.getMessageByCode("ctor-msg-1").asFormatString(UTF_8));

    // ctor-msg-2: multi-locale (EN, DE) (@MessageDefs container on constructor)
    assertTrue(accessor.hasMessageWithCode("ctor-msg-2"));
    val ctorMsg2 = accessor.getMessageByCode("ctor-msg-2");
    assertInstanceOf(LocaleAware.class, ctorMsg2);
    val ctorMsg2Locales = ((LocaleAware)ctorMsg2).getLocalizedMessages();
    assertEquals("EN ctor msg 2", ctorMsg2Locales.get(ENGLISH).asFormatString(UTF_8));
    assertEquals("DE ctor msg 2", ctorMsg2Locales.get(GERMAN).asFormatString(UTF_8));
  }


  private static void verifyInnerRecordFixture(@NotNull MessageAccessor accessor)
  {
    // inner-msg-1: plain text= form
    assertTrue(accessor.hasMessageWithCode("inner-msg-1"));
    assertEquals("Inner message 1", accessor.getMessageByCode("inner-msg-1").asFormatString(UTF_8));

    // inner-msg-2: multi-locale (EN, DE)
    assertTrue(accessor.hasMessageWithCode("inner-msg-2"));
    val innerMsg2 = accessor.getMessageByCode("inner-msg-2");
    assertInstanceOf(LocaleAware.class, innerMsg2);
    val innerMsg2Locales = ((LocaleAware)innerMsg2).getLocalizedMessages();
    assertEquals("EN inner msg 2", innerMsg2Locales.get(ENGLISH).asFormatString(UTF_8));
    assertEquals("DE inner msg 2", innerMsg2Locales.get(GERMAN).asFormatString(UTF_8));

    // inner-tmpl-1: texts=@Text("value") form
    assertTrue(accessor.hasTemplateWithName("inner-tmpl-1"));
    assertEquals("Inner template 1",
        ((MessageTemplate)accessor.getTemplateByName("inner-tmpl-1")).getMessage().asFormatString(UTF_8));
  }




  @TemplateDef(name = "invalid-template", text = "%{x,true false:1}")
  private static final class InvalidTemplateFixture {
  }


  @MessageDef(code = "invalid-locale", texts = {
      @Text(locale = "en", text = "English"),
      @Text(locale = "de_DE", text = "Deutsch")
  })
  private static final class InvalidMessageLocaleFixture {
  }


  @TemplateDef(name = "invalid-locale", texts = @Text(locale = "de_DE", text = "Deutsch"))
  private static final class InvalidTemplateLocaleFixture {
  }
}
