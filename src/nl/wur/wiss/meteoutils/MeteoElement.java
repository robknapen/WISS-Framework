/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.wur.wiss_framework.meteoutils;

/**
 * Enumeration of meteo elements
 *
 * @author Daniel van Kraalingen (daniel.vankraalingen@wur.nl)
 * @version 1
 */

public enum MeteoElement {
    /** Longitude in decimal degrees of the meteo data */
    LONGITUDEDD,
    /** Latitude in decimal degrees of the meteo data */
    LATITUDEDD,
    /** Altitude in meters of the meteo data */
    ALTITUDEM,
    /** Average temperature over time period */
    TM_AV,
    /** Minimum temperature over time period */
    TM_MN,
    /** Maximum temperature over time period */
    TM_MX,
    /** Cumulated solar radiation over time period */
    Q_CU,
    /** Cumulated top of atmosphere solar radiation over time period */
    TOA_CU,
    /** Cumulated precipitation over time period */
    PR_CU,
    /** Average wind speed at 2 meters height over time period */
    WS2_AV,
    /** Average wind speed at 10 meters height over time period */
    WS10_AV,
    /** Average relative humidity */
    RH_AV,
    /** Average vapour pressure */
    VP_AV,
    /** Average dew point */
    TD_AV,
    /** Penman evaporation amount */
    E0,
    /** Soil evaporation amount */
    ES0,
    /** Evapo-transpiration amount */
    ET0;
}
