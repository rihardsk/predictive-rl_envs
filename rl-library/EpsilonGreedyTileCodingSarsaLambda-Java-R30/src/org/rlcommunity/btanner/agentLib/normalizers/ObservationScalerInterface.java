package org.rlcommunity.btanner.agentLib.normalizers;

import org.rlcommunity.rlglue.codec.taskspec.TaskSpec;
import org.rlcommunity.rlglue.codec.types.Observation;

/**
 * This interface is meant as way to wrap an environment such that the underlying
 * learning algorithm can ignore the observation range and scale of the environment.
 * <p>
 * The behavior is that scaleObservation should return an observation with all of the 
 * doubleArray values to be in [0,1)
 * <p>
 * For example, you can imagine a scaler that keeps track of all of the values 
 * ever seen and then scales the new values into range [0,1] or another scaler
 * that does the same thing based on task spec information.
 *  
 * @author Brian Tanner
 */
public interface ObservationScalerInterface {

    /**
     * Scales thisValue to be in [0,1) according to the scale rules for 
     * Observation.doubleArray[whichDoubleVariable]
     * @param whichDoubleVariable Which double variable from an observation this value is from
     * @param thisValue The value of Observation.doubleArray[whichDoubleVariable]
     * @return a scaled version of this variable
     */
    double scale(int whichDoubleVariable, double thisValue);

    /**
     * Unscales thisValue from [0,1) to its original value according to the inverse
     * scale rules for Observation.doubleArray[whichDoubleVariable]
     * @param whichDoubleVariable
     * @param scaledValue
     * @return
     */
    double unScale(int whichDoubleVariable, double scaledValue);

    /**
     * Scales all values in Observation.doubleArray to be in [0,1) according to the scale rules 
     * @param originalObservation The original observation is left unchanged
     * @return a scaled copy of originalObservation
     */
    Observation scaleObservation(Observation originalObservation);

    /**
     * Call this every time a new observation is seen so that experience-based scalers
     * know what values have been seen.
     * @param theObservation
     */
    void notifyOfValues(Observation theObservation);
    
}
