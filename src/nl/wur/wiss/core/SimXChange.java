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

import nl.wur.wiss_framework.mathutils.Interpolator;
import nl.wur.wiss_framework.mathutils.RangeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.*;

import static java.lang.Double.max;
import static java.lang.Double.min;
import static java.lang.Integer.max;

/**
 * Class enabling dynamic exchange of data
 *
 * @author Daniel van Kraalingen (daniel.vankraalingen@wur.nl)
 * @version 1
 */
public class SimXChange {

    public static final String CLASSNAME_ST = SimXChange.class.getSimpleName();
    public        final String CLASSNAME_IN = this.getClass().getSimpleName();

    // class logger
    private static final Log LOGGER = LogFactory.getLog(SimXChange.class);

    static final int MISSINGINDEX = Integer.MIN_VALUE;

    /** enum to specify numerical aggregation on time dependent data */
    public static enum AggregationY {
        /** the first valid value 'ever' */
        FIRST,
        /** the last valid value 'ever' */
        LAST,
        /** the lowest value 'ever' */
        MIN,
        /**  the highest value 'ever' */
        MAX,
        /** the count of values */
        COUNT,
        /** the sum of values */
        SUM,
        /** the average of values */
        AVERAGE,
        /** the change in value from the first to the last (result = last - first) */
        DELTA,
        /** the difference between the highest and lowest value 'ever' (result = max - min) */
        RANGE
    }

    /** enum to specify aggregate on time itself */
    public static enum AggregationDate {
        /** the date of the first valid value 'ever' */
        FIRST,
        /** the date of the last valid value 'ever' */
        LAST,
        /** the date of the lowest value 'ever' */
        MIN,
        /** the date of the highest value 'ever' */
        MAX
    }

    public static enum SimIDState {
        /** to do */
        RUNNING,
        /** to do */
        TERMINATED_NORMALLY,
        /** to do */
        TERMINATED_ERROR
    }

    private static class Aggregation {
        final SimIDVarNameListItem item;

        final int firstIndex;
        final int lastIndex;

        public Aggregation(SimIDVarNameListItem aSimIDVarNameListItem, int aFirstIndex, int aLastIndex) {
            item = aSimIDVarNameListItem;

            firstIndex = aFirstIndex;
            lastIndex  = aLastIndex;
        }

        /**
         * Info function to search for first non-missing Y.
         *
         * @return value of first non-missing Y, or NaN if all values were missing
         */
        public double firstY() {
            if (item.isAggregated) {
                return item.first;
            } else {
                for (int i = firstIndex; i <= lastIndex; i++) {
                    if (item.hasValues[i]) {
                        return item.varValues[i];
                    }
                }

                return Double.NaN;
            }
        }

        /**
         * Info function to search for last non-missing Y.
         *
         * @return value of first non-missing Y, or NaN if all values were missing
         */
        public double lastY() {
            if (item.isAggregated) {
                return item.last;
            } else {
                for (int i = lastIndex; i >= firstIndex; i--) {
                    if (item.hasValues[i]) {
                        return item.varValues[i];
                    }
                }
                return Double.NaN;
            }
        }

        /**
         * Info function to search for array index of first non-missing.
         *
         * @return index of first non-missing, or MISSINGINDEX if all values were missing
         */
        public int firstXIndex() {
            for (int i = firstIndex; i <= lastIndex; i++) {
                if (item.hasValues[i]) {
                    return i;
                }
            }
            return SimXChange.MISSINGINDEX;
        }

        /**
         * Info function to search for array index of last non-missing.
         *
         * @return index of last non-missing, or MISSINGINDEX if all values were missing
         */
        public int lastXIndex() {
            for (int i = lastIndex; i >= firstIndex; i--) {
                if (item.hasValues[i]) {
                    return i;
                }
            }
            return SimXChange.MISSINGINDEX;
        }

        /**
         * Info function to search for lowest value.
         *
         * @return lowest Y value, or NaN if all values were missing
         */
        public double minY() {
            if (item.isAggregated) {
                return item.min;
            } else {
                double result = Double.MAX_VALUE;
                for (int i = firstIndex; i <= lastIndex; i++) {
                    if (item.hasValues[i]) {
                        result = min(result, item.varValues[i]);
                    }
                }
                if (result == Double.MAX_VALUE) {
                    // no value found
                    result = Double.NaN;
                }
                return result;
            }
        }

        /**
         * Info function to search for highest value.
         *
         * @return highest Y value, or NaN if all values were missing
         */
        public double maxY() {
            if (item.isAggregated) {
                return item.max;
            } else {
                double result = Double.MIN_VALUE;
                for (int i = firstIndex; i <= lastIndex; i++) {
                    if (item.hasValues[i]) {
                        result = max(result,  item.varValues[i]);
                    }
                }
                if (result == Double.MIN_VALUE) {
                    // no value found
                    result = Double.NaN;
                }
                return result;
            }
        }

        /**
         * Info function to search for array index of lowest value.
         *
         * @return first index of X, or MISSINGINDEX if all values were missing
         */
        public int minXIndex() {
            if (item.isAggregated) {
                return item.minIndex;
            } else {
                int result = SimXChange.MISSINGINDEX;
                double vLeast = Double.MAX_VALUE;
                for (int i = firstIndex; i <= lastIndex; i++) {
                    if (item.hasValues[i]) {
                        if (item.varValues[i] < vLeast) {
                            vLeast = item.varValues[i];
                            result = i;
                        }
                    }
                }
                return result;
            }
        }

        /**
         * Info function to search for array index of highest value,
         *
         * @return last index of X, or MISSINGINDEX if all values were missing
         */
        public int maxXIndex() {
            if (item.isAggregated) {
                return item.maxIndex;
            } else {
                int result = SimXChange.MISSINGINDEX;
                double vHighest = Double.MIN_VALUE;
                for (int i = firstIndex; i <= lastIndex; i++) {
                    if (item.hasValues[i]) {
                        if (item.varValues[i] > vHighest) {
                            vHighest = item.varValues[i];
                            result = i;
                        }
                    }
                }
                return result;
            }
        }

        /**
         * Info function to sum all available values.
         *
         * @return sum of Y values, or return NaN if all values were missing
         */
        public double sumY() {
            if (item.isAggregated) {
                return item.sum;
            } else {
                double  result = 0.0;
                boolean fnd    = false;
                for (int i = firstIndex; i <= lastIndex; i++) {
                    if (item.hasValues[i]) {
                        fnd = true;
                        result = result + item.varValues[i];
                    }
                }
                if (!fnd) {
                    result = Double.NaN;
                }
                return result;
            }
        }

        /**
         * Info function to count of available values.
         *
         * @return count of all values, or 0 if all values were missing
         */
        public double countY() {
            if (item.isAggregated) {
                return item.count;
            } else {
                int result = 0;
                for (int i = firstIndex; i <= lastIndex; i++) {
                    if (item.hasValues[i]) {
                        result++;
                    }
                }
                return result;
            }
        }

        /**
         * @return the difference between the last and the first Y
         */
        public double deltaY() {
            return this.lastY() - this.firstY();
        }
    }

    /**
     * the type of the token that is required for the operation to be successful
     */
    private static enum TypeRequirement {
        STATE,
        AUXILIARY,
        NOREQUIREMENT
    }

    /**
     * nested class, defines single object for systemVarList
     */
    public static class SimIDVarNameListItem {
        public String         simIDVarName;
        public String         simID;
        public String         varName;
        public int            simIDListIndex;
        public boolean        isState;
        public ScientificUnit scientificUnit;
        public double         lowerBound;
        public boolean        lowerBoundInclusive;
        public double         upperBound;
        public boolean        upperBoundInclusive;
        public boolean        isLocked;
        public double         varChange;
        public boolean[]      hasValues;    // always allocated, flag indicates whether valid value is at index
        public boolean        isAggregated; // whether only aggregations are stored instead of the whole time series (saves memory in array varValues)
        public double[]       varValues;    // only allocated with isAggregated = false (saves memory)
        public int            count;        // only used with isAggregated = true
        public double         first;        // only used with isAggregated = true
        public double         previous;     // only used with isAggregated = true
        public double         last;         // only used with isAggregated = true
        public double         min;          // only used with isAggregated = true
        public int            minIndex;     // only used with isAggregated = true
        public double         max;          // only used with isAggregated = true
        public int            maxIndex;     // only used with isAggregated = true
        public double         sum;          // only used with isAggregated = true
        /**
         * Default constructor
         */
        public SimIDVarNameListItem() {

        }

        /**
         * Constructor to create new object with same properties as aItemToClone
         *
         * @param aItemToClone the object to create the clone of
         */
        public SimIDVarNameListItem(SimIDVarNameListItem aItemToClone) {

            final String methodName = "SimIDVarNameListItem";

            if (aItemToClone == null) {
                throw new IllegalArgumentException(String.format("%s.%s : empty item to clone", this.getClass().getSimpleName(), methodName));
            }

            // repeat all fields
            this.simIDVarName        = aItemToClone.simIDVarName;
            this.simID               = aItemToClone.simID;
            this.varName             = aItemToClone.varName;
            this.simIDListIndex      = aItemToClone.simIDListIndex;
            this.isState             = aItemToClone.isState;
            this.scientificUnit      = aItemToClone.scientificUnit;
            this.lowerBound          = aItemToClone.lowerBound;
            this.lowerBoundInclusive = aItemToClone.lowerBoundInclusive;
            this.upperBound          = aItemToClone.upperBound;
            this.upperBoundInclusive = aItemToClone.upperBoundInclusive;
            this.isLocked            = aItemToClone.isLocked;
            this.varChange           = aItemToClone.varChange;
            this.hasValues           = new boolean[aItemToClone.hasValues.length];
            System.arraycopy(aItemToClone.hasValues, 0, this.hasValues, 0, aItemToClone.hasValues.length);
            this.isAggregated        = aItemToClone.isAggregated;
            this.varValues           = new double[aItemToClone.varValues.length];
            System.arraycopy(aItemToClone.varValues, 0, this.varValues, 0, aItemToClone.varValues.length);
            this.count               = aItemToClone.count;
            this.first               = aItemToClone.first;
            this.previous            = aItemToClone.previous;
            this.last                = aItemToClone.last;
            this.min                 = aItemToClone.min;
            this.minIndex            = aItemToClone.minIndex;
            this.max                 = aItemToClone.max;
            this.maxIndex            = aItemToClone.maxIndex;
            this.sum                 = aItemToClone.sum;
        }
    }

    /**
     * Nested class, defines single registered simIDListIndex.
     */
    public static class SimIDListItem {
        public String     simID;
        public String     simObjectClassName;
        public int        startDateIndex;
        public int        endDateIndex;
        public SimIDState simIDState;
        public String     simIDMsg;

        /**
         * Default constructor
         */
        public SimIDListItem() {

        }

        /**
         * Constructor to create new object with same properties as
         * aItemToClone.
         *
         * @param aItemToClone the object to create the clone of
         */
        public SimIDListItem(SimIDListItem aItemToClone) {

            final String methodName = "SimIDListItem";

            if (aItemToClone == null) {
                throw new IllegalArgumentException(String.format("%s.%s : empty item to clone", this.getClass().getSimpleName(), methodName));
            }

            // repeat all fields
            this.simID              = aItemToClone.simID;
            this.simObjectClassName = aItemToClone.simObjectClassName;
            this.startDateIndex     = aItemToClone.startDateIndex;
            this.endDateIndex       = aItemToClone.endDateIndex;
            this.simIDState         = aItemToClone.simIDState;
            this.simIDMsg           = aItemToClone.simIDMsg;
        }
    }

    /**
     * Nested class, defines single object for stateVarForceValues.
     */
    public static class StateVarForceItem {
        public int    dateIndex;
        public int    varListIndex;
        public double oldValue;
        public double newValue;

        /**
         * Default constructor
         */
        public StateVarForceItem() {

        }

        /**
         * Constructor to create new object with same properties as
         * aItemToClone.
         *
         * @param aItemToClone the object to create the clone of
         */
        public StateVarForceItem(StateVarForceItem aItemToClone) {

            final String methodName = "StateVarForceItem";

            if (aItemToClone == null) {
                throw new IllegalArgumentException(String.format("%s.%s : empty item to clone",  this.getClass().getSimpleName(), methodName));
            }

            // repeat all fields
            this.dateIndex    = aItemToClone.dateIndex;
            this.varListIndex = aItemToClone.varListIndex;
            this.oldValue     = aItemToClone.oldValue;
            this.newValue     = aItemToClone.newValue;
        }
    }

    /**
     * the runID given to this SimXChange object
     */
    private final String runID;

    static final int INITIALSIMIDCNT   = 10;
    static final int INITIALVARNAMECNT = 100;
    static final int INITIALFULLTIMESERIESCNT = 10;

    // object fields
    private boolean   datePeriodSet = false;
    private LocalDate startDate     = null;
    private LocalDate endDate       = null;
    private int       endDateIndex  = -1; // startDateIndex not necessary, would be zero anyhow

    private LocalDate curDate       = null;
    private int       curDateIndex  = -1; // 0 = start date, 1 = start date + 1, etcetera
    private final int indexOffset; // required for token generation

    // data structures for simIDListIndex storage (simIDMap contains index locations for simIDList)
    private final HashMap<String, Integer> simIDMap;
    private final ArrayList<SimIDListItem> simIDList; // contains simIDListIndex metadata

    // data structures for simIDListIndex.varName storage (simIDVarNameMap contains index locations for simIDVarNameList)
    private final HashMap<String, Integer>          simIDVarNameMap;
    private final ArrayList<SimIDVarNameListItem>   simIDVarNameList; // contains simIDListIndex.varName metadata

    // stores unique VarName's and array locations where it occurs (can be more than one)
    private final HashMap<String, ArrayList<Integer>> varNameMap;

    // stores forced states
    private final ArrayList<StateVarForceItem> stateVarForceValues;

    private final HashSet<String> fullTimeSeriesList;

    private boolean traceLogging = false;

    private boolean pause = false;

    /**
     * Constructs object
     *
     * @param aID identifier this object should use in reporting etc.
     */
    public SimXChange(String aID) {

        // generates random number between 0 and Integer.MAX_VALUE - 1
        // so indexOffset = -Integer.MAX_VALUE is never generated
        Random rndTmp = new Random();
        indexOffset = -1 * rndTmp.nextInt(Integer.MAX_VALUE);

        simIDMap  = new HashMap<>(INITIALSIMIDCNT);
        simIDList = new ArrayList<>(INITIALSIMIDCNT);

        simIDVarNameMap  = new HashMap<>(INITIALVARNAMECNT);
        simIDVarNameList = new ArrayList<>(INITIALVARNAMECNT);

        varNameMap          = new HashMap<>(INITIALVARNAMECNT);
        stateVarForceValues = new ArrayList<>(INITIALVARNAMECNT);

        fullTimeSeriesList = new HashSet<>(INITIALFULLTIMESERIESCNT);

        runID = aID;
    }

    // private methods ---------------------------------------------------------

    /**
     * Checks that the datePeriodSet flag is set, throws exception if not.
     *
     * @param aCaller the name of the method that called this method (used in
     * exception messages)
     * @throws IllegalStateException if the date period is not set.
     */
    private void checkDatePeriodSet(String aCaller) {

        if (!datePeriodSet) {
            throw new IllegalStateException(String.format("%s.%s: Date period not set.",
                                                           CLASSNAME_IN, aCaller));
        }
    }

    /**
     * Create one concatenated string from aSimID and aVarName.
     *
     * @param aSimID the SimID to be used
     * @param aVarName the VarName to be used
     * @return the special concatenation of aSimID and aVarName
     */
    private static String composeSimIDVarName(String aSimID, String aVarName) {
        // do not use String.format, this is much faster !
        return (aSimID + "." + aVarName);
    }

    /**
     * Info function to find location of aSimIDVarName (case sensitive) in
     * SimIDVarNameMap.
     * <p>
     * Returns positive value of simIDVarNameList index if lookup aSimIDVarName
     * was successful, otherwise returns MISSINGINDEX.
     * </p>
     *
     * @param aSimIDVarName special concatenated SimID VarName combination to
     * look for
     * @return index location in simIDVarNameList
     */
    private int getSimIDVarNameListIndex(String aSimIDVarName) {

        Integer index = simIDVarNameMap.get(aSimIDVarName);
        if (index != null) {
            return index;
        } else {
            return SimXChange.MISSINGINDEX;
        }
    }

    /**
     * Info function to find location of aSimID (case insensitive) in simIDMap.
     *
     * <p>
     * Returns positive value of simIDList index if lookup was successful,
     * otherwise returns MISSINGINDEX.
     * </p>
     *
     * @param aSimID the SimID to use
     * @return index location in simIDMap of aSimID
     */
    private int getSimIDListIndex(String aSimID) {

        Integer index = simIDMap.get(aSimID.toUpperCase(Locale.US));
        if (index != null) {
            return index;
        } else {
            return SimXChange.MISSINGINDEX;
        }
    }

    /**
     * Checks whether aToken points to a valid location, return that location if
     * it is valid.
     *
     * @param aToken the token to check
     * @param aCaller the name caller of the caller, to be used in exception
     * messages
     * @param aCheckCanWrite if true, whether the token can be used to write
     * data
     * @param aCheckType the type of the token to be checked
     *
     * @return the index of aToken's location
     */
    private int checkValidToken(int aToken, String aCaller, boolean aCheckCanWrite, TypeRequirement aCheckType) {

        if (!this.isValidToken(aToken)) {
            throw new IllegalArgumentException(String.format("%s.%s : The token %d is not valid.",
                                                              CLASSNAME_IN, aCaller, aToken));
        }

        final int index = this.decodeTokenToIndex(aToken);

        if (!RangeUtils.inRange(index, 0 , (simIDVarNameList.size() - 1))) {
            throw new IllegalArgumentException(String.format("%s.%s : The token %d is not valid.",
                                                              CLASSNAME_IN, aCaller, aToken));
        }

        if (aCheckCanWrite) {
            if (!this.tokenCanWrite(aToken)) {
                throw new IllegalArgumentException(String.format("%s.%s : The token %d for %s cannot be used to write.",
                                                                  CLASSNAME_IN, aCaller, aToken, simIDVarNameList.get(index).simIDVarName));
            }
        }

        final SimIDVarNameListItem item = simIDVarNameList.get(index);

        if (aCheckCanWrite) {
            if (item.isLocked) {
                throw new IllegalArgumentException(String.format("%s.%s : The token %d for %s cannot be used to write because the variable is locked.",
                                                                  CLASSNAME_IN, aCaller, aToken, simIDVarNameList.get(index).simIDVarName));
            }
        }

        if (item.isState) {
            if (aCheckType == TypeRequirement.AUXILIARY) {
                throw new IllegalArgumentException(String.format("%s.%s : The token %d for %s is a state variable, not an auxiliary variable.",
                                                                 CLASSNAME_IN, aCaller, aToken, item.simIDVarName));
            }
        } else {
            if (aCheckType == TypeRequirement.STATE) {
                throw new IllegalArgumentException(String.format("%s.%s : The token %d for %s is an auxiliary variable, not a state variable.",
                                                                 CLASSNAME_IN, aCaller, aToken, item.simIDVarName));
            }
        }

        return index;
    }

    /**
     * Info function to test whether aValue is within the range as given in
     * aSimIDVarNameListItem.
     *
     * @param aSimIDVarNameListItem item containing registration info
     * @param aValue the value to be checked
     * @return flag whether aValue is within the range of aSimIDVarNameListItem
     */
    private static boolean inRange(SimIDVarNameListItem aSimIDVarNameListItem, double aValue) {

        // RangeUtils.inRange checks are inclusive
        boolean result = RangeUtils.inRange(aValue, aSimIDVarNameListItem.lowerBound, aSimIDVarNameListItem.upperBound);

        if (result) {
            // aValue is within range, but maybe hits an exclusive bound
            if (!aSimIDVarNameListItem.lowerBoundInclusive && (aValue == aSimIDVarNameListItem.lowerBound)) {
                result = false;
            }
            else if ((!aSimIDVarNameListItem.upperBoundInclusive) && (aValue == aSimIDVarNameListItem.upperBound)) {
                result = false;
            }
        }

        return result;
    }

    /**
     * Info function that flags whether item contains active data on the current
     * simulation + aDelta.
     *
     * @param item item to do the test on
     * @param aDelta offset to use for the evaluation
     * @return flag whether item is active on curent date + aDelta
     */
    private boolean varActive(SimIDVarNameListItem item, int aDelta) {

        final String methodName = "varActive";

        final int indexDate = curDateIndex + aDelta;

        if (!this.isValidDateIndex(indexDate)) {
            throw new IllegalArgumentException(String.format("%s.%s : The delta value (%d) is not valid.",
                                                              CLASSNAME_IN, methodName, aDelta));
        }

        if (indexDate >= 0) {
            return item.hasValues[indexDate];
        } else {
            return false;
        }
    }

    /**
     * Info function that flags whether the item is active on current date +
     * delta.
     *
     * @param item item to do the test on
     * @param aDelta offset to use for the evaluation
     * @return the required value
     */
    private boolean simIDActive(SimIDVarNameListItem item, int aDelta) {

        final String methodName = "simIDActive";

        final int indexDate = curDateIndex + aDelta;

        if (!this.isValidDateIndex(indexDate)) {
            throw new IllegalArgumentException(String.format("%s.%s : The delta value (%d) is not valid.",
                                                              CLASSNAME_IN, methodName, aDelta));
        }

        if (indexDate >= 0) {
            final int indexDateEnd = simIDList.get(item.simIDListIndex).endDateIndex;
            if ((indexDateEnd == SimXChange.MISSINGINDEX) || (indexDate <= indexDateEnd)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Info function to encode aIndex into a token.
     *
     * @param aIndex the index to encode
     * @param aCanWrite whether the token must have write privilege
     * @return the encoded token value
     */
    private int encodeIndexToToken(int aIndex, boolean aCanWrite){

        int result = indexOffset + 2 * aIndex;
        if (!aCanWrite) {
            result++;
        }

        return result;
    }

    /**
     * Info function to decode aToken to an index location.
     *
     * @param aToken the token to decode
     * @return the index location corresponding with aToken
     */
    private int decodeTokenToIndex(int aToken) {

        final int tmp = aToken - indexOffset;
        return (tmp - tmp % 2) / 2;
    }

    /**
     * Info function to test whether aToken has write privilege.
     *
     * @param aToken the token to use
     * @return whether aToken has write privilege
     */
    private boolean tokenCanWrite(int aToken) {

        final int tmp = aToken - indexOffset;
        return ((tmp % 2) == 0);
    }

    /**
     * Info function to return whether a value for aVarName is not there.
     * <p>
     * Across all simIDListIndex's except simIDListIndex=aSimIDToIgnore), or
     * does not have proper values for the current time (function returns true)
     * if a proper value is found for some simIDListIndex, the function returns
     * false, aVarName and aSimIDToIgnore should be uppercased by the caller !
     * </p>
     *
     * @param aVarName variable name to test
     * @param aSimIDToIgnore SimID to ignore
     * @return the result of the test
     */
    private boolean isVarNameValueMissing(String aVarName, String aSimIDToIgnore) {

        boolean result = true;

        if (varNameMap.containsKey(aVarName)) {
            final ArrayList<Integer> arr = varNameMap.get(aVarName);

            result = true;
            for (int i = 0; i <= (arr.size() - 1); i++) {
                final SimIDVarNameListItem item = simIDVarNameList.get(arr.get(i));

                if (!item.simID.equals(aSimIDToIgnore)) {
                    if (this.varActive(item, 0)) {
                        result = false;
                        return result;
                    }
                }
            }
        }

        return result;
    }

    /**
     * Register aSimID and aVarName and return the token.
     * <p>
     * The aSimID/aVarName can exist, in that case aNativeScientificUnit and
     * aIsState must not change and the registration should have been released,
     * the returned token enables setting rates with the token if it is a state.
     * </p>
     *
     * @param aSimID the SimID to register
     * @param aVarName the variable name to register with the SimID
     * @param aNativeScientificUnit the native scientific unit of the
     * registration
     * @param aLowerBound the allowed lower bound for all values passed
     * @param aUpperBound the allowed upper bound for all values passed
     * @param aIsState whether the registered variable is a state or not
     * (false=auxiliary variable)
     * @param aCaller the name of the caller (used for readability of
     * exceptions)
     * @return the token for the registration
     */
    private int registerSimIDVarName(String aSimID, String aVarName, ScientificUnit aNativeScientificUnit, double aLowerBound, double aUpperBound, boolean aIsState, String aCaller) {

        if (StringUtils.isBlank(aSimID)) {
            throw new IllegalArgumentException(String.format("%s.%s : The simid of variable %s is empty.",
                                                              CLASSNAME_IN, aCaller, aVarName));
        }

        if (StringUtils.isBlank(aVarName)) {
            throw new IllegalArgumentException(String.format("%s.%s : The variable of simid %s is empty.",
                                                              CLASSNAME_IN, aCaller, aSimID));
        }

        if (aLowerBound > aUpperBound) {
            throw new IllegalArgumentException(String.format("%s.%s : The lower bound (%g) of %s.%s is larger than the upperbound (%g).",
                                                              CLASSNAME_IN, aCaller, aLowerBound, aSimID, aVarName, aUpperBound));
        }
        final String simID   = aSimID.toUpperCase(Locale.US);

        final int simIDListIndex = this.getSimIDListIndex(simID);

        if (simIDListIndex == SimXChange.MISSINGINDEX) {
            throw new IllegalArgumentException(String.format("%s.%s : The simid %s of variable %s is not registered.",
                                                              CLASSNAME_IN, aCaller, aSimID, aVarName));
        }

        final String varName = aVarName.toUpperCase(Locale.US);

        final String simIDVarName = this.composeSimIDVarName(simID, varName);

        int simIDVarNameListIndex = this.getSimIDVarNameListIndex(simIDVarName);

        if (simIDVarNameListIndex == SimXChange.MISSINGINDEX) {
            // first registration, returned token which will give write access to rate

            // lock writes to all aVarName's registered by other simIDListIndex's
            for (int i = 0; i <= (simIDVarNameList.size() - 1); i++) {
                if (simIDVarNameList.get(i).varName.equals(varName)) {
                    simIDVarNameList.get(i).isLocked = true;
                }
            }

            // add item to simIDVarNameMap, simIDVarNameList and varNameMap

            final SimIDVarNameListItem item = new SimIDVarNameListItem();
            item.isState                    = aIsState;
            item.simIDVarName               = simIDVarName;
            item.simID                      = simID;
            item.varName                    = varName;
            item.simIDListIndex             = simIDListIndex;
            item.scientificUnit             = aNativeScientificUnit;
            item.isLocked                   = false;

            if (aLowerBound == Double.MIN_VALUE) {
                item.lowerBound          = 0.0;
                item.lowerBoundInclusive = false;
            } else {
                item.lowerBound          = aLowerBound;
                item.lowerBoundInclusive = true;
            }

            if (aUpperBound == -Double.MIN_VALUE) {
                item.upperBound          = 0.0;
                item.upperBoundInclusive = false;
            } else {
                item.upperBound          = aUpperBound;
                item.upperBoundInclusive = true;
            }

            // allocation and initial values for item.varValues
            item.varValues = new double[(this.maxDuration() + 1)];
            item.hasValues = new boolean[(this.maxDuration() + 1)];
            for (int i = 0; i <= (item.varValues.length - 1); i++) {
                item.varValues[i] = Double.NaN;
                item.hasValues[i] = false;
            }

            // new item and initial empty values
            item.varChange    = Double.NaN;
            item.isAggregated = !fullTimeSeriesList.contains(aVarName);
            item.count        = 0;
            item.first        = Double.NaN;
            item.previous     = Double.NaN;
            item.last         = Double.NaN;
            item.min          = Double.NaN;
            item.minIndex     = SimXChange.MISSINGINDEX;
            item.max          = Double.NaN;
            item.maxIndex     = SimXChange.MISSINGINDEX;
            item.sum          = Double.NaN;

            // add to lists at the end
            simIDVarNameList.add(item);

            simIDVarNameListIndex = simIDVarNameList.size() - 1;
            simIDVarNameMap.put(simIDVarName, simIDVarNameListIndex);

            // add to VarNameMap
            ArrayList<Integer> arr;
            if (varNameMap.containsKey(varName)) {
                arr = varNameMap.get(varName);
            } else {
                arr = new ArrayList<>(INITIALVARNAMECNT);
            }
            arr.add(simIDVarNameListIndex);
            varNameMap.put(varName, arr);
        } else {
            throw new IllegalArgumentException(String.format("%s.%s : The variable %s is already registered.",
                                                                      CLASSNAME_IN, aCaller, simIDVarName));
        }

        return this.encodeIndexToToken(simIDVarNameListIndex, true);
    }

    /**
     * Add a forced value to the list of forced values.
     *
     * @param aIndex xxx
     * @param aOldValue the old value
     * @param aNewValue the new (=forced) value
     */
    private void addStateVarForceItem(int aIndex, double aOldValue, double aNewValue) {

        final StateVarForceItem item = new StateVarForceItem();

        item.dateIndex    = curDateIndex;
        item.varListIndex = aIndex;
        item.oldValue     = aOldValue;
        item.newValue     = aNewValue;

        stateVarForceValues.add(item);
    }

    /**
     * Info function to compose a range caption for the item.
     *
     * @param aSimIDVarNameListItem the item to compose the caption for
     * @return the range caption
     */
    private String rangeCaption(SimIDVarNameListItem aSimIDVarNameListItem) {

        String result = "(";

        result = result + String.format("%g", aSimIDVarNameListItem.lowerBound);
        if (aSimIDVarNameListItem.lowerBoundInclusive) {
            result = result + " <=";
        } else {
            result = result + " <";
        }

        result = result + " x ";

        if (aSimIDVarNameListItem.upperBoundInclusive) {
            result = result + "<= ";
        } else {
            result = result + "< ";
        }

        result = result + String.format("%g", aSimIDVarNameListItem.upperBound);

        result = result + ")";

        return result;
    }

    // public methods ----------------------------------------------------------
    // every public method should consider uppercasing string arguments before
    // further processing

    /**
     * Switch trace logging on or off.
     *
     * @param aTraceLogging flag to switch trace logging on (true) or off (false)
     */
    public void setTraceLogging(boolean aTraceLogging) {
        this.traceLogging = aTraceLogging;
    }

    /**
     * Info function whether the program should pause.
     * <p>
     * Works only in debug mode and requires breakpoints to exist
     * </p>
     *
     * @param aPause whether pausing should happen asap
     */
    public void setPause(boolean aPause) {
        this.pause = aPause;
    }

    /**
     * Info function whether an execution pause must happen now.
     *
     * @return whether a pause should happen now
     */
    public boolean pauseNow() {
        return pause;
    }

    /**
     * Defines the start and end date of simulation, can be set only once !!
     *
     * @param aStartDate start date of simulation
     * @param aEndDate end date of simulation
     */
    public void setDatePeriod(LocalDate aStartDate, LocalDate aEndDate) {

        final String methodName = "setDatePeriod";

        if (datePeriodSet) {
            throw new IllegalStateException(String.format("%s.%s : Date period cannot be set twice.",
                                                           CLASSNAME_IN, methodName));
        }
        if (aStartDate.isAfter(aEndDate)) {
            throw new IllegalArgumentException(String.format("%s.%s : No period to simulate. End date must be later than start date (Start=%s, End=%s).",
                                                              CLASSNAME_IN, methodName, aStartDate.toString(), aEndDate.toString()));
        }

        startDate     = aStartDate;
        endDate       = aEndDate;
        curDate       = aStartDate;

        endDateIndex  = DateUtils.diffDays(startDate, endDate);
        curDateIndex  = DateUtils.diffDays(startDate, curDate);

        datePeriodSet = true;
    }

    /**
     * Clears gathered data, and resets clock to start date (if set), trace
     * logging to false.
     */
    public void reset() {

        simIDMap.clear();
        simIDList.clear();
        simIDVarNameMap.clear();
        simIDVarNameList.clear();
        varNameMap.clear();
        stateVarForceValues.clear();

        if (datePeriodSet) {
            curDate      = startDate;
            curDateIndex = DateUtils.diffDays(startDate, curDate);
        }

        traceLogging = false;
    }

    /**
     * Terminate the object (no implementation yet)
     */
    public void terminate() {
    }

    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    // begin: simple info methods

    /**
     * Info function to return standard value for an invalid token.
     *
     * @return value for invalid token
     */
    public static int invalidToken() {
        return -1 * Integer.MAX_VALUE;
    }

    /**
     * Info function to return flag whether provided token is valid.
     *
     * @param token the token to test
     * @return true if token is valid
     */
    public static boolean isValidToken(int token) {
        return token != invalidToken();
    }

    /**
     * Info function to return the run ID.
     *
     * @return the run ID
     */
    public String getRunID() {
        return runID;
    }

    /**
     * Info function to return the set start date.
     *
     * @return the set start date
     */
    public LocalDate getStartDate() {

        final String methodName = "getStartDate";

        this.checkDatePeriodSet(methodName);

        return startDate;
    }

    /**
     * Info function to return the set end date.
     *
     * @return the set end date
     */
    public LocalDate getEndDate() {

        final String methodName = "getEndDate";

        this.checkDatePeriodSet(methodName);

        return endDate;
    }

    /**
     * Info function to return the current date.
     *
     * @return the current date
     */
    public LocalDate getCurDate() {

        final String methodName = "getCurDate";

        this.checkDatePeriodSet(methodName);

        return curDate;
    }

    /**
     * Info function to return the number of days that have passed since start
     * (e.g. from March 10 to March 11 is 1 day).
     *
     * @return the required value
     */
    public int elapsed() {

        final String methodName = "elapsed";

        this.checkDatePeriodSet(methodName);

        return DateUtils.diffDays(startDate, curDate);
    }

    /**
     * Info function to return the maximum maxDuration of the simulation.
     * <p>
     * Is essentially the number of days from start date till end date.
     * </p>
     * @return the required value
     */
    public int maxDuration() {

        final String methodName = "maxDuration";

        this.checkDatePeriodSet(methodName);

        return DateUtils.diffDays(startDate, endDate);
    }

    /**
     * Info function to see whether the current date is the start date.
     *
     * @return the required value
     */
    public boolean isOnStartDate() {

        final String methodName = "isOnStartDate";

        this.checkDatePeriodSet(methodName);

        return curDate.equals(startDate);
    }

    /**
     * Info function to see whether the current date is the end date.
     *
     * @return the required value
     */
    public boolean isOnEndDate() {

        final String methodName = "isOnEndDate";

        this.checkDatePeriodSet(methodName);

        return curDate.equals(endDate);
    }

    /**
     * Info function to find the year of the current date of simulation.
     *
     * @return the required value
     */
    public int year() {

        final String methodName = "year";

        this.checkDatePeriodSet(methodName);

        return curDate.getYear();
    }

    /**
     * Info function to find the month of the current date of simulation.
     *
     * @return the required value
     */
    public int month() {

        final String methodName = "month";

        this.checkDatePeriodSet(methodName);

        return curDate.getMonthValue();
    }

    /**
     * Info function to find the day in the month (1-31) of the current date of
     * simulation.
     *
     * @return the required value
     */
    public int dayInMonth() {

        final String methodName = "dayInMonth";

        this.checkDatePeriodSet(methodName);

        return curDate.getDayOfMonth();
    }

    /**
     * Info function to find the day in the year (1-365/366) of the current date of
     * simulation.
     *
     * @return the required value
     */
    public int dayInYear() {

        final String methodName = "dayInYear";

        this.checkDatePeriodSet(methodName);

        return curDate.getDayOfYear();
    }

    /**
     * Info function to find the SimID of a token.
     *
     * @param aToken the token to get the SimID of
     * @return the required SimID
     */
    public String getSimIDFromToken(int aToken) {

        final String methodName = "getSimIDFromToken";

        final int index = this.checkValidToken(aToken, methodName, false, TypeRequirement.NOREQUIREMENT);
        return simIDVarNameList.get(index).simID;
    }

    /**
     * Info function to find the varName of a token.
     *
     * @param aToken the token to get the varName of
     * @return the required vanName
     */
    public String getVarNameFromToken(int aToken) {

        final String methodName = "getVarNameFromToken";

        final int index = this.checkValidToken(aToken, methodName, false, TypeRequirement.NOREQUIREMENT);
        return simIDVarNameList.get(index).varName;
    }

    /**
     * Info function to find whether aDateIndex is a valid index.
     *
     * @param aDateIndex the date index to test
     * @return flag whether aDateIndex is valid
     */
    public boolean isValidDateIndex(int aDateIndex) {

        final String methodName = "isValidDateIndex";

        this.checkDatePeriodSet(methodName);

        return RangeUtils.inRange(aDateIndex, 0, endDateIndex);
    }

    /**
     * Info function to find if aDate is a valid date (in the range of start and
     * end date).
     *
     * @param aDate the date to test
     * @return flag whether aDate is valid
     */
    public boolean isValidDate(LocalDate aDate) {

        final String methodName = "isValidDate";

        this.checkDatePeriodSet(methodName);

        return RangeUtils.inRange(aDate, startDate, endDate);
    }

    /**
     * Get a date through a date index, when index=0, the start date is returned
     * when index=1, start date + 1 is returned etc.
     *
     * @param aDateIndex the date index to use
     * @return the date corresponding with the date index
     */
    public LocalDate getDateByDateIndex(int aDateIndex) {

        final String methodName = "getDateByDateIndex";

        if (!this.isValidDateIndex(aDateIndex)) {
            throw new IllegalArgumentException(String.format("%s.%s : The date index (%d) is not valid.",
                                                              CLASSNAME_IN, methodName, aDateIndex));
        }

        return startDate.plusDays(aDateIndex);
    }
    // end:  simple info methods
    // -------------------------------------------------------------------------

    /**
     * Integrates all state variables with rates that have been set.
     * <p>
     * If a rate has not been set, the state variable becomes switched off and
     * cannot be switched on again, returns the number of integrations.
     * </p>
     *
     * @param aDate the date to update to (must be 1 later than current date, is
     * checked)
     * @return the number of updated variables
     */
    public int updateToDate(LocalDate aDate) {

        final String methodName = "updateToDate";

        if (traceLogging) {
            LOGGER.trace(String.format("%s.%s : Date=%s", CLASSNAME_IN, methodName, aDate.toString()));
        }

        this.checkDatePeriodSet(methodName);

        int result = 0;

        // check that aDate is exactly 1 day after curDate
        if (!aDate.isEqual(curDate.plusDays(1))) {
            throw new IllegalArgumentException(String.format("%s.%s : Cannot set date to %s, previous date is more than 1 day different (%s) (time step error).",
                                                              CLASSNAME_IN, methodName, aDate.toString(), curDate.toString()));
        }

        curDate      = aDate;
        curDateIndex = DateUtils.diffDays(startDate, curDate);

        // update only state items with the rate of change, leave aux as is, have received NaN value
        // initially, so is ok
        for (SimIDVarNameListItem item : simIDVarNameList) {
            if (item.isState) {
                if (this.varActive(item, -1) && this.simIDActive(item, 0)) {
                    // the previous date has a valid value, so try to integrate
                    // if the rate of change is not valid, then the value is not updated
                    // and remains at NaN, consequently it cannot be updated ever again
                    if (!Double.isNaN(item.varChange)) {
                        // bounds checks are done in methods setSimValueAux en setSimValueState
                        item.hasValues[curDateIndex] = true;

                        if (!item.isAggregated) {
                            item.varValues[curDateIndex] = item.varValues[curDateIndex - 1] + item.varChange;
                        } else {
                            item.previous = item.last;
                            item.last     = item.last + item.varChange;

                            if (item.count == 0) item.first = item.last;

                            if (item.count > 0) {
                                // store the first index where the minimum occurred
                                if (item.min < item.last) {
                                    item.min      = item.last;
                                    item.minIndex = curDateIndex;
                                }
                            } else {
                                item.min = item.last;
                                item.minIndex = curDateIndex;
                            }

                            if (item.count > 0) {
                                // store the first index where the maximum occurred
                                if (item.max > item.last) {
                                    item.max      = item.last;
                                    item.maxIndex = curDateIndex;
                                }
                            } else {
                                item.max = item.last;
                                item.maxIndex = curDateIndex;
                            }

                            if (item.count > 0) {
                                item.sum = item.sum + item.last;
                            } else {
                                item.sum = item.last;
                            }

                            item.count++;
                        }
                        item.varChange = Double.NaN;

                        result++;
                    }
                }
            }
        }

        return result;
    }

    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    // begin: standard methods for SimObject I/O

    /**
     * Use for initialisation, registration and overriding state variables, not
     * for normal integration!
     *
     * @param aSimValueState object containing the forced value and other info
     * required by this method
     */
    public void forceSimValueState(SimValueState aSimValueState) {

        final String methodName = "forceSimValueState";

        // todo: moet hier nog iets gebeuren ivm enddate van een simid ?

        if (traceLogging) {
            LOGGER.trace(aSimValueState.toString());
        }

        this.checkDatePeriodSet(methodName);

        if (!this.isVarNameValueMissing(aSimValueState.n, aSimValueState.simID)) {
            throw new IllegalArgumentException(String.format("%s.%s : %s cannot be forced on date %s because this variable with a different simID is already active.",
                    CLASSNAME_IN, methodName, aSimValueState.getCaptionState(), curDate.toString()));
        }

        if (Double.isNaN(aSimValueState.v)) {
            throw new IllegalArgumentException(String.format("%s.%s : %s cannot be forced on date %s because this value is missing.",
                    CLASSNAME_IN, methodName, aSimValueState.getCaptionState(), curDate.toString()));
        }

        if (!this.isValidToken(aSimValueState.getT())) {
            final int token = this.registerSimIDVarName(aSimValueState.simID, aSimValueState.n, aSimValueState.u, aSimValueState.lb, aSimValueState.ub, true, "forceState");
            aSimValueState.setT(token);
        }

        final int indexState = this.checkValidToken(aSimValueState.getT(), methodName, true, TypeRequirement.STATE);
        final SimIDVarNameListItem item = simIDVarNameList.get(indexState);

        // if the state on the date is missing, check that all previous values
        // (if any) are also missing, it is ok if the value on the date is not missing
        // in that case the value will simply be overwritten

        if (!item.hasValues[curDateIndex]) {
            for (int i = 0; i <= (curDateIndex - 1); i++) {
                if (item.hasValues[i]) {
                    throw new IllegalArgumentException(String.format("%s.%s : %s cannot be forced on date %s because previous values are not missing.",
                            CLASSNAME_IN, methodName, aSimValueState.getCaptionState(), curDate.toString()));
                }
            }
        }

        // convert state to native unit
        final double candidateStateValue = ScientificUnitConversion.convert(aSimValueState.n, aSimValueState.v, aSimValueState.u, item.scientificUnit);

        if (!this.inRange(item, candidateStateValue)) {
            throw new IllegalArgumentException(String.format("%s.%s : %s cannot be forced on date %s because of a range violation: value=%g, unit=%s, range=%s.",
                    CLASSNAME_IN, methodName, aSimValueState.getCaptionState(), curDate.toString(), candidateStateValue, item.scientificUnit.getUnitCaption(), this.rangeCaption(item)));
        }

        // situation for the state is ok, value can be set
        if (!item.isAggregated) {
            this.addStateVarForceItem(indexState, item.varValues[curDateIndex], candidateStateValue);

            item.varValues[curDateIndex] = candidateStateValue;
        } else {
            this.addStateVarForceItem(indexState, item.last, candidateStateValue);

            item.previous = item.last;
            item.last     = candidateStateValue;

            if (item.count == 0) item.first = item.last;

            if (item.count > 0) {
                // store the first index where the minimum occurred
                if (item.min < item.last) {
                    item.min      = item.last;
                    item.minIndex = curDateIndex;
                }
            } else {
                item.min = item.last;
                item.minIndex = curDateIndex;
            }

            if (item.count > 0) {
                // store the first index where the maximum occurred
                if (item.max > item.last) {
                    item.max      = item.last;
                    item.maxIndex = curDateIndex;
                }
            } else {
                item.max = item.last;
                item.maxIndex = curDateIndex;
            }

            if (item.count > 0) {
                item.sum = item.sum + item.last;
            } else {
                item.sum = item.last;
            }

            item.count++;
        }
        item.hasValues[curDateIndex] = true;
    }

    /**
     * Standard procedure for defining rates of state variables (=states), will
     * be integrated on updateToDate method.
     *
     * @param aSimValueState aSimValueState object containing the forced value
     * and other info required
     */
    public void setSimValueState(SimValueState aSimValueState) {

        final String methodName = "setSimValueState";

        if (traceLogging) {
            LOGGER.trace(aSimValueState.toString());
        }

        this.checkDatePeriodSet(methodName);

        if (!this.isVarNameValueMissing(aSimValueState.n, aSimValueState.simID)) {
            throw new IllegalArgumentException(String.format("%s.%s : %s cannot be set on date %s because this variable with a different simID is already active.",
                                                              CLASSNAME_IN, methodName, aSimValueState.getCaptionRate(), curDate.toString()));
        }

        if (Double.isNaN(aSimValueState.r)) {
            throw new IllegalArgumentException(String.format("%s.%s : %s cannot be set on date %s because this value is missing.",
                                                              CLASSNAME_IN, methodName, aSimValueState.getCaptionRate(), curDate.toString()));
        }

        final int                  index = this.checkValidToken(aSimValueState.getT(), "setSimValueState", true, TypeRequirement.STATE);
        final SimIDVarNameListItem item  = simIDVarNameList.get(index);

        if (!Double.isNaN(item.varChange)) {
            throw new IllegalArgumentException(String.format("%s.%s : %s cannot be set on date %s because this value is already set.",
                                                              CLASSNAME_IN, methodName, aSimValueState.getCaptionRate(), curDate.toString()));
        }

        if (!this.varActive(item, 0)) {
            throw new IllegalArgumentException(String.format("%s.%s : %s cannot be set on date %s because the state is inactive.",
                                                              CLASSNAME_IN, methodName, aSimValueState.getCaptionRate(), curDate.toString()));
        }

        // convert value to native unit
        final double candidateRateValue = ScientificUnitConversion.convert(aSimValueState.n, aSimValueState.r, aSimValueState.u, item.scientificUnit);

        final double candidateStateValue;
        if (!item.isAggregated) {
            candidateStateValue = item.varValues[curDateIndex] + candidateRateValue;
        } else {
            candidateStateValue = item.last + candidateRateValue;
        }

        if (!this.inRange(item, candidateStateValue)) {
            throw new IllegalArgumentException(String.format("%s.%s : %s cannot be set on date %s because of a range violation for the new value of the corresponding state: oldvalue=%g, newvalue=%g, unit=%s, range=%s.",
                                                              CLASSNAME_IN, methodName, aSimValueState.getCaptionRate(), curDate.toString(), item.varValues[curDateIndex], candidateStateValue, item.scientificUnit.getUnitCaption(), this.rangeCaption(item)));
        }

        item.varChange = candidateRateValue;
    }

    /**
     * Info function to get a refreshed copy on the current date of the state
     * variable in the provided object.
     *
     * @param aSimValueState the object in which to copy the state at the
     * current date
     */
    public void getSimValueState(SimValueState aSimValueState) {

        final String methodName = "getSimValueState";

        this.checkDatePeriodSet(methodName);

        final int                  index = this.checkValidToken(aSimValueState.getT(), methodName, false, TypeRequirement.NOREQUIREMENT);
        final SimIDVarNameListItem item  = simIDVarNameList.get(index);

        if (!item.isState) {
            throw new IllegalStateException(String.format("%s.%s : Variable %s is not registered as a state variable.",
                                                          CLASSNAME_IN, methodName, aSimValueState.n));
        }

        aSimValueState.v = this.getValueByIndex(index, curDateIndex, aSimValueState.u, false);
        if (curDateIndex >= 1) {
            aSimValueState.vp = this.getValueByIndex(index, (curDateIndex - 1), aSimValueState.u, false);
        } else {
            aSimValueState.vp = Double.NaN;
        }
    }

    /**
     * Simply set the aux value on the current date.
     *
     * @param aSimValueAux the object containing the info
     */
    public void setSimValueAux(SimValueAux aSimValueAux) {

        final String methodName = "setSimValueAux";

        if (traceLogging) {
            LOGGER.trace(aSimValueAux.toString());
        }

        this.checkDatePeriodSet(methodName);

        if (!this.isVarNameValueMissing(aSimValueAux.n, aSimValueAux.simID)) {
            throw new IllegalArgumentException(String.format("%s.%s : %s cannot be set because on date %s because this variable with a different simID is already active.",
                                                              CLASSNAME_IN, methodName, aSimValueAux.getCaption(), curDate.toString()));
        }

        if (Double.isNaN(aSimValueAux.v)) {
            throw new IllegalArgumentException(String.format("%s.%s : %s cannot be set on date %s because this is a missing value.",
                                                              CLASSNAME_IN, methodName, aSimValueAux.getCaption(), curDate.toString()));
        }

        if (!this.isValidToken(aSimValueAux.getT())) {
            final int token = this.registerSimIDVarName(aSimValueAux.simID, aSimValueAux.n, aSimValueAux.u, aSimValueAux.lb, aSimValueAux.ub, false, "setSimValueAux");
            aSimValueAux.setT(token);
        }

        final int                  index = this.checkValidToken(aSimValueAux.getT(), methodName, true, TypeRequirement.AUXILIARY);
        final SimIDVarNameListItem item  = simIDVarNameList.get(index);

        final int prevDateIndex = curDateIndex - 1;

        if (prevDateIndex >= 0) {
            // there is a previous item
            if (!item.hasValues[prevDateIndex]) {
                // the previous item is missing, see if all earliers items
                // are also missing
                for (int i = 0; i < prevDateIndex; i++) {
                    if (item.hasValues[i]) {
                        throw new IllegalArgumentException(String.format("%s.%s : %s cannot be set on date %s because previous values are not missing.",
                                                                          CLASSNAME_IN, methodName, aSimValueAux.getCaption(), curDate.toString()));
                    }
                }
            }
        }

        // convert value to native unit
        final double candidateAuxValue = ScientificUnitConversion.convert(aSimValueAux.n, aSimValueAux.v, aSimValueAux.u, item.scientificUnit);

        if (!this.inRange(item, candidateAuxValue)) {
            throw new IllegalArgumentException(String.format("%s.%s : %s cannot be set on date %s because of a range violation: value=%g, unit=%s, range=%s.",
                                                              CLASSNAME_IN, methodName, aSimValueAux.getCaption(), curDate.toString(), candidateAuxValue, item.scientificUnit.getUnitCaption(), this.rangeCaption(item)));
        }

        // situation for the value is ok, value can be set
        item.hasValues[curDateIndex] = true;

        if (!item.isAggregated) {
            item.varValues[curDateIndex] = candidateAuxValue;
        } else {
            item.previous = item.last;
            item.last     = candidateAuxValue;

            if (item.count == 0) item.first = item.last;

            if (item.count > 0) {
                // store the first index where the minimum occurred
                if (item.min < item.last) {
                    item.min      = item.last;
                    item.minIndex = curDateIndex;
                }
            } else {
                item.min = item.last;
                item.minIndex = curDateIndex;
            }

            if (item.count > 0) {
                // store the first index where the maximum occurred
                if (item.max > item.last) {
                    item.max      = item.last;
                    item.maxIndex = curDateIndex;
                }
            } else {
                item.max = item.last;
                item.maxIndex = curDateIndex;
            }

            if (item.count > 0) {
                item.sum = item.sum + item.last;
            } else {
                item.sum = item.last;
            }

            item.count++;
        }
    }

    /**
     * Info function to get refreshed copy of value in SimValueExternal object
     * for the provided date index.
     * <p>
     * Essentially, this searches for a hit on the provided SimID and variable
     * name.
     * </p>
     *
     * @param aSimValueExternal the object to be refreshed
     * @param aDateIndex the date index for which
     */
    private void getSimValueExternalBySimIDVarName(SimValueExternal aSimValueExternal, int aDateIndex) {

        final String methodName = "getSimValueExternalBySimIDVarName";

        this.checkDatePeriodSet(methodName);

        final int    simIDVarNameListIndex = this.checkValidToken(aSimValueExternal.t, methodName, false, TypeRequirement.NOREQUIREMENT);
        final double v                     = this.getValueByIndex(simIDVarNameListIndex, aDateIndex, aSimValueExternal.u, false);

        aSimValueExternal.setV(v);

        // find out whether the simobject is terminated
        final int i = simIDVarNameList.get(simIDVarNameListIndex).simIDListIndex;

        boolean b;
        if (simIDList.get(i).endDateIndex == SimXChange.MISSINGINDEX) {
            b = false;
        } else {
            b = (aDateIndex >= simIDList.get(i).endDateIndex);
        }
        aSimValueExternal.setTerminated(b);
    }

    /**
     * Info function to get a refreshed copy on the current date.
     * <p>
     * Go through whole list searching for hit on variable name only (so across
     * all SimID's), returned value is a NaN if the search failed.
     * </p>
     *
     * @param aSimValueExternal the object to be refreshed
     */
    public void getSimValueExternalByVarName(SimValueExternal aSimValueExternal) {

        this.getSimValueExternalByVarNameDelta(aSimValueExternal, 0);
    }

    /**
     * Info function to get a refreshed copy on the delta date.
     * <p>
     * Go through whole list searching for hit on variable name only (so across
     * all SimID's), returned value is a NaN if the search failed.
     * </p>
     *
     * @param aSimValueExternal the object to be refreshed
     * @param aDelta the number of days relative to the current date, valid
     * values are provided only if aDelta is zero or negative
     */
    public void getSimValueExternalByVarNameDelta(SimValueExternal aSimValueExternal, int aDelta) {

        final String methodName = "getSimValueExternalByVarNameDelta";

        if (aDelta > 0) {
            throw new IllegalArgumentException(String.format("%s.%s : Delta value cannot be positive" +
                                                             " (call info: Variable=%s, Delta=%d).",
                                                              CLASSNAME_IN, methodName, aSimValueExternal.n, aDelta));
        }

        // find the date index pointed to by aDelta
        final int dateIndex = curDateIndex + aDelta;

        if (dateIndex >= 0) {
            // dateIndex points to data existing in this instance, see if aSimValueExternal.n is already registered

            // see if aSimValueExternal.n is already registered
            if (!this.isValidToken(aSimValueExternal.t)) {
                aSimValueExternal.t = this.getTokenReadByVarNameDateIndex(aSimValueExternal.n, dateIndex, false);
            }

            if (this.isValidToken(aSimValueExternal.t)) {
                // aSimValueExternal.n is already registered and readonly token is obtained
                // so try get the value, can be a NaN !
                this.getSimValueExternalBySimIDVarName(aSimValueExternal, dateIndex);

                if (aSimValueExternal.isMissing()) {
                    // try to lock on to another source providing this quantity
                    aSimValueExternal.t = this.getTokenReadByVarNameDateIndex(aSimValueExternal.n, dateIndex, false);
                    if (this.isValidToken(aSimValueExternal.t)) {
                        // lock on successful, so try to get the value, can be a NaN
                        this.getSimValueExternalBySimIDVarName(aSimValueExternal, dateIndex);
                    }
                }
            }
        } else {
            aSimValueExternal.t = invalidToken();
            aSimValueExternal.setV(Double.NaN);
        }
    }

    /**
     * Info function to get a refreshed copy on the provided date.
     * <p>
     * Go through whole list searching for hit on variable name only (so across
     * all SimID's), returned value is a NaN if the search failed.
     * </p>
     *
     * @param aSimValueExternal the object to be refreshed
     * @param aDate the date for the method to work on
     */
    public void getSimValueExternalByVarNameDate(SimValueExternal aSimValueExternal, LocalDate aDate) {

        final String methodName = "getSimValueExternalByVarNameDate";

        if (!this.isValidDate(aDate)) {
            throw new IllegalArgumentException(String.format("%s.%s : Date value (%s) not in valid range (%s, %s) for variable %s.",
                                                              CLASSNAME_IN, methodName, aDate, startDate, endDate, aSimValueExternal.n));
        }

        // find the value pointed to by aDate
        final int dateIndex = DateUtils.diffDays(startDate, aDate);

        // see if aSimValueExternal.n is already registered
        if (!this.isValidToken(aSimValueExternal.t)) {
            aSimValueExternal.t = this.getTokenReadByVarNameDateIndex(aSimValueExternal.n, dateIndex, false);
        }

        if (this.isValidToken(aSimValueExternal.t)) {
            // aSimValueExternal.n is already registered and readonly token is obtained
            // so try get the value, can be a NaN !
            this.getSimValueExternalBySimIDVarName(aSimValueExternal, dateIndex);

            if (aSimValueExternal.isMissing()) {
                // try to lock on to another source providing this quantity
                aSimValueExternal.t = this.getTokenReadByVarNameDateIndex(aSimValueExternal.n, dateIndex, false);
                if (this.isValidToken(aSimValueExternal.t)) {
                    // lock on successful, so try to get the value, can be a NaN
                    this.getSimValueExternalBySimIDVarName(aSimValueExternal, dateIndex);
                }
            }
        }
    }
    // end: standard methods for SimObject I/O
    // -------------------------------------------------------------------------

    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    // begin: methods to obtain a single value

    /**
     * Info function to return straight double value.
     *
     * @param aSimIDVarNameListIndex index into simIDVarNameList object
     * @param aDateIndex index for date, relative to start date
     * @param aScientificUnit the unit to convert the result into
     * @return the required double value
     */
    private double getValueByIndex(int aSimIDVarNameListIndex, int aDateIndex, ScientificUnit aScientificUnit, boolean aCheckNotMissing) {

        final String methodName = "getValueByIndex";

        if (!RangeUtils.inRange(aSimIDVarNameListIndex, 0, (simIDVarNameList.size() - 1))) {
            throw new IllegalArgumentException(String.format("%s.%s : aSimIDVarNameListIndex value (%d) not in valid range (0, %d).",
                                                              CLASSNAME_IN, methodName, aSimIDVarNameListIndex, (simIDVarNameList.size() - 1)));
        }

        if (!RangeUtils.inRange(aDateIndex, 0, curDateIndex)) {
            throw new IllegalArgumentException(String.format("%s.%s : aDateIndex value (%d) not in valid range (0, %d).",
                                                              CLASSNAME_IN, methodName, aDateIndex, curDateIndex));
        }

        final SimIDVarNameListItem item  = simIDVarNameList.get(aSimIDVarNameListIndex);

        if ((aDateIndex < (curDateIndex - 1)) && item.isAggregated) {
            throw new IllegalArgumentException(String.format("%s.%s : %s aDateIndex value (%d) illegal for aggregated states.",
                    CLASSNAME_IN, methodName, item.simIDVarName, aDateIndex));
        }

        // note that the else part can only be safely done if the previous if is programmed correctly
        double result;
        if (!item.isAggregated) {
            result = item.varValues[aDateIndex];
        } else {
            result = (aDateIndex == curDateIndex) ? item.last : item.previous;
        }

        if (!Double.isNaN(result)) {
            result = ScientificUnitConversion.convert(item.varName, result, item.scientificUnit, aScientificUnit);
        }

        if (aCheckNotMissing) {
            if (Double.isNaN(result)) {
                throw new IllegalArgumentException(String.format("%s.%s : Variable %s is missing.",
                                                                  CLASSNAME_IN, methodName, item.simIDVarName));
            }
        }

        return result;
    }

    /**
     * Info function to return straight double value for aToken on the current
     * date, for the required unit.
     * <p>
     * The token value is equivalent to searching for SimID and variable name.
     * </p>
     *
     * @param aToken the token to work on
     * @param aScientificUnit the unit to convert the value into
     * @return the required value
     */
    public double getValueBySimIDVarname(int aToken, ScientificUnit aScientificUnit) {

        return this.getValueBySimIDVarnameDate(aToken, curDate, aScientificUnit, true);
    }

    /**
     * Info function to return straight double value for aToken on the provided
     * date, for the required unit.
     * <p>
     * The token value is equivalent to searching for SimID and variable name.
     * An exception will occur if the value cannot be found.
     * </p>
     *
     * @param aToken the token to work on
     * @param aDate the date to work on
     * @param aScientificUnit the unit to convert the value into
     * @return the required value
     */
    public double getValueBySimIDVarnameDate(int aToken, LocalDate aDate, ScientificUnit aScientificUnit) {

        return this.getValueBySimIDVarnameDate(aToken, aDate, aScientificUnit, true);
    }

    /**
     * Info function to return straight double value for aToken on the provided
     * date index, for the required unit.
     * <p>
     * The token value is equivalent to searching for SimID and variable name.
     * An exception will occur if the value cannot be found.
     * </p>
     *
     * @param aToken the token to work on
     * @param aDateIndex the date index to work on
     * @param aScientificUnit the unit to convert the value into
     * @return the required value
     */
    public double getValueBySimIDVarnameDateIndex(int aToken, int aDateIndex, ScientificUnit aScientificUnit) {

        return this.getValueBySimIDVarnameDateIndex(aToken, aDateIndex, aScientificUnit, true);
    }

    /**
     * Info function to return straight double value for aToken on the delta
     * date, for the required unit.
     * <p>
     * The token value is equivalent to searching for SimID and variable name.
     * An exception will occur if the value cannot be found.
     * </p>
     *
     * @param aToken the token to work on
     * @param aDelta offset to use for the evaluation
     * @param aScientificUnit the unit to convert the value into
     * @return the required value
     */
    public double getValueBySimIDVarnameDelta(int aToken, int aDelta, ScientificUnit aScientificUnit) {

        return this.getValueBySimIDVarnameDelta(aToken, aDelta, aScientificUnit, true);
    }

    /**
     * Info function to return straight double value for aToken on the provided
     * date, for the required unit.
     * <p>
     * The token value is equivalent to searching for SimID and variable name.
     * An exception will occur if aCheckNotMissing = true.
     * </p>
     *
     * @param aToken the token to work on
     * @param aDate the date to work on
     * @param aScientificUnit the unit to convert the value into
     * @param aCheckNotMissing whether missing values should give an exception
     * @return the required value
     */
    public double getValueBySimIDVarnameDate(int aToken, LocalDate aDate, ScientificUnit aScientificUnit, boolean aCheckNotMissing) {

        final String methodName = "getValueBySimIDVarnameDate";

        final int index = this.checkValidToken(aToken, methodName, false, TypeRequirement.NOREQUIREMENT);

        if (!this.isValidDate(aDate)) {
            final SimIDVarNameListItem item  = simIDVarNameList.get(index);
            final String               simID = this.getSimIDFromToken(aToken);

            throw new IllegalArgumentException(String.format("%s.%s : Date value (%s) not in valid range (%s, %s) for simID=%s, varName=%s.",
                                                              CLASSNAME_IN, methodName, aDate, startDate, endDate, simID, item.simIDVarName));
        }

        final int dateIndex = DateUtils.diffDays(startDate, aDate);

        final double result = this.getValueByIndex(index, dateIndex, aScientificUnit, aCheckNotMissing);

        return result;
    }

    /**
     * Info function to return straight double value for aToken on the provided
     * date index, for the required unit.
     * <p>
     * The token value is equivalent to searching for SimID and variable name.
     * An exception will occur if aCheckNotMissing = true.
     * </p>
     *
     * @param aToken the token to work on
     * @param aDateIndex the date index to work on
     * @param aScientificUnit the unit to convert the value into
     * @param aCheckNotMissing whether missing values should give an exception
     * @return the required value
     */
    public double getValueBySimIDVarnameDateIndex(int aToken, int aDateIndex, ScientificUnit aScientificUnit, boolean aCheckNotMissing) {

        final String methodName = "getValueBySimIDVarnameDateIndex";

        final int index = this.checkValidToken(aToken, methodName, false, TypeRequirement.NOREQUIREMENT);

        if (!this.isValidDateIndex(aDateIndex)) {
            final SimIDVarNameListItem item  = simIDVarNameList.get(index);
            final String               simID = this.getSimIDFromToken(aToken);

            throw new IllegalArgumentException(String.format("%s.%s : Date index value (%d) not in valid range (0, %d) for simID=%s, varName=%s.",
                                                              CLASSNAME_IN, methodName, aDateIndex, endDateIndex, simID, item.simIDVarName));
        }

        final double result = this.getValueByIndex(index, aDateIndex, aScientificUnit, aCheckNotMissing);

        return result;
    }

    /**
     * Info function to return straight double value for aToken on the delta
     * date, for the required unit.
     * <p>
     * The token value is equivalent to searching for SimID and variable name.
     * An exception will occur if aCheckNotMissing = true.
     * </p>
     *
     * @param aToken the token to work on
     * @param aDelta offset to use for the evaluation
     * @param aScientificUnit the unit to convert the value into
     * @param aCheckNotMissing gives exception if SimID and VarName of token
     * cannot be found
     * @return the required value
     */
    public double getValueBySimIDVarnameDelta(int aToken, int aDelta, ScientificUnit aScientificUnit, boolean aCheckNotMissing) {

        final String methodName = "getValueBySimIDVarnameDelta";

        if (aDelta > 0) {
            final String simID   = this.getSimIDFromToken(aToken);
            final String varName = this.getVarNameFromToken(aToken);

            throw new IllegalArgumentException(String.format("%s.%s : Delta value cannot be positive" +
                                                             " (call info: SimID=%s, Variable=%s, Delta=%d).",
                                                              CLASSNAME_IN, methodName, simID, varName, aDelta));
        }

        // find the date index pointed to by aDelta
        final int dateIndex = curDateIndex + aDelta;

        final double result = this.getValueBySimIDVarnameDateIndex(aToken, dateIndex, aScientificUnit, aCheckNotMissing);

        return result;
    }

    // end: methods to obtain a single value
    // -------------------------------------------------------------------------

    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    // begin: methods to obtain an aggregated value

    /**
     * Info function to return aggregated double value for aToken, for the
     * required unit, for the required aggregation.
     * <p>
     * The token value is equivalent to searching for SimID and variable name.
     * Aggregation will take place over the whole period of simulation. An
     * exception will occur if the aggregation cannot be determined.
     * </p>
     *
     * @param aToken the token to work on
     * @param aScientificUnit the unit to convert the value into
     * @param aAggregationY the required aggregation
     * @return the required value
     */
    public double getValueBySimIDVarNameAgg(int aToken, ScientificUnit aScientificUnit, AggregationY aAggregationY) {

        return this.getValueBySimIDVarNameAgg(aToken, aScientificUnit, aAggregationY, true);
    }

    /**
     * Info function to return aggregated double value for aToken, for the
     * required unit, for the required aggregation.
     * <p>
     * The token value is equivalent to searching for SimID and variable name.
     * Aggregation will take place over the whole period of simulation. An
     * exception will occur if aCheckNotMissing = true.
     * </p>
     *
     * @param aToken the token to work on
     * @param aScientificUnit the unit to convert the value into
     * @param aAggregationY the required aggregation
     * @param aCheckNotMissing whether missing values should give an exception
     * @return the required value
     */
    public double getValueBySimIDVarNameAgg(int aToken, ScientificUnit aScientificUnit, AggregationY aAggregationY, boolean aCheckNotMissing) {

        final String methodName = "getValueBySimIDVarNameAgg";

        this.checkDatePeriodSet(methodName);

        double result = Double.NaN;

        final int index = this.checkValidToken(aToken, methodName, false, TypeRequirement.NOREQUIREMENT);
        final SimIDVarNameListItem item = simIDVarNameList.get(index);

        final int firstIndex = 0;

        final int lastIndex  = item.varValues.length - 1;

        final Aggregation aggregation = new Aggregation(item, firstIndex, lastIndex);
        switch (aAggregationY) {
            case FIRST:
                result = aggregation.firstY();
                break;
            case LAST:
                result = aggregation.lastY();
                break;
            case MIN:
                result = aggregation.minY();
                break;
            case MAX:
                result = aggregation.maxY();
                break;
            case COUNT:
                result = aggregation.countY();
                break;
            case SUM:
                result = aggregation.sumY();
                break;
            case AVERAGE:
                final double cnt = aggregation.countY();
                if (cnt != 0.0) {
                    result = aggregation.sumY() / cnt;
                }
                break;
            case DELTA:
                result = aggregation.deltaY();
                break;
            case RANGE:
                result = aggregation.maxY();
                if (!Double.isNaN(result)) {
                    result = result - aggregation.minY();
                }
                break;
            default:
                throw new IllegalArgumentException(String.format("%s.%s : Invalid SimXChange.AggregationY value.",
                                                                  CLASSNAME_IN, methodName));
        }

        if (aCheckNotMissing) {
            if (Double.isNaN(result)) {
                throw new IllegalArgumentException(String.format("%s.%s : Variable %s is missing.",
                                                                  CLASSNAME_IN, methodName, item.simIDVarName));
            }
        }

        result = ScientificUnitConversion.convert(item.varName, result, item.scientificUnit, aScientificUnit);

        return result;
    }

    /**
     * Info function to return aggregated double value for aToken, for the
     * required unit, for the required aggregation over a number of previous
     * days.
     * <p>
     * The token value is equivalent to searching for SimID and variable name.
     * An exception will occur if the aggregation cannot be determined.
     * </p>
     *
     * @param aToken the token to work on
     * @param aScientificUnit the unit to convert the value into
     * @param aAggregationY the required aggregation
     * @param aDayCount the number of historic days for the aggregation
     * @return the required value
     */
    public double getValueBySimIDVarNameAggMoving(int aToken, ScientificUnit aScientificUnit, AggregationY aAggregationY, int aDayCount) {

        return this.getValueBySimIDVarNameAggMoving(aToken, aScientificUnit, aAggregationY, aDayCount, true);
    }

    /**
     * Info function to return aggregated double value for aToken, for the
     * required unit, for the required aggregation over a number of previous
     * days.
     * <p>
     * The token value is equivalent to searching for SimID and variable name.
     * An exception will occur if aCheckNotMissing = true.
     * </p>
     *
     * @param aToken the token to work on
     * @param aScientificUnit the unit to convert the value into
     * @param aAggregationY the required aggregation
     * @param aDayCount the number of historic days for the aggregation
     * @param aCheckNotMissing whether missing values should give an exception
     * @return the required value
     */
    public double getValueBySimIDVarNameAggMoving(int aToken, ScientificUnit aScientificUnit, AggregationY aAggregationY, int aDayCount, boolean aCheckNotMissing) {

        final String methodName = "getValueBySimIDVarNameAggMoving";

        if (aDayCount <= 0) {
            throw new IllegalArgumentException(String.format("%s.%s : Illegal value for aDayCount (%d) (must be > 0).",
                                                              CLASSNAME_IN, methodName, aDayCount));
        } else if (aDayCount > this.maxDuration() + 1) {
            throw new IllegalArgumentException(String.format("%s.%s : Illegal value for aDayCount (%d) (must be <= %d).",
                                                              CLASSNAME_IN, methodName, aDayCount, (this.maxDuration() + 1)));
        }

        this.checkDatePeriodSet(methodName);

        double result = Double.NaN;

        final int                  index = this.checkValidToken(aToken, methodName, false, TypeRequirement.NOREQUIREMENT);
        final SimIDVarNameListItem item  = simIDVarNameList.get(index);

        final int lastIndex  = curDateIndex; // do not go until the end of the array
        final int firstIndex = max(0, (lastIndex - aDayCount + 1));

        final Aggregation aggregation = new Aggregation(item, firstIndex, lastIndex);

        switch (aAggregationY) {
            case FIRST:
                result = aggregation.firstY();
                break;
            case LAST:
                result = aggregation.lastY();
                break;
            case MIN:
                result = aggregation.minY();
                break;
            case MAX:
                result = aggregation.maxY();
                break;
            case COUNT:
                result = aggregation.countY();
                break;
            case SUM:
                result = aggregation.sumY();
                break;
            case AVERAGE:
                final double cnt = aggregation.countY();
                if (cnt != 0.0) {
                    result = aggregation.sumY() / cnt;
                }
                break;
            case DELTA:
                result = aggregation.deltaY();
                break;
            case RANGE:
                result = aggregation.maxY();
                if (!Double.isNaN(result)) {
                    result = result - aggregation.minY();
                }
                break;
            default:
                throw new IllegalArgumentException(String.format("%s.%s : Invalid SimXChange.AggregationY value.",
                                                                  CLASSNAME_IN, methodName));
        }

        if (aCheckNotMissing) {
            if (Double.isNaN(result)) {
                throw new IllegalArgumentException(String.format("%s.%s : Variable %s is missing on date %s.",
                                                                  CLASSNAME_IN, methodName, item.simIDVarName, curDate.toString()));
            }
        }

        result = ScientificUnitConversion.convert(item.varName, result, item.scientificUnit, aScientificUnit);

        return result;
    }

    /**
     * Info function to return an aggregated date for aToken (such as the first
     * or last date of a value, or the date of the lowest or highest value).
     * <p>
     * The token value is equivalent to searching for SimID and variable name.
     * An exception will occur if there was no value to aggregate.
     * <p>
     * @param aToken the token to work on
     * @param aAggregationDate required date aggregation
     * @return the required value
     */
    public LocalDate getDateBySimIDVarNameAgg(int aToken, AggregationDate aAggregationDate) {

        return this.getDateBySimIDVarNameAgg(aToken, aAggregationDate, true);
    }
    /**
     * Info function to return an aggregated date for aToken (such as the first
     * or last date of a value, or the date of the lowest or highest value).
     * <p>
     * The token value is equivalent to searching for SimID and variable name.
     * An exception will occur if aCheckNotMissing = true.
     * <p>
     * @param aToken the token to work on
     * @param aAggregationDate required date aggregation
     * @param aCheckNotMissing whether missing values should give an exception
     * @return the required value
     */
    public LocalDate getDateBySimIDVarNameAgg(int aToken, AggregationDate aAggregationDate, boolean aCheckNotMissing) {

        final String methodName = "getDateBySimIDVarNameAgg";

        this.checkDatePeriodSet(methodName);

        final int result;

        final int index = this.checkValidToken(aToken, methodName, false, TypeRequirement.NOREQUIREMENT);
        final SimIDVarNameListItem item = simIDVarNameList.get(index);

        final int firstIndex = 0;
        final int lastIndex  = item.varValues.length - 1;

        final Aggregation aggregation = new Aggregation(item, firstIndex, lastIndex);
        switch (aAggregationDate) {
            case FIRST:
                result = aggregation.firstXIndex();
                break;
            case LAST:
                result = aggregation.lastXIndex();
                break;
            case MIN:
                result = aggregation.minXIndex();
                break;
            case MAX:
                result = aggregation.maxXIndex();
                break;
            default:
                throw new IllegalArgumentException(String.format("%s.%s : Invalid SimXChange.AggregationDate value.",
                                                                  CLASSNAME_IN, methodName));
        }

        LocalDate ldResult = LocalDate.of(1900, 1, 1);
        if (result != SimXChange.MISSINGINDEX) {
            ldResult = startDate.plusDays(result);
        } else {
            if (aCheckNotMissing) {
                throw new IllegalArgumentException(String.format("%s.%s : Variable %s is missing.",
                                                                  CLASSNAME_IN, methodName, item.simIDVarName));
            }
        }

        return ldResult;
    }

    /**
     * Info function to return array of dates where value of aToken crosses
     * aValue.
     * <p>
     * When upwards, a date where the token value is larger than aValue and the
     * token value smaller or equal aValue on the previous date is provided in
     * the returned ArrayList. When not upwards, the inverse logic takes place.
     * </p>
     *
     * @param aToken the token of the simIDListIndex / varName combination
     * @param aScientificUnit the unit for the evaluations
     * @param aValue the value for the evaluations
     * @param aUpwards whether upward or downward crosses are required
     *
     * @return array of dates, can be empty, but not null.
     */
    public ArrayList<LocalDate> getDatesBySimIDVarNameCrosses(int aToken, ScientificUnit aScientificUnit, double aValue, boolean aUpwards) {

        final String methodName = "getDatesBySimIDVarNameCrosses";

        this.checkDatePeriodSet(methodName);

        final ArrayList<LocalDate> result = new ArrayList<>();

        final int index = this.checkValidToken(aToken, methodName, false, TypeRequirement.NOREQUIREMENT);
        final SimIDVarNameListItem item = simIDVarNameList.get(index);

        if (item.isAggregated) {
            throw new IllegalArgumentException(String.format("%s.%s : %s is defined aggregated, so time series data are not available",
                                                             CLASSNAME_IN, methodName, item.simIDVarName));
        }

        final int firstIndex = 1; // start at the 2nd index !!
        final int lastIndex  = item.varValues.length - 1;

        double v;  // for value at date
        double vp; // for value at previous date

        vp = item.varValues[0];
        if (!Double.isNaN(vp)) {
            vp = ScientificUnitConversion.convert(item.varName, vp, item.scientificUnit, aScientificUnit);
        }

        for (int i = firstIndex; i <= lastIndex; i++) {
            v = item.varValues[i];

            if (!Double.isNaN(vp) && !Double.isNaN(v)) {
                // previous and current value are not missing
                v  = ScientificUnitConversion.convert(item.varName, item.varValues[i], item.scientificUnit, aScientificUnit);

                if (aUpwards) {
                    if ((v > aValue) && (vp <= aValue)) {
                        result.add(startDate.plusDays(i));
                    }
                } else {
                    if ((v < aValue) && (vp >= aValue)) {
                        result.add(startDate.plusDays(i));
                    }
                }
            }
            vp = v;
        }

        return result;
    }

    /**
     * Info function that returns array of aggregated values for aVarName in the
     * required unit, in order of registration.
     *
     * @param aVarName the variable name to look for across all simbobjects
     * @param aScientificUnit the unit to convert the values into
     * @param aAggregationY the required aggregation
     * @return the array of double values
     */
    public ArrayList<Double> getValuesByVarNameAgg(String aVarName, ScientificUnit aScientificUnit, AggregationY aAggregationY) {

        final String methodName = "getValuesByVarNameAgg";

        this.checkDatePeriodSet(methodName);

        final String varName = aVarName.toUpperCase(Locale.US);

        final ArrayList<Double> result = new ArrayList<>();

        if (varNameMap.containsKey(varName)) {
            final ArrayList<Integer> arr = varNameMap.get(varName);

            for (int i = 0; i <= (arr.size() - 1); i++) {
                final double d = this.getValueBySimIDVarNameAgg(this.encodeIndexToToken(arr.get(i), false), aScientificUnit, aAggregationY, false);
                result.add(d);
            }
        }

        return result;
    }
    // end: methods to obtain an aggregated value
    // -------------------------------------------------------------------------

    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    // begin: methods to obtain an interpolator

    /**
     * Info function to obtain an interpolator with all valid values for the
     * token.
     * <p>
     * Index date, where date of start of simulation is zero, is provided as X,
     * value as Y.
     * </p>
     *
     * @param aToken a valid token for which to obtain the Interpolator object
     * @return the required interpolator
     */
    public Interpolator getInterpolatorBySimIDVarName(int aToken) {

        return this.getInterpolatorBySimIDVarName(aToken, false);
    }

    /**
     * Info function that returns an interpolator object for the provided token.
     *
     * @param aToken a valid token for which to obtain the Interpolator object
     * @param aXYSwapped if true, day since start of simulation (date index) is
     * stored in Interpolator object as Y, aToken related values as X (since in
     * some cases equal values for the token may occur, such as for development
     * state of a crop, the first of the equal ones is stored). The values for
     * the token are not allowed to be lower.
     * @return an Interpolator object with the day since start of simulation and
     * values for the token
     */
    public Interpolator getInterpolatorBySimIDVarName(int aToken, boolean aXYSwapped) {

        final String methodName = "getInterpolatorBySimIDVarName";

        this.checkDatePeriodSet(methodName);

        final int                  index = this.checkValidToken(aToken, methodName, false, TypeRequirement.NOREQUIREMENT);
        final SimIDVarNameListItem item  = simIDVarNameList.get(index);

        final Interpolator interpolator = new Interpolator(item.simIDVarName, ScientificUnit.DATE, item.scientificUnit);

        if (item.isAggregated) {
            throw new IllegalArgumentException(String.format("%s.%s : %s is defined aggregated, so time series data are not available",
                    CLASSNAME_IN, methodName, item.simIDVarName));
        }

        // this assumes that there is only one 'burst' of data, which should be guaranteed prior, it is not
        // this method's responsibility to check this
        // simply copy non-missing data to the interpolator
        for (int indexDate = 0; indexDate <= (simIDVarNameList.get(0).hasValues.length - 1); indexDate++) {
            if (item.hasValues[indexDate]) {
                final double v = item.varValues[indexDate];
                if (!aXYSwapped) {
                    // indexDate is always ascending, so no special provision required to avoid adding
                    // identical x values
                    interpolator.add(indexDate, v);
                } else {
                    // avoid adding identical x values to the interpolator. Do by adding only the first of
                    // the identical ones
                    if (interpolator.count() == 0) {
                        interpolator.add(v, indexDate);
                    } else if (v != interpolator.getXMax()) {
                        interpolator.add(v, indexDate);
                    }
                }
            }
        }

        return interpolator;
    }
    // end: methods to obtain an interpolator
    // -------------------------------------------------------------------------

    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    // begin: methods to obtain a token for a varname

    /**
     * Info function that returns a readonly token for aSimID / aVarName,
     * exception when not there.
     *
     * @param aSimID the SimID to look for
     * @param aVarName the variable name to look for
     * @return the token value
     */
    public int getTokenReadBySimIDVarName(String aSimID, String aVarName) {

        final String methodName = "getTokenReadBySimIDVarName";

        if (StringUtils.isBlank(aSimID)) {
            throw new IllegalArgumentException(String.format("%s.%s : The simID is empty.",
                                                              CLASSNAME_IN, methodName));
        }

        if (StringUtils.isBlank(aVarName)) {
            throw new IllegalArgumentException(String.format("%s.%s : The variable is empty.",
                                                              CLASSNAME_IN, methodName));
        }

        final String simID   = aSimID.toUpperCase(Locale.US);
        final String varName = aVarName.toUpperCase(Locale.US);

        final String simIDVarName = this.composeSimIDVarName(simID, varName);
        final int    curIndex     = this.getSimIDVarNameListIndex(simIDVarName);

        if (curIndex == SimXChange.MISSINGINDEX) {
            throw new IllegalArgumentException(String.format("%s.%s : The simID (%s) and variable (%s) are not registered.",
                                                              CLASSNAME_IN, methodName, aSimID, varName));
        }

        return this.encodeIndexToToken(curIndex, false);
    }
    /**
     * Returns a readonly token for aVarName on the current date of simulation.
     * <p>
     * Gives an exception if aVarName cannot be found.
     * </p>
     * @param aVarName the variable name to look for
     * @return the required token value
     */
    public int getTokenReadByVarName(String aVarName) {

        return this.getTokenReadByVarNameDateIndex(aVarName, curDateIndex, true);
    }
    /**
     * Returns a readonly token for aVarName on the current date of simulation.
     *
     * @param aVarName the variable name to look for
     * @param aCheckNotMissing when true, gives exception if aVarName could not
     * be found
     * @return the required token value
     */
    public int getTokenReadByVarName(String aVarName, boolean aCheckNotMissing) {

        return this.getTokenReadByVarNameDateIndex(aVarName, curDateIndex, aCheckNotMissing);
    }

    /**
     * Returns a token for aVarName on current date of simulation + aDelta.
     *
     * @param aVarName the variable name to look for
     * @param aDelta offset to use for the evaluation
     * @param aCheckNotMissing when true, gives exception if aVarName could not
     * be found
     * @return the required token value
     */
    public int getTokenReadByVarNameDelta(String aVarName, int aDelta, boolean aCheckNotMissing) {

        final String methodName = "getTokenReadByVarNameDelta";

        if (aDelta > 0) {
            throw new IllegalArgumentException(String.format("%s.%s : Delta value cannot be positive" +
                                                             " (call info: Variable=%s, Delta=%d).",
                                                              CLASSNAME_IN, methodName, aVarName, aDelta));
        }

        // find the date index pointed to by aDelta
        final int dateIndex = curDateIndex + aDelta;

        return this.getTokenReadByVarNameDateIndex(aVarName, dateIndex, aCheckNotMissing);
    }

    /**
     * Info function that returns a readonly token of the aVarName on aDate,
     * checks for exactly one not missing value
     *
     * @param aVarName the variable name to look for
     * @param aCheckNotMissing when true, gives exception when aVarName could
     * not be found
     * @param aDate the date to look for aVarName
     * @return the token value
     */
    public int getTokenReadByVarNameDate(String aVarName, LocalDate aDate, boolean aCheckNotMissing) {

        if (!this.isValidDate(aDate)) {
            throw new IllegalArgumentException(String.format("%s.getTokenReadByVarName : Date value (%s) not in valid range (%s, %s) for variable %s.",
                                                              CLASSNAME_IN, aDate, startDate, endDate, aVarName));
        }

        return this.getTokenReadByVarNameDateIndex(aVarName, DateUtils.diffDays(startDate, aDate), aCheckNotMissing);
    }

    /**
     * Info function that returns a readonly token to the requested aVarName,
     * checks for exactly 1 occurrence that gives a real value at the indicated
     * dateIndex of this request.
     * <p>
     * Otherwise the invalid token is returned. In that case, when
     * aCheckNotMissing=true, an exception is thrown. So when
     * aCheckNotMissing=true, it is guaranteed that there is a usable value for
     * aVarName, otherwise an exception will follow.
     * </p>
     *
     * @param aVarName the variable name to look for
     * @param aDateIndex the date index to look for
     * @param aCheckNotMissing when true, gives exception if the value could not
     * be found
     * @return the token value
     */
    public int getTokenReadByVarNameDateIndex(String aVarName, int aDateIndex, boolean aCheckNotMissing) {

        final String methodName = "getTokenReadByVarNameDateIndex";

        if (StringUtils.isBlank(aVarName)) {
            throw new IllegalArgumentException(String.format("%s.%s : The variable is empty.",
                                                              CLASSNAME_IN, methodName));
        }

        if (!this.isValidDateIndex(aDateIndex)) {
            throw new IllegalArgumentException(String.format("%s.%s : The date index (%d) is not valid.",
                                                              CLASSNAME_IN, methodName, aDateIndex));
        }

        final String varName = aVarName.toUpperCase(Locale.US);

        int result = this.invalidToken();

        int cnt = 0;
        for (int i = 0; i <= (simIDVarNameList.size() - 1); i++) {
            if (simIDVarNameList.get(i).varName.equals(varName)) {
                final SimIDVarNameListItem item = simIDVarNameList.get(i);
                if (item.hasValues[aDateIndex]) {
                    cnt++;

                    if (cnt == 1) {
                        result = this.encodeIndexToToken(i, false);
                        // keep on checking whether this is the only one
                    } else {
                        throw new IllegalStateException(String.format("%s.%s : More than one active value for variable %s.",
                                                                       CLASSNAME_IN, methodName, varName));
                    }
                }
            }
        }

        if (aCheckNotMissing && (!this.isValidToken(result))) {
            throw new IllegalStateException(String.format("%s.%s : The variable %s is not active.",
                                                           CLASSNAME_IN, methodName, varName));
        }

        return result;
    }
    // end: methods to obtain a token for a varname
    // -------------------------------------------------------------------------

    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    // begin: methods specifically dealing with simIDListIndex's

    /**
     * Register SimID and ClassName.
     *
     * @param aSimID the simID to register
     * @param aSimObjectClassName the class name of the simIDListIndex to
     * register
     */
    public void registerSimID(String aSimID, String aSimObjectClassName) {

        final String methodName = "registerSimID";

        if (StringUtils.isBlank(aSimID)) {
            throw new IllegalArgumentException(String.format("%s.%s : The simID is empty.",
                                                              CLASSNAME_IN, methodName));
        }

        if (StringUtils.isBlank(aSimObjectClassName)) {
            throw new IllegalArgumentException(String.format("%s.%s : The aSimObjectClassName is empty.",
                                                              CLASSNAME_IN, methodName));
        }

        int curIndex = this.getSimIDListIndex(aSimID.toUpperCase(Locale.US));

        if (curIndex == SimXChange.MISSINGINDEX) {
            SimIDListItem item = new SimIDListItem();

            item.simID              = aSimID.toUpperCase(Locale.US);
            item.simObjectClassName = aSimObjectClassName.toUpperCase(Locale.US);
            item.simIDState         = SimIDState.RUNNING;
            item.simIDMsg           = "";
            item.startDateIndex     = curDateIndex;
            item.endDateIndex       = SimXChange.MISSINGINDEX;

            // add to list at the end
            simIDList.add(item);

            curIndex = simIDList.size() - 1;
            simIDMap.put(aSimID.toUpperCase(Locale.US), curIndex);
        } else {
            throw new IllegalArgumentException(String.format("%s.%s : The simID %s is already registered.",
                                                              CLASSNAME_IN, methodName, aSimID));
        }
    }

    /**
     * Normal termination.
     * <p>
     * End date of aSimID is set to current date.
     * </p>
     *
     * @param aSimID the simID to terminate
     */
    public void terminateSimID(String aSimID) {

        this.terminateSimID(aSimID, false, "");
    }

    /**
     * Termination with possibility to store error message of the simulation.
     *
     * <p>
     * End date of aSimID is set to current date.
     * </p>
     *
     * @param aSimID to do
     * @param aError flag to indicate whether error occurred
     * @param aErrorMsg error message
     */
    public void terminateSimID(String aSimID, boolean aError, String aErrorMsg) {

        final String methodName = "terminateSimID";

        if (StringUtils.isBlank(aSimID)) {
            throw new IllegalArgumentException(String.format("%s.%s : The simID is empty.",
                                                              CLASSNAME_IN, methodName));
        }

        final int curIndex = this.getSimIDListIndex(aSimID.toUpperCase(Locale.US));

        if (curIndex == SimXChange.MISSINGINDEX) {
            throw new IllegalArgumentException(String.format("%s.%s : The simID %s is not registered.",
                                                              CLASSNAME_IN, methodName, aSimID));
        }

        SimIDListItem simIDItem = simIDList.get(curIndex);

        if ((simIDItem.simIDState == SimIDState.TERMINATED_NORMALLY) ||
            (simIDItem.simIDState == SimIDState.TERMINATED_ERROR)) {
            throw new IllegalArgumentException(String.format("%s.%s : The simID %s is already terminated.",
                                                              CLASSNAME_IN, methodName, aSimID));
        }

        if (!aError) {
            simIDItem.simIDState = SimIDState.TERMINATED_NORMALLY;
            simIDItem.simIDMsg   = "";
        } else {
            simIDItem.simIDState = SimIDState.TERMINATED_ERROR;
            simIDItem.simIDMsg   = aErrorMsg;
        }
        simIDItem.endDateIndex = curDateIndex;
    }

    /**
     * Info function to return list with all simIDListIndex's in order of time
     * registration
     *
     * @return the ArrayList
     */
    public ArrayList<String> getSimIDs() {

        final ArrayList<String> result = new ArrayList<>();

        for (SimIDListItem item : simIDList) {
            result.add(item.simID);
        }

        return result;
    }

    /**
     * Info function to return list with simID's that contained aVarName in
     * order of time registration.
     *
     * @param aVarName the variable name name for which the list is requested
     * @return the required list
     */
    public ArrayList<String> getSimIDsByVarName(String aVarName) {

        final String methodName = "getSimIDsByVarName";

        if (StringUtils.isBlank(aVarName)) {
            throw new IllegalArgumentException(String.format("%s.%s : The variable is empty.",
                                                              CLASSNAME_IN, methodName));
        }

        final String varName = aVarName.toUpperCase(Locale.US);

        final ArrayList<String> result = new ArrayList<>();

        for (int indexDate = 0; indexDate <= (simIDVarNameList.get(0).hasValues.length - 1); indexDate++) {

            for (int indexList = 0; indexList <= (simIDVarNameList.size() - 1); indexList++) {

                final String itemSimID   = simIDVarNameList.get(indexList).simID;
                final String itemVarName = simIDVarNameList.get(indexList).varName;

                if (simIDVarNameList.get(indexList).hasValues[indexDate] && itemVarName.equals(varName)) {
                    if (!result.contains(itemSimID)) {
                        result.add(itemSimID);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Info function to return list of all simID's of the given
     * SimObjectClassName.
     *
     * @param aSimObjectClassName the ClassName of SimObject to look for.
     * @return the required list
     */
    public ArrayList<String> getSimIDsBySimObjectClassName(String aSimObjectClassName) {

        final String methodName = "getSimIDsBySimObjectClassName";

        if (StringUtils.isBlank(aSimObjectClassName)) {
            throw new IllegalArgumentException(String.format("%s.%s : The simObjectClassName is empty.",
                                                              CLASSNAME_IN, methodName));
        }

        final String simObjectClassName = aSimObjectClassName.toUpperCase(Locale.US);

        final ArrayList<String> result = new ArrayList<>();

        for (SimIDListItem item : simIDList) {
            if (item.simObjectClassName.equals(simObjectClassName)) {
                result.add(item.simID);
            }
        }

        return result;
    }

    /**
     * Info function to get info object for aSimID.
     *
     * @param aSimID the sim id to work on
     * @return the required info function
     */
    public SimIDListItem getSimIDInfoBySimID(String aSimID) {

        final String methodName = "getSimIDInfoBySimID";

        if (StringUtils.isBlank(aSimID)) {
            throw new IllegalArgumentException(String.format("%s.%s : The simID is empty.",
                                                              CLASSNAME_IN, methodName));
        }

        final int curIndex = this.getSimIDListIndex(aSimID.toUpperCase(Locale.US));

        if (curIndex == SimXChange.MISSINGINDEX) {
            throw new IllegalArgumentException(String.format("%s.%s : The simID %s is not registered.",
                                                              CLASSNAME_IN, methodName, aSimID));
        }

        // return copy to caller
        return new SimIDListItem(simIDList.get(curIndex));
    }

    /**
     * Info function that returns the number of days that aSimID is active (e.g.
     * from March 10 to March 11 is 1 day).
     *
     * @param aSimID the sim id to work on
     * @return the number of days
     */
    public int getElapsedBySimID(String aSimID) {

        if (StringUtils.isBlank(aSimID)) {
            throw new IllegalArgumentException(String.format("%s.getElapsedBySimID : The simID is empty.",
                                                              CLASSNAME_IN));
        }

        final int curIndex = this.getSimIDListIndex(aSimID.toUpperCase(Locale.US));

        if (curIndex == SimXChange.MISSINGINDEX) {
            throw new IllegalArgumentException(String.format("%s.getElapsedBySimID : The simID %s is not registered.",
                                                              CLASSNAME_IN, aSimID));
        }

        return (curDateIndex - simIDList.get(curIndex).startDateIndex);
    }

    /**
     * Info function that returns true if there is at least one reqistration
     * with the mentioned ClassName.
     *
     * @param aSimObjectClassName the ClassName to look for
     * @return whether the registration is there
     */
    public boolean isSimObjectClassNameRunning(String aSimObjectClassName){

        final String methodName = "isSimObjectClassNameRunning";

        if (StringUtils.isBlank(aSimObjectClassName)) {
            throw new IllegalArgumentException(String.format("%s.%s : The simObjectClassName is empty.",
                                                              CLASSNAME_IN, methodName));
        }

        final String simObjectClassName = aSimObjectClassName.toUpperCase(Locale.US);

        boolean result = false;
        for (SimIDListItem item : simIDList) {
            if (item.simObjectClassName.equals(simObjectClassName)) {
                result = true;
                break;
            }
        }

        return result;
    }
    // end: methods specifically dealing with simIDListIndex's
    // -------------------------------------------------------------------------

    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    // begin: info methods across all variables

    /**
     * Info function to return the number of variables.
     *
     * @return the required value
     */
    public int getVarNameCount() {

        if (simIDVarNameList != null) {
            return simIDVarNameList.size();
        } else {
            return -1;
        }
    }

    /**
     * Info function to get a SimIDVarNameListItem item for aIndex.
     *
     * @param aIndex the index to work on
     * @return the required result
     */
    public SimIDVarNameListItem getVarNameItem(int aIndex) {

        final String methodName = "getVarNameItem";

        if (simIDVarNameList == null) {
            throw new IllegalStateException(String.format("%s.%s : varList object not created.", CLASSNAME_IN, methodName));
        }

        if (!RangeUtils.inRange(aIndex, 0, (simIDVarNameList.size() - 1))) {
            throw new IllegalArgumentException(String.format("%s.%s : Index (%d) not in valid range (0 to %d).",
                                                              CLASSNAME_IN, methodName, aIndex, (simIDVarNameList.size() - 1)));
        }
        // return copy to caller
        return new SimIDVarNameListItem(simIDVarNameList.get(aIndex));
    }

    /**
     * Info function to get the number of forced values.
     *
     * @return the required result
     */
    public int getStateVarForceCount() {

        if (stateVarForceValues != null) {
            return stateVarForceValues.size();
        } else {
            return -1;
        }
    }

    /**
     * Info function to get a StateVarForceItem item for aIndex.
     *
     * @param aIndex the index to work on
     * @return the required result
     */
    public StateVarForceItem getStateVarForceItemByIndex(int aIndex) {

        final String methodName = "getStateVarForceItemByIndex";

        if (stateVarForceValues == null) {
            throw new IllegalStateException(String.format("%s.%s : stateVarForceValues object not created.", CLASSNAME_IN, methodName));
        }

        if (!RangeUtils.inRange(aIndex, 0, (stateVarForceValues.size() - 1))) {
            throw new IllegalArgumentException(String.format("%s.%s : Index (%d) not in valid range (0 to %d).",
                                                              CLASSNAME_IN, methodName, aIndex, (stateVarForceValues.size() - 1)));
        }

        return new StateVarForceItem(stateVarForceValues.get(aIndex));
    }
    // end: info methods across all variables
    // -------------------------------------------------------------------------

    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    // begin: miscellaneous methods

    /**
     * Method to switch on full time series gathering (enabling certain output
     * query functions)
     * @param aVarName
     */
    public void SetFullTimeSeries(String aVarName) {

        final String methodName = "SetFullTimeSeries";

        if (StringUtils.isBlank(aVarName)) {
            throw new IllegalArgumentException(String.format("%s.%s : Empty variable.",
                    CLASSNAME_IN, methodName));
        }

        if (curDateIndex != -1) {
            throw new IllegalArgumentException(String.format("%s.%s : %s cannot be set to full time series once simulation has started",
                    CLASSNAME_IN, methodName, aVarName));
        }

        if (!fullTimeSeriesList.contains(aVarName)) {
            fullTimeSeriesList.add(aVarName);
        }
    }

    /**
     * Method to create a report file of the time series data and state forcing
     * contained in the object.
     * <p>
     * Separation defaults to comma, comments to // and empty values to -.
     * </p>
     *
     * @param aFilePath the path to write the report to
     */
    public void report(String aFilePath) {
        this.report(aFilePath, ",", "//", "-");
    }

    /**
     * Method to create a report file of the time series data and state forcing
     * contained in the object.
     *
     * @param aFilePath the path to write the report to
     * @param aSepar the separator char(s) to use between values
     * @param aCommentPrefix the chars(s) to use as comment prefix
     * @param aEmptyValue the char(s) to use when a value is empty
     */
    public void report(String aFilePath, String aSepar, String aCommentPrefix, String aEmptyValue) {

        final String methodName = "report";

        final String SEPAR         = aSepar         == null ? ","  : aSepar;
        final String COMMENTPREFIX = aCommentPrefix == null ? "#"  : aCommentPrefix;
        final String EMPTYVALUE    = aEmptyValue    == null ? "NA" : aEmptyValue;

        final String DATE         = "DATE";
        final String DATE_UNIT    = "";
        final String ELAPSED      = "ELAPSED";
        final String ELAPSED_UNIT = "Days";
        final String VAR          = "VAR";
        final String OLDVALUE     = "OldValue";
        final String NEWVALUE     = "NewValue";
        final String UNIT         = "Unit";

        final String SECTIONTIMESERIESSTATES = COMMENTPREFIX + " Time series of state and auxiliary variables";
        final String SECTIONFORCEDSTATES     = COMMENTPREFIX + " Time series of forced state and auxiliary variables";

        final String HEADER_WOFOST_VERSION   = COMMENTPREFIX + " WISS-WOFOST version 1.0"; // TODO: get real version number

        if (!true) {
            throw new IllegalArgumentException("hier nog doen");
        }

        if (StringUtils.isBlank(aFilePath)) {
            throw new IllegalArgumentException(String.format("%s.%s : Empty file path.",
                                                              CLASSNAME_IN, methodName));
        }

        if (StringUtils.isBlank(SEPAR)) {
            throw new IllegalArgumentException(String.format("%s.%s : Empty separator.",
                                                             CLASSNAME_IN, methodName));
        }

        if (StringUtils.isBlank(COMMENTPREFIX)) {
            throw new IllegalArgumentException(String.format("%s.%s : Empty comment prefix.",
                                                             CLASSNAME_IN, methodName));
        }

        if (StringUtils.isBlank(EMPTYVALUE)) {
            throw new IllegalArgumentException(String.format("%s.%s : Empty empty value.",
                                                             CLASSNAME_IN, methodName));
        }

        // see if the data contain only one owner or more, compare every single one against
        // every other single one for a mismatch, if found continue processing
        boolean onlyOneSimID = true;
        for (int i = 0; i <= (simIDVarNameList.size() - 2); i++) {
            for (int j = i + 1; j <= (simIDVarNameList.size() - 1); j++) {
                if (simIDVarNameList.get(i).simID.equals(simIDVarNameList.get(j).simID)) {
                    onlyOneSimID = false;
                    break;
                }
            }
        }

        try (PrintWriter fw = new PrintWriter(new BufferedWriter(new FileWriter(aFilePath)))) {
            // write header for the file
            fw.println(HEADER_WOFOST_VERSION);
            fw.println(COMMENTPREFIX);
            fw.println(COMMENTPREFIX + " RUN_ID   = " + runID);
            fw.println(COMMENTPREFIX + " RUN_DATE = " + LocalDate.now());
            fw.println(COMMENTPREFIX);

            if (simIDVarNameList.size() > 0) {
                // write unit line -----------------------------------------------------
                fw.println();
                fw.println(SECTIONTIMESERIESSTATES);
                fw.println();
                fw.print(COMMENTPREFIX + " Column units: ");
                fw.print(DATE_UNIT);
                fw.print(aSepar);
                fw.print(ELAPSED_UNIT);
                for (int i = 0; i <= (simIDVarNameList.size() - 1); i++) {
                    fw.print(aSepar);
                    fw.print(simIDVarNameList.get(i).scientificUnit.getUnitCaption());
                }
                fw.println(); // end of unit line

                // write header --------------------------------------------------------
                fw.println();

                fw.print(DATE);
                fw.print(aSepar);
                fw.print(ELAPSED);
                for (int i = 0; i <= (simIDVarNameList.size() - 1); i++) {
                    fw.print(aSepar);
                    if (onlyOneSimID) {
                        fw.print(simIDVarNameList.get(i).varName);
                    } else{
                        fw.print(simIDVarNameList.get(i).simIDVarName);
                    }
                }
                fw.println(); // end of header line

                // write data ----------------------------------------------------------
                for (int indexDate = 0; indexDate <= (simIDVarNameList.get(0).varValues.length - 1); indexDate++) {
                    fw.print(startDate.plusDays(indexDate).toString());

                    fw.print(aSepar);
                    fw.print(indexDate);

                    for (int indexState = 0; indexState <= (simIDVarNameList.size() - 1); indexState++) {
                        fw.print(aSepar);

                        double outputValue;
                        outputValue = simIDVarNameList.get(indexState).varValues[indexDate];
                        if (!Double.isNaN(outputValue)) {
                            fw.format("%g", outputValue);
                        } else {
                            fw.print(EMPTYVALUE);
                        }
                    }
                    fw.println(); // end of value line
                }
            }

            // TODO: should be written to a separate file

            if (stateVarForceValues.size() > 0) {
                // write forced state changes ------------------------------------------

                // count number of really changed values
                int stateVarForceChangeCnt = 0;
                for (StateVarForceItem stateVarForceItem : stateVarForceValues) {
                    if (stateVarForceItem.newValue != stateVarForceItem.oldValue) {
                        stateVarForceChangeCnt++;
                    }
                }

                if (stateVarForceChangeCnt > 0) {
                    // at least one state really changed
                    // write header
                    fw.println();
                    fw.println(SECTIONFORCEDSTATES);
                    fw.println();

                    fw.print(DATE);
                    fw.print(aSepar);
                    fw.print(VAR);
                    fw.print(aSepar);
                    fw.print(OLDVALUE);
                    fw.print(aSepar);
                    fw.print(NEWVALUE);
                    fw.print(aSepar);
                    fw.print(UNIT);

                    fw.println(); // end of header line

                    for (StateVarForceItem stateVarForceItem : stateVarForceValues) {
                        fw.print(this.getDateByDateIndex(stateVarForceItem.dateIndex).toString());
                        fw.print(aSepar);
                        fw.print(simIDVarNameList.get(stateVarForceItem.varListIndex).simIDVarName);

                        fw.print(aSepar);

                        final double outputValueOld = stateVarForceItem.oldValue;
                        final double outputValueNew = stateVarForceItem.newValue;

                        if (outputValueOld != outputValueNew) {
                            // do only when value has really changed
                            if (!Double.isNaN(outputValueOld)) {
                                fw.format("%g", outputValueOld);
                            }
                            else {
                                fw.print(EMPTYVALUE);
                            }

                            fw.print(aSepar);

                            if (!Double.isNaN(outputValueNew)) {
                                fw.format("%g", outputValueNew);
                            }
                            else {
                                fw.print(EMPTYVALUE);
                            }
                        }

                        fw.print(aSepar);

                        String outputString;
                        outputString = simIDVarNameList.get(stateVarForceItem.varListIndex).scientificUnit.getUnitCaption();
                        fw.print(outputString);

                        fw.println(); // end of value line
                    }
                }
            }

            fw.flush();

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
    // end: miscellaneous methods
    // -------------------------------------------------------------------------
}
