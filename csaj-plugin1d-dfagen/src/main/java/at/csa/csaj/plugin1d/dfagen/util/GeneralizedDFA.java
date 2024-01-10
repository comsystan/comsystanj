/*-
 * #%L
 * Project: ImageJ2 signal plugin for computing Generalized (multifractal) DFA
 * File: GeneralizedDFA.java
 * 
 * $Id$
 * $HeadURL$
 * 
 * This file is part of ComsystanJ software, hereinafter referred to as "this program".
 * %%
 * Copyright (C) 2024 Comsystan Software
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
package at.csa.csaj.plugin1d.dfagen.util;


import javax.swing.JOptionPane;

import org.scijava.log.LogService;
import at.csa.csaj.commons.regression.LinearRegression;

/**
 * 
 * @author Helmut Ahammer
 * @since 2024-01 Generalized (multifractal) DFA
 * 
 * According to:
 * Kantelhardt, Jan W., Stephan A. Zschiegner, Eva Koscielny-Bunde, Shlomo Havlin, Armin Bunde, und H. Eugene Stanley.
 * „Multifractal detrended fluctuation analysis of nonstationary time series“.
 * Physica A: Statistical Mechanics and its Applications 316, Nr. 1 (15. Dezember 2002): 87–114.
 * https://doi.org/10.1016/S0378-4371(02)01383-3.
 */

public class GeneralizedDFA {

	private LogService logService;
	
	private int progressBarMin = 0;
	private int progressBarMax = 100;
	private double[]   lnDataX; //[]
	private double[][] lnDataY; //[q][]
	private Object operator;

	public double[] getLnDataX() {
		return lnDataX;
	}

	public void setLnDataX(double[] lnDataX) {
		this.lnDataX = lnDataX;
	}

	public double[][] getLnDataY() {
		return lnDataY;
	}

	public void setLnDataY(double[][] lnDataY) {
		this.lnDataY = lnDataY;
	}

	public int getProgressBarMin() {
		return progressBarMin;
	}

	public void setProgressBarMin(int progressBarMin) {
		this.progressBarMin = progressBarMin;
	}

	public int getProgressBarMax() {
		return progressBarMax;
	}

	public void setProgressBarMax(int progressBarMax) {
		this.progressBarMax = progressBarMax;
	}

	/**
	 * This is the standard constructor with the possibility to import calling
	 * class This is useful for progress bar and canceling functionality
	 * 
	 * @param operator
	 * 
	 */
	public GeneralizedDFA(LogService logService) {
		this.logService = logService;
	}

	/**
	 * This is the standard constructor
	 */
	public GeneralizedDFA() {

	}

	/**
	 * This method computes the "Fluctuation function" for every q of the series
	 * 
	 * @param data  1D data vector
	 * @param winSize
	 *            winSize must be smaller than the total number of time points
	 *            winSize should not be greater than N/3 (N number of data points)!
	 * @return double F[winSize][numQ] "Fluctuation functions"
	 */
	public double[][] computeFluctuationFunctions(double[] data, int winSizeMax, int minQ, int maxQ, int numQ) {
		int N = data.length;
		if (winSizeMax > N) {
			winSizeMax = N / 3;
			logService.info(this.getClass().getName() + " DFA parameter window size too large, automatically set to data length/3");
			JOptionPane.showMessageDialog(null, " DFA parameter window size too large, automatically set to data length/3", "Info", JOptionPane.INFORMATION_MESSAGE);

		}
			
		//Mean of data series
		double meanData = 0.0;
		for (int i = 0; i < N; i++) {
			meanData = meanData + data[i];
		}
		meanData = meanData/data.length;
		
		//Cumulative sum of data series
		for (int i = 1; i < N; i++) {
			data[i] = data[i-1] + (data[i] - meanData);
		}
		
		
		double[][] F = new double[numQ][winSizeMax];
		for (int q = 0; q< numQ; q++) {
			F[q][1] = Double.MIN_VALUE;
			F[q][2] = Double.MIN_VALUE;
			F[q][3] = Double.MIN_VALUE;  //will be hopefully eliminated later on
		}
		
		Integer numWin;
		double[] flucWin;
		double[] segmentDataY;
		double[] segmentDataX;
		int startIndex;  
		int endIndex;	
		LinearRegression lr;
		double[] residuals; //Simply the differences of the data y values and the computed regression y values.		
		
		double sum = 0.0;
		
		for (int winLength = 4; winLength <= winSizeMax; winLength++) {// windows with size 1,2, and 3 should not be used according to Peng et al.
			numWin = (int)Math.floor((double)(N/(double)winLength));	
			flucWin = new double[numWin];
			
			for (int w = 1; w <= numWin; w++) {
				//Extract data points with length winLength
				segmentDataY = new double[winLength];
				segmentDataX = new double[winLength];
				startIndex = (w-1) * winLength;  
				endIndex   = startIndex + winLength -1 ;		
				for (int i = startIndex; i <= endIndex; i++) {
					segmentDataY[i-startIndex] = data[i];
					segmentDataX[i-startIndex] = i-startIndex +1; 
				}
					
				//Compute fluctuation of segment	
				lr = new LinearRegression();
				residuals = lr.calculateResiduals(segmentDataX, segmentDataY); //Simply the differences of the data y values and the computed regression y values.		
				
			
				sum = 0.0;		
				for (int y = 0; y < residuals.length; y++) {
					sum = sum + (residuals[y]*residuals[y]);
				}
				sum = sum/residuals.length;
					
				flucWin[w-1] = sum;	//Equ.2 Kantelhardt et al
			}
			
			double meanF = 0.0;
			for (int q = 0; q < numQ; q++) {
				//Mean fluctuation for one window size:
				meanF = 0.0;
				for (int w = 1; w <= numWin; w++) {
					meanF = meanF + Math.pow(flucWin[w-1], (double)(minQ+q)/2.0);  //Equ.4 Kantelhardt et al
				}			
				meanF = meanF/numWin;
				
				F[q][winLength-1] =  Math.pow(meanF, 1.0/(minQ+q)); //Equ.4 Kantelhardt et al
			} //Simple DFA alpha for q==2;
	
//			if (operator != null) {
//				this.operator.fireProgressChanged(winLength * (progressBarMax - progressBarMin) / winSize + progressBarMin);
//				if (this.operator.isCancelled(this.operator.getParentTask()))
//					return null;
//			}
		}
		//F.remove(0);  // does not work because of linear regression later on
		//Set first three values to the 4th witch is really computed
		for (int q = 0; q < numQ; q++) {
			F[q][0] = F[q][3];
			F[q][1] = F[q][3];	
			F[q][2] = F[q][3];	
		}
		
		return F;
	}

	/**
	 * 
	 * @param F   DFA Fluctuation functions [q][]
	 * @param regStart
	 * @param regEnd
	 * @return double regression paramters for ever [q][]
	 */
	public double[][] computeAlphas(double[][] F, int regStart, int regEnd, int numQ) {
		
		double[][] regressionParamsQ = new double[numQ][5];
		lnDataX       = new double[F[0].length];
		lnDataY       = new double[numQ][F[0].length];
		double[] lnDataXSingle = new double[F[0].length];
		double[] lnDataYSingle = new double[F[0].length];
		
		double lnX;
		double lnY;
		
		for (int q = 0; q < numQ; q++) {
			//lnDataY = new Vector<Double>();
			//lnDataX = new Vector<Double>(); // k
			for (int i = 0; i < F[0].length; i++) {
				if (F[q][i] == 0) F[q][i] =  Double.MIN_VALUE;
			}
			// System.out.println("DFA: lnk ln(F)");
			//logService.info(this.getClass().getName() + " DFA: lnX   ln(F)"); 
			for (int i = 0; i < F[0].length; i++) {
				lnX = Math.log(i + 1);
				lnY = Math.log(F[q][i]);
				lnDataX[i]    = lnX;
				lnDataY[q][i] = lnY;
				// System.out.println(lnX + " " + lnY);
				//logService.info(this.getClass().getName() + "      " + lnX + "     " + lnY); 
			}
				
			int F0length = F[0].length;
			int lnDX = lnDataX.length;
			int lnDY = lnDataY[0].length;
			
			
			// Compute regression
			//get regression data vectors for q
			for (int i = 0; i < F[0].length; i++) {
				lnDataXSingle[i] = lnDataX[i];
				lnDataYSingle[i] = lnDataY[q][i];
			}
			LinearRegression lr = new LinearRegression();
			double[] regressionParams = lr.calculateParameters(lnDataXSingle, lnDataYSingle, regStart, regEnd);
			// 0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
			
			for (int r = 0; r < 5; r++ ) {
				regressionParamsQ[q][r] = regressionParams[r];
			}
		}//q
		return regressionParamsQ;
	}

}// END
