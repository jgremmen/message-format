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
package de.sayayi.lib.message.formatter.parameter.runtime.extra;

import de.sayayi.lib.message.MessageSupport;
import de.sayayi.lib.message.MessageSupportFactory;
import de.sayayi.lib.message.formatter.DefaultFormatterService;
import de.sayayi.lib.message.internal.part.parameter.AbstractFormatterTest;
import lombok.val;
import org.junit.jupiter.api.*;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Tests {@link InetAddressFormatter}: formattable types and formatting of the IP address and the host name.
 *
 * @author Jeroen Gremmen
 * @since 0.25.0
 */
@DisplayName("Inet address formatter")
@TestMethodOrder(MethodOrderer.DisplayName.class)
final class InetAddressFormatterTest extends AbstractFormatterTest
{
  private MessageSupport messageSupport;


  @BeforeEach
  void init()
  {
    val formatterService = new DefaultFormatterService();
    formatterService.addFormatter(new InetAddressFormatter());

    messageSupport = MessageSupportFactory.create(formatterService);
  }


  @Test
  void testFormattableTypes() {
    assertFormatterForType(new InetAddressFormatter(), InetAddress.class);
  }


  @Test
  @DisplayName("Format IP address and host name")
  void testFormat() throws UnknownHostException
  {
    val address = InetAddress.getByAddress("db01", new byte[] { 10, 0, 0, 7 });

    assertEquals("Connected to db01 (10.0.0.7)", messageSupport
        .message("Connected to %{a,inet:name} (%{a})")
        .with("a", address)
        .format());
    assertEquals("10.0.0.7", messageSupport.message("%{a,inet:ip}").with("a", address).format());
  }


  @Test
  @DisplayName("Format IPv6 address")
  void testFormatIPv6() throws UnknownHostException
  {
    val address = InetAddress.getByAddress("ip6",
        new byte[] { 0x20, 0x01, 0x0d, (byte)0xb8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 });

    assertEquals("2001:db8:0:0:0:0:0:1", messageSupport.message("%{a}").with("a", address).format());
  }


  @Test
  @DisplayName("Unknown inet value delegates to the next formatter")
  void testUnknownConfigValue() throws UnknownHostException
  {
    val address = InetAddress.getByAddress("db01", new byte[] { 10, 0, 0, 7 });

    assertEquals(address.toString(), messageSupport.message("%{a,inet:unknown}").with("a", address).format());
  }
}
