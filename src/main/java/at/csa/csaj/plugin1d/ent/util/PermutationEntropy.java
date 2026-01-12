/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: PermutationEntropy.java
 * 
 * $Id$
 * $HeadURL$
 * 
 * This file is part of ComsystanJ software, hereinafter referred to as "this program".
 * %%
 * Copyright (C) 2024 - 2026 Comsystan Software
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

import org.apache.commons.math3.stat.ranking.NaNStrategy;
import org.apache.commons.math3.stat.ranking.NaturalRanking;
import org.apache.commons.math3.stat.ranking.TiesStrategy;
import org.scijava.log.LogService;

/**
 * PE according to original paper:
 * Bandt C and Pompe B. Permutation Entropy: A Natural Complexity Measure for Time Series. Phys Rev Lett Vol88(17) 2002
 * 
 * Weighted PE according to:
 * Fadlallah B, Chen B, Keil A, Príncipe J. Weighted-permutation entropy: A complexity measure for time series incorporating amplitude information. Phys Rev E. 20. Februar 2013;87(2):022911. 
 * 
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
	 * @return double[] {permutationEntropy, weightedPermutationEntropy}
	 * 
	 */
	public double[] calcPermutationEntropy(double[] data1D, int n, int d){
		numbDataPoints = data1D.length;
		if (n > numbDataPoints / 3) {
			n = numbDataPoints / 3;
			logService.info(this.getClass().getName() + " Parameter m too large, automatically set to data length/3");
		}
		if (n < 1) {
			logService.info(this.getClass().getName() + " Parameter m too small, Permutation entropy cannot be calulated");
			return new double[] {99999999d, 99999999d};
		}
		if (d < 0) {
			logService.info(this.getClass().getName() + " Delay too small, Permutation entropy cannot be calulated");
			return new double[] {999999999d, 999999999d};
		}
	
		//generate permutation patterns
		char[] buffer = new char[n];
		for (int i = 0; i < buffer.length; i++){
			buffer[i] = (char)(i+1);
		}	  
		int[][] permArray = Permutation.allPermutations(n);
			
		double[] counts         = new double[permArray.length];
		double[] countsWeighted = new double[permArray.length];
		double[] sumWeighted    = new double[permArray.length];
//		long  factorial = ArithmeticUtils.factorial(m);
		//System.out.println("PEntropy: permArray.length: "+ permArray.length+ "     factorial: " + factorial);
		
		
		double permutationEntropy = 0d;
		double weightedPermutationEntropy = 0d;
		double[] sample;
		NaturalRanking ranking;
        double[] rankOfSample;
        double diff;
        double mean;       //for weighted PE
        double weight = 0; //for weighted PE 
 
		for(int j = 0; j < data1D.length-d*(n-1); j++){ 			
		
			sample = new double[n];
			for (int s = 0; s < n; s=s+1){ //s=s+d?
				sample[s] = data1D[j+s*d];
			}
			ranking = new NaturalRanking(NaNStrategy.REMOVED, TiesStrategy.SEQUENTIAL);
	        rankOfSample = ranking.rank(sample);
	       
	        //look for matches
	        for(int i = 0; i < permArray.length; i++){
	        	
	        	mean = this.calcMean(sample);
	        	weight = 0.0;
        		for (int ii = 0; ii < sample.length; ii++) {
        			weight = weight + Math.pow((sample[ii] - mean), 2);
        		}
        		weight = weight/sample.length; //sample.length == n
        		sumWeighted[i] = sumWeighted[i] + weight;
        		
        		diff = 0.0;
	        	for (int s = 0; s < n; s++){
	        		diff = diff + Math.abs(permArray[i][s]- rankOfSample[s]);
	        	}    
	        	if (diff == 0){ //pattern match
	        		counts[i] = counts[i] + 1; //for usual PE	
	        		countsWeighted[i] = countsWeighted[i] + weight; //for weighted PE	
	        	}   
	        }
		}
		
//		double sum         = 0.0d;
//		for(int i = 0; i <counts.length; i++){
//			sum = sum + counts[i];
//		}
		for(int i = 0; i <counts.length; i++){
			//counts[i] = counts[i] / sum; //should be identical, used until v1.2.1 
			counts[i] = counts[i] / (numbDataPoints - (n-1)*d); //is the same but shorter
			countsWeighted[i] = countsWeighted[i] / sumWeighted[i];
		}
		
		//Last equation
		double entropySum = 0.0d;
		for(int i = 0; i <counts.length; i++){
			if (counts[i] != 0){
				entropySum = entropySum + counts[i] * Math.log(counts[i]);
			}
		}
		double entropySumWeighted = 0.0d;
		for(int i = 0; i <countsWeighted.length; i++){
			if (countsWeighted[i] != 0){
				entropySumWeighted = entropySumWeighted + countsWeighted[i] * Math.log(countsWeighted[i]);
			}
		}
		permutationEntropy         = -entropySum;
		weightedPermutationEntropy = -entropySumWeighted;
		return new double[]{permutationEntropy, weightedPermutationEntropy}; //PE, weightedPE
	}

}
