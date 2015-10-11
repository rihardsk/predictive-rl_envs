package org.rlcommunity.gdk.learning;

/* Copyright 2009 George Konidaris
 * http://www-all.cs.umass.edu/~gdk/
 * gdk@cs.umass.edu
 * 
 *
 *  Significant modifications by Will Dabney, to include his Alpha Bounds
 *  method for automatically scaling alpha.  
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
 
import org.rlcommunity.gdk.fa.LinearFA;
import org.rlcommunity.rlglue.codec.types.Observation;

/**
 * Sarsa(lambda) with linear function approximation.
 * Action selection is epsilon-greedy.
 * 
 * @author George Konidaris (gdk at cs dot umass dot edu)
 */
public class SarsaLambdaFA
{

  /**
   * Create a new learning algorithm instance.
   * 
   * @param actions	the number of action available to the agent.
   * @param fa		a linear function approximator instance.
   * @param Alpha	learning rate.
   * @param Gamma	discount factor.
   * @param Lambda	lambda parameter.
   * @param Eps		action selection parameter.
   * @param Auto_Alpha  use Dabney's automatic alpha method.
   */
  public SarsaLambdaFA(int actions, LinearFA fa,
			double Alpha, double Gamma, double Lambda, double Eps,
			boolean Auto_Alpha)
    {
     // Copy parameters.
     alpha = Alpha;
     gamma = Gamma;
     eps = Eps;
     lambda = Lambda;
     
     use_alpha_bounds = Auto_Alpha;

     nactions = actions;
     
     // Create the necessary function approximators.
     FA = new LinearFA[nactions];
     
     for(int j = 0; j < nactions; j++)
    	 FA[j] = fa.duplicate();
     
     // Setup trace vector.
     setupTrace();
    }
    
    /**
     * Return the learner's internal function approximators. 
     * 
     * @return an array of linear function approximators. 
     */
    public LinearFA [] GetFAs()
     {
       	return FA;
     }
	
    /**
     * Create the trace vector. 
     */
    protected void setupTrace()
    { 
      traces = new double[nactions][FA[0].getNumBasisFunctions()];
      
      clearTraces();
    }
        
    /**
     * Epsilon-greedy action selection, with ties broken randomly.
     * 
     * @param s	the state at which the agent will act.
     * @return	the selected action.
     */
    public int nextMove(Observation s)
    { 
    	// Check for exploration step.
        if(Math.random() < eps)
        {
          int domov = (int)(Math.random() * nactions);
          if(domov == nactions) domov = nactions - 1;
          
          return domov;
        }
        
        double best = Double.NEGATIVE_INFINITY;
        int bcount = 1;
        int poss = 0;
        
        // Keep a record of actions with equal action-values,
        // for use in tie-breaking.
        boolean[] sel = new boolean[nactions];
        for(int l = 0; l < nactions; l++) sel[l] = false;
        
        // For each action ...
        for(int j = 0; j < nactions; j++)
        {
           double newmx = FA[j].valueAt(s);
               
           // If the action is the best so far ...
           if(newmx > best)
           {
               best = newmx;
               bcount = 1;
               poss = j;
               
               // Clear the selected list ...
               for(int l = 0; l < nactions; l++)
                   sel[l] = false;
               
               //  ... except for this action.
               sel[poss] = true;
           }
           // Otherwise, if it's a tie ...
           else if(newmx == best)
           {
        	   // Update the selected list.
               bcount++;
               sel[j] = true;
           }
        }
        
        // If there's only one best action return it.
        if(bcount == 1)
        {
            return poss;
        }
        // Otherwise ...
        else
        {
            // Select a random action number 
            int vpos = (int)(Math.random() * bcount);
            if(vpos == bcount) vpos = bcount - 1;
            
            // Return that action.
            for(int k = 0; k < nactions; k++)
            {
               if(sel[k])
               {
                   if(vpos == 0) return k;
                   else vpos--;
               }
            }
        }
        
        // This should never happen ...
        System.out.println("Action selection problem ... ");
        return nactions;
    }

    /**
     * Perform an update given a (s, a, r, s, a) tuple.
     * 
     * @param s		the start state.
     * @param action	the action.
     * @param sprime	the next state (null if we have reached the end of the episode).
     * @param nextact	the next action (-1 if we have reached the end of the episode).
     * @param r			the reward.
     */
    public void update(Observation s, int action, Observation sprime, int nextact, double r)
    {    
    	// Compute temporal difference error
        double delta = r - FA[action].valueAt(s);
        
        // If we're not at the end of the episode add in the value of
        // the next state.
        if((sprime != null) && (nextact != -1))
        {
        	delta += gamma*FA[nextact].valueAt(sprime);
        }
        
        // Check for divergence
        if(java.lang.Double.isNaN(delta))
        {
            System.out.println("Function approximation divergence (SarsaLambdaFA)");
            System.exit(1);
        }
        
        // Update each basis function
        for(int j = 0; j < nactions; j++)
        {
        	// First decay traces
            for(int k = 0; k < FA[j].getNumBasisFunctions(); k++)
            {
            	traces[j][k] = traces[j][k] * gamma*lambda;
            }
        	
            // Then add active set of basis functions to traces.
            if(j == action)
            {
            	double [] phi = FA[j].computeFeatures(s);
            	
            	for(int k = 0; k < FA[j].getNumBasisFunctions(); k++)
            	{
            		traces[j][k] += phi[k];
            	}
            }
	}

	// Alpha Bounds code (Dabney)
	if (use_alpha_bounds) 
	 {
	    double epsilon_alpha = 0.0;

	    if((sprime != null) && (nextact != -1)) 
		{
		double [] phi_tp = FA[nextact].computeFeatures(sprime);
		double [] phi_t = FA[action].computeFeatures(s);
		for (int k = 0; k < FA[action].getNumBasisFunctions(); k++) 
		    {
			epsilon_alpha += gamma * phi_tp[k] * traces[nextact][k] - phi_t[k] * traces[action][k];
		    }
		} 
	    else 
		{
		    double [] phi_t = FA[action].computeFeatures(s);
		    for (int k = 0; k < FA[action].getNumBasisFunctions(); k++) 
			{
			    epsilon_alpha += -1.0 * phi_t[k] * traces[action][k];
			}
		}

	    double new_alpha  = Math.abs(-1.0 / epsilon_alpha);

	    // Handle each of the possible situations
	    if (epsilon_alpha >= 0.0) 
		{ 
		    // epsilon_alpha == 0, therefore deltas are the same after the update...
		    // Doing nothing here: 
		    // There doesn't exist a scalar step-size that solves the equation and is greater than zero
		} 
	    else if (epsilon_alpha < 0.0) 
		{ 
		    // epsilon_alpha < 0, we have good bounds 
		    //	    if (alpha > new_alpha)
		    //System.out.println("Alpha bounded: " + alpha + " " + new_alpha + " " + epsilon_alpha);
		    alpha = Math.min(new_alpha, alpha);
		} 
	 }
	 // END of Alpha Bound code (Dabney)

	for(int j = 0; j < nactions; j++)
	{
           // Build weight deltas to add to weights
           double [] w_deltas = new double[FA[j].getNumBasisFunctions()];
           for(int k = 0; k < FA[j].getNumBasisFunctions(); k++)
           {
        	   w_deltas[k] = alpha*delta*traces[j][k];
           }
            
           // Update weights
           FA[j].addToWeights(w_deltas);            
        }      
    }
    
    /**
     * Compute the value of a state (maximizing over actions).
     * 
     * @param s	the state.
     * @return	the value of s.
     */
    public double maxQ(Observation s)
    {
    	double mx = Double.NEGATIVE_INFINITY;
    	
    	for(int j = 0; j < nactions; j++)
    	{
    		mx = Math.max(mx, FA[j].valueAt(s));
    	}
    	
    	return mx;
    }
    
    /**
     * Decay alpha by a given factor.
     * 
     * @param dec the decay factor.
     */
    public void decayAlpha(double dec)
    {
    	alpha = alpha * dec;
    }
    
    /**
     * Clear out traces. This must be done at the
     * end of every episode. 
     */
    public void clearTraces()
    {
    	for(int j = 0; j < nactions; j++)
    	{
    		for(int k = 0; k < FA[0].getNumBasisFunctions(); k++)
    		{
    			traces[j][k] = 0;
    		}
    	}
    }
   
    /**
     * Revert to greedy behavior.
     */
    public void clearEpsilon()
    {
        eps = 0.0;
    }
    
    private double alpha, gamma, eps, lambda;
    private LinearFA [] FA;
    private double [][] traces;
    private int nactions;
    private final boolean use_alpha_bounds;
}