/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: CsajAlgorithm_ShannonEntropy.java
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
 * This class calculates Generalised entropies of a probability distribution  
 * <li>according to a review of Amigó, J.M., Balogh, S.G., Hernández, S., 2018. A Brief Review of Generalised Entropies. Entropy 20, 813. https://doi.org/10.3390/e20110813
 * <li>and to: Tsallis Introduction to Nonextensive Statistical Mechanics, 2009, S105-106
 * <li>(SE     according to Amigo etal. and Tsekouras, G.A.; Tsallis, C. Generalised entropy arising from a distribution of q indices. Phys. Rev. E 2005,)
 * <li>SE      according to N. R. Pal and S. K. Pal: Object background segmentation using new definitions of entropy, IEEE Proc. 366 (1989), 284–295.
							and N. R. Pal and S. K. Pal, Entropy: a new definitions and its applications, IEEE Transactions on systems, Man and Cybernetics, 21(5), 1260-1270, 1999
 * <li>H       according to Amigo etal.
 * <li>Renyi   according to Amigo etal.
 * <li>Tsallis according to Amigo etal.
 * <li>SNorm   according to Tsallis Introduction to Nonextensive Statistical Mechanics, 2009, S105-106
 * <li>SEscort according to Tsallis Introduction to Nonextensive Statistical Mechanics, 2009, S105-106
 * <li>SEta    according to Amigo etal. and Anteneodo, C.; Plastino, A.R. Maximum entropy approach to stretched exponential probability distributions. J. Phys. A Math. Gen. 1999, 32, 1089–1098.	
 * <li>SKappa  according to Amigo etal. and Kaniadakis, G. Statistical mechanics in the context of special relativity. Phys. Rev. E 2002, 66, 056125
 * <li>SB      according to Amigo etal. and Curado, E.M.; Nobre, F.D. On the stability of analytic entropic forms. Physica A 2004, 335, 94–106.
 * <li>SBeta   according to Amigo etal. and Shafee, F. Lambert function and a new non-extensive form of entropy. IMA J. Appl. Math. 2007, 72, 785–800.
 * <li>SGamma  according to Amigo etal. and Tsallis Introduction to Nonextensive Statistical Mechanics, 2009, S61
 * 
 *
 * @author Helmut Ahammer
 * @since  2025 07
 */
public class CsajAlgorithm_ShannonEntropy {
	
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
	public CsajAlgorithm_ShannonEntropy(){
		
	}
	
	/**
	 * This is the standard constructor
	 */
	public CsajAlgorithm_ShannonEntropy(double[] probabilities){	
		setProbabilities(probabilities);
	}
	
	/**
	 * This method computes the Shannon entropy
	 * @return
	 */
	public double compH(boolean skipZeroBin) {
		double H = Double.NaN;
	
		double sum = 0.0f;
		int binStart = 0;
		if (skipZeroBin) binStart = 1;
		for (int pp = binStart; pp < probabilities.length; pp++) {
			
				if (probabilities[pp] == 0) {// damit logarithmus nicht undefiniert ist;
					sum = sum +  Double.MIN_VALUE*Math.log( Double.MIN_VALUE);
				}
				else {
					sum = sum + probabilities[pp]*Math.log(probabilities[pp]);
				}
		}			
		H = -sum;
		return H;
	}
	
	/**
	 * This method computes the Shannon entropy
	 * @return
	 */
	public double compNormalisedH(boolean skipZeroBin) {

		return compH(skipZeroBin)/(Math.log(probabilities.length));
	}
	
}
