/*-
  * #%L
 * Project: ImageJ2 signal plugin for recurrence quantification analysis.
 * File: Csaj1DRQA.java
 * 
 * $Id$
 * $HeadURL$
 * 
 * This file is part of ComsystanJ software, hereinafter referred to as "this program".
 * %%
 * Copyright (C) 2023 - 2024 Comsystan Software
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
package at.csa.csaj.plugin1d.rqa;

import java.awt.Frame;
import java.awt.Toolkit;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import net.imagej.ImageJ;
import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;

import org.scijava.ItemIO;
import org.scijava.ItemVisibility;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
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
import org.scijava.table.Column;
import org.scijava.table.DefaultGenericTable;
import org.scijava.table.DefaultTableDisplay;
import org.scijava.table.DoubleColumn;
import org.scijava.table.FloatColumn;
import org.scijava.table.GenericColumn;
import org.scijava.table.IntColumn;
import org.scijava.ui.UIService;
import org.scijava.widget.Button;
import org.scijava.widget.ChoiceWidget;
import org.scijava.widget.NumberWidget;
import at.csa.csaj.commons.plot.RegressionPlotFrame;
import at.csa.csaj.commons.algorithms.Surrogate1D;
import at.csa.csaj.commons.dialog.WaitingDialogWithProgressBar;
import at.csa.csaj.plugin1d.open.Csaj1DOpener;
import at.csa.csaj.plugin1d.rqa.util.leshao.LimitedQueue;
import at.csa.csaj.plugin1d.rqa.util.leshao.RecurrentMatrix;

/**
 * A {@link Command} plugin computing <Recurrence quantification analysis</a>
 * of a  sequence.
 */
@Plugin(type = ContextCommand.class, 
	headless = true,
	label = "Recurrence quantification analysis",
	initializer = "initialPluginLaunch",
	//iconPath = "/images/comsystan-??.png", //Menu entry icon
	menu = {
	@Menu(label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT, mnemonic = MenuConstants.PLUGINS_MNEMONIC),
	@Menu(label = "ComsystanJ"),
	@Menu(label = "1D Sequence(s)"),
	@Menu(label = "Recurrence quantification analysis ", weight = 146)}) //Space at the end of the label is necessary to avoid duplicate with image2d plugin 
//public class SequenceRQA<T extends RealType<T>> extends InteractiveCommand { // non blocking  GUI
public class Csaj1DRQA<T extends RealType<T>> extends ContextCommand implements Previewable { //modal GUI with cancel

	private static final String PLUGIN_LABEL            = "<html><b>Recurrence quantification analysis</b></html>";
	private static final String SPACE_LABEL             = "";
	//private static final String REGRESSION_LABEL        = "<html><b>Regression parameters</b></html>";
	private static final String PHASESPACE_LABEL        = "<html><b>Phase space parameters</b></html>";
	private static final String ALGORITHMS_LABEL        = "<html><b>Algorithms</b></html>";
	private static final String ANALYSISOPTIONS_LABEL   = "<html><b>Analysis options</b></html>";
	private static final String BACKGROUNDOPTIONS_LABEL = "<html><b>Background Option</b></html>";
	private static final String DISPLAYOPTIONS_LABEL    = "<html><b>Display options</b></html>";
	private static final String PROCESSOPTIONS_LABEL    = "<html><b>Process options</b></html>";
	
	private static double[] sequence1D;
	private static double[] domain1D;
	private static double[] subSequence1D;
	private static double[] surrSequence1D;
	Column<? extends Object> sequenceColumn;
	
	private static String tableInName;
	private static String[] sliceLabels;
	private static long numColumns = 0;
	private static long numRows = 0;
	private static long numDimensions = 0;
	private static int  numSurrogates = 0;
	private static int  numBoxLength = 0;
	private static long numSubsequentBoxes = 0;
	private static long numGlidingBoxes = 0;
	
	//private static final int  numKMax = 1000;
	
	//private static ArrayList<RegressionPlotFrame> doubleLogPlotList = new ArrayList<RegressionPlotFrame>();
	private static ArrayList<RegressionPlotFrame> doubleLogPlotList = null;
	private static float[][] reccurentMatrix;
	private static Img<UnsignedByteType> imgRecurrentMatrix;
	
	private static final String tableOutName = "Table - RQA";
	
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
	

	@Parameter(label = tableOutName, type = ItemIO.OUTPUT)
	private DefaultGenericTable tableOut;


	// Widget elements------------------------------------------------------

	//@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	//private final String labelPlugin = PLUGIN_LABEL;

	//@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	//private final String labelSpace = SPACE_LABEL;
	
	@Parameter(type = ItemIO.INPUT)
	private DefaultTableDisplay  defaultTableDisplay;


//	//-----------------------------------------------------------------------------------------------------
//	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
//	private final String labelRegression = REGRESSION_LABEL;
//	
//	@Parameter(label = "Maximum delay k",
//			   description = "Maximal delay between data points",
//			   style = NumberWidget.SPINNER_STYLE,
//			   min = "3",
//			   max = "9999999999999999999",
//			   stepSize = "1",
//			   persist = false, // restore  previous value  default  =  true
//			   initializer = "initialKMax", callback = "callbackKMax")
//	private int spinnerInteger_KMax;
//
//	@Parameter(label = "Regression Min",
//			   description = "Minimum x value of linear regression",
//			   style = NumberWidget.SPINNER_STYLE,
//			   min = "1",
//			   max = "9999999999999999999",
//			   stepSize = "1",
//			   persist = false, //restore previous value default = true
//			   initializer = "initialRegMin", callback = "callbackRegMin")
//	private int spinnerInteger_RegMin = 1;
//
//	@Parameter(label = "Regression Max",
//			   description = "Maximum x value of linear regression",
//			   style = NumberWidget.SPINNER_STYLE,
//			   min = "2",
//			   max = "9999999999999999999",
//			   stepSize = "1",
//			   persist = false, //restore previous value default = true
//			   initializer = "initialRegMax", callback = "callbackRegMax")
//	private int spinnerInteger_RegMax = 3;
	
	//-----------------------------------------------------------------------------------------------------
//	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
//	private final String labelAlgorithmsOptions = ALGORITHMS_LABEL;
//	
//	@Parameter(label = "Algorithm",
//    		   description = "Type of algorithm",
// 		       style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
// 		       choices = {"RQA"},  //, "CrossRQA"},
// 		       initializer = "initialAlgorithm",
//               callback = "callbackAlgorithm")
//    private String choiceRadioButt_Algorithm;
	
	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelPhaseSpaceOptions = PHASESPACE_LABEL;

	@Parameter(label = "Embedding dimension",
			   description = "Embedding dimension of phase space",
			   style = NumberWidget.SPINNER_STYLE, 
			   min = "1",
			   max = "9999999999999999999",
			   stepSize = "1",
			   persist = true, // restore  previous value  default  =  true
			   initializer = "initialEmbDim",
			   callback = "callbackEmbDim")
	private int spinnerInteger_EmbDim;
	
	@Parameter(label = "Tau",
			   description = "Time lag/delay of phase space reconstruction",
			   style = NumberWidget.SPINNER_STYLE, 
			   min = "1",
			   max = "9999999999999999999",
			   stepSize = "1",
			   persist = true, // restore  previous value  default  =  true
			   initializer = "initialTau",
			   callback = "callbackTau")
	private int spinnerInteger_Tau;
	    
	@Parameter(label = "Eps",
			   description = "Epsilon of neighboring data points",
		       style = NumberWidget.SPINNER_STYLE,
		       min = "0",
		       max = "999999999999999999999999",
		       stepSize = "0.01",
		       initializer = "initialEps",
		       callback = "callbackEps")
	 private float spinnerFloat_Eps;
	
	@Parameter(label = "Norm",
 		       description = "Type of norm (distance)",
		       style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
		       choices = {"Euclidean", "Maximum", "Cityblock", "Angular"},
		       initializer = "initialNorm",
               callback = "callbackNorm")
	private String choiceRadioButt_Norm;
	
//    @Parameter(label = "Point pairs #",
// 		   description = "Number of point pairs for which the divergences are computed",
//		       style = NumberWidget.SPINNER_STYLE,
//		       min = "0",
//		       max = "9999999999999999999",
//		       initializer = "initialNumPointPairs",
//		       stepSize = "1",
//		       callback = "callbackNumPointPairs")
//    private int spinnerInteger_NumPointPairs;
    
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

//	@Parameter(label = "Show double log plot",
//		   	   persist = true, //restore previous value default = true
//			   initializer = "initialShowDoubleLogPlots")
//	private boolean booleanShowDoubleLogPlot;

    @Parameter(label = "Show recurrence plot",
		       persist = true,  //restore previous value default = true   
		       initializer = "initialShowRecurrenceMatrix")
	 private boolean booleanShowReccurenceMatrix;
	
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
	
	@Parameter(label = "Column #", description = "column number", style = NumberWidget.SPINNER_STYLE, min = "1", max = "1000", stepSize = "1",
			   persist = false, // restore  previous value  default  =  true
			   initializer = "initialNumColumn", callback = "callbackNumColumn")
	private int spinnerInteger_NumColumn;

	@Parameter(label = "Process single column #", callback = "callbackProcessSingleColumn")
	private Button buttonProcessSingleColumn;

//	@Parameter(label = "Process all columns", callback = "callbackProcessAllColumns")
//	private Button buttonProcessAllColumns;


	// ---------------------------------------------------------------------
		
	protected void initialPluginLaunch() {
		//tableIn = (DefaultGenericTable) defaultTableDisplay.get(0);
		checkItemIOIn();
	}
	
//	protected void initialKMax() {
//		//numbKMax = (int) Math.floor(tableIn.getRowCount() / 3.0);
//		spinnerInteger_KMax = 20;
//	}
//	protected void initialRegMin() {
//		spinnerInteger_RegMin = 1;
//	}
//	protected void initialRegMax() {
//		//numbKMax = (int) Math.floor(tableIn.getRowCount() / 3.0);
//		spinnerInteger_RegMax = 8;
//	}

	//	protected void initialAlgorithm() {
//		choiceRadioButt_Algorithm = "RQA";
//	}
	protected void initialEmbDim() {
		spinnerInteger_EmbDim = 2;
	}
	protected void initialTau() {
		spinnerInteger_Tau = 1;
	}
//	protected void initialNumPointPairs() {
//	    spinnerInteger_NumPointPairs = 10;
//	}
	protected void initialEps() {
	    spinnerFloat_Eps = 0.001f;
	}
	protected void initialNorm() {
		choiceRadioButt_Norm = "Norm";
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
	protected void initialSkipZeroes() {
		booleanSkipZeroes = false;
	}	
//	protected void initialShowDoubleLogPlots() {
//		booleanShowDoubleLogPlot = true;
//	}
	protected void initialShowReccurenceMatrix() {
	    booleanShowReccurenceMatrix = false;
	}
	protected void initialOverwriteDisplays() {
    	booleanOverwriteDisplays = true;
	}
	protected void initialNumColumn() {
		spinnerInteger_NumColumn = 1;
	}

	// ------------------------------------------------------------------------------	
//	/** Executed whenever the {@link #spinnerInteger_KMax} parameter changes. */
//	protected void callbackKMax() {
//
//		if (spinnerInteger_KMax < 2) {
//			spinnerInteger_KMax = 2;
//		}
//		if (spinnerInteger_KMax > numKMax) {
//			spinnerInteger_KMax = numKMax;
//		}
//		if (spinnerInteger_RegMax > spinnerInteger_KMax) {
//			spinnerInteger_RegMax = spinnerInteger_KMax;
//		}
//		if (spinnerInteger_RegMin >= spinnerInteger_RegMax - 1) {
//			spinnerInteger_RegMin  = spinnerInteger_RegMax - 1;
//		}
//		logService.info(this.getClass().getName() + " k set to " + spinnerInteger_KMax);
//	}
//
//	/** Executed whenever the {@link #spinnerInteger_RegMin} parameter changes. */
//	protected void callbackRegMin() {
//		if (spinnerInteger_RegMin >= spinnerInteger_RegMax - 1) {
//			spinnerInteger_RegMin  = spinnerInteger_RegMax - 1;
//		}
//		if (spinnerInteger_RegMin < 1) {
//			spinnerInteger_RegMin = 1;
//		}
//		logService.info(this.getClass().getName() + " Regression Min set to " + spinnerInteger_RegMin);
//	}
//
//	/** Executed whenever the {@link #spinnerInteger_RegMax} parameter changes. */
//	protected void callbackRegMax() {
//		if (spinnerInteger_RegMax <= spinnerInteger_RegMin + 1) {
//			spinnerInteger_RegMax  = spinnerInteger_RegMin + 1;
//		}
//		if (spinnerInteger_RegMax > spinnerInteger_KMax) {
//			spinnerInteger_RegMax = spinnerInteger_KMax;
//		}
//
//		logService.info(this.getClass().getName() + " Regression Max set  to " + spinnerInteger_RegMax);
//	}
	
//	/** Executed whenever the {@link #choiceRadioButt_Algorithm} parameter changes. */
//	protected void callbackAlgorithm() {
//		logService.info(this.getClass().getName() + " Algorithm changed to " + choiceRadioButt_Algorithm);
//	}
	
	/** Executed whenever the {@link #spinnerInteger_EmbDim} parameter changes. */
	protected void callbackEmbDim() {
		logService.info(this.getClass().getName() + " Embedding dimension set to " + spinnerInteger_EmbDim);
	}
	
	/** Executed whenever the {@link #spinnerInteger_Tau} parameter changes. */
	protected void callbackTau() {
		logService.info(this.getClass().getName() + " Tau set to " + spinnerInteger_Tau);
	}

//	/** Executed whenever the {@link #spinFloat_NumPointPairs} parameter changes. */
//	protected void callbackNumPointPairs() {
//		logService.info(this.getClass().getName() + " Number ofpoint pairs changed to " + spinnerInteger_NumPointPairs);
//	}
	
	/** Executed whenever the {@link #spinFloat_Eps} parameter changes. */
	protected void callbackEps() {
		logService.info(this.getClass().getName() + " Epsilon changed to " + spinnerFloat_Eps);
	}
	
	/** Executed whenever the {@link #choiceRadioButt_Norm} parameter changes. */
	protected void callbackNorm() {
		logService.info(this.getClass().getName() + " Norm changed to " + choiceRadioButt_Norm);
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
	    	   	uiService.show(tableOutName, tableOut);
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
		    	    startWorkflowForSingleColumn();
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
	    startWorkflowForAllColumns();
	}
	
	public void checkItemIOIn() {

		//DefaultTableDisplay dtd = (DefaultTableDisplay) displays.get(0);
		tableIn = (DefaultGenericTable) defaultTableDisplay.get(0);
	
		// get some info
		tableInName = defaultTableDisplay.getName();
		numColumns  = tableIn.getColumnCount();
		numRows     = tableIn.getRowCount();
			
		sliceLabels = new String[(int) numColumns];
	      
		logService.info(this.getClass().getName() + " Name: "      + tableInName); 
		logService.info(this.getClass().getName() + " Columns #: " + numColumns);
		logService.info(this.getClass().getName() + " Rows #: "    + numRows); 
	}

	/**
	* This method starts the workflow for a single column of the active display
	*/
	protected void startWorkflowForSingleColumn() {
	
		dlgProgress = new WaitingDialogWithProgressBar("Computing Recurrence quantification analysis, please wait... Open console window for further info.",
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
	
		dlgProgress = new WaitingDialogWithProgressBar("Computing Recurrence quantification analysis, please wait... Open console window for further info.",
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
		//tableOut.add(new IntColumn("Max delay k"));
		//tableOut.add(new IntColumn("Reg Min"));
		//tableOut.add(new IntColumn("Reg Max"));
		//tableOut.add(new GenericColumn("Algorithm"));
		tableOut.add(new IntColumn("Emb dim"));
		tableOut.add(new IntColumn("Tau"));
		tableOut.add(new FloatColumn("Eps"));
		tableOut.add(new GenericColumn("Norm"));
		//tableOut.add(new IntColumn("# Point pairs"));
	
		//"Entire sequence", "Subsequent boxes", "Gliding box" 
		if (choiceRadioButt_SequenceRange.equals("Entire sequence")){
			tableOut.add(new DoubleColumn("RR"));	
			tableOut.add(new DoubleColumn("DET"));
			tableOut.add(new DoubleColumn("RATIO"));
			tableOut.add(new DoubleColumn("DIV"));
			tableOut.add(new DoubleColumn("Lmean"));
			tableOut.add(new DoubleColumn("Lmax"));
			tableOut.add(new DoubleColumn("ENT"));
			tableOut.add(new DoubleColumn("LAM"));
			tableOut.add(new DoubleColumn("TT"));
			
			if (choiceRadioButt_SurrogateType.equals("No surrogates")) {
				//do nothing	
			} else { //Surrogates
				tableOut.add(new DoubleColumn("RR_Surr"));   //Mean surrogate value	
				tableOut.add(new DoubleColumn("DET_Surr"));  //Mean surrogate value
				tableOut.add(new DoubleColumn("RATIO_Surr"));  //Mean surrogate value
				tableOut.add(new DoubleColumn("DIV_Surr"));  //Mean surrogate value
				tableOut.add(new DoubleColumn("Lmean_Surr"));//Mean surrogate value
				tableOut.add(new DoubleColumn("Lmax_Surr")); //Mean surrogate value
				tableOut.add(new DoubleColumn("Ent_Surr"));       //Mean surrogate value
				tableOut.add(new DoubleColumn("LAM_Surr"));       //Mean surrogate value
				tableOut.add(new DoubleColumn("TT_Surr"));        //Mean surrogate value
				for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("RR_Surr-#"   +(s+1))); 
				for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("DET_Surr-#"  +(s+1))); 
				for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("RATIO_Surr-#"+(s+1))); 
				for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("DIV_Surr-#"  +(s+1))); 
				for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("Lmean_Surr-#"+(s+1))); 
				for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("Lmax_Surr-#" +(s+1))); 
				for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("ENT_Surr-#"  +(s+1))); 
				for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("LAM_Surr-#"  +(s+1))); 
				for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("TT_Surr-#"   +(s+1))); 
			}	
		} 
		else if (choiceRadioButt_SequenceRange.equals("Subsequent boxes")){
			for (int n = 1; n <= numSubsequentBoxes; n++) {
				tableOut.add(new DoubleColumn("RR-#" + n));	
			}
			for (int n = 1; n <= numSubsequentBoxes; n++) {
				tableOut.add(new DoubleColumn("DET-#" + n));	
			}
		}
		else if (choiceRadioButt_SequenceRange.equals("Gliding box")){
			for (int n = 1; n <= numGlidingBoxes; n++) {
				tableOut.add(new DoubleColumn("RR-#" + n));	
			}
			for (int n = 1; n <= numGlidingBoxes; n++) {
				tableOut.add(new DoubleColumn("DET-#" + n));	
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
		
		if (optDeleteExistingImgs) {
			//List<Display<?>> list = defaultDisplayService.getDisplays();
			//for (int i = 0; i < list.size(); i++) {
			//	display = list.get(i);
			//	System.out.println("display name: " + display.getName());
			//	if (display.getName().contains("Name")) display.close(); //does not close correctly in Fiji, it is only not available any more
			//}			
			//List<ImageDisplay> listImgs = defaultImageDisplayService.getImageDisplays(); //Is also not closed in Fiji 
		
			Frame frame;
			Frame[] listFrames = JFrame.getFrames();
			for (int i = listFrames.length -1 ; i >= 0; i--) { //Reverse order, otherwise focus is not given free from the last image
				frame = listFrames[i];
				//System.out.println("frame name: " + frame.getTitle());
				if (frame.getTitle().contains("Recurrence matrix")) {
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
			List<Display<?>> list = defaultDisplayService.getDisplays();
			for (int i = 0; i < list.size(); i++) {
				Display<?> display = list.get(i);
				//System.out.println("display name: " + display.getName());
				if (display.getName().contains(tableOutName))
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
		double[] resultValues = process(tableIn, c); 
		// 0 RR, 1 DET, 2 Lmean, 3 Lmax
		if (resultValues != null) logService.info(this.getClass().getName() + " RR: " + resultValues[0]);
			
		logService.info(this.getClass().getName() + " Processing finished.");
		writeToTable(0, c, resultValues); //write always to the first row
		
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
		
		// loop over all slices of stack
		for (int s = 0; s < numColumns; s++) { // s... numb er of sequence column
			//if (!exec.isShutdown()) {
				int percent = (int)Math.round((  ((float)s)/((float)numColumns)   *100.f   ));
				dlgProgress.updatePercent(String.valueOf(percent+"%"));
				dlgProgress.updateBar(percent);
				//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
				statusService.showStatus((s+1), (int)numColumns, "Processing " + (s+1) + "/" + (int)numColumns);
	
				long startTime = System.currentTimeMillis();
				logService.info(this.getClass().getName() + " Processing sequence column number " + (s+1) + "(" + numColumns + ")");
				
				// Compute result values
				double[] resultValues = process(tableIn, s);
				// 0 RR, 1 DET, 2 Lmean, 3 Lmax
				logService.info(this.getClass().getName() + " Processing finished.");
				writeToTable(s, s, resultValues);
	
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
	 * @param int rowNumber to write in the result table
	 * @param in sequenceNumber column number of sequence from tableIn.
	 * @param double[] result values
	 */
	private void writeToTable(int rowNumber, int sequenceNumber, double[] resultValues) {
		logService.info(this.getClass().getName() + " Writing to the table...");
		
		int row = rowNumber;
		int tableColStart = 0;
		int tableColEnd   = 0;
		int tableColLast  = 0;
		
		// 0 RR, 1 DET, 2 Lmean, 3 Lmax
		// fill table with values
		tableOut.appendRow();
		tableOut.set(0, row, tableInName);//File Name
		if (sliceLabels != null)  tableOut.set(1, row, tableIn.getColumnHeader(sequenceNumber)); //Column Name
		tableOut.set(2, row, choiceRadioButt_SequenceRange); //Sequence Method
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
		tableOut.set( 6, row, booleanSkipZeroes); //Zeroes removed	
		//tableOut.set( 7, row, spinnerInteger_KMax);      //KMax
		//tableOut.set( 8, row, spinnerInteger_RegMin);    //RegMin
		//tableOut.set( 9, row, spinnerInteger_RegMax);    //RegMax	
		//tableOut.set(7,  row, choiceRadioButt_Algorithm);  //Algorithm	
		tableOut.set(7,  row, spinnerInteger_EmbDim);      //EmbDim	
		tableOut.set(8,  row, spinnerInteger_Tau);         //Tau	
		tableOut.set(9,  row, spinnerFloat_Eps);           //Eps	
		tableOut.set(10, row, choiceRadioButt_Norm);       //Norm	
		//tableOut.set(12, row, spinnerInteger_NumPointPairs); //# Point pairs
		tableColLast = 10;
		
		if (resultValues == null) { //set missing result values to NaN
			tableColStart = tableColLast + 1;
			tableColEnd = tableOut.getColumnCount() - 1;
			for (int c = tableColStart; c <= tableColEnd; c++ ) {
				tableOut.set(c, row, Double.NaN);
			}
		}
		else { //set result values
			tableColStart = tableColLast + 1;
			tableColEnd = tableColStart + resultValues.length - 1;
			for (int c = tableColStart; c <= tableColEnd; c++ ) {
				tableOut.set(c, row, resultValues[c-tableColStart]);
			}
		}
	}

	/**
	 * shows the result table
	 */
	private void showTable() {
		// Show table
		uiService.show(tableOutName, tableOut);
	}
	
	/**
	*
	* Processing
	*/
	private double[] process(DefaultGenericTable dgt, int col) { //  c column number
	
		if (dgt == null) {
			logService.info(this.getClass().getName() + " WARNING: dgt==null, no sequence for processing!");
		}

		String  sequenceRange  = choiceRadioButt_SequenceRange;
		String  surrType       = choiceRadioButt_SurrogateType;
		numSurrogates          = spinnerInteger_NumSurrogates;
		numBoxLength           = spinnerInteger_BoxLength;
		int     numDataPoints  = dgt.getRowCount();
		//int   numKMax        = spinnerInteger_KMax;
		//int   regMin         = spinnerInteger_RegMin;
		//int   regMax         = spinnerInteger_RegMax;
		//String  algorithm      = choiceRadioButt_Algorithm; //"RQA" "CrossRQA"
		String  algorithm      = "RQA";
		int     embDim	       = spinnerInteger_EmbDim;
		int     tau			   = spinnerInteger_Tau;
		//int  sampFrequ	   = spinnerInteger_SampFrequ;
		float   eps 		   = spinnerFloat_Eps;
		String  norm           = choiceRadioButt_Norm;
		//int     numPointPairs  = spinnerInteger_NumPointPairs; //????
		boolean skipZeroes     = booleanSkipZeroes;
		//boolean optShowPlot    = booleanShowDoubleLogPlot;
		boolean optShowReccurenceMatrix = booleanShowReccurenceMatrix;
			
		//************************************************************************************************
		//Point pairs with position distances (not value distances!) <= periodMean are not considered 
		//NOTE: Must be at least 0 to remove point pairs constructed of one single point with value distance = 0
		//periodMean >= 0: 
		int periodMean = 0;
		//*************************************************************************************************
		
		double[] resultValues = new double[9]; // // 0 RR, 1 DET, 2 RATIO, 3 DIV, 4 Lmean, 5 Lmax, 6 Ent, 7 LAM,  8 TT
		for (int r = 0; r<resultValues.length; r++) resultValues[r] = Double.NaN;

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
		//for (int n = 0; n < numDataPoints; n++) domain1D[n] = n+1;
			
		
		float[] rqaValues = null;
		//float rr;//float
		//int   lMax;//int
		//float det;//float
		
		//Prepare RQA
		boolean isDiagonal = true;
		RecurrentMatrix rm;
		
		//Copy to LimitedQueue
		//This may be skipped for a future release
		LimitedQueue<Float> timeSeries = new LimitedQueue<Float>(sequence1D.length);//Initiate data
		for (int t = 0; t < sequence1D.length; t++) {
			 timeSeries.add((float)sequence1D[t]);
			 
		}
		
	
		//"Entire sequence", "Subsequent boxes", "Gliding box" 
		//********************************************************************************************************
		if (sequenceRange.equals("Entire sequence")){	
			if (surrType.equals("No surrogates")) {
				resultValues = new double[9]; // RR, DET, RATIO, DIV, Lmean, Lmax, ENT, LAM, TT	
			} else {
				resultValues = new double[9+9+9*numSurrogates]; // RR_Surr, DET_Surr, Lmin_Surr, Lmax_Surr, RR_Surr1, Det_Surr1, Lmean_Surr1,...Surr2...Surr3...
			}
			for (int r = 0; r < resultValues.length; r++) resultValues[r] = Double.NaN;
			//logService.info(this.getClass().getName() + " Column #: "+ (col+1) + "  " + sequenceColumn.getHeader() + "  Size of sequence = " + sequence1D.length);
			//if (sequence1D.length == 0) return null; //e.g. if sequence had only NaNs
			
			if (sequence1D.length > (tau * 2)) { // only data series which are large enough
				
				if (algorithm.equals("RQA")) {
					
					rm = new RecurrentMatrix(timeSeries, embDim, tau, eps, isDiagonal, norm);
					rqaValues = rm.getRQAValues();
					
					if (optShowReccurenceMatrix) {
					
						reccurentMatrix = rm.getRecurrentMatrix();
						int matrixWidth  = reccurentMatrix.length;
						int matrixHeight = reccurentMatrix[0].length;
						
						int maxSize = matrixHeight;
						if (matrixWidth > matrixHeight) maxSize = matrixWidth;
						
						//define maximal size for display
						int limitSize = 500;
						
						if (maxSize > limitSize) { //get a smaller image copy of the matrix
			
							float factor = 0;
							//get factor
							if (matrixWidth > matrixHeight) factor = (float)limitSize/(float)matrixWidth;  
							else                            factor = (float)limitSize/(float)matrixHeight;
							//get new size
							int smallWidth  = (int)(factor*(float)matrixWidth);
							int smallHeight = (int)(factor*(float)matrixHeight);
							
							//get down-sampled image of recurrence matrix
							imgRecurrentMatrix = new ArrayImgFactory<>(new UnsignedByteType()).create(smallWidth, smallHeight);
					    	Cursor<UnsignedByteType> cursor = imgRecurrentMatrix.cursor();
					    	final long[] pos = new long[2];
					    	float value;
					    	int posX;
					    	int posY;
							while (cursor.hasNext()) {
								cursor.fwd();
								cursor.localize(pos);
								posX = (int)((float)(pos[0])/factor);
								posY = matrixHeight - 1 - (int)((float)pos[1]/factor);
								//if (posY>1017) logService.info(this.getClass().getName() + " posX:" + posX + "     posY:" + posY);
								if ((posX < matrixWidth)&&(posY < matrixHeight)) {//positions should be safe, just to be sure
									value = reccurentMatrix[posX][posY]; //not checked if borders are really reached
									if (value == 1) cursor.get().setReal(255);
								}
							}
							
						} else { //original size
							//get image of recurrence matrix
							imgRecurrentMatrix = new ArrayImgFactory<>(new UnsignedByteType()).create(matrixWidth, matrixHeight);
					    	Cursor<UnsignedByteType> cursor = imgRecurrentMatrix.cursor();
					    	final long[] pos = new long[2];
					    	float value;
							while (cursor.hasNext()) {
								cursor.fwd();
								cursor.localize(pos);
								value = reccurentMatrix[(int)pos[0]][matrixHeight - 1 - (int)pos[1]];
								if (value == 1) cursor.get().setReal(255);
							}
						}
						uiService.show(String.valueOf(col+1) + " Recurrence matrix " + matrixWidth + "x" + matrixHeight, imgRecurrentMatrix);
					}
				}
				else if (algorithm.equals("CrossRQA")) {
					//TODO
				}
					
				resultValues[0] = rqaValues[0];  //RR
				resultValues[1] = rqaValues[1];  //DET
				resultValues[2] = rqaValues[1]/rqaValues[0]; //RATIO
				resultValues[3] = 1.0/rqaValues[3];          //DIV
				resultValues[4] = rqaValues[2];  //Lmean
				resultValues[5] = rqaValues[3];  //Lmax
				resultValues[6] = rqaValues[4];  //ENT
				resultValues[7] = rqaValues[5];  //LAM
				resultValues[8] = rqaValues[6];  //TT
				int lastMainResultsIndex = 8;
				
				if (!surrType.equals("No surrogates")) { //Add surrogate analysis
					surrSequence1D = new double[sequence1D.length];
					
					double sumRR    = 0.0;
					double sumDET   = 0.0;
					double sumRATIO = 0.0;
					double sumDIV   = 0.0;
 					double sumLMean = 0.0;
					double sumLMax  = 0.0;
					double sumENT   = 0.0;
					double sumLAM   = 0.0;
					double sumTT    = 0.0;
					
					Surrogate1D surrogate1D = new Surrogate1D();
					String windowingType = "Rectangular";
					for (int s = 0; s < numSurrogates; s++) {
						//choices = {"No surrogates", "Shuffle", "Gaussian", "Random phase", "AAFT"}, 
						if (surrType.equals("Shuffle"))      surrSequence1D = surrogate1D.calcSurrogateShuffle(sequence1D);
						if (surrType.equals("Gaussian"))     surrSequence1D = surrogate1D.calcSurrogateGaussian(sequence1D);
						if (surrType.equals("Random phase")) surrSequence1D = surrogate1D.calcSurrogateRandomPhase(sequence1D, windowingType);
						if (surrType.equals("AAFT"))         surrSequence1D = surrogate1D.calcSurrogateAAFT(sequence1D, windowingType);
				
						
						if (algorithm.equals("RQA")) {
							timeSeries = new LimitedQueue<Float>(surrSequence1D.length);//Initiate data
							for (int t = 0; t < surrSequence1D.length; t++) {
								 timeSeries.add((float)surrSequence1D[t]);
								 
							}
							rm = new RecurrentMatrix(timeSeries, embDim, tau, eps, isDiagonal, norm);
							rqaValues = rm.getRQAValues();
							
						}
						else if (algorithm.equals("CrossRQA")) {
							//TODO
						}					
						resultValues[lastMainResultsIndex + 10 +                 + s] = rqaValues[0];
						resultValues[lastMainResultsIndex + 10 +   numSurrogates + s] = rqaValues[1];
						resultValues[lastMainResultsIndex + 10 + 2*numSurrogates + s] = rqaValues[1]/rqaValues[0]; //RATIO
						resultValues[lastMainResultsIndex + 10 + 3*numSurrogates + s] = 1.0/rqaValues[3];          //DIV
						resultValues[lastMainResultsIndex + 10 + 4*numSurrogates + s] = rqaValues[2];
						resultValues[lastMainResultsIndex + 10 + 5*numSurrogates + s] = rqaValues[3];
						resultValues[lastMainResultsIndex + 10 + 6*numSurrogates + s] = rqaValues[4];
						resultValues[lastMainResultsIndex + 10 + 7*numSurrogates + s] = rqaValues[5];
						resultValues[lastMainResultsIndex + 10 + 8*numSurrogates + s] = rqaValues[6];
						
				
						sumRR    += rqaValues[0];
						sumDET   += rqaValues[1];
						sumRATIO += rqaValues[1]/rqaValues[0]; //RATIO
						sumDIV   += 1.0/rqaValues[3];          //DIV
						sumLMean += rqaValues[2];
						sumLMax  += rqaValues[3];
						sumENT   += rqaValues[4];
						sumLAM   += rqaValues[5];
						sumTT    += rqaValues[6];
					}
					resultValues[lastMainResultsIndex + 1] = sumRR/numSurrogates;
					resultValues[lastMainResultsIndex + 2] = sumDET/numSurrogates;
					resultValues[lastMainResultsIndex + 3] = sumRATIO/numSurrogates;
					resultValues[lastMainResultsIndex + 4] = sumDIV/numSurrogates;
					resultValues[lastMainResultsIndex + 5] = sumLMean/numSurrogates;
					resultValues[lastMainResultsIndex + 6] = sumLMax/numSurrogates;
					resultValues[lastMainResultsIndex + 7] = sumENT/numSurrogates;
					resultValues[lastMainResultsIndex + 8] = sumLAM/numSurrogates;
					resultValues[lastMainResultsIndex + 9] = sumTT/numSurrogates;
				}	
			} 
		//********************************************************************************************************	
		} else if (sequenceRange.equals("Subsequent boxes")){
			resultValues = new double[(int) (2*numSubsequentBoxes)]; // RR DET == two* number of boxes		
			for (int r = 0; r<resultValues.length; r++) resultValues[r] = Double.NaN;
			subSequence1D = new double[(int) numBoxLength];
			//number of boxes may be smaller than intended because of NaNs or removed zeroes
			long actualNumSubsequentBoxes = (long) Math.floor((double)sequence1D.length/(double)spinnerInteger_BoxLength);
		
			//get sub-sequences and compute dimensions
			for (int i = 0; i < actualNumSubsequentBoxes; i++) {	
				logService.info(this.getClass().getName() + " Processing subsequent box #: "+(i+1) + "/" + actualNumSubsequentBoxes);	
				int start = (i*numBoxLength);
				for (int ii = start; ii < (start + numBoxLength); ii++){ 
					subSequence1D[ii-start] = sequence1D[ii];
				}
				timeSeries = new LimitedQueue<Float>(subSequence1D.length);//Initiate data
				for (int t = 0; t < subSequence1D.length; t++) {
					 timeSeries.add((float)subSequence1D[t]); 
				}
				//Compute specific values************************************************
				if (subSequence1D.length > (tau * 2)) { // only data series which are large enough
					
					if (algorithm.equals("RQA")) {
						rm = new RecurrentMatrix(timeSeries, embDim, tau, eps, isDiagonal, norm);
						rqaValues = rm.getRQAValues();
					}
					else if (algorithm.equals("CrossRQA")) {
					
					}
					
					
					//if (optShowPlot){ //show all plots
//					if ((optShowPlot) && (i==0)){ //show only first plot
//					}
					//only RR and DET
					resultValues[i]                             = rqaValues[0]; //RR
					resultValues[(int)(i + numSubsequentBoxes)] = rqaValues[1]; //DET	
				} 
				//***********************************************************************
			}	
		//********************************************************************************************************			
		} else if (sequenceRange.equals("Gliding box")){
			resultValues = new double[(int) (2*numGlidingBoxes)]; // RR DET == two * number of boxes	
			for (int r = 0; r<resultValues.length; r++) resultValues[r] = Double.NaN;
			subSequence1D = new double[(int) numBoxLength];
			//number of boxes may be smaller because of NaNs or removed zeroes
			long actualNumGlidingBoxes = sequence1D.length - spinnerInteger_BoxLength + 1;
			
			//get sub-sequences and compute dimensions
			for (int i = 0; i < actualNumGlidingBoxes; i++) {
				logService.info(this.getClass().getName() + " Processing gliding box #: "+(i+1) + "/" + actualNumGlidingBoxes);	
				int start = i;
				for (int ii = start; ii < (start + numBoxLength); ii++){ 
					subSequence1D[ii-start] = sequence1D[ii];
				}	
				timeSeries = new LimitedQueue<Float>(subSequence1D.length);//Initiate data
				for (int t = 0; t < subSequence1D.length; t++) {
					 timeSeries.add((float)subSequence1D[t]);
					 
				}
				//Compute specific values************************************************
				if (subSequence1D.length > (tau * 2)) { // only data series which are large enough
					
					if (algorithm.equals("RQA")) {
						rm = new RecurrentMatrix(timeSeries, embDim, tau, eps, isDiagonal, norm);
						rqaValues = rm.getRQAValues();
					}
					else if (algorithm.equals("CrossRQA")) {
					
					}
									
					//if (optShowPlot){ //show all plots
//					if ((optShowPlot) && (i==0)){ //show only first plot
//						String preName = sequenceColumn.getHeader() + "-Box #" + (i+1);
//					}	
					//only RR and DET
					resultValues[i]                          = rqaValues[0]; //RR;
					resultValues[(int)(i + numGlidingBoxes)] = rqaValues[1]; //DET		
				}
				//***********************************************************************
			}
		}
		
		return resultValues;
		// RR, LMax, Det
		// Output
		// uiService.show(tableOutName, table);
	}

	// This method shows the double log plot
	private void showPlot(double[] dataX, double[] dataY, String preName, int col, int regMin, int regMax) {
		if (dataX == null) {
			logService.info(this.getClass().getName() + " lnDataX == null, cannot display the plot!");
			return;
		}
		if (dataY == null) {
			logService.info(this.getClass().getName() + " lnDataY == null, cannot display the plot!");
			return;
		}
		if (col < 0) {
			logService.info(this.getClass().getName() + " col < 0, cannot display the plot!");
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
			preName += "Col" + String.format("%03d", col) + "-";
		}
		boolean isLineVisible = false; // ?
		RegressionPlotFrame doubleLogPlot = DisplayRegressionPlotXY(dataX, dataY, isLineVisible,
				"Plot - RQA", preName + "-" + tableInName, "Delay k", "<ln(divergence)>", "", regMin, regMax);
		doubleLogPlotList.add(doubleLogPlot);
		
	}
	
	// This method shows the double log plot
	private void showPlots(double[] dataX, double[][] dataY, String preName, int col, int regMin, int regMax) {
		if (dataX == null) {
			logService.info(this.getClass().getName() + " lnDataX == null, cannot display the plot!");
			return;
		}
		if (dataY == null) {
			logService.info(this.getClass().getName() + " lnDataY == null, cannot display the plot!");
			return;
		}
		if (col < 0) {
			logService.info(this.getClass().getName() + " col < 0, cannot display the plot!");
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
			preName += "Col" + String.format("%03d", col) + "-";
		}
		boolean isLineVisible = false; // ?
		String[] legendLabels = new String[dataY.length];
		for (int l = 0; l < dataY.length; l++) legendLabels[l] = String.valueOf(l+1);
		RegressionPlotFrame doubleLogPlot = DisplayRegressionPlotXY(dataX, dataY, isLineVisible,
				"Plot - RQqs", preName + "-" + tableInName, "Delay k", "<ln(divergence)>", legendLabels, regMin, regMax);
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
	 * @param regMin                minimum value for regression range
	 * @param regMax                maximal value for regression range
	 * @param optDeleteExistingPlot option if existing plot should be deleted before
	 *                              showing a new plot
	 * @param interpolType          The type of interpolation
	 * @return RegressionPlotFrame
	 */
	private RegressionPlotFrame DisplayRegressionPlotXY(double[] dataX, double[] dataY,
			boolean isLineVisible, String frameTitle, String plotLabel, String xAxisLabel, String yAxisLabel, String legendLabel,
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
	private RegressionPlotFrame DisplayRegressionPlotXY(double[] dataX, double[][] dataY,
			boolean isLineVisible, String frameTitle, String plotLabel, String xAxisLabel, String yAxisLabel, String[] legendLabels,
			int regMin, int regMax) {
		// jFreeChart
		RegressionPlotFrame pl = new RegressionPlotFrame(dataX, dataY, isLineVisible, frameTitle, plotLabel, xAxisLabel,
				yAxisLabel, legendLabels, regMin, regMax);
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
		ij.command().run(Csaj1DOpener.class, true).get().getOutput(tableInName);
		//open and run Plugin
		ij.command().run(Csaj1DRQA.class, true);
	}
}
