/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: WalkingDivider.java
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
package at.csa.csaj.plugin1d.frac.util;

import org.scijava.log.LogService;

import at.csa.csaj.commons.Regression_Linear;



/**
 * 2006
 * <p>
 * <b>Changes</b>
 * <ul>
 * <li>
 * </ul>
 * 
 * 
 * @author Helmut Ahammer
 * @since 2024 05
 */

public class WalkingDivider {

	private LogService logService;
	
	private int progressBarMin = 0;
	private int progressBarMax = 100;
	
	private double[] eps;
	private double[] lnDataX;
	private double[] lnDataY;
	
	private String scalingType;

	public void setScalingType(String scalingType) {
		this.scalingType = scalingType;
	}
	
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
	 * This is the standard constructor
	 */
	public WalkingDivider(String scalingType) {
		this.scalingType = scalingType;
	}

	/**
	 * This method calculates the "Lengths" of the series
	 * 
	 * @param data  1D data double[]
	 * @param numScales  number of newly calculated time series, 2^numScales must be smaller than
	 *              the total number of time points 2^numScales should not be greater than
	 *              N/3 (N number of data points)!
	 * @return double[] pathLenghts "path lengths"
	 */
	public double[] calcLengths(double[] dataX, double[] dataY, int numScales) {
		
		int N = dataX.length;
		if (Math.pow(2, numScales) > N) {
			numScales = (int) (Math.log(N/3) / Math.log(2));
			//logService.warn(this.getClass().getName() + ": Walking divider parameter maxScale too large, automatically set to data length/3");
			System.out.println(this.getClass().getName() + ": Walking divider parameter maxScale too large, automatically set to data length/3");
		}
	
		eps = new double[numScales];
		double[] pathLengths = new double[numScales];	
       
	    if (scalingType.equals("Radial distance")) {
		
		  for (int i = 0; i < numScales; i++) {
	        	int scale = (int) Math.round(Math.pow(2, i));
	        	eps[i] = scale;
	        	int jStart = 0;
	        	double radialDist = 0;
	            for (int j = 1; j < dataX.length; j=j+1) {
	            	radialDist = Math.sqrt(Math.pow(dataX[j] - dataX[jStart], 2) + Math.pow(dataY[j] - dataY[jStart], 2));
	            	//New crossing found
	            	if (radialDist >= scale) {
	            		pathLengths[i] += scale;
	            		jStart = j;
	            		radialDist = 0;
	            	}
	            }
	        }
		  
		} else if (scalingType.equals("Coordinates")) {
		
	        for (int i = 0; i < numScales; i++) {
	        	int scale = (int) Math.round(Math.pow(2, i));
	        	eps[i] = scale;
	            for (int j = scale; j < dataX.length; j=j+scale) {
	                    pathLengths[i] += Math.sqrt(Math.pow(dataX[j] - dataX[j - scale], 2) + Math.pow(dataY[j] - dataY[j - scale], 2));
	            }
	        }
		}
	        
		return pathLengths;
	}

	/**
	 * 
	 * @param pathLengths Walking divider Lengths
	 * @param numRegStart
	 * @param numRegEnd
	 * @return double[] regression parameters
	 */
	public double[] calcRegression(double[] pathLengths, int numRegStart, int numRegEnd) {
		lnDataY = new double[pathLengths.length];
		lnDataX = new double[pathLengths.length]; //scales
		//lnDataY = new Vector<Double>();
		//lnDataX = new Vector<Double>(); // scales
		for (int i = 0; i < pathLengths.length; i++) {
			if (pathLengths[i] == 0) {
				pathLengths[i] = Double.MIN_VALUE;
				//logService.warn(this.getClass().getName() + ": Zero value detected at regression line position: " + (i+1));	
				System.out.println(this.getClass().getName() + ": Zero value detected at regression line position: " + (i+1));
			}
		}
	
		double lnX;
		double lnY;
		for (int i = 0; i < pathLengths.length; i++) {
			lnX = Math.log(eps[i]);
			lnY = Math.log(pathLengths[i]);
			lnDataX[i] = lnX;
			lnDataY[i] = lnY;				
		}
	
		// Compute regression
		Regression_Linear lr = new Regression_Linear();

//		double[] dataXArray = new double[lnDataX.size()];
//		double[] dataYArray = new double[lnDataY.size()];
//		for (int i = 0; i < lnDataX.size(); i++) {
//			dataXArray[i] = lnDataX.get(i).doubleValue();
//		}
//		for (int i = 0; i < lnDataY.size(); i++) {
//			dataYArray[i] = lnDataY.get(i).doubleValue();
//		}

		double[] regressionParams = lr.calculateParameters(lnDataX, lnDataY, numRegStart, numRegEnd);
		//0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
		
		return regressionParams;
	}
	
}
