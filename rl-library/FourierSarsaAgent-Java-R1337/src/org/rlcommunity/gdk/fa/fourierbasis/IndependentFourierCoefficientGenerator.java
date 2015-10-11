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

/**
 * Generate Fourier Basis coefficients assuming that each variable
 * in the state descriptor contributes independently to reward. 
 * This is equivalent to restricting all coefficient vectors to
 * have only one non-zero element.
 * 
 * @author George Konidaris (gdk at cs dot umass dot edu)
 */
public class IndependentFourierCoefficientGenerator implements
		FourierCoefficientGenerator
{
	/**
	 * Compute the independent Fourier Basis coefficient matrix for
	 * a given number of variables up to a given order.
	 * 
	 * @param nvars		the number of variables.
	 * @param order		the highest coefficient value for any individual variable.
	 * @return	a two dimensional array of doubles. The first dimension length is
	 * the number of basis functions, and the second is the number of state variables.
	 */
	public double[][] computeFourierCoefficients(int nvars, int order)
	{
		int nterms = (order*nvars) + 1;
		
		double [][] multipliers = new double[nterms][nvars];
		
		int pos = 0;
		
		// Zero all coefficients.
		for(int j = 0; j < nterms; j++)
		{
			for(int k = 0; k < nvars; k++)
			{
				multipliers[j][k] = 0;
			}
		}
		
		// The first bf has all zero coefficients.
		// They are already zeroed, so skip it.
		pos++;
		
		// Cycle through each variable.
		for(int v = 0; v < nvars; v++)
		{
			// For each variable, cycle up to its order.
			for(int ord = 1; ord <= order; ord++)
			{
				multipliers[pos][v] = ord;
				pos++;
			}		
		}
		
		return multipliers;
	}
}
