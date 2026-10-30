/*
 * Copyright 2019 Jeroen Gremmen
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

import de.sayayi.lib.message.formatter.FormattableType;
import de.sayayi.lib.message.formatter.parameter.AbstractMultiSelectFormatter;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.util.Set;

import static de.sayayi.lib.message.part.TextPartFactory.noSpaceText;


/**
 * Parameter formatter for {@link InetAddress} values.
 * <p>
 * This formatter uses the {@code inet} configuration key to select which representation of the address to format.
 * <p>
 * If the configuration value does not match a known option, formatting is delegated to the next available formatter.
 * <p>
 * <b>Configuration keys:</b>
 * <table border="1">
 *   <caption>Configuration keys for the inet address formatter</caption>
 *   <tr><th>Key</th><th>Values</th><th>Default</th><th>Purpose</th></tr>
 *   <tr>
 *     <td>{@code inet}</td>
 *     <td>{@code ip}, {@code name}, {@code fqdn}</td>
 *     <td>{@code ip}</td>
 *     <td>
 *       Formats the IP address string, the host name or the fully qualified domain name (canonical host name).
 *       The latter two may require a name service lookup. Any other value delegates to the next formatter.
 *     </td>
 *   </tr>
 * </table>
 * <p>
 * <b>Examples:</b>
 * <p>
 * This formatter is not part of the {@link de.sayayi.lib.message.formatter.DefaultFormatterService
 * DefaultFormatterService} and must be registered explicitly:
 * <pre>{@code
 * var formatterService = new DefaultFormatterService();
 * formatterService.addFormatter(new InetAddressFormatter());
 *
 * var messageSupport = MessageSupportFactory.create(formatterService).seal();
 * }</pre>
 * <p>
 * Format the IP address and the host name of an address:
 * <pre>{@code
 * var address = InetAddress.getByAddress("db01", new byte[] { 10, 0, 0, 7 });
 *
 * messageSupport
 *     .message("Connected to %{a,inet:name} (%{a})")
 *     .with("a", address)
 *     .format();
 * // "Connected to db01 (10.0.0.7)"
 * }</pre>
 * <p>
 * The canonical host name depends on the name service of the system:
 * <pre>{@code
 * messageSupport
 *     .message("Host: %{a,inet:fqdn}")
 *     .with("a", InetAddress.getLoopbackAddress())
 *     .format();
 * // e.g. "Host: localhost"
 * }</pre>
 *
 * @author Jeroen Gremmen
 * @since 0.3.1
 */
public final class InetAddressFormatter extends AbstractMultiSelectFormatter<InetAddress>
{
  /**
   * Creates a new inet address formatter with the configuration key {@code inet} and selection options for IP address,
   * host name and FQDN.
   */
  public InetAddressFormatter()
  {
    super("inet", "ip", true);

    register("name", (context,inetAddress) -> noSpaceText(inetAddress.getHostName()));
    register("fqdn", (context,inetAddress) -> noSpaceText(inetAddress.getCanonicalHostName()));
    register("ip", (context,inetAddress) -> noSpaceText(inetAddress.getHostAddress()));
  }


  /**
   * {@inheritDoc}
   *
   * @return  a set containing the {@link InetAddress} formattable type, never {@code null}
   */
  @Override
  public @NotNull Set<FormattableType> getFormattableTypes() {
    return Set.of(new FormattableType(InetAddress.class));
  }
}
