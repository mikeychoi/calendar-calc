package com.mikeychoi.calendarcalc.repository;

import com.mikeychoi.calendarcalc.entity.Holiday;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Starts only the JPA slice (entity + repository + in-memory H2).
 * Each test runs in a transaction that is rolled back afterwards, so tests don't affect each other.
 */
@DataJpaTest
class HolidayRepositoryTest {

    @Autowired
    private HolidayRepository repository;

    @Test
    void saveGeneratesIdAndStoresTrimmedName() {
        Holiday saved = repository.saveAndFlush(new Holiday(LocalDate.of(2027, 12, 25), "  Christmas  "));

        assertThat(saved.getId()).isNotNull();
        assertThat(repository.findById(saved.getId()))
                .get()
                .extracting(Holiday::getName)
                .isEqualTo("Christmas");
    }

    @Test
    void findByDateBetweenReturnsOnlyThatYearInDateOrder() {
        repository.save(new Holiday(LocalDate.of(2026, 12, 31), "New Year's Eve 2026"));
        repository.save(new Holiday(LocalDate.of(2027, 12, 31), "New Year's Eve"));
        repository.save(new Holiday(LocalDate.of(2027, 1, 1), "New Year's Day"));
        repository.save(new Holiday(LocalDate.of(2027, 7, 5), "Independence Day (observed)"));
        repository.save(new Holiday(LocalDate.of(2028, 1, 1), "New Year's Day 2028"));

        List<Holiday> result = repository.findByDateBetweenOrderByDateAsc(
                LocalDate.of(2027, 1, 1), LocalDate.of(2027, 12, 31));

        // Both boundary dates (Jan 1 and Dec 31) are included; other years are excluded.
        assertThat(result)
                .extracting(Holiday::getDate)
                .containsExactly(
                        LocalDate.of(2027, 1, 1),
                        LocalDate.of(2027, 7, 5),
                        LocalDate.of(2027, 12, 31));
    }

    @Test
    void findByDateBetweenReturnsEmptyListWhenNoHolidaysInYear() {
        repository.save(new Holiday(LocalDate.of(2027, 12, 25), "Christmas"));

        assertThat(repository.findByDateBetweenOrderByDateAsc(
                LocalDate.of(2030, 1, 1), LocalDate.of(2030, 12, 31))).isEmpty();
    }

    @Test
    void findAllByOrderByDateAscReturnsEveryHolidayEarliestFirst() {
        repository.save(new Holiday(LocalDate.of(2028, 1, 1), "New Year's Day 2028"));
        repository.save(new Holiday(LocalDate.of(2027, 12, 25), "Christmas"));

        assertThat(repository.findAllByOrderByDateAsc())
                .extracting(Holiday::getDate)
                .containsExactly(LocalDate.of(2027, 12, 25), LocalDate.of(2028, 1, 1));
    }

    @Test
    void differentHolidaysMayShareADate() {
        LocalDate date = LocalDate.of(2027, 12, 25);
        repository.saveAndFlush(new Holiday(date, "Christmas"));
        repository.saveAndFlush(new Holiday(date, "Company Shutdown"));

        assertThat(repository.findByDateBetweenOrderByDateAsc(date, date)).hasSize(2);
    }

    @Test
    void sameDateAndNameIgnoringCaseAndSpacesIsRejectedByDatabase() {
        LocalDate date = LocalDate.of(2027, 12, 25);
        repository.saveAndFlush(new Holiday(date, "Christmas"));

        assertThatThrownBy(() -> repository.saveAndFlush(new Holiday(date, "  CHRISTMAS ")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
