package com.mikeychoi.calendarcalc.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.util.Locale;

/**
 * A holiday stored in the holiday calendar (one row in the "holidays" table).
 *
 * Identity is the database-generated id, not the date: several holidays may share a date.
 * Exact duplicates (same date and same name, ignoring case and surrounding spaces) are
 * not allowed. The unique constraint on (holiday_date, normalized_name) enforces that
 * at the database level.
 */
@Entity
@Table(
        name = "holidays",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_holidays_date_normalized_name",
                columnNames = {"holiday_date", "normalized_name"}
        )
)
public class Holiday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "holiday_date", nullable = false)
    private LocalDate date;

    // Display name: trimmed, original capitalization kept.
    @Column(nullable = false)
    private String name;

    // Trimmed and lower-cased copy of name, used only for duplicate detection.
    @Column(name = "normalized_name", nullable = false)
    private String normalizedName;

    /** Required by JPA, which creates instances via reflection when loading rows. */
    protected Holiday() {
    }

    public Holiday(LocalDate date, String name) {
        this.date = date;
        setName(name);
    }

    /** The single place that defines what makes two holiday names "the same". */
    public static String normalizeName(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    public Long getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getName() {
        return name;
    }

    /** Sets the display name and keeps normalizedName in sync, so the two can never disagree. */
    public void setName(String name) {
        this.name = name.trim();
        this.normalizedName = normalizeName(name);
    }

    public String getNormalizedName() {
        return normalizedName;
    }
}
