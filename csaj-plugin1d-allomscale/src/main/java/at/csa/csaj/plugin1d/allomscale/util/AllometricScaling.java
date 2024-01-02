package at.csa.csaj.plugin1d.allomscale.util;

/*
 * #%L
 * Project: ImageJ2 signal plugin for allometric scaling
 * File: AllometricScaling.java
 * 
 * $Id$
 * $HeadURL$
 * 
 * This file is part of ComsystanJ software, hereinafter referred to as "this program".
 * %%
 * Copyright (C) 2021 - 2024 Comsystan Software
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

import javax.swing.JOptionPane;
import org.scijava.log.LogService;
import at.csa.csaj.commons.regression.LinearRegression;

/**
 * Fractal Dimension using Allometric Scaling
 * 
 * @author Helmut Ahammer
 * @since  2021 04
 */
@SuppressWarnings({"rawtypes","unchecked"})
public class AllometricScaling {
	
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
	 * 
	 * @param logService
	 * 
	 */
	public AllometricScaling(LogService logService) {
		this.logService = logService;
	}
	
	/**
	 * This is the standard constructor
	 */
	public AllometricScaling(){}
	
	/**
	 * This method calculates the mean of a series
	 * @param data a vector of doubles
	 * @return double
	 */
	private double calcMean(double[] data){
		double sum = 0.0;
		double mean = 0.0;
		for  (int i = 0; i < data.length; i++){
			sum = sum + data[i];
		}
		mean = sum / data.length;
		return mean;
	}
	

	/**
	 * This method calculates the standard deviation of a series
	 * @param data a vector of doubles
	 * @param mean 
	 * @return double
	 */
	@SuppressWarnings("unused")
	private double calcStdDev(double[] data, double mean){
		double var = this.calcVariance(data, mean);
		return Math.sqrt(var);
	}
	
	/**
	 * This method calculates the variance of a series
	 * @param data the data 
	 * @param mean
	 * @return double
	 */
	private double calcVariance(double[] data, double mean){
		double sum = 0.0;
		for  (int i = 0; i < data.length; i++){
			sum = sum + (data[i] - mean) * (data[i] - mean);
		}
		sum = sum / data.length;	
		return sum;
	}
	
	/**
	 * This method calculates the standard deviations and the means of the downscaled data series
	 * @param data 1D data vector
	 * @return double[][] array of two vectors
	 */
	public double[][] calcMeansAndVariances(double[] data){
	
		if (data.length < 10){
			//
		}
		
		int N = 0;
		while (N < data.length/4){	
			N = N +1;
		}
		
		double[][] asData  = new double[2][N];   //[0][] means Vector    [1][] standard deviations Vector
		double[] means     = new double[N];
		double[] variances = new double[N];
		double mean;
		double var;
		
		for(int f = 0; f < N; f++){ 
			//int numInt = (int)Math.pow(2, f);  //number of data points for interpolation
			int numInt = f+1;  //number of data points for interpolation
			//System.out.println("Allometric Scaling: f(N):" + f+"("+N+")" + "    numInt:"+numInt);
					
			//get number of iterations
			int numIter = 0;
			for (int i = 0; i <= data.length-numInt; i = i+numInt){
				numIter += 1;
			}	
			
			double[] dataAggregated = new double[numIter];
			for (int i = 0; i <= data.length-numInt; i = i+numInt){
				double sum = 0.0;
				for (int ii = i; ii < i + numInt; ii++){
					sum += data[ii];
				}
				//sum = sum/numInt;
				//for (int ii = i; ii < i + numInt; ii++){
					dataAggregated[i/numInt] = sum;
				//}
			}		
			mean  = this.calcMean(dataAggregated);	
			var   = this.calcVariance(dataAggregated, mean);
			//var   = this.calcStdDev(dataAggregated, mean);
			//means.add(mean);  //mean can be negative and that is a log problem 
			means[f]     = (double)numInt;
			variances[f] = var;	
			//System.out.println("Allometric Scaling:   mean:"+mean+"   variance:"+var);
					
		}	
	
		asData[0] = means;
		asData[1] = variances;	
		return asData;
	}

	/**
	 * 
	 * @param asData data pairs
	 * @param regStart 
	 * @param regEnd
	 * @param plotName the name
	 * @param showPlot 
	 * @param deleteExistingPlot
	 * @return double[] result 
	 */
	public double[] calcLogRegression(double[][] asData, int regStart, int regEnd){
		double[] regressionParams;
		
		if (regEnd > asData[0].length){
			regEnd = asData[0].length;
			logService.info(this.getClass().getName() + " AllometricScaling: Regression max > 1/4 sequence size is not allowed");
			JOptionPane.showMessageDialog(null, "AllometricScaling: Regression max > 1/4 sequence size is not allowed!", "Info", JOptionPane.INFORMATION_MESSAGE);
			regressionParams = new double[5];
			for (int p = 0; p < regressionParams.length; p++) regressionParams[p] = Double.NaN;
			return regressionParams;
		}
		
		lnDataX = new double[asData[0].length];
		lnDataY = new double[asData[0].length];  
		double lnX;
		double lnY;
		for (int i = 0; i < asData[0].length; i++){
			//System.out.println("Allometric Scaling: i:" + i + "  mean:"+ asData[0][i]+ "  variance: " + asData[1][i]);
			if ((Double)asData[0][i] == 0) asData[0][i] = Double.MIN_VALUE;
			if ((Double)asData[1][i] == 0) asData[1][i] = Double.MIN_VALUE;
		}
		boolean NaNdetected = false;
		for (int i = 0; i < asData[0].length; i++){
			lnX = Math.log((Double) asData[0][i]);
			lnY = Math.log((Double) asData[1][i]);
			if (Double.isNaN(lnX)){
				NaNdetected = true;
				logService.info(this.getClass().getName() + " AllometricScaling: NaN value detected, maybe a mean is negative, which is not allowed for allometric scaling");
				JOptionPane.showMessageDialog(null, "AllometricScaling: NaN value detected, maybe a mean is negative, which is not allowed for allometric scaling", "Info", JOptionPane.INFORMATION_MESSAGE); 
			}
			if (Double.isNaN(lnY)){
				NaNdetected = true;
				logService.info(this.getClass().getName() + " AllometricScaling: NaN value detected, maybe a mean is negative, which is not allowed for allometric scaling");
				JOptionPane.showMessageDialog(null, "AllometricScaling: NaN value detected, maybe a mean is negative, which is not allowed for allometric scaling", "Info", JOptionPane.INFORMATION_MESSAGE); 
			}	
			lnDataX[i] = lnX;
			lnDataY[i] = lnY;
		}
		if (NaNdetected) JOptionPane.showMessageDialog(null, "AllometricScaling: NaN value detected, maybe a mean is negative, which is not allowed for allometric scaling", "Info", JOptionPane.INFORMATION_MESSAGE); 
	
		// Compute regression
		LinearRegression lr = new LinearRegression();
		regressionParams = lr.calculateParameters(lnDataX, lnDataY, regStart, regEnd);
		// 0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
		return regressionParams;
		
	}
}
