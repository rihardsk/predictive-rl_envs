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
 * An interface for generating Fourier Basis coefficients.
 * This interface is intended to make it easy to produce 
 * FourierBasis instances with constrained coefficient vectors.
 * 
 * This class has a method that returns a two dimensional 
 * array of doubles:<BR>
 *  <PRE>coeffs[nterms][nvars]</PRE><BR>
 * where there are nterms basis functions and nvars variables. The jth
 * basis function is then computed as:<BR>
 * <PRE>cos(pi * (x[0]*coeffs[j][0] + x[1]*coeffs[j][1] + ... x[nvars - 1]*coeffs[j][nvars - 1])),</PRE><BR>
 * for state vector x.
 * 
 * Constraining the coefficients may be useful in practice because 
 * the full set of
 * up to order m terms for a Fourier Basis over k variables
 * has (m + 1)^k basis functions. Thus, for high-dimensional domains
 * a full basis is not feasible. 
 * 
 * The most common method is to assume all variables contribute
 * independently towards reward. This is implemented in the
 * IndependentFourierCoefficientGenerator class, and is
 * equivalent to constraining all coefficient vectors
 * to have only one non-zero entry. Note that
 * this is generally a BAD assumption (and will lead to
 * poor results in domains as simple as Mountain Car).
 * 
 * Another method that may soon become common is to use 
 * a feature selection algorithm over a full Fourier Basis.
 * 
 * Note: the 0th basis function coefficients should always
 * all be zero (i.e., the first basis function should be
 * 1 everywhere).
 * 
 * @author George Konidaris (gdk at cs dot umass dot edu)
 */
public interface FourierCoefficientGenerator
{
	/**
	 * Generate the Fourier Basis coefficients for a given number
	 * of variables, up to a given order. 
	 * 
	 * This method should return a two-dimensional array of doubles. 
	 * The first dimension has the same size as the number of 
	 * basis functions used, 
	 * and the second dimension has the same size as the number of 
	 * variables in the domain. 
	 * 
	 * @param nvars	the number of variables in the domain state.
	 * @param order	the Fourier Basis order. 
	 * 
	 * @return a two dimensional array of doubles. 
	 */
	public double [][] computeFourierCoefficients(int nvars, int order);
}
