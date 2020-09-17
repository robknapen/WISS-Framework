/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.wur.wiss.meteoutils;

import static java.lang.Math.*;

/**
 *
 * @author kraal001
 */
public class WeatherUtils {

    public static final String CLASSNAME_ST = WeatherUtils.class.getSimpleName();
    public        final String CLASSNAME_IN = this.getClass().getSimpleName();

    // conversion factor from windspeed at 10 m height to windspeed at 2m height
    // conversion, natural logs : log(2.0/0.033)/log(10.0/0.033)
    private static final double CONVWIND10MTO2M = 0.71832604;

    // conversion factor from windspeed at 10 m height to windspeed at 2m height
    // = 4.87 / (Ln(67.8 * 10 - 5.42 (Ln is natural logarithm)
    private static final double CONVWIND10MTO2MFAO = 0.747951075167944;

    public static double avgAirPressureFractionFAO(double aAltitudeM, double aAirTempK) {

        return pow(((aAirTempK - 0.0065 * aAltitudeM) / aAirTempK), 5.26);
    }

    // formula for FAO Penman-Monteith formula
    // result in kPa / degree Celsius
    public static double satVapPressSlopeAtTempFAOkPaC(double aTempC) {

        // 4098 should actually be 17.27 * 237.3 = 4098.171 !
        // but to keep 100 % compatible with FAO text, we leave this

        // square aTempC + 237.3 (no fast function exists in standard math
        double tmp = aTempC + 237.3;
        tmp = tmp * tmp;

        final double result = 4098.0 * satVapPressAtTempFAOkPa(aTempC) / tmp;

        return result;
    }

    // formula for FAO Penman-Monteith formula
    // result in kPa
    public static double satVapPressAtTempFAOkPa(double aTempC) {

        return 0.6108 * exp((17.27 * aTempC)/(aTempC + 237.3));
    }

    public static double wind10MTo2M(double aWindspeed10M) {

        final String methodName = "wind10MTo2M";

        if (aWindspeed10M < 0.0) {
            throw new IllegalArgumentException(String.format("%s.%s : The wind speed at 10 m cannot be less than zero.",
                                                              CLASSNAME_ST, methodName));
        }

        if (aWindspeed10M > 0.0) {
            return aWindspeed10M * CONVWIND10MTO2M;
        } else {
            return 0.0;
        }
    }

    public static double wind10MTo2MFAO(double aWindspeed10M) {

        final String methodName = "wind10MTo2MFAO";

        if (aWindspeed10M < 0.0) {
            throw new IllegalArgumentException(String.format("%s.%s : The wind speed at 10 m cannot be less than zero.",
                                                              CLASSNAME_ST, methodName));
        }

        if (aWindspeed10M > 0.0) {
            return aWindspeed10M * CONVWIND10MTO2MFAO;
        } else {
            return 0.0;
        }
    }
}
