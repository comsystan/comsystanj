/*-
 * #%L
 * Project: ImageJ2 plugin to generate 3D image volumes.
 * File: Img3DVolumeGenerator.java
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

package at.csa.csaj.img3d.generate;

import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ImageJ;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.FinalRealInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.FloorInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.LanczosInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineRandomAccessible;
import net.imglib2.realtransform.AffineRealRandomAccessible;
import net.imglib2.realtransform.AffineRealRandomAccessible.AffineRealRandomAccess;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.InverseRealTransform;
import net.imglib2.realtransform.RealTransformRandomAccessible;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.AbstractIntegerType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import net.imglib2.view.IntervalView;
import net.imglib2.view.RandomAccessibleOnRealRandomAccessible;
import net.imglib2.view.Views;

import org.apache.commons.math3.util.Precision;
import org.scijava.ItemIO;
import org.scijava.ItemVisibility;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.command.ContextCommand;
import org.scijava.command.Previewable;
import org.scijava.display.DisplayService;
import org.scijava.log.LogService;
import org.scijava.menu.MenuConstants;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import org.scijava.widget.ChoiceWidget;
import org.scijava.widget.NumberWidget;
import at.csa.csaj.commons.dialog.WaitingDialogWithProgressBar;
import edu.emory.mathcs.jtransforms.fft.FloatFFT_3D;
import io.scif.services.DatasetIOService;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.text.SimpleDateFormat;
import java.util.Random;
import java.util.TimeZone;
import javax.swing.UIManager;

/**
 * This is an ImageJ {@link Command} plugin for generation of §D image volumes.
 * <p>
 * The {@link run} method implements the computations.
 * </p>
 * @param <C>
 */
@Plugin(type = ContextCommand.class,
		label = "Image volume generator",
		//iconPath = "/images/comsystan-??.png", //Menu entry icon
		menu = {
        @Menu(label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT, mnemonic = MenuConstants.PLUGINS_MNEMONIC),
        @Menu(label = "ComsystanJ"),
        @Menu(label = "Image (3D)"),
        @Menu(label = "Image volume generator", weight = 10)})
public class Img3DVolumeGenerator<T extends RealType<T>, C> extends ContextCommand implements Previewable { //modal GUI with cancel
		
	private static final String PLUGIN_LABEL 			= "<html><b>Generates 3D image volums</b></html>";
	private static final String SPACE_LABEL 			= "";
	private static final String METHODOPTIONS_LABEL     = "<html><b>Method options</b></html>";
  
	@Parameter
	private LogService logService;

	@Parameter
	private StatusService statusService;
	
	@Parameter (label = "Generated volume",type = ItemIO.OUTPUT) //so that it can be displayed
	private Dataset datasetOut;

	@Parameter
	private DatasetService datasetService;

	@Parameter
	private DatasetIOService datasetIOService;
	
	@Parameter
	private DisplayService displayService;

    @Parameter
    private UIService uiService;

    @Parameter
    private OpService opService;

	private Img<FloatType> volFloat;
	private Img<UnsignedByteType> resultVolume;
	private Img<FloatType> ifsVolume;
	private RandomAccess<FloatType> raF;
	private RandomAccess<UnsignedByteType> ra;
	private Cursor<UnsignedByteType> cursor;
	private Cursor<FloatType> cursoF;
	
	//Widget elements------------------------------------------------------
	//-----------------------------------------------------------------------------------------------------
//  @Parameter(label = " ", visibility = ItemVisibility.MESSAGE)
//	private final String labelPlugin = PLUGIN_LABEL;

    @Parameter(label = " ", visibility = ItemVisibility.MESSAGE)
  	private final String labelSpace = SPACE_LABEL;
    
    @Parameter(label = "Width [pixel]",
    		   description = "Width of output image stack in pixel",
    		   style = NumberWidget.SPINNER_STYLE,
    		   min = "1",
    		   max = "32768",
    		   stepSize = "1",
    		   persist = true,  //restore previous value default = true
    		   initializer = "initialWidth",
    		   callback = "changedWidth")
    private int spinnerInteger_Width;
    
    @Parameter(label = "Height [pixel]",
    		   description = "Height of output image stack in pixel",
 		       style = NumberWidget.SPINNER_STYLE,
 		       min = "1",
 		       max = "32768", 
 		       stepSize = "1",
 		       persist = true,  //restore previous value default = true
 		       initializer = "initialHeight",
 		       callback = "changedHeight")
    private int spinnerInteger_Height;
    
    @Parameter(label = "Depth [pixel]",
 	   	       description = "Depth of output image stack",
	  		   style = NumberWidget.SPINNER_STYLE,
	  		   min = "1",
	  		   max = "32768",
	  		   stepSize = "1",
	  		   persist = true,  //restore previous value default = true
	  		   initializer = "initialDepth",  
	  		   callback = "changedDepth")
    private int spinnerInteger_Depth;
    
    @Parameter(label = "Color model",
			   description = "Color model of output image",
			   style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
			   choices = {"Grey-8bit", "Color-RGB"}, //
			   persist = true,  //restore previous value default = true
			   initializer = "initialColorModelType",
			   callback = "callbackColorModelType")
	private String choiceRadioButt_ColorModelType;
    
    @Parameter(label = "Volume type",
    		   description = "Type of output image stack, FFT..Fast Fourier transform, MPD..Midpoint displacement",
    		   style = ChoiceWidget.LIST_BOX_STYLE,
    		   choices = {"Random", "Gaussian", "Constant", 
    				      "Fractal volume - FFT", "Fractal volume - MPD",
    				      "Fractal IFS - Menger", //"Fractal IFS - Sierpinski-1", "Fractal IFS - Sierpinski-2",
    				     },
    		   persist = true,  //restore previous value default = true
    		   initializer = "initialVolumeType",
               callback = "changedVolumeType")
    private String choiceRadioButt_VolumeType;
    
	//-----------------------------------------------------------------------------------------------------
    @Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
    private final String labelMethodOptions = METHODOPTIONS_LABEL;
    
    @Parameter(label = "Grey/R",
    		   description = "Grey value of Grey image or of the RGB R channel",
	  		   style = NumberWidget.SPINNER_STYLE,
	  		   min = "0",
	  		   max = "255",
	  		   stepSize = "1",
	  		   persist = true,  //restore previous value default = true
	  		   initializer = "initialR",  		 
	  		   callback = "changedR")
    private int spinnerInteger_R;
    
    @Parameter(label = "G",
    		   description = "Grey value of the RGB G channel",
	  		   style = NumberWidget.SPINNER_STYLE,
	  		   min = "0",
	  		   max = "255",
	  		   stepSize = "1",
	  		   persist = true,  //restore previous value default = true
	  		   initializer = "initialG",
	  		   callback = "changedG")
    private int spinnerInteger_G;
    
    @Parameter(label = "B",
    		   description = "Grey value of the RGB B channel",
	  		   style = NumberWidget.SPINNER_STYLE,
	  		   min = "0",
	  		   max = "255",
	  		   stepSize = "1",
	  		   persist = true,  //restore previous value default = true				
	  		   initializer = "initialB",  		  
	  		   callback = "changedB")
    private int spinnerInteger_B;
    
    @Parameter(label = "(Fractal volume) Dimension",
    		   description = "Fractal dimension of fractal volume in the range [3,4]",
	  		   style = NumberWidget.SPINNER_STYLE,
	  		   min = "2.99", //otherwise 3 may not be reached because of these float errors
	  		   max = "4",
	  		   stepSize = "0.1",
	  		   persist = true,  //restore previous value default = true
	  		   initializer = "initialFracDim",
	  		   callback = "changedFracDim")
    private float spinnerFloat_FracDim;
    
    @Parameter(label = "(IFS) #",
	   	       description = "Number of iteration for IFS algorithms",
	  		   style = NumberWidget.SPINNER_STYLE,
	  		   min = "1",
	  		   max = "999999999999999999999",
	  		   stepSize = "1",
	  		   persist = true,  //restore previous value default = true
	  		   initializer = "initialNumIterations",
	  		   callback = "changedNumIterations")
 private int spinnerInteger_NumIterations;
 
    
    //---------------------------------------------------------------------
    //The following initializer functions set initial values	
    protected void initialWidth() {
    	spinnerInteger_Width = 512;
    }

    protected void initialHeight() {
    	spinnerInteger_Height = 512;
    }
    
    protected void initialDepth() {
    	spinnerInteger_Depth = 512;
    }

    protected void initialColorModelType() {
		choiceRadioButt_ColorModelType = "Grey-8bit";
	}

    protected void initialVolumeType() {
    	choiceRadioButt_VolumeType = "Random";
    }
        
    protected void initialR() {
    	spinnerInteger_R = 255;
    }
    
    protected void initialG() {
    	spinnerInteger_G = 0;
    }
    
    protected void initialB() {
    	spinnerInteger_B = 0;
    }
    
	protected void initialFracDim() {
	 	//round to one decimal after the comma
	 	spinnerFloat_FracDim = 3.5f;
	}
	
	protected void initialNumIterations() {
		spinnerInteger_NumIterations = 3;
	}

	// ------------------------------------------------------------------------------	
	/** Executed whenever the {@link #spinnerInteger_Width} parameter changes. */
	protected void changedWidth() {
		logService.info(this.getClass().getName() + " Width changed to " + spinnerInteger_Width + " pixel");
	}
	/** Executed whenever the {@link #spinnerInteger_Height} parameter changes. */
	protected void changedHeight() {
		logService.info(this.getClass().getName() + " Height changed to " + spinnerInteger_Height + " pixel");
	}
	
	/** Executed whenever the {@link #spinnerInteger_Depth} parameter changes. */
	protected void changedDepth() {
		logService.info(this.getClass().getName() + " Depth changed to " + spinnerInteger_Depth);
	}
	
	/** Executed whenever the {@link #choiceRadioButt_ColorModelType} parameter changes. */
	protected void callbackColorModelType() {
		if      (choiceRadioButt_VolumeType.equals("Fractal volume - FFT"))  choiceRadioButt_ColorModelType = "Grey-8bit";
		else if (choiceRadioButt_VolumeType.equals("Fractal volume - MPD"))  choiceRadioButt_ColorModelType = "Grey-8bit";
		logService.info(this.getClass().getName() + " Color model set to " + choiceRadioButt_ColorModelType);
	}
	
	/** Executed whenever the {@link #choiceRadioButt_VolumeType} parameter changes. */
	protected void changedVolumeType() {
		if      (choiceRadioButt_VolumeType.equals("Fractal volume - FFT"))  choiceRadioButt_ColorModelType = "Grey-8bit";
		else if (choiceRadioButt_VolumeType.equals("Fractal volume - MPD"))  choiceRadioButt_ColorModelType = "Grey-8bit";
		logService.info(this.getClass().getName() + " Volume type changed to " + choiceRadioButt_VolumeType);
	}
		
	/** Executed whenever the {@link #spinnerInteger_R} parameter changes. */
	protected void changedR() {
		logService.info(this.getClass().getName() + " Constant/Channel R changed to " + spinnerInteger_R);
	}
	
	/** Executed whenever the {@link #spinnerInteger_G} parameter changes. */
	protected void changedG() {
		logService.info(this.getClass().getName() + " Chanel G changed to " + spinnerInteger_G);
	}
	
	/** Executed whenever the {@link #spinnerInteger_B} parameter changes. */
	protected void changedB() {
		logService.info(this.getClass().getName() + " Channel B changed to " + spinnerInteger_B);
	}
	
	protected void changedFracDim() {
		//logService.info(this.getClass().getName() + " FD changed to " + spinnerFloat_FracDim);
	 	//round to one decimal after the comma
	 	//spinnerFloat_FracDim = Math.round(spinnerFloat_FracDim * 10f)/10f;
	 	spinnerFloat_FracDim = Precision.round(spinnerFloat_FracDim, 1);
	 	logService.info(this.getClass().getName() + " FD changed to " + spinnerFloat_FracDim);
	}
	
	/** Executed whenever the {@link #spinnerInteger_NumIterations} parameter changes. */
	protected void changedNumIterations() {
		logService.info(this.getClass().getName() + " Iterations/Number changed to " + spinnerInteger_NumIterations);
	}
		
    // You can control how previews work by overriding the "preview" method.
 	// The code written in this method will be automatically executed every
 	// time a widget value changes.
 	public void preview() {
 		logService.info(this.getClass().getName() + " Preview initiated");
 		//statusService.showStatus(message);
 	}
 	
    // This is often necessary, for example, if your  "preview" method manipulates data;
 	// the "cancel" method will then need to revert any changes done by the previews back to the original state.
 	public void cancel() {
 		logService.info(this.getClass().getName() + " Widget canceled");
 	}
 	
    private void compute3DRandomGrey(int greyMax) {
	
		Random random = new Random();
		random.setSeed(System.currentTimeMillis());
		resultVolume = new ArrayImgFactory<>(new UnsignedByteType()).create(datasetOut.dimension(0), datasetOut.dimension(1), datasetOut.dimension(2));
		Cursor<UnsignedByteType> cursor = resultVolume.cursor();
		//final long[] pos = new long[dataset.numDimensions()];
		while (cursor.hasNext()) {
			cursor.fwd();
			//cursor.localize(pos);
			cursor.get().setReal((int)(random.nextFloat()*greyMax));
		}  			
	}
    
    private void compute3DRandomRGB(int greyMaxR, int greyMaxG, int greyMaxB) {
    	
		Random random = new Random();
		random.setSeed(System.currentTimeMillis());
		resultVolume = new ArrayImgFactory<>(new UnsignedByteType()).create(datasetOut.dimension(0), datasetOut.dimension(1), datasetOut.dimension(2), datasetOut.dimension(3));
		Cursor<UnsignedByteType> cursor = resultVolume.cursor();
		long[] pos = new long[4];
		while (cursor.hasNext()) {
			cursor.fwd();
			cursor.localize(pos);
			if      (pos[3] == 0) cursor.get().setReal((int)(random.nextFloat()*greyMaxR));
			else if (pos[3] == 1) cursor.get().setReal((int)(random.nextFloat()*greyMaxG));
			else if (pos[3] == 2) cursor.get().setReal((int)(random.nextFloat()*greyMaxB));
		}  			
	}

	private void compute3DGaussianGrey(int greyMax) {
        
    	float mu = (float)greyMax/2f;
    	float sigma = 30f;
    	Random random = new Random();
    	resultVolume = new ArrayImgFactory<>(new UnsignedByteType()).create(datasetOut.dimension(0), datasetOut.dimension(1), datasetOut.dimension(2));
    	Cursor<UnsignedByteType> cursor = resultVolume.cursor();
    	//final long[] pos = new long[dataset.numDimensions()];
		while (cursor.hasNext()) {
			cursor.fwd();
			//cursor.localize(pos);
			cursor.get().setReal((int)(random.nextGaussian()*sigma + mu));
		}  			
	}
   
	private void compute3DGaussianRGB(int greyMaxR, int greyMaxG, int greyMaxB) {
        
    	float muR = (float)greyMaxR/2f;
    	float muG = (float)greyMaxG/2f;
    	float muB = (float)greyMaxB/2f;
    	float sigma = 30f;
    	Random random = new Random();
		resultVolume = new ArrayImgFactory<>(new UnsignedByteType()).create(datasetOut.dimension(0), datasetOut.dimension(1), datasetOut.dimension(2), datasetOut.dimension(3));
		Cursor<UnsignedByteType> cursor = resultVolume.cursor();
		long[] pos = new long[4];
		while (cursor.hasNext()) {
			cursor.fwd();
			cursor.localize(pos);		
			if      (pos[3] == 0) cursor.get().setReal((int)(random.nextGaussian()*sigma + muR));
			else if (pos[3] == 1) cursor.get().setReal((int)(random.nextGaussian()*sigma + muG));
			else if (pos[3] == 2) cursor.get().setReal((int)(random.nextGaussian()*sigma + muB));		
		}  			
	}
	
    private void compute3DConstantGrey(int constant) {
        
    	resultVolume = new ArrayImgFactory<>(new UnsignedByteType()).create(datasetOut.dimension(0), datasetOut.dimension(1), datasetOut.dimension(2));
    	Cursor<UnsignedByteType> cursor = resultVolume.cursor();
    	
    	//final long[] pos = new long[dataset.numDimensions()];
		while (cursor.hasNext()) {
			cursor.fwd();
			//cursor.localize(pos);
			cursor.get().setReal(constant);
		}  			
	}
    
    private void compute3DConstantRGB(int constR, int constG, int constB) {
        
		resultVolume = new ArrayImgFactory<>(new UnsignedByteType()).create(datasetOut.dimension(0), datasetOut.dimension(1), datasetOut.dimension(2), datasetOut.dimension(3));
    	Cursor<UnsignedByteType> cursor = resultVolume.cursor();
    	long[] pos = new long[4];
		while (cursor.hasNext()) {
			cursor.fwd();
			cursor.localize(pos);
			if      (pos[3] == 0) cursor.get().setReal(constR);
			else if (pos[3] == 1) cursor.get().setReal(constG);
			else if (pos[3] == 2) cursor.get().setReal(constB);
		}  			
	}
    
    //@author Moritz Hackhofer
    private void compute3DFracFFTGrey(float fracDim, int greyMax) {
    	   
    	int width  = (int)datasetOut.dimension(0);
    	int height = (int)datasetOut.dimension(1);
    	int depth  = (int)datasetOut.dimension(2);
    	Cursor<FloatType> cursorF;
    	long[] pos;
    	
    	//create empty volume
		volFloat = new ArrayImgFactory<>(new FloatType()).create(width, height, depth);
    	
		//Using JTransform package
		//https://github.com/wendykierp/JTransforms
		//https://wendykierp.github.io/JTransforms/apidocs/
		//The sizes of all dimensions must be power of two.	
		// Round to next largest power of two. The resulting volume will be later cropped according to GUI input
		int widthDFT  = width  == 1 ? 1 : Integer.highestOneBit(width  - 1) * 2;
		int heightDFT = height == 1 ? 1 : Integer.highestOneBit(height - 1) * 2;
		int depthDFT  = depth  == 1 ? 1 : Integer.highestOneBit(depth  - 1) * 2;
		
		//All DFT axes must have the same size, otherwise image will be anisotropic
		widthDFT  = (int)Math.max(Math.max(widthDFT, heightDFT), depthDFT); 
		heightDFT = widthDFT;
		depthDFT  = widthDFT;		
		
		//JTransform needs rows and columns swapped!!!!!
		int slices  = depthDFT;
		int rows    = heightDFT;
		int columns = widthDFT;
   
		float[][][] volFFT = new float[slices][rows][2*columns]; //Every frequency entry needs a pair of columns: for real and imaginary part
		
		FloatFFT_3D FFT = new FloatFFT_3D(slices, rows, columns); //Here always the simple DFT width
    	
    	// Define 8 origins for fft. Not sure if this is really necessary or if 4 would be sufficient.
    	// Define origin as 0,0,0. //frequency = 0;
    	final long[] origin1 = {0, 0, 0};
    	// Define a 2nd 'origin' at bottom right of image
    	final long[] origin2 = {0, rows-1, 0};   	
    	// Define a 3nd 'origin' at top left of image
    	final long[] origin3 = {0, 0, slices-1};    	
    	// Define a 4th 'origin' at top right of image
    	final long[] origin4 = {0, rows-1, slices-1};
    	
    	final long[] origin5 = {columns-1, 0, 0};
    	// Define a 2nd 'origin' at bottom right of image
    	final long[] origin6 = {columns-1, rows-1, 0};   	
    	// Define a 3nd 'origin' at top left of image
    	final long[] origin7 = {columns-1, 0, slices-1};    	
    	// Define a 4th 'origin' at top right of image
    	final long[] origin8 = {columns-1, rows-1, slices-1};
    	  	
    	// Define half height and depth. Later used to find right origin
    	final long fftHalfSlices  = slices/2;
    	final long fftHalfRows    = rows/2;
    	final long fftHalfColumns = columns/2;
		
		// generate random pixel values
		Random random = new Random();
		random.setSeed(System.currentTimeMillis());
		// Hurst parameter
    	float b = 11.0f - (2.0f * fracDim); 
    	
    	double dist = 0;
    	double g;
    	float u;
    	float n;
    	float m;
    	float real;
		float imag;
 
		long[] posFFT = new long[3];
		
		// Loop through all pixels.
		for (int k1 = 0; k1 < slices; k1++) {
			for (int k2 = 0; k2 < rows; k2++) {
				for (int k3 = 0; k3 < columns; k3++) {
						posFFT[2] = k1;
						posFFT[1] = k2;
						posFFT[0] = k3;
						
						// change origin depending on cursor position
						if      (posFFT[0] <  fftHalfColumns && posFFT[1] <  fftHalfRows && posFFT[2] <  fftHalfSlices)  dist = Util.distance(origin1, posFFT);
						else if (posFFT[0] <  fftHalfColumns && posFFT[1] >= fftHalfRows && posFFT[2] <  fftHalfSlices)  dist = Util.distance(origin2, posFFT);
						else if (posFFT[0] <  fftHalfColumns && posFFT[1] <  fftHalfRows && posFFT[2] >= fftHalfSlices)  dist = Util.distance(origin3, posFFT);
						else if (posFFT[0] <  fftHalfColumns && posFFT[1] >= fftHalfRows && posFFT[2] >= fftHalfSlices)  dist = Util.distance(origin4, posFFT);
						else if (posFFT[0] >= fftHalfColumns && posFFT[1] <  fftHalfRows && posFFT[2] <  fftHalfSlices)  dist = Util.distance(origin5, posFFT);
						else if (posFFT[0] >= fftHalfColumns && posFFT[1] >= fftHalfRows && posFFT[2] <  fftHalfSlices)  dist = Util.distance(origin6, posFFT);
						else if (posFFT[0] >= fftHalfColumns && posFFT[1] <  fftHalfRows && posFFT[2] >= fftHalfSlices)  dist = Util.distance(origin7, posFFT);
						else if (posFFT[0] >= fftHalfColumns && posFFT[1] >= fftHalfRows && posFFT[2] >= fftHalfSlices)  dist = Util.distance(origin8, posFFT);
				
						// generate random numbers
						g = random.nextGaussian();
						u = random.nextFloat();
						
						// calculate real and imaginary parts
						n = (float) (g * Math.cos(2 * Math.PI * u));
						m = (float) (g * Math.sin(2 * Math.PI * u));
						n = (float) (n * Math.pow(dist+1, -b / 2));
						m = (float) (m * Math.pow(dist+1, -b / 2));
						
						// write values to FFT
						volFFT[k1][k2][2*k3]	= n; //Real
						volFFT[k1][k2][2*k3+1]	= m; //Imaginary							
				}
			}
		}	
		//Inverse FFT		
		//vol is now really complex, Real and Imaginary pairs
		FFT.complexInverse(volFFT, false);
		
		//Write to volFloat
		//Note that only the values inside the dimensions of volFloat are copied 
		cursorF = volFloat.localizingCursor();
		pos = new long[3];
		float value;
		float min = Float.MAX_VALUE;
		float max = -Float.MAX_VALUE;
		
		while (cursorF.hasNext()) {
			cursorF.fwd();
			cursorF.localize(pos); 
			//JTransform needs rows and columns swapped!!!!!
			real = volFFT[(int)pos[2]][(int)pos[1]][(int)(2*pos[0])];
			//imag = volFFT[(int)pos[2]][(int)pos[1]][(int)(2*pos[0]+1)];
			//value = (float)Math.sqrt(real*real + imag*imag);
			cursorF.get().set(real); //only Real part for an image volume with real values
			if (real > max) {
				max = real;
			}
			if (real < min) {
				min = real;
			}
		}
		
		resultVolume = opService.create().img(volFloat, new UnsignedByteType());
		RandomAccess<UnsignedByteType> ra = resultVolume.randomAccess();	
		cursorF = volFloat.cursor();
		
		//cursor = ifft.cursor();
    	//final long[] pos = new long[resultVol.numDimensions()];
    	float rf = (greyMax/(max-min)); //rescale factor
		while (cursorF.hasNext()) {
			cursorF.fwd();
			cursorF.localize(pos);
			value= cursorF.get().getRealFloat();
			value = rf * (value -min); //Rescale to 0  - greyMax
			ra.setPosition(pos);
			//ra.get().set((int)(rf * (value -min)));
			ra.get().set((int)(Math.round(value)));	
		}		
		//resultVolume;
	}
    
    
    //@author Moritz Hackhofer
    private void compute3DFracMPDGrey(float fracDim, int greyMax) {
    	int width  = (int)datasetOut.dimension(0);
    	int height = (int)datasetOut.dimension(1);
    	int depth  = (int)datasetOut.dimension(2);
    	//resultVolume = new ArrayImgFactory<>(new UnsignedByteType()).create(width, height, depth);
    
    	//Hurst exponent
		float H = 4.f - (float)fracDim;
		
		// Random numbers
		float mu    = 0.f;
		float sigma = 1.f;
		Random random = new Random();
		random.setSeed(System.currentTimeMillis());
					
		// Round to next largest power of two. The resulting image will be cropped according to GUI input
		int N = width  == 1 ? 1 : Integer.highestOneBit(width  - 1) * 2;
		int M = height == 1 ? 1 : Integer.highestOneBit(height - 1) * 2;
		int O = depth  == 1 ? 1 : Integer.highestOneBit(depth  - 1) * 2;
				
		//All axes must have the same size, otherwise image will be anisotropic
		N = (int)Math.max(Math.max(N, M), O); 
		M = N;
		O = N;	
		
//    	// Get highest dimension to determine maxLevel
//    	int maxLevel;
//    	if      (N > M & N > O) {maxLevel = (int) (Math.log(N)/Math.log(2));}
//    	else if (M > N & M > O) {maxLevel = (int) (Math.log(M)/Math.log(2));}
//    	else if (O > N & O > M) {maxLevel = (int) (Math.log(O)/Math.log(2));}
//    	else    {maxLevel = (int) (Math.log(N)/Math.log(2));}
		
		int maxLevel = (int)(Math.log(N)/Math.log(2));
		
		// Create desired float array
		float[][][] volMPD = new float[N+1][M+1][O+1];
			
		// Start with random corner values at corners
		volMPD[0][0][0]  = (float) (random.nextGaussian() * sigma + mu);
		volMPD[0][M][0]  = (float) (random.nextGaussian() * sigma + mu); 
		volMPD[0][0][O]  = (float) (random.nextGaussian() * sigma + mu); 
		volMPD[0][M][O]  = (float) (random.nextGaussian() * sigma + mu); 
		volMPD[N][0][0]  = (float) (random.nextGaussian() * sigma + mu); 
		volMPD[N][M][0]  = (float) (random.nextGaussian() * sigma + mu); 
		volMPD[N][0][O]  = (float) (random.nextGaussian() * sigma + mu); 
		volMPD[N][M][O]  = (float) (random.nextGaussian() * sigma + mu); 
			
	    // Initialize step sizes for each dimension
		int d1 = N / 2;
		int d2 = M / 2;
		int d3 = O / 2;
		
		int x;
		int y;
		int z;
			
		// Iterate until maxLevel is reached. After that every pixel has a value assigned
		for (int stage = 0; stage < maxLevel; stage++) {
			// Flatten distribution for each step
			sigma = (float) (sigma/Math.pow(2.0,H)); 	
			
			// Middlepoints:
			for (x = d1; x <= N; x = x + 2*d1) {
				for (y = d2; y <= M; y = y + 2*d2) {
					for (z = d3; z <= O; z = z + 2*d3) {				
					     volMPD[x][y][z] = (float)((volMPD[x+d1][y+d2][z+d3]+volMPD[x+d1][y-d2][z+d3]
					    		 				   +volMPD[x-d1][y+d2][z+d3]+volMPD[x-d1][y-d2][z+d3]
					    		 				   +volMPD[x+d1][y+d2][z-d3]+volMPD[x+d1][y-d2][z-d3]
					    		 			       +volMPD[x-d1][y+d2][z-d3]+volMPD[x-d1][y-d2][z-d3])/8
					    		 				   +random.nextGaussian() * sigma + mu); 
					}
				}
			}
			// Faces:
			for (x = d1; x <= N; x = x + 2*d1) {
				for (y = d2; y <= M; y = y + 2*d2) {
					for (z = d3; z <= O; z = z + 2*d3) {
						
						// If the 6th pixel does not exist(would lie outside of the cube). Take
						// the 5 nearest reference points instead of 6.
				        if (z - 2*d3 < 0) { 
				        	volMPD[x][y][z-d3] = (float)((volMPD[x+d1][y+d2][z-d3]+volMPD[x+d1][y-d2][z-d3]
				        							     +volMPD[x-d1][y+d2][z-d3]+volMPD[x-d1][y-d2][z-d3]
				        						         +volMPD[x][y][z])/5
				        							     +random.nextGaussian() * sigma + mu);
				        } else {
				           	volMPD[x][y][z-d3] = (float)((volMPD[x+d1][y+d2][z-d3]+volMPD[x+d1][y-d2][z-d3]
				           							     +volMPD[x-d1][y+d2][z-d3]+volMPD[x-d1][y-d2][z-d3]
				           							     +volMPD[x][y][z]+volMPD[x][y][z-2*d3])/6
				           							     +random.nextGaussian() * sigma + mu);
				        }
				        // If the 6th pixel does not exist(would lie outside of the cube). Take
						// the 5 nearest reference points instead of 6.
				        if (z+2*d3 > O) {
				        	volMPD[x][y][z+d3] = (float)((volMPD[x+d1][y+d2][z+d3]+volMPD[x+d1][y-d2][z+d3]
				        							     +volMPD[x-d1][y+d2][z+d3]+volMPD[x-d1][y-d2][z+d3]
				        							     +volMPD[x][y][z])/5
				        							     +random.nextGaussian() * sigma + mu);
				        } else {
						    volMPD[x][y][z+d3] = (float)((volMPD[x+d1][y+d2][z+d3]+volMPD[x+d1][y-d2][z+d3]
						    						     +volMPD[x-d1][y+d2][z+d3]+volMPD[x-d1][y-d2][z+d3]
						    						     +volMPD[x][y][z]+volMPD[x][y][z+2*d3])/6
						    						     +random.nextGaussian() * sigma + mu);
						}
				        // If the 6th pixel does not exist(would lie outside of the cube). Take
						// the 5 nearest reference points instead of 6.
				        if (y - 2*d2 < 0) {
				           	volMPD[x][y-d2][z] = (float)((volMPD[x+d1][y-d2][z+d3]+volMPD[x-d1][y-d2][z+d3]
				           							     +volMPD[x+d1][y-d2][z-d3]+volMPD[x-d1][y-d2][z-d3]
				           							     +volMPD[x][y][z])/5
				           							     +random.nextGaussian() * sigma + mu);		        	
				        } else {
				        	volMPD[x][y-d2][z] = (float)((volMPD[x+d1][y-d2][z+d3]+volMPD[x-d1][y-d2][z+d3]
				        							     +volMPD[x+d1][y-d2][z-d3]+volMPD[x-d1][y-d2][z-d3]
				        							     +volMPD[x][y][z]+volMPD[x][y-2*d2][z])/6
				        							     +random.nextGaussian() * sigma + mu);
				        }
				        // If the 6th pixel does not exist(would lie outside of the cube). Take
						// the 5 nearest reference points instead of 6.
				        if (y +2*d2 > M) {
						    volMPD[x][y+d2][z] = (float)((volMPD[x+d1][y+d2][z+d3]+volMPD[x-d1][y+d2][z+d3]
						    						     +volMPD[x+d1][y+d2][z-d3]+volMPD[x-d1][y+d2][z-d3]
						    						     +volMPD[x][y][z])/5
						    						     +random.nextGaussian() * sigma + mu);
				        } else {
						    volMPD[x][y+d2][z] = (float)((volMPD[x+d1][y+d2][z+d3]+volMPD[x-d1][y+d2][z+d3]
						    						     +volMPD[x+d1][y+d2][z-d3]+volMPD[x-d1][y+d2][z-d3]
						    						     +volMPD[x][y][z]+volMPD[x][y+2*d2][z])/6
						    						     +random.nextGaussian() * sigma + mu);
						}
				        // If the 6th pixel does not exist(would lie outside of the cube). Take
						// the 5 nearest reference points instead of 6.
						if (x - 2*d1 < 0) {
				        	volMPD[x-d1][y][z] = (float)((volMPD[x-d1][y+d2][z+d3]+volMPD[x-d1][y-d2][z+d3]
				        							     +volMPD[x-d1][y+d2][z-d3]+volMPD[x-d1][y-d2][z-d3]
				        							     +volMPD[x][y][z])/5
				        							     +random.nextGaussian() * sigma + mu);					      				 		
						} else {		
				        	volMPD[x-d1][y][z] = (float)((volMPD[x-d1][y+d2][z+d3]+volMPD[x-d1][y-d2][z+d3]
				        							     +volMPD[x-d1][y+d2][z-d3]+volMPD[x-d1][y-d2][z-d3]
				        							     +volMPD[x][y][z]+volMPD[x-2*d1][y][z])/6
				        							     +random.nextGaussian() * sigma + mu);
				        }
						// If the 6th pixel does not exist(would lie outside of the cube). Take
						// the 5 nearest reference points instead of 6.
				        if (x + 2*d1 > N) {
				        	volMPD[x+d1][y][z] = (float)((volMPD[x+d1][y+d2][z+d3]+volMPD[x+d1][y-d2][z+d3]
				        							     +volMPD[x+d1][y+d2][z-d3]+volMPD[x+d1][y-d2][z-d3]
				        							     +volMPD[x][y][z])/5
				        							     +random.nextGaussian() * sigma + mu);
				        } else {
				        	volMPD[x+d1][y][z] = (float)((volMPD[x+d1][y+d2][z+d3]+volMPD[x+d1][y-d2][z+d3]
				        							     +volMPD[x+d1][y+d2][z-d3]+volMPD[x+d1][y-d2][z-d3]
				        							     +volMPD[x][y][z]+volMPD[x+2*d1][y][z])/6
				        							     +random.nextGaussian() * sigma + mu);
				       }	
			        }	
				}	
			}
			// Middle points of edges:	
			for (x = d1; x <= N; x = x + 2*d1) {
				for (y = d2; y <= M; y = y + 2*d2) {
					for (z = d3; z <= O; z = z + 2*d3) {
						// z fixed
						// The if conditions check if points are outside of the cube or not. 
						// If yes less reference points are averaged. The same approach is 
						// done for x fixed, y fixed and z fixed. 
						if(x - 2*d1 > 0 && y - 2*d2 > 0) {							
							volMPD[x-d1][y-d2][z] = (float)((volMPD[x][y-d2][z]+volMPD[x-d1][y][z]
															+volMPD[x-d1][y-d2][z-d3]+volMPD[x-d1][y-d2][z+d3]+volMPD[x-2*d1][y-d2][z]
															+volMPD[x-d1][y-2*d2][z])/6
															+random.nextGaussian() * sigma + mu);
						}
						else if(x - 2*d1 > 0 && y - 2*d2 < 0) {
							volMPD[x-d1][y-d2][z] = (float)((volMPD[x][y-d2][z]+volMPD[x-d1][y][z]
															+volMPD[x-d1][y-d2][z-d3]+volMPD[x-d1][y-d2][z+d3]+volMPD[x-2*d1][y-d2][z])/5
															+random.nextGaussian() * sigma + mu);
						}
						else if(x - 2*d1 < 0 && y - 2*d2 > 0) {
							volMPD[x-d1][y-d2][z] = (float)((volMPD[x][y-d2][z]+volMPD[x-d1][y][z]
															+volMPD[x-d1][y-d2][z-d3]+volMPD[x-d1][y-d2][z+d3]+volMPD[x-d1][y-2*d2][z])/5
															+random.nextGaussian() * sigma + mu);
						}	
						else {
							volMPD[x-d1][y-d2][z] = (float)((volMPD[x][y-d2][z]+volMPD[x-d1][y][z]
															+volMPD[x-d1][y-d2][z-d3]+volMPD[x-d1][y-d2][z+d3])/4
															+random.nextGaussian() * sigma + mu);
						}							
						
						
						if(x - 2*d1 > 0 && y + 2*d2 < M) {
							volMPD[x-d1][y+d2][z] = (float)((volMPD[x][y+d2][z]+volMPD[x-d1][y][z]
															+volMPD[x-d1][y+d2][z-d3]+volMPD[x-d1][y+d2][z+d3]+volMPD[x-2*d1][y+d2][z]
															+volMPD[x-d1][y+2*d2][z])/6
															+random.nextGaussian() * sigma + mu);
						}
						else if(x - 2*d1 < 0 && y + 2*d2 < M) {
							volMPD[x-d1][y+d2][z] = (float)((volMPD[x][y+d2][z]+volMPD[x-d1][y][z]
															+volMPD[x-d1][y+d2][z-d3]+volMPD[x-d1][y+d2][z+d3]+volMPD[x-d1][y+2*d2][z])/5
															+random.nextGaussian() * sigma + mu);
						}
						else if(x - 2*d1 > 0 && y + 2*d2 > M) {	
							volMPD[x-d1][y+d2][z] = (float)((volMPD[x][y+d2][z]+volMPD[x-d1][y][z]
															+volMPD[x-d1][y+d2][z-d3]+volMPD[x-d1][y+d2][z+d3]+volMPD[x-2*d1][y+d2][z])/5
															+random.nextGaussian() * sigma + mu);
						}
						else {	
						volMPD[x-d1][y+d2][z] = (float)((volMPD[x][y+d2][z]+volMPD[x-d1][y][z]
														+volMPD[x-d1][y+d2][z-d3]+volMPD[x-d1][y+d2][z+d3])/4
														+random.nextGaussian() * sigma + mu);
						}
						
						if(x + 2*d1 < N && y + 2*d2 < M) {
							volMPD[x+d1][y+d2][z] = (float)((volMPD[x][y+d2][z]+volMPD[x+d1][y][z]
															+volMPD[x+d1][y+d2][z-d3]+volMPD[x+d1][y+d2][z+d3]+volMPD[x+2*d1][y+d2][z]
															+volMPD[x+d1][y+2*d2][z])/6
															+random.nextGaussian() * sigma + mu);
						}
						else if (x + 2*d1 > N && y + 2*d2 < M) {
							volMPD[x+d1][y+d2][z] = (float)((volMPD[x][y+d2][z]+volMPD[x+d1][y][z]
															+volMPD[x+d1][y+d2][z-d3]+volMPD[x+d1][y+d2][z+d3]+volMPD[x+d1][y+2*d2][z])/5
															+random.nextGaussian() * sigma + mu);
						}
						else if (x + 2*d1 < N && y + 2*d2 > M) {
							volMPD[x+d1][y+d2][z] = (float)((volMPD[x][y+d2][z]+volMPD[x+d1][y][z]
															+volMPD[x+d1][y+d2][z-d3]+volMPD[x+d1][y+d2][z+d3]+volMPD[x+2*d1][y+d2][z])/5
															+random.nextGaussian() * sigma + mu);
						}
						else {								
							volMPD[x+d1][y+d2][z] = (float)((volMPD[x][y+d2][z]+volMPD[x+d1][y][z]
															+volMPD[x+d1][y+d2][z-d3]+volMPD[x+d1][y+d2][z+d3])/4
															+random.nextGaussian() * sigma + mu);
						}
						
						if(x + 2*d1 < N && y - 2*d2 > 0) {
							volMPD[x+d1][y-d2][z] = (float)((volMPD[x][y-d2][z]+volMPD[x+d1][y][z]
															+volMPD[x+d1][y-d2][z-d3]+volMPD[x+d1][y-d2][z+d3]+volMPD[x+2*d1][y-d2][z]
															+volMPD[x+d1][y-2*d2][z])/6
															+random.nextGaussian() * sigma + mu);
						}
						else if (x + 2*d1 > N && y - 2*d2 > 0) {
							volMPD[x+d1][y-d2][z] = (float)((volMPD[x][y-d2][z]+volMPD[x+d1][y][z]
															+volMPD[x+d1][y-d2][z-d3]+volMPD[x+d1][y-d2][z+d3]+volMPD[x+d1][y-2*d2][z])/5
															+random.nextGaussian() * sigma + mu);
						}	
						else if (x + 2*d1 < N && y - 2*d2 < 0) {
							volMPD[x+d1][y-d2][z] = (float)((volMPD[x][y-d2][z]+volMPD[x+d1][y][z]
															+volMPD[x+d1][y-d2][z-d3]+volMPD[x+d1][y-d2][z+d3]+volMPD[x+2*d1][y-d2][z])/5
															+random.nextGaussian() * sigma + mu);
						}
						else if (x + 2*d1 > N && y - 2*d2 < 0) {
							volMPD[x+d1][y-d2][z] = (float)((volMPD[x][y-d2][z]+volMPD[x+d1][y][z]
															+volMPD[x+d1][y-d2][z-d3]+volMPD[x+d1][y-d2][z+d3])/4
															+random.nextGaussian() * sigma + mu);
						}
						
						if(x - 2*d1 > 0 && z - 2*d3 > 0) {
						// y fixed
							volMPD[x-d1][y][z-d3] = (float)((volMPD[x][y][z-d3]+volMPD[x-d1][y][z]
															+volMPD[x-d1][y-d2][z-d3]+volMPD[x-d1][y+d2][z-d3]+volMPD[x-2*d1][y][z-d3]
															+volMPD[x-d1][y][z-2*d3])/6
															+random.nextGaussian() * sigma + mu);
						}
						else if (x - 2*d1 < 0 && z - 2*d3 > 0) {
							volMPD[x-d1][y][z-d3] = (float)((volMPD[x][y][z-d3]+volMPD[x-d1][y][z]
															+volMPD[x-d1][y-d2][z-d3]+volMPD[x-d1][y+d2][z-d3]+volMPD[x-d1][y][z-2*d3])/5
															+random.nextGaussian() * sigma + mu);
						}		
						else if (x - 2*d1 > 0 && z - 2*d3 < 0) {
							volMPD[x-d1][y][z-d3] = (float)((volMPD[x][y][z-d3]+volMPD[x-d1][y][z]
															+volMPD[x-d1][y-d2][z-d3]+volMPD[x-d1][y+d2][z-d3]+volMPD[x-2*d1][y][z-d3])/5
															+random.nextGaussian() * sigma + mu);
						}	
						else {
							volMPD[x-d1][y][z-d3] = (float)((volMPD[x][y][z-d3]+volMPD[x-d1][y][z]
															+volMPD[x-d1][y-d2][z-d3]+volMPD[x-d1][y+d2][z-d3])/4
															+random.nextGaussian() * sigma + mu);
						}
						
						if(x - 2*d1 > 0 && z + 2*d3 < O) {
							volMPD[x-d1][y][z+d3] = (float)((volMPD[x][y][z+d3]+volMPD[x-d1][y][z]
															+volMPD[x-d1][y-d2][z+d3]+volMPD[x-d1][y+d2][z+d3]+volMPD[x-2*d1][y][z+d3]
															+volMPD[x-d1][y][z+2*d3])/6
															+random.nextGaussian() * sigma + mu);
						}
						else if (x - 2*d1 > 0 && z + 2*d3 > O) {
							volMPD[x-d1][y][z+d3] = (float)((volMPD[x][y][z+d3]+volMPD[x-d1][y][z]
															+volMPD[x-d1][y-d2][z+d3]+volMPD[x-d1][y+d2][z+d3]+volMPD[x-2*d1][y][z+d3])/5
															+random.nextGaussian() * sigma + mu);
						}
						else if (x - 2*d1 < 0 && z + 2*d3 < O) {	
							volMPD[x-d1][y][z+d3] = (float)((volMPD[x][y][z+d3]+volMPD[x-d1][y][z]
															+volMPD[x-d1][y-d2][z+d3]+volMPD[x-d1][y+d2][z+d3]+volMPD[x-d1][y][z+2*d3])/5
															+random.nextGaussian() * sigma + mu);
						}
						else {
							volMPD[x-d1][y][z+d3] = (float)((volMPD[x][y][z+d3]+volMPD[x-d1][y][z]
															+volMPD[x-d1][y-d2][z+d3]+volMPD[x-d1][y+d2][z+d3])/4
															+random.nextGaussian() * sigma + mu);
						}
						
						if(x + 2*d1 < N && z + 2*d3 < O) {
							volMPD[x+d1][y][z+d3] = (float)((volMPD[x][y][z+d3]+volMPD[x+d1][y][z]
															+volMPD[x+d1][y-d2][z+d3]+volMPD[x+d1][y+d2][z+d3]+volMPD[x+2*d1][y][z+d3]
															+volMPD[x+d1][y][z+2*d3])/6
															+random.nextGaussian() * sigma + mu);
						}
						else if(x + 2*d1 < N && z + 2*d3 > O) {
							volMPD[x+d1][y][z+d3] = (float)((volMPD[x][y][z+d3]+volMPD[x+d1][y][z]
															+volMPD[x+d1][y-d2][z+d3]+volMPD[x+d1][y+d2][z+d3]+volMPD[x+2*d1][y][z+d3])/5
															+random.nextGaussian() * sigma + mu);
						}
						else if(x + 2*d1 > N && z + 2*d3 < O) {	
							volMPD[x+d1][y][z+d3] = (float)((volMPD[x][y][z+d3]+volMPD[x+d1][y][z]
															+volMPD[x+d1][y-d2][z+d3]+volMPD[x+d1][y+d2][z+d3]+volMPD[x+d1][y][z+2*d3])/5
															+random.nextGaussian() * sigma + mu);
						}
						else {
							volMPD[x+d1][y][z+d3] = (float)((volMPD[x][y][z+d3]+volMPD[x+d1][y][z]
															+volMPD[x+d1][y-d2][z+d3]+volMPD[x+d1][y+d2][z+d3])/4
													        +random.nextGaussian() * sigma + mu);
						}
						
						if(x + 2*d1 < N && z - 2*d3 > 0) {
							volMPD[x+d1][y][z-d3] = (float)((volMPD[x][y][z-d3]+volMPD[x+d1][y][z]
															+volMPD[x+d1][y-d2][z-d3]+volMPD[x+d1][y+d2][z-d3]+volMPD[x+2*d1][y][z-d3]
															+volMPD[x+d1][y][z-2*d3])/6
															+random.nextGaussian() * sigma + mu);
						}
						else if(x + 2*d1 < N && z - 2*d3 < 0) {	
							volMPD[x+d1][y][z-d3] = (float)((volMPD[x][y][z-d3]+volMPD[x+d1][y][z]
															+volMPD[x+d1][y-d2][z-d3]+volMPD[x+d1][y+d2][z-d3]+volMPD[x+2*d1][y][z-d3])/5
															+random.nextGaussian() * sigma + mu);
						}
						else if(x + 2*d1 > N && z - 2*d3 > 0) {	
							volMPD[x+d1][y][z-d3] = (float)((volMPD[x][y][z-d3]+volMPD[x+d1][y][z]
															+volMPD[x+d1][y-d2][z-d3]+volMPD[x+d1][y+d2][z-d3]+volMPD[x+d1][y][z-2*d3])/5
															+random.nextGaussian() * sigma + mu);
						}					
						else {
							volMPD[x+d1][y][z-d3] = (float)((volMPD[x][y][z-d3]+volMPD[x+d1][y][z]
															+volMPD[x+d1][y-d2][z-d3]+volMPD[x+d1][y+d2][z-d3])/4
															+random.nextGaussian() * sigma + mu);
						}
						
						if(y - 2*d2 > 0 && z - 2*d3 > 0) {
						// x fixed
							volMPD[x][y-d2][z-d3] = (float)((volMPD[x][y-d2][z]+volMPD[x][y][z-d3]
															+volMPD[x-d1][y-d2][z-d3]+volMPD[x+d1][y-d2][z-d3]+volMPD[x][y-2*d2][z]
															+volMPD[x][y][z-2*d3])/6
															+random.nextGaussian() * sigma + mu);
						}
						else if (y - 2*d2 > 0 && z - 2*d3 < 0) {
							volMPD[x][y-d2][z-d3] = (float)((volMPD[x][y-d2][z]+volMPD[x][y][z-d3]
															+volMPD[x-d1][y-d2][z-d3]+volMPD[x+d1][y-d2][z-d3]+volMPD[x][y-2*d2][z])/5
															+random.nextGaussian() * sigma + mu);
						}
						else if (y - 2*d2 < 0 && z - 2*d3 > 0) {
							volMPD[x][y-d2][z-d3] = (float)((volMPD[x][y-d2][z]+volMPD[x][y][z-d3]
															+volMPD[x-d1][y-d2][z-d3]+volMPD[x+d1][y-d2][z-d3]+volMPD[x][y][z-2*d3])/5
															+random.nextGaussian() * sigma + mu);
						}
						else {	
							volMPD[x][y-d2][z-d3] = (float)((volMPD[x][y-d2][z]+volMPD[x][y][z-d3]
															+volMPD[x-d1][y-d2][z-d3]+volMPD[x+d1][y-d2][z-d3])/4
															+random.nextGaussian() * sigma + mu);
						}
						
						if(y + 2*d2 < M && z - 2*d3 > 0) {
							volMPD[x][y+d2][z-d3] = (float)((volMPD[x][y+d2][z]+volMPD[x-d1][y][z]
															+volMPD[x-d1][y+d2][z-d3]+volMPD[x+d1][y+d2][z-d3]+volMPD[x][y+2*d2][z-d3]
															+volMPD[x][y+d2][z-2*d3])/6
															+random.nextGaussian() * sigma + mu);
						}
						else if(y + 2*d2 < M && z - 2*d3 < 0) {
							volMPD[x][y+d2][z-d3] = (float)((volMPD[x][y+d2][z]+volMPD[x-d1][y][z]
															+volMPD[x-d1][y+d2][z-d3]+volMPD[x+d1][y+d2][z-d3]+volMPD[x][y+2*d2][z-d3])/5
															+random.nextGaussian() * sigma + mu);
						}
						else if(y + 2*d2 > M && z - 2*d3 > 0) {
							volMPD[x][y+d2][z-d3] = (float)((volMPD[x][y+d2][z]+volMPD[x-d1][y][z]
															+volMPD[x-d1][y+d2][z-d3]+volMPD[x+d1][y+d2][z-d3]+volMPD[x][y+d2][z-2*d3])/5
															+random.nextGaussian() * sigma + mu);
						}
						else {
							volMPD[x][y+d2][z-d3] = (float)((volMPD[x][y+d2][z]+volMPD[x-d1][y][z]
															+volMPD[x-d1][y+d2][z-d3]+volMPD[x+d1][y+d2][z-d3])/4
															+random.nextGaussian() * sigma + mu);
						}
						
						if(y + 2*d2 < M && z + 2*d3 < O) {
							volMPD[x][y+d2][z+d3] = (float)((volMPD[x][y+d2][z]+volMPD[x][y][z+d3]
															+volMPD[x-d1][y+d2][z+d3]+volMPD[x+d1][y+d2][z+d3]+volMPD[x][y+2*d2][z+d3]
															+volMPD[x][y+d2][z+2*d3])/6
															+random.nextGaussian() * sigma + mu);
						}
						else if (y + 2*d2 < M && z + 2*d3 > O) {
							volMPD[x][y+d2][z+d3] = (float)((volMPD[x][y+d2][z]+volMPD[x][y][z+d3]
															+volMPD[x-d1][y+d2][z+d3]+volMPD[x+d1][y+d2][z+d3]+volMPD[x][y+2*d2][z+d3])/5
															+random.nextGaussian() * sigma + mu);
						}
						else if (y + 2*d2 > M && z + 2*d3 < O) {
							volMPD[x][y+d2][z+d3] = (float)((volMPD[x][y+d2][z]+volMPD[x][y][z+d3]
															+volMPD[x-d1][y+d2][z+d3]+volMPD[x+d1][y+d2][z+d3]+volMPD[x][y+d2][z+2*d3])/5
															+random.nextGaussian() * sigma + mu);
						}
						else {
							volMPD[x][y+d2][z+d3] = (float)((volMPD[x][y+d2][z]+volMPD[x][y][z+d3]
															+volMPD[x-d1][y+d2][z+d3]+volMPD[x+d1][y+d2][z+d3])/4
															+random.nextGaussian() * sigma + mu);
						}
						
						if(y - 2*d2 > 0 && z - 2*d3 > 0) {
							volMPD[x][y-d2][z+d3] = (float)((volMPD[x][y-d2][z]+volMPD[x][y][z+d3]
															+volMPD[x-d1][y-d2][z+d3]+volMPD[x+d1][y-d2][z+d3]+volMPD[x][y-2*d2][z-d3]
															+volMPD[x][y-d2][z-2*d3])/6
															+random.nextGaussian() * sigma + mu);
						}	
						else if (y - 2*d2 > 0 && z - 2*d3 < 0) {
							volMPD[x][y-d2][z+d3] = (float)((volMPD[x][y-d2][z]+volMPD[x][y][z+d3]
															+volMPD[x-d1][y-d2][z+d3]+volMPD[x+d1][y-d2][z+d3]+volMPD[x][y-2*d2][z-d3])/5
															+random.nextGaussian() * sigma + mu);
						}	
						else if (y - 2*d2 < 0 && z - 2*d3 > 0) {
							volMPD[x][y-d2][z+d3] = (float)((volMPD[x][y-d2][z]+volMPD[x][y][z+d3]
															+volMPD[x-d1][y-d2][z+d3]+volMPD[x+d1][y-d2][z+d3]+volMPD[x][y-d2][z-2*d3])/5
															+random.nextGaussian() * sigma + mu);
						}	
						else {
							volMPD[x][y-d2][z+d3] = (float)((volMPD[x][y-d2][z]+volMPD[x][y][z+d3]
															+volMPD[x-d1][y-d2][z+d3]+volMPD[x+d1][y-d2][z+d3])/4
															+random.nextGaussian() * sigma + mu);
						}			
					}	
				}	
			}		
			// half step sizes after each iteration
			d1 = d1/ 2;
			d2 = d2/ 2;
			d3 = d3/ 2;
			
			// Minimum step size is 1
			if(d1<1) {d1 = 1;}
			if(d2<1) {d2 = 1;}
			if(d3<1) {d3 = 1;}
			
		} //stage
		
		// scale to int
		float allMin = getMin(volMPD, N, M, O);
		float allMax = getMax(volMPD, N, M, O);		
			
		//int greyMax = 255;  		
		float scale = greyMax / (allMax - allMin);
		
		//resultVolume = new ArrayImgFactory<>(new UnsignedByteType()).create(datasetOut.dimension(0), datasetOut.dimension(1), datasetOut.dimension(2));
		resultVolume = opService.create().img(datasetOut, new UnsignedByteType());
		RandomAccess<UnsignedByteType> ra = resultVolume.randomAccess();
		
		// write to randomAccess	
		for (int k1 = 0; k1 < width; k1++) {
			for (int k2 = 0; k2 < height; k2++) {
				for (int k3 = 0; k3 < depth; k3++) {
					//floatArr[k1 + k2*N + k3*N*N] = fBm[k1][k2][k3];		
					ra.setPosition(k1, 0);
					ra.setPosition(k2, 1);
					ra.setPosition(k3, 2);
					//int value = (int) ((fBm[k1][k2][k3] - allMin) * scale);	 
					ra.get().setReal((int) Math.round((volMPD[k1][k2][k3] - allMin) * scale));
				}
			}
		}  	
    	//resultVolume
	}
    
	// Method for getting the maximum value
    public static float getMax(float[][][] inputArray, int N, int M, int O) { 
    	//float maxValue = inputArray[0][0][0]; 
      	float maxValue = -Float.MAX_VALUE;
		for (int k1 = 0; k1 < N; k1++) {
			for (int k2 = 0; k2 < M; k2++) {
				for (int k3 = 0; k3 < O; k3++) {
					if(inputArray[k1][k2][k3] > maxValue) { 
						maxValue = inputArray[k1][k2][k3]; 
        			}
				}
			} 
		} 
		return maxValue; 
    }
   
    // Method for getting the minimum value
    public static float getMin(float[][][] inputArray, int N, int M, int O) { 
    	//float minValue = inputArray[0][0][0]; 
    	float minValue = Float.MAX_VALUE; 
  		for (int k1 = 0; k1 < N; k1++) {
  			for (int k2 = 0; k2 < M; k2++) {
  				for (int k3 = 0; k3 < O; k3++) {
  					if(inputArray[k1][k2][k3] < minValue) { 
  						minValue = inputArray[k1][k2][k3]; 
          			}
  				}
  			} 
  		} 
  		return minValue; 
    } 
    
    private void compute3DFracMengerGrey(int numIterations, int greyMax) {
    	
//		numIterations = 10; // iteration
	 	int width  = (int)datasetOut.dimension(0);
    	int height = (int)datasetOut.dimension(1);
    	int depth  = (int)datasetOut.dimension(2);
		resultVolume = new ArrayImgFactory<>(new UnsignedByteType()).create(datasetOut.dimension(0), datasetOut.dimension(1), datasetOut.dimension(2));

	
//		// this algorithm properly works only for image sizes
//		// 2*3*3*3*3.......
//		int ifsWidth  = 2;
//		int ifsHeight = 2;
//		int ifsDepth  = 2;
//
//		while ((width > ifsWidth) && (height > ifsHeight) && (depth > ifsDepth)){
//			ifsWidth  = ifsWidth  * 3;
//			ifsHeight = ifsHeight * 3;
//			ifsDepth  = ifsDepth  * 3; 
//		}
		
		ifsVolume = new ArrayImgFactory<>(new FloatType()).create(width, height, depth);	
		raF = ifsVolume.randomAccess();

		//System.out.println("ImageGenerator:     width:   " + width);
		int tileSizeX = width/3;
		int tileSizeY = height/3;
		int tileSizeZ = depth/3;
		
		// set initial centered square
		int xMin = Math.round((float) width  / 3);
		int xMax = Math.round((float) width  / 3 * 2);
		int yMin = Math.round((float) height / 3);
		int yMax = Math.round((float) height / 3 * 2);
		int zMin = Math.round((float) depth  / 3);
		int zMax = Math.round((float) depth  / 3 * 2);
		
		for (int x = xMin - 1; x < xMax - 1; x++) {
		for (int y = yMin - 1; y < yMax - 1; y++) {
		for (int z = zMin - 1; z < zMax - 1; z++) {
			raF.setPosition(x, 0);
			raF.setPosition(y, 1);
			raF.setPosition(z, 2);
			raF.get().setReal(greyMax);
		}
		}
		}
		
		int dummy = 0;

//		// Affine transformation
		
		// declare how to interpolate the volume
		String interpolType = "Linear";
		InterpolatorFactory factory = null;
		if (interpolType.contentEquals("Linear") ) {
			// create an InterpolatorFactory RealRandomAccessible using linear interpolation
			factory = new NLinearInterpolatorFactory<FloatType>();
		}
		if (interpolType.contentEquals("Lanczos") ) {
			// create an InterpolatorFactory RealRandomAccessible using lanczos interpolation
			factory = new LanczosInterpolatorFactory<FloatType>();
		}
		if (interpolType.contentEquals("Floor") ) {
			// create an InterpolatorFactory RealRandomAccessible using floor interpolation
			factory = new FloorInterpolatorFactory<FloatType>();
		}
		if (interpolType.contentEquals("Nearest Neighbor") ) {
		// create an InterpolatorFactory RealRandomAccessible using nearst neighbor interpolation
		    factory = new NearestNeighborInterpolatorFactory<FloatType>();
		}
	
		Img<FloatType> ifsVolume2 = ifsVolume;
		for (int i = 0; i < numIterations; i++) {
			ifsVolume2 = ifsVolume.copy();
			RealRandomAccessible< FloatType > interpolant = Views.interpolate(Views.extendMirrorSingle(ifsVolume2), factory);
						
		
			AffineTransform3D at3D = new AffineTransform3D();
			//at3D.setTranslation(0.0, 0.0, 0.0);
			//at3D.set(m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23);
			at3D.set(1.0/3.0,     0.0,     0.0,   0.0,
					     0.0, 1.0/3.0,     0.0,   0.0,
					     0.0,     0.0, 1.0/3.0,   0.0);
			
			//Transform the image
			AffineRandomAccessible<FloatType, AffineGet> transformed = RealViews.affine(interpolant, at3D);
			
			//Apply the original interval to the transformed image 
			IntervalView<FloatType> bounded = Views.interval(transformed, ifsVolume);
//			Dataset dataset = datasetService.create(bounded);
//			uiService.show("bounded", bounded);
			
			//Add together
			Cursor<FloatType> cursorF = ifsVolume.cursor();
			RandomAccess<FloatType> raB = bounded.randomAccess();
	    	long[] pos = new long[3];
			while (cursorF.hasNext()) {
				cursorF.fwd();
				cursorF.localize(pos);
				raB.setPosition(pos);
				if (cursorF.get().getRealFloat() == 0) { //do not overwrite white pixels
					cursorF.get().set((int)Math.round(((FloatType)raB.get()).getRealFloat()));
				} 
			}  	
			
		}
			
		// Convert---------------------------------------
		cursor = resultVolume.cursor();
		raF = ifsVolume.randomAccess();
    	long[] pos = new long[3];
		while (cursor.hasNext()) {
			cursor.fwd();
			cursor.localize(pos);
			raF.setPosition(pos);
			cursor.get().set((int)Math.round((raF.get()).getRealFloat()));
		}  	
			
		ifsVolume = null;
    }
    

  
    
    /**
     * @param 
     * @throws Exception
     */
    @Override
    public void run() {
    	//WaitingDialogWithProgressBar dlgProgress = new WaitingDialogWithProgressBar("<html>Generating a 3D image volume, please wait...<br>Open console window for further info.</html>");
		WaitingDialogWithProgressBar dlgProgress = new WaitingDialogWithProgressBar("Generating a 3D image volume, please wait... Open console window for further info.",
		                                                                             logService, false, null); //isCanceable = false, because no following method listens to exec.shutdown 

		dlgProgress.updatePercent("");
		dlgProgress.setBarIndeterminate(true);
		dlgProgress.setVisible(true);
		
    	long startTimeAll = System.currentTimeMillis();
         // create the ImageJ application context with all available services
    	//final ImageJ ij = new ImageJ();
    	//ij.ui().showUI();

    	
//    	final MessageType messageType = MessageType.QUESTION_MESSAGE;
//		final OptionType optionType = OptionType.OK_CANCEL_OPTION;
//
//		// Prompt for confirmation.
//		//final UIService uiService = getContext().getService(UIService.class);
//		Result result = uiService.showDialog("Compute a 3D fractal?", "FractalCreation3D", messageType, optionType);
//
//		// Cancel the command execution if the user does not agree.
//		//if (result != Result.YES_OPTION) System.exit(-1);
//		if (result != Result.YES_OPTION) return;
    		
		//collect parameters
		int width     			= spinnerInteger_Width;
		int height    			= spinnerInteger_Height;
		int depth    			= spinnerInteger_Depth;
		String colorModelType   = choiceRadioButt_ColorModelType;//"Grey-8bit", "Color-RGB"
		String volumeType		= choiceRadioButt_VolumeType;
		int greyR   			= spinnerInteger_R;
		int greyG   			= spinnerInteger_G;
		int greyB   			= spinnerInteger_B;
		float fracDim 			= spinnerFloat_FracDim;
		int numIterations		= spinnerInteger_NumIterations;
	
		// Create an image.
		
		String name = "3D image stack";
		if 		(volumeType.equals("Random"))   								name = "Random image stack";
		else if (volumeType.equals("Gaussian")) 								name = "Gaussian image stack";
		else if (volumeType.equals("Constant")) 								name = "Constant image stack";
		else if (volumeType.equals("Fractal volume - FFT"))						name = "Fractal volume - FFT";
		else if (volumeType.equals("Fractal volume - MPD"))						name = "Fractal volume - MPD";
		else if (volumeType.equals("Fractal IFS - Menger"))						name = "Fractal IFS - Menger";
		else if (volumeType.equals("Fractal IFS - Sierpinski-1"))				name = "Fractal IFS - Sierpinski-1";
		else if (volumeType.equals("Fractal IFS - Sierpinski-2"))				name = "Fractal IFS - Sierpinski-2";
				
		AxisType[] axes  = null;
		long[] dims 	 = null;
		int bitsPerPixel = 0;
		boolean signed   = false;
		boolean floating = false;
		boolean virtual  = false;

		//dataset = ij.dataset().create(dims, name, axes, bitsPerPixel, signed, floating);
		//datasetOut = datasetService.create(dims, name, axes, bitsPerPixel, signed, floating, virtual);	
		//RandomAccess<T> randomAccess = (RandomAccess<T>) dataset.getImgPlus().randomAccess();
		
		if (colorModelType.equals("Grey-8bit")) {

				bitsPerPixel = 8;
				dims = new long[]{width, height, depth};
				axes = new AxisType[]{Axes.X, Axes.Y, Axes.Z};
				datasetOut = datasetService.create(dims, name, axes, bitsPerPixel, signed, floating, virtual);
					
				RandomAccess<RealType<?>> ra;
				Cursor<UnsignedByteType> cursor;
				long[] pos3D; 
				float value;
				long startTime;
				long duration;
				dlgProgress.setBarIndeterminate(false);
	
				startTime = System.currentTimeMillis();
				logService.info(this.getClass().getName() + " Generating image volume");
				
				if      (volumeType.equals("Random"))   					compute3DRandomGrey(greyR);
				else if (volumeType.equals("Gaussian")) 					compute3DGaussianGrey(greyR);
				else if (volumeType.equals("Constant")) 					compute3DConstantGrey(greyR);
				else if (volumeType.equals("Fractal volume - FFT"))			compute3DFracFFTGrey(fracDim, greyR);
				else if (volumeType.equals("Fractal volume - MPD")) 		compute3DFracMPDGrey(fracDim, greyR);
				else if (volumeType.equals("Fractal IFS - Menger")) 		compute3DFracMengerGrey(numIterations, greyR);
				else if (volumeType.equals("Fractal IFS - Sierpinski-1")) 	;//compute3DSierpinski1Grey(numIterations, greyR);
				else if (volumeType.equals("Fractal IFS - Sierpinski-2")) 	;//compute3DSierpinski2Grey(numIterations, greyR);
				
									
				ra = datasetOut.randomAccess();
				cursor = resultVolume.cursor();
				pos3D = new long[3];
				
				while (cursor.hasNext()) {
					cursor.fwd();
					cursor.localize(pos3D);
					value= cursor.get().getRealFloat();
					//value = rf * (value -min); //Rescale to 0  255
					pos3D = new long[] {pos3D[0], pos3D[1], pos3D[2]};
					ra.setPosition(pos3D);
					ra.get().setReal(value);
				}
				duration = System.currentTimeMillis() - startTime;
				TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
				SimpleDateFormat sdf = new SimpleDateFormat();
				sdf.applyPattern("HHH:mm:ss:SSS");
				logService.info(this.getClass().getName() + " Elapsed time: "+ sdf.format(duration));
								
		}
		else if (colorModelType.equals("Color-RGB")) {
	
				bitsPerPixel = 8;
				dims = new long[]{width, height, depth, 3};
				axes = new AxisType[]{Axes.X, Axes.Y, Axes.Z, Axes.CHANNEL};
				datasetOut = datasetService.create(dims, name, axes, bitsPerPixel, signed, floating, virtual);
				datasetOut.setCompositeChannelCount(3);
				datasetOut.setRGBMerged(true);
					
				RandomAccess<RealType<?>> ra = datasetOut.randomAccess();
				Cursor<UnsignedByteType> cursor;
				long[] pos4D;
				float value;
				long startTime;
				long duration;
				dlgProgress.setBarIndeterminate(false);

				startTime = System.currentTimeMillis();
				logService.info(this.getClass().getName() + " Generating image volume");
				
		
				if      (volumeType.equals("Random"))   					compute3DRandomRGB(greyR, greyG, greyB);
				else if (volumeType.equals("Gaussian")) 					compute3DGaussianRGB(greyR, greyG, greyB);
				else if (volumeType.equals("Constant")) 					compute3DConstantRGB(greyR, greyG, greyB);
				else if (volumeType.equals("Fractal volume - FFT"))			;//compute3DFracFFTRGB(greyR, greyG, greyB); //not implemented yet
				else if (volumeType.equals("Fractal volume - MPD")) 		;//compute3DFracMPDRGB(greyR, greyG, greyB); //not implemented yet
				else if (volumeType.equals("Fractal IFS - Menger")) 		;//compute3DFracMengerRGB(numIterations, greyR);
				else if (volumeType.equals("Fractal IFS - Sierpinski-1")) 	;//compute3DSierpinski1RGB(numIterations, greyR);
				else if (volumeType.equals("Fractal IFS - Sierpinski-2")) 	;//compute3DSierpinski2RGB(numIterations, greyR);
				
				cursor = resultVolume.cursor();
				pos4D = new long[4];		
				while (cursor.hasNext()) {
					cursor.fwd();
					cursor.localize(pos4D);
					value= cursor.get().getRealFloat();
					//value = rf * (value -min); //Rescale to 0  255		
					ra.setPosition(new long[] {pos4D[0], pos4D[1], pos4D[2], pos4D[3]});
					ra.get().setReal(value);
				}
				
				duration = System.currentTimeMillis() - startTime;
				TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
				SimpleDateFormat sdf = new SimpleDateFormat();
				sdf.applyPattern("HHH:mm:ss:SSS");
				logService.info(this.getClass().getName() + " Elapsed time: "+ sdf.format(duration));		
		}
		
		statusService.showProgress(0, 100);
		statusService.clearStatus();
		
		dlgProgress.addMessage("Processing finished! Displaying image volume...");
		//not necessary because datasetOut is an IO type
		//ij.ui().show("Image", datasetOut);
		//if (choiceRadioButt_VolumeType.equals("Random"))   uiService.show("Random",   datasetOut);
		//if (choiceRadioButt_VolumeType.equals("Constant")) uiService.show("Constant", datasetOut);
		uiService.show(datasetOut.getName(), datasetOut);
		
		long duration = System.currentTimeMillis() - startTimeAll;
		TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
		SimpleDateFormat sdf = new SimpleDateFormat();
		sdf.applyPattern("HHH:mm:ss:SSS");
		logService.info(this.getClass().getName() + " Elapsed time for all images: "+ sdf.format(duration));
		dlgProgress.setVisible(false);
		dlgProgress.dispose();
    }

   
    
   

	public static void main(final String... args) throws Exception {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch(Throwable t) {
		
		}
//        // create the ImageJ application context with all available services
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
//
//        // ask the user for a file to open
//        final File file = ij.ui().chooseFile(null, "open");
//
//        if (file != null) {
//            // load the dataset
//            final Dataset dataset = ij.scifio().datasetIO().open(file.getPath());
//
//            // show the image
//            ij.ui().show(dataset);
//
//            // invoke the plugin
//            ij.command().run(FracCreate3D.class, true);
//        }
//       
         //invoke the plugin
         ij.command().run(Img3DVolumeGenerator.class, true);
    	
    }

}
