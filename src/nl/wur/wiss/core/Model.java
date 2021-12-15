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
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

import java.util.ArrayList;

/**
 * The base class that handles simulation objects. SimObjects can be added,
 * terminated, and removed from the simulation. A custom simulation can be
 * created by inheriting from this class and providing own implementations..
 *
 * @author Daniel van Kraalingen (daniel.vankraalingen@wur.nl)
 * @author Rob Knapen (rob.knapen@wur.nl)
 * @version 1
 */
public abstract class Model {

    public static final String CLASSNAME_ST = Model.class.getSimpleName();
    public        final String CLASSNAME_IN = this.getClass().getSimpleName();

    // class logger
    private static final Log LOGGER = LogFactory.getLog(Model.class);

    public enum State { INITIALISING,
                        INTERVENING,
                        AUXCALCULATING,
                        RATECALCULATING,
                        TERMINATING };

    private State state = State.INITIALISING;

    // input and output data for the simulation
    protected final ParXChange parXChange;
    protected final SimXChange simXChange;

    // the list of instantiated controllers (all run from start to end)
    protected final ArrayList<SimController> simControllersRunning = new ArrayList<>();
    // the list of running SimObjects
    protected final ArrayList<SimObject>     simObjectsRunning     = new ArrayList<>();

    // count of the simObjects that were started (number can only increase !)
    protected int simObjectsStartedCnt = 0;

    protected String title;
    protected String description;
    protected int    majorVersion = Integer.MIN_VALUE;
    protected int    minorVersion = Integer.MIN_VALUE;

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

    public State getState() {
        return state;
    }

    /**
     * Initialises logging with the standard WISS core settings and
     * the specified root logging level.
     *
     * The default logging configuration is loaded from the file:
     *      nl/alterra/wiss/core/res/log4j2.xml
     *
     * @param rootLevel at which logging should take place
     */
    public void initLogging(Level rootLevel) {
        this.initLogging("nl/alterra/wiss/core/res/log4j2.xml", rootLevel);
    }

    /**
     * Initialises logging with the configuration loaded from the specified
     * location and the given root logging level.
     *
     * @param configLocation location to load Log4j2 configuration from
     * @param rootLevel at which logging should take place
     */
    public void initLogging(String configLocation, Level rootLevel) {
        Configurator.initialize("logger", configLocation);
        Configurator.setRootLevel(rootLevel);
    }

    public ParXChange getParXChange() {
        return parXChange;
    }

    public SimXChange getSimXChange() {
        return simXChange;
    }

    /**
     * Constructs object.
     *
     * @param aParXChange the ParXChange object to use
     * @param aSimXChange the SimXChange object to use
     */
    public Model(ParXChange aParXChange,
                 SimXChange aSimXChange) {

        final String methodName = "Model";

        if (aParXChange == null) {
            throw new IllegalArgumentException(String.format("%s.%s : aParXChange cannot be null.",
                                                              CLASSNAME_IN, methodName));
        }

        if (aSimXChange == null) {
            throw new IllegalArgumentException(String.format("%s.%s : aSimXChange cannot be null.",
                                                              CLASSNAME_IN, methodName));
        }

        parXChange = aParXChange;
        simXChange = aSimXChange;
    }

    /**
    * See which SimObjects have to be started, returns the number started.
    *
    * @return the required value
    */
    // todo : moet hier geen state gezet worden ?
    public int testForSimObjectsToStart() {

        int result = 0;

        // do .testForSimObjectsToStart on all controllers in simControllersRunning
        // returns list of started SimObjects for each controller
        for (SimController simctrl : simControllersRunning) {

            final int cnt_before = simObjectsRunning.size();

            simctrl.testForSimObjectsToStart(simObjectsRunning);

            final int cnt_after = simObjectsRunning.size();
            final int cnt       = cnt_after - cnt_before;

            if (cnt > 0) {
                simObjectsStartedCnt += cnt;
                result               += cnt;
            }
        }

        return result;
    }

    /**
     * Terminates SimObjects that have to be terminated (through all running
     * controllers).
     *
     */
    public void testForSimObjectsToTerminate() {

        // run .testForSimObjectsToTerminate method on all controllers in simControllersRunning array
        // returns list of terminated SimObjects for each controller, need only to be removed from the list
        for (SimController simctrl : simControllersRunning) {

            ArrayList<SimObject> simObjectsNewlyTerminated = simctrl.testForSimObjectsToTerminate();

            if (simObjectsNewlyTerminated != null) {
                for (SimObject simObject : simObjectsNewlyTerminated) {

                    if (simObject.getState() != SimObject.State.TERMINATED) {
                        throw new AssertionError(String.format("%s.testForSimObjectsToTerminate : simObject with simID=%s is not in %s state.",
                                                                CLASSNAME_IN, simObject.getSimID(), SimObject.State.TERMINATED));
                    }
                    simObjectsRunning.removeAll(simObjectsNewlyTerminated);
                }
            }
        }

        ArrayList<SimObject> simObjectsNewlyTerminated = new ArrayList<>();

        for (SimObject simObject : simObjectsRunning) {

            if (!simObject.canContinue()) {
                LOGGER.debug(String.format("Terminating %s with simID=%s (by their own request).",
                                           simObject.CLASSNAME_IN, simObject.getSimID()));
                simObject.terminate();
                simObjectsNewlyTerminated.add(simObject);
            }
        }

        if (simObjectsNewlyTerminated.size() >= 1) {
            simObjectsRunning.removeAll(simObjectsNewlyTerminated);
        }
    }

    /**
     * Flags whether SimObjects have run in the past, but nog anymore.
     *
     * @return the required value
     */
    public boolean testForTerminateByModel() {
        // stop simulation if some simObject was running in the past, but all running simObjects have stopped
        return ((simObjectsStartedCnt >= 1) && (simObjectsRunning.isEmpty()));
    }

    /**
     * Terminate all SimObjects running.
     */
    public void simObjectsTerminate() {
        state = State.TERMINATING;

        // terminate from last to first
        for (int i = simObjectsRunning.size() - 1; i >= 0; i--) {
            LOGGER.debug(String.format("Terminating %s with simID=%s.",
                                       simObjectsRunning.get(i).CLASSNAME_IN, simObjectsRunning.get(i).getSimID()));
            simObjectsRunning.get(i).terminate();
        }
    }

    /**
     * Runs aModelAction on all running SimObjects.
     *
     * @param aModelAction the action to carry out
     */
    public void doModelAction(ModelAction aModelAction) {

        final String methodName = "doModelAction";

        switch (aModelAction) {
            case INTERVENE:
                if ((state != State.INITIALISING) && (state != State.RATECALCULATING)) {
                    throw new IllegalStateException(String.format("%s.%s : Internal error, method intervene wanted on state %s.",
                                                                   CLASSNAME_IN, methodName, this.state));
                }
                state = State.INTERVENING;

                break;
            case AUXCALCULATIONS:
                if ((state != State.INITIALISING) && (state != State.INTERVENING) && (state != State.AUXCALCULATING)) {
                    throw new IllegalStateException(String.format("%s.%s : Internal error, method auxCalculations wanted on state %s.",
                                                                   CLASSNAME_IN, methodName, this.state));
                }
                state = State.AUXCALCULATING;

                break;
            case RATECALCULATIONS:
                if (state != State.AUXCALCULATING) {
                    throw new IllegalStateException(String.format("%s.%s : Internal error, method rateCalculations wanted on state %s.",
                                                                   CLASSNAME_IN, methodName, this.state));
                }
                state = State.RATECALCULATING;

                break;
            default:
                throw new AssertionError(String.format("%s.%s : invalid model action %d.",
                                                        CLASSNAME_IN, methodName, aModelAction));
        }

            // do .doModelAction on all simObjects in simObjectList
        for (SimObject simObject : simObjectsRunning) {
            simObject.doModelAction(aModelAction);
        }
}

    /**
     * Get the simObject for the simID from the list of running simObjects,
     *
     * @param aSimID the SimID to search for
     * @param aMustFind whether an exception must be raised when aSimID is not found
     * @return the required SimObject
     */
    public SimObject getSimObjectBySimID(String aSimID, boolean aMustFind) {

        final String methodName = "getSimObjectBySimID";

        if (StringUtils.isBlank(aSimID)) {
            throw new IllegalArgumentException(String.format("%s.%s : The simID is empty.",
                                                              CLASSNAME_IN, methodName));
        }

        for (SimObject simObject : simObjectsRunning) {
            if (simObject.simID.equalsIgnoreCase(aSimID)) {
                return simObject;
            }
        }

        if (aMustFind) {
            throw new IllegalArgumentException(String.format("%s.%s : Cannot find simID=%s",
                                                              CLASSNAME_IN, methodName, aSimID));
        }

        return null;
    }

    /**
     * Return the array index of simObjectsRunning where simID=aSimID.
     *
     * @param aSimID the SimID to look for
     * @param aMustFind whether an exception must be raised when aSimID is not found
     * @return the required array index
     */
    public int GetSimObjectsRunningIndexBySimID(String aSimID, boolean aMustFind) {

        final String methodName = "GetSimObjectsRunningIndexBySimID";

        if (StringUtils.isBlank(aSimID)) {
            throw new IllegalArgumentException(String.format("%s.%s : The simID is empty.",
                                                              CLASSNAME_IN, methodName));
        }

        for (int i = 0; i < simObjectsRunning.size(); i++) {
            if (simObjectsRunning.get(i).simID.equalsIgnoreCase(aSimID)) {
                return i;
            }
        }

        if (aMustFind) {
            throw new IllegalArgumentException(String.format("%s.%s : Cannot find simID=%s",
                                                              CLASSNAME_IN, methodName, aSimID));
        }

        return -1;
    }
}
