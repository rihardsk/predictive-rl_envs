package org.rlcommunity.btanner.agentLib.functionApproximators;

import org.rlcommunity.rlglue.codec.types.Observation;


public interface FunctionApproximatorInterface {
	public void init();
	public void  update(Observation theObservation, double delta);
	public double query(Observation theObservation);
	public double query(Observation theObservation, long uniqueId);
        public void cleanup();
}
