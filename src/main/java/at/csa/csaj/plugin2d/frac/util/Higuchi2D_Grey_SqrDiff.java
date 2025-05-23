/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Higuchi2D_Grey_SqrDiff.java
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

/**	Higuchi-Dimension for 2D images, method of direct differences
 *
 * @author Nikolaus Sabathiel, Martin Reiss, Helmut Ahammer
 * @since 2020-11-20  
 */

public class Higuchi2D_Grey_SqrDiff implements Higuchi2DMethods{

	private RandomAccessibleInterval<?> rai = null;
	private int         numK = 0;
	private double[]    totals = null;
	private double[]    eps = null;
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
	public Higuchi2D_Grey_SqrDiff(RandomAccessibleInterval<?> rai, int numK, boolean skipZeroes) {
		this.rai = rai;
		this.numK = numK;
		this.skipZeroes = skipZeroes;
	}

	public Higuchi2D_Grey_SqrDiff() {
	}

	/**
	 * This method calculates the Box Counting dimension in 3D
	 * The stack of PlanarImages is first converted to a 3D boolean array
	 * @return totals
	 */
	@Override
	public double[] calcTotals() {

		if (eps == null) this.calcEps();
		
		System.out.println("Higuchi2D_SqrDiff_HA: Preparing integer array...");
		
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
			    norm = ((double)N-1.0) * ((double)M-1.0)/(flr1*(double)k * flr2*(double)k);
			    
			    numZeroesDetected = 0;				
				for (int i = 1; i <= flr1; i++) { 
					for (int j = 1; j <= flr2 ; j++) {
						
						x_a=m1+i*k-1;       y_a=m2+j*k-1;
						x_c=m1+(i-1)*k-1;   y_c=m2+j*k-1;
						x_e=m1+i*k-1;       y_e=m2+(j-1)*k-1;              
						//x_z=m1+(i-1)*k-1;   y_z=m2+(j-1)*k-1;

						// Z  E  H
						// C  A  B
						// G  D  F  
							
						pos[0] = x_a;
						pos[1] = y_a;
						ra.setPosition(pos);
						A= (int) ra.get().getRealFloat();
						
						pos[0] = x_c;
						pos[1] = y_c;
						ra.setPosition(pos);
						C= (int) ra.get().getRealFloat();	
						
						pos[0] = x_e;
						pos[1] = y_e;
						ra.setPosition(pos);
						E= (int) ra.get().getRealFloat();	
						
//						pos[0] = x_z;
//						pos[1] = y_z;
//						ra.setPosition(pos);
//						Z= (int) ra.get().getRealFloat();	
								        
						if (!skipZeroes) { //no skipping
//							L_vec[mm] += ((Z-C)*(Z-C)+
//				                          (A-C)*(A-C)+
//				                          (Z-E)*(Z-E)+
//				                          (A-E)*(A-E))/4.0;// * norm;	
							L_vec[mm] += ((A-C)*(A-C) + (A-E)*(A-E))/2.0;// * norm;	
							} else { // check for zeroes
								if ((A == 0) || (C == 0) || (E == 0)) { //zero detected
									//do not add to the sum but correct later on norm
									numZeroesDetected += 1;
								} else { //no zeroes detected
//									L_vec[mm] += ((Z-C)*(Z-C)+
//						                      (A-C)*(A-C)+
//						                      (Z-E)*(Z-E)+
//						                      (A-E)*(A-E))/4.0; // * norm;	
									L_vec[mm] += ((A-C)*(A-C) + (A-E)*(A-E))/2.0;// * norm;	
								}
							}
						} //j
					} //i	
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
				L_Sum =  L_Sum + L_vec[mm1] / ((double)(k*k));  
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
			eps[n] = (n + 1) *  (n + 1);
		}
		return eps;
	}	
}
