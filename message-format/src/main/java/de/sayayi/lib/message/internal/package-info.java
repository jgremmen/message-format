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

/**
 * Internal implementations of the message format API.
 * <p>
 * This package contains the concrete {@link de.sayayi.lib.message.Message Message} implementations
 * such as {@link de.sayayi.lib.message.internal.TextMessage TextMessage},
 * {@link de.sayayi.lib.message.internal.CompoundMessage CompoundMessage} and
 * {@link de.sayayi.lib.message.internal.EmptyMessage EmptyMessage}, as well as coded message
 * variants like {@link de.sayayi.lib.message.internal.EmptyMessageWithCode EmptyMessageWithCode},
 * {@link de.sayayi.lib.message.internal.MessageDelegateWithCode MessageDelegateWithCode} and
 * {@link de.sayayi.lib.message.internal.LocalizedMessageBundleWithCode LocalizedMessageBundleWithCode}.
 * <p>
 * It also provides the {@link de.sayayi.lib.message.internal.MessageSupportImpl MessageSupportImpl}
 * which is the core implementation of
 * {@link de.sayayi.lib.message.MessageSupport.ConfigurableMessageSupport ConfigurableMessageSupport},
 * and the {@link de.sayayi.lib.message.internal.InternalMessageBuilder InternalMessageBuilder}
 * for programmatically constructing messages.
 * <p>
 * <strong>This package is not part of the public API.</strong> Classes in this package are subject
 * to change without notice and should not be referenced directly by application code.
 *
 * @author Jeroen Gremmen
 * @since 0.1.0
 */
package de.sayayi.lib.message.internal;
