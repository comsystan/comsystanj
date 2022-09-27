/*-
 * #%L
 * Project: ImageJ2 plugin for adding noise.
 * File: Plugin2DImageNoise.java
 * 
 * $Id$
 * $HeadURL$
 * 
 * This file is part of ComsystanJ software, hereinafter referred to as "this program".
 * %%
 * Copyright (C) 2022 Comsystan Software
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
package at.csa.csaj.plugin2d.noise;

import java.awt.Frame;
import java.awt.Toolkit;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Random;
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
import net.imglib2.type.numeric.real.FloatType;
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
import at.csa.csaj.commons.dialog.WaitingDialogWithProgressBar;
import io.scif.DefaultImageMetadata;
import io.scif.MetaTable;


/**
 * A {@link Command} plugin for adding <noise</a>
 * to an image.
 */
@Plugin(type = ContextCommand.class,
		headless = true,
		initializer = "initialPluginLaunch",
		//iconPath = "/images/comsystan-??.png", //Menu entry icon
		menu = {
		@Menu(label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT, mnemonic = MenuConstants.PLUGINS_MNEMONIC),
		@Menu(label = "ComsystanJ"),
		@Menu(label = "2D Image(s)"),
		@Menu(label = "Noise", weight = 20)})
//public class Img2DImageNoise<T extends RealType<T>> extends InteractiveCommand { // non blocking  GUI
public class Csaj2DImageNoise<T extends RealType<T>> extends ContextCommand implements Previewable { //modal GUI with cancel

	private static final String PLUGIN_LABEL          = "<html><b>Noise</b></html>";
	private static final String SPACE_LABEL           = "";
	private static final String NOISEOPTIONS_LABEL    = "<html><b>Noise adding options</b></html>";
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
	
	private static final String imageOutName = "Noise added image(s)";
	private static final String imagePreviewName = "Preview image";
	private static Dataset datasetPreview;
	
	private WaitingDialogWithProgressBar dlgProgress;
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
	private final String labelNoiseOptions = NOISEOPTIONS_LABEL;
	
	@Parameter(label = "Noise",
			   description = "Noise type",
			   style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
			   choices = {"Shot", "Salt&Pepper", "Uniform", "Gaussian", "Rayleigh", "Exponential"}, //
			   persist = true,  //restore previous value default = true
			   initializer = "initialNoiseType",
			   callback = "callbackNoiseType")
	private String choiceRadioButt_NoiseType;
	
	@Parameter(label = "Percentage(%) or scale",
			   description = "Maximal percentage of affected data points or scaling parameter (e.g. sigma for Gaussian)",
			   style = NumberWidget.SPINNER_STYLE, 
			   min = "0",
			   max = "9999999999999999999",
			   stepSize = "0.1",
			   persist = true, // restore  previous value  default  =  true
			   initializer = "initialPercentage",
			   callback = "callbackPercentage")
	private float spinnerFloat_Percentage;
	
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
	
	@Parameter(label   = "   Process single image #    ",
		    	callback = "callbackProcessSingleImage")
	private Button buttonProcessSingelImage;
   
//	Deactivated, because it does not work in Fiji (although it works in ImageJ2 -Eclipse)	
// @Parameter(label   = "Process single active image ",
//  		    callback = "callbackProcessActiveImage")
//	private Button buttonProcessActiveImage;
	
// @Parameter(label   = "Process all available images",
//		        callback = "callbackProcessAllImages")
//	private Button buttonProcessAllImages;

	// ---------------------------------------------------------------------

	// The following initializer functions set initial value
	protected void initialPluginLaunch() {
		//datasetIn = imageDisplayService.getActiveDataset();
		checkItemIOIn();
	}
	protected void initialNoiseType() {
		choiceRadioButt_NoiseType = "Gaussian";
	} 
		
	protected void initialPercentage() {
		spinnerFloat_Percentage = 10;
	}
	protected void initialOverwriteDisplays() {
		booleanOverwriteDisplays = true;
	}
	protected void initialNumImageSlice() {
    	spinnerInteger_NumImageSlice = 1;
	}
	
	// ------------------------------------------------------------------------------
	

	/** Executed whenever the {@link #choiceRadioButt_NoiseType} parameter changes. */
	protected void callbackNoiseType() {
		logService.info(this.getClass().getName() + " Noise type set to " + choiceRadioButt_NoiseType);
	}
	
	/** Executed whenever the {@link #spinnerFloat_Percentage} parameter changes. */
	protected void callbackPercentage() {
	 	//round to ?? decimal after the comma
	 	//spinnerFloat_Percentage_ = Math.round(spinnerFloat_Percentage * 10f)/10f;
	 	spinnerFloat_Percentage = Precision.round(spinnerFloat_Percentage, 2);
		logService.info(this.getClass().getName() + " Sigma/Percentage set to " + spinnerFloat_Percentage);
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
		logService.info(this.getClass().getName() + " Widget canceled");
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
		//if(ij.ui().isHeadless()){
		//}	
	    startWorkflowForAllImages();
	}
	
	public void checkItemIOIn() {
	
		//datasetIn = imageDisplayService.getActiveDataset();
		//datasetIn = imageDisplayService.getActiveDataset();
		if (datasetIn == null) { 
			logService.info(this.getClass().getName() + " WARNING: Input dataset is not available");
			this.cancel("WARNING: Input data set is not available!");
		}	
		if ((datasetIn.firstElement() instanceof UnsignedByteType) || (datasetIn.firstElement() instanceof FloatType)) {
			// That is OK, proceed
		} else {
	
			final MessageType messageType = MessageType.QUESTION_MESSAGE;
			final OptionType optionType = OptionType.OK_CANCEL_OPTION;
			final String title = "Validation result";
			final String message = "Data type not allowed: " + datasetIn.getType().getClass().getSimpleName();
			// Prompt for confirmation.
			// final UIService uiService = getContext().getService(UIService.class);
			Result result = uiService.showDialog(message, title, messageType, optionType);
	
			// Cancel the command execution if the user does not agree.
			// if (result != Result.YES_OPTION) System.exit(-1);
			// if (result != Result.YES_OPTION) return;
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
			long[] dims = new long[]{width, height};
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
			long[] dims = new long[]{width, height, 3};
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
		
		dlgProgress = new WaitingDialogWithProgressBar("Adding noise, please wait... Open console window for further info.",
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

		dlgProgress = new WaitingDialogWithProgressBar("Adding noise, please wait... Open console window for further info.",
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
	 * @param <RandomAccessibleInterval>
	 * @return 
	 */
	private RandomAccessibleInterval process(RandomAccessibleInterval rai) { //one slice grey or RGB

		if (rai == null) {
			logService.info(this.getClass().getName() + " WARNING: rai==null, no image for processing!");
		}
			
		String noiseType  = choiceRadioButt_NoiseType;//"Shot" "Salt&Pepper", "Uniform" "Gaussian" "Rayleigh" "Exponential"
		double fraction   = (double)spinnerFloat_Percentage/100f;
		double scaleParam = spinnerFloat_Percentage; //That is not a good practice
		
		//imageType = "Grey"; // "Grey" "RGB"....
		//numSlices;
		
		Random random = new Random();
		random.setSeed(System.currentTimeMillis());
		int randomInt;	
		int pixelValue;	
		int newValue;
		long numChanged = 0;
		
		//"Shot" "Salt&Pepper", "Uniform" "Gaussian" "Rayleigh" "Exponential"
		if (noiseType.equals("Shot")) {
			//The percentage of all pixels are changed
			if (imageType.equals("Grey")) { //rai should have 2 dimensions
						
				//The percentage of all pixels are changed 
				int max = -Integer.MAX_VALUE;
				//double min = Double.MAX_VALUE;
					
				cursor = Views.iterable(rai).localizingCursor();	
				while (cursor.hasNext()) {
					cursor.fwd();
					//cursor.localize(pos);	
					pixelValue = ((UnsignedByteType) cursor.get()).get();
					if (pixelValue > max) max = pixelValue;
				} //cursor
				
				cursor.reset();
				while (cursor.hasNext()) {
					cursor.fwd();
					//cursor.localize(pos);	
					if (random.nextDouble() < fraction) {
						numChanged = numChanged + 1;
						((UnsignedByteType) cursor.get()).set(max);
					}	
				}
						
			} else if (imageType.equals("RGB")) {
			
				int numDim = rai.numDimensions();
				RandomAccessibleInterval<T> raiSlice = null;	
				
				for (int b = 0; b < numDim; b++) {
					raiSlice = (RandomAccessibleInterval<T>) Views.hyperSlice(rai, 2, b);
					
					//The percentage of all pixels are changed 
					int max = -Integer.MAX_VALUE;
					//double min = Double.MAX_VALUE;
					
					cursor = Views.iterable(raiSlice).localizingCursor();			
					while (cursor.hasNext()) {
						cursor.fwd();
						//cursor.localize(pos);	
						pixelValue = ((UnsignedByteType) cursor.get()).get();
						if (pixelValue > max) max = pixelValue;
					} //cursor
					
					cursor.reset();
					while (cursor.hasNext()) {
						cursor.fwd();
						//cursor.localize(pos);	
						if (random.nextDouble() < fraction/3.0) { //for each channel only 1/3 should be changed
							numChanged = numChanged + 1;
							((UnsignedByteType) cursor.get()).set(max);
						}	
					}							
				} //b
			} //RGB	
		} //Shot
		
		if (noiseType.equals("Salt&Pepper")) {
			//The percentage of all pixels are changed  
			
			if (imageType.equals("Grey")) { //rai should have 2 dimensions
			
				//The percentage of all pixels are changed 
				int max = -Integer.MAX_VALUE;
				int min =  Integer.MAX_VALUE;
					
				cursor = Views.iterable(rai).localizingCursor();	
				while (cursor.hasNext()) {
					cursor.fwd();
					//cursor.localize(pos);	
					pixelValue = ((UnsignedByteType) cursor.get()).get();
					if (pixelValue > max) max = pixelValue;
					if (pixelValue < min) min = pixelValue;
				} //cursor
				
				cursor.reset();
				while (cursor.hasNext()) {
					cursor.fwd();
					//cursor.localize(pos);	
					if (random.nextDouble() < fraction) {
						numChanged = numChanged + 1;
						randomInt = random.nextInt(2);
						if 		(randomInt == 0) ((UnsignedByteType) cursor.get()).set(max);
						else if (randomInt == 1) ((UnsignedByteType) cursor.get()).set(min);			
					}	
				}
						
			} else if (imageType.equals("RGB")) {
				
				int numDim = rai.numDimensions();
				RandomAccessibleInterval<T> raiSlice = null;	
				
				for (int b = 0; b < numDim; b++) {
					raiSlice = (RandomAccessibleInterval<T>) Views.hyperSlice(rai, 2, b);			
					//The percentage of all pixels are changed 
					int max = -Integer.MAX_VALUE;
					int min =  Integer.MAX_VALUE;
					
					cursor = Views.iterable(raiSlice).localizingCursor();			
					while (cursor.hasNext()) {
						cursor.fwd();
						//cursor.localize(pos);	
						pixelValue = ((UnsignedByteType) cursor.get()).get();
						if (pixelValue > max) max = pixelValue;
						if (pixelValue < min) min = pixelValue;
					} //cursor
					
					cursor.reset();
					while (cursor.hasNext()) {
						cursor.fwd();
						//cursor.localize(pos);	
						if (random.nextDouble() < fraction/3.0) { //for each channel only 1/3 should be changed
							numChanged = numChanged + 1;
							randomInt = random.nextInt(2);
							if 		(randomInt == 0) ((UnsignedByteType) cursor.get()).set(max);
							else if (randomInt == 1) ((UnsignedByteType) cursor.get()).set(min); 						
						}	
					}							
				} //b
			} //RGB	
		} //Salt&Pepper
		
		if (noiseType.equals("Uniform")) {
			
			//The percentage of each original pixel value is computed.
			//A value between 0 and this percentage is added or subtracted to the original value
			
			if (imageType.equals("Grey")) { //rai should have 2 dimensions
						
				cursor = Views.iterable(rai).localizingCursor();	
				while (cursor.hasNext()) {
					cursor.fwd();
					//cursor.localize(pos);	
					numChanged = numChanged + 1;
					pixelValue = ((UnsignedByteType) cursor.get()).get();
					randomInt = random.nextInt(2);
					if (randomInt == 0) {
						newValue = pixelValue + (int)Math.round(random.nextDouble()*fraction*pixelValue); 
						if (newValue > 255) newValue = 255;
						((UnsignedByteType) cursor.get()).set(newValue);
					}
					else if (randomInt == 1) {
						newValue = pixelValue - (int)Math.round(random.nextDouble()*fraction*pixelValue); 
						if (newValue < 0) newValue = 0;
						((UnsignedByteType) cursor.get()).set(newValue);
					}
				}
						
			} else if (imageType.equals("RGB")) {
			
				int numDim = rai.numDimensions();
				RandomAccessibleInterval<T> raiSlice = null;	
				
				for (int b = 0; b < numDim; b++) {
					raiSlice = (RandomAccessibleInterval<T>) Views.hyperSlice(rai, 2, b);				
					cursor = Views.iterable(raiSlice).localizingCursor();			
					while (cursor.hasNext()) {
						cursor.fwd();
						//cursor.localize(pos);	
						numChanged = numChanged + 1;
						pixelValue = ((UnsignedByteType) cursor.get()).get();
						randomInt = random.nextInt(2);
						if (randomInt == 0) {
							newValue = pixelValue + (int)Math.round(random.nextDouble()*fraction*pixelValue/3.0); //for each channel only 1/3 change
							if (newValue > 255) newValue = 255;
							((UnsignedByteType) cursor.get()).set(newValue);
						}
						else if (randomInt == 1) {
							newValue = pixelValue - (int)Math.round(random.nextDouble()*fraction*pixelValue/3.0); //for each channel only 1/3 change
							if (newValue < 0) newValue = 0;
							((UnsignedByteType) cursor.get()).set(newValue);
						}
					} //cursor					
				} //b
			} //RGB	
		} //Uniform
		
		if (noiseType.equals("Gaussian")) {
			
			//Gaussian noise with mean=0 and the specified sigma is added
			double sigma = scaleParam;
			
			if (imageType.equals("Grey")) { //rai should have 2 dimensions
						
				cursor = Views.iterable(rai).localizingCursor();	
				while (cursor.hasNext()) {
					cursor.fwd();
					//cursor.localize(pos);	
					numChanged = numChanged + 1;
					pixelValue = ((UnsignedByteType) cursor.get()).get();
					randomInt = random.nextInt(2);
					
					if (randomInt == 0) {
						newValue = pixelValue + (int)Math.round(random.nextGaussian() * sigma); 
						if (newValue > 255) newValue = 255;
						((UnsignedByteType) cursor.get()).set(newValue);
					}
					else if (randomInt == 1) {
						newValue = pixelValue - (int)Math.round(random.nextGaussian() * sigma); 
						if (newValue < 0) newValue = 0;
						((UnsignedByteType) cursor.get()).set(newValue);
					}
					
				}
						
			} else if (imageType.equals("RGB")) {
			
				int numDim = rai.numDimensions();
				RandomAccessibleInterval<T> raiSlice = null;	
				
				for (int b = 0; b < numDim; b++) {
					raiSlice = (RandomAccessibleInterval<T>) Views.hyperSlice(rai, 2, b);				
					cursor = Views.iterable(raiSlice).localizingCursor();			
					while (cursor.hasNext()) {
						cursor.fwd();
						//cursor.localize(pos);	
						numChanged = numChanged + 1;
						pixelValue = ((UnsignedByteType) cursor.get()).get();
						randomInt = random.nextInt(2);
						if (randomInt == 0) {
							newValue = pixelValue + (int)Math.round(random.nextGaussian() * sigma /3.0); //for each channel only 1/3 change
							if (newValue > 255) newValue = 255;
							((UnsignedByteType) cursor.get()).set(newValue);
						}
						else if (randomInt == 1) {
							newValue = pixelValue - (int)Math.round(random.nextGaussian() * sigma /3.0); //for each channel only 1/3 change
							if (newValue < 0) newValue = 0;
							((UnsignedByteType) cursor.get()).set(newValue);
						}
					} //cursor					
				} //b
			} //RGB	
		} //Gaussian
		
		if (noiseType.equals("Rayleigh")) {
			
			//Rayleigh noise with the specified scale parameter is added
			//see https://www.randomservices.org/random/special/Rayleigh.html      point 35.
			//or
			//https://en.wikipedia.org/wiki/Rayleigh_distribution
			if (imageType.equals("Grey")) { //rai should have 2 dimensions
						
				cursor = Views.iterable(rai).localizingCursor();	
				while (cursor.hasNext()) {
					cursor.fwd();
					//cursor.localize(pos);	
					numChanged = numChanged + 1;
					pixelValue = ((UnsignedByteType) cursor.get()).get();
					randomInt = random.nextInt(2);
					
					if (randomInt == 0) {
						newValue = pixelValue + (int)Math.round(scaleParam * Math.sqrt(-2*Math.log(random.nextDouble()))); 
						if (newValue > 255) newValue = 255;
						((UnsignedByteType) cursor.get()).set(newValue);
					}
					else if (randomInt == 1) {
						newValue = pixelValue - (int)Math.round(scaleParam * Math.sqrt(-2*Math.log(random.nextDouble()))); 
						if (newValue < 0) newValue = 0;
						((UnsignedByteType) cursor.get()).set(newValue);
					}
					
				}
						
			} else if (imageType.equals("RGB")) {
			
				int numDim = rai.numDimensions();
				RandomAccessibleInterval<T> raiSlice = null;	
				
				for (int b = 0; b < numDim; b++) {
					raiSlice = (RandomAccessibleInterval<T>) Views.hyperSlice(rai, 2, b);				
					cursor = Views.iterable(raiSlice).localizingCursor();			
					while (cursor.hasNext()) {
						cursor.fwd();
						//cursor.localize(pos);	
						numChanged = numChanged + 1;
						pixelValue = ((UnsignedByteType) cursor.get()).get();
						randomInt = random.nextInt(2);
						if (randomInt == 0) {
							newValue = pixelValue + (int)Math.round(scaleParam * Math.sqrt(-2*Math.log(random.nextDouble()))/3.0); //for each channel only 1/3 change
							if (newValue > 255) newValue = 255;
							((UnsignedByteType) cursor.get()).set(newValue);
						}
						else if (randomInt == 1) {
							newValue = pixelValue - (int)Math.round(scaleParam * Math.sqrt(-2*Math.log(random.nextDouble()))/3.0);  //for each channel only 1/3 change
							if (newValue < 0) newValue = 0;
							((UnsignedByteType) cursor.get()).set(newValue);
						}
					} //cursor					
				} //b
			} //RGB	
		} //Rayleigh
		
		if (noiseType.equals("Exponential")) {
			
			//Gaussian noise with mean=0 and the specified sigma is added
			double sigma = scaleParam;
			
			if (imageType.equals("Grey")) { //rai should have 2 dimensions
						
				cursor = Views.iterable(rai).localizingCursor();	
				while (cursor.hasNext()) {
					cursor.fwd();
					//cursor.localize(pos);	
					numChanged = numChanged + 1;
					pixelValue = ((UnsignedByteType) cursor.get()).get();
					randomInt = random.nextInt(2);
					
					if (randomInt == 0) {
						newValue = pixelValue + (int)Math.round(-scaleParam*Math.log(random.nextDouble())); 
						if (newValue > 255) newValue = 255;
						((UnsignedByteType) cursor.get()).set(newValue);
					}
					else if (randomInt == 1) {
						newValue = pixelValue - (int)Math.round(-scaleParam*Math.log(random.nextDouble())); 
						if (newValue < 0) newValue = 0;
						((UnsignedByteType) cursor.get()).set(newValue);
					}
					
				}
						
			} else if (imageType.equals("RGB")) {
			
				int numDim = rai.numDimensions();
				RandomAccessibleInterval<T> raiSlice = null;	
				
				for (int b = 0; b < numDim; b++) {
					raiSlice = (RandomAccessibleInterval<T>) Views.hyperSlice(rai, 2, b);				
					cursor = Views.iterable(raiSlice).localizingCursor();			
					while (cursor.hasNext()) {
						cursor.fwd();
						//cursor.localize(pos);	
						numChanged = numChanged + 1;
						pixelValue = ((UnsignedByteType) cursor.get()).get();
						randomInt = random.nextInt(2);
						if (randomInt == 0) {
							newValue = pixelValue + (int)Math.round(-scaleParam*Math.log(random.nextDouble())/3.0); //for each channel only 1/3 change
							if (newValue > 255) newValue = 255;
							((UnsignedByteType) cursor.get()).set(newValue);
						}
						else if (randomInt == 1) {
							newValue = pixelValue - (int)Math.round(-scaleParam*Math.log(random.nextDouble())/3.0); //for each channel only 1/3 change
							if (newValue < 0) newValue = 0;
							((UnsignedByteType) cursor.get()).set(newValue);
						}
					} //cursor					
				} //b
			} //RGB	
		} //Exponential
		
		logService.info(this.getClass().getName() + " Number of changed pixels: " + numChanged);
		
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
		// ij.command().run(FractalDimensionHiguchi1D.class,
		// true).get().getOutput("image");
		ij.command().run(Csaj2DImageNoise.class, true);
	}
}
