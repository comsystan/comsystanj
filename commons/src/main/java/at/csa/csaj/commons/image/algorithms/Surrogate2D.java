/*-
 * #%L
 * Project: Commons for ComsystanJ ImageJ Plugins
 * File: Surrogate2D.java
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
package at.csa.csaj.commons.image.algorithms;


import java.util.Random;
import java.util.Vector;
import org.apache.commons.math3.stat.ranking.NaNStrategy;
import org.apache.commons.math3.stat.ranking.NaturalRanking;
import org.apache.commons.math3.stat.ranking.TiesStrategy;

import at.csa.csaj.commons.signal.algorithms.Surrogate;
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_2D;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.Views;



/**
 * This class calculates surrogate data
 * Options: Shuffle, Gaussian, Phase randomized, AAFT, Pseudo-Periodic, Multivariate
 * see e.g. Mark Shelhammer, Nonlinear Dynamics in Physiology, World Scientific 2007
 * 
 * RGB  3D rai only partly implemented
 * 
 * @author Helmut Ahammer
 * @since   2022 02
 */
public class Surrogate2D {

	public final static int SURROGATE_SHUFFLE         = 0;
	public final static int SURROGATE_GAUSSIAN        = 1;
	public final static int SURROGATE_RANDOMPHASE     = 2;
	public final static int SURROGATE_AAFT            = 3;
	public final static int SURROGATE_PSEUDOPERIODIC  = 4;
	public final static int SURROGATE_MULTIVARIATE    = 5;
	
	private static RandomAccessibleInterval<DoubleType>  raiWindowed; 
	Cursor<?> cursor;

	/**
	 * This is the standard constructor
	 */
	public Surrogate2D(){
		
	}
	
	/**
	 * This method calculates the minimum and maximum of a rai
	 * @param rai
	 * @return double[] minmax
	 */
	public double[] calcMinMax(RandomAccessibleInterval<?> rai){ //2D rai
		double[] minmax = new double[2];
		double pixelVal;
		minmax[0] =  Double.MAX_VALUE;
		minmax[1] = -Double.MAX_VALUE;
		cursor = Views.iterable(rai).localizingCursor();
		//pos = new long[2];
		while (cursor.hasNext()) {
			cursor.fwd();
			//cursor.localize(pos); 
			pixelVal = ((UnsignedByteType) cursor.get()).get();
			if (pixelVal < minmax[0]) minmax[0] = pixelVal; //min
			if (pixelVal > minmax[1]) minmax[1] = pixelVal; //max
		}
		return minmax;
	}
	
	

	/**
	 * This method calculates a surrogate rai using the shuffle method
	 * The image is randomly shuffled
	 * @param  rai
	 * @return surrogate rai
	 */
	public RandomAccessibleInterval calcSurrogateShuffle(RandomAccessibleInterval rai) { //2D rai grey or §D rai for RGB	
		
		int numDim = rai.numDimensions();
		
		Random random = new Random();
		random.setSeed(System.currentTimeMillis());
		
		Vector<Integer> imageVec;
		int index;
		RandomAccessibleInterval raiSlice = null;	
		
		if (numDim == 2) { //2D grey
			cursor = Views.iterable(rai).localizingCursor();
			imageVec = new Vector<Integer>();
			while (cursor.hasNext()) {
				cursor.fwd();
				//cursor.localize(pos);	
				imageVec.add((int)((UnsignedByteType) cursor.get()).get()); //copy image data to the vector
			} //cursor
			
			cursor.reset();		
			while (cursor.hasNext()) {
				cursor.fwd();
				//cursor.localize(pos);	
				index = random.nextInt(imageVec.size()); //get random index
				((UnsignedByteType) cursor.get()).set(imageVec.get(index)); //set
				imageVec.removeElementAt(index);
			} //cursor
			imageVec.clear();
			imageVec = null;
		}
		else if (numDim == 3) { //3D RGB is working
			//Does the same, but only on one sclice of the rai 
			for (int b = 0; b < numDim; b++) {
				raiSlice = (RandomAccessibleInterval) Views.hyperSlice(rai, 2, b);
		
				cursor = Views.iterable(raiSlice).localizingCursor();	
				imageVec = new Vector<Integer>();
				while (cursor.hasNext()) {
					cursor.fwd();
					//cursor.localize(pos);	
					imageVec.add((int)((UnsignedByteType) cursor.get()).get()); //copy image data to the vector
				} //cursor
				
				cursor.reset();		
				while (cursor.hasNext()) {
					cursor.fwd();
					//cursor.localize(pos);	
					index = random.nextInt(imageVec.size()); //get random index
					((UnsignedByteType) cursor.get()).set(imageVec.get(index)); //set
					imageVec.removeElementAt(index);
				} //cursor
				imageVec.clear();
				imageVec = null;
			} //b
		}; //RGB	
		
		return rai;
	}
	
	/**
	 * This method calculates a surrogate image using the Gaussian method
	 * A Gaussian image with identical mean and standard deviation as the original image is constructed 
	 * @param  rai
	 * @return surrogate rai
	 */
	public RandomAccessibleInterval calcSurrogateGaussian(RandomAccessibleInterval rai) { //2D rai grey or §D rai for RGB	
		
		int numDim  = rai.numDimensions();
		long width  = rai.dimension(0);
		long height = rai.dimension(1);
		
		Random random = new Random();
		random.setSeed(System.currentTimeMillis());
		
		double mean   = 0.0;
		double stdDev = 0.0;
		double pixelVal;
		
		RandomAccessibleInterval raiSlice = null;	
		
		if (numDim == 2) { //2D grey
			cursor = Views.iterable(rai).localizingCursor();
			while (cursor.hasNext()) {
				cursor.fwd();
				//cursor.localize(pos);	
				mean = mean + ((UnsignedByteType) cursor.get()).get();
			} //cursor
			mean = mean/(width*height);
			System.out.println( "Mean " + mean);
		
			cursor.reset();		
			while (cursor.hasNext()) {
				cursor.fwd();
				//cursor.localize(pos);
				pixelVal = ((UnsignedByteType) cursor.get()).get();
				stdDev = stdDev + ((pixelVal - mean)*(pixelVal - mean));			
			} //cursor
			stdDev = stdDev/(width*height);
			stdDev = Math.sqrt(stdDev);
			System.out.println("SD " + stdDev);
			
			cursor.reset();		
			while (cursor.hasNext()) {
				cursor.fwd();
				//cursor.localize(pos);
				pixelVal = Math.round(random.nextGaussian()*stdDev + mean);		
				while ((pixelVal < 0) || (pixelVal > 255)) {// values outside of [0,255] not allowed 
					pixelVal = Math.round(random.nextGaussian()*stdDev + mean);
				}
				((UnsignedByteType) cursor.get()).set((int)pixelVal); //set					
			} //cursor		
		}
		else if (numDim == 3) { //3D RGB is working
			//Does the same, but only on one sclice of the rai 
			for (int b = 0; b < numDim; b++) {
				raiSlice = (RandomAccessibleInterval) Views.hyperSlice(rai, 2, b);
				cursor = Views.iterable(raiSlice).localizingCursor();
				while (cursor.hasNext()) {
					cursor.fwd();
					//cursor.localize(pos);	
					mean = mean + ((UnsignedByteType) cursor.get()).get();
				} //cursor
				mean = mean/(width*height);
				System.out.println("Slice " + b + "   Mean " + mean);
			
				cursor.reset();		
				while (cursor.hasNext()) {
					cursor.fwd();
					//cursor.localize(pos);
					pixelVal = ((UnsignedByteType) cursor.get()).get();
					stdDev = stdDev + ((pixelVal - mean)*(pixelVal - mean));			
				} //cursor
				stdDev = stdDev/(width*height);
				stdDev = Math.sqrt(stdDev);
				System.out.println("Slice " + b + "   SD " + stdDev);
				
				cursor.reset();		
				while (cursor.hasNext()) {
					cursor.fwd();
					//cursor.localize(pos);
					pixelVal = Math.round(random.nextGaussian()*stdDev + mean);		
					while ((pixelVal < 0) || (pixelVal > 255)) {// values outside of [0,255] not allowed 
						pixelVal = Math.round(random.nextGaussian()*stdDev + mean);
					}
					((UnsignedByteType) cursor.get()).set((int)pixelVal); //set					
				} //cursor			
			} //b
		}; //RGB	
		
		return rai;
	}
	
	/**
	 * This method calculates a surrogate rai using the Random phase  method
	 * FFT transformation, randomized phases, inverse transformation
	 * @param rai
	 * @param windowing type
	 * @return surrogate rai
	 */
	public RandomAccessibleInterval calcSurrogateRandomPhase(RandomAccessibleInterval rai, String windowingType) { //2D rai grey or 3D rai for RGB	
		
		int numDim = rai.numDimensions();
	 	int width  = (int)rai.dimension(0);
    	int height = (int)rai.dimension(1);
    
    	long[] pos;
    	double[][] imgA;
    	
    	double[] minmaxOrig;
    	double[] minmax;
    	double pixelVal;
    	
    	// generate random values
		Random random = new Random();
		random.setSeed(System.currentTimeMillis());
   
		RandomAccessibleInterval raiSlice = null;	
		Cursor<?> cursor;
			
		if (numDim == 2) { //2D grey
			
			minmaxOrig = this.calcMinMax(rai);
			
			//In the order of increasing filter strength
			if (windowingType.equals("Rectangular")) {
				raiWindowed = windowingRectangular(rai);
			}
			else if (windowingType.equals("Bartlett")) {
				raiWindowed = windowingBartlett(rai);
			}
			else if (windowingType.equals("Hamming")) {
				raiWindowed = windowingHamming(rai);
			}
			else if (windowingType.equals("Hanning")) {
				raiWindowed = windowingHanning(rai);
			}
			else if (windowingType.equals("Blackman")) {
				raiWindowed = windowingBlackman(rai);
			}
			else if (windowingType.equals("Gaussian")) {
				raiWindowed = windowingGaussian(rai);
			}
			else if (windowingType.equals("Parzen")) {
				raiWindowed = windowingParzen(rai);
			}
			
//			ops filter fft seems to be a Hadamard transform rather than a true FFT
//			output size is automatically padded, so has rather strange dimensions.
//			output is vertically symmetric 
//			F= 0 is at (0.0) and (0,SizeY)
//			imgFloat = this.createImgFloat(raiWindowed);
//			RandomAccessibleInterval<C> raifft = opService.filter().fft(imgFloat);
//			
//			//This would also work with identical output 
//			ImgFactory<ComplexFloatType> factory = new ArrayImgFactory<ComplexFloatType>(new ComplexFloatType());
//			int numThreads = 6;
//			final FFT FFT = new FFT();
//			Img<ComplexFloatType> imgCmplx = FFT.realToComplex((RandomAccessibleInterval<R>) raiWindowed, factory, numThreads);

			//Using JTransform package
			//https://github.com/wendykierp/JTransforms
			//https://wendykierp.github.io/JTransforms/apidocs/
			//The sizes of both dimensions must be power of two.
			int dftWidth = 2; 
			int dftHeight = 2;
			while (dftWidth < width) {
				dftWidth = dftWidth * 2;
			}
			while (dftHeight < height) {
				dftHeight = dftHeight * 2;
			}
					
			//JTransform needs rows and columns swapped!!!!!
			int rows    = dftHeight;
			int columns = dftWidth;
			
			
			//JTransform needs rows and columns swapped!!!!!
			imgA = new double[rows][2*columns]; //Every frequency entry needs a pair of columns: for real and imaginary part
			cursor = Views.iterable(raiWindowed).localizingCursor();
			pos = new long[2];
			while (cursor.hasNext()) {
				cursor.fwd();
				cursor.localize(pos); 
				//JTransform needs rows and columns swapped!!!!!
				imgA[(int)pos[1]][(int)pos[0]] = ((DoubleType) cursor.get()).get();
			}
			
			//JTransform needs rows and columns swapped!!!!!
			DoubleFFT_2D FFT = new DoubleFFT_2D(rows, columns); //Here always the simple DFT width
			//dFFT.realForward(imgArrD);   //The first two columns are not symmetric and seem to be not right
			FFT.realForwardFull(imgA);   //The right part is not symmetric!!
			//Power image constructed later is also not exactly symmetric!!!!!
			
			
			//Forward FFT of imgA
			//is not really necessary because imgA will be overwritten after Forward FFT
//			cursorF = imgFloat.localizingCursor();
//			pos = new long[2];
//			while (cursorF.hasNext()) {
//				cursorF.fwd();
//				cursorF.localize(pos); 
//				//JTransform needs rows and columns swapped!!!!!
//				imgA[(int)pos[1]][(int)(pos[0])] = cursorF.get().get();
//			}
//			
//			//JTransform needs rows and columns swapped!!!!!
//			////FFT.realForward(imgA);  //The first two columns are not symmetric and seem to be not right
//			FFT.realForwardFull(imgA);  //The right part is not symmetric!!
//			////Power image constructed later is also not exactly symmetric!!!!!
			
			
			//Optionally show FFT Real Imag image
			//************************************************************************************
//			ArrayImg<FloatType, ?> imgFFT = new ArrayImgFactory<>(new FloatType()).create(2*dftWidth, dftHeight); //always single 2D
//			cursorF = imgFFT.localizingCursor();
//			pos = new long[2];
//			while (cursorF.hasNext()){
//				cursorF.fwd();
//				cursorF.localize(pos);
//				//JTransform needs rows and columns swapped!!!!!
//				cursorF.get().set((float)imgA[(int)pos[1]][(int)pos[0]]);
//			}		
//			//Get min max
//			float min = Float.MAX_VALUE;
//			float max = -Float.MAX_VALUE;
//			float valF;
//			cursorF = imgFFT.cursor();
//			while (cursorF.hasNext()) {
//				cursorF.fwd();
//				valF = cursorF.get().get();
//				if (valF > max) max = valF;
//				if (valF < min) min = valF;
//			}	
//			//Rescale to 0...255
//			cursorF = imgFFT.cursor();
//			while (cursorF.hasNext()) {
//				cursorF.fwd();
//				cursorF.localize(pos);
//				cursorF.get().set(255f*(cursorF.get().get() - min)/(max - min));		
//			}	
//			uiService.show("FFT", imgFFT);	
			//************************************************************************************
		
			double mag;
					
			//set FFT real and imaginary values
			for (int k1 = 0; k1 < rows/2; k1++) {
				for (int k2 = 0; k2 < columns/2; k2++) {
					mag = Math.sqrt(imgA[k1][2*k2]*imgA[k1][2*k2] + imgA[k1][2*k2+1]*imgA[k1][2*k2+1]); //Magnitude	//(2*x)...Real parts   (2*x+1).... Imaginary parts					
					imgA[k1][2*k2]   = mag * Math.cos(2 * Math.PI * random.nextDouble()); //(2*x)  ...Real part
					imgA[k1][2*k2+1] = mag * Math.sin(2 * Math.PI * random.nextDouble()); //(2*x+1)...Imaginary part 
				}
			}		
			for (int k1 = rows/2; k1 < rows; k1++) {
				for (int k2 = 0; k2 < columns/2; k2++) {
					mag = Math.sqrt(imgA[k1][2*k2]*imgA[k1][2*k2] + imgA[k1][2*k2+1]*imgA[k1][2*k2+1]); //Magnitude	//(2*x)...Real parts   (2*x+1).... Imaginary parts					
					imgA[k1][2*k2]   = mag * Math.cos(2 * Math.PI * random.nextDouble()); //(2*x)  ...Real part
					imgA[k1][2*k2+1] = mag * Math.sin(2 * Math.PI * random.nextDouble()); //(2*x+1)...Imaginary part 

				}
			}	
			for (int k1 = 0; k1 < rows/2; k1++) {
				for (int k2 = columns/2; k2 < columns; k2++) {
					mag = Math.sqrt(imgA[k1][2*k2]*imgA[k1][2*k2] + imgA[k1][2*k2+1]*imgA[k1][2*k2+1]); //Magnitude	//(2*x)...Real parts   (2*x+1).... Imaginary parts					
					imgA[k1][2*k2]   = mag * Math.cos(2 * Math.PI * random.nextDouble()); //(2*x)  ...Real part
					imgA[k1][2*k2+1] = mag * Math.sin(2 * Math.PI * random.nextDouble()); //(2*x+1)...Imaginary part 

				}
			}
			for (int k1 = rows/2; k1 < rows; k1++) {
				for (int k2 = columns/2; k2 < columns; k2++) {
					mag = Math.sqrt(imgA[k1][2*k2]*imgA[k1][2*k2] + imgA[k1][2*k2+1]*imgA[k1][2*k2+1]); //Magnitude	//(2*x)...Real parts   (2*x+1).... Imaginary parts					
					imgA[k1][2*k2]   = mag * Math.cos(2 * Math.PI * random.nextDouble()); //(2*x)  ...Real part
					imgA[k1][2*k2+1] = mag * Math.sin(2 * Math.PI * random.nextDouble()); //(2*x+1)...Imaginary part 

				}
			}
			
			//uiService.show("img", img);
			//uiService.show("fft", fft);
			
			//Inverse FFT and show image
			//imgA is now really complex, Real and Imaginary pairs
			FFT.complexInverse(imgA, false);
			
			//minmax
			minmax = new double[2];
			minmax[0] =  Double.MAX_VALUE;
			minmax[1] = -Double.MAX_VALUE;
			cursor = Views.iterable(rai).localizingCursor();
			pos = new long[2];
			while (cursor.hasNext()) {
				cursor.fwd();
				cursor.localize(pos); 
				//JTransform needs rows and columns swapped!!!!!
				pixelVal = imgA[(int)pos[1]][(int)(2*pos[0])];
				if (pixelVal < minmax[0]) minmax[0] = pixelVal;
				if (pixelVal > minmax[1]) minmax[1] = pixelVal;
			}
			
			cursor = Views.iterable(rai).localizingCursor();
			pos = new long[2];
			double rf = (minmaxOrig[1]-minmaxOrig[0])/(minmax[1]-minmax[0]); //rescale factor
			while (cursor.hasNext()) {
				cursor.fwd();
				cursor.localize(pos); 
				//JTransform needs rows and columns swapped!!!!!
				((UnsignedByteType) cursor.get()).set((int)Math.round(minmaxOrig[0] + rf*(imgA[(int)pos[1]][(int)(2*pos[0])]-minmax[0])));	//Rescale to 0  255 and set
			}
			
			//uiService.show("imgFloat after Inverse FFT", imgFloat);	
			
//			//Change from FloatType to UnsignedByteType
//			//Find min and max values
//			cursorF = imgFloat.cursor();
//			float min = Float.MAX_VALUE;
//			float max = -Float.MAX_VALUE;
//			float value;
//			while (cursorF.hasNext()){
//				cursorF.fwd();
//				value =  cursorF.get().getRealFloat();
//				if (value > max) max = value;
//				if (value < min) min = value;
//			}
//			
//			resultImg = opService.create().img(imgFloat, new UnsignedByteType());
//			RandomAccess<UnsignedByteType> ra = resultImg.randomAccess();
//			cursorF = imgFloat.cursor();
//	    	pos = new long[resultImg.numDimensions()];
//	    	float rf = (greyValueMax/(max-min)); //rescale factor
//			while (cursorF.hasNext()) {
//				cursorF.fwd();
//				cursorF.localize(pos);
//				value= cursorF.get().getRealFloat();
//				value = rf * (value - min); //Rescale to 0  255
//				ra.setPosition(pos);
//				ra.get().set((int)(Math.round(value)));	
//			}
//			//resultImg;
				
		} //numDim == 2
		else if (numDim == 3) { //3D RGB
			//Does the same, but only on one sclice of the rai 
			for (int b = 0; b < numDim; b++) {
				raiSlice = (RandomAccessibleInterval) Views.hyperSlice(rai, 2, b);
			
				minmaxOrig = this.calcMinMax(raiSlice);
				
				//In the order of increasing filter strength
				if (windowingType.equals("Rectangular")) {
					raiWindowed = windowingRectangular(raiSlice);
				}
				else if (windowingType.equals("Bartlett")) {
					raiWindowed = windowingBartlett(raiSlice);
				}
				else if (windowingType.equals("Hamming")) {
					raiWindowed = windowingHamming(raiSlice);
				}
				else if (windowingType.equals("Hanning")) {
					raiWindowed = windowingHanning(raiSlice);
				}
				else if (windowingType.equals("Blackman")) {
					raiWindowed = windowingBlackman(raiSlice);
				}
				else if (windowingType.equals("Gaussian")) {
					raiWindowed = windowingGaussian(raiSlice);
				}
				else if (windowingType.equals("Parzen")) {
					raiWindowed = windowingParzen(raiSlice);
				}
				
				//Using JTransform package
				//https://github.com/wendykierp/JTransforms
				//https://wendykierp.github.io/JTransforms/apidocs/
				//The sizes of both dimensions must be power of two.
				int dftWidth = 2; 
				int dftHeight = 2;
				while (dftWidth < width) {
					dftWidth = dftWidth * 2;
				}
				while (dftHeight < height) {
					dftHeight = dftHeight * 2;
				}
						
				//JTransform needs rows and columns swapped!!!!!
				int rows    = dftHeight;
				int columns = dftWidth;
				
				
				//JTransform needs rows and columns swapped!!!!!
				imgA = new double[rows][2*columns]; //Every frequency entry needs a pair of columns: for real and imaginary part
				cursor = Views.iterable(raiWindowed).localizingCursor();
				pos = new long[2];
				while (cursor.hasNext()) {
					cursor.fwd();
					cursor.localize(pos); 
					//JTransform needs rows and columns swapped!!!!!
					imgA[(int)pos[1]][(int)pos[0]] = ((DoubleType) cursor.get()).get();
				}
				
				//JTransform needs rows and columns swapped!!!!!
				DoubleFFT_2D FFT = new DoubleFFT_2D(rows, columns); //Here always the simple DFT width
				//dFFT.realForward(imgArrD);   //The first two columns are not symmetric and seem to be not right
				FFT.realForwardFull(imgA);   //The right part is not symmetric!!
				//Power image constructed later is also not exactly symmetric!!!!!
				
				double mag;
						
				//set FFT real and imaginary values
				for (int k1 = 0; k1 < rows/2; k1++) {
					for (int k2 = 0; k2 < columns/2; k2++) {
						mag = Math.sqrt(imgA[k1][2*k2]*imgA[k1][2*k2] + imgA[k1][2*k2+1]*imgA[k1][2*k2+1]); //Magnitude	//(2*x)...Real parts   (2*x+1).... Imaginary parts					
						imgA[k1][2*k2]   = mag * Math.cos(2 * Math.PI * random.nextDouble()); //(2*x)  ...Real part
						imgA[k1][2*k2+1] = mag * Math.sin(2 * Math.PI * random.nextDouble()); //(2*x+1)...Imaginary part 
					}
				}		
				for (int k1 = rows/2; k1 < rows; k1++) {
					for (int k2 = 0; k2 < columns/2; k2++) {
						mag = Math.sqrt(imgA[k1][2*k2]*imgA[k1][2*k2] + imgA[k1][2*k2+1]*imgA[k1][2*k2+1]); //Magnitude	//(2*x)...Real parts   (2*x+1).... Imaginary parts					
						imgA[k1][2*k2]   = mag * Math.cos(2 * Math.PI * random.nextDouble()); //(2*x)  ...Real part
						imgA[k1][2*k2+1] = mag * Math.sin(2 * Math.PI * random.nextDouble()); //(2*x+1)...Imaginary part 

					}
				}	
				for (int k1 = 0; k1 < rows/2; k1++) {
					for (int k2 = columns/2; k2 < columns; k2++) {
						mag = Math.sqrt(imgA[k1][2*k2]*imgA[k1][2*k2] + imgA[k1][2*k2+1]*imgA[k1][2*k2+1]); //Magnitude	//(2*x)...Real parts   (2*x+1).... Imaginary parts					
						imgA[k1][2*k2]   = mag * Math.cos(2 * Math.PI * random.nextDouble()); //(2*x)  ...Real part
						imgA[k1][2*k2+1] = mag * Math.sin(2 * Math.PI * random.nextDouble()); //(2*x+1)...Imaginary part 

					}
				}
				for (int k1 = rows/2; k1 < rows; k1++) {
					for (int k2 = columns/2; k2 < columns; k2++) {
						mag = Math.sqrt(imgA[k1][2*k2]*imgA[k1][2*k2] + imgA[k1][2*k2+1]*imgA[k1][2*k2+1]); //Magnitude	//(2*x)...Real parts   (2*x+1).... Imaginary parts					
						imgA[k1][2*k2]   = mag * Math.cos(2 * Math.PI * random.nextDouble()); //(2*x)  ...Real part
						imgA[k1][2*k2+1] = mag * Math.sin(2 * Math.PI * random.nextDouble()); //(2*x+1)...Imaginary part 

					}
				}
				
				//uiService.show("img", img);
				//uiService.show("fft", fft);
				
				//Inverse FFT and show image
				//imgA is now really complex, Real and Imaginary pairs
				FFT.complexInverse(imgA, false);
				
				//minmax
				minmax = new double[2];
				minmax[0] =  Double.MAX_VALUE;
				minmax[1] = -Double.MAX_VALUE;
				cursor = Views.iterable(raiSlice).localizingCursor();
				pos = new long[2];
				while (cursor.hasNext()) {
					cursor.fwd();
					cursor.localize(pos); 
					//JTransform needs rows and columns swapped!!!!!
					pixelVal = imgA[(int)pos[1]][(int)(2*pos[0])];
					if (pixelVal < minmax[0]) minmax[0] = pixelVal;
					if (pixelVal > minmax[1]) minmax[1] = pixelVal;
				}
				
				cursor = Views.iterable(raiSlice).localizingCursor();
				pos = new long[2];
				double rf = (minmaxOrig[1]-minmaxOrig[0])/(minmax[1]-minmax[0]); //rescale factor
				while (cursor.hasNext()) {
					cursor.fwd();
					cursor.localize(pos); 
					//JTransform needs rows and columns swapped!!!!!
					((UnsignedByteType) cursor.get()).set((int)Math.round(minmaxOrig[0] + rf*(imgA[(int)pos[1]][(int)(2*pos[0])]-minmax[0])));	//Rescale to 0  255 and set
				}
				
			} //b
		}; //RGB	
		
		return rai;
	}

	/**
	/**
	 * This method calculates a surrogate image using the AAFT (amplitude adjusted FT) method
	 * A Gaussian signal y is constructed
	 * y is ranked according to the original image
	 * then y is FFT converted, phase randomized and inverse FFT back converted yielding y'
	 * the original image is ranked according to y'
	 * @param rai
	 * @param windowing type
	 * @return surrogate rai
	 */
	public RandomAccessibleInterval calcSurrogateAAFT(RandomAccessibleInterval rai, String windowingType) { //2D rai grey or 3D rai for RGB	
		
		int numDim = rai.numDimensions();
	 	int width  = (int)rai.dimension(0);
    	int height = (int)rai.dimension(1);
    
    	long[] pos;
   
    	//for converting image into 1D list
    	int length1D = width*height;
    	double[] image1D     = new double[length1D];
		double[] surrogate1D = new double[length1D];
    	
		Surrogate surrogate;
		RandomAccess<UnsignedByteType> ra;
		RandomAccessibleInterval raiSlice = null;	
		Cursor<?> cursor;
			
		if (numDim == 2) { //2D grey
			
			//No windowing here
			//Windowing is done for the 1D sequence
		
			//Convert rai to 1D list 	
			ra = rai.randomAccess();
			pos = new long[2];
			int l = 0;
			for (int x = 0; x < width; x++) {
				for (int y  =0; y < height; y++) {
					pos[0] = x;
					pos[1] = y;
					ra.setPosition(pos);
					image1D[l] = (double)((UnsignedByteType) ra.get()).get();
					l=l+1;
				}
			}
					
			//calculate rank of input signal
			NaturalRanking ranking = new NaturalRanking(NaNStrategy.REMOVED, TiesStrategy.SEQUENTIAL);
	        double[] rankOfSignal = ranking.rank(image1D);
		
	        //Calculate Gaussian signal
	    	Random random = new Random();
			random.setSeed(System.currentTimeMillis());;
			double[] gauss = new double[length1D];
			for (int i = 0; i < length1D; i++){
				gauss[i] = random.nextGaussian();	
			}
			
			//Rank Gaussian signal according to input signal
			double[] gaussRank = new double[length1D];
			for (int i = 0; i < length1D; i++){
				gaussRank[i] = gauss[(int) rankOfSignal[i]-1];
			}
			
	        //calculate phase randomized signal of ranked Gaussian
			//this call fires also the progress bar events
			surrogate = new Surrogate(); //Signal surrogate
	        double[] gaussPhaseRandom = surrogate.calcSurrogateRandomPhase(gaussRank, windowingType);
		
	        //calculate rank of Gaussian (Ranked) phase randomized
			ranking = new NaturalRanking(NaNStrategy.REMOVED, TiesStrategy.SEQUENTIAL);
	        double[] rankOfGaussPhaseRandom = ranking.rank(gaussPhaseRandom);
		  
	        //Rank input signal according to Gaussian (Ranked) phase randomized
			for (int i = 0; i < length1D; i++){
				surrogate1D[i] = image1D[(int)  rankOfGaussPhaseRandom[i]-1];
			}
			
			//Re-convert 1D surrogate to 2D image	
			ra = rai.randomAccess();
			pos = new long[2];
			for (int x = 0; x < width; x++) {
				for (int y = 0; y < height; y++) {
					pos[0] = x;
					pos[1] = y;
					ra.setPosition(pos);
					ra.get().set((int)Math.round(surrogate1D[y*width + x]));
					//ra.get().set(240); //only for testing
				}
			}
			
			
		}
		else if (numDim == 3) { //3D RGB
			//Does the same, but only on one sclice of the rai 
			for (int b = 0; b < numDim; b++) {
				raiSlice = (RandomAccessibleInterval) Views.hyperSlice(rai, 2, b);
				
				//No windowing here
				//Windowing is done for the 1D sequence
			
				//Convert rai to 1D list 	
				ra = raiSlice.randomAccess();
				pos = new long[2];
				int l = 0;
				for (int x = 0; x < width; x++) {
					for (int y  =0; y < height; y++) {
						pos[0] = x;
						pos[1] = y;
						ra.setPosition(pos);
						image1D[l] = (double)((UnsignedByteType) ra.get()).get();
						l=l+1;
					}
				}
						
				//calculate rank of input signal
				NaturalRanking ranking = new NaturalRanking(NaNStrategy.REMOVED, TiesStrategy.SEQUENTIAL);
		        double[] rankOfSignal = ranking.rank(image1D);
			
		        //Calculate Gaussian signal
		    	Random random = new Random();
				random.setSeed(System.currentTimeMillis());;
				double[] gauss = new double[length1D];
				for (int i = 0; i < length1D; i++){
					gauss[i] = random.nextGaussian();	
				}
				
				//Rank Gaussian signal according to input signal
				double[] gaussRank = new double[length1D];
				for (int i = 0; i < length1D; i++){
					gaussRank[i] = gauss[(int) rankOfSignal[i]-1];
				}
				
		        //calculate phase randomized signal of ranked Gaussian
				//this call fires also the progress bar events
				surrogate = new Surrogate(); //Signal surrogate
		        double[] gaussPhaseRandom = surrogate.calcSurrogateRandomPhase(gaussRank, windowingType);
			
		        //calculate rank of Gaussian (Ranked) phase randomized
				ranking = new NaturalRanking(NaNStrategy.REMOVED, TiesStrategy.SEQUENTIAL);
		        double[] rankOfGaussPhaseRandom = ranking.rank(gaussPhaseRandom);
			  
		        //Rank input signal according to Gaussian (Ranked) phase randomized
				for (int i = 0; i < length1D; i++){
					surrogate1D[i] = image1D[(int)  rankOfGaussPhaseRandom[i]-1];
				}
				
				//Re-convert 1D surrogate to 2D image	
				ra = raiSlice.randomAccess();
				pos = new long[2];
				for (int x = 0; x < width; x++) {
					for (int y = 0; y < height; y++) {
						pos[0] = x;
						pos[1] = y;
						ra.setPosition(pos);
						ra.get().set((int)Math.round(surrogate1D[y*width + x]));
						//ra.get().set(240); //only for testing
					}
				}			
			} //b
		}; //RGB	
		
		return rai;
	}
	
	/**
	 * This method computes if a number is a power of 2
	 * 
	 * @param number
	 * @return
	 */
	public boolean isPowerOfTwo(int number) {
	    if (number % 2 != 0) {
	      return false;
	    } else {
	      for (int i = 0; i <= number; i++) {
	        if (Math.pow(2, i) == number) return true;
	      }
	    }
	    return false;
	 }
	
	/**
	 * This method increases the size of a signal to the next power of 2 
	 * 
	 * @param signal
	 * @return
	 */
	public double[] addZerosUntilPowerOfTwo (double[] signal) {
		int p = 1;
		double[] newSignal;
		int oldLength = signal.length;
		while (Math.pow(2, p) < oldLength) {
			p = p +1;
	    }
		newSignal = new double[(int) Math.pow(2, p)];
		for (int i = 0; i < oldLength; i++) {
			newSignal[i] = signal[i];
		}
		return newSignal;
	}
	
	/**
	 * This method does Rectangular windowing
	 * See also www.labbookpages.co.uk/audio/firWindowing.html#windows
	 * @param  rai
	 * @return windowed rai
	 */
	private RandomAccessibleInterval<DoubleType> windowingRectangular (RandomAccessibleInterval<?> rai) {
		int width  = (int) rai.dimension(0);
		int height = (int) rai.dimension(1);	
		raiWindowed = new ArrayImgFactory<>(new DoubleType()).create(width, height); //always single 2D
		
		double weight = 1.0;
	
		Cursor<DoubleType> cursorD = Views.iterable(raiWindowed).localizingCursor();
		long[] pos = new long[raiWindowed.numDimensions()];
		RandomAccess<RealType<?>> ra = (RandomAccess<RealType<?>>) rai.randomAccess();
		while (cursorD.hasNext()){
			cursorD.fwd();
			cursorD.localize(pos);
			ra.setPosition(pos);
			cursorD.get().setReal(ra.get().getRealDouble()*weight); //simply a copy
		} 
	    return raiWindowed; 
	}
	
	/**
	 * This method does Bartlett windowing
	 * See Burge Burge, Digital Image Processing, Springer
	 * @param  rai
	 * @return windowed rai
	 */
	private RandomAccessibleInterval<DoubleType> windowingBartlett (RandomAccessibleInterval<?> rai) {
		
		int width  = (int) rai.dimension(0);
		int height = (int) rai.dimension(1);	
		raiWindowed = new ArrayImgFactory<>(new DoubleType()).create(width, height); //always single 2D
		
		double r_u;
		double r_v;
		double r_uv;
		double weight;
		
		//Create a full weight window
//		double[][] window = new double[width][height];
//		for (int u = 0; u < width; u++) {
//			for (int v = 0; v < height; v++) {
//				r_u = 2.0*u/width-1.0;
//				r_v = 2.0*v/height-1.0;
//				r_uv = Math.sqrt(r_u*r_u + r_v*r_v);
//				if ((r_uv >= 0) && (r_uv <=1)) window[u][v] = 1 - r_uv;
//				else window[u][v] = 0.0;
//			}
//		}
		
		Cursor<DoubleType> cursorD = Views.iterable(raiWindowed).localizingCursor();
		long[] pos = new long[raiWindowed.numDimensions()];
		RandomAccess<RealType<?>> ra = (RandomAccess<RealType<?>>) rai.randomAccess();
		while (cursorD.hasNext()){
			cursorD.fwd();
			cursorD.localize(pos);
			ra.setPosition(pos);
			r_u = 2.0*(pos[0]+0.5)/width -1.0;   //+0.5 so that the maximum is really centered
			r_v = 2.0*(pos[1]+0.5)/height-1.0;
			r_uv = Math.sqrt(r_u*r_u + r_v*r_v);
			if ((r_uv >= 0) && (r_uv <=1)) weight = 1 - r_uv;
			else weight = 0.0;	
			//if(pos[1] == 1) System.out.println("Bartlett windowing weight " + pos[0] +" "+pos[1]+"  "+ weight);
			cursorD.get().setReal(ra.get().getRealDouble()*weight);
		} 
	    return raiWindowed; 
	}

	/**
	 * This method does Hamming windowing
	 * See also www.labbookpages.co.uk/audio/firWindowing.html#windows
	 * See Burge Burge, Digital Image Processing, Springer
	 * @param  rai
	 * @return windowed rai
	 */
	private RandomAccessibleInterval<DoubleType> windowingHamming (RandomAccessibleInterval<?> rai) {
	
		int width  = (int) rai.dimension(0);
		int height = (int) rai.dimension(1);	
		raiWindowed = new ArrayImgFactory<>(new DoubleType()).create(width, height); //always single 2D
		
		double r_u;
		double r_v;
		double r_uv;
		double weight;
		
		Cursor<DoubleType> cursorD = Views.iterable(raiWindowed).localizingCursor();
		long[] pos = new long[raiWindowed.numDimensions()];
		RandomAccess<RealType<?>> ra = (RandomAccess<RealType<?>>) rai.randomAccess();
		while (cursorD.hasNext()){
			cursorD.fwd();
			cursorD.localize(pos);
			ra.setPosition(pos);
			r_u = 2.0*(pos[0]+0.5)/width -1.0;   //+0.5 so that the maximum is really centered
			r_v = 2.0*(pos[1]+0.5)/height-1.0;
			r_uv = Math.sqrt(r_u*r_u + r_v*r_v);
			if ((r_uv >= 0) && (r_uv <=1)) weight = 0.54 + 0.46*Math.cos(Math.PI*(r_uv)); //== 0.54 - 0.46*Math.cos(Math.PI*(1.0-r_uv));
			else weight = 0.0;	
			//if(pos[1] == 1) System.out.println("Hamming windowing weight " + pos[0] +" "+pos[1]+"  "  + weight);
			cursorD.get().setReal(ra.get().getRealDouble()*weight);
		} 
	    return raiWindowed; 
	}
	
	/**
	 * This method does Hanning windowing
	 * See also www.labbookpages.co.uk/audio/firWindowing.html#windows
	 * See Burge Burge, Digital Image Processing, Springer
	 * @param  rai
	 * @return windowed rai
	 */
	private RandomAccessibleInterval<DoubleType> windowingHanning (RandomAccessibleInterval<?> rai) {
		
		int width  = (int) rai.dimension(0);
		int height = (int) rai.dimension(1);	
		raiWindowed = new ArrayImgFactory<>(new DoubleType()).create(width, height); //always single 2D
		
		double r_u;
		double r_v;
		double r_uv;
		double weight = 0;
		
		Cursor<DoubleType> cursorD = Views.iterable(raiWindowed).localizingCursor();
		long[] pos = new long[raiWindowed.numDimensions()];
		RandomAccess<RealType<?>> ra = (RandomAccess<RealType<?>>) rai.randomAccess();
		while (cursorD.hasNext()){
			cursorD.fwd();
			cursorD.localize(pos);
			ra.setPosition(pos);
			r_u = 2.0*(pos[0]+0.5)/width -1.0;   //+0.5 so that the maximum is really centered
			r_v = 2.0*(pos[1]+0.5)/height-1.0;
			r_uv = Math.sqrt(r_u*r_u + r_v*r_v);
			if ((r_uv >= 0) && (r_uv <=1)) {
				//weight = 0.5*Math.cos(Math.PI*r_uv+1); //Burge Burge  gives negative weights!
				weight = 0.5 + 0.5*Math.cos(Math.PI*(r_uv)); //== 0.5 - 0.5*Math.cos(Math.PI*(1-r_uv));
			}
			else weight = 0.0;	
			//if(pos[1] == 1) System.out.println("Hanning windowing weight " + pos[0] +" "+pos[1]+"  " + weight);
			cursorD.get().setReal(ra.get().getRealDouble()*weight);
		} 
	    return raiWindowed; 
	}
	
	/**
	 * This method does Blackman windowing
	 * See also www.labbookpages.co.uk/audio/firWindowing.html#windows
	 * See Burge Burge, Digital Image Processing, Springer
	 * @param  rai
	 * @return windowed rai
	 */
	private RandomAccessibleInterval<DoubleType> windowingBlackman (RandomAccessibleInterval<?> rai) {
		int width  = (int) rai.dimension(0);
		int height = (int) rai.dimension(1);	
		raiWindowed = new ArrayImgFactory<>(new DoubleType()).create(width, height); //always single 2D
		
		double r_u;
		double r_v;
		double r_uv;
		double weight;
		
		Cursor<DoubleType> cursorD = Views.iterable(raiWindowed).localizingCursor();
		long[] pos = new long[raiWindowed.numDimensions()];
		RandomAccess<RealType<?>> ra = (RandomAccess<RealType<?>>) rai.randomAccess();
		while (cursorD.hasNext()){
			cursorD.fwd();
			cursorD.localize(pos);
			ra.setPosition(pos);
			r_u = 2.0*(pos[0]+0.5)/width -1.0;   //+0.5 so that the maximum is really centered
			r_v = 2.0*(pos[1]+0.5)/height-1.0;
			r_uv = Math.sqrt(r_u*r_u + r_v*r_v);
			//if ((r_uv >= 0) && (r_uv <=1)) weight = 0.42 - 0.5*Math.cos(Math.PI*(1.0-r_uv)) + 0.08*Math.cos(2.0*Math.PI*(1.0-r_uv));
			if ((r_uv >= 0) && (r_uv <=1)) weight = 0.42 - 0.5*Math.cos(Math.PI*(1.0-r_uv)) + 0.08*Math.cos(2.0*Math.PI*(1.0-r_uv));
			else weight = 0.0;	
			//if(pos[1] == 1) System.out.println("Blackman windowing weight " + pos[0] +" "+pos[1]+"  "  + weight);
			cursorD.get().setReal(ra.get().getRealDouble()*weight);
		} 
	    return raiWindowed; 
	}
	
	/**
	 * This method does Gaussian windowing
	 * See also www.labbookpages.co.uk/audio/firWindowing.html#windows
	 * See Burge Burge, Digital Image Processing, Springer
	 * @param  rai
	 * @return windowed rai
	 */
	private RandomAccessibleInterval<DoubleType> windowingGaussian (RandomAccessibleInterval<?> rai) {
		int width  = (int) rai.dimension(0);
		int height = (int) rai.dimension(1);	
		raiWindowed = new ArrayImgFactory<>(new DoubleType()).create(width, height); //always single 2D
		
		double r_u;
		double r_v;
		double r_uv;
		double weight = 0;
		double sigma  = 0.3;
		double sigma2 = sigma*sigma;
		
		Cursor<DoubleType> cursorD = Views.iterable(raiWindowed).localizingCursor();
		long[] pos = new long[raiWindowed.numDimensions()];
		RandomAccess<RealType<?>> ra = (RandomAccess<RealType<?>>) rai.randomAccess();
		while (cursorD.hasNext()){
			cursorD.fwd();
			cursorD.localize(pos);
			ra.setPosition(pos);
			r_u = 2.0*(pos[0]+0.5)/width -1.0;   //+0.5 so that the maximum is really centered
			r_v = 2.0*(pos[1]+0.5)/height-1.0;
			r_uv = Math.sqrt(r_u*r_u + r_v*r_v);
			weight = Math.exp(-(r_uv*r_uv)/(2.0*sigma2));
			//if(pos[1] == 1) System.out.println("Gaussian windowing weight " + pos[0] +" "+pos[1]+"  "  + weight);
			cursorD.get().setReal(ra.get().getRealDouble()*weight);
		} 
	    return raiWindowed; 
	}

	/**
	 * This method does Parzen windowing
	 * See also www.labbookpages.co.uk/audio/firWindowing.html#windows
	 * See Burge Burge, Digital Image Processing, Springer
	 * @param  rai
	 * @return windowed rai
	 */
	private RandomAccessibleInterval<DoubleType> windowingParzen (RandomAccessibleInterval<?> rai) {
	
		int width  = (int) rai.dimension(0);
		int height = (int) rai.dimension(1);	
		raiWindowed = new ArrayImgFactory<>(new DoubleType()).create(width, height); //always single 2D
		
		double r_u;
		double r_v;
		double r_uv;
		double weight;
		
		Cursor<DoubleType> cursorD = Views.iterable(raiWindowed).localizingCursor();
		long[] pos = new long[raiWindowed.numDimensions()];
		RandomAccess<RealType<?>> ra = (RandomAccess<RealType<?>>) rai.randomAccess();
		while (cursorD.hasNext()){
			cursorD.fwd();
			cursorD.localize(pos);
			ra.setPosition(pos);
			r_u = 2.0*(pos[0]+0.5)/width -1.0;   //+0.5 so that the maximum is really centered
			r_v = 2.0*(pos[1]+0.5)/height-1.0;
			r_uv = Math.sqrt(r_u*r_u + r_v*r_v);
			//if      ((r_uv >= 0) && (r_uv <0.5)) weight = 1.0 - 6.0*Math.pow(r_uv, 2) + 6.0*Math.pow(r_uv, 3); //Burge Burge gives double peaks, seems to be wrong
			if      ((r_uv >= 0) && (r_uv <0.5)) weight = 1.0 - 6.0*Math.pow(r_uv, 2)*(1-r_uv);
			else if ((r_uv >= 0.5) && (r_uv <1)) weight = 2.0*Math.pow(1-r_uv, 3);
			else    weight = 0.0;	
			//if(pos[1] == 1) System.out.println("Parzen windowing weight " + pos[0] +" "+pos[1]+"  "  + weight);
			cursorD.get().setReal(ra.get().getRealDouble()*weight);
		} 
	    return raiWindowed; 
	}

}
