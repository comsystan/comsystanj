/*-
 * #%L
 * Project: ImageJ2 plugin for computing the 3D fractal fragmentation indices.
 * File: Csaj3DFractalFragmentation.java
 * 
 * $Id$
 * $HeadURL$
 * 
 * This file is part of ComsystanJ software, hereinafter referred to as "this program".
 * %%
 * Copyright (C) 2022 - 2024 Comsystan Software
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


package at.csa.csaj.plugin3d.frac.dim.ff;

import java.awt.Frame;
import java.awt.Toolkit;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import org.scijava.menu.MenuConstants;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;
import org.scijava.table.DefaultGenericTable;
import org.scijava.table.DoubleColumn;
import org.scijava.table.GenericColumn;
import org.scijava.table.IntColumn;
import org.scijava.ui.DialogPrompt.MessageType;
import org.scijava.ui.DialogPrompt.OptionType;
import org.scijava.ui.DialogPrompt.Result;
import org.scijava.ui.UIService;
import org.scijava.widget.Button;
import org.scijava.widget.ChoiceWidget;
import org.scijava.widget.FileWidget;
import org.scijava.widget.NumberWidget;

import at.csa.csaj.commons.algorithms.geopolygon3d.GeoPoint;
import at.csa.csaj.commons.algorithms.geopolygon3d.GeoPolygon;
import at.csa.csaj.commons.algorithms.geopolygon3d.GeoPolygonProc;
import at.csa.csaj.commons.algorithms.quickhull3d.Point3d;
import at.csa.csaj.commons.algorithms.quickhull3d.QuickHull3D;
import at.csa.csaj.commons.dialog.WaitingDialogWithProgressBar;
import at.csa.csaj.commons.plot.RegressionPlotFrame;
import at.csa.csaj.commons.regression.LinearRegression;
import at.csa.csaj.plugin3d.frac.dim.box.util.BoxCounting3DMethods;
import at.csa.csaj.plugin3d.frac.dim.box.util.BoxCounting3D_Grey;
import at.csa.csaj.plugin3d.frac.dim.generalized.util.GeneralizedDim3DMethods;
import at.csa.csaj.plugin3d.frac.dim.generalized.util.GeneralizedDim3D_Grey;
import io.scif.DefaultImageMetadata;
import io.scif.MetaTable;


/**
 * A {@link ContextCommand} plugin computing <3D fractal fragmentation indices</a>
 * of an image volume.
 */
@Plugin(type = ContextCommand.class,
headless = true,
label = "3D Fractal fragmentation indices",
initializer = "initialPluginLaunch",
//iconPath = "/images/comsystan-??.png", //Menu entry icon
menu = {
@Menu(label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT, mnemonic = MenuConstants.PLUGINS_MNEMONIC),
@Menu(label = "ComsystanJ"),
@Menu(label = "3D Volume"),
@Menu(label = "3D Fractal fragmentation indices", weight = 80)})
//public class Csaj3DFractalFragmentation<T extends RealType<T>> extends InteractiveCommand { // non blocking  GUI
public class Csaj3DFractalFragmentation<T extends RealType<T>> extends ContextCommand implements Previewable { //modal GUI with cancel

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
	private static ArrayList<RegressionPlotFrame> doubleLogPlotList = new ArrayList<RegressionPlotFrame>();
	private static double[] resultValuesTable; //the corresponding regression values
	private static final String tableOutName = "Table - 3D FFI & FFDI";
	
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

	@Parameter(label = tableOutName, type = ItemIO.OUTPUT)
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
    
    @Parameter(label = "Regression Min",
    		   description = "Minimum x value of linear regression",
 		       style = NumberWidget.SPINNER_STYLE,
 		       min = "1",
 		       max = "32768",
 		       stepSize = "1",
 		       persist = false,   //restore previous value default = true
 		       initializer = "initialRegMin",
 		       callback = "callbackRegMin")
    private int spinnerInteger_RegMin = 1;
 
    @Parameter(label = "Regression Max",
    		   description = "Maximum x value of linear regression",
    		   style = NumberWidget.SPINNER_STYLE,
		       min = "3",
		       max = "32768",
		       stepSize = "1",
		       persist = false,   //restore previous value default = true
		       initializer = "initialRegMax",
		       callback = "callbackRegMax")
     private int spinnerInteger_RegMax = 3;

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
	
	@Parameter(label   = "    Process single volume     ",
		    	callback = "callbackProcessSingleVolume")
	private Button buttonProcessSingleVolume;
	
//	Deactivated, because it does not work in Fiji (although it works in ImageJ2 -Eclipse)	
//	@Parameter(label   = "Process single active image ",
//		    callback = "callbackProcessActiveImage")
//	private Button buttonProcessActiveImage;

//	@Parameter(label = "Process all available images",
//			callback = "callbackProcessAllImages")
//	private Button buttonProcessAllImages;
	
	// ---------------------------------------------------------------------
		
	protected void initialPluginLaunch() {
		//datasetIn = imageDisplayService.getActiveDataset();
		checkItemIOIn();
	}
	
    protected void initialNumBoxes() {
      	numBoxes = getMaxBoxNumber(datasetIn.max(0)+1, datasetIn.dimension(1), datasetIn.dimension(2));
      	spinnerInteger_NumBoxes = numBoxes;
    }
    protected void initialRegMin() {
    	spinnerInteger_RegMin = 1;
    }
    protected void initialRegMax() {
    	numBoxes = getMaxBoxNumber(datasetIn.max(0)+1, datasetIn.dimension(1), datasetIn.dimension(2));
    	spinnerInteger_RegMax =  numBoxes;
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
		if (spinnerInteger_RegMax > spinnerInteger_NumBoxes) {
			spinnerInteger_RegMax = spinnerInteger_NumBoxes;
		}
		if (spinnerInteger_RegMin >= spinnerInteger_RegMax - 2) {
			spinnerInteger_RegMin = spinnerInteger_RegMax - 2;
		}

		numBoxes = spinnerInteger_NumBoxes;
		logService.info(this.getClass().getName() + " Number of boxes set to " + spinnerInteger_NumBoxes);
	}
    /** Executed whenever the {@link #spinnerInteger_RegMin} parameter changes. */
	protected void callbackRegMin() {
		if (spinnerInteger_RegMin >= spinnerInteger_RegMax - 2) {
			spinnerInteger_RegMin = spinnerInteger_RegMax - 2;
		}
		if(spinnerInteger_RegMin < 1) {
			spinnerInteger_RegMin = 1;
		}
		logService.info(this.getClass().getName() + " Regression Min set to " + spinnerInteger_RegMin);
	}
	/** Executed whenever the {@link #spinnerInteger_RegMax} parameter changes. */
	protected void callbackRegMax() {
		if (spinnerInteger_RegMax <= spinnerInteger_RegMin + 2) {
			spinnerInteger_RegMax = spinnerInteger_RegMin + 2;
		}		
		if (spinnerInteger_RegMax > spinnerInteger_NumBoxes) {
			spinnerInteger_RegMax = spinnerInteger_NumBoxes;
		}
		
		logService.info(this.getClass().getName() + " Regression Max set to " + spinnerInteger_RegMax);
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
	    	   	uiService.show(tableOutName, tableOut);
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
	    	   	uiService.show(tableOutName, tableOut);
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
		    	   	uiService.show(tableOutName, tableOut);   //Show table because it did not go over the run() method
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
	    startWorkflowForSingleVolume();
	}

	public void checkItemIOIn() {

		//datasetIn = imageDisplayService.getActiveDataset();

		if ((datasetIn.firstElement() instanceof UnsignedByteType) || (datasetIn.firstElement() instanceof FloatType)) {
			// That is OK, proceed
		} else {

			final MessageType messageType = MessageType.WARNING_MESSAGE;
			final OptionType optionType = OptionType.DEFAULT_OPTION;
			final String title = "Image type validation result";
			final String message = "Data type not allowed: " + datasetIn.getType().getClass().getSimpleName();
			// Prompt for confirmation.
			// final UIService uiService = getContext().getService(UIService.class);
			Result result = uiService.showDialog(message, title, messageType, optionType);
			// Cancel the command execution if the user does not agree.
			// if (result != Result.YES_OPTION) System.exit(-1);
			// if (result != Result.YES_OPTION) return;
		}
		// get some info
		width = datasetIn.dimension(0);
		height = datasetIn.dimension(1);
		//depth = dataset.getDepth(); //does not work if third axis ist not specifyed as z-Axis
		numDimensions = datasetIn.numDimensions();
	
		//compositeChannelCount = datasetIn.getImgPlus().getCompositeChannelCount(); //1  Grey,   3 RGB
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

		// get the name of dataset
		datasetName = datasetIn.getName();
		
		try {
			Map<String, Object> prop = datasetIn.getProperties();
			DefaultImageMetadata metaData = (DefaultImageMetadata) prop.get("scifio.metadata.image");
			MetaTable metaTable = metaData.getTable();
		} catch (NullPointerException npe) {
			// TODO Auto-generated catch block
			//npe.printStackTrace();
			logService.info(this.getClass().getName() + " WARNING: It was not possible to read scifio metadata."); 
		}
  	
		logService.info(this.getClass().getName() + " Name: " + datasetName); 
		logService.info(this.getClass().getName() + " Image size = " + width+"x"+height); 
		logService.info(this.getClass().getName() + " Image type: " + imageType); 
		logService.info(this.getClass().getName() + " Number of images = "+ numSlices); 
		
		//RGB not allowed
		if (!imageType.equals("Grey")) { 
			logService.info(this.getClass().getName() + " WARNING: Grey value image volume expected!");
			this.cancel("WARNING: Grey value image volume expected!");
		}
	}

	/**
	* This method starts the workflow for a single image of the active display
	*/
	protected void startWorkflowForSingleVolume() {
	
		dlgProgress = new WaitingDialogWithProgressBar("Computing 3D fractal fragmentation indices, please wait... Open console window for further info.",
							logService, false, exec); //isCanceable = false, because no following method listens to exec.shutdown 
		dlgProgress.updatePercent("");
		dlgProgress.setBarIndeterminate(true);
		dlgProgress.setVisible(true);
    	deleteExistingDisplays();
        logService.info(this.getClass().getName() + " Processing volume...");
		processSingleInputVolume();
		dlgProgress.addMessage("Processing finished! Collecting data for table...");
		generateTableHeader();
		writeSingleResultToTable();
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
				if (display.getName().contains(tableOutName)) display.close();
			}			
		}
	}


	/** This method computes the maximal number of possible boxes*/
	private int getMaxBoxNumber(long width, long height, long depth) { 
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

		//resultValuesTable = new double[15]; //3x5 regression parameters
		resultValuesTable = new double[30]; //x2 for complex hull
		
		//get rai
		RandomAccessibleInterval<T> rai = null;	
	
		rai =  (RandomAccessibleInterval<T>) datasetIn.copy().getImgPlus(); //dim==3

		// Compute regression parameters
		double[] regressionValues = process(rai); //rai is 3D
		//D1	 0 Intercept,  1 Slope,  2 InterceptStdErr,  3 SlopeStdErr,  4 RSquared
		//Mass	 5 Intercept,  6 Slope,  7 InterceptStdErr,  8 SlopeStdErr,  9 RSquared
		//Perim	10 Intercept, 11 Slope, 12 InterceptStdErr, 13 SlopeStdErr, 14 RSquared
		
		//set values for output table
		for (int i = 0; i < regressionValues.length; i++ ) {
			resultValuesTable[i] = regressionValues[i]; 
		}
		//Compute dimension
		double dim = Double.NaN;	
		dim = -regressionValues[1];
		if ((choiceRadioButt_ScanningType.equals("Raster box"))  && (choiceRadioButt_ColorModelType.equals("Binary")));       //Do nothing, slope = dim	
		//if ((choiceRadioButt_ScanningType.equals("Sliding box")) && (choiceRadioButt_ColorModelType.equals("Binary")));
		//TO DO 
		
		double dimD1    = Double.NaN;
		double dimMass  = Double.NaN;
		double dimPerim = Double.NaN;
		dimD1    =  regressionValues[1]; //Slope for D1
		dimMass  = -regressionValues[6];
		dimPerim = -regressionValues[11];
		
		resultValuesTable[1]  = dimD1;
		resultValuesTable[6]  = dimMass;
		resultValuesTable[11] = dimPerim;
		logService.info(this.getClass().getName() + " 3D D mass: " + dimMass);
		
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
		Point3d[] points = new Point3d[numPoints];
		int p = 0;
		cursor = Views.iterable(rai).localizingCursor();
		int[] pos = new int[3];
		while(cursor.hasNext()) {
			cursor.next();
			cursor.localize(pos);		
			if (((UnsignedByteType) cursor.get()).getInteger() > 0 ) {
				points[p] = new Point3d(pos[0], pos[1], pos[2]) ;
				p += 1;
			}
		}
		
		//get vertices of convex hull
		QuickHull3D hull3D = new QuickHull3D();
		hull3D.build (points);
		Point3d[] vertices = hull3D.getVertices();
	
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
		ArrayList<GeoPoint> cubeVertices = new ArrayList<GeoPoint>();
		Point3d p3d;
		for (int v = 0; v < vertices.length; v++) {
			p3d = vertices[v];
			cubeVertices.add(new GeoPoint(p3d.x, p3d.y, p3d.z));
		}
		
		//delete quickhull3d classes to save memory
		points   = null;
		hull3D   = null;
		vertices = null;
		
		// Create polygon instance
		GeoPolygon polygonInst = new GeoPolygon(cubeVertices);
		// Create main process instance
		GeoPolygonProc procInst = new GeoPolygonProc(polygonInst);
		
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
		regressionValues = process(rai); //rai is 3D
		//D1	 0 Intercept,  1 Slope,  2 InterceptStdErr,  3 SlopeStdErr,  4 RSquared
		//Mass	 5 Intercept,  6 Slope,  7 InterceptStdErr,  8 SlopeStdErr,  9 RSquared
		//Perim	10 Intercept, 11 Slope, 12 InterceptStdErr, 13 SlopeStdErr, 14 RSquared
		
		//set values for output table
		for (int i = 0; i < regressionValues.length; i++ ) {
			resultValuesTable[15+i] = regressionValues[i]; 
		}
		//Compute dimension
		dim = Double.NaN;	
		dim = -regressionValues[1];
		if ((choiceRadioButt_ScanningType.equals("Raster box"))  && (choiceRadioButt_ColorModelType.equals("Binary")));       //Do nothing, slope = dim	
		//if ((choiceRadioButt_ScanningType.equals("Sliding box")) && (choiceRadioButt_ColorModelType.equals("Binary")));
		//TO DO 
		
		dimD1    = Double.NaN;
		dimMass  = Double.NaN;
		dimPerim = Double.NaN;
		dimD1    =  regressionValues[1]; //Slope for D1
		dimMass  = -regressionValues[6];
		dimPerim = -regressionValues[11];
		
		resultValuesTable[15+1]  = dimD1;
		resultValuesTable[15+6]  = dimMass;
		resultValuesTable[15+11] = dimPerim;
		logService.info(this.getClass().getName() + " 3D D mass-hull: " + dimMass);
		
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
		IntColumn columnRegMin             = new IntColumn("RegMin");
		IntColumn columnRegMax             = new IntColumn("RegMax");
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
		tableOut.add(columnRegMin);
		tableOut.add(columnRegMax);
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
	*  writes current result to table
	*  @param int slice number of active image.
	*/
	private void writeSingleResultToTable() { 

		int regMin            = spinnerInteger_RegMin;
		int regMax            = spinnerInteger_RegMax;
		int numBoxes          = spinnerInteger_NumBoxes;
		//String fractalDimType  = choiceRadioButt_FractalDimType;
		String scanningType   = choiceRadioButt_ScanningType;  //Raster box     Sliding box
		String colorModelType = choiceRadioButt_ColorModelType;	 //Binary  DBC   RDBC
		//D1        0 Intercept,  1 Dim, 2  InterceptStdErr, 3  SlopeStdErr,  4 RSquared
		//Mass      5 Intercept,  6 Dim, 7  InterceptStdErr, 8  SlopeStdErr,  9 RSquared
		//Boundary 10 Intercept, 11 Dim, 12 InterceptStdErr, 13 SlopeStdErr, 14 RSquared
		// fill table with values
		tableOut.appendRow();
		tableOut.set("File name",	 	 tableOut.getRowCount()-1, datasetName);	
		tableOut.set("# Boxes",		     tableOut.getRowCount()-1, numBoxes);
		tableOut.set("RegMin",			 tableOut.getRowCount()-1, regMin);
		tableOut.set("RegMax",			 tableOut.getRowCount()-1, regMax);
		tableOut.set("Scanning type",	 tableOut.getRowCount()-1, scanningType);
		tableOut.set("Color model",		 tableOut.getRowCount()-1, colorModelType);
		//table.set("Fractal dimension type",   table.getRowCount()-1, fractalDimType);	
		tableOut.set("3D FFI",         	 tableOut.getRowCount()-1, resultValuesTable[6] - resultValuesTable[11]); //FFI
		tableOut.set("3D FFDI",          tableOut.getRowCount()-1, resultValuesTable[1]*(1.0-(resultValuesTable[6] - resultValuesTable[11]))); //FFDI = D1(1-FFI)
		tableOut.set("3D FTI",         	 tableOut.getRowCount()-1, (resultValuesTable[15+6] - resultValuesTable[15+11])-(resultValuesTable[6] - resultValuesTable[11])); //FFI hull - FFI
		tableOut.set("3D D1",         	 tableOut.getRowCount()-1, resultValuesTable[1]); //D1
		tableOut.set("3D Dmass",         tableOut.getRowCount()-1, resultValuesTable[6]); //Dmass
		tableOut.set("3D Dboundary",     tableOut.getRowCount()-1, resultValuesTable[11]); //D-boundary
		tableOut.set("R2 D1",            tableOut.getRowCount()-1, resultValuesTable[4]);
		tableOut.set("R2 Dmass",         tableOut.getRowCount()-1, resultValuesTable[9]);
		tableOut.set("R2 Dboundary",     tableOut.getRowCount()-1, resultValuesTable[14]);
		tableOut.set("3D CH_D1",         tableOut.getRowCount()-1, resultValuesTable[15+1]); //D1   Complex Hull values
		tableOut.set("3D CH_D-mass",     tableOut.getRowCount()-1, resultValuesTable[15+6]); //Dmass
		tableOut.set("3D CH_D-boundary", tableOut.getRowCount()-1, resultValuesTable[15+11]); //D-boundary
		tableOut.set("CH_R2-D1",         tableOut.getRowCount()-1, resultValuesTable[15+4]);
		tableOut.set("CH_R2-mass",       tableOut.getRowCount()-1, resultValuesTable[15+9]);
		tableOut.set("CH_R2-boundary",   tableOut.getRowCount()-1, resultValuesTable[15+14]);
	}

	/**
	*
	* Processing
	*/
	private double[] process(RandomAccessibleInterval<?> rai) { //3Dvolume
	
		logService.info(this.getClass().getName() + " Processing of volume started");
		if (rai == null) {
			logService.info(this.getClass().getName() + " WARNING: rai==null, no image for processing!");
		}
		
		int regMin            = spinnerInteger_RegMin;
		int regMax            = spinnerInteger_RegMax;
		int numBoxes          = spinnerInteger_NumBoxes;
		String fractalDimType = "Box counting"; // choiceRadioButt_FractalDimType;
		//NOTE: IF minQ IS CHANGED, THE lnDatY[q] FOR THE REGRESSION MUST ALSO BE CHANGED***********************************************
		int minQ            = 0; //NOTE!!!!  //spinnerInteger_MinQ;
		int maxQ            = 2; //spinnerInteger_MaxQ;
		String scanningType   = choiceRadioButt_ScanningType;    //Raster box     Sliding box
		String colorModelType = choiceRadioButt_ColorModelType;	 //Binary  DBC   RDBC
		int pixelPercentage   = 100; //spinnerInteger_PixelPercentage;
		boolean optShowPlot   = booleanShowDoubleLogPlot;


		if ((!colorModelType.equals("Binary")) && (regMin == 1)){
			regMin = 2; //regMin == 1 (single pixel box is not possible for DBC algorithms)
		}

		long width  = rai.dimension(0);
		long height = rai.dimension(1);
		long depth  = rai.dimension(2);
		
		double[] regressionParams = null;
	
		String plot_method = "3D Fractal fragmentation indices";
		String xAxis = "ln(eps)";
		String yAxis = "ln(Count)";
		
		GeneralizedDim3DMethods   gd3DMass  = null;
		BoxCounting3DMethods      bc3DMass  = null;
		BoxCounting3DMethods      bc3DPerim = null;
		
		if (imageType.equals("Grey")) {// grey image   //additional check, is already checked during validation of active dataset
			//*****************************************************************************************************************************************
			
			//D1
			gd3DMass = new GeneralizedDim3D_Grey(rai, numBoxes, minQ, maxQ, scanningType, colorModelType, pixelPercentage, dlgProgress, statusService);	
			
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
		double[][] totalsGen         = new double[numQ][numBoxes]; //several Generalized dims will be computed but only one will be taken in the end
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
					if (totalsGen[q][n] <= 1) {
						//lnTotals[numBoxes - n - 1] = 0.0; //Math.log(Float.MIN_VALUE); // damit logarithmus nicht undefiniert ist//IQM
						lnTotalsGen[q][n] = 0.0;
					} else if (Double.isNaN(totalsGen[q][n])) {
						//lnTotals[numBoxes - n - 1] = 0.0;
						lnTotalsGen[q][n] = Double.NaN;
					} else {
						//lnTotals[numBoxes - n - 1] = Math.log(totals[n]);//IQM
						if ((q + minQ) != 1)
							lnTotalsGen[q][n] = Math.log(totalsGen[q][n]);
						if ((q + minQ) == 1) //D1
							lnTotalsGen[q][n] = totalsGen[q][n];
					}
				}
				//Dmass
				if (totalsMass[n] <= 1) {
					//lnTotals[numBoxes - n - 1] = 0.0; //Math.log(Float.MIN_VALUE); // damit logarithmus nicht undefiniert ist//IQM
					lnTotalsMass[n] = 0.0;
				} else if (Double.isNaN(totalsMass[n])) {
					//lnTotals[numBoxes - n - 1] = 0.0;
					lnTotalsMass[n] = Double.NaN;
				} else {
					//lnTotals[numBoxes - n - 1] = Math.log(totals[n]);//IQM
					lnTotalsMass[n] = Math.log(totalsMass[n]); //
				}
				//Dboundary
				if (totalsBoundary[n] <= 1) {
					//lnTotals[numBoxes - n - 1] = 0.0; //Math.log(Float.MIN_VALUE); // damit logarithmus nicht undefiniert ist//IQM
					lnTotalsBoundary[n] = 0.0;
				} else if (Double.isNaN(totalsBoundary[n])) {
					//lnTotals[numBoxes - n - 1] = 0.0;
					lnTotalsBoundary[n] = Double.NaN;
				} else {
					//lnTotals[numBoxes - n - 1] = Math.log(totals[n]);//IQM
					lnTotalsBoundary[n] = Math.log(totalsBoundary[n]); //
				}
				
				//lnEps[n] = Math.log(eps[numBoxes - n - 1 ]); //IQM
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
			
			RegressionPlotFrame doubleLogPlot = DisplayRegressionPlotXY(lnDataX, lnDataY, isLineVisible, "Double Log Plots - fractal fragmentation indices", 
					preName + datasetName, xAxisLabel, yAxisLabel, legendLabels,
					regMin, regMax);
			doubleLogPlotList.add(doubleLogPlot);
		}
				
		// Compute regressions
		logService.info(this.getClass().getName() + " Computing of regressions.....");
		LinearRegression lr = new LinearRegression();
		double[] regressionParamsD1 = lr.calculateParameters(lnDataX, lnDataY[0], regMin, regMax);
		lr = new LinearRegression();
		double[] regressionParamsMass = lr.calculateParameters(lnDataX, lnDataY[1], regMin, regMax);
		lr = new LinearRegression();
		double[] regressionParamsPerim = lr.calculateParameters(lnDataX, lnDataY[2], regMin, regMax);
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
			
		return regressionParams;
		//Output
		//uiService.show(tableOutName, table);
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
	 * @param regMin minimum value for regression range
	 * @param regMax maximal value for regression range 
	 * @param optDeleteExistingPlot option if existing plot should be deleted before showing a new plot
	 * @param interpolType The type of interpolation
	 * @return RegressionPlotFrame
	 */			
	private RegressionPlotFrame DisplayRegressionPlotXY(double[] dataX, double[][] dataY, boolean isLineVisible,
			String frameTitle, String plotLabel, String xAxisLabel, String yAxisLabel, String[] legendLabels, int regMin, int regMax) {
		// jFreeChart
		RegressionPlotFrame pl = new RegressionPlotFrame(dataX, dataY, isLineVisible, frameTitle, plotLabel, xAxisLabel,
				yAxisLabel, legendLabels, regMin, regMax);
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
		// ij.command().run(Csaj3DFractalFragmentation.class,
		// true).get().getOutput("image");
		ij.command().run(Csaj3DFractalFragmentation.class, true);
	}
}
