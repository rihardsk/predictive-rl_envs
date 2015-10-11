package org.rlcommunity.gdk.fa.fourierbasis;

/* Copyright 2009 George Konidaris
 * http://www-all.cs.umass.edu/~gdk/
 * gdk@cs.umass.edu
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
import org.rlcommunity.rlglue.codec.taskspec.TaskSpec;
import org.rlcommunity.rlglue.codec.taskspec.ranges.DoubleRange;

/**
 * Fourier Basis implementation.
 * Updated June 2011, to include alpha scaling. 
 *
 * @author George Konidaris (gdk at cs dot umass dot edu)
 */
public class FourierFA implements LinearFA 
{
    /**
     * Construct a new FourierBasis with a given task specification and order. 
     * Assume a full basis initialized to 0 everywhere.
     * 
     * @param ts		task specification (containing the number of 
     * 					variables in the state descriptor).
     * @param order		the upper bound on individual FourierBasis coefficients.
     */
    public FourierFA(TaskSpec ts, int order)
    {
	this(ts, order, 0, new FullFourierCoefficientGenerator());
    }
	
    /**
     * Construct a new FourierBasis with a given task specification, order and initial value. 
     * Assume a full basis.
     * 
     * @param ts		task specification (containing the number of 
     * 					variables in the state descriptor).
     * @param order		the upper bound on individual FourierBasis coefficients.
     * @param init		initial value.
     */
    public FourierFA(TaskSpec ts, int order, double init)
    {
	this(ts, order, init, new FullFourierCoefficientGenerator());
    }
	
    /**
     * Construct a new FourierBasis with a given task specification and order, and  
     * using a given coefficient generator.
     * 
     * @param ts		task specification (containing the number of 
     * 					variables in the state descriptor).
     * @param order		the upper bound on individual FourierBasis coefficients.
     * @param gen		coefficient generator.
     */
    public FourierFA(TaskSpec ts, int order, FourierCoefficientGenerator gen)
    {
	this(ts, order, 0, gen);
    }
	
    /**
     * Construct a new FourierBasis with a given task specification, order and initial value,
     * and using a given coefficient generator.
     * 
     * @param ts		task specification (containing the number of 
     * 					variables in the state descriptor).
     * @param order		the upper bound on individual FourierBasis coefficients.
     * @param init		initial value.
     * @param gen		coefficient generator.
     */
    public FourierFA(TaskSpec ts, int order, double init, FourierCoefficientGenerator gen)
    {
	task_spec = ts;
	int nvars = ts.getNumContinuousObsDims();
	initialize(nvars, order, init, gen);
    }
	
    /**
     * Create a separate copy of this function approximator. 
     */
    public LinearFA duplicate() 
    {	
	// Create a new FourierFA with the same specifications.
	FourierFA f = new FourierFA(task_spec, Order, Generator);
		
	// Copy the FA weights across.
	for(int j = 0; j < weights.length; j++)
	    {
		f.weights[j] = weights[j];
	    }
		
	return f;
    }
	
    /**
     * Obtain the number of basis functions (and hence the number of
     * weights) in this function approximator.
     * 
     * @return the number of basis functions.
     */
    public int getNumBasisFunctions()
    {
	return nterms;
    }
	
    /**
     * Compute the feature vector for a given state.
     * This is achieved by evaluating each Fourier Basis function
     * at that state.
     * 
     * @param s		the state in question.
     * @return 		a vector of doubles representing each basis function evaluated at s.
     */
    public double [] computeFeatures(Observation s)
    {
	double [] ss = s.doubleArray;
		
	for(int pos = 0; pos < nterms; pos++)
	    {
		double dsum = 0;
		
		for(int j = 0; j < NVariables; j++)
		    {
			double sval = scale(ss[j], j);		
			dsum += sval*multipliers[pos][j];
		    }
			
		phi[pos] = Math.cos((Math.PI) * dsum);
	    }
		
	return phi;
    }
	
    /**
     * Scale a state variable to between 0 and 1.
     * (this is required for the Fourier Basis).
     * 
     * @param val	the state variable.
     * @param pos	the state variable number.
     * @return		the normalized state variable.
     */
    private double scale(double val, int pos)
    {
	DoubleRange range = task_spec.getContinuousObservationRange(pos);
		
	return (val - range.getMin()) / (range.getMax() - range.getMin());
    }
	
    /**
     * Initialize the FourierBasis using the given parameters.
     * 
     * @param nvars		the number of state variables in the domain.
     * @param order		the basis order (the highest integer value of any individual coefficient). 
     * @param init		the initial value.
     * @param gen		the coefficient generator object.
     */
    private void initialize(int nvars, int order, double init, FourierCoefficientGenerator gen) 
    {
	NVariables = nvars;
	Order = order;
	Generator = gen;

	// Obtain the coefficient vectors. 
	multipliers = gen.computeFourierCoefficients(NVariables, Order);
	nterms = multipliers.length;
		
	// Create and initialize the feature and weight vectors.
	phi = new double[nterms];
	weights = new double[nterms];
	alpha_scale = new double[nterms];

	for(int j = 0; j < nterms; j++)
	    {
		weights[j] = 0;
		phi[j] = 0;

		alpha_scale[j] = computeAlphaScale(multipliers[j]);
	    }
		
	// Initialize the function approximator to have value
	// init everywhere. (the 0th basis function is always
	// 1 everywhere)
	weights[0] = init;
    }

    /**
     * Compute the alpha scaling factor for a given basis function coefficient vector.
     * Note that if use_alpha_scaling is set to false, this returns 1 for 
     * any coefficient vector.
     *
     * @param multipliers A basis function coefficient vector.
     * @return 1/|multipliers|, 1 when |multipliers| = 0.
     */
    protected double computeAlphaScale(double [] multipliers)
    {
	if(!use_alpha_scaling) return 1.0;

	double tot = 0;

	for(int j = 0; j < NVariables; j++)
	    {
		tot = tot + (multipliers[j]*multipliers[j]);
	    }

	tot = Math.sqrt(tot);

	if(tot == 0.0) return 1.0;
	else return 1/tot;
    }

    /**
     * Compute the value of this function approximator at a given state.
     * 
     * @param s		the state observation in question.
     * @return the value at state s.
     */
    public double valueAt(Observation s) 
    {
	// Compute the features at s.
	double [] ph = computeFeatures(s);
		
	// Compute the weighted sum.
	double sm = 0.0;
		
	for(int j = 0; j < weights.length; j++)
	    sm += ph[j]*weights[j];
		
	return sm;
    }
	
    /**
     * Set the value of this function approximator everywhere.
     * 
     * @param d		the target value.
     */
    public void setValue(double d)
    {	
	// All weights are zero ...
	for(int j = 0; j < nterms; j++)
	    weights[j] = 0.0;
		
	// ... except the 0th one.
	weights[0] = d;
    }
	
    /**
     * Add a weight delta vector to the function approximator's internal weights.
     * 
     * @param w_delta	the vector to be added to the FA's weights.
     */
    public void addToWeights(double [] w_delta)
    {
    	for(int j = 0; j < nterms; j++)
    	{
    		weights[j] += alpha_scale[j]*w_delta[j];
    	}
    }
    
    /**
     * Set the function approximator's weight vector. 
     * 
     * @param new_weights 	the new weight vector.
     */
    public void setWeights(double [] new_weights)
    {
    	for(int j = 0; j < nterms; j++)
    	{
    		weights[j] = new_weights[j];
    	}
    }
	
    private double weights[];
    private double phi[];
    private double alpha_scale[];
    
    private double multipliers[][];
    private int nterms;
    
    private FourierCoefficientGenerator Generator;
    private int NVariables, Order;
    
    TaskSpec task_spec;

    private final boolean use_alpha_scaling = true;
}
