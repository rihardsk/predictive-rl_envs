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
package org.rlcommunity.btanner.agentLib.rewardModifiers;

import org.rlcommunity.rlglue.codec.types.Observation;

/**
 *  This could be used for things like potential-based shaping.  This module keeps
 *  track of observations you have seen and generates rewards.
 *
 * This is a state to state reward modifier.  It does not take the action or
 * action rewards into effect.  That might be a mistake.
 * @author btanner
 */
public interface RewardModifierInterface {
    /**
     * Clean up your data structures.
     */
    public abstract void cleanup();

    public abstract void init();

    public void message(String payLoad);

    /**
     * Give the modified reward from start.
     * @param theObservation
     * @param theAction
     */
    public abstract void start(Observation theObservation);

    /**
     * Give the modified reward for step.
     * @param theObservation
     * @param theAction
     * @return
     */
    public abstract double step(Observation theObservation, double r);

    /**
     * Give the modified reward for end
     * @param r
     */public abstract double end(double r);
}
