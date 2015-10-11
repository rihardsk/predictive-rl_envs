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
package org.rlcommunity.btanner.agentLib.actionSelectors.softMax;

import java.util.Random;
import org.rlcommunity.btanner.agentLib.actionSelectors.*;
import org.rlcommunity.btanner.agentLib.learningModules.ValueProviderInterface;
import rlVizLib.general.ParameterHolder;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpec;
import org.rlcommunity.rlglue.codec.types.Observation;

/**
 *
 * @author Brian Tanner
 */
public class SoftMaxActionSelector implements ActionSelectorInterface {

    double temperature = 1.0;
    int actionCount = 0;
    Random R = new Random();

    public SoftMaxActionSelector(TaskSpec theTaskObject, ParameterHolder p) {
        this.temperature = p.getDoubleParam("softmax-temperature");

        actionCount = 1 + theTaskObject.getDiscreteActionRange(0).getMax() - theTaskObject.getDiscreteActionRange(0).getMin();
        assert (actionCount > 0);

        //here we're asserting there IS only one discrete action variable. 
        assert (theTaskObject.getNumDiscreteActionDims() == 1); //check the number of discrete actions is only 1
        assert (theTaskObject.getNumContinuousActionDims() == 0); //check that there is no continuous actions
        

    }

    public int sampleAction(Observation theObservation, ValueProviderInterface theValueProvider, long uniqueId) {
        int action = 0;

        double r = R.nextDouble();

        double[] theProbs = getActionProbabilities(theObservation, theValueProvider);



        while (theProbs[action] < r && (action < actionCount - 1)) {
            r -= theProbs[action];
            action++;
        }


        return action;
    }

    public static void addToParameterHolder(ParameterHolder p) {
        p.addDoubleParam("softmax-temperature", 1.0d);
    }

    public int sampleAction(Observation sprime, ValueProviderInterface aThis) {
        return sampleAction(sprime, aThis, -1);

    }

    public double[] getActionProbabilities(Observation theObservation, ValueProviderInterface theValueProvider) {
        double[] actionValues = new double[actionCount];
        for (int a = 0; a < actionCount; a++) {
            actionValues[a] = theValueProvider.query(theObservation, a);
        }
        return getActionProbabilities(actionValues);
    }

    public double[] getActionProbabilities(double[] actionValues) {
        double[] allExpValues = new double[actionCount];
        double[] allbaseValues = new double[actionCount];
        double actionExpSum = 0;

        double maxSoFar = -Double.MAX_VALUE;
        for (int a = 0; a < actionCount; a++) {
            allbaseValues[a] = actionValues[a];
            if (maxSoFar < allbaseValues[a]) {
                maxSoFar = allbaseValues[a];
            }
        }

        for (int a = 0; a < actionCount; a++) {
            double thisActionValue = allbaseValues[a] - maxSoFar;
            double thisExpValue = Math.exp(thisActionValue / temperature);
            actionExpSum += thisExpValue;
            allExpValues[a] = thisExpValue;
        }
        for (int a = 0; a < actionCount; a++) {
            allExpValues[a] /= actionExpSum;
        }
        return allExpValues;
    }

    public void cleanup() {
        R=null;
    }
}
