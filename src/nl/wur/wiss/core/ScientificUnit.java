/*
 * Copyright 1988, 2013, 2016 Wageningen Environmental Research
 *
 * For licensing information read the included LICENSE.unitCaption file.
 *
 * Unless required by applicable law or agreed to in writing, this software
 * is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied.
 */
package nl.wur.wiss_framework.core;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Definitions of scientific units for simulation variables in general.
 *
 * @author Daniel van Kraalingen (daniel.vankraalingen@wur.nl)
 * @author Rob Knapen (rob.knapen@wur.nl)
 * @version 1
 */
public enum ScientificUnit {
    /** not applicable */
    NA                  ("[NA]"),
    /** dimensionless  */
    NODIM               ("[-]"),
    /** dimensionless, volume.volume-1, for bulk density a.o., must have 0-1 range */
    NODIM_VOLUME        ("[volume.volume-1]"),
    /** dimensionless, mass.mass-1, for conversion factors etc., must have 0-1 range */
    NODIM_MASS          ("[mass.mass-1]"),
    /** dimensionless, area.area-1, for leaf area index etc., must NOT have 0-inf range */
    NODIM_AREA          ("[area.area-1]"),
    /** dimensionless, radiation energy.radiation energy-1, for atmospheric transmission etc., must have 0-inf range */
    NODIM_RADIATION     ("[radiation energy.radiation energy-1]"),
    /** percentage (no range restriction !) */
    PRC                 ("%s"),
    /** hectopascal */
    HPA                 ("[hpa]"),
    /** millibar */
    MBAR                ("[mbar]"),
    /** ppm */
    PPM                 ("[ppm]"),
    /** yearly change in ppm */
    PPM_Y               ("[ppm.y-1]"),
    /** number per square meter */
    CNT_M2              ("[no.m-2]"),
    /** number per hectare */
    CNT_HA              ("[no.ha-1]"),
    /** kilogram per square meter */
    KG_M2               ("[kg.m-2]"),
    /** kilogram per hectare */
    KG_HA               ("[kg.ha-1]"),
    /** kilogram per kilogram per day */
    KG_KG1D1            ("[kg.kg-1.d-1]"),
    /** kilogram per hectare per hour */
    KG_HA1HR1           ("[kg.ha-1.hr-1]"),
    /** hectare per hectare per day */
    HA_HA1D1            ("[ha.ha-1.d-1]"),
    /** hectare per kilogram */
    HA_KG               ("[ha.kg-1]"),
    /** degrees Celsius */
    CELSIUS             ("[C]"),
    /** Celsius degree days */
    CELSIUS_DAYS        ("[C.d]"),
    /** degrees Fahrenheit */
    FAHRENHEIT          ("[F]"),
    /** Kelvin */
    KELVIN              ("[K]"),
    /** megajoules per square meter per day */
    MJ_M2D1             ("[mj.m-2.d-1]"),
    /** kilojoules per square meter per day */
    KJ_M2D1             ("[kj.m-2.d-1]"),
    /** joules per square meter per day */
    J_M2D1              ("[j.m-2.d-1]"),
    /** millimeter */
    MM                  ("[mm]"),
    /** millimeter per day */
    MM_D1               ("[mm.d-1]"),
    /** centimeter */
    CM                  ("[cm]"),
    /** centimeter per day */
    CM_D1               ("[cm.d-1]"),
    /** meter */
    M                   ("[m]"),
    /** meter per second */
    M_S                 ("[m.s-1]"),
    /** meter per day */
    M_D1                ("[m.d-1]"),
    /** angular decimal degrees for latitude, longitude etc. */
    ANGULARDD           ("[deg]"),
    /** decimal hours */
    HOUR                ("[hr]"),
    /** normal calendar date */
    DATE                ("[date]"),
    /** relative number of days */
    DATEREL             ("[Date relative]"),
    /** day */
    DAYS                ("[d]"),
    /** 1 / day-1 */
    PER_DAY             ("[d-1]"),
    /** day number in the year since Jan 1st (1/jan = doy 1) */
    DAYOFYEAR           ("[doy]"),
    /** year */
    YEAR                ("[year]"),
    /** light use efficiency// light use efficiency */
    KG_HA1HR1J1_M2S1    ("[kg.ha-1.hr-1.J-1.m2.s1]");

    public static final String CLASSNAME_ST = ScientificUnit.class.getSimpleName();
    public        final String CLASSNAME_IN = this.getClass().getSimpleName();

    // fields
    private final String unitCaption;

    private ScientificUnit(String aUnitCaption) {
        this.unitCaption = aUnitCaption;
    }

    public String getUnitCaption() {
        return unitCaption;
    }

    @Override
    public String toString() {
        return CLASSNAME_IN + "{unitCaption=" + this.unitCaption + '}';
    }

    /**
     * Finds the ScientificUnit enum value whose textual unit representation
     * matches the specified unitCaption, or throws an IllegalArgumentException
     * when no match can be found. The search is done case insensitive.
     *
     * @param txt with unit representation to search for
     * @return ScientificUnit enum value that matches
     * @throws IllegalArgumentException when no match can be found
     */
    public static ScientificUnit findByTxt(String txt) throws IllegalArgumentException {
        return Arrays.stream(ScientificUnit.values())
                .filter(e -> e.getUnitCaption().equalsIgnoreCase(txt))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No matching ScientificUnit value found for: " + txt));
    }

    /**
     * Decode a text representation of one or more scientific units into an
     * array of ScientificUnit values. The text representation is expected to
     * start with a [ and end with an ]. Definitions of the units should be
     * separated by ;. For each unit a match (on the text representation) needs
     * to be available in the enumeration or otherwise an IllegalStateException
     * will be thrown.
     *
     * @param combinedUnitsTxt containing one or multiple scientific units
     * @return array of matching ScientificUnit values
     */
    public static ScientificUnit[] fromTxt(String combinedUnitsTxt) {
        String[] unitTexts = combinedUnitsTxt.substring(1, combinedUnitsTxt.length() - 1).split(";");

        ArrayList<ScientificUnit> result = new ArrayList<>();
        for (String unitText : unitTexts) {
            result.add(ScientificUnit.findByTxt("[" + unitText + "]"));
        }

        return result.toArray(new ScientificUnit[]{});
    }

}
