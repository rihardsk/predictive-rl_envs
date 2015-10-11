#include <stdio.h>	/* for printf */
#include <rlglue/RL_glue.h> /* RL_ function prototypes and RL-Glue types */
	
int whichEpisode=0;

/* Run One Episode of length maximum cutOff*/
void runEpisode(int stepLimit) {        
    int terminal=RL_episode(stepLimit);
	printf("\nEpisode %d\t %d steps \t%f total reward.\n",whichEpisode,RL_num_steps(),RL_return());
	whichEpisode++;
}

int main(int argc, char *argv[]) {
	const char* task_spec;
	const char* responseMessage;
	const reward_observation_action_terminal_t *stepResponse;
	const observation_action_t *startResponse;

	printf("\n\nExperiment starting up!\n");


	task_spec=RL_init();
	printf("RL_init called, the environment sent task spec: %s\n",task_spec);

	responseMessage=RL_env_message("set rewards 0 -1 -40");
	printf("Environment responded to \"set rewards 0 -1 -40\" with: %s\n",responseMessage);
	responseMessage=RL_env_message("set noise 0.00002");
	responseMessage=RL_env_message("set verbose true");

	printf("\n\n----------Running a few episodes----------\n");
	RL_agent_message("set frequency 1");
	runEpisode(100);
	printf("\n");

	RL_cleanup();


	return 0;
}
