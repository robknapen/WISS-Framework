/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.wur.wiss_framework.fileutils;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 *
 * @author Daniel van Kraalingen
 */
public class TextFileAsArrayList {

    public static final String CLASSNAME_ST = TextFileAsArrayList.class.getSimpleName();
    public        final String CLASSNAME_IN = this.getClass().getSimpleName();

    public static ArrayList<String> get(String aFilePath, boolean aTrim) throws IOException {

        final String methodName = "get";

        if (StringUtils.isBlank(aFilePath)) {
            throw new IllegalArgumentException(String.format("%s.%s : File path is empty.",
                                                              CLASSNAME_ST, methodName));
        }

        ArrayList<String> result = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(aFilePath))) {
            br.lines().forEach(line -> result.add(aTrim ? line.trim() : line));
        }

        return result;
    }
}
