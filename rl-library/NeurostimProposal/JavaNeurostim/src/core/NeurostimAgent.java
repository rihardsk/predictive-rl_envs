package core;

import org.rlcommunity.rlglue.codec.AgentInterface;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpec;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;
import org.rlcommunity.rlglue.codec.util.AgentLoader;

public class NeurostimAgent implements AgentInterface {


	protected int stim=0;
	protected int count=0;
	protected int Nfreq=0;
	protected int Ndt=0;

	public void agent_init(String taskSpecification) {


		TaskSpec taskSpec=new TaskSpec(taskSpecification);

		String extra=taskSpec.getExtraString();

		//Parsing of the EXTRAs
		String[] extraDico =extra.split("[\\s=]");
		int i=0;
		while(i<extraDico.length){
			if(extraDico[i].equalsIgnoreCase("TIME")){
				Ndt = Integer.parseInt(extraDico[i+1]);
			}
			i+=2;
		}

		setFrequency(1.0);

	}

	public String agent_message(String message) {
		if(message.equalsIgnoreCase("what is your name?"))
			return "My name is NeurostimAgent!";

		String[] tokens = message.split(" ");

		/*first token*/
		if(tokens.length>1 && tokens[0].equalsIgnoreCase("set")){
			/*next token*/
			if(tokens[1].equalsIgnoreCase("frequency")){
				if(tokens.length<3)return "Invalid Command:\n\tset frequency [double]";
				double freq = Double.parseDouble(tokens[2]);
				setFrequency(freq);
				}
			else{
				return "Argument of set not recognized.";
			}
		}
		return "I don't know how to respond to your message";
	}

	public Action agent_start(Observation arg0) {

		Action returnAction = new Action(1, 0, 0);

		count=1;
		returnAction.intArray[0]=1;

		return returnAction;
	}

	public Action agent_step(double reward, Observation observation) {

		Action returnAction = new Action(1, 0, 0);

		if(Nfreq!=-1 && count++%Nfreq==0){
			stim = 1;
		}else{
			stim = 0;
		}
		returnAction.intArray[0]=stim;

		return returnAction;
	}



	public void agent_cleanup() {}

	public void agent_end(double reward) {}

	protected void setFrequency(double freq){
		if(freq!=0){
			Nfreq=(int)((1.0/freq)*Ndt);
			if(Nfreq==0)Nfreq=1;
		}
		else
			Nfreq=-1;

		if(Nfreq==0)Nfreq=1;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		AgentLoader theLoader=new AgentLoader(new NeurostimAgent());
		theLoader.run();
	}

}
