/*-
 * #%L
 * Project: ImageJ signal plugin for computing fractal dimension with roughness scaling extraction.
 * File: RSE.java
 * 
 * $Id$
 * $HeadURL$
 * 
 * This file is part of ComsystanJ software, hereinafter referred to as "this program".
 * %%
 * Copyright (C) 2021 Comsystan Software
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
package at.csa.csaj.sig.frac.dim.rse.util;


import java.util.Random;

import org.scijava.log.LogService;

import at.csa.csaj.commons.regression.LinearRegression;

/**
 * Fractal Dimension using Roughness scaling extraction RSE
 * see Wang et al., Fractal Analysis on Artificial Profiles and Electroencephalography Signals by Roughness Scaling Extraction Algorithm
 * IEEE Access, 2019, DOI 10.1109/ACCESS.2019.2926515
 * 
 * See also
 * Li et al., A continuous variation of roughness scaling characteristics across fractal and non-fractal profiles, Fractals, 2021, https://doi.org/10.1142/S0218348X21501097.
 * Li et al. state that RSE dimension is smaller than 1 for non-fractal signals (constructed with -2 < D_W-M  < 1 ), see Figure 2
 * BUT this implementation according to Wang gives only Drse values slightly under 1!
 * 
 * <p>
 * <b>Changes</b>
 * <ul>
 * <li>
 * </ul>
 * 
 * 
 * @author Helmut Ahammer
 * @since 2021 08
 */

public class RSE {

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
	public RSE() {

	}

	/**
	 * This method calculates the root-mean-squared roughness or RMS of the series
	 * 
	 * @param sequence  1D data double[]
	 * @param lMax  number of newly calculated time series, lMax must be smaller than
	 *              the total number of time points lMax should not be greater than
	 *              N/3 (N number of data points)!
	 * @return double[] Rq[l] RMSs
	 */
	public double[] calcRqs(double[] sequence, int lMax) {
		
		int M = 50; //number of randomly chosen sub-sequences with same length according to Wang paper
		double sig = 0.85; //scaling factor was not used -> sig = 1;
		
		
		int rm; //random starting index
		Random random = new Random();
		double[] Rqm; //several Rq's with different starting point m;
		double[] Rq = new double[lMax];
		double[] subSequence;
		double meanSubSequence;
		double meanRq;
		int N = sequence.length;
		
		if (lMax > N) {
			lMax = N / 3;
			//logService.info(this.getClass().getName() + ": RMS parameter lMax too large, automatically set to data length/3");
			System.out.println(this.getClass().getName() + ": RMS parameter lMax too large, automatically set to data length/3");
		}

		for (int length = 2; length <= lMax; length++) { //several sub-sequence lengths, length = 1 gives back always 0 and must not be computed
			Rqm= new double[M];
			for (int m = 0; m < M; m++) { //m.. starting point
		
				rm = (int)Math.round(random.nextDouble() * (N - length)); //starting index
				subSequence = new double[length];
				for (int i  = 0; i < length; i++) {
					subSequence[i] = sequence[rm+i];
				}
				
				//Mean of subsequence
				meanSubSequence = 0.0;
				for (int s = 0; s < length; s++) {
					meanSubSequence += subSequence[s];
				}
				meanSubSequence = meanSubSequence/length;
				
				//Rq of subsequence
				Rqm[m] = 0.0;
				for (int s = 0; s < length; s++) {
					Rqm[m] += (subSequence[s]-meanSubSequence)*(subSequence[s]-meanSubSequence);
				}
				Rqm[m] = Math.sqrt(Rqm[m]/length);
			} // for m
			
			// Compute mean:
			meanRq = 0.0;
			for (int m = 1; m <= M; m++) {
				meanRq = meanRq + Rqm[m - 1];
			}
			meanRq = meanRq/M;
			
			Rq[length-1] =  meanRq;
		} //for l
		return Rq;
	}

	/**
	 * 
	 * @param Rq
	 * @param regStart
	 * @param regEnd
	 * @return double[] regression parameters
	 */
	public double[] calcDimension(double[] Rq, int regStart, int regEnd) {
		lnDataY = new double[Rq.length];
		lnDataX = new double[Rq.length]; //k
		//lnDataY = new Vector<Double>();
		//lnDataX = new Vector<Double>(); // k
		for (int i = 0; i < Rq.length; i++) {
			if (Rq[i] == 0)
				Rq[i] = Double.NaN;
		}
	
		for (int i = 0; i < Rq.length; i++) {
			double lnX = Math.log(i + 1); //lengths
			double lnY = Math.log(Rq[i]);
			lnDataX[i] = lnX;
			lnDataY[i] = lnY;
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
