/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: HurstRS.java
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
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;

import at.csa.csaj.commons.Util_GenerateInterval;


/**
 * Rescaled range Hurst coefficient
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

public class HurstRS {

	private LogService logService;
	
	
	private int progressBarMin = 0;
	private int progressBarMax = 100;
	private double[] sequence;
	private double hurst;
	private double[] lnDataX;
	private double[] lnDataY;

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
	public HurstRS() {

	}
	
	/**
	 * This is a constructor with a LogService
	 */
	public HurstRS(LogService logService) {
		this.logService = logService;
	}
	
	/**
	 * This method calculates the Hurst coefficient using the rescaled range RS method
	 * 
	 * @param sequence  1D data double[]
	 * @param int[] window interval 
	 * @return double H
	 */
	public double computeH(double[] data, int[] winInterval) {

		 double[] tsArray = new double[data.length];
	        for (int i = 0; i < data.length; i++) {
	            tsArray[i] = data[i];
	        }
	        List<Double> rsW = new ArrayList<>();
	        for (int tau : winInterval) {
	            List<Double> rs = new ArrayList<>();
	            for (int start = 0; start < tsArray.length; start += tau) {
	                double[] pts = Arrays.copyOfRange(tsArray, start, start + tau);
	                double r = max(pts) - min(pts);
	                double s = sqrt(mean(diffSquared(pts)));
	                rs.add(r / s);
	            }
	            rsW.add(mean(rs));
	        }
        
        lnDataX = new double[winInterval.length];
        for (int i = 0; i < winInterval.length; i++) {
            lnDataX[i] = Math.log(winInterval[i]);
        }
        
        lnDataY = new double[rsW.size()];
        for (int i = 0; i < rsW.size(); i++) {
            lnDataY[i] = Math.log(rsW.get(i));
        }
        
        double[] polyfit = fitPoly(lnDataX, lnDataY, 1);
        hurst = polyfit[1]; 
        
        //or
//    	LinearRegression lr = new LinearRegression();
//		double[] regressionParams = lr.calculateParameters(logWins, logStdev, 1, lags.size());
//		//0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared    
//        hurst = regressionParams[1]; 
        
        return hurst;


	}
	
 
    
    private double max(double[] arr) {
        double max = arr[0];
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] > max) {
                max = arr[i];
            }
        }
        return max;
    }
    
    private double min(double[] arr) {
        double min = arr[0];
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] < min) {
                min = arr[i];
            }
        }
        return min;
    }
    
    private double sqrt(double num) {
        return Math.sqrt(num);
    }
    
    private double mean(List<Double> rs) {
        double sum = 0;
        for (double num : rs) {
            sum += num;
        }
        return sum / rs.size();
    }
    
    private double[] diff(double[] arr) {
        double[] diffArr = new double[arr.length - 1];
        for (int i = 0; i < arr.length - 1; i++) {
            diffArr[i] = arr[i + 1] - arr[i];
        }
        return diffArr;
    }
    
    private List<Double> diffSquared(double[] arr) {
    	List<Double> diffArr = new ArrayList<>();
        for (int i = 0; i < arr.length - 1; i++) {
            diffArr.add((arr[i + 1] - arr[i]) * (arr[i + 1] - arr[i]));
        }
        return diffArr;
    }
    
    private double[] fitPoly(double[] x, double[] y, int deg) {
       WeightedObservedPoints points = new WeightedObservedPoints();
   	   for (int i = 0; i < x.length; i++) {
   	       points.add(x[i], y[i]);
   	   }
   	    
   	   PolynomialCurveFitter fitter = PolynomialCurveFitter.create(1);
   	   double[] coefficients = fitter.fit(points.toList());
   	    
   	   return coefficients;
    }
    

}
