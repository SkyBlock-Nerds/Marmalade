package net.hypixel.nerdbot.marmalade.validation;

import net.hypixel.nerdbot.marmalade.exception.FormattedException;
import net.hypixel.nerdbot.marmalade.functional.Result;
import net.hypixel.nerdbot.marmalade.validation.Validator.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidatorTest {

    @Test
    void noErrorsValidatesCleanly() {
        Validator.create()
                .notNull("hello", "name")
                .notBlank("world", "value")
                .validate();
    }

    @Test
    void accumulatesMultipleErrors() {
        Validator validator = Validator.create()
                .notNull(null, "fieldA")
                .notBlank("", "fieldB")
                .notEmpty(List.of(), "fieldC");

        assertThat(validator.getErrors()).hasSize(3);
    }

    @Test
    void validateThrowsWithAllErrors() {
        Validator validator = Validator.create()
                .notNull(null, "fieldA")
                .notBlank("", "fieldB")
                .notNull(null, "fieldC");

        assertThatThrownBy(validator::validate)
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> {
                    ValidationException ve = (ValidationException) ex;
                    assertThat(ve.getErrors()).hasSize(3);
                });
    }

    @Test
    void toResultReturnsSuccessWhenNoErrors() {
        Result<Void, ValidationException> result = Validator.create()
                .notNull("value", "name")
                .toResult();

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void toResultReturnsFailureWithErrors() {
        Result<Void, ValidationException> result = Validator.create()
                .notNull(null, "name")
                .toResult();

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void minLengthAccumulatesError() {
        Validator validator = Validator.create()
                .minLength("hi", 5, "name");

        assertThat(validator.hasErrors()).isTrue();
        assertThat(validator.getErrors()).hasSize(1);
        assertThat(validator.getErrors().get(0).fieldName()).isEqualTo("name");
    }

    @Test
    void maxLengthAccumulatesError() {
        Validator validator = Validator.create()
                .maxLength("toolongstring", 5, "name");

        assertThat(validator.hasErrors()).isTrue();
        assertThat(validator.getErrors()).hasSize(1);
        assertThat(validator.getErrors().get(0).fieldName()).isEqualTo("name");
    }

    @Test
    void inRangeAccumulatesError() {
        Validator validator = Validator.create()
                .inRange(50, 1, 10, "count");

        assertThat(validator.hasErrors()).isTrue();
        assertThat(validator.getErrors()).hasSize(1);
        assertThat(validator.getErrors().get(0).fieldName()).isEqualTo("count");
    }

    @Test
    void notEmptyAccumulatesError() {
        Validator validator = Validator.create()
                .notEmpty(List.of(), "items");

        assertThat(validator.hasErrors()).isTrue();
        assertThat(validator.getErrors()).hasSize(1);
        assertThat(validator.getErrors().get(0).fieldName()).isEqualTo("items");
    }

    @Test
    void checkAccumulatesOnFalse() {
        Validator validator = Validator.create()
                .check(false, "value {} is invalid", "foo");

        assertThat(validator.hasErrors()).isTrue();
        assertThat(validator.getErrors().get(0).message()).isEqualTo("value foo is invalid");
    }

    @Test
    void checkDoesNotAccumulateOnTrue() {
        Validator validator = Validator.create()
                .check(true, "this should not appear");

        assertThat(validator.hasErrors()).isFalse();
    }

    @Test
    void validationExceptionIsFormattedException() {
        Validator validator = Validator.create()
                .notNull(null, "field");

        assertThatThrownBy(validator::validate)
                .isInstanceOf(ValidationException.class)
                .isInstanceOf(FormattedException.class);
    }

    @Test
    void chainingReturnsThis() {
        Validator validator = Validator.create();
        assertThat(validator.notNull("x", "f")).isSameAs(validator);
        assertThat(validator.notBlank("x", "f")).isSameAs(validator);
        assertThat(validator.minLength("hello", 3, "f")).isSameAs(validator);
        assertThat(validator.maxLength("hi", 10, "f")).isSameAs(validator);
        assertThat(validator.inRange(5, 1, 10, "f")).isSameAs(validator);
        assertThat(validator.notEmpty(List.of("a"), "f")).isSameAs(validator);
        assertThat(validator.check(true, "ok")).isSameAs(validator);
    }
}