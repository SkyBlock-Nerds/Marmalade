package net.hypixel.nerdbot.marmalade.functional;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResultTest {

    @Test
    void successHoldsValue() {
        Result<String, Exception> result = Result.success("hello");
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.isFailure()).isFalse();
    }

    @Test
    void failureHoldsError() {
        Result<String, IOException> result = Result.failure(new IOException("fail"));
        assertThat(result.isFailure()).isTrue();
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void mapTransformsSuccess() {
        Result<String, Exception> result = Result.success("hello");
        Result<Integer, Exception> mapped = result.map(String::length);
        assertThat(mapped.orElse(0)).isEqualTo(5);
    }

    @Test
    void mapSkipsFailure() {
        Result<String, IOException> result = Result.failure(new IOException("fail"));
        Result<Integer, IOException> mapped = result.map(String::length);
        assertThat(mapped.isFailure()).isTrue();
    }

    @Test
    void flatMapChainsSuccess() {
        Result<String, Exception> result = Result.success("42");
        Result<Integer, Exception> chained = result.flatMap(s -> Result.success(Integer.parseInt(s)));
        assertThat(chained.orElse(0)).isEqualTo(42);
    }

    @Test
    void flatMapShortCircuitsOnFailure() {
        Result<String, Exception> result = Result.failure(new Exception("fail"));
        Result<Integer, Exception> chained = result.flatMap(s -> Result.success(Integer.parseInt(s)));
        assertThat(chained.isFailure()).isTrue();
    }

    @Test
    void mapErrorTransformsError() {
        IOException original = new IOException("original");
        Result<String, IOException> result = Result.failure(original);
        Result<String, RuntimeException> mapped = result.mapError(e -> new RuntimeException("wrapped", e));
        assertThat(mapped.isFailure()).isTrue();
        assertThatThrownBy(mapped::orElseThrow)
                .isInstanceOf(RuntimeException.class)
                .hasMessage("wrapped")
                .hasCause(original);
    }

    @Test
    void orElseReturnsValueOnSuccess() {
        assertThat(Result.success("hello").orElse("default")).isEqualTo("hello");
    }

    @Test
    void orElseReturnsDefaultOnFailure() {
        assertThat(Result.<String, Exception>failure(new Exception("fail")).orElse("default")).isEqualTo("default");
    }

    @Test
    void orElseGetReturnsSuppliedOnFailure() {
        assertThat(Result.<String, Exception>failure(new Exception("fail")).orElseGet(() -> "supplied")).isEqualTo("supplied");
    }

    @Test
    void orElseThrowReturnsValueOnSuccess() throws Exception {
        assertThat(Result.<String, Exception>success("hello").orElseThrow()).isEqualTo("hello");
    }

    @Test
    void orElseThrowThrowsOnFailure() {
        IOException error = new IOException("fail");
        Result<String, IOException> result = Result.failure(error);
        assertThatThrownBy(result::orElseThrow).isSameAs(error);
    }

    @Test
    void toOptionalReturnsValueOnSuccess() {
        assertThat(Result.success("hello").toOptional()).isEqualTo(Optional.of("hello"));
    }

    @Test
    void toOptionalReturnsEmptyOnFailure() {
        assertThat(Result.<String, Exception>failure(new Exception("fail")).toOptional()).isEmpty();
    }

    @Test
    void streamContainsValueOnSuccess() {
        assertThat(Result.success("hello").stream().toList()).containsExactly("hello");
    }

    @Test
    void streamIsEmptyOnFailure() {
        assertThat(Result.<String, Exception>failure(new Exception("fail")).stream().toList()).isEmpty();
    }

    @Test
    void ofCapturesSuccess() {
        Result<String, Exception> result = Result.of(() -> "hello");
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.orElse("")).isEqualTo("hello");
    }

    @Test
    void ofCapturesException() {
        Result<String, Exception> result = Result.of(() -> {
            throw new IOException("boom");
        });
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void mapErrorSkipsSuccess() {
        Result<String, IOException> result = Result.success("hello");
        Result<String, RuntimeException> mapped = result.mapError(RuntimeException::new);
        assertThat(mapped.isSuccess()).isTrue();
        assertThat(mapped.orElse("")).isEqualTo("hello");
    }
}
