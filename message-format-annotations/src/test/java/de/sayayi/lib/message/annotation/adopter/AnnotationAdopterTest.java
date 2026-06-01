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
import de.sayayi.lib.message.annotation.adopter.fixture.AnnotationsFixture;
import de.sayayi.lib.message.annotation.adopter.lib.AsmAnnotationAdopter;
import de.sayayi.lib.message.annotation.adopter.lib.ByteBuddyAnnotationAdopter;
import de.sayayi.lib.message.annotation.adopter.lib.SpringAnnotationAdopter;
import de.sayayi.lib.message.annotation.adopter.util.SyntheticMessageDef;
import de.sayayi.lib.message.annotation.adopter.util.SyntheticTemplateDef;
import de.sayayi.lib.message.annotation.adopter.util.SyntheticText;
import de.sayayi.lib.message.formatter.DefaultFormatterService;
import lombok.val;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.createTempFile;
import static java.util.Locale.*;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.objectweb.asm.Opcodes.*;


/**
 * Parameterized tests for all non-default {@link AnnotationAdopter} methods, executed for each of the
 * three adopter implementations: ASM, ByteBuddy and Spring.
 *
 * @author Jeroen Gremmen
 * @since 0.24.0
 */
@DisplayName("AnnotationAdopter adopt methods")
@TestMethodOrder(MethodOrderer.DisplayName.class)
class AnnotationAdopterTest
{
  @ParameterizedTest(name = "{0}")
  @MethodSource("adopters")
  @DisplayName("adopt(Class<?> type)")
  void testAdoptType(@NotNull Class<? extends AbstractAnnotationAdopter> adopterClass) throws Exception
  {
    val cms = newMessageSupport();

    createAdopter(adopterClass, cms).adopt(AnnotationsFixture.class);
    verifyFixture(cms.getMessageAccessor());
  }


  @ParameterizedTest(name = "{0}")
  @MethodSource("adopters")
  @DisplayName("adopt(Path classFile)")
  void testAdoptPath(@NotNull Class<? extends AbstractAnnotationAdopter> adopterClass) throws Exception
  {
    val cms = newMessageSupport();

    createAdopter(adopterClass, cms).adopt(classFilePath(AnnotationsFixture.class));
    verifyFixture(cms.getMessageAccessor());
  }


  @ParameterizedTest(name = "{0}")
  @MethodSource("adopters")
  @DisplayName("ignore synthetic method annotations")
  void testIgnoreSyntheticMethodAnnotations(@NotNull Class<? extends AbstractAnnotationAdopter> adopterClass)
      throws Exception
  {
    val cms = newMessageSupport();

    createAdopter(adopterClass, cms).adopt(syntheticMethodFixtureClassFile());

    val accessor = cms.getMessageAccessor();
    assertTrue(accessor.hasMessageWithCode("control-msg"));
    assertEquals("Control message", accessor.getMessageByCode("control-msg").asFormatString(UTF_8));
    assertTrue(accessor.hasTemplateWithName("control-tmpl"));
    assertEquals("Control template", accessor.getTemplateByName("control-tmpl").asFormatString(UTF_8));

    assertFalse(accessor.hasMessageWithCode("synthetic-msg"));
    assertFalse(accessor.hasTemplateWithName("synthetic-tmpl"));
  }


  @ParameterizedTest(name = "{0}")
  @MethodSource("adopters")
  @DisplayName("adopt(ClassLoader, Set<String>)")
  void testAdoptClassLoader(@NotNull Class<? extends AbstractAnnotationAdopter> adopterClass) throws Exception
  {
    val cms = newMessageSupport();

    createAdopter(adopterClass, cms).adopt(
        AnnotationsFixture.class.getClassLoader(),
        Set.of(AnnotationsFixture.class.getPackageName()));
    verifyFixture(cms.getMessageAccessor());
  }


  @ParameterizedTest(name = "{0}")
  @MethodSource("adopters")
  @DisplayName("adopt(MessageDef)")
  @SuppressWarnings("ExtractMethodRecommender")
  void testAdoptMessageDef(@NotNull Class<? extends AbstractAnnotationAdopter> adopterClass) throws Exception
  {
    val cms = newMessageSupport();
    val adopter = createAdopter(adopterClass, cms);

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


  @ParameterizedTest(name = "{0}")
  @MethodSource("adopters")
  @DisplayName("adopt(TemplateDef)")
  @SuppressWarnings("ExtractMethodRecommender")
  void testAdoptTemplateDef(@NotNull Class<? extends AbstractAnnotationAdopter> adopterClass) throws Exception
  {
    val cms = newMessageSupport();
    val adopter = createAdopter(adopterClass, cms);

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
    assertEquals("Direct template 1", accessor.getTemplateByName("tmpl-d1").asFormatString(UTF_8));

    assertTrue(accessor.hasTemplateWithName("tmpl-d2"));
    assertEquals("Direct template 2", accessor.getTemplateByName("tmpl-d2").asFormatString(UTF_8));

    assertTrue(accessor.hasTemplateWithName("tmpl-d3"));
    val tmpl3 = accessor.getTemplateByName("tmpl-d3");
    assertInstanceOf(LocaleAware.class, tmpl3);
    val locTemplates3 = ((LocaleAware) tmpl3).getLocalizedMessages();
    assertEquals("EN direct tmpl 3", locTemplates3.get(ENGLISH).asFormatString(UTF_8));
    assertEquals("DE direct tmpl 3", locTemplates3.get(GERMAN).asFormatString(UTF_8));
    assertEquals("FR direct tmpl 3", locTemplates3.get(FRENCH).asFormatString(UTF_8));
  }


  @Contract(pure = true)
  static @NotNull Stream<Arguments> adopters()
  {
    return Stream.of(
        arguments(Named.of("ASM", AsmAnnotationAdopter.class)),
        arguments(Named.of("Byte-Buddy", ByteBuddyAnnotationAdopter.class)),
        arguments(Named.of("Spring", SpringAnnotationAdopter.class)));
  }


  @Contract(pure = true)
  private static @NotNull ConfigurableMessageSupport newMessageSupport() {
    return MessageSupportFactory.create(DefaultFormatterService.getSharedInstance());
  }


  @Contract(pure = true)
  private static AbstractAnnotationAdopter createAdopter(
      @NotNull Class<? extends AbstractAnnotationAdopter> adopterClass,
      @NotNull ConfigurableMessageSupport cms) throws Exception {
    return adopterClass.getDeclaredConstructor(ConfigurableMessageSupport.class).newInstance(cms);
  }


  @Contract(pure = true)
  @SuppressWarnings("SameParameterValue")
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
    assertEquals("Type template 1", accessor.getTemplateByName("type-tmpl-1").asFormatString(UTF_8));

    // type-tmpl-2: texts=@Text("value") form
    assertTrue(accessor.hasTemplateWithName("type-tmpl-2"));
    assertEquals("Type template 2", accessor.getTemplateByName("type-tmpl-2").asFormatString(UTF_8));

    // type-tmpl-3: multi-locale (EN, DE)
    assertTrue(accessor.hasTemplateWithName("type-tmpl-3"));
    val typeTmpl3 = accessor.getTemplateByName("type-tmpl-3");
    assertInstanceOf(LocaleAware.class, typeTmpl3);
    val typeTmpl3Locales = ((LocaleAware)typeTmpl3).getLocalizedMessages();
    assertEquals("EN type tmpl 3", typeTmpl3Locales.get(ENGLISH).asFormatString(UTF_8));
    assertEquals("DE type tmpl 3", typeTmpl3Locales.get(GERMAN).asFormatString(UTF_8));

    // --- Templates: method-level ---

    // method-tmpl-1: plain text= form — standalone @TemplateDef on method
    assertTrue(accessor.hasTemplateWithName("method-tmpl-1"));
    assertEquals("Method template 1", accessor.getTemplateByName("method-tmpl-1").asFormatString(UTF_8));

    // method-tmpl-2: texts=@Text("value") form (@TemplateDefs container on method)
    assertTrue(accessor.hasTemplateWithName("method-tmpl-2"));
    assertEquals("Method template 2", accessor.getTemplateByName("method-tmpl-2").asFormatString(UTF_8));

    // method-tmpl-3: multi-locale (EN, FR) — @TemplateDefs container on method
    assertTrue(accessor.hasTemplateWithName("method-tmpl-3"));
    val methodTmpl3 = accessor.getTemplateByName("method-tmpl-3");
    assertInstanceOf(LocaleAware.class, methodTmpl3);
    val methodTmpl3Locales = ((LocaleAware)methodTmpl3).getLocalizedMessages();
    assertEquals("EN method tmpl 3", methodTmpl3Locales.get(ENGLISH).asFormatString(UTF_8));
    assertEquals("FR method tmpl 3", methodTmpl3Locales.get(FRENCH).asFormatString(UTF_8));
  }
}
