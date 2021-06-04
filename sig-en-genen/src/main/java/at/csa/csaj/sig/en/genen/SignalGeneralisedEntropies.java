/*-
 * #%L
 * Project: ImageJ signal plugin for computing generalizeed entropies
 * File: SignalGeneralisedEntropies.java
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

package at.csa.csaj.sig.en.genen;

import java.awt.Toolkit;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.UIManager;
import net.imagej.ImageJ;
import net.imagej.ops.OpService;
import net.imglib2.type.numeric.RealType;

import org.apache.commons.math3.random.EmpiricalDistribution;
import org.apache.commons.math3.special.Gamma;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.util.Precision;
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
import at.csa.csaj.commons.signal.algorithms.Surrogate;
import at.csa.csaj.commons.dialog.WaitingDialogWithProgressBar;
import at.csa.csaj.sig.open.SignalOpener;


/**
 * A {@link Command} plugin computing <Generalised entropies</a>
 * of a signal.
 *  <li>according to a review of Amigó, J.M., Balogh, S.G., Hernández, S., 2018. A Brief Review of Generalized Entropies. Entropy 20, 813. https://doi.org/10.3390/e20110813
 * <li>and to: Tsallis Introduction to Nonextensive Statistical Mechanics, 2009, S105-106
 * <li>(SE     according to Amigo etal. and Tsekouras, G.A.; Tsallis, C. Generalized entropy arising from a distribution of q indices. Phys. Rev. E 2005,)
 * <li>SE      according to N. R. Pal and S. K. Pal: Object background segmentation using new definitions of entropy, IEEE Proc. 366 (1989), 284–295.
							and N. R. Pal and S. K. Pal, Entropy: a new definitions and its applications, IEEE Transactions on systems, Man and Cybernetics, 21(5), 1260-1270, 1999
 * <li>H       according to Amigo etal.
 * <li>Renyi   according to Amigo etal.
 * <li>Tsallis according to Amigo etal.
 * <li>SNorm   according to Tsallis Introduction to Nonextensive Statistical Mechanics, 2009, S105-106
 * <li>SEscort according to Tsallis Introduction to Nonextensive Statistical Mechanics, 2009, S105-106
 * <li>SEta    according to Amigo etal. and Anteneodo, C.; Plastino, A.R. Maximum entropy approach to stretched exponential probability distributions. J. Phys. A Math. Gen. 1999, 32, 1089–1098.	
 * <li>SKappa  according to Amigo etal. and Kaniadakis, G. Statistical mechanics in the context of special relativity. Phys. Rev. E 2002, 66, 056125
 * <li>SB      according to Amigo etal. and Curado, E.M.; Nobre, F.D. On the stability of analytic entropic forms. Physica A 2004, 335, 94–106.
 * <li>SBeta   according to Amigo etal. and Shafee, F. Lambert function and a new non-extensive form of entropy. IMA J. Appl. Math. 2007, 72, 785–800.
 * <li>SGamma  according to Amigo etal. and Tsallis Introduction to Nonextensive Statistical Mechanics, 2009, S61
 * 
 * 
 * *****************************************************
 * For Surrogates and Subsequent/Gliding boxes:
 * Chose an entropy type
 * Set min and max to the same value.
 * Actually the min value is taken for computation
 * ******************************************************  
 * 
 */
@Plugin(type = InteractiveCommand.class, headless = true, menuPath = "Plugins>ComsystanJ>Signal>Generalised entropies")
public class SignalGeneralisedEntropies<T extends RealType<T>> extends InteractiveCommand implements Command, Previewable { // non blocking  GUI
//public class SignalGeneralisedEntropies<T extends RealType<T>> implements Command {	//modal GUI

	private static final String PLUGIN_LABEL            = "<html><b>Generalised entropies</b></html>";
	private static final String SPACE_LABEL             = "";
	private static final String ENTROPYOPTIONS_LABEL    = "<html><b>Entropy options</b></html>";
	private static final String ANALYSISOPTIONS_LABEL   = "<html><b>Analysis options</b></html>";
	private static final String BACKGROUNDOPTIONS_LABEL = "<html><b>Background option</b></html>";
	private static final String DISPLAYOPTIONS_LABEL    = "<html><b>Display option</b></html>";
	private static final String PROCESSOPTIONS_LABEL    = "<html><b>Process options</b></html>";
	
	private static double[] signal1D;
	private static double[] domain1D;
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
	
	private static int   minQ;
	private static int   maxQ;
	private static float minEta;
	private static float maxEta;
	private static float minKappa;
	private static float maxKappa;
	private static float minB;
	private static float maxB;
	private static float minBeta;
	private static float maxBeta;
	private static float minGamma;
	private static float maxGamma;
	
	private static int   stepQ;
	private static float stepEta;
	private static float stepKappa;
	private static float stepB;
	private static float stepBeta;
	private static float stepGamma;
	
	private static int numQ;
	private static int numEta;
	private static int numKappa;
	private static int numB;
	private static int numBeta;
	private static int numGamma;
	
	// data arrays		
	private static double   genEntSE;
	private static double[] genEntH;	
	private static double[] genEntRenyi;
	private static double[] genEntTsallis;	
	private static double[] genEntSNorm;	
	private static double[] genEntSEscort;	
	private static double[] genEntSEta;	
	private static double[] genEntSKappa;	
	private static double[] genEntSB;	
	private static double[] genEntSBeta;	
	private static double[] genEntSGamma;
	
	double[] probabilities         = null; //pi's
	double[] probabilitiesSurrMean = null; //pi's
	
	double[] resultValues;
	
	private static final String tableOutName = "Table - Generalized entropies";
	
	WaitingDialogWithProgressBar dlgProgress;
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
	private final String labelEntropyOptions = ENTROPYOPTIONS_LABEL;
	
	@Parameter(label = "pixelPercentage type",
			description = "Selection of pixelPercentage type",
			style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
			choices = {"Actual", "Pairwise differences", "Sum of differences", "SD"}, 
			//persist  = false,  //restore previous value default = true
			initializer = "initialPixelPercentageType",
			callback = "callbackPixelPercentageType")
	private String choiceRadioButt_pixelPercentageType;

	@Parameter(label = "lag", description = "delta for computation", style = NumberWidget.SPINNER_STYLE, min = "1", max = "1000000", stepSize = "1",
			   persist = false, // restore  previous value  default  =  true
			   initializer = "initialLag", callback = "callbackLag")
	private int spinnerInteger_Lag;
	
	@Parameter(label = "(Renyi/Tsallis/SNorm/SEscort) Min q", description = "minimal Q for Renyi and Tsallis entropies", style = NumberWidget.SPINNER_STYLE, min = "-1000", max = "1000", stepSize = "1",
			   persist = false, // restore  previous value  default  =  true
			   initializer = "initialMinQ", callback = "callbackMinQ")
	private int spinnerInteger_MinQ;

	@Parameter(label = "(Renyi/Tsallis/SNorm/SEscort) Max q", description = "maximal Q for Renyi Tsallis entropies", style = NumberWidget.SPINNER_STYLE, min = "-1000", max = "1000", stepSize = "1",
			   persist = false, //restore previous value default = true
			   initializer = "initialMaxQ", callback = "callbackMaxQ")
	private int spinnerInteger_MaxQ;

	@Parameter(label = "(SEta) Min eta", description = "minimal Eta for SEta entropies", style = NumberWidget.SPINNER_STYLE, min = "0f", max = "100000f", stepSize = "0.1",
			   persist = false, // restore  previous value  default  =  true
			   initializer = "initialMinEta", callback = "callbackMinEta")
	private float spinnerFloat_MinEta;
	
	@Parameter(label = "(SEta) Max eta", description = "maximal Eta for SEta entropies", style = NumberWidget.SPINNER_STYLE, min = "0f", max = "100000f", stepSize = "0.1",
			   persist = false, // restore  previous value  default  =  true
			   initializer = "initialMaxEta", callback = "callbackMaxEta")
	private float spinnerFloat_MaxEta;
	
	@Parameter(label = "(SKappa) Min kappa", description = "minimal Kappa for SKappa entropies", style = NumberWidget.SPINNER_STYLE, min = "0.000000000001f", max = "0.999999999999f", stepSize = "0.1",
			   persist = false, // restore  previous value  default  =  true
			   initializer = "initialMinKappa", callback = "callbackMinKappa")
	private float spinnerFloat_MinKappa;
	
	@Parameter(label = "(SKappa) Max kappa", description = "maximal Kappa for SKappa entropies", style = NumberWidget.SPINNER_STYLE, min = "0.000000000001f", max = "0.999999999999f", stepSize = "0.1",
			   persist = false, // restore  previous value  default  =  true
			   initializer = "initialMaxKappa", callback = "callbackMaxKappa")
	private float spinnerFloat_MaxKappa;
	
	@Parameter(label = "(SB) Min b", description = "minimal B for SB entropies", style = NumberWidget.SPINNER_STYLE, min = "0.000000000001f", max = "100000f", stepSize = "1",
			   persist = false, // restore  previous value  default  =  true
			   initializer = "initialMinB", callback = "callbackMinB")
	private float spinnerFloat_MinB;
	
	@Parameter(label = "(SB) Max b", description = "maximal B for SB entropies", style = NumberWidget.SPINNER_STYLE, min = "1f", max = "100000f", stepSize = "1",
			   persist = false, // restore  previous value  default  =  true
			   initializer = "initialMaxB", callback = "callbackMaxB")
	private float spinnerFloat_MaxB;
	
	@Parameter(label = "(SBeta) Min beta", description = "minimal Beta for SBeta entropies", style = NumberWidget.SPINNER_STYLE, min = "-100000f", max = "100000f", stepSize = "0.1",
			   persist = false, // restore  previous value  default  =  true
			   initializer = "initialMinBeta", callback = "callbackMinBeta")
	private float spinnerFloat_MinBeta;
	
	@Parameter(label = "(SBeta) Max beta", description = "maximal Beta for SBeta entropies", style = NumberWidget.SPINNER_STYLE, min = "-100000f", max = "100000f", stepSize = "0.1",
			   persist = false, // restore  previous value  default  =  true
			   initializer = "initialMaxBeta", callback = "callbackMaxBeta")
	private float spinnerFloat_MaxBeta;
	
	@Parameter(label = "(SGamma) Min gamma", description = "minimal Gamma for SGamma entropies", style = NumberWidget.SPINNER_STYLE, min = "0f", max = "100000f", stepSize = "1",
			   persist = false, // restore  previous value  default  =  true
			   initializer = "initialMinGamma", callback = "callbackMinGamma")
	private float spinnerFloat_MinGamma;
	
	@Parameter(label = "(SGamma) Max gamma", description = "maximal Gamma for SGamma entropies", style = NumberWidget.SPINNER_STYLE, min = "-0f", max = "100000f", stepSize = "0.1",
			   persist = false, // restore  previous value  default  =  true
			   initializer = "initialMaxGamma", callback = "callbackMaxGamma")
	private float spinnerFloat_MaxGamma;
	
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
			   min = "1", max = "9999999999999999999", stepSize = "1",
			   persist = false, // restore  previous value  default  =  true
			   initializer = "initialNumSurrogates", callback = "callbackNumSurrogates")
	private int spinnerInteger_NumSurrogates;
	
	@Parameter(label = "Box length", description = "Length of subsequent or gliding box", style = NumberWidget.SPINNER_STYLE, 
			   min = "2", max = "9999999999999999999", stepSize = "1",
			   persist = false, // restore  previous value  default  =  true
			   initializer = "initialBoxLength", callback = "callbackBoxLength")
	private int spinnerInteger_BoxLength;
	
	@Parameter(label = "(Surr/Box) Entropy type",
			description = "Entropy for Surrogates, Subsequent boxes or Gliding box",
			style = ChoiceWidget.LIST_BOX_STYLE,
			choices = {"SE", "H1", "H2", "H3", "Renyi", "Tsallis", "SNorm", "SEscort", "SEta", "SKappa", "SB", "SBeta", "SGamma"}, 
			//persist  = false,  //restore previous value default = true
			initializer = "initialEntropyType",
			callback = "callbackEntropyType")
	private String choiceRadioButt_EntropyType;
	
	//-----------------------------------------------------------------------------------------------------
//	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
//	private final String labelBackgroundOptions = BACKGROUNDOPTIONS_LABEL;

	@Parameter(label = "Remove zero values", persist = false,
		       callback = "callbackRemoveZeroes")
	private boolean booleanRemoveZeroes;
	
	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelDisplayOptions = DISPLAYOPTIONS_LABEL;

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

	@Parameter(label = "Process all columns", callback = "callbackProcessAllColumns")
	private Button buttonProcessAllColumns;


	// ---------------------------------------------------------------------
	// The following initialzer functions set initial values
	
	protected void initialPixelPercentageType() {
		choiceRadioButt_pixelPercentageType = "Actual"; //"Actual", "Pairwise differences", "Sum of differences", "SD"
	} 
	
	protected void initialLag() {
		spinnerInteger_Lag = 1;
	}
	
	protected void initialMinQ() {
		spinnerInteger_MinQ = -5;
	}
	
	protected void initialMaxQ() {
		spinnerInteger_MaxQ = 5;
	}
	
	protected void initialMinEta() {
		spinnerFloat_MinEta = 0.1f;
	}
	
	protected void initialMaxEta() {
		spinnerFloat_MaxEta = 1f;
	}
	protected void initialMinKappa() {
		spinnerFloat_MinKappa = 0.1f;
	}
	
	protected void initialMaxKappa() {
		spinnerFloat_MaxKappa = 0.9f;
	}
	
	protected void initialMinB() {
		spinnerFloat_MinB = 1.0f;
	}
	
	protected void initialMaxB() {
		spinnerFloat_MaxB = 10.0f;
	}
	
	protected void initialMinBeta() {
		spinnerFloat_MinBeta = 0.5f;
	}
	
	protected void initialMaxBeta() {
		spinnerFloat_MaxBeta = 1.5f;
	}
	
	protected void initialMinGamma() {
		spinnerFloat_MinGamma = 0.1f;
	}
	
	protected void initialMaxGamma() {
		spinnerFloat_MaxGamma = 1.0f;
	}
	
	protected void initialAnalysisType() {
		choiceRadioButt_AnalysisType = "Entire signal";
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
	
	protected void initialEntropyType() {
		choiceRadioButt_EntropyType = "Renyi"; //"SE", "H1", "H2", "H3", "Renyi", "Tsallis", "SNorm", "SEscort", "SEta", "SKappa", "SB", "SBeta", "SGamma"
	} 
	
	protected void initialRemoveZeroes() {
		booleanRemoveZeroes = false;
	}	
	
	protected void initialOverwriteDisplays() {
    	booleanOverwriteDisplays = true;
}
	
	protected void initialNumColumn() {
		spinnerInteger_NumColumn = 1;
	}

	// The following method is known as "callback" which gets executed
	// whenever the value of a specific linked parameter changes.
	
	/** Executed whenever the {@link #choiceRadioButt_pixelPercentageType} parameter changes. */
	protected void callbackPixelPercentageType() {
		logService.info(this.getClass().getName() + " Propability type set to " + choiceRadioButt_pixelPercentageType);
	}
	

	/** Executed whenever the {@link #spinInteger_Lag} parameter changes. */
	protected void callbackLag() {
		logService.info(this.getClass().getName() + " Lag set to " + spinnerInteger_Lag);
	}
	
	/** Executed whenever the {@link #spinInteger_MinQ} parameter changes. */
	protected void callbackMinQ() {
		logService.info(this.getClass().getName() + " Minimal Q set to " + spinnerInteger_MinQ);
	}

	/** Executed whenever the {@link #spinInteger_MaxQ} parameter changes. */
	protected void callbackMaxQ() {
		logService.info(this.getClass().getName() + " Maximal Q set to " + spinnerInteger_MaxQ);
	}
	
	/** Executed whenever the {@link #spinFloat_MinEta} parameter changes. */
	protected void callbackMinEta() {
		logService.info(this.getClass().getName() + " Minimal Eta set to " + spinnerFloat_MinEta);
	}
	
	/** Executed whenever the {@link #spinFloat_MaxEta} parameter changes. */
	protected void callbackMaxEta() {
		logService.info(this.getClass().getName() + " Maximal Eta set to " + spinnerFloat_MaxEta);
	}
	
	/** Executed whenever the {@link #spinFloat_MinKappa} parameter changes. */
	protected void callbackMinKappa() {
		logService.info(this.getClass().getName() + " Minimal Kappa set to " + spinnerFloat_MinKappa);
	}
	
	/** Executed whenever the {@link #spinFloat_MaxKappa} parameter changes. */
	protected void callbackMaxKapa() {
		logService.info(this.getClass().getName() + " Maximal Kappa set to " + spinnerFloat_MaxKappa);
	}
	
	/** Executed whenever the {@link #spinFloat_MinB} parameter changes. */
	protected void callbackMinB() {
		logService.info(this.getClass().getName() + " Minimal B set to " + spinnerFloat_MinB);
	}
	
	/** Executed whenever the {@link #spinFloat_MaxB} parameter changes. */
	protected void callbackMaxB() {
		logService.info(this.getClass().getName() + " Maximal B set to " + spinnerFloat_MaxB);
	}
	
	/** Executed whenever the {@link #spinFloat_MinBeta} parameter changes. */
	protected void callbackMinBeta() {
		logService.info(this.getClass().getName() + " Minimal Beta set to " + spinnerFloat_MinBeta);
	}
	
	/** Executed whenever the {@link #spinFloat_MaxBeta} parameter changes. */
	protected void callbackMaxBeta() {
		logService.info(this.getClass().getName() + " Maximal Beta set to " + spinnerFloat_MaxBeta);
	}
	
	/** Executed whenever the {@link #spinFloat_MinGamma} parameter changes. */
	protected void callbackMinGamma() {
		logService.info(this.getClass().getName() + " Minimal Gamma set to " + spinnerFloat_MinGamma);
	}
	
	/** Executed whenever the {@link #spinFloat_MaxGamma} parameter changes. */
	protected void callbackMaxGamma() {
		logService.info(this.getClass().getName() + " Maximal Gamma set to " + spinnerFloat_MaxGamma);
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

	/** Executed whenever the {@link #choiceRadioButt_EntropyType} parameter changes. */
	protected void callbackEntropyType() {
		logService.info(this.getClass().getName() + " Entropy type for surrogate or box set to " + choiceRadioButt_EntropyType);
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
	 * Executed whenever the {@link #buttonProcessSingleColumn} button is pressed.
	 */
	protected void callbackProcessSingleColumn() {
		//prepare  executer service
		exec = Executors.newSingleThreadExecutor();
		
		//dlgProgress = new WaitingDialogWithProgressBar("<html>Computing Generalised entropies, please wait...<br>Open console window for further info.</html>");
		dlgProgress = new WaitingDialogWithProgressBar("Computing Generalised entropies, please wait... Open console window for further info.",
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
            		//int activeColumnIndex = getActiveColumnIndex();
            		if (spinnerInteger_NumColumn <= numColumns) processSingleInputColumn(spinnerInteger_NumColumn - 1);
            		dlgProgress.addMessage("Processing finished!");		
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
		
		//dlgProgress = new WaitingDialogWithProgressBar("<html>Computing Generalised entropies, please wait...<br>Open console window for further info.</html>");
		dlgProgress = new WaitingDialogWithProgressBar("Computing Generalised entropies, please wait... Open console window for further info.",
																					logService, true, exec); //isCanceable = true, because processAllInputSignalss(dlgProgress) listens to exec.shutdown 
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
		tableResult.add(new IntColumn("Surrogates #"));
		tableResult.add(new IntColumn("Box length"));
		tableResult.add(new BoolColumn("Zeroes removed"));
	
		tableResult.add(new GenericColumn("pixelPercentage type"));		
		tableResult.add(new IntColumn("Lag"));
		
		minQ       = spinnerInteger_MinQ;
		maxQ       = spinnerInteger_MaxQ;
		minEta     = Precision.round(spinnerFloat_MinEta, 1); //round to 1 decimal, because sometimes float is not exact
		maxEta     = Precision.round(spinnerFloat_MaxEta, 1);
		minKappa   = Precision.round(spinnerFloat_MinKappa, 1);
		maxKappa   = Precision.round(spinnerFloat_MaxKappa, 1);
		minB       = Precision.round(spinnerFloat_MinB, 1);
		maxB       = Precision.round(spinnerFloat_MaxB, 1);
		minBeta    = Precision.round(spinnerFloat_MinBeta, 1);
		maxBeta    = Precision.round(spinnerFloat_MaxBeta, 1);
		minGamma   = Precision.round(spinnerFloat_MinGamma, 1);
		maxGamma   = Precision.round(spinnerFloat_MaxGamma, 1);
		
		stepQ     = 1;
		stepEta   = 0.1f;
		stepKappa = 0.1f;
		stepB     = 1.0f;
		stepBeta  = 0.1f;
		stepGamma = 0.1f;
	
		numQ     =        (maxQ     - minQ)    /stepQ     + 1;
		numEta   = (int) ((maxEta   - minEta)  /stepEta   + 1);
		numKappa = (int) ((maxKappa - minKappa)/stepKappa + 1);
		numB     = (int) ((maxB     - minB)    /stepB     + 1);
		numBeta  = (int) ((maxBeta  - minBeta) /stepBeta  + 1);
		numGamma = (int) ((maxGamma - minGamma)/stepGamma + 1);
			
		//"Entire signal", "Subsequent boxes", "Gliding box" 
		if (choiceRadioButt_AnalysisType.equals("Entire signal")){
			
			if (choiceRadioButt_SurrogateType.equals("No surrogates")) {
				//"SE", "H1", "H2", "H3", "Renyi", "Tsallis", "SNorm", "SEscort", "SEta", "SKappa", "SB", "SBeta", "SGamma"
				tableResult.add(new DoubleColumn("SE"));
				tableResult.add(new DoubleColumn("H1"));
				tableResult.add(new DoubleColumn("H2"));
				tableResult.add(new DoubleColumn("H3"));
				for (int q = 0; q < numQ; q++) tableResult.add(new DoubleColumn("Renyi_q"   + (minQ + q))); 
				for (int q = 0; q < numQ; q++) tableResult.add(new DoubleColumn("Tsallis_q" + (minQ + q))); 
				for (int q = 0; q < numQ; q++) tableResult.add(new DoubleColumn("SNorm_q"   + (minQ + q))); 
				for (int q = 0; q < numQ; q++) tableResult.add(new DoubleColumn("SEscort_q" + (minQ + q))); 
				for (int e = 0; e < numEta;   e++)  tableResult.add(new DoubleColumn("SEta_e"    + String.format ("%.1f", minEta   + e*stepEta)));
				for (int k = 0; k < numKappa; k++)  tableResult.add(new DoubleColumn("SKappa_k"  + String.format ("%.1f", minKappa + k*stepKappa))); 
				for (int b = 0; b < numB;     b++)  tableResult.add(new DoubleColumn("SB_b"      + String.format ("%.1f", minB     + b*stepB))); 
				for (int be= 0; be< numBeta;  be++) tableResult.add(new DoubleColumn("SBeta_be"  + String.format ("%.1f", minBeta  +be*stepBeta))); 
				for (int g = 0; g < numGamma; g++)  tableResult.add(new DoubleColumn("SGamma_g"  + String.format ("%.1f", minGamma + g*stepGamma))); 
	
			} else { //Surrogates	
				if (choiceRadioButt_EntropyType.equals("SE")) {
					tableResult.add(new DoubleColumn("SE"));
					tableResult.add(new DoubleColumn("SE_Surr"));  //Mean surrogate value	
					for (int s = 0; s < numSurrogates; s++) tableResult.add(new DoubleColumn("SE_Surr#"+(s+1))); 
				}
				else if (choiceRadioButt_EntropyType.equals("H1")) {
					tableResult.add(new DoubleColumn("H1"));
					tableResult.add(new DoubleColumn("H1_Surr"));  //Mean surrogate value	
					for (int s = 0; s < numSurrogates; s++) tableResult.add(new DoubleColumn("H1_Surr#"+(s+1))); 
				}
				else if (choiceRadioButt_EntropyType.equals("H2")) {
					tableResult.add(new DoubleColumn("H2"));
					tableResult.add(new DoubleColumn("H2_Surr"));  //Mean surrogate value	
					for (int s = 0; s < numSurrogates; s++) tableResult.add(new DoubleColumn("H2_Surr#"+(s+1))); 
				}
				else if (choiceRadioButt_EntropyType.equals("H3")) {
					tableResult.add(new DoubleColumn("H3"));
					tableResult.add(new DoubleColumn("H3_Surr"));  //Mean surrogate value	 
					for (int s = 0; s < numSurrogates; s++) tableResult.add(new DoubleColumn("H3_Surr#"+(s+1))); 
				}
				else if (choiceRadioButt_EntropyType.equals("Renyi")) {
					tableResult.add(new DoubleColumn("Renyi_q"   + minQ)); 
					tableResult.add(new DoubleColumn("Renyi_q"+minQ+"_Surr"));  //Mean surrogate value	
					for (int s = 0; s < numSurrogates; s++) tableResult.add(new DoubleColumn("Renyi_q"+minQ+"_Surr#"+(s+1))); 
				}
				else if (choiceRadioButt_EntropyType.equals("Tsallis")) {
					tableResult.add(new DoubleColumn("Tsallis_q" + minQ)); 
					tableResult.add(new DoubleColumn("Tsallis_q"+minQ+"_Surr"));  //Mean surrogate value	
					for (int s = 0; s < numSurrogates; s++) tableResult.add(new DoubleColumn("Tsallis_q"+minQ+"_Surr#"+(s+1))); 
				}
				else if (choiceRadioButt_EntropyType.equals("SNorm")) {
					tableResult.add(new DoubleColumn("SNorm_q"   + minQ));
					tableResult.add(new DoubleColumn("SNorm_q"+minQ+"_Surr"));  //Mean surrogate value	
					for (int s = 0; s < numSurrogates; s++) tableResult.add(new DoubleColumn("SNorm_q"+minQ+"_Surr#"+(s+1))); 
				}
				else if (choiceRadioButt_EntropyType.equals("SEscort")) {
					tableResult.add(new DoubleColumn("SEscort_q" + minQ)); 
					tableResult.add(new DoubleColumn("SEscort_q"+minQ+"_Surr"));  //Mean surrogate value	
					for (int s = 0; s < numSurrogates; s++) tableResult.add(new DoubleColumn("SEscort_q"+minQ+"_Surr#"+(s+1))); 
				}
				else if (choiceRadioButt_EntropyType.equals("SEta")) {
					tableResult.add(new DoubleColumn("SEta_e"    + String.format ("%.1f", minEta)));
					tableResult.add(new DoubleColumn("SEta_e"+minEta+"_Surr"));  //Mean surrogate value	
					for (int s = 0; s < numSurrogates; s++) tableResult.add(new DoubleColumn("SEta_e"+minEta+"_Surr#"+(s+1))); 
				}
				else if (choiceRadioButt_EntropyType.equals("SKappa")) {
					tableResult.add(new DoubleColumn("SKappa_k"  + String.format ("%.1f", minKappa))); 
					tableResult.add(new DoubleColumn("SKappa_k"+minKappa+"_Surr"));  //Mean surrogate value	 
					for (int s = 0; s < numSurrogates; s++) tableResult.add(new DoubleColumn("SKappa_k"+minKappa+"_Surr#"+(s+1))); 
				}
				else if (choiceRadioButt_EntropyType.equals("SB")) {
					tableResult.add(new DoubleColumn("SB_b"      + String.format ("%.1f", minB))); 
					tableResult.add(new DoubleColumn("SB_b"+minB+"_Surr"));  //Mean surrogate value	
					for (int s = 0; s < numSurrogates; s++) tableResult.add(new DoubleColumn("SB_b"+minB+"_Surr#"+(s+1))); 
				}
				else if (choiceRadioButt_EntropyType.equals("SBeta")) {
					tableResult.add(new DoubleColumn("SBeta_be"  + String.format ("%.1f", minBeta))); 
					tableResult.add(new DoubleColumn("SBeta_be"+minBeta+"_Surr"));  //Mean surrogate value	 
					for (int s = 0; s < numSurrogates; s++) tableResult.add(new DoubleColumn("SBeta_be"+minBeta+"_Surr#"+(s+1))); 
				}
				else if (choiceRadioButt_EntropyType.equals("SGamma")) {
					tableResult.add(new DoubleColumn("SGamma_g"  + String.format ("%.1f", minGamma))); 
					tableResult.add(new DoubleColumn("SGamma_g"+minGamma+"_Surr"));  //Mean surrogate value	 
					for (int s = 0; s < numSurrogates; s++) tableResult.add(new DoubleColumn("SGamma_g"+minGamma+"_Surr#"+(s+1))); 
				}
			}
		} 
		else if (choiceRadioButt_AnalysisType.equals("Subsequent boxes")){
			String entropyHeader = "";
			if      (choiceRadioButt_EntropyType.equals("SE"))      {entropyHeader = "SE";}
			else if (choiceRadioButt_EntropyType.equals("H1"))      {entropyHeader = "H1";}
			else if (choiceRadioButt_EntropyType.equals("H2"))      {entropyHeader = "H2";}
			else if (choiceRadioButt_EntropyType.equals("H3"))      {entropyHeader = "H3";}
			else if (choiceRadioButt_EntropyType.equals("Renyi"))   {entropyHeader = "Renyi_q"+minQ;}
			else if (choiceRadioButt_EntropyType.equals("Tsallis")) {entropyHeader = "Tsallis_q"+minQ;}
			else if (choiceRadioButt_EntropyType.equals("SNorm"))   {entropyHeader = "SNorm_q"+minQ;}
			else if (choiceRadioButt_EntropyType.equals("SEscort")) {entropyHeader = "SEescort_q"+minQ;}
			else if (choiceRadioButt_EntropyType.equals("SEta"))    {entropyHeader = "SEta_e"+minEta;}
			else if (choiceRadioButt_EntropyType.equals("SKappa"))  {entropyHeader = "SKappa_e"+minKappa;}
			else if (choiceRadioButt_EntropyType.equals("SB"))      {entropyHeader = "SB_b"+minB;}
			else if (choiceRadioButt_EntropyType.equals("SBeta"))   {entropyHeader = "SBeta_be"+minBeta;}
			else if (choiceRadioButt_EntropyType.equals("SGamma"))  {entropyHeader = "SGamma_g"+minGamma;}
				
			for (int n = 1; n <= numSubsequentBoxes; n++) {
				tableResult.add(new DoubleColumn(entropyHeader+"-#" + n));	
			}	
		}
		else if (choiceRadioButt_AnalysisType.equals("Gliding box")){
			String entropyHeader = "";
			if      (choiceRadioButt_EntropyType.equals("SE"))      {entropyHeader = "SE";}
			else if (choiceRadioButt_EntropyType.equals("H1"))      {entropyHeader = "H1";}
			else if (choiceRadioButt_EntropyType.equals("H2"))      {entropyHeader = "H2";}
			else if (choiceRadioButt_EntropyType.equals("H3"))      {entropyHeader = "H3";}
			else if (choiceRadioButt_EntropyType.equals("Renyi"))   {entropyHeader = "Renyi_q"+minQ;}
			else if (choiceRadioButt_EntropyType.equals("Tsallis")) {entropyHeader = "Tsallis_q"+minQ;}
			else if (choiceRadioButt_EntropyType.equals("SNorm"))   {entropyHeader = "SNorm_q"+minQ;}
			else if (choiceRadioButt_EntropyType.equals("SEscort")) {entropyHeader = "SEescort_q"+minQ;}
			else if (choiceRadioButt_EntropyType.equals("SEta"))    {entropyHeader = "SEta_e"+minEta;}
			else if (choiceRadioButt_EntropyType.equals("SKappa"))  {entropyHeader = "SKappa_e"+minKappa;}
			else if (choiceRadioButt_EntropyType.equals("SB"))      {entropyHeader = "SB_b"+minB;}
			else if (choiceRadioButt_EntropyType.equals("SBeta"))   {entropyHeader = "SBeta_be"+minBeta;}
			else if (choiceRadioButt_EntropyType.equals("SGamma"))  {entropyHeader = "SGamma_g"+minGamma;}
		
			for (int n = 1; n <= numGlidingBoxes; n++) {
				tableResult.add(new DoubleColumn(entropyHeader+"-#" + n));	
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
				if (display.getName().equals(tableOutName))
					display.close();
			}
		}
	}

	/** This method takes the column at position c and computes results. 
	 * 
	 */
	private void processSingleInputColumn (int c) throws InterruptedException {
		
		long startTime = System.currentTimeMillis();
		
		// Compute result values
		double[] resultValues = process(tableIn, c); 
		// 0 Entropy
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
				// 0 Entropy
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
		
		// 0 Entropy
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
		
		tableResult.set(7, row, choiceRadioButt_pixelPercentageType);    // Lag
		tableResult.set(8, row, spinnerInteger_Lag);    // Lag
		tableColLast = 8;
		
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
			tableColStart = tableColLast +1;
			tableColEnd = (int) (tableColStart + 1 * numSubsequentBoxes); //1 or 2  for 1 or 2 parameters
			for (int c = tableColStart; c < tableColEnd; c++ ) {
				tableResult.set(c, row, resultValues[c-tableColStart]);
			}	
		}
		else if (choiceRadioButt_AnalysisType.equals("Gliding box")){
			tableColStart = tableColLast +1;
			tableColEnd = (int) (tableColStart + 1 * numGlidingBoxes); //1 or 2 for 1 or 2 parameters 
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
	
		String  analysisType  = choiceRadioButt_AnalysisType;
		String  surrType      = choiceRadioButt_SurrogateType;
		int     boxLength     = spinnerInteger_BoxLength;
		int     numDataPoints = dgt.getRowCount();
		String  probType      = choiceRadioButt_pixelPercentageType;
		int     numLag        = spinnerInteger_Lag;
		
		boolean removeZeores  = booleanRemoveZeroes;
		
		//min max and step values are already set in the table header generation method
		
		// data arrays		
		genEntSE      = 0.0f;
		genEntH       = new double[3];	
		genEntRenyi   = new double[numQ];
		genEntTsallis = new double[numQ];	
		genEntSNorm   = new double[numQ];	
		genEntSEscort = new double[numQ];	
		genEntSEta    = new double[numEta];	
		genEntSKappa  = new double[numKappa];	
		genEntSB      = new double[numB];	
		genEntSBeta   = new double[numBeta];	
		genEntSGamma  = new double[numGamma];
		
		int numOfEntropies = 1 + 3 + 4*numQ + numEta + numKappa + numB + numBeta + numGamma;
		
		resultValues = new double[numOfEntropies]; // 
		for (int r = 0; r < resultValues.length; r++) resultValues[r] = Float.NaN;
		
		//******************************************************************************************************
		domain1D  = new double[numDataPoints];
		signal1D = new double[numDataPoints];
		
		signalColumn = dgt.get(col);
		for (int n = 0; n < numDataPoints; n++) {
			domain1D[n]  = n+1;
			signal1D[n] = Double.valueOf((Double)signalColumn.get(n));
		}	
		
		signal1D = removeNaN(signal1D);
		if (removeZeores) signal1D = removeZeroes(signal1D);
		
		//May be smaller than before
		numDataPoints = signal1D.length;
		
		logService.info(this.getClass().getName() + " Column #: "+ (col+1) + "  " + signalColumn.getHeader() + "  Size of signal = " + numDataPoints);	
		
		double entropyValue = Float.NaN;
		
		//"Entire signal", "Subsequent boxes", "Gliding box" 
		//********************************************************************************************************
		if (analysisType.equals("Entire signal")){	
	
			if (surrType.equals("No surrogates")) {		
				resultValues = new double[numOfEntropies]; // 		
				for (int r = 0; r < resultValues.length; r++) resultValues[r] = Float.NaN;
				
				logService.info(this.getClass().getName() + " Column #: "+ (col+1) + "  " + signalColumn.getHeader() + "  Size of signal = " + signal1D.length);	
				
				probabilities = compProbabilities(signal1D, numLag, probType);				
				genEntSE      = this.compSE();
				genEntH       = this.compH();	//H1 H2 H3 
				genEntRenyi   = this.compRenyi  (minQ, maxQ, numQ);
				genEntTsallis = this.compTsallis(minQ, maxQ, numQ);	
				genEntSNorm   = this.compSNorm  (minQ, maxQ, numQ);	
				genEntSEscort = this.compSEscort(minQ, maxQ, numQ);	
				genEntSEta    = this.compSEta   (minEta,   maxEta,   stepEta,   numEta);	
				genEntSKappa  = this.compSKappa (minKappa, maxKappa, stepKappa, numKappa);	
				genEntSB      = this.compSB     (minB,     maxB,     stepB,     numB);	
				genEntSBeta   = this.compSBeta  (minBeta,  maxBeta,  stepBeta,  numBeta);	
				genEntSGamma  = this.compSGamma (minGamma, maxGamma, stepGamma, numGamma);

				resultValues[0] = genEntSE;
				resultValues[1] = genEntH[0];
				resultValues[2] = genEntH[1];
				resultValues[3] = genEntH[2];	
				int start = 3 + 1;
				int end   = start + numQ;
				for (int i = start; i < end; i++) resultValues[i] = genEntRenyi[i-start];	
				start = end;
				end   = start + numQ;
				for (int i = start; i < end; i++) resultValues[i] = genEntTsallis[i-start];
				start = end;
				end   = start + numQ;
				for (int i = start; i < end; i++) resultValues[i] = genEntSNorm[i-start];
				start = end;
				end   = start + numQ;
				for (int i = start; i < end; i++) resultValues[i] = genEntSEscort[i-start];
				start = end;
				end   = start + numEta;
				for (int i = start; i < end; i++) resultValues[i] = genEntSEta[i-start];
				start = end;
				end   = start + numKappa;
				for (int i = start; i < end; i++) resultValues[i] = genEntSKappa[i-start];
				start = end;
				end   = start + numB;
				for (int i = start; i < end; i++) resultValues[i] = genEntSB[i-start];
				start = end;
				end   = start + numBeta;
				for (int i = start; i < end; i++) resultValues[i] = genEntSBeta[i-start];
				start = end;
				end   = start + numGamma;
				for (int i = start; i < end; i++) resultValues[i] = genEntSGamma[i-start];
					
				//int lastMainResultsIndex =  numOfEntropies - 1;
				
			} else {
				resultValues = new double[1+1+1*numSurrogates]; // Entropy,  Entropy_SurrMean, Entropy_Surr#1, Entropy_Surr#2......
				
				probabilities = compProbabilities(signal1D, numLag, probType);		
				
				//"SE", "H1", "H2", "H3", "Renyi", "Tsallis", "SNorm", "SEscort", "SEta", "SKappa", "SB", "SBeta", "SGamma"
				if (choiceRadioButt_EntropyType.equals("SE"))           entropyValue = compSE();
				else if (choiceRadioButt_EntropyType.equals("H1"))      entropyValue = (compH())[0]; 
				else if (choiceRadioButt_EntropyType.equals("H2"))      entropyValue = (compH())[1];
				else if (choiceRadioButt_EntropyType.equals("H3"))      entropyValue = (compH())[2]; 
				else if (choiceRadioButt_EntropyType.equals("Renyi"))   entropyValue = (compRenyi  (minQ, maxQ, numQ))[0]; //value for min
				else if (choiceRadioButt_EntropyType.equals("Tsallis")) entropyValue = (compTsallis(minQ, maxQ, numQ))[0]; //value for min
				else if (choiceRadioButt_EntropyType.equals("SNorm"))   entropyValue = (compSNorm  (minQ, maxQ, numQ))[0]; //value for min
				else if (choiceRadioButt_EntropyType.equals("SEscort")) entropyValue = (compSEscort(minQ, maxQ, numQ))[0]; //value for min
				else if (choiceRadioButt_EntropyType.equals("SEta"))    entropyValue = (compSEta   (minEta,   maxEta,   stepEta,   numEta))[0];//value for min 
				else if (choiceRadioButt_EntropyType.equals("SKappa"))  entropyValue = (compSKappa (minKappa, maxKappa, stepKappa, numKappa))[0]; //value for min
				else if (choiceRadioButt_EntropyType.equals("SB"))      entropyValue = (compSB     (minB,     maxB,     stepB,     numB))[0]; //value for min
				else if (choiceRadioButt_EntropyType.equals("SBeta"))   entropyValue = (compSBeta  (minBeta,  maxBeta,  stepBeta,  numBeta))[0]; //value for min
				else if (choiceRadioButt_EntropyType.equals("SGamma"))  entropyValue = (compSGamma (minGamma, maxGamma, stepGamma, numGamma))[0]; //value for min

				resultValues[0] = entropyValue;
				int lastMainResultsIndex = 0;
				
				surrSignal1D = new double[signal1D.length];
				
				double sumEntropies   = 0.0f;
				Surrogate surrogate = new Surrogate();
				for (int s = 0; s < numSurrogates; s++) {
					//choices = {"No surrogates", "Shuffle", "Gaussian", "Random phase", "AAFT"}, 
					if      (surrType.equals("Shuffle"))      surrSignal1D = surrogate.calcSurrogateShuffle(signal1D);
					else if (surrType.equals("Gaussian"))     surrSignal1D = surrogate.calcSurrogateGaussian(signal1D);
					else if (surrType.equals("Random phase")) surrSignal1D = surrogate.calcSurrogateRandomPhase(signal1D);
					else if (surrType.equals("AAFT"))         surrSignal1D = surrogate.calcSurrogateAAFT(signal1D);
			
					probabilities = compProbabilities(surrSignal1D, numLag, probType);		
					
					//"SE", "H1", "H2", "H3", "Renyi", "Tsallis", "SNorm", "SEscort", "SEta", "SKappa", "SB", "SBeta", "SGamma"
					if (choiceRadioButt_EntropyType.equals("SE"))           entropyValue = compSE();
					else if (choiceRadioButt_EntropyType.equals("H1"))      entropyValue = (compH())[0]; 
					else if (choiceRadioButt_EntropyType.equals("H2"))      entropyValue = (compH())[1];
					else if (choiceRadioButt_EntropyType.equals("H3"))      entropyValue = (compH())[2]; 
					else if (choiceRadioButt_EntropyType.equals("Renyi"))   entropyValue = (compRenyi  (minQ, maxQ, numQ))[0]; //value for min
					else if (choiceRadioButt_EntropyType.equals("Tsallis")) entropyValue = (compTsallis(minQ, maxQ, numQ))[0]; //value for min
					else if (choiceRadioButt_EntropyType.equals("SNorm"))   entropyValue = (compSNorm  (minQ, maxQ, numQ))[0]; //value for min
					else if (choiceRadioButt_EntropyType.equals("SEscort")) entropyValue = (compSEscort(minQ, maxQ, numQ))[0]; //value for min
					else if (choiceRadioButt_EntropyType.equals("SEta"))    entropyValue = (compSEta   (minEta,   maxEta,   stepEta,   numEta))[0];//value for min 
					else if (choiceRadioButt_EntropyType.equals("SKappa"))  entropyValue = (compSKappa (minKappa, maxKappa, stepKappa, numKappa))[0]; //value for min
					else if (choiceRadioButt_EntropyType.equals("SB"))      entropyValue = (compSB     (minB,     maxB,     stepB,     numB))[0]; //value for min
					else if (choiceRadioButt_EntropyType.equals("SBeta"))   entropyValue = (compSBeta  (minBeta,  maxBeta,  stepBeta,  numBeta))[0]; //value for min
					else if (choiceRadioButt_EntropyType.equals("SGamma"))  entropyValue = (compSGamma (minGamma, maxGamma, stepGamma, numGamma))[0]; //value for min
						
					resultValues[lastMainResultsIndex + 2 + s] = entropyValue;
					sumEntropies += entropyValue;
				}
				resultValues[lastMainResultsIndex + 1] = sumEntropies/numSurrogates;
			}
		
		//********************************************************************************************************	
		} else if (analysisType.equals("Subsequent boxes")){
			resultValues = new double[(int) (2*numSubsequentBoxes)]; // Dim R2 == two * number of boxes		
			for (int r = 0; r<resultValues.length; r++) resultValues[r] = Float.NaN;
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
				probabilities = compProbabilities(subSignal1D, numLag, probType);	
				//"SE", "H1", "H2", "H3", "Renyi", "Tsallis", "SNorm", "SEscort", "SEta", "SKappa", "SB", "SBeta", "SGamma"
				if (choiceRadioButt_EntropyType.equals("SE"))           entropyValue = compSE();
				else if (choiceRadioButt_EntropyType.equals("H1"))      entropyValue = (compH())[0];
				else if (choiceRadioButt_EntropyType.equals("H2"))      entropyValue = (compH())[1];
				else if (choiceRadioButt_EntropyType.equals("H3"))      entropyValue = (compH())[2]; 
				else if (choiceRadioButt_EntropyType.equals("Renyi"))   entropyValue = (compRenyi  (minQ, maxQ, numQ))[0]; //value for min
				else if (choiceRadioButt_EntropyType.equals("Tsallis")) entropyValue = (compTsallis(minQ, maxQ, numQ))[0]; //value for min
				else if (choiceRadioButt_EntropyType.equals("SNorm"))   entropyValue = (compSNorm  (minQ, maxQ, numQ))[0]; //value for min
				else if (choiceRadioButt_EntropyType.equals("SEscort")) entropyValue = (compSEscort(minQ, maxQ, numQ))[0]; //value for min
				else if (choiceRadioButt_EntropyType.equals("SEta"))    entropyValue = (compSEta   (minEta,   maxEta,   stepEta,   numEta))[0];//value for min 
				else if (choiceRadioButt_EntropyType.equals("SKappa"))  entropyValue = (compSKappa (minKappa, maxKappa, stepKappa, numKappa))[0]; //value for min
				else if (choiceRadioButt_EntropyType.equals("SB"))      entropyValue = (compSB     (minB,     maxB,     stepB,     numB))[0]; //value for min
				else if (choiceRadioButt_EntropyType.equals("SBeta"))   entropyValue = (compSBeta  (minBeta,  maxBeta,  stepBeta,  numBeta))[0]; //value for min
				else if (choiceRadioButt_EntropyType.equals("SGamma"))  entropyValue = (compSGamma (minGamma, maxGamma, stepGamma, numGamma))[0]; 	//value for min
				resultValues[i] = entropyValue;			
				//***********************************************************************
			}	
		//********************************************************************************************************			
		} else if (analysisType.equals("Gliding box")){
			resultValues = new double[(int) (2*numGlidingBoxes)]; // Dim R2 == two * number of boxes	
			for (int r = 0; r<resultValues.length; r++) resultValues[r] = Float.NaN;
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
				probabilities = compProbabilities(subSignal1D, numLag, probType);	
				//"SE", "H1", "H2", "H3", "Renyi", "Tsallis", "SNorm", "SEscort", "SEta", "SKappa", "SB", "SBeta", "SGamma"
				if (choiceRadioButt_EntropyType.equals("SE"))           entropyValue = compSE();
				else if (choiceRadioButt_EntropyType.equals("H1"))      entropyValue = (compH())[0];
				else if (choiceRadioButt_EntropyType.equals("H2"))      entropyValue = (compH())[1];
				else if (choiceRadioButt_EntropyType.equals("H3"))      entropyValue = (compH())[2]; 
				else if (choiceRadioButt_EntropyType.equals("Renyi"))   entropyValue = (compRenyi  (minQ, maxQ, numQ))[0]; 
				else if (choiceRadioButt_EntropyType.equals("Tsallis")) entropyValue = (compTsallis(minQ, maxQ, numQ))[0]; 
				else if (choiceRadioButt_EntropyType.equals("SNorm"))   entropyValue = (compSNorm  (minQ, maxQ, numQ))[0]; 
				else if (choiceRadioButt_EntropyType.equals("SEscort")) entropyValue = (compSEscort(minQ, maxQ, numQ))[0]; 
				else if (choiceRadioButt_EntropyType.equals("SEta"))    entropyValue = (compSEta   (minEta,   maxEta,   stepEta,   numEta))[0]; 
				else if (choiceRadioButt_EntropyType.equals("SKappa"))  entropyValue = (compSKappa (minKappa, maxKappa, stepKappa, numKappa))[0]; 
				else if (choiceRadioButt_EntropyType.equals("SB"))      entropyValue = (compSB     (minB,     maxB,     stepB,     numB))[0]; 
				else if (choiceRadioButt_EntropyType.equals("SBeta"))   entropyValue = (compSBeta  (minBeta,  maxBeta,  stepBeta,  numBeta))[0]; 
				else if (choiceRadioButt_EntropyType.equals("SGamma"))  entropyValue = (compSGamma (minGamma, maxGamma, stepGamma, numGamma))[0]; 	
				resultValues[i] = entropyValue;		
				//***********************************************************************
			}
		}	
		return resultValues;
		// SampEn or AppEn
		// Output
		// uiService.show(tableName, table);
	}
	
	//------------------------------------------------------------------------------------------------------
	/**
	 * This computes probabilities of actual values
	 * 
	 * @param signal
	 * @param lag
	 * @param probOption
	 * @return probabilities[]
	 */
	//"Actual", "Pairwise differences", "Sum of differences", "SD"
	private double[] compProbabilities(double[] signal, int lag, String probType) {
		if (probType.equals("Actual")) lag = 0; //to be sure that eps = 0 for that case
		double signalMin = Double.MAX_VALUE;
		double signalMax = -Double.MAX_VALUE;
		double signalDouble[] = new double[signal.length - lag]; 
		
		if (probType.equals("Actual")) {//Actual values
			for (int i = 0; i < signal.length - lag; i++) {
				signalDouble[i] = signal[i];
				if (signalDouble[i] < signalMin) signalMin = signalDouble[i];  
				if (signalDouble[i] > signalMax) signalMax = signalDouble[i];  
			}
		}
		if (probType.equals("Pairwise differences")) {//Pairwise differences
			for (int i = 0; i < signal.length - lag; i++) {
				signalDouble[i] = Math.abs(signal[i+lag] - signal[i]); //Difference
				if (signalDouble[i] < signalMin) signalMin = signalDouble[i];  
				if (signalDouble[i] > signalMax) signalMax = signalDouble[i];  
			}
		}
		if (probType.equals("Sum of differences")) {//Sum of differences in between lag
			for (int i = 0; i < signal.length - lag; i++) {
				double sum = 0.0;
				for (int ii = 0; ii < lag; ii++) {
					sum = sum + Math.abs(signal[i+ii+1] - signal[i+ii]); //Difference
				}
				signalDouble[i] = sum;
				if (signalDouble[i] < signalMin) signalMin = signalDouble[i];  
				if (signalDouble[i] > signalMax) signalMax = signalDouble[i];  
			}
		}
		if (probType.equals("SD")) {//SD in between lag
			for (int i = 0; i < signal.length - lag; i++) {
				double mean = 0.0;
				for (int ii = 0; ii < lag; ii++) {
					mean = mean + signal[i+ii]; //Difference
				}
				mean = mean/((double)lag);
				double sumDiff2 = 0.0;
				for (int ii = 0; ii <= lag; ii++) {
					sumDiff2 = sumDiff2 + Math.pow(signal[i+ii] - mean, 2); //Difference
				}	
				signalDouble[i] = Math.sqrt(sumDiff2/((double)lag));
				if (signalDouble[i] < signalMin) signalMin = signalDouble[i];  
				if (signalDouble[i] > signalMax) signalMax = signalDouble[i];  
			}
		}
	
		//Apache
		int binNumber = 1000;
		int binSize = (int) ((signalMax - signalMin)/binNumber);
		long[] histogram = new long[binNumber];
		EmpiricalDistribution distribution = new EmpiricalDistribution(binNumber);
		distribution.load(signalDouble);
		int k = 0;
		for(SummaryStatistics stats: distribution.getBinStats())
		{
		    histogram[k++] = stats.getN();
		}   

//	    double xValues[] = new double[binNumber];
//        for (int i = 0; i < binNumber; i++) {
//            if (i == 0){
//                xValues[i] = signalMin;
//            } else {
//                xValues[i] = xValues[i-1] + binSize;
//            }
//        }
        double[] pis = new double[binNumber]; 

		double totalsMax = 0.0;
		for (int p= 0; p < binNumber; p++) {
			pis[p] = histogram[p];
			totalsMax = totalsMax + histogram[p]; // calculate total count for normalization
		}	
		
		// normalization
		double sumP = 0.0;
		for (int p = 0; p < pis.length; p++) {	
			pis[p] = pis[p] / totalsMax;
			sumP = sumP + pis[p];
		}
		logService.info(this.getClass().getName() + " Sum of probabilities: " + sumP);
		return pis;
        
	}
	
	/**
	 * This method computes the SE entropy
	 * @return
	 */
	private double compSE() {
		double sum = 0.0f;
		for (int pp = 0; pp < probabilities.length; pp++) {
			//if (probabilities[pp] != 0) {
				//sum = sum +  probabilities[pp] * (1.0 - Math.exp((probabilities[pp] - 1.0) / probabilities[pp]) ); //almost always exact  1!?	//According to and Tsekouras & Tsallis, and Tsallis book
				sum = sum + probabilities[pp] * (Math.exp(1.0 - probabilities[pp]) - 1.0); //around 1.7 // N. R. Pal and S. K. Pal: Object background segmentation using new definitions of entropy, IEEE Proc. 366 (1989), 284–295.
															// N. R. Pal and S. K. Pal, Entropy: a new definitions and its applications, IEEE Transactions on systems, Man and Cybernetics, 21(5), 1260-1270, 1999
				//sum = sum +  probabilities[pp] * Math.exp(1.0 - probabilities[pp]); //always around 2.7 // Hassan Badry Mohamed El-Owny, Exponential Entropy Approach for Image Edge Detection, International Journal of Theoretical and Applied Mathematics 2016; 2(2): 150-155 http://www.sciencepublishinggroup.com/j/ijtam doi: 10.11648/j.ijtam.20160202.29 Hassan Badry Mohamed El-Owny1, 
			//}
		}
		return sum;
	}
	
	/**
	 * This method computes H1, H2 and H3 entropies
	 * According to Amigo etal. paper
	 * @return
	 */
	private double[] compH() {
		
		double genEntH1 = 0;
		double genEntH2 = 0;
		double genEntH3 = 0;	
		
		for (int pp = 0; pp < probabilities.length; pp++) {
			if (probabilities[pp] != 0) {
					double pHochp = Math.pow(probabilities[pp], probabilities[pp]);
					genEntH1 = genEntH1 + (1.0 - pHochp);
					genEntH2 = genEntH2 + Math.log(2.0-pHochp);
					genEntH3 = genEntH3 + (probabilities[pp] + Math.log(2.0-pHochp));	
			}
		}
		genEntH2 = Math.exp(genEntH2);
		
		return new double []{genEntH1, genEntH2, genEntH3};
	}
	
	/**
	 * This method computes generalisied Renyi entropies
	 * According to Amigo etal. paper
	 * @param minQ
	 * @param maxQ
	 * @param numQ
	 * @return
	 */
	private double[] compRenyi(int minQ, int maxQ, int numQ) {
		double[] genEntRenyi   = new double[numQ];
		for (int q = 0; q < numQ; q++) {
			double sum = 0.0f;
			for (int pp = 0; pp < probabilities.length; pp++) {
				if ((q + minQ) == 1) { //q=1 special case
					if (probabilities[pp] == 0) {// damit logarithmus nicht undefiniert ist;
						sum = sum +  Double.MIN_VALUE*Math.log( Double.MIN_VALUE); //for q=1 Renyi is equal to S_BGS (Bolzmann Gibbs Shannon entropy)
					}
					else {
						sum = sum + probabilities[pp]*Math.log(probabilities[pp]); //for q=1 Renyi is equal to S_BGS (Bolzmann Gibbs Shannon entropy)
					}
				}
				else if (((q + minQ) <=  0 ) && (probabilities[pp] != 0.0)){ //leaving out 0 is essential! and according to Amigo etal. page 2
					sum = sum + Math.pow(probabilities[pp],(q + minQ));	
				}
				else if ( (q + minQ) > 0 ) {
					sum = sum + Math.pow(probabilities[pp],(q + minQ));
				}
			}			
			if ((q + minQ) == 1) { //special case q=1 Renyi is equal to S_BGS (Bolzmann Gibbs Shannon entropy)
				genEntRenyi[q] = -sum; 
			}	
			else {
				if (sum == 0) sum = Float.MIN_VALUE; // damit logarithmus nicht undefiniert ist
				genEntRenyi[q] = Math.log(sum)/(1.0-(q + minQ));	
			}
		}//q
		return genEntRenyi;
	}
	
	/**
	 * This method computes generalized Tsallis entropies
	 * According to Amigo etal. paper
	 * @param minQ
	 * @param maxQ
	 * @param numQ
	 * @return
	 */
	private double[] compTsallis(int minQ, int maxQ, int numQ) {
		double[] genEntTsallis   = new double[numQ];
		for (int q = 0; q < numQ; q++) {
			double sum = 0.0f;
			for (int pp = 0; pp < probabilities.length; pp++) {
				if ((q + minQ) == 1) { //q=1 special case
					if (probabilities[pp] == 0) {// damit logarithmus nicht undefiniert ist;
						sum = sum +  Double.MIN_VALUE*Math.log( Double.MIN_VALUE); //for q=1 Renyi is equal to S_BGS (Bolzmann Gibbs Shannon entropy)
					}
					else {
						sum = sum + probabilities[pp]*Math.log(probabilities[pp]); //for q=1 Renyi is equal to S_BGS (Bolzmann Gibbs Shannon entropy)
					}
				}
				else if (((q + minQ) <=  0 ) && (probabilities[pp] != 0.0)) { //leaving out 0 is essential! and according to Amigo etal. page 2
						sum = sum + Math.pow(probabilities[pp],(q + minQ));	
				}
				else if ( (q + minQ) > 0 ) {
						sum = sum + Math.pow(probabilities[pp],(q + minQ));	
				}				
			}				
			if ((q + minQ) == 1) { // special case for q=1 Tsallis is equal to S_BGS (Bolzmann Gibbs Shannon entropy)
				genEntTsallis[q] = -sum;
			}
			else {
				genEntTsallis[q] = (sum-1)/(1.0-(q + minQ));
			}		
		}//q						
		return genEntTsallis;
	}
	
	/**
	 * This method computes the generalized entropy SNorm
	 * According to Amigo etal. paper
	 * @param minQ
	 * @param maxQ
	 * @param numQ
	 * @return
	 */
	private double[] compSNorm(int minQ, int maxQ, int numQ) {
		double[] genEntSNorm   = new double[numQ];
		for (int q = 0; q < numQ; q++) {
			double sum = 0.0f;
			for (int pp = 0; pp < probabilities.length; pp++) {
				if ((q + minQ) == 1) { //q=1 special case
					if (probabilities[pp] == 0) {// damit logarithmus nicht undefiniert ist;
						sum = sum +  Double.MIN_VALUE*Math.log( Double.MIN_VALUE); //for q=1 Snorm is equal to S_BGS (Bolzmann Gibbs Shannon entropy)
					}
					else {
						sum = sum + probabilities[pp]*Math.log(probabilities[pp]); //for q=1 Snorm is equal to S_BGS (Bolzmann Gibbs Shannon entropy)
					}
				}
				else if (((q + minQ) <=  0 ) && (probabilities[pp] != 0.0)){ //leaving out 0 is essential! and according to Amigo etal. page 2
					sum = sum + Math.pow(probabilities[pp],(q + minQ));	
				}
				else if ( (q + minQ) > 0 ) {
					sum = sum + Math.pow(probabilities[pp],(q + minQ));
				}
//				else {
//					sum = sum + Math.pow(probabilities[pp],(q + minQ));
//				}
			}			
			if ((q + minQ) == 1) { //special case q=1 SNorm is equal to S_BGS (Bolzmann Gibbs Shannon entropy)
				genEntSNorm[q] = -sum; 
			}	
			else {
				genEntSNorm[q] = (1.0-(1.0/sum))/(1.0-(q + minQ));	
			}
		}//q		
		return genEntSNorm;
	}
	
	/**
	 * This method computes the generalized Escort entropies
	 * According to Tsallis Introduction to Nonextensive Statistical Mechanics, 2009, S105-106
	 * @param minQ
	 * @param maxQ
	 * @param numQ
	 * @return
	 */
	private double[] compSEscort(int minQ, int maxQ, int numQ) {
		double[] genEntSEscort = new double[numQ];
		for (int q = 0; q < numQ; q++) {
			double sum = 0.0f;
			for (int pp = 0; pp < probabilities.length; pp++) {
				if ((q + minQ) == 1) { //q=1 special case
					if (probabilities[pp] == 0) {// damit logarithmus nicht undefiniert ist;
						sum = sum +  Double.MIN_VALUE*Math.log( Double.MIN_VALUE); //for q=1 SEscort is equal to S_BGS (Bolzmann Gibbs Shannon entropy)
					}
					else {
						sum = sum + probabilities[pp]*Math.log(probabilities[pp]); //for q=1 SEscort is equal to S_BGS (Bolzmann Gibbs Shannon entropy)
					}
				}
				else if (((q + minQ) <=  0 ) && (probabilities[pp] != 0.0)){ //leaving out 0 is essential! and according to Amigo etal. page 2
					sum = sum + Math.pow(probabilities[pp], 1.0/(q + minQ));	
				}
				else if ( (q + minQ) > 0 ) {
					sum = sum + Math.pow(probabilities[pp], 1.0/(q + minQ));
				}
//				else {
//					sum = sum + Math.pow(probabilities[pp], 1.0/(q + minQ));
//				}
			}			
			if ((q + minQ) == 1) { //special case q=1 SEscort is equal to S_BGS (Bolzmann Gibbs Shannon entropy)
				genEntSEscort[q] = -sum; 
			}	
			else {
				genEntSEscort[q] = (1.0 - Math.pow(sum, -(q+minQ)))/((q + minQ) - 1.0);	
			}
		}//q		
		return genEntSEscort;
	}
	
	/**
	 * This method computes the gneralized SEta entropies
	 * According to Amigo etal. and Anteneodo, C.; Plastino, A.R. Maximum entropy approach to stretched exponential pixelPercentage distributions. J. Phys. A Math. Gen. 1999, 32, 1089–1098.
	 * @param minEta
	 * @param maxEta
	 * @param stepEta
	 * @param numEta
	 * @return
	 */
	private double[] compSEta(float minEta, float maxEta, float stepEta, int numEta) {
		double[] genEntSEta = new double[numEta];
		for (int n = 0; n < numEta; n++) {
			double eta = minEta + n*stepEta; //SEta is equal to S_BGS (Bolzmann Gibbs Shannon entropy) for eta = 1 
			double sum = 0.0f;
			for (int pp = 0; pp < probabilities.length; pp++) {
				if (probabilities[pp] != 0){

					//compute incomplete Gamma function using Apache's classes
					double gam1 = Gamma.regularizedGammaQ((eta+1.0)/eta, -Math.log(probabilities[pp])) * Math.exp(Gamma.logGamma((eta+1.0)/eta));
					double gam2 = probabilities[pp]*Math.exp(Gamma.logGamma((eta+1.0f)/eta)); 
					sum = sum + gam1 - gam2;	
				}
			}	
			genEntSEta[n] = sum;
		}//q		
		return genEntSEta;
	}
	
	/**
	 * This method computes generalized SKappa entropies
	 * According to Amigo etal. and Kaniadakis, G. Statistical mechanics in the context of special relativity. Phys. Rev. E 2002, 66, 056125
	 * @param minKappa
	 * @param maxKappa
	 * @param stepKappa
	 * @param numKappa
	 * @return
	 */
	private double[] compSKappa(float minKappa, float maxKappa, float stepKappa, int numKappa) {
		double[] genEntSKappa = new double[numKappa];
		for (int k = 0; k < numKappa; k++) {
			double kappa = minKappa + k*stepKappa; //SKappa is equal to S_BGS (Bolzmann Gibbs Shannon entropy) for kappa = 0 
			double sum = 0.0f;
			for (int pp = 0; pp < probabilities.length; pp++) {
				if (kappa == 0) { //kappa=0 special case S_BGS (Bolzmann Gibbs Shannon entropy)
					if (probabilities[pp] == 0) {// damit logarithmus nicht undefiniert ist;
						sum = sum +  Double.MIN_VALUE*Math.log( Double.MIN_VALUE); //for k = 0 SKappa is equal to S_BGS (Bolzmann Gibbs Shannon entropy)
					}
					else {
						sum = sum + probabilities[pp]*Math.log(probabilities[pp]); //for k=0 SKappa is equal to S_BGS (Bolzmann Gibbs Shannon entropy)
					}
				}
				else {
					//if (probabilities[pp] != 0){			
						sum = sum + (Math.pow(probabilities[pp], 1.0-kappa) - Math.pow(probabilities[pp], 1.0+kappa))/(2.0*kappa);			
					//}
				}
			}
			if (kappa == 0){
				genEntSKappa[k] = -sum;
			}
			else {
				genEntSKappa[k] = sum;
			}
		}//q		
		return genEntSKappa;
	}
	
	/**
	 * This method computes the generalized SB entropies
	 * aAccording to Amigo etal. and Curado, E.M.; Nobre, F.D. On the stability of analytic entropic forms. Physica A 2004, 335, 94–106.
	 * @param minB
	 * @param maxB
	 * @param stepB
	 * @param numB
	 * @return
	 */
	private double[] compSB(float minB, float maxB, float stepB, int numB) {
		double[] genEntSB = new double[numB];
		for (int n = 0; n < numB; n++) {
			double valueB = minB + n*stepB; //SB is equal to S_BGS (Bolzmann Gibbs Shannon entropy) for ????????????????? 
			double sum = 0.0f;
			for (int pp = 0; pp < probabilities.length; pp++) {
				//if (probabilities[pp] != 0){
					sum = sum + (1.0 - Math.exp(-valueB*probabilities[pp]));
				//}
			}	
			genEntSB[n] = sum + (Math.exp(-valueB)-1.0); 
		}//q		
		return genEntSB;
	}
	
	/**
	 * This method computes generalized SBeta entropies
	 * According to Amigo etal. and Shafee, F. Lambert function and a new non-extensive form of entropy. IMA J. Appl. Math. 2007, 72, 785–800.
	 * @param minBeta
	 * @param maxBeta
	 * @param stepBeta
	 * @param numBeta
	 * @return
	 */
	private double[] compSBeta(float minBeta, float maxBeta, float stepBeta, int numBeta) {
		double[] genEntSBeta = new double[numBeta];
		for (int n = 0; n < numBeta; n++) {
			double valueBeta = minBeta + n*stepBeta; //SBeta is equal to S_BGS (Bolzmann Gibbs Shannon entropy) for valueBeta = 1; 
			double sum = 0.0f;
			for (int pp = 0; pp < probabilities.length; pp++) {		
				if (probabilities[pp] != 0.0){ //leaving out 0 
					sum = sum + Math.pow(probabilities[pp],  valueBeta) * Math.log(1.0/probabilities[pp]);
				}
			}
			genEntSBeta[n] = sum;					
		}//q		
		return genEntSBeta;
	}
	
	/**
	 * This method computes generalized SGamma entropies
	 * According to Amigo etal. and Tsallis Introduction to Nonextensive Statistical Mechanics, 2009, S61
	 * @param minGamma
	 * @param maxGamma
	 * @param stepGamma
	 * @param numGamma
	 * @return
	 */
	private double[] compSGamma(float minGamma, float maxGamma, float stepGamma, int numGamma) {
		double[] genEntSGamma = new double[numGamma];
		for (int g = 0; g < numGamma; g++) {
			double valueGamma = minGamma + g*stepGamma; //SGama is equal to S_BGS (Bolzmann Gibbs Shannon entropy) for valueGamma = 1; 
			double sum = 0.0f;
			for (int pp = 0; pp < probabilities.length; pp++) {		
				if (probabilities[pp] != 0.0){ //leaving out 0 
					sum = sum + Math.pow(probabilities[pp],  1.0/valueGamma) * Math.log(1.0/probabilities[pp]);
				}
			}
			genEntSGamma[g] = sum;					
		}//q
		return genEntSGamma;
		
	}
	
	/**
	 * This method calculates the mean of a data series
	 * 
	 * @param data1D
	 * @return Double Mean
	 */
	private double calcMean(double[] data1D) {
		double sum = 0;
		for (double d : data1D) {
			sum += d;
		}
		return sum / data1D.length;
	}
	
	/**
	 * This method calculates the mean of a data series
	 * 
	 * @param data1D[numberOfSurrogates][]
	 * @return double Mean
	 */
	private double[] calcSurrMean(double[][] data1D) {
		double surrMean[] = new double [data1D.length];
		for (int p = 0; p <  data1D.length; p++) {	
			for (int n = 0; n < data1D[0].length; n++ ) {
				surrMean[p] = surrMean[p] + data1D[n][p];
			}	
			surrMean[p] = surrMean[p] / data1D[0].length;
		}
		return surrMean;
	}
	
	private int getMaxLag(int length) { // 
		int result = 0;
		if (this.choiceRadioButt_AnalysisType.equals("Entire signal")) {
			result = (length) / 2;
		}
		else {
			float boxWidth = 1f;
			int number = 1; // inclusive original image
			while (boxWidth <= length) {
				boxWidth = boxWidth * 2;
				number = number + 1;
			}
			result = number - 1;
		}
		return result;
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
	
	// ---------------------------------------------------------------------------------------------
	
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
		ij.command().run(SignalGeneralisedEntropies.class, true);
	}
}
