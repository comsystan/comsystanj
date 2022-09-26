/*-
 * #%L
 * Project: ImageJ2 sequence plugin for computing fractal dimension with 1D Higuchi algorithm.
 * File: Higuchi.java
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
package at.csa.csaj.plugin1d.frac.dim.hig1d.util;


import org.scijava.log.LogService;

import at.csa.csaj.commons.regression.LinearRegression;



/**
 * Fractal Dimension using Higuchi Dimension see T. Higuchi, Physica D 31, 1988,
 * 277 and T. Higuchi, Physica D 46, 1990, 254 see also W.Klonowski, E.
 * Olejarczyk, R. Stepien, P. Jalowiecki and R. Rudner, Monitoring The Depth Of
 * Anaesthesia Using Fractal Complexity Method,333-342 in:Complexus Mundi:
 * Emergent Patterns In Nature, M.M. Novak ed., World Scientific Publishing,
 * 2006
 * <p>
 * <b>Changes</b>
 * <ul>
 * <li>
 * </ul>
 * 
 * 
 * @author Helmut Ahammer
 * @since 2020 06
 */

public class Higuchi {

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
	public Higuchi() {

	}

	/**
	 * This method calculates the "Lengths" of the series
	 * 
	 * @param data  1D data double[]
	 * @param k     number of newly calculated time series, k must be smaller than
	 *              the total number of time points k should not be greater than
	 *             N/3 (N number of data points)!
	 * @return double[] L[k] "lengths"
	 */
	public double[] calcLengths(double[] data, int k) {
		int N = data.length;
		if (k > N) {
			k = N / 3;
			//logService.info(this.getClass().getName() + ": Higuchi parameter k too large, automatically set to data length/3");
			System.out.println(this.getClass().getName() + ": Higuchi parameter k too large, automatically set to data length/3");
		}

		// double[] L = new double[k];
		double[] L = new double[k];
		for (int kk = 1; kk <= k; kk++) {
			double[] Lmk = new double[kk];
			L[kk - 1] = 0.0;
			for (int m = 1; m <= kk; m++) {
				double norm = (N - 1) / Math.floor((double) (N - m) / (double) kk) / kk / kk;
				// double[] TimeSerArr = new double[(N-m)/kk];
				// TimeSerArr[0] = data[m-1];
				int i = 1;
				while (i <= (N - m) / kk) {
					// System.out.println("Higuchi i:" + i +" m:"+ m +" k:"+kk);
					// TimeSerArr[i] = data[m-1+i*kk];
					Lmk[m - 1] += Math.abs(data[m - 1 + i * kk] - data[m - 1 + (i - 1) * kk]) * norm;
					i = i + 1;
				}
				// Lmk[m-1] =
				// FLOAT(TOTAL(ABS(REFORM(TimeSerArr[0:*])-([REFORM(TimeSerArr[0]),
				// REFORM(TimeSerArr[0:*])])))) * FLOAT((N-1))
				// /FLOAT(FLOAT(N-m)/kk) / FLOAT(kk) /FLOAT(kk)
				// L.set(kk-1, L.get(kk-1).doubleValue() + Lmk[m-1]);
			}
			// L[kk-1] = FLOAT(TOTAL(Lmk))/ FLOAT(kk)
			// L[kk-1] = L[kk-1] / (double) kk;

			// Compute mean:
			double mean = 0.0;
			for (int m = 1; m <= kk; m++) {
				mean = mean + Lmk[m - 1];
			}
			mean = mean / kk;

			// //Optional Compute Median
			// double median = 0.0;
			// ArrayList<Double> severalLenghts = new ArrayList<Double>();
			// for (int m = 1; m <= kk; m++){
			// severalLenghts.add(Lmk[m-1]);
			// }
			// if (severalLenghts.size() > 0) {
			// Collections.sort(severalLenghts);
			// int middleOfInterval = (severalLenghts.size()) / 2;
			// if (severalLenghts.size() % 2 == 0) {
			// median = (severalLenghts.get(middleOfInterval)
			// + severalLenghts.get(middleOfInterval - 1)) / 2;
			// } else {
			// median = severalLenghts.get(middleOfInterval);
			// }
			// }

			L[kk - 1] =  mean;
			// L.set(kk-1, L.get(kk-1).doubleValue() /kk);
		}
		return L;
	}

	/**
	 * 
	 * @param L Higuchi Lengths
	 * @param regStart
	 * @param regEnd
	 * @return double Dh
	 */
	public double[] calcDimension(double[] L, int regStart, int regEnd) {
		lnDataY = new double[L.length];
		lnDataX = new double[L.length]; //k
		//lnDataY = new Vector<Double>();
		//lnDataX = new Vector<Double>(); // k
		for (int i = 0; i < L.length; i++) {
			if (L[i] == 0)
				L[i] = Double.MIN_VALUE;
		}
		// System.out.println("Higuchi: lnk ln(L)");
		//logService.info(this.getClass().getName() + ": Higuchi: lnk        ln(L)"); // Eliminate this line for Alex (auch noch in PlotOpFracHiguchi Zeile 126)
		//System.out.println(this.getClass().getName() + ": Higuchi: lnk        ln(L)"); // Eliminate this line for Alex (auch noch in PlotOpFracHiguchi Zeile 126)
	
		double lnX;
		double lnY;
		for (int i = 0; i < L.length; i++) {
			lnX = Math.log(i + 1);
			lnY = Math.log(L[i]);
			lnDataX[i] = lnX;
			lnDataY[i] = lnY;
			// System.out.println(lnX + " " + lnY);
			//logService.info(this.getClass().getName() + ": " +lnX + "        " + lnY); // Eliminate this line for Alex(auch noch in PlotOpFracHiguchi Zeile 126)
			//System.out.println(this.getClass().getName() + ": " +lnX + "        " + lnY); // Eliminate this line for Alex(auch noch in PlotOpFracHiguchi Zeile 126)				
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
