/*-
 * #%L
 * Project: ImageJ2 signal plugin for computing DFA
 * File: DFA.java
 * 
 * $Id$
 * $HeadURL$
 * 
 * This file is part of ComsystanJ software, hereinafter referred to as "this program".
 * %%
 * Copyright (C) 2021 - 2022 Comsystan Software
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
package at.csa.csaj.plugin1d.dfa.util;


import javax.swing.JOptionPane;

import org.scijava.log.LogService;
import at.csa.csaj.commons.regression.LinearRegression;

/**
 * 
 * @author Helmut Ahammer
 * @since 2021-02-24 DFA according to Peng etal 1994 and e.g. Hardstone etal. 2012
 */
public class DFA {

	private LogService logService;
	
	private int progressBarMin = 0;
	private int progressBarMax = 100;
	private double[] lnDataX;
	private double[] lnDataY;
	private Object operator;

	public double[] getLnDataX() {
		return lnDataX;
	}

	public void setLnDataX(double[] lnDataX) {
		this.lnDataX = lnDataX;
	}

	public double[] getLnDataY() {
		return lnDataY;
	}

	public void setLnDataY(double[] lnDataY) {
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
	public DFA(LogService logService) {
		this.logService = logService;
	}

	/**
	 * This is the standard constructor
	 */
	public DFA() {

	}

	/**
	 * This method computes the "Fluctuation function" of the series
	 * 
	 * @param data  1D data vector
	 * @param winSize
	 *            winSize must be smaller than the total number of time points
	 *            winSize should not be greater than N/3 (N number of data points)!
	 * @return double F[winSize] "Fluctuation functions"
	 */
	public double[] computeFluctuationFunction(double[] data, int winSize) {
		int N = data.length;
		if (winSize > N) {
			winSize = N / 3;
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
		
		
		double[] F = new double[winSize];
		F[0] = Double.MIN_VALUE;
		F[1] = Double.MIN_VALUE;
		F[2] = Double.MIN_VALUE;  //will be hopefully eliminated later on
		
		for (int winLength = 4; winLength <= winSize; winLength++) {// windows with size 1,2, and 3 should not be used according to Peng et al.
			Integer numWin = (int)Math.floor((double)(N/(double)winLength));	
			double[] flucWin = new double[numWin];
			
			for (int w = 1; w <= numWin; w++) {
				//Extract data points with length winLength
				double[] regDataY = new double[winLength];
				double[] regDataX = new double[winLength];
				int startIndex = (w-1) * winLength;  
				int endIndex   = startIndex + winLength -1 ;		
				for (int i = startIndex; i <= endIndex; i++) {
					regDataY[i-startIndex] = data[i];
					regDataX[i-startIndex] = i-startIndex +1; 
				}
					
				//Compute fluctuation of segment	
				LinearRegression lr = new LinearRegression();
				double[] residuals = lr.calculateResiduals(regDataX, regDataY); //Simply the differences of the data y values and the computed regression y values.		
				
				double rms = 0.0;		
				for (int y = 0; y < residuals.length; y++) {
					rms = rms + (residuals[y]*residuals[y]);
				}
				rms = rms/residuals.length;
				rms = Math.sqrt(rms);
				flucWin[w-1] = rms;			
			}
			//Mean fluctuation for one window size:
			double meanF = 0.0;
			for (int w = 1; w <= numWin; w++) {
				meanF = meanF + flucWin[w-1];
			}			
			meanF = meanF / numWin;

			F[winLength-1] =  meanF;
	
//			if (operator != null) {
//				this.operator.fireProgressChanged(winLength * (progressBarMax - progressBarMin) / winSize + progressBarMin);
//				if (this.operator.isCancelled(this.operator.getParentTask()))
//					return null;
//			}
		}
		//F.remove(0);  // does not work because of linear regression later on
		//Set first three values to the 4th witch is really computed
		F[0] = F[3];
		F[1] = F[3];	
		F[2] = F[3];
		return F;
	}

	/**
	 * 
	 * @param F   DFA Fluctuation function
	 * @param regStart
	 * @param regEnd
	 * @return double alpha
	 */
	public double[] computeAlpha(double[] F, int regStart, int regEnd) {
		lnDataY = new double[F.length];
		lnDataX = new double[F.length]; //k
		//lnDataY = new Vector<Double>();
		//lnDataX = new Vector<Double>(); // k
		for (int i = 0; i < F.length; i++) {
			if (F[i] == 0) F[i] =  Double.MIN_VALUE;
		}
		// System.out.println("DFA: lnk ln(F)");
		logService.info(this.getClass().getName() + " DFA: lnX   ln(F)"); 
		for (int i = 0; i < F.length; i++) {
			double lnX = Math.log(i + 1);
			double lnY = Math.log(F[i]);
			lnDataX[i] = lnX;
			lnDataY[i] = lnY;
			// System.out.println(lnX + " " + lnY);
			//logService.info(this.getClass().getName() + "      " + lnX + "     " + lnY); 
		}
		
		// Compute regression
		LinearRegression lr = new LinearRegression();
		double[] regressionParams = lr.calculateParameters(lnDataX, lnDataY, regStart, regEnd);
		// 0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
		return regressionParams;
	}

}// END
