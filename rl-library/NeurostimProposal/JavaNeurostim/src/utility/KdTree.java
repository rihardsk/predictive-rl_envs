package utility;

import java.util.PriorityQueue;

/**
 * The main data structure, one for each k-d tree, pointing
 * to a tree of an indeterminate number of TDNodes.
 */
public class KdTree {

	private int idCount =0;
	
	public int squared(int x){
		return x*x;
	}
	public float squared(float x){
		return x*x;
	}
	public double squared(double x){
		return x*x;
	}

	//typedef struct {
	//  float lower, upper;
	//} interval;
	static public class Interval{
		double lower, upper;
		public Interval(){
			this.lower=0;
			this.upper=0;
		}
		public Interval(double lower,double upper){
			this.lower=lower;
			this.upper=upper;
		}
		public void set(Interval i){
			this.lower=i.lower;
			this.upper=i.upper;
		}
	}

	public class Result implements Comparable<Result>{
		double dis;
		public int idx;
		public Result(double distance,int neighbor){
			this.dis=distance;
			this.idx=neighbor;
		}
		public boolean isSmaller(Result e1, Result e2) {
			return (e1.dis < e2.dis);
		}
		/**
		 * Natural oredering: Descending order of distances.
		 */
		public int compareTo(Result e) {
			if(dis<e.dis)
				return 1;
			if(dis>e.dis)
				return -1;
			return 0;
		}
	}

	static public class ResultVector{

		public PriorityQueue<Result> pq = new PriorityQueue<Result>(); 

		public double maxValue() {
			return pq.peek().dis ; // very first element
		}

		public void offer(Result e) {
			pq.offer(e); // what a vector does.
		}
		public double popPushPeek(Result e) {
			// remove the maximum priority element on the queue and replace it
			// with 'e', and return its priority.
			//
			// here, it means replacing the first element [0] with e, and re heapifying.
			Result r=pq.poll();
			pq.offer(e);
			return pq.peek().dis;
		}
		public int size(){return pq.size();}

		public void sort(){//TODO
			pq.offer(pq.poll());
		}

	}
	//
//	search record substructure
	//
//	one of these is created for each search.
//	this holds useful information  to be used
//	during the search



	static double infinity = Double.MAX_VALUE;

	public class SearchRecord {

		private double[] qv; 
		private int dim;
		private boolean rearrange;
		private int nn; // , nfound;
		private double ballsize;
		private int centeridx, correltime;

		private ResultVector result;  // results
		private double[][] data; 
		private int[] ind; 
		// constructor

		public SearchRecord(double[] qv, KdTree tree,ResultVector result){
			this.qv=qv;
			this.result=result;
			this.data=tree.data;
			this.ind= tree.ind;
			this.dim = tree.dim;
			this.rearrange = tree.rearrange;
			this.ballsize = infinity; 
			this.nn = 0; 
		}

	}

	/**
	 * A node in the td-tree.  Many are created per tree dynamically..
	 */
	public class KDNode{
		private int id=0;
		
		public KDNode(int dim)
		{
			box= new Interval[dim];
			for(int i=0;i<dim;i++)
				box[i]=new Interval();
			left = right = null;
			id=++idCount;
		}
		public void print(){
			System.out.print("["+id+"]["+box.length+"]{");
			for(Interval i:box)
				System.out.printf("(%.3f %.3f)", i.lower,i.upper);
			System.out.println("}");
			System.out.println("[Left]");
			if(left==null)System.out.println("null");else left.print();
			System.out.println("[Right]");
			if(right==null)System.out.println("null");else right.print();
		}


		private int cutDim;                                 // dimension to cut; 
		private double cutVal, cutValLeft, cutValRight;  //cut value
		private int l,u;  // extents in index array for searching

		private Interval[] box; // [min,max] of the box enclosing all points

		private KDNode left, right;  // pointers to left and right nodes. 

		/**
		 * Utility 
		 */
		private double disFromBnd(double x, double amin, double amax) {
			if (x > amax) {
				return(x-amax); 
			} else if (x < amin)
				return (amin-x);
			else
				return 0;
		}

		/**
		 *  Recursive innermost core routine for searching.
		 */
		private void search(SearchRecord sr)
		{
			// This uses true distance to bounding box as the
			// criterion to search the secondary node. 
			//
			// This results in somewhat fewer searches of the secondary nodes
			// than 'search', which uses the vdiff vector,  but as this
			// takes more computational time, the overall performance may not
			// be improved in actual run time. 
			//

			if ( (left == null) && (right == null)) {
				// is a terminal node
				if (sr.nn == 0) {
					processTerminalNodeFixedball(sr);
				} else {
					processTerminalNode(sr);
				}
			} 
			else {
				KDNode ncloser, nfarther;

				double extra;
				double qval = sr.qv[cutDim];

				// value of the wall boundary on the cut dimension. 
				if (qval < cutVal) {
					ncloser = left;
					nfarther = right;
					extra = cutValRight-qval;
				} else {
					ncloser = right;
					nfarther = left;
					extra = qval-cutValLeft; 
				}

				if (ncloser != null) ncloser.search(sr);

				if ((nfarther != null) && (squared(extra) < sr.ballsize)) {
					// first cut
					if (nfarther.boxInSearchRange(sr)) {
						nfarther.search(sr); 
					}      
				}
			}
		}

		/**
		 * return true if the bounding box for this node is within the
		 * search range given by the searchvector and maximum ballsize in 'sr'.
		 */
		private boolean boxInSearchRange(SearchRecord sr)
		{
			//
			// does the bounding box, represented by minbox[*],maxbox[*]
			// have any point which is within 'sr.ballsize' to 'sr.qv'??
			//

			int dim = sr.dim;
			double dis2 =0; 
			double ballsize = sr.ballsize; 
			for (int i=0; i<dim;i++) {
				dis2 += squared(disFromBnd(sr.qv[i],box[i].lower,box[i].upper));
				if (dis2 > ballsize)
					return false;
			}
			return true;
		}

		private void check_queryInBound(SearchRecord sr){} // debugging only

		// for processing final buckets. 

		private void processTerminalNode(SearchRecord sr){
			int centeridx  = sr.centeridx;
			int correltime = sr.correltime;
			int nn = sr.nn; //TODO: nn is unsigned
			int dim = sr.dim;
			double ballsize = sr.ballsize;
			//
			boolean rearrange = sr.rearrange; 
			double[][] data = sr.data;

			boolean debug = false;

			if (debug) {
				System.out.println("Processing terminal node "+l+","+u);
				check_queryInBound(sr);
			}
			MainLoop:
			for (int i=l; i<=u;i++) {
				int indexofi;  // sr.ind[i]; 
				double dis;
				boolean early_exit; 

				if (rearrange) {
					early_exit = false;
					dis = 0;
					for (int k=0; k<dim; k++) {
						dis += squared(data[i][k] - sr.qv[k]);
						if (dis > ballsize) {
							early_exit=true; 
							break;
						}
					}
					if(early_exit) continue MainLoop; // next iteration of mainloop
					// why do we do things like this?  because if we take an early
					// exit (due to distance being too large) which is common, then
					// we need not read in the actual point index, thus saving main
					// memory bandwidth.  If the distance to point is less than the
					// ballsize, though, then we need the index.
					//
					indexofi = sr.ind[i];
				} else {
					// 
					// but if we are not using the rearranged data, then
					// we must always 
					indexofi = sr.ind[i];
					early_exit = false;
					dis = 0;
					for (int k=0; k<dim; k++) {
						dis += squared(data[indexofi][k] - sr.qv[k]);
						if (dis > ballsize) {
							early_exit= true; 
							break;
						}
					}
					if(early_exit) continue MainLoop; // next iteration of mainloop
				} // end if rearrange. 

				if (centeridx > 0) {
					// we are doing decorrelation interval
					if (Math.abs(indexofi-centeridx) < correltime) continue MainLoop; // skip this point. 
				}

				// here the point must be added to the list.
				//
				// two choices for any point.  The list so far is either
				// undersized, or it is not.
				//
				if (sr.result.size() < nn) {
					Result e=new Result(dis,indexofi);
					sr.result.offer(e); 
					if (sr.result.size() == nn) ballsize = sr.result.maxValue();
					// Set the ball radius to the largest on the list (maximum priority).
				} else {
					//
					// if we get here then the current node, has a squared 
					// distance smaller
					// than the last on the list, and belongs on the list.
					// 
					Result e=new Result(dis,indexofi);
					ballsize = sr.result.popPushPeek(e); 
				}
			} // main loop
			sr.ballsize = ballsize;
		}


		private void processTerminalNodeFixedball(SearchRecord sr){
			int centeridx  = sr.centeridx;
			int correltime = sr.correltime;
			int dim        = sr.dim;
			double ballsize = sr.ballsize;
			//
			boolean rearrange = sr.rearrange; 
			double[][] data =sr.data;

			for (int i=l; i<=u;i++) {
				int indexofi = sr.ind[i]; 
				double dis;
				boolean early_exit; 

				if (rearrange) {
					early_exit = false;
					dis = 0;
					for (int k=0; k<dim; k++) {
						dis += squared(data[i][ k] - sr.qv[k]);
						if (dis > ballsize) {
							early_exit=true; 
							break;
						}
					}
					if(early_exit) continue; // next iteration of mainloop
					// why do we do things like this?  because if we take an early
					// exit (due to distance being too large) which is common, then
					// we need not read in the actual point index, thus saving main
					// memory bandwidth.  If the distance to point is less than the
					// ballsize, though, then we need the index.
					//
					indexofi = sr.ind[i];
				} else {
					// 
					// but if we are not using the rearranged data, then
					// we must always 
					indexofi = sr.ind[i];
					early_exit = false;
					dis = 0;
					for (int k=0; k<dim; k++) {
						dis += squared(data[indexofi][k] - sr.qv[k]);
						if (dis > ballsize) {
							early_exit= true; 
							break;
						}
					}
					if(early_exit) continue; // next iteration of mainloop
				} // end if rearrange. 

				if (centeridx > 0) {
					// we are doing decorrelation interval
					if (Math.abs(indexofi-centeridx) < correltime) continue; // skip this point. 
				}

				{
					Result e=new Result(dis,indexofi);
					sr.result.pq.add(e);
				}
			}
		}
	}
	public final double[][] theData;
	// "theData" is a reference to the underlying multi_array of the
	// data to be included in the tree.

	public final int N;   // number of data points
	public int dim; //
	public boolean sortResults;  // USERS set to 'true'. 
	public final boolean rearrange; // are we rearranging? 

	public KdTree(double[][] data,boolean rearrange,int dim){
		this.theData=data;
		this.N = data.length;
		this.dim =data[0].length;
		this.sortResults =false;
		this.rearrange=rearrange; 
		this.root=null;
		this.data=null;
		this.ind=new int[N]; 
		//
		// initialize the constant references using this unusual C++
		// feature.
		//
		if (dim > 0) 
			this.dim = dim;

		buildTree();

		if (this.rearrange) {
			rearrangedData = new double[N][this.dim];
			// permute the data for it.
			for (int i=0; i<N; i++) {
				for (int j=0; j<this.dim; j++) {
					rearrangedData[i][j]= theData[ind[i]][j];
				}
			}
			this.data = rearrangedData;
		} else {
			this.data = theData;
		}
	}


	public KdTree(double[][] data){this(data,true,-1);};
	public KdTree(double[][] data,boolean rearrange){this(data,rearrange,-1);};
	public KdTree(double[][] data,int dim){this(data,true,dim);};

	public void print(){
		root.print();
	}

//	search routines
	/**
	 * search for n nearest to a given query vector 'qv' usin
	 * exhaustive slow search.  For debugging, usually.
	 */
	public void nNearestBruteForce(double[] qv, int nn, ResultVector result){
		result.pq.clear();
		for (int i=0; i<N; i++) {
			double dis = 0;
			for (int j=0; j<dim; j++) {
				dis += squared( theData[i][j] - qv[j]);
			}
			Result e= new Result(dis, i); 
			result.pq.add(e);
		}
		result.sort();
	}


	/**
	 * search for n nearest to a given query vector 'qv'.
	 */
	public void nNearest(double[] qv, int nn, ResultVector result){
		SearchRecord sr= new SearchRecord(qv,this,result);

		result.pq.clear(); 

		sr.centeridx = -1;
		sr.correltime = 0;
		sr.nn = nn; 

		root.search(sr); 

		if (sortResults) result.sort();

	}


	/**
	 * Search for 'nn' nearest to point [idxin] of the input data, excluding
	 * neighbors within correltime 
	 */
	public void nNearest_around_point(int idxin, int correltime, int nn,ResultVector result){
		double[] qv= new double[dim];  //  query vector

		result.pq.clear(); 

		for (int i=0; i<dim; i++) {
			qv[i]=theData[idxin][i]; 
		}
//		copy the query vector.

		{
			SearchRecord sr= new SearchRecord(qv, this, result);
//			construct the search record.
			sr.centeridx = idxin;
			sr.correltime = correltime;
			sr.nn = nn; 
			root.search(sr); 
		}

		if (sortResults) result.sort();
	}

	/**
	 * Search for all neighbors in ball of size (square Euclidean distance) r2.   Return number of neighbors in 'result.size()'
	 */
	public void rNearest(double[] qv, double r2,ResultVector result){
//		search for all within a ball of a certain radius
		SearchRecord sr= new SearchRecord(qv,this,result);
		//vector<float> vdiff(dim,0.0); 

		result.pq.clear(); 

		sr.centeridx = -1;
		sr.correltime = 0;
		sr.nn = 0; 
		sr.ballsize = r2; 

		root.search(sr); 

		if (sortResults) result.sort();

	}

	/**
	 * Like 'rNearest', but around existing point, with decorrelation interval. 
	 */
	public void rNearest_around_point(int idxin, int correltime, double r2,ResultVector result){
		double[] qv=new double[dim];  //  query vector

		result.pq.clear(); 

		for (int i=0; i<dim; i++) {
			qv[i]=theData[idxin][i]; 
		}
//		copy the query vector.

		{
			SearchRecord sr=new SearchRecord(qv, this, result);
//			construct the search record.
			sr.centeridx = idxin;
			sr.correltime = correltime;
			sr.ballsize = r2; 
			sr.nn = 0; 
			root.search(sr); 
		}

		if (sortResults) result.sort();

	}

	/**
	 * Count number of neighbors within square distance r2.
	 */
	public int rCount(double[] qv, double r2){
//		search for all within a ball of a certain radius
		ResultVector result= new ResultVector(); 
		SearchRecord sr= new SearchRecord(qv,this,result);

		sr.centeridx = -1;
		sr.correltime = 0;
		sr.nn = 0; 
		sr.ballsize = r2; 

		root.search(sr); 
		return result.pq.size();
	}

	/**
	 * Like rCount, c
	 */
	public int rCount_around_point(int idxin, int correltime, double r2){
		double[] qv=new double[dim];  //  query vector


		for (int i=0; i<dim; i++) {
			qv[i]=theData[idxin][i]; 
		}
		// copy the query vector.

		{
			ResultVector result= new ResultVector(); 
			SearchRecord sr= new SearchRecord(qv, this, result);
			// construct the search record.
			sr.centeridx = idxin;
			sr.correltime = correltime;
			sr.ballsize = r2; 
			sr.nn = 0; 
			root.search(sr); 
			return(result.size());
		}


	}

//	friend class kdtree2Node;
//	friend class searchrecord;
//	private:
//	private data members

	private KDNode root; // the root pointer

	private double[][] data;
//	pointing either to theData or an internal
//	rearranged data as necessary

	private int[] ind; 
//	the index for the tree leaves.  Data in a leaf with bounds [l,u] are
//	in  'theData[ind[l],*] to theData[ind[u],*]

	private double[][] rearrangedData;  
//	if rearrange is true then this is the rearranged data storage. 


	private static final int bucketsize = 12;  // global constant. 

//	building routines
	//public void setData(double[][] ind){/*TODO:unimplemented*/} 
	
	/**
	 * Builds the tree.  Used upon construction.
	 */
	private void buildTree(){
		for (int i=0; i<N; i++) 
			ind[i]=i; 
		root = buildTreeForRange(0,N-1,null); 

	} 

	private KDNode buildTreeForRange(int l, int u, KDNode parent){
		// recursive function to build 
		KDNode node = new KDNode(dim);
		// the newly created node. 

		if (u<l) {
			return(null); // no data in this node. 
		}


		if ((u-l) <= bucketsize) {
			// create a terminal node. 

			// always compute true bounding box for terminal node. 
			for (int i=0;i<dim;i++) {
				spreadInCoordinate(i,l,u,node.box[i]);
			}

			node.cutDim = 0; 
			node.cutVal = 0;
			node.l = l;
			node.u = u;
			node.left = node.right = null;


		} else {
			//
			// Compute an APPROXIMATE bounding box for this node.
			// if parent == NULL, then this is the root node, and 
			// we compute for all dimensions.
			// Otherwise, we copy the bounding box from the parent for
			// all coordinates except for the parent's cut dimension.  
			// That, we recompute ourself.
			//
			int c = -1;
			double maxspread = 0;
			int m; 

			for (int i=0;i<dim;i++) {
				if ((parent == null) || (parent.cutDim == i)) {
					spreadInCoordinate(i,l,u,node.box[i]);
				} else {
					node.box[i].set(parent.box[i]);
				}
				
				double spread = node.box[i].upper - node.box[i].lower; 
				if (spread>maxspread) {
					maxspread = spread;
					c=i; 
				}
			}

			// 
			// now, c is the identity of which coordinate has the greatest spread
			//


			double sum; 
			double average;

			sum = 0;
			for (int k=l; k <= u; k++) {
				sum += theData[ind[k]][c];
			}
			average = sum/(double)(u-l+1);

			m = selectOnCoordinateValue(c,average,l,u);



			// move the indices around to cut on dim 'c'.
			node.cutDim=c;
			node.l = l;
			node.u = u;

			node.left = buildTreeForRange(l,m,node);
			node.right = buildTreeForRange(m+1,u,node);

			if (node.right == null) {
				for (int i=0; i<dim; i++) 
					node.box[i].set(node.left.box[i]); 
				node.cutVal = node.left.box[c].upper;
				node.cutValLeft = node.cutValRight = node.cutVal;
			} 
			else if (node.left == null) {
				for (int i=0; i<dim; i++) 
					node.box[i].set(node.right.box[i]); 
				node.cutVal =  node.right.box[c].upper;
				node.cutValLeft = node.cutValRight = node.cutVal;
			} 
			else {
				node.cutValRight = node.right.box[c].lower;
				node.cutValLeft  = node.left.box[c].upper;
				node.cutVal = (node.cutValLeft + node.cutValRight) / 2f; 
				//
				// now recompute true bounding box as union of subtree boxes.
				// This is now faster having built the tree, being logarithmic in
				// N, not linear as would be from naive method.
				//
				for (int i=0; i<dim; i++) {
					node.box[i].upper = Math.max(node.left.box[i].upper,
							node.right.box[i].upper);

					node.box[i].lower = Math.min(node.left.box[i].lower,
							node.right.box[i].lower);
				}
			}
		}
		return node;
	}

	/**
	 * Move indices in ind[l..u] so that the elements in [l .. k] 
	 * are less than the [k+1..u] elmeents, viewed across dimension 'c'. 
	 */
	private void selectOnCoordinate(int c, int k, int l, int u){
		while (l < u) {
			int t = ind[l];
			int m = l;
			for (int i=l+1; i<=u; i++) {
				if ( theData[ind[i]][c] < theData[t][c]) {
					m++;
					Mathops.swap(ind, i, m);
				}
			} // for i 
			Mathops.swap(ind, l, m);

			if (m <= k) l = m+1;
			if (m >= k) u = m-1;
		} // while loop
	}
	/**
	 * Move indices in ind[l..u] so that the elements in [l .. return]
	 * are <= alpha, and hence are less than the [return+1..u]
	 * elements, viewed across dimension 'c'.
	 */
	private int selectOnCoordinateValue(int c, double alpha, int l, int u){
		int lb = l, ub = u;

		while (lb < ub) {
			if (theData[ind[lb]][c] <= alpha) {
				lb++; // good where it is.
			} else {
				Mathops.swap(ind, lb, ub);
				ub--;
			}
		}

		// here ub=lb
		if (theData[ind[lb]][c] <= alpha)
			return(lb);
		else
			return(lb-1);
	}

	private void spreadInCoordinate(int c, int l, int u, Interval interv)
	{
		// return the minimum and maximum of the indexed data between l and u in
		// sminOut and smaxOut.
		double smin, smax;
		double lmin, lmax;
		int i; 

		smin = theData[ind[l]][c];
		smax = smin;


		// process two at a time.
		for (i=l+2; i<= u; i+=2) {
			lmin = theData[ind[i-1]][c];
			lmax = theData[ind[i]  ][c];

			if (lmin > lmax) {
				double t = lmin;
				lmin = lmax;
				lmax = t;
			}
			if (smin > lmin) smin = lmin;
			if (smax <lmax) smax = lmax;
		}
		// is there one more element? 
		if (i == u+1) {
			double last = theData[ind[u]][c];
			if (smin>last) smin = last;
			if (smax<last) smax = last;
		}
		interv.lower = smin;
		interv.upper = smax;
//		System.out.format("Spread in coordinate %d=[%.6f,%.6f]\n",c,smin,smax);
	}
}
