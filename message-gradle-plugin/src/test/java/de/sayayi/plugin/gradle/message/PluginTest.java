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
import lombok.val;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;

import static java.nio.file.Files.*;
import static java.util.Objects.requireNonNull;
import static org.gradle.testkit.runner.TaskOutcome.*;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests {@link MessageFormatPlugin}: pack task runs with no, plain and filtered sources, duplicate message handling,
 * duplicate strategy formats, plugin order and the packaged jar.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
@DisplayName("Message format Gradle plugin")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class PluginTest
{
  private static String MESSAGE_FORMAT_ANNOTATIONS_JAR;

  @TempDir
  private File testProjectDir;

  private File testPackageDir;
  private File packFile;


  @BeforeAll
  static void prepareExternalJar() throws IOException
  {
    try(var is = PluginTest.class.getClassLoader().getResourceAsStream("external-jars.properties")) {
      var props = new Properties();

      props.load(is);

      MESSAGE_FORMAT_ANNOTATIONS_JAR = props.getProperty("message-format-annotations");
    }
  }


  @BeforeEach
  void prepareProject() throws IOException
  {
    write(new File(testProjectDir, "settings.gradle").toPath(),
        List.of(
            "rootProject.name = 'test-message-pack'",
            // isolate the build cache, so cache entries of other tests or test runs are not reused
            "buildCache { local { directory = file('build-cache') } }"));

    write(new File(testProjectDir, "gradle.properties").toPath(), List.of(""));

    writeBuildGradle(List.of());

    testPackageDir = new File(testProjectDir, "src/main/java/test");
    packFile = new File(testProjectDir, "build/messageFormatPack/test-message-pack.mfp");

    createDirectories(testPackageDir.toPath());
  }


  @Test
  @DisplayName("Pack task without sources")
  void testNoSources() throws IOException
  {
    val result = runner("messageFormatPack")
        .withDebug(true)
        .build();

    val pack = assertPackTaskSucceeded(result);

    assertEquals(Set.of(), pack.getMessageCodes());
    assertEquals(Set.of(), pack.getTemplateNames());
  }


  @Test
  @DisplayName("Pack task with a single class")
  void testWithSource() throws IOException
  {
    copy(getResource("test-source-1.java"),
        new File(testPackageDir, "Source1.java").toPath());

    val result = runner("messageFormatPack")
        .withDebug(true)
        .build();

    val pack = assertPackTaskSucceeded(result);

    assertEquals(4, pack.getMessageCodes().size());
    assertEquals(Set.of(), pack.getTemplateNames());
  }


  @Test
  @DisplayName("Pack task with filtered message codes")
  void testWithFilteredSource() throws IOException
  {
    writeBuildGradle(List.of(
        "messageFormat {",
        "  messages {",
        "    include '.*INNER.*'",
        "  }",
        "}"
    ));

    copy(getResource("test-source-1.java"),
        new File(testPackageDir, "Source1.java").toPath());

    val result = runner("messageFormatPack")
        .withDebug(true)
        .build();

    val pack = assertPackTaskSucceeded(result);

    assertEquals(Set.of("MSG-INNER1"), pack.getMessageCodes());
    assertEquals(Set.of(), pack.getTemplateNames());
  }


  @Test
  @DisplayName("Pack task with filtered message codes configured after task realization")
  void testWithFilteredSourceAfterRealization() throws IOException
  {
    writeBuildGradle(List.of(
        "jar {",
        "  from messageFormatPack {",
        "    into 'META-INF'",
        "  }",
        "}",
        "messageFormat {",
        "  messages {",
        "    include '.*INNER.*'",
        "  }",
        "}"
    ));

    copy(getResource("test-source-1.java"),
        new File(testPackageDir, "Source1.java").toPath());

    val result = runner("messageFormatPack")
        .withDebug(true)
        .build();

    val pack = assertPackTaskSucceeded(result);

    assertEquals(Set.of("MSG-INNER1"), pack.getMessageCodes());
    assertEquals(Set.of(), pack.getTemplateNames());
  }


  @Test
  @DisplayName("Pack task with message code filters from extension and task")
  void testWithFilteredSourceFromExtensionAndTask() throws IOException
  {
    writeBuildGradle(List.of(
        "messageFormatPack {",
        "  messages {",
        "    include 'MSG1'",
        "  }",
        "}",
        "messageFormat {",
        "  messages {",
        "    include '.*INNER.*'",
        "  }",
        "}"
    ));

    copy(getResource("test-source-1.java"),
        new File(testPackageDir, "Source1.java").toPath());

    val result = runner("messageFormatPack")
        .withDebug(true)
        .build();

    val pack = assertPackTaskSucceeded(result);

    assertEquals(Set.of("MSG1", "MSG-INNER1"), pack.getMessageCodes());
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
    writeBuildGradle(List.of("messageFormat.messages.duplicateStrategy = '" + duplicateMsgStrategy + "'"));

    copy(getResource("test-source-1.java"),
        new File(testPackageDir, "Source1.java").toPath());
    copy(getResource("test-source-2.java"),
        new File(testPackageDir, "Source2.java").toPath());

    val runner = runner("messageFormatPack")
        .withDebug(true);
    val result = success
        ? runner.build()
        : runner.buildAndFail();

    assertEquals(success ? SUCCESS : FAILED,
        requireNonNull(result.task(":messageFormatPack")).getOutcome());
  }


  @Test
  @DisplayName("Plugin applied before the java plugin")
  void testAppliedBeforeJavaPlugin() throws IOException
  {
    write(new File(testProjectDir, "build.gradle").toPath(), List.of(
        "plugins {",
        "  id 'de.sayayi.plugin.gradle.message'",
        "  id 'java'",
        "}",
        "dependencies {",
        "  implementation files('" + MESSAGE_FORMAT_ANNOTATIONS_JAR + "')",
        "}"
    ));

    copy(getResource("test-source-1.java"),
        new File(testPackageDir, "Source1.java").toPath());

    val result = runner("messageFormatPack")
        .withDebug(true)
        .build();

    assertFalse(assertPackTaskSucceeded(result).getMessageCodes().isEmpty());
  }


  @Test
  @DisplayName("Equal duplicate strategies in different formats keep the pack task up-to-date")
  void testDuplicateStrategyFormats() throws IOException
  {
    copy(getResource("test-source-1.java"),
        new File(testPackageDir, "Source1.java").toPath());

    for(var duplicateStrategy: List.of("'fail'", "'FAIL'", "\"${'fail'}\"",
                                       "de.sayayi.plugin.gradle.message.DuplicateStrategy.FAIL"))
    {
      writeBuildGradle(List.of("messageFormat.messages.duplicateStrategy = " + duplicateStrategy));

      val result = runner("messageFormatPack").build();

      assertEquals(duplicateStrategy.equals("'fail'") ? SUCCESS : UP_TO_DATE,
          requireNonNull(result.task(":messageFormatPack")).getOutcome(), duplicateStrategy);
    }
  }


  @Test
  @DisplayName("Unknown duplicate strategy")
  void testUnknownDuplicateStrategy() throws IOException
  {
    writeBuildGradle(List.of("messageFormat.messages.duplicateStrategy = 'unknown'"));

    val result = runner("messageFormatPack").buildAndFail();

    assertTrue(result.getOutput().contains("Unknown duplicates strategy: unknown"));
  }


  @Test
  @DisplayName("Invalid regular expression")
  void testInvalidRegex() throws IOException
  {
    writeBuildGradle(List.of("messageFormat.messages.include '[invalid'"));

    copy(getResource("test-source-1.java"),
        new File(testPackageDir, "Source1.java").toPath());

    val result = runner("messageFormatPack").buildAndFail();

    assertTrue(result.getOutput().contains("Invalid include regular expression '[invalid'"));
  }


  @Test
  @DisplayName("Pack task with custom action is neither up-to-date nor loaded from the build cache")
  void testActionAlwaysRuns() throws IOException
  {
    writeBuildGradle(List.of(
        "messageFormatPack {",
        "  action { println \"Scanned message codes: ${it.messageCodes.size()}\" }",
        "}"
    ));

    copy(
        getResource("test-source-1.java"),
        new File(testPackageDir, "Source1.java").toPath());

    for(var arguments: List.of(
        List.of("messageFormatPack", "--build-cache"),
        List.of("messageFormatPack", "--build-cache"),
        List.of("clean", "messageFormatPack", "--build-cache")))
    {
      val result = runner(arguments.toArray(String[]::new)).build();

      assertEquals(SUCCESS, requireNonNull(result.task(":messageFormatPack")).getOutcome(), arguments.toString());
      assertTrue(result.getOutput().contains("Scanned message codes: 4"), arguments.toString());
    }
  }


  @Test
  @DisplayName("Pack task without custom action is loaded from the build cache")
  void testNoActionFromCache() throws IOException
  {
    copy(
        getResource("test-source-1.java"),
        new File(testPackageDir, "Source1.java").toPath());

    assertEquals(
        SUCCESS,
        requireNonNull(runner("messageFormatPack", "--build-cache")
            .build()
            .task(":messageFormatPack"))
            .getOutcome());
    assertEquals(
        FROM_CACHE,
        requireNonNull(runner("clean", "messageFormatPack", "--build-cache")
            .build()
            .task(":messageFormatPack"))
            .getOutcome());
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

    val result = runner("jar")
        .withDebug(true)
        .build();

    assertEquals(SUCCESS, requireNonNull(result.task(":jar")).getOutcome());
    assertEquals(SUCCESS, requireNonNull(result.task(":messageFormatPack")).getOutcome());
  }


  @Contract(pure = true)
  private @NotNull GradleRunner runner(@NotNull String... arguments)
  {
    return GradleRunner
        .create()
        .withProjectDir(testProjectDir)
        .withArguments(arguments)
        .withPluginClasspath()
        .forwardOutput();
  }


  /**
   * Asserts that the pack task succeeded and wrote a readable pack file, and returns the messages of the pack.
   */
  private @NotNull MessageAccessor assertPackTaskSucceeded(@NotNull BuildResult result) throws IOException
  {
    assertEquals(SUCCESS, requireNonNull(result.task(":messageFormatPack")).getOutcome());
    assertTrue(packFile.isFile() && packFile.canRead());

    return readMessagePack(packFile);
  }


  @Contract(pure = true)
  private InputStream getResource(@NotNull String filename) {
    return requireNonNull(getClass().getClassLoader().getResourceAsStream(filename));
  }


  @Contract(pure = true)
  private @NotNull MessageAccessor readMessagePack(@NotNull File pack) throws IOException
  {
    return MessageSupportFactory
        .createGeneric()
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
        "  implementation files('" + MESSAGE_FORMAT_ANNOTATIONS_JAR + "')",
        "}"
    ));

    gradleLines.addAll(lines);

    write(new File(testProjectDir, "build.gradle").toPath(), gradleLines);
  }
}
