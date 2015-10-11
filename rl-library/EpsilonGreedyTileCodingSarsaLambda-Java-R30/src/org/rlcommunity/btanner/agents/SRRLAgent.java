package org.rlcommunity.btanner.agents;

import org.rlcommunity.btanner.agentLib.actionSelectors.epsilonGreedy.EpsilonGreedyActionSelector;
import org.rlcommunity.btanner.agentLib.functionApproximators.CMAC.CMACFunctionApproximatorFactory;
import org.rlcommunity.btanner.agentLib.functionApproximators.CMAC.NDCMACTileCoder;
import org.rlcommunity.btanner.agentLib.learningBoosters.experienceReplay.ExpectedExperienceReplayLambdaLearningBooster;
import org.rlcommunity.btanner.agentLib.learningModules.sarsaLambda.SarsaLambdaLearningModule;
import rlVizLib.general.ParameterHolder;
import rlVizLib.messaging.NotAnRLVizMessageException;
import rlVizLib.messaging.agent.AgentMessageParser;
import rlVizLib.messaging.agent.AgentMessages;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpec;
import rlVizLib.visualization.QueryableAgent;
import org.rlcommunity.rlglue.codec.AgentInterface;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;

public class SRRLAgent implements AgentInterface, QueryableAgent {

    protected int actionCount;
    protected SarsaLambdaLearningModule theLearningModule = null;
    protected ExpectedExperienceReplayLambdaLearningBooster theBooster = null;
    protected EpsilonGreedyActionSelector theActionSelector = null;
    protected TaskSpec theTaskObject = null;
    protected ParameterHolder theParamHolder;

    public static ParameterHolder getDefaultParameters() {
        ParameterHolder p = new ParameterHolder();
        return p;
    }

    public SRRLAgent() {
        this(SRRLAgent.getDefaultParameters());
    }

    public SRRLAgent(ParameterHolder p) {
        super();
        this.theParamHolder = p;
        SarsaLambdaLearningModule.addToParameterHolder(p);
        ExpectedExperienceReplayLambdaLearningBooster.addToParameterHolder(p);
        EpsilonGreedyActionSelector.addToParameterHolder(p);
        NDCMACTileCoder.addToParameterHolder(p);    

        theParamHolder.setDoubleParam("sarsalambda-alpha", .125d);
        theParamHolder.setDoubleParam("sarsalambda-lambda", 0.0d);
        theParamHolder.setIntegerParam("e-replay-lesson-size", 16);
        theParamHolder.setIntegerParam("e-replay-replay-count", 64);
        theParamHolder.setDoubleParam("e-replay-lambda", 0.9d);
    }

    /**
     * It is assumed that this  method will be overridden and that the overrider
     * will call super with theTaskSpec and then setup their own stuff
     * like learning module
     * @param theTaskSpec
     */
    public void agent_init(String theTaskSpec) {
        theTaskObject = new TaskSpec(theTaskSpec);

        actionCount = 1 + theTaskObject.getDiscreteActionRange(0).getMax() - theTaskObject.getDiscreteActionRange(0).getMin();
        assert (actionCount > 0);

        //here we're asserting there IS only one discrete action variable. 
        assert (theTaskObject.getNumDiscreteActionDims() == 1); //check the number of discrete actions is only 1
        assert (theTaskObject.getNumContinuousActionDims() == 0); //check that there is no continuous actions
        

        theActionSelector = new EpsilonGreedyActionSelector(theTaskObject, theParamHolder);
        theLearningModule = new SarsaLambdaLearningModule(theTaskObject, theParamHolder, new CMACFunctionApproximatorFactory());
        theBooster = new ExpectedExperienceReplayLambdaLearningBooster(theTaskObject, theParamHolder, theLearningModule, theActionSelector);
    }

    public Action agent_start(Observation theObservation) {
        int theAction = chooseAction(theObservation);
        theLearningModule.start(theObservation, theAction);
        theBooster.start(theObservation, theAction);

        return makeAction(theAction);
    }

    public Action agent_step(double reward, Observation theObservation) {
        int theAction = chooseAction(theObservation);
        theLearningModule.step(theObservation, reward, theAction);
        theBooster.step(theObservation, reward, theAction);
        return makeAction(theAction);
    }

    public void agent_end(double reward) {
        theLearningModule.end(reward);
        theBooster.end(reward);
    }

    public String agent_message(String theMessage) {
        AgentMessages theMessageObject;
        try {
            theMessageObject = AgentMessageParser.parseMessage(theMessage);
        } catch (NotAnRLVizMessageException e) {
            System.err.println("Someone sent " + getClass() + " a message that wasn't RL-Viz compatible");
            return "I only respond to RL-Viz messages!";
        }

        if (theMessageObject.canHandleAutomatically(this)) {
            return theMessageObject.handleAutomatically(this);
        }

        System.out.println(getClass() + " :: Unhandled Message :: " + theMessageObject);
        return null;
    }

    protected int chooseAction(Observation theObservation) {
        return theActionSelector.sampleAction(theObservation, theLearningModule);
    }

    public void agent_cleanup() {
    }

    public void agent_freeze() {
    }

    private Action makeAction(int theNormalizeAction) {
        Action action = new Action(1, 0);/* The Action constructor takes two arguements: 1) the size of the int array 2) the size of the double array*/
        action.intArray[0] = theNormalizeAction - theTaskObject.getDiscreteActionRange(0).getMin(); /*Set the action value*/        
        return action;
    }

    public double getValueForState(Observation theObservation) {
        //this could be called before init if things aren't synchronized
        if (theLearningModule == null) {
            return 0.0d;
        }

        double[] probabilities = theActionSelector.getActionProbabilities(theObservation, theLearningModule);
        double totalValue = 0.0d;
        for (int a = 0; a < actionCount; a++) {
            double thisActionValue = theLearningModule.queryNoSideEffect(theObservation, a);
            totalValue += probabilities[a] * thisActionValue;
        }
        return totalValue;
    }
}
