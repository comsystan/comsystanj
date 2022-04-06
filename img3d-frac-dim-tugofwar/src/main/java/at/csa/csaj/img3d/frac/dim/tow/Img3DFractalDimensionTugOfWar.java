/*-
 * #%L
 * Project: ImageJ2 plugin for computing fractal dimension with 3D tug of war algorithm.
 * File: Img3DFractalDimensionTugOfWar.java
 * 
 * $Id$
 * $HeadURL$
 * 
 * This file is part of ComsystanJ software, hereinafter referred to as "this program".
 * %%
 * Copyright (C) 2020 - 2022 Comsystan Software
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


package at.csa.csaj.img3d.frac.dim.tow;

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
import at.csa.csaj.commons.dialog.WaitingDialogWithProgressBar;
import at.csa.csaj.commons.plot.RegressionPlotFrame;
import at.csa.csaj.commons.regression.LinearRegression;
import io.scif.DefaultImageMetadata;
import io.scif.MetaTable;

/**
 * A {@link ContextCommand} plugin computing <the 3D fractal Tug of war dimension</a>
 * of an image volume.
 */
@Plugin(type = ContextCommand.class,
headless = true,
label = "3D Tug of war  dimension",
initializer = "initialPluginLaunch",
//iconPath = "/images/comsystan-??.png", //Menu entry icon
menu = {
@Menu(label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT, mnemonic = MenuConstants.PLUGINS_MNEMONIC),
@Menu(label = "ComsystanJ"),
@Menu(label = "Image (3D)"),
@Menu(label = "3D Tug of war dimension", weight = 60)})
//public class Img3DFractalDimensionTugOfWar<T extends RealType<T>> extends InteractiveCommand { // non blocking  GUI
public class Img3DFractalDimensionTugOfWar<T extends RealType<T>> extends ContextCommand implements Previewable { //modal GUI with cancel

	private static final String PLUGIN_LABEL            = "Computes fractal dimension with 3D tug of war algorithm";
	private static final String SPACE_LABEL             = "";
	private static final String REGRESSION_LABEL        = "<html><b>Regression parameters</b></html>";
	private static final String METHODOPTIONS_LABEL     = "<html><b>Method options</b></html>";
	//private static final String BACKGROUNDOPTIONS_LABEL = "<html><b>Background option</b></html>";
	private static final String DISPLAYOPTIONS_LABEL    = "<html><b>Display options</b></html>";
	private static final String PROCESSOPTIONS_LABEL    = "<html><b>Process options</b></html>";
	
	private static RandomAccessibleInterval<?> rai;
	private static RandomAccess<UnsignedByteType> ra;
	private static RandomAccessibleInterval<?> raiBox;
	private static Cursor<?> cursor = null;
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
	private static int  numBoxes = 0;
	
	private static double[] sequence;
	private static ArrayList<RegressionPlotFrame> doubleLogPlotList = new ArrayList<RegressionPlotFrame>();
	private static double[] resultValuesTable; //the corresponding regression values
	private static final String tableOutName = "Table - 3D FFT dimension";
	
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

	 @Parameter(label = "Number of boxes",
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
     private final String labelMethodOptions = METHODOPTIONS_LABEL;
     
     @Parameter(label = "Accuracy",
 		        description = "Accuracy (default=30)",
	       	    style = NumberWidget.SPINNER_STYLE,
	            min = "1",
	            max = "99999999999999",
	            stepSize = "1",
	            persist = true,  //restore previous value default = true
	            initializer = "initialNumAccuracy",
	            callback    = "callbackNumAccuracy")
     private int spinnerInteger_NumAcurracy;
     
     @Parameter(label = "Confidence",
 		        description = "Confidence (default=5)",
	       	    style = NumberWidget.SPINNER_STYLE,
	            min = "1",
	            max = "99999999999999",
	            stepSize = "1",
	            persist = true,  //restore previous value default = true
	            initializer = "initialNumConfidence",
	            callback    = "callbackNumConfidence")
     private int spinnerInteger_NumConfidence;

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
	
	@Parameter(label   = "    Process single volume     ",
		       callback = "callbackProcessSingleVolume")
	private Button buttonProcessSingleVolume;
	
//	Deactivated, because it does not work in Fiji (although it works in ImageJ2 -Eclipse)	
//@Parameter(label   = "Process single active image ",
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
      	numBoxes = getMaxBoxNumber(datasetIn.dimension(0), datasetIn.dimension(1), datasetIn.dimension(2));
      	spinnerInteger_NumBoxes = numBoxes;
    }
    protected void initialRegMin() {
    	spinnerInteger_RegMin = 1;
    }
    protected void initialRegMax() {
    	numBoxes = getMaxBoxNumber(datasetIn.dimension(0), datasetIn.dimension(1), datasetIn.dimension(2));
    	spinnerInteger_RegMax =  numBoxes;
    }
 
    protected void initialNumAccuracy() {
    	spinnerInteger_NumAcurracy = 30; //s1=30 Wang paper
    }
    
    protected void initialNumConfidence() {
    	spinnerInteger_NumConfidence = 5; //s2=5 Wang paper
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
	
	/** Executed whenever the {@link #spinnerInteger_NumAccuracy} parameter changes. */
	protected void callbackNumAccuracy() {
		logService.info(this.getClass().getName() + " Accuracy set to " + spinnerInteger_NumAcurracy);
	}
	
	/** Executed whenever the {@link #spinnerInteger_NumConfidence} parameter changes. */
	protected void callbackNumConfidence() {
		logService.info(this.getClass().getName() + " Confidence set to " + spinnerInteger_NumConfidence);
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
			logService.info(this.getClass().getName() + " WARNING: Grey value image volume expected!");
			this.cancel("WARNING: Grey value image volume expected!");
		}
	}

	/**
	* This method starts the workflow for a single image of the active display
	*/
	protected void startWorkflowForSingleVolume() {
	
		dlgProgress = new WaitingDialogWithProgressBar("Computing 3D FFT dimension, please wait... Open console window for further info.",
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

		resultValuesTable = new double[10];
	
		//get rai
		RandomAccessibleInterval<T> rai = null;	
	
		rai =  (RandomAccessibleInterval<T>) datasetIn.getImgPlus(); //dim==3

		// Compute regression parameters
		double[] regressionValues = process(rai); //rai is 3D

		//set values for output table
		for (int i = 0; i < regressionValues.length; i++ ) {
			resultValuesTable[i] = regressionValues[i]; 
		}
		//Compute dimension
		double dim = Double.NaN;	
		
		dim = regressionValues[1];
		resultValuesTable[1] = dim;
	
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
		IntColumn columnAccuracy           = new IntColumn("Accuracy");
		IntColumn columnConfidence         = new IntColumn("Confidence");
		DoubleColumn columnDp              = new DoubleColumn("Dtow");
		DoubleColumn columnR2              = new DoubleColumn("R2");
		DoubleColumn columnStdErr          = new DoubleColumn("StdErr");

		tableOut = new DefaultGenericTable();
		tableOut.add(columnFileName);
		tableOut.add(columnMaxNumBoxes);
		tableOut.add(columnRegMin);
		tableOut.add(columnRegMax);
		tableOut.add(columnAccuracy);
		tableOut.add(columnConfidence);
		tableOut.add(columnDp);
		tableOut.add(columnR2);
		tableOut.add(columnStdErr);
	}

	/** 
	*  writes current result to table
	*  @param int slice number of active image.
	*/
	private void writeSingleResultToTable() { 

		int numImages     = spinnerInteger_NumBoxes;
		int regMin        = spinnerInteger_RegMin;
		int regMax        = spinnerInteger_RegMax;
		int accuracy 	  = spinnerInteger_NumAcurracy;
		int confidence    = spinnerInteger_NumConfidence;

		// 0 Intercept, 1 Dim, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
		// fill table with values
		tableOut.appendRow();
		tableOut.set("File name",    tableOut.getRowCount()-1, datasetName);	
		tableOut.set("# Boxes",    	 tableOut.getRowCount()-1, numImages);	
		tableOut.set("RegMin",       tableOut.getRowCount()-1, regMin);	
		tableOut.set("RegMax",       tableOut.getRowCount()-1, regMax);
		tableOut.set("Accuracy",     tableOut.getRowCount()-1, accuracy);	
		tableOut.set("Confidence",   tableOut.getRowCount()-1, confidence);	
		tableOut.set("Dtow",         tableOut.getRowCount()-1, resultValuesTable[1]);
		tableOut.set("R2",           tableOut.getRowCount()-1, resultValuesTable[4]);
		tableOut.set("StdErr",       tableOut.getRowCount()-1, resultValuesTable[3]);	
	}

	/**
	*
	* Processing
	*/
	private double[] process(RandomAccessibleInterval<?> rai) { //3Dvolume
	
		dlgProgress.setBarIndeterminate(false);
		int percent;
		
		if (rai == null) {
			logService.info(this.getClass().getName() + " WARNING: rai==null, no image for processing!");
		}
		
		int numBoxes      = spinnerInteger_NumBoxes;
		int regMin        = spinnerInteger_RegMin;
		int regMax        = spinnerInteger_RegMax;
		int accuracy 	  = spinnerInteger_NumAcurracy;
		int confidence    = spinnerInteger_NumConfidence;
		
		boolean optShowPlot  = booleanShowDoubleLogPlot;

		int width  = (int)rai.dimension(0);
		int height = (int)rai.dimension(1);
		int depth  = (int)rai.dimension(2);
		
		double[] regressionParams = null;
		
		//Convert image to float
		//Img<T> image = (Img<T>) dataset.getImgPlus();
		//RandomAccessibleInterval<T> rai = (RandomAccessibleInterval<T>)dataset.getImgPlus();
		//IterableInterval ii = dataset.getImgPlus();
		//Img<FloatType> imgFloat = opService.convert().float32(ii);
		
		double[] totals = new double[numBoxes];
		int[]eps        = new int[numBoxes];
		
		// definition of eps
		for (int n = 0; n < numBoxes; n++) {
			eps[n] = (int)(Math.round(Math.pow(2, n)));
			//logService.info(this.getClass().getName() + " n:" + n + " eps:  " + eps[n]);	
		}		
		
		//Tug of war parameters
		int s1=accuracy; 	//accuracy parameter	//s1=30 (Paper)
		int s2=confidence;	//confidence parameter	//s2=5 (Paper)
				
		double count[]  = new double[s1];
		double meanS1[] = new double[s2];
		
		long totalNumberOfPoints = 0;
		
		double count_sum = 0.0;
		
		int q = 8837;
		int xx = 0;
		int yy = 0;
		int zz = 0;

		int x_part = 0;
		int y_part = 0;
		int z_part = 0;

		int hash_function = 0;
		int k = 0;
		
		boolean even;
		int sample;
		
		if (imageType.equals("Grey")) {// grey image   //additional check, is already checked during validation of active dataset
		
			// Count total number of points		
			cursor = Views.iterable(rai).localizingCursor();
			while (cursor.hasNext()) {
				cursor.fwd();
				sample = ((UnsignedByteType) cursor.get()).getInteger();
				if (sample > 0) totalNumberOfPoints = totalNumberOfPoints + 1;		
			}
			
			// Get coordinates
			ra = (RandomAccess<UnsignedByteType>) rai.randomAccess();
			int[] xCoordinate=new int [(int)totalNumberOfPoints];
			int[] yCoordinate=new int [(int)totalNumberOfPoints];	
			int[] zCoordinate=new int [(int)totalNumberOfPoints];	
			
			k = 0;
			do {
				for(int x = 0; x < width; x++){	
					for(int y = 0; y < height; y++){
						for(int z = 0; z < depth; z++){
							ra.setPosition(x, 0);
							ra.setPosition(y, 1);
							ra.setPosition(z, 2);
							if (ra.get().getInteger() > 0 ){	
								xCoordinate[k] = x;
								yCoordinate[k] = y;
								zCoordinate[k] = z;
								k++;
							}
						}
					}
				}
			} while ( k < totalNumberOfPoints );
			
			int radius = 0;
			int a_prim = 0;
			int b_prim = 0;
			int c_prim = 0;
			int d_prim = 0;
	
			percent = 5;
			dlgProgress.updatePercent(String.valueOf(percent+"%"));
			dlgProgress.updateBar(percent);
			//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
			statusService.showStatus(percent, 100, "Initializing finished");
			
			for (int n = 0; n < numBoxes; n++) {			
				
				percent = (int)Math.max(Math.round((  ((float)n)/((float)numBoxes)   *100.f   )), percent);
				dlgProgress.updatePercent(String.valueOf(percent+"%"));
				dlgProgress.updateBar(percent);
				//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
				statusService.showStatus((n+1), numBoxes, "Processing " + (n+1) + "/" + numBoxes);
				
				radius = eps[n];
				
				// STEP 1: PARTITION s1*s2 COUNTERS INTO s2 SETS OF s1 COUNTERS
				for(int s2_i = 0; s2_i < s2; s2_i++){
					for(int s1_i = 0; s1_i < s1; s1_i++){	
					
						a_prim = getPrimeNumber();
						b_prim = getPrimeNumber();
						c_prim = getPrimeNumber();
						d_prim = getPrimeNumber();

						// sum over coordinates
						for(int i = 0; i < totalNumberOfPoints; i++){	
							xx=(int) Math.floor( xCoordinate[i]/radius); 
							yy=(int) Math.floor( yCoordinate[i]/radius); 
							zz=(int) Math.floor( zCoordinate[i]/radius); 
							
							// hash function in parts
							x_part =               a_prim*xx*xx*xx +               b_prim*xx*xx +               c_prim*xx +               d_prim;
							y_part =        a_prim*a_prim*yy*yy*yy +        b_prim*b_prim*yy*yy +        c_prim*c_prim*yy +        d_prim*d_prim;
							z_part = a_prim*a_prim*a_prim*zz*zz*zz + b_prim*b_prim*b_prim*zz*zz + c_prim*c_prim*c_prim*zz + d_prim*d_prim*d_prim;

							hash_function = (x_part+y_part+z_part)%q; // UPDATE 07.08.2013 

							even = (hash_function & 1) == 0; // Very slow! Try hash_function=1000 

							if (even) {
								count[s1_i]++;
							} else {
								count[s1_i]+=-1;
							}
						} // end sum over coordinates
					} // end s1 loop

					for(int s1_i = 0; s1_i < s1; s1_i++){
					count_sum += count[s1_i]*count[s1_i]; // square the counters values	
					}
					// STEP 2: COMPUTE THE MEAN OF EACH SUBSET
					meanS1[s2_i] = count_sum / s1; 
					
					// Set values equal to zero
					count_sum = 0.0;
					for(int s1_i = 0; s1_i < s1; s1_i++){	
					count[s1_i] = 0;
					}
					
				} // End s2 loop
				
				// STEP 3: DETERMINE THE MEDIAN
				for(int s2_i = 0; s2_i < s2; s2_i++){
				totals[n] += meanS1[s2_i] / s2;	
				}

				// Set values equal to zero
//				for(int s2_i = 0; s2_i < s2; s2_i++){	
//				meanS1[s2_i] = 0;
//				}	
				meanS1 = new double[s2];
			}// end boxsize loop	
		
			
		} else if (imageType.equals("RGB")) { // RGB image  //additional check, is already checked during validation of active dataset
		
			//not implemented

		}
		
		//Computing log values for plot 
		//Change sequence of entries to start with a pixel
		double[] lnTotals = new double[totals.length];
		double[] lnEps    = new double[eps.length];
		for (int n = 0; n < totals.length; n++) {
			if (totals[n] <= 0) {
				System.out.println("<=0 detected");
				lnTotals[n] = Double.NaN;
			} else if (Double.isNaN(totals[n])) {
				System.out.println("NaN detected");
				lnTotals[n] = Double.NaN;
			} else if (Double.isInfinite(totals[n])) {
				System.out.println("Infinite detected");
				lnTotals[n] = Double.NaN;
			} else {
				lnTotals[n] = Math.log(totals[n]); //
			}	
			if (eps[n] <= 0) {
				lnEps[n] = -Double.MAX_VALUE;
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
			
		if (regMax > lnEps.length) {
			regMax = lnEps.length;
			this.spinnerInteger_RegMax = regMax;
		}
		if (regMin >= regMax) {
			regMin = regMax - 3;
			this.spinnerInteger_RegMin = regMin;
		}
		if (regMin < 1) {
			regMin = 1;
			this.spinnerInteger_RegMin = regMin;
		}
		
		//Create double log plot	
		boolean isLineVisible = false; //?		
		String frameTitle = "Double Log Plot - 3D Tug of war dimension";
		String xAxisLabel = "ln(Box width)";
		String yAxisLabel = "ln(Count)";
		if (optShowPlot) {
			if ((imageType.equals("Grey")) || (imageType.equals("RGB"))) { //both are OK
				String preName = "Volume-";
				RegressionPlotFrame doubleLogPlot = DisplayRegressionPlotXY(lnEps, lnTotals, isLineVisible, frameTitle, 
						preName + datasetName, xAxisLabel, yAxisLabel, "",
						regMin, regMax);
				doubleLogPlotList.add(doubleLogPlot);
			}
			else {
		
			}
		}
		
		// Compute regression
		LinearRegression lr = new LinearRegression();
		regressionParams = lr.calculateParameters(lnEps, lnTotals, regMin, regMax);
		//0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
			
		return regressionParams;
		// Output
		// uiService.show("Table - 3D FFT dimension", table);
	}
	
private int getPrimeNumber(){
		
		int[] primeNumbers = {
			  46349,48533,44123,
			  46351,48539,44129,
			  46381,48541,44131,
			  46399,48563,44159,
			  46411,48571,44171,
			  46439,48589,44179,
			  46441,48593,44189,
			  46447,48611,44201,
			  46451,48619,44203,
			  46457,48623,44207,
			  46471,48647,44221,
			  46477,48649,44249,
			  46489,48661,44257,
			  46499,48673,44263,
			  46507,48677,44267,
			  46511,48679,44269,
			  46523,48731,44273,
			  46549,48733,44279,
			  46559,48751,44281,
			  46567,48757,44293,
			  46573,48761,44351,
			  46589,48767,44357,
			  46591,48779,44371,
			  46601,48781,44381,
			  46619,48787,44383,
			  46633,48799,44389,
			  46639,48809,44417,
			  46643,48817,44449,
			  46649,48821,44453,
			  46663,48823,44483,
			  46679,48847,44491,
			  46681,48857,44497,
			  46687,48859,44501,
			  46691,48869,44507,
			  46703,48871,44519,
			  46723,48883,44531,
			  46727,48889,44533,
			  46747,48907,44537,
			  46751,48947,44543,
			  46757,48953,44549,
			  46769,48973,44563,
			  46771,48989,44579,
			  46807,48991,44587,
			  46811,49003,44617,
			  46817,49009,44621,
			  46819,49019,44623,
			  46829,49031,44633,
			  46831,49033,44641,
			  46853,49037,44647,
			  46861,49043,44651,
			  46867,49057,44657,
			  46877,49069,44683,
			  46889,49081,44687,
			  46901,49103,44699,
			  46919,49109,44701,
			  46933,49117,44711,
			  46957,49121,44729,
			  46993,49123,44741,
			  46997,49139,44753,
			  47017,49157,44771,
			  47041,49169,44773,
			  47051,49171,44777,
			  47057,49177,44789,
			  47059,49193,44797,
			  47087,49199,44809,
			  47093,49201,44819,
			  47111,49207,44839,
			  47119,49211,44843,
			  47123,49223,44851,
			  47129,49253,44867,
			  47137,49261,44879,
			  47143,49277,44887,
			  47147,49279,44893,
			  47149,49297,44909,
			  47161,49307,44917,
			  47189,49331,44927,
			  47207,49333,44939,
			  47221,49339,44953,
			  47237,49363,44959,
			  47251,49367,44963,
			  47269,49369,44971,
			  47279,49391,44983,
			  47287,49393,44987,
			  47293,49409,45007,
			  47297,49411,45013,
			  47303,49417,45053,
			  47309,49429,45061,
			  47317,49433,45077,
			  47339,49451,45083,
			  47351,49459,45119,
			  47353,49463,45121,
			  47363,49477,45127,
			  47381,49481,45131,
			  47387,49499,45137,
			  47389,49523,45139,
			  47407,49529,45161,
			  47417,49531,45179,
			  47419,49537,45181,
			  47431,49547,45191,
			  47441,49549,45197,
			  47459,49559,45233,
			  47491,49597,45247,
			  47497,49603,45259,
			  47501,49613,45263,
			  47507,49627,45281,
			  47513,49633,45289,
			  47521,49639,45293,
			  47527,49663,45307,
			  47533,49667,45317,
			  47543,49669,45319,
			  47563,49681,45329,
			  47569,49697,45337,
			  47581,49711,45341,
			  47591,49727,45343,
			  47599,49739,45361,
			  47609,49741,45377,
			  47623,49747,45389,
			  47629,49757,45403,
			  47639,49783,45413,
			  47653,49787,45427,
			  47657,49789,45433,
			  47659,49801,45439,
			  47681,49807,45481,
			  47699,49811,45491,
			  47701,49823,45497,
			  47711,49831,45503,
			  47713,49843,45523,
			  47717,49853,45533,
			  47737,49871,45541,
			  47741,49877,45553,
			  47743,49891,45557,
			  47777,49919,45569,
			  47779,49921,45587,
			  47791,49927,45589,
			  47797,49937,45599,
			  47807,49939,45613,
			  47809,49943,45631,
			  47819,49957,45641,
			  47837,49991,45659,
			  47843,49993,45667,
			  47857,49999,45673,
			  47869,50021,45677,
			  47881,50023,45691,
			  47903,50033,45697,
			  47911,50047,45707,
			  47917,50051,45737,
			  47933,50053,45751,
			  47939,50069,45757,
			  47947,50077,45763,
			  47951,50087,45767,
			  47963,50093,45779,
			  47969,50101,45817,
			  47977,50111,45821,
			  47981,50119,45823,
			  48017,50123,45827,
			  48023,50129,45833,
			  48029,50131,45841,
			  48049,50147,45853,
			  48073,50153,45863,
			  48079,50159,45869,
			  48091,50177,45887,
			  48109,50207,45893,
			  48119,50221,45943,
			  48121,50227,45949,
			  48131,50231,45953,
			  48157,50261,45959,
			  48163,50263,45971,
			  48179,50273,45979,
			  48187,50287,45989,
			  48193,50291,46021,
			  48197,50311,46027,
			  48221,50321,46049,
			  48239,50329,46051,
			  48247,50333,46061,
			  48259,50341,46073,
			  48271,50359,46091,
			  48281,50363,46093,
			  48299,50377,46099,
			  48311,50383,46103,
			  48313,50387,46133,
			  48337,50411,46141,
			  48341,50417,46147,
			  48353,50423,46153,
			  48371,50441,46171,
			  48383,50459,46181,
			  48397,50461,46183,
			  48407,50497,46187,
			  48409,50503,46199,
			  48413,50513,46219,
			  48437,50527,46229,
			  48449,50539,46237,
			  48463,50543,46261,
			  48473,50549,46271,
			  48479,50551,46273,
			  48481,50581,46279,
			  48487,50587,46301,
			  48491,50591,46307,
			  48497,50593,46309,
			  48523,50599,46327,
			  48527,50627,44119
			};
		
		int randn = (int)(Math.random()*150);
		return primeNumbers[randn]; 	
	}
	
	// This method shows the double log plot
	private void showPlot(double[] lnDataX, double[] lnDataY, String preName, int regMin, int regMax) {
		if (imageType.equals("Grey")) {
			if (lnDataX == null) {
				logService.info(this.getClass().getName() + " lnDataX == null, cannot display the plot!");
				return;
			}
			if (lnDataY == null) {
				logService.info(this.getClass().getName() + " lnDataY == null, cannot display the plot!");
				return;
			}
			if (regMin >= regMax) {
				logService.info(this.getClass().getName() + " regMin >= regMax, cannot display the plot!");
				return;
			}
			if (regMax <= regMin) {
				logService.info(this.getClass().getName() + " regMax <= regMin, cannot display the plot!");
				return;
			}
			// String preName = "";
			if (preName == null) {
				preName = "Volume-";
			} else {
				preName = "Volume-";
			}
			boolean isLineVisible = false; // ?
			RegressionPlotFrame doubleLogPlot = DisplayRegressionPlotXY(lnDataX, lnDataY, isLineVisible,
					"Double Log Plot - 3D FFT dimension", preName + datasetName, "ln(k)", "ln(Power)", "", regMin, regMax);
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
	 * @param regMin                minimum value for regression range
	 * @param regMax                maximal value for regression range
	 * @param optDeleteExistingPlot option if existing plot should be deleted before
	 *                              showing a new plot
	 * @param interpolType          The type of interpolation
	 * @return RegressionPlotFrame
	 */
	private RegressionPlotFrame DisplayRegressionPlotXY(double[] dataX, double[] dataY,
			boolean isLineVisible, String frameTitle, String plotLabel, String xAxisLabel, String yAxisLabel,String legendLabel,
			int regMin, int regMax) {
		// jFreeChart
		RegressionPlotFrame pl = new RegressionPlotFrame(dataX, dataY, isLineVisible, frameTitle, plotLabel, xAxisLabel,
				yAxisLabel, legendLabel, regMin, regMax);
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
		// ij.command().run(Img3DFractalDimensionTugOfWar.class,
		// true).get().getOutput("image");
		ij.command().run(Img3DFractalDimensionTugOfWar.class, true);
	}
}
