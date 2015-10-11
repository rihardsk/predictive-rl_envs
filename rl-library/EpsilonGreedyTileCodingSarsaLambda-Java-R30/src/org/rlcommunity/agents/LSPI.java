/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rlcommunity.agents;

import java.util.Collection;
import org.rlcommunity.btanner.agentLib.dataStructures.DataPoint;
import Jama.Matrix;
import Jama.SingularValueDecomposition;
import java.util.ArrayList;
import java.util.Random;
import org.rlcommunity.btanner.agentLib.actionSelectors.ActionSelectorInterface;
import org.rlcommunity.btanner.agentLib.learningModules.ValueProviderInterface;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpec;
import org.rlcommunity.rlglue.codec.types.Observation;
import rlVizLib.general.ParameterHolder;


    /**
 * This code is basically a straight port from the LSPI Code at:
 * http://www.cs.duke.edu/research/AI/LSPI/
 *
 * The distribution license for that code seems unclear.  I will inquire soon.
 * @author btanner
 */
public class LSPI implements ActionSelectorInterface{

    private final Collection<DataPoint> rawData;
    private Matrix A;
    private Matrix B;
    private Matrix PiPhihat;
    private Matrix Phihat;
    private Matrix Rhat;
    double discountFactor = 1.0d;

    Matrix Policy=null;

    private final BasisFunctionSet theBasisSet;
    private final int numActions;

    public static void addToParamHolder(ParameterHolder P){
        P.addIntegerParam("num-basis-partitions",2);
    }
    public LSPI(Collection<DataPoint> rawData,TaskSpec theTaskSpec, ParameterHolder P) {
        this.numActions=theTaskSpec.getDiscreteActionRange(0).getRangeSize();
        this.rawData = rawData;

        //Figure out the observation size
        int obsDimension=rawData.iterator().next().getS().getNumDoubles();

        theBasisSet=new TestRBFBasisSet(obsDimension,P.getIntegerParam("num-basis-partitions") , theTaskSpec);
        int numBasis=theBasisSet.getNumFeatures();
        A = new Matrix(numBasis, numBasis);
        B = new Matrix(numBasis, 1);
        PiPhihat = new Matrix(rawData.size(), numBasis);

        Phihat = new Matrix(rawData.size(), numBasis);
        Rhat = new Matrix(rawData.size(), 1);

        Policy=new Matrix(numBasis, numBasis);

    }

        /** Solves WA = B using a SVD decomposition.
     *
     * @param A The left hand matrix.
     * @param B The right hand matrix.
     * @return The SVD solution to WA = B.
     */
    public static Matrix solveMatrix(Matrix A, Matrix B) {
        SingularValueDecomposition SVD = A.svd();
        Matrix S = SVD.getS().copy();
        int rank = SVD.rank();

        // Transform S so that its eigenvalues are inverted
        for (int i = 0; i < rank; i++) {
            double s = S.get(i, i);
            S.set (i, i, 1.0 / s);
        }

        for (int i = rank; i < S.getColumnDimension(); i++)
            S.set(i, i, 0);

        // A+ = U * S^-1 * Vt
        Matrix U = SVD.getU();
        Matrix Vt = SVD.getV().transpose();

        Matrix Ap = U.times(S);

        Ap = Ap.times(Vt).transpose();

        // W = A+ * B
        return Ap.times(B);
    }

    public void Solve() {
        preProcess();


        //We do 2 iterations off the bat because we might not have started
        //with a similarly parameterized policy.
        Matrix Wold=doIteration(true);
        int iterationCount=1;

        while(iterationCount<20){
            System.out.print("Iteration: "+iterationCount+" ");
            Policy=doIteration();

            Matrix difference=Policy.minus(Wold);
            double LMAXNorm=difference.normInf();
            double L2Norm=difference.norm2();

            System.out.printf("Norms Max:%.2f L2:%.2f\n",LMAXNorm,L2Norm);

            if(L2Norm<.0001){
                break;
            }
            Wold=Policy;
            iterationCount++;
        }
    }

    private Matrix doIteration(){
        return doIteration(false);
    }
 
    /**
     * Returns the weight matrix
     * @return
     */
    private Matrix doIteration(boolean first) {
        if(first){
            calcRandomPolicyFeatures();
        }else{
            calcPolicyFeatures();
        }
//
//        System.out.println("PhiHat is: "+Phihat.getRowDimension()+" x "+Phihat.getColumnDimension());
//        System.out.println("PiPhihat is: "+PiPhihat.getRowDimension()+" x "+PiPhihat.getColumnDimension());
//        System.out.println("PiPhihat * discount is: "+PiPhihat.times(discountFactor).getRowDimension()+" x "+PiPhihat.times(discountFactor).getColumnDimension());
//
//        System.out.println("Rhat is: "+Rhat.getRowDimension()+" x "+Rhat.getColumnDimension());
//        for(int i=0;i<Rhat.getRowDimension();i++){
//            System.out.printf(" %.2f",Rhat.get(i, 0));
//        }
//        System.out.println();
        A = (Phihat.transpose()).times(Phihat.minus(PiPhihat.times(discountFactor)));
        B = Phihat.transpose().times(Rhat);
//        System.out.println("B is: "+B.getRowDimension()+" x "+B.getColumnDimension());
//        for(int i=0;i<B.getRowDimension();i++){
//            for(int j=0;j<B.getColumnDimension();j++){
//                System.out.printf(" %.2f",B.get(i, j));
//            }
//            System.out.println();
//        }
//            System.out.println();
//Original Matlab
//  A = Phihat' * (Phihat - new_policy.discount * PiPhihat);
//  b = Phihat' * Rhat;


        int theRank = A.rank();


        Matrix theWeights = null;
        if (theRank == theBasisSet.getNumFeatures()) {
            System.out.print("It's full rank: "+theRank);
            theWeights=A.solve(B);
        } else {
            System.out.print("It's NOT full rank: "+theRank);
            theWeights=solveMatrix(A, B);
        }

//        System.out.println("The weights is: "+theWeights.getRowDimension()+" x "+theWeights.getColumnDimension());
        return theWeights;
    }

    private void calcPolicyFeatures() {
        int thisPointIndex = 0;
        for (DataPoint thisDataPoint : rawData) {
            if (!thisDataPoint.isAbsorbing()) {
                int nextAction = calculateNextAction(thisDataPoint.getSPrime());
                setBasis(thisPointIndex, PiPhihat.getArray(), thisDataPoint.getSPrime(), nextAction);
            }
            thisPointIndex++;
        }
        
//Original Matlab
//  %%% Loop through the samples
//  for i=1:howmany
//
//    %%% Make sure the nextstate is not an absorbing state
//    if ~samples(i).absorb
//
//      %%% Compute the policy and the corresponding basis at the next state
//      nextaction = policy_function(policy, samples(i).nextstate);
//      nextphi = feval(new_policy.basis, samples(i).nextstate, nextaction);
//      PiPhihat(i,:) = nextphi';
//
//    end
//
//  end
    }

        private void calcRandomPolicyFeatures() {
        int thisPointIndex = 0;
        Random R=new Random();
        for (DataPoint thisDataPoint : rawData) {
            if (!thisDataPoint.isAbsorbing()) {
                int nextAction = R.nextInt(numActions);
                setBasis(thisPointIndex, PiPhihat.getArray(), thisDataPoint.getSPrime(), nextAction);
            }
            thisPointIndex++;
        }
        }


    private void preProcess() {
        int thisPointIndex = 0;
        for (DataPoint thisDataPoint : rawData) {
//            System.out.println("Observation is: "+thisDataPoint.getS().getDouble(0)+" "+thisDataPoint.getS().getDouble(1));
//            System.out.println("Action is: "+thisDataPoint.getAction());
//            double[] theseFeatures=((TestRBFBasisSet)(theBasisSet)).calculateFeatures(thisDataPoint.getS(), thisDataPoint.getAction(),false);
//            for (double d : theseFeatures) {
//                System.out.printf(" %.2f",d);
//            }
//            System.out.println();
            setBasis(thisPointIndex, Phihat.getArray(), thisDataPoint.getS(), thisDataPoint.getAction());
            Rhat.set(thisPointIndex, 0, thisDataPoint.getReward());
            thisPointIndex++;
        }
//Original Matlab
//  for i=1:howmany
//          phi = feval(new_policy.basis, samples(i).state, samples(i).action);
//          Phihat(i,:) = phi';
//          Rhat(i) = samples(i).reward;
//  end
    }
//
    private int calculateNextAction(Observation S) {
        double bestValue=calculateValue(S,0);

        ArrayList<Integer> bestActions=new ArrayList<Integer>();
        bestActions.add(0);

        for(int i=1;i<numActions;i++){
            double thisValue=calculateValue(S,i);
            if(thisValue==bestValue){
                bestActions.add(i);
            }else{
                if(thisValue>bestValue){
                    bestActions.clear();
                    bestActions.add(i);
                bestValue=thisValue;
                }
            }
        }

        Random R=new Random();
        return bestActions.get(R.nextInt(bestActions.size()));
        }

       public double calculateValue(Observation theState, int theAction) {
             double[] theseFeatures=theBasisSet.calculateFeatures(theState, theAction);
             
             Matrix phi=new Matrix(1,theseFeatures.length);
             for(int i=0;i<theseFeatures.length;i++){
                 phi.set(0, i, theseFeatures[i]);
             }

             Matrix theValue=phi.times(Policy);
             return theValue.get(0,0);

    }

/**
     * Calculate the features for (s,a), and fill in ROW theRowIndex of theFeatureArray
     * @param theRowIndex
     * @param array
     * @param s
     * @param action
     */
    private void setBasis(int theRowIndex, double[][] theFeatureArray, Observation s, int action) {
        double[] theseFeatures=theBasisSet.calculateFeatures(s, action);
        for(int i=0;i<theFeatureArray[theRowIndex].length;i++){
            theFeatureArray[theRowIndex][i]=theseFeatures[i];
        }
    }

    public int sampleAction(Observation theObservation, ValueProviderInterface theLearningModule) {
        return calculateNextAction(theObservation);
    }

    public int sampleAction(Observation theObservation, ValueProviderInterface theLearningModule, long uniqueId) {
        return calculateNextAction(theObservation);
    }

    public double[] getActionProbabilities(Observation theObservation, ValueProviderInterface theLearningModule) {
        double[] actionProbs=new double[numActions];
        actionProbs[calculateNextAction(theObservation)]=1.0d;
        return actionProbs;
    }

    public void cleanup() {
    }
}
