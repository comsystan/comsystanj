/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complexity analyses of 1D signals, 2D images and 3D volumes
 * File: CsajAlgorithm_FisherInformation.java
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
 * This class calculates the Fisher information measure of a probability distribution  
 * Arouxet MaB, Bariviera AF, Hansen R, Pastor VE. A compact information-theoretic framework for texture classification: Hilbert curves, amplitude-aware permutation entropy, and explainability. Chaos. 3. August 2026;36(8):083102. doi:10.1063/5.0341167
 * Olivares F, Plastino A, Rosso OA. Contrasting chaos with noise via local versus global information quantifiers. Physics Letters A. 9. April 2012;376(19):1577–83. doi:10.1016/j.physleta.2012.03.039
 *
 * @author Helmut Ahammer
 * @since  2026 09
 */
public class CsajAlgorithm_FisherInformation {
	
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
	public CsajAlgorithm_FisherInformation(){
		
	}
	
	/**
	 * This is the standard constructor
	 */
	public CsajAlgorithm_FisherInformation(double[] probabilities){	
		setProbabilities(probabilities);
	}
	
	/**
	 * This method computes the Fisher information measure
	 * @return
	 */
	public double compFIM(boolean skipZeroBin) {
		double FIM = Double.NaN;
	
		double sum = 0.0f;
		int binStart = 0;
		if (skipZeroBin) binStart = 1;
		for (int pp = binStart; pp < probabilities.length - 1; pp++) {
			sum = sum + Math.pow(Math.sqrt(probabilities[pp + 1]) - Math.sqrt(probabilities[pp]), 2);
		}			
		FIM = sum;
		return FIM;
	}
	
	/**
	 * This method computes the the Fisher information measure
	 * @return
	 */
	public double compNormalisedFIM(boolean skipZeroBin) {
		
		double F0 = 0.5;
		if      (probabilities[0] == 1) F0 = 1.0;
		else if (probabilities[probabilities.length-1] == 1) F0 = 1.0;
		
		return F0 * compFIM(skipZeroBin);
	}
	
}
