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
import org.apache.commons.lang3.StringUtils;

/**
 * Class which can hold external value, only SimXChange should set values in an instance !
 *
 * @author Daniel van Kraalingen (daniel.vankraalingen@wur.nl)
 * @version 1
 */
public class SimValueExternal {

    public static final String CLASSNAME_ST = SimValueExternal.class.getSimpleName();
    public        final String CLASSNAME_IN = this.getClass().getSimpleName();

    /** token to the value, can be set multiple times */
    public int                  t;
    /** value itself */
    private double              v;
    /** the unit, can be set only once */
    public final ScientificUnit u;
    /** the name, can be set only once */
    public final String         n;
    /** the name of the object that called (and created) this object (to make exception texts more clear) */
    private final String   caller;
    /** whether the variable of the token is terminated */
    private boolean    terminated;

    /**
     *
     * @param aVarName the name of the variable
     * @param aScientificUnit the unit of the variable
     * @param aCaller the name of the caller
     */
    public SimValueExternal(String aVarName, ScientificUnit aScientificUnit, String aCaller) {
        super();

        final String methodName = "SimValueExternal";

        if (StringUtils.isBlank(aCaller)) {
            throw new IllegalArgumentException(String.format("%s.%s : The caller is empty.",
                                                              CLASSNAME_IN, methodName));
        }

        if (StringUtils.isBlank(aVarName)) {
            throw new IllegalArgumentException(String.format("%s.%s : The variable is empty (caller=%s).",
                                                              CLASSNAME_IN, methodName, aCaller));
        }

        t          = SimXChange.invalidToken();
        u          = aScientificUnit;
        n          = aVarName.toUpperCase(Locale.US);
        caller     = aCaller;
        v          = Double.NaN;
        terminated = true;
    }

    @Override
    public String toString() {
        return String.format("%s{%s: value=%g (unit=%s)}", CLASSNAME_IN, n, v, u.getUnitCaption());
    }

    /**
     *
     * @return a caption for the object
     */
    public String getCaption() {
        return String.format("%s: value=%g (unit=%s)", n, v, u.getUnitCaption());
    }

    /**
     * @return value (.v property) itself, cannot be missing
     */
    public double v() {
        this.checkNotMissing();

        return v;
    }

    /**
     * @param v sets the value (allow setting of missing value)
     */
    public void setV(double v) {
        this.v = v;
    }

    public boolean isTerminated() {
        return terminated;
    }

    public void setTerminated(boolean terminated) {
        this.terminated = terminated;
    }

    /**
     * @return whether .v property is missing
     */
    public boolean isMissing() {
        return Double.isNaN(v);
    }

    /**
     *
     * @return whether .v property is not missing
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
            throw new IllegalStateException(String.format("%s.%s : Value of %s is not allowed to have a value (caller=%s).",
                                                           CLASSNAME_IN, methodName, n, caller));
        }
    }

    /**
     * checks whether the value (.v property) is not missing, if not, an IllegalStateException is thrown
     */
    public void checkNotMissing() {

        final String methodName = "checkNotMissing";

        if (Double.isNaN(v)) {
            throw new IllegalStateException(String.format("%s.%s : Value of %s is not allowed to be empty (caller=%s).",
                                                           CLASSNAME_IN, methodName, n, caller));
        }
    }
}
