/*-
 * #%L
 * Project: ImageJ2 signal plugin for generating a Poincare plot
 * File: SignalPoincarePlot.java
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

package at.csa.csaj.sig.poincare;

import java.awt.Frame;
import java.awt.Point;
import java.awt.Toolkit;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JFrame;
import javax.swing.UIManager;
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
import org.scijava.log.LogService;
import org.scijava.menu.MenuConstants;
import org.scijava.plugin.Menu;
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
import at.csa.csaj.commons.plot.SignalPlotFrame;
import at.csa.csaj.sig.open.SignalOpener;


/**
 * A {@link Command} plugin computing <Poincare plots</a>
 * of a signal.
 */
@Plugin(type = ContextCommand.class, 
	headless = true,
	label = "Poincare plot",
	initializer = "initialPluginLaunch",
	//iconPath = "/images/comsystan-??.png", //Menu entry icon
	menu = {
	@Menu(label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT, mnemonic = MenuConstants.PLUGINS_MNEMONIC),
	@Menu(label = "ComsystanJ"),
	@Menu(label = "Signal"),
	@Menu(label = "Poincare plot ", weight = 140)}) //Space at the end of the label is necessary to avoid duplicate with image2d plugin 
//public class SignalPoincarePlot<T extends RealType<T>> extends InteractiveCommand { // non blocking  GUI
public class SignalPoincarePlot<T extends RealType<T>> extends ContextCommand implements Previewable { //modal GUI with cancel

	private static final String PLUGIN_LABEL                = "<html><b>Poincare plot</b></html>";
	private static final String SPACE_LABEL                 = "";
	private static final String POINCAREPLOT_LABEL          = "<html><b>Poincare plot option</b></html>";
	private static final String ANALYSISOPTIONS_LABEL       = "<html><b>Analysis options</b></html>";
	private static final String BACKGROUNDOPTIONS_LABEL     = "<html><b>Background option</b></html>";
	private static final String DISPLAYOPTIONS_LABEL        = "<html><b>Display option</b></html>";
	private static final String PROCESSOPTIONS_LABEL        = "<html><b>Process options</b></html>";
	
	private static double[] signal1D;
	private static double[] domain1D;
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
	
	@Parameter(label = "Lag", description = "(time) lag",
			   style = NumberWidget.SPINNER_STYLE,
			   min = "0",
			   max = "1000",
			   stepSize = "1",
			   persist = true, // restore  previous value  default  =  true
			   initializer = "initialNumLag",
			   callback = "callbackNumLag")
	private int spinnerInteger_NumLag;

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

	@Parameter(label = "Remove zero values",
			   persist = true,
		       callback = "callbackRemoveZeroes")
	private boolean booleanRemoveZeroes;
	
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
	
	// ---------------------------------------------------------------------
		
	
	protected void initialPluginLaunch() {
		//tableIn = (DefaultGenericTable) defaultTableDisplay.get(0);
		initialNumLag();
		checkItemIOIn();
	}
	
	protected void initialNumLag() {
		spinnerInteger_NumLag = 10;
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
	
	
	/** Executed whenever the {@link #spinnerInteger_NumLag} parameter changes. */
	protected void callbackNumLag() {
		logService.info(this.getClass().getName() + " Lag set to " + spinnerInteger_NumLag);
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
	    	   	//uiService.show(tableOutName, tableOut);
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
	    	   	//uiService.show(tableOutName, tableOut);
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
		    	   	//uiService.show(tableOutName, tableOut);   //Show table because it did not go over the run() method
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
		
		numLag = spinnerInteger_NumLag;
		// number of lags > numDataPoints is not allowed
		if (numLag >= numRows) {
			numLag = (int)numRows - 1;
			spinnerInteger_NumLag = numLag;
		}
	   
		logService.info(this.getClass().getName() + " Name: "      + tableInName); 
		logService.info(this.getClass().getName() + " Columns #: " + numColumns);
		logService.info(this.getClass().getName() + " Rows #: "    + numRows); 
	}

	/**
	* This method starts the workflow for a single column of the active display
	*/
	protected void startWorkflowForSingleColumn() {
		
		dlgProgress = new WaitingDialogWithProgressBar("Generating Poincare plot, please wait... Open console window for further info.",
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
		
		dlgProgress = new WaitingDialogWithProgressBar("Generating Poincare plot, please wait... Open console window for further info.",
							logService, false, exec); //isCanceable = true, because processAllInputSignalss(dlgProgress) listens to exec.shutdown 
		dlgProgress.setVisible(true);

    	logService.info(this.getClass().getName() + " Processing all available columns");
    	deleteExistingDisplays();
		generateTableHeader();
		processAllInputColumns();
		dlgProgress.addMessage("Processing finished!");
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
//		
//		if (optDeleteExistingTables) {
//			List<Display<?>> list = defaultDisplayService.getDisplays();
//			for (int i = 0; i < list.size(); i++) {
//				Display<?> display = list.get(i);
//				//System.out.println("display name: " + display.getName());
//				if (display.getName().contains(tableOutName))
//					display.close();
//			}
//		}
		if (optDeleteExistingImgs) {
			Frame frame;
			Frame[] listFrames = JFrame.getFrames();
			for (int i = listFrames.length -1 ; i >= 0; i--) { //Reverse order, otherwise focus is not given free from the last image
				frame = listFrames[i];
				//System.out.println("frame name: " + frame.getTitle());
				if (frame.getTitle().contains("Poincare plot(s)")) {
					frame.setVisible(false); //Successfully closes also in Fiji
					frame.dispose();
				}
			}
		}
	}

	/** 
	 * This method takes the single column and computes results. 
	 * @Param int s
	 * */
	private void processSingleInputColumn (int s) {
		
		long startTime = System.currentTimeMillis();
		
		double[] dataY = process(tableIn, s); 
		logService.info(this.getClass().getName() + " Processing finished.");
		
		//int selectedOption = JOptionPane.showConfirmDialog(null, "Do you want to display the Autocorrelation?\nNot recommended for a large number of signals", "Display option", JOptionPane.YES_NO_OPTION); 
		//if (selectedOption == JOptionPane.YES_OPTION) {
			if (dataY == null) return;
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
			SignalPlotFrame pdf = new SignalPlotFrame(signal1D, dataY, isLineVisible, "Poincare plot(s)", signalTitle, xLabel, yLabel, seriesLabels[0]);
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
	 * This method loops over all input columns and computes results. 
	 * 
	 * */
	private void processAllInputColumns() {
		
		long startTimeAll = System.currentTimeMillis();
		double dataY[][] = new double[(int) numColumns][(int) numRows];
		
		// loop over all slices of stack
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
				
				if (resultValues != null) {
					for (int i = 0; i < resultValues.length; i++) {
						dataY[s][i] = resultValues[i]; 
					}
				}
				
				logService.info(this.getClass().getName() + " Processing finished.");
	
				long duration = System.currentTimeMillis() - startTime;
				TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
				SimpleDateFormat sdf = new SimpleDateFormat();
				sdf.applyPattern("HHH:mm:ss:SSS");
				logService.info(this.getClass().getName() + " Elapsed time: "+ sdf.format(duration));
			//}
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
			SignalPlotFrame pdf = new SignalPlotFrame(signal1D, dataY, isLineVisible, "Poincare plot(s)", signalTitle, xLabel, yLabel, seriesLabels);
			Point pos = pdf.getLocation();
			pos.x = (int) (pos.getX() - 100);
			pos.y = (int) (pos.getY() + 100);
			pdf.setLocation(pos);
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
		//uiService.show(tableOutName, tableOut);
	}
	
	/**
	*
	* Processing
	*/
	private double[] process(DefaultGenericTable dgt, int col) { //  c column number
	
		if (dgt == null) {
			logService.info(this.getClass().getName() + " WARNING: dgt==null, no signal for processing!");
		}
		
		String  signalRange  = choiceRadioButt_SignalRange;
		String  surrType      = choiceRadioButt_SurrogateType;
		//int     boxLength     = spinnerInteger_BoxLength;
		int     numDataPoints = dgt.getRowCount();
		boolean removeZeores  = booleanRemoveZeroes;
		int numLag            = this.numLag; 
		
		//******************************************************************************************************
		//domain1D = new double[numDataPoints];
		signal1D = new double[numDataPoints];
		for (int n = 0; n < numDataPoints; n++) {
			//domain1D[n] = Double.NaN;
			signal1D[n] = Double.NaN;
		}
		
		signalColumn = dgt.get(col);
		String columnType = signalColumn.get(0).getClass().getSimpleName();	
		logService.info(this.getClass().getName() + " Column type: " + columnType);	
		if (!columnType.equals("Double")) {
			logService.info(this.getClass().getName() + " NOTE: Column type is not supported");	
			return null; 
		}
		
		for (int n = 0; n < numDataPoints; n++) {
			//domain1D[n]  = n+1;
			signal1D[n] = Double.valueOf((Double)signalColumn.get(n));
		}	
		
		signal1D = removeNaN(signal1D);
		if (removeZeores) signal1D = removeZeroes(signal1D);

		//numDataPoints may be smaller now
		numDataPoints = signal1D.length;
		
		logService.info(this.getClass().getName() + " Column #: "+ (col+1) + "  " + signalColumn.getHeader() + "  Size of signal = " + numDataPoints);
		if (numDataPoints == 0) return null; //e.g. if signal had only NaNs
		
		//domain1D = new double[numDataPoints];
		//for (int n = 0; n < numDataPoints; n++) domain1D[n] = n+1;
		
		// number of max lag >= numDataPoints is not allowed
		if (numLag >= numDataPoints) {
			numLag = numDataPoints;
		}
		double[] resultValues = new double[numDataPoints];
		//double[] rangeAuCorr  = new double[numLag];
		for (int i = 0; i < numDataPoints; i++) {
			resultValues[i]   = Double.NaN;
		}
						
		//"Entire signal", "Subsequent boxes", "Gliding box" 
		//********************************************************************************************************
		if (signalRange.equals("Entire signal")){	//only this option is possible for autocorrelation
			
			if (!surrType.equals("No surrogates")) {
				Surrogate surrogate = new Surrogate();	
				String windowingType = "Rectangular";
				//choices = {"No surrogates", "Shuffle", "Gaussian", "Random phase", "AAFT"}, 
				if (surrType.equals("Shuffle"))      signal1D = surrogate.calcSurrogateShuffle(signal1D);
				if (surrType.equals("Gaussian"))     signal1D = surrogate.calcSurrogateGaussian(signal1D);
				if (surrType.equals("Random phase")) signal1D = surrogate.calcSurrogateRandomPhase(signal1D, windowingType);
				if (surrType.equals("AAFT"))         signal1D = surrogate.calcSurrogateAAFT(signal1D, windowingType);
			}
			
			//Generate Poincare plot
			for (int i = 0; i < (numDataPoints - numLag); i++) {
				resultValues[i] = signal1D[i + numLag];
			}
			
			
		//********************************************************************************************************	
		} else if (signalRange.equals("Subsequent boxes")){ //not for Autocorrelation
		
		//********************************************************************************************************			
		} else if (signalRange.equals("Gliding box")){ //not for autocorrelation
		
		}
		
		return resultValues;
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
