/*-
 * #%L
 * Project: ImageJ plugin for computing the Generalized entropies
 * File: Img2DGeneralizedEntropies.java
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

package at.csa.csaj.img2d.en;

import java.awt.Toolkit;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

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
import org.scijava.table.DefaultGenericTable;
import org.scijava.table.DoubleColumn;
import org.scijava.table.GenericColumn;
import org.scijava.table.IntColumn;
import org.scijava.ui.DialogPrompt.MessageType;
import org.scijava.ui.DialogPrompt.OptionType;
import org.scijava.ui.DialogPrompt.Result;
import org.scijava.ui.UIService;
import org.scijava.widget.Button;
import org.scijava.widget.ChoiceWidget;
import org.scijava.widget.FileWidget;
import org.scijava.widget.NumberWidget;

import at.csa.csaj.commons.dialog.WaitingDialogWithProgressBar;
import at.csa.csaj.commons.plot.PlotDisplayFrame;
import at.csa.csaj.commons.plot.RegressionPlotFrame;
import at.csa.csaj.commons.regression.LinearRegression;
import io.scif.DefaultImageMetadata;
import io.scif.MetaTable;

/**
 * A {@link Command} plugin computing
 * <a>the generalized entropies</a>
 * of an image.
 * 
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
 * 
 */
@Plugin(type = InteractiveCommand.class, 
        headless = true,
        label = "Generalized entropies", menu = {
        @Menu(label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT, mnemonic = MenuConstants.PLUGINS_MNEMONIC),
        @Menu(label = "ComsystanJ"),
        @Menu(label = "Image (2D)"),
        @Menu(label = "Generalized entropies", weight = 33)})
public class Img2DGeneralizedEntropies<T extends RealType<T>> extends InteractiveCommand implements Command, Previewable { //non blocking GUI
//public class Img2DGeneralizedEntropies<T extends RealType<T>> implements Command {	//modal GUI
	
	private static final String PLUGIN_LABEL            = "<html><b>Computes Generalized entropies</b></html>";
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
	private static String[] sliceLabels;
	private static long width  = 0;
	private static long height = 0;
	private static long numDimensions = 0;
	private static long numSlices  = 0;
	private static ArrayList<PlotDisplayFrame> genRenyiPlotList = new ArrayList<PlotDisplayFrame>();
	
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
	private static double[][] resultValuesTable; //[# image slice][entropy values]
	private static final String tableName = "Table - Generalized entropies";
	
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
	
	//This parameter does not work in an InteractiveCommand plugin (duplicate displayService error during startup) pom-scijava 24.0.0
	//in Command Plugin no problem
	//@Parameter  
	//private DisplayService displayService;
	
	@Parameter  //This works in an InteractiveCommand plugin
    private DefaultDisplayService defaultDisplayService;
	
	@Parameter
	private DatasetService datasetService;
	
	//Input dataset which is updated in callback functions
	@Parameter (type = ItemIO.INPUT)
	private Dataset datasetIn;

	@Parameter(type = ItemIO.OUTPUT)
	private DefaultGenericTable table;

	
	 //Widget elements------------------------------------------------------
		//-----------------------------------------------------------------------------------------------------
	    //@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
		//private final String labelPlugin = PLUGIN_LABEL;

	    //@Parameter(label = " ", visibility = ItemVisibility.MESSAGE,  persist = false)
	  	//private final String labelSpace = SPACE_LABEL;
	    
		//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelEntropyOptions = ENTROPYOPTIONS_LABEL;
     
 	@Parameter(label = "Probability type",
			description = "Selection of probability type",
			style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
			choices = {"Grey values"}, //, "Pairwise differences", "Sum of differences", "SD"}, 
			//persist  = false,  //restore previous value default = true
			initializer = "initialProbabilityType",
			callback = "callbackProbabilityType")
	private String choiceRadioButt_ProbabilityType;

//	@Parameter(label = "lag", description = "delta for computation", style = NumberWidget.SPINNER_STYLE, min = "1", max = "1000000", stepSize = "1",
//			   persist = false, // restore  previous value  default  =  true
//			   initializer = "initialLag", callback = "callbackLag")
//	private int spinnerInteger_Lag;

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
 		    //persist  = false,  //restore previous value default = true
		        initializer = "initialShowRenyiPlot")
	 private boolean booleanShowRenyiPlot;
     
    @Parameter(label = "Overwrite result display(s)",
   	    	description = "Overwrite already existing result images, plots or tables",
   	    	//persist  = false,  //restore previous value default = true
   			initializer = "initialOverwriteDisplays")
    private boolean booleanOverwriteDisplays;
     
  //-----------------------------------------------------------------------------------------------------
    @Parameter(label = " ", visibility = ItemVisibility.MESSAGE,  persist = false)
    private final String labelProcessOptions = PROCESSOPTIONS_LABEL;
    
    @Parameter(label = "Immediate processing", visibility = ItemVisibility.INVISIBLE, persist = false,
   	    	description = "Immediate processing of active image when a parameter is changed",
   			callback = "callbackProcessImmediately")
   private boolean booleanProcessImmediately;
    
    @Parameter(label   = "Process single active image ",
   		    callback = "callbackProcessActiveImage")
	private Button buttonProcessActiveImage;
    
    @Parameter(label   = "Process all available images",
		        callback = "callbackProcessAllImages")
	private Button buttonProcessAllImages;
    
    //---------------------------------------------------------------------
    //The following initialzer functions set initial values
 
    protected void initialProbabilityType() {
 		choiceRadioButt_ProbabilityType = "Grey values"; //"Grey values", "Pairwise differences", "Sum of differences", "SD"
 	} 
 	
// 	protected void initialLag() {
// 		spinnerInteger_Lag = 1;
// 	}
 	
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
   
    protected void initialShowRenyiPlot() {
    	booleanShowRenyiPlot = true;
    }
   
    protected void initialOverwriteDisplays() {
    	booleanOverwriteDisplays = true;
    }

	// The following method is known as "callback" which gets executed
	// whenever the value of a specific linked parameter changes.

	/** Executed whenever the {@link #choiceRadioButt_ProbabilityType} parameter changes. */
	protected void callbackProbabilityType() {
		logService.info(this.getClass().getName() + " Propability type set to " + choiceRadioButt_ProbabilityType);
	}
	

//	/** Executed whenever the {@link #spinInteger_Lag} parameter changes. */
//	protected void callbackLag() {
//		logService.info(this.getClass().getName() + " Lag set to " + spinnerInteger_Lag);
//	}
	
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
		
	/** Executed whenever the {@link #booleanProcessImmediately} parameter changes. */
	protected void callbackProcessImmediately() {
		logService.info(this.getClass().getName() + " Process immediately set to " + booleanProcessImmediately);
	}
	
	/** Executed whenever the {@link #buttonProcessActiveImage} button is pressed. */
	protected void callbackProcessActiveImage() {
		//prepare  executer service
		exec = Executors.newSingleThreadExecutor();
				
		//dlgProgress = new WaitingDialogWithProgressBar("<html>Computing Generalized entropies, please wait...<br>Open console window for further info.</html>");
		dlgProgress = new WaitingDialogWithProgressBar("Computing Generalized entropies, please wait... Open console window for further info.",
				logService, false, exec); //isCanceable = false, because no following method listens to exec.shutdown 
		dlgProgress.updatePercent("");
		dlgProgress.setBarIndeterminate(true);
		dlgProgress.setVisible(true);

       	exec.execute(new Runnable() {
            public void run() {
        	    try {
        	    	logService.info(this.getClass().getName() + " Processing active image");
            		deleteExistingDisplays();
            		getAndValidateActiveDataset();
            		int activeSliceIndex = getActiveImageIndex();
            		processActiveInputImage(activeSliceIndex);
            		dlgProgress.addMessage("Processing finished! Collecting data for table...");
            		generateTableHeader();
            		collectActiveResultAndShowTable(activeSliceIndex);
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
	
	/** Executed whenever the {@link #buttonProcessAllImages} button is pressed. 
	 *  This is the main processing method usually implemented in the run() method for */
	protected void callbackProcessAllImages() {
		//prepare  executer service
		exec = Executors.newSingleThreadExecutor();
				
		//dlgProgress = new WaitingDialogWithProgressBar("<html>Computing Generalized entropies, please wait...<br>Open console window for further info.</html>");
		dlgProgress = new WaitingDialogWithProgressBar("Computing Generalized entropies, please wait... Open console window for further info.",
																					logService, true, exec); //isCanceable = true, because processAllInputImages(dlgProgress) listens to exec.shutdown 
		dlgProgress.setVisible(true);
		
		exec.execute(new Runnable() {
            public void run() {	
            	try {
	            	logService.info(this.getClass().getName() + " Processing all available images");
	        		deleteExistingDisplays();
	        		getAndValidateActiveDataset();
	        		processAllInputImages();
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
 		if (booleanProcessImmediately) callbackProcessActiveImage();
 		//statusService.showStatus(message);
 	}
 	
    // This is often necessary, for example, if your  "preview" method manipulates data;
 	// the "cancel" method will then need to revert any changes done by the previews back to the original state.
 	public void cancel() {
 		logService.info(this.getClass().getName() + " Widget canceled");
 	}
    //---------------------------------------------------------------------------
	
 	
 	/** The run method executes the command. */
	@Override
	public void run() {
		//Nothing, because non blocking dialog has no automatic OK button and would call this method twice during start up
	
		//ij.log().info( "Run" );
		logService.info(this.getClass().getName() + " Run");

		if(ij.ui().isHeadless()){
			//execute();
			this.callbackProcessAllImages();
		}
	}
	
	public void getAndValidateActiveDataset() {

		datasetIn = imageDisplayService.getActiveDataset();
	
		if ( (datasetIn.firstElement() instanceof UnsignedByteType) ||
	         (datasetIn.firstElement() instanceof FloatType) ){
			//That is OK, proceed
		} else {
					
	    	final MessageType messageType = MessageType.QUESTION_MESSAGE;
			final OptionType optionType = OptionType.OK_CANCEL_OPTION;
			final String title = "Validation result";
			final String message = "Data type not allowed: " + datasetIn.getType().getClass().getSimpleName();
			// Prompt for confirmation.
			//final UIService uiService = getContext().getService(UIService.class);
			Result result = uiService.showDialog(message, title, messageType, optionType);
			
			// Cancel the command execution if the user does not agree.
			//if (result != Result.YES_OPTION) System.exit(-1);
			//if (result != Result.YES_OPTION) return;
			return;
		}
		// get some info
		width = datasetIn.dimension(0);
		height = datasetIn.dimension(1);
		//numSlices = dataset.getDepth(); //does not work if third axis ist not specifyed as z-Axis
		
		numDimensions = datasetIn.numDimensions();
		if (numDimensions == 2) {
			numSlices = 1; // single image
		} else if (numDimensions == 3) { // Image stack
			numSlices =datasetIn.dimension(2);
		}
		
		// get name of dataset
		datasetName = datasetIn.getName();
		
		try {
			Map<String, Object> prop = datasetIn.getProperties();
			DefaultImageMetadata metaData = (DefaultImageMetadata) prop.get("scifio.metadata.image");
			MetaTable metaTable = metaData.getTable();
			sliceLabels = (String[]) metaTable.get("SliceLabels");
			//eliminate additional image info delimited with \n (since pom-scijava 29.2.1)
			for (int i = 0; i < sliceLabels.length; i++) {
				String label = sliceLabels[i];
				int index = label.indexOf("\n");
				//if character has been found, otherwise index = -1
				if (index > 0) sliceLabels[i] = label.substring(0, index);		
			}
		} catch (NullPointerException npe) {
			// TODO Auto-generated catch block
			//npe.printStackTrace();
			logService.info(this.getClass().getName() + " WARNING: It was not possible to read scifio metadata."); 
		}	
		logService.info(this.getClass().getName() + " Name: " + datasetName); 
		logService.info(this.getClass().getName() + " Image size: " + width+"x"+height); 
		logService.info(this.getClass().getName() + " Number of images = "+ numSlices); 
		
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
	
	/** This method deletes already open displays*/
	private void deleteExistingDisplays() {
		boolean optDeleteExistingPlots  = false;
		boolean optDeleteExistingTables = false;
		boolean optDeleteExistingImgs   = false;
		if (booleanOverwriteDisplays) {
			optDeleteExistingPlots  = true;
			optDeleteExistingTables = true;
			optDeleteExistingImgs   = true;
		}
		
		if (optDeleteExistingPlots){
//			//This dose not work with DisplayService because the JFrame is not "registered" as an ImageJ display	
			if (genRenyiPlotList != null) {
				for (int l = 0; l < genRenyiPlotList.size(); l++) {
					genRenyiPlotList.get(l).setVisible(false);
					genRenyiPlotList.get(l).dispose();
					//genDimPlotList.remove(l);  /
				}
				genRenyiPlotList.clear();		
			}
		}
		if (optDeleteExistingTables){
			List<Display<?>> list = defaultDisplayService.getDisplays();
			for (int i = 0; i < list.size(); i++) {
				Display<?> display = list.get(i);
				//System.out.println("display name: " + display.getName());
				if (display.getName().equals(tableName)) display.close();
			}			
		}
	}
	
	/** This method computes the maximal number of possible boxes*/
	private int getMaxBoxNumber(long width, long height) { 
		float boxWidth = 1f;
		int number = 1; 
		while ((boxWidth <= width) && (boxWidth <= height)) {
			boxWidth = boxWidth * 2;
			number = number + 1;
		}
		return number - 1;
	}
	
	/** This method takes the active image and computes results. 
	 *
	 */
	private void processActiveInputImage(int s) throws InterruptedException{
		long startTime = System.currentTimeMillis();
		int numOfEntropies = 1 + 3 + 4*numQ + numEta + numKappa + numB + numBeta + numGamma;
		resultValuesTable = new double[(int) numSlices][numOfEntropies];
		
		//convert to float values
		//Img<T> image = (Img<T>) dataset.getImgPlus();
		//mg<FloatType> imgFloat; // = opService.convert().float32((Img<T>)dataset.getImgPlus());

		RandomAccessibleInterval<?> rai = null;	
		if( (s==0) && (numSlices == 1) && (numDimensions == 2) ) { // for only one 2D image;
			rai =  (RandomAccessibleInterval<?>) datasetIn.getImgPlus();

		} else if ( (numSlices > 1) && (numDimensions == 3) ){ // for a stack of 2D images
			rai = (RandomAccessibleInterval<?>) Views.hyperSlice(datasetIn, 2, s);
		
		}

		//Compute generlaized entropies
		resultValues = process(rai, s);	
		//Entropies H1, H2, H3, .....
			
		//set values for output table
		for (int i = 0; i < resultValues.length; i++ ) {
					resultValuesTable[s][i] = resultValues[i]; 
		}

		long duration = System.currentTimeMillis() - startTime;
		TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
		SimpleDateFormat sdf = new SimpleDateFormat();
		sdf.applyPattern("HHH:mm:ss:SSS");
		logService.info(this.getClass().getName() + " Elapsed time: "+ sdf.format(duration));
	}
	
	/** This method loops over all input images and computes results. 
	 *
	 **/
	private void processAllInputImages() throws InterruptedException{
		
		long startTimeAll = System.currentTimeMillis();
		int numOfEntropies = 1 + 3 + 4*numQ + numEta + numKappa + numB + numBeta + numGamma;
		resultValuesTable = new double[(int) numSlices][numOfEntropies];
	
		//convert to float values
		//Img<T> image = (Img<T>) dataset.getImgPlus();
		//Img<FloatType> imgFloat; // = opService.convert().float32((Img<T>)dataset.getImgPlus());

		
		//loop over all slices of stack
		for (int s = 0; s < numSlices; s++){ //p...planes of an image stack
			if (!exec.isShutdown()){
				int percent = (int)Math.round((  ((float)s)/((float)numSlices)   *100.f   ));
				dlgProgress.updatePercent(String.valueOf(percent+"%"));
				dlgProgress.updateBar(percent);
				//logService.info(this.getClass().getName() + " Progress bar value = " + percent);
				statusService.showStatus((s+1), (int)numSlices, "Processing " + (s+1) + "/" + (int)numSlices);
	//			try {
	//				Thread.sleep(3000);
	//			} catch (InterruptedException e) {
	//				// TODO Auto-generated catch block
	//				e.printStackTrace();
	//			}
				
				long startTime = System.currentTimeMillis();
				logService.info(this.getClass().getName() + " Processing image number " + (s+1) + "(" + numSlices + ")");
				//get slice and convert to float values
				//imgFloat = opService.convert().float32((Img<T>)dataset.gett);	
				
				RandomAccessibleInterval<?> rai = null;	
				if( (s==0) && (numSlices == 1) && (numDimensions == 2) ) { // for only one 2D image;
					rai =  (RandomAccessibleInterval<?>) datasetIn.getImgPlus();
	
				} else if ( (numSlices > 1) && (numDimensions == 3) ){ // for a stack of 2D images
					rai = (RandomAccessibleInterval<?>) Views.hyperSlice(datasetIn, 2, s);
				
				}
				//Compute generlaized entropies
				resultValues = process(rai, s);	
				//Entropies H1, H2, H3, .....
					
				//set values for output table
				for (int i = 0; i < resultValues.length; i++ ) {
							resultValuesTable[s][i] = resultValues[i]; 
				}
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
	private void generateTableHeader(){
		
		GenericColumn columnFileName  = new GenericColumn("File name");
		GenericColumn columnSliceName = new GenericColumn("Slice name");
		GenericColumn columnProbType  = new GenericColumn("Probability type");
		//GenericColumn columnLag       = new GenericColumn("Lag");
				
	
	    table = new DefaultGenericTable();
		table.add(columnFileName);
		table.add(columnSliceName);
		table.add(columnProbType);	
		//table.add(columnLag);	
	
		//"SE", "H1", "H2", "H3", "Renyi", "Tsallis", "SNorm", "SEscort", "SEta", "SKappa", "SB", "SBeta", "SGamma"
		table.add(new DoubleColumn("SE"));
		table.add(new DoubleColumn("H1"));
		table.add(new DoubleColumn("H2"));
		table.add(new DoubleColumn("H3"));
		for (int q = 0; q < numQ; q++) table.add(new DoubleColumn("Renyi_q"   + (minQ + q))); 
		for (int q = 0; q < numQ; q++) table.add(new DoubleColumn("Tsallis_q" + (minQ + q))); 
		for (int q = 0; q < numQ; q++) table.add(new DoubleColumn("SNorm_q"   + (minQ + q))); 
		for (int q = 0; q < numQ; q++) table.add(new DoubleColumn("SEscort_q" + (minQ + q))); 
		for (int e = 0; e < numEta;   e++)  table.add(new DoubleColumn("SEta_e"    + String.format ("%.1f", minEta   + e*stepEta)));
		for (int k = 0; k < numKappa; k++)  table.add(new DoubleColumn("SKappa_k"  + String.format ("%.1f", minKappa + k*stepKappa))); 
		for (int b = 0; b < numB;     b++)  table.add(new DoubleColumn("SB_b"      + String.format ("%.1f", minB     + b*stepB))); 
		for (int be= 0; be< numBeta;  be++) table.add(new DoubleColumn("SBeta_be"  + String.format ("%.1f", minBeta  +be*stepBeta))); 
		for (int g = 0; g < numGamma; g++)  table.add(new DoubleColumn("SGamma_g"  + String.format ("%.1f", minGamma + g*stepGamma))); 
	}
	
	
	
	/** collects current result and shows table
	 *  @param int slice number of active image.
	 */
	private void collectActiveResultAndShowTable(int sliceNumber) {
		
		int tableColStart = 0;
		int tableColEnd   = 0;
		int tableColLast  = 0;
		
	    int s = sliceNumber;		
			//fill table with values
			table.appendRow();
			table.set("File name",   	 table.getRowCount() - 1, datasetName);	
			if (sliceLabels != null) 	 table.set("Slice name", table.getRowCount() - 1, sliceLabels[s]);
			table.set("Probability type", table.getRowCount() - 1, choiceRadioButt_ProbabilityType);    // Lag
			//table.set("Lag", table.getRowCount() - 1, spinnerInteger_Lag);    // Lag
			tableColLast = 2;
			
			int numParameters = resultValuesTable[s].length;
			tableColStart = tableColLast + 1;
			tableColEnd = tableColStart + numParameters;
			for (int c = tableColStart; c < tableColEnd; c++ ) {
				table.set(c, table.getRowCount() - 1, resultValuesTable[s][c-tableColStart]);
			}	
		//Show table
		uiService.show(tableName, table);
	}
	
	/** collects all results and shows table */
	private void collectAllResultsAndShowTable() {
	
		int tableColStart = 0;
		int tableColEnd   = 0;
		int tableColLast  = 0;
		
		//loop over all slices
		for (int s = 0; s < numSlices; s++){ //slices of an image stack
			//fill table with values
			table.appendRow();
			table.set("File name",   	 table.getRowCount() - 1, datasetName);	
			if (sliceLabels != null) 	 table.set("Slice name", table.getRowCount() - 1, sliceLabels[s]);
			table.set("Probability type", table.getRowCount() - 1, choiceRadioButt_ProbabilityType);    // Lag
			//table.set("Lag", table.getRowCount() - 1, spinnerInteger_Lag);    // Lag
			tableColLast = 2;
			
			int numParameters = resultValuesTable[s].length;
			tableColStart = tableColLast + 1;
			tableColEnd = tableColStart + numParameters;
			for (int c = tableColStart; c < tableColEnd; c++ ) {
				table.set(c, table.getRowCount() - 1, resultValuesTable[s][c-tableColStart]);
			}	
		}
		//Show table
		uiService.show(tableName, table);
	}
							
	/** 
	 * Processing ****************************************************************************************
	 * 
	 * */
	private double[] process(RandomAccessibleInterval<?> rai, int plane) { //plane plane (Image) number

		String  probType      = choiceRadioButt_ProbabilityType;
		//int     numLag        = spinnerInteger_Lag;
		boolean optShowRenyiPlot = booleanShowRenyiPlot;
	
		int numBands = 1;
		long width  = rai.dimension(0);
		long height = rai.dimension(1);
		
		String imageType = "8-bit";  //  "RGB"....
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
	
		//Convert image to float
		//Img<T> image = (Img<T>) dataset.getImgPlus();
		//RandomAccessibleInterval<T> rai = (RandomAccessibleInterval<T>)dataset.getImgPlus();
		//IterableInterval ii = dataset.getImgPlus();
		//Img<FloatType> imgFloat = opService.convert().float32(ii);
		
	
		//probabilities = compProbabilities2(rai, probType);	
		probabilities = compProbabilities2(rai, probType); //faster					
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
			String preName = "";
			String axisNameX = "";
			String axisNameY = "";
			if (numSlices > 1) {
				preName = "Slice-"+String.format("%03d", plane) +"-";
			}
			axisNameX = "q";
			axisNameY = "Renyi";
		
			PlotDisplayFrame dimGenPlot = DisplaySinglePlotXY(qList, entList, isLineVisible, "Generalized Renyi entropies", 
					preName + datasetName, axisNameX, axisNameY, "");
			genRenyiPlotList.add(dimGenPlot);
		}		
		
		
		return resultValues;
		//Output
		//uiService.show(tableName, table);
		//result = ops.create().img(image, new FloatType());
		//table
	}

	
	/**
	 * This computes probabilities of actual values
	 * 
	 * @param signal
	 * @param probOption
	 * @return probabilities[]
	 */
	//"Grey values" (, "Pairwise differences", "Sum of differences", "SD")
	private double[] compProbabilities(RandomAccessibleInterval<?> rai, String probType) {
		double imageMin = Double.MAX_VALUE;
		double imageMax = -Double.MAX_VALUE;
		double imageDouble[] = new double[(int) (rai.dimension(0)*rai.dimension(1))]; 
		int i = 0;
		if (probType.equals("Grey values")) {//Actual values
			cursor = Views.iterable(rai).cursor();
			while (cursor.hasNext()) {
				cursor.fwd();
				imageDouble[i] = (double)((UnsignedByteType) cursor.get()).getInteger();
				i++;
			}
		}
		if (probType.equals("Pairwise differences")) {//Pairwise differences
		}
		if (probType.equals("Sum of differences")) {//Sum of differences in between lag
		}
		if (probType.equals("SD")) {//SD in between lag
		}
	
		//Apache
		int binNumber = 255;
		int binSize = (int) ((imageMax - imageMin)/binNumber);
		long[] histogram = new long[binNumber];
		EmpiricalDistribution distribution = new EmpiricalDistribution(binNumber);
		distribution.load(imageDouble);
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
	 * This computes probabilities of actual values
	 * 
	 * @param signal
	 * @param probOption
	 * @return probabilities[]
	 */
	//"Grey values" (, "Pairwise differences", "Sum of differences", "SD")
	private double[] compProbabilities2(RandomAccessibleInterval<?> rai, String probType) { //shorter computation
		int binNumber = 256;
		double[] pis = new double[binNumber]; 
		double imageMin = Double.MAX_VALUE;
		double imageMax = -Double.MAX_VALUE;
		int sample;
		double totalsMax = 0.0;
	
		if (probType.equals("Grey values")) {//Actual values
			imgUnsignedByte = this.createImgUnsignedByte(rai);
			cursor = imgUnsignedByte.cursor();
			while (cursor.hasNext()) {
				cursor.fwd();
				sample = ((UnsignedByteType) cursor.get()).getInteger();
				pis[sample]++;
				totalsMax++;
			}
		}
		if (probType.equals("Pairwise differences")) {//Pairwise differences
		}
		if (probType.equals("Sum of differences")) {//Sum of differences in between lag
		}
		if (probType.equals("SD")) {//SD in between lag
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
	 * According to Amigo etal. and Anteneodo, C.; Plastino, A.R. Maximum entropy approach to stretched exponential probability distributions. J. Phys. A Math. Gen. 1999, 32, 1089–1098.
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
	 * Displays a multiple regression plot in a separate window.
	 * <p>
	 *		
	 *
	 * </p>
	 * 
	 * @param dataX data values for x-axis.
	 * @param dataY data values for y-axis.
	 * @param isLineVisible option if regression line is visible
	 * @param frameTitle title of frame
	 * @param plotLabel  label of plot
	 * @param xAxisLabel label of x-axis
	 * @param yAxisLabel label of y-axis
	 * @param regMin minimum value for regression range
	 * @param regMax maximal value for regression range 
	 * @param optDeleteExistingPlot option if existing plot should be deleted before showing a new plot
	 * @param interpolType The type of interpolation
	 * @return RegressionPlotFrame
	 */			
	private RegressionPlotFrame DisplayMultipleRegressionPlotXY(double[] dataX, double[][] dataY, boolean isLineVisible,
			String frameTitle, String plotLabel, String xAxisLabel, String yAxisLabel, String[] legendLabels, int regMin, int regMax) {
		// jFreeChart
		RegressionPlotFrame pl = new RegressionPlotFrame(dataX, dataY, isLineVisible, frameTitle, plotLabel, xAxisLabel,
				yAxisLabel, legendLabels, regMin, regMax);
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
	private PlotDisplayFrame DisplaySinglePlotXY(double[] dataX, double[] dataY, boolean isLineVisible,
			String frameTitle, String plotLabel, String xAxisLabel, String yAxisLabel, String legendLabel) {
		// jFreeChart
		PlotDisplayFrame pl = new PlotDisplayFrame(dataX, dataY, isLineVisible, frameTitle, plotLabel, xAxisLabel,
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
	private Img<UnsignedByteType > createImgUnsignedByte(RandomAccessibleInterval<?> rai){ //rai must always be a single 2D plane
		
		imgUnsignedByte = new ArrayImgFactory<>(new UnsignedByteType()).create(width, height); //always single 2D
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
	
	/**
	 * 
	 * This methods creates a Img<FloatType>
	 */
	private Img<FloatType > createImgFloat(RandomAccessibleInterval<?> rai){ //rai must always be a single 2D plane
		
		imgFloat = new ArrayImgFactory<>(new FloatType()).create(width, height); //always single 2D
		Cursor<FloatType> cursor = imgFloat.localizingCursor();
		final long[] pos = new long[imgFloat.numDimensions()];
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
		return imgFloat;
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
		//ij.command().run(Img2DGeneralizedEntropies.class, true).get().getOutput("image");
		ij.command().run(Img2DGeneralizedEntropies.class, true);
	}
}

