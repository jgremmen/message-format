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
package de.sayayi.lib.message.formatter.post.runtime;

import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.DefaultFormatterService;
import de.sayayi.lib.message.internal.part.parameter.AbstractFormatterTest;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static java.lang.Math.PI;
import static java.util.Locale.GERMANY;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Jeroen Gremmen
 */
@DisplayName("'clip' post-formatter")
@TestMethodOrder(MethodOrderer.DisplayName.class)
@SuppressWarnings("UnnecessaryUnicodeEscape")
final class ClipPostFormatterTest extends AbstractFormatterTest
{
  private static final String TEXT = "This is a very long text which is going to be clipped at a specific length";


  @Test
  @DisplayName("Clip without ellipsis")
  void testClipNoEllipsis()
  {
    val messageSupport = MessageSupportFactory
        .create(new DefaultFormatterService(), NO_CACHE_INSTANCE)
        .setDefaultConfig("clip-suffix", false)
        .setLocale(GERMANY);

    assertEquals(
        "",
        messageSupport
            .message("%(clip,'%{v}',clip:64)")
            .with("v", "")
            .format());

    assertEquals(
        TEXT,
        messageSupport
            .message("%(clip,'%{v}',clip:-4)")
            .with("v", TEXT)
            .format());

    assertEquals(
        TEXT.substring(0, 64),
        messageSupport
            .message("%(clip,'%{v}',clip:64)")
            .with("v", TEXT)
            .format());

    assertEquals(
        TEXT.substring(0, 9),
        messageSupport
            .message("%(clip,'%{v}',clip:10)")
            .with("v", TEXT)
            .format());

    assertEquals(
        TEXT.substring(0, 1),
        messageSupport
            .message("%(clip,'%{v}',clip:1)")
            .with("v", TEXT)
            .format());

    assertEquals(
        TEXT,
        messageSupport
            .message("%(clip,'%{v}',clip:0)")
            .with("v", TEXT)
            .format());

    assertEquals(
        "3,1415926535",
        messageSupport
            .message("%(clip,\"%{v,number:'#.##################'}\",clip:12)")
            .with("v", PI)
            .format());
  }


  @Test
  @DisplayName("Clip size")
  void testClipSize()
  {
    val messageSupport = MessageSupportFactory
        .create(new DefaultFormatterService(), NO_CACHE_INSTANCE);

    assertEquals(
        TEXT.substring(0, 63) + "\u2026",
        messageSupport
            .message("%(clip,'%{v}',clip:64)")
            .with("v", TEXT)
            .format());

    assertEquals(
        "This is\u2026",
        messageSupport
            .message("%(clip,'%{v}',clip:9)")
            .with("v", TEXT)
            .format());

    assertEquals(
        "This i\u2026",
        messageSupport
            .message("%(clip,'%{v}',clip:7)")
            .with("v", TEXT)
            .format());

    assertEquals(
        "This\u2026",
        messageSupport
            .message("%(clip,'%{v}',clip:2)")
            .with("v", TEXT)
            .format());

    assertEquals(
        TEXT,
        messageSupport
            .message("%(clip,'%{v}',clip:0)")
            .with("v", TEXT)
            .format());
  }


  @Test
  @DisplayName("Enable/disable Ellipsis")
  void testClipEllipsis()
  {
    val messageSupport = MessageSupportFactory
        .create(new DefaultFormatterService(), NO_CACHE_INSTANCE)
        .setLocale(GERMANY);

    assertEquals(
        "3,141592653\u2026",
        messageSupport
            .message("%(clip,\"%{v,number:'#.##################'}\",clip:12,clip-suffix:true)")
            .with("v", PI)
            .format());

    assertEquals(
        "3,1415926535",
        messageSupport
            .message("%(clip,\"%{v,number:'#.##################'}\",clip:12,clip-suffix:false)")
            .with("v", PI)
            .format());
  }


  @Test
  @DisplayName("Clip with custom suffix")
  void testClipCustomSuffix()
  {
    val messageSupport = MessageSupportFactory
        .create(new DefaultFormatterService(), NO_CACHE_INSTANCE)
        .setLocale(GERMANY);

    assertEquals(
        "3,14159 usw.",
        messageSupport
            .message("%(clip,\"%{v,number:'#.##################'}\",clip:12,clip-suffix-text:' usw.')")
            .with("v", PI)
            .format());
  }
}
