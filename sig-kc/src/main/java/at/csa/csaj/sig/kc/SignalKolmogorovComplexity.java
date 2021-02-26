/*-
 * #%L
 * Project: ImageJ signal plugin for computing the Kolmogorov complexity and Logical depth
 * File: SignalKolmogorovComplexity.java
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


package at.csa.csaj.sig.kc;

import java.awt.Toolkit;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;

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
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;
import org.scijava.table.BoolColumn;
import org.scijava.table.Column;
import org.scijava.table.DefaultGenericTable;
import org.scijava.table.DefaultTableDisplay;
import org.scijava.table.DoubleColumn;
import org.scijava.table.FloatColumn;
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
 * A {@link Command} plugin computing <Kolmogorov complexity and Logical depth</a>
 * of a signal.
 */
@Plugin(type = InteractiveCommand.class, headless = true, menuPath = "Plugins>ComsystanJ>Signal>Kolmogorov complexity")
public class SignalKolmogorovComplexity<T extends RealType<T>> extends InteractiveCommand implements Command, Previewable { // non blocking  GUI
//public class SignalKC<T extends RealType<T>> implements Command {	//modal GUI

	private static final String PLUGIN_LABEL            = "<html><b>Kolmogorov complexity / Logical depth</b></html>";
	private static final String SPACE_LABEL             = "";
	private static final String COMPRESSIONTYPE_LABEL   = "<html><b>Compression type</b></html>";
	private static final String SIGNALMETHOD_LABEL      = "<html><b>Signal evaluation</b></html>";
	private static final String BACKGROUNDOPTIONS_LABEL = "<html><b>Background option</b></html>";
	private static final String DISPLAYOPTIONS_LABEL    = "<html><b>Display option</b></html>";
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
	
	private static final String tableOutName = "Table - Higuchi dimension";
	
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

//	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
//	private final String labelSpace = SPACE_LABEL;
	
	@Parameter(type = ItemIO.INPUT)
	private DefaultTableDisplay  defaultTableDisplay;

	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelRegression = COMPRESSIONTYPE_LABEL;
	
	@Parameter(label = "Compression type",
			description = "ZLIB or GZIB",
			style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
			choices = {"ZLIB", "GZIB"}, 
			//persist  = false,  //restore previous value default = true
			initializer = "initialCompressionType",
			callback = "callbackCompressionType")
	private String choiceRadioButt_CompressionType;
	
	@Parameter(label = "Iterations for LD:",
    		   description = "Number of compressions to compute averages",
	       	   style = NumberWidget.SPINNER_STYLE,
	           min = "1",
	           max = "99999999999999",
	           stepSize = "1",
	           persist  = false,  //restore previous value default = true
	           initializer = "initialNumIterations",
	           callback    = "callbackNumIterations")
    private int spinnerInteger_NumIterations;
	
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
	
	@Parameter(label = "Box length:", description = "Length of subsequent or gliding box - Shoud be at least three times ParamM", style = NumberWidget.SPINNER_STYLE, 
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
	
	@Parameter(label = "Process first column", callback = "callbackProcessActiveColumn")
	private Button buttonProcessActiveColumn;

	@Parameter(label = "Process all columns", callback = "callbackProcessAllColumns")
	private Button buttonProcessAllColumns;


	// ---------------------------------------------------------------------
	// The following initialzer functions set initial values
	
	protected void initialCompressioniType() {
		choiceRadioButt_CompressionType = "ZLIB";
	} 
	
	protected void initialNumIterations() {
	    spinnerInteger_NumIterations = 10;
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
	
	protected void initialDeleteExistingTable() {
		booleanDeleteExistingTable = true;
	}

	// The following method is known as "callback" which gets executed
	// whenever the value of a specific linked parameter changes.
	
	
	/** Executed whenever the {@link #choiceRadioButt_CompressionType} parameter changes. */
	protected void callbackCompressionType() {
		logService.info(this.getClass().getName() + " Compression type set to " + choiceRadioButt_CompressionType);
	}
	
	/** Executed whenever the {@link #spinInteger_NumIations} parameter changes. */
	protected void callbackNumIterations() {
		logService.info(this.getClass().getName() + " Number of iterations images set to " + spinnerInteger_NumIterations);
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
	
	/**
	 * Executed whenever the {@link #buttonProcessActiveColumn} button is pressed.
	 */
	protected void callbackProcessActiveColumn() {
		//prepare  executer service
		exec = Executors.newSingleThreadExecutor();
		
		//WaitingDialogWithProgressBar dlgProgress = new WaitingDialogWithProgressBar("<html>Computing KC and LD, please wait...<br>Open console window for further info.</html>");
		WaitingDialogWithProgressBar dlgProgress = new WaitingDialogWithProgressBar("Computing KC and LD, please wait... Open console window for further info.",
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
            		processActiveInputColumn(activeColumnIndex, dlgProgress);
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
		
		//WaitingDialogWithProgressBar dlgProgress = new WaitingDialogWithProgressBar("<html>Computing KC and LD, please wait...<br>Open console window for further info.</html>");
		WaitingDialogWithProgressBar dlgProgress = new WaitingDialogWithProgressBar("Computing KC and LD, please wait... Open console window for further info.",
																					logService, true, exec); //isCanceable = true, because processAllInputSignalss(dlgProgress) listens to exec.shutdown 
		dlgProgress.setVisible(true);

		exec.execute(new Runnable() {
            public void run() {	
            	try {
	            	logService.info(this.getClass().getName() + " Processing all available columns");
	        		getAndValidateActiveDataset();
	        		generateTableHeader();
	        		deleteExistingDisplays();
	        		processAllInputColumns(dlgProgress);
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
		tableResult.add(new GenericColumn("Signal type")); //"Entire signal", "Subsequent boxes", "Gliding box" 
		tableResult.add(new GenericColumn("Surrogate type"));
		tableResult.add(new IntColumn("# Surrogates"));
		tableResult.add(new IntColumn("Box length"));
		tableResult.add(new BoolColumn("Zeroes removed"));
	
		tableResult.add(new GenericColumn("Compression"));	
		
		if (choiceRadioButt_SignalType.equals("Entire signal")){
			tableResult.add(new DoubleColumn("Signal size [kB]")); //0 SigSize, 1 KC, 2 SigSize-KC, 3 KC/SigSize, 4 numIter, 5 LD
			tableResult.add(new DoubleColumn("KC [kB]"));
			tableResult.add(new DoubleColumn("Signal size - KC [kB]"));
			tableResult.add(new DoubleColumn("KC/Signalsize"));
			tableResult.add(new DoubleColumn("Itereations [#]"));
			tableResult.add(new DoubleColumn("LD [ns]"));
			if (choiceRadioButt_SurrogateType.equals("No surrogates")) {
				//do nothing	
			} else { //Surrogates
				tableResult.add(new DoubleColumn("KC_Surr [kB]"));  //Mean surrogate value
				tableResult.add(new DoubleColumn("LD_Surr [kB]"));  //Mean surrogate value	
				for (int s = 0; s < numSurrogates; s++) tableResult.add(new DoubleColumn("KC_Surr-#"+(s+1)+" [kB]")); 
				for (int s = 0; s < numSurrogates; s++) tableResult.add(new DoubleColumn("LD_Surr-#"+(s+1)+" [ns]")); 
			}	
		} 
		else if (choiceRadioButt_SignalType.equals("Subsequent boxes")){
			for (int n = 1; n <= numSubsequentBoxes; n++) {
				tableResult.add(new DoubleColumn("KC-#" + n + " [kB]"));	
			}
			for (int n = 1; n <= numSubsequentBoxes; n++) {
				tableResult.add(new DoubleColumn("LD-#" + n + " [ns]"));	
			}
		}
		else if (choiceRadioButt_SignalType.equals("Gliding box")){
			for (int n = 1; n <= numGlidingBoxes; n++) {
				tableResult.add(new DoubleColumn("KC-#" + n + " [kB]"));	
			}
			for (int n = 1; n <= numGlidingBoxes; n++) {
				tableResult.add(new DoubleColumn("LD-#" + n + " [ns]"));	
			}
		}	
	}
	
	/**
	 * This method deletes already open displays
	 * 
	 */
	private void deleteExistingDisplays() {
		boolean optDeleteExistingTable = booleanDeleteExistingTable;
		
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

	/** This method takes the active column and computes results. 
	 * @param dlgProgress */
	private void processActiveInputColumn (int s, WaitingDialogWithProgressBar dlgProgress) throws InterruptedException {
		
		long startTime = System.currentTimeMillis();
		
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
	}

	/** This method loops over all input columns and computes results. 
	 * @param dlgProgress */
	private void processAllInputColumns(WaitingDialogWithProgressBar dlgProgress) throws InterruptedException{
		
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
				// 0 Entropy
				logService.info(this.getClass().getName() + " Processing finished.");
				writeToTable(s, resultValues);
	
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
	 * @param int slice number of active signal.
	 * @param double[] result values
	 */
	private void writeToTable(int signalNumber, double[] resultValues) {
		logService.info(this.getClass().getName() + " Writing to the table...");
		int row = signalNumber;
		int tableColStart = 0;
		int tableColEnd   = 0;
		int tableColLast  = 0;
		
		// 0 Entropy
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
		
		tableResult.set(7, row, this.choiceRadioButt_CompressionType);
		tableColLast = 7;
		
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
			tableColStart = tableColLast +1;
			tableColEnd = (int) (tableColStart + 2 * numSubsequentBoxes); //1,2,3...  for 1,2,3... parameters
			for (int c = tableColStart; c < tableColEnd; c++ ) {
				tableResult.set(c, row, resultValues[c-tableColStart]);
			}	
		}
		else if (choiceRadioButt_SignalType.equals("Gliding box")){
			tableColStart = tableColLast +1;
			tableColEnd = (int) (tableColStart + 2 * numGlidingBoxes); //1,2,3.. for 1,2,3.... parameters 
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
	
		String  signalType    = choiceRadioButt_SignalType;
		String  surrType      = choiceRadioButt_SurrogateType;
		int     boxLength     = spinnerInteger_BoxLength;
		int     numDataPoints = dgt.getRowCount();
		boolean removeZeores  = booleanRemoveZeroes;
		
		String compressionType = choiceRadioButt_CompressionType;
		int numIterations = spinnerInteger_NumIterations;
		
		double[] resultValues = new double[6]; //0 SigSize, 1 KC, 2 SigSize-KC, 3 KC/SigSize, 4 numIter, 5 LD
		for (int r = 0; r < resultValues.length; r++) resultValues[r] = Double.NaN;
		
		double signalSize; //Bytes
	    double originalSize;   //[kB]
	    double kc = Double.NaN;
	    double ld = Double.NaN;

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
		
		signalSize   = 8.0 * signal1D.length; //Bytes
	    originalSize = signalSize/1024;   //[kB]
		
		//int numActualRows = 0;
		logService.info(this.getClass().getName() + " Column #: "+ (col+1) + "  " + signalColumn.getHeader() + "  Size of signal = " + numDataPoints);	

		//"Entire signal", "Subsequent boxes", "Gliding box" 
		//********************************************************************************************************
		if (signalType.equals("Entire signal")){	
			if (surrType.equals("No surrogates")) {
				resultValues = new double[6]; //0 SigSize, 1 KC, 2 SigSize-KC, 3 KC/SigSize, 4 numIter, 5 LD
			} else {
				resultValues = new double[6+2+2*numSurrogates]; //........+KC_SurrMean, LD_SurrMean, KC_Surr-#1,........LD_Surr-#1,.........
			}
			for (int r = 0; r < resultValues.length; r++) resultValues[r] = Double.NaN;
			logService.info(this.getClass().getName() + " Column #: "+ (col+1) + "  " + signalColumn.getHeader() + "  Size of signal = " + signal1D.length);	
			
			byte[] compressedSignal = null;
			if (choiceRadioButt_CompressionType.equals("ZLIB"))      compressedSignal =  calcCompressedSignal_ZLIB(signal1D);
			else if (choiceRadioButt_CompressionType.equals("GZIB")) compressedSignal =  calcCompressedSignal_GZIB(signal1D);
			kc =  (double)compressedSignal.length/1024; //[kB]	
				
			byte[] decompressedSignal;
			DescriptiveStatistics stats = new DescriptiveStatistics();
			for (int it = 0; it < numIterations; it++){
				long startTime = System.nanoTime();
				if (choiceRadioButt_CompressionType.equals("ZLIB"))      decompressedSignal = calcDecompressedSignal_ZLIB(compressedSignal);
				else if (choiceRadioButt_CompressionType.equals("GZIB")) decompressedSignal = calcDecompressedSignal_GZIB(compressedSignal);
				long time = System.nanoTime();
				stats.addValue((double)(time - startTime));
			}
			//durationTarget = (double)(System.nanaoTime() - startTime) / (double)iterations;
			ld = stats.getPercentile(50); //Median	; //[ns]		
		
			//0 SigSize, 1 KC, 2 SigSize-KC, 3 KC/SigSize, 4 numIter, 5 LD
			resultValues[0] = originalSize;
			resultValues[1] = kc;
			resultValues[2] = originalSize - kc;
			resultValues[3] = kc/originalSize;
			resultValues[4] = numIterations;
			resultValues[5] = ld;	
			int lastMainResultsIndex = 5;
			
			if (!surrType.equals("No surrogates")) { //Add surrogate analysis
				surrSignal1D = new double[signal1D.length];
				
				double sumKCs   = 0.0;
				double sumLDs   = 0.0;
				Surrogate surrogate = new Surrogate();
				for (int s = 0; s < numSurrogates; s++) {
					//choices = {"No surrogates", "Shuffle", "Gaussian", "Random phase", "AAFT"}, 
					if (surrType.equals("Shuffle"))      surrSignal1D = surrogate.calcSurrogateShuffle(signal1D);
					if (surrType.equals("Gaussian"))     surrSignal1D = surrogate.calcSurrogateGaussian(signal1D);
					if (surrType.equals("Random phase")) surrSignal1D = surrogate.calcSurrogateRandomPhase(signal1D);
					if (surrType.equals("AAFT"))         surrSignal1D = surrogate.calcSurrogateAAFT(signal1D);
			
					compressedSignal = null;
					if (choiceRadioButt_CompressionType.equals("ZLIB"))      compressedSignal =  calcCompressedSignal_ZLIB(surrSignal1D);
					else if (choiceRadioButt_CompressionType.equals("GZIB")) compressedSignal =  calcCompressedSignal_GZIB(surrSignal1D);
					kc =  (double)compressedSignal.length/1024; //[kB]	
					
					decompressedSignal = null;
					stats = new DescriptiveStatistics();
					for (int it = 0; it < numIterations; it++){
						long startTime = System.nanoTime();
						if (choiceRadioButt_CompressionType.equals("ZLIB"))      decompressedSignal = calcDecompressedSignal_ZLIB(compressedSignal);
						else if (choiceRadioButt_CompressionType.equals("GZIB")) decompressedSignal = calcDecompressedSignal_GZIB(compressedSignal);
						long time = System.nanoTime();
						stats.addValue((double)(time - startTime));
					}
					//durationTarget = (double)(System.nanaoTime() - startTime) / (double)iterations;
					ld = stats.getPercentile(50); //Median; //[ns]		
					
					
					resultValues[lastMainResultsIndex + 3 + s] = kc;
					resultValues[lastMainResultsIndex + 3 + numSurrogates + s] = ld;
					sumKCs += kc;
					sumLDs += ld;
				}
				resultValues[lastMainResultsIndex + 1] = sumKCs/numSurrogates;
				resultValues[lastMainResultsIndex + 2] = sumLDs/numSurrogates;
			}	
			
		//********************************************************************************************************	
		} else if (signalType.equals("Subsequent boxes")){
			resultValues = new double[(int) (2*numSubsequentBoxes)]; // KC LD == two * number of boxes		
			for (int r = 0; r<resultValues.length; r++) resultValues[r] = Double.NaN;
			subSignal1D = new double[(int) boxLength];
			//number of boxes may be smaller than intended because of NaNs or removed zeroes
			long actualNumSubsequentBoxes = (long) Math.floor((double)signal1D.length/(double)spinnerInteger_BoxLength);
		
			byte[] compressedSignal = null;
			byte[]  decompressedSignal = null;
			
			//get sub-signals and compute values
			for (int i = 0; i < actualNumSubsequentBoxes; i++) {	
				logService.info(this.getClass().getName() + " Processing subsequent box #: "+(i+1) + "/" + actualNumSubsequentBoxes);	
				int start = (i*boxLength);
				for (int ii = start; ii < (start + boxLength); ii++){ 
					subSignal1D[ii-start] = signal1D[ii];
				}
				//Compute specific values************************************************
				if (choiceRadioButt_CompressionType.equals("ZLIB"))      compressedSignal =  calcCompressedSignal_ZLIB(subSignal1D);
				else if (choiceRadioButt_CompressionType.equals("GZIB")) compressedSignal =  calcCompressedSignal_GZIB(subSignal1D);
				kc =  (double)compressedSignal.length/1024; //[kB]	
						
				DescriptiveStatistics stats = new DescriptiveStatistics();
				for (int it = 0; it < numIterations; it++){
					long startTime = System.nanoTime();
					if (choiceRadioButt_CompressionType.equals("ZLIB"))      decompressedSignal = calcDecompressedSignal_ZLIB(compressedSignal);
					else if (choiceRadioButt_CompressionType.equals("GZIB")) decompressedSignal = calcDecompressedSignal_GZIB(compressedSignal);
					long time = System.nanoTime();
					stats.addValue((double)(time - startTime));
				}
				ld = stats.getPercentile(50); //Median	[ns]	
				resultValues[i] = kc;	
				resultValues[(int)(i +   numSubsequentBoxes)] = ld;					
				//***********************************************************************
			}	
		//********************************************************************************************************			
		} else if (signalType.equals("Gliding box")){
			resultValues = new double[(int) (2*numGlidingBoxes)]; // KC LD == two * number of boxes	
			for (int r = 0; r<resultValues.length; r++) resultValues[r] = Double.NaN;
			subSignal1D = new double[(int) boxLength];
			//number of boxes may be smaller because of NaNs or removed zeroes
			long actualNumGlidingBoxes = signal1D.length - spinnerInteger_BoxLength + 1;
			
			byte[] compressedSignal = null;
			byte[]  decompressedSignal = null;
			
			//get sub-signals and compute dimensions
			for (int i = 0; i < actualNumGlidingBoxes; i++) {
				logService.info(this.getClass().getName() + " Processing gliding box #: "+(i+1) + "/" + actualNumGlidingBoxes);	
				int start = i;
				for (int ii = start; ii < (start + boxLength); ii++){ 
					subSignal1D[ii-start] = signal1D[ii];
				}	
				//Compute specific values************************************************
				if (choiceRadioButt_CompressionType.equals("ZLIB"))      compressedSignal = calcCompressedSignal_ZLIB(subSignal1D);
				else if (choiceRadioButt_CompressionType.equals("GZIB")) compressedSignal = calcCompressedSignal_GZIB(subSignal1D);
				kc =  (double)compressedSignal.length/1024; //[kB]	
						
				DescriptiveStatistics stats = new DescriptiveStatistics();
				for (int it = 0; it < numIterations; it++){
					long startTime = System.nanoTime();
					if (choiceRadioButt_CompressionType.equals("ZLIB"))      decompressedSignal = calcDecompressedSignal_ZLIB(compressedSignal);
					else if (choiceRadioButt_CompressionType.equals("GZIB")) decompressedSignal = calcDecompressedSignal_GZIB(compressedSignal);
					long time = System.nanoTime();
					stats.addValue((double)(time - startTime));
				}
				ld = stats.getPercentile(50); //Median	[ns]	
				resultValues[i] = kc;	
				resultValues[(int)(i +   numGlidingBoxes)] = ld;			
				//***********************************************************************
			}
		}
		return resultValues;
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
	
	/**
	 * This method calculates the mean of a data series
	 * 
	 * @param data1D
	 * @return Double Mean
	 */
	private Double calcMean(double[] data1D) {
		double sum = 0;
		for (double d : data1D) {
			sum += d;
		}
		return sum / data1D.length;
	}
	
	/**
	 * This method calculates and returns compressed signal
	 * @param signal 1D vector
	 * @return byte[] compressed signal
	 */
	private byte[] calcCompressedSignal_ZLIB(double[] signal) {
		
		 byte[] data = new byte[signal.length * 8];
		 for (int i = 0; i < signal.length; i++){
			 long v = Double.doubleToLongBits(signal[i]);
			 data[i*8+7] = (byte)(v);
			 data[i*8+6] = (byte)(v>>>8);
			 data[i*8+5] = (byte)(v>>>16);
			 data[i*8+4] = (byte)(v>>>24);
			 data[i*8+3] = (byte)(v>>>32);
			 data[i*8+2] = (byte)(v>>>40);
			 data[i*8+1] = (byte)(v>>>48);
			 data[i*8]   = (byte)(v>>>56);
		 }
		 Deflater deflater = new Deflater(); 
		 deflater.setLevel(Deflater.BEST_COMPRESSION);
	     deflater.setInput(data); 
	     ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);  

	     deflater.finish(); 
	     byte[] buffer = new byte[1048];  
	     while (!deflater.finished()) { 
	    	 int count = deflater.deflate(buffer); 
	    	 //System.out.println("PlotOpComplLogDepth  Count: " +count);
	         outputStream.write(buffer, 0, count);  
	     } 
	     try {
			 outputStream.close();
		 } catch (IOException e) {
			 // TODO Auto-generated catch block
			 e.printStackTrace();
		 } 
	     byte[] output = outputStream.toByteArray(); 
	     deflater.end();
	     //System.out.println("PlotOpComplLogDepth Original: " + data.length  ); 
	     //System.out.println("PlotOpComplLogDepth ZLIB Compressed: " + output.length ); 
	     return output;
	}
	
	/**
	 * This method calculates and returns compressed signal
	 * @param signal 1D vector
	 * @return byte[] compressed signal
	 */
	private byte[] calcCompressedSignal_GZIB(double[] signal) {
		
		 byte[] data = new byte[signal.length * 8];
		 for (int i = 0; i < signal.length; i++){
			 long v = Double.doubleToLongBits(signal[i]);
			 data[i*8+7] = (byte)(v);
			 data[i*8+6] = (byte)(v>>>8);
			 data[i*8+5] = (byte)(v>>>16);
			 data[i*8+4] = (byte)(v>>>24);
			 data[i*8+3] = (byte)(v>>>32);
			 data[i*8+2] = (byte)(v>>>40);
			 data[i*8+1] = (byte)(v>>>48);
			 data[i*8]   = (byte)(v>>>56);
		 }
		  ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	        try{
	            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream);
	            gzipOutputStream.write(data);
	            gzipOutputStream.close();
	        } catch(IOException e){
	            throw new RuntimeException(e);
	        }
	     byte[] output = outputStream.toByteArray(); 
	     try {
			outputStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    //System.out.println("PlotOpComplLogDepth Original: " + data.length  ); 
	    //System.out.println("PlotOpComplLogDepth GZIB Compressed: " + output.length ); 
	    return output;
	}
	
	/**
	 * This method decompresses byte array
	 * @param  byte[] array  compressed
	 * @return byte[] array  decompressed
	 */
	private byte[] calcDecompressedSignal_ZLIB(byte[] array) {
		Inflater inflater = new Inflater();   
		inflater.setInput(array);  
		   
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();  
		byte[] buffer = new byte[1024];  
		while (!inflater.finished()) {  
		    int count = 0;
			try {
				count = inflater.inflate(buffer);
			} catch (DataFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}  
		    outputStream.write(buffer, 0, count);  
		}  
		try {
			outputStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
		byte[] output = outputStream.toByteArray();  		   
		inflater.end();	
	    //System.out.println("PlotOpComplLogDepth ZLIB Input: " + array.length  ); 
	    //System.out.println("PlotOpComplLogDepth Decompressed: " + output.length ); 
	    return output;
	}
	/**
	 * This method decompresses byte array
	 * @param  byte[] array  compressed
	 * @return byte[] array  decompressed
	 */
	private byte[] calcDecompressedSignal_GZIB(byte[] array) {
		
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		ByteArrayInputStream inputStream = new ByteArrayInputStream(array);
		InputStream in = null;
		try {
			in = new GZIPInputStream(inputStream);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    byte[] bbuf = new byte[256];
	    while (true) {
	        int r = 0;
			try {
				r = in.read(bbuf);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        if (r < 0) {
	          break;
	        }
	        buffer.write(bbuf, 0, r);
	    }
		byte[] output = buffer.toByteArray();  		   
		try {
			buffer.close();
			inputStream.close();
			in.close();		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    //System.out.println("PlotOpComplLogDepth GZIB Input: " + array.length  ); 
	    //System.out.println("PlotOpComplLogDepth Decompressed: " + output.length ); 
	    return output;
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
		ij.command().run(SignalKolmogorovComplexity.class, true);
	}
}
