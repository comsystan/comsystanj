/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj2DFracDimPyramidCmd.java
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
package at.csa.csaj.plugin2d.frac;

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
import net.imglib2.RealInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.FloorInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.LanczosInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
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
import org.scijava.table.DefaultGenericTable;
import org.scijava.table.DoubleColumn;
import org.scijava.table.GenericColumn;
import org.scijava.table.IntColumn;
import org.scijava.ui.UIService;
import org.scijava.widget.Button;
import org.scijava.widget.FileWidget;
import org.scijava.widget.NumberWidget;
import at.csa.csaj.commons.CsajDialog_WaitingWithProgressBar;
import at.csa.csaj.commons.CsajPlot_RegressionFrame;
import at.csa.csaj.commons.CsajRegression_Linear;
import at.csa.csaj.commons.CsajCheck_ItemIn;
import at.csa.csaj.commons.CsajContainer_ProcessMethod;

/**
 * A {@link ContextCommand} plugin computing
 * <the fractal pyramid dimension </a>
 * of an image.
 */
@Plugin(type = ContextCommand.class, 
        headless = true,
        label = "Pyramid dimension",
        initializer = "initialPluginLaunch",
        iconPath = "/icons/comsystan-logo-grey46-16x16.png", //Menu entry icon
        menu = {})

public class Csaj2DFracDimPyramidCmd<T extends RealType<T>> extends ContextCommand implements Previewable {
	
	private static final String PLUGIN_LABEL            = "<html><b>Computes fractal dimension with an image pyramid";
	private static final String SPACE_LABEL             = "";
	private static final String REGRESSION_LABEL        = "<html><b>Regression parameters</b></html>";
	private static final String INTERPOLATION_LABEL     = "<html><b>Interpolation options</b></html>";
	private static final String BACKGROUNDOPTIONS_LABEL = "<html><b>Background option</b></html>";
	private static final String DISPLAYOPTIONS_LABEL    = "<html><b>Display options</b></html>";
	private static final String PROCESSOPTIONS_LABEL    = "<html><b>Process options</b></html>";
	
	
	private static Img<FloatType> imgFloat; 
	private static Img<FloatType> imgSubSampled;
	private static double[][] imgArr;
	private static double[][] imgArrPolySurface;
	private static Cursor<FloatType> cursorF = null;
	private static String datasetName;
	private static String[] sliceLabels;
	
	private static boolean isBinary = true; //To switch between binary and grey value algorithm in developer modus.
	
	private static long width  = 0;
	private static long height = 0;
	private static long numDimensions = 0;
	private static long numSlices = 0;
	private static long compositeChannelCount =0;
	private static String imageType = "";
	private static int  numbMaxPyramidImages = 0;
	private static ArrayList<CsajPlot_RegressionFrame> doubleLogPlotList = new ArrayList<CsajPlot_RegressionFrame>();
	
	public static final String TABLE_OUT_NAME = "Table - Pyramid dimension";
	
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
	
	//This parameter does not work in an InteractiveCommand plugin (duplicate displayService error during startup) pom-scijava 24.0.0
	//in Command Plugin no problem
	//@Parameter  
	//private DisplayService displayService;
	
	@Parameter  //This works in an InteractiveCommand plugin
    private DefaultDisplayService defaultDisplayService;
	
	@Parameter
	private DatasetService datasetService;

	@Parameter(label = TABLE_OUT_NAME, type = ItemIO.OUTPUT)
	private DefaultGenericTable tableOut;

	
    //Widget elements------------------------------------------------------
	//-----------------------------------------------------------------------------------------------------
	//@Parameter(label = " ", visibility = ItemVisibility.MESSAGE,   persist = false)
	//private final String labelPlugin = PLUGIN_LABEL;
	
	//@Parameter(label = " ", visibility = ItemVisibility.MESSAGE,    persist = false)
	//private final String labelSpace = SPACE_LABEL;
	
	//Input dataset which is updated in callback functions
	@Parameter (type = ItemIO.INPUT)
	private Dataset datasetIn;
	
	//-----------------------------------------------------------------------------------------------------
	@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String labelRegression = REGRESSION_LABEL;
	
	@Parameter(label = "Pyramid images #",
			   description = "Number of subsequently half sized images",
	       	   style = NumberWidget.SPINNER_STYLE,
	           min = "3",
	           max = "32768",
	           stepSize = "1",
	           persist = false,  //restore previous value default = true
	           initializer = "initialNumImages",
	           callback    = "callbackNumImages")
	private int spinnerInteger_PyramidImages;
    
    @Parameter(label = "Regression Start",
    		   description = "Minimum x value of linear regression",
 		       style = NumberWidget.SPINNER_STYLE,
 		       min = "1",
 		       max = "32768",
 		       stepSize = "1",
 		       persist = false,   //restore previous value default = true
 		       initializer = "initialNumRegStart",
 		       callback = "callbackNumRegStart")
    private int spinnerInteger_NumRegStart = 1;
 
    @Parameter(label = "Regression End",
    		   description = "Maximum x value of linear regression",
    		   style = NumberWidget.SPINNER_STYLE,
		       min = "3",
		       max = "32768",
		       stepSize = "1",
		       persist = false,   //restore previous value default = true
		       initializer = "initialNumRegEnd",
		       callback = "callbackNumRegEnd")
     private int spinnerInteger_NumRegEnd = 3;
    
     //-----------------------------------------------------------------------------------------------------
//     @Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
//     private final String labelInterpolation = INTERPOLATION_LABEL;
     
//     @Parameter(label = "Interpolation",
//    		    description = "Type of interpolation for subscaled images",
//    		    style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
//      		    choices = {"Linear", "Floor", "Lanczos", "Nearest Neighbor"},
//      		    //persist = false,  //restore previous value default = true
//    		    initializer = "initialInterpolation",
//                callback = "callbackInterpolation")
//     private String choiceRadioButt_Interpolation;
     
     //-----------------------------------------------------------------------------------------------------
     @Parameter(label = " ", visibility = ItemVisibility.MESSAGE,   persist = false)
     private final String labelDisplayOptions = DISPLAYOPTIONS_LABEL;
      
     @Parameter(label = "Show double log plot",
    		    persist = true,  //restore previous value default = true
  		        initializer = "initialShowDoubleLogPlots")
	 private boolean booleanShowDoubleLogPlot;
      
     @Parameter(label = "Show downscaled images",
    		    persist = true,  //restore previous value default = true   
    		    initializer = "initialShowImages")
 	 private boolean booleanShowImages;
     
     @Parameter(label = "Overwrite result display(s)",
    	    	description = "Overwrite already existing result images, plots or tables",
    	    	persist = true,  //restore previous value default = true
    			initializer = "initialOverwriteDisplays")
    private boolean booleanOverwriteDisplays;
     
     //-----------------------------------------------------------------------------------------------------
     @Parameter(label = " ", visibility = ItemVisibility.MESSAGE,  persist = false)
     private final String labelProcessOptions = PROCESSOPTIONS_LABEL;
     
     @Parameter(label = "Immediate processing", visibility = ItemVisibility.INVISIBLE, persist = false,
    	    	description = "Immediate processing of active image when a parameter is changed",
    			callback = "callbackProcessImmediately")
    private boolean booleanProcessImmediately;
     
 	@Parameter(label = "OK - process image #", description = "Image slice number", style = NumberWidget.SPINNER_STYLE, min = "1", max = "99999999", stepSize = "1",
			   persist = false, // restore  previous value  default  =  true
			   initializer = "initialNumImageSlice",
			   callback = "callbackNumImageSlice")
	private int spinnerInteger_NumImageSlice;
 	
	@Parameter(label = "OK - process all images",
			   description = "Set for final Command.run execution",
			   persist = false, // restore  previous value  default  =  true
			   initializer = "initialProcessAll")
	private boolean processAll;
	
	@Parameter(label = "   Preview of single image #    ", callback = "callbackProcessSingleImage")
	private Button buttonProcessSingelImage;
	
//	Deactivated, because it does not work in Fiji (although it works in ImageJ2 -Eclipse)	
//	@Parameter(label = "Preview of single active image ", callback = "callbackProcessActiveImage")
//	private Button buttonProcessActiveImage;
     
	@Parameter(label = "Preview of all available images", callback = "callbackProcessAllImages")
	private Button buttonProcessAllImages;

    //---------------------------------------------------------------------
    //The following initializer functions set initial values
	protected void initialPluginLaunch() {
		checkItemIOIn();
	}
    protected void initialNumImages() {
    	if (datasetIn == null) {
    		logService.error(this.getClass().getName() + " ERROR: Input image = null");
    		cancel("ComsystanJ 2D plugin cannot be started - missing input image.");
    		return;
    	} else {
    		numbMaxPyramidImages = getMaxPyramidNumber(datasetIn.dimension(0), datasetIn.dimension(1));
    	}
      	spinnerInteger_PyramidImages = numbMaxPyramidImages;
    }
    protected void initialNumRegStart() {
    	spinnerInteger_NumRegStart = 1;
    }
    protected void initialNumRegEnd() {
      	if (datasetIn == null) {
    		logService.error(this.getClass().getName() + " ERROR: Input image = null");
    		cancel("ComsystanJ 2D plugin cannot be started - missing input image.");
    		return;
    	} else {
    		numbMaxPyramidImages = getMaxPyramidNumber(datasetIn.dimension(0), datasetIn.dimension(1));
    	}
    	spinnerInteger_NumRegEnd =  numbMaxPyramidImages;
    }
//    protected void initialInterpolation() {
//    	choiceRadioButt_Interpolation = "Linear";
//    }
    protected void initialShowDoubleLogPlots() {
    	booleanShowDoubleLogPlot = true;
    }
    protected void initialShowImages() {
    	booleanShowImages = false;
    }
    protected void initialOverwriteDisplays() {
    	booleanOverwriteDisplays = true;
    }
	protected void initialNumImageSlice() {
    	spinnerInteger_NumImageSlice = 1;
	}
  
	// ------------------------------------------------------------------------------
	
	/** Executed whenever the {@link #spinnerInteger_NumImages} parameter changes. */
	protected void callbackNumImages() {	
		if  (spinnerInteger_PyramidImages < 3) {
			spinnerInteger_PyramidImages = 3;
		}
		if  (spinnerInteger_PyramidImages > numbMaxPyramidImages) {
			spinnerInteger_PyramidImages = numbMaxPyramidImages;
		}
		if (spinnerInteger_NumRegEnd > spinnerInteger_PyramidImages) {
			spinnerInteger_NumRegEnd = spinnerInteger_PyramidImages;
		}
		if (spinnerInteger_NumRegStart >= spinnerInteger_NumRegEnd - 2) {
			spinnerInteger_NumRegStart = spinnerInteger_NumRegEnd - 2;
		}
		logService.info(this.getClass().getName() + " Number of pyramid images set to " + spinnerInteger_PyramidImages);
	}
    /** Executed whenever the {@link #spinnerInteger_NumRegStart} parameter changes. */
	protected void callbackNumRegStart() {
		if (spinnerInteger_NumRegStart >= spinnerInteger_NumRegEnd - 2) {
			spinnerInteger_NumRegStart = spinnerInteger_NumRegEnd - 2;
		}
		if(spinnerInteger_NumRegStart < 1) {
			spinnerInteger_NumRegStart = 1;
		}
		logService.info(this.getClass().getName() + " Regression Min set to " + spinnerInteger_NumRegStart);
	}
	/** Executed whenever the {@link #spinnerInteger_NumRegEnd} parameter changes. */
	protected void callbackNumRegEnd() {
		if (spinnerInteger_NumRegEnd <= spinnerInteger_NumRegStart + 2) {
			spinnerInteger_NumRegEnd = spinnerInteger_NumRegStart + 2;
		}		
		if (spinnerInteger_NumRegEnd > spinnerInteger_PyramidImages) {
			spinnerInteger_NumRegEnd = spinnerInteger_PyramidImages;
		}
		
		logService.info(this.getClass().getName() + " Regression Max set to " + spinnerInteger_NumRegEnd);
	}
//	/** Executed whenever the {@link #choiceRadioButt_Interpolation} parameter changes. */
//	protected void callbackInterpolation() {
//		logService.info(this.getClass().getName() + " Interpolation method set to " + choiceRadioButt_Interpolation);
//	}
	
	/** Executed whenever the {@link #booleanProcessImmediately} parameter changes. */
	protected void callbackProcessImmediately() {
		logService.info(this.getClass().getName() + " Process immediately set to " + booleanProcessImmediately);
	}
	
	/** Executed whenever the {@link #spinnerInteger_NumImageSlice} parameter changes. */
	protected void callbackNumImageSlice() {
		if (spinnerInteger_NumImageSlice > numSlices){
			logService.info(this.getClass().getName() + " No more images available");
			spinnerInteger_NumImageSlice = (int)numSlices;
		}
		logService.info(this.getClass().getName() + " Image slice number set to " + spinnerInteger_NumImageSlice);
	}
		
	/**
	 * Executed whenever the {@link #buttonProcessSingleImage} button is pressed.
	 * It is not executed in the same exact manner such as run()
	 * So a thread for displaying properly the Progressbar window is needed
	 * Execution of the code is then not on the Event Dispatch Thread EDT, where all GUI windows are executed
	 * The @Parameter ItemIO.OUTPUT is not automatically shown 
	 */
	protected void callbackProcessSingleImage() {
		//prepare  executer service
		exec = Executors.newSingleThreadExecutor();
	   	exec.execute(new Runnable() {
	        public void run() {
	    	    startWorkflowForSingleImage();
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
	        	startWorkflowForAllImages();
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
		    	    startWorkflowForSingleImage();
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

		checkItemIOIn();
		if (processAll) startWorkflowForAllImages();
		else            startWorkflowForSingleImage();
	
		logService.info(this.getClass().getName() + " Finished command run");
	}
	
	public void checkItemIOIn() {
		//Get input meta data
		HashMap<String, Object> datasetInInfo = CsajCheck_ItemIn.checkDatasetIn(logService, datasetIn);
		if (datasetInInfo == null) {
			logService.error(MethodHandles.lookup().lookupClass().getName() + " ERROR: Missing input image or image type is not byte or float");
			cancel("ComsystanJ 2D plugin cannot be started - missing input image or wrong image type.");
		} else {
			width  =       			(long)datasetInInfo.get("width");
			height =       			(long)datasetInInfo.get("height");
			numDimensions =         (int)datasetInInfo.get("numDimensions");
			compositeChannelCount = (int)datasetInInfo.get("compositeChannelCount");
			numSlices =             (long)datasetInInfo.get("numSlices");
			imageType =   			(String)datasetInInfo.get("imageType");
			datasetName = 			(String)datasetInInfo.get("datasetName");
			sliceLabels = 			(String[])datasetInInfo.get("sliceLabels");
			
			//RGB not allowed
			if (!imageType.equals("Grey")) { 
				logService.error(this.getClass().getName() + " ERROR: Grey value image(s) expected!");
				cancel("ComsystanJ 2D plugin cannot be started - grey value image(s) expected!");
			}
		}
	}
		
	/**
	 * This method starts the workflow for a single image of the active display
	 */
	protected void startWorkflowForSingleImage() {
					
		dlgProgress = new CsajDialog_WaitingWithProgressBar("Computing Pyramid dimension, please wait... Open console window for further info.",
				logService, false, exec); //isCanceable = false, because no following method listens to exec.shutdown 
		dlgProgress.updatePercent("");
		dlgProgress.setBarIndeterminate(true);
		dlgProgress.setVisible(true);	
	
		deleteExistingDisplays();
		generateTableHeader();
		int sliceIndex = spinnerInteger_NumImageSlice - 1;
		logService.info(this.getClass().getName() + " Processing single image " + (sliceIndex + 1));
		processSingleInputImage(sliceIndex);
	
		dlgProgress.addMessage("Processing finished! Collecting data for table...");		
		dlgProgress.setVisible(false);
		dlgProgress.dispose();	
		Toolkit.getDefaultToolkit().beep();			
	}
	
	/**
	 * This method starts the workflow for all images of the active display
	 */
	protected void startWorkflowForAllImages() {
		
		dlgProgress = new CsajDialog_WaitingWithProgressBar("Computing Pyramid dimensions, please wait... Open console window for further info.",
					logService, false, exec); //isCanceable = true, because processAllInputImages(dlgProgress) listens to exec.shutdown 
		dlgProgress.setVisible(true);		
	
		logService.info(this.getClass().getName() + " Processing all available images");
		deleteExistingDisplays();
		generateTableHeader();
		processAllInputImages();
		
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
			//List<Display<?>> list = defaultDisplayService.getDisplays();
			//for (int i = 0; i < list.size(); i++) {
			//	display = list.get(i);
			//	System.out.println("display name: " + display.getName());
			//	if (display.getName().contains("Name")) display.close(); //does not close correctly in Fiji, it is only not available any more
			//}			
			//List<ImageDisplay> listImgs = defaultImageDisplayService.getImageDisplays(); //Is also not closed in Fiji 
		
			Frame frame;
			Frame[] listFrames = JFrame.getFrames();
			for (int i = listFrames.length -1 ; i >= 0; i--) { //Reverse order, otherwise focus is not given free from the last image
				frame = listFrames[i];
				//System.out.println("frame name: " + frame.getTitle());
				if (frame.getTitle().contains("downscaled image")) {
					frame.setVisible(false); //Successfully closes also in Fiji
					frame.dispose();
				}
			}
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
				if (display.getName().contains(TABLE_OUT_NAME)) display.close();
			}			
		}
	}
	
	/** This method computes the maximal number of downscaled images*/
	public static int getMaxPyramidNumber(long width, long height) { // inclusive original image
		float oldWidth = width;
		float oldHeight = height;
		int number = 1; // inclusive original image
		boolean abbruch = false;
		while (!abbruch) {
			float newWidth = oldWidth / 2;
			float newHeight = oldHeight / 2;
			if ((newWidth < 1.0) || (newHeight < 1.0)) {
				abbruch = true;
			} else {
				oldWidth = newWidth;
				oldHeight = newHeight;
				// System.out.println("FractalDimensionPyramid: newWidth: " + newWidth);
				// System.out.println("FractalDimensiomPyramid: newHeight: " + newHeight);
				number = number + 1;
			}
		}
		return number;
	}
	
	/** This method takes the active image and computes results. 
	 *
	 **/
	private void processSingleInputImage(int s) {
		long startTime = System.currentTimeMillis();
		
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
		CsajContainer_ProcessMethod containerPM = process(rai, s);	
		//0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
		
		writeToTable(0, s, containerPM); //write always to the first row
		
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
	private void processAllInputImages() {
		
		long startTimeAll = System.currentTimeMillis();
	
		//convert to float values
		//Img<T> image = (Img<T>) dataset.getImgPlus();
		//Img<FloatType> imgFloat; // = opService.convert().float32((Img<T>)dataset.getImgPlus());

		CsajContainer_ProcessMethod containerPM;
		//loop over all slices of stack
		for (int s = 0; s < numSlices; s++){ //p...planes of an image stack
			//if (!exec.isShutdown()) {
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
				containerPM = process(rai, s);	
				//0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
				
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
		IntColumn columnMaxNumbPyramidImgs = new IntColumn("#Pyramid images");
		GenericColumn columnNumRegStart    = new GenericColumn("Reg Start");
		GenericColumn columnNumRegEnd      = new GenericColumn("Reg End");
		//GenericColumn columnInterpolation  = new GenericColumn("Interpolation");
		DoubleColumn columnDp              = new DoubleColumn("Dp");
		DoubleColumn columnR2              = new DoubleColumn("R2");
		DoubleColumn columnStdErr          = new DoubleColumn("StdErr");
		
	    tableOut = new DefaultGenericTable();
		tableOut.add(columnFileName);
		tableOut.add(columnSliceName);
		tableOut.add(columnMaxNumbPyramidImgs);
		tableOut.add(columnNumRegStart);
		tableOut.add(columnNumRegEnd);
		//table.add(columnInterpolation);
		tableOut.add(columnDp);
		tableOut.add(columnR2);
		tableOut.add(columnStdErr);
	}
	
	/**
	 * collects current result and writes to table
	 * 
	 * @param int numRow to write in the result table
	 * @param int numSlice sclice number of images from datasetIn.
	 * @param CsajContainer_ProcessMethod containerPM
	 */
	private void writeToTable(int numRow, int numSlice, CsajContainer_ProcessMethod containerPM) {
	
		int numRegStart = spinnerInteger_NumRegStart;
		int numRegEnd   = spinnerInteger_NumRegEnd;
		int numImages   = spinnerInteger_PyramidImages;
//		String interpolType = choiceRadioButt_Interpolation;
		
		int row = numRow;
	    int s = numSlice;	
			//0 Intercept, 1 Dim, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared		
			//fill table with values
			tableOut.appendRow();
			tableOut.set("File name",       row, datasetName);	
			if (sliceLabels != null)        tableOut.set("Slice name", row, sliceLabels[s]);
			tableOut.set("#Pyramid images", row, numImages);	
			tableOut.set("Reg Start",       row, "("+numRegStart+")" + containerPM.item2_Values[0]); //(NumRegStart)epsRegStart
			tableOut.set("Reg End",      	row, "("+numRegEnd+")"   + containerPM.item2_Values[1]); //(NumRegEnd)epsRegEnd
//			table.set("Interpolation",   table.getRowCount()-1, interpolType);	
			tableOut.set("Dp",              row, containerPM.item1_Values[1]);
			tableOut.set("R2",              row, containerPM.item1_Values[4]);
			tableOut.set("StdErr",          row, containerPM.item1_Values[3]);		
	}
					
	/** 
	 * Processing 
	 *
	 */
	private CsajContainer_ProcessMethod process(RandomAccessibleInterval<?> rai, int plane) { //plane plane (Image) number
		
		if (rai == null) {
			logService.info(this.getClass().getName() + " WARNING: rai==null, no image for processing!");
		}
		
		//Change to float because of interpolation
		imgFloat = createImgFloat(rai);
		
		int numRegStart          = spinnerInteger_NumRegStart;
		int numRegEnd          = spinnerInteger_NumRegEnd;
		int numPyramidImages= spinnerInteger_PyramidImages;
//		String interpolType = choiceRadioButt_Interpolation;
		
		int numBands = 1;
		
		boolean optShowPlot    = booleanShowDoubleLogPlot;
		boolean optShowDownscaledImages = booleanShowImages;
		
		long width  = rai.dimension(0);
		long height = rai.dimension(1);
		
		String imageType = "Grey";  //"Grey"  "RGB"....
	
		double[] epsRegStartEnd   = new double[2];  // epsRegStart, epsRegEnd
		double[] regressionParams = null;
		double[] resultValues = null;
		
		//Convert image to float
		//Img<T> image = (Img<T>) dataset.getImgPlus();
		//RandomAccessibleInterval<T> rai = (RandomAccessibleInterval<T>)dataset.getImgPlus();
		//IterableInterval ii = dataset.getImgPlus();
		//Img<FloatType> imgFloat = opService.convert().float32(ii);
		
		//Img<FloatType> imgFloat = opService.convert().float32(image);
		

		double[][] totals = new double[numPyramidImages][numBands];
		// double[] totalsMax = new double[numBands]; //for binary images
		int[] eps = new int[numPyramidImages];
		
		// definition of eps
		for (int n = 0; n < numPyramidImages; n++) {		
			eps[n] = (int)Math.pow(2, n);
			//logService.info(this.getClass().getName() + " n:" + n + " eps:  " + eps[n]);			
		}		
		
		if (isBinary) {// binary image
			
			int downSamplingFactor;
			for (int n = 0; n < numPyramidImages; n++) { //Downscaling incl. no downscaling
			    // "base-pyramid", i.e. layers of pyramid from base layer
				downSamplingFactor = (int)Math.pow(2,n); //of downsampling
				downSamplingFactor =  eps[n]; //of downsampling
				imgFloat = createImgFloat(rai);
			    imgSubSampled = subSamplingByAveraging(imgFloat, downSamplingFactor);
			   // uiService.show("Subsampled image", imgSubSampled);
				//logService.info(this.getClass().getName() + " width:"+ (imgSubSampled.dimension(0)) + " height:" + (imgSubSampled.dimension(1)));
					
				//Img<FloatType> img = (Img<FloatType>) opService.run(net.imagej.ops.create.img.CreateImgFromRAI.class, rai);
				//opService.run(net.imagej.ops.copy.CopyRAI.class, img, rai);
				
				//****IMPORTANT****Displaying a rai slice (pseudo 2D) directly with e.g. uiService.show(name, rai);
				//pushes a 3D array to the display and
				//yields mouse moving errors because the third dimension is not available	
				if ((optShowDownscaledImages) && (n > 0)) uiService.show("1/"+downSamplingFactor+" downscaled image", imgSubSampled);
			
				// Loop through all pixels.
				cursorF = imgSubSampled.localizingCursor();
				while (cursorF.hasNext()) {
					cursorF.fwd();
					//cursorF.localize(pos);
					for (int b = 0; b < numBands; b++) {
						if (cursorF.get().get() > 0) totals[n][b] += 1; // Binary Image //[1, 255] and not [255, 255] because interpolation introduces grey values other than 255!
						//totals[n][b] = totals[n][b]; // / totalsMax[b];
					}
				}	
			}
			//Computing log values for plot 
			//Change sequence of entries to start with smallest image
			double[][] lnTotals = new double[numPyramidImages][numBands];
			double[] lnEps      = new double[numPyramidImages];
			for (int n = 0; n < numPyramidImages; n++) {
				for (int b = 0; b < numBands; b++) {
					if (totals[n][b] == 0) {
						lnTotals[n][b] = Math.log(Double.MIN_VALUE); //Math.log(Float.MIN_VALUE); // damit logarithmus nicht undefiniert ist
					} else if (Double.isNaN(totals[n][b])) {
						lnTotals[n][b] = Double.NaN;
					} else {
						lnTotals[n][b] = Math.log(totals[n][b]);
					}
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
			
				double[] lnDataX = new double[numPyramidImages];
				double[] lnDataY = new double[numPyramidImages];
					
				for (int n = 0; n < numPyramidImages; n++) {
					lnDataY[n] = lnTotals[n][b];
					lnDataX[n] = lnEps[n];
				}
				// System.out.println("FractalDimensionPyramid: dataY: "+ dataY);
				// System.out.println("FractalDimensionPyramid: dataX: "+ dataX);
			
				if (optShowPlot) {
					String preName = "";
					if (numSlices > 1) {
						preName = "Slice-"+String.format("%03d", plane) +"-";
					}
					CsajPlot_RegressionFrame doubleLogPlot = DisplayRegressionPlotXY(lnDataX, lnDataY, isLineVisible,"Double log plot - Pyramid dimension", 
							preName + datasetName, "ln(2^n)", "ln(Count)", "",
							numRegStart, numRegEnd);
					doubleLogPlotList.add(doubleLogPlot);
				}
				
				// Compute regression
				CsajRegression_Linear lr = new CsajRegression_Linear();
				regressionParams = lr.calculateParameters(lnDataX, lnDataY, numRegStart, numRegEnd);
				//0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
			}
			
		} else { //grey value image
			//STILL NO READY YET****************************************************************************************
			int downSamplingFactor;
			for (int n = 0; n < numPyramidImages; n++) { //Downscaling incl. no downscaling
			    // "base-pyramid", i.e. layers of pyramid from base layer
				downSamplingFactor = (int)Math.pow(2,n); //of downsampling
				downSamplingFactor =  eps[n]; //of downsampling
				imgFloat = createImgFloat(rai);
			    imgSubSampled = subSamplingByAveraging(imgFloat, downSamplingFactor);
			    //imgSubSampled = subSamplingByNearestNeighbour(imgFloat, downSamplingFactor);
			    
			    //eps[n] = (int)imgSubSampled.dimension(0); 
			    //uiService.show("Subsampled image", imgSubSampled);
				//logService.info(this.getClass().getName() + " width:"+ (imgSubSampled.dimension(0)) + " height:" + (imgSubSampled.dimension(1)));
					
				//Img<FloatType> img = (Img<FloatType>) opService.run(net.imagej.ops.create.img.CreateImgFromRAI.class, rai);
				//opService.run(net.imagej.ops.copy.CopyRAI.class, img, rai);
				
				//****IMPORTANT****Displaying a rai slice (pseudo 2D) directly with e.g. uiService.show(name, rai);
				//pushes a 3D array to the display and
				//yields mouse moving errors because the third dimension is not available	
				if ((optShowDownscaledImages) && (n > 0)) uiService.show("1/"+downSamplingFactor+" downscaled image", imgSubSampled);
				
				//Apply detrending (flattening) with polynomials of order = 1
//				//image has flipped xy indices!!!!!
//				imgArr = new double[(int)imgSubSampled.dimension(1)][(int)imgSubSampled.dimension(0)];
//				// Write to image
//				cursorF = imgSubSampled.localizingCursor();
//				int[] pos = new int[2]; 
//				while (cursorF.hasNext()) {
//					cursorF.fwd();
//					cursorF.localize(pos);
//					imgArr[pos[1]][pos[0]] = cursorF.get().get();
//				}	
//			    //Remove mean value to keep the math from blowing up
//		        double meanImage = 0.0;
//		        for (int y = 0; y < imgArr.length; y++) {
//		        	for (int x = 0; x < imgArr[0].length; x++) {
//			        	meanImage += imgArr[y][x];
//			        }	
//		        }
//		        meanImage = meanImage/(imgArr.length*imgArr[0].length);
//		        for (int y = 0; y < imgArr.length; y++) {
//		        	for (int x = 0; x < imgArr[0].length; x++) {
//			        	imgArr[y][x] -= meanImage;
//			        }	
//		        }
//		     
//		        int polyOrderX = 1;
//		        int polyOrderY = 1;
//				PolynomialFit2D pf2D = new PolynomialFit2D();
//				double[][] polyParams = pf2D.calculateParameters(imgArr, polyOrderX, polyOrderY);
//				
//				double dTemp;
//				double yTemp;
//		        // Create an image of the fitted surface
//		        // Example:                
//		        //    dtemp =  (polyParams[3][3]*y*y*y + polyParams[2][3]*y*y + polyParams[1][3]*y + polyParams[0][3])*x*x*x;
//		        //    dtemp += (polyParams[3][2]*y*y*y + polyParams[2][2]*y*y + polyParams[1][2]*y + polyParams[0][2])*x*x;
//		        //    dtemp += (polyParams[3][1]*y*y*y + polyParams[2][1]*y*y + polyParams[1][1]*y + polyParams[0][1])*x;
//		        //    dtemp += (polyParams[3][0]*y*y*y + polyParams[2][0]*y*y + polyParams[1][0]*y + polyParams[0][0]);
//		        imgArrPolySurface = new double[imgArr.length][imgArr[0].length];
//		        for (int y = 0; y < imgArr.length; y++) {
//		        	for (int x = 0; x < imgArr[0].length; x++) {
//		        		 dTemp = 0;
//		                 // Determine the value of the fit at pixel iy,ix
//		                 for(int powx = polyOrderX; powx >= 0; powx--) {
//		                     yTemp = 0;
//		                     for(int powy = polyOrderY; powy >= 0; powy--) {
//		                         yTemp += polyParams[powy][powx] * Math.pow((double)y,(double)powy);
//		                     }
//		                     dTemp += yTemp * Math.pow((double)x,(double)powx);
//		                 }
//		                 // Add back the mean of the image
//		                 imgArrPolySurface[y][x] = dTemp + meanImage;
//			        }	
//		        }
//		        //Subtract polySurface and write back to imgSubSampled
//		        //Note that values of imgSubSampled will then be distributed around 0 and will contain negative values 
//		    	cursorF = imgSubSampled.localizingCursor();
//				pos = new int[2];
//				while (cursorF.hasNext()) {
//					cursorF.fwd();
//					cursorF.localize(pos);			
//					cursorF.get().set(cursorF.get().get() - (float)imgArrPolySurface[pos[1]][pos[0]]);
//				}	
				
				//if ((optShowDownscaledImages) && (n > 0)) uiService.show("1/"+downSamplingFactor+" detrended image", imgSubSampled);
				//uiService.show("Detrended image", imgSubSampled);
			
				// compute totals for double log plot
//				cursorF = imgSubSampled.localizingCursor();
//				double value;
//				double min = Double.MAX_VALUE;
//				double max = -Double.MAX_VALUE;
//				double mean = 0.0;
//				int num = 0;
//				while (cursorF.hasNext()) {
//					cursorF.fwd();					
//					value = cursorF.get().get();
//					mean = mean + value; 
//			
//					if (value > max) max = value;
//					if (value < min) min = value;
//					
//					num = num +1;	
//				}	
//				mean = mean /num;
//						
//				cursorF = imgSubSampled.localizingCursor();
//				double meanRq = 0.0;
//				int     numRq = 0;
//				while (cursorF.hasNext()) {
//					cursorF.fwd();
//					meanRq = meanRq + (cursorF.get().get()-mean)*(cursorF.get().get()-mean); //Mean is already very low and near 0 
//					numRq = numRq +1;	
//				}	
//				meanRq = meanRq /numRq;
				
				//first derivative, because it is a spatial parameter
				double derivative = 0.0;
				float value1 = 0;
				float value2 = 0;
				int subWidth  = (int)imgSubSampled.dimension(0);
				int subHeight = (int)imgSubSampled.dimension(1);
				
				RandomAccess<FloatType> ra = imgSubSampled.randomAccess();
				for (int x = 0; x < (subWidth - 1); x++) {
					for (int y = 0; y < subHeight ; y++) {
						ra.setPosition(new int[]{x,y});
						value1 = ra.get().get();
						ra.setPosition(new int[]{x+1,y});
						value2 = ra.get().get();
					}	
					derivative += Math.sqrt(Math.abs(value2-value1));
				}
				
				for (int x = 0; x < subWidth; x++) {
					for (int y = 0; y < (subHeight - 1); y++) {
						ra.setPosition(new int [] {x,y});
						value1 = ra.get().get();
						ra.setPosition(new int [] {x,y+1});
						value2 = ra.get().get();
					}	
					derivative +=  Math.sqrt(Math.abs(value2-value1));
				}
				
				derivative /= Math.pow(subWidth*subHeight, 2);
						
				for (int b = 0; b < numBands; b++) {
					//totals[n][b] = Math.sqrt(meanRq); //?????????????????not sure about that??????????????????????
					//totals[n][b] = meanRq;
					//totals[n][b] = (max-min);///mean; ///mean;
					totals[n][b] = Math.sqrt(derivative);
				}
				
			}
			//Computing log values for plot 
			//Change sequence of entries to start with smallest image
			double[][] lnTotals = new double[numPyramidImages][numBands];
			double[] lnEps      = new double[numPyramidImages];
			for (int n = 0; n < numPyramidImages; n++) {
				for (int b = 0; b < numBands; b++) {
					if (totals[n][b] == 0) {
						lnTotals[n][b] = Math.log(Double.MIN_VALUE); //Math.log(Float.MIN_VALUE); // damit logarithmus nicht undefiniert ist
					} else if (Double.isNaN(totals[n][b])) {
						lnTotals[n][b] = Double.NaN;
					} else {
						lnTotals[n][b] = Math.log(totals[n][b]);
					}
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
			
				double[] lnDataX = new double[numPyramidImages];
				double[] lnDataY = new double[numPyramidImages];
					
				for (int n = 0; n < numPyramidImages; n++) {
					lnDataY[n] = lnTotals[n][b];	
					lnDataX[n] = lnEps[n];
				}
				// System.out.println("FractalDimensionPyramid: dataY: "+ dataY);
				// System.out.println("FractalDimensionPyramid: dataX: "+ dataX);
			
				if (optShowPlot) {
					String preName = "";
					if (numSlices > 1) {
						preName = "Slice-"+String.format("%03d", plane) +"-";
					}
					CsajPlot_RegressionFrame doubleLogPlot = DisplayRegressionPlotXY(lnDataX, lnDataY, isLineVisible,"Double log plot - Pyramid dimension", 
							preName + datasetName, "ln(2^n)", "ln(Count)", "",
							numRegStart, numRegEnd);
					doubleLogPlotList.add(doubleLogPlot);
				}
				
				// Compute regression
				CsajRegression_Linear lr = new CsajRegression_Linear();
				regressionParams = lr.calculateParameters(lnDataX, lnDataY, numRegStart, numRegEnd);
				//0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
			}
		}
		
		//Compute result values
		resultValues = regressionParams;
		double dim = Double.NaN;
		dim = -regressionParams[1];
		resultValues[1] = dim; 
		logService.info(this.getClass().getName() + " Pyramid dimension: " + dim);
		
		epsRegStartEnd[0] = eps[numRegStart-1];
		epsRegStartEnd[1] = eps[numRegEnd-1];
	
		return new CsajContainer_ProcessMethod(resultValues, epsRegStartEnd);
		//Output
		//uiService.show(TABLE_OUT_NAME, table);
		////result = ops.create().img(image, new FloatType()); may not work in older Fiji versions
		//result = new ArrayImgFactory<>(new FloatType()).create(image.dimension(0), image.dimension(1)); 
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
	 * @param numRegStart minimum value for regression range
	 * @param numRegEnd maximal value for regression range 
	 * @param optDeleteExistingPlot option if existing plot should be deleted before showing a new plot
	 * @param interpolType The type of interpolation
	 * @return RegressionPlotFrame
	 */			
	private CsajPlot_RegressionFrame DisplayRegressionPlotXY(double[] dataX, double[] dataY, boolean isLineVisible,
			String frameTitle, String plotLabel, String xAxisLabel, String yAxisLabel, String legendLabel, int numRegStart, int numRegEnd) {
		// jFreeChart
		CsajPlot_RegressionFrame pl = new CsajPlot_RegressionFrame(dataX, dataY, isLineVisible, frameTitle, plotLabel, xAxisLabel,
				yAxisLabel, legendLabel, numRegStart, numRegEnd);
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
			mean  = 0f;
			count = 0;
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
	
	/**
	 * This method creates a smaller image by averaging pixel values 
	 * @param imgFloat
	 * @param downSamplingFactor
	 * @return
	 */	
	private Img<FloatType> subSamplingByNearestNeighbour(Img<FloatType> imgFloat, int downSamplingFactor) {
		
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
		float value= 0f;
	
		// for all pixels of the output image
		while (cursorF.hasNext()) {
			cursorF.fwd();
			cursorF.localize(pos);
			//Get average
			ra.setPosition(pos[0]*averagingSize, 0);
			ra.setPosition(pos[1]*averagingSize, 1);
			value = ra.get().getRealFloat();
			cursorF.get().set(value);
		}
		return imgSubSampled;
	}

	/**
	 * Performs interpolation.
	 * <p>
	 * It is a good practice to structure non-trivial routines as
	 * {@code public static} so that it can be conveniently called from external
	 * code.
	 * </p>
	 * @param IntervalView<?> iv  intervalView to be interpolated
	 * @param final String interpolType the interpolation type
	 * @param interpolType The type of interpolation
	 * @return RealRandomAccessible<FloatType> interpolant
	 */
	private <T extends Type< T> >  RealRandomAccessible<FloatType> interpolate( Img<FloatType> imgFloat, final String interpolType){
		// declare how we want the image to be interpolated
		InterpolatorFactory factory = null;
		if (interpolType.contentEquals("Linear") ) {
			// create an InterpolatorFactory RealRandomAccessible using linear interpolation
			factory = new NLinearInterpolatorFactory<FloatType>();
		}
		if (interpolType.contentEquals("Lanczos") ) {
			// create an InterpolatorFactory RealRandomAccessible using lanczos interpolation
			factory = new LanczosInterpolatorFactory<FloatType>();
		}
		if (interpolType.contentEquals("Floor") ) {
			// create an InterpolatorFactory RealRandomAccessible using floor interpolation
			factory = new FloorInterpolatorFactory<FloatType>();
		}
		if (interpolType.contentEquals("Nearest Neighbor") ) {
		// create an InterpolatorFactory RealRandomAccessible using nearst neighbor interpolation
		    factory = new NearestNeighborInterpolatorFactory<FloatType>();
		}
	
		// create a RandomAccessible using the factory and views method
		// it is important to extend the image first, the interpolation scheme might
		// grep pixels outside of the boundaries even when locations inside are queried
		// as they integrate pixel information in a local neighborhood - the size of
		// this neighborhood depends on which interpolator is used
		RealRandomAccessible< FloatType > interpolant = Views.interpolate(Views.extendMirrorSingle(imgFloat), factory);
			
		return interpolant;
        //Img<FloatType> scaled = scale(interpolant, interval, new ArrayImgFactory<>( new FloatType() ), scale);  
		//return scale(interpolant, interval, new ArrayImgFactory<>( new FloatType() ), scale);
	}
	
	/**
	 * Compute a magnified version of a given real interval
	 * magnification may be 1 (no magnification and no interpolation) or even smaller than 1 
	 *
	 * @param interpolant - the input data
	 * @param interval - the real interval on the source that should be magnified
	 * @param arrayImgFactory - the image factory for the output image
	 * @param magnification - the ratio of magnification
	 * @return - an Img that contains the magnified image content
	 */
	public static < T extends Type< T > > Img<FloatType> magnify( RealRandomAccessible<FloatType> interpolant,
		RealInterval interval, ArrayImgFactory<FloatType> arrayImgFactory, double magnification )
	{
		int numDimensions = interval.numDimensions();

		// compute the number of pixels of the output and the size of the real interval
		long[]      pixelSize = new long[numDimensions];
		double[] intervalSize = new double[numDimensions];

		for ( int d = 0; d < numDimensions; ++d ){
			intervalSize[d] = interval.realMax(d) - interval.realMin(d);
			pixelSize[d] = Math.round( (intervalSize[d] + 1) * magnification);
		}

		// create the output image
		//Img< FloatType > output = arrayImgFactory.create( pixelSize );	
		ArrayImg<FloatType, ?> imgDownscaled = arrayImgFactory.create( pixelSize );

		// cursor to iterate over all pixels
		Cursor <FloatType> cursor = imgDownscaled.localizingCursor();

		// create a RealRandomAccess on the source (interpolator)
		RealRandomAccess<FloatType> realRandomAccess = interpolant.realRandomAccess();

		// the temporary array to compute the position
		double[] tmp = new double[numDimensions];

		// for all pixels of the output image
		while (cursor.hasNext())
		{
			cursor.fwd();

			// compute the appropriate location of the interpolator
			for ( int d = 0; d < numDimensions; ++d)
				tmp[d] = cursor.getDoublePosition(d) / imgDownscaled.realMax(d) * intervalSize[d]
						+ interval.realMin(d);

			// set the position
			realRandomAccess.setPosition( tmp );
			//System.out.println("tmp[0]" + tmp[0] + "   tmp[1]"+tmp[1]);
			// set the new value	
			cursor.get().set( realRandomAccess.get());
		}

		return imgDownscaled;
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
		//ij.command().run(MethodHandles.lookup().lookupClass().getName(), true).get().getOutput("image");
		ij.command().run(MethodHandles.lookup().lookupClass().getName(), true);
	}
}

