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
package org.rlcommunity.btanner.agentLib.learningBoosters;

import org.rlcommunity.btanner.agentLib.learningModules.*;
import org.rlcommunity.btanner.agentLib.actionSelectors.ActionSelectorInterface;
import org.rlcommunity.btanner.agentLib.learningModules.LearningModuleInterface;
import rlVizLib.general.ParameterHolder;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpec;

/**
 *
 * @author btanner
 */
public abstract class AbstractLearningBoosterFactory {
    protected LearningModuleInterface theLearningModule;
    protected ActionSelectorInterface theActionSelector;

    public AbstractLearningBoosterFactory() {
        this(null,null);
        
    }
    public AbstractLearningBoosterFactory(LearningModuleInterface theLearningModule, ActionSelectorInterface theActionSelector) {
        this.theLearningModule = theLearningModule;
        this.theActionSelector = theActionSelector;
    }
    abstract public LearningBoosterInterface makeLearningBooster(TaskSpec theTaskObject, ParameterHolder p, LearningModuleInterface LearningModule, ActionSelectorInterface theActionSelector);

    abstract protected void addToPH(ParameterHolder p);

    public void addToParameterHolder(ParameterHolder p) {
        addToPH(p);
    }
}
