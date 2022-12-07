/*-
 * #%L
 * Project: Commons for ComsystanJ ImageJ Plugins
 * File: GeneralizedEntropies.java
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
package at.csa.csaj.commons.algorithms;

import org.apache.commons.math3.special.Gamma;

/**
 * This class calculates Generalized entropies of a probability distribution  
 * <li>according to a review of Amigó, J.M., Balogh, S.G., Hernández, S., 2018. A Brief Review of Generalized Entropies. Entropy 20, 813. https://doi.org/10.3390/e20110813
 * <li>and to: Tsallis Introduction to Nonextensive Statistical Mechanics, 2009, S105-106
 * <li>(SE     according to Amigo etal. and Tsekouras, G.A.; Tsallis, C. Generalized entropy arising from a distribution of q indices. Phys. Rev. E 2005,)
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
 * @since  2022 12
 */
public class GeneralizedEntropies {
	
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
	public GeneralizedEntropies(){
		
	}
	
	/**
	 * This is the standard constructor
	 */
	public GeneralizedEntropies(double[] probabilities){	
		setProbabilities(probabilities);
	}
	
	
	//
	/**
	 * This method computes the SE entropy
	 * @return
	 */
	public double compSE() {
		double sum = 0.0f;
		for (int pp = 0; pp < probabilities.length; pp++) {
			//if (probabilities[pp] != 0) {
				//sum = sum +  probabilities[pp] * (1.0 - Math.exp((probabilities[pp] - 1.0) / probabilities[pp]) ); //almost always exact  1!?	//According to and Tsekouras & Tsallis, and Tsallis book
				sum = sum + probabilities[pp] * (Math.exp(1.0 - probabilities[pp]) - 1.0); //around 1.7 // N. R. Pal and S. K. Pal: Object background segmentation using new definitions of entropy, IEEE Proc. 366 (1989), 284–295.
															// N. R. Pal and S. K. Pal, Entropy: a new definitions and its applications, IEEE Transactions on systems, Man and Cybernetics, 21(5), 1260-1270, 1999
				//sum = sum +  probabilities[pp] * Math.exp(1.0 - probabilities[pp]); //always around 2.7 // Hassan Badry Mohamed El-Owny, Exponential Entropy Approach for Image Edge Detection, International Journal of Theoretical and Applied Mathematics 2016; 2(2): 150-155 http://www.sciencepublishinggroup.com/j/ijtam doi: 10.11648/j.ijtam.20160202.29 Hassan Badry Mohamed El-Owny1, 
			//}
		}
		return sum;
	}
	
	/**
	 * This method computes H1, H2 and H3 entropies
	 * According to Amigo etal. paper
	 * @return
	 */
	public double[] compH() {
		
		double genEntH1 = 0;
		double genEntH2 = 0;
		double genEntH3 = 0;	
		double pHochp;
		
		for (int pp = 0; pp < probabilities.length; pp++) {
			if (probabilities[pp] != 0) {
					pHochp = Math.pow(probabilities[pp], probabilities[pp]);
					genEntH1 = genEntH1 + (1.0 - pHochp);
					genEntH2 = genEntH2 + Math.log(2.0-pHochp);
					genEntH3 = genEntH3 + (probabilities[pp] + Math.log(2.0-pHochp));	
			}
		}
		genEntH2 = Math.exp(genEntH2);
		
		return new double []{genEntH1, genEntH2, genEntH3};
	}
	
	/**
	 * This method computes generalisied Renyi entropies
	 * According to Amigo etal. paper
	 * @param minQ
	 * @param maxQ
	 * @param numQ
	 * @return
	 */
	public double[] compRenyi(int minQ, int maxQ, int numQ) {
		double[] genEntRenyi   = new double[numQ];
		for (int q = 0; q < numQ; q++) {
			double sum = 0.0f;
			for (int pp = 0; pp < probabilities.length; pp++) {
				if ((q + minQ) == 1) { //q=1 special case
					if (probabilities[pp] == 0) {// damit logarithmus nicht undefiniert ist;
						sum = sum +  Double.MIN_VALUE*Math.log( Double.MIN_VALUE); //for q=1 Renyi is equal to S_BGS (Bolzmann Gibbs Shannon entropy)
					}
					else {
						sum = sum + probabilities[pp]*Math.log(probabilities[pp]); //for q=1 Renyi is equal to S_BGS (Bolzmann Gibbs Shannon entropy)
					}
				}
				else if (((q + minQ) <=  0 ) && (probabilities[pp] != 0.0)){ //leaving out 0 is essential! and according to Amigo etal. page 2
					sum = sum + Math.pow(probabilities[pp],(q + minQ));	
				}
				else if ( (q + minQ) > 0 ) {
					sum = sum + Math.pow(probabilities[pp],(q + minQ));
				}
			}			
			if ((q + minQ) == 1) { //special case q=1 Renyi is equal to S_BGS (Bolzmann Gibbs Shannon entropy)
				genEntRenyi[q] = -sum; 
			}	
			else {
				if (sum == 0) sum = Float.MIN_VALUE; // damit logarithmus nicht undefiniert ist
				genEntRenyi[q] = Math.log(sum)/(1.0-(q + minQ));	
			}
		}//q
		return genEntRenyi;
	}
	
	/**
	 * This method computes generalized Tsallis entropies
	 * According to Amigo etal. paper
	 * @param minQ
	 * @param maxQ
	 * @param numQ
	 * @return
	 */
	public double[] compTsallis(int minQ, int maxQ, int numQ) {
		double[] genEntTsallis   = new double[numQ];
		for (int q = 0; q < numQ; q++) {
			double sum = 0.0f;
			for (int pp = 0; pp < probabilities.length; pp++) {
				if ((q + minQ) == 1) { //q=1 special case
					if (probabilities[pp] == 0) {// damit logarithmus nicht undefiniert ist;
						sum = sum +  Double.MIN_VALUE*Math.log( Double.MIN_VALUE); //for q=1 Renyi is equal to S_BGS (Bolzmann Gibbs Shannon entropy)
					}
					else {
						sum = sum + probabilities[pp]*Math.log(probabilities[pp]); //for q=1 Renyi is equal to S_BGS (Bolzmann Gibbs Shannon entropy)
					}
				}
				else if (((q + minQ) <=  0 ) && (probabilities[pp] != 0.0)) { //leaving out 0 is essential! and according to Amigo etal. page 2
						sum = sum + Math.pow(probabilities[pp],(q + minQ));	
				}
				else if ( (q + minQ) > 0 ) {
						sum = sum + Math.pow(probabilities[pp],(q + minQ));	
				}				
			}				
			if ((q + minQ) == 1) { // special case for q=1 Tsallis is equal to S_BGS (Bolzmann Gibbs Shannon entropy)
				genEntTsallis[q] = -sum;
			}
			else {
				genEntTsallis[q] = (sum-1)/(1.0-(q + minQ));
			}		
		}//q						
		return genEntTsallis;
	}
	
	/**
	 * This method computes the generalized entropy SNorm
	 * According to Amigo etal. paper
	 * @param minQ
	 * @param maxQ
	 * @param numQ
	 * @return
	 */
	public double[] compSNorm(int minQ, int maxQ, int numQ) {
		double[] genEntSNorm   = new double[numQ];
		for (int q = 0; q < numQ; q++) {
			double sum = 0.0f;
			for (int pp = 0; pp < probabilities.length; pp++) {
				if ((q + minQ) == 1) { //q=1 special case
					if (probabilities[pp] == 0) {// damit logarithmus nicht undefiniert ist;
						sum = sum +  Double.MIN_VALUE*Math.log( Double.MIN_VALUE); //for q=1 Snorm is equal to S_BGS (Bolzmann Gibbs Shannon entropy)
					}
					else {
						sum = sum + probabilities[pp]*Math.log(probabilities[pp]); //for q=1 Snorm is equal to S_BGS (Bolzmann Gibbs Shannon entropy)
					}
				}
				else if (((q + minQ) <=  0 ) && (probabilities[pp] != 0.0)){ //leaving out 0 is essential! and according to Amigo etal. page 2
					sum = sum + Math.pow(probabilities[pp],(q + minQ));	
				}
				else if ( (q + minQ) > 0 ) {
					sum = sum + Math.pow(probabilities[pp],(q + minQ));
				}
//				else {
//					sum = sum + Math.pow(probabilities[pp],(q + minQ));
//				}
			}			
			if ((q + minQ) == 1) { //special case q=1 SNorm is equal to S_BGS (Bolzmann Gibbs Shannon entropy)
				genEntSNorm[q] = -sum; 
			}	
			else {
				genEntSNorm[q] = (1.0-(1.0/sum))/(1.0-(q + minQ));	
			}
		}//q		
		return genEntSNorm;
	}
	
	/**
	 * This method computes the generalized Escort entropies
	 * According to Tsallis Introduction to Nonextensive Statistical Mechanics, 2009, S105-106
	 * @param minQ
	 * @param maxQ
	 * @param numQ
	 * @return
	 */
	public double[] compSEscort(int minQ, int maxQ, int numQ) {
		double[] genEntSEscort = new double[numQ];
		for (int q = 0; q < numQ; q++) {
			double sum = 0.0f;
			for (int pp = 0; pp < probabilities.length; pp++) {
				if ((q + minQ) == 1) { //q=1 special case
					if (probabilities[pp] == 0) {// damit logarithmus nicht undefiniert ist;
						sum = sum +  Double.MIN_VALUE*Math.log( Double.MIN_VALUE); //for q=1 SEscort is equal to S_BGS (Bolzmann Gibbs Shannon entropy)
					}
					else {
						sum = sum + probabilities[pp]*Math.log(probabilities[pp]); //for q=1 SEscort is equal to S_BGS (Bolzmann Gibbs Shannon entropy)
					}
				}
				else if (((q + minQ) <=  0 ) && (probabilities[pp] != 0.0)){ //leaving out 0 is essential! and according to Amigo etal. page 2
					sum = sum + Math.pow(probabilities[pp], 1.0/(q + minQ));	
				}
				else if ( (q + minQ) > 0 ) {
					sum = sum + Math.pow(probabilities[pp], 1.0/(q + minQ));
				}
//				else {
//					sum = sum + Math.pow(probabilities[pp], 1.0/(q + minQ));
//				}
			}			
			if ((q + minQ) == 1) { //special case q=1 SEscort is equal to S_BGS (Bolzmann Gibbs Shannon entropy)
				genEntSEscort[q] = -sum; 
			}	
			else {
				genEntSEscort[q] = (1.0 - Math.pow(sum, -(q+minQ)))/((q + minQ) - 1.0);	
			}
		}//q		
		return genEntSEscort;
	}
	
	/**
	 * This method computes the gneralized SEta entropies
	 * According to Amigo etal. and Anteneodo, C.; Plastino, A.R. Maximum entropy approach to stretched exponential probability distributions. J. Phys. A Math. Gen. 1999, 32, 1089–1098.
	 * @param minEta
	 * @param maxEta
	 * @param stepEta
	 * @param numEta
	 * @return
	 */
	public double[] compSEta(float minEta, float maxEta, float stepEta, int numEta) {
		double[] genEntSEta = new double[numEta];
		for (int n = 0; n < numEta; n++) {
			double eta = minEta + n*stepEta; //SEta is equal to S_BGS (Bolzmann Gibbs Shannon entropy) for eta = 1 
			double sum = 0.0f;
			double gam1;
			double gam2;
			for (int pp = 0; pp < probabilities.length; pp++) {
				if (probabilities[pp] != 0){

					//compute incomplete Gamma function using Apache's classes
					//gam1 = Gamma.regularizedGammaQ((eta+1.0)/eta, -Math.log(probabilities[pp])) * Math.exp(Gamma.logGamma((eta+1.0)/eta));
					//gam2 = probabilities[pp]*Math.exp(Gamma.logGamma((eta+1.0f)/eta));
					gam1 = Gamma.regularizedGammaQ((eta+1.0)/eta, -Math.log(probabilities[pp])) * Gamma.gamma((eta+1.0)/eta);
					gam2 = probabilities[pp]*Gamma.gamma((eta+1.0f)/eta); 
					sum = sum + gam1 - gam2;	
				}
			}	
			genEntSEta[n] = sum;
		}//q		
		return genEntSEta;
	}
	
	/**
	 * This method computes generalized SKappa entropies
	 * According to Amigo etal. and Kaniadakis, G. Statistical mechanics in the context of special relativity. Phys. Rev. E 2002, 66, 056125
	 * @param minKappa
	 * @param maxKappa
	 * @param stepKappa
	 * @param numKappa
	 * @return
	 */
	public double[] compSKappa(float minKappa, float maxKappa, float stepKappa, int numKappa) {
		double[] genEntSKappa = new double[numKappa];
		for (int k = 0; k < numKappa; k++) {
			double kappa = minKappa + k*stepKappa; //SKappa is equal to S_BGS (Bolzmann Gibbs Shannon entropy) for kappa = 0 
			double sum = 0.0f;
			for (int pp = 0; pp < probabilities.length; pp++) {
				if (kappa == 0) { //kappa=0 special case S_BGS (Bolzmann Gibbs Shannon entropy)
					if (probabilities[pp] == 0) {// damit logarithmus nicht undefiniert ist;
						sum = sum +  Double.MIN_VALUE*Math.log( Double.MIN_VALUE); //for k = 0 SKappa is equal to S_BGS (Bolzmann Gibbs Shannon entropy)
					}
					else {
						sum = sum + probabilities[pp]*Math.log(probabilities[pp]); //for k=0 SKappa is equal to S_BGS (Bolzmann Gibbs Shannon entropy)
					}
				}
				else {
					//if (probabilities[pp] != 0){			
						sum = sum + (Math.pow(probabilities[pp], 1.0-kappa) - Math.pow(probabilities[pp], 1.0+kappa))/(2.0*kappa);			
					//}
				}
			}
			if (kappa == 0){
				genEntSKappa[k] = -sum;
			}
			else {
				genEntSKappa[k] = sum;
			}
		}//q		
		return genEntSKappa;
	}
	
	/**
	 * This method computes the generalized SB entropies
	 * aAccording to Amigo etal. and Curado, E.M.; Nobre, F.D. On the stability of analytic entropic forms. Physica A 2004, 335, 94–106.
	 * @param minB
	 * @param maxB
	 * @param stepB
	 * @param numB
	 * @return
	 */
	public double[] compSB(float minB, float maxB, float stepB, int numB) {
		double[] genEntSB = new double[numB];
		for (int n = 0; n < numB; n++) {
			double valueB = minB + n*stepB; //SB is equal to S_BGS (Bolzmann Gibbs Shannon entropy) for ????????????????? 
			double sum = 0.0f;
			for (int pp = 0; pp < probabilities.length; pp++) {
				//if (probabilities[pp] != 0){
					sum = sum + (1.0 - Math.exp(-valueB*probabilities[pp]));
				//}
			}	
			genEntSB[n] = sum + (Math.exp(-valueB)-1.0); 
		}//q		
		return genEntSB;
	}
	
	/**
	 * This method computes generalized SBeta entropies
	 * According to Amigo etal. and Shafee, F. Lambert function and a new non-extensive form of entropy. IMA J. Appl. Math. 2007, 72, 785–800.
	 * @param minBeta
	 * @param maxBeta
	 * @param stepBeta
	 * @param numBeta
	 * @return
	 */
	public double[] compSBeta(float minBeta, float maxBeta, float stepBeta, int numBeta) {
		double[] genEntSBeta = new double[numBeta];
		for (int n = 0; n < numBeta; n++) {
			double valueBeta = minBeta + n*stepBeta; //SBeta is equal to S_BGS (Bolzmann Gibbs Shannon entropy) for valueBeta = 1; 
			double sum = 0.0f;
			for (int pp = 0; pp < probabilities.length; pp++) {		
				if (probabilities[pp] != 0.0){ //leaving out 0 
					sum = sum + Math.pow(probabilities[pp],  valueBeta) * Math.log(1.0/probabilities[pp]);
				}
			}
			genEntSBeta[n] = sum;					
		}//q		
		return genEntSBeta;
	}
	
	/**
	 * This method computes generalized SGamma entropies
	 * According to Amigo etal. and Tsallis Introduction to Nonextensive Statistical Mechanics, 2009, S61
	 * @param minGamma
	 * @param maxGamma
	 * @param stepGamma
	 * @param numGamma
	 * @return
	 */
	public double[] compSGamma(float minGamma, float maxGamma, float stepGamma, int numGamma) {
		double[] genEntSGamma = new double[numGamma];
		for (int g = 0; g < numGamma; g++) {
			double valueGamma = minGamma + g*stepGamma; //SGama is equal to S_BGS (Bolzmann Gibbs Shannon entropy) for valueGamma = 1; 
			double sum = 0.0f;
			for (int pp = 0; pp < probabilities.length; pp++) {		
				if (probabilities[pp] != 0.0){ //leaving out 0 
					sum = sum + Math.pow(probabilities[pp],  1.0/valueGamma) * Math.log(1.0/probabilities[pp]);
				}
			}
			genEntSGamma[g] = sum;					
		}//q
		return genEntSGamma;
		
	}
	
}
