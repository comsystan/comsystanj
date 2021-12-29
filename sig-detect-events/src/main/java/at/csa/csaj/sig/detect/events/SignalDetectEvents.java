/*-
 * #%L
 * Project: ImageJ2 signal plugin for detecting events
 * File: SignalDetectEvents.java
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

package at.csa.csaj.sig.detect.events;

import java.awt.Point;
import java.awt.Toolkit;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
import org.scijava.table.Column;
import org.scijava.table.DefaultGenericTable;
import org.scijava.table.DefaultTableDisplay;
import org.scijava.table.DoubleColumn;
import org.scijava.table.GenericColumn;
import org.scijava.ui.UIService;
import org.scijava.widget.Button;
import org.scijava.widget.ChoiceWidget;
import org.scijava.widget.NumberWidget;
import at.csa.csaj.commons.signal.algorithms.Surrogate;
import at.csa.csaj.commons.dialog.WaitingDialogWithProgressBar;
import at.csa.csaj.commons.plot.SignalPlotFrame;
import at.csa.csaj.sig.detect.events.util.osea4java.OSEAFactory;
import at.csa.csaj.sig.detect.events.util.osea4java.QRSDetector;
import at.csa.csaj.sig.detect.events.util.osea4java.QRSDetector2;
import at.csa.csaj.sig.detect.events.util.osea4java.classification.BeatDetectionAndClassification;
import at.csa.csaj.sig.detect.events.util.osea4java.classification.BeatDetectionAndClassification.BeatDetectAndClassifyResult;
import at.csa.csaj.sig.detect.events.util.osea4java.classification.ECGCODES;
import at.csa.csaj.sig.open.SignalOpener;


/**
 * A {@link Command} plugin for <detecting events</a>
 * in a signal.
 */
@Plugin(type = ContextCommand.class,
	headless = true,
	label = "Event detection",
	initializer = "initialPluginLaunch",
	//iconPath = "/images/comsystan-??.png", //Menu entry icon
	menu = {
	@Menu(label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT, mnemonic = MenuConstants.PLUGINS_MNEMONIC),
	@Menu(label = "ComsystanJ"),
	@Menu(label = "Signal"),
	@Menu(label = "Event detection", weight = 11)})
//public class SignalDetectEvents<T extends RealType<T>> extends InteractiveCommand { // non blocking  GUI
public class SignalDetectEvents<T extends RealType<T>> extends ContextCommand implements Previewable { //modal GUI with cancel

	private static final String PLUGIN_LABEL                = "<html><b>Event detection</b></html>";
	private static final String SPACE_LABEL                 = "";
	private static final String DETECTIONOPTIONS_LABEL      = "<html><b>Event detection options</b></html>";
	private static final String ANALYSISOPTIONS_LABEL       = "<html><b>Analysis options</b></html>";
	private static final String BACKGROUNDOPTIONS_LABEL     = "<html><b>Background option</b></html>";
	private static final String DISPLAYOPTIONS_LABEL        = "<html><b>Display option</b></html>";
	private static final String PROCESSOPTIONS_LABEL        = "<html><b>Process options</b></html>";
	
	private static double[] signal1D;
	private static double[] domain1D;
	private static double[] subSignal1D;
	private static double[] surrSignal1D;
	Column<? extends Object> signalColumn;
	Column<? extends Object> domainColumn;
	
	private static String tableInName;
	private static String[] sliceLabels;
	private static long numColumns = 0;
	private static long numRows = 0;
//	private static int  numSurrogates = 0;
//	private static int  numBoxLength = 0;
//	private static long numSubsequentBoxes = 0;
//	private static long numGlidingBoxes = 0;
	
	private static double[] domainNew; //This holds a copy of detectedDomain for displaying (plotting)
	private static double[] eventX2; //This holds a copy of dataX2 for displaying (plotting)
	private static double[] eventY2; //This holds a copy of dataX2 for displaying (plotting)

	//Output variables
	private static ArrayList<Double> detectedEvents = new ArrayList<Double>();
	private static ArrayList<Double> detectedDomain = new ArrayList<Double>();
	private static ArrayList<Integer> eventDataIdx    = new ArrayList<Integer>(); //Integer!
	//private static ArrayList<Double> coordinateX    = new ArrayList<Double>();
	//private static ArrayList<Double> intervals      = new ArrayList<Double>();
	//private static ArrayList<Double> heights        = new ArrayList<Double>();
	//private static ArrayList<Double> deltaHeights   = new ArrayList<Double>();
	//private static ArrayList<Double> energies       = new ArrayList<Double>();
	private static ArrayList<Double> eventDataX2    = new ArrayList<Double>(); //events and for displaying points in extra frame
	private static ArrayList<Double> eventDataY2    = new ArrayList<Double>(); //events and for displaying points in extra frame
	
	private static final int numTableOutPreCols = 1; //Number of columns before data (signal) columns, see methods generateTableHeader() and writeToTable()
	private static ArrayList<SignalPlotFrame> displayList = new ArrayList<SignalPlotFrame>();
	private static final String tableOutName = "Table - Detect events";
	
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

//	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
//	private final String labelSpace = SPACE_LABEL;
	
	@Parameter(type = ItemIO.INPUT)
	private DefaultTableDisplay  defaultTableDisplay;

	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelDetectionOptions = DETECTIONOPTIONS_LABEL;
	
	@Parameter(label = "Event type",
			description = "Event type",
			style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
			choices = {"Peaks", "Valleys", "Slope", "QRS peaks (Chen&Chen)", "QRS peaks (Osea)"},
			//persist  = false,  //restore previous value default = true
			initializer = "initialEventType",
			callback = "callbackEventType")
	private String choiceRadioButt_EventType;
	
	@Parameter(label = "Threshold type",
			description = "Threshold type",
			style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
			choices = {"(Peaks, Valleys, Slope) Threshold", "(Slope) MAC"}, //Simple threshold or Moving Average Curves (MACs) according to Lu et al., Med. Phys. 33, 3634 (2006); http://dx.doi.org/10.1118/1"
			//persist  = false,  //restore previous value default = true
			initializer = "initialThresholdType",
			callback = "callbackThresholdType")
	private String choiceRadioButt_ThresholdType;
	
	@Parameter(label = "(Threshold) Threshold", description = "Threshold", style = NumberWidget.SPINNER_STYLE, 
			   min = "-9999999999999999999", max = "9999999999999999999", stepSize = "1",
			   persist = false, // restore  previous value  default  =  true
			   initializer = "initialThreshold", callback = "callbackThreshold")
	private float spinnerFloat_Threshold;
	
	@Parameter(label = "Estimate Tau", callback = "callbackEstimateTau")
	private Button buttonEstimateTau;
	
	@Parameter(label = "(MAC) Tau", description = "Tau", style = NumberWidget.SPINNER_STYLE, 
			   min = "1", max = "9999999999999999999", stepSize = "1",
			   persist = false, // restore  previous value  default  =  true
			   initializer = "initialTau", callback = "callbackTau")
	private int spinnerInteger_Tau;
	
	@Parameter(label = "(MAC) Offset", description = "Offset", style = NumberWidget.SPINNER_STYLE, 
			   min = "0", max = "9999999999999999999", stepSize = "1",
			   persist = false, // restore  previous value  default  =  true
			   initializer = "initialOffset", callback = "callbackOffset")
	private float spinnerFloat_Offset;
	
	@Parameter(label = "(Slope) Slope type",
			description = "Slope type",
			style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
			choices = {"Positive", "Negative"}, 
			//persist  = false,  //restore previous value default = true
			initializer = "initialSlopeType",
			callback = "callbackSlopeType")
	private String choiceRadioButt_SlopeType;
	
	@Parameter(label = "(Chen&Chen) M", description = "Chen&Chen highpass filter parameter, usually set to 3,5,7,9...\")", style = NumberWidget.SPINNER_STYLE, 
			   min = "1", max = "9999999999999999999", stepSize = "2",
			   persist = false, // restore  previous value  default  =  true
			   initializer = "initialChenM", callback = "callbackChenM")
	private int spinnerInteger_ChenM;
	
	@Parameter(label = "(Chen&Chen) Sum interval", description = "Chen&Chen lowpass filter parameter, usually set to 10,20,30,40,50...", style = NumberWidget.SPINNER_STYLE, 
			   min = "1", max = "9999999999999999999", stepSize = "10",
			   persist = false, // restore  previous value  default  =  true
			   initializer = "initialSumInterval", callback = "callbackSumInterval")
	private int spinnerInteger_SumInterval;
	
	@Parameter(label = "(Chen&Chen) Peak frame", description = "Chen&Chen frame for peak parameter, usually set to 100, 150, 200, 250, 300,...", style = NumberWidget.SPINNER_STYLE, 
			   min = "1", max = "9999999999999999999", stepSize = "50",
			   persist = false, // restore  previous value  default  =  true
			   initializer = "initialPeakFrame", callback = "callbackPeakFrame")
	private int spinnerInteger_PeakFrame;
	
	@Parameter(label = "(Osea) Osea method",
			description = "Osea method",
			style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
			choices = {"QRSDetect", "QRSDetect2", "BeatDetectAndClassify"}, //QRSDetect using medians, RSDetect2 using means, BeatDetectionAndClassify using QRSDetect2 for detection
			//persist  = false,  //restore previous value default = true
			initializer = "initialOseaMethod",
			callback = "callbackOseaMethod")
	private String choiceRadioButt_OseaMethod;
	
	@Parameter(label = "(Osea) Sample rate (Hz)", description = "Sample rate of singla in Hz", style = NumberWidget.SPINNER_STYLE, 
			   min = "1", max = "9999999999999999999", stepSize = "1",
			   persist = false, // restore  previous value  default  =  true
			   initializer = "initialSampleRate", callback = "callbackSampleRate")
	private int spinnerInteger_SampleRate;
		
	@Parameter(label = "Output type",
			description = "Output type",
			style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
			choices = {"(Peaks, Valleys, Slope, Chen&Chen, Osea) Event domain values", "(Peaks, Valleys, Slope, Chen&Chen, Osea) Event values", 
					"(Peaks, Valleys, Slope, Chen&Chen, Osea) Intervals", "(Peaks, Valleys) Heights", "(Peaks, Valleys) Energies", "(Peaks, Valleys) delta Heights"}, 
			//persist  = false,  //restore previous value default = true
			initializer = "initialOutputType",
			callback = "callbackOutputType")
	private String choiceRadioButt_OutputType;
	

	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelAnalysisOptions = ANALYSISOPTIONS_LABEL;

	@Parameter(label = "Signal range",
			description = "Entire signal, Subsequent boxes or Gliding box",
			style = ChoiceWidget.LIST_BOX_STYLE,
			choices = {"Entire signal"}, //, "Subsequent boxes", "Gliding box"}, 
			//persist  = false,  //restore previous value default = true
			initializer = "initialSignalRange",
			callback = "callbackSignalRange")
	private String choiceRadioButt_SignalRange;
	
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
	
	@Parameter(label = "First data column is the domain", persist = false,
			callback = "callbackFirstColIsDomain")
    private boolean booleanFirstColIsDomain;
	
	@Parameter(label = "Subtract mean", persist = false,
			callback = "callbackSubtractMean")
    private boolean booleanSubtractMean;
	
	@Parameter(label = "Signal scaling factor", description = "This factor is multiplied to the signal values. Usefull for small signal levels (e.g. Osea uses int values!)", style = NumberWidget.SPINNER_STYLE, 
			   min = "-9999999999999999999", max = "9999999999999999999", stepSize = "1",
			   persist = false, // restore  previous value  default  =  true
			   initializer = "initialSignalScalingFactor", callback = "callbackSignalScalingFactor")
	private float spinnerFloat_SignalScalingFactor;
	
	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelDisplayOptions = DISPLAYOPTIONS_LABEL;

	@Parameter(label = "Display detected events on original signal",
			   // persist = false, //restore previous value default = true
			   initializer = "initialDisplayOnOriginalSignal")
	private boolean booleanDisplayOnOriginalSignal;
	
	@Parameter(label = "Display detected events as a signal",
			   // persist = false, //restore previous value default = true
			   initializer = "initialDisplayAsSignal")
	private boolean booleanDisplayAsSignal;
	
	@Parameter(label = "Overwrite result display(s)",
	    	description = "Overwrite already existing result images, plots or tables",
	    	//persist  = false,  //restore previous value default = true
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
	
	protected void initialEventType() {
		choiceRadioButt_EventType = "Peaks";
	} 
	
	protected void initialThresholdType() {
		choiceRadioButt_ThresholdType = "Threshold";
	} 
	
	protected void initialThreshold() {
		spinnerFloat_Threshold = 1f;
	}
	
	protected void initialTau() {
		spinnerInteger_Tau = 1;
	}
	
	protected void initialOffset() {
		spinnerFloat_Offset = 0f;
	}
	
	protected void initialSlopeType() {
		choiceRadioButt_SlopeType = "Positive";
	} 
	
	protected void initialChenM() {
		spinnerInteger_ChenM = 5;
	}
	
	protected void initialSumInterval() {
		spinnerInteger_SumInterval = 30;
	}
	
	protected void initialPeakFrame() {
		spinnerInteger_PeakFrame = 250;
	}
	
	protected void initialOseaMethod() {
		choiceRadioButt_OseaMethod = "QRSDetect";
	} 
	
	protected void initialSampleRate() {
		spinnerInteger_SampleRate = 100;
	}
	
	protected void initialOutputType() {
		choiceRadioButt_OutputType = "Intervals";
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
	
//	protected void initialRemoveZeroes() {
//		booleanRemoveZeroes = false;
//	}	
	
	protected void initialFirstColIsDomain() {
		booleanFirstColIsDomain = false;
	}	
	
	protected void initialSubtractMean() {
		booleanSubtractMean = false;
	}	

	protected void initialSignalScalingFactor() {
		 spinnerFloat_SignalScalingFactor = 1;
	}
	
	protected void initialDisplayOnOriginalSignal() {
		booleanDisplayOnOriginalSignal = false;
	}
	
	protected void initialDisplayAsSignal() {
		booleanDisplayAsSignal = false;
	}
	
	protected void initialOverwriteDisplays() {
    	booleanOverwriteDisplays = true;
	}
	
	protected void initialNumColumn() {
		spinnerInteger_NumColumn = 1;
		if (spinnerInteger_NumColumn < numColumns) {
			signalColumn = tableIn.get(this.spinnerInteger_NumColumn - 1);
			signal1D = new double[signalColumn.size()];
			for (int n = 0; n < signalColumn.size(); n++) {
				signal1D[n] = Double.valueOf((Double)signalColumn.get(n));
			}	
		}
	}

	// ------------------------------------------------------------------------------
	
	
	/** Executed whenever the {@link #choiceRadioButt_EventType} parameter changes. */
	protected void callbackEventType() {
		logService.info(this.getClass().getName() + " Event type set to " + choiceRadioButt_EventType);
	}
	
	/** Executed whenever the {@link #choiceRadioButt_ThresholdType} parameter changes. */
	protected void callbackThresholdType() {
		logService.info(this.getClass().getName() + " Threshold type set to " + choiceRadioButt_ThresholdType);
	}
	
	/** Executed whenever the {@link #spinFloat_Threshold} parameter changes. */
	protected void callbackThreshold() {
		logService.info(this.getClass().getName() + " Threshold set to " + spinnerFloat_Threshold);
	}
	
	/**
	 * Executed whenever the {@link #buttonEstimTau} button is pressed.
	 */
	protected void callbackEstimateTau() {
		//estimation of Tau
		//eliminate mean value in order to suppress the DC component in the power spectrum
		// not needed if the DC component of the power spectrum is simply not taken later on
//		double sum = 0.0;
//		for (int i = 0; i < signal1D.length; i++) {
//			sum = sum + signal1D[i];
//		}
//		double mean = sum / signal1D.length;
//		for (int i = 0; i < signal1D.length; i++) {
//			signal1D[i] = signal1D[i] - mean;
//		}
//		logService.info(this.getClass().getName() + "  Mean value of signal: " + mean);
//		logService.info(this.getClass().getName() + "  Mean value subtracted from signal in order to computer Tau");

		//estimate Tau with FFT
		//Oppenheim & Schafer, DiscreteTimeSignalProcessing-ed3-2010 p.854
		
		// data length must have a power of 2
		int powerSize = 1;
		while (signal1D.length > powerSize) {
			powerSize = powerSize * 2;
		}
		double[] data = new double[powerSize];
		// set data
		if (powerSize <= signal1D.length) { //
			for (int i = 0; i < powerSize; i++) {
				data[i] = signal1D[i];
			}
		} else {
			for (int i = 0; i < signal1D.length; i++) {
				data[i] = signal1D[i];
			}
			for (int i = signal1D.length; i < powerSize; i++) {
				data[i] = 0.0d;
			}
		}	
		
		//signalOut = new double[numDataPoints];
		//rangeOut  = new double[numDataPoints];
		
	    //Assumes n is even.
		DftNormalization normalization = null;
		String normType = "Standard";
		if (normType.equals("Standard")) normalization = DftNormalization.STANDARD;
		if (normType.equals("Unitary"))  normalization = DftNormalization.UNITARY;
		FastFourierTransformer transformer = new FastFourierTransformer(normalization);
	
		Complex[] complx  = transformer.transform(data, TransformType.FORWARD);
		
		//Magnitude or power spectrum
		double[] fft = new double[complx.length/2];
		String outType = "Power";
		if (outType.equals("Power")) {
			for (int i = 0; i < complx.length/2; i++) {               
				//signalOut[i] = complx[i].getReal()*complx[i].getReal() + complx[i].getImaginary()*complx[i].getImaginary(); //Power spectrum
				fft[i] = complx[i].abs()*complx[i].abs();
			}	
		} else 	if (outType.equals("Magnitude")) {
			for (int i = 0; i < complx.length/2; i++) {               
				//signalOut[i] = Math.sqrt(complx[i].getReal()*complx[i].getReal() + complx[i].getImaginary()*complx[i].getImaginary()); //Magnitude
				fft[i] = complx[i].abs();
			}	
		}
		
		//output scaling
//		String scalingType = "Linear";
//		if (scalingType.equals("Log")){
//			double value = Double.NaN;
//			for (int i = 0; i < signalOut.length; i++) {
//				value = signalOut[i];
//				if (value == 0.0) value = Double.MIN_VALUE; 
//				signalOut[i] = Math.log10(value);
//			}
//		} else if (scalingType.equals("Ln")){
//			double value = Double.NaN;
//			for (int i = 0; i < signalOut.length; i++) {
//				value = signalOut[i];
//				if (value == 0.0) value = Double.MIN_VALUE; 
//				signalOut[i] = Math.log(value);
//			}
//		} else if (scalingType.equals("Linear")){
//			//Do nothing
//		}
		
		//generate time domain axis
//		double[] timeDomain = new double[complx.length/2];
//		String timeDomainType = "Unitary";
//		if (timeDomainType.equals("Unitary")) {
//			for (int n = 0; n < signalOut.length; n++) {
//				domain1D[n]  = n+1;
//			}	
//		} else if (timeDomainType.equals("Hz")) {
//			for (int f = 0; f < signalOut.length; f++) {
//				domain1D[f]  = (double)f*this.spinnerInteger_SampleRate/(signalOut.length*2 - 2);
//			}	
//		}
		
		//find maximum of the power spectrum
		double psMax = -Double.MAX_VALUE;
		double frequ = -1.0;
		int numK = fft.length;
		for (int i = 1; i < fft.length; i++) { //0..DC component not included

			if (fft[i] > psMax) {
				psMax = fft[i];
				frequ = i + 1;
			}
		}
		this.spinnerInteger_Tau = (int) Math.round(1.0 / frequ * numK * 2);
		callbackTau();
	}
	
	/** Executed whenever the {@link #spinnerInteger_Tau} parameter changes. */
	protected void callbackTau() {
		logService.info(this.getClass().getName() + " Tau set to " + spinnerInteger_Tau);
	}
	
	/** Executed whenever the {@link #spinFloat_Offset} parameter changes. */
	protected void callbackOffset() {
		logService.info(this.getClass().getName() + " Offset set to " + spinnerFloat_Offset);
	}
	
	/** Executed whenever the {@link #choiceRadioButt_SlopeType} parameter changes. */
	protected void callbackSlopeType() {
		logService.info(this.getClass().getName() + " Slope type set to " + choiceRadioButt_SlopeType);
	}
	
	/** Executed whenever the {@link #spinnerInteger_ChenM} parameter changes. */
	protected void callbackChenM() {
		logService.info(this.getClass().getName() + " Chen M set to " + spinnerInteger_ChenM);
	}
	
	/** Executed whenever the {@link #spinnerInteger_SumInterval} parameter changes. */
	protected void callbackSumInterval() {
		logService.info(this.getClass().getName() + " Sum interval set to " + spinnerInteger_SumInterval);
	}
	
	/** Executed whenever the {@link #spinnerInteger_PeakFrame} parameter changes. */
	protected void callbackPeakFrame() {
		logService.info(this.getClass().getName() + " Peak frame set to " + spinnerInteger_PeakFrame);
	}
	
	/** Executed whenever the {@link #choiceRadioButt_OseaMethod} parameter changes. */
	protected void callbackOseaMethod() {
		logService.info(this.getClass().getName() + " Osea method set to " + choiceRadioButt_OseaMethod);
	}
	
	/** Executed whenever the {@link #spinnerInteger_SampleRate} parameter changes. */
	protected void callbackSampleRate() {
		logService.info(this.getClass().getName() + " Sample rate set to " + spinnerInteger_SampleRate);
	}
	
	/** Executed whenever the {@link #choiceRadioButt_OutputType} parameter changes. */
	protected void callbackOutputType() {
		logService.info(this.getClass().getName() + " Output type set to " + choiceRadioButt_OutputType);
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

//	/** Executed whenever the {@link #booleanRemoveZeroes} parameter changes. */
//	protected void callbackRemoveZeroes() {
//		logService.info(this.getClass().getName() + " Remove zeroes set to " + booleanRemoveZeroes);
//	}
	
	/** Executed whenever the {@link #booleanFirstColIsDomain} parameter changes. */
	protected void callbackFirstColIsDomain() {
		logService.info(this.getClass().getName() + " First Column is the domain set to " + booleanFirstColIsDomain);
	}
	
	/** Executed whenever the {@link #booleanSubtractMean} parameter changes. */
	protected void callbackSubtractMean() {
		logService.info(this.getClass().getName() + " Subtract mean set to " + booleanSubtractMean);
	}
	
	/** Executed whenever the {@link #spinnerFloat_SignalScalingFactor} parameter changes. */
	protected void callbackSignalScalingFactor() {
		logService.info(this.getClass().getName() + " Signal scaling factor set to " + spinnerFloat_SignalScalingFactor);
	}

	/** Executed whenever the {@link #booleanDisplayOnOriginalSignal} parameter changes. */
	protected void callbackDisplayOnOriginalSignal() {
		logService.info(this.getClass().getName() + " Display events on original signal set to " + booleanDisplayOnOriginalSignal);
	}
	
	/** Executed whenever the {@link #booleanDisplayAsSignal} parameter changes. */
	protected void callbackDisplayAsSignal() {
		logService.info(this.getClass().getName() + " Display events as signal set to " + booleanDisplayAsSignal);
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
		if (this.spinnerInteger_NumColumn < numColumns) {
			signalColumn = tableIn.get(this.spinnerInteger_NumColumn - 1);
			signal1D = new double[signalColumn.size()];
			for (int n = 0; n < signalColumn.size(); n++) {
				signal1D[n] = Double.valueOf((Double)signalColumn.get(n));
			}	
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
			
//		sliceLabels = new String[(int) numColumns];
				   
		logService.info(this.getClass().getName() + " Name: "      + tableInName); 
		logService.info(this.getClass().getName() + " Columns #: " + numColumns);
		logService.info(this.getClass().getName() + " Rows #: "    + numRows); 
	}

	/**
	* This method starts the workflow for a single column of the active display
	*/
	protected void startWorkflowForSingleColumn() {
	
		dlgProgress = new WaitingDialogWithProgressBar("Searching for events, please wait... Open console window for further info.",
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
			dlgProgress = new WaitingDialogWithProgressBar("Searching for events, please wait... Open console window for further info.",
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
		tableOut.add(new GenericColumn("Options"));

		//First add all columns
		String preString = "Events";
		for (int c = 0; c < numColumns; c++) {
			tableOut.add(new DoubleColumn(preString+"-" + tableIn.getColumnHeader(c)));
		}
		
		//Then append rows (do not add columns after that)
		//row number of output rows is not available, because it is dependent on the search result
		//may be set in writeToTable method 
		//tableOut.appendRows((int) numRows);
		
		//set Options data
		tableOut.appendRow();
		tableOut.set(0, tableOut.getRowCount()-1, "Event type: "     + this.choiceRadioButt_EventType);
		
		//"Peaks", "Valleys", "Slope", "QRS peaks (Chen&Chen)", "QRS peaks (Osea)"},	
		if (this.choiceRadioButt_EventType.equals("Peaks")) {
			if (this.choiceRadioButt_ThresholdType.equals("(Peaks, Valleys, Slope) Threshold")) {
				tableOut.appendRow();
				tableOut.set(0, tableOut.getRowCount()-1, "Threshold type: Threshold");
				tableOut.appendRow();
				tableOut.set(0, tableOut.getRowCount()-1, "Threshold: "  + this.spinnerFloat_Threshold);
			}	
		}	
		else if (this.choiceRadioButt_EventType.equals("Valleys")) {		
			if (this.choiceRadioButt_ThresholdType.equals("(Peaks, Valleys, Slope) Threshold")) {
				tableOut.appendRow();
				tableOut.set(0, tableOut.getRowCount()-1, "Threshold type: Threshold");
				tableOut.appendRow();
				tableOut.set(0, tableOut.getRowCount()-1, "Threshold: "  + this.spinnerFloat_Threshold);
			}
		}
		else if (this.choiceRadioButt_EventType.equals("Slope")) {// Thresholding or MAC)
			if (this.choiceRadioButt_ThresholdType.equals("(Peaks, Valleys, Slope) Threshold")) {
				tableOut.appendRow();
				tableOut.set(0, tableOut.getRowCount()-1, "Threshold type: Threshold");
				tableOut.appendRow();
				tableOut.set(0, tableOut.getRowCount()-1, "Threshold: "  + this.spinnerFloat_Threshold);
			}
			else if (this.choiceRadioButt_ThresholdType.equals("(Slope) MAC")) {
				tableOut.appendRow();
				tableOut.set(0, tableOut.getRowCount()-1, "Threshold type: MAC");
				tableOut.appendRow();
				tableOut.set(0, tableOut.getRowCount()-1, "Tau: "            + this.spinnerInteger_Tau);
				tableOut.appendRow();
				tableOut.set(0, tableOut.getRowCount()-1, "Offset: "         + this.spinnerFloat_Offset);
			}	
			tableOut.appendRow();
			tableOut.set(0, tableOut.getRowCount()-1, "Slope type: "     + this.choiceRadioButt_SlopeType);
		}	
		else if (this.choiceRadioButt_EventType.equals("QRS peaks (Chen&Chen)")) {		
			tableOut.appendRow();
			tableOut.set(0, tableOut.getRowCount()-1, "Chen m: "         + this.spinnerInteger_ChenM);
			tableOut.appendRow();
			tableOut.set(0, tableOut.getRowCount()-1, "Sum interval: "   + this.spinnerInteger_SumInterval);
			tableOut.appendRow();
			tableOut.set(0, tableOut.getRowCount()-1, "Peak frame: "     + this.spinnerInteger_PeakFrame);
		}
		else if (this.choiceRadioButt_EventType.equals("QRS peaks (Osea)")) {		
			tableOut.appendRow();
			tableOut.set(0, tableOut.getRowCount()-1, "Chen m: "         + this.spinnerInteger_ChenM);
			tableOut.appendRow();
			tableOut.set(0, tableOut.getRowCount()-1, "Sum interval: "   + this.spinnerInteger_SumInterval);
			tableOut.appendRow();
			tableOut.set(0, tableOut.getRowCount()-1, "Peak frame: "     + this.spinnerInteger_PeakFrame);		
		}
			
		String outputType = "";
		//"(Peaks, Valleys, Slope, Chen&Chen, Osea) Event domain values", "(Peaks, Valleys, Slope, Chen&Chen, Osea) Event values", "(Peaks, Valleys, Slope, Chen&Chen, Osea) Intervals", "(Peaks, Valleys) Heights", "(Peaks, Valleys) Energies", "(Peaks, Valleys) delta Heights"}, 
		if (choiceRadioButt_OutputType.equals("(Peaks, Valleys, Slope, Chen&Chen, Osea) Event domain values")) {
			outputType = "Output: Event domain values";
		} else if (choiceRadioButt_OutputType.equals("(Peaks, Valleys, Slope, Chen&Chen, Osea) Event values")) {
				outputType = "Output: Event values";
		} else if (choiceRadioButt_OutputType.equals("(Peaks, Valleys, Slope, Chen&Chen, Osea) Intervals")) {
			outputType = "Output: Intervals";
		} else if (choiceRadioButt_OutputType.equals("(Peaks, Valleys) Heights")) {
			outputType = "Output: Heights";
		} else if (choiceRadioButt_OutputType.equals("(Peaks, Valleys) Energies")) {
			outputType = "Output: Energies";
		} else if (choiceRadioButt_OutputType.equals("(Peaks, Valleys) delta Heights")) {
			outputType = "Output: delta Heights";
		} 	
		tableOut.appendRow();
		tableOut.set(0, tableOut.getRowCount()-1, outputType);
		
		tableOut.appendRow();
		tableOut.set(0, tableOut.getRowCount()-1, "Surr type: "      + this.choiceRadioButt_SurrogateType);
		
		tableOut.appendRow();
		tableOut.set(0, tableOut.getRowCount()-1, "First column is the domain: " + this.booleanFirstColIsDomain);
		
		tableOut.appendRow();
		tableOut.set(0, tableOut.getRowCount()-1, "Subtract Mean: " + this.booleanSubtractMean);
		
		tableOut.appendRow();
		tableOut.set(0, tableOut.getRowCount()-1, "Scaling factor: " + this.spinnerFloat_SignalScalingFactor);
	}
	
	
	/**
	 * This method deletes already open displays
	 * 
	 */
	private void deleteExistingDisplays() {
		
		boolean optDeleteExistingPlots    = false;
		boolean optDeleteExistingTables   = false;
		boolean optDeleteExistingImgs     = false;
		boolean optDeleteExistingDisplays = false;
		if (booleanOverwriteDisplays) {
			optDeleteExistingPlots    = true;
			optDeleteExistingTables   = true;
			optDeleteExistingImgs     = true;
			optDeleteExistingDisplays = true;
		}
		
		if (optDeleteExistingDisplays) {
//			//This dose not work with DisplayService because the JFrame is not "registered" as an ImageJ display	
			if (displayList != null) {
				for (int l = 0; l < displayList.size(); l++) {
					displayList.get(l).setVisible(false);
					displayList.get(l).dispose();
					// doubleLogPlotList.remove(l); /
				}
				displayList.clear();
			}
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
	 * This method takes the single column s and computes results. 
	 * @Param int s
	 * */
	private void processSingleInputColumn (int s) {
		
		long startTime = System.currentTimeMillis();
	
		// Compute result values
		process(tableIn, s); 
		// 0 Entropy
		logService.info(this.getClass().getName() + " Processing finished.");
		writeToTable(s, detectedEvents);
		
		//eliminate empty columns
		leaveOverOnlyOneSignalColumn(s+numTableOutPreCols); // +  because of text columns
		
		//int selectedOption = JOptionPane.showConfirmDialog(null, "Do you want to display the result?\nNot recommended for a large number of signals", "Display option", JOptionPane.YES_NO_OPTION); 
		//if (selectedOption == JOptionPane.YES_OPTION) {
		if (booleanDisplayAsSignal) {
			int[] cols = new int[tableOut.getColumnCount()-numTableOutPreCols]; //- because of first text columns	
			boolean isLineVisible = false;
			String signalTitle = "Events - ";			
			// {"(Peaks, Valleys, Slope, Chen&Chen, Osea) Event domain values", "(Peaks, Valleys, Slope, Chen&Chen, Osea) Event values", "(Peaks, Valleys, Slope, Chen&Chen, Osea) Intervals", "(Peaks, Valleys) Heights", "(Peaks, Valleys) Energies", "(Peaks, Valleys) delta Heights"}, 
			if (choiceRadioButt_OutputType.equals("(Peaks, Valleys, Slope, Chen&Chen, Osea) Event domain values")) {
				signalTitle = "Event domain values";
			} else if (choiceRadioButt_OutputType.equals("(Peaks, Valleys, Slope, Chen&Chen, Osea) Event values")) {
				signalTitle = "Event values";
			} else if (choiceRadioButt_OutputType.equals("(Peaks, Valleys, Slope, Chen&Chen, Osea) Intervals")) {
				signalTitle = "Intervals";
			} else if (choiceRadioButt_OutputType.equals("(Peaks, Valleys) Heights")) {
				signalTitle = "Heights";
			} else if (choiceRadioButt_OutputType.equals("(Peaks, Valleys) Energies")) {
				signalTitle = "Energies";
			} else if (choiceRadioButt_OutputType.equals("(Peaks, Valleys) delta Heights")) {
				signalTitle = "delta Heights";
			} 
			String xLabel = "#";
			String yLabel = "Value";
			String[] seriesLabels = new String[tableOut.getColumnCount()-numTableOutPreCols]; //- because of first text columns			
			for (int c = numTableOutPreCols; c < tableOut.getColumnCount(); c++) { //because of first text columns	
				cols[c-numTableOutPreCols] = c; //- because of first text columns	
				seriesLabels[c-numTableOutPreCols] = tableOut.getColumnHeader(c); //- because of first two text columns					
			}
			domainNew = new double[detectedDomain.size()];
			for (int i = 0; i<domainNew.length; i++) {
				domainNew[i] = detectedDomain.get(i).doubleValue(); 
			}
			//display of detected events only 
			SignalPlotFrame pdf = new SignalPlotFrame(domainNew, tableOut, cols, isLineVisible, "Events", signalTitle, xLabel, yLabel, seriesLabels);
			Point pos = pdf.getLocation();
			pos.x = (int) (pos.getX() - 100);
			pos.y = (int) (pos.getY() + 100);
			pdf.setLocation(pos);
			pdf.setVisible(true);
			displayList.add(pdf);
		}
		if (booleanDisplayOnOriginalSignal) {
			boolean isLineVisible = false;
			eventX2 = new double[eventDataX2.size()];
 			eventY2 = new double[eventDataY2.size()]; //should have the same size as X2
			for (int i = 0; i<domainNew.length; i++) {
				eventX2[i] = eventDataX2.get(i).doubleValue();
				eventY2[i] = eventDataY2.get(i).doubleValue(); 
			}
			SignalPlotFrame pdf = new SignalPlotFrame(domain1D, signal1D, eventX2, eventY2, isLineVisible, 
												"Detected events", tableIn.getColumnHeader(s) + " + Events", "Samples [a.u.]", "Values [a.u.]", tableIn.getColumnHeader(s), "Events");
			Point pos = pdf.getLocation();
			pos.x = (int) (pos.getX() - 100);
			pos.y = (int) (pos.getY() + 100);
			pdf.setLocation(pos);
			pdf.setVisible(true);
			displayList.add(pdf);
		}
			
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
	private void leaveOverOnlyOneSignalColumn(int c) {
		String header = tableOut.getColumnHeader(c);
		int numCols = tableOut.getColumnCount();
		for (int i = numCols-1; i >= numTableOutPreCols; i--) {    //leave also first text column(s)
			if (!tableOut.getColumnHeader(i).equals(header))  tableOut.removeColumn(i);	
		}	
	}

	/**
	 * This method loops over all input columns and computes results. 
	 * 
	 * */
	private void processAllInputColumns() {
		
		long startTimeAll = System.currentTimeMillis();
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
				process(tableIn, s);
				writeToTable(s, detectedEvents);
				
				logService.info(this.getClass().getName() + " Processing finished.");
			
				long duration = System.currentTimeMillis() - startTime;
				TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
				SimpleDateFormat sdf = new SimpleDateFormat();
				sdf.applyPattern("HHH:mm:ss:SSS");
				logService.info(this.getClass().getName() + " Elapsed time: "+ sdf.format(duration));
			//}
		} //s	
		statusService.showProgress(0, 100);
		statusService.clearStatus();
		
		//int selectedOption = JOptionPane.showConfirmDialog(null, "Do you want to display the FFT result?\nNot recommended for a large number of signals", "Display option", JOptionPane.YES_NO_OPTION); 
		//if (selectedOption == JOptionPane.YES_OPTION) {
		if (booleanDisplayAsSignal) {
			int[] cols = new int[tableOut.getColumnCount()-numTableOutPreCols]; //- because of first text columns	
			boolean isLineVisible = true;
			String signalTitle = "Events - ";			
			// {"(Peaks, Valleys, Slope, Chen&Chen, Osea) Event domain values", "(Peaks, Valleys, Slope, Chen&Chen, Osea) Event values",  "(Peaks, Valleys, Slope, Chen&Chen, Osea) Intervals", "(Peaks, Valleys) Heights", "(Peaks, Valleys) Energies", "(Peaks, Valleys) delta Heights"}, 
			if (choiceRadioButt_OutputType.equals("(Peaks, Valleys, Slope, Chen&Chen, Osea) Event domain values")) {
				signalTitle = "Events - Domain values";
			} else if (choiceRadioButt_OutputType.equals("(Peaks, Valleys, Slope, Chen&Chen, Osea) Event values")) {
				signalTitle = "Events - Values";
			} else if (choiceRadioButt_OutputType.equals("(Peaks, Valleys, Slope, Chen&Chen, Osea) Intervals")) {
				signalTitle = "Events - Intervals";
			} else if (choiceRadioButt_OutputType.equals("(Peaks, Valleys) Heights")) {
				signalTitle = "Events - Heights";
			} else if (choiceRadioButt_OutputType.equals("(Peaks, Valleys) Energies")) {
				signalTitle = "Events - Energies";
			} else if (choiceRadioButt_OutputType.equals("(Peaks, Valleys) delta Heights")) {
				signalTitle = "Events - delta Heights";
			} 
			String xLabel = "#";
			String yLabel = "Value";
			String[] seriesLabels = new String[tableOut.getColumnCount()-numTableOutPreCols]; //- because of first text columns		
			for (int c = numTableOutPreCols; c < tableOut.getColumnCount(); c++) { //because of first text columns	
				cols[c-numTableOutPreCols] = c;  //- because of first two text columns	
				seriesLabels[c-numTableOutPreCols] = tableOut.getColumnHeader(c);	//-because of first text columns				
			}
			domainNew = new double[tableOut.getRowCount()];
			for (int i = 0; i<domainNew.length; i++) {
				domainNew[i] = i+1; 
			}
			//display of detected events only 
			SignalPlotFrame pdf = new SignalPlotFrame(domainNew, tableOut, cols, isLineVisible, "Events", signalTitle, xLabel, yLabel, seriesLabels);
			Point pos = pdf.getLocation();
			pos.x = (int) (pos.getX() - 100);
			pos.y = (int) (pos.getY() + 100);
			pdf.setLocation(pos);
			pdf.setVisible(true);
			displayList.add(pdf);
		}
		if (booleanDisplayOnOriginalSignal) {

			//TO DO
			
		}
			
		long duration = System.currentTimeMillis() - startTimeAll;
		TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
		SimpleDateFormat sdf = new SimpleDateFormat();
		sdf.applyPattern("HHH:mm:ss:SSS");
		logService.info(this.getClass().getName() + " Elapsed processing time for all signal(s): "+ sdf.format(duration));
	}
	
	/**
	 * collects current result and writes to table
	 * 
	 * @param int column number of active signal.
	 * @param ArrayList<Double> result values
	 */
	private void writeToTable(int signalNumber, ArrayList<Double> resultValues) {
		logService.info(this.getClass().getName() + " Writing to the table...");
			
		if (resultValues != null) {
			int currentNumRows = tableOut.getRowCount();
			if (currentNumRows < resultValues.size()) {
				tableOut.appendRows(resultValues.size() - currentNumRows); //append
				for (int r = currentNumRows; r < tableOut.getRowCount(); r++ ) {		
					for (int c = numTableOutPreCols; c < tableOut.getColumnCount(); c++ ) {	
						tableOut.set(c, r, Double.NaN); //// pre fill with NaNs
					}
				}	
			}		
			for (int r = 0; r < resultValues.size(); r++ ) {		
				//tableOut.set(2, r, domain1D[r]); // time domain column	
				tableOut.set(numTableOutPreCols + signalNumber, r, resultValues.get(r).doubleValue()); //+ because of first text columns	
			}		
			//Fill up with NaNs (this can be because of NaNs in the input signal or deletion of zeroes)
			if (tableOut.getRowCount() > resultValues.size()) {
				for (int r = resultValues.size(); r < tableOut.getRowCount(); r++ ) {
					//tableOut.set(2, r, domain1D[r]); // time domain column	
					tableOut.set(numTableOutPreCols + signalNumber, r, Double.NaN); //+ because of first text columns	
				}
			}
		} else {//resultValues == null
			//overwrite with NaNs
			for (int r = 0; r < tableOut.getRowCount(); r++ ) {
				//tableOut.set(2, r, domain1D[r]); // time domain column	
				tableOut.set(numTableOutPreCols + signalNumber, r, Double.NaN); //+ because of first text columns	
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
	 * @param dgt
	 * @param col
	 */
	private void process(DefaultGenericTable dgt, int col) { //  c column number
	
		if (dgt == null) {
			logService.info(this.getClass().getName() + " WARNING: dgt==null, no signal for processing!");
		}
		
		String  signalRange   = choiceRadioButt_SignalRange;
		String  surrType       = choiceRadioButt_SurrogateType;
		//int     boxLength     = spinnerInteger_BoxLength;
		int     numDataPoints  = dgt.getRowCount();
		//boolean removeZeores  = booleanRemoveZeroes;
	
		String eventType      = choiceRadioButt_EventType; //"Peaks", "Valleys", "Slope", "QRS peaks (Chen&Chen)", "QRS peaks (Osea)"},
		String thresholdType  = choiceRadioButt_ThresholdType; //"(Peaks, Valleys, Slope) Threshold", "(Slope) MAC"}, //Simple threshold or Moving Average Curves (MACs) according to Lu et al., Med. Phys. 33, 3634 (2006); http://dx.doi.org/10.1118/1"
		float  threshold      = spinnerFloat_Threshold;
		int    tau            = spinnerInteger_Tau;
		float  offset         = spinnerFloat_Offset;
		String slopeType      = choiceRadioButt_SlopeType;//Positive, Negative
		int    chenM          = spinnerInteger_ChenM;
		int    sumInterval    = spinnerInteger_SumInterval;
		int    peakFrame      = spinnerInteger_PeakFrame;
		String oseaMethod     = choiceRadioButt_OseaMethod; //"QRSDetect", "QRSDetect2", "BeatDetectAndClassify"}, //QRSDetect using medians, RSDetect2 using means, BeatDetectionAndClassify using QRSDetect2 for detection
		int    sampleRate     = spinnerInteger_SampleRate;
		String outputType     = choiceRadioButt_OutputType;//"(Peaks, Valleys, Slope, Chen&Chen, Osea) Event domain values", "(Peaks, Valleys, Slope, Chen&Chen, Osea) Event values",  "(Peaks, Valleys, Slope, Chen&Chen, Osea) Intervals", "(Peaks, Valleys) Heights", "(Peaks, Valleys) Energies", "(Peaks, Valleys) delta Heights"},
		boolean firstColIsDomain = booleanFirstColIsDomain;
		boolean subtractMean  = booleanSubtractMean;
		float   scalingFactor = spinnerFloat_SignalScalingFactor;
		//******************************************************************************************************
		
		int intersections = 1; // TODO: GUI; for MAC: 1=intervals between intersections of signal and MAC, 0=intervals between maxima/minima

		//clear is not enough
//		detectedDomain.clear();
//		detectedEvents.clear(); 
//		eventDataIdx.clear();
//		eventDataX2.clear();
//		eventDataY2.clear();

		detectedEvents = null;
		detectedDomain = null;
		eventDataIdx   = null;
		eventDataX2    = null;
		eventDataY2    = null;
		
		detectedEvents = new ArrayList<Double>();
		detectedDomain = new ArrayList<Double>();
		eventDataIdx   = new ArrayList<Integer>(); //Integer!
		eventDataX2    = new ArrayList<Double>();
		eventDataY2    = new ArrayList<Double>();
			
		if (booleanFirstColIsDomain) {
			domain1D = new double[numDataPoints];
			signal1D = new double[numDataPoints];
			domainColumn = dgt.get(0);
			signalColumn = dgt.get(col);
			for (int n = 0; n < numDataPoints; n++) {
				domain1D[n] = Double.valueOf((Double)domainColumn.get(n));
				signal1D[n] = Double.valueOf((Double)signalColumn.get(n));
			}	
		} else {
			domain1D = new double[numDataPoints];
			signal1D = new double[numDataPoints];
			signalColumn = dgt.get(col);
			for (int n = 0; n < numDataPoints; n++) {
				domain1D[n] = n+1;
				signal1D[n] = Double.valueOf((Double)signalColumn.get(n));
			}	
		}
		
		if (subtractMean) {
			double sum = 0.0;
			for (double s: signal1D) {
				sum = sum + s;
			}
			double mean = sum / signal1D.length;
			for (int i = 0; i < signal1D.length; i++) {
				signal1D[i] = signal1D[i] - mean;
			}
			logService.info(this.getClass().getName() + " Subtracted mean value " + mean +" from signal");
		}
	
		if (Float.compare(scalingFactor,  1f) != 0) {
			for (int i = 0; i < signal1D.length; i++) {
				signal1D[i] = signal1D[i] * scalingFactor;
			}
			logService.info(this.getClass().getName() + " Signal scaled by " + scalingFactor);
		}
		
		double samplingInterval   = domain1D[1] - domain1D[0];
		
		//signal1D = removeNaN(signal1D);
		//if (removeZeores) signal1D = removeZeroes(signal1D);
		
		//numDataPoints may be smaller now
		numDataPoints = signal1D.length;
			
		
		//int numActualRows = 0;
		logService.info(this.getClass().getName() + " Column #: "+ (col+1) + "  " + signalColumn.getHeader() + "  Size of signal = " + numDataPoints);	
			
		//"Entire signal", "Subsequent boxes", "Gliding box" 
		//********************************************************************************************************
		if (signalRange.equals("Entire signal")){	//only this option is possible for FFT
			
			if (!surrType.equals("No surrogates")) {
				Surrogate surrogate = new Surrogate();	
				//choices = {"No surrogates", "Shuffle", "Gaussian", "Random phase", "AAFT"}, 
				if (surrType.equals("Shuffle"))      signal1D = surrogate.calcSurrogateShuffle(signal1D);
				if (surrType.equals("Gaussian"))     signal1D = surrogate.calcSurrogateGaussian(signal1D);
				if (surrType.equals("Random phase")) signal1D = surrogate.calcSurrogateRandomPhase(signal1D);
				if (surrType.equals("AAFT"))         signal1D = surrogate.calcSurrogateAAFT(signal1D);
			}
			
			//logService.info(this.getClass().getName() + " Column #: "+ (col+1) + "  " + signalColumn.getHeader() + "  Size of signal = " + signal1D.length);	
				
			//"Peaks", "Valleys", "Slope", "QRS peaks (Chen&Chen)", "QRS peaks (Osea)"},	
			if (eventType.equals("Peaks")) {
				this.lookingForPeakPoints(threshold, domain1D, signal1D);			
			}	
			else if (eventType.equals("Valleys")) {		
				this.lookingForValleyPoints(threshold, domain1D, signal1D);			
			}
			else if (eventType.equals("Slope")) {// Thresholding or MAC)
				//"(Peaks, Valleys, Slope) Threshold", "(Slope) MAC"
				if (thresholdType.equals("(Peaks, Valleys, Slope) Threshold")) {// Thresholding	
					this.lookingForSlopePointsUsingThresholding(threshold, slopeType, domain1D, signal1D);	
				}	
				if (thresholdType.equals("(Slope) MAC")) { // Moving Average Curves (MACs):/ Lu et al., Med. Phys. 33, 3634 (2006); http://dx.doi.org/10.1118/1.2348764
					this.lookingForSlopePointsUsingMAC(threshold, tau, offset, intersections, slopeType, domain1D, signal1D );
				}				
			}	
			else if (eventType.equals("QRS peaks (Chen&Chen)")) {		
				this.lookingForQRSPointsChen(chenM, sumInterval, peakFrame, domain1D, signal1D);			
			}
			else if (eventType.equals("QRS peaks (Osea)")) {		
				this.lookingForQRSPointsOsea(oseaMethod, sampleRate, domain1D, signal1D);			
			}
			//Output options
			//###############################################################################################	
			if (outputType.equals("(Peaks, Valleys, Slope, Chen&Chen, Osea) Event domain values")) {
				if (eventDataIdx == null || eventDataIdx.isEmpty()){
					logService.info(this.getClass().getName() + " No coordinates found! Maybe that threshold is not well set");
					return;
				}		
				//PlotTools.displayPointFinderPlotXY(rangeOld, signal, dataX2, dataY2, false, "Point Finder", "Signal + Points", "Samples [a.u.]", "Values [a.u.]");	
				//(ArrayList<Double>) rangeNew.clone();
				//detectedEvents = coordinateX;
				for (int i = 0; i < eventDataIdx.size(); i++) {
					if (firstColIsDomain) {
						detectedDomain.add(domain1D[eventDataIdx.get(i).intValue()]);
					} else {
						detectedDomain.add((double)(i+1));
					}
				}
				detectedEvents = eventDataX2;
			}	
			else if (outputType.equals("(Peaks, Valleys, Slope, Chen&Chen, Osea) Event values")) {
				if (eventDataIdx == null || eventDataIdx.isEmpty()){
					logService.info(this.getClass().getName() + " No coordinates found! Maybe that threshold is not well set");
					return;
				}		
				//PlotTools.displayPointFinderPlotXY(rangeOld, signal, dataX2, dataY2, false, "Point Finder", "Signal + Points", "Samples [a.u.]", "Values [a.u.]");	
				//(ArrayList<Double>) rangeNew.clone();
				//detectedEvents = coordinateX;
				for (int i = 0; i < eventDataIdx.size(); i++) {
					if (firstColIsDomain) {
						detectedDomain.add(domain1D[eventDataIdx.get(i).intValue()]);
					} else {
						detectedDomain.add((double)(i+1));
					}
				}
				detectedEvents = eventDataY2;
			}	
			else if (outputType.equals("(Peaks, Valleys, Slope, Chen&Chen, Osea) Intervals")) {
				if (eventDataIdx == null || eventDataIdx.isEmpty()){
					logService.info(this.getClass().getName() + " No intervals found! Maybe that threshold is not well set");
					//DialogUtil.getInstance().showDefaultErrorMessage("No intervals found! Maybe that threshold is not well set");	
					return;
				}
				//PlotTools.displayPointFinderPlotXY(rangeOld, signal, dataX2, dataY2, false, "Point Finder", "Signal + Points", "Samples [a.u.]", "Values [a.u.]");
				//(ArrayList<Double>) rangeNew.clone();
				//detectedEvents = intervals;
				double factor = 1.0;
				if (eventType.equals("QRS peaks (Osea)") && !firstColIsDomain) factor = sampleRate;
				
				for (int i = 1; i < eventDataIdx.size(); i++) {
					if (firstColIsDomain) {
						detectedDomain.add(domain1D[eventDataIdx.get(i).intValue()]);
					} else {
						detectedDomain.add((double)(i+1));
					}
					detectedEvents.add((eventDataX2.get(i).doubleValue() - eventDataX2.get(i-1).doubleValue())/factor);
				}
			
				// eliminate first element because it is almost always too short
				eventDataIdx.remove(0);
				eventDataX2.remove(0);
				eventDataY2.remove(0);
				
				if (outputType.equals("(Peaks, Valleys, Slope, Chen&Chen, Osea) Intervals")) {
					if (eventType.equals("QRS peaks (Osea)")) {
						double meanInterval = 0.0;
						for ( double e:  detectedEvents) meanInterval += meanInterval;
						meanInterval = meanInterval/detectedEvents.size();
						logService.info(this.getClass().getName() + " Mean RR interval: "+ meanInterval*1000   + " [ms]");
						logService.info(this.getClass().getName() + " Mean HR: "         + 1.0/meanInterval*60 + " [1/min]");
					}
				}	
			}	
			else if (outputType.equals("(Peaks, Valleys) Heights")) {
				if (eventDataIdx == null || eventDataIdx.isEmpty()){
					logService.info(this.getClass().getName() + " No heights found! Maybe that threshold is not well set");
					//DialogUtil.getInstance().showDefaultErrorMessage("No heights found! Maybe that threshold is not well set");	
					return;
				}
				//PlotTools.displayPointFinderPlotXY(rangeOld, signal, dataX2, dataY2, false, "Point Finder", "Signal + Points", "Samples [a.u.]", "Values [a.u.]");
				//(ArrayList<Double>) rangeNew.clone();
				//detectedEvents = heights;
				
				ArrayList<Double> valuesOfOneInterval = new ArrayList<Double>();
				double median = 0;
				int timeStampIdx1;
				int timeStampIdx2;
				for (int i = 1; i < eventDataIdx.size(); i++) {
					
					if (firstColIsDomain) {
						detectedDomain.add(domain1D[eventDataIdx.get(i)]);
					} else {
						detectedDomain.add((double)(i+1));
					}
								
					// looking for baseline
					// median is a good choice if there is a longer baseline
					// for short baselines this may fail
					timeStampIdx1 = eventDataIdx.get(i-1).intValue();
					timeStampIdx2 = eventDataIdx.get(i).intValue();
					for (int n = (int) timeStampIdx1; n < timeStampIdx2; n++) {
						valuesOfOneInterval.add(signal1D[n]);
					}
					if (valuesOfOneInterval.size() > 0) {
						Collections.sort(valuesOfOneInterval);
						int middleOfInterval = (valuesOfOneInterval.size()) / 2;
						median = 0;
						if (valuesOfOneInterval.size() % 2 == 0) {
							median = (valuesOfOneInterval.get(middleOfInterval)
									+ valuesOfOneInterval.get(middleOfInterval - 1)) / 2;
						} else {
							median = valuesOfOneInterval.get(middleOfInterval);
						}
					}	
					detectedEvents.add(signal1D[timeStampIdx2] - median);
				}		
			}
			else if (outputType.equals("(Peaks, Valleys) delta Heights")) {
				if (eventDataIdx == null || eventDataIdx.isEmpty()) {
					logService.info(this.getClass().getName() + " No delta Heights found! Maybe that threshold is not well set");
					//DialogUtil.getInstance().showDefaultErrorMessage("No delta Heights found! Maybe that threshold is not well set");	
					return;
				}
				//PlotTools.displayPointFinderPlotXY(rangeOld, signal, dataX2, dataY2, false, "Point Finder", "Signal + Points", "Samples [a.u.]", "Values [a.u.]");
				//(ArrayList<Double>) rangeNew.clone();
				//detectedEvents = deltaHeights;
				
				ArrayList<Double> valuesOfOneInterval = new ArrayList<Double>();
				double median = 0;
				int timeStampIdx1;
				int timeStampIdx2;
				for (int i = 1; i < eventDataIdx.size(); i++) {
					
					if (firstColIsDomain) {
						detectedDomain.add(domain1D[eventDataIdx.get(i).intValue()]);
					} else {
						detectedDomain.add((double)(i+1));
					}
					// looking for baseline
					// median is a good choice if there is a longer baseline
					// for short baselines this may fail
					timeStampIdx1 = eventDataIdx.get(i-1).intValue();
					timeStampIdx2 = eventDataIdx.get(i).intValue();
					for (int n = (int) timeStampIdx1; n < timeStampIdx2; n++) {
						valuesOfOneInterval.add(signal1D[n]);
					}
					if (valuesOfOneInterval.size() > 0) {
						Collections.sort(valuesOfOneInterval);

						int middleOfInterval = (valuesOfOneInterval.size()) / 2;
						median = 0;
						if (valuesOfOneInterval.size() % 2 == 0) {
							median = (valuesOfOneInterval.get(middleOfInterval)
									+ valuesOfOneInterval.get(middleOfInterval - 1)) / 2;
						} else {
							median = valuesOfOneInterval.get(middleOfInterval);
						}
					}
					detectedEvents.add((eventDataX2.get(i).doubleValue() - eventDataX2.get(i-1).doubleValue())
							* ((signal1D[timeStampIdx2] - median) * (signal1D[timeStampIdx2] - median))); //interval*hight^2
					//	peakEnergies.add(((stamp2 - stamp1) * samplingInterval) * ((signal1D[(int) stamp2) - median)));
				}
				//eliminate first entry because it is wrong (delta of the first point in the signal)
				eventDataIdx.remove(0);
				eventDataX2.remove(0);
				eventDataY2.remove(0);	
			}
			else if (outputType.equals("(Peaks, Valleys) Energies")) {
				if (eventDataIdx == null || eventDataIdx.isEmpty()){
					logService.info(this.getClass().getName() + " No energies found! Maybe that threshold is not well set");
					//DialogUtil.getInstance().showDefaultErrorMessage("No energies found! Maybe that threshold is not well set");
					return;
				}
				//PlotTools.displayPointFinderPlotXY(rangeOld, signal, dataX2, dataY2, false, "Point Finder", "Signal + Points", "Samples [a.u.]", "Values [a.u.]");
				//(ArrayList<Double>) rangeNew.clone();
				//detectedEvents = energies;
			
				int timeStampIdx1;
				int timeStampIdx2;
				for (int i = 1; i < eventDataIdx.size(); i++) {
					
					if (firstColIsDomain) {
						detectedDomain.add(domain1D[eventDataIdx.get(i).intValue()]);
					} else {
						detectedDomain.add((double)(i+1));
					}
					
					timeStampIdx1 = eventDataIdx.get(i-1).intValue();
					timeStampIdx2 = eventDataIdx.get(i).intValue();
					detectedEvents.add((signal1D[timeStampIdx2]) - (signal1D[timeStampIdx1]));
				}
				eventDataIdx.remove(0);
				eventDataX2.remove(0);
				eventDataY2.remove(0);
			}
		//********************************************************************************************************	
		} else if (signalRange.equals("Subsequent boxes")){ //not for Detect events
		
		//********************************************************************************************************			
		} else if (signalRange.equals("Gliding box")){ //not for Detect events
		
		}
		
		//return detectedEvents;
		// 
		// Output
		// uiService.show(tableOutName, table);
	}

	
	private void lookingForSlopePointsUsingThresholding(float threshold, String slope, double[] rangeOld, double[] signal ){
		
		boolean lookingForThreshold = true;
		int     numberOfFoundPoints = 0;
		double  timeStamp1 = 0.0;
		double  timeStamp2 = 0.0;
		
		if (slope.equals("Positive")) {
			for (int i = 1; i < signal.length - 1; i++) {
				double value = signal[i];

				if (value < threshold) {
					lookingForThreshold = true;
				}
				if (lookingForThreshold) {
					if (value >= threshold) {
						if (signal[i + 1] > signal[i - 1]) {
							numberOfFoundPoints = numberOfFoundPoints + 1;
							timeStamp2 = rangeOld[i];
							eventDataIdx.add(numberOfFoundPoints);
							eventDataX2.add(timeStamp2);
							eventDataY2.add(value);
							timeStamp1 = timeStamp2;
							timeStamp2 = 0;
						}
						// Threshold found
						lookingForThreshold = false;
					}
				}
			}
		}

		if (slope.equals("Negative")) {
			for (int i = 1; i < signal.length - 1; i++) {
				double value = signal[i];

				if (value > threshold) {
					lookingForThreshold = true;
				}
				if (lookingForThreshold) {
					if (value <= threshold) {
						if (signal[i + 1] < signal[i - 1]) {
							numberOfFoundPoints = numberOfFoundPoints + 1;
							timeStamp2 = rangeOld[i];
							eventDataIdx.add(numberOfFoundPoints);
							eventDataX2.add(timeStamp2);
							eventDataY2.add(value);
							timeStamp1 = timeStamp2;
							timeStamp2 = 0;
						}
						// Threshold found
						lookingForThreshold = false;
					}
				}
			}
		}
	}
	//-----------------------------------------------------------------------------------------
	private void lookingForSlopePointsUsingMAC(float threshold, int tau, float offset, int intersections, String slope, double[] rangeOld, double[] signal ) {
		// calculate MAC:
		// int T = 20; // range which is taken for MAC: 2T+1;
		int T = tau; // range which is taken for MAC: 2T+1;

		int L = signal.length;
		double temp = 0, temp2 = 0;
		double[] MAC = new double[signal.length];

		// if signal is too short for the application of the algorithm
		if ((2 * T) > signal.length) {
			//throw new Exception("The signal is too short for this algorithm!");
			logService.info(this.getClass().getName() + " The signal is too short for the MAC algorithm!");
			return;
		}
		// equation (1)
		for (int i = 0; i < 2 * T; i++) {
			temp = temp + signal[i]; // begin (first row (1))
			temp2 = temp2 + signal[L - i - 1]; // end (last row (1))
		}
		for (int i = 0; i < T; i++) { // mean
			MAC[i] = temp / (2 * T) + offset;
			MAC[L - i - 1] = temp2 / (2 * T) + offset;
		}

		// equation (1) middle part
		MAC[T] = MAC[T - 1] * 2 * T + signal[T + T - 1];
		for (int i = T + 1; i < (L - T); i++) {
			MAC[i] = MAC[i - 1] + signal[i + T] - signal[i - T - 1];
		}
		for (int i = T; i < (L - T); i++) { // mean
			MAC[i] = MAC[i] / (2 * T + 1) + offset;
		}

		// ------------------------------------------------------------------------------
		// Find Intersections:
		ArrayList<Integer> intersect_up   = new ArrayList<Integer>();
		ArrayList<Integer> intersect_down = new ArrayList<Integer>();
		ArrayList<Double>  maxVec         = new ArrayList<Double>();
		ArrayList<Double>  minVec         = new ArrayList<Double>();

		int last_intersect = 0;
		int numberOfFoundIntervals = 0;

		for (int i = 0; i < signal.length - 1; i++) {
			double value_t = signal[i];
			double value_t1 = signal[i + 1];

			if ((value_t <= MAC[i]) && (value_t1 >= MAC[i + 1])) {
				if (last_intersect == 1) { // if there are two+ consecutive up intersects ... overwrite the first one
					intersect_up.set(intersect_up.size() - 1, i);
				} // end if
				else {
					intersect_up.add(i);
				} // end else
				last_intersect = 1;
			} else if ((value_t >= MAC[i]) && (value_t1 <= MAC[i + 1])) {
				if (last_intersect == -1) { // if there are two+ consecutive up intersects ... overwrite the first one
					intersect_down.set(intersect_down.size() - 1, i);
				} // end if
				else {
					intersect_down.add(i);
				} // end else
				last_intersect = -1;
			}
		} // end for

		// ------------------------------------------------------------------------------
		// Find Maxima/Minima between intersections:
		int fromIndex, toIndex;
		// ------------------------------------------------------------------------------
		// find Maxima:
		for (int i = 0; i < intersect_up.size(); i++) {
			if (i < intersect_down.size()) {
				fromIndex = intersect_up.get(i);
				toIndex = intersect_down.get(i);
				double max = -Double.MAX_VALUE;
				maxVec.add(rangeOld[fromIndex]);
				for (int j = fromIndex; j < toIndex; j++) {
					if (signal[j] > max) {
						maxVec.set(i, rangeOld[j]);
						max = signal[j];
					} // end if
				} // end for
			} // end if
		} // end for
			// ------------------------------------------------------------------------------
			// find Minima:
		for (int i = 0; i < intersect_down.size(); i++) {
			if (i < intersect_up.size()) {
				fromIndex = intersect_down.get(i);
				toIndex = intersect_up.get(i);
				double min = Double.MAX_VALUE;
				minVec.add(rangeOld[fromIndex]);
				for (int j = fromIndex; j < toIndex; j++) {
					if (signal[j] < min) {
						minVec.set(i, rangeOld[j]);
						min = signal[j];
					} // end if
				} // end for
			} // end if
		} // end for

		// intervals between maxima/minima:
		if (intersections == 0) {
			// ------------------------------------------------------------------------------
			// Find Intervals (distance between Maxima/Minima):
			if (slope.equals("Positive")) { // Intervals by peaks find intervals:
				numberOfFoundIntervals = maxVec.size() - 1;
				for (int i = 0; i < numberOfFoundIntervals; i++) {
					detectedDomain.add((double) i + 1);
					eventDataX2.add(maxVec.get(i + 1));
					eventDataY2.add(signal[i + 1]);
				} // end for
			} // end if
			else if (slope.equals("Negative")) { // Intervals by valleys find intervals:
				numberOfFoundIntervals = minVec.size() - 1;
				for (int i = 0; i < numberOfFoundIntervals; i++) {				
					eventDataIdx.add(i + 1);
					eventDataX2.add(minVec.get(i + 1));
					eventDataY2.add(signal[i + 1]);
				} // end for
			} // end else if
		} // end if
			// intervals between intersections of signal and MAC:
		else if (intersections == 1) {
			// ------------------------------------------------------------------------------
			// Find Intervals (distance between Intersections):
			if (slope.equals("Positive")) { // Intervals between up-intersection find intervals:
				numberOfFoundIntervals = intersect_up.size() - 1;
				for (int i = 0; i < numberOfFoundIntervals; i++) {		
					eventDataIdx.add(i + 1);
					eventDataX2.add(rangeOld[intersect_up.get(i + 1)]);
					//eventDataY2.add(signal1D[intersect_up.get(i + 1)) - signal1D[intersect_up.get(i)));
					eventDataY2.add(signal[intersect_up.get(i + 1)]);
				} // end for
			} // end if
			else if (slope.equals("Negative")) { // Intervals between down-intersections find intervals:
				numberOfFoundIntervals = intersect_down.size() - 1;
				for (int i = 0; i < numberOfFoundIntervals; i++) {										
					eventDataIdx.add(i + 1);
					eventDataX2.add(rangeOld[intersect_down.get(i + 1)]);
					//eventDataY2.add(signal1D[intersect_down.get(i + 1)) - signal1D[intersect_down.get(i)));
					eventDataY2.add(signal[intersect_down.get(i + 1)]);
				} // end for
			} // end else if
		} // end else if

	} // end if method MAC
	// ------------------------------------------------------------------------------
	/**
	 * Looking for Peaks
	 * 2917-07-Adam Dolgos
	 * A peak is searched for all values that are higher than the threshold and are in between two values that are equal to the threshold
	 * The last peak of a signal may be lost because the threshold value may not be reached any more.
	 */
	private void lookingForPeakPoints(float threshold, double[] rangeOld, double[] signal ) {
	
		boolean lookingForPeak = false;
		boolean first = true;
		int     numberOfFoundPoints = 0;
		double  timeStamp1 = rangeOld[0];
		double  timeStamp2 = rangeOld[0];
		int     timeStampIdx1 = 0;
		int     timeStampIdx2 = 0;
		double  localMax     = signal[0];
		double  currentValue = signal[0];
		ArrayList<Double> valuesOfOneInterval = new ArrayList<Double>();
		
			for (int i = 1; i < signal.length - 1; i++) {
				double value = signal[i];

				if (value > threshold) {
					lookingForPeak = true; // looking for peak
					if (signal[i + 1] < signal[i] && signal[i - 1] <= signal[i]) {

						if (first) { //first maximum over the threshold
							first = false;
							timeStamp2    = rangeOld[i];
							timeStampIdx2 = i;
							localMax = signal[i];
						}
						currentValue = signal[i];

						if (currentValue > localMax) {
							timeStamp2    = rangeOld[i];
							timeStampIdx2 = i;
							localMax = currentValue;
						}
					}
				}
				if (lookingForPeak) {

					if (value < threshold && timeStamp2 != 0) {  //After being a while higher than the threshold it is again now under the threshold and we are locally ready 

						numberOfFoundPoints = numberOfFoundPoints + 1;
						
						eventDataIdx.add(numberOfFoundPoints);
						eventDataX2.add(timeStamp2);
						eventDataY2.add(localMax);
						
						timeStamp1 = timeStamp2;
						timeStamp2 = 0;
						timeStampIdx1 = timeStampIdx2;
						timeStampIdx2 = 0;

						valuesOfOneInterval.clear();
						lookingForPeak = false;
						first = true;

					} else {
						lookingForPeak = true;
					}
				}
			}		
	}
	// ------------------------------------------------------------------------------
	/**
	 * Looking for Valleys
	 * 2917-07-Adam Dolgos
	 * A valley is searched for all values that are lower than the threshold and are in between two values that are equal to the threshold
	 * The last peak of a signal may be lost because the threshold value may not be reached any more.
	 */
	private void lookingForValleyPoints(float threshold, double[] rangeOld, double[] signal ) {
		
		boolean lookingForValley = false;
		boolean first = true;
		int     numberOfFoundPoints = 0;
		double  timeStamp1 = rangeOld[0];
		double  timeStamp2 = rangeOld[0];
		int     timeStampIdx1 = 0;
		int     timeStampIdx2 = 0;
		double  median = 0;
		double  localMin = signal[0];
		double  currentValue = signal[0];	
	    ArrayList<Double> valuesOfOneInterval = new ArrayList<Double>();

		for (int i = 1; i < signal.length - 1; i++) {

			double value = signal[i];

			if (value < threshold) {
				lookingForValley = true; // looking for valley
				if (signal[i + 1] > signal[i] && signal[i - 1] >= signal[i]) { 

					if (first) { //first minimum under the threshold
						first = false;
						timeStamp2    = rangeOld[i];
						timeStampIdx2 = i;
						localMin = signal[i];
					}
					currentValue = signal[i];

					if (currentValue < localMin) {
						timeStamp2    = rangeOld[i];	
						timeStampIdx2 = i;	
						localMin = currentValue;
					}
				}
			}
			if (lookingForValley) {
				if (value > threshold && timeStamp2 != 0) { //After being a while lower than the threshold it is again now over the threshold and we are locally ready 

					numberOfFoundPoints = numberOfFoundPoints + 1;
	
					eventDataIdx.add(numberOfFoundPoints);
					eventDataX2.add(timeStamp2);
					eventDataY2.add(localMin);
					
					timeStamp1 = timeStamp2;
					timeStamp2 = 0;
					timeStampIdx1 = timeStampIdx2;
					timeStampIdx2 = 0;				
					valuesOfOneInterval.clear();
					lookingForValley = false;
					first = true;

				} else {
					lookingForValley = true;
				}
			}
		}	
	}
	//------------------------------------------------------------------------------------------------------------------------
	/**
	 * Looking for QRS Peaks
	 * 2018-10 HA according to Chen, H.C., Chen, S.W., 2003. A moving average based filtering system with its application to real-time QRS detection, in: Computers in Cardiology, 2003. Presented at the Computers in Cardiology, 
	 * 													2003, pp. 585–588. https://doi.org/10.1109/CIC.2003.1291223
	 *
	 * adapted  float[] to vectors and floats doubles
	 */
	private void lookingForQRSPointsChen(int M, int sumInterval, int peakFrame, double[] rangeOld, double[] signal ) {
	
		double timeStamp1 = 0.0;
		double timeStamp2 = 0.0;
			
	    //int M = 5; //default 5 for highPass should be 3,5,7
	    //int sumInterval = 19; //default 30 for lowPass should be about 150ms (15 points for 100Hz, 30 points for 200Hz) 
	    //int peakFrame = 150; //default 250
	    
		double[] highPass = highPass(signal,  M);
		double[] lowPass = lowPass(highPass, sumInterval);
		int[] QRSpeaks = QRS(lowPass, peakFrame); //gives back 1 for a peak, otherwise zero. 
		
		
//		dataX2 = rangeOld;
//		dataY2 = highPass;
//		this.rangeNew = dataX2;
//		this.coordinateY = dataY2;

		
		int numberOfFoundPoints = 0;
		for (int i=0; i < signal.length; i++) {
			if (QRSpeaks[i]	== 1) {	
				numberOfFoundPoints = numberOfFoundPoints +1;
				timeStamp2 = rangeOld[i];

				eventDataIdx.add(numberOfFoundPoints);
				eventDataX2.add(timeStamp2);
				eventDataY2.add(signal[i]);
				
				timeStamp1 = timeStamp2;
				timeStamp2 = 0.0;
			}			
		}
	}
	//-------------------------------------------------------------------------------------------------------------
	 //===============High Pass Filter================================
	 // M Window size， 5 or 7
	 // y1: M
	 // y2:Group delay (M+1/2)
	 
	 public double[] highPass(double[] sig, int M) { //nsamp: data
	        double[] highPass = new double[sig.length];
	        int nsamp = sig.length;
	        float constant = (float) 1/M;
	 
	        for(int i=0; i<sig.length; i++) { //sig: input data array
	            double y1 = 0;
	            double y2 = 0;
	 
	            int y2_index = i-((M+1)/2);
	            if(y2_index < 0) { //array
	                y2_index = nsamp + y2_index;
	            }
	            y2 = sig[y2_index];
	 
	            float y1_sum = 0;
	            for(int j=i; j>i-M; j--) {
	                int x_index = i - (i-j);
	                if(x_index < 0) {
	                    x_index = nsamp + x_index;
	                }
	                y1_sum += sig[x_index];
	            }
	 
	            y1 = constant * y1_sum; //constant = 1/M
	            //highPass.set(i, y2 - y1); 
	            highPass[i] =  y2 - y1; 
	        }         
	        return highPass;
	    }
	
	 //============Low pass filter==================
	 //Non Linear
	 public static double[] lowPass(double[] sig, int sumInterval) {
		  	int nsamp = sig.length;
		 	double[] lowPass = new double[sig.length];
	        for(int i=0; i<sig.length; i++) {
	            double sum = 0.0;
	            if(i+sumInterval < sig.length) {
	                for(int j=i; j<i+sumInterval; j++) {
	                    double current = sig[j] * sig[j]; //
	                    sum += current; //
	                }
	            }
	            else if(i+sumInterval >= sig.length) { //array
	                int over = i+sumInterval - sig.length; 
	                for(int j=i; j<sig.length; j++) {
	                    double current = sig[j] * sig[j];
	                    sum += current;
	                }
	                //?? over<0
	                for(int j=0; j<over; j++) {
	                    double current = sig[j] * sig[j];
	                    sum += current;
	                }
	            }
	            lowPass[i] = sum;
	        }
	 
	        return lowPass;
	    }
	 
	 //=================QRS Detection================
	 //beat seeker
	 //alpha: forgetting factor 0.001 - 0.1
	 //Gama: weighting factor 0.15 - 0.2 
	 
	 public static int[] QRS(double[] lowPass, int peakFrame) {
	        int[] QRS = new int[lowPass.length];
	 
	        double treshold = 0;
	 
	        //Threshold
	        for(int i=0; i<peakFrame; i++) {
	            if(lowPass[i] > treshold) {
	                treshold = lowPass[i];
	            }
	        }
	 
	        //int frame = 250; //window size 250 PEAK
	        int frame = peakFrame;
	    		        
	        for(int i=0; i<lowPass.length; i+=frame) { //250
	            double max = 0;
	            int index = 0;
	            if(i + frame > lowPass.length) { //
	                index = lowPass.length;
	            }
	            else {
	                index = i + frame;
	            }
	            for(int j=i; j<index; j++) {
	                if(lowPass[j] > max) max = lowPass[j]; //250data
	            }
	            boolean added = false;
	            for(int j=i; j<index; j++) {
	                if(lowPass[j] > treshold && !added) {
	                    QRS[j] = 1; //250 (0.5)
	                    			//real time frame1
	                    added = true;
	                }
	                else {
	                    QRS[j] = 0;
	                }
	            }	 
	            double gama = (Math.random() > 0.5) ? 0.15 : 0.20;
	            double alpha = 0.01 + (Math.random() * ((0.1 - 0.01)));
	 
	            treshold = alpha * gama * max + (1 - alpha) * treshold;	 
	        }
	 
	        return QRS;
	    }
	
	 
		//------------------------------------------------------------------------------------------------------------------------
		/**
		 * Looking for QRS Peaks
		 * 2021-03 HA according OSEA
		 *
		 */
		private void lookingForQRSPointsOsea(String oseaMethod, int sampleRate, double[] rangeOld, double[] signal ) {
		
			double timeStamp1 = 0.0;
			double timeStamp2 = 0.0;
				
			//QRS-Detection
			int numberOfFoundPoints = 0;
			
			//int sampleRate = 125; //
			//int oseaMethod = 2;  //0...QRSDetector, 1...QRSDetector2,  2....BeatDetectionAndClassify
			
			//Method1-QRSDetector----------------------------------------------------------------------------
			//Hamilton, Tompkins, W. J., "Quantitative investigation of QRS detection rules using the MIT/BIH arrhythmia database",IEEE Trans. Biomed. Eng., BME-33, pp. 1158-1165, 1987.
			//using medians
			if (oseaMethod.equals("QRSDetect")){ //"QRSDetect", "QRSDetect2", "BeatDetectAndClassify"}
				QRSDetector qrsDetector = OSEAFactory.createQRSDetector(sampleRate);	
				for (int i = 0; i < signal.length; i++) {
					//scale signal up, because if it is in the range 0-1 nothing will be detected
					int delay = qrsDetector.QRSDet((int)(signal[i])); //gives back the delay of preceding QRS peak; //I think the first 10 peaks are thrown away 
					
					if (delay != 0) {
					    //System.out.println("A QRS-Complex was detected at sample: " + (i-result));
						numberOfFoundPoints = numberOfFoundPoints +1;
						timeStamp2 = rangeOld[i-delay];
								
						eventDataIdx.add(numberOfFoundPoints);
						eventDataX2.add(timeStamp2);
						eventDataY2.add(signal[i-delay]);
						
						timeStamp1 = timeStamp2;
						timeStamp2 = 0.0;				
					}
		    	}
				logService.info(this.getClass().getName() + " Number of detected QRS complexes using QRSDetect: "+ numberOfFoundPoints);
			}//-----------------------------------------------------------------------------------------------
			//Method2-QRSDetector2----------------------------------------------------------------------------
			//Hamilton, Tompkins, W. J., "Quantitative investigation of QRS detection rules using the MIT/BIH arrhythmia database",IEEE Trans. Biomed. Eng., BME-33, pp. 1158-1165, 1987.
			//using means
			if (oseaMethod.equals("QRSDetect2")){ //"QRSDetect", "QRSDetect2", "BeatDetectAndClassify"}
				QRSDetector2 qrsDetector = OSEAFactory.createQRSDetector2(sampleRate);	
				for (int i = 0; i < signal.length; i++) {
					//scale signal up, because if it is in the range 0-1 nothing will be detected
					int delay = qrsDetector.QRSDet((int)(signal[i])); //gives back the delay of preceding QRS peak;
					
					if (delay != 0) {
					    //System.out.println("A QRS-Complex was detected at sample: " + (i-result));
						numberOfFoundPoints = numberOfFoundPoints +1;
						timeStamp2 = rangeOld[i-delay];
							
						eventDataIdx.add(numberOfFoundPoints);
						eventDataX2.add(timeStamp2);
						eventDataY2.add(signal[i-delay]);
						
						timeStamp1 = timeStamp2;
						timeStamp2 = 0.0;				
					}
		    	}
				logService.info(this.getClass().getName() + " Number of detected QRS complexes using QRSDetect2: "+ numberOfFoundPoints);
			}//------------------------------------------------------------------------------------------------------
			
			//Method3-detection and classification--------------------------------------------------------------------
			//QRSDetector2 for detection
			if (oseaMethod.equals("BeatDetectAndClassify")){ //"QRSDetect", "QRSDetect2", "BeatDetectAndClassify"}
				int numberOfNormalPeaks   = 0;
				int numberOfPVCPeaks      = 0;
				int numberOfUnknownPeaks  = 0;
				BeatDetectionAndClassification bdac = OSEAFactory.createBDAC(sampleRate, sampleRate/2);		
				for (int i = 0; i < signal.length; i++) {
					//scale signal up, because if it is in the range 0-1 nothing will be detected
					BeatDetectAndClassifyResult delay = bdac.BeatDetectAndClassify((int)(signal[i]));
						
					if (delay.samplesSinceRWaveIfSuccess != 0) {
							int qrsPosition =  i - delay.samplesSinceRWaveIfSuccess;
	
							if (delay.beatType == ECGCODES.NORMAL) {
								//logService.info(this.getClass().getName() + " A normal beat type was detected at sample: " + qrsPosition);
								numberOfNormalPeaks += 1; 
							} else if (delay.beatType == ECGCODES.PVC) {
								//logService.info(this.getClass().getName() + " A premature ventricular contraction was detected at sample: " + qrsPosition);
								numberOfPVCPeaks += 1;
							} else if (delay.beatType == ECGCODES.UNKNOWN) {
								//logService.info(this.getClass().getName() + " An unknown beat type was detected at sample: " + qrsPosition);
								numberOfUnknownPeaks +=1;
							} 
						numberOfFoundPoints = numberOfFoundPoints +1;
						timeStamp2 = rangeOld[qrsPosition];
						//System.out.println("qrsPosition: " +qrsPosition);
						//System.out.println("timeStamp2: "  +timeStamp2);
						//System.out.println("timeStamp1: "  +timeStamp1);
											
						eventDataIdx.add(numberOfFoundPoints);
						eventDataX2.add(timeStamp2);
						eventDataY2.add(signal[qrsPosition]);	
						
						timeStamp1 = timeStamp2;
						timeStamp2 = 0.0;
					}			
					//--------------------------------------------------------------------------------------
				}
				logService.info(this.getClass().getName() + " Number of detected QRS complexes using BeatDetectAndClassify: "+ numberOfFoundPoints);
				logService.info(this.getClass().getName() + " Number of normal QRS peaks: "+ numberOfNormalPeaks);
				logService.info(this.getClass().getName() + " Number of premature ventricular contractions: "+ numberOfPVCPeaks);
				logService.info(this.getClass().getName() + " Number of unknown QRS peaks: "+ numberOfUnknownPeaks);
			}//Method 3-------------------------------------------------------------------------------------
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
		ij.command().run(SignalDetectEvents.class, true);
	}
}
