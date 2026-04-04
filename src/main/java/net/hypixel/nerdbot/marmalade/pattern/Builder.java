package net.hypixel.nerdbot.marmalade.pattern;

/**
 * Abstract base class for builders that separate validation from object construction.
 * Subclasses implement {@link #validate()} and {@link #construct()}; callers use {@link #build()}.
 *
 * @param <T> the type of object produced by this builder
 */
public abstract class Builder<T> {

    /**
     * Validates the builder's current state, throwing if any required fields are missing or inconsistent.
     */
    protected abstract void validate();

    /**
     * Constructs and returns the target object, assuming the builder state has already been validated.
     *
     * @return a new instance of {@code T}
     */
    protected abstract T construct();

    /**
     * Validates the builder state and constructs the target object.
     * Equivalent to calling {@link #validate()} followed by {@link #construct()}.
     *
     * @return a new instance of {@code T}
     */
    public final T build() {
        validate();
        return construct();
    }
}
