/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: BetaPSD.java
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

import java.util.Vector;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.scijava.log.LogService;

import at.csa.csaj.commons.Regression_Linear;

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
	 * @param sequence
	 * @param onlyLowFrequ
	 * @param numRegStart
	 * @param numRegEnd
	 * @return
	 */
	public double[] computeRegression(double[] sequence, int numRegStart, int numRegEnd) {

		//double[] sequencePower = this.calcDFTPower(sequence);
		double[] sequencePower = this.calcDFTPowerWithApache(sequence); //far more faster 2sec instead of 5min!
		//sequencePower.remove(0);
	
		String plotName = "Plot";
		lnDataY = new double[sequencePower.length];
		lnDataX = new double[sequencePower.length];

		//avoid zeros
		for (int i = 0; i < sequencePower.length; i++){
			//if (sequencePower[i] == 0) sequencePower[i] = Double.MIN_VALUE;
		}
			
	
		for (int i = 0; i < sequencePower.length; i++){
			lnDataX[i] = Math.log(i+1);
			//lnDataY[i] = Math.log(sequencePower[i]) /Math.log(sequencePower[0]);
			lnDataY[i] = Math.log(sequencePower[i]);
		}
			
		// Compute regression
		Regression_Linear lr = new Regression_Linear();
		double[] regressionParams = lr.calculateParameters(lnDataX, lnDataY, numRegStart, numRegEnd);
		//0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
		
		double[] result = {regressionParams[1], regressionParams[4], regressionParams[3]};  //slope, r2, slope standard error
		//logService.info(this.getClass().getName() + "  Beta:" + (-result[0])+ " r2:" + result[1] + " StdErr:" + result[2]);
		//System.out.println("BetaPSD  Beta:" + (-result[0])+ " r2:" + result[1] + " StdErr:" + result[2]);
		
		return result;
	}
	
	/**
	 * This method calculates the power spectrum of the DFT.
	 * @param sequence
	 * @return the DFT power spectrum
	 */
	private double[] calcDFTPower(double[] sequence) {
	
		int length = sequence.length;
		double[]sequencePower = new double[length/2]; //length/2 because spectrum is symmetric
		double sumReal = 0;
		double sumImag = 0;
		
		for (int k = 0; k < length/2; k++) { //length/2 because spectrum is symmetric
			sumReal = 0;
			sumImag = 0;
			for (int n = 0; n < length; n++) { //input points
				//double cos = Math.cos(2*Math.PI * n * k / length);
				//double sin = Math.sin(2*Math.PI * n * k / length);		
				sumReal +=  sequence[n] * Math.cos(2*Math.PI * n * k / length);
				sumImag += -sequence[n] * Math.sin(2*Math.PI * n * k / length);		
			}
			sequencePower[k] = sumReal*sumReal+sumImag*sumImag;
		}
		return sequencePower;
	}
	
	/**
	 * This method calculates the power spectrum of a 1D sequence using Apache method
	 * This is 12 times faster (without padding with zeroes)
	 * @param sequence
	 * @return the DFT power spectrum
	 */
	private double[] calcDFTPowerWithApache(double[] sequence) {
	
		int length = sequence.length;
		double[]sequencePower = new double[length/2]; //length/2 because spectrum is symmetric
		
		//FFT needs power of two
		if (!isPowerOfTwo(sequence.length)) {
			sequence = addZerosUntilPowerOfTwo(sequence);
		}	
		FastFourierTransformer transformer = new FastFourierTransformer(DftNormalization.STANDARD);
	    Complex[] complex = transformer.transform(sequence, TransformType.FORWARD);
	    for (int k =0; k < length/2; k++) {//length/2 because spectrum is symmetric
	    	sequencePower[k] = complex[k].getReal()*complex[k].getReal() + complex[k].getImaginary()*complex[k].getImaginary();  
	    }
		return sequencePower;
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
	 * This method increases the size of a sequence to the next power of 2 
	 * 
	 * @param sequence
	 * @return
	 */
	public double[] addZerosUntilPowerOfTwo (double[] sequence) {
		int p = 1;
		double[] newSequence;
		int oldLength = sequence.length;
		while (Math.pow(2, p) < oldLength) {
			p = p +1;
	    }
		newSequence = new double[(int) Math.pow(2, p)];
		for (int i = 0; i < oldLength; i++) {
			newSequence[i] = sequence[i];
		}
		return newSequence;
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
