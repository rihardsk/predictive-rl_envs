package org.rlcommunity.btanner.agentLib.normalizers;

import org.rlcommunity.btanner.agentLib.normalizers.ObservationScalerInterface;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpec;
import org.rlcommunity.rlglue.codec.types.Observation;

public class ObservationNormalizer implements	ObservationScalerInterface {
	double doubleMins[] = null;
	double doubleMaxs[]=null;
	double observationDividers[]=null;
	boolean rangeDetermined = false;

	public ObservationNormalizer(int numVariables){
		doubleMins=new double[numVariables];
		doubleMaxs=new double[numVariables];
		observationDividers=new double[numVariables];

		for(int i=0;i<numVariables;i++){
			doubleMins[i]=Double.MAX_VALUE;
			doubleMaxs[i]=Double.MIN_VALUE;
			observationDividers[i]=Double.MAX_VALUE;
                       
                        

		}
//                rangeDetermined=true;
//                
//                //Quick cheat for mountain car
//                Observation minO=new Observation(0,2);
//                minO.doubleArray[0]=-1.2d;
//                minO.doubleArray[1]=-.07d;
//                notifyOfValues(minO);
//
//                Observation maxO=new Observation(0,2);
//                maxO.doubleArray[0]=.6d;
//                maxO.doubleArray[1]=.07d;
//                notifyOfValues(maxO);
	}
	public void notifyOfValues(Observation theObservations) {
		if(!rangeDetermined){
			//if we haven't seen any observations yet, we initialize our min and max arrays to hold the 
			//current values of the observation variables and then break
			this.rangeDetermined = true;
			for(int i=0; i< theObservations.doubleArray.length; i++){
				this.doubleMins[i] = theObservations.doubleArray[i];
				this.doubleMaxs[i] = theObservations.doubleArray[i];
			}
			return;
		}

		//updating our double Max and Mins
		for(int i =0; i< theObservations.doubleArray.length; i++){
			if(theObservations.doubleArray[i] < this.doubleMins[i]){
				this.doubleMins[i] = theObservations.doubleArray[i];
			}
			if(theObservations.doubleArray[i] > this.doubleMaxs[i]){
				this.doubleMaxs[i] = theObservations.doubleArray[i];
			}
		}

		for(int i=0; i< this.observationDividers.length; i++){
			this.observationDividers[i] = (this.doubleMaxs[i] - this.doubleMins[i]);

			if(observationDividers[i]==0)
				System.out.println("Ahh, its 0!");
		}

	}

	public double scale(int whichDoubleVariable, double thisValue) {
//WE are normalizing to [0,1).  If we've only seen one number before, lets just be tricky
                if(observationDividers[whichDoubleVariable]==0.0d){
                    if(thisValue>doubleMaxs[whichDoubleVariable])return .99999999999d;
                    if(thisValue<doubleMaxs[whichDoubleVariable])return 0.0d;
                    return .5d;
}
		double returnValue= (thisValue-doubleMins[whichDoubleVariable])/observationDividers[whichDoubleVariable];
                if(returnValue<0.0d)returnValue=0.0d;
//hack
                if(returnValue>=1.0d)returnValue=.99999999999d;
return returnValue;
	}
	
	public Observation scaleObservation(Observation originalObservation) {
		int numInts=originalObservation.intArray.length;
		int numDoubles=originalObservation.doubleArray.length;
		
		Observation returnObs=new Observation(numInts,numDoubles);
		
		for(int i=0;i<numInts;i++){
			returnObs.intArray[i]=originalObservation.intArray[i];
		}
		for(int i=0;i<numDoubles;i++){
//                        System.out.print(""+originalObservation.doubleArray[i]+"=>");
			returnObs.doubleArray[i]=scale(i,originalObservation.doubleArray[i]);
  //                      System.out.print(""+returnObs.doubleArray[i]+"\t");
		}   
//System.out.println();
		
		return returnObs;
	}

    public double unScale(int whichDoubleVariable, double scaledValue) {
            double returnValue=(scaledValue*observationDividers[whichDoubleVariable])+doubleMins[whichDoubleVariable];
            return returnValue;
    }
    
    public static void main(String args[]){
        ObservationNormalizer N=new ObservationNormalizer(1);
        Observation o=new Observation(0,1);
        o.doubleArray[0]=0.0;
        N.notifyOfValues(o);
        o.doubleArray[0]=1.0;
        N.notifyOfValues(o);
        
        System.out.println("After seeing 0,1, .5 scales to: "+N.scale(0,.5));
        System.out.println("After seeing 0,1, .5 inverse scales to: "+N.unScale(0,.5));

        o.doubleArray[0]=5.0;
        N.notifyOfValues(o);

        o.doubleArray[0]=-10.0;
        N.notifyOfValues(o);

        System.out.println("After seeing -10,5, .5 scales to: "+N.scale(0,.5));
        System.out.println("After seeing -10,5, .5 inverse scales to: "+N.unScale(0,.5));

        
        
    }

}
