/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.wur.wiss_framework.mathutils;

/**
 *
 * @author kraal001
 */
public class MathUtils {

    public static final String CLASSNAME_ST = MathUtils.class.getSimpleName();
    public        final String CLASSNAME_IN = this.getClass().getSimpleName();

    /**
     * Rounds a double to an integer, checking for valid conversion (within min
     * and max value of an integer type.
     *
     * @param aDouble the value to safe round to an integer
     * @return the converted integer value
     */
    public static int doubleToInt(double aDouble) {

        final String methodName = "doubleToInt";

        if (!RangeUtils.inRange(aDouble, Integer.MIN_VALUE, Integer.MAX_VALUE))
            throw new IllegalArgumentException(String.format("%s.%s : The double argument (%g) is not in the integer bounds (%d, %d).",
                                                              CLASSNAME_ST, methodName, aDouble, Integer.MIN_VALUE, Integer.MAX_VALUE));

        return (int)Math.round(aDouble);
    }
}
