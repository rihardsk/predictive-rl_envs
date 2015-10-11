/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rlcommunity.btanner.agentLib.dataStructures;

import org.rlcommunity.rlglue.codec.types.Observation;

/**
 *
 * @author Brian Tanner
 */
public class DataPoint {

    private final Observation s;
    private final Observation sprime;
    private final int action;
    private final double reward;

    public DataPoint(Observation s, int action, double reward, Observation sprime) {
        this.s = s;
        this.action = action;
        this.reward = reward;
        this.sprime = sprime;
    }

    public DataPoint(Observation s, int action, double reward) {
        this(s, action, reward, null);
    }


    public final boolean isAbsorbing(){
        return sprime==null;
    }

    public final Observation getS(){
        return s;
    }

    public final Observation getSPrime(){
        return sprime;
    }

    public final int getAction(){
        return action;
    }

    public final double getReward(){
        return reward;
    }

}
