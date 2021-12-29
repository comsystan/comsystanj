/*-
 * #%L
 * Project: ImageJ2 signal plugin for computing standard HRV measurements
 * File: SignalHRV.java
 * 
 * $Id$
 * $HeadURL$
 * 
 * This file is part of ComsystanJ software, hereinafter referred to as "this program".
 * %%
 * Copyright (C) 2021 Comsystan Software
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

package at.csa.csaj.sig.hrv;

import java.awt.Toolkit;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.UIManager;
import net.imagej.ImageJ;
import net.imagej.ops.OpService;
import net.imglib2.type.numeric.RealType;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
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
import org.scijava.table.GenericColumn;
import org.scijava.table.IntColumn;
import org.scijava.ui.UIService;
import org.scijava.widget.Button;
import org.scijava.widget.ChoiceWidget;
import org.scijava.widget.NumberWidget;
import at.csa.csaj.commons.signal.algorithms.Surrogate;
import at.csa.csaj.commons.dialog.WaitingDialogWithProgressBar;
import at.csa.csaj.sig.open.SignalOpener;


/**
 * A {@link Command} plugin computing <Standard HRV measurements</a> of a signal.
 * according to
 *  Heart rate variability Standards of measurement, physiological interpretation, and clinical use
 *  Task Force of The European Society of Cardiology and The North American Society of Pacing and Electrophysiology
 *  Malik et al. European Heart Journal (1996) 17,354-381
 *  
 *  and
 *  McNames & Aboy Med Bio Eng Comput (2006) 44:747–756)
 *    
 * From intervals:              
 * SDNN, the standard deviation of NN intervals. Often calculated over a 24-hour period.
 * SDANN, the standard deviation of the average NN intervals calculated over short periods, usually 5 minutes.
 * SDNN is therefore a measure of changes in heart rate due to cycles longer than 5 minutes. SDNN reflects all the cyclic components responsible for variability in the period of recording, therefore it represents total variability.
 * SDNNIndex is the mean of the 5-min standard deviations of the NN interval calculated over 24 h, which measures the variability due to cycles shorter than 5 min.
 * HRVTI The HRV triangular index is a measure of the shape of the NN interval distribution. Generally, uniform distributions representing large variability have large values and distributions with single large peaks have small values. The metric is defined in terms of a histogram of the NN intervals.
 * 
 * From Interval (absolute) differences:
 * RMSSD ("root mean square of successive differences"), the square root of the mean of the squares of the successive differences between adjacent NNs.
 * SDSD ("standard deviation of successive differences"), the standard deviation of the successive differences between adjacent NNs.
 * NN50, the number of pairs of successive NNs that differ by more than 50 ms.
 * pNN50, the proportion of NN50 divided by total number of NNs.
 * NN20, the number of pairs of successive NNs that differ by more than 20 ms.
 * pNN20, the proportion of NN20 divided by total number of NNs.
 * 
 * 
 * *****************************************************
 * For Surrogates and Subsequent/Gliding boxes:
 * Chose a feature
 * Set min and max to the same value.
 * Actually the min value is taken for computation
 * ******************************************************  
 * 
 * @author Ahammer
 * @since  2021 03
 
 */
@Plugin(type = ContextCommand.class, 
	headless = true,
	label = "Standard HRV measurements",
	initializer = "initialPluginLaunch",
	//iconPath = "/images/comsystan-??.png", //Menu entry icon
	menu = {
	@Menu(label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT, mnemonic = MenuConstants.PLUGINS_MNEMONIC),
	@Menu(label = "ComsystanJ"),
	@Menu(label = "Signal"),
	@Menu(label = "Standard HRV measurements", weight = 13)})
//public class SignalHRV<T extends RealType<T>> extends InteractiveCommand { // non blocking  GUI
public class SignalHRV<T extends RealType<T>> extends ContextCommand implements Previewable { //modal GUI with cancel

	private static final String PLUGIN_LABEL              = "<html><b>Standard HRV measurements</b></html>";
	private static final String SPACE_LABEL               = "";
	private static final String MEASUREMENTSOPTIONS_LABEL = "<html><b>Measurement options</b></html>";
	private static final String ANALYSISOPTIONS_LABEL     = "<html><b>Analysis options</b></html>";
	private static final String BACKGROUNDOPTIONS_LABEL   = "<html><b>Background option</b></html>";
	private static final String DISPLAYOPTIONS_LABEL      = "<html><b>Display option</b></html>";
	private static final String PROCESSOPTIONS_LABEL      = "<html><b>Process options</b></html>";
	
	private static double[] signal1D;
	private static double[] domain1D;
	private static double[] subdomain1D;
	private static double[] subSignal1D;
	private static double[] surrSignal1D;
	Column<? extends Object> signalColumn;
	
	private static String tableInName;
	private static String[] sliceLabels;
	private static long numColumns = 0;
	private static long numRows = 0;
	private static long numDimensions = 0;
	private static int  numSurrogates = 0;
	private static int  numBoxLength = 0;
	private static long numSubsequentBoxes = 0;
	private static long numGlidingBoxes = 0;
	
	private static double numbnn;
	private static double meannn;
	private static double sdnn  ;
	private static double sdann ;		
	private static double sdnni ;	
	private static double hrvti ;	
	private static double rmssd ;		
	private static double sdsd  ;		
	private static double nn50  ;		
	private static double pnn50 ;		
	private static double nn20  ;		
	private static double pnn20 ;	
	private static double ulf   ;//power spectral parameters
	private static double vlf   ;
	private static double lf    ;	
	private static double hf    ;	
	private static double lfn   ;	
	private static double hfn   ;	
	private static double lfhf  ;	
	private static double tp    ;	
	
	private static double[] diffSignal;
	
	// data array	
	private static double[] resultValues;

	private static final String tableOutName = "Table - HRV";
	
	WaitingDialogWithProgressBar dlgProgress;
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

//	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
//	private final String labelSpace = SPACE_LABEL;
	
	@Parameter(type = ItemIO.INPUT)
	private DefaultTableDisplay  defaultTableDisplay;

	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelMeasurementsOptions = MEASUREMENTSOPTIONS_LABEL;
	
	@Parameter(label = "Time base",
			description = "Selection of time base in ms or sec",
			style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
			choices = {"ms", "sec"}, 
			//persist  = false,  //restore previous value default = true
			initializer = "initialTimeBase",
			callback = "callbackTimeBase")
	private String choiceRadioButt_TimeBase;
	
	@Parameter(label = "Windowing for PSD",
			description = "Windowing type",
			style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
			choices = {"Rectangular", "Cosine", "Lanczos", "Bartlett", "Hamming", "Hanning", "Blackman", "Gaussian", "Parzen"}, 
			//According to Malik etal "Bartlett", "Hamming", "Hanning"
			persist  = false,  //restore previous value default = true
			initializer = "initialWindowingType",
			callback = "callbackWindowingType")
	private String choiceRadioButt_WindowingType;
	
	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelAnalysisOptions = ANALYSISOPTIONS_LABEL;

	@Parameter(label = "Signal range",
			description = "Entire signal, Subsequent boxes or Gliding box",
			style = ChoiceWidget.LIST_BOX_STYLE,
			choices = {"Entire signal", "Subsequent boxes", "Gliding box"}, 
			//persist  = false,  //restore previous value default = true
			initializer = "initialSignalRange",
			callback = "callbackSignalRange")
	private String choiceRadioButt_SignalRange;
	
	@Parameter(label = "(Entire signal) Surrogates",
			description = "Surrogates types - Only for Entire signal type!",
			style = ChoiceWidget.LIST_BOX_STYLE,
			choices = {"No surrogates", "Shuffle", "Gaussian", "Random phase", "AAFT"}, 
			persist  = false,  //restore previous value default = true
			initializer = "initialSurrogateType",
			callback = "callbackSurrogateType")
	private String choiceRadioButt_SurrogateType;
	
	@Parameter(label = "Surrogates #", description = "Number of computed surrogates", style = NumberWidget.SPINNER_STYLE, 
			   min = "1", max = "9999999999999999999", stepSize = "1",
			   persist = false, // restore  previous value  default  =  true
			   initializer = "initialNumSurrogates", callback = "callbackNumSurrogates")
	private int spinnerInteger_NumSurrogates;
	
	@Parameter(label = "Box length", description = "Length of subsequent or gliding box", style = NumberWidget.SPINNER_STYLE, 
			   min = "2", max = "9999999999999999999", stepSize = "1",
			   persist = false, // restore  previous value  default  =  true
			   initializer = "initialBoxLength", callback = "callbackBoxLength")
	private int spinnerInteger_BoxLength;
	
	@Parameter(label = "(Surr/Box) Measurement type",
			description = "Measurement for Surrogates, Subsequent boxes or Gliding box",
			style = ChoiceWidget.LIST_BOX_STYLE,
			//"Beats [#]", "MeanHR [1/min]", "MeanNN [ms]", "SDNN [ms]", "SDANN [ms]", "SDNNI [ms]", "HRVTI", "RMSSD [ms]", "SDSD [ms]", "NN50 [#]", "PNN50 [%]", "NN20 [#]", "PNN20 [%]", "ULF [ms^2]", "ULF [ms^2]", "VLF [ms^2]", "LF [ms^2]", "HF [ms^2]", "LFnorm", "HFnorm", "LF/HF", "TP [ms^2]"
			choices = {"Beats [#]", "MeanHR [1/min]", "MeanNN [ms]", "SDNN [ms]", "SDANN [ms]", "SDNNI [ms]", "HRVTI", "RMSSD [ms]", "SDSD [ms]", "NN50 [#]", "PNN50 [%]", "NN20 [#]", "PNN20 [%]", "ULF [ms^2]", "VLF [ms^2]", "LF [ms^2]", "HF [ms^2]", "LFnorm", "HFnorm", "LF/HF", "TP [ms^2]"}, 
			//persist  = false,  //restore previous value default = true
			initializer = "initialMeasurementType",
			callback = "callbackMeasurementType")
	private String choiceRadioButt_MeasurementType;
	
	//-----------------------------------------------------------------------------------------------------
//	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
//	private final String labelBackgroundOptions = BACKGROUNDOPTIONS_LABEL;

	@Parameter(label = "Remove zero values", persist = false,
		       callback = "callbackRemoveZeroes")
	private boolean booleanRemoveZeroes;
	
	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelDisplayOptions = DISPLAYOPTIONS_LABEL;

	@Parameter(label = "Overwrite result display(s)",
	    	description = "Overwrite already existing result images, plots or tables",
	    	//persist  = false,  //restore previous value default = true
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
	
	protected void initialTimeBase() {
		choiceRadioButt_TimeBase = "ms"; //"ms", "sec"
	} 
	
	protected void initialWindowingType() {
		choiceRadioButt_WindowingType = "Hanning";
	} 
	
	protected void initialSignalRange() {
		choiceRadioButt_SignalRange = "Entire signal";
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
	
	protected void initialMeasurementType() {
		choiceRadioButt_MeasurementType = "MeanHR [1/min]"; //"Beats [#]", "MeanHR [1/min]", "MeanNN [ms]", "SDNN [ms]", "SDANN [ms]", "SDNNI [ms]", "HRVTI", "RMSSD [ms]", "SDSD [ms]", "NN50 [#]", "PNN50 [%]", "NN20 [#]", "PNN20 [%]", "ULF [ms^2]", "VLF [ms^2]", "LF [ms^2]", "HF [ms^2]", "LFnorm", "HFnorm", "LF/HF", "TP [ms^2]"
	} 
	
	protected void initialRemoveZeroes() {
		booleanRemoveZeroes = false;
	}	
	
	protected void initialOverwriteDisplays() {
    	booleanOverwriteDisplays = true;
	}
	
	protected void initialNumColumn() {
		spinnerInteger_NumColumn = 1;
	}

	// ------------------------------------------------------------------------------
	
	
	/** Executed whenever the {@link #choiceRadioButt_WindowingType} parameter changes. */
	protected void callbackWindowingType() {
		logService.info(this.getClass().getName() + " Windowing type set to " + choiceRadioButt_WindowingType);
	}
	
	/** Executed whenever the {@link #choiceRadioButt_TimeBase} parameter changes. */
	protected void callbackTimeBase() {
		logService.info(this.getClass().getName() + " Time base set to " + choiceRadioButt_TimeBase);
	}	

	/** Executed whenever the {@link #choiceRadioButt_SignalRange} parameter changes. */
	protected void callbackSignalRange() {
		logService.info(this.getClass().getName() + " Signal range set to " + choiceRadioButt_SignalRange);
		if (!choiceRadioButt_SignalRange.equals("Entire signal")){
			choiceRadioButt_SurrogateType = "No surrogates";
			callbackSurrogateType();
		}
	}
	
	/** Executed whenever the {@link #choiceRadioButt_SurrogateType} parameter changes. */
	protected void callbackSurrogateType() {	
		if (!choiceRadioButt_SignalRange.equals("Entire signal")){
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

	/** Executed whenever the {@link #choiceRadioButt_MeasurementType} parameter changes. */
	protected void callbackMeasurementType() {
		logService.info(this.getClass().getName() + " Measurement type set to " + choiceRadioButt_MeasurementType);
	}
	
	/** Executed whenever the {@link #booleanRemoveZeroes} parameter changes. */
	protected void callbackRemoveZeroes() {
		logService.info(this.getClass().getName() + " Remove zeroes set to " + booleanRemoveZeroes);
	}

	/** Executed whenever the {@link #booleanProcessImmediately} parameter changes. */
	protected void callbackProcessImmediately() {
		logService.info(this.getClass().getName() + " Process immediately set to " + booleanProcessImmediately);
	}
	
	/** Executed whenever the {@link #spinnerInteger_NumColumn} parameter changes. */
	protected void callbackNumColumn() {
		if (spinnerInteger_NumColumn > tableIn.getColumnCount()){ //
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
		
		dlgProgress = new WaitingDialogWithProgressBar("Computing HRV measurements, please wait... Open console window for further info.",
							logService, false, exec); //isCanceable = false, because no following method listens to exec.shutdown 
		dlgProgress.updatePercent("");
		dlgProgress.setBarIndeterminate(true);
		dlgProgress.setVisible(true);
		
    	logService.info(this.getClass().getName() + " Processing single signal");
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
		
		dlgProgress = new WaitingDialogWithProgressBar("Computing HRV measurements, please wait... Open console window for further info.",
							logService, false, exec); //isCanceable = true, because processAllInputSignalss(dlgProgress) listens to exec.shutdown 
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
		tableOut.add(new GenericColumn("Signal range"));
		tableOut.add(new GenericColumn("Surrogate type"));
		tableOut.add(new IntColumn("Surrogates #"));
		tableOut.add(new IntColumn("Box length"));
		tableOut.add(new BoolColumn("Zeroes removed"));
	
		tableOut.add(new GenericColumn("Time base"));	
		tableOut.add(new GenericColumn("Windowing"));
		
		//"Entire signal", "Subsequent boxes", "Gliding box" 
		if (choiceRadioButt_SignalRange.equals("Entire signal")){
			
			if (choiceRadioButt_SurrogateType.equals("No surrogates")) {
				//"Beats [#]", "MeanHR [1/min]", "MeanNN [ms]", "SDNN [ms]", "SDANN [ms]", "SDNNI [ms]", "HRVTI", "RMSSD [ms]", "SDSD [ms]", 
				//"NN50 [#]", "PNN50 [%]", "NN20 [#]", "PNN20 [%]", "ULF [ms^2]", "VLF [ms^2]", "LF [ms^2]", "HF [ms^2]", "LFnorm", "HFnorm", "LF/HF", "TP [ms^2]"
				tableOut.add(new DoubleColumn("Beats [#]"));
				tableOut.add(new DoubleColumn("MeanHR [1/min]"));
				tableOut.add(new DoubleColumn("MeanNN [ms]"));
				tableOut.add(new DoubleColumn("SDNN [ms]"));
				tableOut.add(new DoubleColumn("SDANN [ms]"));
				tableOut.add(new DoubleColumn("SDNNI [ms]"));
				tableOut.add(new DoubleColumn("HRVTI"));
				tableOut.add(new DoubleColumn("RMSSD [ms]"));
				tableOut.add(new DoubleColumn("SDSD [ms]"));
				tableOut.add(new DoubleColumn("NN50 [#]"));
				tableOut.add(new DoubleColumn("PNN50 [%]"));
				tableOut.add(new DoubleColumn("NN20 [#]"));
				tableOut.add(new DoubleColumn("PNN20 [%]"));
				tableOut.add(new DoubleColumn("ULF [ms^2]"));
				tableOut.add(new DoubleColumn("VLF [ms^2]"));
				tableOut.add(new DoubleColumn("LF [ms^2]"));
				tableOut.add(new DoubleColumn("HF [ms^2]"));
				tableOut.add(new DoubleColumn("TP [ms^2]"));
				tableOut.add(new DoubleColumn("LFnorm"));
				tableOut.add(new DoubleColumn("HFnorm"));
				tableOut.add(new DoubleColumn("LF/HF"));
				

			} else { //Surrogates	
				if (choiceRadioButt_MeasurementType.equals("Beats [#]")) {
					tableOut.add(new DoubleColumn("Beats [#]"));
					tableOut.add(new DoubleColumn("Beats_Surr"));  //Mean surrogate value	
					for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("Beats_Surr#"+(s+1))); 
				}
				else if (choiceRadioButt_MeasurementType.equals("MeanHR [1/min]")) {
					tableOut.add(new DoubleColumn("MeanHR [1/min]"));
					tableOut.add(new DoubleColumn("MeanHR_Surr"));  //Mean surrogate value	
					for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("MeanHR_Surr#"+(s+1))); 
				}
				else if (choiceRadioButt_MeasurementType.equals("MeanNN [ms]")) {
					tableOut.add(new DoubleColumn("MeanNN [ms]"));
					tableOut.add(new DoubleColumn("MeanNN_Surr"));  //Mean surrogate value	
					for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("MeanNN_Surr#"+(s+1))); 
				}
				else if (choiceRadioButt_MeasurementType.equals("SDNN [ms]")) {
					tableOut.add(new DoubleColumn("SDNN [ms]"));
					tableOut.add(new DoubleColumn("SDNN_Surr"));  //Mean surrogate value	 
					for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("SDNN_Surr#"+(s+1))); 
				}
				else if (choiceRadioButt_MeasurementType.equals("SDANN [ms]")) {
					tableOut.add(new DoubleColumn("SDANN [ms]")); 
					tableOut.add(new DoubleColumn("SDANN_Surr"));  //Mean surrogate value	
					for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("SDANN_Surr#"+(s+1))); 
				}
				else if (choiceRadioButt_MeasurementType.equals("SDNNI [ms]")) {
					tableOut.add(new DoubleColumn("SDNNI [ms]")); 
					tableOut.add(new DoubleColumn("SDNNI_Surr"));  //Mean surrogate value	
					for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("SDNNI_Surr#"+(s+1))); 
				}
				else if (choiceRadioButt_MeasurementType.equals("HRVTI")) {
					tableOut.add(new DoubleColumn("HRVTI")); 
					tableOut.add(new DoubleColumn("HRVTI_Surr"));  //Mean surrogate value	
					for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("HRVTI_Surr#"+(s+1))); 
				}
				else if (choiceRadioButt_MeasurementType.equals("RMSSD [ms]")) {
					tableOut.add(new DoubleColumn("RMSSD [ms]")); 
					tableOut.add(new DoubleColumn("RMSSD_Surr"));  //Mean surrogate value	
					for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("RMSSD_Surr#"+(s+1))); 
				}
				else if (choiceRadioButt_MeasurementType.equals("SDSD [ms]")) {
					tableOut.add(new DoubleColumn("SDSD [ms]")); 
					tableOut.add(new DoubleColumn("SDSD_Surr"));  //Mean surrogate value	
					for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("SDSD_Surr#"+(s+1))); 
				}
				else if (choiceRadioButt_MeasurementType.equals("NN50 [#]")) {
					tableOut.add(new DoubleColumn("NN50 [#]")); 
					tableOut.add(new DoubleColumn("NN50_Surr"));  //Mean surrogate value	
					for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("NN50_Surr#"+(s+1)));
				}
				else if (choiceRadioButt_MeasurementType.equals("PNN50 [%]")) {
					tableOut.add(new DoubleColumn("PNN50 [%]")); 
					tableOut.add(new DoubleColumn("PNN50_Surr"));  //Mean surrogate value	
					for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("PNN50_Surr#"+(s+1))); 
				}
				else if (choiceRadioButt_MeasurementType.equals("NN20 [#]")) {
					tableOut.add(new DoubleColumn("NN20 [#]")); 
					tableOut.add(new DoubleColumn("NN20_Surr"));  //Mean surrogate value	
					for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("NN20 [#]_Surr#"+(s+1))); 
				}
				else if (choiceRadioButt_MeasurementType.equals("PNN20 [%]")) {
					tableOut.add(new DoubleColumn("PNN20 [%]")); 
					tableOut.add(new DoubleColumn("PNN20_Surr"));  //Mean surrogate value	
					for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("PNN20 [%]_Surr#"+(s+1))); 
				}
				else if (choiceRadioButt_MeasurementType.equals("ULF [ms^2]")) {
					tableOut.add(new DoubleColumn("ULF [ms^2]")); 
					tableOut.add(new DoubleColumn("ULF_Surr"));  //Mean surrogate value	
					for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("ULF_Surr#"+(s+1))); 
				}
				else if (choiceRadioButt_MeasurementType.equals("VLF [ms^2]")) {
					tableOut.add(new DoubleColumn("VLF [ms^2]")); 
					tableOut.add(new DoubleColumn("VLF_Surr"));  //Mean surrogate value	
					for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("VLF_Surr#"+(s+1))); 
				}
				else if (choiceRadioButt_MeasurementType.equals("LF [ms^2]")) {
					tableOut.add(new DoubleColumn("LF [ms^2]")); 
					tableOut.add(new DoubleColumn("LF_Surr"));  //Mean surrogate value	
					for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("LF_Surr#"+(s+1))); 
				}
				else if (choiceRadioButt_MeasurementType.equals("HF [ms^2]")) {
					tableOut.add(new DoubleColumn("HF [ms^2]")); 
					tableOut.add(new DoubleColumn("HF_Surr"));  //Mean surrogate value	
					for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("HF [%]_Surr#"+(s+1))); 
				}
				else if (choiceRadioButt_MeasurementType.equals("TP [ms^2]")) {
					tableOut.add(new DoubleColumn("TP [ms^2]")); 
					tableOut.add(new DoubleColumn("TP_Surr"));  //Mean surrogate value	
					for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("TP_Surr#"+(s+1))); 
				}
				else if (choiceRadioButt_MeasurementType.equals("LFnorm")) {
					tableOut.add(new DoubleColumn("LFnorm")); 
					tableOut.add(new DoubleColumn("LFnorm_Surr"));  //Mean surrogate value	
					for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("LFnorm_Surr#"+(s+1))); 
				}
				else if (choiceRadioButt_MeasurementType.equals("HFnorm")) {
					tableOut.add(new DoubleColumn("HFnorm")); 
					tableOut.add(new DoubleColumn("HFnorm_Surr"));  //Mean surrogate value	
					for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("HFnorm_Surr#"+(s+1))); 
				}
				else if (choiceRadioButt_MeasurementType.equals("LF/HF")) {
					tableOut.add(new DoubleColumn("LF/HF")); 
					tableOut.add(new DoubleColumn("LF/HF_Surr"));  //Mean surrogate value	
					for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("LF/HF_Surr#"+(s+1))); 
				}		
			}
		} 
		else if (choiceRadioButt_SignalRange.equals("Subsequent boxes")){
		
			String entropyHeader = choiceRadioButt_MeasurementType;	
			for (int n = 1; n <= numSubsequentBoxes; n++) {
				tableOut.add(new DoubleColumn(entropyHeader+"-#" + n));	
			}	
		}
		else if (choiceRadioButt_SignalRange.equals("Gliding box")){
		
			String entropyHeader = choiceRadioButt_MeasurementType;		
			for (int n = 1; n <= numGlidingBoxes; n++) {
				tableOut.add(new DoubleColumn(entropyHeader+"-#" + n));	
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
				if (display.getName().equals(tableOutName))
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
		double[] resultValues = process(tableIn, c); 
		// 0 Entropy
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
		
		// loop over all slices of stack starting wit
		for (int s = 0; s < numColumns; s++) { // s... number of signal column 
			//if (!exec.isShutdown()) {
				int percent = (int)Math.round((  ((float)s)/((float)numColumns)   *100.f   ));
				dlgProgress.updatePercent(String.valueOf(percent+"%"));
				dlgProgress.updateBar(percent);
				//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
				statusService.showStatus((s+1), (int)numColumns, "Processing " + (s+1) + "/" + (int)numColumns);
	
				long startTime = System.currentTimeMillis();
				logService.info(this.getClass().getName() + " Processing signal column number " + (s+1) + "(" + numColumns + ")");
				
				// Compute result values
				double[] resultValues = process(tableIn, s);

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
		logService.info(this.getClass().getName() + " Elapsed processing time for all signal(s): "+ sdf.format(duration));
	}
	
	/**
	 * collects current result and writes to table
	 * 
	 * @param int rowNumber to write in the result table
	 * @param in signalNumber column number of signal from tableIn.
	 * @param double[] result values
	 */
	private void writeToTable(int rowNumber, int signalNumber, double[] resultValues) {
		logService.info(this.getClass().getName() + " Writing to the table...");
		int row = rowNumber;
		int tableColStart = 0;
		int tableColEnd   = 0;
		int tableColLast  = 0;
		
		// 0 Entropy
		// fill table with values
		tableOut.appendRow();
		tableOut.set(0, row, tableInName);//File Name
		if (sliceLabels != null)  tableOut.set(1, row, tableIn.getColumnHeader(signalNumber)); //Column Name
	
		tableOut.set(2, row, choiceRadioButt_SignalRange); //Signal Method
		tableOut.set(3, row, choiceRadioButt_SurrogateType); //Surrogate Method
		if (choiceRadioButt_SignalRange.equals("Entire signal") && (!choiceRadioButt_SurrogateType.equals("No surrogates"))) {
			tableOut.set(4, row, spinnerInteger_NumSurrogates); //# Surrogates
		} else {
			tableOut.set(4, row, null); //# Surrogates
		}
		if (!choiceRadioButt_SignalRange.equals("Entire signal")){
			tableOut.set(5, row, spinnerInteger_BoxLength); //Box Length
		} else {
			tableOut.set(5, row, null);
		}	
		tableOut.set(6, row, booleanRemoveZeroes); //Zeroes removed
		
		tableOut.set(7, row, choiceRadioButt_TimeBase);    //
		tableOut.set(8, row, this.choiceRadioButt_WindowingType);
		tableColLast = 8;
		
		//"Entire signal", "Subsequent boxes", "Gliding box" 
		if (choiceRadioButt_SignalRange.equals("Entire signal")){
			int numParameters = resultValues.length;
			tableColStart = tableColLast + 1;
			tableColEnd = tableColStart + numParameters;
			for (int c = tableColStart; c < tableColEnd; c++ ) {
				tableOut.set(c, row, resultValues[c-tableColStart]);
			}	
			if (choiceRadioButt_SurrogateType.equals("No surrogates")) {
				//do nothing	
			} else { //Surrogates
				//already set
			}	
		} 
		else if (choiceRadioButt_SignalRange.equals("Subsequent boxes")){
			tableColStart = tableColLast +1;
			tableColEnd = (int) (tableColStart + 1 * numSubsequentBoxes); //1 or 2  for 1 or 2 parameters
			for (int c = tableColStart; c < tableColEnd; c++ ) {
				tableOut.set(c, row, resultValues[c-tableColStart]);
			}	
		}
		else if (choiceRadioButt_SignalRange.equals("Gliding box")){
			tableColStart = tableColLast +1;
			tableColEnd = (int) (tableColStart + 1 * numGlidingBoxes); //1 or 2 for 1 or 2 parameters 
			for (int c = tableColStart; c < tableColEnd; c++ ) {
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
			logService.info(this.getClass().getName() + " WARNING: dgt==null, no signal for processing!");
		}
		
		String  signalRange   = choiceRadioButt_SignalRange;
		String  surrType      = choiceRadioButt_SurrogateType;
		int     boxLength     = spinnerInteger_BoxLength;
		int     numDataPoints = dgt.getRowCount();
		String  timeBase      = choiceRadioButt_TimeBase;
		String  windowingType = choiceRadioButt_WindowingType;
		
		boolean removeZeores  = booleanRemoveZeroes;
				
		int numOfMeasurements = 21;
		
		resultValues = new double[numOfMeasurements]; // 
		for (int r = 0; r < resultValues.length; r++) resultValues[r] = Float.NaN;
		
		//******************************************************************************************************
		domain1D         = new double[numDataPoints];
		signal1D         = new double[numDataPoints];
		
		signalColumn = dgt.get(col); 
		for (int n = 0; n < numDataPoints; n++) {
			//domain1D[n]  = n+1;
			signal1D[n] = Double.valueOf((Double)signalColumn.get(n));
		}
		//Compute time values from intervals
		domain1D[0] = 0.0;
		for (int n = 1; n < numDataPoints; n++) {
			//domain1D[n]  = n+1;
			domain1D[n]  = domain1D[n-1] + signal1D[n];  //time values from intervals
		}	
		
		signal1D = removeNaN(signal1D);
		if (removeZeores) signal1D = removeZeroes(signal1D);
		
		//May be smaller than before
		numDataPoints = signal1D.length;
		
		logService.info(this.getClass().getName() + " Column #: "+ (col+1) + "  " + signalColumn.getHeader() + "  Size of signal = " + numDataPoints);	
		
		double measurementValue = Float.NaN;
		
		//"Entire signal", "Subsequent boxes", "Gliding box" 
		//********************************************************************************************************
		if (signalRange.equals("Entire signal")){	
	
			if (surrType.equals("No surrogates")) {		
				resultValues = new double[numOfMeasurements]; // 		
				for (int r = 0; r < resultValues.length; r++) resultValues[r] = Float.NaN;			
				//logService.info(this.getClass().getName() + " Column #: "+ (col+1) + "  " + signalColumn.getHeader() + "  Size of signal = " + signal1D.length);	
				
				numbnn = (double)numDataPoints;
				meannn = calcMeanNN(signal1D, timeBase);
				sdnn   = calcSDNN  (signal1D, timeBase);
				sdann  = calcSDANN (signal1D, timeBase);		
				sdnni  = calcSDNNI (signal1D, timeBase);	
				hrvti  = calcHRVTI (signal1D, timeBase);	
				
				diffSignal = getAbsDiffSignal(signal1D);		
				rmssd  = calcRMSSD(diffSignal, timeBase);		
				sdsd   = calcSDSD (diffSignal, timeBase);		
				nn50   = calcNN50 (diffSignal, timeBase);		
				pnn50  = nn50/numbnn;		
				nn20   = calcNN20(diffSignal ,timeBase);		
				pnn20  = nn20/numbnn;	
						
				double[] psdParameters = calcPSDParameters(domain1D, signal1D, timeBase, windowingType);
				
				ulf  = psdParameters[0];
				vlf  = psdParameters[1];
				lf   = psdParameters[2];
				hf   = psdParameters[3];
				tp   = psdParameters[4];
				lfn  = psdParameters[5];
				hfn  = psdParameters[6];
				lfhf = psdParameters[7];
									
				resultValues[0]  = numbnn;
				resultValues[1]  = 1.0/meannn*1000.0*60.0;  //  1/min
				resultValues[2]  = meannn; //ms
				resultValues[3]  = sdnn;
				resultValues[4]  = sdann;	
				resultValues[5]  = sdnni;	
				resultValues[6]  = hrvti;
				resultValues[7]  = rmssd;	
				resultValues[8]  = sdsd;	
				resultValues[9]  = nn50;
				resultValues[10] = pnn50;
				resultValues[11] = nn20;
				resultValues[12] = pnn20;
				resultValues[13] = ulf;	
				resultValues[14] = vlf;	
				resultValues[15] = lf;	
				resultValues[16] = hf;	
				resultValues[17] = tp;	
				resultValues[18] = lfn;	
				resultValues[19] = hfn;	
				resultValues[20] = lfhf;	
		
			} else {
				resultValues = new double[1+1+1*numSurrogates]; // Measurement,  Measurement_SurrMean, Measurement_Surr#1, Measurement_Surr#2......
					
				//"Beats [#]", "MeanHR [1/min]", "MeanNN [ms]", "SDNN [ms]", "SDANN [ms]", "SDNNI [ms]", "HRVTI", "RMSSD [ms]", "SDSD [ms]", 
				//"NN50 [#]", "PNN50 [%]", "NN20 [#]", "PNN20 [%]", "ULF [ms^2]", "VLF [ms^2]", "LF [ms^2]", "HF [ms^2]", "LFnorm", "HFnorm", "LF/HF", "TP [ms^2]"
				if      (choiceRadioButt_MeasurementType.equals("Beats [#]"))      measurementValue = (double)numDataPoints;
				else if (choiceRadioButt_MeasurementType.equals("MeanHR [1/min]")) measurementValue = calcMeanHR(signal1D, timeBase);	
				else if (choiceRadioButt_MeasurementType.equals("MeanNN [ms]"))    measurementValue = calcMeanNN(signal1D, timeBase);
				else if (choiceRadioButt_MeasurementType.equals("SDNN [ms]"))      measurementValue = calcSDNN  (signal1D, timeBase);
				else if (choiceRadioButt_MeasurementType.equals("SDANN [ms]"))     measurementValue = calcSDANN (signal1D, timeBase);	
				else if (choiceRadioButt_MeasurementType.equals("SDNNI [ms]"))     measurementValue = calcSDNNI (signal1D, timeBase);	
				else if (choiceRadioButt_MeasurementType.equals("HRVTI"))          measurementValue = calcHRVTI (signal1D, timeBase);
				else if (choiceRadioButt_MeasurementType.equals("RMSSD [ms]")) {
					diffSignal = getAbsDiffSignal(signal1D);	
					measurementValue = calcRMSSD(diffSignal, timeBase);	
				}
				else if (choiceRadioButt_MeasurementType.equals("SDSD [ms]")) {
					diffSignal = getAbsDiffSignal(signal1D);	
					measurementValue = calcSDSD (diffSignal, timeBase);	
				}
				else if (choiceRadioButt_MeasurementType.equals("NN50 [#]")) {
					diffSignal = getAbsDiffSignal(signal1D);
					measurementValue = calcNN50 (diffSignal, timeBase);
				}
				else if (choiceRadioButt_MeasurementType.equals("PNN50 [%]")) {
					diffSignal = getAbsDiffSignal(signal1D);
					measurementValue = calcNN50 (diffSignal, timeBase)/numDataPoints;	 
				}
				else if (choiceRadioButt_MeasurementType.equals("NN20 [#]")) {
					diffSignal = getAbsDiffSignal(signal1D);
					measurementValue = calcNN20 (diffSignal, timeBase);
				}
				else if (choiceRadioButt_MeasurementType.equals("PNN20 [%]")) {
					diffSignal = getAbsDiffSignal(signal1D);
					measurementValue = calcNN20 (diffSignal, timeBase)/numDataPoints; 
				}
				else if (choiceRadioButt_MeasurementType.equals("ULF [ms^2]")) {
					double[] psdParameters = calcPSDParameters(domain1D, signal1D, timeBase, windowingType);
					measurementValue = psdParameters[0];
				}
				else if (choiceRadioButt_MeasurementType.equals("VLF [ms^2]")) {
					double[] psdParameters = calcPSDParameters(domain1D, signal1D, timeBase, windowingType);
					measurementValue = psdParameters[1];
				}
				else if (choiceRadioButt_MeasurementType.equals("LF [ms^2]")) {
					double[] psdParameters = calcPSDParameters(domain1D, signal1D, timeBase, windowingType);
					measurementValue = psdParameters[2];
				}
				else if (choiceRadioButt_MeasurementType.equals("HF [ms^2]")) {
					double[] psdParameters = calcPSDParameters(domain1D, signal1D, timeBase, windowingType);
					measurementValue = psdParameters[3];
				}
				else if (choiceRadioButt_MeasurementType.equals("TP [ms^2]")) {
					double[] psdParameters = calcPSDParameters(domain1D, signal1D, timeBase, windowingType);
					measurementValue = psdParameters[4];
				}
				else if (choiceRadioButt_MeasurementType.equals("LFnorm")) {
					double[] psdParameters = calcPSDParameters(domain1D, signal1D, timeBase, windowingType);
					measurementValue = psdParameters[5];
				}
				else if (choiceRadioButt_MeasurementType.equals("HFnorm")) {
					double[] psdParameters = calcPSDParameters(domain1D, signal1D, timeBase, windowingType);
					measurementValue = psdParameters[6];
				}
				else if (choiceRadioButt_MeasurementType.equals("LF/HF")) {
					double[] psdParameters = calcPSDParameters(domain1D, signal1D, timeBase, windowingType);
					measurementValue = psdParameters[7];
				}
				
				resultValues[0] = measurementValue;
				int lastMainResultsIndex = 0;
				
				surrSignal1D = new double[signal1D.length];
				
				double sumEntropies   = 0.0f;
				Surrogate surrogate = new Surrogate();
				for (int s = 0; s < numSurrogates; s++) {
					//choices = {"No surrogates", "Shuffle", "Gaussian", "Random phase", "AAFT"}, 
					if      (surrType.equals("Shuffle"))      surrSignal1D = surrogate.calcSurrogateShuffle(signal1D);
					else if (surrType.equals("Gaussian"))     surrSignal1D = surrogate.calcSurrogateGaussian(signal1D);
					else if (surrType.equals("Random phase")) surrSignal1D = surrogate.calcSurrogateRandomPhase(signal1D);
					else if (surrType.equals("AAFT"))         surrSignal1D = surrogate.calcSurrogateAAFT(signal1D);
											
					//"Beats [#]", "MeanHR [1/min]", "MeanNN [ms]", "SDNN [ms]", "SDANN [ms]", "SDNNI [ms]", "HRVTI", "RMSSD [ms]", "SDSD [ms]", 
					//"NN50 [#]", "PNN50 [%]", "NN20 [#]", "PNN20 [%]", "ULF [ms^2]", "VLF [ms^2]", "LF [ms^2]", "HF [ms^2]", "LFnorm", "HFnorm", "LF/HF", "TP [ms^2]"
					if      (choiceRadioButt_MeasurementType.equals("Beats [#]"))      measurementValue = (double)numDataPoints;
					else if (choiceRadioButt_MeasurementType.equals("MeanHR [1/min]")) measurementValue = calcMeanHR(surrSignal1D, timeBase);	
					else if (choiceRadioButt_MeasurementType.equals("MeanNN [ms]"))    measurementValue = calcMeanNN(surrSignal1D, timeBase);
					else if (choiceRadioButt_MeasurementType.equals("SDNN [ms]"))      measurementValue = calcSDNN  (surrSignal1D, timeBase);
					else if (choiceRadioButt_MeasurementType.equals("SDANN [ms]"))     measurementValue = calcSDANN (surrSignal1D, timeBase);	
					else if (choiceRadioButt_MeasurementType.equals("SDNNI [ms]"))     measurementValue = calcSDNNI (surrSignal1D, timeBase);	
					else if (choiceRadioButt_MeasurementType.equals("HRVTI"))          measurementValue = calcHRVTI (surrSignal1D, timeBase);
					else if (choiceRadioButt_MeasurementType.equals("RMSSD [ms]")) {
						diffSignal = getAbsDiffSignal(surrSignal1D);	
						measurementValue = calcRMSSD(diffSignal, timeBase);	
					}
					else if (choiceRadioButt_MeasurementType.equals("SDSD [ms]")) {
						diffSignal = getAbsDiffSignal(surrSignal1D);	
						measurementValue = calcSDSD (diffSignal, timeBase);	
					}
					else if (choiceRadioButt_MeasurementType.equals("NN50 [#]")) {
						diffSignal = getAbsDiffSignal(surrSignal1D);
						measurementValue = calcNN50 (diffSignal, timeBase);
					}
					else if (choiceRadioButt_MeasurementType.equals("PNN50 [%]")) {
						diffSignal = getAbsDiffSignal(surrSignal1D);
						measurementValue = calcNN50 (diffSignal, timeBase)/numDataPoints;	 
					}
					else if (choiceRadioButt_MeasurementType.equals("NN20 [#]")) {
						diffSignal = getAbsDiffSignal(surrSignal1D);
						measurementValue = calcNN20 (diffSignal, timeBase);
					}
					else if (choiceRadioButt_MeasurementType.equals("PNN20 [%]")) {
						diffSignal = getAbsDiffSignal(surrSignal1D);
						measurementValue = calcNN20 (diffSignal, timeBase)/numDataPoints; 
					}
					else if (choiceRadioButt_MeasurementType.equals("ULF [ms^2]")) {
						double[] psdParameters = calcPSDParameters(domain1D, surrSignal1D, timeBase, windowingType);
						measurementValue = psdParameters[0];
					}
					else if (choiceRadioButt_MeasurementType.equals("VLF [ms^2]")) {
						double[] psdParameters = calcPSDParameters(domain1D, surrSignal1D, timeBase, windowingType);
						measurementValue = psdParameters[1];
					}
					else if (choiceRadioButt_MeasurementType.equals("LF [ms^2]")) {
						double[] psdParameters = calcPSDParameters(domain1D, surrSignal1D, timeBase, windowingType);
						measurementValue = psdParameters[2];
					}
					else if (choiceRadioButt_MeasurementType.equals("HF [ms^2]")) {
						double[] psdParameters = calcPSDParameters(domain1D, surrSignal1D, timeBase, windowingType);
						measurementValue = psdParameters[3];
					}
					else if (choiceRadioButt_MeasurementType.equals("TP [ms^2]")) {
						double[] psdParameters = calcPSDParameters(domain1D, surrSignal1D, timeBase, windowingType);
						measurementValue = psdParameters[4];
					}
					else if (choiceRadioButt_MeasurementType.equals("LFnorm")) {
						double[] psdParameters = calcPSDParameters(domain1D, surrSignal1D, timeBase, windowingType);
						measurementValue = psdParameters[5];
					}
					else if (choiceRadioButt_MeasurementType.equals("HFnorm")) {
						double[] psdParameters = calcPSDParameters(domain1D, surrSignal1D, timeBase, windowingType);
						measurementValue = psdParameters[6];
					}
					else if (choiceRadioButt_MeasurementType.equals("LF/HF")) {
						double[] psdParameters = calcPSDParameters(domain1D, surrSignal1D, timeBase, windowingType);
						measurementValue = psdParameters[7];
					}				
					resultValues[lastMainResultsIndex + 2 + s] = measurementValue;
					sumEntropies += measurementValue;
				}
				resultValues[lastMainResultsIndex + 1] = sumEntropies/numSurrogates;
			}
		
		//********************************************************************************************************	
		} else if (signalRange.equals("Subsequent boxes")){
			resultValues = new double[(int) (2*numSubsequentBoxes)]; // Dim R2 == two * number of boxes		
			for (int r = 0; r<resultValues.length; r++) resultValues[r] = Float.NaN;
			subSignal1D = new double[(int) boxLength];
			subdomain1D  = new double[(int) boxLength];
			//number of boxes may be smaller than intended because of NaNs or removed zeroes
			long actualNumSubsequentBoxes = (long) Math.floor((double)signal1D.length/(double)spinnerInteger_BoxLength);
		
			//get sub-signals and compute dimensions
			for (int i = 0; i < actualNumSubsequentBoxes; i++) {	
				logService.info(this.getClass().getName() + " Processing subsequent box #: "+(i+1) + "/" + actualNumSubsequentBoxes);	
				int start = (i*boxLength);
				for (int ii = start; ii < (start + boxLength); ii++){ 
					subSignal1D[ii-start] = signal1D[ii];
					subdomain1D[ii-start] = domain1D[ii];
				}
		
				//Compute specific values************************************************
				//"Beats [#]", "MeanHR [1/min]", "MeanNN [ms]", "SDNN [ms]", "SDANN [ms]", "SDNNI [ms]", "HRVTI", "RMSSD [ms]", "SDSD [ms]", 
				//"NN50 [#]", "PNN50 [%]", "NN20 [#]", "PNN20 [%]", "ULF [ms^2]", "VLF [ms^2]", "LF [ms^2]", "HF [ms^2]", "LFnorm", "HFnorm", "LF/HF", "TP [ms^2]"
				if      (choiceRadioButt_MeasurementType.equals("Beats [#]"))      measurementValue = (double)numDataPoints;
				else if (choiceRadioButt_MeasurementType.equals("MeanHR [1/min]")) measurementValue = calcMeanHR(subSignal1D, timeBase);	
				else if (choiceRadioButt_MeasurementType.equals("MeanNN [ms]"))    measurementValue = calcMeanNN(subSignal1D, timeBase);
				else if (choiceRadioButt_MeasurementType.equals("SDNN [ms]"))      measurementValue = calcSDNN  (subSignal1D, timeBase);
				else if (choiceRadioButt_MeasurementType.equals("SDANN [ms]"))     measurementValue = calcSDANN (subSignal1D, timeBase);	
				else if (choiceRadioButt_MeasurementType.equals("SDNNI [ms]"))     measurementValue = calcSDNNI (subSignal1D, timeBase);	
				else if (choiceRadioButt_MeasurementType.equals("HRVTI"))          measurementValue = calcHRVTI (subSignal1D, timeBase);
				else if (choiceRadioButt_MeasurementType.equals("RMSSD [ms]")) {
					diffSignal = getAbsDiffSignal(subSignal1D);	
					measurementValue = calcRMSSD(diffSignal, timeBase);	
				}
				else if (choiceRadioButt_MeasurementType.equals("SDSD [ms]")) {
					diffSignal = getAbsDiffSignal(subSignal1D);	
					measurementValue = calcSDSD (diffSignal, timeBase);	
				}
				else if (choiceRadioButt_MeasurementType.equals("NN50 [#]")) {
					diffSignal = getAbsDiffSignal(subSignal1D);
					measurementValue = calcNN50 (diffSignal, timeBase);
				}
				else if (choiceRadioButt_MeasurementType.equals("PNN50 [%]")) {
					diffSignal = getAbsDiffSignal(subSignal1D);
					measurementValue = calcNN50 (diffSignal, timeBase)/numDataPoints;	 
				}
				else if (choiceRadioButt_MeasurementType.equals("NN20 [#]")) {
					diffSignal = getAbsDiffSignal(subSignal1D);
					measurementValue = calcNN20 (diffSignal, timeBase);
				}
				else if (choiceRadioButt_MeasurementType.equals("PNN20 [%]")) {
					diffSignal = getAbsDiffSignal(subSignal1D);
					measurementValue = calcNN20 (diffSignal, timeBase)/numDataPoints; 
				}
				else if (choiceRadioButt_MeasurementType.equals("ULF [ms^2]")) {
					double[] psdParameters = calcPSDParameters(subdomain1D, subSignal1D, timeBase, windowingType);
					measurementValue = psdParameters[0];
				}
				else if (choiceRadioButt_MeasurementType.equals("VLF [ms^2]")) {
					double[] psdParameters = calcPSDParameters(subdomain1D, subSignal1D, timeBase, windowingType);
					measurementValue = psdParameters[1];
				}
				else if (choiceRadioButt_MeasurementType.equals("LF [ms^2]")) {
					double[] psdParameters = calcPSDParameters(subdomain1D, subSignal1D, timeBase, windowingType);
					measurementValue = psdParameters[2];
				}
				else if (choiceRadioButt_MeasurementType.equals("HF [ms^2]")) {
					double[] psdParameters = calcPSDParameters(subdomain1D, subSignal1D, timeBase, windowingType);
					measurementValue = psdParameters[3];
				}
				else if (choiceRadioButt_MeasurementType.equals("TP [ms^2]")) {
					double[] psdParameters = calcPSDParameters(subdomain1D, subSignal1D, timeBase, windowingType);
					measurementValue = psdParameters[4];
				}	
				else if (choiceRadioButt_MeasurementType.equals("LFnorm")) {
					double[] psdParameters = calcPSDParameters(subdomain1D, subSignal1D, timeBase, windowingType);
					measurementValue = psdParameters[5];
				}
				else if (choiceRadioButt_MeasurementType.equals("HFnorm")) {
					double[] psdParameters = calcPSDParameters(subdomain1D, subSignal1D, timeBase, windowingType);
					measurementValue = psdParameters[6];
				}
				else if (choiceRadioButt_MeasurementType.equals("LF/HF")) {
					double[] psdParameters = calcPSDParameters(subdomain1D, subSignal1D, timeBase, windowingType);
					measurementValue = psdParameters[7];
				}
				
				resultValues[i] = measurementValue;			
				//***********************************************************************
			}	
		//********************************************************************************************************			
		} else if (signalRange.equals("Gliding box")){
			resultValues = new double[(int) (2*numGlidingBoxes)]; // Dim R2 == two * number of boxes	
			for (int r = 0; r<resultValues.length; r++) resultValues[r] = Float.NaN;
			subSignal1D = new double[(int) boxLength];
			subdomain1D = new double[(int) boxLength];
			//number of boxes may be smaller because of NaNs or removed zeroes
			long actualNumGlidingBoxes = signal1D.length - spinnerInteger_BoxLength + 1;
			
			//get sub-signals and compute dimensions
			for (int i = 0; i < actualNumGlidingBoxes; i++) {
				logService.info(this.getClass().getName() + " Processing gliding box #: "+(i+1) + "/" + actualNumGlidingBoxes);	
				int start = i;
				for (int ii = start; ii < (start + boxLength); ii++){ 
					subSignal1D[ii-start] = signal1D[ii];
					subdomain1D[ii-start]  = domain1D[ii];
				}	
				
				//Compute specific values************************************************
				//"Beats [#]", "MeanHR [1/min]", "MeanNN [ms]", "SDNN [ms]", "SDANN [ms]", "SDNNI [ms]", "HRVTI", "RMSSD [ms]", "SDSD [ms]", 
				//"NN50 [#]", "PNN50 [%]", "NN20 [#]", "PNN20 [%]", "ULF [ms^2]", "VLF [ms^2]", "LF [ms^2]", "HF [ms^2]", "LFnorm", "HFnorm", "LF/HF", "TP [ms^2]"
				if      (choiceRadioButt_MeasurementType.equals("Beats [#]"))      measurementValue = (double)numDataPoints;
				else if (choiceRadioButt_MeasurementType.equals("MeanHR [1/min]")) measurementValue = calcMeanHR(subSignal1D, timeBase);	
				else if (choiceRadioButt_MeasurementType.equals("MeanNN [ms]"))    measurementValue = calcMeanNN(subSignal1D, timeBase);
				else if (choiceRadioButt_MeasurementType.equals("SDNN [ms]"))      measurementValue = calcSDNN  (subSignal1D, timeBase);
				else if (choiceRadioButt_MeasurementType.equals("SDANN [ms]"))     measurementValue = calcSDANN (subSignal1D, timeBase);	
				else if (choiceRadioButt_MeasurementType.equals("SDNNI [ms]"))     measurementValue = calcSDNNI (subSignal1D, timeBase);	
				else if (choiceRadioButt_MeasurementType.equals("HRVTI"))          measurementValue = calcHRVTI (subSignal1D, timeBase);
				else if (choiceRadioButt_MeasurementType.equals("RMSSD [ms]")) {
					diffSignal = getAbsDiffSignal(subSignal1D);	
					measurementValue = calcRMSSD(diffSignal, timeBase);	
				}
				else if (choiceRadioButt_MeasurementType.equals("SDSD [ms]")) {
					diffSignal = getAbsDiffSignal(subSignal1D);	
					measurementValue = calcSDSD (diffSignal, timeBase);	
				}
				else if (choiceRadioButt_MeasurementType.equals("NN50 [#]")) {
					diffSignal = getAbsDiffSignal(subSignal1D);
					measurementValue = calcNN50 (diffSignal, timeBase);
				}
				else if (choiceRadioButt_MeasurementType.equals("PNN50 [%]")) {
					diffSignal = getAbsDiffSignal(subSignal1D);
					measurementValue = calcNN50 (diffSignal, timeBase)/numDataPoints;	 
				}
				else if (choiceRadioButt_MeasurementType.equals("NN20 [#]")) {
					diffSignal = getAbsDiffSignal(subSignal1D);
					measurementValue = calcNN20 (diffSignal, timeBase);
				}
				else if (choiceRadioButt_MeasurementType.equals("PNN20 [%]")) {
					diffSignal = getAbsDiffSignal(subSignal1D);
					measurementValue = calcNN20 (diffSignal, timeBase)/numDataPoints; 
				}
				else if (choiceRadioButt_MeasurementType.equals("ULF [ms^2]")) {
					double[] psdParameters = calcPSDParameters(subdomain1D, subSignal1D, timeBase, windowingType);
					measurementValue = psdParameters[0];
				}
				else if (choiceRadioButt_MeasurementType.equals("VLF [ms^2]")) {
					double[] psdParameters = calcPSDParameters(subdomain1D, subSignal1D, timeBase, windowingType);
					measurementValue = psdParameters[1];
				}
				else if (choiceRadioButt_MeasurementType.equals("LF [ms^2]")) {
					double[] psdParameters = calcPSDParameters(subdomain1D, subSignal1D, timeBase, windowingType);
					measurementValue = psdParameters[2];
				}
				else if (choiceRadioButt_MeasurementType.equals("HF [ms^2]")) {
					double[] psdParameters = calcPSDParameters(subdomain1D, subSignal1D, timeBase, windowingType);
					measurementValue = psdParameters[3];
				}
				else if (choiceRadioButt_MeasurementType.equals("TP [ms^2]")) {
					double[] psdParameters = calcPSDParameters(subdomain1D, subSignal1D, timeBase, windowingType);
					measurementValue = psdParameters[4];
				}
				else if (choiceRadioButt_MeasurementType.equals("LFnorm")) {
					double[] psdParameters = calcPSDParameters(subdomain1D, subSignal1D, timeBase, windowingType);
					measurementValue = psdParameters[5];
				}
				else if (choiceRadioButt_MeasurementType.equals("HFnorm")) {
					double[] psdParameters = calcPSDParameters(subdomain1D, subSignal1D, timeBase, windowingType);
					measurementValue = psdParameters[6];
				}
				else if (choiceRadioButt_MeasurementType.equals("LF/HF")) {
					double[] psdParameters = calcPSDParameters(subdomain1D, subSignal1D, timeBase, windowingType);
					measurementValue = psdParameters[7];
				}
				
				resultValues[i] = measurementValue;		
				//***********************************************************************
			}
		}	
		return resultValues;
		// SampEn or AppEn
		// Output
		// uiService.show(tableOutName, table);
	}
	
	//------------------------------------------------------------------------------------------------------

	/**
	 * This method calculates the mean of a data series
	 * 
	 * @param data1D
	 * @return double Mean
	 */
	private double calcMean(double[] data1D) {
		double sum = 0.0;
		for (double d : data1D) {
			sum += d;
		}
		return sum / data1D.length;
	}
	
	/**
	 * This method calculates the mean of a data series
	 * 
	 * @param data1D
	 * @return double Mean
	 */
	private double calcMean(Vector<Double> data1D) {
		double sum = 0.0;
		for (double d : data1D) {
			sum += d;
		}
		return sum / data1D.size();
	}
	
	
	/**
	 * This method calculates the SD 
	 * 
	 * @param data1D
	 * @return double 
	 */
	private double calcSD(double[] data1D) {
		double sd = 0.0;
		double sum = 0.0;
		double mean = calcMean(data1D);
		for (double d : data1D) {
			sum += Math.pow(d-mean, 2.0);
		}
		sd = Math.sqrt(sum/(data1D.length-1)); //ms
		return sd;
	}
	
	
	/**
	 * This method calculates the SD 
	 * 
	 * @param data1D
	 * @return double
	 */
	private double calcSD(Vector<Double> data1D) {
		double sd = 0.0;
		double sum = 0.0;
		double mean = calcMean(data1D);
		for (double d : data1D) {
			sum += Math.pow(d-mean, 2.0);
		}
		sd = Math.sqrt(sum/(data1D.size()-1)); //ms
		return sd;
	}
	
	/**
	 * This method calculates the mean heart rate
	 * 
	 * @param data1D
	 * @return Double Mean intervals
	 */
	private double calcMeanHR(double[] data1D, String timeBase) {
		double meanNN = calcMeanNN(subSignal1D, timeBase);
		double meanHR = 1.0/meanNN*1000.0*60.0;
		return meanHR;
	}
	
	/**
	 * This method calculates the mean of intervals
	 * 
	 * @param data1D
	 * @return Double Mean intervals
	 */
	private double calcMeanNN(double[] data1D, String timeBase) {
		double meanNN = Double.NaN;
		double sum = 0.0;
		for (double d : data1D) {
			sum += d;
		}
		if (timeBase.equals("ms"))  meanNN = sum / data1D.length; //ms
		if (timeBase.equals("sec")) meanNN = sum / data1D.length * 1000.0; //s   meanNN dann in ms für Ausgabe
		
		return meanNN;
	}
	
	/**
	 * This method calculates the SDNN (simply the SD)
	 * This is simply the Standard Deviation
	 * 
	 * @param data1D, timeBase
	 * @return Double 
	 */
	private double calcSDNN(double[] data1D, String timeBase) {
		double sdnn = Double.NaN;
		double sum = 0.0;
		double mean = calcMean(data1D);
		for (double d : data1D) {
			sum += Math.pow(d-mean, 2.0);
		}
		if (timeBase.equals("ms")) sdnn = Math.sqrt(sum/(data1D.length-1)); //ms
		if (timeBase.equals("sec")) sdnn = Math.sqrt(sum/(data1D.length-1)) *1000.0; //s    sdnn dann in ms für Ausagabe
		return sdnn;
	}
	/**
	 * This method calculates the SDANN  (SD over 5 minute means)
	 * 
	 * @param data1D, timeBase
	 * @return Double 
	 */
	private double calcSDANN(double[] data1D, String timeBase) {
		double sdann = 0.0;	
		double fiveMinutes = 5.0;
		if (timeBase.equals("ms"))  fiveMinutes = fiveMinutes * 60.0 *1000.0;  //ms
		if (timeBase.equals("sec")) fiveMinutes = fiveMinutes * 60.0 *1.0;     //s
		Vector<Double> fiveMinutesMeans  = new Vector<Double>();
		Vector<Double> fiveMinutesSignal = new Vector<Double>();
		double sumOfSubsequentIntervals = 0.0;
		
		for (int i = 0; i < data1D.length; i++) { // scroll through intervals		
			fiveMinutesSignal.add(data1D[i]);
			sumOfSubsequentIntervals += data1D[i];
			
			if (sumOfSubsequentIntervals >= fiveMinutes) {
				fiveMinutesMeans.add(calcMean(fiveMinutesSignal));
				fiveMinutesSignal = new Vector<Double>();
				sumOfSubsequentIntervals = 0.0;
			}			
		}
		if (timeBase.equals("ms"))  sdann = calcSD(fiveMinutesMeans); //ms
		if (timeBase.equals("sec")) sdann = calcSD(fiveMinutesMeans) *1000.0; //s    sdnn dann in ms für Ausagabe
		return sdann;
	}
	/**
	 * This method calculates the SDNNI (Mean over 5 minute SDs)
	 * 
	 * @param data1D, timeBase
	 * @return Double 
	 */
	private double calcSDNNI(double[] data1D , String timeBase) {
		double sdnni = 0.0;
		double fiveMinutes = 5.0;
		if (timeBase.equals("ms"))  fiveMinutes = fiveMinutes * 60.0 *1000.0;  //ms
		if (timeBase.equals("sec")) fiveMinutes = fiveMinutes * 60.0 *1.0;     //s
		Vector<Double> fiveMinutesSDs    = new Vector<Double>();
		Vector<Double> fiveMinutesSignal = new Vector<Double>();
		double sumOfSubsequentIntervals = 0.0;
		
		for (int i = 0; i < data1D.length; i++) { // scroll through intervals		
			fiveMinutesSignal.add(data1D[i]);
			sumOfSubsequentIntervals += data1D[i];
			
			if (sumOfSubsequentIntervals >= fiveMinutes) {				
				//double mean = calcMean(fiveMinutesSignal); 
				fiveMinutesSDs.add(calcSD(fiveMinutesSignal));
				fiveMinutesSignal = new Vector<Double>();
				sumOfSubsequentIntervals = 0.0;
			}			
		}	
		if (timeBase.equals("ms"))  sdnni = calcMean(fiveMinutesSDs); //ms
		if (timeBase.equals("sec")) sdnni = calcMean(fiveMinutesSDs) *1000.0; //s    sdnn dann in ms für Ausagabe
		return sdnni;
	}
	
	/**
	 * This method calculates the HRVTI (HRV triangular index) with a histogram
	 * according to McNames & Aboy Med Bio Eng Comput (2006) 44:747–756)
	 * 
	 * @param data1D, timeBase
	 * @return Double 
	 */
	private double calcHRVTI(double[] data1D, String timeBase) {
		double hrvti    = 0.0;
		double histoMax = 0.0;
		double binSize = 8; //ms  8ms according to McNames & Aboy Med Bio Eng Comput (2006) 44:747–756)
		
		if (timeBase.equals("ms"))  binSize = binSize; //ms
		if (timeBase.equals("sec")) binSize = binSize/1000.0; //s
		
		
		// create a histogram with bin size
		//check min max values
		double maxValue = -Double.MAX_VALUE; // for histogram
		double minValue = Double.MAX_VALUE; //in order to check if there are negative values
		for (double v : data1D) {
			if (v > maxValue) maxValue = v;
			if (v < minValue) minValue = v;
		}	
		if (minValue < 0.0) { //stop execution, negative values are not allowed	
			logService.info(this.getClass().getName() + " Negative values are not allowed!");
			return Double.NaN;
		}
		
		int binNumber = (int) Math.ceil(maxValue/binSize);
		int[] histo = new int[binNumber];
		for (int b = 0; b < binNumber; b++) histo[b] = 0;
				
		for (int i = 0; i < data1D.length; i++) { // scroll through intervals, negative values are not allowed
			//System.out.println("PlotOpHRV: i: " + i + "     data1D[i]: " + data1D.[i));
			int index = (int) Math.ceil(data1D[i]/binSize);
			if (index == 0) index = 1; //can occur if data1D.[i) = 0.0 
			histo[index-1] = histo[index-1] + 1;		
		}
		//search for maximum in the histogram
		for (double h : histo) {
			//System.out.println("PlotOpHRV: Histogram  h: " + h);
			if (h > histoMax) histoMax = h;
		}
		//System.out.println("PlotOpHRV: histoMax: " + histoMax);
		hrvti = data1D.length/histoMax;
		return hrvti;
	}
	
	
	/**
	 * This method computes the differences of subsequent data values
	 *  *
	 * @param data1D
	 * @return double[]
	 */
	private double[] getAbsDiffSignal(double[] data1D){
		double[] diffData1D = new double[data1D.length];
		for (int i = 0; i < data1D.length-1; i++){	
			diffData1D[i] = Math.abs((data1D[i+1] - data1D[i]));
		}
		return diffData1D;
	}
	
	/**
	 * This method calculates the RMSSD (root of mean squared interval differences)
	 * 
	 * @param diffData1D, timeBase
	 * @return Double 
	 */
	private double calcRMSSD(double[] diffData1D, String timeBase) {
		double rmssd= Double.NaN;
		double sum = 0.0;
		for (double d : diffData1D) {
			sum += Math.pow(d, 2.0);
		}
		if (timeBase.equals("ms"))  rmssd = Math.sqrt(sum/(diffData1D.length));
		if (timeBase.equals("sec")) rmssd = Math.sqrt(sum/(diffData1D.length)) *  1000.0; //s    rmssd dann in ms für Ausagabe
		return rmssd;
	}
	/**
	 * This method calculates the SDSD (SD of interval differences)
	 * 
	 * @param diffData1D, timeBase
	 * @return Double 
	 */
	private double calcSDSD(double[] diffData1D, String timeBase ) {
		double sdsd = Double.NaN;
		if (timeBase.equals("ms")) sdsd = calcSD(diffData1D);
		if (timeBase.equals("sec")) sdsd = calcSD(diffData1D) * 1000.0; //s    rmssd dann in ms für Ausagabe
		return sdsd;
	}
	/**
	 * This method calculates the NN50 (number of interval differences of successive NN intervals greater than 50ms)
	 * 
	 * @param diffData1D, timeBase
	 * @return Double 
	 */
	private double calcNN50(double[] diffData1D, String timeBase) {
		double nn50 = 0.0;
		double ms50 = 50.0; //ms
		if (timeBase.equals("ms")) // ms do nothing 
		if (timeBase.equals("sec")) ms50 = ms50/1000.0;  //s
		for (double d : diffData1D) {
			if (d > ms50) nn50 += 1;
		}
		return nn50;
	}
	/**
	 * This method calculates the NN20 (number of interval differences of successive NN intervals greater than 20 ms)
	 * 
	 * @param diffData1D
	 * @return Double 
	 */
	private double calcNN20(double[] diffData1D, String timeBase) {
		double nn20 = 0.0;
		double ms20 = 20.0; //ms
		if (timeBase.equals("ms")) // ms do nothing 
		if (timeBase.equals("sec")) ms20 = ms20/1000.0;  //s
		for (double d : diffData1D) {
			if (d > ms20) nn20 += 1;
		}
		return nn20;
	}
	
	
	
	// ---------------------------------------------------------------------------------------------
	
	/**
	 * This method calculates the frequency components
	 * ULF    ultra low frequency 0.00001-0.003 HZ      Akselrod et al. [6]. 0.00001 - 0.003 Hz Akselrod S.:Components of Heart Rate Variability,In:  Malik M., Camm A.J. (eds.):Heart RateVariability,Armonk, N.Y. Futura Pub. Co. Inc., pp 147-163, 1995
	 * VLF    very low frequency 0.003-0.04 Hz
	 * LF     low frequency 0.04-0.15 Hz
	 * HF     high frequency 0.15-0.4 Hz
	 * TP     total power 0.00001-0.4 Hz
	 * LFn    LF norm = 100*LF/(TP-VLF)
	 * HFn    HF norm = 100*HF/(TP-VLF)
	 * LF/HF  ratio
	 * 
	 * @param XData1D, YData1D, timeBase
	 * @return Double[7] (VLF, LF, HF, TP, LFn, HFn, LF/HF)
	 */
	private double[] calcPSDParameters(double[] xData1D, double[] yData1D,  String timeBase, String windowingType) {
		//xData1D are the absolute times tn of subsequent beats (the first beat is missing)
		//xData1D should start with 0 to compute frequency components correctly, therefore - xData1D[i] - xData1D[0]
		//yData1D are the corresponding beat to beat intervals in ms or seconds,  
		
		double ulf = 0.0;
		double vlf = 0.0;
		double lf  = 0.0;
		double hf  = 0.0;
		double tp  = 0.0;
		double[] psdParameters = new double[8];
	 
		if      (windowingType.equals("Rectangular")) yData1D = windowingRectangular(yData1D);
		else if (windowingType.equals("Cosine"))      yData1D = windowingCosine(yData1D);
		else if (windowingType.equals("Lanczos"))     yData1D = windowingLanczos(yData1D);
		else if (windowingType.equals("Bartlett"))    yData1D = windowingBartlett(yData1D);
		else if (windowingType.equals("Hamming"))     yData1D = windowingHamming(yData1D);
		else if (windowingType.equals("Hanning"))     yData1D = windowingHanning(yData1D);
		else if (windowingType.equals("Blackman"))    yData1D = windowingBlackman(yData1D);
		else if (windowingType.equals("Gaussian"))    yData1D = windowingGaussian(yData1D);
		else if (windowingType.equals("Parzen"))      yData1D = windowingParzen(yData1D);

		//xData1D may be longer than yData1D!!!!!!
		double[] xData = new double[yData1D.length];
		double[] yData = new double[yData1D.length];
		
		
		// set data
		//assuming the first x value = 0;
		
		if (timeBase.equals("ms")) {//ms  convert to seconds because of FFT in Hz
			for (int i = 0; i < yData1D.length; i++) {
				xData[i] = (xData1D[i] - xData1D[0])/1000.0; 	//Subsequent boxes -> xData1D should start with 0 to compute frequency components correctly, therefore - xData1D[i] - xData1D[0]
				yData[i] = yData1D[i]/1000.0;
			}
		}
		if (timeBase.equals("sec")) {//s 
			for (int i = 0; i < yData1D.length; i++) {
				xData[i] = xData1D[i] - xData1D[0];  //Subsequent boxes -> xData1D should start with 0 to compute frequency components correctly, therefore - xData1D[i] - xData1D[0]
				yData[i] = yData1D[i];
			}
		}
		//interpolate interval sequence to get equidistant time spacings - a sample rate
		//LinearInterpolator interpolator = new LinearInterpolator();
		//AkimaSplineInterpolator interpolator = new AkimaSplineInterpolator();
		//BicubicInterpolator interpolator = new BicubicInterpolator();
		SplineInterpolator interpolator = new SplineInterpolator();
		PolynomialSplineFunction psf = interpolator.interpolate(xData, yData);
		
		//Virtual sample rate = 4Hz
		double fS = 4.0; //Virtual sample frequency in Hz
		double virtSampleTime = 0.25; //virtual sample time in sec (first virtual time point)
		double timeOfLastBeat = xData[xData.length -1]; //time of last beat (interval) in seconds, roughly the recording time
		int numbVirtTimePts = (int) Math.floor(timeOfLastBeat/virtSampleTime);
	
		//System.out.println("SignalHRV: number of virtual data points for FFT: "+ numbVirtTimePts);
		if (numbVirtTimePts < 10) {
			logService.info(this.getClass().getName() + " Number of datapoints for FFT: "+numbVirtTimePts+ " is too low!");
			
			psdParameters[0] = Double.NaN;
			psdParameters[1] = Double.NaN;
			psdParameters[2] = Double.NaN;
			psdParameters[3] = Double.NaN;
			psdParameters[4] = Double.NaN;
			psdParameters[5] = Double.NaN;
			psdParameters[6] = Double.NaN;
			psdParameters[7] = Double.NaN;
		
			return psdParameters;
		}
		
		//double[] xInterpolData = new double [numbVirtTimePts];
		double[] yInterpolData = new double[numbVirtTimePts + 1]; //+DC
		double[] xInterpolData = new double[numbVirtTimePts + 1];
		
		for (int t = 0; t <= numbVirtTimePts; t++) { //
			yInterpolData[t] = psf.value(t*virtSampleTime);
			xInterpolData[t] = t*virtSampleTime;
			//System.out.println("t: " + t + "       time: " + (t*virtSampleTime) + "     yInterpolate[t]: " +  yInterpolData[t-1]);
		}
		
		//data length must have a power of 2 
		//therefore, extend with zeros 
		int powerSize = 1;
		while (yInterpolData.length > powerSize){
			powerSize = powerSize*2;
		}
		//powerSize = powerSize /2;
		double[] yInterpolDataExtended = new double[powerSize];
		//set data
		if (powerSize <= yInterpolData.length) {
			for (int i = 0; i < powerSize; i++){
				yInterpolDataExtended[i] = yInterpolData[i];
		 }
		 } else {
			 for (int i = 0; i < yInterpolData.length; i++){
				 yInterpolDataExtended[i] = yInterpolData[i];
			 }
			 for (int i = yInterpolData.length; i < powerSize; i++){
				 yInterpolDataExtended[i] = 0.0d; //extend with zeros
			 }
		 }
		   
	    //FFT from 0 to 4Hz
	    FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.UNITARY); //DftNormalization.STANDARD gives wrong results for ulf, vlf, lf, hf .....!!!!! 
	    Complex[] complex = fft.transform(yInterpolDataExtended, TransformType.FORWARD);

		double[] ps = new double[complex.length/2]; // +1 because DC value at the first place, the rest is symmetric. The second part is mirrored and redundant
		//for (int i = 0; i < complex.length/2; i++) ps[i] = complex[i].getReal()*complex[i].getReal()  + complex[i].getImaginary()*complex[i].getImaginary();
		for (int i = 0; i < complex.length/2; i++) ps[i] = complex[i].abs()*complex[i].abs();
		
//		//to plot power spectrum 
//		double[] signalPS = new double[];
//		double[] rangePS = new double[];
//		String plotLogOrLin = "Lin" 
//		for (int i = 0; i < ps[0].length; i++){
//			if(plotLogOrLin.equals("Log")){
//				signalPS[i] =Math.log(ps[i]);  //log Power Spectrum
//			}
//			if(plotLogOrLin.equals("Lin")){
//				signalPS[i] = ps[i];  //Power Spectrum
//			}
//			rangePS[i] = virtualSampleTime[i]; //if (1/samplerate) is set
//		}
		
		//scroll through spectrum and sum up 
		
		double psRange = ps.length; //only half the spectrum
		double deltaF = fS/(psRange*2.0); 
		double freq; 
		double sumPS = 0; //Area of Power spectrum
		for (int f = 0; f < psRange ; f++) { //f=0 is the DC content
			freq =  f * deltaF; //== f * (delta f)
			//if ( f <= 400) System.out.println("SignalHRV f " + f + "  ps[f] " + ps[f] + "   freq " + freq);
			//if ((f>psRange -100) && ( f < psRange)) System.out.println("SignalHRV f " + f + "  ps[f] " + ps[f] + "   freq " + freq);
			//System.out.println("SignalHRV f " + f + "  ps[f] " + ps[f] + "   freq " + freq);
			if ((freq > 0.00001) && (freq <= 0.003)) { //ULF
				//System.out.println("SignalHRV f " + f + "  ps[f] " + ps[f] + "   freq " + freq);
				ulf = ulf + ps[f];
			}
			else if ((freq > 0.003) && (freq <= 0.04)) { //VLF
				//System.out.println("SignalHRV f " + f + "  ps[f] " + ps[f] + "   freq " + freq);
				vlf = vlf + ps[f];
			}
			else if ((freq > 0.04) && (freq <= 0.15)) { //LF
				//System.out.println("SignalHRV f " + f + "  ps[f] " + ps[f] + "   freq " + freq);
				lf = lf + ps[f];
			}
			else if ((freq > 0.15) && (freq <= 0.4)) { //HF
				//System.out.println("SignalHRV f " + f + "  ps[f] " + ps[f] + "   freq " + freq);
				hf = hf + ps[f];
			}
			if ((freq > 0.00001) && (freq <= 0.4)) { //TP
				tp = tp + ps[f];
			}	
			sumPS = sumPS + ps[f];
		}	
		//compute simple normalized values in %
//		vlf = vlf/sumPS*100;
//		lf = lf/sumPS*100;
//		hf = hf/sumPS*100;
//		tp = tp/sumPS*100;	
//		System.out.println("Sum of percentages:" + (vlf+lf+hf));
		
		//Absolute units are s^2, compute units in ms^2 -> *1000*1000
		//Multiply with bin size (delta f) of spectrum -> fS/(fRange*2)   == 1/(n* delta t)  delta t... sampling period 
		ulf = ulf*1000.0*1000.0*deltaF;
		vlf = vlf*1000.0*1000.0*deltaF;
		lf  =  lf*1000.0*1000.0*deltaF;
		hf  =  hf*1000.0*1000.0*deltaF;
		tp  =  tp*1000.0*1000.0*deltaF;	

		psdParameters[0] = ulf;
		psdParameters[1] = vlf;
		psdParameters[2] = lf;
		psdParameters[3] = hf;
		psdParameters[4] = tp;
		psdParameters[5] = 100.0*lf/(tp-vlf);
		psdParameters[6] = 100.0*hf/(tp-vlf);
		psdParameters[7] = lf/hf;
	
		return psdParameters;
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
	
	
	// This method removes zero background from field signal1D
	private double[] removeZeroes(double[] signal) {
		int lengthOld = signal.length;
		int lengthNew = 0;
		
		for (int i = 0; i < lengthOld; i++) {
			if (signal[i] != 0) lengthNew += 1;
		}
		signal1D = new double[lengthNew];
		int ii = -1;
		for (int i = 0; i < lengthOld; i++) {
			if (signal[i] != 0) {
				ii +=  1;
				signal1D[ii] = signal[i];
			}
		}
		return signal1D;
	}
	
	// This method removes NaN  from field signal1D
	private double[] removeNaN(double[] signal) {
		int lengthOld = signal.length;
		int lengthNew = 0;
		
		for (int i = 0; i < lengthOld; i++) {
			if (!Double.isNaN(signal[i])) {
				lengthNew += 1;
			}
		}
		signal1D = new double[lengthNew];
		int ii = -1;
		for (int i = 0; i < lengthOld; i++) {
			if (!Double.isNaN(signal[i])) {
				ii +=  1;
				signal1D[ii] = signal[i];
			}
		}
		return signal1D;
	}
	
	/**
	 * This method does Rectangular windowing
	 * According to www.labbookpages.co.uk/audio/firWindowing.html#windows
	 * https://de.wikipedia.org/wiki/Fensterfunktion
	 * @param signal
	 * @return windowed signal
	 */
	private double[] windowingRectangular (double[] signal) {
		double weight = 1.0;
	     for(int i = 0; i < signal.length; ++i) {
	    	 signal[i] = signal[i] * weight;
	     }
	     return signal; 
	}
	
	/**
	 * This method does Cosine windowing
	 * According to www.labbookpages.co.uk/audio/firWindowing.html#windows
	 * https://de.wikipedia.org/wiki/Fensterfunktion
	 * @param signal
	 * @return windowed signal
	 */
	private double[] windowingCosine (double[] signal) {
		 double M = signal.length - 1;
		 double weight = 0.0;
	     for(int n = 0; n < signal.length; n++) {
	    	 weight = Math.sin(Math.PI*n/M);
	    	 signal[n] = signal[n] * weight;
	    	 //System.out.println("SignalFFT Cosine weight " + weight);
	     }
	     return signal; 
	}

	/**
	 * This method does  Lanczos windowing
	 * According to www.labbookpages.co.uk/audio/firWindowing.html#windows
	 * https://de.wikipedia.org/wiki/Fensterfunktion
	 * @param signal
	 * @return windowed signal
	 */
	private double[] windowingLanczos (double[] signal) {
		 double M = signal.length - 1;
		 double weight = 0.0;
		 double x = 0.0;
	     for(int n = 0; n < signal.length; n++) {
	    	 x = Math.PI*(2.0*n/M-1);
	    	 if (x == 0) weight = 1.0;
	    	 else weight =  Math.sin(x)/x;
	    	 signal[n] = signal[n] * weight;
	    	 //System.out.println("SignalFFT Lanczos weight  n " + n + "  "  + weight);
	     }
	     return signal; 
	}

	/**
	 * This method does Bartlett windowing
	 * According to www.labbookpages.co.uk/audio/firWindowing.html#windows
	 * https://de.wikipedia.org/wiki/Fensterfunktion
	 * @param signal
	 * @return windowed signal
	 */
	private double[] windowingBartlett (double[] signal) {
		 double M = signal.length - 1;
		 double weight = 0.0;
	     for(int n = 0; n < signal.length; n++) {
	    	 weight = 1.0-(2.0*Math.abs((double)n-M/2.0)/M);
	    	 signal[n] = signal[n] * weight;
	    	 //System.out.println("SignalFFT Bartlett weight " + weight);
	     }
	     return signal; 
	}

	/**
	 * This method does Hamming windowing
	 * According to www.labbookpages.co.uk/audio/firWindowing.html#windows
	 * https://de.wikipedia.org/wiki/Fensterfunktion
	 * @param signal
	 * @return windowed signal
	 */
	private double[] windowingHamming (double[] signal) {
		 double M = signal.length - 1;
		 double weight = 0.0;
	     for(int n = 0; n < signal.length; n++) {
	    	 weight = 0.54 - 0.46 * Math.cos(2.0 * Math.PI * n / M);
	    	 signal[n] = signal[n] * weight;
	    	 //System.out.println("SignalFFT Hamming weight " + weight);
	     }
	     return signal; 
	}

	/**
	 * This method does Hanning windowing
	 * According to www.labbookpages.co.uk/audio/firWindowing.html#windows
	 * https://de.wikipedia.org/wiki/Fensterfunktion
	 * @param signal
	 * @return windowed signal
	 */
	private double[] windowingHanning (double[] signal) {
		 double M = signal.length - 1;
		 double weight = 0.0;
	     for(int n = 0; n < signal.length; n++) {
	    	 weight = 0.5 - 0.5 * Math.cos(2.0 * Math.PI * n / M);
	    	 signal[n] = signal[n] * weight;
	    	 //System.out.println("SignalFFT Hanning weight " + weight);
	     }
	     return signal; 
	}
	
	/**
	 * This method does Blackman windowing
	 * According to www.labbookpages.co.uk/audio/firWindowing.html#windows
	 * https://de.wikipedia.org/wiki/Fensterfunktion
	 * @param signal
	 * @return windowed signal
	 */
	private double[] windowingBlackman (double[] signal) {
		 double M = signal.length - 1;
		 double weight = 0.0;
	     for(int n = 0; n < signal.length; n++) {
	    	 weight = 0.42 - 0.5 * Math.cos(2.0 * Math.PI * n / M) + 0.008 * Math.cos(4.0 * Math.PI * n / M);
	    	 signal[n] = signal[n] * weight;
	    	 //System.out.println("SignalFFT Blackman weight " + weight);
	     }
	     return signal; 
	}
	
	/**
	 * This method does Gaussian windowing
	 * According to www.labbookpages.co.uk/audio/firWindowing.html#windows
	 * https://de.wikipedia.org/wiki/Fensterfunktion
	 * @param signal
	 * @return windowed signal
	 */
	private double[] windowingGaussian (double[] signal) {
		 double M = signal.length - 1;
		 double weight = 0.0;
		 double sigma = 0.3;
		 double exponent = 0.0;
	     for(int n = 0; n < signal.length; n++) {
	    	 exponent = ((double)n-M/2)/(sigma*M/2.0);
	    	 exponent *= exponent;
	    	 weight = Math.exp(-0.5*exponent);
	    	 signal[n] = signal[n] * weight;
	    	 //System.out.println("SignalFFT Gaussian weight " + weight);
	     }
	     return signal; 
	}
	
	/**
	 * This method does Parzen windowing
	 * According to www.labbookpages.co.uk/audio/firWindowing.html#windows
	 * https://de.wikipedia.org/wiki/Fensterfunktion
	 * @param signal
	 * @return windowed signal
	 */
	private double[] windowingParzen (double[] signal) {
		double M = signal.length - 1;
		double nn;
		double weight = 0.0;
	    for(int n = 0; n < signal.length; n++) {
	    	nn = Math.abs((double)n-M/2);
	    	if      ((nn >= 0.0) && (nn < M/4))  weight = 1.0 - 6.0*Math.pow(nn/(M/2), 2) * (1- nn/(M/2));
	    	else if ((nn >= M/4) && (nn <= M/2)) weight = 2.0*Math.pow(1-nn/(M/2), 3);
	    	signal[n] = signal[n] * weight;
	      	//System.out.println("SignalFFT Parzen weight n " + n + "  "  + weight);
	     }
	     return signal; 
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
		
		// open and display a signal, waiting for the operation to finish.
		ij.command().run(SignalOpener.class, true).get().getOutput(tableInName);
		//open and run Plugin
		ij.command().run(SignalHRV.class, true);
	}
}
