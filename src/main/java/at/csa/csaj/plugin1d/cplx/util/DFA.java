/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: DFA.java
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
package at.csa.csaj.plugin1d.cplx.util;


import javax.swing.JOptionPane;

import org.scijava.log.LogService;

import at.csa.csaj.commons.CsajRegression_Linear;

/**
 * 
 * @author Helmut Ahammer
 * @since 2021-02-24 DFA according to Peng etal 1994 and e.g. Hardstone etal. 2012
 */
public class DFA {

	private LogService logService;
	
	private int progressBarMin = 0;
	private int progressBarMax = 100;
	
	private double[] eps;
	private double[] lnDataX;
	private double[] lnDataY;
	private Object operator;

	public double[] getEps() {
		return eps;
	}

	public void setEps(double[] eps) {
		this.eps = eps;
	}
	
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
	 * @param winSizeMax
	 *            winSize must be smaller than the total number of time points
	 *            winSize should not be greater than N/3 (N number of data points)!
	 * @return double F[winSize] "Fluctuation functions"
	 */
	public double[] computeFluctuationFunction(double[] data, int winSizeMax) {
		int N = data.length;
		if (winSizeMax > N) {
			winSizeMax = N / 3;
			logService.info(this.getClass().getName() + " DFA parameter window size too large, automatically set to data length/3");
			JOptionPane.showMessageDialog(null, " DFA parameter window size too large, automatically set to data length/3", "Warning", JOptionPane.WARNING_MESSAGE);

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
		
		eps = new double[winSizeMax];
		for (int e = 0; e < winSizeMax; e++) eps[e] = e+1;
				
		double[] F = new double[winSizeMax];
		F[0] = Double.MIN_VALUE;
		F[1] = Double.MIN_VALUE;
		F[2] = Double.MIN_VALUE;  //will be hopefully eliminated later on
		
		Integer numWin;	
		double[] flucWin;
		
		double[] segmentDataY;
		double[] segmentDataX;
		int startIndex;  
		int endIndex;	
		CsajRegression_Linear lr;
		double[] residuals; 
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
				lr = new CsajRegression_Linear();
				residuals = lr.calculateResiduals(segmentDataX, segmentDataY); //Simply the differences of the data y values and the computed regression y values.		
				
				sum = 0.0;		
				for (int y = 0; y < residuals.length; y++) {
					sum = sum + (residuals[y]*residuals[y]);
				}
				sum = sum/residuals.length;
				flucWin[w-1] = sum;			
			}
			//Mean fluctuation for one window size:
			double meanF = 0.0;
			for (int w = 1; w <= numWin; w++) {
				meanF = meanF + flucWin[w-1];
			}			
			meanF = meanF/numWin;

			F[winLength-1] =  Math.sqrt(meanF);
	
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
	 * @param numRegStart
	 * @param numRegEnd
	 * @return double alpha
	 */
	public double[] computeAlpha(double[] F, int numRegStart, int numRegEnd) {
		lnDataY = new double[F.length];
		lnDataX = new double[F.length]; //k
		//lnDataY = new Vector<Double>();
		//lnDataX = new Vector<Double>(); // k
		double lnX;
		double lnY;
		for (int i = 0; i < F.length; i++) {
			if (F[i] == 0) F[i] =  Double.MIN_VALUE;
		}
		// System.out.println("DFA: lnk ln(F)");
		//logService.info(this.getClass().getName() + " DFA: lnX   ln(F)"); 
		for (int i = 0; i < F.length; i++) {
			lnX = Math.log(eps[i]);
			lnY = Math.log(F[i]);
			lnDataX[i] = lnX;
			lnDataY[i] = lnY;
			// System.out.println(lnX + " " + lnY);
			//logService.info(this.getClass().getName() + "      " + lnX + "     " + lnY); 
		}
		
		// Compute regression
		CsajRegression_Linear lr = new CsajRegression_Linear();
		double[] regressionParams = lr.calculateParameters(lnDataX, lnDataY, numRegStart, numRegEnd);
		// 0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
		return regressionParams;
	}

}// END
