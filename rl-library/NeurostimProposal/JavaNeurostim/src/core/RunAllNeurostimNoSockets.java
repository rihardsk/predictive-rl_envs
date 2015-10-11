package core;


import org.rlcommunity.rlglue.codec.AgentInterface;
import org.rlcommunity.rlglue.codec.EnvironmentInterface;
import org.rlcommunity.rlglue.codec.LocalGlue;
import org.rlcommunity.rlglue.codec.RLGlue;

public class RunAllNeurostimNoSockets {
	public static void main(String[] args){
		//Create the Agent
		AgentInterface theAgent=new NeurostimAgent();
		
		//Create the Environment
		EnvironmentInterface theEnvironment=new NeurostimEnvironment();
		
		LocalGlue localGlueImplementation=new LocalGlue(theEnvironment,theAgent);
		RLGlue.setGlue(localGlueImplementation);
		
		
		//Run the main method of the Neurostim Experiment, using the arguments were were passed
		//This will run the experiment in the main thread.  The Agent and Environment will run
		//locally, without sockets.
		NeurostimExperiment.main(args);
		//System.out.println("RunAllNeurostimNoSockets Complete");
		
	}
}
