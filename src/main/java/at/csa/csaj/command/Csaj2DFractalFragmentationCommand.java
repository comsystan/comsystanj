/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj2DFractalFragmentation.java
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
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.display.ImageDisplayService;
import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.morphology.Erosion;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.roi.Masks;
import net.imglib2.roi.Regions;
import net.imglib2.roi.geom.GeomMasks;
import net.imglib2.roi.geom.real.WritablePolygon2D;
import net.imglib2.type.Type;
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
import org.scijava.widget.FileWidget;
import org.scijava.widget.NumberWidget;

import at.csa.csaj.commons.Algorithm_ConvexHull2D;
import at.csa.csaj.commons.Container_Items;
import at.csa.csaj.commons.Dialog_WaitingWithProgressBar;
import at.csa.csaj.commons.Plot_RegressionFrame;
import at.csa.csaj.commons.Regression_Linear;
import at.csa.csaj.commons.Container_ProcessMethod;
import io.scif.DefaultImageMetadata;
import io.scif.MetaTable;

/**
 * A {@link ContextCommand} plugin computing
 * <the fractal fragmentation indices</a>
 * of an image.
 */
@Plugin(type = ContextCommand.class,
	headless = true,
	label = "Fractal fragmentation indices",
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
public class Csaj2DFractalFragmentationCommand<T extends RealType<T>> extends ContextCommand implements Previewable {
	
	private static final String PLUGIN_LABEL            = "<html><b>Computes Fractal fragmentation indices</b></html>";
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
	private static String[] sliceLabels;
	private static long width  = 0;
	private static long height = 0;
	private static long numDimensions = 0;
	private static long numSlices = 0;
	private static long compositeChannelCount =0;
	private static String imageType = "";
	private static int  numBoxes = 0;
	private static ArrayList<Plot_RegressionFrame> doubleLogPlotList = new ArrayList<Plot_RegressionFrame>();
	
	private static final String tableOutName = "Table - Fractal fragmentation indices";
	
	private Dialog_WaitingWithProgressBar dlgProgress;
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
	
	//Input dataset which is updated in callback functions
	@Parameter (type = ItemIO.INPUT)
	private Dataset datasetIn;

	@Parameter(label = tableOutName, type = ItemIO.OUTPUT)
	private DefaultGenericTable tableOut;

	
   //Widget elements------------------------------------------------------
	//-----------------------------------------------------------------------------------------------------
    //@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	//private final String labelPlugin = PLUGIN_LABEL;

    //@Parameter(label = " ", visibility = ItemVisibility.MESSAGE,  persist = false)
  	//private final String labelSpace = SPACE_LABEL;
    
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
//     @Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
//     private final String labelMethodOptions = METHODOPTIONS_LABEL;
//     
//     @Parameter(label = "Fractal dimension",
//	    description = "Type of fractal dimension computation",
//	    style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
//	    choices = {"Box counting", "Pyramid"},
//	    //persist = false,  //restore previous value default = true
//	    initializer = "initialFracalDimType",
//	    callback = "callbackFractalDimType")
//     private String choiceRadioButt_FractalDimType;
     
//     @Parameter(label = "Scanning method",
//    		    description = "Type of box scanning",
//    		    style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
//      		    choices = {"Raster box"} //, "Sliding box"}, //does not give the right dimension values
//      		    //persist = false,  //restore previous value default = true
//    		    initializer = "initialScanningType",
//                callback = "callbackScanningType")
//     private String choiceRadioButt_ScanningType;
//     
//     @Parameter(label = "Color model",
// 		    description = "Type of image and computation",
// 		    style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
//   		    choices = {"Binary"}, //grey value does make sense "DBC", "RDBC"},
//   		    //persist = false,  //restore previous value default = true
// 		    initializer = "initialColorModelType",
//             callback = "callbackColorModelType")
//     private String choiceRadioButt_ColorModelType;
     
 	//-----------------------------------------------------------------------------------------------------
     @Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
     private final String labelDisplayOptions = DISPLAYOPTIONS_LABEL;
      
     @Parameter(label = "Show double log plot",
    		    persist = true,  //restore previous value default = true
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
//	@Parameter(label = "Process single active image ", callback = "callbackProcessActiveImage")
//	private Button buttonProcessActiveImage;
     
	@Parameter(label = "Process all available images", callback = "callbackProcessAllImages")
	private Button buttonProcessAllImages;


    //---------------------------------------------------------------------
 
    //The following initializer functions set initial values	
	protected void initialPluginLaunch() {
		checkItemIOIn();
	}
    protected void initialNumBoxes() {
    	if (datasetIn == null) {
    		logService.error(this.getClass().getName() + " ERROR: Input image = null");
    		cancel("ComsystanJ 2D plugin cannot be started - missing input image.");
    		return;
    	} else {
    		numBoxes = getMaxBoxNumber(datasetIn.dimension(0), datasetIn.dimension(1));
    	}
      	spinnerInteger_NumBoxes = numBoxes;
    }
    protected void initialNumRegStart() {
    	spinnerInteger_NumRegStart = 1;
    }
    protected void initialNumRegEnd() {
    	if (datasetIn == null) {
    		logService.error(this.getClass().getName() + " ERROR: Input image = null");
    		cancel("ComsystanJ 2D plugin cannot be started - missing input image.");
    		return;
    	} else {
    		numBoxes = getMaxBoxNumber(datasetIn.dimension(0), datasetIn.dimension(1));
    	}
    	spinnerInteger_NumRegEnd =  numBoxes;
    }
//    protected void initialFractalDimType() {
//    	choiceRadioButt_FractalDimType = "Box counting";
//    }
//    protected void initialScanningType() {
//    	choiceRadioButt_ScanningType = "Raster box";
//    }
//    protected void initialColorModelType() {
//    	choiceRadioButt_ColorModelType = "Binary";
//    }
    protected void initialShowDoubleLogPlots() {
    	booleanShowDoubleLogPlot = true;
    }
	protected void initialShowConvexHull() {
		booleanShowConvexHull = true;
	}
    protected void initialOverwriteDisplays() {
    	booleanOverwriteDisplays = true;
    }
	protected void initialNumImageSlice() {
    	spinnerInteger_NumImageSlice = 1;
	}
	// ------------------------------------------------------------------------------
	
	/** Executed whenever the {@link #spinnerInteger_NumBoxes} parameter changes. */
	protected void callbackNumBoxes() {
		
		if  (spinnerInteger_NumBoxes < 3) {
			spinnerInteger_NumBoxes = 3;
		}
		int numMaxBoxes = getMaxBoxNumber(datasetIn.dimension(0), datasetIn.dimension(1));	
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

//	/** Executed whenever the {@link #choiceRadioButtFractalDimType} parameter changes. */
//	protected void callbackFractalDimType() {
//		logService.info(this.getClass().getName() + " Fractal dimension computation method set to " + choiceRadioButt_FractalDimType);
//		
//	}
	
//	/** Executed whenever the {@link #choiceRadioButtScanningType} parameter changes. */
//	protected void callbackScanningType() {
//		logService.info(this.getClass().getName() + " Box method set to " + choiceRadioButt_ScanningType);
//		
//	}
//	
//	/** Executed whenever the {@link #choiceRadioButt_ColorModelType} parameter changes. */
//	protected void callbackColorModelType() {
//		logService.info(this.getClass().getName() + " Color model type set to " + choiceRadioButt_ColorModelType);
//		
//	}
	
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
	        	startWorkflowForAllImages();
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
		    	    startWorkflowForSingleImage();
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
		//numSlices = dataset.getDepth(); //does not work if third axis ist not specifyed as z-Axis
		
		numDimensions = datasetIn.numDimensions();
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
		logService.info(this.getClass().getName() + " Name: " + datasetName); 
		logService.info(this.getClass().getName() + " Image size: " + width+"x"+height); 
		logService.info(this.getClass().getName() + " Image type: " + imageType); 
		logService.info(this.getClass().getName() + " Number of images = "+ numSlices); 
		
		//RGB not allowed
		if (!imageType.equals("Grey")) { 
			logService.warn(this.getClass().getName() + " WARNING: Grey value image(s) expected!");
			cancel("ComsystanJ 2D plugin cannot be started - grey value image(s) expected!");
		}
	}
	
	/**
	* This method starts the workflow for a single image of the active display
	*/
	protected void startWorkflowForSingleImage() {

		dlgProgress = new Dialog_WaitingWithProgressBar("Computing fractal fragmentation indices, please wait... Open console window for further info.",
				logService, false, exec); //isCanceable = false, because no following method listens to exec.shutdown 
		dlgProgress.updatePercent("");
		dlgProgress.setBarIndeterminate(true);
		dlgProgress.setVisible(true);

		deleteExistingDisplays();
		generateTableHeader();
 		int sliceIndex = spinnerInteger_NumImageSlice - 1;
        logService.info(this.getClass().getName() + " Processing single image " + (sliceIndex + 1));
		processSingleInputImage(sliceIndex);
	
		dlgProgress.addMessage("Processing finished! Collecting data for table...");
		dlgProgress.setVisible(false);
		dlgProgress.dispose();
		Toolkit.getDefaultToolkit().beep();
	}
	
	/**
	* This method starts the workflow for all images of the active display
	*/
	protected void startWorkflowForAllImages() {
	
		dlgProgress = new Dialog_WaitingWithProgressBar("Computing fractal fragmentation indices, please wait... Open console window for further info.",
					logService, false, exec); //isCanceable = true, because processAllInputImages(dlgProgress) listens to exec.shutdown 
		dlgProgress.setVisible(true);
		
    	logService.info(this.getClass().getName() + " Processing all available images");
		deleteExistingDisplays();
		generateTableHeader();
		processAllInputImages();
	
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
					//doubleLogPlotList.remove(l);  /
				}
				doubleLogPlotList.clear();		
			}
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
	private int getMaxBoxNumber(long width, long height) { 
		float boxWidth = 1f;
		int number = 1; 
		while ((boxWidth <= width) && (boxWidth <= height)) {
			boxWidth = boxWidth * 2;
			number = number + 1;
		}
		return number - 1;
	}
	
	/** This method takes the active image and computes results. 
	 *
	 */
	private void processSingleInputImage(int s) {
		long startTime = System.currentTimeMillis();
			
		//convert to float values
		//Img<T> image = (Img<T>) dataset.getImgPlus();
		//mg<FloatType> imgFloat; // = opService.convert().float32((Img<T>)dataset.getImgPlus());

		RandomAccessibleInterval<?> rai = null;	
		if( (s==0) && (numSlices == 1) && (numDimensions == 2) ) { // for only one 2D image;
			rai =  (RandomAccessibleInterval<?>) datasetIn.copy().getImgPlus(); //copy() because rai will be changed for convex hull

		} else if ( (numSlices > 1) && (numDimensions == 3) ){ // for a stack of 2D images
			rai = (RandomAccessibleInterval<?>) Views.hyperSlice(datasetIn.copy(), 2, s); //copy() because rai will be changed for convex hull	
		}
		
		//Compute regression parameters
		Container_ProcessMethod containerPM = process(rai, s);	
		//D1	 0 Intercept,  1 D1,  2 InterceptStdErr,  3 SlopeStdErr,  4 RSquared
		//Mass	 5 Intercept,  6 DMass,  7 InterceptStdErr,  8 SlopeStdErr,  9 RSquared
		//Perim	10 Intercept, 11 DPerim, 12 InterceptStdErr, 13 SlopeStdErr, 14 RSquared
			
		logService.info(this.getClass().getName() + " Dmass: " + containerPM.item1_Values[6]);
		//writeToTable(0, s, containerPM); //write always to the first row //later because convex hull
			
		//Do it again for convex Hull**************************************************
//      This did not work for non compact objects 
//		//Create Bittype imgBit
//		imgBit = createImgBit(rai);
//		//Polygon2D poly = ij.convert().convert(imgBit, Polygon2D.class);//is always null
//		poly = ij.op().geom().contour(imgBit, true);
//		defaultConvexHull2D = new DefaultConvexHull2D();
//		hull = defaultConvexHull2D.calculate(poly);
//		
//		//areas for checking
//		DoubleType areaPoly  = (DoubleType) opService.run("geom.size", poly);
//		DoubleType areaHull1 = (DoubleType) opService.run("geom.size", hull);
//		DoubleType areaHull2 = (DoubleType) opService.run("geom.sizeConvexHull", poly); //same result
//		
//		RandomAccessibleInterval raiPoly = Views.interval(Masks.toIterableRegion(poly), rai);
//		RandomAccessibleInterval raiHull = Views.interval(Masks.toIterableRegion(hull), rai);
//		
//		cursor = Regions.sample(Masks.toIterableRegion(hull), rai).localizingCursor();
//		while(cursor.hasNext()) {
//			cursor.next();
//			((UnsignedByteType) cursor.get()).set(255); //This changes the rai
//		}
//		uiService.show("raiPoly", raiPoly);
//		uiService.show("raiHull", raiHull);
//		if (booleanShowConvexHull) uiService.show("Convex hull", rai);
			
		//get list of object points	
		List<Point> pointList = new ArrayList<>();
		cursor = Views.iterable(rai).localizingCursor();
		int[] pos = new int[2];
		while(cursor.hasNext()) {
			cursor.next();
			cursor.localize(pos);
			
			if (((UnsignedByteType) cursor.get()).getInteger() > 0 ) {
				pointList.add(new Point(pos[0], pos[1])) ;
			}
		}
		
		Algorithm_ConvexHull2D convexHull2D = new Algorithm_ConvexHull2D();
		List<Point> hullList = convexHull2D.convexHull(pointList);
		
//		WritablePolygon2D hull = GeomMasks.polygon2D(hullPointsX, hullPointsY);
		WritablePolygon2D hull = GeomMasks.polygon2D(hullList);
		
		//RandomAccessibleInterval raiPoly = Views.interval(Masks.toIterableRegion(poly), rai);
		RandomAccessibleInterval raiHull = Views.interval(Masks.toIterableRegion(hull), rai);
		
		cursor = Regions.sample(Masks.toIterableRegion(hull), rai).localizingCursor();
		while(cursor.hasNext()) {
			cursor.next();
			((UnsignedByteType) cursor.get()).set(255); //This changes the rai
		}
		
//		uiService.show("raiHull", raiHull);
		if (booleanShowConvexHull) uiService.show("Convex hull", rai);
			
		//Compute regression parameters
		Container_ProcessMethod containerPMCH = process(rai, s);	
		//D1	 0 Intercept,  1 D1,  2 InterceptStdErr,  3 SlopeStdErr,  4 RSquared
		//Mass	 5 Intercept,  6 DMass,  7 InterceptStdErr,  8 SlopeStdErr,  9 RSquared
		//Perim	10 Intercept, 11 DPerim, 12 InterceptStdErr, 13 SlopeStdErr, 14 RSquared
			
		logService.info(this.getClass().getName() + " Dmass-convexhull : " + containerPMCH.item1_Values[6]);
		
		int length = containerPM.item1_Values.length; //15
		double[] resultValues = new double[2*length];
		for (int i=0; i < length; i++) {
			resultValues[i]    = containerPM.item1_Values[i];
			resultValues[15+i] = containerPMCH.item1_Values[i]; //Convex hull
		}
		
		Container_Items containerItems = new Container_Items(resultValues, containerPM.item2_Values);
		writeToTable(0, s, containerItems); //write always to the first row
		 
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
	
	/** This method loops over all input images and computes results. 
	 *
	 **/
	private void processAllInputImages() {
		
		long startTimeAll = System.currentTimeMillis();
	
		//convert to float values
		//Img<T> image = (Img<T>) dataset.getImgPlus();
		//Img<FloatType> imgFloat; // = opService.convert().float32((Img<T>)dataset.getImgPlus());
	
		Container_ProcessMethod containerPM;
		Container_ProcessMethod containerPMCH;
		//loop over all slices of stack
		for (int s = 0; s < numSlices; s++){ //p...planes of an image stack
			//if (!exec.isShutdown()) {
				int percent = (int)Math.round((  ((float)s)/((float)numSlices)   *100.f   ));
				dlgProgress.updatePercent(String.valueOf(percent+"%"));
				dlgProgress.updateBar(percent);
				//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
				statusService.showStatus((s+1), (int)numSlices, "Processing " + (s+1) + "/" + (int)numSlices);
	//			try {
	//				Thread.sleep(3000);
	//			} catch (InterruptedException e) {
	//				// TODO Auto-generated catch block
	//				e.printStackTrace();
	//			}
				
				long startTime = System.currentTimeMillis();
				logService.info(this.getClass().getName() + " Processing image number " + (s+1) + "(" + numSlices + ")");
				//get slice and convert to float values
				//imgFloat = opService.convert().float32((Img<T>)dataset.gett);	
				
				RandomAccessibleInterval<?> rai = null;	
				if( (s==0) && (numSlices == 1) && (numDimensions == 2) ) { // for only one 2D image;
					rai =  (RandomAccessibleInterval<?>) datasetIn.copy().getImgPlus(); //copy() because rai will be changed for convex hull
	
				} else if ( (numSlices > 1) && (numDimensions == 3) ){ // for a stack of 2D images
					rai = (RandomAccessibleInterval<?>) Views.hyperSlice(datasetIn.copy(), 2, s); //copy() because rai will be changed for convex hull
				
				}
				//Compute regression parameters
				containerPM = process(rai, s);	
				//D1	 0 Intercept,  1 Slope,  2 InterceptStdErr,  3 SlopeStdErr,  4 RSquared
				//Mass	 5 Intercept,  6 Slope,  7 InterceptStdErr,  8 SlopeStdErr,  9 RSquared
				//Perim	10 Intercept, 11 Slope, 12 InterceptStdErr, 13 SlopeStdErr, 14 RSquared
				
				logService.info(this.getClass().getName() + " Dmass: " + containerPM.item1_Values[6]);
				//writeToTable(0, s, containerPM); //write always to the first row //later because convex hull
				
				//Do it again for convex Hull**************************************************
//				//Create Bittype imgBit
//				imgBit = createImgBit(rai);
//				//Polygon2D poly = ij.convert().convert(imgBit, Polygon2D.class);//is always null
//				poly = ij.op().geom().contour(imgBit, false );
//				defaultConvexHull2D = new DefaultConvexHull2D();
//				hull = defaultConvexHull2D.calculate(poly);
//				
//				//areas to check 
//				DoubleType areaPoly  = (DoubleType) opService.run("geom.size", poly);
//				DoubleType areaHull1 = (DoubleType) opService.run("geom.size", hull);
//				DoubleType areaHull2 = (DoubleType) opService.run("geom.sizeConvexHull", poly); //same result
//				
//				cursor = Regions.sample(Masks.toIterableRegion(hull), rai).localizingCursor();
//				while(cursor.hasNext()) {
//					cursor.next();
//					((UnsignedByteType) cursor.get()).set(255); //This changes the rai
//				}
//				//if (booleanShowConvexHull) uiService.show("Convex hull", rai);
				
				//get list of object points	
				List<Point> pointList = new ArrayList<>();
				cursor = Views.iterable(rai).localizingCursor();
				int[] pos = new int[2];
				while(cursor.hasNext()) {
					cursor.next();
					cursor.localize(pos);
					
					if (((UnsignedByteType) cursor.get()).getInteger() > 0 ) {
						pointList.add(new Point(pos[0], pos[1])) ;
					}
				}
				
				Algorithm_ConvexHull2D convexHull2D = new Algorithm_ConvexHull2D();
				List<Point> hullList = convexHull2D.convexHull(pointList);
				
//				WritablePolygon2D hull = GeomMasks.polygon2D(hullPointsX, hullPointsY);
				WritablePolygon2D hull = GeomMasks.polygon2D(hullList);
				
				//RandomAccessibleInterval raiPoly = Views.interval(Masks.toIterableRegion(poly), rai);
				RandomAccessibleInterval raiHull = Views.interval(Masks.toIterableRegion(hull), rai);
				
				cursor = Regions.sample(Masks.toIterableRegion(hull), rai).localizingCursor();
				while(cursor.hasNext()) {
					cursor.next();
					((UnsignedByteType) cursor.get()).set(255); //This changes the rai
				}
				if (booleanShowConvexHull) uiService.show("Convex hull", rai);
				//Compute regression parameters
				containerPMCH = process(rai, s);	
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
				
				Container_Items containerItems = new Container_Items(resultValues, containerPM.item2_Values);
				writeToTable(s, s, containerItems);
				
				long duration = System.currentTimeMillis() - startTime;
				TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
				SimpleDateFormat sdf = new SimpleDateFormat();
				sdf.applyPattern("HHH:mm:ss:SSS");
				logService.info(this.getClass().getName() + " Elapsed time: "+ sdf.format(duration));
			//}
		} //s
		statusService.showProgress(0, 100);
		statusService.clearStatus();
		
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
		
		long duration = System.currentTimeMillis() - startTimeAll;
		TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
		SimpleDateFormat sdf = new SimpleDateFormat();
		sdf.applyPattern("HHH:mm:ss:SSS");
		logService.info(this.getClass().getName() + " Elapsed processing time for all image(s): "+ sdf.format(duration));
	}
	
	/** Generates the table header {@code DefaultGenericTable} */
	private void generateTableHeader(){
		
		GenericColumn columnFileName       = new GenericColumn("File name");
		GenericColumn columnSliceName      = new GenericColumn("Slice name");
		IntColumn columnMaxNumBoxes        = new IntColumn("# Boxes");
		GenericColumn columnNumRegStart    = new GenericColumn("Reg Start");
		GenericColumn columnNumRegEnd      = new GenericColumn("Reg End");
		//GenericColumn columnFractalDimType = new GenericColumn("Fractal dimension type");
		DoubleColumn columnFFI             = new DoubleColumn("FFI");
		DoubleColumn columnFFDI            = new DoubleColumn("FFDI");
		DoubleColumn columnFTI             = new DoubleColumn("FTI");
		DoubleColumn columnD1              = new DoubleColumn("D1");
		DoubleColumn columnDmass           = new DoubleColumn("D-mass");
		DoubleColumn columnDbound          = new DoubleColumn("D-boundary");
		DoubleColumn columnR2d1            = new DoubleColumn("R2-D1");
		DoubleColumn columnR2mass          = new DoubleColumn("R2-mass");
		DoubleColumn columnR2bound         = new DoubleColumn("R2-boundary");
		DoubleColumn columnCH_D1           = new DoubleColumn("CH_D1");  //CH----ComplexHull
		DoubleColumn columnCH_Dmass        = new DoubleColumn("CH_D-mass");
		DoubleColumn columnCH_Dbound       = new DoubleColumn("CH_D-boundary");
		DoubleColumn columnCH_R2d1         = new DoubleColumn("CH_R2-D1");
		DoubleColumn columnCH_R2mass       = new DoubleColumn("CH_R2-mass");
		DoubleColumn columnCH_R2bound      = new DoubleColumn("CH_R2-boundary");
	
	    tableOut = new DefaultGenericTable();
		tableOut.add(columnFileName);
		tableOut.add(columnSliceName);
		tableOut.add(columnMaxNumBoxes);
		tableOut.add(columnNumRegStart);
		tableOut.add(columnNumRegEnd);
		//table.add(columnFractalDimType);
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
	 * @param int numRow to write in the result table
	 * @param int numSlice sclice number of images from datasetIn.
	 * @param Container_Items containerItems
	 */
	private void writeToTable(int numRow, int numSlice, Container_Items containerItems) { 
	
		int numRegStart = spinnerInteger_NumRegStart;
		int numRegEnd   = spinnerInteger_NumRegEnd;
		int numImages   = spinnerInteger_NumBoxes;
		//String fractalDimType  = choiceRadioButt_FractalDimType;
		//String scanningType    = choiceRadioButt_ScanningType;	
		//String colorModelType  = choiceRadioButt_ColorModelType;	

		int row = numRow;
	    int s = numSlice;	
	    	//D1        0 Intercept,  1 Dim, 2  InterceptStdErr, 3  SlopeStdErr,  4 RSquared
			//Mass      5 Intercept,  6 Dim, 7  InterceptStdErr, 8  SlopeStdErr,  9 RSquared
		    //Boundary 10 Intercept, 11 Dim, 12 InterceptStdErr, 13 SlopeStdErr, 14 RSquared
			//fill table with values
			tableOut.appendRow();
			tableOut.set("File name",   	 row, datasetName);	
			if (sliceLabels != null) 	     tableOut.set("Slice name", row, sliceLabels[s]);
			tableOut.set("# Boxes",    	     row, numImages);	
			tableOut.set("Reg Start",        row, "("+numRegStart+")" + containerItems.item2_Values[0]); //(NumRegStart)epsRegStart
			tableOut.set("Reg End",      	 row, "("+numRegEnd+")"   + containerItems.item2_Values[1]); //(NumRegEnd)epsRegEnd
			//table.set("Fractal dimension type",   table.getRowCount()-1, fractalDimType);	
			tableOut.set("FFI",         	 row, containerItems.item1_Values[6] - containerItems.item1_Values[11]); //FFI
			tableOut.set("FFDI",         	 row, containerItems.item1_Values[1]*(1.0-(containerItems.item1_Values[6] - containerItems.item1_Values[11]))); //FFDI = D1(1-FFI)
			tableOut.set("FTI",         	 row, (containerItems.item1_Values[15+6] - containerItems.item1_Values[15+11])-(containerItems.item1_Values[6] - containerItems.item1_Values[11])); //FFI hull - FFI
			tableOut.set("D1",         	     row, containerItems.item1_Values[1]); //D1
			tableOut.set("D-mass",           row, containerItems.item1_Values[6]); //Dmass
			tableOut.set("D-boundary",       row, containerItems.item1_Values[11]); //D-boundary
			tableOut.set("R2-D1",         	 row, containerItems.item1_Values[4]);
			tableOut.set("R2-mass",          row, containerItems.item1_Values[9]);
			tableOut.set("R2-boundary",      row, containerItems.item1_Values[14]);
			tableOut.set("CH_D1",         	 row, containerItems.item1_Values[15+1]); //D1   Complex Hull values
			tableOut.set("CH_D-mass",        row, containerItems.item1_Values[15+6]); //Dmass
			tableOut.set("CH_D-boundary",    row, containerItems.item1_Values[15+11]); //D-boundary
			tableOut.set("CH_R2-D1",         row, containerItems.item1_Values[15+4]);
			tableOut.set("CH_R2-mass",       row, containerItems.item1_Values[15+9]);
			tableOut.set("CH_R2-boundary",   row, containerItems.item1_Values[15+14]);
	}
								
	/** 
	 * Processing ****************************************************************************************
	 * */
	private Container_ProcessMethod process(RandomAccessibleInterval<?> rai, int plane) { //plane plane (Image) number

		if (rai == null) {
			logService.info(this.getClass().getName() + " WARNING: rai==null, no image for processing!");
		}
		
		int numRegStart = spinnerInteger_NumRegStart;
		int numRegEnd   = spinnerInteger_NumRegEnd;
		int numBoxes    = spinnerInteger_NumBoxes;
		
		//String scanningType    = choiceRadioButt_ScanningType;	
		//String colorModelType  = choiceRadioButt_ColorModelType;	
//		if ((!colorModelType.equals("Binary")) && (numRegStart == 1)){
//			numRegStart = 2; //numRegStart == 1 (single pixel box is not possible for DBC algorithms)
//		}	
		
		String fractalDimType  = "Box counting"; //choiceRadioButt_FractalDimType;
		//NOTE: IF minQ IS CHANGED, THE lnDatY[q] FOR THE REGRESSION MUST ALSO BE CHANGED***********************************************
		int minQ            = 0; //NOTE!!!!  //spinnerInteger_MinQ;
		int maxQ            = 2; //spinnerInteger_MaxQ;
		String scanningType    = "Raster box";
		int pixelPercentage    = 100;//spinnerInteger_PixelPercentage;
		String colorModelType  = "Binary";	
			
		boolean optShowPlot    = booleanShowDoubleLogPlot;
		
		long width  = rai.dimension(0);
		long height = rai.dimension(1);
		
		String imageType = "8-bit";  //  "RGB"....
	
		double[] epsRegStartEnd   = new double[2];  // epsRegStart, epsRegEnd
		double[] regressionParams = null;
		double[] resultValues = null;
		
		//Convert image to float
		//Img<T> image = (Img<T>) dataset.getImgPlus();
		//RandomAccessibleInterval<T> rai = (RandomAccessibleInterval<T>)dataset.getImgPlus();
		//IterableInterval ii = dataset.getImgPlus();
		//Img<FloatType> imgFloat = opService.convert().float32(ii);
		
		int numQ = maxQ - minQ + 1;
		double[][] totalsGen         = new double[numQ][numBoxes]; //several Generalised dims will be computed but only one will be taken in the end
		double[]   totalsGenMax      = new double[numBoxes];
		double[]   totalsMass        = new double[numBoxes];
		//double[]     totalsMassMax     = new double[numBands]; //for binary images
		double[]   totalsBoundary    = new double[numBoxes];
		//double[]     totalsBoundaryMax = new double[numBands]; //for binary images
		int[]      eps               = new int[numBoxes];
		
		// definition of eps
		for (int n = 0; n < numBoxes; n++) {
			eps[n] = (int)Math.round(Math.pow(2, n));
			//logService.info(this.getClass().getName() + " n:" + n + " eps:  " + eps[n]);	
		}	
			
		//********************************D1
		long number_of_points = 0;
		int max_random_number = (int) (100/pixelPercentage); // Evaluate max. random number
		if (scanningType.equals("Raster box")) max_random_number = 1; //take always all boxes 
		int random_number = 0;
		//int radius;		
		double count = 0.0;
		int sample = 0;
		int delta = 0;
		int boxSize = 0;	
			
		if  (max_random_number == 1) { // no statistical approach, take all image pixels		
			for (int n = 0; n < numBoxes; n++) { //2^0  to 2^numBoxes		
				boxSize = eps[n];
				if (scanningType.equals("Raster box"))  delta = boxSize;
				if (scanningType.equals("Sliding box")) delta = 1;
				for (int x =0; x <= (width-boxSize); x = x+delta){
					for (int y =0;  y <= (height-boxSize); y = y+delta){
						count = 0;
						raiBox = Views.interval(rai, new long[]{x, y}, new long[]{x+boxSize-1, y+boxSize-1});
						// Loop through all pixels of this box.
						cursor = Views.iterable(raiBox).localizingCursor();
						while (cursor.hasNext()) { //Box
							cursor.fwd();
							//cursor.localize(pos);
							sample = ((UnsignedByteType) cursor.get()).get();
							if ( sample > 0) {
								if      (colorModelType.equals("Binary")) count = count + 1; // Binary Image: 0 and [1, 255]! and not: 0 and 255
								else if (colorModelType.equals("Grey"))   count = count + sample;
							}			
						}//while Box
						//count = count / numObjectPixels; // normalized mass of current box
						totalsGenMax[n] = totalsGenMax[n] + count; // calculate total count for normalization
						// System.out.println("IqmOpFracGendim: b: "+b+ "   count: "+ count );
						if (count > 0) {
							for (int q = 0; q < numQ; q++) {
								if ((q + minQ) == 1) totalsGen[q][n] = totalsGen[q][n] + count * Math.log(count); // D1
								else                    totalsGen[q][n] = totalsGen[q][n] + Math.pow(count, (q + minQ)); // GenDim
							}
						}
					} //y	
				} //x                                          
			} //n
			
		} // no statistical approach
		else { //statistical approach
			for (int n = 0; n < numBoxes; n++) { //2^0  to 2^numBoxes		
				//radius = eps[n];	
				boxSize = eps[n];
				if (scanningType.equals("Raster box"))  delta = boxSize;
				if (scanningType.equals("Sliding box")) delta = 1;
				for (int x = 0; x <= (width-boxSize); x = x+delta){
					for (int y = 0;  y <= (height-boxSize); y = y+delta){		
						random_number = (int) (Math.random()*max_random_number+1);
						if( random_number == 1 ){ // UPDATE 07.08.2013 
							count = 0;
							raiBox = Views.interval(rai, new long[]{x, y}, new long[]{x+boxSize-1, y+boxSize-1});
							// Loop through all pixels of this box.
							cursor = Views.iterable(raiBox).localizingCursor();
							while (cursor.hasNext()) { //Box
								cursor.fwd();
								//cursor.localize(pos);
								sample = ((UnsignedByteType) cursor.get()).get();
								if ( sample > 0) {
									if      (colorModelType.equals("Binary")) count = count + 1; // Binary Image: 0 and [1, 255]! and not: 0 and 255
									else if (colorModelType.equals("Grey"))   count = count + sample;
								}			
							}//while Box
							//count = count / numObjectPixels; // normalized mass of current box
							totalsGenMax[n] = totalsGenMax[n] + count; // calculate total count for normalization
							// System.out.println("IqmOpFracGendim: b: "+b+ "   count: "+ count );
							if (count > 0) {
								for (int q = 0; q < numQ; q++) {
									if ((q + minQ) == 1) totalsGen[q][n] = totalsGen[q][n] + count * Math.log(count); // D1
									else                    totalsGen[q][n] = totalsGen[q][n] + Math.pow(count, (q + minQ)); // GenDim
								}
							}
						}
					} //y	
				} //x  
			} //n Box sizes		
		}
	
		// normalization
		for (int n = 0; n < numBoxes; n++) {

			for (int q = 0; q < numQ; q++) {
				totalsGen[q][n] = totalsGen[q][n] / totalsGenMax[n];
			}
		}
		
		//********************************Dmass
		//********************************Binary Image: 0 and [1, 255]! and not: 0 and 255
		if (fractalDimType.equals("Box counting")) {//"Box counting", "Pyramid"
			//Box counting
			//n=0  2^0 = 1 ... single pixel
			// Loop through all pixels.
	
			cursor = Views.iterable(rai).localizingCursor();	
			while (cursor.hasNext()) {
				cursor.fwd();
				//cursor.localize(pos);			
				if (((UnsignedByteType) cursor.get()).get() > 0) {
					totalsMass[0] += 1; // Binary Image: 0 and [1, 255]! and not: 0 and 255
					//totalsMassMax = totalsMassMax + 1; // / totalsMax;
				}
				//totals[n] = totals[n]; // / totalsMax;
			}
			
			boxSize = 0;		
			delta = 0;

			for (int n = 1; n < numBoxes; n++) { //2^1  to 2^numBoxes		
				boxSize = eps[n];	
				if      (scanningType.equals("Raster box"))  delta = boxSize;
				else if (scanningType.equals("Sliding box")) delta = 1;
				for (int x =0; x <= (width-boxSize); x=x+delta){
					for (int y =0;  y<= (height-boxSize); y=y+delta){
						raiBox = Views.interval(rai, new long[]{x, y}, new long[]{x+boxSize-1, y+boxSize-1});
						boolean isGreaterZeroFound = false;
						// Loop through all pixels of this box.
						cursor = Views.iterable(raiBox).localizingCursor();
						while (cursor.hasNext()) { //Box
							cursor.fwd();
							//cursorF.localize(pos);				
							if (((UnsignedByteType) cursor.get()).get() > 0) {
								totalsMass[n] += 1; // Binary Image: 0 and [1, 255]! and not: 0 and 255
								//totalsMassMax = totalsMassMax + 1; // / totalsMax;
								isGreaterZeroFound = true;
							}			
							if (isGreaterZeroFound) break; //do not search in this box any more
						}//while Box
					} //y	
				} //x                                          
			} //n
			
			
			//*********create boundary image
			
			//Compute erode image
			RectangleShape kernel = new RectangleShape(1, false); //3x3kernel skipCenter = false
			Runtime runtime  = Runtime.getRuntime();
			long maxMemory   = runtime.maxMemory();
			long totalMemory = runtime.totalMemory();
			long freeMemory  = runtime.freeMemory();
			int availableProcessors = runtime.availableProcessors();
			//System.out.println("availabel processors: " + availableProcessors);
			
			int numThreads = 6; //For erosion //with 6 it was 3 times faster than only with one thread
			if (numThreads > availableProcessors) numThreads = availableProcessors;

			long[] pos = new long[2];
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
			//imgTemp is now the boundary image
			//uiService.show("Boundary image", imgTemp);
			//******************************************************Dboundary
			//do again Box counting with boundary image
			//n=0  2^0 = 1 ... single pixel
			// Loop through all pixels.

			cursorF = imgTemp.localizingCursor();	
			while (cursorF.hasNext()) {
				cursorF.fwd();
				//cursor.localize(pos);			
				if (((FloatType) cursorF.get()).get() > 0) {
					totalsBoundary[0] += 1; // Binary Image: 0 and [1, 255]! and not: 0 and 255
					//totalsBoundaryMax[b] = totalsBoundaryMax[b] + 1;
				}
			}
		
			for (int n = 1; n < numBoxes; n++) { //2^1  to 2^numBoxes		
				boxSize = eps[n];		
				if      (scanningType.equals("Raster box"))  delta = boxSize;
				else if (scanningType.equals("Sliding box")) delta = 1;
				for (int x =0; x <= (width-boxSize); x=x+delta){
					for (int y =0;  y<= (height-boxSize); y=y+delta){
						raiBox = Views.interval(imgTemp, new long[]{x, y}, new long[]{x+boxSize-1, y+boxSize-1});
						boolean isGreaterZeroFound = false;
						// Loop through all pixels of this box.
						cursorF = (Cursor<FloatType>) Views.iterable(raiBox).localizingCursor();
						while (cursorF.hasNext()) { //Box
							cursorF.fwd();
							//cursorF.localize(pos);				
							if (((FloatType) cursorF.get()).get() > 0) {
								totalsBoundary[n] += 1; // Binary Image: 0 and [1, 255]! and not: 0 and 255
								//totalsBoundaryMax[b] = totalsBoundaryMax[b] + 1; // / totalsMax[b];
								isGreaterZeroFound = true;
							}			
							if (isGreaterZeroFound) break; //do not search in this box any more
						}//while Box
					} //y	
				} //x                                          
			} //n
				
		} //Box Counting dimension
		
		else if (fractalDimType.equals("Pyramid")) {//"Box counting", "Pyramid"
			//only sub-sampling by averaging of original image works.
			//interpolation and then sub-sampling does not give the same results as Box counting
			int numPyramidImages = numBoxes;
			int downSamplingFactor;
			
			//Compute Mass
			for (int n = 0; n < numPyramidImages; n++) { //Downscaling incl. no downscaling
				 // "base-pyramid", i.e. layers of pyramid from base layer
				downSamplingFactor =  eps[n]; //of downsampling
				imgFloat = createImgFloat(rai);
			    imgSubSampled = subSamplingByAveraging(imgFloat, downSamplingFactor);
			   // uiService.show("Subsampled image", imgSubSampled);
				//logService.info(this.getClass().getName() + " width:"+ (imgDownscaled.dimension(0)) + " height:" + (imgDownscaled.dimension(1)));

				//****IMPORTANT****Displaying a rai slice (pseudo 2D) directly with e.g. uiService.show(name, rai);
				//pushes a 3D array to the display and
				//yields mouse moving errors because the third dimension is not available		
				
				//if ((optShowDownscaledImages) && (n > 0)) uiService.show("1/"+power+" downscaled image", imgDownscaled);
				// Loop through all pixels.
				cursorF = imgSubSampled.localizingCursor();
				while (cursorF.hasNext()) {
					cursorF.fwd();
					//cursorF.localize(pos);
					
					if (cursorF.get().get() > 0) { // Binary Image //[1, 255] and not [255, 255] because interpolation introduces grey values other than 255!
						totalsMass[n] += 1; 
						//totalsMassMax[b] = totalsMassMax[b]  + 1; // / totalsMax[b];
					}
						
					
				}	
			}//n
			//*********create boundary image	
			//Compute erode image
			RectangleShape kernel = new RectangleShape(1, false); //3x3kernel skipCenter = false
			Runtime runtime  = Runtime.getRuntime();
			long maxMemory   = runtime.maxMemory();
			long totalMemory = runtime.totalMemory();
			long freeMemory  = runtime.freeMemory();
			int availableProcessors = runtime.availableProcessors();
			//System.out.println("availabel processors: " + availableProcessors);
			
			int numThreads = 6; //For erosion //with 6 it was 3 times faster than only with one thread
			if (numThreads > availableProcessors) numThreads = availableProcessors;

			long[] pos = new long[2];
			int pixelValueImgOrig;
			float pixelValueImgTemp;
			imgFloat = this.createImgFloat(rai);
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
			//Compute Boundary
			for (int n = 0; n < numPyramidImages; n++) { //Downscaling incl. no downscaling
				 // "base-pyramid", i.e. layers of pyramid from base layer
				downSamplingFactor =  eps[n]; //of downsampling
			    imgSubSampled = subSamplingByAveraging(imgTemp, downSamplingFactor);
				//logService.info(this.getClass().getName() + " width:"+ (imgDownscaled.dimension(0)) + " height:" + (imgDownscaled.dimension(1)));

				//****IMPORTANT****Displaying a rai slice (pseudo 2D) directly with e.g. uiService.show(name, rai);
				//pushes a 3D array to the display and
				//yields mouse moving errors because the third dimension is not available		
				
				//if ((optShowDownscaledImages) && (n > 0)) uiService.show("1/"+power+" downscaled image", imgDownscaled);
				// Loop through all pixels.
				cursorF = imgSubSampled.localizingCursor();
				while (cursorF.hasNext()) {
					cursorF.fwd();
					//cursorF.localize(pos);
				
					if (cursorF.get().get() > 0) { // Binary Image //[1, 255] and not [255, 255] because interpolation introduces grey values other than 255!
						totalsBoundary[n] += 1; 
						//totalsBoundaryMax[b] = totalsBoundaryMax[b]  + 1; // / totalsMax[b];
					}
					
				}	
			}//n
		}
		
		
		//Normalization 
//		for (int n = 0; n < numBoxes; n++) {
//			for (int b = 0; b < numBands; b++) {
//				totalsMass[n][b] = totalsMass[n][b] / totalsMassMax[b];
//				totalsBoundary[n][b] = totalsBoundary[n][b] / totalsBoundaryMax[b];
//			}
//		}	
		//Computing log values for plot 
		//Change sequence of entries to start with a pixel
		double[][] lnTotalsGen  	= new double[numQ][numBoxes];
		double[]   lnTotalsMass     = new double[numBoxes];
		double[]   lnTotalsBoundary = new double[numBoxes];
		double[]   lnEps            = new double[numBoxes];
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
				lnTotalsMass[n] = Math.log(totalsMass[n]); //
			}
			//Dboundary
			if (totalsBoundary[n] == 0) {
				lnTotalsBoundary[n] = Math.log(Double.MIN_VALUE);
			} else if (Double.isNaN(totalsBoundary[n])) {
				lnTotalsBoundary[n] = Double.NaN;
			} else {
				lnTotalsBoundary[n] = Math.log(totalsBoundary[n]); //
			}
			
			lnEps[n] = Math.log(eps[n]);
			//logService.info(this.getClass().getName() + " n:" + n + " eps:  " + eps[n][b]);
			//logService.info(this.getClass().getName() + " n:" + n + " lnEps:  "+  lnEps[n][b] );
			//logService.info(this.getClass().getName() + " n:" + n + " totalsMass[n][b]: " + totalsMass[n][b]);
		}
		
		//Create double log plot
		boolean isLineVisible = false; //?
				
		// Plot //nur ein Band!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		double[]   lnDataX  = new double[numBoxes];
		double[][] lnDataY  = new double[3][numBoxes]; //for D1, Dmass Dboundary
	
			
		//only first band!!!!!!!!!
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
			String preName = "";
			if (numSlices > 1) {
				preName = "Slice-"+String.format("%03d", plane) +"-";
			}
			String xAxisLabel = "";
			if      (fractalDimType.equals("Box counting")) xAxisLabel = "ln(Box size)";
			else if (fractalDimType.equals("Pyramid"))      xAxisLabel = "ln(2^n)";
			String yAxisLabel = "ln(Count)";
			
			String[] legendLabels = new String[3];
			legendLabels[0] = "D1";
			legendLabels[1] = "D Mass";
			legendLabels[2] = "D Boundary";
			
			Plot_RegressionFrame doubleLogPlot = DisplayRegressionPlotXY(lnDataX, lnDataY, isLineVisible, "Double Log Plots - Fractal fragmentation indices", 
					preName + datasetName, xAxisLabel, yAxisLabel, legendLabels,
					numRegStart, numRegEnd);
			doubleLogPlotList.add(doubleLogPlot);
		}
				
		// Compute regressions
		Regression_Linear lr = new Regression_Linear();
		double[] regressionParamsD1 = lr.calculateParameters(lnDataX, lnDataY[0], numRegStart, numRegEnd);
		lr = new Regression_Linear();
		double[] regressionParamsMass = lr.calculateParameters(lnDataX, lnDataY[1], numRegStart, numRegEnd);
		lr = new Regression_Linear();
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
		
		//D1	 0 Intercept,  1 slope,  2 InterceptStdErr,  3 SlopeStdErr,  4 RSquared
		//Mass	 5 Intercept,  6 slope,  7 InterceptStdErr,  8 SlopeStdErr,  9 RSquared
		//Perim	10 Intercept, 11 slope, 12 InterceptStdErr, 13 SlopeStdErr, 14 RSquared
		
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
		//logService.info(this.getClass().getName() + " D mass: " + dimMass); //do this in processSingleInputImage() and ProcessAllInputImages()
		
		epsRegStartEnd[0] = eps[numRegStart-1];
		epsRegStartEnd[1] = eps[numRegEnd-1];
	
		return new Container_ProcessMethod(resultValues, epsRegStartEnd);
		//Output
		//uiService.show(tableOutName, table);
		////result = ops.create().img(image, new FloatType()); may not work in older Fiji versions
		//result = new ArrayImgFactory<>(new FloatType()).create(image.dimension(0), image.dimension(1)); 
		//table
	}

	
	

	//This methods reduces dimensionality to 2D just for the display 	
	//****IMPORTANT****Displaying a rai slice (pseudo 2D) directly with e.g. uiService.show(name, rai);
	//pushes a 3D array to the display and
	//yields mouse moving errors because the third dimension is not available
	private <T extends Type<T>, F> void displayImage(String name, IterableInterval<FloatType> iv) {

		// Create an image.
		long[] dims = {iv.max(0)+1, iv.max(0)+1};
		AxisType[] axes = {Axes.X, Axes.Y};
		int bitsPerPixel = 32;
		boolean signed = true;
		boolean floating = true;
		boolean virtual = false;
		//dataset = ij.dataset().create(dims, name, axes, bitsPerPixel, signed, floating);
		Dataset datasetDisplay = datasetService.create(dims, name, axes, bitsPerPixel, signed, floating, virtual);
		
		RandomAccess<RealType<?>> ra = datasetDisplay.randomAccess();
		
		Cursor<FloatType> cursor = iv.localizingCursor();
    	final long[] pos = new long[iv.numDimensions()];
		while (cursor.hasNext()) {
			cursor.fwd();
			cursor.localize(pos);
			ra.setPosition(pos);
			//ra.setPosition(pos[0], 0);
			//ra.setPosition(pos[1], 1);
			ra.get().setReal(cursor.get().get());
		}  	
		
		uiService.show(name, datasetDisplay);
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
	private Plot_RegressionFrame DisplayRegressionPlotXY(double[] dataX, double[][] dataY, boolean isLineVisible,
			String frameTitle, String plotLabel, String xAxisLabel, String yAxisLabel, String[] legendLabels, int numRegStart, int numRegEnd) {
		// jFreeChart
		Plot_RegressionFrame pl = new Plot_RegressionFrame(dataX, dataY, isLineVisible, frameTitle, plotLabel, xAxisLabel,
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
	 * This methods creates an Img<FloatType>
	 */
	private Img<FloatType > createImgFloat(RandomAccessibleInterval<?> rai){ //rai must always be a single 2D plane
		
		imgFloat = new ArrayImgFactory<>(new FloatType()).create(rai.dimension(0), rai.dimension(1)); //always single 2D
		Cursor<FloatType> cursor = imgFloat.localizingCursor();
		final long[] pos = new long[imgFloat.numDimensions()];
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
			cursor.get().setReal(ra.get().getRealFloat());
		}	
		return imgFloat;
	}
	
	/**
	 * 
	 * This methods creates an Img<BitType>
	 */
	private Img<BitType > createImgBit(RandomAccessibleInterval<?> rai){ //rai must always be a single 2D plane
		
		imgBit = new ArrayImgFactory<>(new BitType()).create(rai.dimension(0), rai.dimension(1)); //always single 2D
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
	 * This method creates a smaller image by averaging pixel values 
	 * @param imgFloat
	 * @param downSamplingFactor
	 * @return
	 */	
	private Img<FloatType> subSamplingByAveraging(Img<FloatType> imgFloat, int downSamplingFactor) {
		
		int averagingSize = downSamplingFactor;
		
		int numDimensions = imgFloat.numDimensions();
		// compute the number of pixels of the output and the size of the real interval
		long[] newSize = new long[numDimensions];
	
		for ( int d = 0; d < numDimensions; ++d ){
			newSize[d] = (int)Math.floor(imgFloat.dimension(d) / downSamplingFactor);
		}		
		// create the output image
		ArrayImgFactory arrayImgFactory = new ArrayImgFactory<>(new FloatType());
		imgSubSampled = arrayImgFactory.create( newSize );

		// cursor to iterate over all pixels
		cursorF = imgSubSampled.localizingCursor();

		// create a RandomAccess on the source
		RandomAccess<FloatType> ra = imgFloat.randomAccess();
		long[] pos = new long[numDimensions];
		float mean = 0f;
		long count = 0;
		// for all pixels of the output image
		while (cursorF.hasNext()) {
			cursorF.fwd();
			cursorF.localize(pos);
			//Get average
			mean = 0f;
			
			for (int i = 0; i < averagingSize; i++) {
				for (int j = 0; j < averagingSize; j++) {
					ra.setPosition(pos[0]*averagingSize + i, 0);
					ra.setPosition(pos[1]*averagingSize + j, 1);
					mean = mean + ra.get().getRealFloat();
					count = count +1;
				}
			}
			mean = mean/(float)count;
			cursorF.get().set(mean);
		}
		return imgSubSampled;
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
		//ij.command().run(MethodHandles.lookup().lookupClass().getName(), true).get().getOutput("image");
		ij.command().run(MethodHandles.lookup().lookupClass().getName(), true);
	}
}

