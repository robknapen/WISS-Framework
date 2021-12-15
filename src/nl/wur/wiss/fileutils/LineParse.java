/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.wur.wiss_framework.fileutils;

import nl.wur.wiss_framework.mathutils.RangeUtils;

import java.util.ArrayList;

/**
 * Parses a string for words separated by separators and enclosed in quote characters.
 * Both can be set, separators can be more than one, but the quote character can be
 * only one character.
 *
 * @author Daniel van Kraalingen (daniel.vankraalingen@wur.nl)
 * @version 1
 */
public class LineParse {

    public static final String CLASSNAME_ST = LineParse.class.getSimpleName();
    public        final String CLASSNAME_IN = this.getClass().getSimpleName();

    public static enum CharType {
        PLAIN,     // character that is not a separator and not a quotechar
        SEPARATOR, // character is a separator
        QUOTECHAR  // character is a quotechar
    }

    private static final String DEFAULTSEPARATOR = ",";
    private static final String DEFAULTQUOTECHAR = "\"";
    private static final int    EMPTYSTRINGINDEX = -1;

    private String separators = DEFAULTSEPARATOR;

    private String quoteChar  = DEFAULTQUOTECHAR;

    private String line = ""; // the string to parse
    private final ArrayList<Integer> beginList = new ArrayList<>();
    private final ArrayList<Integer> endList   = new ArrayList<>();
    private boolean separateRepetitive = false;

    private CharType findCharType(String aChar) {
        if (separators.contains(aChar)) {
            return CharType.SEPARATOR;
        } else if (quoteChar.contains(aChar)) {
            return CharType.QUOTECHAR;
        } else {
            return CharType.PLAIN;
        }
    }

    // parsing can work on more than one separator char e.g. ',;'
    // separation then works on ',', and ';'
    /**
     * @return a String of the defined separators
     */
    public String getSeparators() {
        return separators;
    }

    /**
     * Sets the string of value separators to use
     *
     * @param aSeparators the string of value separators
     */
    public void setSeparators(String aSeparators) {
        this.separators = aSeparators;
    }

    /**
     * Sets whether repetitive separators are ignored or are separators of
     * empty items, when false (default), 'x,,,x' is seen as 2 words : x and x,
     * when true, it is seen as 4 words, x, {@literal <}empty{@literal >}, {@literal <}empty{@literal >} and x
     *
     * @param aSeparateRepetitive to do
     */
    public void setSeparateRepetitive(boolean aSeparateRepetitive) {
        this.separateRepetitive = aSeparateRepetitive;
    }

    /**
     * @return the separateRepetitive flag
     */
    public boolean isSeparateRepetitive() {
        return separateRepetitive;
    }

    /**
     * @return the quote char (can only be one character)
     */
    public String getQuoteChar() {
        return quoteChar;
    }

    /**
     * Sets the quote char (can only be one char)
     * @param aQuoteChar the quote char to use
     */
    public void setQuoteChar(String aQuoteChar) {

        final String methodName = "setQuoteChar";

        int len = aQuoteChar.length();

        if (len >= 2) {
            throw new IllegalArgumentException(String.format("%s.%s : Illegal length for quoteChar (%s)(must have length 0 or 1).",
                                                             CLASSNAME_IN, methodName, aQuoteChar));
        }

        // make sure quotechar is not used as separator
        if (len != 0) {
            if (this.findCharType(aQuoteChar) == CharType.SEPARATOR) {
                throw new IllegalArgumentException(String.format("%s.%s : QuoteChar (%s) cannot be a separator.",
                                                                 CLASSNAME_IN, methodName, aQuoteChar));
            }
        }

        this.quoteChar = aQuoteChar;
    }

    /**
     * @return the line which has been parsed
     */
    public String getLine() {
        return line;
    }

    /**
     * @return the number of items after parsing
     */
    public int count() {
        return beginList.size();
    }

    /**
     * returns the parsed words
     *
     * @param aIndex the required position
     * @return the parsed word at the required position
     */
    public String getWord(int aIndex) {

        final String methodName = "getWord";

        if (!RangeUtils.inRange(aIndex, 0, beginList.size() - 1)) {
            throw new IllegalArgumentException(String.format("%s.%s : Index (%d) not within allowed bounds (0, %d).",
                                                             CLASSNAME_IN, methodName, aIndex, (beginList.size() - 1)));
        }

        String result;
        if ((beginList.get(aIndex) != EMPTYSTRINGINDEX) && (endList.get(aIndex) != EMPTYSTRINGINDEX)) {
            result = line.substring(beginList.get(aIndex), endList.get(aIndex) + 1);
        } else {
            result = "";
        }

        return result;
    }

    /**
     * sets the line to be parsed, parsing is automatic on calling this method
     *
     * @param aLine the line to be parsed
     */
    public void setLine(String aLine) {

        final String methodName = "setLine";

        line = aLine;
        int lineLength = line.length();

        beginList.clear();
        endList.clear();

        CharType oldCharType = CharType.SEPARATOR;
        CharType newCharType = CharType.SEPARATOR;

        boolean withinQuotedSection = false;
        boolean keepOldCharType     = false;

        String sChar;
        for (int i = 0; i <= (lineLength - 1); i++) {
            sChar = line.substring(i, (i + 1));

            if (!keepOldCharType) {
                oldCharType = newCharType;
            }
            newCharType = this.findCharType(sChar);

            if (withinQuotedSection) {
                if (oldCharType == CharType.QUOTECHAR) {
                    // if oldchar is quotechar, then pointer is at the first character in a quotechar section
                    if (newCharType == CharType.QUOTECHAR) {
                        // first character is itself a quotechar, so is empty quotechar section !!!
                        beginList.add(EMPTYSTRINGINDEX);
                        endList.add(EMPTYSTRINGINDEX);
                        withinQuotedSection = false;
                    } else {
                        beginList.add(i);
                    }
                } else if (newCharType == CharType.QUOTECHAR) {
                    // the last character in a quotechar section (previous ones were normal), so end of word
                    endList.add((i - 1));
                    withinQuotedSection = false;
                }
            } else {
                // if quotechar is not preceded by normal char (not a separator),
                // consider it to be part of the current word
                if ((newCharType == CharType.QUOTECHAR) && (oldCharType == CharType.PLAIN)) {
                    newCharType = CharType.PLAIN;
                }

                switch (oldCharType) {
                    case PLAIN:
                        if (newCharType == CharType.SEPARATOR) {
                            // separator after normal character, is end of word
                            endList.add((i-1));
                        }
                        break;
                    case SEPARATOR:

                        switch (newCharType) {
                            case PLAIN:
                                // normal character after a separator, start of word
                                beginList.add(i);
                                break;
                            case QUOTECHAR:
                                // quote character after a separator, start of quoted section
                                withinQuotedSection = true;
                                break;
                            case SEPARATOR:
                                if (separateRepetitive) {
                                    // empty string
                                    beginList.add(EMPTYSTRINGINDEX);
                                    endList.add(EMPTYSTRINGINDEX);
                                }
                                break;
                            default:
                                throw new AssertionError(String.format("%s.%s : Invalid CharType.",
                                                                       CLASSNAME_IN, methodName));
                        }
                        break;
                    case QUOTECHAR:
                        // if normal char follows closing quote char, skip all normal chars until something else
                        keepOldCharType = (newCharType == CharType.PLAIN);
                        break;
                    default:
                        throw new AssertionError(String.format("%s.%s : Invalid CharType.",
                                                                CLASSNAME_IN, methodName));
                }
            }
        }

        // process end of the string
        if (withinQuotedSection) {
            if (newCharType != CharType.QUOTECHAR) {
                // there is at least one character in the quote char section, terminate word
                // at the last char of the line
                endList.add(lineLength - 1);
            } else {
                // empty string
                beginList.add(EMPTYSTRINGINDEX);
                endList.add(EMPTYSTRINGINDEX);
            }
        } else {
            if (!keepOldCharType) {
                // not in skip mode
                switch (newCharType) {
                    case PLAIN:
                        // string ended with a normal character
                        endList.add(lineLength - 1);
                        break;
                    case SEPARATOR:
                        if (separateRepetitive) {
                            // empty string
                            beginList.add(EMPTYSTRINGINDEX);
                            endList.add(EMPTYSTRINGINDEX);
                        }
                        break;
                }
            }
        }
        if (beginList.size() != endList.size()) {
            throw new AssertionError(String.format("%s.%s : Word arrays of unequal length on line=>%s<.",
                                                   CLASSNAME_IN, methodName, line));
        }
    }
}
