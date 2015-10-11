package core;

import org.rlcommunity.rlglue.codec.util.AgentLoader;
import org.rlcommunity.rlglue.codec.util.EnvironmentLoader;

public class RunAllNeurostim {

	/**
	 * @param args
	 */
	public static void main(String[] args){
		//Create an agentLoader that will start the agent when its run method is called
		AgentLoader theAgentLoader=new AgentLoader(new NeurostimAgent());
		//Create an environmentloader that will start the environment when its run method is called
		EnvironmentLoader theEnvironmentLoader=new EnvironmentLoader(new NeurostimEnvironment());
		
		//Create threads so that the agent and environment can run asynchronously 		
		Thread agentThread=new Thread(theAgentLoader);
		Thread environmentThread=new Thread(theEnvironmentLoader);
		
		//Start the threads
		agentThread.start();
		environmentThread.start();
		
		//Run the main method of the Neurostim Experiment, using the arguments were were passed
		//This will run the experiment in the main thread.
		NeurostimExperiment.main(args);
		System.out.println("RunAllNeurostim Complete");
		
		//Quit Java, including stopping the other threads
		System.exit(1);
	}

}
