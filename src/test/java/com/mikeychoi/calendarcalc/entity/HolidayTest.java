package com.mikeychoi.calendarcalc.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** Plain unit test: no Spring, no database. */
class HolidayTest {

    @Test
    void constructorTrimsNameAndComputesNormalizedName() {
        Holiday holiday = new Holiday(LocalDate.of(2027, 12, 25), "  Christmas Day ");

        assertThat(holiday.getName()).isEqualTo("Christmas Day");
        assertThat(holiday.getNormalizedName()).isEqualTo("christmas day");
    }

    @Test
    void setNameKeepsNormalizedNameInSync() {
        Holiday holiday = new Holiday(LocalDate.of(2027, 12, 25), "Christmas");

        holiday.setName(" XMAS ");

        assertThat(holiday.getName()).isEqualTo("XMAS");
        assertThat(holiday.getNormalizedName()).isEqualTo("xmas");
    }

    @Test
    void namesDifferingOnlyInCaseAndSurroundingSpacesNormalizeToTheSameValue() {
        assertThat(Holiday.normalizeName("  CHRISTMAS "))
                .isEqualTo(Holiday.normalizeName("christmas"));
    }
}
