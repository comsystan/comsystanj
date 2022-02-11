/*-
 * #%L
 * Project: ImageJ2 signal plugin for computing fractal dimension with roughness scaling extraction.
 * File: SignalFractalDimensionRSE.java
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
package at.csa.csaj.sig.frac.dim.rse;

import java.awt.Toolkit;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import at.csa.csaj.commons.plot.RegressionPlotFrame;
import at.csa.csaj.commons.signal.algorithms.Surrogate;
import at.csa.csaj.commons.dialog.WaitingDialogWithProgressBar;
import at.csa.csaj.sig.frac.dim.rse.util.RSE;
import at.csa.csaj.sig.open.SignalOpener;

/**
 * A {@link Command} plugin computing <the RSE dimension</a>
 * of a  signal.
 */
@Plugin(type = ContextCommand.class, 
	headless = true,
	label = "RSE dimension",
	initializer = "initialPluginLaunch",
	//iconPath = "/images/comsystan-??.png", //Menu entry icon
	menu = {
	@Menu(label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT, mnemonic = MenuConstants.PLUGINS_MNEMONIC),
	@Menu(label = "ComsystanJ"),
	@Menu(label = "Signal"),
	@Menu(label = "RSE dimension", weight = 210)})
//public class SignalFractalDimensionRSE<T extends RealType<T>> extends InteractiveCommand { // non blocking  GUI
public class SignalFractalDimensionRSE<T extends RealType<T>> extends ContextCommand implements Previewable { //modal GUI with cancel

	private static final String PLUGIN_LABEL            = "<html><b>RSE dimension</b></html>";
	private static final String SPACE_LABEL             = "";
	private static final String REGRESSION_LABEL        = "<html><b>Fractal regression parameters</b></html>";
	private static final String RSEOPTIONS_LABEL        = "<html><b>RSE options</b></html>";
	private static final String ANALYSISOPTIONS_LABEL   = "<html><b>Analysis options</b></html>";
	private static final String BACKGROUNDOPTIONS_LABEL = "<html><b>Background Option</b></html>";
	private static final String DISPLAYOPTIONS_LABEL    = "<html><b>Display options</b></html>";
	private static final String PROCESSOPTIONS_LABEL    = "<html><b>Process options</b></html>";
	
	private static double[] signal1D;
	private static double[] domain1D;
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
	
	private static final int  numLMax = 1000;
	
	private static ArrayList<RegressionPlotFrame> doubleLogPlotList = new ArrayList<RegressionPlotFrame>();
	
	private static final String tableOutName = "Table - RSE dimension";
	
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


	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelRegression = REGRESSION_LABEL;

	@Parameter(label = "Maximal length",
			   description = "Maximal length of sub-sequences",
			   style = NumberWidget.SPINNER_STYLE,
			   min = "3",
			   max = "9999999999999999999",
			   stepSize = "1",
			   persist = false, // restore  previous value  default  =  true
			   initializer = "initialLMax", callback = "callbackLMax")
	private int spinnerInteger_LMax;

	@Parameter(label = "Regression Min",
			   description = "Minimum x value of linear regression",
			   style = NumberWidget.SPINNER_STYLE,
			   min = "2",
			   max = "9999999999999999999",
			   stepSize = "1",
			   persist = false, //restore previous value default = true
			   initializer = "initialRegMin", callback = "callbackRegMin")
	private int spinnerInteger_RegMin = 3;

	@Parameter(label = "Regression Max",
			   description = "Maximum x value of linear regression",
			   style = NumberWidget.SPINNER_STYLE,
			   min = "3",
			   max = "9999999999999999999",
			   stepSize = "1",
			   persist = false, //restore previous value default = true
			   initializer = "initialRegMax", callback = "callbackRegMax")
	private int spinnerInteger_RegMax = 8;
	
	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelRSEOptions = RSEOPTIONS_LABEL;

	@Parameter(label = "M",
			   description = "Number of randomly chosen sub-sequences for each length (M=50 recommended)",
			   style = NumberWidget.SPINNER_STYLE,
			   min = "1",
			   max = "9999999999999999999",
			   stepSize = "1",
			   persist = false, //restore previous value default = true
			   initializer = "initialNumM", callback = "callbackNumM")
	private int spinnerInteger_NumM = 50;
	
	@Parameter(label = "Polynomial Order",
			   description = "Order of polynomial flattening (1.. recommended, 0.. without flattening)",
			   style = NumberWidget.SPINNER_STYLE,
			   min = "0",
			   max = "9999999999999999999",
			   stepSize = "1",
			   persist = false, //restore previous value default = true
			   initializer = "initialFlatteningOrder", callback = "callbackFlatteningOrder")
	private int spinnerInteger_FlatteningOrder = 1;
	
	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelAnalysisOptions = ANALYSISOPTIONS_LABEL;

	@Parameter(label = "Signal range",
			   description = "Entire signal, Subsequent boxes or Gliding box",
			   style = ChoiceWidget.LIST_BOX_STYLE,
			   choices = {"Entire signal", "Subsequent boxes", "Gliding box"}, 
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
			   description = "Length of subsequent or gliding box - Shoud be at least three times LMax",
			   style = NumberWidget.SPINNER_STYLE, 
			   min = "2",
			   max = "9999999999999999999",
			   stepSize = "1",
			   persist = true, // restore  previous value  default  =  true
			   initializer = "initialBoxLength", callback = "callbackBoxLength")
	private int spinnerInteger_BoxLength;
	
	//-----------------------------------------------------------------------------------------------------
//	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
//	private final String labelBackgroundOptions = BACKGROUNDOPTIONS_LABEL;

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
	protected void initialLMax() {
		//numbLMax = (int) Math.floor(tableIn.getRowCount() / 3.0);
		spinnerInteger_LMax = 8;
	}
	protected void initialRegMin() {
		spinnerInteger_RegMin = 3;
	}
	protected void initialRegMax() {
		//numbLMax = (int) Math.floor(tableIn.getRowCount() / 3.0);
		spinnerInteger_RegMax = 8;
	}
	protected void initialNumM() {
		spinnerInteger_NumM = 50;
	}
	protected void initialFlatteningOrder() {
		spinnerInteger_FlatteningOrder = 1;
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
	protected void initialRemoveZeroes() {
		booleanRemoveZeroes = false;
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
	
	
	/** Executed whenever the {@link #spinnerInteger_LMax} parameter changes. */
	protected void callbackLMax() {

		if (spinnerInteger_LMax < 3) {
			spinnerInteger_LMax = 3;
		}
		if (spinnerInteger_LMax > numLMax) {
			spinnerInteger_LMax = numLMax;
		}
		if (spinnerInteger_RegMax > spinnerInteger_LMax) {
			spinnerInteger_RegMax = spinnerInteger_LMax;
		}
		if (spinnerInteger_RegMin >= spinnerInteger_RegMax - 2) {
			spinnerInteger_RegMin = spinnerInteger_RegMax - 2;
		}
		logService.info(this.getClass().getName() + " LMax set to " + spinnerInteger_LMax);
	}

	/** Executed whenever the {@link #spinnerInteger_RegMin} parameter changes. */
	protected void callbackRegMin() {
		if (spinnerInteger_RegMin >= spinnerInteger_RegMax - 2) {
			spinnerInteger_RegMin = spinnerInteger_RegMax - 2;
		}
		if (spinnerInteger_RegMin < 2) {
			spinnerInteger_RegMin = 2;
		}
		logService.info(this.getClass().getName() + " Regression Min set to " + spinnerInteger_RegMin);
	}

	/** Executed whenever the {@link #spinnerInteger_RegMax} parameter changes. */
	protected void callbackRegMax() {
		if (spinnerInteger_RegMax <= spinnerInteger_RegMin + 2) {
			spinnerInteger_RegMax = spinnerInteger_RegMin + 2;
		}
		if (spinnerInteger_RegMax > spinnerInteger_LMax) {
			spinnerInteger_RegMax = spinnerInteger_LMax;
		}

		logService.info(this.getClass().getName() + " Regression Max set to " + spinnerInteger_RegMax);
	}
	
	/** Executed whenever the {@link #spinnerInteger_NumM} parameter changes. */
	protected void callbackNumM() {
		logService.info(this.getClass().getName() + " M set to " + spinnerInteger_NumM);
	}
	
	/** Executed whenever the {@link #spinnerInteger_FlatteningOrder} parameter changes. */
	protected void callbackFlatteningOrder() {
		logService.info(this.getClass().getName() + " Flattening order set to " + spinnerInteger_FlatteningOrder);
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
	
		dlgProgress = new WaitingDialogWithProgressBar("Computing RSE dimensions, please wait... Open console window for further info.",
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
	
		dlgProgress = new WaitingDialogWithProgressBar("Computing RSE dimensions, please wait... Open console window for further info.",
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
		tableOut.add(new IntColumn("# Surrogates"));
		tableOut.add(new IntColumn("Box length"));
		tableOut.add(new BoolColumn("Zeroes removed"));
		
		tableOut.add(new IntColumn("L Max"));
		tableOut.add(new IntColumn("Reg Min"));
		tableOut.add(new IntColumn("Reg Max"));
		tableOut.add(new IntColumn("M"));
		tableOut.add(new IntColumn("Flattening order"));
	
		//"Entire signal", "Subsequent boxes", "Gliding box" 
		if (choiceRadioButt_SignalRange.equals("Entire signal")){
			tableOut.add(new DoubleColumn("Drse"));	
			tableOut.add(new DoubleColumn("R2"));
			tableOut.add(new DoubleColumn("StdErr"));
			
			if (choiceRadioButt_SurrogateType.equals("No surrogates")) {
				//do nothing	
			} else { //Surrogates
				tableOut.add(new DoubleColumn("Drse_Surr")); //Mean surrogate value	
				tableOut.add(new DoubleColumn("R2_Surr")); //Mean surrogate value
				tableOut.add(new DoubleColumn("StdErr_Surr")); //Mean surrogate value
				for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("Drse_Surr-#"+(s+1))); 
				for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("R2_Surr-#"+(s+1))); 
				for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("StdErr_Surr-#"+(s+1))); 
			}	
		} 
		else if (choiceRadioButt_SignalRange.equals("Subsequent boxes")){
			for (int n = 1; n <= numSubsequentBoxes; n++) {
				tableOut.add(new DoubleColumn("Drse-#" + n));	
			}
			for (int n = 1; n <= numSubsequentBoxes; n++) {
				tableOut.add(new DoubleColumn("R2-#" + n));	
			}
		}
		else if (choiceRadioButt_SignalRange.equals("Gliding box")){
			for (int n = 1; n <= numGlidingBoxes; n++) {
				tableOut.add(new DoubleColumn("Drse-#" + n));	
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
				if (display.getName().equals(tableOutName))
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
		// 0 Drse, 1 R2, 2 StdErr
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
				// 0 Drse, 1 R2, 2 StdErr
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
		
		// 0 Drse, 1 R2, 2 StdErr
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
		tableOut.set(6,  row, booleanRemoveZeroes); //Zeroes removed
		
		tableOut.set(7,  row, spinnerInteger_LMax); // LMax
		tableOut.set(8,  row, spinnerInteger_RegMin); //RegMin
		tableOut.set(9,  row, spinnerInteger_RegMax); //RegMax	
		tableOut.set(10, row, spinnerInteger_NumM); //M number of randomly chosen sub-sequences for each length 	
		tableOut.set(11, row, spinnerInteger_FlatteningOrder); //FlatteningOrder 	
		tableColLast = 11;
		
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
			//Drse R2 StdErr
			tableColStart = tableColLast +1;
			tableColEnd = (int) (tableColStart + 2 * numSubsequentBoxes); // 2 parameters
			for (int c = tableColStart; c < tableColEnd; c++ ) {
				tableOut.set(c, row, resultValues[c-tableColStart]);
			}	
		}
		else if (choiceRadioButt_SignalRange.equals("Gliding box")){
			//Drse R2 StdErr
			tableColStart = tableColLast +1;
			tableColEnd = (int) (tableColStart + 2 * numGlidingBoxes); // 2 parameters 
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
		
		String signalRange    = choiceRadioButt_SignalRange;
		String surrType       = choiceRadioButt_SurrogateType;
		int boxLength         = spinnerInteger_BoxLength;
		int numDataPoints     = dgt.getRowCount();
		int numLMax           = spinnerInteger_LMax;
		int regMin            = spinnerInteger_RegMin;
		int regMax            = spinnerInteger_RegMax;
		int numM			  = spinnerInteger_NumM;
		int flatteningOrder   = spinnerInteger_FlatteningOrder;
		boolean removeZeores  = booleanRemoveZeroes;
	
		boolean optShowPlot   = booleanShowDoubleLogPlot;
		
		double[] resultValues = new double[3]; // Dim, R2, StdErr
		for (int r = 0; r<resultValues.length; r++) resultValues[r] = Double.NaN;
		
//		double[]totals = new double[numLMax];
//		double[]eps = new double[numLMax];
//		// definition of eps
//		for (int kk = 0; kk < numLMax; kk++) {
//			eps[kk] = kk + 1;		
//			//logService.info(this.getClass().getName() + " k=" + kk + " eps= " + eps[kk][b]);
//		}
		//******************************************************************************************************
		domain1D  = new double[numDataPoints];
		signal1D = new double[numDataPoints];
		
		signalColumn = dgt.get(col);
		for (int n = 0; n < numDataPoints; n++) {
			domain1D[n]  = n+1;
			signal1D[n] = Double.valueOf((Double)signalColumn.get(n));
		}	
		
		signal1D = removeNaN(signal1D);
		if (removeZeores) signal1D = removeZeroes(signal1D);
		RSE rse;
		double[] Rq;
		double[] regressionValues = null;
		
		
		//"Entire signal", "Subsequent boxes", "Gliding box" 
		//********************************************************************************************************
		if (signalRange.equals("Entire signal")){	
			if (surrType.equals("No surrogates")) {
				resultValues = new double[3]; // Dim, R2, StdErr	
			} else {
				resultValues = new double[3+3+3*numSurrogates]; // Dim_Surr, R2_Surr, StdErr_Surr,	Dim_Surr1, Dim_Surr2,  Dim_Surr3, ......R2_Surr1,.... 2, 3......
			}
			for (int r = 0; r < resultValues.length; r++) resultValues[r] = Double.NaN;
			logService.info(this.getClass().getName() + " Column #: "+ (col+1) + "  " + signalColumn.getHeader() + "  Size of signal = " + signal1D.length);	
			if (signal1D.length > (numLMax * 2)) { // only data series which are large enough
				rse = new RSE();
				Rq = rse.calcRqs(signal1D, numLMax, numM, flatteningOrder);
				regressionValues = rse.calcDimension(Rq, regMin, regMax);
				// 0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
				
				if (optShowPlot) {
					String preName = signalColumn.getHeader();
					showPlot(rse.getLnDataX(), rse.getLnDataY(), preName, col, regMin, regMax);
				}	
				resultValues[0] = 2.0-regressionValues[1]; // Drse = 2-slope
				resultValues[1] = regressionValues[4]; //R2
				resultValues[2] = regressionValues[3]; //StdErr
				int lastMainResultsIndex = 2;
				
				if (!surrType.equals("No surrogates")) { //Add surrogate analysis
					surrSignal1D = new double[signal1D.length];
					
					double sumDims   = 0.0;
					double sumR2s    = 0.0;
					double sumStdErr = 0.0;
					Surrogate surrogate = new Surrogate();
					String windowingType = "Rectangular";
					for (int s = 0; s < numSurrogates; s++) {
						//choices = {"No surrogates", "Shuffle", "Gaussian", "Random phase", "AAFT"}, 		
						if (surrType.equals("Shuffle"))      surrSignal1D = surrogate.calcSurrogateShuffle(signal1D);
						if (surrType.equals("Gaussian"))     surrSignal1D = surrogate.calcSurrogateGaussian(signal1D);
						if (surrType.equals("Random phase")) surrSignal1D = surrogate.calcSurrogateRandomPhase(signal1D, windowingType);
						if (surrType.equals("AAFT"))         surrSignal1D = surrogate.calcSurrogateAAFT(signal1D, windowingType);
				
						rse = new RSE();
						Rq = rse.calcRqs(surrSignal1D, numLMax, numM, flatteningOrder);
						regressionValues = rse.calcDimension(Rq, regMin, regMax);
						// 0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
						resultValues[lastMainResultsIndex + 4 + s]                    = 2.0-regressionValues[1];
						resultValues[lastMainResultsIndex + 4 + numSurrogates + s]    = regressionValues[4];
						resultValues[lastMainResultsIndex + 4 + (2*numSurrogates) +s] = regressionValues[3];
						sumDims    +=  2.0-regressionValues[1];
						sumR2s     +=  regressionValues[4];
						sumStdErr  +=  regressionValues[3];
					}
					resultValues[lastMainResultsIndex + 1] = sumDims/numSurrogates;
					resultValues[lastMainResultsIndex + 2] = sumR2s/numSurrogates;
					resultValues[lastMainResultsIndex + 3] = sumStdErr/numSurrogates;
				}	
			} 
		//********************************************************************************************************	
		} else if (signalRange.equals("Subsequent boxes")){
			resultValues = new double[(int) (2*numSubsequentBoxes)]; // Dim R2 == two * number of boxes		
			for (int r = 0; r<resultValues.length; r++) resultValues[r] = Double.NaN;
			subSignal1D = new double[(int) boxLength];
			//number of boxes may be smaller than intended because of NaNs or removed zeroes
			long actualNumSubsequentBoxes = (long) Math.floor((double)signal1D.length/(double)spinnerInteger_BoxLength);
		
			//get sub-signals and compute dimensions
			for (int i = 0; i < actualNumSubsequentBoxes; i++) {	
				logService.info(this.getClass().getName() + " Processing subsequent box #: "+(i+1) + "/" + actualNumSubsequentBoxes);	
				int start = (i*boxLength);
				for (int ii = start; ii < (start + boxLength); ii++){ 
					subSignal1D[ii-start] = signal1D[ii];
				}
				//Compute specific values************************************************
				if (subSignal1D.length > (numLMax * 2)) { // only data series which are large enough
					rse = new RSE();
					Rq = rse.calcRqs(subSignal1D, numLMax, numM, flatteningOrder);
					regressionValues = rse.calcDimension(Rq, regMin, regMax);
					// 0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
					
					//if (optShowPlot){ //show all plots
					if ((optShowPlot) && (i==0)){ //show only first plot
						String preName = signalColumn.getHeader() + "-Box#" + (i+1);
						showPlot(rse.getLnDataX(), rse.getLnDataY(), preName, col, regMin, regMax);
					}
					resultValues[i]                             = 2.0-regressionValues[1]; // Drse = 2-slope;
					resultValues[(int)(i + numSubsequentBoxes)] = regressionValues[4];  //R2		
				} 
				//***********************************************************************
			}	
		//********************************************************************************************************			
		} else if (signalRange.equals("Gliding box")){
			resultValues = new double[(int) (2*numGlidingBoxes)]; // Dim R2 == two * number of boxes	
			for (int r = 0; r<resultValues.length; r++) resultValues[r] = Double.NaN;
			subSignal1D = new double[(int) boxLength];
			//number of boxes may be smaller because of NaNs or removed zeroes
			long actualNumGlidingBoxes = signal1D.length - spinnerInteger_BoxLength + 1;
			
			//get sub-signals and compute dimensions
			for (int i = 0; i < actualNumGlidingBoxes; i++) {
				logService.info(this.getClass().getName() + " Processing gliding box #: "+(i+1) + "/" + actualNumGlidingBoxes);	
				int start = i;
				for (int ii = start; ii < (start + boxLength); ii++){ 
					subSignal1D[ii-start] = signal1D[ii];
				}	
				//Compute specific values************************************************
				if (subSignal1D.length > (numLMax * 2)) { // only data series which are large enough
					rse = new RSE();
					Rq = rse.calcRqs(subSignal1D, numLMax, numM, flatteningOrder);
					regressionValues = rse.calcDimension(Rq, regMin, regMax);
					// 0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
					
					//if (optShowPlot){ //show all plots
					if ((optShowPlot) && (i==0)){ //show only first plot
						String preName = signalColumn.getHeader() + "-Box #" + (i+1);
						showPlot(rse.getLnDataX(), rse.getLnDataY(), preName, col, regMin, regMax);
					}	
					resultValues[i]                          = 2.0-regressionValues[1]; // Drse = 2-slope;
					resultValues[(int)(i + numGlidingBoxes)] = regressionValues[4];  //R2		
				}
				//***********************************************************************
			}
		}
		
		return resultValues;
		// Dim, R2, StdErr
		// Output
		// uiService.show(tableOutName, table);
	}

	// This method shows the double log plot
	private void showPlot(double[] lnDataX, double[] lnDataY, String preName, int col, int regMin, int regMax) {
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
		RegressionPlotFrame doubleLogPlot = DisplayRegressionPlotXY(lnDataX, lnDataY, isLineVisible,
				"Double Log Plot - RSE Dimension", preName + "-" + tableInName, "ln(L)", "ln(Rq)", "", regMin, regMax);
		doubleLogPlotList.add(doubleLogPlot);
		
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
		ij.command().run(SignalFractalDimensionRSE.class, true);
	}
}
