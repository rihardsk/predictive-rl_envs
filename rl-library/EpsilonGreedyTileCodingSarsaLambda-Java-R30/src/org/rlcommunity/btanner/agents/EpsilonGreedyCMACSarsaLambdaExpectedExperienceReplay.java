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
import org.rlcommunity.btanner.agentLib.functionApproximators.CMAC.CMACFunctionApproximatorFactory;
import org.rlcommunity.btanner.agentLib.functionApproximators.FunctionApproximatorFactoryInterface;
import org.rlcommunity.btanner.agentLib.learningBoosters.AbstractLearningBoosterFactory;
import org.rlcommunity.btanner.agentLib.learningBoosters.experienceReplay.ExpectedExperienceReplayLearningBoosterFactory;
import org.rlcommunity.btanner.agentLib.learningModules.AbstractLearningModuleFactory;

import org.rlcommunity.btanner.agentLib.learningModules.sarsaLambda.SarsaLambdaLearningModuleFactory;
import rlVizLib.dynamicLoading.Unloadable;
import rlVizLib.general.ParameterHolder;

/**
 *
 * @author Brian Tanner
 */
public class EpsilonGreedyCMACSarsaLambdaExpectedExperienceReplay extends AbstractSarsa implements Unloadable {

    public static ParameterHolder getDefaultParameters() {
        FunctionApproximatorFactoryInterface FAF=new CMACFunctionApproximatorFactory();
        ActionSelectorFactoryInterface ASF=new EpsilonGreedyActionSelectorFactory();
        SarsaLambdaLearningModuleFactory ERLMF=new SarsaLambdaLearningModuleFactory(FAF, ASF);
        ParameterHolder p = AbstractSarsa.getDefaultParameters(ERLMF);
        
        ExpectedExperienceReplayLearningBoosterFactory eReplayFactory=new ExpectedExperienceReplayLearningBoosterFactory();
        eReplayFactory.addToParameterHolder(p);
        return p;
    }
   @Override
    protected AbstractLearningModuleFactory makeLearningModuleFactory() {
        return new SarsaLambdaLearningModuleFactory(makeFunctionApproximatorFactory(), makeActionSelectorFactory());
    }
    
    protected FunctionApproximatorFactoryInterface makeFunctionApproximatorFactory() {
        return new CMACFunctionApproximatorFactory();
    }

    protected ActionSelectorFactoryInterface makeActionSelectorFactory() {
        return new EpsilonGreedyActionSelectorFactory();
    }

   public EpsilonGreedyCMACSarsaLambdaExpectedExperienceReplay(ParameterHolder p){
        super(p);
    }

    public EpsilonGreedyCMACSarsaLambdaExpectedExperienceReplay(){
        this(getDefaultParameters());
    }
    
    @Override
    protected Vector<AbstractLearningBoosterFactory> makeBoosterFactories() {
       Vector<AbstractLearningBoosterFactory> theseBoosters=new Vector<AbstractLearningBoosterFactory>();
       theseBoosters.add(new ExpectedExperienceReplayLearningBoosterFactory());
       return theseBoosters;
    }

}
