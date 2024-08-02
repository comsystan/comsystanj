/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj1DHurst.java
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
package at.csa.csaj.plugin1d.frac;

import java.awt.Toolkit;
import java.lang.invoke.MethodHandles;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
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

import at.csa.csaj.commons.Algorithm_Surrogate1D;
import at.csa.csaj.commons.Dialog_WaitingWithProgressBar;
import at.csa.csaj.commons.Plot_RegressionFrame;
import at.csa.csaj.commons.Container_ProcessMethod;
import at.csa.csaj.commons.Util_GenerateInterval;
import at.csa.csaj.plugin1d.frac.util.HKprocess;
import at.csa.csaj.plugin1d.frac.util.HurstRS;
import at.csa.csaj.plugin1d.frac.util.HurstSP;
import at.csa.csaj.plugin1d.misc.Csaj1DOpener;

/**
 * A {@link ContextCommand} plugin computing <the Hurst coefficient</a>
* of a sequence.
 * fGn Hurst-Kolmogorov process, specially suited for small sequence lengths
 * fGm traditional Rescaled range algorithm
 * fGm simpler Scaling property algorithm
 *
 * HK according to:
 * Tyralis, Hristos, und Demetris Koutsoyiannis.
 * „A Bayesian Statistical Model for Deriving the Predictive Distribution of Hydroclimatic Variables“.
 * Climate Dynamics 42, Nr. 11 (1. Juni 2014): 2867–83.
 * https://doi.org/10.1007/s00382-013-1804-y.
 *
 * See also:
 * Likens et al. „Better than DFA? A Bayesian Method for Estimating the Hurst Exponent in Behavioral Sciences“.
 * arXiv, 26. Januar 2023.
 * https://doi.org/10.48550/arXiv.2301.11262.
 * 
 * For RS and SP:
 * https://www.adrian.idv.hk/2021-07-26-hurst/
 * Adrian S. Tam  •  © 2023  •  CC-BY-SA 4.0 •  http://www.adrian.idv.hk
*/
 
@Plugin(type = ContextCommand.class, 
	headless = true,
	label = "Hurst coefficient (HK,RS,SP)",
	initializer = "initialPluginLaunch",
	iconPath = "/icons/comsystan-logo-grey46-16x16.png", //Menu entry icon
	menu = {
	@Menu(label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT, mnemonic = MenuConstants.PLUGINS_MNEMONIC),
	@Menu(label = "ComsystanJ"),
	@Menu(label = "1D Sequence(s)"),
	@Menu(label = "Fractal analyses", weight = 6),
	@Menu(label = "Hurst coefficient (HK,RS,SP)")}) //Space at the end of the label is necessary to avoid duplicate with 2D plugin 
//public class Csaj1DHurst<T extends RealType<T>> extends InteractiveCommand { // non blocking  GUI
public class Csaj1DHurst<T extends RealType<T>> extends ContextCommand implements Previewable { //modal GUI with cancel

	private static final String PLUGIN_LABEL            = "<html><b>Hurst coefficient (HK,RS,SP)</b></html>";
	private static final String SPACE_LABEL             = "";
	private static final String HURSTOPTIONS_LABEL      = "<html><b>Hurst options</b></html>";
	private static final String ANALYSISOPTIONS_LABEL   = "<html><b>Analysis options</b></html>";
	private static final String BACKGROUNDOPTIONS_LABEL = "<html><b>Background option</b></html>";
	private static final String DISPLAYOPTIONS_LABEL    = "<html><b>Display options</b></html>";
	private static final String PROCESSOPTIONS_LABEL    = "<html><b>Process options</b></html>";
	
	private static double[] sequence1D;
	private static double[] subSequence1D;
	private static double[] surrSequence1D;
	Column<? extends Object> sequenceColumn;
	
	private static String tableInName;
	private static String[] sliceLabels;
	private static long numColumns = 0;
	private static long numRows = 0;
	private static long numDimensions = 0;
	private static int  numSurrogates = 0;
	private static int  numBoxLength = 0;
	private static long numSubsequentBoxes = 0;
	private static long numGlidingBoxes = 0;
	
	private static int   hkN;
	private static int   epsRegStart;
	private static int   epsRegEnd;
	private static int   epsN;
	private static int[] epsInterval;
	private static int   spMaxLag;
	
	HKprocess hkProcess;
	HurstRS hurstRS;
	HurstSP hurstSP;
	
	double hkH; //Hurst Kolmogorov H
	double rsH; //Rescaled range R/S H
	double spH; //Scaling property SP H
	
	private static ArrayList<Plot_RegressionFrame> doubleLogPlotList = new ArrayList<Plot_RegressionFrame>();
	
	private static final String tableOutName = "Table - Hurst";
	
	private Dialog_WaitingWithProgressBar dlgProgress;
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

	//@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	//private final String labelSpace = SPACE_LABEL;
	
	@Parameter(type = ItemIO.INPUT)
	private DefaultTableDisplay  defaultTableDisplay;

	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelHurstOptions = HURSTOPTIONS_LABEL;
	
	@Parameter(label = "fGn HK",
			   persist = false,
			   description = "",
			   initializer = "initialHurstHK",
		       callback = "callbackHurstHK")
	private boolean booleanHurstHK;
	
	@Parameter(label = "fBm RS",
			   persist = true,
			   initializer = "initialHurstRS",
		       callback = "callbackHurstRS")
	private boolean booleanHurstRS;
	
	@Parameter(label = "fBm SP",
			   persist = true,
			   initializer = "initialHurstSP",
		       callback = "callbackHurstSP")
	private boolean booleanHurstSP;
	
	@Parameter(label = "(HK) n",
			   description = "Number of samples from the posterior distribution - default=500",
			   style = NumberWidget.SPINNER_STYLE,
			   min = "1",
			   max = "9999999999999999999",
			   stepSize = "1",
			   persist = false, //restore previous value default = true
			   initializer = "initialHKN", callback = "callbackHKN")
	private int spinnerInteger_HKN = 500;

	@Parameter(label = "(RS) Win Min",
			   description = "Minimum size of window",
			   style = NumberWidget.SPINNER_STYLE,
			   min = "1",
			   max = "9999999999999999999",
			   stepSize = "1",
			   persist = false, //restore previous value default = true
			   initializer = "initialEpsRegStart", callback = "callbackEpsRegStart")
	private int spinnerInteger_EpsRegStart = 1;

	@Parameter(label = "(RS) Win Max",
			   description = "Maximum size of window",
			   style = NumberWidget.SPINNER_STYLE,
			   min = "3",
			   max = "9999999999999999999",
			   stepSize = "1",
			   persist = false, //restore previous value default = true
			   initializer = "initialEpsRegEnd", callback = "callbackEpsRegEnd")
	private int spinnerInteger_EpsRegEnd = 3;
	
	@Parameter(label = "(RS) Win #",
			   description = "Number of unique window sizes - default=10",
			   style = NumberWidget.SPINNER_STYLE,
			   min = "3",
			   max = "9999999999999999999",
			   stepSize = "1",
			   persist = false, //restore previous value default = true
			   initializer = "initialEpsN", callback = "callbackEpsN")
	private int spinnerInteger_EpsN = 10;
	
	@Parameter(label = "(SP) Max lag",
			   description = "Maximum lag of data values",
			   style = NumberWidget.SPINNER_STYLE,
			   min = "3",
			   max = "9999999999999999999",
			   stepSize = "1",
			   persist = false, //restore previous value default = true
			   initializer = "initialSPMaxLag", callback = "callbackSPMaxLag")
	private int spinnerInteger_SPMaxLag = 10;
	
//	//-----------------------------------------------------------------------------------------------------
//	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
//	private final String labelRegression = REGRESSION_LABEL;

	
	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelAnalysisOptions = ANALYSISOPTIONS_LABEL;

	@Parameter(label = "Sequence range",
			   description = "Entire sequence, Subsequent boxes or Gliding box",
			   style = ChoiceWidget.LIST_BOX_STYLE,
			   choices = {"Entire sequence", "Subsequent boxes", "Gliding box"}, 
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
	
	@Parameter(label = "Surrogates #",
			   description = "Number of computed surrogates",
			   style = NumberWidget.SPINNER_STYLE, 
			   min = "1",
			   max = "9999999999999999999",
			   stepSize = "1",
			   persist = true, // restore  previous value  default  =  true
			   initializer = "initialNumSurrogates",
			   callback = "callbackNumSurrogates")
	private int spinnerInteger_NumSurrogates;
	
	@Parameter(label = "Box length",
			   description = "Length of subsequent or gliding box",
			   style = NumberWidget.SPINNER_STYLE, 
			   min = "2",
			   max = "9999999999999999999",
			   stepSize = "1",
			   persist = true, // restore  previous value  default  =  true
			   initializer = "initialBoxLength",
			   callback = "callbackBoxLength")
	private int spinnerInteger_BoxLength;
	
	@Parameter(label = "(Surr/Box) Hurst type",
			   description = "Hurst method for Surrogates, Subsequent boxes or Gliding box",
			   style = ChoiceWidget.LIST_BOX_STYLE,
			   choices = {"H (HK)", "H (RS)", "H (SP)"},
			   persist = true,  //restore previous value default = true
			   initializer = "initialSurrBoxHurstType",
			   callback = "callbackSurrBoxHurstType")
	private String choiceRadioButt_SurrBoxHurstType;
		
	//-----------------------------------------------------------------------------------------------------
//	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
//	private final String labelBackgroundOptions = BACKGROUNDOPTIONS_LABEL;

	@Parameter(label = "Skip zero values",
			   persist = true,
		       callback = "callbackSkipZeroes")
	private boolean booleanSkipZeroes;
	
	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelDisplayOptions = DISPLAYOPTIONS_LABEL;

	@Parameter(label = "Show double log plot",
		   	   persist = true, //restore previous value default = true
			   initializer = "initialShowDoubleLogPlots")
	private boolean booleanShowDoubleLogPlot;

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

//	@Parameter(label = "Process all columns", callback = "callbackProcessAllColumns")
//	private Button buttonProcessAllColumns;


	// ---------------------------------------------------------------------
		
	protected void initialPluginLaunch() {
		checkItemIOIn();
	}	
	protected void initialHurstHK() {
		booleanHurstHK = false;
	}	
	protected void initialHurstRS() {
		booleanHurstRS = false;
	}
	protected void initialHurstSP() {
		booleanHurstSP = false;
	}
	protected void initialHKN() {
		spinnerInteger_HKN = 500;	
	}
	protected void initialEpsRegStart() {
		epsRegStart = 2;
		spinnerInteger_EpsRegStart = epsRegStart;
	}
	protected void initialEpsRegEnd() {
		epsRegEnd = 0;
		try {
			epsRegEnd = (int) Math.floor((double)tableIn.getRowCount() / 1.0);
		} catch (NullPointerException npe) {
			logService.error(this.getClass().getName() + " ERROR: NullPointerException, input table = null");
			cancel("ComsystanJ 1D plugin cannot be started - missing input table.");;
		}	
		spinnerInteger_EpsRegEnd = epsRegEnd;
	}
	protected void initialEpsN() {
		if (epsRegEnd - epsRegStart + 1  < 10) {
			epsN = epsRegEnd - epsRegStart + 1;
		} else {
			epsN = 10;
		}
		spinnerInteger_EpsN = epsN;
	}
	protected void initialSPMaxLag() {
		spinnerInteger_SPMaxLag = 10;	
	}
	protected void initialSequenceRange() {
		choiceRadioButt_SequenceRange = "Entire sequence";
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
		numSubsequentBoxes = (long) Math.floor((double)numRows/(double)spinnerInteger_BoxLength);
		numGlidingBoxes = numRows - spinnerInteger_BoxLength + 1;
	}
	protected void initialSurrBoxHurstType() {
		this.choiceRadioButt_SurrBoxHurstType = "H (RS)";
	}
	protected void initialSkipZeroes() {
		booleanSkipZeroes = false;
	}	
	protected void initialShowDoubleLogPlots() {
		booleanShowDoubleLogPlot = true;
	}
	protected void initialOverwriteDisplays() {
    	booleanOverwriteDisplays = true;
	}
	protected void initialNumColumn() {
		spinnerInteger_NumColumn = 1;
	}

	// ------------------------------------------------------------------------------
	
	/** Executed whenever the {@link #booleanHurstHK} parameter changes. */
	protected void callbackHurstHK() {
		logService.info(this.getClass().getName() + " fGn HK set to " + booleanHurstHK);
		if (booleanHurstHK == true) {
		booleanHurstRS = false;
		booleanHurstSP = false;
		logService.info(this.getClass().getName() + " fBm RS set to " + booleanHurstRS);
		logService.info(this.getClass().getName() + " fBm SP set to " + booleanHurstSP);
		}	
	}
	/** Executed whenever the {@link #booleanHurstRS} parameter changes. */
	protected void callbackHurstRS() {
		logService.info(this.getClass().getName() + " fBm RS set to " + booleanHurstRS);
		if (booleanHurstRS == true) {
		booleanHurstHK = false;
		logService.info(this.getClass().getName() + " fGn HK set to " + booleanHurstHK);
		}
	}
	/** Executed whenever the {@link #booleanHurstSP} parameter changes. */
	protected void callbackHurstSP() {
		logService.info(this.getClass().getName() + " fBm SP set to " + booleanHurstSP);
		if (booleanHurstSP == true) {
		booleanHurstHK = false;
		logService.info(this.getClass().getName() + " fGn HK set to " + booleanHurstHK);
		}
	}
	/** Executed whenever the {@link #spinnerInteger_HKN} parameter changes. */
	protected void callbackHKN() {	
		if (spinnerInteger_HKN < 1) {
			spinnerInteger_HKN = 1;
		}	
		if (spinnerInteger_HKN < 100) {
			JOptionPane.showMessageDialog(null, "n should be higher for high quality estimates of H!", "Reliability warning", JOptionPane.WARNING_MESSAGE);
		}
		logService.info(this.getClass().getName() + " n set to " + spinnerInteger_HKN);
	}
	/** Executed whenever the {@link #spinnerInteger_EpsRegStart} parameter changes. */
	protected void callbackEpsRegStart() {
		
		if (spinnerInteger_EpsRegStart >= spinnerInteger_EpsRegEnd - 2) {
			spinnerInteger_EpsRegStart = spinnerInteger_EpsRegEnd - 2;
		}
		if (spinnerInteger_EpsRegStart < 1) {
			spinnerInteger_EpsRegStart = 1;
		}
		epsRegStart = spinnerInteger_EpsRegStart;
		logService.info(this.getClass().getName() + " Regression Min set to " + spinnerInteger_EpsRegStart);
		callbackEpsN();
	}
	/** Executed whenever the {@link #spinnerInteger_EpsRegEnd} parameter changes. */
	protected void callbackEpsRegEnd() {
		if (spinnerInteger_EpsRegEnd <= spinnerInteger_EpsRegStart + 2) {
			spinnerInteger_EpsRegEnd = spinnerInteger_EpsRegStart + 2;
		}
		if (spinnerInteger_EpsRegEnd > numRows) {
			spinnerInteger_EpsRegEnd = (int)numRows;
		}
		epsRegEnd = spinnerInteger_EpsRegEnd;
		logService.info(this.getClass().getName() + " Regression Max set  to " + spinnerInteger_EpsRegEnd);
		callbackEpsN();
	}
	/** Executed whenever the {@link #spinnerInteger_EpsN} parameter changes. */
	protected void callbackEpsN() {
		if (spinnerInteger_EpsRegEnd - spinnerInteger_EpsRegStart + 1 < spinnerInteger_EpsN) {
			spinnerInteger_EpsN = spinnerInteger_EpsRegEnd - spinnerInteger_EpsRegStart + 1;
		}
		epsN = spinnerInteger_EpsN;
		epsInterval = Util_GenerateInterval.getIntLogDistributedInterval(epsRegStart, epsRegEnd, epsN);
		
		if (epsInterval.length < epsN) {
			logService.info(this.getClass().getName() + " Note: Eps interval is limited to a sequence of unique values");
			epsN =epsInterval.length;
			spinnerInteger_EpsN = epsN;
		}
		logService.info(this.getClass().getName() + " Regression # set to: " + spinnerInteger_EpsN);
		logService.info(this.getClass().getName() + " Eps #        set to: " + epsInterval.length);
		logService.info(this.getClass().getName() + " Eps interval set to: " + Arrays.toString(epsInterval));
	}
	/** Executed whenever the {@link #spinnerIntegerSPMaxLag} parameter changes. */
	protected void callbackSPMaxLag() {	
		if (spinnerInteger_SPMaxLag > numRows) {
			spinnerInteger_SPMaxLag = (int)numRows;
		}
		logService.info(this.getClass().getName() + " Max lag set to " + spinnerInteger_SPMaxLag);
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
	
	/** Executed whenever the {@link #spinnerInteger_NumSurrogates} parameter changes. */
	protected void callbackNumSurrogates() {
		numSurrogates = spinnerInteger_NumSurrogates;
		logService.info(this.getClass().getName() + " Number of surrogates set to " + spinnerInteger_NumSurrogates);
	}
	
	/** Executed whenever the {@link #spinnerInteger_BoxLength} parameter changes. */
	protected void callbackBoxLength() {
		numBoxLength = spinnerInteger_BoxLength;
		numSubsequentBoxes = (long) Math.floor((double)numRows/(double)spinnerInteger_BoxLength);
		numGlidingBoxes = numRows - spinnerInteger_BoxLength + 1;
		logService.info(this.getClass().getName() + " Box length set to " + spinnerInteger_BoxLength);
	}

	/** Executed whenever the {@link #choiceRadioButt_SurrBoxHurstType} parameter changes. */
	protected void callbackSurrBoxHurstType() {
		logService.info(this.getClass().getName() + " Hurst type for surrogate or box set to " + choiceRadioButt_SurrBoxHurstType);
	}
	
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
				
		sliceLabels = new String[(int) numColumns];
	      
		logService.info(this.getClass().getName() + " Name: "      + tableInName); 
		logService.info(this.getClass().getName() + " Columns #: " + numColumns);
		logService.info(this.getClass().getName() + " Rows #: "    + numRows); 
	}
	
	/**
	* This method starts the workflow for a single column of the active display
	*/
	protected void startWorkflowForSingleColumn() {
		
		dlgProgress = new Dialog_WaitingWithProgressBar("Computing Hurst value(s), please wait... Open console window for further info.",
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
		
		dlgProgress = new Dialog_WaitingWithProgressBar("Computing Hurst values, please wait... Open console window for further info.",
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
		tableOut.add(new GenericColumn("File name"));
		tableOut.add(new GenericColumn("Column name"));	
		tableOut.add(new GenericColumn("Sequence range"));
		tableOut.add(new GenericColumn("Surrogate type"));
		tableOut.add(new IntColumn("# Surrogates"));
		tableOut.add(new IntColumn("Box length"));
		tableOut.add(new BoolColumn("Skip zeroes"));
		
		tableOut.add(new IntColumn("(HK) n"));
		tableOut.add(new IntColumn("(RS) Win Min"));
		tableOut.add(new IntColumn("(RS) Win Max"));
		tableOut.add(new IntColumn("(RS) Win #"));
		tableOut.add(new IntColumn("(SP) Max lag"));

		//"Entire sequence", "Subsequent boxes", "Gliding box" 
		if (choiceRadioButt_SequenceRange.equals("Entire sequence")){
			tableOut.add(new DoubleColumn("H (HK)"));	
			tableOut.add(new DoubleColumn("H (RS)"));	
			tableOut.add(new DoubleColumn("H (SP)"));
					
			if (choiceRadioButt_SurrogateType.equals("No surrogates")) {
				//do nothing	
			} else { //Surrogates
				tableOut.add(new DoubleColumn(choiceRadioButt_SurrBoxHurstType+"_Surr")); //Mean surrogate value	
				for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn(choiceRadioButt_SurrBoxHurstType+"_Surr-#"+(s+1))); 
			}	
		} 
		else if (choiceRadioButt_SequenceRange.equals("Subsequent boxes")){
			for (int n = 1; n <= numSubsequentBoxes; n++) {
				tableOut.add(new DoubleColumn(choiceRadioButt_SurrBoxHurstType+"-#" + n));	
			}
		}
		else if (choiceRadioButt_SequenceRange.equals("Gliding box")){
			for (int n = 1; n <= numGlidingBoxes; n++) {
				tableOut.add(new DoubleColumn(choiceRadioButt_SurrBoxHurstType+"-#" + n));	
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

		if (optDeleteExistingPlots) {
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
		if (optDeleteExistingTables) {
			List<Display<?>> list = defaultDisplayService.getDisplays();
			for (int i = 0; i < list.size(); i++) {
				Display<?> display = list.get(i);
				//System.out.println("display name: " + display.getName());
				if (display.getName().contains(tableOutName))
					display.close();
			}
		}
	}

  	/** 
	 * This method takes the single column c and computes results. 
	 * @Param int c
	 * */
	private void processSingleInputColumn (int c) {
		
		long startTime = System.currentTimeMillis();
		
		// Compute result values
		Container_ProcessMethod containerPM = process(tableIn, c); 
		// 0 D, 1 R2, 2 StdErr
		//logService.info(this.getClass().getName() + " H (HK): " + containerPM.result1_Values[0]);
		logService.info(this.getClass().getName() + " Processing finished.");
		writeToTable(0, c, containerPM); //write always to the first row
		
		long duration = System.currentTimeMillis() - startTime;
		TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
		SimpleDateFormat sdf = new SimpleDateFormat();
		sdf.applyPattern("HHH:mm:ss:SSS");
		logService.info(this.getClass().getName() + " Elapsed time: "+ sdf.format(duration));
	}

	/** This method loops over all input columns and computes results. 
	 * @param dlgProgress */
	private void processAllInputColumns() {
		
		long startTimeAll = System.currentTimeMillis();
		
		Container_ProcessMethod containerPM;
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
				// 0 Dh, 1 R2, 2 StdErr
				logService.info(this.getClass().getName() + " Processing finished.");
				writeToTable(s, s, containerPM);
	
				long duration = System.currentTimeMillis() - startTime;
				TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
				SimpleDateFormat sdf = new SimpleDateFormat();
				sdf.applyPattern("HHH:mm:ss:SSS");
				logService.info(this.getClass().getName() + " Elapsed time: "+ sdf.format(duration));
			//}
		} //s	
		statusService.showProgress(0, 100);
		statusService.clearStatus();
		
		long duration = System.currentTimeMillis() - startTimeAll;
		TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
		SimpleDateFormat sdf = new SimpleDateFormat();
		sdf.applyPattern("HHH:mm:ss:SSS");
		logService.info(this.getClass().getName() + " Elapsed processing time for all sequence(s): "+ sdf.format(duration));
	}
	
	/**
	 * collects current result and writes to table
	 * 
	 * @param int numRow to write in the result table
	 * @param in sequenceNumber column number of sequence from tableIn.
	 * @param Container_ProcessMethod containerPM
	 */
	private void writeToTable(int numRow, int sequenceNumber, Container_ProcessMethod containerPM) {
		logService.info(this.getClass().getName() + " Writing to the table...");
		int row = numRow;
		int tableColStart = 0;
		int tableColEnd   = 0;
		int tableColLast  = 0;
		
		// 0 Dh, 1 R2, 2 StdErr
		// fill table with values
		tableOut.appendRow();
		tableOut.set(0, row, tableInName);//File Name
		if (sliceLabels != null)  tableOut.set(1, row, tableIn.getColumnHeader(sequenceNumber)); //Column Name
		tableOut.set(2, row, choiceRadioButt_SequenceRange);  //Sequence range
		tableOut.set(3, row, choiceRadioButt_SurrogateType); //Surrogate Method
		if (choiceRadioButt_SequenceRange.equals("Entire sequence") && (!choiceRadioButt_SurrogateType.equals("No surrogates"))) {
			tableOut.set(4, row, spinnerInteger_NumSurrogates); //# Surrogates
		} else {
			tableOut.set(4, row, null); //# Surrogates
		}
		if (!choiceRadioButt_SequenceRange.equals("Entire sequence")){
			tableOut.set(5, row, spinnerInteger_BoxLength); //Box Length
		} else {
			tableOut.set(5, row, null);
		}	
		tableOut.set(6,  row, booleanSkipZeroes); //Zeroes removed
		if (booleanHurstHK) {
			tableOut.set(7,  row, hkN); 
		} else {
			tableOut.set(7,  row, null); 
		}
		if (booleanHurstRS) {
			tableOut.set(8,  row, epsRegStart); //may be changed by the algorithm
			tableOut.set(9,  row, epsRegEnd); //may be changed by the algorithm
			tableOut.set(10, row, epsN);   //may be changed by the algorithm
		} else {
			tableOut.set(8,  row, null);
			tableOut.set(9,  row, null);
			tableOut.set(10, row, null);
		}
		if (booleanHurstSP) {
			tableOut.set(11, row, spMaxLag);
		} else {
			tableOut.set(11, row, null);
		}
		tableColLast = 11;
		
		if (containerPM == null) { //set missing result values to NaN
			tableColStart = tableColLast + 1;
			tableColEnd = tableOut.getColumnCount() - 1;
			for (int c = tableColStart; c <= tableColEnd; c++ ) {
				tableOut.set(c, row, Double.NaN);
			}
		}
		else { //set result values
			tableColStart = tableColLast + 1;
			tableColEnd = tableColStart + containerPM.item1_Values.length - 1;
			for (int c = tableColStart; c <= tableColEnd; c++ ) {
				tableOut.set(c, row, containerPM.item1_Values[c-tableColStart]);
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
	* Processing
	*/
	private Container_ProcessMethod process(DefaultGenericTable dgt, int col) { //  c column number
	
		if (dgt == null) {
			logService.info(this.getClass().getName() + " WARNING: dgt==null, no sequence for processing!");
		}

		String sequenceRange  = choiceRadioButt_SequenceRange;
		String surrType       = choiceRadioButt_SurrogateType;
		numSurrogates         = spinnerInteger_NumSurrogates;
		numBoxLength          = spinnerInteger_BoxLength;
		booleanHurstHK        = booleanHurstHK;
		booleanHurstRS        = booleanHurstRS;
		booleanHurstSP        = booleanHurstSP;
		hkN                   = spinnerInteger_HKN;
		epsRegStart                = spinnerInteger_EpsRegStart; //may be changed later
		epsRegEnd                = spinnerInteger_EpsRegEnd; //may be changed later
		epsN                  = spinnerInteger_EpsN;   //may be changed later
		spMaxLag              = spinnerInteger_SPMaxLag;
		
		int numDataPoints     = dgt.getRowCount();
		boolean skipZeroes    = booleanSkipZeroes;	
		boolean optShowPlot   = booleanShowDoubleLogPlot;
		epsInterval = Util_GenerateInterval.getIntLogDistributedInterval(epsRegStart, epsRegEnd, epsN);
		
		double[] resultValues = new double[3]; // hkH, rsH, spH
		for (int r = 0; r<resultValues.length; r++) resultValues[r] = Double.NaN;
		
//		double[]totals = new double[numKMax];
//		double[]eps = new double[numKMax];
//		// definition of eps
//		for (int kk = 0; kk < numKMax; kk++) {
//			eps[kk] = kk + 1;		
//			//logService.info(this.getClass().getName() + " k=" + kk + " eps= " + eps[kk][b]);
//		}
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
		if (skipZeroes) sequence1D = removeZeroes(sequence1D);

		//numDataPoints may be smaller now
		numDataPoints = sequence1D.length;
		
		logService.info(this.getClass().getName() + " Column #: "+ (col+1) + "  " + sequenceColumn.getHeader() + "  Size of sequence = " + numDataPoints);
		if (numDataPoints == 0) return null; //e.g. if sequence had only NaNs
		
		//domain1D = new double[numDataPoints];
		//for (int n = 0; n < numDataPoints; n++) domain1D[n] = n+1
		
		//"Entire sequence", "Subsequent boxes", "Gliding box" 
		//********************************************************************************************************
		if (sequenceRange.equals("Entire sequence")){	
			if (surrType.equals("No surrogates")) {
				resultValues = new double[3]; // hkH rsH spH
			} else {
				resultValues = new double[3+1+1*numSurrogates]; //+ Mean_Surr, H_Surr1, H_Surr2, H_Surr3,....
			}
			for (int r = 0; r < resultValues.length; r++) resultValues[r] = Double.NaN;
			//logService.info(this.getClass().getName() + " Column #: "+ (col+1) + "  " + sequenceColumn.getHeader() + "  Size of sequence = " + sequence1D.length);	
			//if (sequence1D.length) == 0) return null; //e.g. if sequence had only NaNs
			hkH = Double.NaN;
			rsH = Double.NaN;
			spH = Double.NaN;
			
			if (booleanHurstHK) {
				//Compute H with HK method
				hkProcess = new HKprocess(logService);
				hkProcess.computeH(sequence1D, hkN);
				hkH = hkProcess.getH();
			}		
			if (booleanHurstRS) {
				hurstRS = new HurstRS();
				hurstRS.computeH(sequence1D, epsInterval);
				rsH = hurstRS.getH();
				
				if (optShowPlot) {	
					String preName = "R/S " +sequenceColumn.getHeader();
					showPlot(hurstRS.getLnDataX(), hurstRS.getLnDataY(), preName, col, "ln(Window size)", "ln(R/S)", 1, hurstRS.getLnDataX().length);
				}
			}	
			if (booleanHurstSP) {
				hurstSP = new HurstSP();
				hurstSP.computeH(sequence1D, spMaxLag);
				spH = hurstSP.getH();
			}				
			resultValues[0]  = hkH;
			resultValues[1]  = rsH;
			resultValues[2]  = spH;
			logService.info(this.getClass().getName() + " Eps #:  " + epsInterval.length);
			logService.info(this.getClass().getName() + " Eps interval:  " + Arrays.toString(epsInterval));
			logService.info(this.getClass().getName() + " H (HK):" + hkH);
			logService.info(this.getClass().getName() + " H (RS):" + rsH);
			logService.info(this.getClass().getName() + " H (SP):" + spH);
	
			int lastMainResultsIndex = 2;
			
			if (!surrType.equals("No surrogates")) { //Add surrogate analysis
				surrSequence1D = new double[sequence1D.length];
				
				double H  = Double.NaN;
				double sumHs  = 0.0;
				
				Algorithm_Surrogate1D surrogate1D = new Algorithm_Surrogate1D();
				String windowingType = "Rectangular";
				for (int s = 0; s < numSurrogates; s++) {
					//choices = {"No surrogates", "Shuffle", "Gaussian", "Random phase", "AAFT"}, 
					if (surrType.equals("Shuffle"))      surrSequence1D = surrogate1D.calcSurrogateShuffle(sequence1D);
					if (surrType.equals("Gaussian"))     surrSequence1D = surrogate1D.calcSurrogateGaussian(sequence1D);
					if (surrType.equals("Random phase")) surrSequence1D = surrogate1D.calcSurrogateRandomPhase(sequence1D, windowingType);
					if (surrType.equals("AAFT"))         surrSequence1D = surrogate1D.calcSurrogateAAFT(sequence1D, windowingType);

					if (this.choiceRadioButt_SurrBoxHurstType.equals("H (HK)")) {
						//Compute H with HK method
						hkProcess = new HKprocess(logService);
						hkProcess.computeH(surrSequence1D, hkN);
						H = hkProcess.getH();
					}		
					if (this.choiceRadioButt_SurrBoxHurstType.equals("H (RS)")) {
						hurstRS = new HurstRS();
						hurstRS.computeH(surrSequence1D, epsInterval);
						H = hurstRS.getH();
						
//						if (optShowPlot) {	
//							String preName = "R/S " +sequenceColumn.getHeader();
//							showPlot(hurstRS.getLnDataX(), hurstRS.getLnDataY(), preName, col, "ln(Window size)", "ln(R/S)", 1, hurstRS.getLnDataX().length);
//						}
					}	
					if (this.choiceRadioButt_SurrBoxHurstType.equals("H (SP)")) {
						hurstSP = new HurstSP();
						hurstSP.computeH(surrSequence1D, spMaxLag);
						H = hurstSP.getH();
					}		
					
					resultValues[lastMainResultsIndex   + 2 + s]= H;
					//resultValues[lastMainResultsIndex + 2 + numSurrogates + s]    = ?;
					//resultValues[lastMainResultsIndex + 2 + (2*numSurrogates) +s] = ?;
					sumHs += H;
					
				}
				logService.info(this.getClass().getName() + " H Surr mean:" + sumHs/numSurrogates);
				resultValues[lastMainResultsIndex   + 1] = sumHs/numSurrogates;
				//resultValues[lastMainResultsIndex + 2] = sum?s/numSurrogates;
				//resultValues[lastMainResultsIndex + 3] = sum?s/?;				
			} 
		//********************************************************************************************************	
		} else if (sequenceRange.equals("Subsequent boxes")){
			resultValues = new double[(int) (1*numSubsequentBoxes)]; // H ==  1*number of boxes       Dim R2 == two * number of boxes		
			for (int r = 0; r<resultValues.length; r++) resultValues[r] = Double.NaN;
			subSequence1D = new double[(int) numBoxLength];
			//number of boxes may be smaller than intended because of NaNs or removed zeroes
			long actualNumSubsequentBoxes = (long) Math.floor((double)sequence1D.length/(double)spinnerInteger_BoxLength);
			double H = Double.NaN;
			hkProcess = new HKprocess(logService);
			hurstRS = new HurstRS();
			hurstSP = new HurstSP();
				
			//get sub-sequences and compute dimensions
			for (int i = 0; i < actualNumSubsequentBoxes; i++) {	
				logService.info(this.getClass().getName() + " Processing subsequent box #: "+(i+1) + "/" + actualNumSubsequentBoxes);	
				int start = (i*numBoxLength);
				for (int ii = start; ii < (start + numBoxLength); ii++){ 
					subSequence1D[ii-start] = sequence1D[ii];
				}
				//Compute specific values************************************************
				if (this.choiceRadioButt_SurrBoxHurstType.equals("H (HK)")) {
					hkProcess.computeH(subSequence1D, hkN);
					H = hkProcess.getH();
				}
				if (this.choiceRadioButt_SurrBoxHurstType.equals("H (RS)")) {			
					hurstRS.computeH(subSequence1D, epsInterval);
					H = hurstRS.getH();
//					if (optShowPlot) {	
//						String preName = "R/S " +sequenceColumn.getHeader();
//						showPlot(hurstRS.getLnDataX(), hurstRS.getLnDataY(), preName, col, "ln(Window size)", "ln(R/S)", 1, hurstRS.getLnDataX().length);
//					}
				}	
				if (this.choiceRadioButt_SurrBoxHurstType.equals("H (SP)")) {	
					hurstSP.computeH(subSequence1D, spMaxLag);
					H = hurstSP.getH();
				}	
			
				resultValues[i]                               = H; // Hurst;
				//resultValues[(int)(i + numSubsequentBoxes)] = ?; //?		
				//***********************************************************************
			}	
		//********************************************************************************************************			
		} else if (sequenceRange.equals("Gliding box")){
			resultValues = new double[(int) (1*numGlidingBoxes)]; // H == 1*number of boxes  Dim R2 == two * number of boxes	
			for (int r = 0; r<resultValues.length; r++) resultValues[r] = Double.NaN;
			subSequence1D = new double[(int) numBoxLength];
			//number of boxes may be smaller because of NaNs or removed zeroes
			long actualNumGlidingBoxes = sequence1D.length - spinnerInteger_BoxLength + 1;
			double H = Double.NaN;
			hkProcess = new HKprocess(logService);
			hurstRS = new HurstRS();
			hurstSP = new HurstSP();
		
			//get sub-sequences and compute dimensions
			for (int i = 0; i < actualNumGlidingBoxes; i++) {
				logService.info(this.getClass().getName() + " Processing gliding box #: "+(i+1) + "/" + actualNumGlidingBoxes);	
				int start = i;
				for (int ii = start; ii < (start + numBoxLength); ii++){ 
					subSequence1D[ii-start] = sequence1D[ii];
				}	
				
				//Compute specific values************************************************
				if (choiceRadioButt_SurrBoxHurstType.equals("H (HK)")) {
					hkProcess.computeH(subSequence1D, hkN);
					H = hkProcess.getH();
				}
				if (choiceRadioButt_SurrBoxHurstType.equals("H (RS)")) {			
					hurstRS.computeH(subSequence1D, epsInterval);
					H = hurstRS.getH();
//					if (optShowPlot) {	
//						String preName = "R/S " +sequenceColumn.getHeader();
//						showPlot(hurstRS.getLnDataX(), hurstRS.getLnDataY(), preName, col, "ln(Window size)", "ln(R/S)", 1, hurstRS.getLnDataX().length);
//					}
				}	
				if (choiceRadioButt_SurrBoxHurstType.equals("H (SP)")) {	
					hurstSP.computeH(subSequence1D, spMaxLag);
					H = hurstSP.getH();
				}	
			
				resultValues[i]                               = H; // Hurst;
				//resultValues[(int)(i + numSubsequentBoxes)] = ?; //?	
				//***********************************************************************
			}
		}
		
		return new Container_ProcessMethod(resultValues);
		// Dim, R2, StdErr
		// Output
		// uiService.show(tableOutName, table);
	}

	// This method shows the double log plot
	private void showPlot(double[] lnDataX, double[] lnDataY, String preName, int col, String labelX, String labelY, int epsRegStart, int epsRegEnd) {
		if (lnDataX == null) {
			logService.info(this.getClass().getName() + " lnDataX == null, cannot display the plot!");
			return;
		}
		if (lnDataY == null) {
			logService.info(this.getClass().getName() + " lnDataY == null, cannot display the plot!");
			return;
		}
		if (col < 0) {
			logService.info(this.getClass().getName() + " col < 0, cannot display the plot!");
			return;
		}
		if (epsRegStart >= epsRegEnd) {
			logService.info(this.getClass().getName() + " epsRegStart >= epsRegEnd, cannot display the plot!");
			return;
		}
		if (epsRegEnd <= epsRegStart) {
			logService.info(this.getClass().getName() + " epsRegEnd <= epsRegStart, cannot display the plot!");
			return;
		}
		// String preName = "";
		if (preName == null) {
			preName += "Col" + String.format("%03d", col) + "-";
		}
		boolean isLineVisible = false; // ?
		Plot_RegressionFrame doubleLogPlot = DisplayRegressionPlotXY(lnDataX, lnDataY, isLineVisible,
				"Double log plot - ", preName + "-" + tableInName, labelX, labelY, "", epsRegStart, epsRegEnd);
		doubleLogPlotList.add(doubleLogPlot);
		
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
	 * @param epsRegStart                minimum value for regression range
	 * @param epsRegEnd                maximal value for regression range
	 * @param optDeleteExistingPlot option if existing plot should be deleted before
	 *                              showing a new plot
	 * @param interpolType          The type of interpolation
	 * @return RegressionPlotFrame
	 */
	private Plot_RegressionFrame DisplayRegressionPlotXY(double[] dataX, double[] dataY,
			boolean isLineVisible, String frameTitle, String plotLabel, String xAxisLabel, String yAxisLabel, String legendLabel,
			int epsRegStart, int epsRegEnd) {
		// jFreeChart
		Plot_RegressionFrame pl = new Plot_RegressionFrame(dataX, dataY, isLineVisible, frameTitle, plotLabel, xAxisLabel,
				yAxisLabel, legendLabel, epsRegStart, epsRegEnd);
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
		
		// open and display a sequence, waiting for the operation to finish.
		ij.command().run(Csaj1DOpener.class, true).get().getOutput(tableInName);
		//open and run Plugin
		ij.command().run(MethodHandles.lookup().lookupClass().getName(), true);
	}
}
