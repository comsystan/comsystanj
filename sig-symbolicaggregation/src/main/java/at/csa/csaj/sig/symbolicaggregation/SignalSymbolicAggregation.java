/*-
 * #%L
 * Project: ImageJ signal plugin for symbolic aggregation
 * File: SignalSymbolicAggregation.java
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

package at.csa.csaj.sig.symbolicaggregation;

import java.awt.Toolkit;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.UIManager;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ImageJ;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
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
import org.scijava.table.Column;
import org.scijava.table.DefaultGenericTable;
import org.scijava.table.DefaultTableDisplay;
import org.scijava.ui.UIService;
import org.scijava.widget.Button;
import org.scijava.widget.ChoiceWidget;
import org.scijava.widget.NumberWidget;
import at.csa.csaj.commons.signal.algorithms.Surrogate;
import at.csa.csaj.commons.dialog.WaitingDialogWithProgressBar;
import at.csa.csaj.sig.open.SignalOpener;


/**
 * A {@link Command} plugin generating a <Symbolic aggregation</a>
 * of a signal.
 */
@Plugin(type = InteractiveCommand.class, headless = true, menuPath = "Plugins>ComsystanJ>Signal>Symbolic aggregation")
public class SignalSymbolicAggregation<T extends RealType<T>> extends InteractiveCommand implements Command, Previewable { // non blocking  GUI
//public class SignalSymbolicAggregation<T extends RealType<T>> implements Command {	//modal GUI

	private static final String PLUGIN_LABEL                      = "<html><b>Symbolic aggregation</b></html>";
	private static final String SPACE_LABEL                       = "";
	private static final String SYMBOLICAGGREGATIONOPTIONS_LABEL = "<html><b>Symbolic aggregation options</b></html>";
	private static final String ANALYSISOPTIONS_LABEL             = "<html><b>Analysis options</b></html>";
	private static final String BACKGROUNDOPTIONS_LABEL           = "<html><b>Background option</b></html>";
	private static final String DISPLAYOPTIONS_LABEL              = "<html><b>Display option</b></html>";
	private static final String PROCESSOPTIONS_LABEL              = "<html><b>Process options</b></html>";
	
	private static double[] signal1D;
	private static double[] domain1D;
	private static double[] subSignal1D;
	private static double[] surrSignal1D;
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
	

	private static String signalString = null; //Symbolic representation of signal
	private static String[][] LUMatrix;
	
	private static final String imageOutName = "Symbolic aggregation";
	
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
	
	@Parameter
	private DatasetService datasetService;



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
	
	
	@Parameter(label = "Symbolic aggregation", type = ItemIO.OUTPUT)
	private Dataset datasetOut;
	
	private Img<UnsignedByteType> singleImg;
	

	// Widget elements------------------------------------------------------

	//@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	//private final String labelPlugin = PLUGIN_LABEL;

//	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
//	private final String labelSpace = SPACE_LABEL;
	
	@Parameter(type = ItemIO.INPUT)
	private DefaultTableDisplay  defaultTableDisplay;

	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelSymbolicAggregationOptions = SYMBOLICAGGREGATIONOPTIONS_LABEL;
	
	@Parameter(label = "Aggregation length", description = "Aggregation length", style = NumberWidget.SPINNER_STYLE, 
		min = "2", max = "9999999999999999999", stepSize = "1",
		persist = false, // restore  previous value  default  =  true
		initializer = "initialAggLength", callback = "callbackAggLength")
	private int spinnerInteger_AggLength;
	
	@Parameter(label = "Alphabet size", description = "Number of distinct characters", style = NumberWidget.SPINNER_STYLE, 
		min = "4", max = "4", stepSize = "1", 
		persist = false, // restore  previous value  default  =  true
		initializer = "initialAlphabetSize", callback = "callbackAlphabetSize")
	private int spinnerInteger_AlphabetSize;

	@Parameter(label = "Word length", description = "Length of a word", style = NumberWidget.SPINNER_STYLE, 
		min = "1", max = "9999999999999999999", stepSize = "1", 
		persist = false, // restore  previous value  default  =  true
		initializer = "initialWordLength", callback = "callbackWordLength")
	private int spinnerInteger_WordLength;
	
		@Parameter(label = "Subword length", description = "Number of characters for reconstruction - Chaos game", style = NumberWidget.SPINNER_STYLE, 
		min = "1", max = "9999999999999999999", stepSize = "1", 
		persist = false, // restore  previous value  default  =  true
		initializer = "initialSubWordLength", callback = "callbackSubWordLength")
	private int spinnerInteger_SubWordLength;
	
		@Parameter(label = "Magnification", description = "Magnification for output image", style = NumberWidget.SPINNER_STYLE, 
		min = "1", max = "9999999999999999999", stepSize = "1", 
		persist = false, // restore  previous value  default  =  true
		initializer = "initialMag", callback = "callbackMag")
	private int spinnerInteger_Mag;
	
	@Parameter(label = "Image size", visibility = ItemVisibility.MESSAGE, persist = false)
	private String labelImageSize = "400x400";
		
	@Parameter(label = "Color model",
			description = "Color model of output image",
			style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
			choices = {"Grey-8bit", "Color-RGB"}, //
			//persist  = false,  //restore previous value default = true
			initializer = "initialColorModelType",
			callback = "callbackColorModelType")
	private String choiceRadioButt_ColorModelType;

	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelAnalysisOptions = ANALYSISOPTIONS_LABEL;

	@Parameter(label = "Analysis type",
			description = "Entire signal, Subsequent boxes or Gliding box",
			style = ChoiceWidget.LIST_BOX_STYLE,
			choices = {"Entire signal"}, //, "Subsequent boxes", "Gliding box"}, 
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

//	@Parameter(label = "Remove zero values", persist = false,
//		       callback = "callbackRemoveZeroes")
//	private boolean booleanRemoveZeroes;
	
	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelDisplayOptions = DISPLAYOPTIONS_LABEL;

	@Parameter(label = "Delete existing result image",
			   // persist = false, //restore previous value default = true
			   initializer = "initialDeleteExistingImage")
	private boolean booleanDeleteExistingImage;

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
	
	protected void initialAggLength() {
		spinnerInteger_AggLength = 2;
	}

	protected void initialAlphabetSize() {
		spinnerInteger_AlphabetSize = 4;
	}
	
	protected void initialWordLength() {
		spinnerInteger_WordLength = 4;
	}
	
	protected void initialSubWordLength() {
		spinnerInteger_SubWordLength = 2;
	}
	
	protected void initialMag() {
		spinnerInteger_Mag = 100;
	}
	
	protected void initialColorModelType() {
		choiceRadioButt_ColorModelType = "Grey-8bit";
	}
	
	protected void initialAnalysisType() {
		choiceRadioButt_AnalysisType = "Entire signal";
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
	
//	protected void initialRemoveZeroes() {
//		booleanRemoveZeroes = false;
//	}	
	
	protected void initialDeleteExistingImage() {
		booleanDeleteExistingImage = true;
	}

	// The following method is known as "callback" which gets executed
	// whenever the value of a specific linked parameter changes.
	
		/** Executed whenever the {@link #spinInteger_AggLength} parameter changes. */
	protected void callbackAggLength() {
		logService.info(this.getClass().getName() + " Aggregation length set to " + spinnerInteger_AggLength);
	}

		/** Executed whenever the {@link #spinInteger_AlphabetSize} parameter changes. */
	protected void callbackAlphabetSize() {
		logService.info(this.getClass().getName() + " Alphabet size set to " + spinnerInteger_AlphabetSize);
		labelImageSize = String.valueOf(((int) (Math.sqrt(spinnerInteger_AlphabetSize))) * (int) Math.pow(2, spinnerInteger_SubWordLength-1) * spinnerInteger_Mag);
		labelImageSize = labelImageSize+"x"+labelImageSize;
		logService.info(this.getClass().getName() + " Image size will be " + labelImageSize);
	}

		/** Executed whenever the {@link #spinInteger_WordLength} parameter changes. */
	protected void callbackWordLength() {
		logService.info(this.getClass().getName() + " Word length set to " + spinnerInteger_WordLength);
	}

		/** Executed whenever the {@link #spinInteger_SubWordLength} parameter changes. */
	protected void callbackSubWordLength() {
		logService.info(this.getClass().getName() + " Sub-word length set to " + spinnerInteger_SubWordLength);
		labelImageSize = String.valueOf(((int) (Math.sqrt(spinnerInteger_AlphabetSize))) * (int) Math.pow(2, spinnerInteger_SubWordLength-1) * spinnerInteger_Mag);
		labelImageSize = labelImageSize+"x"+labelImageSize;
		logService.info(this.getClass().getName() + " Image size will be " + labelImageSize);
	}

		/** Executed whenever the {@link #spinInteger_Mag} parameter changes. */
	protected void callbackMag() {
		logService.info(this.getClass().getName() + " Magnification set to " + spinnerInteger_Mag);
		labelImageSize = String.valueOf(((int) (Math.sqrt(spinnerInteger_AlphabetSize))) * (int) Math.pow(2, spinnerInteger_SubWordLength-1) * spinnerInteger_Mag);
		labelImageSize = labelImageSize+"x"+labelImageSize;
		logService.info(this.getClass().getName() + " Image size will be " + labelImageSize);
	}
	
	/** Executed whenever the {@link #choiceRadioButt_ColorModelType} parameter changes. */
	protected void callbackColorModelType() {
		logService.info(this.getClass().getName() + " Color model set to " + choiceRadioButt_ColorModelType);
	}

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

//	/** Executed whenever the {@link #booleanRemoveZeroes} parameter changes. */
//	protected void callbackRemoveZeroes() {
//		logService.info(this.getClass().getName() + " Remove zeroes set to " + booleanRemoveZeroes);
//	}

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
		
		//dlgProgress = new WaitingDialogWithProgressBar("<html>Generating symbolic aggregation, please wait...<br>Open console window for further info.</html>");
		dlgProgress = new WaitingDialogWithProgressBar("Generating symbolic aggregation, please wait... Open console window for further info.",
																					logService, false, exec); //isCanceable = false, because no following method listens to exec.shutdown 
		dlgProgress.updatePercent("");
		dlgProgress.setBarIndeterminate(true);
		dlgProgress.setVisible(true);
		
    	exec.execute(new Runnable() {
            public void run() {
        	    try {
        	    	logService.info(this.getClass().getName() + " Processing single signal");
            		getAndValidateActiveDataset();
           
            		deleteExistingDisplays();
            		//int activeColumnIndex = getActiveColumnIndex();
            		//processActiveInputColumn(activeColumnIndex);
              		if (spinnerInteger_NumColumn <= numColumns) processSingleInputColumn(spinnerInteger_NumColumn - 1);
            		dlgProgress.addMessage("Processing finished! Preparing visualization...");		
            		//collectActiveResultAndShowTable(activeColumnIndex);
            		showImage();
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
		
		//dlgProgress = new WaitingDialogWithProgressBar("<html>Generating symbolic aggregation, please wait...<br>Open console window for further info.</html>");
		dlgProgress = new WaitingDialogWithProgressBar("Generating symbolic aggregation, please wait... Open console window for further info.",
																					logService, true, exec); //isCanceable = true, because processAllInputSignalss(dlgProgress) listens to exec.shutdown 
		dlgProgress.setVisible(true);

		exec.execute(new Runnable() {
            public void run() {	
            	try {
	            	logService.info(this.getClass().getName() + " Processing all available columns");
	        		getAndValidateActiveDataset();
	      
	        		deleteExistingDisplays();
	        		processAllInputColumns();
	        		dlgProgress.addMessage("Processing finished! Preparing visualization...");
	        		showImage();
	        		
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
	
	/**
	 * This method deletes already open displays
	 * 
	 */
	private void deleteExistingDisplays() {
		boolean optDeleteExistingImage = booleanDeleteExistingImage;
		
		if (optDeleteExistingImage) {
			List<Display<?>> list = defaultDisplayService.getDisplays();
			for (int i = 0; i < list.size(); i++) {
				Display<?> display = list.get(i);
				//System.out.println("display name: " + display.getName());
				if (display.getName().equals(imageOutName))
					display.close();
			}
		}
	}

  	/** 
	 * This method takes the single column s and computes results. 
	 * @Param int s
	 * */
	private void processSingleInputColumn (int s) throws InterruptedException {
		
		long startTime = System.currentTimeMillis();
		
		// Compute result values
		singleImg = process(tableIn, s); 
		datasetOut = datasetService.create(singleImg);	
		
		if (datasetOut.numDimensions() == 2) {
			datasetOut.axis(0).setType(Axes.X);
			datasetOut.axis(1).setType(Axes.Y);
		} else if (datasetOut.numDimensions() == 3) { //RGB
			datasetOut.axis(0).setType(Axes.X);
			datasetOut.axis(1).setType(Axes.Y);
			datasetOut.axis(2).setType(Axes.CHANNEL);
			datasetOut.setCompositeChannelCount(3);
		}
		datasetOut.setName(imageOutName);
		logService.info(this.getClass().getName() + " Processing finished.");

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
		
		
		//create empty image
		datasetOut = null;
		RandomAccess<T> randomAccessResultImg = null;
		Cursor<UnsignedByteType> cursor;
		long[] pos;
		int value;
		
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
				singleImg = process(tableIn, s);
				
				//create stack datasetOut with info from first image
				if (s == 0) {
					String name = imageOutName;
					int bitsPerPixel;
					AxisType[] axes;
					long[] dims;
					if ((numColumns > 1) && (choiceRadioButt_ColorModelType.equals("Grey-8bit"))) {
						bitsPerPixel = 8;
						dims = new long[]{singleImg.dimension(0), singleImg.dimension(1), numColumns};
						axes = new AxisType[]{Axes.X, Axes.Y, Axes.Z};
					}
					else if ((numColumns > 1) && (choiceRadioButt_ColorModelType.equals("Color-RGB"))) {
						bitsPerPixel = 8; //24 throws an error?
						dims = new long[]{singleImg.dimension(0), singleImg.dimension(1), 3, numColumns}; //RGB
						axes = new AxisType[]{Axes.X, Axes.Y, Axes.CHANNEL, Axes.Z};
					}
					else {
						logService.info(this.getClass().getName() + " Number of images is " + numColumns +" - Generation not possible!");
						return;
					}
					
					boolean signed   = false;
					boolean floating = false;
					boolean virtual  = false;

					//dataset = ij.dataset().create(dims, name, axes, bitsPerPixel, signed, floating);
					datasetOut = datasetService.create(dims, name, axes, bitsPerPixel, signed, floating, virtual);
					if ((numColumns > 1) && (choiceRadioButt_ColorModelType.equals("Color-RGB"))) {
						datasetOut.setCompositeChannelCount(3);
						datasetOut.setRGBMerged(true);
					}
					//RandomAccess<T> randomAccess = (RandomAccess<T>) dataset.getImgPlus().randomAccess();
					//resultImg = new ArrayImgFactory<>(new UnsignedByteType()).create(singleImg.dimension(0), singleImg.dimension(1), numColumns);
					randomAccessResultImg = (RandomAccess<T>) datasetOut.randomAccess();
				}
				//Copy to resultImg
				if (choiceRadioButt_ColorModelType.equals("Grey-8bit")) {
					cursor = singleImg.localizingCursor();
					pos = new long[2];
					while (cursor.hasNext()) {
						cursor.fwd();
						cursor.localize(pos);
						value = cursor.get().getInteger();
						randomAccessResultImg.setPosition(pos[0], 0);
						randomAccessResultImg.setPosition(pos[1], 1);
						randomAccessResultImg.setPosition(s, 2);
						randomAccessResultImg.get().setReal(value);
					}  	
				}
			
				if (choiceRadioButt_ColorModelType.equals("Color-RGB")) {
					cursor = singleImg.localizingCursor();
					pos = new long[3];
					while (cursor.hasNext()) {
						cursor.fwd();
						cursor.localize(pos);
						value = cursor.get().getInteger();
						randomAccessResultImg.setPosition(pos[0], 0);
						randomAccessResultImg.setPosition(pos[1], 1);
						randomAccessResultImg.setPosition(pos[2], 2);
						randomAccessResultImg.setPosition(s, 3);
						randomAccessResultImg.get().setReal(value);
					}  	
				}
				
				logService.info(this.getClass().getName() + " Processing finished.");
			
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
	 * shows the result image(s)
	 */
	private void showImage() {
		// Show table
		uiService.show(imageOutName, datasetOut);
	}
	
	/**
	*
	* Processing
	*/
	private Img<UnsignedByteType>  process(DefaultGenericTable dgt, int col) { //  c column number
	
		String  analysisType    = choiceRadioButt_AnalysisType;
		String  surrType        = choiceRadioButt_SurrogateType;
		int     numDataPoints   = dgt.getRowCount();
		//boolean removeZeores  = booleanRemoveZeroes;
		int     aggLength       = spinnerInteger_AggLength;
		int     alphabetSize    = spinnerInteger_AlphabetSize;
		int     wordLength      = spinnerInteger_WordLength;
		int     subWordLength   = spinnerInteger_SubWordLength;
		int     mag             = spinnerInteger_Mag;
		String  colorModelType  = choiceRadioButt_ColorModelType;//"Grey-8bit", "Color-RGB"
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
		
		
		//int numActualRows = 0;
		logService.info(this.getClass().getName() + " Column #: "+ (col+1) + "  " + signalColumn.getHeader() + "  Size of signal = " + numDataPoints);	
			
		//"Entire signal", "Subsequent boxes", "Gliding box" 
		//********************************************************************************************************
		if (analysisType.equals("Entire signal")){	//only this option is possible for FFT
			
			if (!surrType.equals("No surrogates")) {
				Surrogate surrogate = new Surrogate();	
				//choices = {"No surrogates", "Shuffle", "Gaussian", "Random phase", "AAFT"}, 
				if (surrType.equals("Shuffle"))      signal1D = surrogate.calcSurrogateShuffle(signal1D);
				if (surrType.equals("Gaussian"))     signal1D = surrogate.calcSurrogateGaussian(signal1D);
				if (surrType.equals("Random phase")) signal1D = surrogate.calcSurrogateRandomPhase(signal1D);
				if (surrType.equals("AAFT"))         signal1D = surrogate.calcSurrogateAAFT(signal1D);
			}
			
			//logService.info(this.getClass().getName() + " Column #: "+ (col+1) + "  " + signalColumn.getHeader() + "  Size of signal = " + signal1D.length);	
			
			Img<UnsignedByteType> img = null;
			RandomAccess<UnsignedByteType> randomAccess = null;
			
			Img<UnsignedByteType> imgRGB = null;
			RandomAccess<UnsignedByteType> randomAccessRGB = null;
			
			Img<UnsignedByteType> imgBig = null;
			RandomAccess<UnsignedByteType> randomAccessBig = null;
			
			Img<UnsignedByteType> imgBigRGB = null;
			RandomAccess<UnsignedByteType> randomAccessBigRGB = null;
			
			//normalization to mean = 0 and variance = 1
			double mean   = this.calcMean(signal1D);
			double stdDev = this.calcStandardDeviation(signal1D);
			
			for (int i = 0; i < numDataPoints; i++) {
				signal1D[i] = (signal1D[i] - mean) / stdDev;
			}
			//System.out.println("SignalSymbolicAggregation: mean: " + this.calcMean(signal1D));
			//System.out.println("SignalSymbolicAggregation: stDev: "+ this.calcStandardDeviation(signal1D));	
			//calculate aggregated signal
			int numInt = aggLength;  //number of data points for interpolation				
			ArrayList<Double> signalAggregated = new ArrayList<Double>();
			for (int i = 0; i <= signal1D.length-numInt; i = i+numInt){
				double sum = 0.0;
				for (int ii = i; ii < i + numInt; ii++){
					sum += signal1D[ii];
				}
				sum = sum/numInt;
				signalAggregated.add(sum);
			}
			
			//convert signal to symbolic representation (symbols of strings a,b,c,d,....)
			//Vector<String> signalSymbols = new Vector<String>();
			signalString = new String();
			double[] bpLUT    = this.getBreakpointsLUT(alphabetSize);
			String[] alphabet = this.getAlphabet(alphabetSize);
			for (int i = 0; i < signalAggregated.size(); i++){
				String string =  "a";
				for (int bp = 0; bp < bpLUT.length; bp++){
					if (signalAggregated.get(i) > bpLUT[bp]) string = alphabet[bp+1];	
				}
				signalString = signalString + string;	
			}
				
			//prepare matrices
			if (alphabetSize == 4){		
				//create LookUp Matrix
				LUMatrix = new String[(int) Math.sqrt(alphabetSize)][(int) Math.sqrt(alphabetSize)];
				alphabet = this.getAlphabet(alphabetSize);
				LUMatrix[0][0] = alphabet[0]; //"a"
				LUMatrix[0][1] = alphabet[1]; //"b"
				LUMatrix[1][0] = alphabet[2]; //"c"
				LUMatrix[1][1] = alphabet[3]; //"d"
				
				int n = 1;
				while (n < subWordLength){ //create levels
					String[][] matrix1 = new String[LUMatrix.length][LUMatrix[0].length];
					String[][] matrix2 = new String[LUMatrix.length][LUMatrix[0].length];
					String[][] matrix3 = new String[LUMatrix.length][LUMatrix[0].length];
					String[][] matrix4 = new String[LUMatrix.length][LUMatrix[0].length];
					
					for (int i = 0; i < LUMatrix.length; i++){
						for (int j = 0; j < LUMatrix[0].length; j++){
							matrix1[i][j] = LUMatrix[i][j];
							matrix2[i][j] = LUMatrix[i][j];
							matrix3[i][j] = LUMatrix[i][j];
							matrix4[i][j] = LUMatrix[i][j];
						}
					}	
							
					for (int i = 0; i < LUMatrix.length; i++){
						for (int j = 0; j < LUMatrix[0].length; j++){
							matrix1[i][j] = alphabet[0] + matrix1[i][j];
							matrix2[i][j] = alphabet[1] + matrix2[i][j];
							matrix3[i][j] = alphabet[2] + matrix3[i][j];
							matrix4[i][j] = alphabet[3] + matrix4[i][j];
						}
					}
					LUMatrix = new String[LUMatrix.length * 2][LUMatrix[0].length * 2];
					//paste to four quadrants
					for (int i = 0; i < matrix1.length; i++){
						for (int j = 0; j < matrix1[0].length; j++){
							LUMatrix[i][j] = matrix1[i][j];	
						}
					}
					for (int i = 0; i < matrix2.length; i++){
						for (int j = 0; j < matrix2[0].length; j++){
							LUMatrix[i][j + matrix2[0].length] = matrix2[i][j];	
						}
					}
					for (int i = 0; i < matrix3.length; i++){
						for (int j = 0; j < matrix3[0].length; j++){
							LUMatrix[i + matrix3[0].length][j] = matrix3[i][j];	
						}
					}
					for (int i = 0; i < matrix4.length; i++){
						for (int j = 0; j < matrix4[0].length; j++){
							LUMatrix[i +  matrix4.length][j +  matrix4[0].length] = matrix4[i][j];	
						}
					}		
					n = n + 1;
				}			
				
				//create new image					
			    int matrixSize = (int) Math.sqrt(alphabetSize) * (int) Math.pow(2, subWordLength-1);
				//System.out.println("PlotOpSymbolicAggregation: matrixSize: " + matrixSize);
				//System.out.println("PlotOpSymbolicAggregation: LUMatrix.length: " + LUMatrix.length);
				
			    if (matrixSize != LUMatrix.length) {
			    	logService.info(this.getClass().getName() + " Sizes of matrices do not fit!");
			    }
			    
				
				//create empty image
				img = new ArrayImgFactory<>(new UnsignedByteType()).create(matrixSize, matrixSize);
				randomAccess = img.randomAccess();
				
				//look for matches and count them
				
			} // alphabet = 4
			
			
			//scroll through symbol signal, look for subword matches and count them
			for (int s = 0; s < signalString.length() - wordLength; s = s + wordLength ){
				String word = (String) signalString.subSequence(s, s + wordLength);
				//look for matches of subwords
				for (int i = 0; i < LUMatrix.length; i++){		
					for (int j = 0; j < LUMatrix[0].length; j++){
						//if (word.contains(LUMatrix[i][j])) wr.setSample(i, j, 0, wr.getSample(i, j, 0) + 1);
						if (word.contains(LUMatrix[i][j])) {
							randomAccess.setPosition(i, 0);
							randomAccess.setPosition(j, 1);
							randomAccess.get().setReal(randomAccess.get().getRealFloat() + 1f);
						}
					}
				}
			}
			
//			//print results
//			for (int i = 0; i < img.dimension(0); i++){		
//				for (int j = 0; j < img.dimension(1); j++){
//					randomAccess.setPosition(i, 0);
//					randomAccess.setPosition(j, 1);
//					System.out.println("SignalSymbolicAggregation: result "+i+"   "+j+"     " + randomAccess.get().get());
//				}
//			}	
					
			//normalize 
			float max = 0f;
			Cursor<UnsignedByteType> cursorImg = img.localizingCursor();
			//final long[] posImg = new long[img.numDimensions()];
			// Loop through all pixels.
			while (cursorImg.hasNext()) {
				cursorImg.fwd();
				//cursorImg.localize(posImg);
				if (cursorImg.get().get() > max) max = (float)(cursorImg.get().getInteger());
			}
			//System.out.println("SignalSymbolicAggregation: max: " + max);
			cursorImg.reset(); // = img.localizingCursor();
			while (cursorImg.hasNext()) {
				cursorImg.fwd();
				//cursorImg.localize(posImg);
				cursorImg.get().set((int) (Math.round(((float)(cursorImg.get().getInteger())/max) * 255f)));
			}
			
//				//print results
//				for (int i = 0; i < img.dimension(0); i++){		
//					for (int j = 0; j < img.dimension(1); j++){
//						randomAccess.setPosition(i, 0);
//						randomAccess.setPosition(j, 1);
//						System.out.println("SignalSymbolicAggregation: result "+i+"   "+j+"     " + randomAccess.get().get());
//					}
//				}	
				
			if (colorModelType.equals("Grey-8bit")) {
				
				//Change float to byte----------------------------------------------------
				
				//change image size
				//mag;// x scale factor
				//mag;// y scale factor
				//create empty image
				imgBig = new ArrayImgFactory<>(new UnsignedByteType()).create(img.dimension(0)*mag, img.dimension(1)*mag);
				randomAccessBig = imgBig.randomAccess();
				int greyValue;
				for (int y = 0; y < img.dimension(1); y++) {
					for (int x = 0; x < img.dimension(0); x++) {		
						randomAccess.setPosition(x, 0);
						randomAccess.setPosition(y, 1);
						greyValue = randomAccess.get().getInteger();
						
						//copy to larger image
						for (int yy = y*mag; yy < y*mag+mag; yy++) { 
							for (int xx = x*mag; xx < x*mag+mag; xx++) {
								randomAccessBig.setPosition(xx, 0);
								randomAccessBig.setPosition(yy, 1);
								randomAccessBig.get().set(greyValue);
							}
						}	
					}
				}	
					
//				for (int i = 0; i < imgBig.dimension(0); i++){		
//				for (int j = 0; j < imgBig.dimension(1); j++){
//					randomAccessBig.setPosition(i, 0);
//					randomAccessBig.setPosition(j, 1);
//					System.out.println("SignalSymbolicAggregation: result "+i+"   "+j+"     " + randomAccessBig.get().get());
//				}
//				}	
				
				singleImg = imgBig;
				
			} else 	if (colorModelType.equals("Color-RGB")) {
				
				//create empty image
				imgRGB = new ArrayImgFactory<>(new UnsignedByteType()).create(img.dimension(0), img.dimension(1), img.dimension(0));
				randomAccessRGB = imgRGB.randomAccess();
				
				 // Create the R,G,B arrays for the false color image ...
				int red[][]   = new int[(int) img.dimension(0)][(int) img.dimension(1)];
				int green[][] = new int[(int) img.dimension(0)][(int) img.dimension(1)];
				int blue[][]  = new int[(int) img.dimension(0)][(int) img.dimension(1)];
				
				float midSlope   = (float)(255.0/(192.0 - 64.0));
				float leftSlope  = (float)(255.0/64.0);
				float rightSlope = (float)(-255.0/63.0);
				float greyValue;
				int entry = 0;
				for (int y = 0; y < img.dimension(1); y++) {
					for (int x = 0; x < img.dimension(0); x++){
						randomAccess.setPosition(x, 0);
						randomAccess.setPosition(y, 1);
						greyValue = randomAccess.get().getRealFloat();
						// Now the false color assignment ...
						if ( greyValue < 64 ) {
							red  [x][y] = 0;
							green[x][y] = Math.round(leftSlope*greyValue);
							blue [x][y] = 255;
						}
						else if ( ( greyValue >= 64 ) && ( greyValue < 192 ) ){
							red  [x][y] = Math.round(255+midSlope*(greyValue-192));
							green[x][y] = 255;
							blue [x][y] = Math.round(255-midSlope*(greyValue-64));
						}
						else {
							red  [x][y] = 255;
							green[x][y]= Math.round(255+rightSlope*(greyValue-192));
							blue [x][y] = 0;
						}
					}
				}
				// Now create the false color image ...
				for (int y = 0; y < imgRGB.dimension(1); y++) {
					for (int x = 0; x < imgRGB.dimension(0); x++) {		
						randomAccessRGB.setPosition(x, 0);
						randomAccessRGB.setPosition(y, 1);
						randomAccessRGB.setPosition(0, 2);
						randomAccessRGB.get().set(red[x][y]);
						randomAccessRGB.setPosition(1, 2);
						randomAccessRGB.get().set(green[x][y]);
						randomAccessRGB.setPosition(2, 2);
						randomAccessRGB.get().set(blue[x][y]);
					}
				}	
				
				//Change float to byte----------------------------------------------------
				
				//change image size
				//mag;// x scale factor
				//mag;// y scale factor	
				imgBigRGB = new ArrayImgFactory<>(new UnsignedByteType()).create(img.dimension(0)*mag, img.dimension(1)*mag, 3);
				randomAccessBigRGB = imgBigRGB.randomAccess();
				int[] RGBValue = new int[3];
				for (int y = 0; y < img.dimension(1); y++) {
					for (int x = 0; x < img.dimension(0); x++) {		
						randomAccessRGB.setPosition(x, 0);
						randomAccessRGB.setPosition(y, 1);
						randomAccessRGB.setPosition(0, 2);
						RGBValue[0] = randomAccessRGB.get().get();
						randomAccessRGB.setPosition(1, 2);
						RGBValue[1] = randomAccessRGB.get().get();
						randomAccessRGB.setPosition(2, 2);
						RGBValue[2] = randomAccessRGB.get().get();
						
						for (int yy = y*mag; yy < y*mag+mag; yy++) { 
							for (int xx = x*mag; xx < x*mag+mag; xx++) {
								randomAccessBigRGB.setPosition(xx, 0);
								randomAccessBigRGB.setPosition(yy, 1);
								randomAccessBigRGB.setPosition(0, 2);
								randomAccessBigRGB.get().set(RGBValue[0]);
								randomAccessBigRGB.setPosition(1, 2);
								randomAccessBigRGB.get().set(RGBValue[1]);
								randomAccessBigRGB.setPosition(2, 2);
								randomAccessBigRGB.get().set(RGBValue[2]);
							}
						}	
					}
				}			
				singleImg = imgBigRGB;
			}
		
				
		//********************************************************************************************************	
		} else if (analysisType.equals("Subsequent boxes")){ //not for Symbolic aggregation
		
		//********************************************************************************************************			
		} else if (analysisType.equals("Gliding box")){ //not for ymbolic aggregation
		
		}
		
		return singleImg;
		// 
		// Output
		// uiService.show(imageOutName, resultImg);
	}

	
	/**
	 * This method gets back the alphabet 
	 * @param index
	 * @return character
	 */
	private String[] getAlphabet(int alphabetSize) {
		String[] alphabet = new String[alphabetSize];
		for (int i = 0; i < alphabetSize; i++){
			if (i ==  0) alphabet[0]  = "a";
			if (i ==  1) alphabet[1]  = "b";
			if (i ==  2) alphabet[2]  = "c";
			if (i ==  3) alphabet[3]  = "d";
			if (i ==  4) alphabet[4]  = "e";
			if (i ==  5) alphabet[5]  = "f";
			if (i ==  6) alphabet[6]  = "g";
			if (i ==  7) alphabet[7]  = "h";
			if (i ==  8) alphabet[8]  = "i";
			if (i ==  9) alphabet[9]  = "j";
			if (i == 10) alphabet[10] = "k";
			if (i == 11) alphabet[11] = "l";
			if (i == 12) alphabet[12] = "m";
			if (i == 13) alphabet[13] = "n";
			if (i == 14) alphabet[14] = "o";
			if (i == 15) alphabet[15] = "p";
			if (i == 16) alphabet[16] = "q";
			if (i == 17) alphabet[17] = "r";
			if (i == 18) alphabet[18] = "s";
			if (i == 19) alphabet[19] = "t";
			if (i == 20) alphabet[20] = "u";
			if (i == 21) alphabet[21] = "v";
			if (i == 22) alphabet[22] = "w";
			if (i == 23) alphabet[23] = "x";
			if (i == 24) alphabet[24] = "y";
			if (i == 25) alphabet[25] = "z";		
		}
	
		return alphabet;
	}

	/**
	 * This method gets back a LUT of breakpoints (levels) in order to segment a signal into symbols 
	 * @param alphabetSize
	 * @return a double array
	 */
	private double[] getBreakpointsLUT(int alphabetSize) {
		
		double[] breackpointsLUT = new double[alphabetSize -1];
	
		if (alphabetSize == 2){
			breackpointsLUT[0] = 0.0;
		}
		if (alphabetSize == 3){
			breackpointsLUT[0] = -0.43;
			breackpointsLUT[1] =  0.43;
		}
		if (alphabetSize == 4){
			breackpointsLUT[0] = -0.67;
			breackpointsLUT[1] =  0.0;
			breackpointsLUT[2] =  0.67;
		}
		if (alphabetSize == 5){
			breackpointsLUT[0] = -0.84;
			breackpointsLUT[1] = -0.25;
			breackpointsLUT[2] =  0.25;
			breackpointsLUT[3] =  0.84;
		}
		if (alphabetSize == 6){
			breackpointsLUT[0] = -0.97;
			breackpointsLUT[1] = -0.43;
			breackpointsLUT[2] =  0.0;
			breackpointsLUT[3] =  0.43;
			breackpointsLUT[4] =  0.97;
		}
		if (alphabetSize == 7){
			breackpointsLUT[0] = -1.07;
			breackpointsLUT[1] = -0.57;
			breackpointsLUT[2] = -0.18;
			breackpointsLUT[3] =  0.18;
			breackpointsLUT[4] =  0.57;
			breackpointsLUT[5] =  1.07;
		}
		if (alphabetSize == 8){
			breackpointsLUT[0] = -1.15;
			breackpointsLUT[1] = -0.67;
			breackpointsLUT[2] = -0.32;
			breackpointsLUT[3] =  0.0;
			breackpointsLUT[4] =  0.32;
			breackpointsLUT[5] =  0.67;
			breackpointsLUT[6] =  1.15;
		}
		if (alphabetSize == 9){
			breackpointsLUT[0] = -1.22;
			breackpointsLUT[1] = -0.76;
			breackpointsLUT[2] = -0.43;
			breackpointsLUT[3] = -0.14;
			breackpointsLUT[4] =  0.14;
			breackpointsLUT[5] =  0.43;
			breackpointsLUT[6] =  0.76;
			breackpointsLUT[7] =  1.22;
		}
		if (alphabetSize == 10){
			breackpointsLUT[0] = -1.28;
			breackpointsLUT[1] = -0.84;
			breackpointsLUT[2] = -0.52;
			breackpointsLUT[3] = -0.25;
			breackpointsLUT[4] =  0.0;
			breackpointsLUT[5] =  0.25;
			breackpointsLUT[6] =  0.52;
			breackpointsLUT[7] =  0.84;
			breackpointsLUT[8] =  1.28;
		}
		return breackpointsLUT;
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
	
	
	/**
	 * This method calculates the variance of a data series
	 * @param data1D
	 * @return Double Variance
	 */
	@SuppressWarnings("unused")
	private double calcVariance(double[] data1D){
		double mean = calcMean(data1D);
		double sum = 0;
		for(double d: data1D){
			sum = sum + ((d - mean) * (d - mean));
		}
		return sum/(data1D.length-1);  //  1/(n-1) is used by histo.getStandardDeviation() too
	}
	/**
	 * This method calculates the variance of a data series
	 * @param data1D, mean
	 * @return Double Variance
	 */
	private double calcVariance(double[] data1D, double mean){
		double sum = 0;
		for(double d: data1D){
			sum = sum + ((d - mean) * (d - mean));
		}
		return sum/(data1D.length-1);  //  1/(n-1) is used by histo.getStandardDeviation() too
	}
	/**
	 * This method calculates the standard deviation of a data series
	 * @param data1D
	 * @return Double standard deviation
	 */
	private double calcStandardDeviation(double[] data1D){
		double mean = this.calcMean(data1D);
		double variance  = this.calcVariance(data1D, mean);
		return Math.sqrt(variance);
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
		ij.command().run(SignalSymbolicAggregation.class, true);
	}
}
