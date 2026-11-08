package com.mikeychoi.calendarcalc.dto;

/**
 * How days are counted in an add/subtract calculation.
 *
 * In JSON this is the plain string "CALENDAR" or "BUSINESS"; Jackson converts it to
 * the enum automatically and rejects any other value.
 */
public enum CalculationType {

    /** Every day counts, including weekends and holidays. */
    CALENDAR,

    /** Only weekdays that are not in the holiday calendar count. */
    BUSINESS
}
