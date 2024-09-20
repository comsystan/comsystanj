/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj1DAutoCorrelation.java
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

package at.csa.csaj.plugin1d.lin;

import java.awt.Frame;
import java.awt.Point;
import java.awt.Toolkit;
import java.lang.invoke.MethodHandles;
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
import org.scijava.table.GenericColumn;
import org.scijava.ui.UIService;
import org.scijava.widget.Button;
import org.scijava.widget.ChoiceWidget;
import org.scijava.widget.NumberWidget;

import at.csa.csaj.commons.CsajAlgorithm_Surrogate1D;
import at.csa.csaj.commons.CsajDialog_WaitingWithProgressBar;
import at.csa.csaj.commons.CsajPlot_SequenceFrame;
import at.csa.csaj.commons.CsajContainer_ProcessMethod;
import at.csa.csaj.plugin1d.misc.Csaj1DOpenerCommand;


/**
 * A {@link InteractiveCommand} plugin computing <Autocorrelation</a>
 * of a sequence.
 */
@Plugin(type = InteractiveCommand.class,
	headless = true,
	label = "Autocorrelation",
	initializer = "initialPluginLaunch",
	iconPath = "/icons/comsystan-logo-grey46-16x16.png", //Menu entry icon
	menu = {
	@Menu(label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT, mnemonic = MenuConstants.PLUGINS_MNEMONIC),
	@Menu(label = "ComsystanJ"),
	@Menu(label = "1D Sequence(s)"),
	@Menu(label = "Linear analyses", weight = 3),
	@Menu(label = "Autocorrelation ")}) //Space at the end of the label is necessary to avoid duplicate with 2D plugin 
/**
 * Csaj Interactive: InteractiveCommand (nonmodal GUI without OK and cancel button, NOT for Scripting!)
 * Csaj Macros:      ContextCommand     (modal GUI with OK and Cancel buttons, for scripting)
 * Developer note:
 * Develop the InteractiveCommand plugin Csaj***.java
 * The Maven build will execute CreateCommandFiles.java which creates Csaj***Command.java files
 *
 *
 */
public class Csaj1DAutoCorrelation<T extends RealType<T>> extends InteractiveCommand implements Previewable {

	private static final String PLUGIN_LABEL                = "<html><b>Autocorrelation</b></html>";
	private static final String SPACE_LABEL                 = "";
	private static final String AUTOCORRELATIONMETHOD_LABEL = "<html><b>Autocorelation method</b></html>";
	private static final String ANALYSISOPTIONS_LABEL       = "<html><b>Analysis options</b></html>";
	private static final String BACKGROUNDOPTIONS_LABEL     = "<html><b>Background option</b></html>";
	private static final String DISPLAYOPTIONS_LABEL        = "<html><b>Display option</b></html>";
	private static final String PROCESSOPTIONS_LABEL        = "<html><b>Process options</b></html>";
	
	private static double[] sequence1D;
	private static double[] domain1D;
	private static double[] subSequence1D;
	private static double[] surrSequence1D;
	Column<? extends Object> sequenceColumn;
	
	private static String tableInName;
	private static String[] sliceLabels;
	private static long numColumns = 0;
	private static long numRows = 0;
	private static int  numSurrogates = 0;
	private static int  numBoxLength = 0;
	private static long numSubsequentBoxes = 0;
	private static long numGlidingBoxes = 0;
	
	private static int numMaxLag = 1;

	private static final int numTableOutPreCols = 2; //Number of text columns before data (sequence) columns, see methods generateTableHeader() and writeToTable()
	public static final String TABLE_OUT_NAME = "Table - Autocorrelation";
	
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
	private final String labelAutoCorrelationMethod = AUTOCORRELATIONMETHOD_LABEL;
	
	@Parameter(label = "Autocorrelation method",
			description = "Autocorrelation",
			style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
			choices = {"Large N", "Small N", "FFT"}, 
			persist = true,  //restore previous value default = true
			initializer = "initialAutoCorrelationMethod",
			callback = "callbackAutoCorrelationMethod")
	private String choiceRadioButt_AutoCorrelationMethod;

	@Parameter(label = "Limit to Max lag",
			   persist = true,
		       callback = "callbackLimitToMaxLag")
	private boolean booleanLimitToMaxLag;
	
	@Parameter(label = "(Limit) Max lag", description = "maximal (time) lag",
			   style = NumberWidget.SPINNER_STYLE,
			   min = "1",
			   max = "1000",
			   stepSize = "1",
			   persist = true, // restore  previous value  default  =  true
			   initializer = "initialNumMaxLag",
			   callback = "callbackNumMaxLag")
	private int spinnerInteger_NumMaxLag;

	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelAnalysisOptions = ANALYSISOPTIONS_LABEL;

	@Parameter(label = "Sequence range",
		       description = "Entire sequence, Subsequent boxes or Gliding box",
			   style = ChoiceWidget.LIST_BOX_STYLE,
			   choices = {"Entire sequence"}, //, "Subsequent boxes", "Gliding box"}, 
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
	
//	@Parameter(label = "# Surrogates:", description = "Number of computed surrogates", style = NumberWidget.SPINNER_STYLE, 
//			   min = "1", max = "9999999999999999999", stepSize = "1",
//			   persist = false, // restore  previous value  default  =  true
//			   initializer = "initialNumSurrogates", callback = "callbackNumSurrogates")
//	private int spinnerInteger_NumSurrogates;
	
//	@Parameter(label = "Box length:", description = "Length of subsequent or gliding box - Shoud be at least three times numMaxLag", style = NumberWidget.SPINNER_STYLE, 
//			   min = "2", max = "9999999999999999999", stepSize = "1",
//			   persist = false, // restore  previous value  default  =  true
//			   initializer = "initialBoxLength", callback = "callbackBoxLength")
//	private int spinnerInteger_BoxLength;
	
	//-----------------------------------------------------------------------------------------------------
//	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
//	private final String labelBackgroundOptions = BACKGROUNDOPTIONS_LABEL;

	@Parameter(label = "Skip zero values", persist = false,
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
	
	protected void initialAutoCorrelationMethod() {
		choiceRadioButt_AutoCorrelationMethod = "Large N";
	} 
	
	protected void initialNumMaxLag() {
		spinnerInteger_NumMaxLag = 10;	
		numMaxLag = this.spinnerInteger_NumMaxLag;
	
		// number of lags > numDataPoints is not allowed
		if (numMaxLag >= numRows) {
			numMaxLag = (int)numRows - 1;
			spinnerInteger_NumMaxLag = numMaxLag;
		}

		if (this.booleanLimitToMaxLag) {
			//numMaxLag = numMaxLag;// - 1;
		} else {
			numMaxLag = (int)numRows;
		}	
	}

	protected void initialSequenceRange() {
		choiceRadioButt_SequenceRange = "Entire sequence";
	} 
	
	protected void initialLimitToMaxLag() {
		booleanLimitToMaxLag = true;
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
	
	/** Executed whenever the {@link #choiceRadioButt_AutoCorrelationMethod} parameter changes. */
	protected void callbackAutoCorrelationMethod() {
		logService.info(this.getClass().getName() + " Autocorrelation method set to " + choiceRadioButt_AutoCorrelationMethod);
	}
	
	/** Executed whenever the {@link #booleanLimitToMaxLag} parameter changes. */
	protected void callbackLimitToMaxLag() {
		logService.info(this.getClass().getName() + " Limit lags set to " + booleanLimitToMaxLag);
	}
	
	/** Executed whenever the {@link #spinnerInteger_NumMaxLag} parameter changes. */
	protected void callbackNumMaxLag() {
		numMaxLag = this.spinnerInteger_NumMaxLag;
		
		// number of lags > numDataPoints is not allowed
		if (numMaxLag >= numRows) {
			numMaxLag = (int)numRows - 1;
			spinnerInteger_NumMaxLag = numMaxLag;
		}

		if (this.booleanLimitToMaxLag) {
			//numMaxLag = numMaxLag;// - 1;
		} else {
			numMaxLag = (int)numRows;
		}	
		logService.info(this.getClass().getName() + " Lag set to " + spinnerInteger_NumMaxLag);
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
		numRows     = tableIn.getRowCount();
		
//		numSubsequentBoxes = (long) Math.floor((double)numRows/(double)spinnerInteger_BoxLength);
//		numGlidingBoxes = numRows - spinnerInteger_BoxLength + 1;
		
//		sliceLabels = new String[(int) numColumns];
			   
		logService.info(this.getClass().getName() + " Name: "      + tableInName); 
		logService.info(this.getClass().getName() + " Columns #: " + numColumns);
		logService.info(this.getClass().getName() + " Rows #: "    + numRows); 
	}

	/**
	* This method starts the workflow for a single column of the active display
	*/
	protected void startWorkflowForSingleColumn() {
		
		dlgProgress = new CsajDialog_WaitingWithProgressBar("Computing Autocorrelation, please wait... Open console window for further info.",
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
	
		dlgProgress = new CsajDialog_WaitingWithProgressBar("Computing Autocorrelation, please wait... Open console window for further info.",
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
		tableOut.add(new GenericColumn("Surrogate type"));
		tableOut.add(new GenericColumn("AC-Method"));
		for (int c = 0; c < numColumns; c++) {
			tableOut.add(new GenericColumn("AC-" + tableIn.getColumnHeader(c)));
		}	
		tableOut.appendRows((int) numMaxLag);
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
		if (optDeleteExistingImgs) {
			Frame frame;
			Frame[] listFrames = JFrame.getFrames();
			for (int i = listFrames.length -1 ; i >= 0; i--) { //Reverse order, otherwise focus is not given free from the last image
				frame = listFrames[i];
				//System.out.println("frame name: " + frame.getTitle());
				if (frame.getTitle().contains("Autocorrelation sequence(s)")) {
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
		CsajContainer_ProcessMethod containerPM = process(tableIn, s); 
		// 0 Entropy
		logService.info(this.getClass().getName() + " resultValues[0]: " + containerPM.item1_Values[0]);
		logService.info(this.getClass().getName() + " Processing finished.");
		writeToTable(s, containerPM);
		
		//eliminate empty columns
		leaveOverOnlyOneSequenceColumn(s+numTableOutPreCols); // + because of text columns
		
		//int selectedOption = JOptionPane.showConfirmDialog(null, "Do you want to display the Autocorrelation?\nNot recommended for a large number of sequences", "Display option", JOptionPane.YES_NO_OPTION); 
		//if (selectedOption == JOptionPane.YES_OPTION) {
			if (containerPM != null) { 
				int[] cols = new int[tableOut.getColumnCount()-numTableOutPreCols]; //- because of first text columns	
				boolean isLineVisible = true;
				String sequenceTitle = "Autocorrelation - " + this.choiceRadioButt_AutoCorrelationMethod;
				String xLabel = "#";
				String yLabel = "Value";
				String[] seriesLabels = new String[tableOut.getColumnCount()-numTableOutPreCols]; //- because of first text columns			
				for (int c = numTableOutPreCols; c < tableOut.getColumnCount(); c++) { //because of first text columns	
					cols[c-numTableOutPreCols] = c; //- because of first text columns	
					seriesLabels[c-numTableOutPreCols] = tableOut.getColumnHeader(c); //- because of first text columns					
				}
				CsajPlot_SequenceFrame pdf = new CsajPlot_SequenceFrame(tableOut, cols, isLineVisible, "Autocorrelation sequence(s)", sequenceTitle, xLabel, yLabel, seriesLabels);
				Point pos = pdf.getLocation();
				pos.x = (int) (pos.getX() - 100);
				pos.y = (int) (pos.getY() + 100);
				pdf.setLocation(pos);
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
		for (int i = numCols-1; i > 1; i--) {    //leave also first 2 text column
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
		
		//int selectedOption = JOptionPane.showConfirmDialog(null, "Do you want to display the Autocorrelations?\nNot recommended for a large number of sequences", "Display option", JOptionPane.YES_NO_OPTION); 
		//if (selectedOption == JOptionPane.YES_OPTION) {
			int[] cols = new int[tableOut.getColumnCount()-numTableOutPreCols]; //- because of first text columns	
			boolean isLineVisible = true;
			String sequenceTitle = "Autocorrelation - " + this.choiceRadioButt_AutoCorrelationMethod;
			String xLabel = "#";
			String yLabel = "Value";
			String[] seriesLabels = new String[tableOut.getColumnCount()-numTableOutPreCols]; //- because of first text columns		
			for (int c = numTableOutPreCols; c < tableOut.getColumnCount(); c++) { // because of first text columns	
				cols[c-numTableOutPreCols] = c;  //- because of first text columns	
				seriesLabels[c-numTableOutPreCols] = tableOut.getColumnHeader(c);	//- because of first text columns				
			}
			CsajPlot_SequenceFrame pdf = new CsajPlot_SequenceFrame(tableOut, cols, isLineVisible, "Autocorrelation sequence(s)", sequenceTitle, xLabel, yLabel, seriesLabels);
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
				tableOut.set(0, r, this.choiceRadioButt_SurrogateType);
				tableOut.set(1, r, this.choiceRadioButt_AutoCorrelationMethod);
				tableOut.set(sequenceNumber + numTableOutPreCols, r, Double.NaN); //+ because of first text columns	
			}
		}
		else {
			for (int r = 0; r < containerPM.item1_Values.length; r++ ) {
				tableOut.set(0, r, this.choiceRadioButt_SurrogateType);
				tableOut.set(1, r, this.choiceRadioButt_AutoCorrelationMethod);
				tableOut.set(sequenceNumber + numTableOutPreCols, r, containerPM.item1_Values[r]); //+ because of first text columns	
			}
			
			//Fill up with NaNs (this can be because of NaNs in the input sequence or deletion of zeroes)
			if (tableOut.getRowCount() > containerPM.item1_Values.length) {
				for (int r = containerPM.item1_Values.length; r < tableOut.getRowCount(); r++ ) {
					tableOut.set(0, r, this.choiceRadioButt_SurrogateType);
					tableOut.set(1, r, this.choiceRadioButt_AutoCorrelationMethod);
					tableOut.set(sequenceNumber + numTableOutPreCols, r, Double.NaN); //+ because of first text columns	
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
		
		String  sequenceRange = choiceRadioButt_SequenceRange;
		String  surrType      = choiceRadioButt_SurrogateType;
		//numSurrogates        = spinnerInteger_NumSurrogates;
		//numBoxLength         = spinnerInteger_BoxLength;
		int     numDataPoints = dgt.getRowCount();
		boolean skipZeroes    = booleanSkipZeroes;
		int numMaxLag         = this.numMaxLag; 
		
		//******************************************************************************************************
		//domain1D  = new double[numDataPoints];
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
		
		//int numActualRows = 0;
		logService.info(this.getClass().getName() + " Column #: "+ (col+1) + "  " + sequenceColumn.getHeader() + "  Size of sequence = " + numDataPoints);	
		if (numDataPoints == 0) return null; //e.g. if sequence had only NaNs
		
		//domain1D = new double[numDataPoints];
		//for (int n = 0; n < numDataPoints; n++) domain1D[n] = n+1
		
		// number of max lag >= numDataPoints is not allowed
		if (numMaxLag >= numDataPoints) {
			numMaxLag = numDataPoints;
		}
		double[] sequenceAuCorr = new double[(int) numMaxLag];
		//double[] rangeAuCorr  = new double[numMaxLag];
		for (int d = 0; d < numMaxLag; d++) {
			sequenceAuCorr[d]   = Double.NaN;
			//rangeAuCorr[d]  = Double.NaN;
		}
			
	
			
		//"Entire sequence", "Subsequent boxes", "Gliding box" 
		//********************************************************************************************************
		if (sequenceRange.equals("Entire sequence")){	//only this option is possible for autocorrelation
			
			if (!surrType.equals("No surrogates")) {
				CsajAlgorithm_Surrogate1D surrogate1D = new CsajAlgorithm_Surrogate1D();	
				//choices = {"No surrogates", "Shuffle", "Gaussian", "Random phase", "AAFT"},
				String windowingType = "Rectangular";
				if (surrType.equals("Shuffle"))      sequence1D = surrogate1D.calcSurrogateShuffle(sequence1D);
				if (surrType.equals("Gaussian"))     sequence1D = surrogate1D.calcSurrogateGaussian(sequence1D);
				if (surrType.equals("Random phase")) sequence1D = surrogate1D.calcSurrogateRandomPhase(sequence1D, windowingType);
				if (surrType.equals("AAFT"))         sequence1D = surrogate1D.calcSurrogateAAFT(sequence1D, windowingType);
			}
			
			//logService.info(this.getClass().getName() + " Column #: "+ (col+1) + "  " + sequenceColumn.getHeader() + "  Size of sequence = " + sequence1D.length);
			//if (sequence1D.length == 0) return null; //e.g. if sequence had only NaNs
			
			if (choiceRadioButt_AutoCorrelationMethod.equals("Large N")) {
				sequenceAuCorr = new double[(int) numMaxLag];
				//rangeAuCorr  = new double[numMaxLag];
				for (int d = 0; d < numMaxLag; d++) {
					sequenceAuCorr[d] = Double.NaN;
					//rangeAuCorr[d]  = Double.NaN;
				}
				double mean = this.calcMean(sequence1D);
				for (int d = 0; d < numMaxLag; d++) {
					double a = 0.0d;
					double b = 0.0d;
					for (int i = 0; i < numDataPoints - d; i++) {
						a = a + ((sequence1D[i] - mean) * (sequence1D[i + d] - mean));
						b = b + ((sequence1D[i] - mean) * (sequence1D[i] - mean));
					}
					// finalize sum for b
					for (int i = numDataPoints - d - 1; i < numDataPoints; i++) { 
						b = b + ((sequence1D[i] - mean) * (sequence1D[i] - mean));
					}
					if ((a == 0) && (b == 0)) {
						sequenceAuCorr[d] = 1.0d;
					} else {
						sequenceAuCorr[d] = (a / b);
					}
				}
				//rangeAuCorr = plotModel.getDomain();
			}
			else if (choiceRadioButt_AutoCorrelationMethod.equals("Small N")) {
				sequenceAuCorr = new double[(int) numMaxLag];
				//rangeAuCorr  = new double[numMaxLag];
				for (int d = 0; d < numMaxLag; d++) {
					sequenceAuCorr[d] = Double.NaN;
					//rangeAuCorr[d]  = Double.NaN;
				}
				double mean1 = this.calcMean(sequence1D);
				double[] sequence2;
				for (int d = 0; d < numMaxLag; d++) {
					sequence2 = new double[sequence1D.length - d];
					for (int i = 0; i < sequence2.length; i++) {
						sequence2[i] = sequence1D[i + d];
					}
					double mean2 = this.calcMean(sequence2);
					double a = 0.0d;
					double b = 0.0d;
					double c = 0.0d;
					for (int i = 0; i < sequence1D.length - d; i++) {
						a = a + ((sequence1D[i]  - mean1) * (sequence2[i]  - mean2));
						b = b + ((sequence1D[i]  - mean1) * (sequence1D[i] - mean1));
						c = c + ((sequence2[i]   - mean2) * (sequence2[i]  - mean2));
					}
					b = Math.sqrt(b);
					c = Math.sqrt(c);

					if ((a == 0) && (b == 0) && (c == 0)) {
						sequenceAuCorr[d] = 1.0d;
					} else {
						sequenceAuCorr[d] = a / (b * c);
					}
				}
				//rangeAuCorr = plotModel.getDomain();
			}
			else if (choiceRadioButt_AutoCorrelationMethod.equals("FFT")) {
				//Oppenheim & Schafer, DiscreteTimeSequenceProcessing-ed3-2010 p.854
				//Wiener-Khinchin theorem
				//Fourier transform the sequence and compute the power (magnitude^2  = |fft|^2)
				//The ACF is the inverse Fourier transform of the power
				//or
				//Fourier transform the sequence 
				//Multiply result with conjugate of result   (  |fft|^2 = fft*fft_conjugate   )
				//ACF is the inverse FFT
				
				// data length must have a power of 2
				int powerSize = 1;
				while (numMaxLag > powerSize) {
					powerSize = powerSize * 2;
				}
				double[] data = new double[powerSize];
				// set data
				if (powerSize <= sequence1D.length) { //
					for (int i = 0; i < powerSize; i++) {
						data[i] = sequence1D[i];
					}
				} else {
					for (int i = 0; i < sequence1D.length; i++) {
						data[i] = sequence1D[i];
					}
					for (int i = sequence1D.length; i < powerSize; i++) {
						data[i] = 0.0d;
					}
				}	
				
				sequenceAuCorr = new double[numMaxLag];
				//rangeAuCorr  = new double[numMaxLag];
				
				//double[] autoCorr = new double[data.length];
			    // Assumes n is even.
				FastFourierTransformer transformer = new FastFourierTransformer(DftNormalization.STANDARD);
				        
				Complex[] complx  = transformer.transform(data, TransformType.FORWARD);
		
//				//with magnitude
//				double[] magnitude = new double[complx.length];
//				for (int i = 0; i < complx.length; i++) {               
//					//magnitude[i] = Math.sqrt(complx[i].getReal()*complx[i].getReal() + complx[i].getImaginary()*complx[i].getImaginary()); //Magnitude
//					magnitude[i] = complx[i].abs(); //magnitude
//				}
//				//magnitude[0] = 0;
//				complx = transformer.transform(magnitude, TransformType.INVERSE);
//				
//				for (int i = 0; i < sequenceAuCorr.length; i++) {
//					sequenceAuCorr[i] = complx[i].getReal() / sequence1D.length; //complx[0].getReal(); //normalize to 1
//				}
				
				
				//or with complex times the conjugate of the same complex (or for cross correlation a second complex) 
				//-------------------------------------------------------------------
				for (int i = 0; i < complx.length; i++) {               
					complx[i] = complx[i].multiply(complx[i].conjugate());
				}
				complx = transformer.transform(complx, TransformType.INVERSE);
				
				for (int i = 0; i < sequenceAuCorr.length; i++) {
					sequenceAuCorr[i] = complx[i].getReal() / sequence1D.length; ///complx[0].getReal(); //normalize to 1
				}
				//-------------------------------------------------------------------
					
			}
			
		//********************************************************************************************************	
		} else if (sequenceRange.equals("Subsequent boxes")){ //not for Autocorrelation
		
		//********************************************************************************************************			
		} else if (sequenceRange.equals("Gliding box")){ //not for autocorrelation
		
		}
		
		return new CsajContainer_ProcessMethod(sequenceAuCorr);
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
		ij.command().run(Csaj1DOpenerCommand.class, true).get();
		//open and run Plugin
		ij.command().run(MethodHandles.lookup().lookupClass().getName(), true);
	}
}
