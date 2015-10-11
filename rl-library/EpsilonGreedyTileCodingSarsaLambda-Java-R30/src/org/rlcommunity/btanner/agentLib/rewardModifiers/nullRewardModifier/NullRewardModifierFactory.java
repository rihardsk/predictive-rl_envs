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

package org.rlcommunity.btanner.agentLib.rewardModifiers.nullRewardModifier;

import org.rlcommunity.btanner.agentLib.rewardModifiers.AbstractRewardModifierFactory;
import org.rlcommunity.btanner.agentLib.rewardModifiers.RewardModifierInterface;
import rlVizLib.general.ParameterHolder;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpec;

/**
 * This is quite simple.
 * @author Brian Tanner
 */
public class NullRewardModifierFactory extends AbstractRewardModifierFactory {
    public NullRewardModifierFactory(){
        super();
    }
    @Override
    public RewardModifierInterface makeRewardModifier(TaskSpec theTaskObject, ParameterHolder p) {
        return new NullRewardModifier(theTaskObject, p);
    }

}
