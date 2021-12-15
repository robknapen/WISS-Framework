/*
 * Copyright 2017 Wageningen Environmental Research
 *
 * For licensing information read the included LICENSE.txt file.
 *
 * Unless required by applicable law or agreed to in writing, this software
 * is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied.
 */
package nl.wur.wiss_framework.meteoutils;

import nl.wur.wiss_framework.core.ScientificUnit;
import nl.wur.wiss_framework.core.ScientificUnitConversion;
import nl.wur.wiss_framework.mathutils.RangeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * In-memory implementation of a MeteoReader.
 *
 * This class serves as a container for MeteoData items, and can handle the
 * data storage for other implementors of the MeteoReader interface. I.e. by
 * deferring to an associated instance.
 *
 * An InMemoryMeteoReader, after data is stored in it, can be passed in a
 * ParXChange object to provide meteo data for a simulation.
 *
 * @author Rob Knapen
 */
public class InMemoryMeteoReader implements MeteoReader, Iterable<LocalDate>, Serializable {

    public static final String CLASSNAME_ST = InMemoryMeteoReader.class.getSimpleName();
    public        final String CLASSNAME_IN = this.getClass().getSimpleName();

    private static final long serialVersionUID = 1L;

    private static final Log LOGGER = LogFactory.getLog(InMemoryMeteoReader.class);

    // create an unmodifiable set of all meteo elements the reader can provide
    public static final Set<MeteoElement> DEFAULT_SOURCE_ELEMENTS = Collections.unmodifiableSet(
            Stream.of(
                    MeteoElement.TM_MN, MeteoElement.TM_MX, MeteoElement.TM_AV,
                    MeteoElement.VP_AV,
                    MeteoElement.WS10_AV, MeteoElement.WS2_AV,
                    MeteoElement.PR_CU,
                    MeteoElement.Q_CU,
                    MeteoElement.E0, MeteoElement.ES0, MeteoElement.ET0
            ).collect(Collectors.toSet())
    );

    // inner class to store meteo data
    public static class MeteoData implements Serializable {

        private static final long serialVersionUID = 1L;

        public LocalDate day;           // date
        public double temperatureMax;   // degrees C
        public double temperatureMin;   // degrees C
        public double temperatureAvg;   // degrees C
        public double vapourPressure;   // HPA
        public double windSpeed10M;     // m/s at 10 m
        public double windSpeed2M;      // m/s at 2 m
        public double precipitation;    // mm
        public double e0;               // mm
        public double es0;              // mm
        public double et0;              // mm
        public double radiation;        // MJM2

        @Override
        public String toString() {
            return "MeteoData{" +
                    "day=" + day +
                    ", temperatureMax=" + temperatureMax +
                    ", temperatureMin=" + temperatureMin +
                    ", temperatureAvg=" + temperatureAvg +
                    ", vapourPressure=" + vapourPressure +
                    ", windSpeed10M=" + windSpeed10M +
                    ", windSpeed2M=" + windSpeed2M +
                    ", precipitation=" + precipitation +
                    ", e0=" + e0 +
                    ", es0=" + es0 +
                    ", et0=" + et0 +
                    ", radiation=" + radiation +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MeteoData meteoData = (MeteoData) o;

            return day.equals(meteoData.day);
        }

        @Override
        public int hashCode() {
            return day.hashCode();
        }
    }

    private Double longitude = Double.NaN;
    private Double latitude = Double.NaN;
    private Double altitude = Double.NaN;
    private Set<MeteoElement> sourceElements = DEFAULT_SOURCE_ELEMENTS;

    private final TreeMap<LocalDate, MeteoData> data = new TreeMap<>(); // use a TreeMap to guarantee sort on keys

    public InMemoryMeteoReader() {
        data.clear();
    }

    public long size() {
        return data.size();
    }

    public void clear() { data.clear(); }

    public MeteoData get(LocalDate key) {
        return data.get(key);
    }

    public Collection<MeteoData> getAll() {
        return data.values();
    }

    public MeteoData put(LocalDate date, MeteoData meteoData) {
        return data.put(date, meteoData);
    }

    public boolean hasDataForRange(LocalDate fromDate, LocalDate toDate) {
        if (data.size() != 0) {
            if ((data.firstKey().isEqual(fromDate) || data.firstKey().isBefore(fromDate)) &&
                    (data.lastKey().isEqual(toDate) || data.lastKey().isAfter(toDate))) {
                return true;
            }
        }
        return false;
    }

    public void setSourceElements(Set<MeteoElement> sourceElements) {
        this.sourceElements = sourceElements;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public void setAltitude(Double altitude) {
        this.altitude = altitude;
    }

    public void setLocation(Double longitude, Double latitude, Double altitude) {
        this.longitude = longitude;
        this.latitude = latitude;
        this.altitude = altitude;
    }

    public void clearLocation() {
        this.longitude = Double.NaN;
        this.latitude = Double.NaN;
        this.altitude = Double.NaN;
    }

    @Override
    public Iterator<LocalDate> iterator() {
        return data.keySet().iterator();
    }

    @Override
    public double getLongitudeDD() {
        return (this.longitude != null) ? this.longitude.doubleValue() : Double.NaN;
    }

    @Override
    public double getLatitudeDD() {
        return (this.latitude != null) ? this.latitude.doubleValue() : Double.NaN;
    }

    @Override
    public double getAltitudeM() {
        return (this.altitude != null) ? this.altitude.doubleValue() : Double.NaN;
    }

    @Override
    public LocalDate getSourceFirstDate() {
        return data.firstKey();
    }

    @Override
    public LocalDate getSourceLastDate() {
        return data.lastKey();
    }

    @Override
    public LocalDate getPreparedFirstDate() {
        return data.firstKey();
    }

    @Override
    public LocalDate getPreparedLastDate() {
        return data.lastKey();
    }

    @Override
    public Set<MeteoElement> getSourceElements() {
        return sourceElements;
    }

    @Override
    public ScientificUnit getNativeUnit(MeteoElement meteoElement) {

        final String methodName = "getNativeUnit";

        switch(meteoElement) {
            case TM_MN:   { return ScientificUnit.CELSIUS; }
            case TM_MX:   { return ScientificUnit.CELSIUS; }
            case TM_AV:   { return ScientificUnit.CELSIUS; }
            case PR_CU:   { return ScientificUnit.MM_D1; }
            case VP_AV:   { return ScientificUnit.HPA; }
            case WS10_AV: { return ScientificUnit.M_S; }
            case WS2_AV:  { return ScientificUnit.M_S; }
            case Q_CU:    { return ScientificUnit.MJ_M2D1;}
            case E0:      { return ScientificUnit.MM_D1; }
            case ES0:     { return ScientificUnit.MM_D1; }
            case ET0:     { return ScientificUnit.MM_D1; }
            default:
                throw new IllegalArgumentException(String.format("%s.%s : Unknown MeteoElement %s", CLASSNAME_IN, methodName, meteoElement.toString()));
        }
    }

    @Override
    public void prepare(LocalDate aStartDate, LocalDate aEndDate, Set<MeteoElement> aElements) {
        // void - In Memory data should already been loaded
    }

    @Override
    public Set<MeteoElement> getPreparedElements() {
        return sourceElements;
    }

    @Override
    public double getValue(LocalDate date, MeteoElement meteoElement, ScientificUnit unit) {

        final String methodName = "getValue";

        if (!RangeUtils.inRange(date, getPreparedFirstDate(), getPreparedLastDate())) {
            throw new IllegalArgumentException(String.format("%s.%s : Illegal date (%s). Must be between prepared dates %s and %s.",
                    CLASSNAME_IN, methodName, date.toString(), getPreparedFirstDate().toString(), getPreparedLastDate().toString()));
        }

        MeteoData md = data.get(date);

        if (md == null) {
            throw new IllegalArgumentException(String.format("%s.%s : Meteo element %s is not available on date=%s.",
                    CLASSNAME_IN, methodName, meteoElement.toString(), date.toString()));
        } else {
            String name = meteoElement.toString();
            Double value;
            ScientificUnit su;

            switch(meteoElement) {
                case TM_MN:   { value = md.temperatureMin; su = ScientificUnit.CELSIUS; break; }
                case TM_MX:   { value = md.temperatureMax; su = ScientificUnit.CELSIUS; break; }
                case TM_AV:   { value = md.temperatureAvg; su = ScientificUnit.CELSIUS; break; }
                case PR_CU:   { value = md.precipitation; su = ScientificUnit.MM_D1; break; }
                case VP_AV:   { value = md.vapourPressure; su = ScientificUnit.HPA; break; }
                case WS10_AV: { value = md.windSpeed10M; su = ScientificUnit.M_S; break; }
                case WS2_AV:  { value = md.windSpeed2M; su = ScientificUnit.M_S; break; }
                case Q_CU:    { value = md.radiation; su = ScientificUnit.MJ_M2D1; break; }
                case E0:      { value = md.e0; su = ScientificUnit.MM_D1; break; }
                case ES0:     { value = md.es0; su = ScientificUnit.MM_D1; break; }
                case ET0:     { value = md.et0; su = ScientificUnit.MM_D1; break; }
                default:
                    throw new IllegalArgumentException(String.format("%s.%s : Unknown MeteoElement %s", CLASSNAME_IN, methodName, meteoElement.toString()));
            }

            if (!value.isNaN()) {
                return ScientificUnitConversion.convert(name, (double) value, su, unit);
            } else {
                throw new IllegalArgumentException(String.format("%s.%s : Meteo element %s is not " +
                        "available on date=%s.", CLASSNAME_IN, methodName, meteoElement.toString(), date.toString()));
            }
        }
    }
}
