/****************************
 *
 * Keith Bush (2009)
 *
 * Reasoning and Learning Lab
 * McGill University
 *
 ****************************/

#ifndef MODEL_H
#define MODEL_H

#include "kdtree2.h"

namespace ublas = boost::numeric::ublas;

class Model{

 public:

  //Constructor
  Model(char * init_file);

  //Base get/set functions
  int get_size();
  int get_dim();
  int get_Ndt();
  int get_seiz_label();
  double get_min();
  double get_max();
  double get_min_by_dimension(int dim);
  double get_max_by_dimension(int dim);


  //Initialize trajectory
  void reset();
  void reset(int index);
  void reset(const ublas::vector<double> & state);

  //Numerical Integration
  void step(int stim, double noise);

  //Extract Data
  int get_neighbor(); 
  int get_neighbor(const ublas::vector<double> & state);

  int get_label(int nbr);
  int get_label(const ublas::vector<double> & state);
  
  ublas::vector<double> get_state();
  double get_signal();

  //Fill in
  int convert_action_to_actionid(double);

 private:
  
  //Model features
  ublas::matrix<double,ublas::column_major> features;
  
  //Model labels
  ublas::matrix<double,ublas::column_major> labels;
  
  //Model state
  ublas::vector<double> s;
  
  //Reconstruction vector;
  ublas::vector<double> inv;

  //Prototype stimulation gradient
  ublas::vector<double> dstim;

  //Prototype noise magnitudes
  ublas::vector<double> noise_mags;

  //kdtree for nearest neighbor storage
  kdtree2* tree;

  //Current model neighbor
  int neighbor;  

  //Model params
  int N;                   //number of model states
  int E;                   //dimension of model state
  int L;                   //dimension of model labels

  int seiz_label;          //integer value of seizures in labeling
  double stim_magnitude;   //stimulation intensity
  int Ndt;                 //numerical steps per second (Hz^-1)

  //Helper functions
  int nearest_nbr();
  int nearest_nbr_kdtree();

};

#endif
