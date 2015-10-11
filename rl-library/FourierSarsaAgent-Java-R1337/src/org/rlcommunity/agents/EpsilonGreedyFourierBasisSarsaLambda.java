package org.rlcommunity.agents;

/* Copyright 2009 George Konidaris
 * http://www-all.cs.umass.edu/~gdk/
 * gdk@cs.umass.edu
 * 
 * Based on Brian Tanner's EpsilonGreedyTileCodingSarsaLambda source code.
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

import java.net.URL;

import org.rlcommunity.gdk.fa.fourierbasis.*;
import org.rlcommunity.gdk.learning.*;

import org.rlcommunity.rlglue.codec.*;
import org.rlcommunity.rlglue.codec.util.*;
import org.rlcommunity.rlglue.codec.types.*;
import org.rlcommunity.rlglue.codec.taskspec.*;

import rlVizLib.general.ParameterHolder;
import rlVizLib.messaging.NotAnRLVizMessageException;
import rlVizLib.messaging.agent.AgentMessageParser;
import rlVizLib.messaging.agent.AgentMessages;
import rlVizLib.messaging.agentShell.TaskSpecResponsePayload;
import rlVizLib.messaging.interfaces.HasImageInterface;
import rlVizLib.visualization.QueryableAgent;

/**
 * RL-Glue driver class for an epsilon-greedy sarsa-lambda agent using the Fourier Basis.
 * 
 * @author George Konidaris (gdk at cs dot umass dot edu)
 */
public class EpsilonGreedyFourierBasisSarsaLambda implements AgentInterface, HasImageInterface,QueryableAgent 
{
    protected int actionCount = -1;
    protected SarsaLambdaFA learner = null;
    protected TaskSpec theTaskObject = null;
    protected ParameterHolder theParamHolder = null;
    
    protected Observation lastObservation = null;
    protected int lastAction = -1;

    /**
     * Creates an agent with the default parameters.
     * 
     * @see getDefaultParameters
     */
    public EpsilonGreedyFourierBasisSarsaLambda() 
    {
        this(getDefaultParameters());
    }

    /**
     * Creates an agent with given parameters.
     * 
     * @param p		a ParameterHolder object containing the given parameters.
     * Required parameters are:
     *  alpha
     *  gamma
     *  lambda
     *  epsilon
     *  auto-alpha
     *  fourier-order
     */
    public EpsilonGreedyFourierBasisSarsaLambda(ParameterHolder p) 
    {
        super();
        this.theParamHolder = p;
    }

    /**
     * Return a ParameterHolder object with the agent's default parameter settings.
     *
     * @return a default ParameterHolder object.
     */
    public static ParameterHolder getDefaultParameters() 
    {
        ParameterHolder p = new ParameterHolder();
     
        p.addDoubleParam("alpha", 1.0);
        p.addDoubleParam("gamma", 1.0);
        p.addDoubleParam("lambda", 0.9);
        p.addDoubleParam("epsilon", 0.01);
        
	p.addBooleanParam("auto-alpha", true);

        p.addIntegerParam("fourier-order", 5);
        
        return p;
    }
    
    /**
     * Initialize the agent (extract parameters and create the learner).
     * 
     * @param theTaskSpec the task specification.
     */
    public void agent_init(String theTaskSpec) 
    {
    	// Extract and save the task spec object
        theTaskObject = new TaskSpec(theTaskSpec);

        // Extract and save the number of actions
        actionCount = 1 + theTaskObject.getDiscreteActionRange(0).getMax() - theTaskObject.getDiscreteActionRange(0).getMin();
        assert (actionCount > 0);

        // Ensure that there is only one discrete action available
        assert (theTaskObject.getNumDiscreteActionDims() == 1); //check the number of discrete actions is only 1
        assert (theTaskObject.getNumContinuousActionDims() == 0); //check that there are no continuous actions

        // Extract parameters
        double alpha = theParamHolder.getDoubleParam("alpha");
        double lambda = theParamHolder.getDoubleParam("lambda");
        double gamma = theParamHolder.getDoubleParam("gamma");
        double epsilon = theParamHolder.getDoubleParam("epsilon");
     
	boolean auto_alpha = theParamHolder.getBooleanParam("auto-alpha");

        int fourier_order = theParamHolder.getIntegerParam("fourier-order");
     
        // Create function approximator and learner
        FourierFA fa = new FourierFA(theTaskObject, fourier_order);
        learner = new SarsaLambdaFA(actionCount, fa, alpha, gamma, lambda, epsilon, auto_alpha);
    }

    /**
     * Begins the episode.
     * 
     * @param theObservation 	the first state observation.
     */
    public Action agent_start(Observation theObservation) 
    {
    	// Obtain an action from the learner, save state and action for update.
        lastAction = chooseAction(theObservation);
        lastObservation = theObservation;

        // Return the selected action
        return makeAction(lastAction);
    }

    /**
     * Process an action selection step. The agent uses stored values from its
     * previous state and action to perform a learning update. 
     * 
     * @param reward		the reward obtained during the just-executed step.
     * @param theObservation	the new state observation.
     */
    public Action agent_step(double reward, Observation theObservation) 
    {
    	// Obtain the next agent, perform the update now that world cycle complete.
        int theAction = chooseAction(theObservation);
  
        learner.update(lastObservation, lastAction, theObservation, theAction, reward);

        // Save the current state and action for next time.
        lastAction = theAction;
        lastObservation = theObservation;
        
        // Return the selected action
        return makeAction(theAction);
    }

    /**
     * The final step in an episode.
     * 
     * @param reward		the final reward. 
     */
    public void agent_end(double reward) 
    {
    	// Update with a null next state and clear traces.
    	learner.update(lastObservation, lastAction, null, -1, reward);
    	learner.clearTraces();    

    	// Clear saved variables.
    	lastObservation = null;
    	lastAction = -1;    	
    }

    /**
     * Process an incoming message.
     * 
     * @theMessage		incoming message text.
     */
    public String agent_message(String theMessage) 
    {
    	AgentMessages theMessageObject;
        
    	try 
    	{
            theMessageObject = AgentMessageParser.parseMessage(theMessage);
        } 
    	catch (NotAnRLVizMessageException e) 
        {
            System.err.println("Someone sent " + getClass() + " a message that wasn't RL-Viz compatible");
            return "I only respond to RL-Viz messages!";
        }

        if (theMessageObject.canHandleAutomatically(this)) 
        {
            return theMessageObject.handleAutomatically(this);
        }

        System.out.println(getClass() + " :: Unhandled Message :: " + theMessageObject);
        return null;
    }

    /**
     * Action selection.
     * 
     * @param theObservation	the state observation.
     * @return	the action selected.
     */
    protected int chooseAction(Observation theObservation) 
    {
        return learner.nextMove(theObservation);
    }

    /**
     * Cleanup.
     */
    public void agent_cleanup() 
    {
        learner = null;
        theTaskObject = null;
    }

    /**
     * Determines whether this agent is compatible with a task and parameter set. 
     * 
     * @param P		the parameter set.
     * @param TaskSpecString	the task specification.
     * @return	a response.
     */
    public static TaskSpecResponsePayload isCompatible(ParameterHolder P, String TaskSpecString) 
    {
        TaskSpec theTSO = new TaskSpec(TaskSpecString);
        
        if (theTSO.getNumContinuousActionDims() > 0) 
        {
            return new TaskSpecResponsePayload(true, "This agent does not support continuous actions.");
        }
        if (theTSO.getNumDiscreteObsDims() > 0) 
        {
            return new TaskSpecResponsePayload(true, "This agent does not support discrete observations.");
        }
        return new TaskSpecResponsePayload(false, "");
    }

    /**
     * Normalizes an action - internally we consider integer actions in a range
     * starting at zero, but some environments start at some other number.
     * This function simply adjusts the output range of a given integer action
     * and places it in an Action object.
     * 
     * @param theNormalizeAction	the action (starting at zero).
     * @return	the normalized Action object.
     */
    private Action makeAction(int theNormalizeAction) 
    {
    	// A single integer (and no doubles).
        Action action = new Action(1, 0);
        
        // Normalize back to the appropriate range.
        action.intArray[0] = theNormalizeAction - theTaskObject.getDiscreteActionRange(0).getMin(); 
        
        // Return
        return action;
    }

    /**
     * Return the value function for a given state.
     * 
     * @param theObservation  the state observation in question.
     * @return the value of theObservation.
     */
    public double getValueForState(Observation theObservation) 
    {
        // Make sure a learner exists.
        if (learner == null) 
        {
            return 0.0;
        }

        return learner.maxQ(theObservation);
    }

    /**
     * Main: create an EpsilonGreedyFourierBasisSarsaLambda object. 
     * 
     * @param args	command-line parameters (ignored).
     */
    public static void main(String[] args) 
    {
        AgentLoader L = new AgentLoader(new EpsilonGreedyFourierBasisSarsaLambda());
        L.run();
    }

    /**
     * Obtain the agent image.
     */
    public URL getImageURL() 
    {
        return getClass().getResource("/images/epsilongreedyfourierbasissarsalambda.png");
    }
}
