/*
 * Copyright 1988, 2013, 2017 Wageningen Environmental Research
 *
 * For licensing information read the included LICENSE.txt file.
 *
 * Unless required by applicable law or agreed to in writing, this software
 * is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied.
 */
package nl.wur.wiss.core;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

/**
 * Collection of static simulation parameters.
 *
 * @author Daniel van Kraalingen
 * @author Rob Knapen
 */
public class ParXChange implements Iterable<ParXChange.VarInfo>, Serializable {

    public static final String CLASSNAME_ST = ParXChange.class.getSimpleName();
    public        final String CLASSNAME_IN = this.getClass().getSimpleName();

    private static final long serialVersionUID = 1L;

    static final int INITIALVARCNT = 100;

    /**
     * Information about a stored variable. For each variable the owner, its
     * name, and the class type are considered to be the unique key. The
     * scientific units, mutable and deleted state of the variable is considered
     * extra information, and not included in the equals and hash-code methods
     * for this class.
     */
    public static class VarInfo implements Serializable {

        private static final long serialVersionUID = 1L;

        // primary key
        public String ucVarName;
        public Class<?> cls;

        // var info
        public ScientificUnit scientificUnit;
        public boolean markedFinal;
        public boolean markedDeleted;


        public <T> VarInfo(String varName, Class<T> cls) {
            this.ucVarName = varName.toUpperCase(Locale.US);
            this.cls = cls;
            this.scientificUnit = ScientificUnit.NA;
            this.markedFinal = false;
            this.markedDeleted = false;
        }


        @Override
        public String toString() {
            return "VarInfo{" + ", ucVarName=" + ucVarName + ", cls=" + cls + '}';
        }


        @Override
        public int hashCode() {
            int hash = 7;
            hash = 97 * hash + Objects.hashCode(this.ucVarName);
            hash = 97 * hash + Objects.hashCode(this.cls);
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
            final VarInfo other = (VarInfo) obj;
            if (!Objects.equals(this.ucVarName, other.ucVarName)) {
                return false;
            }
            if (!Objects.equals(this.cls, other.cls)) {
                return false;
            }
            return true;
        }
    }


    // single map to store all types of variables
    private final HashMap<VarInfo, Object> data = new HashMap<>(INITIALVARCNT);

    // constructor
    public ParXChange() {
        // void
    }


    /**
     * Creates an iterator over the variable info (VarInfo) objects that
     * serve as keys in the map storing all variables and values.
     * @return to do
     */
    @Override
    public Iterator<ParXChange.VarInfo> iterator() {
        return data.keySet().iterator();
    }


    /**
     * Retrieves the value stored in the map for the variable specified by
     * the VarInfo key, or null if no mapping exists.
     *
     * @param key of variable to retrieve the value for
     * @return value stored for the variable, or null
     */
    public Object get(VarInfo key) {
        return data.get(key);
    }


    /**
     * Returns the size of the map storing the variable keys and values.
     *
     * @return size of the map
     */
    public int size() {
        return data.size();
    }


    /**
     * Checks if a variable with the specified owner, variable name, and data
     * type is available; excludes variables marked as deleted.
     *
     * When checking for a variable of type Double, contains will also return
     * true if a similar variable of type Integer is available. This matches
     * the behavior of the getDouble method and the get method that uses the
     * ScientificUnit argument.
     *
     * @param <T> Type of the requested variable
     * @param varName name of the requested variable
     * @param cls Class type of the requested variable
     * @return true if the requested variable is available, false otherwise
     */
    public <T> boolean contains(String varName, Class<T> cls) {
        return contains(varName, cls, false);
    }


    /**
     * Checks if a variable with the specified owner, variable name, and data
     * type is available. Variables marked as deleted will be included or
     * excluded based on the includeDeleted parameter.
     *
     * When checking for a variable of type Double, contains will also return
     * true if a similar variable of type Integer is available. This matches
     * the behavior of the getDouble method and the get method that uses the
     * ScientificUnit argument.
     *
     * @param <T> Type of the requested variable
     * @param varName name of the requested variable
     * @param cls Class type of the requested variable
     * @param includeDeleted include or exclude variables marked as deleted
     * @return true if the requested variable is available, false otherwise
     */
    public <T> boolean contains(String varName, Class<T> cls, boolean includeDeleted) {
        // fallback mechanisme for Double class, accept Integer types as well
        VarInfo vi = new VarInfo(varName, cls);
        if ((cls == Double.class) && (!data.keySet().contains(vi))) {
            vi = new VarInfo(varName, Integer.class);
        }

        // find parameter and verify its deleted state
        final VarInfo searchInfo = vi;
        if (data.keySet().contains(searchInfo)) {
            if (data.keySet().stream().anyMatch((info) -> (info.equals(searchInfo) && (includeDeleted || !info.markedDeleted)))) {
                return true;
            }
        }

        return false;
    }


    /**
     * Gets the value of a variable stored in the ParXChange instance. Owner,
     * VarName and Class of the variable are considered to be the primary
     * key that uniquely identifies the variable. Duplicates are not allowed.
     *
     * An assertion error will be thrown when trying to get a
     * variable that does not exists, or that has been marked deleted.
     *
     * When retrieving numeric values (subclasses of Number) you should use
     * the get() method that allows to specify a ScientificUnit as argument, so
     * that proper unit conversion can take place. Using this get() without
     * ScientificUnit parameter for Number (sub)classes will thrown an assertion
     * error.
     *
     * @param <T> Type of the requested variable
     * @param varName name of the requested variable
     * @param aCaller identifies whom is requesting, used in error messages
     * @param cls Class type of the requested variable
     * @return value of the requested variable, if it exists
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String varName, String aCaller, Class<T> cls) {

        final String methodName = "get";

        // see if we have the requested variable
        VarInfo varInfo = getInfo(varName, cls);
        if (varInfo == null) {
            throw new IllegalArgumentException(String.format("%s.%s : Could not locate %s (caller=%s).",
                                                              CLASSNAME_IN, methodName, varName, aCaller));
        }

        if (varInfo.markedDeleted) {
            throw new IllegalArgumentException(String.format("%s.%s : Attempt to retrieve variable %s that is marked as deleted (caller=%s).",
                                                              CLASSNAME_IN, methodName, varName, aCaller));
        }

        // get the raw value
        Object value = data.get(varInfo);

        // require the use of scientific units for numeric data
        if (value instanceof Number) {
            throw new IllegalArgumentException(String.format("%s.%s : To retrieve numeric data for variable %s use the get method with a ScientificUnit parameter! (caller=%s).",
                                                              CLASSNAME_IN, methodName, varName, aCaller));
        }

        // return it
        return (T) value;
    }


    /**
     * Gets the value of a variable stored in the ParXChange instance, converted
     * (if needed and possible) to the specified target scientific units.
     * Owner, VarName and Class of the variable are considered to be the primary
     * key that uniquely identifies the variable. Duplicates are not allowed.
     *
     * An assertion error will be thrown when trying to get a
     * variable that does not exists, or that has been marked deleted.
     *
     * When requesting a variable of type Double, the method will defer to
     * the method getDouble. This method will first look for a matching Double
     * type variable, but when not found search for a matching Integer type
     * variable. If that exists it will returned as a Double.
     *
     * Unit conversion is currently only support for Integer and Double data
     * types.
     *
     * @param <T> Type of the requested variable
     * @param varName name of the requested variable
     * @param aCaller identifies whom is requesting, used in error messages
     * @param cls Class type of the requested variable
     * @param targetUnit scientific unit to convert the value to
     * @return value of the requested variable, if it exists
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String varName, String aCaller, Class<T> cls, ScientificUnit targetUnit) {

        final String methodName = "get";

        // treat Doubles (probably the most common datatype used) with a fallback mechanism
        if (cls == Double.class) {
            return (T) getDouble(varName, aCaller, targetUnit);
        }

        // handle all other (numeric) datatypes
        VarInfo varInfo = getInfo(varName, cls);
        if (varInfo == null) {
            throw new IllegalArgumentException(String.format("%s.%s : Could not locate %s (caller=%s).",
                                                              CLASSNAME_IN, methodName, varName, aCaller));
        }

        if (varInfo.markedDeleted) {
            throw new IllegalArgumentException(String.format("%s.%s : Attempt to retrieve variable %s that is marked as deleted (caller=%s).",
                                                              CLASSNAME_IN, methodName, varName, aCaller));
        }

        // get the raw value
        Object value = data.get(varInfo);

        if (value instanceof Number) {
            try {
                return (T) ScientificUnitConversion.convert(varName, (Number) value, varInfo.scientificUnit, targetUnit);
            } catch (Exception e) {
                throw new IllegalArgumentException(String.format("%s.%s : Unit conversion error for variable %s, %s (caller=%s).",
                        CLASSNAME_IN, methodName, varName, e.getMessage(), aCaller));
            }
        }

        // for all other types simply return the value
        return (T) value;
    }


    /**
     * Gets the value of a variable stored in the ParXChange instance, converted
     * (if needed and possible) to the specified target scientific units.
     *
     * This method will specifically search for a variable of type Double, and
     * if not available look for one of type Integer. When found the value will
     * be returned as a Double.
     *
     * Owner, VarName and Class (Double or Integer in this case) of the variable
     * are considered to be the primary key that uniquely identifies the
     * variable. Duplicates are not allowed.
     *
     * An assertion error will be thrown when trying to get a
     * variable that does not exists, or that has been marked deleted.
     *
     * @param varName name of the requested variable
     * @param aCaller identifies whom is requesting, used in error messages
     * @param targetUnit scientific unit to convert the value to
     * @return value of the requested variable as Double, if it exists
     */
    @SuppressWarnings("unchecked")
    public Double getDouble(String varName, String aCaller, ScientificUnit targetUnit) {

        final String methodName = "getDouble";

        // try to find a Double first, Integer second
        VarInfo varInfo = getInfo(varName, Double.class);
        if (varInfo == null) {
            varInfo = getInfo(varName, Integer.class);
        }

        if (varInfo == null) {
            throw new IllegalArgumentException(String.format("%s.%s : Could not locate %s (caller=%s), neither as double nor integer.",
                                                              CLASSNAME_IN, methodName, varName, aCaller));
        }

        if (varInfo.markedDeleted) {
            throw new IllegalArgumentException(String.format("%s.%s : Attempt to retrieve variable %s that is marked as deleted (caller=%s).",
                                                              CLASSNAME_IN, methodName, varName, aCaller));
        }

        // get the raw value, we know it is a Number
        Number value = (Number)data.get(varInfo);

        try {
            value = ScientificUnitConversion.convert(varName, value, varInfo.scientificUnit, targetUnit);
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("%s.%s : Unit conversion error for variable %s, %s (caller=%s).",
                    CLASSNAME_IN, methodName, varName, e.getMessage(), aCaller));
        }

        // always return the value as a double
        return value.doubleValue();
    }


    /**
     * Gets the meta-data for the specified variable, if it exists.
     *
     * @param <T> Type of the requested variable
     * @param varName name of the requested variable
     * @param cls Class type of the requested variable
     * @return VarInfo for the requested variable, or null
     */
    public <T> VarInfo getInfo(String varName, Class<T> cls) {
        VarInfo searchInfo = new VarInfo(varName, cls);
        if (data.keySet().contains(searchInfo)) {
            for (VarInfo info : data.keySet()) {
                if (info.equals(searchInfo)) {
                    return info;
                }
            }
        }
        return null;
    }


    @SuppressWarnings("unchecked")
    public <T> void set(ParValue<T> value) {
        set(value.n, (Class<T>)value.v().getClass(), value.v(), value.u);
    }

    /**
     * Sets the value for a variable in the ParXChange instance.
     *
     * New variables will be added as mutable. Existing variables will be
     * updated, unless they have been marked as final (immutable). When the
     * variable exists but is marked deleted the delete mark will be removed
     * and the value updated (even when the variable is also marked final).
     *
     * An attempt to set a final (not marked deleted) variable will raise an
     * IllegalArgumentException. Empty or null owner and variable names will
     * also cause an IllegalArgumentException.
     *
     * When storing numeric values (subclasses of Number) you should use the
     * set() method that allows to specify a ScientificUnit as argument, so
     * that proper unit conversion can take place. Using this set() without
     * ScientificUnit parameter for Number (sub)classes will thrown an assertion
     * error.
     *
     * @param <T> Type of the requested variable
     * @param varName name of the requested variable
     * @param cls Class type of the requested variable
     * @param value to store for the variable
     */
    public <T> void set(String varName, Class<T> cls, T value) {
        // require the use of scientific units for numeric data

        final String methodName = "set";

        if (value instanceof Number) {
            throw new IllegalArgumentException(String.format("%s.%s : To store numeric data for variable %s use the set method with a ScientificUnit parameter!",
                                                              CLASSNAME_IN, methodName, varName));
        }
        set(varName, cls, false, value, ScientificUnit.NA);
    }

    /**
     * Sets the value for a variable in the ParXChange instance.
     *
     * New variables will be added as mutable. Existing variables will be
     * updated, unless they have been marked as final (immutable). When the
     * variable exists but is marked deleted the delete mark will be removed
     * and the value updated (even when the variable is also marked final).
     *
     * An attempt to set a final (not marked deleted) variable will raise an
     * IllegalArgumentException. Empty or null owner and variable names will
     * also cause an IllegalArgumentException.
     *
     * @param <T> Type of the requested variable
     * @param varName name of the requested variable
     * @param cls Class type of the requested variable
     * @param value to store for the variable
     * @param unit ScientificUnit of the value
     */
    public <T> void set(String varName, Class<T> cls, T value, ScientificUnit unit) {
        set(varName, cls, false, value, unit);
    }


    /**
     * Sets the value for a variable in the ParXChange instance.
     *
     * New variables can be added as mutable or immutable. Existing variables
     * will be updated, unless they have been marked as immutable. When the
     * variable exists but is marked deleted the delete mark will be removed
     * and the value updated (even when the variable is also marked final).
     *
     * An attempt to set a final (not marked deleted) variable will raise an
     * IllegalArgumentException. Empty or null owner and variable names will
     * also cause an IllegalArgumentException.
     *
     * @param <T> Type of the requested variable
     * @param varName name of the requested variable
     * @param cls Class type of the requested variable
     * @param isImmutable if changing the value is not allowed
     * @param value to store for the variable
     * @param unit ScientificUnit of the value
     */
    public <T> void set(String varName, Class<T> cls, boolean isImmutable, T value, ScientificUnit unit) {
        checkArguments(varName);

        final String methodName = "set";

        VarInfo varInfo = getInfo(varName, cls);
        if (varInfo != null) {
            // re-activate the variable
            if (varInfo.markedDeleted) {
                varInfo.markedDeleted = false;
            } else {
                // existing data will be updated, check if it is marked immutable
                if (varInfo.markedFinal) {
                    throw new IllegalArgumentException(String.format("%s.%s : %s cannot be set more than once.",
                                                                      CLASSNAME_IN, methodName, varInfo));
                }
            }
        } else {
            // store new data
            varInfo = new VarInfo(varName, cls);
        }

        varInfo.scientificUnit = unit;
        varInfo.markedFinal = isImmutable;
        data.put(varInfo, value);
    }


    /**
     * Marks a variable as deleted in the ParXChange instance. Variables marked
     * as deleted can no longer be retrieved with get(). They can be set() and
     * then the delete mark will be removed.
     *
     * An attempt to delete a variable that is already marked deleted will
     * raise an IllegalArgumentException. Empty or null owner and variable
     * names will also cause an IllegalArgumentException.
     *
     * @param <T> Type of the requested variable
     * @param varName name of the requested variable
     * @param cls Class type of the requested variable
     */
    public <T> void delete(String varName, Class<T> cls) {
        checkArguments(varName);

        final String methodName = "delete";

        VarInfo varInfo = getInfo(varName, cls);
        if (varInfo == null) {
            throw new IllegalArgumentException(String.format("%s.%s : Could not locate %s.",
                                                              CLASSNAME_IN, methodName, varName));
        } else {
            if (varInfo.markedDeleted) {
                throw new IllegalArgumentException(String.format("%s.%s : Attempt to delete variable %s  that is alread marked deleted.",
                                                                  CLASSNAME_IN, methodName, varName));
            }
            varInfo.markedDeleted = true;
        }
    }

    private void checkArguments(String varName) {

        final String methodName = "checkArguments";

        if (StringUtils.isBlank(varName)) {
            throw new IllegalArgumentException(String.format("%s.%s : A variable is empty.",
                                                              CLASSNAME_IN, methodName));
        }
    }
}
