/*-
 * #%L
 * Project: ImageJ signal plugin for generating a Poincare plot
 * File: SignalPoincarePlot.java
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

package at.csa.csaj.sig.poincare;

import java.awt.Toolkit;
import java.text.SimpleDateFormat;
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
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;
import org.scijava.table.Column;
import org.scijava.table.DefaultGenericTable;
import org.scijava.table.DefaultTableDisplay;
import org.scijava.ui.UIService;
import org.scijava.widget.Button;
import org.scijava.widget.ChoiceWidget;
import org.scijava.widget.NumberWidget;
import at.csa.csaj.commons.signal.algorithms.Surrogate;
import at.csa.csaj.commons.dialog.WaitingDialogWithProgressBar;
import at.csa.csaj.commons.plot.PlotDisplayFrame;
import at.csa.csaj.sig.open.SignalOpener;


/**
 * A {@link Command} plugin computing <Poincare plots</a>
 * of a signal.
 */
@Plugin(type = InteractiveCommand.class, headless = true, menuPath = "Plugins>ComsystanJ>Signal>Poincare plot")
public class SignalPoincarePlot<T extends RealType<T>> extends InteractiveCommand implements Command, Previewable { // non blocking  GUI
//public class SignalPoincarePlot<T extends RealType<T>> implements Command {	//modal GUI

	private static final String PLUGIN_LABEL                = "<html><b>Poincare plot</b></html>";
	private static final String SPACE_LABEL                 = "";
	private static final String POINCAREPLOT_LABEL          = "<html><b>Poincare plot option</b></html>";
	private static final String SIGNALOPTIONS_LABEL         = "<html><b>Signal options</b></html>";
	private static final String BACKGROUNDOPTIONS_LABEL     = "<html><b>Background option</b></html>";
	private static final String DISPLAYOPTIONS_LABEL        = "<html><b>Display option</b></html>";
	private static final String PROCESSOPTIONS_LABEL        = "<html><b>Process options</b></html>";
	
	private static double[] signal1D;
	private static double[] xAxis1D;
	private static double[] subSignal1D;
	private static double[] surrSignal1D;
	Column<? extends Object> signalColumn;
	
	private static String tableInName;
	private static String[] sliceLabels;
	private static long numColumns = 0;
	private static long numRows = 0;
	private static int  numSurrogates = 0;
	private static int  numBoxLength = 0;
	private static long numSubsequentBoxes = 0;
	private static long numGlidingBoxes = 0;
	
	private static int numLag = 1;

	
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
	

	// Widget elements------------------------------------------------------

	//@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	//private final String labelPlugin = PLUGIN_LABEL;

//	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
//	private final String labelSpace = SPACE_LABEL;
	
	@Parameter(type = ItemIO.INPUT)
	private DefaultTableDisplay  defaultTableDisplay;

	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelAutoCorrelationMethod = POINCAREPLOT_LABEL;
	
	@Parameter(label = "Lag", description = "(time) lag", style = NumberWidget.SPINNER_STYLE, min = "0", max = "1000", stepSize = "1",
			   persist = false, // restore  previous value  default  =  true
			   initializer = "initialNumLag", callback = "callbackNumLag")
	private int spinnerInteger_NumLag;

	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelSignalOptions = SIGNALOPTIONS_LABEL;
	
	@Parameter(label = "Signal type",
			description = "Entire signal, Subsequent boxes or Gliding box",
			style = ChoiceWidget.LIST_BOX_STYLE,
			choices = {"Entire signal"}, //, "Subsequent boxes", "Gliding box"}, 
			//persist  = false,  //restore previous value default = true
			initializer = "initialSignalType",
			callback = "callbackSignalType")
		private String choiceRadioButt_SignalType;
	
	@Parameter(label = "(Entire signal) Surrogates",
			description = "Surrogates types - Only for Entire signal type!",
			style = ChoiceWidget.LIST_BOX_STYLE,
			choices = {"No surrogates", "Shuffle", "Gaussian", "Random phase", "AAFT"}, 
			persist  = false,  //restore previous value default = true
			initializer = "initialSurrogateType",
			callback = "callbackSurrogateType")
	private String choiceRadioButt_SurrogateType;
	
//	@Parameter(label = "# Surrogates:", description = "Number of computed surrogates", style = NumberWidget.SPINNER_STYLE, 
//			   min = "1", max = "9999999999999999999", stepSize = "1",
//			   persist = false, // restore  previous value  default  =  true
//			   initializer = "initialNumSurrogates", callback = "callbackNumSurrogates")
//	private int spinnerInteger_NumSurrogates;
	
//	@Parameter(label = "Box length:", description = "Length of subsequent or gliding box - Shoud be at least three times numLag", style = NumberWidget.SPINNER_STYLE, 
//			   min = "2", max = "9999999999999999999", stepSize = "1",
//			   persist = false, // restore  previous value  default  =  true
//			   initializer = "initialBoxLength", callback = "callbackBoxLength")
//	private int spinnerInteger_BoxLength;
	
	//-----------------------------------------------------------------------------------------------------
//	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
//	private final String labelBackgroundOptions = BACKGROUNDOPTIONS_LABEL;

	@Parameter(label = "Remove zero values", persist = false,
		       callback = "callbackRemoveZeroes")
	private boolean booleanRemoveZeroes;
	
	//-----------------------------------------------------------------------------------------------------
//	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
//	private final String labelDisplayOptions = DISPLAYOPTIONS_LABEL;

//	@Parameter(label = "Delete existing result table",
//			   // persist = false, //restore previous value default = true
//			   initializer = "initialDeleteExistingTable")
//	private boolean booleanDeleteExistingTable;

	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelProcess = PROCESSOPTIONS_LABEL;

	@Parameter(label = "Preview", visibility = ItemVisibility.INVISIBLE, persist = false,
		       callback = "callbackPreview")
	private boolean booleanPreview;
	
	@Parameter(label = "Column #", description = "column number", style = NumberWidget.SPINNER_STYLE, min = "1", max = "1000", stepSize = "1",
			   persist = false, // restore  previous value  default  =  true
			   initializer = "initialNumColumn", callback = "callbackNumColumn")
	private int spinnerInteger_NumColumn;

	@Parameter(label = "Process single column #", callback = "callbackProcessSingleColumn")
	private Button buttonProcessSingleColumn;
	
	@Parameter(label = "Process all columns", callback = "callbackProcessAllColumns")
	private Button buttonProcessAllColumns;


	// ---------------------------------------------------------------------
	// The following initialzer functions set initial values
	

	
	protected void initialNumLag() {
		spinnerInteger_NumLag = 10;
	}
	
	protected void initialSignalType() {
		choiceRadioButt_SignalType = "Entire signal";
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
//	}
	
	protected void initialRemoveZeroes() {
		booleanRemoveZeroes = false;
	}	
	
//	protected void initialDeleteExistingTable() {
//		booleanDeleteExistingTable = true;
//	}
	
	protected void initialNumColumn() {
		spinnerInteger_NumColumn = 1;
	}

	// The following method is known as "callback" which gets executed
	// whenever the value of a specific linked parameter changes.
	
	/** Executed whenever the {@link #spinInteger_NumLag} parameter changes. */
	protected void callbackNumLag() {
		logService.info(this.getClass().getName() + " Lag set to " + spinnerInteger_NumLag);
	}
	
	/** Executed whenever the {@link #choiceRadioButt_SignalType} parameter changes. */
	protected void callbackSignalType() {
		logService.info(this.getClass().getName() + " Signal type set to " + choiceRadioButt_SignalType);
		if (!choiceRadioButt_SignalType.equals("Entire signal")){
			choiceRadioButt_SurrogateType = "No surrogates";
			callbackSurrogateType();
		}
	}
	
	/** Executed whenever the {@link #choiceRadioButt_SurrogateType} parameter changes. */
	protected void callbackSurrogateType() {	
		if (!choiceRadioButt_SignalType.equals("Entire signal")){
			choiceRadioButt_SurrogateType = "No surrogates";
			logService.info(this.getClass().getName() + " Surrogates not allowed for subsequent or gliding boxes!");
		}	
		logService.info(this.getClass().getName() + " Surrogate type set to " + choiceRadioButt_SurrogateType);
	}
	
//	/** Executed whenever the {@link #spinInteger_NumSurrogates} parameter changes. */
//	protected void callbackNumSurrogates() {
//		numSurrogates = spinnerInteger_NumSurrogates;
//		logService.info(this.getClass().getName() + " Number of surrogates set to " + spinnerInteger_NumSurrogates);
//	}
//	
//	/** Executed whenever the {@link #spinInteger_BoxLength} parameter changes. */
//	protected void callbackBoxLength() {
//		numBoxLength = spinnerInteger_BoxLength;
//		logService.info(this.getClass().getName() + " Box length set to " + spinnerInteger_BoxLength);
//	}

	/** Executed whenever the {@link #booleanRemoveZeroes} parameter changes. */
	protected void callbackRemoveZeroes() {
		logService.info(this.getClass().getName() + " Remove zeroes set to " + booleanRemoveZeroes);
	}

	/** Executed whenever the {@link #booleanPreview} parameter changes. */
	protected void callbackPreview() {
		logService.info(this.getClass().getName() + " Preview set to " + booleanPreview);
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
		
		//dlgProgress = new WaitingDialogWithProgressBar("<html>Generating Poincare plot, please wait...<br>Open console window for further info.</html>");
		dlgProgress = new WaitingDialogWithProgressBar("Generating Poincare plot, please wait... Open console window for further info.",
																					logService, false, exec); //isCanceable = false, because no following method listens to exec.shutdown 
		dlgProgress.updatePercent("");
		dlgProgress.setBarIndeterminate(true);
		dlgProgress.setVisible(true);
		
    	exec.execute(new Runnable() {
            public void run() {
        	    try {
        	    	logService.info(this.getClass().getName() + " Processing single signal");
            		getAndValidateActiveDataset();
            		generateTableHeader();
            		deleteExistingDisplays();
            		if (spinnerInteger_NumColumn <= numColumns) processSingleInputColumn(spinnerInteger_NumColumn - 1);
            		dlgProgress.addMessage("Processing finished!");		
            		//collectActiveResultAndShowTable(activeColumnIndex);
            		//showTable();
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
		
		//dlgProgress = new WaitingDialogWithProgressBar("<html>Generating Poincare plot, please wait...<br>Open console window for further info.</html>");
		dlgProgress = new WaitingDialogWithProgressBar("Generating Poincare plot, please wait... Open console window for further info.",
																					logService, true, exec); //isCanceable = true, because processAllInputSignalss(dlgProgress) listens to exec.shutdown 
		dlgProgress.setVisible(true);

		exec.execute(new Runnable() {
            public void run() {	
            	try {
	            	logService.info(this.getClass().getName() + " Processing all available columns");
	        		getAndValidateActiveDataset();
	        		generateTableHeader();
	        		deleteExistingDisplays();
	        		processAllInputColumns();
	        		dlgProgress.addMessage("Processing finished!");
	        		//collectAllResultsAndShowTable();
	        		//showTable();
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
		if (booleanPreview) callbackProcessSingleColumn();
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
		
//		numSubsequentBoxes = (long) Math.floor((double)numRows/(double)spinnerInteger_BoxLength);
//		numGlidingBoxes = numRows - spinnerInteger_BoxLength + 1;
		
//		sliceLabels = new String[(int) numColumns];
		
		numLag = this.spinnerInteger_NumLag;
		// number of lags > numDataPoints is not allowed
		if (numLag >= numRows) {
			numLag = (int)numRows - 1;
		}
	   
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
		
	
	}
	
	
	/**
	 * This method deletes already open displays
	 * 
	 */
	private void deleteExistingDisplays() {
//		boolean optDeleteExistingTable = booleanDeleteExistingTable;
//		
//		if (optDeleteExistingTable) {
//			List<Display<?>> list = defaultDisplayService.getDisplays();
//			for (int i = 0; i < list.size(); i++) {
//				Display<?> display = list.get(i);
//				//System.out.println("display name: " + display.getName());
//				if (display.getName().equals(tableOutName))
//					display.close();
//			}
//		}
	}

	/** 
	 * This method takes the single column and computes results. 
	 * @Param int s
	 * */
	private void processSingleInputColumn (int s) throws InterruptedException {
		
		long startTime = System.currentTimeMillis();
		
		double[] dataY = process(tableIn, s); 
		logService.info(this.getClass().getName() + " Processing finished.");
		
		//int selectedOption = JOptionPane.showConfirmDialog(null, "Do you want to display the Autocorrelation?\nNot recommended for a large number of signals", "Display option", JOptionPane.YES_NO_OPTION); 
		//if (selectedOption == JOptionPane.YES_OPTION) {
			int[] cols = new int[1]; 
			boolean isLineVisible = true;
			String signalTitle = "Poincare plot Signal #" + (s+1) + " " + tableIn.getColumnHeader(s);
			String xLabel = "x";
			String yLabel = "x+"+this.numLag;
			String[] seriesLabels = new String[1];			
			for (int c = 0; c < cols.length; c++) {
				cols[c] = c;
				seriesLabels[c] = tableIn.getColumnHeader(s);					
			}
			PlotDisplayFrame pdf = new PlotDisplayFrame(signal1D, dataY, isLineVisible, "Poincare plot", signalTitle, xLabel, yLabel, seriesLabels[0]);
			pdf.setVisible(true);
		//}
		
		long duration = System.currentTimeMillis() - startTime;
		TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
		SimpleDateFormat sdf = new SimpleDateFormat();
		sdf.applyPattern("HHH:mm:ss:SSS");
		logService.info(this.getClass().getName() + " Elapsed time: "+ sdf.format(duration));
	}


	/**
	 * This method loops over all input columns and computes results. 
	 * 
	 * */
	private void processAllInputColumns() throws InterruptedException{
		
		long startTimeAll = System.currentTimeMillis();
		double dataY[][] = new double[(int) numColumns][(int) numRows];
		
		// loop over all slices of stack
		for (int s = 0; s < numColumns; s++) { // s... number of signal column
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
				
				for (int i = 0; i < resultValues.length; i++) {
					dataY[s][i] = resultValues[i]; 
				}
				
				logService.info(this.getClass().getName() + " Processing finished.");
	
				long duration = System.currentTimeMillis() - startTime;
				TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
				SimpleDateFormat sdf = new SimpleDateFormat();
				sdf.applyPattern("HHH:mm:ss:SSS");
				logService.info(this.getClass().getName() + " Elapsed time: "+ sdf.format(duration));
			}
		} //s

		//int selectedOption = JOptionPane.showConfirmDialog(null, "Do you want to display the Autocorrelation?\nNot recommended for a large number of signals", "Display option", JOptionPane.YES_NO_OPTION); 
		//if (selectedOption == JOptionPane.YES_OPTION) {
			int[] cols = new int[(int) numColumns]; 
			boolean isLineVisible = true;
			String signalTitle = "Poincare plots";
			String xLabel = "x";
			String yLabel = "x+"+this.numLag;
			String[] seriesLabels = new String[(int) numColumns];			
			for (int c = 0; c < numColumns; c++) {
				cols[c] = c;
				seriesLabels[c] = tableIn.getColumnHeader(c);					
			}
			PlotDisplayFrame pdf = new PlotDisplayFrame(signal1D, dataY, isLineVisible, "Poincare plots", signalTitle, xLabel, yLabel, seriesLabels);
			pdf.setVisible(true);
		//}
		
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
	 * @param int column number of single signal.
	 * @param double[] result values
	 */
	private void writeToTable(int rowNumber, double[] resultValues) {
		logService.info(this.getClass().getName() + " Writing to the table...");
		

	}

	/**
	 * shows the result table
	 */
	private void showTable() {
		// Show table
		//uiService.show(tableOutName, tableResult);
	}
	
	/**
	*
	* Processing
	*/
	private double[] process(DefaultGenericTable dgt, int col) { //  c column number
	
		String  signalType    = choiceRadioButt_SignalType;
		String  surrType      = choiceRadioButt_SurrogateType;
		//int     boxLength     = spinnerInteger_BoxLength;
		int     numDataPoints = dgt.getRowCount();
		boolean removeZeores  = booleanRemoveZeroes;
		int numLag            = this.numLag; 
		
		//******************************************************************************************************
		//xAxis1D  = new double[numDataPoints];
		signal1D = new double[numDataPoints];
		
		signalColumn = dgt.get(col);
		for (int n = 0; n < numDataPoints; n++) {
			//xAxis1D[n]  = n+1;
			signal1D[n] = Double.valueOf((Double)signalColumn.get(n));
		}	
		
		signal1D = removeNaN(signal1D);
		if (removeZeores) signal1D = removeZeroes(signal1D);
		
		//numDataPoints may be smaller now
		numDataPoints = signal1D.length;
		
		// number of max lag >= numDataPoints is not allowed
		if (numLag >= numDataPoints) {
			numLag = numDataPoints;
		}
		double[] resultValues = new double[numDataPoints];
		//double[] rangeAuCorr  = new double[numLag];
		for (int i = 0; i < numDataPoints; i++) {
			resultValues[i]   = Double.NaN;
		}
			
		//int numActualRows = 0;
		logService.info(this.getClass().getName() + " Column #: "+ (col+1) + "  " + signalColumn.getHeader() + "  Size of signal = " + numDataPoints);	
			
		//"Entire signal", "Subsequent boxes", "Gliding box" 
		//********************************************************************************************************
		if (signalType.equals("Entire signal")){	//only this option is possible for autocorrelation
			
			if (!surrType.equals("No surrogates")) {
				Surrogate surrogate = new Surrogate();	
				//choices = {"No surrogates", "Shuffle", "Gaussian", "Random phase", "AAFT"}, 
				if (surrType.equals("Shuffle"))      signal1D = surrogate.calcSurrogateShuffle(signal1D);
				if (surrType.equals("Gaussian"))     signal1D = surrogate.calcSurrogateGaussian(signal1D);
				if (surrType.equals("Random phase")) signal1D = surrogate.calcSurrogateRandomPhase(signal1D);
				if (surrType.equals("AAFT"))         signal1D = surrogate.calcSurrogateAAFT(signal1D);
			}
			
			//Generate Poincare plot
			for (int i = 0; i < (numDataPoints - numLag); i++) {
				resultValues[i] = signal1D[i + numLag];
			}
			
			
		//********************************************************************************************************	
		} else if (signalType.equals("Subsequent boxes")){ //not for Autocorrelation
		
		//********************************************************************************************************			
		} else if (signalType.equals("Gliding box")){ //not for autocorrelation
		
		}
		
		return resultValues;
		// Output
		// uiService.show(tableName, table);
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
		ij.command().run(SignalPoincarePlot.class, true);
	}
}
