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
package de.sayayi.lib.message.plugin.gradle;

import de.sayayi.lib.message.MessageFactory;
import de.sayayi.lib.message.MessageSupport.MessageSupportAccessor;
import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.GenericFormatterService;
import lombok.val;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static java.nio.file.Files.copy;
import static java.nio.file.Files.*;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public class TestPlugin
{
  @TempDir File testProjectDir;
  File buildDir;
  File javaDir;
  File testPackageDir;
  File packFile;


  @BeforeEach
  void prepareProject() throws IOException
  {
    write(new File(testProjectDir, "settings.gradle").toPath(),
        singletonList("rootProject.name = 'test-message-pack'"));
    write(new File(testProjectDir, "build.gradle").toPath(), Arrays.asList(
        "plugins {",
        "  id 'java'",
        "  id 'de.sayayi.lib.message.plugin.gradle'",
        "}"));

    buildDir = new File(testProjectDir, "build");
    javaDir = new File(testProjectDir, "src/main/java");
    testPackageDir = new File(javaDir, "test");
    packFile = new File(buildDir, "messageFormat/resources/META-INF/message.pack");

    createDirectories(testPackageDir.toPath());
  }


  @Test
  void testNoSources() throws IOException
  {
    val result = GradleRunner.create()
        .withProjectDir(testProjectDir)
        .withArguments("messageFormatPack")
        .withPluginClasspath()
        .withDebug(true)
        .forwardOutput()
        .build();

    assertEquals(SUCCESS, requireNonNull(result.task(":messageFormatPack")).getOutcome());
    assertTrue(packFile.isFile() && packFile.canRead());

    val pack = readMessagePack(packFile);
    assertEquals(emptySet(), pack.getMessageCodes());
    assertEquals(emptySet(), pack.getTemplateNames());
  }


  @Test
  void testWithSource() throws IOException
  {
    copy(getResource("test-source-1.java"),
        new File(testPackageDir, "Source1.java").toPath());

    val result = GradleRunner.create()
        .withProjectDir(testProjectDir)
        .withArguments("messageFormatPack")
        .withPluginClasspath()
        .withDebug(true)
        .forwardOutput()
        .build();

    assertEquals(SUCCESS, requireNonNull(result.task(":messageFormatPack")).getOutcome());
    assertTrue(packFile.isFile() && packFile.canRead());

    val pack = readMessagePack(packFile);
    assertEquals(4, pack.getMessageCodes().size());
    assertEquals(emptySet(), pack.getTemplateNames());
  }


  @Test
  void testWithFilteredSource() throws IOException
  {
    write(new File(testProjectDir, "build.gradle").toPath(), Arrays.asList(
        "plugins {",
        "  id 'java'",
        "  id 'de.sayayi.lib.message.plugin.gradle'",
        "}",
        "messageFormat {",
        "  include '.*INNER.*'",
        "}"));

    copy(getResource("test-source-1.java"),
        new File(testPackageDir, "Source1.java").toPath());

    val result = GradleRunner.create()
        .withProjectDir(testProjectDir)
        .withArguments("messageFormatPack")
        .withPluginClasspath()
        .withDebug(true)
        .forwardOutput()
        .build();

    assertEquals(SUCCESS, requireNonNull(result.task(":messageFormatPack")).getOutcome());
    assertTrue(packFile.isFile() && packFile.canRead());

    val pack = readMessagePack(packFile);
    assertEquals(singleton("MSG-INNER1"), pack.getMessageCodes());
    assertEquals(emptySet(), pack.getTemplateNames());
  }


  private InputStream getResource(String filename) {
    return requireNonNull(getClass().getClassLoader().getResourceAsStream(filename));
  }


  private MessageSupportAccessor readMessagePack(File pack) throws IOException
  {
    val messageSupport = MessageSupportFactory.create(new GenericFormatterService(),
        MessageFactory.NO_CACHE_INSTANCE);

    return messageSupport
        .importMessages(newInputStream(pack.toPath()))
        .getAccessor();
  }
}
