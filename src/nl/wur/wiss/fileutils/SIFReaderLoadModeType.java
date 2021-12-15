/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.wur.wiss_framework.fileutils;

/**
 *
 * @author Daniel van Kraalingen
 */
public enum SIFReaderLoadModeType {
    /** unknown loader type */
    UNKNOWN,
    /** merge the data with existing data */
    MERGE,
    /** replace the existing data with the new data*/
    REPLACE;

    public static SIFReaderLoadModeType getSIFFileReaderLoadModeType(String s) {

        String us = s.toUpperCase();

        if (MERGE.name().equals(us)) {
            return MERGE;
        } else if (REPLACE.name().equals(us)) {
            return REPLACE;
        }

        // execution should not arrive here
        throw new IllegalArgumentException(String.format("No Enum specified for this string (%s).",
                                                          s));
    }
}
