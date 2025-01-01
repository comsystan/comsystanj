/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: CsajAlgorithm_Surrogate1D.java
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
package at.csa.csaj.commons;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
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
public class CsajAlgorithm_Surrogate1D {

	public final static int SURROGATE_SHUFFLE         = 0;
	public final static int SURROGATE_GAUSSIAN        = 1;
	public final static int SURROGATE_RANDOMPHASE     = 2;
	public final static int SURROGATE_AAFT            = 3;
	public final static int SURROGATE_PSEUDOPERIODIC  = 4;
	public final static int SURROGATE_MULTIVARIATE    = 5;
	
	double[] surrogate;
	List<Double> list;
	

	/**
	 * This is the standard constructor
	 */
	public CsajAlgorithm_Surrogate1D(){
		
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
	 * @param doubleArray
	 * @return Double standard deviation
	 */
	private double calcStandardDeviation(double[] doubleArray){
		double variance  = this.calcVariance(doubleArray);
		return Math.sqrt(variance);
	}
	
	/**
	 * This method calculates a surrogate data double array using the shuffle method
	 * The sequence is randomly shuffled
	 * @param sequence
	 * @return surrogate data 
	 */
	public double[] calcSurrogateShuffle(double[] sequence) {	
	
		surrogate = new double[sequence.length];
		list = new ArrayList<Double>();
		
		for (int i = 0; i < sequence.length; i++) list.add(sequence[i]);
	
		//Shuffle
		Random random = new Random();
		random.setSeed(System.currentTimeMillis());
		Collections.shuffle(list, random);
		//Collections.Shuffle() does a Fisher-Yates shuffle.
		//It's a more evenly distributed form of shuffling,
		//and does not reshuffle what might have previously been shuffled already.
		//The Fisher-Yates shuffle (also known as Donald Knuth Shuffle)
		//is an unbiased algorithm that shuffles items in the array in an equally likely probability.
		//It avoids the chance of 'moving' the same object twice.
		//The easy way (also the known as the naive implementation) is
		//to pick randomly any array index and shuffle it over,
		//meaning there's a high chance of picking the same index that has already been shuffled.

		for (int i = 0; i < surrogate.length; i++) surrogate[i] = list.get(i);
		
		list.clear();
		list = null;
		
		return surrogate;
	}

	/**
	 * This method calculates a surrogate data double array using the Gaussian method
	 * A Gaussian sequence with identical mean and standard deviation as the original sequence is constructed 
	 * @param sequence
	 * @return surrogate data 
	 */
	public double[] calcSurrogateGaussian(double[] sequence) {	
		surrogate = new double[sequence.length];
		Random random = new Random();
		random.setSeed(System.currentTimeMillis());
		double mean   = this.calcMean(sequence);
		double stdDev = this.calcStandardDeviation(sequence);
		
		for (int i = 0; i < surrogate.length; i++)surrogate[i]  = random.nextGaussian()*stdDev + mean;			
		
		return surrogate;
	}

	/**
	 * This method calculates a surrogate data double array using the phase randomized method
	 * The sequence is FFT converted, phase randomized and inverse FFT back converted
	 * @param sequence
	 * @return surrogate data 
	 */
	public double[] calcSurrogateRandomPhase(double[] sequence, String windowingType) {	
		
		int sequenceLength = sequence.length;
		Random random = new Random();
		random.setSeed(System.currentTimeMillis());	
		
		if (windowingType.equals("Rectangular")) {
			sequence = windowingRectangular(sequence);
		}
		else if (windowingType.equals("Cosine")) {
			sequence = windowingCosine(sequence);
		}
		else if (windowingType.equals("Lanczos")) {
			sequence = windowingLanczos(sequence);
		}
		else if (windowingType.equals("Bartlett")) {
			sequence = windowingBartlett(sequence);
		}
		else if (windowingType.equals("Hamming")) {
			sequence = windowingHamming(sequence);
		}
		else if (windowingType.equals("Hanning")) {
			sequence = windowingHanning(sequence);
		}
		else if (windowingType.equals("Blackman")) {
			sequence = windowingBlackman(sequence);
		}	
		else if (windowingType.equals("Gaussian")) {
			sequence = windowingGaussian(sequence);
		}
		else if (windowingType.equals("Parzen")) {
			sequence = windowingParzen(sequence);
		}
		
		//FFT needs power of two
		if (!isPowerOfTwo(sequenceLength)) {
			sequence = addZerosUntilPowerOfTwo(sequence);
		}
		
		surrogate = new double[sequence.length];	
		
		FastFourierTransformer transformer = new FastFourierTransformer(DftNormalization.STANDARD);
		Complex[] complx = transformer.transform(sequence, TransformType.FORWARD);  
		
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
        //Maybe decrease size of sequence to power of two
      	if (!isPowerOfTwo(sequenceLength)) {

      		double[] temp = new double[sequenceLength];
      		for (int i = 0; i < sequenceLength; i++) {
      			temp[i] = surrogate[i];
      		}
      		surrogate =  temp;
      	} 
		return surrogate;
	}

	/**
	 * This method calculates a surrogate data double array using the AAFT (amplitude adjusted FT) method
	 * A Gaussian sequence y is constructed
	 * y is ranked according to the original sequence
	 * then y is FFT converted, phase randomized and inverse FFT back converted yielding y'
	 * the original sequence is ranked according to y'
	 * @param sequence
	 * @param windowing type
	 * @return surrogate data 
	 */
	public double[] calcSurrogateAAFT(double[] sequence, String windowingType) {
		
		int sequenceLength = sequence.length;
		surrogate = new double[sequenceLength];
	
		//calculate rank of input sequence
		NaturalRanking ranking = new NaturalRanking(NaNStrategy.REMOVED, TiesStrategy.SEQUENTIAL);
        double[] rankOfSequence = ranking.rank(sequence);
	
     
        //Calculate Gaussian sequence
    	Random random = new Random();
		random.setSeed(System.currentTimeMillis());;
		
		double[] gauss = new double[sequenceLength];
		for (int i = 0; i < sequenceLength; i++){
			gauss[i] = random.nextGaussian();	
		}

		//Rank Gaussian sequence according to input sequence
		double[] gaussRank = new double[sequenceLength];
		for (int i = 0; i < sequenceLength; i++){
			gaussRank[i] = gauss[(int) rankOfSequence[i]-1];
		}
		
        //calculate phase randomized sequence of ranked Gaussian
		//this call fires also the progress bar events
        double[] gaussPhaseRandom = this.calcSurrogateRandomPhase(gaussRank, windowingType);
	
        //calculate rank of Gaussian (Ranked) phase randomized
		ranking = new NaturalRanking(NaNStrategy.REMOVED, TiesStrategy.SEQUENTIAL);
        double[] rankOfGaussPhaseRandom = ranking.rank(gaussPhaseRandom);
	
        //Rank input sequence according to Gaussian (Ranked) phase randomized
		for (int i = 0; i < sequenceLength; i++){
			surrogate[i] = sequence[(int)  rankOfGaussPhaseRandom[i]-1];
		}
		
		return surrogate;
	}
	/**
	 * This method calculates a surrogate data double array using the pseudo periodic method
	 * Not yet implemented
	 * @param sequence
	 * @return surrogate data 
	 */
	public double[] calcSurrogatePseudoPeriodic(double[] sequence) {	
		surrogate = new double[sequence.length];
			
		return surrogate;
	}
	/**
	 * This method calculates a surrogate data double array using the multivariate method
	 * Not yet implemented
	 * @param sequence
	 * @return surrogate data 
	 */
	public double[] calcSurrogateMultivariate(double[] sequence) {	
		surrogate = new double[sequence.length];
			
		return surrogate;
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
	 * This method does Rectangular windowing
	 * According to www.labbookpages.co.uk/audio/firWindowing.html#windows
	 * https://de.wikipedia.org/wiki/Fensterfunktion
	 * @param sequence
	 * @return windowed sequence
	 */
	private double[] windowingRectangular (double[] sequence) {
		double weight = 1.0;
	     for(int i = 0; i < sequence.length; ++i) {
	    	 sequence[i] = sequence[i] * weight;
	     }
	     return sequence; 
	}
	
	/**
	 * This method does Cosine windowing
	 * According to www.labbookpages.co.uk/audio/firWindowing.html#windows
	 * https://de.wikipedia.org/wiki/Fensterfunktion
	 * @param sequence
	 * @return windowed sequence
	 */
	private double[] windowingCosine (double[] sequence) {
		 double M = sequence.length - 1;
		 double weight = 0.0;
	     for(int n = 0; n < sequence.length; n++) {
	    	 weight = Math.sin(Math.PI*n/M);
	    	 sequence[n] = sequence[n] * weight;
	    	 //System.out.println("SequenceFFT Cosine weight " + weight);
	     }
	     return sequence; 
	}

	/**
	 * This method does  Lanczos windowing
	 * According to www.labbookpages.co.uk/audio/firWindowing.html#windows
	 * https://de.wikipedia.org/wiki/Fensterfunktion
	 * @param sequence
	 * @return windowed sequence
	 */
	private double[] windowingLanczos (double[] sequence) {
		 double M = sequence.length - 1;
		 double weight = 0.0;
		 double x = 0.0;
	     for(int n = 0; n < sequence.length; n++) {
	    	 x = Math.PI*(2.0*n/M-1);
	    	 if (x == 0) weight = 1.0;
	    	 else weight =  Math.sin(x)/x;
	    	 sequence[n] = sequence[n] * weight;
	    	 //System.out.println("SequenceFFT Lanczos weight  n " + n + "  "  + weight);
	     }
	     return sequence; 
	}

	/**
	 * This method does Bartlett windowing
	 * According to www.labbookpages.co.uk/audio/firWindowing.html#windows
	 * https://de.wikipedia.org/wiki/Fensterfunktion
	 * @param sequence
	 * @return windowed sequence
	 */
	private double[] windowingBartlett (double[] sequence) {
		 double M = sequence.length - 1;
		 double weight = 0.0;
	     for(int n = 0; n < sequence.length; n++) {
	    	 weight = 1.0-(2.0*Math.abs((double)n-M/2.0)/M);
	    	 sequence[n] = sequence[n] * weight;
	    	 //System.out.println("SequenceFFT Bartlett weight " + weight);
	     }
	     return sequence; 
	}

	/**
	 * This method does Hamming windowing
	 * According to www.labbookpages.co.uk/audio/firWindowing.html#windows
	 * https://de.wikipedia.org/wiki/Fensterfunktion
	 * @param sequence
	 * @return windowed sequence
	 */
	private double[] windowingHamming (double[] sequence) {
		 double M = sequence.length - 1;
		 double weight = 0.0;
	     for(int n = 0; n < sequence.length; n++) {
	    	 weight = 0.54 - 0.46 * Math.cos(2.0 * Math.PI * n / M);
	    	 sequence[n] = sequence[n] * weight;
	    	 //System.out.println("SequenceFFT Hamming weight " + weight);
	     }
	     return sequence; 
	}

	/**
	 * This method does Hanning windowing
	 * According to www.labbookpages.co.uk/audio/firWindowing.html#windows
	 * https://de.wikipedia.org/wiki/Fensterfunktion
	 * @param sequence
	 * @return windowed sequence
	 */
	private double[] windowingHanning (double[] sequence) {
		 double M = sequence.length - 1;
		 double weight = 0.0;
	     for(int n = 0; n < sequence.length; n++) {
	    	 weight = 0.5 - 0.5 * Math.cos(2.0 * Math.PI * n / M);
	    	 sequence[n] = sequence[n] * weight;
	    	 //System.out.println("SequenceFFT Hanning weight " + weight);
	     }
	     return sequence; 
	}
	
	/**
	 * This method does Blackman windowing
	 * According to www.labbookpages.co.uk/audio/firWindowing.html#windows
	 * https://de.wikipedia.org/wiki/Fensterfunktion
	 * @param sequence
	 * @return windowed sequence
	 */
	private double[] windowingBlackman (double[] sequence) {
		 double M = sequence.length - 1;
		 double weight = 0.0;
	     for(int n = 0; n < sequence.length; n++) {
	    	 weight = 0.42 - 0.5 * Math.cos(2.0 * Math.PI * n / M) + 0.008 * Math.cos(4.0 * Math.PI * n / M);
	    	 sequence[n] = sequence[n] * weight;
	    	 //System.out.println("SequenceFFT Blackman weight " + weight);
	     }
	     return sequence; 
	}
	
	/**
	 * This method does Gaussian windowing
	 * According to www.labbookpages.co.uk/audio/firWindowing.html#windows
	 * https://de.wikipedia.org/wiki/Fensterfunktion
	 * @param sequence
	 * @return windowed sequence
	 */
	private double[] windowingGaussian (double[] sequence) {
		 double M = sequence.length - 1;
		 double weight = 0.0;
		 double sigma = 0.3;
		 double exponent = 0.0;
	     for(int n = 0; n < sequence.length; n++) {
	    	 exponent = ((double)n-M/2)/(sigma*M/2.0);
	    	 exponent *= exponent;
	    	 weight = Math.exp(-0.5*exponent);
	    	 sequence[n] = sequence[n] * weight;
	    	 //System.out.println("SequenceFFT Gaussian weight " + weight);
	     }
	     return sequence; 
	}
	
	/**
	 * This method does Parzen windowing
	 * According to www.labbookpages.co.uk/audio/firWindowing.html#windows
	 * https://de.wikipedia.org/wiki/Fensterfunktion
	 * @param sequence
	 * @return windowed sequence
	 */
	private double[] windowingParzen (double[] sequence) {
		double M = sequence.length - 1;
		double nn;
		double weight = 0.0;
	    for(int n = 0; n < sequence.length; n++) {
	    	nn = Math.abs((double)n-M/2);
	    	if      ((nn >= 0.0) && (nn < M/4))  weight = 1.0 - 6.0*Math.pow(nn/(M/2), 2) * (1- nn/(M/2));
	    	else if ((nn >= M/4) && (nn <= M/2)) weight = 2.0*Math.pow(1-nn/(M/2), 3);
	    	sequence[n] = sequence[n] * weight;
	      	//System.out.println("SequenceFFT Parzen weight n " + n + "  "  + weight);
	     }
	     return sequence; 
	}
	

}
