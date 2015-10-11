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
#include <cstdlib> 
#include <ctime> 
#include <cmath>

#include "matvecops.h"

namespace ublas = boost::numeric::ublas;
using namespace std;

ublas::vector<double> c(const ublas::vector<double> & u, const ublas::vector<double> & v){

  ublas::vector<double> result(u.size()+v.size());
  subrange(result,0,u.size()) = u;
  subrange(result,u.size(),(u.size()+v.size()))=v;
  return(result);

}

ublas::vector<double> c(const ublas::vector<double> & u, const double v){

  ublas::vector<double> result(u.size()+1);
  subrange(result,0,u.size()) = u;
  result(result.size()-1)=v;
  return(result);

}

ublas::vector<double> c(const double u, const ublas::vector<double> & v){

  ublas::vector<double> result(v.size()+1);
  result(0)=u;
  subrange(result,1,(v.size()+1)) = v;

  return(result);

}


double max(const ublas::vector<double> & u){

  double max = u(0);
  double curr=-1.0;
  for(int i=1;i<u.size();i++){
    if(u(i)>max){
      max=u(i);
    }
  }

  return(max);
}

int max(const ublas::vector<int> & u){

  int max = u(0);
  int curr=-1.0;
  for(int i=1;i<u.size();i++){
    if(u(i)>max){
      max=u(i);
    }
  }

  return(max);
}


ublas::vector<double> apply(const ublas::vector<double> & u, double (*fp)(double)){

  ublas::vector<double> result;
  result = u;

  for(int i=0; i<u.size(); i++){
    result(i)=(*fp)((float)u(i));
  }

  return(result);

}


double dist(const ublas::vector<double> & u, const ublas::vector<double> & v){
  
  double result = 0.0;
  
  for(int i=0; i<u.size(); i++){
    result+=pow(u(i)-v(i),2);
  }
  return(sqrt(result));

}



ublas::matrix<double,ublas::column_major> rbind(const ublas::matrix<double,ublas::column_major> & u, const ublas::vector<double> & v){
  
  ublas::matrix<double,ublas::column_major> newu(u.size1()+1,u.size2());

  for(int i=0;i<(newu.size1()-1);i++){
    for(int j=0;j<newu.size2();j++){
      newu(i,j)=u(i,j);
    }
  }

  for(int j =0;j<u.size2();j++){
    newu((newu.size1()-1),j) = v(j);
  }

  return(newu);

}


ublas::matrix<double,ublas::column_major> zero_map(const int M,const int N){
  ublas::matrix<double,ublas::column_major> map(M,N);

  for(int m=0;m<M;m++){
    for(int n=0;n<N;n++){
      map(m,n)=0.0;
    }
  }

  return(map);

}


double abssum(const ublas::matrix<double,ublas::column_major> & u){

  double result =0.0;
  for(int m=0;m<u.size1();m++){
    for(int n=0;n<u.size2();n++){
      result+=abs(u(m,n));
    }
  }

  return(result);

}
