/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: CsajAlgorithm_ProbabilityDistance.java
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
package at.csa.csaj.commons;


/**
 * Kowalski AM, Martín MT, Plastino A, Rosso OA, Casas M.
 * Distances in Probability Space and the Statistical Complexity Setup.
 * Entropy. Juni 2011;13(6):1055–75. 
 * 
 * @author Helmut Ahammer
 * @since  2025 07
 */
public class CsajAlgorithm_ProbabilityDistance {
	
	private double[] probabilities1;
	private double[] probabilities2;
	
	public double[] getProbabilities1() {
		return probabilities1;
	}
	
	public double[] getProbabilities2() {
		return probabilities2;
	}

	public void setProbabilities1(double[] probabilities1) {
		this.probabilities1 = probabilities1;
	}
	public void setProbabilities2(double[] probabilities2) {
		this.probabilities2 = probabilities2;
	}
	
	public void setUniformProbabilities2(int length) {
		//set to uniform distribution
		this.probabilities2 = new double[length];
		for (int j = 0; j<length; j++) probabilities2[j] = 1.0/((double)length);
	}

	/**
	 * This is the standard constructor
	 */
	public CsajAlgorithm_ProbabilityDistance(){
		
	}
	
	/**
	 * This is the standard constructor
	 */
	public CsajAlgorithm_ProbabilityDistance(double[] probabilities1){	
		//It is assumed that the second distribution is uniform
		setProbabilities1(probabilities1);
		setUniformProbabilities2(probabilities1.length);
	}
	
	/**
	 * This is the standard constructor
	 */
	public CsajAlgorithm_ProbabilityDistance(double[] probabilities1, double[] probabilities2){	
		setProbabilities1(probabilities1);
		setProbabilities2(probabilities2);
	}
	
	/**
	 * This method computes the distance between two probabilities
	 * Euclidean norm
	 * 
	 * Used for LMC complexity in:
	 * Lopez-Ruiz, L.; Mancini, H.; Calbet, X. A statistical measure of complexity. Phys. Lett. A 1995,  209, 321–326.
	 * 
	 * @return
	 */
	public double compD_E(boolean skipZeroBin) {
		double d = 0.0;
	
		int binStart = 0;
		if (skipZeroBin) binStart = 1;
		for (int i = binStart; i < probabilities1.length; i++) {
		
			d = d + ((probabilities1[i] - probabilities2[i]) * (probabilities1[i] - probabilities2[i]));		
		}			
		
		return d;
	}
	
	/**
	 * This method computes the normalized distance between two probabilities
	 */
	public double compNormalisedD_E(boolean skipZeroBin) {

		return (double)probabilities1.length/((double)probabilities1.length - 1.0) * compD_E(skipZeroBin);
	}
	
	/**
	 * This method computes the distance between two probabilities
	 * Wootter's distance 
	 * W. K. Wootters, Physical Review D 23, 357 (1981). https://doi.org/10.1103/PhysRevD.23.357
	 * 
	 * @return
	 */
	public double compD_W(boolean skipZeroBin) {
		double d = 0.0;
		int length = probabilities1.length;
	
		int binStart = 0;
		if (skipZeroBin) binStart = 1;
		for (int i = binStart; i < length; i++) {
		
			//all of the following 3 versions give the same result
			//d = d + (Math.pow(probabilities1[i], 0.5) * Math.pow(probabilities2[i], 0.5));
			//d = d + (Math.pow(probabilities1[i]*probabilities2[i], 0.5));
			d = d + (Math.sqrt(probabilities1[i]*probabilities2[i]));	
			
		}			
		
		return Math.acos(d);
	}
	
	/**
	 * This method computes the normalized distance between two probabilities
	 */
	public double compNormalisedD_W(boolean skipZeroBin) {
		
		int length = probabilities1.length;
		
		return compD_W(skipZeroBin)/Math.acos(Math.sqrt(1.0/length));
	}
	
	/**
	 * This method computes the distance between two probabilities
	 * Kullbak-Leibler relative entropy (Kullbak-Leibler divergence)
	 * Kullback, S.; Leibler, R.A. On Information and Sufficiency”. Ann. Math. Stat. 1951, 22, 79–86.
	 * 
	 * @return
	 */
	public double compD_K(boolean skipZeroBin) {
		double d = 0.0;
	
		int binStart = 0;
		if (skipZeroBin) binStart = 1;
		for (int i = binStart; i < probabilities1.length; i++) {
			if (probabilities1[i] != 0 && probabilities2[i] != 0) {
				d = d + (probabilities1[i] * Math.log(probabilities1[i]/probabilities2[i]));
			}
		}			
		
		return d;
	}
	
	/**
	 * This method computes the normalized distance between two probabilities
	 * Bonnici, V. (2020). Kullback-Leibler divergence between quantum distributions, and its upper-bound.
	 * https://arxiv.org/abs/2008.05932
	 * 
	 */
	public double compNormalisedD_K(boolean skipZeroBin) {
		
//		//Compute maximum
//		//set one element of probabilities1 to 1, all other to 0 
//		//set probabilities2 to uniform	
//		double dMax = 0.0;
//		int length = probabilities1.length;
//		double[] prob1 = new double[length];
//		double[] prob2 = new double[length];
//		
//		                                 prob1[0] = 1.0;
//		for (int i = 1; i < length; i++) prob1[i] = 0.0;
//		for (int i = 0; i < length; i++) prob2[i] = 1.0/(double)length;
//		
//		int binStart = 0;
//		if (skipZeroBin) binStart = 1;
//		for (int i = binStart; i < length; i++) {
//			if (prob1[i] != 0 && prob2[i] != 0) {
//				dMax = dMax + (prob1[i] * Math.log(prob1[i]/prob2[i]));
//			}
//		}	
//		//Result will be always ln(length)!!!!!!!!!!!!!
		
		double dMax = Math.log(probabilities1.length);
		
		//System.out.println("dMax: " + dMax);
		return compD_K(skipZeroBin)/dMax;
	}
	
	/**
	 * This method computes the distance between two probabilities
	 * Jensen (Shannon) divergence
	 * P. W. Lamberti, M. Martin, A. Plastino, and O. Rosso, Physica A: Statistical Mechanics and its Applications 334, 119 (2004) https://doi.org/10.1016/j.physa.2005.11.053
	 * A. Majtey, P.W. Lamberti, M.T. Martin, A. Plastino, Eur. Phys. J. D 32 (2005) 413  https://doi.org/10.1140/epjd/e2005-00005-1
	 * https://en.wikipedia.org/wiki/Jensen%E2%80%93Shannon_divergence
	 * "Its value is 0 if equal distributions are compared, but it is shown to not have an upper-bound to its possible value."
	 * 
	 * Chen, M., & Sbert, M. (2019).
	 * On the upper bound of the kullback-leibler divergence and cross entropy.
	 * https://arxiv.org/abs/1911.08334
	 * 
	 * @return
	 */
	public double compD_J(boolean skipZeroBin) {
		double d = 0.0;
		double h12 = 0.0;
		double h1 = 0.0;
		double h2 = 0.0;
		int N = probabilities1.length;
		double[] probabilities = new double[N];
		
		int binStart = 0;
		if (skipZeroBin) binStart = 1;
		for (int i = binStart; i<N; i++) probabilities[i] = (probabilities1[i] + probabilities2[i])/2.0;	
		CsajAlgorithm_ShannonEntropy se = new CsajAlgorithm_ShannonEntropy(probabilities);
		h12 = se.compH(skipZeroBin);
		
		se = new CsajAlgorithm_ShannonEntropy(probabilities1);
		h1 = se.compH(skipZeroBin);
		
		se = new CsajAlgorithm_ShannonEntropy(probabilities2);
		h2 = se.compH(skipZeroBin);
	
		return h12 - h1/2.0 - h2/2.0;
	}
	
	/**
	 * This method computes the normalized distance between two probabilities
	 * Bounds of DJ
	 * https://en.wikipedia.org/wiki/Jensen%E2%80%93Shannon_divergence 
	 * The Jensen–Shannon divergence is bounded by 1 for two discrete probability distributions with base 2 logarithm
	 * With base-e logarithm, which is commonly used in statistical thermodynamics, the upper bound is ln(2)
	 * 
	 */
	public double compNormalisedD_J(boolean skipZeroBin) {
	
		return compD_J(skipZeroBin)/Math.log(2.0);
	}
	
	
	
	
	
}
