/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.wur.wiss.fileutils;

import java.time.LocalDate;

/**
 *
 * @author kraal001
 */
public class SIFTimeStampConverter {

    public static final String CLASSNAME_ST = SIFTimeStampConverter.class.getSimpleName();
    public        final String CLASSNAME_IN = this.getClass().getSimpleName();

    public static final String TIMESTAMPCODENODATA = "N";
    public static final String TIMESTAMPCODEYEAR   = "Y";
    public static final String TIMESTAMPCODEMONTH  = "M";
    public static final String TIMESTAMPCODEWEEK   = "W";
    public static final String TIMESTAMPCODEDEKAD  = "K";
    public static final String TIMESTAMPCODEDAY    = "D";

    private static final LocalDate TIMESTAMPMISSING = LocalDate.of(1900, 1, 1);

    /**
     *
     * @param aUcTimeStampCode to do
     * @param aFirstDay to do
     *
     * @return the date as derived from the input arguments
     */
    public static LocalDate dateFromCode(String aUcTimeStampCode, boolean aFirstDay) {

        final String methodName = "dateFromCode";

        int codeLength = aUcTimeStampCode.length();

        LocalDate result = TIMESTAMPMISSING;

        int iTmp1;
        int iTmp2;
        int iTmp3;

        if ((codeLength !=  1) &&
            (codeLength !=  5) &&
            (codeLength !=  8) &&
            (codeLength !=  9) &&
            (codeLength != 11)) {
            throw new IllegalArgumentException(String.format("%s.%s : Illegal TimeStampCode (%s).",
                                                              CLASSNAME_ST,
                                                              methodName,
                                                              aUcTimeStampCode));
        }

        if (aUcTimeStampCode.substring(0, 1).equals(TIMESTAMPCODENODATA)) {
            if (codeLength != 1) {
                throw new IllegalArgumentException(String.format("%s.%s : Illegal TimeStampCode (%s)",
                                                                 CLASSNAME_ST,
                                                                 methodName,
                                                                 aUcTimeStampCode));
            }

            result = LocalDate.of(1900, 1, 1);
        } else if (aUcTimeStampCode.substring(0, 1).equals(TIMESTAMPCODEYEAR)) {
            try {
              iTmp1 = Integer.parseInt(aUcTimeStampCode.substring(1, 5));
            } catch (Exception e) {
                throw new IllegalArgumentException(String.format("%s.%s : Illegal TimeStampCode (%s).",
                                                                 CLASSNAME_ST,
                                                                 methodName,
                                                                 aUcTimeStampCode));
            }

            // see if M, W or K are following
            if (codeLength > 5) {
                // something is following Yxxxx
                if (aUcTimeStampCode.substring(5, 6).equals(TIMESTAMPCODEMONTH)) {
                    // string must be at least YxxxxMxx, but maybe longer
                    try {
                        iTmp2 = Integer.parseInt(aUcTimeStampCode.substring(6, 8));
                    } catch (Exception e) {
                        throw new IllegalArgumentException(String.format("%s.%s : Illegal TimeStampCode (%s).",
                                                                          CLASSNAME_ST,
                                                                          methodName,
                                                                          aUcTimeStampCode));
                    }

                    // see if YxxxxMxx is followed by something
                    if (codeLength > 8) {
                        // something is following YxxxxMxx
                        if (aUcTimeStampCode.substring(8, 9).equals(TIMESTAMPCODEDAY)) {
                            // string is YxxxxMxxDxx

                            if (codeLength != 11) {
                                throw new IllegalArgumentException(String.format("%s.%s : Illegal TimeStampCode (%s).",
                                                                                 CLASSNAME_ST,
                                                                                 methodName,
                                                                                 aUcTimeStampCode));
                            }

                            try {
                                iTmp3 = Integer.parseInt(aUcTimeStampCode.substring(9, 11));
                            } catch (Exception e) {
                                throw new IllegalArgumentException(String.format("%s.%s : Illegal TimeStampCode (%s).",
                                                                                 CLASSNAME_ST,
                                                                                 methodName,
                                                                                 aUcTimeStampCode));
                            }

                            try {
                                result = LocalDate.of(iTmp1, iTmp2, iTmp3);
                            } catch (Exception e) {
                                throw new IllegalArgumentException(String.format("%s.%s : Illegal TimeStampCode (%s).",
                                                                                  CLASSNAME_ST,
                                                                                  methodName,
                                                                                  aUcTimeStampCode));
                            }
                        } else if (aUcTimeStampCode.substring(9, 10).equals(TIMESTAMPCODEDEKAD)) {
                            // string is YxxxxMxxKxx

                            // todo : not yet implemented, see TimeStampConverter in Delphi

                        } else {
                            throw new IllegalArgumentException(String.format("%s.%s : Illegal TimeStampCode (%s).",
                                                                             CLASSNAME_ST,
                                                                             methodName,
                                                                             aUcTimeStampCode));
                        }
                    } else {
                        // nothing is following after YxxxxMxx

                        // todo : not yet implemented, see TimeStampConverter in Delphi
                    }
                } else if (aUcTimeStampCode.substring(5, 6).equals(TIMESTAMPCODEWEEK)) {
                    // todo : not yet implemented, see TimeStampConverter in Delphi
                } else if (aUcTimeStampCode.substring(5, 6).equals(TIMESTAMPCODEDEKAD)) {
                    // todo : not yet implemented, see TimeStampConverter in Delphi
                } else if (aUcTimeStampCode.substring(5, 6).equals(TIMESTAMPCODEDAY)) {
                    // string must be YxxxxDxxx, and cannot be longer
                    if (codeLength != 9) {
                        throw new IllegalArgumentException(String.format("%s.%s : Illegal TimeStampCode (%s).",
                                                                         CLASSNAME_ST,
                                                                         methodName,
                                                                         aUcTimeStampCode));
                    }

                    try {
                        iTmp2 = Integer.parseInt(aUcTimeStampCode.substring(6, 9));
                    } catch (Exception e) {
                        throw new IllegalArgumentException(String.format("%s.%s : Illegal TimeStampCode (%s).",
                                                                         CLASSNAME_ST,
                                                                         methodName,
                                                                         aUcTimeStampCode));
                    }

                    try {
                        result = LocalDate.ofYearDay(iTmp1, iTmp2);
                    } catch (Exception e) {
                        throw new IllegalArgumentException(String.format("%s.%s : Illegal TimeStampCode (%s).",
                                                                          CLASSNAME_ST,
                                                                          methodName,
                                                                          aUcTimeStampCode));
                    }
                } else {
                    throw new IllegalArgumentException(String.format("%s.%s : Illegal TimeStampCode (%s).",
                                                                      CLASSNAME_ST,
                                                                      methodName,
                                                                      aUcTimeStampCode));
                }
            } else {
                // nothing following the Yxxxx anymore, encode

                // todo : not yet implemented, see TimeStampConverter in Delphi
            }
        }

        return result;
    }

    /**
     *
     * @param aDate the date to be used for the date to string conversion
     * @return the date in {@literal <}yyyy{@literal >}{@literal <}mm{@literal >}{@literal <}dd{@literal >} format
     */
    public static String codeYMDFromDate(LocalDate aDate) {

        final String result = String.format("Y%04dM%02dD%02d", aDate.getYear(), aDate.getMonthValue(), aDate.getDayOfMonth());

        return result;
    }
}
