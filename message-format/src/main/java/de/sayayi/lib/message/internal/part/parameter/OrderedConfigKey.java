package de.sayayi.lib.message.internal.part.parameter;

import de.sayayi.lib.message.part.parameter.key.ConfigKey;
import org.jetbrains.annotations.NotNull;


/**
 *
 * @param order      the order of the configuration key, used for sorting
 * @param configKey  the configuration key, not {@code null}
 *
 * @since 0.21.0
 */
record OrderedConfigKey(int order, @NotNull ConfigKey configKey) {
}
