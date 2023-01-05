/*-
 * #%L
 * Project: ImageJ2 plugin for computing the 3D Minkowski dimension.
 * File: Minkowski3D_Grey.java
 * 
 * $Id$
 * $HeadURL$
 * 
 * This file is part of ComsystanJ software, hereinafter referred to as "this program".
 * %%
 * Copyright (C) 2022 - 2023 Comsystan Software
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
package at.csa.csaj.plugin3d.frac.dim.minkowski.util;

import java.util.List;
import org.scijava.app.StatusService;
import org.scijava.ui.UIService;
import at.csa.csaj.commons.dialog.WaitingDialogWithProgressBar;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.morphology.Dilation;
import net.imglib2.algorithm.morphology.Erosion;
import net.imglib2.algorithm.morphology.StructuringElements;
import net.imglib2.algorithm.neighborhood.Shape;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;

/**	Minkowski dimension for 3D image volumes, grey and binary
  *
 * @author Helmut Ahammer
 * @since 2022-11-21  
 */

public class Minkowski3D_Grey implements Minkowski3DMethods{
	
	private static Img<FloatType> imgFloat; 
	private static Img<FloatType> imgU;
	private static Img<FloatType> imgB; 
	private static Img<FloatType> imgUplusOne;
	private static Img<FloatType> imgBminusOne; 
	private static Img<FloatType> imgUDil;
	private static Img<FloatType> imgBErode;
	private static Img<FloatType> imgV;
	private static Img<UnsignedByteType>imgUnsignedByte;
	private static RandomAccess<FloatType> raF1;
	private static RandomAccess<FloatType> raF2;
	private static RandomAccessibleInterval<FloatType> raiF;	
	private static Cursor<UnsignedByteType> cursorUBT = null;
	private static Cursor<FloatType> cursorF = null;
	private RandomAccessibleInterval<?> rai = null;
	private int numDilations = 0;
	private long width = 0;
	private long height = 0;
	private long depth = 0;
	private double[] totals = null;
	private double[] eps = null;
	private String shapeType;
	private String morphologicalType;
	private boolean showLastMorphImg;
	private WaitingDialogWithProgressBar dlgProgress;
	private StatusService statusService;
	
	private UIService uiService;
	
	@Override
	public double[] getTotals() {
		return totals;
	}

	@Override
	public void setTotals(double[] totals) {
		this.totals = totals;
	}

	@Override
	public double[] getEps() {
		return eps;
	}

	@Override
	public void setEps(double[] eps) {
		this.eps = eps;
	}


	/**
	 * This is the standard constructor
	 * 
	 * @param operator the {@link AbstractOperator} firing progress updates
	 */
	public Minkowski3D_Grey(RandomAccessibleInterval<?> rai, int numDilations, String shapeType, String morphologicalType, boolean showLastMorphImg, UIService uiService, WaitingDialogWithProgressBar dlgProgress, StatusService statusService) {
		this.rai               = rai;
		this.width             = rai.dimension(0);
		this.height            = rai.dimension(1);
		this.depth             = rai.dimension(2);
		this.numDilations      = numDilations;
		this.shapeType         = shapeType;
		this.morphologicalType = morphologicalType;
		this.showLastMorphImg  = showLastMorphImg;
		this.uiService         = uiService;
		this.dlgProgress       = dlgProgress;
		this.statusService     = statusService;
	}

	public Minkowski3D_Grey() {
	}

	/**
	 * This method calculates the 3D Box counting dimension
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
			
		double[] totals = new double[numDilations];
	
		
		percent = 1;
		dlgProgress.updatePercent(String.valueOf(percent+"%"));
		dlgProgress.updateBar(percent);
		//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
		statusService.showStatus(percent, 100, "Initializing finished");
	
		//********************************Binary Image: 0 and [1, 255]! and not: 0 and 255
		if (morphologicalType.equals("Binary dilation")) {//{"Binary dilation", "Blanket dilation/erosion", "Variation dilation/erosion"}
			//Minkowski
			//n=0  2^0 = 1 ... single pixel
			// Loop through all pixels.
			//get count(area) and copy image 
//				cursor = (Cursor<UnsignedByteType>) Views.iterable(rai).localizingCursor();	
//				while (cursor.hasNext()) {
//					cursor.fwd();
//					//cursor.localize(pos);			
//					if (((UnsignedByteType) cursor.get()).get() > 0) totals[0] += 1; // Binary Image: 0 and [1, 255]! and not: 0 and 255
//					//totals[n] = totals[n]; // / totalsMax;
//				}

			imgUnsignedByte = createImgUnsignedByte(rai); //This copies the image, otherwise the original image would be dilated
		
			List<Shape> strel = null;			
			if      (shapeType.equals("Square"))  strel = StructuringElements.square(1, 3); //3x3kernel, 3 dimensions
			else if (shapeType.equals("Disk"))    strel = StructuringElements.disk(1, 3);
			else if (shapeType.equals("Diamond")) strel = StructuringElements.diamond(1, 3);
					
			Runtime runtime  = Runtime.getRuntime();
			long maxMemory   = runtime.maxMemory();
			long totalMemory = runtime.totalMemory();
			long freeMemory  = runtime.freeMemory();
			int availableProcessors = runtime.availableProcessors();
			//System.out.println("available processors: " + availableProcessors);
			int numThreads = 6; //For dilation //with 6 it was 3 times faster than only with one thread
			if (numThreads > availableProcessors) numThreads = availableProcessors;
			long[] pos = new long[3];
			long[] min = new long[] {0, 0, 0};
			long[] max = new long[] {rai.dimension(0), rai.dimension(1), rai.dimension(2)};
			Interval interval = new FinalInterval(min, max);
			
			for (int n = 0; n < numDilations; n++) { //	
				percent = (int)Math.max(Math.round((((float)n)/((float)numDilations)*100.f)), percent);
				dlgProgress.updatePercent(String.valueOf(percent+"%"));
				dlgProgress.updateBar(percent);
				//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
				statusService.showStatus((n+1), numDilations, "Processing " + (n+1) + "/" + numDilations);
				
				//Compute dilated image
				Dilation.dilateInPlace(imgUnsignedByte, interval, strel, numThreads); //dilated image
						
				//uiService.show("" + (n+1) + "Dilated volume", imgUnsignedByte);
				if ((showLastMorphImg)&&(n == numDilations - 1)) uiService.show("Last dilated volume", imgUnsignedByte);
				
				cursorUBT = imgUnsignedByte.localizingCursor();	
				while (cursorUBT.hasNext()) {
					cursorUBT.fwd();
					//cursor.localize(pos);			
					if (((UnsignedByteType) cursorUBT.get()).get() > 0) totals[n] += 1; // Binary Image: 0 and [1, 255]! and not: 0 and 255
					//totals[n] = totals[n]; // / totalsMax;
				}		                      
			} //n
		}
		//NOTE: Grey value algorithms yield good estimates for low FD's e.g. 3D FFT FD = 3.1 
		//But too low values for high FDs e.g. 3D FFT FD=3.9 --> only about 3.4
		//*******************************Grey Value Image
		else if (morphologicalType.equals("Blanket dilation/erosion")) {// {"Binary dilation", "Blanket dilation/erosion", "Variation dilation/erosion"}
			imgFloat = createImgFloat(rai); //This copies the image, otherwise the original image would be dilated
			imgU = imgFloat.copy();
			imgB = imgFloat.copy();
			imgUplusOne  = imgFloat.copy();
			imgBminusOne = imgFloat.copy();
			imgV = imgFloat.copy();
			
			List<Shape> strel = null;			
			if      (shapeType.equals("Square"))  strel = StructuringElements.square(1, 3); //3x3kernel, 3 dimensions
			else if (shapeType.equals("Disk"))    strel = StructuringElements.disk(1, 3);
			else if (shapeType.equals("Diamond")) strel = StructuringElements.diamond(1, 3);
			
			Runtime runtime  = Runtime.getRuntime();
			long maxMemory   = runtime.maxMemory();
			long totalMemory = runtime.totalMemory();
			long freeMemory  = runtime.freeMemory();
			int availableProcessors = runtime.availableProcessors();
			//System.out.println("available processors: " + availableProcessors);
			int numThreads = 6; //For dilation //with 6 it was 3 times faster than only with one thread
			if (numThreads > availableProcessors) numThreads = availableProcessors;
			long[] pos = new long[3];
			float sample1;
			float sample2;

		
			for (int n = 0; n < numDilations; n++) { //
				percent = (int)Math.max(Math.round((((float)n)/((float)numDilations)*100.f)), percent);
				dlgProgress.updatePercent(String.valueOf(percent+"%"));
				dlgProgress.updateBar(percent);
				//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
				statusService.showStatus((n+1), numDilations, "Processing " + (n+1) + "/" + numDilations);
				
				//Add plus one to imgU
				cursorF = imgU.localizingCursor();
				raF1 = imgUplusOne.randomAccess();
				while (cursorF.hasNext()) {
					cursorF.fwd();	
					cursorF.localize(pos);
					raF1.setPosition(pos);
					raF1.get().set(cursorF.get().get() + 1f); 
				}			
				//Dilated imgU
				imgUDil = Dilation.dilate(imgU, strel, numThreads);
				//uiService.show("Dilated volume", imgUDil);
				if ((showLastMorphImg)&&(n == numDilations -1)) uiService.show("Last dilated volume", imgUDil);
				
				//Get Max and overwrite imgU
				cursorF = imgU.localizingCursor();
				raF1 = imgUplusOne.randomAccess();
				raF2 = imgUDil.randomAccess();
				while (cursorF.hasNext()) {
					cursorF.fwd();
					cursorF.localize(pos);
					raF1.setPosition(pos);
					raF2.setPosition(pos);
					sample1 = raF1.get().get();
					sample2 = raF2.get().get();
					if ((sample2 > sample1)) cursorF.get().set(sample2);
					else cursorF.get().set(sample1);
				}	
				
				//Subtract one to imgB
				cursorF = imgB.localizingCursor();
				raF1 = imgBminusOne.randomAccess();
				while (cursorF.hasNext()) {
					cursorF.fwd();	
					cursorF.localize(pos);
					raF1.setPosition(pos);
					raF1.get().set(cursorF.get().get() - 1f); 
				}			
				//Erode imgB
				imgBErode = Erosion.erode(imgB, strel, numThreads);
				//uiService.show("Eroded volume", imgBErode);
				if ((showLastMorphImg)&&(n == numDilations -1)) uiService.show("Last eroded volume", imgBErode);
				
				//Get Min and overwrite imgB
				cursorF = imgB.localizingCursor();
				raF1 = imgBminusOne.randomAccess();
				raF2 = imgBErode.randomAccess();
				while (cursorF.hasNext()) {
					cursorF.fwd();
					cursorF.localize(pos);
					raF1.setPosition(pos);
					raF2.setPosition(pos);
					sample1 = raF1.get().get();
					sample2 = raF2.get().get();
					if ((sample2 < sample1)) cursorF.get().set(sample2);
					else cursorF.get().set(sample1);
				}	
				
				//Compute volume imgV
				cursorF = imgV.localizingCursor();
				raF1 = imgU.randomAccess();
				raF2 = imgB.randomAccess();
				while (cursorF.hasNext()) {
					cursorF.fwd();
					cursorF.localize(pos);
					raF1.setPosition(pos);
					raF2.setPosition(pos);
					sample1 = raF1.get().get();
					sample2 = raF2.get().get();
					cursorF.get().set(sample1 - sample2);
				}	
				
				//Get counts with imgV
				cursorF = imgV.localizingCursor();
				raF1 = imgU.randomAccess();
				raF2 = imgB.randomAccess();
				while (cursorF.hasNext()) {
					cursorF.fwd();
					totals[n] = totals[n] + cursorF.get().get(); //totalAreas			
				}
				
				// totalAreas[ee] =
				// totalAreas[n] / (2* (n+1)); //Peleg et al.
				// better is following according to Dubuc et al. equation 9

				//totals[n] = totals[n] / ((n+ 1) * (n + 1) * (n + 1)); // eq.9 Dubuc et.al. for surfaces
				//Without this normalization Dim would be: Dim = 3-slope; for surfaces						                      
			} //n
		}
		//NOTE: Grey value algorithms yield good estimates for low FD's e.g. 3D FFT FD = 3.1 
		//But too low values for high FDs e.g. 3D FFT FD=3.9 --> only about 3.4
		else if (morphologicalType.equals("Variation dilation/erosion")) {// {"Binary dilation", "Blanket dilation/erosion", "Variation dilation/erosion"}
			imgFloat = createImgFloat(rai); //This copies the image, otherwise the original image would be dilated
			imgU         = imgFloat.copy();
			imgB         = imgFloat.copy();
			imgUDil      = imgFloat.copy();
			imgBErode    = imgFloat.copy();
			imgUplusOne  = imgFloat.copy();
			imgBminusOne = imgFloat.copy();
			imgV         = imgFloat.copy();
			
			List<Shape> strel = null;			
			if      (shapeType.equals("Square"))  strel = StructuringElements.square(1, 3); //3x3kernel, 3 dimensions
			else if (shapeType.equals("Disk"))    strel = StructuringElements.disk(1, 3);
			else if (shapeType.equals("Diamond")) strel = StructuringElements.diamond(1, 3);
			
			Runtime runtime  = Runtime.getRuntime();
			long maxMemory   = runtime.maxMemory();
			long totalMemory = runtime.totalMemory();
			long freeMemory  = runtime.freeMemory();
			int availableProcessors = runtime.availableProcessors();
			//System.out.println("available processors: " + availableProcessors);
			int numThreads = 6; //For dilation //with 6 it was 3 times faster than only with one thread
			if (numThreads > availableProcessors) numThreads = availableProcessors;
			long[] pos = new long[3];
			float sample1;
			float sample2;
		
			for (int n = 0; n < numDilations; n++) { //
				percent = (int)Math.max(Math.round((((float)n)/((float)numDilations)*100.f)), percent);
				dlgProgress.updatePercent(String.valueOf(percent+"%"));
				dlgProgress.updateBar(percent);
				//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
				statusService.showStatus((n+1), numDilations, "Processing " + (n+1) + "/" + numDilations);
				
				//Dilated imgU
				imgUDil = Dilation.dilate(imgU, strel, numThreads); //2D

				//uiService.show("Dilated volume", imgUDil);
				if ((showLastMorphImg)&&(n == numDilations -1)) uiService.show("Last dilated volume", imgUDil);
				
				//Overwrite imgU
				cursorF = imgU.localizingCursor();
				raF2 = imgUDil.randomAccess();
				while (cursorF.hasNext()) {
					cursorF.fwd();
					cursorF.localize(pos);
					raF2.setPosition(pos);
					sample2 = raF2.get().get();
					cursorF.get().set(sample2);
				}	
							
				//Erode imgB
				imgBErode = Erosion.erode(imgB, strel, numThreads);
			
				//uiService.show("Eroded image", imgBErode);
				if ((showLastMorphImg)&&(n == numDilations -1)) uiService.show("Last eroded volume", imgBErode);
				
				//Overwrite imgB
				cursorF = imgB.localizingCursor();
				raF2 = imgBErode.randomAccess();
				while (cursorF.hasNext()) {
					cursorF.fwd();
					cursorF.localize(pos);
					raF2.setPosition(pos);
					sample2 = raF2.get().get();
					cursorF.get().set(sample2);
				}	
				
				//Compute volume imgV
				cursorF = imgV.localizingCursor();
				raF1 = imgU.randomAccess();
				raF2 = imgB.randomAccess();
				while (cursorF.hasNext()) {
					cursorF.fwd();
					cursorF.localize(pos);
					raF1.setPosition(pos);
					raF2.setPosition(pos);
					sample1 = raF1.get().get();
					sample2 = raF2.get().get();
					cursorF.get().set(sample1 - sample2);
				}	
				
				//Get counts with imgV
				cursorF = imgV.localizingCursor();
				raF1 = imgU.randomAccess();
				raF2 = imgB.randomAccess();
				while (cursorF.hasNext()) {
					cursorF.fwd();
					
					totals[n] = totals[n] + cursorF.get().get(); //totalAreas
					
				}	
				//totals[n] = totals[n] / ((n+ 1) * (n + 1) * (n + 1)); // eq.17 Dubuc et.al.		
				//Without this normalization Dim would be: Dim = 3-slope; for surfaces
			} //n
		}
	
		return totals;
	}
	
	/**
	 * This method calculates the dilations
	 * @return eps
	 */
	@Override
	public double[] calcEps() {

		eps = new double[numDilations];
		for (int n = 0; n < numDilations; n++) {
			eps[n] = n + 1;
		}
		return eps;
	}	
	
	/**
	 * 
	 * This methods creates an Img<UnsignedByteType>
	 */
	private Img<UnsignedByteType > createImgUnsignedByte(RandomAccessibleInterval<?> rai){ //rai must always be a single 3D
		
		imgUnsignedByte = new ArrayImgFactory<>(new UnsignedByteType()).create(rai.dimension(0), rai.dimension(1), rai.dimension(2)); //always single 3D
		Cursor<UnsignedByteType> cursor = imgUnsignedByte.localizingCursor();
		final long[] pos = new long[imgUnsignedByte.numDimensions()];
		RandomAccess<RealType<?>> ra = (RandomAccess<RealType<?>>) rai.randomAccess();
		while (cursor.hasNext()){
			cursor.fwd();
			cursor.localize(pos);
			ra.setPosition(pos);
			//	ra.setPosition(pos[0], 0);
			//	ra.setPosition(pos[1], 1);
			//  ra.setPosition(pos[2], 2);
			//ra.get().setReal(cursor.get().get());
			cursor.get().setReal(ra.get().getRealFloat());
		}
		
		return imgUnsignedByte;
	}
	
	/**
	 * 
	 * This methods creates a Img<FloatType>
	 */
	private Img<FloatType > createImgFloat(RandomAccessibleInterval<?> rai){ //rai must always be a single 3D
		
		imgFloat = new ArrayImgFactory<>(new FloatType()).create(rai.dimension(0), rai.dimension(1), rai.dimension(2)); //always single 3D
		Cursor<FloatType> cursor = imgFloat.localizingCursor();
		final long[] pos = new long[imgFloat.numDimensions()];
		RandomAccess<RealType<?>> ra = (RandomAccess<RealType<?>>) rai.randomAccess();
		while (cursor.hasNext()){
			cursor.fwd();
			cursor.localize(pos);
			ra.setPosition(pos);
			//	ra.setPosition(pos[0], 0);
			//	ra.setPosition(pos[1], 1);
			//  ra.setPosition(pos[2], 2);
			//ra.get().setReal(cursor.get().get());
			cursor.get().setReal(ra.get().getRealFloat());
		}
		
		return imgFloat;
	}

}
