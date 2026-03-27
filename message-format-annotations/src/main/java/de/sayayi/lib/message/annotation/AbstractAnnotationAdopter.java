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
package de.sayayi.lib.message.annotation;

import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.MessageSupport.ConfigurableMessageSupport;
import de.sayayi.lib.message.MessageSupport.MessagePublisher;
import de.sayayi.lib.message.adopter.AbstractMessageAdopter;
import de.sayayi.lib.message.exception.DuplicateMessageException;
import de.sayayi.lib.message.exception.DuplicateTemplateException;
import de.sayayi.lib.message.exception.MessageAdopterException;
import de.sayayi.lib.message.exception.MessageParserException;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import static de.sayayi.lib.message.util.MessageUtil.validateName;
import static java.nio.file.Files.newInputStream;
import static java.util.Locale.forLanguageTag;
import static java.util.Objects.requireNonNull;


/**
 * Abstract base class for adopting messages and templates defined by {@link MessageDef} and {@link TemplateDef}
 * annotations found in compiled class files.
 * <p>
 * This adopter provides multiple strategies for discovering annotated classes:
 * <ul>
 *   <li>
 *     <b>Classpath scanning</b> – {@link #adopt(ClassLoader, Set)} scans one or more packages for class files and
 *     parses every class found on the classpath (including jar, war and zip archives).
 *   </li>
 *   <li>
 *     <b>Single class file</b> – {@link #adopt(Path)} and {@link #adopt(File)} accept individual class files.
 *   </li>
 *   <li>
 *     <b>Loaded type</b> – {@link #adopt(Class)} uses the class loader of a loaded type to locate its class file.
 *   </li>
 *   <li>
 *     <b>Annotation instances</b> – {@link #adopt(MessageDef)} and {@link #adopt(TemplateDef)} accept synthesized or
 *     mocked annotation instances directly, even though the annotations have
 *     {@linkplain java.lang.annotation.RetentionPolicy#CLASS CLASS} retention.
 *   </li>
 * </ul>
 * <p>
 * Messages are analyzed per class (see {@link #parseClass(InputStream)}). Each class is visited at most once;
 * subsequent calls with the same class are silently ignored. If there is a requirement to select only a subset of the
 * messages provided by a class, the message support must be configured with an appropriate
 * {@link de.sayayi.lib.message.MessageSupport.MessageFilter MessageFilter} or
 * {@link de.sayayi.lib.message.MessageSupport.TemplateFilter TemplateFilter}.
 * <p>
 * Concrete subclasses must implement the {@link #parseClass(InputStream)} method to provide the actual bytecode
 * analysis strategy (e.g. using the ASM library).
 * <p>
 * This class also contains inner record implementations ({@link MessageDefImpl}, {@link TemplateDefImpl} and
 * {@link TextImpl}) that may be used by subclasses to construct annotation instances from parsed bytecode data.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0  (refactored in 0.12.0)
 *
 * @see MessageDef
 * @see TemplateDef
 */
@SuppressWarnings("UnusedReturnValue")
public abstract class AbstractAnnotationAdopter extends AbstractMessageAdopter
{
  private static final Set<String> ZIP_PROTOCOLS = Set.of("zip", "jar", "war");

  private final Set<String> indexedClasses = new HashSet<>();


  /**
   * Create an annotation adopter for the given {@code configurableMessageSupport}. The message factory and message
   * publisher are both obtained from the configurable message support instance.
   *
   * @param configurableMessageSupport  configurable message support, not {@code null}
   */
  protected AbstractAnnotationAdopter(@NotNull ConfigurableMessageSupport configurableMessageSupport) {
    super(configurableMessageSupport);
  }


  /**
   * Create an annotation adopter for the given {@code messageFactory} and {@code publisher}. This constructor allows
   * the message factory and message publisher to be provided independently.
   *
   * @param messageFactory  message factory used for parsing message format strings, not {@code null}
   * @param publisher       message publisher used for publishing parsed messages and templates, not {@code null}
   */
  protected AbstractAnnotationAdopter(@NotNull MessageFactory messageFactory, @NotNull MessagePublisher publisher) {
    super(messageFactory, publisher);
  }


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
      final var url = urls.nextElement();

      if (ZIP_PROTOCOLS.contains(url.getProtocol()))
        adopt_scan_zipEntries(url, classPathPrefix);
      else
      {
        final var directory = url.getFile();
        final var baseDirectory = new File(directory.endsWith(classPathPrefix)
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
      final var baseDirectoryPath = baseDirectory.toPath();

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
    final var con = zipUrl.openConnection();
    final ZipFile zipFile;

    if (con instanceof JarURLConnection)
      zipFile = ((JarURLConnection)con).getJarFile();
    else
    {
      final var urlFile = zipUrl.getFile();
      try {
        var separatorIndex = urlFile.indexOf("*/");
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
        final var zipEntry = entries.nextElement();
        final var classPathName = zipEntry.getName();

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
   * Adopt messages and templates from the given class file identified by {@code classFile}. If the class file has
   * already been visited, this method returns immediately without re-parsing.
   *
   * @param classFile  path to the class file to analyze for message annotations, not {@code null}
   *
   * @return  this annotation adopter instance, never {@code null}
   *
   * @throws MessageAdopterException  if the class file cannot be read
   * @throws MessageParserException   if a message or template text cannot be parsed
   *
   * @since 0.12.0
   */
  @Contract(value = "_ -> this")
  public @NotNull AbstractAnnotationAdopter adopt(@NotNull Path classFile)
  {
    final var classPath = classFile.toAbsolutePath();

    if (!indexedClasses.contains(classPath.toString()))
    {
      try(var inputStream = newInputStream(classPath)) {
        parseClass(inputStream);
        indexedClasses.add(classPath.toString());
      } catch(Exception ex) {
        throw new MessageAdopterException("failed to adopt messages and templates from class file " + classFile, ex);
      }
    }

    return this;
  }


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
  public @NotNull AbstractAnnotationAdopter adopt(@NotNull File classFile)
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
  public @NotNull AbstractAnnotationAdopter adopt(@NotNull Class<?> type)
  {
    final var typeName = type.getName();

    if (!indexedClasses.contains(typeName))
    {
      var classLoader = type.getClassLoader();
      if (classLoader != null)
      {
        final var classResourceName = typeName.replace('.', '/') + ".class";

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
  public @NotNull AbstractAnnotationAdopter adopt(@NotNull MessageDef messageDef)
  {
    final var texts = messageDef.texts();
    final var code = messageDef.code();

    if (texts.length == 0)
      messagePublisher.addMessage(messageFactory.parseMessage(code, messageDef.text()));
    else
    {
      final var localizedTexts = new LinkedHashMap<Locale,String>();

      for(var text: texts)
      {
        final var value = text.locale().isEmpty() && text.text().isEmpty() ? text.value() : text.text();

        localizedTexts.compute(forLanguageTag(text.locale()), (locale,mappedValue) -> {
          if (mappedValue == null || mappedValue.equals(value))
            return value;

          // if a message text differs from the previous definition-> throw
          throw new DuplicateMessageException(code, "different message definition for same locale '" + locale + "'");
        });
      }

      messagePublisher.addMessage(messageFactory.parseMessage(code, localizedTexts));
    }

    return this;
  }


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
  public @NotNull AbstractAnnotationAdopter adopt(@NotNull TemplateDef templateDef)
  {
    final var texts = templateDef.texts();
    final var name = templateDef.name();

    if (texts.length == 0)
      messagePublisher.addTemplate(name, messageFactory.parseTemplate(templateDef.text()));
    else
    {
      final var localizedTexts = new LinkedHashMap<Locale,String>();

      for(var text: texts)
      {
        final var value = text.locale().isEmpty() && text.text().isEmpty() ? text.value() : text.text();

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
   * Parse the contents of a class file provided by {@code classInputStream} and adopt any {@link MessageDef} and
   * {@link TemplateDef} annotations found. Concrete subclasses implement this method to provide the actual bytecode
   * analysis strategy.
   *
   * @param classInputStream  input stream of a class file, not {@code null}
   *
   * @throws IOException  if an I/O error occurs while reading the class file
   */
  protected abstract void parseClass(@NotNull InputStream classInputStream) throws IOException;




  /**
   * Concrete implementation of the {@link MessageDef} annotation interface, used by subclasses to construct
   * {@code MessageDef} instances from parsed bytecode data. The {@code code} is trimmed on construction.
   *
   * @param code   unique message code, not {@code null}
   * @param text   single message text (used when {@code texts} is empty), not {@code null}
   * @param texts  localized text variants, not {@code null}
   */
  @SuppressWarnings("ClassExplicitlyAnnotation")
  protected record MessageDefImpl(@NotNull String code, @Language("MessageFormat") @NotNull String text,
                                  @NotNull Text[] texts) implements MessageDef
  {
    public MessageDefImpl(@NotNull String code, String text, @NotNull Text[] texts) {
      this.code = code.trim();
      this.text = text == null ? "" : text.trim();
      this.texts = texts;
    }


    @Override
    public Class<? extends Annotation> annotationType() {
      return MessageDef.class;
    }


    @Override
    public @NotNull String toString() {
      return "MessageDef(code=" + code + ",text=" + text + ",texts=" + Arrays.toString(texts) + ')';
    }
  }




  /**
   * Concrete implementation of the {@link TemplateDef} annotation interface, used by subclasses to construct
   * {@code TemplateDef} instances from parsed bytecode data. The {@code name} is validated on construction.
   *
   * @param name   unique template name, not {@code null}
   * @param text   single template text (used when {@code texts} is empty), not {@code null}
   * @param texts  localized text variants, not {@code null}
   *
   * @author Jeroen Gremmen
   * @since 0.8.0
   */
  @SuppressWarnings("ClassExplicitlyAnnotation")
  protected record TemplateDefImpl(@NotNull String name, @Language("MessageFormat") @NotNull String text,
                                   @NotNull Text[] texts) implements TemplateDef
  {
    public TemplateDefImpl(@NotNull String name, String text, @NotNull Text[] texts)
    {
      this.name = validateName(name, "template name");
      this.text = text == null ? "" : text.trim();
      this.texts = texts;
    }


    @Override
    public Class<? extends Annotation> annotationType() {
      return TemplateDef.class;
    }


    @Override
    public @NotNull String toString() {
      return "TemplateDef(name=" + name + ",text=" + text + ",texts=" + Arrays.toString(texts) + ')';
    }
  }




  /**
   * Concrete implementation of the {@link Text} annotation interface, used by subclasses to construct {@code Text}
   * instances from parsed bytecode data. All string fields are trimmed on construction.
   *
   * @param locale  locale tag (e.g. {@code "en"} or {@code "de"}), not {@code null}
   * @param text    localized message or template text, not {@code null}
   * @param value   shorthand text value (used when both {@code locale} and {@code text} are empty), not {@code null}
   *
   * @author Jeroen Gremmen
   */
  @SuppressWarnings("ClassExplicitlyAnnotation")
  protected record TextImpl(@NotNull String locale, @Language("MessageFormat") @NotNull String text,
                            @Language("MessageFormat") @NotNull String value) implements Text
  {
    public TextImpl(String locale, String text, String value)
    {
      this.locale = locale == null ? "" : locale.trim();
      this.text = text == null ? "" : text.trim();
      this.value = value == null ? "" : value.trim();
    }


    @Override
    public Class<? extends Annotation> annotationType() {
      return Text.class;
    }


    @Override
    public @NotNull String toString() {
      return "Text(locale=" + locale + ",text=" + text + ",value=" + value + ')';
    }
  }
}
