#include <string.h> /*strcmp*/
#include <stdio.h> /*printf*/
#include <stdlib.h>

#include <rlglue/Environment_common.h>/* env_ function prototypes and RL-Glue types */
#include <rlglue/utils/C/RLStruct_util.h> /* helpful functions for allocating structs and cleaning them up */

#include <boost/numeric/bindings/traits/ublas_matrix.hpp>
#include <boost/numeric/bindings/traits/ublas_vector.hpp>
#include <boost/numeric/ublas/matrix_proxy.hpp>
#include <boost/numeric/ublas/vector_proxy.hpp>
#include <boost/numeric/ublas/io.hpp>

#include <iostream>
#include "model.h"
 
observation_t this_observation;
reward_observation_terminal_t this_reward_observation;

Model* model;

char* inputFile = (char*)"./params/params.dat";

double normalReward=0;
double stimulationReward=-1;
double seizureReward=-40;

double noise=0.00001;
int verbose=0;

const char* env_init()
{    
	model = new Model(inputFile);
	
	/*Find reward min and max*/
	double min;
	double max;
	
	if(normalReward>seizureReward){
		min = seizureReward;
		max = normalReward;
	}
	else{
		max = seizureReward;
		min = normalReward;
	}
	if(max<stimulationReward){
		max=stimulationReward;
	}
	else if(min>stimulationReward){
		min=stimulationReward;
	}
	/*END: Find reward min and max*/
	
	/*Write the tast_spec*/
	char* task_spec=(char*)calloc(300,sizeof(char));
	sprintf(task_spec,	"VERSION RL-Glue-3.0 "
						"PROBLEMTYPE continuing "
						"DISCOUNTFACTOR 1.0 "
						"OBSERVATIONS "
							"INTS (0 1) "
							"DOUBLES (%f %f) "
							"CHARCOUNT 0 "
						"ACTIONS "
							"INTS (0 1) "
						"REWARDS (%f %f) "
						"EXTRA "
							"TIME %d"
						,model->get_min(), model->get_max(),min,max,model->get_Ndt());
	/*END: Write the tast_spec*/

	/* Allocate the observation variable*/
	allocateRLStruct(&this_observation,1,1,0);
	
	/*Setup the reward_observation variable */
	this_reward_observation.observation=&this_observation;
	this_reward_observation.reward=0;
	this_reward_observation.terminal=0;
	/*END: Setup the reward_observation variable*/
	
   return task_spec;
}

const observation_t *env_start()
{ 
	model->reset();
	
	/*Signal*/
	this_observation.doubleArray[0]=model->get_signal();
	
	/*Label: 0 normal / 1 seizure*/
	this_observation.intArray[0]=(model->get_label(model->get_neighbor())==model->get_seiz_label());
	
	
	if(verbose && (std::cout<<	"Environment start:"<<std::endl<<
								"Reward\tLabel\tState\t \t... Action"<<std::endl<<
								"\t"<<this_observation.intArray[0]<<"\t"<<this_observation.doubleArray[0]<<" \t... "));


  	return &this_observation;
}

const reward_observation_terminal_t *env_step(const action_t *this_action)
{
	/*Apply the action on the model*/
	model->step(this_action->intArray[0],noise);
	
	/*Determine the reward of the new state.*/
	double reward=0;
	if(this_action->intArray[0]==1){
		reward=stimulationReward;
	}
	if((model->get_label(model->get_neighbor())==model->get_seiz_label())){
		reward+=seizureReward;
	}
	else{
		reward+=normalReward;
	}
	this_reward_observation.reward = reward;
	/*END: Determine the reward of the new state.*/
	
	/*Signal*/
	this_observation.doubleArray[0]=model->get_signal();
	
	/*Label: 0 normal / 1 seizure*/
	this_observation.intArray[0]=(model->get_label(model->get_neighbor())==model->get_seiz_label());
	
	if(verbose && (std::cout<<	this_action->intArray[0]<<std::endl<<
								reward<<"\t"<<this_observation.intArray[0]<<"\t"<<this_observation.doubleArray[0]<<" \t... "));
	
	
	return &this_reward_observation;
}

void env_cleanup()
{
	free(model);
	clearRLStruct(&this_observation);
}

const char* env_message(const char* inMessage) {
	
	const char * outMessage="I don't know how to respond to your message";
	char *inMessageCopy=(char *)malloc((strlen(inMessage)+1)*sizeof(char));
	strcpy(inMessageCopy,inMessage);	
	/*first token*/
	char *token = strtok (inMessageCopy," ");
	
	if(strcmp(inMessage,"what is your name?")==0)
		return "my name is NeurostimEnvironment!";
	else if(strncmp(token,"set",3)==0){
		/*next token*/
		token = strtok (0," ");
		if(strncmp(token,"inputfile",9)==0){
			/*inputFile token*/
			token = strtok (0,"\n");
			inputFile=(char *)malloc((strlen(token)+1)*sizeof(char));
			strcpy(inputFile,token);
			#ifdef DEBUG
				printf("DEBUG: inputFile changed to: %s\n",inputFile);
			#endif
			outMessage="inputFile updated, changes will take effect on next init.";
		}
		else if(strncmp(token,"rewards",8)==0){
		
			outMessage="arguments of set rewards are [double] [double] [double].";	
			
			/*normal state reward value token*/
			token = strtok (0," ");
			if(token!=NULL){
				double normal=atof(token);
				/*stimulation reward(penalty) value token*/
				token = strtok (0," ");
				if(token!=NULL){
					double stimulation=atof(token);
					/*seizure state reward(penalty) value token*/
					token = strtok (0," ");
					if(token!=NULL){
						double seizure=atof(token);
						
						normalReward=normal;
						stimulationReward=stimulation;
						seizureReward=seizure;

						#ifdef DEBUG
							printf("DEBUG: reward changed to: %f, %f, %f\n",normalReward, stimulationReward, seizureReward);
						#endif
						outMessage="rewards updated.";
					}		
				}
			}
		}
		else if(strncmp(token,"noise",5)==0){
			/*noise value token*/
			token = strtok (0," ");
			if(token!=NULL){
				noise=atof(token);
				#ifdef DEBUG
					printf("DEBUG: noise changed to: %f\n",noise);
				#endif
				outMessage="noise updated.";
			}
			else{
				outMessage="argument to set noise is [double]";
			}
		}
		else if(strncmp(token,"verbose",7)==0){
			/*verbose value token*/
			token = strtok (0," ");
			if(strncmp(token,"true",4)==0){
				verbose = 1;
				outMessage="verbose enabled.";
			}
			else if(strncmp(token,"false",5)==0){
				verbose = 0;
				outMessage="verbose disabled.";
			}
			else{
				outMessage="Argument of set verbose is [true/false].";
			}
		}
		else{
			#ifdef DEBUG
				printf("DEBUG: unrecognized: %s\n",inMessage+4);
			#endif
	   	 	outMessage="Argument of set not recognized.";
		}
		
	}
	
	free(inMessageCopy);
	return outMessage;
}

