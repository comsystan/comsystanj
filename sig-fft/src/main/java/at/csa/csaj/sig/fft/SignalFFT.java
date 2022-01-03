/*-
 * #%L
 * Project: ImageJ2 signal plugin for computing Fast Fourier transformation
 * File: SignalFFT.java
 * 
 * $Id$
 * $HeadURL$
 * 
 * This file is part of ComsystanJ software, hereinafter referred to as "this program".
 * %%
 * Copyright (C) 2021 - 2022 Comsystan Software
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

package at.csa.csaj.sig.fft;

import java.awt.Frame;
import java.awt.Point;
import java.awt.Toolkit;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JFrame;
import javax.swing.UIManager;
import net.imagej.ImageJ;
import net.imagej.ops.OpService;
import net.imglib2.type.numeric.RealType;

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
import org.scijava.table.Column;
import org.scijava.table.DefaultGenericTable;
import org.scijava.table.DefaultTableDisplay;
import org.scijava.table.DoubleColumn;
import org.scijava.table.GenericColumn;
import org.scijava.ui.UIService;
import org.scijava.widget.Button;
import org.scijava.widget.ChoiceWidget;
import org.scijava.widget.NumberWidget;
import at.csa.csaj.commons.signal.algorithms.Surrogate;
import at.csa.csaj.commons.dialog.WaitingDialogWithProgressBar;
import at.csa.csaj.commons.plot.SignalPlotFrame;
import at.csa.csaj.sig.open.SignalOpener;


/**
 * A {@link Command} plugin computing <FFT</a>
 * of a signal.
 */
@Plugin(type = ContextCommand.class,
	headless = true,
	label = "FFT",
	initializer = "initialPluginLaunch",
	//iconPath = "/images/comsystan-??.png", //Menu entry icon
	menu = {
	@Menu(label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT, mnemonic = MenuConstants.PLUGINS_MNEMONIC),
	@Menu(label = "ComsystanJ"),
	@Menu(label = "Signal"),
	@Menu(label = "FFT", weight = 8)})
//public class SignalFFT<T extends RealType<T>> extends InteractiveCommand { // non blocking  GUI
public class SignalFFT<T extends RealType<T>> extends ContextCommand implements Previewable { //modal GUI with cancel

	private static final String PLUGIN_LABEL                = "<html><b>Fast Fourier TRansformation</b></html>";
	private static final String SPACE_LABEL                 = "";
	private static final String FFTOPTIONS_LABEL            = "<html><b>FFT options</b></html>";
	private static final String ANALYSISOPTIONS_LABEL       = "<html><b>Analysis options</b></html>";
	private static final String BACKGROUNDOPTIONS_LABEL     = "<html><b>Background option</b></html>";
	private static final String DISPLAYOPTIONS_LABEL        = "<html><b>Display option</b></html>";
	private static final String PROCESSOPTIONS_LABEL        = "<html><b>Process options</b></html>";
	
	private static double[] signal1D;
	private static double[] domain1D;
	private static double[] subSignal1D;
	private static double[] surrSignal1D;
	private static double[] signalOut;
	Column<? extends Object> signalColumn;
	//Column<? extends Object> domainColumn;
	
	private static String tableInName;
	private static String[] sliceLabels;
	private static long numColumns = 0;
	private static long numRows = 0;
//	private static int  numSurrogates = 0;
//	private static int  numBoxLength = 0;
//	private static long numSubsequentBoxes = 0;
//	private static long numGlidingBoxes = 0;
	
	private static final int numTableOutPreCols = 4; //Number of columns before data (signal) columns, see methods generateTableHeader() and writeToTable()
	private static final String tableOutName = "Table - FFT";
	
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

//	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
//	private final String labelSpace = SPACE_LABEL;
	
	@Parameter(type = ItemIO.INPUT)
	private DefaultTableDisplay  defaultTableDisplay;

	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelFFTOptions = FFTOPTIONS_LABEL;
	
	@Parameter(label = "Windowing",
			   description = "Windowing type",
			   style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
			   choices = {"Rectangular", "Cosine", "Lanczos", "Bartlett", "Hamming", "Hanning", "Blackman", "Gaussian", "Parzen"}, 
			   persist = true,  //restore previous value default = true
			   initializer = "initialWindowingType",
			   callback = "callbackWindowingType")
	private String choiceRadioButt_WindowingType;
	
	@Parameter(label = "Output",
			   description = "Output type",
			   style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
			   choices = {"Power", "Magnitude"}, 
			   persist = true,  //restore previous value default = true
			   initializer = "initialOutputType",
			   callback = "callbackOutputType")
	private String choiceRadioButt_OutputType;
	
	@Parameter(label = "Normalization",
			   description = "Normalization type",
			   style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
			   choices = {"Standard", "Unitary"}, 
			   persist = true,  //restore previous value default = true
			   initializer = "initialNormalizationType",
			   callback = "callbackNormalizationType")
	private String choiceRadioButt_NormalizationType;
	
	@Parameter(label = "Scaling",
			   description = "Scaling of output values",
			   style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
			   choices = {"Log", "Ln", "Linear"}, 
			   persist = true,  //restore previous value default = true
			   initializer = "initialScalingType",
			   callback = "callbackScalingType")
	private String choiceRadioButt_ScalingType;
	
	@Parameter(label = "Time domain",
			   description = "Time domain type",
			   style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
			   choices = {"Unitary", "Hz"}, 
			   persist = true,  //restore previous value default = true
			   initializer = "initialTimeDomainType",
			   callback = "callbackTimeDomainType")
	private String choiceRadioButt_TimeDomainType;
	
	@Parameter(label = "Sample rate (Hz)",
			   description = "Sample rate",
			   style = NumberWidget.SPINNER_STYLE, 
			   min = "1",
			   max = "9999999999999999999",
			   stepSize = "1",
			   persist = true, // restore  previous value  default  =  true
			   initializer = "initialSampleRate",
			   callback = "callbackSampleRate")
	private int spinnerInteger_SampleRate;

	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelAnalysisOptions = ANALYSISOPTIONS_LABEL;

	@Parameter(label = "Signal range",
			   description = "Entire signal, Subsequent boxes or Gliding box",
			   style = ChoiceWidget.LIST_BOX_STYLE,
			   choices = {"Entire signal"}, //, "Subsequent boxes", "Gliding box"}, 
			   persist = true,  //restore previous value default = true
			   initializer = "initialSignalRange",
			   callback = "callbackSignalRange")
	private String choiceRadioButt_SignalRange;
	
	@Parameter(label = "(Entire signal) Surrogates",
			   description = "Surrogates types - Only for Entire signal type!",
			   style = ChoiceWidget.LIST_BOX_STYLE,
			   choices = {"No surrogates", "Shuffle", "Gaussian", "Random phase", "AAFT"}, 
			   persist = true,  //restore previous value default = true
			   initializer = "initialSurrogateType",
			   callback = "callbackSurrogateType")
	private String choiceRadioButt_SurrogateType;
	
//	@Parameter(label = "Surrogates #", description = "Number of computed surrogates", style = NumberWidget.SPINNER_STYLE, 
//			   min = "1", max = "9999999999999999999", stepSize = "1",
//			   persist = true, // restore  previous value  default  =  true
//			   initializer = "initialNumSurrogates", callback = "callbackNumSurrogates")
//	private int spinnerInteger_NumSurrogates;
	
//	@Parameter(label = "Box length", description = "Length of subsequent or gliding box - Shoud be at least three times numMaxLag", style = NumberWidget.SPINNER_STYLE, 
//			   min = "2", max = "9999999999999999999", stepSize = "1",
//			   persist = true, // restore  previous value  default  =  true
//			   initializer = "initialBoxLength", callback = "callbackBoxLength")
//	private int spinnerInteger_BoxLength;
	
	//-----------------------------------------------------------------------------------------------------
//	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
//	private final String labelBackgroundOptions = BACKGROUNDOPTIONS_LABEL;

//	@Parameter(label = "Remove zero values", persist = false,
//		       callback = "callbackRemoveZeroes")
//	private boolean booleanRemoveZeroes;
	
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
	
	protected void initialWindowingType() {
		choiceRadioButt_WindowingType = "Hanning";
	} 
	
	protected void initialOutputType() {
		choiceRadioButt_OutputType = "Power";
	} 
	
	protected void initialNormalizationType() {
		choiceRadioButt_NormalizationType = "Standard";
	} 

	protected void initialScalingType() {
		choiceRadioButt_ScalingType = "Log";
	} 
	
	protected void initialTimeDomainType() {
		choiceRadioButt_TimeDomainType = "Unitary";
	} 
	
	protected void initialSampleRate() {
		spinnerInteger_SampleRate = 1000;
	}
	
	protected void initialSignalRange() {
		choiceRadioButt_SignalRange = "Entire signal";
	} 
	
	protected void initialSurrogateType() {
		choiceRadioButt_SurrogateType = "No surrogates";
	} 
	
//	protected void initialNumSurrogates() {
//		numSurrogates = 10;
//		spinnerInteger_NumSurrogates = numSurrogates;
//	}
//	
//	protected void initialBoxLength() {
//		numBoxLength = 100;
//		spinnerInteger_BoxLength =  (int) numBoxLength;
//		numSubsequentBoxes = (long) Math.floor((double)numRows/(double)spinnerInteger_BoxLength);
//		numGlidingBoxes = numRows - spinnerInteger_BoxLength + 1;
//	}
	
//	protected void initialRemoveZeroes() {
//		booleanRemoveZeroes = false;
//	}	
	
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
	
	/** Executed whenever the {@link #choiceRadioButt_OutputType} parameter changes. */
	protected void callbackOutputType() {
		logService.info(this.getClass().getName() + " Output type set to " + choiceRadioButt_OutputType);
	}
	
	/** Executed whenever the {@link #choiceRadioButt_NormalizationType} parameter changes. */
	protected void callbackNormalizationType() {
		logService.info(this.getClass().getName() + " Normalization type set to " + choiceRadioButt_NormalizationType);
	}
	
	/** Executed whenever the {@link #choiceRadioButt_ScalingType} parameter changes. */
	protected void callbackScalingType() {
		logService.info(this.getClass().getName() + " Scaling type set to " + choiceRadioButt_ScalingType);
	}
	
	/** Executed whenever the {@link #choiceRadioButt_TimeDomainType} parameter changes. */
	protected void callbackTimeDomainType() {
		logService.info(this.getClass().getName() + " Time domain type set to " + choiceRadioButt_TimeDomainType);
	}
	
	/** Executed whenever the {@link #spinnerInteger_SampleRate} parameter changes. */
	protected void callbackSampleRate() {
		logService.info(this.getClass().getName() + " Sample rate set to " + spinnerInteger_SampleRate);
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
	
//	/** Executed whenever the {@link #spinnerInteger_NumSurrogates} parameter changes. */
//	protected void callbackNumSurrogates() {
//		numSurrogates = spinnerInteger_NumSurrogates;
//		logService.info(this.getClass().getName() + " Number of surrogates set to " + spinnerInteger_NumSurrogates);
//	}
//	
//	/** Executed whenever the {@link #spinnerInteger_BoxLength} parameter changes. */
//	protected void callbackBoxLength() {
//		numBoxLength = spinnerInteger_BoxLength;
//		numSubsequentBoxes = (long) Math.floor((double)numRows/(double)spinnerInteger_BoxLength);
//		numGlidingBoxes = numRows - spinnerInteger_BoxLength + 1;
//		logService.info(this.getClass().getName() + " Box length set to " + spinnerInteger_BoxLength);
//	}

//	/** Executed whenever the {@link #booleanRemoveZeroes} parameter changes. */
//	protected void callbackRemoveZeroes() {
//		logService.info(this.getClass().getName() + " Remove zeroes set to " + booleanRemoveZeroes);
//	}

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
			
//		sliceLabels = new String[(int) numColumns];
		
		logService.info(this.getClass().getName() + " Name: "      + tableInName); 
		logService.info(this.getClass().getName() + " Columns #: " + numColumns);
		logService.info(this.getClass().getName() + " Rows #: "    + numRows); 
	}

	/**
	* This method starts the workflow for a single column of the active display
	*/
	protected void startWorkflowForSingleColumn() {
		
		dlgProgress = new WaitingDialogWithProgressBar("Computing FFT, please wait... Open console window for further info.",
							logService, false, exec); //isCanceable = false, because no following method listens to exec.shutdown 
		dlgProgress.updatePercent("");
		dlgProgress.setBarIndeterminate(true);
		dlgProgress.setVisible(true);
	
    	logService.info(this.getClass().getName() + " Processing single signal");
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
	
		dlgProgress = new WaitingDialogWithProgressBar("Computing FFT, please wait... Open console window for further info.",
							logService, false, exec); //isCanceable = true, because processAllInputSignalss(dlgProgress) listens to exec.shutdown 
		dlgProgress.setVisible(true);

    	logService.info(this.getClass().getName() + " Processing all available columns");
  		deleteExistingDisplays();
		generateTableHeader();
		processAllInputColumns();
		dlgProgress.addMessage("Processing finished! Preparing result table...");;
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
		tableOut.add(new GenericColumn("Surrogate type"));
		tableOut.add(new GenericColumn("Normalization"));	
		
		tableOut.add(new GenericColumn("Windowing"));
		String output = "";
		if      (this.choiceRadioButt_OutputType.equals("Power"))     output = "Power";
		else if (this.choiceRadioButt_OutputType.equals("Magnitude")) output = "Magnitude";
		
		String pre = "";
		if      (this.choiceRadioButt_ScalingType.equals("Log"))    pre = "Log("+output+")";
		else if (this.choiceRadioButt_ScalingType.equals("Ln"))     pre = "Ln("+output+")";
		else if (this.choiceRadioButt_ScalingType.equals("Linear")) pre = output;
			
			
		if (this.choiceRadioButt_TimeDomainType.equals("Unitary")) tableOut.add(new DoubleColumn("Time domain (#)"));
		if (this.choiceRadioButt_TimeDomainType.equals("Hz"))      tableOut.add(new DoubleColumn("Time domain (Hz)"));
		for (int c = 0; c < numColumns; c++) {
			tableOut.add(new DoubleColumn(pre+"-" + tableIn.getColumnHeader(c)));
		}	
		//tableOut.appendRows((int) numRows);
		
		//FFT must be done wit hpower of 2
		int powerSize = 1;
		while (numRows > powerSize) {
			powerSize = powerSize * 2;
		}
		tableOut.appendRows((int) powerSize/2); 	//FFT result for magnitude or power is only 1/2, the second half is mirrored and redundant
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
		if (optDeleteExistingImgs) {
			Frame frame;
			Frame[] listFrames = JFrame.getFrames();
			for (int i = listFrames.length -1 ; i >= 0; i--) { //Reverse order, otherwise focus is not given free from the last image
				frame = listFrames[i];
				//System.out.println("frame name: " + frame.getTitle());
				if (frame.getTitle().contains("FFT Signal(s)")) {
					frame.setVisible(false); //Successfully closes also in Fiji
					frame.dispose();
				}
			}
		}
	}

  	/** 
	 * This method takes the single column s and computes results. 
	 * @Param int s
	 * */
	private void processSingleInputColumn (int s) {
		
		long startTime = System.currentTimeMillis();
		
		// Compute result values
		double[] resultValues = process(tableIn, s); 
		// 0 Entropy
		logService.info(this.getClass().getName() + " Processing finished.");
		writeToTable(s, resultValues);
		
		//eliminate empty columns
		leaveOverOnlyOneSignalColumn(s+numTableOutPreCols); // +  because of text columns
		
		//int selectedOption = JOptionPane.showConfirmDialog(null, "Do you want to display the FFT result?\nNot recommended for a large number of signals", "Display option", JOptionPane.YES_NO_OPTION); 
		//if (selectedOption == JOptionPane.YES_OPTION) {
			int[] cols = new int[tableOut.getColumnCount()-numTableOutPreCols]; //- because of first text columns	
			boolean isLineVisible = true;
			String signalTitle = "FFT - " + this.choiceRadioButt_OutputType;
			String xLabel = "#";
			if (choiceRadioButt_TimeDomainType.equals("Unitary")) xLabel = "#";
			else if (choiceRadioButt_TimeDomainType.equals("Hz")) xLabel = "Hz";
			String yLabel = "Value";
			if (choiceRadioButt_ScalingType.equals("Log")) yLabel = "Log("+choiceRadioButt_OutputType+")";
			else if (choiceRadioButt_ScalingType.equals("Ln")) yLabel = "Ln("+choiceRadioButt_OutputType+")";
			else if (choiceRadioButt_ScalingType.equals("Linear")) yLabel = choiceRadioButt_OutputType;
			String[] seriesLabels = new String[tableOut.getColumnCount()-numTableOutPreCols]; //- because of first text columns			
			for (int c = numTableOutPreCols; c < tableOut.getColumnCount(); c++) { //because of first text columns	
				cols[c-numTableOutPreCols] = c; //- because of first text columns	
				seriesLabels[c-numTableOutPreCols] = tableOut.getColumnHeader(c); //- because of first two text columns					
			}
			SignalPlotFrame pdf = new SignalPlotFrame(domain1D, tableOut, cols, isLineVisible, "FFT Signal(s)", signalTitle, xLabel, yLabel, seriesLabels);
			Point pos = pdf.getLocation();
			pos.x = (int) (pos.getX() - 100);
			pos.y = (int) (pos.getY() + 100);
			pdf.setLocation(pos);
			pdf.setVisible(true);
		//}
		
		long duration = System.currentTimeMillis() - startTime;
		TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
		SimpleDateFormat sdf = new SimpleDateFormat();
		sdf.applyPattern("HHH:mm:ss:SSS");
		logService.info(this.getClass().getName() + " Elapsed time: "+ sdf.format(duration));
	}

	
	/**
	 * This method eliminates all columns despite one with number c
	 * @param c
	 */
	private void leaveOverOnlyOneSignalColumn(int c) {
		String header = tableOut.getColumnHeader(c);
		int numCols = tableOut.getColumnCount();
		for (int i = numCols-1; i >= numTableOutPreCols; i--) {    //leave also first text column
			if (!tableOut.getColumnHeader(i).equals(header))  tableOut.removeColumn(i);	
		}	
	}

	/**
	 * This method loops over all input columns and computes results. 
	 * 
	 * */
	private void processAllInputColumns() {
		
		long startTimeAll = System.currentTimeMillis();
		
		// loop over all slices of stack
		for (int s = 0; s < numColumns; s++) { // s... numb er of signal column
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
				// 0 Entropy
				logService.info(this.getClass().getName() + " Processing finished.");
				writeToTable(s, resultValues);
	
				long duration = System.currentTimeMillis() - startTime;
				TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
				SimpleDateFormat sdf = new SimpleDateFormat();
				sdf.applyPattern("HHH:mm:ss:SSS");
				logService.info(this.getClass().getName() + " Elapsed time: "+ sdf.format(duration));
			//}
		} //s	
		statusService.showProgress(0, 100);
		statusService.clearStatus();
		
		//int selectedOption = JOptionPane.showConfirmDialog(null, "Do you want to display the FFT result?\nNot recommended for a large number of signals", "Display option", JOptionPane.YES_NO_OPTION); 
		//if (selectedOption == JOptionPane.YES_OPTION) {
			int[] cols = new int[tableOut.getColumnCount()-numTableOutPreCols]; //- because of first text columns	
			boolean isLineVisible = true;
			String signalTitle = "FFT - " + this.choiceRadioButt_OutputType;
			String xLabel = "#";
			if (choiceRadioButt_TimeDomainType.equals("Unitary")) xLabel = "#";
			else if (choiceRadioButt_TimeDomainType.equals("Hz")) xLabel = "Hz";
			String yLabel = "Value";
			if (choiceRadioButt_ScalingType.equals("Log")) yLabel = "Log("+choiceRadioButt_OutputType+")";
			else if (choiceRadioButt_ScalingType.equals("Ln")) yLabel = "Ln("+choiceRadioButt_OutputType+")";
			else if (choiceRadioButt_ScalingType.equals("Linear")) yLabel = choiceRadioButt_OutputType;
			String[] seriesLabels = new String[tableOut.getColumnCount()-numTableOutPreCols]; //- because of first text columns		
			for (int c = numTableOutPreCols; c < tableOut.getColumnCount(); c++) { //because of first text columns	
				cols[c-numTableOutPreCols] = c;  //-2 because of first two text columns	
				seriesLabels[c-numTableOutPreCols] = tableOut.getColumnHeader(c);	//-because of first text columns				
			}
			SignalPlotFrame pdf = new SignalPlotFrame(domain1D, tableOut, cols, isLineVisible, "FFT Signal(s)", signalTitle, xLabel, yLabel, seriesLabels);
			Point pos = pdf.getLocation();
			pos.x = (int) (pos.getX() - 100);
			pos.y = (int) (pos.getY() + 100);
			pdf.setLocation(pos);
			pdf.setVisible(true);
		//}
			
		long duration = System.currentTimeMillis() - startTimeAll;
		TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
		SimpleDateFormat sdf = new SimpleDateFormat();
		sdf.applyPattern("HHH:mm:ss:SSS");
		logService.info(this.getClass().getName() + " Elapsed processing time for all signal(s): "+ sdf.format(duration));
	}
	
	/**
	 * collects current result and writes to table
	 * 
	 * @param int column number of active signal.
	 * @param double[] result values
	 */
	private void writeToTable(int signalNumber, double[] resultValues) {
		logService.info(this.getClass().getName() + " Writing to the table...");
		
		for (int r = 0; r < resultValues.length; r++ ) {
			tableOut.set(0, r, this.choiceRadioButt_SurrogateType);
			tableOut.set(1, r, this.choiceRadioButt_NormalizationType);
			tableOut.set(2, r, this.choiceRadioButt_WindowingType);
			tableOut.set(3, r, domain1D[r]); // time domain column	
			tableOut.set(numTableOutPreCols + signalNumber, r, resultValues[r]); //+ because of first text columns	
		}
		
		//Fill up with NaNs (this can be because of NaNs in the input signal or deletion of zeroes)
		if (tableOut.getRowCount() > resultValues.length) {
			for (int r = resultValues.length; r < tableOut.getRowCount(); r++ ) {
				tableOut.set(0, r, this.choiceRadioButt_SurrogateType);
				tableOut.set(1, r, this.choiceRadioButt_NormalizationType);
				tableOut.set(2, r, this.choiceRadioButt_WindowingType);
				tableOut.set(3, r, domain1D[r]); // time domain column	
				tableOut.set(numTableOutPreCols + signalNumber, r, Double.NaN); //+ because of first text columns	
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
		String  surrType       = choiceRadioButt_SurrogateType;
		//int     boxLength     = spinnerInteger_BoxLength;
		int     numDataPoints  = dgt.getRowCount();
		//boolean removeZeores  = booleanRemoveZeroes;
		String  outType        = choiceRadioButt_OutputType;//"Magnitude", "Power"
		String  normType       = choiceRadioButt_NormalizationType;//"Standard", "Unitary"
		String  scalingType    = choiceRadioButt_ScalingType;//"Standard", "Unitary"
		String  timeDomainType = choiceRadioButt_TimeDomainType;//"Unitary", "Hz"
		int     sampleRate     = spinnerInteger_SampleRate;
		String  windowingType  = choiceRadioButt_WindowingType;
		//******************************************************************************************************
		
	
		//domain1D  = new double[numDataPoints];
		signal1D = new double[numDataPoints];
		signalColumn = dgt.get(col);
		for (int n = 0; n < numDataPoints; n++) {
			//domain1D[n]  = n+1;
			signal1D[n] = Double.valueOf((Double)signalColumn.get(n));
		}	
		
		signal1D = removeNaN(signal1D);
		//if (removeZeores) signal1D = removeZeroes(signal1D);
		
		//numDataPoints may be smaller now
		numDataPoints = signal1D.length;
		
		signalOut = null;
		
//		double[] signalOut = new double[numDataPoints];
//		for (double d: signalOut) {
//			d = Double.NaN;
//		}
		
		
		//int numActualRows = 0;
		logService.info(this.getClass().getName() + " Column #: "+ (col+1) + "  " + signalColumn.getHeader() + "  Size of signal = " + numDataPoints);	
			
		//"Entire signal", "Subsequent boxes", "Gliding box" 
		//********************************************************************************************************
		if (signalRange.equals("Entire signal")){	//only this option is possible for FFT
			
			if (!surrType.equals("No surrogates")) {
				Surrogate surrogate = new Surrogate();	
				//choices = {"No surrogates", "Shuffle", "Gaussian", "Random phase", "AAFT"}, 
				if (surrType.equals("Shuffle"))      signal1D = surrogate.calcSurrogateShuffle(signal1D);
				if (surrType.equals("Gaussian"))     signal1D = surrogate.calcSurrogateGaussian(signal1D);
				if (surrType.equals("Random phase")) signal1D = surrogate.calcSurrogateRandomPhase(signal1D);
				if (surrType.equals("AAFT"))         signal1D = surrogate.calcSurrogateAAFT(signal1D);
			}
			
			//logService.info(this.getClass().getName() + " Column #: "+ (col+1) + "  " + signalColumn.getHeader() + "  Size of signal = " + signal1D.length);	
		
			if (windowingType.equals("Rectangular")) {
				signal1D = windowingRectangular(signal1D);
			}
			else if (windowingType.equals("Cosine")) {
				signal1D = windowingCosine(signal1D);
			}
			else if (windowingType.equals("Lanczos")) {
				signal1D = windowingLanczos(signal1D);
			}
			else if (windowingType.equals("Bartlett")) {
				signal1D = windowingBartlett(signal1D);
			}
			else if (windowingType.equals("Hamming")) {
				signal1D = windowingHamming(signal1D);
			}
			else if (windowingType.equals("Hanning")) {
				signal1D = windowingHanning(signal1D);
			}
			else if (windowingType.equals("Blackman")) {
				signal1D = windowingBlackman(signal1D);
			}	
			else if (windowingType.equals("Gaussian")) {
				signal1D = windowingGaussian(signal1D);
			}
			else if (windowingType.equals("Parzen")) {
				signal1D = windowingParzen(signal1D);
			}
			
			//Oppenheim & Schafer, DiscreteTimeSignalProcessing-ed3-2010 p.854		
			// data length must have a power of 2
			int powerSize = 1;
			while (numDataPoints > powerSize) {
				powerSize = powerSize * 2;
			}
			double[] data = new double[powerSize];
			// set data
			if (powerSize <= signal1D.length) { //
				for (int i = 0; i < powerSize; i++) {
					data[i] = signal1D[i];
				}
			} else {
				for (int i = 0; i < signal1D.length; i++) {
					data[i] = signal1D[i];
				}
				for (int i = signal1D.length; i < powerSize; i++) {
					data[i] = 0.0d;
				}
			}	
			
			//signalOut = new double[numDataPoints];
			//rangeOut  = new double[numDataPoints];
			
		    //Assumes n is even.
			DftNormalization normalization = null;
			if (normType.equals("Standard")) normalization = DftNormalization.STANDARD;
			if (normType.equals("Unitary"))  normalization = DftNormalization.UNITARY;
			FastFourierTransformer transformer = new FastFourierTransformer(normalization);
		
			Complex[] complx  = transformer.transform(data, TransformType.FORWARD);
			
			//Magnitude or power spectrum
			signalOut = new double[complx.length/2];
			if (outType.equals("Power")) {
				for (int i = 0; i < complx.length/2; i++) {               
					//signalOut[i] = complx[i].getReal()*complx[i].getReal() + complx[i].getImaginary()*complx[i].getImaginary(); //Power spectrum
					signalOut[i] = complx[i].abs()*complx[i].abs();
				}	
			} else 	if (outType.equals("Magnitude")) {
				for (int i = 0; i < complx.length/2; i++) {               
					//signalOut[i] = Math.sqrt(complx[i].getReal()*complx[i].getReal() + complx[i].getImaginary()*complx[i].getImaginary()); //Magnitude
					signalOut[i] = complx[i].abs();
				}	
			}
			
			//output scaling
			if (scalingType.equals("Log")){
				double value = Double.NaN;
				for (int i = 0; i < signalOut.length; i++) {
					value = signalOut[i];
					if (value == 0.0) value = Double.MIN_VALUE; 
					signalOut[i] = Math.log10(value);
				}
			} else if (scalingType.equals("Ln")){
				double value = Double.NaN;
				for (int i = 0; i < signalOut.length; i++) {
					value = signalOut[i];
					if (value == 0.0) value = Double.MIN_VALUE; 
					signalOut[i] = Math.log(value);
				}
			} else if (scalingType.equals("Linear")){
				//Do nothing
			}
			
			//generate time domain axis
			domain1D = new double[complx.length/2];
			if (timeDomainType.equals("Unitary")) {
				for (int n = 0; n < signalOut.length; n++) {
					domain1D[n]  = n+1;
				}	
			} else if (timeDomainType.equals("Hz")) {
				for (int f = 0; f < signalOut.length; f++) {
					domain1D[f]  = (double)f*sampleRate/(signalOut.length*2 - 2);
				}	
			}
			
		//********************************************************************************************************	
		} else if (signalRange.equals("Subsequent boxes")){ //not for FFT
		
		//********************************************************************************************************			
		} else if (signalRange.equals("Gliding box")){ //not for FFT
		
		}
		
		return signalOut;
		// 
		// Output
		// uiService.show(tableOutName, table);
	}

	
	/**
	 * This method calculates the mean of a data series
	 * 
	 * @param data1D
	 * @return Double Mean
	 */
	public Double calcMean(double[] data1D) {
		double sum = 0;
		for (double d : data1D) {
			sum += d;
		}
		return sum / data1D.length;
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
		ij.command().run(SignalFFT.class, true);
	}
}
