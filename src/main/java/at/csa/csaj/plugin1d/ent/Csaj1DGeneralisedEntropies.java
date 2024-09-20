/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj1DGeneralisedEntropies.java
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

package at.csa.csaj.plugin1d.ent;

import java.awt.Toolkit;
import java.lang.invoke.MethodHandles;
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

import at.csa.csaj.commons.CsajAlgorithm_GeneralisedEntropies;
import at.csa.csaj.commons.CsajAlgorithm_Surrogate1D;
import at.csa.csaj.commons.CsajDialog_WaitingWithProgressBar;
import at.csa.csaj.commons.CsajContainer_ProcessMethod;
import at.csa.csaj.plugin1d.misc.Csaj1DOpenerCommand;


/**
 * A {@link InteractiveCommand} plugin computing <Generalised entropies</a>
 * of a sequence.
 * <li>according to a review of Amigó, J.M., Balogh, S.G., Hernández, S., 2018. A Brief Review of Generalised Entropies. Entropy 20, 813. https://doi.org/10.3390/e20110813
 * <li>and to: Tsallis Introduction to Nonextensive Statistical Mechanics, 2009, S105-106
 * <li>(SE     according to Amigo etal. and Tsekouras, G.A.; Tsallis, C. Generalised entropy arising from a distribution of q indices. Phys. Rev. E 2005,)
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
@Plugin(type = InteractiveCommand.class, 
	headless = true,
	label = "Generalised entropies",
	initializer = "initialPluginLaunch",
	iconPath = "/icons/comsystan-logo-grey46-16x16.png", //Menu entry icon
	menu = {
	@Menu(label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT, mnemonic = MenuConstants.PLUGINS_MNEMONIC),
	@Menu(label = "ComsystanJ"),
	@Menu(label = "1D Sequence(s)"),
	@Menu(label = "Entropy analyses", weight = 5),
	@Menu(label = "Generalised entropies ")}) //Space at the end of the label is necessary to avoid duplicate with 2D plugin 
/**
 * Csaj Interactive: InteractiveCommand (nonmodal GUI without OK and cancel button, NOT for Scripting!)
 * Csaj Macros:      ContextCommand     (modal GUI with OK and Cancel buttons, for scripting)
 * Developer note:
 * Develop the InteractiveCommand plugin Csaj***.java
 * The Maven build will execute CreateCommandFiles.java which creates Csaj***Command.java files
 *
 *
 */
public class Csaj1DGeneralisedEntropies<T extends RealType<T>> extends InteractiveCommand implements Previewable {

	private static final String PLUGIN_LABEL            = "<html><b>Generalised entropies</b></html>";
	private static final String SPACE_LABEL             = "";
	private static final String ENTROPYOPTIONS_LABEL    = "<html><b>Entropy options</b></html>";
	private static final String ANALYSISOPTIONS_LABEL   = "<html><b>Analysis options</b></html>";
	private static final String BACKGROUNDOPTIONS_LABEL = "<html><b>Background option</b></html>";
	private static final String DISPLAYOPTIONS_LABEL    = "<html><b>Display option</b></html>";
	private static final String PROCESSOPTIONS_LABEL    = "<html><b>Process options</b></html>";
	
	private static double[] sequence1D;
	private static double[] domain1D;
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
	
	public static final String TABLE_OUT_NAME = "Table - Generalised entropies";
	
	CsajDialog_WaitingWithProgressBar dlgProgress;
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
	

	@Parameter(label = TABLE_OUT_NAME, type = ItemIO.OUTPUT)
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
	private final String labelEntropyOptions = ENTROPYOPTIONS_LABEL;
	
	@Parameter(label = "Probability type",
			   description = "Selection of probability type",
			   style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
			   choices = {"Sequence values", "Pairwise differences", "Sum of differences", "SD"}, 
			   persist = false,  //restore previous value default = true
			   initializer = "initialProbabilityType",
			   callback = "callbackProbabilityType")
	private String choiceRadioButt_ProbabilityType;

	@Parameter(label = "Lag",
			   description = "(difference)delta between two data points",
			   style = NumberWidget.SPINNER_STYLE,
			   min = "1",
			   max = "1000000",
			   stepSize = "1",
			   persist = false, // restore  previous value  default  =  true
			   initializer = "initialLag",
			   callback = "callbackLag")
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
	
	@Parameter(label = "(SGamma) Min gamma", description = "minimal Gamma for SGamma entropies", style = NumberWidget.SPINNER_STYLE, min = "0f", max = "100000f", stepSize = "0.1",
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
	
	@Parameter(label = "(Surr/Box) Entropy type",
			   description = "Entropy for Surrogates, Subsequent boxes or Gliding box",
			   style = ChoiceWidget.LIST_BOX_STYLE,
			   choices = {"SE", "H1", "H2", "H3", "Renyi", "Tsallis", "SNorm", "SEscort", "SEta", "SKappa", "SB", "SBeta", "SGamma"}, 
			   persist = true,  //restore previous value default = true
			   initializer = "initialEntropyType",
			   callback = "callbackEntropyType")
	private String choiceRadioButt_EntropyType;
	
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

	@Parameter(label = "Process all columns", callback = "callbackProcessAllColumns")
	private Button buttonProcessAllColumns;


	// ---------------------------------------------------------------------
		
	protected void initialPluginLaunch() {
		checkItemIOIn();
	}
	
	protected void initialProbabilityType() {
		choiceRadioButt_ProbabilityType = "Sequence values"; //"Sequence values", "Pairwise differences", "Sum of differences", "SD"
	} 
	
	protected void initialLag() {
		spinnerInteger_Lag = 1;
	}
	
	protected void initialMinQ() {
		spinnerInteger_MinQ = -5;
		minQ = spinnerInteger_MinQ;
		numQ = (maxQ - minQ)/stepQ + 1;
	}
	
	protected void initialMaxQ() {
		spinnerInteger_MaxQ = 5;
		maxQ = spinnerInteger_MaxQ;
		numQ = (maxQ - minQ)/stepQ + 1;
	}
	
	protected void initialMinEta() {
		spinnerFloat_MinEta = 0.1f;
		minEta = Precision.round(spinnerFloat_MinEta, 1); //round to 1 decimal, because sometimes float is not exact
		numEta = (int)((maxEta - minEta)/stepEta + 1);
	}
	
	protected void initialMaxEta() {
		spinnerFloat_MaxEta = 1f;
		maxEta = Precision.round(spinnerFloat_MaxEta, 1);
		numEta = (int)((maxEta - minEta)/stepEta + 1);
	}
	protected void initialMinKappa() {
		spinnerFloat_MinKappa = 0.1f;
		minKappa = Precision.round(spinnerFloat_MinKappa, 1);
		numKappa = (int)((maxKappa - minKappa)/stepKappa + 1);
	}
	
	protected void initialMaxKappa() {
		spinnerFloat_MaxKappa = 0.9f;
		maxKappa = Precision.round(spinnerFloat_MaxKappa, 1);
		numKappa = (int)((maxKappa - minKappa)/stepKappa + 1);
	}
	
	protected void initialMinB() {
		spinnerFloat_MinB = 1.0f;
		minB = Precision.round(spinnerFloat_MinB, 1);
		numB = (int)((maxB - minB)/stepB + 1);
	}
	
	protected void initialMaxB() {
		spinnerFloat_MaxB = 10.0f;
		maxB = Precision.round(spinnerFloat_MaxB, 1);
		numB = (int)((maxB - minB)/stepB + 1);
	}
	
	protected void initialMinBeta() {
		spinnerFloat_MinBeta = 0.5f;
		minBeta = Precision.round(spinnerFloat_MinBeta, 1);
		numBeta = (int)((maxBeta - minBeta) /stepBeta + 1);
	}
	
	protected void initialMaxBeta() {
		spinnerFloat_MaxBeta = 1.5f;
		maxBeta = Precision.round(spinnerFloat_MaxBeta, 1);
		numBeta = (int)((maxBeta - minBeta) /stepBeta + 1);
	}
	
	protected void initialMinGamma() {
		spinnerFloat_MinGamma = 0.1f;
		minGamma = Precision.round(spinnerFloat_MinGamma, 1);
		numGamma = (int)((maxGamma - minGamma)/stepGamma + 1);
	}
	
	protected void initialMaxGamma() {
		spinnerFloat_MaxGamma = 1.0f;
		maxGamma = Precision.round(spinnerFloat_MaxGamma, 1);
		numGamma = (int)((maxGamma - minGamma)/stepGamma + 1);
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
	
	protected void initialEntropyType() {
		choiceRadioButt_EntropyType = "Renyi"; //"SE", "H1", "H2", "H3", "Renyi", "Tsallis", "SNorm", "SEscort", "SEta", "SKappa", "SB", "SBeta", "SGamma"
	} 
	
	protected void initialSkipZeroes() {
		booleanSkipZeroes = false;
	}	
	
	protected void initialOverwriteDisplays() {
    	booleanOverwriteDisplays = true;
}
	
	protected void initialNumColumn() {
		spinnerInteger_NumColumn = 1;
	}

	// ------------------------------------------------------------------------------
	/** Executed whenever the {@link #choiceRadioButt_ProbabilityType} parameter changes. */
	protected void callbackProbabilityType() {
		logService.info(this.getClass().getName() + " Propability type set to " + choiceRadioButt_ProbabilityType);
		if (choiceRadioButt_ProbabilityType.contains("Sequence values")) {
			logService.info(this.getClass().getName() + " Sequence values only with lag = 1");
			spinnerInteger_Lag = 1;
		}
	}
	
	/** Executed whenever the {@link #spinnerInteger_Lag} parameter changes. */
	protected void callbackLag() {
		if (choiceRadioButt_ProbabilityType.contains("Sequence values")) {
			logService.info(this.getClass().getName() + " Sequence values only with lag = 1");
			spinnerInteger_Lag = 1;
		}
		logService.info(this.getClass().getName() + " Lag set to " + spinnerInteger_Lag);
	}
	
	/** Executed whenever the {@link #spinnerInteger_MinQ} parameter changes. */
	protected void callbackMinQ() {
		minQ = spinnerInteger_MinQ;
		numQ = (maxQ - minQ)/stepQ + 1;
		logService.info(this.getClass().getName() + " Minimal Q set to " + spinnerInteger_MinQ);
	}

	/** Executed whenever the {@link #spinnerInteger_MaxQ} parameter changes. */
	protected void callbackMaxQ() {
		maxQ = spinnerInteger_MaxQ;
		numQ = (maxQ - minQ)/stepQ + 1;
		logService.info(this.getClass().getName() + " Maximal Q set to " + spinnerInteger_MaxQ);
	}
	
	/** Executed whenever the {@link #spinFloat_MinEta} parameter changes. */
	protected void callbackMinEta() {
		minEta = Precision.round(spinnerFloat_MinEta, 1); //round to 1 decimal, because sometimes float is not exact
		numEta = (int)((maxEta - minEta)/stepEta + 1);
		logService.info(this.getClass().getName() + " Minimal Eta set to " + spinnerFloat_MinEta);
	}
	
	/** Executed whenever the {@link #spinFloat_MaxEta} parameter changes. */
	protected void callbackMaxEta() {
		maxEta = Precision.round(spinnerFloat_MaxEta, 1);
		numEta = (int)((maxEta - minEta)/stepEta + 1);
		logService.info(this.getClass().getName() + " Maximal Eta set to " + spinnerFloat_MaxEta);
	}
	
	/** Executed whenever the {@link #spinFloat_MinKappa} parameter changes. */
	protected void callbackMinKappa() {
		minKappa = Precision.round(spinnerFloat_MinKappa, 1);
		numKappa = (int)((maxKappa - minKappa)/stepKappa + 1);
		logService.info(this.getClass().getName() + " Minimal Kappa set to " + spinnerFloat_MinKappa);
	}
	
	/** Executed whenever the {@link #spinFloat_MaxKappa} parameter changes. */
	protected void callbackMaxKapa() {
		maxKappa = Precision.round(spinnerFloat_MaxKappa, 1);
		numKappa = (int)((maxKappa - minKappa)/stepKappa + 1);
		logService.info(this.getClass().getName() + " Maximal Kappa set to " + spinnerFloat_MaxKappa);
	}
	
	/** Executed whenever the {@link #spinFloat_MinB} parameter changes. */
	protected void callbackMinB() {
		minB = Precision.round(spinnerFloat_MinB, 1);
		numB = (int)((maxB - minB)/stepB + 1);
		logService.info(this.getClass().getName() + " Minimal B set to " + spinnerFloat_MinB);
	}
	
	/** Executed whenever the {@link #spinFloat_MaxB} parameter changes. */
	protected void callbackMaxB() {
		maxB = Precision.round(spinnerFloat_MaxB, 1);
		numB = (int)((maxB - minB)/stepB + 1);
		logService.info(this.getClass().getName() + " Maximal B set to " + spinnerFloat_MaxB);
	}
	
	/** Executed whenever the {@link #spinFloat_MinBeta} parameter changes. */
	protected void callbackMinBeta() {
		minBeta = Precision.round(spinnerFloat_MinBeta, 1);
		numBeta = (int)((maxBeta - minBeta) /stepBeta + 1);
		logService.info(this.getClass().getName() + " Minimal Beta set to " + spinnerFloat_MinBeta);
	}
	
	/** Executed whenever the {@link #spinFloat_MaxBeta} parameter changes. */
	protected void callbackMaxBeta() {
		maxBeta = Precision.round(spinnerFloat_MaxBeta, 1);
		numBeta = (int)((maxBeta - minBeta) /stepBeta + 1);
		logService.info(this.getClass().getName() + " Maximal Beta set to " + spinnerFloat_MaxBeta);
	}
	
	/** Executed whenever the {@link #spinFloat_MinGamma} parameter changes. */
	protected void callbackMinGamma() {
		minGamma = Precision.round(spinnerFloat_MinGamma, 1);
		numGamma = (int)((maxGamma - minGamma)/stepGamma + 1);
		logService.info(this.getClass().getName() + " Minimal Gamma set to " + spinnerFloat_MinGamma);
	}
	
	/** Executed whenever the {@link #spinFloat_MaxGamma} parameter changes. */
	protected void callbackMaxGamma() {
		maxGamma = Precision.round(spinnerFloat_MaxGamma, 1);
		numGamma = (int)((maxGamma - minGamma)/stepGamma + 1);
		logService.info(this.getClass().getName() + " Maximal Gamma set to " + spinnerFloat_MaxGamma);
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

	/** Executed whenever the {@link #choiceRadioButt_EntropyType} parameter changes. */
	protected void callbackEntropyType() {
		logService.info(this.getClass().getName() + " Entropy type for surrogate or box set to " + choiceRadioButt_EntropyType);
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
	    	   	uiService.show(TABLE_OUT_NAME, tableOut);
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
	    	   	uiService.show(TABLE_OUT_NAME, tableOut);
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
		    	   	uiService.show(TABLE_OUT_NAME, tableOut);   //Show table because it did not go over the run() method
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
		
		stepQ     = 1;
		stepEta   = 0.1f;
		stepKappa = 0.1f;
		stepB     = 1.0f;
		stepBeta  = 0.1f;
		stepGamma = 0.1f;
	      
		logService.info(this.getClass().getName() + " Name: "      + tableInName); 
		logService.info(this.getClass().getName() + " Columns #: " + numColumns);
		logService.info(this.getClass().getName() + " Rows #: "    + numRows); 
	}

	/**
	* This method starts the workflow for a single column of the active display
	*/
	protected void startWorkflowForSingleColumn() {
	
		dlgProgress = new CsajDialog_WaitingWithProgressBar("Computing Generalised entropies, please wait... Open console window for further info.",
							logService, false, exec); //isCanceable = false, because no following method listens to exec.shutdown 
		dlgProgress.updatePercent("");
		dlgProgress.setBarIndeterminate(true);
		dlgProgress.setVisible(true);
	
    	logService.info(this.getClass().getName() + " Processing single sequence");
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
	
		dlgProgress = new CsajDialog_WaitingWithProgressBar("Computing Generalised entropies, please wait... Open console window for further info.",
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
		tableOut.add(new IntColumn("Surrogates #"));
		tableOut.add(new IntColumn("Box length"));
		tableOut.add(new BoolColumn("Skip zeroes"));
	
		tableOut.add(new GenericColumn("Probability type"));		
		tableOut.add(new IntColumn("Lag"));
			
		//"Entire sequence", "Subsequent boxes", "Gliding box" 
		if (choiceRadioButt_SequenceRange.equals("Entire sequence")){
			
			if (choiceRadioButt_SurrogateType.equals("No surrogates")) {
				//"SE", "H1", "H2", "H3", "Renyi", "Tsallis", "SNorm", "SEscort", "SEta", "SKappa", "SB", "SBeta", "SGamma"
				tableOut.add(new DoubleColumn("SE"));
				tableOut.add(new DoubleColumn("H1"));
				tableOut.add(new DoubleColumn("H2"));
				tableOut.add(new DoubleColumn("H3"));
				for (int q = 0; q < numQ; q++) tableOut.add(new DoubleColumn("Renyi_q"   + (minQ + q))); 
				for (int q = 0; q < numQ; q++) tableOut.add(new DoubleColumn("Tsallis_q" + (minQ + q))); 
				for (int q = 0; q < numQ; q++) tableOut.add(new DoubleColumn("SNorm_q"   + (minQ + q))); 
				for (int q = 0; q < numQ; q++) tableOut.add(new DoubleColumn("SEscort_q" + (minQ + q))); 
				for (int e = 0; e < numEta;   e++)  tableOut.add(new DoubleColumn("SEta_e"    + String.format ("%.1f", minEta   + e*stepEta)));
				for (int k = 0; k < numKappa; k++)  tableOut.add(new DoubleColumn("SKappa_k"  + String.format ("%.1f", minKappa + k*stepKappa))); 
				for (int b = 0; b < numB;     b++)  tableOut.add(new DoubleColumn("SB_b"      + String.format ("%.1f", minB     + b*stepB))); 
				for (int be= 0; be< numBeta;  be++) tableOut.add(new DoubleColumn("SBeta_be"  + String.format ("%.1f", minBeta  +be*stepBeta))); 
				for (int g = 0; g < numGamma; g++)  tableOut.add(new DoubleColumn("SGamma_g"  + String.format ("%.1f", minGamma + g*stepGamma))); 
	
			} else { //Surrogates	
				if (choiceRadioButt_EntropyType.equals("SE")) {
					tableOut.add(new DoubleColumn("SE"));
					tableOut.add(new DoubleColumn("SE_Surr"));  //Mean surrogate value	
					for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("SE_Surr#"+(s+1))); 
				}
				else if (choiceRadioButt_EntropyType.equals("H1")) {
					tableOut.add(new DoubleColumn("H1"));
					tableOut.add(new DoubleColumn("H1_Surr"));  //Mean surrogate value	
					for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("H1_Surr#"+(s+1))); 
				}
				else if (choiceRadioButt_EntropyType.equals("H2")) {
					tableOut.add(new DoubleColumn("H2"));
					tableOut.add(new DoubleColumn("H2_Surr"));  //Mean surrogate value	
					for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("H2_Surr#"+(s+1))); 
				}
				else if (choiceRadioButt_EntropyType.equals("H3")) {
					tableOut.add(new DoubleColumn("H3"));
					tableOut.add(new DoubleColumn("H3_Surr"));  //Mean surrogate value	 
					for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("H3_Surr#"+(s+1))); 
				}
				else if (choiceRadioButt_EntropyType.equals("Renyi")) {
					tableOut.add(new DoubleColumn("Renyi_q"   + minQ)); 
					tableOut.add(new DoubleColumn("Renyi_q"+minQ+"_Surr"));  //Mean surrogate value	
					for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("Renyi_q"+minQ+"_Surr#"+(s+1))); 
				}
				else if (choiceRadioButt_EntropyType.equals("Tsallis")) {
					tableOut.add(new DoubleColumn("Tsallis_q" + minQ)); 
					tableOut.add(new DoubleColumn("Tsallis_q"+minQ+"_Surr"));  //Mean surrogate value	
					for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("Tsallis_q"+minQ+"_Surr#"+(s+1))); 
				}
				else if (choiceRadioButt_EntropyType.equals("SNorm")) {
					tableOut.add(new DoubleColumn("SNorm_q"   + minQ));
					tableOut.add(new DoubleColumn("SNorm_q"+minQ+"_Surr"));  //Mean surrogate value	
					for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("SNorm_q"+minQ+"_Surr#"+(s+1))); 
				}
				else if (choiceRadioButt_EntropyType.equals("SEscort")) {
					tableOut.add(new DoubleColumn("SEscort_q" + minQ)); 
					tableOut.add(new DoubleColumn("SEscort_q"+minQ+"_Surr"));  //Mean surrogate value	
					for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("SEscort_q"+minQ+"_Surr#"+(s+1))); 
				}
				else if (choiceRadioButt_EntropyType.equals("SEta")) {
					tableOut.add(new DoubleColumn("SEta_e"    + String.format ("%.1f", minEta)));
					tableOut.add(new DoubleColumn("SEta_e"+minEta+"_Surr"));  //Mean surrogate value	
					for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("SEta_e"+minEta+"_Surr#"+(s+1))); 
				}
				else if (choiceRadioButt_EntropyType.equals("SKappa")) {
					tableOut.add(new DoubleColumn("SKappa_k"  + String.format ("%.1f", minKappa))); 
					tableOut.add(new DoubleColumn("SKappa_k"+minKappa+"_Surr"));  //Mean surrogate value	 
					for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("SKappa_k"+minKappa+"_Surr#"+(s+1))); 
				}
				else if (choiceRadioButt_EntropyType.equals("SB")) {
					tableOut.add(new DoubleColumn("SB_b"      + String.format ("%.1f", minB))); 
					tableOut.add(new DoubleColumn("SB_b"+minB+"_Surr"));  //Mean surrogate value	
					for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("SB_b"+minB+"_Surr#"+(s+1))); 
				}
				else if (choiceRadioButt_EntropyType.equals("SBeta")) {
					tableOut.add(new DoubleColumn("SBeta_be"  + String.format ("%.1f", minBeta))); 
					tableOut.add(new DoubleColumn("SBeta_be"+minBeta+"_Surr"));  //Mean surrogate value	 
					for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("SBeta_be"+minBeta+"_Surr#"+(s+1))); 
				}
				else if (choiceRadioButt_EntropyType.equals("SGamma")) {
					tableOut.add(new DoubleColumn("SGamma_g"  + String.format ("%.1f", minGamma))); 
					tableOut.add(new DoubleColumn("SGamma_g"+minGamma+"_Surr"));  //Mean surrogate value	 
					for (int s = 0; s < numSurrogates; s++) tableOut.add(new DoubleColumn("SGamma_g"+minGamma+"_Surr#"+(s+1))); 
				}
			}
		} 
		else if (choiceRadioButt_SequenceRange.equals("Subsequent boxes")){
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
				tableOut.add(new DoubleColumn(entropyHeader+"-#" + n));	
			}	
		}
		else if (choiceRadioButt_SequenceRange.equals("Gliding box")){
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
				tableOut.add(new DoubleColumn(entropyHeader+"-#" + n));	
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
				if (display.getName().contains(TABLE_OUT_NAME))
					display.close();
			}
		}
	}

	/** This method takes the column at position c and computes results. 
	 * 
	 */
	private void processSingleInputColumn (int c) {
		
		long startTime = System.currentTimeMillis();
		
		// Compute result values
		CsajContainer_ProcessMethod containerPM = process(tableIn, c); 
		// 0 Entropy
		logService.info(this.getClass().getName() + " Gen entropy SE: " + containerPM.item1_Values[0]);
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
		
		CsajContainer_ProcessMethod containerPM;
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
				// 0 Entropy
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
	 * @param CsajContainer_ProcessMethod containerPM
	 */
	private void writeToTable(int numRow, int sequenceNumber, CsajContainer_ProcessMethod containerPM) {
		logService.info(this.getClass().getName() + " Writing to the table...");
		int row = numRow;
		int tableColStart = 0;
		int tableColEnd   = 0;
		int tableColLast  = 0;
		
		// 0 Entropy
		// fill table with values
		tableOut.appendRow();
		tableOut.set(0, row, tableInName);//File Name
		if (sliceLabels != null)  tableOut.set(1, row, tableIn.getColumnHeader(sequenceNumber)); //Column Name
	
		tableOut.set(2, row, choiceRadioButt_SequenceRange); //Sequence Method
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
		tableOut.set(6, row, booleanSkipZeroes); //Zeroes removed
		
		tableOut.set(7, row, choiceRadioButt_ProbabilityType);    // Lag
		tableOut.set(8, row, spinnerInteger_Lag);    // Lag
		tableColLast = 8;
		
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
		uiService.show(TABLE_OUT_NAME, tableOut);
	}
	
	/**
	*
	* Processing
	*/
	private CsajContainer_ProcessMethod process(DefaultGenericTable dgt, int col) { //  c column number
	
		if (dgt == null) {
			logService.info(this.getClass().getName() + " WARNING: dgt==null, no sequence for processing!");
		}
		
		String  sequenceRange = choiceRadioButt_SequenceRange;
		String  surrType      = choiceRadioButt_SurrogateType;
		numSurrogates         = spinnerInteger_NumSurrogates;
		numBoxLength          = spinnerInteger_BoxLength;
		int     numDataPoints = dgt.getRowCount();
		String  probType      = choiceRadioButt_ProbabilityType;
		int     lag           = spinnerInteger_Lag;
		boolean skipZeroes    = booleanSkipZeroes;
		
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
		
		double[] resultValues = new double[numOfEntropies]; // 
		for (int r = 0; r < resultValues.length; r++) resultValues[r] = Float.NaN;
		
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
				
		double entropyValue = Float.NaN;
		CsajAlgorithm_GeneralisedEntropies ge;
		
		//"Entire sequence", "Subsequent boxes", "Gliding box" 
		//********************************************************************************************************
		if (sequenceRange.equals("Entire sequence")){	
	
			if (surrType.equals("No surrogates")) {		
				resultValues = new double[numOfEntropies]; // 		
				for (int r = 0; r < resultValues.length; r++) resultValues[r] = Float.NaN;
				
				//logService.info(this.getClass().getName() + " Column #: "+ (col+1) + "  " + sequenceColumn.getHeader() + "  Size of sequence = " + sequence1D.length);	
				//if (sequence1D.length == 0) return null; //e.g. if sequence had only NaNs
				
				probabilities = compProbabilities(sequence1D, lag, probType);				
				ge = new CsajAlgorithm_GeneralisedEntropies(probabilities);
				
				genEntSE      = ge.compSE();
				genEntH       = ge.compH();	//H1 H2 H3 
				genEntRenyi   = ge.compRenyi  (minQ, maxQ, numQ);
				genEntTsallis = ge.compTsallis(minQ, maxQ, numQ);	
				genEntSNorm   = ge.compSNorm  (minQ, maxQ, numQ);	
				genEntSEscort = ge.compSEscort(minQ, maxQ, numQ);	
				genEntSEta    = ge.compSEta   (minEta,   maxEta,   stepEta,   numEta);	
				genEntSKappa  = ge.compSKappa (minKappa, maxKappa, stepKappa, numKappa);	
				genEntSB      = ge.compSB     (minB,     maxB,     stepB,     numB);	
				genEntSBeta   = ge.compSBeta  (minBeta,  maxBeta,  stepBeta,  numBeta);	
				genEntSGamma  = ge.compSGamma (minGamma, maxGamma, stepGamma, numGamma);

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
				
				probabilities = compProbabilities(sequence1D, lag, probType);		
				ge = new CsajAlgorithm_GeneralisedEntropies(probabilities);
				
				//"SE", "H1", "H2", "H3", "Renyi", "Tsallis", "SNorm", "SEscort", "SEta", "SKappa", "SB", "SBeta", "SGamma"
				if (choiceRadioButt_EntropyType.equals("SE"))           entropyValue = ge.compSE();
				else if (choiceRadioButt_EntropyType.equals("H1"))      entropyValue = (ge.compH())[0]; 
				else if (choiceRadioButt_EntropyType.equals("H2"))      entropyValue = (ge.compH())[1];
				else if (choiceRadioButt_EntropyType.equals("H3"))      entropyValue = (ge.compH())[2]; 
				else if (choiceRadioButt_EntropyType.equals("Renyi"))   entropyValue = (ge.compRenyi  (minQ, maxQ, numQ))[0]; //value for min
				else if (choiceRadioButt_EntropyType.equals("Tsallis")) entropyValue = (ge.compTsallis(minQ, maxQ, numQ))[0]; //value for min
				else if (choiceRadioButt_EntropyType.equals("SNorm"))   entropyValue = (ge.compSNorm  (minQ, maxQ, numQ))[0]; //value for min
				else if (choiceRadioButt_EntropyType.equals("SEscort")) entropyValue = (ge.compSEscort(minQ, maxQ, numQ))[0]; //value for min
				else if (choiceRadioButt_EntropyType.equals("SEta"))    entropyValue = (ge.compSEta   (minEta,   maxEta,   stepEta,   numEta))[0];//value for min 
				else if (choiceRadioButt_EntropyType.equals("SKappa"))  entropyValue = (ge.compSKappa (minKappa, maxKappa, stepKappa, numKappa))[0]; //value for min
				else if (choiceRadioButt_EntropyType.equals("SB"))      entropyValue = (ge.compSB     (minB,     maxB,     stepB,     numB))[0]; //value for min
				else if (choiceRadioButt_EntropyType.equals("SBeta"))   entropyValue = (ge.compSBeta  (minBeta,  maxBeta,  stepBeta,  numBeta))[0]; //value for min
				else if (choiceRadioButt_EntropyType.equals("SGamma"))  entropyValue = (ge.compSGamma (minGamma, maxGamma, stepGamma, numGamma))[0]; //value for min

				resultValues[0] = entropyValue;
				int lastMainResultsIndex = 0;
				
				surrSequence1D = new double[sequence1D.length];
				
				double sumEntropies   = 0.0f;
				CsajAlgorithm_Surrogate1D surrogate1D = new CsajAlgorithm_Surrogate1D();
				String windowingType = "Rectangular";
				for (int s = 0; s < numSurrogates; s++) {
					//choices = {"No surrogates", "Shuffle", "Gaussian", "Random phase", "AAFT"}, 	
					if      (surrType.equals("Shuffle"))      surrSequence1D = surrogate1D.calcSurrogateShuffle(sequence1D);
					else if (surrType.equals("Gaussian"))     surrSequence1D = surrogate1D.calcSurrogateGaussian(sequence1D);
					else if (surrType.equals("Random phase")) surrSequence1D = surrogate1D.calcSurrogateRandomPhase(sequence1D, windowingType);
					else if (surrType.equals("AAFT"))         surrSequence1D = surrogate1D.calcSurrogateAAFT(sequence1D, windowingType);
			
					probabilities = compProbabilities(surrSequence1D, lag, probType);
					ge = new CsajAlgorithm_GeneralisedEntropies(probabilities);
					
					//"SE", "H1", "H2", "H3", "Renyi", "Tsallis", "SNorm", "SEscort", "SEta", "SKappa", "SB", "SBeta", "SGamma"
					if (choiceRadioButt_EntropyType.equals("SE"))           entropyValue = ge.compSE();
					else if (choiceRadioButt_EntropyType.equals("H1"))      entropyValue = (ge.compH())[0]; 
					else if (choiceRadioButt_EntropyType.equals("H2"))      entropyValue = (ge.compH())[1];
					else if (choiceRadioButt_EntropyType.equals("H3"))      entropyValue = (ge.compH())[2]; 
					else if (choiceRadioButt_EntropyType.equals("Renyi"))   entropyValue = (ge.compRenyi  (minQ, maxQ, numQ))[0]; //value for min
					else if (choiceRadioButt_EntropyType.equals("Tsallis")) entropyValue = (ge.compTsallis(minQ, maxQ, numQ))[0]; //value for min
					else if (choiceRadioButt_EntropyType.equals("SNorm"))   entropyValue = (ge.compSNorm  (minQ, maxQ, numQ))[0]; //value for min
					else if (choiceRadioButt_EntropyType.equals("SEscort")) entropyValue = (ge.compSEscort(minQ, maxQ, numQ))[0]; //value for min
					else if (choiceRadioButt_EntropyType.equals("SEta"))    entropyValue = (ge.compSEta   (minEta,   maxEta,   stepEta,   numEta))[0];//value for min 
					else if (choiceRadioButt_EntropyType.equals("SKappa"))  entropyValue = (ge.compSKappa (minKappa, maxKappa, stepKappa, numKappa))[0]; //value for min
					else if (choiceRadioButt_EntropyType.equals("SB"))      entropyValue = (ge.compSB     (minB,     maxB,     stepB,     numB))[0]; //value for min
					else if (choiceRadioButt_EntropyType.equals("SBeta"))   entropyValue = (ge.compSBeta  (minBeta,  maxBeta,  stepBeta,  numBeta))[0]; //value for min
					else if (choiceRadioButt_EntropyType.equals("SGamma"))  entropyValue = (ge.compSGamma (minGamma, maxGamma, stepGamma, numGamma))[0]; //value for min
						
					resultValues[lastMainResultsIndex + 2 + s] = entropyValue;
					sumEntropies += entropyValue;
				}
				resultValues[lastMainResultsIndex + 1] = sumEntropies/numSurrogates;
			}
		
		//********************************************************************************************************	
		} else if (sequenceRange.equals("Subsequent boxes")){
			resultValues = new double[(int) (numSubsequentBoxes)]; // only one of possible Entropy values, Dim R2 == two * number of boxes		
			for (int r = 0; r<resultValues.length; r++) resultValues[r] = Float.NaN;
			subSequence1D = new double[(int) numBoxLength];
			//number of boxes may be smaller than intended because of NaNs or removed zeroes
			long actualNumSubsequentBoxes = (long) Math.floor((double)sequence1D.length/(double)spinnerInteger_BoxLength);
		
			//get sub-sequences and compute dimensions
			for (int i = 0; i < actualNumSubsequentBoxes; i++) {	
				logService.info(this.getClass().getName() + " Processing subsequent box #: "+(i+1) + "/" + actualNumSubsequentBoxes);	
				int start = (i*numBoxLength);
				for (int ii = start; ii < (start + numBoxLength); ii++){ 
					subSequence1D[ii-start] = sequence1D[ii];
				}
				//Compute specific values************************************************
				probabilities = compProbabilities(subSequence1D, lag, probType);	
				ge = new CsajAlgorithm_GeneralisedEntropies(probabilities);
				//"SE", "H1", "H2", "H3", "Renyi", "Tsallis", "SNorm", "SEscort", "SEta", "SKappa", "SB", "SBeta", "SGamma"
				if (choiceRadioButt_EntropyType.equals("SE"))           entropyValue = ge.compSE();
				else if (choiceRadioButt_EntropyType.equals("H1"))      entropyValue = (ge.compH())[0];
				else if (choiceRadioButt_EntropyType.equals("H2"))      entropyValue = (ge.compH())[1];
				else if (choiceRadioButt_EntropyType.equals("H3"))      entropyValue = (ge.compH())[2]; 
				else if (choiceRadioButt_EntropyType.equals("Renyi"))   entropyValue = (ge.compRenyi  (minQ, maxQ, numQ))[0]; //value for min
				else if (choiceRadioButt_EntropyType.equals("Tsallis")) entropyValue = (ge.compTsallis(minQ, maxQ, numQ))[0]; //value for min
				else if (choiceRadioButt_EntropyType.equals("SNorm"))   entropyValue = (ge.compSNorm  (minQ, maxQ, numQ))[0]; //value for min
				else if (choiceRadioButt_EntropyType.equals("SEscort")) entropyValue = (ge.compSEscort(minQ, maxQ, numQ))[0]; //value for min
				else if (choiceRadioButt_EntropyType.equals("SEta"))    entropyValue = (ge.compSEta   (minEta,   maxEta,   stepEta,   numEta))[0];//value for min 
				else if (choiceRadioButt_EntropyType.equals("SKappa"))  entropyValue = (ge.compSKappa (minKappa, maxKappa, stepKappa, numKappa))[0]; //value for min
				else if (choiceRadioButt_EntropyType.equals("SB"))      entropyValue = (ge.compSB     (minB,     maxB,     stepB,     numB))[0]; //value for min
				else if (choiceRadioButt_EntropyType.equals("SBeta"))   entropyValue = (ge.compSBeta  (minBeta,  maxBeta,  stepBeta,  numBeta))[0]; //value for min
				else if (choiceRadioButt_EntropyType.equals("SGamma"))  entropyValue = (ge.compSGamma (minGamma, maxGamma, stepGamma, numGamma))[0]; 	//value for min
				resultValues[i] = entropyValue;			
				//***********************************************************************
			}	
		//********************************************************************************************************			
		} else if (sequenceRange.equals("Gliding box")){
			resultValues = new double[(int) (numGlidingBoxes)]; // only one of possible Entropy values,  Dim R2 == two * number of boxes	
			for (int r = 0; r<resultValues.length; r++) resultValues[r] = Float.NaN;
			subSequence1D = new double[(int) numBoxLength];
			//number of boxes may be smaller because of NaNs or removed zeroes
			long actualNumGlidingBoxes = sequence1D.length - spinnerInteger_BoxLength + 1;
			
			//get sub-sequences and compute dimensions
			for (int i = 0; i < actualNumGlidingBoxes; i++) {
				logService.info(this.getClass().getName() + " Processing gliding box #: "+(i+1) + "/" + actualNumGlidingBoxes);	
				int start = i;
				for (int ii = start; ii < (start + numBoxLength); ii++){ 
					subSequence1D[ii-start] = sequence1D[ii];
				}	
				//Compute specific values************************************************
				probabilities = compProbabilities(subSequence1D, lag, probType);	
				ge = new CsajAlgorithm_GeneralisedEntropies(probabilities);
				
				//"SE", "H1", "H2", "H3", "Renyi", "Tsallis", "SNorm", "SEscort", "SEta", "SKappa", "SB", "SBeta", "SGamma"
				if (choiceRadioButt_EntropyType.equals("SE"))           entropyValue = ge.compSE();
				else if (choiceRadioButt_EntropyType.equals("H1"))      entropyValue = (ge.compH())[0];
				else if (choiceRadioButt_EntropyType.equals("H2"))      entropyValue = (ge.compH())[1];
				else if (choiceRadioButt_EntropyType.equals("H3"))      entropyValue = (ge.compH())[2]; 
				else if (choiceRadioButt_EntropyType.equals("Renyi"))   entropyValue = (ge.compRenyi  (minQ, maxQ, numQ))[0]; 
				else if (choiceRadioButt_EntropyType.equals("Tsallis")) entropyValue = (ge.compTsallis(minQ, maxQ, numQ))[0]; 
				else if (choiceRadioButt_EntropyType.equals("SNorm"))   entropyValue = (ge.compSNorm  (minQ, maxQ, numQ))[0]; 
				else if (choiceRadioButt_EntropyType.equals("SEscort")) entropyValue = (ge.compSEscort(minQ, maxQ, numQ))[0]; 
				else if (choiceRadioButt_EntropyType.equals("SEta"))    entropyValue = (ge.compSEta   (minEta,   maxEta,   stepEta,   numEta))[0]; 
				else if (choiceRadioButt_EntropyType.equals("SKappa"))  entropyValue = (ge.compSKappa (minKappa, maxKappa, stepKappa, numKappa))[0]; 
				else if (choiceRadioButt_EntropyType.equals("SB"))      entropyValue = (ge.compSB     (minB,     maxB,     stepB,     numB))[0]; 
				else if (choiceRadioButt_EntropyType.equals("SBeta"))   entropyValue = (ge.compSBeta  (minBeta,  maxBeta,  stepBeta,  numBeta))[0]; 
				else if (choiceRadioButt_EntropyType.equals("SGamma"))  entropyValue = (ge.compSGamma (minGamma, maxGamma, stepGamma, numGamma))[0]; 	
				resultValues[i] = entropyValue;		
				//***********************************************************************
			}
		}	
		return new CsajContainer_ProcessMethod(resultValues);
		// SampEn or AppEn
		// Output
		// uiService.show(TABLE_OUT_NAME, table);
	}
	
	//------------------------------------------------------------------------------------------------------
	/**
	 * This computes probabilities of actual values
	 * 
	 * @param sequence
	 * @param lag
	 * @param probOption
	 * @return probabilities[]
	 */
	//"Sequence values", "Pairwise differences", "Sum of differences", "SD"
	private double[] compProbabilities(double[] sequence, int lag, String probType) {
		if (probType.equals("Sequence values")) lag = 0; //to be sure that eps = 0 for that case
		double sequenceMin = Double.MAX_VALUE;
		double sequenceMax = -Double.MAX_VALUE;
		double[] sequenceDouble = null;
		
		if (probType.equals("Sequence values")) {//Actual values without lag
			sequenceDouble = new double[sequence.length]; 
			for (int i = 0; i < sequenceDouble.length; i++) {
				sequenceDouble[i] = sequence[i];
				if (sequenceDouble[i] < sequenceMin) sequenceMin = sequenceDouble[i];  
				if (sequenceDouble[i] > sequenceMax) sequenceMax = sequenceDouble[i];  
			}
		}
		if (probType.equals("Pairwise differences")) {//Pairwise differences
			sequenceDouble = new double[sequence.length - lag]; 
			for (int i = 0; i < sequenceDouble.length; i++) {
				sequenceDouble[i] = Math.abs(sequence[i+lag] - sequence[i]); //Difference
				if (sequenceDouble[i] < sequenceMin) sequenceMin = sequenceDouble[i];  
				if (sequenceDouble[i] > sequenceMax) sequenceMax = sequenceDouble[i];  
			}
		}
		if (probType.equals("Sum of differences")) {//Sum of differences in between lag == integral
			sequenceDouble = new double[sequence.length - lag]; 
			for (int i = 0; i < sequenceDouble.length; i++) {
				double sum = 0.0;
				for (int ii = 0; ii < lag; ii++) {
					sum = sum + Math.abs(sequence[i+ii+1] - sequence[i+ii]); //Sum of differences
				}
				sequenceDouble[i] = sum;
				if (sequenceDouble[i] < sequenceMin) sequenceMin = sequenceDouble[i];  
				if (sequenceDouble[i] > sequenceMax) sequenceMax = sequenceDouble[i];  
			}
		}
		if (probType.equals("SD")) {//SD in between lag
			sequenceDouble = new double[sequence.length - lag]; 
			for (int i = 0; i < sequenceDouble.length; i++) {
				double mean = 0.0;
				for (int ii = 0; ii < lag; ii++) {
					mean = mean + sequence[i+ii]; //Sum for Mean
				}
				mean = mean/((double)lag);
				double sumDiff2 = 0.0;
				for (int ii = 0; ii <= lag; ii++) {
					sumDiff2 = sumDiff2 + Math.pow(sequence[i+ii] - mean, 2); //Difference
				}	
				sequenceDouble[i] = Math.sqrt(sumDiff2/((double)lag));
				if (sequenceDouble[i] < sequenceMin) sequenceMin = sequenceDouble[i];  
				if (sequenceDouble[i] > sequenceMax) sequenceMax = sequenceDouble[i];  
			}
		}
	
		//Apache
		int binNumber = 1000;
		int binSize = (int) ((sequenceMax - sequenceMin)/binNumber);
		long[] histogram = new long[binNumber];
		EmpiricalDistribution distribution = new EmpiricalDistribution(binNumber);
		distribution.load(sequenceDouble);
		int k = 0;
		for(SummaryStatistics stats: distribution.getBinStats())
		{
		    histogram[k++] = stats.getN();
		}   

//	    double xValues[] = new double[binNumber];
//        for (int i = 0; i < binNumber; i++) {
//            if (i == 0){
//                xValues[i] = sequenceMin;
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
		if (this.choiceRadioButt_SequenceRange.equals("Entire sequence")) {
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
		
		// open and display a sequence, waiting for the operation to finish.
		ij.command().run(Csaj1DOpenerCommand.class, true).get();
		//open and run Plugin
		ij.command().run(MethodHandles.lookup().lookupClass().getName(), true);
	}
}
