package de.sayayi.lib.message.pack;

import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.GenericFormatterService;
import lombok.val;
import org.gradle.testkit.runner.GradleRunner;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.stream.Stream;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static java.nio.file.Files.*;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Jeroen Gremmen
 * @since 0.9.1
 */
@TestMethodOrder(MethodOrderer.DisplayName.class)
@DisplayName("Message pack backward compatibility")
class PackCompatibilityTest
{
  @TempDir File testProjectDir;


  private static Stream<Arguments> pluginVersionParameters()
  {
    return Stream.of(
        Arguments.of("0.8.0", "Version0_8_0.java", false, "8.2", 8, 0),
        Arguments.of("0.8.1", "Version0_8_1.java", false, "8.2.1", 9, 1),
        Arguments.of("0.8.1.1", "Version0_8_1_1.java", false, "8.2.1", 9, 1),
        Arguments.of("0.8.2", "Version0_8_2.java", true, "8.2.1", 9, 1),
        Arguments.of("0.8.3", "Version0_8_3.java", true, "8.3", 9, 1),
        Arguments.of("0.8.3.1", "Version0_8_3_1.java", true, "8.3", 9, 1),
        Arguments.of("0.9.0", "Version0_9_0.java", false, "8.4", 9, 1),
        Arguments.of("0.9.1", "Version0_9_1.java", false, "8.4", 9, 1)
        //Arguments.of("0.10.0", "Version0_10_0.java", false, "8.7", 9, 1)
    );
  }


  @DisplayName("Test compatibility with")
  @ParameterizedTest(name = "Version {0}")
  @MethodSource("pluginVersionParameters")
  @EnabledIfSystemProperty(named = "test-pack-compat", matches = "true",
                           disabledReason = "Message pack compatibility tests not required")
  void testCompatibility(@NotNull String version, @NotNull String className, boolean pluginRepo,
                         @NotNull String gradleVersion, int messageCount, int templateCount)
      throws IOException
  {
    writeSettingsGradle(pluginRepo, version);
    writeGradleProperties();
    writeBuildGradle(pluginRepo, version);

    val testPackageDir = new File(testProjectDir, "src/main/java/test");
    createDirectories(testPackageDir.toPath());

    copy(requireNonNull(getClass().getClassLoader().getResourceAsStream(className)),
        new File(testPackageDir, className).toPath());

    val result = GradleRunner.create()
        .withGradleVersion(gradleVersion)
        .withProjectDir(testProjectDir)
        .withArguments("messageFormatPack", "--stacktrace")
        .withDebug(true)
        .forwardOutput()
        .build();
    assertEquals(SUCCESS, requireNonNull(result.task(":messageFormatPack")).getOutcome());

    try(val packStream = newInputStream(
        new File(testProjectDir, ".build/messageFormatPack/message.pack").toPath())) {
      val messageAccessor = MessageSupportFactory
          .create(new GenericFormatterService(), NO_CACHE_INSTANCE)
          .importMessages(packStream)
          .getMessageAccessor();

      assertEquals(messageCount, messageAccessor.getMessageCodes().size());
      assertEquals(templateCount, messageAccessor.getTemplateNames().size());
    }
  }


  private void writeSettingsGradle(boolean pluginRepo, @NotNull String version) throws IOException
  {
    val settingLines = new ArrayList<String>();

    if (pluginRepo)
    {
      settingLines.addAll(asList(
          "pluginManagement {",
          "  repositories {",
          "    gradlePluginPortal()",
          "  }",
          "}"));
    }
    else
    {
      settingLines.addAll(asList(
          "buildscript {",
          "  repositories {",
          "    mavenCentral()",
          "  }",
          "  dependencies {",
          "    classpath 'de.sayayi.lib:message-gradle-plugin:" + version + "'",
          "  }",
          "}"));
    }
    settingLines.add("rootProject.name = 'message-pack-compatibility'");

    write(new File(testProjectDir, "settings.gradle").toPath(), settingLines);
  }


  private void writeGradleProperties() throws IOException {
    write(new File(testProjectDir, "gradle.properties").toPath(), singletonList(""));
  }


  private void writeBuildGradle(boolean pluginRepo, @NotNull String version) throws IOException
  {
    val buildLines = new ArrayList<String>();

    if (pluginRepo)
    {
      buildLines.addAll(asList(
          "plugins {",
          "  id 'java'",
          "  id 'de.sayayi.plugin.gradle.message' version '" + version + "'",
          "}"));
    }
    else
    {
      buildLines.addAll(asList(
          "plugins {",
          "  id 'java'",
          "}",
          "apply plugin: 'de.sayayi.plugin.gradle.message'"));
    }

    buildLines.addAll(asList(
        "layout.buildDirectory = '.build'",
        "println 'Gradle version ' + gradle.gradleVersion",
        "messageFormatPack {",
        "  action {",
        "    println 'packing ' + messageCodes.size() + ' messages'",
        "    println 'packing ' + templateNames.size() + ' templates'",
        "  }",
        "}"
    ));


    write(new File(testProjectDir, "build.gradle").toPath(), buildLines);
  }
}
