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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import static nl.wur.wiss_framework.mathutils.MathUtils.doubleToInt;

/**
 * Does conversion of a value in one scientific unit to another scientific unit
 *
 * @author Daniel van Kraalingen (daniel.vankraalingen@wur.nl)
 * @version 1
 */
public class ScientificUnitConversion {

    public static final String CLASSNAME_ST = ScientificUnitConversion.class.getSimpleName();
    public        final String CLASSNAME_IN = this.getClass().getSimpleName();

    // class logger
    private static final Log LOGGER = LogFactory.getLog(ScientificUnitConversion.class);

    /**
     * Converts a value to a different scientific unit. Does not do bounds check, intentionally.
     *
     * @param aInputValue the value to convert
     * @param aSourceUnit the original unit of the value
     * @param aTargetUnit the target unit of the value
     */
    private static double _convert(double aInputValue, ScientificUnit aSourceUnit, ScientificUnit aTargetUnit) {
        double result = Double.NaN;

        switch (aSourceUnit) {
            case HPA:
                switch (aTargetUnit) {
                    case MBAR:
                        result = aInputValue;
                        break;
                }
                break;
            case MBAR:
                switch (aTargetUnit) {
                    case HPA:
                        result = aInputValue;
                        break;
                }
                break;
            case CNT_M2:
                switch (aTargetUnit) {
                    case CNT_HA:
                        result = aInputValue * 10000.0;
                        break;
                }
                break;
            case CNT_HA:
                switch (aTargetUnit) {
                    case CNT_M2:
                        result = aInputValue * 0.0001;
                        break;
                }
                break;
            case KG_M2:
                switch (aTargetUnit) {
                    case KG_HA:
                        result = aInputValue * 10000.0;
                        break;
                }
                break;
            case KG_HA:
                switch (aTargetUnit) {
                    case KG_M2:
                        result = aInputValue * 0.0001;
                        break;
                }
                break;
            case MJ_M2D1:
                switch (aTargetUnit) {
                    case KJ_M2D1:
                        result = aInputValue * 1000.0;
                        break;
                    case J_M2D1:
                        result = aInputValue * 1000000.0;
                        break;
                }
                break;
            case KJ_M2D1:
                switch (aTargetUnit) {
                    case MJ_M2D1:
                        result = aInputValue * 0.001;
                        break;
                    case J_M2D1:
                        result = aInputValue * 1000.0;
                        break;
                }
                break;
            case J_M2D1:
                switch (aTargetUnit) {
                    case MJ_M2D1:
                        result = aInputValue * 0.000001;
                        break;
                    case KJ_M2D1:
                        result = aInputValue * 0.001;
                        break;
                }
                break;
            case MM:
                switch (aTargetUnit) {
                    case CM:
                        result = aInputValue * 0.1;
                        break;
                    case M:
                        result = aInputValue * 0.001;
                        break;
                }
                break;
            case CM:
                switch (aTargetUnit) {
                    case MM:
                        result = aInputValue * 10.0;
                        break;
                    case M:
                        result = aInputValue * 0.01;
                        break;
                }
                break;
            case M:
                switch (aTargetUnit) {
                    case MM:
                        result = aInputValue * 1000.0;
                        break;
                    case CM:
                        result = aInputValue * 100.0;
                        break;
                }
                break;
            case MM_D1:
                switch (aTargetUnit) {
                    case CM_D1:
                        result = aInputValue * 0.1;
                        break;
                    case M_D1:
                        result = aInputValue * 0.001;
                        break;
                }
                break;
            case CM_D1:
                switch (aTargetUnit) {
                    case MM_D1:
                        result = aInputValue * 10.0;
                        break;
                    case M_D1:
                        result = aInputValue * 0.01;
                        break;
                }
                break;
            case M_D1:
                switch (aTargetUnit) {
                    case MM_D1:
                        result = aInputValue * 1000.0;
                        break;
                    case CM_D1:
                        result = aInputValue * 100.0;
                        break;
                }
                break;
            case CELSIUS:
                switch (aTargetUnit) {
                    case FAHRENHEIT:
                        result = 32.0 + aInputValue * 9.0 / 5.0;
                        break;
                    case KELVIN:
                        result = aInputValue + 273.15;
                        break;
                }
                break;
            case KELVIN:
                switch (aTargetUnit) {
                    case FAHRENHEIT:
                        result = 32.0 + (aInputValue - 273.15) * 9.0 / 5.0;
                        break;
                    case CELSIUS:
                        result = aInputValue - 273.15;
                        break;
                }
                break;
            case FAHRENHEIT:
                switch (aTargetUnit) {
                    case CELSIUS:
                        result = (aInputValue - 32.0) * 5.0 / 9.0;
                        break;
                    case KELVIN:
                        result = 273.15 + (aInputValue - 32.0) * 5.0 / 9.0;
                        break;
                }
                break;
        }
        return result;
    }

    /**
     * Converts a value to a different scientific unit. Does not do bounds check, intentionally.
     *
     * @param aInputName the name of the value to convert (used in error messages)
     * @param aInputValue the value to convert
     * @param aSourceUnit the original unit of the value
     * @param aTargetUnit the target unit of the value
     * @return the converted number
     */
    public static Number convert(String aInputName, Number aInputValue, ScientificUnit aSourceUnit, ScientificUnit aTargetUnit) {

        final String methodName = "convert";

        if (aInputValue instanceof Integer) {
            return convert(aInputName, (int) aInputValue, aSourceUnit, aTargetUnit);
        } else if (aInputValue instanceof Double) {
            return convert(aInputName, (double) aInputValue, aSourceUnit, aTargetUnit);
        }

        throw new IllegalArgumentException(String.format("%s.%s : Don't know how to convert numeric data of type %s.",
                                                          CLASSNAME_ST, methodName, aInputValue.getClass().getSimpleName()));
    }

    /**
     * Converts a value to a different scientific unit. Does not do bounds check, intentionally.
     *
     * @param aInputName the name of the value to convert (used in error messages)
     * @param aInputValue the value to convert
     * @param aSourceUnit the original unit of the value
     * @param aTargetUnit the target unit of the value
     * @return the converted number
     */
    public static int convert(String aInputName, int aInputValue, ScientificUnit aSourceUnit, ScientificUnit aTargetUnit) {
        final double inputValue  = aInputValue;
        final double outputValue = ScientificUnitConversion.convert(aInputName, inputValue, aSourceUnit, aTargetUnit);

        return doubleToInt(outputValue);
    }

    /**
     * Converts a value to a different scientific unit. Does not do bounds check, intentionally.
     *
     * @param aInputName the name of the value to convert (used in error messages)
     * @param aInputValue the value to convert
     * @param aSourceUnit the original unit of the value
     * @param aTargetUnit the target unit of the value
     * @return the converted number
     */
    public static double convert(String aInputName, double aInputValue, ScientificUnit aSourceUnit, ScientificUnit aTargetUnit) {
        double result;

        final String methodName = "convert";

        if ((aSourceUnit == null) || (aTargetUnit == null)) {
            throw new IllegalArgumentException(String.format("%s.%s : Cannot convert %s, sourceUnit and / or targetUnit equal to NULL.",
                                                              CLASSNAME_ST, methodName, aInputName));
        }

        if (aTargetUnit.equals(aSourceUnit) || Double.isNaN(aInputValue)) {
            result = aInputValue;
        }
        else {
            // source and target units not identical, try unit conversion
            if (ScientificUnit.NA.equals(aSourceUnit) ||
                ScientificUnit.NA.equals(aTargetUnit)) {
                throw new IllegalArgumentException(String.format("%s.%s : Cannot convert %s, value=%g, unit=\"%s\" into unit=\"%s\".",
                                                                  CLASSNAME_ST, methodName, aInputName, aInputValue,
                                                                  aSourceUnit.getUnitCaption(),
                                                                  aTargetUnit.getUnitCaption()));
            }

            result = ScientificUnitConversion._convert(aInputValue, aSourceUnit, aTargetUnit);

            if (Double.isNaN(result)) {
                throw new AssertionError(String.format("%s.%s : No conversion defined for %s, value=%g, unit=\"%s\" into unit=\"%s\".",
                                                        CLASSNAME_ST, methodName, aInputName, aInputValue,
                                                        aSourceUnit.getUnitCaption(),
                                                        aTargetUnit.getUnitCaption()));
            }
        }
        return result;
    }
}
