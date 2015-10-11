/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rlcommunity.btanner.agentLib.normalizers;

import org.rlcommunity.rlglue.codec.taskspec.TaskSpec;
import org.rlcommunity.rlglue.codec.taskspec.ranges.DoubleRange;
import org.rlcommunity.rlglue.codec.types.Observation;

/**
 *
 * @author btanner
 */
public class TaskSpecObservationNormalizer implements ObservationScalerInterface {

    double[] Dmins;
    double[] Dmaxs;
    double[] Dranges;

 
    public TaskSpecObservationNormalizer(TaskSpec ts) {
        Dmins = new double[ts.getNumContinuousObsDims()];
        Dmaxs = new double[ts.getNumContinuousObsDims()];
        Dranges = new double[ts.getNumContinuousObsDims()];

        int currentDIndex = 0;
        for (int i = 0; i < ts.getNumContinuousObsDims(); i++) {
            DoubleRange thisRange=ts.getContinuousObservationRange(i);
                Dmins[currentDIndex] = thisRange.getMin();
                Dmaxs[currentDIndex] = thisRange.getMax();
                Dranges[currentDIndex] = thisRange.getMax()-thisRange.getMin();
                currentDIndex++;
        }
    }

    public double scale(int whichDoubleVariable, double thisValue) {
        double returnValue = (thisValue - Dmins[whichDoubleVariable]) / Dranges[whichDoubleVariable];
        return returnValue;
    }

    public double unScale(int whichDoubleVariable, double thisValue) {
        return thisValue * Dranges[whichDoubleVariable] + Dmins[whichDoubleVariable];
    }

    public Observation scaleObservation(Observation originalObservation) {
        int numInts = originalObservation.intArray.length;
        //Only check variables that we were told about in the task spec
        int numDoubles = Dmins.length;

        Observation returnObs = new Observation(numInts, numDoubles);

        for (int i = 0; i < numInts; i++) {
            returnObs.intArray[i] = originalObservation.intArray[i];
        }
        for (int i = 0; i < numDoubles; i++) {
            returnObs.doubleArray[i] = scale(i, originalObservation.doubleArray[i]);
        }

        return returnObs;
    }

    public void notifyOfValues(Observation o) {
        //Only check variables that we were told about in the task spec
        for (int i = 0; i < Dmins.length; i++) {
            if (o.doubleArray[i] < Dmins[i]) {
                System.err.println("Var: " + i + " : " + o.doubleArray[i] + " is less than task spec min: " + Dmins[i]);
            }
            if (o.doubleArray[i] > Dmaxs[i]) {
                System.err.println("Var: " + i + " : " + o.doubleArray[i] + " is more than task spec max: " + Dmaxs[i]);
            }
        }
    }

    public void notifyOfSpec(TaskSpec theTaskSpec) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
