/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj2DSegRGBRelCmd.java
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
package at.csa.csaj.plugin2d.preproc;

import java.awt.Frame;
import java.awt.Toolkit;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.text.SimpleDateFormat;
import java.util.HashMap;
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
import net.imagej.roi.ROIService;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.roi.RealMask;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.Views;

import org.scijava.ItemIO;
import org.scijava.ItemVisibility;
import org.scijava.app.StatusService;
import org.scijava.command.ContextCommand;
import org.scijava.command.Previewable;
import org.scijava.display.DefaultDisplayService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;
import org.scijava.ui.UIService;
import org.scijava.widget.Button;
import org.scijava.widget.ChoiceWidget;
import org.scijava.widget.FileWidget;
import org.scijava.widget.NumberWidget;

import at.csa.csaj.commons.CsajCheck_ItemIn;
import at.csa.csaj.commons.CsajDialog_WaitingWithProgressBar;


/**
 * A {@link ContextCommand} plugin for adding <noise</a>
 * to an image.
 */
@Plugin(type = ContextCommand.class,
		headless = true,
		initializer = "initialPluginLaunch",
		iconPath = "/icons/comsystan-logo-grey46-16x16.png", //Menu entry icon
		menu = {})

public class Csaj2DSegRGBRelCmd<T extends RealType<T>> extends ContextCommand implements Previewable {

	private static final String PLUGIN_LABEL          = "<html><b>Noise</b></html>";
	private static final String SPACE_LABEL           = "";
	private static final String RATIOTYPE_LABEL    = "<html><b>Relative ratio type</b></html>";
	private static final String DISPLAYOPTIONS_LABEL  = "<html><b>Display options</b></html>";
	private static final String PROCESSOPTIONS_LABEL  = "<html><b>Process options</b></html>";

	Cursor<?> cursor = null;
	private static String datasetName;
	private static String[] sliceLabels;
	private static long width     = 0;
	private static long height    = 0;
	private static long numDimensions = 0;
	private static long numSlices = 0;
	private static long compositeChannelCount =0;
	private static String imageType = "";
	private static RealMask realMask;  //ROI
	
	public static final String IMAGE_OUT_NAME = "RGB Relative segmented image(s)";
	
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
	
	@Parameter
	private ROIService roiService;

	// Input dataset which is updated in callback functions
	@Parameter(type = ItemIO.INPUT)
	private Dataset datasetIn;

	@Parameter(label = IMAGE_OUT_NAME, type = ItemIO.OUTPUT)
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
	private final String labelRatioTypeOptions = RATIOTYPE_LABEL;
	
	@Parameter(label = "RGB channel ratio type",
			   description = "Type of relative RGB ratio",
			   style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
			   choices = {"R/(R+G+B)", "R/(G+B)", "R/G", "R/B", "G/(R+G+B)", "G/(R+B)", "G/R", "G/B", "B/(R+G+B)", "B/(R+G)", "B/R", "B/G"},
			   persist = true, //restore previous value default = true
			   initializer = "initialRatioType",
			   callback = "callbackRatioType")
	private String choiceRadioButt_RatioType;
	
	@Parameter(label = "Ratio threshold",
			   description = "Relative ratio threshold",
			   style = NumberWidget.SPINNER_STYLE,
			   min = "0f",
			   max = "1f",
			   stepSize = "0.01",
			   persist = true, // restore  previous value  default  =  true
			   initializer = "initialRatio",
			   callback = "callbackRatio")
	private float spinnerFloat_Ratio;
	
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
    
	@Parameter(label = "OK - process image #", description = "Image slice number", style = NumberWidget.SPINNER_STYLE, min = "1", max = "99999999", stepSize = "1",
			   persist = false, // restore  previous value  default  =  true
			   initializer = "initialNumImageSlice",
			   callback = "callbackNumImageSlice")
	private int spinnerInteger_NumImageSlice;
	
	@Parameter(label = "OK - process all images",
			   description = "Set for final Command.run execution",
			   persist = false, // restore  previous value  default  =  true
			   initializer = "initialProcessAll")
	private boolean processAll;
	
	@Parameter(label = "   Preview of single image #    ", callback = "callbackProcessSingleImage")
	private Button buttonProcessSingelImage;
   
//	Deactivated, because it does not work in Fiji (although it works in ImageJ2 -Eclipse)	
//  @Parameter(label = "Preview of single active image ", callback = "callbackProcessActiveImage")
//	private Button buttonProcessActiveImage;
	
	@Parameter(label = "Preview of all available images", callback = "callbackProcessAllImages")
	private Button buttonProcessAllImages;

	// ---------------------------------------------------------------------
	// The following initializer functions set initial value
	protected void initialPluginLaunch() {
		checkItemIOIn();
	}
	
	protected void initialRatioType() {
		choiceRadioButt_RatioType = "R/(R+G+B)";
	}	
	protected void initialRatio() {
 		spinnerFloat_Ratio = 0.5f;
 	}
	protected void initialOverwriteDisplays() {
		booleanOverwriteDisplays = true;
	}
	protected void initialNumImageSlice() {
    	spinnerInteger_NumImageSlice = 1;
	}
	
	// ------------------------------------------------------------------------------
	/** Executed whenever the {@link #choiceRadioButt_NoiseType} parameter changes. */
	/** Executed whenever the {@link #choiceRadioButt_RatioType} parameter changes. */
	protected void callbackRatioType() {
		logService.info(this.getClass().getName() + " Ratio type set to " + choiceRadioButt_RatioType);
	}
	/** Executed whenever the {@link #spinnerFloat_Ratio} parameter changes. */
	protected void callbackRatio() {
		logService.info(this.getClass().getName() + " Realtive ratio set to " + spinnerFloat_Ratio);
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
	    		uiService.show(IMAGE_OUT_NAME, datasetOut);   //Show result because it did not go over the run() method
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
	    	   	uiService.show(IMAGE_OUT_NAME, datasetOut);
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
		    		uiService.show(IMAGE_OUT_NAME, datasetOut);   //Show result because it did not go over the run() method
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
		logService.info(this.getClass().getName() + " Starting command run");

		checkItemIOIn();
		if (processAll) startWorkflowForAllImages();
		else            startWorkflowForSingleImage();
	
		logService.info(this.getClass().getName() + " Finished command run");
	}
	
	public void checkItemIOIn() {
		//Get input meta data
		HashMap<String, Object> datasetInInfo = CsajCheck_ItemIn.checkDatasetIn(logService, datasetIn);
		if (datasetInInfo == null) {
			logService.error(MethodHandles.lookup().lookupClass().getName() + " ERROR: Missing input image or image type is not byte or float");
			cancel("ComsystanJ 2D plugin cannot be started - missing input image or wrong image type.");
		} else {
			width  =       			(long)datasetInInfo.get("width");
			height =       			(long)datasetInInfo.get("height");
			numDimensions =         (int)datasetInInfo.get("numDimensions");
			compositeChannelCount = (int)datasetInInfo.get("compositeChannelCount");
			numSlices =             (long)datasetInInfo.get("numSlices");
			imageType =   			(String)datasetInInfo.get("imageType");
			datasetName = 			(String)datasetInInfo.get("datasetName");
			sliceLabels = 			(String[])datasetInInfo.get("sliceLabels");
			
			//Grey not allowed
			if (!imageType.equals("RGB")) { 
				logService.error(this.getClass().getName() + " ERROR: Grey value image(s) expected!");
				cancel("ComsystanJ 2D plugin cannot be started - RGB image(s) expected!");
			}
		}
	}

	/**
	 * This method generates the dataset
	 * @param rai
	 */
	private void generateDataset(RandomAccessibleInterval<T> rai) {
		if (imageType.equals("Grey")) {
			//RGB image
			boolean signed   = false;
			boolean floating = false;
			boolean virtual  = false;
			
			int bitsPerPixel = 8;
			//long[] dims = new long[]{width, height};
			long[] dims = new long[]{rai.dimension(0), rai.dimension(1)};
			AxisType[] axes = new AxisType[]{Axes.X, Axes.Y};
			datasetOut = datasetService.create(dims, IMAGE_OUT_NAME, axes, bitsPerPixel, signed, floating, virtual);	
			
			Cursor<RealType<?>> cursor = datasetOut.localizingCursor();
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
			datasetOut = datasetService.create(dims, IMAGE_OUT_NAME, axes, bitsPerPixel, signed, floating, virtual);	
			datasetOut.setCompositeChannelCount(3);
			datasetOut.setRGBMerged(true);
//			datasetOut.setChannelMinimum(0, 0);
//			datasetOut.setChannelMinimum(1, 0);
//			datasetOut.setChannelMinimum(2, 0);
//			datasetOut.setChannelMaximum(0, 255);
//			datasetOut.setChannelMaximum(1, 255);
//			datasetOut.setChannelMaximum(2, 255);
//			datasetOut.initializeColorTables(3);
//			datasetOut.setColorTable(ColorTables.RED,   0);
//			datasetOut.setColorTable(ColorTables.GREEN, 1);
//			datasetOut.setColorTable(ColorTables.BLUE,  2);
			
			Cursor<RealType<?>> cursor = datasetOut.localizingCursor();
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
		
		dlgProgress = new CsajDialog_WaitingWithProgressBar("RGB relative segmenting, please wait... Open console window for further info.",
							logService, false, exec); //isCanceable = false, because no following method listens to exec.shutdown 
		
		dlgProgress.updatePercent("");
		dlgProgress.setBarIndeterminate(true);
		dlgProgress.setVisible(true);
	
		deleteExistingDisplays();
    	int sliceIndex = spinnerInteger_NumImageSlice - 1;
		logService.info(this.getClass().getName() + " Processing single image " + (sliceIndex + 1));
		RandomAccessibleInterval<T> rai = processSingleInputImage(sliceIndex);
		generateDataset(rai);
		dlgProgress.addMessage("Processing finished!");
		dlgProgress.setVisible(false);
		dlgProgress.dispose();
		Toolkit.getDefaultToolkit().beep();   
	}

	/**
	* This method starts the workflow for all images of the active display
	*/
	protected void startWorkflowForAllImages() {

		dlgProgress = new CsajDialog_WaitingWithProgressBar("RGB relative segmenting, please wait... Open console window for further info.",
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
				if ((frame.getTitle().contains(IMAGE_OUT_NAME)) ) {
					frame.setVisible(false); //Successfully closes also in Fiji
					frame.dispose();
					
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
//				if (display.getName().contains(TABLE_OUT_NAME)) display.close();
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
		datasetOut.setName(IMAGE_OUT_NAME);
		
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
		datasetOut.setName(IMAGE_OUT_NAME);
		
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
	 * @param <RandomAccessibleInterval>
	 * @return 
	 */
	private RandomAccessibleInterval process(RandomAccessibleInterval rai) { //one slice grey or RGB

		if (rai == null) {
			logService.info(this.getClass().getName() + " WARNING: rai==null, no image for processing!");
		}
		
		String ratioType = choiceRadioButt_RatioType;
		float ratio = spinnerFloat_Ratio;
		//float ratio = Precision.round(spinnerFloat_Ratio, 2); //round to 1 decimal, because sometimes float is not exact
		
		
		
		RandomAccessibleInterval<T> raiR = null;	
		RandomAccessibleInterval<T> raiG = null;	
		RandomAccessibleInterval<T> raiB = null;	
		RandomAccess<T> raR = null;
		RandomAccess<T> raG = null;
		RandomAccess<T> raB = null;
		double red;
		double green;
		double blue;
		double sum;
		long[] pos = new long[2];
					
		if (ratioType.equals("R/(R+G+B)")) {	
			raiR = Views.hyperSlice(rai, 2, 0); //R
			raG  = Views.hyperSlice(rai, 2, 1).randomAccess(); //G
			raB  = Views.hyperSlice(rai, 2, 2).randomAccess(); //B
					
			cursor = Views.iterable(raiR).localizingCursor();	
			while (cursor.hasNext()) {
				cursor.fwd();
				cursor.localize(pos);
				raG.setPosition(pos);
				raB.setPosition(pos);
				red   = (double)((UnsignedByteType) cursor.get()).get();
				green = raG.get().getRealDouble();
				blue  = raB.get().getRealDouble();
				if ((red/(red+green+blue)) > ratio) {
					((UnsignedByteType) cursor.get()).set(255);
					raG.get().setReal(0);
					raB.get().setReal(0);
				} else {	
//					((UnsignedByteType) cursor.get()).set(0);
//					raG.get().setReal(0);
//					raB.get().setReal(0);
				}	
			} //cursor
			
		} else if (ratioType.equals("R/(G+B)")) {	
			raiR = Views.hyperSlice(rai, 2, 0); //R
			raG  = Views.hyperSlice(rai, 2, 1).randomAccess(); //G
			raB  = Views.hyperSlice(rai, 2, 2).randomAccess(); //B
					
			cursor = Views.iterable(raiR).localizingCursor();	
			while (cursor.hasNext()) {
				cursor.fwd();
				cursor.localize(pos);
				raG.setPosition(pos);
				raB.setPosition(pos);
				red   = (double)((UnsignedByteType) cursor.get()).get();
				green = raG.get().getRealDouble();
				blue  = raB.get().getRealDouble();
				if ((red/(green+blue)) > ratio) {
					((UnsignedByteType) cursor.get()).set(255);
					raG.get().setReal(0);
					raB.get().setReal(0);
				} else {	
//					((UnsignedByteType) cursor.get()).set(0);
//					raG.get().setReal(0);
//					raB.get().setReal(0);
				}	
			} //cursor
		
		} else if (ratioType.equals("R/G")) {	
			raiR = Views.hyperSlice(rai, 2, 0); //R
			raG  = Views.hyperSlice(rai, 2, 1).randomAccess(); //G
			raB  = Views.hyperSlice(rai, 2, 2).randomAccess(); //B
					
			cursor = Views.iterable(raiR).localizingCursor();	
			while (cursor.hasNext()) {
				cursor.fwd();
				cursor.localize(pos);
				raG.setPosition(pos);
				raB.setPosition(pos);
				red   = (double)((UnsignedByteType) cursor.get()).get();
				green = raG.get().getRealDouble();
				blue  = raB.get().getRealDouble();
				if ((red/green) > ratio) {
					((UnsignedByteType) cursor.get()).set(255);
					raG.get().setReal(0);
					raB.get().setReal(0);
				} else {	
//					((UnsignedByteType) cursor.get()).set(0);
//					raG.get().setReal(0);
//					raB.get().setReal(0);
				}	
			} //cursor
		
		} else if (ratioType.equals("R/B")) {	
			raiR = Views.hyperSlice(rai, 2, 0); //R
			raG  = Views.hyperSlice(rai, 2, 1).randomAccess(); //G
			raB  = Views.hyperSlice(rai, 2, 2).randomAccess(); //B
					
			cursor = Views.iterable(raiR).localizingCursor();	
			while (cursor.hasNext()) {
				cursor.fwd();
				cursor.localize(pos);
				raG.setPosition(pos);
				raB.setPosition(pos);
				red   = (double)((UnsignedByteType) cursor.get()).get();
				green = raG.get().getRealDouble();
				blue  = raB.get().getRealDouble();
				if ((red/blue) > ratio) {
					((UnsignedByteType) cursor.get()).set(255);
					raG.get().setReal(0);
					raB.get().setReal(0);
				} else {	
//					((UnsignedByteType) cursor.get()).set(0);
//					raG.get().setReal(0);
//					raB.get().setReal(0);
				}	
			} //cursor
		
		} else if (ratioType.equals("G/(R+G+B)")) {	
			raR  = Views.hyperSlice(rai, 2, 0).randomAccess(); //R
			raiG = Views.hyperSlice(rai, 2, 1); //G
			raB  = Views.hyperSlice(rai, 2, 2).randomAccess(); //B
					
			cursor = Views.iterable(raiG).localizingCursor();	
			while (cursor.hasNext()) {
				cursor.fwd();
				cursor.localize(pos);
				raR.setPosition(pos);
				raB.setPosition(pos);
				red   = raR.get().getRealDouble();
				green = (double)((UnsignedByteType) cursor.get()).get();
				blue  = raB.get().getRealDouble();
				if ((green/(red+green+blue)) > ratio) {
					raR.get().setReal(0);
					((UnsignedByteType) cursor.get()).set(255);
					raB.get().setReal(0);
				} else {	

				}	
			} //cursor		
		
		} else if (ratioType.equals("G/(R+B)")) {	
			raR  = Views.hyperSlice(rai, 2, 0).randomAccess(); //R
			raiG = Views.hyperSlice(rai, 2, 1); //G
			raB  = Views.hyperSlice(rai, 2, 2).randomAccess(); //B
					
			cursor = Views.iterable(raiG).localizingCursor();	
			while (cursor.hasNext()) {
				cursor.fwd();
				cursor.localize(pos);
				raR.setPosition(pos);
				raB.setPosition(pos);
				red   = raR.get().getRealDouble();
				green = (double)((UnsignedByteType) cursor.get()).get();
				blue  = raB.get().getRealDouble();
				if ((green/(red+blue)) > ratio) {
					raR.get().setReal(0);
					((UnsignedByteType) cursor.get()).set(255);
					raB.get().setReal(0);
				} else {	

				}	
			} //cursor
		
		} else if (ratioType.equals("G/R")) {	
			raR  = Views.hyperSlice(rai, 2, 0).randomAccess(); //R
			raiG = Views.hyperSlice(rai, 2, 1); //G
			raB  = Views.hyperSlice(rai, 2, 2).randomAccess(); //B
					
			cursor = Views.iterable(raiG).localizingCursor();	
			while (cursor.hasNext()) {
				cursor.fwd();
				cursor.localize(pos);
				raR.setPosition(pos);
				raB.setPosition(pos);
				red   = raR.get().getRealDouble();
				green = (double)((UnsignedByteType) cursor.get()).get();
				blue  = raB.get().getRealDouble();
				if ((green/red) > ratio) {
					raR.get().setReal(0);
					((UnsignedByteType) cursor.get()).set(255);
					raB.get().setReal(0);
				} else {	

				}	
			} //cursor
		
		} else if (ratioType.equals("G/B")) {	
			raR  = Views.hyperSlice(rai, 2, 0).randomAccess(); //R
			raiG = Views.hyperSlice(rai, 2, 1); //G
			raB  = Views.hyperSlice(rai, 2, 2).randomAccess(); //B
					
			cursor = Views.iterable(raiG).localizingCursor();	
			while (cursor.hasNext()) {
				cursor.fwd();
				cursor.localize(pos);
				raR.setPosition(pos);
				raB.setPosition(pos);
				red   = raR.get().getRealDouble();
				green = (double)((UnsignedByteType) cursor.get()).get();
				blue  = raB.get().getRealDouble();
				if ((green/blue) > ratio) {
					raR.get().setReal(0);
					((UnsignedByteType) cursor.get()).set(255);
					raB.get().setReal(0);
				} else {	

				}	
			} //cursor	
			
		} else if (ratioType.equals("B/(R+G+B)")) {	
			raR  = Views.hyperSlice(rai, 2, 0).randomAccess(); //R
			raG  = Views.hyperSlice(rai, 2, 1).randomAccess(); //G
			raiB = Views.hyperSlice(rai, 2, 2); //B
					
			cursor = Views.iterable(raiB).localizingCursor();	
			while (cursor.hasNext()) {
				cursor.fwd();
				cursor.localize(pos);
				raR.setPosition(pos);
				raG.setPosition(pos);
				red   = raR.get().getRealDouble();
				green = raG.get().getRealDouble();
				blue  = (double)((UnsignedByteType) cursor.get()).get();
				if ((blue/(red+green+blue)) > ratio) {
					raR.get().setReal(0);
					raG.get().setReal(0);
					((UnsignedByteType) cursor.get()).set(255);
				} else {	
					
				}	
			} //cursor	
			
		} else if (ratioType.equals("B/(R+G)")) {	
			raR  = Views.hyperSlice(rai, 2, 0).randomAccess(); //R
			raG  = Views.hyperSlice(rai, 2, 1).randomAccess(); //G
			raiB = Views.hyperSlice(rai, 2, 2); //B
					
			cursor = Views.iterable(raiB).localizingCursor();	
			while (cursor.hasNext()) {
				cursor.fwd();
				cursor.localize(pos);
				raR.setPosition(pos);
				raG.setPosition(pos);
				red   = raR.get().getRealDouble();
				green = raG.get().getRealDouble();
				blue  = (double)((UnsignedByteType) cursor.get()).get();
				if ((blue/(red+green)) > ratio) {
					raR.get().setReal(0);
					raG.get().setReal(0);
					((UnsignedByteType) cursor.get()).set(255);
				} else {	
					
				}	
			} //cursor
			
		} else if (ratioType.equals("B/R")) {	
			raR  = Views.hyperSlice(rai, 2, 0).randomAccess(); //R
			raG  = Views.hyperSlice(rai, 2, 1).randomAccess(); //G
			raiB = Views.hyperSlice(rai, 2, 2); //B
					
			cursor = Views.iterable(raiB).localizingCursor();	
			while (cursor.hasNext()) {
				cursor.fwd();
				cursor.localize(pos);
				raR.setPosition(pos);
				raG.setPosition(pos);
				red   = raR.get().getRealDouble();
				green = raG.get().getRealDouble();
				blue  = (double)((UnsignedByteType) cursor.get()).get();
				if ((blue/red) > ratio) {
					raR.get().setReal(0);
					raG.get().setReal(0);
					((UnsignedByteType) cursor.get()).set(255);
				} else {	
					
				}	
			} //cursor
			
		} else if (ratioType.equals("B/G")) {	
			raR  = Views.hyperSlice(rai, 2, 0).randomAccess(); //R
			raG  = Views.hyperSlice(rai, 2, 1).randomAccess(); //G
			raiB = Views.hyperSlice(rai, 2, 2); //B
					
			cursor = Views.iterable(raiB).localizingCursor();	
			while (cursor.hasNext()) {
				cursor.fwd();
				cursor.localize(pos);
				raR.setPosition(pos);
				raG.setPosition(pos);
				red   = raR.get().getRealDouble();
				green = raG.get().getRealDouble();
				blue  = (double)((UnsignedByteType) cursor.get()).get();
				if ((blue/green) > ratio) {
					raR.get().setReal(0);
					raG.get().setReal(0);
					((UnsignedByteType) cursor.get()).set(255);
				} else {	
					
				}	
			} //cursor
			
		}	
		return rai;	
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
