package de.sayayi.lib.message.pack;

import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.GenericFormatterService;
import lombok.val;
import org.gradle.testkit.runner.GradleRunner;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
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
@DisplayName("Pack backward compatibility test")
public class PackCompatibilityTest
{
  @TempDir File testProjectDir;


  private static Stream<Arguments> pluginVersionParameters()
  {
    return Stream.of(
        Arguments.of("0.8.2", "Version0_8_2.java", "8.2.1", 4, 0),
        Arguments.of("0.8.3", "Version0_8_3.java", "8.3", 4, 0),
        Arguments.of("0.8.3.1", "Version0_8_3_1.java", "8.3", 4, 0)
    );
  }


  @DisplayName("Test compatibility with")
  @ParameterizedTest(name = "version {0} ({3} messages, {4} templates)")
  @MethodSource("pluginVersionParameters")
  void testCompatibility(@NotNull String version, @NotNull String className,
                         @NotNull String gradleVersion, int messageCount, int templateCount)
      throws IOException
  {
    write(new File(testProjectDir, "settings.gradle").toPath(), asList(
        "pluginManagement {",
        "  repositories {",
        "    gradlePluginPortal()",
        "    mavenCentral()",
        "  }",
        "}",
        "rootProject.name = 'test-message-pack'"));

    write(new File(testProjectDir, "gradle.properties").toPath(), singletonList(""));

    write(new File(testProjectDir, "build.gradle").toPath(), asList(
        "plugins {",
        "  id 'java'",
        "  id 'de.sayayi.plugin.gradle.message' version '" + version + "'",
        "}",
        "layout.buildDirectory = '.build'",
        "println 'Gradle version ' + gradle.gradleVersion",
        "messageFormatPack {",
        "  action {",
        "    println 'packing ' + messageCodes.size() + ' messages'",
        "    println 'packing ' + templateNames.size() + ' templates'",
        "  }",
        "}"));

    val testPackageDir = new File(testProjectDir, "src/main/java/test");
    val packFile = new File(testProjectDir, ".build/messageFormatPack/message.pack");

    createDirectories(testPackageDir.toPath());
    copy(requireNonNull(getClass().getClassLoader().getResourceAsStream(className)),
        new File(testPackageDir, className).toPath());

    val result = GradleRunner.create()
        .withGradleVersion(gradleVersion)
        .withProjectDir(testProjectDir)
        .withArguments("messageFormatPack")
        .withDebug(true)
        .forwardOutput()
        .build();
    assertEquals(SUCCESS, requireNonNull(result.task(":messageFormatPack")).getOutcome());

    val messageAccessor = MessageSupportFactory
        .create(new GenericFormatterService(), NO_CACHE_INSTANCE)
        .importMessages(newInputStream(packFile.toPath()))
        .getMessageAccessor();

    assertEquals(messageCount, messageAccessor.getMessageCodes().size());
    assertEquals(templateCount, messageAccessor.getTemplateNames().size());
  }
}
