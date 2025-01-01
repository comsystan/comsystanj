/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: CsajUtil_DoubleSort.java
 * 
 * $Id$
 * $HeadURL$
 * 
 * This file is part of ComsystanJ software, hereinafter referred to as "this program".
 * %%
 * Copyright (C) 2024 - 2025 Comsystan Software
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package at.csa.csaj.commons;


public class CsajUtil_DoubleSort {
	
	int[]    indices;
	double[] doubles;
	double   doubleTemp;
	int      indexTemp;
	
	/*
	 * Constructor
	 * @Param double[]
	 */
	public CsajUtil_DoubleSort(double[] ds) {
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
