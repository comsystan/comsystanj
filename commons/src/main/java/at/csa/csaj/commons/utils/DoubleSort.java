package at.csa.csaj.commons.utils;


public class DoubleSort {
	
	int[]    indices;
	double[] doubles;
	double   doubleTemp;
	int      indexTemp;
	
	/*
	 * Constructor
	 * @Param double[]
	 */
	public DoubleSort(double[] ds) {
		int N = ds.length;
		doubles = ds;
		indices = new int[N];
		for (int i = 0; i < N; i++) {
			indices[i] = i;	
		}
		
	    for(int i = 0; i < N; i++) {
	        for(int j = i + 1; j < N; j++) {
	            if(doubles[j] <= doubles[i]) {
	                doubleTemp = doubles[i];
	                doubles[i] = doubles[j];
	                doubles[j] = doubleTemp;
	                indexTemp  = indices[i];
	                indices[i] = indices[j];
	                indices[j] = indexTemp;
	            }
	        }
	    }
	}
	
	public double[] getSortedDoubles() {
		return doubles;
	}
	
	public int[] getSortedIndices() {		
		return indices;
	}
}
