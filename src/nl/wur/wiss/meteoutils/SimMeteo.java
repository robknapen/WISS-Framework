/*
 * Copyright 1988, 2013, 2016 Alterra, Wageningen UR
 *
 * Licensed under the EUPL, Version 1.1 or as soon they
 * will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the
 * Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in
 * writing, software distributed under the Licence is
 * distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */
package nl.wur.wiss_framework.meteoutils;

import nl.wur.wiss_framework.core.*;
import nl.wur.wiss_framework.mathutils.RangeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author kraal001
 */

/*
General:
========

Reads weather data from the provided meteo object and ensures that weather
is punt in SimXChange on the proper simulation day.

*/

public class SimMeteo extends SimObject {

    // class logger
    private static final Log LOGGER = LogFactory.getLog(SimMeteo.class);

    public static final String METEO = "METEO";

    // BEGIN INPUT from parXChange ---------------------------------------------

    private MeteoReader meteo = null;

    private final String PublisherE0  = parXChange.get(MeteoReader.PUBLISHER_E0 , CLASSNAME_IN, String.class);
    private final String PublisherES0 = parXChange.get(MeteoReader.PUBLISHER_ES0, CLASSNAME_IN, String.class);
    private final String PublisherET0 = parXChange.get(MeteoReader.PUBLISHER_ET0, CLASSNAME_IN, String.class);

    // END INPUT from parXChange -----------------------------------------------

    // BEGIN INPUT from systemXChange ------------------------------------------
    // END INPUT from systemXChange --------------------------------------------

    // BEGIN OUTPUT to parXChange ----------------------------------------------

    private final double LONGITUDEDD;
    private final double LATITUDEDD;
    private final double ALTITUDEM;

    // END OUTPUT to parXChange ------------------------------------------------

    // BEGIN OUTPUT to systemXChange -------------------------------------------

    private final SimValueAux TM_MX = new SimValueAux(simID, MeteoElement.TM_MX.name(), ScientificUnit.CELSIUS, RangeUtils.RangeType.TEMPCELSIUS );
    private final SimValueAux TM_MN = new SimValueAux(simID, MeteoElement.TM_MN.name(), ScientificUnit.CELSIUS, RangeUtils.RangeType.TEMPCELSIUS );
    private final SimValueAux Q_CU  = new SimValueAux(simID, MeteoElement.Q_CU.name() , ScientificUnit.J_M2D1 , RangeUtils.RangeType.ZEROPOSITIVE);
    private final SimValueAux VP_AV = new SimValueAux(simID, MeteoElement.VP_AV.name(), ScientificUnit.HPA    , RangeUtils.RangeType.ZEROPOSITIVE);
    private final SimValueAux WS_AV = new SimValueAux(simID, MeteoElement.WS2_AV.name(), ScientificUnit.M_S    , RangeUtils.RangeType.ZEROPOSITIVE);
    private final SimValueAux PR_CU = new SimValueAux(simID, MeteoElement.PR_CU.name(), ScientificUnit.MM_D1  , RangeUtils.RangeType.ZEROPOSITIVE);

    private final SimValueAux E0  = new SimValueAux(simID, MeteoElement.E0.name() , ScientificUnit.MM_D1, RangeUtils.RangeType.ZEROPOSITIVE);
    private final SimValueAux ES0 = new SimValueAux(simID, MeteoElement.ES0.name(), ScientificUnit.MM_D1, RangeUtils.RangeType.ZEROPOSITIVE);
    private final SimValueAux ET0 = new SimValueAux(simID, MeteoElement.ET0.name(), ScientificUnit.MM_D1, RangeUtils.RangeType.ZEROPOSITIVE);

    // END OUTPUT to systemXChange ---------------------------------------------

    // Internal declarations ---------------------------------------------------
    private boolean bPublishE0;
    private boolean bPublishES0;
    private boolean bPublishET0;

    public SimMeteo(String     aSimID,
                    ParXChange aParXChange,
                    SimXChange aSimXChange) {
        super(aSimID, aParXChange, aSimXChange, 1, 1, "SimMeteo", "Provision of meteo data");

//      this.pullSimXChangeData();

        meteo = parXChange.get(SimMeteo.METEO, simID, MeteoReader.class, ScientificUnit.NA);

        LONGITUDEDD = meteo.getLongitudeDD();
        LATITUDEDD  = meteo.getLatitudeDD();
        ALTITUDEM   = meteo.getAltitudeM();

        // publish meteo station location
        parXChange.set(MeteoElement.LONGITUDEDD.name(), Double.class, true, LONGITUDEDD, ScientificUnit.ANGULARDD);
        parXChange.set(MeteoElement.LATITUDEDD.name() , Double.class, true, LATITUDEDD , ScientificUnit.ANGULARDD);
        parXChange.set(MeteoElement.ALTITUDEM.name()  , Double.class, true, ALTITUDEM  , ScientificUnit.M);

        // prepare with the required elements, duplicates will be ignored
        HashSet<MeteoElement> requiredElements = new HashSet<>();
        requiredElements.add(MeteoElement.TM_MX);
        requiredElements.add(MeteoElement.TM_MN);
        requiredElements.add(MeteoElement.Q_CU);
        requiredElements.add(MeteoElement.VP_AV);
        requiredElements.add(MeteoElement.WS2_AV);
        requiredElements.add(MeteoElement.PR_CU);

        Set<MeteoElement> availableElements = meteo.getSourceElements();

        // see what to do with E0 ----------------------------------------------
        bPublishE0 = false;
        if (PublisherE0.equals(MeteoReader.PUBLISHER_AUTOMATIC)) {
            bPublishE0 = availableElements.contains(MeteoElement.E0);
        } else if (PublisherE0.equals(CLASSNAME_IN)) {
            bPublishE0 = true;
        }

        // see what to do with ES0 ---------------------------------------------
        bPublishES0 = false;
        if (PublisherES0.equals(MeteoReader.PUBLISHER_AUTOMATIC)) {
            bPublishES0 = availableElements.contains(MeteoElement.ES0);
        } else if (PublisherES0.equals(CLASSNAME_IN)) {
            bPublishES0 = true;
        }

        // see what to do with ET0 ---------------------------------------------
        bPublishET0 = false;
        if (PublisherET0.equals(MeteoReader.PUBLISHER_AUTOMATIC)) {
            bPublishET0 = availableElements.contains(MeteoElement.ET0);
        } else if (PublisherET0.equals(CLASSNAME_IN)) {
            bPublishET0 = true;
        }

        // log warnings when a variable that this SimObject is capable of, is not wanted
        if (bPublishE0) {
            requiredElements.add(MeteoElement.E0);
        } else {
            LOGGER.warn(String.format("SimObject %s with simID=%s will not be active for %s, this variable is already determined by some other simobject.",
                                       CLASSNAME_IN, simID, MeteoElement.E0.name()));
        }

        if (bPublishES0) {
            requiredElements.add(MeteoElement.ES0);
        } else {
            LOGGER.warn(String.format("SimObject %s with simID=%s will not be active for %s, this variable is already determined by some other simobject.",
                                       CLASSNAME_IN, simID, MeteoElement.ES0.name()));
        }

        if (bPublishET0) {
            requiredElements.add(MeteoElement.ET0);
        } else {
            LOGGER.warn(String.format("SimObject %s with simID=%s will not be active for %s, this variable is already determined by some other simobject.",
                                       CLASSNAME_IN, simID, MeteoElement.ET0.name()));
        }

        meteo.prepare(simXChange.getStartDate(), simXChange.getEndDate(), requiredElements);

        // set initial values for all states, use:
        // <SimValueState>.v = <value>

//      this.pushSimXChangeData();
        this.auxCalculations();
    }

    @Override
    public void intervene() {
        super.intervene();

//      this.pullSimXChangeData();

//      if (simXChange.pauseNow()) {
//          final String curDate = simXChange.getCurDate().toString();
//          System.out.println(String.format("Debug break point in %s on date=%s", simID, curDate));
//      }

        // states can be forced here
    }

    @Override
    public void auxCalculations() {
        super.auxCalculations();

//      this.pullSimXChangeData();

        if (simXChange.pauseNow()) {
            final String curDate = simXChange.getCurDate().toString();
            System.out.println(String.format("Debug break point in %s on date=%s", simID, curDate));
        }

        LocalDate curDate = simXChange.getCurDate();

        TM_MX.v = meteo.getValue(curDate, MeteoElement.TM_MX , TM_MX.u);
        TM_MN.v = meteo.getValue(curDate, MeteoElement.TM_MN , TM_MN.u);
        Q_CU.v  = meteo.getValue(curDate, MeteoElement.Q_CU  , Q_CU.u);
        VP_AV.v = meteo.getValue(curDate, MeteoElement.VP_AV , VP_AV.u);
        WS_AV.v = meteo.getValue(curDate, MeteoElement.WS2_AV, WS_AV.u);
        PR_CU.v = meteo.getValue(curDate, MeteoElement.PR_CU , PR_CU.u);

        if (bPublishE0) {
            E0.v  = meteo.getValue(curDate, MeteoElement.E0 , E0.u);
        }

        if (bPublishES0) {
            ES0.v = meteo.getValue(curDate, MeteoElement.ES0, ES0.u);
        }

        if (bPublishET0) {
            ET0.v = meteo.getValue(curDate, MeteoElement.ET0, ET0.u);
        }
        this.pushSimXChangeData();
    }

    @Override
    public void rateCalculations() {
        // default is to continue simulation of this module
        super.rateCalculations();

//      this.pullSimXChangeData();

//      if (simXChange.pauseNow()) {
//          final String curDate = simXChange.getCurDate().toString();
//          System.out.println(String.format("Debug break point in %s on date=%s", simID, curDate));
//      }

//      this.pushSimXChangeData();
    }

    @Override
    public boolean canContinue() {
        // can always continue
        return true;
    }

    @Override
    public void terminate() {
        // delete any parXChange item which is set in this object, use:
        // parXChange.delete(<owner>, <name>, <class>)
        parXChange.delete(MeteoElement.LONGITUDEDD.name(), Double.class);
        parXChange.delete(MeteoElement.LATITUDEDD.name() , Double.class);
        parXChange.delete(MeteoElement.ALTITUDEM.name()  , Double.class);

        super.terminate();
    }

    private void pullSimXChangeData() {

        // get copies of all external variables (<SimValueExternal> variables) that need to
        // be there at any time during object existence, use:
        // simXChange.getSimValueExternalByVarNameDelta(<SimValueExternal>)

        if (this.isIntervening() ||
            this.isAuxCalculating() ||
            this.isRateCalculating()) {

            // get copies of all owned states (<SimValueState> variables), only when in time loop, use:
            // simXChange.getSimValueState(<SimValueState>)
        }
    }

    private void pushSimXChangeData() {

        if (this.isInitializing()) {

            // set initial value of all owned states (<SimValueState> variables), use:
            // simXChange.forceSimValueState(<SimValueState>)

        } else if (this.isAuxCalculating()) {

            // set replacement values of all owned auxiliary variables (<SimValueAux> variables), use:
            // simXChange.setSimValueAux(<SimValueAux>)

            simXChange.setSimValueAux(TM_MX);
            simXChange.setSimValueAux(TM_MN);
            simXChange.setSimValueAux(Q_CU);
            simXChange.setSimValueAux(VP_AV);
            simXChange.setSimValueAux(WS_AV);
            simXChange.setSimValueAux(PR_CU);

            if (bPublishE0) {
                simXChange.setSimValueAux(E0);
            }

            if (bPublishES0) {
                simXChange.setSimValueAux(ES0);
            }

            if (bPublishET0) {
                simXChange.setSimValueAux(ET0);
            }

        } else if (this.isRateCalculating()) {

            // set rates of all owned states (<SimValueState> variables), use:
            // simXChange.setSimValueState(<SimValueState>)

        }
    }
}
