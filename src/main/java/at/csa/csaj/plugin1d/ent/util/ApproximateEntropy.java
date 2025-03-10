/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: ApproximateEntropy.java
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
package at.csa.csaj.plugin1d.ent.util;

import org.scijava.log.LogService;

/**
 * Pincus Approximate Entropy Pincus S.M., Approximate entropy as a measure of
 * system complexity, Proc. Natl. Acad. Sci. USA, Vol.88, 2297-2301, 1991
 * <p>
 * <b>Changes</b>
 * <ul>
 * 	<li>
 * </ul>
 * 
 * @author Helmut Ahammer
 * @since  2021-02-25
 */
public class ApproximateEntropy {
	
	private LogService logService;
	
	private int numbDataPoints = 0; // length of 1D data series

	/**
	 * This is the standard constructor with the possibility to import calling
	 * class This is useful for progress bar and canceling functionality
	 * 
	 * @param operator
	 * 
	 */
	public ApproximateEntropy(LogService logService) {
		this.logService = logService;
	}


	/**
	 * This is the standard constructor
	 */
	public ApproximateEntropy() {
	}

	/**
	 * This method calculates the mean of a data series
	 * 
	 * @param data1D
	 * @return the mean
	 */
	private Double calcMean(double[] data1D) {
		double sum = 0;
		for (double d : data1D) {
			sum += d;
		}
		return sum / data1D.length;
	}

	/**
	 * This method calculates the variance of a data series
	 * 
	 * @param data1D
	 * @return the variance
	 */
	private double calcVariance(double[] data1D) {
		double mean = calcMean(data1D);
		double sum = 0;
		for (double d : data1D) {
			sum = sum + ((d - mean) * (d - mean));
		}
		return sum / (data1D.length - 1); // 1/(n-1) is used by histo.getStandardDeviation() too
	}

	/**
	 * This method calculates the standard deviation of a data series
	 * 
	 * @param data1D
	 * @return the standard deviation
	 */
	private double calcStandardDeviation(double[] data1D) {
		double variance = this.calcVariance(data1D);
		return Math.sqrt(variance);
	}

	/**
	 * This method calculates new data series
	 * 
	 * @param data1D   1D data vector
	 * @param m: number of new calculated time series (m = 2, Pincus et al. 1994)
	 * @return Vector of Series (vectors)
	 * 
	 */
	private double[][] calcNewSeries(double[] data1D, int m,
			int d) {
		// int numSeries = numbDataPoints-m+1;
		int numSeries = numbDataPoints - (m - 1) * d;
		int numSerieLength = (m - 1) * d + 1;
		double[][] newDataSeries = new double[numSeries][numSerieLength];
		for (int i = 0; i < numSeries; i++) {
		
			// for(int ii = i; ii <= i+m-1 ; ii++){ //get m data points
			for (int ii = i; ii <= i + (m - 1) * d; ii = ii + d) { // get m data points stepwidth = delay
				newDataSeries[i][ii-i] = data1D[ii];
			}
		}
		return newDataSeries;
	}

	/**
	 * This method calculates the number of correlations
	 * 
	 * @param newDataSeries vector of 1D vectors
	 * @param distR distance in %of SD
	 * @return Vector (Number of Correlations)
	 * 
	 */
	private int[] calcNumberOfCorrelations(double[][] newDataSeries, int m, double distR) {

		int numSeries = newDataSeries.length;
		int[] numberOfCorrelations = new int[numSeries];
		double[] seriesI = null;
		double[] seriesJ = null;
		double distMax;
		double dist;
		
//		for (int i = 0; i < numSeries; i++) { // initialize Vector
//		numberOfCorrelations[i] = 0;
//	}
		for (int i = 0; i < numSeries; i++) {
			for (int j = 0; j < numSeries; j++) {
				seriesI = new double[newDataSeries[i].length];
				seriesJ = new double[newDataSeries[j].length];
				for (int ni = 0; ni < newDataSeries[i].length ;ni++) seriesI[ni] = newDataSeries[i][ni];
				for (int nj = 0; nj < newDataSeries[j].length ;nj++) seriesJ[nj] = newDataSeries[j][nj];
				distMax = 0;
				for (int k = 1; k <= m; k++) {
					dist = Math.abs(seriesI[k-1] - seriesJ[k-1]);
					if (dist > distMax) {
						distMax = dist;
					}
				}
				if (distMax <= distR) {
					numberOfCorrelations[i] = numberOfCorrelations[i] + 1;
				}
			}
		}
		return numberOfCorrelations;
	}

	/**
	 * This method calculates logarithm of correlations
	 * 
	 * @param numberOfCorrelations vector of mumbers
	 * @param m number of new calculated time series (m = 2, Pincus et al. 1994)
	 * @param d delay
	 * @return Vector correlations
	 */
	private double[] calcLogCorrelations(int[] numberOfCorrelations, int m, int d) {

		double[] correlations = new double[numberOfCorrelations.length];
		double value;
		for (int n = 0; n < numberOfCorrelations.length; n++) {
			// double value =
			// (double)numberOfCorrelations.get(n)/(numbDataPoints-m+1);
			value = ((double) numberOfCorrelations[n] / (numbDataPoints - (m - 1) * d)); //

			if (value > 0)
				correlations[n] = Math.log(value);
		}
		return correlations;
	}

	/**
	 * This method calculates the sum of correlations
	 * 
	 * @param correlations vector of correlations
	 * @param m number of newly calculated time series (m = 2, Pincus et al.1994) 
	 * @param d delay
	 * @return sumOfCorrelations double
	 * 
	 */
	private double calcSumOfCorrelation(double[] correlations, int m,
			int d) {
		double sumOfCorrelations = 0;
		for (int n = 0; n < correlations.length; n++) {
			sumOfCorrelations = sumOfCorrelations + correlations[n];
		}
		// sumOfCorrelations = sumOfCorrelations/(numbDataPoints-m+1);
		sumOfCorrelations = sumOfCorrelations / (numbDataPoints - (m - 1) * d);
		return sumOfCorrelations;
	}

	/**
	 * This method calculates the approximate entropy
	 * 
	 * @param data1D 1D data vector
	 * @param m number of newly calculated time series (m = 2, Pincus et al.1994) m should not be greater than N/3 (N number of data
	 *            points)! 
	 * @param r maximal distance radius r (10%sd < r < 25%sd sd =
	 *            standard deviation of time series, Pincus et al. 1994)
	 * @param d delay
	 * @return Approximate Entropy (single double value)
	 * 
	 */
	public double calcApproximateEntropy(double[] data1D, int m, double r, int d) {
		numbDataPoints = data1D.length;
		if (m > numbDataPoints / 3) {
			m = numbDataPoints / 3;
			logService.info(this.getClass().getName() + " Parameter m too large, automatically set to data length/3");
		}
		if (m < 1) {
			logService.info(this.getClass().getName() + " Parameter m too small, Sample entropy cannot be calulated");
			return 99999999d;
		}
		if (d < 0) {
			logService.info(this.getClass().getName() + " Delay too small, Sample entropy cannot be calulated");
			return 999999999d;
		}

		double     appEntropy = 0d;
		double[]   fmr = new double[2];
		double[][] newDataSeries;
		double     distR;
		int[]      numberOfCorrelations;
		double[]   correlations;
		double     sumOfCorrelations;
		
		for (int mm = m; mm <= m + 1; mm++) {
			newDataSeries = this.calcNewSeries(data1D, mm, d);
			distR = this.calcStandardDeviation(data1D) * r;
			numberOfCorrelations = this.calcNumberOfCorrelations(newDataSeries, mm, distR);
			correlations = this.calcLogCorrelations(numberOfCorrelations, mm, d);
			sumOfCorrelations = this.calcSumOfCorrelation(correlations, mm, d);
			fmr[mm - m] = sumOfCorrelations;
		}

		appEntropy = fmr[0] - fmr[1];
		return appEntropy;
	}

}
