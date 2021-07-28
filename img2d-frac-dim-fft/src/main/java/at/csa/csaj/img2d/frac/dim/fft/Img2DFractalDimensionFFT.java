/*-
 * #%L
 * Project: ImageJ plugin for computing fractal dimension with FFT
 * File: Img2DFractalDimensionFFT.java
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

package at.csa.csaj.img2d.frac.dim.fft;

import java.awt.Frame;
import java.awt.Toolkit;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.array.ArrayRandomAccess;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
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
import at.csa.csaj.commons.plot.RegressionPlotFrame;
import at.csa.csaj.commons.regression.LinearRegression;
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_2D;
import io.scif.DefaultImageMetadata;
import io.scif.MetaTable;

/**
 * A {@link Command} plugin computing
 * <the fractal dimension by FFT</a>
 * of an image.
 */
@Plugin(type = InteractiveCommand.class, 
        headless = true,
	    label = "FFT dimension", menu = {
        @Menu(label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT, mnemonic = MenuConstants.PLUGINS_MNEMONIC),
        @Menu(label = "ComsystanJ"),
        @Menu(label = "Image (2D)"),
        @Menu(label = "FFT dimension", weight = 11)})
public class Img2DFractalDimensionFFT<T extends RealType<T>> extends InteractiveCommand implements Command, Previewable { //non blocking GUI
//public class Img2DFractalDimensionFFT<T extends RealType<T>> implements Command {	//modal GUI
	
	private static final String PLUGIN_LABEL            = "<html><b>Computes fractal dimension with FFT</b></html>";
	private static final String SPACE_LABEL             = "";
	private static final String REGRESSION_LABEL        = "<html><b>Regression parameters</b></html>";
	private static final String METHODOPTIONS_LABEL     = "<html><b>Method options</b></html>";
	private static final String BACKGROUNDOPTIONS_LABEL = "<html><b>Background option</b></html>";
	private static final String DISPLAYOPTIONS_LABEL    = "<html><b>Display options</b></html>";
	private static final String PROCESSOPTIONS_LABEL    = "<html><b>Process options</b></html>";
	
	private static double[][] imgA;
	private static Img<FloatType> imgFloat; 
	private static Img<UnsignedByteType> imgUnsignedByte;
	private static Img<UnsignedByteType> imgMirrored;
	private static RandomAccessibleInterval<DoubleType>  raiWindowed; 
	private static RandomAccessibleInterval<?> rai;
	private static RandomAccess<?> ra;
	private static Cursor<?> cursor = null;
	private static String datasetName;
	private static String[] sliceLabels;
	private static long width  = 0;
	private static long height = 0;
	private static long numDimensions = 0;
	private static long numSlices  = 0;
	private static int  numOfK = 0;
	
	private static double[] sequence;
	
	private static ArrayList<RegressionPlotFrame> doubleLogPlotList = new ArrayList<RegressionPlotFrame>();
	private static double[][] resultValuesTable; //first column is the image index, second column are the corresponding regression values
	private static final String tableName = "Table - FFT dimension";
	
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
  	private final String labelRegression = REGRESSION_LABEL;

    @Parameter(label = "Maximal k",
    		   description = "Maximal frequency k",
	       	   style = NumberWidget.SPINNER_STYLE,
	           min = "1",
	           max =  "10000000000",
	           stepSize = "1",
	           persist  = false,  //restore previous value default = true
	           initializer = "initialMaxK",
	           callback    = "callbackMaxK")
    private int spinnerInteger_MaxK;
    
    @Parameter(label = "Regression Min",
    		   description = "Minimum x value of linear regression",
 		       style = NumberWidget.SPINNER_STYLE,
 		       min = "1",
 		       max =  "10000000000",
 		       stepSize = "1",
 		       //persist  = false,   //restore previous value default = true
 		       initializer = "initialRegMin",
 		       callback = "callbackRegMin")
    private int spinnerInteger_RegMin = 1;
 
    @Parameter(label = "Regression Max",
    		   description = "Maximum x value of linear regression",
    		   style = NumberWidget.SPINNER_STYLE,
		       min = "3",
		       max = "10000000000",
		       stepSize = "1",
		       persist  = false,   //restore previous value default = true
		       initializer = "initialRegMax",
		       callback = "callbackRegMax")
    private int spinnerInteger_RegMax = 3;
    
	//-----------------------------------------------------------------------------------------------------
    @Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
    private final String labelMethodOptions = METHODOPTIONS_LABEL;
     
 	@Parameter(label = "Windowing",
			description = "Windowing type with increasing filter strength",
			style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
			choices = {"Rectangular", "Bartlett", "Hamming", "Hanning", "Blackman", "Gaussian", "Parzen"}, //In the order of increasing filter strength
			persist  = false,  //restore previous value default = true
			initializer = "initialWindowingType",
			callback = "callbackWindowingType")
	private String choiceRadioButt_WindowingType;
     
    @Parameter(label = "Power spectrum",
    		    description = "Type of power spectrum computation",
    		    style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
      		    choices = {"Circular average", "Mean of line scans", "Integral of line scans"},
      		    //persist  = false,  //restore previous value default = true
    		    initializer = "initialPowerSpecType",
                callback = "callbackPowerSpecType")
    private String choiceRadioButt_PowerSpecType;
     
//   @Parameter(label = "Add mirrored images",
//    		 	description = "Add horizontally,vertically and diagonally mirrored images. Supresses edge errors",
// 		    	//persist  = false,  //restore previous value default = true
//		        initializer = "initialAddMirroredImages",
//		        callback = "callbackAddMirroredImages")
//	 private boolean booleanAddMirroredImages;
    
 	//-----------------------------------------------------------------------------------------------------
    @Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
    private final String labelDisplayOptions = DISPLAYOPTIONS_LABEL;
      
    @Parameter(label = "Show double log plot",
    		    //persist  = false,  //restore previous value default = true
  		        initializer = "initialShowDoubleLogPlots")
	private boolean booleanShowDoubleLogPlot;
       
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
     
 	@Parameter(label = "Image #", description = "Image slice number", style = NumberWidget.SPINNER_STYLE, min = "1", max = "99999999", stepSize = "1",
			   persist = false, // restore  previous value  default  =  true
			   initializer = "initialNumImageSlice",
			   callback = "callbackNumImageSlice")
	private int spinnerInteger_NumImageSlice;
	
	@Parameter(label   = "   Process single image #    ",
		    	callback = "callbackProcessSingleImage")
	private Button buttonProcessSingelImage;
    
//	Deactivated, because it does not work in Fiji (although it works in ImageJ2 -Eclipse)	
//  @Parameter(label   = "Process single active image ",
//   		    callback = "callbackProcessActiveImage")
//	private Button buttonProcessActiveImage;
	
    @Parameter(label   = "Process all available images",
 		        callback = "callbackProcessAllImages")
	private Button buttonProcessAllImages;

    //---------------------------------------------------------------------
 
    //The following initialzer functions set initial values
    protected void initialMaxK() {
      	numOfK = getMaxK((int)datasetIn.dimension(0), (int)datasetIn.dimension(1));
      	spinnerInteger_MaxK = numOfK;
    }
    protected void initialRegMin() {
    	spinnerInteger_RegMin = 1;
    }
    protected void initialRegMax() {
    	numOfK = getMaxK((int)datasetIn.dimension(0), (int)datasetIn.dimension(1));
    	spinnerInteger_RegMax =  numOfK;
    }
    protected void initialWindowingType() {
		choiceRadioButt_WindowingType = "Hanning";
	} 
    protected void initialPowerSpecType() {
    	choiceRadioButt_PowerSpecType = "Circular average";
    	// to set maximal k and RegMax
    	getAndValidateActiveDataset();
		int numOfK = getMaxK((int)width, (int)height);
		spinnerInteger_MaxK   = numOfK;
		spinnerInteger_RegMax = numOfK;	
    } 
//    protected void initialAddMirroredImages() {
//    	booleanAddMirroredImages = true;
//    }
    protected void initialShowDoubleLogPlots() {
    	booleanShowDoubleLogPlot = true;
    }
    protected void initialOverwriteDisplays() {
    	booleanOverwriteDisplays = true;
    }
	protected void initialNumImageSlice() {
    	spinnerInteger_NumImageSlice = 1;
	}
    
	// The following method is known as "callback" which gets executed
	// whenever the value of a specific linked parameter changes.
	/** Executed whenever the {@link #spinInteger_MaxK} parameter changes. */
	protected void callbackMaxK() {
		
		if  (spinnerInteger_MaxK < 3) {
			spinnerInteger_MaxK = 3;
		}
		int numMaxK = getMaxK((int)datasetIn.dimension(0), (int)datasetIn.dimension(1));	
		if (spinnerInteger_MaxK > numMaxK) {
			spinnerInteger_MaxK = numMaxK;
		};
		if (spinnerInteger_RegMax > spinnerInteger_MaxK) {
			spinnerInteger_RegMax = spinnerInteger_MaxK;
		}
		if (spinnerInteger_RegMin >= spinnerInteger_RegMax - 2) {
			spinnerInteger_RegMin = spinnerInteger_RegMax - 2;
		}

		numOfK = spinnerInteger_MaxK;
		logService.info(this.getClass().getName() + " Maximal k set to " + spinnerInteger_MaxK);
	}
    /** Executed whenever the {@link #spinInteger_RegMin} parameter changes. */
	protected void callbackRegMin() {
		if (spinnerInteger_RegMin >= spinnerInteger_RegMax - 2) {
			spinnerInteger_RegMin = spinnerInteger_RegMax - 2;
		}
		if(spinnerInteger_RegMin < 1) {
			spinnerInteger_RegMin = 1;
		}
		logService.info(this.getClass().getName() + " Regression Min set to " + spinnerInteger_RegMin);
	}
	/** Executed whenever the {@link #spinInteger_RegMax} parameter changes. */
	protected void callbackRegMax() {
		if (spinnerInteger_RegMax <= spinnerInteger_RegMin + 2) {
			spinnerInteger_RegMax = spinnerInteger_RegMin + 2;
		}		
		if (spinnerInteger_RegMax > spinnerInteger_MaxK) {
			spinnerInteger_RegMax = spinnerInteger_MaxK;
		}
		
		logService.info(this.getClass().getName() + " Regression Max set to " + spinnerInteger_RegMax);
	}
	
	/** Executed whenever the {@link #choiceRadioButt_WindowingType} parameter changes. */
	protected void callbackWindowingType() {
		logService.info(this.getClass().getName() + " Windowing type set to " + choiceRadioButt_WindowingType);
	}
	
	/** Executed whenever the {@link #choiceRadioButt_PowerSpecType} parameter changes. */
	protected void callbackPowerSpecType() {
		getAndValidateActiveDataset();
		int numOfK = getMaxK((int)width, (int)height);
		spinnerInteger_MaxK   = numOfK;
		spinnerInteger_RegMax = numOfK;	
		logService.info(this.getClass().getName() + " Power spectrum type set to " + choiceRadioButt_PowerSpecType);
		
	}
	
//	/** Executed whenever the {@link #booleanAddMirroredImages} parameter changes. */
//	protected void callbackAddMirroredImages() {
//		getAndValidateActiveDataset();
//		int numOfK = getMaxK((int)width, (int)height);
//		spinnerInteger_MaxK   = numOfK;
//		spinnerInteger_RegMax = numOfK;	
//		logService.info(this.getClass().getName() + " Add mirrored images set to " + booleanAddMirroredImages);
//		
//	}
	
	/** Executed whenever the {@link #booleanProcessImmediately} parameter changes. */
	protected void callbackProcessImmediately() {
		logService.info(this.getClass().getName() + " Process immediately set to " + booleanProcessImmediately);
	}
	
	/** Executed whenever the {@link #spinInteger_NumImageSlice} parameter changes. */
	protected void callbackNumImageSlice() {
		getAndValidateActiveDataset();
		if (spinnerInteger_NumImageSlice > numSlices){
			logService.info(this.getClass().getName() + " No more images available");
			spinnerInteger_NumImageSlice = (int)numSlices;
		}
		logService.info(this.getClass().getName() + " Image slice number set to " + spinnerInteger_NumImageSlice);
	}
	

	/** Executed whenever the {@link #buttonProcessSingelImage} button is pressed. */
	protected void callbackProcessSingleImage() {
		//prepare  executer service
		exec = Executors.newSingleThreadExecutor();
				
		//dlgProgress = new WaitingDialogWithProgressBar("<html>Computing FFT dimensions, please wait...<br>Open console window for further info.</html>");
		dlgProgress = new WaitingDialogWithProgressBar("Computing FFT dimensions, please wait... Open console window for further info.",
				logService, false, exec); //isCanceable = false, because no following method listens to exec.shutdown 
		dlgProgress.updatePercent("");
		dlgProgress.setBarIndeterminate(true);
		dlgProgress.setVisible(true);
	
	   	exec.execute(new Runnable() {
	        public void run() {
	    	    try {
	    	    	deleteExistingDisplays();
	        		getAndValidateActiveDataset();
	        		int sliceIndex = spinnerInteger_NumImageSlice - 1;
	        		logService.info(this.getClass().getName() + " Processing single image " + (sliceIndex + 1));
	        		processSingleInputImage(sliceIndex);
	        		dlgProgress.addMessage("Processing finished! Collecting data for table...");
	        		generateTableHeader();
	        		collectActiveResultAndShowTable(sliceIndex);
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
	
	/** Executed whenever the {@link #buttonProcessActiveImage} button is pressed. */
	protected void callbackProcessActiveImage() {
		//prepare  executer service
		exec = Executors.newSingleThreadExecutor();
				
		//dlgProgress = new WaitingDialogWithProgressBar("<html>Computing FFT dimensions, please wait...<br>Open console window for further info.</html>");
		dlgProgress = new WaitingDialogWithProgressBar("Computing FFT dimensions, please wait... Open console window for further info.",
				logService, false, exec); //isCanceable = false, because no following method listens to exec.shutdown 
		dlgProgress.updatePercent("");
		dlgProgress.setBarIndeterminate(true);
		dlgProgress.setVisible(true);

       	exec.execute(new Runnable() {
            public void run() {
        	    try {
        	    	deleteExistingDisplays();
            		getAndValidateActiveDataset();
            		int activeSliceIndex = getActiveImageIndex();
            		logService.info(this.getClass().getName() + " Processing active image " + (activeSliceIndex + 1));
            		processSingleInputImage(activeSliceIndex);
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
				
		//dlgProgress = new WaitingDialogWithProgressBar("<html>Computing Box dimensions, please wait...<br>Open console window for further info.</html>");
		dlgProgress = new WaitingDialogWithProgressBar("Computing Box dimensions, please wait... Open console window for further info.",
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
 		if (booleanProcessImmediately) callbackProcessSingleImage();
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
//			//	if (display.getName().equals("Name")) display.close(); //does not close correctly in Fiji, it is only not available any more
//			//}			
//			//List<ImageDisplay> listImgs = defaultImageDisplayService.getImageDisplays(); //Does not also close in Fiji
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
			if (doubleLogPlotList != null) {
				for (int l = 0; l < doubleLogPlotList.size(); l++) {
					doubleLogPlotList.get(l).setVisible(false);
					doubleLogPlotList.get(l).dispose();
					//doubleLogPlotList.remove(l);  /
				}
				doubleLogPlotList.clear();		
			}
		}
		if (optDeleteExistingTables) {
			Display<?> display;
			List<Display<?>> list = defaultDisplayService.getDisplays();
			for (int i = 0; i < list.size(); i++) {
				display = list.get(i);
				//System.out.println("display name: " + display.getName());
				if (display.getName().contains(tableName)) display.close();
			}			
		}
	}
	
	/**
	 * This method computes the maximal number of possible k's
	 */
	private int getMaxK(int width, int heigth) { //

		
		if (choiceRadioButt_PowerSpecType != null) { //during startup it is null
			//"Circular average", "Mean of line scans", "Integral of line scans"
			if      (choiceRadioButt_PowerSpecType.equals("Circular average")) {
	
				int dftWidth = 2; 
				int dftHeight = 2;
				while (dftWidth < width) {
					dftWidth = dftWidth * 2;
				}
				while (dftHeight < height) {
					dftHeight = dftHeight * 2;
				}
				
				numOfK = dftWidth * dftHeight; //Will be lowered later, after averaging
			
			}
			else if ((choiceRadioButt_PowerSpecType.equals("Mean of line scans")) || (choiceRadioButt_PowerSpecType.equals("Integral of line scans"))) {
				
				if (width < height) {
					numOfK = (int) height/2 - 1; //-1 because f=0 is not taken
				} else {
					numOfK = width/2-1;
				}
				//if (booleanAddMirroredImages) numOfK *= 2;  
				
			}
		} else { //during startup it is null
			if (width < height) {
				numOfK = (int) height/2-1;
			} else {
				numOfK = width/2-1;
			}
		}

		return numOfK;
	}
	
	/** This method takes the active image and computes results. 
	 *
	 */
	private void processSingleInputImage(int s) throws InterruptedException{
		long startTime = System.currentTimeMillis();
		resultValuesTable = new double[(int) numSlices][10];
		
		//convert to float values
		//Img<T> image = (Img<T>) dataset.getImgPlus();
		//mg<FloatType> imgFloat; // = opService.convert().float32((Img<T>)dataset.getImgPlus());

		RandomAccessibleInterval<?> rai = null;	
		if( (s==0) && (numSlices == 1) && (numDimensions == 2) ) { // for only one 2D image;
			rai =  (RandomAccessibleInterval<?>) datasetIn.getImgPlus();

		} else if ( (numSlices > 1) && (numDimensions == 3) ){ // for a stack of 2D images
			rai = (RandomAccessibleInterval<?>) Views.hyperSlice(datasetIn, 2, s);
		
		}

		//Compute regression parameters
		double[] regressionValues = process(rai, s);	
			//0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
			
		//set values for output table
		for (int i = 0; i < regressionValues.length; i++ ) {
				resultValuesTable[s][i] = regressionValues[i]; 
		}
		//Compute dimension
		double dim = Double.NaN;
		
		//dim = (3.0 * topDim + 2.0 + slope) / 2.0;
		//"Circular average", "Mean of line scans", "Integral of line scans"
		if      (choiceRadioButt_PowerSpecType.equals("Circular average")) {
			dim = (8.0 + regressionValues[1])/2.0;	
		}
		else if (choiceRadioButt_PowerSpecType.equals("Mean of line scans")) {
			dim = (7.0 + regressionValues[1])/2.0;
		}
		else if (choiceRadioButt_PowerSpecType.equals("Integral of line scans")) {
			dim = (6.0 + regressionValues[1])/2.0;	
		}
	
		resultValuesTable[s][1] = dim;
		
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
	
	/** This method loops over all input images and computes results. 
	 *
	 **/
	private void processAllInputImages() throws InterruptedException{
		
		long startTimeAll = System.currentTimeMillis();
		resultValuesTable = new double[(int) numSlices][10];
	
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
				//Compute regression parameters
				double[] regressionValues = process(rai, s);	
					//0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
				
				//set values for output table
				for (int i = 0; i < regressionValues.length; i++ ) {
					resultValuesTable[s][i] = regressionValues[i]; 
				}
				//Compute dimension
				double dim = Double.NaN;
			
				//dim = (3.0 * topDim + 2.0 + slope) / 2.0;
				//"Circular average", "Mean of line scans", "Integral of line scans"
				if       (choiceRadioButt_PowerSpecType.equals("Circular average")) {
					dim = (8.0 + regressionValues[1])/2.0;	
				}
				else if  (choiceRadioButt_PowerSpecType.equals("Mean of line scans")) {
					dim = (7.0 + regressionValues[1])/2.0;
				}
				else if (choiceRadioButt_PowerSpecType.equals("Integral of line scans")) {
					dim = (6.0 + regressionValues[1])/2.0;	
				}
			
				resultValuesTable[s][1] = dim;
				
				long duration = System.currentTimeMillis() - startTime;
				TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
				SimpleDateFormat sdf = new SimpleDateFormat();
				sdf.applyPattern("HHH:mm:ss:SSS");
				logService.info(this.getClass().getName() + " Elapsed time: "+ sdf.format(duration));
			}
		} //s
		statusService.showProgress(0, 100);
		statusService.clearStatus();
	
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
		
		long duration = System.currentTimeMillis() - startTimeAll;
		TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
		SimpleDateFormat sdf = new SimpleDateFormat();
		sdf.applyPattern("HHH:mm:ss:SSS");
		logService.info(this.getClass().getName() + " Elapsed processing time for all image(s): "+ sdf.format(duration));
	}
	
	/** Generates the table header {@code DefaultGenericTable} */
	private void generateTableHeader(){
		
		GenericColumn columnFileName       = new GenericColumn("File name");
		GenericColumn columnSliceName      = new GenericColumn("Slice name");
		IntColumn columnMaxMaxK            = new IntColumn("Max k");
		IntColumn columnRegMin             = new IntColumn("RegMin");
		IntColumn columnRegMax             = new IntColumn("RegMax");
		GenericColumn columnWindowingType  = new GenericColumn("Windowing type");
		GenericColumn columnPowerSpecType  = new GenericColumn("PowerSpec type");
		//GenericColumn columnAddMirrors     = new GenericColumn("Add mirrors");
		DoubleColumn columnDf              = new DoubleColumn("Df");
		DoubleColumn columnR2              = new DoubleColumn("R2");
		DoubleColumn columnStdErr          = new DoubleColumn("StdErr");
		
	    table = new DefaultGenericTable();
		table.add(columnFileName);
		table.add(columnSliceName);
		table.add(columnMaxMaxK);
		table.add(columnRegMin);
		table.add(columnRegMax);
		table.add(columnWindowingType);
		table.add(columnPowerSpecType);
		//table.add(columnAddMirrors);
		table.add(columnDf);
		table.add(columnR2);
		table.add(columnStdErr);
	}
	
	/** collects current result and shows table
	 *  @param int slice number of active image.
	 */
	private void collectActiveResultAndShowTable(int sliceNumber) {
	
		int regMin            = spinnerInteger_RegMin;
		int regMax            = spinnerInteger_RegMax;
		int numMaxK           = spinnerInteger_MaxK;
		String windowingType  = choiceRadioButt_WindowingType;
		String powerSpecType  = choiceRadioButt_PowerSpecType;	
		//boolean addMirrors    = booleanAddMirroredImages;
	
	    int s = sliceNumber;	
			//0 Intercept, 1 Dim, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared		
			//fill table with values
			table.appendRow();
			table.set("File name",   	 table.getRowCount() - 1, datasetName);	
			if (sliceLabels != null) 	 table.set("Slice name", table.getRowCount() - 1, sliceLabels[s]);
			table.set("Max k",      	 table.getRowCount()-1, numMaxK);	
			table.set("RegMin",      	 table.getRowCount()-1, regMin);	
			table.set("RegMax",      	 table.getRowCount()-1, regMax);	
			table.set("Windowing type",  table.getRowCount()-1, windowingType);	
			table.set("PowerSpec type",  table.getRowCount()-1, powerSpecType);	
			//table.set("Add mirrors",  	 table.getRowCount()-1, addMirrors);	
			table.set("Df",          	 table.getRowCount()-1, resultValuesTable[s][1]);
			table.set("R2",          	 table.getRowCount()-1, resultValuesTable[s][4]);
			table.set("StdErr",      	 table.getRowCount()-1, resultValuesTable[s][3]);		
		
		//Show table
		uiService.show(tableName, table);
	}
	
	/** collects all results and shows table */
	private void collectAllResultsAndShowTable() {
	
		int regMin            = spinnerInteger_RegMin;
		int regMax            = spinnerInteger_RegMax;
		int numMaxK      	  = spinnerInteger_MaxK;
		String windowingType  = choiceRadioButt_WindowingType;
		String powerSpecType  = choiceRadioButt_PowerSpecType;
		//boolean addMirrors    = booleanAddMirroredImages;

		//loop over all slices
		for (int s = 0; s < numSlices; s++){ //slices of an image stack
			//0 Intercept, 1 Dim, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared		
			//fill table with values
			table.appendRow();
			table.set("File name",	   	 table.getRowCount() - 1, datasetName);	
			if (sliceLabels != null)	 table.set("Slice name", table.getRowCount() - 1, sliceLabels[s]);
			table.set("Max k",    	     table.getRowCount()-1, numMaxK);	
			table.set("RegMin",      	 table.getRowCount()-1, regMin);	
			table.set("RegMax",      	 table.getRowCount()-1, regMax);	
			table.set("Windowing type",  table.getRowCount()-1, windowingType);	
			table.set("PowerSpec type",  table.getRowCount()-1, powerSpecType);	
			//table.set("Add mirrors",  	 table.getRowCount()-1, addMirrors);	
			table.set("Df",          	 table.getRowCount()-1, resultValuesTable[s][1]);
			table.set("R2",          	 table.getRowCount()-1, resultValuesTable[s][4]);
			table.set("StdErr",      	 table.getRowCount()-1, resultValuesTable[s][3]);		
		}
		//Show table
		uiService.show(tableName, table);
	}
							
	/** 
	 * Processing 
	 * @param ****************************************************************************************
	 * */
	private double[] process(RandomAccessibleInterval<?> rai, int plane) { //plane plane (Image) number
	
		int regMin            = spinnerInteger_RegMin;
		int regMax            = spinnerInteger_RegMax;
		int numMaxK           = spinnerInteger_MaxK;
		String windowingType  = choiceRadioButt_WindowingType;
		String powerSpecType  = choiceRadioButt_PowerSpecType;	
		//boolean addMirrors    = booleanAddMirroredImages;
		
		int numBands = 1;
		
		boolean optShowPlot    = booleanShowDoubleLogPlot;
		
		long width  = rai.dimension(0);
		long height = rai.dimension(1);
		
		String imageType = "8-bit";  //  "RGB"....
	
		double[] regressionParams = null;
		
		//Convert image to float
		//Img<T> image = (Img<T>) dataset.getImgPlus();
		//RandomAccessibleInterval<T> rai = (RandomAccessibleInterval<T>)dataset.getImgPlus();
		//IterableInterval ii = dataset.getImgPlus();
		//Img<FloatType> imgFloat = opService.convert().float32(ii);
		
		double[] totals = null; //= new double[numBoxes];
		// double[] totalsMax = new double[numBands]; //for binary images
		double[] eps = null; // = new int[numBoxes];
		
//		if (addMirrors) {		
//			createImgMirrored(rai);
//			rai =  (RandomAccessibleInterval<?>) imgMirrored;
//			width  = rai.dimension(0);
//			height = rai.dimension(1);
//			//uiService.show("Mirrored rai", rai);		
//		
//		} else {
//			//take rai as it comes
//			//uiService.show("Image rai", rai);	
//		}

		//In the order of increasing filter strength
		if (windowingType.equals("Rectangular")) {
			raiWindowed = windowingRectangular(rai);
		}
		else if (windowingType.equals("Bartlett")) {
			raiWindowed = windowingBartlett(rai);
		}
		else if (windowingType.equals("Hamming")) {
			raiWindowed = windowingHamming(rai);
		}
		else if (windowingType.equals("Hanning")) {
			raiWindowed = windowingHanning(rai);
		}
		else if (windowingType.equals("Blackman")) {
			raiWindowed = windowingBlackman(rai);
		}
		else if (windowingType.equals("Gaussian")) {
			raiWindowed = windowingGaussian(rai);
		}
		else if (windowingType.equals("Parzen")) {
			raiWindowed = windowingParzen(rai);
		}
			
		if (powerSpecType.equals("Circular average")) { //{"Circular average", "Mean of line scans", "Integral of line scans"},
			
//			ops filter fft seems to be a Hadamard transform rather than a true FFT
//			output size is automatically padded, so has rather strange dimensions.
//			output is vertically symmetric 
//			F= 0 is at (0.0) and (0,SizeY)
//			imgFloat = this.createImgFloat(raiWindowed);
//			RandomAccessibleInterval<C> raifft = opService.filter().fft(imgFloat);
//			
//			//This would also work with identical output 
//			ImgFactory<ComplexFloatType> factory = new ArrayImgFactory<ComplexFloatType>(new ComplexFloatType());
//			int numThreads = 6;
//			final FFT FFT = new FFT();
//			Img<ComplexFloatType> imgCmplx = FFT.realToComplex((RandomAccessibleInterval<R>) raiWindowed, factory, numThreads);

			//Using JTransform package
			//https://github.com/wendykierp/JTransforms
			//https://wendykierp.github.io/JTransforms/apidocs/
			//The sizes of both dimensions must be power of two.
			int dftWidth = 2; 
			int dftHeight = 2;
			while (dftWidth < width) {
				dftWidth = dftWidth * 2;
			}
			while (dftHeight < height) {
				dftHeight = dftHeight * 2;
			}
					
			//JTransform needs rows and columns swapped!!!!!
			int rows    = dftHeight;
			int columns = dftWidth;
			
			//JTransform needs rows and columns swapped!!!!!
			imgA = new double[rows][2*columns]; //Every frequency entry needs a pair of columns: for real and imaginary part
			Cursor<?> cursor = Views.iterable(raiWindowed).localizingCursor();
			long[] pos = new long[2];
			while (cursor.hasNext()) {
				cursor.fwd();
				cursor.localize(pos); 
				//JTransform needs rows and columns swapped!!!!!
				imgA[(int)pos[1]][(int)pos[0]] = ((DoubleType) cursor.get()).get();
			}
			
			//JTransform needs rows and columns swapped!!!!!
			DoubleFFT_2D FFT = new DoubleFFT_2D(rows, columns); //Here always the simple DFT width
			//dFFT.realForward(imgArrD);   //The first two columns are not symmetric and seem to be not right
			FFT.realForwardFull(imgA);   //The right part is not symmetric!!
			//Power image constructed later is also not exactly symmetric!!!!!
			
			
			//Optionally show FFT Real Imag image
			//************************************************************************************
//			ArrayImg<FloatType, ?> imgFFT = new ArrayImgFactory<>(new FloatType()).create(2*dftWidth, dftHeight); //always single 2D
//			Cursor<FloatType> cursorF = imgFFT.localizingCursor();
//			pos = new long[2];
//			while (cursorF.hasNext()){
//				cursorF.fwd();
//				cursorF.localize(pos);
//				//JTransform needs rows and columns swapped!!!!!
//				cursorF.get().set((float)imgArrD[(int)pos[1]][(int)pos[0]]);
//			}		
//			//Get min max
//			float min = Float.MAX_VALUE;
//			float max = -Float.MAX_VALUE;
//			float valF;
//			cursorF = imgFFT.cursor();
//			while (cursorF.hasNext()) {
//				cursorF.fwd();
//				valF = cursorF.get().get();
//				if (valF > max) max = valF;
//				if (valF < min) min = valF;
//			}	
//			//Rescale to 0...255
//			cursorF = imgFFT.cursor();
//			while (cursorF.hasNext()) {
//				cursorF.fwd();
//				cursorF.localize(pos);
//				cursorF.get().set(255f*(cursorF.get().get() - min)/(max - min));		
//			}	
//			uiService.show("FFT", imgFFT);	
			//************************************************************************************
			
			//Get power values
			final long[] origin1  = {0, 0};         	 //left top
			final long[] origin2  = {0, rows-1};    	 //left bottom
			final long[] origin3  = {columns-1, 0}; 	 //right top
			final long[] origin4  = {columns-1, rows-1}; //right bottom
			
			long[] posFFT = new long[2];
			long numOfK = rows * columns; 
											
			double[] powers       = new double[(int) numOfK];
			double[] allKs        = new double[(int) numOfK];
			double[] powersSorted = new double[(int) numOfK];
			double[] allKsSorted  = new double[(int) numOfK];
			double[] powersCircAverage = new double[(int) numOfK]; //will have some zeroes at the end
			double[] allKsCircAverage  = new double[(int) numOfK]; //will have some zeroes at the end
			
			//Optionally prepare Power image
//			ArrayImg<DoubleType, ?> imgPower = new ArrayImgFactory<>(new DoubleType()).create(dftWidth, dftHeight); //always single 2D
//			ArrayRandomAccess<DoubleType> raPower = imgPower.randomAccess();
//			pos = new long[2];

			int p = 0;
			for (int k1 = 0; k1 < rows/2; k1++) {
				for (int k2 = 0; k2 < columns/2; k2++) {
					posFFT[1] = k1;
					posFFT[0] = k2;
					allKs[p]  = Util.distance(origin1, posFFT); //Distance
					powers[p] = imgA[k1][2*k2]*imgA[k1][2*k2] + imgA[k1][2*k2+1]*imgA[k1][2*k2+1]; //Power	//(2*x)...Real parts   (2*x+1).... Imaginary parts
					p += 1;		
			
//					//write to imgPower
//					pos = new long[] {posFFT[1], posFFT[0]};
//					raPower.setPosition(pos);
//					raPower.get().set(Math.log(powers[p-1]));
				}
			}
			for (int k1 = rows/2; k1 < rows; k1++) {
				for (int k2 = 0; k2 < columns/2; k2++) {
					posFFT[1] = k1;
					posFFT[0] = k2;
					allKs[p]  = Util.distance(origin2, posFFT); //Distance
					powers[p] = imgA[k1][2*k2]*imgA[k1][2*k2] + imgA[k1][2*k2+1]*imgA[k1][2*k2+1]; //Power	//(2*x)...Real parts   (2*x+1).... Imaginary parts
					p += 1;

//					//write to imgPower
//					pos = new long[] {posFFT[1], posFFT[0]};
//					raPower.setPosition(pos);
//					raPower.get().set(Math.log(powers[p-1]));
				}
			}	
			for (int k1 = 0; k1 < rows/2; k1++) {
				for (int k2 = columns/2; k2 < columns; k2++) {
					posFFT[1] = k1;
					posFFT[0] = k2;
					allKs[p]  = Util.distance(origin3, posFFT); //Distance
					powers[p] = imgA[k1][2*k2]*imgA[k1][2*k2] + imgA[k1][2*k2+1]*imgA[k1][2*k2+1]; //Power	//(2*x)...Real parts   (2*x+1).... Imaginary parts
					p += 1;	

//					//write to imgPower
//					pos = new long[] {posFFT[1], posFFT[0]};
//					raPower.setPosition(pos);
//					raPower.get().set(Math.log(powers[p-1]));
				}
			}
			for (int k1 = rows/2; k1 < rows; k1++) {
				for (int k2 = columns/2; k2 < columns; k2++) {
					posFFT[1] = k1;
					posFFT[0] = k2;
					allKs[p]  = Util.distance(origin4, posFFT); //Distance
					powers[p] = imgA[k1][2*k2]*imgA[k1][2*k2] + imgA[k1][2*k2+1]*imgA[k1][2*k2+1]; //Power	//(2*x)...Real parts   (2*x+1).... Imaginary parts
					p += 1;

//					//write to imgPower
//					pos = new long[] {posFFT[1], posFFT[0]};
//					raPower.setPosition(pos);
//					raPower.get().set(Math.log(powers[p-1]));
				}
			}
				
//			//imgPower
//			//Get min max
//			double min = Double.MAX_VALUE;
//			double max = -Double.MAX_VALUE;
//			double valD;
//			ArrayCursor<DoubleType> cursorD = imgPower.cursor();
//			while (cursorD.hasNext()) {
//				cursorD.fwd();
//				valD = cursorD.get().get();
//				if (valD > max) max = valD;
//				if (valD < min) min = valD;
//			}	
//			//Rescale to 0...255
//			cursorD = imgPower.cursor();
//			while (cursorD.hasNext()) {
//				cursorD.fwd();
//				cursorD.localize(pos);
//				cursorD.get().set(255.0*(cursorD.get().get() - min)/(max - min));		
//			}	
//			uiService.show("Power", imgPower);
			
			//allKs and powers are unsorted!!
			//Sorting essential for limited RegStart RegEnd settings
			//Get the sorted index
			Integer[] idx = new Integer[allKs.length];
			for (int i = 0; i < idx.length; i++) idx[i] = i;

			// for (int i = 0; i < idx.length; i++ ) System.out.println("idx: " + idx[i]);
			Arrays.sort(idx, new Comparator<Integer>() {
				@Override
				public int compare(Integer idx1, Integer idx2) {
					return Double.compare(allKs[idx1], allKs[idx2]);
				}
			});
			// for (int i = 0; i < idx.length; i++ ) System.out.println("idx: "+ idx[i]);

			//Get sorted vectors
			powersSorted = new double[powers.length];
			allKsSorted  = new double[allKs.length];
			for (int i = 0; i < idx.length; i++) {
				powersSorted[i] = powers[idx[i]];
				allKsSorted[i]  = allKs[idx[i]]; // idx is sorted
			}	
		
			//Go through and average power for same k values
			//Then, number of data points will be lower
			double powMean = 0;
			int numEqualK = 0; // number of equal k values
			int i = 0; //index for new vector;
			int ii = 0; //index for old vector
			double kValue = allKsSorted[0];
			while (i < allKsSorted.length) { //will not be reached
				while (allKsSorted[ii] == kValue) {
					//if  (powersSorted[ii] != 0) {
						powMean += powersSorted[ii];
						numEqualK += 1;	
					//}	
					ii  += 1;
					if (ii >= allKsSorted.length) break;
				}
				powMean = powMean /numEqualK;
				powersCircAverage[i] = powMean;
				allKsCircAverage[i]  = kValue;
				//Reset for the next k
				if (ii < allKsSorted.length - 1) {	
					numEqualK = 0;
					kValue = allKsSorted[ii];
					powMean = 0.0;
					i += 1; //index for next mean value
				} else {
					break;
				}
			}
			
			//find largest k to be taken
			double maxK =0;; //an actual maximal k value  // is not an integer number
			int maxKIdx=0;
		
			for (int a = 0; a < allKsCircAverage.length; a++) {
				if (allKsCircAverage[a] >= maxK) {
					maxK = allKsCircAverage[a];
					maxKIdx = a;
				}
			}
				
			totals = new double[maxKIdx+1-1]; //-1 because f=0 is not taken 
			eps    = new double[maxKIdx+1-1];
			for (int k = 0; k < eps.length; k++) { 
				totals[k] = powersCircAverage[k+1]; //do not take f=0 ;
				eps[k]    = allKsCircAverage[k+1];  //do not take f=0;
			}
		
//			//However, take all data points for regression - the whole frequency spectrum
//			totals = new double[allKsSorted.length];
//			eps    = new double[allKsSorted.length];
//			for (int k = 0; k < eps.length; k++) { //k=0 incl. frequency = 0
//				totals[k] = powersSorted[k] + 0.00000001; //to get rid of zeroes
//				eps[k]    = allKsSorted[k] + 1; //to get rid of zeroes
//			}
			
		}
		
		if (powerSpecType.equals("Mean of line scans")) { //{"Circular average", "Mean of line scans", "Integral of line scans"},
		
			ra = raiWindowed.randomAccess();
			int maxK;
			if (width < height) maxK = (int) (width/2);  //Take only half of the spectrum //Symmetric DFT  is half the input length
			else maxK = (int) (height/2);
			
			double[] power;
			double[] powerMean = new double[maxK]; //power has size ^2
		
			//all rows
			for (int h = 0; h < height; h++) { 
				//System.out.println("FFTDim h "  + h);
				sequence = new double[(int) width];
				for (int w = 0; w < width; w++) { // one row
					ra.setPosition(w, 0);
					ra.setPosition(h, 1); //row at position h
					sequence[w] = ((DoubleType) ra.get()).get();
				}
				//power = calcDFTPower(); // very slow 
				power = calcDFTPowerWithApache(); //of variable sequence
				//power = calcDFTPowerWithJTransform(); //as fast as Apache and quite identical results for power^2 sized images
				for (int k = 0; k < (powerMean.length); k++ ) {
					powerMean[k] += power[k]; 
				}
			}	
			//all columns
			for (int w = 0; w < width; w++) {
				//System.out.println("FFTDim w "  + w);
				sequence = new double[(int) height];
				for (int h = 0; h < height; h++) { // one row
					ra.setPosition(w, 0); // column at position w
					ra.setPosition(h, 1);
					sequence[h] = ((DoubleType) ra.get()).get();
				}
				//power = calcDFTPower(); //very slow //of variable sequence
				power = calcDFTPowerWithApache(); //of variable sequence    /Symmetric DFT  is half the input length
				//power = calcDFTPowerWithJTransform(); //as fast as Apache and quite identical results for power^2 sized images
				for (int k = 0; k < (powerMean.length); k++ ) {//Symmetric DFT  is half the input length
					powerMean[k] += power[k]; 
				}
			}	
			//mean power spectrum
			for (int k = 0; k < powerMean.length; k++ ) {
				powerMean [k] = powerMean[k]/(width+height); 
			}

			//fill up totals with powerMean values
			totals = new double[powerMean.length - 1];  //-1 k=0 zero frequency is skipped, does not fit the linear regression 
			eps    = new double[powerMean.length - 1];  //-1 k=0 zero frequency is skipped, does not fit the linear regression 
			for (int n = 0 ; n < totals.length; n++) {
				totals[n] = powerMean[n+1]; //k=0 zero frequency is skipped, does not fit the linear regression 
				eps[n]    = n + 1; 			//k=0 zero frequency is skipped, 
			}
				
		}
		
		else if (powerSpecType.equals("Integral of line scans")) { //{"Circular average", "Mean of line scans", "Integral of line scans"},
		//Just the same as "Mean of line scans", but additionally a final integration of the mean PS

			ra = raiWindowed.randomAccess();
			int maxK;
			if (width < height) maxK = (int) (width/2);  //Take only half of the spectrum //Symmetric DFT  is half the input length
			else maxK = (int) (height/2);
			
			double[] power;
			double[] powerMean = new double[maxK]; //power has size
			double[] powerIntegrated;
			
			//all rows
			for (int h = 0; h < height; h++) { 
				//System.out.println("FFTDim h "  + h);
				sequence = new double[(int) width];
				for (int w = 0; w < width; w++) { // one row
					ra.setPosition(w, 0);
					ra.setPosition(h, 1); //row at position h
					sequence[w] = ((DoubleType) ra.get()).get();
				}
				//power = calcDFTPower(); // very slow 
				power = calcDFTPowerWithApache(); //of variable sequence
				for (int k = 0; k < (powerMean.length); k++ ) {
					powerMean[k] += power[k]; 
				}
			}	
			//all columns
			for (int w = 0; w < width; w++) {
				//System.out.println("FFTDim w "  + w);
				sequence = new double[(int) height];
				for (int h = 0; h < height; h++) { // one row
					ra.setPosition(w, 0); // column at position w
					ra.setPosition(h, 1);
					sequence[h] = ((DoubleType) ra.get()).get();
				}
				//power = calcDFTPower(); //very slow //of variable sequence
				power = calcDFTPowerWithApache(); //of variable sequence
				for (int k = 0; k < (powerMean.length); k++ ) {//Symmetric DFT  is half the input length
					powerMean[k] += power[k]; 
				}
			}	
			//mean power spectrum
			for (int k = 0; k < powerMean.length; k++ ) {
				powerMean [k] = powerMean[k]/(width+height); 
			}

			//Integrate
			powerIntegrated = new double[powerMean.length];
			for (int k = 0; k < powerMean.length; k++ ) {
				for (int kk = k; kk < powerMean.length; kk++ ) {
					powerIntegrated[k] += powerMean[kk]; 
				}
			}
			
			//fill up totals with powerMean values
			totals = new double[powerIntegrated.length - 1];  //-1 k=0 zero frequency is skipped, does not fit the linear regression 
			eps    = new double[powerIntegrated.length - 1];  //-1 k=0 zero frequency is skipped, does not fit the linear regression 
			for (int n = 0 ; n < totals.length; n++) {
				totals[n] = powerIntegrated[n+1]; //k=0 zero frequency is skipped, does not fit the linear regression 
				eps[n]    = n + 1;                //k=0 zero frequency is skipped, 
			}		
		}
		

		//*********************************************************************************************
	
		//Computing log values for plot 
		//Change sequence of entries to start with a pixel
		double[] lnTotals = new double[totals.length];
		double[] lnEps    = new double[eps.length];
		for (int n = 0; n < totals.length; n++) {
			if (totals[n] <= 0) {
				lnTotals[n] = -Double.MAX_VALUE;
			} else if (Double.isNaN(totals[n])) {
				lnTotals[n] = Double.NaN;
			} else {
				lnTotals[n] = Math.log(totals[n]); //
			}
			if (eps[n] <= 0) {
				lnEps[n] = -Double.MAX_VALUE;
			} else if (Double.isNaN(eps[n])) {
				lnEps[n] = Double.NaN;
			} else {
				lnEps[n] = Math.log(eps[n]); //
			}
			//lnEps[n] = Math.log(eps[n]);
			//logService.info(this.getClass().getName() + " n:" + n + " eps:  " + eps[n]);
			//logService.info(this.getClass().getName() + " n:" + n + " lnEps:  "+  lnEps[n]);
			//logService.info(this.getClass().getName() + " n:" + n + " totals[n][b]: " + totals[n]);	
		}
			
		if (regMax > lnEps.length) {
			regMax = lnEps.length;
			this.spinnerInteger_RegMax = regMax;
		}
		if (regMin >= regMax) {
			regMin = regMax - 3;
			this.spinnerInteger_RegMin = regMin;
		}
		if (regMin < 1) {
			regMin = 1;
			this.spinnerInteger_RegMin = regMin;
		}
		
		//Create double log plot
		boolean isLineVisible = false; //?
		for (int b = 0; b < numBands; b++) { // mehrere Bands
		
			// System.out.println("FractalDimensionFFT: dataY: "+ dataY);
			// System.out.println("FractalDimensionFFT: dataX: "+ dataX);
			if (optShowPlot) {			
				String preName = "";
				if (numSlices > 1) {
					preName = "Slice-"+String.format("%03d", plane) +"-";
				}
				RegressionPlotFrame doubleLogPlot = DisplayRegressionPlotXY(lnEps, lnTotals, isLineVisible, "Double Log Plot - FFT Dimension", 
						preName + datasetName, "ln(k)", "ln(Power)", "",
						regMin, regMax);
				doubleLogPlotList.add(doubleLogPlot);
			}
			
			// Compute regression
			LinearRegression lr = new LinearRegression();
			regressionParams = lr.calculateParameters(lnEps, lnTotals, regMin, regMax);
			//0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
		}
		
		return regressionParams;
		//Output
		//uiService.show(tableName, table);
		//result = ops.create().img(image, new FloatType());
		//table
	}

	/**
	 * 
	 * This methods creates an Img<UnsignedByteType>
	 */
	private Img<UnsignedByteType > createImgMirrored(RandomAccessibleInterval<?> rai){ //rai must always be a single 2D plane
		
		imgMirrored = new ArrayImgFactory<>(new UnsignedByteType()).create(rai.dimension(0)*2, rai.dimension(1)*2); //doubled size!  //always single 2D
		Cursor<UnsignedByteType> cursor = imgMirrored.localizingCursor();
		final long[] pos = new long[imgMirrored.numDimensions()];
		RandomAccess<RealType<?>> ra = (RandomAccess<RealType<?>>) rai.randomAccess();
		while (cursor.hasNext()){
			cursor.fwd();
			cursor.localize(pos);
			
			//System.out.println("FFT dimension Alt pos[0] " + pos[0] + "    pos[1] " + pos[1]);
			//Position just to the right
			if (pos[0] >= width && pos[1] < height) pos[0] = (width-1) - (pos[0]-width);	
			//Position just under the bottom
			if (pos[0] < width && pos[1] >= height) pos[1] = (height-1) - (pos[1]-height);		
			//Position  under the right bottom corner
			if (pos[0] >= width && pos[1] >= height) {
				pos[0] = (width-1)  - (pos[0]-width);
				pos[1] = (height-1) - (pos[1]-height);
			}	
			//System.out.println("FFT dimension Neu pos[0] " + pos[0] + "    pos[1] " + pos[1]);	
			ra.setPosition(pos);	
			cursor.get().setReal(ra.get().getRealFloat());
		}
		return imgMirrored;
	}
	
	
	/**
	 * This method does Rectangular windowing
	 * See also www.labbookpages.co.uk/audio/firWindowing.html#windows
	 * @param  rai
	 * @return windowed rai
	 */
	private RandomAccessibleInterval<DoubleType> windowingRectangular (RandomAccessibleInterval<?> rai) {
		int width  = (int) rai.dimension(0);
		int height = (int) rai.dimension(1);	
		raiWindowed = new ArrayImgFactory<>(new DoubleType()).create(width, height); //always single 2D
		
		double weight = 1.0;
	
		Cursor<DoubleType> cursorD = Views.iterable(raiWindowed).localizingCursor();
		long[] pos = new long[raiWindowed.numDimensions()];
		RandomAccess<RealType<?>> ra = (RandomAccess<RealType<?>>) rai.randomAccess();
		while (cursorD.hasNext()){
			cursorD.fwd();
			cursorD.localize(pos);
			ra.setPosition(pos);
			cursorD.get().setReal(ra.get().getRealDouble()*weight); //simply a copy
		} 
	    return raiWindowed; 
	}
	
	/**
	 * This method does Bartlett windowing
	 * See Burge Burge, Digital Image Processing, Springer
	 * @param  rai
	 * @return windowed rai
	 */
	private RandomAccessibleInterval<DoubleType> windowingBartlett (RandomAccessibleInterval<?> rai) {
		
		int width  = (int) rai.dimension(0);
		int height = (int) rai.dimension(1);	
		raiWindowed = new ArrayImgFactory<>(new DoubleType()).create(width, height); //always single 2D
		
		double r_u;
		double r_v;
		double r_uv;
		double weight;
		
		//Create a full weight window
//		double[][] window = new double[width][height];
//		for (int u = 0; u < width; u++) {
//			for (int v = 0; v < height; v++) {
//				r_u = 2.0*u/width-1.0;
//				r_v = 2.0*v/height-1.0;
//				r_uv = Math.sqrt(r_u*r_u + r_v*r_v);
//				if ((r_uv >= 0) && (r_uv <=1)) window[u][v] = 1 - r_uv;
//				else window[u][v] = 0.0;
//			}
//		}
		
		Cursor<DoubleType> cursorD = Views.iterable(raiWindowed).localizingCursor();
		long[] pos = new long[raiWindowed.numDimensions()];
		RandomAccess<RealType<?>> ra = (RandomAccess<RealType<?>>) rai.randomAccess();
		while (cursorD.hasNext()){
			cursorD.fwd();
			cursorD.localize(pos);
			ra.setPosition(pos);
			r_u = 2.0*(pos[0]+0.5)/width -1.0;   //+0.5 so that the maximum is really centered
			r_v = 2.0*(pos[1]+0.5)/height-1.0;
			r_uv = Math.sqrt(r_u*r_u + r_v*r_v);
			if ((r_uv >= 0) && (r_uv <=1)) weight = 1 - r_uv;
			else weight = 0.0;	
			//if(pos[1] == 1) System.out.println("Bartlett windowing weight " + pos[0] +" "+pos[1]+"  "+ weight);
			cursorD.get().setReal(ra.get().getRealDouble()*weight);
		} 
	    return raiWindowed; 
	}

	/**
	 * This method does Hamming windowing
	 * See also www.labbookpages.co.uk/audio/firWindowing.html#windows
	 * See Burge Burge, Digital Image Processing, Springer
	 * @param  rai
	 * @return windowed rai
	 */
	private RandomAccessibleInterval<DoubleType> windowingHamming (RandomAccessibleInterval<?> rai) {
	
		int width  = (int) rai.dimension(0);
		int height = (int) rai.dimension(1);	
		raiWindowed = new ArrayImgFactory<>(new DoubleType()).create(width, height); //always single 2D
		
		double r_u;
		double r_v;
		double r_uv;
		double weight;
		
		Cursor<DoubleType> cursorD = Views.iterable(raiWindowed).localizingCursor();
		long[] pos = new long[raiWindowed.numDimensions()];
		RandomAccess<RealType<?>> ra = (RandomAccess<RealType<?>>) rai.randomAccess();
		while (cursorD.hasNext()){
			cursorD.fwd();
			cursorD.localize(pos);
			ra.setPosition(pos);
			r_u = 2.0*(pos[0]+0.5)/width -1.0;   //+0.5 so that the maximum is really centered
			r_v = 2.0*(pos[1]+0.5)/height-1.0;
			r_uv = Math.sqrt(r_u*r_u + r_v*r_v);
			if ((r_uv >= 0) && (r_uv <=1)) weight = 0.54 + 0.46*Math.cos(Math.PI*(r_uv)); //== 0.54 - 0.46*Math.cos(Math.PI*(1.0-r_uv));
			else weight = 0.0;	
			//if(pos[1] == 1) System.out.println("Hamming windowing weight " + pos[0] +" "+pos[1]+"  "  + weight);
			cursorD.get().setReal(ra.get().getRealDouble()*weight);
		} 
	    return raiWindowed; 
	}
	
	/**
	 * This method does Hanning windowing
	 * See also www.labbookpages.co.uk/audio/firWindowing.html#windows
	 * See Burge Burge, Digital Image Processing, Springer
	 * @param  rai
	 * @return windowed rai
	 */
	private RandomAccessibleInterval<DoubleType> windowingHanning (RandomAccessibleInterval<?> rai) {
		
		int width  = (int) rai.dimension(0);
		int height = (int) rai.dimension(1);	
		raiWindowed = new ArrayImgFactory<>(new DoubleType()).create(width, height); //always single 2D
		
		double r_u;
		double r_v;
		double r_uv;
		double weight = 0;
		
		Cursor<DoubleType> cursorD = Views.iterable(raiWindowed).localizingCursor();
		long[] pos = new long[raiWindowed.numDimensions()];
		RandomAccess<RealType<?>> ra = (RandomAccess<RealType<?>>) rai.randomAccess();
		while (cursorD.hasNext()){
			cursorD.fwd();
			cursorD.localize(pos);
			ra.setPosition(pos);
			r_u = 2.0*(pos[0]+0.5)/width -1.0;   //+0.5 so that the maximum is really centered
			r_v = 2.0*(pos[1]+0.5)/height-1.0;
			r_uv = Math.sqrt(r_u*r_u + r_v*r_v);
			if ((r_uv >= 0) && (r_uv <=1)) {
				//weight = 0.5*Math.cos(Math.PI*r_uv+1); //Burge Burge  gives negative weights!
				weight = 0.5 + 0.5*Math.cos(Math.PI*(r_uv)); //== 0.5 - 0.5*Math.cos(Math.PI*(1-r_uv));
			}
			else weight = 0.0;	
			//if(pos[1] == 1) System.out.println("Hanning windowing weight " + pos[0] +" "+pos[1]+"  " + weight);
			cursorD.get().setReal(ra.get().getRealDouble()*weight);
		} 
	    return raiWindowed; 
	}
	
	/**
	 * This method does Blackman windowing
	 * See also www.labbookpages.co.uk/audio/firWindowing.html#windows
	 * See Burge Burge, Digital Image Processing, Springer
	 * @param  rai
	 * @return windowed rai
	 */
	private RandomAccessibleInterval<DoubleType> windowingBlackman (RandomAccessibleInterval<?> rai) {
		int width  = (int) rai.dimension(0);
		int height = (int) rai.dimension(1);	
		raiWindowed = new ArrayImgFactory<>(new DoubleType()).create(width, height); //always single 2D
		
		double r_u;
		double r_v;
		double r_uv;
		double weight;
		
		Cursor<DoubleType> cursorD = Views.iterable(raiWindowed).localizingCursor();
		long[] pos = new long[raiWindowed.numDimensions()];
		RandomAccess<RealType<?>> ra = (RandomAccess<RealType<?>>) rai.randomAccess();
		while (cursorD.hasNext()){
			cursorD.fwd();
			cursorD.localize(pos);
			ra.setPosition(pos);
			r_u = 2.0*(pos[0]+0.5)/width -1.0;   //+0.5 so that the maximum is really centered
			r_v = 2.0*(pos[1]+0.5)/height-1.0;
			r_uv = Math.sqrt(r_u*r_u + r_v*r_v);
			//if ((r_uv >= 0) && (r_uv <=1)) weight = 0.42 - 0.5*Math.cos(Math.PI*(1.0-r_uv)) + 0.08*Math.cos(2.0*Math.PI*(1.0-r_uv));
			if ((r_uv >= 0) && (r_uv <=1)) weight = 0.42 - 0.5*Math.cos(Math.PI*(1.0-r_uv)) + 0.08*Math.cos(2.0*Math.PI*(1.0-r_uv));
			else weight = 0.0;	
			//if(pos[1] == 1) System.out.println("Blackman windowing weight " + pos[0] +" "+pos[1]+"  "  + weight);
			cursorD.get().setReal(ra.get().getRealDouble()*weight);
		} 
	    return raiWindowed; 
	}
	
	/**
	 * This method does Gaussian windowing
	 * See also www.labbookpages.co.uk/audio/firWindowing.html#windows
	 * See Burge Burge, Digital Image Processing, Springer
	 * @param  rai
	 * @return windowed rai
	 */
	private RandomAccessibleInterval<DoubleType> windowingGaussian (RandomAccessibleInterval<?> rai) {
		int width  = (int) rai.dimension(0);
		int height = (int) rai.dimension(1);	
		raiWindowed = new ArrayImgFactory<>(new DoubleType()).create(width, height); //always single 2D
		
		double r_u;
		double r_v;
		double r_uv;
		double weight = 0;
		double sigma  = 0.3;
		double sigma2 = sigma*sigma;
		
		Cursor<DoubleType> cursorD = Views.iterable(raiWindowed).localizingCursor();
		long[] pos = new long[raiWindowed.numDimensions()];
		RandomAccess<RealType<?>> ra = (RandomAccess<RealType<?>>) rai.randomAccess();
		while (cursorD.hasNext()){
			cursorD.fwd();
			cursorD.localize(pos);
			ra.setPosition(pos);
			r_u = 2.0*(pos[0]+0.5)/width -1.0;   //+0.5 so that the maximum is really centered
			r_v = 2.0*(pos[1]+0.5)/height-1.0;
			r_uv = Math.sqrt(r_u*r_u + r_v*r_v);
			weight = Math.exp(-(r_uv*r_uv)/(2.0*sigma2));
			//if(pos[1] == 1) System.out.println("Gaussian windowing weight " + pos[0] +" "+pos[1]+"  "  + weight);
			cursorD.get().setReal(ra.get().getRealDouble()*weight);
		} 
	    return raiWindowed; 
	}

	/**
	 * This method does Parzen windowing
	 * See also www.labbookpages.co.uk/audio/firWindowing.html#windows
	 * See Burge Burge, Digital Image Processing, Springer
	 * @param  rai
	 * @return windowed rai
	 */
	private RandomAccessibleInterval<DoubleType> windowingParzen (RandomAccessibleInterval<?> rai) {
	
		int width  = (int) rai.dimension(0);
		int height = (int) rai.dimension(1);	
		raiWindowed = new ArrayImgFactory<>(new DoubleType()).create(width, height); //always single 2D
		
		double r_u;
		double r_v;
		double r_uv;
		double weight;
		
		Cursor<DoubleType> cursorD = Views.iterable(raiWindowed).localizingCursor();
		long[] pos = new long[raiWindowed.numDimensions()];
		RandomAccess<RealType<?>> ra = (RandomAccess<RealType<?>>) rai.randomAccess();
		while (cursorD.hasNext()){
			cursorD.fwd();
			cursorD.localize(pos);
			ra.setPosition(pos);
			r_u = 2.0*(pos[0]+0.5)/width -1.0;   //+0.5 so that the maximum is really centered
			r_v = 2.0*(pos[1]+0.5)/height-1.0;
			r_uv = Math.sqrt(r_u*r_u + r_v*r_v);
			//if      ((r_uv >= 0) && (r_uv <0.5)) weight = 1.0 - 6.0*Math.pow(r_uv, 2) + 6.0*Math.pow(r_uv, 3); //Burge Burge gives double peaks, seems to be wrong
			if      ((r_uv >= 0) && (r_uv <0.5)) weight = 1.0 - 6.0*Math.pow(r_uv, 2)*(1-r_uv);
			else if ((r_uv >= 0.5) && (r_uv <1)) weight = 2.0*Math.pow(1-r_uv, 3);
			else    weight = 0.0;	
			//if(pos[1] == 1) System.out.println("Parzen windowing weight " + pos[0] +" "+pos[1]+"  "  + weight);
			cursorD.get().setReal(ra.get().getRealDouble()*weight);
		} 
	    return raiWindowed; 
	}
	
	/**
	 * This method calculates the power spectrum of a 1D signal.
	 * Very slow
	 * @param sequence
	 * @return the DFT power spectrum
	 */
	private double[] calcDFTPower() {
	
		int length = sequence.length;
		double[]signalPower = new double[length];
		double sumReal = 0;
		double sumImag = 0;
		double norm = 0.0;
		
		for (int k = 0; k < length; k++) { //spectrum is symmetric around length/2
			sumReal = 0;
			sumImag = 0;
			for (int n = 0; n < length; n++) { //input points
				//double cos = Math.cos(2*Math.PI * n * k / length);
				//double sin = Math.sin(2*Math.PI * n * k / length);		
				sumReal +=  sequence[n] * Math.cos(2.0*Math.PI * n * k / length);
				sumImag += -sequence[n] * Math.sin(2.0*Math.PI * n * k / length);		
			}
			signalPower[k] = sumReal*sumReal+sumImag*sumImag; 
		}
//		for (int k = 0; k < length; k++) {
//			norm += signalPower[k];
//		}
//		
//		for (int k = 0; k < length; k++) {
//			signalPower[k] = signalPower[k] / norm;
//		}
		return signalPower;
	}
	
	/**
	 * This method calculates the power spectrum of a 1D signal using Apache method
	 * This is 12 times faster (without padding with zeroes)
	 * @param sequence
	 * @return the DFT power spectrum
	 */
	private double[] calcDFTPowerWithApache() {
	
		//FFT needs power of two
		if (!isPowerOfTwo(sequence.length)) {
			sequence = addZerosUntilPowerOfTwo(sequence);
		}	
		FastFourierTransformer transformer = new FastFourierTransformer(DftNormalization.STANDARD);
	    Complex[] complex = transformer.transform(sequence, TransformType.FORWARD);
	    double[] signalPower = new double[complex.length];
	    for (int k =0; k < complex.length; k++) {
	    	signalPower[k] = complex[k].getReal()*complex[k].getReal() + complex[k].getImaginary()*complex[k].getImaginary();  
	    }
		return signalPower;
	}
	
	/**
	 * This method calculates the power spectrum of a 1D signal using JTransform
	 * @param sequence
	 * @return the DFT power spectrum
	 */
	private double[] calcDFTPowerWithJTransform() {
		
		//as fast as Apache and quite identical results for power^2 sized images
	
//		//FFT needs not power of two
//		if (!isPowerOfTwo(sequence.length)) {
//			sequence = addZerosUntilPowerOfTwo(sequence);
//		}	
		
		 //n..of points
	    int n = sequence.length;
	    
		DoubleFFT_1D dFFT = new DoubleFFT_1D(n);
		dFFT.realForward(sequence);
	
	    double[] signalPower = new double[n];
	    
		
		//According to
		//Apidocs of JTransform  DoubleFFT_2D.realForward(double[][] a])
		//https://wendykierp.github.io/JTransforms/apidocs/
	   
	    if (n % 2 == 0) { //even 	
		    //a[2*k]   = Re[k], 0<=k<n/2
		    //a[2*k+1] = Im[k], 0< k<n/2 
		    //a[1]     = Re[n/2]
	    
	    	//add real parts^2
		    for (int k = 0; k < n/2; k++) {
		    	signalPower[k] += sequence[2*k]*sequence[2*k];
		    }
		    //add imaginary parts^2
		    for (int k = 1; k < n/2; k++) {
		    	signalPower[k] += sequence[2*k+1]*sequence[2*k+1];
		    }
		    signalPower[n/2] += sequence[1]*sequence[1];
		    
	    } else { //odd
	    	//a[2*k]   = Re[k], 0<=k<(n+1)/2 
	    	//a[2*k+1] = Im[k], 0 <k<(n-1)/2
	    	//a[1] = Im[(n-1)/2] 	
		    for (int k = 0; k < (n+1)/2; k++) {
		    	signalPower[k] += sequence[2*k]*sequence[2*k];  //add real parts^2
		    }
		    for (int k = 1; k < (n-1)/2; k++) { // add imaginary parts^2
		    	signalPower[k] += sequence[2*k+1]*sequence[2*k+1];	    	
		    }
		    signalPower[(n-1)/2] += sequence[1]*sequence[1];
	    }
	    
		return signalPower;
	}
	
	/**
	 * This method computes if a number is a power of 2
	 * 
	 * @param number
	 * @return
	 */
	public boolean isPowerOfTwo(int number) {
	    if (number % 2 != 0) {
	      return false;
	    } else {
	      for (int i = 0; i <= number; i++) {
	        if (Math.pow(2, i) == number) return true;
	      }
	    }
	    return false;
	 }
	
	/**
	 * This method increases the size of a signal to the next power of 2 
	 * 
	 * @param signal
	 * @return
	 */
	public double[] addZerosUntilPowerOfTwo (double[] signal) {
		int p = 1;
		double[] newSignal;
		int oldLength = signal.length;
		while (Math.pow(2, p) < oldLength) {
			p = p +1;
	    }
		newSignal = new double[(int) Math.pow(2, p)];
		for (int i = 0; i < oldLength; i++) {
			newSignal[i] = signal[i];
		}
		return newSignal;
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
	 * Displays a regression plot in a separate window.
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
	private RegressionPlotFrame DisplayRegressionPlotXY(double[] dataX, double[] dataY, boolean isLineVisible,
			String frameTitle, String plotLabel, String xAxisLabel, String yAxisLabel, String legendLabel, int regMin, int regMax) {
		// jFreeChart
		RegressionPlotFrame pl = new RegressionPlotFrame(dataX, dataY, isLineVisible, frameTitle, plotLabel, xAxisLabel,
				yAxisLabel, legendLabel, regMin, regMax);
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
		
		imgUnsignedByte = new ArrayImgFactory<>(new UnsignedByteType()).create(rai.dimension(0), rai.dimension(1)); //always single 2D
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
		
		imgFloat = new ArrayImgFactory<>(new FloatType()).create(rai.dimension(0), rai.dimension(1)); //always single 2D
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
		//ij.command().run(Img2DFractalDimensionFFT.class, true).get().getOutput("image");
		ij.command().run(Img2DFractalDimensionFFT.class, true);
	}
}

