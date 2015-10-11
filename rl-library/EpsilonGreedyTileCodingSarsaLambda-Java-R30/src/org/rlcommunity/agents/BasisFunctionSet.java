/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.rlcommunity.agents;

import org.rlcommunity.rlglue.codec.types.Observation;

/**
 *
 * @author btanner
 */
public interface BasisFunctionSet {
    public int getNumFeatures();
    public double[] calculateFeatures(Observation theState, int theAction);
}
