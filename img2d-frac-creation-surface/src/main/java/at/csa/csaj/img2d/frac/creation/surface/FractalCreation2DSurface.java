/*-
 * #%L
 * Project: ImageJ plugin to create 2D fractal surfaces.
 * File: FractalCreation2DSurface.java
 * 
 * $Id$
 * $HeadURL$
 * 
 * This file is part of ComsystanJ software, hereinafter referred to as "this program".
 * %%
 * Copyright (C) 2020 - 2021 Comsystan Software
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
package at.csa.csaj.img2d.frac.creation.surface;

import net.imagej.DatasetService;

import net.imagej.ImageJ;
import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.ComplexType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import org.scijava.ItemIO;
import org.scijava.ItemVisibility;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.command.Previewable;
import org.scijava.display.DisplayService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import org.scijava.widget.ChoiceWidget;
import org.scijava.widget.NumberWidget;

import at.csa.csaj.commons.dialog.WaitingDialogWithProgressBar;
import io.scif.services.DatasetIOService;

import java.text.SimpleDateFormat;
import java.util.Random;
import java.util.TimeZone;

import javax.swing.UIManager;
/**
 * This is an ImageJ {@link Command} plugin for creation of 2D Fractals.
 * <p>
 * The code here is using
 * </p>
 * <p>
 * The {@link run} method implements the computations.
 * </p>
 */
@Plugin(type = Command.class, menuPath = "Plugins>ComsystanJ>Image(2D)>Fractal surface creation")
public class FractalCreation2DSurface<T extends RealType<T>> implements Command, Previewable {
	
	private static final String PLUGIN_LABEL = "Computes 2D grey value fractals";
	private static final String SPACE_LABEL = "";
  
	@Parameter
	private LogService logService;

	@Parameter
	private StatusService statusService;
	
	@Parameter(label = "Fractal Surface", type = ItemIO.OUTPUT)
	private Img<UnsignedByteType> resultImg;

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
    
    //Widget elements------------------------------------------------------
    
    @Parameter(visibility = ItemVisibility.MESSAGE)
	private final String labelPlugin = PLUGIN_LABEL;

    @Parameter(visibility = ItemVisibility.MESSAGE)
  	private final String labelSpace = SPACE_LABEL;
    
    
    @Parameter(label = "Width [pixel]:",
    		   style = NumberWidget.SPINNER_STYLE,
    		   min = "1",
    		   max = "32768",
    		   initializer = "initialWidth",
    		   stepSize = "1",
    		   callback = "changedWidth")
    private int spinnerInteger_Width;
    
    @Parameter(label = "Height [pixel]:",
 		       style = NumberWidget.SPINNER_STYLE,
 		       min = "1",
 		       max = "32768",
 		       initializer = "initialHeight",
 		       stepSize = "1",
 		       callback = "changedHeight")
    private int spinnerInteger_Height;
    
    @Parameter(label = "Fractal Dimension:",
	  		   style = NumberWidget.SPINNER_STYLE,
	  		   min = "2",
	  		   max = "3",
	  		   initializer = "initialFracDim",
	  		   stepSize = "0.1",
	  		   callback = "changedFracDim")
     private float spinnerFloat_FracDim;
    
    @Parameter(label = "Method",
    		   style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
    		   choices = { "FFT", "MPD"},
    		   initializer = "initialMethod",
               callback = "changedMethod")
    private String choiceRadioButt_Method;
    //---------------------------------------------------------------------
    
    //The following initialzer functions set initial values
    protected void initialWidth() {
    	spinnerInteger_Width = 512;
    }
    protected void initialHeight() {
    	spinnerInteger_Height = 512;
    }
    protected void initialFracDim() {
    	spinnerFloat_FracDim = 2.5f;
    }
    protected void initialMethod() {
    	choiceRadioButt_Method = "FFT";
    }
    // The following method is known as "callback" which gets executed
 	// whenever the value of a specific linked parameter changes.
 	/** Executed whenever the {@link #spinInteger_Dim} parameter changes. */
 	protected void changedWidth() {
 		logService.info(this.getClass().getName() + " Width changed to " + spinnerInteger_Width + " pixel.");
 	}
 	protected void changedHeight() {
 		logService.info(this.getClass().getName() + " Height changed to " + spinnerInteger_Height + " pixel.");
 	}
 	protected void changedFracDim() {
 		//round to one decimal after the comma
 		spinnerFloat_FracDim = Math.round(spinnerFloat_FracDim * 10f)/10f;
 		logService.info(this.getClass().getName() + " Constant changed to " + spinnerFloat_FracDim);
 	}
    
	/** Executed whenever the {@link #choiceRadioButt_Method} parameter changes. */
	protected void changedMethod() {
		logService.info(this.getClass().getName() + " Method changed to " + choiceRadioButt_Method + ".");
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
 	
    //-------------------------------------------------------------------------------------------
    private <C extends ComplexType<C>> void computeFrac2DFFT(int width, int height, float fracDim) {
   
		//create empty image
		Img<FloatType> img = new ArrayImgFactory<>(new FloatType()).create(width, height);
		
//		//optionally set grey values
//		RandomAccess<FloatType> imgRa = img.randomAccess();
//		final long[] posImg = new long[img.numDimensions()];
//		// Loop through all pixels.
//		final Cursor<FloatType> cursorImg = img.localizingCursor();
//		while (cursorImg.hasNext()) {
//			cursorImg.fwd();
//			cursorImg.localize(posImg);
//			cursorImg.get().set(133f);
//		}
		
		RandomAccessibleInterval<C> raifft = opService.filter().fft(img);
		
	
//    	//Optinally Show fft image
//		Img<FloatType> fftMag1 = new ArrayImgFactory<>(new FloatType()).create(fft.dimension(0), fft.dimension(1));
//		RandomAccess<FloatType> fftMag1Ra = fftMag1.randomAccess();
//		final long[] posFFT1 = new long[fftMag1.numDimensions()];
//		// Loop through all pixels.
//		final Cursor<C> cursorFFT1 = Views.iterable(fft).localizingCursor();
//		while (cursorFFT1.hasNext()) {
//			cursorFFT1.fwd();
//			cursorFFT1.localize(posFFT1);
//			//float power = (float) Math.sqrt(cursorFFT1.get().getRealFloat() * cursorFFT1.get().getRealFloat() + cursorFFT1.get().getImaginaryFloat() * cursorFFT1.get().getImaginaryFloat()  );
//			float power = cursorFFT1.get().getPowerFloat();
//			fftMag1Ra.setPosition(posFFT1);
//			fftMag1Ra.get().set(power);
//		}
//		uiService.show("fftMag1", fftMag1);		
		
		// Declare an array to hold the current position of the cursor.
		final long[] posFFT = new long[raifft.numDimensions()];
		
		// Define origin as 0,0. //frequency = 0;
		final long[] origin = {0, 0};

		// Define a 2nd 'origin' at bottom left of image.   //frequency = 0;
		final long[] origin2 = {0, raifft.dimension(1)};

			
		// The 2nd 'origin' is at bottom left of the fft image.
		// fft  consists of two images above each other with a single width but doubled height
		//final long fftWidth     = fft.dimension(0);
		final long fftHalfHeight= raifft.dimension(1)/2;
		
		// generate random pixel values
		Random generator = new Random();
		double b = 8.0f - (2.0f * fracDim);		// FD = (B+6)/2 laut Closed contour fracatal dimension estimation... J.B. Florindo

		// Loop through all pixels.
		final Cursor<C> cursorFFT = Views.iterable(raifft).localizingCursor();
		while (cursorFFT.hasNext()) {
			cursorFFT.fwd();
			cursorFFT.localize(posFFT);
			
			if (posFFT[1] <= fftHalfHeight) {
				// Calculate distance from 0,0 
				final double dist = Util.distance(origin, posFFT);	
				//double newMagnitude = Math.pow(dist+1, -b / 2);
				//float imag = cursorFFT.get().getImaginaryFloat();
				//float real = (float) Math.sqrt(newMagnitude*newMagnitude - (imag*imag));
				//cursorFFT.get().setReal(real);
				
				double g = generator.nextGaussian();
				double u = generator.nextFloat();
				double n = g * Math.cos(2 * Math.PI * u);
				double m = g * Math.sin(2 * Math.PI * u);
				n = n * Math.pow(dist+1, -b / 2);
				m = m * Math.pow(dist+1, -b / 2);
				cursorFFT.get().setReal(n);
				cursorFFT.get().setImaginary(m);			
				
				//let imaginary unchanged
				//System.out.println("FracCreate2DSurface- posFFT[0]:"+ posFFT[0] + " posFFT[1]:" + posFFT[1] +" dist:"+ dist +" newMagnitude:" + newMagnitude + " real:" + real + " imag:" + imag);
			}
			else if (posFFT[1] > fftHalfHeight) {
				// Calculate distance from bottom left corner
				final double dist2 = Util.distance(origin2, posFFT);
				double g = generator.nextGaussian();
				double u = generator.nextFloat();
				double n = g * Math.cos(2 * Math.PI * u);
				double m = g * Math.sin(2 * Math.PI * u);
				n = n * Math.pow(dist2+1, -b / 2);
				m = m * Math.pow(dist2+1, -b / 2);
				cursorFFT.get().setReal(n);
				cursorFFT.get().setImaginary(m);		
			

				//System.out.println("FracCreate2DSurface- posFFT[0]:"+ posFFT[0] + " posFFT[1]:" + posFFT[1] +" dist2:"+ dist2 +" newMagnitude:" + newMagnitude + " real:" + real + " imag:" + imag);
				}
		}
		
		//uiService.show("img", img);
		//uiService.show("fft", fft);
		
		//Inverse FFT and show image	
		Img<FloatType> ifft = opService.create().img(img, new FloatType());
		opService.filter().ifft(ifft, raifft);
		//change from FloatType to UnsignedByteType
		//find min and max values
		Cursor<FloatType> cursor = ifft.cursor();
		float min = Float.MAX_VALUE;
		float max = -Float.MAX_VALUE;
		float value;
		while (cursor.hasNext()){
			cursor.fwd();
			value =  cursor.get().getRealFloat();
			if (value > max) max = value;
			if (value < min) min = value;
		}
		
		
		resultImg = opService.create().img(img, new UnsignedByteType());
		RandomAccess<UnsignedByteType> ra = resultImg.randomAccess();
		cursor = ifft.cursor();
    	final long[] pos = new long[resultImg.numDimensions()];
    	float rf = (255/(max-min)); //rescale factor
		while (cursor.hasNext()) {
			cursor.fwd();
			cursor.localize(pos);
			value= cursor.get().getRealFloat();
			//value = rf * (value -min); //Rescale to 0  255
			ra.setPosition(pos);
			ra.get().set((int)(rf * (value -min)));
		}
		//resultImg;
	}
    
    //
    private void computeFrac2DMPD(int width, int height,  double fracDim) {
  
		
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
    	//WaitingDialog dlg = new WaitingDialog("<html>Creation of a 2D surface image, please wait...</html>", false);
		WaitingDialogWithProgressBar dlgProgress = new WaitingDialogWithProgressBar("Creating 2D fractal surface, please wait... Open console window for further info.",
                logService, false, null); //isCanceable = false, because no following method listens to exec.shutdown 
		dlgProgress.setVisible(true);	
    	long startTime = System.currentTimeMillis();
         // create the ImageJ application context with all available services
    	//final ImageJ ij = new ImageJ();
    	//ij.ui().showUI();

		//Collect parameters
		int     width = spinnerInteger_Width;
		int    height = spinnerInteger_Height;
		float fracDim = spinnerFloat_FracDim;
			
		//please select method "FFT" or "MPD"
		//String method = "FFT" ;	
		if (choiceRadioButt_Method.equals("FFT")) computeFrac2DFFT(width, height, fracDim);
		if (choiceRadioButt_Method.equals("MPD")) computeFrac2DMPD(width, height, fracDim);
		
		//ij.ui().show("Fractal 3D", dataset3D);
		//if (choiceRadioButt_Method.equals("FFT")) uiService.show("3D Fractal-FFT", resultImg);
		//if (choiceRadioButt_Method.equals("MPD")) uiService.show("3D Fractal-MPD", resultImg);
		
		long duration = System.currentTimeMillis() - startTime;
		TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
		SimpleDateFormat sdf = new SimpleDateFormat();
		sdf.applyPattern("HHH:mm:ss:SSS");
		logService.info(this.getClass().getName() + " Elapsed time: "+ sdf.format(duration));
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
         ij.command().run(FractalCreation2DSurface.class, true);
    	
    }

}
