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
package de.sayayi.lib.message.scanner.spring;

import de.sayayi.lib.message.AbstractScannedMessageBundle;
import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.annotation.MessageDef;
import de.sayayi.lib.message.annotation.MessageDefs;
import de.sayayi.lib.message.annotation.Text;
import de.sayayi.lib.message.scanner.MessageDefImpl;
import de.sayayi.lib.message.scanner.TextImpl;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.classreading.MetadataReader;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static java.util.Objects.requireNonNull;


/**
 * @author Jeroen Gremmen
 */
public class SpringClassPathMessageBundle extends AbstractScannedMessageBundle
{
  private static final String MESSAGE_DEFS_ANNOTATION_CLASSNAME = MessageDefs.class.getName();
  private static final String MESSAGE_DEF_ANNOTATION_CLASSNAME = MessageDef.class.getName();

  private final ResourceLoader resourceLoader;


  public SpringClassPathMessageBundle(@NotNull MessageFactory messageFactory, @NotNull Set<String> packageNames,
                                      @NotNull ResourceLoader resourceLoader)
  {
    super(messageFactory, packageNames, requireNonNull(resourceLoader.getClassLoader()));

    this.resourceLoader = resourceLoader;
  }


  @Override
  protected void scan(@NotNull Set<String> packageNames, @NotNull ClassLoader classLoader)
  {
    val scanner = new ClassPathScanningMessagesProvider();

    scanner.setResourceLoader(resourceLoader);

    for(val packageName: packageNames)
      scanner.findCandidateComponents(packageName);
  }




  private final class ClassPathScanningMessagesProvider extends ClassPathScanningCandidateComponentProvider
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
        val annotationMetadata = metadataReader.getAnnotationMetadata();

        handleAnnotations(annotationMetadata.getAnnotations());

        annotationMetadata.getAnnotatedMethods(MESSAGE_DEFS_ANNOTATION_CLASSNAME).forEach(mmd ->
            handleMessageDefsAnnotations(mmd.getAnnotations()));

        annotationMetadata.getAnnotatedMethods(MESSAGE_DEF_ANNOTATION_CLASSNAME).forEach(mmd ->
            handleMessageDefAnnotations(mmd.getAnnotations()));
      }

      return false;
    }


    private void handleAnnotations(MergedAnnotations annotations)
    {
      handleMessageDefsAnnotations(annotations);
      handleMessageDefAnnotations(annotations);
    }


    private void handleMessageDefsAnnotations(MergedAnnotations annotations)
    {
      MergedAnnotation<MessageDefs> messageDefsAnnotation = annotations.get(MESSAGE_DEFS_ANNOTATION_CLASSNAME);
      if (messageDefsAnnotation.isDirectlyPresent())
        handleMessageDefs(messageDefsAnnotation);
    }


    private void handleMessageDefAnnotations(MergedAnnotations annotations)
    {
      MergedAnnotation<MessageDef> messageDefAnnotation = annotations.get(MESSAGE_DEF_ANNOTATION_CLASSNAME);
      if (messageDefAnnotation.isDirectlyPresent())
        handleMessageDef(messageDefAnnotation);
    }


    private void handleMessageDefs(MergedAnnotation<MessageDefs> annotation)
    {
      for(val messageDefAnnotation: annotation.getAnnotationArray("value", MessageDef.class))
        handleMessageDef(messageDefAnnotation);
    }


    private void handleMessageDef(MergedAnnotation<MessageDef> annotation)
    {
      val texts = new ArrayList<Text>();

      for(val textAnnotation: annotation.getAnnotationArray("texts", Text.class))
      {
        texts.add(new TextImpl(
            textAnnotation.getString("locale"),
            textAnnotation.getString("text"),
            textAnnotation.getString("value")));
      }

      val message = getMessageFactory().parse(new MessageDefImpl(
          annotation.getString("code"),
          annotation.getString("text"),
          texts.toArray(new Text[0])));

      add(message);
    }
  }
}
