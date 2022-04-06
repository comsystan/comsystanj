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
import net.imglib2.RandomAccess;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.FloorInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.LanczosInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import net.imglib2.view.IntervalView;
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
		label = "3D Image volume generator",
		//iconPath = "/images/comsystan-??.png", //Menu entry icon
		menu = {
        @Menu(label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT, mnemonic = MenuConstants.PLUGINS_MNEMONIC),
        @Menu(label = "ComsystanJ"),
        @Menu(label = "Image (3D)"),
        @Menu(label = "3D Image volume generator", weight = 10)})
public class Img3DVolumeGenerator<T extends RealType<T>, C> extends ContextCommand implements Previewable { //modal GUI with cancel
		
	private static final String PLUGIN_LABEL 			= "<html><b>Generates 3D image volumes</b></html>";
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

	//private Img<FloatType> volFloat;
	private float[][][] volMPD;
	private float[][][] volFFT;
	private Img<FloatType> volIFS;
	private Img<FloatType> volIFSTemp;
	private Img<FloatType> volIFSCopy;
	private RandomAccess<FloatType> raF = null;
	private RandomAccess<RealType<?>> ra;
	private Cursor<RealType<?>> cursor;
	private Cursor<FloatType> cursorF;
	
	private String colorModelType;
	
	WaitingDialogWithProgressBar dlgProgress;
	
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
    				      "Fractal IFS - Menger", "Fractal IFS - Sierpinski",
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
	  		   min = "0",
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
		logService.info(this.getClass().getName() + " Color model set to " + choiceRadioButt_ColorModelType);
	}
	
	/** Executed whenever the {@link #choiceRadioButt_VolumeType} parameter changes. */
	protected void changedVolumeType() {
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
 	
    private void compute3DRandom(int greyMaxR, int greyMaxG, int greyMaxB) {
    	
    	dlgProgress.setBarIndeterminate(true);
		Random random = new Random();
		random.setSeed(System.currentTimeMillis());
		cursor = datasetOut.cursor();
		
		if (colorModelType.equals("Grey-8bit")) {
			while (cursor.hasNext()) {
				cursor.fwd();
				cursor.get().setReal((int)(random.nextFloat()*greyMaxR));
			} 
					
		} else if (colorModelType.equals("Color-RGB")) {
		 	long[] pos = new long[4];
			while (cursor.hasNext()) {
				cursor.fwd();	
				cursor.localize(pos);
				if      (pos[3] == 0) cursor.get().setReal((int)Math.round(random.nextFloat()*greyMaxR));
				else if (pos[3] == 1) cursor.get().setReal((int)Math.round(random.nextFloat()*greyMaxG));
				else if (pos[3] == 2) cursor.get().setReal((int)Math.round(random.nextFloat()*greyMaxB));
			} 	
		}		
	}
    
	private void compute3DGaussian(int greyMaxR, int greyMaxG, int greyMaxB) {
        
		dlgProgress.setBarIndeterminate(true);
    	float muR = (float)greyMaxR/2f;
    	float muG = (float)greyMaxG/2f;
    	float muB = (float)greyMaxB/2f;
    	float sigma = 30f;
    	Random random = new Random();
    	random.setSeed(System.currentTimeMillis());

    	cursor = datasetOut.cursor();
		
		if (colorModelType.equals("Grey-8bit")) {
			while (cursor.hasNext()) {
				cursor.fwd();
				cursor.get().setReal((int)(random.nextGaussian()*sigma + muR));
			} 
					
		} else if (colorModelType.equals("Color-RGB")) {
		 	long[] pos = new long[4];
			while (cursor.hasNext()) {
				cursor.fwd();	
				cursor.localize(pos);
				if      (pos[3] == 0) cursor.get().setReal((int)Math.round(random.nextGaussian()*sigma + muR));
				else if (pos[3] == 1) cursor.get().setReal((int)Math.round(random.nextGaussian()*sigma + muG));
				else if (pos[3] == 2) cursor.get().setReal((int)Math.round(random.nextGaussian()*sigma + muB));
			} 	
		}			
	}
	
    private void compute3DConstant(int constR, int constG, int constB) {
        
    	dlgProgress.setBarIndeterminate(true);
    	cursor = datasetOut.cursor();
    
    	if (colorModelType.equals("Grey-8bit")) {
			while (cursor.hasNext()) {
				cursor.fwd();
				cursor.get().setReal(constR);
			} 
					
		} else if (colorModelType.equals("Color-RGB")) {
		 	long[] pos = new long[4];
			while (cursor.hasNext()) {
				cursor.fwd();
				cursor.localize(pos);
				if      (pos[3] == 0) cursor.get().setReal(constR);
				else if (pos[3] == 1) cursor.get().setReal(constG);
				else if (pos[3] == 2) cursor.get().setReal(constB);
			} 	
		}				
	}
    
    //@author Moritz Hackhofer
    private void compute3DFracFFT(float fracDim, int greyMaxR, int greyMaxG, int greyMaxB ) {
    	  
    	dlgProgress.setBarIndeterminate(false);
    	int percent;
    	int width  = (int)datasetOut.dimension(0);
    	int height = (int)datasetOut.dimension(1);
    	int depth  = (int)datasetOut.dimension(2);
    	long[] pos;
    	
    	//create empty volume
		//volFloat = new ArrayImgFactory<>(new FloatType()).create(width, height, depth);
    	
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
   
		volFFT = new float[slices][rows][2*columns]; //Every frequency entry needs a pair of columns: for real and imaginary part
		
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
		
		percent = 1;
		dlgProgress.updatePercent(String.valueOf(percent+"%"));
		dlgProgress.updateBar(percent);
		//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
		statusService.showStatus(percent, 100, "Initializing finished");
		
		// Loop through all pixels.
		for (int k1 = 0; k1 < slices; k1++) {
			
			percent = (int)Math.max(Math.round((  ((float)k1)/((float)slices)   *100.f   )), percent);
			dlgProgress.updatePercent(String.valueOf(percent+"%"));
			dlgProgress.updateBar(percent);
			//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
			statusService.showStatus((k1+1), slices, "Processing " + (k1+1) + "/" + slices);
			
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
			
		//Find min max;
		float min = Float.MAX_VALUE;
		float max = -Float.MAX_VALUE;	
		// Loop through all pixels.
		for (int k1 = 0; k1 < slices; k1++) {
			
			percent = (int)Math.round((  ((float)k1)/((float)slices)   *100.f   ));
			dlgProgress.updatePercent(String.valueOf(percent+"%"));
			dlgProgress.updateBar(percent);
			//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
			statusService.showStatus((k1+1), slices, "Processing " + (k1+1) + "/" + slices); 
			
			for (int k2 = 0; k2 < rows; k2++) {
				for (int k3 = 0; k3 < columns; k3++) {
					real = volFFT[k1][k2][2*k3];
					if (real > max) {
						max = real;
					}
					if (real < min) {
						min = real;
					}
				}
			}
		}
		
		cursor = datasetOut.cursor();	
		
		if (colorModelType.equals("Grey-8bit")) {
	    	pos = new long[3];
	    	float rf = ((float)greyMaxR/(max-min)); //rescale factor
			while (cursor.hasNext()) {
				cursor.fwd();
				cursor.localize(pos);
				real = volFFT[(int)pos[2]][(int)pos[1]][(int)(2*pos[0])];
				real = rf * (real - min); //Rescale to 0  - greyMax
				cursor.get().setReal((int)(Math.round(real)));	
			}		
					
		} else if (colorModelType.equals("Color-RGB")) {
			
		 	pos = new long[4];
		 	float rfR = ((float)greyMaxR/(max-min)); //rescale factor
		 	float rfG = ((float)greyMaxG/(max-min)); //rescale factor
		 	float rfB = ((float)greyMaxB/(max-min)); //rescale factor
		 	float realR;
		 	float realG;
		 	float realB;
			while (cursor.hasNext()) {
				cursor.fwd();
				cursor.localize(pos);
				real = volFFT[(int)pos[2]][(int)pos[1]][(int)(2*pos[0])];
				if      (pos[3] == 0) { realR = rfR * (real - min); cursor.get().setReal((int)(Math.round(realR))); }
				else if (pos[3] == 1) { realG = rfG * (real - min); cursor.get().setReal((int)(Math.round(realG))); }
				else if (pos[3] == 2) { realB = rfB * (real - min); cursor.get().setReal((int)(Math.round(realB))); }
			} 	
		}	
		
		volFFT = null;
	}
    
    
    //@author Moritz Hackhofer
    private void compute3DFracMPD(float fracDim, int greyMaxR, int greyMaxG, int greyMaxB) {
    	
    	dlgProgress.setBarIndeterminate(false);
    	int percent;
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
		volMPD = new float[N+1][M+1][O+1];
			
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
		
		percent = 1;
		dlgProgress.updatePercent(String.valueOf(percent+"%"));
		dlgProgress.updateBar(percent);
		//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
		statusService.showStatus(percent, 100, "Initializing finished");
		
		// Iterate until maxLevel is reached. After that every pixel has a value assigned
		for (int stage = 0; stage < maxLevel; stage++) {
			
			percent = (int)Math.max(Math.round((  ((float)stage)/((float)maxLevel)   *100.f   )), percent);
			dlgProgress.updatePercent(String.valueOf(percent+"%"));
			dlgProgress.updateBar(percent);
			//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
			statusService.showStatus((stage+1), maxLevel, "Processing " + (stage+1) + "/" + maxLevel);
			
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
		
		//Write to output
		float allMin = getMin(volMPD, N, M, O);
		float allMax = getMax(volMPD, N, M, O);		
				
    	cursor = datasetOut.cursor();	
		
		if (colorModelType.equals("Grey-8bit")) {
	 		
			float scale = (float)greyMaxR / (allMax - allMin);	
			ra = datasetOut.randomAccess();
			
			// write to Output	
			for (int k1 = 0; k1 < width; k1++) {
				for (int k2 = 0; k2 < height; k2++) {
					for (int k3 = 0; k3 < depth; k3++) {
						ra.setPosition(k1, 0);
						ra.setPosition(k2, 1);
						ra.setPosition(k3, 2);
						ra.get().setReal((int) Math.round((volMPD[k1][k2][k3] - allMin) * scale));
					}
				}
			} 
					
		} else if (colorModelType.equals("Color-RGB")) {
			
			float scaleR = (float)greyMaxR / (allMax - allMin);	
			float scaleG = (float)greyMaxG / (allMax - allMin);	
			float scaleB = (float)greyMaxB / (allMax - allMin);	
			ra = datasetOut.randomAccess();
			
			// write to Output	
			for (int k1 = 0; k1 < width; k1++) {
				for (int k2 = 0; k2 < height; k2++) {
					for (int k3 = 0; k3 < depth; k3++) {
						ra.setPosition(k1, 0);
						ra.setPosition(k2, 1);
						ra.setPosition(k3, 2);
						ra.setPosition(0, 3);
						ra.get().setReal((int) Math.round((volMPD[k1][k2][k3] - allMin) * scaleR));
						ra.setPosition(1, 3);
						ra.get().setReal((int) Math.round((volMPD[k1][k2][k3] - allMin) * scaleG));
						ra.setPosition(2, 3);
						ra.get().setReal((int) Math.round((volMPD[k1][k2][k3] - allMin) * scaleB));
					}
				}
			} 
		}	
		
		volFFT = null;  	
	}
    
	// Method for getting the maximum value
    public static float getMax(float[][][] inputArray, int N, int M, int O) { 
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
    
    //This methods computes a Menger volume
    //FD = log20/log3 = 2.7268
    private void compute3DFracMenger(int numIterations, int greyMaxR, int greyMaxG, int greyMaxB) {
	    	
    	dlgProgress.setBarIndeterminate(false);
    	int percent;
	 	int width  = (int)datasetOut.dimension(0);
    	int height = (int)datasetOut.dimension(1);
    	int depth  = (int)datasetOut.dimension(2);

		volIFS = new ArrayImgFactory<>(new FloatType()).create(width, height, depth);	
		raF = volIFS.randomAccess();
	
		// set initial centered cube
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
			raF.get().setReal(255);
		}
		}
		}
			
		// Declare interpolation type
		String interpolType = "Linear";
		InterpolatorFactory factory = null;
		if      (interpolType.contentEquals("Linear") )           factory = new NLinearInterpolatorFactory<FloatType>();
		else if (interpolType.contentEquals("Lanczos") )          factory = new LanczosInterpolatorFactory<FloatType>();
		else if (interpolType.contentEquals("Floor") )            factory = new FloorInterpolatorFactory<FloatType>();
		else if (interpolType.contentEquals("Nearest Neighbor") ) factory = new NearestNeighborInterpolatorFactory<FloatType>();
				
		RealRandomAccessible<FloatType> interpolant;
		AffineRandomAccessible<FloatType, AffineGet> transformed;
		IntervalView<FloatType> bounded;	
		AffineTransform3D at3D = new AffineTransform3D();
		//at3D.setTranslation(0.0, 0.0, 0.0);
		//at3D.set(m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23);
		at3D.set(1.0/3.0,     0.0,     0.0,   0.0,
				     0.0, 1.0/3.0,     0.0,   0.0,
				     0.0,     0.0, 1.0/3.0,   0.0);
		
		percent = 10;
		dlgProgress.updatePercent(String.valueOf(percent+"%"));
		dlgProgress.updateBar(percent);
		//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
		statusService.showStatus(percent, 100, "Initializing finished");
		
		// Affine transformations-----------------------------------------------------------------
		for (int i = 0; i < numIterations; i++) {
		
			percent = (int)Math.max(Math.round((  ((float)i)/((float)numIterations)   *100.f   )), percent);
			dlgProgress.updatePercent(String.valueOf(percent+"%"));
			dlgProgress.updateBar(percent);
			//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
			statusService.showStatus((i+1), numIterations, "Processing " + (i+1) + "/" + numIterations);
			
			volIFSCopy = volIFS.copy();
			
			//fast way with only one affine transformation!
			interpolant = Views.interpolate(Views.extendMirrorSingle(volIFSCopy), factory); //and only one transformation
			//or slow way wit hseveral transformations
			//interpolant = Views.interpolate(Views.extendZero(volIFSTemp), factory);	//and several transformations and stiching together needed
			
			//Transform the volume
			transformed = RealViews.affine(interpolant, at3D);	
			//Apply the original interval to the transformed image 
			bounded = Views.interval(transformed, volIFS); //ifsVolume 1 or ifsVolume2 does not matter, only size is taken
//			Dataset dataset = datasetService.create(bounded);
//			uiService.show("bounded", bounded);		
			//Adding together
			cursorF = volIFS.cursor();
			raF = bounded.randomAccess();
	    	long[] pos = new long[3];
			while (cursorF.hasNext()) {
				cursorF.fwd();
				cursorF.localize(pos);
				raF.setPosition(pos);
				if (cursorF.get().getRealFloat() == 0) { //do not overwrite already white pixels
					cursorF.get().set((int)Math.round(((FloatType)raF.get()).getRealFloat()));
				} 
			}  			
		}
			
		// Convert and write to Output---------------------------------------
		cursor = datasetOut.cursor(); //3D (Grey) or 4D (RGB)
		raF = volIFS.randomAccess();  //raF always 3D
    
		if (colorModelType.equals("Grey-8bit")) {
			long[] pos = new long[3];
			while (cursor.hasNext()) {
				cursor.fwd();
				cursor.localize(pos);
				raF.setPosition(pos);
				if (raF.get().getRealFloat() > 0) {
					cursor.get().setReal(greyMaxR);
				}			
			}  	
					
		} else if (colorModelType.equals("Color-RGB")) {
			long[] pos = new long[4];
			while (cursor.hasNext()) {
				cursor.fwd();
				cursor.localize(pos);
				raF.setPosition(pos);
				//raF always 3D but does not matter here, only the three first positions are taken
				if (raF.get().getRealFloat() > 0) { //raF always 3D
					if      (pos[3] == 0) cursor.get().setReal(greyMaxR);
					else if (pos[3] == 1) cursor.get().setReal(greyMaxG);
					else if (pos[3] == 2) cursor.get().setReal(greyMaxB);
				}			
			}  	
		}	
		
		volIFS     = null;
		volIFSCopy = null;
	}
    
    //This methods computes a Sierpinski volume
    // FD = log5/log2 = 2.3219
    private void compute3DFracSierpinski(int numIterations, int greyMaxR, int greyMaxG, int greyMaxB) {
		
    	dlgProgress.setBarIndeterminate(false);
    	int percent;
    	long[] pos;
    	RealRandomAccessible<FloatType> interpolant;
    	RealRandomAccess<FloatType> rraF;
		AffineRandomAccessible<FloatType, AffineGet> transformed;
		IntervalView<FloatType> bounded;	
		AffineTransform3D at3D  = new AffineTransform3D();
		AffineTransform3D at3D1 = new AffineTransform3D();
		AffineTransform3D at3D2 = new AffineTransform3D();
		AffineTransform3D at3D3 = new AffineTransform3D();
		AffineTransform3D at3D4 = new AffineTransform3D();
		AffineTransform3D at3D5 = new AffineTransform3D();
    	
	 	int width  = (int)datasetOut.dimension(0);
    	int height = (int)datasetOut.dimension(1);
    	int depth  = (int)datasetOut.dimension(2);
    	
    	height = depth = width; //All sizes must be equal for quadratic pyramid
    	double size = width;
    	double hPyramid = size/Math.sqrt(2);  //Height of pyramid
    	int offsetZ = (int)Math.round(((float)depth - (float)hPyramid)/2f);
    	int numSlices = (int)Math.round(hPyramid);
    	
    	//Define the coner points of footprint
    	double[] corner1 = new double[] {0, 0, 0};
    	double[] corner2 = new double[] {size-1, 0, 0};
    	double[] corner3 = new double[] {0, size-1, 0};
    	double[] corner4 = new double[] {size-1, size-1, 0};
    	
    	//Define direction vectors from the corners to the tip of the pyramid
    	//These follow the edges of the pyramid and are needed to define the intersection points with the subsequent volume slices
    	//See intersections of a line with a plane
    	double[] dir1 = new double[] { size/2.0,  size/2.0, size/Math.sqrt(2) }; 
    	double[] dir2 = new double[] {-size/2.0,  size/2.0, size/Math.sqrt(2) }; 
    	double[] dir3 = new double[] { size/2.0, -size/2.0, size/Math.sqrt(2) }; 
    	double[] dir4 = new double[] {-size/2.0, -size/2.0, size/Math.sqrt(2) }; 

		volIFS = new ArrayImgFactory<>(new FloatType()).create(width, height, depth);	
		//raF = volIFS.randomAccess();
	
		
		// Declare interpolation type
		String interpolType = "Linear";
		InterpolatorFactory factory = null;
		if      (interpolType.contentEquals("Linear") )           factory = new NLinearInterpolatorFactory<FloatType>();
		else if (interpolType.contentEquals("Lanczos") )          factory = new LanczosInterpolatorFactory<FloatType>();
		else if (interpolType.contentEquals("Floor") )            factory = new FloorInterpolatorFactory<FloatType>();
		else if (interpolType.contentEquals("Nearest Neighbor") ) factory = new NearestNeighborInterpolatorFactory<FloatType>();
				
		//interpolant = Views.interpolate(Views.extendMirrorSingle(volIFS), factory);				
		
		raF = volIFS.randomAccess();

		// set initial footprint of quadratic pyramid
		double xMin = corner1[0];
		double xMax = corner2[0];
		double yMin = corner1[1];
		double yMax = corner3[1];
		double z = 0;
		
		for (double x = xMin; x <= xMax; x=x+0.5) { //some points may be written twice
		for (double y = yMin; y <= yMax; y=y+0.5) {
			raF.setPosition((int)Math.round(x), 0);
			raF.setPosition((int)Math.round(y), 1);
			raF.setPosition((int)Math.round(z), 2);
			raF.get().setReal(255);
		}
		}
		
//		Dataset dataset = datasetService.create(volIFS);
//		uiService.show("volIFS", dataset);	
		
		//set subsequent squares in the corresponding slices
		//number of slices == size;
		//k slope of line;
		double k = 0;
		for (int s = 1; s < numSlices; s++) {
			k= Math.sqrt(2)*(double)s/size;   //distance is equal to slice number
			xMin = corner1[0] + k * dir1[0];  //x minimal coordinate of two intersection point 
			xMax = corner2[0] + k * dir2[0];  //x maximal coordinate of two intersection point
			yMin = corner1[1] + k * dir1[1];
			yMax = corner3[1] + k * dir3[1];
			z = s;	
			for (double x = xMin; x <= xMax; x=x+0.5) { //some points may be written twice
			for (double y = yMin; y <= yMax; y=y+0.5) {
				raF.setPosition((int)Math.round(x), 0);
				raF.setPosition((int)Math.round(y), 1);
				raF.setPosition((int)Math.round(z), 2);
				raF.get().setReal(255);
			}
			}		
		}
		
		//at3D1.scale(0.5);
		//at3D1.setTranslation(0.0, 0.0, 0.0);
		//at3D.set(m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23);	
		
		at3D1.set(0.5, 0.0, 0.0,   1.0 / 4.0 * width,  
				  0.0, 0.5, 0.0,   1.0 / 4.0 * height,
				  0.0, 0.0, 0.5,   1.0/Math.sqrt(2) * depth /2.0); //Top pyramid
		
		at3D2.set(0.5, 0.0, 0.0,   0.5 * width,
				  0.0, 0.5, 0.0,   0.5 * height,
				  0.0, 0.0, 0.5,   0.0 * depth);
		
		at3D3.set(0.5, 0.0, 0.0,   0.0 * width,
				  0.0, 0.5, 0.0,   0.0 * height,
				  0.0, 0.0, 0.5,   0.0 * depth);
		
		at3D4.set(0.5, 0.0, 0.0,   0.5 * width,
				  0.0, 0.5, 0.0,   0.0 * height,
				  0.0, 0.0, 0.5,   0.0 * depth);
		
		at3D5.set(0.5, 0.0, 0.0,   0.0 * width,
				  0.0, 0.5, 0.0,   0.5 * height,
				  0.0, 0.0, 0.5,   0.0 * depth);
		
		percent = 10;
		dlgProgress.updatePercent(String.valueOf(percent+"%"));
		dlgProgress.updateBar(percent);
		//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
		statusService.showStatus(percent, 100, "Initializing finished");
		
		// Affine transformations-----------------------------------------------------------------
		for (int i = 0; i < numIterations; i++) {
		
			percent = (int)Math.max(Math.round((  ((float)i)/((float)numIterations)   *100.f   )), percent);
			dlgProgress.updatePercent(String.valueOf(percent+"%"));
			dlgProgress.updateBar(percent);
			//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
			statusService.showStatus((i+1), numIterations, "Processing " + (i+1) + "/" + numIterations);
			
			volIFSCopy = volIFS.copy();
			
			for (int a = 1; a <= 5; a++ ) {
			
				if      (a == 1) at3D = at3D1;
				else if (a == 2) at3D = at3D2;
				else if (a == 3) at3D = at3D3;
				else if (a == 4) at3D = at3D4;
				else if (a == 5) at3D = at3D5;
			
				volIFSTemp = volIFSCopy.copy();
				
				interpolant = Views.interpolate(Views.extendZero(volIFSTemp), factory);	
				//Transform the volume
				transformed = RealViews.affine(interpolant, at3D);	
				//Apply the original interval to the transformed image 
				bounded = Views.interval(transformed, volIFS); //volIFS does not matter, only size is taken
//				Dataset dataset = datasetService.create(bounded);
//				uiService.show("bounded", bounded);		
				//Adding together
				cursorF = volIFS.cursor();
				raF = bounded.randomAccess();
		    	pos = new long[3];
				while (cursorF.hasNext()) {
					cursorF.fwd();
					cursorF.localize(pos);
					raF.setPosition(pos);
					if 	    (a == 1) cursorF.get().set((int)Math.round(((FloatType)raF.get()).getRealFloat())); //Overwrite all pixels
					else {
						if (cursorF.get().getRealFloat() == 0) { //do not overwrite already white pixels
							cursorF.get().set((int)Math.round(((FloatType)raF.get()).getRealFloat()));
						} 	
					}				
				} 
			} //a		
		}
			
		// Convert and write to Output---------------------------------------
		cursor = datasetOut.cursor(); //3D (Grey) or 4D (RGB)
		raF = volIFS.randomAccess();  //always 3D
		long zz;
    
		if (colorModelType.equals("Grey-8bit")) {
			pos = new long[3];
			while (cursor.hasNext()) {
				cursor.fwd();
				cursor.localize(pos);
				zz = (depth - 1) - pos[2] - offsetZ; // mirrored vertically and shifted to center  	
				if (zz > 0 && zz < depth) {
					raF.setPosition(pos[0], 0);
					raF.setPosition(pos[1], 1); 
					raF.setPosition(zz, 2);
					if (raF.get().getRealFloat() > 0) {
						cursor.get().setReal(greyMaxR);
					}
				}
			}  	
					
		} else if (colorModelType.equals("Color-RGB")) {
			pos = new long[4];
			while (cursor.hasNext()) {
				cursor.fwd();
				cursor.localize(pos);
				zz = (depth - 1) - pos[2] - offsetZ; // mirrored vertically and shifted to center  	
				if (zz > 0 && zz < depth) {
					raF.setPosition(pos[0], 0);
					raF.setPosition(pos[1], 1); 
					raF.setPosition(zz, 2);
					//raF always 3D
					if (raF.get().getRealFloat() > 0) {
						if      (pos[3] == 0) cursor.get().setReal(greyMaxR);
						else if (pos[3] == 1) cursor.get().setReal(greyMaxG);
						else if (pos[3] == 2) cursor.get().setReal(greyMaxB);
					}			
				}
					
			}  		
		
		} 
	
		volIFS     = null;
		volIFSTemp = null;
		volIFSCopy = null;
    }
    
    
    /**
     * @param 
     * @throws Exception
     */
    @Override
    public void run() {
    	//dlgProgress = new WaitingDialogWithProgressBar("<html>Generating a 3D image volume, please wait...<br>Open console window for further info.</html>");
		dlgProgress = new WaitingDialogWithProgressBar("Generating a 3D image volume, please wait... Open console window for further info.",
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
		colorModelType          = choiceRadioButt_ColorModelType;//"Grey-8bit", "Color-RGB"
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
		else if (volumeType.equals("Fractal IFS - Sierpinski"))					name = "Fractal IFS - Sierpinski";
	
				
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
					
		} else if (colorModelType.equals("Color-RGB")) {
			
			bitsPerPixel = 8;
			dims = new long[]{width, height, depth, 3};
			axes = new AxisType[]{Axes.X, Axes.Y, Axes.Z, Axes.CHANNEL};
			datasetOut = datasetService.create(dims, name, axes, bitsPerPixel, signed, floating, virtual);
			datasetOut.setCompositeChannelCount(3);
			datasetOut.setRGBMerged(true);
		}

		long startTime = System.currentTimeMillis();
		logService.info(this.getClass().getName() + " Generating image volume");
		
		if      (volumeType.equals("Random"))   					compute3DRandom(greyR, greyG, greyB);
		else if (volumeType.equals("Gaussian")) 					compute3DGaussian(greyR, greyG, greyB);
		else if (volumeType.equals("Constant")) 					compute3DConstant(greyR, greyG, greyB);
		else if (volumeType.equals("Fractal volume - FFT"))			compute3DFracFFT(fracDim, greyR, greyG, greyB);
		else if (volumeType.equals("Fractal volume - MPD")) 		compute3DFracMPD(fracDim, greyR, greyG, greyB);
		else if (volumeType.equals("Fractal IFS - Menger")) 		compute3DFracMenger(numIterations, greyR, greyG, greyB);
		else if (volumeType.equals("Fractal IFS - Sierpinski")) 	compute3DFracSierpinski(numIterations, greyR, greyG, greyB);
			
		int percent = 0;
		dlgProgress.updatePercent(String.valueOf(percent+"%"));
		dlgProgress.updateBar(percent);
		statusService.showProgress(0, 100);
		statusService.clearStatus();
		
		dlgProgress.addMessage("Processing finished! Displaying image volume...");
		//not necessary because datasetOut is an IO type
		//uiService.show(datasetOut.getName(), datasetOut);
		
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
