/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj3DSuccolarityCmd.java
 * 
 * $Id$
 * $HeadURL$
 * 
 * This file is part of ComsystanJ software, hereinafter referred to as "this program".
 * %%
 * Copyright (C) 2024 - 2026 Comsystan Software
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

package at.csa.csaj.plugin3d.frac;

import java.awt.Frame;
import java.awt.Toolkit;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ImageJ;
import net.imagej.Position;
import net.imagej.display.ImageDisplayService;
import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.morphology.Dilation;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import org.scijava.ItemIO;
import org.scijava.ItemVisibility;
import org.scijava.app.StatusService;
import org.scijava.command.ContextCommand;
import org.scijava.command.Previewable;
import org.scijava.display.DefaultDisplayService;
import org.scijava.display.Display;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;
import org.scijava.table.DefaultGenericTable;
import org.scijava.table.DoubleColumn;
import org.scijava.table.GenericColumn;
import org.scijava.table.IntColumn;
import org.scijava.ui.UIService;
import org.scijava.widget.Button;
import org.scijava.widget.ChoiceWidget;
import org.scijava.widget.FileWidget;
import org.scijava.widget.NumberWidget;

import at.csa.csaj.commons.CsajDialog_WaitingWithProgressBar;
import at.csa.csaj.commons.CsajPlot_RegressionFrame;
import at.csa.csaj.commons.CsajCheck_ItemIn;
import at.csa.csaj.commons.CsajContainer_ProcessMethod;

/**
 * A {@link ContextCommand} plugin computing <the 3D Succolarity</a>
 * of an image volume.
 */
@Plugin(type = ContextCommand.class,
		headless = true,
		label = "3D Succolarity",
		initializer = "initialPluginLaunch",
		iconPath = "/icons/comsystan-logo-grey46-16x16.png", //Menu entry icon
		menu = {})

public class Csaj3DSuccolarityCmd<T extends RealType<T>> extends ContextCommand implements Previewable {

	private static final String PLUGIN_LABEL            = "Computes 3D Succolarity";
	private static final String SPACE_LABEL             = "";
	private static final String REGRESSION_LABEL        = "<html><b>Regression parameters</b></html>";
	private static final String METHODOPTIONS_LABEL     = "<html><b>Method options</b></html>";
	private static final String BACKGROUNDOPTIONS_LABEL = "<html><b>Background option</b></html>";
	private static final String DISPLAYOPTIONS_LABEL    = "<html><b>Display options</b></html>";
	private static final String PROCESSOPTIONS_LABEL    = "<html><b>Process options</b></html>";

	private static Img<FloatType> volFloat;
	private static Img<UnsignedByteType> volFlood;
	private static RandomAccess<UnsignedByteType> raFlood;
	private static Img<UnsignedByteType> volDil;
	private static RandomAccessibleInterval<?> raiBox;
	private static RandomAccess<UnsignedByteType> ra;
	
	private static Cursor<?> cursor = null;
	private static String datasetName;
	private static long width = 0;
	private static long height = 0;
	private static long depth = 0;
	private static long numDimensions = 0;
	private static long numSlices = 0;
	private static int numVolumes = 0;
	private static long compositeChannelCount = 0;
	private static String imageType = "";
	private static int  numBoxes = 0;
	private static ArrayList<CsajPlot_RegressionFrame> doubleLogPlotList = new ArrayList<CsajPlot_RegressionFrame>();

	public static final String TABLE_OUT_NAME = "Table - 3D Succolarities";
	
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
	
	//@Parameter
	//private DefaultThreadService defaultThreadService;

	// This parameter does not work in an InteractiveCommand plugin
	// -->> (duplicate displayService error during startup) pom-scijava 24.0.0
	// no problem in a Command Plugin
	//@Parameter
	//private DisplayService displayService;

	@Parameter // This works in an InteractiveCommand plugin
	private DefaultDisplayService defaultDisplayService;

	@Parameter
	private DatasetService datasetService;

	@Parameter(label = TABLE_OUT_NAME, type = ItemIO.OUTPUT)
	private DefaultGenericTable tableOut;


	// Widget elements------------------------------------------------------
	//-----------------------------------------------------------------------------------------------------
	//@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	//private final String labelPlugin = PLUGIN_LABEL;

	//@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	//private final String labelSpace = SPACE_LABEL;

	// Input dataset which is updated in callback functions
	@Parameter(type = ItemIO.INPUT)
	private Dataset datasetIn;

	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelRegression = REGRESSION_LABEL;

	@Parameter(label = "Number of boxes",
    		   description = "Number of distinct box sizes following the power of 2",
	       	   style = NumberWidget.SPINNER_STYLE,
	           min = "1",
	           max = "32768",
	           stepSize = "1",
	           persist = false,  //restore previous value default = true
	           initializer = "initialNumBoxes",
	           callback    = "callbackNumBoxes")
    private int spinnerInteger_NumBoxes;
    
//    @Parameter(label = "Regression Start",
//    		   description = "Minimum x value of linear regression",
// 		       style = NumberWidget.SPINNER_STYLE,
// 		       min = "1",
// 		       max = "32768",
// 		       stepSize = "1",
// 		       persist = false,   //restore previous value default = true
// 		       initializer = "initialNumRegStart",
// 		       callback = "callbackNumRegStart")
//    private int spinnerInteger_NumRegStart = 1;
// 
//    @Parameter(label = "Regression End",
//    		   description = "Maximum x value of linear regression",
//    		   style = NumberWidget.SPINNER_STYLE,
//		       min = "3",
//		       max = "32768",
//		       stepSize = "1",
//		       persist = false,   //restore previous value default = true
//		       initializer = "initialNumRegEnd",
//		       callback = "callbackNumRegEnd")
//     private int spinnerInteger_NumRegEnd = 3;

	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelMethod = METHODOPTIONS_LABEL;

    @Parameter(label = "Scanning method",
    		    description = "Type of 3D box scanning",
    		    style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
      		    choices = {"Raster box"},//, "Sliding box", "Tug of war"}, //3D Sliding box is implemented but very long lasting 
      		    persist = true,  //restore previous value default = true
    		    initializer = "initialScanningType",
                callback = "callbackScanningType")
    private String choiceRadioButt_ScanningType;
    
    //-----------------------------------------------------------------------------------------------------
    //@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
    //private final String labelFloodingOptions = FLOODINGOPTIONS_LABEL;
    
    @Parameter(label = "Flooding type",
   		       description = "Type of flooding, e.g. Top to down or Left to right.... or mean of all 6 directions",
   		       style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
     		   choices = {"T2D", "D2T", "L2R", "R2L", "B2F", "F2B", "Mean & Anisotropy"},
     		   persist = true,  //restore previous value default = true
   		       initializer = "initialFloodingType",
               callback = "callbackFloodingType")
    private String choiceRadioButt_FloodingType;

	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelDisplayOptions = DISPLAYOPTIONS_LABEL;

	@Parameter(label = "Show double log plot",
		   	   persist = true, //restore previous value default = true
			   initializer = "initialShowDoubleLogPlots")
	private boolean booleanShowDoubleLogPlot;

	@Parameter(label = "Overwrite result display(s)",
	    	description = "Overwrite already existing result images, plots or tables",
	    	persist = true,  //restore previous value default = true
			initializer = "initialOverwriteDisplays")
	private boolean booleanOverwriteDisplays;

	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelProcessOptions = PROCESSOPTIONS_LABEL;

	@Parameter(label = "Immediate processing", visibility = ItemVisibility.INVISIBLE, persist = false,
	    	description = "Immediate processing of active image when a parameter is changed",
			callback = "callbackProcessImmediately")
	private boolean booleanProcessImmediately;
	
//	@Parameter(label = "OK - process image #", description = "Image slice number", style = NumberWidget.SPINNER_STYLE, min = "1", max = "99999999", stepSize = "1",
//			   persist = false, // restore  previous value  default  =  true
//			   initializer = "initialNumImageSlice",
//			   callback = "callbackNumImageSlice")
//	private int spinnerInteger_NumImageSlice;
	
	@Parameter(label = "    OK - process single volume     ", callback = "callbackProcessSingleVolume")
	private Button buttonProcessSingleVolume;
	
//	Deactivated, because it does not work in Fiji (although it works in ImageJ2 -Eclipse)	
//	@Parameter(label = "Preview of single active image ", callback = "callbackProcessActiveImage")
//	private Button buttonProcessActiveImage;

//	@Parameter(label = "Preview of all available images", callback = "callbackProcessAllImages")
//	private Button buttonProcessAllImages;
	
	// ---------------------------------------------------------------------
		
	protected void initialPluginLaunch() {
		checkItemIOIn();
	}
	
    protected void initialNumBoxes() {
    	if (datasetIn == null) {
    		logService.error(this.getClass().getName() + " ERROR: Input image volume = null");
    		cancel("ComsystanJ 3D plugin cannot be started - missing input image volume.");
    		return;
    	} else {
    		numBoxes = getMaxBoxNumber(datasetIn.dimension(0), datasetIn.dimension(1), datasetIn.dimension(2));
    	}
      	spinnerInteger_NumBoxes = numBoxes;
    }
//    protected void initialNumRegStart() {
//    	spinnerInteger_NumRegStart = 1;
//    }
//    protected void initialNumRegEnd() {
//    	if (datasetIn == null) {
//			logService.error(this.getClass().getName() + " ERROR: Input image volume = null");
//			cancel("ComsystanJ 3D plugin cannot be started - missing input image volume.");
//			return;
//	  	} else {
//			numBoxes = getMaxBoxNumber(datasetIn.dimension(0), datasetIn.dimension(1), datasetIn.dimension(2));
//	  	}
//    	spinnerInteger_NumRegEnd =  numBoxes;
//    }
	
    protected void initialScanningType() {
    	choiceRadioButt_ScanningType = "Raster box";
    }

	protected void initialShowDoubleLogPlots() {
		booleanShowDoubleLogPlot = true;
	}
	protected void initialOverwriteDisplays() {
    	booleanOverwriteDisplays = true;
	}
	
//	protected void initialNumImageSlice() {
//    	spinnerInteger_NumImageSlice = 1;
//	}
	
	// ------------------------------------------------------------------------------
	

	/** Executed whenever the {@link #spinnerInteger_NumBoxes} parameter changes. */
	protected void callbackNumBoxes() {
		
		if  (spinnerInteger_NumBoxes < 3) {
			spinnerInteger_NumBoxes = 3;
		}
		int numMaxBoxes = getMaxBoxNumber(datasetIn.dimension(0), datasetIn.dimension(1), datasetIn.dimension(2));	
		if (spinnerInteger_NumBoxes > numMaxBoxes) {
			spinnerInteger_NumBoxes = numMaxBoxes;
		};
//		if (spinnerInteger_NumRegEnd > spinnerInteger_NumBoxes) {
//			spinnerInteger_NumRegEnd = spinnerInteger_NumBoxes;
//		}
//		if (spinnerInteger_NumRegStart >= spinnerInteger_NumRegEnd - 2) {
//			spinnerInteger_NumRegStart = spinnerInteger_NumRegEnd - 2;
//		}

		numBoxes = spinnerInteger_NumBoxes;
		logService.info(this.getClass().getName() + " Number of boxes set to " + spinnerInteger_NumBoxes);
	}
//    /** Executed whenever the {@link #spinnerInteger_NumRegStart} parameter changes. */
//	protected void callbackNumRegStart() {
//		if (spinnerInteger_NumRegStart >= spinnerInteger_NumRegEnd - 2) {
//			spinnerInteger_NumRegStart = spinnerInteger_NumRegEnd - 2;
//		}
//		if(spinnerInteger_NumRegStart < 1) {
//			spinnerInteger_NumRegStart = 1;
//		}
//		logService.info(this.getClass().getName() + " Regression Min set to " + spinnerInteger_NumRegStart);
//	}
//	/** Executed whenever the {@link #spinnerInteger_NumRegEnd} parameter changes. */
//	protected void callbackNumRegEnd() {
//		if (spinnerInteger_NumRegEnd <= spinnerInteger_NumRegStart + 2) {
//			spinnerInteger_NumRegEnd = spinnerInteger_NumRegStart + 2;
//		}		
//		if (spinnerInteger_NumRegEnd > spinnerInteger_NumBoxes) {
//			spinnerInteger_NumRegEnd = spinnerInteger_NumBoxes;
//		}
//		
//		logService.info(this.getClass().getName() + " Regression Max set to " + spinnerInteger_NumRegEnd);
//	}
	
//	/** Executed whenever the {@link #spinnerInteger_NumAccuracy} parameter changes. */
//	protected void callbackNumAccuracy() {
//		logService.info(this.getClass().getName() + " Accuracy set to " + spinnerInteger_NumAcurracy);
//	}
//	
//	/** Executed whenever the {@link #spinnerInteger_NumConfidence} parameter changes. */
//	protected void callbackNumConfidence() {
//		logService.info(this.getClass().getName() + " Confidence set to " + spinnerInteger_NumConfidence);
//	}
	
	/** Executed whenever the {@link #booleanProcessImmediately} parameter changes. */
	protected void callbackProcessImmediately() {
		logService.info(this.getClass().getName() + " Process immediately set to " + booleanProcessImmediately);
	}
	
//	/** Executed whenever the {@link #spinnerInteger_NumImageSlice} parameter changes. */
//	protected void callbackNumImageSlice() {
//		if (spinnerInteger_NumImageSlice > numSlices){
//			logService.info(this.getClass().getName() + " No more images available");
//			spinnerInteger_NumImageSlice = (int)numSlices;
//		}
//		logService.info(this.getClass().getName() + " Image slice number set to " + spinnerInteger_NumImageSlice);
//	}
	
	/**
	 * Executed whenever the {@link #buttonProcessSingleImage} button is pressed.
	 * It is not executed in the same exact manner such as run()
	 * So a thread for displaying properly the Progressbar window is needed
	 * Execution of the code is then not on the Event Dispatch Thread EDT, where all GUI windows are executed
	 * The @Parameter ItemIO.OUTPUT is not automatically shown 
	 */
	protected void callbackProcessSingleVolume() {
		//prepare  executer service
		exec = Executors.newSingleThreadExecutor();
	   	exec.execute(new Runnable() {
	        public void run() {
	    	    startWorkflowForSingleVolume();
	    	   	uiService.show(TABLE_OUT_NAME, tableOut);
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
	        	startWorkflowForSingleVolume();
	    	   	uiService.show(TABLE_OUT_NAME, tableOut);
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
		    	    startWorkflowForSingleVolume();
		    	   	uiService.show(TABLE_OUT_NAME, tableOut);   //Show table because it did not go over the run() method
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
		startWorkflowForSingleVolume();
	
		logService.info(this.getClass().getName() + " Finished command run");
	}

	public void checkItemIOIn() {

		//Define supported image types for this plugin
		String[] supportedImageTypes = {"Grey"};
		//String[] supportedImageTypes = {"RGB"};
		//String[] supportedImageTypes = {"Grey", "RGB"};
		
		//Check input and get input meta data
		HashMap<String, Object> datasetInInfo = CsajCheck_ItemIn.checkVolumeDatasetIn(logService, datasetIn, supportedImageTypes);
		if (datasetInInfo == null) {
			logService.error(MethodHandles.lookup().lookupClass().getName() + " ERROR: Inital check failed");
			cancel("ComsystanJ 3D plugin cannot be started - Initial check failed.");
		} else {
			width  =       			(long)datasetInInfo.get("width");
			height =       			(long)datasetInInfo.get("height");
			depth  =       			(long)datasetInInfo.get("depth");
			numDimensions =         (int)datasetInInfo.get("numDimensions");
			compositeChannelCount = (int)datasetInInfo.get("compositeChannelCount");
			numSlices =             (long)datasetInInfo.get("numSlices");
			imageType =   			(String)datasetInInfo.get("imageType");
			datasetName = 			(String)datasetInInfo.get("datasetName");
			//sliceLabels = 		(String[])datasetInInfo.get("sliceLabels");
		}
	}

	/**
	* This method starts the workflow for a single image of the active display
	*/
	protected void startWorkflowForSingleVolume() {
	
		dlgProgress = new CsajDialog_WaitingWithProgressBar("Computing 3D Succolarity, please wait... Open console window for further info.",
							logService, false, exec); //isCanceable = false, because no following method listens to exec.shutdown 
		dlgProgress.updatePercent("");
		dlgProgress.setBarIndeterminate(true);
		dlgProgress.setVisible(true);
    
		deleteExistingDisplays();
    	generateTableHeader();
        logService.info(this.getClass().getName() + " Processing volume...");
		processSingleInputVolume();
		
		dlgProgress.addMessage("Processing finished! Collecting data for table...");
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
			
			//Check if Grey or RGB
			if (imageType.equals("Grey")) {
				//do nothing, it is OK
			};
			if (imageType.equals("RGB")) {
				//At first Index runs through RGB channels and then through stack index
				//0... R of first RGB image, 1.. G of first RGB image, 2..B of first RGB image, 3... R of second RGB image, 4...G, 5...B,.......
				activeSliceIndex = (int) Math.floor((float)activeSliceIndex/3.f);
			}
			
			
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
	
	/**
	 * This method deletes already open displays
	 * 
	 */
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
//			//List<Display<?>> list = defaultDisplayService.getDisplays();
//			//for (int i = 0; i < list.size(); i++) {
//			//	display = list.get(i);
//			//	System.out.println("display name: " + display.getName());
//			//	if (display.getName().contains("Name")) display.close(); //does not close correctly in Fiji, it is only not available any more
//			//}			
//			//List<ImageDisplay> listImgs = defaultImageDisplayService.getImageDisplays(); //Is also not closed in Fiji 
//		
//			Frame frame;
//			Frame[] listFrames = JFrame.getFrames();
//			for (int i = listFrames.length -1 ; i >= 0; i--) { //Reverse order, otherwise focus is not given free from the last image
//				frame = listFrames[i];
//				//System.out.println("frame name: " + frame.getTitle());
//				if (frame.getTitle().contains("Name")) {
//					frame.setVisible(false); //Successfully closes also in Fiji
//					frame.dispose();
//				}
//			}
		}
		if (optDeleteExistingPlots) {
//			//This dose not work with DisplayService because the JFrame is not "registered" as an ImageJ display	
			if (doubleLogPlotList != null) {
				for (int l = 0; l < doubleLogPlotList.size(); l++) {
					doubleLogPlotList.get(l).setVisible(false);
					doubleLogPlotList.get(l).dispose();
					// doubleLogPlotList.remove(l); /
				}
				doubleLogPlotList.clear();
			}
//			//ImageJ PlotWindows aren't recognized by DeafultDisplayService!!?
//			List<Display<?>> list = defaultDisplayService.getDisplays();
//			for (int i = 0; i < list.size(); i++) {
//				Display<?> display = list.get(i);
//				System.out.println("display name: " + display.getName());
//				if (display.getName().contains("Grey value profile"))
//					display.close();
//			}
	
		}
		if (optDeleteExistingTables) {
			Display<?> display;
			List<Display<?>> list = defaultDisplayService.getDisplays();
			for (int i = 0; i < list.size(); i++) {
				display = list.get(i);
				//System.out.println("display name: " + display.getName());
				if (display.getName().contains(TABLE_OUT_NAME)) display.close();
			}			
		}
	}


	/** This method computes the maximal number of possible boxes*/
	public static int getMaxBoxNumber(long width, long height, long depth) { 
		float boxWidth = 1f;
		int number = 1; 
		while ((boxWidth <= width) && (boxWidth <= height) && (boxWidth <= depth)) {
			boxWidth = boxWidth * 2;
			number = number + 1;
		}
		return number - 1;
	}

	/** This method takes the active image volume and computes results. 
	 *
	 **/
	private void processSingleInputVolume() {
		
		long startTime = System.currentTimeMillis();
	
		//get rai
		RandomAccessibleInterval<T> rai = null;	
	
		rai =  (RandomAccessibleInterval<T>) datasetIn.getImgPlus(); //dim==3

		// Compute regression parameters
		CsajContainer_ProcessMethod containerPM = process(rai); //rai is 3D

		writeToTable(containerPM);
	
		//Set/Reset focus to DatasetIn display
		//may not work for all Fiji/ImageJ2 versions or operating systems
		Frame frame;
		Frame[] listFrames = JFrame.getFrames();
		for (int i = 0; i < listFrames.length; i++) {
			frame = listFrames[i];
			//System.out.println("frame name: " + frame.getTitle());
			if (frame.getTitle().contains(datasetIn.getName())) { //sometimes Fiji adds some characters to the frame title such as "(V)"
				frame.setVisible(true);
				frame.toFront();
				frame.requestFocus();
			}
		}
		
		long duration = System.currentTimeMillis() - startTime;
		TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
		SimpleDateFormat sdf = new SimpleDateFormat();
		sdf.applyPattern("HHH:mm:ss:SSS");
		logService.info(this.getClass().getName() + " Elapsed time: "+ sdf.format(duration));
	}

	

	/** Generates the table header {@code DefaultGenericTable} */
	private void generateTableHeader() {
		
		numBoxes = this.spinnerInteger_NumBoxes; 
		
		GenericColumn columnFileName       = new GenericColumn("File name");
		IntColumn columnMaxNumBoxes        = new IntColumn("# Boxes");;
		//GenericColumn columnNumRegStart   = new GenericColumn("Reg Start");
		//GenericColumn columnNumRegEnd     = new GenericColumn("Reg End");
		GenericColumn columnScanningType   = new GenericColumn("Scanning type");
		GenericColumn columnFloodType      = new GenericColumn("Flooding type");
		DoubleColumn columnPotSucc         = new DoubleColumn("Succ reservoir");

		tableOut = new DefaultGenericTable();
		tableOut.add(columnFileName);
		tableOut.add(columnMaxNumBoxes);
		//tableOut.add(columnNumRegStart);
		//tableOut.add(columnNumRegEnd);
		tableOut.add(columnScanningType);
		tableOut.add(columnFloodType);
		tableOut.add(columnPotSucc);	
		String preString = "Succ";
		for (int i = 0; i < numBoxes; i++) {
			tableOut.add(new DoubleColumn(preString + "-" + (int)Math.pow(2,i) + "x" + (int)Math.pow(2, i)));
		}
		preString = "Delta succ";
		for (int i = 0; i < numBoxes; i++) {
			tableOut.add(new DoubleColumn(preString + "-" + (int)Math.pow(2,i) + "x" + (int)Math.pow(2, i)));
		}
		if (choiceRadioButt_FloodingType.equals("Mean & Anisotropy")) {
			preString = "Anisotropy";
			for (int i = 0; i < numBoxes; i++) {
				tableOut.add(new DoubleColumn(preString + "-" + (int)Math.pow(2,i) + "x" + (int)Math.pow(2, i)));
			}	
		}

	}

	/**
	 * collects current result and writes to table
	 * 
	 * @param CsajContainer_ProcessMethod containerPM
	 */
	private void writeToTable(CsajContainer_ProcessMethod containerPM) { 

		int numBoxes = spinnerInteger_NumBoxes;
		//int numRegStart  = spinnerInteger_NumRegStart;
		//int numRegEnd  = spinnerInteger_NumRegEnd;
		String scanningType   = choiceRadioButt_ScanningType;  //Raster box     Sliding box
		String floodingType = choiceRadioButt_FloodingType;
		
		int tableColStart = 0;
		int tableColEnd   = 0;
		int tableColLast  = 0;
		
		int row = 0;
		// fill table with values
		tableOut.appendRow();
		tableOut.set("File name",		row, datasetName);	
		tableOut.set("# Boxes",			row, numBoxes);
		//tableOut.set("Reg Start",       row, "("+numRegStart+")" + epsRegStartEnd[0]); //(NumRegStart)epsRegStart
		//tableOut.set("Reg End",      	row, "("+numRegEnd+")"   + epsRegStartEnd[1]); //(NumRegEnd)epsRegEnd

		tableOut.set("Scanning type",   row, scanningType);
		tableOut.set("Flooding type", row, floodingType);
		tableOut.set("Succ reservoir",row, containerPM.item1_Values[0]); //Succolarity reservoir (= potential succolarity)
		tableColLast = 4;
		
		int numParameters = containerPM.item1_Values.length - 1; //-1 because succolarity reservoir (= potential succolarity) is already set to table
		tableColStart = tableColLast + 1;
		tableColEnd = tableColStart + numParameters;
		for (int c = tableColStart; c < tableColEnd; c++ ) {
			tableOut.set(c, row, containerPM.item1_Values[c-tableColStart + 1]); //+1 because first entry is succolarity reservoir (= potential succolarity)
		}	
	}

	/**
	*
	* Processing
	*/
	private CsajContainer_ProcessMethod process(RandomAccessibleInterval<?> rai) { //3Dvolume
	
		if (rai == null) {
			logService.info(this.getClass().getName() + " WARNING: rai==null, no image for processing!");
		}
		
		//int numRegStart      = spinnerInteger_NumRegStart;
		//int numRegEnd        = spinnerInteger_NumRegEnd;
		numBoxes              = spinnerInteger_NumBoxes;
		String scanningType   = choiceRadioButt_ScanningType;    //Raster box     Sliding box not implemented yet
		String floodingType   = choiceRadioButt_FloodingType;	
		boolean optShowPlot   = booleanShowDoubleLogPlot;

		//double[] epsRegStartEnd   = new double[2];  // epsRegStart, epsRegEnd
	
		//Get succolarity reservoir (= potential succolarity)
		//Succolarity reservoir (= potential succolarity) is identical for each flooding direction
		double succReservoir = computeSuccReservoir(rai);
		
		double[] succolaritiesT2D      = null;
		double[] succolaritiesD2T      = null;
		double[] succolaritiesL2R      = null;
		double[] succolaritiesR2L      = null;
		double[] succolaritiesB2F      = null;
		double[] succolaritiesF2B      = null;
		double[] succolarities         = null;
		double[] succolaritiesExtended = null; //With added succolarities such as Succolarity reservoir  , Delta succolarities, Anisotropy
		double[] anisotropyIndices     = null;
			
		//********************************Binary Image: 0 and [1, 255]! and not: 0 and 255	
		if (floodingType.equals("T2D")) {
			//get and initialize T2D flooded image
			initializeImgFlood_T2D(rai);
			floodingVolFlood();
			succolarities = computeSuccolarities_T2D();	
		}
		else if (floodingType.equals("D2T")) {
			initializeImgFlood_D2T(rai);
			floodingVolFlood();
			succolarities = computeSuccolarities_D2T();	
		}
		else if (floodingType.equals("L2R")) {
			initializeImgFlood_L2R(rai);
			floodingVolFlood();
			succolarities = computeSuccolarities_L2R();	
		}
		else if (floodingType.equals("R2L")) {
			initializeImgFlood_R2L(rai);
			floodingVolFlood();
			succolarities = computeSuccolarities_R2L();	
		}
		else if (floodingType.equals("B2F")) {
			initializeImgFlood_B2F(rai);
			floodingVolFlood();
			succolarities = computeSuccolarities_B2F();	
		}
		else if (floodingType.equals("F2B")) {
			initializeImgFlood_F2B(rai);
			floodingVolFlood();
			succolarities = computeSuccolarities_F2B();	
		}
		
		else if (floodingType.equals("Mean & Anisotropy")) {
			initializeImgFlood_T2D(rai);
			floodingVolFlood();
			succolaritiesT2D = computeSuccolarities_T2D();	
			
			initializeImgFlood_D2T(rai);
			floodingVolFlood();
			succolaritiesD2T = computeSuccolarities_D2T();	
		
			initializeImgFlood_L2R(rai);
			floodingVolFlood();
			succolaritiesL2R = computeSuccolarities_L2R();	
			
			initializeImgFlood_R2L(rai);
			floodingVolFlood();
			succolaritiesR2L = computeSuccolarities_R2L();	
			
			initializeImgFlood_B2F(rai);
			floodingVolFlood();
			succolaritiesB2F = computeSuccolarities_B2F();	
			
			initializeImgFlood_F2B(rai);
			floodingVolFlood();
			succolaritiesF2B = computeSuccolarities_F2B();	
			
			succolarities = new double[numBoxes];
			for (int s = 0; s < numBoxes; s++) {
				succolarities[s] = (succolaritiesT2D[s] + succolaritiesD2T[s] + succolaritiesL2R[s] + succolaritiesR2L[s] + succolaritiesB2F[s] + succolaritiesF2B[s])/6.0;
			}
			anisotropyIndices = new double[numBoxes];
			//Fractional anisotropy
			//https://en.wikipedia.org/wiki/Fractional_anisotropy
			//A value of zero means that diffusion is isotropic, i.e. it is unrestricted (or equally restricted) in all directions.
			//A value of one means that diffusion occurs only along one axis and is fully restricted along all other directions.
			double eVx; //eigenvalue X
			double eVy; //eigenvalue Y
			double eVz; //eigenvalue Z
			double eV1; //Largest eigenvalue
			double eV2; //Second largest eigenvalue
			double eV3; //.....
			double eVMean;
			double FA1; //Fractional anisotropy
			double FA2; //Fractional anisotropy

			for (int s = 0; s < numBoxes; s++) {
				eVx = (succolaritiesL2R[s]+succolaritiesR2L[s])/2.0;
				eVy = (succolaritiesT2D[s]+succolaritiesD2T[s])/2.0;
				eVz = (succolaritiesB2F[s]+succolaritiesF2B[s])/2.0;
			
				double[] eVArray = {eVx, eVy, eVz};
				Arrays.sort(eVArray); //Should not be necessary, at least for FA2
			
				eV1 = eVArray[2]; //largest eigenvalue
				eV2 = eVArray[1];
				eV3 = eVArray[0]; //smallest eigenvalue 
				eVMean = (eV1+eV2+eV3)/3.0;
				
				FA1 = Math.sqrt(3.0/2.0*((Math.pow(eV1-eVMean, 2) + Math.pow(eV2-eVMean, 2) + Math.pow(eV3-eVMean, 2))/(eV1*eV1+eV2*eV2+eV3*eV3)));
				//or equivalently
				FA2 = Math.sqrt(1.0/2.0*((Math.pow(eV1-eV2, 2) + Math.pow(eV2-eV3, 2) + Math.pow(eV3-eV1, 2))/(eV1*eV1+eV2*eV2+eV3*eV3)));
				
				anisotropyIndices[s] = FA1;
			}
		}
			
		//Create double log plot
		boolean isLineVisible = false; //?
			
		// Plot //nur ein Band!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		double[] lnDataX = new double[numBoxes];
		double[] lnDataY = new double[numBoxes];
			
		double succ;
		for (int k = 0; k < numBoxes; k++) {
			if (succolarities[k] == 0) {
				succ = Double.MIN_VALUE;
			} else  {
				succ = succolarities[k];
			};
			lnDataY[k] = Math.log(100.0*succ);		
			lnDataX[k] = Math.log(Math.pow(2, k)); //box size	
		}
		// System.out.println("FractalDimensionBoxCounting: dataY: "+ dataY);
		// System.out.println("FractalDimensionBoxCounting: dataX: "+ dataX);
	
		if (optShowPlot) {			
			String preName = "";
			CsajPlot_RegressionFrame doubleLogPlot = DisplayRegressionPlotXY(lnDataX, lnDataY, isLineVisible,"Double log plot - Succolarity", 
					preName + datasetName, "ln(Box width)", "ln(100.Succolarity)", "",
					1, numBoxes);
			doubleLogPlotList.add(doubleLogPlot);
		}
		
		// Compute regression
		//LinearRegression lr = new LinearRegression();
		//regressionParams = lr.calculateParameters(lnDataX, lnDataY, numRegStart, numRegEnd);
		//0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
		
		
		if (floodingType.equals("Mean & Anisotropy")) {
			succolaritiesExtended = new double[1+2*succolarities.length+anisotropyIndices.length]; //Succolarity reservoir (= potential succolarity), Succolarities and Delta succolarities
		} else {
			succolaritiesExtended = new double[1+2*succolarities.length]; //Succolarity reservoir (= potential succolarity), Succolarities and Delta succolarities
		}
		
		succolaritiesExtended[0] = succReservoir;
		
		for (int s = 0; s < succolarities.length; s++) {
			succolaritiesExtended[1+s] = succolarities[s]; //Succolarities
		}
		for (int s = 0; s < succolarities.length; s++) {
			succolaritiesExtended[1+succolarities.length + s] = succReservoir - succolarities[s]; //Delta succolarities
		}
		if (floodingType.equals("Mean & Anisotropy")) {
			for (int s = 0; s < anisotropyIndices.length; s++) {
				succolaritiesExtended[1+2*succolarities.length + s] = anisotropyIndices[s]; //Anisotropy indices
			}
		}
		
		logService.info(this.getClass().getName() + " Succolarities[0]: " + succolaritiesExtended[0]);
		
//		epsRegStartEnd[0] = eps[numRegStart-1];
//		epsRegStartEnd[1] = eps[numRegEnd-1];
		
		return new CsajContainer_ProcessMethod(succolaritiesExtended);
		
		// Output
		// uiService.show(TABLE_OUT_NAME, table);
	}
	
	/**
	 * This method computes the Succolarity reservoir (= potential succolarity)
	 * Andronache, Ion. „Analysis of Forest Fragmentation and Connectivity Using Fractal Dimension and Succolarity“.
	 * Land 13, Nr. 2 (Februar 2024): 138.
	 * https://doi.org/10.3390/land13020138.

	 * @param rai
	 */
	private double computeSuccReservoir(RandomAccessibleInterval<?> rai) { 
	
		double numBlackPixels = 0;
		double numTotalPixels = 0;
		
		cursor = Views.iterable(rai).localizingCursor();
		while (cursor.hasNext()) {
			cursor.fwd();				
			if (((UnsignedByteType) cursor.get()).get() == 0) {
				numBlackPixels += 1.0; // Binary Image: 0 and [1, 255]! and not: 0 and 255
			}			
			numTotalPixels += 1.0;
		}
		return numBlackPixels/numTotalPixels; //Succolarity reservoir (= potential succolarity)
	}

	/**
	 * This method generates an image volume for flooding with set Top pixels
	 * @param rai
	 */
	//get and initialize T2D flooded volume
	private void initializeImgFlood_T2D(RandomAccessibleInterval<?> rai) { 
		int value = 0;
		volFlood = new ArrayImgFactory<>(new UnsignedByteType()).create(width, height, depth); //always 3D
		raFlood = volFlood.randomAccess();
		ra = (RandomAccess<UnsignedByteType>) rai.randomAccess();
		ra.setPosition(0, 1);     //y=Top;
		raFlood.setPosition(0, 1);//y=Top;
		for (int x=0; x < width; x++) {
			ra.setPosition(x, 0);
			for (int z=0; z < depth; z++) {	
				ra.setPosition(z, 2);
				value = ra.get().getInteger();
				if (value == 0) {
					raFlood.setPosition(x, 0);
					raFlood.setPosition(z, 2);
					raFlood.get().set(255);
				}
			}
		}
	}
	
	/**
	 * This method generates an image volume for flooding with set Bottom pixels
	 * @param rai
	 */
	//get and initialize D2T flooded image
	private void initializeImgFlood_D2T(RandomAccessibleInterval<?> rai) { 
		int value = 0;
		volFlood = new ArrayImgFactory<>(new UnsignedByteType()).create(width, height, depth); //always 3D
		raFlood = volFlood.randomAccess();
		ra = (RandomAccess<UnsignedByteType>) rai.randomAccess();
		ra.setPosition(height-1, 1);     //y=Bottom;
		raFlood.setPosition(height-1, 1);//y=Bottom;
		for (int x=0; x < width; x++) {
			ra.setPosition(x, 0);
			for (int z=0; z < depth; z++) {	
				ra.setPosition(z, 2);
				value = ra.get().getInteger();
				if (value == 0) {
					raFlood.setPosition(x, 0);
					raFlood.setPosition(z, 2);
					raFlood.get().set(255);
				}
			}
		}
	}
	
	/**
	 * This method generates an image volume for flooding with set Left pixels
	 * @param rai
	 */
	//get and initialize L2R flooded volume
	private void initializeImgFlood_L2R(RandomAccessibleInterval<?> rai) { 
		int value = 0;
		volFlood = new ArrayImgFactory<>(new UnsignedByteType()).create(width, height, depth); //always 3D
		raFlood = volFlood.randomAccess();
		ra = (RandomAccess<UnsignedByteType>) rai.randomAccess();
		ra.setPosition(0, 0);     //x=Left;
		raFlood.setPosition(0, 0);//x=Left;
		for (int y=0; y < height; y++) {
			ra.setPosition(y, 1);
			for (int z=0; z < depth; z++) {	
				ra.setPosition(z, 2);
				value = ra.get().getInteger();
				if (value == 0) {
					raFlood.setPosition(y, 1);
					raFlood.setPosition(z, 2);
					raFlood.get().set(255);
				}
			}
		}
	}
	
	/**
	 * This method generates an image volume for flooding with set Right pixels
	 * @param rai
	 */
	//get and initialize R2L flooded volume
	private void initializeImgFlood_R2L(RandomAccessibleInterval<?> rai) { 
			int value = 0;
			volFlood = new ArrayImgFactory<>(new UnsignedByteType()).create(width, height, depth); //always 3D
			raFlood = volFlood.randomAccess();
			ra = (RandomAccess<UnsignedByteType>) rai.randomAccess();
			ra.setPosition(width-1, 0);     //x=Right;
			raFlood.setPosition(width-1, 0);//x=Right;
			for (int y=0; y < height; y++) {
				ra.setPosition(y, 1);
				for (int z=0; z < depth; z++) {	
					ra.setPosition(z, 2);
					value = ra.get().getInteger();
					if (value == 0) {
						raFlood.setPosition(y, 1);
						raFlood.setPosition(z, 2);
						raFlood.get().set(255);
					}
				}
			}
		}
	
	/**
	 * This method generates an image volume for flooding with set Back pixels
	 * @param rai
	 */
	//get and initialize B2F flooded volume
	private void initializeImgFlood_B2F(RandomAccessibleInterval<?> rai) { 
		int value = 0;
		volFlood = new ArrayImgFactory<>(new UnsignedByteType()).create(width, height, depth); //always 3D
		raFlood = volFlood.randomAccess();
		ra = (RandomAccess<UnsignedByteType>) rai.randomAccess();
		ra.setPosition(0, 2);     //z=Back;
		raFlood.setPosition(0, 2);//z=Back;
		for (int x=0; x < width; x++) {
			ra.setPosition(x, 0);
			for (int y=0; y < height; y++) {	
				ra.setPosition(y, 1);
				value = ra.get().getInteger();
				if (value == 0) {
					raFlood.setPosition(x, 0);
					raFlood.setPosition(y, 1);
					raFlood.get().set(255);
				}
			}
		}
	}
	
	/**
	 * This method generates an image volume for flooding with set Front pixels
	 * @param rai
	 */
	//get and initialize F2B flooded image
	private void initializeImgFlood_F2B(RandomAccessibleInterval<?> rai) { 
		int value = 0;
		volFlood = new ArrayImgFactory<>(new UnsignedByteType()).create(width, height, depth); //always 3D
		raFlood = volFlood.randomAccess();
		ra = (RandomAccess<UnsignedByteType>) rai.randomAccess();
		ra.setPosition(depth-1, 2);     //z=Bottom;
		raFlood.setPosition(depth-1, 2);//z=Bottom;
		for (int x=0; x < width; x++) {
			ra.setPosition(x, 0);
			for (int y=0; y < height; y++) {	
				ra.setPosition(y, 1);
				value = ra.get().getInteger();
				if (value == 0) {
					raFlood.setPosition(x, 0);
					raFlood.setPosition(y, 1);
					raFlood.get().set(255);
				}
			}
		}
	}
	
	
	/**
	 * This methods floods the image by subsequent dilations and subtractions of the original image volume
	 */
	//flooding volFlood
	private void floodingVolFlood() {
		RectangleShape kernel = new RectangleShape(1, false); //3x3kernel skipCenter = false
		Runtime runtime = Runtime.getRuntime();
		long maxMemory = runtime.maxMemory();
		long totalMemory = runtime.totalMemory();
		long freeMemory = runtime.freeMemory();
		int availableProcessors = runtime.availableProcessors();
		//System.out.println("availabel processors: " + availableProcessors);
		
		int numThreads = 6; //For dilation //with 6 it was 3 times faster than only with one thread
		if (numThreads > availableProcessors) numThreads = availableProcessors;
		
		int diff = 99999999;
		int pixelValueImgOrig;
		int pixelValueImgFlood;
		int pixelValueImgDil;
	
		long[] pos = new long[3];
		while (diff != 0) {
			//Compute dilated image
			volDil = Dilation.dilate(volFlood, kernel, numThreads);
			//subtract original image
			cursor = volDil.localizingCursor();
			while (cursor.hasNext()) {
				cursor.fwd();
				cursor.localize(pos);
				ra.setPosition(pos);
				pixelValueImgDil = ((UnsignedByteType) cursor.get()).get();
				pixelValueImgOrig = ra.get().getInteger();
				// Binary Image: 0 and [1, 255]! and not: 0 and 255
				if ((pixelValueImgDil > 0) && (pixelValueImgOrig > 0)) {
					((UnsignedByteType) cursor.get()).setReal(0);					
				}
			}
			//get diff
			diff = 0;
			cursor = volDil.localizingCursor();
			while (cursor.hasNext()) {
				cursor.fwd();
				cursor.localize(pos);
				raFlood.setPosition(pos);
				pixelValueImgDil = ((UnsignedByteType) cursor.get()).get();
				pixelValueImgFlood = raFlood.get().getInteger();
				// Binary Image: 0 and [1, 255]! and not: 0 and 255
				if ((pixelValueImgDil > 0) && (pixelValueImgFlood == 0)) {
					diff = diff + 1;
					break; //it is not necessary to search any more
				} else if ((pixelValueImgDil == 0) && (pixelValueImgFlood > 0)) {
					diff = diff + 1; //it is not necessary to search any more
					break;
				}	
			}		
			volFlood = volDil;
			//uiService.show("imgFlood", volFlood);
			raFlood = volFlood.randomAccess();
		}
	}
	
	//Following four methods to compute succolarities are quite identical
	//Pressure is the only variable which is differently computed
	
	/**
	 * This method computes succolarities for distinct box sizes from Top two down
	 * @return
	 */
	private double[] computeSuccolarities_T2D() {
		double pressure = Double.NaN; //Pressure of a box 
		double occ= Double.NaN; //occupation percentage in a single box
		double[] succ = new double[numBoxes];
		double[] norm = new double[numBoxes];
		for (int c = 0; c < succ.length; c++) succ[c] = Double.NaN;
		for (int n = 0; n < norm.length; n++) norm[n] = Double.NaN;
		int boxSize;	
		int delta = 0;
		
//		//extra for k = 0 did not be really be faster
//		//k = 0 box size == 1
//		boxSize = (int) Math.pow(2, 0); //= 1;
//		//boxSize = k+1;	
//		numOfScannedBoxes = (int) (width*height);
//		pressure = 0.0;
//		maxPR = (double)(height - 1 + height)/2.0;
//		long[] pos = new long[2];
//		cursor = imgFlood.localizingCursor();
//		while (cursor.hasNext()) { //imgFlood
//			cursor.fwd();	
//			if (((UnsignedByteType) cursor.get()).get() > 0) {
//				cursor.localize(pos); 
//				// Binary Image: 0 and [1, 255]! and not: 0 and 255
//				pressure = (double)(pos[1] + pos[1] + 1.0)/2.0;
//				succ[0] = succ[0] + pressure;     //(occ/(boxSize*boxSize)*pressure); /boxSize == 1   occ == 1 
//			}								
//		}//while imgFlood
//		//Normalization
//		succ[0] = succ[0]/((double)numOfScannedBoxes*1*maxPR);
		
		//all box sizes
		for (int k = 0; k < numBoxes; k++) { //	
			succ[k] = 0.0;
			norm[k] = 0.0;
			boxSize = (int) Math.pow(2, k);		
			if      (choiceRadioButt_ScanningType.equals("Sliding box")) delta = 1;
			else if (choiceRadioButt_ScanningType.equals("Raster box"))  delta = boxSize;	
			pressure = 0.0;
			for (int y = 0;  y<= (height-boxSize); y=y+delta){
				//Pressure is the only variable which is distinct between these 4 methods
				pressure = ((double)y + (double)y + (double)boxSize)/2.0;
				for (int x = 0; x <= (width-boxSize); x=x+delta){
					for (int z = 0; z <= (depth-boxSize); z=z+delta){
						raiBox = Views.interval(volFlood, new long[]{x, y, z}, new long[]{x+boxSize-1, y+boxSize-1, z+boxSize-1});
						occ = 0;
						// Loop through all pixels of this box.
						cursor = Views.iterable(raiBox).localizingCursor();
						while (cursor.hasNext()) { //Box
							cursor.fwd();
							//cursorF.localize(pos); 		
							if (((UnsignedByteType) cursor.get()).get() > 0) {
								// Binary Image: 0 and [1, 255]! and not: 0 and 255
								occ = occ + 1.0;
							}								
						}//while Box
						succ[k] = succ[k] + (occ/(boxSize*boxSize*boxSize)*pressure);
						norm[k] = norm[k] + pressure; //1*pressure //so for a totally filled box (image)
					}//z
				} //y	
			} //x
			//Normalization
			succ[k] = succ[k]/norm[k];
		} //K
		return succ;
	}
	
	/**
	 * This method computes succolarities for distinct box sizes from Down two top
	 * @return
	 */
	private double[] computeSuccolarities_D2T() {
		double pressure = Double.NaN; //Pressure of a box 
		double occ= Double.NaN; //occupation percentage in a single box
		double[] succ = new double[numBoxes];
		double[] norm = new double[numBoxes];
		for (int c = 0; c < succ.length; c++) succ[c] = Double.NaN;
		for (int n = 0; n < norm.length; n++) norm[n] = Double.NaN;
		int boxSize;	
		int delta = 0;
		
		//all box sizes
		for (int k = 0; k < numBoxes; k++) { //	
			succ[k] = 0.0;
			norm[k] = 0.0;
			boxSize = (int) Math.pow(2, k);		
			if      (choiceRadioButt_ScanningType.equals("Sliding box")) delta = 1;
			else if (choiceRadioButt_ScanningType.equals("Raster box"))  delta = boxSize;
			pressure = 0.0;
			for (int y = (int)height-1;  y>= boxSize-1; y=y-delta){
				//Pressure is the only variable which is distinct between these 4 methods
				pressure = ((double)(height-1-y) + (double)(height-1-y + boxSize))/2.0;
				for (int x = 0; x <= (width-boxSize); x=x+delta){
					for (int z = 0; z <= (depth-boxSize); z=z+delta){
						raiBox = Views.interval(volFlood, new long[]{x, y-boxSize+1, z}, new long[]{x+boxSize-1, y, z+boxSize-1});
						occ = 0.0;
						// Loop through all pixels of this box.
						cursor = Views.iterable(raiBox).localizingCursor();
						while (cursor.hasNext()) { //Box
							cursor.fwd();
							//cursorF.localize(pos); 		
							if (((UnsignedByteType) cursor.get()).get() > 0) {
								// Binary Image: 0 and [1, 255]! and not: 0 and 255
								occ = occ + 1.0;
							}								
						}//while Box
						succ[k] = succ[k] + (occ/(boxSize*boxSize*boxSize)*pressure);
						norm[k] = norm[k] + pressure; //1*pressure //so for a totally filled box (image)
					}//z
				} //y	
			} //x
			//Normalization
			succ[k] = succ[k]/norm[k];
		} //k
		return succ;
	}
	
	/**
	 * This method computes succolarities for distinct box sizes from Left to right
	 * @return
	 */
	private double[] computeSuccolarities_L2R() {
		double pressure = Double.NaN; //Pressure of a box 
		double occ= Double.NaN; //occupation percentage in a single box
		double[] succ = new double[numBoxes];
		double[] norm = new double[numBoxes];
		for (int c = 0; c < succ.length; c++) succ[c] = Double.NaN;
		for (int n = 0; n < norm.length; n++) norm[n] = Double.NaN;
		int boxSize;	
		int delta = 0;
		//all box sizes
		for (int k = 0; k < numBoxes; k++) { //	
			succ[k] = 0.0;
			norm[k] = 0.0;
			boxSize = (int) Math.pow(2, k);		
			if      (choiceRadioButt_ScanningType.equals("Sliding box")) delta = 1;
			else if (choiceRadioButt_ScanningType.equals("Raster box"))  delta = boxSize;
			pressure = 0.0;
			for (int x = 0; x <= (width-boxSize); x=x+delta){
				//Pressure is the only variable which is distinct between these 4 methods
				pressure = ((double)x + (double)x + (double)boxSize)/2.0;
				for (int y = 0;  y<= (height-boxSize); y=y+delta){
					for (int z = 0; z <= (depth-boxSize); z=z+delta){
						raiBox = Views.interval(volFlood, new long[]{x, y, z}, new long[]{x+boxSize-1, y+boxSize-1, z+boxSize-1});
						occ = 0.0;
						// Loop through all pixels of this box.
						cursor = Views.iterable(raiBox).localizingCursor();
						while (cursor.hasNext()) { //Box
							cursor.fwd();
							//cursorF.localize(pos); 		
							if (((UnsignedByteType) cursor.get()).get() > 0) {
								// Binary Image: 0 and [1, 255]! and not: 0 and 255
								occ = occ + 1.0;
							}								
						}//while Box
						succ[k] = succ[k] + (occ/(boxSize*boxSize*boxSize)*pressure);
						norm[k] = norm[k] + pressure; //1*pressure //so for a totally filled box (image)
					}//z
				} //y	
			} //x
			//Normalization
			succ[k] = succ[k]/norm[k];
		} //k
		return succ;
	}
	
	/**
	 * This method computes succolarities for distinct box sizes from Right to left
	 * @return
	 */
	private double[] computeSuccolarities_R2L() {
		double pressure = Double.NaN; //Pressure of a box 
		double occ= Double.NaN; //occupation percentage in a single box
		double[] succ = new double[numBoxes];
		double[] norm = new double[numBoxes];
		for (int c = 0; c < succ.length; c++) succ[c] = Double.NaN;
		for (int n = 0; n < norm.length; n++) norm[n] = Double.NaN;
		int boxSize;
		int delta = 0;

		//all box sizes
		for (int k = 0; k < numBoxes; k++) { //	
			succ[k] = 0.0;
			norm[k] = 0.0;
			boxSize = (int) Math.pow(2, k);		
			if      (choiceRadioButt_ScanningType.equals("Sliding box")) delta = 1;
			else if (choiceRadioButt_ScanningType.equals("Raster box"))  delta = boxSize;
			pressure = 0.0;
			for (int x = (int)width-1; x >= boxSize-1; x=x-delta){
				//Pressure is the only variable which is distinct between these 4 methods
				pressure = ((double)(width-1-x) + (double)(width-1-x + boxSize))/2.0; 
				for (int y = 0;  y<= (height-boxSize); y=y+delta){
					for (int z = 0; z <= (depth-boxSize); z=z+delta){
						raiBox = Views.interval(volFlood, new long[]{x-boxSize+1, y, z}, new long[]{x, y+boxSize-1, z+boxSize-1});
						occ = 0.0;
						// Loop through all pixels of this box.
						cursor = Views.iterable(raiBox).localizingCursor();
						while (cursor.hasNext()) { //Box
							cursor.fwd();
							//cursorF.localize(pos); 		
							if (((UnsignedByteType) cursor.get()).get() > 0) {
								// Binary Image: 0 and [1, 255]! and not: 0 and 255
								occ = occ + 1.0;
							}								
						}//while Box
						succ[k] = succ[k] + (occ/(boxSize*boxSize*boxSize)*pressure);
						norm[k] = norm[k] + pressure; //1*pressure //so for a totally filled box (image)
					}//z
				} //y	
			} //x
			//Normalization
			succ[k] = succ[k]/norm[k];
		} //k
		return succ;
	}

	/**
	 * This method computes succolarities for distinct box sizes from Back two Front
	 * @return
	 */
	private double[] computeSuccolarities_B2F() {
		double pressure = Double.NaN; //Pressure of a box 
		double occ= Double.NaN; //occupation percentage in a single box
		double[] succ = new double[numBoxes];
		double[] norm = new double[numBoxes];
		for (int c = 0; c < succ.length; c++) succ[c] = Double.NaN;
		for (int n = 0; n < norm.length; n++) norm[n] = Double.NaN;
		int boxSize;	
		int delta = 0;
	
		//all box sizes
		for (int k = 0; k < numBoxes; k++) { //	
			succ[k] = 0.0;
			norm[k] = 0.0;
			boxSize = (int) Math.pow(2, k);		
			if      (choiceRadioButt_ScanningType.equals("Sliding box")) delta = 1;
			else if (choiceRadioButt_ScanningType.equals("Raster box"))  delta = boxSize;	
			pressure = 0.0;
			for (int z = 0;  z<= (depth-boxSize); z=z+delta){
				//Pressure is the only variable which is distinct between these 4 methods
				pressure = ((double)z + (double)z + (double)boxSize)/2.0;
				for (int x = 0; x <= (width-boxSize); x=x+delta){
					for (int y = 0; y <= (height-boxSize); y=y+delta){
						raiBox = Views.interval(volFlood, new long[]{x, y, z}, new long[]{x+boxSize-1, y+boxSize-1, z+boxSize-1});
						occ = 0;
						// Loop through all pixels of this box.
						cursor = Views.iterable(raiBox).localizingCursor();
						while (cursor.hasNext()) { //Box
							cursor.fwd();
							//cursorF.localize(pos); 		
							if (((UnsignedByteType) cursor.get()).get() > 0) {
								// Binary Image: 0 and [1, 255]! and not: 0 and 255
								occ = occ + 1.0;
							}								
						}//while Box
						succ[k] = succ[k] + (occ/(boxSize*boxSize*boxSize)*pressure);
						norm[k] = norm[k] + pressure; //1*pressure //so for a totally filled box (image)
					}//z
				} //y	
			} //x
			//Normalization
			succ[k] = succ[k]/norm[k];
		} //K
		return succ;
	}
	
	/**
	 * This method computes succolarities for distinct box sizes from Front two Back
	 * @return
	 */
	private double[] computeSuccolarities_F2B() {
		double pressure = Double.NaN; //Pressure of a box 
		double occ= Double.NaN; //occupation percentage in a single box
		double[] succ = new double[numBoxes];
		double[] norm = new double[numBoxes];
		for (int c = 0; c < succ.length; c++) succ[c] = Double.NaN;
		for (int n = 0; n < norm.length; n++) norm[n] = Double.NaN;
		int boxSize;	
		int delta = 0;
		
		//all box sizes
		for (int k = 0; k < numBoxes; k++) { //	
			succ[k] = 0.0;
			norm[k] = 0.0;
			boxSize = (int) Math.pow(2, k);		
			if      (choiceRadioButt_ScanningType.equals("Sliding box")) delta = 1;
			else if (choiceRadioButt_ScanningType.equals("Raster box"))  delta = boxSize;
			pressure = 0.0;
			for (int z = (int)depth-1;  z>= boxSize-1; z=z-delta){
				//Pressure is the only variable which is distinct between these 4 methods
				pressure = ((double)(depth-1-z) + (double)(depth-1-z + boxSize))/2.0;
				for (int x = 0; x <= (width-boxSize); x=x+delta){
					for (int y = 0; y <= (height-boxSize); y=y+delta){
						raiBox = Views.interval(volFlood, new long[]{x, y, z-boxSize+1}, new long[]{x+boxSize-1, y+boxSize-1, z});
						occ = 0.0;
						// Loop through all pixels of this box.
						cursor = Views.iterable(raiBox).localizingCursor();
						while (cursor.hasNext()) { //Box
							cursor.fwd();
							//cursorF.localize(pos); 		
							if (((UnsignedByteType) cursor.get()).get() > 0) {
								// Binary Image: 0 and [1, 255]! and not: 0 and 255
								occ = occ + 1.0;
							}								
						}//while Box
						succ[k] = succ[k] + (occ/(boxSize*boxSize*boxSize)*pressure);
						norm[k] = norm[k] + pressure; //1*pressure //so for a totally filled box (image)
					}//z
				} //y	
			} //x
			//Normalization
			succ[k] = succ[k]/norm[k];
		} //k
		return succ;
	}
	
	
	// This method shows the double log plot
	private void showPlot(double[] lnDataX, double[] lnDataY, String preName, int numRegStart, int numRegEnd) {
		if (imageType.equals("Grey")) {
			if (lnDataX == null) {
				logService.info(this.getClass().getName() + " lnDataX == null, cannot display the plot!");
				return;
			}
			if (lnDataY == null) {
				logService.info(this.getClass().getName() + " lnDataY == null, cannot display the plot!");
				return;
			}
			if (numRegStart >= numRegEnd) {
				logService.info(this.getClass().getName() + " numRegStart >= numRegEnd, cannot display the plot!");
				return;
			}
			if (numRegEnd <= numRegStart) {
				logService.info(this.getClass().getName() + " numRegEnd <= numRegStart, cannot display the plot!");
				return;
			}
			// String preName = "";
			if (preName == null) {
				preName = "Volume-";
			} else {
				preName = "Volume-";
			}
			boolean isLineVisible = false; // ?
			CsajPlot_RegressionFrame doubleLogPlot = DisplayRegressionPlotXY(lnDataX, lnDataY, isLineVisible,
					"Double log plot - 3D Succolarity", preName + datasetName, "ln(k)", "ln(L)", "", numRegStart, numRegEnd);
			doubleLogPlotList.add(doubleLogPlot);
		}
		if (!imageType.equals("Grey")) {

		}
	}
	

	/**
	 * Displays a regression plot in a separate window.
	 * <p>
	 * 
	 *
	 * </p>
	 * 
	 * @param dataX                 data values for x-axis.
	 * @param dataY                 data values for y-axis.
	 * @param isLineVisible         option if regression line is visible
	 * @param frameTitle            title of frame
	 * @param plotLabel             label of plot
	 * @param xAxisLabel            label of x-axis
	 * @param yAxisLabel            label of y-axis
	 * @param numRegStart                minimum value for regression range
	 * @param numRegEnd                maximal value for regression range
	 * @param optDeleteExistingPlot option if existing plot should be deleted before
	 *                              showing a new plot
	 * @param interpolType          The type of interpolation
	 * @return RegressionPlotFrame
	 */
	private CsajPlot_RegressionFrame DisplayRegressionPlotXY(double[] dataX, double[] dataY,
			boolean isLineVisible, String frameTitle, String plotLabel, String xAxisLabel, String yAxisLabel,String legendLabel,
			int numRegStart, int numRegEnd) {
		// jFreeChart
		CsajPlot_RegressionFrame pl = new CsajPlot_RegressionFrame(dataX, dataY, isLineVisible, frameTitle, plotLabel, xAxisLabel,
				yAxisLabel, legendLabel, numRegStart, numRegEnd);
		pl.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		pl.pack();
		// int horizontalPercent = 5;
		// int verticalPercent = 5;
		// RefineryUtilities.positionFrameOnScreen(pl, horizontalPercent,
		// verticalPercent);
		// CommonTools.centerFrameOnScreen(pl);
		pl.setVisible(true);
		return pl;
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
