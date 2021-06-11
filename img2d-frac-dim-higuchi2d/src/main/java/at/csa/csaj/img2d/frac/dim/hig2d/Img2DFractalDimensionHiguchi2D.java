/*-
 * #%L
 * Project: ImageJ plugin for computing fractal dimension with 2D Higuchi algorithms.
 * File: Img2DFractalDimensionHiguchi2D.java
 * 
 * $Id$
 * $HeadURL$
 * 
 * This file is part of ComsystanJ software, hereinafter referred to as "this program".
 * %%
 * Copyright (C) 2020 - 2021 Comsystan Software
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


package at.csa.csaj.img2d.frac.dim.hig2d;

import java.awt.Toolkit;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
import net.imglib2.view.Views;
import org.scijava.ItemIO;
import org.scijava.ItemVisibility;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
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

import at.csa.csaj.commons.dialog.WaitingDialogWithProgressBar;
import at.csa.csaj.commons.plot.RegressionPlotFrame;
import at.csa.csaj.commons.regression.LinearRegression;
import at.csa.csaj.img2d.frac.dim.hig2d.util.Higuchi2DMethods;
import at.csa.csaj.img2d.frac.dim.hig2d.util.Higuchi2D_Grey_DirDiff;
import at.csa.csaj.img2d.frac.dim.hig2d.util.Higuchi2D_Grey_KfoldDiff;
import at.csa.csaj.img2d.frac.dim.hig2d.util.Higuchi2D_Grey_MultDiff;
import at.csa.csaj.img2d.frac.dim.hig2d.util.Higuchi2D_Grey_MultDiff2;
import at.csa.csaj.img2d.frac.dim.hig2d.util.Higuchi2D_Grey_SqrDiff;
import at.csa.csaj.img2d.frac.dim.hig2d.util.Higuchi2D_Grey_TriangArea;
import at.csa.csaj.img2d.frac.dim.hig2d.util.Higuchi2D_RGBColWeight_NotReadyYet;
import at.csa.csaj.img2d.frac.dim.hig2d.util.Higuchi2D_RGBDiff_KfoldDiff;
import at.csa.csaj.img2d.frac.dim.hig2d.util.Higuchi2D_RGBDiff_MultDiff;
import at.csa.csaj.img2d.frac.dim.hig2d.util.Higuchi2D_RGBDiff_SqrDiff;
import at.csa.csaj.img2d.frac.dim.hig2d.util.Higuchi2D_RGBROIColWeight_NotReadyYet;
import at.csa.csaj.img2d.frac.dim.hig2d.util.Higuchi2D_RGB_KfoldDiff;
import at.csa.csaj.img2d.frac.dim.hig2d.util.Higuchi2D_RGB_MultDiff;
import at.csa.csaj.img2d.frac.dim.hig2d.util.Higuchi2D_RGB_SqrDiff;
import io.scif.DefaultImageMetadata;
import io.scif.MetaTable;

/**
 * A {@link Command} plugin computing <the 2D Higuchi dimension</a>
 * of an image.
 */
@Plugin(type = InteractiveCommand.class,
headless = true,
label = "Higuchi dimension 2D",
menu = {
@Menu(label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT, mnemonic = MenuConstants.PLUGINS_MNEMONIC),
@Menu(label = "ComsystanJ"),
@Menu(label = "Image (2D)"),
@Menu(label = "Higuchi dimension 2D", weight = 10)})
public class Img2DFractalDimensionHiguchi2D<T extends RealType<T>> extends InteractiveCommand implements Command, Previewable { // non blocking  GUI
//public class Img2DFractalDimensionHiguchi2D<T extends RealType<T>> implements Command {	//modal GUI

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
	private static boolean isGrey = true;
	private static boolean isRGB = false;
	private static long width = 0;
	private static long height = 0;
	private static int compChCount = 0; //1  Grey,   3 RGB
	private static long numDimensions = 0;
	private static long numSlices = 0;
	private static int  numbKMax = 0;
	private static ArrayList<RegressionPlotFrame> doubleLogPlotList = new ArrayList<RegressionPlotFrame>();
	private static double[][] resultValuesTable; // first column is the image index, second column are the corresponding regression values
	private static final String tableName = "Table - Higuchi2D dimension";
	
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

	@Parameter(type = ItemIO.OUTPUT)
	private DefaultGenericTable table;


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
	private int spinnerInteger_RegMax = 3;

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
			   persist = false, //restore previous value default = true
			   initializer = "initialMethod", callback = "callbackMethod")
	private String choiceRadioButt_Method;

	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelBackgroundOptions = BACKGROUNDOPTIONS_LABEL;
	
	@Parameter(label = "Skip zero values", persist = false,
		       callback = "callbackSkipZeroes")
	private boolean booleanSkipZeroes;
	
	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelDisplayOptions = DISPLAYOPTIONS_LABEL;

	@Parameter(label = "Show double log plot",
		   	   // persist = false, //restore previous value default = true
			   initializer = "initialShowDoubleLogPlots")
	private boolean booleanShowDoubleLogPlot;

	@Parameter(label = "Overwrite result display(s)",
	    	description = "Overwrite already existing result images, plots or tables",
	    	//persist  = false,  //restore previous value default = true
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
	
	@Parameter(label   = "   Process single image #    ",
		    	callback = "callbackProcessSingleImage")
	private Button buttonProcessSingelImage;
	
//	Deactivated, because it does not work in Fiji (although it works in ImageJ2 -Eclipse)	
//@Parameter(label   = "Process single active image ",
//		    callback = "callbackProcessActiveImage")
//	private Button buttonProcessActiveImage;

	@Parameter(label = "Process all available images",
				callback = "callbackProcessAllImages")
	private Button buttonProcessAllImages;
	
	// ---------------------------------------------------------------------
	// The following initialzer functions set initial values

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
		choiceRadioButt_Method = "K-fold differences";
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
	
	// The following method is known as "callback" which gets executed
	// whenever the value of a specific linked parameter changes.

	/** Executed whenever the {@link #spinInteger_KMax} parameter changes. */
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

	/** Executed whenever the {@link #spinInteger_RegMin} parameter changes. */
	protected void callbackRegMin() {
		if (spinnerInteger_RegMin >= spinnerInteger_RegMax - 2) {
			spinnerInteger_RegMin = spinnerInteger_RegMax - 2;
		}
		if (spinnerInteger_RegMin < 1) {
			spinnerInteger_RegMin = 1;
		}
		logService.info(this.getClass().getName() + " Regression Min set to " + spinnerInteger_RegMin);
	}

	/** Executed whenever the {@link #spinInteger_RegMax} parameter changes. */
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
	
	/** Executed whenever the {@link #spinInteger_NumImageSlice} parameter changes. */
	protected void callbackNumImageSlice() {
		getAndValidateActiveDataset();
		if (spinnerInteger_NumImageSlice > numSlices){
			logService.info(this.getClass().getName() + " No more images available");
			spinnerInteger_NumImageSlice = (int)numSlices;
		}
		logService.info(this.getClass().getName() + " Image slice number set to " + spinnerInteger_NumImageSlice);
	}
	

	/** Executed whenever the {@link #buttonProcessSingelImage} button is pressed. */
	protected void callbackProcessSingleImage() {
		//prepare  executer service
		exec = Executors.newSingleThreadExecutor();
		
		//dlgProgress = new WaitingDialogWithProgressBar("<html>Computing Higuchi2D dimensions, please wait...<br>Open console window for further info.</html>");
		dlgProgress = new WaitingDialogWithProgressBar("Computing Higuchi2D dimensions, please wait... Open console window for further info.",
																					logService, false, exec); //isCanceable = false, because no following method listens to exec.shutdown 
		dlgProgress.updatePercent("");
		dlgProgress.setBarIndeterminate(true);
		dlgProgress.setVisible(true);
		
    	exec.execute(new Runnable() {
            public void run() {
        	    try {
        	    	deleteExistingDisplays();
            		int validation = getAndValidateActiveDataset();
            		if (validation == 1){
            			int sliceIndex = spinnerInteger_NumImageSlice - 1;
            	        logService.info(this.getClass().getName() + " Processing single image " + (sliceIndex + 1));
	            		processActiveInputImage(sliceIndex);
	            		dlgProgress.addMessage("Processing finished! Collecting data for table...");
	            		generateTableHeader();
	            		collectActiveResultAndShowTable(sliceIndex);
            		}
            		dlgProgress.setVisible(false);
            		dlgProgress.dispose();
            		Toolkit.getDefaultToolkit().beep();
                } catch(InterruptedException e){
                	 exec.shutdown();
                } finally {
                	exec.shutdown();
                }		
            }
        });
	}
	
	/**
	 * Executed whenever the {@link #buttonProcessActiveImage} button is pressed.
	 */
	protected void callbackProcessActiveImage() {
		//prepare  executer service
		exec = Executors.newSingleThreadExecutor();
		
		//dlgProgress = new WaitingDialogWithProgressBar("<html>Computing Higuchi2D dimensions, please wait...<br>Open console window for further info.</html>");
		dlgProgress = new WaitingDialogWithProgressBar("Computing Higuchi2D dimensions, please wait... Open console window for further info.",
																					logService, false, exec); //isCanceable = false, because no following method listens to exec.shutdown 
		dlgProgress.updatePercent("");
		dlgProgress.setBarIndeterminate(true);
		dlgProgress.setVisible(true);
		
    	exec.execute(new Runnable() {
            public void run() {
        	    try {
        	    	deleteExistingDisplays();
            		int validation = getAndValidateActiveDataset();
            		if (validation == 1){
	            		int activeSliceIndex = getActiveImageIndex();
	            		logService.info(this.getClass().getName() + " Processing active image " + (activeSliceIndex + 1));
	            		processActiveInputImage(activeSliceIndex);
	            		dlgProgress.addMessage("Processing finished! Collecting data for table...");
	            		generateTableHeader();
	            		collectActiveResultAndShowTable(activeSliceIndex);
            		}
            		dlgProgress.setVisible(false);
            		dlgProgress.dispose();
            		Toolkit.getDefaultToolkit().beep();
                } catch(InterruptedException e){
                	 exec.shutdown();
                } finally {
                	exec.shutdown();
                }		
            }
        });
	}

	/**
	 * Executed whenever the {@link #buttonProcessAllImages} button is pressed. This
	 * is the main processing method usually implemented in the run() method for
	 */
	protected void callbackProcessAllImages() {
		//prepare  executer service
		exec = Executors.newSingleThreadExecutor();
		//exec =  defaultThreadService.getExecutorService();
		
		//dlgProgress = new WaitingDialogWithProgressBar("<html>Computing Higuchi2D dimensions, please wait...<br>Open console window for further info.</html>");
		dlgProgress = new WaitingDialogWithProgressBar("Computing Higuchi2D dimensions, please wait... Open console window for further info.",
																					logService, true, exec); //isCanceable = true, because processAllInputImages(dlgProgress) listens to exec.shutdown 
		dlgProgress.setVisible(true);

		exec.execute(new Runnable() {
            public void run() {	
            	try {
	            	logService.info(this.getClass().getName() + " Processing all available images");
	            	deleteExistingDisplays();
	        		int validation = getAndValidateActiveDataset();
	        		if (validation == 1){
		        		processAllInputImages();
		        		dlgProgress.addMessage("Processing finished! Collecting data for table...");
		        		generateTableHeader();
		        		collectAllResultsAndShowTable();
	        		}
	        		dlgProgress.setVisible(false);
	        		dlgProgress.dispose();
	        		Toolkit.getDefaultToolkit().beep();
            	} catch(InterruptedException e){
                    //Thread.currentThread().interrupt();
            		exec.shutdown();
                } finally {
                	exec.shutdown();
                }      	
            }
        });	
		
	}
	
	// You can control how previews work by overriding the "preview" method.
	// The code written in this method will be automatically executed every
	// time a widget value changes.
	public void preview() {
		logService.info(this.getClass().getName() + " Preview initiated");
		if (booleanProcessImmediately) callbackProcessSingleImage();
		// statusService.showStatus(message);
	}

	// This is often necessary, for example, if your "preview" method manipulates
	// data;
	// the "cancel" method will then need to revert any changes done by the previews
	// back to the original state.
	public void cancel() {
		logService.info(this.getClass().getName() + " Widget canceled");
	}
	// ---------------------------------------------------------------------------

	/** The run method executes the command. */
	@Override
	public void run() {
		// Nothing, because non blocking dialog has no automatic OK button and would
		// call this method twice during start up

		// ij.log().info( "Run" );
		logService.info(this.getClass().getName() + " Run");

		if (ij.ui().isHeadless()) {
			// execute();
			this.callbackProcessAllImages();
		}
	}

	public int getAndValidateActiveDataset() {

		datasetIn = imageDisplayService.getActiveDataset();

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
			return -1;  //1 OK
		}
		// get some info
		width = datasetIn.dimension(0);
		height = datasetIn.dimension(1);
		//depth = dataset.getDepth(); //does not work if third axis ist not specifyed as z-Axis
		numDimensions = datasetIn.numDimensions();
		compChCount = datasetIn.getImgPlus().getCompositeChannelCount(); //1  Grey,   3 RGB
		
		if ((numDimensions == 2) && (compChCount == 1)) { // single grey image
			numSlices = 1; 
			isGrey = true;
			isRGB = false;
		} else if ((numDimensions == 3) && (compChCount == 1)) { // Grey Image stack
			numSlices =datasetIn.dimension(2);
			isGrey = true;
			isRGB = false;
		} else if ((numDimensions == 3) && (compChCount == 3)) { // Single RGB Image
			numSlices = 1;
			isGrey = false;
			isRGB = true;
		} else if ((numDimensions == 4) && (compChCount == 3)) { // RGB Image stack 
			numSlices =datasetIn.dimension(3); //0 width, 1 height, 2 RGB channels, 3 stack
			isGrey = false;
			isRGB = true;
		}

		// get the name of dataset
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
         
		//Check if Image type and selected Method fits together 	
		if (isGrey) {// grey image
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
				
				return -1;
			}
		}	
		if (isRGB) {// RGB image
			//******************************************************************************************************
			if  ( (choiceRadioButt_Method.equals("K-fold differences"))
				||(choiceRadioButt_Method.equals("Multiplicated differences"))
				||(choiceRadioButt_Method.equals("Multiplicated differences2"))
				||(choiceRadioButt_Method.equals("Squared differences"))
				||(choiceRadioButt_Method.equals("Direct differences"))
				||(choiceRadioButt_Method.equals("Triangle areas")) )
			{
				Result result = uiService.showDialog("A grey value algorithm cannot be applied to RGB images!", "Alert", MessageType.WARNING_MESSAGE, OptionType.DEFAULT_OPTION);
				return - 1;
			}
		}
		
		logService.info(this.getClass().getName() + " Name: " + datasetName); 
		if (isGrey) logService.info(this.getClass().getName() + " Image type = Grey"); 
		if (isRGB)  logService.info(this.getClass().getName() + " Image type = RGB"); 
		logService.info(this.getClass().getName() + " Image size = " + width+"x"+height); 
		logService.info(this.getClass().getName() + " Number of images = "+ numSlices); 
		return 1;
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
			if (isGrey) {
				//do nothing, it is OK
			};
			if (isRGB) {
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
		boolean optDeleteExistingPlots  = false;
		boolean optDeleteExistingTables = false;
		boolean optDeleteExistingImgs   = false;
		if (booleanOverwriteDisplays) {
			optDeleteExistingPlots  = true;
			optDeleteExistingTables = true;
			optDeleteExistingImgs   = true;
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
			List<Display<?>> list = defaultDisplayService.getDisplays();
			for (int i = 0; i < list.size(); i++) {
				Display<?> display = list.get(i);
				//System.out.println("display name: " + display.getName());
				if (display.getName().equals(tableName))
					display.close();
			}
		}
	}

	/** This method takes the active image and computes results. 
	 *
	 **/
	private void processActiveInputImage (int s) throws InterruptedException {
		
		long startTime = System.currentTimeMillis();

		resultValuesTable = new double[(int) numSlices][10];
	
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
//		double[] resultValues = process(iv, s);
//		// 0 Dh-row, 1 R2-row, 2 StdErr-row, 3 Dh-col, 4 R2-col, 5 StdErr-col, 6 Dh, 7 R2, 8 Stderr
		
		//get rai
		RandomAccessibleInterval<?> rai = null;	
		if( (s==0) && (numSlices == 1) && (isGrey)) { // for a single grey 2D image;
			rai =  (RandomAccessibleInterval<?>) datasetIn.getImgPlus();

		} else if ( (numSlices > 1) && (isGrey)){ // for a stack of grey 2D images
			rai = (RandomAccessibleInterval<?>) Views.hyperSlice(datasetIn, 2, s);
		
		} else	if( (s==0) && (numSlices == 1) && (isRGB)) { // for a single RGB 2D image;
			rai =  (RandomAccessibleInterval<?>) datasetIn.getImgPlus();

		} else if ( (numSlices > 1) && (isRGB)){ // for a stack of RGB 2D images 
			rai = (RandomAccessibleInterval<?>) Views.hyperSlice(datasetIn, 3, s);  //width x height x 3 RGBChannels
		}

		// Compute regression parameters
		double[] regressionValues = process(rai, s); //rai is already 2D, s parameter only for display titles
		//0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared

		//set values for output table
		for (int i = 0; i < regressionValues.length; i++ ) {
			resultValuesTable[s][i] = regressionValues[i]; 
		}
		//Compute dimension
		double dim = 0.0;	
		dim = -regressionValues[1];
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
		resultValuesTable[s][1] = dim;
	
		long duration = System.currentTimeMillis() - startTime;
		TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
		SimpleDateFormat sdf = new SimpleDateFormat();
		sdf.applyPattern("HHH:mm:ss:SSS");
		logService.info(this.getClass().getName() + " Elapsed time: "+ sdf.format(duration));
	}

	/** This method loops over all input images and computes results. 
	 *
	 **/
	private void processAllInputImages() throws InterruptedException{
		
		long startTimeAll = System.currentTimeMillis();
		
		resultValuesTable = new double[(int) numSlices][10];

		// convert to float values
		// Img<T> image = (Img<T>) dataset.getImgPlus();
		// Img<FloatType> imgFloat; // =
		// opService.convert().float32((Img<T>)dataset.getImgPlus());

		// loop over all slices of stack
		for (int s = 0; s < numSlices; s++) { // p...planes of an image stack
			if (!exec.isShutdown()){
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
	//			double[] resultValues = process(iv, s);
	//			// 0 Dh, 1 R2, 2 Stderr
				
				
				//get rai
				RandomAccessibleInterval<?> rai = null;	
				if( (s==0) && (numSlices == 1) && (isGrey)) { // for a single grey 2D image;
					rai =  (RandomAccessibleInterval<?>) datasetIn.getImgPlus();

				} else if ( (numSlices > 1) && (isGrey)){ // for a stack of grey 2D images
					rai = (RandomAccessibleInterval<?>) Views.hyperSlice(datasetIn, 2, s);
				
				} else	if( (s==0) && (numSlices == 1) && (isRGB)) { // for a single RGB 2D image;
					rai =  (RandomAccessibleInterval<?>) datasetIn.getImgPlus();

				} else if ( (numSlices > 1) && (isRGB)){ // for a stack of RGB 2D images 
					rai = (RandomAccessibleInterval<?>) Views.hyperSlice(datasetIn, 3, s);  //width x height x 3 RGBChannels
				}
	
				// Compute regression parameters
				double[] regressionValues = process(rai, s); //rai is already 2D, s parameter only for display titles
				//0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
	
				//set values for output table
				for (int i = 0; i < regressionValues.length; i++ ) {
					resultValuesTable[s][i] = regressionValues[i]; 
				}
				//Compute dimension
				double dim = 0.0;	
				dim = -regressionValues[1];
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
				resultValuesTable[s][1] = dim;
				
				long duration = System.currentTimeMillis() - startTime;
				TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
				SimpleDateFormat sdf = new SimpleDateFormat();
				sdf.applyPattern("HHH:mm:ss:SSS");
				logService.info(this.getClass().getName() + " Elapsed time: "+ sdf.format(duration));
			}
		} //s	
		statusService.showProgress(0, 100);
		statusService.clearStatus();
		
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
		IntColumn columnRegMin           = new IntColumn("RegMin");
		IntColumn columnRegMax           = new IntColumn("RegMax");
		GenericColumn columnMethod       = new GenericColumn("Method");
		BoolColumn columnSkipZeroes      = new BoolColumn("Skip zeroes");
		DoubleColumn columnDh            = new DoubleColumn("Dh");
		DoubleColumn columnR2            = new DoubleColumn("R2");
		DoubleColumn columnStdErr        = new DoubleColumn("StdErr");

		table = new DefaultGenericTable();
		table.add(columnFileName);
		table.add(columnSliceName);
		table.add(columnKMax);
		table.add(columnRegMin);
		table.add(columnRegMax);
		table.add(columnMethod);
		table.add(columnSkipZeroes);
		table.add(columnDh);
		table.add(columnR2);
		table.add(columnStdErr);

	}

	/**
	 * collects current result and shows table
	 * 
	 * @param int slice number of active image.
	 */
	private void collectActiveResultAndShowTable(int sliceNumber) {

		int regMin = spinnerInteger_RegMin;
		int regMax = spinnerInteger_RegMax;
		int numKMax = spinnerInteger_KMax;

		int s = sliceNumber;
		// 0 Intercept, 1 Dim, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
		// fill table with values
		table.appendRow();
		table.set("File name",  table.getRowCount() - 1, datasetName);	
		if (sliceLabels != null) table.set("Slice name", table.getRowCount() - 1, sliceLabels[s]);
		table.set("k",           table.getRowCount() - 1, numKMax);
		table.set("RegMin",      table.getRowCount() - 1, regMin);
		table.set("RegMax",      table.getRowCount() - 1, regMax);
		table.set("Method",      table.getRowCount() - 1, choiceRadioButt_Method);
		table.set("Skip zeroes", table.getRowCount() - 1, booleanSkipZeroes);
		table.set("Dh",          table.getRowCount() - 1, resultValuesTable[s][1]);
		table.set("R2",          table.getRowCount() - 1, resultValuesTable[s][4]);
		table.set("StdErr",      table.getRowCount() - 1, resultValuesTable[s][3]);
	
		// Show table
		uiService.show(tableName, table);
	}

	/** collects all results and shows table */
	private void collectAllResultsAndShowTable() {

		int regMin = spinnerInteger_RegMin;
		int regMax = spinnerInteger_RegMax;
		int numKMax = spinnerInteger_KMax;

		// loop over all slices
		for (int s = 0; s < numSlices; s++) { // slices of an image stack
			// 0 Intercept, 1 Dim, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
			// fill table with values
			table.appendRow();
			table.set("File name",  table.getRowCount() - 1, datasetName);	
			if (sliceLabels != null) table.set("Slice name", table.getRowCount() - 1, sliceLabels[s]);
			table.set("k",           table.getRowCount() - 1, numKMax);
			table.set("RegMin",      table.getRowCount() - 1, regMin);
			table.set("RegMax",      table.getRowCount() - 1, regMax);
			table.set("Method",      table.getRowCount() - 1, choiceRadioButt_Method);
			table.set("Skip zeroes", table.getRowCount() - 1, booleanSkipZeroes);
			table.set("Dh",          table.getRowCount() - 1, resultValuesTable[s][1]);
			table.set("R2",          table.getRowCount() - 1, resultValuesTable[s][4]);
			table.set("StdErr",      table.getRowCount() - 1, resultValuesTable[s][3]);	
		}
		uiService.show(tableName, table);
		
	}

	/**
	*
	* Processing
	*/
	private double[] process(RandomAccessibleInterval<?> rai, int plane) { // plane plane (Image) number
	
		int regMin           = spinnerInteger_RegMin;
		int regMax           = spinnerInteger_RegMax;
		int numKMax          = spinnerInteger_KMax;
		boolean skipZeroes   = booleanSkipZeroes;
		boolean optShowPlot  = booleanShowDoubleLogPlot;

		long width  = rai.dimension(0);
		long height = rai.dimension(1);

		double[] regressionParams = null;
	
		String plot_method="Higuchi dimension 2D";
		String xAxis = "ln(eps)";
		String yAxis = "ln(Count)";
		
		Higuchi2DMethods hig2Method = null;
		
		if (isGrey) {// grey image   //additional check, is already checked during validation of active dataset
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
			
		} else if (isRGB) { // RGB image  //additional check, is already checked during validation of active dataset
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
	
		logService.info(this.getClass().getName() + " Higuchi2D");
		logService.info(this.getClass().getName() + " lnEps: \t  lnTotals:");	
		for (int i = 0; i < eps.length; i++) {
			lnEps[i]    = Math.log(eps[i]);
			lnTotals[i] = Math.log(totals[i]);
			logService.info(this.getClass().getName() + (String.valueOf(lnEps[i]) + "\t "   + String.valueOf(lnTotals[i])));
		}
		
		
		//Create double log plot
	
//		Vector<Double> dataX = new Vector<Double>();
//		Vector<Double> dataY = new Vector<Double>();		
//		for (int n = 0; n < eps.length; n++) {	
//			dataY.add(lnTotals[n]);
//			dataX.add(lnEps[n]);
//		}
		
		boolean isLineVisible = false; //?		
		if (optShowPlot) {
			if ((isGrey) || (isRGB)) { //both are OK
				String preName = "";
				if (numSlices > 1) {
					preName = "Slice-"+String.format("%03d", plane) +"-";
				}
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

//		double[] dataXArray = new double[dataX.size()];
//		double[] dataYArray = new double[dataY.size()];
//		for (int i = 0; i < dataX.size(); i++) {
//			dataXArray[i] = dataX.get(i).doubleValue();
//		}
//		for (int i = 0; i < dataY.size(); i++) {
//			dataYArray[i] = dataY.get(i).doubleValue();
//		}

		regressionParams = lr.calculateParameters(lnEps, lnTotals, regMin, regMax);
		//0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
			
		return regressionParams;
		// Output
		// uiService.show("Table - Higuchi dimension", table);
		// result = ops.create().img(image, new FloatType());
		// table
	}

	// This method shows the double log plot
	private void showPlot(double[] lnDataX, double[] lnDataY, String preName, int plane, int regMin, int regMax) {
		if (isGrey) {
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
				preName = "Slice" + String.format("%03d", plane) + "-";
			} else {
				preName = preName + String.format("%03d", plane) + "-";
			}
			boolean isLineVisible = false; // ?
			RegressionPlotFrame doubleLogPlot = DisplayRegressionPlotXY(lnDataX, lnDataY, isLineVisible,
					"Double Log Plot - Higuchi Dimension", preName + datasetName, "ln(k)", "ln(L)", "", regMin, regMax);
			doubleLogPlotList.add(doubleLogPlot);
		}
		if (!isGrey) {

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
		// ij.command().run(Img2DFractalDimensionHiguchi2D.class,
		// true).get().getOutput("image");
		ij.command().run(Img2DFractalDimensionHiguchi2D.class, true);
	}
}
