package core;

import org.rlcommunity.rlglue.codec.RLGlue;

public class NeurostimExperiment {

	protected int whichEpisode = 0;
	
    /* Run One Episode of length maximum cutOff*/
    private void runEpisode(int stepLimit) {
        int terminal = RLGlue.RL_episode(stepLimit);

        int totalSteps = RLGlue.RL_num_steps();
        double totalReward = RLGlue.RL_return();

        System.out.println("\nEpisode " + whichEpisode + "\t " + totalSteps + " steps \t" + totalReward + " total reward\t " + terminal + " natural end");

        whichEpisode++;
    }
    
    public void runExperiment() {
    	System.out.println("\n\nExperiment starting up!");
        String taskSpec = RLGlue.RL_init();
        System.out.println("RL_init called, the environment sent task spec: " + taskSpec);
        

        System.out.println("\n\n----------Sending some sample messages----------");

        /*Talk to the agent and environment a bit...*/
        String responseMessage = RLGlue.RL_agent_message("what is your name?");
        System.out.println("Agent responded to \"what is your name?\" with: " + responseMessage);

		System.out.println("\n\n----------Running a few episodes----------");
		RLGlue.RL_agent_message("set frequency 1");
		RLGlue.RL_env_message("set noise 0.00001");
		RLGlue.RL_env_message("set verbose true");
		runEpisode(1000);


        RLGlue.RL_cleanup();
    }
    
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		(new NeurostimExperiment()).runExperiment();
	}

}
