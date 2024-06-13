/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Csaj2DFracDimMassRadius.java
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
import org.scijava.ItemIO;
import org.scijava.ItemVisibility;
import org.scijava.app.StatusService;
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
import org.scijava.table.IntColumn;
import org.scijava.ui.DialogPrompt.MessageType;
import org.scijava.ui.DialogPrompt.OptionType;
import org.scijava.ui.DialogPrompt.Result;
import org.scijava.ui.UIService;
import org.scijava.widget.Button;
import org.scijava.widget.ChoiceWidget;
import org.scijava.widget.FileWidget;
import org.scijava.widget.NumberWidget;

import at.csa.csaj.commons.Dialog_WaitingWithProgressBar;
import at.csa.csaj.commons.Plot_RegressionFrame;
import at.csa.csaj.commons.Regression_Linear;
import at.csa.csaj.commons.Container_ProcessMethod;
import io.scif.DefaultImageMetadata;
import io.scif.MetaTable;

/**
 * A {@link ContextCommand} plugin computing
 * <a>the fractal mass radius dimension </a>
 * of an image.
 */
@Plugin(type = ContextCommand.class, 
        headless = true,
        label = "Mass radius dimension",
        initializer = "initialPluginLaunch",
        iconPath = "/icons/comsystan-logo-grey46-16x16.png", //Menu entry icon
        menu = {
        @Menu(label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT, mnemonic = MenuConstants.PLUGINS_MNEMONIC),
        @Menu(label = "ComsystanJ"),
        @Menu(label = "2D Image(s)"),
		@Menu(label = "Fractal analyses", weight = 6),
        @Menu(label = "Mass radius dimension")})
//public class Csaj2DFracDimMassRadius<T extends RealType<T>> extends InteractiveCommand { //non blocking GUI
public class Csaj2DFracDimMassRadius<T extends RealType<T>> extends ContextCommand implements Previewable { //modal GUI with cancel
	
	private static final String PLUGIN_LABEL            = "<html><b>Computes mass radius dimension</b></html>";
	private static final String SPACE_LABEL             = "";
	private static final String REGRESSION_LABEL        = "<html><b>Regression parameters</b></html>";
	private static final String METHODOPTIONS_LABEL     = "<html><b>Method options</b></html>";
	private static final String BACKGROUNDOPTIONS_LABEL = "<html><b>Background option</b></html>";
	private static final String DISPLAYOPTIONS_LABEL    = "<html><b>Display options</b></html>";
	private static final String PROCESSOPTIONS_LABEL    = "<html><b>Process options</b></html>";
	
	private static Img<FloatType> imgFloat; 
	private static RandomAccessibleInterval<?> raiBox;
	private static RandomAccess<?> ra;
	private static Cursor<?> cursor = null;
	private static String datasetName;
	private static String[] sliceLabels;
	private static long width  = 0;
	private static long height = 0;
	private static long numDimensions = 0;
	private static long numSlices = 0;
	private static long compositeChannelCount =0;
	private static String imageType = "";
	private static int  numDiscs = 0;
	private static ArrayList<Plot_RegressionFrame> doubleLogPlotList = new ArrayList<Plot_RegressionFrame>();
	
	private static final String tableOutName = "Table - Mass radius dimension";
	
	private Dialog_WaitingWithProgressBar dlgProgress;
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

	@Parameter(label = tableOutName, type = ItemIO.OUTPUT)
	private DefaultGenericTable tableOut;

	
   //Widget elements------------------------------------------------------
	//-----------------------------------------------------------------------------------------------------
    //@Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
	//private final String labelPlugin = PLUGIN_LABEL;

    //@Parameter(label = " ", visibility = ItemVisibility.MESSAGE,  persist = false)
  	//private final String labelSpace = SPACE_LABEL;
    
	//-----------------------------------------------------------------------------------------------------
    @Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
  	private final String labelRegression = REGRESSION_LABEL;

    @Parameter(label = "Number of discs",
    		   description = "Number of distinct discs with radii following the power of 2",
	       	   style = NumberWidget.SPINNER_STYLE,
	           min = "1",
	           max = "32768",
	           stepSize = "1",
	           persist = false,  //restore previous value default = true
	           initializer = "initialNumDiscs",
	           callback    = "callbackNumDiscs")
    private int spinnerInteger_NumDiscs;
    
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
     @Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
     private final String labelMethodOptions = METHODOPTIONS_LABEL;
     
     @Parameter(label = "Scanning type",
 		    description = "Discs over center of mass or discs over several object pixels",
 		    style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
   		    choices = {"Disc over center of mass", "Sliding disc"}, 
   		    persist = true,  //restore previous value default = true
 		    initializer = "initialScanningType",
             callback = "callbackScanningType")
     private String choiceRadioButt_ScanningType;
     
     @Parameter(label = "Color model",
  		    description = "Type of image and computation",
  		    style = ChoiceWidget.RADIO_BUTTON_VERTICAL_STYLE,
    		    choices = {"Binary", "Grey"},
    		    persist = true,  //restore previous value default = true
  		    initializer = "initialColorModelType",
              callback = "callbackColorModelType")
     private String choiceRadioButt_ColorModelType;
     
     @Parameter(label = "(Sliding disc) Pixel %",
  		   description = "% of object pixels to be taken - to lower computation times",
	       	   style = NumberWidget.SPINNER_STYLE,
	           min = "1",
	           max = "100",
	           stepSize = "1",
	           //persist = false,  //restore previous value default = true
	           initializer = "initialPixelPercentage",
	           callback    = "callbackPixelPercentage")
     private int spinnerInteger_PixelPercentage;
     
 	//-----------------------------------------------------------------------------------------------------
     @Parameter(label = " ", visibility = ItemVisibility.MESSAGE, persist = false)
     private final String labelDisplayOptions = DISPLAYOPTIONS_LABEL;
      
     @Parameter(label = "Show double log plot",
    		    persist = true,  //restore previous value default = true
  		        initializer = "initialShowDoubleLogPlots")
	 private boolean booleanShowDoubleLogPlot;
       
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
     
 	@Parameter(label = "Image #", description = "Image slice number", style = NumberWidget.SPINNER_STYLE, min = "1", max = "99999999", stepSize = "1",
			   persist = false, // restore  previous value  default  =  true
			   initializer = "initialNumImageSlice",
			   callback = "callbackNumImageSlice")
	private int spinnerInteger_NumImageSlice;
	
	@Parameter(label   = "   Process single image #    ",
		    	callback = "callbackProcessSingleImage")
	private Button buttonProcessSingelImage;
	
//	Deactivated, because it does not work in Fiji (although it works in ImageJ2 -Eclipse)	
//	@Parameter(label   = "Process single active image ",
//		    callback = "callbackProcessActiveImage")
//	private Button buttonProcessActiveImage;
     
//  Parameter(label   = "Process all available images",
// 		        callback = "callbackProcessAllImages")
//	private Button buttonProcessAllImages;


    //---------------------------------------------------------------------
 
    //The following initializer functions set initial values	
	protected void initialPluginLaunch() {
		checkItemIOIn();
	}
    protected void initialNumDiscs() {
    	if (datasetIn == null) {
    		logService.error(this.getClass().getName() + " ERROR: Input image = null");
    		cancel("ComsystanJ 2D plugin cannot be started - missing input image.");
    		return;
    	} else {
    		numDiscs = getMaxBoxNumber(datasetIn.dimension(0), datasetIn.dimension(1));
    	}   	
      	spinnerInteger_NumDiscs = numDiscs;
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
    		numDiscs = getMaxBoxNumber(datasetIn.dimension(0), datasetIn.dimension(1));
    	}
    	spinnerInteger_NumRegEnd =  numDiscs;
    }
    protected void initialScanningType() {
    	choiceRadioButt_ScanningType = "Sliding disc";
    }
    protected void initialColorModelType() {
    	choiceRadioButt_ColorModelType = "Binary";
    } 
    protected void initialPixelPercentage() {
      	spinnerInteger_PixelPercentage = 10;
    }
    protected void initialShowDoubleLogPlots() {
    	booleanShowDoubleLogPlot = true;
    }
    protected void initialOverwriteDisplays() {
    	booleanOverwriteDisplays = true;
    }
    
	protected void initialNumImageSlice() {
    	spinnerInteger_NumImageSlice = 1;
	}
	
  
	// ------------------------------------------------------------------------------
	
	/** Executed whenever the {@link #spinnerInteger_NumDiscs} parameter changes. */
	protected void callbackNumDiscs() {
		
		if  (spinnerInteger_NumDiscs < 3) {
			spinnerInteger_NumDiscs = 3;
		}
		int numMaxBoxes = getMaxBoxNumber(datasetIn.dimension(0), datasetIn.dimension(1));	
		if (spinnerInteger_NumDiscs > numMaxBoxes) {
			spinnerInteger_NumDiscs = numMaxBoxes;
		};
		if (spinnerInteger_NumRegEnd > spinnerInteger_NumDiscs) {
			spinnerInteger_NumRegEnd = spinnerInteger_NumDiscs;
		}
		if (spinnerInteger_NumRegStart >= spinnerInteger_NumRegEnd - 2) {
			spinnerInteger_NumRegStart = spinnerInteger_NumRegEnd - 2;
		}

		numDiscs = spinnerInteger_NumDiscs;
		logService.info(this.getClass().getName() + " Number of boxes set to " + spinnerInteger_NumDiscs);
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
		if (spinnerInteger_NumRegEnd > spinnerInteger_NumDiscs) {
			spinnerInteger_NumRegEnd = spinnerInteger_NumDiscs;
		}
		
		logService.info(this.getClass().getName() + " Regression Max set to " + spinnerInteger_NumRegEnd);
	}
	
	/** Executed whenever the {@link #choiceRadioButt_ScanningType} parameter changes. */
	protected void callbackScanningType() {
		logService.info(this.getClass().getName() + " Scanning method set to " + choiceRadioButt_ScanningType);
		
	}
	
	/** Executed whenever the {@link #choiceRadioButt_ColorModelType} parameter changes. */
	protected void callbackColorModelType() {
		logService.info(this.getClass().getName() + " Color model type set to " + choiceRadioButt_ColorModelType);
	}
	
	/** Executed whenever the {@link #spinnerInteger_PixelPercentage} parameter changes. */
	protected void callbackPixelPercentage() {
		logService.info(this.getClass().getName() + " Pixel % set to " + spinnerInteger_PixelPercentage);
	}
	
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
	        	startWorkflowForAllImages();
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
		    	    startWorkflowForSingleImage();
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
		//if(ij.ui().isHeadless()){
		//}	
	    startWorkflowForAllImages();
	}
	
	public void checkItemIOIn() {
	
		//datasetIn = imageDisplayService.getActiveDataset();
		if (datasetIn == null) {
			logService.error(this.getClass().getName() + " ERROR: Input image = null");
			cancel("ComsystanJ 2D plugin cannot be started - missing input image.");
			return;
		}

		if ( (datasetIn.firstElement() instanceof UnsignedByteType) ||
			 (datasetIn.firstElement() instanceof FloatType) ){
			//That is OK, proceed
		} else {
			logService.warn(this.getClass().getName() + " WARNING: Data type is not Byte or Float");
			cancel("ComsystanJ 2D plugin cannot be started - data type is not Byte or Float.");
			return;
		}
		
		// get some info
		width = datasetIn.dimension(0);
		height = datasetIn.dimension(1);
		//numSlices = dataset.getDepth(); //does not work if third axis ist not specifyed as z-Axis
		
		numDimensions = datasetIn.numDimensions();
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
		logService.info(this.getClass().getName() + " Image type: " + imageType); 
		logService.info(this.getClass().getName() + " Number of images = "+ numSlices); 
		
		//RGB not allowed
		if (!imageType.equals("Grey")) { 
			logService.warn(this.getClass().getName() + " WARNING: Grey value image(s) expected!");
			cancel("ComsystanJ 2D plugin cannot be started - grey value image(s) expected!");
		}
	}
	
	/**
	* This method starts the workflow for a single image of the active display
	*/
	protected void startWorkflowForSingleImage() {
			
		dlgProgress = new Dialog_WaitingWithProgressBar("Computing Mass radius dimensions, please wait... Open console window for further info.",
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
		
		dlgProgress = new Dialog_WaitingWithProgressBar("Computing Mass radius dimensions, please wait... Open console window for further info.",
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
				if (display.getName().contains(tableOutName)) display.close();
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
		Container_ProcessMethod containerPM = process(rai, s);	
		//0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared

		writeToTable(0, s, containerPM);
		
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

		Container_ProcessMethod containerPM;
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
		IntColumn columnMaxNumDiscs        = new IntColumn("# Discs");
		GenericColumn columnNumRegStart    = new GenericColumn("Reg Start");
		GenericColumn columnNumRegEnd      = new GenericColumn("Reg End");
		GenericColumn columnScanningType   = new GenericColumn("Scanning type");
		GenericColumn columnColorModelType = new GenericColumn("Color model");
		IntColumn columnPixelPercentage    = new IntColumn("(Sliding disc) Pixel %");
		DoubleColumn columnDc              = new DoubleColumn("Dmass");
		DoubleColumn columnR2              = new DoubleColumn("R2");
		DoubleColumn columnStdErr          = new DoubleColumn("StdErr");
		
	    tableOut = new DefaultGenericTable();
		tableOut.add(columnFileName);
		tableOut.add(columnSliceName);
		tableOut.add(columnMaxNumDiscs);
		tableOut.add(columnNumRegStart);
		tableOut.add(columnNumRegEnd);
		tableOut.add(columnScanningType);
		tableOut.add(columnColorModelType);
		tableOut.add(columnPixelPercentage);
		tableOut.add(columnDc);
		tableOut.add(columnR2);
		tableOut.add(columnStdErr);
	}
	
		/**
		 * collects current result and writes to table
		 * 
		 * @param int numRow to write in the result table
		 * @param int numSlice sclice number of images from datasetIn.
		 * @param Container_ProcessMethod containerPM
		 */
		private void writeToTable(int numRow, int numSlice, Container_ProcessMethod containerPM) { 
		
		int numDiscs           = spinnerInteger_NumDiscs;
		int numRegStart        = spinnerInteger_NumRegStart;
		int numRegEnd          = spinnerInteger_NumRegEnd;
		int pixelPercentage    = spinnerInteger_PixelPercentage;
		String scanningType    = choiceRadioButt_ScanningType;
		String colorModelType  = choiceRadioButt_ColorModelType;	
		
		int row = numRow;
	    int s = numSlice;	
		//0 Intercept, 1 Dim, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared		
		//fill table with values
		tableOut.appendRow();
		tableOut.set("File name",   	 tableOut.getRowCount() - 1, datasetName);	
		if (sliceLabels != null) 	     tableOut.set("Slice name", tableOut.getRowCount() - 1, sliceLabels[s]);
		tableOut.set("# Discs",    	     tableOut.getRowCount()-1, numDiscs);	
		tableOut.set("Reg Start",        tableOut.getRowCount()-1, "("+numRegStart+")" + containerPM.item2_Values[0]); //(NumRegStart)epsRegStart
		tableOut.set("Reg End",      	 tableOut.getRowCount()-1, "("+numRegEnd+")"   + containerPM.item2_Values[1]); //(NumRegEnd)epsRegEnd
		tableOut.set("Scanning type",    tableOut.getRowCount()-1, scanningType);
		tableOut.set("Color model",      tableOut.getRowCount()-1, colorModelType);
		if (scanningType.equals("Sliding disc")) tableOut.set("(Sliding disc) Pixel %", tableOut.getRowCount()-1, pixelPercentage);	
		tableOut.set("Dmass",          	 tableOut.getRowCount()-1, containerPM.item1_Values[1]);
		tableOut.set("R2",          	 tableOut.getRowCount()-1, containerPM.item1_Values[4]);
		tableOut.set("StdErr",      	 tableOut.getRowCount()-1, containerPM.item1_Values[3]);		
	}
	
							
	/** 
	 * Processing ****************************************************************************************
	 * */
	private Container_ProcessMethod process(RandomAccessibleInterval<?> rai, int plane) { //plane plane (Image) number

		if (rai == null) {
			logService.info(this.getClass().getName() + " WARNING: rai==null, no image for processing!");
		}
		
		int numRegStart            = spinnerInteger_NumRegStart;
		int numRegEnd            = spinnerInteger_NumRegEnd;
		int numDiscs          = spinnerInteger_NumDiscs;
		String scanningType   = choiceRadioButt_ScanningType;	
		String colorModelType = choiceRadioButt_ColorModelType;	
		int pixelPercentage   = spinnerInteger_PixelPercentage;
		boolean optShowPlot   = booleanShowDoubleLogPlot;
		long width  = rai.dimension(0);
		long height = rai.dimension(1);
		
		String imageType = "8-bit";  //  "RGB"....
	
		double[] epsRegStartEnd   = new double[2];  // epsRegStart, epsRegEnd
		double[] regressionParams = null;
		double[] resultValues = null;
		
		//Convert image to float
		//Img<T> image = (Img<T>) dataset.getImgPlus();
		//RandomAccessibleInterval<T> rai = (RandomAccessibleInterval<T>)dataset.getImgPlus();
		//IterableInterval ii = dataset.getImgPlus();
		//Img<FloatType> imgFloat = opService.convert().float32(ii);
	
		double[]totals     = new double[numDiscs];
		//double[][] totalsMax  = new double[numDiscs][numBands]; //for binary images
		int[] eps = new int[numDiscs];
		
		// definition of eps
		for (int n = 0; n < numDiscs; n++) {
			eps[n] = (int)Math.round(Math.pow(2, n));
			//logService.info(this.getClass().getName() + " n:" + n + " eps:  " + eps[n]);		
		}		
	
		if (scanningType.equals("Disc over center of mass")) {
			//Fast - only over center of mass
			ra = rai.randomAccess();
			long number_of_points = 0; // total number of object points 
			int radius;		
			long count = 0;
			int sample = 0;
			
			double[] com = this.computeCenterOfMass(rai);
			int comX = (int) Math.round(com[0]);
			int comY = (int) Math.round(com[1]);
			
			//get total number of object points
			for (int x = 0; x < width; x++){
				for (int y = 0; y < height; y++){	
					ra.setPosition(x, 0);
					ra.setPosition(y, 1);	
					if(((UnsignedByteType) ra.get()).get() > 0) number_of_points++; // total number of points 	
				}
			}
				
			for (int n = 0; n < numDiscs; n++) { //2^0  to 2^numDiscs		
				
				radius = eps[n];								
				// scroll through discs over center of mass
				for (int xx = comX - radius + 1; xx < comX + radius ; xx++) {
					if(xx >= 0 && xx < width) { // catch index-out-of-bounds exception
						for (int yy = comY - radius + 1; yy < comY + radius; yy++) {
							if(yy >= 0 && yy < height) { // catch index-out-of-bounds exception
								//if (Math.sqrt((xx-x)*(xx-x)+(yy-y)*(yy-y)) <= radius) { //HA
									ra.setPosition(xx, 0);
									ra.setPosition(yy, 1);	
									sample = ((UnsignedByteType) ra.get()).get();
									if((sample > 0) ){
										if (colorModelType.equals("Binary")) count = count + 1;
										if (colorModelType.equals("Grey"))   count = count + sample;
									}
								//}//<= radius	
							 }
						}//yy
					}
				}//XX
						
				// calculate the average number of neighboring points within distance "radius":  
				//number of neighbors = counts-total_number_of_points for correlation dimension
				//mass = counts for mass radius dimension
				//average number of neighbors = number of neighbors / total_number_of_points
				// totals[n]=(double)(count-number_of_points)/number_of_points; //for Correlation dimension 	
				totals[n]=(double)(count);///number_of_points; m(r) = M(r) = number of pixels inside r //for MR dimension 	
				//totals[n]=(double)(count)/number_of_points; //Same result, just numbers are smaller	 m(r) = M(r)/M  M.. number of object pixels
				//System.out.println("Counts:"+count+", total number of points:"+number_of_points);
				// set counts equal to zero
				count=0;	
			} //n Box sizes		
		} //Fast - only over center of mass
		
		//********************************Binary Image: 0 and >0! and not: 0 and 255
		//Mass radius method	
		else if (scanningType.equals("Sliding disc")) {
			//Classical mass radius dimension with radius over a pixel
			//radius is estimated by box
			ra = rai.randomAccess();
			long number_of_points = 0; // total number of object points 
			int max_random_number = (int) (100/pixelPercentage); // Evaluate max. random number
			int random_number = 0;
			int radius;		
			long count = 0;
			int sample = 0;
			
			if  (max_random_number == 1) { // no statistical approach, take all image pixels
				for (int n = 0; n < numDiscs; n++) { //2^0  to 2^numDiscs		
					radius = eps[n];			
					for (int x = 0; x < width; x++){
						for (int y = 0; y < height; y++){	
							ra.setPosition(x, 0);
							ra.setPosition(y, 1);	
							if((((UnsignedByteType) ra.get()).get() > 0) ){
								number_of_points++; // total number of object points 	
								// scroll through sub-array 
								for (int xx = x - radius + 1; xx < x + radius ; xx++) {
									if(xx >= 0 && xx < width) { // catch index-out-of-bounds exception
										for (int yy = y - radius + 1; yy < y + radius; yy++) {
											if(yy >= 0 && yy < height) { // catch index-out-of-bounds exception
												if (Math.sqrt((xx-x)*(xx-x)+(yy-y)*(yy-y)) <= radius) { //HA
													ra.setPosition(xx, 0);
													ra.setPosition(yy, 1);	
													sample = ((UnsignedByteType) ra.get()).get();
													if((sample > 0) ){
														if (colorModelType.equals("Binary")) count = count + 1;
														if (colorModelType.equals("Grey"))   count = count + sample;
													}
												}//<= radius	
											}
										}//yy
									}
								}//XX
							}
						} //y	
					} //x  
					// calculate the average number of neighboring points within distance "radius":  
					//number of neighbors = counts-total_number_of_points for correlation dimension
					//mass = counts for mass radius dimension
					//average number of neighbors = number of neighbors / total_number_of_points
					// totals[n]=(double)(count-number_of_points)/number_of_points; //for Correlation dimension 	
					totals[n]=(double)(count);///number_of_points; m(r) = M(r) = number of pixels inside r //for MR dimension 	
					//totals[n]=(double)(count)/number_of_points; //Same result, just numbers are smaller	 m(r) = M(r)/M  M.. number of object pixels
					//System.out.println("Counts:"+counts+" total number of points:"+total_number_of_points);
					// set counts equal to zero
					count=0;	
					number_of_points=0;
				} //n Box sizes		
			} // no statistical approach
			else { //statistical approach
				for (int n = 0; n < numDiscs; n++) { //2^0  to 2^numDiscs		
					radius = eps[n];				
					for (int x = 0; x < width; x++){
						for (int y = 0;  y < height; y++){		
							// if max_random_number > 1 only a fraction is taken e.g. for 50% max_random_number = 2
							random_number = (int) (Math.random()*max_random_number+1); //+1 because (int) truncates digits after the decimal point
							if( random_number == 1 ){ //random_number will always be 1 when percentage is 100 and therefore max_random_number is 1
								ra.setPosition(x, 0);
								ra.setPosition(y, 1);	
								if((((UnsignedByteType) ra.get()).get() > 0) ){
									number_of_points++; // total number of points 	
									// scroll through sub-array 
									for (int xx = x - radius + 1; xx < x + radius ; xx++) {
										if(xx >= 0 && xx < width) { // catch index-out-of-bounds exception
											for (int yy = y - radius + 1; yy < y + radius; yy++) {
												if(yy >= 0 && yy < height) { // catch index-out-of-bounds exception
													if (Math.sqrt((xx-x)*(xx-x)+(yy-y)*(yy-y)) <= radius) { //HA
														ra.setPosition(xx, 0);
														ra.setPosition(yy, 1);	
														sample = ((UnsignedByteType) ra.get()).get();
														if((sample > 0) ){
															if (colorModelType.equals("Binary")) count = count + 1;
															if (colorModelType.equals("Grey"))   count = count + sample;
														}
													}//<= radius	
												 }
											}//yy
										}
									}//XX
								}
							}
						} //y	
					} //x  
					// calculate the average number of neighboring points within distance "radius":  
					//number of neighbors = counts-total_number_of_points for correlation dimension
					//mass = counts for mass radius dimension
					//average number of neighbors = number of neighbors / total_number_of_points
					// totals[n]=(double)(count-number_of_points)/number_of_points; //for Correlation dimension 	
					totals[n]=(double)(count);///number_of_points; m(r) = M(r) = number of pixels inside r //for MR dimension 	
					//totals[n]=(double)(count)/number_of_points; //Same result, just numbers are smaller	 m(r) = M(r)/M  M.. number of object pixels
					//System.out.println("Counts:"+counts+" total number of points:"+total_number_of_points);
					// set counts equal to zero
					count=0;	
					number_of_points=0;
				} //n Box sizes		
			} //statistical approach
		} //
	
		//Computing log values for plot 
		//Change sequence of entries to start with a pixel
		double[] lnTotals = new double[numDiscs];
		double[] lnEps    = new double[numDiscs];
		
		for (int n = 0; n < numDiscs; n++) {	
			if (totals[n] == 0) {
				lnTotals[n] = Math.log(Double.MIN_VALUE);
			} else if (Double.isNaN(totals[n])) {
				lnTotals[n] = Double.NaN;
			} else {
				lnTotals[n] = Math.log(totals[n]);
			}
			lnEps[n] = Math.log(eps[n]);
			//logService.info(this.getClass().getName() + " n:" + n + " eps:  " + eps[n]);
			//logService.info(this.getClass().getName() + " n:" + n + " lnEps:  "+  lnEps[n]);
			//logService.info(this.getClass().getName() + " n:" + n + " totals[n]: " + totals[n]);		
		}
		
		//Create double log plot
		boolean isLineVisible = false; //?		
		// Plot //nur ein Band!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		double[] lnDataX = new double[numDiscs];
		double[] lnDataY = new double[numDiscs];
			
		for (int n = 0; n < numDiscs; n++) {	
			lnDataY[n] = lnTotals[n];		
			lnDataX[n] = lnEps[n];
		}
		// System.out.println("FractalDimensionBoxCounting: dataY: "+ dataY);
		// System.out.println("FractalDimensionBoxCounting: dataX: "+ dataX);
	
		if (optShowPlot) {			
			String preName = "";
			String axisNameX = "";
			String axisNameY = "";
			if (numSlices > 1) {
				preName = "Slice-"+String.format("%03d", plane) +"-";
			}
			if (scanningType.equals("Sliding disc")) {
				axisNameX = "ln(Radius)";
				axisNameY = "ln(Count)";
			}
			else if (scanningType.equals("Raster box")) {
				axisNameX = "ln(Box width)";
				axisNameY = "ln(Count^2)";
			}
			
			Plot_RegressionFrame doubleLogPlot = DisplayRegressionPlotXY(lnDataX, lnDataY, isLineVisible, "Double log plot - Mass radius dimension", 
					preName + datasetName, axisNameX, axisNameY, "",
					numRegStart, numRegEnd);
			doubleLogPlotList.add(doubleLogPlot);
		}
		
		// Compute regression
		Regression_Linear lr = new Regression_Linear();
		regressionParams = lr.calculateParameters(lnDataX, lnDataY, numRegStart, numRegEnd);
		//0 Intercept, 1 Slope, 2 InterceptStdErr, 3 SlopeStdErr, 4 RSquared
	
		//Compute result values
		resultValues = regressionParams;
		double dim = Double.NaN;
		dim = regressionParams[1]; //dim = slope
		//resultValues[1] = dim;
		logService.info(this.getClass().getName() + " Mass radius dimension: " + dim);
		
		epsRegStartEnd[0] = eps[numRegStart-1];
		epsRegStartEnd[1] = eps[numRegEnd-1];
	
		return new Container_ProcessMethod(resultValues, epsRegStartEnd);
		//Output
		//uiService.show(tableOutName, table);
	    ////result = ops.create().img(image, new FloatType()); may not work in older Fiji versions
		//result = new ArrayImgFactory<>(new FloatType()).create(image.dimension(0), image.dimension(1)); 
		//table
	}

	
	// Find the center of the image
	// This methods computes the center of mass;
	private  double[] computeCenterOfMass(RandomAccessibleInterval<?> rai) {
	
		double[] com = new double[2];
		float sumGrey = 0f;

		//if( (datasetIn.firstElement() instanceof UnsignedByteType) ){
		//}
		Cursor<?> cursor = Views.iterable(rai).localizingCursor();
		final long[] pos = new long[rai.numDimensions()];
		while (cursor.hasNext()) {
			cursor.fwd();
			cursor.localize(pos);
			com[0]= com[0] + pos[0] * ((UnsignedByteType) cursor.get()).get();
			com[1]= com[1] + pos[1] * ((UnsignedByteType) cursor.get()).get();
			sumGrey = sumGrey + ((UnsignedByteType) cursor.get()).get();
		}
		com[0]= com[0]/sumGrey;
		com[1]= com[1]/sumGrey;
			
		
		return com;
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
	private Plot_RegressionFrame DisplayRegressionPlotXY(double[] dataX, double[] dataY, boolean isLineVisible,
			String frameTitle, String plotLabel, String xAxisLabel, String yAxisLabel, String legendLabel, int numRegStart, int numRegEnd) {
		// jFreeChart
		Plot_RegressionFrame pl = new Plot_RegressionFrame(dataX, dataY, isLineVisible, frameTitle, plotLabel, xAxisLabel,
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
		//ij.command().run(Csaj2DFracDimMassRadius.class, true).get().getOutput("image");
		ij.command().run(Csaj2DFracDimMassRadius.class, true);
	}
}

