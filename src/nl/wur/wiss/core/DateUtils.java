/*
 * Copyright 1988, 2013, 2016 Wageningen Environmental Research
 *
 * For licensing information read the included LICENSE.txt file.
 *
 * Unless required by applicable law or agreed to in writing, this software
 * is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied.
 */
package nl.wur.wiss_framework.core;

import nl.wur.wiss_framework.mathutils.RangeUtils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Provides utility methods to work with dates
 *
 * @author Daniel van Kraalingen (daniel.vankraalingen@wur.nl)
 * @version 1
 */
public class DateUtils {

    public static final String CLASSNAME_ST = DateUtils.class.getSimpleName();
    public        final String CLASSNAME_IN = this.getClass().getSimpleName();

    /**
     * Counts the number of days between dateFrom and dateTo
     *
     * @param dateFrom date on which to start counting
     * @param dateTo   date on which to end counting
     * @return the number of days between dateFrom and dateTo
     */
    public static int diffDays(LocalDate dateFrom, LocalDate dateTo) {
        Long diffDays = ChronoUnit.DAYS.between(dateFrom, dateTo);
        return diffDays.intValue();
    }

    /**
     * Returns a LocalDate for the provided year and day in the year.
     *
     * @param aYear the year to convert into a date
     * @param aDayInYear the day in the year to convert into a date
     * @return the required date
     */
    public static LocalDate getLocalDate(int aYear, int aDayInYear) {

        final String methodName = "getLocalDate";

        LocalDate result = LocalDate.of(aYear, 1, 1);

        if (!result.isLeapYear()) {
            if (!RangeUtils.inRange(aDayInYear, 1, 365)) {
                throw new IllegalArgumentException(String.format("%s.%s : Illegal day in year for non leap year (day=%d).",
                                                                 CLASSNAME_ST, methodName, aDayInYear));
            }
        } else {
            if (!RangeUtils.inRange(aDayInYear, 1, 366)) {
                throw new IllegalArgumentException(String.format("%s.%s : Illegal day in year for leap year (day=%d).",
                                                                 CLASSNAME_ST, methodName, aDayInYear));
            }
        }

        return result.plusDays(aDayInYear - 1);
    }
}
