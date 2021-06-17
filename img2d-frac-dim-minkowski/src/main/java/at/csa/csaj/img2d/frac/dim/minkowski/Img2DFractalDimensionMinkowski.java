/*-
 * #%L
 * Project: ImageJ plugin for computing fractal Minkowski dimension
 * File: Img2DFractalDimensionMinkowski.java
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

package at.csa.csaj.img2d.frac.dim.minkowski;

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
import net.imagej.display.DefaultImageDisplayService;
import net.imagej.display.ImageDisplay;
import net.imagej.display.ImageDisplayService;
import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.morphology.Dilation;
import net.imglib2.algorithm.morphology.Erosion;
import net.imglib2.algorithm.neighborhood.HorizontalLineShape;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.algorithm.neighborhood.Shape;
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
import org.scijava.ui.UserInterface;
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
 * <the fractal Minkowski dimension </a>
 * of an image.
 */
@Plugin(type = InteractiveCommand.class, 
        headless = true,
        label = "Minkowski dimension", menu = {
        @Menu(label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT, mnemonic = MenuConstants.PLUGINS_MNEMONIC),
        @Menu(label = "ComsystanJ"),
        @Menu(label = "Image (2D)"),
        @Menu(label = "Minkowski dimension", weight = 5)})
public class Img2DFractalDimensionMinkowski<T extends RealType<T>> extends InteractiveCommand implements Command, Previewable { //non blocking GUI
//public class Img2DFractalDimensionMinkowski<T extends RealType<T>> implements Command {	//modal GUI
	
	private static final String PLUGIN_LABEL            = "<html><b>Computes fractal Minkowski dimension</b></html>";
	private static final String SPACE_LABEL             = "";
	private static final String REGRESSION_LABEL        = "<html><b>Regression parameters</b></html>";
	private static final String MORPHOLOGYOPTIONS_LABEL = "<html><b>Morphology options</b></html>";
	private static final String BACKGROUNDOPTIONS_LABEL = "<html><b>Background option</b></html>";
	private static final String DISPLAYOPTIONS_LABEL    = "<html><b>Display options</b></html>";
	private static final String PROCESSOPTIONS_LABEL    = "<html><b>Process options</b></html>";
	
	private static Img<FloatType> imgFloat; 
	private static Img<FloatType> imgU;
	private static Img<FloatType> imgB; 
	private static Img<FloatType> imgUplusOne;
	private static Img<FloatType> imgBminusOne; 
	private static Img<FloatType> imgUDil;
	private static Img<FloatType> imgBErode;
	private static Img<FloatType> imgV;
	private static Img<UnsignedByteType>imgUnsignedByte;
	private static RandomAccess<FloatType> raF1;
	private static RandomAccess<FloatType> raF2;
	private static RandomAccessibleInterval<FloatType> raiF;	
	private static Cursor<UnsignedByteType> cursorUBT = null;
	private static Cursor<FloatType> cursorF = null;
	private static String datasetName;
	private static String[] sliceLabels;
	private static long width  = 0;
	private static long height = 0;
	private static long numDimensions = 0;
	private static long numSlices  = 0;
	private static ArrayList<RegressionPlotFrame> doubleLogPlotList = new ArrayList<RegressionPlotFrame>();
	private static double[][] resultValuesTable; //first column is the image index, second column are the corresponding regression values
	private static final String tableName = "Table - Minkowski dimension";
	
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
	
	@Parameter
	private DefaultImageDisplayService defaultImageDisplayService;
	
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

    @Parameter(label = "Number of dilations",
    		   description = "Number of dilations to increase object size",
	       	   style = NumberWidget.SPINNER_STYLE,
	           min = "1",
	           max = "32768",
	           stepSize = "1",
	           persist  = false,  //restore previous value default = true
	           initializer = "initialNumDilations",
	           callback    = "callbackNumDilations")
    private int spinnerInteger_NumDilations;
    
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
     private final String labelMethodOptions = MORPHOLOGYOPTIONS_LABEL;
     
     @Parameter(label = "Kernel shape type",
  		    description = "Shape of morphological structuring element",
  		    style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
    		    choices = {"Square", "Horizontal", "Vertical"},
    		    //persist  = false,  //restore previous value default = true
  		    initializer = "initialShapeType",
              callback = "callbackShapeType")
      private String choiceRadioButt_ShapeType;
     
     @Parameter(label = "Morphological operator",
 		    description = "Type of image and according morphological operation",
 		    style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
   		    choices = {"Binary dilation", "Blanket dilation/erosion"},
   		    //persist  = false,  //restore previous value default = true
 		    initializer = "initialMorphologicalOperator",
             callback = "callbackMorphologicalOperator")
     private String choiceRadioButt_MorphologicalOperator;
     
 	//-----------------------------------------------------------------------------------------------------
     @Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
     private final String labelDisplayOptions = DISPLAYOPTIONS_LABEL;
      
     @Parameter(label = "Show double log plot",
    		    //persist  = false,  //restore previous value default = true
  		        initializer = "initialShowDoubleLogPlots")
	 private boolean booleanShowDoubleLogPlot;
     
     @Parameter(label = "Show last dilated/eroded image",
 		    //persist  = false,  //restore previous value default = true
		        initializer = "initialShowLastMorphImg")
	 private boolean booleanShowLastMorphImg;
         
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
//@Parameter(label   = "Process single active image ",
//		    callback = "callbackProcessActiveImage")
//	private Button buttonProcessActiveImage;
     
     @Parameter(label   = "Process all available images",
 		        callback = "callbackProcessAllImages")
	 private Button buttonProcessAllImages;

    //---------------------------------------------------------------------
 
    //The following initialzer functions set initial values
    protected void initialNumDilations() {
      	spinnerInteger_NumDilations = 20;
    }
    protected void initialRegMin() {
    	spinnerInteger_RegMin = 1;
    }
    protected void initialRegMax() {
    	spinnerInteger_RegMax =  20;
    }
    protected void initialShapeType() {
    	choiceRadioButt_ShapeType = "Square";
    }
    protected void initialMorphologicalOperator() {
    	choiceRadioButt_MorphologicalOperator = "Binary dilation";
    }
    protected void initialShowDoubleLogPlots() {
    	booleanShowDoubleLogPlot = true;
    }
    protected void initialShowLastMorphImg() {
    	booleanShowLastMorphImg = true;
    }
    protected void initialOverwriteDisplays() {
    	booleanOverwriteDisplays = true;
    }
	protected void initialNumImageSlice() {
    	spinnerInteger_NumImageSlice = 1;
	}
    
	// The following method is known as "callback" which gets executed
	// whenever the value of a specific linked parameter changes.
	/** Executed whenever the {@link #spinInteger_NumBoxes} parameter changes. */
	protected void callbackNumDilations() {
		
		if  (spinnerInteger_NumDilations < 3) {
			spinnerInteger_NumDilations = 3;
		}
		if (spinnerInteger_RegMax > spinnerInteger_NumDilations) {
			spinnerInteger_RegMax = spinnerInteger_NumDilations;
		}
		if (spinnerInteger_RegMin >= spinnerInteger_RegMax - 2) {
			spinnerInteger_RegMin = spinnerInteger_RegMax - 2;
		}
		logService.info(this.getClass().getName() + " Number of dilations set to " + spinnerInteger_NumDilations);
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
		if (spinnerInteger_RegMax > spinnerInteger_NumDilations) {
			spinnerInteger_RegMax = spinnerInteger_NumDilations;
		}
		
		logService.info(this.getClass().getName() + " Regression Max set to " + spinnerInteger_RegMax);
	}

	/** Executed whenever the {@link #choiceRadioButt_ShapeType} parameter changes. */
	protected void callbackShapeType() {
		logService.info(this.getClass().getName() + " Shape type set to " + choiceRadioButt_ShapeType);
		
	}
	
	/** Executed whenever the {@link #choiceRadioButt_MorphologicalOperator} parameter changes. */
	protected void callbackMorphologicalOperator() {
		logService.info(this.getClass().getName() + " Morphological operator set to " + choiceRadioButt_MorphologicalOperator);
		
	}
	
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
				
		//dlgProgress = new WaitingDialogWithProgressBar("<html>Computing Minkowski dimensions, please wait...<br>Open console window for further info.</html>");
		dlgProgress = new WaitingDialogWithProgressBar("Computing Minkowski dimensions, please wait... Open console window for further info.",
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
				
		//dlgProgress = new WaitingDialogWithProgressBar("<html>Computing Minkowski dimensions, please wait...<br>Open console window for further info.</html>");
		dlgProgress = new WaitingDialogWithProgressBar("Computing Minkowski dimensions, please wait... Open console window for further info.",
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
				
		//dlgProgress = new WaitingDialogWithProgressBar("<html>Computing Minkowski dimensions, please wait...<br>Open console window for further info.</html>");
		dlgProgress = new WaitingDialogWithProgressBar("Computing Minkowski dimensions, please wait... Open console window for further info.",
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
		
		boolean optDeleteExistingPlots  = false;
		boolean optDeleteExistingTables = false;
		boolean optDeleteExistingImgs   = false;
		if (booleanOverwriteDisplays) {
			optDeleteExistingPlots  = true;
			optDeleteExistingTables = true;
			optDeleteExistingImgs   = true;
		}
		
		if (optDeleteExistingImgs){
//			List<Display<?>> list = defaultDisplayService.getDisplays();
//			for (int i = 0; i < list.size(); i++) {
//				display = list.get(i);
//				System.out.println("display name: " + display.getName());
//				if (display.getName().equals("Last dilated image")) display.close(); //does not close correctly in Fiji, it is only not available any more
//				if (display.getName().equals("Last eroded image"))  display.close();
//			}			
//			List<ImageDisplay> listImgs = defaultImageDisplayService.getImageDisplays(); //Does not also close in Fiji
		
			Frame frame;
			Frame[] listFrames = JFrame.getFrames();
			for (int i = listFrames.length -1 ; i >= 0; i--) { //Reverse order, otherwise focus is not given free from the last image
				frame = listFrames[i];
				System.out.println("display name: " + frame.getTitle());
				if (frame.getTitle().equals("Last dilated image")) {
					frame.setVisible(false); //Successfully closes also in Fiji
					frame.dispose();
				}
				if (frame.getTitle().equals("Last eroded image")) {
					frame.setVisible(false); //Successfully closes also in Fiji
					frame.dispose();
				}
			}
		}
		
		Display<?> display;
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
				display = list.get(i);
				System.out.println("display name: " + display.getName());
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
		
		if      (choiceRadioButt_MorphologicalOperator.equals("Binary dilation"))          dim  = 2-regressionValues[1]; //Standard Dm according to Peleg et al.
		else if (choiceRadioButt_MorphologicalOperator.equals("Blanket dilation/erosion")) dim  = -regressionValues[1];  //better for grey images "Dm" Dubuc etal..
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
			
				if      (choiceRadioButt_MorphologicalOperator.equals("Binary dilation"))          dim  = 2-regressionValues[1]; //Standard Dm according to Peleg et al.
				else if (choiceRadioButt_MorphologicalOperator.equals("Blanket dilation/erosion")) dim  = -regressionValues[1];  //better for grey images "Dm" Dubuc etal..
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
		IntColumn columnMaxNumBoxes        = new IntColumn("# Dilations");
		IntColumn columnRegMin             = new IntColumn("RegMin");
		IntColumn columnRegMax             = new IntColumn("RegMax");
		GenericColumn columnShapeType      = new GenericColumn("Shape type");
		GenericColumn columnMorphOp        = new GenericColumn("Morphological operator");
		DoubleColumn columnDm              = new DoubleColumn("Dm");
		DoubleColumn columnR2              = new DoubleColumn("R2");
		DoubleColumn columnStdErr          = new DoubleColumn("StdErr");
		
	    table = new DefaultGenericTable();
		table.add(columnFileName);
		table.add(columnSliceName);
		table.add(columnMaxNumBoxes);
		table.add(columnRegMin);
		table.add(columnRegMax);
		table.add(columnShapeType);
		table.add(columnMorphOp);
		table.add(columnDm);
		table.add(columnR2);
		table.add(columnStdErr);
	}
	
	/** collects current result and shows table
	 *  @param int slice number of active image.
	 */
	private void collectActiveResultAndShowTable(int sliceNumber) {
	
		int regMin           = spinnerInteger_RegMin;
		int regMax           = spinnerInteger_RegMax;
		int numDilations     = spinnerInteger_NumDilations;
		String shapeType     = choiceRadioButt_ShapeType; 
		String morphOp       = choiceRadioButt_MorphologicalOperator;	
	    int s = sliceNumber;	
			//0 Intercept, 1 Dim, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared		
			//fill table with values
			table.appendRow();
			table.set("File name",   	 table.getRowCount() - 1, datasetName);	
			if (sliceLabels != null) 	 table.set("Slice name", table.getRowCount() - 1, sliceLabels[s]);
			table.set("# Dilations",     table.getRowCount()-1, numDilations);	
			table.set("RegMin",      	 table.getRowCount()-1, regMin);	
			table.set("RegMax",      	 table.getRowCount()-1, regMax);	
			table.set("Shape type",      table.getRowCount()-1, shapeType);	
			table.set("Morphological operator",   table.getRowCount()-1, morphOp);	
			table.set("Dm",          	 table.getRowCount()-1, resultValuesTable[s][1]);
			table.set("R2",          	 table.getRowCount()-1, resultValuesTable[s][4]);
			table.set("StdErr",      	 table.getRowCount()-1, resultValuesTable[s][3]);		
		
		//Show table
		uiService.show(tableName, table);
	}
	
	/** collects all results and shows table */
	private void collectAllResultsAndShowTable() {
	
		int regMin          = spinnerInteger_RegMin;
		int regMax          = spinnerInteger_RegMax;
		int numDilations    = spinnerInteger_NumDilations;
		String shapeType    = choiceRadioButt_ShapeType; 
		String morphOp      = choiceRadioButt_MorphologicalOperator;	
		//loop over all slices
		for (int s = 0; s < numSlices; s++){ //slices of an image stack
			//0 Intercept, 1 Dim, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared		
			//fill table with values
			table.appendRow();
			table.set("File name",	   	 table.getRowCount() - 1, datasetName);	
			if (sliceLabels != null)	 table.set("Slice name", table.getRowCount() - 1, sliceLabels[s]);
			table.set("# Dilations",     table.getRowCount()-1, numDilations);	
			table.set("RegMin",      	 table.getRowCount()-1, regMin);	
			table.set("RegMax",      	 table.getRowCount()-1, regMax);	
			table.set("Shape type",      table.getRowCount()-1, shapeType);	
			table.set("Morphological operator",   table.getRowCount()-1, morphOp);		
			table.set("Dm",          	 table.getRowCount()-1, resultValuesTable[s][1]);
			table.set("R2",          	 table.getRowCount()-1, resultValuesTable[s][4]);
			table.set("StdErr",      	 table.getRowCount()-1, resultValuesTable[s][3]);		
		}
		//Show table
		uiService.show(tableName, table);
	}
							
	/** 
	 * Processing ****************************************************************************************
	 * <li> Peleg S., Naor J., Hartley R., Avnir D. Multiple Resolution Texture Analysis and Classification, IEEE Trans Patt Anal Mach Intel Vol PAMI-6 No 4 1984, 518-523 !!!
	 * <li> Y.Y. Tang, E.C.M. Lam, New method for feature extraction based on fractal behavior, Patt. Rec. 35 (2002) 1071-1081   Ref. zu Peleg etal.
	 * <li> Dubuc B., Zucker S.W., Tricot C., Quiniou J.F., Wehbi D. Evaluating the fractal dimension of surfaces, Proc.R.Soc.Lond. A 425, 113-127, 1989 
	 * */
	private double[] process(RandomAccessibleInterval<?> rai, int plane) { //plane plane (Image) number

		
		int regMin          = spinnerInteger_RegMin;
		int regMax          = spinnerInteger_RegMax;
		int numDilations    = spinnerInteger_NumDilations;
		String shapeType    = choiceRadioButt_ShapeType; 
		String MorphologicalType = choiceRadioButt_MorphologicalOperator;	 //binary  Blanket
		boolean optShowPlot    = booleanShowDoubleLogPlot;
		
		int numBands = 1;
		long width  = rai.dimension(0);
		long height = rai.dimension(1);
		
		String imageType = "8-bit";  //  "RGB"....
	
		double[] regressionParams = null;
		
		//Convert image to float
		//Img<T> image = (Img<T>) dataset.getImgPlus();
		//RandomAccessibleInterval<T> rai = (RandomAccessibleInterval<T>)dataset.getImgPlus();
		//IterableInterval ii = dataset.getImgPlus();
		//Img<FloatType> imgFloat = opService.convert().float32(ii);
		

		double[][] totals = new double[numDilations][numBands];
		// double[] totalsMax = new double[numBands]; //for binary images
		int[]      eps    = new int[numDilations];
		
		// definition of eps
		for (int n = 0; n < numDilations; n++) {
				eps[n] = n + 1;
				//logService.info(this.getClass().getName() + " n:" + n + " eps:  " + eps[n]);	
		}		
		
		//********************************Binary Image: 0 and [1, 255]! and not: 0 and 255
		if (MorphologicalType.equals("Binary dilation")) {//{"Binary dilation", "Blanket dilation/erosion"}
			//Minkowski
			//n=0  2^0 = 1 ... single pixel
			// Loop through all pixels.
			//get count(area) and copy image 
//			for (int b = 0; b < numBands; b++) {
//				cursor = (Cursor<UnsignedByteType>) Views.iterable(rai).localizingCursor();	
//				while (cursor.hasNext()) {
//					cursor.fwd();
//					//cursor.localize(pos);			
//					if (((UnsignedByteType) cursor.get()).get() > 0) totals[0][b] += 1; // Binary Image: 0 and [1, 255]! and not: 0 and 255
//					//totals[n][b] = totals[n][b]; // / totalsMax[b];
//				}
//			}//b
			imgUnsignedByte = createImgUnsignedByte(rai); //This copies the image, otherwise the original image would be dilated
			Shape kernel = null;
			if (shapeType.equals("Square"))          kernel = new RectangleShape(1, false); //3x3kernel skipCenter = false
			else if (shapeType.equals("Horizontal")) kernel = new HorizontalLineShape(1, 0, false); //1,0 ..one step horizontal 1,1.. one step vertical
			else if (shapeType.equals("Vertical"))   kernel = new HorizontalLineShape(1, 1, false); //1,0 ..one step horizontal 1,1.. one step vertical
			Runtime runtime  = Runtime.getRuntime();
			long maxMemory   = runtime.maxMemory();
			long totalMemory = runtime.totalMemory();
			long freeMemory  = runtime.freeMemory();
			int availableProcessors = runtime.availableProcessors();
			//System.out.println("available processors: " + availableProcessors);
			int numThreads = 6; //For dilation //with 6 it was 3 times faster than only with one thread
			if (numThreads > availableProcessors) numThreads = availableProcessors;
			long[] pos = new long[2];
			long[] min = new long[] {0,0};
			long[] max = new long[] {rai.dimension(0), rai.dimension(1)};
			Interval interval = new FinalInterval(min, max);
			for (int b = 0; b < numBands; b++) {
				for (int n = 0; n < numDilations; n++) { //	
					//Compute dilated image
					Dilation.dilateInPlace(imgUnsignedByte, interval, kernel, numThreads); //dilateds image
					//uiService.show("Dilated image", imgUnsignedByte);
					if ((booleanShowLastMorphImg)&&(n == numDilations -1)) uiService.show("Last dilated image", imgUnsignedByte);
					cursorUBT = imgUnsignedByte.localizingCursor();	
					while (cursorUBT.hasNext()) {
						cursorUBT.fwd();
						//cursor.localize(pos);			
						if (((UnsignedByteType) cursorUBT.get()).get() > 0) totals[n][b] += 1; // Binary Image: 0 and [1, 255]! and not: 0 and 255
						//totals[n][b] = totals[n][b]; // / totalsMax[b];
					}		                      
				} //n
			}//b band		
		}
		//*******************************Grey Value Image
		else if (MorphologicalType.equals("Blanket dilation/erosion")) {// {"Binary dilation", "Blanket dilation/erosion"}
			imgFloat = createImgFloat(rai); //This copies the image, otherwise the original image would be dilated
			imgU = imgFloat.copy();
			imgB = imgFloat.copy();
			imgUplusOne  = imgFloat.copy();
			imgBminusOne = imgFloat.copy();
			imgV = imgFloat.copy();
			
			Shape kernel = null;
			if (shapeType.equals("Square"))          kernel = new RectangleShape(1, false); //3x3kernel skipCenter = false
			else if (shapeType.equals("Horizontal")) kernel = new HorizontalLineShape(1, 0, false); //1,0 ..one step horizontal 1,1.. one step vertical
			else if (shapeType.equals("Vertical"))   kernel = new HorizontalLineShape(1, 1, false); //1,0 ..one step horizontal 1,1.. one step vertical
			Runtime runtime  = Runtime.getRuntime();
			long maxMemory   = runtime.maxMemory();
			long totalMemory = runtime.totalMemory();
			long freeMemory  = runtime.freeMemory();
			int availableProcessors = runtime.availableProcessors();
			//System.out.println("available processors: " + availableProcessors);
			int numThreads = 6; //For dilation //with 6 it was 3 times faster than only with one thread
			if (numThreads > availableProcessors) numThreads = availableProcessors;
			long[] pos = new long[2];
			float sample1;
			float sample2;

		
			for (int n = 0; n < numDilations; n++) { //
				
				//Add plus one to imgU
				cursorF = imgU.localizingCursor();
				raF1 = imgUplusOne.randomAccess();
				while (cursorF.hasNext()) {
					cursorF.fwd();	
					cursorF.localize(pos);
					raF1.setPosition(pos);
					raF1.get().set(cursorF.get().get() + 1f); 
				}			
				//Dilated imgU
				imgUDil = Dilation.dilate(imgU, kernel, numThreads);
				//uiService.show("Dilated image", imgUDil);
				if ((booleanShowLastMorphImg)&&(n == numDilations -1)) uiService.show("Last dilated image", imgUDil);
				
				//Get Max and overwrite imgU
				cursorF = imgU.localizingCursor();
				raF1 = imgUplusOne.randomAccess();
				raF2 = imgUDil.randomAccess();
				while (cursorF.hasNext()) {
					cursorF.fwd();
					cursorF.localize(pos);
					raF1.setPosition(pos);
					raF2.setPosition(pos);
					sample1 = raF1.get().get();
					sample2 = raF2.get().get();
					if ((sample2 > sample1)) cursorF.get().set(sample2);
					else cursorF.get().set(sample1);
				}	
				
				//Subtract one to imgB
				cursorF = imgB.localizingCursor();
				raF1 = imgBminusOne.randomAccess();
				while (cursorF.hasNext()) {
					cursorF.fwd();	
					cursorF.localize(pos);
					raF1.setPosition(pos);
					raF1.get().set(cursorF.get().get() - 1f); 
				}			
				//Erode imgB
				imgBErode = Erosion.erode(imgB, kernel, numThreads);
				//uiService.show("Eroded image", imgBErode);
				if ((booleanShowLastMorphImg)&&(n == numDilations -1)) uiService.show("Last eroded image", imgBErode);
				
				//Get Min and overwrite imgB
				cursorF = imgB.localizingCursor();
				raF1 = imgBminusOne.randomAccess();
				raF2 = imgBErode.randomAccess();
				while (cursorF.hasNext()) {
					cursorF.fwd();
					cursorF.localize(pos);
					raF1.setPosition(pos);
					raF2.setPosition(pos);
					sample1 = raF1.get().get();
					sample2 = raF2.get().get();
					if ((sample2 < sample1)) cursorF.get().set(sample2);
					else cursorF.get().set(sample1);
				}	
				
				//Compute volume imgV
				cursorF = imgV.localizingCursor();
				raF1 = imgU.randomAccess();
				raF2 = imgB.randomAccess();
				while (cursorF.hasNext()) {
					cursorF.fwd();
					cursorF.localize(pos);
					raF1.setPosition(pos);
					raF2.setPosition(pos);
					sample1 = raF1.get().get();
					sample2 = raF2.get().get();
					cursorF.get().set(sample1 - sample2);
				}	
				
				//Get counts with imgV
				cursorF = imgV.localizingCursor();
				raF1 = imgU.randomAccess();
				raF2 = imgB.randomAccess();
				while (cursorF.hasNext()) {
					cursorF.fwd();
					for (int b = 0; b < numBands; b++) {
						totals[n][b] = totals[n][b] + cursorF.get().get(); //totalAreas
					}
				}
				
				// for (int b = 0; b < numBands; b++) totalAreas[ee][b] =
				// totalAreas[n][b] / (2* (n+1)); //Peleg et al.
				// better is following according to Dubuc et al. equation 9
				for (int b = 0; b < numBands; b++)
					totals[n][b] = totals[n][b] / ((n+ 1) * (n + 1) * (n + 1)); // eq.9 Dubuc et.al.
				
				                      
			} //n
		}
			
		//Computing log values for plot 
		//Change sequence of entries to start with a pixel
		double[][] lnTotals = new double[numDilations][numBands];
		double[]   lnEps    = new double[numDilations];
		for (int n = 0; n < numDilations; n++) {
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
				lnEps[n] = Math.log(eps[n]);
				//logService.info(this.getClass().getName() + " n:" + n + " eps:  " + eps[n]);
				//logService.info(this.getClass().getName() + " n:" + n + " lnEps:  "+  lnEps[n][b] );
				//logService.info(this.getClass().getName() + " n:" + n + " totals[n][b]: " + totals[n][b]);
			}
		}
		
		//Create double log plot
		boolean isLineVisible = false; //?
		for (int b = 0; b < numBands; b++) { // mehrere Bands
			
			// Plot //nur ein Band!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
			double[] lnDataX = new double[numDilations];
			double[] lnDataY = new double[numDilations];
				
			for (int n = 0; n < numDilations; n++) {	
				lnDataY[n] = lnTotals[n][b];		
				lnDataX[n] = lnEps[n];
			}
		
			if (optShowPlot) {			
				String preName = "";
				if (numSlices > 1) {
					preName = "Slice-"+String.format("%03d", plane) +"-";
				}
				RegressionPlotFrame doubleLogPlot = DisplayRegressionPlotXY(lnDataX, lnDataY, isLineVisible, "Double log plot - Minkowski Dimension", 
						preName + datasetName, "ln(Dilation span)", "ln(Area)", "",
						regMin, regMax);
				doubleLogPlotList.add(doubleLogPlot);
			}
			
			// Compute regression
			LinearRegression lr = new LinearRegression();
			regressionParams = lr.calculateParameters(lnDataX, lnDataY, regMin, regMax);
			//0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
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
		//ij.command().run(Img2DFractalDimensionMinkowski.class, true).get().getOutput("image");
		ij.command().run(Img2DFractalDimensionMinkowski.class, true);
	}
}

