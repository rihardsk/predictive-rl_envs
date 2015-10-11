package system;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;
import java.util.Scanner;
import utility.KdTree;
import utility.Mathops;

public class Model {

	//Model params
	private int N;                   //number of model states
	private int E;                   //dimension of model state
	private int L;                   //dimension of model labels

	private int seizureLabel;          //integer value of seizures in labeling
	private double stimMagnitude;   //stimulation intensity
	private int Ndt;                 //numerical steps per second (Hz^-1)
	private Random rand=new Random(System.currentTimeMillis());

	//Base get functions
	public int getSize(){return N;}
	public int getDim(){return E;}
	public int getNdt(){return Ndt;}
	public int getSeizureLabel(){return seizureLabel;}

	protected double getMinByDimension(int dim){
		double value = features[0][dim];
		for(int n=1;n<N;n++){
			if(features[n][dim]<value){
				value=features[n][dim];
			}
		}
		return(value);

	}

	protected double getMaxByDimension(int dim){

		double value = features[0][dim];
		for(int n=1;n>N;n++){
			if(features[n][dim]<value){
				value=features[n][dim];
			}
		}
		return(value);

	}
	
	public double getMax(){
		double[] max=new double[E];
		for(int e=0;e<E;e++){
			if(inv[e]>0)
				max[e]=getMaxByDimension(e);
			else
				max[e]=getMinByDimension(e);
		}
		return Mathops.dotProduct(max, inv);
	}
	
	public double getMin(){
		double[] min=new double[E];
		for(int e=0;e<E;e++){
			if(inv[e]<0)
				min[e]=getMaxByDimension(e);
			else
				min[e]=getMinByDimension(e);
		}
		return Mathops.dotProduct(min, inv);
	}

	public double[] getState(){return s;}

	public double getSignal(){
		return Mathops.dotProduct(s,inv);
	}


	public Model(String initfile){
		//Load model params from file

		FileReader inputFile = null;

		String featuresFilename=null;
		String labelsFilename=null;
		String stimulationFilename=null;
		String invbasisFilename=null;

		try{
			inputFile =new FileReader(initfile);
			Scanner s=new Scanner(new BufferedReader(inputFile));
			featuresFilename=s.nextLine();
			labelsFilename=s.nextLine();
			stimulationFilename=s.nextLine();
			invbasisFilename=s.nextLine();

			N = s.nextInt(); //=23198; 
			E= s.nextInt(); //=5;     
			L=s.nextInt(); //=3;     

			seizureLabel=s.nextInt();      //=3;    
			stimMagnitude=s.nextDouble();  //=0.02; 
			Ndt=s.nextInt();             //=5  

			s.close();
		}
		catch(IOException e){
		}
		finally{
			try{
				if(inputFile!=null)inputFile.close();
			}catch(IOException e){
				System.out.println("Could not load model.");
				e.printStackTrace();
			}
		}


		//Initialize model features to zero
		features = Mathops.doubleZeroMap(N,E);

		//Initialize model labels to zero
		labels = Mathops.intZeroMap(N,L);

		//Initialize model state to zero
		s= Mathops.doubleZeroVector(E);

		//Initialize reconstruction vector
		inv= Mathops.doubleZeroVector(E);
		
		//Initialize prototype stimulation
		dstim= Mathops.doubleZeroVector(E);

		//Initialize prototype noise
		noiseMags=Mathops.doubleZeroVector(E);

		//--------------------
		//Load Data from Files
		//--------------------
		inputFile=null;
		try{
			//Load features from datafile
			inputFile =new FileReader(featuresFilename);
			Scanner s=new Scanner(new BufferedReader(inputFile));

			for(int n=0; n<N; n++){
				for(int e=0; e<E; e++){
					features[n][e]=s.nextDouble();
				}
			}
			s.close();
			inputFile=null;

			//	Load labels from datafile
			inputFile =new FileReader(labelsFilename);
			s=new Scanner(new BufferedReader(inputFile));

			for(int n=0; n<N; n++){
				for(int l=0; l<3; l++){
					labels[n][l]=s.nextInt();
				}
			}
			s.close();
			inputFile=null;

			// Load stimulation from datafile
			inputFile =new FileReader(stimulationFilename);
			s=new Scanner(new BufferedReader(inputFile));

			for(int e=0; e<E; e++){
				dstim[e]=s.nextDouble();
			}

			s.close();
			
			// Load invbasis from datafile
			inputFile =new FileReader(invbasisFilename);
			s=new Scanner(new BufferedReader(inputFile));

			for(int e=0; e<E; e++){
				inv[e]=s.nextDouble();
			}

			s.close();

		}
		catch(IOException e){
			System.out.println("Could not load model.");
			e.printStackTrace();
		}
		finally{
			try{
				if(inputFile!=null)inputFile.close();
			}catch(IOException e){

			}

		}

		//---------------------
		//Construct Noise Model
		//---------------------
		double[] runSum= new double[E];
		for(int n=0;n<(N-1);n++){
			for(int e=0;e<E;e++){
				double diff=features[(n+1)][e]-features[n][e];
				runSum[e]=runSum[e]+Math.abs(diff);
			}
		}

		for(int e=0;e<E;e++){
			noiseMags[e]=runSum[e]/N;
		}

		//------------------
		//Construct K-d Tree
		//------------------

		//Fit kdtree2 datatype
		double[][] realdata= new double[N][E]; 

		//Copy features over
		for (int n=0; n<N; n++) {
			for (int e=0; e<E; e++) 
				realdata[n][e]=features[n][e];
		}

		//Build tree
		tree = new KdTree(realdata,true);
		//----------------
		//Initialize State
		//----------------
		reset();

	}


	//Initialize trajectory
	public void reset(){
		//Rand index on range 1:N
		int randomIndex1 = rand.nextInt(N+1);
		int randomIndex2 = rand.nextInt(N+1);

		//Avg 
		for(int e=0;e<E;e++){
			s[e]=(features[randomIndex1][e]+features[randomIndex2][e])/2;
		}

		//Compute nbr of state
		neighbor=nearestNbrKdTree();

	}

	public void reset(int index){

		//Extract state from features at index
		for(int e=0;e<E;e++){
			s[e]=features[index][e];
		}

		//Compute nbr of state
		neighbor=index;

	}

	public void reset(double[] state){

		//Extract state from features at index
		for(int e=0;e<E;e++){
			s[e]=state[e];
		}

		//Compute nbr of state
		neighbor=nearestNbrKdTree();

	}

	//Numerical Integration
	public void step(int stim, double noise){

		double[] nbrVec=new double[E];
		double[] nextNbrVec=new double[E];

		//Check NN bounds
		if(neighbor>=N){//features.size1()
			reset();
		}

		for(int e=0;e<E;e++){
			nbrVec[e]=features[neighbor][e];
			nextNbrVec[e]=features[(neighbor==N-1)?0:neighbor+1][e];
		}

		//Compute the gradient
		double[] gradient = new double[E];
		for(int e=0;e<E;e++){
			gradient[e]= nextNbrVec[e]-nbrVec[e];
		}



		//Compute the noise
		double[] sNoise=new double[E];
		Random rand =new Random();
		for(int e=0;e<E;e++){
			sNoise[e]=noise*noiseMags[e]*(2*rand.nextDouble());
		}

		//Integrate
		for(int e=0;e<E;e++){
			s[e]=s[e]+(gradient[e]+sNoise[e]+stim*stimMagnitude*dstim[e]);
		}

		//Update NN
		neighbor=nearestNbrKdTree();

	}


	//Extract Data
	public int getNeighbor(){return(neighbor);}
	public int getNeighbor(double[] state){return 0;}

	public int getLabel(int nbr){
		return labels[nbr][2];
	}

	public int getLabel(double[] state){return 0;}

	//Fill in
	public int convertActionToActionId(double x){return 0;}

	//Model features
	private double[][] features;

	//Model labels
	private int[][] labels;

	//Model state
	private double[] s;

	//Signal reconstruction vector
	private double[] inv;
	
	//Prototype stimulation gradient
	private double[] dstim;

	//Prototype noise magnitudes
	private double[] noiseMags;

	//kdtree for nearest neighbor storage
	private KdTree tree;

	//Current model neighbor
	private int neighbor;  

	//Helper functions
	public int nearestNbr(){

		int leastNbrId=0;
		double leastDist=1.e30;

		for(int n=0;n<N;n++){
			double[] nbr=Mathops.doubleZeroVector(E);
			for(int e=0;e<E;e++){
				nbr[e]=features[n][e];
			}

			//Calc difference
			double[] delta= new double[E];
			for(int e=0;e<E;e++){
				delta[e]=nbr[e]-s[e];
			}

			//Calc sum of squares
			double sumSqr=0.0;
			for(int e=0;e<E;e++){
				sumSqr+=delta[e]*delta[e];
			}

			//Calc dist 
			double dist=Math.sqrt(sumSqr);

			//Check for nearest nbr
			if(dist<leastDist){
				leastNbrId=n;
				leastDist=dist;
			}

		}

		return(leastNbrId);

	}

	private int nearestNbrKdTree(){

		int K=1;
		KdTree.ResultVector result = new KdTree.ResultVector();

		double[] query= new double[E];


		for(int e=0;e<E;e++){
			query[e]=s[e];
		}

		tree.nNearest(query,K,result); // search f

		return(result.pq.peek().idx);

	}
}
