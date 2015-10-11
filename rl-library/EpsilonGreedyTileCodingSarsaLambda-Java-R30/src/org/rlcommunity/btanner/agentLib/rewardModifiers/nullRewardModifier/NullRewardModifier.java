package org.rlcommunity.btanner.agentLib.rewardModifiers.nullRewardModifier;


import org.rlcommunity.btanner.agentLib.rewardModifiers.RewardModifierInterface;
import rlVizLib.general.ParameterHolder;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpec;
import org.rlcommunity.rlglue.codec.types.Observation;

/**
 * This class will generate potential function rewards from s,s' using the difference
 * between their CONTINUOUS-VALUED state variables only.
 *
 * The observations will NOT be scaled to be in [0,1].  The actual Euclidean distance is used
 * to calculate the difference between subsequent observations.
 *
 * Unlike the Potential Function Scaling used by Marc Bellemare in his PotentialFuncContinuousGridWorld,
 * we will multiply the distance by the scaling factor, not divide by it.
 *
 * This is nice because it means if we set the scaling factor to 0, we have no scaling.
 * @author btanner
 */
public final class NullRewardModifier implements RewardModifierInterface {

    public static void addToParameterHolder(ParameterHolder p) {
    }

    public NullRewardModifier(TaskSpec theTaskObject, ParameterHolder p) {

    }

    public void init() {

    }

    public void start(Observation theObservation) {
    }

    public double step(Observation theObservation, double r) {
    return r;
   }

    public double end(double r) {
        return r;
    }

    public void cleanup() {
    }

    public void message(String payLoad) {
    }
}
