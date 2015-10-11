package org.rlcommunity.btanner.agents;

import java.util.Vector;
import org.rlcommunity.btanner.agentLib.actionSelectors.ActionSelectorInterface;
import org.rlcommunity.btanner.agentLib.learningBoosters.AbstractLearningBoosterFactory;
import org.rlcommunity.btanner.agentLib.learningBoosters.LearningBoosterInterface;
import org.rlcommunity.btanner.agentLib.learningModules.LearningModuleInterface;
import org.rlcommunity.btanner.agentLib.learningModules.AbstractLearningModuleFactory;
import org.rlcommunity.btanner.agentLib.rewardModifiers.AbstractRewardModifierFactory;
import org.rlcommunity.btanner.agentLib.rewardModifiers.RewardModifierInterface;
import org.rlcommunity.btanner.agentLib.rewardModifiers.nullRewardModifier.NullRewardModifierFactory;
import rlVizLib.general.ParameterHolder;
import rlVizLib.messaging.NotAnRLVizMessageException;
import rlVizLib.messaging.agent.AgentMessageParser;
import rlVizLib.messaging.agent.AgentMessages;
import rlVizLib.visualization.QueryableAgent;
import org.rlcommunity.rlglue.codec.AgentInterface;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpec;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;

public abstract class AbstractSarsa implements AgentInterface, QueryableAgent {

    protected int actionCount;
    protected LearningModuleInterface theLearningModule = null;
    protected Vector<LearningBoosterInterface> theBoosters = null;
    protected ActionSelectorInterface theActionSelector = null;
    protected RewardModifierInterface theRewardModifier = null;
    protected TaskSpec theTaskObject = null;
    protected ParameterHolder theParamHolder;

    public static ParameterHolder getDefaultParameters() {
        ParameterHolder p = new ParameterHolder();
        return p;
    }

    protected abstract AbstractLearningModuleFactory makeLearningModuleFactory();

    protected abstract Vector<AbstractLearningBoosterFactory> makeBoosterFactories();

    /**
     * Override this if yoi want to have reward modifiers.
     * @return
     */
    protected AbstractRewardModifierFactory makeRewardModifierFactory() {
        return new NullRewardModifierFactory();
    }

    public static ParameterHolder getDefaultParameters(AbstractLearningModuleFactory LMF) {
        ParameterHolder p = getDefaultParameters();
        LMF.addToParameterHolder(p);
        return p;
    }

    public AbstractSarsa() {
        this(AbstractSarsa.getDefaultParameters());
    }

    public AbstractSarsa(ParameterHolder p) {
        super();
        this.theParamHolder = p;
    }

    /**
     * It is assumed that this  method will be overridden and that the overrider
     * will call super with theTaskSpec and then setup their own stuff
     * like learning module
     * @param theTaskSpec
     */
    public void agent_init(String theTaskSpec) {
        theBoosters = new Vector<LearningBoosterInterface>();
        theTaskObject = new TaskSpec(theTaskSpec);

        actionCount = 1 + theTaskObject.getDiscreteActionRange(0).getMax() - theTaskObject.getDiscreteActionRange(0).getMin();
        assert (actionCount > 0);

        //here we're asserting there IS only one discrete action variable. 
        assert (theTaskObject.getNumDiscreteActionDims() == 1); //check the number of discrete actions is only 1
        assert (theTaskObject.getNumContinuousActionDims() == 0); //check that there is no continuous actions

        AbstractLearningModuleFactory theLMF = makeLearningModuleFactory();
        theActionSelector = theLMF.makeActionSelector(theTaskObject, theParamHolder);
        theLearningModule = theLMF.makeLearningModule(theTaskObject, theParamHolder);

        Vector<AbstractLearningBoosterFactory> theBoosterFactories = makeBoosterFactories();

        for (AbstractLearningBoosterFactory thisBoosterFactory : theBoosterFactories) {
            theBoosters.add(thisBoosterFactory.makeLearningBooster(theTaskObject, theParamHolder, theLearningModule, theActionSelector));
        }

        AbstractRewardModifierFactory theRewardModifierFactory = makeRewardModifierFactory();
        theRewardModifier = theRewardModifierFactory.makeRewardModifier(theTaskObject, theParamHolder);
    }

    public Action agent_start(Observation theObservation) {
        int thePrimitiveAction = chooseAction(theObservation);
        theLearningModule.start(theObservation, thePrimitiveAction);
        for (LearningBoosterInterface thisBooster : theBoosters) {
            thisBooster.start(theObservation, thePrimitiveAction);
        }

        theRewardModifier.start(theObservation);

        return makeAction(thePrimitiveAction);
    }

    public Action agent_step(double originalReward, Observation theObservation) {
        int thePrimitiveAction = chooseAction(theObservation);
        double modifiedReward = theRewardModifier.step(theObservation, originalReward);

        theLearningModule.step(theObservation, modifiedReward, thePrimitiveAction);
        for (LearningBoosterInterface thisBooster : theBoosters) {
            thisBooster.step(theObservation, modifiedReward, thePrimitiveAction);
        }
        return makeAction(thePrimitiveAction);
    }

    public void agent_end(double originalReward) {
        double modifiedReward = theRewardModifier.end(originalReward);
        theLearningModule.end(modifiedReward);
        for (LearningBoosterInterface thisBooster : theBoosters) {
            thisBooster.end(modifiedReward);
        }
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
        }else{
            if(theMessageObject.getPayLoad().startsWith("GOALHINT")){
                theRewardModifier.message(theMessageObject.getPayLoad());
                return "Nothing really to respond with.";
            }
        }

        System.out.println(getClass() + " :: Unhandled Message :: " + theMessageObject);
        return null;
    }

    protected int chooseAction(Observation theObservation) {
        return theActionSelector.sampleAction(theObservation, theLearningModule);
    }

    public void agent_cleanup() {
        if (theActionSelector != null) {
            theActionSelector.cleanup();
            this.theActionSelector = null;
        }
        /*Should probably write a cleanup method for boosters*/
        if (theBoosters != null) {
            for (LearningBoosterInterface thisLearningBooster : theBoosters) {
                thisLearningBooster.cleanup();
            }

            theBoosters.clear();
            theBoosters = null;
        }
        if (theLearningModule != null) {
            theLearningModule.cleanup();
            theLearningModule = null;
        }

        if (theRewardModifier != null) {
            theRewardModifier.cleanup();
            theRewardModifier = null;
        }

    }

    private Action makeAction(int theNormalizeAction) {
        Action action = new Action(1, 0);/* The Action constructor takes two arguements: 1) the size of the int array 2) the size of the double array*/
        action.intArray[0] = theNormalizeAction - theTaskObject.getDiscreteActionRange(0).getMin(); /*Set the action value*/
        return action;
    }

    public double getValueForStateAction(Observation theObservation, int whichAction) {
        //this could be called before init if things aren't synchronized
        if (theLearningModule == null) {
            return 0.0d;
        }
        return theLearningModule.queryNoSideEffect(theObservation, whichAction);
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
