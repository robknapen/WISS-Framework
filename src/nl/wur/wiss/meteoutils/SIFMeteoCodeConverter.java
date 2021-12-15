/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.wur.wiss_framework.meteoutils;

import nl.wur.wiss_framework.core.ScientificUnit;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;

/**
 *
 * @author kraal001
 */
public class SIFMeteoCodeConverter {

    public static final String CLASSNAME_ST = SIFMeteoCodeConverter.class.getSimpleName();
    public        final String CLASSNAME_IN = this.getClass().getSimpleName();

    public static final String OBJECTTYPE = "METEO";

    public static final String LONGITUDEDD = "LONGITUDEDD";
    public static final String LATITUDEDD  = "LATITUDEDD";
    public static final String ALTITUDEM   = "ALTITUDEM";

    public static MeteoElement getMeteoElementFromCode(String aElementCode) {

        final String methodName = "getMeteoElementFromCode";

        if (StringUtils.isBlank(aElementCode)) {
            throw new IllegalArgumentException(String.format("%s.%s : The meteo element code is empty.",
                                                             CLASSNAME_ST, methodName));
        }

        final String ucElementCode = aElementCode.toUpperCase(Locale.US);

        final MeteoElement result;

        switch (ucElementCode) {
            case "TM_AV_C":
                result = MeteoElement.TM_AV;
                break;
            case "TM_MN_C":
                result = MeteoElement.TM_MN;
                break;
            case "TM_MX_C":
                result = MeteoElement.TM_MX;
                break;
            case "Q_CU_MJM2":
                result = MeteoElement.Q_CU;
                break;
            case "PR_CU_MM":
                result = MeteoElement.PR_CU;
                break;
            case "VP_AV_HPA":
                result = MeteoElement.VP_AV;
                break;
            case "WS2_AV_MS":
                result = MeteoElement.WS2_AV;
                break;
            case "WS10_AV_MS":
                result = MeteoElement.WS10_AV;
                break;
            case "RH_AV_PRC":
                result = MeteoElement.RH_AV;
                break;
            case "TD_AV_C":
                result = MeteoElement.TD_AV;
                break;
            case "E0_MM":
                result = MeteoElement.E0;
                break;
            case "ES0_MM":
                result = MeteoElement.ES0;
                break;
            case "ET0_MM":
                result = MeteoElement.ET0;
                break;
            default:
                throw new AssertionError(String.format("%s.%s: unknown meteo element code (%s)",
                                                        CLASSNAME_ST, methodName, aElementCode));
        }

        return result;
    }

    public static ScientificUnit getScientificUnitFromCode(String aElementCode) {

        final String methodName = "getScientificUnitFromCode";

        if (StringUtils.isBlank(aElementCode)) {
            throw new IllegalArgumentException(String.format("%s.%s : The meteo element code is empty.",
                                                             CLASSNAME_ST, methodName));
        }

        final String ucElementCode = aElementCode.toUpperCase(Locale.US);

        final ScientificUnit result;

        switch (ucElementCode) {
            case "TM_AV_C":
                result = ScientificUnit.CELSIUS;
                break;
            case "TM_MN_C":
                result = ScientificUnit.CELSIUS;
                break;
            case "TM_MX_C":
                result = ScientificUnit.CELSIUS;
                break;
            case "Q_CU_MJM2":
                result = ScientificUnit.MJ_M2D1;
                break;
            case "PR_CU_MM":
                result = ScientificUnit.MM_D1;
                break;
            case "VP_AV_HPA":
                result = ScientificUnit.HPA;
                break;
            case "WS2_AV_MS":
                result = ScientificUnit.M_S;
                break;
            case "WS10_AV_MS":
                result = ScientificUnit.M_S;
                break;
            case "RH_AV_PRC":
                result = ScientificUnit.PRC;
                break;
            case "TD_AV_C":
                result = ScientificUnit.CELSIUS;
                break;
            case "E0_MM":
                result = ScientificUnit.MM_D1;
                break;
            case "ES0_MM":
                result = ScientificUnit.MM_D1;
                break;
            case "ET0_MM":
                result = ScientificUnit.MM_D1;
                break;

            default:
                throw new AssertionError(String.format("%s.%s: unknown meteo element code (%s)",
                                                        CLASSNAME_ST, methodName, aElementCode));
        }

        return result;
    }

    public static String getCodeFromMeteoElement(MeteoElement aMeteoElement, ScientificUnit aScientificUnit) {

        final String methodName = "getCodeFromMeteoElement";

        String result;

        switch (aMeteoElement) {
            case TM_AV:
                result = "TM_AV";

                switch (aScientificUnit) {
                    case CELSIUS:
                        result = result + "_C";
                        break;
                    default:
                        throwUnitException(aScientificUnit);
                }

                break;
            case TM_MN:
                result = "TM_MN";

                switch (aScientificUnit) {
                    case CELSIUS:
                        result = result + "_C";
                        break;
                    default:
                        throwUnitException(aScientificUnit);
                }

                break;
            case TM_MX:
                result = "TM_MX";

                switch (aScientificUnit) {
                    case CELSIUS:
                        result = result + "_C";
                        break;
                    default:
                        throwUnitException(aScientificUnit);
                }

                break;
            case Q_CU:
                result = "Q_CU";

                switch (aScientificUnit) {
                    case MJ_M2D1:
                        result = result + "_MJM2";
                        break;
                    default:
                        throwUnitException(aScientificUnit);
                }

                break;
            case PR_CU:
                result = "PR_CU";

                switch (aScientificUnit) {
                    case MM_D1:
                        result = result + "_MM";
                        break;
                    default:
                        throwUnitException(aScientificUnit);
                }

                break;
            case VP_AV:
                result = "VP_AV";

                switch (aScientificUnit) {
                    case HPA:
                        result = result + "_HPA";
                        break;
                    default:
                        throwUnitException(aScientificUnit);
                }

                break;
            case WS2_AV:
                result = "WS2_AV";

                switch (aScientificUnit) {
                    case M_S:
                        result = result + "_MS";
                        break;
                    default:
                        throwUnitException(aScientificUnit);
                }

                break;
            case WS10_AV:
                result = "WS10_AV";

                switch (aScientificUnit) {
                    case M_S:
                        result = result + "_MS";
                        break;
                    default:
                        throwUnitException(aScientificUnit);
                }

                break;
            case E0:
                result = "E0";

                switch (aScientificUnit) {
                    case MM_D1:
                        result = result + "_MM";
                        break;
                    default:
                        throwUnitException(aScientificUnit);
                }

                break;
            case ES0:
                result = "ES0";

                switch (aScientificUnit) {
                    case MM_D1:
                        result = result + "_MM";
                        break;
                    default:
                        throwUnitException(aScientificUnit);
                }

                break;
            case ET0:
                result = "ET0";

                switch (aScientificUnit) {
                    case MM_D1:
                        result = result + "_MM";
                        break;
                    default:
                        throwUnitException(aScientificUnit);
                }

                break;

            default:
                throw new AssertionError(String.format("%s.%s: unknown MeteoElement code (%s)",
                                                        CLASSNAME_ST, methodName, aMeteoElement.name()));
        }
        return result;
    }

    private static void throwUnitException(ScientificUnit unit) {

        final String methodName = "throwUnitException";

        throw new AssertionError(String.format("%s.%s : unknown ScientificUnit (%s)",
                                                CLASSNAME_ST, methodName, unit.name()));
    }
}
