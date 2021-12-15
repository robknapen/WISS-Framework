/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.wur.wiss_framework.meteoutils;

import nl.wur.wiss_framework.fileutils.SIFReader;
import nl.wur.wiss_framework.fileutils.SIFReaderLoadModeType;
import nl.wur.wiss_framework.fileutils.SIFTimeStampConverter;
import nl.wur.wiss_framework.mathutils.RangeUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.Set;

/**
 *
 * @author kraal001
 */
public class SIFMeteoWriter {

    public static final String CLASSNAME_ST = SIFMeteoWriter.class.getSimpleName();
    public        final String CLASSNAME_IN = this.getClass().getSimpleName();

    // fields for which the MeteoReader interface has no provision, but need to be in the sif file
    private SIFReaderLoadModeType loadMode = SIFReaderLoadModeType.REPLACE;

    /**
     * gets the load mode that will be written to the sif file
     *
     * @return the load mode
     */
    public SIFReaderLoadModeType getLoadMode() {
        return loadMode;
    }

    /**
     * Sets the load mode that will be written to the sif file
     *
     * @param loadMode the load mode that will be used
     */
    public void setLoadMode(SIFReaderLoadModeType loadMode) {
        this.loadMode = loadMode;
    }

    /**
     * writes prepared data of the provided object to the mentioned path as a sif file
     *
     * @param aMeteoReader to do
     * @param aFilePath to do
     * @throws IllegalArgumentException to do
     * @throws java.lang.Exception to do
     */
    public void writePreparedData(MeteoReader aMeteoReader, String aFilePath) throws Exception {

        final String methodName = "writePreparedData";

        if (aMeteoReader == null) {
            throw new IllegalArgumentException(String.format("%s.%s : empty meteo reader.", CLASSNAME_IN, methodName));
        }

        if (aMeteoReader.getPreparedElements().isEmpty()) {
            throw new IllegalArgumentException(String.format("%s.%s : no prepared elements in provided MeteoReader object.", CLASSNAME_IN, methodName));
        }

        if (StringUtils.isBlank(aFilePath)) {
            throw new IllegalArgumentException(String.format("%s.%s : empty file path.", CLASSNAME_IN, methodName));
        }

        PrintWriter fw = new PrintWriter(new BufferedWriter(new FileWriter(aFilePath)));

        try {
            fw.println(String.format("%s this file is meant for debugging purposes only", SIFReader.COMMENTPREFIX));

            // write section break
            fw.println(String.format("%s%s %s", SIFReader.SECTIONPREFIX, SIFMeteoCodeConverter.OBJECTTYPE, loadMode.name()));

            // write required data for SIFMeteoReader
            fw.println(String.format("%s = %g", SIFMeteoCodeConverter.LONGITUDEDD, aMeteoReader.getLongitudeDD()));
            fw.println(String.format("%s = %g", SIFMeteoCodeConverter.LATITUDEDD , aMeteoReader.getLatitudeDD()));
            fw.println(String.format("%s = %g", SIFMeteoCodeConverter.ALTITUDEM  , aMeteoReader.getAltitudeM()));
            fw.println();

            final Set<MeteoElement> elements = aMeteoReader.getPreparedElements();

            // write timestamp block header
            fw.print(SIFReader.TIMESTAMPCROSSTABLE);
            for (MeteoElement element : elements) {
                fw.print(",");
                fw.print(SIFMeteoCodeConverter.getCodeFromMeteoElement(element, aMeteoReader.getNativeUnit(element)));
            }
            fw.println();

            final LocalDate firstDate = aMeteoReader.getPreparedFirstDate();
            final LocalDate lastDate  = aMeteoReader.getPreparedLastDate();

            aMeteoReader.prepare(firstDate, lastDate, elements);

            // loop over dates
            LocalDate curDate = firstDate;
            while (RangeUtils.inRange(curDate, firstDate, lastDate)) {

                // print timestamp code
                fw.print(SIFTimeStampConverter.codeYMDFromDate(curDate));

                // loop over elements
                for (MeteoElement element : elements) {
                    fw.print(",");
                    fw.print(String.format("%g", aMeteoReader.getValue(curDate, element, aMeteoReader.getNativeUnit(element))));
                }

                fw.println();

                curDate = curDate.plusDays(1);
            }

            fw.println();

        } finally {
            fw.flush();
        }
    }
}
