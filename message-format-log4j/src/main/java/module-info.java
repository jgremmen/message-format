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
 * Log4j integration module for the message-format library.
 * <p>
 * Provides {@link de.sayayi.lib.message.log4j.Log4jMessageFactory Log4jMessageFactory}, a Log4j
 * {@link org.apache.logging.log4j.message.MessageFactory MessageFactory} that uses the message-format syntax for
 * formatting log messages. Parameters passed to log methods are available as {@code p1}, {@code p2}, etc. in the
 * message format.
 * <p>
 * Example usage:
 * <pre>{@code
 *   import org.apache.logging.log4j.LogManager;
 *   import org.apache.logging.log4j.Logger;
 *
 *   Logger logger = LogManager.getLogger(MyClass.class, new Log4jMessageFactory());
 *
 *   logger.info("Hello %{p1}, you have %{p2} new messages.", "World", 5);
 * }</pre>
 *
 * @author Jeroen Gremmen
 * @since 0.21.0
 */
module de.sayayi.lib.message.log4j
{
  requires de.sayayi.lib.message;
  requires org.apache.logging.log4j;

  requires static org.jetbrains.annotations;

  exports de.sayayi.lib.message.log4j;
}
