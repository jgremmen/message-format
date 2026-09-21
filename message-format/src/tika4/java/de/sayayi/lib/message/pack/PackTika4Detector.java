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
package de.sayayi.lib.message.pack;

import de.sayayi.lib.pack.detector.AbstractTika4Detector;

import static de.sayayi.lib.message.pack.PackConstants.MIME_TYPE;
import static de.sayayi.lib.message.pack.PackConstants.PACK_CONFIG;


/**
 * Apache Tika detector for message format pack files. This detector uses the
 * {@link PackConstants#PACK_CONFIG pack configuration} to identify files by their magic bytes and reports the
 * {@link PackConstants#MIME_TYPE message format pack MIME type}.
 * <p>
 * Apache Tika discovers detectors through the {@link java.util.ServiceLoader ServiceLoader} mechanism. To register
 * this class, add its fully qualified name to a service file named
 * {@code META-INF/services/org.apache.tika.detect.Detector}:
 * <pre>
 * de.sayayi.lib.message.pack.PackTika4Detector
 * </pre>
 * With the detector on the classpath, Tika picks it up automatically:
 * <pre>
 * Tika tika = new Tika();
 * String mimeType = tika.detect(packInputStream);
 * </pre>
 *
 * @author Jeroen Gremmen
 * @since 0.25.0
 */
public final class PackTika4Detector extends AbstractTika4Detector
{
  /** Creates a new Tika 4 detector for message format pack files. */
  public PackTika4Detector() {
    super(PACK_CONFIG, MIME_TYPE);
  }
}
