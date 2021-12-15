/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.wur.wiss_framework.core;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;

/**
 *
 * @author kraal001
 */
public abstract class SimController {

    public static final String CLASSNAME_ST = SimController.class.getSimpleName();
    public        final String CLASSNAME_IN = this.getClass().getSimpleName();

    // class logger
    private static final Log LOGGER = LogFactory.getLog(SimController.class);

    protected final Model      model;
    protected final ParXChange parXChange;
    protected final SimXChange simXChange;

    /**
     *
     * @param aDirector to do
     * @param aParXChange to do
     * @param aSimXChange to do
     */
    public SimController(Model   aDirector,
                         ParXChange aParXChange,
                         SimXChange aSimXChange) {

        final String methodName = "SimController";

        if (aDirector == null) {
            throw new IllegalArgumentException(String.format("%s.%s : aDirector cannot be null.",
                                                              CLASSNAME_IN,methodName));
        }

        if (aParXChange == null) {
            throw new IllegalArgumentException(String.format("%s.%s : aParXChange cannot be null.",
                                                              CLASSNAME_IN, methodName));
        }

        if (aSimXChange == null) {
            throw new IllegalArgumentException(String.format("%s.%s : aSimXChange cannot be null.",
                                                              CLASSNAME_IN, methodName));
        }

        model   = aDirector;
        parXChange = aParXChange;
        simXChange = aSimXChange;
    }

    /**
     *
     * @param aSimID the SimID to work on
     * @return the terminated SimObject
     */
    protected SimObject terminateAndGetSimObjectBySimID(String aSimID) {

        return this.terminateAndGetSimObjectBySimID(aSimID, false, "");
    }

    /**
     *
     * @param aSimID the SimID to work on
     * @param aError obsolete? to do
     * @param aErrorMsg obsolete? to do
     * @return to do
     */
    protected SimObject terminateAndGetSimObjectBySimID(String aSimID, boolean aError, String aErrorMsg) {

        final String methodName = "terminateAndGetSimObjectBySimID";

        if (StringUtils.isBlank(aSimID)) {
            throw new IllegalArgumentException(String.format("%s.%s : The simID is empty.",
                                                              CLASSNAME_IN, methodName));
        }

        // get the object, is not guaranteed to exist, terminate it,
        // and return it so it will be removed from the list
        SimObject simObject = model.getSimObjectBySimID(aSimID, false);
        if (simObject != null) {
            LOGGER.debug(String.format("Terminating %s with simID=%s (by %s).",
                                       simObject.CLASSNAME_IN, simObject.getSimID(), CLASSNAME_IN));
            simObject.terminate();
        }

        return simObject;
    }

    /**
     * Tests for SimObjects that need to be started. Newly started SimObjects
     * are added to the list. Is called at every time step.
     *
     * @param simObjectsRunning the list of running SimObjects.
     */
    public abstract void testForSimObjectsToStart(ArrayList<SimObject> simObjectsRunning);

    /**
     * Return list of simObjects that need to be stopped.
     *
     * @return the required list
     */
    public abstract ArrayList<SimObject> testForSimObjectsToTerminate();
/*
        example code to stop a simobject
        final ArrayList<SimObject> result = new ArrayList<>();

        // finish publisher of PREDSTATE when the state exceeds 15 items per ha
        final int tPRED = simXChange.getTokenReadByVarName(ModelPreyPred1.PREDSTATE, false);
        if (simXChange.isValidToken(tPRED)) {
            // there is a SimObject publishing a valid PRED, so there is a predator, so test for termination
            final double PRED = simXChange.getValueBySimIDVarname(tPRED, ScientificUnit.CNT_HA);

            if (PRED > 15.0) {
                final String    simID     = simXChange.getSimIDFromToken(tPRED);
                final SimObject simObject = this.terminateAndGetSimObjectBySimID(simID, false, "threshold reached");
                if (simObject != null) {
                    result.add(simObject);
                }
            }
        }
        return result;
*/

}
