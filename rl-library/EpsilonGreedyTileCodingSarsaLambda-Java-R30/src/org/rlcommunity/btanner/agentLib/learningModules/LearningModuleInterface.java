package org.rlcommunity.btanner.agentLib.learningModules;

import org.rlcommunity.rlglue.codec.types.Observation;

public interface LearningModuleInterface extends LearningInterface, ValueProviderInterface{

        /**
         * Clean up any temporary structures and
         * storage.  This is meant to make garbage 
         * collection easier for java in very long
         * experiments
         */
        public void cleanup();


        public double query(Observation theObservation, int i, long uniqueId);
	
	//This maybe shouldn't be here
	public abstract double queryNoSideEffect(Observation theObservation, int theAction);

        //We should move this into a more specific subclass
        public void update(Observation s, int action, double delta);

}
