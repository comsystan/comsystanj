/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj1DHurstPSDCmd.java
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
package at.csa.csaj.plugin1d.frac;

import java.awt.Toolkit;
import java.lang.invoke.MethodHandles;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import net.imagej.ImageJ;
import net.imagej.ops.OpService;
import net.imglib2.type.numeric.RealType;

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
import org.scijava.table.Column;
import org.scijava.table.DefaultGenericTable;
import org.scijava.table.DefaultTableDisplay;
import org.scijava.table.DoubleColumn;
import org.scijava.table.GenericColumn;
import org.scijava.table.IntColumn;
import org.scijava.ui.UIService;
import org.scijava.widget.Button;
import org.scijava.widget.ChoiceWidget;
import org.scijava.widget.NumberWidget;

import at.csa.csaj.commons.CsajAlgorithm_Surrogate1D;
import at.csa.csaj.commons.CsajCheck_ItemIn;
import at.csa.csaj.commons.CsajDialog_WaitingWithProgressBar;
import at.csa.csaj.commons.CsajPlot_RegressionFrame;
import at.csa.csaj.commons.CsajContainer_ProcessMethod;
import at.csa.csaj.plugin1d.frac.util.BetaDispH;
import at.csa.csaj.plugin1d.frac.util.BetaPSD;
import at.csa.csaj.plugin1d.frac.util.BetaSWVH;
import at.csa.csaj.plugin1d.misc.Csaj1DOpenerCmd;

/**
 * A {@link ContextCommand} plugin computing <the Hurst coefficient using power spectral densities</a>
* of a sequence.
 */

//According to:
//Eke et al., 2000, Pflugers Archiv-European Journal of Physiology, 
//https://doi.org/10.1007/s004249900135

@Plugin(type = ContextCommand.class, 
		headless = true,
		label = "Hurst coefficient (PSD)",
		initializer = "initialPluginLaunch",
		iconPath = "/icons/comsystan-logo-grey46-16x16.png", //Menu entry icon
		menu = {}) //Space at the end of the label is necessary to avoid duplicate with 2D plugin 

public class Csaj1DHurstPSDCmd<T extends RealType<T>> extends ContextCommand implements Previewable {

	private static final String PLUGIN_LABEL            = "<html><b>Hurst coefficient (PSD)</b></html>";
	private static final String SPACE_LABEL             = "";
	private static final String HURSTOPTIONS_LABEL      = "<html><b>Hurst options</b></html>";
	private static final String REGRESSION_LABEL        = "<html><b>Fractal regression parameters</b></html>";
	private static final String ANALYSISOPTIONS_LABEL   = "<html><b>Analysis options</b></html>";
	private static final String BACKGROUNDOPTIONS_LABEL = "<html><b>Background option</b></html>";
	private static final String DISPLAYOPTIONS_LABEL    = "<html><b>Display options</b></html>";
	private static final String PROCESSOPTIONS_LABEL    = "<html><b>Process options</b></html>";
	
	private static double[] sequence1D;
	private static double[] domain1D;
	private static double[] subSequence1D;
	private static double[] surrSequence1D;
	Column<? extends Object> sequenceColumn;
	
	private static String tableInName;
	private static String[] columnLabels;
	private static long numColumns = 0;
	private static long numRows = 0;
	private static long numDimensions = 0;
	private static int  numSurrogates = 0;
	private static int  numBoxLength = 0;
	private static long numSubsequentBoxes = 0;
	private static long numGlidingBoxes = 0;
	
	private static double[] sequenceNew;
	private static double[] sequenceSSC;
	private static int numRegStart;
	private static int numRegEnd;
	private static String  sequenceType = "?";
	
	double psd_Beta    = Double.NaN;
	double psd_H       = Double.NaN;
	double psd_r2      = Double.NaN;
	double psd_stdErr  = Double.NaN;

	double ssc_Beta    = Double.NaN;
	double ssc_r2      = Double.NaN;
	double ssc_stdErr  = Double.NaN;

	double disp_Beta   = Double.NaN;
	double disp_H 	   = Double.NaN;
	double disp_r2     = Double.NaN;
	double disp_stdErr = Double.NaN;
	
	double swv_Beta    = Double.NaN;
	double swv_H 	   = Double.NaN;
	double swv_r2      = Double.NaN;
	double swv_stdErr  = Double.NaN;
	
	BetaPSD betaPSD;
	BetaSWVH betaSWVH;
	BetaDispH betaDispH;
	double[] resultPSD;
	double[] resultSSC;
	double[] resultDispH;
	double[] resultSWVH;
	
	private static ArrayList<CsajPlot_RegressionFrame> doubleLogPlotList = new ArrayList<CsajPlot_RegressionFrame>();
	
	public static final String TABLE_OUT_NAME = "Table - Hurst (PSD)";
	
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


	//@Parameter
	//private DefaultThreadService defaultThreadService;

	// This parameter does not work in an InteractiveCommand plugin
	// -->> (duplicate displayService error during startup) pom-scijava 24.0.0
	// no problem in a Command Plugin
	//@Parameter
	//private DisplayService displayService;

	@Parameter // This works in an InteractiveCommand plugin
	private DefaultDisplayService defaultDisplayService;

	//@Parameter(type = ItemIO.INPUT)
	private DefaultGenericTable tableIn;
	

	@Parameter(label = TABLE_OUT_NAME, type = ItemIO.OUTPUT)
	private DefaultGenericTable tableOut;


	// Widget elements------------------------------------------------------

	//@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	//private final String labelPlugin = PLUGIN_LABEL;

	//@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	//private final String labelSpace = SPACE_LABEL;
	
	@Parameter(type = ItemIO.INPUT)
	private DefaultTableDisplay  defaultTableDisplay;

	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelHurstOptions = HURSTOPTIONS_LABEL;
	
	@Parameter(label = "PSD option",
			   description = "Power spectral density or low frequency PSD with parabolic windowing and end matching",
			   style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
			   //PSD: power spectrum density without any improvements
			   //lowPSDwe: power spectrum density using only lower frequencies (low), parabolic windowing (w) and end matching (e) (bridge detrending)
			   choices = {"PSD", "lowPSDwe"}, 
			   persist = true,  //restore previous value default = true
			   initializer = "initialPSDType",
			   callback = "callbackPSDType")
	private String choiceRadioButt_PSDType;

//	//-----------------------------------------------------------------------------------------------------
//	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
//	private final String labelRegression = REGRESSION_LABEL;

	@Parameter(label = "(PSD) Regression Min",
			   description = "Minimum x value of linear regression",
			   style = NumberWidget.SPINNER_STYLE,
			   min = "1",
			   max = "9999999999999999999",
			   stepSize = "1",
			   persist = false, //restore previous value default = true
			   initializer = "initialNumRegStart", callback = "callbackNumRegStart")
	private int spinnerInteger_NumRegStart = 1;

	@Parameter(label = "(PSD) Regression Max",
			   description = "Maximum x value of linear regression",
			   style = NumberWidget.SPINNER_STYLE,
			   min = "3",
			   max = "9999999999999999999",
			   stepSize = "1",
			   persist = false, //restore previous value default = true
			   initializer = "initialNumRegEnd", callback = "callbackNumRegEnd")
	private int spinnerInteger_NumRegEnd = 3;
	
	@Parameter(label = "SWV option",
			   description = "Scaled window variance or bridge detrending SWV",
			   style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
			   //SWV: scaled windowed variance (mean of SD's)
			   //bdSWV: scaled windowed variance (mean of SD's) with bridge detrending
			   choices = {"SWV", "bdSWV"}, 
			   persist = true,  //restore previous value default = true
			   initializer = "initialSWVType",
			   callback = "callbackSWVType")
	private String choiceRadioButt_SWVType;
	
	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelAnalysisOptions = ANALYSISOPTIONS_LABEL;

	@Parameter(label = "Sequence range",
			   description = "Entire sequence, Subsequent boxes or Gliding box",
			   style = ChoiceWidget.LIST_BOX_STYLE,
			   choices = {"Entire sequence", "Subsequent boxes", "Gliding box"}, 
			   persist = true,  //restore previous value default = true
			   initializer = "initialSequenceRange",
			   callback = "callbackSequenceRange")
	private String choiceRadioButt_SequenceRange;
	
	@Parameter(label = "(Entire sequence) Surrogates",
			   description = "Surrogates types - Only for Entire sequence type!",
			   style = ChoiceWidget.LIST_BOX_STYLE,
			   choices = {"No surrogates", "Shuffle", "Gaussian", "Random phase", "AAFT"}, 
			   persist = true,  //restore previous value default = true
			   initializer = "initialSurrogateType",
			   callback = "callbackSurrogateType")
	private String choiceRadioButt_SurrogateType;
	
	@Parameter(label = "Surrogates #",
			   description = "Number of computed surrogates",
			   style = NumberWidget.SPINNER_STYLE, 
			   min = "1",
			   max = "9999999999999999999",
			   stepSize = "1",
			   persist = true, // restore  previous value  default  =  true
			   initializer = "initialNumSurrogates",
			   callback = "callbackNumSurrogates")
	private int spinnerInteger_NumSurrogates;
	
	@Parameter(label = "Box length",
			   description = "Length of subsequent or gliding box",
			   style = NumberWidget.SPINNER_STYLE, 
			   min = "2",
			   max = "9999999999999999999",
			   stepSize = "1",
			   persist = true, // restore  previous value  default  =  true
			   initializer = "initialBoxLength",
			   callback = "callbackBoxLength")
	private int spinnerInteger_BoxLength;
	
	@Parameter(label = "(Surr/Box) Hurst type",
			   description = "Hurst type for Surrogates, Subsequent boxes or Gliding box",
			   style = ChoiceWidget.LIST_BOX_STYLE,
			   choices = {"PSD_Beta"},   //may be later  "DISP", "SWV"}, //"PSD", "DISP", "SWV"
			   persist = true,  //restore previous value default = true
			   initializer = "initialSurrBoxHurstType",
			   callback = "callbackSurrBoxHurstType")
	private String choiceRadioButt_SurrBoxHurstType;
	
	//-----------------------------------------------------------------------------------------------------
//	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
//	private final String labelBackgroundOptions = BACKGROUNDOPTIONS_LABEL;

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
	private final String labelProcess = PROCESSOPTIONS_LABEL;

	@Parameter(label = "Immediate processing", visibility = ItemVisibility.INVISIBLE, persist = false,
	    	description = "Immediate processing when a parameter is changed",
			callback = "callbackProcessImmediately")
	private boolean booleanProcessImmediately;
	
	@Parameter(label = "OK - process column #", description = "column number", style = NumberWidget.SPINNER_STYLE, min = "1", max = "1000", stepSize = "1",
			   persist = false, // restore  previous value  default  =  true
			   initializer = "initialNumColumn", callback = "callbackNumColumn")
	private int spinnerInteger_NumColumn;

	@Parameter(label = "OK - process all columns",
			   description = "Set for final Command.run execution",
			   persist = false, // restore  previous value  default  =  true
			   initializer = "initialProcessAll")
	private boolean processAll;
	
	@Parameter(label = "Preview of single column #", callback = "callbackProcessSingleColumn")
	private Button buttonProcessSingleColumn;

	@Parameter(label = "Preview of all columns", callback = "callbackProcessAllColumns")
	private Button buttonProcessAllColumns;


	// ---------------------------------------------------------------------
		
	protected void initialPluginLaunch() {
		checkItemIOIn();
	}
	protected void initialPSDType() {
		choiceRadioButt_PSDType = "lowPSDwe";
	}
	protected void initialNumRegStart() {
		spinnerInteger_NumRegStart = 1;
		
//		this.getAndValidateActiveDataset();
//		if (numRows > 0){
//			int poweSpecLength = (int)numRows/2;
//			if (choiceRadioButt_PSDType.equals("PSD")){	
//				//set regression
//				numRegStart = 1;
//			}
//			else if (choiceRadioButt_PSDType.equals("lowPSDwe")){
//				//restrict regression	
//				numRegStart = poweSpecLength/8;
//			}	
//			this.spinnerInteger_NumRegStart = numRegStart;
//			this.callbackNumRegStart();
//		}
	}
	protected void initialNumRegEnd() {
//		int numMax = 0;
//		try {
//			numMax = (int) Math.floor((double)tableIn.getRowCount() / 3.0);
//		} catch (NullPointerException npe) {
//			logService.error(this.getClass().getName() + " ERROR: NullPointerException, input table = null");
//			cancel("ComsystanJ 1D plugin cannot be started - missing input table.");;
//		}
		spinnerInteger_NumRegEnd = 8;
		
//		this.getAndValidateActiveDataset();
//		if (numRows > 0){
//			int poweSpecLength = (int)numRows/2;
//			if (choiceRadioButt_PSDType.equals("PSD")){	
//				//set regression
//				numRegEnd = poweSpecLength;
//			}
//			else if (choiceRadioButt_PSDType.equals("lowPSDwe")){
//				//restrict regression	
//				numRegEnd = poweSpecLength/2;		
//			}	
//			this.spinnerInteger_NumRegEnd = numRegEnd;
//			this.callbackNumRegEnd();	
//		}
		
	}
	protected void initialSWVType() {
		choiceRadioButt_SWVType = "bdSWV";
	}
	protected void initialSequenceRange() {
		choiceRadioButt_SequenceRange = "Entire sequence";
	} 
	protected void initialSurrogateType() {
		choiceRadioButt_SurrogateType = "No surrogates";
	} 
	protected void initialNumSurrogates() {
		numSurrogates = 10;
		spinnerInteger_NumSurrogates = numSurrogates;
	}
	protected void initialBoxLength() {
		numBoxLength = 100;
		spinnerInteger_BoxLength =  (int) numBoxLength;
		numSubsequentBoxes = (long) Math.floor((double)numRows/(double)spinnerInteger_BoxLength);
		numGlidingBoxes = numRows - spinnerInteger_BoxLength + 1;
	}
	protected void initialSurrBoxHurstType() {
		this.choiceRadioButt_SurrBoxHurstType = "PSD";
	}
	protected void initialSkipZeroes() {
		booleanSkipZeroes = false;
	}	
	protected void initialShowDoubleLogPlots() {
		booleanShowDoubleLogPlot = true;
	}
	protected void initialOverwriteDisplays() {
    	booleanOverwriteDisplays = true;
	}
	protected void initialNumColumn() {
		spinnerInteger_NumColumn = 1;
	}

	// ------------------------------------------------------------------------------
	

	
	/** Executed whenever the {@link #spinnerInteger_PSDType} parameter changes. */
	protected void callbackPSDType() {
		logService.info(this.getClass().getName() + " PSD type set to " + choiceRadioButt_PSDType);
		if (numRows > 0){
			int poweSpecLength = (int)numRows/2;
			if (choiceRadioButt_PSDType.equals("PSD")){	
				//set regression
				numRegStart = 1;
				numRegEnd = poweSpecLength;
			}
			else if (choiceRadioButt_PSDType.equals("lowPSDwe")){
				//restrict regression	
				numRegStart = poweSpecLength/8;
				numRegEnd = poweSpecLength/2;		
			}	
			this.spinnerInteger_NumRegStart = numRegStart;
			this.spinnerInteger_NumRegEnd = numRegEnd;
			this.callbackNumRegStart();
			this.callbackNumRegEnd();	
		}
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
		logService.info(this.getClass().getName() + " Regression Max set  to " + spinnerInteger_NumRegEnd);
	}
	
	/** Executed whenever the {@link #spinnerInteger_SWVType} parameter changes. */
	protected void callbackSWVType() {
		logService.info(this.getClass().getName() + " SWV type set to " + choiceRadioButt_SWVType);
	}
	
	/** Executed whenever the {@link #choiceRadioButt_SequenceRange} parameter changes. */
	protected void callbackSequenceRange() {
		logService.info(this.getClass().getName() + " Sequence range set to " + choiceRadioButt_SequenceRange);
		if (!choiceRadioButt_SequenceRange.equals("Entire sequence")){
			choiceRadioButt_SurrogateType = "No surrogates";
			callbackSurrogateType();
		}
	}
	
	/** Executed whenever the {@link #choiceRadioButt_SurrogateType} parameter changes. */
	protected void callbackSurrogateType() {	
		if (!choiceRadioButt_SequenceRange.equals("Entire sequence")){
			choiceRadioButt_SurrogateType = "No surrogates";
			logService.info(this.getClass().getName() + " Surrogates not allowed for subsequent or gliding boxes!");
		}	
		logService.info(this.getClass().getName() + " Surrogate type set to " + choiceRadioButt_SurrogateType);
	}
	
	/** Executed whenever the {@link #spinnerInteger_NumSurrogates} parameter changes. */
	protected void callbackNumSurrogates() {
		numSurrogates = spinnerInteger_NumSurrogates;
		logService.info(this.getClass().getName() + " Number of surrogates set to " + spinnerInteger_NumSurrogates);
	}
	
	/** Executed whenever the {@link #spinnerInteger_BoxLength} parameter changes. */
	protected void callbackBoxLength() {
		numBoxLength = spinnerInteger_BoxLength;
		numSubsequentBoxes = (long) Math.floor((double)numRows/(double)spinnerInteger_BoxLength);
		numGlidingBoxes = numRows - spinnerInteger_BoxLength + 1;
		logService.info(this.getClass().getName() + " Box length set to " + spinnerInteger_BoxLength);
	}
	
	/** Executed whenever the {@link #choiceRadioButt_SurrBoxHurstType} parameter changes. */
	protected void callbackSurrBoxHurstType() {
		logService.info(this.getClass().getName() + " Hurst type for surrogate or box set to " + choiceRadioButt_SurrBoxHurstType);
	}

	/** Executed whenever the {@link #booleanSkipZeroes} parameter changes. */
	protected void callbackSkipZeroes() {
		logService.info(this.getClass().getName() + " Skip zeroes set to " + booleanSkipZeroes);
	}

	/** Executed whenever the {@link #booleanProcessImmediately} parameter changes. */
	protected void callbackProcessImmediately() {
		logService.info(this.getClass().getName() + " Process immediately set to " + booleanProcessImmediately);
	}
	
	/** Executed whenever the {@link #spinnerInteger_NumColumn} parameter changes. */
	protected void callbackNumColumn() {
		if (spinnerInteger_NumColumn > tableIn.getColumnCount()){
			logService.info(this.getClass().getName() + " No more columns available");
			spinnerInteger_NumColumn = tableIn.getColumnCount();
		}
		logService.info(this.getClass().getName() + " Column number set to " + spinnerInteger_NumColumn);
	}
	
	/**
	 * Executed whenever the {@link #buttonProcessSingleColumn} button is pressed.
	 * It is not executed in the same exact manner such as run()
	 * So a thread for displaying properly the Progressbar window is needed
	 * Execution of the code is then not on the Event Dispatch Thread EDT, where all GUI windows are executed
	 * The @Parameter ItemIO.OUTPUT is not automatically shown 
	 */
	protected void callbackProcessSingleColumn() {
		//prepare  executer service
		exec = Executors.newSingleThreadExecutor();
	   	exec.execute(new Runnable() {
	        public void run() {
	    	    startWorkflowForSingleColumn();
	    	   	uiService.show(TABLE_OUT_NAME, tableOut);
	        }
	    });
	   	exec.shutdown(); //No new tasks
	}
	
	/** Executed whenever the {@link #buttonProcessActiveColumn} button is pressed.*/
	protected void callbackProcessActiveColumn() {
	
	}
	
	/**
	 * Executed whenever the {@link #buttonProcessAllColumns} button is pressed.
	 * It is not executed in the same exact manner such as run()
	 * So a thread for displaying properly the Progressbar window is needed
	 * Execution of the code is then not on the Event Dispatch Thread EDT, where all GUI windows are executed
	 * The @Parameter ItemIO.OUTPUT is not automatically shown 
	 */
	protected void callbackProcessAllColumns() {
		//prepare  executer service
		exec = Executors.newSingleThreadExecutor();
	   	exec.execute(new Runnable() {
	        public void run() {
	        	startWorkflowForAllColumns();
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
		    	    startWorkflowForSingleColumn();
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
		if (processAll) startWorkflowForAllColumns();
		else			startWorkflowForSingleColumn();

		logService.info(this.getClass().getName() + " Finished command run");
	}

	public void checkItemIOIn() {

		//Check input and get input meta data
		HashMap<String, Object> datasetInInfo = CsajCheck_ItemIn.checkTableIn(logService, defaultTableDisplay);
		if (datasetInInfo == null) {
			logService.error(MethodHandles.lookup().lookupClass().getName() + " ERROR: Inital check failed");
			cancel("ComsystanJ 1D plugin cannot be started - Initial check failed.");
		} else {
			tableIn =      (DefaultGenericTable)datasetInInfo.get("tableIn");
			tableInName =  (String)datasetInInfo.get("tableInName"); 
			numColumns  =  (int)datasetInInfo.get("numColumns");
			numRows =      (int)datasetInInfo.get("numRows");
			columnLabels = (String[])datasetInInfo.get("columnLabels");

			numSurrogates = spinnerInteger_NumSurrogates;
			numBoxLength  = spinnerInteger_BoxLength;
			numSubsequentBoxes = (long)Math.floor((double)numRows/(double)spinnerInteger_BoxLength);
			numGlidingBoxes    = numRows - spinnerInteger_BoxLength + 1;
					
			//Set additional plugin specific values****************************************************
			
			//*****************************************************************************************
		}
	}
	
	/**
	* This method starts the workflow for a single column of the active display
	*/
	protected void startWorkflowForSingleColumn() {
		
		dlgProgress = new CsajDialog_WaitingWithProgressBar("Computing Hurst value, please wait... Open console window for further info.",
							logService, false, exec); //isCanceable = false, because no following method listens to exec.shutdown 
		dlgProgress.updatePercent("");
		dlgProgress.setBarIndeterminate(true);
		dlgProgress.setVisible(true);

		logService.info(this.getClass().getName() + " Processing single sequence");
		deleteExistingDisplays();
		generateTableHeader();
		if (spinnerInteger_NumColumn <= numColumns) processSingleInputColumn(spinnerInteger_NumColumn - 1);
		dlgProgress.addMessage("Processing finished! Preparing result table...");		
		dlgProgress.setVisible(false);
		dlgProgress.dispose();
		Toolkit.getDefaultToolkit().beep();
	}

	/**
	* This method starts the workflow for all columns of the active display
	*/
	protected void startWorkflowForAllColumns() {
		
		dlgProgress = new CsajDialog_WaitingWithProgressBar("Computing Hurst values, please wait... Open console window for further info.",
							logService, false, exec); //isCanceable = true, because processAllInputSequencess(dlgProgress) listens to exec.shutdown 
		dlgProgress.setVisible(true);

    	logService.info(this.getClass().getName() + " Processing all available columns");
 		deleteExistingDisplays();
		generateTableHeader();
		processAllInputColumns();
		dlgProgress.addMessage("Processing finished! Preparing result table...");
		dlgProgress.setVisible(false);
		dlgProgress.dispose();
		Toolkit.getDefaultToolkit().beep();
	}

	/**
	 * This methods gets the index of the active column in the table
	 * @return int index
	 */
	private int getActiveColumnIndex() {
		int activeColumnIndex = 0;
		try {
			//This works in eclipse but not as jar in the plugin folder of fiji 
			//SCIFIO activated: throws a NullPointerException
			//SCIFIO deactivated: gives always back index = 0! 
			
			//TO DOxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
			//Position pos = imageDisplayService.getActivePosition;
			//activeColumnIndex = (int) pos.getIndex();
			//xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
			
			//This gives always back 0, SCIFIO setting does not matter
			//int activeSliceNumber = (int) imageDisplayService.getActiveImageDisplay().getActiveView().getPlanePosition().getIndex();
			//???
			//int activeSliceNumber = (int) defaultImageDisplayService.getActivePosition().getIndex(); 
			//int activeSliceNumber2 = (int) defaultImageDisplayService.getActiveImageDisplay().getActiveView().getPlanePosition().getIndex();
		} catch (NullPointerException npe) {
			// TODO Auto-generated catch block
			//npe.printStackTrace();
			logService.info(this.getClass().getName() + " WARNING: It was not possible to get active column index. Index set to first column.");
			activeColumnIndex = 0;
		} 
		logService.info(this.getClass().getName() + " Active slice index = " + activeColumnIndex);
		//logService.info(this.getClass().getName() + " Active slice index alternative = " + activeSliceNumber2);
		return activeColumnIndex;
	}
	
	/** Generates the table header {@code DefaultGenericTable} */
	private void generateTableHeader() {
		
		tableOut = new DefaultGenericTable();
		tableOut.add(new GenericColumn("File name"));
		tableOut.add(new GenericColumn("Column name"));	
		tableOut.add(new GenericColumn("Sequence range"));
		tableOut.add(new GenericColumn("Surrogate type"));
		tableOut.add(new IntColumn("# Surrogates"));
		tableOut.add(new IntColumn("Box length"));
		tableOut.add(new BoolColumn("Skip zeroes"));
		
		tableOut.add(new GenericColumn("Reg Start"));
		tableOut.add(new GenericColumn("Reg End"));
	
		//"Entire sequence", "Subsequent boxes", "Gliding box" 
		if (choiceRadioButt_SequenceRange.equals("Entire sequence")){
			tableOut.add(new GenericColumn("Sequence type"));
	 
			tableOut.add(new DoubleColumn(choiceRadioButt_PSDType+"_Beta"));	
			tableOut.add(new DoubleColumn(choiceRadioButt_PSDType+"_H"));	
			tableOut.add(new DoubleColumn(choiceRadioButt_PSDType+"_R2"));	
			tableOut.add(new DoubleColumn(choiceRadioButt_PSDType+"_StdErr"));	
			
			tableOut.add(new DoubleColumn("SSC_Beta"));	
			tableOut.add(new DoubleColumn("SSC_R2"));	
			tableOut.add(new DoubleColumn("SSC_StdErr"));	
			
			tableOut.add(new DoubleColumn("Disp_Beta"));	
			tableOut.add(new DoubleColumn("Disp_H"));		
			tableOut.add(new DoubleColumn("Disp_R2"));
			tableOut.add(new DoubleColumn("Disp_StdErr"));
			
			tableOut.add(new DoubleColumn(choiceRadioButt_SWVType+"_Beta"));	
			tableOut.add(new DoubleColumn(choiceRadioButt_SWVType+"_H"));	
			tableOut.add(new DoubleColumn(choiceRadioButt_SWVType+"_R2"));	
			tableOut.add(new DoubleColumn(choiceRadioButt_SWVType+"_StdErr"));	
			
			if (choiceRadioButt_SurrogateType.equals("No surrogates")) {
				//do nothing	
			} else { //Surrogates
				tableOut.add(new DoubleColumn(choiceRadioButt_SurrBoxHurstType+"_Beta_Surr")); //Mean surrogate value	
				tableOut.add(new DoubleColumn(choiceRadioButt_SurrBoxHurstType+"R2_Surr"));    //Mean surrogate value
				for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn(choiceRadioButt_SurrBoxHurstType+"_Beta_Surr-#"+(s+1))); 
				for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("R2_Surr-#"+(s+1))); 
			}	
		} 
		else if (choiceRadioButt_SequenceRange.equals("Subsequent boxes")){
			for (int n = 1; n <= numSubsequentBoxes; n++) {
				tableOut.add(new DoubleColumn(choiceRadioButt_SurrBoxHurstType+"_Beta-#" + n));	
			}
			for (int n = 1; n <= numSubsequentBoxes; n++) {
				tableOut.add(new DoubleColumn("R2-#" + n));	
			}
		}
		else if (choiceRadioButt_SequenceRange.equals("Gliding box")){
			for (int n = 1; n <= numGlidingBoxes; n++) {
				tableOut.add(new DoubleColumn(choiceRadioButt_SurrBoxHurstType+"_Beta-#" + n));	
			}
			for (int n = 1; n <= numGlidingBoxes; n++) {
				tableOut.add(new DoubleColumn("R2-#" + n));	
			}
		}	
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
				if (display.getName().contains(TABLE_OUT_NAME))
					display.close();
			}
		}
	}

  	/** 
	 * This method takes the single column c and computes results. 
	 * @Param int c
	 * */
	private void processSingleInputColumn (int c) {
		
		long startTime = System.currentTimeMillis();
		
		// Compute result values
		CsajContainer_ProcessMethod containerPM = process(tableIn, c); 
		// 0 D, 1 R2, 2 StdErr
		logService.info(this.getClass().getName() + " psd H: " + containerPM.item1_Values[1]);
		logService.info(this.getClass().getName() + " Processing finished.");
		writeToTable(0, c, containerPM); //write always to the first row
		
		long duration = System.currentTimeMillis() - startTime;
		TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
		SimpleDateFormat sdf = new SimpleDateFormat();
		sdf.applyPattern("HHH:mm:ss:SSS");
		logService.info(this.getClass().getName() + " Elapsed time: "+ sdf.format(duration));
	}

	/** This method loops over all input columns and computes results. 
	 * @param dlgProgress */
	private void processAllInputColumns() {
		
		long startTimeAll = System.currentTimeMillis();
		
		CsajContainer_ProcessMethod containerPM;
		// loop over all slices of stack
		for (int s = 0; s < numColumns; s++) { // s... number of sequence column
			//if (!exec.isShutdown()) {
				int percent = (int)Math.round((  ((float)s)/((float)numColumns)   *100.f   ));
				dlgProgress.updatePercent(String.valueOf(percent+"%"));
				dlgProgress.updateBar(percent);
				//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
				statusService.showStatus((s+1), (int)numColumns, "Processing " + (s+1) + "/" + (int)numColumns);
	
				long startTime = System.currentTimeMillis();
				logService.info(this.getClass().getName() + " Processing sequence column number " + (s+1) + "(" + numColumns + ")");
				
				// Compute result values
				containerPM = process(tableIn, s);
				// 0 Dh, 1 R2, 2 StdErr
				logService.info(this.getClass().getName() + " Processing finished.");
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
		
		long duration = System.currentTimeMillis() - startTimeAll;
		TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
		SimpleDateFormat sdf = new SimpleDateFormat();
		sdf.applyPattern("HHH:mm:ss:SSS");
		logService.info(this.getClass().getName() + " Elapsed processing time for all sequence(s): "+ sdf.format(duration));
	}
	
	/**
	 * collects current result and writes to table
	 * 
	 * @param int numRow to write in the result table
	 * @param in sequenceNumber column number of sequence from tableIn.
	 * @param CsajContainer_ProcessMethod containerPM
	 */
	private void writeToTable(int numRow, int sequenceNumber, CsajContainer_ProcessMethod containerPM) {
		logService.info(this.getClass().getName() + " Writing to the table...");
		int row = numRow;
		int tableColStart = 0;
		int tableColEnd   = 0;
		int tableColLast  = 0;
		
		// 0 Dh, 1 R2, 2 StdErr
		// fill table with values
		tableOut.appendRow();
		tableOut.set(0, row, tableInName);//File Name
		if (columnLabels != null)  tableOut.set(1, row, tableIn.getColumnHeader(sequenceNumber)); //Column Name
		tableOut.set(2, row, choiceRadioButt_SequenceRange);  //Sequence range
		tableOut.set(3, row, choiceRadioButt_SurrogateType); //Surrogate Method
		if (choiceRadioButt_SequenceRange.equals("Entire sequence") && (!choiceRadioButt_SurrogateType.equals("No surrogates"))) {
			tableOut.set(4, row, spinnerInteger_NumSurrogates); //# Surrogates
		} else {
			tableOut.set(4, row, null); //# Surrogates
		}
		if (!choiceRadioButt_SequenceRange.equals("Entire sequence")){
			tableOut.set(5, row, spinnerInteger_BoxLength); //Box Length
		} else {
			tableOut.set(5, row, null);
		}	
		tableOut.set(6, row, booleanSkipZeroes); //Zeroes removed
		tableOut.set(7, row, numRegStart); //may be changed by the algorithm, particularly by lowPSD
		tableOut.set(8, row, numRegEnd); //may be changed by the algorithm, particularly by lowPSD
	
		tableColLast = 8;
		
		if (containerPM == null) { //set missing result values to NaN
			tableColStart = tableColLast + 1;
			tableColEnd = tableOut.getColumnCount() - 1;
			for (int c = tableColStart; c <= tableColEnd; c++ ) {
				tableOut.set(c, row, Double.NaN);
			}
		}
		else { //set result values
			tableColStart = tableColLast + 1;
			tableColEnd = tableColStart + containerPM.item1_Values.length - 1;
			for (int c = tableColStart; c <= tableColEnd; c++ ) {
				tableOut.set(c, row, containerPM.item1_Values[c-tableColStart]);
			}
		}
	}

	/**
	 * shows the result table
	 */
	private void showTable() {
		// Show table
		uiService.show(TABLE_OUT_NAME, tableOut);
	}
	
	/**
	*
	* Processing
	*/
	private CsajContainer_ProcessMethod process(DefaultGenericTable dgt, int col) { //  c column number
	
		if (dgt == null) {
			logService.info(this.getClass().getName() + " WARNING: dgt==null, no sequence for processing!");
		}

		String sequenceRange  = choiceRadioButt_SequenceRange;
		String surrType       = choiceRadioButt_SurrogateType;
		numSurrogates         = spinnerInteger_NumSurrogates;
		numBoxLength          = spinnerInteger_BoxLength;
		int numDataPoints     = dgt.getRowCount();
		numRegStart                = spinnerInteger_NumRegStart; //may be changed later
		numRegEnd                = spinnerInteger_NumRegEnd; //may be changed later
		boolean skipZeroes    = booleanSkipZeroes;	
		String psdType        = choiceRadioButt_PSDType;
		String swvType        = choiceRadioButt_SWVType;	
		boolean optShowPlot   = booleanShowDoubleLogPlot;
		
		double[] resultValues = new double[3]; // Dim, R2, StdErr
		for (int r = 0; r<resultValues.length; r++) resultValues[r] = Double.NaN;
		
//		double[]totals = new double[numKMax];
//		double[]eps = new double[numKMax];
//		// definition of eps
//		for (int kk = 0; kk < numKMax; kk++) {
//			eps[kk] = kk + 1;		
//			//logService.info(this.getClass().getName() + " k=" + kk + " eps= " + eps[kk][b]);
//		}
		//******************************************************************************************************
		//domain1D = new double[numDataPoints];
		sequence1D = new double[numDataPoints];
		for (int n = 0; n < numDataPoints; n++) {
			//domain1D[n] = Double.NaN;
			sequence1D[n] = Double.NaN;
		}
		
		sequenceColumn = dgt.get(col);
		String columnType = sequenceColumn.get(0).getClass().getSimpleName();	
		logService.info(this.getClass().getName() + " Column type: " + columnType);	
		if (!columnType.equals("Double")) {
			logService.info(this.getClass().getName() + " NOTE: Column type is not supported");	
			return null; 
		}
		
		for (int n = 0; n < numDataPoints; n++) {
			//domain1D[n]  = n+1;
			sequence1D[n] = Double.valueOf((Double)sequenceColumn.get(n));
		}	
		
		sequence1D = removeNaN(sequence1D);
		if (skipZeroes) sequence1D = removeZeroes(sequence1D);

		//numDataPoints may be smaller now
		numDataPoints = sequence1D.length;
		
		logService.info(this.getClass().getName() + " Column #: "+ (col+1) + "  " + sequenceColumn.getHeader() + "  Size of sequence = " + numDataPoints);
		if (numDataPoints == 0) return null; //e.g. if sequence had only NaNs
		
		//domain1D = new double[numDataPoints];
		//for (int n = 0; n < numDataPoints; n++) domain1D[n] = n+1
				
		//"Entire sequence", "Subsequent boxes", "Gliding box" 
		//********************************************************************************************************
		if (sequenceRange.equals("Entire sequence")){	
			if (surrType.equals("No surrogates")) {
				resultValues = new double[15]; // 4xPSD, 3xSCC, 4xDisp, 4xSWV
			} else {
				resultValues = new double[15+2+2*numSurrogates]; //+ Mean_Surr, Mean R2_Surr,  Dim_Surr1, Dim_Surr2,  Dim_Surr3, ......R2_Surr1,.... 2, 3......
			}
			for (int r = 0; r < resultValues.length; r++) resultValues[r] = Double.NaN;
			//logService.info(this.getClass().getName() + " Column #: "+ (col+1) + "  " + sequenceColumn.getHeader() + "  Size of sequence = " + sequence1D.length);	
			//if (sequence1D.length) == 0) return null; //e.g. if sequence had only NaNs
			
			psd_Beta    = Double.NaN;
			psd_H       = Double.NaN;
			psd_r2      = Double.NaN;
			psd_stdErr  = Double.NaN;
		
			ssc_Beta    = Double.NaN;
			ssc_r2      = Double.NaN;
			ssc_stdErr  = Double.NaN;
	
			disp_Beta   = Double.NaN;
			disp_H 	    = Double.NaN;
			disp_r2     = Double.NaN;
			disp_stdErr = Double.NaN;
			
			swv_Beta    = Double.NaN;
			swv_H 	    = Double.NaN;
			swv_r2      = Double.NaN;
			swv_stdErr  = Double.NaN;
			
			resultPSD = null;
			betaPSD = new BetaPSD();
			if (psdType.equals("PSD")){	
				//check endpoint of regression, because power spectrum is only half the size of the sequence
				int poweSpecLength = sequence1D.length/2;
				if (numRegEnd > poweSpecLength){
					numRegEnd = poweSpecLength;
					this.spinnerInteger_NumRegEnd = poweSpecLength;
					this.callbackNumRegEnd();
				}		
				resultPSD = betaPSD.computeRegression(sequence1D, numRegStart, numRegEnd);		
			}
			else if (psdType.equals("lowPSDwe")){
			
				//apply parabolic windowing	
				sequenceNew = this.calcParabolicWindowing(sequence1D); 
				//apply bridge dentrending
				sequenceNew = this.calcBridgeDetrending(sequenceNew);
			
				//restrict regression
				int poweSpecLength = sequence1D.length/2;
				numRegStart = poweSpecLength/8;
				numRegEnd = poweSpecLength/2;
				
				this.spinnerInteger_NumRegStart = numRegStart;
				this.spinnerInteger_NumRegEnd = numRegEnd;
				this.callbackNumRegStart();
				this.callbackNumRegEnd();
						
				resultPSD = betaPSD.computeRegression(sequenceNew, numRegStart, numRegEnd);	
			}
				
			psd_Beta    = -resultPSD[0];
			psd_r2      = resultPSD[1];
			psd_stdErr  = resultPSD[2];
			
			if (optShowPlot) {	
				String preName = "Power Spectral Densitiy " +sequenceColumn.getHeader();
				showPlot(betaPSD.getLnDataX(), betaPSD.getLnDataY(), preName, col, "ln(k)", "ln(P)", numRegStart, numRegEnd);
			}	
					
			boolean sequenceIsfGn     = false;
			boolean sequenceIsfBm     = false;
			boolean sequenceIsUnknown = true;
			
			if (psd_Beta <= -1.2){
				logService.info(this.getClass().getName() + " Sequence type is wether fGn nor fBm");
				sequenceIsfGn     = false;
				sequenceIsfBm     = false;
				sequenceIsUnknown = true;
			}
			if (psd_Beta >=3.2){
				logService.info(this.getClass().getName() + " Sequence type is wether fGn nor fBm");
				sequenceIsfGn     = false;
				sequenceIsfBm     = false;
				sequenceIsUnknown = true;
			}
			
			if ((psd_Beta <= 0.38) && (psd_Beta >-1.2)){ //fGn
				logService.info(this.getClass().getName() + " fGn sequence detected");
				sequenceIsfGn     = true;
				sequenceIsfBm     = false;	
				sequenceIsUnknown = false;
				sequenceType = "fGn";
			}
			if ((psd_Beta >= 1.04) && (psd_Beta < 3.2)){ //fBm
				logService.info(this.getClass().getName() + " fBm sequence detected");
				sequenceIsfGn     = false;
				sequenceIsfBm     = true;	
				sequenceIsUnknown = false;
				sequenceType = "fBm";
			}	
			if ((psd_Beta > 0.38) && (psd_Beta < 1.04)){ //sequence between fGn and fBm
				logService.info(this.getClass().getName() + " Sequence type could not be detected, starting SSC method");
				//sequence summation conversion (fGn to fBm, fBm to summed fBm)
				sequenceSSC = new double[sequence1D.length];
				sequenceSSC[0] = sequence1D[0];		
				for (int i = 1; i < sequence1D.length; i++){
					sequenceSSC[i] = sequenceSSC[i-1] + sequence1D[i];
				}
				
				betaSWVH = new BetaSWVH();
				resultSSC= betaSWVH.computeRegression(sequenceSSC);
				ssc_Beta    = -resultSSC[0];
				ssc_r2      = resultSSC[1];
				ssc_stdErr  = resultSSC[2];
				
				if (optShowPlot) {	
					String preName = "Scaled window variance for H " +sequenceColumn.getHeader();
					showPlot(betaSWVH.getLnDataX(), betaSWVH.getLnDataY(), preName, col, "ln(winSize/winSize0)", "ln(SD/SD0)", 1, betaSWVH.getLnDataX().length);
				}	

				if (ssc_Beta <= 0.6){ //fGn
					logService.info(this.getClass().getName() + " fGn sequence after SSC detected");
					sequenceIsfGn     = true;
					sequenceIsfBm     = false;	
					sequenceIsUnknown = false;
					sequenceType      = "fGn";
				}
				if (ssc_Beta >= 1.0){ //fBm
					logService.info(this.getClass().getName() + " fBm sequence after SSC detected");
					sequenceIsfGn     = false;
					sequenceIsfBm     = true;	
					sequenceIsUnknown = false;
					sequenceType      = "fBm";
				}	
				if ((ssc_Beta > 0.6) && (ssc_Beta < 1.0)){ //not clear
					logService.info(this.getClass().getName() + " Sequence type could not be detected, even using SSC");
					sequenceIsfGn     = false;
					sequenceIsfBm     = false;	
					sequenceIsUnknown = true;
					sequenceType      = "?";
				}		
			}
			if (sequenceIsfGn){ //fGn
				psd_H = (psd_Beta + 1)/2;
				betaDispH = new BetaDispH();
				resultDispH = betaDispH.computeRegression(sequence1D);
				disp_Beta 	 = resultDispH[0];
				disp_H		 = 1 + resultDispH[0];	
				disp_r2      = resultDispH[1];
				disp_stdErr  = resultDispH[2];
				
				if (optShowPlot) {	
					String preName = "Dispersional plot for H fGn " +sequenceColumn.getHeader();
					showPlot(betaDispH.getLnDataX(), betaDispH.getLnDataY(), preName, col, "ln(winSize/winSize0)", "ln(SD/SD0)", 1, betaDispH.getLnDataX().length);
				}		
			}
			if (sequenceIsfBm){ //fBm
				psd_H = (psd_Beta - 1)/2;
				resultSWVH = null;
				betaSWVH = new BetaSWVH();
				if (swvType.equals("SWV")){
					resultSWVH	= betaSWVH.computeRegression(sequence1D);
				}
				if (swvType.equals("bdSWV")){
					double[] sequenceNew = this.calcBridgeDetrending(sequence1D); 
					resultSWVH	= betaSWVH.computeRegression(sequenceNew);
				}
			
				if (optShowPlot) {	
					String preName = "Scaled window variance for H fBm " +sequenceColumn.getHeader();
					showPlot(betaSWVH.getLnDataX(), betaSWVH.getLnDataY(), preName, col, "ln(winSize/winSize0)", "ln(SD/SD0)", 1, betaSWVH.getLnDataX().length);
				}	
				swv_Beta 	= resultSWVH[0];
				swv_H		= resultSWVH[0];	
				swv_r2      = resultSWVH[1];
				swv_stdErr  = resultSWVH[2];	
			}	
	
			resultValues[0]  = psd_Beta;
			resultValues[1]  = psd_H;
			resultValues[2]  = psd_r2;
			resultValues[3]  = psd_stdErr;
			resultValues[4]  = ssc_Beta;
			resultValues[5]  = ssc_r2;
			resultValues[6]  = ssc_stdErr;
			resultValues[7]  = disp_Beta;
			resultValues[8]  = disp_H;
			resultValues[9]  = disp_r2;
			resultValues[10] = disp_stdErr;
			resultValues[11] = swv_Beta;
			resultValues[12] = swv_H;
			resultValues[13] = swv_r2;
			resultValues[14] = swv_stdErr;
		
			int lastMainResultsIndex = 14;
			
			if (!surrType.equals("No surrogates")) { //Add surrogate analysis
				surrSequence1D = new double[sequence1D.length];
				
				double beta  = Double.NaN;
				double r2    = Double.NaN;
				double sumBetas  = 0.0;
				double sumR2s    = 0.0;
				int poweSpecLength;
				
				CsajAlgorithm_Surrogate1D surrogate1D = new CsajAlgorithm_Surrogate1D();
				String windowingType = "Rectangular";
				for (int s = 0; s < numSurrogates; s++) {
					//choices = {"No surrogates", "Shuffle", "Gaussian", "Random phase", "AAFT"}, 
					if (surrType.equals("Shuffle"))      surrSequence1D = surrogate1D.calcSurrogateShuffle(sequence1D);
					if (surrType.equals("Gaussian"))     surrSequence1D = surrogate1D.calcSurrogateGaussian(sequence1D);
					if (surrType.equals("Random phase")) surrSequence1D = surrogate1D.calcSurrogateRandomPhase(sequence1D, windowingType);
					if (surrType.equals("AAFT"))         surrSequence1D = surrogate1D.calcSurrogateAAFT(sequence1D, windowingType);

					//"PSD", "DISP", "SWV"
					if (this.choiceRadioButt_SurrBoxHurstType.equals("PSD_Beta")) {
						poweSpecLength = surrSequence1D.length/2;	
						if (psdType.equals("PSD")){			
							resultPSD = betaPSD.computeRegression(surrSequence1D, 1 , poweSpecLength);		
						}
						else if (psdType.equals("lowPSDwe")){
						
							//apply parabolic windowing	
							sequenceNew = this.calcParabolicWindowing(surrSequence1D); 
							//apply bridge dentrending
							sequenceNew = this.calcBridgeDetrending(surrSequence1D);
								
							resultPSD = betaPSD.computeRegression(sequenceNew, poweSpecLength/8, poweSpecLength/2);	
						}
			
						beta = -resultPSD[0];
						r2   =  resultPSD[1];
					}
					else if (this.choiceRadioButt_SurrBoxHurstType.equals("DISP_Beta")) {
						
					}
					else if (this.choiceRadioButt_SurrBoxHurstType.equals("SWV_Beta")) {
						
					}			
					resultValues[lastMainResultsIndex + 3 + s]                    = beta;
					resultValues[lastMainResultsIndex + 3 + numSurrogates + s]    = r2;
					//resultValues[lastMainResultsIndex + 3 + (2*numSurrogates) +s] = ?;
					sumBetas += beta;
					sumR2s   += r2;
					//sum  +=  ;
				}
				resultValues[lastMainResultsIndex + 1] = sumBetas/numSurrogates;
				resultValues[lastMainResultsIndex + 2] = sumR2s/numSurrogates;
				//resultValues[lastMainResultsIndex + 3] = sum/?;				
			} 
		//********************************************************************************************************	
		} else if (sequenceRange.equals("Subsequent boxes")){
			resultValues = new double[(int) (2*numSubsequentBoxes)]; // Dim R2 == two * number of boxes		
			for (int r = 0; r<resultValues.length; r++) resultValues[r] = Double.NaN;
			subSequence1D = new double[(int) numBoxLength];
			//number of boxes may be smaller than intended because of NaNs or removed zeroes
			long actualNumSubsequentBoxes = (long) Math.floor((double)sequence1D.length/(double)spinnerInteger_BoxLength);
			double beta = Double.NaN;
			double r2   = Double.NaN;
			int poweSpecLength;
			betaPSD = new BetaPSD();
			resultPSD = null;
			
			//get sub-sequences and compute dimensions
			for (int i = 0; i < actualNumSubsequentBoxes; i++) {	
				logService.info(this.getClass().getName() + " Processing subsequent box #: "+(i+1) + "/" + actualNumSubsequentBoxes);	
				int start = (i*numBoxLength);
				for (int ii = start; ii < (start + numBoxLength); ii++){ 
					subSequence1D[ii-start] = sequence1D[ii];
				}
//				//Compute specific values************************************************
				
				//"PSD", "DISP", "SWV"
				if (choiceRadioButt_SurrBoxHurstType.equals("PSD_Beta")) {
					poweSpecLength = subSequence1D.length/2;	
					
					if (psdType.equals("PSD")){			
						resultPSD = betaPSD.computeRegression(subSequence1D, 1 , poweSpecLength);		
					}
					else if (psdType.equals("lowPSDwe")){
					
						//apply parabolic windowing	
						sequenceNew = this.calcParabolicWindowing(subSequence1D); 
						//apply bridge dentrending
						sequenceNew = this.calcBridgeDetrending(subSequence1D);
							
						resultPSD = betaPSD.computeRegression(sequenceNew, poweSpecLength/8, poweSpecLength/2);	
					}
		
					beta = -resultPSD[0];
					r2   =  resultPSD[1];
				}
				else if (choiceRadioButt_SurrBoxHurstType.equals("DISP_Beta")) {
					
				}
				else if (choiceRadioButt_SurrBoxHurstType.equals("SWV_Beta")) {
					
				}			
				
//				if (optShowPlot) {	
//					String preName = "PSD_Beta " +sequenceColumn.getHeader() + "-Box#" + (i+1);
//					showPlot(betaPSD.getLnDataX(), betaPSD.getLnDataY(), preName, col, "", "", 1, betaPSD.getLnDataX().length);
//				}
				resultValues[i]                             = beta; // Hurst;
				resultValues[(int)(i + numSubsequentBoxes)] = r2;  //R2		
			
				//***********************************************************************
			}	
		//********************************************************************************************************			
		} else if (sequenceRange.equals("Gliding box")){
			resultValues = new double[(int) (2*numGlidingBoxes)]; // Dim R2 == two * number of boxes	
			for (int r = 0; r<resultValues.length; r++) resultValues[r] = Double.NaN;
			subSequence1D = new double[(int) numBoxLength];
			//number of boxes may be smaller because of NaNs or removed zeroes
			long actualNumGlidingBoxes = sequence1D.length - spinnerInteger_BoxLength + 1;
			double beta = Double.NaN;
			double r2   = Double.NaN;
			int poweSpecLength;
			betaPSD = new BetaPSD();
			resultPSD = null;
			
			//get sub-sequences and compute dimensions
			for (int i = 0; i < actualNumGlidingBoxes; i++) {
				logService.info(this.getClass().getName() + " Processing gliding box #: "+(i+1) + "/" + actualNumGlidingBoxes);	
				int start = i;
				for (int ii = start; ii < (start + numBoxLength); ii++){ 
					subSequence1D[ii-start] = sequence1D[ii];
				}	
				//Compute specific values************************************************
			
				//"PSD", "DISP", "SWV"
				if (choiceRadioButt_SurrBoxHurstType.equals("PSD_Beta")) {
					poweSpecLength = subSequence1D.length/2;	
					
					if (psdType.equals("PSD")){			
						resultPSD = betaPSD.computeRegression(subSequence1D, 1 , poweSpecLength);		
					}
					else if (psdType.equals("lowPSDwe")){
					
						//apply parabolic windowing	
						sequenceNew = this.calcParabolicWindowing(subSequence1D); 
						//apply bridge detrending
						sequenceNew = this.calcBridgeDetrending(subSequence1D);
							
						resultPSD = betaPSD.computeRegression(sequenceNew, poweSpecLength/8, poweSpecLength/2);	
					}
		
					beta = -resultPSD[0];
					r2   =  resultPSD[1];
				}
				else if (choiceRadioButt_SurrBoxHurstType.equals("DISP_Beta")) {
					
				}
				else if (choiceRadioButt_SurrBoxHurstType.equals("SWV_Beta")) {
					
				}						
				//if (optShowPlot){ //show all plots
//				if ((optShowPlot) && (i==0)){ //show only first plot
//					String preName = "PSD_Beta" + sequenceColumn.getHeader() + "-Box #" + (i+1);
//					showPlot(betaPSD.getLnDataX(), betaPSD.getLnDataY(), preName, col, "", "", 1, betaPSD.getLnDataX().length);
//				}	
				resultValues[i]                          = beta; //beta;
				resultValues[(int)(i + numGlidingBoxes)] = r2;  //R2		
				
				//***********************************************************************
			}
		}
		
		return new CsajContainer_ProcessMethod(resultValues);
		// Dim, R2, StdErr
		// Output
		// uiService.show(TABLE_OUT_NAME, table);
	}


	/**
	 * This Method applies bridge dentrending (end matching)
	 * subtraction of a line connecting the first and the last point
	 * @param sequence
	 * @return bridge dentrending as {@link double[]} of doubles
	 */
	private double[] calcBridgeDetrending(double[] sequence) {
		int length = sequence.length;
		double[]sequenceBD = new double[length];
		double first = sequence[0];
		double last = sequence[length - 1];
		double step = (last - first)/(length - 1);
		for  (int i = 0; i < length; i++){
			double lineValue = first + (i * step);
			sequenceBD[i] = sequence[i]- lineValue;
		}
		return sequenceBD;
	}

	/**
	 * This method applies parabolic windowing
	 * according to Equ6 Eke etal 2000
	 * @param sequence
	 * @return the parabolic windowing of the sequence
	 */
	private double[] calcParabolicWindowing(double[] sequence) {
		int length = sequence.length;
		double[] sequenceW = new double[length];
		for  (int j = 0; j < length; j++){
			double b = (2.0d*(j+1))/(length+1) - 1;
			double parabWin = 1.0d - b*b;
			sequenceW[j] = sequence[j]*parabWin;
		}
		return sequenceW;
	}

	// This method shows the double log plot
	private void showPlot(double[] lnDataX, double[] lnDataY, String preName, int col, String labelX, String labelY, int numRegStart, int numRegEnd) {
		if (lnDataX == null) {
			logService.info(this.getClass().getName() + " lnDataX == null, cannot display the plot!");
			return;
		}
		if (lnDataY == null) {
			logService.info(this.getClass().getName() + " lnDataY == null, cannot display the plot!");
			return;
		}
		if (col < 0) {
			logService.info(this.getClass().getName() + " col < 0, cannot display the plot!");
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
			preName += "Col" + String.format("%03d", col) + "-";
		}
		boolean isLineVisible = false; // ?
		CsajPlot_RegressionFrame doubleLogPlot = DisplayRegressionPlotXY(lnDataX, lnDataY, isLineVisible,
				"Double log plot - ", preName + "-" + tableInName, labelX, labelY, "", numRegStart, numRegEnd);
		doubleLogPlotList.add(doubleLogPlot);
		
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
	
	// This method removes NaN  from field sequence1D
	private double[] removeNaN(double[] sequence) {
		int lengthOld = sequence.length;
		int lengthNew = 0;
		
		for (int i = 0; i < lengthOld; i++) {
			if (!Double.isNaN(sequence[i])) {
				lengthNew += 1;
			}
		}
		sequence1D = new double[lengthNew];
		int ii = -1;
		for (int i = 0; i < lengthOld; i++) {
			if (!Double.isNaN(sequence[i])) {
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
	private CsajPlot_RegressionFrame DisplayRegressionPlotXY(double[] dataX, double[] dataY,
			boolean isLineVisible, String frameTitle, String plotLabel, String xAxisLabel, String yAxisLabel, String legendLabel,
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
		
		// open and display a sequence, waiting for the operation to finish.
		ij.command().run(Csaj1DOpenerCmd.class, true).get();
		//open and run Plugin
		ij.command().run(MethodHandles.lookup().lookupClass().getName(), true);
	}
}
