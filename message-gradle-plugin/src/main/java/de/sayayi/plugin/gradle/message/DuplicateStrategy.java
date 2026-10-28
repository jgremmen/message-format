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
package de.sayayi.plugin.gradle.message;


/**
 * Strategy for handling duplicate message codes and template names, i.e. messages or templates that are defined more
 * than once, possibly with different texts.
 *
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public enum DuplicateStrategy
{
  /** Keeps the first message or template and silently ignores any duplicates. */
  IGNORE,

  /** Keeps the first message or template and ignores any duplicates, logging a warning if their content differs. */
  IGNORE_AND_WARN,

  /** Replaces an existing message or template with its duplicate without notice. */
  OVERRIDE,

  /** Replaces an existing message or template with a duplicate of different content, logging a warning. */
  OVERRIDE_AND_WARN,

  /** Fails the build if a duplicate with different content is found; identical duplicates are ignored. */
  FAIL
}
