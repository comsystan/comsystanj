/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj3DFracDimFFT.java
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
import java.util.Arrays;
import java.util.Comparator;
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
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.scijava.ItemIO;
import org.scijava.ItemVisibility;
import org.scijava.app.StatusService;
import org.scijava.command.InteractiveCommand;
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

import at.csa.csaj.commons.Dialog_WaitingWithProgressBar;
import at.csa.csaj.commons.Plot_RegressionFrame;
import at.csa.csaj.commons.Regression_Linear;
import at.csa.csaj.commons.Container_ProcessMethod;
import edu.emory.mathcs.jtransforms.fft.FloatFFT_3D;
import io.scif.DefaultImageMetadata;
import io.scif.MetaTable;

/**
 * A {@link InteractiveCommand} plugin computing <the 3D FFT dimension</a>
 * of an image volume.
 */
@Plugin(type = InteractiveCommand.class,
headless = true,
label = "3D FFT dimension",
initializer = "initialPluginLaunch",
iconPath = "/icons/comsystan-logo-grey46-16x16.png", //Menu entry icon
menu = {
@Menu(label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT, mnemonic = MenuConstants.PLUGINS_MNEMONIC),
@Menu(label = "ComsystanJ"),
@Menu(label = "3D Volume"),
@Menu(label = "3D Fractal analyses", weight = 6),
@Menu(label = "3D FFT dimension")})
/**
 * Csaj Interactive: InteractiveCommand (nonmodal GUI without OK and cancel button, NOT for Scripting!)
 * Csaj Macros:      ContextCommand     (modal GUI with OK and Cancel buttons, for scripting)
 * Developer note:
 * Develop the InteractiveCommand plugin Csaj***.java
 * Hard copy it and rename to            Csaj***Command.java
 * Eliminate complete menu entry
 * Change 4x (incl. import) to ContextCommand instead of InteractiveCommand
 */
public class Csaj3DFracDimFFT<T extends RealType<T>> extends InteractiveCommand implements Previewable {

	private static final String PLUGIN_LABEL            = "Computes fractal dimension with 3D FFT";
	private static final String SPACE_LABEL             = "";
	private static final String REGRESSION_LABEL        = "<html><b>Regression parameters</b></html>";
	private static final String METHODOPTIONS_LABEL     = "<html><b>Method options</b></html>";
	//private static final String BACKGROUNDOPTIONS_LABEL = "<html><b>Background option</b></html>";
	private static final String DISPLAYOPTIONS_LABEL    = "<html><b>Display options</b></html>";
	private static final String PROCESSOPTIONS_LABEL    = "<html><b>Process options</b></html>";
	
	private static float[][][] volA;
	private static RandomAccessibleInterval<FloatType>  raiWindowed; 
	private static RandomAccessibleInterval<?> rai;
	private static RandomAccess<?> ra;
	//private static Cursor<?> cursor = null;
	private static String datasetName;
	private static String[] sliceLabels;
	private static long width  = 0;
	private static long height = 0;
	private static long depth  = 0;
	private static long numDimensions = 0;
	private static long numSlices     = 0;
	private static int numVolumes     = 0;
	private static long compositeChannelCount = 0;
	private static String imageType = "";
	private static int  numOfK = 0;
	
	private static double[] sequence;
	private static ArrayList<Plot_RegressionFrame> doubleLogPlotList = new ArrayList<Plot_RegressionFrame>();
	
	private static final String tableOutName = "Table - 3D FFT dimension";
	
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

	@Parameter(label = "Maximal k",
  		   	   description = "Maximal frequency k",
	       	   style = NumberWidget.SPINNER_STYLE,
	           min = "1",
	           max =  "10000000000",
	           stepSize = "1",
	           persist  = false,  //restore previous value default = true
	           initializer = "initialMaxK",
	           callback    = "callbackMaxK")
	private int spinnerInteger_MaxK;
  
	@Parameter(label = "Regression Start",
  		   	   description = "Minimum x value of linear regression",
		       style = NumberWidget.SPINNER_STYLE,
		       min = "1",
		       max =  "10000000000",
		       stepSize = "1",
		       //persist  = false,   //restore previous value default = true
		       initializer = "initialNumRegStart",
		       callback = "callbackNumRegStart")
	private int spinnerInteger_NumRegStart = 1;

	@Parameter(label = "Regression End",
			   description = "Maximum x value of linear regression",
			   style = NumberWidget.SPINNER_STYLE,
		       min = "3",
		       max = "10000000000",
		       stepSize = "1",
		       persist  = false,   //restore previous value default = true
		       initializer = "initialNumRegEnd",
		       callback = "callbackNumRegEnd")
	private int spinnerInteger_NumRegEnd = 3;

	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
    private final String labelMethodOptions = METHODOPTIONS_LABEL;
     
 	@Parameter(label = "Windowing",
			description = "Windowing type with increasing filter strength",
			style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
			choices = {"Rectangular", "Bartlett", "Hamming", "Hanning", "Blackman", "Gaussian", "Parzen"}, //In the order of increasing filter strength
			persist  = false,  //restore previous value default = true
			initializer = "initialWindowingType",
			callback = "callbackWindowingType")
	private String choiceRadioButt_WindowingType;
     
    @Parameter(label = "Power spectrum",
    		    description = "Type of power spectrum computation",
    		    style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
      		    choices = {"Spherical average", "Mean of line scans", "Integral of line scans"},
      		    //persist  = false,  //restore previous value default = true
    		    initializer = "initialPowerSpecType",
                callback = "callbackPowerSpecType")
    private String choiceRadioButt_PowerSpecType;
	     
//  @Parameter(label = "Add mirrored images",
//	    		description = "Add horizontally,vertically and diagonally mirrored images. Supresses edge errors",
//	 		    //persist  = false,  //restore previous value default = true
//			    initializer = "initialAddMirroredImages",
//			    callback = "callbackAddMirroredImages")
//	private boolean booleanAddMirroredImages;

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
	
	protected void initialMaxK() {
      	numOfK = getMaxK((int)width, (int)height, (int)depth);
      	spinnerInteger_MaxK = numOfK;
    }
    protected void initialNumRegStart() {
    	spinnerInteger_NumRegStart = 1;
    }
    protected void initialNumRegEnd() {
    	numOfK = getMaxK((int)width, (int)height, (int)depth);
    	spinnerInteger_NumRegEnd =  numOfK;
    }
    protected void initialWindowingType() {
		choiceRadioButt_WindowingType = "Hanning";
	} 
    protected void initialPowerSpecType() {
    	choiceRadioButt_PowerSpecType = "Spherical average";
    	// to set maximal k and NumRegEnd
		int numOfK = getMaxK((int)width, (int)height, (int)depth);
		spinnerInteger_MaxK   = numOfK;
		spinnerInteger_NumRegEnd = numOfK;	
    } 
//	protected void initialAddMirroredImages() {
//		booleanAddMirroredImages = true;
//	}

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
	/** Executed whenever the {@link #spinInteger_MaxK} parameter changes. */
	protected void callbackMaxK() {
		
		if  (spinnerInteger_MaxK < 3) {
			spinnerInteger_MaxK = 3;
		}
		int numMaxK = getMaxK((int)width, (int)height, (int)depth);	
		if (spinnerInteger_MaxK > numMaxK) {
			spinnerInteger_MaxK = numMaxK;
		};
		if (spinnerInteger_NumRegEnd > spinnerInteger_MaxK) {
			spinnerInteger_NumRegEnd = spinnerInteger_MaxK;
		}
		if (spinnerInteger_NumRegStart >= spinnerInteger_NumRegEnd - 2) {
			spinnerInteger_NumRegStart = spinnerInteger_NumRegEnd - 2;
		}

		numOfK = spinnerInteger_MaxK;
		logService.info(this.getClass().getName() + " Maximal k set to " + spinnerInteger_MaxK);
	}
    /** Executed whenever the {@link #spinInteger_NumRegStart} parameter changes. */
	protected void callbackNumRegStart() {
		if (spinnerInteger_NumRegStart >= spinnerInteger_NumRegEnd - 2) {
			spinnerInteger_NumRegStart = spinnerInteger_NumRegEnd - 2;
		}
		if(spinnerInteger_NumRegStart < 1) {
			spinnerInteger_NumRegStart = 1;
		}
		logService.info(this.getClass().getName() + " Regression Min set to " + spinnerInteger_NumRegStart);
	}
	/** Executed whenever the {@link #spinInteger_NumRegEnd} parameter changes. */
	protected void callbackNumRegEnd() {
		if (spinnerInteger_NumRegEnd <= spinnerInteger_NumRegStart + 2) {
			spinnerInteger_NumRegEnd = spinnerInteger_NumRegStart + 2;
		}		
		if (spinnerInteger_NumRegEnd > spinnerInteger_MaxK) {
			spinnerInteger_NumRegEnd = spinnerInteger_MaxK;
		}
		
		logService.info(this.getClass().getName() + " Regression Max set to " + spinnerInteger_NumRegEnd);
	}

	/** Executed whenever the {@link #choiceRadioButt_WindowingType} parameter changes. */
	protected void callbackWindowingType() {
		logService.info(this.getClass().getName() + " Windowing type set to " + choiceRadioButt_WindowingType);
	}
	
	/** Executed whenever the {@link #choiceRadioButt_PowerSpecType} parameter changes. */
	protected void callbackPowerSpecType() {
		int numOfK = getMaxK((int)width, (int)height, (int)depth);
		spinnerInteger_MaxK   = numOfK;
		spinnerInteger_NumRegEnd = numOfK;	
		logService.info(this.getClass().getName() + " Power spectrum type set to " + choiceRadioButt_PowerSpecType);
		
	}
	
//	/** Executed whenever the {@link #booleanAddMirroredImages} parameter changes. */
//	protected void callbackAddMirroredImages() {
//		getAndValidateActiveDataset();
//		int numOfK = getMaxK((int)width, (int)height);
//		spinnerInteger_MaxK   = numOfK;
//		spinnerInteger_NumRegEnd = numOfK;	
//		logService.info(this.getClass().getName() + " Add mirrored images set to " + booleanAddMirroredImages);
//		
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
			startWorkflowForSingleVolume();
		}
	}

	public void checkItemIOIn() {

		//datasetIn = imageDisplayService.getActiveDataset();
		if (datasetIn == null) {
			logService.error(this.getClass().getName() + " ERROR: Input image volume = null");
			cancel("ComsystanJ 3D plugin cannot be started - missing input image volume.");
			return;
		}

		if ( (datasetIn.firstElement() instanceof UnsignedByteType) ||
			 (datasetIn.firstElement() instanceof FloatType) ){
			//That is OK, proceed
		} else {
			logService.warn(this.getClass().getName() + " WARNING: Data type is not Byte or Float");
			cancel("ComsystanJ 3D plugin cannot be started - data type is not Byte or Float.");
			return;
		}
		
		// get some info
		width  = datasetIn.dimension(0);
		height = datasetIn.dimension(1);
		depth  = datasetIn.dimension(2);
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
			logService.warn(this.getClass().getName() + " WARNING: Grey value image volume expected!");
			cancel("ComsystanJ 3D plugin cannot be started - grey value image volume expected!");
		}
		//Image volume expected
		if (numSlices == 1) { 
			logService.warn(this.getClass().getName() + " WARNING: Single image instead of image volume detected");
			cancel("ComsystanJ 3D plugin cannot be started - image volume expected!");
		}
	}

	/**
	* This method starts the workflow for a single image of the active display
	*/
	protected void startWorkflowForSingleVolume() {
	
		dlgProgress = new Dialog_WaitingWithProgressBar("Computing 3D FFT dimension, please wait... Open console window for further info.",
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
				if (display.getName().contains(tableOutName)) display.close();
			}			
		}
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
		Container_ProcessMethod containerPM = process(rai); //rai is 3D

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
		
		GenericColumn columnFileName       = new GenericColumn("File name");
		IntColumn columnMaxK               = new IntColumn("Max k");
		GenericColumn columnNumRegStart    = new GenericColumn("Reg Start");
		GenericColumn columnNumRegEnd      = new GenericColumn("Reg End");
		GenericColumn columnWindowingType  = new GenericColumn("Windowing type");
		GenericColumn columnPowerSpecType  = new GenericColumn("PowerSpec type");
		//GenericColumn columnAddMirrors   = new GenericColumn("Add mirrors");
		DoubleColumn columnDf              = new DoubleColumn("3D Df");
		DoubleColumn columnR2              = new DoubleColumn("R2");
		DoubleColumn columnStdErr          = new DoubleColumn("StdErr");

		tableOut = new DefaultGenericTable();
		tableOut.add(columnFileName);
		tableOut.add(columnMaxK);
		tableOut.add(columnNumRegStart);
		tableOut.add(columnNumRegEnd);
		tableOut.add(columnWindowingType);
		tableOut.add(columnPowerSpecType);
		//tableOut.add(columnAddMirrors);
		tableOut.add(columnDf);
		tableOut.add(columnR2);
		tableOut.add(columnStdErr);
	}

	/**
	 * collects current result and writes to table
	 * 
	 * @param Container_ProcessMethod containerPM
	 */
	private void writeToTable(Container_ProcessMethod containerPM) { 

		int numRegStart       = spinnerInteger_NumRegStart;
		int numRegEnd         = spinnerInteger_NumRegEnd;
		int numMaxK           = spinnerInteger_MaxK;
		String windowingType  = choiceRadioButt_WindowingType;
		String powerSpecType  = choiceRadioButt_PowerSpecType;	
		//boolean addMirrors    = booleanAddMirroredImages;

		// 0 Intercept, 1 Dim, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
		// fill table with values
		tableOut.appendRow();
		tableOut.set("File name",       tableOut.getRowCount() - 1, datasetName);	
		tableOut.set("Max k",           tableOut.getRowCount() - 1, numMaxK);
		tableOut.set("Reg Start",       tableOut.getRowCount()-1, "("+numRegStart+")" + containerPM.item2_Values[0]); //(NumRegStart)epsRegStart
		tableOut.set("Reg End",      	tableOut.getRowCount()-1, "("+numRegEnd+")"   + containerPM.item2_Values[1]); //(NumRegEnd)epsRegEnd
		tableOut.set("Windowing type",  tableOut.getRowCount()-1, windowingType);	
		tableOut.set("PowerSpec type",  tableOut.getRowCount()-1, powerSpecType);	
		//tableOut.set("Add mirrors",  	tableOut.getRowCount()-1, addMirrors);	
		tableOut.set("3D Df",          	tableOut.getRowCount()-1, containerPM.item1_Values[1]);
		tableOut.set("R2",          	tableOut.getRowCount()-1, containerPM.item1_Values[4]);
		tableOut.set("StdErr",      	tableOut.getRowCount()-1, containerPM.item1_Values[3]);	
	}

	/**
	*
	* Processing
	*/
	private Container_ProcessMethod process(RandomAccessibleInterval<?> rai) { //3Dvolume
	
		dlgProgress.setBarIndeterminate(false);
		int percent;
		
		if (rai == null) {
			logService.info(this.getClass().getName() + " WARNING: rai==null, no image for processing!");
		}
		
		int numRegStart       = spinnerInteger_NumRegStart;
		int numRegEnd         = spinnerInteger_NumRegEnd;
		int numMaxK           = spinnerInteger_MaxK;
		String windowingType  = choiceRadioButt_WindowingType;
		String powerSpecType  = choiceRadioButt_PowerSpecType;	
		//boolean addMirrors    = booleanAddMirroredImages;
		
		boolean optShowPlot  = booleanShowDoubleLogPlot;

		int width  = (int)rai.dimension(0);
		int height = (int)rai.dimension(1);
		int depth  = (int)rai.dimension(2);
		
		double[] epsRegStartEnd   = new double[2];  // epsRegStart, epsRegEnd
		double[] regressionParams = null;
		double[] resultValues = null;
		
		//Convert image to float
		//Img<T> image = (Img<T>) dataset.getImgPlus();
		//RandomAccessibleInterval<T> rai = (RandomAccessibleInterval<T>)dataset.getImgPlus();
		//IterableInterval ii = dataset.getImgPlus();
		//Img<FloatType> imgFloat = opService.convert().float32(ii);
		
		double[] totals = null; //= new double[numBoxes];
		// double[] totalsMax = new double[numBands]; //for binary images
		double[] eps = null; // = new int[numBoxes];
		
//		if (addMirrors) {		
//			createImgMirrored(rai);
//			rai =  (RandomAccessibleInterval<?>) imgMirrored;
//			width  = rai.dimension(0);
//			height = rai.dimension(1);
//			//uiService.show("Mirrored rai", rai);		
//		
//		} else {
//			//take rai as it comes
//			//uiService.show("Image rai", rai);	
//		}
			
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
		
		if (imageType.equals("Grey")) {// grey image   //additional check, is already checked during validation of active dataset
		
			if (powerSpecType.equals("Spherical average")) { //{"Spherical average", "Mean of line scans", "Integral of line scans"},
				
//				ops filter fft seems to be a Hadamard transform rather than a true FFT
//				output size is automatically padded, so has rather strange dimensions.
//				output is vertically symmetric 
//				F= 0 is at (0.0) and (0,SizeY)
//				imgFloat = this.createImgFloat(raiWindowed);
//				RandomAccessibleInterval<C> raifft = opService.filter().fft(imgFloat);
//				
//				//This would also work with identical output 
//				ImgFactory<ComplexFloatType> factory = new ArrayImgFactory<ComplexFloatType>(new ComplexFloatType());
//				int numThreads = 6;
//				final FFT FFT = new FFT();
//				Img<ComplexFloatType> imgCmplx = FFT.realToComplex((RandomAccessibleInterval<R>) raiWindowed, factory, numThreads);

				//Using JTransform package
				//https://github.com/wendykierp/JTransforms
				//https://wendykierp.github.io/JTransforms/apidocs/
				//The sizes of both dimensions must be power of two.
				// Round to next largest power of two. The resulting image will be cropped according to GUI input
				int widthDFT  = width  == 1 ? 1 : Integer.highestOneBit(width  - 1) * 2;
				int heightDFT = height == 1 ? 1 : Integer.highestOneBit(height - 1) * 2;
				int depthDFT  = depth  == 1 ? 1 : Integer.highestOneBit(depth  - 1) * 2;
				
				//All DFT axes must have the same size, otherwise lowest frequencies are not the same for anisotropic sizes
				widthDFT  = (int)Math.max(Math.max(widthDFT, heightDFT), depthDFT); 
				heightDFT = widthDFT;
				depthDFT  = widthDFT;
					
				//JTransform needs rows and columns swapped!!!!!
				int slices  = depthDFT;
				int rows    = heightDFT;
				int columns = widthDFT;
				
				//JTransform needs rows and columns swapped!!!!!
				volA = new float[slices][rows][2*columns]; //Every frequency entry needs a pair of columns: for real and imaginary part
				Cursor<?> cursor = Views.iterable(raiWindowed).localizingCursor();
				long[] pos = new long[3];
				while (cursor.hasNext()) {
					cursor.fwd();
					cursor.localize(pos); 
					//JTransform needs rows and columns swapped!!!!!
					volA[(int)pos[2]][(int)pos[1]][(int)pos[0]] = ((FloatType) cursor.get()).get();
				}
				
				// JTransform needs rows and columns swapped!!!!!
				FloatFFT_3D FFT = new FloatFFT_3D(slices, rows, columns); //Here always the simple DFT width
				//FFT.realForward(volA);   
				FFT.realForwardFull(volA);   
					
				//Define 8 origins for FFT
		    	//Define origin as 0,0,0. //frequency = 0;
		    	final long[] origin1 = {        0,      0,        0};
		    	final long[] origin2 = {        0, rows-1,        0}; //bottom right of image   	
		    	final long[] origin3 = {        0,      0, slices-1}; //top left of image    	
		    	final long[] origin4 = {        0, rows-1, slices-1}; //top right of image    	
		    	
		    	//Following origins are symmetric because sum of powers is zero with .realForward(volA); 
		    	//final long[] origin5 = {columns-1,      0,        0}; 
		    	//final long[] origin6 = {columns-1, rows-1,        0}; //bottom right of image 
		    	//final long[] origin7 = {columns-1,      0, slices-1}; //top left of image 	 	
		    	//final long[] origin8 = {columns-1, rows-1, slices-1}; //top right of image
						
				// Define arrays
				long[] posFFT = new long[3];
			    long numOfK = slices * rows * columns; 
												
				float[] powers            = new float[(int)numOfK];
				float[] allKs             = new float[(int)numOfK];
				float[] powersSorted      = new float[(int)numOfK];
				float[] allKsSorted       = new float[(int)numOfK];
				float[] powersCircAverage = new float[(int)numOfK]; //will have some zeroes at the end
				float[] allKsCircAverage  = new float[(int)numOfK]; //will have some zeroes at the end
				
				// Define half height and depth. Later used to find right origin
		    	final long fftHalfSlices  = slices/2;
		    	final long fftHalfRows    = rows/2;
		    	final long fftHalfColumns = columns/2;

				int p = 0;
				float dist = 0;
				
//				//for debug control purposes only
//				float sumDist1 = 0;
//				float sumDist2 = 0;
//				float sumDist3 = 0;
//				float sumDist4 = 0;
//				float sumDist5 = 0;
//				float sumDist6 = 0;
//				float sumDist7 = 0;
//				float sumDist8 = 0;
//				float sumPowers1 = 0;
//				float sumPowers2 = 0;
//				float sumPowers3 = 0;
//				float sumPowers4 = 0;
//				float sumPowers5 = 0;
//				float sumPowers6 = 0;
//				float sumPowers7 = 0;
//				float sumPowers8 = 0;
				
				percent = 1;
				dlgProgress.updatePercent(String.valueOf(percent+"%"));
				dlgProgress.updateBar(percent);
				//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
				statusService.showStatus(percent, 100, "Initializing finished");
								
				// Loop through all pixels and write distances and powers to arrays
				for (int k1 = 0; k1 < slices; k1++) {
					
					percent = (int)Math.max(Math.round((  ((float)k1)/((float)slices)   *100.f   )), percent);
					dlgProgress.updatePercent(String.valueOf(percent+"%"));
					dlgProgress.updateBar(percent);
					//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
					statusService.showStatus((k1+1), slices, "Processing " + (k1+1) + "/" + slices);
					
					for (int k2 = 0; k2 < rows; k2++) {
						for (int k3 = 0; k3 < columns/2; k3++) { //with 4 origins
						//for (int k3 = 0; k3 < columns; k3++) { //with 8 origins
								posFFT[2] = k1;
								posFFT[1] = k2;
								posFFT[0] = k3;
					
								// change origin depending on cursor position
								if      (posFFT[0] <  fftHalfColumns && posFFT[1] <  fftHalfRows && posFFT[2] <  fftHalfSlices) dist = (float)Util.distance(origin1, posFFT);
								else if (posFFT[0] <  fftHalfColumns && posFFT[1] >= fftHalfRows && posFFT[2] <  fftHalfSlices) dist = (float)Util.distance(origin2, posFFT);
								else if (posFFT[0] <  fftHalfColumns && posFFT[1] <  fftHalfRows && posFFT[2] >= fftHalfSlices) dist = (float)Util.distance(origin3, posFFT);
								else if (posFFT[0] <  fftHalfColumns && posFFT[1] >= fftHalfRows && posFFT[2] >= fftHalfSlices) dist = (float)Util.distance(origin4, posFFT);
//								else if (posFFT[0] >= fftHalfColumns && posFFT[1] <  fftHalfRows && posFFT[2] <  fftHalfSlices) dist = (float)Util.distance(origin5, posFFT);
//								else if (posFFT[0] >= fftHalfColumns && posFFT[1] >= fftHalfRows && posFFT[2] <  fftHalfSlices) dist = (float)Util.distance(origin6, posFFT);
//								else if (posFFT[0] >= fftHalfColumns && posFFT[1] <  fftHalfRows && posFFT[2] >= fftHalfSlices) dist = (float)Util.distance(origin7, posFFT);
//								else if (posFFT[0] >= fftHalfColumns && posFFT[1] >= fftHalfRows && posFFT[2] >= fftHalfSlices) dist = (float)Util.distance(origin8, posFFT);
								
								allKs[p]  = dist; //Distance
								powers[p] = volA[k1][k2][2*k3]*volA[k1][k2][2*k3] + volA[k1][k2][2*k3+1]*volA[k1][k2][2*k3+1];
								p += 1;	
															
//								//for debug control purposes only
//								if      (posFFT[0] <  fftHalfColumns && posFFT[1] <  fftHalfRows && posFFT[2] <  fftHalfSlices) { sumDist1 += dist; sumPowers1 += powers[p-1]; }
//								else if (posFFT[0] <  fftHalfColumns && posFFT[1] >= fftHalfRows && posFFT[2] <  fftHalfSlices) { sumDist2 += dist; sumPowers2 += powers[p-1]; }
//								else if (posFFT[0] <  fftHalfColumns && posFFT[1] <  fftHalfRows && posFFT[2] >= fftHalfSlices) { sumDist3 += dist; sumPowers3 += powers[p-1]; }
//								else if (posFFT[0] <  fftHalfColumns && posFFT[1] >= fftHalfRows && posFFT[2] >= fftHalfSlices) { sumDist4 += dist; sumPowers4 += powers[p-1]; }
//								else if (posFFT[0] >= fftHalfColumns && posFFT[1] <  fftHalfRows && posFFT[2] <  fftHalfSlices) { sumDist5 += dist; sumPowers5 += powers[p-1]; }
//								else if (posFFT[0] >= fftHalfColumns && posFFT[1] >= fftHalfRows && posFFT[2] <  fftHalfSlices) { sumDist6 += dist; sumPowers6 += powers[p-1]; }
//								else if (posFFT[0] >= fftHalfColumns && posFFT[1] <  fftHalfRows && posFFT[2] >= fftHalfSlices) { sumDist7 += dist; sumPowers7 += powers[p-1]; }
//								else if (posFFT[0] >= fftHalfColumns && posFFT[1] >= fftHalfRows && posFFT[2] >= fftHalfSlices) { sumDist8 += dist; sumPowers8 += powers[p-1]; }					
						
						}
					}
				}	
				
//				//for debug control purposes only
//				//sumDistances should all be equal
//				//sumPowers should be symmetric
//				System.out.println("sumDist1: " + sumDist1);
//				System.out.println("sumDist2: " + sumDist2);
//				System.out.println("sumDist3: " + sumDist3);
//				System.out.println("sumDist4: " + sumDist4);
//				System.out.println("sumDist5: " + sumDist5);
//				System.out.println("sumDist6: " + sumDist6);
//				System.out.println("sumDist7: " + sumDist7);
//				System.out.println("sumDist8: " + sumDist8);
//				System.out.println("sumPower1: " + sumPowers1);
//				System.out.println("sumPower2: " + sumPowers2);
//				System.out.println("sumPower3: " + sumPowers3);
//				System.out.println("sumPower4: " + sumPowers4);
//				System.out.println("sumPower5: " + sumPowers5);
//				System.out.println("sumPower6: " + sumPowers6);
//				System.out.println("sumPower7: " + sumPowers7);
//				System.out.println("sumPower8: " + sumPowers8);
			
				//allKs and powers are unsorted!!
				//Sorting essential for limited NumRegStart NumRegEnd settings
				//Get the sorted index
				Integer[] idx = new Integer[allKs.length];
				for (int i = 0; i < idx.length; i++) idx[i] = i;

				Arrays.sort(idx, new Comparator<Integer>() {
					@Override
					public int compare(Integer idx1, Integer idx2) {
						return Double.compare(allKs[idx1], allKs[idx2]);
					}
				});
				
				//Get sorted vectors
				powersSorted = new float[powers.length];
				allKsSorted  = new float[allKs.length];
				for (int i = 0; i < idx.length; i++) {
					powersSorted[i] = powers[idx[i]];
					allKsSorted[i]  = allKs[idx[i]]; // idx is sorted
				}	
			
				//Go through and average power for same k values
				//Then, number of data points will be lower
				float powMean = 0;
				int numEqualK = 0; // number of equal k values
				int i = 0; //index for new vector;
				int ii = 0; //index for old vector
				float kValue = allKsSorted[0];
				while (i < allKsSorted.length) { //will not be reached
					while (allKsSorted[ii] == kValue) {
						//if  (powersSorted[ii] != 0) {
							powMean += powersSorted[ii];
							numEqualK += 1;	
						//}	
						ii  += 1;
						if (ii >= allKsSorted.length) break;
					}
					powMean = powMean /numEqualK;
					powersCircAverage[i] = powMean;
					allKsCircAverage[i]  = kValue;
					//Reset for the next k
					if (ii < allKsSorted.length - 1) {	
						numEqualK = 0;
						kValue = allKsSorted[ii];
						powMean = 0;
						i += 1; //index for next mean value
					} else {
						break;
					}
				}
				
				//find largest k to be taken
				float maxK = 0; //an actual maximal k value  // is not an integer number
				int maxKIdx = 0;
			
				for (int a = 0; a < allKsCircAverage.length; a++) {
					if (allKsCircAverage[a] >= maxK) {
						maxK = allKsCircAverage[a];
						maxKIdx = a;
					}
				}
					
				totals = new double[maxKIdx+1-1]; //-1 because f=0 is not taken 
				eps    = new double[maxKIdx+1-1];
				for (int k = 0; k < eps.length; k++) { 
					totals[k] = powersCircAverage[k+1]; //do not take f=0 ;
					eps[k]    = allKsCircAverage[k+1];  //do not take f=0;
				}	
			}
			
			if (powerSpecType.equals("Mean of line scans")) { //{"Spherical average", "Mean of line scans", "Integral of line scans"},
			
				ra = raiWindowed.randomAccess();
				
				// Round to next largest power of two. The resulting image will be cropped according to GUI input
				int widthDFT  = width  == 1 ? 1 : Integer.highestOneBit(width  - 1) * 2;
				int heightDFT = height == 1 ? 1 : Integer.highestOneBit(height - 1) * 2;
				int depthDFT  = depth  == 1 ? 1 : Integer.highestOneBit(depth  - 1) * 2;
				
				//All DFT axes must have the same size, otherwise lowest frequencies are not the same for anisotropic sizes
				widthDFT  = (int)Math.max(Math.max(widthDFT, heightDFT), depthDFT); 
				heightDFT = widthDFT;
				depthDFT  = widthDFT;
				
				int maxK = widthDFT/2;  //Take only half of the spectrum //Symmetric DFT  is half the input length
				
				double[] power;
				double[] powerMean = new double[maxK]; //power has size ^2
			
				percent = 1;
				dlgProgress.updatePercent(String.valueOf(percent+"%"));
				dlgProgress.updateBar(percent);
				//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
				statusService.showStatus(percent, 100, "Initializing finished");
				
				//all pillars
				for (int h = 0; h < height; h++) { 
					//System.out.println("FFTDim h "  + h);			
					for (int w = 0; w < width; w++) { 
						sequence = new double[depthDFT];
						for (int d = 0; d < depth; d++) {					
							ra.setPosition(w, 0);
							ra.setPosition(h, 1); //row at position h
							ra.setPosition(d, 2); //depth at position d
							sequence[d] = ((FloatType) ra.get()).get();
						}			
						//power = calcDFTPower(); // very slow 
						power = calcDFTPowerWithApache(); //of variable sequence
						//power = calcDFTPowerWithJTransform(); //as fast as Apache and quite identical results for power^2 sized images
						for (int k = 0; k < (powerMean.length); k++ ) {
							powerMean[k] += power[k]; 
						}
					}
				}
				
				percent = 30;
				dlgProgress.updatePercent(String.valueOf(percent+"%"));
				dlgProgress.updateBar(percent);
				//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
				statusService.showStatus(percent, 100, "Axis 1(3) finished");
					
				//all columns
				for (int w = 0; w < width; w++) {
					for (int d = 0; d < depth; d++) {	
					//System.out.println("FFTDim w "  + w);
						sequence = new double[heightDFT];
						for (int h = 0; h < height; h++) { // one row
							ra.setPosition(w, 0); //column at position w
							ra.setPosition(h, 1);
							ra.setPosition(d, 2); //depth at position d
							sequence[h] = ((FloatType) ra.get()).get();
						}				
						//power = calcDFTPower(); //very slow //of variable sequence
						power = calcDFTPowerWithApache(); //of variable sequence    /Symmetric DFT  is half the input length
						//power = calcDFTPowerWithJTransform(); //as fast as Apache and quite identical results for power^2 sized images
						for (int k = 0; k < (powerMean.length); k++ ) {//Symmetric DFT  is half the input length
							powerMean[k] += power[k]; 
						}
					}
				}
				
				percent = 60;
				dlgProgress.updatePercent(String.valueOf(percent+"%"));
				dlgProgress.updateBar(percent);
				//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
				statusService.showStatus(percent, 100, "Axis2(3) finished");
				//all rows
				for (int h = 0; h < height; h++) {
					for (int d = 0; d < depth; d++) {	
					//System.out.println("FFTDim w "  + w);
						sequence = new double[widthDFT];
						for (int w = 0; w < width; w++) { // one row
							ra.setPosition(w, 0); //column at position w
							ra.setPosition(h, 1);
							ra.setPosition(d, 2); //depth at position d
							sequence[w] = ((FloatType) ra.get()).get();
						}				
						//power = calcDFTPower(); //very slow //of variable sequence
						power = calcDFTPowerWithApache(); //of variable sequence    /Symmetric DFT  is half the input length
						//power = calcDFTPowerWithJTransform(); //as fast as Apache and quite identical results for power^2 sized images
						for (int k = 0; k < (powerMean.length); k++ ) {//Symmetric DFT  is half the input length
							powerMean[k] += power[k]; 
						}
					}
				}
					
				percent = 90;
				dlgProgress.updatePercent(String.valueOf(percent+"%"));
				dlgProgress.updateBar(percent);
				//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
				statusService.showStatus(percent, 100, "Axis 3(3) finished");
				//mean power spectrum
				for (int k = 0; k < powerMean.length; k++ ) {
					powerMean [k] = powerMean[k]/(width+height+depth); 
				}

				//fill up totals with powerMean values
				totals = new double[powerMean.length - 1];  //-1 k=0 zero frequency is skipped
				eps    = new double[powerMean.length - 1];  //-1 k=0 zero frequency is skipped
				for (int n = 0 ; n < totals.length; n++) {
					totals[n] = powerMean[n+1]; //k=0 zero frequency is skipped, does not fit the linear regression 
					eps[n]    = n + 1; 			//k=0 zero frequency is skipped, 
				}
					
			}
			
			else if (powerSpecType.equals("Integral of line scans")) { //{"Spherical average", "Mean of line scans", "Integral of line scans"},
			//Just the same as "Mean of line scans", but additionally a final integration of the mean PS

				ra = raiWindowed.randomAccess();
				
				// Round to next largest power of two. The resulting image will be cropped according to GUI input
				int widthDFT  = width  == 1 ? 1 : Integer.highestOneBit(width  - 1) * 2;
				int heightDFT = height == 1 ? 1 : Integer.highestOneBit(height - 1) * 2;
				int depthDFT  = depth  == 1 ? 1 : Integer.highestOneBit(depth  - 1) * 2;
				
				//All DFT axes must have the same size, otherwise lowest frequencies are not the same for anisotropic sizes
				widthDFT  = (int)Math.max(Math.max(widthDFT, heightDFT), depthDFT); 
				heightDFT = widthDFT;
				depthDFT  = widthDFT;
				
				int maxK = widthDFT/2;  //Take only half of the spectrum //Symmetric DFT  is half the input length
				
				double[] power;
				double[] powerMean = new double[maxK]; //power has size
				double[] powerIntegrated;
				
				percent = 1;
				dlgProgress.updatePercent(String.valueOf(percent+"%"));
				dlgProgress.updateBar(percent);
				//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
				statusService.showStatus(percent, 100, "Initializing finished");
				
				//all rows
				for (int h = 0; h < height; h++) { 
					for (int d = 0; d < depth; d++) { 
						//System.out.println("FFTDim h "  + h);
						sequence = new double[widthDFT];
						for (int w = 0; w < width; w++) { // one row
							ra.setPosition(w, 0);
							ra.setPosition(h, 1); //row at position h
							ra.setPosition(d, 2);
							sequence[w] = ((FloatType) ra.get()).get();
						}
						//power = calcDFTPower(); // very slow 
						power = calcDFTPowerWithApache(); //of variable sequence
						for (int k = 0; k < (powerMean.length); k++ ) {
							powerMean[k] += power[k]; 
						}
					}
				}
				percent = 30;
				dlgProgress.updatePercent(String.valueOf(percent+"%"));
				dlgProgress.updateBar(percent);
				//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
				statusService.showStatus(percent, 100, "Axis 1(3) finished");
				
				//all columns
				for (int w = 0; w < width; w++) {
					for (int d = 0; d < depth; d++) { 
						//System.out.println("FFTDim w "  + w);
						sequence = new double[heightDFT];
						for (int h = 0; h < height; h++) { // one row
							ra.setPosition(w, 0); //column at position w
							ra.setPosition(h, 1);
							ra.setPosition(d, 2); //depth at position d
							sequence[h] = ((FloatType) ra.get()).get();
						}
						//power = calcDFTPower(); //very slow //of variable sequence
						power = calcDFTPowerWithApache(); //of variable sequence
						for (int k = 0; k < (powerMean.length); k++ ) {//Symmetric DFT  is half the input length
							powerMean[k] += power[k]; 
						}	
					}
				}
				percent = 60;
				dlgProgress.updatePercent(String.valueOf(percent+"%"));
				dlgProgress.updateBar(percent);
				//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
				statusService.showStatus(percent, 100, "Axis 2(3) finished");
				
				//all slices
				for (int w = 0; w < width; w++) {
					for (int h = 0; h < height; h++) { 
						//System.out.println("FFTDim w "  + w);
						sequence = new double[depthDFT];
						for (int d = 0; d < depth; d++) {  // one row
							ra.setPosition(w, 0); //column at position w
							ra.setPosition(h, 1);
							ra.setPosition(d, 2); //
							sequence[d] = ((FloatType) ra.get()).get();
						}
						//power = calcDFTPower(); //very slow //of variable sequence
						power = calcDFTPowerWithApache(); //of variable sequence
						for (int k = 0; k < (powerMean.length); k++ ) {//Symmetric DFT  is half the input length
							powerMean[k] += power[k]; 
						}	
					}
				}
				percent = 90;
				dlgProgress.updatePercent(String.valueOf(percent+"%"));
				dlgProgress.updateBar(percent);
				//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
				statusService.showStatus(percent, 100, "Axis 3(3) finished");
							
				//mean power spectrum
				for (int k = 0; k < powerMean.length; k++ ) {
					powerMean [k] = powerMean[k]/(width+height); 
				}

				//Integrate
				powerIntegrated = new double[powerMean.length];
				for (int k = 0; k < powerMean.length; k++ ) {
					for (int kk = k; kk < powerMean.length; kk++ ) {
						powerIntegrated[k] += powerMean[kk]; 
					}
				}
				
				//fill up totals with powerMean values
				totals = new double[powerIntegrated.length - 1];  //-1 k=0 zero frequency is skipped
				eps    = new double[powerIntegrated.length - 1];  //-1 k=0 zero frequency is skipped 
				for (int n = 0 ; n < totals.length; n++) {
					totals[n] = powerIntegrated[n+1]; //k=0 zero frequency is skipped, does not fit the linear regression 
					eps[n]    = n + 1;                //k=0 zero frequency is skipped, 
				}		
			}
			

			//*********************************************************************************************
		
			
		} else if (imageType.equals("RGB")) { // RGB image  //additional check, is already checked during validation of active dataset
		
			//not implemented

		}
		
		//Computing log values for plot 
		//Change sequence of entries to start with a pixel
		double[] lnTotals = new double[totals.length];
		double[] lnEps    = new double[eps.length];
		for (int n = 0; n < totals.length; n++) {
			if (totals[n] == 0) {
				System.out.println("==0 detected");
				lnTotals[n] = Math.log(Double.MIN_VALUE);
			} else if (Double.isNaN(totals[n])) {
				System.out.println("NaN detected");
				lnTotals[n] = Double.NaN;
			} else if (Double.isInfinite(totals[n])) {
				System.out.println("Infinite detected");
				lnTotals[n] = Double.NaN;
			} else {
				lnTotals[n] = Math.log(totals[n]); //
			}	
			if (eps[n] == 0) {
				lnEps[n] = Math.log(Double.MIN_VALUE);
			} else if (Double.isNaN(eps[n])) {
				lnEps[n] = Double.NaN;
			} else {
				lnEps[n] = Math.log(eps[n]); //
			}
			//lnEps[n] = Math.log(eps[n]);
			//logService.info(this.getClass().getName() + " n:" + n + " eps:  " + eps[n]);
			//logService.info(this.getClass().getName() + " n:" + n + " lnEps:  "+  lnEps[n]);
			//logService.info(this.getClass().getName() + " n:" + n + " totals[n][b]: " + totals[n]);	
		}
			
		if (numRegEnd > lnEps.length) {
			numRegEnd = lnEps.length;
			this.spinnerInteger_NumRegEnd = numRegEnd;
		}
		if (numRegStart >= numRegEnd) {
			numRegStart = numRegEnd - 3;
			this.spinnerInteger_NumRegStart = numRegStart;
		}
		if (numRegStart < 1) {
			numRegStart = 1;
			this.spinnerInteger_NumRegStart = numRegStart;
		}
		
		//Create double log plot	
		boolean isLineVisible = false; //?		
		String frameTitle ="Double log plot - 3D FFT dimension";
		String xAxisLabel = "ln(k)";
		String yAxisLabel = "ln(Power)";
		if (optShowPlot) {
			if ((imageType.equals("Grey")) || (imageType.equals("RGB"))) { //both are OK
				String preName = "Volume-";
				Plot_RegressionFrame doubleLogPlot = DisplayRegressionPlotXY(lnEps, lnTotals, isLineVisible, frameTitle, 
						preName + datasetName, xAxisLabel, yAxisLabel, "",
						numRegStart, numRegEnd);
				doubleLogPlotList.add(doubleLogPlot);
			}
			else {
		
			}
		}
		
		// Compute regression
		Regression_Linear lr = new Regression_Linear();
		regressionParams = lr.calculateParameters(lnEps, lnTotals, numRegStart, numRegEnd);
		//0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
			
		//Compute result values
		resultValues = regressionParams;
		double dim = Double.NaN;	
		dim = regressionParams[1];
		if       (choiceRadioButt_PowerSpecType.equals("Spherical average")) {
			dim = (11.0 + regressionParams[1])/2.0;
		}
		else if  (choiceRadioButt_PowerSpecType.equals("Mean of line scans")) {
			dim =  (9.0 + regressionParams[1])/2.0;
		}
		else if (choiceRadioButt_PowerSpecType.equals("Integral of line scans")) {
			dim = (9.0 + regressionParams[1])/2.0;	
		}
		resultValues[1] = dim;
		logService.info(this.getClass().getName() + " 3D FFT dimension: " + dim);
		
		epsRegStartEnd[0] = eps[numRegStart-1];
		epsRegStartEnd[1] = eps[numRegEnd-1];
	
		return new Container_ProcessMethod(resultValues, epsRegStartEnd);
		// Output
		// uiService.show("Table - 3D FFT dimension", table);
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
		int depth  = (int) rai.dimension(2);	
		raiWindowed = new ArrayImgFactory<>(new FloatType()).create(width, height, depth); //always single 3d
		
		double weight = 1.0;
	
		Cursor<FloatType> cursorF = Views.iterable(raiWindowed).localizingCursor();
		long[] pos = new long[raiWindowed.numDimensions()];		
		RandomAccess<RealType<?>> ra = (RandomAccess<RealType<?>>) rai.randomAccess();
		
		while (cursorF.hasNext()){
			cursorF.fwd();
			cursorF.localize(pos);
			ra.setPosition(pos);
			cursorF.get().setReal(ra.get().getRealDouble()*weight); //simply a copy
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
		int depth  = (int) rai.dimension(2);	
		raiWindowed = new ArrayImgFactory<>(new FloatType()).create(width, height, depth); //always single 3d
		
		double r_u;
		double r_v;
		double r_w;
		double r_uvw;
		double weight;
				
		Cursor<FloatType> cursorF = Views.iterable(raiWindowed).localizingCursor();
		long[] pos = new long[raiWindowed.numDimensions()];
		RandomAccess<RealType<?>> ra = (RandomAccess<RealType<?>>) rai.randomAccess();
		while (cursorF.hasNext()){
			cursorF.fwd();
			cursorF.localize(pos);
			ra.setPosition(pos);
			r_u = 2.0*(pos[0]+0.5)/width -1.0;   //+0.5 so that the maximum is really centered
			r_v = 2.0*(pos[1]+0.5)/height -1.0;
			r_w = 2.0*(pos[2]+0.5)/depth -1.0;
			r_uvw = Math.sqrt(r_u*r_u + r_v*r_v + r_w*r_w);
			if ((r_uvw >= 0) && (r_uvw <=1)) weight = 1 - r_uvw;
			else weight = 0.0;	
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
		int depth  = (int) rai.dimension(2);	
		raiWindowed = new ArrayImgFactory<>(new FloatType()).create(width, height, depth); //always single 3d
		
		double r_u;
		double r_v;
		double r_w;
		double r_uvw;
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
			r_w = 2.0*(pos[2]+0.5)/depth-1.0;
			r_uvw = Math.sqrt(r_u*r_u + r_v*r_v + r_w*r_w);
			if ((r_uvw >= 0) && (r_uvw <=1)) weight = 0.54 + 0.46*Math.cos(Math.PI*(r_uvw)); //== 0.54 - 0.46*Math.cos(Math.PI*(1.0-r_uv));
			else weight = 0.0;	
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
		int depth  = (int) rai.dimension(2);	
		raiWindowed = new ArrayImgFactory<>(new FloatType()).create(width, height, depth); //always single 3d
		
		double r_u;
		double r_v;
		double r_w;
		double r_uvw;
		double weight = 0;
		
		Cursor<FloatType> cursorF = Views.iterable(raiWindowed).localizingCursor();
		long[] pos = new long[raiWindowed.numDimensions()];
		RandomAccess<RealType<?>> ra = (RandomAccess<RealType<?>>) rai.randomAccess();
		while (cursorF.hasNext()){
			cursorF.fwd();
			cursorF.localize(pos);
			ra.setPosition(pos);
			r_u = 2.0*(pos[0]+0.5)/width  -1.0;   //+0.5 so that the maximum is really centered
			r_v = 2.0*(pos[1]+0.5)/height -1.0;
			r_w = 2.0*(pos[2]+0.5)/depth  -1.0;
			r_uvw = Math.sqrt(r_u*r_u + r_v*r_v + r_w*r_w);
			if ((r_uvw >= 0) && (r_uvw <=1)) {
				//weight = 0.5*Math.cos(Math.PI*r_uv+1); //Burge Burge  gives negative weights!
				weight = 0.5 + 0.5*Math.cos(Math.PI*(r_uvw)); //== 0.5 - 0.5*Math.cos(Math.PI*(1-r_uv));
			}
			else weight = 0.0;	
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
		int depth  = (int) rai.dimension(2);	
		raiWindowed = new ArrayImgFactory<>(new FloatType()).create(width, height, depth); //always single 3d
		
		double r_u;
		double r_v;
		double r_w;
		double r_uvw;
		double weight;
		
		Cursor<FloatType> cursorF = Views.iterable(raiWindowed).localizingCursor();
		long[] pos = new long[raiWindowed.numDimensions()];
		RandomAccess<RealType<?>> ra = (RandomAccess<RealType<?>>) rai.randomAccess();
		while (cursorF.hasNext()){
			cursorF.fwd();
			cursorF.localize(pos);
			ra.setPosition(pos);
			r_u = 2.0*(pos[0]+0.5)/width - 1.0;   //+0.5 so that the maximum is really centered
			r_v = 2.0*(pos[1]+0.5)/height - 1.0;
			r_w = 2.0*(pos[2]+0.5)/depth - 1.0;
			r_uvw = Math.sqrt(r_u*r_u + r_v*r_v + r_w*r_w);
			//if ((r_uv >= 0) && (r_uv <=1)) weight = 0.42 - 0.5*Math.cos(Math.PI*(1.0-r_uv)) + 0.08*Math.cos(2.0*Math.PI*(1.0-r_uv));
			if ((r_uvw >= 0) && (r_uvw <=1)) weight = 0.42 - 0.5*Math.cos(Math.PI*(1.0-r_uvw)) + 0.08*Math.cos(2.0*Math.PI*(1.0-r_uvw));
			else weight = 0.0;	
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
		int depth  = (int) rai.dimension(2);	
		raiWindowed = new ArrayImgFactory<>(new FloatType()).create(width, height, depth); //always single 3d
		
		double r_u;
		double r_v;
		double r_w;
		double r_uvw;
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
			r_w = 2.0*(pos[2]+0.5)/depth-1.0;
			r_uvw = Math.sqrt(r_u*r_u + r_v*r_v + r_w*r_w);
			weight = Math.exp(-(r_uvw*r_uvw)/(2.0*sigma2));
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
		int depth  = (int) rai.dimension(2);	
		raiWindowed = new ArrayImgFactory<>(new FloatType()).create(width, height, depth); //always single 3d
		
		double r_u;
		double r_v;
		double r_w;
		double r_uvw;
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
			r_w = 2.0*(pos[2]+0.5)/depth-1.0;
			r_uvw = Math.sqrt(r_u*r_u + r_v*r_v + r_w*r_w);
			//if      ((r_uv >= 0) && (r_uv <0.5)) weight = 1.0 - 6.0*Math.pow(r_uv, 2) + 6.0*Math.pow(r_uv, 3); //Burge Burge gives double peaks, seems to be wrong
			if      ((r_uvw >= 0) && (r_uvw <0.5)) weight = 1.0 - 6.0*Math.pow(r_uvw, 2)*(1-r_uvw);
			else if ((r_uvw >= 0.5) && (r_uvw <1)) weight = 2.0*Math.pow(1-r_uvw, 3);
			else    weight = 0.0;	
			cursorF.get().setReal(ra.get().getRealFloat()*weight);
		} 
	    return raiWindowed; 
	}
	
	
	/**
	 * This method calculates the power spectrum of a 1D sequence using Apache method
	 * This is 12 times faster (without padding with zeroes)
	 * @param sequence
	 * @return the DFT power spectrum
	 */
	private double[] calcDFTPowerWithApache() {
	
		//FFT needs power of two
		if (!isPowerOfTwo(sequence.length)) {
			sequence = addZerosUntilPowerOfTwo(sequence);
		}	
		FastFourierTransformer transformer = new FastFourierTransformer(DftNormalization.STANDARD);
	    Complex[] complex = transformer.transform(sequence, TransformType.FORWARD);
	    double[] sequencePower = new double[complex.length];
	    for (int k =0; k < complex.length; k++) {
	    	sequencePower[k] = complex[k].getReal()*complex[k].getReal() + complex[k].getImaginary()*complex[k].getImaginary();  
	    }
		return sequencePower;
	}
	
	
	/**
	 * This method computes if a number is a power of 2
	 * 
	 * @param number
	 * @return
	 */
	public boolean isPowerOfTwo(int number) {
	    if (number % 2 != 0) {
	      return false;
	    } else {
	      for (int i = 0; i <= number; i++) {
	        if (Math.pow(2, i) == number) return true;
	      }
	    }
	    return false;
	 }
	
	/**
	 * This method increases the size of a sequence to the next power of 2 
	 * 
	 * @param sequence
	 * @return
	 */
	public double[] addZerosUntilPowerOfTwo (double[] sequence) {
		int p = 1;
		double[] newSequence;
		int oldLength = sequence.length;
		while (Math.pow(2, p) < oldLength) {
			p = p +1;
	    }
		newSequence = new double[(int) Math.pow(2, p)];
		for (int i = 0; i < oldLength; i++) {
			newSequence[i] = sequence[i];
		}
		return newSequence;
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
			Plot_RegressionFrame doubleLogPlot = DisplayRegressionPlotXY(lnDataX, lnDataY, isLineVisible,
					"Double log plot - 3D FFT dimension", preName + datasetName, "ln(k)", "ln(Power)", "", numRegStart, numRegEnd);
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
	private Plot_RegressionFrame DisplayRegressionPlotXY(double[] dataX, double[] dataY,
			boolean isLineVisible, String frameTitle, String plotLabel, String xAxisLabel, String yAxisLabel,String legendLabel,
			int numRegStart, int numRegEnd) {
		// jFreeChart
		Plot_RegressionFrame pl = new Plot_RegressionFrame(dataX, dataY, isLineVisible, frameTitle, plotLabel, xAxisLabel,
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

	/**
	 * This method computes the maximal number of possible k's
	 */
	private int getMaxK(int width, int height, int depth) { //

		// Ensure that dft dimensions are the nearest power of two to actual dimension
		int widthDFT  = width  == 1 ? 1 : Integer.highestOneBit(width  - 1) * 2;
		int heightDFT = height == 1 ? 1 : Integer.highestOneBit(height - 1) * 2;
		int depthDFT  = depth  == 1 ? 1 : Integer.highestOneBit(depth  - 1) * 2;
			
		//All DFT axes must have the same size, otherwise lowest frequencies are not the same for anisotropic sizes
		widthDFT  = (int)Math.max(Math.max(widthDFT, heightDFT), depthDFT); 
		heightDFT = widthDFT;
		depthDFT  = widthDFT;
		
		if (choiceRadioButt_PowerSpecType != null) { //during startup it is null
			//"Spherical average", "Mean of line scans", "Integral of line scans"
			if (choiceRadioButt_PowerSpecType.equals("Spherical average")) {
			
				//Will be lowered later, after averaging
				numOfK = widthDFT * heightDFT * depthDFT; 
			}
			// For the 1D projections max k is calculated differently
			else if ((choiceRadioButt_PowerSpecType.equals("Mean of line scans")) || (choiceRadioButt_PowerSpecType.equals("Integral of line scans"))) {				
				//Will be lowered later, after averaging
				numOfK = widthDFT/2 -1; 			
			}
		} else { //during startup it is null
			numOfK = widthDFT * heightDFT * depthDFT;
		}

		return numOfK;
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
