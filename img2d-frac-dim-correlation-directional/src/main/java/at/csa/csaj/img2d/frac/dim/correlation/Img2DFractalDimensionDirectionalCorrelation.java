/*-
 * #%L
 * Project: ImageJ plugin for computing direction dependent correlation dimension.
 * File: Img2DFractalDimensionDirectionalCorrelation.java
 * 
 * $Id$
 * $HeadURL$
 * 
 * This file is part of ComsystanJ software, hereinafter referred to as "this program".
 * %%
 * Copyright (C) 2020 - 2021 Comsystan Software
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

package at.csa.csaj.img2d.frac.dim.correlation;

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
import net.imagej.display.ImageDisplayService;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
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
import at.csa.csaj.commons.plot.RegressionPlotFrame;
import at.csa.csaj.commons.regression.LinearRegression;
import at.csa.csaj.commons.dialog.WaitingDialogWithProgressBar;
import ij.gui.PlotWindow;
import io.scif.DefaultImageMetadata;
import io.scif.MetaTable;

/**
 * A {@link Command} plugin computing <Directional correlation dimension</a>
 * of an image.
 */
@Plugin(type = InteractiveCommand.class,
		headless = true,
		label = "Directional correlation dimension", menu = {
        @Menu(label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT, mnemonic = MenuConstants.PLUGINS_MNEMONIC),
        @Menu(label = "ComsystanJ"),
        @Menu(label = "Image (2D)"),
        @Menu(label = "Directional correlation dimension", weight = 7)})
public class Img2DFractalDimensionDirectionalCorrelation<T extends RealType<T>> extends InteractiveCommand implements Command, Previewable { // non blocking  GUI
//public class Img2DFractalDimensionDirectionalCorrelation<T extends RealType<T>> implements Command {	//modal GUI

	private static final String PLUGIN_LABEL            = "<html><b>Computes fractal dimension with directinal correlation</b></html>";
	private static final String SPACE_LABEL             = "";
	private static final String REGRESSION_LABEL        = "<html><b>Regression parameters</b></html>";
	private static final String METHODOPTIONS_LABEL     = "<html><b>Direction</b></html>";
	private static final String BACKGROUNDOPTIONS_LABEL = "<html><b>Background option</b></html>";
	private static final String DISPLAYOPTIONS_LABEL    = "<html><b>Display options</b></html>";
	private static final String PROCESSOPTIONS_LABEL    = "<html><b>Process options</b></html>";

	private static Img<FloatType> imgFloat;
	LinearInterpolator interpolator;
	PolynomialSplineFunction psf;
	private static String datasetName;
	private static String[] sliceLabels;
	private static long width = 0;
	private static long height = 0;
	private static long numDimensions = 0;
	private static long numSlices = 0;
	private static int  numBoxes = 0;
	private static double[] anglesGrad;
	private static ArrayList<RegressionPlotFrame> doubleLogPlotList = new ArrayList<RegressionPlotFrame>();
	private static ArrayList<PlotWindow>          plotWindowList    = new ArrayList<PlotWindow>(); //ImageJ plot windows

	private static double[][] resultValuesTable; // first column is the image index, second column are the corresponding regression values
	private static final String tableName = "Table - Directional correlation dimension";
	
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

	@Parameter(type = ItemIO.OUTPUT)
	private DefaultGenericTable table;


	// Widget elements------------------------------------------------------

	//-----------------------------------------------------------------------------------------------------
	//@Parameter(label = " ",visibility = ItemVisibility.MESSAGE, persist = false)
	//private final String labelPlugin = PLUGIN_LABEL;

	//@Parameter(label = " ",visibility = ItemVisibility.MESSAGE, persist = false)
	//private final String labelSpace = SPACE_LABEL;
	
	// Input dataset which is updated in callback functions
	@Parameter(type = ItemIO.INPUT)
	private Dataset datasetIn;

	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelRegression = REGRESSION_LABEL;

	@Parameter(label = "Number of distances",
    		   description = "Number of distinct distances following the power of 2",
	       	   style = NumberWidget.SPINNER_STYLE,
	           min = "1",
	           max = "32768",
	           stepSize = "1",
	           persist  = false,  //restore previous value default = true
	           initializer = "initialNumBoxes",
	           callback    = "callbackNumBoxes")
    private int spinnerInteger_NumBoxes;

	@Parameter(label = "Regression Min", description = "Minimum x value of linear regression", style = NumberWidget.SPINNER_STYLE, min = "1", max = "32768", stepSize = "1",
			   persist = false, //restore previous value default = true
			   initializer = "initialRegMin", callback = "callbackRegMin")
	private int spinnerInteger_RegMin = 1;

	@Parameter(label = "Regression Max", description = "Maximum x value of linear regression", style = NumberWidget.SPINNER_STYLE, min = "3", max = "32768", stepSize = "1",
			   persist = false, //restore previous value default = true
			   initializer = "initialRegMax", callback = "callbackRegMax")
	private int spinnerInteger_RegMax = 3;

	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelMethod = METHODOPTIONS_LABEL;

	@Parameter(label = "Direction", description = "Direction counting pair-wise correlations", style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE, choices = {
		       "Horizontal and vertical direction", "Mean of     4 radial directions [0-180°]", "Mean of 180 radial directions [0-180°]" },
			   persist = false, //restore previous value default = true
			   initializer = "initialDirection", callback = "callbackDirection")
	private String choiceRadioButt_Direction;

    @Parameter(label = "Analysis type",
  		    description = "Type of image and computation",
  		    style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
    		    choices = {"Binary", "Grey"},
    		    //persist  = false,  //restore previous value default = true
  		    initializer = "initialAnalysisType",
              callback = "callbackAnalysisType")
    private String choiceRadioButt_AnalysisType;
	
	@Parameter(label = "Pixel %",
	  		   description = "% of image pixels to be taken - to lower computation times",
		       style = NumberWidget.SPINNER_STYLE,
		       min = "1",
		       max = "100",
		       stepSize = "1",
		       //persist  = false,  //restore previous value default = true
		       initializer = "initialPixelPercentage",
		       callback    = "callbackPixelPercentage")
	private int spinnerInteger_PixelPercentage;
	
	//-----------------------------------------------------------------------------------------------------
	//@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	//private final String labelBackgroundOptions = BACKGROUNDOPTIONS_LABEL;

	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelDisplayOptions = DISPLAYOPTIONS_LABEL;

	@Parameter(label = "Show double log plot",
		   	   // persist = false, //restore previous value default = true
			   initializer = "initialShowDoubleLogPlots")
	private boolean booleanShowDoubleLogPlot;

	@Parameter(label = "Get Dc values of all radial directions",
			   // persist = false, //restore previous value default = true
			   initializer = "initialGetAllRadialDsValues")
	private boolean booleanGetAllRadialDsValues;

	@Parameter(label = "Overwrite result display(s)",
	    	description = "Overwrite already existing result images, plots or tables",
	    	//persist  = false,  //restore previous value default = true
			initializer = "initialOverwriteDisplays")
	private boolean booleanOverwriteDisplays;
	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelProcessOptions = PROCESSOPTIONS_LABEL;

	@Parameter(label = "Immediate processing", visibility = ItemVisibility.INVISIBLE, persist = false,
	    	description = "Immediate processing of active image when a parameter is changed",
			callback = "callbackProcessImmediately")
	private boolean booleanProcessImmediately;
	
	@Parameter(label = "Process single active image ", callback = "callbackProcessActiveImage")
	private Button buttonProcessActiveImage;

	@Parameter(label = "Process all available images", callback = "callbackProcessAllImages")
	private Button buttonProcessAllImages;


	// ---------------------------------------------------------------------
	// The following initialzer functions set initial values

	protected void initialNumBoxes() {
		numBoxes = getMaxBoxNumber(datasetIn.dimension(0), datasetIn.dimension(1));
	    spinnerInteger_NumBoxes = numBoxes;
	}

	protected void initialRegMin() {
		spinnerInteger_RegMin = 1;
	}

	protected void initialRegMax() {
		//spinnerInteger_RegMax = (int) Math.floor((Math.min(datasetIn.max(0) + 1, datasetIn.max(1) + 1)) / 3.0);
	  	numBoxes = getMaxBoxNumber(datasetIn.dimension(0), datasetIn.dimension(1));
		spinnerInteger_RegMax = numBoxes;
	}

	protected void initialDirection() {
		choiceRadioButt_Direction = "Horizontal and vertical direction";
	}

	protected void initialAnalysisType() {
	    choiceRadioButt_AnalysisType = "Binary";
	}
	
	protected void initialPixelPercentage() {
	    spinnerInteger_PixelPercentage = 100;
	}
	
	protected void initialShowDoubleLogPlots() {
		booleanShowDoubleLogPlot = true;
	}
	
	protected void initialOverwriteDisplays() {
    	booleanOverwriteDisplays = true;
	}
	
	protected void initialGetAllRadialDsValues() {
		booleanGetAllRadialDsValues = false;
	}

	// The following method is known as "callback" which gets executed
	// whenever the value of a specific linked parameter changes.

	/** Executed whenever the {@link #spinInteger_NumBoxes} parameter changes. */
	protected void callbackNumBoxes() {
		
		if  (spinnerInteger_NumBoxes < 3) {
			spinnerInteger_NumBoxes = 3;
		}
		int numMaxBoxes = getMaxBoxNumber(datasetIn.dimension(0), datasetIn.dimension(1));	
		if (spinnerInteger_NumBoxes > numMaxBoxes) {
			spinnerInteger_NumBoxes = numMaxBoxes;
		};
		if (spinnerInteger_RegMax > spinnerInteger_NumBoxes) {
			spinnerInteger_RegMax = spinnerInteger_NumBoxes;
		}
		if (spinnerInteger_RegMin >= spinnerInteger_RegMax - 2) {
			spinnerInteger_RegMin = spinnerInteger_RegMax - 2;
		}

		numBoxes = spinnerInteger_NumBoxes;
		logService.info(this.getClass().getName() + " Number of boxes set to " + spinnerInteger_NumBoxes);
	}

	/** Executed whenever the {@link #spinInteger_RegMin} parameter changes. */
	protected void callbackRegMin() {
		if (spinnerInteger_RegMin >= spinnerInteger_RegMax - 2) {
			spinnerInteger_RegMin = spinnerInteger_RegMax - 2;
		}
		if (spinnerInteger_RegMin < 1) {
			spinnerInteger_RegMin = 1;
		}
		logService.info(this.getClass().getName() + " Regression Min set to " + spinnerInteger_RegMin);
	}

	/** Executed whenever the {@link #spinInteger_RegMax} parameter changes. */
	protected void callbackRegMax() {
		if (spinnerInteger_RegMax <= spinnerInteger_RegMin + 2) {
			spinnerInteger_RegMax = spinnerInteger_RegMin + 2;
		}
		if (spinnerInteger_RegMax > spinnerInteger_NumBoxes) {
			spinnerInteger_RegMax = spinnerInteger_NumBoxes;
		}

		logService.info(this.getClass().getName() + " Regression Max set  to " + spinnerInteger_RegMax);
	}

	/** Executed whenever the {@link #choiceRadioButt_Direction} parameter changes. */
	protected void callbackDirection() {
		logService.info(this.getClass().getName() + " Direction set to " + choiceRadioButt_Direction);
	}
	
	/** Executed whenever the {@link #choiceRadioButt_AnalysisType} parameter changes. */
	protected void callbackAnalysisType() {
		logService.info(this.getClass().getName() + " Analysis method set to " + choiceRadioButt_AnalysisType);
	}
	
	/** Executed whenever the {@link #spinInteger_PixelPercentage} parameter changes. */
	protected void callbackPixelPercentage() {
		logService.info(this.getClass().getName() + " Pixel % set to " + spinnerInteger_PixelPercentage);
	}
	
	/** Executed whenever the {@link #booleanProcessImmediately} parameter changes. */
	protected void callbackProcessImmediately() {
		logService.info(this.getClass().getName() + " Process immediately set to " + booleanProcessImmediately);
	}
	
	/**
	 * Executed whenever the {@link #buttonProcessActiveImage} button is pressed.
	 */
	protected void callbackProcessActiveImage() {
		//prepare  executer service
		exec = Executors.newSingleThreadExecutor();
		
		//dlgProgress = new WaitingDialogWithProgressBar("<html>Computing Directional correlation dimensions, please wait...<br>Open console window for further info.</html>");
		dlgProgress = new WaitingDialogWithProgressBar("Computing Directional correlation dimensions, please wait... Open console window for further info.",
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

	/**
	 * Executed whenever the {@link #buttonProcessAllImages} button is pressed. This
	 * is the main processing method usually implemented in the run() method for
	 */
	protected void callbackProcessAllImages() {
		//prepare  executer service
		exec = Executors.newSingleThreadExecutor();
		//exec =  defaultThreadService.getExecutorService();
		
		//dlgProgress = new WaitingDialogWithProgressBar("<html>Computing Directional correlation dimensions, please wait...<br>Open console window for further info.</html>");
		dlgProgress = new WaitingDialogWithProgressBar("Computing Directional correlation dimensions, please wait... Open console window for further info.",
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
			this.callbackProcessAllImages();
		}
	}

	public void getAndValidateActiveDataset() {

		datasetIn = imageDisplayService.getActiveDataset();

		if ((datasetIn.firstElement() instanceof UnsignedByteType) || (datasetIn.firstElement() instanceof FloatType)) {
			// That is OK, proceed
		} else {

			final MessageType messageType = MessageType.QUESTION_MESSAGE;
			final OptionType optionType = OptionType.OK_CANCEL_OPTION;
			final String title = "Validation result";
			final String message = "Data type not allowed: " + datasetIn.getType().getClass().getSimpleName();
			// Prompt for confirmation.
			// final UIService uiService = getContext().getService(UIService.class);
			Result result = uiService.showDialog(message, title, messageType, optionType);

			// Cancel the command execution if the user does not agree.
			// if (result != Result.YES_OPTION) System.exit(-1);
			// if (result != Result.YES_OPTION) return;
			return;
		}
		// get some info
		width = datasetIn.max(0) + 1;
		height = datasetIn.max(1) + 1;
		//depth = dataset.getDepth(); //does not work if third axis ist not specifyed as z-Axis
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
			if (plotWindowList != null) {
				for (int l = 0; l < plotWindowList.size(); l++) {
					plotWindowList.get(l).setVisible(false);
					plotWindowList.get(l).dispose();
					// plotWindowList.remove(l); /
				}
				plotWindowList.clear();
			}
		}
		if (optDeleteExistingTables) {
			List<Display<?>> list = defaultDisplayService.getDisplays();
			for (int i = 0; i < list.size(); i++) {
				Display<?> display = list.get(i);
				//System.out.println("display name: " + display.getName());
				if (display.getName().equals(tableName))
					display.close();
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
	 **/
	private void processActiveInputImage (int s) throws InterruptedException {
		
		long startTime = System.currentTimeMillis();
		if (choiceRadioButt_Direction.equals("Mean of 180 radial directions [0-180°]") && (booleanGetAllRadialDsValues)) {
			resultValuesTable = new double[(int) numSlices][192]; //11 + 181
		} else if (choiceRadioButt_Direction.equals("Mean of     4 radial directions [0-180°]") && (booleanGetAllRadialDsValues)) {
			resultValuesTable = new double[(int) numSlices][16]; //11 + 5
		}	else {
			resultValuesTable = new double[(int) numSlices][13];
		}

		// convert to float values
		// Img<T> image = (Img<T>) dataset.getImgPlus();
		// mg<FloatType> imgFloat; // =
		// opService.convert().float32((Img<T>)dataset.getImgPlus());

		// get slice and convert to float values
		// imgFloat = opService.convert().float32((Img<T>)dataset.getImgPlus());

//		imgFloat = new ArrayImgFactory<>(new FloatType()).create(width, height); // always single 2D
//		Cursor<FloatType> cursor = imgFloat.localizingCursor();
//		final long[] pos = new long[imgFloat.numDimensions()];
//		RandomAccess<RealType<?>> ra = datasetIn.randomAccess();
//		while (cursor.hasNext()) {
//			cursor.fwd();
//			cursor.localize(pos);
//			if (numSlices == 1) { // for only one 2D image;
//				ra.setPosition(pos[0], 0);
//				ra.setPosition(pos[1], 1);
//			} else { // for more than one image e.g. image stack
//				ra.setPosition(pos[0], 0);
//				ra.setPosition(pos[1], 1);
//				ra.setPosition(s, 2);
//			}
//			// ra.get().setReal(cursor.get().get());
//			cursor.get().setReal(ra.get().getRealFloat());
//		}
//
//		IntervalView<FloatType> iv = Views.interval(imgFloat, new long[] { 0, 0 },
//				new long[] { imgFloat.max(0), imgFloat.max(1) });
//
//		// Compute regression parameters
//		double[] resultValues = process(iv, s);
//		// 0 Ds-row, 1 R2-row, 2 StdErr-row, 3 Ds-col, 4 R2-col, 5 StdErr-col, 6 Ds, 7 R2, 8 Stderr
		
		
		RandomAccessibleInterval<?> rai = null;	
		if( (s==0) && (numSlices == 1) && (numDimensions == 2) ) { // for only one 2D image;
			rai =  (RandomAccessibleInterval<?>) datasetIn.getImgPlus();

		} else if ( (numSlices > 1) && (numDimensions == 3) ){ // for a stack of 2D images
			rai = (RandomAccessibleInterval<?>) Views.hyperSlice(datasetIn, 2, s);
		
		}

		// Compute regression parameters
		double[] resultValues = process(rai, s); //rai is already 2D, s parameter only for display titles
		// 0 Ds-row, 1 R2-row, 2 StdErr-row, 3 Ds-col, 4 R2-col, 5 StdErr-col, 6 Ds, 7 R2, 8 Stderr

		resultValuesTable[s][0] = resultValues[0]; // Ds-row
		resultValuesTable[s][1] = resultValues[3]; // Ds-col
		resultValuesTable[s][2] = resultValues[6]; // Ds
		resultValuesTable[s][3] = resultValues[1]; // R2-row
		resultValuesTable[s][4] = resultValues[4]; // R2-col
		resultValuesTable[s][5] = resultValues[7]; // R2
		resultValuesTable[s][6] = resultValues[2]; // StdErr-row
		resultValuesTable[s][7] = resultValues[5]; // StdErr-col
		resultValuesTable[s][8] = resultValues[8]; // StdErr
		resultValuesTable[s][9] = resultValues[9]; //#Radial directions
		resultValuesTable[s][10] = resultValues[10]; //Anisotropy index
		
		
		if (choiceRadioButt_Direction.equals("Mean of 180 radial directions [0-180°]") && (booleanGetAllRadialDsValues)) {
			for (int a = 0; a < 181; a++) {
			resultValuesTable[s][11+a] = resultValues[11+a];
			}
		}
		if (choiceRadioButt_Direction.equals("Mean of     4 radial directions [0-180°]") && (booleanGetAllRadialDsValues)) {
			for (int a = 0; a < 5; a++) {
			resultValuesTable[s][11+a] = resultValues[11+a];
			}
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
		if (choiceRadioButt_Direction.equals("Mean of 180 radial directions [0-180°]") && (booleanGetAllRadialDsValues)) {
			resultValuesTable = new double[(int) numSlices][192]; //11 + 181
		} else if (choiceRadioButt_Direction.equals("Mean of     4 radial directions [0-180°]") && (booleanGetAllRadialDsValues)) {
			resultValuesTable = new double[(int) numSlices][16]; //11 + 5
		}	else {
			resultValuesTable = new double[(int) numSlices][13];
		}

		// convert to float values
		// Img<T> image = (Img<T>) dataset.getImgPlus();
		// Img<FloatType> imgFloat; // =
		// opService.convert().float32((Img<T>)dataset.getImgPlus());

		// loop over all slices of stack
		for (int s = 0; s < numSlices; s++) { // p...planes of an image stack
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
				// get slice and convert to float values
				// imgFloat = opService.convert().float32((Img<T>)dataset.gett);
	
	//			imgFloat = new ArrayImgFactory<>(new FloatType()).create(width, height); // always single 2D
	//			Cursor<FloatType> cursor = imgFloat.localizingCursor();
	//			final long[] pos = new long[imgFloat.numDimensions()];
	//			RandomAccess<RealType<?>> ra = datasetIn.randomAccess();
	//			while (cursor.hasNext()) {
	//				cursor.fwd();
	//				cursor.localize(pos);
	//				if (numSlices == 1) { // for only one 2D image;
	//					ra.setPosition(pos[0], 0);
	//					ra.setPosition(pos[1], 1);
	//				} else { // for more than one image e.g. image stack
	//					ra.setPosition(pos[0], 0);
	//					ra.setPosition(pos[1], 1);
	//					ra.setPosition(s, 2);
	//				}
	//				// ra.get().setReal(cursor.get().get());
	//				cursor.get().setReal(ra.get().getRealFloat());
	//			}
	//
	//			IntervalView<FloatType> iv = Views.interval(imgFloat, new long[] { 0, 0 },
	//					new long[] { imgFloat.max(0), imgFloat.max(1) });
	//
	//			// Compute regression parameters
	//			double[] resultValues = process(iv, s);
	//			// 0 Ds-row, 1 R2-row, 2 StdErr-row, 3 Ds-col, 4 R2-col, 5 StdErr-col, 6 Ds, 7 R2, 8 Stderr
				
				
				RandomAccessibleInterval<?> rai = null;	
				if( (s==0) && (numSlices == 1) && (numDimensions == 2) ) { // for only one 2D image;
					rai =  (RandomAccessibleInterval<?>) datasetIn.getImgPlus();
	
				} else if ( (numSlices > 1) && (numDimensions == 3) ){ // for a stack of 2D images
					rai = (RandomAccessibleInterval<?>) Views.hyperSlice(datasetIn, 2, s);
				
				}
	
				// Compute regression parameters
				double[] resultValues = process(rai, s); //rai is already 2D, s parameter only for display titles
				// 0 Ds-row, 1 R2-row, 2 StdErr-row, 3 Ds-col, 4 R2-col, 5 StdErr-col, 6 Ds, 7 R2, 8 Stderr
	
				resultValuesTable[s][0] = resultValues[0]; // Ds-row
				resultValuesTable[s][1] = resultValues[3]; // Ds-col
				resultValuesTable[s][2] = resultValues[6]; // Ds
				resultValuesTable[s][3] = resultValues[1]; // R2-row
				resultValuesTable[s][4] = resultValues[4]; // R2-col
				resultValuesTable[s][5] = resultValues[7]; // R2
				resultValuesTable[s][6] = resultValues[2]; // StdErr-row
				resultValuesTable[s][7] = resultValues[5]; // StdErr-col
				resultValuesTable[s][8] = resultValues[8]; // StdErr
				resultValuesTable[s][9] = resultValues[9]; //#Radial directions
				resultValuesTable[s][10] = resultValues[10]; //Anisotropy index
	
				if (choiceRadioButt_Direction.equals("Mean of 180 radial directions [0-180°]") && (booleanGetAllRadialDsValues)) {
					for (int a = 0; a < 181; a++) {
					resultValuesTable[s][11+a] = resultValues[11+a];
					}
				} 
				if (choiceRadioButt_Direction.equals("Mean of     4 radial directions [0-180°]") && (booleanGetAllRadialDsValues)) {
					for (int a = 0; a < 5; a++) {
					resultValuesTable[s][11+a] = resultValues[11+a];
					}
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
	private void generateTableHeader() {
		
		GenericColumn columnFileName       = new GenericColumn("File name");
		GenericColumn columnSliceName      = new GenericColumn("Slice name");
		IntColumn columnMaxNumBoxes        = new IntColumn("# Boxes");
		IntColumn columnRegMin             = new IntColumn("RegMin");
		IntColumn columnRegMax             = new IntColumn("RegMax");
		GenericColumn columnMethod         = new GenericColumn("Method");
		GenericColumn columnAnalysisType   = new GenericColumn("Analysis type");
		DoubleColumn columnDsRow      	   = new DoubleColumn("Dc-hor");
		DoubleColumn columnDsCol      	   = new DoubleColumn("Dc-vert");
		DoubleColumn columnDs         	   = new DoubleColumn("Dc");
		DoubleColumn columnR2Row      	   = new DoubleColumn("R2-hor");
		DoubleColumn columnR2Col      	   = new DoubleColumn("R2-vert");
		DoubleColumn columnR2         	   = new DoubleColumn("R2");
		DoubleColumn columnStdErrRow   	   = new DoubleColumn("StdErr-hor");
		DoubleColumn columnStdErrCol 	   = new DoubleColumn("StdErr-vert");
		DoubleColumn columnStdErr     	   = new DoubleColumn("StdErr");
		IntColumn columnNumRadialLines     = new IntColumn("# Radial directions");
		DoubleColumn columnAnisotropyIndex = new DoubleColumn("Anisotropy index");

		table = new DefaultGenericTable();
		table.add(columnFileName);
		table.add(columnSliceName);
		table.add(columnMaxNumBoxes);
		table.add(columnRegMin);
		table.add(columnRegMax);
		table.add(columnMethod);
		table.add(columnAnalysisType);
		table.add(columnDsRow);
		table.add(columnDsCol);
		table.add(columnDs);
		table.add(columnR2Row);
		table.add(columnR2Col);
		table.add(columnR2);
		table.add(columnStdErrRow);
		table.add(columnStdErrCol);
		table.add(columnStdErr);
		table.add(columnNumRadialLines);
		table.add(columnAnisotropyIndex);
		if (choiceRadioButt_Direction.equals("Mean of 180 radial directions [0-180°]") && (booleanGetAllRadialDsValues)){
			for (int a = 0; a < 181; a++) {
				table.add(new DoubleColumn("Dc " +  anglesGrad[a] + "°"));
			}
		}
		if (choiceRadioButt_Direction.equals("Mean of     4 radial directions [0-180°]") && (booleanGetAllRadialDsValues)){
			for (int a = 0; a < 5; a++) {
				table.add(new DoubleColumn("Dc " + anglesGrad[a] + "°"));
			}
		}
	}

	/**
	 * collects current result and shows table
	 * 
	 * @param int slice number of active image.
	 */
	private void collectActiveResultAndShowTable(int sliceNumber) {

		int numBoxes = spinnerInteger_NumBoxes;
		int regMin   = spinnerInteger_RegMin;
		int regMax   = spinnerInteger_RegMax;
		String direction     = choiceRadioButt_Direction;
		String analysisType  = choiceRadioButt_AnalysisType;
	
		int s = sliceNumber;
		// 0 Intercept, 1 Dim, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
		// fill table with values
		table.appendRow();
		table.set("File name",  		 table.getRowCount() - 1, datasetName);	
		if (sliceLabels != null) 		 table.set("Slice name", table.getRowCount() - 1, sliceLabels[s]);
		table.set("# Boxes",    		 table.getRowCount()-1, numBoxes);	
		table.set("RegMin",         	 table.getRowCount() - 1, regMin);
		table.set("RegMax",         	 table.getRowCount() - 1, regMax);
		table.set("Method",        	     table.getRowCount() - 1, direction);
		table.set("Analysis type",  	 table.getRowCount() - 1, analysisType);
		table.set("Dc-hor",     		 table.getRowCount() - 1, resultValuesTable[s][0]);
		table.set("Dc-vert",    		 table.getRowCount() - 1, resultValuesTable[s][1]);
		table.set("Dc",         		 table.getRowCount() - 1, resultValuesTable[s][2]);
		table.set("R2-hor",     		 table.getRowCount() - 1, resultValuesTable[s][3]);
		table.set("R2-vert",     		 table.getRowCount() - 1, resultValuesTable[s][4]);
		table.set("R2",         		 table.getRowCount() - 1, resultValuesTable[s][5]);
		table.set("StdErr-hor", 		 table.getRowCount() - 1, resultValuesTable[s][6]);
		table.set("StdErr-vert", 		 table.getRowCount() - 1, resultValuesTable[s][7]);
		table.set("StdErr",    			 table.getRowCount() - 1, resultValuesTable[s][8]);
		table.set("# Radial directions", table.getRowCount() - 1, (int) resultValuesTable[s][9]);
		table.set("Anisotropy index",    table.getRowCount() - 1, resultValuesTable[s][10]); //Anisotropy index =(Dr-Dc)/(De-Dt)

		//add 181 angles
		if (choiceRadioButt_Direction.equals("Mean of 180 radial directions [0-180°]") && (booleanGetAllRadialDsValues)){
			for (int a = 0; a < 181; a++) {
				table.set("Dc "+anglesGrad[a]+"°", table.getRowCount() - 1, resultValuesTable[s][11+a]);
			}
		}
		//add 4+1 angles
		if (choiceRadioButt_Direction.equals("Mean of     4 radial directions [0-180°]") && (booleanGetAllRadialDsValues)){
			for (int a = 0; a < 5; a++) {
				table.set("Dc "+anglesGrad[a]+"°", table.getRowCount() - 1, resultValuesTable[s][11+a]);
			}
		}
		
		// Show table
		uiService.show(tableName, table);
	}

	/** collects all results and shows table */
	private void collectAllResultsAndShowTable() {
		
		int numBoxes = spinnerInteger_NumBoxes;
		int regMin   = spinnerInteger_RegMin;
		int regMax   = spinnerInteger_RegMax;
		String direction     = choiceRadioButt_Direction;
		String analysisType  = choiceRadioButt_AnalysisType;	

		// loop over all slices
		for (int s = 0; s < numSlices; s++) { // slices of an image stack
			// 0 Intercept, 1 Dim, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
			// fill table with values
			table.appendRow();
			table.set("File name",  		 table.getRowCount() - 1, datasetName);	
			if (sliceLabels != null) 		 table.set("Slice name", table.getRowCount() - 1, sliceLabels[s]);
			table.set("# Boxes",    		 table.getRowCount()-1, numBoxes);	
			table.set("RegMin",        		 table.getRowCount() - 1, regMin);
			table.set("RegMax",         	 table.getRowCount() - 1, regMax);
			table.set("Method",        	     table.getRowCount() - 1, direction);
			table.set("Analysis type",  	 table.getRowCount() - 1, analysisType);
			table.set("Dc-hor",     		 table.getRowCount() - 1, resultValuesTable[s][0]);
			table.set("Dc-vert",     		 table.getRowCount() - 1, resultValuesTable[s][1]);
			table.set("Dc",        			 table.getRowCount() - 1, resultValuesTable[s][2]);
			table.set("R2-hor",     		 table.getRowCount() - 1, resultValuesTable[s][3]);
			table.set("R2-vert",     		 table.getRowCount() - 1, resultValuesTable[s][4]);
			table.set("R2",         		 table.getRowCount() - 1, resultValuesTable[s][5]);
			table.set("StdErr-hor", 		 table.getRowCount() - 1, resultValuesTable[s][6]);
			table.set("StdErr-vert", 		 table.getRowCount() - 1, resultValuesTable[s][7]);
			table.set("StdErr",     		 table.getRowCount() - 1, resultValuesTable[s][8]);
			table.set("# Radial directions", table.getRowCount() - 1, (int)resultValuesTable[s][9]);
			table.set("Anisotropy index",    table.getRowCount() - 1, resultValuesTable[s][10]); //Anisotropy index anisotropy index =(Ds-Dv)/(De-Dt)
			
			//add 181 angles
			if (choiceRadioButt_Direction.equals("Mean of 180 radial directions [0-180°]") && (booleanGetAllRadialDsValues)){
				for (int a = 0; a < 181; a++) {
					table.set("Dc "+anglesGrad[a]+"°", table.getRowCount() - 1, resultValuesTable[s][11+a]);
			
				}
			}
			//add 5 angles
			if (choiceRadioButt_Direction.equals("Mean of     4 radial directions [0-180°]") && (booleanGetAllRadialDsValues)){
				for (int a = 0; a < 5; a++) {
					table.set("Dc "+anglesGrad[a]+"°", table.getRowCount() - 1, resultValuesTable[s][11+a]);
				}
			}
		}
		uiService.show(tableName, table);
	}

	/**
	*
	* Processing
	*/
	private double[] process(RandomAccessibleInterval<?> rai, int plane) { // plane plane (Image) number
		
		int numBoxes         = spinnerInteger_NumBoxes;
		int regMin           = spinnerInteger_RegMin;
		int regMax           = spinnerInteger_RegMax;
		String direction 	 = choiceRadioButt_Direction;	
		String analysisType  = choiceRadioButt_AnalysisType;	
		int pixelPercentage  = spinnerInteger_PixelPercentage;	
		boolean optShowPlot  = booleanShowDoubleLogPlot;
		
		int numBands = 1;

		long width = rai.dimension(0);
		long height = rai.dimension(1);

		String imageType = "Binary"; // "Grey" "RGB"....
		double[] regressionValues;
		double[] resultValues;
		if (direction.equals("Mean of 180 radial directions [0-180°]")) {
			resultValues = new double[192]; //11 +181 Dcs
		} else if (direction.equals("Mean of     4 radial directions [0-180°]")) {
				resultValues = new double[16]; //11 + 5 Dcs
		} else {
			resultValues = new double[11]; // Dim-row, R2-row, StdErr-row, Dim-col, R2-col, StdErr-col, Dim, R2, StdErr
		}
	
		//******************************************************************************************************
		if (direction.equals("Horizontal and vertical direction")) {

			// Ds-0°  180°--------------------------------------------------------------------------------
			logService.info(this.getClass().getName() + " Computing horizontal Dc");
			regressionValues =this.computeRegressionValues(rai, numBoxes, regMin, regMax, analysisType, pixelPercentage, 0, 180, optShowPlot, plane, numBands);	
			resultValues[0] = regressionValues[1]; // Dc = slope
			resultValues[1] = regressionValues[4];
			resultValues[2] = regressionValues[3];
		
			// Dc- +90° -90° ---------------------------------------------------------------------------------
			logService.info(this.getClass().getName() + " Computing vertical Dc");
			regressionValues =this.computeRegressionValues(rai, numBoxes, regMin, regMax, analysisType, pixelPercentage, 90, -90, optShowPlot, plane, numBands);
			
			resultValues[3] = regressionValues[1]; // Dc = slope
			resultValues[4] = regressionValues[4];
			resultValues[5] = regressionValues[3];
			
			//Ds --------------------------------------------------------------------------------------------------
			resultValues[6] = (resultValues[0] + resultValues[3]) / 2.0; // Dc = (Dc-hor + Dc-vert)/2
			resultValues[7] = (resultValues[1] + resultValues[4]) / 2.0; // R2 = (R2-hor + R2-vert)/2
			resultValues[8] = (resultValues[2] + resultValues[5]) / 2.0; // StdErr = (StdErr-hor + StdErr-vert)/2
			
			resultValues[9]  = 2; //# Actual number of radial directions
			resultValues[10] = Math.abs(resultValues[0] - resultValues[3])/(2-1); //ABS(Ds-hor - Ds -vert)/(De - Dt);
		}
		//******************************************************************************************************************************************
		else if (direction.equals("Mean of 180 radial directions [0-180°]") || direction.equals("Mean of     4 radial directions [0-180°]")) {
			
			int numAngles = 0;
			double Ds_0   = Double.NaN;
			double Ds_90  = Double.NaN; 
			//define number of angles in the range of 0 - pi
			//int numAngles = (180 + 1);  //maximal 180, maybe only 4 (0°, 45°, 90°, 135°, 180°)
			if (direction.equals("Mean of 180 radial directions [0-180°]")) {
				numAngles = (180 + 1); //range 0 - pi through the center of the image
			}
			if (direction.equals("Mean of     4 radial directions [0-180°]")) {
				numAngles = (4 + 1);   //range 0 - pi through the center of the image
			}
							
			anglesGrad = new double[numAngles];
			for (int i = 0; i < numAngles; i++) {
				//angles[i] = (i * Math.PI / (numAngles - 1) - (Math.PI / 2.0)); // -pi/2,...,0,...+pi/2
				//anglesRad[i]  = (i * Math.PI / (numAngles - 1)); // simply Counterclockwise 
				anglesGrad[i] = (i * 180/ (numAngles - 1)); // simply Counterclockwise 
			}
			
			//Dc-radial--------------------------------------------------------------------------------------------------	
			int numActualRadialDirections = 0; //some directions have too less pixels and may be thrown away later on
	
			double angle1;
			double angle2;
			
			for (int a = 0; a < numAngles; a++) { // loop through angle			
				angle1 = anglesGrad[a];
				angle2 = angle1 +180;
				
				logService.info(this.getClass().getName() + " Computing Dc at angle " + angle1 +"° and " + angle2 + "°");
				regressionValues =this.computeRegressionValues(rai, numBoxes, regMin, regMax, analysisType, pixelPercentage, angle1, angle2, optShowPlot, plane, numBands);
				
				double dim = regressionValues[1];
				if (dim == 0.0) dim = Double.NaN;
				if (regressionValues[4] > 0.9) { //R2 >0.9
					if (a < (numAngles - 1)) { // Mean only from 4 bzw. 180 angles
						numActualRadialDirections += 1;
						resultValues[6] += dim; // Ds = slope
						resultValues[7] += regressionValues[4];
						resultValues[8] += regressionValues[3];
					}
					if ( angle1 == 0.0)  Ds_0  = dim;
					if ( angle1 == 90.0) Ds_90 = dim;
					//add 180 +1 angles
					if (booleanGetAllRadialDsValues){
						//one of 181 Ds values
						resultValues[11+a] = dim;
					}
					
	
				} //
				
			} //angles a for (int a = 0; a < numAngles; a++) { // loop through angles
			// mean values
			resultValues[6] = resultValues[6]/numActualRadialDirections; //average   
			resultValues[7] = resultValues[7]/numActualRadialDirections; //average
			resultValues[8] = resultValues[8]/numActualRadialDirections; //average		
			
			//set other table entries to NaN
			resultValues[0] = Float.NaN;
			resultValues[1] = Float.NaN;
			resultValues[2] = Float.NaN;
			resultValues[3] = Float.NaN;
			resultValues[4] = Float.NaN;
			resultValues[5] = Float.NaN;
			
			//set # of actual signals
			resultValues[9]  = numActualRadialDirections; // # Radial lines	
			resultValues[10] = Math.abs(Ds_0 - Ds_90)/(2-1); //ABS(Ds-0° - Ds-90°)/(De - Dt);
			
			logService.info(this.getClass().getName() + " Number of actual radial directions=" + numActualRadialDirections);
		}

		return resultValues;
		// Dim-row, R2-row, StdErr-row, Dim-col, R2-col, StdErr-col, Dim, R2, StdErr
		// Output
		// uiService.show(tableName, table);
		// result = ops.create().img(image, new FloatType());
		// table
	}
	
	/**
	 * Compute directional correlations
	 * @return double[] regressionValues
	 */
	private double[] computeRegressionValues(RandomAccessibleInterval<?> rai, int numBoxes, int regMin, int regMax, String analysisType, int pixelPercentage, 
																	  double angle1, double angle2, boolean optShowPlot, int plane, int numBands) {
		//WARNING: Output is only the last band!!
		double[] regressionValues = null; //output of this method
		double[][] totals = new double[numBoxes][numBands];
		// double[] totalsMax = new double[numBands]; //for binary images
		int[][] eps = new int[numBoxes][numBands];
		// definition of eps
		for (int n = 0; n < numBoxes; n++) {
			for (int b = 0; b < numBands; b++) {	
				eps[n][b] = (int)Math.round(Math.pow(2, n));
				//logService.info(this.getClass().getName() + " n:" + n + " eps:  " + eps[n][b]);	
			}
		}	
		RandomAccess<?> ra=  rai.randomAccess();
		long number_of_points = 0;
		int max_random_number = (int) (100/pixelPercentage); // Evaluate max. random number
		int random_number = 0;
		int radius;		
		long count = 0;
		int sample = 0;
		double phi1 = angle1;	// angle in plane in grad
		double phi2 = angle2;	// angle in plane in grad	
		int xWert = 0;
		int yWert = 0; 
		double norm = Double.NaN; //normalization factor. Now we have only 2r of possible pairs instead of r^2pi
		
		if  (max_random_number == 1) { // no statistical approach, take all image pixels
			for (int b = 0; b < numBands; b++) {
				for (int n = 0; n < numBoxes; n++) { //2^1  to 2^numBoxes		
					radius = eps[n][b];	
					norm = (double)radius*(double)radius*Math.PI/(2.0*(double)radius); //HA
					for (int x = 0; x < width; x++){
						for (int y = 0; y < height; y++){	
							ra.setPosition(x, 0);
							ra.setPosition(y, 1);
							if((((UnsignedByteType) ra.get()).get() > 0) ){
								number_of_points++; // total number of points 	
								// scroll through sub-array with spherical coordinates (r,theta,phi) 
								for (int r = 1; r <= radius; r++) {	//r = 1 HA				
									xWert =  (int)Math.round(  r  * Math.cos( Math.toRadians(phi1)) )  ;
									yWert =  (int)Math.round(  r  * Math.sin( Math.toRadians(phi1)) )  ;							
									if( x + xWert >= 0 && x + xWert < width && y + yWert >= 0 && y + yWert < height ){
										ra.setPosition(x+xWert, 0);
										ra.setPosition(y+yWert, 1);	
										sample = ((UnsignedByteType) ra.get()).get();	
										if((sample > 0) ){
											if (analysisType.equals("Binary")) count = count + 1;
											if (analysisType.equals("Grey"))   count = count + sample;
										}
									}
									xWert =  (int)Math.round(  r  * Math.cos( Math.toRadians(phi2)) )  ;
									yWert =  (int)Math.round(  r  * Math.sin( Math.toRadians(phi2)) )  ;							
									if( x + xWert >= 0 && x + xWert < width && y + yWert >= 0 && y + yWert < height ){
										ra.setPosition(x+xWert, 0);
										ra.setPosition(y+yWert, 1);	
										sample = ((UnsignedByteType) ra.get()).get();	
										if((sample > 0) ){
											if (analysisType.equals("Binary")) count = count + 1;
											if (analysisType.equals("Grey"))   count = count + sample;
										}				
									}
								}// radius
							}
						} //y	
					} //x  
					// calculate the average number of neighboring points within distance "radius":  
					//number of neighbors = counts //no self counts because radius starts with 1
					//average number of neighbors = number of neighbors / total_number_of_points		 
					totals[n][b] = (double)count*norm/number_of_points; //norm HA
					//System.out.println("Counts:"+counts+" total number of points:"+total_number_of_points);
					// set counts equal to zero
					count=0;	
					number_of_points=0;
				} //n Box sizes		
			}//b band
		} // no statistical approach
		else { //statistical approach
			for (int b = 0; b < numBands; b++) {
				for (int n = 0; n < numBoxes; n++) { //2^1  to 2^numBoxes		
					radius = eps[n][b];	
					norm = (double)radius*(double)radius*Math.PI/(2.0*(double)radius); //HA
					for (int x = 0; x < width; x++){
						for (int y = 0;  y < height; y++){		
							random_number = (int) (Math.random()*max_random_number+1);
							if( random_number == 1 ){ // UPDATE 07.08.2013 
								ra.setPosition(x, 0);
								ra.setPosition(y, 1);	
								if((((UnsignedByteType) ra.get()).get() > 0) ){
									number_of_points++; // total number of points 	
									// scroll through sub-array with spherical coordinates (r,theta,phi) 
									for (int r = 1; r <= radius; r++) {	//r = 1 HA					
										xWert =  (int)Math.round(  r  * Math.cos( Math.toRadians(phi1)) )  ;
										yWert =  (int)Math.round(  r  * Math.sin( Math.toRadians(phi1)) )  ;							
										if( x + xWert >= 0 && x + xWert < width && y + yWert >= 0 && y + yWert < height ){
											ra.setPosition(x+xWert, 0);
											ra.setPosition(y+yWert, 1);	
											sample = ((UnsignedByteType) ra.get()).get();	
											if((sample > 0) ){
												if (analysisType.equals("Binary")) count = count + 1;
												if (analysisType.equals("Grey"))   count = count + sample;
											}				
										}
										xWert =  (int)Math.round(  r  * Math.cos( Math.toRadians(phi2)) )  ;
										yWert =  (int)Math.round(  r  * Math.sin( Math.toRadians(phi2)) )  ;							
										if( x + xWert >= 0 && x + xWert < width && y + yWert >= 0 && y + yWert < height ){
											ra.setPosition(x+xWert, 0);
											ra.setPosition(y+yWert, 1);	
											sample = ((UnsignedByteType) ra.get()).get();	
											if((sample > 0) ){
												if (analysisType.equals("Binary")) count = count + 1;
												if (analysisType.equals("Grey"))   count = count + sample;
											}				
										}
									}// radius
								}
							}
						} //y	
					} //x  
					// calculate the average number of neighboring points within distance "radius":  
					//number of neighbors = counts //no self counts because radius starts with 1
					//average number of neighbors = number of neighbors / total_number_of_points		 
					totals[n][b] = (double)count*norm/number_of_points; //norm HA
					//System.out.println("Counts:"+counts+" total number of points:"+total_number_of_points);
					// set counts equal to zero
					count=0;	
					number_of_points=0;
				} //n Box sizes		
			}//b band
		}
	
		//Computing log values for plot 
		//Change sequence of entries to start with a pixel
		double[][] lnTotals = new double[numBoxes][numBands];
		double[][] lnEps    = new double[numBoxes][numBands];
		for (int n = 0; n < numBoxes; n++) {
			for (int b = 0; b < numBands; b++) {
				if (totals[n][b] <= 1) {
					//lnTotals[numBoxes - n - 1][b] = 0.0; //Math.log(Float.MIN_VALUE); // damit logarithmus nicht undefiniert ist//IQM
					lnTotals[n][b] = 0.0;
				} else if (Double.isNaN(totals[n][b])) {
					//lnTotals[numBoxes - n - 1][b] = 0.0;
					lnTotals[n][b] = Double.NaN;
				} else {
					//lnTotals[numBoxes - n - 1][b] = Math.log(totals[n][b]);//IQM
					lnTotals[n][b] = Math.log(totals[n][b]); //
				}
				//lnEps[n][b] = Math.log(eps[numBoxes - n - 1 ][b]); //IQM
				lnEps[n][b] = Math.log(eps[n][b]);
				//logService.info(this.getClass().getName() + " n:" + n + " eps:  " + eps[n][b]);
				//logService.info(this.getClass().getName() + " n:" + n + " lnEps:  "+  lnEps[n][b] );
				//logService.info(this.getClass().getName() + " n:" + n + " totals[n][b]: " + totals[n][b]);
			}
		}
		
		//Create double log plot
		boolean isLineVisible = false; //?
		for (int b = 0; b < numBands; b++) { // mehrere Bands
			
			// Plot //nur ein Band!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
			double[] lnDataX = new double[numBoxes];
			double[] lnDataY = new double[numBoxes];
				
			for (int n = 0; n < numBoxes; n++) {	
				lnDataY[n] = lnTotals[n][b];		
				lnDataX[n] = lnEps[n][b];
			}
			// System.out.println("FractalDimensionBoxCounting: dataY: "+ dataY);
			// System.out.println("FractalDimensionBoxCounting: dataX: "+ dataX);
		
			if (optShowPlot) {			
				String preName = "";
				if (numSlices > 1) {
					preName = "Slice-"+String.format("%03d", plane);
				}
				if(    (angle1 == 0)
					|| (angle1 == 45)
					|| (angle1 == 90)
					|| (angle1 == 135)
					|| (angle1 == 180)) { //show first middle and last plot
					preName =  preName + " " + angle1 +"° "+ angle2 +"° ";
					RegressionPlotFrame doubleLogPlot = DisplayRegressionPlotXY(lnDataX, lnDataY, isLineVisible, "Double Log Plot - Directional correlation dimension", 
							preName + datasetName, "ln(Radius)", "ln(Count)", "",
							regMin, regMax);
					doubleLogPlotList.add(doubleLogPlot);
				}			
			}
			// Compute regression
			LinearRegression lr = new LinearRegression();
			regressionValues = lr.calculateParameters(lnDataX, lnDataY, regMin, regMax);
			//0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
		}
			
		return regressionValues; //output is only the last band
	}
	

	// This method shows the double log plot
	private void showPlot(double[] lnDataX, double[] lnDataY, String preName, int plane, int regMin, int regMax) {
	
		if (lnDataX == null) {
			logService.info(this.getClass().getName() + " lnDataX == null, cannot display the plot!");
			return;
		}
		if (lnDataY == null) {
			logService.info(this.getClass().getName() + " lnDataY == null, cannot display the plot!");
			return;
		}
		if (plane < 0) {
			logService.info(this.getClass().getName() + " plane < 0, cannot display the plot!");
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
			preName = "Slice-" + String.format("%03d", plane) + "-";
		} else {
			preName = preName + String.format("%03d", plane) + "-";
		}
		
		boolean isLineVisible = false; // ?
		RegressionPlotFrame doubleLogPlot = DisplayRegressionPlotXY(lnDataX, lnDataY, isLineVisible,
				"Double Log Plot - Directional correlation dimension", preName + datasetName, "ln(k)", "ln(L)", "", regMin, regMax);
		doubleLogPlotList.add(doubleLogPlot);
		
		
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
			boolean isLineVisible, String frameTitle, String plotLabel, String xAxisLabel, String yAxisLabel, String legendLabel,
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
		if (image != null) ij.ui().show(image);
		// execute the filter, waiting for the operation to finish.
		// ij.command().run(Img2DFractalDimensionDirectionalCorrelation.class,
		// true).get().getOutput("image");
		ij.command().run(Img2DFractalDimensionDirectionalCorrelation.class, true);
	}
}
