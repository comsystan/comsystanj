/*-
 * #%L
 * Project: ImageJ2 plugin for computing fractal dimension with 3D Higuchi algorithms.
 * File: Img3DFractalDimensionHiguchi3D.java
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


package at.csa.csaj.img3d.frac.dim.hig3d;

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
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
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
import org.scijava.table.BoolColumn;
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
import at.csa.csaj.commons.dialog.WaitingDialogWithProgressBar;
import at.csa.csaj.commons.plot.RegressionPlotFrame;
import at.csa.csaj.commons.regression.LinearRegression;
import at.csa.csaj.img3d.frac.dim.hig3d.util.Higuchi3DMethods;
import at.csa.csaj.img3d.frac.dim.hig3d.util.Higuchi3D_Grey_DirDiff;
import at.csa.csaj.img3d.frac.dim.hig3d.util.Higuchi3D_Grey_MultDiff;
import at.csa.csaj.img3d.frac.dim.hig3d.util.Higuchi3D_Grey_SqrDiff;
import io.scif.DefaultImageMetadata;
import io.scif.MetaTable;

/**
 * A {@link ContextCommand} plugin computing <the 3D Higuchi dimension</a>
 * of an image volume.
 */
@Plugin(type = ContextCommand.class,
headless = true,
label = "Higuchi dimension 3D",
initializer = "initialPluginLaunch",
//iconPath = "/images/comsystan-??.png", //Menu entry icon
menu = {
@Menu(label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT, mnemonic = MenuConstants.PLUGINS_MNEMONIC),
@Menu(label = "ComsystanJ"),
@Menu(label = "Image (3D)"),
@Menu(label = "Higuchi dimension 3D", weight = 50)})
//public class Img3DFractalDimensionHiguchi3D<T extends RealType<T>> extends InteractiveCommand { // non blocking  GUI
public class Img3DFractalDimensionHiguchi3D<T extends RealType<T>> extends ContextCommand implements Previewable { //modal GUI with cancel

	private static final String PLUGIN_LABEL            = "Computes fractal dimension with Higuchi 3D algorithms";
	private static final String SPACE_LABEL             = "";
	private static final String REGRESSION_LABEL        = "<html><b>Regression parameters</b></html>";
	private static final String METHODOPTIONS_LABEL     = "<html><b>Method</b></html>";
	private static final String BACKGROUNDOPTIONS_LABEL = "<html><b>Background option</b></html>";
	private static final String DISPLAYOPTIONS_LABEL    = "<html><b>Display options</b></html>";
	private static final String PROCESSOPTIONS_LABEL    = "<html><b>Process options</b></html>";
	//private static Img<FloatType> imgFloat;
	private static String datasetName;
	private static long width = 0;
	private static long height = 0;
	private static long depth = 0;
	private static long numDimensions = 0;
	private static long numSlices = 0;
	private static int numVolumes = 0;
	private static long compositeChannelCount =0;
	private static String imageType = "";
	private static int  numbKMax = 0;
	private static ArrayList<RegressionPlotFrame> doubleLogPlotList = new ArrayList<RegressionPlotFrame>();
	private static double[] resultValuesTable; //the corresponding regression values
	private static final String tableOutName = "Table - Higuchi3D dimension";
	
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

	@Parameter(label = "k", description = "Maximal delay between data points", style = NumberWidget.SPINNER_STYLE, min = "3", max = "32768", stepSize = "1",
			   persist = false, // restore  previous value  default  =  true
			   initializer = "initialKMax", callback = "callbackKMax")
	private int spinnerInteger_KMax;

	@Parameter(label = "Regression Min", description = "Minimum x value of linear regression", style = NumberWidget.SPINNER_STYLE, min = "1", max = "32768", stepSize = "1",
			   persist = false, //restore previous value default = true
			   initializer = "initialRegMin", callback = "callbackRegMin")
	private int spinnerInteger_RegMin = 1;

	@Parameter(label = "Regression Max", description = "Maximum x value of linear regression", style = NumberWidget.SPINNER_STYLE, min = "3", max = "32768", stepSize = "1",
			   persist = false, //restore previous value default = true
			   initializer = "initialRegMax", callback = "callbackRegMax")
	private int spinnerInteger_RegMax = 8;

	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelInterpolation = METHODOPTIONS_LABEL;

	@Parameter(label = "Method", description = "Type of Higuchi 3D algorithm", style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE, choices = {
			   "Multiplicated differences",														   
			   "Squared differences",
			   "Direct differences",
			   },
			   persist = true, //restore previous value default = true
			   initializer = "initialMethod", callback = "callbackMethod")
	private String choiceRadioButt_Method;

	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelBackgroundOptions = BACKGROUNDOPTIONS_LABEL;
	
	@Parameter(label = "Skip zero values",
			   persist = true,
		       callback = "callbackSkipZeroes")
	private boolean booleanSkipZeroes;
	
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
	
	protected void initialKMax() {
		numbKMax = (int) Math.floor((Math.min(datasetIn.dimension(0), datasetIn.dimension(1))) / 3.0);
		spinnerInteger_KMax = numbKMax;
	}

	protected void initialRegMin() {
		spinnerInteger_RegMin = 1;
	}

	protected void initialRegMax() {
		spinnerInteger_RegMax = (int) Math.floor((Math.min(datasetIn.dimension(0), datasetIn.dimension(1))) / 3.0);
	}

	protected void initialMethod() {
		choiceRadioButt_Method = "Direct differences";
		//Check if Image type and selected Method fits together 	
		if (imageType.equals("Grey")) {// grey image
			if	(false){
				Result result = uiService.showDialog("An RGB algorithm cannot be applied to gray image volumes!", "Alert", MessageType.WARNING_MESSAGE, OptionType.DEFAULT_OPTION);
			}
		}	
		if (imageType.equals("RGB")) {// RGB image
			//******************************************************************************************************
			if  ( (choiceRadioButt_Method.equals("Multiplicated differences"))
				||(choiceRadioButt_Method.equals("Squared differences"))
				||(choiceRadioButt_Method.equals("Direct differences")) )
			{
				Result result = uiService.showDialog("A grey value algorithm cannot be applied to RGB image volumes!", "Alert", MessageType.WARNING_MESSAGE, OptionType.DEFAULT_OPTION);
			}
		}
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
	

	/** Executed whenever the {@link #spinnerInteger_KMax} parameter changes. */
	protected void callbackKMax() {

		if (spinnerInteger_KMax < 3) {
			spinnerInteger_KMax = 3;
		}
		if (spinnerInteger_KMax > numbKMax) {
			spinnerInteger_KMax = numbKMax;
		}
		if (spinnerInteger_RegMax > spinnerInteger_KMax) {
			spinnerInteger_RegMax = spinnerInteger_KMax;
		}
		if (spinnerInteger_RegMin >= spinnerInteger_RegMax - 2) {
			spinnerInteger_RegMin = spinnerInteger_RegMax - 2;
		}
		logService.info(this.getClass().getName() + " k set to " + spinnerInteger_KMax);
	}

	/** Executed whenever the {@link #spinnerInteger_RegMin} parameter changes. */
	protected void callbackRegMin() {
		if (spinnerInteger_RegMin >= spinnerInteger_RegMax - 2) {
			spinnerInteger_RegMin = spinnerInteger_RegMax - 2;
		}
		if (spinnerInteger_RegMin < 1) {
			spinnerInteger_RegMin = 1;
		}
		logService.info(this.getClass().getName() + " Regression Min set to " + spinnerInteger_RegMin);
	}

	/** Executed whenever the {@link #spinnerInteger_RegMax} parameter changes. */
	protected void callbackRegMax() {
		if (spinnerInteger_RegMax <= spinnerInteger_RegMin + 2) {
			spinnerInteger_RegMax = spinnerInteger_RegMin + 2;
		}
		if (spinnerInteger_RegMax > spinnerInteger_KMax) {
			spinnerInteger_RegMax = spinnerInteger_KMax;
		}

		logService.info(this.getClass().getName() + " Regression Max set  to " + spinnerInteger_RegMax);
	}

	/** Executed whenever the {@link #choiceRadioButt_Method} parameter changes. */
	protected void callbackMethod() {
		//Check if Image type and selected Method fits together 	
		if (imageType.equals("Grey")) {// grey image
			if	(false){
				Result result = uiService.showDialog("An RGB algorithm cannot be applied to gray image volumes!", "Alert", MessageType.WARNING_MESSAGE, OptionType.DEFAULT_OPTION);
			}
		}	
		if (imageType.equals("RGB")) {// RGB image
			//******************************************************************************************************
			if  ( (choiceRadioButt_Method.equals("Multiplicated differences"))
				||(choiceRadioButt_Method.equals("Squared differences"))
				||(choiceRadioButt_Method.equals("Direct differences")) )
			{
				Result result = uiService.showDialog("A grey value algorithm cannot be applied to RGB image volumes!", "Alert", MessageType.WARNING_MESSAGE, OptionType.DEFAULT_OPTION);
			}
		}
		logService.info(this.getClass().getName() + " Method set to " + choiceRadioButt_Method);
	}
	
	
	/** Executed whenever the {@link #booleanSkipZeroes} parameter changes. */
	protected void callbackSkipZeroes() {
		logService.info(this.getClass().getName() + " Skip zeroes set to " + booleanSkipZeroes);
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
	
		dlgProgress = new WaitingDialogWithProgressBar("Computing Higuchi3D dimension, please wait... Open console window for further info.",
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
	
		rai =  (RandomAccessibleInterval<T>) datasetIn.getImgPlus(); //dim==2 or 3

		// Compute regression parameters
		double[] regressionValues = process(rai); //rai is 3D

		//set values for output table
		for (int i = 0; i < regressionValues.length; i++ ) {
			resultValuesTable[i] = regressionValues[i]; 
		}
		//Compute dimension
		double dim = 0.0;	
		dim = -regressionValues[1];
		if (choiceRadioButt_Method.equals("Multiplicated differences")) dim = dim + 2;
		if (choiceRadioButt_Method.equals("Squared differences"))  		dim = dim + 2;
		if (choiceRadioButt_Method.equals("Direct differences"));       //Do nothing, slope = dim	
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
		
		GenericColumn columnFileName     = new GenericColumn("File name");
		IntColumn columnKMax             = new IntColumn("k");
		IntColumn columnRegMin           = new IntColumn("RegMin");
		IntColumn columnRegMax           = new IntColumn("RegMax");
		GenericColumn columnMethod       = new GenericColumn("Method");
		BoolColumn columnSkipZeroes      = new BoolColumn("Skip zeroes");
		DoubleColumn columnDh            = new DoubleColumn("Dh");
		DoubleColumn columnR2            = new DoubleColumn("R2");
		DoubleColumn columnStdErr        = new DoubleColumn("StdErr");

		tableOut = new DefaultGenericTable();
		tableOut.add(columnFileName);
		tableOut.add(columnKMax);
		tableOut.add(columnRegMin);
		tableOut.add(columnRegMax);
		tableOut.add(columnMethod);
		tableOut.add(columnSkipZeroes);
		tableOut.add(columnDh);
		tableOut.add(columnR2);
		tableOut.add(columnStdErr);

	}

	/** 
	*  writes current result to table
	*  @param int slice number of active image.
	*/
	private void writeSingleResultToTable() { 

		int regMin = spinnerInteger_RegMin;
		int regMax = spinnerInteger_RegMax;
		int numKMax = spinnerInteger_KMax;

		// 0 Intercept, 1 Dim, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
		// fill table with values
		tableOut.appendRow();
		tableOut.set("File name",  tableOut.getRowCount() - 1, datasetName);	
		tableOut.set("k",           tableOut.getRowCount() - 1, numKMax);
		tableOut.set("RegMin",      tableOut.getRowCount() - 1, regMin);
		tableOut.set("RegMax",      tableOut.getRowCount() - 1, regMax);
		tableOut.set("Method",      tableOut.getRowCount() - 1, choiceRadioButt_Method);
		tableOut.set("Skip zeroes", tableOut.getRowCount() - 1, booleanSkipZeroes);
		tableOut.set("Dh",          tableOut.getRowCount() - 1, resultValuesTable[1]);
		tableOut.set("R2",          tableOut.getRowCount() - 1, resultValuesTable[4]);
		tableOut.set("StdErr",      tableOut.getRowCount() - 1, resultValuesTable[3]);
	}

	/**
	*
	* Processing
	*/
	private double[] process(RandomAccessibleInterval<?> rai) { //3Dvolume
	
		if (rai == null) {
			logService.info(this.getClass().getName() + " WARNING: rai==null, no image for processing!");
		}
		
		int regMin           = spinnerInteger_RegMin;
		int regMax           = spinnerInteger_RegMax;
		int numKMax          = spinnerInteger_KMax;
		boolean skipZeroes   = booleanSkipZeroes;
		boolean optShowPlot  = booleanShowDoubleLogPlot;

		long width  = rai.dimension(0);
		long height = rai.dimension(1);
		long depth  = rai.dimension(2);
		
		double[] regressionParams = null;
	
		String plot_method="Higuchi dimension 3D";
		String xAxis = "ln(eps)";
		String yAxis = "ln(Count)";
		
		Higuchi3DMethods hig3Method = null;
		
		if (imageType.equals("Grey")) {// grey image   //additional check, is already checked during validation of active dataset
			//*****************************************************************************************************************************************
			if (choiceRadioButt_Method.equals("Multiplicated differences")) {
				hig3Method = new Higuchi3D_Grey_MultDiff(rai, numKMax, skipZeroes);	
				plot_method="Higuchi 3D - Multiplicated differences";
				xAxis = "ln(k^3)";
				yAxis = "ln(V(k))";
			}
			//******************************************************************************************************************************************
			if (choiceRadioButt_Method.equals("Squared differences")) {
				hig3Method = new Higuchi3D_Grey_SqrDiff(rai, numKMax, skipZeroes);
				plot_method="Higuchi 3D - Squared differences";
				xAxis = "ln(k^2)";
				yAxis = "ln(A(k))";
			}
			//******************************************************************************************************************************************
			if (choiceRadioButt_Method.equals("Direct differences")) {
				hig3Method = new Higuchi3D_Grey_DirDiff(rai, numKMax, skipZeroes);
				plot_method="Higuchi 3D - Direct differences";
				xAxis = "ln(k)";
				yAxis = "ln(L(k))";	
			}
		
			
		} else if (imageType.equals("RGB")) { // RGB image  //additional check, is already checked during validation of active dataset
		
			//no method implemented

		}
		
		double[] eps    = hig3Method.calcEps();
		double[] totals = hig3Method.calcTotals();
		
		if (eps == null || totals == null) return null;
		
		double[] lnEps    = new double[numKMax];
		double[] lnTotals = new double[numKMax];
	
		logService.info(this.getClass().getName() + " Higuchi3D");
		logService.info(this.getClass().getName() + " lnEps: \t  lnTotals:");	
		for (int i = 0; i < eps.length; i++) {
			lnEps[i]    = Math.log(eps[i]);
			lnTotals[i] = Math.log(totals[i]);
			logService.info(this.getClass().getName() + (String.valueOf(lnEps[i]) + "\t "   + String.valueOf(lnTotals[i])));
		}
		
		
		//Create double log plot	
		boolean isLineVisible = false; //?		
		if (optShowPlot) {
			if ((imageType.equals("Grey")) || (imageType.equals("RGB"))) { //both are OK
				String preName = "Volume-";
				RegressionPlotFrame doubleLogPlot = DisplayRegressionPlotXY(lnEps, lnTotals, isLineVisible, plot_method, 
						preName + datasetName, xAxis, yAxis, "",
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
		// uiService.show("Table - Higuchi dimension", table);
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
					"Double Log Plot - Higuchi Dimension", preName + datasetName, "ln(k)", "ln(L)", "", regMin, regMax);
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
		// ij.command().run(Img3DFractalDimensionHiguchi3D.class,
		// true).get().getOutput("image");
		ij.command().run(Img3DFractalDimensionHiguchi3D.class, true);
	}
}
