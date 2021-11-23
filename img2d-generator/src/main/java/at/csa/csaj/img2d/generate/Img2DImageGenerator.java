/*-
 * #%L
 * Project: ImageJ plugin to generate 2D images.
 * File: Img2DImageGenerator.java
 * 
 * $Id$
 * $HeadURL$
 * 
 * This file is part of ComsystanJ software, hereinafter referred to as "this program".
 * %%
 * Copyright (C) 2021 Comsystan Software
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

package at.csa.csaj.img2d.generate;

import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ImageJ;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;

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
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_2D;
import io.scif.services.DatasetIOService;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
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
 * This is an ImageJ {@link Command} plugin for generation of images.
 * <p>
 * The {@link run} method implements the computations.
 * </p>
 * @param <C>
 */
@Plugin(type = ContextCommand.class, label = "Image generator", menu = {
        @Menu(label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT, mnemonic = MenuConstants.PLUGINS_MNEMONIC),
        @Menu(label = "ComsystanJ"),
        @Menu(label = "Image (2D)"),
        @Menu(label = "Image generator", weight = 2)})
public class Img2DImageGenerator<T extends RealType<T>, C> extends ContextCommand implements Previewable { //modal GUI with cancel
	
	private static final String PLUGIN_LABEL = "<html><b>Generates 2D images</b></html>";
	private static final String SPACE_LABEL = "";
  
	@Parameter
	private LogService logService;

	@Parameter
	private StatusService statusService;
	
	@Parameter (label = "Generated image",type = ItemIO.OUTPUT) //so that it can be displayed
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

	private Img<FloatType> imgFloat;
	private Img<UnsignedByteType> resultImg;
	private Img<FloatType> mpdImg; //Midpoint displacement 
	private Img<FloatType> sosImg; //sum of sine
	private Img<UnsignedByteType> hrmImg; //HRM
	BufferedImage  ifsBuffImg; //IFS  Menger,....
	WritableRaster ifsRaster;
    
	//Widget elements------------------------------------------------------
	//-----------------------------------------------------------------------------------------------------
//  @Parameter(label = " ", visibility = ItemVisibility.MESSAGE)
//	private final String labelPlugin = PLUGIN_LABEL;

    @Parameter(label = " ", visibility = ItemVisibility.MESSAGE)
  	private final String labelSpace = SPACE_LABEL;
    
    @Parameter(label = "Width [pixel]",
    		   description = "Width of output image in pixel",
    		   style = NumberWidget.SPINNER_STYLE,
    		   min = "1",
    		   max = "32768",
    		   initializer = "initialWidth",
    		   stepSize = "1",
    		   callback = "changedWidth")
    private int spinnerInteger_Width;
    
    @Parameter(label = "Height [pixel]",
    		   description = "Height of output image in pixel",
 		       style = NumberWidget.SPINNER_STYLE,
 		       min = "1",
 		       max = "32768",
 		       initializer = "initialHeight",
 		       stepSize = "1",
 		       callback = "changedHeight")
    private int spinnerInteger_Height;
    
    @Parameter(label = "Number of images",
 	   	   description = "Number of output images",
	  		   style = NumberWidget.SPINNER_STYLE,
	  		   min = "1",
	  		   max = "999999999999999999999",
	  		   initializer = "initialNumImages",
	  		   stepSize = "1",
	  		   callback = "changedNumImages")
    private int spinnerInteger_NumImages;
    
    @Parameter(label = "Color model",
			description = "Color model of output image",
			style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
			choices = {"Grey-8bit", "Color-RGB"}, //
			//persist  = false,  //restore previous value default = true
			initializer = "initialColorModelType",
			callback = "callbackColorModelType")
	private String choiceRadioButt_ColorModelType;
    
    @Parameter(label = "Image type",
    		   description = "Type of output image, FFT..Fast Fourier transform, MPD..Midpoint displacement, HRM..Hirarchical random maps, IFS..Iterated function system",
    		   style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
    		   choices = {"Random", "Gaussian", "Sine - radial", "Sine - horizontal", "Sine - vertical",  "Constant", 
    				   "Fractal surface - FFT", "Fractal surface - MPD", "Fractal surface - Sum of sine", "Fractal - HRM",
    				   "Fractal IFS - Menger", "Fractal IFS - Sierpinski-1", "Fractal IFS - Sierpinski-2",
    				   "Fractal IFS - Koch snowflake",  "Fractal IFS - Fern", "Fractal IFS - Heighway dragon"},
               callback = "changedImageType")
    private String choiceRadioButt_ImageType;
    
    @Parameter(label = "Grey/R",
    		   description = "Grey value of Grey image or of the RGB R channel",
	  		   style = NumberWidget.SPINNER_STYLE,
	  		   min = "0",
	  		   max = "255",
	  		   initializer = "initialR",
	  		   stepSize = "1",
	  		   callback = "changedR")
    private int spinnerInteger_R;
    
    @Parameter(label = "G",
    		   description = "Grey value of the RGB G channel",
	  		   style = NumberWidget.SPINNER_STYLE,
	  		   min = "0",
	  		   max = "255",
	  		   initializer = "initialG",
	  		   stepSize = "1",
	  		   callback = "changedG")
    private int spinnerInteger_G;
    
    @Parameter(label = "B",
    		   description = "Grey value of the RGB B channel",
	  		   style = NumberWidget.SPINNER_STYLE,
	  		   min = "0",
	  		   max = "255",
	  		   initializer = "initialB",
	  		   stepSize = "1",
	  		   callback = "changedB")
    private int spinnerInteger_B;
    
    @Parameter(label = "(Fractal surface) Dimension",
    		   description = "Fractal dimension of fractal surface in the range [2,3]",
	  		   style = NumberWidget.SPINNER_STYLE,
	  		   min = "1.99", //otherwise 2 cannot be reached becaus of these float errors
	  		   max = "3",
	  		   initializer = "initialFracDim",
	  		   stepSize = "0.1",
	  		   callback = "changedFracDim")
    private float spinnerFloat_FracDim;
    
    @Parameter(label = "(Sine/Sum of sine) Frequency",
 		   description = "Frequency for Sine or Sum of sine method ",
	  		   style = NumberWidget.SPINNER_STYLE,
	  		   min = "0", 
	  		   max = "99999999999999999999",
	  		   initializer = "initialSineSumOfSineFrequency",
	  		   stepSize = "1",
	  		   callback = "changedSineSumOfSineFrequency")
    private float spinnerFloat_SineSumOfSineFrequency;
    
    @Parameter(label = "(Sum of sine) Amplitude",
  		   description = "Amplitude for Sum of sine method ",
 	  		   style = NumberWidget.SPINNER_STYLE,
 	  		   min = "0", 
 	  		   max = "99999999999999999999",
 	  		   initializer = "initialSumOfSineAmplitude",
 	  		   stepSize = "1",
 	  		   callback = "changedSumOfSineAmplitude")
    private float spinnerFloat_SumOfSineAmplitude;

    @Parameter(label = "(Sum of sine/IFS) Iterations",
 	   	       description = "Number of iterations for Sum of sine method",
	  		   style = NumberWidget.SPINNER_STYLE,
	  		   min = "1",
	  		   max = "999999999999999999999",
	  		   initializer = "initialNumSumOfSineIterations",
	  		   stepSize = "1",
	  		   callback = "changedNumSumOfSineIterations")
    private int spinnerInteger_NumSumOfSineIterations;
    
    @Parameter(label = "(HRM) Probability 1",
    		   description = "Probability of first level",
  	  		   style = NumberWidget.SPINNER_STYLE,
  	  		   min = "0", 
  	  		   max = "1",
  	  		   initializer = "initialHRMProbability1",
  	  		   stepSize = "0.01",
  	  		   callback = "changedHRMProbability1")
    private float spinnerFloat_HRMProbability1;
    
    @Parameter(label = "(HRM) Probability 2",
 		       description = "Probability of second level",
	  		   style = NumberWidget.SPINNER_STYLE,
	  		   min = "0", 
	  		   max = "1",
	  		   initializer = "initialHRMProbability2",
	  		   stepSize = "0.01",
	  		   callback = "changedHRMProbability2")
    private float spinnerFloat_HRMProbability2;
    
    @Parameter(label = "(HRM) Probability 3",
 		       description = "Probability of third level",
	  		   style = NumberWidget.SPINNER_STYLE,
	  		   min = "0", 
	  		   max = "1",
	  		   initializer = "initialHRMProbability3",
	  		   stepSize = "0.01",
	  		   callback = "changedHRMProbability3")
    private float spinnerFloat_HRMProbability3;
    
    @Parameter(label = "(Koch) Number of polygons",
	   	       description = "Starting number of polygons for Koch snowflake",
	  		   style = NumberWidget.SPINNER_STYLE,
	  		   min = "3",
	  		   max = "999999999999999999999",
	  		   initializer = "initialNumPolygons",
	  		   stepSize = "1",
	  		   callback = "changedNumPolygons")
    private int spinnerInteger_NumPolygons;
    
    //---------------------------------------------------------------------
    
    
    //The following initializer functions set initial values	
    protected void initialWidth() {
    	spinnerInteger_Width = 512;
    }

    protected void initialHeight() {
    	spinnerInteger_Height = 512;
    }
    
    protected void initialNumImages() {
    	spinnerInteger_NumImages = 1;
    }

    protected void initialColorModelType() {
		choiceRadioButt_ColorModelType = "Grey-8bit";
	}

    protected void initialImageType() {
    	choiceRadioButt_ImageType = "Random";
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
	 	spinnerFloat_FracDim = 2.5f;
	}

	protected void initialSineSumOfSineFrequency() {
	 	//round to one decimal after the comma
	 	spinnerFloat_SineSumOfSineFrequency = 2f;
	}
	
	protected void initialSumOfSineAmplitude() {
	 	//round to one decimal after the comma
	 	spinnerFloat_SumOfSineAmplitude = 2f;
	}
	
	protected void initialNumSumOfSineIterations() {
		spinnerInteger_NumSumOfSineIterations = 10;
	}
	
	protected void initialHRMProbability1() {
	 	//round to two decimal after the comma
	 	spinnerFloat_HRMProbability1 = 0.5f;
	}
	
	protected void initialHRMProbability2() {
	 	//round to two decimal after the comma
	 	spinnerFloat_HRMProbability2 = 0.5f;
	}
	
	protected void initialHRMProbability3() {
	 	//round to two decimal after the comma
	 	spinnerFloat_HRMProbability3 = 0.5f;
	}
	
	protected void initialNumPolygons() {
		spinnerInteger_NumPolygons = 3;
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
	
	/** Executed whenever the {@link #spinnerInteger_NumImages} parameter changes. */
	protected void changedNumImages() {
		logService.info(this.getClass().getName() + " Number of images changed to " + spinnerInteger_NumImages);
	}
	
	/** Executed whenever the {@link #choiceRadioButt_ColorModelType} parameter changes. */
	protected void callbackColorModelType() {
		logService.info(this.getClass().getName() + " Color model set to " + choiceRadioButt_ColorModelType);
	}
	
	/** Executed whenever the {@link #choiceRadioButt_ImageType} parameter changes. */
	protected void changedImageType() {
		logService.info(this.getClass().getName() + " Image type changed to " + choiceRadioButt_ImageType);
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
	
	protected void changedSineSumOfSineFrequency() {
		//logService.info(this.getClass().getName() + " Sum of sine frequency changed to " + spinnerFloat_SineSumOfSineFrequency);
	 	//round to ?? decimal after the comma
	 	//spinnerFloat_SineSumOfSineFrequency = Math.round(spinnerFloat_SineSumOfSineFrequency * 1f)/1f;
	 	spinnerFloat_SineSumOfSineFrequency = Precision.round(spinnerFloat_SineSumOfSineFrequency, 0);
	 	logService.info(this.getClass().getName() + " Sum of sine frequency changed to " + spinnerFloat_SineSumOfSineFrequency);
	}
	
	protected void changedSumOfSineAmplitude() {
		//logService.info(this.getClass().getName() + " Sum of sine amplitude changed to " + spinnerFloat_SumOfSineAmplitude);
	 	//round to ?? decimal after the comma
	 	//spinnerFloat_SumOfSineAmplitude = Math.round(spinnerFloat_SumOfSineAmplitude * 1f)/1f;
	 	spinnerFloat_SumOfSineAmplitude = Precision.round(spinnerFloat_SumOfSineAmplitude, 0);
	 	logService.info(this.getClass().getName() + " Sum of sine amplitude changed to " + spinnerFloat_SumOfSineAmplitude);
	}
	
	/** Executed whenever the {@link #spinnerInteger_NumSumOfSineIterations} parameter changes. */
	protected void changedNumSumOfSineIterations() {
		logService.info(this.getClass().getName() + " Sum of sine iterations changed to " + spinnerInteger_NumSumOfSineIterations);
	}
	
	protected void changedHRMProbability1() {
		//logService.info(this.getClass().getName() + " Sum of sine amplitude changed to " + spinnerFloat_HRMProbability);
	 	//round to ?? decimal after the comma
	 	//spinnerFloat_HRMProbability = Math.round(spinnerFloat_HRMProbability * 1f)/1f;
	 	spinnerFloat_HRMProbability1 = Precision.round(spinnerFloat_HRMProbability1, 2);
	 	logService.info(this.getClass().getName() + " Probability 1 changed to " + spinnerFloat_HRMProbability1);
	}
	
	protected void changedHRMProbability2() {
		//logService.info(this.getClass().getName() + " Sum of sine amplitude changed to " + spinnerFloat_HRMProbability);
	 	//round to ?? decimal after the comma
	 	//spinnerFloat_HRMProbability = Math.round(spinnerFloat_HRMProbability * 1f)/1f;
	 	spinnerFloat_HRMProbability2 = Precision.round(spinnerFloat_HRMProbability2, 2);
	 	logService.info(this.getClass().getName() + " Probability 2 changed to " + spinnerFloat_HRMProbability2);
	}
	
	protected void changedHRMProbability3() {
		//logService.info(this.getClass().getName() + " Sum of sine amplitude changed to " + spinnerFloat_HRMProbability);
	 	//round to ?? decimal after the comma
	 	//spinnerFloat_HRMProbability = Math.round(spinnerFloat_HRMProbability * 1f)/1f;
	 	spinnerFloat_HRMProbability3 = Precision.round(spinnerFloat_HRMProbability3, 2);
	 	logService.info(this.getClass().getName() + " Probability 3 changed to " + spinnerFloat_HRMProbability3);
	}
	
	/** Executed whenever the {@link #spinnerInteger_NumPolygons} parameter changes. */
	protected void changedNumPolygons() {
		logService.info(this.getClass().getName() + " Number of polygons changed to " + spinnerInteger_NumPolygons);
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
 	
    
    private void computeRandomImage(int greyValueMax) {
    
    	Random random = new Random();
    	resultImg = new ArrayImgFactory<>(new UnsignedByteType()).create(datasetOut.dimension(0), datasetOut.dimension(1));
    	Cursor<UnsignedByteType> cursor = resultImg.cursor();
    	//final long[] pos = new long[dataset.numDimensions()];
		while (cursor.hasNext()) {
			cursor.fwd();
			//cursor.localize(pos);
			cursor.get().setReal((int)(random.nextFloat()*greyValueMax));
		}  			
	}
    
    private void computeGaussianImage(int greyValueMax) {
        
    	float greyMiddle = (float)greyValueMax/2.f;
    	Random random = new Random();
    	resultImg = new ArrayImgFactory<>(new UnsignedByteType()).create(datasetOut.dimension(0), datasetOut.dimension(1));
    	Cursor<UnsignedByteType> cursor = resultImg.cursor();
    	//final long[] pos = new long[dataset.numDimensions()];
		while (cursor.hasNext()) {
			cursor.fwd();
			//cursor.localize(pos);
			cursor.get().setReal((int)(random.nextGaussian()*30f + greyMiddle));
		}  			
	}
    
    private void computeSineImage(String type, float frequency, int greyValueMax) {
	  
	    long width  = datasetOut.dimension(0);   
	    long height = datasetOut.dimension(1);
	 
    	Random random = new Random();
    	resultImg = new ArrayImgFactory<>(new UnsignedByteType()).create(width, height);
    	RandomAccess<UnsignedByteType> resultImgRa = resultImg.randomAccess();
    	float offsetX = ((float)width-1f)/2f;
    	float offsetY = ((float)height-1f)/2f;
    	float radius = (float) Math.sqrt(offsetX*offsetX + offsetY*offsetY);
    	float value = 0;
    	float omega = (float) (frequency*2*Math.PI);
    	float phi   = (float) (frequency*2*Math.PI/4);
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				if (type.equals("radial")) {
					value = (float) (Math.sin(Math.sqrt((x-offsetX)*(x-offsetX) + (y-offsetY)*(y-offsetY)) / radius * omega + phi));
				}
				else if (type.equals("horizontal")) {
					value = (float) (Math.sin((float) x / (width - 1) * omega + phi));
				}
				else if (type.equals("vertical")) {
					value = (float) (Math.sin((float) y / (width - 1) * omega + phi));
				}
				value = (value + 1.0f) / 2.0f * greyValueMax;  //Make positive and Normalize, greyValueMax up to 255
				resultImgRa.setPosition(x, 0);
				resultImgRa.setPosition(y, 1);
				resultImgRa.get().set((byte)value);
			}
		}
	}

    private void computeConstantImage(int constant) {
        
    	resultImg = new ArrayImgFactory<>(new UnsignedByteType()).create(datasetOut.dimension(0), datasetOut.dimension(1));
    	Cursor<UnsignedByteType> cursor = resultImg.cursor();
    	
    	//final long[] pos = new long[dataset.numDimensions()];
		while (cursor.hasNext()) {
			cursor.fwd();
			//cursor.localize(pos);
			cursor.get().setReal(constant);
		}  			
	}
    
    private void computeFrac2DFFT(float fracDim, int greyValueMax) {
    	   
    	int width  = (int)datasetOut.dimension(0);
    	int height = (int)datasetOut.dimension(1);
    	Cursor<FloatType> cursorF;
    	long[] pos;
   
		//create empty image
		imgFloat = new ArrayImgFactory<>(new FloatType()).create(width, height);
		
		//optionally set grey values
//		cursorF = imgFloat.localizingCursor();
//		while (cursorF.hasNext()) {
//			cursorF.fwd();
//			cursorF.get().set(133f);
//		}
//		uiService.show("imgFloat", imgFloat);

//		ops filter fft seems to be a Hadamard transform rather than a true FFT
//		output size is automatically padded, so has rather strange dimensions.
//		output is vertically symmetric 
//		F= 0 is at (0.0) and (0,SizeY)
//		imgFloat = this.createImgFloat(raiWindowed);
//		RandomAccessibleInterval<C> raifft = opService.filter().fft(imgFloat);
//		
//		//This would also work with identical output 
//		ImgFactory<ComplexFloatType> factory = new ArrayImgFactory<ComplexFloatType>(new ComplexFloatType());
//		int numThreads = 6;
//		final FFT FFT = new FFT();
//		Img<ComplexFloatType> imgCmplx = FFT.realToComplex((RandomAccessibleInterval<R>) raiWindowed, factory, numThreads);

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
		double[][] imgA = new double[rows][2*columns]; //Every frequency entry needs a pair of columns: for real and imaginary part
		DoubleFFT_2D FFT = new DoubleFFT_2D(rows, columns); //Here always the simple DFT width
		
		//Forward FFT of imgA
		//is not really necessary because imgA will be overwritten after Forward FFT
//		cursorF = imgFloat.localizingCursor();
//		pos = new long[2];
//		while (cursorF.hasNext()) {
//			cursorF.fwd();
//			cursorF.localize(pos); 
//			//JTransform needs rows and columns swapped!!!!!
//			imgA[(int)pos[1]][(int)(pos[0])] = cursorF.get().get();
//		}
//		
//		//JTransform needs rows and columns swapped!!!!!
//		////FFT.realForward(imgA);  //The first two columns are not symmetric and seem to be not right
//		FFT.realForwardFull(imgA);  //The right part is not symmetric!!
//		////Power image constructed later is also not exactly symmetric!!!!!
		
		
		//Optionally show FFT Real Imag image
		//************************************************************************************
//		ArrayImg<FloatType, ?> imgFFT = new ArrayImgFactory<>(new FloatType()).create(2*dftWidth, dftHeight); //always single 2D
//		cursorF = imgFFT.localizingCursor();
//		pos = new long[2];
//		while (cursorF.hasNext()){
//			cursorF.fwd();
//			cursorF.localize(pos);
//			//JTransform needs rows and columns swapped!!!!!
//			cursorF.get().set((float)imgA[(int)pos[1]][(int)pos[0]]);
//		}		
//		//Get min max
//		float min = Float.MAX_VALUE;
//		float max = -Float.MAX_VALUE;
//		float valF;
//		cursorF = imgFFT.cursor();
//		while (cursorF.hasNext()) {
//			cursorF.fwd();
//			valF = cursorF.get().get();
//			if (valF > max) max = valF;
//			if (valF < min) min = valF;
//		}	
//		//Rescale to 0...255
//		cursorF = imgFFT.cursor();
//		while (cursorF.hasNext()) {
//			cursorF.fwd();
//			cursorF.localize(pos);
//			cursorF.get().set(255f*(cursorF.get().get() - min)/(max - min));		
//		}	
//		uiService.show("FFT", imgFFT);	
		//************************************************************************************
		
		
		// Declare an array to hold the current position of the cursor.
		long[] posFFT = new long[2];
		
		//Get power values
		final long[] origin1  = {0, 0};         	 //left top
		final long[] origin2  = {0, rows-1};    	 //left bottom
		final long[] origin3  = {columns-1, 0}; 	 //right top
		final long[] origin4  = {columns-1, rows-1}; //right bottom
		
		// generate random pixel values
		Random generator = new Random();
		double b = 8.0f - (2.0f * fracDim);		// FD = (B+6)/2 laut Closed contour fractal dimension estimation... J.B. Florindo
		double dist1;
		double dist2;
		double dist3;
		double dist4;
		double g;
		double u;
		double n;
		double m;
				
		//set FFT real and imaginary values
		for (int k1 = 0; k1 < rows/2; k1++) {
			for (int k2 = 0; k2 < columns/2; k2++) {
				posFFT[1] = k1;
				posFFT[0] = k2;
				dist1  = Util.distance(origin1, posFFT); //Distance
				g = generator.nextGaussian();
				u = generator.nextFloat();
				n = g * Math.cos(2 * Math.PI * u);
				m = g * Math.sin(2 * Math.PI * u);
				n = n * Math.pow(dist1+1, -b / 2);
				m = m * Math.pow(dist1+1, -b / 2);
				imgA[k1][2*k2]   = n; //(2*x)  ...Real part
				imgA[k1][2*k2+1] = m; //(2*x+1)...Imaginary part 
			}
		}
		
		for (int k1 = rows/2; k1 < rows; k1++) {
			for (int k2 = 0; k2 < columns/2; k2++) {
				posFFT[1] = k1;
				posFFT[0] = k2;
				dist2  = Util.distance(origin2, posFFT); //Distance
				g = generator.nextGaussian();
				u = generator.nextFloat();
				n = g * Math.cos(2 * Math.PI * u);
				m = g * Math.sin(2 * Math.PI * u);
				n = n * Math.pow(dist2+1, -b / 2);
				m = m * Math.pow(dist2+1, -b / 2);
				imgA[k1][2*k2]   = n; //(2*x)  ...Real part
				imgA[k1][2*k2+1] = m; //(2*x+1)...Imaginary part 
			}
		}	
		for (int k1 = 0; k1 < rows/2; k1++) {
			for (int k2 = columns/2; k2 < columns; k2++) {
				posFFT[1] = k1;
				posFFT[0] = k2;
				dist3  = Util.distance(origin3, posFFT); //Distance
				g = generator.nextGaussian();
				u = generator.nextFloat();
				n = g * Math.cos(2 * Math.PI * u);
				m = g * Math.sin(2 * Math.PI * u);
				n = n * Math.pow(dist3+1, -b / 2);
				m = m * Math.pow(dist3+1, -b / 2);
				imgA[k1][2*k2]   = n; //(2*x)  ...Real part
				imgA[k1][2*k2+1] = m; //(2*x+1)...Imaginary part 
			}
		}
		for (int k1 = rows/2; k1 < rows; k1++) {
			for (int k2 = columns/2; k2 < columns; k2++) {
				posFFT[1] = k1;
				posFFT[0] = k2;
				dist4  = Util.distance(origin4, posFFT); //Distance
				g = generator.nextGaussian();
				u = generator.nextFloat();
				n = g * Math.cos(2 * Math.PI * u);
				m = g * Math.sin(2 * Math.PI * u);
				n = n * Math.pow(dist4+1, -b / 2);
				m = m * Math.pow(dist4+1, -b / 2);
				imgA[k1][2*k2]   = n; //(2*x)  ...Real part
				imgA[k1][2*k2+1] = m; //(2*x+1)...Imaginary part
			}
		}
		
		//uiService.show("img", img);
		//uiService.show("fft", fft);
		
		//Inverse FFT and show image
		//imgA is now really complex, Real and Imaginary pairs
		FFT.complexInverse(imgA, false);
		
		cursorF = imgFloat.localizingCursor();
		pos = new long[2];
		while (cursorF.hasNext()) {
			cursorF.fwd();
			cursorF.localize(pos); 
			//JTransform needs rows and columns swapped!!!!!
			cursorF.get().set((float) imgA[(int)pos[1]][(int)(2*pos[0])]);
		}
		
		//uiService.show("imgFloat after Inverse FFT", imgFloat);	
		
		//Change from FloatType to UnsignedByteType
		//Find min and max values
		cursorF = imgFloat.cursor();
		float min = Float.MAX_VALUE;
		float max = -Float.MAX_VALUE;
		float value;
		while (cursorF.hasNext()){
			cursorF.fwd();
			value =  cursorF.get().getRealFloat();
			if (value > max) max = value;
			if (value < min) min = value;
		}
		
		resultImg = opService.create().img(imgFloat, new UnsignedByteType());
		RandomAccess<UnsignedByteType> ra = resultImg.randomAccess();
		cursorF = imgFloat.cursor();
    	pos = new long[resultImg.numDimensions()];
    	float rf = (greyValueMax/(max-min)); //rescale factor
		while (cursorF.hasNext()) {
			cursorF.fwd();
			cursorF.localize(pos);
			value= cursorF.get().getRealFloat();
			value = rf * (value - min); //Rescale to 0  255
			ra.setPosition(pos);
			ra.get().set((int)(Math.round(value)));	
		}
		//resultImg;
	}
    
    //
    private void computeFrac2DMPD(float fracDim, int greyValueMax) {
    	int width  = (int)datasetOut.dimension(0);
    	int height = (int)datasetOut.dimension(1);
    	resultImg = new ArrayImgFactory<>(new UnsignedByteType()).create(width, height);
    
		// Size of Image [2^(N_steps-1)+1] x [2^(N_steps-1)+1]:
		int N_steps = 1;
		while( ((Math.pow(2, N_steps-1) + 1) < width ) || ((Math.pow(2, N_steps-1) + 1) < height) ){
			N_steps = N_steps +1;
		}
	    int mpdWidth  = (int)Math.pow(2, N_steps-1) + 1; 
	    int mpdHeight = (int)Math.pow(2, N_steps-1) + 1; 
		
		mpdImg = new ArrayImgFactory<>(new FloatType()).create(mpdWidth, mpdHeight);
		RandomAccess<FloatType> mpdRa = mpdImg.randomAccess();
		//-----------------------------------
		//Hurst exponent:
		double H = 3.0-fracDim;

		double mu    = 0.0;
		double sigma = 1.0;

		//Starting-image:
		int N = 2;	
		//generate random values matrix NxN
		double[][] I = new double[N][N];
		Random generator = new Random();
		for (int nn = 0; nn < N; nn++) {
			for (int mm = 0; mm < N; mm++) {
				I[nn][mm] = generator.nextGaussian() * sigma + mu;
			}
		}
       
		for(int kk = 1; kk < N_steps; kk++){
			//next step:
		    //new sigma:
		    sigma = sigma/Math.pow(2.0,H);
			 
		    //size of new image:    
		    int N_new = 2*N-1;  //ungerade Zahlen 3  5  7  9  
		    double[][]   I2         = new double[N_new][N_new]; // array filled with zeros
		  
		    //data from old image in new image:
		    for(int x = 0; x < N_new; x++){
		    	if (x % 2 == 0) { //x is even % gives the remainder, even numbers do not have a remainder
			        for(int y = 0; y < N_new; y++){
			        	if (y % 2 == 0) { //y is even
			        		I2[x][y] = I[x/2][y/2];
			        	}
			        }
		    	}
		    }    
		    //new central middle points + random shift
		    for(int x = 0; x < N_new; x++){
		    	if (x % 2 != 0) { //x is odd
			        for(int y = 0; y < N_new; y++){
			        	if (y % 2 != 0) { //y is odd
			        		I2[x][y] = (1.f/4.f*(I2[x-1][y-1]+I2[x+1][y-1]+I2[x-1][y+1]+I2[x+1][y+1]))
			        				 + (float) (generator.nextGaussian()*sigma+mu); //random shift
			        	}
			        }
		    	}
		    }	     
		    //new not-central middle points 1 + random shift	         
		    for(int x = 0; x < N_new; x++){
		    	if (x % 2 != 0) { //x is odd
			        for(int y = 1; y < N_new-2; y++){
			        	if (y % 2 == 0) { //y is even
			        		I2[x][y] = (1.f/4.f*(I2[x][y-1]+I2[x][y+1]+I2[x-1][y]+I2[x+1][y])) 
			        				 + (float) (generator.nextGaussian() * sigma + mu); //random shift
			        	}
			        }
		    	}
			}
		    //new not-central middle points 2 + random shift	         
		    for(int x = 1; x < N_new-2; x++){
		    	if (x % 2 == 0) { //x is even
			        for(int y = 0; y < N_new; y++){
			        	if (y % 2 != 0) { //y is odd
			        		I2[x][y] = (1.f/4.f*(I2[x][y-1]+I2[x][y+1]+I2[x-1][y]+I2[x+1][y])) 
			        				 + (float) (generator.nextGaussian() * sigma + mu); //random shift
			        	}
			        }
		    	}
			}


		    //missing points at borders
		    //left
		    int x = 0;
			for(int y = 0; y < N_new; y++){
				if (y % 2 != 0) { //y is odd
					I2[x][y] = (1.f/3.f*(I2[x][y-1]+I2[x][y+1]+I2[x+1][y])) 
			        		 + (float) (generator.nextGaussian() * sigma + mu); //random shift
			    }
			}
		    //right
		    x = N_new-1;
			for(int y = 0; y < N_new; y++){
				if (y % 2 != 0) { //y is odd
					I2[x][y] = (1.f/3.f*(I2[x][y-1]+I2[x][y+1]+I2[x-1][y])) 
			        		 + (float) (generator.nextGaussian() * sigma + mu); //random shift
			    }
			}
		    //top
		    int y = 0;
			for( x = 0; x < N_new; x++){
				if (x % 2 != 0) { //y is odd
					I2[x][y] = (1.f/3.f*(I2[x-1][y]+I2[x+1][y]+I2[x][y+1])) 
			        		 + (float) (generator.nextGaussian() * sigma + mu); //random shift
			    }
			}
		    //bottom
		    y = N_new-1;
			for( x = 0; x < N_new; x++){
				if (x % 2 != 0) { //y is odd
					I2[x][y] = (1.f/3.f*(I2[x-1][y]+I2[x+1][y]+I2[x][y-1])) 
			        		 + (float) (generator.nextGaussian() * sigma + mu); //random shift
			    }
			}
		    N = N_new;
		    I = I2;
		} //kk

		//double greyValueMax = 255.0;
		double min = Double.MAX_VALUE;
		double max = -Double.MAX_VALUE;
		
		Cursor<FloatType> cursorFloat = mpdImg.cursor();
    	long[] pos = new long[2];
		while (cursorFloat.hasNext()) {
			cursorFloat.fwd();
			cursorFloat.localize(pos);
			cursorFloat.get().setReal(I[(int) pos[0]][(int) pos[1]]);
			if (I[(int) pos[0]][(int) pos[1]]  < min ) min =  I[(int) pos[0]][(int) pos[1]];
			if (I[(int) pos[0]][(int) pos[1]]  > max ) max =  I[(int) pos[0]][(int) pos[1]];
		} 
		
		// Normalize grey values---------------------------------------------------
//		cursorFloat = mpdImg.cursor();
//    	pos = new long[2];
//		while (cursorFloat.hasNext()) {
//			cursorFloat.fwd();
//			//cursorFloat.localize(pos);
//			//float value = cursorFloat.get().getRealFloat();
//			//float newValue = (float) (greyValueMax*(value-min)/(max-min));
//			cursorFloat.get().set((float) (greyValueMax*(cursorFloat.get().get()-min)/(max-min)));
//		} 
		
		// Crop to original size & Normalize---------------------------------------
		Cursor<UnsignedByteType> cursor = resultImg.cursor();
    	//final long[] pos = new long[dataset.numDimensions()];
		while (cursor.hasNext()) {
			cursor.fwd();
			cursor.localize(pos);
			mpdRa.setPosition(pos);
			cursor.get().set((int)Math.round(greyValueMax*(mpdRa.get().get() - min)/(max -min)));
		}  		
		
	}
    
    
    private void computeFracSumOfSine(int numIterations, float frequency, float amplitude, float greyValueMax) {
    	
//		numIterations = 10; // iteration (number of summations)
//		frequency = 2; // frequency
//		amplitude = 2; // amplitude
    	
	 	int width  = (int)datasetOut.dimension(0);
    	int height = (int)datasetOut.dimension(1);
    	resultImg = new ArrayImgFactory<>(new UnsignedByteType()).create(width, height);
    	RandomAccess<UnsignedByteType> ra = resultImg.randomAccess();
    	
    	sosImg = new ArrayImgFactory<>(new FloatType()).create(width, height);
		RandomAccess<FloatType> sosRa = sosImg.randomAccess();
		
		//greyValueMax = 255.0;
		double min = Double.MAX_VALUE;
		double max = -Double.MAX_VALUE;
		
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				float value = 0;
				for (int k = 0; k <= numIterations; k++) {
					// normalize x and y to -pi,....0, +pi
					float xx = (float) ((float) x / ((float) (width - 1)) * 2 * Math.PI);
					float yy = (float) ((float) y / ((float) (height - 1))* 2 * Math.PI);
					xx = (float) (xx - (Math.PI));
					yy = (float) (yy - (Math.PI));
					value = value + (float) ((Math.sin(xx * Math.pow(frequency, k)) + Math.sin(yy * Math.pow(frequency, k))) / Math.pow(amplitude, k));
				}
				if (value  < min ) min = value;
				if (value  > max ) max = value;
				sosRa.setPosition(x, 0);
				sosRa.setPosition(y, 1);
				sosRa.get().set(value);
			}
		}

		// Normalize grey values---------------------------------------------------
//		cursorFloat = sosImg.cursor();
//    	pos = new long[2];
//		while (cursorFloat.hasNext()) {
//			cursorFloat.fwd();
//			//cursorFloat.localize(pos);
//			//float value = cursorFloat.get().getRealFloat();
//			//float newValue = (float) (greyValueMax*(value-min)/(max-min));
//			cursorFloat.get().set((float) (greyValueMax*(cursorFloat.get().get()-min)/(max-min)));
//		} 
		
		// Convert & Normalize---------------------------------------
		Cursor<UnsignedByteType> cursor = resultImg.cursor();
    	long[] pos = new long[2];
		while (cursor.hasNext()) {
			cursor.fwd();
			cursor.localize(pos);
			sosRa.setPosition(pos);
			cursor.get().set((int)Math.round(greyValueMax*(sosRa.get().get() - min)/(max -min)));
		}  		
    }
    
    
    /**
     * This method computes HRM hierarchical random maps to get images with distinct lacunarities
     * Plotnick et al 1993 Lacunarity indices as measures of landscape texture
     * 
     * e.g. number of iterations (levels) = 3;
     * Ideally, the given image size yields an integer initial matrix size, matrix size = size^1/3 = 3rd root(size)
     * If not, the algorithm takes the next higher integer matrix size. Then the result image is too large and will be cropped to the right size. 
     * 
     * @param numIterations
     * @param probabilities
     * @param greValueMax
     */
    private void computeFracHRM(int numIterations, float[] probabilities, float greyValueMax) {
    	
		//numIterations = 3; // iterations
    	
    	long width  = datasetOut.dimension(0);
    	long height = datasetOut.dimension(1);
    	
    	long zoomX = (long)(Math.ceil(Math.pow(width,  1.0/(double)numIterations))); //zoom is sometimes too large //this must be corrected in the end
 		long zoomY = (long)(Math.ceil(Math.pow(height, 1.0/(double)numIterations)));
    	
    	long startWidth  = zoomX;
    	long startHeight = zoomY;
    	
    	logService.info(this.getClass().getName() + " Initial matrix size " + startWidth + "x" + startHeight);
    	if (startWidth != startHeight) logService.info(this.getClass().getName() + " WARNING: Initial matrix is not symmetric! "); 
    	
		long newWidth;
		long newHeight;
		long[] size    = new long[2];
		long[] newSize = new long[2];
		
		long[] pos;
		
		ArrayImgFactory arrayImgFactory;
		int sample;
		
    	Random random = new Random();
    	resultImg = new ArrayImgFactory<>(new UnsignedByteType()).create(startWidth, startHeight);
    	Cursor<UnsignedByteType> cursor = resultImg.cursor();
    	//final long[] pos = new long[dataset.numDimensions()];
		while (cursor.hasNext()) {
			cursor.fwd();
			//cursor.localize(pos);
			if (random.nextDouble() < probabilities[0]) {
				cursor.get().setReal((int)greyValueMax); //255
			} else {
				cursor.get().setReal(0); //0
			}
		}  
		
//		uiService.show("HRM 1", resultImg);	
//		int dummy;
		
		for (int i = 1; i < numIterations; i++) {
			
			// copy image to hrmImg
			size[0] = resultImg.dimension(0); 	
			size[1] = resultImg.dimension(1); 		
			arrayImgFactory = new ArrayImgFactory<>(new UnsignedByteType());
			hrmImg = arrayImgFactory.create( size );
			RandomAccess<UnsignedByteType> ra = hrmImg.randomAccess();	
			// cursor to iterate over all pixels
			cursor = resultImg.localizingCursor();	
			pos = new long[2];
			while (cursor.hasNext()) {
				cursor.fwd();
				cursor.localize(pos);
				ra.setPosition(pos);
				ra.get().set(cursor.get());
			}  
		
			newSize[0] = hrmImg.dimension(0)*zoomX; 	
			newSize[1] = hrmImg.dimension(1)*zoomY; 			
			
			// create the output image
			arrayImgFactory = new ArrayImgFactory<>(new UnsignedByteType());
			resultImg = arrayImgFactory.create( newSize );
			
			ra = hrmImg.randomAccess();

			// copy values to larger image
			cursor = resultImg.localizingCursor();
			pos = new long[2];
			while (cursor.hasNext()) {
				cursor.fwd();
				cursor.localize(pos);
				pos[0] = (long)Math.floor((float)pos[0] / zoomX);
				pos[1] = (long)Math.floor((float)pos[1] / zoomY);
				ra.setPosition(pos);
				cursor.get().set(ra.get());
			}  
		
			cursor = resultImg.localizingCursor();
			while (cursor.hasNext()) {
				cursor.fwd();
				cursor.localize(pos);
				sample = cursor.get().getInteger();
				
				if (sample == greyValueMax){
					if (random.nextDouble() < probabilities[i]){
						cursor.get().setReal((int)greyValueMax); //255
					}
					else {
						cursor.get().setReal(0); //0
					}
				}	
			} 
			
//			uiService.show("HRM " + (i+1), resultImg);	
//			int dummy2;
			
		}//for
		
		//sometimes it is necessary to crop to exact size
		//copy image to hrmImg
		size[0] = resultImg.dimension(0); 	
		size[1] = resultImg.dimension(1); 		
		if ((size[0] > width) || (size[1] > height) ){
			
			//copy to hrmImg
			arrayImgFactory = new ArrayImgFactory<>(new UnsignedByteType());
			hrmImg = arrayImgFactory.create( size );
			RandomAccess<UnsignedByteType> ra = hrmImg.randomAccess();	
			// cursor to iterate over all pixels
			cursor = resultImg.localizingCursor();	
			pos = new long[2];
			while (cursor.hasNext()) {
				cursor.fwd();
				cursor.localize(pos);
				ra.setPosition(pos);
				ra.get().set(cursor.get());
			}  
			
			//crop to right size
			size[0] = width; 	
			size[1] = height; 		
			arrayImgFactory = new ArrayImgFactory<>(new UnsignedByteType());
			resultImg = arrayImgFactory.create( size );
			ra = hrmImg.randomAccess();	
			// cursor to iterate over all pixels
			cursor = resultImg.localizingCursor();	
			pos = new long[2];
			while (cursor.hasNext()) {
				cursor.fwd();
				cursor.localize(pos);
				ra.setPosition(pos);
				cursor.get().set(ra.get());
			}  	
		}	
		hrmImg = null;
    }
    
    private void computeFracMenger(int numIterations, int greyValueMax) {
    	
//		numIterations = 10; // iteration
	 	int width  = (int)datasetOut.dimension(0);
    	int height = (int)datasetOut.dimension(1);
    	resultImg = new ArrayImgFactory<>(new UnsignedByteType()).create(width, height);
    	//RandomAccess<UnsignedByteType> ra = resultImg.randomAccess();
    	
		// this algorithm properly works only for image sizes
		// 2*3*3*3*3.......
		int ifsWidth  = 2;
		int ifsHeight = 2;

		while ((width > ifsWidth) && (height > ifsHeight)){
			ifsWidth  = ifsWidth   * 3;
			ifsHeight = ifsHeight * 3;
		}

		//System.out.println("ImageGenerator:     width:   " + width);
		//System.out.println("ImageGenerator
		int tileSizeX = ifsWidth/3;
		int tileSizeY = ifsHeight/3;

		ifsBuffImg = new BufferedImage(ifsWidth, ifsHeight, BufferedImage.TYPE_BYTE_GRAY);
		ifsRaster  = ifsBuffImg.getRaster();
	
		//ifsImg = new ArrayImgFactory<>(new UnsignedByteType()).create(tempWidth, tempHeight);
		//RandomAccess<UnsignedByteType> ifsRa = ifsImg.randomAccess();
		
		// set initial centered square
		int xMin = Math.round((float) ifsWidth / 3);
		int xMax = Math.round((float) ifsWidth / 3 * 2);
		int yMin = Math.round((float) ifsHeight / 3);
		int yMax = Math.round((float) ifsHeight / 3 * 2);
		
		for (int x = xMin - 1; x < xMax - 1; x++) {
		for (int y = yMin - 1; y < yMax - 1; y++) {
			ifsRaster.setSample(x, y, 0, greyValueMax);
		}
		}

		// Affine transformation
		//8 surrounding images with 1/3 size
		AffineTransform at1 = new AffineTransform(1.0f / 3.0f, 0.0f, 0.0f, 1.0f / 3.0f, 0.0f * ifsWidth, 0.0f * ifsHeight);
		
		BufferedImage ifsBI1 = null;
		WritableRaster ifsR1 = null;

		AffineTransformOp op;
		int thres = greyValueMax/2;
		int greyValue;
		for (int i = 0; i < numIterations; i++) {
		
			op = new AffineTransformOp(at1, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
			ifsBI1 = op.filter(ifsBuffImg, null);
			ifsR1 = ifsBI1.getRaster();
			
			//Stitch together	
			for (int x = 0; x < tileSizeX; x++) { //ifsR1 has only 1/3 the size of ifsRaster
			for (int y = 0; y < tileSizeY; y++) {
				greyValue = ifsR1.getSample(x, y, 0);
				ifsRaster.setSample(              x,               y, 0, greyValue);
				ifsRaster.setSample(tileSizeX   + x,               y, 0, greyValue);
				ifsRaster.setSample(tileSizeX*2 + x,               y, 0, greyValue);
				
				ifsRaster.setSample(              x, tileSizeY   + y, 0, greyValue);
				ifsRaster.setSample(tileSizeX*2 + x, tileSizeY   + y, 0, greyValue);
				
				ifsRaster.setSample(              x, tileSizeY*2 + y, 0, greyValue);
				ifsRaster.setSample(tileSizeX   + x, tileSizeY*2 + y, 0, greyValue);
				ifsRaster.setSample(tileSizeX*2 + x, tileSizeY*2 + y, 0, greyValue);			
			}
			}
			
			//binarize if e.g. for bilinear interpolation
//	    	for (int x = 0; x < ifsWidth;  x++) {
//			for (int y = 0; y < ifsHeight; y++) {	
//				//greyValue = ifsRaster.getSample(x, y, 0);
//				if (ifsRaster.getSample(x, y, 0) >= thres) ifsRaster.setSample(x,  y,  0,  greyValueMax);
//				else ifsRaster.setSample(x,  y,  0,  0);
//			}
//	    	}
		}

		//rescale to original size if necessary	 
		if ((width != ifsWidth) || (height != ifsHeight)) {
			BufferedImage ifsBuffImgResized = new BufferedImage(width, height, ifsBuffImg.getType());
		    Graphics2D graphics2D = ifsBuffImgResized.createGraphics();
		    graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);  
		    graphics2D.drawImage(ifsBuffImg, 0, 0, width, height, null);
		    graphics2D.dispose();	
			ifsBuffImg = ifsBuffImgResized;
			ifsBuffImgResized = null;
			ifsRaster = ifsBuffImg.getRaster();
		}
		
		 
		//binarize e.g for bilinear interpolation
    	for (int x = 0; x < width;  x++) {
		for (int y = 0; y < height; y++) {	
			if (ifsRaster.getSample(x, y, 0) >= thres) ifsRaster.setSample(x,  y,  0,  greyValueMax);
			else ifsRaster.setSample(x,  y,  0,  0);
		}
    	}
    	
		// Convert---------------------------------------
		Cursor<UnsignedByteType> cursor = resultImg.cursor();
    	long[] pos = new long[2];
		while (cursor.hasNext()) {
			cursor.fwd();
			cursor.localize(pos);
			cursor.get().set(ifsRaster.getSample((int)pos[0], (int)pos[1], 0));
		}  	
		
		ifsBuffImg = null;
		ifsRaster = null;
		
		ifsBI1 = null;
		ifsR1 = null;

    }

    private void computeFracSierpinski1(int numIterations, int greyValueMax) {
    	// Sierpinski Gasket Method 1
		// adapted from THE NONLINEAR WORKBOOK
		// THE NONLINEAR WORKBOOK Chaos, Fractals, Cellular Automata, Neural Networks, Genetic Algorithms, Gene Expression Programming, Support Vector Machine, Wavelets,
		// Hidden Markov Models, Fuzzy Logic with C++, Java and SymbolicC++  Programs(4th Edition)
		// by Willi-Hans Steeb (University of Johannesburg, South Africa)
		// see http://www.worldscibooks.com/chaos/6883.html
    	
    	int width  = (int)datasetOut.dimension(0);
    	int height = (int)datasetOut.dimension(1);
    	resultImg = new ArrayImgFactory<>(new UnsignedByteType()).create(width, height);

		ifsBuffImg = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
		ifsRaster  = ifsBuffImg.getRaster();
		
		Graphics g = ifsBuffImg.getGraphics();
		//g.setColor(Color.WHITE);
		g.setColor(new Color(greyValueMax, greyValueMax, greyValueMax));
		// g.drawRect(0, 0, imgWidth-1, imgHeight-1);
		int n, n1, l, k, m, u1l, u2l, v1l, v2l, xl;
		double u1, u2, v1, v2, a, h, s, x, y;
		// itMax = 4; // step in the construction
		double T[] = new double[numIterations];
		a = Math.sqrt(3.0);
		for (m = 0; m <= numIterations; m++) {
			for (n = 0; n <= ((int) Math.exp(m * Math.log(3.0))); n++) {
				n1 = n;
				for (l = 0; l <= (m - 1); l++) {
					T[l] = n1 % 3;
					n1 = n1 / 3;
				}
				x = 0.0;
				y = 0.0;
				for (k = 0; k <= (m - 1); k++) {
					double temp = Math.exp(k * Math.log(2.0));
					x += Math.cos((4.0 * T[k] + 1.0) * Math.PI / 6.0) / temp;
					y += Math.sin((4.0 * T[k] + 1.0) * Math.PI / 6.0) / temp;
				}
				u1 = x + a / (Math.exp((m + 1.0) * Math.log(2.0)));
				u2 = x - a / (Math.exp((m + 1.0) * Math.log(2.0)));
				v1 = y - 1.0 / (Math.exp((m + 1.0) * Math.log(2.0)));
				v2 = y + 1.0 / (Math.exp(m * Math.log(2.0)));
				xl = (int) (width / 4 * x + width / 2 + 0.5); // imgWidth/4 gibt die absolute Gr��e an
				u1l = (int) (width / 4 * u1 + width / 2 + 0.5);
				u2l = (int) (width / 4 * u2 + width / 2 + 0.5);
				v1l = (int) (width / 4 * v1 + width / 10 * 6 + 0.5);
				v2l = (int) (width / 4 * v2 + width / 10 * 6 + 0.5); // imgWidth/10*6 gibt die vertikale Zentrierung an
				g.drawLine(u1l, v1l, xl, v2l);
				g.drawLine(xl, v2l, u2l, v1l);
				g.drawLine(u2l, v1l, u1l, v1l);
			}
		}
		// Convert---------------------------------------
		Cursor<UnsignedByteType> cursor = resultImg.cursor();
    	long[] pos = new long[2];
		while (cursor.hasNext()) {
			cursor.fwd();
			cursor.localize(pos);
			cursor.get().set(ifsRaster.getSample((int)pos[0], (int)pos[1], 0));
		}  	
		g.dispose();
		ifsBuffImg = null;
		ifsRaster = null;
	}
    
    private void computeFracSierpinski2(int numIterations, int greyValueMax) {

    	int width  = (int)datasetOut.dimension(0);
    	int height = (int)datasetOut.dimension(1);
    	resultImg = new ArrayImgFactory<>(new UnsignedByteType()).create(width, height);

		ifsBuffImg = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
		ifsRaster  = ifsBuffImg.getRaster();
		
		Graphics g = ifsBuffImg.getGraphics();
		//g.setColor(Color.WHITE);
		g.setColor(new Color(greyValueMax, greyValueMax, greyValueMax));

		// length of initial triangle
		int l = width / 10 * 10;
		// imgHeight of initial triangle
		int h = (int) (Math.sqrt(3.0d) / 2.0d * l);
		// Offset of the lower left point
		// int offSetX = imgWidth/2-l/2;
		int offSetX = 0;

		// initial triangle
		Polygon polygon = new Polygon();
		polygon.addPoint(offSetX - 1, 0);
		polygon.addPoint(width/2 - 1, h - 1);
		polygon.addPoint(width - offSetX - 1, 0);
		// g.drawPolygon(polygon); result very bad (missing lines, scattered
		// lines, very dependent on interpolation method
		g.fillPolygon(polygon);

		//crop to height h
//		ParameterBlock pbCrop = new ParameterBlock();
//		pbCrop.addSource(pi);
//		pbCrop.add(0.0f); // ((float) offSetX);
//		pbCrop.add(0.0f); // ((float) offSetY);
//		pbCrop.add((float) width); // (float) newWidth);
//		pbCrop.add((float) h); // (float) newHeight);
//		pi = JAI.create("Crop", pbCrop, null);
//		piOut = pi;

		// Affine transformation
		// AffineTransform at = new AffineTransform(m00, m10, m01, m11, m02, m12);
		AffineTransform at1 = new AffineTransform(0.5f, 0.0f, 0.0f, 0.5f,   1.0f / 4.0f * width, (float) (Math.sqrt(3) / 4.0f) * width);
		AffineTransform at2 = new AffineTransform(0.5f, 0.0f, 0.0f, 0.5f,          0.5f * width,                          0.0f * width);
		AffineTransform at3 = new AffineTransform(0.5f, 0.0f, 0.0f, 0.5f,                  0.0f,                                  0.0f);
		
		BufferedImage ifsBI1 = null;
		BufferedImage ifsBI2 = null;
		BufferedImage ifsBI3 = null;
		WritableRaster ifsR1 = null;
		WritableRaster ifsR2 = null;
		WritableRaster ifsR3 = null;

		AffineTransformOp op;
		int thres = greyValueMax/2;
		int greyValue;
		for (int i = 0; i < numIterations; i++) {
		
			op = new AffineTransformOp(at1, AffineTransformOp.TYPE_BILINEAR);
			ifsBI1 = op.filter(ifsBuffImg, null);
			ifsR1 = ifsBI1.getRaster();
			
			op = new AffineTransformOp(at2, AffineTransformOp.TYPE_BILINEAR);
			ifsBI2 = op.filter(ifsBuffImg, null);
			ifsR2 = ifsBI2.getRaster();
			
			op = new AffineTransformOp(at3, AffineTransformOp.TYPE_BILINEAR);
			ifsBI3 = op.filter(ifsBuffImg, null);
			ifsR3 = ifsBI3.getRaster();
			
			//erase target image before rewriting it
			for (int x = 0; x < ifsRaster.getWidth();  x++) { //
			for (int y = 0; y < ifsRaster.getHeight(); y++) {
				ifsRaster.setSample(x, y, 0, 0);
			}
			}
			
			//Stitch together, each Affine transformed image may have a different size
			//Sequence is ESSENTIAL, because larger images may overwrite smaller ones
			//Eventually overwrite only if (ifsR pixel == 255)
			for (int x = 0; x < ifsR1.getWidth();  x++) { //
			for (int y = 0; y < ifsR1.getHeight(); y++) {
				greyValue = ifsR1.getSample(x, y, 0);
				if (greyValue > 0) ifsRaster.setSample(x, y, 0, greyValue);
			}
			}
			//Stitch together	
			for (int x = 0; x < ifsR2.getWidth();  x++) { //
			for (int y = 0; y < ifsR2.getHeight(); y++) {
				greyValue = ifsR2.getSample(x, y, 0);
				if (greyValue > 0) ifsRaster.setSample(x, y, 0, greyValue);
			}
			}
			//Stitch together	
			for (int x = 0; x < ifsR3.getWidth();  x++) { //
			for (int y = 0; y < ifsR3.getHeight(); y++) {
				greyValue = ifsR3.getSample(x, y, 0);
				if (greyValue > 0) ifsRaster.setSample(x, y, 0, greyValue);
			}
			}	
		}	
		// binarize, da affine interpoliert
    	for (int x = 0; x < width;  x++) {
		for (int y = 0; y < height; y++) {	
			if (ifsRaster.getSample(x, y, 0) >= thres) ifsRaster.setSample(x,  y,  0,  greyValueMax);
			else ifsRaster.setSample(x,  y,  0,  0);
		}
    	}

		// Convert---------------------------------------
		Cursor<UnsignedByteType> cursor = resultImg.cursor();
    	long[] pos = new long[2];
		while (cursor.hasNext()) {
			cursor.fwd();
			cursor.localize(pos);
			cursor.get().set(ifsRaster.getSample((int)pos[0], (int)pos[1], 0));
		}  	
		g.dispose();	
		ifsBuffImg = null;
		ifsRaster = null;
  	}
    
    private void computeFracKochSnowflake(int numPolygons, int numIterations, int greyValueMax) {
    	
    	int width  = (int)datasetOut.dimension(0);
    	int height = (int)datasetOut.dimension(1);
    	resultImg = new ArrayImgFactory<>(new UnsignedByteType()).create(width, height);

		ifsBuffImg = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
		ifsRaster  = ifsBuffImg.getRaster();
		
		Graphics g = ifsBuffImg.getGraphics();
		//g.setColor(Color.WHITE);
		g.setColor(new Color(greyValueMax, greyValueMax, greyValueMax));
		int mPoly = numPolygons; // Mehrseitiges Polygon als Inititator (von 3 bis-------)
		if (mPoly < 3)
			mPoly = 3;
		double theta = 2d * Math.PI / mPoly;
		double gamma = Math.PI - theta;
		if (mPoly == 3)
			gamma = gamma + (3d * (Math.PI / 180d)); // !!!!!!!!!!

		System.out.println("Initiator: " + mPoly + "seitiges Polygon");
		// System.out.println("gamma: "+ gamma*180/(Math.PI));

		int n = mPoly; // Anzahl der Vektoren
		double[][] xOld = new double[2][n]; // Anfangswerte
		double[][] yOld = new double[2][n];

		// Bestimmung der Seitenl�nge und Position des Polygons, daher sind alle Polygone schoen im Bild
		// double radiusAussen = 150.0; //Aussenradius eines Polygons
		double radiusAussen = width / 3; // Aussenradius eines Polygons
		double radiusInnen = radiusAussen * Math.cos(theta / 2); // InnenRadius
		double seite = 2d * radiusAussen * Math.sin(theta / 2); // Seitenl�nge des Polygons

		xOld[0][0] = (width / 2 - seite / 2);
		xOld[1][0] = (width / 2 + seite / 2);

		yOld[0][0] = (width / 2 - radiusInnen);
		yOld[1][0] = (width / 2 - radiusInnen);

		// first line
		// PLOTS, [XOld[0,0], XOld[1,0]], [YOld[0,0], YOld[1,0]], /DEVICE
		// g.drawLine((int)xOld[0][0], (int)yOld[0][0], (int)xOld[1][0], (int)yOld[1][0]);

		double betrag = (Math.sqrt(Math.pow(yOld[1][0] - yOld[0][0], 2)
				+ Math.pow(xOld[1][0] - xOld[0][0], 2)));

		// mPoly-1 Vektoren, Anfangspolynom

		for (int i = 1; i < mPoly; i++) {
			int vecPosOld = i - 1;
			double alpha = (Math
					.atan((yOld[1][vecPosOld] - yOld[0][vecPosOld])
							/ (xOld[1][vecPosOld] - xOld[0][vecPosOld])));// Winkel des alten Vektors

			// Korrektur fueSonderfaelle: Vektor genau auf einer der 4Achsenrichtungen bzw. fuer 2.ten, 3.ten, 4.ten Quadranten
			if ((yOld[1][vecPosOld] > yOld[0][vecPosOld])
					&& (xOld[1][vecPosOld] == xOld[0][vecPosOld]))
				alpha = Math.PI / 2;
			if ((yOld[1][vecPosOld] < yOld[0][vecPosOld])
					&& (xOld[1][vecPosOld] == xOld[0][vecPosOld]))
				alpha = Math.PI * 3d / 2d;
			if ((yOld[1][vecPosOld] == yOld[0][vecPosOld])
					&& (xOld[1][vecPosOld] > xOld[0][vecPosOld]))
				alpha = 0d;
			if ((yOld[1][vecPosOld] == yOld[0][vecPosOld])
					&& (xOld[1][vecPosOld] < xOld[0][vecPosOld]))
				alpha = Math.PI;
			if ((yOld[1][vecPosOld] > yOld[0][vecPosOld])
					&& (xOld[1][vecPosOld] < xOld[0][vecPosOld]))
				alpha = Math.PI + alpha;
			if ((yOld[1][vecPosOld] < yOld[0][vecPosOld])
					&& (xOld[1][vecPosOld] < xOld[0][vecPosOld]))
				alpha = Math.PI + alpha;
			if ((yOld[1][vecPosOld] < yOld[0][vecPosOld])
					&& (xOld[1][vecPosOld] > xOld[0][vecPosOld]))
				alpha = (2d * Math.PI) + alpha;

			alpha = alpha + theta;

			// Neuer Vektor
			xOld[0][i] = xOld[1][i - 1];
			xOld[1][i] = xOld[1][i - 1] + betrag * Math.cos(alpha);

			yOld[0][i] = yOld[1][i - 1];
			yOld[1][i] = yOld[1][i - 1] + betrag * Math.sin(alpha);

			// rest of initial polygon
			// PLOTS, [xOld[0,i], xOld[1,i]], [yOld[0,i], yOld[1,i]], /DEVICE
			// g.drawLine((int)xOld[0][i], (int)yOld[0][i], (int)xOld[1][i], (int)yOld[1][i]);
		}

		// *******Hauptschleife Iteration*******************
		// 8 //only the last iteration draws the lines
		double[][] x;
		double[][] y;

		int vecPosNeu;
		int vecPosOld;

		double betragOld;
		double betragNeu;
		double alpha;

		int thres = greyValueMax/2;
		for (int i = 1; i <= numIterations; i++) {
			//System.out.println("Iteration Nr.: " + i);

			// VectorSize = SIZE(xOld)
			int vectorSize = xOld[0].length;

			// if i == 1 THEN VectorSize = [0][0][1] ELSE VectorSize =
			// SIZE(xOld)
			x = new double[2][3 * vectorSize];
			y = new double[2][3 * vectorSize];

			for (int nn = 1; nn <= vectorSize; nn++) {// Vektorschleife

				// 1.ter neuer Teilvektor
				vecPosNeu = 3 * (nn - 1);
				vecPosOld = (nn - 1);

				betragOld = Math.sqrt(Math.pow(yOld[1][0] - yOld[0][0], 2) + Math.pow(xOld[1][0] - xOld[0][0], 2));
				betragNeu = (betragOld / 2) / Math.sqrt(1.25 - Math.cos(gamma));
				alpha = Math.atan((yOld[1][vecPosOld] - yOld[0][vecPosOld]) / (xOld[1][vecPosOld] - xOld[0][vecPosOld]));// Winkel des alten Vektors

				// Korrektur für Sonderfälle: Vektor genau auf einer der 4Achsenrichtungen bzw.für 2.ten, 3.ten, 4.ten Quadranten
				if ((yOld[1][vecPosOld] > yOld[0][vecPosOld])
						&& (xOld[1][vecPosOld] == xOld[0][vecPosOld]))
					alpha = Math.PI / 2;
				if ((yOld[1][vecPosOld] < yOld[0][vecPosOld])
						&& (xOld[1][vecPosOld] == xOld[0][vecPosOld]))
					alpha = Math.PI * 3d / 2d;
				if ((yOld[1][vecPosOld] == yOld[0][vecPosOld])
						&& (xOld[1][vecPosOld] > xOld[0][vecPosOld]))
					alpha = 0d;
				if ((yOld[1][vecPosOld] == yOld[0][vecPosOld])
						&& (xOld[1][vecPosOld] < xOld[0][vecPosOld]))
					alpha = Math.PI;
				if ((yOld[1][vecPosOld] > yOld[0][vecPosOld])
						&& (xOld[1][vecPosOld] < xOld[0][vecPosOld]))
					alpha = Math.PI + alpha;
				if ((yOld[1][vecPosOld] < yOld[0][vecPosOld])
						&& (xOld[1][vecPosOld] < xOld[0][vecPosOld]))
					alpha = Math.PI + alpha;
				if ((yOld[1][vecPosOld] < yOld[0][vecPosOld])
						&& (xOld[1][vecPosOld] > xOld[0][vecPosOld]))
					alpha = (2d * Math.PI) + alpha;

				alpha = Math.asin((betragNeu / 2) * (Math.sin(gamma) / (betragOld / 2))) + alpha; // Neuer Winkel f�r 1.ten Vektor

				x[0][vecPosNeu] = xOld[0][vecPosOld];
				x[1][vecPosNeu] = xOld[0][vecPosOld] + Math.cos(alpha) * betragNeu;
				y[0][vecPosNeu] = yOld[0][vecPosOld];
				y[1][vecPosNeu] = yOld[0][vecPosOld] + Math.sin(alpha) * betragNeu;

				// PLOTS, [X[0,vecPosNeu], X[1,vecPosNeu]],[Y[0,vecPosNeu], Y[1,vecPosNeu]], /DEVICE
				if (i == numIterations)
					g.drawLine((int) x[0][vecPosNeu], (int) y[0][vecPosNeu], (int) x[1][vecPosNeu], (int) y[1][vecPosNeu]);

				// 2.ter neuer Teilvektor
				vecPosNeu = vecPosNeu + 1;
				x[0][vecPosNeu] = x[1][vecPosNeu - 1];
				y[0][vecPosNeu] = y[1][vecPosNeu - 1];
				double beta = Math.PI - alpha - gamma;

				x[1][vecPosNeu] = x[1][vecPosNeu - 1] + Math.cos(beta) * betragNeu;
				y[1][vecPosNeu] = y[1][vecPosNeu - 1] - Math.sin(beta) * betragNeu;

				// PLOTS, [X[0,vecPosNeu], X[1,vecPosNeu]],[Y[0,vecPosNeu], Y[1,vecPosNeu]], /DEVICE
				if (i == numIterations)
					g.drawLine((int) x[0][vecPosNeu], (int) y[0][vecPosNeu], (int) x[1][vecPosNeu], (int) y[1][vecPosNeu]);

				// 3.ter neuer TeilVektor
				vecPosNeu = vecPosNeu + 1;
				x[0][vecPosNeu] = x[1][vecPosNeu - 1];
				x[1][vecPosNeu] = x[1][vecPosNeu - 1] + Math.cos(alpha) * betragNeu;
				y[0][vecPosNeu] = y[1][vecPosNeu - 1];
				y[1][vecPosNeu] = y[1][vecPosNeu - 1] + Math.sin(alpha) * betragNeu;

				// PLOTS, [X[0,vecPosNeu], X[1,vecPosNeu]],[Y[0,vecPosNeu], Y[1,vecPosNeu]], /DEVICE
				if (i == numIterations)
					g.drawLine((int) x[0][vecPosNeu], (int) y[0][vecPosNeu], (int) x[1][vecPosNeu], (int) y[1][vecPosNeu]);

			} // Vektorschleife

			xOld = new double[2][3 * vectorSize]; // R�cksetzen
			yOld = new double[2][3 * vectorSize];
			xOld = x;
			yOld = y;

		} // Hauptschleife Iteration i

    	// binarize, if necessary
//    	for (int x = 0; x < width;  x++) {
//		for (int y = 0; y < height; y++) {	
//			if (ifsRaster.getSample(x, y, 0) >= thres) ifsRaster.setSample(x,  y,  0,  greyValueMax);
//			else ifsRaster.setSample(x,  y,  0,  0);
//		}
//    	}

		// Convert---------------------------------------
		Cursor<UnsignedByteType> cursor = resultImg.cursor();
    	long[] pos = new long[2];
		while (cursor.hasNext()) {
			cursor.fwd();
			cursor.localize(pos);
			cursor.get().set(ifsRaster.getSample((int)pos[0], (int)pos[1], 0));
		}  	
		g.dispose();
		ifsBuffImg = null;
		ifsRaster = null;
    }
    
    
  private void computeFracFern(int numIterations, int greyValueMax) {
    	
    	int width  = (int)datasetOut.dimension(0);
    	int height = (int)datasetOut.dimension(1);
    	resultImg = new ArrayImgFactory<>(new UnsignedByteType()).create(width, height);

		ifsBuffImg = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
		ifsRaster  = ifsBuffImg.getRaster();
		
		Graphics g = ifsBuffImg.getGraphics();
		//g.setColor(Color.WHITE);
		g.setColor(new Color(greyValueMax, greyValueMax, greyValueMax));

		// adapted from THE NONLINEAR WORKBOOK
		double mxx, myy, bxx, byy;
		double x, y, xn, yn, r;
		int pex, pey;
		// int max = 15000; // number of iterations
		int max = numIterations;
		x = 0.5;
		y = 0.0; // starting point

		// convert(0.0,1.0,1.0,-0.5);
		double xiz = 0.0d;
		double ysu = 1.0d;
		double xde = 1.0d;
		double yinf = -0.5d;
		double maxx, maxy, xxfin, xxcom, yyin, yysu;
		// maxx = 600; maxy = 450;
		maxx = width / 10 * 10;
		maxy = height / 10 * 22; // links rechts bzw H�he
		xxcom = 0.15 * maxx;
		xxfin = 0.75 * maxx;
		yyin = 0.8 * maxy;
		yysu = 0.2 * maxy;
		mxx = 2.5 * (xxfin - xxcom) / (xde - xiz); // Breite
		bxx = 0.415 * (xxcom + xxfin - mxx * (xiz + xde)); // links rechts
		myy = 1.0 * (yyin - yysu) / (yinf - ysu);
		byy = 0.37 * (yysu + yyin - myy * (yinf + ysu)); // oben unten

		// Original values (too small)
		// maxx = 600; maxy = 450; //links rechts bzw H�he
		// xxcom = 0.15*maxx; xxfin = 0.75*maxx;
		// yyin = 0.8*maxy; yysu = 0.2*maxy;
		// mxx = (xxfin-xxcom)/(xde-xiz);
		// bxx = 0.5*(xxcom+xxfin-mxx*(xiz+xde));
		// myy = (yyin-yysu)/(yinf-ysu);
		// byy = 0.5*(yysu+yyin-myy*(yinf+ysu));

		// setBackground(Color.white);
		// g.setColor(Color.black);
		for (int i = 0; i <= max; i++) {
			r = Math.random(); // generate a random number
			if (r <= 0.02) {
				xn = 0.5;
				yn = 0.27 * y;
			} // map 1
			else if ((r > 0.02) && (r <= 0.17)) {
				xn = -0.139 * x + 0.263 * y + 0.57; // map 2
				yn = 0.246 * x + 0.224 * y - 0.036;
			} else if ((r > 0.17) && (r <= 0.3)) {
				xn = 0.17 * x - 0.215 * y + 0.408; // map 3
				yn = 0.222 * x + 0.176 * y + 0.0893;
			} else {
				xn = 0.781 * x + 0.034 * y + 0.1075; // map 4
				yn = -0.032 * x + 0.739 * y + 0.27;
			}
			x = xn;
			y = yn;
			pex = (int) (mxx * x + bxx);
			pey = (int) (myy * y + byy);
			g.drawLine(pex, pey, pex, pey); // output to screen
		}

    	// binarize, if necessary
//    	for (int x = 0; x < width;  x++) {
//		for (int y = 0; y < height; y++) {	
//			if (ifsRaster.getSample(x, y, 0) >= thres) ifsRaster.setSample(x,  y,  0,  greyValueMax);
//			else ifsRaster.setSample(x,  y,  0,  0);
//		}
//    	}

		// Convert---------------------------------------
		Cursor<UnsignedByteType> cursor = resultImg.cursor();
    	long[] pos = new long[2];
		while (cursor.hasNext()) {
			cursor.fwd();
			cursor.localize(pos);
			cursor.get().set(ifsRaster.getSample((int)pos[0], (int)pos[1], 0));
		}  	
		g.dispose();
		ifsBuffImg = null;
		ifsRaster = null;
    }
    
  private void computeFracHeighway(int numIterations, int greyValueMax) {
  	
  	int width  = (int)datasetOut.dimension(0);
  	int height = (int)datasetOut.dimension(1);
  	resultImg = new ArrayImgFactory<>(new UnsignedByteType()).create(width, height);

		ifsBuffImg = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
		ifsRaster  = ifsBuffImg.getRaster();
		
		Graphics g = ifsBuffImg.getGraphics();
		//g.setColor(Color.WHITE);
		g.setColor(new Color(greyValueMax, greyValueMax, greyValueMax));

		// Polygon polygon = new Polygon();
		// polygon.addPoint(imgWidth/3, imgHeight/3);
		// polygon.addPoint(imgWidth/3, imgHeight/3*2);
		// polygon.addPoint(imgWidth/3*2, imgHeight/3*2);
		// polygon.addPoint(imgWidth/3*2, imgHeight/3);
		// g.fillPolygon(polygon);

		int scaling = (int) (Math.min(width, height) / 3.65);
		int xorig = scaling;
		int yorig = scaling;
		int x1 = xorig + scaling;
		int y1 = yorig;
		int x2 = xorig;
		int y2 = yorig - scaling;
		int x3 = xorig - scaling;
		int y3 = yorig;
		dragonr(g, scaling, x1, y1, x2, y2, x3, y3, numIterations);

  	// binarize, if necessary
//  	for (int x = 0; x < width;  x++) {
//		for (int y = 0; y < height; y++) {	
//			if (ifsRaster.getSample(x, y, 0) >= thres) ifsRaster.setSample(x,  y,  0,  greyValueMax);
//			else ifsRaster.setSample(x,  y,  0,  0);
//		}
//  	}

		// Convert---------------------------------------
		Cursor<UnsignedByteType> cursor = resultImg.cursor();
  	long[] pos = new long[2];
		while (cursor.hasNext()) {
			cursor.fwd();
			cursor.localize(pos);
			cursor.get().set(ifsRaster.getSample((int)pos[0], (int)pos[1], 0));
		}  	
		g.dispose();
		ifsBuffImg = null;
		ifsRaster = null;
  }
  
	/**
	 * This method draws the Heighway's Dragon This is adapted from the
	 * Nonlinear Workbook
	 * 
	 * @param g
	 * @param scaling
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 * @param x3
	 * @param y3
	 * @param n
	 */
	public void dragonr(Graphics g, int scaling, int x1, int y1, int x2,
			int y2, int x3, int y3, int n) {
		if (n == 1) {
			g.drawLine(x1 + scaling, y1 + scaling, x2 + scaling, y2 + scaling);
			g.drawLine(x2 + scaling, y2 + scaling, x3 + scaling, y3 + scaling);
		} else {
			int x4 = (x1 + x3) / 2;
			int y4 = (y1 + y3) / 2;
			int x5 = x3 + x2 - x4;
			int y5 = y3 + y2 - y4;
			dragonr(g, scaling, x2, y2, x4, y4, x1, y1, n - 1);
			dragonr(g, scaling, x2, y2, x5, y5, x3, y3, n - 1);
		}
	}
	/**
     * This main function serves for development purposes.
     * It allows you to run the plugin immediately out of
     * your integrated development environment (IDE).
     *
     * @param args whatever, it's ignored
     * @throws Exception
     */
    

    @Override
    public void run() {
    	//WaitingDialogWithProgressBar dlgProgress = new WaitingDialogWithProgressBar("<html>Generating 2D image, please wait...<br>Open console window for further info.</html>");
		WaitingDialogWithProgressBar dlgProgress = new WaitingDialogWithProgressBar("Generating 2D image, please wait... Open console window for further info.",
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
		int numImages			= spinnerInteger_NumImages;
		String colorModelType   = choiceRadioButt_ColorModelType;//"Grey-8bit", "Color-RGB"
		String imageType		= choiceRadioButt_ImageType;
		int greyR   			= spinnerInteger_R;
		int greyG   			= spinnerInteger_G;
		int greyB   			= spinnerInteger_B;
		float fracDim 			= spinnerFloat_FracDim;
		float frequency  		= spinnerFloat_SineSumOfSineFrequency;
		float sosAmplitude      = spinnerFloat_SumOfSineAmplitude;
		float[] probabilities   = new float[]{spinnerFloat_HRMProbability1, spinnerFloat_HRMProbability2, spinnerFloat_HRMProbability3};
		int numIterations		= spinnerInteger_NumSumOfSineIterations;
		int numPolygons			= spinnerInteger_NumPolygons;
	
		// Create an image.
		
		String name = "2D image";
		if 		(imageType.equals("Random"))   						name = "Random image(s)";
		else if (imageType.equals("Gaussian")) 						name = "Gaussian image(s)";
		else if (imageType.equals("Sine - radial")) 				name = "Radial sinusoidal image(s)";
		else if (imageType.equals("Sine - horizontal")) 			name = "Horizontal sinusoidal image(s)";
		else if (imageType.equals("Sine - vertical")) 				name = "Vertical sinusoidal image(s)";
		else if (imageType.equals("Constant")) 						name = "Constant image(s)";
		else if (imageType.equals("Fractal surface - FFT"))			name = "Fractal surface(s) - FFT";
		else if (imageType.equals("Fractal surface - MPD"))			name = "Fractal surface(s) - MPD";
		else if (imageType.equals("Fractal surface - Sum of sine")) name = "Fractal surface(s) - Sum of sine";
		else if (imageType.equals("Fractal - HRM"))					name = "Fractal - HRM";
		else if (imageType.equals("Fractal IFS - Menger"))			name = "Fractal IFS - Menger";
		else if (imageType.equals("Fractal IFS - Sierpinski-1"))	name = "Fractal IFS - Sierpinski-1";
		else if (imageType.equals("Fractal IFS - Sierpinski-2"))	name = "Fractal IFS - Sierpinski-2";
		else if (imageType.equals("Fractal IFS - Koch snowflake"))	name = "Fractal IFS - Koch snowflake";
		else if (imageType.equals("Fractal IFS - Fern"))			name = "Fractal IFS - Fern";
		else if (imageType.equals("Fractal IFS - Heighway dragon"))	name = "Fractal IFS - Heighway dragon";
			
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
			if (numImages == 1) {
				bitsPerPixel = 8;
				dims = new long[]{width, height};
				axes = new AxisType[]{Axes.X, Axes.Y};
				datasetOut = datasetService.create(dims, name, axes, bitsPerPixel, signed, floating, virtual);	

				if      (imageType.equals("Random"))   						computeRandomImage(greyR);
				else if (imageType.equals("Gaussian")) 						computeGaussianImage(greyR);
				else if (imageType.equals("Sine - radial")) 				computeSineImage("radial",     frequency, greyR);
				else if (imageType.equals("Sine - horizontal")) 			computeSineImage("horizontal", frequency, greyR);
				else if (imageType.equals("Sine - vertical")) 				computeSineImage("vertical",   frequency, greyR);
				else if (imageType.equals("Constant")) 						computeConstantImage(greyR);
				else if (imageType.equals("Fractal surface - FFT"))			computeFrac2DFFT(fracDim, greyR);
				else if (imageType.equals("Fractal surface - MPD")) 		computeFrac2DMPD(fracDim, greyR);
				else if (imageType.equals("Fractal surface - Sum of sine")) computeFracSumOfSine(numIterations, frequency, sosAmplitude, greyR);
				else if (imageType.equals("Fractal - HRM"))					computeFracHRM(3, probabilities, greyR);
				else if (imageType.equals("Fractal IFS - Menger"))			computeFracMenger(numIterations, greyR);
				else if (imageType.equals("Fractal IFS - Sierpinski-1"))	computeFracSierpinski1(numIterations, greyR);
				else if (imageType.equals("Fractal IFS - Sierpinski-2"))	computeFracSierpinski2(numIterations, greyR);
				else if (imageType.equals("Fractal IFS - Koch snowflake"))	computeFracKochSnowflake(numPolygons, numIterations, greyR);
				else if (imageType.equals("Fractal IFS - Fern"))			computeFracFern(numIterations, greyR);
				else if (imageType.equals("Fractal IFS - Heighway dragon"))	computeFracHeighway(numIterations, greyR);
				
				RandomAccess<RealType<?>> ra = datasetOut.randomAccess();
				Cursor<UnsignedByteType> cursor = resultImg.cursor();
				long[] pos = new long[2];
				float value;
				
				while (cursor.hasNext()) {
					cursor.fwd();
					cursor.localize(pos);
					value= cursor.get().getRealFloat();
					//value = rf * (value -min); //Rescale to 0  255
					ra.setPosition(pos);
					ra.get().setReal(value);
				}
			}
			else if (numImages >= 1) {
				bitsPerPixel = 8;
				dims = new long[]{width, height, numImages};
				axes = new AxisType[]{Axes.X, Axes.Y, Axes.Z};
				datasetOut = datasetService.create(dims, name, axes, bitsPerPixel, signed, floating, virtual);
					
				RandomAccess<RealType<?>> ra;
				Cursor<UnsignedByteType> cursor;
				long[] pos2D;
				long[] pos3D; 
				float value;
				long startTime;
				long duration;
				dlgProgress.setBarIndeterminate(false);
				for (int n =0; n < numImages; n++) {
					
					int percent = (int)Math.round((  ((float)n)/((float)numImages)   *100.f   ));
					dlgProgress.updatePercent(String.valueOf(percent+"%"));
					dlgProgress.updateBar(percent);
					//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
					statusService.showStatus((n+1), (int)numImages, "Generating " + (n+1) + "/" + (int)numImages);

					startTime = System.currentTimeMillis();
					logService.info(this.getClass().getName() + " Generating image number " + (n+1) + "(" + numImages + ")");
					
					if      (imageType.equals("Random"))   						computeRandomImage(greyR);
					else if (imageType.equals("Gaussian")) 						computeGaussianImage(greyR);
					else if (imageType.equals("Sine - radial")) 				computeSineImage("radial",     frequency, greyR);
					else if (imageType.equals("Sine - horizontal")) 			computeSineImage("horizontal", frequency, greyR);
					else if (imageType.equals("Sine - vertical")) 				computeSineImage("vertical",   frequency, greyR);
					else if (imageType.equals("Constant")) 						computeConstantImage(greyR);
					else if (imageType.equals("Fractal surface - FFT"))			computeFrac2DFFT(fracDim, greyR);
					else if (imageType.equals("Fractal surface - MPD")) 		computeFrac2DMPD(fracDim, greyR);
					else if (imageType.equals("Fractal surface - Sum of sine")) computeFracSumOfSine(numIterations, frequency, sosAmplitude, greyR);
					else if (imageType.equals("Fractal - HRM"))					computeFracHRM(3, probabilities, greyR);
					else if (imageType.equals("Fractal IFS - Menger"))			computeFracMenger(numIterations, greyR);
					else if (imageType.equals("Fractal IFS - Sierpinski-1"))	computeFracSierpinski1(numIterations, greyR);
					else if (imageType.equals("Fractal IFS - Sierpinski-2"))	computeFracSierpinski2(numIterations, greyR);
					else if (imageType.equals("Fractal IFS - Koch snowflake"))	computeFracKochSnowflake(numPolygons, numIterations, greyR);
					else if (imageType.equals("Fractal IFS - Fern"))			computeFracFern(numIterations, greyR);
					else if (imageType.equals("Fractal IFS - Heighway dragon"))	computeFracHeighway(numIterations, greyR);
					
					ra = datasetOut.randomAccess();
					cursor = resultImg.cursor();
					pos2D = new long[2];
					pos3D = new long[3];
					
					while (cursor.hasNext()) {
						cursor.fwd();
						cursor.localize(pos2D);
						value= cursor.get().getRealFloat();
						//value = rf * (value -min); //Rescale to 0  255
						pos3D = new long[] {pos2D[0], pos2D[1], n};
						ra.setPosition(pos3D);
						ra.get().setReal(value);
					}
					duration = System.currentTimeMillis() - startTime;
					TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
					SimpleDateFormat sdf = new SimpleDateFormat();
					sdf.applyPattern("HHH:mm:ss:SSS");
					logService.info(this.getClass().getName() + " Elapsed time: "+ sdf.format(duration));
				}			
			}
		}
		else if (colorModelType.equals("Color-RGB")) {
			if (numImages == 1) {
				bitsPerPixel = 8;
				dims = new long[]{width, height, 3};
				axes = new AxisType[]{Axes.X, Axes.Y, Axes.CHANNEL};
				datasetOut = datasetService.create(dims, name, axes, bitsPerPixel, signed, floating, virtual);	
				datasetOut.setCompositeChannelCount(3);
				datasetOut.setRGBMerged(true);
				
				//R G B
				RandomAccess<RealType<?>> ra = datasetOut.randomAccess();
				Cursor<UnsignedByteType> cursor;
				long[] pos2D = new long[2];
				float value;
				int greyValue = 0;
				for (int chan = 0; chan <= 2; chan++ ){
					switch (chan){
						case 0: greyValue = greyR; break;
						case 1: greyValue = greyG; break;
						case 2: greyValue = greyB; break;
					}
					if (imageType.equals("Random"))   			    			computeRandomImage(greyValue);
					else if (imageType.equals("Gaussian")) 						computeGaussianImage(greyValue);
					else if (imageType.equals("Sine - radial")) 				computeSineImage("radial",     frequency, greyValue);
					else if (imageType.equals("Sine - horizontal")) 			computeSineImage("horizontal", frequency, greyValue);
					else if (imageType.equals("Sine - vertical")) 				computeSineImage("vertical",   frequency, greyValue);
					else if (imageType.equals("Constant")) 						computeConstantImage(greyValue);
					else if (imageType.equals("Fractal surface - FFT"))			computeFrac2DFFT(fracDim, greyValue);
					else if (imageType.equals("Fractal surface - MPD")) 		computeFrac2DMPD(fracDim, greyValue);
					else if (imageType.equals("Fractal surface - Sum of sine")) computeFracSumOfSine(numIterations, frequency, sosAmplitude, greyValue);
					else if (imageType.equals("Fractal - HRM"))					computeFracHRM(3, probabilities, greyValue);
					else if (imageType.equals("Fractal IFS - Menger"))			computeFracMenger(numIterations, greyValue);
					else if (imageType.equals("Fractal IFS - Sierpinski-1"))	computeFracSierpinski1(numIterations, greyValue);
					else if (imageType.equals("Fractal IFS - Sierpinski-2"))	computeFracSierpinski2(numIterations, greyValue);
					else if (imageType.equals("Fractal IFS - Koch snowflake"))	computeFracKochSnowflake(numPolygons, numIterations, greyValue);
					else if (imageType.equals("Fractal IFS - Fern"))			computeFracFern(numIterations, greyValue);
					else if (imageType.equals("Fractal IFS - Heighway dragon"))	computeFracHeighway(numIterations, greyValue);
					
					cursor = resultImg.cursor();
					
					while (cursor.hasNext()) {
						cursor.fwd();
						cursor.localize(pos2D);
						value= cursor.get().getRealFloat();
						//value = rf * (value -min); //Rescale to 0  255
						ra.setPosition(pos2D[0], 0);
						ra.setPosition(pos2D[1], 1);
						ra.setPosition(chan, 2); //R  G  B
						ra.get().setReal(value);
					}
				} //RGB		
			}
			else if (numImages >= 1) {
				bitsPerPixel = 8;
				dims = new long[]{width, height, 3, numImages};
				axes = new AxisType[]{Axes.X, Axes.Y, Axes.CHANNEL, Axes.Z};
				datasetOut = datasetService.create(dims, name, axes, bitsPerPixel, signed, floating, virtual);
				datasetOut.setCompositeChannelCount(3);
				datasetOut.setRGBMerged(true);
					
				RandomAccess<RealType<?>> ra = datasetOut.randomAccess();
				Cursor<UnsignedByteType> cursor;
				long[] pos2D;
				float value;
				int greyValue = 0;
				long startTime;
				long duration;
				dlgProgress.setBarIndeterminate(false);
				for (int n =0; n < numImages; n++) {
					
					int percent = (int)Math.round((  ((float)n)/((float)numImages)   *100.f   ));
					dlgProgress.updatePercent(String.valueOf(percent+"%"));
					dlgProgress.updateBar(percent);
					//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
					statusService.showStatus((n+1), (int)numImages, "Generating " + (n+1) + "/" + (int)numImages);

					startTime = System.currentTimeMillis();
					logService.info(this.getClass().getName() + " Generating image number " + (n+1) + "(" + numImages + ")");
					
					for (int chan = 0; chan <= 2; chan++ ){ //RGB
						
						switch (chan){
						case 0: greyValue = greyR; break;
						case 1: greyValue = greyG; break;
						case 2: greyValue = greyB; break;
					}
						if (imageType.equals("Random"))   			    			computeRandomImage(greyValue);
						else if (imageType.equals("Gaussian")) 						computeGaussianImage(greyValue);
						else if (imageType.equals("Sine - radial")) 				computeSineImage("radial",     frequency, greyValue);
						else if (imageType.equals("Sine - horizontal")) 			computeSineImage("horizontal", frequency, greyValue);
						else if (imageType.equals("Sine - vertical")) 				computeSineImage("vertical",   frequency, greyValue);
						else if (imageType.equals("Constant")) 						computeConstantImage(greyValue);
						else if (imageType.equals("Fractal surface - FFT"))			computeFrac2DFFT(fracDim, greyValue);
						else if (imageType.equals("Fractal surface - MPD")) 		computeFrac2DMPD(fracDim, greyValue);
						else if (imageType.equals("Fractal surface - Sum of sine")) computeFracSumOfSine(numIterations, frequency, sosAmplitude, greyValue);
						else if (imageType.equals("Fractal - HRM"))					computeFracHRM(3, probabilities, greyValue);
						else if (imageType.equals("Fractal IFS - Menger"))			computeFracMenger(numIterations, greyValue);
						else if (imageType.equals("Fractal IFS - Sierpinski-1"))	computeFracSierpinski1(numIterations, greyValue);
						else if (imageType.equals("Fractal IFS - Sierpinski-2"))	computeFracSierpinski2(numIterations, greyValue);
						else if (imageType.equals("Fractal IFS - Koch snowflake"))	computeFracKochSnowflake(numPolygons, numIterations, greyValue);
						else if (imageType.equals("Fractal IFS - Fern"))			computeFracFern(numIterations, greyValue);
						else if (imageType.equals("Fractal IFS - Heighway dragon"))	computeFracHeighway(numIterations, greyValue);
						
						cursor = resultImg.cursor();
						pos2D = new long[2];		
						while (cursor.hasNext()) {
							cursor.fwd();
							cursor.localize(pos2D);
							value= cursor.get().getRealFloat();
							//value = rf * (value -min); //Rescale to 0  255
							
							ra.setPosition(new long[] {pos2D[0], pos2D[1], chan, n});
							ra.get().setReal(value);
						}
					}//RGB
					duration = System.currentTimeMillis() - startTime;
					TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
					SimpleDateFormat sdf = new SimpleDateFormat();
					sdf.applyPattern("HHH:mm:ss:SSS");
					logService.info(this.getClass().getName() + " Elapsed time: "+ sdf.format(duration));
				}//n
			}
		}
		
		statusService.showProgress(0, 100);
		statusService.clearStatus();
		
		dlgProgress.addMessage("Processing finished! Displaying image(s)...");
		//not necessary because datasetOut is an IO type
		//ij.ui().show("Image", datasetOut);
		//if (choiceRadioButt_ImageType.equals("Random"))   uiService.show("Random",   datasetOut);
		//if (choiceRadioButt_ImageType.equals("Constant")) uiService.show("Constant", datasetOut);
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
         ij.command().run(Img2DImageGenerator.class, true);
    	
    }

}
