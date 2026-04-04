package net.hypixel.nerdbot.marmalade.pattern;

import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A thread-safe, generic key-value registry backed by a {@link ConcurrentHashMap}.
 * Supports configurable duplicate-key handling and optional case-insensitive String key matching.
 *
 * @param <K> the type of keys maintained by this registry
 * @param <V> the type of values stored in this registry
 */
@Slf4j
public class Registry<K, V> {

    /**
     * Defines how the registry behaves when a key is registered more than once.
     */
    public enum DuplicatePolicy {
        /** Throws {@link IllegalArgumentException} if the key already exists. */
        THROW,
        /** Replaces the existing value with the new one. */
        OVERWRITE,
        /** Silently discards the new value if the key already exists. */
        IGNORE
    }

    private final ConcurrentHashMap<K, V> store = new ConcurrentHashMap<>();
    private final DuplicatePolicy defaultPolicy;
    private final boolean caseInsensitive;

    /**
     * Creates a registry with {@link DuplicatePolicy#THROW} and case-sensitive key matching.
     */
    public Registry() {
        this(DuplicatePolicy.THROW, false);
    }

    /**
     * Creates a registry with the given duplicate policy and case-sensitive key matching.
     *
     * @param defaultPolicy the policy to apply when a duplicate key is registered
     */
    public Registry(DuplicatePolicy defaultPolicy) {
        this(defaultPolicy, false);
    }

    /**
     * Creates a registry with {@link DuplicatePolicy#THROW} and the specified case sensitivity.
     *
     * @param caseInsensitive if {@code true}, String keys are lowercased before storage and lookup
     */
    public Registry(boolean caseInsensitive) {
        this(DuplicatePolicy.THROW, caseInsensitive);
    }

    /**
     * Creates a registry with explicit duplicate policy and case sensitivity settings.
     *
     * @param defaultPolicy   the policy to apply when a duplicate key is registered
     * @param caseInsensitive if {@code true}, String keys are lowercased before storage and lookup
     */
    public Registry(DuplicatePolicy defaultPolicy, boolean caseInsensitive) {
        this.defaultPolicy = defaultPolicy;
        this.caseInsensitive = caseInsensitive;
    }

    @SuppressWarnings("unchecked")
    private K normalizeKey(K key) {
        if (caseInsensitive) {
            if (!(key instanceof String)) {
                throw new IllegalArgumentException(
                    "Case-insensitive registry requires String keys, but got: " + key.getClass().getName()
                );
            }
            return (K) ((String) key).toLowerCase();
        }
        return key;
    }

    /**
     * Registers a key-value pair using the registry's default {@link DuplicatePolicy}.
     *
     * @param key   the key to register
     * @param value the value to associate with the key
     */
    public void register(K key, V value) {
        register(key, value, defaultPolicy);
    }

    /**
     * Registers a key-value pair using the specified {@link DuplicatePolicy}.
     *
     * @param key    the key to register
     * @param value  the value to associate with the key
     * @param policy the policy to apply if the key is already registered
     * @throws IllegalArgumentException if {@code policy} is {@link DuplicatePolicy#THROW} and the key already exists
     */
    public void register(K key, V value, DuplicatePolicy policy) {
        K normalizedKey = normalizeKey(key);
        switch (policy) {
            case THROW -> {
                V existing = store.putIfAbsent(normalizedKey, value);
                if (existing != null) {
                    throw new IllegalArgumentException("Key already registered: " + key);
                }
                log.debug("Registered key '{}' with value '{}'", key, value);
            }
            case OVERWRITE -> {
                store.put(normalizedKey, value);
                log.debug("Registered (overwrite) key '{}' with value '{}'", key, value);
            }
            case IGNORE -> {
                V existing = store.putIfAbsent(normalizedKey, value);
                if (existing != null) {
                    log.debug("Ignored duplicate registration for key '{}'", key);
                } else {
                    log.debug("Registered key '{}' with value '{}'", key, value);
                }
            }
        }
    }

    /**
     * Returns the value associated with the given key, if present.
     *
     * @param key the key to look up
     * @return an {@link Optional} containing the value, or empty if the key is not registered
     */
    public Optional<V> get(K key) {
        return Optional.ofNullable(store.get(normalizeKey(key)));
    }

    /**
     * Returns the value associated with the given key, throwing if absent.
     *
     * @param key the key to look up
     * @return the value associated with the key
     * @throws NoSuchElementException if no value is registered for the key
     */
    public V getOrThrow(K key) {
        return get(key).orElseThrow(() -> new NoSuchElementException("No value registered for key: " + key));
    }

    /**
     * Returns {@code true} if the registry contains a mapping for the given key.
     *
     * @param key the key to check
     * @return {@code true} if the key is registered, {@code false} otherwise
     */
    public boolean contains(K key) {
        return store.containsKey(normalizeKey(key));
    }

    /**
     * Returns all values currently stored in the registry.
     *
     * @return a live collection view of all registered values
     */
    public Collection<V> getAll() {
        return store.values();
    }

    /**
     * Returns the set of all keys currently registered.
     *
     * @return a live set view of all registered keys
     */
    public Set<K> keys() {
        return store.keySet();
    }

    /**
     * Returns the number of entries in the registry.
     *
     * @return the current entry count
     */
    public int size() {
        return store.size();
    }

    /**
     * Returns {@code true} if the registry contains no entries.
     *
     * @return {@code true} if the registry is empty, {@code false} otherwise
     */
    public boolean isEmpty() {
        return store.isEmpty();
    }

    /**
     * Removes the entry for the given key, if present.
     *
     * @param key the key to remove
     * @return an {@link Optional} containing the removed value, or empty if the key was not registered
     */
    public Optional<V> remove(K key) {
        return Optional.ofNullable(store.remove(normalizeKey(key)));
    }

    /**
     * Removes all entries from the registry.
     */
    public void clear() {
        store.clear();
    }

    /**
     * Returns an immutable snapshot of the registry's current state.
     *
     * @return an unmodifiable copy of all key-value pairs at the time of the call
     */
    public Map<K, V> snapshot() {
        return Map.copyOf(store);
    }
}
