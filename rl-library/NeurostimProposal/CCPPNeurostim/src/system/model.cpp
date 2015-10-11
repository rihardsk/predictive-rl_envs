/****************************
 *
 * Keith Bush (2009)
 *
 * Reasoning and Learning Lab
 * McGill University
 *
 ****************************/

#include <cassert>
#include <boost/numeric/bindings/traits/ublas_matrix.hpp>
#include <boost/numeric/bindings/traits/ublas_vector.hpp>
#include <boost/numeric/ublas/matrix_proxy.hpp>
#include <boost/numeric/ublas/vector_proxy.hpp>
#include <boost/numeric/ublas/io.hpp>

#include <iostream>
#include <fstream>
#include <string>
#include <cstdlib> 
#include <ctime>
#include <cmath>

#include "model.h"
#include "kdtree2.h"
#include "matvecops.h"

typedef multi_array<float,2> array2dfloat;

namespace ublas = boost::numeric::ublas;
using namespace std;

Model::Model(char* filename){

  
  //Load model params from file
  ifstream input_file;

  input_file.open(filename);
  
  string features_filename;
  string labels_filename;
  string stimulation_filename;
  string invbasis_filename;
  
  
  if (input_file.is_open()){

    getline(input_file,features_filename);
    getline(input_file,labels_filename);
    getline(input_file,stimulation_filename);
    getline(input_file,invbasis_filename);

    input_file >> N; //=23198; 
    input_file >> E; //=5;     
    input_file >> L; //=3;     

    input_file >> seiz_label;      //=3;    
    input_file >> stim_magnitude;  //=0.02; 
    input_file >> Ndt;             //=5 
 
    input_file.close();
    input_file.clear();

  }
    
    
  //Initialize model features to zero
  features = zero_map(N,E);

  //Initialize model labels to zero
  labels = zero_map(N,L);

  //Initialize model state to zero
  s=ublas::zero_vector<float>(E);
  
  //Inititalize reconcstruction vector
  inv=ublas::zero_vector<double>(E);

  //Initialize prototype stimulation
  dstim=ublas::zero_vector<float>(E);

  //Initialize prototype noise
  noise_mags=ublas::zero_vector<float>(E);

  //--------------------
  //Load Data from Files
  //--------------------
  
  
  //Load features from datafile
  input_file.open(features_filename.c_str());
  for(int n=0; n<N; n++){
    for(int e=0; e<E; e++){
      input_file >> features(n,e);
    }
  }
  input_file.close();
  input_file.clear();
    
  //Load labels from datafile
  input_file.open(labels_filename.c_str());
  for(int n=0; n<N; n++){
    for(int l=0; l<3; l++){
      input_file >> labels(n,l);
    }
  }
  input_file.close();
  input_file.clear();
  
  //Load stimulation from datafile
  input_file.open(stimulation_filename.c_str());
  for(int e=0; e<E; e++){
    input_file >> dstim(e);
  }
  input_file.close();
  input_file.clear();
  
    //Load stimulation from datafile
  input_file.open(invbasis_filename.c_str());
  for(int e=0; e<E; e++){
    input_file >> inv(e);
  }
  input_file.close();
  input_file.clear();
  
  
  //---------------------
  //Construct Noise Model
  //---------------------
  ublas::vector<double>run_sum(E);
  for(int n=0;n<(N-1);n++){
    for(int e=0;e<E;e++){
      double diff=features((n+1),e)-features(n,e);
      run_sum(e)+=abs(diff);
    }
  }
  noise_mags = run_sum/N;

  //------------------
  //Construct K-d Tree
  //------------------
  
  
  //Fit kdtree2 datatype
  array2dfloat realdata; 
  
  //Copy features over
  realdata.resize(extents[N][E]); 
  for (int n=0; n<N; n++) {
    for (int e=0; e<E; e++) 
      realdata[n][e] = features(n,e);
  }
  
  //Build tree
  tree = new kdtree2(realdata,true);
  //----------------
  //Initialize State
  //----------------
  srand((unsigned)time(0));
  reset();
}


void Model::reset(){
  
  //Rand index on range 1:N
  int random_index1 = rand()%N+1;
  int random_index2 = rand()%N+1;

  //Avg 
  for(int e=0;e<E;e++){
    s(e)=(features(random_index1,e)+features(random_index2,e))/2;
  }
  
  //Compute nbr of state
  neighbor=nearest_nbr_kdtree();

}


void Model::reset(int index){

  //Extract state from features at index
  for(int e=0;e<E;e++){
    s(e)=features(index,e);
  }
  
  //Compute nbr of state
  neighbor=index;

}


void Model::reset(const ublas::vector<double> & state){

  //Extract state from features at index
  for(int e=0;e<E;e++){
    s(e)=state(e);
  }
  
  //Compute nbr of state
  neighbor=nearest_nbr_kdtree();

}



double Model::get_min_by_dimension(int dim){

  double value = features(0,dim);
  for(int n=1;n<N;n++){
    if(features(n,dim)<value){
      value=features(n,dim);
    }
  }
  return(value);

}


double Model::get_max_by_dimension(int dim){

  double value = features(0,dim);
  for(int n=1;n>N;n++){
    if(features(n,dim)<value){
      value=features(n,dim);
    }
  }
  return(value);

}


double Model::get_max(){
  double dp=0;
  for(int e=0;e<E;e++){
  	if(inv(e)>0)
  		dp+=get_max_by_dimension(e)*inv(e);
  	else
  		dp+=get_min_by_dimension(e)*inv(e);
  }
  return(dp);
}

double Model::get_min(){
  double dp=0;
  for(int e=0;e<E;e++){
  	if(inv(e)<0)
  		dp+=get_max_by_dimension(e)*inv(e);
  	else
  		dp+=get_min_by_dimension(e)*inv(e);
  }
  return(dp);
}



int Model::get_size(){
  return(features.size1());
}

int Model::get_dim(){
  return(features.size2());
}

int Model::get_Ndt(){
  return(Ndt);
}

int Model::get_seiz_label(){
  return(seiz_label);
}


void Model::step(int stim, double noise){

  ublas::vector<double>nbr_vec(E);
  ublas::vector<double>next_nbr_vec(E);

  //Check NN bounds
  if(neighbor>=features.size1()){
    reset();
  }
  
  for(int e=0;e<E;e++){
    nbr_vec(e)=features(neighbor,e);
    next_nbr_vec(e)=features(neighbor+1,e);
    
  }

  //Compute the gradient
  ublas::vector<double>gradient=next_nbr_vec-nbr_vec;

  //Compute the noise
  ublas::vector<double>s_noise(E);
  for(int e=0;e<E;e++){
    int rnum = rand();
    s_noise(e)=noise*noise_mags(e)*(2*(double)rand()/RAND_MAX-1);
  }

  //Integrate
  s=s+(gradient+s_noise+stim*stim_magnitude*dstim);

  //Update NN
  neighbor=nearest_nbr_kdtree();

}


int Model::get_label(int nbr){
  return(labels(nbr,2));
}

int Model::get_neighbor(){

  return(neighbor);
  
}

ublas::vector<double> Model::get_state(){

  return(s);
  
}

double Model::get_signal(){
	double dp=0;
	for(int e=0;e<E;e++)
		dp+=s(e)*inv(e);
  	return(dp);
}


//****** BRUTE FORCE FOR DEBUG ******
int Model::nearest_nbr(){
  
  int least_nbr_id=0;
  double least_dist=1.e30;

  for(int n=0;n<N;n++){
    ublas::vector<double>nbr=ublas::zero_vector<float>(E);
    for(int e=0;e<E;e++){
      nbr(e)=features(n,e);
    }
    
    //Calc difference
    ublas::vector<double>delta=nbr-s;

    //Calc sum of squares
    double sum_sqr=0.0;
    for(int e=0;e<E;e++){
      sum_sqr+=delta(e)*delta(e);
    }

    //Calc dist 
    double dist=sqrt(sum_sqr);

    //Check for nearest nbr
    if(dist<least_dist){
      least_nbr_id=n;
      least_dist=dist;
    }

  }

  return(least_nbr_id);

}


int Model::nearest_nbr_kdtree(){

  int K=1;
  kdtree2_result_vector result;

  vector<float>query(E);
  
  
  for(int e=0;e<E;e++){
    query[e]=s(e);
  }

  tree->n_nearest(query,K,result); // search f
  
  return(result[0].idx);
  
}
