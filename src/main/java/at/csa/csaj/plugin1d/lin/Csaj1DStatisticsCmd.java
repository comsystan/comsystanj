/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj1DStatisticsCmd.java
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
package at.csa.csaj.plugin1d.lin;

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
import net.imagej.ImageJ;
import net.imagej.ops.OpService;
import net.imglib2.type.numeric.RealType;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
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

import at.csa.csaj.plugin1d.misc.Csaj1DOpenerCmd;
import at.csa.csaj.commons.CsajAlgorithm_Surrogate1D;
import at.csa.csaj.commons.CsajCheck_ItemIn;
import at.csa.csaj.commons.CsajDialog_WaitingWithProgressBar;
import at.csa.csaj.commons.CsajPlot_RegressionFrame;
import at.csa.csaj.commons.CsajContainer_ProcessMethod;

/**
 * A {@link ContextCommand} plugin computing <Statistics</a>
 * of a sequence.
 */
@Plugin(type = ContextCommand.class,
		headless = true,
		label = "Statistics",
		initializer = "initialPluginLaunch",
		iconPath = "/icons/comsystan-logo-grey46-16x16.png", //Menu entry icon
		menu = {}) //Space at the end of the label is necessary to avoid duplicate with 2D plugin 

public class Csaj1DStatisticsCmd<T extends RealType<T>> extends ContextCommand implements Previewable {

	private static final String PLUGIN_LABEL            = "<html><b>Sequence statistics<b/></html>";
	private static final String SPACE_LABEL             = "";
	private static final String ANALYSISOPTIONS_LABEL   = "<html><b>Analysis options</b></html>";
	private static final String BACKGROUNDOPTIONS_LABEL = "<html><b>Background option</b></html>";
	private static final String DISPLAYOPTIONS_LABEL    = "<html><b>Display option</b></html>";
	private static final String PROCESSOPTIONS_LABEL    = "<html><b>Process options</b></html>";

	private static DefaultGenericTable  tableIn;

	private static double[] sequence1D;
	private static double[] domain1D;
	private static Double valueDataPoint;
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
	private static ArrayList<CsajPlot_RegressionFrame> doubleLogPlotList = new ArrayList<CsajPlot_RegressionFrame>();

	public static final String TABLE_OUT_NAME = "Table - Sequence statistics";
	
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
	//private DefaultGenericTable tableIn;

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
			   max = "9999999999999999999999999",
			   stepSize = "1",
			   persist = true, // restore  previous value  default  =  true
			   initializer = "initialNumSurrogates",
			   callback = "callbackNumSurrogates")
	private int spinnerInteger_NumSurrogates;
	
	@Parameter(label = "Box length",
			   description = "Length of subsequent or gliding box",
			   style = NumberWidget.SPINNER_STYLE, 
			   min = "2",
			   max = "9999999999999999999999999",
			   stepSize = "1",
			   persist = true, // restore  previous value  default  =  true
			   initializer = "initialBoxLength", callback = "callbackBoxLength")
	private int spinnerInteger_BoxLength;

	//-----------------------------------------------------------------------------------------------------
//	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
//	private final String labelBackgroundOptions = BACKGROUNDOPTIONS_LABEL;
	
	@Parameter(label = "Skip zero values",
			   persist = true,
		       initializer = "initialSkipZeroes", callback = "callbackSkipZeroes")
	private boolean booleanSkipZeroes;
	
	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelDisplayOptions = DISPLAYOPTIONS_LABEL;

	@Parameter(label = "Overwrite result display(s)",
	    	  description = "Overwrite already existing result images, plots or tables",
	    	  persist = true,  //restore previous value default = true
	    	  initializer = "initialOverwriteDisplays")
	private boolean booleanOverwriteDisplays;

	//************************************************************************
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelProcessOptions = PROCESSOPTIONS_LABEL;

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
    //The following initializer functions set initial values	
	protected void initialPluginLaunch() {
		checkItemIOIn();
	}
    protected void initialSequenceRange() {
    	choiceRadioButt_SequenceRange = "Entire sequence";
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
	protected void initialOverwriteDisplays() {
    	booleanOverwriteDisplays = true;
	}
	protected void initialNumColumn() {
		spinnerInteger_NumColumn = 1;
	}
	
	//-------------------------------------------------------------------------
	
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
	
		dlgProgress = new CsajDialog_WaitingWithProgressBar("Computing sequence statistics, please wait... Open console window for further info.",
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
	
		dlgProgress = new CsajDialog_WaitingWithProgressBar("Computing sequence statistics, please wait... Open console window for further info.",
							logService, false, exec); //isCanceable = true, because processAllInputSequences(dlgProgress) listens to exec.shutdown 
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
	
		//"Entire sequence", "Subsequent boxes", "Gliding box" 
		if (choiceRadioButt_SequenceRange.equals("Entire sequence")){
			tableOut.add(new DoubleColumn("# Data points"));	
			tableOut.add(new DoubleColumn("Min"));	
			tableOut.add(new DoubleColumn("Max"));
			tableOut.add(new DoubleColumn("Median"));
			tableOut.add(new DoubleColumn("RMS"));
			tableOut.add(new DoubleColumn("Mean"));
			tableOut.add(new DoubleColumn("SD"));
			tableOut.add(new DoubleColumn("Kurtosis"));
			tableOut.add(new DoubleColumn("Skewness"));
			tableOut.add(new DoubleColumn("Sum"));
			tableOut.add(new DoubleColumn("Sum of squares"));
			
			
			if (choiceRadioButt_SurrogateType.equals("No surrogates")) {
				//do nothing	
			} else { //Surrogates
				tableOut.add(new DoubleColumn("Median_Surr")); //Mean surrogate value	
				tableOut.add(new DoubleColumn("Mean_Surr")); //Mean surrogate value
				tableOut.add(new DoubleColumn("SD_Surr")); //Mean surrogate value
				for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("Median_Surr-#"+(s+1))); 
				for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("Mean_Surr-#"+(s+1))); 
				for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("SD_Surr-#"+(s+1))); 
			}			
			
		} 
		else if (choiceRadioButt_SequenceRange.equals("Subsequent boxes")){
			for (int n = 1; n <= numSubsequentBoxes; n++) {
				tableOut.add(new DoubleColumn("Median_" + n));	
			}
			for (int n = 1; n <= numSubsequentBoxes; n++) {
				tableOut.add(new DoubleColumn("Mean_" + n));	
			}
			for (int n = 1; n <= numSubsequentBoxes; n++) {
				tableOut.add(new DoubleColumn("SD_" + n));	
			}
		}
		else if (choiceRadioButt_SequenceRange.equals("Gliding box")){
			for (int n = 1; n <= numGlidingBoxes; n++) {
				tableOut.add(new DoubleColumn("Median_" + n));	
			}
			for (int n = 1; n <= numGlidingBoxes; n++) {
				tableOut.add(new DoubleColumn("Mean_" + n));	
			}
			for (int n = 1; n <= numGlidingBoxes; n++) {
				tableOut.add(new DoubleColumn("SD_" + n));	
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

		if (optDeleteExistingTables) {
			List<Display<?>> list = defaultDisplayService.getDisplays();
			for (int i = 0; i < list.size(); i++) {
				Display<?> display = list.get(i);
				//System.out.println("display name: " + display.getName());
				if (display.getName().contains(TABLE_OUT_NAME))
					display.close();
					//This might free some memory
					display = null;
			}
		}
	}

  	/** 
	 * This method takes the single column and computes results. 
	 * @Param int c
	 * */
	private void processSingleInputColumn (int c) {
		
		long startTime = System.currentTimeMillis();
		
		//Compute result values
		CsajContainer_ProcessMethod containerPM = process(tableIn, c); 
		// 0 numDataPoints 1 Min 2 Max 3 Median 4 QuMean 5 Mean 6 SD 7 Kurt 8 Skew 9 Sum 10 SumSqr
		logService.info(this.getClass().getName() + " Mean: " + containerPM.item1_Values[5]);
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
		for (int s = 0; s < numColumns; s++) { // s... number of sequence columns
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
				// 0 numDataPoints 1 Min 2 Max 3 Median 4 QuMean 5 Mean 6 SD 7 Kurt 8 Skew 9 Sum 10 SumSqr
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
		
		// 0 numDataPoints 1 Min 2 Max 3 Median 4 QuMean 5 Mean 6 SD 7 Kurt 8 Skew 9 Sum 10 SumSqr
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
		tableColLast = 6;
		
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
		//This might free some memory
		tableOut = null;
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
		boolean skipZeroes    = booleanSkipZeroes;
		double[] resultValues = null;
		// Get a DescriptiveStatistics instance
		DescriptiveStatistics stats = null;
		
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
		//for (int n = 0; n < numDataPoints; n++) domain1D[n] = n+1;
	
		//"Entire sequence", "Subsequent boxes", "Gliding box"
		//********************************************************************************************************
		if (sequenceRange.equals("Entire sequence")){
			if (surrType.equals("No surrogates")) {
				resultValues = new double[11]; // 0 numDataPoints 1 Min 2 Max 3 Median 4 QuMean 5 Mean 6 SD 7 Kurt 8 Skew 9 Sum 10 SumSqr
			} else {
				resultValues = new double[11+3+3*numSurrogates]; // Median_Surr, Mean_Surr, SD_Surr,	Median_Surr1, Median_Surr2, Median_Surr3, ...... Mean_Surr1  2, 3 .......
			}
			for (int r = 0; r<resultValues.length; r++) resultValues[r] = Double.NaN;
			
			// Get a DescriptiveStatistics instance
			stats = new DescriptiveStatistics();
			// Add the data from the array
			for( int i = 0; i < sequence1D.length; i++) {
				//valueDataPoint = Double.valueOf((Double)column.get(i));
				valueDataPoint = sequence1D[i];
				if (!valueDataPoint.isNaN()) {
						stats.addValue(valueDataPoint);
				}
			}
			// Compute statistics
			resultValues[0] = stats.getN();
			resultValues[1] = stats.getMin();
			resultValues[2] = stats.getMax();
			resultValues[3] = stats.getPercentile(50);
			resultValues[4] = stats.getQuadraticMean();
			resultValues[5] = stats.getMean();		
			resultValues[6] = stats.getStandardDeviation();
			resultValues[7] = stats.getKurtosis();
			resultValues[8] = stats.getSkewness();
			resultValues[9] = stats.getSum();
			resultValues[10] = stats.getSumsq();
			int lastMainResultsIndex = 10;
					
			if (!surrType.equals("No surrogates")) { //Add surrogate analysis
				surrSequence1D = new double[sequence1D.length];
				
				double sumMedians   = 0.0;
				double sumMeans    = 0.0;
				double sumSDs = 0.0;
				CsajAlgorithm_Surrogate1D surrogate1D = new CsajAlgorithm_Surrogate1D();
				String windowingType = "Rectangular";
				for (int s = 0; s < numSurrogates; s++) {
					//choices = {"No surrogates", "Shuffle", "Gaussian", "Random phase", "AAFT"}, 
					if (surrType.equals("Shuffle"))      surrSequence1D = surrogate1D.calcSurrogateShuffle(sequence1D);
					if (surrType.equals("Gaussian"))     surrSequence1D = surrogate1D.calcSurrogateGaussian(sequence1D);
					if (surrType.equals("Random phase")) surrSequence1D = surrogate1D.calcSurrogateRandomPhase(sequence1D, windowingType);
					if (surrType.equals("AAFT"))         surrSequence1D = surrogate1D.calcSurrogateAAFT(sequence1D, windowingType);
			
					// Get a DescriptiveStatistics instance
					stats = new DescriptiveStatistics();
					// Add the data from the array
					for( int i = 0; i < surrSequence1D.length; i++) {
						//valueDataPoint = Double.valueOf((Double)column.get(i));
						valueDataPoint = surrSequence1D[i];
						if (!valueDataPoint.isNaN()) {
								stats.addValue(valueDataPoint);
						}
					}	
					resultValues[lastMainResultsIndex + 4 + s]                    = stats.getPercentile(50);
					resultValues[lastMainResultsIndex + 4 + numSurrogates + s]    = stats.getMean();	
					resultValues[lastMainResultsIndex + 4 + (2*numSurrogates) +s] = stats.getStandardDeviation();			
					sumMedians    +=  stats.getPercentile(50);
					sumMeans     +=  stats.getMean();	
					sumSDs  +=  stats.getStandardDeviation();
				}
				resultValues[lastMainResultsIndex + 1] = sumMedians/numSurrogates;
				resultValues[lastMainResultsIndex + 2] = sumMeans/numSurrogates;
				resultValues[lastMainResultsIndex + 3] = sumSDs/numSurrogates;
			}	
			
		}	
		//********************************************************************************************************
		else if (sequenceRange.equals("Subsequent boxes")){
			resultValues = new double[(int) (3*numSubsequentBoxes)]; // Median Mean SD == three * number of boxes	
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
				//Compute specific values************************************************
				// Get a DescriptiveStatistics instance
				stats = new DescriptiveStatistics();
				for (int ii = 0; ii < numBoxLength; ii++){ 
					valueDataPoint = subSequence1D[ii];
					if (!valueDataPoint.isNaN()) {
							stats.addValue(valueDataPoint);
					}
				}
				resultValues[i]                               = stats.getPercentile(50);
				resultValues[(int)(i +   numSubsequentBoxes)] = stats.getMean();		
				resultValues[(int)(i + 2*numSubsequentBoxes)] = stats.getStandardDeviation();
				//***********************************************************************
			}	
		}
		//********************************************************************************************************
		else if (sequenceRange.equals("Gliding box")){
			resultValues = new double[(int) (3*numGlidingBoxes)]; // Median Mean SD == three * number of boxes		
			for (int r = 0; r<resultValues.length; r++) resultValues[r] = Double.NaN;
			subSequence1D = new double[(int) numBoxLength];
			//number of boxes may be smaller because of NaNs or removed zeroes
			long actualNumGlidingBoxes = sequence1D.length - spinnerInteger_BoxLength + 1;
			//get sub-sequences and push it directly to stats
			for (int i = 0; i < actualNumGlidingBoxes; i++) {
				logService.info(this.getClass().getName() + " Processing gliding box #: "+(i+1) + "/" + actualNumGlidingBoxes);	
				int start = i;
				for (int ii = start; ii < (start + numBoxLength); ii++){ 
					subSequence1D[ii-start] = sequence1D[ii];
				}	
				//Compute specific values************************************************
				// Get a DescriptiveStatistics instance
				stats = new DescriptiveStatistics();
				for (int ii = 0; ii < numBoxLength; ii++){ 
					valueDataPoint = subSequence1D[ii];
					if (!valueDataPoint.isNaN()) {
							stats.addValue(valueDataPoint);
					}
				}
				// Compute statistics	
				resultValues[i]                            = stats.getPercentile(50);
				resultValues[(int)(i +   numGlidingBoxes)] = stats.getMean();		
				resultValues[(int)(i + 2*numGlidingBoxes)] = stats.getStandardDeviation();
				//***********************************************************************
			}
		}
	 	
		return new CsajContainer_ProcessMethod(resultValues);
		// 0 numDataPoints 1 Min 2 Max 3 Median 4 QuMean 5 Mean 6 SD 7 Kurt 8 Skew 9 Sum 10 SumSqr
		// Output
		// uiService.show(TABLE_OUT_NAME, table);
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