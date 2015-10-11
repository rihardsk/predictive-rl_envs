/*
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.rlcommunity.btanner.agentLib.functionApproximators.CMAC;

import java.util.Vector;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpec;
import rlVizLib.general.ParameterHolder;
import org.rlcommunity.rlglue.codec.types.Observation;

/**
 * NOT THREAD SAFE
 * @author Brian Tanner
 */
public class CombinatorialTileCoder implements FeatureBasedFunctionApproximatorInterface {
    private Vector<SubsetTileCoder> allCoders=new Vector<SubsetTileCoder>();


    private final double[] coderWeights;
    private final int memoryPerCoder;
    private final int totalFeatureCount;
    private final int numTilings;
    
    public static void addToParameterHolder(ParameterHolder p) {
        p.addIntegerParam("cmac-grid-size", 8);
        p.addIntegerParam("cmac-num-tilings", 16);
        p.addIntegerParam("cmac-memory-size", 50000);
        p.addDoubleParam("cmac-initial-value", 0.0d);
        p.addDoubleParam("cmac-complex-base",1.0d);

        p.setAlias("cmac-memorySize", "cmac-memory-size");
        p.setAlias("cmac-numTilings", "cmac-num-tilings");
        p.setAlias("cmac-gridSize", "cmac-grid-size");
    }

    public CombinatorialTileCoder(TaskSpec theTaskSpec,ParameterHolder p) {

        int memorySize = p.getIntegerParam("cmac-memory-size");
        int theGridSize = p.getIntegerParam("cmac-grid-size");
        numTilings = p.getIntegerParam("cmac-num-tilings");
        double complexityBase=p.getDoubleParam("cmac-complex-base");

        assert complexityBase!=0 : "Setting cmac-complex-base to 0 means CRASH!";
        double initialValue = p.getDoubleParam("cmac-initial-value");

        int[] theVarSet=new int[theTaskSpec.getNumContinuousObsDims()];
        for(int i=0;i<theVarSet.length;i++){
            theVarSet[i]=i;
        }
        //Generate the powerset of all dimensions.  Fun times.
        Vector<int[]> powerSetOfDims=powerset(theVarSet);

        int numSubCoders=powerSetOfDims.size();

        //Lets split the memory size between all of the tile coders...
        memoryPerCoder=memorySize/numSubCoders;

        totalFeatureCount=memoryPerCoder*numSubCoders;

        coderWeights=new double[numSubCoders];
        double weightSum=0.0d;

        int counter=0;
        for (int[] thisVarSet : powerSetOfDims) {
            coderWeights[counter]=Math.pow(complexityBase,thisVarSet.length);
            weightSum+=coderWeights[counter];
            counter++;
        }
        for (int i=0;i<numSubCoders;i++) {
            coderWeights[i]/=weightSum;
        }


        for (int i=0;i<numSubCoders;i++) {
//            System.out.printf("Coder %d has %d dims and weight %.3f\n",i,powerSetOfDims.get(i).length,coderWeights[i]);
            SubsetTileCoder thisTC=new SubsetTileCoder(memoryPerCoder, theGridSize, numTilings, initialValue*coderWeights[i], powerSetOfDims.get(i));
            allCoders.add(thisTC);
        }

    }

    public void init() {
    }

    /**
     * This basically splits up the error and propagates it to all of the sub tile coders.
     * @param normalizedObservation
     * @param delta We assume that delta has been reduced by a step size already,
     * but has not been split to consider how many tiles there are (because the
     * tile coder is black box that way).  Don't worry, we will scale delta.
     */
    public final void update(Observation normalizedObservation, double delta) {
        int numSubCoders=allCoders.size();

        for (int i=0;i<numSubCoders;i++) {
            allCoders.get(i).update(normalizedObservation, delta*coderWeights[i]);
        }
    }


    public double query(Observation normalizedObservation) {
        return query(normalizedObservation, -1);
    }

    public double query(Observation normalizedObservation, long uniqueId) {
        int numSubCoders=allCoders.size();

        double valueEstimate = 0.0d;
        for (int i=0;i<numSubCoders;i++) {
            valueEstimate+=coderWeights[i]*allCoders.get(i).query(normalizedObservation);
        }
        return valueEstimate;
    }

    /*
    The next few functions are because we implement FeatureBasedFunctionApproximatorInterface
     */
    public int getMemorySize() {
        return totalFeatureCount;
    }

    public int getActiveFeatureCount() {
        return numTilings*allCoders.size();
    }

    /**
     * Loop through the coders, giving them each a slice of the total features to fill in.
     * @param normalizedObservation
     * @param theFeatures
     */
    public void setActiveFeatures(Observation normalizedObservation, int[] theFeatures) {
        int activeOffset=0;
        int memoryOffset=0;
        int[] tmpFeatures=new int[numTilings];
        for (SubsetTileCoder thisTileCoder : allCoders) {
            thisTileCoder.setActiveFeatures(normalizedObservation, tmpFeatures);
            for(int i=0;i<numTilings;i++){
                theFeatures[activeOffset+i]=memoryOffset+tmpFeatures[i];
            }
            activeOffset+=numTilings;
            memoryOffset+=memoryPerCoder;
        }
    }

    public void incrementFeatureByAmount(int aggregateFeatureNumber, double delta) {
        int whichCoder=aggregateFeatureNumber/memoryPerCoder;
        int whichFeature=aggregateFeatureNumber%memoryPerCoder;

        double splitWeight = delta*coderWeights[whichCoder];
        allCoders.get(whichCoder).incrementFeatureByAmount(whichFeature, splitWeight);
    }

    public void cleanup() {
    }

    /**
     * Brian Tanner: June 2009.
     * I stole (and then modified) code from here:
     * http://jvalentino.blogspot.com/2007/02/shortcut-to-calculating-power-set-using.html
     *
     *
     * Returns the power set from the given set by using a binary counter
     * Example: S = {a,b,c}
     * P(S) = {[], [c], [b], [b, c], [a], [a, c], [a, b], [a, b, c]}
     * @param set String[]
     * @return LinkedHashSet
     */
   private static Vector<int[]> powerset(int[] set) {

       //create the empty power set
       Vector<int[]> power = new Vector<int[]>();

       //get the number of elements in the set
       int elements = set.length;

       //the number of members of a power set is 2^n
       int powerElements = (int) Math.pow(2,elements);


       //run a binary counter for the number of power elements
       //start at 1 because we don't want the empty one
       for (int i = 1; i < powerElements; i++) {

           //convert the binary number to a string containing n digits
           String binary = intToBinary(i, elements);

           //create a new set
           Vector<Integer> innerSet = new Vector<Integer>();

           //convert each digit in the current binary number to the corresponding element
            //in the given set
           for (int j = 0; j < binary.length(); j++) {
               if (binary.charAt(j) == '1')
                   innerSet.add(set[j]);
           }
           int[] innerSetArray=new int[innerSet.size()];
           for(int j=0;j<innerSet.size();j++){
               innerSetArray[j]=innerSet.get(j);
           }

           //add the new set to the power set
           power.add(innerSetArray);

       }
       return power;
   }

   /**
     * Brian Tanner: June 2009.
     * Again, I stole (and then modified) code from here:
     * http://jvalentino.blogspot.com/2007/02/shortcut-to-calculating-power-set-using.html
     *
     * Converts the given integer to a String representing a binary number
     * with the specified number of digits
     * For example when using 4 digits the binary 1 is 0001
     * @param binary int
     * @param digits int
     * @return String
     */
   private static String intToBinary(int binary, int digits) {

       String temp = Integer.toBinaryString(binary);
       int foundDigits = temp.length();
       String returner = temp;
       for (int i = foundDigits; i < digits; i++) {
           returner = "0" + returner;
       }

       return returner;
   }
}



