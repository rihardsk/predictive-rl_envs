/*
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.rlcommunity.btanner.agentLib.functionApproximators.CMAC;

import java.util.Random;

/**
 * This code is a direct port from C to Java of the code at 
 * <a target=_new href=http://rlai.cs.ualberta.ca/RLAI/RLtoolkit/tilecoding.html>the RLAI Tile Coding Page</a>.
 * 
 * It was a very quick port.  It has been tested to some degree, but as the license says, it is provided
 * with no warranty.  And the license might be a lie here, because I copied it from
 * the group's web page.
 * 
 * @author Brian Tanner
 * @author Richard Sutton
 * @author Mark Lee
 */
public class TileCoder {

    final static int MAX_NUM_VARS = 300;       // Maximum number of variables in a grid-tiling      
    final static int MAX_NUM_COORDS = 100;     // Maximum number of hashing coordinates      
    final static int MaxLONGINT = 2147483647;
    static int qstate[] = new int[TileCoder.MAX_NUM_VARS];
    static int base[] = new int[MAX_NUM_VARS];
    static int coordinates[] = new int[MAX_NUM_VARS * 2 + 1];   /* one interval number per relevant dimension */


    /**
     * Should this be a static method?
     * @param the_tiles Integer array to fill up with tile indices
     * @param tileStartOffset Where to start filling the array (used if calling multiple times with 1 array)
     * @param num_tilings   Number of tilings (number of array spots to fill)
     * @param memory_size   Maximum number for each array index
     * @param doubles       The array of double variables
     * @param ints          The array of int variables
     */
    public synchronized void tiles(
            int the_tiles[], // provided array contains returned tiles (tile indices)
            int tileStartOffset,
            int num_tilings, // number of tile indices to be returned in tiles       
            int memory_size, // total number of possible tiles
            double doubles[], // array of doubleing point variables
            int ints[]) // array of integer variables
    {
        int num_doubles = doubles.length;
        int num_ints = ints.length;
        int num_coordinates = num_doubles + num_ints + 1;

        for (int i = 0; i < num_ints; i++) {
            coordinates[num_doubles + 1 + i] = ints[i];
        }

        /* quantize state to integers (henceforth, tile widths == num_tilings) */
        for (int i = 0; i < num_doubles; i++) {
            qstate[i] = (int) (doubles[i] * num_tilings); //This used to be math.floor be we can just cast
            base[i] = 0;
        }

        int i = 0;
        /*compute the tile numbers */
        for (int j = 0; j < num_tilings; j++) {

            /* loop over each relevant dimension */
            for (i = 0; i < num_doubles; i++) {

                /* find coordinates of activated tile in tiling space */
                if (qstate[i] >= base[i]) {
                    coordinates[i] = qstate[i] - ((qstate[i] - base[i]) % num_tilings);
                } else {
                    coordinates[i] = qstate[i] + 1 + ((base[i] - qstate[i] - 1) % num_tilings) - num_tilings;
                }

                /* compute displacement of next tiling in quantized space */
                base[i] += 1 + (2 * i);
            }
            /* add additional indices for tiling and hashing_set so they hash differently */
            coordinates[i] = j;

            the_tiles[tileStartOffset + j] = (int) hash_UNH(coordinates, num_coordinates, memory_size, 449);
        }
    }

    /* hash_UNH
    Takes an array of integers and returns the corresponding tile after hashing 
     */
    static final int RNDSEQNUMBER = 16384;
    static Random theRand = new Random();
    static int rndseq[] = new int[RNDSEQNUMBER];
    static boolean first_call = true;

    long hash_UNH(int ints[], int num_ints, long m, int increment) {
        int i, k;
        long index = 0;
        long sum = 0;

        /* if first call to hashing, initialize table of random numbers */
        if (first_call) {
            for (k = 0; k < RNDSEQNUMBER; k++) {
                rndseq[k] = 0;
                for (i = 0; i < 4/*int(sizeof(int))*/; ++i) {
                    rndseq[k] = (rndseq[k] << 8) | (theRand.nextInt() & 0xff);
                }//do these need to change?
            }
            first_call = false;
        }

        for (i = 0; i < num_ints; i++) {
            /* add random table offset for this dimension and wrap around */
            index = ints[i];
            index += (increment * i);
            /* index %= RNDSEQNUMBER; */
            index = index & (RNDSEQNUMBER - 1);
            while (index < 0) {
                index += RNDSEQNUMBER;
            }

            /* add selected random number to sum */
//			System.out.println("Sum ("+sum+") += (long)rndseq["+(int)index+"] which is "+ (long)rndseq[(int)index]);
            sum += (long) rndseq[(int) index];
        }
        index = (int) (sum % m);
        while (index < 0) {
            index += m;
        }

        /* printf("index is %d \n", index); */

        return (index);
    }

//
//	int hash(int *ints, int num_ints, collision_table *ct);
//
//	/* hash
//	Takes an array of integers and returns the corresponding tile after hashing 
//	*/
//	int hash(int *ints, int num_ints, collision_table *ct)
//	{
//		int j;
//		long ccheck;
//		
//		ct->calls++;
//		j = hash_UNH(ints, num_ints, ct->m, 449);
//		ccheck = hash_UNH(ints, num_ints, MaxLONGINT, 457);
//		if (ccheck == ct->data[j])
//		    ct->clearhits++;
//		else if (ct->data[j] == -1) {
//			ct->clearhits++;
//		    ct->data[j] = ccheck; }
//		else if (ct->safe == 0)
//			ct->collisions++;
//		else {
//			long h2 = 1 + 2 * hash_UNH(ints,num_ints,(MaxLONGINT)/4,449);
//			int i = 0;
//			while (++i) {
//				ct->collisions++;
//				j = (j+h2) % (ct->m);
//				/*printf("collision (%d) \n",j);*/
//				if (i > ct->m) {printf("\nTiles: Collision table out of Memory"); exit(0);}
//				if (ccheck == ct->data[j]) break;
//				if (ct->data[j] == -1) {ct->data[j] = ccheck; break;}
//			}
//		}			
//		return j;
//	}
//
//
//	void collision_table::Save(std::ofstream &o)const{
//		o.write((char *)&m,sizeof(m));
//		
//		o.write((char *)data,sizeof(data)*m);
//		o.write((char *)&safe,sizeof(safe));
//		o.write((char *)&calls,sizeof(calls));
//		o.write((char *)&clearhits,sizeof(clearhits));
//		o.write((char *)&collisions,sizeof(collisions));
//	}
//
//
//	void collision_table::reset() {
//	    for (int i=0; i<m; i++) data[i] = -1;
//	    calls = 0;
//	    clearhits = 0;
//	    collisions = 0;
//	}
//
//	collision_table::collision_table(int size, int safety) {
//		int tmp = size;
//		while (tmp > 2){
//			if (tmp % 2 != 0) {
//				printf("\nSize of collision table must be power of 2 %d",size);
//				exit(0);
//			}
//			tmp /= 2;
//		}
//		data = new long[size];
//		m = size;
//		safe = safety;
//		reset();
//	}
//
//	collision_table::collision_table(std::ifstream &i) {
//		i.read((char *)&m,sizeof(m));
//		data = new long[m];
//		i.read((char *)data,sizeof(data)*m);
//		i.read((char *)&safe,sizeof(safe));
//		i.read((char *)&calls,sizeof(calls));
//		i.read((char *)&clearhits,sizeof(clearhits));
//		i.read((char *)&collisions,sizeof(collisions));
//	}
//	collision_table::~collision_table() {
//		delete[] data;
//	}
//
//	int collision_table::usage() {
//		int count = 0;
//		for (int i=0; i<m; i++) if (data[i] != -1) 
//		{
//			
//			count++;
//	    }
//			
//			return count;
//	}
//
//	void collision_table::print() {
//	    printf("Collision table: Safety : %d Usage : %d Size : %ld Calls : %ld Collisions : %ld\n",this->safe,this->usage(),this->m,this->calls,this->collisions);
//	}
//
//	void collision_table::save(int file) {
//		write(file, (char *) &m, sizeof(long));
//		write(file, (char *) &safe, sizeof(int));
//		write(file, (char *) &calls, sizeof(long));
//		write(file, (char *) &clearhits, sizeof(long));
//		write(file, (char *) &collisions, sizeof(long));
//		write(file, (char *) data, m*sizeof(long));
//	}
//
//	void collision_table::restore(int file) {
//		read(file, (char *) &m, sizeof(long));
//		read(file, (char *) &safe, sizeof(int));
//		read(file, (char *) &calls, sizeof(long));
//		read(file, (char *) &clearhits, sizeof(long));
//		read(file, (char *) &collisions, sizeof(long));
//		read(file, (char *) data, m*sizeof(long));
//	}
//
//	/*
//	 void collision_table::save(char *filename) {
//		 write(open(filename, O_BINARY | O_CREAT | O_WRONLY);
//	};
//
//	void collision_table::restore(char *filename) {
//		read(open(filename, O_BINARY | O_CREAT | O_WRONLY);
//	}
//	*/
//
//
//	int i_tmp_arr[MAX_NUM_VARS];
//	double f_tmp_arr[MAX_NUM_VARS];
//
//	// No ints
//	void tiles(int the_tiles[],int nt,int memory,double doubles[],int nf) {
//	    tiles(the_tiles,nt,memory,doubles,nf,i_tmp_arr,0);
//	}
//	void tiles(int the_tiles[],int nt,collision_table *ct,double doubles[],int nf) {
//	    tiles(the_tiles,nt,ct,doubles,nf,i_tmp_arr,0);
//	}
//
//	//one int
//	void tiles(int the_tiles[],int nt,int memory,double doubles[],int nf,int h1) {
//	    i_tmp_arr[0]=h1;
//	    tiles(the_tiles,nt,memory,doubles,nf,i_tmp_arr,1);
//	}
//	void tiles(int the_tiles[],int nt,collision_table *ct,double doubles[],int nf,int h1) {
//	    i_tmp_arr[0]=h1;
//	    tiles(the_tiles,nt,ct,doubles,nf,i_tmp_arr,1);
//	}
//
//	// two ints
//	void tiles(int the_tiles[],int nt,int memory,double doubles[],int nf,int h1,int h2) {
//	    i_tmp_arr[0]=h1;
//	    i_tmp_arr[1]=h2;
//	    tiles(the_tiles,nt,memory,doubles,nf,i_tmp_arr,2);
//	}
//	void tiles(int the_tiles[],int nt,collision_table *ct,double doubles[],int nf,int h1,int h2) {
//	    i_tmp_arr[0]=h1;
//	    i_tmp_arr[1]=h2;
//	    tiles(the_tiles,nt,ct,doubles,nf,i_tmp_arr,2);
//	}
//
//	// three ints
//	void tiles(int the_tiles[],int nt,int memory,double doubles[],int nf,int h1,int h2,int h3) {
//	    i_tmp_arr[0]=h1;
//	    i_tmp_arr[1]=h2;
//	    i_tmp_arr[2]=h3;
//	    tiles(the_tiles,nt,memory,doubles,nf,i_tmp_arr,3);
//	}
//	void tiles(int the_tiles[],int nt,collision_table *ct,double doubles[],int nf,int h1,int h2,int h3) {
//	    i_tmp_arr[0]=h1;
//	    i_tmp_arr[1]=h2;
//	    i_tmp_arr[2]=h3;
//	    tiles(the_tiles,nt,ct,doubles,nf,i_tmp_arr,3);
//	}
//
//	// one double, No ints
//	void tiles1(int the_tiles[],int nt,int memory,double f1) {
//	    f_tmp_arr[0]=f1;
//	    tiles(the_tiles,nt,memory,f_tmp_arr,1,i_tmp_arr,0);
//	}
//	void tiles1(int the_tiles[],int nt,collision_table *ct,double f1) {
//	    f_tmp_arr[0]=f1;
//	    tiles(the_tiles,nt,ct,f_tmp_arr,1,i_tmp_arr,0);
//	}
//
//	// one double, one int
//	void tiles1(int the_tiles[],int nt,int memory,double f1,int h1) {
//	    f_tmp_arr[0]=f1;
//	    i_tmp_arr[0]=h1;
//	    tiles(the_tiles,nt,memory,f_tmp_arr,1,i_tmp_arr,1);
//	}
//	void tiles1(int the_tiles[],int nt,collision_table *ct,double f1,int h1) {
//	    f_tmp_arr[0]=f1;
//	    i_tmp_arr[0]=h1;
//	    tiles(the_tiles,nt,ct,f_tmp_arr,1,i_tmp_arr,1);
//	}
//
//	// one double, two ints
//	void tiles1(int the_tiles[],int nt,int memory,double f1,int h1,int h2) {
//	    f_tmp_arr[0]=f1;
//	    i_tmp_arr[0]=h1;
//	    i_tmp_arr[1]=h2;
//	    tiles(the_tiles,nt,memory,f_tmp_arr,1,i_tmp_arr,2);
//	}
//	void tiles1(int the_tiles[],int nt,collision_table *ct,double f1,int h1,int h2) {
//	    f_tmp_arr[0]=f1;
//	    i_tmp_arr[0]=h1;
//	    i_tmp_arr[1]=h2;
//	    tiles(the_tiles,nt,ct,f_tmp_arr,1,i_tmp_arr,2);
//	}
//
//	// one double, three ints
//	void tiles1(int the_tiles[],int nt,int memory,double f1,int h1,int h2,int h3) {
//	    f_tmp_arr[0]=f1;
//	    i_tmp_arr[0]=h1;
//	    i_tmp_arr[1]=h2;
//	    i_tmp_arr[2]=h3;
//	    tiles(the_tiles,nt,memory,f_tmp_arr,1,i_tmp_arr,3);
//	}
//	void tiles1(int the_tiles[],int nt,collision_table *ct,double f1,int h1,int h2,int h3) {
//	    f_tmp_arr[0]=f1;
//	    i_tmp_arr[0]=h1;
//	    i_tmp_arr[1]=h2;
//	    i_tmp_arr[2]=h3;
//	    tiles(the_tiles,nt,ct,f_tmp_arr,1,i_tmp_arr,3);
//	}
//
//	// two doubles, No ints
//	void tiles2(int the_tiles[],int nt,int memory,double f1,double f2) {
//	    f_tmp_arr[0]=f1;
//	    f_tmp_arr[1]=f2;
//	    tiles(the_tiles,nt,memory,f_tmp_arr,2,i_tmp_arr,0);
//	}
//	void tiles2(int the_tiles[],int nt,collision_table *ct,double f1,double f2) {
//	    f_tmp_arr[0]=f1;
//	    f_tmp_arr[1]=f2;
//	    tiles(the_tiles,nt,ct,f_tmp_arr,2,i_tmp_arr,0);
//	}
//
//	// two doubles, one int
//	void tiles2(int the_tiles[],int nt,int memory,double f1,double f2,int h1) {
//	    f_tmp_arr[0]=f1;
//	    f_tmp_arr[1]=f2;
//	    i_tmp_arr[0]=h1;
//	    tiles(the_tiles,nt,memory,f_tmp_arr,2,i_tmp_arr,1);
//	}
//	void tiles2(int the_tiles[],int nt,collision_table *ct,double f1,double f2,int h1) {
//	    f_tmp_arr[0]=f1;
//	    f_tmp_arr[1]=f2;
//	    i_tmp_arr[0]=h1;
//	    tiles(the_tiles,nt,ct,f_tmp_arr,2,i_tmp_arr,1);
//	}
//
//	// two doubles, two ints
//	void tiles2(int the_tiles[],int nt,int memory,double f1,double f2,int h1,int h2) {
//	    f_tmp_arr[0]=f1;
//	    f_tmp_arr[1]=f2;
//	    i_tmp_arr[0]=h1;
//	    i_tmp_arr[1]=h2;
//	    tiles(the_tiles,nt,memory,f_tmp_arr,2,i_tmp_arr,2);
//	}
//	void tiles2(int the_tiles[],int nt,collision_table *ct,double f1,double f2,int h1,int h2) {
//	    f_tmp_arr[0]=f1;
//	    f_tmp_arr[1]=f2;
//	    i_tmp_arr[0]=h1;
//	    i_tmp_arr[1]=h2;
//	    tiles(the_tiles,nt,ct,f_tmp_arr,2,i_tmp_arr,2);
//	}
//
//	// two doubles, three ints
//	void tiles2(int the_tiles[],int nt,int memory,double f1,double f2,int h1,int h2,int h3) {
//	    f_tmp_arr[0]=f1;
//	    f_tmp_arr[1]=f2;
//	    i_tmp_arr[0]=h1;
//	    i_tmp_arr[1]=h2;
//	    i_tmp_arr[2]=h3;
//	    tiles(the_tiles,nt,memory,f_tmp_arr,2,i_tmp_arr,3);
//	}
//	void tiles2(int the_tiles[],int nt,collision_table *ct,double f1,double f2,int h1,int h2,int h3) {
//	    f_tmp_arr[0]=f1;
//	    f_tmp_arr[1]=f2;
//	    i_tmp_arr[0]=h1;
//	    i_tmp_arr[1]=h2;
//	    i_tmp_arr[2]=h3;
//	    tiles(the_tiles,nt,ct,f_tmp_arr,2,i_tmp_arr,3);
//	}
//
//	void tileswrap(
//				   int the_tiles[],               // provided array contains returned tiles (tile indices)
//				   int num_tilings,           // number of tile indices to be returned in tiles       
//				   int memory_size,           // total number of possible tiles
//				   double doubles[],            // array of doubleing point variables
//				   int num_doubles,            // number of doubleing point variables
//				   int wrap_widths[],         // array of widths (length and units as in doubles)
//				   int ints[],				  // array of integer variables
//				   int num_ints)             // number of integer variables
//	{
//		int i,j;
//		int qstate[MAX_NUM_VARS];
//		int base[MAX_NUM_VARS];
//	    int wrap_widths_times_num_tilings[MAX_NUM_VARS];
//		int coordinates[MAX_NUM_VARS * 2 + 1];   /* one interval number per relevant dimension */
//		int num_coordinates = num_doubles + num_ints + 1;
//		
//		for (int i=0; i<num_ints; i++) coordinates[num_doubles+1+i] = ints[i];
//	    
//		/* quantize state to integers (henceforth, tile widths == num_tilings) */
//	    for (i = 0; i < num_doubles; i++) {
//	    	qstate[i] = (int) floor(doubles[i] * num_tilings);
//	    	base[i] = 0;
//	    	wrap_widths_times_num_tilings[i] = wrap_widths[i] * num_tilings;
//	    }
//	    
//	    /*compute the tile numbers */
//	    for (j = 0; j < num_tilings; j++) {
//			
//			/* loop over each relevant dimension */
//			for (i = 0; i < num_doubles; i++) {
//				
//	    		/* find coordinates of activated tile in tiling space */
//				if (qstate[i] >= base[i])
//					coordinates[i] = qstate[i] - ((qstate[i] - base[i]) % num_tilings);
//				else
//					coordinates[i] = qstate[i]+1 + ((base[i] - qstate[i] - 1) % num_tilings) - num_tilings;
//				if (wrap_widths[i] != 0) coordinates[i] = coordinates[i] % wrap_widths_times_num_tilings[i];
//				if (coordinates[i] < 0) {
//					while (coordinates[i] < 0)
//	                    coordinates[i] += wrap_widths_times_num_tilings[i];
//				}
//				/* compute displacement of next tiling in quantized space */
//				base[i] += 1 + (2 * i);
//			}
//			/* add additional indices for tiling and hashing_set so they hash differently */
//			coordinates[i] = j;
//			
//			the_tiles[j] = hash_UNH(coordinates, num_coordinates, memory_size, 449);
//		}
//		return;
//	}
//
//	void tileswrap(
//				   int the_tiles[],               // provided array contains returned tiles (tile indices)
//				   int num_tilings,           // number of tile indices to be returned in tiles       
//				   collision_table *ctable,   // total number of possible tiles
//				   double doubles[],            // array of doubleing point variables
//				   int num_doubles,            // number of doubleing point variables
//				   int wrap_widths[],         // array of widths (length and units as in doubles)
//				   int ints[],				  // array of integer variables
//				   int num_ints)             // number of integer variables
//	{
//		int i,j;
//		int qstate[MAX_NUM_VARS];
//		int base[MAX_NUM_VARS];
//	    int wrap_widths_times_num_tilings[MAX_NUM_VARS];
//		int coordinates[MAX_NUM_VARS * 2 + 1];   /* one interval number per relevant dimension */
//		int num_coordinates = num_doubles + num_ints + 1;
//		
//		for (int i=0; i<num_ints; i++) coordinates[num_doubles+1+i] = ints[i];
//	    
//		/* quantize state to integers (henceforth, tile widths == num_tilings) */
//	    for (i = 0; i < num_doubles; i++) {
//	    	qstate[i] = (int) floor(doubles[i] * num_tilings);
//	    	base[i] = 0;
//	    	wrap_widths_times_num_tilings[i] = wrap_widths[i] * num_tilings;
//	    }
//	    
//	    /*compute the tile numbers */
//	    for (j = 0; j < num_tilings; j++) {
//			
//			/* loop over each relevant dimension */
//			for (i = 0; i < num_doubles; i++) {
//				
//	    		/* find coordinates of activated tile in tiling space */
//				if (qstate[i] >= base[i])
//					coordinates[i] = qstate[i] - ((qstate[i] - base[i]) % num_tilings);
//				else
//					coordinates[i] = qstate[i]+1 + ((base[i] - qstate[i] - 1) % num_tilings) - num_tilings;
//					        
//				if (wrap_widths[i] != 0) coordinates[i] = coordinates[i] % wrap_widths_times_num_tilings[i];
//				if (coordinates[i] < 0) {
//					while (coordinates[i] < 0)
//	                    coordinates[i] += wrap_widths_times_num_tilings[i];
//				}
//				/* compute displacement of next tiling in quantized space */
//				base[i] += 1 + (2 * i);
//			}
//			/* add additional indices for tiling and hashing_set so they hash differently */
//			coordinates[i] = j;
//			
//			the_tiles[j] = hash(coordinates, num_coordinates,ctable);
//		}
//		return;
//	}
//
//	
//	
    //#define MaxLONGINT (((2147483647+1)/4)-1)  

//	void tiles(
//			   int the_tiles[],               // provided array contains returned tiles (tile indices)
//			   int num_tilings,           // number of tile indices to be returned in tiles       
//			   int memory_size,           // total number of possible tiles
//			   double doubles[],            // array of doubleing point variables
//			   int num_doubles,            // number of doubleing point variables
//			   int ints[],				  // array of integer variables
//			   int num_ints);             // number of integer variables
//
//	class collision_table : public swSerializable {
//	public:
//	    collision_table(int,int);
//		collision_table(std::ifstream &i);
//	    ~collision_table();
//	    long m;
//	    long *data;
//	    int safe;
//	    long calls;
//	    long clearhits;
//	    long collisions;
//	    void reset();
//	    int usage();
//	    void print();
//	    void save(int);
//	    void restore(int);
//		virtual void Save(std::ofstream &o)const;
//
//	};
//
//
//	void tiles(
//			   int the_tiles[],               // provided array contains returned tiles (tile indices)
//			   int num_tilings,           // number of tile indices to be returned in tiles       
//			   collision_table *ctable,   // total number of possible tiles
//			   double doubles[],            // array of doubleing point variables
//			   int num_doubles,            // number of doubleing point variables
//			   int ints[],				  // array of integer variables
//			   int num_ints);             // number of integer variables
//
//	int hash_UNH(int *ints, int num_ints, long m, int increment);
//	int hash(int *ints, int num_ints, collision_table *ctable);
//
//	// no ints
//	void tiles(int the_tiles[],int nt,int memory,double doubles[],int nf);
//	void tiles(int the_tiles[],int nt,collision_table *ct,double doubles[],int nf);
//
//
//	// one int
//	void tiles(int the_tiles[],int nt,int memory,double doubles[],int nf,int h1);
//	void tiles(int the_tiles[],int nt,collision_table *ct,double doubles[],int nf,int h1);
//
//	// two ints
//	void tiles(int the_tiles[],int nt,int memory,double doubles[],int nf,int h1,int h2);
//	void tiles(int the_tiles[],int nt,collision_table *ct,double doubles[],int nf,int h1,int h2);
//
//	// three ints
//	void tiles(int the_tiles[],int nt,int memory,double doubles[],int nf,int h1,int h2,int h3);
//	void tiles(int the_tiles[],int nt,collision_table *ct,double doubles[],int nf,int h1,int h2,int h3);
//
//	// one double, no ints
//	void tiles1(int the_tiles[],int nt,int memory,double f1);
//	void tiles1(int the_tiles[],int nt,collision_table *ct,double f1);
//
//	// one double, one int
//	void tiles1(int the_tiles[],int nt,int memory,double f1,int h1);
//	void tiles1(int the_tiles[],int nt,collision_table *ct,double f1,int h1);
//
//	// one double, two ints
//	void tiles1(int the_tiles[],int nt,int memory,double f1,int h1,int h2);
//	void tiles1(int the_tiles[],int nt,collision_table *ct,double f1,int h1,int h2);
//
//	// one double, three ints
//	void tiles1(int the_tiles[],int nt,int memory,double f1,int h1,int h2,int h3);
//	void tiles1(int the_tiles[],int nt,collision_table *ct,double f1,int h1,int h2,int h3);
//
//	// two doubles, no ints
//	void tiles2(int the_tiles[],int nt,int memory,double f1,double f2);
//	void tiles2(int the_tiles[],int nt,collision_table *ct,double f1,double f2);
//
//	// two doubles, one int
//	void tiles2(int the_tiles[],int nt,int memory,double f1,double f2,int h1);
//	void tiles2(int the_tiles[],int nt,collision_table *ct,double f1,double f2,int h1);
//
//	// two doubles, two ints
//	void tiles2(int the_tiles[],int nt,int memory,double f1,double f2,int h1,int h2);
//	void tiles2(int the_tiles[],int nt,collision_table *ct,double f1,double f2,int h1,int h2);
//
//	// two doubles, three ints
//	void tiles2(int the_tiles[],int nt,int memory,double f1,double f2,int h1,int h2,int h3);
//	void tiles2(int the_tiles[],int nt,collision_table *ct,double f1,double f2,int h1,int h2,int h3);
//
//	void tileswrap(
//				   int the_tiles[],               // provided array contains returned tiles (tile indices)
//				   int num_tilings,           // number of tile indices to be returned in tiles       
//				   int memory_size,           // total number of possible tiles
//				   double doubles[],            // array of doubleing point variables
//				   int num_doubles,            // number of doubleing point variables
//				   int wrap_widths[],         // array of widths (length and units as in doubles)
//				   int ints[],				  // array of integer variables
//				   int num_ints);             // number of integer variables
//
//	void tileswrap(
//				   int the_tiles[],               // provided array contains returned tiles (tile indices)
//				   int num_tilings,           // number of tile indices to be returned in tiles       
//				   collision_table *ctable,   // total number of possible tiles
//				   double doubles[],            // array of doubleing point variables
//				   int num_doubles,            // number of doubleing point variables
//				   int wrap_widths[],         // array of widths (length and units as in doubles)
//				   int ints[],				  // array of integer variables
//				   int num_ints);             // number of integer variables
//
//
//
//	#endif
}
