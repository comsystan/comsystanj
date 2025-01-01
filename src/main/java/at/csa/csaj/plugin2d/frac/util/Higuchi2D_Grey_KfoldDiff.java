/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Higuchi2D_Grey_KfoldDiff.java
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
package at.csa.csaj.plugin2d.frac.util;

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

/**	Higuchi-Dimension for 2D images, method k times difference
 *
 * @author Nikolaus Sabathiel, Martin Reiss, Helmut Ahammer
 * @since 2020-11-20  
 */

public class Higuchi2D_Grey_KfoldDiff implements Higuchi2DMethods{

	private RandomAccessibleInterval<?> rai = null;
	private int numK = 0;
	private double[] totals = null;
	private double[] eps = null;
	private boolean skipZeroes;
	
	
	@Override
	public double[] getTotals() {
		return totals;
	}

	@Override
	public void setTotals(double[] totals) {
		this.totals = totals;
	}

	@Override
	public double[] getEps() {
		return eps;
	}

	@Override
	public void setEps(double[] eps) {
		this.eps = eps;
	}


	/**
	 * This is the standard constructor
	 * 
	 * @param operator the {@link AbstractOperator} firing progress updates
	 */
	public Higuchi2D_Grey_KfoldDiff(RandomAccessibleInterval<?> rai, int numK, boolean skipZeroes) {
		this.rai = rai;
		this.numK = numK;
		this.skipZeroes = skipZeroes;
	}

	public Higuchi2D_Grey_KfoldDiff() {
	}

	/**
	 * This method calculates the Higuchi dimension in 2D
	 * @return totals
	 */
	@Override
	public double[] calcTotals() {

		if (eps == null) this.calcEps();
		
		//System.out.println("Higuchi2D_KfoldDiff_HA: Preparing integer array...");
		
		// Get size 
		long width = rai.dimension(0);
		long height = rai.dimension(1);
		RandomAccess<RealType<?>>ra = (RandomAccess<RealType<?>>) rai.randomAccess();
		
		// Prepare 2D integer array
		//int[][] array2D = new int[(int)width][(int)height];
		
//		for (int x = 0; x < width; x++) {
//			for (int y = 0; y < height; y++) {	
//				ra.setPosition(x, 0);
//				ra.setPosition(y, 1);
//				array2D[x][y] = (int) ra.get().getRealFloat();	
//			}
//		}
			
		double[] totals = new double[numK];
		long N = width;
		long M = height;
		long[] pos = new long[2];
					
		double flr1, flr2, norm;
		long   numZeroesDetected;
		int    x_a, y_a, x_c, y_c, x_e, y_e, x_z, y_z;
		int    A, C, E, Z;
		
		
		for (int k = 1; k <= numK; k++) {
				
			//operator.fireProgressChanged((int) ((float)(k)/(float)numK*100.0f));
			double[] L_vec = new double[k*k];
			int mm = 0;
			for (int m1 = 1; m1 <= k ; m1++) {
				for (int m2 = 1; m2 <= k ; m2++) {

					flr1 = Math.floor ((double)(N-m1)/(double)k);
					flr2 = Math.floor ((double)(M-m2)/(double)k);
					norm = (N-1.0) * (M-1.0)/(flr1*(double)k * flr2*(double)k); 
						
					numZeroesDetected = 0;
					for (int i = 1; i <= flr1; i++) { 
						for (int j = 1; j <= flr2 ; j++) {
							x_a=m1+i*k-1;       y_a=m2+j*k-1;
							x_c=m1+(i-1)*k-1;   y_c=m2+j*k-1;
							x_e=m1+i*k-1;       y_e=m2+(j-1)*k-1;              
							x_z=m1+(i-1)*k-1;   y_z=m2+(j-1)*k-1;

							//int A=array2D[x_a][y_a];    // Z  E  H
							//int C=array2D[x_c][y_c];    // C  A  B
							//int E=array2D[x_e][y_e];    // G  D  F
							//int Z=array2D[x_z][y_z];  
								
							pos[0] = x_a;
							pos[1] = y_a;
							ra.setPosition(pos);
							A = (int) ra.get().getRealFloat();
							
							pos[0] = x_c;
							pos[1] = y_c;
							ra.setPosition(pos);
							C = (int) ra.get().getRealFloat();	
							
							pos[0] = x_e;
							pos[1] = y_e;
							ra.setPosition(pos);
							E = (int) ra.get().getRealFloat();	
							
							pos[0] = x_z;
							pos[1] = y_z;
							ra.setPosition(pos);
							Z = (int) ra.get().getRealFloat();	
							
//							int x_b=m1+(i+1)*k-1;    int y_b=m2+j*k-1;
//							int x_d=m1+i*k-1;        int y_d=m2+(j+1)*k-1;
//							int x_f=m1+(i+1)*k-1;    int y_f=m2+(j+1)*k-1;
//							int x_g=m1+(i-1)*k-1;    int y_g=m2+(j+1)*k-1;
//							int x_h=m1+(i+1)*k-1;    int y_h=m2+(j-1)*k-1;
//								
//							int B=array2D[x_b][y_b];
//							int D=array2D[x_d][y_d];
//							int F=array2D[x_f][y_f];
//							int G=array2D[x_g][y_g];
//							int H=array2D[x_h][y_h];		
		        			
							if (!skipZeroes) { //no skipping
//							L_vec[mm] += ((3.0/2.0*Math.abs(C-Z)*k + 1.0/2.0*Math.abs(A-C)*k+
//										   3.0/2.0*Math.abs(E-Z)*k + 1.0/2.0*Math.abs(A-E)*k)); // * norm;
								
							L_vec[mm] += ((Math.abs(C-Z)*k + Math.abs(A-C)*k+
								               Math.abs(E-Z)*k + Math.abs(A-E)*k))/4.0;  // * norm;
								
//							L_vec[mm] += ((Math.abs(C-Z)*k + Math.abs(A-C)*k+
//							                  Math.abs(E-Z)*k + Math.abs(A-E)*k)+
//							                    
//							                 (Math.abs(C-G)*k + Math.abs(A-C)*k+
//							                  Math.abs(D-G)*k + Math.abs(A-D)*k)+
//							                   
//							                  (Math.abs(D-F)*k + Math.abs(A-D)*k+
//							                   Math.abs(B-F)*k + Math.abs(A-B)*k)+
//							                   
//							                  (Math.abs(B-H)*k + Math.abs(A-B)*k+
//							                   Math.abs(E-H)*k + Math.abs(A-E)*k))/8.0; // * norm;
							} else { // check for zeroes
								if ((A == 0) || (C == 0) || (E == 0) || (Z == 0)) { //zero detected
									//do not add to the sum but correct later on norm
									numZeroesDetected += 1;
								} else { //no zeroes detected
									L_vec[mm] += ((Math.abs(C-Z)*k + Math.abs(A-C)*k+
								               Math.abs(E-Z)*k + Math.abs(A-E)*k))/4.0;  // * norm;
								}
							}																																			
						} //j
					}  //i	
					//corrected norm
					//flr1*flr2 is the number of times that a value is usually added to L_vec (without skipping zeroes)
					//numZeroesDetected is the number of detected zeroes
					//(flr1*flr2 - numZeroesDetected) is the number of times that a value is added to L_vec with skipping zeroes
					double normCorrection = flr1*flr2/(flr1*flr2 - numZeroesDetected);
					L_vec[mm] = L_vec[mm] * norm * normCorrection;
					mm++;
				} //m2
			} //m1
			//Mean over all m's
			Double L_Sum = 0.0;
			for (int mm1 = 0; mm1 < k*k; mm1++) {			
				L_Sum =  L_Sum + L_vec[mm1] / ((double)(k*k));  //last k outside the brackets
			}
			totals[k-1] = L_Sum/(double)(k*k);
		} //k		
		return totals;
	}
	
	/**
	 * This method calculates the Boxes
	 * @return eps
	 */
	@Override
	public double[] calcEps() {

		eps = new double[numK];
		for (int n = 0; n < numK; n++) {
			eps[n] = (n + 1);// * (n + 1);
		}
		return eps;
	}	

	


}
