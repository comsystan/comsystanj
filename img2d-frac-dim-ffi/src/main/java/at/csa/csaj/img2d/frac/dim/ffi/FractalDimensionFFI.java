/*-
 * #%L
 * Project: ImageJ plugin for computing the fractal fragmentation index FFI
 * File: FractalDimensionFFI.java
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

package at.csa.csaj.img2d.frac.dim.ffi;

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
import net.imglib2.algorithm.morphology.Erosion;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
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
import io.scif.DefaultImageMetadata;
import io.scif.MetaTable;

/**
 * A {@link Command} plugin computing
 * <the fractal fragmentation index </a>
 * of an image.
 */
@Plugin(type = InteractiveCommand.class,
	headless = true,
	label = "FFI",
	menu = {
	@Menu(label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT, mnemonic = MenuConstants.PLUGINS_MNEMONIC),
	@Menu(label = "ComsystanJ"),
	@Menu(label = "2D Image"),
	@Menu(label = "Fractal fragmentation index", weight = 22)})
public class FractalDimensionFFI<T extends RealType<T>> extends InteractiveCommand implements Command, Previewable { //non blocking GUI
//public class FractalDimensionFFI<T extends RealType<T>> implements Command {	//modal GUI
	
	private static final String PLUGIN_LABEL            = "<html><b>Computes fractal fragmentation index</b></html>";
	private static final String SPACE_LABEL             = "";
	private static final String REGRESSION_LABEL        = "<html><b>Regression parameters</b></html>";
	private static final String METHODOPTIONS_LABEL     = "<html><b>Method options</b></html>";
	private static final String BACKGROUNDOPTIONS_LABEL = "<html><b>Background option</b></html>";
	private static final String DISPLAYOPTIONS_LABEL    = "<html><b>Display options</b></html>";
	private static final String PROCESSOPTIONS_LABEL    = "<html><b>Process options</b></html>";
	
	private static Img<FloatType> imgFloat; 
	private static Img<FloatType> imgSubSampled;
	private static Img<FloatType> imgTemp;
	private static RandomAccessibleInterval<?> raiBox;
	private static RandomAccess<UnsignedByteType> ra;
	private static Cursor<?> cursor = null;
	private static Cursor<FloatType> cursorF = null;
	private static String datasetName;
	private static String[] sliceLabels;
	private static long width  = 0;
	private static long height = 0;
	private static long numDimensions = 0;
	private static long numSlices  = 0;
	private static int  numBoxes = 0;
	private static ArrayList<RegressionPlotFrame> doubleLogPlotList = new ArrayList<RegressionPlotFrame>();
	private static double[][] resultValuesTable; //first column is the image index, second column are the corresponding regression values
	private static final String tableName = "Table - Box counting dimension";
	
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

    @Parameter(label = "# of boxes/pyramid images",
    		   description = "Number of distinct box sizes with the power of 2",
	       	   style = NumberWidget.SPINNER_STYLE,
	           min = "1",
	           max = "32768",
	           stepSize = "1",
	           persist  = false,  //restore previous value default = true
	           initializer = "initialNumBoxes",
	           callback    = "callbackNumBoxes")
    private int spinnerInteger_NumBoxes;
    
    @Parameter(label = "Regression Min",
    		   description = "Minimum x value of linear regression",
 		       style = NumberWidget.SPINNER_STYLE,
 		       min = "1",
 		       max = "32768",
 		       stepSize = "1",
 		       //persist  = false,   //restore previous value default = true
 		       initializer = "initialRegMin",
 		       callback = "callbackRegMin")
    private int spinnerInteger_RegMin = 1;
 
    @Parameter(label = "Regression Max",
    		   description = "Maximum x value of linear regression",
    		   style = NumberWidget.SPINNER_STYLE,
		       min = "3",
		       max = "32768",
		       stepSize = "1",
		       persist  = false,   //restore previous value default = true
		       initializer = "initialRegMax",
		       callback = "callbackRegMax")
     private int spinnerInteger_RegMax = 3;
    
	//-----------------------------------------------------------------------------------------------------
     @Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
     private final String labelMethodOptions = METHODOPTIONS_LABEL;
     
     @Parameter(label = "Fractal dimension",
	    description = "Type of fractal dimension computation",
	    style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
	    choices = {"Box counting", "Pyramid"},
	    //persist  = false,  //restore previous value default = true
	    initializer = "initialFracalDimType",
	    callback = "callbackFractalDimType")
     private String choiceRadioButt_FractalDimType;
     
//     @Parameter(label = "Scanning method",
//    		    description = "Type of box scanning",
//    		    style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
//      		    choices = {"Raster box"} //, "Sliding box"}, //does not give the right dimension values
//      		    //persist  = false,  //restore previous value default = true
//    		    initializer = "initialScanningType",
//                callback = "callbackScanningType")
//     private String choiceRadioButt_ScanningType;
//     
//     @Parameter(label = "Analysis type",
// 		    description = "Type of image and computation",
// 		    style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
//   		    choices = {"Binary"}, //grey value does make sense "DBC", "RDBC"},
//   		    //persist  = false,  //restore previous value default = true
// 		    initializer = "initialAnalysisMethod",
//             callback = "callbackAnalysisMethod")
//     private String choiceRadioButt_AnalysisMethod;
     
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
     
     @Parameter(label   = "Process single active image ",
    		    callback = "callbackProcessActiveImage")
 	 private Button buttonProcessActiveImage;
     
     @Parameter(label   = "Process all available images",
 		        callback = "callbackProcessAllImages")
	 private Button buttonProcessAllImages;


    //---------------------------------------------------------------------
 
    //The following initialzer functions set initial values
    protected void initialNumBoxes() {
      	numBoxes = getMaxBoxNumber(datasetIn.max(0)+1, datasetIn.max(1)+1);
      	spinnerInteger_NumBoxes = numBoxes;
    }
    protected void initialRegMin() {
    	spinnerInteger_RegMin = 1;
    }
    protected void initialRegMax() {
    	numBoxes = getMaxBoxNumber(datasetIn.max(0)+1, datasetIn.max(1)+1);
    	spinnerInteger_RegMax =  numBoxes;
    }
    protected void initialFractalDimType() {
    	choiceRadioButt_FractalDimType = "Box counting";
    }
//    protected void initialScanningType() {
//    	choiceRadioButt_ScanningType = "Raster box";
//    }
//    protected void initialAnalysisMethod() {
//    	choiceRadioButt_AnalysisMethod = "Binary";
//    }
    protected void initialShowDoubleLogPlots() {
    	booleanShowDoubleLogPlot = true;
    }
    protected void initialOverwriteDisplays() {
    	booleanOverwriteDisplays = true;
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
		if (spinnerInteger_RegMax > spinnerInteger_NumBoxes) {
			spinnerInteger_RegMax = spinnerInteger_NumBoxes;
		}
		
		logService.info(this.getClass().getName() + " Regression Max set to " + spinnerInteger_RegMax);
	}

	/** Executed whenever the {@link #choiceRadioButtFractalDimType} parameter changes. */
	protected void callbackFractalDimType() {
		logService.info(this.getClass().getName() + " Fractal dimension computation method set to " + choiceRadioButt_FractalDimType);
		
	}
	
//	/** Executed whenever the {@link #choiceRadioButtScanningType} parameter changes. */
//	protected void callbackScanningType() {
//		logService.info(this.getClass().getName() + " Box method set to " + choiceRadioButt_ScanningType);
//		
//	}
//	
//	/** Executed whenever the {@link #choiceRadioButt_AnalysisMethod} parameter changes. */
//	protected void callbackAnalysisMethod() {
//		logService.info(this.getClass().getName() + " Analysis method set to " + choiceRadioButt_AnalysisMethod);
//		
//	}
	
	/** Executed whenever the {@link #booleanProcessImmediately} parameter changes. */
	protected void callbackProcessImmediately() {
		logService.info(this.getClass().getName() + " Process immediately set to " + booleanProcessImmediately);
	}
	
	/** Executed whenever the {@link #buttonProcessActiveImage} button is pressed. */
	protected void callbackProcessActiveImage() {
		//prepare  executer service
		exec = Executors.newSingleThreadExecutor();
				
		//dlgProgress = new WaitingDialogWithProgressBar("<html>Computing Box dimensions, please wait...<br>Open console window for further info.</html>");
		dlgProgress = new WaitingDialogWithProgressBar("Computing Box dimensions, please wait... Open console window for further info.",
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
			if (doubleLogPlotList != null) {
				for (int l = 0; l < doubleLogPlotList.size(); l++) {
					doubleLogPlotList.get(l).setVisible(false);
					doubleLogPlotList.get(l).dispose();
					//doubleLogPlotList.remove(l);  /
				}
				doubleLogPlotList.clear();		
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
		//Mass      0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
		//Perimeter 5 Intercept, 6 Slope, 7 InterceptStdErr, 8 SlopeStdErr, 9 RSquared
			
		//set values for output table
		for (int i = 0; i < regressionValues.length; i++ ) {
				resultValuesTable[s][i] = regressionValues[i]; 
		}
		//Compute dimension
		double dimMass  = Double.NaN;
		double dimPerim = Double.NaN;
		
		dimMass  = -regressionValues[1];
		dimPerim = -regressionValues[6];
		resultValuesTable[s][1] = dimMass;
		resultValuesTable[s][6] = dimPerim;
		
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
				double dimMass  = Double.NaN;
				double dimPerim = Double.NaN;
				
				dimMass  = -regressionValues[1];
				dimPerim = -regressionValues[6];
				resultValuesTable[s][1] = dimMass;
				resultValuesTable[s][6] = dimPerim;
				
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
		
		GenericColumn columnFileName       = new GenericColumn("File name");
		GenericColumn columnSliceName      = new GenericColumn("Slice name");
		IntColumn columnMaxNumBoxes        = new IntColumn("# Boxes");
		IntColumn columnRegMin             = new IntColumn("RegMin");
		IntColumn columnRegMax             = new IntColumn("RegMax");
		GenericColumn columnFractalDimType = new GenericColumn("Fractal dimension type");
		DoubleColumn columnFFI             = new DoubleColumn("FFI");
		DoubleColumn columnDmass           = new DoubleColumn("D-mass");
		DoubleColumn columnR2mass          = new DoubleColumn("R2-mass");
		DoubleColumn columnDbound          = new DoubleColumn("D-boundary");
		DoubleColumn columnR2bound         = new DoubleColumn("R2-boundary");
	
	    table = new DefaultGenericTable();
		table.add(columnFileName);
		table.add(columnSliceName);
		table.add(columnMaxNumBoxes);
		table.add(columnRegMin);
		table.add(columnRegMax);
		table.add(columnFractalDimType);
		table.add(columnFFI);
		table.add(columnDmass);
		table.add(columnR2mass);
		table.add(columnDbound);
		table.add(columnR2bound);
	}
	
	/** collects current result and shows table
	 *  @param int slice number of active image.
	 */
	private void collectActiveResultAndShowTable(int sliceNumber) {
	
		int regMin             = spinnerInteger_RegMin;
		int regMax             = spinnerInteger_RegMax;
		int numImages          = spinnerInteger_NumBoxes;
		String fractalDimType  = choiceRadioButt_FractalDimType;
		//String scanningType  = choiceRadioButt_ScanningType;	
		//String analysisType  = choiceRadioButt_AnalysisMethod;	
//		if ((!analysisType.equals("Binary")) && (regMin == 1)){
//			regMin = 2; //regMin == 1 (single pixel box is not possible for DBC algorithms)
//		}	
	    int s = sliceNumber;	
			//Mass     0 Intercept, 1 Dim, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
		    //Boundary 5 Intercept, 6 Dim, 7 InterceptStdErr, 8 SlopeStdErr, 9 RSquared		
			//fill table with values
			table.appendRow();
			table.set("File name",   	 table.getRowCount() - 1, datasetName);	
			if (sliceLabels != null) 	 table.set("Slice name", table.getRowCount() - 1, sliceLabels[s]);
			table.set("# Boxes",    	 table.getRowCount()-1, numImages);	
			table.set("RegMin",      	 table.getRowCount()-1, regMin);	
			table.set("RegMax",      	 table.getRowCount()-1, regMax);	
			table.set("Fractal dimension type",   table.getRowCount()-1, fractalDimType);	
			table.set("FFI",         	 table.getRowCount()-1, resultValuesTable[s][1] - resultValuesTable[s][6]);
			table.set("D-mass",          table.getRowCount()-1, resultValuesTable[s][1]);
			table.set("R2-mass",         table.getRowCount()-1, resultValuesTable[s][4]);
			table.set("D-boundary",      table.getRowCount()-1, resultValuesTable[s][6]);
			table.set("R2-boundary",     table.getRowCount()-1, resultValuesTable[s][9]);
			
		//Show table
		uiService.show(tableName, table);
	}
	
	/** collects all results and shows table */
	private void collectAllResultsAndShowTable() {
	
		int regMin          = spinnerInteger_RegMin;
		int regMax          = spinnerInteger_RegMax;
		int numImages    	= spinnerInteger_NumBoxes;
		String fractalDimType  = choiceRadioButt_FractalDimType;
//		String scanningType = choiceRadioButt_ScanningType;	
//		String analysisType = choiceRadioButt_AnalysisMethod;	
//		if ((!analysisType.equals("Binary")) && (regMin == 1)){
//			regMin = 2; //regMin == 1 (single pixel box is not possible for DBC algorithms)
//		}
		//loop over all slices
		for (int s = 0; s < numSlices; s++){ //slices of an image stack
			//Mass     0 Intercept, 1 Dim, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
		    //Boundary 5 Intercept, 6 Dim, 7 InterceptStdErr, 8 SlopeStdErr, 9 RSquared		
			//fill table with values
			table.appendRow();
			table.set("File name",	   	 table.getRowCount() - 1, datasetName);	
			if (sliceLabels != null)	 table.set("Slice name", table.getRowCount() - 1, sliceLabels[s]);
			table.set("# Boxes",    	 table.getRowCount()-1, numImages);	
			table.set("RegMin",      	 table.getRowCount()-1, regMin);	
			table.set("RegMax",      	 table.getRowCount()-1, regMax);	
			table.set("Fractal dimension type",   table.getRowCount()-1, fractalDimType);	
			table.set("FFI",         	 table.getRowCount()-1, resultValuesTable[s][1] - resultValuesTable[s][6]);
			table.set("D-mass",          table.getRowCount()-1, resultValuesTable[s][1]);
			table.set("R2-mass",         table.getRowCount()-1, resultValuesTable[s][4]);
			table.set("D-boundary",      table.getRowCount()-1, resultValuesTable[s][6]);
			table.set("R2-boundary",     table.getRowCount()-1, resultValuesTable[s][9]);
		}
		//Show table
		uiService.show(tableName, table);
	}
							
	/** 
	 * Processing ****************************************************************************************
	 * */
	private double[] process(RandomAccessibleInterval<?> rai, int plane) { //plane plane (Image) number

		
		int regMin          = spinnerInteger_RegMin;
		int regMax          = spinnerInteger_RegMax;
		int numBoxes        = spinnerInteger_NumBoxes;
		String fractalDimType  = choiceRadioButt_FractalDimType;
		//String scanningType  = choiceRadioButt_ScanningType;	
		//String analysisType  = choiceRadioButt_AnalysisMethod;	
//		if ((!analysisType.equals("Binary")) && (regMin == 1)){
//			regMin = 2; //regMin == 1 (single pixel box is not possible for DBC algorithms)
//		}	
		String scanningType = "Raster box";
		String analysisType = "Binary";	
		
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
		

		double[][] totalsMass        = new double[numBoxes][numBands];
		double[][] totalsBoundary    = new double[numBoxes][numBands];
		double[]   totalsMassMax     = new double[numBands]; //for binary images
		double[]   totalsBoundaryMax = new double[numBands]; //for binary images
		int[]      eps               = new int[numBoxes];
		
		// definition of eps
		for (int n = 0; n < numBoxes; n++) {
				eps[n] = (int) Math.pow(2, n);
				//logService.info(this.getClass().getName() + " n:" + n + " eps:  " + eps[n]);	
		}		
		
		//********************************Binary Image: 0 and [1, 255]! and not: 0 and 255
		if (fractalDimType.equals("Box counting")) {//"Box counting", "Pyramid"
			//Box counting
			//n=0  2^0 = 1 ... single pixel
			// Loop through all pixels.
			for (int b = 0; b < numBands; b++) {
				cursor = Views.iterable(rai).localizingCursor();	
				while (cursor.hasNext()) {
					cursor.fwd();
					//cursor.localize(pos);			
					if (((UnsignedByteType) cursor.get()).get() > 0) {
						totalsMass[0][b] += 1; // Binary Image: 0 and [1, 255]! and not: 0 and 255
						totalsMassMax[b] = totalsMassMax[b] + 1; // / totalsMax[b];
					}
					//totals[n][b] = totals[n][b]; // / totalsMax[b];
				}
			}//b	
			int boxSize;		
			int delta = 0;
			for (int b = 0; b < numBands; b++) {
				for (int n = 1; n < numBoxes; n++) { //2^1  to 2^numBoxes		
					boxSize = eps[n];	
					if      (scanningType.equals("Raster box"))  delta = boxSize;
					else if (scanningType.equals("Sliding box")) delta = 1;
					for (int x =0; x <= (width-boxSize); x=x+delta){
						for (int y =0;  y<= (height-boxSize); y=y+delta){
							raiBox = Views.interval(rai, new long[]{x, y}, new long[]{x+boxSize-1, y+boxSize-1});
							boolean isGreaterZeroFound = false;
							// Loop through all pixels of this box.
							cursor = Views.iterable(raiBox).localizingCursor();
							while (cursor.hasNext()) { //Box
								cursor.fwd();
								//cursorF.localize(pos);				
								if (((UnsignedByteType) cursor.get()).get() > 0) {
									totalsMass[n][b] += 1; // Binary Image: 0 and [1, 255]! and not: 0 and 255
									totalsMassMax[b] = totalsMassMax[b] + 1; // / totalsMax[b];
									isGreaterZeroFound = true;
								}			
								if (isGreaterZeroFound) break; //do not search in this box any more
							}//while Box
						} //y	
					} //x                                          
				} //n
			}//b band	
			
			//*********create boundary image
			
			//Compute erode image
			RectangleShape kernel = new RectangleShape(1, false); //3x3kernel skipCenter = false
			Runtime runtime  = Runtime.getRuntime();
			long maxMemory   = runtime.maxMemory();
			long totalMemory = runtime.totalMemory();
			long freeMemory  = runtime.freeMemory();
			int availableProcessors = runtime.availableProcessors();
			//System.out.println("availabel processors: " + availableProcessors);
			
			int numThreads = 6; //For erosion //with 6 it was 3 times faster than only with one thread
			if (numThreads > availableProcessors) numThreads = availableProcessors;

			long[] pos = new long[2];
			int pixelValueImgOrig;
			float pixelValueImgTemp;
			
			imgFloat = createImgFloat(rai);
			imgTemp = Erosion.erode(imgFloat, kernel, numThreads); //eroded image
			//uiService.show("Eroded image", imgTemp);
			ra = (RandomAccess<UnsignedByteType>) rai.randomAccess();
			//subtraction ImgOrig - ImgErode
			cursorF = imgTemp.localizingCursor();
			while (cursorF.hasNext()) {
				cursorF.fwd();
				cursorF.localize(pos);
				ra.setPosition(pos);
				pixelValueImgTemp = ((FloatType) cursorF.get()).get();    //eroded image
				pixelValueImgOrig = ((UnsignedByteType) ra.get()).getInteger(); //original image
				// Binary Image: 0 and [1, 255]! and not: 0 and 255
				if ((pixelValueImgOrig > 0) && (pixelValueImgTemp == 0)) {
					((FloatType) cursorF.get()).set(255f);					
				} else {
					((FloatType) cursorF.get()).set(0f);		
				}
			}
			//uiService.show("Boundary image", imgTemp);
			
			//do again Box counting with boundary image
			//n=0  2^0 = 1 ... single pixel
			// Loop through all pixels.
			for (int b = 0; b < numBands; b++) {
				cursorF = imgTemp.localizingCursor();	
				while (cursorF.hasNext()) {
					cursorF.fwd();
					//cursor.localize(pos);			
					if (((FloatType) cursorF.get()).get() > 0) {
						totalsBoundary[0][b] += 1; // Binary Image: 0 and [1, 255]! and not: 0 and 255
						totalsBoundaryMax[b] = totalsBoundaryMax[b] + 1;
					}
				}
			}//b	
		
			for (int b = 0; b < numBands; b++) {
				for (int n = 1; n < numBoxes; n++) { //2^1  to 2^numBoxes		
					boxSize = eps[n];		
					if      (scanningType.equals("Raster box"))  delta = boxSize;
					else if (scanningType.equals("Sliding box")) delta = 1;
					for (int x =0; x <= (width-boxSize); x=x+delta){
						for (int y =0;  y<= (height-boxSize); y=y+delta){
							raiBox = Views.interval(imgTemp, new long[]{x, y}, new long[]{x+boxSize-1, y+boxSize-1});
							boolean isGreaterZeroFound = false;
							// Loop through all pixels of this box.
							cursorF = (Cursor<FloatType>) Views.iterable(raiBox).localizingCursor();
							while (cursorF.hasNext()) { //Box
								cursorF.fwd();
								//cursorF.localize(pos);				
								if (((FloatType) cursorF.get()).get() > 0) {
									totalsBoundary[n][b] += 1; // Binary Image: 0 and [1, 255]! and not: 0 and 255
									totalsBoundaryMax[b] = totalsBoundaryMax[b] + 1; // / totalsMax[b];
									isGreaterZeroFound = true;
								}			
								if (isGreaterZeroFound) break; //do not search in this box any more
							}//while Box
						} //y	
					} //x                                          
				} //n
			}//b band				
		} //Box Counting dimension
		else if (fractalDimType.equals("Pyramid")) {//"Box counting", "Pyramid"
			//only sub-sampling by averaging of original image works.
			//interpolation and then sub-sampling does not give the same results as Box counting
			int numPyramidImages = numBoxes;
			int downSamplingFactor;
			
			//Compute Mass
			for (int n = 0; n < numPyramidImages; n++) { //Downscaling incl. no downscaling
				 // "base-pyramid", i.e. layers of pyramid from base layer
				downSamplingFactor =  eps[n]; //of downsampling
				imgFloat = createImgFloat(rai);
			    imgSubSampled = subSamplingByAveraging(imgFloat, downSamplingFactor);
			   // uiService.show("Subsampled image", imgSubSampled);
				//logService.info(this.getClass().getName() + " width:"+ (imgDownscaled.dimension(0)) + " height:" + (imgDownscaled.dimension(1)));

				//****IMPORTANT****Displaying a rai slice (pseudo 2D) directly with e.g. uiService.show(name, rai);
				//pushes a 3D array to the display and
				//yields mouse moving errors because the third dimension is not available		
				
				//if ((optShowDownscaledImages) && (n > 0)) uiService.show("1/"+power+" downscaled image", imgDownscaled);
				// Loop through all pixels.
				cursorF = imgSubSampled.localizingCursor();
				while (cursorF.hasNext()) {
					cursorF.fwd();
					//cursorF.localize(pos);
					for (int b = 0; b < numBands; b++) {
						if (cursorF.get().get() > 0) { // Binary Image //[1, 255] and not [255, 255] because interpolation introduces grey values other than 255!
							totalsMass[n][b] += 1; 
							totalsMassMax[b] = totalsMassMax[b]  + 1; // / totalsMax[b];
						}
						
					}
				}	
			}//n
			//*********create boundary image	
			//Compute erode image
			RectangleShape kernel = new RectangleShape(1, false); //3x3kernel skipCenter = false
			Runtime runtime  = Runtime.getRuntime();
			long maxMemory   = runtime.maxMemory();
			long totalMemory = runtime.totalMemory();
			long freeMemory  = runtime.freeMemory();
			int availableProcessors = runtime.availableProcessors();
			//System.out.println("availabel processors: " + availableProcessors);
			
			int numThreads = 6; //For erosion //with 6 it was 3 times faster than only with one thread
			if (numThreads > availableProcessors) numThreads = availableProcessors;

			long[] pos = new long[2];
			int pixelValueImgOrig;
			float pixelValueImgTemp;
			imgFloat = this.createImgFloat(rai);
			imgTemp = Erosion.erode(imgFloat, kernel, numThreads); //eroded image
			//uiService.show("Eroded image", imgTemp);
			ra = (RandomAccess<UnsignedByteType>) rai.randomAccess();
			//subtraction ImgOrig - ImgErode
			cursorF = imgTemp.localizingCursor();
			while (cursorF.hasNext()) {
				cursorF.fwd();
				cursorF.localize(pos);
				ra.setPosition(pos);
				pixelValueImgTemp = ((FloatType) cursorF.get()).get();    //eroded image
				pixelValueImgOrig = ((UnsignedByteType) ra.get()).getInteger(); //original image
				// Binary Image: 0 and [1, 255]! and not: 0 and 255
				if ((pixelValueImgOrig > 0) && (pixelValueImgTemp == 0)) {
					((FloatType) cursorF.get()).set(255f);					
				} else {
					((FloatType) cursorF.get()).set(0f);		
				}
			}
			//uiService.show("Boundary image", imgTemp);
			//Compute Boundary
			for (int n = 0; n < numPyramidImages; n++) { //Downscaling incl. no downscaling
				 // "base-pyramid", i.e. layers of pyramid from base layer
				downSamplingFactor =  eps[n]; //of downsampling
			    imgSubSampled = subSamplingByAveraging(imgTemp, downSamplingFactor);
				//logService.info(this.getClass().getName() + " width:"+ (imgDownscaled.dimension(0)) + " height:" + (imgDownscaled.dimension(1)));

				//****IMPORTANT****Displaying a rai slice (pseudo 2D) directly with e.g. uiService.show(name, rai);
				//pushes a 3D array to the display and
				//yields mouse moving errors because the third dimension is not available		
				
				//if ((optShowDownscaledImages) && (n > 0)) uiService.show("1/"+power+" downscaled image", imgDownscaled);
				// Loop through all pixels.
				cursorF = imgSubSampled.localizingCursor();
				while (cursorF.hasNext()) {
					cursorF.fwd();
					//cursorF.localize(pos);
					for (int b = 0; b < numBands; b++) {
						if (cursorF.get().get() > 0) { // Binary Image //[1, 255] and not [255, 255] because interpolation introduces grey values other than 255!
							totalsBoundary[n][b] += 1; 
							totalsBoundaryMax[b] = totalsBoundaryMax[b]  + 1; // / totalsMax[b];
						}
					}
				}	
			}//n
		}
		
		
		//Normalization 
//		for (int n = 0; n < numBoxes; n++) {
//			for (int b = 0; b < numBands; b++) {
//				totalsMass[n][b] = totalsMass[n][b] / totalsMassMax[b];
//				totalsBoundary[n][b] = totalsBoundary[n][b] / totalsBoundaryMax[b];
//			}
//		}	
		//Computing log values for plot 
		//Change sequence of entries to start with a pixel
		double[][] lnTotalsMass     = new double[numBoxes][numBands];
		double[][] lnTotalsBoundary = new double[numBoxes][numBands];
		double[] lnEps              = new double[numBoxes];
		for (int n = 0; n < numBoxes; n++) {
			for (int b = 0; b < numBands; b++) {
				if (totalsMass[n][b] <= 1) {
					//lnTotals[numBoxes - n - 1][b] = 0.0; //Math.log(Float.MIN_VALUE); // damit logarithmus nicht undefiniert ist//IQM
					lnTotalsMass[n][b] = 0.0;
				} else if (Double.isNaN(totalsMass[n][b])) {
					//lnTotals[numBoxes - n - 1][b] = 0.0;
					lnTotalsMass[n][b] = Double.NaN;
				} else {
					//lnTotals[numBoxes - n - 1][b] = Math.log(totals[n][b]);//IQM
					lnTotalsMass[n][b] = Math.log(totalsMass[n][b]); //
				}
				if (totalsBoundary[n][b] <= 1) {
					//lnTotals[numBoxes - n - 1][b] = 0.0; //Math.log(Float.MIN_VALUE); // damit logarithmus nicht undefiniert ist//IQM
					lnTotalsBoundary[n][b] = 0.0;
				} else if (Double.isNaN(totalsBoundary[n][b])) {
					//lnTotals[numBoxes - n - 1][b] = 0.0;
					lnTotalsBoundary[n][b] = Double.NaN;
				} else {
					//lnTotals[numBoxes - n - 1][b] = Math.log(totals[n][b]);//IQM
					lnTotalsBoundary[n][b] = Math.log(totalsBoundary[n][b]); //
				}
				
				//lnEps[n][b] = Math.log(eps[numBoxes - n - 1 ][b]); //IQM
				lnEps[n] = Math.log(eps[n]);
				//logService.info(this.getClass().getName() + " n:" + n + " eps:  " + eps[n][b]);
				//logService.info(this.getClass().getName() + " n:" + n + " lnEps:  "+  lnEps[n][b] );
				//logService.info(this.getClass().getName() + " n:" + n + " totalsMass[n][b]: " + totalsMass[n][b]);
			}
		}
		
		//Create double log plot
		boolean isLineVisible = false; //?
				
		// Plot //nur ein Band!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		double[]   lnDataX  = new double[numBoxes];
		double[][] lnDataY  = new double[2][numBoxes];
	
			
		//only first band!!!!!!!!!
		for (int n = 0; n < numBoxes; n++) {	
			lnDataY[0][n]  = lnTotalsMass[n][0];	
			lnDataY[1][n]  = lnTotalsBoundary[n][0];	
			lnDataX[n]     = lnEps[n];
		}
		// System.out.println("FractalDimensionBoxCounting: dataY: "+ dataY);
		// System.out.println("FractalDimensionBoxCounting: dataX: "+ dataX);
	
		if (optShowPlot) {			
			String preName = "";
			if (numSlices > 1) {
				preName = "Slice-"+String.format("%03d", plane) +"-";
			}
			String xAxisLabel = "";
			if      (fractalDimType.equals("Box counting")) xAxisLabel = "ln(Box size)";
			else if (fractalDimType.equals("Pyramid"))      xAxisLabel = "ln(2^n)";
			
			String[] legendLabels = new String[2];
			legendLabels[0] = "Mass";
			legendLabels[1] = "Boundary";
			
			RegressionPlotFrame doubleLogPlot = DisplayRegressionPlotXY(lnDataX, lnDataY, isLineVisible, "Double Log Plots - FFI", 
					preName + datasetName, xAxisLabel, "ln(Count)", legendLabels,
					regMin, regMax);
			doubleLogPlotList.add(doubleLogPlot);
		}
				
		// Compute regressions
		LinearRegression lr = new LinearRegression();
		double[] regressionParamsMass = lr.calculateParameters(lnDataX, lnDataY[0], regMin, regMax);
		//0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
		lr = new LinearRegression();
		double[] regressionParamsPerim = lr.calculateParameters(lnDataX, lnDataY[1], regMin, regMax);
		
		regressionParams = new double[regressionParamsMass.length + regressionParamsPerim.length]; 
		for (int r = 0; r < regressionParamsMass.length; r++) {
			regressionParams[r] = regressionParamsMass[r];
		}
		for (int r = 0; r < regressionParamsPerim.length; r++) {
			regressionParams[r + regressionParamsMass.length] = regressionParamsPerim[r];
		}
			
		return regressionParams;
		//Output
		//uiService.show(tableName, table);
		//result = ops.create().img(image, new FloatType());
		//table
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
	private RegressionPlotFrame DisplayRegressionPlotXY(double[] dataX, double[][] dataY, boolean isLineVisible,
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
	 * 
	 * This methods creates an Img<FloatType>
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
	
	/**
	 * This method creates a smaller image by averaging pixel values 
	 * @param imgFloat
	 * @param downSamplingFactor
	 * @return
	 */	
	private Img<FloatType> subSamplingByAveraging(Img<FloatType> imgFloat, int downSamplingFactor) {
		
		int averagingSize = downSamplingFactor;
		
		int numDimensions = imgFloat.numDimensions();
		// compute the number of pixels of the output and the size of the real interval
		long[] newSize = new long[numDimensions];
	
		for ( int d = 0; d < numDimensions; ++d ){
			newSize[d] = (int)Math.floor(imgFloat.dimension(d) / downSamplingFactor);
		}		
		// create the output image
		ArrayImgFactory arrayImgFactory = new ArrayImgFactory<>(new FloatType());
		imgSubSampled = arrayImgFactory.create( newSize );

		// cursor to iterate over all pixels
		cursorF = imgSubSampled.localizingCursor();

		// create a RandomAccess on the source
		RandomAccess<FloatType> ra = imgFloat.randomAccess();
		long[] pos = new long[numDimensions];
		float mean = 0f;
		long count = 0;
		// for all pixels of the output image
		while (cursorF.hasNext()) {
			cursorF.fwd();
			cursorF.localize(pos);
			//Get average
			mean = 0f;
			
			for (int i = 0; i < averagingSize; i++) {
				for (int j = 0; j < averagingSize; j++) {
					ra.setPosition(pos[0]*averagingSize + i, 0);
					ra.setPosition(pos[1]*averagingSize + j, 1);
					mean = mean + ra.get().getRealFloat();
					count = count +1;
				}
			}
			mean = mean/(float)count;
			cursorF.get().set(mean);
		}
		return imgSubSampled;
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
		//ij.command().run(FractalDimensionFFI.class, true).get().getOutput("image");
		ij.command().run(FractalDimensionFFI.class, true);
	}
}

