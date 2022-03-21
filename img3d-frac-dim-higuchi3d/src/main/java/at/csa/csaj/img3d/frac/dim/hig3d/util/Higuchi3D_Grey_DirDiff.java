/*-
 * #%L
 * Project: ImageJ2 plugin for computing fractal dimension with 3D Higuchi algorithms.
 * File: Higuchi3D_Grey_DirDiff.java
 * 
 * $Id$
 * $HeadURL$
 * 
 * This file is part of ComsystanJ software, hereinafter referred to as "this program".
 * %%
 * Copyright (C) 2020 - 2022 Comsystan Software
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
package at.csa.csaj.img3d.frac.dim.hig3d.util;

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

/**	Higuchi-Dimension for 3D image volumes, method of direct differences
  *
 * @author Moritz Hackhofer, Nikolaus Sabathiel, Martin Reiss, Helmut Ahammer
 * @since 2021-03-14  
 */

public class Higuchi3D_Grey_DirDiff implements Higuchi3DMethods{
	
	private RandomAccessibleInterval<?> rai = null;
	private int numK = 0;
	private long width = 0;
	private long height = 0;
	private long depth = 0;
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
	public Higuchi3D_Grey_DirDiff(RandomAccessibleInterval<?> rai, int numK, boolean skipZeroes) {
		this.rai        = rai;
		this.width      = rai.dimension(0);
		this.height     = rai.dimension(1);
		this.depth      = rai.dimension(2);
		this.numK       = numK;
		this.skipZeroes = skipZeroes;
	}

	public Higuchi3D_Grey_DirDiff() {
	}

	/**
	 * This method calculates the Higuchi dimension in 2D
	 * @return totals
	 */
	@Override
	public double[] calcTotals() {

		if (eps == null) this.calcEps();
		
		//System.out.println("Higuchi3D_DirDiff_HA: Preparing integer array...");
		
		// Get size 
		//long width = raiVolume.dimension(0);
		//long height = raiVolume.dimension(1);
		//long depth = raiVolume.dimension(2);
		//RandomAccess<RealType<?>>ra = (RandomAccess<RealType<?>>) raiVolume.randomAccess();
		

			
		double[] totals = new double[numK];
		long N = width;
	    long M = height;
		long O = depth;
		RandomAccess<RealType<?>>ra = (RandomAccess<RealType<?>>) rai.randomAccess();
		long[] pos = new long[3];
		
		double flr1, flr2,flr3, norm;
		long   numZeroesDetected;
		int    x_a1, y_a1, z_a1, x_c1, y_c1, z_c1, x_e1, y_e1, z_e1;
		int    x_a2, y_a2, z_a2;
		int    A1, C1, E1, A2;    	
		int    k, i, j, l, m1, m2, m3;
		
		double normCorrection;
			
		for (k = 1; k <= numK; k++) {
			//operator.fireProgressChanged((int) ((float)(k)/(float)numK*100.0f));
			double[] L_vec = new double[k*k*k];
			int mm = 0;
			for (m1 = 1; m1 <= k ; m1++) {
				for (m2 = 1; m2 <= k ; m2++) {
					for (m3 = 1; m3 <= k ; m3++) {

					flr1 = Math.floor ((double)(N-m1)/(double)k);
					flr2 = Math.floor ((double)(M-m2)/(double)k);
					flr3 = Math.floor ((double)(O-m3)/(double)k);
					norm = ((double)N-1.0)/(flr1*(double)k) * ((double)M-1.0)/(flr2*(double)k) * ((double)O-1.0)/(flr3*(double)k);
					
					numZeroesDetected = 0;
					for (i = 1; i < flr1; i++) { 
						for (j = 1; j < flr2; j++) {
							for (l = 1; l < flr3; l++) {
							
								x_a1=m1+i*k-1;      y_a1=m2+j*k-1;		z_a1=m3+l*k-1;
								x_a2=m1+i*k-1; ;    y_a2=m2+j*k-1;		z_a2=m3+(l-1)*k-1;
								
								x_c1=m1+(i-1)*k-1;  y_c1=m2+j*k-1;		z_c1=m3+l*k-1;
								
								x_e1=m1+i*k-1;      y_e1=m2+(j-1)*k-1;	z_e1=m3+l*k-1;
								
								// Central Pixel: A1, A1,A2,C1,E1 are used
								// Z0  E0  H0 		Z1  E1  H1 	 	 Z2  E2  H2
								// C0  A0  B0		C1  A1  B1		 C2  A2  B2
								// G0  D0  F0		G1  D1  F1		 G2  D2  F2		

								
								pos[0] = x_a1;
								pos[1] = y_a1;
								pos[2] = z_a1;
								ra.setPosition(pos);
								A1 = (int)ra.get().getRealFloat();
														
								pos[0] = x_a2;
								pos[1] = y_a2;
								pos[2] = z_a2;
								ra.setPosition(pos);
								A2 = (int)ra.get().getRealFloat();
							
								pos[0] = x_c1;
								pos[1] = y_c1;
								pos[2] = z_c1;
								ra.setPosition(pos);
								C1 = (int)ra.get().getRealFloat();
														
							
								pos[0] = x_e1;
								pos[1] = y_e1;
								pos[2] = z_e1;
								ra.setPosition(pos); 
								E1 = (int)ra.get().getRealFloat();
										
			 
							if (!skipZeroes) { //no skipping
								L_vec[mm] += (Math.abs(A1 - A2) + Math.abs(A1 - C1) + Math.abs(A1 - E1))/3;
								
							} else { // check for zeroes
								if ((A1 == 0) || (C1 == 0) || (E1 == 0) || (A2 == 0)) { //zero detected
										//do not add to the sum but correct later on norm
										numZeroesDetected += 1;
								} else { //no zeroes detected
									L_vec[mm] += (Math.abs(A1 - A2) + Math.abs(A1 - C1) + Math.abs(A1 - E1))/3;
								}
							}			
						} 
					}     	
					}
					//corrected norm
					//flr1*flr2*flr3 is the number of times that a value is usually added to L_vec (without skipping zeroes)
					//numZeroesDetected is the number of detected zeroes
					//(flr1*flr2*flr3 - numZeroesDetected) is the number of times that a value is added to L_vec with skipping zeroes
					normCorrection = flr1*flr2*flr3/(flr1*flr2*flr3 - numZeroesDetected);
					L_vec[mm] = L_vec[mm] * norm * normCorrection;
					mm++;
					
					}//m3
				} //m2
			}//m1
			//Mean over all m
			Double L_Sum = 0.0;
			for (int mm1 = 0; mm1 < k*k*k; mm1++) {			
				//L_Sum =  L_Sum + L_vec[mm1] / ((double)(k));  //last k outside the brackets
				
				L_Sum =  L_Sum + L_vec[mm1] / ((double)(k));  //last k outside the brackets
			}
			totals[k-1] = L_Sum/((double)(k*k*k));
		}//k	
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
			eps[n] = (n + 1);
		}
		return eps;
	}	

}
