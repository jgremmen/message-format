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
package de.sayayi.lib.message.adopter;

import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.MessageSupport.ConfigurableMessageSupport;
import de.sayayi.lib.message.MessageSupport.MessagePublisher;
import de.sayayi.lib.message.annotation.MessageDef;
import de.sayayi.lib.message.annotation.TemplateDef;
import de.sayayi.lib.message.annotation.Text;
import de.sayayi.lib.message.exception.DuplicateMessageException;
import de.sayayi.lib.message.exception.DuplicateTemplateException;
import de.sayayi.lib.message.exception.MessageAdopterException;
import de.sayayi.lib.message.exception.MessageParserException;
import de.sayayi.lib.message.internal.EmptyMessage;
import de.sayayi.lib.message.internal.EmptyMessageWithCode;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import static java.nio.file.Files.newInputStream;
import static java.util.Locale.forLanguageTag;
import static java.util.Objects.requireNonNull;


/**
 * This abstract class defines various methods for adopting messages and templates defined by
 * annotations.
 * <p>
 * Messages are analysed per class (see {@link #parseClass(InputStream)}). If there is a
 * requirement to select a part of the messages provided by a class, the message support must
 * be configured with an appropriate
 * {@link de.sayayi.lib.message.MessageSupport.MessageFilter MessageFilter} or
 * {@link de.sayayi.lib.message.MessageSupport.TemplateFilter TemplateFilter}.
 * <p>
 * Even though the annotations all have class retention, 2 adopt methods
 * ({@link #adopt(MessageDef)} and {@link #adopt(TemplateDef)}) are available to analyse
 * synthesized/mocked annotations.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
@SuppressWarnings({"UnusedReturnValue", "UnknownLanguage"})
public abstract class AbstractAnnotationAdopter extends AbstractMessageAdopter
{
  private static final Set<String> ZIP_PROTOCOLS = Set.of("zip", "jar", "war");

  private final Set<String> indexedClasses = new HashSet<>();


  /**
   * Create an annotation adopter for the given {@code configurableMessageSupport}.
   *
   * @param configurableMessageSupport  configurable message support, not {@code null}
   */
  protected AbstractAnnotationAdopter(@NotNull ConfigurableMessageSupport configurableMessageSupport) {
    super(configurableMessageSupport);
  }


  /**
   * Create an annotation adopter for the given {@code messageFactory} and {@code publisher}.
   *
   * @param messageFactory  message factory, not {@code null}
   * @param publisher       message publisher, not {@code null}
   */
  protected AbstractAnnotationAdopter(@NotNull MessageFactory messageFactory, @NotNull MessagePublisher publisher) {
    super(messageFactory, publisher);
  }


  /**
   * Scan the classpath (with the given packages) for message annotations and adopt them.
   *
   * @param classLoader   classloader for locating classes, not {@code null}
   * @param packageNames  package names to scan, not {@code null}
   *
   * @return  this annotation adopter, never {@code null}
   *
   * @throws MessageParserException  in case the template could not be parsed
   */
  public @NotNull AbstractAnnotationAdopter adopt(@NotNull ClassLoader classLoader, @NotNull Set<String> packageNames)
  {
    try {
      for(var packageName: packageNames)
        adopt_scan(classLoader, packageName);
    } catch(Exception ex) {
      throw new MessageAdopterException("failed to scan class path for messages and templates", ex);
    }

    return this;
  }


  private void adopt_scan(@NotNull ClassLoader classLoader, @NotNull String packageName) throws Exception
  {
    var classPathPrefix = packageName.replace('.', '/');
    if (!classPathPrefix.endsWith("/"))
      classPathPrefix = classPathPrefix + '/';

    for(var urls = classLoader.getResources(classPathPrefix); urls.hasMoreElements();)
    {
      var url = urls.nextElement();

      if (ZIP_PROTOCOLS.contains(url.getProtocol()))
        adopt_scan_zipEntries(url, classPathPrefix);
      else
      {
        var directory = url.getFile();
        var baseDirectory = new File(directory.endsWith(classPathPrefix)
            ? directory.substring(0, directory.length() - classPathPrefix.length()) : directory);

        if (baseDirectory.isDirectory())
          adopt_scan_directory(baseDirectory, new File(directory));
      }
    }
  }


  private void adopt_scan_directory(@NotNull File baseDirectory, @NotNull File directory) throws IOException
  {
    var files = directory.listFiles();
    if (files != null)
    {
      var baseDirectoryPath = baseDirectory.toPath();

      for(var file: files)
        if (file.isDirectory())
          adopt_scan_directory(baseDirectory, file);
        else
        {
          var classNamePath = baseDirectoryPath
              .relativize(file.toPath()).toString().replace('\\', '/');

          if (classNamePath.endsWith(".class") && scan_checkVisited(classNamePath))
          {
            try(var classInputStream = newInputStream(file.toPath())) {
              parseClass(classInputStream);
            }
          }
        }
    }
  }


  private void adopt_scan_zipEntries(@NotNull URL zipUrl, @NotNull String classPathPrefix) throws IOException
  {
    var con = zipUrl.openConnection();
    final ZipFile zipFile;

    if (con instanceof JarURLConnection)
      zipFile = ((JarURLConnection)con).getJarFile();
    else
    {
      var urlFile = zipUrl.getFile();
      try {
        int separatorIndex = urlFile.indexOf("*/");
        if (separatorIndex == -1)
          separatorIndex = urlFile.indexOf("!/");

        zipFile = adopt_scan_createZipFileFromUrl(separatorIndex != -1
            ? urlFile.substring(0, separatorIndex) : urlFile);
      } catch(ZipException ex) {
        return;
      }
    }

    try {
      for(var entries = zipFile.entries(); entries.hasMoreElements();)
      {
        var zipEntry = entries.nextElement();
        var classPathName = zipEntry.getName();

        if (classPathName.endsWith(".class") &&
            classPathName.startsWith(classPathPrefix) &&
            scan_checkVisited(classPathName))
        {
          try(var classInputStream = zipFile.getInputStream(zipEntry)) {
            parseClass(classInputStream);
          }
        }
      }
    } finally {
      zipFile.close();
    }
  }


  private @NotNull ZipFile adopt_scan_createZipFileFromUrl(@NotNull String zipFileUrl) throws IOException
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
    return indexedClasses.add(classPathName);
  }


  /**
   * Adopt messages for the given {@code classFile}.
   *
   * @param classFile  location of the class file to analyse for messages, not {@code null}
   *
   * @return  this annotation adopter instance, never {@code null}
   *
   * @throws MessageParserException  in case the template could not be parsed
   */
  public @NotNull AbstractAnnotationAdopter adopt(@NotNull File classFile)
  {
    var classPath = classFile.toPath().toAbsolutePath();

    if (!indexedClasses.contains(classPath.toString()))
    {
      try(var inputStream = Files.newInputStream(classPath)) {
        parseClass(inputStream);
        indexedClasses.add(classPath.toString());
      } catch(Exception ex) {
        throw new MessageAdopterException("failed to adopt messages and templates from class file " + classFile, ex);
      }
    }

    return this;
  }


  /**
   * Adopt messages for the given {@code type}.
   *
   * @param type  type to analyse for messages, not {@code null}
   *
   * @return  this annotation adopter instance, never {@code null}
   *
   * @throws MessageParserException  in case the template could not be parsed
   */
  public @NotNull AbstractAnnotationAdopter adopt(@NotNull Class<?> type)
  {
    var typeName = type.getName();

    if (!indexedClasses.contains(typeName))
    {
      var classLoader = type.getClassLoader();
      if (classLoader != null)
      {
        var classResourceName = typeName.replace('.', '/') + ".class";

        try(var inputStream = classLoader.getResourceAsStream(classResourceName)) {
          parseClass(requireNonNull(inputStream));
          indexedClasses.add(typeName);
        } catch(Exception ex) {
          throw new MessageAdopterException("failed to adopt messages and templates from type " + typeName, ex);
        }
      }
    }

    return this;
  }


  /**
   * Publish the message defined in the given {@link MessageDef} annotation.
   *
   * @param messageDef  {@code MessageDef} annotation, not {@code null}
   *
   * @return  this annotation adopter instance, never {@code null}
   *
   * @throws DuplicateMessageException  if different messages are provided for the same locale
   * @throws MessageParserException     in case the template could not be parsed
   */
  public @NotNull AbstractAnnotationAdopter adopt(@NotNull MessageDef messageDef)
  {
    var texts = messageDef.texts();
    var code = messageDef.code();

    if (texts.length == 0)
    {
      @Language("MessageFormat") var text = messageDef.text();

      messagePublisher.addMessage(text.isEmpty()
          ? new EmptyMessageWithCode(code)
          : messageFactory.parseMessage(code, text));
    }
    else
    {
      var localizedTexts = new LinkedHashMap<Locale,String>();

      for(var text: texts)
      {
        var value = text.locale().isEmpty() && text.text().isEmpty() ? text.value() : text.text();

        localizedTexts.compute(forLanguageTag(text.locale()), (locale,mappedValue) -> {
          if (mappedValue == null || mappedValue.equals(value))
            return value;

          // if message text differs from previous definition -> throw
          throw new DuplicateMessageException(code, "different message definition for same locale '" + locale + "'");
        });
      }

      messagePublisher.addMessage(messageFactory.parseMessage(code, localizedTexts));
    }

    return this;
  }


  /**
   * Publish the template defined in the given {@link TemplateDef} annotation.
   *
   * @param templateDef  {@code TemplateDef} annotation, not {@code null}
   *
   * @return  this annotation adopter instance, never {@code null}
   *
   * @throws DuplicateTemplateException  if different template messages are provided for the
   *                                     same locale
   * @throws MessageParserException      in case the template could not be parsed
   */
  public @NotNull AbstractAnnotationAdopter adopt(@NotNull TemplateDef templateDef)
  {
    var texts = templateDef.texts();
    var name = templateDef.name();

    if (texts.length == 0)
    {
      @Language("MessageFormat") var text = templateDef.text();

      messagePublisher.addTemplate(name, text.isEmpty()
          ? EmptyMessage.INSTANCE
          : messageFactory.parseTemplate(text));
    }
    else
    {
      var localizedTexts = new LinkedHashMap<Locale,String>();

      for(var text: texts)
      {
        var value = text.locale().isEmpty() && text.text().isEmpty() ? text.value() : text.text();

        localizedTexts.compute(forLanguageTag(text.locale()), (locale, mappedValue) -> {
          if (mappedValue == null || mappedValue.equals(value))
            return value;

          // if template text differs from previous definition -> throw
          throw new DuplicateTemplateException(name, "different template definition for same locale '" + locale + "'");
        });
      }

      try {
        messagePublisher.addTemplate(name, messageFactory.parseTemplate(localizedTexts));
      } catch(MessageParserException ex) {
        throw ex.withTemplate(name);
      }
    }

    return this;
  }


  /**
   * Scan contents of class provided by {@code classInputStream}.
   *
   * @param classInputStream  input stream of class file, not {@code null}
   *
   * @throws IOException  in case of an I/O exception
   */
  protected abstract void parseClass(@NotNull InputStream classInputStream) throws IOException;




  /**
   * {@code MessageDef} annotation implementation.
   */
  @SuppressWarnings("ClassExplicitlyAnnotation")
  static final class MessageDefImpl implements MessageDef
  {
    private final @NotNull String code;
    @Language("MessageFormat")
    private final @NotNull String text;
    private final @NotNull Text[] texts;


    MessageDefImpl(@NotNull String code, String text, @NotNull Text[] texts)
    {
      this.code = code.trim();
      this.text = text == null ? "" : text.trim();
      this.texts = texts;
    }


    @Override
    public String code() {
      return code;
    }


    @Override
    public String text() {
      return text;
    }


    @Override
    public Text[] texts() {
      return texts;
    }


    @Override
    public Class<? extends Annotation> annotationType() {
      return MessageDef.class;
    }


    @Override
    public String toString() {
      return "MessageDef(code=" + code + ",text=" + text + ",texts=" + Arrays.toString(texts) + ')';
    }
  }




  /**
   *  {@code TemplateDef} annotation implementation.
   *
   * @author Jeroen Gremmen
   * @since 0.8.0
   */
  @SuppressWarnings("ClassExplicitlyAnnotation")
  static final class TemplateDefImpl implements TemplateDef
  {
    private final @NotNull String name;
    @Language("MessageFormat")
    private final @NotNull String text;
    private final @NotNull Text[] texts;


    @SuppressWarnings("ConstantValue")
    TemplateDefImpl(@NotNull String name, String text, @NotNull Text[] texts)
    {
      if ((this.name = name == null ? "" : name.trim()).isEmpty())
        throw new IllegalArgumentException("name must not be empty");

      this.text = text == null ? "" : text.trim();
      this.texts = texts;
    }


    @Override
    public String name() {
      return name;
    }


    @Override
    public String text() {
      return text;
    }


    @Override
    public Text[] texts() {
      return texts;
    }


    @Override
    public Class<? extends Annotation> annotationType() {
      return TemplateDef.class;
    }


    @Override
    public String toString() {
      return "TemplateDef(name=" + name + ",text=" + text + ",texts=" + Arrays.toString(texts) + ')';
    }
  }




  /**
   * {@code Text} annotation implementation.
   *
   * @author Jeroen Gremmen
   */
  @SuppressWarnings("ClassExplicitlyAnnotation")
  static final class TextImpl implements Text
  {
    private final @NotNull String locale;
    @Language("MessageFormat")
    private final @NotNull String text;
    @Language("MessageFormat")
    private final @NotNull String value;


    TextImpl(String locale, String text, String value)
    {
      this.locale = locale == null ? "" : locale.trim();
      this.text = text == null ? "" : text.trim();
      this.value = value == null ? "" : value.trim();
    }


    @Override
    public String locale() {
      return locale;
    }


    @Override
    public String text() {
      return text;
    }


    @Override
    public String value() {
      return value;
    }


    @Override
    public Class<? extends Annotation> annotationType() {
      return Text.class;
    }


    @Override
    public String toString() {
      return "Text(locale=" + locale + ",text=" + text + ",value=" + value + ')';
    }
  }
}
