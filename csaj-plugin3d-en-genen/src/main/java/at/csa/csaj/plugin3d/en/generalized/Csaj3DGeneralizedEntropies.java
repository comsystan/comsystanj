/*-
 * #%L
 * Project: ImageJ2 plugin for computing 3D Generalized entropies
 * File: Csaj3DGeneralizedEntropies.java
 * 
 * $Id$
 * $HeadURL$
 * 
 * This file is part of ComsystanJ software, hereinafter referred to as "this program".
 * %%
 * Copyright (C) 2022 - 2024 Comsystan Software
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


package at.csa.csaj.plugin3d.en.generalized;

import java.awt.Frame;
import java.awt.Toolkit;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ImageJ;
import net.imagej.Position;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.display.ImageDisplayService;
import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import org.apache.commons.math3.random.EmpiricalDistribution;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.util.Precision;
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
import org.scijava.table.DefaultGenericTable;
import org.scijava.table.DoubleColumn;
import org.scijava.table.GenericColumn;
import org.scijava.ui.DialogPrompt.MessageType;
import org.scijava.ui.DialogPrompt.OptionType;
import org.scijava.ui.DialogPrompt.Result;
import org.scijava.ui.UIService;
import org.scijava.widget.Button;
import org.scijava.widget.ChoiceWidget;
import org.scijava.widget.FileWidget;
import org.scijava.widget.NumberWidget;
import at.csa.csaj.commons.algorithms.GeneralizedEntropies;
import at.csa.csaj.commons.dialog.WaitingDialogWithProgressBar;
import at.csa.csaj.commons.plot.RegressionPlotFrame;
import at.csa.csaj.commons.plot.SequencePlotFrame;
import io.scif.DefaultImageMetadata;
import io.scif.MetaTable;

/**
 * A {@link ContextCommand} plugin computing <3D Generalized entropies</a>
 * of an image volume.
 * 
 * * A {@link Command} plugin computing <Generalised entropies</a>
 * of a sequence.
 * <li>according to a review of Amigó, J.M., Balogh, S.G., Hernández, S., 2018. A Brief Review of Generalized Entropies. Entropy 20, 813. https://doi.org/10.3390/e20110813
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
 */
@Plugin(type = ContextCommand.class,
headless = true,
label = "3D Generalized entropies",
initializer = "initialPluginLaunch",
//iconPath = "/images/comsystan-??.png", //Menu entry icon
menu = {
@Menu(label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT, mnemonic = MenuConstants.PLUGINS_MNEMONIC),
@Menu(label = "ComsystanJ"),
@Menu(label = "3D Volume"),
@Menu(label = "3D Generalized Entropies", weight = 90)})
//public class Csaj3DGeneralizedEntropies<T extends RealType<T>> extends InteractiveCommand { // non blocking  GUI
public class Csaj3DGeneralizedEntropies<T extends RealType<T>> extends ContextCommand implements Previewable { //modal GUI with cancel

	private static final String PLUGIN_LABEL            = "<html><b>Computes 3D Generalized entropies</b></html>";
	private static final String SPACE_LABEL             = "";
	private static final String ENTROPYOPTIONS_LABEL    = "<html><b>Entropy options</b></html>";
	private static final String BACKGROUNDOPTIONS_LABEL = "<html><b>Background option</b></html>";
	private static final String DISPLAYOPTIONS_LABEL    = "<html><b>Display options</b></html>";
	private static final String PROCESSOPTIONS_LABEL    = "<html><b>Process options</b></html>";

	private static Img<FloatType> imgFloat; 
	private static Img<UnsignedByteType> imgUnsignedByte;
	private static RandomAccess<?> ra;
	private static Cursor<?> cursor = null;
	private static String datasetName;
	private static long width = 0;
	private static long height = 0;
	private static long depth = 0;
	private static long numDimensions = 0;
	private static long numSlices = 0;
	private static int numVolumes = 0;
	private static long compositeChannelCount = 0;
	private static String imageType = "";
	private static int  numBoxes = 0;
	private static ArrayList<SequencePlotFrame> genRenyiPlotList = new ArrayList<SequencePlotFrame>();
	private static double[] resultValuesTable; //the corresponding regression values
	private static final String tableOutName = "Table - 3D Generalized entropies";
	
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
	
	double[] resultValues; //Entropy values
	
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
	private ImageDisplayService imageDisplayService;
	
	//@Parameter
	//private DefaultThreadService defaultThreadService;

	// This parameter does not work in an InteractiveCommand plugin
	// -->> (duplicate displayService error during startup) pom-scijava 24.0.0
	// no problem in a Command Plugin
	//@Parameter
	//private DisplayService displayService;

	@Parameter // This works in an InteractiveCommand plugin
	private DefaultDisplayService defaultDisplayService;

	@Parameter
	private DatasetService datasetService;

	@Parameter(label = tableOutName, type = ItemIO.OUTPUT)
	private DefaultGenericTable tableOut;


	// Widget elements------------------------------------------------------
	//-----------------------------------------------------------------------------------------------------
	//@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	//private final String labelPlugin = PLUGIN_LABEL;

	//@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	//private final String labelSpace = SPACE_LABEL;

	// Input dataset which is updated in callback functions
	@Parameter(type = ItemIO.INPUT)
	private Dataset datasetIn;

	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelEntropyOptions = ENTROPYOPTIONS_LABEL;
     
 	@Parameter(label = "Probability type",
			description = "Selection of probability type",
			style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
			choices = {"Grey values", "Pairwise differences"}, // "Sum of differences", "SD"}, 
			persist = true,  //restore previous value default = true
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
	private final String labelDisplayOptions = DISPLAYOPTIONS_LABEL;

	@Parameter(label = "Show Renyi plot",
		       persist = true,  //restore previous value default = true
		       initializer = "initialShowRenyiPlot")
	 private boolean booleanShowRenyiPlot;

	@Parameter(label = "Overwrite result display(s)",
	    	description = "Overwrite already existing result images, plots or tables",
	    	persist = true,  //restore previous value default = true
			initializer = "initialOverwriteDisplays")
	private boolean booleanOverwriteDisplays;

	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelProcessOptions = PROCESSOPTIONS_LABEL;

	@Parameter(label = "Immediate processing", visibility = ItemVisibility.INVISIBLE, persist = false,
	    	description = "Immediate processing of active image when a parameter is changed",
			callback = "callbackProcessImmediately")
	private boolean booleanProcessImmediately;
	
//	@Parameter(label = "Image #", description = "Image slice number", style = NumberWidget.SPINNER_STYLE, min = "1", max = "99999999", stepSize = "1",
//			   persist = false, // restore  previous value  default  =  true
//			   initializer = "initialNumImageSlice",
//			   callback = "callbackNumImageSlice")
//	private int spinnerInteger_NumImageSlice;
	
	@Parameter(label   = "    Process single volume     ",
		    	callback = "callbackProcessSingleVolume")
	private Button buttonProcessSingleVolume;
	
//	Deactivated, because it does not work in Fiji (although it works in ImageJ2 -Eclipse)	
//@Parameter(label   = "Process single active image ",
//		    callback = "callbackProcessActiveImage")
//	private Button buttonProcessActiveImage;

//	@Parameter(label = "Process all available images",
//			callback = "callbackProcessAllImages")
//	private Button buttonProcessAllImages;
	
	// ---------------------------------------------------------------------	
	protected void initialPluginLaunch() {
		//datasetIn = imageDisplayService.getActiveDataset();
		checkItemIOIn();
	}
	
	protected void initialProbabilityType() {
	 	choiceRadioButt_ProbabilityType = "Grey values"; //"Grey values", "Pairwise differences", "Sum of differences", "SD"
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
	 	numBeta = (int)((maxBeta - minBeta)/stepBeta  + 1);
	 }
	 	
	 protected void initialMaxBeta() {
	 	spinnerFloat_MaxBeta = 1.5f;
	 	maxBeta = Precision.round(spinnerFloat_MaxBeta, 1);
	 	numBeta = (int)((maxBeta - minBeta)/stepBeta  + 1);
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
	   
	   protected void initialShowRenyiPlot() {
	   	booleanShowRenyiPlot = true;
	 }
	 
	 protected void initialOverwriteDisplays() {
		 booleanOverwriteDisplays = true;
	 }
		 
	// ------------------------------------------------------------------------------		
	/** Executed whenever the {@link #choiceRadioButt_ProbabilityType} parameter changes. */
	protected void callbackProbabilityType() {
		logService.info(this.getClass().getName() + " Propability type set to " + choiceRadioButt_ProbabilityType);
		if (choiceRadioButt_ProbabilityType.contains("Grey values")) {
			logService.info(this.getClass().getName() + " Grey values only with lag = 1");
			spinnerInteger_Lag = 1;
		}
	}
		
	/** Executed whenever the {@link #spinnerInteger_Lag} parameter changes. */
	protected void callbackLag() {
		if (choiceRadioButt_ProbabilityType.contains("Grey values")) {
			logService.info(this.getClass().getName() + " Grey values only with lag = 1");
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
		numBeta = (int)((maxBeta - minBeta)/stepBeta  + 1);
		logService.info(this.getClass().getName() + " Minimal Beta set to " + spinnerFloat_MinBeta);
	}
		
	/** Executed whenever the {@link #spinFloat_MaxBeta} parameter changes. */
	protected void callbackMaxBeta() {
		maxBeta = Precision.round(spinnerFloat_MaxBeta, 1);
		numBeta = (int)((maxBeta - minBeta)/stepBeta  + 1);
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
			
	/** Executed whenever the {@link #booleanProcessImmediately} parameter changes. */
	protected void callbackProcessImmediately() {
		logService.info(this.getClass().getName() + " Process immediately set to " + booleanProcessImmediately);
	}
	
//	/** Executed whenever the {@link #spinnerInteger_NumImageSlice} parameter changes. */
//	protected void callbackNumImageSlice() {
//		if (spinnerInteger_NumImageSlice > numSlices){
//			logService.info(this.getClass().getName() + " No more images available");
//			spinnerInteger_NumImageSlice = (int)numSlices;
//		}
//		logService.info(this.getClass().getName() + " Image slice number set to " + spinnerInteger_NumImageSlice);
//	}
	
	/**
	 * Executed whenever the {@link #buttonProcessSingleImage} button is pressed.
	 * It is not executed in the same exact manner such as run()
	 * So a thread for displaying properly the Progressbar window is needed
	 * Execution of the code is then not on the Event Dispatch Thread EDT, where all GUI windows are executed
	 * The @Parameter ItemIO.OUTPUT is not automatically shown 
	 */
	protected void callbackProcessSingleVolume() {
		//prepare  executer service
		exec = Executors.newSingleThreadExecutor();
	   	exec.execute(new Runnable() {
	        public void run() {
	    	    startWorkflowForSingleVolume();
	    	   	uiService.show(tableOutName, tableOut);
	        }
	    });
	   	exec.shutdown(); //No new tasks
	}
	
	/** Executed whenever the {@link #buttonProcessActiveImage} button is pressed.*/
	protected void callbackProcessActiveImage() {
	
	}
	
	/**
	 * Executed whenever the {@link #buttonProcessAllImages} button is pressed.
	 * It is not executed in the same exact manner such as run()
	 * So a thread for displaying properly the Progressbar window is needed
	 * Execution of the code is then not on the Event Dispatch Thread EDT, where all GUI windows are executed
	 * The @Parameter ItemIO.OUTPUT is not automatically shown 
	 */
	protected void callbackProcessAllImages() {
		//prepare  executer service
		exec = Executors.newSingleThreadExecutor();
	   	exec.execute(new Runnable() {
	        public void run() {
	        	startWorkflowForSingleVolume();
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
		    	    startWorkflowForSingleVolume();
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
	    startWorkflowForSingleVolume();
	}

	public void checkItemIOIn() {

		//datasetIn = imageDisplayService.getActiveDataset();

		if ((datasetIn.firstElement() instanceof UnsignedByteType) || (datasetIn.firstElement() instanceof FloatType)) {
			// That is OK, proceed
		} else {

			final MessageType messageType = MessageType.WARNING_MESSAGE;
			final OptionType optionType = OptionType.DEFAULT_OPTION;
			final String title = "Image type validation result";
			final String message = "Data type not allowed: " + datasetIn.getType().getClass().getSimpleName();
			// Prompt for confirmation.
			// final UIService uiService = getContext().getService(UIService.class);
			Result result = uiService.showDialog(message, title, messageType, optionType);
			// Cancel the command execution if the user does not agree.
			// if (result != Result.YES_OPTION) System.exit(-1);
			// if (result != Result.YES_OPTION) return;
		}
		// get some info
		width = datasetIn.dimension(0);
		height = datasetIn.dimension(1);
		//depth = dataset.getDepth(); //does not work if third axis ist not specifyed as z-Axis
		numDimensions = datasetIn.numDimensions();
	
		//compositeChannelCount = datasetIn.getImgPlus().getCompositeChannelCount(); //1  Grey,   3 RGB
		compositeChannelCount = datasetIn.getCompositeChannelCount();
		if ((numDimensions == 2) && (compositeChannelCount == 1)) { //single Grey image
			numSlices = 1;
			imageType = "Grey";
		} else if ((numDimensions == 3) && (compositeChannelCount == 1)) { // Grey stack	
			numSlices = datasetIn.dimension(2); //x,y,z
			imageType = "Grey";
		} else if ((numDimensions == 3) && (compositeChannelCount == 3)) { //Single RGB image	
			numSlices = 1;
			imageType = "RGB";
		} else if ((numDimensions == 4) && (compositeChannelCount == 3)) { // RGB stack	x,y,composite,z
			numSlices = datasetIn.dimension(3); //x,y,composite,z
			imageType = "RGB";
		}

		// get the name of dataset
		datasetName = datasetIn.getName();
		
		try {
			Map<String, Object> prop = datasetIn.getProperties();
			DefaultImageMetadata metaData = (DefaultImageMetadata) prop.get("scifio.metadata.image");
			MetaTable metaTable = metaData.getTable();
		} catch (NullPointerException npe) {
			// TODO Auto-generated catch block
			//npe.printStackTrace();
			logService.info(this.getClass().getName() + " WARNING: It was not possible to read scifio metadata."); 
		}
  	
		logService.info(this.getClass().getName() + " Name: " + datasetName); 
		logService.info(this.getClass().getName() + " Image size = " + width+"x"+height); 
		logService.info(this.getClass().getName() + " Image type: " + imageType); 
		logService.info(this.getClass().getName() + " Number of images = "+ numSlices); 
		
		stepQ     = 1;
		stepEta   = 0.1f;
		stepKappa = 0.1f;
		stepB     = 1.0f;
		stepBeta  = 0.1f;
		stepGamma = 0.1f;
		
		//RGB not allowed
		if (!imageType.equals("Grey")) { 
			logService.info(this.getClass().getName() + " WARNING: Grey value image volume expected!");
			this.cancel("WARNING: Grey value image volume expected!");
		}
	}

	/**
	* This method starts the workflow for a single image of the active display
	*/
	protected void startWorkflowForSingleVolume() {
	
		dlgProgress = new WaitingDialogWithProgressBar("Computing 3D Generalized entropies, please wait... Open console window for further info.",
							logService, false, exec); //isCanceable = false, because no following method listens to exec.shutdown 
		dlgProgress.updatePercent("");
		dlgProgress.setBarIndeterminate(true);
		dlgProgress.setVisible(true);
    	deleteExistingDisplays();
        logService.info(this.getClass().getName() + " Processing volume...");
		processSingleInputVolume();
		dlgProgress.addMessage("Processing finished! Collecting data for table...");
		generateTableHeader();
		writeSingleResultToTable();
		dlgProgress.setVisible(false);
		dlgProgress.dispose();
		Toolkit.getDefaultToolkit().beep();
	}

	/**
	 * This methods gets the index of the active image in a stack
	 * @return int index
	 */
	private int getActiveImageIndex() {
		int activeSliceIndex = 0;
		try {
			//This works in eclipse but not as jar in the plugin folder of fiji 
			//SCIFIO activated: throws a NullPointerException
			//SCIFIO deactivated: gives always back index = 0! 
			Position pos = imageDisplayService.getActivePosition();
			activeSliceIndex = (int) pos.getIndex();
			
			//This gives always back 0, SCIFIO setting does not matter
			//int activeSliceNumber = (int) imageDisplayService.getActiveImageDisplay().getActiveView().getPlanePosition().getIndex();
			//???
			//int activeSliceNumber = (int) defaultImageDisplayService.getActivePosition().getIndex(); 
			//int activeSliceNumber2 = (int) defaultImageDisplayService.getActiveImageDisplay().getActiveView().getPlanePosition().getIndex();
			
			//Check if Grey or RGB
			if (imageType.equals("Grey")) {
				//do nothing, it is OK
			};
			if (imageType.equals("RGB")) {
				//At first Index runs through RGB channels and then through stack index
				//0... R of first RGB image, 1.. G of first RGB image, 2..B of first RGB image, 3... R of second RGB image, 4...G, 5...B,.......
				activeSliceIndex = (int) Math.floor((float)activeSliceIndex/3.f);
			}
			
			
		} catch (NullPointerException npe) {
			// TODO Auto-generated catch block
			//npe.printStackTrace();
			logService.info(this.getClass().getName() + " WARNING: It was not possible to get active slice index. Index set to first image.");
			activeSliceIndex = 0;
		} 
		logService.info(this.getClass().getName() + " Active slice index = " + activeSliceIndex);
		//logService.info(this.getClass().getName() + " Active slice index alternative = " + activeSliceNumber2);
		return activeSliceIndex;
	}
	
	/**
	 * This method deletes already open displays
	 * 
	 */
	private void deleteExistingDisplays() {
		
		boolean optDeleteExistingImgs   = false;
		boolean optDeleteExistingPlots  = false;
		boolean optDeleteExistingTables = false;
		if (booleanOverwriteDisplays) {
			optDeleteExistingImgs   = true;
			optDeleteExistingPlots  = true;
			optDeleteExistingTables = true;
		}

		if (optDeleteExistingImgs) {
//			//List<Display<?>> list = defaultDisplayService.getDisplays();
//			//for (int i = 0; i < list.size(); i++) {
//			//	display = list.get(i);
//			//	System.out.println("display name: " + display.getName());
//			//	if (display.getName().contains("Name")) display.close(); //does not close correctly in Fiji, it is only not available any more
//			//}			
//			//List<ImageDisplay> listImgs = defaultImageDisplayService.getImageDisplays(); //Is also not closed in Fiji 
//		
//			Frame frame;
//			Frame[] listFrames = JFrame.getFrames();
//			for (int i = listFrames.length -1 ; i >= 0; i--) { //Reverse order, otherwise focus is not given free from the last image
//				frame = listFrames[i];
//				//System.out.println("frame name: " + frame.getTitle());
//				if (frame.getTitle().contains("Name")) {
//					frame.setVisible(false); //Successfully closes also in Fiji
//					frame.dispose();
//				}
//			}
		}
		if (optDeleteExistingPlots) {
//			//This dose not work with DisplayService because the JFrame is not "registered" as an ImageJ display	
			if (genRenyiPlotList != null) {
				for (int l = 0; l < genRenyiPlotList.size(); l++) {
					genRenyiPlotList.get(l).setVisible(false);
					genRenyiPlotList.get(l).dispose();
					//genDimPlotList.remove(l);  /
				}
				genRenyiPlotList.clear();		
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
			Display<?> display;
			List<Display<?>> list = defaultDisplayService.getDisplays();
			for (int i = 0; i < list.size(); i++) {
				display = list.get(i);
				//System.out.println("display name: " + display.getName());
				if (display.getName().contains(tableOutName)) display.close();
			}			
		}
	}


	/** This method computes the maximal number of possible boxes*/
	private int getMaxBoxNumber(long width, long height, long depth) { 
		float boxWidth = 1f;
		int number = 1; 
		while ((boxWidth <= width) && (boxWidth <= height) && (boxWidth <= depth)) {
			boxWidth = boxWidth * 2;
			number = number + 1;
		}
		return number - 1;
	}

	/** This method takes the active image volume and computes results. 
	 *
	 **/
	private void processSingleInputVolume() {
		
		long startTime = System.currentTimeMillis();

		int numOfEntropies = 1 + 3 + 4*numQ + numEta + numKappa + numB + numBeta + numGamma;
		resultValuesTable = new double[numOfEntropies];
	
		//get rai
		RandomAccessibleInterval<T> rai = null;	
	
		rai =  (RandomAccessibleInterval<T>) datasetIn.getImgPlus(); //dim==3

		//Compute generalized entropies
		resultValues = process(rai);	
		//Gen entropies SE H1, H2, H3, .....
					
		logService.info(this.getClass().getName() + " Generalized entropy SE: " + resultValues[0]);
		//set values for output table
		for (int i = 0; i < resultValues.length; i++ ) {
			resultValuesTable[i] = resultValues[i]; 
		}
		
		//Set/Reset focus to DatasetIn display
		//may not work for all Fiji/ImageJ2 versions or operating systems
		Frame frame;
		Frame[] listFrames = JFrame.getFrames();
		for (int i = 0; i < listFrames.length; i++) {
			frame = listFrames[i];
			//System.out.println("frame name: " + frame.getTitle());
			if (frame.getTitle().contains(datasetIn.getName())) { //sometimes Fiji adds some characters to the frame title such as "(V)"
				frame.setVisible(true);
				frame.toFront();
				frame.requestFocus();
			}
		}
		
		long duration = System.currentTimeMillis() - startTime;
		TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
		SimpleDateFormat sdf = new SimpleDateFormat();
		sdf.applyPattern("HHH:mm:ss:SSS");
		logService.info(this.getClass().getName() + " Elapsed time: "+ sdf.format(duration));
	}

	

	/** Generates the table header {@code DefaultGenericTable} */
	private void generateTableHeader() {
		
		GenericColumn columnFileName  = new GenericColumn("File name");
		GenericColumn columnProbType  = new GenericColumn("Probability type");
		GenericColumn columnLag       = new GenericColumn("Lag");

	    tableOut = new DefaultGenericTable();
		tableOut.add(columnFileName);
		tableOut.add(columnProbType);	
		tableOut.add(columnLag);	
	
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

	}

	/** 
	*  writes current result to table
	*  @param int slice number of active image.
	*/
	private void writeSingleResultToTable() { 

		int tableColStart = 0;
		int tableColEnd   = 0;
		int tableColLast  = 0;
		
		//fill table with values
		tableOut.appendRow();
		tableOut.set("File name",   	 tableOut.getRowCount() - 1, datasetName);	
		tableOut.set("Probability type", tableOut.getRowCount() - 1, choiceRadioButt_ProbabilityType); 
		tableOut.set("Lag",              tableOut.getRowCount() - 1, spinnerInteger_Lag);    // Lag
		tableColLast = 2;
			
		int numParameters = resultValuesTable.length;
		tableColStart = tableColLast + 1;
		tableColEnd = tableColStart + numParameters;
		for (int c = tableColStart; c < tableColEnd; c++ ) {
			tableOut.set(c, tableOut.getRowCount() - 1, resultValuesTable[c-tableColStart]);
		}	
	}

	/**
	*
	* Processing
	*/
	private double[] process(RandomAccessibleInterval<?> rai) { //3Dvolume
	
		if (rai == null) {
			logService.info(this.getClass().getName() + " WARNING: rai==null, no image for processing!");
		}
		
		String  probType         = choiceRadioButt_ProbabilityType;
		int     lag              = spinnerInteger_Lag;
		boolean optShowRenyiPlot = booleanShowRenyiPlot;

//		if ((!colorModelType.equals("Binary")) && (regMin == 1)){
//			regMin = 2; //regMin == 1 (single pixel box is not possible for DBC algorithms)
//		}

		long width  = rai.dimension(0);
		long height = rai.dimension(1);
		long depth  = rai.dimension(2);
		
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
		
		//probabilities = compProbabilities(rai, lag, probType); //3D rai	grey
		probabilities = compProbabilities2(rai, lag, probType); //faster  //3D grey	
		GeneralizedEntropies ge = new GeneralizedEntropies(probabilities);
		
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
		
		
		//Collect some entropies for display
		double[] entList;
		double[] qList;		
		if (optShowRenyiPlot) {	
			int offset = 4; //S, H1, H2, H3
			entList = new double[numQ];
			qList   = new double[numQ];
			for (int q = 0; q < numQ; q++) {
				qList[q] = q + minQ;
				entList[q] = resultValues[offset + q];
			}
			
			boolean isLineVisible = false; //?
			String preName = "Volume-";
			String axisNameX = "";
			String axisNameY = "";
			
			axisNameX = "q";
			axisNameY = "Renyi";
		
			SequencePlotFrame dimGenPlot = DisplaySinglePlotXY(qList, entList, isLineVisible, "Generalized Renyi entropies", 
					preName + datasetName, axisNameX, axisNameY, "");
			genRenyiPlotList.add(dimGenPlot);
		}		
			
		return resultValues;
		//Output
		//uiService.show(tableOutName, table);
		////result = ops.create().img(image, new FloatType()); may not work in older Fiji versions
		//result = new ArrayImgFactory<>(new FloatType()).create(image.dimension(0), image.dimension(1)); 
		//table
	}
	
	/**
	 * This computes probabilities of actual values
	 * 
	 * @param sequence
	 * @param lag
	 * @param probOption
	 * @return probabilities[]
	 */
	//"Grey values" (, "Pairwise differences", "Sum of differences", "SD")
	private double[] compProbabilities(RandomAccessibleInterval<?> rai, int lag, String probType) {//3D rai grey
		//double volumeMin = Double.MAX_VALUE;
		//double volumeMax = -Double.MAX_VALUE;
		double volumeMin = 0;
		double volumeMax = 255;
		long width  = rai.dimension(0);
		long height = rai.dimension(1);
		long depth  = rai.dimension(2);
		double[] volumeDouble = null;
	
		if (probType.equals("Grey values")) {//Actual values without lag
			volumeDouble = new double[(int)(width*height*depth)]; 
			cursor = Views.iterable(rai).cursor();
			int i = 0;
			while (cursor.hasNext()) {
				cursor.fwd();
				volumeDouble[i] = (double)((UnsignedByteType) cursor.get()).getInteger();
				i++;
			}
		}
		else if (probType.equals("Pairwise differences")) {//Pairwise differences
			volumeDouble = new double[(int)(depth*height*(width-lag) + depth*width*(height-lag) + width*height*(depth-lag))];
			ra = rai.randomAccess(rai);
			long[] pos = new long[3];
			int sample1;
			int sample2;
			int i = 0;
			//x direction pairs
			for (int z = 0; z < depth; z++){
				for (int y = 0; y < height; y++){
					for (int x = 0; x < width - lag; x++){
						pos[0] = x;
						pos[1] = y;
						pos[2] = z;
						ra.setPosition(pos);
						sample1 = ((UnsignedByteType) ra.get()).get();
						pos[0] = x + lag;
						//pos[1] = y;
						//pos[2] = z;
						ra.setPosition(pos);
						sample2 = ((UnsignedByteType) ra.get()).get();	
						volumeDouble[i] = Math.abs(sample2-sample1);
						i++;
					}
				}
			}
			//y direction pairs
			for (int z = 0; z < depth; z++){
				for (int x = 0; x < width; x++){
					for (int y = 0; y < height - lag; y++){
						pos[0] = x;
						pos[1] = y;
						pos[2] = z;
						ra.setPosition(pos);
						sample1 = ((UnsignedByteType) ra.get()).get();
						//pos[0] = x;
						pos[1] = y + lag;
						//pos[2] = z;
						ra.setPosition(pos);
						sample2 = ((UnsignedByteType) ra.get()).get();	
						volumeDouble[i] = Math.abs(sample2-sample1);
						i++;
					}
				}	
			}
			//z direction pairs
			for (int x = 0; x < width; x++){
				for (int y = 0; y < width; y++){
					for (int z = 0; z < depth - lag; z++){
						pos[0] = x;
						pos[1] = y;
						pos[2] = z;
						ra.setPosition(pos);
						sample1 = ((UnsignedByteType) ra.get()).get();
						//pos[0] = x;
						//pos[1] = y;
						pos[2] = z + lag;
						ra.setPosition(pos);
						sample2 = ((UnsignedByteType) ra.get()).get();	
						volumeDouble[i] = Math.abs(sample2-sample1);
						i++;
					}
				}	
			}
				
		}
		else if (probType.equals("Sum of differences")) {//Sum of differences in between lag == integral
		}
		else if (probType.equals("SD")) {//SD in between lag
		}
	
		//Apache
		int binNumber = 255;
		int binSize = (int) ((volumeMax - volumeMin)/binNumber);
		long[] histogram = new long[binNumber];
		EmpiricalDistribution distribution = new EmpiricalDistribution(binNumber);
		distribution.load(volumeDouble);
		int k = 0;
		for(SummaryStatistics stats: distribution.getBinStats())
		{
		    histogram[k++] = stats.getN();
		}   

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
	 * This computes probabilities of actual values
	 * 
	 * @param sequence
	 * @param lag
	 * @param probOption
	 * @return probabilities[]
	 */
	//"Grey values" (, "Pairwise differences", "Sum of differences", "SD")
	private double[] compProbabilities2(RandomAccessibleInterval<?> rai, int lag, String probType) { //shorter computation
		long width    = rai.dimension(0);
		long height   = rai.dimension(1);
		long depth    = rai.dimension(2);
		int binNumber = 256;
		double[] pis = new double[binNumber]; 
		//double volumeMin = Double.MAX_VALUE;
		//double volumeMax = -Double.MAX_VALUE;
		double volumeMin = 0;
		double volumeMax = 255;
		double totalsMax = 0.0;
	
		if (probType.equals("Grey values")) {//Actual values
			
			int sample;
			//imgUnsignedByte = this.createImgUnsignedByte(rai);
			//cursor = imgUnsignedByte.cursor();
			cursor = Views.iterable(rai).localizingCursor();
			while (cursor.hasNext()) {
				cursor.fwd();
				sample = ((UnsignedByteType) cursor.get()).getInteger();
				pis[sample]++;
				totalsMax++;
			}
		}
		else if (probType.equals("Pairwise differences")) {//Pairwise differences
		
			ra = rai.randomAccess(rai);
			long[] pos = new long[3];
			int sample1;
			int sample2;
			
			//x direction pairs
			for (int z = 0; z < depth; z++){
				for (int y = 0; y < height; y++){
					for (int x = 0; x < width - lag; x++){
						pos[0] = x;
						pos[1] = y;
						pos[2] = z;
						ra.setPosition(pos);
						sample1 = ((UnsignedByteType) ra.get()).get();
						pos[0] = x + lag;
						//pos[1] = y;
						//pos[2] = z;
						ra.setPosition(pos);
						sample2 = ((UnsignedByteType) ra.get()).get();	
						pis[Math.abs(sample2-sample1)]++;
						totalsMax++;
					}
				}
			}
			//y direction pairs
			for (int z = 0; z < depth; z++){
				for (int x = 0; x < width; x++){
					for (int y = 0; y < height - lag; y++){
						pos[0] = x;
						pos[1] = y;
						pos[2] = z;
						ra.setPosition(pos);
						sample1 = ((UnsignedByteType) ra.get()).get();
						//pos[0] = x;
						pos[1] = y + lag;
						//pos[2] = z;
						ra.setPosition(pos);
						sample2 = ((UnsignedByteType) ra.get()).get();	
						pis[Math.abs(sample2-sample1)]++;
						totalsMax++;
					}
				}	
			}
			//z direction pairs
			for (int x = 0; x < width; x++){
				for (int y = 0; y < width; y++){
					for (int z = 0; z < depth - lag; z++){
						pos[0] = x;
						pos[1] = y;
						pos[2] = z;
						ra.setPosition(pos);
						sample1 = ((UnsignedByteType) ra.get()).get();
						//pos[0] = x;
						//pos[1] = y;
						pos[2] = z + lag;
						ra.setPosition(pos);
						sample2 = ((UnsignedByteType) ra.get()).get();	
						pis[Math.abs(sample2-sample1)]++;
						totalsMax++;
					}
				}	
			}
			
			
		}
		else if (probType.equals("Sum of differences")) {//Sum of differences in between lag  == integral
		}
		else if (probType.equals("SD")) {//SD in between lag
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
	 * This method calculates the number of pixels >0 param RandomAccessibleInterval<?> rai
	 * return double
	 */
	private long getNumberOfNonZeroPixels(RandomAccessibleInterval<?> rai) {
		long total = 0;
		cursor = Views.iterable(rai).localizingCursor();
		while (cursor.hasNext()) { //Box
			cursor.fwd();
			//cursor.localize(pos);				
			if (((UnsignedByteType) cursor.get()).get() > 0) {
				total++; // Binary Image: 0 and [1, 255]! and not: 0 and 255
			}			
		}//while 
		return total;
	}
	
	//This methods reduces dimensionality to 2D just for the display 	
	//****IMPORTANT****Displaying a rai slice (pseudo 2D) directly with e.g. uiService.show(name, rai);
	//pushes a 3D array to the display and
	//yields mouse moving errors because the third dimension is not available
	private <T extends Type<T>, F> void displayImage(String name, IterableInterval<FloatType> iv) {

		// Create an image.
		long[] dims = {iv.max(0)+1, iv.max(0)+1};
		AxisType[] axes = {Axes.X, Axes.Y};
		int bitsPerPixel = 32;
		boolean signed = true;
		boolean floating = true;
		boolean virtual = false;
		//dataset = ij.dataset().create(dims, name, axes, bitsPerPixel, signed, floating);
		Dataset datasetDisplay = datasetService.create(dims, name, axes, bitsPerPixel, signed, floating, virtual);
		
		RandomAccess<RealType<?>> ra = datasetDisplay.randomAccess();
		
		Cursor<FloatType> cursor = iv.localizingCursor();
    	final long[] pos = new long[iv.numDimensions()];
		while (cursor.hasNext()) {
			cursor.fwd();
			cursor.localize(pos);
			ra.setPosition(pos);
			//ra.setPosition(pos[0], 0);
			//ra.setPosition(pos[1], 1);
			ra.get().setReal(cursor.get().get());
		}  	
		uiService.show(name, datasetDisplay);
	}
	
	/**
	 * Displays a single plot in a separate window.
	 * @param dataX
	 * @param dataY
	 * @param isLineVisible
	 * @param frameTitle
	 * @param plotLabel
	 * @param xAxisLabel
	 * @param yAxisLabel
	 * @param legendLabel
	 * @param regMin
	 * @param regMax
	 * @return
	 */
	private SequencePlotFrame DisplaySinglePlotXY(double[] dataX, double[] dataY, boolean isLineVisible,
			String frameTitle, String plotLabel, String xAxisLabel, String yAxisLabel, String legendLabel) {
		// jFreeChart
		SequencePlotFrame pl = new SequencePlotFrame(dataX, dataY, isLineVisible, frameTitle, plotLabel, xAxisLabel,
				yAxisLabel, legendLabel);
		pl.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		pl.pack();
		// int horizontalPercent = 5;
		// int verticalPercent = 5;
		// RefineryUtilities.positionFrameOnScreen(pl, horizontalPercent,
		// verticalPercent);
		//CommonTools.centerFrameOnScreen(pl);
		pl.setVisible(true);
		return pl;	
	}
	

	/**
	 * 
	 * This methods creates an Img<UnsignedByteType>
	 */
	private Img<UnsignedByteType > createImgUnsignedByte(RandomAccessibleInterval<?> rai){ //rai must always be a single 3D volume
		
		imgUnsignedByte = new ArrayImgFactory<>(new UnsignedByteType()).create(rai.dimension(0), rai.dimension(1), rai.dimension(2)); //always single 3D
		Cursor<UnsignedByteType> cursor = imgUnsignedByte.localizingCursor();
		final long[] pos = new long[imgUnsignedByte.numDimensions()];
		RandomAccess<RealType<?>> ra = (RandomAccess<RealType<?>>) rai.randomAccess();
		while (cursor.hasNext()){
			cursor.fwd();
			cursor.localize(pos);
			ra.setPosition(pos);
			//if (numSlices == 1) { //for only one 2D image;
			//	ra.setPosition(pos[0], 0);
			//	ra.setPosition(pos[1], 1);
			//} else { //for more than one image e.g. image stack
			//	ra.setPosition(pos[0], 0);
			//	ra.setPosition(pos[1], 1);
			//	ra.setPosition(s, 2);
			//}
			//ra.get().setReal(cursor.get().get());
			cursor.get().setReal(ra.get().getRealFloat());
		}
		
		return imgUnsignedByte;
	}

	// This method shows the double log plot
	private void showPlot(double[] lnDataX, double[] lnDataY, String preName, int regMin, int regMax) {
		if (imageType.equals("Grey")) {
			if (lnDataX == null) {
				logService.info(this.getClass().getName() + " lnDataX == null, cannot display the plot!");
				return;
			}
			if (lnDataY == null) {
				logService.info(this.getClass().getName() + " lnDataY == null, cannot display the plot!");
				return;
			}
			if (regMin >= regMax) {
				logService.info(this.getClass().getName() + " regMin >= regMax, cannot display the plot!");
				return;
			}
			if (regMax <= regMin) {
				logService.info(this.getClass().getName() + " regMax <= regMin, cannot display the plot!");
				return;
			}
			// String preName = "";
			if (preName == null) {
				preName = "Volume-";
			} else {
				preName = "Volume-";
			}
//			boolean isLineVisible = false; // ?
//			RegressionPlotFrame doubleLogPlot = DisplayRegressionPlotXY(lnDataX, lnDataY, isLineVisible,
//					"Double Log Plot - 3D Higuchi Dimension", preName + datasetName, "ln(k)", "ln(L)", "", regMin, regMax);
//			doubleLogPlotList.add(doubleLogPlot);
		}
		if (!imageType.equals("Grey")) {

		}
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
			boolean isLineVisible, String frameTitle, String plotLabel, String xAxisLabel, String yAxisLabel,String legendLabel,
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

		// open and display an image
		final File imageFile = ij.ui().chooseFile(null, FileWidget.OPEN_STYLE);
		final Dataset image = ij.scifio().datasetIO().open(imageFile.getAbsolutePath());
		ij.ui().show(image);
		// execute the filter, waiting for the operation to finish.
		// ij.command().run(Csaj3DGeneralizedEntropies.class,
		// true).get().getOutput("image");
		ij.command().run(Csaj3DGeneralizedEntropies.class, true);
	}
}
