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

import java.util.Locale;
import nl.wur.wiss.mathutils.RangeUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Class which can hold auxiliary value
 *
 * @author Daniel van Kraalingen (daniel.vankraalingen@wur.nl)
 * @version 1
 */
public class SimValueAux {

    public static final String CLASSNAME_ST = SimValueAux.class.getSimpleName();
    public        final String CLASSNAME_IN = this.getClass().getSimpleName();

    /** token to the value, can be set only once */
    private int                 t;
    /** value itself */
    public double               v;
    /** the unit, can be set only once */
    public final ScientificUnit u;
    /** the name, can be set only once */
    public final String         n;
    /** the simID, can be set only once */
    public final String         simID;
    /** the name of the object that called (and created) this object (to make exception texts more clear) */
    public final double        lb;
    /** upper bound, can be set only once */
    public final double        ub;

    /**
     * Constructs object with a predefined range type (RangeUtils.RangeType)
     *
     * @param aSimID the SimID of the SimObject owning the variable
     * @param aVarName the name of the variable
     * @param aScientificUnit the unit of the variable
     * @param aRangeType the range type of the variable
     */
    public SimValueAux(String aSimID, String aVarName, ScientificUnit aScientificUnit, RangeUtils.RangeType aRangeType) {

        this(aSimID, aVarName, aScientificUnit, RangeUtils.getLowerBound(aRangeType), RangeUtils.getUpperBound(aRangeType));
    }

    /**
     * Constructs object with a predefined range type (RangeUtils.RangeType)
     *
     * @param aSimID the SimID of the SimObject owning the variable
     * @param aVarName the name of the variable
     * @param aScientificUnit the unit of the variable
     * @param aLowerBound the allowed lower bound
     * @param aUpperBound the allowed upper bound
     */
    public SimValueAux(String aSimID, String aVarName, ScientificUnit aScientificUnit, Double aLowerBound, Double aUpperBound) {
        super();

        final String methodName = "SimValueAux";

        if (StringUtils.isBlank(aSimID)) {
            throw new IllegalArgumentException(String.format("%s.%s : The simID is empty.",
                                                              CLASSNAME_IN, methodName));
        }

        if (StringUtils.isBlank(aVarName)) {
            throw new IllegalArgumentException(String.format("%s.%s : The variable is empty (simID=%s).",
                                                              CLASSNAME_IN, methodName, aSimID));
        }

        if (aLowerBound > aUpperBound) {
            throw new IllegalArgumentException(String.format("%s.%s : The lower bound (%g) is larger than the upperbound (%g) (simID=%s).",
                                                              CLASSNAME_IN, methodName, aLowerBound, aUpperBound, aSimID));
        }

        t      = SimXChange.invalidToken();
        u      = aScientificUnit;
        n      = aVarName.toUpperCase(Locale.US);
        simID  = aSimID.toUpperCase(Locale.US);
        lb     = aLowerBound;
        ub     = aUpperBound;

        v  = Double.NaN;
    }

    @Override
    public String toString() {
        return String.format("%s{%s.%s: auxvalue=%g (unit=%s)}", CLASSNAME_IN, simID, n, v, u.getUnitCaption());
    }

    /**
     * @return a caption for the object
     */
    public String getCaption() {
        return String.format("%s.%s: auxvalue=%g (unit=%s)", simID, n, v, u.getUnitCaption());
    }

    /**
     * @return value of the variable's token
     */
    public int getT() {
        return t;
    }

    /**
     * Sets the token for the variable, can be set only once
     *
     * @param aToken to do
     */
    public void setT(int aToken) {

        final String methodName = "setT";

        if (!SimXChange.isValidToken(this.t) && SimXChange.isValidToken(aToken)) {
            t = aToken;
        } else {
            // todo foutmelding beter maken
            throw new IllegalStateException(String.format("%s.%s : The token can be set only once (simID=%s).",
                                                           CLASSNAME_IN, methodName, simID));
        }
    }

    /**
     * @return whether the value (.v property) is missing
     */
    public boolean isMissing() {
        return Double.isNaN(v);
    }

    /**
     * @return whether the value (.v property) is not missing
     */
    public boolean isNotMissing() {
        return !Double.isNaN(v);
    }

    /**
     * checks whether the value (.v property) is missing, if not, an IllegalStateException is thrown
     */
    public void checkMissing() {

        final String methodName = "checkMissing";

        if (!Double.isNaN(v)) {
            throw new IllegalStateException(String.format("%s.%s : %s.%s is not allowed to have a value.",
                                                           CLASSNAME_IN, methodName, simID, n));
        }
    }

    /**
     * checks whether the value (.v property) is not missing, if not, an IllegalStateException is thrown
     */
    public void checkNotMissing() {

        final String methodName = "checkNotMissing";

        if (Double.isNaN(v)) {
            throw new IllegalStateException(String.format("%s.%s : %s.%s is not allowed to be empty.",
                                                           CLASSNAME_IN, methodName, simID, n));
        }
    }
}
