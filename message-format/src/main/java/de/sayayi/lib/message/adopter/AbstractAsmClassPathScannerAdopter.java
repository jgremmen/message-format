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
import de.sayayi.lib.message.exception.MessageAdopterException;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import static java.lang.ClassLoader.getSystemClassLoader;
import static java.util.Arrays.asList;


/**
 * <p>
 *   This class provides the basis for Asm based classpath scanners.
 * </p>
 *
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 *
 * @see AnnotationAdopter
 */
abstract class AbstractAsmClassPathScannerAdopter extends AbstractMessageAdopter
    implements ClassPathScannerAdopter
{

  private static final Set<String> ZIP_PROTOCOLS = new HashSet<>(asList("zip", "jar", "war"));

  private final ClassLoader classLoader;
  private final Set<String> packageNames;
  private final Set<String> visitedClasses;
  protected final AnnotationAdopter annotationAdopter;


  protected AbstractAsmClassPathScannerAdopter(@NotNull ConfigurableMessageSupport configurableMessageSupport,
                                               @NotNull Set<String> packageNames, ClassLoader classLoader)
  {
    this(configurableMessageSupport.getAccessor().getMessageFactory(), configurableMessageSupport,
        packageNames, classLoader);
  }


  protected AbstractAsmClassPathScannerAdopter(@NotNull MessageFactory messageFactory,
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


  @Override
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
              parseClass(classInputStream);
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
            parseClass(classInputStream);
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


  /**
   * Scan contents of class provided by {@code classInputStream}.
   *
   * @param classInputStream  input stream of class file, not {@code null}
   *
   * @throws IOException  in case of an I/O exception
   */
  protected abstract void parseClass(@NotNull InputStream classInputStream) throws IOException;
}
