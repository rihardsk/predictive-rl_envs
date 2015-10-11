package org.rlcommunity.gdk.fa;

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
 
import org.rlcommunity.rlglue.codec.types.Observation;

/**
 * Linear function approximator interface. Learning algorithms should use
 * this interface for linear function approximation, so that they can be 
 * compatible with any function approximator derived from it. 
 * 
 * @author George Konidaris (gdk at cs dot umass dot edu)
 */
public interface LinearFA 
{
    /**
     * Create a duplicate function approximator. The new FA should be of the
     * same type, use the same set of basis functions, and have equal (but
     * distinct) weights. A user should be able to use this function to create
     * an FA they can modify independently of the original. 
     * 
     * @return a new function approximator of the same type.
     */
    public LinearFA duplicate();
	
    /**
     * Compute the value of this function approximator at a given state.
     * 
     * @param s	the state observation in question.
     * @return the value of the function approximated at state s.
     */
    public double valueAt(Observation s);
	
    /**
     * Compute the value of the function approximator's basis functions
     * at a given state. 
     * 
     * @param s		the state observation in question.
     * @return	an array of doubles, each item in the array representing the value
     * of an individual basis function used by the function approximator.
     */
    public double[] computeFeatures(Observation s);  
    
    /**
     * Determine the number of basis functions (and hence number of parameters)
     * in the function approximator.
     * 
     * @return	the number of basis functions used by the FA.
     */
    public int getNumBasisFunctions();
   
    /**
     * Set the function approximator to a constant value.
     * (you can typically only do this for a linear FA when one of its
     * basis functions is 1 everywhere)
     * 
     * @param val	the desired value.
     */
    public void setValue(double val);
    
    /**
     * Add a given vector to the function approximator's weight vector. 
     * This is typically used by an incremental learning algorithm.
     * 
     * @param w_delta	an array of doubles to be added to the
     * function approximator's weights.
     */
    public void addToWeights(double [] w_delta);
    
    /**
     * Set the function approximator's weight vector.
     * 
     * @param new_weights the new weight vector.
     */
    public void setWeights(double [] new_weights);
}
