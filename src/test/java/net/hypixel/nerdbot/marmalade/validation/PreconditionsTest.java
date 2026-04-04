package net.hypixel.nerdbot.marmalade.validation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PreconditionsTest {

    @Test
    void notNullPassesThrough() {
        String value = "hello";
        assertThat(Preconditions.notNull(value, "value")).isSameAs(value);
    }

    @Test
    void notNullThrowsOnNull() {
        assertThatThrownBy(() -> Preconditions.notNull(null, "myField"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("myField");
    }

    @Test
    void notNullWithFormattedMessage() {
        assertThatThrownBy(() -> Preconditions.notNull(null, "Field {} must not be null", "username"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Field username must not be null");
    }

    @Test
    void notBlankPassesThrough() {
        assertThat(Preconditions.notBlank("hello", "value")).isEqualTo("hello");
    }

    @Test
    void notBlankThrowsOnNull() {
        assertThatThrownBy(() -> Preconditions.notBlank(null, "name"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @Test
    void notBlankThrowsOnEmpty() {
        assertThatThrownBy(() -> Preconditions.notBlank("", "name"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @Test
    void notBlankThrowsOnWhitespace() {
        assertThatThrownBy(() -> Preconditions.notBlank("   ", "name"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @Test
    void minLengthPassesThrough() {
        assertThat(Preconditions.minLength("hello", 3, "value")).isEqualTo("hello");
    }

    @Test
    void minLengthThrowsWhenTooShort() {
        assertThatThrownBy(() -> Preconditions.minLength("hi", 5, "name"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @Test
    void maxLengthPassesThrough() {
        assertThat(Preconditions.maxLength("hi", 10, "value")).isEqualTo("hi");
    }

    @Test
    void maxLengthThrowsWhenTooLong() {
        assertThatThrownBy(() -> Preconditions.maxLength("toolongstring", 5, "name"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @Test
    void inRangePassesThrough() {
        assertThat(Preconditions.inRange(5, 1, 10, "count")).isEqualTo(5);
    }

    @Test
    void inRangeThrowsBelowMin() {
        assertThatThrownBy(() -> Preconditions.inRange(0, 1, 10, "count"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("count");
    }

    @Test
    void inRangeThrowsAboveMax() {
        assertThatThrownBy(() -> Preconditions.inRange(11, 1, 10, "count"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("count");
    }

    @Test
    void inRangeAcceptsBoundaryValues() {
        assertThat(Preconditions.inRange(1, 1, 10, "count")).isEqualTo(1);
        assertThat(Preconditions.inRange(10, 1, 10, "count")).isEqualTo(10);
    }

    @Test
    void positivePassesThrough() {
        assertThat(Preconditions.positive(1, "count")).isEqualTo(1);
    }

    @Test
    void positiveThrowsOnZero() {
        assertThatThrownBy(() -> Preconditions.positive(0, "count"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("count");
    }

    @Test
    void positiveThrowsOnNegative() {
        assertThatThrownBy(() -> Preconditions.positive(-1, "count"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("count");
    }

    @Test
    void notEmptyPassesThrough() {
        List<String> list = List.of("a", "b");
        assertThat(Preconditions.notEmpty(list, "items")).isSameAs(list);
    }

    @Test
    void notEmptyThrowsOnEmptyCollection() {
        assertThatThrownBy(() -> Preconditions.notEmpty(List.of(), "items"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("items");
    }

    @Test
    void notEmptyThrowsOnNull() {
        assertThatThrownBy(() -> Preconditions.notEmpty(null, "items"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("items");
    }

    @Test
    void checkPassesOnTrue() {
        Preconditions.check(true, "should not throw");
    }

    @Test
    void checkThrowsOnFalse() {
        assertThatThrownBy(() -> Preconditions.check(false, "value {} is invalid", "foo"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("value foo is invalid");
    }
}