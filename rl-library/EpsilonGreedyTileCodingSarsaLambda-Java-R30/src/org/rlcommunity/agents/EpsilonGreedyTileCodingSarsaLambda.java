/*
 * Copyright 2008 Brian Tanner
 * http://bt-recordbook.googlecode.com/
 * brian@tannerpages.com
 * http://brian.tannerpages.com
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.rlcommunity.agents;

import java.net.URL;
import org.rlcommunity.btanner.agentLib.actionSelectors.epsilonGreedy.EpsilonGreedyActionSelector;
import org.rlcommunity.btanner.agentLib.functionApproximators.CMAC.CMACFunctionApproximatorFactory;
import org.rlcommunity.btanner.agentLib.functionApproximators.CMAC.NDCMACTileCoder;
import org.rlcommunity.btanner.agentLib.learningModules.sarsaLambda.SarsaLambdaLearningModule;
import org.rlcommunity.rlglue.codec.AgentInterface;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;
import org.rlcommunity.rlglue.codec.util.AgentLoader;
import rlVizLib.general.ParameterHolder;
import rlVizLib.messaging.NotAnRLVizMessageException;
import rlVizLib.messaging.agent.AgentMessageParser;
import rlVizLib.messaging.agent.AgentMessages;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpec;
import rlVizLib.messaging.agentShell.TaskSpecResponsePayload;
import rlVizLib.messaging.interfaces.HasImageInterface;
import rlVizLib.visualization.QueryableAgent;

/**
 *
 * @author Brian Tanner
 */
public class EpsilonGreedyTileCodingSarsaLambda implements AgentInterface, HasImageInterface,QueryableAgent {

    protected int actionCount;
    protected SarsaLambdaLearningModule theLearningModule = null;
    protected EpsilonGreedyActionSelector theActionSelector = null;
    protected CMACFunctionApproximatorFactory theFAFactory = null;
    protected TaskSpec theTaskObject = null;
    protected ParameterHolder theParamHolder;

    public static ParameterHolder getDefaultParameters() {
        ParameterHolder p = new ParameterHolder();
        SarsaLambdaLearningModule.addToParameterHolder(p);
        NDCMACTileCoder.addToParameterHolder(p);
        EpsilonGreedyActionSelector.addToParameterHolder(p);
        return p;
    }

    public EpsilonGreedyTileCodingSarsaLambda() {
        this(EpsilonGreedyTileCodingSarsaLambda.getDefaultParameters());
    }

    public EpsilonGreedyTileCodingSarsaLambda(ParameterHolder p) {
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
        theTaskObject = new TaskSpec(theTaskSpec);

        actionCount = 1 + theTaskObject.getDiscreteActionRange(0).getMax() - theTaskObject.getDiscreteActionRange(0).getMin();
        assert (actionCount > 0);

        //here we're asserting there IS only one discrete action variable. 
        assert (theTaskObject.getNumDiscreteActionDims() == 1); //check the number of discrete actions is only 1
        assert (theTaskObject.getNumContinuousActionDims() == 0); //check that there is no continuous actions


        theLearningModule = new SarsaLambdaLearningModule(theTaskObject, theParamHolder, new CMACFunctionApproximatorFactory());
        theActionSelector = new EpsilonGreedyActionSelector(theTaskObject, theParamHolder);
    }

    public Action agent_start(Observation theObservation) {
        int theAction = chooseAction(theObservation);
        theLearningModule.start(theObservation, theAction);

        return makeAction(theAction);
    }

    public Action agent_step(double reward, Observation theObservation) {
        int theAction = chooseAction(theObservation);
        theLearningModule.step(theObservation, reward, theAction);

        return makeAction(theAction);
    }

    public void agent_end(double reward) {
        theLearningModule.end(reward);
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
        this.theActionSelector.cleanup();
        this.theLearningModule.cleanup();
        theActionSelector = null;
        theLearningModule = null;
        theFAFactory = null;
        theTaskObject = null;
    }

    public static TaskSpecResponsePayload isCompatible(ParameterHolder P, String TaskSpecString) {
        if(!TaskSpec.checkTaskSpec(TaskSpecString)){
            return new TaskSpecResponsePayload(true, "This agent does not support environments without standard task specs.");
        }

        TaskSpec theTSO = new TaskSpec(TaskSpecString);

        if (theTSO.getNumDiscreteActionDims() > 1) {
            return new TaskSpecResponsePayload(true, "This agent does not support continuous actions.");
        }
        if (theTSO.getNumContinuousActionDims() > 0) {
            return new TaskSpecResponsePayload(true, "This agent does not support continuous actions.");
        }
        if (theTSO.getNumDiscreteObsDims() > 0) {
            return new TaskSpecResponsePayload(true, "This agent does not support discrete observations.");
        }
        return new TaskSpecResponsePayload(false, "");
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

    public static void main(String[] args) {
        AgentLoader L = new AgentLoader(new EpsilonGreedyTileCodingSarsaLambda());
        L.run();
    }

    public URL getImageURL() {
        return getClass().getResource("/images/epsilongreedytilecodingsarsalambda.png");
    }
}
