/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.wur.wiss.meteoutils;

import java.time.LocalDate;
import java.util.Set;
import nl.wur.wiss.core.ScientificUnit;

/**
 * Interface meant to be used by a SimObject for reading meteo data
 *
 * @author kraal001
 */
public interface MeteoReader {
    /** get longitude in decimal degrees of source data
     *
     * @return the required value
     */
    double            getLongitudeDD();
    /** get latitude in decimal degrees of source data
     *
     * @return the required value
     */
    double            getLatitudeDD();
    /** get altitude in meters of source data
     *
     * @return the required value
     */
    double            getAltitudeM();
    /** get first date of source data
     *
     * @return the required value
     */
    LocalDate         getSourceFirstDate();
    /** get last date of source data
     *
     * @return the required value
     */
    LocalDate         getSourceLastDate();
    /** get first date of prepared data
     *
     * @return the required value
     */
    LocalDate         getPreparedFirstDate();
    /** get last date of prepared data
     *
     * @return the required value
     */
    LocalDate         getPreparedLastDate();
    /** returns set of available MeteoElements in source
     *
     * @return the required value
     */
    Set<MeteoElement> getSourceElements();
    /** get the native unit of the meteo element
     *
     * @param aMeteoElement the meteo element for the native unit
     * @return the required value
     */
    ScientificUnit    getNativeUnit(MeteoElement aMeteoElement);
    /** prepare for the required period and set of elements
     * @param aStartDate the first date for which meteo data must be prepared
     * @param aEndDate the last date for which meteo data must be prepared
     * @param aElements list of elements which need to be ready for delivery
    */
    void              prepare(LocalDate aStartDate, LocalDate aEndDate, Set<MeteoElement> aElements);
    /** returns set of MeteoElements in prepared data
     *
     * @return the required value
     */
    Set<MeteoElement> getPreparedElements();
    /** returns value for element, returns NaN if not available, aDate must be in prepared period, aMeteoElement in prepared elements
     *
     * @param aDate the date of the required value
     * @param aMeteoElement the meteo element of the required value
     * @param aUnit the required unit of the value
     * @return the required value
     */
    double            getValue(LocalDate aDate, MeteoElement aMeteoElement, ScientificUnit aUnit);
}
