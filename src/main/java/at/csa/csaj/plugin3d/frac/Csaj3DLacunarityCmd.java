/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj3DLacunarityCmd.java
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

package at.csa.csaj.plugin3d.frac;

import java.awt.Frame;
import java.awt.Toolkit;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
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
import at.csa.csaj.plugin3d.frac.util.Lacunarity3DMethods;
import at.csa.csaj.plugin3d.frac.util.Lacunarity3D_Grey;

/**
 * A {@link ContextCommand} plugin computing <the 3D Lacunarity</a>
 * of an image volume.
 */
@Plugin(type = ContextCommand.class,
	headless = true,
	label = "3D Lacunarity",
	initializer = "initialPluginLaunch",
	iconPath = "/icons/comsystan-logo-grey46-16x16.png", //Menu entry icon
	menu = {})

public class Csaj3DLacunarityCmd<T extends RealType<T>> extends ContextCommand implements Previewable {

	private static final String PLUGIN_LABEL            = "Computes 3D Lacunarity";
	private static final String SPACE_LABEL             = "";
	private static final String REGRESSION_LABEL        = "<html><b>Regression parameters</b></html>";
	private static final String METHODOPTIONS_LABEL     = "<html><b>Method options</b></html>";
	private static final String BACKGROUNDOPTIONS_LABEL = "<html><b>Background option</b></html>";
	private static final String DISPLAYOPTIONS_LABEL    = "<html><b>Display options</b></html>";
	private static final String PROCESSOPTIONS_LABEL    = "<html><b>Process options</b></html>";

	private static Img<FloatType> imgFloat; 
	private static RandomAccess<?> ra;
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

	public static final String TABLE_OUT_NAME = "Table - 3D Lacunarities";
	
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
     
    @Parameter(label = "Color model",
 		    description = "Type of image and computation",
 		    style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
   		    choices = {"Binary", "Grey"}, //, "DBC", "RDBC"},
   		    persist = true,  //restore previous value default = true
 		    initializer = "initialColorModelType",
             callback = "callbackColorModelType")
    private String choiceRadioButt_ColorModelType;
    
//    Only for sliding box option
    @Parameter(label = "(Sliding box) Pixel %",
    		   description = "% of image pixels to be taken - to lower computation times",
  	       	   style = NumberWidget.SPINNER_STYLE,
  	           min = "1",
  	           max = "100",
  	           stepSize = "1",
  	           //persist = false,  //restore previous value default = true
  	           initializer = "initialPixelPercentage",
  	           callback    = "callbackPixelPercentage")
    private int spinnerInteger_PixelPercentage;
//    
//    @Parameter(label = "(Tug of war) Accuracy",
//		       description = "Accuracy (default=90)",
//	       	   style = NumberWidget.SPINNER_STYLE,
//	           min = "1",
//	           max = "99999999999999",
//	           stepSize = "1",
//	           persist = true,  //restore previous value default = true
//	           initializer = "initialNumAccuracy",
//	           callback    = "callbackNumAccuracy")
//    private int spinnerInteger_NumAcurracy;
//
//    @Parameter(label = "(Tug of war) Confidence",
//		       description = "Confidence (deafault=15)",
//	       	   style = NumberWidget.SPINNER_STYLE,
//	           min = "1",
//	           max = "99999999999999",
//	           stepSize = "1",
//	           persist = true,  //restore previous value default = true
//	           initializer = "initialNumConfidence",
//	           callback    = "callbackNumConfidence")
//    private int spinnerInteger_NumConfidence;

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
    protected void initialColorModelType() {
    	choiceRadioButt_ColorModelType = "Binary";
    }
    protected void initialPixelPercentage() {
      	spinnerInteger_PixelPercentage = 10;
    }
//    protected void initialNumAccuracy() {
//    	spinnerInteger_NumAcurracy = 90; //s1=30 Wang paper
//    }
//    
//    protected void initialNumConfidence() {
//    	spinnerInteger_NumConfidence = 15; //s2=5 Wang paper
//    }
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

	/** Executed whenever the {@link #choiceRadioButtScanningType} parameter changes. */
	protected void callbackScanningType() {
		if (choiceRadioButt_ScanningType.equals("Tug of war")) {
			if (choiceRadioButt_ColorModelType.equals("Grey")) {
				logService.info(this.getClass().getName() + " NOTE! Only binary Tug of war algorithm possible!");
				choiceRadioButt_ColorModelType = "Binary";
			}
		}
		logService.info(this.getClass().getName() + " Box method set to " + choiceRadioButt_ScanningType);
		
	}
	
	/** Executed whenever the {@link #choiceRadioButt_ColorModelType} parameter changes. */
	protected void callbackColorModelType() {
		if (choiceRadioButt_ScanningType.equals("Tug of war")) {
			if (choiceRadioButt_ColorModelType.equals("Grey")) {
				logService.info(this.getClass().getName() + " NOTE! Only binary Tug of war algorithm possible!");
				choiceRadioButt_ColorModelType = "Binary";
			}
		}
		logService.info(this.getClass().getName() + " Color model type set to " + choiceRadioButt_ColorModelType);
	}
	
	/** Executed whenever the {@link #spinnerInteger_PixelPercentage} parameter changes. */
	protected void callbackPixelPercentage() {
		logService.info(this.getClass().getName() + " Pixel % set to " + spinnerInteger_PixelPercentage);
	}
	
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
	
		dlgProgress = new CsajDialog_WaitingWithProgressBar("Computing 3D Lacunarity, please wait... Open console window for further info.",
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
		GenericColumn columnColorModelType = new GenericColumn("Color model");
		//IntColumn columnPixelPercentage    = new IntColumn("(Sliding Box) Pixel %");
		DoubleColumn columnL_RP            = new DoubleColumn("<L>-R&P"); //weighted mean L
		DoubleColumn columnL_SV            = new DoubleColumn("<L>-S&V");   //mean L

		tableOut = new DefaultGenericTable();
		tableOut.add(columnFileName);
		tableOut.add(columnMaxNumBoxes);
		//tableOut.add(columnNumRegStart);
		//tableOut.add(columnNumRegEnd);
		tableOut.add(columnScanningType);
		tableOut.add(columnColorModelType);
		//tableOut.add(columnPixelPercentage);
		tableOut.add(columnL_RP);
		tableOut.add(columnL_SV);
		
		String preString = "L";
//		int epsWidth = 1;
//		for (int i = 0; i < numBoxes; i++) {
//			table.add(new DoubleColumn(preString + "-" + (2*epsWidth+1) + "x" + (2*epsWidth+1)));
//			epsWidth = epsWidth * 2;
//		}
		for (int n = 0; n < numBoxes; n++) {
			tableOut.add(new DoubleColumn(preString + " " + (int)Math.round(Math.pow(2, n)) + "x" + (int)Math.round(Math.pow(2, n))));
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
		int pixelPercentage   = spinnerInteger_PixelPercentage;
		int accuracy 	      = 0; //spinnerInteger_NumAcurracy;
		int confidence        = 0; //spinnerInteger_NumConfidence;
		String scanningType   = choiceRadioButt_ScanningType;  //Raster box     Sliding box
		String colorModelType = choiceRadioButt_ColorModelType;	 //Binary  DBC   RDBC
		
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
		if (scanningType.equals("Tug of war")) {
			tableOut.set("Scanning type",   row, "Tug of war Acc" + accuracy + " Conf" + confidence);
		}
		else if (scanningType.equals("Sliding box")) {
			tableOut.set("Scanning type",   row, "Sliding box "  + pixelPercentage + "%");
		}
		else {
			tableOut.set("Scanning type",   row, scanningType);
		}
		tableOut.set("Color model",	    	row, colorModelType);
		//if (scanningType.equals("Sliding box")) tableOut.set("(Sliding Box) Pixel %", row, pixelPercentage);	
		tableOut.set("<L>-R&P",   		   row, containerPM.item1_Values[containerPM.item1_Values.length - 2]); //
		tableOut.set("<L>-S&V",   		   row, containerPM.item1_Values[containerPM.item1_Values.length - 1]); //last entry	
		tableColLast = 5;
		
		int numParameters = containerPM.item1_Values.length - 2; 
		tableColStart = tableColLast + 1;
		tableColEnd = tableColStart + numParameters;
		for (int c = tableColStart; c < tableColEnd; c++ ) {
			tableOut.set(c, row, containerPM.item1_Values[c-tableColStart]);
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
		String scanningType   = choiceRadioButt_ScanningType;    //Raster box     Sliding box
		String colorModelType = choiceRadioButt_ColorModelType;	 //Binary  DBC   RDBC
		int pixelPercentage   = spinnerInteger_PixelPercentage;
		boolean optShowPlot   = booleanShowDoubleLogPlot;


//		if ((!colorModelType.equals("Binary")) && (numRegStart == 1)){
//			numRegStart = 2; //numRegStart == 1 (single pixel box is not possible for DBC algorithms)
//		}

		double[] epsRegStartEnd   = new double[2];  // epsRegStart, epsRegEnd
		
		// data array
		int[]    eps;
		//totals == lacunarities
		double[]totals = new double[numBoxes + 2]; //+2 because of weighted mean L and mean L
		for (int l = 0; l < totals.length; l++) totals[l] = Double.NaN;
	
		String plot_method = "3D Lacunarities";
		String xAxis = "ln(eps)";
		String yAxis = "ln(Count)";
		
		Lacunarity3DMethods cd3D = null;
		
		if (imageType.equals("Grey")) {// grey image   //additional check, is already checked during validation of active dataset
			//*****************************************************************************************************************************************
			cd3D = new Lacunarity3D_Grey(rai, numBoxes, scanningType, colorModelType, pixelPercentage, dlgProgress, statusService);	
			plot_method="3D Lacunarity";
			xAxis = "ln(Box size)";
			yAxis = "ln(L)";
		
			//******************************************************************************************************************************************
			
		} else if (imageType.equals("RGB")) { // RGB image  //additional check, is already checked during validation of active dataset
		
			//no method implemented

		}
		
		eps    = cd3D.calcEps();
		totals = cd3D.calcTotals();
		
		if (   eps == null || totals == null) return null;
		
		//Compute weighted mean Lacunarity according to Equ.3 of Roy&Perfect, 2014, Fractals, Vol22, No3
		//DOI: 10.1142/S0218348X14400039
		double L_RP = 0.0;
		double sumBoxSize = 0.0;
		for (int n = 0; n < numBoxes; n++) {	
			L_RP = L_RP + Math.log10(totals[n]) * Math.log10(   eps[n]);
			sumBoxSize = sumBoxSize + Math.log10(   eps[n]);
		}
		totals[numBoxes] = L_RP/sumBoxSize; //append it to the list of lacunarities	
		
		//Compute mean Lacunarity according to Equ.29 of Sengupta and Vinoy, 2006, Fractals, Vol14 No4 p271-282
		//DOI: 10.1142/S0218348X06003313 
		double L_SV = 0.0;
		for (int n = 0; n < numBoxes; n++) {	
			L_SV = L_SV + totals[n] *    eps[n];
		}
		totals[numBoxes + 1] = Math.log(L_SV/(   eps[numBoxes-1] -    eps[0])); //append it to the list of lacunarities	
			
		//prepare plot
		double[] lnDataX = new double[numBoxes];
		double[] lnDataY = new double[numBoxes];
		for (int n = 0; n < numBoxes; n++) {	
				if (totals[n] == 0) {
					lnDataY[n]= Math.log(Float.MIN_VALUE);
				} else {
					lnDataY[n] = Math.log(totals[n]);
				}
				lnDataX[n] = Math.log(   eps[n]);
				// System.out.println("Lacunarity: " );	
		}
				
		//Create double log plot	
		boolean isLineVisible = false; //?		
		if (optShowPlot) {
			if ((imageType.equals("Grey")) || (imageType.equals("RGB"))) { //both are OK
				String preName = "Volume-";
				CsajPlot_RegressionFrame doubleLogPlot = DisplayRegressionPlotXY(lnDataX, lnDataY, isLineVisible, plot_method, 
						preName + datasetName, xAxis, yAxis, "",
						1, numBoxes);
				doubleLogPlotList.add(doubleLogPlot);
			}
			else {
	
			}
		}
		
		// Compute regression
		//LinearRegression lr = new LinearRegression();
		//regressionParams = lr.calculateParameters(lnEps, lnTotals, numRegStart, numRegEnd);
		//0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
			
		logService.info(this.getClass().getName() + " 3D Lacunarities[0]: " + totals[0]);
		
		epsRegStartEnd[0] = eps[0];            //eps[numRegStart-1];
		epsRegStartEnd[1] = eps[eps.length-1]; //eps[numRegEnd-1];
	
		return new CsajContainer_ProcessMethod(totals, epsRegStartEnd);
	
		// Output
		// uiService.show("Table - 3D Lacunarities", table);
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
					"Double log plot - 3D Lacunarity", preName + datasetName, "ln(k)", "ln(L)", "", numRegStart, numRegEnd);
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
