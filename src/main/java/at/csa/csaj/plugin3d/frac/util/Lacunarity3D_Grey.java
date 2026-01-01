/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Lacunarity3D_Grey.java
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
package at.csa.csaj.plugin3d.frac.util;

import java.util.ArrayList;

import org.scijava.app.StatusService;
import org.scijava.plugin.Parameter;

import at.csa.csaj.commons.CsajDialog_WaitingWithProgressBar;
import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

/**	Lacunarity for 3D image volumes, grey and binary
  *
 * @author Helmut Ahammer, Martin Reiss
 * @since 2022-11-18  
 */

public class Lacunarity3D_Grey implements Lacunarity3DMethods{
	
	private RandomAccessibleInterval<?> rai = null;
	private static Img<UnsignedByteType> imgBin;
	private static RandomAccess<UnsignedByteType> raBin;
	private static Img<FloatType> imgDil;
	private static RandomAccess<FloatType> raDil;
	private static RandomAccessibleInterval<?> raiDil;
	private static RandomAccessibleInterval<?> raiBox;
	private static RandomAccess<UnsignedByteType> ra;
	private static Cursor<?> cursor = null;
	private int numBoxes = 0;
	private long width = 0;
	private long height = 0;
	private long depth = 0;
	private double[] totals = null;
	private int[] eps = null;
	private String scanningType;
	private String colorModelType;
	private int pixelPercentage;
	private CsajDialog_WaitingWithProgressBar dlgProgress;
	private StatusService statusService;
	
	@Parameter
	private OpService opService;
	
	@Override
	public double[] getTotals() {
		return totals;
	}

	@Override
	public void setTotals(double[] totals) {
		this.totals = totals;
	}

	@Override
	public int[] getEps() {
		return eps;
	}

	@Override
	public void setEps(int[] eps) {
		this.eps = eps;
	}


	/**
	 * This is the standard constructor
	 * 
	 * @param operator the {@link AbstractOperator} firing progress updates
	 */
	public Lacunarity3D_Grey(RandomAccessibleInterval<?> rai, int numBoxes, String scanningType, String colorModelType, int pixelPercentage, CsajDialog_WaitingWithProgressBar dlgProgress, StatusService statusService) {
		this.rai             = rai;
		this.width           = rai.dimension(0);
		this.height          = rai.dimension(1);
		this.depth           = rai.dimension(2);
		this.numBoxes        = numBoxes;
		this.scanningType    = scanningType;
		this.colorModelType  = colorModelType;
		this.pixelPercentage = pixelPercentage;
		this.dlgProgress     = dlgProgress;
		this.statusService   = statusService;
	}

	public Lacunarity3D_Grey() {
	}

	/**
	 * This method calculates the 3D Correlation dimension
	 * @return totals
	 */
	@Override
	public double[] calcTotals() {
		
		dlgProgress.setBarIndeterminate(false);
		int percent;

		if (eps == null) this.calcEps();
		
		// Get size 
		//long width = raiVolume.dimension(0);
		//long height = raiVolume.dimension(1);
		//long depth = raiVolume.dimension(2);
		//RandomAccess<RealType<?>>ra = (RandomAccess<RealType<?>>) raiVolume.randomAccess();
		
		//totals == lacunarities
		double[] totals = new double[numBoxes + 2]; //+2 because of weighted mean L and mean L
		for (int l = 0; l < totals.length; l++) totals[l] = Double.NaN;
		
		long number_of_points = 0;
		int max_random_number = (int) (100/pixelPercentage); // Evaluate max. random number
		if (scanningType.equals("Raster box")) max_random_number = 1; //take always all boxes 
		int random_number = 0;
		
		percent = 1;
		dlgProgress.updatePercent(String.valueOf(percent+"%"));
		dlgProgress.updateBar(percent);
		//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
		statusService.showStatus(percent, 100, "Initializing finished");
			
		//------------------------------------------------------------------------------------------
		if (scanningType.equals("Raster box") || scanningType.equals("Sliding box")) {	
			double mean = 0.0;
			double var = 0.0;
			//int epsWidth = 1;
			int boxSize = 0;
			double count = 0;
			int sample = 0;
			int delta = 0;
			ArrayList<Double> countList;
			if  (max_random_number == 1) { // no statistical approach, take all image pixels		
				//loop through box sizes, each box size gives a lacunarity
				for (int n = 0; n < numBoxes; n++) {		
					percent = (int)Math.max(Math.round((((float)n)/((float)numBoxes)*100.f)), percent);
					dlgProgress.updatePercent(String.valueOf(percent+"%"));
					dlgProgress.updateBar(percent);
					//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
					statusService.showStatus((n+1), numBoxes, "Processing " + (n+1) + "/" + numBoxes);
					
					boxSize = eps[n];
					countList = new ArrayList<Double>();
					mean = 0.0;
					var = 0.0;
					sample = 0;
					if      (scanningType.equals("Raster box")) delta = boxSize;
					else if (scanningType.equals("Sliding box")) delta = 1;
					//Raster through image
					for (int x = 0; x <= (width-boxSize); x = x+delta){
						for (int y = 0;  y <= (height-boxSize); y = y+delta){		
							for (int z = 0;  z <= (depth-boxSize); z = z+delta){		
								raiBox = Views.interval(rai, new long[]{x, y, z}, new long[]{x+boxSize-1, y+boxSize-1, z+boxSize-1});
								count = 0;
								// Loop through all pixels of this box.
								cursor = Views.iterable(raiBox).localizingCursor();
								while (cursor.hasNext()) { //Box
									cursor.fwd();
									//cursorF.localize(pos); 
									sample = ((UnsignedByteType) cursor.get()).get();
									if (sample > 0) {
										// Binary Image: 0 and [1, 255]! and not: 0 and 255
										if (colorModelType.equals("Binary")) count = count + 1.0;
										if (colorModelType.equals("Grey"))   count = count + sample;
									}								
								}//while Box
								countList.add(count);
								//if (colorModelType.equals("Binary")) countList.add(count);
								//if (colorModelType.equals("Grey"))   countList.add(count/(255*boxSize*boxSize));
							} //z
						} //y	
					} //x
					//mean for this box size
					for (double c: countList) mean = mean + c;
					mean = mean/countList.size();
					
					//variance for this box size 
					for (double c: countList) var = (var + (c - mean) * (c - mean));
					var = var/countList.size();
					
					// calculate and set lacunarity value
					totals[n]= var/ (mean * mean) + 1; // lacunarityboxSizes[l] = boxSize;// = epsWidth; = kernelSize;
				} 
			}
			else { //statistical approach
				//loop through box sizes, each box size gives a lacunarity
				for (int n = 0; n < numBoxes; n++) {		
					percent = (int)Math.max(Math.round((((float)n)/((float)numBoxes)*100.f)), percent);
					dlgProgress.updatePercent(String.valueOf(percent+"%"));
					dlgProgress.updateBar(percent);
					//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
					statusService.showStatus((n+1), numBoxes, "Processing " + (n+1) + "/" + numBoxes);
					
					boxSize = eps[n];
					countList = new ArrayList<Double>();
					mean = 0.0;
					var = 0.0;
					sample = 0;
					if      (scanningType.equals("Raster box")) delta = boxSize;
					else if (scanningType.equals("Sliding box")) delta = 1;
					//Raster through image
					for (int x = 0; x <= (width-boxSize); x = x+delta){
						for (int y = 0;  y <= (height-boxSize); y = y+delta){	
							for (int z = 0;  z <= (depth-boxSize); z = z+delta){	
								random_number = (int) (Math.random()*max_random_number+1);
								if( random_number == 1 ){ // UPDATE 07.08.2013 
									raiBox = Views.interval(rai, new long[]{x, y, z}, new long[]{x+boxSize-1, y+boxSize-1, z+boxSize-1});
									count = 0;
									// Loop through all pixels of this box.
									cursor = Views.iterable(raiBox).localizingCursor();
									while (cursor.hasNext()) { //Box
										cursor.fwd();
										//cursorF.localize(pos); 
										sample = ((UnsignedByteType) cursor.get()).get();
										if (sample > 0) {
											// Binary Image: 0 and [1, 255]! and not: 0 and 255
											if (colorModelType.equals("Binary")) count = count + 1.0;
											if (colorModelType.equals("Grey"))   count = count + sample;
										}								
									}//while Box
									countList.add(count);
									//if (colorModelType.equals("Binary")) countList.add(count);
									//if (colorModelType.equals("Grey"))   countList.add(count/(255*boxSize*boxSize));
								}
							} //z
						} //y	
					} //x
					//mean for this box size
					for (double c: countList) mean = mean + c;
					mean = mean/countList.size();
					
					//variance for this box size 
					for (double c: countList) var = (var + (c - mean) * (c - mean));
					var = var/countList.size();
					
					// calculate and set lacunarity value
					totals[n] = var/ (mean * mean) + 1; // lacunarity , sometimes + 1, sometimes not
				} 
			}
		}
		
		
		//------------------------------------------------------------------------------------------
		//this seems to be not accurate as it should be
		//maybe convolution of kernel at boundaries make problems
		else if (scanningType.equals("Fast Sliding box")) {
			RectangleShape kernel;
			Runtime runtime = Runtime.getRuntime();
			long maxMemory = runtime.maxMemory();
			long totalMemory = runtime.totalMemory();
			long freeMemory = runtime.freeMemory();
			int availableProcessors = runtime.availableProcessors();
			//System.out.println("availabel processors: " + availableProcessors);
			
			int numThreads = 6; //For dilation //with 6 it was 3 times faster than only with one thread
			if (numThreads > availableProcessors) numThreads = availableProcessors;
			
			//create binary image with 0 and 1
			//later neighbors can be simply counted by dilation
			imgBin = new ArrayImgFactory<>(new UnsignedByteType()).create(width, height); //always single 2D
			//uiService.show("imgBin1", imgBin);
			raBin = imgBin.randomAccess();
			ra = (RandomAccess<UnsignedByteType>) rai.randomAccess();
			cursor = imgBin.localizingCursor();
			double sample = 0;
			int[] pos = new int[3];
			while (cursor.hasNext()) {
				cursor.fwd();
				cursor.localize(pos);
				ra.setPosition(pos);
				sample = (double)ra.get().getInteger();
				if (sample == 0.0) {
					((UnsignedByteType) cursor.get()).set(0);
				} else {
					if (colorModelType.equals("Binary")) ((UnsignedByteType) cursor.get()).set(1);
					if (colorModelType.equals("Grey"))   ((UnsignedByteType) cursor.get()).set((int)sample); //simply a copy
				}		
			}
			//uiService.show("imgBin2", imgBin);
			double mean = 0.0;
			double var = 0.0;
			int N = 0;
			int epsWidth = 1;
			Img<DoubleType> avgKernel;
			//loop through box sizes, each box size gives a lacunarity
			for (int n = 0; n < numBoxes; n++) {
				percent = (int)Math.max(Math.round((((float)n)/((float)numBoxes)*100.f)), percent);
				dlgProgress.updatePercent(String.valueOf(percent+"%"));
				dlgProgress.updateBar(percent);
				//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
				statusService.showStatus((n+1), numBoxes, "Processing " + (n+1) + "/" + numBoxes);
				
				//Compute dilated image
				int kernelSize = 2 * epsWidth + 1;
	//			kernel = new RectangleShape(kernelSize, false); //kernelSize x kernelSize skipCenter = true
	//			imgDil = (Img<UnsignedByteType>) Dilation.dilate(imgBin, kernel, numThreads);
				
				// create the averageing kernel
				//avgKernel = opService.create().img(new int[] {kernelSize, kernelSize}); may not work in older Fiji versions
				avgKernel = new ArrayImgFactory<>(new DoubleType()).create(kernelSize, kernelSize); 
				
				for (DoubleType k : avgKernel) {
					k.setReal(1.0);
				}
				//This must be solved******************************************************
				//TODO
				//This causes an error during runtime:
				//incompatible types: net.imglib2.RandomAccessibleInterval<O> cannot be converted to net.imglib2.img.Img<net.imglib2.type.numeric.real.FloatType>
				//imgDil = (Img<FloatType>) opService.filter().convolve(imgBin, avgKernel);
				//************************************************************************
				
				//uiService.show("imgDil", imgDil);
				// each pixel value is now the sum of the box (neighborhood values)
				// get statistics
				mean = 0.0;
				var = 0.0;
				N = 0;
				sample = 0;
				
				// mean
				cursor = imgDil.localizingCursor();
				while (cursor.hasNext()) {
					cursor.fwd();
					sample = ((FloatType) cursor.get()).getRealFloat();//-1 subtract midpoint itself from number
					//if (sample > 0f) {
						N = N + 1;
						mean = mean + sample;
					//} 	
				}
				mean = mean/N;
			
				// variance
				sample = 0.0;
				cursor = imgDil.localizingCursor();
				while (cursor.hasNext()) {
					cursor.fwd();
					sample = ((FloatType) cursor.get()).getRealFloat();//-1 subtract midpoint itself from number
					//if (sample > 0f) {
						//N=N+1; //calculated already
						var = (var + (sample - mean) * (sample - mean));
					//} 	
				}
				var = var/N;
				// calculate and set lacunarity value
				totals[n] = var/ (mean * mean) + 1; // lacunarity , sometimes + 1, sometimes not
				eps[n] = kernelSize;// = epsWidth; = kernelSize;
				// System.out.println("Lacunarity:  mean: " +  mean + "      stddev: "+stddev );
		
				epsWidth = epsWidth * 2;
				// epsWidth = epsWidth+1;
			} // 0>=l<=numEps loop through eps
		}	
		//------------------------------------------------------------------------------------------
		else if (scanningType.equals("Tug of war")) {	
			/**
			 * 
			 * Martin Reiss
			 * DESCRIPTION:
			 *  Die äquivalente Definition der Lacunarity aus [1] erlaubt es nun, eine neue Approximationsmethode 
			 *  für die Fixed-Grid Lacunarity vorzuschlagen. Das erste Moment bleibt für einen Fixed-Grid Scan bei den verschiedenen Boxgrö"sen konstant. 
			 *  Dies entspricht einfach der Gesamtzahl aller Objektpixel. Somit ist die Lacunarity nur mehr vom zweiten Moment der Besetzungszahlen abhängig. 
			 *  Dieses zweite Moment kann aber mittels der Tug of War Methode berechnet werden. Der neu vorgeschlagene Algorithmus ermöglicht es also 
			 *  die Fixed-Grid Lacunarity mittels Tug of War zu approximieren. 
			 *  Das beschriebene Verfahren soll in der Folge als Tug of War Lacunarity [2] bezeichnet werden.
			 *  
			 *  [1] Tolle C.R., McJunkin T.R., Gorsich D.J., An efficient implementation of the gliding box lacunarity algorithm.
			 *  [2] Masterthesis: Fraktale Dimension von Volumsdaten, Martin Reiß
			 * 
			 *  KNOWN PROBLEMS/OPEN QUESTIONS:
			 * 	1) Method not that fast as it's supposed to be.
			 * 	2) In 1D TugOfWar Lacunarity the result is very sensitive to the value of q. (Example: Regular Line) 
			 * 
			 *  UPDATES: MR 19.10.2016 elimination of negative values in getTotals() method according to Chaos 2016 paper
			 */	
			int s1 = 30;//accuracy;   // s1 = 30; Wang paper	//accuracy 	
			int s2 = 5; //confidence; // s2 = 5;	 Wang paper //confidence
			int q  = 10501;	//prime number
			
			double[] sum2   = new double[numBoxes];
			//double[] totals = new double[numBoxes];		
			double[] count  = new double[s1];
			double[] meanS1 = new double[s2];
			
			long L = 0;		
			double sum = 0.0;		
			boolean even;	
			int xx = 0;
			int yy = 0;
			int zz = 0;
			int part1 = 0;
			int part2 = 0;
			int part3 = 0;
			int hashFunction = 0;
			int k = 0;

			double lac;
			int epsWidth = 1;
			int boxSize = 0;
			//for (int n = 0; n < numBoxes; n++) eps[n] = (int)Math.pow(2, n);
			for (int n = 0; n < numBoxes; n++) totals[n] = 0.0;
			
			// Count total number of points		
			int sample;
			cursor = Views.iterable(rai).localizingCursor();
			while (cursor.hasNext()) {
				cursor.fwd();
				sample = ((UnsignedByteType) cursor.get()).getInteger();
				if (sample > 0) L = L + 1;		
			}

			// Get coordinates
			ra = (RandomAccess<UnsignedByteType>) rai.randomAccess();
			int[] xCoordinate = new int[(int) L];
			int[] yCoordinate = new int[(int) L];	
			int[] zCoordinate = new int[(int) L];	
			
			do {
				for(int x = 0; x < width; x++){	
					for(int y = 0; y < height; y++){
						for(int z = 0; z < depth; z++){
							ra.setPosition(x, 0);
							ra.setPosition(y, 1);
							ra.setPosition(z, 2);
							if (ra.get().getInteger() > 0 ){	
								xCoordinate[k] = x;
								yCoordinate[k] = y;
								zCoordinate[k] = z;
								k++;
							}
						}
					}
				}
			} while ( k < L );
			
			//TugOfWar - algorithm	
			for (int c = 0; c < count.length; c++) count[c] = 0.0;
			for (int s = 0; s < sum2.length; s++) sum2[s] = 0.0;
			sum = 0.0;
			//epsWidth = 1;
			
			int a_prim = 0;
			int b_prim = 0;
			int c_prim = 0;
			int d_prim = 0;
			
			percent = 5;
			dlgProgress.updatePercent(String.valueOf(percent+"%"));
			dlgProgress.updateBar(percent);
			//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
			statusService.showStatus(percent, 100, "Initializing finished");
			
			if (L > 0) {
				for (int n = 0; n < numBoxes; n++) {	
						percent = (int)Math.max(Math.round((((float)n)/((float)numBoxes)*100.f)), percent);
						dlgProgress.updatePercent(String.valueOf(percent+"%"));
						dlgProgress.updateBar(percent);
						//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
						statusService.showStatus((n+1), numBoxes, "Processing " + (n+1) + "/" + numBoxes);
						boxSize = eps[n];
						//boxSize = 2 * epsWidth + 1;
						
						// STEP 1: PARTITION s1*s2 COUNTERS INTO s2 SETS OF s1 COUNTERS
						for(int s2_i = 0; s2_i < s2; s2_i++){
							for(int s1_i = 0; s1_i < s1; s1_i++){	
								
								a_prim = getPrimeNumber();
								b_prim = getPrimeNumber();
								c_prim = getPrimeNumber();
								d_prim = getPrimeNumber();
		
								for(int i = 0; i < L; i++){	
									xx = (int)Math.round(xCoordinate[i]/boxSize); 
									yy = (int)Math.round(yCoordinate[i]/boxSize); 
									// hash function 
									part1 = a_prim*xx*xx*xx + b_prim*xx*xx + c_prim*xx + d_prim;
									part2 = a_prim*a_prim*yy*yy*yy + b_prim*b_prim*yy*yy + c_prim*c_prim*yy + d_prim*d_prim;	
									part3 = a_prim*a_prim*a_prim*zz*zz*zz + b_prim*b_prim*b_prim*zz*zz + c_prim*c_prim*c_prim*zz + d_prim*d_prim*d_prim;
									hashFunction = (part1 + part2 + part3) % q; 
									even = (hashFunction & 1) == 0;							
									if (even) {
										count[s1_i]++;
									} else {
										count[s1_i]+=-1;
									}
								}
							} 
							for(int s1_i = 0; s1_i < s1; s1_i++){
								sum += count[s1_i]*count[s1_i]; 
							}				
							meanS1[s2_i] = sum / s1; 					
							sum = 0.0;
							for(int s1_i = 0; s1_i < s1; s1_i++){	
								count[s1_i] = 0;
							}	
						} 				
						for(int s2_i = 0; s2_i < s2; s2_i++){
							sum2[n] += meanS1[s2_i] / s2;	
						}							
						lac = (double)(sum2[n]/(L*L))*(width*height/(boxSize*boxSize));
						
						//Note that lacunarity (= log[totals]) is always positive.
//							if (lac < 1) {
//								totals[n] =  + 1; //Martin did it so
//							} else {
//								totals[n] = lac;								
//							}
						
						//Changed by Helmut
						totals[n] = lac;	
						
						for(int s2_i = 0; s2_i < s2; s2_i++){	
							meanS1[s2_i] = 0;
						}
						//boxSizes[n] = boxSize;// = epsWidth; = kernelSize;
						//epsWidth = epsWidth * 2;
						// epsWidth = epsWidth+1;
				} //Box sizes
				
			} else {
				// Set totals to zero if image is empty.
				for (int n = 0; n < numBoxes; n++) totals[n] =  0;	
			}		
		}//Tug of war
		//---------------------------------------------------------------------------------------------
		//Compute weighted mean Lacunarity according to Equ.3 of Roy&Perfect, 2014, Fractals, Vol22, No3
		//DOI: 10.1142/S0218348X14400039
		double L_RP = 0.0;
		double sumBoxSize = 0.0;
		for (int n = 0; n < numBoxes; n++) {	
			L_RP = L_RP + Math.log10(totals[n]) * Math.log10(eps[n]);
			sumBoxSize = sumBoxSize + Math.log10(eps[n]);
		}
		totals[numBoxes] = L_RP/sumBoxSize; //append it to the list of lacunarities	
		
		//Compute mean Lacunarity according to Equ.29 of Sengupta and Vinoy, 2006, Fractals, Vol14 No4 p271-282
		//DOI: 10.1142/S0218348X06003313 
		double L_SV = 0.0;
		for (int n = 0; n < numBoxes; n++) {	
			L_SV = L_SV + totals[n] * eps[n];
		}
		totals[numBoxes + 1] = Math.log(L_SV/(eps[numBoxes-1] - eps[0])); //append it to the list of lacunarities	

		return totals; //lacunarities		
	}
	
	/**
	 * This method calculates the Boxes
	 * @return eps
	 */
	@Override
	public int[] calcEps() {

		eps = new int[numBoxes];
		for (int n = 0; n < numBoxes; n++) {
			eps[n] = (int)Math.round(Math.pow(2, n));
		}
		return eps;
	}	
	
	//-------------------------------------------------------------------------------------------
	public int getPrimeNumber(){
		
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
		
		int randn = (int) (Math.random()*500);
		return primeNumbers[randn]; 	
	}

}


