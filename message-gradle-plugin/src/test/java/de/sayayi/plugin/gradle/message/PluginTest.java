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
package de.sayayi.plugin.gradle.message;

import de.sayayi.lib.message.MessageSupport.MessageAccessor;
import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.GenericFormatterService;
import lombok.val;
import org.gradle.testkit.runner.GradleRunner;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static java.nio.file.Files.*;
import static java.util.Objects.requireNonNull;
import static org.gradle.testkit.runner.TaskOutcome.FAILED;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
@DisplayName("Message format Gradle plugin")
class PluginTest
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
        List.of("rootProject.name = 'test-message-pack'"));

    write(new File(testProjectDir, "gradle.properties").toPath(), List.of(""));

    writeBuildGradle(List.of());

    buildDir = new File(testProjectDir, "build");
    javaDir = new File(testProjectDir, "src/main/java");
    testPackageDir = new File(javaDir, "test");
    packFile = new File(buildDir, "messageFormatPack/message.pack");

    createDirectories(testPackageDir.toPath());
  }


  @Test
  @DisplayName("Pack task without sources")
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
    assertEquals(Set.of(), pack.getMessageCodes());
    assertEquals(Set.of(), pack.getTemplateNames());
  }


  @Test
  @DisplayName("Pack task with a single class")
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
    assertEquals(Set.of(), pack.getTemplateNames());
  }


  @Test
  @DisplayName("Pack task with filtered message codes")
  void testWithFilteredSource() throws IOException
  {
    writeBuildGradle(List.of(
        "messageFormat {",
        "  include '.*INNER.*'",
        "}"
    ));

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
    assertEquals(Set.of("MSG-INNER1"), pack.getMessageCodes());
    assertEquals(Set.of(), pack.getTemplateNames());
  }


  private static Stream<Arguments> testDuplicateMessage_parameters()
  {
    return Stream.of(
        Arguments.of("ignore", true),
        Arguments.of("ignore-and-warn", true),
        Arguments.of("override", true),
        Arguments.of("override-and-warn", true),
        Arguments.of("fail", false)
    );
  }


  @DisplayName("Pack task with duplicate strategy")
  @ParameterizedTest(name = "Strategy: {0}")
  @MethodSource("testDuplicateMessage_parameters")
  void testDuplicateMessage(@NotNull String duplicateMsgStrategy, boolean success)
      throws IOException
  {
    writeBuildGradle(List.of("messageFormat.duplicateMsgStrategy = '" + duplicateMsgStrategy + "'"));

    copy(getResource("test-source-1.java"),
        new File(testPackageDir, "Source1.java").toPath());
    copy(getResource("test-source-2.java"),
        new File(testPackageDir, "Source2.java").toPath());

    val runner = GradleRunner.create()
        .withProjectDir(testProjectDir)
        .withArguments("messageFormatPack")
        .withPluginClasspath()
        .withDebug(true)
        .forwardOutput();
    val result = success
        ? runner.build()
        : runner.buildAndFail();

    assertEquals(success ? SUCCESS : FAILED,
        requireNonNull(result.task(":messageFormatPack")).getOutcome());
  }


  @Test
  @DisplayName("Jar task with messageFormatPack dependency")
  void testJar() throws IOException
  {
    writeBuildGradle(List.of(
        "jar {",
        "  from messageFormatPack {",
        "    into 'META-INF'",
        "  }",
        "}"
    ));

    copy(getResource("test-source-1.java"),
        new File(testPackageDir, "Source1.java").toPath());
    copy(getResource("test-source-2.java"),
        new File(testPackageDir, "Source2.java").toPath());

    val result = GradleRunner.create()
        .withProjectDir(testProjectDir)
        .withArguments("jar")
        .withPluginClasspath()
        .withDebug(true)
        .forwardOutput()
        .build();

    assertEquals(SUCCESS, requireNonNull(result.task(":jar")).getOutcome());
    assertEquals(SUCCESS, requireNonNull(result.task(":messageFormatPack")).getOutcome());
  }


  @Contract(pure = true)
  private InputStream getResource(@NotNull String filename) {
    return requireNonNull(getClass().getClassLoader().getResourceAsStream(filename));
  }


  @Contract(pure = true)
  private @NotNull MessageAccessor readMessagePack(@NotNull File pack) throws IOException
  {
    val messageSupport = MessageSupportFactory
        .create(new GenericFormatterService(), NO_CACHE_INSTANCE);

    return messageSupport
        .importMessages(newInputStream(pack.toPath()))
        .getMessageAccessor();
  }


  private void writeBuildGradle(List<String> lines) throws IOException
  {
    var gradleLines = new ArrayList<>(List.of(
        "plugins {",
        "  id 'java'",
        "  id 'de.sayayi.plugin.gradle.message'",
        "}",
        "dependencies {",
        "  implementation files('" + System.getProperty("MFA_JAR") + "')",
        "}"
    ));

    gradleLines.addAll(lines);

    write(new File(testProjectDir, "build.gradle").toPath(), gradleLines);
  }
}
