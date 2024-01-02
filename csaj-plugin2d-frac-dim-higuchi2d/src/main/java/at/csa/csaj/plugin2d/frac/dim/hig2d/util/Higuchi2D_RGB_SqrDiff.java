/*-
 * #%L
 * Project: ImageJ2 plugin for computing fractal dimension with 2D Higuchi algorithms.
 * File: Higuchi2D_RGB_SqrDiff.java
 * 
 * $Id$
 * $HeadURL$
 * 
 * This file is part of ComsystanJ software, hereinafter referred to as "this program".
 * %%
 * Copyright (C) 2020 - 2024 Comsystan Software
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
package at.csa.csaj.plugin2d.frac.dim.hig2d.util;

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

/**	Higuchi-Dimension for RGB images
*
* @author Nikolaus Sabathiel, Martin Reiss, Helmut Ahammer
* @since 2020-11-23  
*/

public class Higuchi2D_RGB_SqrDiff implements Higuchi2DMethods{

	private RandomAccessibleInterval<?> rai = null;
	private int         numK     = 0;
	private double[]    totals   = null;
	private double[]    eps      = null;
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
	public Higuchi2D_RGB_SqrDiff(RandomAccessibleInterval<?> rai, int numK, boolean skipZeroes)  {
		this.rai   = rai;
		this.numK  = numK;
		this.skipZeroes = skipZeroes;
	}

	public Higuchi2D_RGB_SqrDiff() {
	}

	/**
	 * This method calculates the Higuchi dimension in 2D for RGB iamges
	 * @return totals
	 */
	@Override
	public double[] calcTotals() {

		if (eps == null) this.calcEps();
		
		//System.out.println("Higuchi2D_RGB: Preparing integer stack...");
		
		// Get size 
		long width = rai.dimension(0);
		long height = rai.dimension(1);
		RandomAccess<RealType<?>>ra = (RandomAccess<RealType<?>>) rai.randomAccess();


//		// Prepare 3D boolean array
//		int[][] array2D_0 = new int[width][height];
//		int[][] array2D_1 = new int[width][height];
//		int[][] array2D_2 = new int[width][height];
		
//		Raster r = pi.getData();
//		for (int x = 0; x < width; x++) {
//			for (int y = 0; y < height; y++) {		
//				array2D_0[x][y] = r.getSample(x, y, 0);
//				array2D_1[x][y] = r.getSample(x, y, 1);
//				array2D_2[x][y] = r.getSample(x, y, 2);
//			}
//		}
		
		double[] totals = new double[numK];
		long N = width;
		long M = height;
		long[] pos = new long[3];
		
		double flr1, flr2, norm;
		long   numZeroesDetected;					
		double A_blue,B_blue,C_blue,D_blue,E_blue,F_blue,G_blue,H_blue,Z_blue;
		double A_green,B_green,C_green,D_green,E_green,F_green,G_green,H_green,Z_green;
		double A_red,B_red,C_red,D_red,E_red,F_red,G_red,H_red,Z_red;
		double sumDiff_Blue, sumDiff_Red,sumDiff_Green;	
			
		for (int k = 1; k <=numK; k++) {
				
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
						for (int j = 1; j <= flr2; j++) {			
				        		    
//							//RED------------------------------------------
//							A_red=array2D_0[m1+i*k-1]    [m2+j*k-1];    // Z  E  H
//					        C_red=array2D_0[m1+(i-1)*k-1][m2+j*k-1];    // C  A  B
//					        E_red=array2D_0[m1+i*k-1]    [m2+(j-1)*k-1];// G  D  F 
//					        Z_red=array2D_0[m1+(i-1)*k-1][m2+(j-1)*k-1];
//					        
//					        //B_red=array2D_0[m1+(i+1)*k-1][m2+j*k-1];    
//					        //D_red=array2D_0[m1+i*k-1]    [m2+(j+1)*k-1];
//					        //F_red=array2D_0[m1+(i+1)*k-1][m2+(j+1)*k-1];
//					        //G_red=array2D_0[m1+(i-1)*k-1][m2+(j+1)*k-1];
//					        //H_red=array2D_0[m1+(i+1)*k-1][m2+(j-1)*k-1];
//					    		    			
//							//Green------------------------------------------					
//							A_green=array2D_1[m1+i*k-1]    [m2+j*k-1];		        
//					        C_green=array2D_1[m1+(i-1)*k-1][m2+j*k-1]; 
//					        E_green=array2D_1[m1+i*k-1]    [m2+(j-1)*k-1];
//					        Z_green=array2D_1[m1+(i-1)*k-1][m2+(j-1)*k-1];
//					        //B_green=array2D_1[m1+(i+1)*k-1][m2+j*k-1];
//					        //D_green=array2D_1[m1+i*k-1]    [m2+(j+1)*k-1];
//					        //F_green=array2D_1[m1+(i+1)*k-1][m2+(j+1)*k-1];
//					        //G_green=array2D_1[m1+(i-1)*k-1][m2+(j+1)*k-1];
//					        //H_green=array2D_1[m1+(i+1)*k-1][m2+(j-1)*k-1];
//					      					        
//				        		            
//							//BLUE------------------------------------------		
//							A_blue=array2D_2[m1+i*k-1]    [m2+j*k-1];      	  
//					        C_blue=array2D_2[m1+(i-1)*k-1][m2+j*k-1];           
//					        E_blue=array2D_2[m1+i*k-1]    [m2+(j-1)*k-1];
//					        Z_blue=array2D_2[m1+(i-1)*k-1][m2+(j-1)*k-1];
//					        //B_blue=array2D_2[m1+(i+1)*k-1][m2+j*k-1];      
//					        //D_blue=array2D_2[m1+i*k-1]    [m2+(j+1)*k-1];
//					        //F_blue=array2D_2[m1+(i+1)*k-1][m2+(j+1)*k-1];
//					        //G_blue=array2D_2[m1+(i-1)*k-1][m2+(j+1)*k-1];
//					        //H_blue=array2D_2[m1+(i+1)*k-1][m2+(j-1)*k-1];
				        
					        //A-----------------------------------
							pos[0] = m1+i*k-1;
							pos[1] = m2+j*k-1;
							pos[2] = 0; //R
							ra.setPosition(pos);
							A_red= ra.get().getRealDouble();
							pos[2] = 1; //G
							ra.setPosition(pos);
							A_green= ra.get().getRealDouble();
							pos[2] = 2; //B
							ra.setPosition(pos);
							A_blue= ra.get().getRealDouble();
							
							//C-----------------------------------
							pos[0] = m1+(i-1)*k-1;
							pos[1] = m2+j*k-1;
							pos[2] = 0; //R
							ra.setPosition(pos);
							C_red= ra.get().getRealDouble();
							pos[2] = 1; //G
							ra.setPosition(pos);
							C_green= ra.get().getRealDouble();
							pos[2] = 2; //B
							ra.setPosition(pos);
							C_blue= ra.get().getRealDouble();
							
							//E-----------------------------------
							pos[0] = m1+i*k-1;
							pos[1] = m2+(j-1)*k-1;
							pos[2] = 0; //R
							ra.setPosition(pos);
							E_red= ra.get().getRealDouble();
							pos[2] = 1; //G
							ra.setPosition(pos);
							E_green= ra.get().getRealDouble();
							pos[2] = 2; //B
							ra.setPosition(pos);
							E_blue= ra.get().getRealDouble();
						
							//Z-----------------------------------
							pos[0] = m1+(i-1)*k-1;
							pos[1] = m2+(j-1)*k-1;
							pos[2] = 0; //R
							ra.setPosition(pos);
							Z_red= ra.get().getRealDouble();
							pos[2] = 1; //G
							ra.setPosition(pos);
							Z_green= ra.get().getRealDouble();
							pos[2] = 2; //B
							ra.setPosition(pos);
							Z_blue= ra.get().getRealDouble();
							
							if (!skipZeroes) { //no skipping
								  sumDiff_Red = Math.abs(A_red-C_red)*Math.abs(A_red-C_red)+
					        		      Math.abs(A_red-E_red)*Math.abs(A_red-E_red)+
					        		      Math.abs(Z_red-E_red)*Math.abs(Z_red-E_red)+
					        		      Math.abs(Z_red-C_red)*Math.abs(Z_red-C_red);
						    
								  sumDiff_Green = Math.abs(A_green-C_green)*Math.abs(A_green-C_green)+
						        		          Math.abs(A_green-E_green)*Math.abs(A_green-E_green)+
						        		          Math.abs(Z_green-E_green)*Math.abs(Z_green-E_green)+
						        		          Math.abs(Z_green-C_green)*Math.abs(Z_green-C_green);		        
								        
								 sumDiff_Blue = Math.abs(A_blue-C_blue)*Math.abs(A_blue-C_blue)+
								        	    Math.abs(A_blue-E_blue)*Math.abs(A_blue-E_blue)+
								        		Math.abs(Z_blue-E_blue)*Math.abs(Z_blue-E_blue)+
								        		Math.abs(Z_blue-C_blue)*Math.abs(Z_blue-C_blue);
				        
			                     L_vec[mm] += (sumDiff_Blue/4.0 + sumDiff_Red/4.0 + sumDiff_Green/4.0);// * norm;
							} else { // check for zeroes
								if ((A_red==0)||(A_green==0)||(A_blue==0)||
									(C_red==0)||(C_green==0)||(C_blue==0)||
									(E_red==0)||(E_green==0)||(E_blue==0)||
									(Z_red==0)||(Z_green==0)||(Z_blue==0)) { //zero detected
										//do not add to the sum but correct later on norm
										numZeroesDetected += 1;
								} else { //no zeroes detected
									  sumDiff_Red = Math.abs(A_red-C_red)*Math.abs(A_red-C_red)+
						        		      Math.abs(A_red-E_red)*Math.abs(A_red-E_red)+
						        		      Math.abs(Z_red-E_red)*Math.abs(Z_red-E_red)+
						        		      Math.abs(Z_red-C_red)*Math.abs(Z_red-C_red);
							    
									  sumDiff_Green = Math.abs(A_green-C_green)*Math.abs(A_green-C_green)+
							        		          Math.abs(A_green-E_green)*Math.abs(A_green-E_green)+
							        		          Math.abs(Z_green-E_green)*Math.abs(Z_green-E_green)+
							        		          Math.abs(Z_green-C_green)*Math.abs(Z_green-C_green);		        
									        
									 sumDiff_Blue = Math.abs(A_blue-C_blue)*Math.abs(A_blue-C_blue)+
									        	    Math.abs(A_blue-E_blue)*Math.abs(A_blue-E_blue)+
									        		Math.abs(Z_blue-E_blue)*Math.abs(Z_blue-E_blue)+
									        		Math.abs(Z_blue-C_blue)*Math.abs(Z_blue-C_blue);
					        
				                     L_vec[mm] += (sumDiff_Blue/4.0 + sumDiff_Red/4.0 + sumDiff_Green/4.0);// * norm;
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
				L_Sum =  L_Sum + L_vec[mm1] / ((double)(k*k));  //last k outside the brackets
			}
			totals[k-1] = L_Sum/((double)(k*k));			
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
			eps[n] = (n + 1)*(n + 1);
		}
		return eps;
	}	

}
