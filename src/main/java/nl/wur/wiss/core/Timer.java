/*
 * Copyright 1988, 2013, 2016 Wageningen Environmental Research
 *
 * For licensing information read the included LICENSE.txt file.
 *
 * Unless required by applicable law or agreed to in writing, this software
 * is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied.
 */
package nl.wur.wiss.core;

/**
 * Carries out a calendar with daily steps
 *
 * @author Daniel van Kraalingen (daniel.vankraalingen@wur.nl)
 * @version 1
 */
import java.time.LocalDate;
import nl.wur.wiss.mathutils.RangeUtils;

public class Timer {

    public static final String CLASSNAME_ST = Timer.class.getSimpleName();
    public        final String CLASSNAME_IN = this.getClass().getSimpleName();

    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate curDate;
    private LocalDate pauseDate;
    private boolean   pauseDateSet = false;
    private int       duration;
    private boolean   datePeriodSet = false;
    private boolean   terminate = false;

    /**
     * Constructs object
     */
    public Timer() {
    }

    protected void checkDatePeriodSet() {

        final String methodName = "checkDatePeriodSet";

        if (!datePeriodSet) {
            throw new IllegalStateException(String.format("%s.%s : Date period not set", CLASSNAME_IN, methodName));
        }
    }

    @Override
    public String toString() {
        return CLASSNAME_IN + "{startDate=" + startDate + ", endDate=" + endDate + ", curDate" + curDate + "}";
    }

    /**
     * Resets the internal state to the state immediately after construction (so there is
     * no date period set !), works different from reset() method !
     */
    public void clear() {
        datePeriodSet = false;
        terminate = false;
    }

    /**
     * Reinitialises the calendar on startDate
     */
    public void reset() {
        this.checkDatePeriodSet();

        curDate = startDate;
        terminate = false;
    }

    /**
     * Defines the start and end date of simulation, can be set only once !!
     *
     * @param aStartDate date on which to start the simulation
     * @param aEndDate   date on which to end the simulation
     */
    public void setDatePeriod(LocalDate aStartDate, LocalDate aEndDate) {

        final String methodName = "setDatePeriod";

        if (datePeriodSet) {
            throw new IllegalStateException(String.format("%s.%s : Date period cannot be set twice", CLASSNAME_IN, methodName));
        }
        if (aStartDate.isAfter(aEndDate)) {
            throw new IllegalArgumentException(String.format("%s.%s : No period to simulate. End date must be later than start date (Start=%s, End=%s)",
                                                              CLASSNAME_IN, methodName, aStartDate.toString(), aEndDate.toString()));
        }

        startDate = aStartDate;
        endDate   = aEndDate;
        duration  = DateUtils.diffDays(startDate, endDate);

        curDate = startDate;

        datePeriodSet = true;
    }

    /**
     * Defines the start and end date of simulation (through duration),
     * can be set only once !!
     *
     * @param aStartDate date on which to start the simulation
     * @param aDuration the duration in days of the simulation (must be 1 or larger)
     */
    public void setDateDuration(LocalDate aStartDate, int aDuration) {

        final String methodName = "setDateDuration";

        if (datePeriodSet) {
            throw new IllegalStateException(String.format("%s.%s : Date period cannot be set twice", CLASSNAME_IN, methodName));
        }
        if (!(aDuration >= 1)) {
            throw new IllegalArgumentException(String.format("%s.%s : No duration to simulate. Duration (%d) must be 1 or larger than 1",
                                                              CLASSNAME_IN, methodName, aDuration));
        }

        startDate = aStartDate;
        duration  = aDuration;
        endDate   = aStartDate.plusDays(duration);

        curDate = aStartDate;

        datePeriodSet = true;
    }

    /**
     * Increases date by 1, sets terminate() to true when date is beyond end date
     * and then sets date back to end date, so that simulation must stop because of time
     */
    public void dateStep() {

        final String methodName = "dateStep";

        this.checkDatePeriodSet();

        if (terminate) {
            throw new IllegalStateException(String.format("%s.%s : Object already terminated", CLASSNAME_IN, methodName));
        }

        if (curDate.isBefore(endDate) || curDate.isEqual(endDate)) {
            curDate = curDate.plusDays(1);
        }

        if (curDate.isAfter(endDate)) {
            terminate = true;
            curDate = endDate;
        }
    }

    /**
     * @return the current date
     */
    public LocalDate date() {
        this.checkDatePeriodSet();

        return curDate;
    }

    /**
     * @return the current year
     */
    public int year() {
        this.checkDatePeriodSet();

        return curDate.getYear();
    }

    /**
     * @return the current month
     */
    public int month() {
        this.checkDatePeriodSet();

        return curDate.getMonthValue();
    }

    /**
     * @return the current day number in the month (e.g. Feb 12 = 12)
     */
    public int dayInMonth() {
        this.checkDatePeriodSet();

        return curDate.getDayOfMonth();
    }

    /**
     * @return the current day number in the year (e.g. Feb 1 = 32)
     */
    public int dayInYear() {
        this.checkDatePeriodSet();

        return curDate.getDayOfYear();
    }

    /**
     * @return whether the date period has been set
     */
    public boolean datePeriodSet() {
        return datePeriodSet;
    }

    /**
     * @return the start date
     */
    public LocalDate startDate() {
        this.checkDatePeriodSet();

        return startDate;
    }

    /**
     * @return the end date
     */
    public LocalDate endDate() {
        this.checkDatePeriodSet();

        return endDate;
    }

    /**
     * @return the simulation duration from start to end date (e.g. from Jan 1 to Jan 2 is 1 day)
     */
    public int duration() {
        this.checkDatePeriodSet();

        return duration;
    }

    /**
     * @return the number of days that have passed since start of date stepping
     * (e.g. from March 10 to March 11 is 1 day)
     */
    public int elapsed() {
        this.checkDatePeriodSet();

        return DateUtils.diffDays(startDate, curDate);
    }

    /**
     * @return flag that indicates whether simulation must stop
     */
    public boolean terminate() {
        return terminate;
    }

    /**
     *
     * @return whether on start date
     */
    public boolean isOnStartDate() {
        this.checkDatePeriodSet();

        return curDate.equals(startDate);
    }

    /**
     *
     * @return whether on end date
     */
    public boolean isOnEndDate() {
        this.checkDatePeriodSet();

        return curDate.equals(endDate);
    }

    /**
     * Sets date on which a pause is required (in conjunction with setting a breakpoint)
     * @param aPauseDate the date on which to pause
     */
    public void setPauseDate(LocalDate aPauseDate) {

        final String methodName = "setPauseDate";

        this.checkDatePeriodSet();

        if (!RangeUtils.inRange(aPauseDate, startDate, endDate)) {
            throw new IllegalArgumentException(String.format("%s.%s : Pause date (%s) not within start and end date (Start=%s, End=%s)",
                                                             CLASSNAME_IN, methodName, aPauseDate.toString(), startDate.toString(), endDate.toString()));
        }

        this.pauseDate = aPauseDate;
        pauseDateSet   = true;
    }

    /**
     * @return whether the internal date is equal to or after a pause date (when set)
     */
    public boolean pauseNow() {
        if (pauseDateSet) {
            return (curDate.equals(pauseDate) || curDate.isAfter(pauseDate));
        } else {
            return false;
        }
    }
}
