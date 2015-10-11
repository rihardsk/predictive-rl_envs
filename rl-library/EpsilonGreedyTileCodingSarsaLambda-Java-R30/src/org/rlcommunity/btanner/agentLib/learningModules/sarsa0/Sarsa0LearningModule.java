package org.rlcommunity.btanner.agentLib.learningModules.sarsa0;

import org.rlcommunity.btanner.agentLib.learningModules.LearningModuleInterface;
import org.rlcommunity.btanner.agentLib.functionApproximators.FunctionApproximatorInterface;
import java.util.Vector;

import org.rlcommunity.btanner.agentLib.functionApproximators.FunctionApproximatorFactoryInterface;
import org.rlcommunity.btanner.agentLib.normalizers.ObservationScalerInterface;
import org.rlcommunity.btanner.agentLib.normalizers.TaskSpecObservationNormalizer;
import rlVizLib.general.ParameterHolder;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpec;
import org.rlcommunity.rlglue.codec.types.Observation;

public class Sarsa0LearningModule implements LearningModuleInterface {

    public static void addToParameterHolder(ParameterHolder p) {
        p.addDoubleParam("sarsa0-alpha", .25);
    }
    int lastAction;
    Observation lastObservation=null;
    Vector<FunctionApproximatorInterface> theFAVector=null;
    ObservationScalerInterface theObsScaler=null;
    double alpha = .1;

    public Sarsa0LearningModule(TaskSpec theTaskObject, ParameterHolder p, FunctionApproximatorFactoryInterface FAF) {
        this.alpha = p.getDoubleParam("sarsa0-alpha");

        int actionCount = 1 + theTaskObject.getDiscreteActionRange(0).getMax() - theTaskObject.getDiscreteActionRange(0).getMin();
        assert (actionCount > 0);

        //here we're asserting there IS only one discrete action variable. 
        assert (theTaskObject.getNumDiscreteActionDims() == 1); //check the number of discrete actions is only 1
        assert (theTaskObject.getNumContinuousActionDims() == 0); //check that there is no continuous actions
        

        theObsScaler = new TaskSpecObservationNormalizer(theTaskObject);

        //One FA per action
        theFAVector = new Vector<FunctionApproximatorInterface>();

        for (int i = 0; i < actionCount; i++) {
            theFAVector.add(FAF.makeFunctionApproximator(theTaskObject, p));
        }
    }

    public void init() {
    // TODO Auto-generated method stub

    }

    public void start(Observation theObservation, int theAction) {
        assert (theObsScaler != null);
        theObsScaler.notifyOfValues(theObservation);
        lastObservation = theObservation;
        lastAction = theAction;
    }

    public void step(Observation theObservation, double r, int theAction) {
        assert (theObsScaler != null);
        theObsScaler.notifyOfValues(theObservation);

        double lastValue = query(lastObservation, lastAction);
        double thisValue = query(theObservation, theAction);
        
//        System.out.printf("The 3 values: %.2f %.2f %.2f\n",query(lastObservation, 0),query(lastObservation, 1),query(lastObservation, 2));

        double target = r + thisValue;
        double delta = target - lastValue;

        update(lastObservation, lastAction, delta);
        lastObservation = theObservation;
        lastAction = theAction;
    }

    public void end(double r) {
        double target = r;
        double lastValue = query(lastObservation, lastAction);

        double delta = target - lastValue;
        update(lastObservation, lastAction, delta);
    }

    public void update(Observation theObservation, int theAction, double delta) {
        assert (theObsScaler != null);
        Observation scaledObservation = theObsScaler.scaleObservation(theObservation);
        theFAVector.get(theAction).update(scaledObservation, alpha * delta);
    }

    public double queryNoSideEffect(Observation theObservation, int theAction) {
        assert (theObsScaler != null);
        Observation scaledObservation = theObsScaler.scaleObservation(theObservation);
        return theFAVector.get(theAction).query(scaledObservation);
    }

    public double query(Observation theObservation, int theAction, long uniqueId) {
        assert (theObsScaler != null);
        theObsScaler.notifyOfValues(theObservation);
        Observation scaledObservation = theObsScaler.scaleObservation(theObservation);
        return theFAVector.get(theAction).query(scaledObservation,uniqueId);
    }

    public double query(Observation theObservation, int theAction) {
        return query(theObservation,theAction,-1);
    }

    public void cleanup() {
        if(theFAVector!=null){
            for (FunctionApproximatorInterface thisFA : theFAVector) {
                thisFA.cleanup();
            }
            theFAVector.clear();
            theFAVector=null;
        }
        theObsScaler=null;
    }
}
