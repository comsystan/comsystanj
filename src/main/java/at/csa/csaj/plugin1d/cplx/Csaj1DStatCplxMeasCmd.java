/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj1DStatCplxMeasCmd.java
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

package at.csa.csaj.plugin1d.cplx;

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

import org.apache.commons.math3.random.EmpiricalDistribution;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
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

import at.csa.csaj.commons.CsajAlgorithm_ProbabilityDistance;
import at.csa.csaj.commons.CsajAlgorithm_ShannonEntropy;
import at.csa.csaj.commons.CsajAlgorithm_Surrogate1D;
import at.csa.csaj.commons.CsajCheck_ItemIn;
import at.csa.csaj.commons.CsajDialog_WaitingWithProgressBar;
import at.csa.csaj.commons.CsajPlot_SequenceFrame;
import at.csa.csaj.commons.CsajContainer_ProcessMethod;
import at.csa.csaj.plugin1d.misc.Csaj1DOpenerCmd;


/**
 * A {@link ContextCommand} plugin computing <Statistical complexity measures</a>
 * 
 * 
 * *****************************************************
 * For Surrogates and Subsequent/Gliding boxes:
 * Chose an SCM type
 * Set min and max to the same value.
 * Actually the min value is taken for computation
 * ******************************************************  
 * 
 */
@Plugin(type = ContextCommand.class, 
		headless = true,
		label = "Statistical complexity measures",
		initializer = "initialPluginLaunch",
		iconPath = "/icons/comsystan-logo-grey46-16x16.png", //Menu entry icon
		menu = {}) //Space at the end of the label is necessary to avoid duplicate with 2D plugin 

public class Csaj1DStatCplxMeasCmd<T extends RealType<T>> extends ContextCommand implements Previewable {

	private static final String PLUGIN_LABEL            = "<html><b>Statistical complexity measures</b></html>";
	private static final String SPACE_LABEL             = "";
	private static final String SCMOPTIONS_LABEL        = "<html><b>SCM options</b></html>";
	private static final String ANALYSISOPTIONS_LABEL   = "<html><b>Analysis options</b></html>";
	private static final String BACKGROUNDOPTIONS_LABEL = "<html><b>Background option</b></html>";
	private static final String DISPLAYOPTIONS_LABEL    = "<html><b>Display option</b></html>";
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
	
	// data arrays		
	private static double scm_e;
	private static double scm_w;
	private static double scm_k;
	private static double scm_j;
	private static double shannonH;
	private static double d_e;
	private static double d_w;
	private static double d_k;
	private static double d_j;

	double[] probabilities         = null; //pi's
	double[] probabilitiesSurrMean = null; //pi's
	
	private static ArrayList<CsajPlot_SequenceFrame> genRenyiPlotList = new ArrayList<CsajPlot_SequenceFrame>();
	
	public static final String TABLE_OUT_NAME = "Table - Generalised entropies";
	
	CsajDialog_WaitingWithProgressBar dlgProgress;
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

//	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
//	private final String labelSpace = SPACE_LABEL;
	
	@Parameter(type = ItemIO.INPUT)
	private DefaultTableDisplay  defaultTableDisplay;

	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelEntropyOptions = SCMOPTIONS_LABEL;
	
	@Parameter(label = "Probability type",
			   description = "Selection of probability type",
			   style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
			   choices = {"Sequence values", "Pairwise differences", "Sum of differences", "SD"}, 
			   persist = false,  //restore previous value default = true
			   initializer = "initialProbabilityType",
			   callback = "callbackProbabilityType")
	private String choiceRadioButt_ProbabilityType;
	
	@Parameter(label = "Lag",
			   description = "Delta (difference) between two data points",
			   style = NumberWidget.SPINNER_STYLE,
			   min = "1",
			   max = "1000000",
			   stepSize = "1",
			   persist = false, // restore  previous value  default  =  true
			   initializer = "initialLag",
			   callback = "callbackLag")
	private int spinnerInteger_Lag;

	@Parameter(label = "Normalise H",
			   description = "Normalisation of Shannon entropy H - recommended",
		       persist = true,  //restore previous value default = true
		       initializer = "initialNormaliseH")
	 private boolean booleanNormaliseH;
	
	@Parameter(label = "Normalise D",
		       description = "Normalisation of statistical distribution distance D - recommended",
		       persist = true,  //restore previous value default = true
		       initializer = "initialNormaliseD")
	 private boolean booleanNormaliseD;
	
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
	
	@Parameter(label = "(Surr/Box) SCM type",
			   description = "SCM type for Surrogates, Subsequent boxes or Gliding box",
			   style = ChoiceWidget.LIST_BOX_STYLE,
			   choices = {"SCM_E", "SCM_W", "SCM_K", "SCM_J"}, 
			   persist = true,  //restore previous value default = true
			   initializer = "initialSCMType",
			   callback = "callbackSCMType")
	private String choiceRadioButt_SCMType;
	
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
	
	protected void initialProbabilityType() {
		choiceRadioButt_ProbabilityType = "Sequence values"; //"Sequence values", "Pairwise differences", "Sum of differences", "SD"
	} 
	
	protected void initialLag() {
		spinnerInteger_Lag = 1;
	}
	
	protected void initialNormaliseH() {
		booleanNormaliseH = true;
	}
	
	protected void initialNormaliseD() {
		booleanNormaliseD = true;
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
	
	protected void initialSCMType() {
		choiceRadioButt_SCMType = "SCM_E"; //"SCM_E", "SCM_W", "SCM_K", "SCM_J"
	} 
	
	protected void initialSkipZeroes() {
		booleanSkipZeroes = false;
	}	
	
	protected void initialOverwriteDisplays() {
    	booleanOverwriteDisplays = true;
}
	
	protected void initialNumColumn() {
		spinnerInteger_NumColumn = 1;
	}

	// ------------------------------------------------------------------------------
	/** Executed whenever the {@link #choiceRadioButt_ProbabilityType} parameter changes. */
	protected void callbackProbabilityType() {
		logService.info(this.getClass().getName() + " Propability type set to " + choiceRadioButt_ProbabilityType);
		if (choiceRadioButt_ProbabilityType.contains("Sequence values")) {
			logService.info(this.getClass().getName() + " Sequence values only with lag = 1");
			spinnerInteger_Lag = 1;
		}
	}
	
	/** Executed whenever the {@link #spinnerInteger_Lag} parameter changes. */
	protected void callbackLag() {
		if (choiceRadioButt_ProbabilityType.contains("Sequence values")) {
			logService.info(this.getClass().getName() + " Sequence values only with lag = 1");
			spinnerInteger_Lag = 1;
		}
		logService.info(this.getClass().getName() + " Lag set to " + spinnerInteger_Lag);
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

	/** Executed whenever the {@link #choiceRadioButt_SCMType} parameter changes. */
	protected void callbackSCMType() {
		logService.info(this.getClass().getName() + " SCM type for surrogate or box set to " + choiceRadioButt_SCMType);
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
		
		//Set field variables

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
	
		dlgProgress = new CsajDialog_WaitingWithProgressBar("Computing Generalised entropies, please wait... Open console window for further info.",
							logService, false, exec); //isCanceable = false, because no following method listens to exec.shutdown 
		dlgProgress.updatePercent("");
		dlgProgress.setBarIndeterminate(true);
		dlgProgress.setVisible(true);
	
    	logService.info(this.getClass().getName() + " Processing single sequence");
    	deleteExistingDisplays();
		generateTableHeader();
		if (spinnerInteger_NumColumn <= numColumns) processSingleInputColumn(spinnerInteger_NumColumn - 1);
		dlgProgress.addMessage("Processing finished!");		
		dlgProgress.setVisible(false);
		dlgProgress.dispose();
		Toolkit.getDefaultToolkit().beep();
	}

	/**
	* This method starts the workflow for all columns of the active display
	*/
	protected void startWorkflowForAllColumns() {
	
		dlgProgress = new CsajDialog_WaitingWithProgressBar("Computing Generalised entropies, please wait... Open console window for further info.",
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
		tableOut.add(new IntColumn("Surrogates #"));
		tableOut.add(new IntColumn("Box length"));
		tableOut.add(new BoolColumn("Skip zeroes"));
	
		tableOut.add(new GenericColumn("Probability type"));	
		tableOut.add(new IntColumn("Lag"));
		tableOut.add(new BoolColumn("Nomalised H"));
		tableOut.add(new BoolColumn("Nomalised D"));
			
		//"Entire sequence", "Subsequent boxes", "Gliding box" 
		if (choiceRadioButt_SequenceRange.equals("Entire sequence")){
			
			if (choiceRadioButt_SurrogateType.equals("No surrogates")) {
				//"SCM_E", "SCM_W", "SCM_K", "SCM_J"
				tableOut.add(new DoubleColumn("SCM_E"));
				tableOut.add(new DoubleColumn("SCM_W"));
				tableOut.add(new DoubleColumn("SCM_K"));
				tableOut.add(new DoubleColumn("SCM_J"));
				tableOut.add(new DoubleColumn("H"));
				tableOut.add(new DoubleColumn("D_E"));
				tableOut.add(new DoubleColumn("D_W"));
				tableOut.add(new DoubleColumn("D_K"));
				tableOut.add(new DoubleColumn("D_J"));
				
			} else { //Surrogates	
				if (choiceRadioButt_SCMType.equals("SCM_E")) {
					tableOut.add(new DoubleColumn("SCM_E"));
					tableOut.add(new DoubleColumn("SCM_E_Surr"));  //Mean surrogate value	
					for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("SCM_E_Surr#"+(s+1))); 
				}
				else if (choiceRadioButt_SCMType.equals("SCM_W")) {
					tableOut.add(new DoubleColumn("SCM_W"));
					tableOut.add(new DoubleColumn("SCM_W_Surr"));  //Mean surrogate value	
					for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("SCM_W_Surr#"+(s+1))); 
				}
				else if (choiceRadioButt_SCMType.equals("SCM_K")) {
					tableOut.add(new DoubleColumn("SCM_K"));
					tableOut.add(new DoubleColumn("SCM_K_Surr"));  //Mean surrogate value	
					for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("SCM_K_Surr#"+(s+1))); 
				}
				else if (choiceRadioButt_SCMType.equals("SCM_J")) {
					tableOut.add(new DoubleColumn("SCM_J"));
					tableOut.add(new DoubleColumn("SCM_J_Surr"));  //Mean surrogate value	 
					for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("SCM_J_Surr#"+(s+1))); 
				}
		
			}
		} 
		else if (choiceRadioButt_SequenceRange.equals("Subsequent boxes")){
			String scmHeader = "";
			if      (choiceRadioButt_SCMType.equals("SCM_E"))      {scmHeader = "SCM_E";}
			else if (choiceRadioButt_SCMType.equals("SCM_W"))      {scmHeader = "SCM_W";}
			else if (choiceRadioButt_SCMType.equals("SCM_K"))      {scmHeader = "SCM_K";}
			else if (choiceRadioButt_SCMType.equals("SCM_J"))      {scmHeader = "SCM_J";}
		
				
			for (int n = 1; n <= numSubsequentBoxes; n++) {
				tableOut.add(new DoubleColumn(scmHeader+"-#" + n));	
			}	
		}
		else if (choiceRadioButt_SequenceRange.equals("Gliding box")){
			String scmHeader = "";
			if      (choiceRadioButt_SCMType.equals("SCM_E"))      {scmHeader = "SCM_E";}
			else if (choiceRadioButt_SCMType.equals("SCM_W"))      {scmHeader = "SCM_W";}
			else if (choiceRadioButt_SCMType.equals("SCM_K"))      {scmHeader = "SCM_K";}
			else if (choiceRadioButt_SCMType.equals("SCM_J"))      {scmHeader = "SCM_J";}
		
			for (int n = 1; n <= numGlidingBoxes; n++) {
				tableOut.add(new DoubleColumn(scmHeader+"-#" + n));	
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
			if (genRenyiPlotList != null) {
				for (int l = 0; l < genRenyiPlotList.size(); l++) {
					genRenyiPlotList.get(l).setVisible(false);
					genRenyiPlotList.get(l).dispose();
					//genDimPlotList.remove(l);  /
				}
				genRenyiPlotList.clear();		
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

	/** This method takes the column at position c and computes results. 
	 * 
	 */
	private void processSingleInputColumn (int c) {
		
		long startTime = System.currentTimeMillis();
		
		// Compute result values
		CsajContainer_ProcessMethod containerPM = process(tableIn, c); 
		// 
		logService.info(this.getClass().getName() + " Gen entropy SE: " + containerPM.item1_Values[0]);
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
				// 0 Entropy
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
		
		// 0 Entropy
		// fill table with values
		tableOut.appendRow();
		tableOut.set(0, row, tableInName);//File Name
		if (columnLabels != null)  tableOut.set(1, row, tableIn.getColumnHeader(sequenceNumber)); //Column Name
	
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
		tableOut.set(6, row, booleanSkipZeroes); //Zeroes removed
		
		tableOut.set(7, row, choiceRadioButt_ProbabilityType);
		tableOut.set(8, row, spinnerInteger_Lag);    // Lag
		tableOut.set(9, row, booleanNormaliseH);    
		tableOut.set(10, row, booleanNormaliseD);    
		tableColLast = 10;
		
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
		
		String  sequenceRange = choiceRadioButt_SequenceRange;
		String  surrType      = choiceRadioButt_SurrogateType;
		numSurrogates         = spinnerInteger_NumSurrogates;
		numBoxLength          = spinnerInteger_BoxLength;
		int     numDataPoints = dgt.getRowCount();
		String  probType      = choiceRadioButt_ProbabilityType;
		int     lag           = spinnerInteger_Lag;
		boolean normaliseH    = booleanNormaliseH;
		boolean normaliseD    = booleanNormaliseD;
		boolean skipZeroes    = booleanSkipZeroes;
		boolean skipZeroBin   = false;
		
		//For 1D signals the bin 0 must not be the value 0
		//Skipping zeores is done directly for the input sequence, see below 
		
		// data values		
		scm_e = 0.0;
		scm_w = 0.0;
		scm_k = 0.0;
		scm_j = 0.0;
		shannonH = 0.0;
		d_e = 0.0;
		d_w = 0.0;
		d_k = 0.0;
		d_j = 0.0;
		
		int numOfMeasures = 9;
		
		double[] resultValues = new double[numOfMeasures]; // 
		for (int r = 0; r < resultValues.length; r++) resultValues[r] = Float.NaN;
		
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
				
		double scmValue = Float.NaN;
		CsajAlgorithm_ShannonEntropy se;
		CsajAlgorithm_ProbabilityDistance pd;
		
		//"Entire sequence", "Subsequent boxes", "Gliding box" 
		//********************************************************************************************************
		if (sequenceRange.equals("Entire sequence")){	
	
			if (surrType.equals("No surrogates")) {		
				resultValues = new double[numOfMeasures]; // 		
				for (int r = 0; r < resultValues.length; r++) resultValues[r] = Float.NaN;
				
				//logService.info(this.getClass().getName() + " Column #: "+ (col+1) + "  " + sequenceColumn.getHeader() + "  Size of sequence = " + sequence1D.length);	
				//if (sequence1D.length == 0) return null; //e.g. if sequence had only NaNs
				
				probabilities = compProbabilities(sequence1D, lag, probType);				
				se = new CsajAlgorithm_ShannonEntropy(probabilities);
				pd = new CsajAlgorithm_ProbabilityDistance(probabilities);
				
				if (normaliseH) shannonH = se.compNormalisedH(skipZeroBin);
				else            shannonH = se.compH(skipZeroBin);
				
				if (normaliseD) {
					d_e = pd.compNormalisedD_E(skipZeroBin);
					d_w = pd.compNormalisedD_W(skipZeroBin);
					d_k = pd.compNormalisedD_K(skipZeroBin);
					d_j = pd.compNormalisedD_J(skipZeroBin);
				}
				else {
					d_e = pd.compD_E(skipZeroBin);
					d_w = pd.compD_W(skipZeroBin);
					d_k = pd.compD_K(skipZeroBin);
					d_j = pd.compD_J(skipZeroBin);
				}
						
				scm_e = shannonH*d_e;
				scm_w = shannonH*d_w;
				scm_k = shannonH*d_k;
				scm_j = shannonH*d_j;
				
				resultValues[0] = scm_e;
				resultValues[1] = scm_w;
				resultValues[2] = scm_k;
				resultValues[3] = scm_j;
				resultValues[4] = shannonH;	
				resultValues[5] = d_e;
				resultValues[6] = d_w;
				resultValues[7] = d_k;
				resultValues[8] = d_j;		
				
			} else {
				resultValues = new double[1+1+1*numSurrogates]; // Entropy,  Entropy_SurrMean, Entropy_Surr#1, Entropy_Surr#2......
				
				probabilities = compProbabilities(sequence1D, lag, probType);		
				se = new CsajAlgorithm_ShannonEntropy(probabilities);
				pd = new CsajAlgorithm_ProbabilityDistance(probabilities);
				
				//"SCM_E", "SCM_W", "SCM_K", "SCM_J"
				if (choiceRadioButt_SCMType.equals("SCM_E")) {
					
					if (normaliseH) shannonH = se.compNormalisedH(skipZeroBin);
					else            shannonH = se.compH(skipZeroBin);
					
					if (normaliseD) d_e = pd.compNormalisedD_E(skipZeroBin);
					else            d_e = pd.compD_E(skipZeroBin);
						
					scmValue = shannonH*d_e;
				}
				else if (choiceRadioButt_SCMType.equals("SCM_W")) {
					
					if (normaliseH) shannonH = se.compNormalisedH(skipZeroBin);
					else            shannonH = se.compH(skipZeroBin);
					
					if (normaliseD) d_w = pd.compNormalisedD_W(skipZeroBin);
					else            d_w = pd.compD_W(skipZeroBin);
						
					scmValue = shannonH*d_w;
				}
				else if (choiceRadioButt_SCMType.equals("SCM_K")) {
					
					if (normaliseH) shannonH = se.compNormalisedH(skipZeroBin);
					else            shannonH = se.compH(skipZeroBin);
					
					if (normaliseD) d_k = pd.compNormalisedD_K(skipZeroBin);
					else            d_k = pd.compD_K(skipZeroBin);
						
					scmValue = shannonH*d_k;
				}
				else if (choiceRadioButt_SCMType.equals("SCM_J")) {
				
					if (normaliseH) shannonH = se.compNormalisedH(skipZeroBin);
					else               shannonH = se.compH(skipZeroBin);
					
					if (normaliseD) d_j = pd.compNormalisedD_J(skipZeroBin);
					else            d_j = pd.compD_J(skipZeroBin);
						
					scmValue = shannonH*d_j;
				}
				
					
				resultValues[0] = scmValue;
				int lastMainResultsIndex = 0;
				
				surrSequence1D = new double[sequence1D.length];
				
				CsajAlgorithm_Surrogate1D surrogate1D = new CsajAlgorithm_Surrogate1D();
				String windowingType = "Rectangular";
				for (int s = 0; s < numSurrogates; s++) {
					//choices = {"No surrogates", "Shuffle", "Gaussian", "Random phase", "AAFT"}, 	
					if      (surrType.equals("Shuffle"))      surrSequence1D = surrogate1D.calcSurrogateShuffle(sequence1D);
					else if (surrType.equals("Gaussian"))     surrSequence1D = surrogate1D.calcSurrogateGaussian(sequence1D);
					else if (surrType.equals("Random phase")) surrSequence1D = surrogate1D.calcSurrogateRandomPhase(sequence1D, windowingType);
					else if (surrType.equals("AAFT"))         surrSequence1D = surrogate1D.calcSurrogateAAFT(sequence1D, windowingType);
			
					probabilities = compProbabilities(surrSequence1D, lag, probType);
					se = new CsajAlgorithm_ShannonEntropy(probabilities);
					pd = new CsajAlgorithm_ProbabilityDistance(probabilities);
					
					//"SCM_E", "SCM_W", "SCM_K", "SCM_J"
					if (choiceRadioButt_SCMType.equals("SCM_E")) {
						
						if (normaliseH) shannonH = se.compNormalisedH(skipZeroBin);
						else            shannonH = se.compH(skipZeroBin);
						
						if (normaliseD) d_e = pd.compNormalisedD_E(skipZeroBin);
						else            d_e = pd.compD_E(skipZeroBin);
							
						scmValue = shannonH*d_e;
					}
					else if (choiceRadioButt_SCMType.equals("SCM_W")) {
						
						if (normaliseH) shannonH = se.compNormalisedH(skipZeroBin);
						else            shannonH = se.compH(skipZeroBin);
						
						if (normaliseD) d_w = pd.compNormalisedD_W(skipZeroBin);
						else            d_w = pd.compD_W(skipZeroBin);
							
						scmValue = shannonH*d_w;
					}
					else if (choiceRadioButt_SCMType.equals("SCM_K")) {
						
						if (normaliseH) shannonH = se.compNormalisedH(skipZeroBin);
						else            shannonH = se.compH(skipZeroBin);
						
						if (normaliseD) d_k = pd.compNormalisedD_K(skipZeroBin);
						else            d_k = pd.compD_K(skipZeroBin);
							
						scmValue = shannonH*d_k;
					}
					else if (choiceRadioButt_SCMType.equals("SCM_J")) {
					
						if (normaliseH) shannonH = se.compNormalisedH(skipZeroBin);
						else            shannonH = se.compH(skipZeroBin);
						
						if (normaliseD) d_j = pd.compNormalisedD_J(skipZeroBin);
						else            d_j = pd.compD_J(skipZeroBin);
							
						scmValue = shannonH*d_j;
					}						
					resultValues[lastMainResultsIndex + 2 + s] = scmValue;
					
				}
			}
			
		//********************************************************************************************************	
		} else if (sequenceRange.equals("Subsequent boxes")){
			resultValues = new double[(int) (numSubsequentBoxes)]; // only one of possible Entropy values, Dim R2 == two * number of boxes		
			for (int r = 0; r<resultValues.length; r++) resultValues[r] = Float.NaN;
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
				//Compute specific values************************************************
				probabilities = compProbabilities(subSequence1D, lag, probType);	
				se = new CsajAlgorithm_ShannonEntropy(probabilities);
				pd = new CsajAlgorithm_ProbabilityDistance(probabilities);
				
				//"SCM_E", "SCM_W", "SCM_K", "SCM_J"
				if (choiceRadioButt_SCMType.equals("SCM_E")) {
					
					if (normaliseH) shannonH = se.compNormalisedH(skipZeroBin);
					else            shannonH = se.compH(skipZeroBin);
					
					if (normaliseD) d_e = pd.compNormalisedD_E(skipZeroBin);
					else            d_e = pd.compD_E(skipZeroBin);
						
					scmValue = shannonH*d_e;
				}
				else if (choiceRadioButt_SCMType.equals("SCM_W")) {
					
					if (normaliseH) shannonH = se.compNormalisedH(skipZeroBin);
					else            shannonH = se.compH(skipZeroBin);
					
					if (normaliseD) d_w = pd.compNormalisedD_W(skipZeroBin);
					else            d_w = pd.compD_W(skipZeroBin);
						
					scmValue = shannonH*d_w;
				}
				else if (choiceRadioButt_SCMType.equals("SCM_K")) {
					
					if (normaliseH) shannonH = se.compNormalisedH(skipZeroBin);
					else            shannonH = se.compH(skipZeroBin);
					
					if (normaliseD) d_k = pd.compNormalisedD_K(skipZeroBin);
					else            d_k = pd.compD_K(skipZeroBin);
						
					scmValue = shannonH*d_k;
				}
				else if (choiceRadioButt_SCMType.equals("SCM_J")) {
				
					if (normaliseH) shannonH = se.compNormalisedH(skipZeroBin);
					else               shannonH = se.compH(skipZeroBin);
					
					if (normaliseD) d_j = pd.compNormalisedD_J(skipZeroBin);
					else            d_j = pd.compD_J(skipZeroBin);
						
					scmValue = shannonH*d_j;
				}				
				
				resultValues[i] = scmValue;			
				//***********************************************************************
			}	
		//********************************************************************************************************			
		} else if (sequenceRange.equals("Gliding box")){
			resultValues = new double[(int) (numGlidingBoxes)]; // only one of possible Entropy values,  Dim R2 == two * number of boxes	
			for (int r = 0; r<resultValues.length; r++) resultValues[r] = Float.NaN;
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
				//Compute specific values************************************************
				probabilities = compProbabilities(subSequence1D, lag, probType);	
				se = new CsajAlgorithm_ShannonEntropy(probabilities);
				pd = new CsajAlgorithm_ProbabilityDistance(probabilities);
				
				//"SCM_E", "SCM_W", "SCM_K", "SCM_J"
				if (choiceRadioButt_SCMType.equals("SCM_E")) {
					
					if (normaliseH) shannonH = se.compNormalisedH(skipZeroBin);
					else            shannonH = se.compH(skipZeroBin);
					
					if (normaliseD) d_e = pd.compNormalisedD_E(skipZeroBin);
					else            d_e = pd.compD_E(skipZeroBin);
						
					scmValue = shannonH*d_e;
				}
				else if (choiceRadioButt_SCMType.equals("SCM_W")) {
					
					if (normaliseH) shannonH = se.compNormalisedH(skipZeroBin);
					else            shannonH = se.compH(skipZeroBin);
					
					if (normaliseD) d_w = pd.compNormalisedD_W(skipZeroBin);
					else            d_w = pd.compD_W(skipZeroBin);
						
					scmValue = shannonH*d_w;
				}
				else if (choiceRadioButt_SCMType.equals("SCM_K")) {
					
					if (normaliseH) shannonH = se.compNormalisedH(skipZeroBin);
					else            shannonH = se.compH(skipZeroBin);
					
					if (normaliseD) d_k = pd.compNormalisedD_K(skipZeroBin);
					else            d_k = pd.compD_K(skipZeroBin);
						
					scmValue = shannonH*d_k;
				}
				else if (choiceRadioButt_SCMType.equals("SCM_J")) {
				
					if (normaliseH) shannonH = se.compNormalisedH(skipZeroBin);
					else               shannonH = se.compH(skipZeroBin);
					
					if (normaliseD) d_j = pd.compNormalisedD_J(skipZeroBin);
					else            d_j = pd.compD_J(skipZeroBin);
						
					scmValue = shannonH*d_j;
				}				
				
				resultValues[i] = scmValue;		
				//***********************************************************************
			}
		}	
		return new CsajContainer_ProcessMethod(resultValues);
		// SampEn or AppEn
		// Output
		// uiService.show(TABLE_OUT_NAME, table);
	}
	
	//------------------------------------------------------------------------------------------------------
	/**
	 * This computes probabilities of actual values
	 * 
	 * @param sequence
	 * @param lag
	 * @param probOption
	 * @return probabilities[]
	 */
	//"Sequence values", "Pairwise differences", "Sum of differences", "SD"
	private double[] compProbabilities(double[] sequence, int lag, String probType) {
		//if (probType.equals("Sequence values")) lag = 0; //to be sure that eps = 0 for that case
		double sequenceMin = Double.MAX_VALUE;
		double sequenceMax = -Double.MAX_VALUE;
		double[] sequenceDouble = null;
		
		if (probType.equals("Sequence values")) {//Actual values without lag
			sequenceDouble = new double[sequence.length]; 
			for (int i = 0; i < sequenceDouble.length; i++) {
				sequenceDouble[i] = sequence[i];
				if (sequenceDouble[i] < sequenceMin) sequenceMin = sequenceDouble[i];  
				if (sequenceDouble[i] > sequenceMax) sequenceMax = sequenceDouble[i];  
			}
		}
		if (probType.equals("Pairwise differences")) {//Pairwise differences
			sequenceDouble = new double[sequence.length - lag]; 
			for (int i = 0; i < sequenceDouble.length; i++) {
				sequenceDouble[i] = Math.abs(sequence[i+lag] - sequence[i]); //Difference
				if (sequenceDouble[i] < sequenceMin) sequenceMin = sequenceDouble[i];  
				if (sequenceDouble[i] > sequenceMax) sequenceMax = sequenceDouble[i];  
			}
		}
		if (probType.equals("Sum of differences")) {//Sum of differences in between lag == integral
			sequenceDouble = new double[sequence.length - lag]; 
			for (int i = 0; i < sequenceDouble.length; i++) {
				double sum = 0.0;
				for (int ii = 0; ii < lag; ii++) {
					sum = sum + Math.abs(sequence[i+ii+1] - sequence[i+ii]); //Sum of differences
				}
				sequenceDouble[i] = sum;
				if (sequenceDouble[i] < sequenceMin) sequenceMin = sequenceDouble[i];  
				if (sequenceDouble[i] > sequenceMax) sequenceMax = sequenceDouble[i];  
			}
		}
		if (probType.equals("SD")) {//SD in between lag
			sequenceDouble = new double[sequence.length - lag]; 
			for (int i = 0; i < sequenceDouble.length; i++) {
				double mean = 0.0;
				for (int ii = 0; ii < lag; ii++) {
					mean = mean + sequence[i+ii]; //Sum for Mean
				}
				mean = mean/((double)lag);
				double sumDiff2 = 0.0;
				for (int ii = 0; ii <= lag; ii++) {
					sumDiff2 = sumDiff2 + Math.pow(sequence[i+ii] - mean, 2); //Difference
				}	
				sequenceDouble[i] = Math.sqrt(sumDiff2/((double)lag));
				if (sequenceDouble[i] < sequenceMin) sequenceMin = sequenceDouble[i];  
				if (sequenceDouble[i] > sequenceMax) sequenceMax = sequenceDouble[i];  
			}
		}
	
		//Apache
		int binNumber = 1000;
		int binSize = (int) ((sequenceMax - sequenceMin)/binNumber);
		long[] histogram = new long[binNumber];
		EmpiricalDistribution distribution = new EmpiricalDistribution(binNumber);
		distribution.load(sequenceDouble);
		int k = 0;
		for(SummaryStatistics stats: distribution.getBinStats())
		{
		    histogram[k++] = stats.getN();
		}   
		
//	    double xValues[] = new double[binNumber];
//        for (int i = 0; i < binNumber; i++) {
//            if (i == 0){
//                xValues[i] = sequenceMin;
//            } else {
//                xValues[i] = xValues[i-1] + binSize;
//            }
//        }
        double[] pis = new double[binNumber]; 

		double totalsMax = 0.0;
		for (int p= 0; p < binNumber; p++) {
			pis[p] = histogram[p];
			totalsMax = totalsMax + histogram[p]; // calculate total count for normalization
		}	
		
		// normalization
		double sumP = 0.0;
		for (int p = 0; p < pis.length; p++) {	
			pis[p] = pis[p] / totalsMax;
			sumP = sumP + pis[p];
		}
		logService.info(this.getClass().getName() + " Sum of probabilities: " + sumP);
		return pis;
        
	}
	
	/**
	 * This method calculates the mean of a data series
	 * 
	 * @param data1D
	 * @return Double Mean
	 */
	private double calcMean(double[] data1D) {
		double sum = 0;
		for (double d : data1D) {
			sum += d;
		}
		return sum / data1D.length;
	}
	
	/**
	 * This method calculates the mean of a data series
	 * 
	 * @param data1D[numberOfSurrogates][]
	 * @return double Mean
	 */
	private double[] calcSurrMean(double[][] data1D) {
		double surrMean[] = new double [data1D.length];
		for (int p = 0; p <  data1D.length; p++) {	
			for (int n = 0; n < data1D[0].length; n++ ) {
				surrMean[p] = surrMean[p] + data1D[n][p];
			}	
			surrMean[p] = surrMean[p] / data1D[0].length;
		}
		return surrMean;
	}
	
	private int getMaxLag(int length) { // 
		int result = 0;
		if (this.choiceRadioButt_SequenceRange.equals("Entire sequence")) {
			result = (length) / 2;
		}
		else {
			float boxWidth = 1f;
			int number = 1; // inclusive original image
			while (boxWidth <= length) {
				boxWidth = boxWidth * 2;
				number = number + 1;
			}
			result = number - 1;
		}
		return result;
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
	 * Displays a single plot in a separate window.
	 * @param dataX
	 * @param dataY
	 * @param isLineVisible
	 * @param frameTitle
	 * @param plotLabel
	 * @param xAxisLabel
	 * @param yAxisLabel
	 * @param legendLabel
	 * @param numRegStart
	 * @param numRegEnd
	 * @return
	 */
	private CsajPlot_SequenceFrame DisplaySinglePlotXY(double[] dataX, double[] dataY, boolean isLineVisible,
			String frameTitle, String plotLabel, String xAxisLabel, String yAxisLabel, String legendLabel) {
		// jFreeChart
		CsajPlot_SequenceFrame pl = new CsajPlot_SequenceFrame(dataX, dataY, isLineVisible, frameTitle, plotLabel, xAxisLabel,
				yAxisLabel, legendLabel);
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
	
	// ---------------------------------------------------------------------------------------------
	
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
