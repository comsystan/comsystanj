/*-
 * #%L
 * Project: ImageJ2 signal plugin for computing the Hurst coefficient.
 * File: BetaPSD.java
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
package at.csa.csaj.sig.frac.hurst.util;

import java.util.Vector;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.scijava.log.LogService;
import at.csa.csaj.commons.regression.LinearRegression;

/**
 * Computes the Power Spectral Density
 */
public class BetaPSD{

	
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
	public BetaPSD() {

	}
	
	/**
	 * 
	 * @param signal
	 * @param onlyLowFrequ
	 * @param regMin
	 * @param regMax
	 * @return
	 */
	public double[] computeRegression(double[] signal, int regMin, int regMax) {

		//double[] signalPower = this.calcDFTPower(signal);
		double[] signalPower = this.calcDFTPowerWithApache(signal); //far more faster 2sec instead of 5min!
		//signalPower.remove(0);
	
		String plotName = "Plot";
		lnDataY = new double[signalPower.length];
		lnDataX = new double[signalPower.length];

		//avoid zeros
		for (int i = 0; i < signalPower.length; i++){
			//if (signalPower[i] == 0) signalPower[i] = Double.MIN_VALUE;
		}
			
	
		for (int i = 0; i < signalPower.length; i++){
			lnDataX[i] = Math.log(i+1);
			//lnDataY[i] = Math.log(signalPower[i]) /Math.log(signalPower[0]);
			lnDataY[i] = Math.log(signalPower[i]);
		}
			
		// Compute regression
		LinearRegression lr = new LinearRegression();
		double[] regressionParams = lr.calculateParameters(lnDataX, lnDataY, regMin, regMax);
		//0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
		
		double[] result = {regressionParams[1], regressionParams[4], regressionParams[3]};  //slope, r2, slope standard error
		//logService.info(this.getClass().getName() + "  Beta:" + (-result[0])+ " r2:" + result[1] + " StdErr:" + result[2]);
		//System.out.println("BetaPSD  Beta:" + (-result[0])+ " r2:" + result[1] + " StdErr:" + result[2]);
		
		return result;
	}
	
	/**
	 * This method calculates the power spectrum of the DFT.
	 * @param signal
	 * @return the DFT power spectrum
	 */
	private double[] calcDFTPower(double[] signal) {
	
		int length = signal.length;
		double[]signalPower = new double[length/2]; //length/2 because spectrum is symmetric
		double sumReal = 0;
		double sumImag = 0;
		
		for (int k = 0; k < length/2; k++) { //length/2 because spectrum is symmetric
			sumReal = 0;
			sumImag = 0;
			for (int n = 0; n < length; n++) { //input points
				//double cos = Math.cos(2*Math.PI * n * k / length);
				//double sin = Math.sin(2*Math.PI * n * k / length);		
				sumReal +=  signal[n] * Math.cos(2*Math.PI * n * k / length);
				sumImag += -signal[n] * Math.sin(2*Math.PI * n * k / length);		
			}
			signalPower[k] = sumReal*sumReal+sumImag*sumImag;
		}
		return signalPower;
	}
	
	/**
	 * This method calculates the power spectrum of a 1D signal using Apache method
	 * This is 12 times faster (without padding with zeroes)
	 * @param sequence
	 * @return the DFT power spectrum
	 */
	private double[] calcDFTPowerWithApache(double[] signal) {
	
		int length = signal.length;
		double[]signalPower = new double[length/2]; //length/2 because spectrum is symmetric
		
		//FFT needs power of two
		if (!isPowerOfTwo(signal.length)) {
			signal = addZerosUntilPowerOfTwo(signal);
		}	
		FastFourierTransformer transformer = new FastFourierTransformer(DftNormalization.STANDARD);
	    Complex[] complex = transformer.transform(signal, TransformType.FORWARD);
	    for (int k =0; k < length/2; k++) {//length/2 because spectrum is symmetric
	    	signalPower[k] = complex[k].getReal()*complex[k].getReal() + complex[k].getImaginary()*complex[k].getImaginary();  
	    }
		return signalPower;
	}
	
	/**
	 * This method computes if a number is a power of 2
	 * 
	 * @param number
	 * @return
	 */
	public boolean isPowerOfTwo(int number) {
	    if (number % 2 != 0) {
	      return false;
	    } else {
	      for (int i = 0; i <= number; i++) {
	        if (Math.pow(2, i) == number) return true;
	      }
	    }
	    return false;
	 }
	
	/**
	 * This method increases the size of a signal to the next power of 2 
	 * 
	 * @param signal
	 * @return
	 */
	public double[] addZerosUntilPowerOfTwo (double[] signal) {
		int p = 1;
		double[] newSignal;
		int oldLength = signal.length;
		while (Math.pow(2, p) < oldLength) {
			p = p +1;
	    }
		newSignal = new double[(int) Math.pow(2, p)];
		for (int i = 0; i < oldLength; i++) {
			newSignal[i] = signal[i];
		}
		return newSignal;
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
