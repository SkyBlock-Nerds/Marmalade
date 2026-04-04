package net.hypixel.nerdbot.marmalade.registry;

import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import net.hypixel.nerdbot.marmalade.json.JsonLoader;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Base class for JSON-backed data registries that load a typed collection from a classpath resource
 * and optionally merge overrides from an external file on disk.
 *
 * <p>Subclasses must implement {@link #getArrayType()}, {@link #getResourcePath()}, and
 * {@link #getNameExtractor()} to describe their data type and source. Call {@link #load()} once
 * at startup to populate the registry before using the lookup methods.
 *
 * @param <T> the type of item held in this registry
 */
@Slf4j
public abstract class DataRegistry<T> {

    private final List<T> items = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Abstract methods - subclasses must implement
    // -------------------------------------------------------------------------

    /**
     * Returns the {@code Class} object for the array type used when deserializing JSON.
     * For example, a registry of {@code Foo} items should return {@code Foo[].class}.
     *
     * @return the array class token for type {@code T}
     */
    protected abstract Class<T[]> getArrayType();

    /**
     * Returns the classpath-relative path to the bundled JSON resource file.
     *
     * @return the resource path, e.g. {@code "data/items.json"}
     */
    protected abstract String getResourcePath();

    /**
     * Returns a function that extracts the unique name from an item, used for name-based lookups
     * and external merge matching.
     *
     * @return a non-null function mapping an item to its name
     */
    protected abstract Function<T, String> getNameExtractor();

    // -------------------------------------------------------------------------
    // Optional overrides
    // -------------------------------------------------------------------------

    /**
     * Returns the filename (relative to the external data directory) for the optional override file.
     * Defaults to {@link #getResourcePath()}; override to use a different file name on disk.
     *
     * @return the external file name used when scanning the data directory
     */
    protected String getExternalFileName() {
        return getResourcePath();
    }

    /**
     * Returns a customization function applied to the {@link GsonBuilder} before parsing.
     * Defaults to a no-op; override to register type adapters or other Gson settings.
     *
     * @return a {@link UnaryOperator} that configures a {@code GsonBuilder}
     */
    protected UnaryOperator<GsonBuilder> customizeGson() {
        return UnaryOperator.identity();
    }

    /**
     * Returns the {@link ClassLoader} used to locate the classpath resource.
     * Defaults to the class loader of the concrete subclass.
     *
     * @return the class loader to use for resource resolution
     */
    protected ClassLoader getClassLoader() {
        return getClass().getClassLoader();
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Loads items from the bundled classpath resource and, if present, merges overrides from an
     * external file located in the directory specified by the {@code marmalade.data.dir} system
     * property (defaults to {@code "data"}).
     *
     * @throws IOException if the classpath resource cannot be found or read
     */
    public final void load() throws IOException {
        items.clear();

        URL classpathUrl = getClassLoader().getResource(getResourcePath());
        if (classpathUrl == null) {
            throw new IOException("Classpath resource not found: " + getResourcePath());
        }

        List<T> loaded = JsonLoader.loadFromJson(getArrayType(), classpathUrl, customizeGson());
        items.addAll(loaded);
        log.debug("Loaded {} item(s) from classpath resource '{}'", items.size(), getResourcePath());

        String dataDir = System.getProperty("marmalade.data.dir", "data");
        File externalFile = new File(dataDir, getExternalFileName());

        if (externalFile.exists()) {
            List<T> external = JsonLoader.loadFromJson(getArrayType(), externalFile.getAbsolutePath(), customizeGson());
            log.debug("Merging {} external item(s) from '{}'", external.size(), externalFile.getPath());
            mergeExternal(items, external);
        }

        log.info("Registry '{}' ready with {} item(s)", getClass().getSimpleName(), items.size());
    }

    /**
     * Looks up the first item whose name matches {@code name} (case-insensitive).
     *
     * @param name the name to search for; returns empty if {@code null}
     * @return an {@link Optional} containing the matching item, or empty if none is found
     */
    public Optional<T> byName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        Function<T, String> extractor = getNameExtractor();
        return items.stream()
            .filter(item -> name.equalsIgnoreCase(extractor.apply(item)))
            .findFirst();
    }

    /**
     * Returns the first item that satisfies the given predicate.
     *
     * @param predicate the condition to test against each item
     * @return an {@link Optional} containing the first matching item, or empty if none matches
     */
    public Optional<T> findFirst(Predicate<T> predicate) {
        return items.stream()
            .filter(predicate)
            .findFirst();
    }

    /**
     * Returns all items that satisfy the given predicate.
     *
     * @param predicate the condition to test against each item
     * @return an unmodifiable list of all matching items; empty if none match
     */
    public List<T> findAll(Predicate<T> predicate) {
        return items.stream()
            .filter(predicate)
            .toList();
    }

    /**
     * Returns an unmodifiable snapshot of all items currently held in this registry.
     *
     * @return an immutable list of all loaded items
     */
    public List<T> getAll() {
        return List.copyOf(items);
    }

    /**
     * Returns the number of items currently held in this registry.
     *
     * @return the item count
     */
    public int size() {
        return items.size();
    }

    /**
     * Returns {@code true} if this registry contains no items.
     *
     * @return {@code true} if the registry is empty, {@code false} otherwise
     */
    public boolean isEmpty() {
        return items.isEmpty();
    }

    // -------------------------------------------------------------------------
    // Protected merge hook
    // -------------------------------------------------------------------------

    /**
     * Merges {@code external} items into the {@code existing} list by replacing any item whose name
     * matches (case-insensitive) and appending any that are not already present.
     * Override to implement custom merge semantics.
     *
     * @param existing the mutable list of items already loaded from the classpath resource
     * @param external the list of items loaded from the external override file
     */
    protected void mergeExternal(List<T> existing, List<T> external) {
        Function<T, String> extractor = getNameExtractor();

        for (T incoming : external) {
            String incomingName = extractor.apply(incoming);
            boolean replaced = false;

            for (int i = 0; i < existing.size(); i++) {
                if (incomingName.equalsIgnoreCase(extractor.apply(existing.get(i)))) {
                    existing.set(i, incoming);
                    replaced = true;
                    break;
                }
            }

            if (!replaced) {
                existing.add(incoming);
            }
        }
    }
}
