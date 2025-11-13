/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj3DStatCplxMeasCmd.java
 * 
 * $Id$
 * $HeadURL$
 * 
 * This file is part of ComsystanJ software, hereinafter referred to as "this program".
 * %%
 * Copyright (C) 2024 - 2025 Comsystan Software
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


package at.csa.csaj.plugin3d.cplx;

import java.awt.Frame;
import java.awt.Toolkit;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import org.scijava.ItemIO;
import org.scijava.ItemVisibility;
import org.scijava.app.StatusService;
import org.scijava.command.ContextCommand;
import org.scijava.command.Previewable;
import org.scijava.display.DefaultDisplayService;
import org.scijava.display.Display;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;
import org.scijava.table.BoolColumn;
import org.scijava.table.DefaultGenericTable;
import org.scijava.table.DoubleColumn;
import org.scijava.table.GenericColumn;
import org.scijava.ui.UIService;
import org.scijava.widget.Button;
import org.scijava.widget.ChoiceWidget;
import org.scijava.widget.FileWidget;
import org.scijava.widget.NumberWidget;

import at.csa.csaj.commons.CsajAlgorithm_ProbabilityDistance;
import at.csa.csaj.commons.CsajAlgorithm_ShannonEntropy;
import at.csa.csaj.commons.CsajCheck_ItemIn;
import at.csa.csaj.commons.CsajDialog_WaitingWithProgressBar;
import at.csa.csaj.commons.CsajPlot_RegressionFrame;
import at.csa.csaj.commons.CsajPlot_SequenceFrame;
import at.csa.csaj.commons.CsajContainer_ProcessMethod;

/**
 * A {@link ContextCommand} plugin computing <3D Generalised entropies</a>
 * of an image volume.
 * 
 * * A {@link ContextCommand} plugin computing <Generalised entropies</a>
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
 */
@Plugin(type = ContextCommand.class,
		headless = true,
		label = "3D Statistical complexity measures",
		initializer = "initialPluginLaunch",
		iconPath = "/icons/comsystan-logo-grey46-16x16.png", //Menu entry icon
		menu = {})

public class Csaj3DStatCplxMeasCmd<T extends RealType<T>> extends ContextCommand implements Previewable {

	private static final String PLUGIN_LABEL            = "<html><b>Computes 3D Statistical complexity measures</b></html>";
	private static final String SPACE_LABEL             = "";
	private static final String SCMOPTIONS_LABEL        = "<html><b>SCM options</b></html>";
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
	private static ArrayList<CsajPlot_SequenceFrame> genRenyiPlotList = new ArrayList<CsajPlot_SequenceFrame>();
	
	public static final String TABLE_OUT_NAME = "Table - 3D Statistical complexity measures";
	
	
	// data arrays		
	private static double scm_e;
	private static double scm_w;
	private static double scm_k;
	private static double scm_j;
	private static double shannonH;
	private static double d_e;
	private static double d_w;
	private static double d_k;
	private static double d_j;
	
	double[] probabilities         = null; //pi's
	double[] probabilitiesSurrMean = null; //pi's
	
	//double[] resultValues; //Entropy values
	
	private CsajDialog_WaitingWithProgressBar dlgProgress;
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

	@Parameter(label = TABLE_OUT_NAME, type = ItemIO.OUTPUT)
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
	private final String labelEntropyOptions = SCMOPTIONS_LABEL;
     
 	@Parameter(label = "Probability type",
			description = "Selection of probability type",
			style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
			choices = {"Grey values", "Pairwise differences"}, // "Sum of differences", "SD"}, 
			persist = true,  //restore previous value default = true
			initializer = "initialProbabilityType",
			callback = "callbackProbabilityType")
	private String choiceRadioButt_ProbabilityType;

	@Parameter(label = "Lag",
			   description = "Delta (difference) between two data points",
			   style = NumberWidget.SPINNER_STYLE,
			   min = "1",
			   max = "1000000",
			   stepSize = "1",
			   persist = false, // restore  previous value  default  =  true
			   initializer = "initialLag",
			   callback = "callbackLag")
	private int spinnerInteger_Lag;

	@Parameter(label = "Normalise H",
			   description = "Normalisation of Shannon entropy H - recommended",
		       persist = true,  //restore previous value default = true
		       initializer = "initialNormaliseH")
	 private boolean booleanNormaliseH;
	
	@Parameter(label = "Normalise D",
		       description = "Normalisation of statistical distribution distance D - recommended",
		       persist = true,  //restore previous value default = true
		       initializer = "initialNormaliseD")
	 private boolean booleanNormaliseD;
	
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
	private final String labelProcessOptions = PROCESSOPTIONS_LABEL;

	@Parameter(label = "Immediate processing", visibility = ItemVisibility.INVISIBLE, persist = false,
	    	description = "Immediate processing of active image when a parameter is changed",
			callback = "callbackProcessImmediately")
	private boolean booleanProcessImmediately;
	
//	@Parameter(label = "OK - process image #", description = "Image slice number", style = NumberWidget.SPINNER_STYLE, min = "1", max = "99999999", stepSize = "1",
//			   persist = false, // restore  previous value  default  =  true
//			   initializer = "initialNumImageSlice",
//			   callback = "callbackNumImageSlice")
//	private int spinnerInteger_NumImageSlice;
	
	@Parameter(label = "    OK - process single volume     ", callback = "callbackProcessSingleVolume")
	private Button buttonProcessSingleVolume;
	
//	Deactivated, because it does not work in Fiji (although it works in ImageJ2 -Eclipse)	
//	@Parameter(label = "Preview of single active image ", callback = "callbackProcessActiveImage")
//	private Button buttonProcessActiveImage;

//	@Parameter(label = "Preview of all available images", callback = "callbackProcessAllImages")
//	private Button buttonProcessAllImages;
	
	// ---------------------------------------------------------------------	
	protected void initialPluginLaunch() {
		checkItemIOIn();
	}
	
	protected void initialProbabilityType() {
	 	choiceRadioButt_ProbabilityType = "Grey values"; //"Grey values", "Pairwise differences", "Sum of differences", "SD"
	} 
	 	
	protected void initialLag() {
	 	spinnerInteger_Lag = 1;
	}
	
	protected void initialNormaliseH() {
		booleanNormaliseH = false;
	}
	
	protected void initialNormaliseD() {
		booleanNormaliseD = false;
	}
	
	protected void initialSkipZeroes() {
		booleanSkipZeroes = false;
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
	
	/** Executed whenever the {@link #booleanSkipZeroes} parameter changes. */
	protected void callbackSkipZeroes() {
		logService.info(this.getClass().getName() + " Skip zeroes set to " + booleanSkipZeroes);
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
	    	   	uiService.show(TABLE_OUT_NAME, tableOut);
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
		    	    startWorkflowForSingleVolume();
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
		logService.info(this.getClass().getName() + " Starting command run");
		
		//Set field variables

		checkItemIOIn();
		startWorkflowForSingleVolume();
	
		logService.info(this.getClass().getName() + " Finished command run");
	}

	public void checkItemIOIn() {

		//Define supported image types for this plugin
		String[] supportedImageTypes = {"Grey"};
		//String[] supportedImageTypes = {"RGB"};
		//String[] supportedImageTypes = {"Grey", "RGB"};
		
		//Check input and get input meta data
		HashMap<String, Object> datasetInInfo = CsajCheck_ItemIn.checkVolumeDatasetIn(logService, datasetIn, supportedImageTypes);
		if (datasetInInfo == null) {
			logService.error(MethodHandles.lookup().lookupClass().getName() + " ERROR: Inital check failed");
			cancel("ComsystanJ 3D plugin cannot be started - Initial check failed.");
		} else {
			width  =       			(long)datasetInInfo.get("width");
			height =       			(long)datasetInInfo.get("height");
			depth  =       			(long)datasetInInfo.get("depth");
			numDimensions =         (int)datasetInInfo.get("numDimensions");
			compositeChannelCount = (int)datasetInInfo.get("compositeChannelCount");
			numSlices =             (long)datasetInInfo.get("numSlices");
			imageType =   			(String)datasetInInfo.get("imageType");
			datasetName = 			(String)datasetInInfo.get("datasetName");
			//sliceLabels = 		(String[])datasetInInfo.get("sliceLabels");
		}
	}

	/**
	* This method starts the workflow for a single image of the active display
	*/
	protected void startWorkflowForSingleVolume() {
	
		dlgProgress = new CsajDialog_WaitingWithProgressBar("Computing 3D Generalised entropies, please wait... Open console window for further info.",
							logService, false, exec); //isCanceable = false, because no following method listens to exec.shutdown 
		dlgProgress.updatePercent("");
		dlgProgress.setBarIndeterminate(true);
		dlgProgress.setVisible(true);
		
    	deleteExistingDisplays();
    	generateTableHeader();
        logService.info(this.getClass().getName() + " Processing volume...");
		processSingleInputVolume();
		
		dlgProgress.addMessage("Processing finished! Collecting data for table...");
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
				if (display.getName().contains(TABLE_OUT_NAME)) display.close();
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
		//int numOfMeasures = 9;
	
		//get rai
		RandomAccessibleInterval<T> rai = null;	
	
		rai =  (RandomAccessibleInterval<T>) datasetIn.getImgPlus(); //dim==3

		//Compute generalised entropies
		CsajContainer_ProcessMethod containerPM = process(rai);	
		//Gen entropies SE H1, H2, H3, .....
					
		writeToTable(containerPM);
		
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
		BoolColumn columnNormH        = new BoolColumn("Normalised H");
		BoolColumn columnNormD        = new BoolColumn("Normalised D");		
		BoolColumn columnSkipZeroes   = new BoolColumn("Skip zeroes");		

	    tableOut = new DefaultGenericTable();
		tableOut.add(columnFileName);
		tableOut.add(columnProbType);	
		tableOut.add(columnLag);
		tableOut.add(columnNormH);
		tableOut.add(columnNormD);
		tableOut.add(columnSkipZeroes);
	
		//"SCM_E", "SCM_W", "SCM_K", "SCM_J"
		tableOut.add(new DoubleColumn("SCM_E"));
		tableOut.add(new DoubleColumn("SCM_W"));
		tableOut.add(new DoubleColumn("SCM_K"));
		tableOut.add(new DoubleColumn("SCM_J"));
		tableOut.add(new DoubleColumn("H"));
		tableOut.add(new DoubleColumn("D_E"));
		tableOut.add(new DoubleColumn("D_W"));
		tableOut.add(new DoubleColumn("D_K"));
		tableOut.add(new DoubleColumn("D_J"));
	}

	/**
	 * collects current result and writes to table
	 * 
	 * @param CsajContainer_ProcessMethod containerPM
	 */
	private void writeToTable(CsajContainer_ProcessMethod containerPM) { 

		int tableColStart = 0;
		int tableColEnd   = 0;
		int tableColLast  = 0;
		
		//fill table with values
		tableOut.appendRow();
		tableOut.set("File name",   	 tableOut.getRowCount() - 1, datasetName);	
		tableOut.set("Probability type", tableOut.getRowCount() - 1, choiceRadioButt_ProbabilityType); 
		tableOut.set("Lag",              tableOut.getRowCount() - 1, spinnerInteger_Lag);    // Lag
		tableOut.set("Normalised H",     tableOut.getRowCount() - 1, booleanNormaliseH);    
		tableOut.set("Normalised D",     tableOut.getRowCount() - 1, booleanNormaliseD);
		tableOut.set("Skip zeroes",      tableOut.getRowCount() - 1, booleanSkipZeroes); 		
		tableColLast = 5;
			
		int numParameters = containerPM.item1_Values.length;
		tableColStart = tableColLast + 1;
		tableColEnd = tableColStart + numParameters;
		for (int c = tableColStart; c < tableColEnd; c++ ) {
			tableOut.set(c, tableOut.getRowCount() - 1, containerPM.item1_Values[c-tableColStart]);
		}	
	}

	/**
	*
	* Processing
	*/
	private CsajContainer_ProcessMethod process(RandomAccessibleInterval<?> rai) { //3Dvolume
	
		if (rai == null) {
			logService.info(this.getClass().getName() + " WARNING: rai==null, no image for processing!");
		}
		
		String  probType    = choiceRadioButt_ProbabilityType;
		int     lag         = spinnerInteger_Lag;
		boolean normaliseH  = booleanNormaliseH;
		boolean normaliseD  = booleanNormaliseD;
		boolean skipZeros   = booleanSkipZeroes;
		boolean skipZeroBin = booleanSkipZeroes;

//		if ((!colorModelType.equals("Binary")) && (numRegStart == 1)){
//			numRegStart = 2; //numRegStart == 1 (single pixel box is not possible for DBC algorithms)
//		}

		long width  = rai.dimension(0);
		long height = rai.dimension(1);
		long depth  = rai.dimension(2);
		
		// data values		
		scm_e = 0.0;
		scm_w = 0.0;
		scm_k = 0.0;
		scm_j = 0.0;
		shannonH = 0.0;
		d_e = 0.0;
		d_w = 0.0;
		d_k = 0.0;
		d_j = 0.0;
		
		int numOfMeasures = 9;
		
		double[] resultValues = new double[numOfMeasures]; // 
		for (int r = 0; r < resultValues.length; r++) resultValues[r] = Float.NaN;
		
		//probabilities = compProbabilities(rai, lag, probType); //3D rai	grey
		probabilities = compProbabilities2(rai, lag, probType); //faster  //3D grey	
		CsajAlgorithm_ShannonEntropy se = new CsajAlgorithm_ShannonEntropy(probabilities);
		CsajAlgorithm_ProbabilityDistance pd = new CsajAlgorithm_ProbabilityDistance(probabilities);
		
		if (normaliseH) shannonH = se.compNormalisedH(skipZeroBin);
		else            shannonH = se.compH(skipZeroBin);
		
		if (normaliseD) {
			d_e = pd.compNormalisedD_E(skipZeroBin);
			d_w = pd.compNormalisedD_W(skipZeroBin);
			d_k = pd.compNormalisedD_K(skipZeroBin);
			d_j = pd.compNormalisedD_J(skipZeroBin);
		}
		else {
			d_e = pd.compD_E(skipZeroBin);
			d_w = pd.compD_W(skipZeroBin);
			d_k = pd.compD_K(skipZeroBin);
			d_j = pd.compD_J(skipZeroBin);
		}
				
		scm_e = shannonH*d_e;
		scm_w = shannonH*d_w;
		scm_k = shannonH*d_k;
		scm_j = shannonH*d_j;
		
		resultValues[0] = scm_e;
		resultValues[1] = scm_w;
		resultValues[2] = scm_k;
		resultValues[3] = scm_j;
		resultValues[4] = shannonH;	
		resultValues[5] = d_e;
		resultValues[6] = d_w;
		resultValues[7] = d_k;
		resultValues[8] = d_j;		
						
		logService.info(this.getClass().getName() + " SCM_E: " + resultValues[0]);
		
		return new CsajContainer_ProcessMethod(resultValues);
		//Output
		//uiService.show(TABLE_OUT_NAME, table);
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
	 * @param numRegStart
	 * @param numRegEnd
	 * @return
	 */
	private CsajPlot_SequenceFrame DisplaySinglePlotXY(double[] dataX, double[] dataY, boolean isLineVisible,
			String frameTitle, String plotLabel, String xAxisLabel, String yAxisLabel, String legendLabel) {
		// jFreeChart
		CsajPlot_SequenceFrame pl = new CsajPlot_SequenceFrame(dataX, dataY, isLineVisible, frameTitle, plotLabel, xAxisLabel,
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
	private void showPlot(double[] lnDataX, double[] lnDataY, String preName, int numRegStart, int numRegEnd) {
		if (imageType.equals("Grey")) {
			if (lnDataX == null) {
				logService.info(this.getClass().getName() + " lnDataX == null, cannot display the plot!");
				return;
			}
			if (lnDataY == null) {
				logService.info(this.getClass().getName() + " lnDataY == null, cannot display the plot!");
				return;
			}
			if (numRegStart >= numRegEnd) {
				logService.info(this.getClass().getName() + " numRegStart >= numRegEnd, cannot display the plot!");
				return;
			}
			if (numRegEnd <= numRegStart) {
				logService.info(this.getClass().getName() + " numRegEnd <= numRegStart, cannot display the plot!");
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
//					"Double log plot - 3D Generalised entropies", preName + datasetName, "ln(k)", "ln(L)", "", numRegStart, numRegEnd);
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
	 * @param numRegStart                minimum value for regression range
	 * @param numRegEnd                maximal value for regression range
	 * @param optDeleteExistingPlot option if existing plot should be deleted before
	 *                              showing a new plot
	 * @param interpolType          The type of interpolation
	 * @return RegressionPlotFrame
	 */
	private CsajPlot_RegressionFrame DisplayRegressionPlotXY(double[] dataX, double[] dataY,
			boolean isLineVisible, String frameTitle, String plotLabel, String xAxisLabel, String yAxisLabel,String legendLabel,
			int numRegStart, int numRegEnd) {
		// jFreeChart
		CsajPlot_RegressionFrame pl = new CsajPlot_RegressionFrame(dataX, dataY, isLineVisible, frameTitle, plotLabel, xAxisLabel,
				yAxisLabel, legendLabel, numRegStart, numRegEnd);
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
		// ij.command().run(MethodHandles.lookup().lookupClass().getName(), true).get().getOutput("image");
		ij.command().run(MethodHandles.lookup().lookupClass().getName(), true);
	}
}
