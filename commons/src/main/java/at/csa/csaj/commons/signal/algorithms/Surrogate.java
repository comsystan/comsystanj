/*-
 * #%L
 * Project: Commons for ComsystanJ ImageJ Plugins
 * File: Surrogate.java
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
package at.csa.csaj.commons.signal.algorithms;


import java.util.Random;
import java.util.Vector;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.stat.ranking.NaNStrategy;
import org.apache.commons.math3.stat.ranking.NaturalRanking;
import org.apache.commons.math3.stat.ranking.TiesStrategy;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;



/**
 * This class calculates surrogate data
 * Options: Shuffle, Gaussian, Phase randomized, AAFT, Pseudo-Periodic, Multivariate
 * see e.g. Mark Shelhammer, Nonlinear Dynamics in Physiology, World Scientific 2007
 * 
 * @author Helmut Ahammer
 * @since   2012 11
 */
public class Surrogate {

	public final static int SURROGATE_SHUFFLE         = 0;
	public final static int SURROGATE_GAUSSIAN        = 1;
	public final static int SURROGATE_RANDOMPHASE     = 2;
	public final static int SURROGATE_AAFT            = 3;
	public final static int SURROGATE_PSEUDOPERIODIC  = 4;
	public final static int SURROGATE_MULTIVARIATE    = 5;
	

	/**
	 * This is the standard constructor
	 */
	public Surrogate(){
		
	}

	/**
	 * This method constructs a double[] array from a Vector<Double>
	 * @param data1D
	 * @return double array
	 */
	private double[] convertVectorToDoubleArray(Vector<Double> data1D) {
		double[] doubleArray = new double[data1D.size()];
		for (int i = 0; i < doubleArray.length; i++){
			doubleArray[i] = data1D.get(i);
		}
		return doubleArray;
	}	
	/**
	 * This method constructs a Vector<Double> from a double[] array
	 * @param doubleArray
	 * @return a vector of doubles
	 */
	private Vector<Double> convertDoubleArrayToVector(double[] doubleArray) {
		Vector<Double> data1D = new Vector<Double>(doubleArray.length);			
		for (int i = 0; i < doubleArray.length; i++){
			data1D.add(doubleArray[i]);
		}
		return data1D;
	}
	
	/**
	 * This method calculates the mean of a data series
	 * @param data1D
	 * @return Double Mean
	 */
	public Double calcMean(Vector<Double> data1D){
		double sum = 0;
		for(double d: data1D){
			sum += d;
		}
		return sum/data1D.size();
	}
	/**
	 * This method calculates the mean of a data series
	 * @param doubleArray
	 * @return Double Mean
	 */
	public Double calcMean(double[] doubleArray){
		double sum = 0;
		for(double d: doubleArray){
			sum += d;
		}
		return sum/doubleArray.length;
	}
	
	/**
	 * This method calculates the variance of a data series
	 * @param data1D
	 * @return Double Variance
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
	 * This method calculates the variance of a data series
	 * @param doubleArray
	 * @return Double Variance
	 */
	private double calcVariance(double[] doubleArray){
		double mean = calcMean(doubleArray);
		double sum = 0;
		for(double d: doubleArray){
			sum = sum + ((d - mean) * (d - mean));
		}
		return sum/(doubleArray.length-1);  //  1/(n-1) is used by histo.getStandardDeviation() too
	}
	
	/**
	 * This method calculates the standard deviation of a data series
	 * @param data1D
	 * @return Double standard deviation
	 */
	private double calcStandardDeviation(Vector<Double> data1D){
		double variance  = this.calcVariance(data1D);
		return Math.sqrt(variance);
	}	
	/**
	 * This method calculates the standard deviation of a data series
	 * @param doubleArray
	 * @return Double standard deviation
	 */
	private double calcStandardDeviation(double[] doubleArray){
		double variance  = this.calcVariance(doubleArray);
		return Math.sqrt(variance);
	}
	
	/**
	 * This method calculates a surrogate data double array using the shuffle method
	 * The signal is randomly shuffled
	 * @param signal
	 * @return surrogate data 
	 */
	public double[] calcSurrogateShuffle(double[] signal) {	
		double[] surrogate = new double[signal.length];
		Vector<Double> signalVec = this.convertDoubleArrayToVector(signal);
		
		Random generator = new Random();
		int index;
		int i = 0;
		int n = signalVec.size();
		while (signalVec.size() > 0){
			index = generator.nextInt(signalVec.size());	
			surrogate[i] = signalVec.get(index);
			signalVec.removeElementAt(index);
			i = i + 1;
		}
			
		return surrogate;
	}
	/**
	 * This method calculates a surrogate data vector using the Gaussian method
	 * A Gaussian signal with identical mean and standard deviation as the original signal is constructed 
	 * @param data1D
	 * @return surrogate data 
	 */
	public Vector<Double> calcSurrogateGaussian(Vector<Double> data1D) {	
		Vector<Double> vec = new Vector<Double>();
		Random generator = new Random();
		double mean   = this.calcMean(data1D);
		double stdDev = this.calcStandardDeviation(data1D);
		double nextDataPoint;
		for (int i = 0; i < data1D.size(); i++){
			nextDataPoint = generator.nextGaussian()*stdDev + mean;	
			vec.add(nextDataPoint);
		}		
		return vec;
	}
	/**
	 * This method calculates a surrogate data double array using the Gaussian method
	 * A Gaussian signal with identical mean and standard deviation as the original signal is constructed 
	 * @param signal
	 * @return surrogate data 
	 */
	public double[] calcSurrogateGaussian(double[] signal) {	
		double[] surrogate = new double[signal.length];
		Random generator = new Random();
		double mean   = this.calcMean(signal);
		double stdDev = this.calcStandardDeviation(signal);
		
		for (int i = 0; i < surrogate.length; i++){
			surrogate[i]  = generator.nextGaussian()*stdDev + mean;	
		}		
		return surrogate;
	}

	/**
	 * This method calculates a surrogate data double array using the phase randomized method
	 * The signal is FFT converted, phase randomized and inverse FFT back converted
	 * @param signal
	 * @return surrogate data 
	 */
	public double[] calcSurrogateRandomPhase(double[] signal) {	
		
		int signalLength = signal.length;
		Random generator = new Random();	
		
		//FFT needs power of two
		if (!isPowerOfTwo(signalLength)) {
			signal = addZerosUntilPowerOfTwo(signal);
		}
		
		double[] surrogate = new double[signal.length];	
		
		FastFourierTransformer transformer = new FastFourierTransformer(DftNormalization.STANDARD);
		Complex[] complx = transformer.transform(signal, TransformType.FORWARD);  
		
//	    double[] real = new double[complx.length];
//	    double[] imaginary = new double[complx.length];
//	
//	    for(int i=0; i<real.length; ++i) {
//	      real[i] = complx[i].getReal();
//	      imaginary[i] = complx[i].getImaginary();
//	    }

		//shuffle phase part and get back complex number
		double angle;
		double magnitude;
		for (int i = 0; i < complx.length; i++){		
			angle = generator.nextDouble()*2*Math.PI;
			//get Magnitude;
			magnitude = Math.sqrt(complx[i].getImaginary()*complx[i].getImaginary() + complx[i].getReal()*complx[i].getReal());
			//set back complex number with identical magnitude but shuffled phase
			complx[i] = new Complex(magnitude*Math.cos(angle), magnitude*Math.sin(angle)); 
		}

        Complex[] inverseTransform = transformer.transform(complx, TransformType.INVERSE);  
  
        //Get real parts for output
        for(int i=0; i<inverseTransform.length; i++){
            surrogate[i] = inverseTransform[i].getReal();
        }  
        //Maybe decrease size of signal to power of two
      	if (!isPowerOfTwo(signalLength)) {

      		double[] temp = new double[signalLength];
      		for (int i = 0; i < signalLength; i++) {
      			temp[i] = surrogate[i];
      		}
      		surrogate =  temp;
      	} 
		return surrogate;
	}



	/**
	 * This method calculates a surrogate data double array using the AAFT (amplitude adjusted FT) method
	 * A Gaussian signal y is constructed
	 * y is ranked according to the original signal
	 * then y is FFT converted, phase randomized and inverse FFT back converted yielding y'
	 * the original signal is ranked according to y'
	 * @param signal
	 * @return surrogate data 
	 */
	public double[] calcSurrogateAAFT(double[] signal) {
		
		int signalLength = signal.length;
		double[] surrogate = new double[signalLength];
	
		//calculate rank of input signal
		NaturalRanking ranking = new NaturalRanking(NaNStrategy.REMOVED, TiesStrategy.SEQUENTIAL);
        double[] rankOfSignal = ranking.rank(signal);
	
     
        //Calculate Gaussian signal
		Random generator = new Random();
		double[] gauss = new double[signalLength];
		for (int i = 0; i < signalLength; i++){
			gauss[i] = generator.nextGaussian();	
		}


		//Rank Gaussian signal according to input signal
		double[] gaussRank = new double[signalLength];
		for (int i = 0; i < signalLength; i++){
			gaussRank[i] = gauss[(int) rankOfSignal[i]-1];
		}
		
        //calculate phase randomized signal of ranked Gaussian
		//this call fires also the progress bar events
        double[] gaussPhaseRandom = this.calcSurrogateRandomPhase(gaussRank);
	
        //calculate rank of Gaussian (Ranked) phase randomized
		ranking = new NaturalRanking(NaNStrategy.REMOVED, TiesStrategy.SEQUENTIAL);
        double[] rankOfGaussPhaseRandom = ranking.rank(gaussPhaseRandom);
	
  
        //Rank input signal according to Gaussian (Ranked) phase randomized
		for (int i = 0; i < signalLength; i++){
			surrogate[i] = signal[(int)  rankOfGaussPhaseRandom[i]-1];
		}
		
		return surrogate;
	}
	/**
	 * This method calculates a surrogate data double array using the pseudo periodic method
	 * Not yet implemented
	 * @param signal
	 * @return surrogate data 
	 */
	public double[] calcSurrogatePseudoPeriodic(double[] signal) {	
		double[] surrogate = new double[signal.length];
			
		return surrogate;
	}
	/**
	 * This method calculates a surrogate data double array using the multivariate method
	 * Not yet implemented
	 * @param signal
	 * @return surrogate data 
	 */
	public double[] calcSurrogateMultivariate(double[] signal) {	
		double[] surrogate = new double[signal.length];
			
		return surrogate;
	}
	
	/**
	 * This method calculates new surrogate series
	 * @param data1D 1D data vector
	 * @param method method of surrogate data generation
	 * @param times number of new series
	 * @return Vector of Series (vectors)
	 * 
	 */
	public Vector<Vector<Double>> calcSurrogateSeries(Vector<Double> data1D, int method, int times){
		Vector<Vector<Double>> surrogateSeries = new Vector<Vector<Double>>(times);
		double[] signal = this.convertVectorToDoubleArray(data1D);
			
		for (int i = 0; i < times; i++) { //number of new series
			double[] surrogate = new double[data1D.size()];
			 switch(method){
		     	case SURROGATE_SHUFFLE:
		     		surrogate = this.calcSurrogateShuffle(signal);
		     		break;
		     	case SURROGATE_GAUSSIAN:
		     		surrogate = this.calcSurrogateGaussian(signal);
		     		break;
		     	case SURROGATE_RANDOMPHASE:
		     		surrogate = this.calcSurrogateRandomPhase(signal);
		     		break;
		     	case SURROGATE_AAFT:
		     		surrogate = this.calcSurrogateAAFT(signal);
		     		break;
		     	case SURROGATE_PSEUDOPERIODIC:
		     		surrogate = this.calcSurrogatePseudoPeriodic(signal);
		     		break;
		     	case SURROGATE_MULTIVARIATE:
		     		surrogate = this.calcSurrogateMultivariate(signal);
		     		break;
		        default:
		            System.out.println("no valid surrogate method choosen");
		     } 
			surrogateSeries.add(this.convertDoubleArrayToVector(surrogate));
		}				     			   
		return surrogateSeries;
	
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

}
