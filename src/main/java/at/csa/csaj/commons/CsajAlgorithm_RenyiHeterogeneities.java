/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: CsajAlgorithm_RenyiHeterogeneities.java
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
 * This class calculates Renyi heterogeneities of a probability distribution  
 * According to
 * Nunes A, Alda M, Bardouille T, Trappenberg T. Representational Rényi Heterogeneity. Entropy. April 2020;22(4):417. 
 * Nunes A, Trappenberg T, Alda M. The definition and measurement of heterogeneity. Transl Psychiatry. 24. August 2020;10(1):299.
 * Nunes A, Trappenberg T, Alda M. Measuring heterogeneity in normative models as the effective number of deviation patterns. PLOS ONE. 13. November 2020;15(11):e0242320.
 * Nunes A, Trappenberg T, Alda M. We need an operational framework for heterogeneity in psychiatric research. J Psychiatry Neurosci. Januar 2020;45(1):3–6. 
 * 
 * 
 *
 * @author Helmut Ahammer
 * @since  2025 11
 */
public class CsajAlgorithm_RenyiHeterogeneities {
	
	private double[] probabilities;
	
	public double[] getProbabilities() {
		return probabilities;
	}

	public void setProbabilities(double[] probabilities) {
		this.probabilities = probabilities;
	}

	/**
	 * This is the standard constructor
	 */
	public CsajAlgorithm_RenyiHeterogeneities(){
		
	}
	
	/**
	 * This is the standard constructor
	 */
	public CsajAlgorithm_RenyiHeterogeneities(double[] probabilities){	
		setProbabilities(probabilities);
	}
	
	/**
	 * This method computes Renyi heterogeneities
	 * According to Nunes etal. paper
	 * @param minQ
	 * @param maxQ
	 * @param numQ
	 * @return
	 */
	public double[] compRenyiHets(int minQ, int maxQ, int numQ, boolean skipZeroBin) {
		double[] hetRenyi   = new double[numQ];
		for (int q = 0; q < numQ; q++) {
			double sum = 0.0f;
			int binStart = 0;
			if (skipZeroBin) binStart = 1;
			for (int pp = binStart; pp < probabilities.length; pp++) {
				if ((q + minQ) == 1) { //q=1 special case
					if (probabilities[pp] == 0) {// damit logarithmus nicht undefiniert ist;
						sum = sum +  Double.MIN_VALUE*Math.log( Double.MIN_VALUE); //for q=1 Renyi het is equal to exponential of Shannon entropy
					}
					else {
						sum = sum + probabilities[pp]*Math.log(probabilities[pp]); //for q=1 Renyi het is equal to exponential of Shannon entropy
					}
				}
				else if (((q + minQ) <=  0 ) && (probabilities[pp] != 0.0)){ //leaving out 0
					sum = sum + Math.pow(probabilities[pp],(q + minQ));	
				}
				else if ( (q + minQ) > 0 ) {
					sum = sum + Math.pow(probabilities[pp],(q + minQ));
				}
			}			
			if ((q + minQ) == 1) { //special case q=1 Renyi het is equal to exponential of Shannon entropy
				hetRenyi[q] = Math.exp(-sum); 
			}	
			else {
				hetRenyi[q] = Math.pow(sum, 1.0/(1.0-(q + minQ)));	
			}
		}//q
		return hetRenyi;
	}
	
	
	
}
