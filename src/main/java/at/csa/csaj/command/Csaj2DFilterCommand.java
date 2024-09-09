/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj2DFilterCommand.java
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
package at.csa.csaj.command;

import java.awt.Frame;
import java.awt.Toolkit;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JFrame;
import javax.swing.UIManager;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ImageJ;
import net.imagej.Position;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.display.ImageDisplayService;
import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.convolution.fast_gauss.FastGauss;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.outofbounds.OutOfBoundsBorderFactory;
import net.imglib2.type.numeric.ComplexType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

import org.apache.commons.math3.util.Precision;
import org.scijava.ItemIO;
import org.scijava.ItemVisibility;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.command.ContextCommand;
import org.scijava.command.Previewable;
import org.scijava.display.DefaultDisplayService;
import org.scijava.log.LogService;
import org.scijava.menu.MenuConstants;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;
import org.scijava.ui.DialogPrompt.MessageType;
import org.scijava.ui.DialogPrompt.OptionType;
import org.scijava.ui.DialogPrompt.Result;
import org.scijava.ui.UIService;
import org.scijava.widget.Button;
import org.scijava.widget.ChoiceWidget;
import org.scijava.widget.FileWidget;
import org.scijava.widget.NumberWidget;

import at.csa.csaj.commons.CsajDialog_WaitingWithProgressBar;
import edu.emory.mathcs.jtransforms.fft.FloatFFT_2D;
import io.scif.DefaultImageMetadata;
import io.scif.MetaTable;


/**
 * A {@link ContextCommand} plugin for <filtering</a>
 * an image.
 */
@Plugin(type = ContextCommand.class,
		headless = true,
		initializer = "initialPluginLaunch",
		iconPath = "/icons/comsystan-logo-grey46-16x16.png", //Menu entry icon
		menu = {})
/**
 * Csaj Interactive: InteractiveCommand (nonmodal GUI without OK and cancel button, NOT for Scripting!)
 * Csaj Macros:      ContextCommand     (modal GUI with OK and Cancel buttons, for scripting)
 * Developer note:
 * Develop the InteractiveCommand plugin Csaj***.java
 * The Maven build will execute CreateCommandFiles.java which creates Csaj***Command.java files
 *
 *
 */
public class Csaj2DFilterCommand<T extends RealType<T>> extends ContextCommand implements Previewable {

	private static final String PLUGIN_LABEL          = "<html><b>Filter</b></html>";
	private static final String SPACE_LABEL           = "";
	private static final String FILTEROPTIONS_LABEL   = "<html><b>Filtering options</b></html>";
	private static final String DISPLAYOPTIONS_LABEL  = "<html><b>Display options</b></html>";
	private static final String PROCESSOPTIONS_LABEL  = "<html><b>Process options</b></html>";

	FloatFFT_2D FFT;
	private static float[][] matrixA;
	private static RandomAccessibleInterval<FloatType>  raiWindowed; 
	Cursor<?> cursor = null;
	private static String datasetName;
	private static String[] sliceLabels;
	private static long width     = 0;
	private static long height    = 0;
	private static long numDimensions = 0;
	private static long numSlices = 0;
	private static long compositeChannelCount =0;
	private static String imageType = "";
	
	private static final String imageOutName = "Filtered image(s)";
	private static final String imagePreviewName = "Preview image";
	private static Dataset datasetPreview;
	
	private CsajDialog_WaitingWithProgressBar dlgProgress;
	private ExecutorService exec;
	
	@Parameter
	private ImageJ ij;

	@Parameter
	private PrefService prefService;

	@Parameter
	private LogService logService;
	
	@Parameter
	private StatusService statusService;

	@Parameter
	private OpService opService;

	@Parameter
	private UIService uiService;

	@Parameter
	private ImageDisplayService imageDisplayService;
	
	//This parameter does not work in an InteractiveCommand plugin (duplicate displayService error during startup) pom-scijava 24.0.0
	//in Command Plugin no problem
	//@Parameter  
	//private DisplayService displayService;
	
	@Parameter  //This works in an InteractiveCommand plugin
    private DefaultDisplayService defaultDisplayService;
	
	@Parameter
	private DatasetService datasetService;
	
//	@Parameter
//	private ROIService roiService;

	// Input dataset which is updated in callback functions
	@Parameter(type = ItemIO.INPUT)
	private Dataset datasetIn;

	@Parameter(label = imageOutName, type = ItemIO.OUTPUT)
	private Dataset datasetOut;
	
//	@Parameter
//	private Img<T> image;

	// Widget elements------------------------------------------------------
	//-----------------------------------------------------------------------------------------------------
//	@Parameter(label = " ",visibility = ItemVisibility.MESSAGE, persist = false)
//	private final String labelPlugin = PLUGIN_LABEL;
//
//	@Parameter(visibility = ItemVisibility.MESSAGE, persist = false)
//	private final String labelSpace = SPACE_LABEL;

	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelFilterOptions = FILTEROPTIONS_LABEL;
	
	@Parameter(label = "Filter",
			   description = "Filter type",
			   style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
			   choices = {"Gaussian blur", "Mean", "Median", "Low pass - FFT", "High pass - FFT"}, //
			   persist = true,  //restore previous value default = true
			   initializer = "initialFilterType",
			   callback = "callbackFilterType")
	private String choiceRadioButt_FilterType;
	
	@Parameter(label = "(Gauss)Sigma",
			   description = "Sigma of Gaussina blur",
			   style = NumberWidget.SPINNER_STYLE, 
			   min = "0.5",
			   max = "9999999999999999999",
			   stepSize = "0.1",
			   persist = true, // restore  previous value  default  =  true
			   initializer = "initialSigma",
			   callback = "callbackSigma")
	private float spinnerFloat_Sigma;
	
	@Parameter(label = "(Mean/Median)Kernel size",
			   description = "Kernel size in pixel",
			   style = NumberWidget.SPINNER_STYLE, 
			   min = "1",
			   max = "9999999999999999999",
			   stepSize = "1",
			   persist = true, // restore  previous value  default  =  true
			   initializer = "initialKernelSize",
			   callback = "callbackKernelSize")
	private int spinnerInteger_KernelSize;
	
	
	@Parameter(label = "(FFT)Radius",
			   description = "Cutoff frequency - distance from frequency = 0",
			   style = NumberWidget.SPINNER_STYLE, 
			   min = "0",
			   max = "9999999999999999999",
			   stepSize = "1",
			   persist = true, // restore  previous value  default  =  true
			   initializer = "initialRadius",
			   callback = "callbackRadius")
	private int spinnerInteger_Radius;
	
	//-----------------------------------------------------------------------------------------------------
    @Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
    private final String labelDisplayOptions = DISPLAYOPTIONS_LABEL;
     
    @Parameter(label = "Overwrite result display(s)",
   	    	   description = "Overwrite already existing result images, plots or tables",
   	    	   persist = true,  //restore previous value default = true
   	    	   initializer = "initialOverwriteDisplays")
    private boolean booleanOverwriteDisplays;
    
	//-----------------------------------------------------------------------------------------------------
    @Parameter(label = " ", visibility = ItemVisibility.MESSAGE,  persist = false)
    private final String labelProcessOptions = PROCESSOPTIONS_LABEL;
    
    @Parameter(label = "Immediate processing", visibility = ItemVisibility.INVISIBLE, persist = false,
   	    	description = "Immediate processing of active image when a parameter is changed",
   			callback = "callbackProcessImmediately")
    private boolean booleanProcessImmediately;
    
	@Parameter(label = "Image #", description = "Image slice number", style = NumberWidget.SPINNER_STYLE, min = "1", max = "99999999", stepSize = "1",
			   persist = false, // restore  previous value  default  =  true
			   initializer = "initialNumImageSlice",
			   callback = "callbackNumImageSlice")
	private int spinnerInteger_NumImageSlice;
	
	@Parameter(label = "   Process single image #    ", callback = "callbackProcessSingleImage")
	private Button buttonProcessSingelImage;
   
//	Deactivated, because it does not work in Fiji (although it works in ImageJ2 -Eclipse)	
//  @Parameter(label = "Process single active image ", callback = "callbackProcessActiveImage")
//	private Button buttonProcessActiveImage;
	
	@Parameter(label = "Process all available images", callback = "callbackProcessAllImages")
	private Button buttonProcessAllImages;

	// ---------------------------------------------------------------------

	// The following initializer functions set initial value
	protected void initialPluginLaunch() {
		checkItemIOIn();
	}
	protected void initialFilterType() {
		choiceRadioButt_FilterType = "Gaussian blur";
	} 
		
	protected void initialSigma() {
		spinnerFloat_Sigma = 3f;
	}
	protected void initialKernelSize() {
		spinnerInteger_KernelSize = 3;
	}
	protected void initialRadius() {
		spinnerInteger_Radius = 3;
	}
	protected void initialOverwriteDisplays() {
		booleanOverwriteDisplays = true;
	}
	protected void initialNumImageSlice() {
    	spinnerInteger_NumImageSlice = 1;
	}
	
	// ------------------------------------------------------------------------------
	/** Executed whenever the {@link #choiceRadioButt_FilterType} parameter changes. */
	protected void callbackFilterType() {
		logService.info(this.getClass().getName() + " Filter type set to " + choiceRadioButt_FilterType);
	}
	
	/** Executed whenever the {@link #spinnerInteger_Sigma} parameter changes. */
	protected void callbackSigma() {
		//round to ?? decimal after the comma
	 	//spinnerFloat_Sigma = Math.round(spinnerFloat_Aigma * 10f)/10f;
		if (spinnerFloat_Sigma < 0.5f) spinnerFloat_Sigma = 0.5f;
	 	spinnerFloat_Sigma = Precision.round(spinnerFloat_Sigma, 2);
		logService.info(this.getClass().getName() + " Sigma set to " + spinnerFloat_Sigma);
	}
	
	/** Executed whenever the {@link #spinnerInteger_KernelSize} parameter changes. */
	protected void callbackKernelSize() {
//		 if ( spinnerInteger_KernelSize % 2 == 0 ) {
//			 spinnerInteger_KernelSize = spinnerInteger_KernelSize + 1;  //even numbers are not allowed
			 logService.info(this.getClass().getName() + " Even numbers are not allowed!");
//		 }
		logService.info(this.getClass().getName() + " Kernel size set to " + spinnerInteger_KernelSize);
	}
	/** Executed whenever the {@link #spinnerInteger_Radius} parameter changes. */
	protected void callbackRadius() {
		logService.info(this.getClass().getName() + " Radius set to " + spinnerInteger_Radius);
	}
	
	/** Executed whenever the {@link #booleanProcessImmediately} parameter changes. */
	protected void callbackProcessImmediately() {
		logService.info(this.getClass().getName() + " Process immediately set to " + booleanProcessImmediately);
	}
	
	/** Executed whenever the {@link #spinnerInteger_NumImageSlice} parameter changes. */
	protected void callbackNumImageSlice() {
		if (spinnerInteger_NumImageSlice > numSlices){
			logService.info(this.getClass().getName() + " No more images available");
			spinnerInteger_NumImageSlice = (int)numSlices;
		}
		logService.info(this.getClass().getName() + " Image slice number set to " + spinnerInteger_NumImageSlice);
	}
	
	/**
	 * Executed whenever the {@link #buttonProcessSingleImage} button is pressed.
	 * It is not executed in the same exact manner such as run()
	 * So a thread for displaying properly the Progressbar window is needed
	 * Execution of the code is then not on the Event Dispatch Thread EDT, where all GUI windows are executed
	 * The @Parameter ItemIO.OUTPUT is not automatically shown 
	 */
	protected void callbackProcessSingleImage() {
		//prepare  executer service
		exec = Executors.newSingleThreadExecutor();
	   	exec.execute(new Runnable() {
	        public void run() {
	    	    startWorkflowForSingleImage();
	    		uiService.show(imagePreviewName, datasetPreview);   //Show result because it did not go over the run() method
	        }
	    });
	   	exec.shutdown(); //No new tasks
	}
	
	/** Executed whenever the {@link #buttonProcessActiveImage} button is pressed.*/
	protected void callbackProcessActiveImage() {
	
	}
	
	/**
	 * Executed whenever the {@link #buttonProcessAllImages} button is pressed.
	 * It is not executed in the same exact manner such as run()
	 * So a thread for displaying properly the Progressbar window is needed
	 * Execution of the code is then not on the Event Dispatch Thread EDT, where all GUI windows are executed
	 * The @Parameter ItemIO.OUTPUT is not automatically shown 
	 */
	protected void callbackProcessAllImages() {
		//prepare  executer service
		exec = Executors.newSingleThreadExecutor();
	   	exec.execute(new Runnable() {
	        public void run() {
	        	startWorkflowForAllImages();
	    	   	uiService.show(imageOutName, datasetOut);
	        }
	    });
	   	exec.shutdown(); //No new tasks
	}

	/**
	 * Executed automatically every time a widget value changes.
	 * It is not executed in the same exact manner such as run()
	 * So a thread for displaying properly the Progressbar window is needed
	 * Execution of the code is then not on the Event Dispatch Thread EDT, where all GUI windows are executed
	 * The @Parameter ItemIO.OUTPUT is not automatically shown 
	 */
	@Override //Interface Previewable
	public void preview() { 
	 	logService.info(this.getClass().getName() + " Preview initiated");
	 	if (booleanProcessImmediately) {
			exec = Executors.newSingleThreadExecutor();
		   	exec.execute(new Runnable() {
		        public void run() {
		    	    startWorkflowForSingleImage();
		    		uiService.show(imagePreviewName, datasetPreview);   //Show result because it did not go over the run() method
		        }
		    });
		   	exec.shutdown(); //No new tasks
	 	}	
	}

	/**
	 * This is necessary if the "preview" method manipulates data
	 * the "cancel" method will then need to revert any changes back to the original state.
	 */
	@Override //Interface Previewable
	public void cancel() {
		logService.info(this.getClass().getName() + " ComsystanJ plugin canceled");
	}	 
			 
	/** 
	 * The run method executes the command via a SciJava thread
	 * by pressing the OK button in the UI or
	 * by CommandService.run(Command.class, false, parameters) in a script  
	 *  
	 * The @Parameter ItemIO.INPUT  is automatically harvested 
	 * The @Parameter ItemIO.OUTPUT is automatically shown 
	 * 
	 * A thread is not necessary in this method and should be avoided
	 * Nevertheless a thread may be used to get a reference for canceling
	 * But then the @Parameter ItemIO.OUTPUT would not be automatically shown and
	 * CommandService.run(Command.class, false, parameters) in a script  would not properly work
	 *
	 * An InteractiveCommand (Non blocking dialog) has no automatic OK button and would call this method twice during start up
	 */
	@Override //Interface CommandService
	public void run() {
		logService.info(this.getClass().getName() + " Run");
		if (ij != null) { //might be null in Fiji
			if (ij.ui().isHeadless()) {
			}
		}
		if (this.getClass().getName().contains("Command")) { //Processing only if class is a Csaj***Command.class
			startWorkflowForAllImages();
		}
	}
	
	public void checkItemIOIn() {
	
		//datasetIn = imageDisplayService.getActiveDataset();
		if (datasetIn == null) {
			logService.error(this.getClass().getName() + " ERROR: Input image = null");
			cancel("ComsystanJ 2D plugin cannot be started - missing input image.");
			return;
		}

		if ( (datasetIn.firstElement() instanceof UnsignedByteType) ||
			 (datasetIn.firstElement() instanceof FloatType) ){
			//That is OK, proceed
		} else {
			logService.warn(this.getClass().getName() + " WARNING: Data type is not Byte or Float");
			cancel("ComsystanJ 2D plugin cannot be started - data type is not Byte or Float.");
			return;
		}
		
		// get some info
		width = datasetIn.dimension(0);
		height = datasetIn.dimension(1);
		//depth = dataset.getDepth(); //does not work if third axis ist not specifyed as z-Axis
		numDimensions             = datasetIn.numDimensions();
		boolean isRGBMerged       = datasetIn.isRGBMerged();
	
		compositeChannelCount = datasetIn.getCompositeChannelCount();
		if ((numDimensions == 2) && (compositeChannelCount == 1)) { //single Grey image
			numSlices = 1;
			imageType = "Grey";
		} else if ((numDimensions == 3) && (compositeChannelCount == 1)) { // Grey stack	
			numSlices = datasetIn.dimension(2); //x,y,z
			imageType = "Grey";
		} else if ((numDimensions == 3) && (compositeChannelCount == 3)) { //Single RGB image	
			numSlices = 1;
			imageType = "RGB";
		} else if ((numDimensions == 4) && (compositeChannelCount == 3)) { // RGB stack	x,y,composite,z
			numSlices = datasetIn.dimension(3); //x,y,composite,z
			imageType = "RGB";
		}
		
		// get name of dataset
		datasetName = datasetIn.getName();
			
		try {
			Map<String, Object> prop = datasetIn.getProperties();
			DefaultImageMetadata metaData = (DefaultImageMetadata) prop.get("scifio.metadata.image");
			MetaTable metaTable = metaData.getTable();
			sliceLabels = (String[]) metaTable.get("SliceLabels");
			//eliminate additional image info delimited with \n (since pom-scijava 29.2.1)
			for (int i = 0; i < sliceLabels.length; i++) {
				String label = sliceLabels[i];
				int index = label.indexOf("\n");
				//if character has been found, otherwise index = -1
				if (index > 0) sliceLabels[i] = label.substring(0, index);		
			}
		} catch (NullPointerException npe) {
			// TODO Auto-generated catch block
			//npe.printStackTrace();
			logService.info(this.getClass().getName() + " WARNING: It was not possible to read scifio metadata."); 
		}
		
		logService.info(this.getClass().getName() + " Name: "             + datasetName); 
		logService.info(this.getClass().getName() + " Image size: "       + width+"x"+height); 
		logService.info(this.getClass().getName() + " Image type: "       + imageType); 
		logService.info(this.getClass().getName() + " Number of images = "+ numSlices); 		
	}
	
	/**
	 * This method generates the preview dataset
	 * @param rai
	 */
	private void generateDatasetPreview(RandomAccessibleInterval<T> rai) {
		if (imageType.equals("Grey")) {
			//RGB image
			boolean signed   = false;
			boolean floating = false;
			boolean virtual  = false;
			
			int bitsPerPixel = 8;
			//long[] dims = new long[]{width, height};
			long[] dims = new long[]{rai.dimension(0), rai.dimension(1)};
			AxisType[] axes = new AxisType[]{Axes.X, Axes.Y};
			datasetPreview = datasetService.create(dims, imagePreviewName, axes, bitsPerPixel, signed, floating, virtual);	
			
			Cursor<RealType<?>> cursor = datasetPreview.localizingCursor();
			RandomAccess<T> ra = rai.randomAccess();
			long[] pos2D = new long[2];
			while (cursor.hasNext()) {
				cursor.fwd();
				cursor.localize(pos2D);
				ra.setPosition(pos2D);
				cursor.get().setReal(ra.get().getRealFloat());
			}
		} else if (imageType.equals("RGB")) {
			//RGB image
			boolean signed   = false;
			boolean floating = false;
			boolean virtual  = false;
			
			int bitsPerPixel = 8;
			//long[] dims = new long[]{width, height, 3};
			long[] dims = new long[]{rai.dimension(0), rai.dimension(1), 3};
			AxisType[] axes = new AxisType[]{Axes.X, Axes.Y, Axes.CHANNEL};
			datasetPreview = datasetService.create(dims, imagePreviewName, axes, bitsPerPixel, signed, floating, virtual);	
			datasetPreview.setCompositeChannelCount(3);
			datasetPreview.setRGBMerged(true);
//			datasetPreview.setChannelMinimum(0, 0);
//			datasetPreview.setChannelMinimum(1, 0);
//			datasetPreview.setChannelMinimum(2, 0);
//			datasetPreview.setChannelMaximum(0, 255);
//			datasetPreview.setChannelMaximum(1, 255);
//			datasetPreview.setChannelMaximum(2, 255);
//			datasetPreview.initializeColorTables(3);
//			datasetPreview.setColorTable(ColorTables.RED,   0);
//			datasetPreview.setColorTable(ColorTables.GREEN, 1);
//			datasetPreview.setColorTable(ColorTables.BLUE,  2);
			
			Cursor<RealType<?>> cursor = datasetPreview.localizingCursor();
			RandomAccess<T> ra = rai.randomAccess();
			long[] pos3D = new long[3];
			while (cursor.hasNext()) {
				cursor.fwd();
				cursor.localize(pos3D);
				ra.setPosition(pos3D);
				cursor.get().setReal(ra.get().getRealFloat());
			}
		}	
	}
	
	/**
	* This method starts the workflow for a single image of the active display
	*/
	protected void startWorkflowForSingleImage() {
		
		dlgProgress = new CsajDialog_WaitingWithProgressBar("Filtering, please wait... Open console window for further info.",
							logService, false, exec); //isCanceable = false, because no following method listens to exec.shutdown 
		
		dlgProgress.updatePercent("");
		dlgProgress.setBarIndeterminate(true);
		dlgProgress.setVisible(true);
	
		deleteExistingDisplays();
    	int sliceIndex = spinnerInteger_NumImageSlice - 1;
		logService.info(this.getClass().getName() + " Processing single image " + (sliceIndex + 1));
		RandomAccessibleInterval<T> rai = processSingleInputImage(sliceIndex);
		generateDatasetPreview(rai);
		dlgProgress.addMessage("Processing finished!");
		dlgProgress.setVisible(false);
		dlgProgress.dispose();
		Toolkit.getDefaultToolkit().beep();   
	}

	/**
	* This method starts the workflow for all images of the active display
	*/
	protected void startWorkflowForAllImages() {

		dlgProgress = new CsajDialog_WaitingWithProgressBar("Filtering, please wait... Open console window for further info.",
							logService, false, exec); //isCanceable = true, because processAllInputImages(dlgProgress) listens to exec.shutdown 
		dlgProgress.setVisible(true);
	
		deleteExistingDisplays();
    	logService.info(this.getClass().getName() + " Processing all available images");
		processAllInputImages();
		dlgProgress.addMessage("Processing finished!");
		dlgProgress.setVisible(false);
		dlgProgress.dispose();
		Toolkit.getDefaultToolkit().beep();
	}


	/**
	 * This methods gets the index of the active image in a stack
	 * @return int index
	 */
	private int getActiveImageIndex() {
		int activeSliceIndex = 0;
		try {
			//This works in eclipse but not as jar in the plugin folder of fiji 
			//SCIFIO activated: throws a NullPointerException
			//SCIFIO deactivated: gives always back index = 0! 
			Position pos = imageDisplayService.getActivePosition();
			activeSliceIndex = (int) pos.getIndex();
			
			//This gives always back 0, SCIFIO setting does not matter
			//int activeSliceNumber = (int) imageDisplayService.getActiveImageDisplay().getActiveView().getPlanePosition().getIndex();
			//???
			//int activeSliceNumber = (int) defaultImageDisplayService.getActivePosition().getIndex(); 
			//int activeSliceNumber2 = (int) defaultImageDisplayService.getActiveImageDisplay().getActiveView().getPlanePosition().getIndex();
		} catch (NullPointerException npe) {
			// TODO Auto-generated catch block
			//npe.printStackTrace();
			logService.info(this.getClass().getName() + " WARNING: It was not possible to get active slice index. Index set to first image.");
			activeSliceIndex = 0;
		} 
		logService.info(this.getClass().getName() + " Active slice index = " + activeSliceIndex);
		//logService.info(this.getClass().getName() + " Active slice index alternative = " + activeSliceNumber2);
		return activeSliceIndex;
	}
	
	/** This method deletes already open displays*/
	private void deleteExistingDisplays() {
		
		boolean optDeleteExistingImgs   = false;
		boolean optDeleteExistingPlots  = false;
		boolean optDeleteExistingTables = false;
		if (booleanOverwriteDisplays) {
			optDeleteExistingImgs   = true;
			optDeleteExistingPlots  = true;
			optDeleteExistingTables = true;
		}
		
		if (optDeleteExistingImgs) {
			//List<Display<?>> list = defaultDisplayService.getDisplays();
			//for (int i = 0; i < list.size(); i++) {
			//	display = list.get(i);
			//	System.out.println("display name: " + display.getName());
			//	if (display.getName().contains("Name")) display.close(); //does not close correctly in Fiji, it is only not available any more
			//}			
			//List<ImageDisplay> listImgs = defaultImageDisplayService.getImageDisplays(); //Is also not closed in Fiji 
		
			Frame frame;
			Frame[] listFrames = JFrame.getFrames();
			for (int i = listFrames.length -1 ; i >= 0; i--) { //Reverse order, otherwise focus is not given free from the last image
				frame = listFrames[i];
				//System.out.println("frame name: " + frame.getTitle());
				if ((frame.getTitle().contains(imageOutName)) ) {
					frame.setVisible(false); //Successfully closes also in Fiji
					frame.dispose();
					
				} else if (frame.getTitle().contains(imagePreviewName)) {
					frame.setVisible(false); //Successfully closes also in Fiji
					frame.dispose();
					datasetPreview = null;
				}		
			} //for
		}
//		if (optDeleteExistingPlots) {
//			//This dose not work with DisplayService because the JFrame is not "registered" as an ImageJ display	
//			if (doubleLogPlotList != null) {
//				for (int l = 0; l < doubleLogPlotList.size(); l++) {
//					doubleLogPlotList.get(l).setVisible(false);
//					doubleLogPlotList.get(l).dispose();
//					//doubleLogPlotList.remove(l);  /
//				}
//				doubleLogPlotList.clear();		
//			}
//		}
//		if (optDeleteExistingTables) {
//			Display<?> display;
//			List<Display<?>> list = defaultDisplayService.getDisplays();
//			for (int i = 0; i < list.size(); i++) {
//				display = list.get(i);
//				//System.out.println("display name: " + display.getName());
//				if (display.getName().contains(tableOutName)) display.close();
//			}			
//		}
	}

	/** This method takes a single image and computes results. 
	 * @return 
	 *
	 */
	private RandomAccessibleInterval<T> processSingleInputImage(int s) {
	
		long startTime = System.currentTimeMillis();
		// convert to float values
		// Img<T> image = (Img<T>) dataset.getImgPlus();
		// imgFloat = opService.convert().float32((Img<T>)dataset.getImgPlus());

//		//Prepare output dataset
//		datasetOut = datasetIn.duplicateBlank();
//		//copy metadata
//		datasetOut.getProperties().putAll(datasetIn.getProperties());
//		//Map<String, Object> map = datasetOut.getProperties();
	
//		imgFloat = new ArrayImgFactory<>(new FloatType()).create(width, height); // always single 2D
//		Cursor<FloatType> cursor = imgFloat.localizingCursor();
//		long[] pos = new long[imgFloat.numDimensions()];
//		RandomAccess<RealType<?>> ra = datasetIn.randomAccess();
//		while (cursor.hasNext()) {
//			cursor.fwd();
//			cursor.localize(pos);
//			if (numSlices == 1) { // for only one 2D image;
//				ra.setPosition(pos[0], 0);
//				ra.setPosition(pos[1], 1);
//			} else { // for more than one image e.g. image stack
//				ra.setPosition(pos[0], 0);
//				ra.setPosition(pos[1], 1);
//				ra.setPosition(s, 2);
//			}
//			// ra.get().setReal(cursor.get().get());
//			cursor.get().setReal(ra.get().getRealFloat());
//		}
//
//		IntervalView<FloatType> iv = Views.interval(imgFloat, new long[] { 0, 0 }, new long[] { imgFloat.max(0), imgFloat.max(1) });
//		RandomAccessibleInterval< FloatType > rai = Views.interval(imgFloat, new long[] { 0, 0 }, new long[] { imgFloat.max(0), imgFloat.max(1) });

		
		//Prepare output dataset
		//datasetOut = datasetIn.duplicateBlank();
		datasetOut = datasetIn.duplicate();
		//copy metadata
		(datasetOut.getProperties()).putAll(datasetIn.getProperties());
		//Map<String, Object> map = datasetOut.getProperties();
		datasetOut.setName(imageOutName);
		
		if (imageType.equals("Grey")) {
			//do nothing
		} else if (imageType.equals("RGB")) {
			datasetOut.setCompositeChannelCount(3);
			datasetOut.setRGBMerged(true);		
		}
		
		RandomAccessibleInterval<T> rai = null;	
		if( (s==0) && (numSlices == 1)) { // for only one 2D image, Grey or RGB;
			rai =  (RandomAccessibleInterval<T>) datasetOut.getImgPlus(); //dim==2 or 3

		} else if ( (numSlices > 1)){ // for a stack of 2D images, Grey or RGB
			if (imageType.equals("Grey")) {
				rai = (RandomAccessibleInterval<T>) Views.hyperSlice(datasetOut, 2, s);  //dim==2  x,y,z	
			} else if (imageType.equals("RGB")) {
				rai = (RandomAccessibleInterval<T>) Views.hyperSlice(datasetOut, 3, s);  //dim==3  x,y,composite,z  
			}	
		}

		rai = process(rai); // rai is 2D
		//rai of DatasetOut already set to output values
		
		//uiService.show("Result", rai);
		
		//or
		
//		//write to output dataset
//		Cursor<?> cursor = Views.iterable(rai).localizingCursor();
//		long[] pos = new long[datasetIn.numDimensions()];
//		RandomAccess<RealType<?>> ra = datasetOut.randomAccess();
//		while (cursor.hasNext()) {
//			cursor.fwd();
//			cursor.localize(pos);
//			if (numSlices == 1) { // for only one 2D image;
//				ra.setPosition(pos[0], 0);
//				ra.setPosition(pos[1], 1);
//			} else { // for more than one image e.g. image stack
//				ra.setPosition(pos[0], 0);
//				ra.setPosition(pos[1], 1);
//				ra.setPosition(s, 2);
//			}
//			ra.get().setReal(((UnsignedByteType) cursor.get()).get()); //TYPE MISMATCH????  FLOAT MAY BE WRITTEN TO UNSIGNED 8BIT
//			//cursor.get().setReal(ra.get().getRealFloat());
//		}
	
		// or
		
		//Map<String, Object> map = datasetOut.getProperties();
		//datasetOut = datasetService.create(rai); //without properties such as sliceLabels
		//datasetOut.getProperties().putAll(datasetIn.getProperties());
		
		long duration = System.currentTimeMillis() - startTime;
		TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
		SimpleDateFormat sdf = new SimpleDateFormat();
		sdf.applyPattern("HHH:mm:ss:SSS");
		logService.info(this.getClass().getName() + " Elapsed time: "+ sdf.format(duration));
		
		return rai;
	}

	/** This method loops over all input images and computes results. 
	 *
	 */
	private void processAllInputImages() {
	
		long startTimeAll = System.currentTimeMillis();
		// convert to float values
		// Img<T> image = (Img<T>) dataset.getImgPlus();
		// Img<FloatType> imgFloat; // =
		// opService.convert().float32((Img<T>)dataset.getImgPlus());

		
		// stack of result images
		//List<RandomAccessibleInterval<FloatType>> outputImages = new ArrayList<RandomAccessibleInterval<FloatType>>();
		//or
		//Prepare output dataset
		//datasetOut = datasetIn.duplicateBlank();
		datasetOut = datasetIn.duplicate();
		//copy metadata
		(datasetOut.getProperties()).putAll(datasetIn.getProperties());
		//Map<String, Object> map = datasetOut.getProperties();
		datasetOut.setName(imageOutName);
		
		if (imageType.equals("Grey")) {
			//do nothing
		} else if (imageType.equals("RGB")) {
			datasetOut.setCompositeChannelCount(3);
			datasetOut.setRGBMerged(true);		
		}
	
		
		// loop over all slices of stack
		for (int s = 0; s < numSlices; s++) { // p...planes of an image stack
			//if (!exec.isShutdown()){
				int percent = (int)Math.round((  ((float)s)/((float)numSlices)   *100.f   ));
				dlgProgress.updatePercent(String.valueOf(percent+"%"));
				dlgProgress.updateBar(percent);
				//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
				statusService.showStatus((s+1), (int)numSlices, "Processing " + (s+1) + "/" + (int)numSlices);
				long startTime = System.currentTimeMillis();
				logService.info(this.getClass().getName() + " Processing image number " + (s+1) + "(" + numSlices + ")"); 
		
				// imgFloat = opService.convert().float32((Img<T>)dataset.gett);
	//			IntervalView<FloatType> iv = Views.interval(imgFloat, new long[] { 0, 0 }, new long[] { imgFloat.max(0), imgFloat.max(1) });
	//			RandomAccessibleInterval< FloatType > rai = Views.interval(imgFloat, new long[] { 0, 0 }, new long[] { imgFloat.max(0), imgFloat.max(1) });
	
				RandomAccessibleInterval<T> rai = null;	
				if( (s==0) && (numSlices == 1) ) { // for only one 2D image;
					rai =  (RandomAccessibleInterval<T>) datasetOut.getImgPlus(); //dim==2 or 3
	
				}
				if (numSlices > 1) { // for a stack
					if (imageType.equals("Grey")) {
						rai = (RandomAccessibleInterval<T>) Views.hyperSlice(datasetOut, 2, s); //dim==2  x,y,z  
					} else if (imageType.equals("RGB")) {
						rai = (RandomAccessibleInterval<T>) Views.hyperSlice(datasetOut, 3, s); //dim==3  x,y,composite,z  
					}	
				}
				
				rai = process(rai);  //rai is 2D(grey) or 3D(RGB)
				//rai of DatasetOut already set to output values
				
				//uiService.show("Result", iv);
				
				long duration = System.currentTimeMillis() - startTime;
				TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
				SimpleDateFormat sdf = new SimpleDateFormat();
				sdf.applyPattern("HHH:mm:ss:SSS");
				logService.info(this.getClass().getName() + " Elapsed time: "+ sdf.format(duration));
			//}
		} //s
		
		// stack channels back together
		//RandomAccessibleInterval result = ij.op().transform().stackView(outputImages);
		
		statusService.showProgress(0, 100);
		statusService.clearStatus();
//		try {
//			Thread.sleep(3000);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		Map<String, Object> map = datasetOut.getProperties();
		
		long duration = System.currentTimeMillis() - startTimeAll;
		TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
		SimpleDateFormat sdf = new SimpleDateFormat();
		sdf.applyPattern("HHH:mm:ss:SSS");
		logService.info(this.getClass().getName() + " Elapsed processing time for all image(s): "+ sdf.format(duration));
	}


	/**
	 * 
	 * {@code T extends Type<T>}.
	 * @param <C>
	 * @param <RandomAccessibleInterval>
	 * @return 
	 * @return 
	 */
	private <C extends ComplexType<C>, R extends RealType<R>> RandomAccessibleInterval<R> process(RandomAccessibleInterval<R> rai) { //one slice grey or RGB

		if (rai == null) {
			logService.info(this.getClass().getName() + " WARNING: rai==null, no image for processing!");
		}
			
		String filterType = choiceRadioButt_FilterType;//"Gaussian blur" "Mean" "Median" "Low pass - FFT"
		double sigma      = spinnerFloat_Sigma;
		int kernelSize    = spinnerInteger_KernelSize;
		int radius        = spinnerInteger_Radius;
		
		//imageType = "Grey"; // "Grey" "RGB"....
		//numSlices;
		int pixelValue;	
		double[] sigmas = new double[] {sigma, sigma};
	
		//"Gaussian Blur", "Mean" "Median" "Low pass - FFT"
		if (filterType.equals("Gaussian blur")) {
		
			if (imageType.equals("Grey")) { //rai should have 2 dimensions
				
				FastGauss.convolve(sigmas, Views.extendMirrorSingle(rai), rai);
								
			} else if (imageType.equals("RGB")) {
			
				int numDim = rai.numDimensions();
				RandomAccessibleInterval<R> raiSlice = null;	
				
				for (int b = 0; b < numDim; b++) {
				
					raiSlice = (RandomAccessibleInterval<R>) Views.hyperSlice(rai, 2, b);
					FastGauss.convolve(sigmas, Views.extendMirrorSingle(raiSlice), raiSlice);
									
				} //b
			} //RGB	
		} //Gaussian blur
	
		else if (filterType.equals("Mean")) {
			
			RandomAccess<R> ra;
			IterableInterval<R> intervalF;
			Img<DoubleType> kernelMean;
			RectangleShape shape = new RectangleShape(kernelSize, false);
			long[] pos = new long[2];
			float sampleF;
			
			if (imageType.equals("Grey")) { //rai should have 2 dimensions
					
				//convolve does also work NOTE: ******Do not forget to divide the sampleF values by (kernelSize*kernelSize)*******
//				//kernelMean = opService.create().img(new int[] {kernelSize, kernelSize}); may not work in older Fiji versions
//				kernelMean = new ArrayImgFactory<>(new DoubleType()).create(kernelSize, kernelSize); 				
//				for (DoubleType k : kernelMean) k.setReal(1.0);	
//				//intervalF = (IterableInterval<R>) opService.create().img(rai, new FloatType()); may not work in older Fiji versions
//				intervalF = (IterableInterval<R>) new ArrayImgFactory<>(new FloatType()).create(width, height);
//				intervalF = (IterableInterval<R>) opService.filter().convolve(rai, kernelMean, new OutOfBoundsBorderFactory());	
				
				//intervalF = (IterableInterval<R>) opService.create().img(rai, new FloatType());  may not work in older Fiji versions
				intervalF = (IterableInterval<R>) new ArrayImgFactory<>(new FloatType()).create(width, height);
				intervalF = (IterableInterval<R>) opService.filter().mean(intervalF, rai, shape, new OutOfBoundsBorderFactory());
				//Rewrite to UnsignedByte rai
				ra = rai.randomAccess();
				cursor = intervalF.localizingCursor();
				while (cursor.hasNext()) {
					cursor.fwd();
					cursor.localize(pos);
					ra.setPosition(pos);
					sampleF = ((FloatType) cursor.get()).getRealFloat(); //  /(kernelSize*kernelSize)  //For convolve
					ra.get().setReal((int)Math.round(sampleF));
				}		
					
			} else if (imageType.equals("RGB")) {   
				int numDim = rai.numDimensions();
				RandomAccessibleInterval<R> raiSlice = null;	
				
				for (int b = 0; b < numDim; b++) {
				
					raiSlice = Views.hyperSlice(rai, 2, b);
							
					//convolve does also work NOTE: ******Do not forget to divide the sampleF values by (kernelSize*kernelSize)*******
//					//kernelMean = opService.create().img(new int[] {kernelSize, kernelSize}); may not work in older Fiji versions
//					kernelMean = new ArrayImgFactory<>(new DoubleType()).create(kernelSize, kernelSize); 	
//					for (DoubleType k : kernelMean) k.setReal(1.0);	
//					intervalF = (IterableInterval<R>) opService.filter().convolve(raiSlice, kernelMean, new OutOfBoundsBorderFactory());
					
					//intervalF = (IterableInterval<R>) opService.create().img(rai, new FloatType());  may not work in older Fiji versions
					intervalF = (IterableInterval<R>) new ArrayImgFactory<>(new FloatType()).create(width, height);
					intervalF = (IterableInterval<R>) opService.filter().mean(intervalF, raiSlice, shape, new OutOfBoundsBorderFactory());			
					//Rewrite to UnsignedByte rai
					ra = (RandomAccess<R>) raiSlice.randomAccess();
					cursor = intervalF.localizingCursor();
					while (cursor.hasNext()) {
						cursor.fwd();
						cursor.localize(pos);
						ra.setPosition(pos);
						sampleF = ((FloatType) cursor.get()).getRealFloat(); //  /(kernelSize*kernelSize)  //For convolve
						ra.get().setReal((int)Math.round(sampleF));
					}								
				} //b
			} //RGB	
		} //Mean
		
		else if (filterType.equals("Median")) {
			
			RandomAccess<R> ra;
			IterableInterval<R> intervalF;
			RectangleShape shape = new RectangleShape(kernelSize, false);
			
			long[] pos = new long[2];
			float sampleF;
			
			if (imageType.equals("Grey")) { //rai should have 2 dimensions
							
				//intervalF = (IterableInterval<R>) opService.create().img(rai, new FloatType());  may not work in older Fiji versions
				intervalF = (IterableInterval<R>) new ArrayImgFactory<>(new FloatType()).create(width, height);
				intervalF = (IterableInterval<R>) opService.filter().median(intervalF, rai, shape, new OutOfBoundsBorderFactory());				
				//Rewrite to UnsignedByte rai
				ra = rai.randomAccess();
				cursor = intervalF.localizingCursor();
				while (cursor.hasNext()) {
					cursor.fwd();
					cursor.localize(pos);
					ra.setPosition(pos);
					sampleF = ((FloatType) cursor.get()).getRealFloat();
					ra.get().setReal((int)Math.round(sampleF));
				}		
				
			} else if (imageType.equals("RGB")) {   
				int numDim = rai.numDimensions();
				RandomAccessibleInterval<R> raiSlice = null;	
				
				for (int b = 0; b < numDim; b++) {
				
					raiSlice = Views.hyperSlice(rai, 2, b);
				
					//intervalF = (IterableInterval<R>) opService.create().img(raiSlice, new FloatType());  may not work in older Fiji versions
					intervalF = (IterableInterval<R>) new ArrayImgFactory<>(new FloatType()).create(width, height);		
					intervalF = (IterableInterval<R>) opService.filter().median(intervalF, raiSlice, shape, new OutOfBoundsBorderFactory());			
					//Rewrite to UnsignedByte rai
					ra = (RandomAccess<R>) raiSlice.randomAccess();
					cursor = intervalF.localizingCursor();
					while (cursor.hasNext()) {
						cursor.fwd();
						cursor.localize(pos);
						ra.setPosition(pos);
						sampleF = ((FloatType) cursor.get()).getRealFloat();
						ra.get().setReal((int)Math.round(sampleF));
					}		
				} //b
			} //RGB	
		} //Median
	
		else if (filterType.equals("Low pass - FFT")) {
			
			//The Radius defines which pixels are changed
			if (imageType.equals("Grey")) { //rai should have 2 dimensions

				getWindowedVolume(rai); //This methods computes the windowed rai raiWindowed 
				
//				Short algorithm:
//				ops filter fft seems to be a Hadamard transform rather than a true FFT
//				output size is automatically padded, so has rather strange dimensions.
//				output is vertically symmetric 
				//RandomAccessibleInterval<C> fft = opService.filter().fft(rai);
				
				//Using JTransform package
				//https://github.com/wendykierp/JTransforms
				//https://wendykierp.github.io/JTransforms/apidocs/
				//The sizes of both dimensions must be power of two.
				//Round to next largest power of two. The resulting image will be cropped according to GUI input			
				compute2DFFTMatrix(raiWindowed); //This computes JTransform Fourier transformed matrix matrixA
										
				// Filter it.*********************************************************************************
				lowPass((double)radius);
									
				// Reverse the FFT.******************************************************************************
				//opService.filter().ifft(rai, fft);
				FFT.complexInverse(matrixA, true); //true: values are back in the right range 
				
				cursor = Views.iterable(rai).localizingCursor();
				long[] pos = new long[2];
				float real = 0f;
				while (cursor.hasNext()) {
					cursor.fwd();
					cursor.localize(pos); 
					//JTransform needs rows and columns swapped!!!!!			
					real = matrixA[(int)pos[1]][(int)(2*pos[0])];
				    ((UnsignedByteType) cursor.get()).setReal((int)Math.round(real)); 
				}	
				
				//Normalize if necessary
//				//Find min max;
//				float real = 0;
//				float min = Float.MAX_VALUE;
//				float max = -Float.MAX_VALUE;	
//				float greyMax = 255f;
//				// Loop through all pixels.	
//					for (int k2 = 0; k2 < rows; k2++) {
//						for (int k3 = 0; k3 < columns; k3++) {
//							real = matrixA[k2][2*k3];
//							if (real > max) {
//								max = real;
//							}
//							if (real < min) {
//								min = real;
//							}
//						}
//					}
//				
//						
//				cursor = Views.iterable(rai).localizingCursor();
//				//cursor = datasetOut.cursor();	
//		
//		    	pos = new long[2];
//		    	float rf = ((float)greyMax/(max-min)); //rescale factor
//				while (cursor.hasNext()) {
//					cursor.fwd();
//					cursor.localize(pos);
//					real = matrixA[[(int)pos[1]][(int)(2*pos[0])];
//					real = rf * (real - min); //Rescale to 0  - greyMax
//					((UnsignedByteType) cursor.get()).setReal((int)(Math.round(real)));	
//				}		
							
				
				
			} else if (imageType.equals("RGB")) {
			
				int numBands = 3;
				RandomAccessibleInterval<T> raiSlice = null;	
				
				for (int b = 0; b < numBands; b++) {
				
					raiSlice = (RandomAccessibleInterval<T>) Views.hyperSlice(rai, 2, b);								
					getWindowedVolume(raiSlice); //This methods computes the windowed rai raiWindowed 
					compute2DFFTMatrix(raiWindowed); //This computes JTransform Fourier transformed matrix matrixA								
					// Filter it.*********************************************************************************
					lowPass((double)radius);							
					// Reverse the FFT.******************************************************************************
					//opService.filter().ifft(rai, fft);
					FFT.complexInverse(matrixA, true); //true: values are back in the right range 
					
					cursor = Views.iterable(raiSlice).localizingCursor();
					long[] pos = new long[2];
					float real = 0f;
					while (cursor.hasNext()) {
						cursor.fwd();
						cursor.localize(pos); 
						//JTransform needs rows and columns swapped!!!!!			
						real = matrixA[(int)pos[1]][(int)(2*pos[0])];
					    ((UnsignedByteType) cursor.get()).setReal((int)Math.round(real)); 
					}						
				} //b
			} //RGB	
			
			FFT = null;
			matrixA = null;
					
		} //"Low pass - FFT"
		
		else if (filterType.equals("High pass - FFT")) {
			
			//The Radius defines which pixels are changed
			if (imageType.equals("Grey")) { //rai should have 2 dimensions

				getWindowedVolume(rai); //This methods computes the windowed rai raiWindowed 
				
//				Short algorithm:
//				ops filter fft seems to be a Hadamard transform rather than a true FFT
//				output size is automatically padded, so has rather strange dimensions.
//				output is vertically symmetric 
				//RandomAccessibleInterval<C> fft = opService.filter().fft(rai);
				
				//Using JTransform package
				//https://github.com/wendykierp/JTransforms
				//https://wendykierp.github.io/JTransforms/apidocs/
				//The sizes of both dimensions must be power of two.
				//Round to next largest power of two. The resulting image will be cropped according to GUI input			
				compute2DFFTMatrix(raiWindowed); //This computes JTransform Fourier transformed matrix matrixA
										
				// Filter it.*********************************************************************************
				highPass((double)radius);
									
				// Reverse the FFT.******************************************************************************
				//opService.filter().ifft(rai, fft);
				FFT.complexInverse(matrixA, true); //true: values are back in the right range 
				
//				cursor = Views.iterable(rai).localizingCursor();
//				long[] pos = new long[3];
//				float real = 0f;
//				while (cursor.hasNext()) {
//					cursor.fwd();
//					cursor.localize(pos); 
//					//JTransform needs rows and columns swapped!!!!!			
//					real = matrixA[(int)pos[1]][(int)(2*pos[0])];
//				    ((UnsignedByteType) cursor.get()).setReal((int)Math.round(real)); 
//				}	
				
				//Normalize is necessary
				//Find min max;
				float real = 0;
				float min = Float.MAX_VALUE;
				float max = -Float.MAX_VALUE;	
				float greyMax = 255f;
				int rows = matrixA.length;
				int columns = matrixA[0].length/2;
				// Loop through all pixels.		
				for (int k2 = 0; k2 < rows; k2++) {
					for (int k3 = 0; k3 < columns; k3++) {
						real = matrixA[k2][2*k3];
						if (real > max) {
							max = real;
						}
						if (real < min) {
							min = real;
						}
					}
				}
							
				cursor = Views.iterable(rai).localizingCursor();
				//cursor = datasetOut.cursor();	
		
		    	long[] pos = new long[2];
		    	float rf = ((float)greyMax/(max-min)); //rescale factor
				while (cursor.hasNext()) {
					cursor.fwd();
					cursor.localize(pos);
					real = matrixA[(int)pos[1]][(int)(2*pos[0])];
					real = rf * (real - min); //Rescale to 0  - greyMax
					((UnsignedByteType) cursor.get()).setReal((int)(Math.round(real)));	
				}		
							
							
			} else if (imageType.equals("RGB")) {
			
				int numBands = 3;
				RandomAccessibleInterval<T> raiSlice = null;	
				
				for (int b = 0; b < numBands; b++) {
				
					raiSlice = (RandomAccessibleInterval<T>) Views.hyperSlice(rai, 2, b);			
								
					getWindowedVolume(raiSlice); //This methods computes the windowed rai raiWindowed 
			
					compute2DFFTMatrix(raiWindowed); //This computes JTransform Fourier transformed matrix matrixA
											
					// Filter it.*********************************************************************************
					highPass((double)radius);
										
					// Reverse the FFT.******************************************************************************
					//opService.filter().ifft(rai, fft);
					FFT.complexInverse(matrixA, true); //true: values are back in the right range 
					
					//Normalize is necessary
					//Find min max;
					float real = 0;
					float min = Float.MAX_VALUE;
					float max = -Float.MAX_VALUE;	
					float greyMax = 255f;
					int rows = matrixA.length;
					int columns = matrixA[0].length/2;
					// Loop through all pixels.
					for (int k2 = 0; k2 < rows; k2++) {
						for (int k3 = 0; k3 < columns; k3++) {
							real = matrixA[k2][2*k3];
							if (real > max) {
								max = real;
							}
							if (real < min) {
								min = real;
							}
						}
					}
								
					cursor = Views.iterable(raiSlice).localizingCursor();
					//cursor = datasetOut.cursor();	
			
			    	long[] pos = new long[2];
			    	float rf = ((float)greyMax/(max-min)); //rescale factor
					while (cursor.hasNext()) {
						cursor.fwd();
						cursor.localize(pos);
						real = matrixA[(int)pos[1]][(int)(2*pos[0])];
						real = rf * (real - min); //Rescale to 0  - greyMax
						((UnsignedByteType) cursor.get()).setReal((int)(Math.round(real)));	
					}						
				} //b
			} //RGB	
			
			FFT = null;
			matrixA = null;
					
		} //"High pass - FFT"

		return rai;		
	} 
	
	//*************************************************************************************************
		/**
		 * Performs an inplace low-pass filter on the matrixA
		 * This method is quite identical to the high-pass method, only one line is different
		 * @param radius The radius of the filter.
		 */
		public void lowPass(final double radius) {
			
			//JTransform needs rows and columns swapped!!!!!
			int rows    = matrixA.length;
			int columns = matrixA[0].length/2;
			
			//Define 4 origins for FFT
	    	//Define origin as 0,0,0. //frequency = 0;
	    	final long[] origin1 = {        0,      0};
	    	final long[] origin2 = {        0, rows-1}; //bottom right of image   		    	
	    	
	    	//Following origins are symmetric because sum of powers is zero with .realForward(matrixA); 
	    	final long[] origin5 = {columns-1,      0}; 
	    	final long[] origin6 = {columns-1, rows-1}; //bottom right of image 
			
			long[] posFFT = new long[2];
			float dist = 0;
			
			// Define half height and depth. Later used to find right origin
	    	final long fftHalfRows    = rows/2;
	    	final long fftHalfColumns = columns/2;
	    		    	
			// Loop through all pixels and check radious
				for (int k2 = 0; k2 < rows; k2++) {
					//for (int k3 = 0; k3 < columns/2; k3++) { //with 4 origins
					for (int k3 = 0; k3 < columns; k3++) { //with 8 origins
							posFFT[1] = k2;
							posFFT[0] = k3;
				
							// change origin depending on cursor position
							if      (posFFT[0] <  fftHalfColumns && posFFT[1] <  fftHalfRows) dist = (float)Util.distance(origin1, posFFT);
							else if (posFFT[0] <  fftHalfColumns && posFFT[1] >= fftHalfRows) dist = (float)Util.distance(origin2, posFFT);
							else if (posFFT[0] >= fftHalfColumns && posFFT[1] <  fftHalfRows) dist = (float)Util.distance(origin5, posFFT);
							else if (posFFT[0] >= fftHalfColumns && posFFT[1] >= fftHalfRows) dist = (float)Util.distance(origin6, posFFT);
											
							//set value of FFT to zero.
							if (dist > radius) { //LOW PASS 
								//System.out.println("dist > radius, dist:" + dist  + " radius:" + radius);
								matrixA[k2][2*k3]   = 0.0f;
								matrixA[k2][2*k3+1] = 0.0f;
							}
														
//							//for debug control purposes only
//							if      (posFFT[0] <  fftHalfColumns && posFFT[1] <  fftHalfRows) { sumDist1 += dist; sumPowers1 += powers[p-1]; }
//							else if (posFFT[0] <  fftHalfColumns && posFFT[1] >= fftHalfRows) { sumDist2 += dist; sumPowers2 += powers[p-1]; }
//							else if (posFFT[0] >= fftHalfColumns && posFFT[1] <  fftHalfRows) { sumDist5 += dist; sumPowers5 += powers[p-1]; }
//							else if (posFFT[0] >= fftHalfColumns && posFFT[1] >= fftHalfRows) { sumDist6 += dist; sumPowers6 += powers[p-1]; }								
					}
				}
			}
		
		/**
		 * Performs an inplace high-pass filter on the matrixA
		 * This method is quite identical to the low-pass method, only one line is different
		 * @param radius The radius of the filter.
		 */
		public void highPass(final double radius) {
			
			//JTransform needs rows and columns swapped!!!!!
			int rows    = matrixA.length;
			int columns = matrixA[0].length/2;
			
			//Define 4 origins for FFT
	    	//Define origin as 0,0,0. //frequency = 0;
	    	final long[] origin1 = {        0,      0};
	    	final long[] origin2 = {        0, rows-1}; //bottom right of image   		    	
	    	
	    	//Following origins are symmetric because sum of powers is zero with .realForward(matrixA); 
	    	final long[] origin5 = {columns-1,      0}; 
	    	final long[] origin6 = {columns-1, rows-1}; //bottom right of image 
			
			long[] posFFT = new long[2];
			float dist = 0;
			
			// Define half height and depth. Later used to find right origin
	    	final long fftHalfRows    = rows/2;
	    	final long fftHalfColumns = columns/2;
	    		    	
			// Loop through all pixels and check radious
				for (int k2 = 0; k2 < rows; k2++) {
					//for (int k3 = 0; k3 < columns/2; k3++) { //with 4 origins
					for (int k3 = 0; k3 < columns; k3++) { //with 8 origins
							posFFT[1] = k2;
							posFFT[0] = k3;
				
							// change origin depending on cursor position
							if      (posFFT[0] <  fftHalfColumns && posFFT[1] <  fftHalfRows) dist = (float)Util.distance(origin1, posFFT);
							else if (posFFT[0] <  fftHalfColumns && posFFT[1] >= fftHalfRows) dist = (float)Util.distance(origin2, posFFT);
							else if (posFFT[0] >= fftHalfColumns && posFFT[1] <  fftHalfRows) dist = (float)Util.distance(origin5, posFFT);
							else if (posFFT[0] >= fftHalfColumns && posFFT[1] >= fftHalfRows) dist = (float)Util.distance(origin6, posFFT);
							
							//set value of FFT to zero.
							if (dist < radius) { //HIGH PASS
	 							//System.out.println("dist > radius, dist:" + dist  + " radius:" + radius);
								matrixA[k2][2*k3]   = 0.0f;
								matrixA[k2][2*k3+1] = 0.0f;
							}
														
//							//for debug control purposes only
//							if      (posFFT[0] <  fftHalfColumns && posFFT[1] <  fftHalfRows) { sumDist1 += dist; sumPowers1 += powers[p-1]; }
//							else if (posFFT[0] <  fftHalfColumns && posFFT[1] >= fftHalfRows) { sumDist2 += dist; sumPowers2 += powers[p-1]; }
//							else if (posFFT[0] >= fftHalfColumns && posFFT[1] <  fftHalfRows) { sumDist5 += dist; sumPowers5 += powers[p-1]; }
//							else if (posFFT[0] >= fftHalfColumns && posFFT[1] >= fftHalfRows) { sumDist6 += dist; sumPowers6 += powers[p-1]; }								
					}
				}
		}
		
		/*
		 * This methods computes the JTransform Fourier transformed  matrix matrixA
		 * @param  rai
		 * @return FloatFFT_2D FFT
		 */
		private FloatFFT_2D compute2DFFTMatrix (RandomAccessibleInterval<?> rai) {
			int widthDFT  = width  == 1 ? 1 : Integer.highestOneBit((int)width  - 1) * 2;
			int heightDFT = height == 1 ? 1 : Integer.highestOneBit((int)height - 1) * 2;
			
			//All DFT axes must have the same size, otherwise lowest frequencies are not the same for anisotropic sizes
			widthDFT  = (int)Math.max(widthDFT, heightDFT); 
			heightDFT = widthDFT;
				
			//JTransform needs rows and columns swapped!!!!!
			int rows    = heightDFT;
			int columns = widthDFT;
			
			//JTransform needs rows and columns swapped!!!!!
			matrixA = new float[rows][2*columns]; //Every frequency entry needs a pair of columns: for real and imaginary part
			Cursor<?> cursor = Views.iterable(raiWindowed).localizingCursor();
			long[] pos = new long[2];
			while (cursor.hasNext()) {
				cursor.fwd();
				cursor.localize(pos); 
				//JTransform needs rows and columns swapped!!!!!
				matrixA[(int)pos[1]][(int)pos[0]] = ((FloatType) cursor.get()).get();
			}		
			// JTransform needs rows and columns swapped!!!!!
			FFT = new FloatFFT_2D(rows, columns); //Here always the simple DFT width
			//FFT.realForward(matrixA);   
			FFT.realForwardFull(matrixA); //Computes 3D forward DFT of real data leaving the result in a . This method computes full real forward transform, i.e. you will get the same result as from complexForward called with all imaginary part equal 0. Because the result is stored in a, the input array must be of size slices by rows by 2*columns, with only the first slices by rows by columns elements filled with real data. To get back the original data, use complexInverse on the output of this method.
			return FFT;
		}
		
				
		/**
		 * This methods computes the windowed rai raiWindowed
		 * @param  rai
		 */
		private void getWindowedVolume (RandomAccessibleInterval<?> rai) {
				
			String windowingType = "Rectangular";
			
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
			
		}
		

		/**
		 * This method does Rectangular windowing
		 * See also www.labbookpages.co.uk/audio/firWindowing.html#windows
		 * @param  rai
		 * @return windowed rai
		 */
		private RandomAccessibleInterval<FloatType> windowingRectangular (RandomAccessibleInterval<?> rai) {
			int width  = (int) rai.dimension(0);
			int height = (int) rai.dimension(1);	
			raiWindowed = new ArrayImgFactory<>(new FloatType()).create(width, height); //always single 2D
			
			double weight = 1.0;
		
			Cursor<FloatType> cursorF = Views.iterable(raiWindowed).localizingCursor();
			long[] pos = new long[raiWindowed.numDimensions()];
			RandomAccess<RealType<?>> ra = (RandomAccess<RealType<?>>) rai.randomAccess();
			while (cursorF.hasNext()){
				cursorF.fwd();
				cursorF.localize(pos);
				ra.setPosition(pos);
				cursorF.get().setReal(ra.get().getRealFloat()*weight); //simply a copy
			} 
		    return raiWindowed; 
		}
		
		/**
		 * This method does Bartlett windowing
		 * See Burge Burge, Digital Image Processing, Springer
		 * @param  rai
		 * @return windowed rai
		 */
		private RandomAccessibleInterval<FloatType> windowingBartlett (RandomAccessibleInterval<?> rai) {
			
			int width  = (int) rai.dimension(0);
			int height = (int) rai.dimension(1);	
			raiWindowed = new ArrayImgFactory<>(new FloatType()).create(width, height); //always single 2D
			
			double r_u;
			double r_v;
			double r_uv;
			double weight;
			
			//Create a full weight window
//			double[][] window = new double[width][height];
//			for (int u = 0; u < width; u++) {
//				for (int v = 0; v < height; v++) {
//					r_u = 2.0*u/width-1.0;
//					r_v = 2.0*v/height-1.0;
//					r_uv = Math.sqrt(r_u*r_u + r_v*r_v);
//					if ((r_uv >= 0) && (r_uv <=1)) window[u][v] = 1 - r_uv;
//					else window[u][v] = 0.0;
//				}
//			}
			
			Cursor<FloatType> cursorF = Views.iterable(raiWindowed).localizingCursor();
			long[] pos = new long[raiWindowed.numDimensions()];
			RandomAccess<RealType<?>> ra = (RandomAccess<RealType<?>>) rai.randomAccess();
			while (cursorF.hasNext()){
				cursorF.fwd();
				cursorF.localize(pos);
				ra.setPosition(pos);
				r_u = 2.0*(pos[0]+0.5)/width -1.0;   //+0.5 so that the maximum is really centered
				r_v = 2.0*(pos[1]+0.5)/height-1.0;
				r_uv = Math.sqrt(r_u*r_u + r_v*r_v);
				if ((r_uv >= 0) && (r_uv <=1)) weight = 1 - r_uv;
				else weight = 0.0;	
				//if(pos[1] == 1) System.out.println("Bartlett windowing weight " + pos[0] +" "+pos[1]+"  "+ weight);
				cursorF.get().setReal(ra.get().getRealFloat()*weight);
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
		private RandomAccessibleInterval<FloatType> windowingHamming (RandomAccessibleInterval<?> rai) {
		
			int width  = (int) rai.dimension(0);
			int height = (int) rai.dimension(1);	
			raiWindowed = new ArrayImgFactory<>(new FloatType()).create(width, height); //always single 2D
			
			double r_u;
			double r_v;
			double r_uv;
			double weight;
			
			Cursor<FloatType> cursorF = Views.iterable(raiWindowed).localizingCursor();
			long[] pos = new long[raiWindowed.numDimensions()];
			RandomAccess<RealType<?>> ra = (RandomAccess<RealType<?>>) rai.randomAccess();
			while (cursorF.hasNext()){
				cursorF.fwd();
				cursorF.localize(pos);
				ra.setPosition(pos);
				r_u = 2.0*(pos[0]+0.5)/width -1.0;   //+0.5 so that the maximum is really centered
				r_v = 2.0*(pos[1]+0.5)/height-1.0;
				r_uv = Math.sqrt(r_u*r_u + r_v*r_v);
				if ((r_uv >= 0) && (r_uv <=1)) weight = 0.54 + 0.46*Math.cos(Math.PI*(r_uv)); //== 0.54 - 0.46*Math.cos(Math.PI*(1.0-r_uv));
				else weight = 0.0;	
				//if(pos[1] == 1) System.out.println("Hamming windowing weight " + pos[0] +" "+pos[1]+"  "  + weight);
				cursorF.get().setReal(ra.get().getRealFloat()*weight);
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
		private RandomAccessibleInterval<FloatType> windowingHanning (RandomAccessibleInterval<?> rai) {
			
			int width  = (int) rai.dimension(0);
			int height = (int) rai.dimension(1);	
			raiWindowed = new ArrayImgFactory<>(new FloatType()).create(width, height); //always single 2D
			
			double r_u;
			double r_v;
			double r_uv;
			double weight = 0;
			
			Cursor<FloatType> cursorF = Views.iterable(raiWindowed).localizingCursor();
			long[] pos = new long[raiWindowed.numDimensions()];
			RandomAccess<RealType<?>> ra = (RandomAccess<RealType<?>>) rai.randomAccess();
			while (cursorF.hasNext()){
				cursorF.fwd();
				cursorF.localize(pos);
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
				cursorF.get().setReal(ra.get().getRealFloat()*weight);
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
		private RandomAccessibleInterval<FloatType> windowingBlackman (RandomAccessibleInterval<?> rai) {
			int width  = (int) rai.dimension(0);
			int height = (int) rai.dimension(1);	
			raiWindowed = new ArrayImgFactory<>(new FloatType()).create(width, height); //always single 2D
			
			double r_u;
			double r_v;
			double r_uv;
			double weight;
			
			Cursor<FloatType> cursorF = Views.iterable(raiWindowed).localizingCursor();
			long[] pos = new long[raiWindowed.numDimensions()];
			RandomAccess<RealType<?>> ra = (RandomAccess<RealType<?>>) rai.randomAccess();
			while (cursorF.hasNext()){
				cursorF.fwd();
				cursorF.localize(pos);
				ra.setPosition(pos);
				r_u = 2.0*(pos[0]+0.5)/width -1.0;   //+0.5 so that the maximum is really centered
				r_v = 2.0*(pos[1]+0.5)/height-1.0;
				r_uv = Math.sqrt(r_u*r_u + r_v*r_v);
				//if ((r_uv >= 0) && (r_uv <=1)) weight = 0.42 - 0.5*Math.cos(Math.PI*(1.0-r_uv)) + 0.08*Math.cos(2.0*Math.PI*(1.0-r_uv));
				if ((r_uv >= 0) && (r_uv <=1)) weight = 0.42 - 0.5*Math.cos(Math.PI*(1.0-r_uv)) + 0.08*Math.cos(2.0*Math.PI*(1.0-r_uv));
				else weight = 0.0;	
				//if(pos[1] == 1) System.out.println("Blackman windowing weight " + pos[0] +" "+pos[1]+"  "  + weight);
				cursorF.get().setReal(ra.get().getRealFloat()*weight);
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
		private RandomAccessibleInterval<FloatType> windowingGaussian (RandomAccessibleInterval<?> rai) {
			int width  = (int) rai.dimension(0);
			int height = (int) rai.dimension(1);	
			raiWindowed = new ArrayImgFactory<>(new FloatType()).create(width, height); //always single 2D
			
			double r_u;
			double r_v;
			double r_uv;
			double weight = 0;
			double sigma  = 0.3;
			double sigma2 = sigma*sigma;
			
			Cursor<FloatType> cursorF = Views.iterable(raiWindowed).localizingCursor();
			long[] pos = new long[raiWindowed.numDimensions()];
			RandomAccess<RealType<?>> ra = (RandomAccess<RealType<?>>) rai.randomAccess();
			while (cursorF.hasNext()){
				cursorF.fwd();
				cursorF.localize(pos);
				ra.setPosition(pos);
				r_u = 2.0*(pos[0]+0.5)/width -1.0;   //+0.5 so that the maximum is really centered
				r_v = 2.0*(pos[1]+0.5)/height-1.0;
				r_uv = Math.sqrt(r_u*r_u + r_v*r_v);
				weight = Math.exp(-(r_uv*r_uv)/(2.0*sigma2));
				//if(pos[1] == 1) System.out.println("Gaussian windowing weight " + pos[0] +" "+pos[1]+"  "  + weight);
				cursorF.get().setReal(ra.get().getRealFloat()*weight);
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
		private RandomAccessibleInterval<FloatType> windowingParzen (RandomAccessibleInterval<?> rai) {
		
			int width  = (int) rai.dimension(0);
			int height = (int) rai.dimension(1);	
			raiWindowed = new ArrayImgFactory<>(new FloatType()).create(width, height); //always single 2D
			
			double r_u;
			double r_v;
			double r_uv;
			double weight;
			
			Cursor<FloatType> cursorF = Views.iterable(raiWindowed).localizingCursor();
			long[] pos = new long[raiWindowed.numDimensions()];
			RandomAccess<RealType<?>> ra = (RandomAccess<RealType<?>>) rai.randomAccess();
			while (cursorF.hasNext()){
				cursorF.fwd();
				cursorF.localize(pos);
				ra.setPosition(pos);
				r_u = 2.0*(pos[0]+0.5)/width -1.0;   //+0.5 so that the maximum is really centered
				r_v = 2.0*(pos[1]+0.5)/height-1.0;
				r_uv = Math.sqrt(r_u*r_u + r_v*r_v);
				//if      ((r_uv >= 0) && (r_uv <0.5)) weight = 1.0 - 6.0*Math.pow(r_uv, 2) + 6.0*Math.pow(r_uv, 3); //Burge Burge gives double peaks, seems to be wrong
				if      ((r_uv >= 0) && (r_uv <0.5)) weight = 1.0 - 6.0*Math.pow(r_uv, 2)*(1-r_uv);
				else if ((r_uv >= 0.5) && (r_uv <1)) weight = 2.0*Math.pow(1-r_uv, 3);
				else    weight = 0.0;	
				//if(pos[1] == 1) System.out.println("Parzen windowing weight " + pos[0] +" "+pos[1]+"  "  + weight);
				cursorF.get().setReal(ra.get().getRealFloat()*weight);
			} 
		    return raiWindowed; 
		}
	
	/** The main method enables standalone testing of the command. */
	public static void main(final String... args) throws Exception {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch(Throwable t) {
		
		}
		// create the ImageJ application context with all available services
		final ImageJ ij = new ImageJ();

		// display the user interface
		ij.ui().showUI();

		// open and display an image
		final File imageFile = ij.ui().chooseFile(null, FileWidget.OPEN_STYLE);
		final Dataset image = ij.scifio().datasetIO().open(imageFile.getAbsolutePath());
		ij.ui().show(image);
		// execute the filter, waiting for the operation to finish.
		// ij.command().run(MethodHandles.lookup().lookupClass().getName(), true).get().getOutput("image");
		ij.command().run(MethodHandles.lookup().lookupClass().getName(), true);
	}
}
