/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Higuchi2D_Grey_TriangArea.java
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

/**	Higuchi-Dimension for 2D images, method of triangle areas
 *
 * @author Nikolaus Sabathiel, Martin Reiss, Helmut Ahammer
 * @since 2020-11-20  
 */

public class Higuchi2D_Grey_TriangArea implements Higuchi2DMethods{

	private RandomAccessibleInterval<?> rai = null;
	private int              numK = 0;
	private double[]         totals = null;
	private double[]         eps = null;
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
	public Higuchi2D_Grey_TriangArea(RandomAccessibleInterval<?> rai, int numK, boolean skipZeroes) {
		this.rai = rai;
		this.numK = numK;
		this.skipZeroes = skipZeroes;
	}

	public Higuchi2D_Grey_TriangArea() {
	}

	/**
	 * This method calculates the Higuchi dimension in 2D
	 * @return totals
	 */
	@Override
	public double[] calcTotals() {

		if (eps == null) this.calcEps();
		
		System.out.println("Higuchi2D_TriangArea_HA: Preparing integer array...");
		
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
		double[] A = new double[3];  // Z  E  H
		double[] C = new double[3];  // C  A  B
		double[] E = new double[3];  // G  D  F
		double[] Z = new double[3];
					
		for (int k = 1; k <= numK; k++) {
			//operator.fireProgressChanged((int) ((float)(k)/(float)numK*100.0f));
			double[] L_vec = new double[k*k];
			int mm = 0;
			for (int m1 = 1; m1 <= k ; m1++) {
				for (int m2 = 1; m2 <= k ; m2++) {

					flr1 = Math.floor ((double)(N-m1)/(double)k);
				    flr2 = Math.floor ((double)(M-m2)/(double)k);
				    //norm = ((double)N-1.0) * ((double)M-1.0)/(flr1*(double)k * flr2*(double)k); 
					norm = ((double)N-1.0)/(flr1*(double)k) * ((double)M-1.0)/(flr2*(double)k);
					
					numZeroesDetected = 0;
					for (int i = 1; i <= flr1; i++) { 
						for (int j = 1; j <= flr2 ; j++) {
			
							
							x_a=m1+i*k-1;       y_a=m2+j*k-1;
							x_c=m1+(i-1)*k-1;   y_c=m2+j*k-1;
							x_e=m1+i*k-1;       y_e=m2+(j-1)*k-1;              
							x_z=m1+(i-1)*k-1;   y_z=m2+(j-1)*k-1;

//								int x_b=m1+(i+1)*k-1;    int y_b=m2+j*k-1;
//								int x_d=m1+i*k-1;        int y_d=m2+(j+1)*k-1;
//								int x_f=m1+(i+1)*k-1;    int y_f=m2+(j+1)*k-1;
//								int x_g=m1+(i-1)*k-1;    int y_g=m2+(j+1)*k-1;
//								int x_h=m1+(i+1)*k-1;    int y_h=m2+(j-1)*k-1;
							
		
//							double[] A = new double[3];  // Z  E  H
//							double[] C = new double[3];  // C  A  B
//							double[] E = new double[3];  // G  D  F
//							double[] Z = new double[3];
//								
//								double[] B = new double[3];
//								double[] D = new double[3];
//								double[] F = new double[3];
//								double[] G = new double[3];
//								double[] H = new double[3];
							
							A[0] = x_a;
							A[1] = y_a;
							//A[2] = array2D[x_a][y_a];
							pos[0] = x_a;
							pos[1] = y_a;
							ra.setPosition(pos);
							A[2] = (int) ra.get().getRealFloat();
						
							C[0] = x_c;
							C[1] = y_c;
							//C[2] = array2D[x_c][y_c];
							pos[0] = x_c;
							pos[1] = y_c;
							ra.setPosition(pos);
							C[2]= (int) ra.get().getRealFloat();	
							E[0] = x_e;
							E[1] = y_e;
							//E[2] = array2D[x_e][y_e];
							pos[0] = x_e;
							pos[1] = y_e;
							ra.setPosition(pos);
							E[2] = (int) ra.get().getRealFloat();
							Z[0] = x_z;
							Z[1] = y_z;
							//Z[2] = array2D[x_z][y_z];
							pos[0] = x_z;
							pos[1] = y_z;
							ra.setPosition(pos);
							Z[2] = (int) ra.get().getRealFloat();	
							
//								B[0] = x_b;  B[1] = y_b;  B[2] = array2D[x_b][y_c];
//								D[0] = x_d;  D[1] = y_d;  D[2] = array2D[x_d][y_d];
//								F[0] = x_f;  F[1] = y_f;  F[2] = array2D[x_f][y_f];
//								G[0] = x_g;  G[1] = y_g;  G[2] = array2D[x_g][y_g];
//								H[0] = x_h;  H[1] = y_h;  H[2] = array2D[x_a][y_h];
																							
							double areaACZ = TriangleArea(A, C, Z);
							//double areaADF = TriangleArea(A, D, F);
							//double areaAFB = TriangleArea(A, F, B);
							//double areaABH = TriangleArea(A, B, H);
							//double areaAHE = TriangleArea(A, H, E);
							//double areaAEZ = TriangleArea(A, E, Z);
							//double areaAZC = TriangleArea(A, Z, C);
							double areaAZE = TriangleArea(A, Z, E);
						  
							
							////L_vec[mm] += (areaAGD+areaADF+areaAFB+areaABH+areaAHE+areaAEZ+areaAZC+areaACG) * norm;
							//L_vec[mm] += (areaACZ+areaAZE) * norm;
							if (!skipZeroes) { //no skipping
								L_vec[mm] += (areaACZ+areaAZE);// * norm;
								} else { // check for zeroes
									if ((A[2] == 0) || (C[2] == 0) || (E[2] == 0) || (Z[2] == 0)) { //zero detected
										//do not add to the sum but correct later on norm
										numZeroesDetected += 1;
									} else { //no zeroes detected
										L_vec[mm] += (areaACZ+areaAZE);// * norm;
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
	 * This method calculates the area of a triangle in 3D
	 * @param A vector
	 * @param B vector
	 * @param C vector
	 * @return area
	 */
	private double TriangleArea(double[] A, double[] B, double[]C) {
		
		double[] Diff1 = new double[3];
		double[] Diff2 = new double[3];
		double[] Cross = new double[3];
		
		Diff1[0] = A[0] - B[0];
		Diff1[1] = A[1] - B[1];
		Diff1[2] = A[2] - B[2];	
		
		Diff2[0] = C[0] - A[0];
		Diff2[1] = C[1] - A[1];
		Diff2[2] = C[2] - A[2];
		
		//Cross product
		Cross[0] = Diff1[1]*Diff2[2] - Diff1[2]*Diff2[1];
		Cross[1] = Diff1[2]*Diff2[0] - Diff1[0]*Diff2[2];
		Cross[2] = Diff1[0]*Diff2[1] - Diff1[1]*Diff2[0];
		
		//Area	
		return Math.sqrt(Cross[0]*Cross[0] + Cross[1]*Cross[1] + Cross[2]*Cross[2])/2.0;
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
