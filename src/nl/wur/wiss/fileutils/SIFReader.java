package nl.wur.wiss_framework.fileutils;

import nl.wur.wiss_framework.mathutils.RangeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;


/**
 * Example code for a SIF file reader.
 *
 * @author Daniel van Kraalingen (daniel.vankraalingen@wur.nl)
 * @version 1
 */

// per sectie mag er maar een timeseries block voorkomen (wordt gechecked)
public class SIFReader {

    private static class Message {
        public String  message;
        public int     lineNo;
        public boolean isError;
    }

    public static class STACSection {
        public String                       objectType;
        public SIFReaderLoadModeType        loadMode;
        public HashMap<String, String>      nameValueList      = null;  // the list of name=value pairs that are in the section, name is uppercased !
        public HashMap<String, Integer>     firstColOfBlockIdx = null;  // the first col of a block, if it exists! can be used to quickly locate a date
        public HashMap<String, Integer>     firstRowOfBlockIdx = null;  // the first row of a block, if it exists! can be used to quickly locate an element
        public ArrayList<ArrayList<String>> tableData          = null;  // all ! cols of a block, if it exists, 2-dim string list, inner is cols, outer is rows, first row contains header names
    }

    public static final String COMMENTPREFIX = "//";
    public static final String SECTIONPREFIX = ":";
    public static final String SIFFILETABLESEPARS = " ,\t"; // separs for table : space, comma, tab char
    public static final String SIFFILEQUOTECHAR = "'"; // single quote

    public static final String TIMESTAMPCROSSTABLE = "TIMESTAMPCROSSTABLE";

    private ArrayList<STACSection> sections = new ArrayList<>();

    private ArrayList<String>  fileLines = new ArrayList<>();   // file as arraylist with all lines trimmed
    private String             filePath;

    // class logger
    private static final Log LOGGER = LogFactory.getLog(SIFReader.class);

    public SIFReader (String aFilePath) throws Exception {

        filePath = aFilePath;

        try {
            // create arraylist fileLines with spaces removed from either end
            fileLines = TextFileAsArrayList.get(aFilePath, true);
        } catch (IOException e) {
            String msg = String.format("Error opening file \"%s\" (systemmessage=%s, exception=%s)", aFilePath, e.getMessage(), e.getClass());
            LOGGER.error(msg);
            throw new IOException(msg, e);
        }

        try {

            this.convertToSections();

        } catch (Exception e) {
            String msg = String.format("%s (file=\"%s\")", e.getMessage(), aFilePath);
            LOGGER.error(msg);
            throw new Exception(msg, e);
        }

        LOGGER.info(String.format("Read SIF file \"%s\"", aFilePath));
    }

    private static boolean isStatement (String aLine) {
        boolean result = false;

        if (StringUtils.isNotBlank(aLine)) {
            // line contains some text chars
            if (!StringUtils.startsWith(aLine, COMMENTPREFIX)) {
                // line is not empty and does not start with //, so is a true data line
                result = true;
            }
        }

        return result;
    }

    private static boolean isSectionStart (String aLine) {

        return StringUtils.startsWith(aLine, SECTIONPREFIX);
    }

    private int addNewSection() {
        STACSection section = new STACSection();

        section.objectType         = "";
        section.loadMode           = SIFReaderLoadModeType.UNKNOWN;
        section.nameValueList      = new HashMap<>();
        section.firstColOfBlockIdx = new HashMap<>();
        section.firstRowOfBlockIdx = new HashMap<>();
        section.tableData          = null;

        sections.add(section);

        return (sections.size() - 1);
    }

    // results true when it is Name=SingleValue line
    private boolean processNameValue(String aLine, int aSectionsHigh) {
        boolean result = false;

        int i = aLine.indexOf('='); // zero based
        int l = aLine.length();

        // see if there is a = somewhere in the line (use 1 because if it is 0 then clearly
        // no word can come before the '=')(strings are 0 based !!)
        if ((i >= 1) && (i < (l - 1))) {
            // there is a '=' somewhere in the line, see if the text before it is a
            // single word, when space separ is used
            // create s as string before '=', trimmed, if there is no space in s
            // it is a single word
            String s = aLine.substring(0, i).trim();

            // if single word (when space separs are used) put it in the list. Syntactical
            // non conformities will be discovered later.
            if (!s.contains(" ")) {
                // no space found, it must be a single word
                String key = s;

                s = aLine.substring(i + 1).trim();

                // remove single quotes if they exist on both sides
                if ((s.startsWith(SIFFILEQUOTECHAR)) && s.endsWith(SIFFILEQUOTECHAR) && (s.length() >= 3)){
                    s = s.substring(1, (s.length() - 1));
                }

                String value = s;
                sections.get(aSectionsHigh).nameValueList.put(key.toUpperCase(), value);

                result = true;
            }
        }
        return result;
    }

    private String codeYMDFromDate(LocalDate aDate) {
        return String.format("Y%04dM%02dD%02d", aDate.getYear(), aDate.getMonthValue(), aDate.getDayOfMonth());
    }

    // converts fileLines to sections
    private void convertToSections() throws Exception {

        String   line;
        String   objectType;
        String   loadMode;

        int      sectionsHigh = -1;
        boolean  tableMode = false;

        LineParse lineParse = new LineParse();

        // default quote char for whole sif file
        lineParse.setQuoteChar(SIFFILEQUOTECHAR);

        for (int i = 0; i <= (fileLines.size() - 1); i++) {
            line = fileLines.get(i);

            if (SIFReader.isStatement(line)) {
                // see if it is a section
                if (SIFReader.isSectionStart(line)) {
                    // statement is a section start statement

                    // split on one or more spaces
                    lineParse.setSeparators(" ");
                    lineParse.setLine(line);

                    // since it is a section, it must have an object type and a loadmode
                    if (lineParse.count() != 2) {
                        throw new Exception(String.format("Error in file %s : Incorrect syntax in line : %s.",
                                                           filePath, line));
                    }

                    // the sectionprefix is still part of words[0], additional characters must exist
                    if (!(lineParse.getWord(0).length() >= 2)) {
                        throw new Exception(String.format("Error in file %s : Incorrect syntax in line : %s.",
                                                           filePath, line));
                    }

                    objectType = lineParse.getWord(0).substring(1).toUpperCase();
                    loadMode   = lineParse.getWord(1).toUpperCase();

                    tableMode = false;
                    sectionsHigh = this.addNewSection();

                    sections.get(sectionsHigh).objectType = objectType;
                    sections.get(sectionsHigh).loadMode   = SIFReaderLoadModeType.getSIFFileReaderLoadModeType(loadMode);
                } else {
                    // it is a statement, but not a section start statement

                    // only update sections etc when parsing is within a section
                    if (sectionsHigh >= 0) {
                        if (!tableMode) {
                            // see if the line is a name/value pair, if so add it to .nameValueList
                            boolean b = processNameValue(line, sectionsHigh);
                            if (!b) {
                                // line is not a Name=SingleValue pair, see if it is a table header

                                // split line on separs that are allowed in a table
                                lineParse.setSeparators(SIFFILETABLESEPARS);
                                lineParse.setLine(line);

                                // construct the lineWords arraylist
                                ArrayList<String> lineWords = new ArrayList<>();
                                for (int j = 0; j <= (lineParse.count() - 1); j++) {
                                    lineWords.add(lineParse.getWord(j));
                                }

                                if (lineWords.size() >= 2) {
                                    // line is a table header
                                    // check that it is the first block
                                    if (sections.get(sectionsHigh).tableData != null) {
                                        throw new Exception(String.format("Error in file %s : Section cannot have more than 1 block.",
                                                                           filePath));
                                    }

                                    tableMode = true;

                                    sections.get(sectionsHigh).tableData = new ArrayList<>();
                                    sections.get(sectionsHigh).tableData.add(lineWords);

                                    sections.get(sectionsHigh).firstRowOfBlockIdx = new HashMap<>();
                                    for (int j = 0; j <= (lineWords.size() - 1); j++) {
                                        sections.get(sectionsHigh).firstRowOfBlockIdx.put(lineWords.get(j).toUpperCase(), j);
                                    }

                                    sections.get(sectionsHigh).firstColOfBlockIdx = new HashMap<>();
                                    sections.get(sectionsHigh).firstColOfBlockIdx.put(lineWords.get(0).toUpperCase(), 0);
                                } else {
                                    throw new Exception(String.format("Error in file %s : Not the right number of names in line \"%s\".",
                                                                       filePath, line));
                                }
                            }
                        } else {
                            // parser is in table mode, store the values

                            // split line on separs that are allowed in a table
                            lineParse.setSeparators(SIFFILETABLESEPARS);
                            lineParse.setLine(line);

                            // construct the lineWords arraylist
                            ArrayList<String> lineWords = new ArrayList<>();
                            for (int j = 0; j <= (lineParse.count() - 1); j++) {
                                lineWords.add(lineParse.getWord(j));
                            }

                            if (lineWords.size() == sections.get(sectionsHigh).tableData.get(0).size()) {
                                sections.get(sectionsHigh).tableData.add(lineWords);
                                sections.get(sectionsHigh).firstColOfBlockIdx.put(lineWords.get(0), (sections.get(sectionsHigh).tableData.size() - 1));
                            } else {
                                throw new Exception(String.format("Error in file %s : Number of values changes in line \"%s\".",
                                                                   filePath, line));
                            }
                        }
                    }
                }
            } else {
                // switch off table mode on empty line etc.
                tableMode = false;
            }
        }
    }

    public int getSectionCount() {
        return sections.size();
    }

    public STACSection getSection(int aSectionIndex) {

        if (!RangeUtils.inRange(aSectionIndex, 0, (sections.size() - 1))) {
            throw new IllegalArgumentException(String.format("Error in file %s : Index (%d) not in range (%d, %d).",
                                                              filePath, aSectionIndex, 0, (sections.size() - 1)));
        }

        return sections.get(aSectionIndex);
    }

    // returns value, returns empty string if aMustExist = false and it does not exist
    public String getNameValue(String aName, int aSectionIndex, boolean aMustExist) throws Exception {
        STACSection section = this.getSection(aSectionIndex);

        String result = "";

        if (section.nameValueList.containsKey(aName)) {
            result = section.nameValueList.get(aName);
        } else {
            if (aMustExist) {
                throw new Exception(String.format("Error in file %s : Section %d does not have value for %s.",
                                                   filePath, aSectionIndex, aName));
            }
        }
        return result;
    }

    public ArrayList<String> getTimeSeriesElements(int aSectionIndex) {

        STACSection section = this.getSection(aSectionIndex);

        if (section.tableData == null) {
            throw new IllegalStateException(String.format("Error in file %s : Section %d does not have a time series block.",
                                                           filePath, aSectionIndex));
        }

        ArrayList<String> result = new ArrayList<>();

        // copy values one by one into result
        ArrayList<String> header = section.tableData.get(0);
        // skip first column (is for dates)
        for (int i = 1; i <= (header.size() - 1); i++) {
            result.add(header.get(i));
        }

        return result;
    }

    public double getValue(LocalDate aDate, int aSectionIndex, String aElement) {

        String sDate = this.codeYMDFromDate(aDate);

        STACSection section = this.getSection(aSectionIndex);

        int rowIndex = section.firstColOfBlockIdx.getOrDefault(sDate, -1);
        int colIndex = section.firstRowOfBlockIdx.getOrDefault(aElement.toUpperCase(), -1);

        double result = Double.NaN;
        if ((colIndex >= 0) && (rowIndex >= 0)) {
            result = Double.parseDouble(section.tableData.get(rowIndex).get(colIndex));
        }

        return result;
    }

    public LocalDate getFirstDate(int aSectionIndex) {

        STACSection section    = this.getSection(aSectionIndex);
        String      sFirstDate = section.tableData.get(1).get(0);
        LocalDate   firstDate  = SIFTimeStampConverter.dateFromCode(sFirstDate, true);

        return firstDate;
    }

    public LocalDate getLastDate(int aSectionIndex) {

        STACSection section   = this.getSection(aSectionIndex);
        String      sLastDate = section.tableData.get(section.tableData.size() - 1).get(0);
        LocalDate   lastDate  = SIFTimeStampConverter.dateFromCode(sLastDate, false);

        return lastDate;
    }
}
