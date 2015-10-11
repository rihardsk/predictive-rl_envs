package utility;

public abstract class  Mathops{

	//vector simulation
	static public  int[] intZeroVector(int N){
		int[] vector=new int[N];
		for(int i=0;i<N;i++)
			vector[i]=0;
		return vector;
	}
	static public void swap(int[] vector,int i, int j){
		int tmp = vector[i];
		vector[i]=vector[j];
		vector[j]=tmp;
	}
	static public int[] c(int[] u, int[] v){
		int[] vector = new int[u.length+v.length];
		for(int i=0;i<u.length;i++){
			vector[i]=u[i];
		}
		for(int i=0;i<v.length;i++){
			vector[i+u.length]=v[i];
		}
		return vector;
	}
	static public  int[] c(int[] u, int v){
		int[] vector = new int[u.length+1];
		for(int i=0;i<u.length;i++){
			vector[i]=u[i];
		}
		vector[1+u.length]=v;
		return vector;
	}
	static public  int[] c(int u, int[] v){
		int[] vector = new int[1+v.length];
		vector[0]=u;
		for(int i=1;i<v.length;i++){
			vector[i+1]=v[i];
		}	
		return vector;
	}
	static public int max(int[] u){
		int max=u[0];
		for(int i=1;i<u.length;i++){
			if(u[i]>max){
				max=u[i];
			}
		}
		return max;
	}

	
	static public  float[] floatZeroVector(int N){
		float[] vector=new float[N];
		for(int i=0;i<N;i++)
			vector[i]=0;
		return vector;
	}
	static public void swap(float[] vector,int i, int j){
		float tmp = vector[i];
		vector[i]=vector[j];
		vector[j]=tmp;
	}
	static public float[] c(float[] u, float[] v){
		float[] vector = new float[u.length+v.length];
		for(int i=0;i<u.length;i++){
			vector[i]=u[i];
		}
		for(int i=0;i<v.length;i++){
			vector[i+u.length]=v[i];
		}
		return vector;
	}
	static public  float[] c(float[] u, float v){
		float[] vector = new float[u.length+1];
		for(int i=0;i<u.length;i++){
			vector[i]=u[i];
		}
		vector[1+u.length]=v;
		return vector;
	}
	static public  float[] c(float u, float[] v){
		float[] vector = new float[1+v.length];
		vector[0]=u;
		for(int i=1;i<v.length;i++){
			vector[i+1]=v[i];
		}	
		return vector;
	}
	static public float max(float[] u){
		float max=u[0];
		for(int i=1;i<u.length;i++){
			if(u[i]>max){
				max=u[i];
			}
		}
		return max;
	}

	static public  double[] doubleZeroVector(int N){
		double[] vector=new double[N];
		for(int i=0;i<N;i++)
			vector[i]=0;
		return vector;
	}
	static public void swap(double[] vector,int i, int j){
		double tmp = vector[i];
		vector[i]=vector[j];
		vector[j]=tmp;
	}
	static public double[] c(double[] u, double[] v){
		double[] vector = new double[u.length+v.length];
		for(int i=0;i<u.length;i++){
			vector[i]=u[i];
		}
		for(int i=0;i<v.length;i++){
			vector[i+u.length]=v[i];
		}
		return vector;
	}
	static public  double[] c(double[] u, double v){
		double[] vector = new double[u.length+1];
		for(int i=0;i<u.length;i++){
			vector[i]=u[i];
		}
		vector[1+u.length]=v;
		return vector;
	}
	static public  double[] c(double u, double[] v){
		double[] vector = new double[1+v.length];
		vector[0]=u;
		for(int i=1;i<v.length;i++){
			vector[i+1]=v[i];
		}	
		return vector;
	}
	static public double max(double[] u){
		double max=u[0];
		for(int i=1;i<u.length;i++){
			if(u[i]>max){
				max=u[i];
			}
		}
		return max;
	}



	static public  double dist(double[] u, double[] v){
		double result = 0.0;

		for(int i=0; i<u.length; i++){
			result+=Math.pow(u[i]-v[i],2);
		}
		return Math.sqrt(result);
	}

	static public double dotProduct(double[] u, double[] v){
		double dp=0;
		for(int i=0;i<u.length;i++)
			dp+=u[i]*v[i];
		return dp;
	}
	
	//public  vector<T> apply(vector<T> u, double (*fp)(double)){return null;}

	
	//Matrices
	static public  int[][] intZeroMap(int M, int N){
		int[][] matrix=new int[M][N];
		for(int i=0;i<M;i++)
			for(int j=0;j<N;j++)
				matrix[i][j]=0;
		return matrix;
	}
	static public int[][] rbind(int[][] u,int[] v){
		int[][] matrix = new int[u.length+1][u[0].length];
		for(int i=0;i<u.length;i++){
			for(int j=0;j<u[i].length;j++){
				matrix[i][j]=u[i][j];
			}
		}

		for(int j =0;j<matrix[0].length;j++){
			matrix[matrix.length-1][j]= v[j];
		}

		return matrix;
	}
	static public int abssum(int[][] u){
		int result =0;
		for(int m=0;m<u.length;m++){
			for(int n=0;n<u[m].length;n++){
				result+=Math.abs(u[m][n]);
			}
		}
		return(result);
	}
	
	static public  float[][] floatZeroMap(int M, int N){
		float[][] matrix=new float[M][N];
		for(int i=0;i<M;i++)
			for(int j=0;j<N;j++)
				matrix[i][j]=0;
		return matrix;
	}
	static public float[][] rbind(float[][] u,float[] v){
		float[][] matrix = new float[u.length+1][u[0].length];
		for(int i=0;i<u.length;i++){
			for(int j=0;j<u[i].length;j++){
				matrix[i][j]=u[i][j];
			}
		}

		for(int j =0;j<matrix[0].length;j++){
			matrix[matrix.length-1][j]= v[j];
		}

		return matrix;
	}
	static public float abssum(float[][] u){
		float result =0;
		for(int m=0;m<u.length;m++){
			for(int n=0;n<u[m].length;n++){
				result+=Math.abs(u[m][n]);
			}
		}
		return(result);
	}
	
	static public  double[][] doubleZeroMap(int M, int N){
		double[][] matrix=new double[M][N];
		for(int i=0;i<M;i++)
			for(int j=0;j<N;j++)
				matrix[i][j]=0;
		return matrix;
	}
	static public double[][] rbind(double[][] u,double[] v){
		double[][] matrix = new double[u.length+1][u[0].length];
		for(int i=0;i<u.length;i++){
			for(int j=0;j<u[i].length;j++){
				matrix[i][j]=u[i][j];
			}
		}

		for(int j =0;j<matrix[0].length;j++){
			matrix[matrix.length-1][j]= v[j];
		}

		return matrix;
	}
	static public double abssum(double[][] u){
		double result =0;
		for(int m=0;m<u.length;m++){
			for(int n=0;n<u[m].length;n++){
				result+=Math.abs(u[m][n]);
			}
		}
		return(result);
	}
	
	
	static public double[][] resize(double[][] matrix,int M, int N){
		double[][] tmp = new double[M][N];
		int m= Math.min(M, matrix.length);
		int n=Math.min(N, matrix[0].length);
		for(int i = 0; i< m;i++)
			for(int j=0;j<n;j++)
				tmp[i][j]=matrix[i][j];
		return tmp;
	}
	
}
