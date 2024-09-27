/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj3DFractalFragmentationCmd.java
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
import net.imglib2.algorithm.morphology.Erosion;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.logic.BitType;
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

import at.csa.csaj.commons.Cpol_GeoPoint;
import at.csa.csaj.commons.Cpol_GeoPolygon;
import at.csa.csaj.commons.Cpol_GeoPolygonProc;
import at.csa.csaj.commons.CsajCheck_ItemIn;
import at.csa.csaj.commons.CsajDialog_WaitingWithProgressBar;
import at.csa.csaj.commons.CsajPlot_RegressionFrame;
import at.csa.csaj.commons.QuickHull3D;
import at.csa.csaj.commons.QuickHull3D_Point3d;
import at.csa.csaj.commons.CsajRegression_Linear;
import at.csa.csaj.commons.CsajContainer_Items;
import at.csa.csaj.commons.CsajContainer_ProcessMethod;
import at.csa.csaj.plugin3d.frac.util.BoxCounting3DMethods;
import at.csa.csaj.plugin3d.frac.util.BoxCounting3D_Grey;
import at.csa.csaj.plugin3d.frac.util.GeneralisedDim3DMethods;
import at.csa.csaj.plugin3d.frac.util.GeneralisedDim3D_Grey;

/**
 * A {@link ContextCommand} plugin computing <3D fractal fragmentation indices</a>
 * of an image volume.
 */
@Plugin(type = ContextCommand.class,
	headless = true,
	label = "3D Fractal fragmentation indices",
	initializer = "initialPluginLaunch",
	iconPath = "/icons/comsystan-logo-grey46-16x16.png", //Menu entry icon
	menu = {})

public class Csaj3DFractalFragmentationCmd<T extends RealType<T>> extends ContextCommand implements Previewable {

	private static final String PLUGIN_LABEL            = "Computes 3D Fractal fragmentation indices";
	private static final String SPACE_LABEL             = "";
	private static final String REGRESSION_LABEL        = "<html><b>Regression parameters</b></html>";
	private static final String METHODOPTIONS_LABEL     = "<html><b>Method options</b></html>";
	private static final String BACKGROUNDOPTIONS_LABEL = "<html><b>Background option</b></html>";
	private static final String DISPLAYOPTIONS_LABEL    = "<html><b>Display options</b></html>";
	private static final String PROCESSOPTIONS_LABEL    = "<html><b>Process options</b></html>";

	private static Img<BitType> imgBit; 
	private static Img<FloatType> imgFloat; 
	private static Img<FloatType> imgSubSampled;
	private static Img<FloatType> imgTemp;
	private static RandomAccessibleInterval<?> raiBox;
	private static RandomAccess<UnsignedByteType> ra;
	private static Cursor<?> cursor = null;
	private static Cursor<FloatType> cursorF = null;
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
	
	public static final String TABLE_OUT_NAME = "Table - 3D FFI & FFDI";
	
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

	@Parameter(label = "# of boxes",
    		   description = "Number of distinct box sizes with the power of 2",
	       	   style = NumberWidget.SPINNER_STYLE,
	           min = "1",
	           max = "32768",
	           stepSize = "1",
	           persist = false,  //restore previous value default = true
	           initializer = "initialNumBoxes",
	           callback    = "callbackNumBoxes")
    private int spinnerInteger_NumBoxes;
    
    @Parameter(label = "Regression Start",
    		   description = "Minimum x value of linear regression",
 		       style = NumberWidget.SPINNER_STYLE,
 		       min = "1",
 		       max = "32768",
 		       stepSize = "1",
 		       persist = false,   //restore previous value default = true
 		       initializer = "initialNumRegStart",
 		       callback = "callbackNumRegStart")
    private int spinnerInteger_NumRegStart = 1;
 
    @Parameter(label = "Regression End",
    		   description = "Maximum x value of linear regression",
    		   style = NumberWidget.SPINNER_STYLE,
		       min = "3",
		       max = "32768",
		       stepSize = "1",
		       persist = false,   //restore previous value default = true
		       initializer = "initialNumRegEnd",
		       callback = "callbackNumRegEnd")
     private int spinnerInteger_NumRegEnd = 3;

	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelMethod = METHODOPTIONS_LABEL;

//   @Parameter(label = "Fractal dimension",
//	 description = "Type of fractal dimension computation",
//	 style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
//	 choices = {"Box counting", "Pyramid"},
//	 //persist = false,  //restore previous value default = true
//	 initializer = "initialFracalDimType",
//	 callback = "callbackFractalDimType")
//   private String choiceRadioButt_FractalDimType;

     @Parameter(label = "Scanning method",
    		    description = "Type of 3D box scanning",
    		    style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
      		    choices = {"Raster box"}, //"Sliding box" 
      		    persist = true,  //restore previous value default = true
    		    initializer = "initialScanningType",
                callback = "callbackScanningType")
     private String choiceRadioButt_ScanningType;
     
     @Parameter(label = "Color model",
 		    description = "Type of image and computation",
 		    style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
   		    choices = {"Binary"}, //, "DBC", "RDBC"},
   		    persist = true,  //restore previous value default = true
 		    initializer = "initialColorModelType",
             callback = "callbackColorModelType")
     private String choiceRadioButt_ColorModelType;

	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelDisplayOptions = DISPLAYOPTIONS_LABEL;

	@Parameter(label = "Show double log plot",
		   	   persist = true, //restore previous value default = true
			   initializer = "initialShowDoubleLogPlots")
	private boolean booleanShowDoubleLogPlot;
	
	@Parameter(label = "Show convex hull",
		   	   persist = true, //restore previous value default = true
			   initializer = "initialShowConvexHull")
	private boolean booleanShowConvexHull;

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
	
//	@Parameter(label = "Image #", description = "Image slice number", style = NumberWidget.SPINNER_STYLE, min = "1", max = "99999999", stepSize = "1",
//			   persist = false, // restore  previous value  default  =  true
//			   initializer = "initialNumImageSlice",
//			   callback = "callbackNumImageSlice")
//	private int spinnerInteger_NumImageSlice;
	
	@Parameter(label = "    Process single volume     ", callback = "callbackProcessSingleVolume")
	private Button buttonProcessSingleVolume;
	
//	Deactivated, because it does not work in Fiji (although it works in ImageJ2 -Eclipse)	
//	@Parameter(label = "Process single active image ", callback = "callbackProcessActiveImage")
//	private Button buttonProcessActiveImage;

//	@Parameter(label = "Process all available images", callback = "callbackProcessAllImages")
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
    protected void initialNumRegStart() {
    	spinnerInteger_NumRegStart = 1;
    }
    protected void initialNumRegEnd() {
    	if (datasetIn == null) {
    		logService.error(this.getClass().getName() + " ERROR: Input image volume = null");
    		cancel("ComsystanJ 3D plugin cannot be started - missing input image volume.");
    		return;
    	} else {
    		numBoxes = getMaxBoxNumber(datasetIn.dimension(0), datasetIn.dimension(1), datasetIn.dimension(2));
    	}
    	spinnerInteger_NumRegEnd =  numBoxes;
    }
//    protected void initialFractalDimType() {
//    	choiceRadioButt_FractalDimType = "Box counting";
//    }
    protected void initialScanningType() {
    	choiceRadioButt_ScanningType = "Raster box";
    }
    protected void initialColorModelType() {
    	choiceRadioButt_ColorModelType = "Binary";
    }
	
	protected void initialShowDoubleLogPlots() {
		booleanShowDoubleLogPlot = true;
	}
	
	protected void initialShowConvexHull() {
		booleanShowConvexHull = true;
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
		if (spinnerInteger_NumRegEnd > spinnerInteger_NumBoxes) {
			spinnerInteger_NumRegEnd = spinnerInteger_NumBoxes;
		}
		if (spinnerInteger_NumRegStart >= spinnerInteger_NumRegEnd - 2) {
			spinnerInteger_NumRegStart = spinnerInteger_NumRegEnd - 2;
		}

		numBoxes = spinnerInteger_NumBoxes;
		logService.info(this.getClass().getName() + " Number of boxes set to " + spinnerInteger_NumBoxes);
	}
    /** Executed whenever the {@link #spinnerInteger_NumRegStart} parameter changes. */
	protected void callbackNumRegStart() {
		if (spinnerInteger_NumRegStart >= spinnerInteger_NumRegEnd - 2) {
			spinnerInteger_NumRegStart = spinnerInteger_NumRegEnd - 2;
		}
		if(spinnerInteger_NumRegStart < 1) {
			spinnerInteger_NumRegStart = 1;
		}
		logService.info(this.getClass().getName() + " Regression Min set to " + spinnerInteger_NumRegStart);
	}
	/** Executed whenever the {@link #spinnerInteger_NumRegEnd} parameter changes. */
	protected void callbackNumRegEnd() {
		if (spinnerInteger_NumRegEnd <= spinnerInteger_NumRegStart + 2) {
			spinnerInteger_NumRegEnd = spinnerInteger_NumRegStart + 2;
		}		
		if (spinnerInteger_NumRegEnd > spinnerInteger_NumBoxes) {
			spinnerInteger_NumRegEnd = spinnerInteger_NumBoxes;
		}
		
		logService.info(this.getClass().getName() + " Regression Max set to " + spinnerInteger_NumRegEnd);
	}

	/** Executed whenever the {@link #choiceRadioButtScanningType} parameter changes. */
	protected void callbackScanningType() {
		logService.info(this.getClass().getName() + " Box method set to " + choiceRadioButt_ScanningType);
		
	}
	
	/** Executed whenever the {@link #choiceRadioButt_ColorModelType} parameter changes. */
	protected void callbackColorModelType() {
		logService.info(this.getClass().getName() + " Color model type set to " + choiceRadioButt_ColorModelType);
	}
	
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
	
		dlgProgress = new CsajDialog_WaitingWithProgressBar("Computing 3D fractal fragmentation indices, please wait... Open console window for further info.",
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
		
			Frame frame;
			Frame[] listFrames = JFrame.getFrames();
			for (int i = listFrames.length -1 ; i >= 0; i--) { //Reverse order, otherwise focus is not given free from the last image
				frame = listFrames[i];
				//System.out.println("frame name: " + frame.getTitle());
				if (frame.getTitle().contains("Convex hull")) {
					frame.setVisible(false); //Successfully closes also in Fiji
					frame.dispose();
				}
			}
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
	
		rai =  (RandomAccessibleInterval<T>) datasetIn.copy().getImgPlus(); //dim==3

		// Compute regression parameters
		CsajContainer_ProcessMethod containerPM = process(rai); //rai is 3D
		//D1	 0 Intercept,  1 Slope,  2 InterceptStdErr,  3 SlopeStdErr,  4 RSquared
		//Mass	 5 Intercept,  6 Slope,  7 InterceptStdErr,  8 SlopeStdErr,  9 RSquared
		//Perim	10 Intercept, 11 Slope, 12 InterceptStdErr, 13 SlopeStdErr, 14 RSquared
		
		logService.info(this.getClass().getName() + " 3D Dmass: " + containerPM.item1_Values[6]);
		//writeToTable(0, s, containerPM); //write always to the first row //later because convex hull
		
		//Do it again for convex Hull**************************************************
		logService.info(this.getClass().getName() + " Create convex hull.....");
//		imgBit = createImgBit(rai);
//		int isoLevel = 1; //isoLevel is a threshold
//		mesh = Meshes.marchingCubes(imgBit, isoLevel);
//		//mesh = Meshes.marchingCubes(rai, isoLevel);
//		//or 
//		//mesh = ij.op().geom().marchingCubes(imgBit, isoLevel);
//		
////		defaultConvexHull3D = new DefaultConvexHull3D();
////		hull = defaultConvexHull3D.calculate(mesh);		
////		or
//		List result = opService.geom().convexHull(mesh);
//		hull = (Mesh) result.get(0);
//		
//		//volumes for checking
//		double meshVolume = opService.geom().size(mesh).getRealDouble();
//		double hullVolume =   ij.op().geom().size(hull).getRealDouble();
//			
//		RandomAccessibleInterval<BitType> raiMesh = opService.geom().voxelization(mesh, (int)datasetIn.dimension(0), (int)datasetIn.dimension(1), (int)datasetIn.dimension(2));
//		RandomAccessibleInterval<BitType> raiHull = opService.geom().voxelization(hull, (int)datasetIn.dimension(0), (int)datasetIn.dimension(1), (int)datasetIn.dimension(2));
//		
//		ra = (RandomAccess<UnsignedByteType>) rai.randomAccess();	
//		cursor = (Views.iterable(raiHull)).localizingCursor();
//		long[] pos = new long[3];
//		while(cursor.hasNext()) {
//			cursor.next();
//			cursor.localize(pos);
//			ra.setPosition(pos);
//			if (((BitType) cursor.get()).get() == true)  ((UnsignedByteType) ra.get()).set(255); //This changes the rai
//		}
//		uiService.show("raiMesh", raiMesh);
//		uiService.show("raiHull", raiHull);
//		if (booleanShowConvexHull) uiService.show("Convex hull", rai);

		//Convex hull with
		//QuickHull3D: A Robust 3D Convex Hull Algorithm in Java
		//https://www.cs.ubc.ca/~lloyd/java/quickhull3d.html
		//get number of object points
		int numPoints = 0;
		cursor = Views.iterable(rai).localizingCursor();
		while(cursor.hasNext()) {
			cursor.next();	
			if (((UnsignedByteType) cursor.get()).getInteger() > 0 ) {
				numPoints += 1;
			}
		}
		
		//get list of object points	
		QuickHull3D_Point3d[] points = new QuickHull3D_Point3d[numPoints];
		int p = 0;
		cursor = Views.iterable(rai).localizingCursor();
		int[] pos = new int[3];
		while(cursor.hasNext()) {
			cursor.next();
			cursor.localize(pos);		
			if (((UnsignedByteType) cursor.get()).getInteger() > 0 ) {
				points[p] = new QuickHull3D_Point3d(pos[0], pos[1], pos[2]) ;
				p += 1;
			}
		}
		
		//get vertices of convex hull
		QuickHull3D hull3D = new QuickHull3D();
		hull3D.build (points);
		QuickHull3D_Point3d[] vertices = hull3D.getVertices();
	
//		//write vertices to rai
//		ra = (RandomAccess<UnsignedByteType>) rai.randomAccess();
//		pos = new int[3];
//		for (int v = 0; v <vertices.length; v++) {
//			Point3d pnt = vertices[v];
//			pos[0] = (int)Math.round(pnt.x);
//			pos[1] = (int)Math.round(pnt.y);
//			pos[2] = (int)Math.round(pnt.z);
//			ra.setPosition(pos);
//			ra.get().set(255);
//			//System.out.println (pnt.x + " " + pnt.y + " " + pnt.z);
//		}	
		if (booleanShowConvexHull) uiService.show("Convex hull", rai);
			
		//Change to GeoPolygons to find all pixels inside convex hull
		//https://www.codeproject.com/Articles/1071168/Point-Inside-D-Convex-Polygon-in-Java
		//Create GeoPolygon
		ArrayList<Cpol_GeoPoint> cubeVertices = new ArrayList<Cpol_GeoPoint>();
		QuickHull3D_Point3d p3d;
		for (int v = 0; v < vertices.length; v++) {
			p3d = vertices[v];
			cubeVertices.add(new Cpol_GeoPoint(p3d.x, p3d.y, p3d.z));
		}
		
		//delete quickhull3d classes to save memory
		points   = null;
		hull3D   = null;
		vertices = null;
		
		// Create polygon instance
		Cpol_GeoPolygon polygonInst = new Cpol_GeoPolygon(cubeVertices);
		// Create main process instance
		Cpol_GeoPolygonProc procInst = new Cpol_GeoPolygonProc(polygonInst);
		
		//Main procedure to check if a point (ax, ay, az) is inside the CubeVertices:               
		//procInst.PointInside3DPolygon(ax, ay, az);
		//set rai accordingly
		cursor = Views.iterable(rai).localizingCursor();
		pos = new int[3];
		while(cursor.hasNext()) {
			cursor.next();
			cursor.localize(pos);	
			if (procInst.PointInside3DPolygon(pos[0], pos[1], pos[2])) {
				((UnsignedByteType) cursor.get()).set(255);
			}
		}
		//if (booleanShowConvexHull) uiService.show("Convex hull", rai);
		
		//delete GeoPolygon classes to save memory
		cubeVertices = null;
		polygonInst  = null;
		procInst     = null;
		
		// Compute regression parameters
		logService.info(this.getClass().getName() + " Processing of convex hull volume.....");
		CsajContainer_ProcessMethod containerPMCH = process(rai); //rai is 3D
		//D1	 0 Intercept,  1 Slope,  2 InterceptStdErr,  3 SlopeStdErr,  4 RSquared
		//Mass	 5 Intercept,  6 Slope,  7 InterceptStdErr,  8 SlopeStdErr,  9 RSquared
		//Perim	10 Intercept, 11 Slope, 12 InterceptStdErr, 13 SlopeStdErr, 14 RSquared
		
		logService.info(this.getClass().getName() + " Dmass-convexhull : " + containerPMCH.item1_Values[6]);
		
		int length = containerPM.item1_Values.length; //15
		double[] resultValues = new double[2*length];
		for (int i=0; i < length; i++) {
			resultValues[i]    = containerPM.item1_Values[i];
			resultValues[15+i] = containerPMCH.item1_Values[i]; //Convex hull
		}
		
		CsajContainer_Items containerItems = new CsajContainer_Items(resultValues, containerPM.item2_Values);
		writeToTable(containerItems); //write always to the first row
		
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
		
		GenericColumn columnFileName       = new GenericColumn("File name");
		IntColumn columnMaxNumBoxes        = new IntColumn("# Boxes");
		GenericColumn columnNumRegStart    = new GenericColumn("Reg Start");
		GenericColumn columnNumRegEnd      = new GenericColumn("Reg End");
		GenericColumn columnScanType       = new GenericColumn("Scanning type");
		GenericColumn columnColorModelType = new GenericColumn("Color model");
		//GenericColumn columnFractalDimType = new GenericColumn("Fractal dimension type");
		DoubleColumn columnFFI             = new DoubleColumn("3D FFI");
		DoubleColumn columnFFDI            = new DoubleColumn("3D FFDI");
		DoubleColumn columnFTI             = new DoubleColumn("3D FTI");
		DoubleColumn columnD1              = new DoubleColumn("3D D1");
		DoubleColumn columnDmass           = new DoubleColumn("3D Dmass");
		DoubleColumn columnDbound          = new DoubleColumn("3D Dboundary");
		DoubleColumn columnR2d1            = new DoubleColumn("R2 D1");
		DoubleColumn columnR2mass          = new DoubleColumn("R2 Dmass");
		DoubleColumn columnR2bound         = new DoubleColumn("R2 Dboundary");
		DoubleColumn columnCH_D1           = new DoubleColumn("3D CH_D1");  //CH----ComplexHull
		DoubleColumn columnCH_Dmass        = new DoubleColumn("3D CH_D-mass");
		DoubleColumn columnCH_Dbound       = new DoubleColumn("3D CH_D-boundary");
		DoubleColumn columnCH_R2d1         = new DoubleColumn("CH_R2-D1");
		DoubleColumn columnCH_R2mass       = new DoubleColumn("CH_R2-mass");
		DoubleColumn columnCH_R2bound      = new DoubleColumn("CH_R2-boundary");

		tableOut = new DefaultGenericTable();
		tableOut.add(columnFileName);
		tableOut.add(columnMaxNumBoxes);
		tableOut.add(columnNumRegStart);
		tableOut.add(columnNumRegEnd);
		//tableOut.add(columnFractalDimType);
		tableOut.add(columnScanType);
		tableOut.add(columnColorModelType);
		tableOut.add(columnFFI);
		tableOut.add(columnFFDI);
		tableOut.add(columnFTI);
		tableOut.add(columnD1);
		tableOut.add(columnDmass);
		tableOut.add(columnDbound);
		tableOut.add(columnR2d1);
		tableOut.add(columnR2mass);
		tableOut.add(columnR2bound);
		tableOut.add(columnCH_D1);
		tableOut.add(columnCH_Dmass);
		tableOut.add(columnCH_Dbound);
		tableOut.add(columnCH_R2d1);
		tableOut.add(columnCH_R2mass);
		tableOut.add(columnCH_R2bound);
	}

	/**
	 * collects current result and writes to table
	 * 
	 * @param CsajContainer_Items containerItems
	 */
	private void writeToTable(CsajContainer_Items containerItems) { 

		int numRegStart       = spinnerInteger_NumRegStart;
		int numRegEnd         = spinnerInteger_NumRegEnd;
		int numBoxes          = spinnerInteger_NumBoxes;
		//String fractalDimType  = choiceRadioButt_FractalDimType;
		String scanningType   = choiceRadioButt_ScanningType;  //Raster box     Sliding box
		String colorModelType = choiceRadioButt_ColorModelType;	 //Binary  DBC   RDBC
		//D1        0 Intercept,  1 Dim, 2  InterceptStdErr, 3  SlopeStdErr,  4 RSquared
		//Mass      5 Intercept,  6 Dim, 7  InterceptStdErr, 8  SlopeStdErr,  9 RSquared
		//Boundary 10 Intercept, 11 Dim, 12 InterceptStdErr, 13 SlopeStdErr, 14 RSquared
		
		int row = 0;
		// fill table with values
		tableOut.appendRow();
		tableOut.set("File name",	 	 row, datasetName);	
		tableOut.set("# Boxes",		     row, numBoxes);
		tableOut.set("Reg Start",        row, "("+numRegStart+")" + containerItems.item2_Values[0]); //(NumRegStart)epsRegStart
		tableOut.set("Reg End",      	 row, "("+numRegEnd+")"   + containerItems.item2_Values[1]); //(NumRegEnd)epsRegEnd
		tableOut.set("Color model",		 row, colorModelType);
		//table.set("Fractal dimension type", row, fractalDimType);	
		tableOut.set("3D FFI",         	 row, containerItems.item1_Values[6] - containerItems.item1_Values[11]); //FFI
		tableOut.set("3D FFDI",          row, containerItems.item1_Values[1]*(1.0-(containerItems.item1_Values[6] - containerItems.item1_Values[11]))); //FFDI = D1(1-FFI)
		tableOut.set("3D FTI",         	 row, (containerItems.item1_Values[15+6] - containerItems.item1_Values[15+11])-(containerItems.item1_Values[6] - containerItems.item1_Values[11])); //FFI hull - FFI
		tableOut.set("3D D1",         	 row, containerItems.item1_Values[1]); //D1
		tableOut.set("3D Dmass",         row, containerItems.item1_Values[6]); //Dmass
		tableOut.set("3D Dboundary",     row, containerItems.item1_Values[11]); //D-boundary
		tableOut.set("R2 D1",            row, containerItems.item1_Values[4]);
		tableOut.set("R2 Dmass",         row, containerItems.item1_Values[9]);
		tableOut.set("R2 Dboundary",     row, containerItems.item1_Values[14]);
		tableOut.set("3D CH_D1",         row, containerItems.item1_Values[15+1]); //D1   Complex Hull values
		tableOut.set("3D CH_D-mass",     row, containerItems.item1_Values[15+6]); //Dmass
		tableOut.set("3D CH_D-boundary", row, containerItems.item1_Values[15+11]); //D-boundary
		tableOut.set("CH_R2-D1",         row, containerItems.item1_Values[15+4]);
		tableOut.set("CH_R2-mass",       row, containerItems.item1_Values[15+9]);
		tableOut.set("CH_R2-boundary",   row, containerItems.item1_Values[15+14]);
	}

	/**
	*
	* Processing
	*/
	private CsajContainer_ProcessMethod process(RandomAccessibleInterval<?> rai) { //3Dvolume
	
		logService.info(this.getClass().getName() + " Processing of volume started");
		if (rai == null) {
			logService.info(this.getClass().getName() + " WARNING: rai==null, no image for processing!");
		}
		
		int numRegStart       = spinnerInteger_NumRegStart;
		int numRegEnd         = spinnerInteger_NumRegEnd;
		int numBoxes          = spinnerInteger_NumBoxes;
		String fractalDimType = "Box counting"; // choiceRadioButt_FractalDimType;
		//NOTE: IF minQ IS CHANGED, THE lnDatY[q] FOR THE REGRESSION MUST ALSO BE CHANGED***********************************************
		int minQ            = 0; //NOTE!!!!  //spinnerInteger_MinQ;
		int maxQ            = 2; //spinnerInteger_MaxQ;
		String scanningType   = choiceRadioButt_ScanningType;    //Raster box     Sliding box
		String colorModelType = choiceRadioButt_ColorModelType;	 //Binary  DBC   RDBC
		int pixelPercentage   = 100; //spinnerInteger_PixelPercentage;
		boolean optShowPlot   = booleanShowDoubleLogPlot;


		if ((!colorModelType.equals("Binary")) && (numRegStart == 1)){
			numRegStart = 2; //numRegStart == 1 (single pixel box is not possible for DBC algorithms)
		}

		long width  = rai.dimension(0);
		long height = rai.dimension(1);
		long depth  = rai.dimension(2);
		
		double[] epsRegStartEnd   = new double[2];  // epsRegStart, epsRegEnd
		double[] regressionParams = null;
		double[] resultValues = null;
	
		String plot_method = "3D Fractal fragmentation indices";
		String xAxis = "ln(eps)";
		String yAxis = "ln(Count)";
		
		GeneralisedDim3DMethods   gd3DMass  = null;
		BoxCounting3DMethods      bc3DMass  = null;
		BoxCounting3DMethods      bc3DPerim = null;
		
		if (imageType.equals("Grey")) {// grey image   //additional check, is already checked during validation of active dataset
			//*****************************************************************************************************************************************
			
			//D1
			gd3DMass = new GeneralisedDim3D_Grey(rai, numBoxes, minQ, maxQ, scanningType, colorModelType, pixelPercentage, dlgProgress, statusService);	
			
			//Db mass
			bc3DMass  = new BoxCounting3D_Grey(rai, numBoxes, scanningType, colorModelType, dlgProgress, statusService);
			
			//*********create boundary image
			//Compute erode image
			RectangleShape kernel = new RectangleShape(1, false); //3x3kernel skipCenter = false
			Runtime runtime  = Runtime.getRuntime();
			long maxMemory   = runtime.maxMemory();
			long totalMemory = runtime.totalMemory();
			long freeMemory  = runtime.freeMemory();
			int availableProcessors = runtime.availableProcessors();
			//System.out.println("available processors: " + availableProcessors);
			
			int numThreads = 6; //For erosion //with 6 it was 3 times faster than only with one thread
			if (numThreads > availableProcessors) numThreads = availableProcessors;

			long[] pos = new long[3];
			int pixelValueImgOrig;
			float pixelValueImgTemp;
			
			imgFloat = createImgFloat(rai);
			imgTemp = Erosion.erode(imgFloat, kernel, numThreads); //eroded image
			//uiService.show("Eroded image", imgTemp);
			ra = (RandomAccess<UnsignedByteType>) rai.randomAccess();
			//subtraction ImgOrig - ImgErode
			cursorF = imgTemp.localizingCursor();
			while (cursorF.hasNext()) {
				cursorF.fwd();
				cursorF.localize(pos);
				ra.setPosition(pos);
				pixelValueImgTemp = ((FloatType) cursorF.get()).get();    //eroded image
				pixelValueImgOrig = ((UnsignedByteType) ra.get()).getInteger(); //original image
				// Binary Image: 0 and [1, 255]! and not: 0 and 255
				if ((pixelValueImgOrig > 0) && (pixelValueImgTemp == 0)) {
					((FloatType) cursorF.get()).set(255f);					
				} else {
					((FloatType) cursorF.get()).set(0f);		
				}
			}
			//uiService.show("Boundary image", imgTemp);
			
			//Db boundary
			bc3DPerim = new BoxCounting3D_Grey(imgTemp, numBoxes, scanningType, colorModelType, dlgProgress, statusService);
			plot_method="3D - Double Log Plots - fractal fragmentation indices";
			xAxis = "ln(Box size)";
			yAxis = "ln(Count)";
			
			//******************************************************************************************************************************************
			
			
		} else if (imageType.equals("RGB")) { // RGB image  //additional check, is already checked during validation of active dataset
		
			//no method implemented

		}
			
		int numQ = maxQ - minQ + 1;
		double[][] totalsGen         = new double[numQ][numBoxes]; //several Generalised dims will be computed but only one will be taken in the end
		double[]   totalsMass        = new double[numBoxes];
		double[]   totalsBoundary    = new double[numBoxes];
		double[]   eps               = new double[numBoxes];
		
		eps            = bc3DMass.calcEps();
		logService.info(this.getClass().getName() + " Processing of D1.....");
		totalsGen      = gd3DMass.calcTotals();
		logService.info(this.getClass().getName() + " Processing of Dmass.....");
		totalsMass     = bc3DMass.calcTotals();
		logService.info(this.getClass().getName() + " Processing of Dboundary.....");
		totalsBoundary = bc3DPerim.calcTotals();
		
		if (eps == null || totalsGen == null || totalsMass == null || totalsBoundary  == null) return null;
		
		//Computing log values for plot 
		//Change sequence of entries to start with a pixel
		double[][] lnTotalsGen      = new double[numQ][numBoxes];
		double[]   lnTotalsMass     = new double[numBoxes];
		double[]   lnTotalsBoundary = new double[numBoxes];
		double[]   lnEps            = new double[numBoxes];
	
		//logService.info(this.getClass().getName() + " 3D FFI 6 FFDI");
		//logService.info(this.getClass().getName() + " lnEps: \t  lnTotals:");	

		for (int n = 0; n < numBoxes; n++) {
				for (int q = 0; q < numQ; q++) {
					if (totalsGen[q][n] == 0) {
						totalsGen[q][n] = Double.MIN_VALUE;
					} else if (Double.isNaN(totalsGen[q][n])) {
						totalsGen[q][n] = Double.NaN;
					}
					if ((q + minQ) != 1)
						lnTotalsGen[q][n] = Math.log(totalsGen[q][n]);
					if ((q + minQ) == 1) //D1
						lnTotalsGen[q][n] = totalsGen[q][n];			
				}
				//Dmass
				if (totalsMass[n] == 0) {
					lnTotalsMass[n] = Math.log(Double.MIN_VALUE);
				} else if (Double.isNaN(totalsMass[n])) {
					lnTotalsMass[n] = Double.NaN;
				} else {
					lnTotalsMass[n] = Math.log(totalsMass[n]);
				}
				//Dboundary
				if (totalsBoundary[n] == 0) {
					lnTotalsBoundary[n] = Math.log(Double.MIN_VALUE);
				} else if (Double.isNaN(totalsBoundary[n])) {
					lnTotalsBoundary[n] = Double.NaN;
				} else {
					lnTotalsBoundary[n] = Math.log(totalsBoundary[n]);
				}
				
				lnEps[n] = Math.log(eps[n]);
				//logService.info(this.getClass().getName() + " n:" + n + " eps:  " + eps[n]);
				//logService.info(this.getClass().getName() + " n:" + n + " lnEps:  "+  lnEps[n]);
				//logService.info(this.getClass().getName() + " n:" + n + " totalsMass[n]: " + totalsMass[n]);
		}
				
		//Create double log plot
		boolean isLineVisible = false; //?
				
		// Plot //nur ein Band!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		double[]   lnDataX  = new double[numBoxes];
		double[][] lnDataY  = new double[3][numBoxes]; //for D1, Dmass Dboundary
	
		for (int n = 0; n < numBoxes; n++) {
			//Take only one dimension out of the gneralized dimensions
			lnDataY[0][n]  = lnTotalsGen[1][n];	 //Index 1 for D1 (BUT ONLY IF minQ = 0  !!!!!!!!!)
			lnDataY[1][n]  = lnTotalsMass[n];	
			lnDataY[2][n]  = lnTotalsBoundary[n];	
			lnDataX[n]     = lnEps[n];
		}
		// System.out.println("FractalDimensionBoxCounting: dataY: "+ dataY);
		// System.out.println("FractalDimensionBoxCounting: dataX: "+ dataX);
	
		if (optShowPlot) {	
			logService.info(this.getClass().getName() + " Prepare double log plot......");
			String preName = "";
			String xAxisLabel = "";
			if      (fractalDimType.equals("Box counting")) xAxisLabel = "ln(Box size)";
			else if (fractalDimType.equals("Pyramid"))      xAxisLabel = "ln(2^n)";
			String yAxisLabel = "ln(Count)";
			
			String[] legendLabels = new String[3];
			legendLabels[0] = "D1";
			legendLabels[1] = "D Mass";
			legendLabels[2] = "D Boundary";
			
			CsajPlot_RegressionFrame doubleLogPlot = DisplayRegressionPlotXY(lnDataX, lnDataY, isLineVisible, "Double Log Plots - fractal fragmentation indices", 
					preName + datasetName, xAxisLabel, yAxisLabel, legendLabels,
					numRegStart, numRegEnd);
			doubleLogPlotList.add(doubleLogPlot);
		}
				
		// Compute regressions
		logService.info(this.getClass().getName() + " Computing of regressions.....");
		CsajRegression_Linear lr = new CsajRegression_Linear();
		double[] regressionParamsD1 = lr.calculateParameters(lnDataX, lnDataY[0], numRegStart, numRegEnd);
		lr = new CsajRegression_Linear();
		double[] regressionParamsMass = lr.calculateParameters(lnDataX, lnDataY[1], numRegStart, numRegEnd);
		lr = new CsajRegression_Linear();
		double[] regressionParamsPerim = lr.calculateParameters(lnDataX, lnDataY[2], numRegStart, numRegEnd);
		//0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
		
		regressionParams = new double[regressionParamsD1.length + regressionParamsMass.length + regressionParamsPerim.length]; 
		for (int r = 0; r < regressionParamsD1.length; r++) {
			regressionParams[r] = regressionParamsD1[r];
		}
		for (int r = 0; r < regressionParamsMass.length; r++) {
			regressionParams[r + regressionParamsD1.length] = regressionParamsMass[r];
		}
		for (int r = 0; r < regressionParamsPerim.length; r++) {
			regressionParams[r + regressionParamsD1.length + regressionParamsMass.length] = regressionParamsPerim[r];
		}
		
		//Compute result values
		resultValues = regressionParams;
		double dimD1    = Double.NaN;
		double dimMass  = Double.NaN;
		double dimPerim = Double.NaN;
		dimD1    =  regressionParams[1]; //Slope for D1
		dimMass  = -regressionParams[6];
		dimPerim = -regressionParams[11];	
		resultValues[1]  = dimD1;
		resultValues[6]  = dimMass;
		resultValues[11] = dimPerim;
		//logService.info(this.getClass().getName() + " D mass: " + dimMass); //do this in processSingleInputVolume()
				
		epsRegStartEnd[0] = eps[numRegStart-1];
		epsRegStartEnd[1] = eps[numRegEnd-1];
	
		return new CsajContainer_ProcessMethod(resultValues, epsRegStartEnd);
		//Output
		//uiService.show(TABLE_OUT_NAME, table);
		////result = ops.create().img(image, new FloatType()); may not work in older Fiji versions
		//result = new ArrayImgFactory<>(new FloatType()).create(image.dimension(0), image.dimension(1)); 
		//table
	}

	/**
	 * Displays a regression plot in a separate window.
	 * <p>
	 *		
	 *
	 * </p>
	 * 
	 * @param dataX data values for x-axis.
	 * @param dataY data values for y-axis.
	 * @param isLineVisible option if regression line is visible
	 * @param frameTitle title of frame
	 * @param plotLabel  label of plot
	 * @param xAxisLabel label of x-axis
	 * @param yAxisLabel label of y-axis
	 * @param numRegStart minimum value for regression range
	 * @param numRegEnd maximal value for regression range 
	 * @param optDeleteExistingPlot option if existing plot should be deleted before showing a new plot
	 * @param interpolType The type of interpolation
	 * @return RegressionPlotFrame
	 */			
	private CsajPlot_RegressionFrame DisplayRegressionPlotXY(double[] dataX, double[][] dataY, boolean isLineVisible,
			String frameTitle, String plotLabel, String xAxisLabel, String yAxisLabel, String[] legendLabels, int numRegStart, int numRegEnd) {
		// jFreeChart
		CsajPlot_RegressionFrame pl = new CsajPlot_RegressionFrame(dataX, dataY, isLineVisible, frameTitle, plotLabel, xAxisLabel,
				yAxisLabel, legendLabels, numRegStart, numRegEnd);
		pl.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		pl.pack();
		// int horizontalPercent = 5;
		// int verticalPercent = 5;
		// RefineryUtilities.positionFrameOnScreen(pl, horizontalPercent,
		// verticalPercent);
		//CommonTools.centerFrameOnScreen(pl);
		pl.setVisible(true);
		return pl;	
	}
	
	/**
	 * 
	 * This methods creates an Img<BitType>
	 */
	private Img<BitType > createImgBit(RandomAccessibleInterval<?> rai){ //rai must always be a single 3D volume
		
		imgBit = new ArrayImgFactory<>(new BitType()).create(rai.dimension(0), rai.dimension(1), rai.dimension(2)); //always single 3D volume
		Cursor<BitType> cursor = imgBit.localizingCursor();
		final long[] pos = new long[imgBit.numDimensions()];
		RandomAccess<RealType<?>> ra = (RandomAccess<RealType<?>>) rai.randomAccess();
		while (cursor.hasNext()){
			cursor.fwd();
			cursor.localize(pos);
			ra.setPosition(pos);
			//if (numSlices == 1) { //for only one 2D image;
			//	ra.setPosition(pos[0], 0);
			//	ra.setPosition(pos[1], 1);
			//} else { //for more than one image e.g. image stack
			//	ra.setPosition(pos[0], 0);
			//	ra.setPosition(pos[1], 1);
			//	ra.setPosition(s, 2);
			//}
			//ra.get().setReal(cursor.get().get());
			if (ra.get().getRealFloat() > 0) cursor.get().setOne();
		}	
		return imgBit;
	}

	/**
	 * 
	 * This methods creates an Img<FloatType>
	 */
	private Img<FloatType > createImgFloat(RandomAccessibleInterval<?> rai){ //rai must always be a single 3D volume
		
		imgFloat = new ArrayImgFactory<>(new FloatType()).create(rai.dimension(0), rai.dimension(1), rai.dimension(2)); //always single 3D
		Cursor<FloatType> cursor = imgFloat.localizingCursor();
		final long[] pos = new long[imgFloat.numDimensions()];
		RandomAccess<RealType<?>> ra = (RandomAccess<RealType<?>>) rai.randomAccess();
		while (cursor.hasNext()){
			cursor.fwd();
			cursor.localize(pos);
			ra.setPosition(pos);
			cursor.get().setReal(ra.get().getRealFloat());
		}	
		return imgFloat;
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
