/*-
 * #%L
 * Project: ImageJ plugin for computing fractal dimension with 2D Higuchi algorithms.
 * File: Higuchi2D_RGBROIColWeight_NotReadyYet.java
 * 
 * $Id$
 * $HeadURL$
 * 
 * This file is part of ComsystanJ software, hereinafter referred to as "this program".
 * %%
 * Copyright (C) 2020 - 2021 Comsystan Software
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
package at.csa.csaj.img2d.frac.dim.hig2d.util;

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

/**	Higuchi-Dimension for RGB images
 *
 * @author Nikolaus Sabathiel, Martin Reiss, Helmut Ahammer
 * @since 2020-11-23  
 */

public class Higuchi2D_RGBROIColWeight_NotReadyYet implements Higuchi2DMethods{

	private RandomAccessibleInterval<?> rai = null;
	private int         numK   = 0;
	private double[]    totals = null;
	private double[]    eps    = null;
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
	public Higuchi2D_RGBROIColWeight_NotReadyYet(RandomAccessibleInterval<?> rai, int numK, boolean skipZeroes)  {
		this.rai   = rai;
		this.numK = numK;
		this.skipZeroes = skipZeroes;
	}

	public Higuchi2D_RGBROIColWeight_NotReadyYet() {
	}

	/**
	 * This method calculates the Higuchi dimension for RGB images
	 * 
	 * @return totals
	 */
	@Override
	public double[] calcTotals() {

		if (eps == null) this.calcEps();
		
		//System.out.println("Higuchi2D_ROIColorWeighted: Preparing integer stack...");
		
		// Get size 
		long width = rai.dimension(0);
		long height = rai.dimension(1);
		RandomAccess<RealType<?>>ra = (RandomAccess<RealType<?>>) rai.randomAccess();


		// Prepare 3D integer array
		//TODO eliminate this step
		int[][] array2D_0 = new int[(int)width][(int)height];
		int[][] array2D_1 = new int[(int)width][(int)height];
		int[][] array2D_2 = new int[(int)width][(int)height];
		
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
		double kreuzblue, kreuzred,kreuzgreen;

		double psi = 0;
		double average_red   = 0;
		double average_green = 0;
		double average_blue  = 0;
			
		long average_red_counter   = 0;
		long average_green_counter = 0;
		long average_blue_counter  = 0;
			
		
		//calculate mean of each band	
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				average_red_counter   += (array2D_0[x][y]);
				average_green_counter += (array2D_1[x][y]);
				average_blue_counter  += (array2D_2[x][y]);
			}
		}
			
		average_red = (double)average_red_counter / (width*height);
		average_green = (double)average_green_counter / (width*height);
		average_blue = (double)average_blue_counter / (width*height);
			
		long count = 0;
		long norm1 = 0;
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				if (array2D_1[i][j]<70&&array2D_2[i][j]<120&&array2D_0[i][j]<120&&Math.abs(array2D_0[i][j]-array2D_2[i][j])<21&&array2D_1[i][j]<((array2D_2[i][j]+5)/2.0)) {
					count++;
				}
				if (array2D_1[i][j]<235&&array2D_2[i][j]<235&&array2D_0[i][j]<235) {
					norm1++;
				}
			}
		}		
		double delta = (double)count/norm1;		
	    psi = ((double)(255*255*255)/(average_red*average_green*average_blue));
			
		for (int k = 1; k <=numK; k++) {
				
			//operator.fireProgressChanged((int) ((float)(k)/(float)numK*100.0f));	
		
			double[] L_vec = new double[k*k];
			int mm = 0;
			for (int m1 = 1; m1 <= k ; m1++) {
				for (int m2 = 1; m2 <= k ; m2++) {
					
					flr1 = Math.floor ((double)(N-m1-k)/(double)k);
					flr2 = Math.floor ((double)(M-m2-k)/(double)k);
					norm = ((double)N-2.0) * ((double)M-2.0)/(flr1*(double)k * flr2*(double)k);
			
					numZeroesDetected = 0;
					for (int i = 1; i <= flr1; i++) { 
						for (int j = 1; j <= flr2; j++) {	
				
							//Kreuzpunkte
							A_blue=array2D_2[m1+i*k-1]    [m2+j*k-1];
					        B_blue=array2D_2[m1+(i+1)*k-1][m2+j*k-1];
					        C_blue=array2D_2[m1+(i-1)*k-1][m2+j*k-1];
					        D_blue=array2D_2[m1+i*k-1]    [m2+(j+1)*k-1];
					        E_blue=array2D_2[m1+i*k-1]    [m2+(j-1)*k-1];
					        
					        //Diagonalpunkte
					        F_blue=array2D_2[m1+(i+1)*k-1][m2+(j+1)*k-1];
					        G_blue=array2D_2[m1+(i-1)*k-1][m2+(j+1)*k-1];
					        H_blue=array2D_2[m1+(i+1)*k-1][m2+(j-1)*k-1];
					        Z_blue=array2D_2[m1+(i-1)*k-1][m2+(j-1)*k-1];
					        
					        kreuzblue = 0.125*Math.abs(A_blue-B_blue)*Math.abs(A_blue-B_blue)+
					        		    0.125*Math.abs(A_blue-C_blue)*Math.abs(A_blue-C_blue)+
					        		    0.125*Math.abs(A_blue-D_blue)*Math.abs(A_blue-D_blue)+
					        		    0.125*Math.abs(A_blue-E_blue)*Math.abs(A_blue-E_blue)+
					        		    0.125*Math.abs(A_blue-F_blue)*Math.abs(A_blue-F_blue)+
					        		    0.125*Math.abs(A_blue-G_blue)*Math.abs(A_blue-G_blue)+
					        		    0.125*Math.abs(A_blue-H_blue)*Math.abs(A_blue-H_blue)+
					        		    0.125*Math.abs(A_blue-Z_blue)*Math.abs(A_blue-Z_blue);
							
							//Kreuzpunkte
							A_red=array2D_0[m1+i*k-1]    [m2+j*k-1];
					        B_red=array2D_0[m1+(i+1)*k-1][m2+j*k-1];
					        C_red=array2D_0[m1+(i-1)*k-1][m2+j*k-1];
					        D_red=array2D_0[m1+i*k-1]    [m2+(j+1)*k-1];
					        E_red=array2D_0[m1+i*k-1]    [m2+(j-1)*k-1];
					        
					        //Diagonalpunkte
					        F_red=array2D_0[m1+(i+1)*k-1][m2+(j+1)*k-1];
					        G_red=array2D_0[m1+(i-1)*k-1][m2+(j+1)*k-1];
					        H_red=array2D_0[m1+(i+1)*k-1][m2+(j-1)*k-1];
					        Z_red=array2D_0[m1+(i-1)*k-1][m2+(j-1)*k-1];
					        
					        kreuzred = 0.125*Math.abs(A_red-B_red)*Math.abs(A_red-B_red)+
					        		   0.125*Math.abs(A_red-C_red)*Math.abs(A_red-C_red)+
					        		   0.125*Math.abs(A_red-D_red)*Math.abs(A_red-D_red)+
					        		   0.125*Math.abs(A_red-E_red)*Math.abs(A_red-E_red)+
					        		   0.125*Math.abs(A_red-F_red)*Math.abs(A_red-F_red)+
					        		   0.125*Math.abs(A_red-G_red)*Math.abs(A_red-G_red)+
					        		   0.125*Math.abs(A_red-H_red)*Math.abs(A_red-H_red)+
					        		   0.125*Math.abs(A_red-Z_red)*Math.abs(A_red-Z_red);
							
							//Kreuzpunkte
							A_green=array2D_1[m1+i*k-1]    [m2+j*k-1];
					        B_green=array2D_1[m1+(i+1)*k-1][m2+j*k-1];
					        C_green=array2D_1[m1+(i-1)*k-1][m2+j*k-1];
					        D_green=array2D_1[m1+i*k-1]    [m2+(j+1)*k-1];
					        E_green=array2D_1[m1+i*k-1]    [m2+(j-1)*k-1];
					        
					        //Diagonalpunkte
					        F_green=array2D_1[m1+(i+1)*k-1][m2+(j+1)*k-1];
					        G_green=array2D_1[m1+(i-1)*k-1][m2+(j+1)*k-1];
					        H_green=array2D_1[m1+(i+1)*k-1][m2+(j-1)*k-1];
					        Z_green=array2D_1[m1+(i-1)*k-1][m2+(j-1)*k-1];
					        
					        kreuzgreen = 0.125*Math.abs(A_green-B_green)*Math.abs(A_green-B_green)+
					        		     0.125*Math.abs(A_green-C_green)*Math.abs(A_green-C_green)+
					        		     0.125*Math.abs(A_green-D_green)*Math.abs(A_green-D_green)+
					        		     0.125*Math.abs(A_green-E_green)*Math.abs(A_green-E_green)+
					        		     0.125*Math.abs(A_green-F_green)*Math.abs(A_green-F_green)+
					        		     0.125*Math.abs(A_green-G_green)*Math.abs(A_green-G_green)+
					        		     0.125*Math.abs(A_green-H_green)*Math.abs(A_green-H_green)+
					        		     0.125*Math.abs(A_green-Z_green)*Math.abs(A_green-Z_green);	  
					        
					       // L_vec[mm] += (kreuzblue+kreuzred+kreuzgreen) * norm;
							if (!skipZeroes) { //no skipping
								L_vec[mm] += (kreuzblue+kreuzred+kreuzgreen);// * norm;
							} else { // check for zeroes
								if ((A_red==0)||(A_green==0)||(A_blue==0)||
										(C_red==0)||(C_green==0)||(C_blue==0)||
										(E_red==0)||(E_green==0)||(E_blue==0)||
										(Z_red==0)||(Z_green==0)||(Z_blue==0)|| 
										(F_red==0)||(F_green==0)||(F_blue==0)||
										(G_red==0)||(G_green==0)||(G_blue==0)||
										(H_red==0)||(H_green==0)||(H_blue==0)||
										(Z_red==0)||(Z_green==0)||(Z_blue==0))	
								{ //zero detected
										//do not add to the sum but correct later on norm
										numZeroesDetected += 1;
								} else { //no zeroes detected
									L_vec[mm] += (kreuzblue+kreuzred+kreuzgreen);// * norm;
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
				L_Sum =  L_Sum + L_vec[mm1] / ((double)(k));  //last k outside the brackets
			}
			totals[k-1] =  Math.pow(L_Sum/((double)(k*k)),  psi*delta);  //delta.... ROI density				
		} //k
		return totals;
	}
	
	/**
	 * This method calculates the number of eps
	 * @return eps
	 */
	@Override
	public double[] calcEps() {

		eps = new double[numK];
		for (int n = 0; n < numK; n++) {
			eps[n] = n + 1;
		}
		return eps;
	}	
}
