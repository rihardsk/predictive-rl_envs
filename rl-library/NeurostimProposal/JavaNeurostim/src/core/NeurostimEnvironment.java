package core;

import org.rlcommunity.rlglue.codec.EnvironmentInterface;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;
import org.rlcommunity.rlglue.codec.types.Reward_observation_terminal;
import org.rlcommunity.rlglue.codec.util.EnvironmentLoader;

import system.Model;

public class NeurostimEnvironment implements EnvironmentInterface {

	protected Model model;

	//Model parameters:
	
	protected String initFile="./params/params.dat";
	
	/**Noise value at each step*/
	protected double noise=0.00001;

	//Rewards values
	
	protected double stimulationReward =-1;
	protected double seizureReward =-40;
	protected double normalReward =0;

	//preferably rl Viz compatible
	boolean verbose=false;

	public String env_init() {

		model = new Model(initFile);

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
		String taskSpec=	"VERSION RL-Glue-3.0 "+
		"PROBLEMTYPE continuing "+
		"DISCOUNTFACTOR 1.0 "+
		"OBSERVATIONS "+
			"INTS (0 1) "+
			"DOUBLES ("+model.getMin()+" "+model.getMax()+") "+
			"CHARCOUNT 0 "+
		"ACTIONS "+
			"INTS (0 1) "+
		"REWARDS ("+min+" "+max+") "+
		"EXTRA "+
			"TIME "+model.getNdt();
		/*END: Write the tast_spec*/

		return taskSpec;
	}

	public String env_message(String message) {
		/*Messages syntax [M]:
		 * 
		 * [M]= 	What is your name? 
		 * 		| 	set [parameter]
		 * 
		 * [parameter]= 	initfile [string]
		 * 				|	rewards [double][double][double]
		 * 				|	noise [double]
		 * 				|	verbose [true/false] 
		 */

		if(message.equalsIgnoreCase("what is your name?"))
			return "My name is NeurostimEnvironment!";

		String[] tokens = message.split(" ");

		/*first token*/
		if(tokens.length>1 && tokens[0].equalsIgnoreCase("set")){
			/*next token*/
			if(tokens[1].equalsIgnoreCase("initfile")){
				if(tokens.length<3)return "Invalid Command:\n\tset initfile [string]";
				initFile=tokens[2];
				//safe for path with spaces:
				for(int i=3;i<tokens.length;i++)
					initFile+=" "+tokens[i];
				return "initfile updated to "+initFile+";\nChanges will take effect on next init.";
			}
			else if(tokens[1].equalsIgnoreCase("rewards")){	
				/*normal state reward value token*/
				if(tokens.length>4){
					double normal=Double.parseDouble(tokens[2]);
					/*stimulation reward(penalty) value token*/
					double stimulation=Double.parseDouble(tokens[3]);
					/*seizure state reward(penalty) value token*/
					double seizure=Double.parseDouble(tokens[4]);

					normalReward=normal;
					stimulationReward=stimulation;
					seizureReward=seizure;

					return "rewards updated to "+normalReward+" "+stimulationReward+" "+seizureReward+".";
				}
				return "Invalid Command:\n\tset rewards [double] [double] [double]";
			}
			else if(tokens[1].equalsIgnoreCase("noise")){
				/*noise value token*/
				if(tokens.length>2){
					noise=Double.parseDouble(tokens[2]);
					return "noise updated to "+noise+".";
				}
				else{
					return "Invalid Command:\n\tset noise [double]";
				}
			}
			else if(tokens[1].equalsIgnoreCase("verbose")){
				/*verbose value token*/
				if(tokens[2].equalsIgnoreCase("true")){
					verbose = true;
					return "Verbose mode enabled.";
				}
				else if(tokens[2].equalsIgnoreCase("false")){
					verbose = false;
					return "verbose mode disabled.";
				}
				else{
					return "Invalid Command:\n\tset verbose [true/false].";
				}
			}
			else{
				return "Argument of set not recognized.";
			}
		}
		return "I don't know how to respond to your message";
	}

	
	public Observation env_start() {
		model.reset();

		Observation returnObservation=new Observation(1,1,0);


		returnObservation.doubleArray[0]=model.getSignal();
		

		/*Label: 0 normal / 1 seizure*/
		returnObservation.intArray[0]=currentLabel();

		if(verbose){
			System.out.println("Environment start:");
			System.out.println("Reward\t\tLabel\tState\t\t... Action");
			System.out.format("\t\t%d\t%.6f\t...",returnObservation.intArray[0],returnObservation.doubleArray[0]);
		}

		return returnObservation;
	}

	public Reward_observation_terminal env_step(Action action) {


		Observation returnObservation=new Observation(1,1,0);

		/*Apply the action on the model*/
		model.step(action.intArray[0],noise);

		/*Determine the reward of the new state.*/
		double reward=0;
		if(action.intArray[0]==1){
			reward=stimulationReward;
		}
		if((model.getLabel(model.getNeighbor())==model.getSeizureLabel())){
			reward+=seizureReward;
		}
		else{
			reward+=normalReward;
		}
		/*END: Determine the reward of the new state.*/

		/*Fill the double array with values of the state of the model*/
		returnObservation.doubleArray[0]=model.getSignal();

		/*Label: 0 normal / 1 seizure*/
		returnObservation.intArray[0]=currentLabel();

		if(verbose){
			System.out.println(action.intArray[0]);
			System.out.format("%5f\t%d\t%.6f\t...",reward,returnObservation.intArray[0],returnObservation.doubleArray[0]);
		}		
		Reward_observation_terminal returnRewardObs=new Reward_observation_terminal(reward,returnObservation,0);
		return returnRewardObs;
	}

	public void env_cleanup() {
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		EnvironmentLoader theLoader=new EnvironmentLoader(new NeurostimEnvironment());
		theLoader.run();
	}

	/**
	 * Helper function.
	 * Output 1 during seizures; 0 otherwise.
	 * */
	protected int currentLabel(){
		return (model.getLabel(model.getNeighbor())==model.getSeizureLabel())?1:0;
	}

}
