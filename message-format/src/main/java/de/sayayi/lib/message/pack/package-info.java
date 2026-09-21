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

/**
 * Support for the packed (binary) representation of messages and templates, including the constants that describe it
 * and the Apache Tika detectors that recognize it.
 * <p>
 * A pack is a compact, serialized form of message data that can be written to and read from a stream, optionally
 * compressed. The types in this package define how such data is identified and how it is detected by content
 * inspection tooling.
 * <p>
 * The main types in this package are:
 * <ul>
 *   <li>{@link de.sayayi.lib.message.pack.PackConstants PackConstants} holds the shared MIME type and pack
 *       configuration (magic bytes, version range and compression support) used when reading and writing packs</li>
 *   <li>{@link de.sayayi.lib.message.pack.PackTikaDetector PackTikaDetector} and
 *       {@link de.sayayi.lib.message.pack.PackTika3Detector PackTika3Detector} are Apache Tika 3 detectors that
 *       identify pack files by their magic bytes and report the message format pack MIME type</li>
 *   <li>{@link de.sayayi.lib.message.pack.PackTika4Detector PackTika4Detector} provides the equivalent detector for
 *       Apache Tika 4</li>
 * </ul>
 *
 * @author Jeroen Gremmen
 * @since 0.25.0
 */
package de.sayayi.lib.message.pack;
