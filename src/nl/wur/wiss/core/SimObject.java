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

import nl.wur.wiss_framework.core.TimeDriver.ModelAction;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Base class for simulation classes
 *
 * @author Daniel van Kraalingen (daniel.vankraalingen@wur.nl)
 * @version 1
 */
public abstract class SimObject {

    public static final String CLASSNAME_ST = SimObject.class.getSimpleName();
    public        final String CLASSNAME_IN = this.getClass().getSimpleName();

    // class logger
    private static final Log LOGGER = LogFactory.getLog(SimObject.class);

    // to enable internal checking of method calling order, not meant to be published
    // to the outside world
    public enum State { INITIALISING,
                        INTERVENING,
                        AUXCALCULATING,
                        RATECALCULATING,
                        TERMINATING,
                        TERMINATED};

    private LocalDate initialDate;

    protected final String     simID;
    protected final ParXChange parXChange;
    protected final SimXChange simXChange;

    private String title;
    private String description;
    private int    majorVersion = Integer.MIN_VALUE;
    private int    minorVersion = Integer.MIN_VALUE;

    private boolean errorOnTerminate;
    private String  errorOnTerminateMsg;

    private State state = State.INITIALISING;

    private boolean traceLogging = false;

    public void setTraceLogging(boolean traceLogging) {
        this.traceLogging = traceLogging;
    }

    public int getMajorVersion() {
        if (majorVersion == Integer.MIN_VALUE) {
            throw new IllegalStateException("majorVersion not set");
        }
        return majorVersion;
    }

    public int getMinorVersion() {
        if (minorVersion == Integer.MIN_VALUE) {
            throw new IllegalStateException("minorVersion not set");
        }
        return minorVersion;
    }

    public String getTitle() {
        if (StringUtils.isBlank(title)) {
            throw new IllegalStateException("Title not set");
        }
        return title;
    }

    public String getDescription() {
        if (StringUtils.isBlank(description)) {
            throw new IllegalStateException("Description not set");
        }
        return description;
    }

    public boolean isVersion(int aMajorVersion, int aMinorVersion) {

        return (aMajorVersion == majorVersion) && (aMinorVersion == minorVersion);
    }

    public boolean isSameOrNewerVersion(int aMajorVersionMinimal, int aMinorVersionMinimal) {

        boolean result = (aMajorVersionMinimal < majorVersion);
        if (!result) {
            result = (aMajorVersionMinimal == majorVersion) && (aMinorVersionMinimal <= minorVersion);
        }
        return result;
    }

    public void checkMinimalVersion(int aMajorVersionMinimal, int aMinorVersionMinimal) {
      if (!this.isSameOrNewerVersion(aMajorVersionMinimal, aMinorVersionMinimal)) {
          throw new IllegalStateException(String.format("SimObject %s with version %d.%d cannot be instantiated, minimum version required is %d.%d",
                                                        CLASSNAME_IN, majorVersion, minorVersion, aMajorVersionMinimal, aMinorVersionMinimal));
      }
    }

    /**
     * Constructs object
     *
     * @param aSimID identifier this object should use in reporting etc.
     * @param aParXChange object containing parameters
     * @param aSimXChange object containing dynamic data
     * @param aMajorVersion to do
     * @param aMinorVersion to do
     * @param aTitle to do
     * @param aDescription to do
     */
    public SimObject(String     aSimID,
                     ParXChange aParXChange,
                     SimXChange aSimXChange,
                     int        aMajorVersion,
                     int        aMinorVersion,
                     String     aTitle,
                     String     aDescription) {

        final String methodName = "SimObject";

        if (StringUtils.isBlank(aSimID)) {
            throw new IllegalArgumentException(String.format("%s.%s : SimID is empty.",
                                                              CLASSNAME_IN, methodName));
        }

        if (aParXChange == null) {
            throw new IllegalArgumentException(String.format("%s.%s : aParXChange cannot be null.",
                                                              CLASSNAME_IN, methodName));
        }

        if (aSimXChange == null) {
            throw new IllegalArgumentException(String.format("%s.%s : aSimXChange cannot be null.",
                                                              CLASSNAME_IN, methodName));
        }

        if (aMajorVersion < 0) {
            throw new IllegalArgumentException(String.format("%s.%s : MajorVersion (%d) is invalid.",
                                                              CLASSNAME_IN, methodName, aMajorVersion));
        }

        if (aMinorVersion < 0) {
            throw new IllegalArgumentException(String.format("%s.%s : MinorVersion (%d) is invalid.",
                                                              CLASSNAME_IN, methodName, aMinorVersion));
        }

        if (StringUtils.isBlank(aTitle)) {
            throw new IllegalArgumentException(String.format("%s.%s : Title is empty.",
                                                              CLASSNAME_IN, methodName));
        }

        if (StringUtils.isBlank(aDescription)) {
            throw new IllegalArgumentException(String.format("%s.%s : Description is empty.",
                                                              CLASSNAME_IN, methodName));
        }

        simID        = aSimID;

        parXChange   = aParXChange;
        simXChange   = aSimXChange;

        majorVersion = aMajorVersion;
        minorVersion = aMinorVersion;
        title        = aTitle;
        description  = aDescription;

        initialDate = simXChange.getCurDate();

        errorOnTerminate    = false;
        errorOnTerminateMsg = "";

        simXChange.registerSimID(simID, CLASSNAME_IN);

        if (parXChange.contains(TimeDriver.TRACELOGGING, Boolean.class)) {
          traceLogging = parXChange.get(TimeDriver.TRACELOGGING, CLASSNAME_IN, Boolean.class);
        }
    }

    @Override
    public String toString() {
        return "SimObject{" + "owner=" + simID + '}';
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + Objects.hashCode(this.simID);
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
        final SimObject other = (SimObject) obj;
        if (!Objects.equals(this.simID, other.simID)) {
            return false;
        }
        return true;
    }

    /**
     *
     * @return simulation identifier
     */
    public final String getSimID() {
        return simID;
    }

    /**
     *
     * @return state of the SimObject instance
     */
    public final State getState() {
        return state;
    }

    /**
     * @return number of days that has elapsed since THIS object was started,
     * can be different from the elapsed property of the whole simulation
     */
    public int elapsed() {
        return DateUtils.diffDays(initialDate, simXChange.getCurDate());
    }

    /**
     * @return whether this object is in initialisation phase
     */
    public boolean isInitializing() {
      return state == State.INITIALISING;
    }

    /**
     * @return whether this object is in intervening calculation phase
     */
    public boolean isIntervening() {
      return state == State.INTERVENING;
    }

    /**
     * @return whether this object is in auxiliary calculation phase
     */
    public boolean isAuxCalculating() {
      return state == State.AUXCALCULATING;
    }

    /**
     * @return whether this object is in rate calculation phase
     */
    public boolean isRateCalculating() {
      return state == State.RATECALCULATING;
    }

    /**
     * for doing intervening in own states, not to be called directory but through doModelAction
     */
    public void intervene() {

        final String methodName = "intervene";

        if (traceLogging) {
            LOGGER.trace(String.format("%s.%s", CLASSNAME_IN, methodName, this));
        }

        if (state != State.RATECALCULATING) {
            throw new IllegalStateException(String.format("%s.%s : Internal error, method called on invalid state of SimObject %s.",
                                                           CLASSNAME_IN, methodName, this));
        }
        state = State.INTERVENING;
    }

    /**
     * for doing auxiliary calculations, not to be called directory but through doModelAction
     */
    public void auxCalculations() {

        final String methodName = "auxCalculations";

        if (traceLogging) {
            LOGGER.trace(String.format("%s.%s", CLASSNAME_IN, methodName, this));
        }

        if ((state != State.INITIALISING) && (state != State.INTERVENING) && (state != State.AUXCALCULATING)) {
            throw new IllegalStateException(String.format("%s.%s : Internal error, method called on invalid state of SimObject %s.",
                                                           CLASSNAME_IN, methodName, this));
        }
        state = State.AUXCALCULATING;
    }

    /**
     * for doing rate calculations, not to be called directory but through doModelAction
     */
    public void rateCalculations() {

        final String methodName = "rateCalculations";

        if (traceLogging) {
            LOGGER.trace(String.format("%s.%s", CLASSNAME_IN, methodName, this));
        }

        if (state != State.AUXCALCULATING) {
            throw new IllegalStateException(String.format("%s.%s : Internal error, method called on invalid state of SimObject %s.",
                                                           CLASSNAME_IN, methodName, this));
        }
        state = State.RATECALCULATING;
    }

    public void doModelAction(ModelAction aModelAction) {

        final String methodName = "doModelAction";

        switch (aModelAction) {
            case INTERVENE:
                this.intervene();

                break;
            case AUXCALCULATIONS:
                this.auxCalculations();

                break;
            case RATECALCULATIONS:
                this.rateCalculations();

                break;
            default:
                throw new AssertionError(String.format("%s.%s : invalid calculation mode %s.",
                                                        CLASSNAME_IN, methodName, aModelAction));
        }
    }

    /**
     * Can be called on any state to find out whether this object can go to
     * the next time step, if not, false must be returned
     *
     * @return whether the simObject can continue
     */
    public boolean canContinue() {

        final String methodName = "canContinue";

        if (traceLogging) {
            LOGGER.trace(String.format("%s.%s", CLASSNAME_IN, methodName, this));
        }
        return true;
    }

    /**
     * this object must terminate
     */
    public void terminate() {

        final String methodName = "terminate";

        if (traceLogging) {
            LOGGER.trace(String.format("%s.%s", CLASSNAME_IN, methodName, this));
        }

        // no check on state
        state = State.TERMINATING;

        simXChange.terminateSimID(simID, errorOnTerminate, errorOnTerminateMsg);

        state = State.TERMINATED;
    }
}
