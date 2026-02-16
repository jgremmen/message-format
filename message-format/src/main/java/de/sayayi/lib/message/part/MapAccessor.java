package de.sayayi.lib.message.part;

import de.sayayi.lib.message.Message;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;


/**
 * @author Jeroen Gremmen
 * @since 0.21.0
 */
public interface MapAccessor
{
  /**
   * Returns the map.
   *
   * @return  map, never {@code null}
   */
  @Contract(pure = true)
  @NotNull MessagePart.Map getMap();


  /**
   * Tells whether the map contains an entry with the given {@code keyType} that maps to a message.
   *
   * @param keyType  entry key type to look for, not {@code null}
   *
   * @return  {@code true} if the map contains an entry with the given key type,
   *          {@code false} otherwise
   */
  @Contract(pure = true)
  boolean hasMapMessage(@NotNull MapKey.Type keyType);


  /**
   * Gets a message for {@code key} from the parameter configuration map. The map will be
   * probed for keys with the given {@code keyTypes} only.
   *
   * @param key       the key to get the message for
   * @param keyTypes  key types to be considered when matching the {@code key}, not {@code null}
   *
   * @return  optional instance containing the mapped message, never {@code null}. If no matching
   *          message is found, {@link Optional#empty()} is returned
   */
  @Contract(pure = true)
  default @NotNull Optional<Message.WithSpaces> getMapMessage(Object key, @NotNull Set<MapKey.Type> keyTypes) {
    return getMapMessage(key, keyTypes, false);
  }


  /**
   * Gets a message for {@code key} from the parameter configuration map. The map will be
   * probed for keys with the given {@code keyTypes} only. If no entry for {@code key} is
   * found the default message, if present, will be returned.
   * <p>
   * The default message is considered only if the parameter configuration map contains at
   * least 1 key with a type contained in {@code keyTypes}.
   *
   * @param key             the key to get the message for
   * @param keyTypes        key types to be considered when matching the {@code key},
   *                        not {@code null}
   * @param includeDefault  {@code true} will return the default message (if any) in case no
   *                        matching key is found, {@code false} will not return the default message
   *
   * @return  optional instance containing the mapped message, never {@code null}. If no matching
   *          message is found, {@link Optional#empty()} is returned
   */
  @Contract(pure = true)
  @NotNull Optional<Message.WithSpaces> getMapMessage(Object key, @NotNull Set<MapKey.Type> keyTypes,
                                                      boolean includeDefault);
}
