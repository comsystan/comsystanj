/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: GeneralisedDim3D_Grey.java
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
package at.csa.csaj.plugin3d.frac.util;

import org.scijava.app.StatusService;
import org.scijava.plugin.Parameter;

import at.csa.csaj.commons.CsajDialog_WaitingWithProgressBar;
import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.outofbounds.OutOfBoundsBorderFactory;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/**	Box counting dimension for 3D image volumes, grey and binary
  *
 * @author Helmut Ahammer, Martin Reiss
 * @since 2022-11-16  
 */

public class GeneralisedDim3D_Grey implements GeneralisedDim3DMethods{
	
	private RandomAccessibleInterval<?> rai = null;
	private static Img<UnsignedByteType> imgBin;
	private static RandomAccess<UnsignedByteType> raBin;
	private static Img<FloatType> imgDil;
	private static RandomAccess<FloatType> raDil;
	private static RandomAccessibleInterval<?> raiDil;	
	private static RandomAccessibleInterval<?> raiBox;
	private static RandomAccess<?> ra;
	private static Cursor<?> cursor = null;
	private int numBoxes = 0;
	private long width = 0;
	private long height = 0;
	private long depth = 0;
	private double[][] totals = null;
	private int[] eps = null;
	private int minQ = 0;
	private int maxQ = 0;
	private String scanningType;
	private String colorModelType;
	private int pixelPercentage;
	private CsajDialog_WaitingWithProgressBar dlgProgress;
	private StatusService statusService;
	

	@Parameter
	private OpService opService;
	
	@Override
	public double[][] getTotals() {
		return totals;
	}

	@Override
	public void setTotals(double[][] totals) {
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
	public GeneralisedDim3D_Grey(RandomAccessibleInterval<?> rai, int numBoxes, int minQ, int maxQ, String scanningType, String colorModelType, int pixelPercentage, CsajDialog_WaitingWithProgressBar dlgProgress, StatusService statusService) {
		this.rai             = rai;
		this.width           = rai.dimension(0);
		this.height          = rai.dimension(1);
		this.depth           = rai.dimension(2);
		this.numBoxes        = numBoxes;
		this.minQ         = minQ;
		this.maxQ         = maxQ;
		this.scanningType    = scanningType;
		this.colorModelType  = colorModelType;
		this.pixelPercentage = pixelPercentage;
		this.dlgProgress     = dlgProgress;
		this.statusService   = statusService;
	}

	public GeneralisedDim3D_Grey() {
	}

	/**
	 * This method calculates the 3D Generalised dimensions
	 * @return totals
	 */
	@Override
	public double[][] calcTotals() {
		
		dlgProgress.setBarIndeterminate(false);
		int percent;

		if (eps == null) this.calcEps();
		
		// Get size 
		//long width = raiVolume.dimension(0);
		//long height = raiVolume.dimension(1);
		//long depth = raiVolume.dimension(2);
		//RandomAccess<RealType<?>>ra = (RandomAccess<RealType<?>>) raiVolume.randomAccess();
			
		int numQ = maxQ - minQ + 1;
		double[][] totals    = new double[numQ][numBoxes];
		double[]   totalsMax = new double[numBoxes]; //for binary images
	
		percent = 1;
		dlgProgress.updatePercent(String.valueOf(percent+"%"));
		dlgProgress.updateBar(percent);
		//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
		statusService.showStatus(percent, 100, "Initializing finished");
	
		//********************************Binary Image: 0 and [1, 255]! and not: 0 and 255		
		if ((scanningType.equals("Raster box")) || (scanningType.equals("Sliding box"))) {
			//long numObjectPixels; // number of pixel > 0
			//numObjectPixels = this.getNumberOfNonZeroPixels(rai);
			long number_of_points = 0;
			int max_random_number = (int) (100/pixelPercentage); // Evaluate max. random number
			if (scanningType.equals("Raster box")) max_random_number = 1; //take always all boxes 
			int random_number = 0;
			//int radius;		
			double count = 0.0;
			int sample = 0;
			int delta = 0;
			int boxSize = 0;;	
				
			if  (max_random_number == 1) { // no statistical approach, take all image pixels		
				for (int n = 0; n < numBoxes; n++) { //2^0  to 2^numBoxes	
					percent = (int)Math.max(Math.round((((float)n)/((float)numBoxes)*100.f)), percent);
					dlgProgress.updatePercent(String.valueOf(percent+"%"));
					dlgProgress.updateBar(percent);
					//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
					statusService.showStatus((n+1), numBoxes, "Processing " + (n+1) + "/" + numBoxes);
					
					boxSize = eps[n];
					if (scanningType.equals("Raster box"))  delta = boxSize;
					if (scanningType.equals("Sliding box")) delta = 1;
					for (int x =0; x <= (width-boxSize); x = x+delta){
						for (int y =0;  y <= (height-boxSize); y = y+delta){
							for (int z =0;  z <= (depth-boxSize); z = z+delta){
								count = 0;
								raiBox = Views.interval(rai, new long[]{x, y, z}, new long[]{x+boxSize-1, y+boxSize-1, z+boxSize-1});
								// Loop through all pixels of this box.
								cursor = Views.iterable(raiBox).localizingCursor();
								while (cursor.hasNext()) { //Box
									cursor.fwd();
									//cursor.localize(pos);
									sample = ((UnsignedByteType) cursor.get()).get();
									if ( sample > 0) {
										if      (colorModelType.equals("Binary")) count = count + 1; // Binary Image: 0 and [1, 255]! and not: 0 and 255
										else if (colorModelType.equals("Grey"))   count = count + sample;
									}			
								}//while Box
								//count = count / numObjectPixels; // normalized mass of current box
								totalsMax[n] = totalsMax[n] + count; // calculate total count for normalization
								// System.out.println("IqmOpFracGendim: b: "+b+ "   count: "+ count );
								if (count > 0) {
									for (int q = 0; q < numQ; q++) {
										if ((q + minQ) == 1) totals[q][n] = totals[q][n] + count * Math.log(count); // GenDim
										else                    totals[q][n] = totals[q][n] + Math.pow(count, (q + minQ)); // GenDim
									}
								}
							} //z
						} //y	
					} //x                                          
				} //n
			} // no statistical approach
			else { //statistical approach
				for (int n = 0; n < numBoxes; n++) { //2^0  to 2^numBoxes	
					percent = (int)Math.max(Math.round((((float)n)/((float)numBoxes)*100.f)), percent);
					dlgProgress.updatePercent(String.valueOf(percent+"%"));
					dlgProgress.updateBar(percent);
					//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
					statusService.showStatus((n+1), numBoxes, "Processing " + (n+1) + "/" + numBoxes);
					
					//radius = eps[n];	
					boxSize = eps[n];
					if (scanningType.equals("Raster box"))  delta = boxSize;
					if (scanningType.equals("Sliding box")) delta = 1;
					for (int x = 0; x <= (width-boxSize); x = x+delta){
						for (int y = 0;  y <= (height-boxSize); y = y+delta){
							for (int z =0;  z <= (depth-boxSize); z = z+delta){
								random_number = (int) (Math.random()*max_random_number+1);
								if( random_number == 1 ){ // UPDATE 07.08.2013 
									count = 0;
									raiBox = Views.interval(rai, new long[]{x, y, z}, new long[]{x+boxSize-1, y+boxSize-1, z+boxSize-1});
									// Loop through all pixels of this box.
									cursor = Views.iterable(raiBox).localizingCursor();
									while (cursor.hasNext()) { //Box
										cursor.fwd();
										//cursor.localize(pos);
										sample = ((UnsignedByteType) cursor.get()).get();
										if ( sample > 0) {
											if      (colorModelType.equals("Binary")) count = count + 1; // Binary Image: 0 and [1, 255]! and not: 0 and 255
											else if (colorModelType.equals("Grey"))   count = count + sample;
										}			
									}//while Box
									//count = count / numObjectPixels; // normalized mass of current box
									totalsMax[n] = totalsMax[n] + count; // calculate total count for normalization
									// System.out.println("IqmOpFracGendim: b: "+b+ "   count: "+ count );
									if (count > 0) {
										for (int q = 0; q < numQ; q++) {
											if ((q + minQ) == 1)
												totals[q][n] = totals[q][n] + count * Math.log(count); // GenDim
											else
												totals[q][n] = totals[q][n] + Math.pow(count, (q + minQ)); // GenDim
										}
									}
								}
							} //z
						} //y	
					} //x  
				} //n Box sizes		
			}
		}
		//Fast sliding box does not work properly
		//negative q -  strange results
		//e.g. for Menger carpet
		//the first 3x3 kernel does not fit to the larger ones
		else if (scanningType.equals("Fast sliding box (beta)")) {
//			RectangleShape kernel;
//			Runtime runtime = Runtime.getRuntime();
//			long maxMemory = runtime.maxMemory();
//			long totalMemory = runtime.totalMemory();
//			long freeMemory = runtime.freeMemory();
//			int availableProcessors = runtime.availableProcessors();
//			//System.out.println("Available processors: " + availableProcessors);	
//			int numThreads = 6; //For dilation //with 6 it was 3 times faster than only with one thread
//			if (numThreads > availableProcessors) numThreads = availableProcessors;
			
			//create binary image with 0 and 1
			//later neighbors can be simply counted by dilation
			imgBin = new ArrayImgFactory<>(new UnsignedByteType()).create(width, height, depth); //always single 3D
			//uiService.show("imgBin1", imgBin);
			raBin = imgBin.randomAccess();
			ra = (RandomAccess<UnsignedByteType>) rai.randomAccess();
			
			Interval interval;	 
		    IntervalView<?> iv;
				
			cursor = imgBin.localizingCursor();
			int sample = 0;
			double count = 0.0; //Sum of grey values
			int[] pos = new int[3];
			while (cursor.hasNext()) {
				cursor.fwd();
				cursor.localize(pos);
				ra.setPosition(pos);
				sample = ((UnsignedByteType) ra.get()).getInteger();
				if (sample == 0) {
					((UnsignedByteType) cursor.get()).set(0);
				} else {
					//((UnsignedByteType) cursor.get()).set(1);
					if (colorModelType.equals("Binary")) ((UnsignedByteType) cursor.get()).set(1);
					if (colorModelType.equals("Grey"))   ((UnsignedByteType) cursor.get()).set(sample); //simply a copy
				}		
			}
			//uiService.show("imgBin2", imgBin);
			int proz;
			int epsWidth = 1;
			int kernelSize; 
			Img<DoubleType> avgKernel;
			//loop through box sizes
			for (int n = 0; n < numBoxes; n++) {
				
				percent = (int)Math.max(Math.round((((float)n)/((float)numBoxes)*100.f)), percent);
				dlgProgress.updatePercent(String.valueOf(percent+"%"));
				dlgProgress.updateBar(percent);
				//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
				statusService.showStatus((n+1), numBoxes, "Processing " + (n+1) + "/" + numBoxes);
				
			
				//Compute dilated image
				kernelSize = 2 * epsWidth + 1;
				eps[n] = kernelSize;// = epsWidth; = kernelSize; //overwrite eps[n]	
				
//				kernel = new RectangleShape(kernelSize, false); //kernelSize x kernelSize skipCenter = true
//				imgDil = (Img<UnsignedByteType>) Dilation.dilate(imgBin, kernel, numThreads);
				
				// create the averaging kernel
				//avgKernel = opService.create().img(new int[] {kernelSize, kernelSize});  may not work in older Fiji versions
				avgKernel = new ArrayImgFactory<>(new DoubleType()).create(kernelSize, kernelSize, kernelSize);
							
				for (DoubleType k : avgKernel) {
					k.setReal(1.0);
				}
				//imgDil = (Img<FloatType>) opService.filter().convolve(imgBin, avgKernel);
				imgDil = (Img<FloatType>) opService.filter().convolve(imgBin, avgKernel, new OutOfBoundsBorderFactory()); //not really a difference compared to without Factory
			
//				interval = Intervals.expand(imgBin, -kernelSize );
//				OutOfBoundsBorderFactory oobf = new OutOfBoundsBorderFactory();
//				oobf.create(interval);		 
//				imgDil = (Img<FloatType>) opService.filter().convolve(imgBin, avgKernel, oobf);
						
				//uiService.show("n=" + n + " imgDil", imgDil);
				
				//Each pixel value is now the sum of the box (neighborhood values)
				
				//Restricted interval, because border values are not correctly computed
				interval = Intervals.expand(imgDil, - kernelSize );		 
			    iv = Views.interval(imgDil, interval);
				count = 0.0;
				cursor = iv.localizingCursor();
				while (cursor.hasNext()) {
					cursor.fwd();
					count = ((FloatType) cursor.get()).getRealFloat(); 
					
					totalsMax[n] = totalsMax[n] + count; // calculate total count for normalization
					// System.out.println("IqmOpFracGendim: b: "+b+ "   count: "+ count );
					if (count > 0) {
						for (int q = 0; q < numQ; q++) {
							if ((q + minQ) == 1)
								totals[q][n] = totals[q][n] + count * Math.log(count); // GenDim
							else
								totals[q][n] = totals[q][n] + Math.pow(count, (q + minQ)); // GenDim
						}
					}
					
				}				
				//epsWidth = epsWidth * 2; // kernels larger than image size! Must be in accordance with getMaxBoxNumber()
				epsWidth = epsWidth + 2;
			} // 0>=l<=numEps loop through eps
		}	
		
		//Normalization
		for (int n = 0; n < numBoxes; n++) {
			for (int q = 0; q < numQ; q++) {
				totals[q][n] = totals[q][n]/ totalsMax[n];
			}
		}
		
		return totals;	
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

}
