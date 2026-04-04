package net.hypixel.nerdbot.marmalade.functional;

import java.util.function.Function;

/**
 * An immutable container holding two related values of potentially different types.
 *
 * @param <A> the type of the first element
 * @param <B> the type of the second element
 * @param first  the first element
 * @param second the second element
 */
public record Pair<A, B>(A first, B second) {

    /**
     * Creates a new {@code Pair} from the given values.
     *
     * @param <A>    the type of the first element
     * @param <B>    the type of the second element
     * @param first  the first element
     * @param second the second element
     * @return a new {@code Pair} containing both values
     */
    public static <A, B> Pair<A, B> of(A first, B second) {
        return new Pair<>(first, second);
    }

    /**
     * Returns a new {@code Pair} with the first element transformed by the given function,
     * leaving the second element unchanged.
     *
     * @param <C> the type of the transformed first element
     * @param fn  the function to apply to the first element
     * @return a new {@code Pair} with the mapped first element and the original second element
     */
    public <C> Pair<C, B> mapFirst(Function<A, C> fn) {
        return new Pair<>(fn.apply(first), second);
    }

    /**
     * Returns a new {@code Pair} with the second element transformed by the given function,
     * leaving the first element unchanged.
     *
     * @param <C> the type of the transformed second element
     * @param fn  the function to apply to the second element
     * @return a new {@code Pair} with the original first element and the mapped second element
     */
    public <C> Pair<A, C> mapSecond(Function<B, C> fn) {
        return new Pair<>(first, fn.apply(second));
    }
}
