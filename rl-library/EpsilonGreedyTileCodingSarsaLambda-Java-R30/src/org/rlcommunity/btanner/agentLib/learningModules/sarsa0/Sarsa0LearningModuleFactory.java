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

package org.rlcommunity.btanner.agentLib.learningModules.sarsa0;

import org.rlcommunity.btanner.agentLib.learningModules.LearningModuleInterface;
import org.rlcommunity.btanner.agentLib.actionSelectors.ActionSelectorFactoryInterface;
import org.rlcommunity.btanner.agentLib.functionApproximators.FunctionApproximatorFactoryInterface;
import org.rlcommunity.btanner.agentLib.learningModules.AbstractLearningModuleFactory;
import rlVizLib.general.ParameterHolder;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpec;

/**
 *
 * @author Brian Tanner
 */
public class Sarsa0LearningModuleFactory extends AbstractLearningModuleFactory {
    public Sarsa0LearningModuleFactory(FunctionApproximatorFactoryInterface FAF, ActionSelectorFactoryInterface ASF){
        super(FAF,ASF);
    }
    
    public LearningModuleInterface makeLearningModule(TaskSpec theTaskObject, ParameterHolder p) {
        return new Sarsa0LearningModule(theTaskObject, p, FAF);
    }


    @Override
    protected void addToPH(ParameterHolder p) {
       Sarsa0LearningModule.addToParameterHolder(p);
    }

}
