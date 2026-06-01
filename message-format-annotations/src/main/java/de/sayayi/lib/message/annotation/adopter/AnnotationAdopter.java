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

import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.MessageSupport.ConfigurableMessageSupport;
import de.sayayi.lib.message.MessageSupport.MessagePublisher;
import de.sayayi.lib.message.annotation.MessageDef;
import de.sayayi.lib.message.annotation.TemplateDef;
import de.sayayi.lib.message.annotation.Text;
import de.sayayi.lib.message.annotation.adopter.provider.AnnotationAdopterFactory;
import de.sayayi.lib.message.exception.DuplicateMessageException;
import de.sayayi.lib.message.exception.DuplicateTemplateException;
import de.sayayi.lib.message.exception.MessageAdopterException;
import de.sayayi.lib.message.exception.MessageParserException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;
import java.util.Set;


/**
 * Adopter interface for discovering and publishing messages and templates defined by {@link MessageDef} and
 * {@link TemplateDef} annotations in compiled class files.
 * <p>
 * This interface provides several strategies for locating annotated classes:
 * <ul>
 *   <li>
 *     <b>Classpath scanning</b> – {@link #adopt(ClassLoader, Set)} scans one or more packages for class files,
 *     including jar, war and zip archives.
 *   </li>
 *   <li>
 *     <b>Single class file</b> – {@link #adopt(Path)} and {@link #adopt(File)} accept individual class files.
 *   </li>
 *   <li>
 *     <b>Loaded type</b> – {@link #adopt(Class)} locates the class file of a loaded type via its class loader.
 *   </li>
 *   <li>
 *     <b>Annotation instances</b> – {@link #adopt(MessageDef)} and {@link #adopt(TemplateDef)} accept annotation
 *     instances directly.
 *   </li>
 * </ul>
 * <p>
 * All {@code adopt} methods return the adopter instance itself, allowing calls to be chained fluently.
 * <p>
 * Use one of the {@link #getAutoDetected(ConfigurableMessageSupport) getAutoDetected} factory methods to obtain an
 * implementation that is automatically selected based on the available bytecode analysis library on the classpath.
 *
 * @author Jeroen Gremmen
 * @since 0.24.0
 *
 * @see MessageDef
 * @see TemplateDef
 */
public interface AnnotationAdopter
{
  /**
   * Scan the classpath for class files in the given packages and adopt all message and template annotations found.
   * The scan includes directories as well as jar, war and zip archives on the classpath. Each package is resolved
   * using the given {@code classLoader}. Classes that have already been visited are silently skipped.
   *
   * @param classLoader   classloader for locating package resources on the classpath, not {@code null}
   * @param packageNames  package names to scan (e.g. {@code "com.example.messages"}), not {@code null}
   *
   * @return  this annotation adopter instance, never {@code null}
   *
   * @throws MessageAdopterException  if the classpath scan fails
   * @throws MessageParserException   if a message or template text cannot be parsed
   */
  @Contract(value = "_, _ -> this")
  @NotNull AnnotationAdopter adopt(@NotNull ClassLoader classLoader, @NotNull Set<String> packageNames);


  /**
   * Adopt messages and templates from the given class file identified by {@code classFile}. If the class file has
   * already been visited, this method returns immediately without re-parsing.
   *
   * @param classFile  path to the class file to analyze for message annotations, not {@code null}
   *
   * @return  this annotation adopter instance, never {@code null}
   *
   * @throws MessageAdopterException  if the class file cannot be read
   * @throws MessageParserException   if a message or template text cannot be parsed
   */
  @Contract(value = "_ -> this")
  @NotNull AnnotationAdopter adopt(@NotNull Path classFile);


  /**
   * Adopt messages and templates from the given class file identified by {@code classFile}. This method delegates to
   * {@link #adopt(Path)} after converting the {@link File} to a {@link Path}.
   *
   * @param classFile  location of the class file to analyze for message annotations, not {@code null}
   *
   * @return  this annotation adopter instance, never {@code null}
   *
   * @throws MessageAdopterException  if the class file cannot be read
   * @throws MessageParserException   if a message or template text cannot be parsed
   */
  @Contract(value = "_ -> this")
  default @NotNull AnnotationAdopter adopt(@NotNull File classFile)
  {
    adopt(classFile.toPath());
    return this;
  }


  /**
   * Adopt messages and templates from the class file of the given {@code type}. The class file is located via the
   * type's class loader. If the type has already been visited or has no class loader (e.g. bootstrap classes), this
   * method returns immediately.
   *
   * @param type  type to analyze for message annotations, not {@code null}
   *
   * @return  this annotation adopter instance, never {@code null}
   *
   * @throws MessageAdopterException  if the class file cannot be read
   * @throws MessageParserException   if a message or template text cannot be parsed
   */
  @Contract(value = "_ -> this")
  @NotNull AnnotationAdopter adopt(@NotNull Class<?> type);


  /**
   * Publish the message defined in the given {@link MessageDef} annotation. If the annotation contains multiple
   * {@link Text} entries, they are treated as localized variants of the same message. If only a single
   * {@linkplain MessageDef#text() text} is provided, it is used as the sole message text.
   *
   * @param messageDef  {@code MessageDef} annotation, not {@code null}
   *
   * @return  this annotation adopter instance, never {@code null}
   *
   * @throws DuplicateMessageException  if different messages are provided for the same locale
   * @throws MessageParserException     if a message or template text cannot be parsed
   */
  @Contract(value = "_ -> this")
  @NotNull AnnotationAdopter adopt(@NotNull MessageDef messageDef);


  /**
   * Publish the template defined in the given {@link TemplateDef} annotation. If the annotation contains multiple
   * {@link Text} entries, they are treated as localized variants of the same template. If only a single
   * {@linkplain TemplateDef#text() text} is provided, it is used as the sole template text.
   *
   * @param templateDef  {@code TemplateDef} annotation, not {@code null}
   *
   * @return  this annotation adopter instance, never {@code null}
   *
   * @throws DuplicateTemplateException  if different template messages are provided for the same locale
   * @throws MessageParserException      if the template text cannot be parsed
   */
  @Contract(value = "_ -> this")
  @NotNull AnnotationAdopter adopt(@NotNull TemplateDef templateDef);




  /**
   * Create a new {@code AnnotationAdopter} by auto-detecting the best available bytecode analysis library on the
   * classpath. The message factory and message publisher are both obtained from the given
   * {@code configurableMessageSupport}.
   *
   * @param configurableMessageSupport  configurable message support to use for message parsing and publishing,
   *                                    not {@code null}
   *
   * @return  a new annotation adopter instance, never {@code null}
   *
   * @throws de.sayayi.lib.message.exception.MessageAdopterException
   *         if no suitable annotation adopter provider is available
   */
  @Contract(value = "_ -> new", pure = true)
  static @NotNull AnnotationAdopter getAutoDetected(@NotNull ConfigurableMessageSupport configurableMessageSupport)
  {
    return getAutoDetected(
        configurableMessageSupport.getMessageAccessor().getMessageFactory(), configurableMessageSupport);
  }




  /**
   * Create a new {@code AnnotationAdopter} by auto-detecting the best available bytecode analysis library on the
   * classpath. This method allows the message factory and message publisher to be provided independently.
   *
   * @param messageFactory  message factory used for parsing message format strings, not {@code null}
   * @param publisher       message publisher used for publishing parsed messages and templates, not {@code null}
   *
   * @return  a new annotation adopter instance, never {@code null}
   *
   * @throws de.sayayi.lib.message.exception.MessageAdopterException
   *         if no suitable annotation adopter provider is available
   */
  @Contract(value = "_, _ -> new", pure = true)
  static @NotNull AnnotationAdopter getAutoDetected(@NotNull MessageFactory messageFactory,
                                                    @NotNull MessagePublisher publisher) {
    return AnnotationAdopterFactory.getAutoDetected(messageFactory, publisher);
  }
}
