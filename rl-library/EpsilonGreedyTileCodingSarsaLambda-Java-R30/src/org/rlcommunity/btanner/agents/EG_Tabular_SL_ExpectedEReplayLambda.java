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
package org.rlcommunity.btanner.agents;

import org.rlcommunity.btanner.agentLib.actionSelectors.epsilonGreedy.EpsilonGreedyActionSelectorFactory;
import java.util.Vector;
import org.rlcommunity.btanner.agentLib.actionSelectors.ActionSelectorFactoryInterface;
import org.rlcommunity.btanner.agentLib.functionApproximators.FunctionApproximatorFactoryInterface;
import org.rlcommunity.btanner.agentLib.functionApproximators.tabular.TabularFunctionApproximatorFactory;
import org.rlcommunity.btanner.agentLib.learningBoosters.AbstractLearningBoosterFactory;
import org.rlcommunity.btanner.agentLib.learningBoosters.experienceReplay.ExpectedExperienceReplayLambdaLearningBoosterFactory;
import org.rlcommunity.btanner.agentLib.learningModules.AbstractLearningModuleFactory;

import org.rlcommunity.btanner.agentLib.learningModules.sarsaLambda.SarsaLambdaLearningModuleFactory;
import rlVizLib.general.ParameterHolder;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;
import rlVizLib.dynamicLoading.Unloadable;

/**
 *
 * @author Brian Tanner
 */
public class EG_Tabular_SL_ExpectedEReplayLambda extends AbstractSarsa  implements Unloadable{

    public static ParameterHolder getDefaultParameters() {
        FunctionApproximatorFactoryInterface FAF=new TabularFunctionApproximatorFactory();
        ActionSelectorFactoryInterface ASF=new EpsilonGreedyActionSelectorFactory();
        SarsaLambdaLearningModuleFactory ERLMF=new SarsaLambdaLearningModuleFactory(FAF, ASF);
        ParameterHolder p = AbstractSarsa.getDefaultParameters(ERLMF);
        
        ExpectedExperienceReplayLambdaLearningBoosterFactory eReplayFactory=new ExpectedExperienceReplayLambdaLearningBoosterFactory();
        eReplayFactory.addToParameterHolder(p);
        return p;
    }
   @Override
    protected AbstractLearningModuleFactory makeLearningModuleFactory() {
        return new SarsaLambdaLearningModuleFactory(makeFunctionApproximatorFactory(), makeActionSelectorFactory());
    }
    
    protected FunctionApproximatorFactoryInterface makeFunctionApproximatorFactory() {
        return new TabularFunctionApproximatorFactory();
    }

    @Override
        public Action agent_start(Observation theObservation) {
        //Get the values of all of the s,a pairs
        
        Observation tmpObs=new Observation(1,0);
        
        for(int i=0;i<5;i++){
        tmpObs.intArray[0]=i;
        double a0=super.getValueForStateAction(tmpObs, 0);
        double a1=super.getValueForStateAction(tmpObs, 1);
        System.out.printf("(%.2f, %.2f) ",a0,a1);
        }
        System.out.print("\r");
        System.out.flush();

        return super.agent_start(theObservation);
        }
    protected ActionSelectorFactoryInterface makeActionSelectorFactory() {
        return new EpsilonGreedyActionSelectorFactory();
    }

   public EG_Tabular_SL_ExpectedEReplayLambda(ParameterHolder p){
        super(p);
    }

    public EG_Tabular_SL_ExpectedEReplayLambda(){
        this(getDefaultParameters());
    }
    
    @Override
    protected Vector<AbstractLearningBoosterFactory> makeBoosterFactories() {
       Vector<AbstractLearningBoosterFactory> theseBoosters=new Vector<AbstractLearningBoosterFactory>();
       theseBoosters.add(new ExpectedExperienceReplayLambdaLearningBoosterFactory());
       return theseBoosters;
    }

}
