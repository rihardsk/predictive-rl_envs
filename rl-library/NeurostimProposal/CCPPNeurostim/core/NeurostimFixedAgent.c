#include <stdio.h>  /* for printf */
#include <string.h> /* for strcmp */
#include <iostream>

#include <rlglue/Agent_common.h> /* agent_ function prototypes and RL-Glue types */
#include <rlglue/utils/C/RLStruct_util.h> /* helpful functions for allocating structs and cleaning them up */
#include <rlglue/utils/C/TaskSpec_Parser.h>


action_t this_action;

int stim=0;
int count=0;
int Ndt=0;
int Nfreq=0;

taskspec_t tspec;

//Prototypes:
void setFrequency(double);
void decode_extra(taskspec_t *);

void agent_init(const char* task_spec)
{
	
	allocateRLStruct(&this_action,1,0,0);
	
	decode_taskspec( &tspec, task_spec);
	decode_extra(&tspec);

	setFrequency(1.0);
}


const action_t *agent_start(const observation_t *this_observation) {
	count=1;
    this_action.intArray[0]=1;

	return &this_action;
}

const action_t *agent_step(double reward, const observation_t *this_observation) {
		
	if(Nfreq!=-1 && count++%Nfreq==0){
    	stim = 1;
    }else{
    	stim = 0;
    }
    this_action.intArray[0]=stim;

	return &this_action;
}

void agent_end(double reward) {
}

void agent_cleanup() {
	clearRLStruct(&this_action);
}

const char* agent_message(const char* inMessage) {

	const char * outMessage="I don't know how to respond to your message";
	char *inMessageCopy=(char *)malloc((strlen(inMessage)+1)*sizeof(char));
	strcpy(inMessageCopy,inMessage);	
	/*first token*/
	char *token = strtok (inMessageCopy," ");
	if(strcmp(inMessage,"what is your name?")==0)
		return "my name is NeurostimAgent!";
	else if(strncmp(token,"set",3)==0){
		/*next token*/
		token = strtok (0," ");
		if(strncmp(token,"frequency",9)==0){
			/*frequency value token*/
			token = strtok (0," ");
			if(token!=NULL){
				#ifdef DEBUG
					double freq = atof(token);
					setFrequency(freq);
					printf(	"DEBUG: frequency changed to %f.\n"
							"DEBUG: Nfreq changed to %d.\n",freq, Nfreq);
				#else
					setFrequency(atof(token));
				#endif
				outMessage="frequency updated.";
			}
			else{
				outMessage="argument to set frequency is [double]";
			}

		}
	}
	
	free(inMessageCopy);
	return outMessage;
}

void setFrequency(double freq){
		if(freq!=0){
		Nfreq=(int)((1.0/freq)*Ndt);
		if(Nfreq==0)Nfreq=1;
	}
	else
		Nfreq=-1;
		
	if(Nfreq==0)Nfreq=1;
}

void decode_extra(taskspec_t *tspec){
	char *extra=(char *)malloc((strlen(tspec->extra_spec)+1)*sizeof(char));
	strcpy(extra,tspec->extra_spec);
	char *token = strtok (extra," ");
	while(strcmp(token,"TIME")!=0){
		token = strtok (0," ");
	}
	token = strtok (0," ");
	if(token!=NULL){

		Ndt=atoi(token);
	}
	else{
		Ndt=0;
	}
}



