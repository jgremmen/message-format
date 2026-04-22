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
 * Gradle plugin for scanning message format annotations in compiled classes and packing them into a single message
 * pack file.
 * <p>
 * The {@link de.sayayi.plugin.gradle.message.MessageFormatPlugin MessageFormatPlugin} provides the
 * {@code messageFormat} extension and registers the {@code messageFormatPack} task. The extension is represented by
 * {@link de.sayayi.plugin.gradle.message.MessageFormatExtension MessageFormatExtension} and allows configuring source
 * sets, pack filename, compression, duplicate handling strategy, template validation and include/exclude filters.
 * <p>
 * The {@link de.sayayi.plugin.gradle.message.MessageFormatPackTask MessageFormatPackTask} performs the actual scanning
 * and packing. It scans class files for message and template annotations, applies include/exclude filters based on
 * message codes and writes the result to a packed message file.
 * <p>
 * Duplicate message codes and template names are handled according to the configured
 * {@link de.sayayi.plugin.gradle.message.DuplicateMsgStrategy DuplicateMsgStrategy}.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
package de.sayayi.plugin.gradle.message;
