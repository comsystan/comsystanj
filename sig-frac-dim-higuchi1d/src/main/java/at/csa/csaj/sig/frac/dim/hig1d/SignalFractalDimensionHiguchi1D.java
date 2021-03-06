/*-
 * #%L
 * Project: ImageJ signal plugin for computing fractal dimension with 1D Higuchi algorithm.
 * File: SignalFractalDimensionHiguchi1D.java
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
package at.csa.csaj.sig.frac.dim.hig1d;

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
import org.scijava.command.InteractiveCommand;
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
import at.csa.csaj.sig.frac.dim.hig1d.util.Higuchi;
import at.csa.csaj.commons.plot.RegressionPlotFrame;
import at.csa.csaj.commons.signal.algorithms.Surrogate;
import at.csa.csaj.commons.dialog.WaitingDialogWithProgressBar;
import at.csa.csaj.sig.open.SignalOpener;

/**
 * A {@link Command} plugin computing <the Higuchi dimension</a>
 * of a  signal.
 */
@Plugin(type = InteractiveCommand.class, headless = true, menuPath = "Plugins>ComsystanJ>Signal>Fractal Dimension Higuchi")
public class SignalFractalDimensionHiguchi1D<T extends RealType<T>> extends InteractiveCommand implements Command, Previewable { // non blocking  GUI
//public class SignalFractalDimensionHiguchi1D<T extends RealType<T>> implements Command {	//modal GUI

	private static final String PLUGIN_LABEL            = "<html><b>Genuin Higuchi 1D algorithm</b></html>";
	private static final String SPACE_LABEL             = "";
	private static final String REGRESSION_LABEL        = "<html><b>Fractal regression parameters</b></html>";
	private static final String SIGNALMETHOD_LABEL      = "<html><b>Signal evaluation</b></html>";
	private static final String BACKGROUNDOPTIONS_LABEL = "<html><b>Background Option</b></html>";
	private static final String DISPLAYOPTIONS_LABEL    = "<html><b>Display options</b></html>";
	private static final String PROCESSOPTIONS_LABEL    = "<html><b>Process options</b></html>";
	
	private static double[] signal1D;
	private static double[] xAxis1D;
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
	
	private static final int  numKMax = 1000;
	
	private static ArrayList<RegressionPlotFrame> doubleLogPlotList = new ArrayList<RegressionPlotFrame>();
	
	private static final String tableOutName = "Table - Higuchi dimension";
	
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
	private final String labelRegression = REGRESSION_LABEL;

	@Parameter(label = "k:", description = "Maximal delay between data points", style = NumberWidget.SPINNER_STYLE, min = "3", max = "9999999999999999999", stepSize = "1",
			   persist = false, // restore  previous value  default  =  true
			   initializer = "initialKMax", callback = "callbackKMax")
	private int spinnerInteger_KMax;

	@Parameter(label = "Regression Min:", description = "Minimum x value of linear regression", style = NumberWidget.SPINNER_STYLE, min = "1", max = "9999999999999999999", stepSize = "1",
			   persist = false, //restore previous value default = true
			   initializer = "initialRegMin", callback = "callbackRegMin")
	private int spinnerInteger_RegMin = 1;

	@Parameter(label = "Regression Max:", description = "Maximum x value of linear regression", style = NumberWidget.SPINNER_STYLE, min = "3", max = "9999999999999999999", stepSize = "1",
			   persist = false, //restore previous value default = true
			   initializer = "initialRegMax", callback = "callbackRegMax")
	private int spinnerInteger_RegMax = 3;
	
	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelSignalType = SIGNALMETHOD_LABEL;

	@Parameter(label = "Signal type",
		description = "Entire signal, Subsequent boxes or Gliding box",
		style = ChoiceWidget.LIST_BOX_STYLE,
		choices = {"Entire signal", "Subsequent boxes", "Gliding box"}, 
		//persist  = false,  //restore previous value default = true
		initializer = "initialSignalType",
		callback = "callbackSignalType")
	private String choiceRadioButt_SignalType;
	
	@Parameter(label = "Entire signal - Surrogates",
			description = "Surrogates types - Only for Entire signal type!",
			style = ChoiceWidget.LIST_BOX_STYLE,
			choices = {"No surrogates", "Shuffle", "Gaussian", "Random phase", "AAFT"}, 
			persist  = false,  //restore previous value default = true
			initializer = "initialSurrogateType",
			callback = "callbackSurrogateType")
		private String choiceRadioButt_SurrogateType;
	
	@Parameter(label = "# Surrogates:", description = "Number of computed surrogates", style = NumberWidget.SPINNER_STYLE, 
			   min = "1", max = "9999999999999999999", stepSize = "1",
			   persist = false, // restore  previous value  default  =  true
			   initializer = "initialNumSurrogates", callback = "callbackNumSurrogates")
	private int spinnerInteger_NumSurrogates;
	
	@Parameter(label = "Box length:", description = "Length of subsequent or gliding box - Shoud be at least three times kMax", style = NumberWidget.SPINNER_STYLE, 
			   min = "2", max = "9999999999999999999", stepSize = "1",
			   persist = false, // restore  previous value  default  =  true
			   initializer = "initialBoxLength", callback = "callbackBoxLength")
	private int spinnerInteger_BoxLength;
	
	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelBackgroundOptions = BACKGROUNDOPTIONS_LABEL;

	@Parameter(label = "Remove zero values", persist = false,
		       callback = "callbackRemoveZeroes")
	private boolean booleanRemoveZeroes;
	
	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelDisplayOptions = DISPLAYOPTIONS_LABEL;

	@Parameter(label = "Show double log plot",
		   	   // persist = false, //restore previous value default = true
			   initializer = "initialShowDoubleLogPlots")
	private boolean booleanShowDoubleLogPlot;

	@Parameter(label = "Delete existing double log plot",
			   // persist = false, //restore previous value default = true
			   initializer = "initialDeleteExistingDoubleLogPlots")
	private boolean booleanDeleteExistingDoubleLogPlot;

	@Parameter(label = "Delete existing result table",
			   // persist = false, //restore previous value default = true
			   initializer = "initialDeleteExistingTable")
	private boolean booleanDeleteExistingTable;

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

	protected void initialKMax() {
		//numbKMax = (int) Math.floor(tableIn.getRowCount() / 3.0);
		spinnerInteger_KMax = 8;
	}
	protected void initialRegMin() {
		spinnerInteger_RegMin = 1;
	}
	protected void initialRegMax() {
		//numbKMax = (int) Math.floor(tableIn.getRowCount() / 3.0);
		spinnerInteger_RegMax = 8;
	}
	protected void initialSignalType() {
		choiceRadioButt_SignalType = "Entire signal";
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
	}
	protected void initialRemoveZeroes() {
		booleanRemoveZeroes = false;
	}	
	protected void initialShowDoubleLogPlots() {
		booleanShowDoubleLogPlot = true;
	}
	protected void initialDeleteExistingDoubleLogPlots() {
		booleanDeleteExistingDoubleLogPlot = true;
	}
	protected void initialDeleteExistingTable() {
		booleanDeleteExistingTable = true;
	}

	// The following method is known as "callback" which gets executed
	// whenever the value of a specific linked parameter changes.
	
	/** Executed whenever the {@link #spinInteger_KMax} parameter changes. */
	protected void callbackKMax() {

		if (spinnerInteger_KMax < 3) {
			spinnerInteger_KMax = 3;
		}
		if (spinnerInteger_KMax > numKMax) {
			spinnerInteger_KMax = numKMax;
		}
		if (spinnerInteger_RegMax > spinnerInteger_KMax) {
			spinnerInteger_RegMax = spinnerInteger_KMax;
		}
		if (spinnerInteger_RegMin >= spinnerInteger_RegMax - 2) {
			spinnerInteger_RegMin = spinnerInteger_RegMax - 2;
		}
		logService.info(this.getClass().getName() + " k set to " + spinnerInteger_KMax);
	}

	/** Executed whenever the {@link #spinInteger_RegMin} parameter changes. */
	protected void callbackRegMin() {
		if (spinnerInteger_RegMin >= spinnerInteger_RegMax - 2) {
			spinnerInteger_RegMin = spinnerInteger_RegMax - 2;
		}
		if (spinnerInteger_RegMin < 1) {
			spinnerInteger_RegMin = 1;
		}
		logService.info(this.getClass().getName() + " Regression Min set to " + spinnerInteger_RegMin);
	}

	/** Executed whenever the {@link #spinInteger_RegMax} parameter changes. */
	protected void callbackRegMax() {
		if (spinnerInteger_RegMax <= spinnerInteger_RegMin + 2) {
			spinnerInteger_RegMax = spinnerInteger_RegMin + 2;
		}
		if (spinnerInteger_RegMax > spinnerInteger_KMax) {
			spinnerInteger_RegMax = spinnerInteger_KMax;
		}

		logService.info(this.getClass().getName() + " Regression Max set  to " + spinnerInteger_RegMax);
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
		
		//dlgProgress = new WaitingDialogWithProgressBar("<html>Computing Huguchi1D dimensions, please wait...<br>Open console window for further info.</html>");
		dlgProgress = new WaitingDialogWithProgressBar("Computing Huguchi1D dimensions, please wait... Open console window for further info.",
																					logService, false, exec); //isCanceable = false, because no following method listens to exec.shutdown 
		dlgProgress.updatePercent("");
		dlgProgress.setBarIndeterminate(true);
		dlgProgress.setVisible(true);
		
    	exec.execute(new Runnable() {
            public void run() {
        	    try {
        	    	logService.info(this.getClass().getName() + " Processing active signal");
            		getAndValidateActiveDataset();
            		generateTableHeader();
            		deleteExistingDisplays();
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
		
		//dlgProgress = new WaitingDialogWithProgressBar("<html>Computing Huguchi1D dimensions, please wait...<br>Open console window for further info.</html>");
		dlgProgress = new WaitingDialogWithProgressBar("Computing Huguchi1D dimensions, please wait... Open console window for further info.",
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
		tableResult.add(new GenericColumn("Signal type"));
		tableResult.add(new GenericColumn("Surrogate type"));
		tableResult.add(new IntColumn("# Surrogates"));
		tableResult.add(new IntColumn("Box length"));
		tableResult.add(new BoolColumn("Zeroes removed"));
		
		tableResult.add(new IntColumn("k"));
		tableResult.add(new IntColumn("Reg Min"));
		tableResult.add(new IntColumn("Reg Max"));
	
		//"Entire signal", "Subsequent boxes", "Gliding box" 
		if (choiceRadioButt_SignalType.equals("Entire signal")){
			tableResult.add(new DoubleColumn("Dh"));	
			tableResult.add(new DoubleColumn("R2"));
			tableResult.add(new DoubleColumn("StdErr"));
			
			if (choiceRadioButt_SurrogateType.equals("No surrogates")) {
				//do nothing	
			} else { //Surrogates
				tableResult.add(new DoubleColumn("Dh_Surr")); //Mean surrogate value	
				tableResult.add(new DoubleColumn("R2_Surr")); //Mean surrogate value
				tableResult.add(new DoubleColumn("StdErr_Surr")); //Mean surrogate value
				for (int s = 0; s < numSurrogates; s++) tableResult.add(new DoubleColumn("Dh_Surr-#"+(s+1))); 
				for (int s = 0; s < numSurrogates; s++) tableResult.add(new DoubleColumn("R2_Surr-#"+(s+1))); 
				for (int s = 0; s < numSurrogates; s++) tableResult.add(new DoubleColumn("StdErr_Surr-#"+(s+1))); 
			}	
		} 
		else if (choiceRadioButt_SignalType.equals("Subsequent boxes")){
			for (int n = 1; n <= numSubsequentBoxes; n++) {
				tableResult.add(new DoubleColumn("Dh-#" + n));	
			}
			for (int n = 1; n <= numSubsequentBoxes; n++) {
				tableResult.add(new DoubleColumn("R2-#" + n));	
			}
		}
		else if (choiceRadioButt_SignalType.equals("Gliding box")){
			for (int n = 1; n <= numGlidingBoxes; n++) {
				tableResult.add(new DoubleColumn("Dh-#" + n));	
			}
			for (int n = 1; n <= numGlidingBoxes; n++) {
				tableResult.add(new DoubleColumn("R2-#" + n));	
			}
		}	
	}
	
	/**
	 * This method deletes already open displays
	 * 
	 */
	private void deleteExistingDisplays() {
		boolean optDeleteExistingPlot  = booleanDeleteExistingDoubleLogPlot;
		boolean optDeleteExistingTable = booleanDeleteExistingTable;

		if (optDeleteExistingPlot) {
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
		if (optDeleteExistingTable) {
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
	private void processSingleInputColumn (int c) throws InterruptedException {
		
		long startTime = System.currentTimeMillis();
		
		// Compute result values
		double[] resultValues = process(tableIn, c); 
		// 0 Dh, 1 R2, 2 StdErr
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
		for (int s = 0; s < numColumns; s++) { // s... numb er of signal column
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
				// 0 Dh, 1 R2, 2 StdErr
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
		
		// 0 Dh, 1 R2, 2 StdErr
		// fill table with values
		tableResult.appendRow();
		tableResult.set(0, row, tableInName);//File Name
		if (sliceLabels != null)  tableResult.set(1, row, tableIn.getColumnHeader(signalNumber)); //Column Name
		tableResult.set(2, row, choiceRadioButt_SignalType); //Signal Method
		tableResult.set(3, row, choiceRadioButt_SurrogateType); //Surrogate Method
		if (choiceRadioButt_SignalType.equals("Entire signal") && (!choiceRadioButt_SurrogateType.equals("No surrogates"))) {
			tableResult.set(4, row, spinnerInteger_NumSurrogates); //# Surrogates
		} else {
			tableResult.set(4, row, null); //# Surrogates
		}
		if (!choiceRadioButt_SignalType.equals("Entire signal")){
			tableResult.set(5, row, spinnerInteger_BoxLength); //Box Length
		} else {
			tableResult.set(5, row, null);
		}	
		tableResult.set(6, row, booleanRemoveZeroes); //Zeroes removed
		
		tableResult.set(7, row, spinnerInteger_KMax); // KMax
		tableResult.set(8, row, spinnerInteger_RegMin); //RegMin
		tableResult.set(9, row, spinnerInteger_RegMax); //RegMax	
		tableColLast = 9;
		
		//"Entire signal", "Subsequent boxes", "Gliding box" 
		if (choiceRadioButt_SignalType.equals("Entire signal")){
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
		else if (choiceRadioButt_SignalType.equals("Subsequent boxes")){
			//Dh R2 StdErr
			tableColStart = tableColLast +1;
			tableColEnd = (int) (tableColStart + 2 * numSubsequentBoxes); // 2 parameters
			for (int c = tableColStart; c < tableColEnd; c++ ) {
				tableResult.set(c, row, resultValues[c-tableColStart]);
			}	
		}
		else if (choiceRadioButt_SignalType.equals("Gliding box")){
			//Dh R2 StdErr
			tableColStart = tableColLast +1;
			tableColEnd = (int) (tableColStart + 2 * numGlidingBoxes); // 2 parameters 
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
		uiService.show(tableOutName, tableResult);
	}
	
	/**
	*
	* Processing
	*/
	private double[] process(DefaultGenericTable dgt, int col) { //  c column number
	

		String signalType     = choiceRadioButt_SignalType;
		String surrType       = choiceRadioButt_SurrogateType;
		int boxLength         = spinnerInteger_BoxLength;
		int numDataPoints     = dgt.getRowCount();
		int numKMax           = spinnerInteger_KMax;
		int regMin            = spinnerInteger_RegMin;
		int regMax            = spinnerInteger_RegMax;
		boolean removeZeores  = booleanRemoveZeroes;
	
		boolean optShowPlot   = booleanShowDoubleLogPlot;
		
		double[] resultValues = new double[3]; // Dim, R2, StdErr
		for (int r = 0; r<resultValues.length; r++) resultValues[r] = Double.NaN;
		
//		double[]totals = new double[numKMax];
//		double[]eps = new double[numKMax];
//		// definition of eps
//		for (int kk = 0; kk < numKMax; kk++) {
//			eps[kk] = kk + 1;		
//			//logService.info(this.getClass().getName() + " k=" + kk + " eps= " + eps[kk][b]);
//		}
		//******************************************************************************************************
		xAxis1D  = new double[numDataPoints];
		signal1D = new double[numDataPoints];
		
		signalColumn = dgt.get(col);
		for (int n = 0; n < numDataPoints; n++) {
			xAxis1D[n]  = n+1;
			signal1D[n] = Double.valueOf((Double)signalColumn.get(n));
		}	
		
		signal1D = removeNaN(signal1D);
		if (removeZeores) signal1D = removeZeroes(signal1D);
		Higuchi hig;
		double[] L;
		double[] regressionValues = null;
		
		
		//"Entire signal", "Subsequent boxes", "Gliding box" 
		//********************************************************************************************************
		if (signalType.equals("Entire signal")){	
			if (surrType.equals("No surrogates")) {
				resultValues = new double[3]; // Dim, R2, StdErr	
			} else {
				resultValues = new double[3+3+3*numSurrogates]; // Dim_Surr, R2_Surr, StdErr_Surr,	Dim_Surr1, Dim_Surr2,  Dim_Surr3, ......R2_Surr1,.... 2, 3......
			}
			for (int r = 0; r < resultValues.length; r++) resultValues[r] = Double.NaN;
			logService.info(this.getClass().getName() + " Column #: "+ (col+1) + "  " + signalColumn.getHeader() + "  Size of signal = " + signal1D.length);	
			if (signal1D.length > (numKMax * 2)) { // only data series which are large enough
				hig = new Higuchi();
				L = hig.calcLengths(signal1D, numKMax);
				regressionValues = hig.calcDimension(L, regMin, regMax);
				// 0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
				
				if (optShowPlot) {
					String preName = signalColumn.getHeader();
					showPlot(hig.getLnDataX(), hig.getLnDataY(), preName, col, regMin, regMax);
				}	
				resultValues[0] = -regressionValues[1]; // Dh = -slope
				resultValues[1] = regressionValues[4]; //R2
				resultValues[2] = regressionValues[3]; //StdErr
				int lastMainResultsIndex = 2;
				
				if (!surrType.equals("No surrogates")) { //Add surrogate analysis
					surrSignal1D = new double[signal1D.length];
					
					double sumDims   = 0.0;
					double sumR2s    = 0.0;
					double sumStdErr = 0.0;
					Surrogate surrogate = new Surrogate();
					for (int s = 0; s < numSurrogates; s++) {
						//choices = {"No surrogates", "Shuffle", "Gaussian", "Random phase", "AAFT"}, 
						if (surrType.equals("Shuffle"))      surrSignal1D = surrogate.calcSurrogateShuffle(signal1D);
						if (surrType.equals("Gaussian"))     surrSignal1D = surrogate.calcSurrogateGaussian(signal1D);
						if (surrType.equals("Random phase")) surrSignal1D = surrogate.calcSurrogateRandomPhase(signal1D);
						if (surrType.equals("AAFT"))         surrSignal1D = surrogate.calcSurrogateAAFT(signal1D);
				
						hig = new Higuchi();
						L = hig.calcLengths(surrSignal1D, numKMax);
						regressionValues = hig.calcDimension(L, regMin, regMax);
						// 0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
						resultValues[lastMainResultsIndex + 4 + s]                    = -regressionValues[1];
						resultValues[lastMainResultsIndex + 4 + numSurrogates + s]    = regressionValues[4];
						resultValues[lastMainResultsIndex + 4 + (2*numSurrogates) +s] = regressionValues[3];
						sumDims    += -regressionValues[1];
						sumR2s     +=  regressionValues[4];
						sumStdErr  +=  regressionValues[3];
					}
					resultValues[lastMainResultsIndex + 1] = sumDims/numSurrogates;
					resultValues[lastMainResultsIndex + 2] = sumR2s/numSurrogates;
					resultValues[lastMainResultsIndex + 3] = sumStdErr/numSurrogates;
				}	
			} 
		//********************************************************************************************************	
		} else if (signalType.equals("Subsequent boxes")){
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
				if (subSignal1D.length > (numKMax * 2)) { // only data series which are large enough
					hig = new Higuchi();
					L = hig.calcLengths(subSignal1D, numKMax);
					regressionValues = hig.calcDimension(L, regMin, regMax);
					// 0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
					
					//if (optShowPlot){ //show all plots
					if ((optShowPlot) && (i==0)){ //show only first plot
						String preName = signalColumn.getHeader() + "-Box#" + (i+1);
						showPlot(hig.getLnDataX(), hig.getLnDataY(), preName, col, regMin, regMax);
					}
					resultValues[i]                             = -regressionValues[1]; // Dh = -slope;
					resultValues[(int)(i + numSubsequentBoxes)] = regressionValues[4];  //R2		
				} 
				//***********************************************************************
			}	
		//********************************************************************************************************			
		} else if (signalType.equals("Gliding box")){
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
				if (subSignal1D.length > (numKMax * 2)) { // only data series which are large enough
					hig = new Higuchi();
					L = hig.calcLengths(subSignal1D, numKMax);
					regressionValues = hig.calcDimension(L, regMin, regMax);
					// 0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
					
					//if (optShowPlot){ //show all plots
					if ((optShowPlot) && (i==0)){ //show only first plot
						String preName = signalColumn.getHeader() + "-Box #" + (i+1);
						showPlot(hig.getLnDataX(), hig.getLnDataY(), preName, col, regMin, regMax);
					}	
					resultValues[i]                          = -regressionValues[1]; // Dh = -slope;
					resultValues[(int)(i + numGlidingBoxes)] = regressionValues[4];  //R2		
				}
				//***********************************************************************
			}
		}
		
		return resultValues;
		// Dim, R2, StdErr
		// Output
		// uiService.show(tableName, table);
	}

	// This method shows the double log plot
	private void showPlot(double[] lnDataX, double[] lnDataY, String preName, int col, int regMin, int regMax) {
		// String preName = "";
		if (preName == null) {
			preName += "Col" + String.format("%03d", col) + "-";
		}
		boolean isLineVisible = false; // ?
		RegressionPlotFrame doubleLogPlot = DisplayRegressionPlotXY(lnDataX, lnDataY, isLineVisible,
				"Double Log Plot - Higuchi Dimension", preName + "-" + tableInName, "ln(k)", "ln(L)", "", regMin, regMax);
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
		ij.command().run(SignalFractalDimensionHiguchi1D.class, true);
	}
}
