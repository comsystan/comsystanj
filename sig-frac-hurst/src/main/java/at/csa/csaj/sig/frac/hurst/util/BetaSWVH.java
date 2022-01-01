/*-
 * #%L
 * Project: ImageJ2 signal plugin for computing the Hurst coefficient.
 * File: BetaSWVH.java
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
package at.csa.csaj.sig.frac.hurst.util;

import java.util.Vector;
import org.scijava.log.LogService;
import at.csa.csaj.commons.regression.LinearRegression;

/**
 * This method calculates H using scaled window variance according to Eke et al.
 * Average of local Standard Deviations
 */
public class BetaSWVH {

	
private LogService logService;	
	
	private int progressBarMin = 0;
	private int progressBarMax = 100;
	private double[] lnDataX;
	private double[] lnDataY;

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
	 * This is the standard constructor
	 */
	public BetaSWVH() {

	}

	/**
	 * This method calculates H using scaled window variance according to Eke et al.
	 * Average of local Standard Deviations
	 * 
	 * @param signal
	 * @return
	 */
	public double[] computeRegression(double[] signal) {
		int length = signal.length;
		Vector<Double> sdtDevVec = new Vector<Double>();
		double winMean = 0.0d;
		double winSdtDev = 0;
		
		//get number of iterations
		int winSize = 3; //initial window size
		int numIterations = 0;
	    while ((signal.length / winSize) > 1){ // at least two big windows
	    	winSize = winSize*2;	
	    	numIterations += 1;
		}
		
		double[] dataY = new double[numIterations];
		double[] dataX = new double[numIterations];  
		lnDataY = new double[numIterations];
		lnDataX = new double[numIterations];  
		
		winSize = 3; //initial window size
		int n = 0;
	    while ((signal.length / winSize) > 1){ // at least two big windows
	    	sdtDevVec = new Vector<Double>();
	    	int w = 0; 	
	    	while (w <= (signal.length - winSize)){   //scroll through signal with windows
	    		winMean = 0.0d;
	    		for (int i = w; i < w + winSize; i++){ //scroll through data points of a single window
	    			winMean = winMean +signal[i];
	    		}
	    		winMean = winMean/winSize;
	    		winSdtDev = 0;
	    		for (int i = w; i < w + winSize; i++){ //scroll through data points of a single window
	    			winSdtDev = winSdtDev + ((signal[i] - winMean) * (signal[i] - winMean));  			
	    		}
	    		winSdtDev = winSdtDev/winSize;
	    		winSdtDev = Math.sqrt(winSdtDev);
	    		sdtDevVec.add(winSdtDev);  // add local SD
	    		w = w + winSize;
	    	}
	    	//calculate mean and set data values:
	    	dataY[n] = this.calcMean(sdtDevVec);
	    	dataX[n] = (double)winSize;
	    	winSize = winSize*2;
	    	n = n + 1;
		}
	    
	    //calculate logarithm of normalized data
		for (int i = 0; i < dataY.length; i++){
			if (dataY[i] == 0) dataY[i] =  Double.MIN_VALUE;
		}
		for (int i = 0; i < dataY.length; i++){
			lnDataX[i] = Math.log(dataX[i]/dataX[0]);
			lnDataY[i] = Math.log(dataY[i]/dataY[0]);
		}
		int regMin = 1;
		int regMax = lnDataY.length;
			
		// Compute regression
		LinearRegression lr = new LinearRegression();
		double[] regressionParams = lr.calculateParameters(lnDataX, lnDataY, regMin, regMax);
		//0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
		
		double[] result = {regressionParams[1], regressionParams[4], regressionParams[3]};  //slope, r2, slope standard error
		//swvH = p[1];
		
		return result;
	}
	
	/**
	 * This method calculates the mean of a data series
	 * @param data1D
	 * @return mean 
	 * 
	 */
	public Double calcMean(Vector<Double> data1D){
		double sum = 0;
		for(double d: data1D){
			sum += d;
		}
		return sum/data1D.size();
	}
	
	/**
	 * This method calculates the variance of a data series
	 * @param data1D
	 * @return variance
	 */
	private double calcVariance(Vector<Double> data1D){
		double mean = calcMean(data1D);
		double sum = 0;
		for(double d: data1D){
			sum = sum + ((d - mean) * (d - mean));
		}
		return sum/(data1D.size()-1);  //  1/(n-1) is used by histo.getStandardDeviation() too
	}
	
	/**
	 * This method calculates the standard deviation of a data series
	 * @param data1D
	 * @return standard deviation
	 */
	private double calcStandardDeviation(Vector<Double> data1D){
		double variance  = this.calcVariance(data1D);
		return Math.sqrt(variance);
	}
	
	
}
