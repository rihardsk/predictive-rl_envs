/****************************
 *
 * Keith Bush (2009)
 *
 * Reasoning and Learning Lab
 * McGill University
 *
 ****************************/

#ifndef MATVECOPS_H
#define MATVECOPS_H

#include <cassert>
#include <boost/numeric/bindings/traits/ublas_matrix.hpp>
#include <boost/numeric/bindings/traits/ublas_vector.hpp>
#include <boost/numeric/ublas/matrix_proxy.hpp>
#include <boost/numeric/ublas/vector_proxy.hpp>
#include <boost/numeric/ublas/io.hpp>

#include "matvecops.h"

namespace ublas = boost::numeric::ublas;
using namespace std;

ublas::vector<double> c(const ublas::vector<double> & u, const ublas::vector<double> & v);
ublas::vector<double> c(const ublas::vector<double> & u, const double v);
ublas::vector<double> c(const double u, const ublas::vector<double> & v);
double max(const ublas::vector<double> & u);
int max(const ublas::vector<int> & u);
ublas::vector<double> apply(const ublas::vector<double> & u, double (*fp)(double));

double dist(const ublas::vector<double> & u, const ublas::vector<double> & v);

ublas::matrix<double,ublas::column_major> rbind(const ublas::matrix<double,ublas::column_major> & u, const ublas::vector<double> & v);

ublas::matrix<double,ublas::column_major> zero_map(const int M,const int N);

double abssum(const ublas::matrix<double,ublas::column_major> & u);


#endif
