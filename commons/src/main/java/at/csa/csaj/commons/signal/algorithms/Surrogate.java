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
		
		Random random = new Random();
		random.setSeed(System.currentTimeMillis());
		
		int index;
		int i = 0;
		int n = signalVec.size();
		while (signalVec.size() > 0){
			index = random.nextInt(signalVec.size());	
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
		Random random = new Random();
		random.setSeed(System.currentTimeMillis());
		double mean   = this.calcMean(data1D);
		double stdDev = this.calcStandardDeviation(data1D);
		double nextDataPoint;
		for (int i = 0; i < data1D.size(); i++){
			nextDataPoint = random.nextGaussian()*stdDev + mean;	
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
		Random random = new Random();
		random.setSeed(System.currentTimeMillis());
		double mean   = this.calcMean(signal);
		double stdDev = this.calcStandardDeviation(signal);
		
		for (int i = 0; i < surrogate.length; i++){
			surrogate[i]  = random.nextGaussian()*stdDev + mean;	
		}		
		return surrogate;
	}

	/**
	 * This method calculates a surrogate data double array using the phase randomized method
	 * The signal is FFT converted, phase randomized and inverse FFT back converted
	 * @param signal1D
	 * @return surrogate data 
	 */
	public double[] calcSurrogateRandomPhase(double[] signal1D, String windowingType) {	
		
		int signalLength = signal1D.length;
		Random random = new Random();
		random.setSeed(System.currentTimeMillis());	
		
		if (windowingType.equals("Rectangular")) {
			signal1D = windowingRectangular(signal1D);
		}
		else if (windowingType.equals("Cosine")) {
			signal1D = windowingCosine(signal1D);
		}
		else if (windowingType.equals("Lanczos")) {
			signal1D = windowingLanczos(signal1D);
		}
		else if (windowingType.equals("Bartlett")) {
			signal1D = windowingBartlett(signal1D);
		}
		else if (windowingType.equals("Hamming")) {
			signal1D = windowingHamming(signal1D);
		}
		else if (windowingType.equals("Hanning")) {
			signal1D = windowingHanning(signal1D);
		}
		else if (windowingType.equals("Blackman")) {
			signal1D = windowingBlackman(signal1D);
		}	
		else if (windowingType.equals("Gaussian")) {
			signal1D = windowingGaussian(signal1D);
		}
		else if (windowingType.equals("Parzen")) {
			signal1D = windowingParzen(signal1D);
		}
		
		//FFT needs power of two
		if (!isPowerOfTwo(signalLength)) {
			signal1D = addZerosUntilPowerOfTwo(signal1D);
		}
		
		double[] surrogate = new double[signal1D.length];	
		
		FastFourierTransformer transformer = new FastFourierTransformer(DftNormalization.STANDARD);
		Complex[] complx = transformer.transform(signal1D, TransformType.FORWARD);  
		
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
			angle = random.nextDouble()*2*Math.PI;
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
	 * @param windowing type
	 * @return surrogate data 
	 */
	public double[] calcSurrogateAAFT(double[] signal, String windowingType) {
		
		int signalLength = signal.length;
		double[] surrogate = new double[signalLength];
	
		//calculate rank of input signal
		NaturalRanking ranking = new NaturalRanking(NaNStrategy.REMOVED, TiesStrategy.SEQUENTIAL);
        double[] rankOfSignal = ranking.rank(signal);
	
     
        //Calculate Gaussian signal
    	Random random = new Random();
		random.setSeed(System.currentTimeMillis());;
		double[] gauss = new double[signalLength];
		for (int i = 0; i < signalLength; i++){
			gauss[i] = random.nextGaussian();	
		}


		//Rank Gaussian signal according to input signal
		double[] gaussRank = new double[signalLength];
		for (int i = 0; i < signalLength; i++){
			gaussRank[i] = gauss[(int) rankOfSignal[i]-1];
		}
		
        //calculate phase randomized signal of ranked Gaussian
		//this call fires also the progress bar events
        double[] gaussPhaseRandom = this.calcSurrogateRandomPhase(gaussRank, windowingType);
	
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
	public Vector<Vector<Double>> calcSurrogateSeries(Vector<Double> data1D, int method, String windowingType, int times){
		
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
		     		surrogate = this.calcSurrogateRandomPhase(signal, windowingType);
		     		break;
		     	case SURROGATE_AAFT:
		     		surrogate = this.calcSurrogateAAFT(signal, windowingType);
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
	
	/**
	 * This method does Rectangular windowing
	 * According to www.labbookpages.co.uk/audio/firWindowing.html#windows
	 * https://de.wikipedia.org/wiki/Fensterfunktion
	 * @param signal
	 * @return windowed signal
	 */
	private double[] windowingRectangular (double[] signal) {
		double weight = 1.0;
	     for(int i = 0; i < signal.length; ++i) {
	    	 signal[i] = signal[i] * weight;
	     }
	     return signal; 
	}
	
	/**
	 * This method does Cosine windowing
	 * According to www.labbookpages.co.uk/audio/firWindowing.html#windows
	 * https://de.wikipedia.org/wiki/Fensterfunktion
	 * @param signal
	 * @return windowed signal
	 */
	private double[] windowingCosine (double[] signal) {
		 double M = signal.length - 1;
		 double weight = 0.0;
	     for(int n = 0; n < signal.length; n++) {
	    	 weight = Math.sin(Math.PI*n/M);
	    	 signal[n] = signal[n] * weight;
	    	 //System.out.println("SignalFFT Cosine weight " + weight);
	     }
	     return signal; 
	}

	/**
	 * This method does  Lanczos windowing
	 * According to www.labbookpages.co.uk/audio/firWindowing.html#windows
	 * https://de.wikipedia.org/wiki/Fensterfunktion
	 * @param signal
	 * @return windowed signal
	 */
	private double[] windowingLanczos (double[] signal) {
		 double M = signal.length - 1;
		 double weight = 0.0;
		 double x = 0.0;
	     for(int n = 0; n < signal.length; n++) {
	    	 x = Math.PI*(2.0*n/M-1);
	    	 if (x == 0) weight = 1.0;
	    	 else weight =  Math.sin(x)/x;
	    	 signal[n] = signal[n] * weight;
	    	 //System.out.println("SignalFFT Lanczos weight  n " + n + "  "  + weight);
	     }
	     return signal; 
	}

	/**
	 * This method does Bartlett windowing
	 * According to www.labbookpages.co.uk/audio/firWindowing.html#windows
	 * https://de.wikipedia.org/wiki/Fensterfunktion
	 * @param signal
	 * @return windowed signal
	 */
	private double[] windowingBartlett (double[] signal) {
		 double M = signal.length - 1;
		 double weight = 0.0;
	     for(int n = 0; n < signal.length; n++) {
	    	 weight = 1.0-(2.0*Math.abs((double)n-M/2.0)/M);
	    	 signal[n] = signal[n] * weight;
	    	 //System.out.println("SignalFFT Bartlett weight " + weight);
	     }
	     return signal; 
	}

	/**
	 * This method does Hamming windowing
	 * According to www.labbookpages.co.uk/audio/firWindowing.html#windows
	 * https://de.wikipedia.org/wiki/Fensterfunktion
	 * @param signal
	 * @return windowed signal
	 */
	private double[] windowingHamming (double[] signal) {
		 double M = signal.length - 1;
		 double weight = 0.0;
	     for(int n = 0; n < signal.length; n++) {
	    	 weight = 0.54 - 0.46 * Math.cos(2.0 * Math.PI * n / M);
	    	 signal[n] = signal[n] * weight;
	    	 //System.out.println("SignalFFT Hamming weight " + weight);
	     }
	     return signal; 
	}

	/**
	 * This method does Hanning windowing
	 * According to www.labbookpages.co.uk/audio/firWindowing.html#windows
	 * https://de.wikipedia.org/wiki/Fensterfunktion
	 * @param signal
	 * @return windowed signal
	 */
	private double[] windowingHanning (double[] signal) {
		 double M = signal.length - 1;
		 double weight = 0.0;
	     for(int n = 0; n < signal.length; n++) {
	    	 weight = 0.5 - 0.5 * Math.cos(2.0 * Math.PI * n / M);
	    	 signal[n] = signal[n] * weight;
	    	 //System.out.println("SignalFFT Hanning weight " + weight);
	     }
	     return signal; 
	}
	
	/**
	 * This method does Blackman windowing
	 * According to www.labbookpages.co.uk/audio/firWindowing.html#windows
	 * https://de.wikipedia.org/wiki/Fensterfunktion
	 * @param signal
	 * @return windowed signal
	 */
	private double[] windowingBlackman (double[] signal) {
		 double M = signal.length - 1;
		 double weight = 0.0;
	     for(int n = 0; n < signal.length; n++) {
	    	 weight = 0.42 - 0.5 * Math.cos(2.0 * Math.PI * n / M) + 0.008 * Math.cos(4.0 * Math.PI * n / M);
	    	 signal[n] = signal[n] * weight;
	    	 //System.out.println("SignalFFT Blackman weight " + weight);
	     }
	     return signal; 
	}
	
	/**
	 * This method does Gaussian windowing
	 * According to www.labbookpages.co.uk/audio/firWindowing.html#windows
	 * https://de.wikipedia.org/wiki/Fensterfunktion
	 * @param signal
	 * @return windowed signal
	 */
	private double[] windowingGaussian (double[] signal) {
		 double M = signal.length - 1;
		 double weight = 0.0;
		 double sigma = 0.3;
		 double exponent = 0.0;
	     for(int n = 0; n < signal.length; n++) {
	    	 exponent = ((double)n-M/2)/(sigma*M/2.0);
	    	 exponent *= exponent;
	    	 weight = Math.exp(-0.5*exponent);
	    	 signal[n] = signal[n] * weight;
	    	 //System.out.println("SignalFFT Gaussian weight " + weight);
	     }
	     return signal; 
	}
	
	/**
	 * This method does Parzen windowing
	 * According to www.labbookpages.co.uk/audio/firWindowing.html#windows
	 * https://de.wikipedia.org/wiki/Fensterfunktion
	 * @param signal
	 * @return windowed signal
	 */
	private double[] windowingParzen (double[] signal) {
		double M = signal.length - 1;
		double nn;
		double weight = 0.0;
	    for(int n = 0; n < signal.length; n++) {
	    	nn = Math.abs((double)n-M/2);
	    	if      ((nn >= 0.0) && (nn < M/4))  weight = 1.0 - 6.0*Math.pow(nn/(M/2), 2) * (1- nn/(M/2));
	    	else if ((nn >= M/4) && (nn <= M/2)) weight = 2.0*Math.pow(1-nn/(M/2), 3);
	    	signal[n] = signal[n] * weight;
	      	//System.out.println("SignalFFT Parzen weight n " + n + "  "  + weight);
	     }
	     return signal; 
	}
	

}
