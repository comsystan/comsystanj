/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: HurstSP.java
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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.scijava.log.LogService;


/**
 * Scaling property Hurst coefficient
 * 
 * Converted to Java and adapted from the Python project:
 * https://www.adrian.idv.hk/2021-07-26-hurst/
 * Adrian S. Tam  •  © 2023  •  CC-BY-SA 4.0 •  http://www.adrian.idv.hk
 * 
 * <p>
 * <b>Changes</b>
 * <ul>
 * <li>
 * </ul>
 * 
 * 
 * @author Helmut Ahammer
 * @since 2023 12
 */

public class HurstSP {

	private LogService logService;
	
	
	private int progressBarMin = 0;
	private int progressBarMax = 100;
	private double[] sequence;
	private double hurst;

	public double[] getSequence() {
		return sequence;
	}

	public void setSequence(double[] sequence) {
		this.sequence = sequence;
	}
	
	public double getH() {
		return hurst;
	}

	public void setH(double hurst) {
		this.hurst = hurst;
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
	public HurstSP() {

	}
	
	/**
	 * This is a constructor with a LogService
	 */
	public HurstSP(LogService logService) {
		this.logService = logService;
	}
	
	/**
	 * This method calculates the Hurst coefficient using the scaling method
	 * 
	 * @param sequence  1D data double[]
	 * @param maxLag maxLag   
	 * @return double H
	 */
	public double computeH(double[] data, int maxLag) {

        List<Integer> lags = new ArrayList<>();
        for (int i = 2; i < maxLag; i++) {
            lags.add(i);
        }
        
        double[] tsArray = new double[data.length];
        for (int i = 0; i < data.length; i++) {
            tsArray[i] = data[i];
        }
        
        List<Double> stdev = new ArrayList<>();
        for (int tau : lags) {
            double[] tsSlice = new double[tsArray.length - tau];
            for (int i = tau; i < tsArray.length; i++) {
                tsSlice[i - tau] = tsArray[i] - tsArray[i - tau];
            }
            double std = calculateStandardDeviation(tsSlice);
            stdev.add(std);
        }
        
        double[] logLags = new double[lags.size()];
        for (int i = 0; i < lags.size(); i++) {
            logLags[i] = Math.log(lags.get(i));
        }
        
        double[] logStdev = new double[stdev.size()];
        for (int i = 0; i < stdev.size(); i++) {
            logStdev[i] = Math.log(stdev.get(i));
        }
        
        double[] polyfit = fitPoly(logLags, logStdev, 1);
        hurst = polyfit[1]; 
        
        //or
//    	LinearRegression lr = new LinearRegression();
//		double[] regressionParams = lr.calculateParameters(logLags, logStdev, 1, lags.size());
//		//0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared    
//        hurst = regressionParams[1]; 
        
        return hurst;


	}
	
	private double calculateStandardDeviation(double[] array) {
        double sum = 0;
        for (double num : array) {
            sum += num;
        }
        double mean = sum / array.length;
        
        double sumOfSquaredDifferences = 0;
        for (double num : array) {
            sumOfSquaredDifferences += Math.pow(num - mean, 2);
        }
        
        return Math.sqrt(sumOfSquaredDifferences / array.length);
    }
    
    private double[] fitPoly(double[] x, double[] y, int degree) {
    	
	   WeightedObservedPoints points = new WeightedObservedPoints();
	   for (int i = 0; i < x.length; i++) {
	       points.add(x[i], y[i]);
	   }
	    
	   PolynomialCurveFitter fitter = PolynomialCurveFitter.create(1);
	   double[] coefficients = fitter.fit(points.toList());
	    
	   return coefficients;
    }

}
