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
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;

import java.util.*;


/**
 * The Spring classpath scanner scans classes and publishes the annotated messages found.
 * <p>
 * The scanned classes are not loaded by the classloader but instead are analysed using the Spring
 * {@code ClassPathScanningCandidateComponentProvider} class. Using this class therefore requires
 * a dependency with library {@code org.springframework:spring-context:5.3.26}.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 *
 * @see SpringAsmClassPathScannerAdopter
 */
public final class SpringClassPathScannerAdopter extends AbstractMessageAdopter
    implements ClassPathScannerAdopter
{
  private static final String MESSAGE_DEFS_ANNOTATION_CLASSNAME = MessageDefs.class.getName();
  private static final String MESSAGE_DEF_ANNOTATION_CLASSNAME = MessageDef.class.getName();
  private static final String TEMPLATE_DEFS_ANNOTATION_CLASSNAME = TemplateDefs.class.getName();
  private static final String TEMPLATE_DEF_ANNOTATION_CLASSNAME = TemplateDef.class.getName();

  private final @NotNull Set<String> packageNames;
  private final @NotNull ResourceLoader resourceLoader;
  private final @NotNull AnnotationAdopter annotationAdopter;


  public SpringClassPathScannerAdopter(@NotNull ConfigurableMessageSupport configurableMessageSupport,
                                       @NotNull Set<String> packageNames,
                                       @NotNull ResourceLoader resourceLoader)
  {
    this(configurableMessageSupport.getAccessor().getMessageFactory(), configurableMessageSupport,
        packageNames, resourceLoader);
  }


  public SpringClassPathScannerAdopter(@NotNull MessageFactory messageFactory,
                                       @NotNull MessagePublisher publisher,
                                       @NotNull Set<String> packageNames,
                                       @NotNull ResourceLoader resourceLoader) {
    super(messageFactory, publisher);

    this.packageNames = packageNames;
    this.resourceLoader = resourceLoader;

    annotationAdopter = new AnnotationAdopter(messageFactory, publisher);
  }


  @Override
  public void scan()
  {
    final ClassPathScanningCandidateComponentProvider scanner =
        new ClassPathScanningMessagesProvider();

    scanner.setResourceLoader(resourceLoader);

    packageNames.forEach(scanner::findCandidateComponents);
  }




  private final class ClassPathScanningMessagesProvider
      extends ClassPathScanningCandidateComponentProvider
  {
    private final Set<String> visitedClassnames;


    private ClassPathScanningMessagesProvider()
    {
      super(false);

      visitedClassnames = new HashSet<>();
    }


    @Override
    protected boolean isCandidateComponent(@NotNull AnnotatedBeanDefinition beanDefinition) {
      return true;
    }


    @Override
    protected boolean isCandidateComponent(MetadataReader metadataReader)
    {
      if (visitedClassnames.add(metadataReader.getClassMetadata().getClassName()))
      {
        final AnnotationMetadata annotationMetadata = metadataReader.getAnnotationMetadata();

        handleAnnotations(annotationMetadata.getAnnotations());

        annotationMetadata.getAnnotatedMethods(MESSAGE_DEFS_ANNOTATION_CLASSNAME).forEach(mmd ->
            handleMessageDefsAnnotations(mmd.getAnnotations()));

        annotationMetadata.getAnnotatedMethods(MESSAGE_DEF_ANNOTATION_CLASSNAME).forEach(mmd ->
            handleMessageDefAnnotations(mmd.getAnnotations()));

        annotationMetadata.getAnnotatedMethods(TEMPLATE_DEFS_ANNOTATION_CLASSNAME).forEach(mtd ->
            handleTemplateDefsAnnotations(mtd.getAnnotations()));

        annotationMetadata.getAnnotatedMethods(TEMPLATE_DEF_ANNOTATION_CLASSNAME).forEach(mtd ->
            handleTemplateDefAnnotations(mtd.getAnnotations()));
      }

      return false;
    }


    private void handleAnnotations(MergedAnnotations annotations)
    {
      handleMessageDefsAnnotations(annotations);
      handleMessageDefAnnotations(annotations);
      handleTemplateDefsAnnotations(annotations);
      handleTemplateDefAnnotations(annotations);
    }


    private void handleMessageDefsAnnotations(MergedAnnotations annotations)
    {
      MergedAnnotation<MessageDefs> messageDefsAnnotation =
          annotations.get(MESSAGE_DEFS_ANNOTATION_CLASSNAME);

      if (messageDefsAnnotation.isDirectlyPresent())
        handleMessageDefs(messageDefsAnnotation);
    }


    private void handleMessageDefAnnotations(MergedAnnotations annotations)
    {
      MergedAnnotation<MessageDef> messageDefAnnotation =
          annotations.get(MESSAGE_DEF_ANNOTATION_CLASSNAME);

      if (messageDefAnnotation.isDirectlyPresent())
        handleMessageDef(messageDefAnnotation);
    }


    private void handleMessageDefs(MergedAnnotation<MessageDefs> messageDefs)
    {
      Arrays
          .stream(messageDefs.getAnnotationArray("value", MessageDef.class))
          .forEach(this::handleMessageDef);
    }


    private void handleMessageDef(MergedAnnotation<MessageDef> messageDef)
    {
      final List<Text> texts = new ArrayList<>();

      for(MergedAnnotation<Text> textAnnotation:
          messageDef.getAnnotationArray("texts", Text.class))
      {
        texts.add(new TextImpl(
            textAnnotation.getString("locale"),
            textAnnotation.getString("text"),
            textAnnotation.getString("value")));
      }

      annotationAdopter.adopt(new MessageDefImpl(
          messageDef.getString("code"),
          messageDef.getString("text"),
          texts.toArray(new Text[0])));
    }


    private void handleTemplateDefsAnnotations(MergedAnnotations annotations)
    {
      MergedAnnotation<TemplateDefs> templateDefsAnnotation =
          annotations.get(TEMPLATE_DEFS_ANNOTATION_CLASSNAME);

      if (templateDefsAnnotation.isDirectlyPresent())
        handleTemplateDefs(templateDefsAnnotation);
    }


    private void handleTemplateDefAnnotations(MergedAnnotations annotations)
    {
      MergedAnnotation<TemplateDef> templateDefAnnotation =
          annotations.get(TEMPLATE_DEF_ANNOTATION_CLASSNAME);

      if (templateDefAnnotation.isDirectlyPresent())
        handleTemplateDef(templateDefAnnotation);
    }


    private void handleTemplateDefs(MergedAnnotation<TemplateDefs> templateDefs)
    {
      Arrays
          .stream(templateDefs.getAnnotationArray("value", TemplateDef.class))
          .forEach(this::handleTemplateDef);
    }


    private void handleTemplateDef(MergedAnnotation<TemplateDef> templateDef)
    {
      final List<Text> texts = new ArrayList<>();

      for(MergedAnnotation<Text> textAnnotation:
          templateDef.getAnnotationArray("texts", Text.class))
      {
        texts.add(new TextImpl(
            textAnnotation.getString("locale"),
            textAnnotation.getString("text"),
            textAnnotation.getString("value")));
      }

      annotationAdopter.adopt(new TemplateDefImpl(
          templateDef.getString("name"),
          templateDef.getString("text"),
          texts.toArray(new Text[0])));
    }
  }
}
