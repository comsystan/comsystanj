/*-
 * #%L
 * Project: ImageJ2 signal plugin for computing the Fragmentation fractal dimension of a distribution of sizes.
 * File: FragmentationDimension.java
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
package at.csa.csaj.plugin1d.frac.dim.frag.util;


import java.util.Arrays;

import javax.swing.JOptionPane;

import org.scijava.log.LogService;

import at.csa.csaj.commons.regression.LinearRegression;



/**
 * Fractal Dimension of a size distribution
 * Equivalent to Benoit software
 * x...size
 * n(x)...number of objects with linear sizes greater than x
 * Fractal relation ln(n) = D.ln(x)
 * 
 * <p>
 * <b>Changes</b>
 * <ul>
 * <li>
 * </ul>
 * 
 * 
 * @author Helmut Ahammer
 * @since 2024 01
 */

public class FragmentationDimension {

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
	public FragmentationDimension() {

	}
	
	/**
     * This method removes duplicates
     * Returns the new length of the array
     * 
     * @param array
     * @return length
     */
    private int removeDuplicates(double[] array) {
   
        double[] temp = new double[array.length];
 
        // Start traversing elements
        int j = 0;
        for (int i = 0; i < array.length - 1; i++)
             
            // If current element is not equal to next
            // element then store that current element
            if (array[i] != array[i + 1]) temp[j++] = array[i];
 
        // Store the last element as whether it is unique or
        // repeated, it hasn't stored previously
        temp[j++] = array[array.length - 1];
 
        // Modify original array
        for (int i = 0; i < j; i++) array[i] = temp[i];
 
        return j;
    }
    
    /**
	 * This method calculates the number of sizes without duplicates
	 * 
	 * @param data  1D data double[]
	 * @return int number of distinct sizes 
	 */
	public int calcNumberOfSizes(double[] data) {
		
		double[] dataClone = data.clone();
		Arrays.sort(dataClone);
		return removeDuplicates(dataClone);	
		
	}

	/**
	 * This method calculates the array of sizes X
	 * 
	 * @param data  1D data double[]
	 * @return double[] X "sizes"
	 */
	public double[] calcSizes(double[] data) {
		
		double[] dataClone = data.clone();
		Arrays.sort(dataClone);
		int newLength = removeDuplicates(dataClone);	
		double[] X = new double[newLength];
		for (int x = 0; x < newLength; x++) {
			if (dataClone[x] < 0) {
				JOptionPane.showMessageDialog(null, "Computation of Fragmentation dimension not possible", "Negative value detected", JOptionPane.WARNING_MESSAGE);
				return null;
			}
			X[x] = dataClone[x];
		}
		 
		return X;
	}
	

	/**
	 * This method calculates the the array N of occurrences greater than x
	 * 
	 * @param data  1D data double[]
	 * @param data  X  double[]
	 * @return double[] N "numbers"
	 */
	public double[] calcNumbers(double[] data, double[] X) {
		
		if (X == null) return null; //Negative values
		
		double[] N = new double[X.length];
		for (int x = 0; x < X.length; x++) {
			for (int d = 0; d < data.length; d++) {
				if(data[d] > X[x]) N[x] += 1;
			}
		}
		return N;
	}

	/**
	 * 
	 * @param N Numbers
	 * @param X Sizes
	 * @param regStart
	 * @param regEnd
	 * @return double Dfrag
	 */
	public double[] calcDimension(double[] data, int regStart, int regEnd) {
	
		double[] X = this.calcSizes(data);
		double[] N = this.calcNumbers(data, X);
		
		if (N == null) { //negative values
			double[] regressionParams = new double[5];
			for (int r = 0; r < regressionParams.length; r++) regressionParams[r] = Double.NaN; 		
			return regressionParams;
		}
		
		lnDataY = new double[X.length];
		lnDataX = new double[X.length]; //x

		for (int i = 0; i < X.length; i++) {
			if (X[i] == 0) X[i] = Double.MIN_VALUE;
			if (N[i] == 0) N[i] = Double.MIN_VALUE;
		}
		// System.out.println("Fragmentation: ");
		
		double lnX;
		double lnY;
		for (int i = 0; i < X.length; i++) {
			lnX = Math.log(X[i]);
			lnY = Math.log(N[i]);
			lnDataX[i] = lnX;
			lnDataY[i] = lnY;
			// System.out.println(lnX + " " + lnY);
		}
	
		// Compute regression
		LinearRegression lr = new LinearRegression();

//		double[] dataXArray = new double[lnDataX.size()];
//		double[] dataYArray = new double[lnDataY.size()];
//		for (int i = 0; i < lnDataX.size(); i++) {
//			dataXArray[i] = lnDataX.get(i).doubleValue();
//		}
//		for (int i = 0; i < lnDataY.size(); i++) {
//			dataYArray[i] = lnDataY.get(i).doubleValue();
//		}

		double[] regressionParams = lr.calculateParameters(lnDataX, lnDataY, regStart, regEnd);
		//0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
		
 		return regressionParams;
	}
	
}
