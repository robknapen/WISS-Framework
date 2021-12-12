/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.wur.wiss.core;

import nl.wur.wiss.core.ParXChange;
import nl.wur.wiss.core.SimObject;
import nl.wur.wiss.core.SimXChange;

/**
 *
 * @author kraal001
 */

/*
General:
========

This class is a dummy class, doing nothing but exist

*/
public class SimTemplate extends SimObject {

    // BEGIN INPUT from parXChange ---------------------------------------------
    // END INPUT from parXChange -----------------------------------------------

    // BEGIN INPUT from systemXChange ------------------------------------------
    // END INPUT from systemXChange --------------------------------------------

    // BEGIN OUTPUT to parXChange ----------------------------------------------
    // END OUTPUT to parXChange ------------------------------------------------

    // BEGIN OUTPUT to systemXChange -------------------------------------------
    // END OUTPUT to systemXChange ---------------------------------------------

    // Internal declarations ---------------------------------------------------

    public SimTemplate(String     aSimID,
                    ParXChange aParXChange,
                    SimXChange aSimXChange) {
        super(aSimID, aParXChange, aSimXChange, 1, 1, "SimDummy", "SimDummy");

//      this.pullSimXChangeData();
//      this.pushSimXChangeData();
        this.auxCalculations();
    }

    @Override
    public void intervene() {
        super.intervene();

//      this.pullSimXChangeData();

//      if (simXChange.pauseNow()) {
//          System.out.println(String.format("Debug break point in %s on date=%s", simID, simXChange.getCurDate().toString()));
//      }

        // states can be forced here
    }

    @Override
    public void auxCalculations() {
        super.auxCalculations();

//      this.pullSimXChangeData();

//      if (simXChange.pauseNow()) {
//          System.out.println(String.format("Debug break point in %s on date=%s", simID, simXChange.getCurDate().toString()));
//      }

//      this.pushSimXChangeData();
    }

    @Override
    public void rateCalculations() {
        super.rateCalculations();

//      this.pullSimXChangeData();

//      if (simXChange.pauseNow()) {
//          System.out.println(String.format("Debug break point in %s on date=%s", simID, simXChange.getCurDate().toString()));
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

        super.terminate();
    }

    private void pullSimXChangeData() {

        // get copies of all external variables (<SimValueExternal> variables) that need to
        // be there at any time during object existence, use:
        // simXChange.tryValue(<SimValueExternal>)

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
            // simXChange.forceState(<SimValueState>)

        } else if (this.isAuxCalculating()) {

            // set replacement values of all owned auxiliary variables (<SimValueAux> variables), use:
            // simXChange.setAux(<SimValueAux>)

        } else if (this.isRateCalculating()) {

            // set rates of all owned states (<SimValueState> variables), use:
            // simXChange.setRate(<SimValueState>)

        }
    }
}
