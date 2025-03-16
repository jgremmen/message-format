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
package de.sayayi.lib.message.formatter.post;

import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.AbstractFormatterTest;
import de.sayayi.lib.message.formatter.DefaultFormatterService;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static de.sayayi.lib.message.MessageFactory.NO_CACHE_INSTANCE;
import static java.util.Locale.GERMANY;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Jeroen Gremmen
 */
@DisplayName("'clip' post-formatter")
class ClipPostFormatterTest extends AbstractFormatterTest
{
  private static final String TEXT = "This is a very long text which is going to be clipped at a specific length";


  @Test
  @DisplayName("Clip size")
  void testClipSize()
  {
    val messageSupport = MessageSupportFactory
        .create(new DefaultFormatterService(), NO_CACHE_INSTANCE);

    assertEquals(
        TEXT.substring(0, 61) + "...",
        messageSupport
            .message("%{v,clip:64}")
            .with("v", TEXT)
            .format());

    assertEquals(
        "This is...",
        messageSupport
            .message("%{v,clip:10}")
            .with("v", TEXT)
            .format());

    assertEquals(
        "This...",
        messageSupport
            .message("%{v,clip:8}")
            .with("v", TEXT)
            .format());

    assertEquals(
        "This...",
        messageSupport
            .message("%{v,clip:2}")
            .with("v", TEXT)
            .format());

    assertEquals(
        TEXT,
        messageSupport
            .message("%{v,clip:0}")
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
        TEXT,
        messageSupport
            .message("%{v,clip-ellipsis:false}")
            .with("v", TEXT)
            .format());

    assertEquals(
        "3,1415926535",
        messageSupport
            .message("%{v,number:'#.##################',clip:12,clip-ellipsis:false}")
            .with("v", Math.PI)
            .format());

    assertEquals(
        "3,1415926...",
        messageSupport
            .message("%{v,number:'#.##################',clip:12,clip-ellipsis:true}")
            .with("v", Math.PI)
            .format());
  }
}
