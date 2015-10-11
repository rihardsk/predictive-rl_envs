package org.rlcommunity.btanner.agentLib.learningModules.sarsaLambda;

import org.rlcommunity.btanner.agentLib.learningModules.LearningModuleInterface;
import java.util.Vector;

import org.rlcommunity.btanner.agentLib.functionApproximators.CMAC.FeatureBasedFunctionApproximatorInterface;
import org.rlcommunity.btanner.agentLib.functionApproximators.FunctionApproximatorFactoryInterface;
import org.rlcommunity.btanner.agentLib.functionApproximators.FunctionApproximatorInterface;
import org.rlcommunity.btanner.agentLib.normalizers.ObservationScalerInterface;
import org.rlcommunity.btanner.agentLib.normalizers.TaskSpecObservationNormalizer;
import rlVizLib.general.ParameterHolder;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpec;
import org.rlcommunity.rlglue.codec.types.Observation;

/**
 * Don't use Lambda=1.0 - if you do no traces will ever decay far enought to be
 * culled and memory will fill up.
 *
 * If you use lambda=1.0, we will change it to 1.0d-1.0e-15.
 * 
 * @author btanner
 */
public class SarsaLambdaLearningModule implements LearningModuleInterface {

    public static void addToParameterHolder(ParameterHolder p) {
        p.addDoubleParam("sarsalambda-alpha", .25);
        p.addDoubleParam("sarsalambda-lambda", .25);
    }
    int lastAction;
    Observation lastObservation;
    Vector<FeatureBasedFunctionApproximatorInterface> theFAVector = null;
    ObservationScalerInterface theObsScaler = null;
    double alpha = .1d;
    double lambda = .1d;
    double gamma = 1.0d;
    final static double MAX_LAMBDA=1.0d-1.0e-15;

    /*trace code variables*/
    int maxNonZeroTraces;

    //Lets hope that this is a linearized multi-dimensional matrix
    //IE: It is spit into separate areas for each action
    double traces[];
    /*trace code requirements*/
    int nonZeroTraces[];
    int numNonZeroTraces;
    int nonZeroTracesInverse[];
    double minimumTrace;
    int tempFeatures[][];								// sets of features, one set per action
    double tempQ[];

    public SarsaLambdaLearningModule(TaskSpec theTaskObject, ParameterHolder p, FunctionApproximatorFactoryInterface FAF) {
        this.alpha = p.getDoubleParam("sarsalambda-alpha");
        this.lambda=Math.min(p.getDoubleParam("sarsalambda-lambda"), MAX_LAMBDA);

        int actionCount = 1 + theTaskObject.getDiscreteActionRange(0).getMax() - theTaskObject.getDiscreteActionRange(0).getMin();
        assert (actionCount > 0);

        //here we're asserting there IS only one discrete action variable. 
        assert (theTaskObject.getNumDiscreteActionDims() == 1); //check the number of discrete actions is only 1
        assert (theTaskObject.getNumContinuousActionDims() == 0); //check that there is no continuous actions
        

        theObsScaler = new TaskSpecObservationNormalizer(theTaskObject);

        //One FA per action
        theFAVector = new Vector<FeatureBasedFunctionApproximatorInterface>();


        //Todo: Fix so this isn't an unsafe cast
        for (int i = 0; i < actionCount; i++) {
            theFAVector.add((FeatureBasedFunctionApproximatorInterface) FAF.makeFunctionApproximator(theTaskObject, p));
        }

        int numActiveFeatures = theFAVector.firstElement().getActiveFeatureCount();
        this.tempFeatures = new int[actionCount][numActiveFeatures];

        /* Setup Traces Stuff, Copied from GenericSarsaLambda */
        this.maxNonZeroTraces = 500000;
        this.tempQ = null;
        /*Set up the variables which are consistent in all the constructors*/

        nonZeroTraces = new int[maxNonZeroTraces];
        numNonZeroTraces = 0;
        //For now, we'll assume that all the function approximator (for each action) have the same memory size
        traces = new double[actionCount * theFAVector.get(0).getMemorySize()];
        nonZeroTracesInverse = new int[traces.length];
        minimumTrace = 0.01;

        clearTraces();
    }

    private void clearTraces() {
        /*Initialize the traces being used for the value function*/
        for (int i = 0; i < traces.length; i++) {
            traces[i] = 0.0;                            // clear all traces
            nonZeroTracesInverse[i] = 0;
        }

        for (int i = 0; i < maxNonZeroTraces; i++) {
            nonZeroTraces[i] = 0;
        }
    }

    public void init() {
        clearTraces();
    }

    public void start(Observation theObservation, int theAction) {
        assert (theObsScaler != null);
        theObsScaler.notifyOfValues(theObservation);
        lastObservation = theObservation;
        lastAction = theAction;

        DecayTraces(0.0);  // clear all traces
    }

    private int linearize(int multiArrayIndex, int elementNumber) {
        return multiArrayIndex + elementNumber * theFAVector.get(0).getMemorySize();
    }

    private void clearTracesOtherThan(int actionToNotClear) {
        for (int a = 0; a < theFAVector.size(); a++) {
            if (a != actionToNotClear) {
                for (int j = 0; j < tempFeatures[a].length; j++) {
                    ClearTrace(linearize(tempFeatures[a][j], a));
                }
            }
        }
    }

    private void setTracesForAction(int whichAction) {
        for (int j = 0; j < tempFeatures[whichAction].length; j++) {
            SetTrace(linearize(tempFeatures[whichAction][j], whichAction), 1.0d);
        }
    }

    public void step(Observation theObservation, double r, int theAction) {
        assert (theObsScaler != null);
        theObsScaler.notifyOfValues(theObservation);



        DecayTraces(gamma * lambda);  // let traces fall
        loadF(tempFeatures, lastObservation);      // compute features
//        clearTracesOtherThan(lastAction);
        setTracesForAction(lastAction);
//        System.out.printf("The 3 values: %.2f %.2f %.2f\n",query(lastObservation, 0),query(lastObservation, 1),query(lastObservation, 2));

        //Original Sarsa code
        double lastValue = query(lastObservation, lastAction);
        double thisValue = query(theObservation, theAction);

        double target = r + thisValue;
        double delta = target - lastValue;

        update(delta);

        lastObservation = theObservation;
        lastAction = theAction;
    }

    public void end(double r) {
        DecayTraces(gamma * lambda);  // let traces fall
        loadF(tempFeatures, lastObservation);      // compute features
//        clearTracesOtherThan(lastAction);
        setTracesForAction(lastAction);

        double target = r;
        double lastValue = query(lastObservation, lastAction);

        double delta = target - lastValue;
        update(delta);
    }

    public void update(double delta) {
        double deltaToUse=delta*alpha;
        int numActiveFeatures = theFAVector.get(0).getMemorySize();
        
//        System.out.printf("Nonzero traces %d ",numNonZeroTraces);

        for (int i = 0; i < numNonZeroTraces; i++) // update theta (learn)
        {
            int f = nonZeroTraces[i];
            //f is a linear index into traces[] which has the traces for ALL actions
            //so we need to figure out which action it's really for to update the 
            //appropriate function approximator

            int whichAction = f / numActiveFeatures;
            int whichFeature = f % numActiveFeatures;

//            System.out.printf("%d [%d] -> (%.2f,%.2f) ",whichFeature,whichAction,deltaToUse,traces[f]);


            theFAVector.get(whichAction).incrementFeatureByAmount(whichFeature, deltaToUse*traces[f]);
        }
     
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
        return theFAVector.get(theAction).query(scaledObservation, uniqueId);
    }

    public double query(Observation theObservation, int theAction) {
        return query(theObservation, theAction, -1);
    }

    /*
     * 
     * 
     * Traces Support Code Below
     * 
     * 
     */
    private void ClearTrace(int f) // Clear any trace for feature f
    {
        if (!(traces[f] == 0.0)) {
            ClearExistentTrace(f, nonZeroTracesInverse[f]);
        }
    }

    private void loadF(int F[][], Observation theObservation) {
        assert (theObsScaler != null);
        Observation scaledObservation = theObsScaler.scaleObservation(theObservation);

        for (int a = 0; a < theFAVector.size(); a++) {
            theFAVector.get(a).setActiveFeatures(scaledObservation, F[a]);
        }
    }

    private void SetTrace(int f, double new_trace_value) // Manually Set the trace for feature f to the given value, which must be positive
    {
        if (traces[f] >= minimumTrace) {
            traces[f] = new_trace_value;
        } // trace already exists
        else {
            while (numNonZeroTraces >= maxNonZeroTraces) {
                IncreaseMinTrace();
            } // ensure room for new trace
            traces[f] = new_trace_value;
            nonZeroTraces[numNonZeroTraces] = f;
            nonZeroTracesInverse[f] = numNonZeroTraces;
            numNonZeroTraces++;
        }
    }

    private void IncreaseMinTrace() // Try to make room for more traces by incrementing minimum_trace by 10%, 
    // culling any traces that fall below the new minimum
    {
        minimumTrace += 0.1 * minimumTrace;
        for (int loc = numNonZeroTraces - 1; loc >= 0; loc--) // necessary to loop downwards
        {
            int f = nonZeroTraces[loc];
            if (traces[f] < minimumTrace) {
                ClearExistentTrace(f, loc);
            }
        }
    }

    private void DecayTraces(double decay_rate) // Decays all the (nonzero) traces by decay_rate, removing those below minimum_trace
    {
        for (int loc = numNonZeroTraces - 1; loc >= 0; loc--) // necessary to loop downwards
        {
            int f = nonZeroTraces[loc];
            traces[f] *= decay_rate;
            if (traces[f] < minimumTrace) {
                ClearExistentTrace(f, loc);
            }
        }
    }

    private void ClearExistentTrace(int f, int loc) // Clear the trace for feature f at location loc in the list of nonzero traces
    {
        traces[f] = 0.0;
        numNonZeroTraces--;
        nonZeroTraces[loc] = nonZeroTraces[numNonZeroTraces];
        nonZeroTracesInverse[nonZeroTraces[loc]] = loc;
    }

    public void update(Observation theObservation, int theAction, double delta) {
        assert (theObsScaler != null);
        Observation scaledObservation = theObsScaler.scaleObservation(theObservation);
        theFAVector.get(theAction).update(scaledObservation, alpha * delta);
    }

    public void cleanup() {
        this.nonZeroTraces=null;
        this.nonZeroTracesInverse=null;
        this.tempFeatures=null;
        this.tempQ=null;
        this.traces=null;
        lastObservation=null;
        if (theFAVector != null) {
            for (FunctionApproximatorInterface thisFA : theFAVector) {
                thisFA.cleanup();
            }
            theFAVector.clear();
            theFAVector = null;
        }
        theObsScaler = null;
    }
}
