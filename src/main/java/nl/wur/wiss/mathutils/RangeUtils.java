/*
 * Copyright 2016 Alterra, Wageningen UR
 *
 * Licensed under the EUPL, Version 1.1 or as soon they
 * will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the
 * Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in
 * writing, software distributed under the Licence is
 * distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */
package nl.wur.wiss.mathutils;

import java.time.LocalDate;

/**
 * Various utilities for working with numerical and date ranges.
 *
 * @author Daniel van Kraalingen
 * @author Rob Knapen
 */
public class RangeUtils {

    public static final String CLASSNAME_ST = RangeUtils.class.getSimpleName();
    public        final String CLASSNAME_IN = this.getClass().getSimpleName();

    /**
     * Specify only undisputed simple range types here.
     */
    public static enum RangeType {
        ALL,          // all real and integer numbers
        ZEROPOSITIVE, // zero to positive infinity (real and integer number)
        POSITIVE,     // only positive to infinity (excluding zero) (real and integer number)
        ZERONEGATIVE, // zero to negative infinity (real and integer number)
        NEGATIVE,     // only negative to infinity (excluding zero) (real and integer number)
        ZEROONE,      // only between zero and one, a fraction (including zero and one) (only real numbers)
        TEMPCELSIUS   // only between -273.15 and infinity
    }

    // bounds are inclusive !
    public static boolean inRange(int aValue, int aLower, int aUpper) {

        final String methodName = "inRange";

        if (aLower > aUpper) {
            throw new IllegalArgumentException(String.format("%s.%s (int version) : The lower bound (%d) is larger than the upper bound (%d).",
                                                              CLASSNAME_ST, methodName, aLower, aUpper));
        }

        return ((aValue >= aLower) && (aValue <= aUpper));
    }

    // bounds are inclusive !
    public static boolean inRange(double aValue, double aLower, double aUpper) {

        final String methodName = "inRange";

        if (Double.isNaN(aValue)) {
            throw new IllegalArgumentException(String.format("%s.%s (double version) : The value to test is missing.",
                                                              CLASSNAME_ST, methodName, aLower, aUpper));
        }

        if (Double.isNaN(aLower)) {
            throw new IllegalArgumentException(String.format("%s.%s (double version) : The lower bound is missing.",
                                                              CLASSNAME_ST, methodName, aLower, aUpper));
        }

        if (Double.isNaN(aUpper)) {
            throw new IllegalArgumentException(String.format("%s.%s (double version) : The upper bound is missing.",
                                                              CLASSNAME_ST, methodName, aLower, aUpper));
        }


        if (aLower > aUpper) {
            throw new IllegalArgumentException(String.format("%s.%s (double version) : The lower bound (%g) is larger than the upper bound (%g).",
                                                              CLASSNAME_ST, methodName, aLower, aUpper));
        }

        return ((aValue >= aLower) && (aValue <= aUpper));
    }

    // bounds are inclusive !
    public static boolean inRange(LocalDate aValue, LocalDate aEarlier, LocalDate aLater) {

        final String methodName = "inRange";

        if (aEarlier.isAfter(aLater)) {
            throw new IllegalArgumentException(String.format("%s.%s (LocalDate version) : The earlier date argument (%s) is later than the later date argument (%s).",
                                                              CLASSNAME_ST, methodName, aEarlier, aLater));
        }

        return ((aValue.equals(aEarlier)  || aValue.equals(aLater)) ||
                (aValue.isAfter(aEarlier) && aValue.isBefore(aLater)));
    }

    /**
     * Returns the specified value if it is in the given (min, max) range, or
     * the specified min or max.
     *
     * @param aValue to ensure range for
     * @param aLower minimum boundary of the range
     * @param aUpper maximum boundary of the range
     * @return value, or either the min or max boundary when value exceeds it
     */
    public static int ensureRange(int aValue, int aLower, int aUpper) {

        final String methodName = "ensureRange";

        if (aLower > aUpper) {
            throw new IllegalArgumentException(String.format("%s.%s (int version) : The lower bound (%d) is larger than the upper bound (%d).",
                                                              CLASSNAME_ST, methodName, aLower, aUpper));
        }

        return Math.min(Math.max(aValue, aLower), aUpper);
    }

    /**
     * Returns the specified value if it is in the given (aLower, max) range, or
     * the specified aLower or aUpper.
     *
     * @param aValue to ensure range for
     * @param aLower minimum boundary of the range
     * @param aUpper maximum boundary of the range
     * @return value, or either the aLower or max boundary when value exceeds it
     */
    public static double ensureRange(double aValue, double aLower, double aUpper) {

        final String methodName = "ensureRange";

        if (Double.isNaN(aValue)) {
            throw new IllegalArgumentException(String.format("%s.%s (double version) : The value to test is missing.",
                                                              CLASSNAME_ST, methodName, aLower, aUpper));
        }

        if (Double.isNaN(aLower)) {
            throw new IllegalArgumentException(String.format("%s.%s (double version) : The lower bound is missing.",
                                                              CLASSNAME_ST, methodName, aLower, aUpper));
        }

        if (Double.isNaN(aUpper)) {
            throw new IllegalArgumentException(String.format("%s.%s (double version) : The upper bound is missing.",
                                                              CLASSNAME_ST, methodName, aLower, aUpper));
        }

        if (aLower > aUpper) {
            throw new IllegalArgumentException(String.format("%s.%s (double version) : The lower bound (%g) is larger than the upper bound (%g).",
                                                              CLASSNAME_ST, methodName, aLower, aUpper));
        }

        return Math.min(Math.max(safeExpr(aValue), aLower), aUpper);
    }

    /**
     * Note: do not implement isMissing for Double, use Double.isNaN for that
     *
     * @param aValue the data value to test
     * @return whether that provided date is a missing value
     */
    public static boolean isMissing(LocalDate aValue) {
        return (aValue == null);
    }

    public static boolean isNotMissing(LocalDate aValue) {
        return (aValue != null);
    }

    public static double getLowerBound(RangeType aRangeType) {

        final String methodName = "getLowerBound";

        switch(aRangeType) {
            case ALL:
                return Double.NEGATIVE_INFINITY;
            case ZEROPOSITIVE:
                return 0.0;
            case POSITIVE:
                return Double.MIN_VALUE;
            case ZERONEGATIVE:
                return Double.NEGATIVE_INFINITY;
            case NEGATIVE:
                return Double.NEGATIVE_INFINITY;
            case ZEROONE:
                return 0.0;
            case TEMPCELSIUS:
                return -273.15;
            default:
                throw new AssertionError(String.format("%s.%s : The range type is not recognized.",
                                                        CLASSNAME_ST, methodName));
        }
    }

    public static double getUpperBound(RangeType aRangeType) {

        final String methodName = "getUpperBound";

        switch(aRangeType) {
            case ALL:
                return Double.POSITIVE_INFINITY;
            case ZEROPOSITIVE:
                return Double.POSITIVE_INFINITY;
            case POSITIVE:
                return Double.POSITIVE_INFINITY;
            case ZERONEGATIVE:
                return 0.0;
            case NEGATIVE:
                return -Double.MIN_VALUE;
            case ZEROONE:
                return 1.0;
            case TEMPCELSIUS:
                return Double.POSITIVE_INFINITY;
            default:
                throw new AssertionError(String.format("%s.%s : The range type is not recognized.",
                                                        CLASSNAME_ST, methodName));
        }
    }

    /**
     * Guaranteed to give a valid value (not infinite, not missing). This function
     * is useful when doing floating point calculations where divide by zero's could
     * occur. These do not lead to run time errors in plain java, slowing down software
     * development, cause errors are not detected immediately.
     *
     * @param aValue the value to test
     * @return the value itself when it is ok
     */
    public static double safeExpr(double aValue) {

        final String methodName = "safeExpr";

        if (Double.isFinite(aValue)) {
            return aValue;
        } else if (Double.isInfinite(aValue)) {
            if (aValue == Double.POSITIVE_INFINITY) {
                throw new IllegalArgumentException(String.format("%s.%s : The expression gives a positive infinite result.",
                                                                  CLASSNAME_ST, methodName));
            } else {
                throw new IllegalArgumentException(String.format("%s.%s : The expression gives a negative infinite result.",
                                                                  CLASSNAME_ST, methodName));
            }
        } else if (Double.isNaN(aValue)) {
            throw new IllegalArgumentException(String.format("%s.%s : The expression gives a NaN result.",
                                                              CLASSNAME_ST, methodName));
        } else {
            // execution should never get here
            throw new AssertionError(String.format("%s.%s : The expression gives an undetermined result.",
                                                    CLASSNAME_ST, methodName));
        }
    }
}
