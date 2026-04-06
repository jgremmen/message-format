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
 * This formatter uses the {@code inet} configuration key to select which representation of the address to format:
 * <ul>
 *   <li>{@code ip} (default) &ndash; the IP address string</li>
 *   <li>{@code name} &ndash; the host name</li>
 *   <li>{@code fqdn} &ndash; the fully qualified domain name (canonical host name)</li>
 * </ul>
 * <p>
 * If the configuration value does not match a known option, formatting is delegated to the next available formatter.
 *
 * @author Jeroen Gremmen
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
