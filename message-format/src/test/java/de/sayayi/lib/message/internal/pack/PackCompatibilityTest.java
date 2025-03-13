package de.sayayi.lib.message.internal.pack;

import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.adopter.PropertiesAdopter;
import de.sayayi.lib.message.formatter.DefaultFormatterService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Properties;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.file.Files.newInputStream;
import static java.nio.file.Files.newOutputStream;


/**
 * @author Jeroen Gremmen
 * @since 0.9.1
 */
@TestMethodOrder(MethodOrderer.DisplayName.class)
@DisplayName("Message pack backward compatibility")
class PackCompatibilityTest
{
  public static void main(String[] args) throws IOException
  {
    var cms = MessageSupportFactory.create(new DefaultFormatterService(), NO_CACHE_INSTANCE);
    var properties = new Properties();
    var adopter =  new PropertiesAdopter(cms);

    properties.load(
        new InputStreamReader(newInputStream(Path.of("src/test/resources/template.properties")), ISO_8859_1));
    adopter.adoptTemplates(properties);

    properties.clear();
    properties.load(
        new InputStreamReader(newInputStream(Path.of("src/test/resources/message.properties")), ISO_8859_1));
    adopter.adopt(properties);

    cms.exportMessages(newOutputStream(Path.of(args[0])));
  }
}
