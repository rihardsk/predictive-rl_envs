package org.rlcommunity.btanner.agentLib.rewardModifiers.potentialFunction;

import java.util.StringTokenizer;
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
public class PotentialFunctionRewardModifier implements RewardModifierInterface {

    public static void addToParameterHolder(ParameterHolder p) {
        //Whatever the scale is, the change in distance will be multiplied by the scale
        p.addDoubleParam("potential-function-scale", 1.0d);
    }
    private double rewardScale = 1.0d;
    private Observation goalObservation = null;
    private double lastDistance = 0.0d;

    public PotentialFunctionRewardModifier(TaskSpec theTaskObject, ParameterHolder p) {
        this.rewardScale = p.getDoubleParam("potential-function-scale");

        String extraTaskSpecInfo = theTaskObject.getExtraString();

        StringTokenizer T = new StringTokenizer(extraTaskSpecInfo, ":");
        String currentToken = "";
        while (!currentToken.equals("GOALINFO") && T.hasMoreTokens()) {
            currentToken = T.nextToken();
        }

        if (currentToken.equals("GOALINFO")){
            //First the number of variables
            int numVars = Integer.parseInt(T.nextToken());
            goalObservation = new Observation(0, numVars, 0);
            for (int i = 0; i < numVars; i++) {
                goalObservation.setDouble(i, Double.parseDouble(T.nextToken()));
            }
        }
    }

    public void init() {
        // TODO Auto-generated method stub
        lastDistance = 0.0d;

    }

    public void start(Observation theObservation) {
        if(goalObservation==null){
            System.out.println("Potential Function agent in use, but not hint observation given in task spec or as message.");
        }
        lastDistance = calculateDistanceToGoal(theObservation);
    }

    public double step(Observation theObservation, double r) {
        double distance = calculateDistanceToGoal(theObservation);
        double extraReward = rewardScale * (lastDistance - distance);
        lastDistance = distance;

        return extraReward + r;
    }

    public double end(double r) {
        lastDistance = 0.0d;
        return r;
    }

    public void cleanup() {
        lastDistance = 0.0d;
        rewardScale = 1.0;
    }

    /**
     * The square root of the sum of the squared differences
     * @param theObservation
     * @param lastObservation
     * @return
     */
    private double calculateDistanceToGoal(Observation theObservation) {
        assert theObservation != null;

        assert theObservation.doubleArray.length == goalObservation.doubleArray.length;

        double sumOfSquares = 0.0d;

        for (int i = 0; i < theObservation.doubleArray.length; i++) {
            double diff = theObservation.doubleArray[i] - goalObservation.doubleArray[i];
            double square = diff * diff;

            sumOfSquares += square;
        }
        double distance = Math.sqrt(sumOfSquares);
        return distance;
    }

    public void message(String goalMessage) {
        StringTokenizer T = new StringTokenizer(goalMessage, ":");
        String currentToken = "";
        currentToken=T.nextToken();
        assert(currentToken.equals("GOALHINT"));
        //First the number of variables
        int numVars=Integer.parseInt(T.nextToken());
        goalObservation=new Observation(0,numVars,0);
        for(int i=0;i<numVars;i++){
            goalObservation.setDouble(i, Double.parseDouble(T.nextToken()));
        }
    }
}
