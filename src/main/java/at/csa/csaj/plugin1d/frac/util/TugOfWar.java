/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: TugOfWar.java
 * 
 * $Id$
 * $HeadURL$
 * 
 * This file is part of ComsystanJ software, hereinafter referred to as "this program".
 * %%
 * Copyright (C) 2024 Comsystan Software
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
package at.csa.csaj.plugin1d.frac.util;


import org.scijava.log.LogService;

import at.csa.csaj.commons.CsajRegression_Linear;
import net.imglib2.RandomAccess;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.Views;



/**
 * Martin Reiss
 * TUG OF WAR - KORRELATIONSDIMENSION 
 * <p>
 * <b>Changes</b>
 * <ul>
 * <li>
 * </ul>
 * 
 * 
 * @author Helmut Ahammer
 * @since 2022 04
 */

public class TugOfWar {

	private LogService logService;
	
	
	private int progressBarMin = 0;
	private int progressBarMax = 100;
	
	private double[] eps;
	private double[] lnDataX;
	private double[] lnDataY;

	public double[] getEps() {
		return eps;
	}

	public void setEps(double[] eps) {
		this.eps = eps;
	}
	
	public double[] getLnDataX() {
		return lnDataX;
	}

	public void setLnDataX(double[] lnDataX) {
		this.lnDataX = lnDataX;
	}

	public double[] getLnDataY() {
		return lnDataY;
	}

	public void setLnDataY(double[] lnDataY) {
		this.lnDataY = lnDataY;
	}

	public int getProgressBarMin() {
		return progressBarMin;
	}

	public void setProgressBarMin(int progressBarMin) {
		this.progressBarMin = progressBarMin;
	}

	public int getProgressBarMax() {
		return progressBarMax;
	}

	public void setProgressBarMax(int progressBarMax) {
		this.progressBarMax = progressBarMax;
	}

	/**
	 * This is the standard constructor
	 */
	public TugOfWar() {

	}

	/**
	 * This method calculates the "Lengths" of the series
	 * 
	 * @param data  1D data double[]
	 * @param numBoxes
	 * @param accuracy
	 * @param confidence
	 * @return double[] totals [numBoxes]
	 */
	public double[] calcTotals(double[] data, int numBoxes, int accuracy, int confidence) {
	
		int N = data.length;
		double[] totals = new double[numBoxes];
	
		//Tug of war parameters
		int s1=accuracy; 	//accuracy parameter	//s1=30 (Paper)
		int s2=confidence;	//confidence parameter	//s2=5 (Paper)
				
		double[] count  = new double[s1];
		double[] meanS1 = new double[s2];
		
		long totalNumberOfPoints = 0;
		
		double count_sum = 0.0;
		
		int q = 8837;
		int xx = 0;

		int x_part = 0;

		int hash_function = 0;
		int k = 0;
		
		boolean even;
				
		// Count total number of points		
		for (int n = 0; n < N; n++) {		
			if (data[n] != 0) totalNumberOfPoints = totalNumberOfPoints + 1;		
		}
			
		// Get coordinates
		int[] xCoordinate=new int [(int)totalNumberOfPoints];
		
		do {
			for(int n = 0; n < N; n++){			
				if (data[n] != 0 ){	
					xCoordinate[k] = n;
					k++;
				}
			}
		} while ( k < totalNumberOfPoints );
			
		int radius = 0;
		int a_prim = 0;
		int b_prim = 0;
		int c_prim = 0;
		int d_prim = 0;
				
		for (int n = 0; n < numBoxes; n++) {
			
			radius = (int)Math.round(Math.pow(2, n));		
			double proz = (double) n / (double) numBoxes * 100;
			//System.out.println("Loop progress: " + (int) proz + " %");
		
			// STEP 1: PARTITION s1*s2 COUNTERS INTO s2 SETS OF s1 COUNTERS	
			for(int s2_i = 0; s2_i < s2; s2_i++){
				for(int s1_i = 0; s1_i < s1; s1_i++){	
				
					a_prim = getPrimeNumber();
					b_prim = getPrimeNumber();
					c_prim = getPrimeNumber();
					d_prim = getPrimeNumber();

					// sum over coordinates
					for(int i = 0; i < totalNumberOfPoints; i++){	
						xx=(int) Math.floor( xCoordinate[i]/radius); 

						// hash function in parts
						x_part =        a_prim*xx*xx*xx +        b_prim*xx*xx +        c_prim*xx + d_prim;

						hash_function = (x_part)%q; // UPDATE 07.08.2013 

						even = (hash_function & 1) == 0; // Very slow! Try hash_function=1000 

						if (even) {
							count[s1_i]++;
						} else {
							count[s1_i]+=-1;
						}
					} // end sum over coordinates
				} // end s1 loop

				for(int s1_i = 0; s1_i < s1; s1_i++){
				count_sum += count[s1_i]*count[s1_i]; // square the counters values	
				}
				// STEP 2: COMPUTE THE MEAN OF EACH SUBSET
				meanS1[s2_i] = count_sum / s1; 
				
				// Set values equal to zero
				count_sum = 0.0;
				for(int s1_i = 0; s1_i < s1; s1_i++){	
				count[s1_i] = 0;
				}
				
			} // End s2 loop
			
			// STEP 3: DETERMINE THE MEDIAN
			for(int s2_i = 0; s2_i < s2; s2_i++){
			totals[n] += meanS1[s2_i] / s2;	
			}

			// Set values equal to zero
//					for(int s2_i = 0; s2_i < s2; s2_i++){	
//					meanS1[s2_i] = 0;
//					}			
			meanS1 = new double[s2];
			
		}// end boxsize loop	
			
		return totals;
	}

	/**
	 * 
	 * @param totals Tug of war totals
	 * @param numRegStart
	 * @param numRegEnd
	 * @return double[] regression parameters
	 */
	public double[] calcRegression(double[] totals, int numRegStart, int numRegEnd) {
		
		eps     = new double[totals.length];
		lnDataY = new double[totals.length];
		lnDataX = new double[totals.length]; //NumBoxes

		for (int i = 0; i < totals.length; i++) {
			eps[i] = Math.round(Math.pow(2, i)); 
			if (totals[i] == 0)
				totals[i] = Double.MIN_VALUE;
		}

	
		double lnX;
		double lnY;
		for (int i = 0; i < totals.length; i++) {
			lnX = Math.log(eps[i]);
			lnY = Math.log(totals[i]);
			lnDataX[i] = lnX;
			lnDataY[i] = lnY;				
		}
	
		// Compute regression
		CsajRegression_Linear lr = new CsajRegression_Linear();

//		double[] dataXArray = new double[lnDataX.size()];
//		double[] dataYArray = new double[lnDataY.size()];
//		for (int i = 0; i < lnDataX.size(); i++) {
//			dataXArray[i] = lnDataX.get(i).doubleValue();
//		}
//		for (int i = 0; i < lnDataY.size(); i++) {
//			dataYArray[i] = lnDataY.get(i).doubleValue();
//		}

		double[] regressionParams = lr.calculateParameters(lnDataX, lnDataY, numRegStart, numRegEnd);
		//0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
		
		return regressionParams;
	}
	
private int getPrimeNumber(){
		
		int[] primeNumbers = {
			  46349,48533,44123,
			  46351,48539,44129,
			  46381,48541,44131,
			  46399,48563,44159,
			  46411,48571,44171,
			  46439,48589,44179,
			  46441,48593,44189,
			  46447,48611,44201,
			  46451,48619,44203,
			  46457,48623,44207,
			  46471,48647,44221,
			  46477,48649,44249,
			  46489,48661,44257,
			  46499,48673,44263,
			  46507,48677,44267,
			  46511,48679,44269,
			  46523,48731,44273,
			  46549,48733,44279,
			  46559,48751,44281,
			  46567,48757,44293,
			  46573,48761,44351,
			  46589,48767,44357,
			  46591,48779,44371,
			  46601,48781,44381,
			  46619,48787,44383,
			  46633,48799,44389,
			  46639,48809,44417,
			  46643,48817,44449,
			  46649,48821,44453,
			  46663,48823,44483,
			  46679,48847,44491,
			  46681,48857,44497,
			  46687,48859,44501,
			  46691,48869,44507,
			  46703,48871,44519,
			  46723,48883,44531,
			  46727,48889,44533,
			  46747,48907,44537,
			  46751,48947,44543,
			  46757,48953,44549,
			  46769,48973,44563,
			  46771,48989,44579,
			  46807,48991,44587,
			  46811,49003,44617,
			  46817,49009,44621,
			  46819,49019,44623,
			  46829,49031,44633,
			  46831,49033,44641,
			  46853,49037,44647,
			  46861,49043,44651,
			  46867,49057,44657,
			  46877,49069,44683,
			  46889,49081,44687,
			  46901,49103,44699,
			  46919,49109,44701,
			  46933,49117,44711,
			  46957,49121,44729,
			  46993,49123,44741,
			  46997,49139,44753,
			  47017,49157,44771,
			  47041,49169,44773,
			  47051,49171,44777,
			  47057,49177,44789,
			  47059,49193,44797,
			  47087,49199,44809,
			  47093,49201,44819,
			  47111,49207,44839,
			  47119,49211,44843,
			  47123,49223,44851,
			  47129,49253,44867,
			  47137,49261,44879,
			  47143,49277,44887,
			  47147,49279,44893,
			  47149,49297,44909,
			  47161,49307,44917,
			  47189,49331,44927,
			  47207,49333,44939,
			  47221,49339,44953,
			  47237,49363,44959,
			  47251,49367,44963,
			  47269,49369,44971,
			  47279,49391,44983,
			  47287,49393,44987,
			  47293,49409,45007,
			  47297,49411,45013,
			  47303,49417,45053,
			  47309,49429,45061,
			  47317,49433,45077,
			  47339,49451,45083,
			  47351,49459,45119,
			  47353,49463,45121,
			  47363,49477,45127,
			  47381,49481,45131,
			  47387,49499,45137,
			  47389,49523,45139,
			  47407,49529,45161,
			  47417,49531,45179,
			  47419,49537,45181,
			  47431,49547,45191,
			  47441,49549,45197,
			  47459,49559,45233,
			  47491,49597,45247,
			  47497,49603,45259,
			  47501,49613,45263,
			  47507,49627,45281,
			  47513,49633,45289,
			  47521,49639,45293,
			  47527,49663,45307,
			  47533,49667,45317,
			  47543,49669,45319,
			  47563,49681,45329,
			  47569,49697,45337,
			  47581,49711,45341,
			  47591,49727,45343,
			  47599,49739,45361,
			  47609,49741,45377,
			  47623,49747,45389,
			  47629,49757,45403,
			  47639,49783,45413,
			  47653,49787,45427,
			  47657,49789,45433,
			  47659,49801,45439,
			  47681,49807,45481,
			  47699,49811,45491,
			  47701,49823,45497,
			  47711,49831,45503,
			  47713,49843,45523,
			  47717,49853,45533,
			  47737,49871,45541,
			  47741,49877,45553,
			  47743,49891,45557,
			  47777,49919,45569,
			  47779,49921,45587,
			  47791,49927,45589,
			  47797,49937,45599,
			  47807,49939,45613,
			  47809,49943,45631,
			  47819,49957,45641,
			  47837,49991,45659,
			  47843,49993,45667,
			  47857,49999,45673,
			  47869,50021,45677,
			  47881,50023,45691,
			  47903,50033,45697,
			  47911,50047,45707,
			  47917,50051,45737,
			  47933,50053,45751,
			  47939,50069,45757,
			  47947,50077,45763,
			  47951,50087,45767,
			  47963,50093,45779,
			  47969,50101,45817,
			  47977,50111,45821,
			  47981,50119,45823,
			  48017,50123,45827,
			  48023,50129,45833,
			  48029,50131,45841,
			  48049,50147,45853,
			  48073,50153,45863,
			  48079,50159,45869,
			  48091,50177,45887,
			  48109,50207,45893,
			  48119,50221,45943,
			  48121,50227,45949,
			  48131,50231,45953,
			  48157,50261,45959,
			  48163,50263,45971,
			  48179,50273,45979,
			  48187,50287,45989,
			  48193,50291,46021,
			  48197,50311,46027,
			  48221,50321,46049,
			  48239,50329,46051,
			  48247,50333,46061,
			  48259,50341,46073,
			  48271,50359,46091,
			  48281,50363,46093,
			  48299,50377,46099,
			  48311,50383,46103,
			  48313,50387,46133,
			  48337,50411,46141,
			  48341,50417,46147,
			  48353,50423,46153,
			  48371,50441,46171,
			  48383,50459,46181,
			  48397,50461,46183,
			  48407,50497,46187,
			  48409,50503,46199,
			  48413,50513,46219,
			  48437,50527,46229,
			  48449,50539,46237,
			  48463,50543,46261,
			  48473,50549,46271,
			  48479,50551,46273,
			  48481,50581,46279,
			  48487,50587,46301,
			  48491,50591,46307,
			  48497,50593,46309,
			  48523,50599,46327,
			  48527,50627,44119
			};
		
		int randn = (int) (Math.random()*150);
		return primeNumbers[randn]; 	
	}
	
}
