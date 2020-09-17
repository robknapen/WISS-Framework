/*
 * Copyright 1988, 2013, 2016 Wageningen Environmental Research
 *
 * For licensing information read the included LICENSE.txt file.
 *
 * Unless required by applicable law or agreed to in writing, this software
 * is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied.
 */
package nl.wur.wiss.core;

import java.time.LocalDate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Drives a Model object from start to finish, including initialisation,
 integration etc.
 *
 * @author Daniel van Kraalingen (daniel.vankraalingen@wur.nl)
 * @version 1
 */
public class TimeDriver {

    public static final String CLASSNAME_ST = TimeDriver.class.getSimpleName();
    public        final String CLASSNAME_IN = this.getClass().getSimpleName();

    public static enum State {
        NONEXISTENT,
        CONSTRUCTING,
        CONSTRUCTED,
        INITIALISING,
        INITIALISED,
        UPDATING,
        INTERVENING,
        TESTINGFORSTART,
        AUXCALCULATING,
        RATECALCULATING,
        TESTINGFORSIMOBJECTSTERMINATION,
        TERMINATING_BY_TIMEDRIVER,
        TERMINATING_BY_MODEL,
        TERMINATED_BY_TIMEDRIVER,
        TERMINATED_BY_MODEL
    }

    public static enum ModelAction {
      INTERVENE,
      AUXCALCULATIONS,
      RATECALCULATIONS,
      CANCONTINUE
    }

    public State state = State.NONEXISTENT;

    private final Timer    timer;
    private final Model model;

    // class logger
    private static final Log LOGGER = LogFactory.getLog(TimeDriver.class);

    public static final String STARTDATE = "STARTDATE";
    public static final String ENDDATE   = "ENDDATE";
    public static final String PAUSEDATE = "PAUSEDATE";

    public static final String TRACELOGGING = "TRACELOGGING";

    /**
     * Constructs object
     *
     * @param aModel a Model object to run
     */
    public TimeDriver(Model aModel) {

        final String methodName = "TimeDriver";

        state = State.CONSTRUCTING;

        if (aModel == null) {
            throw new IllegalArgumentException(String.format("%s.%s : Models object does not exist.",
                                                             CLASSNAME_IN, methodName));
        }

        if (aModel.parXChange == null) {
            throw new IllegalArgumentException(String.format("%s.%s : parXChange object does not exist.",
                                                             CLASSNAME_IN, methodName));
        }

        if (aModel.simXChange == null) {
            throw new IllegalArgumentException(String.format("%s.%s : simXChange object does not exist.",
                                                             CLASSNAME_IN, methodName));
        }

        model = aModel;
        timer = new Timer();

        timer.setDatePeriod(model.parXChange.get(STARTDATE, CLASSNAME_IN, LocalDate.class),
                            model.parXChange.get(ENDDATE  , CLASSNAME_IN, LocalDate.class));
        model.simXChange.setDatePeriod(timer.startDate(), timer.endDate());
        if (model.parXChange.contains(PAUSEDATE, LocalDate.class)) {
            timer.setPauseDate(model.parXChange.get(PAUSEDATE, CLASSNAME_IN, LocalDate.class));
        }

        state = State.CONSTRUCTED;
    }

    @Override
    public String toString() {
        return CLASSNAME_IN + "{startDate=" + timer.startDate() + ", curDate=" + timer.date() + "}";
    }

    /**
     * Runs the Model object provided during object creation from start date to end date
     * @throws Exception (todo)
     */
    public void run() throws Exception {

        boolean terminateByTimeDriver = false;
        boolean terminateByModel      = false;

        state = State.INITIALISING;

        timer.reset();
        model.simXChange.reset();

        // do after model.simXChange.reset(), value of traceLogging flag is false otherwise
        if (model.parXChange.contains(TRACELOGGING, Boolean.class)) {
          model.simXChange.setTraceLogging(model.parXChange.get(TRACELOGGING, CLASSNAME_IN, Boolean.class));
        }

        String msg;

        msg = String.format("Running from %s to %s (duration=%d days)", timer.startDate().toString(), timer.endDate().toString(), timer.duration());
        LOGGER.info(msg);

        msg = String.format("Date=%s (elapsed=%d days)", timer.date().toString(), timer.elapsed());
        LOGGER.debug(msg);

        state = State.INITIALISED;

        while (!terminateByTimeDriver && !terminateByModel) {
            state = State.UPDATING;

            // move to next day, and integrate state variables
            if (timer.date().isAfter(timer.startDate())) {
                model.simXChange.updateToDate(timer.date());
            }

            // for debugging purposes ++++++++++++++++++++++++++++++++++++++++++
            model.simXChange.setPause(timer.pauseNow());
            if (model.simXChange.pauseNow()) {
                final String curDate = model.simXChange.getCurDate().toString();
                System.out.println(String.format("Debug break point in %s on date=%s", CLASSNAME_IN, curDate));
            }
            // for debugging purposes ------------------------------------------

            state = State.INTERVENING;
            model.doModelAction(ModelAction.INTERVENE);

            state = State.AUXCALCULATING;
            model.doModelAction(ModelAction.AUXCALCULATIONS);

            // EVERYTHING IS NOW UP TO DATE ON THE CURRENT TIME !!!

            // see which simobjects have to be started, do here otherwise aux
            // vars are not available on current time
            state = State.TESTINGFORSTART;
            {
                // call all SimControllers of model to see if SimObjects need to be started
                int cnt = model.testForSimObjectsToStart(); // when started, auxcalculations must also be done inside this method
                while (cnt > 0) {
                    model.doModelAction(ModelAction.AUXCALCULATIONS);
                    cnt = model.testForSimObjectsToStart(); // when started, auxcalculations must also be done inside this method
                }
            }

            state = State.RATECALCULATING;
            model.doModelAction(ModelAction.RATECALCULATIONS);

            // call all controllers of model and let them terminate the SimObject(s)
            // which need to be terminated, also ask remaining running SimObject(s) whether
            // they can continue (method: SimObject.canContinue), if not, terminate them too
            state = State.TESTINGFORSIMOBJECTSTERMINATION;
            model.testForSimObjectsToTerminate();

            // terminate time loop if there were previous running models but
            // there are none anymore
            terminateByModel = model.testForTerminateByModel();

            if (!terminateByModel) {
                timer.dateStep();
                terminateByTimeDriver = timer.terminate();
            }

            if (!terminateByTimeDriver && !terminateByModel) {
                msg = String.format("Date=%s (elapsed=%d days)",
                                     timer.date().toString(), timer.elapsed());
                LOGGER.debug(msg); // log with debug level, otherwise too much output
            } else if (terminateByModel) {
                msg = String.format("Terminating on date=%s, elapsed=%d days (on request of %s)",
                                     timer.date().toString(), timer.elapsed(), model.CLASSNAME_IN);
                LOGGER.info(msg);
            } else if (terminateByTimeDriver) {
                msg = String.format("Terminating on date=%s, elapsed=%d days (final date reached)",
                                     timer.date().toString(), timer.elapsed());
                LOGGER.info(msg);
            }
        }

        if (terminateByModel) {
          state = State.TERMINATING_BY_MODEL;
        } else if (terminateByTimeDriver) {
          state = State.TERMINATING_BY_TIMEDRIVER;
        }

        // terminate and destroy any left running simobjects
        model.simObjectsTerminate();

        model.simXChange.terminate();

        if (terminateByModel) {
          state = State.TERMINATED_BY_MODEL;
        } else if (terminateByTimeDriver) {
          state = State.TERMINATED_BY_TIMEDRIVER;
        }

        LOGGER.info("Terminated");
    }
}
