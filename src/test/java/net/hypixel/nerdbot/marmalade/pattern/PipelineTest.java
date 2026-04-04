package net.hypixel.nerdbot.marmalade.pattern;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PipelineTest {

    @Test
    void executesStagesInPriorityOrder() {
        Pipeline<String> pipeline = Pipeline.<String>builder()
            .addStage("c", ctx -> ctx + "c", 3)
            .addStage("a", ctx -> ctx + "a", 1)
            .addStage("b", ctx -> ctx + "b", 2)
            .build();

        String result = pipeline.execute("");
        assertThat(result).isEqualTo("abc");
    }

    @Test
    void skipsStagesWhereCanApplyReturnsFalse() {
        Pipeline<String> pipeline = Pipeline.<String>builder()
            .addStage("always", ctx -> ctx + "X", 1)
            .addStage("never", ctx -> ctx + "Y", 2, ctx -> false)
            .addStage("always2", ctx -> ctx + "Z", 3)
            .build();

        String result = pipeline.execute("");
        assertThat(result).isEqualTo("XZ");
    }

    @Test
    void emptyPipelineReturnsContext() {
        Pipeline<String> pipeline = Pipeline.<String>builder().build();
        String result = pipeline.execute("unchanged");
        assertThat(result).isEqualTo("unchanged");
    }

    @Test
    void stageInterfaceImplementation() {
        Pipeline.Stage<Integer> doubler = new Pipeline.Stage<>() {
            @Override
            public Integer apply(Integer context) {
                return context * 2;
            }

            @Override
            public int getPriority() {
                return 5;
            }

            @Override
            public String getName() {
                return "Doubler";
            }
        };

        Pipeline<Integer> pipeline = Pipeline.<Integer>builder()
            .addStage(doubler)
            .build();

        assertThat(pipeline.execute(21)).isEqualTo(42);
    }

    @Test
    void getStagesReturnsUnmodifiableList() {
        Pipeline<String> pipeline = Pipeline.<String>builder()
            .addStage("stage", ctx -> ctx, 0)
            .build();

        List<Pipeline.Stage<String>> stages = pipeline.getStages();
        assertThatThrownBy(() -> stages.add(ctx -> ctx))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void samePriorityPreservesInsertionOrder() {
        Pipeline<String> pipeline = Pipeline.<String>builder()
            .addStage("first", ctx -> ctx + "1", 0)
            .addStage("second", ctx -> ctx + "2", 0)
            .addStage("third", ctx -> ctx + "3", 0)
            .build();

        String result = pipeline.execute("");
        assertThat(result).isEqualTo("123");
    }
}
