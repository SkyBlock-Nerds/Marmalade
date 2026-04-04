package net.hypixel.nerdbot.marmalade.pattern;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * An ordered sequence of {@link Stage} transformations applied to a shared context object.
 * Stages are sorted by priority (ascending) and then by insertion order for stable tie-breaking.
 *
 * @param <C> the type of the context object passed through the pipeline
 */
@Slf4j
public class Pipeline<C> {

    /**
     * A single transformation step in a {@link Pipeline} that accepts and returns a context object.
     *
     * @param <C> the type of the context object
     */
    @FunctionalInterface
    public interface Stage<C> {

        /**
         * Applies this stage's transformation to the given context.
         *
         * @param context the current context
         * @return the transformed context
         */
        C apply(C context);

        /**
         * Returns the priority of this stage; lower values run first.
         * Defaults to {@code 0}.
         *
         * @return the priority value
         */
        default int getPriority() {
            return 0;
        }

        /**
         * Returns {@code true} if this stage should be applied to the given context.
         * Defaults to {@code true} for all contexts.
         *
         * @param context the current context
         * @return {@code true} if this stage should run, {@code false} to skip it
         */
        default boolean canApply(C context) {
            return true;
        }

        /**
         * Returns a human-readable name for this stage, used in logging.
         * Defaults to the simple class name.
         *
         * @return the stage name
         */
        default String getName() {
            return getClass().getSimpleName();
        }
    }

    private record LambdaStage<C>(
        String name,
        Function<C, C> fn,
        int priority,
        Predicate<C> guard
    ) implements Stage<C> {

        @Override
        public C apply(C context) {
            return fn.apply(context);
        }

        @Override
        public int getPriority() {
            return priority;
        }

        @Override
        public boolean canApply(C context) {
            return guard.test(context);
        }

        @Override
        public String getName() {
            return name;
        }
    }

    private record StageEntry<C>(Stage<C> stage, int insertionOrder) {
    }

    private final List<Stage<C>> stages;

    private Pipeline(List<Stage<C>> stages) {
        this.stages = stages;
    }

    /**
     * Passes the context through each stage in order, skipping any stage whose {@link Stage#canApply} returns false.
     *
     * @param context the initial context to process
     * @return the context produced after all applicable stages have run
     */
    public C execute(C context) {
        C current = context;
        for (Stage<C> stage : stages) {
            if (!stage.canApply(current)) {
                log.debug("Skipping stage '{}' (canApply returned false)", stage.getName());
                continue;
            }
            log.debug("Executing stage '{}'", stage.getName());
            current = stage.apply(current);
        }
        return current;
    }

    /**
     * Returns an unmodifiable view of the stages in this pipeline, in execution order.
     *
     * @return an unmodifiable list of stages
     */
    public List<Stage<C>> getStages() {
        return Collections.unmodifiableList(stages);
    }

    /**
     * Returns a new {@link Builder} for constructing a {@link Pipeline}.
     *
     * @param <C> the context type
     * @return a fresh builder instance
     */
    public static <C> Builder<C> builder() {
        return new Builder<>();
    }

    /**
     * Fluent builder for assembling a {@link Pipeline} from individual stages.
     *
     * @param <C> the context type
     */
    public static class Builder<C> {

        private final List<StageEntry<C>> entries = new ArrayList<>();
        private int insertionCounter = 0;

        /**
         * Adds a {@link Stage} implementation to the pipeline.
         *
         * @param stage the stage to add
         * @return this builder
         */
        public Builder<C> addStage(Stage<C> stage) {
            entries.add(new StageEntry<>(stage, insertionCounter++));
            return this;
        }

        /**
         * Adds a named lambda stage with default priority ({@code 0}) and no guard condition.
         *
         * @param name the display name of the stage, used in logging
         * @param fn   the transformation function to apply
         * @return this builder
         */
        public Builder<C> addStage(String name, Function<C, C> fn) {
            return addStage(name, fn, 0);
        }

        /**
         * Adds a named lambda stage with a specified priority and no guard condition.
         *
         * @param name     the display name of the stage, used in logging
         * @param fn       the transformation function to apply
         * @param priority the execution priority - lower values run first
         * @return this builder
         */
        public Builder<C> addStage(String name, Function<C, C> fn, int priority) {
            return addStage(name, fn, priority, ctx -> true);
        }

        /**
         * Adds a named lambda stage with a specified priority and guard predicate.
         *
         * @param name     the display name of the stage, used in logging
         * @param fn       the transformation function to apply
         * @param priority the execution priority - lower values run first
         * @param guard    a predicate that determines whether the stage should run for a given context
         * @return this builder
         */
        public Builder<C> addStage(String name, Function<C, C> fn, int priority, Predicate<C> guard) {
            entries.add(new StageEntry<>(new LambdaStage<>(name, fn, priority, guard), insertionCounter++));
            return this;
        }

        /**
         * Builds the {@link Pipeline}, sorting all added stages by priority then insertion order.
         *
         * @return a new {@link Pipeline} containing the configured stages
         */
        public Pipeline<C> build() {
            List<Stage<C>> sorted = entries.stream()
                .sorted(Comparator
                    .comparingInt((StageEntry<C> e) -> e.stage().getPriority())
                    .thenComparingInt(StageEntry::insertionOrder))
                .map(StageEntry::stage)
                .toList();
            return new Pipeline<>(sorted);
        }
    }
}
