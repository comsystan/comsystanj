/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Img2DFractalDimensionHiguchi1D_BresenhamLineExtraction.java
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

package at.csa.csaj.plugin2d.frac.dim.hig1d.BresenhamVersion;

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
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.region.BresenhamLine;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
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
import at.csa.csaj.plugin2d.frac.dim.hig1d.util.Higuchi;
import at.csa.csaj.commons.plot.RegressionPlotFrame;
import at.csa.csaj.commons.Dialog_WaitingWithProgressBar;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import io.scif.DefaultImageMetadata;
import io.scif.MetaTable;

/**
 * A {@link InteractiveCommand} plugin computing <the Higuchi dimension by 1D sequences</a>
 * of an image.
 */
@Plugin(type = InteractiveCommand.class,
headless = true,
label = "Higuchi dimension 1D",
initializer = "initialPluginLaunch",
menu = {
@Menu(label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT, mnemonic = MenuConstants.PLUGINS_MNEMONIC),
@Menu(label = "ComsystanJ"),
@Menu(label = "2D Image(s)"),
@Menu(label = "Higuchi dimension 1D", weight = 9)})
/**
 * Csaj Interactive: InteractiveCommand (nonmodal GUI without OK and cancel button, NOT for Scripting!)
 * Csaj Macros:      ContextCommand     (modal GUI with OK and Cancel buttons, for scripting)
 * Developer note:
 * Develop the InteractiveCommand plugin Csaj***.java
 * Hard copy it and rename it to         Csaj***Command.java
 * Eliminate complete menu entry
 * Change 4x (incl. import) to ContextCommand instead of InteractiveCommand
 */
public class Img2DFractalDimensionHiguchi1D_BresenhamLineExtraction<T extends RealType<T>> extends InteractiveCommand implements Previewable, {

	private static final String PLUGIN_LABEL            = "<html><b>Computes fractal dimension with the Higuchi 1D algorithm</b></html>";
	private static final String SPACE_LABEL             = "";
	private static final String REGRESSION_LABEL        = "<html><b>Regression parameters</b></html>";
	private static final String METHODOPTIONS_LABEL     = "<html><b>1D profile extraction</b></html>";
	private static final String BACKGROUNDOPTIONS_LABEL = "<html><b>Background option</b></html>";
	private static final String DISPLAYOPTIONS_LABEL    = "<html><b>Display options</b></html>";
	private static final String PROCESSOPTIONS_LABEL    = "<html><b>Process options</b></html>";

	//private static Img<FloatType> imgFloat;
	private static double[] sequence1D;
	private static double[] xAxis1D;
	BresenhamLine lineBresenham;
	ArrayList<long[]> coords;
	LinearInterpolator interpolator;
	PolynomialSplineFunction psf;
	private static Plot plotProfile;
	private static String datasetName;
	private static String[] sliceLabels;
	private static boolean isGrey = true;
	private static long width = 0;
	private static long height = 0;
	private static long numDimensions = 0;
	private static long numSlices = 0;
	private static long compositeChannelCount =0;
	private static String imageType = "";
	private static int numbKMax = 0;
	private static double[] anglesRad;
	private static ArrayList<RegressionPlotFrame> doubleLogPlotList = new ArrayList<RegressionPlotFrame>();
	private static ArrayList<PlotWindow>          plotWindowList    = new ArrayList<PlotWindow>(); //ImageJ plot windows

	
	private static final String tableOutName = "Table - Higuchi dimension";
	
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
	//@Parameter(label = " ",visibility = ItemVisibility.MESSAGE, persist = false)
	//private final String labelPlugin = PLUGIN_LABEL;

	//@Parameter(label = " ",visibility = ItemVisibility.MESSAGE, persist = false)
	//private final String labelSpace = SPACE_LABEL;
	
	// Input dataset which is updated in callback functions
	@Parameter(type = ItemIO.INPUT)
	private Dataset datasetIn;

	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelRegression = REGRESSION_LABEL;

	@Parameter(label = "k",
			   description = "maximal delay between data points",
			   style = NumberWidget.SPINNER_STYLE,
			   min = "3",
			   max = "32768",
			   stepSize = "1",
			   persist = false, // restore  previous value  default  =  true
			   initializer = "initialKMax",
			   callback = "callbackKMax")
	private int spinnerInteger_KMax;

	@Parameter(label = "Regression Start",
			   description = "minimum x value of linear regression",
			   style = NumberWidget.SPINNER_STYLE,
			   min = "1",
			   max = "32768",
			   stepSize = "1",
			   persist = false, //restore previous value default = true
			   initializer = "initialNumRegStart", callback = "callbackNumRegStart")
	private int spinnerInteger_NumRegStart = 1;

	@Parameter(label = "Regression End",
			   description = "maximum x value of linear regression",
			   style = NumberWidget.SPINNER_STYLE,
			   min = "3",
			   max = "32768",
			   stepSize = "1",
			   persist = false, //restore previous value default = true
			   initializer = "initialNumRegEnd", callback = "callbackNumRegEnd")
	private int spinnerInteger_NumRegEnd = 3;

	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelInterpolation = METHODOPTIONS_LABEL;

	@Parameter(label = "Method",
			   description = "type of 1D sequence gathering",
			   style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
			   choices = {"Single centered row/column", "Single meander row/column", "Mean of all rows/columns", "Mean of      4 radial lines [0-pi]", "Mean of 180 radial lines [0-pi]" },
			   persist = true, //restore previous value default = true
			   initializer = "initialMethod",
			   callback = "callbackMethod")
	private String choiceRadioButt_Method;

	
	@Parameter(label = "Remove zero values",
			   persist = true,
		       callback = "callbackRemoveZeroes")
	private boolean booleanRemoveZeroes;
	
	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelDisplayOptions = DISPLAYOPTIONS_LABEL;

	@Parameter(label = "Show double log plot",
		   	   persist = true, //restore previous value default = true
			   initializer = "initialShowDoubleLogPlots")
	private boolean booleanShowDoubleLogPlot;
	
	@Parameter(label = "Show some radial line plots",
		   	   persist = true, //restore previous value default = true
			   initializer = "initialShowSomeRadialLinePlots")
	private boolean booleanShowSomeRadialLinePlots;

	@Parameter(label = "Get Dh value of each radial line",
			   persist = true, //restore previous value default = true
			   initializer = "initialGetRadialDhValues")
	private boolean booleanGetRadialDhValues;
	
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


	// ---------------------------------------------------------------------
		
	protected void initialPluginLaunch() {
		//datasetIn = imageDisplayService.getActiveDataset();
		checkItemIOIn();
	}
	protected void initialKMax() {
		numbKMax = (int) Math.floor((Math.min(datasetIn.dimension(0), datasetIn.dimension(1))) / 3.0);
		spinnerInteger_KMax = numbKMax;
	}

	protected void initialNumRegStart() {
		spinnerInteger_NumRegStart = 1;
	}

	protected void initialNumRegEnd() {
		spinnerInteger_NumRegEnd = (int) Math.floor((Math.min(datasetIn.dimension(0), datasetIn.dimension(1))) / 3.0);
	}

	protected void initialMethod() {
		choiceRadioButt_Method = "Single centered row/column";
	}

	protected void initialShowDoubleLogPlots() {
		booleanShowDoubleLogPlot = true;
	}

	protected void initialShowSomeRadialLinePlotss() {
		booleanShowSomeRadialLinePlots = false;
	}

	protected void initialGetRadialDhValues() {
		booleanGetRadialDhValues = false;
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
		if (spinnerInteger_KMax > numbKMax) {
			spinnerInteger_KMax = numbKMax;
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
		logService.info(this.getClass().getName() + " Method set to " + choiceRadioButt_Method);
	}
	
	
	/** Executed whenever the {@link #booleanRemoveZeroes} parameter changes. */
	protected void callbackRemoveZeroes() {
		logService.info(this.getClass().getName() + " Remove zeroes set to " + booleanRemoveZeroes);
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
	}
	
	/**
	* This method starts the workflow for a single image of the active display
	*/
	protected void startWorkflowForSingleImage() {
	
		dlgProgress = new Dialog_WaitingWithProgressBar("Computing Higuchi1D dimensions, please wait... Open console window for further info.",
					logService, false, exec); //isCanceable = false, because no following method listens to exec.shutdown 
		dlgProgress.updatePercent("");
		dlgProgress.setBarIndeterminate(true);
		dlgProgress.setVisible(true);
	
		deleteExistingDisplays();
		int sliceIndex = spinnerInteger_NumImageSlice - 1;
        logService.info(this.getClass().getName() + " Processing single image " + (sliceIndex + 1));
		processSingleInputImage(sliceIndex);
		dlgProgress.addMessage("Processing finished! Collecting data for table...");
		generateTableHeader();
		writeSingleResultToTable(sliceIndex);
		dlgProgress.setVisible(false);
		dlgProgress.dispose();
		Toolkit.getDefaultToolkit().beep();
	}

	/**
	* This method starts the workflow for all images of the active display
	*/
	protected void startWorkflowForAllImages() {
	
		dlgProgress = new Dialog_WaitingWithProgressBar("Computing Higuchi1D dimensions, please wait... Open console window for further info.",
							logService, false, exec); //isCanceable = true, because processAllInputImages(dlgProgress) listens to exec.shutdown 
		dlgProgress.setVisible(true);

    	logService.info(this.getClass().getName() + " Processing all available images");
		deleteExistingDisplays();
		processAllInputImages();
		dlgProgress.addMessage("Processing finished! Collecting data for table...");
		generateTableHeader();
		writeAllResultsToTable();
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
			if (plotWindowList != null) {
				for (int l = 0; l < plotWindowList.size(); l++) {
					plotWindowList.get(l).setVisible(false);
					plotWindowList.get(l).dispose();
					// plotWindowList.remove(l); /
				}
				plotWindowList.clear();
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

	/** This method takes the active image and computes results. 
	 *
	 **/
	private void processSingleInputImage (int s) {
		
		long startTime = System.currentTimeMillis();
		if (choiceRadioButt_Method.equals("Mean of 180 radial lines [0-pi]") && (booleanGetRadialDhValues)) {
			resultValuesTable = new double[(int) numSlices][194]; //13 + 181
		} else if (choiceRadioButt_Method.equals("Mean of      4 radial lines [0-pi]") && (booleanGetRadialDhValues)) {
			resultValuesTable = new double[(int) numSlices][18]; //13 + 5
		}	else {
			resultValuesTable = new double[(int) numSlices][13];
		}
		
		isGrey = true;

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
		
		
		RandomAccessibleInterval<?> rai = null;	
		if( (s==0) && (numSlices == 1) && (numDimensions == 2) ) { // for only one 2D image;
			rai =  (RandomAccessibleInterval<?>) datasetIn.getImgPlus();

		} else if ( (numSlices > 1) && (numDimensions == 3) ){ // for a stack of 2D images
			rai = (RandomAccessibleInterval<?>) Views.hyperSlice(datasetIn, 2, s);
		
		}

		// Compute regression parameters
		containerPM = process(rai, s); //rai is already 2D, s parameter only for display titles
		// 0 Dh-row, 1 R2-row, 2 StdErr-row, 3 Dh-col, 4 R2-col, 5 StdErr-col, 6 Dh, 7 R2, 8 Stderr

		resultValuesTable[s][0] = resultValues[0]; // Dh-row
		resultValuesTable[s][1] = resultValues[3]; // Dh-col
		resultValuesTable[s][2] = resultValues[6]; // Dh
		resultValuesTable[s][3] = resultValues[1]; // R2-row
		resultValuesTable[s][4] = resultValues[4]; // R2-col
		resultValuesTable[s][5] = resultValues[7]; // R2
		resultValuesTable[s][6] = resultValues[2]; // StdErr-row
		resultValuesTable[s][7] = resultValues[5]; // StdErr-col
		resultValuesTable[s][8] = resultValues[8]; // StdErr
		resultValuesTable[s][9] = resultValues[9];   //#Rows
		resultValuesTable[s][10] = resultValues[10]; //#Columns
		resultValuesTable[s][11] = resultValues[11]; //#RadialLines
		resultValuesTable[s][12] = resultValues[12]; //#Anisotropy index Higuchi Anisotropy index
		
		
		if (choiceRadioButt_Method.equals("Mean of 180 radial lines [0-pi]") && (booleanGetRadialDhValues)) {
			for (int a = 0; a < 181; a++) {
			resultValuesTable[s][13+a] = resultValues[13+a];
			}
		}
		if (choiceRadioButt_Method.equals("Mean of      4 radial lines [0-pi]") && (booleanGetRadialDhValues)) {
			for (int a = 0; a < 5; a++) {
			resultValuesTable[s][13+a] = resultValues[13+a];
			}
		} 
		
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
		if (choiceRadioButt_Method.equals("Mean of 180 radial lines [0-pi]") && (booleanGetRadialDhValues)) {
			resultValuesTable = new double[(int) numSlices][194]; //13 + 181
		} else if (choiceRadioButt_Method.equals("Mean of      4 radial lines [0-pi]") && (booleanGetRadialDhValues)) {
			resultValuesTable = new double[(int) numSlices][18]; //13 + 5
		}	else {
			resultValuesTable = new double[(int) numSlices][13];
		}
		isGrey = true;

		// convert to float values
		// Img<T> image = (Img<T>) dataset.getImgPlus();
		// Img<FloatType> imgFloat; // =
		// opService.convert().float32((Img<T>)dataset.getImgPlus());

		// loop over all slices of stack
		for (int s = 0; s < numSlices; s++) { // p...planes of an image stack
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
	//			// 0 Dh-row, 1 R2-row, 2 StdErr-row, 3 Dh-col, 4 R2-col, 5 StdErr-col, 6 Dh, 7 R2, 8 Stderr
				
				
				RandomAccessibleInterval<?> rai = null;	
				if( (s==0) && (numSlices == 1) && (numDimensions == 2) ) { // for only one 2D image;
					rai =  (RandomAccessibleInterval<?>) datasetIn.getImgPlus();
	
				} else if ( (numSlices > 1) && (numDimensions == 3) ){ // for a stack of 2D images
					rai = (RandomAccessibleInterval<?>) Views.hyperSlice(datasetIn, 2, s);
				
				}
	
				// Compute regression parameters
				containerPM = process(rai, s); //rai is already 2D, s parameter only for display titles
				// 0 Dh-row, 1 R2-row, 2 StdErr-row, 3 Dh-col, 4 R2-col, 5 StdErr-col, 6 Dh, 7 R2, 8 Stderr
	
	
				resultValuesTable[s][0] = resultValues[0]; // Dh-row
				resultValuesTable[s][1] = resultValues[3]; // Dh-col
				resultValuesTable[s][2] = resultValues[6]; // Dh
				resultValuesTable[s][3] = resultValues[1]; // R2-row
				resultValuesTable[s][4] = resultValues[4]; // R2-col
				resultValuesTable[s][5] = resultValues[7]; // R2
				resultValuesTable[s][6] = resultValues[2]; // StdErr-row
				resultValuesTable[s][7] = resultValues[5]; // StdErr-col
				resultValuesTable[s][8] = resultValues[8]; // StdErr
				resultValuesTable[s][9] = resultValues[9];   //#Rows
				resultValuesTable[s][10] = resultValues[10]; //#Columns
				resultValuesTable[s][11] = resultValues[11]; //#RadialLines
				resultValuesTable[s][12] = resultValues[12]; //Anisotropy index Higuchi anisotropy index
	
				if (choiceRadioButt_Method.equals("Mean of 180 radial lines [0-pi]") && (booleanGetRadialDhValues)) {
					for (int a = 0; a < 181; a++) {
					resultValuesTable[s][13+a] = resultValues[13+a];
					}
				} 
				if (choiceRadioButt_Method.equals("Mean of      4 radial lines [0-pi]") && (booleanGetRadialDhValues)) {
					for (int a = 0; a < 5; a++) {
					resultValuesTable[s][13+a] = resultValues[13+a];
					}
				}
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
		GenericColumn columnNumRegStart           = new GenericColumn("Reg Start");
		GenericColumn columnNumRegEnd           = new GenericColumn("Reg End");
		GenericColumn columnMethod       = new GenericColumn("Method");
		BoolColumn columnZeroesRemoved   = new BoolColumn("Zeroes removed");
		DoubleColumn columnDhRow      = new DoubleColumn("Dh-row");
		DoubleColumn columnDhCol      = new DoubleColumn("Dh-col");
		DoubleColumn columnDh         = new DoubleColumn("Dh");
		DoubleColumn columnR2Row      = new DoubleColumn("R2-row");
		DoubleColumn columnR2Col      = new DoubleColumn("R2-col");
		DoubleColumn columnR2         = new DoubleColumn("R2");
		DoubleColumn columnStdErrRow  = new DoubleColumn("StdErr-row");
		DoubleColumn columnStdErrCol  = new DoubleColumn("StdErr-col");
		DoubleColumn columnStdErr     = new DoubleColumn("StdErr");
		IntColumn columnNumRows            = new IntColumn("# Rows");
		IntColumn columnNumColumns         = new IntColumn("# Columns");
		IntColumn columnNumRadialLines     = new IntColumn("# Radial lines");
		DoubleColumn columnAnisotropyIndex =  new DoubleColumn("Anisotropy index");

		tableOut = new DefaultGenericTable();
		tableOut.add(columnFileName);
		tableOut.add(columnSliceName);
		tableOut.add(columnKMax);
		tableOut.add(columnNumRegStart);
		tableOut.add(columnNumRegEnd);
		tableOut.add(columnMethod);
		tableOut.add(columnZeroesRemoved);
		tableOut.add(columnDhRow);
		tableOut.add(columnDhCol);
		tableOut.add(columnDh);
		tableOut.add(columnR2Row);
		tableOut.add(columnR2Col);
		tableOut.add(columnR2);
		tableOut.add(columnStdErrRow);
		tableOut.add(columnStdErrCol);
		tableOut.add(columnStdErr);
		tableOut.add(columnNumRows);
		tableOut.add(columnNumColumns);
		tableOut.add(columnNumRadialLines);
		tableOut.add(columnAnisotropyIndex);
		if (choiceRadioButt_Method.equals("Mean of 180 radial lines [0-pi]") && (booleanGetRadialDhValues)){
			for (int a = 0; a < 181; a++) {
				tableOut.add(new DoubleColumn("Dh-" +  (int)(Math.round(anglesRad[a]*180.0/Math.PI)) + "°"));
			}
		}
		if (choiceRadioButt_Method.equals("Mean of      4 radial lines [0-pi]") && (booleanGetRadialDhValues)){
			for (int a = 0; a < 5; a++) {
				tableOut.add(new DoubleColumn("Dh-" + (int)(Math.round(anglesRad[a]*180.0/Math.PI)) + "°"));
			}
		}
	}

	/** 
	*  writes current result to table
	*  @param int slice number of active image.
	*/
	private void writeSingleResultToTable(int numSlice) { 

		int numRegStart = spinnerInteger_NumRegStart;
		int numRegEnd = spinnerInteger_NumRegEnd;
		int numKMax = spinnerInteger_KMax;

		int s = numSlice;
		// 0 Intercept, 1 Dim, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
		// fill table with values
		tableOut.appendRow();
		tableOut.set("File name",  tableOut.getRowCount() - 1, datasetName);	
		if (sliceLabels != null) tableOut.set("Slice name", tableOut.getRowCount() - 1, sliceLabels[s]);
		tableOut.set("k",              tableOut.getRowCount() - 1, numKMax);
		tableOut.set("Reg Start",         tableOut.getRowCount() - 1, numRegStart);
		tableOut.set("Reg End",         tableOut.getRowCount() - 1, numRegEnd);
		tableOut.set("Method",         tableOut.getRowCount() - 1, choiceRadioButt_Method);
		tableOut.set("Zeroes removed", tableOut.getRowCount() - 1, booleanRemoveZeroes);
		tableOut.set("Dh-row",     tableOut.getRowCount() - 1, resultValuesTable[s][0]);
		tableOut.set("Dh-col",     tableOut.getRowCount() - 1, resultValuesTable[s][1]);
		tableOut.set("Dh",         tableOut.getRowCount() - 1, resultValuesTable[s][2]);
		tableOut.set("R2-row",     tableOut.getRowCount() - 1, resultValuesTable[s][3]);
		tableOut.set("R2-col",     tableOut.getRowCount() - 1, resultValuesTable[s][4]);
		tableOut.set("R2",         tableOut.getRowCount() - 1, resultValuesTable[s][5]);
		tableOut.set("StdErr-row", tableOut.getRowCount() - 1, resultValuesTable[s][6]);
		tableOut.set("StdErr-col", tableOut.getRowCount() - 1, resultValuesTable[s][7]);
		tableOut.set("StdErr",     tableOut.getRowCount() - 1, resultValuesTable[s][8]);
		tableOut.set("# Rows",         tableOut.getRowCount() - 1, (int) resultValuesTable[s][9]);
		tableOut.set("# Columns",      tableOut.getRowCount() - 1, (int) resultValuesTable[s][10]);
		tableOut.set("# Radial lines", tableOut.getRowCount() - 1, (int) resultValuesTable[s][11]);
		tableOut.set("Anisotropy index",            tableOut.getRowCount() - 1, resultValuesTable[s][12]); //Anisotropy index Higuchi anistropy index =(Dr-Dc)/(De-Dt)

		//add 181 angles
		if (choiceRadioButt_Method.equals("Mean of 180 radial lines [0-pi]") && (booleanGetRadialDhValues)){
			for (int a = 0; a < 181; a++) {
				tableOut.set("Dh-"+(int)(Math.round(anglesRad[a]*180.0/Math.PI))+"°", tableOut.getRowCount() - 1, resultValuesTable[s][13+a]);
			}
		}
		//add 4+1 angles
		if (choiceRadioButt_Method.equals("Mean of      4 radial lines [0-pi]") && (booleanGetRadialDhValues)){
			for (int a = 0; a < 5; a++) {
				tableOut.set("Dh-"+(int)(Math.round(anglesRad[a]*180.0/Math.PI))+"°", tableOut.getRowCount() - 1, resultValuesTable[s][13+a]);
			}
		}
	}

	/** 
	*  Writes all results to table
	*/
	private void writeAllResultsToTable() {

		int numRegStart = spinnerInteger_NumRegStart;
		int numRegEnd = spinnerInteger_NumRegEnd;
		int numKMax = spinnerInteger_KMax;

		// loop over all slices
		for (int s = 0; s < numSlices; s++) { // slices of an image stack
			// 0 Intercept, 1 Dim, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
			// fill table with values
			tableOut.appendRow();
			tableOut.set("File name",  tableOut.getRowCount() - 1, datasetName);	
			if (sliceLabels != null) tableOut.set("Slice name", tableOut.getRowCount() - 1, sliceLabels[s]);
			tableOut.set("k",              tableOut.getRowCount() - 1, numKMax);
			tableOut.set("Reg Start",         tableOut.getRowCount() - 1, numRegStart);
			tableOut.set("Reg End",         tableOut.getRowCount() - 1, numRegEnd);
			tableOut.set("Method",         tableOut.getRowCount() - 1, choiceRadioButt_Method);
			tableOut.set("Zeroes removed", tableOut.getRowCount() - 1, booleanRemoveZeroes);
			tableOut.set("Dh-row",     tableOut.getRowCount() - 1, resultValuesTable[s][0]);
			tableOut.set("Dh-col",     tableOut.getRowCount() - 1, resultValuesTable[s][1]);
			tableOut.set("Dh",         tableOut.getRowCount() - 1, resultValuesTable[s][2]);
			tableOut.set("R2-row",     tableOut.getRowCount() - 1, resultValuesTable[s][3]);
			tableOut.set("R2-col",     tableOut.getRowCount() - 1, resultValuesTable[s][4]);
			tableOut.set("R2",         tableOut.getRowCount() - 1, resultValuesTable[s][5]);
			tableOut.set("StdErr-row", tableOut.getRowCount() - 1, resultValuesTable[s][6]);
			tableOut.set("StdErr-col", tableOut.getRowCount() - 1, resultValuesTable[s][7]);
			tableOut.set("StdErr",     tableOut.getRowCount() - 1, resultValuesTable[s][8]);
			tableOut.set("# Rows",         tableOut.getRowCount() - 1, (int)resultValuesTable[s][9]);
			tableOut.set("# Columns",      tableOut.getRowCount() - 1, (int)resultValuesTable[s][10]);
			tableOut.set("# Radial lines", tableOut.getRowCount() - 1, (int)resultValuesTable[s][11]);
			tableOut.set("Anisotropy index",            tableOut.getRowCount() - 1, resultValuesTable[s][12]); //Anisotropy index Higuchi anisotropy index =(Dr-Dc)/(De-Dt)
			
			//add 181 angles
			if (choiceRadioButt_Method.equals("Mean of 180 radial lines [0-pi]") && (booleanGetRadialDhValues)){
				for (int a = 0; a < 181; a++) {
					tableOut.set("Dh-"+(int)(Math.round(anglesRad[a]*180.0/Math.PI))+"°", tableOut.getRowCount() - 1, resultValuesTable[s][13+a]);
			
				}
			}
			//add 5 angles
			if (choiceRadioButt_Method.equals("Mean of      4 radial lines [0-pi]") && (booleanGetRadialDhValues)){
				for (int a = 0; a < 5; a++) {
					tableOut.set("Dh-"+(int)(Math.round(anglesRad[a]*180.0/Math.PI))+"°", tableOut.getRowCount() - 1, resultValuesTable[s][13+a]);
				}
			}
		}
	}

	/**
	*
	* Processing
	*/
	private ResultsFromProcessMethod process(RandomAccessibleInterval<?> rai, int plane) { // plane plane (Image) number
	
		if (rai == null) {
			logService.info(this.getClass().getName() + " WARNING: rai==null, no image for processing!");
		}
		int numRegStart = spinnerInteger_NumRegStart;
		int numRegEnd = spinnerInteger_NumRegEnd;
		int numKMax = spinnerInteger_KMax;
		boolean removeZeores = booleanRemoveZeroes;
	
		int numBands = 1;

		boolean optShowPlot            = booleanShowDoubleLogPlot;
		boolean optShowSomeRadialLinePlots = booleanShowSomeRadialLinePlots;

		long width = rai.dimension(0);
		long height = rai.dimension(1);

		String imageType = "Grey"; // "Grey" "RGB"....

		double[] resultValues;
		if (choiceRadioButt_Method.equals("Mean of 180 radial lines [0-pi]")) {
			resultValues = new double[194]; //13 +181 Dhs
		} else if (choiceRadioButt_Method.equals("Mean of      4 radial lines [0-pi]")) {
				resultValues = new double[18]; //13 + 5 Dhs
		} else {
			resultValues = new double[13]; // Dim-row, R2-row, StdErr-row, Dim-col, R2-col, StdErr-col, Dim, R2, StdErr
		}
	
		double[][] totals = new double[numKMax][numBands];
		// double[] totalsMax = new double[numBands]; //for binary images
		double[][] eps = new double[numKMax][numBands];

		// definition of eps
		for (int kk = 0; kk < numKMax; kk++) {
			for (int b = 0; b < numBands; b++) {
				if (isGrey) {
					eps[kk][b] = kk + 1;
				} else {
					eps[kk][b] = kk + 1; // *width*height (not necessary);

				}
				//logService.info(this.getClass().getName() + " k=" + kk + " eps= " + eps[kk][b]);
			}
		}
	
		if (isGrey) {// binary image
			//******************************************************************************************************
			if (choiceRadioButt_Method.equals("Single centered row/column")) {

				RandomAccess<?> ra=  rai.randomAccess();
				Higuchi hig;
				double[] L;
				double[] regressionParams;
				
			
				// Dh-row Single Row--------------------------------------------------------------------------------
				int numActualRows = 0;
				sequence1D = new double[(int) width];
				for (int w = 0; w < width; w++) { // one row
					ra.setPosition(w, 0);
					ra.setPosition(height / 2, 1); //row in the middle of the image (column)
					sequence1D[w] = ((UnsignedByteType) ra.get()).getRealFloat();
				}
				if (removeZeores) sequence1D = removeZeroes(sequence1D);
				logService.info(this.getClass().getName() + " Single row #: "+ (height/2) + "  Size of sequence = " + sequence1D.length);
				if (sequence1D.length > (numKMax * 2)) { // only data series which are large enough
					numActualRows += 1;
					hig = new Higuchi();
					L = hig.calcLengths(sequence1D, numKMax);
					regressionParams = hig.calcDimension(L, numRegStart, numRegEnd);
					// 0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
					
					if (optShowPlot) {
						String preName = "Row-";
						showPlot(hig.getLnDataX(), hig.getLnDataY(), preName, plane, numRegStart, numRegEnd);
					}
					
					resultValues[0] = -regressionParams[1]; // Dh = -slope
					resultValues[1] = regressionParams[4];
					resultValues[2] = regressionParams[3];
				}
				
				// Dh-col Single Column---------------------------------------------------------------------------------
				int numActualColumns = 0;
				sequence1D = new double[(int) height];
				for (int h = 0; h < height; h++) { // one row
					ra.setPosition(width / 2, 0); // column in the middle of the image (row)
					ra.setPosition(h, 1);
					sequence1D[h] = ((UnsignedByteType) ra.get()).getRealFloat();
				}
				if (removeZeores) sequence1D = removeZeroes(sequence1D);
				logService.info(this.getClass().getName() + " Single column #: "+ (width/2) + "  Size of sequence = " + sequence1D.length);
				//if (sequence1D.length == 0) return null; //e.g. if sequence had only NaNs

				if (sequence1D.length > (numKMax * 2)) { // only data series which are large enough
					numActualColumns += 1;
					hig = new Higuchi();
					L = hig.calcLengths(sequence1D, numKMax);
					regressionParams = hig.calcDimension(L, numRegStart, numRegEnd);
					// 0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
					
					if (optShowPlot) {
						String preName = "Col-";
						showPlot(hig.getLnDataX(), hig.getLnDataY(), preName, plane, numRegStart, numRegEnd);
					}
					
					resultValues[3] = -regressionParams[1]; // Dh = -slope
					resultValues[4] = regressionParams[4];
					resultValues[5] = regressionParams[3];
				}
				//Dh --------------------------------------------------------------------------------------------------
				resultValues[6] = (resultValues[0] + resultValues[3]) / 2.0; // Dh = (Dh-row + Dh-col)/2
				resultValues[7] = (resultValues[1] + resultValues[4]) / 2.0; // R2 = (R2-row + R2-col)/2
				resultValues[8] = (resultValues[2] + resultValues[5]) / 2.0; // StdErr = (StdErr-row + StdErr-col)/2
				
				resultValues[9]  = numActualRows;  //# Actual number of Rows
				resultValues[10] = numActualColumns; //# Actual number of Columns
				resultValues[11] = Float.NaN; //# Actual number of Radial lines
				resultValues[12] = Math.abs(resultValues[0] - resultValues[3])/(2-1); //ABS(Dh-row - Dh -col)/(De - Dt);
			}
			//**********************************************************************************************************
			if (choiceRadioButt_Method.equals("Mean of all rows/columns")) {
				RandomAccess<?> ra=  rai.randomAccess();
				Higuchi hig;
				double[] L;
				double[] regressionParams;
				
			
				// Dh-row Rows---------------------------------------------------------------------------------
				int numActualRows = 0;
				for (int h = 0; h < height; h++) { 
					sequence1D = new double[(int) width];
					for (int w = 0; w < width; w++) { // one row
						ra.setPosition(w, 0);
						ra.setPosition(h, 1); //row at position h
						sequence1D[w] = ((UnsignedByteType) ra.get()).getRealFloat();
					}
					if (removeZeores) sequence1D = removeZeroes(sequence1D);
					//logService.info(this.getClass().getName() + " Row #: "+ h + "  Size of sequence = " + sequence1D.length);
					//if (sequence1D.length == 0) return null; //e.g. if sequence had only NaNs
					if (sequence1D.length > (numKMax * 2)) { // only data series which are large enough
						hig = new Higuchi();
						L = hig.calcLengths(sequence1D, numKMax);
						regressionParams = hig.calcDimension(L, numRegStart, numRegEnd);
						// 0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
						
						if (optShowPlot) {
							if((h == 0) || (h == height/2) ||(h ==height-1)) { //show first middle and last plot
								String preName = "Row-";
								showPlot(hig.getLnDataX(), hig.getLnDataY(), preName, plane, numRegStart, numRegEnd);
							}
						} //if
						
						if (regressionParams[4] > 0.9) {
							numActualRows += 1;
							resultValues[0] += -regressionParams[1]; // Dh = -slope
							resultValues[1] += regressionParams[4];
							resultValues[2] += regressionParams[3];
						}
					} //if
				} //for h
				
				resultValues[0] = resultValues[0]/numActualRows; //average
				resultValues[1] = resultValues[1]/numActualRows; //average
				resultValues[2] = resultValues[2]/numActualRows; //average
				
				// Dh-col Columns---------------------------------------------------------------------------------
				int numActualColumns = 0;
				for (int w = 0; w < width; w++) {
					sequence1D = new double[(int) height];
					for (int h = 0; h < height; h++) { // one row
						ra.setPosition(w, 0); // column at position w
						ra.setPosition(h, 1);
						sequence1D[h] = ((UnsignedByteType) ra.get()).getRealFloat();
					}
					if (removeZeores) sequence1D = removeZeroes(sequence1D);
					//logService.info(this.getClass().getName() + " Column #: "+ w + "  Size of sequence = " + sequence1D.length);
					//if (sequence1D.length == 0) return null; //e.g. if sequence had only NaNs
					if (sequence1D.length > (numKMax * 2)) { // only data series which are large enough
						hig = new Higuchi();
						L = hig.calcLengths(sequence1D, numKMax);
						regressionParams = hig.calcDimension(L, numRegStart, numRegEnd);
						// 0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
						
						if (optShowPlot) {
							if((w == 0) || (w == width/2) ||(w ==width-1)) { //show first middle and last plot
								String preName = "Col-";
								showPlot(hig.getLnDataX(), hig.getLnDataY(), preName, plane, numRegStart, numRegEnd);
							}
						} // IF
						
						if (regressionParams[4] > 0.9) { //R2 >0.9
							numActualColumns += 1;
							resultValues[3] += -regressionParams[1]; // Dh = -slope
							resultValues[4] += regressionParams[4];
							resultValues[5] += regressionParams[3];
						}//
					} //if
				} //for w
				resultValues[3] = resultValues[3]/numActualColumns; //average
				resultValues[4] = resultValues[4]/numActualColumns; //average
				resultValues[5] = resultValues[5]/numActualColumns; //average
				//Dh --------------------------------------------------------------------------------------------------
				resultValues[6] = (resultValues[0] + resultValues[3]) / 2.0; // Dh = (Dh-row + Dh-col)/2
				resultValues[7] = (resultValues[1] + resultValues[4]) / 2.0; // R2 = (R2-row + R2-col)/2
				resultValues[8] = (resultValues[2] + resultValues[5]) / 2.0; // StdErr = (StdErr-row + StdErr-col)/2
				
				resultValues[9]  = numActualRows;  //# Actual number of Rows
				resultValues[10] = numActualColumns; //# Actual number of Columns
				resultValues[11] = Float.NaN; //# Actual number of Radial lines  
				resultValues[12] = Math.abs(resultValues[0] - resultValues[3])/(2-1); //ABS(Dh-row - Dh -col)/(De - Dt);
			}
			//******************************************************************************************************************************************
			if (choiceRadioButt_Method.equals("Single meander row/column")) {
				RandomAccess<?> ra=  rai.randomAccess();
				Higuchi hig;
				double[] L;
				double[] regressionParams;
				
			
				// Dh-row Single meander row---------------------------------------------------------------------------------
				int numActualRows = 0;
				sequence1D = new double[(int) (width*height)];
				
				for (int h = 0; h < height; h++) { // columns
					if (h % 2 ==0) {//even
						for (int w = 0; w < width; w++) { // one row
							ra.setPosition(w, 0);
							ra.setPosition(h, 1); //row in the middle of the image (column)
							sequence1D[w + h * (int)width] = ((UnsignedByteType) ra.get()).getRealFloat();
						}
					}
					else {
						for (int w = (int) (width-1); w >= 0; w--) { // one row
							ra.setPosition(w, 0);
							ra.setPosition(h, 1); //row in the middle of the image (column)
							sequence1D[w + h* (int)width] = ((UnsignedByteType) ra.get()).getRealFloat();
						}
					}
				}
				if (removeZeores) sequence1D = removeZeroes(sequence1D);
				logService.info(this.getClass().getName() + " Single meander row:   Size of sequence = " + sequence1D.length);
				//if (sequence1D.length == 0) return null; //e.g. if sequence had only NaNs
				if (sequence1D.length > (numKMax * 2)) { // only data series which are large enough
					numActualRows += 1;
					hig = new Higuchi();
					L = hig.calcLengths(sequence1D, numKMax);
					regressionParams = hig.calcDimension(L, numRegStart, numRegEnd);
					// 0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
					
					if (optShowPlot) {
						String preName = "Row-";
						showPlot(hig.getLnDataX(), hig.getLnDataY(), preName, plane, numRegStart, numRegEnd);
					}
					
					resultValues[0] = -regressionParams[1]; // Dh = -slope
					resultValues[1] = regressionParams[4];
					resultValues[2] = regressionParams[3];
	
				}
				
				// Dh-col Single meander column---------------------------------------------------------------------------------
				int numActualColumns = 0;
				sequence1D = new double[(int) (width*height)];
				
				for (int w = 0; w < width; w++) { // columns
					if (w % 2 ==0) {//even
						for (int h = 0; h < height; h++) { // one row
							ra.setPosition(w, 0);
							ra.setPosition(h, 1); //row in the middle of the image (column)
							sequence1D[h + w * (int)height] = ((UnsignedByteType) ra.get()).getRealFloat();
						}
					}
					else {
						for (int h = (int) (height-1); h >= 0; h--) { // one row
							ra.setPosition(w, 0);
							ra.setPosition(h, 1); //row in the middle of the image (column)
							sequence1D[h + w* (int)height] = ((UnsignedByteType) ra.get()).getRealFloat();
						}
					}
				}
				if (removeZeores) sequence1D = removeZeroes(sequence1D);
				logService.info(this.getClass().getName() + " Single meander column:   Size of sequence = " + sequence1D.length);
				//if (sequence1D.length == 0) return null; //e.g. if sequence had only NaNs
				if (sequence1D.length > (numKMax * 2)) { // only data series which are large enough
					numActualColumns += 1;
					hig = new Higuchi();
					L = hig.calcLengths(sequence1D, numKMax);
					regressionParams = hig.calcDimension(L, numRegStart, numRegEnd);
					// 0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
					
					if (optShowPlot) {
						String preName = "Col-";
						showPlot(hig.getLnDataX(), hig.getLnDataY(), preName, plane, numRegStart, numRegEnd);
					}
					
					resultValues[3] = -regressionParams[1]; // Dh = -slope
					resultValues[4] = regressionParams[4];
					resultValues[5] = regressionParams[3];		
				}
				//Dh --------------------------------------------------------------------------------------------------
				resultValues[6] = (resultValues[0] + resultValues[3]) / 2.0; // Dh = (Dh-row + Dh-col)/2
				resultValues[7] = (resultValues[1] + resultValues[4]) / 2.0; // R2 = (R2-row + R2-col)/2
				resultValues[8] = (resultValues[2] + resultValues[5]) / 2.0; // StdErr = (StdErr-row + StdErr-col)/2
					
				resultValues[9]  = numActualRows; //# Actual Rows
				resultValues[10] = numActualColumns; //# Actual Columns
				resultValues[11] = Float.NaN;    //# Actual Radial lines  
				resultValues[12] = Math.abs(resultValues[0] - resultValues[3])/(2-1); //ABS(Dh-row - Dh -col)/(De - Dt);
			}
			//******************************************************************************************************************************************
			if (choiceRadioButt_Method.equals("Mean of 180 radial lines [0-pi]") || choiceRadioButt_Method.equals("Mean of      4 radial lines [0-pi]")) {
				
				Higuchi hig;
				double[] L;
				double[] regressionParams;
				int numAngles = 0;
				int indexAngle_0  = 0;
				int indexAngle_90 = 0;
				double Dh_0  = 0.0;
				double Dh_90 = 0.0; 
				//define number of angles in the range of 0 - pi
				//int numAngles = (180 + 1);  //maximal 180, maybe only 4 (0°, 45°, 90°, 135°, 180°)
				if (choiceRadioButt_Method.equals("Mean of 180 radial lines [0-pi]")) {
					numAngles = (180 + 1); //range 0 - pi through the center of the image
					indexAngle_0 = 0;
					indexAngle_90 = (numAngles -1)/2; // = 90
				}
				if (choiceRadioButt_Method.equals("Mean of      4 radial lines [0-pi]")) {
					numAngles = (4 + 1);   //range 0 - pi through the center of the image
					indexAngle_0 = 0;
					indexAngle_90 = (numAngles -1)/2; // = 2
				}
								
				anglesRad = new double[numAngles];
				for (int i = 0; i < numAngles; i++) {
					//angles[i] = (i * Math.PI / (numAngles - 1) - (Math.PI / 2.0)); // -pi/2,...,0,...+pi/2
					anglesRad[i] = (i * Math.PI / (numAngles - 1)); // simply Counterclockwise 
				}
				
				//Dh-r--------------------------------------------------------------------------------------------------	
				int numActualRadialLines = 0; //some lines have too less pixels and are thrown away
				for (int a = 0; a < numAngles; a++) { // loop through angles

					double slope = -Math.tan(anglesRad[a]); //minus, because positive Y-axis values go downwards  

					// set start point x1,y1 and end point x2,y2
					double offSetX = (width / 2.0);
					double offSetY = (height/ 2.0);
					double x1 = 0;
					double y1 = 0;
					double x2 = 0;
					double y2 = 0;

					if (anglesRad[a] == +(Math.PI / 2.0)) { //slope infinite  from bottom to top
						x1 = 1;
						y1 = offSetY;
						x2 = 1;
						y2 = -offSetY + 1;
					}  else if (anglesRad[a] == 0) { //slope = 0  //from left to right
						x1 = -offSetX + 1;
						y1 = 1;
						x2 = offSetX;
						y2 = 1;
					}  else if (anglesRad[a] == Math.PI) { //slope = -0   //from right to left
						x1 = offSetX;
						y1 = 1;
						x2 = -offSetX + 1;
						y2 = 1;
					} else if ((anglesRad[a] >0) && (anglesRad[a] < Math.PI/2.0)){
						
						//start from little left down to little right up around the center of the image
						x1 = -0.01;
						y1 = (int) (slope * x1);

						x2 = 0.01;
						y2 = (int) (slope * x2);

						//Scroll x1 to the left until y1 reaches the border of the image
						while ((y1 + offSetY) < height) { // out of image
							x1 = x1 - 0.01;
							if ((x1 + offSetX) < 1) { //control of x
								x1 = x1 + 0.01;
								y1 = (int) (slope * x1);
								break;
							}			
							y1 = (int) (slope * x1);
						}
						//Scroll x2 to the right until y2 reaches the border of the image
						while ((y2 + offSetY) > 1) { // out of image
							x2 = x2 + 0.01;
							if ((x2 + offSetX) > width ) { //control of x
								x2 = x2 - 0.01;
								y2 = (int) (slope * x2);
								break;
							}			
							y2 = (int) (slope * x2);
						}
						
	
					} else if ((anglesRad[a] > Math.PI/2.0) && (anglesRad[a] < Math.PI)){
						//start from little right down to little left up around the center of the image
						x1 = 0.01;
						y1 = (int) (slope * x1);

						x2 = -0.01;
						y2 = (int) (slope * x2);

						//Scroll x1 to the right until y1 reaches the border of the image
						while ((y1 + offSetY) < height) { // out of image
							x1 = x1 + 0.01;
							if ((x1 + offSetX) > width ) { //control of x
								x1 = x1 - 0.01;
								y1 = (int) (slope * x1);
								break;
							}			
							y1 = (int) (slope * x1);
						}
						//Scroll x2 to the left until y2 reaches the border of the image
						while ((y2 + offSetY) > 1) { // out of image
							x2 = x2 - 0.01;
							if ((x2 + offSetX) < 1 ) { //control of x
								x2 = x2 - 0.01;
								y2 = (int) (slope * x2);
								break;
							}			
							y2 = (int) (slope * x2);
						}
					}
				
					//Set to integer values and image coordinates from 0 to width-1, height-1
					x1 = Math.round(x1 + offSetX) - 1;
					y1 = Math.round(y1 + offSetY) - 1;
					x2 = Math.round(x2 + offSetX) - 1;
					y2 = Math.round(y2 + offSetY) - 1;
					
					// System.out.println("IqmOpFracHiguchi: x1=" + x1+ "  y1="+ y1 + "    x2=" + x2+ "  y2="+ y2);
					if (x1 >= width)
						logService.info(this.getClass().getName() + " ERROR: a=" + a +  "   x1 too high, x1="+ x1);
					if (x1 < 0)
						logService.info(this.getClass().getName() + " ERROR: a=" + a +  "   x1 too low,  x1="+ x1);
					if (x2 >= width)
						logService.info(this.getClass().getName() + " ERROR: a=" + a +  "   x2 too high, x2="+ x2);
					if (x2 < 0)
						logService.info(this.getClass().getName() + " ERROR: a=" + a +  "   x2 too low,  x2="+ x2);
					if (y1 >= height)
						logService.info(this.getClass().getName() + " ERROR: a=" + a +  "   y1 too high, y1="+ y1);
					if (y1 < 0)
						logService.info(this.getClass().getName() + " ERROR: a=" + a +  "   y1 too low,  y1="+ y1);
					if (y2 >= height)
						logService.info(this.getClass().getName() + " ERROR: a=" + a +  "   y2 too high, y2="+ y2);
					if (y2 < 0)
						logService.info(this.getClass().getName() + " ERROR: a=" + a +  "   y2 too low,  y2="+ y2);
					
//                  WritableLine line = GeomMasks.line(new double[]{x1 + offSetX, x2 + offSetX}, new double[] { y1 + offSetY,  y2 + offSetY}, true);
//					
//					DefaultWritableLine line = new DefaultWritableLine(new double[]{x1 + offSetX, y1 + offSetY}, new double[]{x2 + offSetX,  y2 + offSetY}, true);
//				
//					IterableRegion<BoolType> onLine = Regions.iterable(
//					           Views.interval(
//					             Views.raster(Masks.toRealRandomAccessible(line)),
//					             Intervals.smallestContainingInterval(line)));
//									
//					// with Region.sample
//					Cursor<FloatType> cursor = Regions.sample(onLine, iv).cursor();
//					sequence1D = new double[(int) (onLine.size())];
//					logService.info(this.getClass().getName() + " line: "+ a + "  size of sequence=" + onLine.size());
//					int s = 0;
//					while (cursor.hasNext()) {
//						cursor.fwd();			
//						sequence1D[s] = cursor.get().get();
//						s += 1;
//					}
						
//					RealRandomAccessible< FloatType > interpolant = Views.interpolate(Views.extendMirrorSingle(iv),  new NLinearInterpolatorFactory<FloatType>());
//					RealRandomAccess<FloatType> rra = interpolant.realRandomAccess();
//					
//					Cursor<Void> cursor = onLine.cursor();
//					sequence1D = new double[(int) (onLine.size())];
//					int s = 0;
//					while (cursor.hasNext()) {
//						cursor.fwd();
//						rra.setPosition(cursor);
//						sequence1D[s] = rra.get().get();
//						s += 1;
//					}
					
				
					
		            Point point1 = new Point((long) x1, (long)y1);
		            Point point2 = new Point((long) x2, (long)y2);
		            
//					//Print some 
//					if ((anglesRad[a]*180/Math.PI == 89) || (anglesRad[a]*180/Math.PI == 91)) {
//						logService.info(this.getClass().getName() + "a="+a+"   Point1=" + point1.toString());
//						logService.info(this.getClass().getName() + "a="+a+"   Point2=" + point2.toString());
//					}          
		            System.out.println("a="+a+"   Point1=" + point1.toString());
		            System.out.println("a="+a+"   Point2=" + point2.toString());
		 		              
					lineBresenham = new BresenhamLine(rai, point1, point2);
					coords = BresenhamLine.generateCoords(point1, point2);
					
					sequence1D = new double[(int) (coords.size())];
					xAxis1D  = new double[(int) (coords.size())];		
					
					int s = 0;
					while (lineBresenham.hasNext()) {
						lineBresenham.fwd();
						sequence1D[s] = ((UnsignedByteType) lineBresenham.get()).get();
						xAxis1D[s] = (double)(s +1);
						s += 1;
					}	
					//does not work to show lines in an image
//					RandomAccess<FloatType> ra = iv.randomAccess();
//					while (lineBresenham.hasNext()) {
//						lineBresenham.fwd();
//						ra.setPosition(lineBresenham);
//						ra.get().set(150.f);
//					}
//				    this.uiService.show("Line a=", ra);
//					ij.ui().show("Line a=" + a, ra);
					
					//Length correction---------------------------------------------------------------------
					double lengthCorrFactor = 1.0; //no correction
					boolean doLengthCorrection = true;
					if (doLengthCorrection) {
						//Euclidean distance
						double distance = Math.sqrt((x2-x1)*(x2-x1) + (y2-y1)*(y2-y1));
						//Length correction factor (diagonal lines are longer than horizontal or vertical lines 
						lengthCorrFactor = distance/(sequence1D.length -1);
						
//						//destinationLength destination length
//				        int sourceLength = sequence1D.length; //source length
//				        int destinationLength = (int)Math.round(sourceLength*(lengthCorrFactor)); 
//				        double[] destination = new double[destinationLength];
//				      		       
//				        //simple linear interpolation is worse than Apache Math3 LinearInterpolator
//				        destination[0] = sequence1D[0];
//				        destination[destinationLength-1] = sequence1D[sourceLength-1];
	//
//				        for (int i = 1; i < destinationLength-1; i++){
//				            double jd = ((double)i * (double)(sourceLength - 1) / (double)(destinationLength - 1));
//				            int j = (int)jd;
//				            destination[i] = (double) (sequence1D[j] + (sequence1D[j + 1] - sequence1D[j]) * (jd - (double)j));
//				        }
//				        //offSet because sequence must be truncated from both sides
//				   		int offSet = (destinationLength - sourceLength)/2;
//				   		//Remove values which are over the original length
//				   		for (int i = 0; i < sequence1D.length; i++) {
//				   			 sequence1D[i] = (double)destination[i + offSet];
//				   		}
				        	     		
				        interpolator = new LinearInterpolator();
					    psf = interpolator.interpolate(xAxis1D, sequence1D);
					    
					    double offSet = (distance - sequence1D.length +1)/2.0;
					    double[] sequence1DInterpolated = new double[sequence1D.length];
					    //double step = (sequence1D.length-1)/distance;
					    double step = 1.0/lengthCorrFactor;
					    for (int n = 1; n <= sequence1DInterpolated.length; n++) {
					    	sequence1DInterpolated[n-1] = (double)psf.value(offSet + n*step);
					    }
				        sequence1D = sequence1DInterpolated;
						
					} else {
						//no length correction
						//do nothing with sequence 1D
					}
			       //----------------------------------------------------------------------------------------
			        
					if (removeZeores) sequence1D = removeZeroes(sequence1D);
					
					//Resample to corrected length
					
					
					
					if (optShowSomeRadialLinePlots ){
						// get plots of radial lines
	//					if(    (anglesRad[a]*180/Math.PI == 0) 
	//						|| (anglesRad[a]*180/Math.PI == 45) 
	//						|| (anglesRad[a]*180/Math.PI == 90) 
	//						|| (anglesRad[a]*180/Math.PI == 135) 
	//						|| (anglesRad[a]*180/Math.PI == 180)
	//						|| (anglesRad[a]*180/Math.PI == 1)
	//						|| (anglesRad[a]*180/Math.PI == 44)  
	//						|| (anglesRad[a]*180/Math.PI == 46)
	//						|| (anglesRad[a]*180/Math.PI == 89)
	//						|| (anglesRad[a]*180/Math.PI == 91)
	//						|| (anglesRad[a]*180/Math.PI == 134)
	//						|| (anglesRad[a]*180/Math.PI == 136) 
	//						|| (anglesRad[a]*180/Math.PI == 179)) { //show some plots
						if(  (anglesRad[a]*180/Math.PI == 0)
							|| (anglesRad[a]*180/Math.PI == 45) 
							|| (anglesRad[a]*180/Math.PI == 90)
							|| (anglesRad[a]*180/Math.PI == 135)
							|| (anglesRad[a]*180/Math.PI == 180)) { //show some plots
							plotProfile = new Plot(""+ (anglesRad[a]*180/Math.PI) + "° - Grey value profile", "Pixel number", "Grey value");
							//int for shape 0 circle, 1 X, 2 connected dots, 3 square, 4 triangle, 5 +, 6 dot, 7 connected circles, 8 diamond 
							plotProfile.addPoints(xAxis1D, sequence1D, 7); 
							//plotProfile.show();
							PlotWindow window = plotProfile.show();
							plotWindowList.add(window);
						}
					}
										
					logService.info(this.getClass().getName() + " Radial line: "+ a + "  Size of sequence = " + sequence1D.length + "   Length correction factor = " + lengthCorrFactor);
					//if (sequence1D.length == 0) return null; //e.g. if sequence had only NaNs
					if (sequence1D.length > (numKMax * 2)) { // only data series which are large enough		
						hig = new Higuchi();
						L = hig.calcLengths(sequence1D, numKMax);
						regressionParams = hig.calcDimension(L, numRegStart, numRegEnd);
						// 0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
						
						if (optShowPlot) {
							if(    (anglesRad[a]*180/Math.PI == 0)
								|| (anglesRad[a]*180/Math.PI == 45)
								|| (anglesRad[a]*180/Math.PI == 90)
								|| (anglesRad[a]*180/Math.PI == 135)
								|| (anglesRad[a]*180/Math.PI == 180)) { //show first middle and last plot
								String preName = "" + (anglesRad[a]*180.0/Math.PI) +"° - ";
								showPlot(hig.getLnDataX(), hig.getLnDataY(), preName, plane, numRegStart, numRegEnd);
							}
						}					
						double dim = -regressionParams[1];
						if (dim == 0.0) dim = Double.NaN;
						if (regressionParams[4] > 0.9) { //R2 >0.9
							if (a < (numAngles - 1)) { // Mean only from 4 bzw. 180 angles
								numActualRadialLines += 1;
								resultValues[6] += dim; // Dh = -slope
								resultValues[7] += regressionParams[4];
								resultValues[8] += regressionParams[3];
							}
							if ( a == indexAngle_0)  Dh_0  = dim;
							if ( a == indexAngle_90) Dh_90 = dim;
							//add 180 +1 angles
							if (booleanGetRadialDhValues){
								//one of 181 Dh values
								resultValues[13+a] = dim;
							}
							
			
						} //R2 >0.9
					}				
				} //angles a for (int a = 0; a < numAngles; a++) { // loop through angles
				// mean values
				resultValues[6] = resultValues[6]/numActualRadialLines; //average   
				resultValues[7] = resultValues[7]/numActualRadialLines; //average
				resultValues[8] = resultValues[8]/numActualRadialLines; //average		
				
				
				//set other table entries to NaN
				resultValues[0] = Float.NaN;
				resultValues[1] = Float.NaN;
				resultValues[2] = Float.NaN;
				resultValues[3] = Float.NaN;
				resultValues[4] = Float.NaN;
				resultValues[5] = Float.NaN;
				
				//set # of actual sequences
				resultValues[9] = Float.NaN;  //# Actual Rows
				resultValues[10] = Float.NaN;//# Actual Columns
				resultValues[11] = numActualRadialLines; // # Radial lines	
				resultValues[12] = Math.abs(Dh_0 - Dh_90)/(2-1); //ABS(Dh-0° - Dh-90°)/(De - Dt);
				
				logService.info(this.getClass().getName() + " Number of actual radial lines=" + numActualRadialLines);
			}

		} else { // grey value image

		}
		return new ResultsFromProcessMethod(resultValues);
		//Dim-row, R2-row, StdErr-row, Dim-col, R2-col, StdErr-col, Dim, R2, StdErr
		//Output
		//uiService.show(tableOutName, table);
		////result = ops.create().img(image, new FloatType()); may not work in older Fiji versions
		//result = new ArrayImgFactory<>(new FloatType()).create(image.dimension(0), image.dimension(1)); 
		//table
	}

	// This method shows the double log plot
	private void showPlot(double[] lnDataX, double[] lnDataY, String preName, int plane, int numRegStart, int numRegEnd) {
		if (isGrey) {
			if (lnDataX == null) {
				logService.info(this.getClass().getName() + " lnDataX == null, cannot display the plot!");
				return;
			}
			if (lnDataY == null) {
				logService.info(this.getClass().getName() + " lnDataY == null, cannot display the plot!");
				return;
			}
			if (plane < 0) {
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
				preName = "Slice-" + String.format("%03d", plane) + "-";
			} else {
				preName = preName + String.format("%03d", plane) + "-";
			}
			
			boolean isLineVisible = false; // ?
			RegressionPlotFrame doubleLogPlot = DisplayRegressionPlotXY(lnDataX, lnDataY, isLineVisible,
					"Double log plot - Higuchi1D dimension", preName + datasetName, "ln(k)", "ln(L)", "", numRegStart, numRegEnd);
			doubleLogPlotList.add(doubleLogPlot);
		}
		if (!isGrey) {

		}
	}
	
	// This method removes zero background from field sequence1D
	private double[] removeZeroes(double[] sequence) {
		int lengthOld = sequence.length;
		int lengthNew = 0;
		
		for (int i = 0; i < lengthOld; i++) {
			if (sequence[i] != 0) lengthNew += 1;
		}
		sequence1D = new double[lengthNew];
		int ii = -1;
		for (int i = 0; i < lengthOld; i++) {
			if (sequence[i] != 0) {
				ii +=  1;
				sequence1D[ii] = sequence[i];
			}
		}
		return sequence1D;
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
	private RegressionPlotFrame DisplayRegressionPlotXY(double[] dataX, double[] dataY,
			boolean isLineVisible, String frameTitle, String plotLabel, String xAxisLabel, String yAxisLabel, String legendLabel,
			int numRegStart, int numRegEnd) {
		// jFreeChart
		RegressionPlotFrame pl = new RegressionPlotFrame(dataX, dataY, isLineVisible, frameTitle, plotLabel, xAxisLabel,
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
		if (image != null) ij.ui().show(image);
		// execute the filter, waiting for the operation to finish.
		// ij.command().run(MethodHandles.lookup().lookupClass().getName(), true).get().getOutput("image");
		ij.command().run(MethodHandles.lookup().lookupClass().getName(), true);
	}
}
