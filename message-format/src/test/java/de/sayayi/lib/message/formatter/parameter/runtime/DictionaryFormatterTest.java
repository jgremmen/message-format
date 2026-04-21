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
package de.sayayi.lib.message.formatter.parameter.runtime;

import de.sayayi.lib.message.MessageSupport;
import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.parameter.named.StringFormatter;
import de.sayayi.lib.message.internal.part.parameter.AbstractFormatterTest;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Jeroen Gremmen
 * @since 0.21.0
 */
@DisplayName("Dictionary formatter")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class DictionaryFormatterTest extends AbstractFormatterTest
{
  private final MessageSupport messageSupport = MessageSupportFactory
      .create(createFormatterService(
          new DictionaryFormatter(), new MapFormatter(), new IterableFormatter(), new StringFormatter()),
          NO_CACHE_INSTANCE);


  @Test
  @DisplayName("Formattable types")
  void testFormattableTypes() {
    assertFormatterForType(new DictionaryFormatter(), Dictionary.class);
  }


  @Test
  @DisplayName("Hashtable without key config formats entries")
  void testHashtableEntries()
  {
    val ht = new Hashtable<String,String>();
    ht.put("color", "red");
    ht.put("shape", "circle");

    val result = messageSupport.message("%{ht}").with("ht", ht).format();

    assertTrue(result.contains("color=red"), "expected 'color=red' in: " + result);
    assertTrue(result.contains("shape=circle"), "expected 'shape=circle' in: " + result);
  }


  @Test
  @DisplayName("Hashtable with key config returns value")
  void testHashtableKeyLookup()
  {
    val ht = new Hashtable<String,String>();
    ht.put("color", "red");
    ht.put("shape", "circle");

    assertEquals("red", messageSupport.message("%{ht,key:'color'}").with("ht", ht).format());
    assertEquals("circle", messageSupport.message("%{ht,key:'shape'}").with("ht", ht).format());
  }


  @Test
  @DisplayName("Hashtable with missing key returns empty")
  void testHashtableMissingKey()
  {
    val ht = new Hashtable<String,String>();
    ht.put("color", "red");

    assertEquals("", messageSupport.message("%{ht,key:'missing'}").with("ht", ht).format());
  }


  @Test
  @DisplayName("Properties without key config formats entries")
  void testPropertiesEntries()
  {
    val props = new Properties();
    props.setProperty("host", "localhost");
    props.setProperty("port", "8080");

    val result = messageSupport.message("%{p}").with("p", props).format();

    assertTrue(result.contains("host=localhost"), "expected 'host=localhost' in: " + result);
    assertTrue(result.contains("port=8080"), "expected 'port=8080' in: " + result);
  }


  @Test
  @DisplayName("Properties with key config returns value")
  void testPropertiesKeyLookup()
  {
    val props = new Properties();
    props.setProperty("host", "localhost");
    props.setProperty("port", "8080");

    assertEquals("localhost", messageSupport.message("%{p,key:'host'}").with("p", props).format());
    assertEquals("8080", messageSupport.message("%{p,key:'port'}").with("p", props).format());
  }


  @Test
  @DisplayName("Custom Dictionary without key config uses toString")
  void testCustomDictionaryNoKey()
  {
    assertEquals("MyDictionary", messageSupport
        .message("%{d}").with("d", new MyDictionary()).format());
  }


  @Test
  @DisplayName("Custom Dictionary with key config returns value")
  void testCustomDictionaryKeyLookup()
  {
    assertEquals("apple", messageSupport
        .message("%{d,key:'fruit'}").with("d", new MyDictionary()).format());
    assertEquals("carrot", messageSupport
        .message("%{d,key:'veggie'}").with("d", new MyDictionary()).format());
  }


  @Test
  @DisplayName("Custom Dictionary with missing key returns empty")
  void testCustomDictionaryMissingKey()
  {
    assertEquals("", messageSupport
        .message("%{d,key:'missing'}").with("d", new MyDictionary()).format());
  }




  private static final class MyDictionary extends Dictionary<String,String>
  {
    @Override
    public String get(Object key)
    {
      return switch((String)key) {
        case "fruit" -> "apple";
        case "veggie" -> "carrot";
        default -> null;
      };
    }

    @Override public String toString() { return "MyDictionary"; }
    @Override public int size() { throw new UnsupportedOperationException(); }
    @Override public boolean isEmpty() { throw new UnsupportedOperationException(); }
    @Override public Enumeration<String> keys() { throw new UnsupportedOperationException(); }
    @Override public Enumeration<String> elements() { throw new UnsupportedOperationException(); }
    @Override public String put(String k, String v) { throw new UnsupportedOperationException(); }
    @Override public String remove(Object k) { throw new UnsupportedOperationException(); }
  }
}
