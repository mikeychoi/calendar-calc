package com.mikeychoi.calendarcalc.repository;

import com.mikeychoi.calendarcalc.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Database access for holidays.
 *
 * Spring Data JPA generates the implementation at startup. JpaRepository already
 * provides save, findById, findAll, deleteById, existsById, etc. The methods below
 * are "derived queries": Spring builds the SQL from the method name.
 */
public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    /** All holidays, earliest first. */
    List<Holiday> findAllByOrderByDateAsc();

    /** Holidays with start <= date <= end (inclusive), earliest first. */
    List<Holiday> findByDateBetweenOrderByDateAsc(LocalDate start, LocalDate end);
}
