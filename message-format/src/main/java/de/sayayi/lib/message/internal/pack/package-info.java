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

/**
 * Serialization and deserialization support for packing messages and templates into a compact binary format.
 * <p>
 * {@link de.sayayi.lib.message.internal.pack.PackSupport PackSupport} packs and unpacks messages, message parts, map
 * keys and typed values. {@link de.sayayi.lib.message.internal.pack.PackFileTypeDetector PackFileTypeDetector} is a
 * {@link java.nio.file.spi.FileTypeDetector FileTypeDetector} service recognizing pack files by their magic bytes.
 * <p>
 * <strong>This package is not part of the public API.</strong> Its classes are subject to change without notice and
 * should not be referenced directly by application code.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
package de.sayayi.lib.message.internal.pack;
