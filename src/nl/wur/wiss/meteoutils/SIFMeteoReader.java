/*
 * Copyright 1988, 2013, 2016 Wageningen Environmental Research
 *
 * For licensing information read the included LICENSE.txt file.
 *
 * Unless required by applicable law or agreed to in writing, this software
 * is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied.
 */
package nl.wur.wiss_framework.meteoutils;

import nl.wur.wiss_framework.core.ScientificUnit;
import nl.wur.wiss_framework.core.ScientificUnitConversion;
import nl.wur.wiss_framework.fileutils.SIFReader;
import nl.wur.wiss_framework.mathutils.RangeUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Provides meteo data and station location
 *
 * @author Daniel van Kraalingen (daniel.vankraalingen@wur.nl)
 * @version 1
 */
public class SIFMeteoReader implements MeteoReader {

    public static final String CLASSNAME_ST = SIFMeteoReader.class.getSimpleName();
    public        final String CLASSNAME_IN = this.getClass().getSimpleName();

    private final String filePath;

    private final double longitudeDD;
    private final double latitudeDD;
    private final double altitudeM;

    private final LocalDate firstSourceDate;
    private final LocalDate lastSourceDate;

    private LocalDate firstPreparedDate = LocalDate.of(1900, 1, 1);
    private LocalDate lastPreparedDate  = LocalDate.of(1900, 1, 1);

    private final ArrayList<MeteoElement>               sourceElements = new ArrayList<>();
    private final ArrayList<ScientificUnit>             sourceUnits    = new ArrayList<>();

    private final ArrayList<MeteoElement>               preparedElements = new ArrayList<>(); // list of prepared elements
    private final ArrayList<ScientificUnit>             preparedUnits    = new ArrayList<>(); // unit in which each element is stored (in values)
    private final ArrayList<HashMap<LocalDate, Double>> preparedValues   = new ArrayList<>();

    private final SIFReader sifReader;

    private final int DEFAULTSECTION = 0;

    private boolean prepared;

    /**
     * Constructs object
     *
     * @param aFilePath path to read
     * @throws Exception if the longitude, latitude of altitude of the location cannot be obtained from aMeteoProvider
     */
    public SIFMeteoReader(String aFilePath) throws Exception {

        filePath = aFilePath;

        sifReader = new SIFReader(filePath);

        longitudeDD = Double.parseDouble(sifReader.getNameValue(SIFMeteoCodeConverter.LONGITUDEDD, DEFAULTSECTION, true));
        latitudeDD  = Double.parseDouble(sifReader.getNameValue(SIFMeteoCodeConverter.LATITUDEDD , DEFAULTSECTION, true));
        altitudeM   = Double.parseDouble(sifReader.getNameValue(SIFMeteoCodeConverter.ALTITUDEM  , DEFAULTSECTION, true));

        firstSourceDate = sifReader.getFirstDate(DEFAULTSECTION);
        lastSourceDate  = sifReader.getLastDate(DEFAULTSECTION);

        ArrayList<String> sifElements = sifReader.getTimeSeriesElements(DEFAULTSECTION);

        // convert string items to MeteoElement items
        for (String sifElement : sifElements) {
            sourceElements.add(SIFMeteoCodeConverter.getMeteoElementFromCode(sifElement));
            sourceUnits.add(SIFMeteoCodeConverter.getScientificUnitFromCode(sifElement));
        }

        prepared = false;
    }

    @Override
    public String toString() {
        return CLASSNAME_IN + "{filePath=" + filePath + "}";
    }

    // Javadoc is inherited
    @Override
    public double getLongitudeDD() {

        return longitudeDD;
    }

    // Javadoc is inherited
    @Override
    public double getLatitudeDD() {

        return latitudeDD;
    }

    // Javadoc is inherited
    @Override
    public double getAltitudeM() {

        return altitudeM;
    }

    // Javadoc is inherited
    @Override
    public LocalDate getSourceFirstDate() {

        return firstSourceDate;
    }

    // Javadoc is inherited
    @Override
    public LocalDate getSourceLastDate() {

        return lastSourceDate;
    }

    // Javadoc is inherited
    @Override
    public LocalDate getPreparedFirstDate() {

        final String methodName = "getPreparedFirstDate";

        if (!prepared) {
            throw new IllegalStateException(String.format("%s.%s : Object not prepared.",
                                                           CLASSNAME_IN, methodName));
        }

        return firstPreparedDate;
    }

    // Javadoc is inherited
    @Override
    public LocalDate getPreparedLastDate() {

        final String methodName = "getPreparedLastDate";

        if (!prepared) {
            throw new IllegalStateException(String.format("%s.%s : Object not prepared.",
                                                           CLASSNAME_IN, methodName));
        }

        return lastPreparedDate;
    }

    // Javadoc is inherited
    @Override
    public Set<MeteoElement> getSourceElements() {

        ArrayList<String> sifElements = sifReader.getTimeSeriesElements(DEFAULTSECTION);
        Set<MeteoElement> result = new HashSet<>();

        // convert string items to MeteoElement items
        for (String sifElement : sifElements) {
            result.add(SIFMeteoCodeConverter.getMeteoElementFromCode(sifElement));
        }

        return result;
    }

    // Javadoc is inherited
    public ScientificUnit getNativeUnit(MeteoElement aMeteoElement) {

        int i = sourceElements.indexOf(aMeteoElement);
        return sourceUnits.get(i);
    }

    // Javadoc is inherited
    @Override
    public Set<MeteoElement> getPreparedElements() {

        Set<MeteoElement> result = new HashSet<>();

        for (MeteoElement meteoElement : preparedElements) {
            result.add(meteoElement);
        }

        return result;
    }

    // Javadoc is inherited
    @Override
    public void prepare(LocalDate aStartDate, LocalDate aEndDate, Set<MeteoElement> aElements) {

        final String methodName = "prepare";

        if (aEndDate.isBefore(aStartDate)) {
            throw new IllegalArgumentException(String.format("%s.%s : No period for meteo preparation. End date must be later than start date (Start=%s, End=%s).",
                                                              CLASSNAME_IN, methodName, aStartDate.toString(), aEndDate.toString()));
        }

        if (!RangeUtils.inRange(aStartDate, firstSourceDate, lastSourceDate)) {
            throw new IllegalArgumentException(String.format("%s.%s : Illegal date (%s). Must be between source dates %s and %s.",
                                                              CLASSNAME_IN, methodName, aStartDate.toString(), firstSourceDate.toString(), lastSourceDate.toString()));
        }

        if (!RangeUtils.inRange(aEndDate, firstSourceDate, lastSourceDate)) {
            throw new IllegalArgumentException(String.format("%s.%s : Illegal date (%s). Must be between source dates %s and %s.",
                                                              CLASSNAME_IN, methodName, aStartDate.toString(), firstSourceDate.toString(), lastSourceDate.toString()));
        }

        if (aElements.isEmpty()) {
            throw new IllegalArgumentException(String.format("%s.%s : No meteo elements to prepare.",
                                                              CLASSNAME_IN, methodName));
        }

        preparedElements.clear();
        preparedUnits.clear();
        preparedValues.clear();

        firstPreparedDate = aStartDate;
        lastPreparedDate  = aEndDate;

        // try to match required elements with available elements in the meteoprovider

        for (MeteoElement element : aElements) {

                if (element == null) {
                    throw new IllegalArgumentException(String.format("%s.%s : Meteo elements cannot be null.",
                                                                      CLASSNAME_IN, methodName));
                }

                final String         stacName;
                final ScientificUnit stacUnit;

                switch (element) {
                    case Q_CU: {
                        // radiation is required, see what is in the provider ...

                        stacUnit = ScientificUnit.MJ_M2D1;
                        stacName = SIFMeteoCodeConverter.getCodeFromMeteoElement(element, stacUnit);

                        if (sifReader.getTimeSeriesElements(DEFAULTSECTION).contains(stacName)) {
                            // the provider contains a usable column, fill a new object with the data

                            preparedElements.add(element);
                            preparedUnits.add(stacUnit);
                            HashMap<LocalDate, Double> item = new HashMap<>();

                            LocalDate curDate = firstPreparedDate;
                            while (RangeUtils.inRange(curDate, firstPreparedDate, lastPreparedDate)) {
                                item.put(curDate, sifReader.getValue(curDate, DEFAULTSECTION, stacName));
                                curDate = curDate.plusDays(1);
                            }
                            preparedValues.add(item);
                        }
                        break;
                    }
                    case TM_MN: {
                        // minimum temperature is required, see what is in the provider ...

                        stacUnit = ScientificUnit.CELSIUS;
                        stacName = SIFMeteoCodeConverter.getCodeFromMeteoElement(element, stacUnit);

                        if (sifReader.getTimeSeriesElements(DEFAULTSECTION).contains(stacName)) {
                            // the provider contains a usable column, fill a new object with the data

                            preparedElements.add(element);
                            preparedUnits.add(stacUnit);
                            HashMap<LocalDate, Double> item = new HashMap<>();

                            LocalDate curDate = firstPreparedDate;
                            while (RangeUtils.inRange(curDate, firstPreparedDate, lastPreparedDate)) {
                                item.put(curDate, sifReader.getValue(curDate, DEFAULTSECTION, stacName));
                                curDate = curDate.plusDays(1);
                            }
                            preparedValues.add(item);
                        }
                        break;
                    }
                    case TM_MX: {
                        // maximum temperature is required, see what is in the provider ...

                        stacUnit = ScientificUnit.CELSIUS;
                        stacName = SIFMeteoCodeConverter.getCodeFromMeteoElement(element, stacUnit);

                        if (sifReader.getTimeSeriesElements(DEFAULTSECTION).contains(stacName)) {
                            // the provider contains a usable column, fill a new object with the data

                            preparedElements.add(element);
                            preparedUnits.add(stacUnit);
                            HashMap<LocalDate, Double> item = new HashMap<>();

                            LocalDate curDate = firstPreparedDate;
                            while (RangeUtils.inRange(curDate, firstPreparedDate, lastPreparedDate)) {
                                item.put(curDate, sifReader.getValue(curDate, DEFAULTSECTION, stacName));
                                curDate = curDate.plusDays(1);
                            }
                            preparedValues.add(item);
                        }
                        break;
                    }
                    case PR_CU: {
                        // precipitation is required, see what is in the provider ...

                        stacUnit = ScientificUnit.MM_D1;
                        stacName = SIFMeteoCodeConverter.getCodeFromMeteoElement(element, stacUnit);

                        if (sifReader.getTimeSeriesElements(DEFAULTSECTION).contains(stacName)) {
                            // the provider contains a usable column, fill a new object with the data

                            preparedElements.add(element);
                            preparedUnits.add(stacUnit);
                            HashMap<LocalDate, Double> item = new HashMap<>();

                            LocalDate curDate = firstPreparedDate;
                            while (RangeUtils.inRange(curDate, firstPreparedDate, lastPreparedDate)) {
                                item.put(curDate, sifReader.getValue(curDate, DEFAULTSECTION, stacName));
                                curDate = curDate.plusDays(1);
                            }
                            preparedValues.add(item);
                        }
                        break;
                    }
                    case VP_AV: {
                        // vapour pressure is required, see what is in the provider ...

                        stacUnit = ScientificUnit.HPA;
                        stacName = SIFMeteoCodeConverter.getCodeFromMeteoElement(element, stacUnit);

                        if (sifReader.getTimeSeriesElements(DEFAULTSECTION).contains(stacName)) {
                            // the provider contains a usable column, fill a new object with the data

                            preparedElements.add(element);
                            preparedUnits.add(stacUnit);
                            HashMap<LocalDate, Double> item = new HashMap<>();

                            LocalDate curDate = firstPreparedDate;
                            while (RangeUtils.inRange(curDate, firstPreparedDate, lastPreparedDate)) {
                                item.put(curDate, sifReader.getValue(curDate, DEFAULTSECTION, stacName));
                                curDate = curDate.plusDays(1);
                            }
                            preparedValues.add(item);
                        }
                        break;
                    }
                    case WS2_AV: {
                        // wind speed is required, see what is in the provider ...

                        stacUnit = ScientificUnit.M_S;
                        stacName = SIFMeteoCodeConverter.getCodeFromMeteoElement(element, stacUnit);

                        if (sifReader.getTimeSeriesElements(DEFAULTSECTION).contains(stacName)) {
                            // the provider contains a usable column, fill a new object with the data

                            preparedElements.add(element);
                            preparedUnits.add(stacUnit);
                            HashMap<LocalDate, Double> item = new HashMap<>();

                            LocalDate curDate = firstPreparedDate;
                            while (RangeUtils.inRange(curDate, firstPreparedDate, lastPreparedDate)) {
                                item.put(curDate, sifReader.getValue(curDate, DEFAULTSECTION, stacName));
                                curDate = curDate.plusDays(1);
                            }
                            preparedValues.add(item);
                        }
                        break;
                    }
                    default:
                        throw new IllegalArgumentException(String.format("%s.%s : No processing setup for meteo element %s.",
                                                                          CLASSNAME_IN, methodName, element.toString()));
            }
        }
        prepared = true;
    }

    // Javadoc is inherited
    @Override
    public double getValue(LocalDate aCurDate, MeteoElement aElement, ScientificUnit aUnit) {

        final String methodName = "getValue";

        if (!prepared) {
            throw new IllegalStateException(String.format("%s.%s : Object not prepared.",
                                                           CLASSNAME_IN, methodName));
        }

        if (!RangeUtils.inRange(aCurDate, firstPreparedDate, lastPreparedDate)) {
            throw new IllegalArgumentException(String.format("%s.%s : Illegal date (%s). Must be between prepared dates %s and %s.",
                                                              CLASSNAME_IN, methodName, aCurDate.toString(), firstPreparedDate.toString(), lastPreparedDate.toString()));
        }

        int index = preparedElements.indexOf(aElement);
        if (index == -1) {
            throw new IllegalArgumentException(String.format("%s.%s : Meteo element %s has not been prepared.",
                                                              CLASSNAME_IN, methodName, aElement.toString()));
        }

        // get the value from the stored data
        double result = preparedValues.get(index).getOrDefault(aCurDate, Double.NaN);

        if (Double.isNaN(result)) {
            throw new IllegalArgumentException(String.format("%s.%s : Meteo element %s is not available on date=%s.",
                                                              CLASSNAME_IN, methodName, aElement.toString(), aCurDate.toString()));
        }

        result = ScientificUnitConversion.convert(aElement.toString(), result, preparedUnits.get(index), aUnit);

        return result;
    }
}
