package com.mikeychoi.calendarcalc.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks the validation annotations on the date-calculation request DTOs directly with
 * the Jakarta Validator (the same engine @Valid uses in a controller), without Spring.
 */
class DateCalculationDtoValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    private static final LocalDate START = LocalDate.of(2026, 11, 7);

    @BeforeAll
    static void createValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    // ---------- DateAddSubtractRequest ----------

    @Test
    void validAddSubtractRequestHasNoViolations() {
        assertThat(validator.validate(new DateAddSubtractRequest(START, 10, CalculationType.CALENDAR))).isEmpty();
    }

    @Test
    void numDaysBoundariesZeroAndOneHundredThousandAreAllowed() {
        assertThat(validator.validate(new DateAddSubtractRequest(START, 0, CalculationType.BUSINESS))).isEmpty();
        assertThat(validator.validate(new DateAddSubtractRequest(START, 100_000, CalculationType.BUSINESS))).isEmpty();
    }

    @Test
    void negativeNumDaysIsRejected() {
        assertThat(messages(validator.validate(new DateAddSubtractRequest(START, -1, CalculationType.CALENDAR))))
                .containsExactly("numDays must be greater than or equal to 0");
    }

    @Test
    void numDaysAboveOneHundredThousandIsRejected() {
        assertThat(messages(validator.validate(new DateAddSubtractRequest(START, 100_001, CalculationType.CALENDAR))))
                .containsExactly("numDays must be less than or equal to 100000");
    }

    @Test
    void missingFieldsAreEachReported() {
        assertThat(messages(validator.validate(new DateAddSubtractRequest(null, null, null))))
                .containsExactlyInAnyOrder(
                        "startDate must not be null",
                        "numDays must not be null",
                        "calcType must not be null");
    }

    // ---------- DateDifferenceRequest ----------

    @Test
    void validDifferenceRequestHasNoViolations() {
        assertThat(validator.validate(new DateDifferenceRequest(START, START.plusDays(10)))).isEmpty();
    }

    @Test
    void differenceRequestRequiresBothDates() {
        assertThat(messages(validator.validate(new DateDifferenceRequest(null, null))))
                .containsExactlyInAnyOrder("startDate must not be null", "endDate must not be null");
    }

    /** Formats violations the same way GlobalExceptionHandler does: "field message". */
    private static <T> Set<String> messages(Set<ConstraintViolation<T>> violations) {
        return violations.stream()
                .map(v -> v.getPropertyPath() + " " + v.getMessage())
                .collect(java.util.stream.Collectors.toSet());
    }
}
