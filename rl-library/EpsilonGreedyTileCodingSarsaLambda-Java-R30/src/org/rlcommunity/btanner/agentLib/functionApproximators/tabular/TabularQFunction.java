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

package org.rlcommunity.btanner.agentLib.functionApproximators.tabular;

import org.rlcommunity.btanner.agentLib.functionApproximators.CMAC.FeatureBasedFunctionApproximatorInterface;
import rlVizLib.general.ParameterHolder;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpec;
import org.rlcommunity.rlglue.codec.types.Observation;

/**
 *
 * @author Brian Tanner
 */
public class TabularQFunction implements FeatureBasedFunctionApproximatorInterface {
double[] theWeights=null;

public static void addToParameterHolder(ParameterHolder p){
}

public TabularQFunction(TaskSpec theTaskSpecObject,ParameterHolder p){
    
    //Make sure we can try to do this
        assert (theTaskSpecObject.getNumDiscreteObsDims() == 1); //check the number of discrete observations is only 1
        assert (theTaskSpecObject.getNumContinuousObsDims() == 0); //check that there is no continuous observations

       theWeights=new double[theTaskSpecObject.getDiscreteObservationRange(0).getMax()];
    }

    public void init() {

    }
/**
 * 
 * @param normalizedObservation
 * @param delta Assume that delta is pre-alpha reduced
 */
    public void update(Observation normalizedObservation, double delta) {
        theWeights[normalizedObservation.intArray[0]]+=delta;
    }

    
    public double query(Observation normalizedObservation) {
        int theState=normalizedObservation.intArray[0];
        if(theState>=theWeights.length)return 0.0d;
        return theWeights[theState];
    }

    public double query(Observation theObservation, long uniqueId) {
        //This is the same because state is unique for tabular
        return query(theObservation);
    }

    public int getMemorySize() {
        return theWeights.length;
    }

    public int getActiveFeatureCount() {
        return 1;
    }

    public void setActiveFeatures(Observation normalizedObservation, int[] theFeatures) {
        theFeatures[0]=normalizedObservation.intArray[0];
    }

    public void incrementFeatureByAmount(int whatFeature, double delta) {
        theWeights[whatFeature]+=delta;
    }

    public void cleanup() {
        theWeights=null;
    }

}
