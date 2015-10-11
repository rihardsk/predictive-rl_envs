/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rlcommunity.agents;

import java.util.ArrayList;
import java.util.List;
import org.rlcommunity.btanner.agentLib.normalizers.ObservationScalerInterface;
import org.rlcommunity.btanner.agentLib.normalizers.TaskSpecObservationNormalizer;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpec;
import org.rlcommunity.rlglue.codec.types.Observation;

/**
 *
 * @author btanner
 */
public class TestRBFBasisSet implements BasisFunctionSet {

    private List<BasisFunctionInterface> theBasisList = new ArrayList<BasisFunctionInterface>();
    private double beta = 4.0;
    private int numActions = 1;
    private final ObservationScalerInterface theNormalizer;

    /**
     * Assume that the actual points will be in [0,1]
     */
    public TestRBFBasisSet(int numDimensions, int numPartitions, TaskSpec theTaskSpec) {
        this.numActions = theTaskSpec.getDiscreteActionRange(0).getRangeSize();
        theNormalizer = new TaskSpecObservationNormalizer(theTaskSpec);
        double[] indexArray = new double[numDimensions];
        recurse(0, indexArray, numPartitions);
    }

    private void recurse(int thisIndex, double[] indexArray, int numPartitions) {
        for (int i = numPartitions - 1; i >= 0; i--) {
            double thisValue = (0.0d + (double) i) / (double) (numPartitions - 1);
            indexArray[thisIndex] = thisValue;

            if (thisIndex == indexArray.length - 1) {
                theBasisList.add(new GaussianRBFKernel(indexArray, beta));
            } else {
                recurse(thisIndex + 1, indexArray, numPartitions);
            }

        }
    }

    public int getNumFeatures() {
        return 1 + (numActions * theBasisList.size());
    }
    public double[] calculateFeatures(Observation theState, int theAction) {
        return calculateFeatures(theState, theAction,false);
    }


    public double[] calculateFeatures(Observation theState, int theAction, boolean verbose) {
        double[] theFeatures = new double[getNumFeatures()];
        //Bias
        theFeatures[0] = 1.0d;

        if(verbose){
        System.out.println("The action is: "+theAction);
        }
        for (int i = 0; i < theBasisList.size(); i++) {
            int thisFeatureIndex = 1 + theAction * theBasisList.size()+i;
            BasisFunctionInterface thisKernel = theBasisList.get(i);


            double[] scaledDoubleArray=theNormalizer.scaleObservation(theState).doubleArray;


            theFeatures[thisFeatureIndex] = thisKernel.weight(scaledDoubleArray);
        if(verbose){
            System.out.println("Scaled observation for: "+theState.getDouble(0)+" "+theState.getDouble(1)+" is: "+scaledDoubleArray[0]+" "+scaledDoubleArray[1]);
            System.out.println("theFeatures["+thisFeatureIndex+"] = "+theFeatures[thisFeatureIndex]);
        }
        }
        return theFeatures;

    }
}
