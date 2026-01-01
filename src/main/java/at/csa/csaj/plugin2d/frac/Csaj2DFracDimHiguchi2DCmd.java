/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj2DFracDimHiguchi2DCmd.java
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


package at.csa.csaj.plugin2d.frac;

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
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
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

import at.csa.csaj.commons.CsajDialog_WaitingWithProgressBar;
import at.csa.csaj.commons.CsajPlot_RegressionFrame;
import at.csa.csaj.commons.CsajRegression_Linear;
import at.csa.csaj.commons.CsajCheck_ItemIn;
import at.csa.csaj.commons.CsajContainer_ProcessMethod;
import at.csa.csaj.plugin2d.frac.util.Higuchi2DMethods;
import at.csa.csaj.plugin2d.frac.util.Higuchi2D_Grey_DirDiff;
import at.csa.csaj.plugin2d.frac.util.Higuchi2D_Grey_KfoldDiff;
import at.csa.csaj.plugin2d.frac.util.Higuchi2D_Grey_MultDiff;
import at.csa.csaj.plugin2d.frac.util.Higuchi2D_Grey_MultDiff2;
import at.csa.csaj.plugin2d.frac.util.Higuchi2D_Grey_SqrDiff;
import at.csa.csaj.plugin2d.frac.util.Higuchi2D_Grey_TriangArea;
import at.csa.csaj.plugin2d.frac.util.Higuchi2D_RGBColWeight_NotReadyYet;
import at.csa.csaj.plugin2d.frac.util.Higuchi2D_RGBDiff_KfoldDiff;
import at.csa.csaj.plugin2d.frac.util.Higuchi2D_RGBDiff_MultDiff;
import at.csa.csaj.plugin2d.frac.util.Higuchi2D_RGBDiff_SqrDiff;
import at.csa.csaj.plugin2d.frac.util.Higuchi2D_RGBROIColWeight_NotReadyYet;
import at.csa.csaj.plugin2d.frac.util.Higuchi2D_RGB_KfoldDiff;
import at.csa.csaj.plugin2d.frac.util.Higuchi2D_RGB_MultDiff;
import at.csa.csaj.plugin2d.frac.util.Higuchi2D_RGB_SqrDiff;

/**
 * A {@link ContextCommand} plugin computing <the 2D Higuchi dimension</a>
 * of an image.
 */
@Plugin(type = ContextCommand.class,
		headless = true,
		label = "Higuchi dimension 2D",
		initializer = "initialPluginLaunch",
		iconPath = "/icons/comsystan-logo-grey46-16x16.png", //Menu entry icon
		menu = {})

public class Csaj2DFracDimHiguchi2DCmd<T extends RealType<T>> extends ContextCommand implements Previewable {

	private static final String PLUGIN_LABEL            = "Computes fractal dimension with with Higuchi 2D algorithms";
	private static final String SPACE_LABEL             = "";
	private static final String REGRESSION_LABEL        = "<html><b>Regression parameters</b></html>";
	private static final String METHODOPTIONS_LABEL     = "<html><b>Method</b></html>";
	private static final String BACKGROUNDOPTIONS_LABEL = "<html><b>Background option</b></html>";
	private static final String DISPLAYOPTIONS_LABEL    = "<html><b>Display options</b></html>";
	private static final String PROCESSOPTIONS_LABEL    = "<html><b>Process options</b></html>";
	//private static Img<FloatType> imgFloat;
	private static String datasetName;
	private static String[] sliceLabels;
	private static long width = 0;
	private static long height = 0;
	private static long numDimensions = 0;
	private static long numSlices = 0;
	private static long compositeChannelCount =0;
	private static String imageType = "";
	private static int  numKMax = 0;
	private static ArrayList<CsajPlot_RegressionFrame> doubleLogPlotList = new ArrayList<CsajPlot_RegressionFrame>();
	
	public static final String TABLE_OUT_NAME = "Table - Higuchi2D dimension";
	
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

	@Parameter(label = "k", description = "Maximal delay between data points", style = NumberWidget.SPINNER_STYLE, min = "3", max = "32768", stepSize = "1",
			   persist = false, // restore  previous value  default  =  true
			   initializer = "initialKMax", callback = "callbackKMax")
	private int spinnerInteger_KMax;

	@Parameter(label = "Regression Start", description = "Minimum x value of linear regression", style = NumberWidget.SPINNER_STYLE, min = "1", max = "32768", stepSize = "1",
			   persist = false, //restore previous value default = true
			   initializer = "initialNumRegStart", callback = "callbackNumRegStart")
	private int spinnerInteger_NumRegStart = 1;

	@Parameter(label = "Regression End", description = "Maximum x value of linear regression", style = NumberWidget.SPINNER_STYLE, min = "3", max = "32768", stepSize = "1",
			   persist = false, //restore previous value default = true
			   initializer = "initialNumRegEnd", callback = "callbackNumRegEnd")
	private int spinnerInteger_NumRegEnd = 3;

	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelInterpolation = METHODOPTIONS_LABEL;

	@Parameter(label = "Method", description = "Type of Higuchi 2D algorithm", style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE, choices = {
			   "K-fold differences",
			   "Multiplicated differences",
			   "Multiplicated differences2",														   
			   "Squared differences",
			   "Direct differences",
			   "Triangle areas",
			   "RGB Diff - K-fold differences",
			   "RGB Diff - Multiplicated differences",	  
			   "RGB Diff - Squared differences",	
			   "RGB K-fold differences",	  
			   "RGB Multiplicated differences",	  
			   "RGB Squared differences",        //   "RGB - color weighted (alpha)",  "RGB - ROI color weighted (alpha)"},
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
//	@Parameter(label = "Preview of single active image ", callback = "callbackProcessActiveImage")
//	private Button buttonProcessActiveImage;

	@Parameter(label = "Preview of all available images", callback = "callbackProcessAllImages")
	private Button buttonProcessAllImages;
	
	// ---------------------------------------------------------------------
	protected void initialPluginLaunch() {
		checkItemIOIn();
	}
	
	protected void initialKMax() {
		if (datasetIn == null) {
    		logService.error(this.getClass().getName() + " ERROR: Input image = null");
    		cancel("ComsystanJ 2D plugin cannot be started - missing input image.");
    		return;
    	} else {
    		numKMax = getMaxK((int)datasetIn.dimension(0), (int)datasetIn.dimension(1));
    	}
		spinnerInteger_KMax = numKMax;
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
    		numKMax = getMaxK((int)datasetIn.dimension(0), (int)datasetIn.dimension(1));
    	}
		spinnerInteger_NumRegEnd = numKMax;
	}

	protected void initialMethod() {
		choiceRadioButt_Method = "K-fold differences";
		//Check if Image type and selected Method fits together 	
		if (imageType.equals("Grey")) {// grey image
			if	( (choiceRadioButt_Method.equals("RGB Diff - K-fold differences"))
				||(choiceRadioButt_Method.equals("RGB Diff - Multiplicated differences"))
				||(choiceRadioButt_Method.equals("RGB Diff - Squared differences"))
				||(choiceRadioButt_Method.equals("RGB K-fold differences"))
				||(choiceRadioButt_Method.equals("RGB Multiplicated differences"))
				||(choiceRadioButt_Method.equals("RGB Squared differences"))
				||(choiceRadioButt_Method.equals("RGB - color weighted (alpha)"))
				||(choiceRadioButt_Method.equals("RGB - ROI color weighted (alpha)")) )
			{
				Result result = uiService.showDialog("An RGB algorithm cannot be applied to gray images!", "Alert", MessageType.WARNING_MESSAGE, OptionType.DEFAULT_OPTION);
			}
		}	
		if (imageType.equals("RGB")) {// RGB image
			//******************************************************************************************************
			if  ( (choiceRadioButt_Method.equals("K-fold differences"))
				||(choiceRadioButt_Method.equals("Multiplicated differences"))
				||(choiceRadioButt_Method.equals("Multiplicated differences2"))
				||(choiceRadioButt_Method.equals("Squared differences"))
				||(choiceRadioButt_Method.equals("Direct differences"))
				||(choiceRadioButt_Method.equals("Triangle areas")) )
			{
				Result result = uiService.showDialog("A grey value algorithm cannot be applied to RGB images!", "Alert", MessageType.WARNING_MESSAGE, OptionType.DEFAULT_OPTION);
			}
		}
	}

	protected void initialShowDoubleLogPlots() {
		booleanShowDoubleLogPlot = true;
	}
	
	protected void initialOverwriteDisplays() {
    	booleanOverwriteDisplays = true;
	}
	
	protected void initialNumImageSlice() {
    	spinnerInteger_NumImageSlice = 1;
	}
	
	// ------------------------------------------------------------------------------
	

	/** Executed whenever the {@link #spinnerInteger_KMax} parameter changes. */
	protected void callbackKMax() {

		if (spinnerInteger_KMax < 3) {
			spinnerInteger_KMax = 3;
		}
		if (spinnerInteger_KMax > numKMax) {
			spinnerInteger_KMax = numKMax;
		}
		if (spinnerInteger_NumRegEnd > spinnerInteger_KMax) {
			spinnerInteger_NumRegEnd = spinnerInteger_KMax;
		}
		if (spinnerInteger_NumRegStart >= spinnerInteger_NumRegEnd - 2) {
			spinnerInteger_NumRegStart = spinnerInteger_NumRegEnd - 2;
		}
		logService.info(this.getClass().getName() + " k set to " + spinnerInteger_KMax);
	}

	/** Executed whenever the {@link #spinnerInteger_NumRegStart} parameter changes. */
	protected void callbackNumRegStart() {
		if (spinnerInteger_NumRegStart >= spinnerInteger_NumRegEnd - 2) {
			spinnerInteger_NumRegStart = spinnerInteger_NumRegEnd - 2;
		}
		if (spinnerInteger_NumRegStart < 1) {
			spinnerInteger_NumRegStart = 1;
		}
		logService.info(this.getClass().getName() + " Regression Min set to " + spinnerInteger_NumRegStart);
	}

	/** Executed whenever the {@link #spinnerInteger_NumRegEnd} parameter changes. */
	protected void callbackNumRegEnd() {
		if (spinnerInteger_NumRegEnd <= spinnerInteger_NumRegStart + 2) {
			spinnerInteger_NumRegEnd = spinnerInteger_NumRegStart + 2;
		}
		if (spinnerInteger_NumRegEnd > spinnerInteger_KMax) {
			spinnerInteger_NumRegEnd = spinnerInteger_KMax;
		}

		logService.info(this.getClass().getName() + " Regression Max set  to " + spinnerInteger_NumRegEnd);
	}

	/** Executed whenever the {@link #choiceRadioButt_Method} parameter changes. */
	protected void callbackMethod() {
		//Check if Image type and selected Method fits together 	
		if (imageType.equals("Grey")) {// grey image
			if	( (choiceRadioButt_Method.equals("RGB Diff - K-fold differences"))
				||(choiceRadioButt_Method.equals("RGB Diff - Multiplicated differences"))
				||(choiceRadioButt_Method.equals("RGB Diff - Squared differences"))
				||(choiceRadioButt_Method.equals("RGB K-fold differences"))
				||(choiceRadioButt_Method.equals("RGB Multiplicated differences"))
				||(choiceRadioButt_Method.equals("RGB Squared differences"))
				||(choiceRadioButt_Method.equals("RGB - color weighted (alpha)"))
				||(choiceRadioButt_Method.equals("RGB - ROI color weighted (alpha)")) )
			{
				Result result = uiService.showDialog("An RGB algorithm cannot be applied to gray images!", "Alert", MessageType.WARNING_MESSAGE, OptionType.DEFAULT_OPTION);
			}
		}	
		if (imageType.equals("RGB")) {// RGB image
			//******************************************************************************************************
			if  ( (choiceRadioButt_Method.equals("K-fold differences"))
				||(choiceRadioButt_Method.equals("Multiplicated differences"))
				||(choiceRadioButt_Method.equals("Multiplicated differences2"))
				||(choiceRadioButt_Method.equals("Squared differences"))
				||(choiceRadioButt_Method.equals("Direct differences"))
				||(choiceRadioButt_Method.equals("Triangle areas")) )
			{
				Result result = uiService.showDialog("A grey value algorithm cannot be applied to RGB images!", "Alert", MessageType.WARNING_MESSAGE, OptionType.DEFAULT_OPTION);
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
	        	startWorkflowForAllImages();
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
		    	    startWorkflowForSingleImage();
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
			
			//Grey and RGB images are supported
		}
	}

	/**
	* This method starts the workflow for a single image of the active display
	*/
	protected void startWorkflowForSingleImage() {
	
		dlgProgress = new CsajDialog_WaitingWithProgressBar("Computing Higuchi2D dimensions, please wait... Open console window for further info.",
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
		
		dlgProgress = new CsajDialog_WaitingWithProgressBar("Computing Higuchi2D dimensions, please wait... Open console window for further info.",
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
	
	public static int getMaxK(int width, int height) {
		numKMax = (int) Math.floor((Math.min(width, height)) / 3.0);
		return numKMax;
	}

	/** This method takes the active image and computes results. 
	 *
	 **/
	private void processSingleInputImage (int s) {
		
		long startTime = System.currentTimeMillis();
	
		// convert to float values
		// Img<T> image = (Img<T>) dataset.getImgPlus();
		// mg<FloatType> imgFloat; // =
		// opService.convert().float32((Img<T>)dataset.getImgPlus());

		// get slice and convert to float values
		// imgFloat = opService.convert().float32((Img<T>)dataset.getImgPlus());

//		imgFloat = new ArrayImgFactory<>(new FloatType()).create(width, height); // always single 2D
//		Cursor<FloatType> cursor = imgFloat.localizingCursor();
//		final long[] pos = new long[imgFloat.numDimensions()];
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
//		IntervalView<FloatType> iv = Views.interval(imgFloat, new long[] { 0, 0 },
//				new long[] { imgFloat.max(0), imgFloat.max(1) });
//
//		// Compute regression parameters
//		containerPM = process(iv, s);
//		// 0 Dh-row, 1 R2-row, 2 StdErr-row, 3 Dh-col, 4 R2-col, 5 StdErr-col, 6 Dh, 7 R2, 8 Stderr
		
		//get rai
		RandomAccessibleInterval<T> rai = null;	
		if( (s==0) && (numSlices == 1)) { // for only one 2D image, Grey or RGB;
			rai =  (RandomAccessibleInterval<T>) datasetIn.getImgPlus(); //dim==2 or 3

		} else if ( (numSlices > 1)){ // for a stack of 2D images, Grey or RGB
			if (imageType.equals("Grey")) {
				rai = (RandomAccessibleInterval<T>) Views.hyperSlice(datasetIn, 2, s);  //dim==2  x,y,z	
			} else if (imageType.equals("RGB")) {
				rai = (RandomAccessibleInterval<T>) Views.hyperSlice(datasetIn, 3, s);  //dim==3  x,y,composite,z  
			}	
		}

		// Compute regression parameters
		CsajContainer_ProcessMethod containerPM = process(rai, s); //rai is already 2D, s parameter only for display titles
		//0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
		
		writeToTable(0, s, containerPM);
	
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
		
		// convert to float values
		// Img<T> image = (Img<T>) dataset.getImgPlus();
		// Img<FloatType> imgFloat; // =
		// opService.convert().float32((Img<T>)dataset.getImgPlus());

		CsajContainer_ProcessMethod containerPM;
		// loop over all slices of stack
		for (int s = 0; s < numSlices; s++) { // p...planes of an image stack
			//if (!exec.isShutdown()) {
				int percent = (int)Math.round((  ((float)s)/((float)numSlices) * 100.f   ));
				dlgProgress.updatePercent(String.valueOf(percent+"%"));
				dlgProgress.updateBar(percent);
				//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
				statusService.showStatus((s+1), (int)numSlices, "Processing " + (s+1) + "/" + (int)numSlices);
				statusService.showProgress(percent, 100);
	//			try {
	//				Thread.sleep(3000);
	//			} catch (InterruptedException e) {
	//				// TODO Auto-generated catch block
	//				e.printStackTrace();
	//			}
				long startTime = System.currentTimeMillis();
				logService.info(this.getClass().getName() + " Processing image number " + (s+1) + "(" + numSlices + ")");
				// get slice and convert to float values
				// imgFloat = opService.convert().float32((Img<T>)dataset.gett);
	
	//			imgFloat = new ArrayImgFactory<>(new FloatType()).create(width, height); // always single 2D
	//			Cursor<FloatType> cursor = imgFloat.localizingCursor();
	//			final long[] pos = new long[imgFloat.numDimensions()];
	//			RandomAccess<RealType<?>> ra = datasetIn.randomAccess();
	//			while (cursor.hasNext()) {
	//				cursor.fwd();
	//				cursor.localize(pos);
	//				if (numSlices == 1) { // for only one 2D image;
	//					ra.setPosition(pos[0], 0);
	//					ra.setPosition(pos[1], 1);
	//				} else { // for more than one image e.g. image stack
	//					ra.setPosition(pos[0], 0);
	//					ra.setPosition(pos[1], 1);
	//					ra.setPosition(s, 2);
	//				}
	//				// ra.get().setReal(cursor.get().get());
	//				cursor.get().setReal(ra.get().getRealFloat());
	//			}
	//
	//			IntervalView<FloatType> iv = Views.interval(imgFloat, new long[] { 0, 0 },
	//					new long[] { imgFloat.max(0), imgFloat.max(1) });
	//
	//			// Compute regression parameters
	//			containerPM = process(iv, s);
	//			// 0 Dh, 1 R2, 2 Stderr
				
				
				//get rai
				RandomAccessibleInterval<T> rai = null;	
				if( (s==0) && (numSlices == 1) ) { // for only one 2D image;
					rai =  (RandomAccessibleInterval<T>) datasetIn.getImgPlus(); //dim==2 or 3
	
				}
				if (numSlices > 1) { // for a stack
					if (imageType.equals("Grey")) {
						rai = (RandomAccessibleInterval<T>) Views.hyperSlice(datasetIn, 2, s); //dim==2  x,y,z  
					} else if (imageType.equals("RGB")) {
						rai = (RandomAccessibleInterval<T>) Views.hyperSlice(datasetIn, 3, s); //dim==3  x,y,composite,z  
					}	
				}
	
				// Compute regression parameters
				containerPM = process(rai, s); //rai is already 2D, s parameter only for display titles
				//0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
	
				writeToTable(s, s, containerPM);
				
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
	private void generateTableHeader() {
		
		GenericColumn columnFileName     = new GenericColumn("File name");
		GenericColumn columnSliceName    = new GenericColumn("Slice name");
		IntColumn columnKMax             = new IntColumn("k");
		GenericColumn columnNumRegStart  = new GenericColumn("Reg Start");
		GenericColumn columnNumRegEnd    = new GenericColumn("Reg End");
		GenericColumn columnMethod       = new GenericColumn("Method");
		BoolColumn columnSkipZeroes      = new BoolColumn("Skip zeroes");
		DoubleColumn columnDh            = new DoubleColumn("Dh");
		DoubleColumn columnR2            = new DoubleColumn("R2");
		DoubleColumn columnStdErr        = new DoubleColumn("StdErr");

		tableOut = new DefaultGenericTable();
		tableOut.add(columnFileName);
		tableOut.add(columnSliceName);
		tableOut.add(columnKMax);
		tableOut.add(columnNumRegStart);
		tableOut.add(columnNumRegEnd);
		tableOut.add(columnMethod);
		tableOut.add(columnSkipZeroes);
		tableOut.add(columnDh);
		tableOut.add(columnR2);
		tableOut.add(columnStdErr);

	}

	/**
	 * collects current result and writes to table
	 * 
	 * @param int numRow to write in the result table
	 * @param int numSlice sclice number of images from datasetIn.
	 * @param CsajContainer_ProcessMethod containerPM
	 */
	private void writeToTable(int numRow, int numSlice, CsajContainer_ProcessMethod containerPM) { 

		int numRegStart = spinnerInteger_NumRegStart;
		int numRegEnd   = spinnerInteger_NumRegEnd;
		int numKMax     = spinnerInteger_KMax;

		int row = numRow;
		int s = numSlice;
		// 0 Intercept, 1 Dim, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
		// fill table with values
		tableOut.appendRow();
		tableOut.set("File name",  row, datasetName);	
		if (sliceLabels != null) tableOut.set("Slice name", row, sliceLabels[s]);
		tableOut.set("k",           row, numKMax);
		tableOut.set("Reg Start",   row, "("+numRegStart+")" + containerPM.item2_Values[0]); //(NumRegStart)epsRegStart
		tableOut.set("Reg End",     row, "("+numRegEnd+")"   + containerPM.item2_Values[1]); //(NumRegEnd)epsRegEnd
		tableOut.set("Method",      row, choiceRadioButt_Method);
		tableOut.set("Skip zeroes", row, booleanSkipZeroes);
		tableOut.set("Dh",          row, containerPM.item1_Values[1]);
		tableOut.set("R2",          row, containerPM.item1_Values[4]);
		tableOut.set("StdErr",      row, containerPM.item1_Values[3]);
	}


	/**
	*
	* Processing
	*/
	private CsajContainer_ProcessMethod process(RandomAccessibleInterval<?> rai, int plane) { // plane plane (Image) number
	
		if (rai == null) {
			logService.info(this.getClass().getName() + " WARNING: rai==null, no image for processing!");
		}
		
		int numRegStart      = spinnerInteger_NumRegStart;
		int numRegEnd        = spinnerInteger_NumRegEnd;
		int numKMax          = spinnerInteger_KMax;
		boolean skipZeroes   = booleanSkipZeroes;
		boolean optShowPlot  = booleanShowDoubleLogPlot;

		long width  = rai.dimension(0);
		long height = rai.dimension(1);

		double[] epsRegStartEnd   = new double[2];  // epsRegStart, epsRegEnd
		double[] regressionParams = null;
		double[] resultValues = null;
	
		String plot_method="Higuchi dimension 2D";
		String xAxis = "ln(eps)";
		String yAxis = "ln(Count)";
		
		Higuchi2DMethods hig2Method = null;
		
		if (imageType.equals("Grey")) {// grey image   //additional check, is already checked during validation of active dataset
			//******************************************************************************************************
			if (choiceRadioButt_Method.equals("K-fold differences")) {
				hig2Method = new Higuchi2D_Grey_KfoldDiff(rai, numKMax, skipZeroes);		
				plot_method="Higuchi 2D - k-fold differences";
				xAxis = "ln(k)";
				yAxis = "ln(A(k))";
			}
			//*****************************************************************************************************************************************
			if (choiceRadioButt_Method.equals("Multiplicated differences")) {
				hig2Method = new Higuchi2D_Grey_MultDiff(rai, numKMax, skipZeroes);	
				plot_method="Higuchi 2D - Multiplicated differences";
				xAxis = "ln(k^2)";
				yAxis = "ln(A(k))";
			}
			//******************************************************************************************************************************************
			if (choiceRadioButt_Method.equals("Multiplicated differences2")) {
				hig2Method = new Higuchi2D_Grey_MultDiff2(rai, numKMax, skipZeroes);	
				plot_method="Higuchi 2D - Multiplicated differences2";
				xAxis = "ln(k^2)";
				yAxis = "ln(A(k))";
			}
			//******************************************************************************************************************************************
			if (choiceRadioButt_Method.equals("Squared differences")) {
				hig2Method = new Higuchi2D_Grey_SqrDiff(rai, numKMax, skipZeroes);
				plot_method="Higuchi 2D - Squared differences";
				xAxis = "ln(k^2)";
				yAxis = "ln(A(k))";
			}
			//******************************************************************************************************************************************
			if (choiceRadioButt_Method.equals("Direct differences")) {
				hig2Method = new Higuchi2D_Grey_DirDiff(rai, numKMax, skipZeroes);
				plot_method="Higuchi 2D - Direct differences";
				xAxis = "ln(k)";
				yAxis = "ln(L(k))";	
			}
			//******************************************************************************************************************************************
			if (choiceRadioButt_Method.equals("Triangle areas")) {
				hig2Method = new Higuchi2D_Grey_TriangArea(rai, numKMax, skipZeroes);
				plot_method="Higuchi 2D - Triangle Areas";
				xAxis = "ln(k)";
				yAxis = "ln(A(k))";
			}
			
		} else if (imageType.equals("RGB")) { // RGB image  //additional check, is already checked during validation of active dataset
			if (choiceRadioButt_Method.equals("RGB Diff - K-fold differences")) {
				hig2Method = new Higuchi2D_RGBDiff_KfoldDiff(rai, numKMax, skipZeroes);
				plot_method="Higuchi 2D - RGBDiff Kfold differences";
				xAxis = "ln(k)";
				yAxis = "ln(A(k))";
			}
			if (choiceRadioButt_Method.equals("RGB Diff - Multiplicated differences")) {
				hig2Method = new Higuchi2D_RGBDiff_MultDiff(rai, numKMax, skipZeroes);
				plot_method="Higuchi 2D - RGBDiff Multiplicated differences";
				xAxis = "ln(k^2)";
				yAxis = "ln(A(k))";
			}
			if (choiceRadioButt_Method.equals("RGB Diff - Squared differences")) {
				hig2Method = new Higuchi2D_RGBDiff_SqrDiff(rai, numKMax, skipZeroes);
				plot_method="Higuchi 2D - RGBDiff Squared differences";
				xAxis = "ln(k^2)";
				yAxis = "ln(A(k))";
			}
			if (choiceRadioButt_Method.equals("RGB K-fold differences")) {
				hig2Method = new Higuchi2D_RGB_KfoldDiff(rai, numKMax, skipZeroes);
				plot_method="Higuchi 2D - RGB K-fold differences";
				xAxis = "ln(k^2)";
				yAxis = "ln(A(k))";
			}
			if (choiceRadioButt_Method.equals("RGB Multiplicated differences")) {
				hig2Method = new Higuchi2D_RGB_MultDiff(rai, numKMax, skipZeroes);
				plot_method="Higuchi 2D - RGB Multiplicated differences";
				xAxis = "ln(k^2)";
				yAxis = "ln(A(k))";
			}
			if (choiceRadioButt_Method.equals("RGB Squared differences")) {
				hig2Method = new Higuchi2D_RGB_SqrDiff(rai, numKMax, skipZeroes);
				plot_method="Higuchi 2D - RGB Squared differences";
				xAxis = "ln(k^2)";
				yAxis = "ln(A(k))";
			}
			if (choiceRadioButt_Method.equals("RGB - color weighted (alpha)")) {
				hig2Method = new Higuchi2D_RGBColWeight_NotReadyYet(rai, numKMax, skipZeroes);
				plot_method="Higuchi 2D - RGB color weighted"; 
				xAxis = "ln(k)";
				yAxis = "ln(A(k))";
			}
			if (choiceRadioButt_Method.equals("RGB - ROI color weighted (alpha)")) {
				hig2Method = new Higuchi2D_RGBROIColWeight_NotReadyYet(rai, numKMax, skipZeroes);
				plot_method="Higuchi 2D - RGB ROI color weighted";
				xAxis = "ln(k)";
				yAxis = "ln(A(k))";
			}	

		}
		
		double[] eps    = hig2Method.calcEps();
		double[] totals = hig2Method.calcTotals();
		
		if (eps == null || totals == null) return null;
		
		double[] lnEps    = new double[numKMax];
		double[] lnTotals = new double[numKMax];
	
		//logService.info(this.getClass().getName() + " Higuchi2D");
		//logService.info(this.getClass().getName() + " lnEps: \t  lnTotals:");	
		for (int i = 0; i < eps.length; i++) {
			lnEps[i]    = Math.log(eps[i]);
			lnTotals[i] = Math.log(totals[i]);
			//logService.info(this.getClass().getName() + (String.valueOf(lnEps[i]) + "\t "   + String.valueOf(lnTotals[i])));
		}
		
		
		//Create double log plot
		boolean isLineVisible = false; //?		
		if (optShowPlot) {
			if ((imageType.equals("Grey")) || (imageType.equals("RGB"))) { //both are OK
				String preName = "";
				if (numSlices > 1) {
					preName = "Slice-"+String.format("%03d", plane) +"-";
				}
				CsajPlot_RegressionFrame doubleLogPlot = DisplayRegressionPlotXY(lnEps, lnTotals, isLineVisible, plot_method, 
						preName + datasetName, xAxis, yAxis, "",
						numRegStart, numRegEnd);
				doubleLogPlotList.add(doubleLogPlot);
			}
			else {
		
			}
		}
		
		// Compute regression
		CsajRegression_Linear lr = new CsajRegression_Linear();

//		double[] dataXArray = new double[dataX.size()];
//		double[] dataYArray = new double[dataY.size()];
//		for (int i = 0; i < dataX.size(); i++) {
//			dataXArray[i] = dataX.get(i).doubleValue();
//		}
//		for (int i = 0; i < dataY.size(); i++) {
//			dataYArray[i] = dataY.get(i).doubleValue();
//		}

		regressionParams = lr.calculateParameters(lnEps, lnTotals, numRegStart, numRegEnd);
		//0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
			
		//Compute result values
		resultValues = regressionParams;
		double dim = 0.0;	
		dim = -regressionParams[1];
		if (choiceRadioButt_Method.equals("K-fold differences")); //do nothing
		if (choiceRadioButt_Method.equals("Multiplicated differences"))  			dim = dim + 1;
		if (choiceRadioButt_Method.equals("Multiplicated differences2"))  			dim = dim + 1;
		if (choiceRadioButt_Method.equals("Squared differences"))  					dim = dim + 1;
		if (choiceRadioButt_Method.equals("Direct differences"));
		if (choiceRadioButt_Method.equals("Triangle areas"));
		if (choiceRadioButt_Method.equals("RGB Diff - K-fold differences"));
		if (choiceRadioButt_Method.equals("RGB Diff - Multiplicated differences"))  dim = dim + 1;
		if (choiceRadioButt_Method.equals("RGB Diff - Squared differences"))  		dim = dim + 1;
		if (choiceRadioButt_Method.equals("RGB K-fold differences"));
		if (choiceRadioButt_Method.equals("RGB Multiplicated differences"))  		dim = dim + 1;
		if (choiceRadioButt_Method.equals("RGB Squared differences"))  				dim = dim + 1;
		if (choiceRadioButt_Method.equals("RGB - color weighted (alpha)"));
		if (choiceRadioButt_Method.equals("RGB - ROI color weighted (alpha)"));
		resultValues[1] = dim;
		logService.info(this.getClass().getName() + " Higuchi2D dimension: " + dim);
		
		epsRegStartEnd[0] = eps[numRegStart-1];
		epsRegStartEnd[1] = eps[numRegEnd-1];
		
		return new CsajContainer_ProcessMethod(resultValues, epsRegStartEnd);
		//Output
		//uiService.show("Table - Higuchi dimension", table);
		////result = ops.create().img(image, new FloatType()); may not work in older Fiji versions
		//result = new ArrayImgFactory<>(new FloatType()).create(image.dimension(0), image.dimension(1)); 
		//table
	}

	// This method shows the double log plot
	private void showPlot(double[] lnDataX, double[] lnDataY, String preName, int plane, int numRegStart, int numRegEnd) {
		if (imageType.equals("Grey")) {
			if (lnDataX == null) {
				logService.info(this.getClass().getName() + " lnDataX == null, cannot display the plot!");
				return;
			}
			if (lnDataY == null) {
				logService.info(this.getClass().getName() + " lnDataY == null, cannot display the plot!");
				return;
			}
			if (plane< 0) {
				logService.info(this.getClass().getName() + " plane < 0, cannot display the plot!");
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
				preName = "Slice" + String.format("%03d", plane) + "-";
			} else {
				preName = preName + String.format("%03d", plane) + "-";
			}
			boolean isLineVisible = false; // ?
			CsajPlot_RegressionFrame doubleLogPlot = DisplayRegressionPlotXY(lnDataX, lnDataY, isLineVisible,
					"Double log plot - Higuchi2D dimension", preName + datasetName, "ln(k)", "ln(L)", "", numRegStart, numRegEnd);
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
