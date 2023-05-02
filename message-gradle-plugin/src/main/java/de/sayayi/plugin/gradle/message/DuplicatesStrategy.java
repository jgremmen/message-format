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
 * @author Jeroen Gremmen
 * @since 0.8.0
 */
public enum DuplicatesStrategy
{
  /** silently ignore duplicate messages and templates */
  IGNORE,

  /** ignore duplicate messages and templates but log a warning */
  IGNORE_AND_WARN,

  /** silently override duplicate messages and templates */
  OVERRIDE,

  /** override duplicate messages and templates and log a warning */
  OVERRIDE_AND_WARN,

  /** stop processing messages and templates if a duplicate is found */
  FAIL
}
