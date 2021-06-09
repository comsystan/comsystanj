/*-
 * #%L
 * Project: ImageJ plugin for computing fractal dimension with tug of war algorithm.
 * File: FractalDimensionTugOfWar.java
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
package at.csa.csaj.img2d.frac.dim.tow;

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
import org.scijava.table.FloatColumn;
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
 * <the fractal Tug of war dimension </a>
 * of an image.
 */
@Plugin(type = InteractiveCommand.class,
	headless = true,
	label = "Tug of war dimension",
	menu = {
	@Menu(label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT, mnemonic = MenuConstants.PLUGINS_MNEMONIC),
	@Menu(label = "ComsystanJ"),
	@Menu(label = "Image (2D)"),
	@Menu(label = "Tug of war dimension", weight = 11)})
public class FractalDimensionTugOfWar<T extends RealType<T>> extends InteractiveCommand implements Command, Previewable { //non blocking GUI
//public class FractalDimensionTugOfWar<T extends RealType<T>> implements Command {	//modal GUI
	
	private static final String PLUGIN_LABEL            = "<html><b>Computes fractal dimension with tug of war algorithm</b></html>";
	private static final String SPACE_LABEL             = "";
	private static final String REGRESSION_LABEL        = "<html><b>Regression parameters</b></html>";
	private static final String METHODOPTIONS_LABEL     = "<html><b>Method options</b></html>";
	private static final String BACKGROUNDOPTIONS_LABEL = "<html><b>Background option</b></html>";
	private static final String DISPLAYOPTIONS_LABEL    = "<html><b>Display options</b></html>";
	private static final String PROCESSOPTIONS_LABEL    = "<html><b>Process options</b></html>";
	
	private static Img<FloatType> imgFloat; 
	RandomAccessibleInterval<?> raiBox;
	private static RandomAccess<UnsignedByteType> ra;
	Cursor<?> cursor = null;
	private static String datasetName;
	private static String[] sliceLabels;
	private static long width  = 0;
	private static long height = 0;
	private static long numDimensions = 0;
	private static long numSlices  = 0;
	private static int  numBoxes = 0;
	private static ArrayList<RegressionPlotFrame> doubleLogPlotList = new ArrayList<RegressionPlotFrame>();
	private static double[][] resultValuesTable; //first column is the image index, second column are the corresponding regression values
	private static final String tableName = "Table - Tug of war dimension";
	
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

    @Parameter(label = "Number of boxes",
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
     
     @Parameter(label = "Accuracy",
 		       description = "Accuracy",
	       	   style = NumberWidget.SPINNER_STYLE,
	           min = "1",
	           max = "99999999999999",
	           stepSize = "1",
	           //persist  = false,  //restore previous value default = true
	           initializer = "initialNumAccuracy",
	           callback    = "callbackNumAccuracy")
     private int spinnerInteger_NumAcurracy;
     
     @Parameter(label = "Confidence",
 		       description = "Confidence",
	       	   style = NumberWidget.SPINNER_STYLE,
	           min = "1",
	           max = "99999999999999",
	           stepSize = "1",
	           //persist  = false,  //restore previous value default = true
	           initializer = "initialNumConfidence",
	           callback    = "callbackNumConfidence")
     private int spinnerInteger_NumConfidence;
     
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
      	numBoxes = getMaxBoxNumber(datasetIn.dimension(0), datasetIn.dimension(1));
      	spinnerInteger_NumBoxes = numBoxes;
    }
    protected void initialRegMin() {
    	spinnerInteger_RegMin = 1;
    }
    protected void initialRegMax() {
    	numBoxes = getMaxBoxNumber(datasetIn.dimension(0), datasetIn.dimension(1));
    	spinnerInteger_RegMax =  numBoxes;
    }
 
    protected void initialNumAccuracy() {
    	spinnerInteger_NumAcurracy = 90; //s1=30 Wang paper
    }
    
    protected void initialNumConfidence() {
    	spinnerInteger_NumConfidence = 15; //s2=5 Wang paper
    }
    
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
	
	/** Executed whenever the {@link #spinInteger_NumAccuracy} parameter changes. */
	protected void callbackNumAccuracy() {
		logService.info(this.getClass().getName() + " Accuracy set to " + spinnerInteger_NumAcurracy);
	}
	
	/** Executed whenever the {@link #spinInteger_NumConfidence} parameter changes. */
	protected void callbackNumConfidence() {
		logService.info(this.getClass().getName() + " Confidence set to " + spinnerInteger_NumConfidence);
	}

	/** Executed whenever the {@link #booleanProcessImmediately} parameter changes. */
	protected void callbackProcessImmediately() {
		logService.info(this.getClass().getName() + " Process immediately set to " + booleanProcessImmediately);
	}
	
	/** Executed whenever the {@link #buttonProcessActiveImage} button is pressed. */
	protected void callbackProcessActiveImage() {
		//prepare  executer service
		exec = Executors.newSingleThreadExecutor();
				
		//dlgProgress = new WaitingDialogWithProgressBar("<html>Computing Tug of war dimension, please wait...<br>Open console window for further info.</html>");
		dlgProgress = new WaitingDialogWithProgressBar("Computing Tug of war dimension, please wait... Open console window for further info.",
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
				
		//dlgProgress = new WaitingDialogWithProgressBar("<html>Computing Tug of war dimensions, please wait...<br>Open console window for further info.</html>");
		dlgProgress = new WaitingDialogWithProgressBar("Computing Tug of war dimensions, please wait... Open console window for further info.",
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
			//0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
			
		//set values for output table
		for (int i = 0; i < regressionValues.length; i++ ) {
				resultValuesTable[s][i] = regressionValues[i]; 
		}
		//Compute dimension
		double dim = Double.NaN;
		
		dim = regressionValues[1];
		resultValuesTable[s][1] = dim;
		
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
			
				dim = regressionValues[1];
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
		IntColumn columnAccuracy           = new IntColumn("Accuracy");
		IntColumn columnConfidence         = new IntColumn("Confidence");
		DoubleColumn columnDp              = new DoubleColumn("Dtow");
		DoubleColumn columnR2              = new DoubleColumn("R2");
		DoubleColumn columnStdErr          = new DoubleColumn("StdErr");
		
	    table = new DefaultGenericTable();
		table.add(columnFileName);
		table.add(columnSliceName);
		table.add(columnMaxNumBoxes);
		table.add(columnRegMin);
		table.add(columnRegMax);
		table.add(columnAccuracy);
		table.add(columnConfidence);
		table.add(columnDp);
		table.add(columnR2);
		table.add(columnStdErr);
	}
	
	/** collects current result and shows table
	 *  @param int slice number of active image.
	 */
	private void collectActiveResultAndShowTable(int sliceNumber) {
	
		int numImages     = spinnerInteger_NumBoxes;
		int regMin        = spinnerInteger_RegMin;
		int regMax        = spinnerInteger_RegMax;
		int accuracy 	  = spinnerInteger_NumAcurracy;
		int confidence    = spinnerInteger_NumConfidence;
		
	    int s = sliceNumber;	
			//0 Intercept, 1 Dim, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared		
			//fill table with values
			table.appendRow();
			table.set("File name",   	 table.getRowCount() - 1, datasetName);	
			if (sliceLabels != null) 	 table.set("Slice name", table.getRowCount() - 1, sliceLabels[s]);
			table.set("# Boxes",    	 table.getRowCount()-1, numImages);	
			table.set("RegMin",      	 table.getRowCount()-1, regMin);	
			table.set("RegMax",      	 table.getRowCount()-1, regMax);
			table.set("Accuracy",      	 table.getRowCount()-1, accuracy);	
			table.set("Confidence",      table.getRowCount()-1, confidence);	
			table.set("Dtow",          	 table.getRowCount()-1, resultValuesTable[s][1]);
			table.set("R2",          	 table.getRowCount()-1, resultValuesTable[s][4]);
			table.set("StdErr",      	 table.getRowCount()-1, resultValuesTable[s][3]);		
		
		//Show table
		uiService.show(tableName, table);
	}
	
	/** collects all results and shows table */
	private void collectAllResultsAndShowTable() {
	
		int numImages     = spinnerInteger_NumBoxes;
		int regMin        = spinnerInteger_RegMin;
		int regMax        = spinnerInteger_RegMax;
		int accuracy 	  = spinnerInteger_NumAcurracy;
		int confidence    = spinnerInteger_NumConfidence;

		//loop over all slices
		for (int s = 0; s < numSlices; s++){ //slices of an image stack
			//0 Intercept, 1 Dim, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared		
			//fill table with values
			table.appendRow();
			table.set("File name",	   	 table.getRowCount() - 1, datasetName);	
			if (sliceLabels != null)	 table.set("Slice name", table.getRowCount() - 1, sliceLabels[s]);
			table.set("# Boxes",    	 table.getRowCount()-1, numImages);	
			table.set("RegMin",      	 table.getRowCount()-1, regMin);	
			table.set("RegMax",      	 table.getRowCount()-1, regMax);	
			table.set("Accuracy",      	 table.getRowCount()-1, accuracy);	
			table.set("Confidence",      table.getRowCount()-1, confidence);	
			table.set("Dtow",          	 table.getRowCount()-1, resultValuesTable[s][1]);
			table.set("R2",          	 table.getRowCount()-1, resultValuesTable[s][4]);
			table.set("StdErr",      	 table.getRowCount()-1, resultValuesTable[s][3]);		
		}
		//Show table
		uiService.show(tableName, table);
	}
							
	/** 
	 * Processing ****************************************************************************************
	 * */
	/** Martin Reiss
	 * TUG OF WAR - KORRELATIONSDIMENSION
	 * 
	 *  DESCRIPTION:
	 *  Der Tug of War Korrelationsdimension ist eine Approximation der Fixed-Grid Korrelationsdimension. 
	 *  Die Methode reduziert im Vergleich den Speicherplatzaufwand ohne dabei die Laufzeit zu erhöhen und erheblich 
	 *  an Genauigkeit einzubüßen. Das Theorem 2.2 aus [1] zeigt dass ein System von 4 unabhängigen Hashfunktionen verwendet 
	 *  werden kann um einen Wert zu berechnen, dessen Quadrat das zweite Moment, für einen gewisse Boxgröße, approximiert. 
	 *  Das Form bzw. wie diese Hashfunktionen generiert werden sollen ist in [1] aber nicht angegeben. 
	 *  Eine mögliche Hashfunktionen wird wie in [2] vorgeschlagen übernommen. 
	 * 
	 *  [1] N.Alon, Y.Matias, M.Szegedy; The Space Complexity of Approximating the Frequency Moments
	 *  [2] A.Wong, L. Wu, P.B.Gibbons, C.Faloutsos, Fast Estimation of fractal dimension and correlation integral on stream data
	 * 
	 *  KNOWN PROBLEMS/OPEN QUESTIONS:
	 * 	1) Use hash function = x_part+y_part+z_part%q or hash_function = (x_part+y_part+z_part)%q ?
	 * 	2) Method not that fast as it's supposed to be.
	 * 	3) In 1D TugOfWar Lacunarity the result is very sensitive to the value of q. (Example: Regular Line) 
	 *  
	 * 	UPDATE:
	 *  07.08.2013 ad 1): Comparison of BoxCounting-CorrDim and TugOfWar yields that (x_part+y_part+z_part)%q should be the right formula. 
	 *  					-> RadDaten:
	 *  					   BoxCounting-CorrDim = 2.33
	 *  					   PairCounting CorrDim= 2.28 ( only 0.001% of all points considered ) 
	 *  					   TugOfWar            = 2.36 with (x_part+y_part+z_part)%q 
	 *  					   TugOfWar            = 1.58 with x_part+y_part+z_part%q   
	 */
	private double[] process(RandomAccessibleInterval<?> rai, int plane) { //plane plane (Image) number

		int numBoxes      = spinnerInteger_NumBoxes;
		int regMin        = spinnerInteger_RegMin;
		int regMax        = spinnerInteger_RegMax;
		int accuracy 	  = spinnerInteger_NumAcurracy;
		int confidence    = spinnerInteger_NumConfidence;
	
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
		

		double[] totals = new double[numBoxes];
		int[]eps        = new int[numBoxes];
		
		// definition of eps
		for (int n = 0; n < numBoxes; n++) {
			eps[n] = (int)(Math.round(Math.pow(2, n)));
			//logService.info(this.getClass().getName() + " n:" + n + " eps:  " + eps[n]);	
		}		
		
		//Tug of war parameters
		int s1=accuracy; 	//accuracy parameter	//s1=30 (Paper)
		int s2=confidence;	//confidence parameter	//s2=5 (Paper)
				
		double count[]=new double[s1];
		double meanS1[]=new double[s2];
		
		long totalNumberOfPoints = 0;
		
		double count_sum = 0.0;
		
		int q = 8837;
		int xx = 0;
		int yy = 0;

		int x_part = 0;
		int y_part = 0;

		int hash_function = 0;
		int k = 0;
		
		boolean even;
		
		// Count total number of points		
		int sample;
		cursor = Views.iterable(rai).localizingCursor();
		while (cursor.hasNext()) {
			cursor.fwd();
			sample = ((UnsignedByteType) cursor.get()).getInteger();
			if (sample > 0) totalNumberOfPoints = totalNumberOfPoints + 1;		
		}

		// Get coordinates
		ra = (RandomAccess<UnsignedByteType>) rai.randomAccess();
		int[] xCoordinate=new int [(int)totalNumberOfPoints];
		int[] yCoordinate=new int [(int)totalNumberOfPoints];	
		
		do {
			for(int i = 0; i < width; i++){	
				for(int j = 0; j < height; j++){
					ra.setPosition(i, 0);
					ra.setPosition(j, 1);
					if (ra.get().getInteger() > 0 ){	
						xCoordinate[k] = i;
						yCoordinate[k] = j;
						k++;
					}
				}
			}
		} while ( k < totalNumberOfPoints );

		for (int n = 0; n < numBoxes; n++) {
			
			int radius = eps[n];		
			double proz = (double) n / (double) numBoxes * 100;
			//System.out.println("Loop progress: " + (int) proz + " %");
			// STEP 1: PARTITION s1*s2 COUNTERS INTO s2 SETS OF s1 COUNTERS
			for(int s2_i = 0; s2_i < s2; s2_i++){
				for(int s1_i = 0; s1_i < s1; s1_i++){	
					int a_prim = getPrimeNumber();
					int b_prim = getPrimeNumber();
					int c_prim = getPrimeNumber();
					int d_prim = getPrimeNumber();

					// sum over coordinates
					for(int i = 0; i < totalNumberOfPoints; i++){	
						xx=(int) Math.floor( xCoordinate[i]/radius); 
						yy=(int) Math.floor( yCoordinate[i]/radius); 


						// hash function in parts
						x_part = a_prim*xx*xx*xx+b_prim*xx*xx+c_prim*xx+d_prim;
						y_part = a_prim*a_prim*yy*yy*yy+b_prim*b_prim*yy*yy+c_prim*c_prim*yy+d_prim*d_prim;

						hash_function = (x_part+y_part)%q; // UPDATE 07.08.2013 

						even = (hash_function & 1) == 0; // Very slow! Try hash_function=1000 

						if (even) {
							count[s1_i]++;
						} else {
							count[s1_i]+=-1;
						}
					} // end sum over coordinates
				} // end s1 loop

				for(int s1_i = 0; s1_i < s1; s1_i++){
				count_sum += count[s1_i]*count[s1_i]; // square the counters values	
				}
				// STEP 2: COMPUTE THE MEAN OF EACH SUBSET
				meanS1[s2_i] = count_sum / s1; 
				
				// Set values equal to zero
				count_sum = 0.0;
				for(int s1_i = 0; s1_i < s1; s1_i++){	
				count[s1_i] = 0;
				}
				
			} // End s2 loop
			
			// STEP 3: DETERMINE THE MEDIAN
			for(int s2_i = 0; s2_i < s2; s2_i++){
			totals[n] += meanS1[s2_i] / s2;	
			}

			// Set values equal to zero
			for(int s2_i = 0; s2_i < s2; s2_i++){	
			meanS1[s2_i] = 0;
			}	
		}// end boxsize loop	
		
		//Computing log values for plot 
		//Change sequence of entries to start with a pixel
		double[]lnTotals = new double[numBoxes];
		double[]lnEps    = new double[numBoxes];
		for (int n = 0; n < numBoxes; n++) {
			for (int b = 0; b < numBands; b++) {
				if (totals[n] <= 1) {
					//lnTotals[numBoxes - n - 1][b] = 0.0; //Math.log(Float.MIN_VALUE); // damit logarithmus nicht undefiniert ist//IQM
					lnTotals[n] = 0.0;
				} else if (Double.isNaN(totals[n])) {
					//lnTotals[numBoxes - n - 1][b] = 0.0;
					lnTotals[n] = Double.NaN;
				} else {
					//lnTotals[numBoxes - n - 1][b] = Math.log(totals[n][b]);//IQM
					lnTotals[n] = Math.log(totals[n]); //
				}
				//lnEps[n][b] = Math.log(eps[numBoxes - n - 1 ][b]); //IQM
				lnEps[n] = Math.log(eps[n]);
				//logService.info(this.getClass().getName() + " n:" + n + " eps:  " + eps[n]);
				//logService.info(this.getClass().getName() + " n:" + n + " lnEps:  "+  lnEps[n]);
				//logService.info(this.getClass().getName() + " n:" + n + " totals[n][b]: " + totals[n]);
			}
		}
		
		//Create double log plot
		boolean isLineVisible = false; //?
	
		// Plot //nur ein Band!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		double[] lnDataX = new double[numBoxes];
		double[] lnDataY = new double[numBoxes];
			
		for (int n = 0; n < numBoxes; n++) {	
			lnDataY[n] = lnTotals[n];		
			lnDataX[n] = lnEps[n];
		}
		// System.out.println("FractalDimensionBoxCounting: dataY: "+ dataY);
		// System.out.println("FractalDimensionBoxCounting: dataX: "+ dataX);
	
		if (optShowPlot) {			
			String preName = "";
			if (numSlices > 1) {
				preName = "Slice-"+String.format("%03d", plane) +"-";
			}
			RegressionPlotFrame doubleLogPlot = DisplayRegressionPlotXY(lnDataX, lnDataY, isLineVisible, "Double Log Plot - Tug of war dimension", 
					preName + datasetName, "ln(Box width)", "ln(Count)", "",
					regMin, regMax);
			doubleLogPlotList.add(doubleLogPlot);
		}
		
		// Compute regression
		LinearRegression lr = new LinearRegression();
		regressionParams = lr.calculateParameters(lnDataX, lnDataY, regMin, regMax);
		//0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
		
		
		return regressionParams;
		//Output
		//uiService.show(tableName, table);
		//result = ops.create().img(image, new FloatType());
		//table
	}

private int getPrimeNumber(){
		
		int[] primeNumbers = {
			  46349,48533,44123,
			  46351,48539,44129,
			  46381,48541,44131,
			  46399,48563,44159,
			  46411,48571,44171,
			  46439,48589,44179,
			  46441,48593,44189,
			  46447,48611,44201,
			  46451,48619,44203,
			  46457,48623,44207,
			  46471,48647,44221,
			  46477,48649,44249,
			  46489,48661,44257,
			  46499,48673,44263,
			  46507,48677,44267,
			  46511,48679,44269,
			  46523,48731,44273,
			  46549,48733,44279,
			  46559,48751,44281,
			  46567,48757,44293,
			  46573,48761,44351,
			  46589,48767,44357,
			  46591,48779,44371,
			  46601,48781,44381,
			  46619,48787,44383,
			  46633,48799,44389,
			  46639,48809,44417,
			  46643,48817,44449,
			  46649,48821,44453,
			  46663,48823,44483,
			  46679,48847,44491,
			  46681,48857,44497,
			  46687,48859,44501,
			  46691,48869,44507,
			  46703,48871,44519,
			  46723,48883,44531,
			  46727,48889,44533,
			  46747,48907,44537,
			  46751,48947,44543,
			  46757,48953,44549,
			  46769,48973,44563,
			  46771,48989,44579,
			  46807,48991,44587,
			  46811,49003,44617,
			  46817,49009,44621,
			  46819,49019,44623,
			  46829,49031,44633,
			  46831,49033,44641,
			  46853,49037,44647,
			  46861,49043,44651,
			  46867,49057,44657,
			  46877,49069,44683,
			  46889,49081,44687,
			  46901,49103,44699,
			  46919,49109,44701,
			  46933,49117,44711,
			  46957,49121,44729,
			  46993,49123,44741,
			  46997,49139,44753,
			  47017,49157,44771,
			  47041,49169,44773,
			  47051,49171,44777,
			  47057,49177,44789,
			  47059,49193,44797,
			  47087,49199,44809,
			  47093,49201,44819,
			  47111,49207,44839,
			  47119,49211,44843,
			  47123,49223,44851,
			  47129,49253,44867,
			  47137,49261,44879,
			  47143,49277,44887,
			  47147,49279,44893,
			  47149,49297,44909,
			  47161,49307,44917,
			  47189,49331,44927,
			  47207,49333,44939,
			  47221,49339,44953,
			  47237,49363,44959,
			  47251,49367,44963,
			  47269,49369,44971,
			  47279,49391,44983,
			  47287,49393,44987,
			  47293,49409,45007,
			  47297,49411,45013,
			  47303,49417,45053,
			  47309,49429,45061,
			  47317,49433,45077,
			  47339,49451,45083,
			  47351,49459,45119,
			  47353,49463,45121,
			  47363,49477,45127,
			  47381,49481,45131,
			  47387,49499,45137,
			  47389,49523,45139,
			  47407,49529,45161,
			  47417,49531,45179,
			  47419,49537,45181,
			  47431,49547,45191,
			  47441,49549,45197,
			  47459,49559,45233,
			  47491,49597,45247,
			  47497,49603,45259,
			  47501,49613,45263,
			  47507,49627,45281,
			  47513,49633,45289,
			  47521,49639,45293,
			  47527,49663,45307,
			  47533,49667,45317,
			  47543,49669,45319,
			  47563,49681,45329,
			  47569,49697,45337,
			  47581,49711,45341,
			  47591,49727,45343,
			  47599,49739,45361,
			  47609,49741,45377,
			  47623,49747,45389,
			  47629,49757,45403,
			  47639,49783,45413,
			  47653,49787,45427,
			  47657,49789,45433,
			  47659,49801,45439,
			  47681,49807,45481,
			  47699,49811,45491,
			  47701,49823,45497,
			  47711,49831,45503,
			  47713,49843,45523,
			  47717,49853,45533,
			  47737,49871,45541,
			  47741,49877,45553,
			  47743,49891,45557,
			  47777,49919,45569,
			  47779,49921,45587,
			  47791,49927,45589,
			  47797,49937,45599,
			  47807,49939,45613,
			  47809,49943,45631,
			  47819,49957,45641,
			  47837,49991,45659,
			  47843,49993,45667,
			  47857,49999,45673,
			  47869,50021,45677,
			  47881,50023,45691,
			  47903,50033,45697,
			  47911,50047,45707,
			  47917,50051,45737,
			  47933,50053,45751,
			  47939,50069,45757,
			  47947,50077,45763,
			  47951,50087,45767,
			  47963,50093,45779,
			  47969,50101,45817,
			  47977,50111,45821,
			  47981,50119,45823,
			  48017,50123,45827,
			  48023,50129,45833,
			  48029,50131,45841,
			  48049,50147,45853,
			  48073,50153,45863,
			  48079,50159,45869,
			  48091,50177,45887,
			  48109,50207,45893,
			  48119,50221,45943,
			  48121,50227,45949,
			  48131,50231,45953,
			  48157,50261,45959,
			  48163,50263,45971,
			  48179,50273,45979,
			  48187,50287,45989,
			  48193,50291,46021,
			  48197,50311,46027,
			  48221,50321,46049,
			  48239,50329,46051,
			  48247,50333,46061,
			  48259,50341,46073,
			  48271,50359,46091,
			  48281,50363,46093,
			  48299,50377,46099,
			  48311,50383,46103,
			  48313,50387,46133,
			  48337,50411,46141,
			  48341,50417,46147,
			  48353,50423,46153,
			  48371,50441,46171,
			  48383,50459,46181,
			  48397,50461,46183,
			  48407,50497,46187,
			  48409,50503,46199,
			  48413,50513,46219,
			  48437,50527,46229,
			  48449,50539,46237,
			  48463,50543,46261,
			  48473,50549,46271,
			  48479,50551,46273,
			  48481,50581,46279,
			  48487,50587,46301,
			  48491,50591,46307,
			  48497,50593,46309,
			  48523,50599,46327,
			  48527,50627,44119
			};
		
		int randn = (int) (Math.random()*150);
		return primeNumbers[randn]; 	
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
			ra.setPosition(pos[0], 0);
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
		//ij.command().run(FractalDimensionTugOfWar.class, true).get().getOutput("image");
		ij.command().run(FractalDimensionTugOfWar.class, true);
	}
}

