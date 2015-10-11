/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rlcommunity.agents;

/**
 *
 * @author btanner
 */
public class GaussianRBFKernel implements BasisFunctionInterface {

    private final double[] center;
    private final double beta;
    /**
     * Copies center
     * @param center
     */
    public GaussianRBFKernel(double[] center, double beta) {
        this.center = (double[]) center.clone();
        this.beta=beta;
    }

    public double weight(double[] x1){
        return distance(x1,center);
    }

    private double distance(double[] x1, double[] x2) {
        assert(x1.length==x2.length);

        double squaredSumOfDiffs=0.0d;
        for(int i=0;i<x1.length;i++){
            double diff=x1[i]-x2[i];
            squaredSumOfDiffs+=diff*diff;
        }

       double d=Math.exp(-beta*squaredSumOfDiffs);
       return d;
    }

}
