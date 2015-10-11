package org.rlcommunity.gdk.fa.fourierbasis;

/**
 * Generate a full Fourier Basis for a given order and
 * number of variables. 
 * 
 * @author George Konidaris (gdk at cs dot umass dot edu).
 */
public class FullFourierCoefficientGenerator implements
		FourierCoefficientGenerator
{

	/**
	 * Compute the full Fourier Basis coefficient matrix for
	 * a given number of variables up to a given order.
	 * 
	 * @param nvars		the number of variables.
	 * @param order		the highest coefficient value for any individual variable.
	 * @return	a two dimensional array of doubles. The first dimension length is
	 * the number of basis functions, and the second is the number of state variables.
	 */
	public double[][] computeFourierCoefficients(int nvars, int order)
	{
		int nterms = (int)Math.pow(order + 1, nvars);
		double [][] multipliers = new double[nterms][nvars];
		
		int pos = 0;
		
		int c[] = new int[nvars];
		for(int j = 0; j < nvars; j++)
			c[j] = 0;
		
		do
		{
			for(int k = 0; k < nvars; k++)
			{
				multipliers[pos][k] = c[k];
			}
			
			pos++;
			
			// Iterate c
			Iterate(c, nvars, order);
		}
		while(c[0] <= order);
		
		return multipliers;
	}

	
	/**
	 * This method iterates through a coefficient vector
	 * up to a given degree. (like counting in a base of
	 * that degree).
	 * 
	 * @param c			the coefficient vector.
	 * @param NVariables	the number of variables in c.
	 * @param Degree		the degree up to which to increment.
	 */
	private void Iterate(int [] c, int NVariables, int Degree)
	{
		(c[NVariables - 1])++;
		
		if(c[NVariables - 1] > Degree)
		{
			if(NVariables > 1)
			{
				c[NVariables - 1]  = 0;
				Iterate(c, NVariables - 1, Degree);
			}
		}
	}
}
