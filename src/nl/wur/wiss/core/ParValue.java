/*
 * Copyright 1988, 2013, 2016 Wageningen Environmental Research
 *
 * For licensing information read the included LICENSE.txt file.
 *
 * Unless required by applicable law or agreed to in writing, this software
 * is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied.
 */
package nl.wur.wiss_framework.core;

import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;


/**
 * Generic Parameter Value class.
 *
 * E.g. new ParValue{@literal <}Double{@literal >}, new ParValue{@literal <}Integer{@literal >}, etc.
 *
 * @param <T> class for the value of the parameter
 *
 * @author Rob Knapen (rob.knapen@wur.nl)
 */
public class ParValue<T> implements Serializable {

    public static final String CLASSNAME_ST = ParValue.class.getSimpleName();
    public        final String CLASSNAME_IN = this.getClass().getSimpleName();

    private static final long serialVersionUID = 1L;

    /** value itself */
    private T                    v;
    /** the unit, can be set only once */
    public  final ScientificUnit u;
    /** the name, can be set only once */
    public  final String         n;

    // internal fields used for case insensitive equals and hashcode
    private final String ucName;

    /**
     * Creates an instance of the class, with the specified values.
     *
     * @param varValue value of the parameter
     * @param varUnit scientific unit of the parameter
     * @param varName name of the parameter
     */
    public ParValue(T varValue, ScientificUnit varUnit, String varName) {
        super();

        final String methodName = "ParValue";

        if ((varName == null) || (varName.length() == 0)) {
            throw new IllegalArgumentException(String.format("%s.%s : The variable name is empty.",
                                                              CLASSNAME_IN, methodName));
        }

        if (varValue instanceof Number) {
            if (varValue instanceof Double) {
                if (Double.isNaN((Double)varValue)) {
                    throw new IllegalArgumentException(
                        String.format("%s.%s : The double value is empty.",
                                       CLASSNAME_IN, methodName));
                }
            }
        }

        v = varValue;
        u = varUnit;
        n = varName;

        ucName  = n.toUpperCase(Locale.US);
    }

    /**
     * Shorthand method that defers to getV() to retrieve the value of the
     * parameter.
     *
     * @return value of the parameter
     */
    public T v() {
        return getV();
    }

    /**
     * Return the value of the parameter.
     *
     * @return value of the parameter
     */
    public T getV() {
        return v;
    }

    /**
     * Sets a new value for the parameter. It will be checked if the new
     * value is within the range of the Scientific Unit defined for the
     * parameter. If not an IllegalArgumentException will be raised.
     *
     * @param v new value to set
     */
    public void setV(T v) {

        final String methodName = "setV";

        if (v instanceof Number) {
            if (v instanceof Double) {
                if (Double.isNaN((Double)v)) {
                    throw new IllegalArgumentException(String.format("%s.%s : The double value is empty.",
                                                                     CLASSNAME_IN, methodName));
                }
            }
        }

        this.v = v;
    }

    @Override
    public String toString() {
        return String.format("%s{%s}", CLASSNAME_IN, this.getCaption());
    }

    /**
     * @return a caption for the object
     */
    public String getCaption() {
        return "v=" + v + ", u=" + u + ", n=" + n;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 23 * hash + Objects.hashCode(ucName);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ParValue<?> other = (ParValue<?>) obj;
        return Objects.equals(this.ucName, other.ucName);
    }

}
