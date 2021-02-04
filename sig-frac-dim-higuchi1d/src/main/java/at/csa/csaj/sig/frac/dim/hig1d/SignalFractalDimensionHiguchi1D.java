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
import java.util.Vector;
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
import org.scijava.widget.NumberWidget;
import at.csa.csaj.sig.frac.dim.hig1d.util.Higuchi;
import at.csa.csaj.commons.plot.RegressionPlotFrame;
import at.csa.csaj.commons.dialog.WaitingDialogWithProgressBar;
import at.csa.csaj.sig.open.SignalOpener;

/**
 * A {@link Command} plugin computing <the Higuchi dimension</a>
 * of a  signal.
 */
@Plugin(type = InteractiveCommand.class, headless = true, menuPath = "Plugins>ComsystanJ>Signal-Fractal>Fractal Dimension Higuchi")
public class SignalFractalDimensionHiguchi1D<T extends RealType<T>> extends InteractiveCommand implements Command, Previewable { // non blocking  GUI
//public class SignalFractalDimensionHiguchi1D<T extends RealType<T>> implements Command {	//modal GUI

	private static final String PLUGIN_LABEL = "Computes fractal dimension with the Higuchi 1D algorithm";
	private static final String SPACE_LABEL = "";
	private static final String REGRESSION_LABEL = "---------------------------- Regression parameters ----------------------------";
	private static final String METHOD_LABEL = "------------------------------------------------------------------------------------";
	private static final String OPTIONS_LABEL = "------------------------------------- Options -------------------------------------";
	private static final String PROCESS_LABEL = "------------------------------------- Process -------------------------------------";

	private static DefaultGenericTable  tableIn;
	
	private static double[] signal1D;
	private static double[] xAxis1D;

	private static String tableInName;
	private static String[] sliceLabels;
	private static long numColumns = 0;
	private static long numRows = 0;
	private static long numDimensions = 0;
	private static int numbKMax = 0;
	private static ArrayList<RegressionPlotFrame> doubleLogPlotList = new ArrayList<RegressionPlotFrame>();

	private static double[][] resultValuesTable; // first column is the image index, second column are the corresponding regression values
	private static final String tableName = "Table - Higuchi dimension";
	
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
	
	@Parameter(type = ItemIO.INPUT)
	private DefaultTableDisplay  defaultTableDisplay;

	@Parameter(type = ItemIO.OUTPUT)
	private DefaultGenericTable tableResult;


	// Widget elements------------------------------------------------------

	@Parameter(visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelPlugin = PLUGIN_LABEL;

	@Parameter(visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelSpace = SPACE_LABEL;

	@Parameter(visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelRegression = REGRESSION_LABEL;

	@Parameter(label = "k:", description = "maximal delay between data points", style = NumberWidget.SPINNER_STYLE, min = "3", max = "9999999999999999999999999", stepSize = "1",
			   persist = false, // restore  previous value  default  =  true
			   initializer = "initialKMax", callback = "callbackKMax")
	private int spinnerInteger_KMax;

	@Parameter(label = "Regression Min:", description = "minimum x value of linear regression", style = NumberWidget.SPINNER_STYLE, min = "1", max = "9999999999999999999999999", stepSize = "1",
			   persist = false, //restore previous value default = true
			   initializer = "initialRegMin", callback = "callbackRegMin")
	private int spinnerInteger_RegMin = 1;

	@Parameter(label = "Regression Max:", description = "maximum x value of linear regression", style = NumberWidget.SPINNER_STYLE, min = "3", max = "9999999999999999999999999", stepSize = "1",
			   persist = false, //restore previous value default = true
			   initializer = "initialRegMax", callback = "callbackRegMax")
	private int spinnerInteger_RegMax = 3;

	@Parameter(visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelInterpolation = METHOD_LABEL;

	
	@Parameter(label = "Remove zero values", persist = false,
		       callback = "callbackRemoveZeroes")
	private boolean booleanRemoveZeroes;
	
	@Parameter(visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelOptions = OPTIONS_LABEL;

	@Parameter(label = "Show double log plot",
		   	   // persist = false, //restore previous value default = true
			   initializer = "initialShowDoubleLogPlots")
	private boolean booleanShowDoubleLogPlot;
	
	@Parameter(label = "Show some radial line plots",
		   	   // persist = false, //restore previous value default = true
			   initializer = "initialShowSomeRadialLinePlots")
	private boolean booleanShowSomeRadialLinePlots;

	@Parameter(label = "Delete existing double log plot",
			   // persist = false, //restore previous value default = true
			   initializer = "initialDeleteExistingDoubleLogPlots")
	private boolean booleanDeleteExistingDoubleLogPlot;

	@Parameter(label = "Delete existing result table",
			   // persist = false, //restore previous value default = true
			   initializer = "initialDeleteExistingTable")
	private boolean booleanDeleteExistingTable;

	@Parameter(label = "Get Dh value of each radial line",
			   // persist = false, //restore previous value default = true
			   initializer = "initialGetRadialDhValues")
	private boolean booleanGetRadialDhValues;

	
	@Parameter(visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelProcess = PROCESS_LABEL;

	@Parameter(label = "Preview", visibility = ItemVisibility.INVISIBLE, persist = false,
		       callback = "callbackPreview")
	private boolean booleanPreview;
	
	@Parameter(label = "Process first column", callback = "callbackProcessActiveColumn")
	private Button buttonProcessActiveColumn;

	@Parameter(label = "Process all available columns", callback = "callbackProcessAllColumns")
	private Button buttonProcessAllColumns;


	// ---------------------------------------------------------------------

	
	
	// The following initialzer functions set initial values

	protected void initialKMax() {
		//numbKMax = (int) Math.floor(tableIn.getRowCount() / 3.0);
		numbKMax = 3;
		spinnerInteger_KMax = numbKMax;
	}

	protected void initialRegMin() {
		spinnerInteger_RegMin = 1;
	}

	protected void initialRegMax() {
		//numbKMax = (int) Math.floor(tableIn.getRowCount() / 3.0);
		numbKMax = 3;
		spinnerInteger_RegMax = numbKMax;
	}

	protected void initialShowDoubleLogPlots() {
		booleanShowDoubleLogPlot = true;
	}

	protected void initialShowSomeRadialLinePlotss() {
		booleanShowSomeRadialLinePlots = false;
	}
	
	protected void initialDeleteExistingDoubleLogPlots() {
		booleanDeleteExistingDoubleLogPlot = true;
	}

	protected void initialDeleteExistingTable() {
		booleanDeleteExistingTable = true;
	}
	
	protected void initialGetRadialDhValues() {
		booleanGetRadialDhValues = false;
	}

	// The following method is known as "callback" which gets executed
	// whenever the value of a specific linked parameter changes.

	/** Executed whenever the {@link #spinInteger_KMax} parameter changes. */
	protected void callbackKMax() {

		if (spinnerInteger_KMax < 3) {
			spinnerInteger_KMax = 3;
		}
		if (spinnerInteger_KMax > numbKMax) {
			spinnerInteger_KMax = numbKMax;
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

	/** Executed whenever the {@link #booleanRemoveZeroes} parameter changes. */
	protected void callbackRemoveZeroes() {
		logService.info(this.getClass().getName() + " Remove zeroes set to " + booleanRemoveZeroes);
	}

	/** Executed whenever the {@link #booleanPreview} parameter changes. */
	protected void callbackPreview() {
		logService.info(this.getClass().getName() + " Preview set to " + booleanPreview);
	}
	
	/**
	 * Executed whenever the {@link #buttonProcessActiveImage} button is pressed.
	 */
	protected void callbackProcessActiveColumn() {
		//prepare  executer service
		exec = Executors.newSingleThreadExecutor();
		
		//WaitingDialogWithProgressBar dlgProgress = new WaitingDialogWithProgressBar("<html>Computing Huguchi1D dimensions, please wait...<br>Open console window for further info.</html>");
		WaitingDialogWithProgressBar dlgProgress = new WaitingDialogWithProgressBar("Computing Huguchi1D dimensions, please wait... Open console window for further info.",
																					logService, false, exec); //isCanceable = false, because no following method listens to exec.shutdown 
		dlgProgress.updatePercent("");
		dlgProgress.setBarIndeterminate(true);
		dlgProgress.setVisible(true);
		
    	exec.execute(new Runnable() {
            public void run() {
        	    try {
        	    	logService.info(this.getClass().getName() + " Processing active signal");
            		getAndValidateActiveDataset();
            		deleteExistingDisplays();
            		int activeColumnIndex = getActiveColumnIndex();
            		processActiveInputColumn(activeColumnIndex, dlgProgress);
            		dlgProgress.addMessage("Processing finished! Collecting data for table...");
            		generateTableHeader();
            		collectActiveResultAndShowTable(activeColumnIndex);
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
	 * Executed whenever the {@link #buttonProcessAllImages} button is pressed. This
	 * is the main processing method usually implemented in the run() method for
	 */
	protected void callbackProcessAllColumns() {
		//prepare  executer service
		exec = Executors.newSingleThreadExecutor();
		//exec =  defaultThreadService.getExecutorService();
		
		//WaitingDialogWithProgressBar dlgProgress = new WaitingDialogWithProgressBar("<html>Computing Huguchi1D dimensions, please wait...<br>Open console window for further info.</html>");
		WaitingDialogWithProgressBar dlgProgress = new WaitingDialogWithProgressBar("Computing Huguchi1D dimensions, please wait... Open console window for further info.",
																					logService, true, exec); //isCanceable = true, because processAllInputImages(dlgProgress) listens to exec.shutdown 
		dlgProgress.setVisible(true);

		exec.execute(new Runnable() {
            public void run() {	
            	try {
	            	logService.info(this.getClass().getName() + " Processing all available columns");
	        		getAndValidateActiveDataset();
	        		deleteExistingDisplays();
	        		processAllInputColumns(dlgProgress);
	        		dlgProgress.addMessage("Processing finished! Collecting data for table...");
	        		generateTableHeader();
	        		collectAllResultsAndShowTable();
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
		if (booleanPreview) callbackProcessActiveColumn();
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

		//TO DO
		//tableIn = imageDisplayService.getActiveDataset();

//		List<Display<?>> displays = defaultDisplayService.getDisplays();
//		for (int d = 0; d < displays.size(); d++) {
//			String displayName = displays.get(d).getName();
//			logService.info(this.getClass().getName() + " Display name: " + displayName); 
//		}
		//DefaultTableDisplay dtd = (DefaultTableDisplay) displays.get(0);
		tableIn = (DefaultGenericTable) defaultTableDisplay.get(0);
	
		// get some info
		tableInName = defaultTableDisplay.getName();
		numColumns  = tableIn.getColumnCount();
		numRows     = tableIn.getRowCount();
	
		sliceLabels = new String[(int) numColumns];

		           
		logService.info(this.getClass().getName() + " Name: " + tableInName); 
		logService.info(this.getClass().getName() + " Colmuns #: " + numColumns);
		logService.info(this.getClass().getName() + " Rowss #: " + numRows); 
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
	
	/**
	 * This method deletes already open displays
	 * 
	 */
	private void deleteExistingDisplays() {
		boolean optDeleteExistingPlot = booleanDeleteExistingDoubleLogPlot;
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
				if (display.getName().equals(tableName))
					display.close();
			}
		}
	}

	/** This method takes the active column and computes results. 
	 * @param dlgProgress */
	private void processActiveInputColumn (int s, WaitingDialogWithProgressBar dlgProgress) throws InterruptedException {
		
		long startTime = System.currentTimeMillis();
		
		resultValuesTable = new double[(int) numColumns][3];
		// Compute regression parameters
		double[] resultValues = process(tableIn, s); 
		// 0 Dh, 1 R2, 2 StdErr
		resultValuesTable[s][0] = resultValues[0]; // Dh
		resultValuesTable[s][1] = resultValues[1]; // R2
		resultValuesTable[s][2] = resultValues[2]; // StdErr

		sliceLabels[s] = tableIn.getColumnHeader(s);
		
		long duration = System.currentTimeMillis() - startTime;
		TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
		SimpleDateFormat sdf = new SimpleDateFormat();
		sdf.applyPattern("HHH:mm:ss:SSS");
		logService.info(this.getClass().getName() + " Elapsed time: "+ sdf.format(duration));
	}

	/** This method loops over all input columns and computes results. 
	 * @param dlgProgress */
	private void processAllInputColumns(WaitingDialogWithProgressBar dlgProgress) throws InterruptedException{
		
		long startTimeAll = System.currentTimeMillis();
		
		resultValuesTable = new double[(int) numColumns][3];
		// loop over all slices of stack
		for (int s = 0; s < numColumns; s++) { // p...planes of an image stack
			if (!exec.isShutdown()){
				int percent = (int)Math.round((  ((float)s)/((float)numColumns)   *100.f   ));
				dlgProgress.updatePercent(String.valueOf(percent+"%"));
				dlgProgress.updateBar(percent);
				//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
				statusService.showStatus((s+1), (int)numColumns, "Processing " + (s+1) + "/" + (int)numColumns);
	
				long startTime = System.currentTimeMillis();
				logService.info(this.getClass().getName() + " Processing image number " + (s+1) + "(" + numColumns + ")");
				
				// Compute regression parameters
				double[] resultValues = process(tableIn, s); //rai is already 2D, s parameter only for display titles
				// 0 Dh, 1 R2, 2 StdErr
	
	
				resultValuesTable[s][0] = resultValues[0]; //
				resultValuesTable[s][1] = resultValues[1]; //
				resultValuesTable[s][2] = resultValues[2]; //
				
				sliceLabels[s] = tableIn.getColumnHeader(s);
	
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
		logService.info(this.getClass().getName() + " Elapsed processing time for all image(s): "+ sdf.format(duration));
	}

	/** Generates the table header {@code DefaultGenericTable} */
	private void generateTableHeader() {
		
		GenericColumn columnFileName     = new GenericColumn("File name");
		GenericColumn columnSliceName    = new GenericColumn("Column name");
		IntColumn columnKMax             = new IntColumn("k");
		IntColumn columnRegMin           = new IntColumn("RegMin");
		IntColumn columnRegMax           = new IntColumn("RegMax");
		BoolColumn columnZeroesRemoved   = new BoolColumn("Zeroes removed");
		DoubleColumn columnDh         = new DoubleColumn("Dh");	
		DoubleColumn columnR2         = new DoubleColumn("R2");	
		DoubleColumn columnStdErr     = new DoubleColumn("StdErr");
	
		tableResult = new DefaultGenericTable();
		tableResult.add(columnFileName);
		tableResult.add(columnSliceName);
		tableResult.add(columnKMax);
		tableResult.add(columnRegMin);
		tableResult.add(columnRegMax);
		tableResult.add(columnZeroesRemoved);
		tableResult.add(columnDh);
		tableResult.add(columnR2);
		tableResult.add(columnStdErr);

	}

	/**
	 * collects current result and shows table
	 * 
	 * @param int slice number of active image.
	 */
	private void collectActiveResultAndShowTable(int signalNumber) {

		int regMin = spinnerInteger_RegMin;
		int regMax = spinnerInteger_RegMax;
		int numKMax = spinnerInteger_KMax;

		int s = signalNumber;
		// 0 Intercept, 1 Dim, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
		// fill table with values
		tableResult.appendRow();
		tableResult.set("File name",  tableResult.getRowCount() - 1, tableInName);	
		if (sliceLabels != null) tableResult.set("Column name", tableResult.getRowCount() - 1, sliceLabels[s]);
		tableResult.set("k",              tableResult.getRowCount() - 1, numKMax);
		tableResult.set("RegMin",         tableResult.getRowCount() - 1, regMin);
		tableResult.set("RegMax",         tableResult.getRowCount() - 1, regMax);
		tableResult.set("Zeroes removed", tableResult.getRowCount() - 1, booleanRemoveZeroes);
		tableResult.set("Dh",             tableResult.getRowCount() - 1, resultValuesTable[s][0]);
		tableResult.set("R2",             tableResult.getRowCount() - 1, resultValuesTable[s][1]);
		tableResult.set("StdErr",         tableResult.getRowCount() - 1, resultValuesTable[s][2]);
		
		// Show table
		uiService.show(tableName, tableResult);
	}

	/** collects all results and shows table */
	private void collectAllResultsAndShowTable() {

		int regMin = spinnerInteger_RegMin;
		int regMax = spinnerInteger_RegMax;
		int numKMax = spinnerInteger_KMax;

		// loop over all slices
		for (int s = 0; s < numColumns; s++) { // slices of an image stack
			// 0 Intercept, 1 Dim, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
			// fill table with values
			tableResult.appendRow();
			tableResult.set("File name",  tableResult.getRowCount() - 1, tableInName);	
			if (sliceLabels != null) tableResult.set("Column name", tableResult.getRowCount() - 1, sliceLabels[s]);
			tableResult.set("k",              tableResult.getRowCount() - 1, numKMax);
			tableResult.set("RegMin",         tableResult.getRowCount() - 1, regMin);
			tableResult.set("RegMax",         tableResult.getRowCount() - 1, regMax);
			tableResult.set("Zeroes removed", tableResult.getRowCount() - 1, booleanRemoveZeroes);
			tableResult.set("Dh",             tableResult.getRowCount() - 1, resultValuesTable[s][0]);
			tableResult.set("R2",             tableResult.getRowCount() - 1, resultValuesTable[s][1]);
			tableResult.set("StdErr",         tableResult.getRowCount() - 1, resultValuesTable[s][2]);
					
		}
		uiService.show(tableName, tableResult);
	}

	/**
	*
	* Processing
	*/
	private double[] process(DefaultGenericTable dgt, int col) { //  c column number
	
		int regMin = spinnerInteger_RegMin;
		int regMax = spinnerInteger_RegMax;
		int numKMax = spinnerInteger_KMax;
		int numDataPoints = dgt.getRowCount();
		boolean removeZeores = booleanRemoveZeroes;

		boolean optShowPlot            = booleanShowDoubleLogPlot;
		boolean optShowSomeRadialLinePlots = booleanShowSomeRadialLinePlots;

		double[] resultValues;
		resultValues = new double[3]; // Dim, R2, StdErr
		
		double[]totals = new double[numKMax];
		double[]eps = new double[numKMax];
		// definition of eps
		for (int kk = 0; kk < numKMax; kk++) {
			eps[kk] = kk + 1;		
			//logService.info(this.getClass().getName() + " k=" + kk + " eps= " + eps[kk][b]);
		}
		//******************************************************************************************************
		xAxis1D  = new double[numDataPoints];
		signal1D = new double[numDataPoints];
		
		Column<? extends Object> column = dgt.get(col);
		for (int n = 0; n < numDataPoints; n++) {
			xAxis1D[n] = n+1;
			double dataValue = Double.valueOf((Double)column.get(n));
			if (dataValue == 999.999) {//Column is shorter, therefore shorten dataY[]
				signal1D[n] = Double.NaN;	
			} else {
				signal1D[n] = dataValue;	
			}
		}	
		Higuchi hig;
		double[] L;
		double[] regressionValues;
		signal1D = removeNaN(signal1D);
		if (removeZeores) signal1D = removeZeroes(signal1D);
		int numActualRows = 0;
		logService.info(this.getClass().getName() + " Column #: "+ (col+1) + "  " + column.getHeader() + "  Size of signal = " + signal1D.length);	
		if (signal1D.length > (numKMax * 2)) { // only data series which are large enough
			numActualRows += 1;
			hig = new Higuchi();
			L = hig.calcLengths(signal1D, numKMax);
			regressionValues = hig.calcDimension(L, regMin, regMax);
			// 0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
			
			if (optShowPlot) {
				String preName = column.getHeader();
				showPlot(hig.getLnDataX(), hig.getLnDataY(), preName, col, regMin, regMax);
			}	
			resultValues[0] = -regressionValues[1]; // Dh = -slope
			resultValues[1] = regressionValues[4];
			resultValues[2] = regressionValues[3];
		}
			
		return resultValues;
		// Dim, R2, StdErr
		// Output
		// uiService.show(tableName, table);
		// result = ops.create().img(image, new FloatType());
		// table
	}

	// This method shows the double log plot
	private void showPlot(double[] lnDataX, double[] lnDataY, String preName, int col, int regMin, int regMax) {
		// String preName = "";
		if (preName == null) {
			preName += "Col" + String.format("%03d", col) + "-";
		}
		boolean isLineVisible = false; // ?
		RegressionPlotFrame doubleLogPlot = DisplayRegressionPlotXY(lnDataX, lnDataY, isLineVisible,
				"Double Log Plot - Higuchi Dimension", preName + "-" + tableInName, "ln(k)", "ln(L)", regMin, regMax);
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
			boolean isLineVisible, String frameTitle, String plotLabel, String xAxisLabel, String yAxisLabel,
			int regMin, int regMax) {
		// jFreeChart
		RegressionPlotFrame pl = new RegressionPlotFrame(dataX, dataY, isLineVisible, frameTitle, plotLabel, xAxisLabel,
				yAxisLabel, regMin, regMax);
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
