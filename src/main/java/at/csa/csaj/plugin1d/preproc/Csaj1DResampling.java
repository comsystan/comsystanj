/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj1DResampling.java
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

package at.csa.csaj.plugin1d.preproc;

import java.awt.Toolkit;
import java.lang.invoke.MethodHandles;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.UIManager;
import net.imagej.ImageJ;
import net.imagej.ops.OpService;
import net.imglib2.type.numeric.RealType;

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
import org.scijava.table.Column;
import org.scijava.table.DefaultGenericTable;
import org.scijava.table.DefaultTableDisplay;
import org.scijava.table.DoubleColumn;
import org.scijava.table.GenericColumn;
import org.scijava.ui.UIService;
import org.scijava.widget.Button;
import org.scijava.widget.ChoiceWidget;
import org.scijava.widget.NumberWidget;

import at.csa.csaj.commons.CsajAlgorithm_Surrogate1D;
import at.csa.csaj.commons.CsajDialog_WaitingWithProgressBar;
import at.csa.csaj.commons.CsajPlot_SequenceFrame;
import at.csa.csaj.commons.CsajContainer_ProcessMethod;
import at.csa.csaj.plugin1d.misc.Csaj1DOpenerCmd;


/**
 * A {@link InteractiveCommand} plugin for <resampling</a>
 * of a sequence.
 */
@Plugin(type = InteractiveCommand.class,
	headless = true,
	label = "Resampling",
	initializer = "initialPluginLaunch",
	iconPath = "/icons/comsystan-logo-grey46-16x16.png", //Menu entry icon
	menu = {
	@Menu(label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT, mnemonic = MenuConstants.PLUGINS_MNEMONIC),
	@Menu(label = "ComsystanJ"),
	@Menu(label = "1D Sequence(s)"),
	@Menu(label = "Preprocessing", weight = 1),
	@Menu(label = "Resampling ")}) //Space at the end of the label is necessary to avoid duplicate with 2D plugin 
/**
 * Csaj Interactive: InteractiveCommand (nonmodal GUI without OK and cancel button, NOT for Scripting!)
 * Csaj Macros:      ContextCommand     (modal GUI with OK and Cancel buttons, for scripting)
 * Developer note:
 * Develop the InteractiveCommand plugin Csaj***.java
 * The Maven build will execute CreateCommandFiles.java which creates Csaj***Command.java files
 *
 *
 */
public class Csaj1DResampling<T extends RealType<T>> extends InteractiveCommand implements Previewable {

	private static final String PLUGIN_LABEL                = "<html><b>Resampling</b></html>";
	private static final String SPACE_LABEL                 = "";
	private static final String RESAMPLINGOPTIONS_LABEL     = "<html><b>Resampling options</b></html>";
	private static final String ANALYSISOPTIONS_LABEL       = "<html><b>Analysis options</b></html>";
	private static final String BACKGROUNDOPTIONS_LABEL     = "<html><b>Background option</b></html>";
	private static final String DISPLAYOPTIONS_LABEL        = "<html><b>Display option</b></html>";
	private static final String PROCESSOPTIONS_LABEL        = "<html><b>Process options</b></html>";
	
	private static double[] sequence1D;
	private static double[] domain1D;
	private static double[] subSequence1D;
	private static double[] surrSequence1D;
	private static double[] sequenceOut;
	Column<? extends Object> sequenceColumn;
	//Column<? extends Object> domainColumn;
	
	private static String tableInName;
	private static String[] sliceLabels;
	private static long numColumns = 0;
	private static long numRowsIn = 0;
	private static long numRowsOut = 0;
//	private static int  numSurrogates = 0;
//	private static int  numBoxLength = 0;
//	private static long numSubsequentBoxes = 0;
//	private static long numGlidingBoxes = 0;
	
	private static final int numTableOutPreCols = 1; //Number of columns before data (sequence) columns, see methods generateTableHeader() and writeToTable()
	public static final String TABLE_OUT_NAME = "Table - Resampled";
	
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

//	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
//	private final String labelSpace = SPACE_LABEL;
	
	@Parameter(type = ItemIO.INPUT)
	private DefaultTableDisplay  defaultTableDisplay;

	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelResamplingOptions = RESAMPLINGOPTIONS_LABEL;
	
	@Parameter(label = "Resampling type",
			   description = "Resampling type",
			   style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
			   choices = {"Down-sampling", "Up-sampling"},
			   persist = true,  //restore previous value default = true
			   initializer = "initialResamplingType",
			   callback = "callbackResamplingType")
	private String choiceRadioButt_ResamplingType;
	
	@Parameter(label = "Factor #",
			   description = "Resampling factor",
			   style = NumberWidget.SPINNER_STYLE, 
			   min = "1",
			   max = "9999999999999999999",
			   stepSize = "1",
			   persist = true, // restore  previous value  default  =  true
			   initializer = "initialNumFactor",
			   callback = "callbackNumFactor")
	private int spinnerInteger_NumFactor;
	
	@Parameter(label = "Interpolation type",
			   description = "Interpolation type",
			   style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
			   choices = {"None", "Linear"},
			   persist = true,  //restore previous value default = true
			   initializer = "initialInterpolationType",
			   callback = "callbackInterpolationType")
	private String choiceRadioButt_InterpolationType;

	//-----------------------------------------------------------------------------------------------------
//	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
//	private final String labelAnalysisOptions = ANALYSISOPTIONS_LABEL;
//
//	@Parameter(label = "Sequence range",
//			description = "Entire sequence, Subsequent boxes or Gliding box",
//			style = ChoiceWidget.LIST_BOX_STYLE,
//			choices = {"Entire sequence"}, //, "Subsequent boxes", "Gliding box"}, 
//			//persist = false,  //restore previous value default = true
//			initializer = "initialSequenceRange",
//			callback = "callbackSequenceRange")
//	private String choiceRadioButt_SequenceRange;
	
//	@Parameter(label = "(Entire sequence) Surrogates",
//			description = "Surrogates types - Only for Entire sequence type!",
//			style = ChoiceWidget.LIST_BOX_STYLE,
//			choices = {"No surrogates", "Shuffle", "Gaussian", "Random phase", "AAFT"}, 
//			persist = false,  //restore previous value default = true
//			initializer = "initialSurrogateType",
//			callback = "callbackSurrogateType")
//	private String choiceRadioButt_SurrogateType;
	
//	@Parameter(label = "Surrogates #", description = "Number of computed surrogates", style = NumberWidget.SPINNER_STYLE, 
//			   min = "1", max = "9999999999999999999", stepSize = "1",
//			   persist = false, // restore  previous value  default  =  true
//			   initializer = "initialNumSurrogates", callback = "callbackNumSurrogates")
//	private int spinnerInteger_NumSurrogates;
	
//	@Parameter(label = "Box length", description = "Length of subsequent or gliding box - Shoud be at least three times numMaxLag", style = NumberWidget.SPINNER_STYLE, 
//			   min = "2", max = "9999999999999999999", stepSize = "1",
//			   persist = false, // restore  previous value  default  =  true
//			   initializer = "initialBoxLength", callback = "callbackBoxLength")
//	private int spinnerInteger_BoxLength;
	
	//-----------------------------------------------------------------------------------------------------
//	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
//	private final String labelBackgroundOptions = BACKGROUNDOPTIONS_LABEL;

//	@Parameter(label = "Skip zero values", persist = false,
//		       callback = "callbackSkipZeroes")
//	private boolean booleanSkipZeroes;
	
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

	@Parameter(label = "Process all columns", callback = "callbackProcessAllColumns")
	private Button buttonProcessAllColumns;

	// ---------------------------------------------------------------------
		
	protected void initialPluginLaunch() {
		checkItemIOIn();
	}
	
	protected void initialResamplingType() {
		choiceRadioButt_ResamplingType = "Down-sampling";
	} 
	
	protected void initialNumFactor() {
		spinnerInteger_NumFactor = 2;
	}
	
	protected void initialInterpolationType() {
		choiceRadioButt_InterpolationType = "Linear";
	} 
//	protected void initialSequenceRange() {
//		choiceRadioButt_SequenceRange = "Entire sequence";
//	} 
//	
//	protected void initialSurrogateType() {
//		choiceRadioButt_SurrogateType = "No surrogates";
//	} 
	
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
	
//	protected void initialSkipZeroes() {
//		booleanSkipZeroes = false;
//	}	
	
	protected void initialOverwriteDisplays() {
    	booleanOverwriteDisplays = true;
	}
	
	protected void initialNumColumn() {
		spinnerInteger_NumColumn = 1;
	}

	// ------------------------------------------------------------------------------
	
	/** Executed whenever the {@link #choiceRadioButt_ResamplingType} parameter changes. */
	protected void callbackResamplingType() {
		logService.info(this.getClass().getName() + " Resampling type set to " + choiceRadioButt_ResamplingType);
	}
	
	/** Executed whenever the {@link #spinnerInteger_NumFactor} parameter changes. */
	protected void callbackNumFactor() {
		logService.info(this.getClass().getName() + " Resampling factor set to " + spinnerInteger_NumFactor);
	}

	/** Executed whenever the {@link #choiceRadioButt_InterpolationType} parameter changes. */
	protected void callbackInterpolationType() {
		logService.info(this.getClass().getName() + " Interpolation type set to " + choiceRadioButt_InterpolationType);
	}
	
//	/** Executed whenever the {@link #choiceRadioButt_SequenceRange} parameter changes. */
//	protected void callbackSequenceRange() {
//		logService.info(this.getClass().getName() + " Sequence range set to " + choiceRadioButt_SequenceRange);
//		if (!choiceRadioButt_SequenceRange.equals("Entire sequence")){
//			choiceRadioButt_SurrogateType = "No surrogates";
//			callbackSurrogateType();
//		}
//	}
//	
//	/** Executed whenever the {@link #choiceRadioButt_SurrogateType} parameter changes. */
//	protected void callbackSurrogateType() {	
//		if (!choiceRadioButt_SequenceRange.equals("Entire sequence")){
//			choiceRadioButt_SurrogateType = "No surrogates";
//			logService.info(this.getClass().getName() + " Surrogates not allowed for subsequent or gliding boxes!");
//		}	
//		logService.info(this.getClass().getName() + " Surrogate type set to " + choiceRadioButt_SurrogateType);
//	}
//	
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

//	/** Executed whenever the {@link #booleanSkipZeroes} parameter changes. */
//	protected void callbackSkipZeroes() {
//		logService.info(this.getClass().getName() + " Skip zeroes set to " + booleanSkipZeroes);
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
		logService.info(this.getClass().getName() + " Run");
		if (ij != null) { //might be null in Fiji
			if (ij.ui().isHeadless()) {
			}
		}
		if (this.getClass().getName().contains("Command")) { //Processing only if class is a Csaj***Command.class
			startWorkflowForAllColumns();
		}
	}
	
	public void checkItemIOIn() {

		//DefaultTableDisplay dtd = (DefaultTableDisplay) displays.get(0);
		try {
			tableIn = (DefaultGenericTable) defaultTableDisplay.get(0);
		} catch (NullPointerException npe) {
			logService.error(this.getClass().getName() + " ERROR: NullPointerException, input table = null");
			cancel("ComsystanJ 1D plugin cannot be started - missing input table.");;
			return;
		}
	
		// get some info
		tableInName = defaultTableDisplay.getName();
		numColumns  = tableIn.getColumnCount();
		numRowsIn     = tableIn.getRowCount();
				
//		sliceLabels = new String[(int) numColumns];
		
		logService.info(this.getClass().getName() + " Name: "      + tableInName); 
		logService.info(this.getClass().getName() + " Columns #: " + numColumns);
		logService.info(this.getClass().getName() + " Rows #: "    + numRowsIn); 
	}

	/**
	* This method starts the workflow for a single column of the active display
	*/
	protected void startWorkflowForSingleColumn() {
	
		dlgProgress = new CsajDialog_WaitingWithProgressBar("Resampling, please wait... Open console window for further info.",
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
				
		dlgProgress = new CsajDialog_WaitingWithProgressBar("Resampling, please wait... Open console window for further info.",
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
		tableOut.add(new GenericColumn("Resampling type"));
		
		String preString = "";
		//"Down-sampling", "Up-sampling"
		if      (this.choiceRadioButt_ResamplingType.equals("Down-sampling")) {
			preString = "Down/" + this.spinnerInteger_NumFactor;
			numRowsOut= (int)numRowsIn/this.spinnerInteger_NumFactor;
		}
		else if (this.choiceRadioButt_ResamplingType.equals("Up-sampling"))  {
			preString = "Up*"   + this.spinnerInteger_NumFactor;
			numRowsOut= (int)numRowsIn*this.spinnerInteger_NumFactor;
		}
		
		for (int c = 0; c < numColumns; c++) {
			//tableOut.add(new DoubleColumn(preString+"-" + tableIn.getColumnHeader(c)));
			//PreString not used
			tableOut.add(new DoubleColumn(tableIn.getColumnHeader(c)));
		}	
		tableOut.appendRows((int) numRowsOut);
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
		CsajContainer_ProcessMethod containerPM = process(tableIn, s); 
		// 0 Entropy
		logService.info(this.getClass().getName() + " Processing finished.");
		writeToTable(s, containerPM);
		
		//eliminate empty columns
		leaveOverOnlyOneSequenceColumn(s+numTableOutPreCols); // +  because of text columns
		
		//int selectedOption = JOptionPane.showConfirmDialog(null, "Do you want to display the FFT result?\nNot recommended for a large number of sequences", "Display option", JOptionPane.YES_NO_OPTION); 
		//if (selectedOption == JOptionPane.YES_OPTION) {
			if (containerPM != null) { 
				int[] cols = new int[tableOut.getColumnCount()-numTableOutPreCols]; //- because of first text columns	
				boolean isLineVisible = true;
				String sequenceTitle = "Resampling - " + this.choiceRadioButt_ResamplingType;
				String xLabel = "#";
				String yLabel = "Value";
				String[] seriesLabels = new String[tableOut.getColumnCount()-numTableOutPreCols]; //- because of first text columns			
				for (int c = numTableOutPreCols; c < tableOut.getColumnCount(); c++) { //because of first text columns	
					cols[c-numTableOutPreCols] = c; //- because of first text columns	
					seriesLabels[c-numTableOutPreCols] = tableOut.getColumnHeader(c); //- because of first two text columns					
				}
				CsajPlot_SequenceFrame pdf = new CsajPlot_SequenceFrame(tableOut, cols, isLineVisible, "Sequence(s)", sequenceTitle, xLabel, yLabel, seriesLabels);
				pdf.setVisible(true);
			}
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
	private void leaveOverOnlyOneSequenceColumn(int c) {
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
				writeToTable(s, containerPM);
	
				long duration = System.currentTimeMillis() - startTime;
				TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
				SimpleDateFormat sdf = new SimpleDateFormat();
				sdf.applyPattern("HHH:mm:ss:SSS");
				logService.info(this.getClass().getName() + " Elapsed time: "+ sdf.format(duration));
			//}
		} //s	
		statusService.showProgress(0, 100);
		statusService.clearStatus();
		
		//int selectedOption = JOptionPane.showConfirmDialog(null, "Do you want to display the FFT result?\nNot recommended for a large number of sequences", "Display option", JOptionPane.YES_NO_OPTION); 
		//if (selectedOption == JOptionPane.YES_OPTION) {
			int[] cols = new int[tableOut.getColumnCount()-numTableOutPreCols]; //- because of first text columns	
			boolean isLineVisible = true;
			String sequenceTitle = "Resampling - " + this.choiceRadioButt_ResamplingType;
			String xLabel = "#";
			String yLabel = "Value";
			String[] seriesLabels = new String[tableOut.getColumnCount()-numTableOutPreCols]; //- because of first text columns		
			for (int c = numTableOutPreCols; c < tableOut.getColumnCount(); c++) { //because of first text columns	
				cols[c-numTableOutPreCols] = c;  //-2 because of first two text columns	
				seriesLabels[c-numTableOutPreCols] = tableOut.getColumnHeader(c);	//-because of first text columns				
			}
			CsajPlot_SequenceFrame pdf = new CsajPlot_SequenceFrame(tableOut, cols, isLineVisible, "Sequence(s)", sequenceTitle, xLabel, yLabel, seriesLabels);
			pdf.setVisible(true);
		//}
			
		long duration = System.currentTimeMillis() - startTimeAll;
		TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
		SimpleDateFormat sdf = new SimpleDateFormat();
		sdf.applyPattern("HHH:mm:ss:SSS");
		logService.info(this.getClass().getName() + " Elapsed processing time for all sequence(s): "+ sdf.format(duration));
	}
	
	/**
	 * collects current result and writes to table
	 * 
	 * @param int column number of active sequence.
	 * @param CsajContainer_ProcessMethod containerPM
	 */
	private void writeToTable(int sequenceNumber,  CsajContainer_ProcessMethod containerPM) {
		logService.info(this.getClass().getName() + " Writing to the table...");
		
		if (containerPM == null) {
			for (int r = 0; r < tableOut.getRowCount(); r++ ) {
				tableOut.set(numTableOutPreCols + sequenceNumber, r, Double.NaN); //+ because of first text columns	
			}
		}
		else {
			for (int r = 0; r < containerPM.item1_Values.length; r++ ) {
				//"Down-sampling", "Up-sampling"
				if      (this.choiceRadioButt_ResamplingType.equals("Down-sampling")) {
					tableOut.set(0, r, "Down/" + this.spinnerInteger_NumFactor + " Interp-" + this.choiceRadioButt_InterpolationType);
				}
				else if (this.choiceRadioButt_ResamplingType.equals("Up-sampling"))  {
					tableOut.set(0, r, "Up*" + this.spinnerInteger_NumFactor + " Interp-" + this.choiceRadioButt_InterpolationType);			
				}		
				tableOut.set(numTableOutPreCols + sequenceNumber, r, containerPM.item1_Values[r]); //+ because of first text columns	
			}
			
			//Fill up with NaNs (this can be because of NaNs in the input sequence or deletion of zeroes)
			if (tableOut.getRowCount() > containerPM.item1_Values.length) {
				for (int r = containerPM.item1_Values.length; r < tableOut.getRowCount(); r++ ) {
					tableOut.set(numTableOutPreCols + sequenceNumber, r, Double.NaN); //+ because of first text columns	
				}
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
		
		String rsType            = choiceRadioButt_ResamplingType;
		int    rsFactor          = spinnerInteger_NumFactor;
		String interpolType      = choiceRadioButt_InterpolationType;
		//String  sequenceRange  = choiceRadioButt_SequenceRange;
		//String surrType        = choiceRadioButt_SurrogateType;
		//numSurrogates          = spinnerInteger_NumSurrogates;
		//numBoxLength           = spinnerInteger_BoxLength;
		int     numDataPoints    = dgt.getRowCount();
		//boolean skipZeroes     = booleanSkipZeroes;
		//String  surrogateType  = choiceRadioButt_SurrogateType;//
		//int     numSurrogates  = spinnerInteger_NumSurrogates;
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
		//if (skipZeroes) sequence1D = removeZeroes(sequence1D);

		//numDataPoints may be smaller now
		numDataPoints = sequence1D.length;
		
		//int numActualRows = 0;
		logService.info(this.getClass().getName() + " Column #: "+ (col+1) + "  " + sequenceColumn.getHeader() + "  Size of sequence = " + numDataPoints);	
		if (numDataPoints == 0) return null; //e.g. if sequence had only NaNs	
	
		//domain1D = new double[numDataPoints];
		//for (int n = 0; n < numDataPoints; n++) domain1D[n] = n+1;
		
		sequenceOut = null;
		
//		sequenceOut = new double[numDataPoints];
//		for (double d: sequenceOut) {
//			d = Double.NaN;
//		}
				
		//"Entire sequence", "Subsequent boxes", "Gliding box" 
		//********************************************************************************************************
		//if (sequenceRange.equals("Entire sequence")){	//only this option is possible for FFT
			
			
		if (rsType.equals("Down-sampling")) {
			int numDataPointsOut = numDataPoints/rsFactor;
			sequenceOut = new double[numDataPointsOut];
			for (double d: sequenceOut) d = Double.NaN;
				
			if (interpolType.equals("None")) { 
				for (int i=0;  i < sequenceOut.length; i++) {
					sequenceOut[i] = sequence1D[i * rsFactor];
				}
			}
			else if (interpolType.equals("Linear")) {
				double meanSequence = 0.0;
				for (int i=0;  i < sequenceOut.length; i++) {
					meanSequence =0.0;
					for (int d=0; d<rsFactor; d++) {
						meanSequence += (sequence1D[i*rsFactor+d]);
					}
					meanSequence = meanSequence/(double)rsFactor;		
					sequenceOut[i] = meanSequence;
				}		
			}
			else if (interpolType.equals("Bicubic")) { 
				
			}	
			
		} else if (rsType.equals("Up-sampling")) {
			int numDataPointsOut = numDataPoints*rsFactor;
			sequenceOut = new double[numDataPointsOut];
			for (double d: sequenceOut) d = Double.NaN;
			
			if (interpolType.equals("None")) { 
				for (int i = 0;  i < sequence1D.length;  i++) {
					for (int d = 0; d<rsFactor; d++) { //adding identical data values
						sequenceOut[i*rsFactor+d] = sequence1D[i];
					}
				}	
			}		
			if (interpolType.equals("Linear")) { 
				int i;
				double linearDeltaSequence = 0;
				for (i = 0;  i < sequence1D.length - 1;  i++) { //-1 because sequence1D[i+1] is not available
					linearDeltaSequence = (sequence1D[i+1] - sequence1D[i])/rsFactor;
					for (int d = 0; d<rsFactor; d++) { //adding values
						sequenceOut[i*rsFactor+d] = sequence1D[i] + d*linearDeltaSequence;
					}
				}	
				//last values
				i = sequence1D.length -1 ; //last values are computed with last delta
				for (int d = 0; d<rsFactor; d++) { //adding values
					sequenceOut[i*rsFactor+d] = sequence1D[i] + d*linearDeltaSequence;
				}
			}
			if (interpolType.equals("Bicubic")) { 
				
			}
			
		}
			
		
		
		
		//********************************************************************************************************	
		//} else if (sequenceRange.equals("Subsequent boxes")){ //not for Surrogate
		
		//********************************************************************************************************			
		//} else if (sequenceRange.equals("Gliding box")){ //not for Surrogate
		
		//}
		
		return new CsajContainer_ProcessMethod(sequenceOut);
		// 
		// Output
		// uiService.show(TABLE_OUT_NAME, table);
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
