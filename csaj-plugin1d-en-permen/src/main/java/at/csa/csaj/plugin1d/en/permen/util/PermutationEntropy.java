/*-
 * #%L
 * Project: ImageJ2 signal plugin for computing the Permutation entropy
 * File: PermutationEntropy.java
 * 
 * $Id$
 * $HeadURL$
 * 
 * This file is part of ComsystanJ software, hereinafter referred to as "this program".
 * %%
 * Copyright (C) 2021 - 2023 Comsystan Software
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

package at.csa.csaj.plugin1d.en.permen.util;

import org.apache.commons.math3.stat.ranking.NaNStrategy;
import org.apache.commons.math3.stat.ranking.NaturalRanking;
import org.apache.commons.math3.stat.ranking.TiesStrategy;
import org.scijava.log.LogService;

/**
 * 
 * Bandt C and Pompe B. Permutation Entropy: A Natural Complexity Measure for Time Series. Phys Rev Lett Vol88(17) 2002
 * <p>
 * <b>Changes</b>
 * <ul>
 * 	<li>
 * </ul>
 * 
 * @author Helmut Ahammer
 * @since  2021-05-25
 */
public class PermutationEntropy {
	
	private LogService logService;

	private int numbDataPoints = 0; // length of 1D data series

	/**
	 * This is the standard constructor with the possibility to import calling
	 * class This is useful for progress bar and canceling functionality
	 * 
	 * @param operator
	 * 
	 */
	public PermutationEntropy(LogService logService) {
		this.logService = logService;
	}


	/**
	 * This is the standard constructor
	 */
	public PermutationEntropy() {
	}

	/**
	 * This method calculates the mean of a data series.
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
	 * This method calculates the variance of a data series.
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
	 * This method calculates the permutation entropy
	 * @param data1D 1D data vector
	 * @param n order of permutation entropy;  
	 *        n should not be greater than N/3 (N number of data points)!
	 * @param d delay
	 * @return permutationEntropy  (sole double value)
	 * 
	 */
	public double calcPermutationEntropy(double[] data1D, int n, int d){
		numbDataPoints = data1D.length;
		if (n > numbDataPoints / 3) {
			n = numbDataPoints / 3;
			logService.info(this.getClass().getName() + " Parameter m too large, automatically set to data length/3");
		}
		if (n < 1) {
			logService.info(this.getClass().getName() + " Parameter m too small, Permutation entropy cannot be calulated");
			return 99999999d;
		}
		if (d < 0) {
			logService.info(this.getClass().getName() + " Delay too small, Permutation entropy cannot be calulated");
			return 999999999d;
		}
	
		//generate permutation patterns
		char[] buffer = new char[n];
		for (int i = 0; i < buffer.length; i++){
			buffer[i] = (char)(i+1);
		}	  
		int[][] permArray = Permutation.allPermutations(n);
			
		double[] counts = new double[permArray.length];
		
//		long  factorial = ArithmeticUtils.factorial(m);
		//System.out.println("PEntropy: permArray.length: "+ permArray.length+ "     factorial: " + factorial);
		
		
		double permutationEntropy = 0d;
		double[] sample;
		NaturalRanking ranking;
        double[] rankOfSample;
        double diff;
		for(int j = 0; j < data1D.length-d*(n-1); j++){ 			
		
			sample = new double[n];
			for (int s = 0; s <  n; s=s+d){
				sample[s] = data1D[j+s];
			}
			ranking = new NaturalRanking(NaNStrategy.REMOVED, TiesStrategy.SEQUENTIAL);
	        rankOfSample = ranking.rank(sample);
	   
       	
	        //look for matches
	        for(int i = 0; i < permArray.length; i++){
	        	diff = 0.0;
	        	for (int s =0; s < n; s++){
	        		diff = diff + Math.abs(permArray[i][s]- rankOfSample[s]);
	        	}    		
	        	if (diff == 0){
	        		counts[i] = counts[i] + 1 ;
	        	}
	        }
		}
		
		double sum = 0.0d;
		for(int i = 0; i <counts.length; i++){
			sum = sum + counts[i];
		}
		for(int i = 0; i <counts.length; i++){
			counts[i] = counts[i] / sum;
		}
		sum = 0.0d;
		for(int i = 0; i <counts.length; i++){
			if (counts[i] != 0){
				sum = sum + counts[i] * Math.log(counts[i]);
			}
		}
		permutationEntropy = -sum;
		return permutationEntropy;
	}

}
