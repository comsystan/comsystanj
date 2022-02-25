package at.csa.csaj.commons.utils;

import java.util.ArrayList;

public class DoubleSort {
	
	int[] indices;
	ArrayList<Double> arrayList;
	
	double[] doublesSorted;
	
	
	/*
	 * Constructor
	 * @Param double[]
	 */
	public DoubleSort(double[] array) {
		setDoubleArray(array);
	}
	
	/*
	 * Constructor
	 * @Param double[]
	 */
	public DoubleSort() {
	
	}
	
	public void setDoubleArray(double[] array) {
		
		indices = new int[array.length];
		arrayList = new ArrayList<Double>();
		for(int i = 0; i < array.length; i++) {			
			arrayList.add(array[i]);
			indices[i] = i;			
		}	
	}
	
	public double[] getSortedDoubles() {	
		doublesSorted = new double[arrayList.size()];
		for(int i = 0; i < doublesSorted.length; i++) {			
			doublesSorted[i] = arrayList.get(i);
		}
		return doublesSorted;
	}
	
	public int[] getSortedIndices() {		
		return indices;
	}
	
	public void sort() {	
    	double temp_value;
    	int temp_index;
        for(int k = 0; k < arrayList.size(); k++) {
            for(int l = k + 1; l < arrayList.size(); l++) {
                if(arrayList.get(k) >= arrayList.get(l)) {
                    temp_value = arrayList.get(k);
                    arrayList.set(k, arrayList.get(l));
                    arrayList.set(l, temp_value);
                    temp_index = indices[k];
                    indices[k] = indices[l];
                    indices[l] = temp_index;
                }
            }
        }
	}
}
