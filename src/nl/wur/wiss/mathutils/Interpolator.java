/*
 * Copyright 1988, 2013, 2016 Alterra, Wageningen UR
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import nl.wur.wiss.core.ScientificUnit;
import nl.wur.wiss.core.ScientificUnitConversion;
import org.apache.commons.math3.linear.RealMatrix;

/**
 *
 * @author Daniel van Kraalingen
 */
public class Interpolator implements Serializable {

    public static final String CLASSNAME_ST = Interpolator.class.getSimpleName();
    public        final String CLASSNAME_IN = this.getClass().getSimpleName();

    private static final long serialVersionUID = 1L;

    // object fields
    private final String id; // used for identification by the user of the data in case errors occur
    private InterpolatorExtrapolationType extrapolationType;
    private final ScientificUnit xUnit;
    private final ScientificUnit yUnit;

    // slopes (s), will contain one element less, because 10 xy pair, contain 9 slopes !
    // So slope of 2nd to 3rd element is stored in s[1] (index 1 points to second element)
    private final ArrayList<Double> x;
    private final ArrayList<Double> y;
    private final ArrayList<Double> s;

    private boolean slopesValid;
    private int     previousSegmentIndex;

    static final int INITIALELEMENTCNT = 100;


    /**
     * Creates an Interpolator with the specified ID, extrapolation type, and
     * (X,Y) values.
     *
     * @param aID Used for identification by the user of the data in case errors
     * occur
     * @param aType Interpolation extrapolation type
     * @param aX X values of data points (array must be same length as Y)
     * @param aXUnit the unit of the X values
     * @param aY Y values of data points (array must be same length as Y)
     * @param aYUnit the unit of the Y values
     * @return The created interpolator instance
     */
    public static Interpolator create(String aID, InterpolatorExtrapolationType aType, double[] aX, ScientificUnit aXUnit, double[] aY, ScientificUnit aYUnit) {

        final String methodName = "create";

        if (aX.length != aY.length) {
            throw new IllegalArgumentException(String.format("%s.%s : X and Y arrays do not have the same size, ID=%s.",
                                                              CLASSNAME_ST, methodName, aID));
        }

        Interpolator ip = new Interpolator(aID, aXUnit, aYUnit);
        ip.setExtrapolationType(aType);

        for (int i = 0; i < aX.length; i++) {
            ip.add(aX[i], aY[i]);
        }

        return ip;
    }


    /**
     * Creates an Interpolator with the specified ID, extrapolation type, and
     * (X,Y) values.
     *
     * @param aID Used for identification by the user of the data in case errors
     * occur
     * @param aType Interpolation extrapolation type
     * @param aMatrix holding the X and Y data
     * @param aUnits the units of the X and Y values
     * @return The created interpolator instance
     */
    public static Interpolator create(String aID, InterpolatorExtrapolationType aType, RealMatrix aMatrix, ScientificUnit[] aUnits) {

        final String methodName = "create";

        if ((aMatrix == null) || (aMatrix.getColumnDimension() != 2)) {
            throw new IllegalArgumentException(String.format("%s.%s : A 2 column matrix must be specified containing the X and Y data for the Interpolator, ID=%s.",
                                                              CLASSNAME_ST, methodName, aID));
        }
        if ((aUnits == null) || (aUnits.length != 2)) {
            throw new IllegalArgumentException(String.format("%s.%s : Scientific units for X and Y must be specified for the Interpolator, ID=%s.",
                                                              CLASSNAME_ST, methodName, aID));
        }

        Interpolator ip = new Interpolator(aID, aUnits[0], aUnits[1]);
        ip.setExtrapolationType(aType);

        for (int i = 0; i < aMatrix.getRowDimension(); i++) {
            ip.add(aMatrix.getEntry(i, 0), aMatrix.getEntry(i, 1));
        }

        return ip;
    }

    /**
     * Simple instantiation without data yet.
     *
     * @param aID a string describing the object's data in user's terms
     * @param aXUnit the native unit for X
     * @param aYUnit the native unit for Y
     */
    public Interpolator(String aID, ScientificUnit aXUnit, ScientificUnit aYUnit) {
        id = aID; // allow to be empty

        // initialize with zero elements but already allocated memory of INITIALELEMENTCNT elements
        x = new ArrayList<>(INITIALELEMENTCNT);
        y = new ArrayList<>(INITIALELEMENTCNT);
        s = new ArrayList<>(INITIALELEMENTCNT - 1);

        xUnit = aXUnit;
        yUnit = aYUnit;

        slopesValid = false;

        previousSegmentIndex = -1;

        extrapolationType = InterpolatorExtrapolationType.NOEXTRAPOLATION;
    }

    /**
     * recalculate the slopes in the s ArrayList
     */
    private void ensureSlopesValid() {

        if (!slopesValid) {
            int iNext;
            double slope;

            for (int iCur = 0; iCur <= x.size() - 2; iCur++) {
                iNext = iCur + 1;

                // note that divide by zero cannot occur, because no 2 x values are allowed to be the same
                slope = (y.get(iNext) - y.get(iCur)) / (x.get(iNext) - x.get(iCur));
                s.set(iCur, slope);
            }
            slopesValid = true;
        }
    }

    /**
     *
     * @return the count of the number of items
     */
    public int count() {
        return x.size();
    }

    /**
     * Return the x value of a index number
     *
     * @param aIndex the Index to get X for
     * @return the X value at index=aIndex
     */
    public double getX(int aIndex) {

        final String methodName = "getX";

        if (!RangeUtils.inRange(aIndex, 0, (x.size() - 1))) {
            throw new IllegalArgumentException(String.format("%s.%s : Invalid index (%d) for X, ID=%s.",
                                                              CLASSNAME_ST, methodName, aIndex, id));
        }
        return x.get(aIndex);
    }

    /**
     * Return the y value of a index number
     *
     * @param aIndex the Index to get Y for
     * @return the Y value at index=aIndex
     */
    public double getY(int aIndex) {

        final String methodName = "getY";

        if (!RangeUtils.inRange(aIndex, 0, (y.size() - 1))) {
            throw new IllegalArgumentException(String.format("%s.%s : Invalid index (%d) for Y, ID=%s.",
                                                              CLASSNAME_ST, methodName, aIndex, id));
        }

        return y.get(aIndex);
    }

    /**
     * Return the minimum of the X values (is the first one, since they need to
     * be provided in ascending order)
     *
     * @return the first of the array of X values
     */
    public double getXMin() {

        final String methodName = "getXMin";

        if (!(x.size() >= 1)) {
            throw new IllegalStateException(String.format("%s.%s : At least 1 point required, ID=%s.",
                                                           CLASSNAME_ST, methodName, id));
        }
        return x.get(0);
    }

    /**
     * Return the maximum of the X values (is the last one, since they need to
     * be provided in ascending order)
     *
     * @return the last of the array of X values
     */
    public double getXMax() {

        final String methodName = "getXMax";

        if (!(x.size() >= 1)) {
            throw new IllegalStateException(String.format("%s.%s : At least 1 point required, ID=%s.",
                                                           CLASSNAME_ST, methodName, id));
        }
        return x.get(x.size() - 1);
    }

    /**
     * Return flag whether the provided x is within the range of x values
     *
     * @param aX the X to use in the range test
     * @return whether aX is in the range of X values
     */
    public boolean inXRange(double aX) {

        final String methodName = "inXRange";

        // at least 2 points required
        if (!(x.size() >= 2)) {
            throw new IllegalStateException(String.format("%s.%s : At least 2 points required, ID=%s.",
                                                           CLASSNAME_ST, methodName, id));
        }
        return RangeUtils.inRange(aX, this.getXMin(), this.getXMax());
    }

    /**
     * Returns the current setting for extrapolation
     *
     * @return type of extrapolation
     */
    public InterpolatorExtrapolationType getExtrapolationType() {
        return extrapolationType;
    }

    /**
     * Sets the provided extrapolation type
     *
     * @param ExtrapolationType the ExtrapolationType to set from now on
     */
    public void setExtrapolationType(InterpolatorExtrapolationType ExtrapolationType) {
        this.extrapolationType = ExtrapolationType;
    }

    /**
     * Add the given x and y to the list of provided x and y values
     *
     * @param aX the X value of the pair to add (native unit will be applied)
     * @param aY the Y value of the pair to add (native unit will be applied)
     */
    public void add(double aX, double aY) {

        final String methodName = "add";

        if (Double.isNaN(aX)) {
            throw new IllegalArgumentException(String.format("%s.%s : Cannot add NaN value for X, ID=%s.",
                                                              CLASSNAME_ST, methodName, id));
        }

        if (Double.isNaN(aY)) {
            throw new IllegalArgumentException(String.format("%s.%s : Cannot add NaN value for Y, ID=%s.",
                                                              CLASSNAME_ST, methodName, id));
        }

        slopesValid = false;

        // initialize to first item
        previousSegmentIndex = 0;

        if (this.count() >= 1) {
            // there is at least one element, so check that aX is greater than the previous one
            if (!(aX > this.getXMax())) {
                throw new IllegalArgumentException(String.format("%s.%s : X (%g) not increasing (previous %g), ID=%s.",
                                                                  CLASSNAME_ST, methodName, aX, this.getXMax(), id));
            }
        }

        x.add(aX);
        y.add(aY);
        s.add(Double.NaN);
    }


    /**
     * Returns the interpolated value for aX, using the scientific units
     * specified when the interpolator instance was created.
     *
     * @param aX value to interpolate for
     * @return interpolated Y value for aX
     */
    public double interpolate(double aX) {
        return interpolate(aX, xUnit, yUnit);
    }

    /**
     * Returns the interpolated value for aX, using the provided scientific
     * units
     *
     * @param aX the X to use in the interpolation
     * @param aXUnit the unit of X
     * @param aYUnit the required unit for U
     * @return the interpolated Y value
     */
    public double interpolate(double aX, ScientificUnit aXUnit, ScientificUnit aYUnit) {

        final String methodName = "interpolate";

        int highX = x.size() - 1;

        if (Double.isNaN(aX)) {
            throw new IllegalArgumentException(String.format("%s.%s : Cannot interpolate on NaN value, ID=%s.",
                                                              CLASSNAME_ST, methodName, id));
        }

        if (highX <= 0) {
            throw new IllegalStateException(String.format("%s.%s : At least 2 points required for interpolation, ID=%s.",
                                                           CLASSNAME_ST, methodName, id));
        }

        if (extrapolationType == InterpolatorExtrapolationType.NOEXTRAPOLATION) {
            if (aX < x.get(0) || (aX > x.get(highX))) {
                throw new IllegalArgumentException(String.format("%s.%s : extrapolation not allowed on X value (%g), ID=%s.",
                                                                  CLASSNAME_ST, methodName, aX, id));
            }
        }

        int locateIndex; // the lowest index of the segment to interpolate from
        locateIndex = -1;

        // convert X to the value in the native unit, before doing interpolation
        double xNativeUnit = ScientificUnitConversion.convert(id, aX, aXUnit, xUnit);

        if (RangeUtils.inRange(xNativeUnit, x.get(previousSegmentIndex), x.get(previousSegmentIndex + 1))) {
            // previous section is still the right one
            locateIndex = previousSegmentIndex;
        }

        if (locateIndex == -1) {
            // segment not found yet

            if (previousSegmentIndex < (highX - 1)) {
                // there is a next one from the previous one so try it
                if (RangeUtils.inRange(xNativeUnit, x.get(previousSegmentIndex + 1), x.get(previousSegmentIndex + 2))) {
                    // section next from the previous one is the right one
                    locateIndex = previousSegmentIndex + 1;
                }
            }
        }

        if (locateIndex == -1) {
            // segment not found yet

            if (xNativeUnit == x.get(0)) {
                locateIndex = 0;
            } else if (xNativeUnit == x.get(highX)) {
                locateIndex = highX - 1;
            }
        }

        if (locateIndex == -1) {
            // segment not found yet

            // see if the given x is in the x range of the data (excluding boundaries), otherwise searching
            // will fail anyhow
            if (xNativeUnit > x.get(0) && (xNativeUnit < x.get(highX))) {
                locateIndex = Arrays.binarySearch(x.toArray(), xNativeUnit);
                if (locateIndex < 0) {
                    // exact hit not found, calculate index from return value
                    locateIndex = Math.abs(locateIndex) - 2;
                }
            }
        }

        double result;

        if (locateIndex != -1) {
            // segment found, do interpolation
            if (xNativeUnit == x.get(locateIndex)) {
                result = y.get(locateIndex);  // X is exactly on first X of segment
            } else if (xNativeUnit == x.get(locateIndex + 1)) {
                result = y.get(locateIndex + 1); // X is exactly on second X of segment
            } else {
                // do real linear interpolation
                this.ensureSlopesValid();

                result = y.get(locateIndex) + s.get(locateIndex) * (xNativeUnit - x.get(locateIndex));
            }

            previousSegmentIndex = locateIndex;
        } else {
            // segment NOT found, do extrapolation
            switch (extrapolationType) {
                case CONSTANTEXTRAPOLATION:
                    if (xNativeUnit < x.get(0)) {
                        result = y.get(0);
                    } else if (xNativeUnit > x.get(highX)) {
                        result = y.get(highX);
                    }
                    else {
                        throw new AssertionError(String.format("Internal error 79EFA300-5DCA-4B86-9DA5-78566B087ED4, ID=%s",
                                                                id));
                    }
                    break;
                case SLOPEEXTRAPOLATION:
                    this.ensureSlopesValid();

                    if (xNativeUnit< x.get(0)) {
                        result = y.get(0) + s.get(0) * (xNativeUnit - x.get(0));
                    } else if (xNativeUnit > x.get(highX)) {
                        result = y.get(highX) + s.get(highX - 1) * (xNativeUnit - x.get(highX));
                    }
                    else {
                        throw new AssertionError(String.format("Internal error 99EFA300-5DCA-4BB6-9DA5-78566B087ED4, ID=%s",
                                                                id));
                    }
                    break;
                default:
                    throw new AssertionError(String.format("Internal error 0E309CFF-BFD7-4005-B9F6-ADC718C039AD, ID=%s",
                                                            id));
            }
        }

        // convert Y to the value in the requested unit
        result = ScientificUnitConversion.convert(id, result, yUnit, aYUnit);

        return result;
    }

    /**
     * Get the native unit for X
     *
     * @return the unit X is given in
     */
    public ScientificUnit getxUnit() {
        return xUnit;
    }

    /**
     * Get the native unit for Y
     *
     * @return the unit Y is given in
     */
    public ScientificUnit getyUnit() {
        return yUnit;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 53 * hash + Objects.hashCode(this.id);
        hash = 53 * hash + Objects.hashCode(this.extrapolationType);
        hash = 53 * hash + Objects.hashCode(this.xUnit);
        hash = 53 * hash + Objects.hashCode(this.yUnit);
        hash = 53 * hash + Objects.hashCode(this.x);
        hash = 53 * hash + Objects.hashCode(this.y);
        hash = 53 * hash + Objects.hashCode(this.s);
        hash = 53 * hash + (this.slopesValid ? 1 : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Interpolator other = (Interpolator) obj;
        if (this.slopesValid != other.slopesValid) {
            return false;
        }
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        if (this.extrapolationType != other.extrapolationType) {
            return false;
        }
        if (this.xUnit != other.xUnit) {
            return false;
        }
        if (this.yUnit != other.yUnit) {
            return false;
        }
        if (!Objects.equals(this.x, other.x)) {
            return false;
        }
        if (!Objects.equals(this.y, other.y)) {
            return false;
        }
        if (!Objects.equals(this.s, other.s)) {
            return false;
        }
        return true;
    }
}
