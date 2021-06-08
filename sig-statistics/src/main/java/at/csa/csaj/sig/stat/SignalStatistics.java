/*-
 * #%L
 * Project: ImageJ signal plugin for computing statistics.
 * File: SignalStatistics.java
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
package at.csa.csaj.sig.stat;

import java.awt.Toolkit;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import at.csa.csaj.sig.open.SignalOpener;

/**
 * A {@link Command} plugin computing <Statistics</a>
 * of a signal.
 */
@Plugin(type = InteractiveCommand.class,
	headless = true,
	label = "Statistics",
	menu = {
	@Menu(label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT, mnemonic = MenuConstants.PLUGINS_MNEMONIC),
	@Menu(label = "ComsystanJ"),
	@Menu(label = "Signal"),
	@Menu(label = "Statistics", weight = 10)})
public class SignalStatistics<T extends RealType<T>> extends InteractiveCommand implements Command, Previewable { // non blocking  GUI
//public class SignalStatistics<T extends RealType<T>> implements Command {	//modal GUI

	private static final String PLUGIN_LABEL            = "<html><b>Signal statistics<b/></html>";
	private static final String SPACE_LABEL             = "";
	private static final String ANALYSISOPTIONS_LABEL   = "<html><b>Analysis options</b></html>";
	private static final String BACKGROUNDOPTIONS_LABEL = "<html><b>Background option</b></html>";
	private static final String DISPLAYOPTIONS_LABEL    = "<html><b>Display option</b></html>";
	private static final String PROCESSOPTIONS_LABEL    = "<html><b>Process options</b></html>";

	private static DefaultGenericTable  tableIn;

	private static double[] signal1D;
	private static double[] domain1D;
	private static Double valueDataPoint;
	private static double[] subSignal1D;
	private static double[] surrSignal1D;
	Column<? extends Object> signalColumn;

	private static String tableInName;
	private static String[] sliceLabels;
	private static long numColumns = 0;
	private static long numRows = 0;
	private static long numDimensions = 0;
	private static int  numSurrogates = 0;
	private static long numBoxLength = 0;
	private static long numSubsequentBoxes = 0;
	private static long numGlidingBoxes = 0;
	private static ArrayList<RegressionPlotFrame> doubleLogPlotList = new ArrayList<RegressionPlotFrame>();

	private static final String tableName = "Table - Signal statistics";
	
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
	//private DefaultGenericTable tableIn;

	@Parameter(type = ItemIO.OUTPUT)
	private DefaultGenericTable tableResult;


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

	@Parameter(label = "Analysis type",
		description = "Entire signal, Subsequent boxes or Gliding box",
		style = ChoiceWidget.LIST_BOX_STYLE,
		choices = {"Entire signal", "Subsequent boxes", "Gliding box"}, 
		//persist  = false,  //restore previous value default = true
		initializer = "initialAnalysisType",
		callback = "callbackAnalysisType")
	private String choiceRadioButt_AnalysisType;
	
	@Parameter(label = "(Entire signal) Surrogates",
			description = "Surrogates types - Only for Entire signal type!",
			style = ChoiceWidget.LIST_BOX_STYLE,
			choices = {"No surrogates", "Shuffle", "Gaussian", "Random phase", "AAFT"}, 
			persist  = false,  //restore previous value default = true
			initializer = "initialSurrogateType",
			callback = "callbackSurrogateType")
		private String choiceRadioButt_SurrogateType;
	
	@Parameter(label = "Surrogates #", description = "Number of computed surrogates", style = NumberWidget.SPINNER_STYLE, 
			   min = "1", max = "9999999999999999999999999", stepSize = "1",
			   persist = false, // restore  previous value  default  =  true
			   initializer = "initialNumSurrogates", callback = "callbackNumSurrogates")
	private int spinnerInteger_NumSurrogates;
	
	@Parameter(label = "Box length", description = "Length of subsequent or gliding box", style = NumberWidget.SPINNER_STYLE, 
			   min = "2", max = "9999999999999999999999999", stepSize = "1",
			   persist = false, // restore  previous value  default  =  true
			   initializer = "initialBoxLength", callback = "callbackBoxLength")
	private int spinnerInteger_BoxLength;

	//-----------------------------------------------------------------------------------------------------
//	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
//	private final String labelBackgroundOptions = BACKGROUNDOPTIONS_LABEL;
	
	@Parameter(label = "Remove zero values", persist = false,
		       initializer = "initialRemoveZeroes", callback = "callbackRemoveZeroes")
	private boolean booleanRemoveZeroes;
	
	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelDisplayOptions = DISPLAYOPTIONS_LABEL;

	@Parameter(label = "Overwrite result display(s)",
	    	description = "Overwrite already existing result images, plots or tables",
	    	//persist  = false,  //restore previous value default = true
			initializer = "initialOverwriteDisplays")
	private boolean booleanOverwriteDisplays;

	//************************************************************************
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelProcessOptions = PROCESSOPTIONS_LABEL;

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
    //The following initialzer functions set initial values
     
    protected void initialAnalysisType() {
    	choiceRadioButt_AnalysisType = "Entire signal";
    }   
	protected void initialNumSurrogates() {
		numSurrogates = 10;
		spinnerInteger_NumSurrogates = numSurrogates;
	}	
	protected void initialBoxLength() {
		numBoxLength = 100;
		spinnerInteger_BoxLength =  (int) numBoxLength;
	}	
	protected void initialRemoveZeroes() {
		booleanRemoveZeroes = false;
	}	
	protected void initialOverwriteDisplays() {
    	booleanOverwriteDisplays = true;
}
	
	//-------------------------------------------------------------------------
	// The following method is known as "callback" which gets executed
	// whenever the value of a specific linked parameter changes.

	/** Executed whenever the {@link #choiceRadioButt_AnalysisType} parameter changes. */
	protected void callbackAnalysisType() {
		logService.info(this.getClass().getName() + " Signal type set to " + choiceRadioButt_AnalysisType);
		if (!choiceRadioButt_AnalysisType.equals("Entire signal")){
			choiceRadioButt_SurrogateType = "No surrogates";
			callbackSurrogateType();
		}
	}
	
	/** Executed whenever the {@link #choiceRadioButt_SurrogateType} parameter changes. */
	protected void callbackSurrogateType() {	
		if (!choiceRadioButt_AnalysisType.equals("Entire signal")){
			choiceRadioButt_SurrogateType = "No surrogates";
			logService.info(this.getClass().getName() + " Surrogates not allowed for subsequent or gliding boxes!");
		}	
		logService.info(this.getClass().getName() + " Surrogate type set to " + choiceRadioButt_SurrogateType);
	}
	
	/** Executed whenever the {@link #spinInteger_NumSurrogates} parameter changes. */
	protected void callbackNumSurrogates() {
		numSurrogates = spinnerInteger_NumSurrogates;
		logService.info(this.getClass().getName() + " Number of surrogates set to " + spinnerInteger_NumSurrogates);
	}
	
	/** Executed whenever the {@link #spinInteger_BoxLength} parameter changes. */
	protected void callbackBoxLength() {
		numBoxLength = spinnerInteger_BoxLength;
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
	
	/** Executed whenever the {@link #spinInteger_NumColumn} parameter changes. */
	protected void callbackNumColumn() {
		getAndValidateActiveDataset();
		if (spinnerInteger_NumColumn > tableIn.getColumnCount()){
			logService.info(this.getClass().getName() + " No more columns available");
			spinnerInteger_NumColumn = tableIn.getColumnCount();
		}
		logService.info(this.getClass().getName() + " Column number set to " + spinnerInteger_NumColumn);
	}
	
	/**
	 * Executed whenever the {@link #buttonProcessSinglecolumn} button is pressed.
	 */
	protected void callbackProcessSingleColumn() {
		//prepare  executer service
		exec = Executors.newSingleThreadExecutor();
		
		//dlgProgress = new WaitingDialogWithProgressBar("<html>Computing signal statistics, please wait...<br>Open console window for further info.</html>");
		dlgProgress = new WaitingDialogWithProgressBar("Computing signal statistics, please wait... Open console window for further info.",
																					logService, false, exec); //isCanceable = false, because no following method listens to exec.shutdown 
		dlgProgress.updatePercent("");
		dlgProgress.setBarIndeterminate(true);
		dlgProgress.setVisible(true);
		
    	exec.execute(new Runnable() {
            public void run() {
        	    try {
        	    	logService.info(this.getClass().getName() + " Processing single signal");
        	    	deleteExistingDisplays();
        	    	getAndValidateActiveDataset();
            		generateTableHeader(); 		
            		int activeColumnIndex = getActiveColumnIndex();
            		//processActiveInputColumn(activeColumnIndex, dlgProgress);
              		if (spinnerInteger_NumColumn <= numColumns) processSingleInputColumn(spinnerInteger_NumColumn - 1);
            		dlgProgress.addMessage("Processing finished! Preparing result table...");          		
            		//collectActiveResultAndShowTable(activeColumnIndex);
            		showTable();
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
	 * Executed whenever the {@link #buttonProcessAllSignals} button is pressed. This
	 * is the main processing method usually implemented in the run() method for
	 */
	protected void callbackProcessAllColumns() {
		//prepare  executer service
		exec = Executors.newSingleThreadExecutor();
		//exec =  defaultThreadService.getExecutorService();
		
		//dlgProgress = new WaitingDialogWithProgressBar("<html>Computing signal statistics, please wait...<br>Open console window for further info.</html>");
		dlgProgress = new WaitingDialogWithProgressBar("Computing signal statistics, please wait... Open console window for further info.",
																					logService, true, exec); //isCanceable = true, because processAllInputSignals(dlgProgress) listens to exec.shutdown 
		dlgProgress.setVisible(true);

		exec.execute(new Runnable() {
            public void run() {	
            	try {
	            	logService.info(this.getClass().getName() + " Processing all available columns");
	            	deleteExistingDisplays();
	        		getAndValidateActiveDataset();
	        		generateTableHeader();
	        		processAllInputColumns();
	        		dlgProgress.addMessage("Processing finished! Preparing result table...");
	        		//collectAllResultsAndShowTable();
	        		showTable();
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
		if (booleanProcessImmediately) callbackProcessSingleColumn();
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
			this.callbackProcessAllColumns();
		}
	}

	public void getAndValidateActiveDataset() {

		//DefaultTableDisplay dtd = (DefaultTableDisplay) displays.get(0);
		tableIn = (DefaultGenericTable) defaultTableDisplay.get(0);
	
		// get some info
		tableInName = defaultTableDisplay.getName();
		numColumns  = tableIn.getColumnCount();
		numRows     = tableIn.getRowCount();
	
		numSubsequentBoxes = (long) Math.floor((double)numRows/(double)spinnerInteger_BoxLength);
		numGlidingBoxes = numRows - spinnerInteger_BoxLength + 1;
	
		sliceLabels = new String[(int) numColumns];
	           
		logService.info(this.getClass().getName() + " Name: "      + tableInName); 
		logService.info(this.getClass().getName() + " Columns #: " + numColumns);
		logService.info(this.getClass().getName() + " Rows #: "    + numRows); 
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
	
		tableResult = new DefaultGenericTable();
		tableResult.add(new GenericColumn("File name"));
		tableResult.add(new GenericColumn("Column name"));	
		tableResult.add(new GenericColumn("Analysis type"));
		tableResult.add(new GenericColumn("Surrogate type"));
		tableResult.add(new IntColumn("# Surrogates"));
		tableResult.add(new IntColumn("Box length"));
		tableResult.add(new BoolColumn("Zeroes removed"));
	
		//"Entire signal", "Subsequent boxes", "Gliding box" 
		if (choiceRadioButt_AnalysisType.equals("Entire signal")){
			tableResult.add(new DoubleColumn("# Data points"));	
			tableResult.add(new DoubleColumn("Min"));	
			tableResult.add(new DoubleColumn("Max"));
			tableResult.add(new DoubleColumn("Median"));
			tableResult.add(new DoubleColumn("RMS"));
			tableResult.add(new DoubleColumn("Mean"));
			tableResult.add(new DoubleColumn("SD"));
			tableResult.add(new DoubleColumn("Kurtosis"));
			tableResult.add(new DoubleColumn("Skewness"));
			tableResult.add(new DoubleColumn("Sum"));
			tableResult.add(new DoubleColumn("Sum of squares"));
			
			
			if (choiceRadioButt_SurrogateType.equals("No surrogates")) {
				//do nothing	
			} else { //Surrogates
				tableResult.add(new DoubleColumn("Median_Surr")); //Mean surrogate value	
				tableResult.add(new DoubleColumn("Mean_Surr")); //Mean surrogate value
				tableResult.add(new DoubleColumn("SD_Surr")); //Mean surrogate value
				for (int s = 0; s < numSurrogates; s++) tableResult.add(new DoubleColumn("Median_Surr-#"+(s+1))); 
				for (int s = 0; s < numSurrogates; s++) tableResult.add(new DoubleColumn("Mean_Surr-#"+(s+1))); 
				for (int s = 0; s < numSurrogates; s++) tableResult.add(new DoubleColumn("SD_Surr-#"+(s+1))); 
			}			
			
		} 
		else if (choiceRadioButt_AnalysisType.equals("Subsequent boxes")){
			for (int n = 1; n <= numSubsequentBoxes; n++) {
				tableResult.add(new DoubleColumn("Median_" + n));	
			}
			for (int n = 1; n <= numSubsequentBoxes; n++) {
				tableResult.add(new DoubleColumn("Mean_" + n));	
			}
			for (int n = 1; n <= numSubsequentBoxes; n++) {
				tableResult.add(new DoubleColumn("SD_" + n));	
			}
		}
		else if (choiceRadioButt_AnalysisType.equals("Gliding box")){
			for (int n = 1; n <= numGlidingBoxes; n++) {
				tableResult.add(new DoubleColumn("Median_" + n));	
			}
			for (int n = 1; n <= numGlidingBoxes; n++) {
				tableResult.add(new DoubleColumn("Mean_" + n));	
			}
			for (int n = 1; n <= numGlidingBoxes; n++) {
				tableResult.add(new DoubleColumn("SD_" + n));	
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
				if (display.getName().equals(tableName))
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
	private void processSingleInputColumn (int c) throws InterruptedException {
		
		long startTime = System.currentTimeMillis();
		
		//Compute result values
		double[] resultValues = process(tableIn, c); 
		// 0 numDataPoints 1 Min 2 Max 3 Median 4 QuMean 5 Mean 6 SD 7 Kurt 8 Skew 9 Sum 10 SumSqr
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
	private void processAllInputColumns() throws InterruptedException{
		
		long startTimeAll = System.currentTimeMillis();
		
		// loop over all slices of stack
		for (int s = 0; s < numColumns; s++) { // s... number of signal columns
			if (!exec.isShutdown()){
				int percent = (int)Math.round((  ((float)s)/((float)numColumns)   *100.f   ));
				dlgProgress.updatePercent(String.valueOf(percent+"%"));
				dlgProgress.updateBar(percent);
				//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
				statusService.showStatus((s+1), (int)numColumns, "Processing " + (s+1) + "/" + (int)numColumns);
	
				long startTime = System.currentTimeMillis();
				logService.info(this.getClass().getName() + " Processing signal column number " + (s+1) + "(" + numColumns + ")");
				
				// Compute result values
				double[] resultValues = process(tableIn, s);
				// 0 numDataPoints 1 Min 2 Max 3 Median 4 QuMean 5 Mean 6 SD 7 Kurt 8 Skew 9 Sum 10 SumSqr
				logService.info(this.getClass().getName() + " Processing finished.");
				writeToTable(s, s, resultValues);
	
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
		
		// 0 numDataPoints 1 Min 2 Max 3 Median 4 QuMean 5 Mean 6 SD 7 Kurt 8 Skew 9 Sum 10 SumSqr
		// fill table with values
		tableResult.appendRow();
		tableResult.set(0, row, tableInName);//File Name
		if (sliceLabels != null)  tableResult.set(1, row, tableIn.getColumnHeader(signalNumber)); //Column Name
		tableResult.set(2, row, choiceRadioButt_AnalysisType); //Signal Method
		tableResult.set(3, row, choiceRadioButt_SurrogateType); //Surrogate Method
		if (choiceRadioButt_AnalysisType.equals("Entire signal") && (!choiceRadioButt_SurrogateType.equals("No surrogates"))) {
			tableResult.set(4, row, spinnerInteger_NumSurrogates); //# Surrogates
		} else {
			tableResult.set(4, row, null); //# Surrogates
		}
		if (!choiceRadioButt_AnalysisType.equals("Entire signal")){
			tableResult.set(5, row, spinnerInteger_BoxLength); //Box Length
		} else {
			tableResult.set(5, row, null);
		}	
		tableResult.set(6, row, booleanRemoveZeroes); //Zeroes removed
		tableColLast = 6;
		
		
		//"Entire signal", "Subsequent boxes", "Gliding box" 
		if (choiceRadioButt_AnalysisType.equals("Entire signal")){
			int numParameters = resultValues.length;
			tableColStart = tableColLast + 1;
			tableColEnd = tableColStart + numParameters;
			for (int c = tableColStart; c < tableColEnd; c++ ) {
				tableResult.set(c, row, resultValues[c-tableColStart]);
			}	
			if (choiceRadioButt_SurrogateType.equals("No surrogates")) {
				//do nothing	
			} else { //Surrogates
				//already set
			}	
		} 
		else if (choiceRadioButt_AnalysisType.equals("Subsequent boxes")){
			//Median Mean SD
			tableColStart = tableColLast +1;
			tableColEnd = (int) (tableColStart + 3 * numSubsequentBoxes); //3 parameters
			for (int c = tableColStart; c < tableColEnd; c++ ) {
				tableResult.set(c, row, resultValues[c-tableColStart]);
			}	
		}
		else if (choiceRadioButt_AnalysisType.equals("Gliding box")){
			//Median Mean SD
			tableColStart = tableColLast +1;
			tableColEnd = (int) (tableColStart + 3 * numGlidingBoxes); //3 parameters 
			for (int c = tableColStart; c < tableColEnd; c++ ) {
				tableResult.set(c, row, resultValues[c-tableColStart]);
			}	
		}	
	}
	
	/**
	 * shows the result table
	 */
	private void showTable() {
		// Show table
		uiService.show(tableName, tableResult);
		//This might free some memory
		tableResult = null;
	}
	

	/**
	*
	* Processing
	*/
	private double[] process(DefaultGenericTable dgt, int col) { //  c column number
	
		String analysisType    = choiceRadioButt_AnalysisType;
		String surrType      = choiceRadioButt_SurrogateType;
		int boxLength        = spinnerInteger_BoxLength;
		int numDataPoints    = dgt.getRowCount();
		boolean removeZeores = booleanRemoveZeroes;
		double[] resultValues = null;
		// Get a DescriptiveStatistics instance
		DescriptiveStatistics stats = null;
		
		//******************************************************************************************************
		//domain1D  = new double[numDataPoints];
		signal1D = new double[numDataPoints];
		
		signalColumn = dgt.get(col);
		
		for (int n = 0; n < numDataPoints; n++) {
			//domain1D[n] = n+1;
			signal1D[n] = Double.valueOf((Double)signalColumn.get(n));
		}	
		
		signal1D = removeNaN(signal1D);
		if (removeZeores) signal1D = removeZeroes(signal1D);
	
		//int numActualRows = 0;
		logService.info(this.getClass().getName() + " Column #: "+ (col+1) + "  " + signalColumn.getHeader() + "  Size of signal = " + numDataPoints);	
	
		//"Entire signal", "Subsequent boxes", "Gliding box"
		//********************************************************************************************************
		if (analysisType.equals("Entire signal")){
			if (surrType.equals("No surrogates")) {
				resultValues = new double[11]; // 0 numDataPoints 1 Min 2 Max 3 Median 4 QuMean 5 Mean 6 SD 7 Kurt 8 Skew 9 Sum 10 SumSqr
			} else {
				resultValues = new double[11+3+3*numSurrogates]; // Median_Surr, Mean_Surr, SD_Surr,	Median_Surr1, Median_Surr2, Median_Surr3, ...... Mean_Surr1  2, 3 .......
			}
			for (int r = 0; r<resultValues.length; r++) resultValues[r] = Double.NaN;
			
			// Get a DescriptiveStatistics instance
			stats = new DescriptiveStatistics();
			// Add the data from the array
			for( int i = 0; i < signal1D.length; i++) {
				//valueDataPoint = Double.valueOf((Double)column.get(i));
				valueDataPoint = signal1D[i];
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
				surrSignal1D = new double[signal1D.length];
				
				double sumMedians   = 0.0;
				double sumMeans    = 0.0;
				double sumSDs = 0.0;
				Surrogate surrogate = new Surrogate();
				for (int s = 0; s < numSurrogates; s++) {
					//choices = {"No surrogates", "Shuffle", "Gaussian", "Random phase", "AAFT"}, 
					if (surrType.equals("Shuffle"))      surrSignal1D = surrogate.calcSurrogateShuffle(signal1D);
					if (surrType.equals("Gaussian"))     surrSignal1D = surrogate.calcSurrogateGaussian(signal1D);
					if (surrType.equals("Random phase")) surrSignal1D = surrogate.calcSurrogateRandomPhase(signal1D);
					if (surrType.equals("AAFT"))         surrSignal1D = surrogate.calcSurrogateAAFT(signal1D);
			
					// Get a DescriptiveStatistics instance
					stats = new DescriptiveStatistics();
					// Add the data from the array
					for( int i = 0; i < surrSignal1D.length; i++) {
						//valueDataPoint = Double.valueOf((Double)column.get(i));
						valueDataPoint = surrSignal1D[i];
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
		else if (analysisType.equals("Subsequent boxes")){
			resultValues = new double[(int) (3*numSubsequentBoxes)]; // Median Mean SD == three * number of boxes	
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
				// Get a DescriptiveStatistics instance
				stats = new DescriptiveStatistics();
				for (int ii = 0; ii < boxLength; ii++){ 
					valueDataPoint = subSignal1D[ii];
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
		else if (analysisType.equals("Gliding box")){
			resultValues = new double[(int) (3*numGlidingBoxes)]; // Median Mean SD == three * number of boxes		
			for (int r = 0; r<resultValues.length; r++) resultValues[r] = Double.NaN;
			subSignal1D = new double[(int) boxLength];
			//number of boxes may be smaller because of NaNs or removed zeroes
			long actualNumGlidingBoxes = signal1D.length - spinnerInteger_BoxLength + 1;
			//get sub-signals and push it directly to stats
			for (int i = 0; i < actualNumGlidingBoxes; i++) {
				logService.info(this.getClass().getName() + " Processing gliding box #: "+(i+1) + "/" + actualNumGlidingBoxes);	
				int start = i;
				for (int ii = start; ii < (start + boxLength); ii++){ 
					subSignal1D[ii-start] = signal1D[ii];
				}	
				//Compute specific values************************************************
				// Get a DescriptiveStatistics instance
				stats = new DescriptiveStatistics();
				for (int ii = 0; ii < boxLength; ii++){ 
					valueDataPoint = subSignal1D[ii];
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
	 	
		return resultValues;
		// 0 numDataPoints 1 Min 2 Max 3 Median 4 QuMean 5 Mean 6 SD 7 Kurt 8 Skew 9 Sum 10 SumSqr
		// Output
		// uiService.show(tableName, table);
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
		ij.command().run(SignalStatistics.class, true);
	}
}
